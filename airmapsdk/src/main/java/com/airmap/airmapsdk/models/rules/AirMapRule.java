package com.airmap.airmapsdk.models.rules;

import android.text.TextUtils;

import com.airmap.airmapsdk.models.AirMapBaseModel;
import com.airmap.airmapsdk.models.flight.AirMapFlightFeature;
import com.airmap.airmapsdk.util.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

import static com.airmap.airmapsdk.util.Utils.optString;

public class AirMapRule implements Serializable, AirMapBaseModel {

    public enum Status {
        Conflicting, NotConflicting, MissingInfo, InformationRules, Unknown;

        public static Status fromString(String text) {
            switch (text.toLowerCase()) {
                case "conflicting":
                    return Conflicting;
                case "not_conflicting":
                    return NotConflicting;
                case "missing_info":
                    return MissingInfo;
                case "informational":
                case "information_rules":
                    return InformationRules;
                default:
                    return Unknown;
            }
        }

        public int intValue() {
            switch (this) {
                case Conflicting:
                    return 0;
                case MissingInfo:
                    return 1;
                case InformationRules:
                    return 2;
                case NotConflicting:
                    return 3;
            }
            return -1;
        }
    }

    private String shortText;
    private String description;
    private Status status;
    private int displayOrder;
    private List<AirMapFlightFeature> flightFeatures;

    public AirMapRule(JSONObject resultJson) {
        constructFromJson(resultJson);
    }

    @Override
    public AirMapRule constructFromJson(JSONObject json) {
        if (json != null) {
            setShortText(optString(json, "short_text"));
            setDescription(optString(json, "description"));
            setStatus(AirMapRule.Status.fromString(optString(json, "status")));
            setDisplayOrder(json.optInt("display_order", 90000));

            List<AirMapFlightFeature> flightFeatures = new ArrayList<>();
            if (json.has("flight_features")) {
                try{
                    JSONArray flightFeaturesArray = json.optJSONArray("flight_features");
                    for (int i = 0; i < flightFeaturesArray.length(); i++) {
                        flightFeatures.add(new AirMapFlightFeature(flightFeaturesArray.optJSONObject(i)));
                    }
                } catch (NullPointerException npe){
                    Timber.e("NullPointerException when trying to add flight features from rule json");
                    npe.printStackTrace();
                }
            }
            setFlightFeatures(flightFeatures);
        }
        return this;
    }

    public String getDescription() {
        return description;
    }

    public AirMapRule setDescription(String description) {
        this.description = description;
        return this;
    }

    public AirMapRule.Status getStatus() {
        return status;
    }

    public AirMapRule setStatus(AirMapRule.Status status) {
        this.status = status;
        return this;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getShortText() {
        if (!TextUtils.isEmpty(shortText) && !shortText.toLowerCase().equals("not available.")) {
            return shortText;
        }

        return description;
    }

    public void setShortText(String shortText) {
        this.shortText = shortText;
    }

    public List<AirMapFlightFeature> getFlightFeatures() {
        return flightFeatures;
    }

    public void setFlightFeatures(List<AirMapFlightFeature> flightFeatures) {
        this.flightFeatures = flightFeatures;
    }

    @Override
    public String toString() {
        if (!TextUtils.isEmpty(description) && !description.toLowerCase().equals("not available.")) {
            return description;
        }

        return shortText;
    }

    @Override
    public boolean equals(Object o) {
        return o != null && o instanceof AirMapRule && ((AirMapRule) o).getShortText().equals(getShortText())
                && ((AirMapRule) o).getDescription().equals(getDescription()) && ((AirMapRule) o).getDisplayOrder() == getDisplayOrder();
    }
}
