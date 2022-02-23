package com.airmap.airmapsdk.models.agreements;

import com.airmap.airmapsdk.models.AirMapBaseModel;

import org.json.JSONObject;

import java.io.Serializable;

public class AgreementStatus implements Serializable, AirMapBaseModel {

    private boolean hasAgreedToStatus;

    public AgreementStatus() {

    }

    public AgreementStatus(JSONObject jsonObject){
        constructFromJson(jsonObject);
    }

    @Override
    public AirMapBaseModel constructFromJson(JSONObject json) {
        setHasAgreedToStatus(json.optBoolean("has_agreed_to_latest_version"));
        return this;
    }

    public boolean isHasAgreedToStatus() {
        return hasAgreedToStatus;
    }

    public void setHasAgreedToStatus(boolean hasAgreedToStatus) {
        this.hasAgreedToStatus = hasAgreedToStatus;
    }

    @Override
    public String toString() {
        return "AgreementStatus{" +
                "hasAgreedToStatus=" + hasAgreedToStatus +
                '}';
    }
}
