package dotty.tools.dotc
package config

import dotty.tools.io.{ Directory, PlainDirectory, AbstractFile }
import PathResolver.Defaults
import rewrites.Rewrites
import Settings.Setting

class ScalaSettings extends Settings.SettingGroup {

  protected def defaultClasspath: String = sys.env.getOrElse("CLASSPATH", ".")

  /** Path related settings */
  val bootclasspath: Setting[String] = PathSetting("-bootclasspath", "Override location of bootstrap class files.", Defaults.scalaBootClassPath)
  val extdirs: Setting[String] = PathSetting("-extdirs", "Override location of installed extensions.", Defaults.scalaExtDirs)
  val javabootclasspath: Setting[String] = PathSetting("-javabootclasspath", "Override java boot classpath.", Defaults.javaBootClassPath)
  val javaextdirs: Setting[String] = PathSetting("-javaextdirs", "Override java extdirs classpath.", Defaults.javaExtDirs)
  val sourcepath: Setting[String] = PathSetting("-sourcepath", "Specify location(s) of source files.", Defaults.scalaSourcePath)
  val scansource: Setting[Boolean] = BooleanSetting("-scansource", "Scan source files to locate classes for which class-name != file-name")

  val classpath: Setting[String] = PathSetting("-classpath", "Specify where to find user class files.", defaultClasspath) withAbbreviation "-cp"
  val outputDir: Setting[AbstractFile] = OutputSetting("-d", "directory|jar", "destination for generated classfiles.",
    new PlainDirectory(Directory(".")))
  val priorityclasspath: Setting[String] = PathSetting("-priorityclasspath", "class path that takes precedence over all other paths (or testing only)", "")

  /** Other settings */
  val deprecation: Setting[Boolean] = BooleanSetting("-deprecation", "Emit warning and location for usages of deprecated APIs.")
  val migration: Setting[Boolean] = BooleanSetting("-migration", "Emit warning and location for migration issues from Scala 2.")
  val encoding: Setting[String] = StringSetting("-encoding", "encoding", "Specify character encoding used by source files.", Properties.sourceEncoding)
  val explainTypes: Setting[Boolean] = BooleanSetting("-explain-types", "Explain type errors in more detail.")
  val explain: Setting[Boolean] = BooleanSetting("-explain", "Explain errors in more detail.")
  val feature: Setting[Boolean] = BooleanSetting("-feature", "Emit warning and location for usages of features that should be imported explicitly.")
  val help: Setting[Boolean] = BooleanSetting("-help", "Print a synopsis of standard options")
  val color: Setting[String] = ChoiceSetting("-color", "mode", "Colored output", List("always", "never"/*, "auto"*/), "always"/* "auto"*/)
  val target: Setting[String] = ChoiceSetting("-target", "target", "Target platform for object files. All JVM 1.5 targets are deprecated.",
    List("jvm-1.5", "jvm-1.5-fjbg", "jvm-1.5-asm", "jvm-1.6", "jvm-1.7", "jvm-1.8", "msil"), "jvm-1.8")
  val unchecked: Setting[Boolean] = BooleanSetting("-unchecked", "Enable additional warnings where generated code depends on assumptions.")
  val uniqid: Setting[Boolean] = BooleanSetting("-uniqid", "Uniquely tag all identifiers in debugging output.")
  val usejavacp: Setting[Boolean] = BooleanSetting("-usejavacp", "Utilize the java.class.path in classpath resolution.")
  val verbose: Setting[Boolean] = BooleanSetting("-verbose", "Output messages about what the compiler is doing.")
  val version: Setting[Boolean] = BooleanSetting("-version", "Print product version and exit.")
  val pageWidth: Setting[Int] = IntSetting("-pagewidth", "Set page width", 80)
  val strict: Setting[Boolean] = BooleanSetting("-strict", "Use strict type rules, which means some formerly legal code does not typecheck anymore.")
  val language: Setting[List[String]] = MultiStringSetting("-language", "feature", "Enable one or more language features.")
  val rewrite: Setting[Option[Rewrites]] = OptionSetting[Rewrites]("-rewrite", "When used in conjunction with -language:Scala2 rewrites sources to migrate to new syntax")
  val silentWarnings: Setting[Boolean] = BooleanSetting("-nowarn", "Silence all warnings.")
  val fromTasty: Setting[Boolean] = BooleanSetting("-from-tasty", "Compile classes from tasty in classpath. The arguments are used as class names.")

