package com.gingerman.tictactoe;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import com.gingerman.tictactoe.data.DatabaseManager;
import com.gingerman.tictactoe.model.Game;
import com.gingerman.tictactoe.model.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The brains of the data side of the application, this singleton allows access to game logic
 */
public class ApplicationManager {
    private static ApplicationManager sInstance = null;

    private Activity mActivity;
    public List<Player> players = new ArrayList<Player>();

    private Bitmap xBmp = null;
    private Bitmap oBmp = null;

    public interface ApplicationManagerListener {
        public void onComplete(); // callback when initialization is complete
        public void onError(String msg); // error occurred
    }
    public interface CreateGameListener {
        public void onComplete(Game game); // callback to report game created
        public void onError(String msg); // error occurred
    }

    protected ApplicationManager() {}
    public static ApplicationManager getsInstance() {
        if (sInstance == null) {
            sInstance = new ApplicationManager();
        }
        return sInstance;
    }

    /**
     * WARNING: This initializer must be called before the instance is used (should be called in onCreate)
     * @param activity activity context we are running in
     * @param listener provides callback when manager is usable, on UI thread
     */
    public void initialize(final Activity activity, final ApplicationManagerListener listener) {
        mActivity = activity;
        xBmp = BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.x);
        oBmp = BitmapFactory.decodeResource(mActivity.getResources(), R.drawable.o);
        DatabaseManager.getInstance().initialize(mActivity, new ApplicationManagerListener() {
            @Override
            public void onComplete() {
                // Database is ready, populate our in memory data
                new LoadObjectsIntoMemoryTask().execute(new ApplicationManagerListener[]{listener});
            }

            @Override
            public void onError(String msg) {
                if (listener != null) listener.onError("Initialization failed with error message: "+msg);
            }
        });
    }

    /**
     * WARNING: Must be called from the main activity onDestroy method to ensure cleanup
     */
    public void destroy() {
        DatabaseManager.getInstance().destroy();
    }

    /**
     * Players are fetched and created as needed, and a new Game is returned. This game is not
     * serialized to database until completed.
     * @param playerName1 name of first player
     * @param playerName2 name of second player
     * @return a new Game object representing this game
     */
    public void createNewGame(String playerName1, String playerName2, final CreateGameListener listener) {
        if (playerName1 == null || playerName1.length() == 0) playerName1 = "Default1";
        if (playerName2 == null || playerName2.length() == 0 || playerName1.equals(playerName2)) playerName2 = "Default2";

        // find/create player, potentially writing new players to db
        Player player1 = null;
        Player player2 = null;
        for (Player p : players) {
            if (playerName1.equals(p.name)) player1 = p;
            else if (playerName2.equals(p.name)) player2 = p;
        }
        // if not found, create new players and return from database
        if (player1 == null) {
            player1 = DatabaseManager.getInstance().createPlayer(playerName1);
        }
        if (player2 == null) {
            player2 = DatabaseManager.getInstance().createPlayer(playerName2);
        }

        final Player finalPlayer1 = player1;
        final Player finalPlayer2 = player2;

        // Database might have changed, and this is a small app example, so lets repopulate our in memory data to be sure its up to date
        new LoadObjectsIntoMemoryTask().execute(new ApplicationManagerListener[]{new ApplicationManagerListener() {
            @Override
            public void onComplete() {
                if (listener != null) listener.onComplete(finalPlayer1 == null || finalPlayer2 == null ? null : new Game(finalPlayer1, finalPlayer2, xBmp, oBmp));
            }

            @Override
            public void onError(String msg) {
                if (listener != null) listener.onError("Error creating game: " + msg);
            }
        }});
    }

    public void gameCompleted(Game game) {
        if (game == null) return; // nothing to do
        DatabaseManager.getInstance().serializeGameResult(game);
    }

    private class LoadObjectsIntoMemoryTask extends AsyncTask<ApplicationManagerListener, Void, ApplicationManagerListener> {
        @Override
        protected ApplicationManagerListener doInBackground(ApplicationManagerListener... listener) {

            // load in players, and player records
            players = new ArrayList<Player>();
            List<Player> dbPlayers = DatabaseManager.getInstance().fetchAllPlayers();
            if (dbPlayers != null) players.addAll(dbPlayers);

            return listener.length > 0 ? listener[0] : null;
        }

        @Override
        protected void onPostExecute(ApplicationManagerListener listener) {
            if (listener != null) {
                listener.onComplete();
            }
        }
    }
}
