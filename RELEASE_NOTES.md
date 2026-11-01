# Rebased 1.0.0

This is the first Windows x64 release of this fork.
It includes the Git review features from [upstream Rebased](https://github.com/DetachHead/rebased).
It adds code navigation for Python, JavaScript, and TypeScript.

## Changes

- Follow a function, method, or import with **Ctrl+B** in a source editor or a supported workspace diff.
- Resolve Python declarations with the bundled PythonCore plugin and a configured project interpreter.
- Resolve JavaScript and TypeScript declarations with the bundled language server and TypeScript engine.
- Use project configuration from `tsconfig.json` or `jsconfig.json`, including path aliases.
- Right-click a diff and select **Jump to Source**, or press **F4**, to open the file at the mapped position.
- Search for a file inside a collapsed Project directory. The tree expands its path and selects the file.
- Configure Python interpreters and source roots in Rebased. Keep these settings when you reopen the project.

## Windows setup

1. Download `rebased-1.0.0-windows-x64.zip` from the release assets.
2. Extract the complete ZIP into a writable directory.
3. Start `bin/rebased64.exe`. This launcher opens the GUI without a console window.
4. Open your repository. Follow the language setup steps below.

Keep all extracted files together. The package includes the Java runtime, plugins, language server, and fallback TypeScript engine.
You do not need a separate Java installation or a global npm language server package.
The separate `bin/rebased.exe` launcher uses a console window.

For Python, select an existing interpreter in **Settings > Build, Execution, Deployment > Python Interpreter**.
Keep indexing enabled and wait for the interpreter update and indexing to finish.
For a `src/` project layout, mark `src` as a source root.

For JavaScript and TypeScript, install Node.js **22.22.2 or later** and put it on PATH.
You can also select the executable in **Settings > Languages & Frameworks > JavaScript and TypeScript**.
Install your project's dependencies with its normal package manager.
Supported extensions are `.js`, `.jsx`, `.ts`, `.tsx`, `.mjs`, `.mts`, `.cjs`, and `.cts`.

Use **Ctrl+B** to follow a declaration and **Ctrl+Alt+Left** to return with the default Windows keymap.
In the Project tree, type a file name such as `time.js`. Press **Enter** to open the selected file.

## Verification

- The navigation update passed 99 automated tests across JS/TS, LSP, diff mapping, and Python navigation.
- The Project search update passed six automated tests. The six diff mapping tests also passed after the source menu change.
- Windows UI checks covered Python, JavaScript, and TypeScript navigation in source editors and workspace diffs.
- Windows UI checks also covered source navigation from both diff modes and selection of `utils/time.js` inside a collapsed directory.
- The release ZIP passed an archive integrity check. A SHA-256 checksum is provided with the download.

These checks ran for separate feature changes. They do not represent one combined test run.
This package contains the tested executable code from commit `e432912a491859cf3164863a5697e33dc70f83bd`.
The release commit adds documentation and download links.
The local build skipped SBOM generation after the license metadata service timed out.

## Limits

- This release provides a Windows x64 package. Linux and macOS were not validated for this fork's changes.
- Java support is not included. Vue and Svelte component navigation is outside this release.
- JS/TS support provides declaration navigation. Completion, refactoring, diagnostics, and formatting are outside this feature.
- Use **Ctrl+B** for JS/TS navigation. The LSP integration does not provide Ctrl+click navigation here.
- Python static analysis cannot resolve every dynamic reference.
- Unified diff navigation needs a matching workspace file. Historical revision navigation is outside this release.
- Returning after a unified diff jump opens the source editor at the call.
- Project search keeps visible-node matching. The background fallback selects the first hidden match. It skips excluded directories and external libraries.

See the [README](README.md), [Python guide](PYTHON_NAVIGATION.md), and [JS/TS guide](JAVASCRIPT_NAVIGATION.md) for more details.
