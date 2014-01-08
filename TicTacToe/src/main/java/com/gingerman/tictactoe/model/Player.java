package com.gingerman.tictactoe.model;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a Player in memory
 */
public class Player implements Parcelable {
    public int id;
    public String name;
    public int wins;
    public int losses;
    public int draws;

    public interface DB_FIELDS {
        public static final String tableName = "player";
        public static final String id = "id";
        public static final String name = "name";
        public static final String wins = "wins";
        public static final String losses = "losses";
        public static final String draws = "draws";
    }

    public Player(int playerId, String playerName) {
        id = playerId;
        name = playerName;
    }

    public Player(Cursor cursor) {
        if (cursor == null) return;

        id = cursor.getInt(cursor.getColumnIndex(DB_FIELDS.id));
        name = cursor.getString(cursor.getColumnIndex(DB_FIELDS.name));
        wins = cursor.getInt(cursor.getColumnIndex(DB_FIELDS.wins));
        losses = cursor.getInt(cursor.getColumnIndex(DB_FIELDS.losses));
        draws = cursor.getInt(cursor.getColumnIndex(DB_FIELDS.draws));
    }

    public void updateStats(int playerWins, int playerLosses, int playerDraws) {
        wins = playerWins;
        losses = playerLosses;
        draws = playerDraws;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeInt(wins);
        dest.writeInt(losses);
        dest.writeInt(losses);
    }
}
