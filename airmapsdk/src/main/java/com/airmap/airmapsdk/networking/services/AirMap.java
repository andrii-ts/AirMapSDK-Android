package com.airmap.airmapsdk.networking.services;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.Analytics;
import com.airmap.airmapsdk.AnalyticsTracker;
import com.airmap.airmapsdk.Auth;
import com.airmap.airmapsdk.auth.AuthConstants;
import com.airmap.airmapsdk.models.AirMapWeather;
import com.airmap.airmapsdk.models.Coordinate;
import com.airmap.airmapsdk.models.agreements.Agreement;
import com.airmap.airmapsdk.models.agreements.AgreementStatus;
import com.airmap.airmapsdk.models.aircraft.AirMapAircraft;
import com.airmap.airmapsdk.models.aircraft.AirMapAircraftManufacturer;
import com.airmap.airmapsdk.models.aircraft.AirMapAircraftModel;
import com.airmap.airmapsdk.models.airspace.AirMapAirspace;
import com.airmap.airmapsdk.models.comm.AirMapComm;
import com.airmap.airmapsdk.models.flight.AirMapEvaluation;
import com.airmap.airmapsdk.models.flight.AirMapFlight;
import com.airmap.airmapsdk.models.flight.AirMapFlightBriefing;
import com.airmap.airmapsdk.models.flight.AirMapFlightLaancStatus;
import com.airmap.airmapsdk.models.flight.AirMapFlightPlan;
import com.airmap.airmapsdk.models.pilot.AirMapPilot;
import com.airmap.airmapsdk.models.rules.AirMapJurisdiction;
import com.airmap.airmapsdk.models.rules.AirMapRuleset;
import com.airmap.airmapsdk.models.shapes.AirMapGeometry;
import com.airmap.airmapsdk.models.shapes.AirMapPolygon;
import com.airmap.airmapsdk.models.status.AirMapAirspaceStatus;
import com.airmap.airmapsdk.models.status.AirMapStatus;
import com.airmap.airmapsdk.networking.callbacks.AirMapAuthenticationCallback;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.callbacks.AirMapSystemStatusListener;
import com.airmap.airmapsdk.networking.callbacks.AirMapTrafficListener;
import com.airmap.airmapsdk.networking.callbacks.AuthTokenListener;
import com.airmap.airmapsdk.networking.callbacks.LoginCallback;
import com.airmap.airmapsdk.networking.callbacks.LoginListener;
import com.airmap.airmapsdk.util.AirMapTree;
import com.airmap.airmapsdk.util.PreferenceUtils;
import com.airmap.airmapsdk.util.SecuredPreferenceException;
import com.airmap.airmapsdk.util.Utils;
import com.auth0.android.jwt.JWT;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import timber.log.Timber;

import static com.airmap.airmapsdk.util.Utils.timberContainsDebugTree;

@SuppressWarnings({"unused", "ConstantConditions"})
public final class AirMap {
    private Context context;
    private static String authToken;
    private static String apiKey;
    private static JSONObject config;
    private static String userId; //User ID extracted from JWT token
    private static boolean certificatePinning; //Whether to enable certificate pinning

    private static AirMap ourInstance;
    private static AirMapClient client;

    private static TrafficService airMapTrafficService;
    private static SystemStatusService airMapSystemStatusService;
    private static MappingService airMapMapMappingService;
    private static TelemetryService airMapTelemetryService;

    private static LoginListener loginListener;
    private static Analytics analytics;

    private static AuthTokenListener authTokenListener;

    /**
     * Initializes the SDK. This must be called before any requests can be made
     * The API key to be used with the requests must be provided in the airmap.config.json file in
     * your
     * /assets directory.
     *
     * @param context         An Android Context
     * @param authToken       The auth token to be used with the requests
     * @param pinCertificates A Boolean value, if true, will use pinned certificates to validate
     *                        the AirMap server trust
     * @return An AirMap instance
     * @see <a href="http://www.airmap.com/makers">http://www.airmap.com/makers</a>
     */
    public static AirMap init(@NonNull Context context, @Nullable String authToken, boolean pinCertificates) {
        ourInstance = new AirMap(context, authToken, pinCertificates);
        airMapTrafficService = new TrafficService(context); //Initialized here because TrafficService requires AirMap to be initialized already, so it is called after the constructor
        airMapMapMappingService = new MappingService(); //Initialized here because MappingService requires AirMap to be initialized already, so it is called after the constructor
        airMapSystemStatusService = new SystemStatusService(context);
        return ourInstance;
    }

    /**
     * Initializes the SDK. This must be called before any requests can be made
     *
     * @param context   An Android Context
     * @param authToken The auth token to be used with the requests
     * @return An AirMap instance
     * @see <a href="http://www.airmap.com/makers">http://www.airmap.com/makers</a>
     */
    public static AirMap init(@NonNull Context context, @Nullable String authToken) {
        return init(context, authToken, false);
    }

    /**
     * Initializes the SDK. This must be called before any requests can be made
     *
     * @param context         An Android Context
     * @param pinCertificates A Boolean value, if true, will use pinned certificates to validate
     *                        the AirMap server trust
     * @return An AirMap instance
     * @see <a href="http://www.airmap.com/makers">http://www.airmap.com/makers</a>
     */
    public static AirMap init(@NonNull Context context, boolean pinCertificates) {
        return init(context, "", pinCertificates);
    }

