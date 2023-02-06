package com.airmap.airmapsdk.models.flight;

import com.airmap.airmapsdk.models.AirMapBaseModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

public class AirMapFlightLaancStatus implements Serializable, AirMapBaseModel {

    private String authorizerID = "";
    private String redirectUrl = "";
    private boolean laancUp = false;

    @Override
    public AirMapFlightLaancStatus constructFromJson(JSONObject json) {
        try {
            if (json != null){
                JSONArray authorizers = json.optJSONArray("authorizers");
                if (authorizers != null){
                    if (authorizers.length() > 0){
                        for(int i = 0; i < authorizers.length(); i ++) {
                            JSONObject authorizer = authorizers.getJSONObject(i);
                            authorizerID = authorizer.optString("id");
                            redirectUrl = authorizer.optString("redirectUrl");
                            laancUp = authorizer.optBoolean("up");
                        }
                    }
                } else {
                    //If the authorizers array is empty it is because no relevant authorities are associated with the query, and no notification needs to be shown to the user.
                    laancUp = true;
                }
            }
        } catch (JSONException JSONe){
            JSONe.printStackTrace();
            return null; //If unable to process json there might be a problem with the call to the endpoint. Callback will handle null as bad request.
        }
        return this;
    }

    public String getAuthorizerID() {
        return authorizerID;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public boolean isLaancUp() {
        return laancUp;
    }
}