  /** Decompiler settings */
  val printTasty: Setting[Boolean] = BooleanSetting("-print-tasty", "Prints the raw tasty.")
  val printLines: Setting[Boolean] = BooleanSetting("-print-lines", "Show source code line numbers.")

  /** Plugin-related setting */
  val plugin: Setting[List[String]]             = MultiStringSetting  ("-Xplugin", "paths", "Load a plugin from each classpath.")
  val disable: Setting[List[String]]            = MultiStringSetting  ("-Xplugin-disable", "plugin", "Disable plugins by name.")
  val require: Setting[List[String]]            = MultiStringSetting  ("-Xplugin-require", "plugin", "Abort if a named plugin is not loaded.")
  val showPlugins: Setting[Boolean]        = BooleanSetting      ("-Xplugin-list", "Print a synopsis of loaded plugins.")
  val pluginsDir: Setting[String]         = StringSetting       ("-Xpluginsdir", "path", "Path to search for plugin archives.", Defaults.scalaPluginPath)
  val pluginOptions: Setting[List[String]]      = MultiStringSetting  ("-P", "plugin:opt", "Pass an option to a plugin, e.g. -P:<plugin>:<opt>")

  /** -X "Advanced" settings
   */
  val Xhelp: Setting[Boolean] = BooleanSetting("-X", "Print a synopsis of advanced options.")
  val XnoForwarders: Setting[Boolean] = BooleanSetting("-Xno-forwarders", "Do not generate static forwarders in mirror classes.")
  val XminImplicitSearchDepth: Setting[Int] = IntSetting("-Xmin-implicit-search-depth", "Set number of levels of implicit searches undertaken before checking for divergence.", 5)
  val XmaxInlines: Setting[Int] = IntSetting("-Xmax-inlines", "Maximal number of successive inlines", 32)
  val XmaxClassfileName: Setting[Int] = IntSetting("-Xmax-classfile-name", "Maximum filename length for generated classes", 255, 72 to 255)
  val Xmigration: Setting[ScalaVersion] = VersionSetting("-Xmigration", "Warn about constructs whose behavior may have changed since version.")
  val Xprint: Setting[List[String]] = PhasesSetting("-Xprint", "Print out program after")
  val XprintTypes: Setting[Boolean] = BooleanSetting("-Xprint-types", "Print tree types (debugging option).")
  val XprintDiff: Setting[Boolean] = BooleanSetting("-Xprint-diff", "Print changed parts of the tree since last print.")
  val XprintDiffDel: Setting[Boolean] = BooleanSetting("-Xprint-diff-del", "Print changed parts of the tree since last print including deleted parts.")
  val Xprompt: Setting[Boolean] = BooleanSetting("-Xprompt", "Display a prompt after each error (debugging option).")
  val XmainClass: Setting[String] = StringSetting("-Xmain-class", "path", "Class for manifest's Main-Class entry (only useful with -d <jar>)", "")
  val XnoValueClasses: Setting[Boolean] = BooleanSetting("-Xno-value-classes", "Do not use value classes. Helps debugging.")
  val XreplLineWidth: Setting[Int] = IntSetting("-Xrepl-line-width", "Maximal number of columns per line for REPL output", 390)
  val XfatalWarnings: Setting[Boolean] = BooleanSetting("-Xfatal-warnings", "Fail the compilation if there are any warnings.")
  val XverifySignatures: Setting[Boolean] = BooleanSetting("-Xverify-signatures", "Verify generic signatures in generated bytecode.")

