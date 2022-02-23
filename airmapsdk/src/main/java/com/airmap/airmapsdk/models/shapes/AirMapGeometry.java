package com.airmap.airmapsdk.models.shapes;

import com.airmap.airmapsdk.models.Coordinate;
import com.mapbox.mapboxsdk.geometry.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.airmap.airmapsdk.util.Utils.optString;

@SuppressWarnings("unused")
public abstract class AirMapGeometry implements Serializable {
    public static AirMapGeometry getGeometryFromGeoJSON(JSONObject geoJson) {
        if (geoJson != null) {
            String geoType = optString(geoJson, "type").toLowerCase();
            if (geoType.equals("polygon")) {
                List<Coordinate> coordinates = new ArrayList<>();
                //if object is a polygon with a hole

                JSONArray coordArray = geoJson.optJSONArray("coordinates");
                if (coordArray == null) return null;
                int lengthCoordArray = coordArray.length();
                for (int i = 0; i < lengthCoordArray; i++){
                    for (int j = 0; j < coordArray.optJSONArray(i).length(); j++) {
                        JSONArray coordinate = coordArray.optJSONArray(i).optJSONArray(j);
                        if (coordinate == null) continue;
                        LatLng temp = new LatLng(coordinate.optDouble(1), coordinate.optDouble(0)); //Comes in in lng,lat form
                        coordinates.add(new Coordinate(temp)); //This wraps the coordinate
                    }
                }

                return new AirMapPolygon(coordinates);
            } else if (geoType.equals("linestring")) {
                List<Coordinate> coordinates = new ArrayList<>();
                JSONArray coordArray = geoJson.optJSONArray("coordinates");
                if (coordArray == null) return null;
                for (int i = 0; i < coordArray.length(); i++) {
                    JSONArray coordinate = coordArray.optJSONArray(i);
                    if (coordinate == null) continue;
                    LatLng temp = new LatLng(coordinate.optDouble(1), coordinate.optDouble(0)); //Comes in in lng,lat form
                    coordinates.add(new Coordinate(temp)); //This wraps the coordinate
                }
                return new AirMapPath(coordinates);
            } else if (geoType.equals("point")) {
                JSONArray coordinate = geoJson.optJSONArray("coordinates");
                if (coordinate == null) return null;
                LatLng temp = new LatLng(coordinate.optDouble(1), coordinate.optDouble(0)); //Comes in in lng,lat form
                return new AirMapPoint(new Coordinate(temp));
            }
        }
        return null;
    }

    public static JSONObject getGeoJSONFromGeometry(AirMapGeometry geometry) {
        JSONObject jsonObject = new JSONObject();
        try {
            if (geometry instanceof AirMapPolygon) {
                jsonObject.put("type", "Polygon");
                JSONArray coordinates = new JSONArray();
                List<Coordinate> polyCoords = ((AirMapPolygon) geometry).getCoordinates();
                Coordinate firstCoordinate = polyCoords.get(0);
                Coordinate lastCoordinate = polyCoords.get(polyCoords.size() - 1);
                //If polyHasHole there will be 2 polys in coords. Polys wrap back to their starting point. If 1st point is in coords again and not at the end poly has a hole / 2nd poly or has not been closed properly (handled in onGeometryUpdated).
                Boolean polyHasHole = ((polyCoords.lastIndexOf(firstCoordinate) > 0) && !(firstCoordinate.equals(lastCoordinate)));

                if (!polyHasHole) {
                    JSONArray polygon = new JSONArray();
                    for (Coordinate coordinate : polyCoords) {
                        JSONArray point = new JSONArray();
                        point.put(coordinate.getLongitude());
                        point.put(coordinate.getLatitude());
                        polygon.put(point);
                    }
                    coordinates.put(polygon);
                    jsonObject.put("coordinates", coordinates);
                } else if (polyHasHole){
                    //If poly has hole put each geo into polygon, then into JSONArray, then into json object.
                    JSONArray jsonPolyArray = new JSONArray();

                    if (polyHasHole){
                        boolean morePolysToCount = true;
                        do {
                            int lastOccurance = polyCoords.lastIndexOf(polyCoords.get(0));
                            if ( lastOccurance > 0){
                                List<Coordinate> sublist = polyCoords.subList(0, lastOccurance+1);
                                JSONArray polygon = new JSONArray();
                                for (Coordinate coordinate : ((AirMapPolygon) new AirMapPolygon(sublist)).getCoordinates()) {
                                    JSONArray point = new JSONArray();
                                    point.put(coordinate.getLongitude());
                                    point.put(coordinate.getLatitude());
                                    polygon.put(point);
                                }
                                jsonPolyArray.put(polygon);
                                polyCoords = polyCoords.subList(lastOccurance+1, polyCoords.size());
                                if (polyCoords.size() == 0) {
                                    morePolysToCount = false;
                                }
                            }
                        } while (morePolysToCount);
                        jsonObject.put("type", "Polygon");
                        jsonObject.put("coordinates",jsonPolyArray);
                    }
                }

            } else if (geometry instanceof AirMapPoint) {
                jsonObject.put("type", "Point");

                JSONArray coordinates = new JSONArray();
                coordinates.put(((AirMapPoint) geometry).getCoordinate().getLongitude());
                coordinates.put(((AirMapPoint) geometry).getCoordinate().getLatitude());

                jsonObject.put("coordinates", coordinates);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public static AirMapPolygon convertPointToPolygon(AirMapPoint point, float radius) {
        ArrayList<Coordinate> coordinates = new ArrayList<>(); //array to hold all the points

        int degreesBetweenPoints = 8;
        int numberOfPoints = (int) Math.floor(360 / degreesBetweenPoints);
        double distRadians = radius / 6371000.0; // earth radius in meters
        double centerLatRadians = point.getCoordinate().getLatitude() * Math.PI / 180;
        double centerLonRadians = point.getCoordinate().getLongitude() * Math.PI / 180;
        for (int index = 0; index < numberOfPoints; index++) {
            double degrees = index * degreesBetweenPoints;
            double degreeRadians = degrees * Math.PI / 180;
            double pointLatRadians = Math.asin(Math.sin(centerLatRadians) * Math.cos(distRadians) + Math.cos(centerLatRadians) * Math.sin(distRadians) * Math.cos(degreeRadians));
            double pointLonRadians = centerLonRadians + Math.atan2(Math.sin(degreeRadians) * Math.sin(distRadians) * Math.cos(centerLatRadians),
                    Math.cos(distRadians) - Math.sin(centerLatRadians) * Math.sin(pointLatRadians));
            double pointLat = pointLatRadians * 180 / Math.PI;
            double pointLon = pointLonRadians * 180 / Math.PI;

            coordinates.add(new Coordinate(pointLat, pointLon));
        }

        // add first coordinate to end to complete polygon
        coordinates.add(coordinates.get(0));

        return new AirMapPolygon(coordinates);
    }
}
