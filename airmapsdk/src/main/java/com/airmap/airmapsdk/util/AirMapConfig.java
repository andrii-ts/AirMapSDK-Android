package com.airmap.airmapsdk.util;

import com.airmap.airmapsdk.networking.services.AirMap;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

import timber.log.Timber;

public class AirMapConfig {

    private static final String PROD = "";
    private static final String AVAILABLE_ENVS = "available_envs";
    private static boolean isCustomEnvironment = false;
    private static String customEnvironment;

    public static String getAirshipDevelopmentKey(){
        try {
            return AirMap.getConfig().getJSONObject("airship").getString("developmentAppKey");
        } catch (JSONException e) {
            Timber.e(e, "No Airship developmentAppKey from airmap.config.json");
            return "";
        }
    }

    public static String getAirshipDevelopmentSecret(){
        try {
            return AirMap.getConfig().getJSONObject("airship").getString("developmentAppSecret");
        } catch (JSONException e) {
            Timber.e(e, "No Airship developmentAppSecret from airmap.config.json");
            return "";
        }
    }

    public static String getAirshipProductionKey(){
        try {
            return AirMap.getConfig().getJSONObject("airship").getString("productionAppKey");
        } catch (JSONException e) {
            Timber.e(e, "No Airship productionAppKey from airmap.config.json");
            return "";
        }
    }

    public static String getAirshipProductionSecret(){
        try {
            return AirMap.getConfig().getJSONObject("airship").getString("productionAppSecret");
        } catch (JSONException e) {
            Timber.e(e, "No Airship productionAppSecret from airmap.config.json");
            return "";
        }
    }

    public static String getStatusPageUrl(){
        if(isCustomEnvironment){
            try {
                return AirMap.getConfig().getJSONObject("airmap").getJSONObject(AVAILABLE_ENVS).getJSONObject(customEnvironment).getString("status_page");
            } catch (JSONException e) {
                Timber.e(e, "Error getting status page domain from airmap.config.json. Using fallback");
                return "https://status.airmap.com/";
            }
        } else {
            try {
                return AirMap.getConfig().getJSONObject("app").getString("status_page");
            } catch (JSONException e) {
                Timber.e(e, "Error getting status page domain from airmap.config.json. Using fallback");
                return "https://status.airmap.com/";
            }
        }
    }

    public static String getDomain() {
        if(isCustomEnvironment){
            try {
                return AirMap.getConfig().getJSONObject("airmap").getJSONObject(AVAILABLE_ENVS).getJSONObject(customEnvironment).getString("domain");
            } catch (JSONException e) {
                Timber.w(e, "Error getting airmap domain from airmap.config.json. Using fallback");
                return "airmap.com";
            }
        } else {
            try {
                return AirMap.getConfig().getJSONObject("airmap").getString("domain");
            } catch (JSONException e) {
                Timber.w(e, "Error getting airmap domain from airmap.config.json. Using fallback");
                return "airmap.com";
            }
        }
    }

    public static String getEnvironment() {
        if(isCustomEnvironment){
            try {
                String environment = AirMap.getConfig().getJSONObject("airmap").getJSONObject(AVAILABLE_ENVS).getJSONObject(customEnvironment).getString("environment");
                if ("prod".equals(environment)) {
                    return PROD;
                }
                return environment;
            } catch (JSONException e) {
                Timber.e(e, "No environment key from airmap.config.json");
                return PROD;
            }
        } else {
            try {
                String environment = AirMap.getConfig().getJSONObject("airmap").getString("environment");
                if ("prod".equals(environment)) {
                    return PROD;
                }
                return environment;
            } catch (JSONException e) {
                Timber.e(e, "No environment key from airmap.config.json");
                return PROD;
            }
        }
    }

    public static String getApiKey() {
        if(isCustomEnvironment){
            try {
                return AirMap.getConfig().getJSONObject("airmap").getJSONObject(AVAILABLE_ENVS).getJSONObject(customEnvironment).getString("api_key");
            } catch (JSONException e) {
                Timber.e(e, "Error getting api key from airmap.config.json");
                throw new RuntimeException("Error getting api key from airmap.config.json");
            }
        } else {
            try {
                return AirMap.getConfig().getJSONObject("airmap").getString("api_key");
            } catch (JSONException e) {
                Timber.e(e, "Error getting api key from airmap.config.json");
                throw new RuntimeException("Error getting api key from airmap.config.json");
            }
        }
    }

