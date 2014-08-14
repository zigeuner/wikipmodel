name := "wikipmodel"

version := "1.0"

scalaVersion := "2.10.4"

val sparkVersion = "1.0.2"

libraryDependencies <<= scalaVersion {
  scala_version => Seq(
    // Spark and Mllib
    "org.apache.spark" %% "spark-core" % sparkVersion,
    "org.apache.spark" %% "spark-mllib" % sparkVersion,
    // hadoop
    "org.apache.hadoop" % "hadoop-client" % "2.2.0",
    "org.apache.hadoop" % "hadoop-hdfs" % "2.2.0",
    // Lucene
    "org.apache.lucene" % "lucene-core" % "4.8.1",
    // for Porter Stemmer
    "org.apache.lucene" % "lucene-analyzers-common" % "4.8.1",
    // for testing
    "org.scalatest" % "scalatest_2.10" % "2.1.3" % "test",
    // for wikipedia parsing
    "info.bliki.wiki" % "bliki-core" % "3.0.19",
    // Guava for the dictionary
    "com.google.guava" % "guava" % "17.0"
  )
}

resolvers ++= Seq(
	"Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases"
)


// used for goose
//resolvers += Resolver.mavenLocal
