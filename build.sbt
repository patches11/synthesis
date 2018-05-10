name := "synthesis"

version := "0.1"

scalaVersion := "2.11.8"

fork in run := true

resolvers +=
  "JBoss 3rd-party Repository" at "https://repository.jboss.org/nexus/content/repositories/thirdparty-releases/"

resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

javaOptions in run += "-Djava.library.path=/usr/lib/jni"
//javaOptions in run += "-Djava.library.path=/usr/local/Cellar/opencv/2.4.13.2/share/OpenCV/java"

javaOptions in run += "-Dsun.java2d.opengl=True"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.4.10"

libraryDependencies += "ch.jodersky" %% "akka-serial-core" % "4.1.0"
libraryDependencies += "ch.jodersky" % "akka-serial-native" % "4.1.0" % "runtime"

libraryDependencies += "com.fazecast" % "jSerialComm" % "[1.0.0,2.0.0)"

libraryDependencies += "com.google.code.findbugs" % "jsr305" % "1.3.+" % "compile"

// https://mvnrepository.com/artifact/com.twelvemonkeys.imageio/imageio-batik
libraryDependencies += "com.twelvemonkeys.imageio" % "imageio" % "3.2.1"
libraryDependencies += "com.twelvemonkeys.imageio" % "imageio-batik" % "3.2.1"
libraryDependencies += "com.twelvemonkeys.imageio" % "imageio-core" % "3.2.1"
libraryDependencies += "com.twelvemonkeys.common" % "common-lang" % "3.2.1"


libraryDependencies += "batik" % "batik-transcoder" % "1.6-1"

libraryDependencies += "net.java.dev.jna" % "jna" % "4.1.0"

libraryDependencies += "de.ummels" % "scala-prioritymap_2.11" % "1.0.0"

// Breeze

libraryDependencies += "org.scalanlp" %% "breeze" % "0.13.2"

libraryDependencies += "org.scalanlp" %% "breeze-natives" % "0.13.2"

// Koloboke

libraryDependencies += "com.koloboke" % "koloboke-impl-jdk8" % "1.0.0"
libraryDependencies += "com.koloboke" % "koloboke-api-jdk8" % "1.0.0"