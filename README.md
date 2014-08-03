play2-fastassets
================

Accelerates Play2 development mode by leveraging browser cache. 

At first, add the following dependency into your ```project/Build.scala```:

```scala
resolvers += "amateras-repo" at "http://amateras.sourceforge.jp/mvn/"

libraryDependencies += "jp.sf.amateras.play2.fastassets" %% "play2-fastassets" % "0.0.4"
```

Replace the routing to ```controllers.Assets.at``` by ```jp.sf.amateras.play2.fastassets.FastAssets.get``` in ```conf/routes```.
This method returns a response which has a header: ```Cache-Control: private, max-age=3600```.

```shell
#GET /assets/*file controllers.Assets.at(path="/public", file)
GET /assets/*file jp.sf.amateras.play2.fastassets.FastAssets.get(file)
```

And add following configurations into ```conf/application.conf```.

```shell
fastassets.urlPath=/assets
fastassets.realPath=/public
fastassets.versioning=true
```

Use ```FastAssets.at``` instead of ```routes.Assets.at``` in HTML templates.
This method appends a last modified timestamp to the filename and your browser cache it.
When you update the file, this timestamp is also updated. 
So the browser retrieves a new file from the server instead of the cached contents.

```html
@(title: String)(content: Html)
@import jp.sf.amateras.play2.fastassets.FastAssets
<!DOCTYPE html>
<html>
  <head>
    <title>@title</title>
    <link rel="stylesheet" media="screen" href="@FastAssets.at("stylesheets/main.css")">
    <link rel="shortcut icon" type="image/png" href="@FastAssets.at("images/favicon.png")">
    <script src="@FastAssets.at("javascripts/jquery-1.7.1.min.js")" type="text/javascript"></script>
  </head>
  <body>
    @content
  </body>
</html>
```

Release Notes
--------
### 0.1.0 - 3 Aug 2014

* Update for Play 2.3 / Scala 2.11

### 0.0.4 - 24 Apr 2014

* Add `fastassets.versioning` configuration to enable asset versioning in production.

### 0.0.3 - 16 Apr 2014

* Update for play-2.2.2.

### 0.0.2 - 05 Jan 2014

* Ignore Exception in ```FastAssets#at()``` for testing.

### 0.0.1 - 25 Feb 2013

* This is an initial release of play2-fastassets. This version works for play-2.0.1.
