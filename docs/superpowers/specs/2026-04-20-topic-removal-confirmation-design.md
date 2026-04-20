# Topic Removal Confirmation Design

## Summary

Add a confirmation dialog to the setup screen so topic removal is no longer immediate. When a user taps `Remove` for a saved topic, the app should show a modal confirmation step that clearly names the topic being removed before any data is deleted.

## Goals

- Prevent accidental topic deletion from the setup screen.
- Show the topic name in the confirmation copy so the user can verify the exact target.
- Keep the existing delete callback and repository behavior unchanged until the user confirms.
- Fit the change into the current Material 3 Compose patterns already used by `SetupScreen`.

## Non-Goals

- Change repository semantics for deleting topics.
- Add undo, soft-delete, or snackbar-based recovery.
- Introduce a broader screen state refactor for one local confirmation flow.

## Current Behavior

`SetupScreen` renders each saved topic with a `Remove` button. Pressing that button calls `onDeleteTopic(topic.id)` immediately, which removes the topic and updates the status message through `SetupScreenRoute`.

## Proposed Behavior

Pressing `Remove` should open a modal confirmation dialog. The dialog should:

- identify the topic using its display name
- include the raw MQTT topic string as supporting text when useful for clarity
- provide explicit cancel and confirm actions

Only the confirm action should call the existing `onDeleteTopic(topic.id)` callback. Cancel and dismiss actions should close the dialog without mutating topic data or status state.

## Architecture

The confirmation state should live inside `SetupScreen` as local UI state. This is a presentation concern tied to a single user gesture, so it does not need to be lifted into `SetupUiState` or repository code.

The existing route-level deletion flow remains the source of truth for actual removal:

1. `SetupScreen` stores the selected `SavedTopicUiModel` as pending deletion.
2. `SetupScreen` renders an `AlertDialog` while that state is non-null.
3. Confirm triggers `onDeleteTopic(pendingTopic.id)` and clears the pending state.
4. Cancel or dismiss clears the pending state only.

## Components

### `SetupScreen`

- Add local remembered state for the topic currently awaiting confirmation.
- Change the `Remove` button to populate that pending state instead of deleting immediately.
- Render a Material 3 `AlertDialog` when pending state exists.

### `SetupScreenRoute`

- No API change is required.
- The existing `onDeleteTopic(Long)` coroutine path continues to delete the topic, collapse expanded history for that topic, and update the status text.

## Dialog Content

- Title: direct removal confirmation language
- Body: include the topic display name and raw topic string so the user can verify the target
- Confirm action: `Remove`
- Dismiss action: `Cancel`

The copy should be short and specific rather than generic.

## Error Handling

- Cancel or outside dismiss should be a no-op beyond closing the dialog.
- If delete succeeds, the existing route logic should continue to show `Topic removed.`
- If delete fails in the future, this design does not add new error handling; it relies on the existing route behavior.

## Testing

Add Compose UI coverage for the new interaction:

- tapping `Remove` shows the confirmation dialog
- the dialog includes the topic name
- tapping `Cancel` does not call `onDeleteTopic`
- tapping the dialog confirm button does call `onDeleteTopic`

Existing topic-history tests should remain unchanged unless minor setup adjustments are needed for the new dialog state.
