package dotty.tools
package languageserver

import java.net.URI
import java.io._
import java.nio.file._
import java.util.concurrent.{CompletableFuture, ConcurrentHashMap}
import java.util.function.Function

import com.fasterxml.jackson.databind.ObjectMapper

import org.eclipse.lsp4j

import scala.collection._
import scala.collection.JavaConverters._
import scala.util.control.NonFatal
import scala.io.Codec

import dotc._
import ast.{Trees, tpd}
import core._, core.Decorators.{sourcePos => _, _}
import Comments._, Constants._, Contexts._, Flags._, Names._, NameOps._, Symbols._, SymDenotations._, Trees._, Types._
import classpath.ClassPathEntries
import reporting._, reporting.diagnostic.{Message, MessageContainer, messages}
import typer.Typer
import util.{Set => _, _}
import interactive._, interactive.InteractiveDriver._
import Interactive.Include
import config.Printers.interactiv

import languageserver.config.ProjectConfig
import languageserver.worksheet.{Worksheet, WorksheetClient, WorksheetService}

import lsp4j.services._

/** An implementation of the Language Server Protocol for Dotty.
 *
 *  You should not have to directly this class, instead see `dotty.tools.languageserver.Main`.
 *
 *  For more information see:
 *  - The LSP is defined at https://github.com/Microsoft/language-server-protocol
 *  - This implementation is based on the LSP4J library: https://github.com/eclipse/lsp4j
 */
