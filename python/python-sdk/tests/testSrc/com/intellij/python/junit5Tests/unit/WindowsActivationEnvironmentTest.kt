// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.python.junit5Tests.unit

import com.intellij.python.sdk.backend.ActivationScript
import com.intellij.python.sdk.backend.PythonEnvironment
import com.intellij.python.sdk.backend.service.ActivatableEnvironmentService
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.jetbrains.python.sdk.terminal.Shell
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.writeText

@TestApplication
@EnabledOnOs(OS.WINDOWS)
@Timeout(30)
internal class WindowsActivationEnvironmentTest {
  private val root = tempPathFixture()

  @Test
  fun `reads the Windows cmd activation environment`(): Unit = runBlocking {
    val directory = root.get()
    val script = directory.resolve("activate.bat")
    script.writeText("@echo off\r\nset REBASED_ACTIVATION_TEST=activated\r\n")
    val environment = object : PythonEnvironment {
      override val version: String? = null
      override val pythonBinaryPath = directory.resolve("python.exe")

      override fun activationScript(shellType: Shell.Type): ActivationScript? =
        if (shellType == Shell.Type.UNKNOWN) ActivationScript(script) else null
    }

    val variables = ActivatableEnvironmentService().activationEnvironment(environment).orThrow()

    assertEquals("activated", variables["REBASED_ACTIVATION_TEST"])
  }
}
