package com.loxon.javachallenge.memory.api.communication.commands;

import com.loxon.javachallenge.memory.api.Player;
import com.loxon.javachallenge.memory.api.communication.general.Command;

/**
 * Get memory statistics.
 */
public class CommandStats extends Command {

    public CommandStats(final Player player) {
        super(player);
    }

}
