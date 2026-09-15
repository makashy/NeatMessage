# NeatMessage

A privacy-first Android SMS manager focused on making suspicious messages easy to see and control, including sender IDs, short codes, and messages without a normal phone number.

## Current implementation

- Jetpack Compose inbox backed by the device SMS provider
- Runtime `READ_SMS` permission flow with an explicit empty/denied state
- All, Needs review, and Blocked filters
- Search and refresh controls
- Reply/new-message actions through the system SMS composer
- Local block/unblock actions for loaded messages
- Organization sender IDs and unknown senders handled without requiring a phone-number format
- A transparent keyword-based spam signal for the first working classifier

## Run

Open this folder in Android Studio with an Android SDK installed and sync the Gradle project. Run the `app` configuration on an Android 8.0+ emulator or device.

The app requests runtime SMS access and reads real messages from the device. Android requires an app to be the user's default SMS app for reliable incoming-message interception, deleting messages, and system-level blocking. The current block action is an in-app local state; a production release should add the default-SMS-app role flow, persistent rules, and a stronger classifier.