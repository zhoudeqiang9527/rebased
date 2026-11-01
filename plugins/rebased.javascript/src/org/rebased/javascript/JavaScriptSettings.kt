// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.rebased.javascript

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

@Service(Service.Level.PROJECT)
@State(name = "RebasedJavaScript", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
internal class JavaScriptSettings : SimplePersistentStateComponent<JavaScriptSettings.Options>(Options()) {
  class Options : BaseState() {
    var enabled: Boolean by property(true)
    var nodePath: String? by string()
  }
}
