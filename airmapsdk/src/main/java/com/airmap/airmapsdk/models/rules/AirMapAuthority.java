package com.airmap.airmapsdk.models.rules;

import com.airmap.airmapsdk.models.AirMapBaseModel;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Objects;

import static com.airmap.airmapsdk.util.Utils.optString;

public class AirMapAuthority implements Serializable, AirMapBaseModel {

    private String id;
    private String name;
    private String facility;

    public AirMapAuthority() {
    }

    public AirMapAuthority(JSONObject jsonObject) {
        constructFromJson(jsonObject);
    }

    @Override
    public AirMapBaseModel constructFromJson(JSONObject json) {
        setId(optString(json, "id"));
        setName(optString(json, "name"));
        setFacility(optString(json, "facility"));
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFacility() {
        return facility;
    }

    public void setFacility(String facility) {
        this.facility = facility;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AirMapAuthority that = (AirMapAuthority) o;
        if(id == that.id){
            return true;
        } else {
            return false;
        }
    }
}
