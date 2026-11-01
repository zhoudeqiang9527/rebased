# Python navigation preview

This fork bundles PythonCore from the same IntelliJ Platform revision as Rebased.
It supports Python declaration navigation in source editors and workspace diffs.
Build this fork to use the feature. Upstream release downloads do not include this change.

## Setup

1. Open a Python project.
2. Open **Settings > Build, Execution, Deployment > Python Interpreter**.
3. Select **Add Interpreter > Add Local Interpreter** and select an existing Python interpreter.
   You can use the interpreter in your project virtual environment.
4. For a `src/` layout, right-click `src` in the Project view.
   Select **Mark Directory As > Sources Root**.
5. Keep indexing enabled and wait for the interpreter update and indexing to finish.
6. Put the caret on a function call and press **Ctrl+B** with the default Windows keymap.
   Press **Ctrl+Alt+Left** to return.

On Windows, use `bin/rebased64.exe` to start the GUI without a console window.
The `bin/rebased.exe` launcher uses the Windows console subsystem.
The ZIP includes JBR. You need an existing Python interpreter for your project.

## Implementation

- Bundle PythonCore and its required plugins in the Rebased product.
- Restore Facet services and module initialization for Python SDK configuration.
- Register the platform action that marks source roots.
- Map unified diff positions to local source files and delegate navigation to the platform action.
- Select a Windows batch activation script for the environment reader that uses `cmd.exe`.
- Skip plugin recommendation scans when the product has no plugin advertiser service.
- Let the development launcher use the community checkout root.

## Limits

Python navigation depends on static analysis.
Dynamic imports, runtime type changes, and monkey patches can prevent a unique result.

Unified diff navigation maps only to actual workspace files.
It rejects historical content, stale mappings, and lines whose text does not match the source.
After navigation from a unified diff, the return action opens the call in the source editor.

The action also registers the platform mouse shortcuts.
The manual checks covered Ctrl+B. They did not cover Ctrl+click.

## Validation

The Windows x64 preview was built and checked locally on 2026-11-01 with Python 3.13.9.
All 195 tests passed with no failures or skipped tests.

| Test class | Passed |
| --- | ---: |
| UnifiedDiffNavigationTest | 6 |
| PyNavigationTest | 43 |
| PyMultiFileResolveTest | 85 |
| PyRelativeImportResolveTest | 43 |
| WindowsActivationEnvironmentTest | 1 |
| ActivationEnvDeltaTest | 7 |
| FacetModelBridgeTest | 8 |
| FacetWorkspaceModelTest | 2 |

For `FacetModelBridgeTest`, pass `"-Dpass.idea.platform.prefix=Rebased"` to `tests.cmd`.
The existing test cleanup needs a non-null platform prefix.

Manual checks covered local functions, imported aliases, methods, standard library functions, and `src/` imports.
They also covered workspace navigation in unified and side-by-side diffs, external file updates, and navigation back.
The final ZIP passed startup with an empty index directory and retained interpreter and source root settings.
The runtime log had no SEVERE or ERROR entries after indexing and the navigation checks.

The checkout has no `//build:all-products-packaging_test` target.
The attempt to run that check failed during target lookup.
The Windows ZIP was built and run instead.
Other operating systems and performance changes were not tested.

## Build the Windows ZIP

Run this command in PowerShell from the repository root:

```powershell
.\installers.cmd --% -Dintellij.build.target.os=current -Dintellij.build.target.arch=current -Dintellij.build.skip.build.steps=maven_artifacts,windows_exe_installer
```

The ZIP is written to `out/idea-ce/artifacts/rebased.win.zip` on Windows x64.
This command skips Maven artifacts and the EXE installer.
It does not publish a release.
