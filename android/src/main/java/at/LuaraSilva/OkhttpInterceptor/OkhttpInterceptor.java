package at.LuaraSilva.OkhttpInterceptor;

// Import the MBTilesDatabaseHelper class
import at.LuaraSilva.OkhttpInterceptor.MBTilesDatabaseHelper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import androidx.annotation.NonNull;
import android.util.LruCache;

import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;

public class OkhttpInterceptor implements Interceptor {
    private static final String TAG = "OkhttpInterceptor";
    private static final int CACHE_SIZE = 100; // LRU Cache Size for Tiles
    private static final LruCache<String, byte[]> tileCache = new LruCache<>(CACHE_SIZE);
    //executor is unused — unless you plan to offload tile reads to background later, you can remove it
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private final Context context;
    private final String folderName;
    private final String tilesFileName;

    public OkhttpInterceptor(Context context, String folderName, String tilesFileName) {
        this.context = context;
        this.folderName = folderName;
        this.tilesFileName = tilesFileName;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        String url = request.url().toString();
        Log.d(TAG, "Request: " + url);
        
        if (url.contains(folderName + "/" + tilesFileName + "/")) { //url.endsWith(".pbf") &&  url.matches("http://local/tiles/\\d+/\\d+/\\d+\\.pbf") or if (url.matches(".*local/tiles/\\d+/\\d+/\\d+\\.pbf"))

            Log.d(TAG, "Intercepting tile request: " + url);
            String[] parts = url.split("/");
            if (parts.length < 5) {
                return createErrorResponse(request, 400, "Invalid tile URL format");
            }
            
            int z = Integer.parseInt(parts[parts.length - 3]);  // {z}
            int x = Integer.parseInt(parts[parts.length - 2]);  // {x}
            int y = Integer.parseInt(parts[parts.length - 1].replace(".pbf", ""));  // {y}

            // Fetch tile data from cache or database
            byte[] tileData = getTileData( z, x, y);
            
            if (tileData != null) {
                Log.d(TAG, "Creating response for request: " + request);
                return createResponse(request, tileData);
            } else {
                Log.e(TAG, "Tile not found: " + url);
                return createErrorResponse(request, 404, "Tile Not Found");
            }
        }

        // Continue normal request if not a tile request
        return chain.proceed(request);
    }

    private byte[] getTileData(int z, int x, int y) {
        String cacheKey = tilesFileName + "_" + z + "_" + x + "_" + y;
    
        // Check LRU Cache first
        byte[] cachedTile = tileCache.get(cacheKey);
        if (cachedTile != null) {
            return cachedTile;
        }       
    
        // Get tile from SQLite database (Thread-safe)
        MBTilesDatabaseHelper dbHelper = MBTilesDatabaseHelper.getInstance(context, folderName + "/" + tilesFileName + ".mbtiles");
        if (dbHelper == null) {
            Log.e(TAG, "DB helper was null for file: " + folderName + " " + tilesFileName);
            return null;
        }
    
        SQLiteDatabase db = dbHelper.getDatabase();
        if (db != null) {
            byte[] tileData = fetchTileFromDB(db, z, x, y);
            if (tileData != null) {
                tileCache.put(cacheKey, tileData);
            }
            return tileData;
        }
    
        return null;
    }    
    
    private byte[] fetchTileFromDB(SQLiteDatabase db, int z, int x, int y) {
        int tmsY = (1 << z) - 1 - y; // Convert XYZ to TMS
        Cursor cursor = db.rawQuery(
                "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?",
                new String[] { String.valueOf(z), String.valueOf(x), String.valueOf(tmsY) });

        Log.d(TAG, "Fetching tile with SQL query: " + "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?" + " with parameters: " + z + ", " + x + ", " + tmsY);
            
        byte[] tileData = null;
        if (cursor.moveToFirst()) {
            tileData = cursor.getBlob(0);
            Log.d(TAG, "Tile data found: " + tileData.length + " bytes");
        } else {
            Log.e(TAG, "No tile data found for z=" + z + ", x=" + x + ", tmsY=" + tmsY);
        }
        cursor.close();

        // If tile data is found, check if it's compressed and decompress if needed
        if (tileData != null && isGzipCompressed(tileData)) {
            try {
                tileData = decompressGzip(tileData);
                Log.d(TAG, "Tile decompressed successfully");
            } catch (IOException e) {
                Log.e("OkhttpInterceptor", "Failed to decompress GZIP tile", e);
            }
        } else {
            Log.d(TAG, "Tile data is not compressed or already decompressed");
        }

        return tileData;
    }

