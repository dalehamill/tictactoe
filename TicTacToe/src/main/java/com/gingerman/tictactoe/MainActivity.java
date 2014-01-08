package com.gingerman.tictactoe;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
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

                List<Player> players = ApplicationManager.getsInstance().players;
                List<String> names = new ArrayList<String>();
                if (players != null) {
                    for (Player player : players) {
                        if (player != null) names.add(player.name);
                    }
                }

                // Simple array adapter for all player names
                ArrayAdapter<String> playerListAdapter = new ArrayAdapter<String>(
                        getApplicationContext(),
                        android.R.layout.simple_list_item_1,
                        names);

                final Spinner spin1 = (Spinner) findViewById(R.id.player_1_spn);
                spin1.setAdapter(playerListAdapter);
                final Spinner spin2 = (Spinner) findViewById(R.id.player_2_spn);
                spin2.setAdapter(playerListAdapter);
                if (names.size() == 0) {
                    spin1.setVisibility(View.GONE);
                    spin2.setVisibility(View.GONE);
                }

                final EditText p1txt = (EditText) findViewById(R.id.player_1_new);
                final EditText p2txt = (EditText) findViewById(R.id.player_2_new);

                Button playBtn = (Button) findViewById(R.id.playbtn);
                playBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String player1Name = p1txt.getText().toString();
                        if (player1Name.length() == 0 && spin1.getVisibility() == View.VISIBLE) {
                            player1Name = spin1.getSelectedItem().toString();
                        }
                        String player2Name = p2txt.getText().toString();
                        if (player2Name.length() == 0 && spin1.getVisibility() == View.VISIBLE) {
                            player2Name = spin1.getSelectedItem().toString();
                        }

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

        // TODO update results

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

}
