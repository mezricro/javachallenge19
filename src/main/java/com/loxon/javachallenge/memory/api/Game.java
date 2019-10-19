package com.loxon.javachallenge.memory.api;

import com.loxon.javachallenge.memory.api.communication.general.Command;
import com.loxon.javachallenge.memory.api.communication.general.Response;

import java.util.List;

/**
 * Memory-allocation game.
 */
public interface Game {

    /**
     * Registers a player to the game.
     * @param name Name of the player.
     * @return a new player instance
     */
    Player registerPlayer(final String name);

    /**
     * Strats the game.
     * @param initialMemory initial state of memory.
     * @param rounds number of round to be played.
     */
    void startGame(List<MemoryState> initialMemory, final int rounds);

    /**
     * Executes a game round by applying the given commands.
     * @param requests list of commands to execute.
     * @return responses for the given commands.
     */
    List<Response> nextRound(final Command... requests);

    /**
     * Calculates the score-related values for all the registered players.
     * @return list of player scores.
     */
    List<PlayerScore> getScores();

    /**
     * Creates a string visualization of the game-state.
     * @return string to be written on the console during testing.
     */
    String visualize();
}
