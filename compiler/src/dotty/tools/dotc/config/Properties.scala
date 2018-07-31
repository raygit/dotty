package dotty.tools
package dotc
package config

import scala.annotation.internal.sharable

import java.io.{ IOException, PrintWriter }
import java.util.jar.Attributes.{ Name => AttributeName }

/** Loads `library.properties` from the jar. */
object Properties extends PropertiesTrait {
  protected def propCategory    = "compiler"
  protected def pickJarBasedOn  = classOf[Option[_]]

  /** Scala manifest attributes.
   */
  @sharable val ScalaCompilerVersion = new AttributeName("Scala-Compiler-Version")
}

trait PropertiesTrait {
  protected def propCategory: String      // specializes the remainder of the values
  protected def pickJarBasedOn: Class[_]  // props file comes from jar containing this

  /** The name of the properties file */
  protected val propFilename = "/" + propCategory + ".properties"

  /** The loaded properties */
  @sharable protected lazy val scalaProps: java.util.Properties = {
    val props = new java.util.Properties
    val stream = pickJarBasedOn getResourceAsStream propFilename
    if (stream ne null)
      quietlyDispose(props load stream, stream.close)

    props
  }

  private def quietlyDispose(action: => Unit, disposal: => Unit) =
    try     { action }
    finally {
        try     { disposal }
        catch   { case _: IOException => }
    }

  def propIsSet(name: String)                   = System.getProperty(name) != null
  def propIsSetTo(name: String, value: String)  = propOrNull(name) == value
  def propOrElse(name: String, alt: String)     = System.getProperty(name, alt)
  def propOrEmpty(name: String)                 = propOrElse(name, "")
  def propOrNull(name: String)                  = propOrElse(name, null)
  def propOrNone(name: String)                  = Option(propOrNull(name))
  def propOrFalse(name: String)                 = propOrNone(name) exists (x => List("yes", "on", "true") contains x.toLowerCase)
  def setProp(name: String, value: String)      = System.setProperty(name, value)
  def clearProp(name: String)                   = System.clearProperty(name)

  def envOrElse(name: String, alt: String)      = Option(System getenv name) getOrElse alt
  def envOrNone(name: String)                   = Option(System getenv name)

  // for values based on propFilename
  def scalaPropOrElse(name: String, alt: String): String = scalaProps.getProperty(name, alt)
  def scalaPropOrEmpty(name: String): String             = scalaPropOrElse(name, "")
  def scalaPropOrNone(name: String): Option[String]      = Option(scalaProps.getProperty(name))

  /** Either the development or release version if known, otherwise
   *  the empty string.
   */
  def versionNumberString = scalaPropOrEmpty("version.number")

  /** The version number of the jar this was loaded from plus "version " prefix,
   *  or "version (unknown)" if it cannot be determined.
   */
  val versionString = {
    val v = scalaPropOrElse("version.number", "(unknown)")
    "version " + scalaPropOrElse("version.number", "(unknown)") + {
      if (v.contains("SNAPSHOT") || v.contains("NIGHTLY")) {
        "-git-" + scalaPropOrElse("git.hash", "(unknown)")
      } else ""
    }
  }

  /** Whether the current version of compiler is experimental
   *
   *  1. Snapshot and nightly releases are experimental.
   *  2. Features supported by experimental versions of the compiler:
   *     - research plugins
   */
  val experimental = versionString.contains("SNAPSHOT") || versionString.contains("NIGHTLY")

  val copyrightString       = scalaPropOrElse("copyright.string", "(c) 2002-2017 LAMP/EPFL")

  /** This is the encoding to use reading in source files, overridden with -encoding
   *  Note that it uses "prop" i.e. looks in the scala jar, not the system properties.
   */
  def sourceEncoding        = scalaPropOrElse("file.encoding", "UTF-8")
  def sourceReader          = scalaPropOrElse("source.reader", "scala.tools.nsc.io.SourceReader")

  /** This is the default text encoding, overridden (unreliably) with
   *  `JAVA_OPTS="-Dfile.encoding=Foo"`
   */
  def encodingString        = propOrElse("file.encoding", "UTF-8")

  /** The default end of line character.
   */
  def lineSeparator         = propOrElse("line.separator", "\n")

  /** Various well-known properties.
   */
  def javaClassPath         = propOrEmpty("java.class.path")
  def javaHome              = propOrEmpty("java.home")
  def javaVendor            = propOrEmpty("java.vendor")
  def javaVersion           = propOrEmpty("java.version")
  def javaVmInfo            = propOrEmpty("java.vm.info")
  def javaVmName            = propOrEmpty("java.vm.name")
  def javaVmVendor          = propOrEmpty("java.vm.vendor")
  def javaVmVersion         = propOrEmpty("java.vm.version")
  def osName                = propOrEmpty("os.name")
  def scalaHome             = propOrEmpty("scala.home")
  def tmpDir                = propOrEmpty("java.io.tmpdir")
  def userDir               = propOrEmpty("user.dir")
  def userHome              = propOrEmpty("user.home")
  def userName              = propOrEmpty("user.name")

  /** Some derived values.
   */
  def isWin                 = osName startsWith "Windows"
  def isMac                 = javaVendor startsWith "Apple"

  // This is looking for javac, tools.jar, etc.
  // Tries JDK_HOME first, then the more common but likely jre JAVA_HOME,
  // and finally the system property based javaHome.
  def jdkHome              = envOrElse("JDK_HOME", envOrElse("JAVA_HOME", javaHome))

  def versionMsg            = "Scala %s %s -- %s".format(propCategory, versionString, copyrightString)
  def scalaCmd              = if (isWin) "dotr.bat" else "dotr"
  def scalacCmd             = if (isWin) "dotc.bat" else "dotc"

  /** Can the java version be determined to be at least as high as the argument?
   *  Hard to properly future proof this but at the rate 1.7 is going we can leave
   *  the issue for our cyborg grandchildren to solve.
   */
  def isJavaAtLeast(version: String) = {
    val okVersions = version match {
      case "1.5"    => List("1.5", "1.6", "1.7", "1.8")
      case "1.6"    => List("1.6", "1.7", "1.8")
      case "1.7"    => List("1.7", "1.8")
      case "1.8"    => List("1.8")
      case _        => Nil
    }
    okVersions exists (javaVersion startsWith _)
  }
}