class DottyLanguageServer extends LanguageServer
    with TextDocumentService with WorkspaceService with WorksheetService { thisServer =>
  import ast.tpd._

  import DottyLanguageServer._
  import InteractiveDriver._

  import lsp4j.jsonrpc.{CancelChecker, CompletableFutures}
  import lsp4j.jsonrpc.messages.{Either => JEither}
  import lsp4j._


  private[this] var rootUri: String = _

  private[this] var myClient: WorksheetClient = _
  def client: WorksheetClient = myClient

  private[this] var myDrivers: mutable.Map[ProjectConfig, InteractiveDriver] = _

  private[this] var myDependentProjects: mutable.Map[ProjectConfig, mutable.Set[ProjectConfig]] = _

  def drivers: Map[ProjectConfig, InteractiveDriver] = thisServer.synchronized {
    if (myDrivers == null) {
      assert(rootUri != null, "`drivers` cannot be called before `initialize`")
      val configFile = new File(new URI(rootUri + '/' + IDE_CONFIG_FILE))
      val configs: List[ProjectConfig] = (new ObjectMapper).readValue(configFile, classOf[Array[ProjectConfig]]).toList

      val defaultFlags = List("-color:never" /*, "-Yplain-printer","-Yprint-pos"*/)

      myDrivers = new mutable.HashMap
      for (config <- configs) {
        implicit class updateDeco(ss: List[String]) {
          def update(pathKind: String, pathInfo: String) = {
            val idx = ss.indexOf(pathKind)
            val ss1 = if (idx >= 0) ss.take(idx) ++ ss.drop(idx + 2) else ss
            ss1 ++ List(pathKind, pathInfo)
          }
        }
        val settings =
          defaultFlags ++
          config.compilerArguments.toList
            .update("-d", config.classDirectory.getAbsolutePath)
            .update("-classpath", (config.classDirectory +: config.dependencyClasspath).mkString(File.pathSeparator))
            .update("-sourcepath", config.sourceDirectories.mkString(File.pathSeparator)) :+
          "-scansource"
        myDrivers(config) = new InteractiveDriver(settings)
      }
    }
    myDrivers
  }

  /** Restart all presentation compiler drivers, copying open files over */
  private def restart() = thisServer.synchronized {
    interactiv.println("restarting presentation compiler")
    val driverConfigs = for ((config, driver) <- myDrivers.toList) yield
      (config, new InteractiveDriver(driver.settings), driver.openedFiles)
    for ((config, driver, _) <- driverConfigs)
      myDrivers(config) = driver
    System.gc()
    for ((_, driver, opened) <- driverConfigs; (uri, source) <- opened)
      driver.run(uri, source)
    if (Memory.isCritical())
      println(s"WARNING: Insufficient memory to run Scala language server on these projects.")
  }

  private def checkMemory() =
    if (Memory.isCritical()) CompletableFutures.computeAsync { _ => restart() }

  /** The configuration of the project that owns `uri`. */
  def configFor(uri: URI): ProjectConfig = thisServer.synchronized {
    val config =
      drivers.keys.find(config => config.sourceDirectories.exists(sourceDir =>
        new File(uri.getPath).getCanonicalPath.startsWith(sourceDir.getCanonicalPath)))

    config.getOrElse {
      val config = drivers.keys.head
      // println(s"No configuration contains $uri as a source file, arbitrarily choosing ${config.id}")
      config
    }
  }

  /** The driver instance responsible for compiling `uri` */
  def driverFor(uri: URI): InteractiveDriver = {
    drivers(configFor(uri))
  }

  /** A mapping from project `p` to the set of projects that transitively depend on `p`. */
  def dependentProjects: Map[ProjectConfig, Set[ProjectConfig]] = thisServer.synchronized {
    if (myDependentProjects == null) {
      val idToConfig = drivers.keys.map(k => k.id -> k).toMap
      val allProjects = drivers.keySet

      def transitiveDependencies(config: ProjectConfig): Set[ProjectConfig] = {
        val dependencies = config.projectDependencies.map(idToConfig).toSet
        dependencies ++ dependencies.flatMap(transitiveDependencies)
      }

      myDependentProjects = new mutable.HashMap().withDefaultValue(mutable.Set.empty)
      for { project <- allProjects
            dependency <- transitiveDependencies(project) } {
        myDependentProjects(dependency) += project
      }
    }
    myDependentProjects
  }

  def connect(client: WorksheetClient): Unit = {
    myClient = client
  }

  override def exit(): Unit = {
    System.exit(0)
  }

  override def shutdown(): CompletableFuture[Object] = {
    CompletableFuture.completedFuture(new Object)
  }

  def computeAsync[R](fun: CancelChecker => R, synchronize: Boolean = true): CompletableFuture[R] =
    CompletableFutures.computeAsync { cancelToken =>
      // We do not support any concurrent use of the compiler currently.
      def computation(): R = {
        cancelToken.checkCanceled()
        checkMemory()
        try {
          fun(cancelToken)
        } catch {
          case NonFatal(ex) =>
            ex.printStackTrace
            throw ex
        }
      }
      if (synchronize)
        thisServer.synchronized { computation() }
      else
        computation()
    }

  override def initialize(params: InitializeParams) = computeAsync { cancelToken =>
    rootUri = params.getRootUri
    assert(rootUri != null)

    class DottyServerCapabilities(val worksheetRunProvider: Boolean = true) extends lsp4j.ServerCapabilities

    val c = new DottyServerCapabilities
    c.setTextDocumentSync(TextDocumentSyncKind.Full)
    c.setDocumentHighlightProvider(true)
    c.setDocumentSymbolProvider(true)
    c.setDefinitionProvider(true)
    c.setRenameProvider(true)
    c.setHoverProvider(true)
    c.setWorkspaceSymbolProvider(true)
    c.setReferencesProvider(true)
    c.setImplementationProvider(true)
    c.setCompletionProvider(new CompletionOptions(
      /* resolveProvider = */ false,
      /* triggerCharacters = */ List(".").asJava))
    c.setSignatureHelpProvider(new SignatureHelpOptions(
      /* triggerCharacters = */ List("(").asJava))

    // Do most of the initialization asynchronously so that we can return early
    // from this method and thus let the client know our capabilities.
    CompletableFuture.supplyAsync(() => drivers)
      .exceptionally { (ex: Throwable) =>
        ex.printStackTrace
        sys.exit(1)
      }

    new InitializeResult(c)
  }

  override def didOpen(params: DidOpenTextDocumentParams): Unit = thisServer.synchronized {
    checkMemory()
    val document = params.getTextDocument
    val uri = new URI(document.getUri)
    val driver = driverFor(uri)
    val worksheetMode = isWorksheet(uri)

    val (text, positionMapper) =
      if (worksheetMode) (wrapWorksheet(document.getText), Some(toUnwrappedPosition _))
      else (document.getText, None)

    val diags = driver.run(uri, text)

    client.publishDiagnostics(new PublishDiagnosticsParams(
      document.getUri,
      diags.flatMap(diagnostic(_, positionMapper)(driver.currentCtx)).asJava))
  }

  override def didChange(params: DidChangeTextDocumentParams): Unit = {
    val document = params.getTextDocument
    val uri = new URI(document.getUri)
    val worksheetMode = isWorksheet(uri)

    thisServer.synchronized  {
      checkMemory()

      val driver = driverFor(uri)

      val change = params.getContentChanges.get(0)
      assert(change.getRange == null, "TextDocumentSyncKind.Incremental support is not implemented")

      val (text, positionMapper) =
        if (worksheetMode) (wrapWorksheet(change.getText), Some(toUnwrappedPosition _))
        else (change.getText, None)

      val diags = driver.run(uri, text)

      client.publishDiagnostics(new PublishDiagnosticsParams(
        document.getUri,
        diags.flatMap(diagnostic(_, positionMapper)(driver.currentCtx)).asJava))
    }
  }

  override def didClose(params: DidCloseTextDocumentParams): Unit = thisServer.synchronized {
    val document = params.getTextDocument
    val uri = new URI(document.getUri)

    driverFor(uri).close(uri)
  }

  override def didChangeConfiguration(params: DidChangeConfigurationParams): Unit =
    /*thisServer.synchronized*/ {}

  override def didChangeWatchedFiles(params: DidChangeWatchedFilesParams): Unit =
    /*thisServer.synchronized*/ {}

  override def didSave(params: DidSaveTextDocumentParams): Unit = {
    /*thisServer.synchronized*/ {}
  }

  // FIXME: share code with messages.NotAMember
  override def completion(params: CompletionParams) = computeAsync { cancelToken =>
    val uri = new URI(params.getTextDocument.getUri)
    val driver = driverFor(uri)
    implicit val ctx = driver.currentCtx

    val pos = sourcePosition(driver, uri, params.getPosition)
    val items = driver.compilationUnits.get(uri) match {
      case Some(unit) => Interactive.completions(pos)(ctx.fresh.setCompilationUnit(unit))._2
      case None => Nil
    }

    JEither.forRight(new CompletionList(
      /*isIncomplete = */ false, items.map(completionItem).asJava))
  }

  /** If cursor is on a reference, show its definition and all overriding definitions in
   *  the same source as the primary definition.
   *  If cursor is on a definition, show this definition together with all overridden
   *  and overriding definitions (in all sources).
   */
  override def definition(params: TextDocumentPositionParams) = computeAsync { cancelToken =>
    val uri = new URI(params.getTextDocument.getUri)
    val driver = driverFor(uri)
    implicit val ctx = driver.currentCtx

    val pos = sourcePosition(driver, uri, params.getPosition)
    val path = Interactive.pathTo(driver.openedTrees(uri), pos)

    val definitions = Interactive.findDefinitions(path, driver).toList
    definitions.flatMap(d => location(d.namePos, positionMapperFor(d.source))).asJava
  }

  override def references(params: ReferenceParams) = computeAsync { cancelToken =>
    val uri = new URI(params.getTextDocument.getUri)
    val driver = driverFor(uri)

    val includes = {
      val includeDeclaration = params.getContext.isIncludeDeclaration
      Include.references | Include.overriding | (if (includeDeclaration) Include.definitions else 0)
    }

    val pos = sourcePosition(driver, uri, params.getPosition)

    val (definitions, originalSymbol, originalSymbolName) = {
      implicit val ctx: Context = driver.currentCtx
      val path = Interactive.pathTo(driver.openedTrees(uri), pos)
      val originalSymbol = Interactive.enclosingSourceSymbol(path)
      val originalSymbolName = originalSymbol.name.sourceModuleName.toString
      val definitions = Interactive.findDefinitions(path, driver)

      (definitions, originalSymbol, originalSymbolName)
    }

    val references = {
      // Collect the information necessary to look into each project separately: representation of
      // `originalSymbol` in this project, the context and correct Driver.
      val perProjectInfo = inProjectsSeeing(driver, definitions, originalSymbol)

      perProjectInfo.flatMap { (remoteDriver, ctx, definition) =>
        val trees = remoteDriver.sourceTreesContaining(originalSymbolName)(ctx)
        val matches = Interactive.findTreesMatching(trees, includes, definition)(ctx)
        matches.map(tree => location(tree.namePos(ctx), positionMapperFor(tree.source)))
      }
    }.toList

    references.flatten.asJava
  }

  override def rename(params: RenameParams) = computeAsync { cancelToken =>
    val uri = new URI(params.getTextDocument.getUri)
    val driver = driverFor(uri)
    implicit val ctx = driver.currentCtx

    val pos = sourcePosition(driver, uri, params.getPosition)
    val sym = Interactive.enclosingSourceSymbol(driver.openedTrees(uri), pos)

    if (sym == NoSymbol) new WorkspaceEdit()
    else {
      val trees = driver.allTreesContaining(sym.name.sourceModuleName.toString)
      val newName = params.getNewName
      val includes =
        Include.references | Include.definitions | Include.linkedClass | Include.overriding
      val refs = Interactive.findTreesMatching(trees, includes, sym)

      val changes = refs.groupBy(ref => toUriOption(ref.source))
        .flatMap((uriOpt, ref) => uriOpt.map(uri => (uri.toString, ref)))
        .mapValues(refs =>
          refs.flatMap(ref =>
            range(ref.namePos, positionMapperFor(ref.source)).map(nameRange => new TextEdit(nameRange, newName))).asJava)

      new WorkspaceEdit(changes.asJava)
    }
  }

  override def documentHighlight(params: TextDocumentPositionParams) = computeAsync { cancelToken =>
    val uri = new URI(params.getTextDocument.getUri)
    val driver = driverFor(uri)
    implicit val ctx = driver.currentCtx

    val pos = sourcePosition(driver, uri, params.getPosition)
    val uriTrees = driver.openedTrees(uri)
    val sym = Interactive.enclosingSourceSymbol(uriTrees, pos)

    if (sym == NoSymbol) Nil.asJava
    else {
      val refs = Interactive.namedTrees(uriTrees, Include.references | Include.overriding, sym)
      (for {
        ref <- refs if !ref.tree.symbol.isPrimaryConstructor
        nameRange <- range(ref.namePos, positionMapperFor(ref.source))
      } yield new DocumentHighlight(nameRange, DocumentHighlightKind.Read)).asJava
    }
  }

  override def hover(params: TextDocumentPositionParams) = computeAsync { cancelToken =>
    val uri = new URI(params.getTextDocument.getUri)
    val driver = driverFor(uri)
    implicit val ctx = driver.currentCtx

    val pos = sourcePosition(driver, uri, params.getPosition)
    val trees = driver.openedTrees(uri)
    val tp = Interactive.enclosingType(trees, pos)
    val tpw = tp.widenTermRefExpr

    if (tp.isError || tpw == NoType) null // null here indicates that no response should be sent
    else {
      val symbol = Interactive.enclosingSourceSymbol(trees, pos)
      val docComment = ParsedComment.docOf(symbol)
      val content = hoverContent(Some(tpw.show), docComment)
      new Hover(content, null)
    }
  }

  override def documentSymbol(params: DocumentSymbolParams) = computeAsync { cancelToken =>
    val uri = new URI(params.getTextDocument.getUri)
    val driver = driverFor(uri)
    implicit val ctx = driver.currentCtx

    val uriTrees = driver.openedTrees(uri)

    val defs = Interactive.namedTrees(uriTrees, includeReferences = false, _ => true)
    (for {
      d <- defs if !isWorksheetWrapper(d)
      info <- symbolInfo(d.tree.symbol, d.namePos, positionMapperFor(d.source))
    } yield JEither.forLeft(info)).asJava
  }

  override def symbol(params: WorkspaceSymbolParams) = computeAsync { cancelToken =>
    val query = params.getQuery

    drivers.values.toList.flatMap { driver =>
      implicit val ctx = driver.currentCtx

      val trees = driver.allTrees
      val defs = Interactive.namedTrees(trees, nameSubstring = query)
      defs.flatMap(d => symbolInfo(d.tree.symbol, d.namePos, positionMapperFor(d.source)))
    }.asJava
  }

  override def implementation(params: TextDocumentPositionParams) = computeAsync { cancelToken =>
    val uri = new URI(params.getTextDocument.getUri)
    val driver = driverFor(uri)

    val pos = sourcePosition(driver, uri, params.getPosition)

    val (definitions, originalSymbol) = {
      implicit val ctx: Context = driver.currentCtx
      val path = Interactive.pathTo(driver.openedTrees(uri), pos)
      val originalSymbol = Interactive.enclosingSourceSymbol(path)
      val definitions = Interactive.findDefinitions(path, driver)
      (definitions, originalSymbol)
    }

    val implementations = {
      val perProjectInfo = inProjectsSeeing(driver, definitions, originalSymbol)

      perProjectInfo.flatMap { (remoteDriver, ctx, definition) =>
        val trees = remoteDriver.sourceTrees(ctx)
        val predicate = Interactive.implementationFilter(definition)(ctx)
        val matches = Interactive.namedTrees(trees, includeReferences = false, predicate)(ctx)
        matches.map(tree => location(tree.namePos(ctx), positionMapperFor(tree.source)))
      }
    }.toList

    implementations.flatten.asJava
  }

  override def signatureHelp(params: TextDocumentPositionParams) = computeAsync { canceltoken =>

    val uri = new URI(params.getTextDocument.getUri)
    val driver = driverFor(uri)
    implicit val ctx = driver.currentCtx

    val pos = sourcePosition(driver, uri, params.getPosition)
    val trees = driver.openedTrees(uri)
    val path = Interactive.pathTo(trees, pos).dropWhile(!_.isInstanceOf[Apply])

    val (paramN, callableN, alternatives) = Signatures.callInfo(path, pos.pos)
    val signatureInfos = alternatives.flatMap(Signatures.toSignature)

    new SignatureHelp(signatureInfos.map(signatureToSignatureInformation).asJava, callableN, paramN)
  }

  override def getTextDocumentService: TextDocumentService = this
  override def getWorkspaceService: WorkspaceService = this

  // Unimplemented features. If you implement one of them, you may need to add a
  // capability in `initialize`
  override def codeAction(params: CodeActionParams) = null
  override def codeLens(params: CodeLensParams) = null
  override def formatting(params: DocumentFormattingParams) = null
  override def rangeFormatting(params: DocumentRangeFormattingParams) = null
  override def onTypeFormatting(params: DocumentOnTypeFormattingParams) = null
  override def resolveCodeLens(params: CodeLens) = null
  override def resolveCompletionItem(params: CompletionItem) = null

  /**
   * Find the set of projects that have any of `definitions` on their classpath.
   *
   * @param definitions The definitions to consider when looking for projects.
   * @return The set of projects that have any of `definitions` on their classpath.
   */
  private def projectsSeeing(definitions: List[SourceTree])(implicit ctx: Context): Set[ProjectConfig] = {
    if (definitions.isEmpty) {
      drivers.keySet
    } else {
      for {
        definition <- definitions.toSet
        uri <- toUriOption(definition.pos.source).toSet
        config = configFor(uri)
        project <- dependentProjects(config) + config
      } yield project
    }
  }

  /**
   * Finds projects that can see any of `definitions`, translate `symbol` in their universe.
   *
   * @param baseDriver  The driver responsible for the trees in `definitions` and `symbol`.
   * @param definitions The definitions to consider when looking for projects.
   * @param symbol      A symbol to translate in the universes of the remote projects.
   * @return A list consisting of the remote drivers, their context, and the translation of `symbol`
   *         into their universe.
   */
  private def inProjectsSeeing(baseDriver: InteractiveDriver,
                               definitions: List[SourceTree],
                               symbol: Symbol): List[(InteractiveDriver, Context, Symbol)] = {
    val projects = projectsSeeing(definitions)(baseDriver.currentCtx)
    projects.toList.map { config =>
      val remoteDriver = drivers(config)
      val ctx = remoteDriver.currentCtx
      val definition = Interactive.localize(symbol, baseDriver, remoteDriver)
      (remoteDriver, ctx, definition)
    }
  }

}

