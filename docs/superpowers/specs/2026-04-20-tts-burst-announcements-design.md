# TTS Burst Announcements Design

## Summary

Change MQTT text-to-speech behavior so the app does not read every message individually during a short burst on the same topic. When more than 3 messages arrive for one topic within 5 seconds, the app should switch from per-message event announcements to a single summary announcement that states how many messages arrived for that topic.

## Goals

- Prevent noisy TTS when one topic produces a short burst of messages.
- Keep the current per-message announcement behavior for light traffic.
- Apply the burst rule any time the app is running, not only after broker connection.
- Scope the change per topic so one busy topic does not suppress announcements for another topic.

## Non-Goals

- Change notification behavior.
- Change message storage or unread-count behavior.
- Add user-configurable thresholds or timing.
- Suppress all speech once a burst starts without giving the user a summary.

## Current Behavior

`MqttSyncService` processes each incoming MQTT message and calls `MqttEventSpeaker.announceIncomingEvent(...)` once per message. `MqttEventSpeaker` builds a single phrase, `New event on <topic>`, and queues it to Android `TextToSpeech`. The speaker does not currently track burst timing or per-topic state.

## Proposed Behavior

For each topic, track a rolling 5-second burst window:

- messages 1 through 3 in the window keep the current per-message announcement behavior
- message 4 in the same window triggers a summary announcement instead of another per-message announcement
- later messages in that same active burst should continue to use the summary style rather than reintroducing per-message announcements
- once no message for that topic has arrived for more than 5 seconds, the next message starts a fresh burst and can be announced individually again

Example:

- 1 message in 5 seconds on `Front Door` -> `New event on Front Door`
- 3 messages in 5 seconds on `Front Door` -> three individual `New event on Front Door` announcements
- 4 messages in 5 seconds on `Front Door` -> first three individual announcements, then `There are 4 messages on Front Door`
- 6 messages in 5 seconds on `Front Door` -> no per-message speech after the threshold; the burst is represented with topic-count summary speech

## Architecture

Keep `MqttSyncService` unchanged at the integration boundary: it continues to hand every processed incoming message to `MqttEventSpeaker`.

Add burst aggregation logic inside `MqttEventSpeaker`, because that class already owns:

- announcement wording
- announcement queueing
- availability and pending initialization behavior

The speaker should maintain small per-topic burst state keyed by the spoken topic target. That state should contain:

- the active burst count
- the timestamp of the most recent event in the burst
- whether the burst has already switched into summary mode

## State Flow

For each incoming event:

1. Resolve the spoken topic target using the existing display-name fallback behavior.
2. Look up burst state for that topic.
3. If the last event is older than 5 seconds, reset the burst and treat this as the first event.
4. Increment the burst count.
5. Decide announcement mode:
   - counts 1-3: individual event announcement
   - count 4: summary announcement
   - count 5+: continue summary-mode behavior
6. Queue the chosen announcement through the existing TTS path.

## Announcement Copy

Keep the existing single-event phrase:

- `New event on Front Door`

Add a summary phrase for burst mode:

- `There are 4 messages on Front Door`

Use the same display-name fallback as the current implementation, so blank display names still speak the MQTT topic string.

## Implementation Shape

Keep Android-specific `TextToSpeech` integration unchanged as much as possible.

Extract the burst decision into small internal helper functions or small internal data structures inside `MqttEventSpeaker` so the behavior is unit-testable without relying on Android TTS. The public API can stay as `announceIncomingEvent(topicLabel, topic)`.

## Error Handling

- If TTS is unavailable, preserve current behavior and return without announcement.
- If TTS is not initialized yet, preserve current pending-announcement behavior, but queue the burst-aware final announcement strings rather than raw per-message defaults.
- If topic burst state grows stale, it should reset naturally when the 5-second gap is exceeded.

## Testing

Add unit tests for the burst decision logic:

- individual announcements remain unchanged for the first 3 messages within 5 seconds
- the 4th message within 5 seconds switches to a summary announcement
- additional messages in the same 5-second burst remain in summary mode
- after more than 5 seconds, a new message resets the burst back to individual mode
- summary wording uses display name first and falls back to the topic string when needed

Instrumentation coverage is not required for this rule because the change is deterministic logic in the speaker layer rather than UI behavior.