    /**
     * Initializes the SDK. This must be called before any requests can be made
     *
     * @param context An Android Context
     * @return An AirMap instance
     * @see <a href="http://www.airmap.com/makers">http://www.airmap.com/makers</a>
     */
    public static AirMap init(@NonNull Context context) {
        return init(context, "");
    }

    /**
     * Get an AirMap instance
     *
     * @return an AirMap instance
     */
    public static AirMap getInstance() {
        if (ourInstance == null) {
            throw new IllegalStateException("Need to call init() first");
        }
        return ourInstance;
    }

    /**
     * Constructs an AirMap instance
     *
     * @param auth            The auth token
     * @param pinCertificates Whether to enable certificate pinning (recommended)
     */
    private AirMap(@NonNull Context context, @Nullable String auth, boolean pinCertificates) {
        this.context = context;
        authToken = auth == null ? "" : auth;
        certificatePinning = pinCertificates;
        decodeToken(auth);
        try {
            InputStream inputStream = getContext().getResources().getAssets().open("airmap.config.json");
            String json = Utils.readInputStreamAsString(inputStream);
            config = new JSONObject(json);
            apiKey = getConfig().getJSONObject("airmap").getString("api_key");
        } catch (IOException | JSONException | NullPointerException e) {
            throw new RuntimeException("Please ensure you have your airmap.config.json file in your /assets directory");
        }
        client = new AirMapClient(context);
    }

    /**
     * Check if both API key and Auth Token are valid
     *
     * @return Validity of credentials
     */
    public static boolean hasValidCredentials() {
        return apiKey != null && !apiKey.isEmpty() && authToken != null && !authToken.isEmpty() && hasValidAuthenticatedUser();
    }

    /**
     * Get the expiration date of the provided Auth Token. If there was an error parsing the token,
     * then the current time is returned
     *
     * @return The expiration date
     */
    public static Date getAuthTokenExpirationDate() {
        if (TextUtils.isEmpty(authToken)) {
            return new Date();
        }

        JWT jwt = new JWT(authToken);
        return jwt.getExpiresAt();
    }

    public static void getFirebaseToken(AirMapCallback<String> callback) {
        AuthService.getFirebaseToken(callback);
    }

    /**
     * This function should be called when the application enters the background (onDestroy,
     * onStop, onPause)
     */
    public static void suspend() {
        TrafficService service = getAirMapTrafficService();
        if (service != null && service.isConnected()) {
            service.disconnect();
        }

        SystemStatusService systemStatusService = getAirMapSystemStatusService();
        if(systemStatusService != null && systemStatusService.isConnected()) {
            systemStatusService.disconnect();
        }
    }

    /**
     * This function needs to be called when the application is no longer in the background
     * (onStart, onResume)
     */
    public static void resume() {
        TrafficService service = getAirMapTrafficService();
        if (service != null) {
            service.connect();
        }

        SystemStatusService systemStatusService = getAirMapSystemStatusService();
        if(systemStatusService != null) {
            systemStatusService.connect();
        }
    }

    /**
     * Decodes the JWT Auth Token and parses it to get the user ID
     *
     * @param token The JWT Auth Token
     */
    private static void decodeToken(String token) {
        if (TextUtils.isEmpty(token)) {
            Timber.e("No token to decode");
            return;
        }

        JWT jwt = new JWT(token);
        userId = jwt.getSubject();
    }

    /**
     * Needs to be called whenever the auth token changes
     * If the auth token has changed, this allows it to be reflected in the web services
     *
     * @param newAuthToken The new auth token
     */
    public static void setAuthToken(String newAuthToken) {
        authToken = newAuthToken;
        getAirMapTrafficService().setAuthToken(newAuthToken);
        getAirMapSystemStatusService().setAuthToken(newAuthToken);
        decodeToken(authToken);
        if (authTokenListener != null) {
            authTokenListener.onNewToken();
        }
    }

    public static void clearAuthToken() {
        authToken = null;
        getAirMapTrafficService().setAuthToken(null);
        getAirMapSystemStatusService().setAuthToken(null);
    }

    public static void saveTokens(Context context, @Nullable String newAccessToken, @Nullable String newRefreshToken) {
        setAuthToken(newAccessToken);
        try {
            PreferenceUtils.getPreferences(context)
                    .edit()
                    .putString(AuthConstants.REFRESH_TOKEN_KEY, newRefreshToken)
                    .putString(AuthConstants.ACCESS_TOKEN_KEY, newAccessToken)
                    .apply();
        } catch (SecuredPreferenceException e) {
            Timber.e(e, "Secured Preferences Failed");
        }
    }

    /**
     * Needs to be called whenever the API key changes
     * If the API key has changed, this allows it to be reflected in the web services
     *
     * @param newApiKey The new API key
     */
    protected static void setApiKey(String newApiKey) {
        apiKey = newApiKey;
    }

    /**
     * @return a context
     */
    public Context getContext() {
        return context;
    }

    /**
     * @return the authToken
     */
    public static String getAuthToken() {
        return authToken;
    }

    public static String getRefreshToken(Context context) {
        try {
            return PreferenceUtils.getPreferences(context)
                    .getString(AuthConstants.REFRESH_TOKEN_KEY, "");
        } catch (SecuredPreferenceException e) {
            Timber.e(e, "Secured Preferences Failed");
            return "";
        }
    }