object DottyLanguageServer {
  /** Configuration file normally generated by sbt-dotty */
  final val IDE_CONFIG_FILE = ".dotty-ide.json"

  /** Convert an lsp4j.Position to a SourcePosition */
  def sourcePosition(driver: InteractiveDriver, uri: URI, pos: lsp4j.Position): SourcePosition = {
    val actualPosition =
      if (isWorksheet(uri)) toWrappedPosition(pos)
      else pos
    val source = driver.openedFiles(uri)
    if (source.exists) {
      val p = Positions.Position(source.lineToOffset(actualPosition.getLine) + actualPosition.getCharacter)
      new SourcePosition(source, p)
    }
    else NoSourcePosition
  }

  /** Convert a SourcePosition to an lsp4j.Range */
  def range(p: SourcePosition, positionMapper: Option[SourcePosition => SourcePosition] = None): Option[lsp4j.Range] =
    if (p.exists) {
      val mappedPosition = positionMapper.map(_(p)).getOrElse(p)
      Some(new lsp4j.Range(
        new lsp4j.Position(mappedPosition.startLine, mappedPosition.startColumn),
        new lsp4j.Position(mappedPosition.endLine, mappedPosition.endColumn)
      ))
    } else
      None

  /** Convert a SourcePosition to an lsp4.Location */
  def location(p: SourcePosition, positionMapper: Option[SourcePosition => SourcePosition] = None): Option[lsp4j.Location] =
    for {
      uri <- toUriOption(p.source)
      r <- range(p, positionMapper)
    } yield new lsp4j.Location(uri.toString, r)

