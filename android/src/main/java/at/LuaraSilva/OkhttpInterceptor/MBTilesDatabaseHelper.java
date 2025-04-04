package at.LuaraSilva.OkhttpInterceptor;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

public class MBTilesDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "MBTilesDatabaseHelper";
    private static final int DATABASE_VERSION = 1;
    private static MBTilesDatabaseHelper instance;
    private final String dbPath;

    private MBTilesDatabaseHelper(Context context, String mbtilesFolderName, String mbtilesFileName) {
        super(context, null, null, DATABASE_VERSION);
        // Use both the folder and file to construct the path
        File mbtilesFolder = new File(context.getFilesDir(), mbtilesFolderName);
        if (!mbtilesFolder.exists()) {
            mbtilesFolder.mkdirs(); // Ensure folder exists
        }
        this.dbPath = new File(mbtilesFolder, mbtilesFileName).getAbsolutePath();
        // this.dbPath = new File(context.getFilesDir(),
        // mbtilesFileName).getAbsolutePath();
    }

    public static synchronized MBTilesDatabaseHelper getInstance(Context context, String mbtilesFolderName,
            String mbtilesFileName) {
        // Construct the full path using both the folder and the file name
        File mbtilesFolder = new File(context.getFilesDir(), mbtilesFolderName);
        File mbtilesFile = new File(mbtilesFolder, mbtilesFileName);

        if (instance == null
                || !instance.dbPath.equals(mbtilesFile.getAbsolutePath())) {
            instance = new MBTilesDatabaseHelper(context, mbtilesFolderName, mbtilesFileName);
        }
        /*
         * if (instance == null
         * || !instance.dbPath.equals(new File(context.getFilesDir(),
         * mbtilesFileName).getAbsolutePath())) {
         * instance = new MBTilesDatabaseHelper(context, mbtilesFileName);
         * }
         */
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
