# AutoCore

AutoCore is a native Android automation app.

## Build

The GitHub Actions workflow **Build AutoCore APK** builds a debug APK on every push to `main` and supports manual runs.

1. Open the repository's **Actions** tab.
2. Select **Build AutoCore APK**.
3. Open a successful run and download the `AutoCore-0.1.0-debug` artifact.
4. Extract the ZIP to get `app-debug.apk`.

## Current 0.1.0 scope

- Native Android application shell with dashboard and navigation.
- Guided automation templates.
- Weekly schedule editor for silent / normal sound modes.
- Scheduled actions restored after device reboot.
- Android Do Not Disturb permission entry point.
- Local sound sample recording (up to five seconds).
- Text field and soft-keyboard entry for a target key command.

## Not implemented yet

- Continuous sound recognition and sound-to-key execution.
- Accessibility-based key injection into other apps.
- Advanced visual flow editor, variables and multi-condition rules.
- Multiple independent schedules per action and precise-alarm guarantees.

Android may defer inexact alarms under battery restrictions. Changing Do Not Disturb requires the user to grant special access in Android settings. Audio recording requires microphone permission and is started only by the user.

## Technology

Java, Android SDK 35, Gradle, Android AlarmManager and MediaRecorder.
