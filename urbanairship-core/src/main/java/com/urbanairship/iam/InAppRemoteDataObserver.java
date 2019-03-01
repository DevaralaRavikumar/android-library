/* Copyright Urban Airship and Contributors */

package com.urbanairship.iam;

import android.content.Context;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.urbanairship.Logger;
import com.urbanairship.Predicate;
import com.urbanairship.PreferenceDataStore;
import com.urbanairship.UAirship;
import com.urbanairship.json.JsonException;
import com.urbanairship.json.JsonMap;
import com.urbanairship.json.JsonValue;
import com.urbanairship.reactive.Schedulers;
import com.urbanairship.reactive.Subscriber;
import com.urbanairship.reactive.Subscription;
import com.urbanairship.remotedata.RemoteData;
import com.urbanairship.remotedata.RemoteDataPayload;
import com.urbanairship.util.DateUtils;
import com.urbanairship.util.UAStringUtil;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Subscriber for {@link com.urbanairship.remotedata.RemoteData}.
 *
 * @hide
 */
class InAppRemoteDataObserver {

    private static final String IAM_PAYLOAD_TYPE = "in_app_messages";

    // JSON keys
    private static final String MESSAGES_JSON_KEY = "in_app_messages";
    private static final String CREATED_JSON_KEY = "created";
    private static final String UPDATED_JSON_KEY = "last_updated";

    // Data store keys
    private static final String LAST_PAYLOAD_TIMESTAMP_KEY = "com.urbanairship.iam.data.LAST_PAYLOAD_TIMESTAMP";
    private static final String LAST_PAYLOAD_METADATA = "com.urbanairship.iam.data.LAST_PAYLOAD_METADATA";
    private static final String SCHEDULED_MESSAGES_KEY = "com.urbanairship.iam.data.SCHEDULED_MESSAGES";
    private static final String SCHEDULE_NEW_USER_CUTOFF_TIME_KEY = "com.urbanairship.iam.data.NEW_USER_TIME";

    private final PreferenceDataStore preferenceDataStore;
    private Subscription subscription;
    private final List<Listener> listeners = new ArrayList<>();

    interface Listener {

        void onSchedulesUpdated();

    }

    /**
     * Default constructor.
     *
     * @param preferenceDataStore The preference data store.
     */
    InAppRemoteDataObserver(@NonNull PreferenceDataStore preferenceDataStore) {
        this.preferenceDataStore = preferenceDataStore;
    }

