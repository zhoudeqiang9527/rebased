// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.rebased.javascript

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffContext
import com.intellij.diff.DiffExtension
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.tools.fragmented.UnifiedDiffViewer
import com.intellij.diff.tools.simple.SimpleDiffViewer
import com.intellij.diff.tools.util.base.ListenerDiffViewerBase
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Disposer
import com.intellij.platform.lsp.api.LspClientManager
import com.intellij.platform.lsp.api.LspIntegrationProvider
import com.intellij.platform.lsp.testFramework.awaitFileOpenedByLspServer
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.psi.PsiManager
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

/** Runs the actual bundled server and the platform Go to Declaration action, not a mock symbol resolver. */
@TestApplication
@Timeout(60)
internal class JavaScriptNavigationTest {
  private val tempDir = tempPathFixture()
  private val projectFixture = projectFixture(tempDir, openAfterCreation = true)

  @Suppress("unused")
  private val module = projectFixture.moduleFixture(tempDir, addPathToSourceRoot = true)

  private val fixture by codeInsightFixture(projectFixture, tempDir)
  private val project get() = projectFixture.get()

  @ParameterizedTest
  @ValueSource(strings = ["js", "ts", "jsx", "tsx", "mjs", "mts", "cjs", "cts"])
  fun `follows an aliased import to its function definition`(extension: String): Unit = timeoutRunBlocking(timeout = 45.seconds) {
    val importExtension = when (extension) {
      "mjs", "mts" -> ".mjs"
      "cjs", "cts" -> ".cjs"
      else -> ""
    }
    navigate(
      "import-$extension", "main.$extension", "import { greet as welcome } from './utils$importExtension';\nwel<caret>come('reader');\n",
      "utils.$extension", "export function <target>greet(name) { return 'Hello ' + name; }\n",
    )
  }

  @Test
  fun `resolves tsconfig path aliases and class methods`(): Unit = timeoutRunBlocking(timeout = 45.seconds) {
    navigate(
      "paths", "src/main.ts", "import { Greeter } from '@lib/utils';\nnew Greeter().hel<caret>lo();\n",
      "src/lib/utils.ts", "export class Greeter { <target>hello() { return 'hello'; } }\n",
      """{"compilerOptions":{"baseUrl":".","paths":{"@lib/*":["src/lib/*"]}}}""",
    )
  }

  @Test
  fun `resolves CommonJS require calls`(): Unit = timeoutRunBlocking(timeout = 45.seconds) {
    navigate(
      "commonjs", "main.js", "const { greet } = require('./utils');\ngre<caret>et();\n",
      "utils.js", "exports.<target>greet = function () { return 'hello'; };\n",
    )
  }

