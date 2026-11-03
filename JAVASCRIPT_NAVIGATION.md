# JavaScript and TypeScript navigation

This fork uses the IntelliJ Platform LSP client and the TypeScript language server to resolve declarations and find usages.
It keeps the existing TextMate syntax highlighting.
It does not bundle a JavaScript IDE plugin or add Java support.

## Setup

1. Install Node.js 22.22.2 or later. Put the executable on PATH.
2. Start this fork and open your repository.
3. Open a supported source file or workspace diff. The language server starts in a trusted project.
4. Wait for the language server to start. Put the caret on a call and press **Ctrl+B** with the default Windows keymap.
5. Press **Ctrl+Alt+Left** to return.

To find references, put the caret on a function or another symbol and choose **Find Usages** from the editor context menu
(or press **Alt+F7** with the default Windows keymap). The results include references across supported project files.

Supported extensions are `.js`, `.jsx`, `.ts`, `.tsx`, `.mjs`, `.mts`, `.cjs`, and `.cts`.
The server reads the project's `tsconfig.json` or `jsconfig.json`.
It supports functions, methods, imported aliases, CommonJS modules, and configured path aliases.
Resolution uses current editor text, including unsaved changes.

If Node.js is outside PATH, open **Settings > Languages & Frameworks > JavaScript and TypeScript**.
Set **Node.js executable** to the executable file, such as `D:\nodejs\node.exe`.
Leave the field empty to use PATH.
You can also disable navigation for this project.
Apply the settings to restart the language server.

For a project with package dependencies, install those dependencies with the project's normal package manager.
The server cannot resolve a dependency that is absent from the checkout.
Use the language server status widget to inspect a startup failure or restart the server.

## Server distribution

The product build downloads pinned npm tarballs and checks each SHA-512 digest before extraction.
The packages are `typescript-language-server` 6.0.2 and TypeScript 5.9.3.
Both packages use the Apache 2.0 license. The distributed package directories retain their license files.
The manifest is [server-packages.properties](plugins/rebased.javascript/server-packages.properties).

The ZIP includes these packages under `lib/rebased-javascript`.
It does not include Node.js. The user supplies Node.js.
A global npm package installation and a runtime server download are not required.
The server prefers the project's TypeScript installation and uses the bundled engine as a fallback.
Automatic type package acquisition is disabled.

The platform manages file synchronization, request cancellation, process shutdown, and project trust.
The server starts on demand for a supported file.
It uses one semantic TypeScript server and disables the separate syntax server.
Completion, diagnostics, formatting, rename, and other editing features are disabled in this integration.

## Workspace diffs

The existing unified diff adapter also supports JS/TS.
It maps a supported context or added line to the real workspace document, then invokes Go to Declaration.
The file text and line mapping must match.
Historical content and stale mappings are rejected.
After a jump from a unified diff, the return action opens the call in its source editor.

Both unified and side-by-side diffs start the language server without a separate source tab.
The platform keeps workspace documents available until the last diff or source editor closes.
It also restores diff documents after a server restart.
Historical documents do not start a server or replace workspace content.

## Limits

- Use **Ctrl+B**. The native LSP client does not provide Ctrl+click navigation in this checkout.
- Dynamic JavaScript can lack a unique static definition or references that can be determined statically.
- Vue and Svelte component files need a separate language server and are not supported here.
- This feature does not provide a full JavaScript IDE.
- Linux and macOS runtime behavior has not been checked.

## Development

Prepare the same server packages for a source run or an integration test:

```powershell
node tools/prepare-javascript-server.mjs
```

This command requires Node.js, network access, and `tar` on PATH.
It writes the packages to `out/rebased-javascript`.
Set `rebased.javascript.server.path` to that absolute directory for a source run.
The product build prepares its own server directory automatically.

Run the tests from PowerShell. Replace the example checkout path with your path:

```powershell
.\tests.cmd --% --module intellij.rebased.javascript.tests --test org.rebased.javascript.*Test "-Dpass.idea.platform.prefix=Rebased" "-Dpass.rebased.javascript.server.path=E:/git-Rebased/rebased/out/rebased-javascript"
```

The integration tests start the actual server and invoke the editor's Go to Declaration action.
They cover all eight extensions, import aliases, path aliases, methods, CommonJS, and unsaved editor text.
They also open actual diff viewers before a source editor and restart the server with only a diff open.
The configuration tests cover extension selection and the Node.js version requirement.

## Validation

The change passed 99 automated tests on Windows x64 on 2026-11-01 with Node.js 24.15.0.
No test failed or was skipped.

| Test class | Passed |
| --- | ---: |
| JavaScriptNavigationTest | 16 |
| JavaScriptConfigurationTest | 3 |
| LspDiffEditorTest | 3 |
| LspOpenedFilesServiceTest | 5 |
| LspTextDocumentProtocolTest | 23 |
| UnifiedDiffNavigationTest | 6 |
| PyNavigationTest | 43 |

Each JS/TS navigation test uses a separate project and starts the actual packaged server version.
The test framework checks thread cleanup after each test.
The tests cover all eight extensions, imported aliases, path aliases, methods, CommonJS, and unsaved source text.

The Windows ZIP also passed manual checks in its packaged application.
A JavaScript side-by-side diff started the server without an open source tab.
Ctrl+B followed a CommonJS call to its definition, and Ctrl+Alt+Left returned to the diff.
A TypeScript unified diff resolved an imported alias through a configured path alias.
The return action opened the original call in the source editor.
The runtime log had no ERROR or SEVERE entries during these checks.

The checkout has no `//build:all-products-packaging_test` target.
That check failed during target lookup.
The repository format check cannot launch its Bash entry point on this Windows setup.
The generated BUILD files passed a direct check with the repository's native Buildifier binary.