    public static boolean isStage() {
        try {
            return AirMap.getConfig().getJSONObject("airmap").getString("environment").equals("stage");
        } catch (JSONException e) {
            Timber.e(e, "No environment key from airmap.config.json");
            return false;
        }
    }

    public static String getMapboxApiKey() {
        try {
            return AirMap.getConfig().getJSONObject("mapbox").getString("access_token");
        } catch (JSONException e) {
            Timber.e(e, "Error getting mapbox key from airmap.config.json");
            throw new RuntimeException("Error getting mapbox key from airmap.config.json");
        }
    }

    public static String getClientId() {
        if(isCustomEnvironment){
            try {
                return AirMap.getConfig().getJSONObject("airmap").getJSONObject(AVAILABLE_ENVS).getJSONObject(customEnvironment).getString("client_id");
            } catch (JSONException e) {
                Timber.e(e, "Error getting clientId from airmap.config.json");
                throw new RuntimeException("client_id not found in airmap.config.json");
            }
        } else {
            try {
                return AirMap.getConfig().getJSONObject("airmap").getString("client_id");
            } catch (JSONException e) {
                Timber.e(e, "Error getting clientId from airmap.config.json");
                throw new RuntimeException("client_id not found in airmap.config.json");
            }
        }
    }

    public static String getApiOverride(String key, String fallback) {
        try {
            return AirMap.getConfig().getJSONObject("airmap").getJSONObject("api_overrides").getString(key);
        } catch (JSONException e) {
            Timber.w(e, "No overridden end point found in airmap.config.json for: %s", key);
            return fallback;
        }
    }

    public static String getMapStyleUrl() {
        if(isCustomEnvironment){
            try {
                return AirMap.getConfig().getJSONObject("airmap").getJSONObject(AVAILABLE_ENVS).getJSONObject(customEnvironment).getString("map_style");
            } catch (JSONException e) {
                Timber.w(e, "No map style found in airmap.config.json for. Using fallback");
                return null;
            }
        } else {
            try {
                return AirMap.getConfig().getJSONObject("airmap").getString("map_style");
            } catch (JSONException e) {
                Timber.w(e, "No map style found in airmap.config.json for. Using fallback");
                return null;
            }
        }
    }

    public static String getAboutUrl() {
        try {
            return AirMap.getConfig().getJSONObject("app").getString("about_url");
        } catch (JSONException e) {
            Timber.w(e, "No About URL found in airmap.config.json using fallback");
            return "https://" + getDomain();
        }
    }

    public static String getFAQUrl() {
        try {
            return AirMap.getConfig().getJSONObject("app").getString("faq_url");
        } catch (JSONException e) {
            Timber.w(e, "No FAQ in airmap.config.json using fallback");
            return "https://support.airmap.com/hc/" + Locale.getDefault().getLanguage() + "/categories/115000318348-AirMap-for-Drones";
        }
    }

    public static String getPrivacyUrl() {
        try {
            return AirMap.getConfig().getJSONObject("app").getString("privacy_url");
        } catch (JSONException e) {
            Timber.w(e, "No Privacy URL found in airmap.config.json using fallback");
            return "https://" + getDomain() + "/privacy";
        }
    }

    public static String getTermsUrl() {
        try {
            return AirMap.getConfig().getJSONObject("app").getString("terms_url");
        } catch (JSONException e) {
            Timber.w(e, "No Terms URL found in airmap.config.json using fallback");
            return "https://" + getDomain() + "/terms";
        }
    }

    public static String getFeedbackUrl() {
        try {
            return AirMap.getConfig().getJSONObject("app").getString("feedback_url");
        } catch (JSONException e) {
            Timber.w(e, "No Feedback URL found in airmap.config.json using fallback");
            return "https://support.airmap.com/hc/en-us/requests/new";
        }
    }

