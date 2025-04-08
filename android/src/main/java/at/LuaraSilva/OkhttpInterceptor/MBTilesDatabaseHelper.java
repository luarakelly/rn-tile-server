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
    public static synchronized MBTilesDatabaseHelper getInstance(@NonNull Context context, @NonNull String mbtilesFolderName,
                                                                  @NonNull String mbtilesFileName) {
        // Construct the full path using both the folder and the file name
        File mbtilesFile = new File(context.getFilesDir(), mbtilesFolderName + "/" + mbtilesFileName);
        if (!file.exists()) {
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

