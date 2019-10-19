package com.loxon.javachallenge.memory.api.communication.commands;

import com.loxon.javachallenge.memory.api.Player;
import com.loxon.javachallenge.memory.api.communication.general.CommandGeneral;

import java.util.List;

/**
 * Swap two cells.
 */
public class CommandSwap extends CommandGeneral {
    public CommandSwap(final Player player, final List<Integer> cells) {
        super(player, cells);
    }
}
