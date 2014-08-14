package com.wikipmodel

// based on tuxdna's wikipedia.scala

import scala.io.Source
import scala.xml.pull._
import scala.collection.mutable.ArrayBuffer
import java.io.File
import java.io.FileOutputStream
import scala.xml.XML

// for wikipedia markup language processing
import org.apache.commons.lang.StringEscapeUtils
import info.bliki.wiki.filter.PlainTextConverter
import info.bliki.wiki.model.WikiModel

class WikipediaParser() {
  // setup Bliki classes
  val wikiModel = new WikiModel("", "")
  val textConverter = new PlainTextConverter()
  wikiModel.setUp();

  // regexs for removing unnecessaries from wikipedia text
  val shortrefPat = """\<ref.*?\/\>""".r
  val fullrefPat = """(?s)\<ref.*?\>.*?\</ref\>""".r
  val langlinksPat = """\[\[[a-z\-]+:[^\]]+\]\]""".r
  val doublecurlyPat = """\{\{.*?\}\}""".r
  val urlPat = """http:\/\/[^ \<]+""".r
//  val htmltagPat = """<[^!][^>]*>""".r    // not currently used
  val htmlcommentPat = """(?s)\<\!\-\-.*?\-\-\>""".r
  val headerPat = """==+""".r

  def parse(xmlFile: String) = {
    val xml = new XMLEventReader(Source.fromFile(xmlFile, "latin1"))

    var insidePage = false
    var insideBody = false
    var insideTitle = false
    var buf = ArrayBuffer[String]()
    var currentDoc: Document = null

    xml.flatMap(event => 
      event match {
        case EvElemStart(_, "page", _, _) => {   // <page>
          insidePage = true
          currentDoc = Document()
          Iterator.empty
        }
        case EvElemEnd(_, "page") => {   // </page>
          insidePage = false
          currentDoc = currentDoc.copy(body = strip2text(buf.reduce(_ + _)))
          buf.clear
          Iterator(currentDoc)
        }
        case EvElemStart(_, tag, _, _) => {   // generic start tag
          if (insidePage) {
            if (tag == "title") {insideTitle = true}
            if (tag == "text") {insideBody = true}
          }
          Iterator.empty
        }
        case EvElemEnd(_, tag) => {        // generic end tag
          if (insidePage) {
            if (tag == "title") {insideTitle = false}
            if (tag == "text") {insideBody = false}
          }
          Iterator.empty
        }
        case EvText(t) => {     // body text
          if (insidePage) {
            if (insideTitle) {
              currentDoc = currentDoc.copy(labels = currentDoc.labels + t,
              docId = t)
            }
            if (insideBody) {buf += (t)}
          }
          Iterator.empty
        }
        case _ => Iterator.empty // ignore
      }    
    )   
  }

  def strip2text(wikitext: String) = {
    val txtinp = headerPat.replaceAllIn(wikitext, "\n")
    var txtres = wikiModel.render(textConverter, txtinp)

    // convert &gt (and similar) to xml format for further processing
    txtres = StringEscapeUtils.unescapeHtml(
      StringEscapeUtils.unescapeHtml(txtres))

    // remove unnecessary patterns in text
    txtres = htmlcommentPat.replaceAllIn(txtres, " ")
    txtres = shortrefPat.replaceAllIn(txtres, " ")
    txtres = fullrefPat.replaceAllIn(txtres, " ")
    txtres = langlinksPat.replaceAllIn(txtres, " ")
    txtres = doublecurlyPat.replaceAllIn(txtres, " ")
    urlPat.replaceAllIn(txtres, " ")
  }

}

