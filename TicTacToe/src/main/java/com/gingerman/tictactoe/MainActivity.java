package com.gingerman.tictactoe;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Service;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
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

    private ListView mResultsList = null;
    private Spinner mPlayer1Spinner = null;
    private Spinner mPlayer2Spinner = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new CreateGameFragment())
                    .commit();
        }

        // Initialize our game logic manager
        ApplicationManager.getsInstance().initialize(this, new ApplicationManager.InitializationListener() {
            @Override
            public void onComplete() {
                Log.d(LOG_TAG, "ApplicationManager initialization is complete.");

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
                    public void onClick(View v) {
                        String player1Name = mPlayer1Spinner.getSelectedItemPosition() == 0 ? p1txt.getText().toString() :
                                (mPlayer1Spinner.getSelectedItem() == null ? null : mPlayer1Spinner.getSelectedItem().toString());
                        String player2Name = mPlayer2Spinner.getSelectedItemPosition() == 0 ? p2txt.getText().toString() :
                                (mPlayer2Spinner.getSelectedItem() == null ? null : mPlayer2Spinner.getSelectedItem().toString());

                        // clean up the keyboard, if it's open...since it's annoying
                        InputMethodManager imm = (InputMethodManager)getSystemService(Service.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

                        // Create a new game
                        Game newGame = ApplicationManager.getsInstance().createNewGame(
                                player1Name, player2Name);
                        // launch new game into game fragment
                        final Fragment fragment = GameFragment.newInstance(newGame);
                        final FragmentManager manager = getFragmentManager();
                        final FragmentTransaction transaction = manager.beginTransaction();

                        manager.popBackStack(GAME_FRAGMENT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        transaction.add(R.id.container, fragment, GAME_FRAGMENT_TAG);
                        transaction.addToBackStack(GAME_FRAGMENT_TAG);
                        transaction.commit();
                    }
                });

                mResultsList = (ListView) findViewById(R.id.results_list);
                List<Player> players = ApplicationManager.getsInstance().players;
                mResultsList.setAdapter(new ResultsListAdapter(players));
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
    public void onGameComplete(Game game) {
        Log.d(LOG_TAG, "onGameComplete!");
        Toast.makeText(this, String.format("Game completed: %s",
                game.winner == null ? "Game ends in a Draw!" : game.winner.name+" wins!"
                ), 3000).show();
        ApplicationManager.getsInstance().gameCompleted(game);

        // update results
        mResultsList.setAdapter(new ResultsListAdapter(ApplicationManager.getsInstance().players));
        refreshSpinnerListAdapters();

        getFragmentManager().popBackStack(GAME_FRAGMENT_TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
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

    private void refreshSpinnerListAdapters() {
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

        if (names.size() == 1) {
            mPlayer1Spinner.setVisibility(View.INVISIBLE);
            mPlayer2Spinner.setVisibility(View.INVISIBLE);
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
}
