import com.wikipmodel._

import org.apache.spark.SparkContext
import org.apache.hadoop.fs.FileSystem

object SaveWikipedia extends App {

  // setup Spark context
  val sc = new SparkContext("local[4]", "testSave2HDFS")

//  val xmlFile = "wpslice.xml" 
  val xmlFile = "wpslice100K.xml" 
//  val xmlFile = "wpsmall.xml"

  // look for RDD of TermDocs, load or make it 
  val rdddump = "hdfs://localhost/Users/data/hdfs/namenode/wikipediaRDD.txt"
  val termDocsRdd = try {
    val tempRdd = sc.textFile(rdddump)
//    val tempRdd = sc.objectFile(rdddump)
    println("loaded number of documents = " + tempRdd.count)
    tempRdd
  } catch {
    case _ : Throwable => {
      println("failed to load RDD from HDFS")
      println("loading documents from: " + xmlFile)
      val wparser = new WikipediaParser()
      val docs = wparser.parse(xmlFile)
      println("finished initiating parsing")

      // iterator extract terms and output new class which is Document + Terms
      val termDocs = Tokenizer.tokenizeAll(docs.toIterable)

      // put collection in Spark
      println("putting termDocs into RDD...")
      val newRdd = sc.parallelize[TermDoc](termDocs.toSeq)
      val numDocs = termDocs.size

      // try to save to HDFS
      // should try: saveAsSequenceFile(path)  AND/OR saveAsObjectFile(path)
      // and load like: .objectFile or .sequenceFile
      newRdd.saveAsTextFile(rdddump)
//      newRdd.saveAsObjectFile(rdddump)
      newRdd
    }
  }

  println(termDocsRdd)
  println("number of documents = " + termDocsRdd.count)

//  def manOf[T: Manifest](t: T): Manifest[T] = manifest[T]
//  println(manOf(results))

//  println("number of documents = " + numDocs)

  sc.stop()
}

