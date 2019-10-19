package com.loxon.javachallenge.memory.api.communication.general;

import com.loxon.javachallenge.memory.api.Player;

/**
 * Base of all handled commands.
 */
public abstract class Command {
    private Player player;

    public Command( final Player player ) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
