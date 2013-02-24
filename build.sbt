name := "play2-fastassets"

organization := "jp.sf.amateras.play2.fastassets"

version := "0.0.1"

scalaVersion := "2.9.1"

resolvers += "amateras-repo" at "http://amateras.sourceforge.jp/mvn/"

resolvers += "Typsafe releases" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "play" %% "play" % "2.0.1" % "provided->default"
)

publishTo := Some(Resolver.ssh("amateras-repo-scp", "shell.sourceforge.jp", "/home/groups/a/am/amateras/htdocs/mvn/") withPermissions("0664")
  as(System.getProperty("user.name"), new java.io.File(Path.userHome.absolutePath + "/.ssh/id_rsa")))
