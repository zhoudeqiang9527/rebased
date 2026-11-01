// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.projectView.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.SpeedSearchComparator
import com.intellij.ui.TreeSpeedSearch
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.tree.TreeUtil
import javax.swing.JTree

internal class ProjectViewSpeedSearch(
  tree: JTree,
  private val project: Project,
  private val parentDisposable: Disposable,
) : TreeSpeedSearch(tree, false, null, NODE_PRESENTATION_FUNCTION) {
  @Volatile private var searchPattern: String? = null

  init {
    addChangeListener {
      val pattern = enteredPrefix
      searchPattern = pattern
      if (pattern != null) searchCollapsedDirectories(pattern)
    }
  }

  private fun searchCollapsedDirectories(pattern: String) {
    if (pattern.isBlank() || super.findElement(pattern) != null) return

    ReadAction.nonBlocking<VirtualFile?> { findCollapsedProjectFile(project, pattern) }
      .coalesceBy(this)
      .expireWith(parentDisposable)
      .expireWhen { searchPattern != pattern }
      .finishOnUiThread(ModalityState.any()) { file ->
        if (file != null && enteredPrefix == pattern) {
          revealProjectFile(myComponent, file, { enteredPrefix == pattern }) { refreshSelection() }
        }
      }
      .submit(AppExecutorUtil.getAppExecutorService())
  }
}

internal fun findCollapsedProjectFile(project: Project, pattern: String): VirtualFile? {
  val comparator = SpeedSearchComparator(false, true)
  var exact: VirtualFile? = null
  var partial: VirtualFile? = null
  ProjectFileIndex.getInstance(project).iterateContent { file ->
    ProgressManager.checkCanceled()
    if (!file.isDirectory) {
      if (file.name.equals(pattern, ignoreCase = true)) {
        exact = file
        return@iterateContent false
      }
      if (partial == null && comparator.matchingFragments(pattern, file.name) != null) partial = file
    }
    true
  }
  return exact ?: partial
}

internal fun revealProjectFile(tree: JTree, file: VirtualFile, isCurrent: () -> Boolean, onSelected: () -> Unit = {}) {
  if (!file.isValid || !isCurrent()) return
  TreeUtil.promiseMakeVisible(tree, AbstractProjectViewPane.createVisitor(file)).onSuccess { path ->
    if (path != null && file.isValid && isCurrent()) {
      TreeUtil.selectPath(tree, path)
      onSelected()
    }
  }
}
