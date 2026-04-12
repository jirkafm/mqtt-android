# README Design

## Summary

Create a polished repository `README.md` for `mqtt-android` that works for two audiences:

- people who want to install and use the app on an Android device
- developers who want to build, test, and run the project from the command line without relying on Android Studio

The README should follow common GitHub conventions used by established projects: a clear product-first introduction, quick scanning sections, concrete commands, and examples that help a first-time visitor understand the project quickly.

## Goals

- Explain what the application does in plain language.
- Show the main capabilities the current app actually supports.
- Provide a practical usage guide for configuring a broker and subscribed topics.
- Provide CLI-oriented build and run instructions.
- Include one concrete MQTT example that demonstrates the app's workflow.
- Include a screenshot if a real one can be produced from this repository during the task.
- Keep the README accurate to the current codebase and build setup.

## Non-Goals

- Document features that are not implemented yet.
- Add contributor process, release automation, or roadmap sections unless they are needed for clarity.
- Promise a screenshot if generating a real one is not feasible in the current environment.

## Audience Model

### End User

An Android user who wants to monitor MQTT topics, receive notifications, and review recent message history without reading the code.

### Developer

A developer who wants to clone the repository, satisfy local build requirements, run tests, build an APK from the terminal, and optionally install it on a device with `adb`.

## Content Structure

The README should use this structure:

1. Title and short description
2. Feature highlights
3. Screenshot section
4. Quick start for app users
5. Usage walkthrough
6. MQTT example
7. Build requirements
8. Build and test from CLI
9. Install and launch from CLI
10. Optional release signing note
11. Project layout

## Accuracy Constraints

- State that the app targets Android API level 29+ because `minSdk` is 29.
- State that the project builds with Java 17 because the Gradle configuration targets Java 17.
- Mention the actual package/application ID only where useful for CLI launch commands.
- Describe only the features visible in the code today: single broker configuration, topic subscriptions, foreground listener service, notifications, local message history, and text-to-speech announcements.
- Note that release signing is optional and depends on a local `keystore.properties` file.

## Screenshot Strategy

- First try to produce a real screenshot by building the app and using an attached device or emulator.
- If that fails, omit the image rather than inventing a fake in-app screenshot.
- If a screenshot is captured, store it under a repository asset path that is suitable for embedding from `README.md`.

## Verification

- Ensure the README commands align with the actual Gradle tasks already working in the repository.
- Run at least the existing unit test command after README changes to confirm nothing else was broken during the task.
- If a screenshot asset is added, verify the referenced path exists.
