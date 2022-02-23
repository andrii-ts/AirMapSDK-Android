package com.airmap.airmapsdk.models.status;

import com.airmap.airmapsdk.models.AirMapBaseModel;
import com.airmap.airmapsdk.models.Coordinate;
import com.airmap.airmapsdk.models.status.properties.AirMapAirportProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapControlledAirspaceProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapEmergencyProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapHeliportProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapNotamProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapNotificationProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapOptionalProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapParkProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapPowerPlantProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapSchoolProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapSpecialUseProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapTfrProperties;
import com.airmap.airmapsdk.models.status.properties.AirMapWildfireProperties;
import com.airmap.airmapsdk.models.status.timesheet.Timesheet;
import com.airmap.airmapsdk.networking.services.MappingService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

import static com.airmap.airmapsdk.util.Utils.getDateFromIso8601String;
import static com.airmap.airmapsdk.util.Utils.optString;

public class AirMapAdvisory implements Serializable, AirMapBaseModel {
    private String id;
    private String name;
    private String organizationId;
    private MappingService.AirMapAirspaceType type;
    private String city;
    private String state;
    private String country;
    private Date lastUpdated;
    private AirMapColor color;
    private int distance;
    private Coordinate coordinate;
    private String geometryString;
    private AirMapStatusRequirement requirements;
    private List<Timesheet> schedule;
    private int numSimilarAdvisories = 1; //Used only when displaying advisories in command center drawer. Starts at 1, add to for every similar advisory with the same name & height restriction.

    private AirMapOptionalProperties optionalProperties;

    private AirMapAirportProperties airportProperties;
    private AirMapHeliportProperties heliportProperties;
    private AirMapControlledAirspaceProperties controlledAirspaceProperties;
    private AirMapParkProperties parkProperties;
    private AirMapPowerPlantProperties powerPlantProperties;
    private AirMapSchoolProperties schoolProperties;
    private AirMapSpecialUseProperties specialUseProperties;
    private AirMapTfrProperties tfrProperties;
    private AirMapWildfireProperties wildfireProperties;
    private AirMapEmergencyProperties emergencyProperties;
    private AirMapNotamProperties notamProperties;
    private AirMapNotificationProperties notificationProperties;


    /**
     * Initialize an AirMapAdvisory from JSON
     *
     * @param advisoryJson A JSON representation of an AirMapAdvisory
     */
    public AirMapAdvisory(JSONObject advisoryJson) {
        constructFromJson(advisoryJson);
    }

    /**
     * Initialize an AirMapAdvisory with default values
     */
    public AirMapAdvisory() {

    }

    @Override
    public AirMapAdvisory constructFromJson(JSONObject json) {
        if (json != null) {
            setId(optString(json, "id"));
            setName(optString(json, "name"));
            setOrganizationId(optString(json, "organization_id"));
            setType(MappingService.AirMapAirspaceType.fromString(optString(json, "type")));
            setCountry(optString(json, "country"));
            setDistance(json.optInt("distance"));
            setCity(optString(json, "city"));
            setState(optString(json, "state"));
            setColor(AirMapColor.fromString(optString(json, "color")));
            setGeometryString(optString(json, "geometry"));
            double lat = json.optDouble("latitude");
            double lng = json.optDouble("longitude");
            if (lat != Double.NaN && lng != Double.NaN) {
                setCoordinate(new Coordinate(lat, lng));
            }

            String lastUpdated = optString(json, "last_updated");
            setLastUpdated(getDateFromIso8601String(lastUpdated));

            if (json.has("requirements")) {
                setRequirements(new AirMapStatusRequirement(json.optJSONObject("requirements")));
            }

            if (type != null) {
                JSONObject properties = json.optJSONObject("properties");

                // set generic properties (url, description, etc)
                setOptionalProperties(new AirMapOptionalProperties(properties));

                // set type specific properties
                switch (type) {
                    case Airport: {
                        setAirportProperties(new AirMapAirportProperties(properties));
                        break;
                    }
                    case Park: {
                        setParkProperties(new AirMapParkProperties(properties));
                        break;
                    }
                    case SpecialUse: {
                        setSpecialUseProperties(new AirMapSpecialUseProperties(properties));
                        break;
                    }
                    case PowerPlant: {
                        setPowerPlantProperties(new AirMapPowerPlantProperties(properties));
                        break;
                    }
                    case ControlledAirspace: {
                        setControlledAirspaceProperties(new AirMapControlledAirspaceProperties(properties));
                        break;
                    }
                    case School: {
                        setSchoolProperties(new AirMapSchoolProperties(properties));
                        break;
                    }
                    case TFR: {
                        setTfrProperties(new AirMapTfrProperties(properties));
                        break;
                    }
                    case Wildfires:
                    case Fires: {
                        setWildfireProperties(new AirMapWildfireProperties(properties));
                        break;
                    }
                    case Heliport: {
                        setHeliportProperties(new AirMapHeliportProperties(properties));
                        break;
                    }
                    case Emergencies: {
                        setEmergencyProperties(new AirMapEmergencyProperties(properties));
                        break;
                    }
                    case Notam: {
                        setNotamProperties(new AirMapNotamProperties(properties));
                        break;
                    }
                    case Notification: {
                        setNotificationProperties(new AirMapNotificationProperties(properties));
                        break;
                    }
                }
            }

            if(!json.isNull("schedule")){
                Timber.wtf("hey a schedule: " + json.optString("name"));
                List<Timesheet> timesheetList = new ArrayList<>();
                JSONArray timeSheetJson = json.optJSONArray("schedule");
                for(int i = 0; timeSheetJson != null && i < timeSheetJson.length(); i++){
                    timesheetList.add(new Timesheet(timeSheetJson.optJSONObject(i)));
                }
                setSchedule(timesheetList);
                Timber.wtf(this.getSchedule().toString());
            }

        }
        return this;
    }

