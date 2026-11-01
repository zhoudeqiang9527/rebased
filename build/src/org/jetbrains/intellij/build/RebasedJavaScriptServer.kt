// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.intellij.build

import com.intellij.util.io.Decompressor
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Base64
import java.util.Properties

/** Downloads pinned, self-contained npm packages at build time. No npm install or runtime downloads are needed. */
internal suspend fun bundleRebasedJavaScriptServer(targetDir: Path, context: BuildContext) {
  val packages = Properties()
  Files.newInputStream(context.paths.communityHomeDir.resolve("plugins/rebased.javascript/server-packages.properties")).use {
    packages.load(it)
  }
  for (name in packages.stringPropertyNames().sorted()) {
    val (version, expectedHash) = packages.getProperty(name).split(',', limit = 2)
    val archive = downloadFileToCacheLocation("https://registry.npmjs.org/$name/-/$name-$version.tgz",
                                            context.paths.communityHomeDirRoot)
    val digest = MessageDigest.getInstance("SHA-512")
    DigestInputStream(Files.newInputStream(archive), digest).use { it.transferTo(OutputStream.nullOutputStream()) }
    check(Base64.getEncoder().encodeToString(digest.digest()) == expectedHash) { "Checksum mismatch for $name $version" }
    Decompressor.Tar(archive).removePrefixPath("package").extract(targetDir.resolve("lib/rebased-javascript/$name"))
  }
}
