/* Copyright Airship and Contributors */

package com.urbanairship;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.XmlRes;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.urbanairship.push.PushProvider;
import com.urbanairship.util.Checks;
import com.urbanairship.util.ConfigParser;
import com.urbanairship.util.PropertiesConfigParser;
import com.urbanairship.util.UAStringUtil;
import com.urbanairship.util.XmlConfigParser;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * This class holds the set of options necessary to properly initialize
 * {@link com.urbanairship.UAirship}.
 */
public class AirshipConfigOptions {

    private static final int DEFAULT_PRODUCTION_LOG_LEVEL = Log.ERROR;
    private static final int DEFAULT_DEVELOPMENT_LOG_LEVEL = Log.DEBUG;

    /**
     * The ADM transport type for Push.
     */
    @NonNull
    public static final String ADM_TRANSPORT = "ADM";

    /**
     * The FCM transport type for Push.
     */
    @NonNull
    public static final String FCM_TRANSPORT = "FCM";

    // Logs a warning message if the backgroundReportingIntervalSeconds is below this minimum value
    private final static int MIN_BG_REPORTING_INTERVAL_MS = 60 * 1000; // 1 minute

    // Logs a warning message if the backgroundReportingIntervalSeconds is above this maximum value
    private final static int MAX_BG_REPORTING_INTERVAL_MS = 24 * 60 * 60 * 1000; // 24 hours

    // Default airship config properties filename
    private final static String DEFAULT_PROPERTIES_FILENAME = "airshipconfig.properties";

    private static final String CONFIG_ELEMENT = "AirshipConfigOptions";

    /**
     * The application's production app key.
     * <p>
     * This string is generated automatically when you create an app in the Airship
     * dashboard, which you can manually copy into your app configuration.
     */
    @Nullable
    public final String productionAppKey;

    /**
     * The application's production app secret.
     * <p>
     * This string is generated automatically when you create an app in the Airship
     * dashboard, which you can manually copy into your app configuration.
     */
    @Nullable
    public final String productionAppSecret;

    /**
     * The application's development app key.
     * <p>
     * This string is generated automatically when you create an app in the Airship
     * dashboard, which you can manually copy into your app configuration.
     */
    @Nullable
    public final String developmentAppKey;

    /**
     * The application's development app secret.
     * <p>
     * This string is generated automatically when you create an app in the Airship
     * dashboard, which you can manually copy into your app configuration.
     */
    @Nullable
    public final String developmentAppSecret;

    /**
     * The Airship URL. This will always be set to http://device-api.urbanairship.com/
     */
    @NonNull
    public final String hostURL;

    /**
     * The Analytics Server. This will always be set to https://combine.urbanairship.com/
     */
    @NonNull
    public final String analyticsServer;

    /**
     * The remote data server URL.
     */
    @NonNull
    public final String remoteDataURL;

    /**
     * The FCM sender ID for push registration. Used as a fallback
     * if the production or development FCM sender ID is not set.
     * This is your Google API project number.
     * <p>
     * Optional if you are using `urbanairship-fcm` package and want Airship to use the
     * main Firebase application's sender ID.
     */
    @Nullable
    public final String fcmSenderId;

    /**
     * The FCM sender ID used for push registration in development mode.
     * This is your Google API project number.
     * <p>
     * Optional if you are using `urbanairship-fcm` package and want Airship to use the
     * main Firebase application's sender ID.
     */
    @Nullable
    public final String developmentFcmSenderId;

    /**
     * The FCM sender ID used for push registration in production mode.
     * This is your Google API project number.
     * <p>
     * Optional if you are using `urbanairship-fcm` package and want Airship to use the
     * main Firebase application's sender ID.
     */
    @Nullable
    public final String productionFcmSenderId;

    /**
     * The transport types allowed for Push.
     * <p>
     * Defaults to ADM, FCM.
     */
    @Nullable
    public final String[] allowedTransports;

    /**
     * Custom push provider.
     *
     * @hide
     */
    @Nullable
    public final PushProvider customPushProvider;

    /**
     * List of URLs that are allowed to be used for various features, including:
     * Airship JS interface, open external URL action, wallet action, HTML in-app messages,
     * and landing pages. Airship https URLs are included by default.
     * <p>
     * See {@link com.urbanairship.js.Whitelist#addEntry(String)} for valid url patterns.
     * <p>
     * Defaults null.
     */
    @Nullable
    public final String[] whitelist;

    /**
     * Enables/disables whitelist checks for {@link com.urbanairship.js.Whitelist#SCOPE_OPEN_URL}.
     * If disabled, any URL checks with scope {@link com.urbanairship.js.Whitelist#SCOPE_OPEN_URL} will
     * be allowed even if the URL is not in the whitelist.
     * <p>
     * Defaults to false.
     */
    public final boolean enableUrlWhitelisting;

