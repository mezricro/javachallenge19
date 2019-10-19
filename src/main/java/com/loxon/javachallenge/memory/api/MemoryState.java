package com.loxon.javachallenge.memory.api;

/**
 * State of a memory cell.
 */
public enum MemoryState {

    /**
     * System cell, unmodifiable.
     */
    SYSTEM,
    /**
     * Allocated to an application.
     */
    ALLOCATED,
    /**
     * Allocated to an application and unmodifiable.
     */
    FORTIFIED,
    /**
     * Corrupted cell.
     */
    CORRUPT,
    /**
     * Unallocated free cell.
     */
    FREE,
    /**
     * Allocated to the current application.
     */
    OWNED_ALLOCATED,
    /**
     * Allocated to the current application and unmodifiable.
     */
    OWNED_FORTIFIED
}
