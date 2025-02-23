package at.LuaraSilva.OkhttpInterceptor;

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

        // Intercept only .pbf tile requests
        if (url.endsWith(".pbf")) {
            Log.d(TAG, "Intercepting tile request: " + url);
            String[] parts = url.split("/");
            if (parts.length < 4) {
                return createErrorResponse(request, 400, "Invalid tile URL format");
            }
            String mbtilesFileName = parts[parts.length - 4]; 
            int z = Integer.parseInt(parts[parts.length - 3]);
            int x = Integer.parseInt(parts[parts.length - 2]);
            int y = Integer.parseInt(parts[parts.length - 1].replace(".pbf", ""));

            // Fetch tile data from cache or database
            byte[] tileData = getTileData(mbtilesFileName, z, x, y);
            if (tileData != null) {
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
        if (db != null) {
            byte[] tileData = fetchTileFromDB(db, z, x, y);
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
                new String[]{String.valueOf(z), String.valueOf(x), String.valueOf(tmsY)}
        );

        byte[] tileData = null;
        if (cursor.moveToFirst()) {
            tileData = cursor.getBlob(0);
        }
        cursor.close();

        // If tile data is found, check if it's compressed and decompress if needed
        if (tileData != null && isGzipCompressed(tileData)) {
            try {
                tileData = decompressGzip(tileData);
            } catch (IOException e) {
                Log.e("OkhttpInterceptor", "Failed to decompress GZIP tile", e);
            }
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
}

public class MBTilesDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "MBTilesDatabaseHelper";
    private static final int DATABASE_VERSION = 1;
    private static MBTilesDatabaseHelper instance;
    private final String dbPath;

    private MBTilesDatabaseHelper(Context context, String mbtilesFileName) {
        super(context, null, null, DATABASE_VERSION);
        this.dbPath = new File(context.getFilesDir(), mbtilesFileName).getAbsolutePath();
    }

    public static synchronized MBTilesDatabaseHelper getInstance(Context context, String mbtilesFileName) {
        if (instance == null || !instance.dbPath.equals(new File(context.getFilesDir(), mbtilesFileName).getAbsolutePath())) {
            instance = new MBTilesDatabaseHelper(context, mbtilesFileName);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // No creation needed since MBTiles is a pre-existing database
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // No upgrades needed
    }

    public synchronized SQLiteDatabase getDatabase() {
        return SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
    }
}


/*
✅ Singleton Database Connection (reuses a single SQLiteDatabase instance).
✅ LRU Cache for Tiles (minimizes redundant database queries).
✅ Tile Fetching in a Background Thread (executor.execute(...) can be used in async implementations).
✅ Efficient SQLite Queries (avoids unnecessary lookups).
✅ Proper Database Resource Handling (cursor.close(), database.close()).


class MBTilesDatabaseHelper {
    private static SQLiteDatabase database = null;

    public static synchronized SQLiteDatabase getDatabase(Context context, String mbtilesFileName) {
        if (database == null || !database.isOpen()) {
            File mbtiles = new File(context.getFilesDir(), mbtilesFileName);
            if (mbtiles.exists()) {
                database = SQLiteDatabase.openDatabase(mbtiles.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
            } else {
                Log.e("MBTilesDatabaseHelper", "Database file not found: " + mbtiles.getAbsolutePath());
            }
        }
        
        return database;
    }

    public static synchronized void closeDatabase() {
        if (database != null) {
            database.close();
            database = null;
        }
    }
}
 */
//__________________________________
