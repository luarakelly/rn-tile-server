package at.LuaraSilva.OkhttpInterceptor;

import android.content.Context;
import androidx.annotation.NonNull;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.Request;
import okhttp3.Interceptor;
import okhttp3.Response;

import org.maplibre.android.module.http.HttpRequestUtil;

import java.io.IOException;

// import org.maplibre.gl.module.http.HttpRequestImpl; // MapLibre's networking module // got a problem with this import

public class OkhttpInterceptorReactModule extends ReactContextBaseJavaModule {
    private static OkHttpClient client = null;
    /* save context as a variable */

    public OkhttpInterceptorReactModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @NonNull
    @Override
    public String getName() {
        return "OkhttpInterceptor";
    }

    @ReactMethod
    public void initializeInterceptor(String folderName, String tilesFileName, Promise promise) {
        try {
            ReactApplicationContext context = getReactApplicationContext();

            // ✅ Validate .mbtiles before setting interceptor
            boolean isValid = isValidMbtilesFile(context, folderName + "/" + tilesFileName + ".mbtiles");
            if (!isValid) {
                promise.reject("INVALID_FILE", "MBTiles file is invalid or missing required 'tiles' table.");
                return;
            }

            if (client == null) {
                client = new OkHttpClient.Builder()
                    .addInterceptor(new OkhttpInterceptor(context, folderName, tilesFileName))
                    .build();

                HttpRequestUtil.setOkHttpClient(client);
            }

            promise.resolve("Interceptor initialized successfully");
        } catch (Exception e) {
            promise.reject("INIT_ERROR", "Failed to initialize interceptor", e);
        }
    }

    @ReactMethod
    public void isValidMbtilesFile(String folderName, String tilesFileName, Promise promise) {
        try {
            boolean isValid = isValidMbtilesFile(getReactApplicationContext(), folderName + "/" + tilesFileName + ".mbtiles");
            promise.resolve(isValid);
        } catch (Exception e) {
            promise.reject("VALIDATION_ERROR", "Failed to validate MBTiles file", e);
        }
    }

    @ReactMethod
    public void cleanupInterceptor(Promise promise) {
        try {
            HttpRequestUtil.setOkHttpClient(null); // Reset to default client

            OkhttpInterceptor.clearTileCache(); // Clean tile cache
            OkhttpInterceptor.shutdownExecutor(); // Shut down executor

            MBTilesDatabaseHelper.closeAll(); // ✅ Close all DBs

            promise.resolve("Interceptor cleaned up successfully");
        } catch (Exception e) {
            promise.reject("CLEANUP_ERROR", "Failed to clean up interceptor", e);
        }
    }
    
    public static OkHttpClient getHttpClient() {
        return client;
    }

    private boolean isValidMbtilesFile(Context context, String fullRelativePath) {
        SQLiteDatabase db = null;
        Cursor cursorTable = null;
        Cursor cursorData = null;
    
        try {
            File file = new File(context.getFilesDir(), fullRelativePath);
            if (!file.exists()) {
                Log.e("Validation", "MBTiles file does not exist: " + file.getAbsolutePath());
                return false;
            }
    
            if (file.length() < 1024) {
                Log.e("Validation", "MBTiles file is too small to be valid: " + file.getAbsolutePath());
                return false;
            }
    
            db = SQLiteDatabase.openDatabase(file.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
    
            cursorTable = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='tiles'", null);
            boolean hasTilesTable = cursorTable.moveToFirst();
            if (!hasTilesTable) {
                Log.e("Validation", "Tiles table not exists");
            }
    
            cursorData = db.rawQuery("SELECT tile_data FROM tiles LIMIT 1", null);
            boolean hasTilesData = cursorData.moveToFirst();
            if (!hasTilesData) {
                Log.e("Validation", "Tiles table exists but no data");
            }
    
            if (hasTilesTable && hasTilesData) {
                Log.d("Validation", "MBTiles file is valid: " + file.getAbsolutePath());
                return true;
            } else {
                Log.d("Validation", "MBTiles file is invalid: " + file.getAbsolutePath());
                return false;
            }
    
        } catch (Exception e) {
            Log.e("Validation", "Error during MBTiles validation", e);
            return false;
        } finally {
            if (cursorTable != null) cursorTable.close();
            if (cursorData != null) cursorData.close();
            if (db != null && db.isOpen()) db.close();
        }
    }    
}

/**
✅ Everything else in your code (package, initialization, interceptor design) is solid.
You're using a singleton OkHttpClient ✔️

You're passing context correctly ✔️

You're using HttpRequestUtil.setOkHttpClient to inject into MapLibre ✔️

Your .mbtiles LRU cache is isolated and efficient ✔️
 */

/*
@ReactMethod
    // // Set the MapView reference from JavaScript to inject the OkHttpClient later
    public void setMapView(MapView mapViewInstance) {
        this.mapView = mapViewInstance;
    }
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

     /**
      * Interceptor Configuration: You're creating a custom OkHttpInterceptor to intercept HTTP requests, which is great! However, MapLibre doesn't directly expose an API to set a custom OkHttpClient globally (at least not in the public API). So, you need to find a way to configure MapLibre’s networking to use your custom OkHttpClient.

Interceptor on Tile Requests: Your interceptor works well in intercepting .pbf tile requests and checking for data in your cache or database. This part is correct.

Global OkHttpClient Usage: Since MapLibre doesn’t have direct support to inject the OkHttpClient into its networking stack, the goal is to apply your interceptor globally for all network calls. This could mean setting up a global HTTP client that’s used by all the libraries in your app (including MapLibre).

MapLibre Tile Fetching: As of now, MapLibre doesn't directly expose a way to inject an OkHttpClient into its MapView for fetching tiles. But we can configure network requests globally in Android.
      */