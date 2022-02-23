package com.airmap.airmapsdk.ui.views;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.core.app.ActivityCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

import com.airmap.airmapsdk.AirMapException;
import com.airmap.airmapsdk.Analytics;
import com.airmap.airmapsdk.R;
import com.airmap.airmapsdk.controllers.MapDataController;
import com.airmap.airmapsdk.controllers.MapStyleController;
import com.airmap.airmapsdk.models.Container;
import com.airmap.airmapsdk.models.TemporalFilter;
import com.airmap.airmapsdk.models.rules.AirMapJurisdiction;
import com.airmap.airmapsdk.models.rules.AirMapRuleset;
import com.airmap.airmapsdk.models.status.AirMapAdvisory;
import com.airmap.airmapsdk.models.status.AirMapAirspaceStatus;
import com.airmap.airmapsdk.networking.callbacks.AirMapCallback;
import com.airmap.airmapsdk.networking.services.AirMap;
import com.airmap.airmapsdk.networking.services.MappingService;
import com.airmap.airmapsdk.ui.activities.MyLocationMapActivity;
import com.airmap.airmapsdk.util.Utils;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.MultiPolygon;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentOptions;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.module.http.HttpRequestUtil;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.Layer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Dispatcher;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import timber.log.Timber;

import static com.airmap.airmapsdk.util.Utils.getLanguageTag;

