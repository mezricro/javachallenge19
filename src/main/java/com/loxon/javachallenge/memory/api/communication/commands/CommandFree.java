package com.loxon.javachallenge.memory.api.communication.commands;

import com.loxon.javachallenge.memory.api.Player;
import com.loxon.javachallenge.memory.api.communication.general.CommandGeneral;

import java.util.List;

/**
 * Free cells.
 */
public class CommandFree extends CommandGeneral {
    public CommandFree(final Player player, final List<Integer> cells) {
        super(player, cells);
    }
}