  /**
   * Convert a MessageContainer to an lsp4j.Diagnostic. The positions are transformed vy
   * `positionMapper`.
   */
  def diagnostic(mc: MessageContainer,
                 positionMapper: Option[SourcePosition => SourcePosition] = None
                )(implicit ctx: Context): Option[lsp4j.Diagnostic] =
    if (!mc.pos.exists)
      None // diagnostics without positions are not supported: https://github.com/Microsoft/language-server-protocol/issues/249
    else {
      def severity(level: Int): lsp4j.DiagnosticSeverity = {
        import interfaces.{Diagnostic => D}
        import lsp4j.{DiagnosticSeverity => DS}

        level match {
          case D.INFO =>
            DS.Information
          case D.WARNING =>
            DS.Warning
          case D.ERROR =>
            DS.Error
        }
      }

      val message = mc.contained()
      if (displayMessage(message, mc.pos.source)) {
        val code = message.errorId.errorNumber.toString
        range(mc.pos, positionMapper).map(r =>
            new lsp4j.Diagnostic(
              r, mc.message, severity(mc.level), /*source =*/ "", code))
      } else {
        None
      }
    }

  /**
   * Check whether `message` should be displayed in the IDE.
   *
   * Currently we only filter out the warning about pure expressions in statement position when they
   * are immediate children of the worksheet wrapper.
   *
   * @param message    The message to filter.
   * @param sourceFile The sourcefile from which `message` originates.
   * @return true if the message should be displayed in the IDE, false otherwise.
   */
  private def displayMessage(message: Message, sourceFile: SourceFile)(implicit ctx: Context): Boolean = {
    if (isWorksheet(sourceFile)) {
      message match {
        case messages.PureExpressionInStatementPosition(_, exprOwner) =>
          val ownerSym = if (exprOwner.isLocalDummy) exprOwner.owner else exprOwner
          !isWorksheetWrapper(ownerSym)
        case _ =>
          true
      }
    } else {
      true
    }
  }

