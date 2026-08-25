# Gradle Wrapper

This project is configured for Gradle 8.13.

The wrapper scripts are included, but `gradle-wrapper.jar` must be generated once
with a local Gradle installation:

    gradle wrapper --gradle-version 8.13 --distribution-type bin

After that, use:

Windows:
    gradlew.bat assembleDebug
    gradlew.bat bundleRelease

macOS/Linux:
    ./gradlew assembleDebug
    ./gradlew bundleRelease

The wrapper will download the Gradle distribution automatically.


GitHub Actions note: the included workflow does not require gradle-wrapper.jar; it installs Gradle 8.13 directly.
