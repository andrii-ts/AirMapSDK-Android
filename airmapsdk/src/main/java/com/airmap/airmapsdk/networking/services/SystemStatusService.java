package com.airmap.airmapsdk.networking.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.airmap.airmapsdk.models.SystemStatusComponent;
import com.airmap.airmapsdk.networking.callbacks.AirMapSystemStatusListener;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.neovisionaries.ws.client.ThreadType;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketState;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import okhttp3.internal.http.HttpHeaders;
import timber.log.Timber;

public class SystemStatusService extends BaseService {

    private enum ConnectionState {
        Connecting, Connected, Disconnected
    }

    private WebSocketFactory webSocketFactory;
    private WebSocket webSocket;
    private List<AirMapSystemStatusListener> listeners;
    private HashMap<String, SystemStatusComponent> components;
    private List<SystemStatusComponent> notNormalComponents;
    private Gson gson;
    private ConnectionState connectionState;
    private Handler handler;

    /**
     * Initialize an SystemStatusService to receive system health updates
     *
     * @param context An Android Context
     */
    public SystemStatusService(Context context){
        webSocketFactory = new WebSocketFactory();
        try {
            webSocket = webSocketFactory.createSocket(systemStatusSocketUrl, 5000);
        } catch (IOException e) {
            Timber.e(e);
            e.printStackTrace();
        }
        webSocket.addListener(new AirMapWebSocketAdapter());
        webSocket.addHeader("x-api-key", AirMap.getApiKey());
        webSocket.setUserInfo(AirMap.getAuthToken());
        listeners = new ArrayList<>();
        components = new HashMap<>();
        notNormalComponents = new ArrayList<>();
        gson = new Gson();
        handler = new Handler(Looper.getMainLooper());
    }

    public void connect(){
        try {
            webSocket = webSocket.recreate(5000);
        } catch (IOException e) {
            Timber.e(e);
        }
        webSocket.connectAsynchronously();
    }

    public void disconnect(){
        webSocket.disconnect();
    }

    public void receivedStatusUpdate(JsonObject update){
        String overallSystemHealth = update.get("level").getAsString();
        if(!overallSystemHealth.equalsIgnoreCase("normal")){
            notifyOverallSystemHealth(overallSystemHealth);
            return;
        }
        notNormalComponents.clear();

        JsonArray jsonComponents = update.getAsJsonArray("components");

        //add or update components
        for (JsonElement component : jsonComponents){
            SystemStatusComponent systemStatusComponent = gson.fromJson(component, SystemStatusComponent.class);
            if(components.containsKey(systemStatusComponent.getId())){
                components.get(systemStatusComponent.getId()).setLevel(systemStatusComponent.getLevel());
                components.get(systemStatusComponent.getId()).setUpdated_at(systemStatusComponent.getUpdated_at());
                components.get(systemStatusComponent.getId()).setName(systemStatusComponent.getName());
            } else {
                components.put(systemStatusComponent.getId(), systemStatusComponent);
            }

            if(!components.get(systemStatusComponent.getId()).getLevel().equals("normal")) {
                notNormalComponents.add(components.get(systemStatusComponent.getId()));
            }
        }
        notifyOverallSystemHealth(overallSystemHealth);
        notifyBrokenComponents();
    }

    public void onSocketConnected(){
        Timber.i("Socket Connected");
        notifySockedConnected();
    }

    public void onSockedDisconnected(){
        Timber.i("Disconnected");
        components.clear();
        notNormalComponents.clear();
        notifySockedDisconnected();
    }

    public void notifySockedConnected(){
        handler.post(() -> {
            for(AirMapSystemStatusListener listener : listeners){
                if(listener != null){
                    listener.onConnected();
                }
            }
        });
    }

    public void notifySockedDisconnected(){
        handler.post(new Runnable() {
            @Override
            public void run() {
                for(AirMapSystemStatusListener listener : listeners){
                    if(listener != null){
                        listener.onDisconnected();
                    }
                }
            }
        });
    }

    public void notifyComponentHealth(String component, String healthLevel){
        handler.post(() -> {
            for(AirMapSystemStatusListener listener : listeners){
                if(listener != null){
                    listener.onMessage(component, healthLevel);
                }
            }
        });
    }

    public void notifyOverallSystemHealth(String healthLevel){
        notifyComponentHealth("Overall System", healthLevel);
    }

    public void notifyBrokenComponents(){
        for(SystemStatusComponent component : notNormalComponents){
            Timber.i(component.toString());
            //notifyComponentHealth(component.getName(), component.getLevel());
        }

    }

