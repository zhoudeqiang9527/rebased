// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.rebased.javascript

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

internal object JavaScriptBundle : DynamicBundle("messages.RebasedJavaScriptBundle") {
  fun message(@PropertyKey(resourceBundle = "messages.RebasedJavaScriptBundle") key: String, vararg params: Any): String =
    getMessage(key, *params)
}