    /**
     * Checks if the byte array is GZIP compressed.
     */
    private boolean isGzipCompressed(byte[] data) {
        if (data.length < 2) {
            return false;
        }
        // GZIP signature: first two bytes should be 0x1F and 0x8B
        return (data[0] == (byte) 0x1F) && (data[1] == (byte) 0x8B);
    }

    /**
     * Decompresses a GZIP-compressed byte array.
     */
    private byte[] decompressGzip(byte[] compressedData) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedData);
        GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }

        gzipInputStream.close();
        byteArrayInputStream.close();
        byteArrayOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    private Response createResponse(Request request, byte[] tileData) {
        return new Response.Builder()
                .request(request)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(MediaType.parse("application/x-protobuf"), tileData))
                .protocol(Protocol.HTTP_1_1)
                .build();
    }

    private Response createErrorResponse(Request request, int statusCode, String message) {
        return new Response.Builder()
                .request(request)
                .code(statusCode)
                .message(message)
                .body(ResponseBody.create(null, new byte[0]))
                .protocol(Protocol.HTTP_1_1)
                .build();
    }

    public static void clearTileCache() {
        tileCache.evictAll();
    }
    
    public static void shutdownExecutor() {
        executor.shutdown();
    }
}
/**
 * private boolean isValidMbtilesFile(String mbtilesFileName) {
        try {
            File file = context.getDatabasePath(mbtilesFileName);
            if (!file.exists()) {
                Log.e(TAG, "MBTiles file does not exist: " + file.getAbsolutePath());
                return false;
            }
    
            if (file.length() < 1024) {
                Log.e(TAG, "MBTiles file is too small to be valid: " + file.getAbsolutePath());
                return false;
            }
    
            // Try opening the DB to ensure it's valid
            SQLiteDatabase db = SQLiteDatabase.openDatabase(
                file.getAbsolutePath(),
                null,
                SQLiteDatabase.OPEN_READONLY
            );
    
            // Check for required tables
            Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='tiles'", null);
            boolean hasTilesTable = cursor.moveToFirst();
            cursor.close();
            db.close();
    
            if (!hasTilesTable) {
                Log.e(TAG, "MBTiles file is missing required 'tiles' table");
                return false;
            }
    
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to validate MBTiles file", e);
            return false;
        }
    }
 * _________________________________________________________________________________-
public static boolean isValidMBTiles(String path) {
    try {
        SQLiteDatabase db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
        boolean hasTables = cursor.moveToFirst();
        cursor.close();
        db.close();
        return hasTables;
    } catch (Exception e) {
        Log.e("MBTilesValidator", "Invalid or corrupt MBTiles file: " + path, e);
        return false;
    }
}

 */