  @Test
  fun `uses unsaved editor text for definitions`(): Unit = timeoutRunBlocking(timeout = 45.seconds) {
    navigate(
      "unsaved", "main.ts", "import { greet } from './utils';\ngre<caret>et();\n",
      "utils.ts", "export function <target>greet() { return 'hello'; }\n",
      unsaved = true,
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["js", "ts"])
  fun `navigates from a diff without a source editor`(extension: String): Unit = timeoutRunBlocking(timeout = 45.seconds) {
    navigate(
      "diff", "main.$extension", "import { greet } from './utils';\ngre<caret>et();\n",
      "utils.$extension", "export function <target>greet() { return 'hello'; }\n", diff = true,
    )
  }

  @Test
  fun `restarts the server with only a diff open`(): Unit = timeoutRunBlocking(timeout = 45.seconds) {
    navigate(
      "diff-restart", "main.ts", "import { greet } from './utils';\ngre<caret>et();\n",
      "utils.ts", "export function <target>greet() { return 'hello'; }\n", diff = true, restart = true,
    )
  }

  @ParameterizedTest
  @ValueSource(strings = ["js", "ts"])
  fun `opens a unified diff before any source editor`(extension: String): Unit = timeoutRunBlocking(timeout = 45.seconds) {
    navigate(
      "unified", "main.$extension", "import { greet } from './utils';\ngre<caret>et();\n",
      "utils.$extension", "export function <target>greet() { return 'hello'; }\n", diff = true, unified = true,
    )
  }

  private suspend fun navigate(
    folder: String,
    sourceName: String,
    sourceText: String,
    targetName: String,
    targetText: String,
    config: String = """{"compilerOptions":{"allowJs":true,"jsx":"preserve","module":"nodenext","moduleResolution":"nodenext"}}""",
    unsaved: Boolean = false,
    diff: Boolean = false,
    restart: Boolean = false,
    unified: Boolean = false,
  ) {
    val expectedOffset = targetText.indexOf("<target>")
    val sourceOffset = sourceText.indexOf("<caret>")
    val sourceCode = sourceText.replace("<caret>", "")
    val (source, target) = withContext(Dispatchers.EDT) {
      fixture.addFileToProject("$folder/tsconfig.json", config)
      val target = fixture.addFileToProject("$folder/$targetName", targetText.replace("<target>", "")).virtualFile
      val source = fixture.addFileToProject("$folder/$sourceName", if (unsaved) "// saved file\n" else sourceCode).virtualFile
      if (!diff) fixture.configureFromExistingVirtualFile(source)
      source to target
    }
    assertTrue(source.isInLocalFileSystem, "The test file must use the local file system")
    assertTrue(readAction { ProjectFileIndex.getInstance(project).isInContent(source) }, "The test file must be in project content")
    assertTrue(LspIntegrationProvider.getAllExtensions().any { it is JavaScriptLspIntegrationProvider })
    val viewer = if (diff) withContext(Dispatchers.EDT) {
      assertFalse(FileEditorManager.getInstance(project).isFileOpen(source))
      val factory = DiffContentFactory.getInstance()
      val request = SimpleDiffRequest(null, factory.create(project, "// before\n", source), factory.create(project, source), null, null)
      val context = object : DiffContext() {
          override fun getProject(): Project = this@JavaScriptNavigationTest.project
        override fun isWindowFocused(): Boolean = true
        override fun isFocusedInWindow(): Boolean = true
        override fun requestFocusInWindow() {}
      }
      val result: ListenerDiffViewerBase = if (unified) UnifiedDiffViewer(context, request) else SimpleDiffViewer(context, request)
      DiffExtension.EP_NAME.extensionList.forEach { it.onViewerCreated(result, context, request) }
      result.init()
      result.rediff(true)
      result
    } else null
    try {
      awaitFileOpenedByLspServer(project, source)
      if (restart) {
        val manager = LspClientManager.getInstance(project)
        val previous = manager.getClients(JavaScriptLspIntegrationProvider::class.java).single()
        manager.stopAndRestartClientsIfNeeded(JavaScriptLspIntegrationProvider::class.java)
        withTimeout(15.seconds) {
          while (manager.getClients(JavaScriptLspIntegrationProvider::class.java).none { it !== previous }) delay(20)
        }
        awaitFileOpenedByLspServer(project, source)
      }
      if (unsaved) {
        edtWriteAction { fixture.editor.document.setText(sourceCode) }
        assertTrue(withContext(Dispatchers.EDT) { FileDocumentManager.getInstance().isDocumentUnsaved(fixture.editor.document) })
      }
      if (viewer != null) withTimeout(10.seconds) {
        while (withContext(Dispatchers.EDT) { viewer.hasPendingRediff() }) delay(20)
      }
      withContext(Dispatchers.EDT) {
        if (viewer == null || unified) {
          if (unified) fixture.configureFromExistingVirtualFile(source)
          fixture.editor.caretModel.moveToOffset(sourceOffset)
          fixture.performEditorAction("GotoDeclaration")
        }
        else {
          val diffEditor = (viewer as SimpleDiffViewer).editor2
          diffEditor.caretModel.moveToOffset(sourceOffset)
          val action = ActionManager.getInstance().getAction("GotoDeclaration")
          val context = SimpleDataContext.builder()
            .add(CommonDataKeys.PROJECT, project)
            .add(CommonDataKeys.EDITOR, diffEditor)
            .add(CommonDataKeys.VIRTUAL_FILE, source)
            .add(CommonDataKeys.PSI_FILE, PsiManager.getInstance(project).findFile(source))
            .build()
          ActionUtil.performAction(action, AnActionEvent.createEvent(action, context, null, ActionPlaces.UNKNOWN, ActionUiKind.NONE, null))
        }
      }
      withTimeout(15.seconds) {
        while (true) {
          val arrived = withContext(Dispatchers.EDT) {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor
            editor != null && FileDocumentManager.getInstance().getFile(editor.document) == target &&
            editor.caretModel.offset == expectedOffset
          }
          if (arrived) break
          delay(20)
        }
      }
    }
    finally {
      if (viewer != null) withContext(Dispatchers.EDT) { Disposer.dispose(viewer) }
    }
  }
}
