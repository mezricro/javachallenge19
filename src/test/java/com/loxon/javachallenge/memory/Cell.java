package com.loxon.javachallenge.memory;

import com.loxon.javachallenge.memory.api.MemoryState;
import com.loxon.javachallenge.memory.api.Player;

import java.util.*;

public class Cell {
    public Cell(final Integer id, final MemoryState state,  final Player owner) {
        this.owner = owner;
        this.state = state;
        this.id = id;
    }

    public Cell(final Integer id, final MemoryState state) {
        this(id, state, null);
    }

    private Player owner;
    private MemoryState state;
    private Integer id;

    // nem kell swap lista, helyette ez a flag
    private boolean failedSwap = false;

    // eleg csak a cella id-kat es a veluk cserelni probalt cellak
    // id-jat tarolni
    private static HashMap<Integer, Set<Integer>> swapHistory = new HashMap<>();
    public static void clearSwapHistory() {
        swapHistory.clear();
    }

    // a cella koronket max 1x irhato, egyebkent
    // korruptalodik
    private boolean wasWritten = false;
    private boolean canWrite() {
        boolean cantWrite =
                state == MemoryState.SYSTEM ||
                state == MemoryState.FORTIFIED ||
                wasWritten;

        if (cantWrite && wasWritten) {
            this.state = MemoryState.CORRUPT;
        }

        return !cantWrite;
    }

    private boolean canWrite(boolean updateStatus) {
        boolean canWrite = canWrite();
        if (updateStatus) { wasWritten = true; }
        return canWrite;
    }

    public void resetWrites() {
        failedSwap = false;
        wasWritten = false;
    }

    // jatekostol fugg a fortified es allocated statusz
    public MemoryState getState(Player p) {
        MemoryState retVal = state;

        if (owner != null && owner == p) {
            if (state == MemoryState.FORTIFIED)
                retVal = MemoryState.OWNED_FORTIFIED;

            if (state == MemoryState.ALLOCATED)
                retVal = MemoryState.OWNED_ALLOCATED;
        }

        return retVal;
    }

    public MemoryState getState() {
        return this.getState(null);
    }

    public Player getOwner() { return owner; }

    public Integer getId() { return id; }

    public int getBlock() { return id / 4; }

    public void allocate(Player p) {
        if (canWrite() &&
            state == MemoryState.FREE) {

            state = MemoryState.ALLOCATED;
            owner = p;

            wasWritten = true;
        }
    }

    public void free() {
        if (canWrite() &&
            (state == MemoryState.ALLOCATED ||
             state == MemoryState.CORRUPT)) {

            state = MemoryState.FREE;
            owner = null;

            wasWritten = true;
        }
    }

    public void recover(Player p) {
        if (canWrite()) {
            switch (state) {
                case CORRUPT:
                    state = MemoryState.ALLOCATED;
                    owner = p;
                    break;

                case ALLOCATED:
                case FREE:
                    state = MemoryState.CORRUPT;
                    break;
            }

            wasWritten = true;
        }
    }

    public boolean fortify() {
        boolean canFortify =
                canWrite() &&
                state == MemoryState.ALLOCATED;

        if (canFortify) {
            state = MemoryState.FORTIFIED;
        }

        return canFortify;
    }

    // csereljuk ki oket az arrayben + az id-juket
    // igy nem kell tagonket masolgatni (ha meg esetleg
    // adnank hozza fieldeket), viszont kozvetlenul
    // bele kell nyulni a cella arraybe :/
    @Deprecated
    public void swap2(final Integer targetID, Cell[] grid) {
        Cell c = grid[targetID];

        Set<Integer> history = swapHistory
                .putIfAbsent(this.id, new HashSet<>(
                        Collections.singletonList(targetID)));

        if (history != null) history.add(targetID);

        if (canWrite(true) && c.canWrite(true)) {
            grid[targetID] = this;
            grid[this.id] = c;

            c.id = this.id;
            this.id = targetID;
        } else {
            c.corruptSwap(grid);
            this.corruptSwap(grid);
        }
    }

    public static void swap(
            final Integer id1,
            final Integer id2,
            Cell[] grid) {

        Set<Integer> history = swapHistory
                .putIfAbsent(id1, new HashSet<>(
                        Collections.singletonList(id2)));

        if (history != null) history.add(id2);

        Cell c1 = grid[id1];
        Cell c2 = grid[id2];

        if (c1.canWrite(true) && c2.canWrite(true)) {
            grid[id1] = c2;
            grid[id2] = c1;

            c2.id = id1;
            c1.id = id2;
        } else {
            c1.corruptSwap(grid);
            c2.corruptSwap(grid);
        }
    }

    private void corruptSwap(Cell[] grid) {
        this.state = MemoryState.CORRUPT;
        this.failedSwap = true;

        Set<Integer> corrupt = swapHistory.get(this.id);

        if (corrupt != null) {
            swapHistory.remove(this.id);

            for (Integer id : corrupt) {
                grid[id].corruptSwap(grid);
            }
        }
    }

    public boolean successfulySwapped() {
        return !failedSwap;
    }

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

    @Override
    public String toString() {
        String playerId;
        if (owner != null) {
            String ownerName = owner.getName();
            switch (ownerName.length()) {
                case 0:
                    playerId = " ?";
                    break;

                case 1:
                    playerId = ' ' + ownerName.substring(0, 1);
                    break;

                default:
                    playerId = ownerName.substring(0, 2);
                    break;
            }
        } else {
            playerId = "--";
        }

        if (state == MemoryState.SYSTEM) {
            return id + ": (SYS )";
        } else if (state == MemoryState.FREE) {
            return id + ": (FREE)";
        } else {
            return id + ": (" + Cell.convertState(state) + ":" + playerId + ")";
        }
    }
}