  /** Does this URI represent a worksheet? */
  private def isWorksheet(uri: URI): Boolean =
    uri.toString.endsWith(".sc")

  /** Does this sourcefile represent a worksheet? */
  private def isWorksheet(sourcefile: SourceFile): Boolean =
    sourcefile.file.extension == "sc"

  /** Wrap the source of a worksheet inside an `object`. */
  private def wrapWorksheet(source: String): String =
    s"""object ${StdNames.nme.WorksheetWrapper} {
       |$source
       |}""".stripMargin

  /**
   * Map `position` in a wrapped worksheet to the same position in the unwrapped source.
   *
   * Because worksheet are wrapped in an `object`, the positions in the source are one line
   * above from what the compiler sees.
   *
   * @see wrapWorksheet
   * @param position The position as seen by the compiler (after wrapping)
   * @return The position in the actual source file (before wrapping).
   */
  private def toUnwrappedPosition(position: SourcePosition): SourcePosition = {
    new SourcePosition(position.source, position.pos, position.outer) {
      override def startLine: Int = position.startLine - 1
      override def endLine: Int = position.endLine - 1
    }
  }

  /**
   * Map `position` in an unwrapped worksheet to the same position in the wrapped source.
   *
   * Because worksheet are wrapped in an `object`, the positions in the source are one line
   * above from what the compiler sees.
   *
   * @see wrapWorksheet
   * @param position The position as seen by VSCode (before wrapping)
   * @return The position as seen by the compiler (after wrapping)
   */
  private def toWrappedPosition(position: lsp4j.Position): lsp4j.Position = {
    new lsp4j.Position(position.getLine + 1, position.getCharacter)
  }

