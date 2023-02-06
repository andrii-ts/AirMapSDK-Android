package com.airmap.airmapsdk.util;

public class AirMapConstants {

    //Secured Shared Preferences Keys
    public static final String EMAIL = "email";
    public static final String LOGGED_IN = "loggedIn";
    public static final String AUTHENTICATED_USER_ID = "userId";
    public static final String REFRESH_TOKEN = "refreshToken";
    public static final String AUTH_TOKEN = "authToken";

    //Shared Preferences Keys
    public static final String MAP_STYLE = "mapStyle";
    public static final String LAST_LOCATION_LATITUDE = "last_location_latitude";
    public static final String LAST_LOCATION_LONGITUDE = "last_location_longitude";

    //Intent extras
    public static final String URL_EXTRA = "url_extra";
    public static final String AIRCRAFT_EXTRA = "aircraft_extra";
    public static final String PILOT_EXTRA = "pilot_extra";
    public static final String PILOT_DELETED_EXTRA = "pilot_deleted_extra";
    public static final String FLIGHT_PLAN_ID_EXTRA = "flight_plan_id_extra";
    public static final String KEY_VALUE_EXTRAS = "key_value_extras";
    public static final String START_DATE_EXTRA = "start_date_extra";
    public static final String END_DATE_EXTRA = "end_date_extra";
    public static final String FLIGHT_BRIEFING_EXTRA = "flight_briefing_extra";


    //Intent request codes
    public static final int CUSTOM_PROPERTIES_REQUEST_CODE = 112;
    public static final int CREATE_AIRCRAFT_REQUEST_CODE = 2494;
    public static final int EDIT_PROFILE_REQUEST_CODE = 6211;
}
