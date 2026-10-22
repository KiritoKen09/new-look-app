# new-look-app
**Developed by kirioken (KiritoKen09)**

**new-look-app** is an Android application configured for network connectivity and secure file handling, utilizing Android's `FileProvider` for content sharing.

## Run Locally

**Prerequisites:** [Android Studio](https://developer.android.com/studio)

1. Open Android Studio.
2. Select **Open** and choose the directory containing the `new-look-app` project.
3. Allow Android Studio to sync and fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the root project directory and set `GEMINI_API_KEY` in that file to your API key (see `.env.example` for reference).
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or a physical Android device.