    public int getDistance() {
        return distance;
    }

    public AirMapAdvisory setDistance(int distance) {
        this.distance = distance;
        return this;
    }

    public String getId() {
        return id;
    }

    public AirMapAdvisory setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public AirMapAdvisory setName(String name) {
        this.name = name;
        return this;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public AirMapAdvisory setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
        return this;
    }

    public MappingService.AirMapAirspaceType getType() {
        return type;
    }

    public AirMapAdvisory setType(MappingService.AirMapAirspaceType type) {
        this.type = type;
        return this;
    }

    public String getCity() {
        return city;
    }

    public AirMapAdvisory setCity(String city) {
        this.city = city;
        return this;
    }

    public String getState() {
        return state;
    }

    public AirMapAdvisory setState(String state) {
        this.state = state;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public AirMapAdvisory setCountry(String country) {
        this.country = country;
        return this;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public AirMapAdvisory setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
        return this;
    }

    public AirMapColor getColor() {
        return color;
    }

    public AirMapAdvisory setColor(AirMapColor color) {
        this.color = color;
        return this;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public AirMapAdvisory setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
        return this;
    }

    public String getGeometryString() {
        return geometryString;
    }

    public AirMapAdvisory setGeometryString(String geometryString) {
        this.geometryString = geometryString;
        return this;
    }

    public AirMapStatusRequirement getRequirements() {
        return requirements;
    }

    public void setRequirements(AirMapStatusRequirement requirements) {
        this.requirements = requirements;
    }

    public AirMapAirportProperties getAirportProperties() {
        return airportProperties;
    }

    public AirMapAdvisory setAirportProperties(AirMapAirportProperties airportProperties) {
        this.airportProperties = airportProperties;
        return this;
    }

    public AirMapHeliportProperties getHeliportProperties() {
        return heliportProperties;
    }

    public AirMapAdvisory setHeliportProperties(AirMapHeliportProperties heliportProperties) {
        this.heliportProperties = heliportProperties;
        return this;
    }

    public AirMapControlledAirspaceProperties getControlledAirspaceProperties() {
        return controlledAirspaceProperties;
    }

    public AirMapAdvisory setControlledAirspaceProperties(AirMapControlledAirspaceProperties controlledAirspaceProperties) {
        this.controlledAirspaceProperties = controlledAirspaceProperties;
        return this;
    }

    public AirMapParkProperties getParkProperties() {
        return parkProperties;
    }

    public AirMapAdvisory setParkProperties(AirMapParkProperties parkProperties) {
        this.parkProperties = parkProperties;
        return this;
    }

    public AirMapPowerPlantProperties getPowerPlantProperties() {
        return powerPlantProperties;
    }

    public AirMapAdvisory setPowerPlantProperties(AirMapPowerPlantProperties powerPlantProperties) {
        this.powerPlantProperties = powerPlantProperties;
        return this;
    }

    public AirMapSchoolProperties getSchoolProperties() {
        return schoolProperties;
    }

    public AirMapAdvisory setSchoolProperties(AirMapSchoolProperties schoolProperties) {
        this.schoolProperties = schoolProperties;
        return this;
    }

    public AirMapSpecialUseProperties getSpecialUseProperties() {
        return specialUseProperties;
    }

    public AirMapAdvisory setSpecialUseProperties(AirMapSpecialUseProperties specialUseProperties) {
        this.specialUseProperties = specialUseProperties;
        return this;
    }

    public AirMapTfrProperties getTfrProperties() {
        return tfrProperties;
    }

    public AirMapAdvisory setTfrProperties(AirMapTfrProperties tfrProperties) {
        this.tfrProperties = tfrProperties;
        return this;
    }

    public AirMapWildfireProperties getWildfireProperties() {
        return wildfireProperties;
    }

    public AirMapAdvisory setWildfireProperties(AirMapWildfireProperties wildfireProperties) {
        this.wildfireProperties = wildfireProperties;
        return this;
    }

    public AirMapEmergencyProperties getEmergencyProperties() {
        return emergencyProperties;
    }

    public AirMapAdvisory setEmergencyProperties(AirMapEmergencyProperties emergencyProperties) {
        this.emergencyProperties = emergencyProperties;
        return this;
    }

    public AirMapNotamProperties getNotamProperties() {
        return notamProperties;
    }

    public AirMapAdvisory setNotamProperties(AirMapNotamProperties notamProperties) {
        this.notamProperties = notamProperties;
        return this;
    }

    public AirMapNotificationProperties getNotificationProperties() {
        return notificationProperties;
    }

    public void setNotificationProperties(AirMapNotificationProperties notificationProperties) {
        this.notificationProperties = notificationProperties;
    }

    public AirMapOptionalProperties getOptionalProperties() {
        return optionalProperties;
    }

    public AirMapAdvisory setOptionalProperties(AirMapOptionalProperties optionalProperties) {
        this.optionalProperties = optionalProperties;
        return this;
    }

    public List<Timesheet> getSchedule() {
        return schedule;
    }

    public AirMapAdvisory setSchedule(List<Timesheet> timesheets) {
        this.schedule = timesheets;
        return this;
    }

    /**
     * Comparison based on ID
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof AirMapAdvisory && getId().equals(((AirMapAdvisory) o).getId());
    }

    @Override
    public String toString() {
        return getName();
    }

    public void incrementNamSimilarAdvisories() {
        numSimilarAdvisories++;
    }

    public int getNumSimilarAdvisories() {
        return numSimilarAdvisories;
    }

    public void setNumSimilarAdvisories(int numSimilarAdvisories) {
        this.numSimilarAdvisories = numSimilarAdvisories;
    }
}
