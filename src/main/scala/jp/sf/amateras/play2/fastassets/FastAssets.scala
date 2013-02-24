package jp.sf.amateras.play2.fastassets

import play.api._
import play.api.mvc._
import play.api.libs._
import play.api.libs.iteratee._
import Play.current
import java.io._
import java.net.JarURLConnection
import scalax.io.{ Resource }
import org.joda.time.format.{DateTimeFormatter, DateTimeFormat}
import org.joda.time.DateTimeZone
import collection.JavaConverters._
import java.text.SimpleDateFormat

/**
 * Controller that serves static resources.
 *
 * Resources are searched in the classpath.
 *
 * It handles Last-Modified and ETag header automatically.
 * If a gzipped version of a resource is found (Same resource name with the .gz suffix), it is served instead.
 *
 * You can set a custom Cache directive for a particular resource if needed. For example in your application.conf file:
 *
 * {{{
 * "assets.cache./public/images/logo.png" = "max-age=3600"
 * }}}
 *
 * You can use this controller in any application, just by declaring the appropriate route. For example:
 * {{{
 * GET     /assets/\uFEFF*file               controllers.Assets.at(path="/public", file)
 * }}}
 */
object FastAssets extends Controller {

  private lazy val urlPath = play.Play.application().configuration().getString("fastassets.urlPath")
    
  private lazy val realPath = play.Play.application().configuration().getString("fastassets.realPath")
  
  private val timeZoneCode = "GMT"

  //Dateformatter is immutable and threadsafe
  private val df: DateTimeFormatter = 
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss '"+timeZoneCode+"'").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))
  
