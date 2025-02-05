package at.alwinschuster.HttpServer;

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
    private static final String TILE_FILE_NAME = "tiles.mbtiles";
    private static final String STYLES_FILE_NAME = "styles.json";
    private static int port;
    private static Server server = null;
    private static File tilesFile = null; // Variable to store the tile file
    private String styleJson;  // Variable to store the styleJson

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
    public void styleJson(String style) {       
        if (server != null) {
            styleJson = style;  // Store the style JSON in the module
            server.setStyleJson(styleJson);
            Log.d(MODULE_NAME, "Style JSON received ");
        } else {
            Log.e(MODULE_NAME, "Server is not initialized yet");
        } 
    }

    @ReactMethod
    public void storagePath(String localStoragePath) {
        if (server != null) {
            Log.d(MODULE_NAME, "Map local storage path received: " + localStoragePath);
            tilesFile = new File(localStoragePath + TILE_FILE_NAME); // Store the file path to use later

            if (tilesFile.exists()) {
                server.setTilesFile(tilesFile);  // Set the tile file in the server
                Log.d(MODULE_NAME, "MBTiles file exists: " + tilesFile.getAbsolutePath());
            } else {
                Log.e(MODULE_NAME, "MBTiles file does not exist.");
            }    
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
