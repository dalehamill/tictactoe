package com.gingerman.tictactoe.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a game being played between two players, possibly with a winner declared
 */
public class Game implements Parcelable {
    public Player player1;
    public Player player2;
    public Player winner = null;

    public Game(Player gamePlayer1, Player gamePlayer2) {
        player1 = gamePlayer1;
        player2 = gamePlayer2;
    }

    public void setWinner(Player winningPlayer) {
        winner = winningPlayer;
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
    }
}
