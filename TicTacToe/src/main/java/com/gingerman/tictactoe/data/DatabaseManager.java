package com.gingerman.tictactoe.data;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import com.gingerman.tictactoe.ApplicationManager;
import com.gingerman.tictactoe.model.Game;
import com.gingerman.tictactoe.model.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates interaction with the database into an easy to use manager object
 */
public class DatabaseManager {
    private static final String LOG_TAG = "DatabaseManager";

    /**
     * MUST be updated if db changes between releases!
     *
     * Historic db versions:
     * 1. Initial impl
     */
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "tictactoe.db";
    private Activity mActivity;
    private DatabaseOpenHelper mDbHelper = null;

    private static DatabaseManager sInstance = null;

    protected DatabaseManager() {
    }

    /**
     * @return the singleton instance of this manager
     */
    public static DatabaseManager getInstance() {
        if (sInstance == null) {
            sInstance = new DatabaseManager();
        }
        return sInstance;
    }

    /**
     * WARNING: This initializer must be called before the instance is used
     * @param activity activity context we are running in
     * @param listener provides callback when manager is usable, on ui thread
     */
    public void initialize(Activity activity, ApplicationManager.ApplicationManagerListener listener) {
        mActivity = activity;

        new DatabaseCreationTask().execute(new ApplicationManager.ApplicationManagerListener[]{listener});
    }

    /**
     * WARNING: Must be called from the main activity onDestroy method to ensure cleanup
     */
    public void destroy() {
        mDbHelper.close();
    }

    /**
     * AsyncTask to perform the db intialization in the background thread, and respond when it is ready to use via listener callback
     */
    private class DatabaseCreationTask extends AsyncTask<ApplicationManager.ApplicationManagerListener, Void, ApplicationManager.ApplicationManagerListener> {
        @Override
        protected ApplicationManager.ApplicationManagerListener doInBackground(ApplicationManager.ApplicationManagerListener... listener) {
            mDbHelper = new DatabaseOpenHelper(mActivity.getApplicationContext(), DB_NAME, null, DB_VERSION);
            return listener.length > 0 ? listener[0] : null;
        }

        @Override
        protected void onPostExecute(ApplicationManager.ApplicationManagerListener listener) {
            if (listener != null) {
                listener.onComplete();
            }
        }
    }

    /**
     * Custom OpenHelper class to create our db tables, handle upgrades, etc.
     * For purposes of this exercise, this Helper is very simple and will overwrite stats on an upgrade.
     */

    //
    private class DatabaseOpenHelper extends SQLiteOpenHelper {
        public DatabaseOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            createDatabase(db);
        }

        private void createDatabase(SQLiteDatabase db) {
            try {
                db.beginTransaction();

                // Create player table for Player class
                StringBuilder sql = new StringBuilder();
                sql.append(String.format("CREATE TABLE %s (", Player.DB_FIELDS.tableName));
                sql.append(" id INTEGER PRIMARY KEY, ");
                sql.append(" name TEXT NOT NULL UNIQUE, ");
                sql.append(" wins INTEGER DEFAULT 0, ");
                sql.append(" losses INTEGER DEFAULT 0, ");
                sql.append(" draws INTEGER DEFAULT 0 ");
                sql.append(");");

                execute(db, sql.toString());

                // close off transaction
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        /**
         * WARNING: This implementation blows away the full db, and starts from scratch.
         * Should include logic to upgrade based on version numbers, but you know that already...
         * @param db
         * @param oldVersion
         * @param newVersion
         */
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            try {
                db.beginTransaction();

                db.execSQL("DROP TABLE IF EXISTS game;");
                db.execSQL(String.format("DROP TABLE IF EXISTS player;", Player.DB_FIELDS.tableName));

                db.setTransactionSuccessful();

                // now recreate
                createDatabase(db);
            } finally {
                db.endTransaction();
            }
        }
    }

    /**
     * @return a List of all Players known to the database
     */
    public List<Player> fetchAllPlayers() {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        try {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT * ");
            sql.append(String.format(" FROM   %s; ", Player.DB_FIELDS.tableName));

            Cursor cursor = execute(db, sql.toString(), null);
            if (cursor == null || !cursor.moveToFirst()) return null;

            List<Player> players = new ArrayList<Player>();
            while (!cursor.isAfterLast()) {
                players.add(new Player(cursor));
                cursor.moveToNext();
            }

            return players;
        } finally {
            db.close();
        }
    }

    /**
     * @param name name of the player to create (record will be empty)
     * @return A new, db backed up, Player object representing the new player requested
     */
    public Player createPlayer(String name) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        Player player = null;
        try {
            db.beginTransaction();

            ContentValues contentValues = new ContentValues(1);
            contentValues.put(Player.DB_FIELDS.name, name);
            int id = (int) db.insertWithOnConflict(Player.DB_FIELDS.tableName, null, contentValues, SQLiteDatabase.CONFLICT_ROLLBACK);
            if (id >= 0) {
                player = new Player(id, name);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
        return player;
    }

    /**
     * Serialize the game result (ie, update player table) based on details of the Game provided
     * @param game Game containing results to serialize
     */
    public void serializeGameResult(Game game) {
        if (game == null) return; // bad state

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            Player player1 = game.player1;
            int p1row = db.update(Player.DB_FIELDS.tableName, player1.getSerializedValues(),
                    String.format("id = %s", player1.id), null);

            Player player2 = game.player2;
            int p2row = db.update(Player.DB_FIELDS.tableName, player2.getSerializedValues(),
                    String.format("id = %s", player2.id), null);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            db.close();
        }
    }

    // db helper methods
    private void execute(SQLiteDatabase db, String sql) {
        Log.d(LOG_TAG, "executing sql statement: " + sql);
        db.execSQL(sql);
    }
    private Cursor execute(SQLiteDatabase db, String sql, String[] selectionArgs) {
        Log.d(LOG_TAG, "executing raw sql statement: " + sql);
        return db.rawQuery(sql, selectionArgs);
    }
}
