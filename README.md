play2-fastassets
================

Accelerates Play2 development mode by leveraging browser cache. 

At first, add the following dependency into your ```project/Build.scala```:

```scala
resolvers += "amateras-repo" at "http://amateras.sourceforge.jp/mvn/"

libraryDependencies += "jp.sf.amateras.play2.fastassets" %% "play2-fastassets" % "0.0.1"
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
### 0.0.1 - Initial Release

* This is an initial release of play2-fastassets. This version works for play-2.0.1.