    /**
     * @return the API key
     */
    public static String getApiKey() {
        return apiKey;
    }

    public static void setAuthTokenListener(AuthTokenListener newAuthTokenListener) {
        authTokenListener = newAuthTokenListener;
    }

    /**
     * @return the User ID of the authenticated user (if one exists)
     */
    public static String getUserId() {
        return userId;
    }

    public static JSONObject getConfig() {
        return config;
    }

    public static void setLoginListener(LoginListener listener) {
        loginListener = listener;
    }

    public static void setAnalytics(AnalyticsTracker tracker) {
        Analytics.init(tracker);
    }

    /**
     * Logs the user out
     */
    public void logout() {
        AuthService.logout(context);
        setAuthToken(null);
        userId = null;
        getClient().resetClient(context);
        saveTokens(context, null, null);
    }

    /**
     * @return The AirMapClient
     */
    protected static AirMapClient getClient() {
        return client;
    }

    /**
     * @return whether AirMap has been initialized
     */
    public static boolean hasBeenInitialized() {
        return ourInstance != null;
    }

    /**
     * @return whether there is an authenticated pilot associated with the SDK instance
     */
    public static boolean hasValidAuthenticatedUser() {
        return getAuthTokenExpirationDate().after(new Date());
    }

    /**
     * Checks to see if certificate pinning is enable
     *
     * @return whether certificate pinning is enabled
     */
    public static boolean isCertificatePinningEnabled() {
        return certificatePinning;
    }

    /**
     * Enables logging for debugging purposes. Currently cannot be disabled once enabled. This should not need to be
     * called if you are using Timber yourself and have already planted a DebugTree
     */
    public static void enableLogging() {
        // Don't want to double log
        if (!timberContainsDebugTree()) {
            Timber.plant(new AirMapTree());
        }
    }

    /**
     * @return the traffic service
     */
    public static TrafficService getAirMapTrafficService() {
        return airMapTrafficService;
    }

    /**
     * @return the system status service
     */
    public static SystemStatusService getAirMapSystemStatusService() {
        return airMapSystemStatusService;
    }

    //Login

    /**
     * Calls the appropriate method in the login listener
     */
    protected static void showLogin() {
        if (loginListener != null) {
            loginListener.shouldAuthenticate();
        }
    }

    /**
     * Show the login screen
     *
     * @param activity Activity to create the UI with and to deliver results to
     * @param callback AirMap authentication callback
     */
    public static void showLogin(Activity activity, LoginCallback callback) {
        AuthService.loginOrSignup(activity, new AirMapAuthenticationCallback(activity, callback));
    }

    /**
     * Refreshes the pilot's authentication token. Non-blocking
     *
     * @param callback The callback that is invoked on success or error
     */
    public static void refreshAccessToken(@Nullable AirMapCallback<Void> callback) {
        Auth.refreshAccessToken(getInstance().getContext(), callback);
    }

    /**
     * Refreshes the pilot's authentication token. Blocking
     */
    public static void refreshAccessToken() {
        Auth.refreshAccessToken(getInstance().getContext());
    }

    public static void clearRefreshToken() {
        Auth.clearRefreshToken(getInstance().getContext());
    }

    /**
     * Logs in anonymously. The auth token of the anonymous user can be obtained, if required,
     * through {@link AirMap#getAuthToken()}
     */
    public static Call performAnonymousLogin(@NonNull String userId, @Nullable AirMapCallback<Void> callback) {
        return AuthService.performAnonymousLogin(userId, callback);
    }

    //Aircraft

    /**
     * Get a list of all Aircraft Manufacturers
     *
     * @param callback The callback that is called on success or error
     */
    public static Call getManufacturers(@Nullable AirMapCallback<List<AirMapAircraftManufacturer>> callback) {
        return AircraftService.getManufacturers(callback);
    }

    /**
     * Get a list of all Aircraft Models
     *
     * @param callback The callback that is invoked on success or error
     */
    public static Call getModels(@Nullable AirMapCallback<List<AirMapAircraftModel>> callback) {
        return AircraftService.getModels(callback);
    }

    /**
     * Get a list of all Aircraft Models from a given Manufacturer
     *
     * @param manufacturerId The id of the manufacturer to get models for
     * @param callback       The callback that is invoked on success or error
     */
    public static Call getModels(@Nullable String manufacturerId, @Nullable AirMapCallback<List<AirMapAircraftModel>> callback) {
        return AircraftService.getModels(manufacturerId, callback);
    }

    /**
     * Get a model by ID
     *
     * @param modelId  The ID of the model to get
     * @param callback The callback that is invoked on success or error
     */
    public static Call getModel(@NonNull String modelId, @Nullable AirMapCallback<AirMapAircraftModel> callback) {
        if (modelId != null) {
            return AircraftService.getModel(modelId, callback);
        } else {
            if (callback != null) {
                callback.error(new AirMapException("modelId cannot be null"));
            }
            return null;
        }
    }

    //Flight

