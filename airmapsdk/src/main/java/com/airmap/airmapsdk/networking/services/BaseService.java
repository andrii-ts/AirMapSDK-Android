package com.airmap.airmapsdk.networking.services;


import android.text.TextUtils;

import static com.airmap.airmapsdk.util.AirMapConfig.getApiOverride;
import static com.airmap.airmapsdk.util.AirMapConfig.getDomain;
import static com.airmap.airmapsdk.util.AirMapConfig.getEnvironment;

@SuppressWarnings({"ConstantConditions", "WeakerAccess"})
public class BaseService {

    //Base Urls
    protected static final String apiUrl = getHost("api");

    //Map
    protected static final String mapTilesVersion = "/v1";
    protected static final String mapTilesBaseUrl = apiUrl + "/maps/v4/tilejson";
    protected static final String mapTilesRulesUrl = apiUrl + "/tiledata" + mapTilesVersion;
    protected static final String mapTilesBaseJurisdictionsUrl = mapTilesRulesUrl + "/base-jurisdiction/{z}/{x}/{y}";

    //Aircraft
    protected static final String aircraftVersion = "/v2";
    protected static final String aircraftBaseUrl = apiUrl + "/aircraft" + aircraftVersion;
    protected static final String aircraftManufacturersUrl = aircraftBaseUrl + "/manufacturer";
    protected static final String aircraftModelsUrl = aircraftBaseUrl + "/model";
    protected static final String aircraftModelUrl = aircraftModelsUrl + "/%s"; //Replace %s with id using String.format

    //Flight
    protected static final String flightVersion = "/v2";
    protected static final String flightBaseUrl = apiUrl + "/flight" + flightVersion;
    protected static final String flightGetAllUrl = flightBaseUrl;
    protected static final String flightByIdUrl = flightBaseUrl + "/%s"; //Replace %s with id using String.format
    protected static final String flightDeleteUrl = flightByIdUrl + "/delete"; //Replace %s with id using String.format
    protected static final String flightEndUrl = flightByIdUrl + "/end"; //Replace %s with id using String.format
    protected static final String flightStartCommUrl = flightByIdUrl + "/start-comm"; //Replace %s with id using String.format
    protected static final String flightEndCommUrl = flightByIdUrl + "/end-comm"; //Replace %s with id using String.format

    //Flight Plan
    protected static final String flightPlanUrl = getApiOverride("flightplan", flightBaseUrl + "/plan");
    protected static final String flightPlanByFlightIdUrl = flightBaseUrl + "/%s/plan";
    protected static final String flightPlanPatchUrl = flightPlanUrl + "/%s";
    protected static final String flightPlanBatchAuthorizationsUrl = flightBaseUrl + "/plan/batch/authorizations";
    protected static final String flightPlanBriefingUrl = flightPlanPatchUrl + "/briefing";
    protected static final String flightPlanSubmitUrl = flightPlanPatchUrl + "/submit";
    protected static final String flightFeaturesByPlanIdUrl = flightPlanPatchUrl + "/features";

    //Weather
    protected static final String weatherVersion = "/v1";
    protected static final String weatherUrl = apiUrl + "/advisory" + weatherVersion + "/weather";

    //Pilot
    protected static final String pilotVersion = "/v2";
    protected static final String pilotBaseUrl = apiUrl + "/pilot" + pilotVersion;
    protected static final String pilotByIdUrl = pilotBaseUrl + "/%s"; //Replace %s with id using String.format
    protected static final String pilotAircraftUrl = pilotByIdUrl + "/aircraft"; //Replace %s with id using String.format
    protected static final String pilotAircraftByIdUrl = pilotAircraftUrl + "/%s"; //Replace BOTH occurrences of %s with user id and aircraft id, using String.format
    protected static final String pilotSendVerifyUrl = pilotByIdUrl + "/phone/send_token"; //Replace %s with id using String.format
    protected static final String pilotVerifyUrl = pilotByIdUrl + "/phone/verify_token"; //Replace %s with id using String.format

