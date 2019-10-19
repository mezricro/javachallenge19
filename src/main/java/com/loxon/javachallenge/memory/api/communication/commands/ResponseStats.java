package com.loxon.javachallenge.memory.api.communication.commands;

import com.loxon.javachallenge.memory.api.Player;
import com.loxon.javachallenge.memory.api.communication.general.Response;

import java.util.Objects;

/**
 * Response for @{@link CommandStats} command.
 */
public class ResponseStats extends Response {
    private int cellCount;
    private int ownedCells;
    private int freeCells;
    private int allocatedCells;
    private int corruptCells;
    private int fortifiedCells;
    private int systemCells;
    private int remainingRounds;

    public ResponseStats( final Player player ) {
        super(player);
    }

    public int getCellCount() {
        return cellCount;
    }

    public void setCellCount( int cellCount ) {
        this.cellCount = cellCount;
    }

    public int getOwnedCells() {
        return ownedCells;
    }

    public void setOwnedCells( int ownedCells ) {
        this.ownedCells = ownedCells;
    }

    public int getFreeCells() {
        return freeCells;
    }

    public void setFreeCells( int freeCells ) {
        this.freeCells = freeCells;
    }

    public int getAllocatedCells() {
        return allocatedCells;
    }

    public void setAllocatedCells( int allocatedCells ) {
        this.allocatedCells = allocatedCells;
    }

    public int getCorruptCells() {
        return corruptCells;
    }

    public void setCorruptCells( int corruptCells ) {
        this.corruptCells = corruptCells;
    }

    public int getFortifiedCells() {
        return fortifiedCells;
    }

    public void setFortifiedCells( int fortifiedCells ) {
        this.fortifiedCells = fortifiedCells;
    }

    public int getSystemCells() {
        return systemCells;
    }

    public void setSystemCells( int systemCells ) {
        this.systemCells = systemCells;
    }

    public int getRemainingRounds() {
        return remainingRounds;
    }

    public void setRemainingRounds( int remainingRounds ) {
        this.remainingRounds = remainingRounds;
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) {
            return true;
        }
        if ( o == null || getClass() != o.getClass() ) {
            return false;
        }
        ResponseStats that = (ResponseStats) o;
        return cellCount == that.cellCount &&
            ownedCells == that.ownedCells &&
            freeCells == that.freeCells &&
            allocatedCells == that.allocatedCells &&
            corruptCells == that.corruptCells &&
            fortifiedCells == that.fortifiedCells &&
            systemCells == that.systemCells &&
            remainingRounds == that.remainingRounds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cellCount, ownedCells, freeCells, allocatedCells, corruptCells, fortifiedCells, systemCells, remainingRounds);
    }

    @Override
    public String toString() {
        return "ResponseStats{" +
            "cellCount=" + cellCount +
            ", ownedCells=" + ownedCells +
            ", freeCells=" + freeCells +
            ", allocatedCells=" + allocatedCells +
            ", corruptCells=" + corruptCells +
            ", fortifiedCells=" + fortifiedCells +
            ", systemCells=" + systemCells +
            ", remainingRounds=" + remainingRounds +
            '}';
    }
}