    /**
     * List Flights. All parameters are optional and nullable
     *
     * @param limit           Max number of flights to return
     * @param pilotId         Search for flights from a particular pilot
     * @param startAfter      Search for flights that start after this time
     * @param startBefore     Search for flights that start before this time
     * @param endAfter        Search for flights that end after this time
     * @param endBefore       Search for flights that end before this time
     * @param startsAfterNow  Search for flights starting after now (the startsAfter Date will be ignored)
     * @param startsBeforeNow Search for flights starting before now (the startsBefore Date will be ignored)
     * @param endsAfterNow    Search for flights ending after now (the endsAfter Date will be ignored)
     * @param endsBeforeNow   Search for flights ending before now (the endsBefore Date will be ignored)
     * @param country         Search for flights within this country (Length: 3, case insensitive)
     * @param city            Search for flights within this city
     * @param state           Search for flights within this state
     * @param enhanced        Returns enhanced flight, pilot, and aircraft information
     * @param callback        The callback that is invoked on success or error
     */
    public static Call getFlights(@Nullable Integer limit, @Nullable String pilotId,
                                  @Nullable Date startAfter, @Nullable Date startBefore,
                                  @Nullable Date endAfter, @Nullable Date endBefore,
                                  @Nullable Boolean startsAfterNow, @Nullable Boolean startsBeforeNow,
                                  @Nullable Boolean endsAfterNow, @Nullable Boolean endsBeforeNow,
                                  @Nullable String country, @Nullable String city,
                                  @Nullable String state, @Nullable Boolean enhanced,
                                  @Nullable AirMapCallback<List<AirMapFlight>> callback) {
        return FlightService.getFlights(limit, pilotId, startAfter, startBefore, endAfter, endBefore,
                startsAfterNow, startsBeforeNow, endsAfterNow, endsBeforeNow, country, city, state,
                enhanced, callback);
    }

    /**
     * Get a list of all public flights combined with the authenticated pilot's private flights
     *
     * @param callback The callback that is invoked on success or error
     */
    public static Call getPublicFlights(@Nullable AirMapCallback<List<AirMapFlight>> callback) {
        return FlightService.getPublicFlights(1000,null,null, callback);
    }

    /**
     * Get a list of all public flights combined with the authenticated pilot's private flights
     *
     * @param limit    Max number of flights to return
     * @param from     Search for flights from this date
     * @param to       Search for flights to this date
     * @param callback The callback that is invoked on success or error
     */
    public static Call getPublicFlights(@Nullable Integer limit, @Nullable Date from, @Nullable Date to,
                                        @Nullable AirMapCallback<List<AirMapFlight>> callback) {
        return FlightService.getPublicFlights(limit, from, to, callback);
    }

    /**
     * Get the authenticated pilot's current active flight
     *
     * @param callback The callback that is invoked on success or error
     */
    public static Call getCurrentFlight(final @Nullable AirMapCallback<AirMapFlight> callback) {
        AirMapCallback<List<AirMapFlight>> proxy = new AirMapCallback<List<AirMapFlight>>() {
            @Override
            public void onSuccess(List<AirMapFlight> response) {
                if (callback != null) {
                    if (response != null && !response.isEmpty()) {
                        callback.success(response.get(0));
                    } else {
                        callback.success(null);
                    }
                }
            }

            @Override
            public void onError(AirMapException e) {
                if (callback != null) {
                    callback.error(e);
                }
            }
        };
        if (AirMap.getUserId() != null) {
            return FlightService.getFlights(null, AirMap.getUserId(), null, null, null, null, null, true, true, null, null, null, null, true, proxy);
        } else {
            if (callback != null) {
                callback.success(null);
            }
            return null;
        }
    }

    /**
     * Get a flight by its ID
     *
     * @param flightId The ID of the flight to get
     * @param enhance  If true, the response will include explicit profile and aircraft information
     *                 instead of just IDs
     * @param callback The callback that is invoked on success or error
     */
    public static Call getFlight(@NonNull String flightId, boolean enhance, @Nullable AirMapCallback<AirMapFlight> callback) {
        return FlightService.getFlight(flightId, enhance, callback);
    }

    /**
     * Get a flight by its ID
     *
     * @param flightId The ID of the flight to get
     * @param callback The callback that is invoked on success or error
     */
    public static Call getFlight(@NonNull String flightId, @Nullable AirMapCallback<AirMapFlight> callback) {
        return getFlight(flightId, false, callback);
    }

    /**
     * Get all the flights belonging to a pilot
     *
     * @param pilot    The pilot to get flights for
     * @param callback The callback that is invoked on success or error
     */
    public static Call getFlights(@NonNull AirMapPilot pilot, @Nullable AirMapCallback<List<AirMapFlight>> callback) {
        return FlightService.getFlights(null, pilot.getPilotId(), null, null, null, null, null, null, null, null, null, null, null, true, callback);
    }

    /**
     * Get all the flights belonging to the authenticated pilot
     *
     * @param callback The callback that is invoked on success or error
     */
    public static Call getMyFlights(@Nullable AirMapCallback<List<AirMapFlight>> callback) {
        return FlightService.getFlights(null, AirMap.getUserId(), null, null, null, null, null, null, null, null, null, null, null, true, callback);
    }

    public static Call createFlightPlan(AirMapFlightPlan flightPlan, AirMapCallback<AirMapFlightPlan> callback) {
        return FlightService.createFlightPlan(flightPlan, callback);
    }

    public static Call patchFlightPlan(AirMapFlightPlan plan, AirMapCallback<AirMapFlightPlan> callback) {
        return FlightService.patchFlightPlan(plan, callback);
    }

