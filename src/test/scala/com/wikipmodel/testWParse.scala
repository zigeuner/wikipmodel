import org.scalatest.FunSuite
import com.wikipmodel._

class ParseWikipediaTest2 extends FunSuite {

  // more complex example using Bliki + post-processing
  val wparser = new WikipediaParser()
  val text2 = "'''Autism''' is a [[neurodevelopmental disorder]] characterized by impaired [[Interpersonal relationship|social interaction]], [[language acquisition|verbal]] and [[non-verbal communication]], and by restricted and repetitive behavior. The [[Diagnostic and Statistical Manual of Mental Disorders|diagnostic criteria]] require that symptoms become apparent before a child is three years old.&lt;ref name=&quot;DSM-IV-TR-299.00&quot;&gt;{{vcite book blah blah}}&lt;/ref&gt;"
  val text3 = wparser.strip2text(text2)
  test("Bliki+post-processing is capable of stripping complex xml to text") {
    assert(text3 == "Autism is a neurodevelopmental disorder characterized by impaired social interaction, verbal and non-verbal communication, and by restricted and repetitive behavior. The diagnostic criteria require that symptoms become apparent before a child is three years old. ")
  }

  // parse a more complex example (mostly for debugging, no test)
  val xmlFile = "wpautism_page.xml"
  println(xmlFile)
  val source = scala.io.Source.fromFile(xmlFile, "utf-8")
  val xml = source.mkString
  source.close()
  val txtres = wparser.strip2text(xml)
//  println("===TEST======\n" + txtres + "============")
}

