package com.loxon.javachallenge.memory.api.communication.commands;

import com.loxon.javachallenge.memory.api.Player;
import com.loxon.javachallenge.memory.api.communication.general.CommandGeneral;

import java.util.List;

/**
 * Allocate cells to given player.
 */
public class CommandAllocate extends CommandGeneral {
    public CommandAllocate(final Player player, final List<Integer> cells) {
        super(player, cells);
    }
}
