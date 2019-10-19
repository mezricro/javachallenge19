package com.loxon.javachallenge.memory.api;

/**
 * Score of a player in a game.
 */
public class PlayerScore {

    private Player player;
    private int ownedCells;
    private int ownedBlocks;
    private int fortifiedCells;
    private int totalScore;

    public PlayerScore(final Player player) {
        this.player = player;
    }

    /**
     * Get player for this score.
     * @return player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Number of allocated cells for the player (including fortified as well).
     * @return number of cells
     */
    public int getOwnedCells() {
        return ownedCells;
    }

    public void setOwnedCells(final int ownedCells) {
        this.ownedCells = ownedCells;
    }

    /**
     * Number of owned blocks for the player (all cells in a block is owned).
     * @return number of blocks
     */
    public int getOwnedBlocks() {
        return ownedBlocks;
    }

    public void setOwnedBlocks(final int ownedBlocks) {
        this.ownedBlocks = ownedBlocks;
    }

    /**
     * Number of fortified cells for the player.
     * @return number of cells
     */
    public int getFortifiedCells() {
        return fortifiedCells;
    }

    public void setFortifiedCells(final int fortifiedCells) {
        this.fortifiedCells = fortifiedCells;
    }

    /**
     * Total score of the player.
     * @return number of allocated cells + ( 4 * number of allocated blocks )
     */
    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(final int totalScore) {
        this.totalScore = totalScore;
    }

    @Override
    public String toString() {
        return "PlayerScore{" +
                "ownedCells=" + ownedCells +
                ", ownedBlocks=" + ownedBlocks +
                ", fortifiedCells=" + fortifiedCells +
                ", totalScore=" + totalScore +
                '}';
    }
}
