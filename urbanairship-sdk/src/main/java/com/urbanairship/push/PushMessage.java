package com.urbanairship.push;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.urbanairship.Logger;
import com.urbanairship.actions.ActionValue;
import com.urbanairship.actions.OpenRichPushInboxAction;
import com.urbanairship.actions.OverlayRichPushMessageAction;
import com.urbanairship.json.JsonException;
import com.urbanairship.json.JsonMap;
import com.urbanairship.json.JsonValue;
import com.urbanairship.push.iam.InAppMessage;
import com.urbanairship.richpush.RichPushManager;
import com.urbanairship.util.UAMathUtil;
import com.urbanairship.util.UAStringUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A push message, usually created from handling a message intent from either GCM,
 * or another push notification service
 */
public class PushMessage implements Parcelable {

    /**
     * The ping extra indicates a push meant to test whether the application is active
     */
    static final String EXTRA_PING = "com.urbanairship.push.PING";

    /**
     * The alert extra holds the string sent in the "alert" field of an Urban Airship
     * Push Notification.
     */
    public static final String EXTRA_ALERT = "com.urbanairship.push.ALERT";

    /**
     * The push ID extra holds the unique push ID sent in an Urban Airship
     * Push Notification. This is most commonly referred to as the "Send ID"
     * at Urban Airship.
     */
    public static final String EXTRA_SEND_ID = "com.urbanairship.push.PUSH_ID";

    /**
     * The actions extra key holds the payload of actions to be performed with the
     * push.
     */
    public static final String EXTRA_ACTIONS = "com.urbanairship.actions";

    /**
     * The extra key for the payload of Urban Airship actions to be run when an
     * interactive notification action button is opened.
     */
    public static final String EXTRA_INTERACTIVE_ACTIONS = "com.urbanairship.interactive_actions";

    /**
     * The extra key for the interactive notification group that will be displayed with a push.
     */
    public static final String EXTRA_INTERACTIVE_TYPE = "com.urbanairship.interactive_type";

    /**
     * The extra key for the title of the notification.
     */
    public static final String EXTRA_TITLE = "com.urbanairship.title";

    /**
     * The extra key for the summary of the notification.
     */
    public static final String EXTRA_SUMMARY = "com.urbanairship.summary";

    /**
     * The extra key for the wearable payload.
     */
    public static final String EXTRA_WEARABLE = "com.urbanairship.wearable";

    /**
     * The extra key for the style of the notification.
     */
    public static final String EXTRA_STYLE = "com.urbanairship.style";

    /**
     * The extra key indicates if the notification should only be displayed on the device.
     */
    public static final String EXTRA_LOCAL_ONLY = "com.urbanairship.local_only";

    /**
     * The extra key for the priority of the notification. Acceptable values range from PRIORITY_MIN
     * (-2) to PRIORITY_MAX (2).
     * <p/>
     * Defaults to 0.
     */
    public static final String EXTRA_PRIORITY = "com.urbanairship.priority";

    /**
     * The minimum priority value for the notification.
     */
    static final int MIN_PRIORITY = -2;

    /**
     * The maximum priority value for the notification.
     */
    static final int MAX_PRIORITY = 2;

    /**
     * The extra key for the notification's visibility in the lockscreen. Acceptable values are:
     * VISIBILITY_PUBLIC (1), VISIBILITY_PRIVATE (0) or VISIBILITY_SECRET (-1).
     */
    public static final String EXTRA_VISIBILITY = "com.urbanairship.visibility";

    /**
     * The minimum visibility value for the notification in the lockscreen.
     */
    static final int MIN_VISIBILITY = -1;

    /**
     * The maximum visibility value for the notification in the lockscreen.
     */
    static final int MAX_VISIBILITY = 1;

    /**
     * Shows the notification's full content in the lockscreen. This is the default visibility.
     */
    static final int VISIBILITY_PUBLIC = 1;

    /**
     * The extra key for the public notification payload.
     */
    public static final String EXTRA_PUBLIC_NOTIFICATION = "com.urbanairship.public_notification";

    /**
     * The extra key for the category of the notification.
     */
    public static final String EXTRA_CATEGORY = "com.urbanairship.category";

    /**
     * The push ID extra is the ID assigned to a push at the time it is sent.
     * Each API call will result in a unique push ID, so all notifications that are part of a
     * multicast push will have the same push ID.
     */
    public static final String EXTRA_PUSH_ID = "com.urbanairship.push.CANONICAL_PUSH_ID";

