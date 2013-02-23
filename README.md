play2-fastassets
================

Accelerates Play2 development mode by leveraging browser cache. 

Replace the routing to ```controllers.Assets.at``` by ```jp.sf.amateras.play2.fastassets.FastAssets.get```.
This method returns a response which has a header: ```Cache-Control: private, max-age=3600```.

```shell
#GET /assets/*file controllers.Assets.at(path="/public", file)
GET /assets/*file jp.sf.amateras.play2.fastassets.FastAssets.get(file)
```

Add following configurations into ```conf/application.conf```.

```shell
fastassets.urlPath=/assets
fastassets.realPath=/public
```

And use ```FastAssets.at``` instead of ```routes.Assets.at``` in HTML templates.
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
    <img src="@FastAssets.at("images/favicon.png")"/>
  </body>
</html>

```