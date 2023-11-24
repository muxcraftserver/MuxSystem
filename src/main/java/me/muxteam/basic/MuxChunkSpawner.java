package me.muxteam.basic;


import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;

public class MuxChunkSpawner {

    private final Map<EntityType, Integer> pendingMobs = new HashMap<>();

    private long lastSpawn;

    public MuxChunkSpawner() {
        this.lastSpawn = System.currentTimeMillis();
    }

    public boolean canSpawn(EntityType type) {
        return this.lastSpawn + 30_000 < System.currentTimeMillis() && pendingMobs.get(type) > 10;
    }

    private void addMob(EntityType entityType, Location location) {
        pendingMobs.put(entityType, pendingMobs.getOrDefault(entityType, 0) + 1);
    }

    public void spawnMob(EntityType type, Location location) {
        this.addMob(type, location);
        if (!canSpawn(type))
            return;
        this.lastSpawn = System.currentTimeMillis();
        int amount = this.pendingMobs.get(type);
        Entity target = location.getWorld().spawnEntity(location, type);
        final String mobname = MuxChunkSpawner.getMobName(target.getType());
        target.setCustomName("§a" + (amount) + "§a✕ §f" + mobname);
        target.setCustomNameVisible(true);
        pendingMobs.remove(type);
    }

    private static String getMobName(final EntityType type) {
        switch (type) {
            case PIG_ZOMBIE:
                return MuxSystem.getAPI().getLang("mob.zombiepigman");
            case ZOMBIE:
                return MuxSystem.getAPI().getLang("mob.zombie");
            case WITCH:
                return MuxSystem.getAPI().getLang("mob.witch");
            case BLAZE:
                return MuxSystem.getAPI().getLang("mob.blaze");
            case SKELETON:
                return MuxSystem.getAPI().getLang("mob.skeleton");
            default:
                return "";
        }
    }
}