    public boolean isConnected(){
        return true;
    }

    /**
     * Add a listener to be notified of system health updates
     *
     * @param listener An AirMapSystemStatusListener which will be notified when there is a health update
     */
    public void addListener(AirMapSystemStatusListener listener){
        if(listeners.isEmpty()){
            listeners.add(listener);
            connect();
        } else if(!listeners.contains(listener)){
            listeners.add(listener);
        }
    }

    /**
     * Clear all the listeners
     */
    public void removeAllListeners() {
        listeners.clear();
    }

    protected void setAuthToken(String auth){
        webSocket.setUserInfo(auth);
    }

    private class AirMapWebSocketAdapter extends WebSocketAdapter {

        public AirMapWebSocketAdapter() {
            super();
        }

        @Override
        public void onStateChanged(WebSocket websocket, WebSocketState newState) throws Exception {
            super.onStateChanged(websocket, newState);
            Timber.i("New Websocket State: %s", newState.name());
        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            super.onConnected(websocket, headers);
            Timber.i("Headers: %s", headers.toString());
            onSocketConnected();
        }

        @Override
        public void onConnectError(WebSocket websocket, WebSocketException exception) throws Exception {
            super.onConnectError(websocket, exception);
            Timber.e(exception);
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
            onSockedDisconnected();
        }

        @Override
        public void onFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            super.onFrame(websocket, frame);
        }

        @Override
        public void onContinuationFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            super.onContinuationFrame(websocket, frame);
        }

        @Override
        public void onTextFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            super.onTextFrame(websocket, frame);
        }

        @Override
        public void onBinaryFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            super.onBinaryFrame(websocket, frame);
        }

        @Override
        public void onCloseFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            super.onCloseFrame(websocket, frame);
        }

        @Override
        public void onPingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            super.onPingFrame(websocket, frame);
        }

        @Override
        public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            super.onPongFrame(websocket, frame);
        }

        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            super.onTextMessage(websocket, text);
            receivedStatusUpdate(new JsonParser().parse(text).getAsJsonObject());
        }

        @Override
        public void onTextMessage(WebSocket websocket, byte[] data) throws Exception {
            super.onTextMessage(websocket, data);
        }

        @Override
        public void onBinaryMessage(WebSocket websocket, byte[] binary) throws Exception {
            super.onBinaryMessage(websocket, binary);
        }

        @Override
        public void onSendingFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
            super.onSendingFrame(websocket, frame);
        }

        @Override
        public void onFrameSent(WebSocket websocket, WebSocketFrame frame) throws Exception {
            super.onFrameSent(websocket, frame);
        }

        @Override
        public void onFrameUnsent(WebSocket websocket, WebSocketFrame frame) throws Exception {
            super.onFrameUnsent(websocket, frame);
        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
            super.onError(websocket, cause);
            Timber.e(cause);
        }

        @Override
        public void onFrameError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
            super.onFrameError(websocket, cause, frame);
        }

        @Override
        public void onMessageError(WebSocket websocket, WebSocketException cause, List<WebSocketFrame> frames) throws Exception {
            super.onMessageError(websocket, cause, frames);
        }

        @Override
        public void onMessageDecompressionError(WebSocket websocket, WebSocketException cause, byte[] compressed) throws Exception {
            super.onMessageDecompressionError(websocket, cause, compressed);
        }

        @Override
        public void onTextMessageError(WebSocket websocket, WebSocketException cause, byte[] data) throws Exception {
            super.onTextMessageError(websocket, cause, data);
        }

        @Override
        public void onSendError(WebSocket websocket, WebSocketException cause, WebSocketFrame frame) throws Exception {
            super.onSendError(websocket, cause, frame);
        }

        @Override
        public void onUnexpectedError(WebSocket websocket, WebSocketException cause) throws Exception {
            super.onUnexpectedError(websocket, cause);
        }

        @Override
        public void handleCallbackError(WebSocket websocket, Throwable cause) throws Exception {
            super.handleCallbackError(websocket, cause);
        }

        @Override
        public void onSendingHandshake(WebSocket websocket, String requestLine, List<String[]> headers) throws Exception {
            super.onSendingHandshake(websocket, requestLine, headers);
        }

        @Override
        public void onThreadCreated(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {
            super.onThreadCreated(websocket, threadType, thread);
        }

        @Override
        public void onThreadStarted(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {
            super.onThreadStarted(websocket, threadType, thread);
        }

        @Override
        public void onThreadStopping(WebSocket websocket, ThreadType threadType, Thread thread) throws Exception {
            super.onThreadStopping(websocket, threadType, thread);
        }
    }
}
