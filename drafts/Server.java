// modular version
package at.alwinschuster.HttpServer;

import fi.iki.elonen.NanoHTTPD;
import routes.Tile;
import routes.Styles;

public class Server {
    public static void main(String[] args) {
        // Start the server on port 8080
        int port = 8080;
        try {
            Tile tileRoute = new Tile(port);
            Styles styleRoute = new Styles(port);

            // Start handling requests for tiles and styles
            tileRoute.start();
            styleRoute.start();

            System.out.println("Server started on port " + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


//______________________________________________________________
package at.alwinschuster.HttpServer;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.Map;
//import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Random;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
//import java.util.zip.Inflater;

import androidx.annotation.Nullable;
import android.util.Log;

public class Server extends NanoHTTPD {
    private String mbtilesFilePath = "tiles.mbtiles";  // Default path

    private static final String TAG = "HttpServer";
    private static final String SERVER_EVENT_ID = "httpServerResponseReceived";
    private static final String TILE_FILE_NAME = "tiles.mbtiles";
    private static final String STYLES_FILE_NAME = "styles.json";
    private static final String TILE_CACHE_DIR = "tile_cache";
    private static final int MAX_CACHE_SIZE = 50;
    private static final long CLEANUP_INTERVAL = 24 * 60 * 60 * 1000; // 24 hours
    
    private ReactContext reactContext;
    private Map<String, byte[]> tileCache;
    private File tilesFile;

    // Executor for asynchronous tasks
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public Server(ReactContext context, int port) {
        super(port);
        reactContext = context;
        tileCache = new LinkedHashMap<>(MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        };
        tilesFile = new File(TILE_FILE_NAME);

        Log.d(TAG, "Server started");

        if (!tilesFile.exists()) {
            downloadTiles();
        }
        scheduleCleanup();
    }
    
    private void downloadTilesAsync() {
        CompletableFuture.runAsync(() -> { // Asynchronous download task
            try {
                URL url = new URL("https://example.com/path/to/tiles.mbtiles");
                InputStream in = url.openStream();
                Files.copy(in, Paths.get(TILE_FILE_NAME), StandardCopyOption.REPLACE_EXISTING);
                in.close();
                isTilesDownloaded = true; // Set the flag to true once download is complete
                Log.d(TAG, "Tiles downloaded successfully.");
            } catch (Exception e) {
                Log.e(TAG, "Error downloading tiles: " + e.getMessage());
            }
        }, executor); // Use the existing executor
    }

    /**
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private void scheduleCleanup() { //asynchronously
        scheduler.scheduleAtFixedRate(this::cleanOldTiles, 0, CLEANUP_INTERVAL, TimeUnit.MILLISECONDS);
    }
     */
    //2 option: ScheduledExecutorService
    private void scheduleCleanup() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(CLEANUP_INTERVAL);
                    cleanOldTiles();
                } catch (InterruptedException e) {
                    Log.e(TAG, "Cleanup interrupted");
                }
            }
        }).start();
    }

    private void cleanOldTiles() {
        File cacheDir = new File(TILE_CACHE_DIR);
        if (!cacheDir.exists()) return;

        File[] files = cacheDir.listFiles();
        long now = System.currentTimeMillis();

        for (File file : files) {
            if (now - file.lastModified() > CLEANUP_INTERVAL) {
                file.delete();
            }
        }
    }
    //The serve() method is where you'd process incoming HTTP requests. You would define the logic for how to respond based on the URL, HTTP method, and parameters
    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        // This will be used to send back the response asynchronously
        final CompletableFuture<Response> responseFuture = new CompletableFuture<>();

        executor.submit(() -> {// asynchronously
            Log.d(TAG, "Request received: " + uri);
            Response response = null;

            // Match the route and call the corresponding handler
            if (uri.equals("/")) {
                return newFixedLengthResponse("Server is running!");
            } else if (uri.matches("/tile/\\d+/\\d+/\\d+")) {
                // Handle the tile request asynchronously
                handleTileRequest(uri, session, responseFuture);
            } else if (uri.equals("/style.json")) {
                // Handle the style request asynchronously
                handleStyleRequest();
            } else if (uri.matches("/glyphs/[^/]+/[^/]+\\.pbf")) {
                // Handle the glyphs request asynchronously
                handleGlyphsRequest(uri);
            } else {
                return newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
            }

            // Send the response once it's ready
            responseFuture.complete(response);
        });
        // Return a future response placeholder
        try {
            return responseFuture.get();  // Wait for async processing to finish
        } catch (Exception e) {
            Log.e(TAG, "Error waiting for response: " + e.getMessage());
            return newFixedLengthResponse(Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error processing request");
        }
    }

    private void handleTileRequest(String tileRequest, IHTTPSession session, CompletableFuture<Response> responseFuture) {
        executor.submit(() -> {// Fetch and process the tile data asynchronously
            byte[] tileData = null;
            if (tileCache.containsKey(tileRequest)) {
                tileData = tileCache.get(tileRequest);
            } else {
                tileData = getTileDataAsync(tileRequest);
                if (tileData != null) {
                    tileCache.put(tileRequest, tileData);
                }
            }
    
            Response response;
            if (tileData != null) {
                response = newFixedLengthResponse(Status.OK, "application/x-protobuf", new ByteArrayInputStream(tileData), tileData.length);
            } else {
                response = newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "Tile not found");
            }
    
            // Send the response back once the processing is done
            responseFuture.complete(response);
            try {
                session.sendResponse(response);
            } catch (IOException e) {
                Log.e(TAG, "Error sending response to client: " + e.getMessage());
            }
                
        });
    }
    
    // Process the tile (use SQLite to fetch)
    private byte[] getTileDataAsync(String tileRequest) {
        // Asynchronous database query (returning the tile data)
        final CompletableFuture<byte[]> tileDataFuture = new CompletableFuture<>();
        executor.submit(() -> {
            byte[] tileData = getTileData(tileRequest);
            tileDataFuture.complete(tileData);  // Complete the future when done
        });
    
        // Wait for the result of the asynchronous operation
        try {
            return tileDataFuture.get();  // Return the tile data once the background task completes
        } catch (Exception e) {
            Log.e(TAG, "Error fetching tile data asynchronously: " + e.getMessage());
        }
        return null;  // Return null if there was an error
    }
    
    private byte[] getTileData(String tileRequest) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + TILE_FILE_NAME)) {
            String[] parts = tileRequest.split("/");
            if (parts.length < 4) return null;
            // Extract params for z, x, y
            int z = Integer.parseInt(parts[2]);
            int x = Integer.parseInt(parts[3]);
            int y = Integer.parseInt(parts[4].split("\\.")[0]);
            y = (1 << z) - 1 - y; // Convert TMS to XYZ

            //Consider batching tile requests or optimizing the database queries if needed.
            String query = "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, z);
                stmt.setInt(2, x);
                stmt.setInt(3, y);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        byte[] compressedData = rs.getBytes("tile_data");
                        return decompress(compressedData);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching tile data: " + e.getMessage());
        }
        return null;
    }

    private byte[] decompress(byte[] compressedData) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedData);
            GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "Error decompressing tile data: " + e.getMessage());
        }
        return null;
    }  

    private Response handleStyleRequest() {
        final CompletableFuture<Response> responseFuture = new CompletableFuture<>();
        executor.submit(() -> {
            try {
                byte[] stylesJson = Files.readAllBytes(Paths.get(STYLES_FILE_NAME));  // Asynchronously read the file
                responseFuture.complete(newFixedLengthResponse(Status.OK, "application/json", new ByteArrayInputStream(stylesJson), stylesJson.length));
            } catch (Exception e) {
                responseFuture.complete(newFixedLengthResponse(Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error reading styles.json"));
            }
        });
    
        try {
            return responseFuture.get();  // Wait for the async operation to finish
        } catch (Exception e) {
            Log.e(TAG, "Error fetching styles asynchronously: " + e.getMessage());
            return newFixedLengthResponse(Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error processing request");
        }
    }
}

/**
 * private Response handleGlyphsRequest(IHTTPSession session) {
        // Get fontstack and range from URI
        String[] uriParts = session.getUri().split("/");
        String fontstack = uriParts[2];
        String range = uriParts[3];

        // Return the PBF glyphs file
        byte[] glyphsData = getGlyphs(fontstack, range);
        return newFixedLengthResponse(Response.Status.OK, "application/x-protobuf", glyphsData);
    }
 */

 package at.alwinschuster.HttpServer;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.CompletableFuture;
import java.util.zip.GZIPInputStream;

import android.util.Log;

public class Server extends NanoHTTPD {
    private static final String TAG = "HttpServer";
    private static final String TILE_FILE_NAME = "tiles.mbtiles";
    private static final String STYLES_FILE_NAME = "styles.json";
    private static final String TILE_CACHE_DIR = "tile_cache";
    private static final int MAX_CACHE_SIZE = 100;
    private static final long CLEANUP_INTERVAL = 24 * 60 * 60 * 1000; // 24 hours

    private ReactContext reactContext;
    private Map<String, byte[]> tileCache;
    private File tilesFile;
    private final ExecutorService executor;

    private Thread cleanupThread; // Thread for cleanup

    public Server(ReactContext context, int port) {
        super(port);
        reactContext = context;
        tileCache = new LRUCache<>(MAX_CACHE_SIZE);
        tilesFile = new File(TILE_FILE_NAME);

        executor = Executors.newFixedThreadPool(4); // Fixed thread pool for handling requests

        Log.d(TAG, "Server started");

        if (!tilesFile.exists()) {
            downloadTiles();
        }

        scheduleCleanup();
    }

    private void downloadTiles() {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL("https://example.com/path/to/tiles.mbtiles");
                Files.copy(url.openStream(), Paths.get(TILE_FILE_NAME), StandardCopyOption.REPLACE_EXISTING);
                Log.d(TAG, "Tiles downloaded successfully.");
            } catch (Exception e) {
                Log.e(TAG, "Error downloading tiles: " + e.getMessage());
            }
        }, executor);
    }

    private void scheduleCleanup() {
        cleanupThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    Thread.sleep(CLEANUP_INTERVAL);
                    cleanOldTiles();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.e(TAG, "Cleanup thread interrupted");
                }
            }
        });
        cleanupThread.start();
    }

    private void cleanOldTiles() {
        File cacheDir = new File(TILE_CACHE_DIR);
        if (!cacheDir.exists()) return;

        File[] files = cacheDir.listFiles();
        long now = System.currentTimeMillis();
        for (File file : files) {
            if (now - file.lastModified() > CLEANUP_INTERVAL) {
                file.delete();
            }
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        if (uri.equals("/")) {
            return newFixedLengthResponse("Server is running!");
        } else if (uri.matches("/tile/\\d+/\\d+/\\d+")) {
            return handleTileRequest(uri);
        } else if (uri.equals("/style.json")) {
            return handleStyleRequest();
        } else {
            return newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
        }
    }

    private Response handleTileRequest(String tileRequest) {
        return CompletableFuture.supplyAsync(() -> {
            byte[] tileData = tileCache.get(tileRequest);
            if (tileData == null) {
                tileData = getTileData(tileRequest);
                if (tileData != null) {
                    tileCache.put(tileRequest, tileData);
                }
            }
            if (tileData != null) {
                return newFixedLengthResponse(Status.OK, "application/x-protobuf", new ByteArrayInputStream(tileData), tileData.length);
            } else {
                return newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "Tile not found");
            }
        }, executor).join();  // Non-blocking, returns the response once the task is complete
    }

    private byte[] getTileData(String tileRequest) {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + TILE_FILE_NAME)) {
            String[] parts = tileRequest.split("/");
            if (parts.length < 4) return null;

            int z = Integer.parseInt(parts[2]);
            int x = Integer.parseInt(parts[3]);
            int y = Integer.parseInt(parts[4].split("\\.")[0]);
            y = (1 << z) - 1 - y;

            String query = "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, z);
                stmt.setInt(2, x);
                stmt.setInt(3, y);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        byte[] compressedData = rs.getBytes("tile_data");
                        return decompress(compressedData);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching tile data: " + e.getMessage());
        }
        return null;
    }

    private byte[] decompress(byte[] compressedData) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "Error decompressing tile data: " + e.getMessage());
        }
        return null;
    }

    private Response handleStyleRequest() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] stylesJson = Files.readAllBytes(Paths.get(STYLES_FILE_NAME));
                return newFixedLengthResponse(Status.OK, "application/json", new ByteArrayInputStream(stylesJson), stylesJson.length);
            } catch (IOException e) {
                Log.e(TAG, "Error reading styles.json: " + e.getMessage());
                return newFixedLengthResponse(Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error reading styles.json");
            }
        }, executor).join();
    }

    public void stop() {
        // Graceful shutdown logic
        if (cleanupThread != null) {
            cleanupThread.interrupt(); // Interrupt the cleanup thread
        }
        executor.shutdown(); // Shutdown the executor service gracefully
        super.stop(); // Stop the NanoHTTPD server
    }

    // LRU Cache Implementation for tile cache
    private static class LRUCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxSize;

        public LRUCache(int maxSize) {
            super(16, 0.75f, true);
            this.maxSize = maxSize;
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > maxSize;
        }
    }
}