    /**
     * Adds a listener.
     * <p>
     * Updates will be called on the looper provided in
     * {@link #subscribe(RemoteData, Looper, InAppMessageScheduler)}.
     *
     * @param listener The listener to add.
     */
    void addListener(Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener.
     *
     * @param listener The listener to remove.
     */
    void removeListener(Listener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * Subscribes to remote data.
     *
     * @param remoteData The remote data.
     * @param looper The looper to process updates and callbacks on.
     * @param scheduler Scheduler.
     */
    void subscribe(@NonNull final RemoteData remoteData, @NonNull Looper looper, @NonNull final InAppMessageScheduler scheduler) {
        cancel();

        this.subscription = remoteData.payloadsForType(IAM_PAYLOAD_TYPE)
                                      .filter(new Predicate<RemoteDataPayload>() {
                                          @Override
                                          public boolean apply(@NonNull RemoteDataPayload payload) {
                                              if (payload.getTimestamp() != preferenceDataStore.getLong(LAST_PAYLOAD_TIMESTAMP_KEY, -1)) {
                                                  return true;
                                              }

                                              if (!payload.getMetadata().equals(getLastPayloadMetadata())) {
                                                  return true;
                                              }

                                              return false;
                                          }
                                      })
                                      .observeOn(Schedulers.looper(looper))
                                      .subscribe(new Subscriber<RemoteDataPayload>() {
                                          @Override
                                          public void onNext(@NonNull RemoteDataPayload payload) {
                                              try {
                                                  processPayload(payload, scheduler);
                                                  Logger.debug("InAppRemoteDataObserver - Finished processing messages.");
                                              } catch (Exception e) {
                                                  Logger.error(e, "InAppRemoteDataObserver - Failed to process payload: ");
                                              }
                                          }
                                      });
    }

    /**
     * Cancels the subscription.
     */
    void cancel() {
        if (this.subscription != null) {
            this.subscription.cancel();
        }
    }

    /**
     * Processes a payload.
     *
     * @param payload The remote data payload.
     * @param scheduler The scheduler.
     */
    private void processPayload(RemoteDataPayload payload, InAppMessageScheduler scheduler) throws ExecutionException, InterruptedException {
        long lastUpdate = preferenceDataStore.getLong(LAST_PAYLOAD_TIMESTAMP_KEY, -1);
        JsonMap lastPayloadMetadata = getLastPayloadMetadata();

        boolean isMetadataUpToDate = payload.getMetadata().equals(lastPayloadMetadata);

        List<String> messageIds = new ArrayList<>();
        List<InAppMessageScheduleInfo> newSchedules = new ArrayList<>();
        Map<String, String> scheduleIdMap = getScheduleIdMap();

        for (JsonValue messageJson : payload.getData().opt(MESSAGES_JSON_KEY).optList()) {
            long createdTimeStamp, lastUpdatedTimeStamp;

            try {
                createdTimeStamp = DateUtils.parseIso8601(messageJson.optMap().opt(CREATED_JSON_KEY).getString());
                lastUpdatedTimeStamp = DateUtils.parseIso8601(messageJson.optMap().opt(UPDATED_JSON_KEY).getString());
            } catch (ParseException e) {
                Logger.error(e, "Failed to parse in-app message timestamps: %s", messageJson);
                continue;
            }

            String messageId = InAppMessageScheduleInfo.parseMessageId(messageJson);
            if (UAStringUtil.isEmpty(messageId)) {
                Logger.error("Missing in-app message ID: %s", messageJson);
                continue;
            }

            messageIds.add(messageId);

            // Ignore any messages that have not updated since the last payload
            if (isMetadataUpToDate && lastUpdatedTimeStamp <= lastUpdate) {
                continue;
            }

            // If we do not have a schedule ID for the message ID, try to look it up first
            if (!scheduleIdMap.containsKey(messageId)) {
                Collection<InAppMessageSchedule> schedules = scheduler.getSchedules(messageId).get();

                if (schedules != null && !schedules.isEmpty()) {
                    // Make sure we only have a single schedule for the message ID
                    if (schedules.size() > 1) {
                        Logger.debug("InAppRemoteDataObserver - Duplicate schedules for in-app message: %s", messageId);
                        continue;
                    }

                    scheduleIdMap.put(messageId, schedules.iterator().next().getId());
                }
            }

            String existingScheduleId = scheduleIdMap.get(messageId);

            if (createdTimeStamp > lastUpdate) {
                try {
                    InAppMessageScheduleInfo scheduleInfo = InAppMessageScheduleInfo.fromJson(messageJson, InAppMessage.SOURCE_REMOTE_DATA);
                    if (checkSchedule(scheduleInfo, createdTimeStamp)) {
                        newSchedules.add(scheduleInfo);
                        Logger.debug("New in-app message: %s", scheduleInfo);
                    }
                } catch (JsonException e) {
                    Logger.error(e, "Failed to parse in-app message: %s", messageJson);
                }
            } else if (existingScheduleId != null) {
                try {
                    InAppMessageScheduleEdits originalEdits = InAppMessageScheduleEdits.fromJson(messageJson);
                    InAppMessageScheduleEdits edits = InAppMessageScheduleEdits.newBuilder(originalEdits)
                                                                               .setMetadata(payload.getMetadata())
                                                                               // Since we cancel a schedule by setting the end time to 0 (1970), we need to clear
                                                                               // it (-1) if the edits/schedule does not define an end time.
                                                                               .setEnd(originalEdits.getEnd() == null ? -1 : originalEdits.getEnd())
                                                                               .build();

                    InAppMessageSchedule schedule = scheduler.editSchedule(existingScheduleId, edits).get();
                    if (schedule != null) {
                        Logger.debug("Updated in-app message: %s with edits: %s", messageId, edits);
                    }
                } catch (JsonException e) {
                    Logger.error(e, "Failed to parse in-app message edits: %s", messageId);
                }
            }
        }

        // Schedule new in-app messages
        if (!newSchedules.isEmpty()) {
            List<InAppMessageSchedule> schedules = scheduler.schedule(newSchedules, payload.getMetadata()).get();
            if (schedules != null) {
                for (InAppMessageSchedule schedule : schedules) {
                    String messageId = schedule.getInfo().getInAppMessage().getId();
                    scheduleIdMap.put(messageId, schedule.getId());
                }
            }
        }

        // End any messages that are no longer in the listing
        Set<String> removedMessageIds = new HashSet<>(scheduleIdMap.keySet());
        removedMessageIds.removeAll(messageIds);

        if (!removedMessageIds.isEmpty()) {

            // To cancel, we need to set the end time to 0 (1970). To avoid validation error where
            // start needs to be before end if they are both set, we also need to clear the start.
            // If the schedule comes back, the edits will reapply the start time from the schedule
            // if it is set.
            InAppMessageScheduleEdits edits = InAppMessageScheduleEdits.newBuilder()
                                                                       .setMetadata(payload.getMetadata())
                                                                       .setStart(-1)
                                                                       .setEnd(0)
                                                                       .build();
            for (String messageId : removedMessageIds) {
                String scheduleId = scheduleIdMap.remove(messageId);
                if (scheduleId != null) {
                    scheduler.editSchedule(scheduleId, edits).get();
                }
            }
        }

        // Store data
        setScheduleIdMap(scheduleIdMap);
        preferenceDataStore.put(LAST_PAYLOAD_TIMESTAMP_KEY, payload.getTimestamp());
        preferenceDataStore.put(LAST_PAYLOAD_METADATA, payload.getMetadata());

        synchronized (listeners) {
            if (!listeners.isEmpty()) {
                List<Listener> listeners = new ArrayList<>(this.listeners);
                for (Listener listener : listeners) {
                    listener.onSchedulesUpdated();
                }
            }
        }
    }

    /**
     * Helper method to check if the message should be scheduled.
     *
     * @param scheduleInfo The schedule info.
     * @param createdTimeStamp The created times stamp.
     * @return {@code true} if the message should be scheduled, otherwise {@code false}.
     */
    private boolean checkSchedule(InAppMessageScheduleInfo scheduleInfo, long createdTimeStamp) {
        Context context = UAirship.getApplicationContext();
        Audience audience = scheduleInfo.getInAppMessage().getAudience();
        boolean allowNewUser = createdTimeStamp <= getScheduleNewUserCutOffTime();
        return AudienceChecks.checkAudienceForScheduling(context, audience, allowNewUser);
    }

    /**
     * Gets the message ID to schedule ID map.
     *
     * @return The ID map.
     */
    @NonNull
    private Map<String, String> getScheduleIdMap() {
        JsonMap jsonMap = preferenceDataStore.getJsonValue(SCHEDULED_MESSAGES_KEY).optMap();

        Map<String, String> idMap = new HashMap<>();
        for (Map.Entry<String, JsonValue> entry : jsonMap) {
            if (!entry.getValue().isString()) {
                continue;
            }

            idMap.put(entry.getKey(), entry.getValue().optString());
        }

        return idMap;
    }

    /**
     * Sets the message ID to schedule ID map.
     *
     * @param idMap The message ID to schedule ID map.
     */
    private void setScheduleIdMap(Map<String, String> idMap) {
        preferenceDataStore.put(SCHEDULED_MESSAGES_KEY, JsonValue.wrapOpt(idMap));
    }

    /**
     * Gets the schedule new user audience check cut off time.
     *
     * @return The new user cut off time.
     */
    long getScheduleNewUserCutOffTime() {
        return preferenceDataStore.getLong(SCHEDULE_NEW_USER_CUTOFF_TIME_KEY, -1);
    }

    /**
     * Sets the schedule new user cut off time. Any schedules that have
     * a new user condition will be dropped if the schedule create time is after the
     * cut off time.
     *
     * @param time The schedule new user cut off time.
     */
    void setScheduleNewUserCutOffTime(long time) {
        preferenceDataStore.put(SCHEDULE_NEW_USER_CUTOFF_TIME_KEY, time);
    }

    /**
     * Gets the last payload metadata.
     *
     * @return The last payload metadata.
     */
    public JsonMap getLastPayloadMetadata() {
        return preferenceDataStore.getJsonValue(LAST_PAYLOAD_METADATA).optMap();
    }

}
