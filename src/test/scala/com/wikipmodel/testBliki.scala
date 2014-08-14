import org.scalatest.FunSuite

import org.apache.commons.lang.StringEscapeUtils
import info.bliki.wiki.filter.PlainTextConverter
import info.bliki.wiki.model.WikiModel

class ParseWikipediaTest1 extends FunSuite {

  val wikiModel = new WikiModel("", "")
  val textConverter = new PlainTextConverter()

  // basic example using only Bliki
  wikiModel.setUp();
  val text = wikiModel.render(textConverter, 
    "This is a simple [[Hello World]] wiki tag");
  test("Bliki is capable of recognizing and removing [] notation") {
    assert(text == "This is a simple Hello World wiki tag")
  }
  wikiModel.tearDown();
}

