// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.rebased.javascript

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspClient
import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.platform.lsp.api.ProjectWideLspClientDescriptor
import com.intellij.platform.lsp.api.lsWidget.LspClientWidgetItem
import java.nio.file.Path
import kotlin.io.path.isRegularFile

internal class JavaScriptLspIntegrationProvider : LspIntegrationProvider {
  override fun fileOpened(project: Project, file: VirtualFile, clientStarter: LspIntegrationProvider.LspClientStarter) {
    if (javaScriptLanguageId(file.extension) != null && project.service<JavaScriptSettings>().state.enabled) {
      clientStarter.ensureClientStarted(JavaScriptLspDescriptor(project))
    }
  }

  override fun createWidgetItem(lspClient: LspClient, currentFile: VirtualFile?): LspClientWidgetItem =
    LspClientWidgetItem(lspClient, currentFile, AllIcons.General.Language, JavaScriptConfigurable::class.java)
}

internal class JavaScriptLspDescriptor(project: Project) :
  ProjectWideLspClientDescriptor(project, JavaScriptBundle.message("settings.name")) {
  override fun isSupportedFile(file: VirtualFile): Boolean = javaScriptLanguageId(file.extension) != null

  override fun getLanguageId(file: VirtualFile): String = javaScriptLanguageId(file.extension).orEmpty()

  override val lspCustomization = JavaScriptNavigationCustomization()

  override fun createCommandLine(): GeneralCommandLine {
    val options = project.service<JavaScriptSettings>().state
    val node = options.nodePath?.takeIf { it.isNotBlank() }
               ?: PathEnvironmentVariableUtil.findFirst(if (SystemInfoRt.isWindows) "node.exe" else "node")?.toString()
               ?: throw ExecutionException(JavaScriptBundle.message("error.node.missing"))
    val version = CapturingProcessHandler(GeneralCommandLine(node, "--version")).runProcess(5_000)
    if (version.exitCode != 0 || !isSupportedNodeVersion(version.stdout.trim())) {
      throw ExecutionException(JavaScriptBundle.message("error.node.version", node))
    }
    val server = javaScriptServerDirectory().resolve("typescript-language-server/lib/cli.mjs")
    if (!server.isRegularFile()) {
      throw ExecutionException(JavaScriptBundle.message("error.server.missing", server))
    }
    return GeneralCommandLine(node, server.toString(), "--stdio")
      .withWorkDirectory(project.basePath)
  }

  override fun createInitializationOptions(): Any = mapOf(
    "hostInfo" to "Rebased",
    "disableAutomaticTypingAcquisition" to true,
    "tsserver" to mapOf(
      "fallbackPath" to javaScriptServerDirectory().resolve("typescript/lib/tsserver.js").toString(),
      "useSyntaxServer" to "never",
    ),
  )
}

internal fun javaScriptServerDirectory(): Path = Path.of(
  System.getProperty("rebased.javascript.server.path") ?: PathManager.getHomePath() + "/lib/rebased-javascript",
)

internal fun javaScriptLanguageId(extension: String?): String? = when (extension?.lowercase()) {
  "js", "mjs", "cjs" -> "javascript"
  "jsx" -> "javascriptreact"
  "ts", "mts", "cts" -> "typescript"
  "tsx" -> "typescriptreact"
  else -> null
}

internal fun isSupportedNodeVersion(version: String): Boolean {
  val parts = version.removePrefix("v").split('.').map { it.toIntOrNull() ?: return false }
  if (parts.size != 3) return false
  return compareValuesBy(parts, listOf(22, 22, 2), { it[0] }, { it[1] }, { it[2] }) >= 0
}