  //Dateformatter is immutable and threadsafe
  private val dfp: DateTimeFormatter = 
    DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))
  
  private val parsableTimezoneCode = " "+timeZoneCode

  /**
   * Use this method instead of ```routes.Assets.at``` in your HTML template files.
   * This method appends a timestamp to the filename to cache it by the browser.
   */
  def at(file: String): String = {
    Play.mode match {
      case Mode.Prod => urlPath + "/" + file
      case _ => {
        val resourceName = Option(realPath + "/" + file).map(name => if (name.startsWith("/")) name else ("/" + name)).get
        Play.resource(resourceName) match {
          case Some(x) => urlPath + "/" + file.replaceFirst("\\.([^.]+)$", "_" + lastModifiedForTimestamp(x).get + ".$1")
          case None    => urlPath + "/" + file
        }
      }
    }
  }
  
  //Dateformatter is immutable and threadsafe
  private val tf: DateTimeFormatter = 
    DateTimeFormat.forPattern("yyyyMMddHHmmssSSS").withLocale(java.util.Locale.ENGLISH).withZone(DateTimeZone.forID(timeZoneCode))
    
  private def lastModifiedForTimestamp(resource: java.net.URL): Option[String] = {
    lastModifieds.get(resource.toExternalForm).filter(_ => Play.isProd).orElse {
      val maybeLastModified = resource.getProtocol match {
        case "file" => Some(tf.print({new java.util.Date(new java.io.File(resource.getPath).lastModified).getTime}))
        case "jar" => {
            resource.getPath.split('!').drop(1).headOption.flatMap { fileNameInJar =>
              Option(resource.openConnection)
               .collect { case c: JarURLConnection => c }
               .flatMap(c => Option(c.getJarFile.getJarEntry(fileNameInJar.drop(1))))
               .map(_.getTime)
               .filterNot(_ == 0)
               .map(lastModified => tf.print({new java.util.Date(lastModified)}.getTime)) 
            }
        }
        case _ => None
      }
      maybeLastModified.foreach(lastModifieds.put(resource.toExternalForm, _))
      maybeLastModified
    }
  }
  
  /**
   * Generates an `Action` that serves a static resource.
   *
   * @param path the root folder for searching the static resource files, such as `"/public"`
   * @param file the file part extracted from the URL
   */
  def get(file: String): Action[AnyContent] = Action { request =>
    // -- LastModified handling
    def parseDate(date: String): Option[java.util.Date] = try {
      //jodatime does not parse timezones, so we handle that manually
      val d = dfp.parseDateTime(date.replace(parsableTimezoneCode,"")).toDate
      Some(d)
    } catch {
      case _: Exception => None
    }
    
    // remove timestamp
    val realfile = file.replaceFirst("""_[0-9]{17}\.""", ".");

    val resourceName = Option(realPath + "/" + realfile).map(name => if (name.startsWith("/")) name else ("/" + name)).get

    if (new File(resourceName).isDirectory || !new File(resourceName).getCanonicalPath.startsWith(new File(realPath).getCanonicalPath)) {
      NotFound
    } else {

      val resource = {
        Play.resource(resourceName + ".gz").map(_ -> true)
          .filter(_ => request.headers.get(ACCEPT_ENCODING).map(_.split(',').exists(_ == "gzip" && Play.isProd)).getOrElse(false))
          .orElse(Play.resource(resourceName).map(_ -> false))
      }

      resource.map {

        case (url, _) if new File(url.getFile).isDirectory => NotFound

        case (url, isGzipped) => {

          lazy val (length, resourceData) = {
            val stream = url.openStream()
            try {
              (stream.available, Enumerator.fromStream(stream))
            } catch {
              case _ => (0, Enumerator[Array[Byte]]())
            }
          }

          if(length == 0) {
            NotFound
          } else {
            request.headers.get(IF_NONE_MATCH).flatMap { ifNoneMatch => 
              etagFor(url).filter(_ == ifNoneMatch)
            }.map (_ => NotModified).getOrElse {
              request.headers.get(IF_MODIFIED_SINCE).flatMap(parseDate).flatMap { ifModifiedSince =>
                lastModifiedFor(url).flatMap(parseDate).filterNot(lastModified => lastModified.after(ifModifiedSince))
              }.map (_ => NotModified.withHeaders(
                DATE -> df.print({new java.util.Date}.getTime)
              )).getOrElse {

                // Prepare a streamed response
                val response = SimpleResult(
                  header = ResponseHeader(OK, Map(
                    CONTENT_LENGTH -> length.toString,
                    CONTENT_TYPE -> MimeTypes.forFileName(realfile).getOrElse(BINARY),
                    DATE -> df.print({new java.util.Date}.getTime)
                  )),
                  resourceData
                )

                // Is Gzipped?
                val gzippedResponse = if (isGzipped) {
                  response.withHeaders(CONTENT_ENCODING -> "gzip")
                } else {
                  response
                }

                // Add Etag if we are able to compute it
                val taggedResponse = etagFor(url).map(etag => gzippedResponse.withHeaders(ETAG -> etag)).getOrElse(gzippedResponse)
                val lastModifiedResponse = lastModifiedFor(url).map(lastModified => taggedResponse.withHeaders(LAST_MODIFIED -> lastModified)).getOrElse(taggedResponse)

                // Add Cache directive if configured
                val cachedResponse = gzippedResponse.withHeaders(CACHE_CONTROL -> {
                  Play.configuration.getString("\"assets.cache." + resourceName + "\"").getOrElse(Play.mode match {
                    case Mode.Prod => Play.configuration.getString("assets.defaultCache").getOrElse("max-age=3600")
                    case _ => "private, max-age=3600"
                  })
                })

                cachedResponse

              }:Result

            }

          }

        }

      }.getOrElse(NotFound)

    }

  }

  private val lastModifieds = (new java.util.concurrent.ConcurrentHashMap[String, String]()).asScala

  private def lastModifiedFor(resource: java.net.URL): Option[String] = {
    lastModifieds.get(resource.toExternalForm).filter(_ => Play.isProd).orElse {
      val maybeLastModified = resource.getProtocol match {
        case "file" => Some(df.print({new java.util.Date(new java.io.File(resource.getPath).lastModified).getTime}))
        case "jar" => {
            resource.getPath.split('!').drop(1).headOption.flatMap { fileNameInJar =>
              Option(resource.openConnection)
               .collect { case c: JarURLConnection => c }
               .flatMap(c => Option(c.getJarFile.getJarEntry(fileNameInJar.drop(1))))
               .map(_.getTime)
               .filterNot(_ == 0)
               .map(lastModified => df.print({new java.util.Date(lastModified)}.getTime)) 
            }
        }
        case _ => None
      }
      maybeLastModified.foreach(lastModifieds.put(resource.toExternalForm, _))
      maybeLastModified
    }
  }

  // -- ETags handling

  private val etags = (new java.util.concurrent.ConcurrentHashMap[String, String]()).asScala

  private def etagFor(resource: java.net.URL): Option[String] = {
    etags.get(resource.toExternalForm).filter(_ => Play.isProd).orElse {
      val maybeEtag = lastModifiedFor(resource).map(_ + " -> " + resource.toExternalForm).map("\""+Codecs.sha1(_)+"\"")
      maybeEtag.foreach(etags.put(resource.toExternalForm, _))
      maybeEtag
    }
  }

}