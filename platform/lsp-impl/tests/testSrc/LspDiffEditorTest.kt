// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.platform.lsp

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffContext
import com.intellij.diff.FrameDiffTool
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.EDT
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.common.configureServerSession
import com.intellij.platform.lsp.common.fakeLspServerProviderFixture
import com.intellij.platform.lsp.impl.LspClientManagerImpl
import com.intellij.platform.lsp.impl.documentSync.LspOpenedFilesService
import com.intellij.platform.lsp.impl.platformListeners.LspDiffExtension
import com.intellij.platform.testFramework.junit5.codeInsight.fixture.codeInsightFixture
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@TestApplication
internal class LspDiffEditorTest {
  private val tempDir = tempPathFixture()
  private val projectFixture = projectFixture(tempDir, openAfterCreation = true)
  private val project by projectFixture

  @Suppress("unused")
  private val module = projectFixture.moduleFixture(tempDir, addPathToSourceRoot = true)
  private val fixture by codeInsightFixture(projectFixture, tempDir)

  @Suppress("unused")
  private val provider by projectFixture.fakeLspServerProviderFixture()

  @Test
  fun `keeps a file open until the last diff editor closes`(): Unit = checkLifecycle(openSource = false)

  @Test
  fun `closing a source editor keeps the diff file open`(): Unit = checkLifecycle(openSource = true)

  private fun checkLifecycle(openSource: Boolean): Unit = timeoutRunBlocking {
    val file = withContext(Dispatchers.EDT) { fixture.addFileToProject("current.txt", "current content").virtualFile }
    val viewers = withContext(Dispatchers.EDT) { List(2) { createViewer(file) } }
    try {
      val session = configureServerSession(project, file)
      val client = LspClientManagerImpl.getInstanceImpl(project).getClientsWithThisFileOpen(file).single()
      withContext(Dispatchers.EDT) {
        val manager = FileEditorManager.getInstance(project)
        if (openSource) {
          manager.openFile(file, true)
          manager.closeFile(file)
        }
        assertFalse(manager.isFileOpen(file))
        Disposer.dispose(viewers[0])
        assertTrue(client.isFileOpened(file))
        assertTrue(file in LspOpenedFilesService.getDiffFiles(project))
      }
      val closed = session.expectNotification(session.DID_CLOSE) { it.textDocument.uri == session.fileUri(file) }
      withContext(Dispatchers.EDT) { Disposer.dispose(viewers[1]) }
      withTimeout(10.seconds) { closed.await() }
      assertFalse(client.isFileOpened(file))
    }
    finally {
      withContext(Dispatchers.EDT) {
        viewers.filterNot { Disposer.isDisposed(it) }.forEach { Disposer.dispose(it) }
      }
    }
  }

  @Test
  fun `does not register a historical document as a workspace file`(): Unit = timeoutRunBlocking {
    withContext(Dispatchers.EDT) {
      val file = LightVirtualFile("historical.ts", "function previous() {}")
      val viewer = createViewer(file)
      try {
        assertTrue(LspOpenedFilesService.getDiffFiles(project).isEmpty())
        assertTrue(LspClientManagerImpl.getInstanceImpl(project).getRunningClients().isEmpty())
      }
      finally {
        Disposer.dispose(viewer)
      }
    }
  }

  private fun createViewer(file: VirtualFile): FrameDiffTool.DiffViewer {
    val factory = DiffContentFactory.getInstance()
    val request = SimpleDiffRequest(null, factory.create(project, "previous", file), factory.create(project, file), null, null)
    val context = object : DiffContext() {
      override fun getProject(): Project = this@LspDiffEditorTest.project
      override fun isWindowFocused(): Boolean = true
      override fun isFocusedInWindow(): Boolean = true
      override fun requestFocusInWindow() {}
    }
    val viewer = object : FrameDiffTool.DiffViewer {
      override fun getComponent(): JComponent = JPanel()
      override fun getPreferredFocusedComponent(): JComponent? = null
      override fun init(): FrameDiffTool.ToolbarComponents = FrameDiffTool.ToolbarComponents()
      override fun dispose() {}
    }
    LspDiffExtension().onViewerCreated(viewer, context, request)
    return viewer
  }
}
