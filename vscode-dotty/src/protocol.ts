import * as vscode from 'vscode'
import { RequestType, NotificationType } from 'vscode-jsonrpc'
import { VersionedTextDocumentIdentifier, TextDocumentIdentifier } from 'vscode-languageserver-protocol'

import { client } from './extension'

/** The parameters for the `worksheet/run` request. */
export interface WorksheetRunParams {
  textDocument: VersionedTextDocumentIdentifier
}

/** The result of the `worksheet/run` request. */
export interface WorksheetRunResult {
  success: boolean
}

/** The parameters for the `worksheet/publishOutput` notification. */
export interface WorksheetPublishOutputParams {
  textDocument: VersionedTextDocumentIdentifier
  line: number
  content: string
}

/** The parameters for the `tasty/decompile` request. */
export interface TastyDecompileParams {
  textDocument: TextDocumentIdentifier
}

/** The result of the `tasty/decompile` request */
export interface TastyDecompileResult {
	tastyTree: string
	scala: string
	error: number
}

// TODO: Can be removed once https://github.com/Microsoft/vscode-languageserver-node/pull/421
// is merged.
export function asVersionedTextDocumentIdentifier(textDocument: vscode.TextDocument): VersionedTextDocumentIdentifier {
	return {
		uri: client.code2ProtocolConverter.asUri(textDocument.uri),
		version: textDocument.version
	}
}

export function asWorksheetRunParams(textDocument: vscode.TextDocument): WorksheetRunParams {
	return {
    textDocument: asVersionedTextDocumentIdentifier(textDocument)
	}
}


export function asTastyDecompileParams(textDocument: vscode.TextDocument): TastyDecompileParams {
	return {
		textDocument: client.code2ProtocolConverter.asTextDocumentIdentifier(textDocument)
	}
}

/** The `worksheet/run` request */
export namespace WorksheetRunRequest {
 export const type = new RequestType<WorksheetRunParams, WorksheetRunResult, void, void>("worksheet/run")
}

/** The `worksheet/publishOutput` notification */
export namespace WorksheetPublishOutputNotification {
	export const type = new NotificationType<WorksheetPublishOutputParams, void>("worksheet/publishOutput")
}

/** The `tasty/decompile` request */
export namespace TastyDecompileRequest {
	export const type = new RequestType<TastyDecompileParams, TastyDecompileResult, void, void>("tasty/decompile")
}
