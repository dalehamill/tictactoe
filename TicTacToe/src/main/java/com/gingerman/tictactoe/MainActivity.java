package com.gingerman.tictactoe;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Service;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.gingerman.tictactoe.fragments.GameFragment;
import com.gingerman.tictactoe.model.Game;
import com.gingerman.tictactoe.model.Player;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements GameFragment.OnGameListener {
    private static final String LOG_TAG = "MainActivity";
    private static final String GAME_FRAGMENT_TAG = "GameFragment";
    private static final String SPINNER_FRAGMENT_TAG = "SpinnerFragment";

    private ProgressBar mInitProgressBar = null;
    private ListView mResultsList = null;
    private Spinner mPlayer1Spinner = null;
    private Spinner mPlayer2Spinner = null;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new CreateGameFragment())
                    .commit();
        }

        mInitProgressBar = (ProgressBar) findViewById(R.id.container_progress);

        // Initialize our game logic manager
        ApplicationManager.getsInstance().initialize(this, new ApplicationManager.ApplicationManagerListener() {
            @Override
            public void onComplete() {
                Log.d(LOG_TAG, "ApplicationManager initialization is complete.");

                mInitProgressBar.setVisibility(View.GONE);

                mPlayer1Spinner = (Spinner) findViewById(R.id.player_1_spn);
                mPlayer2Spinner = (Spinner) findViewById(R.id.player_2_spn);
                refreshSpinnerListAdapters();

                mPlayer1Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        findViewById(R.id.new_player_1).setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                        if (id == mPlayer2Spinner.getSelectedItemId()) mPlayer1Spinner.setSelection(0, true);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        findViewById(R.id.new_player_1).setVisibility(View.VISIBLE);
                    }
                });
                mPlayer2Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        findViewById(R.id.new_player_2).setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                        if (id == mPlayer1Spinner.getSelectedItemId()) mPlayer2Spinner.setSelection(0, true);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        findViewById(R.id.new_player_2).setVisibility(View.VISIBLE);
                    }
                });

                final EditText p1txt = (EditText) findViewById(R.id.player_1_new);
                final EditText p2txt = (EditText) findViewById(R.id.player_2_new);

                Button playBtn = (Button) findViewById(R.id.playbtn);
                playBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {

                        // display a spinner to show we are thinking...
                        ((Button) v).setText("");
                        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.play_btn_progress);
                        progressBar.setVisibility(View.VISIBLE);

                        String player1Name = mPlayer1Spinner.getSelectedItemPosition() == 0 ? p1txt.getText().toString() :
                                (mPlayer1Spinner.getSelectedItem() == null ? null : mPlayer1Spinner.getSelectedItem().toString());
                        String player2Name = mPlayer2Spinner.getSelectedItemPosition() == 0 ? p2txt.getText().toString() :
                                (mPlayer2Spinner.getSelectedItem() == null ? null : mPlayer2Spinner.getSelectedItem().toString());

                        // clean up the keyboard, if it's open...since it's annoying
                        InputMethodManager imm = (InputMethodManager)getSystemService(Service.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

                        // Create a new game
                        ApplicationManager.getsInstance().createNewGame(player1Name, player2Name, new ApplicationManager.CreateGameListener() {
                            @Override
                            public void onComplete(Game game) {
                                // launch new game into game fragment
                                final Fragment fragment = GameFragment.newInstance(game);
                                final FragmentManager manager = getFragmentManager();
                                final FragmentTransaction transaction = manager.beginTransaction();

                                manager.popBackStack(GAME_FRAGMENT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                transaction.add(R.id.container, fragment, GAME_FRAGMENT_TAG);
                                transaction.addToBackStack(GAME_FRAGMENT_TAG);
                                transaction.commit();

                                // return button and spinner to pre-click positions
                                progressBar.setVisibility(View.GONE);
                                ((Button) v).setText(R.string.play);
                            }

                            @Override
                            public void onError(String msg) {
                                Log.e(LOG_TAG, msg);
                                Toast.makeText(getApplicationContext(), "Create game failed!", 1500).show();
                            }
                        });
                    }
                });

                mResultsList = (ListView) findViewById(R.id.results_list);
                List<Player> players = ApplicationManager.getsInstance().players;
                mResultsList.setAdapter(new ResultsListAdapter(players));
                // avoid keyboard popping up by putting focus on the list
                mResultsList.requestFocus();
            }

            @Override
            public void onError(String msg) {
                Log.e(LOG_TAG, msg);
            }
        });
    }

    @Override
    protected void onDestroy() {
        // Allow game logic manage to clean itself up
        ApplicationManager.getsInstance().destroy();
        super.onDestroy();
    }

    @Override
    public void onGameComplete(final Game game) {
        Log.d(LOG_TAG, "onGameComplete!");
        Toast.makeText(this, String.format("Game completed: %s",
                game.winner == null ? "Game ends in a Draw!" : game.winner.name+" wins!"
                ), 3000).show();

        // pop up our congrats screen (with spinner, while we update stats)
        final Fragment spinner = new SpinnerFragment();
        getFragmentManager().beginTransaction().add(R.id.container, spinner, SPINNER_FRAGMENT_TAG).commit();
        getFragmentManager().popBackStack(GAME_FRAGMENT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        ApplicationManager.getsInstance().gameCompleted(game, new ApplicationManager.ApplicationManagerListener() {
            @Override
            public void onComplete() {
                // update results
                mResultsList.setAdapter(new ResultsListAdapter(ApplicationManager.getsInstance().players));
                mResultsList.refreshDrawableState();
                refreshSpinnerListAdapters(game.player1.name, game.player2.name);

                // avoid keyboard popping up by putting focus on the list
                mResultsList.requestFocus();

                // close spinner
                getFragmentManager().beginTransaction().remove(spinner).commit();
            }

            @Override
            public void onError(String msg) {
                Log.e(LOG_TAG, msg);
            }
        });
    }

    @Override
    public void onGameQuit(Game game) {
        Log.d(LOG_TAG, "onGameQuit!");
        Toast.makeText(this, "Game quit: No winner declared!", 3000).show();
        getFragmentManager().popBackStack(GAME_FRAGMENT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    /**
     * Create game fragment
     */
    public static class CreateGameFragment extends Fragment {

        public CreateGameFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_create, container, false);
            return rootView;
        }
    }

    // Helper method to refresh both spinners
    private void refreshSpinnerListAdapters() {
        refreshSpinnerListAdapters(null, null);
    }
    private void refreshSpinnerListAdapters(String select1, String select2) {
        List<Player> players = ApplicationManager.getsInstance().players;
        final List<String> names = new ArrayList<String>();
        names.add("<Custom>");
        if (players != null) {
            for (Player player : players) {
                if (player != null) names.add(player.name);
            }
        }

        // Simple array adapter for all player names
        final ArrayAdapter<String> playerListAdapter = new ArrayAdapter<String>(
                getApplicationContext(),
                android.R.layout.simple_list_item_1,
                names);

        mPlayer1Spinner.setAdapter(playerListAdapter);
        mPlayer2Spinner.setAdapter(playerListAdapter);

        // select the last played players, if available.
        if (select1 != null) mPlayer1Spinner.setSelection(names.indexOf(select1));
        if (select2 != null) mPlayer2Spinner.setSelection(names.indexOf(select2));

        if (names.size() == 1) {
            mPlayer1Spinner.setVisibility(View.GONE);
            mPlayer2Spinner.setVisibility(View.GONE);
        } else {
            mPlayer1Spinner.setVisibility(View.VISIBLE);
            mPlayer2Spinner.setVisibility(View.VISIBLE);
        }
    }

    private class ResultsListAdapter extends BaseAdapter {
        private List<Player> mPlayers = null;

        public ResultsListAdapter(List<Player> players) {
            mPlayers = new ArrayList<Player>(players.size());
            mPlayers.addAll(players);
        }

        @Override
        public int getCount() {
            return mPlayers.size();
        }

        @Override
        public Player getItem(int position) {
            return mPlayers.get(position);
        }

        @Override
        public long getItemId(int position) {
            Player player = getItem(position);
            return player != null ? player.id : 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Player player = getItem(position);
            if (player == null) return null;

            LayoutInflater inflater = getLayoutInflater();
            final View resultRow = inflater.inflate(R.layout.result_row, null);

            final TextView playerName = (TextView) resultRow.findViewById(R.id.result_player_name);
            playerName.setText(player.name);
            final TextView playerStats = (TextView) resultRow.findViewById(R.id.result_player_stats);
            playerStats.setText(String.format("%s-%s-%s", player.wins, player.losses, player.draws));

            return resultRow;
        }
    }

    // custom built spinner dialog to display while we're thinking
    private class SpinnerFragment extends Fragment {
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.spinner, container, false);
        }
    }
}
