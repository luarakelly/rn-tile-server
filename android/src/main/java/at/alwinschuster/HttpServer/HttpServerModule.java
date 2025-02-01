package at.alwinschuster.HttpServer;

import android.content.Context;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableArray;

import java.io.IOException;

import android.util.Log;

public class HttpServerModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
    ReactApplicationContext reactContext;

    private static final String MODULE_NAME = "HttpServer";

    private static int port;
    private static Server server = null;

    public HttpServerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        reactContext.addLifecycleEventListener(this); // Add lifecycle event listener
    }

    @Override
    public String getName() { // This is how the module will be referenced in JS
        return MODULE_NAME;
    }

    @ReactMethod
    public void start(int port, String bindAddress, String serviceName, Callback callback) {
        Log.d(MODULE_NAME, "Initializing server...");
        this.port = port;

        startServer(bindAddress, callback);
    }

    @ReactMethod
    public void stop(Callback callback) {
        Log.d(MODULE_NAME, "Stopping server...");
        // Graceful shutdown process
        stopServer(callback);
    }

    @ReactMethod
    public void addListener(String eventName) {
        // Set up any upstream listeners or background tasks as necessary
    }

    @ReactMethod
    public void removeListeners(Integer count) {
        // Remove upstream listeners, stop unnecessary background tasks
    }

    @Override
    public void onHostResume() {
        // Handle app resume if necessary
    }

    @Override
    public void onHostPause() {
        // Handle app pause if necessary
    }

    @Override
    public void onHostDestroy() {
        stopServer(new Callback() {
            @Override
            public void invoke(Object... args) {
                // Callback for cleanup (if any)
            }
        });
    }

    private void startServer(String bindAddress, Callback callback) {
        if (this.port == 0) {
            callback.invoke("Invalid port number", null);
            return;
        }

        if (server == null) {
            server = new Server(reactContext, port, bindAddress); // Initialize server with the port
        }

        try {
            server.start();
            callback.invoke(null, "Server started successfully"); // Callback success
        } catch (IOException e) {
            callback.invoke(e.getMessage(), null); // Callback failure with error message
        }
    }

    private void stopServer(Callback callback) {
        if (server != null) {
            server.stop(); // Graceful stop
            server = null; // Nullify server instance after stopping
            port = 0; // Reset port
            callback.invoke(null, "Server stopped successfully");
        } else {
            callback.invoke("Server is not running", null);
        }
    }
}


/**
 * // Method to start the server
    public void startServer() {
        if (!isRunning) {
            try {
                super.start();  // Start the NanoHTTPD server
                isRunning = true;
                Log.d(TAG, "Server started successfully");
            } catch (IOException e) {
                Log.e(TAG, "Error starting server: " + e.getMessage());
            }
        }
    }

    // Method to stop the server
    public void stopServer() {
        if (isRunning) {
            stop();  // Stop the server
            isRunning = false;
            Log.d(TAG, "Server stopped");
        }
    }
 */