    public static Call getFlightPlanById(String flightPlanId, AirMapCallback<AirMapFlightPlan> callback) {
        return FlightService.getFlightPlanById(flightPlanId, callback);
    }

    public static Call getFlightPlanAuthorizationsBatch(List<String> flightPlanIds, AirMapCallback<List<AirMapFlightBriefing>> callback) {
        return FlightService.getFlightPlanAuthorizationsBatch(flightPlanIds, callback);
    }

    public static Call getFlightPlanByFlightId(String flightId, AirMapCallback<AirMapFlightPlan> callback) {
        return FlightService.getFlightPlanByFlightId(flightId, callback);
    }

    public static Call submitFlightPlan(String flightPlanId, boolean isPublic, AirMapCallback<AirMapFlightPlan> callback) {
        return FlightService.submitFlightPlan(flightPlanId, isPublic, callback);
    }

    public static Call submitFlightPlan(String flightPlanId, AirMapCallback<AirMapFlightPlan> callback) {
        return FlightService.submitFlightPlan(flightPlanId, true, callback);
    }

    public static Call getWeather(Coordinate coordinate, Date startTime, Date endTime, AirMapCallback<AirMapWeather> callback) {
        return StatusService.getWeather(coordinate, startTime, endTime, callback);
    }

    /**
     * Create a flight for the pilot
     *
     * @param flight   The flight to create
     * @param callback The callback that is invoked on success or error
     */
    @Deprecated
    public static Call createFlight(@NonNull AirMapFlight flight, @Nullable AirMapCallback<AirMapFlight> callback) {
        return FlightService.createFlight(flight, callback);
    }

    /**
     * End a flight belonging to the logged in pilot
     *
     * @param flightId The ID of the flight to close
     * @param callback The callback that is invoked on success or error
     */
    public static Call endFlight(@NonNull String flightId, @Nullable AirMapCallback<AirMapFlight> callback) {
        return FlightService.endFlight(flightId, callback);
    }

    /**
     * Cancel a flight belonging to the logged in pilot
     *
     * @param flight   The flight to delete
     * @param callback The callback that is invoked on success or error
     */
    public static Call cancelFlight(@NonNull AirMapFlight flight, @Nullable AirMapCallback<Void> callback) {
        return FlightService.cancelFlight(flight, callback);
    }

    /**
     * Cancel a flight belonging to the logged in pilot
     *
     * @param flightId The id of flight to delete
     * @param callback The callback that is invoked on success or error
     */
    public static Call cancelFlight(@NonNull String flightId, @Nullable AirMapCallback<Void> callback) {
        return FlightService.cancelFlight(flightId, callback);
    }

    /**
     * Delete a flight belonging to the logged in pilot
     *
     * @param flight   The flight to delete
     * @param callback The callback that is invoked on success or error
     */
    public static Call deleteFlight(@NonNull AirMapFlight flight, @Nullable AirMapCallback<Void> callback) {
        return FlightService.deleteFlight(flight, callback);
    }

    /**
     * Delete a flight belonging to the logged in pilot
     *
     * @param flightId The id of flight to delete
     * @param callback The callback that is invoked on success or error
     */
    public static Call deleteFlight(@NonNull String flightId, @Nullable AirMapCallback<Void> callback) {
        return FlightService.deleteFlight(flightId, callback);
    }

    /**
     * Get a comm key for a given flight to enable traffic alerts
     *
     * @param flightId The flight ID to get the comm key for
     * @param callback The callback that is invoked on success or error
     */
    public static Call startComm(@NonNull String flightId, @Nullable AirMapCallback<AirMapComm> callback) {
        return FlightService.getCommKey(flightId, callback);
    }

    /**
     * Stop receiving notifications for traffic alerts
     *
     * @param flight   The flight to stop receiving notifications for
     * @param callback The callback that is invoked on success or error
     */
    public static Call clearComm(@NonNull AirMapFlight flight, @Nullable AirMapCallback<Void> callback) {
        return FlightService.clearCommKey(flight, callback);
    }

    //Pilot

    /**
     * Get a pilot by ID
     *
     * @param callback The callback that is invoked on success or error
     */
    public static Call getPilot(@NonNull String pilotId, @Nullable AirMapCallback<AirMapPilot> callback) {
        return PilotService.getPilot(pilotId, callback);
    }

    /**
     * Get the authenticated pilot
     *
     * @param callback The callback that is invoked on success or error
     */
    public static Call getPilot(@Nullable AirMapCallback<AirMapPilot> callback) {
        return getPilot(AirMap.getUserId(), callback);
    }

    /**
     * Update a pilot
     *
     * @param pilot    The updated version of the pilot
     * @param callback The callback that is invoked on success or error
     */
    public static Call updatePilot(@NonNull AirMapPilot pilot, @Nullable AirMapCallback<AirMapPilot> callback) {
        return PilotService.updatePilot(pilot, callback);
    }

    /**
     * Update the authenticated pilot's phone number
     *
     * @param phoneNumber The updated phone number
     * @param callback    The callback that is invoked on success or error
     */
    public static Call updatePhoneNumber(@NonNull String phoneNumber, @Nullable AirMapCallback<Void> callback) {
        return PilotService.updatePhoneNumber(phoneNumber, callback);
    }