  /**
   * Returns the position mapper necessary to unwrap positions for `sourcefile`. If `sourcefile` is
   * not a worksheet, no mapper is necessary. Otherwise, return `toUnwrappedPosition`.
   */
  private def positionMapperFor(sourcefile: SourceFile): Option[SourcePosition => SourcePosition] = {
    if (isWorksheet(sourcefile)) Some(toUnwrappedPosition _)
    else None
  }

  /**
   * Is `sourceTree` the wrapper object that we put around worksheet sources?
   *
   * @see wrapWorksheet
   */
  def isWorksheetWrapper(sourceTree: SourceTree)(implicit ctx: Context): Boolean = {
    isWorksheet(sourceTree.source) && isWorksheetWrapper(sourceTree.tree.symbol)
  }

  /**
   * Is this symbol the wrapper object that we put around worksheet sources?
   *
   * @see wrapWorksheet
   */
  def isWorksheetWrapper(symbol: Symbol)(implicit ctx: Context): Boolean = {
      symbol.name == StdNames.nme.WorksheetWrapper.moduleClassName &&
      symbol.owner == ctx.definitions.EmptyPackageClass
  }

  /** Create an lsp4j.CompletionItem from a Symbol */
  def completionItem(sym: Symbol)(implicit ctx: Context): lsp4j.CompletionItem = {
    def completionItemKind(sym: Symbol)(implicit ctx: Context): lsp4j.CompletionItemKind = {
      import lsp4j.{CompletionItemKind => CIK}

      if (sym.is(Package))
        CIK.Module // No CompletionItemKind.Package (https://github.com/Microsoft/language-server-protocol/issues/155)
      else if (sym.isConstructor)
        CIK.Constructor
      else if (sym.isClass)
        CIK.Class
      else if (sym.is(Mutable))
        CIK.Variable
      else if (sym.is(Method))
        CIK.Method
      else
        CIK.Field
    }

    val label = sym.name.show
    val item = new lsp4j.CompletionItem(label)
    item.setDetail(sym.info.widenTermRefExpr.show)
    item.setKind(completionItemKind(sym))
    item
  }

