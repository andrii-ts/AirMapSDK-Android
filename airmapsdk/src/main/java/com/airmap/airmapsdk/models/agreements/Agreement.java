package com.airmap.airmapsdk.models.agreements;

import com.airmap.airmapsdk.models.AirMapBaseModel;

import org.json.JSONObject;

import java.io.Serializable;
import static com.airmap.airmapsdk.util.Utils.optString;

import java.util.Objects;

public class Agreement implements Serializable, AirMapBaseModel {

    private String id;
    private String version;
    private String type;
    private String documentText;

    public Agreement(JSONObject jsonObject){
        constructFromJson(jsonObject);
    }

    public Agreement() {

    }

    @Override
    public AirMapBaseModel constructFromJson(JSONObject json) {
        if(json != null){
            setId(optString(json, "id"));
            setVersion(optString(json, "version"));
            setType(optString(json, "type"));
            if(json.optJSONObject("document") != null){
                setDocumentText(optString(json.optJSONObject("document"), "text"));
            }
        }
        return this;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDocumentText() {
        return documentText;
    }

    public void setDocumentText(String documentText) {
        this.documentText = documentText;
    }

    @Override
    public String toString() {
        return "Agreement{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
