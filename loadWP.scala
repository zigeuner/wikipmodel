import com.wikipmodel._

object LoadWikipedia extends App {

  val xmlFile = "wpslice.xml"
//  val xmlFile = "wpsmall.xml"

  println("loading documents from: " + xmlFile)
  val wparser = new WikipediaParser()
  val docs = wparser.parse(xmlFile)
  println("finished initiating parsing")

//  for (doc <- docs) { doc match {
//    case Document(id, body, labels) => println(id + "  " + body)}}

  val results = docs map {
    case Document(id, body, labels) => (id, body.split("\\s+").size)
  }

  def manOf[T: Manifest](t: T): Manifest[T] = manifest[T]
  println(manOf(results))

  var count = 0
  for (tup <- results) { 
    count += 1
    tup match {
      case (id, nwords) => if (nwords > 1) println(id + "  " + nwords)
    }
  }

  println ("number of Wikipedia documents parsed: " + count)

}

