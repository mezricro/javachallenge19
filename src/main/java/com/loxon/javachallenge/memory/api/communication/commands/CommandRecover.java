package com.loxon.javachallenge.memory.api.communication.commands;

import com.loxon.javachallenge.memory.api.Player;
import com.loxon.javachallenge.memory.api.communication.general.CommandGeneral;

import java.util.List;

/**
 * Recover cells.
 */
public class CommandRecover extends CommandGeneral {
    public CommandRecover(final Player player, final List<Integer> cells) {
        super(player, cells);
    }
}
