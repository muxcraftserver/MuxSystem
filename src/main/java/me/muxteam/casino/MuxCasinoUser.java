package me.muxteam.casino;

import java.util.UUID;

public class MuxCasinoUser {
    private final UUID uuid;
    private long chips, weeklywins;
    private int energy;
    private boolean changed = false;

    public MuxCasinoUser(final UUID uuid, final long chips, final int energy, final long weeklywins) {
        this.uuid = uuid;
        this.chips = chips;
        this.energy = energy;
        this.weeklywins = weeklywins;
    }
    public UUID getUUID() {
        return uuid;
    }
    public long getChips() {
        return chips;
    }
    public void setChips(final long chips) {
        this.chips = chips;
        this.changed = true;
    }
    public int getEnergy() {
        return energy;
    }
    public void setEnergy(final int energy) {
        this.energy = energy;
        this.changed = true;
    }
    public long getWeeklyWins() {
        return weeklywins;
    }
    public void setWeeklyWins(final long weeklywins) {
        this.weeklywins = weeklywins;
        this.changed = true;
    }
    public boolean hasChanged() {
        return changed;
    }
    public void saved() {
        this.changed = false;
    }
}