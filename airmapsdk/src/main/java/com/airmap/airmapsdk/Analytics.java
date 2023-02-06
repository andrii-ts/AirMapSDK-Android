package com.airmap.airmapsdk;

public class Analytics {

    private static AnalyticsTracker tracker;

    public static void init(AnalyticsTracker analyticsTracker) {
        tracker = analyticsTracker;
    }

    public static void logEvent(String section, String action, String label) {
        if (tracker != null) {
            tracker.logEvent(section, action, label);
        }
    }

    public static void logEvent(String section, String action, String label, int value) {
        if (tracker != null) {
            tracker.logEvent(section, action, label, value);
        }
    }

    public static void logEvent(String section, String action, String label, String value) {
        if (tracker != null) {
            tracker.logEvent(section, action, label, value);
        }
    }

    public static void report(Throwable t) {
        if (tracker != null) {
            tracker.report(t);
        }
    }

    public static void logDebug(String key, String value) {
        if (tracker != null) {
            tracker.logDebug(key, value);
        }
    }

    public static class Page {
        public static final String CREATE_FLIGHT = "Create_Flight";
        public static final String POINT_CREATE_FLIGHT = "Create_Flight_Point";
        public static final String PATH_CREATE_FLIGHT = "Create_Flight_Path";
        public static final String POLYGON_CREATE_FLIGHT = "Create_Flight_Polygon";
        public static final String DETAILS_CREATE_FLIGHT = "Create_Flight_Details";
        public static final String PERMITS_CREATE_FLIGHT = "Create_Flight_Permits";
        public static final String AVAILABLE_PERMITS_CREATE_FLIGHT = "Create_Flight_Available_Permits";
        public static final String NOTICES_CREATE_FLIGHT = "Create_Flight_Flight_Notices";
        public static final String REVIEW_CREATE_FLIGHT = "Create_Flight_Review";

        public static final String PERMIT_DETAILS = "Permit_Details";
        public static final String PILOT_PROFILE = "Pilot_Profile";
        public static final String PHONE_NUMBER_PHONE_VERIFICATION = "Phone_Verification_Phone_Number";
        public static final String SMS_CODE_PHONE_VERIFICATION = "Phone_Verification_SMS_Code";

        public static final String LIST_AIRCRAFT = "List_Aircraft";
        public static final String SELECT_AIRCRAFT = "Select_Aircraft";
        public static final String CREATE_AIRCRAFT = "Create_Aircraft";
        public static final String MANUFACTURERS_CREATE_AIRCRAFT = "Create_Aircraft_Manufacturers";
        public static final String MODEL_CREATE_AIRCRAFT = "Create_Aircraft_Model";

        public static final String ADVISORIES = "Advisories";
        public static final String MAP = "Map";
    }

    public static class Event {
        public static final String unspecified = "";
        public static final String verifyEmail = "email_verification_needed";
        public static final String noNetworkConnection = "no_network_connect";
        public static final String intro = "intro";
        public static final String futureFlightPlans = "future_flights";
        public static final String map = "map";
        public static final String mapLayers = "map_layers";
        public static final String mapLayersHelp = "uao_near_airports_infographic";
        public static final String search = "search";
        public static final String menu = "menu";
        public static final String myAircraft = "my_aircraft";
        public static final String flightPlans = "flights";
        public static final String activeUserFlightPlans = "active_user_flights";
        public static final String permitWallet = "permit_wallet";
        public static final String about = "about";
        public static final String faq = "faq";
        public static final String settings = "settings";
        public static final String login = "login_registration";
        public static final String myProfile = "my_profile";
        public static final String userProfile = "user_profile";
        public static final String flightPlan = "flight";
        public static final String createAirCraft = "create_aircraft";
        public static final String editAirCraft = "edit_aircraft";
        public static final String aircraftSelector = "aircraft_selector";
        public static final String createFlightPlan = "create_flight";
        public static final String editFlightPlan = "edit_flight";
        public static final String viewFlightPlan = "view_flight";
        public static final String flightRequirmentsForm = "flight_requirements_form";
        public static final String statusBottomSheet = "status_bottom_sheet";
        public static final String logout = "logout";
        public static final String deleteAccount = "delete_account";
        public static final String drawer = "drawer";
        public static final String rules = "rules";
        public static final String advisories = "advisories";
        public static final String flightPlanDraw = "flight_plan_draw";
        public static final String flightPlanCheck = "flight_plan_check";
        public static final String flightPlanBrief = "flight_plan_brief";
        public static final String fly = "fly";
        public static final String deeplink_create_flight = "deeplink_create_flight";
        public static final String dji_fly = "dji_fly";
        public static final String flightDetails = "flightDetails";
        public static final String mainMenu = "main_menu";
        public static final String fly_settings = "in_flight_settings";
    }

