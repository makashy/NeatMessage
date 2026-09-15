# NeatMessage

A privacy-first Android SMS manager focused on making suspicious messages easy to see and control, including sender IDs, short codes, and messages without a normal phone number.

## Current prototype

- Jetpack Compose inbox UI
- All, Needs review, and Blocked filters
- Trusted, review, likely spam, and blocked states
- Organization sender IDs and unknown senders shown as normal first-class senders
- Android SMS permissions declared in the manifest

## Run

Open this folder in Android Studio with an Android SDK installed and sync the Gradle project. Run the `app` configuration on an Android 8.0+ emulator or device.

A production release must request runtime SMS permissions and become the user's default SMS app before it can fully manage incoming messages. The next implementation step is to add a `ContentProvider`-backed repository and a `BroadcastReceiver` for SMS delivery, then replace the demo data with on-device messages.