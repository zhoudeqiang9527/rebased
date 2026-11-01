// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.rebased.javascript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class JavaScriptConfigurationTest {
  @Test
  fun `recognizes module and React extensions without replacing file types`() {
    for (extension in listOf("js", "mjs", "cjs", "JS")) assertEquals("javascript", javaScriptLanguageId(extension))
    for (extension in listOf("ts", "mts", "cts", "TS")) assertEquals("typescript", javaScriptLanguageId(extension))
    assertEquals("javascriptreact", javaScriptLanguageId("jsx"))
    assertEquals("typescriptreact", javaScriptLanguageId("tsx"))
  }

  @Test
  fun `does not start for other languages`() {
    for (extension in listOf(null, "py", "java", "json", "vue", "svelte")) assertNull(javaScriptLanguageId(extension))
  }

  @Test
  fun `checks the language server Node version requirement`() {
    for (version in listOf("v22.22.2", "v22.23.0", "v24.0.0", "25.1.0")) assertTrue(isSupportedNodeVersion(version), version)
    for (version in listOf("v22.22.1", "v22.1.0", "v20.19.0", "", "v24", "v24.1.x")) {
      assertFalse(isSupportedNodeVersion(version), version)
    }
  }
}