  /** -Y "Private" settings */
  val YoverrideVars: Setting[Boolean] = BooleanSetting("-Yoverride-vars", "Allow vars to be overridden.")
  val Yhelp: Setting[Boolean] = BooleanSetting("-Y", "Print a synopsis of private options.")
  val Ycheck: Setting[List[String]] = PhasesSetting("-Ycheck", "Check the tree at the end of")
  val YcheckMods: Setting[Boolean] = BooleanSetting("-Ycheck-mods", "Check that symbols and their defining trees have modifiers in sync")
  val Ydebug: Setting[Boolean] = BooleanSetting("-Ydebug", "Increase the quantity of debugging output.")
  val YdebugTrace: Setting[Boolean] = BooleanSetting("-Ydebug-trace", "Trace core operations")
  val YdebugFlags: Setting[Boolean] = BooleanSetting("-Ydebug-flags", "Print all flags of definitions")
  val YdebugMissingRefs: Setting[Boolean] = BooleanSetting("-Ydebug-missing-refs", "Print a stacktrace when a required symbol is missing")
  val YdebugNames: Setting[Boolean] = BooleanSetting("-Ydebug-names", "Show internal representation of names")
  val YtermConflict: Setting[String] = ChoiceSetting("-Yresolve-term-conflict", "strategy", "Resolve term conflicts", List("package", "object", "error"), "error")
  val Ylog: Setting[List[String]] = PhasesSetting("-Ylog", "Log operations during")
  val YemitTastyInClass: Setting[Boolean] = BooleanSetting("-Yemit-tasty-in-class", "Generate tasty in the .class file and add an empty *.hasTasty file.")
  val YlogClasspath: Setting[Boolean] = BooleanSetting("-Ylog-classpath", "Output information about what classpath is being applied.")
  val YdisableFlatCpCaching: Setting[Boolean]  = BooleanSetting("-YdisableFlatCpCaching", "Do not cache flat classpath representation of classpath elements from jars across compiler instances.")

  val Yscala2Unpickler: Setting[String] = StringSetting("-Yscala2-unpickler", "", "Control where we may get Scala 2 symbols from. This is either \"always\", \"never\", or a classpath.", "always")

  val YnoImports: Setting[Boolean] = BooleanSetting("-Yno-imports", "Compile without importing scala.*, java.lang.*, or Predef.")
  val YnoInline: Setting[Boolean] = BooleanSetting("-Yno-inline", "Suppress inlining.")
  val YnoGenericSig: Setting[Boolean] = BooleanSetting("-Yno-generic-signatures", "Suppress generation of generic signatures for Java.")
  val YnoPredef: Setting[Boolean] = BooleanSetting("-Yno-predef", "Compile without importing Predef.")
  val Yskip: Setting[List[String]] = PhasesSetting("-Yskip", "Skip")
  val Ydumpclasses: Setting[String] = StringSetting("-Ydump-classes", "dir", "Dump the generated bytecode to .class files (useful for reflective compilation that utilizes in-memory classloaders).", "")
  val YstopAfter: Setting[List[String]] = PhasesSetting("-Ystop-after", "Stop after") withAbbreviation ("-stop") // backward compat
  val YstopBefore: Setting[List[String]] = PhasesSetting("-Ystop-before", "Stop before") // stop before erasure as long as we have not debugged it fully
  val YtraceContextCreation: Setting[Boolean] = BooleanSetting("-Ytrace-context-creation", "Store stack trace of context creations.")
  val YshowSuppressedErrors: Setting[Boolean] = BooleanSetting("-Yshow-suppressed-errors", "Also show follow-on errors and warnings that are normally suppressed.")
  val YdetailedStats: Setting[Boolean] = BooleanSetting("-Ydetailed-stats", "show detailed internal compiler stats (needs Stats.enabled to be set to true).")
  val Yheartbeat: Setting[Boolean] = BooleanSetting("-Ydetailed-stats", "show heartbeat stack trace of compiler operations (needs Stats.enabled to be set to true).")
  val YprintPos: Setting[Boolean] = BooleanSetting("-Yprint-pos", "show tree positions.")
  val YprintPosSyms: Setting[Boolean] = BooleanSetting("-Yprint-pos-syms", "show symbol definitions positions.")
  val YnoDeepSubtypes: Setting[Boolean] = BooleanSetting("-Yno-deep-subtypes", "throw an exception on deep subtyping call stacks.")
  val YnoPatmatOpt: Setting[Boolean] = BooleanSetting("-Yno-patmat-opt", "disable all pattern matching optimizations.")
  val YplainPrinter: Setting[Boolean] = BooleanSetting("-Yplain-printer", "Pretty-print using a plain printer.")
  val YprintSyms: Setting[Boolean] = BooleanSetting("-Yprint-syms", "when printing trees print info in symbols instead of corresponding info in trees.")
  val YprintDebug: Setting[Boolean] = BooleanSetting("-Yprint-debug", "when printing trees, print some extra information useful for debugging.")
  val YprintDebugOwners: Setting[Boolean] = BooleanSetting("-Yprint-debug-owners", "when printing trees, print owners of definitions.")
  val YshowPrintErrors: Setting[Boolean] = BooleanSetting("-Yshow-print-errors", "don't suppress exceptions thrown during tree printing.")
  val YtestPickler: Setting[Boolean] = BooleanSetting("-Ytest-pickler", "self-test for pickling functionality; should be used with -Ystop-after:pickler")
  val YcheckReentrant: Setting[Boolean] = BooleanSetting("-Ycheck-reentrant", "check that compiled program does not contain vars that can be accessed from a global root.")
  val YdropComments: Setting[Boolean] = BooleanSetting("-Ydrop-comments", "Drop comments when scanning source files.")
  val YcookComments: Setting[Boolean] = BooleanSetting("-Ycook-comments", "Cook the comments (type check `@usecase`, etc.)")
  val YforceSbtPhases: Setting[Boolean] = BooleanSetting("-Yforce-sbt-phases", "Run the phases used by sbt for incremental compilation (ExtractDependencies and ExtractAPI) even if the compiler is ran outside of sbt, for debugging.")
  val YdumpSbtInc: Setting[Boolean] = BooleanSetting("-Ydump-sbt-inc", "For every compiled foo.scala, output the API representation and dependencies used for sbt incremental compilation in foo.inc, implies -Yforce-sbt-phases.")
  val YcheckAllPatmat: Setting[Boolean] = BooleanSetting("-Ycheck-all-patmat", "Check exhaustivity and redundancy of all pattern matching (used for testing the algorithm)")
  val YretainTrees: Setting[Boolean] = BooleanSetting("-Yretain-trees", "Retain trees for top-level classes, accessible from ClassSymbol#tree")
  val YshowTreeIds: Setting[Boolean] = BooleanSetting("-Yshow-tree-ids", "Uniquely tag all tree nodes in debugging output.")