    /**
     * Verify the user's phone number
     *
     * @param callback The callback that is invoked on success or error
     */
    public static Call sendVerificationToken(@Nullable AirMapCallback<Void> callback) {
        return PilotService.sendVerificationToken(callback);
    }

    /**
     * Verify that the text message the pilot received was the correct one
     *
     * @param token    The token that the pilot received in the text
     * @param callback The callback that is invoked on success or error
     */
    public static Call verifyPhoneToken(@NonNull String token, @Nullable AirMapCallback<Boolean> callback) {
        return PilotService.verifyToken(token, callback);
    }

    /**
     * Delete the users profile. WARNING THIS IS IRREVERSIBLE!
     *
     * @param callback The callback that is invoked on success or error
     */
    public static Call deletePilot(@NonNull String pilotId, @Nullable AirMapCallback<AirMapPilot> callback) {
        return PilotService.deletePilot(callback);
    }


    //Aircraft

    /**
     * Get all the authenticated pilot's aircraft
     *
     * @param callback The callback that is invoked on success or error
     */
    public static Call getAircraft(@Nullable AirMapCallback<List<AirMapAircraft>> callback) {
        return PilotService.getAircraft(callback);
    }

    /**
     * Gets an aircraft by its ID
     *
     * @param aircraftId The ID of the aircraft to get
     * @param callback   The callback that is invoked on success or error
     */
    public static Call getAircraft(@NonNull String aircraftId, @Nullable AirMapCallback<AirMapAircraft> callback) {
        return PilotService.getAircraft(aircraftId, callback);
    }

    /**
     * Create an aircraft for the authenticated pilot
     *
     * @param aircraft The aircraft to add to the pilot's profile
     * @param callback The callback that is invoked on success or error
     */
    public static Call createAircraft(@NonNull AirMapAircraft aircraft, @Nullable AirMapCallback<AirMapAircraft> callback) {
        return PilotService.createAircraft(aircraft, callback);
    }

    /**
     * Create an aircraft for the authenticated pilot
     *
     * @param nickname The nickname for the aircraft
     * @param model The aircraft model
     * @param callback The callback that is invoked on success or error
     */
    public static Call createAircraft(@NonNull String nickname, AirMapAircraftModel model, @Nullable AirMapCallback<AirMapAircraft> callback) {
        return PilotService.createAircraft(nickname, model, callback);
    }

    /**
     * Update the nickname of the authenticated pilot's aircraft
     *
     * @param aircraft The aircraft with the updated nickname
     * @param callback The callback that is invoked on success or error
     */
    public static Call updateAircraft(@NonNull AirMapAircraft aircraft, @Nullable AirMapCallback<AirMapAircraft> callback) {
        return PilotService.updateAircraft(aircraft, callback);
    }

    /**
     * Delete an aircraft from the authenticated pilots' profile
     *
     * @param aircraft The aircraft to delete
     * @param callback The callback that is invoked on success or error
     */
    public static Call deleteAircraft(@NonNull AirMapAircraft aircraft, @Nullable AirMapCallback<Void> callback) {
        return PilotService.deleteAircraft(aircraft, callback);
    }

    /**
     * Get a flight status based on a Point and Radius based flight
     *
     * @param coordinate   The coordinate of the flight
     * @param buffer       Number of meters to buffer a flight (the radius of the flight)
     * @param types        Airspace types to include in the calculation and response
     * @param ignoredTypes Airspace types to ignore in the calculation and response
     * @param showWeather  Whether to show the current weather conditions
     * @param date         Date and time for planned flight
     * @param callback     The callback that is invoked on success or error
     */
    @Deprecated
    public static Call checkCoordinate(@NonNull Coordinate coordinate, @Nullable Double buffer,
                                       @Nullable List<MappingService.AirMapAirspaceType> types,
                                       @Nullable List<MappingService.AirMapAirspaceType> ignoredTypes,
                                       boolean showWeather, @Nullable Date date,
                                       @Nullable AirMapCallback<AirMapStatus> callback) {
        return StatusService.checkCoordinate(coordinate, buffer, types, ignoredTypes, showWeather, date, callback);
    }

    /**
     * Get a flight status based on a Multi-point based flight
     *
     * @param path         The points on the flight path
     * @param buffer       The line width for the flight
     * @param takeOffPoint The coordinate of the flight
     * @param types        Airspace types to include in the calculation and response
     * @param ignoredTypes Airspace types to ignore in the calculation and response
     * @param showWeather  Whether to show the current weather conditions
     * @param date         Date and time for planned flight
     * @param callback     The callback that is invoked on success or error
     */
    @Deprecated
    public static Call checkFlightPath(@NonNull List<Coordinate> path, int buffer, @NonNull Coordinate takeOffPoint,
                                       @Nullable List<MappingService.AirMapAirspaceType> types,
                                       @Nullable List<MappingService.AirMapAirspaceType> ignoredTypes, boolean showWeather,
                                       @Nullable Date date, @Nullable AirMapCallback<AirMapStatus> callback) {
        return StatusService.checkFlightPath(path, buffer, takeOffPoint, types, ignoredTypes, showWeather, date, callback);
    }

