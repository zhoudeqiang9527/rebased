package com.intellij.vcs.log.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import com.intellij.vcs.log.impl.CommonUiProperties
import com.intellij.vcs.log.impl.VcsLogApplicationSettings
import com.intellij.vcs.log.ui.editor.DefaultVcsLogFile
import org.mockito.Mockito

class CanCloseVirtualLogFileTest : BasePlatformTestCase() {
  fun testBatchCloseKeepsProtectedLogAndClosesOtherFiles() {
    val settings = VcsLogApplicationSettings()
    ApplicationManager.getApplication().replaceService(VcsLogApplicationSettings::class.java, settings, testRootDisposable)
    settings[CommonUiProperties.SHOW_IN_EDITOR] = true
    val log = Mockito.mock(DefaultVcsLogFile::class.java)
    Mockito.`when`(log.isFirstLogFile).thenReturn(true)
    val file = LightVirtualFile("test.txt")
    val check = CanCloseVirtualLogFile()

    assertFalse(check.canCloseFile(log))
    assertEquals(listOf(file), check.filterFilesToClose(listOf(log, file)))

    settings[CommonUiProperties.SHOW_IN_EDITOR] = false
    assertEquals(listOf(log, file), check.filterFilesToClose(listOf(log, file)))
  }
}