    public static String[] getIntroImages() {
        try {
            JSONArray jsonArray = AirMap.getConfig().getJSONObject("app").getJSONArray("intro_images");
            String[] urls = new String[jsonArray.length()];

            for (int i = 0; i < urls.length; i++) {
                urls[i] = (String) jsonArray.get(i);
            }
            return urls;
        } catch (JSONException e) {
            Timber.w(e, "No intro images found in airmap.config.json using fallback");

            //fallback urls
            return new String[] {
                    "https://cdn.airmap.io/mobile/android/intro/welcome_intro1.png",
                    "https://cdn.airmap.io/mobile/android/intro/welcome_intro2.png",
                    "https://cdn.airmap.io/mobile/android/intro/welcome_intro3.png",
                    "https://cdn.airmap.io/mobile/android/intro/welcome_intro4.png"
            };
        }
    }

    public static String getTwitterHandle() {
        try {
            return AirMap.getConfig().getJSONObject("app").getString("twitter_handle");
        } catch (JSONException e) {
            Timber.e("Unable to get twitter handle");
            return null;
        }
    }

    public static String getDomainForThirdPartyAPI(String key) {
        try {
            return AirMap.getConfig().getJSONObject(key).getString("api");
        } catch (JSONException e) {
            Timber.e("Unable to get domain for third party: " + key);
            return null;
        }
    }

    public static String getFrontendForThirdPartyAPI(String key) {
        try {
            return AirMap.getConfig().getJSONObject(key).getString("frontend");
        } catch (JSONException e) {
            Timber.e("Unable to get frontend for third party: " + key);
            return null;
        }
    }

    public static String getAppIdForThirdPartyAPI(String key) {
        try {
            return AirMap.getConfig().getJSONObject(key).getString("app_id");
        } catch (JSONException e) {
            Timber.e("Unable to get app id for third party: " + key);
            return null;
        }
    }

    public static JSONArray getMapAllowedJurisdictions(){
        try {
            return AirMap.getConfig().getJSONObject("app").getJSONObject("map").getJSONArray("allowed_jurisdictions");
        } catch (JSONException e){
            Timber.e("Unable to get Map Allowed Jurisdictions");
            return null;
        }
    }

    public static LatLng getMapLocation(){
        try{
            return new LatLng(AirMap.getConfig().getJSONObject("app").getJSONObject("map").getJSONObject("location").getDouble("latitude"),
                    AirMap.getConfig().getJSONObject("app").getJSONObject("map").getJSONObject("location").getDouble("longitude"));
        } catch (JSONException e){
            Timber.e(e);
            return null;
        }
    }

    public static int getMapZoom(){
        try{
            return AirMap.getConfig().getJSONObject("app").getJSONObject("map").getInt("zoom");
        } catch (JSONException e){
            Timber.e(e);
            return 0;
        }
    }

    public static int getMapMinZoom(){
        try{
            return AirMap.getConfig().getJSONObject("app").getJSONObject("map").getInt("min_zoom");
        } catch (JSONException e){
            Timber.e(e);
            return 0;
        }
    }

    public static LatLngBounds getMapBounds(){
        try{
            return new LatLngBounds.Builder()
                    .include(new LatLng(AirMap.getConfig().getJSONObject("app").getJSONObject("map").getJSONObject("bounds").getJSONObject("northeast").getDouble("latitude"),
                            AirMap.getConfig().getJSONObject("app").getJSONObject("map").getJSONObject("bounds").getJSONObject("northeast").getDouble("longitude")))
                    .include(new LatLng(AirMap.getConfig().getJSONObject("app").getJSONObject("map").getJSONObject("bounds").getJSONObject("southwest").getDouble("latitude"),
                            AirMap.getConfig().getJSONObject("app").getJSONObject("map").getJSONObject("bounds").getJSONObject("southwest").getDouble("longitude")))
                    .build();
        } catch (JSONException e){
            Timber.e(e);
            return null;
        }
    }

    public static void setIsCustomEnvironment(boolean bool) {
        isCustomEnvironment = bool;
    }

    public static void setCustomEnvironment(String string) {
        customEnvironment = string;
    }
}