    /**
     * Flag indicating whether the application is in production or development.
     * <p>
     * Defaults to <code>false</code>.
     */
    public final boolean inProduction;

    /**
     * Flag indicating whether the application will use analytics.
     * <p>
     * The flag defaults to true.
     */
    public final boolean analyticsEnabled;

    /**
     * Minimum delta in milliseconds between analytics uploads when
     * adding location events while in the background.
     * <p>
     * Defaults to 15 minutes.
     */
    public final long backgroundReportingIntervalMS;

    /**
     * Flag indicating whether to clear an existing named user during a re-install.
     * <p>
     * Defaults to <code>false</code>.
     */
    public final boolean clearNamedUser;

    /**
     * Logger level when the application is in debug mode. Possible values are:
     * <br><ul>
     * <li>ASSERT
     * <li>NONE
     * <li>DEBUG
     * <li>ERROR
     * <li>INFO
     * <li>VERBOSE
     * <li>WARN
     * </ul><br>
     * Defaults to <code>DEBUG</code>
     */
    public final int developmentLogLevel;

    /**
     * Logger level when the application is in production mode. Possible values are:
     * <br><ul>
     * <li>ASSERT
     * <li>NONE
     * <li>DEBUG
     * <li>ERROR
     * <li>INFO
     * <li>VERBOSE
     * <li>WARN
     * </ul><br>
     * Defaults to <code>ERROR</code>
     */
    public final int productionLogLevel;

    /**
     * Flag indicating whether or not to launch the launcher activity when a push notification or push
     * notification button is opened and the application intent receiver did not launch an activity.
     * <p>
     * Defaults to true.
     */
    public final boolean autoLaunchApplication;

    /**
     * Flag indicating whether channel creation delay is enabled or not.
     * <p>
     * The flag defaults to false.
     */
    public final boolean channelCreationDelayEnabled;

    /**
     * Flag indicating whether channel capture feature is enabled or not.
     * <p>
     * The flag defaults to true.
     */
    public final boolean channelCaptureEnabled;

    /**
     * Notification icon.
     */
    @DrawableRes
    public final int notificationIcon;

    /**
     * Large notification icon.
     */
    @DrawableRes
    public final int notificationLargeIcon;

    /**
     * The Wallet URL. This will always be set to https://wallet-api.urbanairship.com
     */
    @NonNull
    public final String walletUrl;

    /**
     * Notification accent color.
     */
    @ColorInt
    public final int notificationAccentColor;

    /**
     * The default notification channel.
     */
    @Nullable
    public final String notificationChannel;

    /**
     * Optional app store link when using the rate app action. If not set,
     * the action will generate it using hte app's current package name.
     * <p>
     * Example: "market://details?id=com.example.android"
     */
    @Nullable
    public final Uri appStoreUri;

    private AirshipConfigOptions(@NonNull Builder builder) {
        this.productionAppKey = builder.productionAppKey;
        this.productionAppSecret = builder.productionAppSecret;
        this.developmentAppKey = builder.developmentAppKey;
        this.developmentAppSecret = builder.developmentAppSecret;
        this.hostURL = builder.hostURL;
        this.analyticsServer = builder.analyticsServer;
        this.remoteDataURL = builder.remoteDataURL;
        this.fcmSenderId = builder.fcmSenderId;
        this.developmentFcmSenderId = builder.developmentFcmSenderId;
        this.productionFcmSenderId = builder.productionFcmSenderId;
        this.allowedTransports = builder.allowedTransports;
        this.whitelist = builder.whitelist;
        this.inProduction = builder.inProduction;
        this.analyticsEnabled = builder.analyticsEnabled;
        this.backgroundReportingIntervalMS = builder.backgroundReportingIntervalMS;
        this.clearNamedUser = builder.clearNamedUser;
        this.developmentLogLevel = builder.developmentLogLevel;
        this.productionLogLevel = builder.productionLogLevel;
        this.autoLaunchApplication = builder.autoLaunchApplication;
        this.channelCreationDelayEnabled = builder.channelCreationDelayEnabled;
        this.channelCaptureEnabled = builder.channelCaptureEnabled;
        this.notificationIcon = builder.notificationIcon;
        this.notificationLargeIcon = builder.notificationLargeIcon;
        this.notificationAccentColor = builder.notificationAccentColor;
        this.walletUrl = builder.walletUrl;
        this.notificationChannel = builder.notificationChannel;
        this.enableUrlWhitelisting = builder.enableUrlWhitelisting;
        this.customPushProvider = builder.customPushProvider;
        this.appStoreUri = builder.appStoreUri;
    }

