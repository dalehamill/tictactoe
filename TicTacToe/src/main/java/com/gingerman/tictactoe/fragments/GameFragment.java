package com.gingerman.tictactoe.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gingerman.tictactoe.R;
import com.gingerman.tictactoe.model.Game;
import com.gingerman.tictactoe.model.Player;

/**
 * Implements the fragment holding the actual game board, and allowing for play of the game itself
 */
public class GameFragment extends Fragment {
    private static final String LOG_TAG = "GameFragment";

    private Game mGame = null;
    private OnGameListener mListener = null;

    private TextView mTitleText;
    private ImageView mTitleImage;

    // Container Activity must implement this interface (allows us to communicate back)
    public interface OnGameListener {
        public void onGameComplete(Game game);
        public void onGameQuit(Game game);
    }

    // central definition of bundle ids to avoid confusion
    private static interface BUNDLE_IDS {
        public static final String game = "game";
    }

    /**
     * Static method to create a new instance of the game fragment
     * @param game incoming game to display/play
     * @return a new GameFragment with provided state
     */
    public static GameFragment newInstance(Game game) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(BUNDLE_IDS.game, game);
        GameFragment fragment = new GameFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_game, container, false);
        if (view != null && mGame != null) {
            mTitleText = (TextView) view.findViewById(R.id.game_turn_txt);
            mTitleImage = (ImageView) view.findViewById(R.id.game_turn_img);
            updateTitle();

            // display game state
            int[] gamePieceIds = {R.id.game_loc_1, R.id.game_loc_2, R.id.game_loc_3,
                    R.id.game_loc_4, R.id.game_loc_5, R.id.game_loc_6,
                    R.id.game_loc_7, R.id.game_loc_8, R.id.game_loc_9};
            for (int i = 0; i < gamePieceIds.length; ++i) {
                final int position = i;
                final ImageButton btn = (ImageButton) view.findViewById(gamePieceIds[i]);
                btn.setImageBitmap(mGame.getImageForGamePosition(i));
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Bitmap bmp = mGame.claimGamePosition(position);
                        if (bmp == null) Toast.makeText(getActivity(), R.string.illegal_move, 2500).show();
                        else {
                            btn.setImageBitmap(bmp);
                            updateTitle();
                        }
                        // check if the game is complete
                        mGame.checkCompleteness(mListener);
                    }
                });
            }

            Button quitBtn = (Button) view.findViewById(R.id.quit_game_btn);
            quitBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mListener != null) mListener.onGameQuit(mGame);
                }
            });
        }

        return view;
    }

    private void updateTitle() {
        mTitleText.setText(String.format("%s's turn!", mGame.current.name));
        mTitleImage.setImageBitmap(mGame.getImageForCurrentPlayer());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle bundle = getArguments();
        if (bundle == null) return;

        mGame = bundle.getParcelable(BUNDLE_IDS.game);
    }

    @Override
    public void onResume() {
        super.onResume();

        Activity activity = getActivity();
        if (activity.getCurrentFocus() != null) {
            InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mListener = (OnGameListener) activity;
            Log.d(LOG_TAG, "game listener set on game fragment");
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnGameListener");
        }
    }
}
