package com.loxon.javachallenge.memory.api.communication.general;

import com.loxon.javachallenge.memory.api.Player;

/**
 * Base class for responses.
 */
public abstract class Response {
    private Player player;

    public Response(final Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