  def hoverContent(content: String): lsp4j.MarkupContent = {
    if (content.isEmpty) null
    else {
      val markup = new lsp4j.MarkupContent
      markup.setKind("markdown")
      markup.setValue(content.trim)
      markup
    }
  }

  private def hoverContent(typeInfo: Option[String],
                           comment: Option[ParsedComment]
                          )(implicit ctx: Context): lsp4j.MarkupContent = {
    val buf = new StringBuilder
    typeInfo.foreach { info =>
      buf.append(s"""```scala
                    |$info
                    |```
                    |""".stripMargin)
    }

    comment.foreach { comment =>
      buf.append(comment.renderAsMarkdown)
    }

    hoverContent(buf.toString)
  }

  /** Create an lsp4j.SymbolInfo from a Symbol and a SourcePosition */
  def symbolInfo(sym: Symbol, pos: SourcePosition, positionMapper: Option[SourcePosition => SourcePosition])(implicit ctx: Context): Option[lsp4j.SymbolInformation] = {
    def symbolKind(sym: Symbol)(implicit ctx: Context): lsp4j.SymbolKind = {
      import lsp4j.{SymbolKind => SK}

      if (sym.is(Package))
        SK.Package
      else if (sym.isConstructor)
        SK.Constructor
      else if (sym.is(Module))
        SK.Module
      else if (sym.isClass)
        SK.Class
      else if (sym.is(Mutable))
        SK.Variable
      else if (sym.is(Method))
        SK.Method
      else
        SK.Field
    }

    val name = sym.name.stripModuleClassSuffix.show
    val containerName =
      if (sym.owner.exists && !sym.owner.isEmptyPackage)
        sym.owner.name.show
      else
        null

    location(pos, positionMapper).map(l => new lsp4j.SymbolInformation(name, symbolKind(sym), l, containerName))
  }

