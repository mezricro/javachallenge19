package com.loxon.javachallenge.memory;

import com.loxon.javachallenge.memory.api.Player;
import com.loxon.javachallenge.memory.api.PlayerScore;

public class RegisteredPlayer extends Player {
    public boolean canPlay = true;
    private PlayerScore score;

    public RegisteredPlayer(String name) {
        super(name);
        score = new PlayerScore(this);
    }

    public PlayerScore getScore() {
        return score;
    }
}
