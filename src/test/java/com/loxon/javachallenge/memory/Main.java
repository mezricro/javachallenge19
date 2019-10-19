package com.loxon.javachallenge.memory;

import com.loxon.javachallenge.memory.api.Game;
import com.loxon.javachallenge.memory.api.MemoryState;
import com.loxon.javachallenge.memory.api.Player;
import org.junit.Assert;

import java.util.Arrays;

public class Main {
    private final static MemoryState F  = MemoryState.FREE;
    private final static MemoryState S  = MemoryState.SYSTEM;
    private final static MemoryState C  = MemoryState.CORRUPT;
    private final static MemoryState AX = MemoryState.ALLOCATED;
    private final static MemoryState AM = MemoryState.OWNED_ALLOCATED;
    private final static MemoryState FX = MemoryState.FORTIFIED;
    private final static MemoryState FM = MemoryState.OWNED_FORTIFIED;

    private final static int      GAME_ROUNDS = 10;


    public static void main(String[] args){
        Game game = new GameImplementation();
        Assert.assertNotNull("Game implementation is required.", game);
        Player pA = game.registerPlayer("a");
        Player pB = game.registerPlayer("b");
        game.startGame(Arrays.asList(
                F, F, F, F, F, S, S, S,
                F, F, F, F, F, F, F, F,
                F, F, F, F, F, F, F, F), GAME_ROUNDS);

        System.out.println(game.visualize());
    }
}
