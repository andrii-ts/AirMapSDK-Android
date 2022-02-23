package com.airmap.airmapsdk.networking.services;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.Analytics;
import com.airmap.airmapsdk.auth.AuthConstants;
import com.airmap.airmapsdk.auth.LoginActivity;
import com.airmap.airmapsdk.models.AirMapToken;
import com.airmap.airmapsdk.networking.callbacks.AirMapAuthenticationCallback;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.callbacks.GenericOkHttpCallback;
import com.airmap.airmapsdk.util.AirMapConfig;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
//import net.openid.appauth.browser.BrowserBlacklist;
import net.openid.appauth.browser.Browsers;
import net.openid.appauth.browser.VersionRange;
import net.openid.appauth.browser.VersionedBrowserMatcher;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Response;
import timber.log.Timber;

import static com.airmap.airmapsdk.networking.services.AirMap.getClient;

@SuppressWarnings("WeakerAccess")
public class AuthService extends BaseService {

    public static void loginOrSignup(Activity activity, AirMapAuthenticationCallback callback) {
        if (!TextUtils.isEmpty(AirMap.getRefreshToken(activity))) {
            logout(activity);
        }

        callback.registerReceiver(activity);

        AppAuthConfiguration appAuthConfig = new AppAuthConfiguration.Builder()
                .build();

        AuthorizationServiceConfiguration serviceConfig =
                new AuthorizationServiceConfiguration(
                        Uri.parse(AuthService.loginUrl),
                        Uri.parse(AuthService.refreshTokenUrl));

        Uri redirectUri = new Uri.Builder()
                .scheme(activity.getPackageName())
                .authority(AuthConstants.REDIRECT_HOST)
                .appendPath(AuthConstants.REDIRECT_PATH)
                .build();

        AuthorizationRequest.Builder authRequestBuilder =
                new AuthorizationRequest.Builder(
                        serviceConfig,
                        AirMapConfig.getClientId(),
                        ResponseTypeValues.CODE,
                        redirectUri);

        AuthorizationRequest authRequest = authRequestBuilder
                .setScope(AuthConstants.SCOPE)
                .setPrompt(AuthorizationRequest.Prompt.LOGIN)
                .build();

        AuthorizationService authService = new AuthorizationService(activity, appAuthConfig);

        if (authService.getBrowserDescriptor() == null) {
            Toast.makeText(activity, "Must install internet browser to login", Toast.LENGTH_SHORT).show();
            Analytics.report(new ActivityNotFoundException("No browser installed for login!"));
        } else {
            authService.performAuthorizationRequest(
                    authRequest,
                    PendingIntent.getActivity(activity, 0, new Intent(activity, LoginActivity.class), 0),
                    PendingIntent.getActivity(activity, 0, new Intent(activity, LoginActivity.class), 0));
        }
    }

    public static Call performAnonymousLogin(String userId, final AirMapCallback<Void> callback) {
        Map<String, String> params = new HashMap<>();
        params.put("user_id", userId);
        return getClient().post(anonymousLoginUrl, params, new GenericOkHttpCallback(new AirMapCallback<AirMapToken>() {
            @Override
            public void onSuccess(AirMapToken response) {
                AirMap.setAuthToken(response.getAuthToken());
                callback.success(null);
            }

            @Override
            public void onError(AirMapException e) {
                callback.error(e);
            }
        }, AirMapToken.class));
    }

    public static void refreshAccessToken(Context context, String refreshToken) {
        Map<String,String> params = new HashMap<>();
        params.put("scope", AuthConstants.SCOPE);
        params.put("client_id", AirMapConfig.getClientId());
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);

        try {
            String json = AirMap.getClient().postSynchronous(refreshTokenUrl, params);
            JSONObject jsonObject = new JSONObject(json);

            // set access token
            String accessToken = jsonObject.getString("access_token");
            AirMap.setAuthToken(accessToken);

            // save new refresh & access token
            String newRefreshToken = jsonObject.getString("refresh_token");
            AirMap.saveTokens(context, accessToken, newRefreshToken);
        } catch (JSONException | IOException e) {
            Timber.e(e,"Failed to refresh access token");
        }
    }

    public static void refreshAccessToken(final Context context, String refreshToken, final AirMapCallback<Void> listener) {
        Map<String,String> params = new HashMap<>();
        params.put("scope", AuthConstants.SCOPE);
        params.put("client_id", AirMapConfig.getClientId());
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);

        AirMap.getClient().post(refreshTokenUrl, params, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Timber.e(e,"Failed to refresh access token");
                if (listener != null) {
                    listener.error(new AirMapException(e.getMessage()));
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    // parse response
                    String json = response.body().string();
                    response.body().close();
                    JSONObject jsonObject = new JSONObject(json);

                    // set access token
                    String accessToken = jsonObject.getString("access_token");
                    AirMap.setAuthToken(accessToken);

                    // save new refresh & access token
                    String newRefreshToken = jsonObject.getString("refresh_token");
                    AirMap.saveTokens(context, accessToken, newRefreshToken);

                    // callback
                    if (listener != null) {
                        listener.success(null);
                    }
                } catch (JSONException e) {
                    Timber.e(e, "error parsing json");
                    if (listener != null) {
                        listener.error(new AirMapException(response.code(), e.getMessage()));
                    }
                }
            }
        });
    }

    public static void logout(Context context) {
        Map<String,String> params = new HashMap<>();
        params.put("client_id", AirMapConfig.getClientId());
        params.put("refresh_token", AirMap.getRefreshToken(context));
        AirMap.getClient().post(logoutUrl, params, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Timber.e(e, "Failed to logout");
            }

            @Override
            public void onResponse(Call call, Response response) {
                Timber.e("Logout successful");
            }
        });
    }

    public static void getFirebaseToken(final AirMapCallback<String> listener) {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(loginUrl).newBuilder();
        urlBuilder.addQueryParameter("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        urlBuilder.addQueryParameter("client_id", AirMapConfig.getClientId());
        urlBuilder.addQueryParameter("id_token", AirMap.getAuthToken());
        urlBuilder.addQueryParameter("scope", "openid");
        urlBuilder.addQueryParameter("device", "android_app");
        String url = urlBuilder.build().toString();
        AirMap.getClient().get(url, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (listener != null) {
                    listener.error(new AirMapException(e.getMessage()));
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String json = response.body().string();
                    response.body().close();
                    JSONObject jsonObject = new JSONObject(json);
                    String customToken = jsonObject.getString("id_token");
                    if (listener != null) {
                        listener.success(customToken);
                    }
                } catch (JSONException e) {
                    Timber.e(e, "error parsing json");
                    if (listener != null) {
                        listener.error(new AirMapException(response.code(), e.getMessage()));
                    }
                }
            }
        });
    }

}
