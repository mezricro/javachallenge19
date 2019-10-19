package com.loxon.javachallenge.memory;

import com.loxon.javachallenge.memory.api.MemoryState;
import com.loxon.javachallenge.memory.api.Player;

public class Cell {
    public Player owner;
    public MemoryState state;
    public Integer id;
    public int block;

    private static char convertState(MemoryState state) {
        switch(state) {
            case FREE: return 'F';
            case FORTIFIED: return '#';
            case SYSTEM: return 'S';
            case CORRUPT: return 'C';
            case ALLOCATED: return 'A';
        }
        return '?';
    }

    public boolean hasOwner() {
        return owner != null;
    }


    @Override
    public String toString() {
        String playerName;
        if (hasOwner()) {
            playerName = owner.getName().substring(0, 2);
        } else {
            playerName = "--";
        }
        if (state == MemoryState.SYSTEM) {
            return "(SYS )";
        } else if (state == MemoryState.FREE) {
            return "(FREE)";
        } else {
            return "(" + Cell.convertState(state) + ":" + playerName + ")";
        }
    }
}
