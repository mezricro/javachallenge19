package com.loxon.javachallenge.memory.api.communication.commands;

import com.loxon.javachallenge.memory.api.MemoryState;
import com.loxon.javachallenge.memory.api.Player;
import com.loxon.javachallenge.memory.api.communication.general.Response;

import java.util.List;
import java.util.Objects;

/**
 * Response for @{@link CommandScan} command.
 */
public class ResponseScan extends Response {
    private int firstCell;
    private List<MemoryState> states;

    public ResponseScan(final Player player, final int firstCell, final List<MemoryState> states) {
        super(player);
        this.firstCell = firstCell;
        this.states = states;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ResponseScan that = (ResponseScan) o;
        return firstCell == that.firstCell &&
                Objects.equals(states, that.states);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstCell, states);
    }

    @Override
    public String toString() {
        return "ResponseScan{" +
                "firstCell=" + firstCell +
                ", states=" + states +
                '}';
    }
}
