package com.loxon.javachallenge.memory.api.communication.commands;

import com.loxon.javachallenge.memory.api.Player;
import com.loxon.javachallenge.memory.api.communication.general.CommandGeneral;

import java.util.List;

/**
 * Fortify cells.
 */
public class CommandFortify extends CommandGeneral {
    public CommandFortify(final Player player, final List<Integer> cells) {
        super(player, cells);
    }
}