  /** Convert `signature` to a `SignatureInformation` */
  def signatureToSignatureInformation(signature: Signatures.Signature): lsp4j.SignatureInformation = {
    val paramInfoss = signature.paramss.map(_.map(paramToParameterInformation))
    val paramLists = signature.paramss.map { paramList =>
      val labels = paramList.map(_.show)
      val prefix = if (paramList.exists(_.isImplicit)) "implicit " else ""
      labels.mkString(prefix, ", ", "")
    }.mkString("(", ")(", ")")
    val tparamsLabel = if (signature.tparams.isEmpty) "" else signature.tparams.mkString("[", ", ", "]")
    val returnTypeLabel = signature.returnType.map(t => s": $t").getOrElse("")
    val label = s"${signature.name}$tparamsLabel$paramLists$returnTypeLabel"
    val documentation = signature.doc.map(DottyLanguageServer.hoverContent)
    val sig = new lsp4j.SignatureInformation(label)
    sig.setParameters(paramInfoss.flatten.asJava)
    documentation.foreach(sig.setDocumentation(_))
    sig
  }

  /** Convert `param` to `ParameterInformation` */
  private def paramToParameterInformation(param: Signatures.Param): lsp4j.ParameterInformation = {
    val documentation = param.doc.map(DottyLanguageServer.hoverContent)
    val info = new lsp4j.ParameterInformation(param.show)
    documentation.foreach(info.setDocumentation(_))
    info
  }
}
