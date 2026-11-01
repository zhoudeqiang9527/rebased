<div align="center">
  <img src="./community-resources/resources/rebased.svg" width="100" height="100" />  
  <h1>Rebased</h1>
  <p>A Git client based on the IntelliJ Platform, with Python, JavaScript, and TypeScript navigation in this fork.</p>
</div>

![](screenshot.png)

## This fork: code navigation for Git reviews

This fork builds on [DetachHead/rebased](https://github.com/DetachHead/rebased).
It adds Python, JavaScript, and TypeScript declaration navigation to help you understand code while you review Git changes.
You can follow a function call to its definition from a source file or a workspace diff.
Version 1.0.0 targets Windows x64.

The feature uses the PythonCore plugin from the same IntelliJ Platform revision as Rebased.
It resolves references through the platform's Python analysis and navigation.
JavaScript and TypeScript use the platform LSP client with a bundled TypeScript language server.
Java support is not included.
Rebased still provides the Git log, change review, staging, and commit interface.

> [!IMPORTANT]
> Download this fork's Windows package to use code navigation.
> The upstream installation links below do not include this fork's features.

### Download version 1.0.0

- [Windows x64 ZIP](https://github.com/zhoudeqiang9527/rebased/releases/download/v1.0.0/rebased-1.0.0-windows-x64.zip)
- [SHA-256 checksum](https://github.com/zhoudeqiang9527/rebased/releases/download/v1.0.0/rebased-1.0.0-windows-x64.zip.sha256)
- [Release notes](RELEASE_NOTES.md)

Extract the complete ZIP into a writable directory. Start `bin/rebased64.exe`.
Keep the runtime, library, and plugin directories beside the executable. No separate Java installation is required.
Use an existing Python interpreter for Python navigation. Install Node.js 22.22.2 or later for JavaScript and TypeScript navigation.

### What we added

| Area | Change in this fork | What you can do |
| --- | --- | --- |
| Python support | Bundle PythonCore and its required plugins. | Resolve Python declarations through the platform's Python analysis. |
| JavaScript and TypeScript | Bundle a language server and a fallback TypeScript engine. | Follow functions, methods, imports, and project path aliases with Ctrl+B. |
| JS/TS setup | Start the language server when a supported source file or workspace diff opens. | Use Node.js from PATH, or select its executable in project settings. |
| Source editors | Enable Python navigation with a configured interpreter and project indexes. | Follow local functions, imported aliases, methods, and standard library calls. |
| Unified diffs | Map a diff position to the actual workspace file before navigation. | Follow a call from a supported context line or added line in a unified diff. |
| JS/TS diffs | Start and retain language services for workspace documents shown in diffs. | Navigate from a diff without opening a source tab first. |
| Open source from a diff | Add the existing source action to the diff editor menu. | Right-click and select **Jump to Source**, or press **F4**, to edit the file at the mapped position. |
| Project speed search | Search project files when the visible tree has no match. | Find a file inside a collapsed directory, expand its path, and select the file. |
| Project setup | Restore Python SDK configuration and the source root action. | Select a virtual environment and mark `src/` as a source root. |
| Windows environments | Use a batch activation script with the environment reader that runs `cmd.exe`. | Update the interpreter environment without opening a PowerShell script in an editor. |
| Product integration | Restore required Facet services and module initialization. Handle the absent plugin advertiser service. | Keep interpreter setup, package updates, and project reopening working in Rebased. |

The unified diff adapter checks the file, text, and line mapping before it navigates.
It rejects historical content and stale mappings instead of treating them as current source code.
The development launcher also supports the standalone community checkout root.

### Open the source file from a diff

Put the caret on the line in the diff that you want to edit.
Right-click the diff editor and select **Jump to Source**. You can also press **F4** with the default Windows keymap.
Rebased opens the source editor and uses the diff's line mapping to place the caret.
This action opens the file. Use **Ctrl+B** when you want to follow a function to its declaration.
The action requires a source file that the diff can navigate to.

### Find files inside collapsed directories

Focus the **Project** tree and type a file name, such as `time.js`.
When the visible nodes have no match, Rebased searches the project content in the background.
It expands the matching path and selects `utils/time.js`, rather than stopping on `utils`.
You can then press **Enter** to open the selected file.

The search ignores case and supports partial names.
It prefers an exact file name when it searches collapsed directories.
It searches project content and skips excluded directories and external libraries.
It does not require the file search index.
Changing the query or closing the search cancels the previous result's selection.
The existing search keys and visible-node matching stay available.
The background fallback selects the first matching file. It does not build a list of all hidden matches.

### JavaScript and TypeScript quick start

1. Download the Windows ZIP above and extract it. Start `bin/rebased64.exe`.
2. Install Node.js **22.22.2 or later** and put it on PATH. Node.js 24 also works.
3. Open your repository and a source file or workspace diff.
   Supported extensions are `.js`, `.jsx`, `.ts`, `.tsx`, `.mjs`, `.mts`, `.cjs`, and `.cts`.
4. Wait for the language server to start. Put the caret on a function call and press **Ctrl+B**.
   Press **Ctrl+Alt+Left** to return with the default Windows keymap.

The server reads your `tsconfig.json` or `jsconfig.json` and uses the project's TypeScript version when available.
The ZIP includes a fallback TypeScript engine. A global npm package installation is not required.
If Node.js is outside PATH, select it in **Settings > Languages & Frameworks > JavaScript and TypeScript**.
The same settings page lets you disable the server for a project.

See [JavaScript and TypeScript navigation](JAVASCRIPT_NAVIGATION.md) for the supported cases, setup, and limits.

### Python quick start on Windows

1. Download the Windows ZIP above and extract it.
2. Start `bin/rebased64.exe`. This GUI launcher does not create a console window.
   The separate `bin/rebased.exe` launcher uses the Windows console subsystem.
3. Open your Python project.
4. Open **Settings > Build, Execution, Deployment > Python Interpreter**.
   Select **Add Interpreter > Add Local Interpreter** and choose an existing Python interpreter.
   You can use the interpreter from the project's virtual environment.
5. If the project has a `src/` layout, right-click `src` in the Project view.
   Select **Mark Directory As > Sources Root**.
6. Keep **Enable indexing files for search** enabled in **Settings > Appearance and Behavior > System Settings**.
   Wait for indexing and the interpreter update to finish.
7. Put the caret on a call and press **Ctrl+B** to go to its declaration.
   Press **Ctrl+Alt+Left** to return with the default Windows keymap.

For example, when `helpers.py` defines `greet`, you can navigate through an imported alias:

```python
# app.py
from helpers import greet as welcome

message = welcome("Ada")  # Put the caret on welcome and press Ctrl+B.
```

The same call can be followed from the current file side of a side-by-side diff.
In a unified diff, the adapter first locates the call in the workspace file, then invokes the language navigation action.

### What we verified

The project search change passed **6 automated tests**, with no failures or skipped tests.
They cover exact names, case, partial names, missing files, expansion of a collapsed path, and cancellation before selection.
The **6 diff mapping tests** also passed after the source menu change.
In the Windows build, we typed `time.js` in a collapsed Project tree and confirmed selection of `utils/time.js`.
We also opened the source at the mapped line from a unified diff menu and with **F4** in a side-by-side diff.

The JS/TS change passed **99 automated tests**, with no failures or skipped tests.
These include 19 JS/TS tests, 31 LSP tests, 6 diff mapping tests, and 43 Python navigation tests.
The JS/TS tests start the actual language server and invoke Go to Declaration.
Manual checks in the Windows ZIP covered JavaScript side-by-side diffs, TypeScript unified diffs, and navigation back.
These checks did not require an open source tab before the jump.
See [JS/TS validation](JAVASCRIPT_NAVIGATION.md#validation) for the covered cases and check limits.

The earlier Python preview passed **195 automated tests**, with no failures or skipped tests.
Those tests cover Python resolution, relative imports, diff mapping, environment activation, and Facet behavior.
The detailed counts are in [Python navigation](PYTHON_NAVIGATION.md#validation).

Python manual checks covered local functions, imported aliases, instance methods, standard library calls, and imports from `src/`.
They also covered unified and side-by-side diffs, external file updates, and navigation back.
The final Windows ZIP was started with an empty index directory.
Interpreter and source root settings survived reopening, and navigation worked after indexing.
The runtime log had no SEVERE or ERROR entries after these checks.

### Current limits

- The supported languages are Python, JavaScript, and TypeScript. Vue and Svelte component files are outside this release.
- JS/TS support provides declaration navigation. Completion, refactoring, diagnostics, and formatting are outside this feature.
- JS/TS navigation requires Node.js 22.22.2 or later. The native LSP client does not provide Ctrl+click navigation here; use Ctrl+B.
- Python navigation depends on static analysis. Dynamic imports and runtime changes can prevent a unique result.
- Unified diff navigation requires a matching workspace file. Historical revision navigation is outside this version's scope.
- Returning after a jump from a unified diff opens the call in the source editor, rather than the diff tab.
- Python manual shortcut checks covered **Ctrl+B**. Python **Ctrl+click** was not checked separately.
- Linux, macOS, and performance changes have not been tested for this feature.

### Build the Windows package

Check the repository's [build requirements](CONTRIBUTING.md#building-rebased), then clone this fork:

```powershell
git clone --depth 1 https://github.com/zhoudeqiang9527/rebased.git
cd rebased
```

Run this command in PowerShell from the repository root:

```powershell
.\installers.cmd --% "-Dintellij.build.target.os=current" "-Dintellij.build.target.arch=current" "-Dintellij.build.skip.build.steps=maven_artifacts,windows_exe_installer"
```

On Windows x64, the output is `out/idea-ce/artifacts/rebased.win.zip`.
The ZIP includes JBR and the JS/TS language server. Use an existing Python interpreter for Python, or Node.js for JS/TS.
This command skips Maven artifacts and the EXE installer.

See [Python navigation](PYTHON_NAVIGATION.md) and [JS/TS navigation](JAVASCRIPT_NAVIGATION.md) for implementation details and validation limits.

## Motivation

Rebased is an open-source remake of the short-lived [jetbrains git client](https://youtrack.jetbrains.com/issue/IJPL-72504/Make-git-client-a-standalone-app#focus=Comments-27-12868395.0-0).
Upstream Rebased removes most bundled IDE plugins and keeps the Git integration, with additional UI tweaks.
This fork adds PythonCore, a JS/TS language server, and the platform services needed for code navigation.

See [this youtrack issue](https://youtrack.jetbrains.com/issue/IJPL-72504/Make-git-client-a-standalone-app) for the many reasons people have been requesting this for almost a decade. At the time of writing, it's the #3 most upvoted open issue on YouTrack.

## Installation

The links and package commands in this section install **upstream Rebased**.
For this fork's navigation features, use [Download version 1.0.0](#download-version-100) above.

### Linux

We recommend installing the AppImage with with [AppManager](https://github.com/kem-a/AppManager) to install it to your applications menu, and for automatic updates:

|Architecture|Install via AppManager|Manual Download|
|-|-|-|
|x86_64|[Install](https://detachhead.codeberg.page/urlwrapper/?url=appimg://install?url=https://github.com/DetachHead/rebased/releases/latest/download/Rebased-x86_64.AppImage)|[Download](https://github.com/DetachHead/rebased/releases/latest/download/Rebased-x86_64.AppImage)
|aarch64|[Install](https://detachhead.codeberg.page/urlwrapper/?url=appimg://install?url=https://github.com/DetachHead/rebased/releases/latest/download/Rebased-aarch64.AppImage)|[Download](https://github.com/DetachHead/rebased/releases/latest/download/Rebased-aarch64.AppImage)

Alternatively you can download the `.tar.gz` from [GitHub Releases](https://github.com/DetachHead/rebased/releases)

### Windows

|Architecture|Installer|Portable `.zip`|
|-|-|-|
|x86_64|[Download](https://github.com/DetachHead/rebased/releases/latest/download/rebased.exe)|[Download](https://github.com/DetachHead/rebased/releases/latest/download/rebased.win.zip)|
|aarch64|[Download](https://github.com/DetachHead/rebased/releases/latest/download/rebased-aarch64.exe)|[Download](https://github.com/DetachHead/rebased/releases/latest/download/rebased-aarch64.win.zip)|

Alternatively you can install via winget:

```ps1
winget install detachhead.rebased --source winget
```

<table>
  <tr>
    <td><a href="https://signpath.io/"><img src="https://signpath.org/assets/favicon-50x50.png" alt="SignPath" width="32" /></a></td>
    <td>Free code signing on Windows provided by <a href="https://signpath.io/">SignPath.io</a>, certificate by <a href="https://signpath.org/">SignPath Foundation</a></td>
  </tr>
</table>

### macOS

Install with [homebrew](https://brew.sh/):

```bash
brew install detachhead/tap/rebased
```

Alternatively you can download the `.dmg` manually:

> [!WARNING]
> 
> When installing the `.dmg` manually, you may see the following error message after copying `Rebased.app` to your Applications folder:
> > "Rebased.app" is damaged and can't be opened. You should move it to the Bin.
> 
> **This is Apple lying to you.** Nothing is "damaged", it's just not code-signed with an Apple Developer certificate. To fix it, run the following command:
> ```bash
> xattr -rd com.apple.quarantine /Applications/Rebased.app
> ```

|Architecture|Download|
|-|-|
|Apple Silicon (aarch64)|[Download](https://github.com/DetachHead/rebased/releases/latest/download/rebased-aarch64.dmg)|
|Intel (x86_64)|[Download](https://github.com/DetachHead/rebased/releases/latest/download/rebased.dmg)|

## Features inherited from upstream

In addition to all the git-related features from [IntelliJ IDEA Community Edition](https://github.com/jetbrains/intellij-community), Rebased has some additional features which aren't available in any of the official JetBrains IDEs.

### Customizable git log location

By default, the git log in Rebased is located in the main editor window, as the graph is typically the main focus of a git GUI client.
However, if you prefer it to be in the tool window at the bottom like in JetBrains IDEs, you can disable this behavior:

1. Go to *Settings > Version Control > Log*
2. Uncheck "Show the log in the editor window"

### Option to disable the `.idea` directory

Unlike the `.vscode` directory, most of the config files generated by JetBrains IDEs are unfortunately not suitable to commit, as they often contain device-specific file paths that don't work if shared with other members of your team.

Rebased is intended to be used with a wider variety of projects, which means you're more likely to use it with repos that weren't intended to be opened in a JetBrains IDE. This means `.idea` config files aren't committed nor are they excluded in `.gitignore`. This results in Rebased generating unwanted config files most of the time.

To address this problem, Rebased allows you to disable the creation of the `.idea` directory in the project root:

1. Go to *Settings > Appearance and Behavior > System Settings*
2. Uncheck "Store project settings in the project root directory"

> [!NOTE]
> This does not disable project-level config. Instead, Rebased will store each project's config inside a single centralized `.idea` directory in the same place as the IDE's global settings.

### Option to disable indexing

Keep indexing enabled to use this fork's Python navigation.

In order for the search feature to work reliably, JetBrains IDEs will scan and index all the files in your project. You can disable the indexing if you don't need to search your project with Rebased, if for example you use a separate IDE that has its own search functionality:

1. Go to *Settings > Appearance and Behavior > System Settings*
2. Uncheck "Enable indexing files for search"

> [!NOTE]
> This does not entirely disable project analysis, as some scanning is still needed in order for Rebased to be able to track unversioned files to show in the git changes list.

### Additional TextMate bundles

IntelliJ Community Edition includes syntax highlighting for many languages, even when their corresponding language plugin is not installed, thanks to the [TextMate Bundles](https://plugins.jetbrains.com/plugin/7221) plugin.

Unfortunately however, syntax highlighting is only available for some languages in their respective paid plugins/IDEs.

One of the goals of Rebased is to prevent users from having to install bloated plugins when they only want basic editor/git functionality, so we ship some additional TextMate bundles that aren't present in IntelliJ:

- [vue](https://github.com/vuejs/language-tools)

(currently only one language, but feel free to open an [issue](https://github.com/DetachHead/rebased/issues/new/choose) or [PR](https://github.com/DetachHead/rebased/compare) for others)

## Plugins

### Compatibility disclaimer

This fork bundles PythonCore and restores the platform services required for the Python scenarios described above.
It also includes a JS/TS navigation plugin that uses the platform LSP client.
The remaining notes describe upstream Rebased's plugin policy and the limits of other marketplace plugins.

While installing plugins from the marketplace is supported, note that many of the core components otherwise present in every JetBrains IDE are disabled in Rebased. This means some plugins may not work properly (or at all) if they depend on such components.

If you encounter any problems with a git-related plugin, you may open an issue, but keep in mind:

- issues about language specific-plugins or other plugins that aren't related to git functionality are likely to be considered out-of-scope for Rebased, but will be assessed on a case-by-case basis
- if fixing an issue with a git-related plugin requires re-enabling bloated IDE features that should otherwise not be required in a git client, it may still be considered out-of-scope

### For plugin developers

If you need to debug your plugin in Rebased, the IntelliJ platform gradle plugin [supports debugging with a local instance of the IDE](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html#dependenciesLocalPlatform). You can configure it to use Rebased.

> [!NOTE]
> If you're on linux using the AppImage, you'll have to either extract or mount it first. If you're using AppManager, you can click "Extract AppImage". Alternatively you can run `./Rebased.AppImage --appimage-mount` to mount it instead.
> 
> Once mounted/extracted, set the `local` path to the `usr` directory inside the AppImage's filesystem, for example:
> ```kts
> dependencies {
>   intellijPlatform {
>     local("/tmp/.mount_Rebaseccgcph/usr")
>   }
> }
> ```

## Contributing

See [`CONTRIBUTING.md`](./CONTRIBUTING.md)

## Credits

- https://github.com/obiscr/intellij-community - a previous attempt at creating a JetBrains git client that I cherrypicked some commits from
- JetBrains (obviously)