    public static class Action {
        public static final String swipe = "swipe";
        public static final String switchControl = "switch";
        public static final String slide = "slide";
        public static final String tap = "tap";
        public static final String zoom = "zoom";
        public static final String toggle = "toggle";
        public static final String upload = "upload";
        public static final String delete = "delete";
        public static final String search = "search";
        public static final String longPress = "longPress";
        public static final String select = "select";
        public static final String drag = "drag";
        public static final String close = "close";
        public static final String exit = "exit";
        public static final String draw = "draw";
        public static final String drop = "drop";
        public static final String start = "start";
        public static final String save = "save";
        public static final String deselect = "deselect";
        public static final String expand = "expand";
        public static final String change = "change";
        public static final String result = "result";
        public static final String success = "success";
        public static final String cancelled = "cancelled";
        public static final String request = "request";
        public static final String cancel = "cancel";
        public static final String show = "show";
    }

    public static class Label {
        public static final String START_CREATE_FLIGHT = "Start Create Flight";
        public static final String POINT = "Point";
        public static final String PATH = "Path";
        public static final String POLYGON = "Polygon";
        public static final String DRAG_POINT = "Drag Point";
        public static final String BUFFER = "Buffer";
        public static final String ZOOM_MAP = "Zoom Map";
        public static final String ADVISORY_ICON = "Advisory Icon";
        public static final String TRASH_ICON = "Trash Icon";
        public static final String NEXT = "Next Button";
        public static final String CANCEL = "Cancel Button";
        public static final String REVIEW = "Review Button";
        public static final String DRAG_PATH_POINT = "Drag Path Point";
        public static final String DRAG_POLYGON_POINT = "Drag Polygon Point";
        public static final String DRAG_NEW_POINT = "Drag New Point";
        public static final String DRAW_PATH = "Draw Path";
        public static final String DRAW_POLYGON = "Draw Polygon";
        public static final String DROP_POINT_TRASH_ICON = "Drop Point Trash Icon";

        public static final String ALTITUDE_SLIDER = "Altitude Slider";
        public static final String FLIGHT_START_TIME = "Flight Start Time";
        public static final String FLIGHT_END_TIME = "Flight End Time";
        public static final String SELECT_PILOT = "Select Pilot";
        public static final String SELECT_AIRCRAFT = "Select Aircraft";
        public static final String NEW_AIRCRAFT = "New Aircraft Button";
        public static final String EDIT_AIRCRAFT = "Edit Aircraft Button";
        public static final String SHARE_FLIGHT = "Share Flight";
        public static final String AIRMAP_PUBLIC_FLIGHT = "AirMap Public Flight";

        public static final String INFO_FAQ_BUTTON = "Info Button (Permit FAQ's)";
        public static final String SELECT_PERMIT = "Select Permit";
        public static final String SELECT_DIFFERENT_PERMIT = "Select a Different Permit";
        public static final String PERMIT_DETAILS = "Permit Details";

        public static final String SAVE = "Save";
        public static final String DELETE = "Delete";
        public static final String SUBMIT = "Submit Button";
        public static final String SUCCESS = "Success";
        public static final String ERROR = "Error";

        public static final String APPLY_PERMIT_SUCCESS = "Apply Permit Success";
        public static final String APPLY_PERMIT_ERROR = "Apply Permit Error";
        public static final String CREATE_FLIGHT_SUCCESS = "Create Flight Success";
        public static final String CREATE_FLIGHT_ERROR = "Create Flight Error";

        public static final String SELECT_MANUFACTURER = "Select Manufacturer";
        public static final String SELECT_MODEL = "Select Model";

        public static final String TFR_DETAILS = "TFR Details";
        public static final String CLOSE_BUTTON = "Close Button";

        public static final String REVIEW_DETAILS_TAB = "Review Details Tab";
        public static final String REVIEW_PERMITS_TAB = "Review Permits Tab";
        public static final String REVIEW_NOTICES_TAB = "Review Notices Tab";