public class AirMapMapView extends MapView implements MapView.OnDidFailLoadingMapListener, MapView.OnDidFinishLoadingStyleListener,
        MapView.OnDidFinishRenderingMapListener, MapView.OnCameraDidChangeListener, MapboxMap.OnMapClickListener, MapDataController.Callback {

    private MapboxMap map;

    private MapStyleController mapStyleController;
    private MapDataController mapDataController;
    private AdvisorySelector advisorySelector;

    // optional callbacks
    private List<OnMapLoadListener> mapLoadListeners;
    private List<OnMapDataChangeListener> mapDataChangeListeners;
    private List<OnAdvisoryClickListener> advisoryClickListeners;

    private boolean useSIMeasurements;
    private boolean isMapLoaded = false;

    private TemporalFilter temporalFilter;

    public AirMapMapView(@NonNull Context context, Configuration configuration, @Nullable MappingService.AirMapMapTheme mapTheme) {
        super(context);
        init(configuration, mapTheme);
    }

    public AirMapMapView(@NonNull Context context) {
        this(context, null, 0);
    }

    public AirMapMapView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AirMapMapView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);


        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.AirMapMapView,
                0, 0);

        int mapThemeValue;
        try {
            mapThemeValue = a.getInteger(R.styleable.AirMapMapView_mapTheme, -1);
        } finally {
            a.recycle();
        }

        MappingService.AirMapMapTheme mapTheme = null;
        switch (mapThemeValue) {
            case 0:
                mapTheme = MappingService.AirMapMapTheme.Standard;
                break;
            case 1:
                mapTheme = MappingService.AirMapMapTheme.Dark;
                break;
            case 2:
                mapTheme = MappingService.AirMapMapTheme.Light;
                break;
            case 3:
                mapTheme = MappingService.AirMapMapTheme.Satellite;
                break;
        }

        Configuration defaultConfig = new AutomaticConfiguration();
        init(defaultConfig, mapTheme);
    }

    private void init(Configuration configuration, @Nullable MappingService.AirMapMapTheme mapTheme) {
        addHeaders();

        mapLoadListeners = new ArrayList<>();
        mapDataChangeListeners = new ArrayList<>();
        advisoryClickListeners = new ArrayList<>();

        advisorySelector = new AdvisorySelector();

        // default data controller
        mapDataController = new MapDataController(this, configuration);

        mapStyleController = new MapStyleController(this, mapTheme, new MapStyleController.Callback() {
            @Override
            public void onMapStyleReset() {
                mapDataController.onMapReset();
            }

            @Override
            public void onMapStyleLoaded() {
                Timber.v("onMapStyleLoaded: %s", getMap().getCameraPosition());
                mapDataController.onMapLoaded();
                isMapLoaded = true;

                for (OnMapLoadListener mapLoadListener : mapLoadListeners) {
                    mapLoadListener.onMapLoaded();
                }
            }
        });

        addOnDidFailLoadingMapListener(this);
        addOnDidFinishRenderingMapListener(this);
        addOnCameraDidChangeListener(this);
    }

    private void addHeaders() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequestsPerHost(20);
        OkHttpClient mapboxHttpClient = new OkHttpClient.Builder().dispatcher(dispatcher).addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request.Builder request = chain.request().newBuilder()
                        .addHeader("x-Api-Key", AirMap.getApiKey());

                if (!TextUtils.isEmpty(getLanguageTag())) {
                    request.addHeader("Accept-Language", getLanguageTag());
                }

                // Used for Enterprise Jurisdictions
                if (!TextUtils.isEmpty(AirMap.getAuthToken())) {
                    request.addHeader("Authorization", "Bearer " + AirMap.getAuthToken());
                }

                return chain.proceed(request.build());
            }
        }).build();
        HttpRequestUtil.setOkHttpClient(mapboxHttpClient);
    }

    public void configure(Configuration configuration) {
        mapDataController.configure(configuration);
    }

    public void setMeasurementSystem(boolean useSIMeasurements) {
        this.useSIMeasurements = useSIMeasurements;
    }

    /**
     * Override data controller
     *
     * @param controller
     */
    public void setMapDataController(MapDataController controller) {
        // destroy old data controller (unsubscribe from rx)
        mapDataController.onDestroy();

        this.mapDataController = controller;
    }

    /**
     * Go to next theme (Standard to Dark to Light to Satellite to Standard to Dark...)
     */
    public void rotateMapTheme() {
        // check if map is ready yet
        if (map == null || advisorySelector.isBusy()) {
            return;
        }
        mapStyleController.rotateMapTheme();
    }

    /**
     * Explicitly set theme (Standard, Dark, Light, Satellite
     */
    public void setMapTheme(MappingService.AirMapMapTheme theme) {
        // check if map is ready yet
        if (map == null || advisorySelector.isBusy()) {
            return;
        }
        mapStyleController.updateMapTheme(theme);
    }

    public void reset() {
        mapStyleController.reset();
    }

    @Override
    public void getMapAsync(@NonNull final OnMapReadyCallback callback) {
        super.getMapAsync(mapboxMap -> {
            map = mapboxMap;
            map.addOnMapClickListener(AirMapMapView.this);
            map.getUiSettings().setLogoGravity(Gravity.BOTTOM | Gravity.END); // Move to bottom right
            map.getUiSettings().setAttributionGravity(Gravity.BOTTOM | Gravity.END); // Move to bottom right
            map.setPrefetchesTiles(true);
            mapStyleController.onMapReady();
            callback.onMapReady(mapboxMap);
        });
    }

    @Override
    public void onCameraDidChange(boolean animated) {
        if (mapDataController != null) {
            mapDataController.onMapRegionChanged();
        }
    }

    @Override
    public void onDidFinishLoadingStyle() {
        // add my location w/ permission check
        LocationComponent locationComponent = map.getLocationComponent();
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) getContext(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MyLocationMapActivity.REQUEST_LOCATION_PERMISSION);
            return;
        }
        LocationComponentOptions options = LocationComponentOptions.builder(getContext())
                .elevation(2f)
                .accuracyAlpha(0f)
                .enableStaleState(false)
                .build();

        locationComponent.activateLocationComponent(getContext(), map.getStyle(), options);
        locationComponent.setLocationComponentEnabled(true);
    }

    @Override
    public void onDidFailLoadingMap(String errorMessage) {
        // Devices without internet connection will not be able to load the mapbox map
        //TODO: add more sophisticated check (like actually check style url for 200)
        if (!Utils.isNetworkConnected(getContext())) {
            for (OnMapLoadListener mapLoadListener : mapLoadListeners) {
                mapLoadListener.onMapFailed(MapFailure.NETWORK_CONNECTION_FAILURE);
            }

        // Devices with an inaccurate date/time will not be able to load the mapbox map
        // If the "automatic date/time" is disabled on the device and the map fails to load, recommend the user enable it
        } else if (Settings.Global.getInt(getContext().getContentResolver(), Settings.Global.AUTO_TIME, 0) == 0) {
            for (OnMapLoadListener mapLoadListener : mapLoadListeners) {
                mapLoadListener.onMapFailed(MapFailure.INACCURATE_DATE_TIME_FAILURE);
            }

        // check connection by requesting the styles json directly (async)
        } else {
            mapStyleController.checkConnection(new AirMapCallback<Void>() {
                @Override
                protected void onSuccess(Void response) {
                    for (OnMapLoadListener mapLoadListener : mapLoadListeners) {
                        mapLoadListener.onMapFailed(MapFailure.UNKNOWN_FAILURE);
                    }

                    String logs = Utils.getMapboxLogs();
                    Analytics.report(new Exception("Mapbox map failed to load due to no network connection but able to access styles directly: " + logs));
                }

                @Override
                protected void onError(AirMapException e) {
                    for (OnMapLoadListener mapLoadListener : mapLoadListeners) {
                        mapLoadListener.onMapFailed(MapFailure.NETWORK_CONNECTION_FAILURE);
                    }
                }
            });
        }
    }

    @Override
    public void onDidFinishRenderingMap(boolean fully) {
        if (mapDataController != null) {
            mapDataController.onMapFinishedRendering();
        }
    }

    @Override
    public boolean onMapClick(@NonNull LatLng point) {
        if (advisoryClickListeners == null || advisoryClickListeners.isEmpty() || advisorySelector.isBusy()) {
            return false;
        }

        advisorySelector.selectAdvisoriesAt(point, this, (featureClicked, advisoryClicked, advisoriesSelected) -> {
            if (featureClicked != null) {
                for (OnAdvisoryClickListener advisoryClickListener : advisoryClickListeners) {
                    advisoryClickListener.onAdvisoryClicked(advisoryClicked, new ArrayList<>(advisoriesSelected));
                }
                // draw yellow outline & zoom
                mapStyleController.highlight(featureClicked, advisoryClicked);
                zoomToFeatureIfNecessary(featureClicked);
            }
        });

        return true;
    }

    List<AirMapAdvisory> getCurrentAdvisories() {
        return mapDataController.getCurrentAdvisories();
    }

    private void zoomToFeatureIfNecessary(Feature featureClicked) {
        LatLngBounds cameraBounds = getMap().getProjection().getVisibleRegion().latLngBounds;
        LatLngBounds.Builder advisoryLatLngsBuilder = new LatLngBounds.Builder();
        boolean zoom = false;

        Geometry geometry = featureClicked.geometry();
        List<Point> points = new ArrayList<>();
        if (geometry instanceof Polygon) {
            points.addAll(((Polygon) geometry).outer().coordinates());
        } else if (geometry instanceof MultiPolygon) {
            List<Polygon> polygons = ((MultiPolygon) geometry).polygons();
            for (Polygon polygon : polygons) {
                points.addAll(polygon.outer().coordinates());
            }
        }

        for (Point position : points) {
            LatLng latLng = new LatLng(position.latitude(), position.longitude());
            advisoryLatLngsBuilder.include(latLng);
            if (!cameraBounds.contains(latLng)) {
                Timber.d("Camera position doesn't contain point");
                zoom = true;
            }
        }

        if (zoom) {
            int padding = Utils.dpToPixels(getContext(), 72).intValue();
            getMap().moveCamera(CameraUpdateFactory.newLatLngBounds(advisoryLatLngsBuilder.build(), padding));
        }
    }

    public void highlight(AirMapAdvisory advisory) {
        RectF mapRectF = new RectF(getLeft(), getTop(), getRight(), getBottom());
        Expression filter = Expression.has("id");
        List<Feature> selectedFeatures = getMap().queryRenderedFeatures(mapRectF, filter);

        for (Feature feature : selectedFeatures) {
            if (advisory.getId().equals(feature.getStringProperty("id"))) {
                mapStyleController.highlight(feature, advisory);
                zoomToFeatureIfNecessary(feature);
                break;
            }
        }
    }

    public void unhighlight() {
        mapStyleController.unhighlight();
    }

    @Override
    public void onRulesetsUpdated(List<AirMapRuleset> availableRulesets, List<AirMapRuleset> selectedRulesets, List<AirMapRuleset> previouslySelectedRulesets) {
        Timber.i("onRulesetsUpdated to: %s from: %s", selectedRulesets, previouslySelectedRulesets);

        setLayers(selectedRulesets, previouslySelectedRulesets);

        for (OnMapDataChangeListener mapDataChangeListener : mapDataChangeListeners) {
            mapDataChangeListener.onRulesetsChanged(availableRulesets, selectedRulesets);
        }
    }

    @Override
    public void onAdvisoryStatusUpdated(AirMapAirspaceStatus advisoryStatus) {
        for (OnMapDataChangeListener mapDataChangeListener : new ArrayList<>(mapDataChangeListeners)) {
            mapDataChangeListener.onAdvisoryStatusChanged(advisoryStatus);
        }
    }

    @Override
    public void onAdvisoryStatusLoading() {
        for (OnMapDataChangeListener mapDataChangeListener : mapDataChangeListeners) {
            mapDataChangeListener.onAdvisoryStatusLoading();
        }
    }

    @Override
    public void onAdvisoryStatusError(MapFailure mapFailure) {
        for (OnMapDataChangeListener mapDataChangeListener : mapDataChangeListeners) {
            mapDataChangeListener.onAdvisoryStatusError(mapFailure);
        }
    }

    @Override
    public void onUnsupportedJurisdictions(ArrayList<AirMapJurisdiction> unsupportedJurisdictions) {
        for(OnMapDataChangeListener mapDataChangeListener : mapDataChangeListeners) {
            mapDataChangeListener.onUnsupportedJurisdictions(unsupportedJurisdictions);
        }
    }


    private void setLayers(List<AirMapRuleset> newRulesets, List<AirMapRuleset> oldRulesets) {
        if (oldRulesets != null) {
            for (AirMapRuleset oldRuleset : oldRulesets) {
                if (!newRulesets.contains(oldRuleset)) {
                    mapStyleController.removeMapLayers(oldRuleset.getId(), oldRuleset.getLayers());
                }
            }
        }


        for (AirMapRuleset newRuleset : newRulesets) {
            if (oldRulesets == null || !oldRulesets.contains(newRuleset)) {
                mapStyleController.addMapLayers(newRuleset, useSIMeasurements);
            }
        }
    }

    public void hideInactiveAirspaces(){
        mapStyleController.hideInactiveAirspace();
    }

    @UiThread
    public MapboxMap getMap() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            Timber.e("*** AirMapMapView accessed from a thread other than the UI-thread:" + Thread.currentThread());
            Analytics.report(new Exception("AirMapMapView accessed from a thread other than the UI-thread: " + Thread.currentThread()));
        }
        return map;
    }

    public List<AirMapRuleset> getSelectedRulesets() {
        return mapDataController.getSelectedRulesets();
    }

    public List<AirMapRuleset> getAvailableRulesets(){
        return mapDataController.getAvailableRulesets();
    }
    
    public void setTemporalFilter(TemporalFilter temporalFilter){
        if(isMapLoaded){
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mapStyleController.setTemporalFilter(temporalFilter);
                    setMapDataController(new MapDataController(AirMapMapView.this, mapDataController.getConfiguration(), temporalFilter));
                }
            }, 1000);
        } else {
            Timber.wtf("Should not be calling setTemporalFilter before map is loaded");
            throw new RuntimeException("please call setTemporalFilter after onMapLoaded callback");
        }
    }

    public void raiseError(MapFailure mapFailure){
        for (OnMapLoadListener mapLoadListener : mapLoadListeners) {
            mapLoadListener.onMapFailed(mapFailure);
        }
    }

    public void disableAdvisories() {
        mapDataController.disableAdvisories();
    }

    public void setMapStyleControllerTemporalFilter(TemporalFilter temporalFilter){
        mapStyleController.setTemporalFilter(temporalFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mapDataController.onDestroy();
    }

    // callbacks
    public void addOnMapLoadListener(OnMapLoadListener listener) {
        if (!mapLoadListeners.contains(listener)) {
            mapLoadListeners.add(listener);
        }

        if (getMap() != null) {
            isMapLoaded = true;
            listener.onMapLoaded();
        }
    }

    public void removeOnMapLoadListener(OnMapLoadListener listener) {
        mapLoadListeners.remove(listener);
    }

    public void addOnMapDataChangedListener(OnMapDataChangeListener listener) {
        mapDataChangeListeners.add(listener);

        if (getMap() != null) {
            listener.onRulesetsChanged(mapDataController.getAvailableRulesets(), mapDataController.getSelectedRulesets());

            if(mapDataController.getAirspaceStatus() != null){
                listener.onAdvisoryStatusChanged(mapDataController.getAirspaceStatus());
            } else {
                listener.onAdvisoryStatusError(MapFailure.ADVISORY_STATUS_NULL);
            }
        }
    }

    public void removeOnMapDataChangedListener(OnMapDataChangeListener listener) {
        mapDataChangeListeners.remove(listener);
    }

    public void addOnAdvisoryClickListener(OnAdvisoryClickListener listener) {
        advisoryClickListeners.add(listener);
    }

    public void removeOnAdvisoryClickListener(OnAdvisoryClickListener listener) {
        advisoryClickListeners.remove(listener);
    }

    public void addAirspaceTypeListener(AirspaceTypeListener airspaceTypeListener) {
        mapStyleController.setAirspaceTypeListener(airspaceTypeListener);
    }

    public void removeAirspaceTypeListener() {
        mapStyleController.setAirspaceTypeListener(null);
    }

    public interface OnMapLoadListener {
        void onMapLoaded();
        default void onMapFailed(MapFailure reason) {}
    }

    public interface OnMapDataChangeListener {
        void onRulesetsChanged(List<AirMapRuleset> availableRulesets, List<AirMapRuleset> selectedRulesets);
        void onAdvisoryStatusChanged(AirMapAirspaceStatus status);
        void onAdvisoryStatusLoading();
        void onAdvisoryStatusError(MapFailure mapFailure);
        void onUnsupportedJurisdictions(ArrayList<AirMapJurisdiction> unsupportedJurisdictions);
    }

    public interface OnAdvisoryClickListener {
        void onAdvisoryClicked(@Nullable AirMapAdvisory advisoryClicked, @Nullable List<AirMapAdvisory> advisoriesFiltered);
    }

    public interface AirspaceTypeListener {
        void onAirspaceTypeAdded(AirMapRuleset ruleset, MappingService.AirMapAirspaceType type, Layer layer);
        void onAirspaceTypeRemoved(MappingService.AirMapAirspaceType type);
    }

    public enum MapFailure {
        INACCURATE_DATE_TIME_FAILURE, NETWORK_CONNECTION_FAILURE, UNKNOWN_FAILURE, REQUEST_RETURNED_5XX, REQUEST_RETURNED_4XX, ADVISORY_STATUS_NULL
    }

    public abstract static class Configuration {
        public enum Type {
            AUTOMATIC,
            DYNAMIC,
            MANUAL
        }

        public final Type type;

        public Configuration(Type type) {
            this.type = type;
        }
    }

    public static class AutomaticConfiguration extends Configuration {

        public AutomaticConfiguration() {
            super(Type.AUTOMATIC);
        }
    }

    public static class DynamicConfiguration extends Configuration {

        public final Set<String> preferredRulesetIds;
        public final Set<String> unpreferredRulesetIds;
        public final boolean enableRecommendedRulesets;

        public DynamicConfiguration(@Nullable Set<String> preferredRulesetIds, @Nullable Set<String> unpreferredRulesetIds, boolean enableRecommendedRulesets) {
            super(Type.DYNAMIC);

            this.preferredRulesetIds = preferredRulesetIds != null ? preferredRulesetIds : new HashSet<String>();
            this.unpreferredRulesetIds = unpreferredRulesetIds != null ? unpreferredRulesetIds : new HashSet<String>();
            this.enableRecommendedRulesets = enableRecommendedRulesets;
        }
    }

    public static class ManualConfiguration extends Configuration {

        public final List<AirMapRuleset> selectedRulesets;

        public ManualConfiguration(List<AirMapRuleset> selectedRulesets) {
            super(Type.MANUAL);

            this.selectedRulesets = selectedRulesets;
        }
    }

    public static abstract class DragListener implements View.OnTouchListener {

        private boolean isDragging;
        private LatLng originLatLng;

        public abstract void onDrag(PointF toPointF, LatLng toLatLng, LatLng fromLatLng);

        public abstract void onFinishedDragging(PointF point, LatLng toLatLng, LatLng fromLatLng);

        @Override
        public final boolean onTouch(View v, MotionEvent event) {
            if (event != null) {
                if (event.getPointerCount() > 1) {
                    //Don't drag if there are multiple fingers on screen
                    return false;
                }
                float screenDensity = v.getContext().getResources().getDisplayMetrics().density;
                PointF tapPoint = new PointF(event.getX(), event.getY());
                float toleranceSides = 8 * screenDensity;
                float toleranceTopBottom = 8 * screenDensity;
                float averageIconWidth = 32 * screenDensity;
                float averageIconHeight = 32 * screenDensity;
                RectF tapRect = new RectF(tapPoint.x - averageIconWidth / 2 - toleranceSides,
                        tapPoint.y - averageIconHeight / 2 - toleranceTopBottom,
                        tapPoint.x + averageIconWidth / 2 + toleranceSides,
                        tapPoint.y + averageIconHeight / 2 + toleranceTopBottom);

                AirMapMapView mapView = (AirMapMapView) v;
                MapboxMap map = mapView.getMap();

                List<Feature> features = map.queryRenderedFeatures(tapRect, Container.POINT_LAYER, Container.MIDPOINT_LAYER);
                if (!features.isEmpty() || isDragging) {
                    boolean doneDragging = event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL;
                    map.getUiSettings().setScrollGesturesEnabled(doneDragging);
                    map.getUiSettings().setZoomGesturesEnabled(doneDragging);

                    if (doneDragging) {
                        // only finish drag if started with down on marker
                        if (isDragging) {
                            LatLng latLng = map.getProjection().fromScreenLocation(tapPoint);
                            onFinishedDragging(tapPoint, latLng, originLatLng);
                            originLatLng = null;
                            isDragging = false;
                        } else {
                            map.getUiSettings().setScrollGesturesEnabled(true);
                            map.getUiSettings().setZoomGesturesEnabled(true);
                            return false;
                        }
                    } else {
                        // starts with down press on marker
                        LatLng latLng = map.getProjection().fromScreenLocation(tapPoint);
                        if (event.getAction() == MotionEvent.ACTION_DOWN && originLatLng == null) {
                            originLatLng = latLng;
                            isDragging = true;
                        }

                        // only drag if started with down on marker
                        if (isDragging) {
                            onDrag(tapPoint, latLng, originLatLng);
                        } else {
                            map.getUiSettings().setScrollGesturesEnabled(true);
                            map.getUiSettings().setZoomGesturesEnabled(true);
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }
    }
}
