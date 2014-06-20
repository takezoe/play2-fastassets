name := "play2-fastassets"

organization := "jp.sf.amateras.play2.fastassets"

version := "0.1.0"

scalaVersion := "2.11.1"

resolvers += "amateras-repo" at "http://amateras.sourceforge.jp/mvn/"

resolvers += "Typesafe releases" at "http://repo.typesafe.com/typesafe/releases/"

//scalacOptions in compile ++= Seq(
//  "-deprecation"
//)

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.3.0" % "provided->default"
)

publishTo := Some(Resolver.ssh("amateras-repo-scp", "shell.sourceforge.jp", "/home/groups/a/am/amateras/htdocs/mvn/") withPermissions("0664")
  as(System.getProperty("user.name"), new java.io.File(Path.userHome.absolutePath + "/.ssh/id_rsa")))
