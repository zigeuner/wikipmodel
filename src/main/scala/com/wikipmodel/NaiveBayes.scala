package com.wikipmodel

import java.io.{File, FilenameFilter}

import jline.ConsoleReader
import org.apache.spark.SparkContext
import org.apache.spark.mllib.classification.{NaiveBayes, NaiveBayesModel}
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint

object NaiveBayesExample extends App {
  // 4 workers
  val sc = new SparkContext("local[4]", "naivebayes")

  val xmlFile = if (args.length == 1) {
    args.head
  } else {
    "wpslice.xml"
  }

  val naiveBayesAndDictionaries = createNaiveBayesModel(xmlFile)

//  val naiveBayesAndDictionaries = createNaiveBayesModelReuters(xmlFile)
  console(naiveBayesAndDictionaries)

//  predict(naiveBayesAndDictionaries, "algeria")
//  predict(naiveBayesAndDictionaries, "computers are great")
//  sc.stop()

  /**
   * REPL loop to enter content directly
   */
  def console(naiveBayesAndDictionaries: NaiveBayesAndDictionaries) = {
    println("Enter 'q' to quit")
    val consoleReader = new ConsoleReader()
    while ( {
      consoleReader.readLine("text> ") match {
        case s if s == "q" => false
        case text: String =>
          predict(naiveBayesAndDictionaries, text)
          true
        case _ => true
      }
    }) {}

    sc.stop()
  }

  def predict(naiveBayesAndDictionaries: NaiveBayesAndDictionaries, content: String) = {
    // extract content from web page
//    val config = new Configuration
//    config.setEnableImageFetching(false)

    // tokenize content and stem it
    val tokens = Tokenizer.tokenize(content)
    // compute TFIDF vector
    val tfIdfs = naiveBayesAndDictionaries.termDictionary.tfIdfs(tokens, naiveBayesAndDictionaries.idfs)
    val vector = naiveBayesAndDictionaries.termDictionary.vectorize(tfIdfs)
    val labelId = naiveBayesAndDictionaries.model.predict(vector)

    // convert label from double
    println("Label: " + naiveBayesAndDictionaries.labelDictionary.valueOf(labelId.toInt))
  }

  /**
   * Load Wikipedia data and build NB model
   */
  def createNaiveBayesModel(xmlFile: String) = {
    println("loading documents from: " + xmlFile)
    val wparser = new WikipediaParser()
    val docs = wparser.parse(xmlFile)
    println("finished initiating parsing")

    // iterator extract terms and output new class which is Document + Terms
    val termDocs = Tokenizer.tokenizeAll(docs.toIterable)

    // put collection in Spark
    println("putting termDocs into RDD...")
    val termDocsRdd = sc.parallelize[TermDoc](termDocs.toSeq)
    val numDocs = termDocs.size
    println("number of documents = " + numDocs)

    // create dictionary term => id, and id => term
    println("collecting terms to make vocabulary...")
    val terms = termDocsRdd.flatMap(_.terms).distinct().collect().sortBy(identity)
    val termDict = new Dictionary(terms)
    println("VOCAB:" + termDict.toString)
    println("number of terms in VOCAB: " + termDict.count)

    println("collecting labels to make dictionary...")
    val labels = termDocsRdd.flatMap(_.labels).distinct().collect()
    val labelDict = new Dictionary(labels)
    println("LABELS:" + labelDict.termToIndex.toString)
    println("number of labels:" + labelDict.count)

    // compute TFIDF and generate vectors for IDF
    val idfs = (termDocsRdd.flatMap(termDoc => 
      termDoc.terms.map((termDoc.doc, _))).distinct().groupBy(_._2) collect {
      // if term is present in less than X documents then remove it
      case (term, docs) if docs.size > 3 =>
        term -> (numDocs.toDouble / docs.size.toDouble)
    }).collect.toMap

    println("IDFs:" + idfs)

    val tfidfs = termDocsRdd flatMap {
      termDoc =>
        val termPairs = termDict.tfIdfs(termDoc.terms, idfs)
        // we consider here that a document only belongs to the first label
        termDoc.labels.headOption.map {
          label =>
            val labelId = labelDict.indexOf(label).toDouble
            val vector = Vectors.sparse(termDict.count, termPairs)
            LabeledPoint(labelId, vector)
        }
    }
    println("TF-IDFs:" + tfidfs)
 
    val model = NaiveBayes.train(tfidfs)
    NaiveBayesAndDictionaries(model, termDict, labelDict, idfs)
  }

  /**
   * Original Reuters based NB model
   */
  def createNaiveBayesModelReuters(directory: String) = {
    val inputFiles = new File(directory).list(new FilenameFilter {
      override def accept(dir: File, name: String) = name.endsWith(".sgm")
    })

    println ("starting extraction from directory: " + directory)
    val fullFileNames = inputFiles.map(directory + "/" + _)
    val docs = ReutersParser.parseAll(fullFileNames)
    println ("done extracting Reuters set: " + docs.size)

    val termDocs = Tokenizer.tokenizeAll(docs)

    // put collection in Spark
    val termDocsRdd = sc.parallelize[TermDoc](termDocs.toSeq)
    val numDocs = termDocs.size

    // create dictionary term => id
    // and id => term
    val terms = termDocsRdd.flatMap(_.terms).distinct().collect().sortBy(identity)
    val termDict = new Dictionary(terms)

    val labels = termDocsRdd.flatMap(_.labels).distinct().collect()
    val labelDict = new Dictionary(labels)

    // compute TFIDF and generate vectors
    // for IDF
    val idfs = (termDocsRdd.flatMap(termDoc => termDoc.terms.map((termDoc.doc, _))).distinct().groupBy(_._2) collect {
      // mapValues not implemented :-(
      // if term is present in less than 3 documents then remove it
      case (term, docs) if docs.size > 3 =>
        term -> (numDocs.toDouble / docs.size.toDouble)
    }).collect.toMap

    val tfidfs = termDocsRdd flatMap {
      termDoc =>
        val termPairs = termDict.tfIdfs(termDoc.terms, idfs)
        // we consider here that a document only belongs to the first label
        termDoc.labels.headOption.map {
          label =>
            val labelId = labelDict.indexOf(label).toDouble
            val vector = Vectors.sparse(termDict.count, termPairs)
            LabeledPoint(labelId, vector)
        }
    }

    val model = NaiveBayes.train(tfidfs)
    NaiveBayesAndDictionaries(model, termDict, labelDict, idfs)
  }
}

case class NaiveBayesAndDictionaries(model: NaiveBayesModel,
                                     termDictionary: Dictionary,
                                     labelDictionary: Dictionary,
                                     idfs: Map[String, Double])
