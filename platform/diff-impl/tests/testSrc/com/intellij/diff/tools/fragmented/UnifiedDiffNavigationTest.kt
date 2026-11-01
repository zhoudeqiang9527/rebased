// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.diff.tools.fragmented

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffContext
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.diff.util.Side
import com.intellij.openapi.application.EDT
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.sourceRootFixture
import com.intellij.testFramework.junit5.fixture.virtualFileFixture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout

@TestApplication
@Timeout(30)
internal class UnifiedDiffNavigationTest {
  private companion object {
    const val BEFORE = "header\nremoved()\n\tcall()\n"
    const val CURRENT = "header\n\tcall()\nadded()\n"
    val projectFixture = projectFixture()
    val sourceRoot = projectFixture.moduleFixture().sourceRootFixture()
  }

  private val currentFile = sourceRoot.virtualFileFixture("current.txt", CURRENT)

  @Test
  fun `maps unchanged and inserted lines to the source character offset`(): Unit = timeoutRunBlocking {
    withViewer { viewer ->
      for (name in listOf("call", "added")) {
        val offset = viewer.editor.document.text.indexOf(name) + 2
        val target = findUnifiedDiffNavigationTarget(viewer, offset)
        assertEquals(currentFile.get(), target?.file)
        assertEquals(CURRENT.indexOf(name) + 2, target?.offset)
      }
    }
  }

  @Test
  fun `does not map a deleted revision line to the workspace`(): Unit = timeoutRunBlocking {
    withViewer { viewer ->
      val offset = viewer.editor.document.text.indexOf("removed") + 2
      assertNull(findUnifiedDiffNavigationTarget(viewer, offset))
    }
  }

  @Test
  fun `maps the workspace on the left side of a reversed comparison`(): Unit = timeoutRunBlocking {
    withViewer(reverse = true) { viewer ->
      val offset = viewer.editor.document.text.indexOf("added") + 2
      val target = findUnifiedDiffNavigationTarget(viewer, offset)
      assertEquals(currentFile.get(), target?.file)
      assertEquals(CURRENT.indexOf("added") + 2, target?.offset)
    }
  }

  @Test
  fun `does not use a revision highlight file as a source document`(): Unit = timeoutRunBlocking {
    withViewer(historicalOnly = true) { viewer ->
      val offset = viewer.editor.document.text.indexOf("call") + 2
      assertNull(findUnifiedDiffNavigationTarget(viewer, offset))
    }
  }

  @Test
  fun `does not navigate before the first comparison`(): Unit = timeoutRunBlocking {
    withViewer(compare = false) { viewer ->
      assertNull(findUnifiedDiffNavigationTarget(viewer, 0))
    }
  }

  @Test
  fun `does not navigate with an outdated line mapping`(): Unit = timeoutRunBlocking {
    withViewer { viewer ->
      val offset = viewer.editor.document.text.indexOf("call") + 2
      WriteCommandAction.runWriteCommandAction(viewer.project) {
        viewer.getDocument(Side.RIGHT).insertString(0, "new line\n")
      }
      assertNull(findUnifiedDiffNavigationTarget(viewer, offset))
    }
  }

  private suspend fun withViewer(
    historicalOnly: Boolean = false,
    compare: Boolean = true,
    reverse: Boolean = false,
    body: (UnifiedDiffViewer) -> Unit,
  ) {
    val project = projectFixture.get()
    val file = currentFile.get()
    val viewer = withContext(Dispatchers.EDT) {
      val factory = DiffContentFactory.getInstance()
      val revision = factory.create(project, BEFORE, file)
      val current = if (historicalOnly) factory.create(project, CURRENT, file) else factory.create(project, file)
      val (left, right) = if (reverse) current to revision else revision to current
      val context = object : DiffContext() {
        override fun getProject(): Project = project
        override fun isWindowFocused(): Boolean = true
        override fun isFocusedInWindow(): Boolean = true
        override fun requestFocusInWindow() {}
      }
      UnifiedDiffViewer(context, SimpleDiffRequest(null, left, right, null, null)).also {
        if (compare) it.init()
      }
    }
    try {
      if (compare) {
        withContext(Dispatchers.EDT) { viewer.rediff(true) }
        withTimeout(10_000) {
          while (withContext(Dispatchers.EDT) { viewer.hasPendingRediff() }) {
            delay(10)
          }
        }
      }
      withContext(Dispatchers.EDT) { body(viewer) }
    }
    finally {
      withContext(NonCancellable + Dispatchers.EDT) { Disposer.dispose(viewer) }
    }
  }
}