    //Status
    protected static final String statusVersion = "/v2";
    protected static final String statusBaseUrl = apiUrl + "/status" + statusVersion;
    protected static final String statusPointUrl = statusBaseUrl + "/point";
    protected static final String statusPathUrl = statusBaseUrl + "/path";
    protected static final String statusPolygonUrl = statusBaseUrl + "/polygon";

    //Airspace
    protected static final String airspaceVersion = "/v2";
    protected static final String airspaceBaseUrl = apiUrl + "/airspace" + airspaceVersion;
    protected static final String airspaceByIdUrl = airspaceBaseUrl + "/%s"; //Replace %s with id using String.format
    protected static final String airspaceByIdsUrl = airspaceBaseUrl + "/list";

    //Traffic Alerts
    protected static final String mqttBaseUrl = getApiOverride("mqtt", getHost("ssl", "mqtt")) + ":8883";
    protected static final String trafficAlertChannel = "uav/traffic/alert/%s"; //Replace %s with id using String.format. *Don't* end this url with a /
    protected static final String situationalAwarenessChannel = "uav/traffic/sa/%s"; //Replace %s with id using String.format

    //Telemetry
    protected static final String telemetryBaseUrl = getApiOverride("telemetry_host", getHost(null, "telemetry"));
    protected static final int telemetryPort = Integer.parseInt(getApiOverride("telemetry_port", "16060"));

    //Auth
    protected static final String anonymousLoginUrl = apiUrl + "/auth/v1/anonymous/token";
    protected static final String authBaseUrl = getHost("auth") + "/realms/airmap/protocol/openid-connect/";
    protected static final String loginUrl = authBaseUrl + "auth";
    protected static final String refreshTokenUrl = authBaseUrl + "token";
    protected static final String logoutUrl = authBaseUrl + "logout";

    //Rules
    protected static final String rulesetsVersion = "/v1";
    protected static final String rulesetBaseUrl = getApiOverride("rules", apiUrl + "/rules" + rulesetsVersion);
    protected static final String rulesetsByIdUrl = rulesetBaseUrl + "/rule"; //Replace %s with id using String.format
    protected static final String rulesetByIdUrl = rulesetBaseUrl + "/%s"; //Replace %s with id using String.format
    protected static final String rulesByIdUrl = rulesetBaseUrl + "/%s"; //Replace %s with id using String.format
    protected static final String advisoriesUrl = apiUrl + "/advisory" + rulesetsVersion + "/airspace";
    protected static final String evaluationUrl = rulesetBaseUrl + "/evaluation";

    //System Status
    public static final String systemStatusVersion = "v1/";
    public static final String systemStatusEndpoint = "/system/" + systemStatusVersion + "status";
    public static final String systemStatusBaseUrl = apiUrl + systemStatusEndpoint;
    public static final String systemStatusSocketUrl = getHost("wss", "api") + systemStatusEndpoint + "/monitor";

    //Agreements
    public static final String agreementsVersion = "/v2";
    public static final String agreementsEndpoint = "/agreements";
    public static final String agreementsBaseUrl = apiUrl + agreementsEndpoint + agreementsVersion;
    public static final String listAgreementsUrl = agreementsBaseUrl + "/authority/%s";
    public static final String getAgreementUrl = agreementsBaseUrl + "/agreement/%s";
    public static final String getAgreementPdfUrl = agreementsBaseUrl + "/agreement/%s/pdf";
    public static final String getAgreementStatusUrl = agreementsBaseUrl + "/agreement/%s/agreed";
    public static final String acceptAgreementUrl = agreementsBaseUrl + "/agreement/%s/agree";

    private static String getHost(String service) {
        return getHost("https", service);
    }

    private static String getHost(String protocol, String service) {
        String env = TextUtils.isEmpty(getEnvironment()) ? "" : getEnvironment() + ".";
        protocol = TextUtils.isEmpty(protocol) ? "" : protocol + "://";
        return protocol + env + service + "." + getDomain();
    }
}