    /**
     * Returns the appropriate development or production app key
     *
     * @return The application key
     */
    @NonNull
    public String getAppKey() {
        //noinspection ConstantConditions
        return inProduction ? productionAppKey : developmentAppKey;
    }

    /**
     * Returns the appropriate development or production app secret
     *
     * @return The application secret
     */
    @NonNull
    public String getAppSecret() {
        //noinspection ConstantConditions
        return inProduction ? productionAppSecret : developmentAppSecret;
    }

    /**
     * Returns the appropriate development or production log level.
     *
     * @return The log level
     */
    public int getLoggerLevel() {
        return inProduction ? productionLogLevel : developmentLogLevel;
    }

    /**
     * Returns the development or production FCM sender ID.
     *
     * @return The FCM sender ID.
     */
    @Nullable
    public String getFcmSenderId() {
        String senderId = inProduction ? productionFcmSenderId : developmentFcmSenderId;

        if (senderId != null) {
            return senderId;
        }

        if (fcmSenderId != null) {
            return fcmSenderId;
        }

        return null;
    }

    /**
     * Check to see if the specified transport type is allowed.
     *
     * @param transport The transport type.
     * @return <code>true</code> if the transport type is allowed, otherwise <code>false</code>.
     */
    public boolean isTransportAllowed(@Nullable String transport) {
        if (allowedTransports == null || transport == null) {
            return false;
        }

        for (String allowedTransport : allowedTransports) {
            if (transport.equalsIgnoreCase(allowedTransport)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Airship config builder.
     */
    public static final class Builder {

        /*
         * Common config fields
         */
        private static final String FIELD_PRODUCTION_APP_KEY = "productionAppKey";
        private static final String FIELD_PRODUCTION_APP_SECRET = "productionAppSecret";
        private static final String FIELD_DEVELOPMENT_APP_KEY = "developmentAppKey";
        private static final String FIELD_DEVELOPMENT_APP_SECRET = "developmentAppSecret";
        private static final String FIELD_HOST_URL = "hostURL";
        private static final String FIELD_ANALYTICS_SERVER = "analyticsServer";
        private static final String FIELD_LANDING_PAGE_CONTENT_URL = "landingPageContentURL";
        private static final String FIELD_REMOTE_DATA_URL = "remoteDataURL";
        private static final String FIELD_GCM_SENDER = "gcmSender";
        private static final String FIELD_ALLOWED_TRANSPORTS = "allowedTransports";
        private static final String FIELD_WHITELIST = "whitelist";
        private static final String FIELD_IN_PRODUCTION = "inProduction";
        private static final String FIELD_ANALYTICS_ENABLED = "analyticsEnabled";
        private static final String FIELD_BACKGROUND_REPORTING_INTERVAL_MS = "backgroundReportingIntervalMS";
        private static final String FIELD_CLEAR_NAMED_USER = "clearNamedUser";
        private static final String FIELD_DEVELOPMENT_LOG_LEVEL = "developmentLogLevel";
        private static final String FIELD_PRODUCTION_LOG_LEVEL = "productionLogLevel";
        private static final String FIELD_AUTO_LAUNCH_APPLICATION = "autoLaunchApplication";
        private static final String FIELD_CHANNEL_CREATION_DELAY_ENABLED = "channelCreationDelayEnabled";
        private static final String FIELD_CHANNEL_CAPTURE_ENABLED = "channelCaptureEnabled";
        private static final String FIELD_NOTIFICATION_ICON = "notificationIcon";
        private static final String FIELD_NOTIFICATION_LARGE_ICON = "notificationLargeIcon";
        private static final String FIELD_NOTIFICATION_ACCENT_COLOR = "notificationAccentColor";
        private static final String FIELD_WALLET_URL = "walletUrl";
        private static final String FIELD_NOTIFICATION_CHANNEL = "notificationChannel";
        private static final String FIELD_FCM_SENDER_ID = "fcmSenderId";
        private static final String FIELD_PRODUCTION_FCM_SENDER_ID = "productionFcmSenderId";
        private static final String FIELD_DEVELOPMENT_FCM_SENDER_ID = "developmentFcmSenderId";
        private static final String FIELD_ENABLE_URL_WHITELISTING = "enableUrlWhitelisting";
        private static final String FIELD_CUSTOM_PUSH_PROVIDER = "customPushProvider";
        private static final String FIELD_APP_STORE_URI = "appStoreUri";

        private String productionAppKey;
        private String productionAppSecret;
        private String developmentAppKey;
        private String developmentAppSecret;
        private String hostURL = "https://device-api.urbanairship.com/";
        private String analyticsServer = "https://combine.urbanairship.com/";
        private String remoteDataURL = "https://remote-data.urbanairship.com/";
        private String fcmSenderId;
        private String productionFcmSenderId;
        private String developmentFcmSenderId;
        private String[] allowedTransports = new String[] { ADM_TRANSPORT, FCM_TRANSPORT };
        private String[] whitelist = null;
        private Boolean inProduction = null;
        private boolean analyticsEnabled = true;
        private long backgroundReportingIntervalMS = 15 * 60 * 1000;
        private boolean clearNamedUser = false;
        private int developmentLogLevel = DEFAULT_DEVELOPMENT_LOG_LEVEL;
        private int productionLogLevel = DEFAULT_PRODUCTION_LOG_LEVEL;
        private boolean autoLaunchApplication = true;
        private boolean channelCreationDelayEnabled = false;
        private boolean channelCaptureEnabled = true;
        private int notificationIcon;
        public int notificationLargeIcon;
        private int notificationAccentColor = NotificationCompat.COLOR_DEFAULT;
        private String walletUrl = "https://wallet-api.urbanairship.com";
        private String notificationChannel;
        private boolean enableUrlWhitelisting;
        private PushProvider customPushProvider;
        private Uri appStoreUri;

        /**
         * Apply the options from the default properties file {@code airshipconfig.properties}.
         * <p>
         * See {@link #applyProperties(Context, String)}.
         *
         * @param context The application context
         * @return The config option builder.
         */
        @NonNull
        public Builder applyDefaultProperties(@NonNull Context context) {
            return applyProperties(context, DEFAULT_PROPERTIES_FILENAME);
        }

        /**
         * Apply the options from a given properties file. The properties file should
         * be available in the assets directory. The properties file can define any of the
         * public {@link AirshipConfigOptions} fields. Example:
         * <pre>
         * {@code
         * # App Credentials
         * developmentAppKey = Your Development App Key
         * developmentAppSecret = Your Development App Secret
         * productionAppKey = Your Production App Key
         * productionAppSecret = Your Production Secret
         *
         * # Flag to indicate what credentials to use
         * inProduction = false
         *
         * # Log levels
         * developmentLogLevel = DEBUG
         * productionLogLevel = ERROR
         *
         * # Notification settings
         * notificationIcon = ic_notification
         * notificationAccentColor = #ff0000
         *
         * }
         * </pre>
         *
         * @param context The application context.
         * @param propertiesFile The name of the properties file in the assets directory.
         * @return The config option builder.
         */
        @NonNull
        public Builder applyProperties(@NonNull Context context, @NonNull String propertiesFile) {
            try {
                ConfigParser configParser = PropertiesConfigParser.fromAssets(context, propertiesFile);
                applyConfigParser(context, configParser);
            } catch (Exception e) {
                Logger.error(e, "AirshipConfigOptions - Unable to apply config.");
            }

            return this;
        }

        /**
         * Applies properties from a given Properties object.
         *
         * @param context The application context.
         * @param properties The properties
         * @return The config option builder.
         */
        @NonNull
        public Builder applyProperties(@NonNull Context context, @NonNull Properties properties) {
            try {
                ConfigParser configParser = PropertiesConfigParser.fromProperties(context, properties);
                applyConfigParser(context, configParser);
            } catch (Exception e) {
                Logger.error(e, "AirshipConfigOptions - Unable to apply config.");
            }

            return this;
        }

        /**
         * Apply options from a xml resource file. The XML file must contain the element {@code AirshipConfigOptions}
         * and any public {@link AirshipConfigOptions} fields should be set as attributes on the element.
         * Example:
         * <pre>
         * {@code
         * <AirshipConfigOptions
         *    notificationIcon = "@drawable/ic_notification"
         *    notificationAccentColor = "@color/color_accent"
         *    inProduction = "false"
         *    productionAppKey = "Your Production App Key"
         *    productionAppSecret = "Your Production App Secret"
         *    productionLogLevel = "NONE"
         *    developmentAppKey = "Your Development App Key"
         *    developmentAppSecret = "Your Development App Secret"
         *    developmentLogLevel = "VERBOSE"
         * }
         * </pre>
         *
         * @param context The application context.
         * @param xmlResourceId The xml resource ID.
         * @return The config option builder.
         */
        @NonNull
        public Builder applyConfig(@NonNull Context context, @XmlRes int xmlResourceId) {
            XmlConfigParser configParser = null;
            try {
                configParser = XmlConfigParser.parseElement(context, xmlResourceId, CONFIG_ELEMENT);
                applyConfigParser(context, configParser);
            } catch (Exception e) {
                Logger.error(e, "AirshipConfigOptions - Unable to apply config.");
            } finally {
                if (configParser != null) {
                    configParser.close();
                }
            }

            return this;
        }

        /**
         * Applies a value to the builder.
         *
         * @param configParser The config parser.
         */
        private void applyConfigParser(Context context, ConfigParser configParser) {
            for (int i = 0; i < configParser.getCount(); i++) {
                try {
                    String name = configParser.getName(i);
                    if (name == null) {
                        continue;
                    }
                    switch (name) {
                        case FIELD_PRODUCTION_APP_KEY:
                            this.setProductionAppKey(configParser.getString(name));
                            break;

                        case FIELD_PRODUCTION_APP_SECRET:
                            this.setProductionAppSecret(configParser.getString(name));
                            break;

                        case FIELD_DEVELOPMENT_APP_KEY:
                            this.setDevelopmentAppKey(configParser.getString(name));
                            break;

                        case FIELD_DEVELOPMENT_APP_SECRET:
                            this.setDevelopmentAppSecret(configParser.getString(name));
                            break;

                        case FIELD_HOST_URL:
                            this.setHostURL(configParser.getString(name, hostURL));
                            break;

                        case FIELD_ANALYTICS_SERVER:
                            this.setAnalyticsServer(configParser.getString(name, analyticsServer));
                            break;

                        case FIELD_REMOTE_DATA_URL:
                            this.setRemoteDataURL(configParser.getString(name));
                            break;

                        case FIELD_GCM_SENDER:
                            throw new IllegalArgumentException("gcmSender no longer supported. Please use " +
                                    "fcmSender or remove it to allow the Airship SDK to pull from the google-services.json.");

                        case FIELD_ALLOWED_TRANSPORTS:
                            this.setAllowedTransports(configParser.getStringArray(name));
                            break;

                        case FIELD_WHITELIST:
                            this.setWhitelist(configParser.getStringArray(name));
                            break;

                        case FIELD_IN_PRODUCTION:
                            this.setInProduction(configParser.getBoolean(name, inProduction));
                            break;

                        case FIELD_ANALYTICS_ENABLED:
                            this.setAnalyticsEnabled(configParser.getBoolean(name, analyticsEnabled));
                            break;

                        case FIELD_BACKGROUND_REPORTING_INTERVAL_MS:
                            this.setBackgroundReportingIntervalMS(configParser.getLong(name, backgroundReportingIntervalMS));
                            break;

                        case FIELD_CLEAR_NAMED_USER:
                            this.setClearNamedUser(configParser.getBoolean(name, clearNamedUser));
                            break;

                        case FIELD_DEVELOPMENT_LOG_LEVEL:
                            this.setDevelopmentLogLevel(Logger.parseLogLevel(configParser.getString(name), developmentLogLevel));
                            break;

                        case FIELD_PRODUCTION_LOG_LEVEL:
                            this.setProductionLogLevel(Logger.parseLogLevel(configParser.getString(name), productionLogLevel));
                            break;

                        case FIELD_AUTO_LAUNCH_APPLICATION:
                            this.setAutoLaunchApplication(configParser.getBoolean(name, autoLaunchApplication));
                            break;

                        case FIELD_CHANNEL_CREATION_DELAY_ENABLED:
                            this.setChannelCreationDelayEnabled(configParser.getBoolean(name, channelCreationDelayEnabled));
                            break;

                        case FIELD_CHANNEL_CAPTURE_ENABLED:
                            this.setChannelCaptureEnabled(configParser.getBoolean(name, channelCaptureEnabled));
                            break;

                        case FIELD_NOTIFICATION_ICON:
                            this.setNotificationIcon(configParser.getDrawableResourceId(name));
                            break;

                        case FIELD_NOTIFICATION_LARGE_ICON:
                            this.setNotificationLargeIcon(configParser.getDrawableResourceId(name));
                            break;

                        case FIELD_NOTIFICATION_ACCENT_COLOR:
                            this.setNotificationAccentColor(configParser.getColor(name, notificationAccentColor));
                            break;

                        case FIELD_WALLET_URL:
                            this.setWalletUrl(configParser.getString(name, walletUrl));
                            break;

                        case FIELD_NOTIFICATION_CHANNEL:
                            this.setNotificationChannel(configParser.getString(name));
                            break;

                        case FIELD_FCM_SENDER_ID:
                            this.setFcmSenderId(configParser.getString(name));
                            break;

                        case FIELD_DEVELOPMENT_FCM_SENDER_ID:
                            this.setDevelopmentFcmSenderId(configParser.getString(name));
                            break;

                        case FIELD_PRODUCTION_FCM_SENDER_ID:
                            this.setProductionFcmSenderId(configParser.getString(name));
                            break;

                        case FIELD_ENABLE_URL_WHITELISTING:
                            this.setEnableUrlWhitelisting(configParser.getBoolean(name, enableUrlWhitelisting));
                            break;

                        case FIELD_CUSTOM_PUSH_PROVIDER:
                            String className = configParser.getString(name);
                            Checks.checkNotNull(className, "Missing custom push provider class name");
                            Class<? extends PushProvider> providerClass = Class.forName(className).asSubclass(PushProvider.class);
                            this.setCustomPushProvider(providerClass.newInstance());
                            break;

                        case FIELD_APP_STORE_URI:
                            this.setAppStoreUri(Uri.parse(configParser.getString(name)));
                            break;
                    }
                } catch (Exception e) {
                    Logger.error(e, "Unable to set config field '%s' due to invalid configuration value.", configParser.getName(i));
                }
            }

            // Determine build mode if not specified in config file.
            if (inProduction == null) {
                detectProvisioningMode(context);
            }
        }

        /**
         * Sets the default notification channel.
         * <p>
         * See {@link com.urbanairship.push.notifications.NotificationFactory#setNotificationChannel(String)}.
         *
         * @param channel The notification channel.
         * @return The config options builder.
         */
        @NonNull
        public Builder setNotificationChannel(@Nullable String channel) {
            this.notificationChannel = channel;
            return this;
        }

        /**
         * Sets the default notification Icon.
         * <p>
         * See {@link com.urbanairship.push.notifications.DefaultNotificationFactory#setSmallIconId(int)}.
         *
         * @param notificationIcon The notification icon.
         * @return The config options builder.
         */
        @NonNull
        public Builder setNotificationIcon(@DrawableRes int notificationIcon) {
            this.notificationIcon = notificationIcon;
            return this;
        }

        /**
         * Sets the large notification Icon.
         * <p>
         * See {@link com.urbanairship.push.notifications.DefaultNotificationFactory#setSmallIconId(int)}.
         *
         * @param notificationLargeIcon The large notification icon.
         * @return The config options builder.
         */
        @NonNull
        public Builder setNotificationLargeIcon(@DrawableRes int notificationLargeIcon) {
            this.notificationLargeIcon = notificationLargeIcon;
            return this;
        }

        /**
         * Sets the default notification accent color.
         * <p>
         * See {@link com.urbanairship.push.notifications.DefaultNotificationFactory#setColor(int)}.
         *
         * @param notificationAccentColor The notification accent color.
         * @return The config options builder.
         */
        @NonNull
        public Builder setNotificationAccentColor(@ColorInt int notificationAccentColor) {
            this.notificationAccentColor = notificationAccentColor;
            return this;
        }

        /**
         * Set the application's production app key.
         *
         * @param productionAppKey The application's production app key.
         * @return The config options builder.
         */
        @NonNull
        public Builder setProductionAppKey(@Nullable String productionAppKey) {
            this.productionAppKey = productionAppKey;
            return this;
        }

        /**
         * Set the application's production app secret.
         *
         * @param productionAppSecret The application's production app secret.
         * @return The config options builder.
         */
        @NonNull
        public Builder setProductionAppSecret(@Nullable String productionAppSecret) {
            this.productionAppSecret = productionAppSecret;
            return this;
        }

        /**
         * Set the application's development app key.
         *
         * @param developmentAppKey The application's development app key.
         * @return The config options builder.
         */
        @NonNull
        public Builder setDevelopmentAppKey(@Nullable String developmentAppKey) {
            this.developmentAppKey = developmentAppKey;
            return this;
        }

        /**
         * Set the application's development app secret.
         *
         * @param developmentAppSecret The application's development app secret.
         * @return The config options builder.
         */
        @NonNull
        public Builder setDevelopmentAppSecret(@Nullable String developmentAppSecret) {
            this.developmentAppSecret = developmentAppSecret;
            return this;
        }

        /**
         * Set the Airship URL.
         *
         * @param hostURL The Airship URL.
         * @return The config options builder.
         */
        @NonNull
        public Builder setHostURL(@NonNull String hostURL) {
            this.hostURL = hostURL;
            return this;
        }

        /**
         * Set the analytics server URL.
         *
         * @param analyticsServer The analytics server URL.
         * @return The config options builder.
         */
        @NonNull
        public Builder setAnalyticsServer(@NonNull String analyticsServer) {
            this.analyticsServer = analyticsServer;
            return this;
        }

        /**
         * Set the remote data URL.
         *
         * @param remoteDataURL The remote data URL.
         * @return The config options builder.
         */
        @NonNull
        public Builder setRemoteDataURL(@Nullable String remoteDataURL) {
            this.remoteDataURL = remoteDataURL;
            return this;
        }

        /**
         * Sets the production FCM sender ID.
         * <p>
         * Optional if you are using `urbanairship-fcm` package and want Airship to use the
         * main Firebase application's sender ID.
         *
         * @param senderId The production FCM sender ID.
         * @return The config options builder.
         */
        @NonNull
        public Builder setProductionFcmSenderId(@Nullable String senderId) {
            this.productionFcmSenderId = senderId;
            return this;
        }

        /**
         * Sets the development FCM sender ID.
         * <p>
         * Optional if you are using `urbanairship-fcm` package and want Airship to use the
         * main Firebase application's sender ID.
         *
         * @param senderId The development FCM sender ID.
         * @return The config options builder.
         */
        @NonNull
        public Builder setDevelopmentFcmSenderId(@Nullable String senderId) {
            this.developmentFcmSenderId = senderId;
            return this;
        }

        /**
         * Sets the default FCM sender ID.
         * <p>
         * Optional if you are using `urbanairship-fcm` package and want Airship to use the
         * main Firebase application's sender ID.
         *
         * @param senderId The FCM sender ID.
         * @return The config options builder.
         */
        @NonNull
        public Builder setFcmSenderId(@Nullable String senderId) {
            this.fcmSenderId = senderId;
            return this;
        }

        /**
         * Set the transport types allowed for Push.
         *
         * @param allowedTransports The transport types allowed for Push.
         * @return The config options builder.
         */
        @NonNull
        public Builder setAllowedTransports(@Nullable String[] allowedTransports) {
            this.allowedTransports = allowedTransports;
            return this;
        }

        /**
         * Set the list of additional URLs that are allowed to be used for various features, including:
         * Airship JS interface, open external URL action, wallet action, HTML in-app messages,
         * and landing pages. Airship https URLs are included by default.
         *
         * @param whitelist The whitelist.
         * @return The config options builder.
         */
        @NonNull
        public Builder setWhitelist(@Nullable String[] whitelist) {
            this.whitelist = whitelist;
            return this;
        }

        /**
         * Set the flag indicating whether the application is in production or development.
         *
         * @param inProduction The flag indicating whether the application is in production or development.
         * @return The config options builder.
         */
        @NonNull
        public Builder setInProduction(boolean inProduction) {
            this.inProduction = inProduction;
            return this;
        }

        /**
         * Automatically determine the provisioning mode of the application.
         *
         * @param context The application context.
         * @return The config options builder.
         */
        @NonNull
        public Builder detectProvisioningMode(@NonNull Context context) {
            try {
                Class<?> clazz = Class.forName(context.getPackageName() + ".BuildConfig");
                Field field = clazz.getField("DEBUG");
                inProduction = !(boolean) field.get(null);
            } catch (Exception e) {
                Logger.warn("AirshipConfigOptions - Unable to determine the build mode. Defaulting to debug.");
                inProduction = false;
            }
            return this;
        }

        /**
         * Set the flag indicating whether the application will use analytics.
         *
         * @param analyticsEnabled The flag indicating whether the application will use analytics.
         * @return The config options builder.
         */
        @NonNull
        public Builder setAnalyticsEnabled(boolean analyticsEnabled) {
            this.analyticsEnabled = analyticsEnabled;
            return this;
        }

        /**
         * Set the background reporting interval.
         *
         * @param backgroundReportingIntervalMS The background reporting interval.
         * @return The config options builder.
         */
        @NonNull
        public Builder setBackgroundReportingIntervalMS(long backgroundReportingIntervalMS) {
            this.backgroundReportingIntervalMS = backgroundReportingIntervalMS;
            return this;
        }

        /**
         * Set the flag whether to clear an existing named user during a re-install.
         *
         * @param clearNamedUser The flag whether to clear an existing named user during a re-install.
         * @return The config options builder.
         */
        @NonNull
        public Builder setClearNamedUser(boolean clearNamedUser) {
            this.clearNamedUser = clearNamedUser;
            return this;
        }

        /**
         * Set the logger level when the application is in debug mode.
         *
         * @param developmentLogLevel The logger level.
         * @return The config options builder.
         */
        @NonNull
        public Builder setDevelopmentLogLevel(int developmentLogLevel) {
            this.developmentLogLevel = developmentLogLevel;
            return this;
        }

        /**
         * Set the logger level when the application is in production mode.
         *
         * @param productionLogLevel The logger level.
         * @return The config options builder.
         */
        @NonNull
        public Builder setProductionLogLevel(int productionLogLevel) {
            this.productionLogLevel = productionLogLevel;
            return this;
        }

        /**
         * Set the flag indicating whether or not to launch the launcher activity when a push notification or push
         * notification button is opened and the application intent receiver did not launch an activity.
         *
         * @param autoLaunchApplication The auto launch flag.
         * @return The config options builder.
         */
        @NonNull
        public Builder setAutoLaunchApplication(boolean autoLaunchApplication) {
            this.autoLaunchApplication = autoLaunchApplication;
            return this;
        }

        /**
         * Set the flag indicating whether channel creation delay is enabled or not.
         *
         * @param channelCreationDelayEnabled The flag indicating whether channel creation delay is enabled or not.
         * @return The config option builder.
         */
        @NonNull
        public Builder setChannelCreationDelayEnabled(boolean channelCreationDelayEnabled) {
            this.channelCreationDelayEnabled = channelCreationDelayEnabled;
            return this;
        }

        /**
         * Set the flag indicating whether channel capture feature is enabled or not.
         *
         * @param channelCaptureEnabled The flag indicating whether channel capture feature is enabled or not.
         * @return The config option builder.
         */
        @NonNull
        public Builder setChannelCaptureEnabled(boolean channelCaptureEnabled) {
            this.channelCaptureEnabled = channelCaptureEnabled;
            return this;
        }

        /**
         * Set the Wallet URL.
         *
         * @param walletUrl The Wallet URL.
         * @return The config options builder.
         */
        @NonNull
        public Builder setWalletUrl(@NonNull String walletUrl) {
            this.walletUrl = walletUrl;
            return this;
        }

        /**
         * Enables/disables whitelist checks for {@link com.urbanairship.js.Whitelist#SCOPE_OPEN_URL}.
         * If disabled, any URL checks with scope {@link com.urbanairship.js.Whitelist#SCOPE_OPEN_URL} will
         * be allowed even if the URL is not in the whitelist.
         *
         * @return The config options builder.
         */
        @NonNull
        public Builder setEnableUrlWhitelisting(boolean enableUrlWhitelisting) {
            this.enableUrlWhitelisting = enableUrlWhitelisting;
            return this;
        }

        /**
         * Used to set a custom push provider for push registration.
         *
         * @param customPushProvider Push provider.
         * @return The config options builder.
         * @hide
         */
        @NonNull
        public Builder setCustomPushProvider(@Nullable PushProvider customPushProvider) {
            this.customPushProvider = customPushProvider;
            return this;
        }

        /**
         * Sets the app store URI for the rate-app action. If not set,
         * the action will generate it using the app's current package name.
         *
         * <p>
         * Example: "market://details?id=com.example.android"
         *
         * @param appStoreUri The app store URI.
         * @return The config options builder.
         */
        @NonNull
        public Builder setAppStoreUri(@Nullable Uri appStoreUri) {
            this.appStoreUri = appStoreUri;
            return this;
        }

        /**
         * Builds the config options. Will fail if any of the following preconditions are not met.
         * <p>
         * <pre>
         * 1. If inProduction is <code>false</code>, development app key and secret must be set.
         * 2. If inProduction is <code>true</code>, production app key and secret must be set.
         * 3. The analytics URI must not be empty if analytics are enabled.
         * 4. The host URL must not be empty.
         * </pre>
         *
         * @return The built config options.
         */
        @NonNull
        public AirshipConfigOptions build() {
            if (inProduction == null) {
                inProduction = false;
            }

            String modeString = inProduction ? "production" : "development";

            String appKey = inProduction ? productionAppKey : developmentAppKey;
            if (appKey == null || appKey.length() == 0 || appKey.indexOf(' ') > 0) {
                throw new IllegalArgumentException("AirshipConfigOptions: " + appKey + " is not a valid " + modeString + " app key");
            }

            String appSecret = inProduction ? productionAppSecret : developmentAppSecret;
            if (appSecret == null || appSecret.length() == 0 || appSecret.indexOf(' ') > 0) {
                throw new IllegalArgumentException("AirshipConfigOptions: " + appSecret + " is not a valid " + modeString + " app secret");
            }

            if (analyticsEnabled && UAStringUtil.isEmpty(analyticsServer)) {
                throw new IllegalArgumentException("Invalid config - analyticsServer is empty or null.");
            }

            if (UAStringUtil.isEmpty(hostURL)) {
                throw new IllegalArgumentException("Invalid config - hostURL is empty or null.");
            }

            if (backgroundReportingIntervalMS < MIN_BG_REPORTING_INTERVAL_MS) {
                Logger.warn("AirshipConfigOptions - The backgroundReportingIntervalMS %s may decrease battery life.", backgroundReportingIntervalMS);
            } else if (backgroundReportingIntervalMS > MAX_BG_REPORTING_INTERVAL_MS) {
                Logger.warn("AirshipConfigOptions - The backgroundReportingIntervalMS %s may provide less detailed analytic reports.", backgroundReportingIntervalMS);
            }

            if (productionAppKey != null && productionAppKey.equals(developmentAppKey)) {
                Logger.warn("Production App Key matches Development App Key");
            }

            if (productionAppSecret != null && productionAppSecret.equals(developmentAppSecret)) {
                Logger.warn("Production App Secret matches Development App Secret");
            }

            return new AirshipConfigOptions(this);
        }

    }

}
