package at.LuaraSilva.OkhttpInterceptor;

import android.content.Context;
import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Interceptor;
import okhttp3.Response;

import java.io.IOException;

import org.maplibre.gl.module.http.HttpRequestImpl; // MapLibre's networking module

public class OkhttpInterceptorReactModule extends ReactContextBaseJavaModule {
    private static OkHttpClient client = null;

    public OkhttpInterceptorReactModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @NonNull
    @Override
    public String getName() {
        return "OkhttpInterceptor";
    }

    @ReactMethod
    public void initializeInterceptor(Promise promise) {
        try {
            if (client == null) {
                ReactApplicationContext context = getReactApplicationContext();
                if (context == null) {
                    promise.reject("INIT_ERROR", "React Context is null");
                    return;
                }

                client = new OkHttpClient.Builder()
                        .addInterceptor(new OkhttpInterceptor(context))
                        .build();
                
                // **Apply the custom OkHttpClient to MapLibre**
                HttpRequestImpl.setOkHttpClient(client);
            }
            promise.resolve("Interceptor initialized successfully");
        } catch (Exception e) {
            promise.reject("INIT_ERROR", "Failed to initialize interceptor", e);
        }
    }

    public static OkHttpClient getHttpClient() {
        return client;
    }
}
/*
//______________________________
import android.content.Context;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReadableArray;
import java.io.File;
import android.util.Log;

import java.io.IOException;

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
    public void styleJson(String styles) {       
        if (server != null) {
            server.setStyleJson(styles);
            Log.d(MODULE_NAME, "Style JSON received ");
        } else {
            Log.e(MODULE_NAME, "Server is not initialized yet");
        } 
    }
    
    @ReactMethod
    public void start(int port, String bindAddress, String serviceName, Callback callback) {
        Log.d(MODULE_NAME, "Initializing server...");
        this.port = port;

        startServer(bindAddress, callback);        
    }

    @ReactMethod
    public void stop() {
        Log.d(MODULE_NAME, "Stopping server...");
        // Graceful shutdown process
        stopServer();
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
        stopServer();
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

    private void stopServer() {
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
*/

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

 /**
     * private void downloadTiles() {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL("https://www.dropbox.com/scl/fi/7olc36bmmpks3fz6ftyoa/finland-shortbread-1.0.mbtiles?rlkey=1bh9yfcpaol5sruk3sy36x4ut&st=pyz2t7s0&dl=1");
                Files.copy(url.openStream(), Paths.get(TILE_FILE_NAME), StandardCopyOption.REPLACE_EXISTING);
                Log.d(TAG, "Tiles downloaded successfully.");
            } catch (Exception e) {
                Log.e(TAG, "Error downloading tiles: " + e.getMessage());
            } 
        }, executor);
    }

    private void downloadTiles(final Runnable onSuccess) {
        CompletableFuture.runAsync(() -> {
            try {
                // Download the tiles file asynchronously
                URL url = new URL("https://www.dropbox.com/scl/fi/7olc36bmmpks3fz6ftyoa/finland-shortbread-1.0.mbtiles?rlkey=1bh9yfcpaol5sruk3sy36x4ut&st=pyz2t7s0&dl=1");
                downloadedTilesFile = new File(getReactApplicationContext().getFilesDir(), "tiles.mbtiles");
                Files.copy(url.openStream(), downloadedTilesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Log.d(MODULE_NAME, "Tiles downloaded successfully.");

                // Invoke the onSuccess callback once the tiles are downloaded
                onSuccess.run();
            } catch (Exception e) {
                Log.e(MODULE_NAME, "Error downloading tiles: " + e.getMessage());
                // If there was an error, notify with a failure
                onSuccess.run();
            }
        });
    }
     */
