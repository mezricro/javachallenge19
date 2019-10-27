package com.loxon.javachallenge.memory;

import com.loxon.javachallenge.memory.api.MemoryState;
import com.loxon.javachallenge.memory.api.Player;
import javafx.util.Pair;

import java.lang.reflect.Array;
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
    private static HashMap<Integer, List<Integer>> swapHistory = new HashMap<>();
    public static void clearSwapHistory() {
        swapHistory.clear();
    }

    // a cella koronket max 1x irhato, egyebkent
    // korruptalodik
    private boolean wasWritten = false;
    private boolean canWrite(boolean updateStatus) {
        boolean cantWrite =
                state == MemoryState.SYSTEM ||
                state == MemoryState.FORTIFIED ||
                wasWritten;

        if (cantWrite && wasWritten) {
            this.state = MemoryState.CORRUPT;
        }

        if (updateStatus) {
            this.wasWritten = true;
        }

        return !cantWrite;
    }

    private boolean canWrite() {
        return canWrite(false);
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

    public static void swap(final Integer id1, final Integer id2, Cell[] grid) {
        List<Integer> history;

        // egyik cella id-je a masik elozmenyei koze...
        history = swapHistory
                .putIfAbsent(id1, new ArrayList<>(
                        Collections.singletonList(id2)));
        if (history != null) history.add(id2);

        // ...es a masik cella id-je az egyik elozmenyei koze,
        // igy id1 -> id2 es id2 -> id1
        history = swapHistory
                .putIfAbsent(id2, new ArrayList<>(
                        Collections.singletonList(id1)));
        if (history != null) history.add(id1);


        Cell c1 = grid[id1];
        Cell c2 = grid[id2];

        if (c1.canWrite(true) && c2.canWrite(true)) {
            grid[id1] = c2;
            grid[id2] = c1;

            c2.id = id1;
            c1.id = id2;
        } else {
//            System.out.println("\nCorrupting...");
//            swapHistory.forEach((k, v) -> System.out.println(k + ", " + v));

            Cell.corruptSwap(id1, grid);
            Cell.corruptSwap(id2, grid);
        }
    }

    private static void corruptSwap(Integer start, Cell[] grid) {
        // egy kiindulasi cellatol kezdve...
        Cell startCell = grid[start];
        startCell.state = MemoryState.CORRUPT;
        startCell.failedSwap = true;

//        System.out.println("Corrupting " + startCell);

        // ...minden olyan cellat elrontunk, amihez koze volt
        List<Integer> toCorrupt = swapHistory.get(start);
        swapHistory.remove(start);

        if (toCorrupt != null) {
            for (Integer id : toCorrupt) {
                Cell target = grid[id];

                // csak azokat, amik meg nem lettek elrontva
                if (!target.failedSwap) {
//                    System.out.println("Corrupting " + target);
                    target.state = MemoryState.CORRUPT;
                    target.failedSwap = true;
                }
            }
        }
    }

    @Deprecated
    private void corruptSwap(Cell[] grid) {
        System.out.println("Corrupting " + this);

        this.state = MemoryState.CORRUPT;
        this.failedSwap = true;

        List<Integer> toCorrupt = swapHistory.get(this.id);

        if (toCorrupt != null) {
            swapHistory.remove(this.id);

            for (Integer id : toCorrupt) {
                Cell target = grid[id];

                // csak azokat, amik meg nem lettek elrontva
                if (!target.failedSwap) {
                    target.corruptSwap(grid);
                }
            }
        }
    }

    public boolean successfulySwapped() {
        return !failedSwap;
    }

    private static char convertState(MemoryState state) {
        char stateChar = '?';
        switch(state) {
            case FREE:
            case FORTIFIED:
                stateChar =  'F';
                break;

            case SYSTEM:
                stateChar = 'S';
                break;

            case CORRUPT:
                stateChar = 'C';
                break;

            case ALLOCATED:
                stateChar = 'A';
                break;
        }
        return stateChar;
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

        String str;
        if (state == MemoryState.SYSTEM) {
            str = "(SYS )";
        } else if (state == MemoryState.FREE) {
            str = "(FREE)";
        } else {
            str = "(" + Cell.convertState(state) + ":" + playerId + ")";
        }

        return str + " @ " + id;
    }

    public String toString(int minLength) {
        String str = this.toString();

        if (str.length() < minLength) {
            char[] padding = new char[minLength - str.length()];
            for (int i = 0; i < padding.length; ++i) {
                padding[i] = ' ';
            }
            str += new String(padding);
        }

        return str;
    }
}
