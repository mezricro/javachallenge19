package com.loxon.javachallenge.memory.api.communication.commands;

import com.loxon.javachallenge.memory.api.Player;
import com.loxon.javachallenge.memory.api.communication.general.Response;

import java.util.List;
import java.util.Objects;

/**
 * General response for cases when response is a list of cell indexes.
 */
public class ResponseSuccessList extends Response {
    private List<Integer> successCells;

    public ResponseSuccessList(final Player player, final List<Integer> successCells) {
        super(player);
        this.successCells = successCells;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ResponseSuccessList that = (ResponseSuccessList) o;
        return Objects.equals(successCells, that.successCells);
    }

    @Override
    public int hashCode() {
        return Objects.hash(successCells);
    }

    @Override
    public String toString() {
        return "ResponseSuccessList{" +
                "successCells=" + successCells +
                '}';
    }
}
