package com.loxon.javachallenge.memory.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Player instance.
 * Each created instance is unique, creating multiple players
 * with the same name will be different.
 */
public class Player {

    private String name;
    private String id = UUID.randomUUID().toString();

    public Player(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Player player = (Player) o;
        return Objects.equals(id, player.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
