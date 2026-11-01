// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.rebased.javascript

import com.intellij.platform.lsp.api.customization.LspCallHierarchyDisabled
import com.intellij.platform.lsp.api.customization.LspCodeActionsDisabled
import com.intellij.platform.lsp.api.customization.LspCodeLensDisabled
import com.intellij.platform.lsp.api.customization.LspCommandsDisabled
import com.intellij.platform.lsp.api.customization.LspCompletionDisabled
import com.intellij.platform.lsp.api.customization.LspCustomization
import com.intellij.platform.lsp.api.customization.LspDiagnosticsDisabled
import com.intellij.platform.lsp.api.customization.LspDocumentColorDisabled
import com.intellij.platform.lsp.api.customization.LspDocumentHighlightsDisabled
import com.intellij.platform.lsp.api.customization.LspDocumentLinkDisabled
import com.intellij.platform.lsp.api.customization.LspDocumentSymbolDisabled
import com.intellij.platform.lsp.api.customization.LspFindReferencesDisabled
import com.intellij.platform.lsp.api.customization.LspFoldingRangeDisabled
import com.intellij.platform.lsp.api.customization.LspFormattingDisabled
import com.intellij.platform.lsp.api.customization.LspHoverDisabled
import com.intellij.platform.lsp.api.customization.LspInlayHintDisabled
import com.intellij.platform.lsp.api.customization.LspOptimizeImportsDisabled
import com.intellij.platform.lsp.api.customization.LspRenameDisabled
import com.intellij.platform.lsp.api.customization.LspSelectionRangeDisabled
import com.intellij.platform.lsp.api.customization.LspSemanticTokensDisabled
import com.intellij.platform.lsp.api.customization.LspSignatureHelpDisabled
import com.intellij.platform.lsp.api.customization.LspTypeHierarchyDisabled
import com.intellij.platform.lsp.api.customization.LspWorkspaceSymbolDisabled

internal class JavaScriptNavigationCustomization : LspCustomization() {
  override val hoverCustomizer = LspHoverDisabled
  override val completionCustomizer = LspCompletionDisabled
  override val semanticTokensCustomizer = LspSemanticTokensDisabled
  override val diagnosticsCustomizer = LspDiagnosticsDisabled
  override val codeActionsCustomizer = LspCodeActionsDisabled
  override val commandsCustomizer = LspCommandsDisabled
  override val formattingCustomizer = LspFormattingDisabled
  override val findReferencesCustomizer = LspFindReferencesDisabled
  override val optimizeImportsCustomizer = LspOptimizeImportsDisabled
  override val documentColorCustomizer = LspDocumentColorDisabled
  override val documentLinkCustomizer = LspDocumentLinkDisabled
  override val foldingRangeCustomizer = LspFoldingRangeDisabled
  override val inlayHintCustomizer = LspInlayHintDisabled
  override val documentHighlightsCustomizer = LspDocumentHighlightsDisabled
  override val signatureHelpCustomizer = LspSignatureHelpDisabled
  override val documentSymbolCustomizer = LspDocumentSymbolDisabled
  override val workspaceSymbolCustomizer = LspWorkspaceSymbolDisabled
  override val callHierarchyCustomizer = LspCallHierarchyDisabled
  override val typeHierarchyCustomizer = LspTypeHierarchyDisabled
  override val selectionRangeCustomizer = LspSelectionRangeDisabled
  override val codeLensCustomizer = LspCodeLensDisabled
  override val renameCustomizer = LspRenameDisabled
}
