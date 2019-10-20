package com.loxon.javachallenge.memory;

import com.loxon.javachallenge.memory.api.Player;

public class RegisteredPlayer extends Player {
    public boolean canPlay = true;

    public RegisteredPlayer(String name) {
        super(name);
    }
}
