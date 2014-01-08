package com.gingerman.tictactoe.model;

import android.content.ContentValues;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.gingerman.tictactoe.fragments.GameFragment;

/**
 * Represents a game being played between two players, possibly with a winner declared
 */
public class Game implements Parcelable {
    private static final String LOG_TAG = "Game";

    public Player player1;
    public Player player2;
    public Player winner = null;
    public Player current;

    private int[] gameBoardState = new int[9];
    private Bitmap xMark;
    private Bitmap oMark;

    private static final int PLAYER_1_MARK = 1;
    private static final int PLAYER_2_MARK = 2;

    public interface DB_FIELDS {
        public static final String tableName = "game";
        public static final String id = "id";
        public static final String player1 = "player_1";
        public static final String player2 = "player_2";
        public static final String winner = "winner";
    }

    /**
     * Construct a new game with empty game state
     * @param gamePlayer1 player 1 (x)
     * @param gamePlayer2 player 2 (o)
     * @param xBmp bitmap to use as x player's mark
     * @param oBmp bitmap to use as o player's mark
     */
    public Game(Player gamePlayer1, Player gamePlayer2, Bitmap xBmp, Bitmap oBmp) {
        player1 = gamePlayer1;
        player2 = gamePlayer2;
        current = player1;
        xMark = xBmp;
        oMark = oBmp;
    }

    /**
     * @param position position to be queried
     * @return the image to load into this position, marking whether or not the spot is taken
     */
    public Bitmap getImageForGamePosition(int position) {
        if (position >= gameBoardState.length || position < 0) return null;
        return gameBoardState[position] == PLAYER_1_MARK ? xMark :
                (gameBoardState[position] == PLAYER_2_MARK ? oMark : null);
    }

    /**
     * @return the icon for the player who's turn it is
     */
    public Bitmap getImageForCurrentPlayer() {
        return current == player1 ? xMark : oMark;
    }

    /**
     * Implements a move (claiming a location in the game map)
     * @param position position that the player wants to claim
     * @param listener allows callback if game is complete
     * @return the bitmap to claim the spot, if move was legal, or null if it was illegal
     */
    public Bitmap claimGamePosition(int position, GameFragment.OnGameListener listener) {
        if (position >= gameBoardState.length || position < 0 || checkCompleteness()) return null;

        if (gameBoardState[position] > 0) return null; // illegal move, already claimed
        gameBoardState[position] = current == player1 ? PLAYER_1_MARK : PLAYER_2_MARK;
        Bitmap bmp = getImageForCurrentPlayer();

        // is the game over, or shall we continue
        if (checkCompleteness()) {
            Log.d(LOG_TAG, "game complete, prompting listener");
            if (listener != null) listener.onGameComplete(this);
        } else {
            current = current == player1 ? player2 : player1; // new players turn
        }
        return bmp;
    }

    public boolean checkCompleteness() {
        // wins occur if three in a row, so check all cases
        if (positionStatus(0,1,2) ||
                positionStatus(3,4,5) ||
                positionStatus(6,7,8) ||
                positionStatus(0,3,6) ||
                positionStatus(1,4,7) ||
                positionStatus(2,5,8) ||
                positionStatus(0,4,8) ||
                positionStatus(2,4,6)) {
            winner = current;
            if (winner == player1) {
                player1.wins++;
                player2.losses++;
            } else {
                player1.losses++;
                player2.wins++;
            }
            return true; // win has occurred
        }
        for (int state : gameBoardState) {
            if (state == 0) return false; // at least one empty spot left
        }
        player1.draws++;
        player2.draws++;
        return true; // game board is full
    }

    /**
     * @return ContentValues representing this object
     */
    public ContentValues getSerializedValues() {
        ContentValues values = new ContentValues(3);
        values.put(DB_FIELDS.player1, player1.id);
        values.put(DB_FIELDS.player2, player2.id);
        if (winner != null) values.put(DB_FIELDS.winner, winner.id);

        return values;
    }

    /**
     * @param p1 first position
     * @param p2 second position
     * @param p3 third position
     * @return if position's are owned by the same user. positions must be in scope of position array.
     */
    private boolean positionStatus(int p1, int p2, int p3) {
        int v1 = gameBoardState[p1];
        return v1 > 0 && v1 == gameBoardState[p2] && v1 == gameBoardState[p3];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(player1, 0);
        dest.writeParcelable(player2, 0);
        dest.writeParcelable(winner, 0);
        dest.writeIntArray(gameBoardState);
    }
}
