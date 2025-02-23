import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

import com.facebook.react.bridge.ReactContext;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.LinkedHashMap;
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
    private String bindAddress;
    private int port;

    private Map<String, byte[]> tileCache;
    private File tilesFile;

    private Thread cleanupThread; // Thread for cleanup

    // Executor for handling asynchronous tasks
    private final ExecutorService executor = Executors.newFixedThreadPool(4); // newFixedThreadPool(4) Limit the thread pool size

    public Server(ReactContext context, int port, String bindAddress) {
        super(bindAddress != null ? bindAddress : "0.0.0.0", port > 0 ? port : 8080);

        this.reactContext = context;
        this.port = port;
        this.bindAddress = bindAddress != null ? bindAddress : "0.0.0.0";  // Default to 127.0.0.1 if not provided
        
        tileCache = new LRUCache<String, byte[]>(MAX_CACHE_SIZE);
        tilesFile = new File(TILE_FILE_NAME);

        Log.d(TAG, "Server started");

        if (!tilesFile.exists()) {
            downloadTiles();
        }

        scheduleCleanup();
    }
   
    private void downloadTiles() {
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
            // Open the file from assets
            InputStream is = reactContext.getAssets().open("tile-assets/style.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            
            // Read into a StringBuilder for efficiency
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();

            String stylesJson = sb.toString();
            
            // Return JSON response
            return newFixedLengthResponse(Status.OK, "application/json", stylesJson);
        } catch (IOException e) {
            Log.e(TAG, "Error reading style.json: " + e.getMessage());
            return newFixedLengthResponse(Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error reading style.json");
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

/**
 * private void scheduleCleanup() {
        executor.submit(() -> {
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
    }
    }
 */

 /**
  * import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import android.util.Log;

public class Server extends NanoHTTPD {
    private static final String TAG = "HttpServer";
    private static final int MAX_CACHE_SIZE = 100;
    private static final long CLEANUP_INTERVAL = 24 * 60 * 60 * 1000; // 24 hours
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_BIND_ADDRESS = "127.0.0.1";

    private ReactContext reactContext;
    private String bindAddress;
    private int port;

    private ExecutorService executor;

    private ServerSocket serverSocket;  // ServerSocket for custom binding

    public Server(ReactContext context, int port, String bindAddress) {
        super(port); // For compatibility, although not used
        this.reactContext = context;
        this.port = port;
        this.bindAddress = bindAddress != null ? bindAddress : DEFAULT_BIND_ADDRESS;
        this.executor = Executors.newFixedThreadPool(4);  // Thread pool for handling requests

        Log.d(TAG, "Server initialized on port: " + port + " binding to: " + bindAddress);
    }

    @Override
    public void start() throws IOException {
        try {
            // Create a new ServerSocket that binds to the specified address and port
            InetAddress inetAddress = InetAddress.getByName(bindAddress);
            serverSocket = new ServerSocket(port, 0, inetAddress);

            Log.d(TAG, "Server started, waiting for connections...");

            // Accept client connections and process them
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                Log.d(TAG, "Client connected: " + clientSocket.getInetAddress());

                // Handle the client connection in a separate thread
                executor.submit(() -> {
                    try {
                        handleClient(clientSocket);
                    } catch (IOException e) {
                        Log.e(TAG, "Error handling client: " + e.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            Log.e(TAG, "Error starting server: " + e.getMessage());
            throw e;
        }
    }

    private void handleClient(Socket clientSocket) throws IOException {
        // Create an HTTP session and delegate the request to NanoHTTPD
        NanoHTTPD.IHTTPSession session = new NanoHTTPD.HttpSession(clientSocket, this);
        Response response = serve(session);
        
        // Write the response back to the client
        try (OutputStream os = clientSocket.getOutputStream()) {
            response.send(os);
            os.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error sending response: " + e.getMessage());
        } finally {
            clientSocket.close();
        }
    }

    @Override
    public Response serve(IHTTPSession session) {
        // Handle incoming requests
        String uri = session.getUri();

        if (uri.equals("/")) {
            return newFixedLengthResponse("Server is running!");
        } else {
            return newFixedLengthResponse(Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found");
        }
    }

    public void stop() throws IOException {
        // Close the server socket and stop accepting new connections
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
            Log.d(TAG, "Server stopped");
        }
        executor.shutdown();  // Shut down the executor service gracefully
    }

    // Other methods, like cleanup, tile handling, etc., can go here...

}
______________________
Could not connet to development server.
failed to connect to {192.168.1.101 (port8081) from /10.0.2.16(port 41846) after 5000ms
___
ipconfig
IPv4 Address. . . . . . . . . . . : 192.168.1.101
  */