  val YprofileEnabled: Setting[Boolean] = BooleanSetting("-Yprofile-enabled", "Enable profiling.")
  val YprofileDestination: Setting[String] = StringSetting("-Yprofile-destination", "file", "where to send profiling output - specify a file, default is to the console.", "")
      //.withPostSetHook( _ => YprofileEnabled.value = true )
  val YprofileExternalTool: Setting[List[String]] = PhasesSetting("-Yprofile-external-tool", "Enable profiling for a phase using an external tool hook. Generally only useful for a single phase", "typer")
      //.withPostSetHook( _ => YprofileEnabled.value = true )
  val YprofileRunGcBetweenPhases: Setting[List[String]] = PhasesSetting("-Yprofile-run-gc", "Run a GC between phases - this allows heap size to be accurate at the expense of more time. Specify a list of phases, or *", "_")
      //.withPostSetHook( _ => YprofileEnabled.value = true )

  // Extremely experimental language features
  val YkindPolymorphism: Setting[Boolean] = BooleanSetting("-Ykind-polymorphism", "Enable kind polymorphism (see http://dotty.epfl.ch/docs/reference/kind-polymorphism.html). Potentially unsound.")

  /** Area-specific debug output */
  val YexplainLowlevel: Setting[Boolean] = BooleanSetting("-Yexplain-lowlevel", "When explaining type errors, show types at a lower level.")
  val YnoDoubleBindings: Setting[Boolean] = BooleanSetting("-Yno-double-bindings", "Assert no namedtype is bound twice (should be enabled only if program is error-free).")
  val YshowVarBounds: Setting[Boolean] = BooleanSetting("-Yshow-var-bounds", "Print type variables with their bounds")
  val YshowNoInline: Setting[Boolean] = BooleanSetting("-Yshow-no-inline", "Show inlined code without the 'inlined from' info")

  val YnoDecodeStacktraces: Setting[Boolean] = BooleanSetting("-Yno-decode-stacktraces", "Show raw StackOverflow stacktraces, instead of decoding them into triggering operations.")

  /** Dottydoc specific settings */
  val siteRoot: Setting[String] = StringSetting(
    "-siteroot",
    "site root",
    "A directory containing static files from which to generate documentation",
    sys.props("user.dir")
  )


  val projectName: Setting[String] = StringSetting (
    "-project",
    "project title",
    "The name of the project",
    ""
  )

  val projectVersion: Setting[String] = StringSetting (
    "-project-version",
    "project version",
    "The current version of your project",
    ""
  )

  val projectUrl: Setting[String] = StringSetting (
    "-project-url",
    "project repository homepage",
    "The source repository of your project",
    ""
  )

  val wikiSyntax: Setting[Boolean] = BooleanSetting("-Xwiki-syntax", "Retains the Scala2 behavior of using Wiki Syntax in Scaladoc")
}
