package com.loxon.javachallenge.memory;

import com.loxon.javachallenge.memory.api.MemoryState;
import com.loxon.javachallenge.memory.api.Player;

import java.util.HashSet;
import java.util.Set;

public class Cell {
    public Cell(Integer id, MemoryState state,  Player owner) {
        this.owner = owner;
        this.state = state;
        this.id = id;
    }

    private Player owner;
    private MemoryState state;
    private Integer id;
    private boolean isFortifying = false;

    private Set<Cell> swapHistory = new HashSet<>();
    private static Set<Cell> swapCorruptions = new HashSet<>();

    public static void clearSwapHistory() {
        swapCorruptions.clear();
    }

    // a cella koronket max 1x irhato, egyebkent
    // korruptalodik
    private int writeCount = 0;
    public boolean validWrite() {
        return writeCount < 2;
    }

    private boolean isPermanent() {
        return state == MemoryState.SYSTEM ||
               state == MemoryState.FORTIFIED;
    }

    private boolean canWrite() {
        if (isPermanent()) return false;

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
        swapHistory.clear();
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

    // megcsereli a cellak adatait. Eleg lenne
    // ID-t cserelni, ha nem ez alapjan lennenek
    // indexelve a jatekban (array index == id)
    @Deprecated
    public void swap(Cell c) {
        if (canWrite() && c.canWrite()) {
            Player playerTemp = c.owner;
            MemoryState stateTemp = c.state;
            boolean fortifyTemp = c.isFortifying;
            int writesTemp = c.writeCount;

            c.owner = this.owner;
            c.state = this.state;
            c.isFortifying = this.isFortifying;
            c.writeCount = this.writeCount;

            this.owner = playerTemp;
            this.state = stateTemp;
            this.isFortifying = fortifyTemp;
            this.writeCount = writesTemp;

            this.swapHistory.add(c);
            c.swapHistory.add(this);
        } else {
            this.corruptSwap();
            c.corruptSwap();
        }
    }

    // csereljuk ki oket az arrayben + az id-juket
    // igy nem kell tagonket masolgatni (ha meg esetleg
    // adnank hozza fieldeket), viszont kozvetlenul
    // bele kell nyulni a cella arraybe :/
    public void swap2(final Integer targetID, Cell[] grid) {
        Cell c = grid[targetID];

        if (canWrite() && c.canWrite()) {
            grid[targetID] = this;
            grid[this.id] = c;

            c.id = this.id;
            c.swapHistory.add(this);

            this.id = targetID;
            this.swapHistory.add(c);
        } else {
            c.corruptSwap();
            this.corruptSwap();
        }
    }

    // minden rosszul swappelt cellat corruptra allit,
    // vegigvonul az osszes erintett cellan - de csak
    // egyszer. Ezt en csak a swapHistory-val meg a
    // swapCorruptions-szel tudtam megoldani. Biztos
    // lehetne valahogy szebben
    // FIXME
    void corruptSwap() {
        if (isPermanent()) return;

        if (!swapCorruptions.contains(this)) {
            this.state = MemoryState.CORRUPT;
            swapCorruptions.add(this);

            for (Cell c : swapHistory) {
                c.corruptSwap();
            }
            swapHistory.clear();
        }
    }

    // pontosan 1 swap esetén számít sikeresnek,
    // egyébként elbaszódik az egész
    public boolean successfulySwapped() {
        return swapHistory.size() == 1;
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
            return "(SYS )";
        } else if (state == MemoryState.FREE) {
            return "(FREE)";
        } else {
            return "(" + Cell.convertState(state) + ":" + playerId + ")";
        }
    }
}
