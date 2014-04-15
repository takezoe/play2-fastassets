name := "play2-fastassets"

organization := "jp.sf.amateras.play2.fastassets"

version := "0.0.4"

scalaVersion := "2.10.4"

resolvers += "amateras-repo" at "http://amateras.sourceforge.jp/mvn/"

resolvers += "Typsafe releases" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play" % "2.2.2" % "provided->default"
)

publishTo := Some(Resolver.ssh("amateras-repo-scp", "shell.sourceforge.jp", "/home/groups/a/am/amateras/htdocs/mvn/") withPermissions("0664")
  as(System.getProperty("user.name"), new java.io.File(Path.userHome.absolutePath + "/.ssh/id_rsa")))
