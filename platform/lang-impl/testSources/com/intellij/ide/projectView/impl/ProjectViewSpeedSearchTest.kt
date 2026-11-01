// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.projectView.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.ProjectViewTestUtil
import com.intellij.testFramework.common.timeoutRunBlocking
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.TestDisposable
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.sourceRootFixture
import com.intellij.testFramework.junit5.fixture.virtualFileFixture
import com.intellij.ui.speedSearch.SpeedSearchSupply
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import javax.swing.JTree

@TestApplication
@Timeout(30)
internal class ProjectViewSpeedSearchTest {
  private companion object {
    val project = projectFixture()
    val sourceRoot = project.moduleFixture().sourceRootFixture()
  }

  private val file = sourceRoot.virtualFileFixture("time.js", "export const now = () => Date.now();")
  private val partial = sourceRoot.virtualFileFixture("runtime.js", "")

  @Test
  fun `finds an exact file inside a collapsed directory before a partial match`(): Unit = timeoutRunBlocking {
    partial.get()
    val expected = prepareFile()
    assertEquals(expected, readAction { findCollapsedProjectFile(project.get(), "time.js") })
  }

  @Test
  fun `matches a file name without case sensitivity`(): Unit = timeoutRunBlocking {
    val expected = prepareFile()
    assertEquals(expected, readAction { findCollapsedProjectFile(project.get(), "TIME.JS") })
  }

  @Test
  fun `keeps partial file name matching`(): Unit = timeoutRunBlocking {
    val expected = prepareFile()
    val other = partial.get()
    assertTrue(readAction { findCollapsedProjectFile(project.get(), "time") } in listOf(expected, other))
  }

  @Test
  fun `does not return a directory or a missing file`(): Unit = timeoutRunBlocking {
    prepareFile()
    assertNull(readAction { findCollapsedProjectFile(project.get(), "utils") })
    assertNull(readAction { findCollapsedProjectFile(project.get(), "missing.js") })
  }

  @Test
  fun `expands a cold project tree and selects the file instead of its parent`(@TestDisposable disposable: Disposable): Unit =
    timeoutRunBlocking {
      val target = prepareFile()
      withTree(disposable) { tree ->
        val selected = CompletableDeferred<Unit>()
        withContext(Dispatchers.EDT) {
          assertInstanceOf(ProjectViewSpeedSearch::class.java, SpeedSearchSupply.getSupply(tree, true))
          assertFalse((0 until tree.rowCount).any { tree.getPathForRow(it).lastPathComponent.toString() == "time.js" })
          revealProjectFile(tree, target, { true }) { selected.complete(Unit) }
        }
        selected.await()
        withContext(Dispatchers.EDT) {
          val path = tree.selectionPath!!
          assertEquals("time.js", path.lastPathComponent.toString())
          assertTrue(tree.isExpanded(path.parentPath))
          assertTrue(path.path.any { it.toString() == "utils" })
        }
      }
    }

  @Test
  fun `a cancelled search cannot change the selection after loading the path`(@TestDisposable disposable: Disposable): Unit =
    timeoutRunBlocking {
      val target = prepareFile()
      withTree(disposable) { tree ->
        val finished = CompletableDeferred<Unit>()
        var checks = 0
        withContext(Dispatchers.EDT) {
          tree.clearSelection()
          revealProjectFile(tree, target, {
            if (++checks == 1) true
            else {
              finished.complete(Unit)
              false
            }
          }) { throw AssertionError("The cancelled search selected a file") }
        }
        finished.await()
        withContext(Dispatchers.EDT) { assertNull(tree.selectionPath) }
      }
    }

  private suspend fun prepareFile(): VirtualFile {
    val target = file.get()
    val root = sourceRoot.get().virtualFile
    edtWriteAction { target.move(this, VfsUtil.createDirectoryIfMissing(root, "utils")) }
    return target
  }

  private suspend fun createTree(disposable: Disposable): JTree {
    val testProject = project.get()
    return withContext(Dispatchers.EDT) {
      ProjectViewTestUtil.setupImpl(testProject, false)
      val pane = ProjectViewPane(testProject)
      Disposer.register(disposable, pane)
      pane.createComponent()
      pane.tree
    }
  }

  private suspend fun withTree(disposable: Disposable, body: suspend (JTree) -> Unit) {
    val treeDisposable = Disposer.newDisposable(disposable)
    val tree = createTree(treeDisposable)
    try {
      body(tree)
    }
    finally {
      withContext(NonCancellable + Dispatchers.EDT) { Disposer.dispose(treeDisposable) }
    }
  }
}
