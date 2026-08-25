# MyFirstMillion

Android app "Мій перший мільйон".

## Automatic GitHub build

This repository includes GitHub Actions.

After uploading the project to GitHub:

1. Open the **Actions** tab.
2. Select **Android Build**.
3. Click **Run workflow** if you want to start it manually.
4. Wait for the build to finish.
5. Open the completed workflow run.
6. Download the artifacts:
   - `MyFirstMillion-debug-apk`
   - `MyFirstMillion-release-aab`

The workflow uses JDK 17 and Gradle 8.13.

## Important

The release AAB produced by this workflow is an unsigned/unpublished build unless
a signing configuration and keystore are added later. For Google Play publication,
we will configure release signing separately.


## Version 0.5
Gold-focused visual refresh, animated progress, contribution labels, and enhanced milestone styling.


## Version 0.8
- Bottom navigation: Home, Add, Path, History.
- Dedicated contribution screen and history screen.
- Target/path settings moved to their own screen.