/*
✅ Singleton Database Connection (reuses a single SQLiteDatabase instance).
✅ LRU Cache for Tiles (minimizes redundant database queries).
✅ Tile Fetching in a Background Thread (executor.execute(...) can be used in async implementations).
✅ Efficient SQLite Queries (avoids unnecessary lookups).
✅ Proper Database Resource Handling (cursor.close(), database.close()).
--------------------------------------------------------------------------------------------------------------

************* IMPORTANT NOTE *************
* THIS CODE IS WORKING - V1, 
* Above I AM TRYING TO IMPROVE THE CODE TO BE MORE EFFICIENT AND CLEANER DURING THE START OF THE APP.

package at.LuaraSilva.OkhttpInterceptor;

// Import the MBTilesDatabaseHelper class
import at.LuaraSilva.OkhttpInterceptor.MBTilesDatabaseHelper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import androidx.annotation.NonNull;
import android.util.LruCache;

import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.GZIPInputStream;

public class OkhttpInterceptor implements Interceptor {
    private static final String TAG = "OkhttpInterceptor";
    private static final int CACHE_SIZE = 100; // LRU Cache Size for Tiles
    private static final LruCache<String, byte[]> tileCache = new LruCache<>(CACHE_SIZE);
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private final Context context;

    public OkhttpInterceptor(Context context) {
        this.context = context;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        String url = request.url().toString();
        Log.d(TAG, "Request: " + url);
        // Intercept only .pbf tile requests
        if (url.contains("local/tiles/")) { //url.endsWith(".pbf") &&  url.matches("http://local/tiles/\\d+/\\d+/\\d+\\.pbf") or if (url.matches(".*local/tiles/\\d+/\\d+/\\d+\\.pbf"))

            Log.d(TAG, "Intercepting tile request: " + url);
            String[] parts = url.split("/");
            if (parts.length < 5) {
                return createErrorResponse(request, 400, "Invalid tile URL format");
            }
            String urlKeyWord = parts[parts.length - 5];  // local
            Log.d(TAG, "urlKeyWord: " + urlKeyWord);
            String mbtilesFileName = parts[parts.length - 4];   // "tiles.mbtiles"
            int z = Integer.parseInt(parts[parts.length - 3]);  // {z}
            int x = Integer.parseInt(parts[parts.length - 2]);  // {x}
            int y = Integer.parseInt(parts[parts.length - 1].replace(".pbf", ""));  // {y}

            // Fetch tile data from cache or database
            byte[] tileData = getTileData(mbtilesFileName, z, x, y);
            
            if (tileData != null) {
                Log.d(TAG, "Creating response for request: " + request);
                return createResponse(request, tileData);
            } else {
                Log.e(TAG, "Tile not found: " + url);
                return createErrorResponse(request, 404, "Tile Not Found");
            }
        }

        // Continue normal request if not a tile request
        return chain.proceed(request);
    }

    private byte[] getTileData(String mbtilesFileName, int z, int x, int y) {
        String cacheKey = mbtilesFileName + "_" + z + "_" + x + "_" + y;

        // Check LRU Cache first
        byte[] cachedTile = tileCache.get(cacheKey);
        if (cachedTile != null) {
            return cachedTile;
        }

        // Get tile from SQLite database (Thread-safe)
        MBTilesDatabaseHelper dbHelper = MBTilesDatabaseHelper.getInstance(context, mbtilesFileName);
        SQLiteDatabase db = dbHelper.getDatabase();
        Log.d(TAG, "db: " + db);
        if (db != null) {
            byte[] tileData = fetchTileFromDB(db, z, x, y);
            Log.d(TAG, "tileData: " + tileData);
            db.close();
            if (tileData != null) {
                tileCache.put(cacheKey, tileData); // Store in cache
            }
            return tileData;
        }

        return null;
    }

    private byte[] fetchTileFromDB(SQLiteDatabase db, int z, int x, int y) {
        int tmsY = (1 << z) - 1 - y; // Convert XYZ to TMS
        Cursor cursor = db.rawQuery(
                "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?",
                new String[] { String.valueOf(z), String.valueOf(x), String.valueOf(tmsY) });

        Log.d(TAG, "Fetching tile with SQL query: " + "SELECT tile_data FROM tiles WHERE zoom_level = ? AND tile_column = ? AND tile_row = ?" + " with parameters: " + z + ", " + x + ", " + tmsY);
            
        byte[] tileData = null;
        if (cursor.moveToFirst()) {
            tileData = cursor.getBlob(0);
            Log.d(TAG, "Tile data found: " + tileData.length + " bytes");
        } else {
            Log.e(TAG, "No tile data found for z=" + z + ", x=" + x + ", tmsY=" + tmsY);
        }
        cursor.close();

        // If tile data is found, check if it's compressed and decompress if needed
        if (tileData != null && isGzipCompressed(tileData)) {
            try {
                tileData = decompressGzip(tileData);
                Log.d(TAG, "Tile decompressed successfully");
            } catch (IOException e) {
                Log.e("OkhttpInterceptor", "Failed to decompress GZIP tile", e);
            }
        } else {
            Log.d(TAG, "Tile data is not compressed or already decompressed");
        }

        return tileData;
    }

    /**
     * Checks if the byte array is GZIP compressed.
     *
    private boolean isGzipCompressed(byte[] data) {
        if (data.length < 2) {
            return false;
        }
        // GZIP signature: first two bytes should be 0x1F and 0x8B
        return (data[0] == (byte) 0x1F) && (data[1] == (byte) 0x8B);
    }

    /**
     * Decompresses a GZIP-compressed byte array.
     *
    private byte[] decompressGzip(byte[] compressedData) throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedData);
        GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = gzipInputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }

        gzipInputStream.close();
        byteArrayInputStream.close();
        byteArrayOutputStream.close();

        return byteArrayOutputStream.toByteArray();
    }

    private Response createResponse(Request request, byte[] tileData) {
        return new Response.Builder()
                .request(request)
                .code(200)
                .message("OK")
                .body(ResponseBody.create(MediaType.parse("application/x-protobuf"), tileData))
                .protocol(Protocol.HTTP_1_1)
                .build();
    }

    private Response createErrorResponse(Request request, int statusCode, String message) {
        return new Response.Builder()
                .request(request)
                .code(statusCode)
                .message(message)
                .body(ResponseBody.create(null, new byte[0]))
                .protocol(Protocol.HTTP_1_1)
                .build();
    }

    public static void clearTileCache() {
        tileCache.evictAll();
    }
    
    public static void shutdownExecutor() {
        executor.shutdown();
    }
}
*/
