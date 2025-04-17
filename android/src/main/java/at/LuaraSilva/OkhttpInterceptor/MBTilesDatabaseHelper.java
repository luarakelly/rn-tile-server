package at.LuaraSilva.OkhttpInterceptor;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MBTilesDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "MBTilesDatabaseHelper";
    private static final int DATABASE_VERSION = 1;

    private static final Map<String, MBTilesDatabaseHelper> helperMap = new HashMap<>();

    private final String dbPath;
    private SQLiteDatabase database;

    private MBTilesDatabaseHelper(@NonNull Context context, @NonNull String absolutePath) {
        super(context, absolutePath, null, DATABASE_VERSION);
        this.dbPath = absolutePath;
    }

    /**
     * Get singleton instance per mbtiles path
     */
    public static synchronized MBTilesDatabaseHelper getInstance(@NonNull Context context, @NonNull String mbtilesRelativePath) {
        File mbtilesFile = new File(context.getFilesDir(), mbtilesRelativePath);
        String path = mbtilesFile.getAbsolutePath();

        if (!helperMap.containsKey(path)) {
            if (!mbtilesFile.exists()) {
                Log.e(TAG, "MBTiles file does not exist: " + path);
                return null;
            }

            helperMap.put(path, new MBTilesDatabaseHelper(context, path));
            Log.d(TAG, "Created new DB helper for: " + path);
        }

        return helperMap.get(path);
    }

    /**
     * Open database if not open
     */
    public synchronized SQLiteDatabase getDatabase() {
        if (database == null || !database.isOpen()) {
            try {
                database = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
                Log.d(TAG, "Database opened: " + dbPath);
            } catch (Exception e) {
                Log.e(TAG, "Failed to open DB: " + dbPath, e);
                database = null;
            }
        }
        return database;
    }

    /**
     * Close and clear instance
     */
    public synchronized void closeDatabase() {
        if (database != null && database.isOpen()) {
            database.close();
            Log.d(TAG, "Database closed: " + dbPath);
        }
        database = null;
    }

    public static synchronized void closeAll() {
        for (MBTilesDatabaseHelper helper : helperMap.values()) {
            helper.closeDatabase();
        }
        helperMap.clear();
        Log.d(TAG, "All database instances closed and cleared");
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // no-op for prebuilt db
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // no-op for prebuilt db
    }
}

/**
* ************* IMPORTANT NOTE *************
* THIS CODE IS WORKING - V1, 
* Above I AM TRYING TO IMPROVE THE CODE TO BE MORE EFFICIENT AND CLEANER DURING THE START OF THE APP.

package at.LuaraSilva.OkhttpInterceptor;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;

public class MBTilesDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "MBTilesDatabaseHelper";
    private static final int DATABASE_VERSION = 1;
    private static MBTilesDatabaseHelper instance;
    private final String dbPath;

    private MBTilesDatabaseHelper(@NonNull Context context, @NonNull String mbtilesFile) {
        // Pass a database name since SQLiteOpenHelper expects it
        super(context, mbtilesFile, null, DATABASE_VERSION);

        // Store the absolute path of the MBTiles file
        this.dbPath = mbtilesFile;
    }

    // Singleton pattern for getting the instance
    public static synchronized MBTilesDatabaseHelper getInstance(@NonNull Context context, @NonNull String mbtilesFileName) {
        // Construct the full path using file name
        File mbtilesFile = new File(context.getFilesDir(), mbtilesFileName);
        if (!mbtilesFile.exists()) {
            Log.e(TAG, "MBTiles file does not exist: " + mbtilesFile);
            // Optionally, you can throw an exception or handle the error accordingly
        }
        Log.d(TAG, "mbtilesFile path: " + mbtilesFile.getAbsolutePath());

        // Check if the instance already exists and if the path is different
        if (instance == null || !instance.dbPath.equals(mbtilesFile.getAbsolutePath())) {
            // If not, create a new instance with the updated file path
            instance = new MBTilesDatabaseHelper(context, mbtilesFile.getAbsolutePath());
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
 */