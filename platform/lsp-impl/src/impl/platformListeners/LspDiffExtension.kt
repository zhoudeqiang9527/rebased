// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.platform.lsp.impl.platformListeners

import com.intellij.diff.DiffContext
import com.intellij.diff.DiffExtension
import com.intellij.diff.FrameDiffTool
import com.intellij.diff.contents.DocumentContent
import com.intellij.diff.requests.ContentDiffRequest
import com.intellij.diff.requests.DiffRequest
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.platform.lsp.impl.documentSync.LspOpenedFilesService

internal class LspDiffExtension : DiffExtension() {
  override fun onViewerCreated(viewer: FrameDiffTool.DiffViewer, context: DiffContext, request: DiffRequest) {
    val project = context.project?.takeUnless { it.isDefault || it.isDisposed } ?: return
    val contents = (request as? ContentDiffRequest)?.contents ?: return
    val files = contents.asSequence().filterIsInstance<DocumentContent>()
      .mapNotNull { FileDocumentManager.getInstance().getFile(it.document) }
      .filter { it.isValid && it.isInLocalFileSystem }
      .toSet()
    LspOpenedFilesService.getInstance(project).trackDiffFiles(files, viewer)
  }
}
