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
    public void initialize(Activity activity, ApplicationManager.InitializationListener listener) {
        mActivity = activity;

        new DatabaseCreationTask().execute(new ApplicationManager.InitializationListener[]{listener});
    }

    /**
     * WARNING: Must be called from the main activity onDestroy method to ensure cleanup
     */
    public void destroy() {
        mDbHelper.close();
    }

    private SQLiteDatabase getDb() {
        return mDbHelper != null ? mDbHelper.getWritableDatabase() : null;
    }

    private class DatabaseCreationTask extends AsyncTask<ApplicationManager.InitializationListener, Void, ApplicationManager.InitializationListener> {
        @Override
        protected ApplicationManager.InitializationListener doInBackground(ApplicationManager.InitializationListener... listener) {
            mDbHelper = new DatabaseOpenHelper(mActivity.getApplicationContext(), DB_NAME, null, DB_VERSION);
            return listener.length > 0 ? listener[0] : null;
        }

        @Override
        protected void onPostExecute(ApplicationManager.InitializationListener listener) {
            if (listener != null) {
                listener.onComplete();
            }
        }
    }

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
                sql.append(" name TEXT NOT NULL UNIQUE ");
                sql.append(");");

                execute(db, sql.toString());

                // Create game table for Game class
                sql = new StringBuilder();
                sql.append("CREATE TABLE game (");
                sql.append(" id INTEGER PRIMARY KEY, ");
                sql.append(" player_1 INTEGER NOT NULL, ");
                sql.append(" player_2 INTEGER NOT NULL, ");
                sql.append(" winner INTEGER, ");
                sql.append(String.format("  FOREIGN KEY (player_1) REFERENCES %s (id), ", Player.DB_FIELDS.tableName));
                sql.append(String.format("  FOREIGN KEY (player_2) REFERENCES %s (id), ", Player.DB_FIELDS.tableName));
                sql.append(String.format("  FOREIGN KEY (winner) REFERENCES %s (id) ", Player.DB_FIELDS.tableName));
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

    public List<Player> fetchAllPlayers() {
        SQLiteDatabase db = getDb();

//        StringBuilder sql = new StringBuilder();
//        sql.append("SELECT p.*, COUNT(w.) AS wins, COUNT(l) AS losses, COUNT(d) AS draws ");
//        sql.append(" FROM   player p, ");
//        sql.append("        game w, ");
//        sql.append("        game l, ");
//        sql.append("        game d ");
//        sql.append(" WHERE ");
//        sql.append("        p.id = w.winner AND ");
//        sql.append("        ((p.id = l.player_1 OR p.id = l.player_2) AND p.id != l.winner AND l.winner IS NOT NULL) AND ");
//        sql.append("        ((p.id = d.player_1 OR p.id = d.player_2) AND d.winner IS NULL); ");

        // TODO proper sql statement to fetch records...

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
    }

    public Player createPlayer(String name) {
        SQLiteDatabase db = getDb();
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
        }
        return player;
    }

    public int serializeGameResult(Game game) {
        if (game == null) return -1; // bad state

        SQLiteDatabase db = getDb();

        try {
            int result = -1;

            db.beginTransaction();

            result = (int) db.insert(Game.DB_FIELDS.tableName, null, game.getSerializedValues());

            Player player1 = game.player1;
            String[] whereArgs1 = {Player.DB_FIELDS.id, Long.toString(player1.id)};
            db.updateWithOnConflict(Player.DB_FIELDS.tableName, player1.getSerializedValues(),
                    "? = ?", whereArgs1, SQLiteDatabase.CONFLICT_REPLACE);

            Player player2 = game.player2;
            String[] whereArgs2 = {Player.DB_FIELDS.id, Long.toString(player2.id)};
            db.updateWithOnConflict(Player.DB_FIELDS.tableName, player2.getSerializedValues(),
                    "? = ?", whereArgs2, SQLiteDatabase.CONFLICT_REPLACE);

            db.setTransactionSuccessful();

            return result;
        } finally {
            db.endTransaction();
        }
    }

    private void execute(SQLiteDatabase db, String sql) {
        Log.d(LOG_TAG, "executing sql statement: " + sql);
        db.execSQL(sql);
    }
    private Cursor execute(SQLiteDatabase db, String sql, String[] selectionArgs) {
        Log.d(LOG_TAG, "executing raw sql statement: " + sql);
        return db.rawQuery(sql, selectionArgs);
    }
}
