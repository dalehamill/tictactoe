package com.gingerman.tictactoe;

import android.app.Activity;
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

    public interface InitializationListener {
        public void onComplete(); // callback when initialization is complete
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
    public void initialize(final Activity activity, final InitializationListener listener) {
        mActivity = activity;
        DatabaseManager.getInstance().initialize(mActivity, new InitializationListener() {
            @Override
            public void onComplete() {
                // Database is ready, populate our in memory data
                new LoadObjectsIntoMemoryTask().execute(new InitializationListener[]{listener});
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
    public Game createNewGame(String playerName1, String playerName2) {
        if (playerName1 == null || playerName1.length() == 0) playerName1 = "Default1";
        if (playerName2 == null || playerName2.length() == 0 || playerName1.equals(playerName2)) playerName2 = "Default2";

        // find/create player
        Player player1 = null;
        Player player2 = null;
        for (Player p : players) {
            if (playerName1.equals(p.name)) player1 = p;
            else if (playerName2.equals(p.name)) player2 = p;
        }
        // if not found, create new players and return from database
        if (player1 == null) player1 = DatabaseManager.getInstance().createPlayer(playerName1);
        if (player2 == null) player2 = DatabaseManager.getInstance().createPlayer(playerName2);

        return player1 == null || player2 == null ? null : new Game(player1, player2);
    }

    private class LoadObjectsIntoMemoryTask extends AsyncTask<InitializationListener, Void, InitializationListener> {
        @Override
        protected ApplicationManager.InitializationListener doInBackground(ApplicationManager.InitializationListener... listener) {

            // load in players, and player records
            players = new ArrayList<Player>();
            players.addAll(DatabaseManager.getInstance().fetchAllPlayers());

            return listener.length > 0 ? listener[0] : null;
        }

        @Override
        protected void onPostExecute(ApplicationManager.InitializationListener listener) {
            if (listener != null) {
                listener.onComplete();
            }
        }
    }
}