    /**
     * Get a flight status based on a flight within a polygon
     *
     * @param geometry     The polygon the flight will be in
     * @param takeOffPoint The take off point of the flight
     * @param types        Airspace types to include in the calculation and response
     * @param ignoredTypes Airspace types to ignore in the calculation and response
     * @param showWeather  Whether to show the current weather conditions
     * @param date         Date and time for planned flight
     * @param callback     The callback that is invoked on success or error
     */
    @Deprecated
    public static Call checkPolygon(@NonNull List<Coordinate> geometry, @NonNull Coordinate takeOffPoint,
                                    @Nullable List<MappingService.AirMapAirspaceType> types,
                                    @Nullable List<MappingService.AirMapAirspaceType> ignoredTypes,
                                    boolean showWeather, @Nullable Date date,
                                    @Nullable AirMapCallback<AirMapStatus> callback) {
        return StatusService.checkPolygon(geometry, takeOffPoint, types, ignoredTypes, showWeather, date, callback);
    }

    /**
     * Get weather from status based on a Point and Radius
     *
     * @param coordinate The coordinate of the flight
     * @param buffer     Number of meters to buffer a flight (the radius of the flight)
     * @param callback   The callback that is invoked on success or error
     */
    @Deprecated
    public static Call checkWeather(@NonNull Coordinate coordinate, @Nullable Double buffer,
                                    @Nullable AirMapCallback<AirMapStatus> callback) {
        return StatusService.checkWeather(coordinate, buffer, callback);
    }

    /**
     * Get an airspace by its ID
     *
     * @param airspaceId The ID of the airspace to get
     * @param callback   The callback that is invoked on success or error
     */
    public static Call getAirspace(@NonNull String airspaceId,
                                   @Nullable AirMapCallback<AirMapAirspace> callback) {
        return AirspaceService.getAirspace(airspaceId, callback);
    }

    /**
     * Get airspaces by a list of their IDs
     *
     * @param airspaceIds The IDs of the airspaces to get
     * @param callback    The callback that is invoked on success or error
     */
    public static Call getAirspace(@NonNull List<String> airspaceIds,
                                   @Nullable AirMapCallback<List<AirMapAirspace>> callback) {
        return AirspaceService.getAirspace(airspaceIds, callback);
    }

    public static Call getJurisdictions(@NonNull AirMapPolygon polygon, @Nullable AirMapCallback<List<AirMapJurisdiction>> callback) {
        return RulesetService.getJurisdictions(AirMapGeometry.getGeoJSONFromGeometry(polygon), AirMap.getAuthToken(), callback);
    }

    /**
     * Get all the rulesets that apply for a specific coordinate
     *
     * @param coordinate
     * @param callback
     * @return
     */
    public static Call getRulesets(@NonNull Coordinate coordinate, @Nullable AirMapCallback<List<AirMapRuleset>> callback) {
        return RulesetService.getRulesets(coordinate, AirMap.getAuthToken(), callback);
    }

    /**
     * Get all the rulesets that apply for a geometry (geoJSON)
     *
     * @param geometry
     * @param callback
     * @return
     */
    public static Call getRulesets(@NonNull JSONObject geometry, @Nullable AirMapCallback<List<AirMapRuleset>> callback) {
        return RulesetService.getRulesets(geometry, AirMap.getAuthToken(), callback);
    }

    /**
     * Get the full ruleset objects for a list of ruleset ids
     *
     * @param rulesetIds
     * @param callback
     * @return
     */
    public static Call getRulesets(@NonNull List<String> rulesetIds, @Nullable AirMapCallback<List<AirMapRuleset>> callback) {
        return RulesetService.getRulesets(rulesetIds, AirMap.getAuthToken(), callback);
    }

    /**
     * Get the entire list of rules of a ruleset
     *
     * @param rulesetId
     * @param callback
     * @return
     */
    public static Call getRules(@NonNull String rulesetId, @Nullable AirMapCallback<AirMapRuleset> callback) {
        return RulesetService.getRules(rulesetId, AirMap.getAuthToken(), callback);
    }

    /**
     * Get the briefing for a flight plan (either before or after flight has been submitted)
     *
     * @param flightPlanId
     * @param callback
     */
    public static void getFlightBrief(@NonNull String flightPlanId, @NonNull AirMapCallback<AirMapFlightBriefing> callback) {
        FlightService.getFlightBriefing(flightPlanId, callback);
    }

    public static void getIfLaancDown(@NonNull String pilotID,@NonNull String geoScope ,@NonNull AirMapCallback<AirMapFlightLaancStatus> callback){
        FlightService.getIfLaancDown(pilotID, geoScope, callback);
    }

    public static void getFlightPlanEvaluation(@NonNull List<String> rulesets, @NonNull AirMapPolygon polygon, AirMapCallback<AirMapEvaluation> callback) {
        getFlightPlanEvaluation(rulesets, AirMapGeometry.getGeoJSONFromGeometry(polygon), callback);
    }

    public static void getFlightPlanEvaluation(@NonNull List<String> rulesets, @NonNull JSONObject geometry, AirMapCallback<AirMapEvaluation> callback) {
        RulesetService.getEvaluation(rulesets, geometry, AirMap.getAuthToken(), callback);
    }