    /**
     * The EXPIRATION extra is a time expressed in seconds since the Epoch after which, if specified, the
     * notification should not be delivered. It is removed from the notification before delivery to the
     * client. If not present, notifications may be delivered arbitrarily late.
     */
    public static final String EXTRA_EXPIRATION = "com.urbanairship.push.EXPIRATION";

    /**
     * The extra key for the {@link com.urbanairship.push.iam.InAppMessage} payload.
     */
    public static final String EXTRA_IN_APP_MESSAGE = "com.urbanairship.in_app";

    private static final List<String> INBOX_ACTION_NAMES = Arrays.asList(
            OpenRichPushInboxAction.DEFAULT_REGISTRY_NAME,
            OpenRichPushInboxAction.DEFAULT_REGISTRY_SHORT_NAME,
            OverlayRichPushMessageAction.DEFAULT_REGISTRY_NAME,
            OverlayRichPushMessageAction.DEFAULT_REGISTRY_SHORT_NAME);

    private final Bundle pushBundle;

    /**
     * Create a new PushMessage
     *
     * @param pushBundle The intent extras for the push
     */
    public PushMessage(Bundle pushBundle) {
        this.pushBundle = pushBundle;
    }

    /**
     * Checks if the expiration exists and is expired
     *
     * @return <code>true</code> if the message is expired, otherwise <code>false</code>
     */
    boolean isExpired() {
        String expirationStr = pushBundle.getString(EXTRA_EXPIRATION);
        if (!UAStringUtil.isEmpty(expirationStr)) {
            Logger.debug("Notification expiration time is \"" + expirationStr + "\"");
            try {
                long expiration = Long.parseLong(expirationStr) * 1000;
                if (expiration < System.currentTimeMillis()) {
                    return true;
                }
            } catch (NumberFormatException e) {
                Logger.debug("Ignoring malformed expiration time: " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Checks if the message is ping or not
     *
     * @return <code>true</code> if the message is a ping to test if
     * application is active, otherwise <code>false</code>
     */
    boolean isPing() {
        return pushBundle.get(EXTRA_PING) != null;
    }

    /**
     * Gets the message's canonical push ID
     *
     * @return The canonical push ID
     */
    @Nullable
    public String getCanonicalPushId() {
        return pushBundle.getString(EXTRA_PUSH_ID);
    }

    /**
     * Gets the rich push message ID
     *
     * @return The rich push message ID, or null if its unavailable.
     */
    @Nullable
    public String getRichPushMessageId() {
        return pushBundle.getString(RichPushManager.RICH_PUSH_KEY);
    }

    /**
     * Gets the notification alert
     *
     * @return The notification alert.
     */
    @Nullable
    public String getAlert() {
        return pushBundle.getString(EXTRA_ALERT);
    }

    /**
     * Gets the push send ID
     *
     * @return The push send Id.
     */
    @Nullable
    public String getSendId() {
        return pushBundle.getString(EXTRA_SEND_ID);
    }

    /**
     * Returns a bundle of all the push extras
     *
     * @return A bundle of all the push extras
     */
    @NonNull
    public Bundle getPushBundle() {
        return new Bundle(pushBundle);
    }

    /**
     * Gets the push message's actions.
     *
     * @return A map of action name to action value.
     */
    @NonNull
    public Map<String, ActionValue> getActions() {
        String actionsPayload = pushBundle.getString(EXTRA_ACTIONS);
        Map<String, ActionValue> actions = new HashMap<>();

        try {
            JsonMap actionsJson = JsonValue.parseString(actionsPayload).getMap();
            if (actionsJson != null) {
                for (Map.Entry<String, JsonValue> entry : actionsJson) {
                    actions.put(entry.getKey(), new ActionValue(entry.getValue()));
                }
            }
        } catch (JsonException e) {
            Logger.error("Unable to parse action payload: " + actionsPayload);
            return actions;
        }

        if (!UAStringUtil.isEmpty(getRichPushMessageId())) {
            if (Collections.disjoint(actions.keySet(), INBOX_ACTION_NAMES)) {
                actions.put(OpenRichPushInboxAction.DEFAULT_REGISTRY_SHORT_NAME, ActionValue.wrap(getRichPushMessageId()));
            }
        }

        return actions;
    }

    /**
     * Gets the notification actions payload.
     *
     * @return The notification actions payload.
     */
    @Nullable
    public String getInteractiveActionsPayload() {
        return pushBundle.getString(EXTRA_INTERACTIVE_ACTIONS);
    }

    /**
     * Gets the notification action button type.
     *
     * @return The interactive notification type.
     */
    @Nullable
    public String getInteractiveNotificationType() {
        return pushBundle.getString(EXTRA_INTERACTIVE_TYPE);
    }

    /**
     * Gets the title of the notification.
     *
     * @return The title of the notification.
     */
    @Nullable
    public String getTitle() {
        return pushBundle.getString(EXTRA_TITLE);
    }

    /**
     * Gets the summary of the notification.
     *
     * @return The summary of the notification.
     */
    @Nullable
    public String getSummary() {
        return pushBundle.getString(EXTRA_SUMMARY);
    }

    /**
     * Gets the wearable payload.
     *
     * @return The wearable payload.
     */
    @Nullable
    public String getWearablePayload() {
        return pushBundle.getString(EXTRA_WEARABLE);
    }

    /**
     * Gets the style payload of the notification.
     *
     * @return The style payload of the notification.
     */
    @Nullable
    public String getStylePayload() {
        return pushBundle.getString(EXTRA_STYLE);
    }

    /**
     * Checks if the notification should only be displayed on the device.
     *
     * @return <code>true</code> if the notification should only be displayed on the device,
     * otherwise <code>false</code>
     * <p/>
     * Defaults to false.
     */
    public boolean isLocalOnly() {
        String value = pushBundle.getString(EXTRA_LOCAL_ONLY);
        return Boolean.parseBoolean(value);
    }

    /**
     * Gets the priority of the notification.
     * <p/>
     * Defaults to 0.
     *
     * @return The priority of the notification.
     */
    public int getPriority() {
        try {
            String value = pushBundle.getString(EXTRA_PRIORITY);
            return UAMathUtil.constrain(Integer.parseInt(value), MIN_PRIORITY, MAX_PRIORITY);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Gets the visibility of the notification for the lockscreen.
     * <p/>
     * Defaults to 1 (VISIBILITY_PUBLIC).
     *
     * @return The visibility of the notification for the lockscreen.
     */
    public int getVisibility() {
        try {
            String value = pushBundle.getString(EXTRA_VISIBILITY);
            return UAMathUtil.constrain(Integer.parseInt(value), MIN_VISIBILITY, MAX_VISIBILITY);
        } catch (NumberFormatException e) {
            return VISIBILITY_PUBLIC;
        }
    }

    /**
     * Gets the public notification payload.
     *
     * @return The public notification payload.
     */
    @Nullable
    public String getPublicNotificationPayload() {
        return pushBundle.getString(EXTRA_PUBLIC_NOTIFICATION);
    }

    /**
     * Gets the category of the notification.
     *
     * @return The category of the notification.
     */
    @Nullable
    public String getCategory() {
        return pushBundle.getString(EXTRA_CATEGORY);
    }

    /**
     * Gets the {@link com.urbanairship.push.iam.InAppMessage} from the push bundle.
     *
     * @return The in-app message.
     */
    @Nullable
    public InAppMessage getInAppMessage() {
        if (pushBundle.containsKey(EXTRA_IN_APP_MESSAGE)) {
            try {
                InAppMessage rawMessage = InAppMessage.parseJson(pushBundle.getString(EXTRA_IN_APP_MESSAGE));
                if (rawMessage == null) {
                    return null;
                }

                InAppMessage.Builder builder = new InAppMessage.Builder(rawMessage)
                        .setId(getSendId());

                if (!UAStringUtil.isEmpty(getRichPushMessageId())) {
                    if (Collections.disjoint(rawMessage.getClickActionValues().keySet(), INBOX_ACTION_NAMES)) {
                        HashMap<String, ActionValue> actions = new HashMap<>(rawMessage.getClickActionValues());
                        actions.put(OpenRichPushInboxAction.DEFAULT_REGISTRY_SHORT_NAME, new ActionValue(JsonValue.wrap(getRichPushMessageId())));
                        builder.setClickActionValues(actions);
                    }
                }

                return builder.create();

            } catch (JsonException e) {
                Logger.error("PushMessage - unable to create in-app message from push payload", e);
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return pushBundle.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeBundle(pushBundle);
    }

    /**
     * Parcel Creator for push messages.
     */
    public static final Parcelable.Creator<PushMessage> CREATOR = new Parcelable.Creator<PushMessage>() {

        @Override
        public PushMessage createFromParcel(Parcel in) {
            return new PushMessage(in.readBundle());
        }

        @Override
        public PushMessage[] newArray(int size) {
            return new PushMessage[size];
        }
    };
}
