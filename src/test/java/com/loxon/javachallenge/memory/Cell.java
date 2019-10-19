package com.loxon.javachallenge.memory;

import com.loxon.javachallenge.memory.api.MemoryState;
import com.loxon.javachallenge.memory.api.Player;
import jdk.nashorn.internal.codegen.MethodEmitter;

public class Cell {
    public Cell(Integer id, MemoryState state,  Player owner) {
        this.owner = owner;
        this.state = state;
        this.id = id;
    }

    private Player owner;
    private MemoryState state;
    private Integer id;

    // a cella koronket max 1x irhato, egyebkent
    // korruptalodik
    private int writeCount = 0;
    public boolean validWrite() {
        return writeCount < 2;
    }
    boolean canWrite() {
        if (state == MemoryState.SYSTEM ||
            state == MemoryState.FORTIFIED) {

            return false;
        }

        if (writeCount != 0) {
            this.state = MemoryState.CORRUPT;
            return false;
        }
        else {
            writeCount++;
            return true;
        }
    }
    void resetWrites() {
        writeCount = 0;
    }

    // jatekostol fugg a fortified es allocated statusz
    public MemoryState getState(Player p) {
        switch (state) {
            case FORTIFIED:
                if (owner == p) {
                    return MemoryState.OWNED_FORTIFIED;
                }
                else {
                    return MemoryState.FORTIFIED;
                }

            case ALLOCATED:
                if (owner == p) {
                    return MemoryState.OWNED_ALLOCATED;
                }
                else {
                    return MemoryState.ALLOCATED;
                }

            default:
                return state;
        }
    }

    public MemoryState getState() {
        return this.getState(null);
    }

    public Player getOwner() { return owner; }

    public Integer getId() { return id; }

    public int getBlock() { return id / 4; }

    public void allocate(Player p) {
        if (!canWrite()) return;

        if (state == MemoryState.FREE) {
            state = MemoryState.ALLOCATED;
            owner = p;
        }
    }

    public void free() {
        if (!canWrite()) return;

        if (state == MemoryState.ALLOCATED ||
            state == MemoryState.CORRUPT) {

            state = MemoryState.FREE;
            owner = null;
        }
    }

    public void recover(Player p) {
        if (!canWrite()) return;

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
    }


    // fortify 2 lepesben:
    // 1., fortify igeny
    // 2., kor vegen meg kell nezni, mas nem e
    //     irt a cellaba (ha igen --> korrupt)
    boolean isFortifying = false;
    public void beginFortify() {
        isFortifying = true;
    }

    public boolean finishFortify() {
        if (canWrite() &&
            isFortifying &&
            state == MemoryState.ALLOCATED) {

            state = MemoryState.FORTIFIED;
            return true;
        }

        return false;
    }



    public void swap(Cell c) {
        //TODO ez mifaszt csinal??
    }

    public boolean hasOwner() {
        return owner != null;
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
        if (hasOwner()) {
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
            return "(SYS )";
        } else if (state == MemoryState.FREE) {
            return "(FREE)";
        } else {
            return "(" + Cell.convertState(state) + ":" + playerId + ")";
        }
    }
}
