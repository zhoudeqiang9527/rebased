// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.rebased.javascript

import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspClientManager
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel

internal class JavaScriptConfigurable(private val project: Project) : BoundConfigurable(JavaScriptBundle.message("settings.name")) {
  override fun createPanel() = panel {
    val options = project.service<JavaScriptSettings>().state
    row {
      checkBox(JavaScriptBundle.message("settings.enabled")).bindSelected(options::enabled)
    }
    row(JavaScriptBundle.message("settings.node")) {
      textFieldWithBrowseButton(FileChooserDescriptorFactory.createSingleFileDescriptor())
        .bindText({ options.nodePath.orEmpty() }, { options.nodePath = it.trim().ifEmpty { null } })
        .comment(JavaScriptBundle.message("settings.node.comment"))
    }
    row { comment(JavaScriptBundle.message("settings.description")) }
  }

  override fun apply() {
    super.apply()
    if (!project.isDefault) {
      LspClientManager.getInstance(project).stopAndRestartClientsIfNeeded(JavaScriptLspIntegrationProvider::class.java)
    }
  }
}
