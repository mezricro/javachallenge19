package com.loxon.javachallenge.memory;

import com.loxon.javachallenge.memory.api.Game;
import com.loxon.javachallenge.memory.api.MemoryState;
import com.loxon.javachallenge.memory.api.Player;
import com.loxon.javachallenge.memory.api.communication.commands.CommandScan;
import com.loxon.javachallenge.memory.api.communication.general.Command;
import com.loxon.javachallenge.memory.api.communication.general.CommandGeneral;
import com.loxon.javachallenge.memory.api.communication.general.Response;
import org.junit.Assert;

import java.util.Arrays;
import java.util.List;

public class Main {
    final private static MemoryState F  = MemoryState.FREE;
    private final static MemoryState S  = MemoryState.SYSTEM;
    final static private MemoryState C  = MemoryState.CORRUPT;
    final private static MemoryState AX = MemoryState.ALLOCATED;
    private final static MemoryState AM = MemoryState.OWNED_ALLOCATED;
    final private static MemoryState FX = MemoryState.FORTIFIED;
    final static private MemoryState FM = MemoryState.OWNED_FORTIFIED;

    private final static int GAME_ROUNDS = 10;

    private static CommandScan scan( final Player player, final int cell ) {
        return new CommandScan(player, cell);
    }

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

        List<Response> resp = game.nextRound(scan(pA, 23));
        for (Response r : resp) {
            System.out.println(r);
        }
    }
}