        public static final String CREATE_FLIGHT_BUTTON = "Create Flight Button";
        public static final String ADVISORY_BUTTON = "Advisory Button";
        public static final String MISSION_TAGS = "Mission Tags";
        public static final String RULES_TAB = "Rules Tab";
        public static final String ADVISORIES_TAB = "Advisories Tab";
        public static final String PICK_ONE = "Pick One";
        public static final String OPTIONAL = "Optional";
        public static final String RULES_INFO = "Rules Info";
        public static final String HEADER = "Header";
        public static final String NEXT_BUTTON = "Next Button";
        public static final String START_TIME = "Start Time";
        public static final String ALTITUDE = "Altitude";
        public static final String PILOT = "Pilot";
        public static final String AIRCRAFT = "Aircraft";
        public static final String FEATURE = "Feature";
        public static final String TOP_NEXT_BUTTON = "Top Next Button";
        public static final String BOTTOM_NEXT_BUTTON = "Bottom Next Button";
        public static final String RULES_VIOLATING = "Rules you may be violating";
        public static final String BOTTOM_SUBMIT_BUTTON = "Bottom Submit Button";
        public static final String TOP_SUBMIT_BUTTON = "Top Submit Button";
        public static final String BRIEF_BUTTON = "Brief Button";
        public static final String END_FLIGHT_BUTTON = "End Flight Button";
        public static final String CANCEL_FLIGHT_BUTTON = "Cancel Flight Button";
        public static final String CONFIRM_END_FLIGHT_BUTTON = "Confirm End Flight Button";
        public static final String CANCEL_END_FLIGHT_BUTTON = "Cancel End Flight Button";
        public static final String PATH_DRAWING_TOOLS = "Path Drawing Tools";
        public static final String POINT_DRAWING_TOOLS = "Point Drawing Tools";
        public static final String POLYGON_DRAWING_TOOLS = "Polygon Drawing Tools";
        public static final String CONFLICTING = "Conflicting Header";
        public static final String NON_CONFLICTING = "Non Conflicting Header";
        public static final String NEEDS_MORE_INFO = "Needing More Info Header";
        public static final String INFORMATIONAL = "Informational Header";

        public static final String DJI_CONNECTION_STATUS = "DJI Connection Status";
        public static final String DJI_CONNECT_BUTTON = "DJI Connect Button";
        public static final String MAP_SLASH_CAMERA_VIEW = "Map/Camera View";
        public static final String TAKE_OFF = "Take off";
        public static final String LAND = "Land";
        public static final String END_FLIGHT_DIALOG = "End Flight Dialog";
        public static final String ANNUNCIATOR_MUTE = "Annunciator Mute";

        public static final String WEBVIEW_LOGIN = "Webview Login";
        public static final String GET_MATCHING_DRONES = "Get Matching Drones";
        public static final String GET_ALL_DRONES = "Get All Drones";
        public static final String SHOW_ALL_DRONES = "Show All Drones";
        public static final String SHOW_MATCHING_DRONES = "Show Matching Drones";

        public static final String IN_FLIGHT_SETTINGS_BUTTON = "In Flight Settings Button";
        public static final String FLIGHT_STATUS_AUDIBLE_ALERTS = "Flight Status Audible Alerts";
        public static final String FLIGHT_STATUS_VISUAL_ALERTS = "Flight Status Visual Alerts";
        public static final String TRAFFIC_AUDIBLE_ALERTS = "Traffic Audible Alerts";
        public static final String TRAFFIC_VISUAL_ALERTS = "Traffic Visual Alerts";
        public static final String GEO_AWARENESS_AUDIBLE_ALERTS = "Geo-Awareness Audible Alerts";
        public static final String GEO_AWARENESS_VISUAL_ALERTS = "Geo-Awareness Visual Alerts";
        public static final String GEOFENCE = "Keep Aircraft From Entering No-Fly Zones";
        public static final String GEOCAGE = "Keep Aircraft From Leaving Flight Plan";

        public static final String TURN_ON_LOCATION_DIALOG = "Turn On Location Dialog";
        public static final String LOCATION_PERMISSIONS = "Location Permissions";
    }

    public static final class Value {
        public static final String AIRMAP = "AirMap";
        public static final String RULES = "Rules";
        public static final String ADVISORIES = "Advisories";
        public static final String YES = "Yes";
        public static final String NO = "No";
        public static final String ENABLED = "Enabled";
        public static final String DISABLED = "Disabled";
    }
}
