# PlantMate Android application

This folder contains the standalone Java/XML Android application. The repository's existing prototype and files at the repository root are retained separately.

## Open and run

1. Install Android Studio and the Android SDK platform required by `app/build.gradle.kts` (compile SDK 37).
2. Open this `android/` folder in Android Studio (the folder containing `settings.gradle.kts`).
3. Allow Gradle Sync to finish.
4. For Firebase-backed features, obtain `google-services.json` for application ID `com.example.ungdungchiasecaycanh` from the project maintainers and place it in `android/app/`. The file is intentionally excluded from Git. The user must also have access to the Firebase project and its enabled services.
5. Select an Android device or emulator and run the `app` configuration.

Without Firebase configuration and project access, online Firebase features will not work. Never add service-account keys, signing keys, passwords, or tokens to the repository.

## Build from a terminal

On Windows, from this folder run `gradlew.bat assembleDebug`. On macOS or Linux, run `./gradlew assembleDebug`.