    public static Call getAirspaceStatus(@NonNull AirMapFlightPlan flightPlan, AirMapCallback<AirMapAirspaceStatus> callback) {
        try {
            JSONObject geometry = new JSONObject(flightPlan.getGeometry());
            return RulesetService.getAdvisories(flightPlan.getRulesetIds(), geometry, flightPlan.getStartsAt(), flightPlan.getEndsAt(), null, AirMap.getAuthToken(), callback);
        } catch (JSONException e) {
            callback.error(new AirMapException("Unable to parse geometry json: \n" + e.getMessage()));
            return null;
        }
    }

    public static Call getAirspaceStatus(@NonNull AirMapPolygon polygon, @NonNull List<String> rulesetIds, AirMapCallback<AirMapAirspaceStatus> callback) {
        return RulesetService.getAdvisories(rulesetIds, polygon, null, null, null, AirMap.getAuthToken(), callback);
    }

    public static Call getAirspaceStatus(@NonNull AirMapPolygon polygon, @NonNull List<String> rulesetIds, @Nullable Date start, @Nullable Date end, AirMapCallback<AirMapAirspaceStatus> callback) {
        return RulesetService.getAdvisories(rulesetIds, polygon, start, end, null, AirMap.getAuthToken(), callback);
    }

    public static Call getAirspaceStatus(@NonNull JSONObject geometry, @NonNull List<String> rulesetIds, @Nullable Date start, @Nullable Date end, AirMapCallback<AirMapAirspaceStatus> callback) {
        return RulesetService.getAdvisories(rulesetIds, geometry, start, end, null, AirMap.getAuthToken(), callback);
    }

    public static String getMapStylesUrl(MappingService.AirMapMapTheme theme) {
        return airMapMapMappingService.getStylesUrl(theme);
    }

    public static Call getMapStylesJson(MappingService.AirMapMapTheme theme, AirMapCallback<JSONObject> listener) {
        return airMapMapMappingService.getStylesJson(theme, listener);
    }

    public static String getRulesetTileUrlTemplate(String rulesetId, List<String> layers, boolean useSIMeasurements) {
        return getRulesetTileUrlTemplate(rulesetId, layers, useSIMeasurements, null, null);
    }

    public static String getRulesetTileUrlTemplate(String rulesetId, List<String> layers, boolean useSIMeasurements, Date start, Date end) {
        return airMapMapMappingService.getRulesetTileUrlTemplate(rulesetId, layers, useSIMeasurements, AirMap.getAuthToken(), start, end);
    }

    public static String getBaseJurisdictionsUrlTemplate() {
        return airMapMapMappingService.getBaseJurisdictionsUrlTemplate();
    }

    /////////////
    // Agreements
    /////////////

    public static Call getAgreements(String authorityId, AirMapCallback<List<Agreement>> callback){
        return AgreementService.getAgreements(authorityId, callback);
    }

    public static Call getAgreement(String agreementId, AirMapCallback<Agreement> callback){
        return AgreementService.getAgreement(agreementId, callback);
    }

    public static Call getAgreementStatus(String agreementId, AirMapCallback<AgreementStatus> callback){
        return AgreementService.getAgreementStatus(agreementId, callback);
    }

    public static Call agreeToAgreement(String agreementId, AirMapCallback<Void> callback){
        return AgreementService.agreeToAgreement(agreementId, callback);
    }

    //////////
    // Traffic
    //////////

    /**
     * Starts the Traffic Alerts service to receive traffic alerts for the current active flight
     *
     * @param listener The callback that is invoked when traffic is added, updated, and removed
     */
    public static void enableTrafficAlerts(AirMapTrafficListener listener) {
        getAirMapTrafficService().addListener(listener);
        getAirMapTrafficService().connect();
    }

    /**
     * Disconnects from Traffic Alerts
     */
    public static void disableTrafficAlerts() {
        getAirMapTrafficService().disconnect();
        getAirMapTrafficService().removeAllListeners();
    }

    /**
     * Adds a callback to the traffic service
     *
     * @param listener The callback that is invoked when traffic is added, updated, and removed
     */
    public static void addTrafficListener(AirMapTrafficListener listener) {
        getAirMapTrafficService().addListener(listener);
    }

    /**
     * Removes all traffic callbacks
     */
    public static void removeAllTrafficListeners() {
        getAirMapTrafficService().removeAllListeners();
    }

    ////////////////
    // System Status
    ////////////////

    /**
     * Starts the System Status service to receive updates on the AirMap System health
     *
     * @param listener the callback that is invoked
     */
    public static void enableSystemStatusAlerts(AirMapSystemStatusListener listener){
        getAirMapSystemStatusService().addListener(listener);
        getAirMapSystemStatusService().connect();
    }

    /**
     * Disconnects from System Status updates
     */
    public static void disableSystemStatusAlerts(){
        getAirMapSystemStatusService().disconnect();
        getAirMapSystemStatusService().removeAllListeners();
    }

    /**
     * Adds a callback to the System Status service
     *
     * @param listener the callback that is invoked
     */
    public static void addSystemStatusListener(AirMapSystemStatusListener listener){
        getAirMapSystemStatusService().addListener(listener);
    }

    /**
     * Removes all System Status callbacks
     */
    public static void removeAllSystemStatusListeners(){
        getAirMapSystemStatusService().removeAllListeners();
    }

    ////////////
    // Telemetry
    ////////////

    public static TelemetryService getTelemetryService() {
        if (airMapTelemetryService == null) {
            airMapTelemetryService = new TelemetryService();
        }

        return airMapTelemetryService;
    }
}
