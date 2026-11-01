// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.diff.tools.fragmented

import com.intellij.diff.util.LineCol
import com.intellij.diff.util.Side
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.ApiStatus
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

internal class UnifiedDiffGoToDeclarationAction private constructor(
  private val viewer: UnifiedDiffViewer,
) : DumbAwareAction() {
  init {
    ActionUtil.copyFrom(this, IdeActions.ACTION_GOTO_DECLARATION)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = e.project != null && findTarget(e) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val target = findTarget(e) ?: return
    val action = ActionManager.getInstance().getAction(IdeActions.ACTION_GOTO_DECLARATION) ?: return
    val editor = FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptor(project, target.file, target.offset), true)
                 ?: return
    val context = SimpleDataContext.getSimpleContext(
      CommonDataKeys.EDITOR, editor, DataManager.getInstance().getDataContext(editor.contentComponent),
    )
    // The source editor has a different coordinate system from the mouse event.
    val event = AnActionEvent.createEvent(action, context, null, e.place, e.uiKind, null)
    ActionUtil.performAction(action, event)
  }

  private fun findTarget(e: AnActionEvent): UnifiedDiffNavigationTarget? {
    val editor = viewer.editor
    if (e.getData(CommonDataKeys.EDITOR) !== editor) return null
    val mouseEvent = e.inputEvent as? MouseEvent
    val offset = if (mouseEvent != null) {
      val component = mouseEvent.component ?: return null
      if (!SwingUtilities.isDescendingFrom(component, editor.contentComponent)) return null
      val point = SwingUtilities.convertPoint(component, mouseEvent.point, editor.contentComponent)
      editor.logicalPositionToOffset(editor.xyToLogicalPosition(point))
    }
    else {
      editor.caretModel.offset
    }
    return findUnifiedDiffNavigationTarget(viewer, offset)
  }

  companion object {
    @JvmStatic
    fun register(viewer: UnifiedDiffViewer) {
      if (ActionManager.getInstance().getAction(IdeActions.ACTION_GOTO_DECLARATION) == null) return
      val proxy = UnifiedDiffGoToDeclarationAction(viewer)
      proxy.registerCustomShortcutSet(proxy.shortcutSet, viewer.editor.contentComponent, viewer)
    }
  }
}

@ApiStatus.Internal
data class UnifiedDiffNavigationTarget(val file: VirtualFile, val offset: Int)

@ApiStatus.Internal
fun findUnifiedDiffNavigationTarget(viewer: UnifiedDiffViewer, offset: Int): UnifiedDiffNavigationTarget? {
  if (viewer.isStateIsOutOfDate) return null
  val unifiedDocument = viewer.editor.document
  if (offset !in 0..unifiedDocument.textLength) return null
  val position = LineCol.fromOffset(unifiedDocument, offset)
  val unifiedLine = unifiedDocument.getText(TextRange(
    unifiedDocument.getLineStartOffset(position.line), unifiedDocument.getLineEndOffset(position.line),
  ))
  for (side in listOf(Side.RIGHT, Side.LEFT)) {
    val sourceLine = viewer.transferLineFromOnesideStrict(side, position.line)
    if (sourceLine < 0) continue
    val document = viewer.getDocument(side)
    val file = FileDocumentManager.getInstance().getFile(document) ?: continue
    // A revision can use a synthetic PSI file with a workspace path.
    if (!file.isValid || !file.isInLocalFileSystem || file.isDirectory) continue
    if (sourceLine >= document.lineCount) continue
    val start = document.getLineStartOffset(sourceLine)
    val end = document.getLineEndOffset(sourceLine)
    if (position.column > end - start) continue
    // Ignore-whitespace comparisons can pair lines with different offsets.
    if (document.getText(TextRange(start, end)) != unifiedLine) continue
    return UnifiedDiffNavigationTarget(file, start + position.column)
  }
  return null
}
