package com.loxon.javachallenge.memory;

import com.loxon.javachallenge.memory.api.Game;
import com.loxon.javachallenge.memory.api.MemoryState;
import com.loxon.javachallenge.memory.api.Player;
import com.loxon.javachallenge.memory.api.PlayerScore;
import com.loxon.javachallenge.memory.api.communication.general.Command;
import com.loxon.javachallenge.memory.api.communication.general.Response;

import java.util.ArrayList;
import java.util.List;

public class GameImplementation implements Game {
    List<Player> players = new ArrayList<>();
    Cell[] cells;

    int maxRounds;
    int currentRound;

    public GameImplementation() {

    }

    @Override
    public Player registerPlayer(String name) {
        Player p = new Player(name);
        players.add(p);
        return p;
    }

    @Override
    public void startGame(List<MemoryState> initialMemory, int rounds) {
        if (rounds <= 0) {
            throw new IllegalArgumentException("Number of rounds must greater than zero.");
        }

        maxRounds = rounds;
        currentRound = 0;

        cells = new Cell[initialMemory.size()];
        for (int i = 0; i < initialMemory.size(); ++i){
            Cell c = new Cell();
            c.id = i;
            c.block = (i / 4);
            c.owner = null;
            c.state = initialMemory.get(i);
            cells[i] = c;
        }
    }

    @Override
    public List<Response> nextRound(Command... requests) {
        return null;
    }


    @Override
    public List<PlayerScore> getScores() {
        return null;
    }

    @Override
    public String visualize() {
        String vis = "";
        for (Player p : players) {
            vis += "Player: " + p.getName() + '\n';
        }


        vis += "\n[";
        for (int i = 0; i < cells.length; ++i){
            vis += cells[i].toString();
            if (i % 4 == 3) {
                vis += "]\n[";
            }
        }

        vis = vis.substring(0, vis.length() - 1);

        vis += "\nmax rounds: " + maxRounds + '\n';
        vis += "current round: " + currentRound;

        return vis;
    }
}
