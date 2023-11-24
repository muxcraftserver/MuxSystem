package me.muxteam.basic;

import com.google.common.collect.Sets;
import com.sk89q.worldguard.bukkit.ConfigurationManager;
import com.sk89q.worldguard.bukkit.WGBukkit;
import me.muxteam.extras.MuxMounts.Rideable;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxteam.MuxAdmin;
import net.minecraft.server.v1_8_R3.EnchantmentManager;
import net.minecraft.server.v1_8_R3.EntityItem;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.EntitySheep;
import org.apache.commons.lang3.mutable.MutableInt;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftItem;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftSheep;
import org.bukkit.craftbukkit.v1_8_R3.util.LongHash;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class MuxAntiLags implements Listener {

    private static Method dropDeathLootMethod = null;
    private static Field itemAgeField = null;

    static {
        try {
            dropDeathLootMethod = EntityLiving.class.getDeclaredMethod("dropDeathLoot", boolean.class, int.class);
            dropDeathLootMethod.setAccessible(true);
            itemAgeField = EntityItem.class.getDeclaredField("age");
            itemAgeField.setAccessible(true);
        } catch (final Exception ignored) {
        }
    }

    private MuxSystem ms;
    private final MuxAdmin admin;
    private final Random r = new Random();
    private final int[] tpsList = new int[10];
    private final Map<String, Long> switchavg = new HashMap<>(), hitsavg = new HashMap<>(), placeavg = new HashMap<>();
    private final Map<String, Integer> switchcount = new HashMap<>(), hitscount = new HashMap<>(), placecount = new HashMap<>();
    private final Map<String, Long> switchlast = new HashMap<>(), hitslast = new HashMap<>(), placelast = new HashMap<>();
    private final ConfigurationManager cm = WGBukkit.getPlugin().getGlobalStateManager();
    private final List<String> forceaways = new ArrayList<>();
    private final Map<String, Map<Long, MuxChunkSpawner>> chunkSpawners = new HashMap<>();
    private boolean noStopLag;
    private final List<Long> chunksWithDroppedItems = new ArrayList<>();
    private final Map<Long, Map<QueuedDroppedItem, Integer>> queuedDroppedItems = new ConcurrentHashMap<>();

    public int range = 16;
    public boolean spawnerTest = true;
    public boolean droppedItemTest = true;

    public MuxAntiLags(final MuxSystem ms) {
        this.ms = ms;
        this.admin = ms.getAdmin();
        ms.getServer().getPluginManager().registerEvents(this, ms);
        startTPSCheck();
        startLagCleanup();
        forceaways.addAll(Sets.newHashSet("shop", "nether", "end"));
    }

    public void close() {
        this.ms = null;
    }

    // CHECK TPS

    public double getTPS() {
        final double denominator = Arrays.stream(tpsList).limit(10).sum() / 10D;
        return (denominator == 0) ? -1 : (100000D / denominator);
    }

    private void startTPSCheck() {
        new BukkitRunnable() {
            long lasttick = System.currentTimeMillis();
            int tickcnt = 0;

            @Override
            public void run() {
                final long time = System.currentTimeMillis();
                tpsList[tickcnt] = (int) (time - lasttick);
                lasttick = time;
                tickcnt = (tickcnt + 1) % 10;
                dropQueuedItems();
            }
        }.runTaskTimer(ms, 0L, 100L);


        new BukkitRunnable() {
            boolean cancelled = false;
            boolean filling = false;

            @Override
            public void run() {
                final ZonedDateTime nowInCET = ZonedDateTime.now(ZoneId.of("CET"));
                if (nowInCET.getHour() >= 13) {
                    cancel();
                    return;
                }
                if (cancelled == false) {
                    ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "wb fill cancel");
                    cancelled = true;
                    noStopLag = true;
                    ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "wb world fill 100");
                    ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "wb fill confirm");
                    filling = true;
                    new BukkitRunnable() { // 5 minuten kein stoplag
                        @Override
                        public void run() {
                            noStopLag = false;
                        }
                    }.runTaskLater(ms, 20L * 60L * 5L);
                    return;
                }
                boolean stoploading = ms.getServer().getOnlinePlayers().size() > 80;
                if (filling && stoploading) {
                    ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "wb fill cancel");
                    filling = false;
                    cancel();
                }
            }
        }.runTaskTimer(ms, 20L * 4L, 20L * 5L);
    }

    private int chunkTicks = 0;
    private int chunkIteration = 0;

    private void dropQueuedItems() {
        boolean fullTick = true;
        final Set<Long> keysToRemove = new HashSet<>();
        int droppedItems = 0;
        for (int i = this.chunkIteration; i < this.chunkIteration + 120; i++) {
            if (this.chunksWithDroppedItems.size() <= i)
                break;
            if (droppedItems > 120) {
                chunkTicks--;
                break;
            }
            long chunkKey = this.chunksWithDroppedItems.get(i);

            Iterator<Map.Entry<QueuedDroppedItem, Integer>> iterator = this.queuedDroppedItems.get(chunkKey).entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<QueuedDroppedItem, Integer> entry = iterator.next();
                int toDrop = Math.min(entry.getValue(), 64);
                int newAmount = entry.getValue() - 64;
                boolean dropped = entry.getKey().drop(toDrop);
                if (dropped)
                    droppedItems++;
                entry.setValue(newAmount);
                if (newAmount <= 0 && dropped) {
                    iterator.remove();
                }
            }
            if (this.queuedDroppedItems.get(chunkKey).isEmpty()) {
                keysToRemove.add(chunkKey);
            }
        }
        if (++this.chunkTicks == 2) {
            this.chunkTicks = 0;
            this.chunkIteration = this.chunkIteration + 120;
            if (this.chunksWithDroppedItems.size() - 1 < this.chunkIteration)
                this.chunkIteration = 0;
        }
        keysToRemove.forEach(key -> {
            this.queuedDroppedItems.remove(key);
            this.chunksWithDroppedItems.remove(key);
        });
    }

    private void doAutoForceaway() {
        final String gwname = ms.getGameSpawn().getWorld().getName();
        final MutableInt count = new MutableInt();
        List<Player> toTeleport = ms.getServer().getOnlinePlayers().stream().filter(player -> {
            final Location location = player.getLocation();
            final boolean inspawn = location.getWorld().getName().equals(gwname) && ms.getSpawnRegion() != null &&
                    ms.getSpawnRegion().contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            if (inspawn) count.add(1);
            return ms.isAFK(player.getName()) && inspawn;
        }).collect(Collectors.toList());
        if (count.intValue() >= 100) {
            toTeleport.forEach(player -> ms.forcePlayer(player, ms.getWarps().getWarpLocation(forceaways.get(r.nextInt(forceaways.size())))));
        }
        toTeleport.clear();
    }

    // STOPLAG

    public boolean isStopLag() {
        return cm.activityHaltToggle;
    }

    // CHECK PING

    public boolean handlePingCommand(final Player p, final String[] args) {
        if (args.length == 0) {
            final int ping = ((CraftPlayer) p).getHandle().ping;
            ms.showItemBar(p, "§fMux§6Pong! §f(" + ping + "ms, " + (ping < 60 ? "§a" + ms.getLang("verygood") : ping < 150 ? ms.getLang("good") : ping < 350 ? ms.getLang("okay") : ping < 500 ? ms.getLang("slow") : ping < 1000 ? ms.getLang("prettyslow") : ms.getLang("veryslow")) + "§f)");
            return true;
        }
        final Player pl = ms.getServer().getPlayer(args[0]);
        if (pl == null || p.canSee(pl) == false) {
            ms.showItemBar(p, ms.hnotonline);
            return true;
        }
        final int ping = ((CraftPlayer) pl).getHandle().ping;
        ms.showItemBar(p, "§fMux§6Pong! §7» §6" + pl.getName() + " §f(" + ping + "ms, " + (ping < 60 ? "§a" + ms.getLang("verygood") : ping < 150 ? ms.getLang("good") : ping < 350 ? ms.getLang("okay") : ping < 500 ? ms.getLang("slow") : ping < 1000 ? ms.getLang("prettyslow") : ms.getLang("veryslow")) + "§f)");
        return true;
    }

    public boolean handlePerformanceCommand(final Player p) {
        p.sendMessage(new String[]{ms.header((byte) 13, "§6"), "  §6§lPerformance", ""});
        double tps = getTPS();
        if (tps > 19.9D) tps = 20D;
        final String t = ms.getDecimalFormat((tps / 20D) * 100) + "%";
        p.sendMessage("  " + ms.getLang("cmd.serverload") + " " + (tps <= 21 ? (tps >= 18 ? "§a" + t + " (" + ms.getLang("cmd.serverload1") + ") " : tps >= 15 ? "§e" + t + " (" + ms.getLang("cmd.serverload2") + ") " : tps >= 10 ? "§c" + t + " (" + ms.getLang("cmd.serverload3") + ") " : "§4" + t + " (" + ms.getLang("cmd.serverload4") + ") ")
                : "§c(" + ms.getLang("cmd.calculatingload") + ")"));
        if (cm.activityHaltToggle) {
            p.sendMessage("  " + ms.getLang("activityhalt.disabled"));
        } else if (admin.REDSTONE.isActive() == false) {
            p.sendMessage("  " + ms.getLang("redstone.disabled"));
        }
        if (p.isOp()) {
            p.sendMessage("  §7Uptime:§c " + ms.timeToString(System.currentTimeMillis() - java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime(), false));
            final Runtime run = Runtime.getRuntime();
            p.sendMessage("  §7RAM: §c" + ms.getDecimalFormat(((run.totalMemory() - run.freeMemory()) / 1024D / 1024D / 1024D)) + " GB §7/ " + ms.getDecimalFormat((run.totalMemory() / 1024D / 1024D / 1024D)) + " GB");
            double usedSpaceGB = 0, totalSpaceGB = 0;
            try {
                final Path path = FileSystems.getDefault().getPath(".");
                final FileStore fileStore = Files.getFileStore(path);
                final long totalSpace = fileStore.getTotalSpace(), usedSpace = totalSpace - fileStore.getUsableSpace();
                usedSpaceGB = usedSpace / (1024D * 1024D * 1024D);
                totalSpaceGB = totalSpace / (1024D * 1024D * 1024D);
            } catch (final Exception ignored) {
            }
            p.sendMessage("  §7Speicher: §c" + ms.getDecimalFormat(usedSpaceGB) + " GB §7/ " + ms.getDecimalFormat(totalSpaceGB) + " GB");
            p.sendMessage(" ");
            final StringBuilder worldinfo = new StringBuilder();
            for (final World w : ms.getServer().getWorlds()) {
                if (worldinfo.toString().isEmpty() == false) worldinfo.append("\n");
                worldinfo.append(ms.getLang("activityhalt.worldinfo")).append(": \"§c").append(w.getName()).append("§7\": §c").append(w.getLoadedChunks().length).append("§7 chunks, §c").append(w.getEntities().size()).append(" §7entities");
            }
            ms.chatClickHoverRun(p, "  " + ms.getLang("activity.worlds.hover"), worldinfo.toString(), "/a");
        }
        p.sendMessage(ms.footer((byte) 13, "§6"));
        return true;
    }

    // LAG DETECTING

    private void startLagCleanup() {
        final Server sr = ms.getServer();
        new BukkitRunnable() {
            boolean slowercheck = false;

            @Override
            public void run() {
                //  removeBuggedMobs();
                if (slowercheck ^= true) {
                    if (admin.CHECKFARMS.isActive()) checkFarmLag(sr);
                    resetDropCooldown();
                    resetPacketCooldown();
                    if (admin.FORCEAWAY.isActive()) doAutoForceaway();
                }
                if (admin.AUTOSTOPLAG.isActive()) clearEntityLags(sr);
                adjustPlayerRange(sr);
                ms.getHolograms().removeOld();
                removeOldPetsAndMounts();
            }
        }.runTaskTimer(ms, 400L, 1200L);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (admin.AUTOSTOPLAG.isActive()) {
                    clearOtherLags();
                }
            }
        }.runTaskTimer(ms, 200L, 40L);
    }

    private void removeBuggedMobs() {
        final Set<Entity> toRemove = new HashSet<>();
        ms.getServer().getWorlds().forEach(w -> w.getEntitiesByClass(Chicken.class)
                .forEach(chicken -> {
                    final Entity passenger = chicken.getPassenger();
                    if (passenger != null) {
                        passenger.eject();
                        toRemove.add(passenger);
                        toRemove.add(chicken);
                    }
                }));
        toRemove.forEach(Entity::remove);
    }

    private void removeOldPetsAndMounts() {
        for (final World world : ms.getServer().getWorlds()) {
            for (final LivingEntity entity : world.getLivingEntities()) {
                if (entity.getUniqueId().equals(ms.getIdentificationId())) {
                    if (ms.getPets().isPet(entity) || ms.getMounts().isMount(entity)) continue;
                    if (entity.getType() == EntityType.WITHER || (entity.isCustomNameVisible() && entity.getCustomName() != null && entity.getCustomName().startsWith("§a") && entity.getCustomName().contains(" §7")))
                        entity.remove();
                }
            }
        }
    }

    private void clearEntityLags(final Server sr) {
        final double tps = getTPS();
        checkVehicleLag(sr);
        if (tps <= 16.5D) {
            for (final World w : sr.getWorlds()) {
                killAll(w);
            }
        }
        if (tps <= 15D && noStopLag == false) {
            sr.dispatchCommand(sr.getConsoleSender(), "stoplag -s");
        }
    }

    private void clearOtherLags() {
        final double tps = getTPS();
        if (tps <= 15D) {
            admin.REDSTONE.setActive(false);
            admin.FIREWORKS.setActive(false);
            admin.DROPSANDEXP.setActive(false);
        } else if (tps > 18.5D) {
            admin.REDSTONE.setActive(true);
            admin.FIREWORKS.setActive(true);
            admin.DROPSANDEXP.setActive(true);
        }
    }

    // PLAYER RANGE

    public void adjustPlayerRange(final Server sr) {
        for (final World w : sr.getWorlds()) {
            ((CraftWorld) w).getHandle().spigotConfig.playerTrackingRange = range;
        }
    }

    // ENTITY LAGS

    public int killAll(final World w) {
        int count = 0;
        final UUID muxuuid = ms.getIdentificationId();
        for (final Chunk chunk : w.getLoadedChunks()) {
            for (final Entity entity : chunk.getEntities()) {
                if (entity instanceof LivingEntity == false || entity instanceof HumanEntity || entity instanceof ArmorStand) {
                    continue;
                } else if (entity instanceof Horse && ((Horse) entity).isTamed()) {
                    continue;
                } else if (entity instanceof Wolf && ((Wolf) entity).isTamed()) {
                    continue;
                } else if (entity instanceof Ocelot && ((Ocelot) entity).isTamed()) {
                    continue;
                } else if (entity.getUniqueId().equals(muxuuid)) {
                    continue;
                }
                entity.remove();
                count++;
            }
        }
        return count;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onSpawnSpawner(final SpawnerSpawnEvent e) {
        if (spawnerTest == false)
            return;
        if (isStopLag()) {
            e.setCancelled(true);
            return;
        }
        if (types.contains(e.getEntity().getType()) == false) {
            return;
        }
        Map<Long, MuxChunkSpawner> spawners = this.chunkSpawners.computeIfAbsent(e.getSpawner().getWorld().getName(), s -> new HashMap<>());
        final Chunk chunk = e.getSpawner().getChunk();
        final long key = LongHash.toLong(e.getSpawner().getX() >> 4, e.getSpawner().getZ() >> 4);
        final MuxChunkSpawner spawner = spawners.computeIfAbsent(key, aLong -> new MuxChunkSpawner());
        spawner.spawnMob(e.getEntityType(), e.getLocation());
        e.setCancelled(true);
    }

    @EventHandler
    public void onEgg(final PlayerEggThrowEvent e) {
        if (WGBukkit.getRegionManager(e.getPlayer().getWorld()).getApplicableRegions(e.getPlayer().getLocation()).size() > 0) {
            e.setHatching(false);
        }
    }

    // DROP COOLDOWN

    private void resetDropCooldown() {
        ms.resetLimit("DROP");
    }

    private void checkDrop(final Player p, final PlayerDropItemEvent e) {
        if (ms.checkLimit(p.getName(), "DROP", 45, true)) {
            ms.showItemBar(p, ms.getLang("dropwait"));
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(final PlayerDropItemEvent e) {
        if (admin.DROPSANDEXP.isActive() == false) {
            ms.showItemBar(e.getPlayer(), ms.getLang("cleanup.usecommand"));
            e.setCancelled(true);
        } else if (e.getItemDrop().getItemStack().getMaxStackSize() == 64) {
            return;
        }
        checkDrop(e.getPlayer(), e);
    }


    // EXP AND FIREWORKS

    @EventHandler(ignoreCancelled = true)
    public void onInteract(final PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            final Material m = e.getMaterial();
            if (m == Material.FIREWORK && admin.FIREWORKS.isActive() == false) {
                ms.showItemBar(e.getPlayer(), ms.getLang("fireworks"));
                e.setCancelled(true);
                e.getPlayer().updateInventory();
            } else if (m == Material.EXP_BOTTLE && admin.DROPSANDEXP.isActive() == false) {
                ms.showItemBar(e.getPlayer(), ms.getLang("expbottle.disabled"));
                e.setCancelled(true);
                e.getPlayer().updateInventory();
            }
        }
    }

    @EventHandler
    public void onProjectileThrownEvent(final ProjectileLaunchEvent e) {
        if (e.getEntity() instanceof ThrownExpBottle && admin.DROPSANDEXP.isActive() == false) {
            e.setCancelled(true);
            final ProjectileSource shooter = e.getEntity().getShooter();
            if (shooter instanceof Player) {
                final Player p = (Player) shooter;
                if (p.getGameMode() != GameMode.CREATIVE) {
                    p.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 1));
                }
            }
        }
    }

    // MERGE MOBS

    final int mergeradius = 5;
    final Set<EntityType> types = Sets.newHashSet(EntityType.PIG_ZOMBIE, EntityType.ZOMBIE, EntityType.WITCH, EntityType.SKELETON, EntityType.BLAZE, EntityType.SHEEP);

    public Set<EntityType> getStackedTypes() {
        return types;
    }

    public void mergeMobs(final Server sr) {
        final UUID muxuuid = ms.getIdentificationId();
        for (final World w : sr.getWorlds()) {
            for (final LivingEntity en : w.getLivingEntities()) {
                if (types.contains(en.getType()) == false || en.isValid() == false || en.isOnGround() == false || en.getUniqueId().equals(muxuuid)) {
                    continue;
                }
                for (final Entity nearby : en.getNearbyEntities(mergeradius, mergeradius, mergeradius)) {
                    if (nearby instanceof LivingEntity == false || nearby.getType() != en.getType() || nearby.isValid() == false) {
                        continue;
                    } else if (nearby instanceof Zombie && en instanceof Zombie && ((Zombie) nearby).isBaby() != ((Zombie) en).isBaby()) {
                        continue;
                    }
                    stackMob(en, (LivingEntity) nearby);
                }
            }
        }
    }

    private void stackMob(final Entity target, final LivingEntity tostack) {
        final String displayName = target.getCustomName(), mobname = getMobName(target.getType());
        final int alreadyStacked = getStacked(displayName);
        int alreadyStacked2 = getStacked(tostack.getCustomName());
        if (alreadyStacked2 == -1) alreadyStacked2 = 1;

        tostack.remove();

        if (alreadyStacked == -1) { // The target is not a stack
            target.setCustomName("§a" + (1 + alreadyStacked2) + "§a✕ §f" + mobname);
            target.setCustomNameVisible(true);
        } else { // The target is already a stack
            target.setCustomName("§a" + (alreadyStacked + alreadyStacked2) + "§a✕ §f" + mobname);
        }
    }

    private int getStacked(final String display) {
        if (display == null || display.startsWith("§a") == false) {
            return -1;
        }
        final String cleanamount = ChatColor.stripColor(display.replace("§a✕", "").split(" ")[0]); // ASCII so it's not possible to set the X via Nametag Item.
        if (cleanamount.matches("[0-9]+") == false || cleanamount.length() > 4) { // Must be number & 9999 Mobs maximum
            return -1;
        } else {
            return Integer.parseInt(cleanamount);
        }
    }

    private String getMobName(final EntityType type) {
        switch (type) {
            case PIG_ZOMBIE:
                return ms.getLang("mob.zombiepigman");
            case ZOMBIE:
                return ms.getLang("mob.zombie");
            case WITCH:
                return ms.getLang("mob.witch");
            case BLAZE:
                return ms.getLang("mob.blaze");
            case SKELETON:
                return ms.getLang("mob.skeleton");
            default:
                return "";
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDeath(final EntityDeathEvent e) {
        if (types.contains(e.getEntity().getType())) {
            int stacked = getStacked(e.getEntity().getCustomName());
            if (stacked > 300) {
                stacked = 300;
                final Player killer = e.getEntity().getKiller();
                if (killer != null) {
                    ms.showLater(killer, String.format(ms.getLang("moblimit"), stacked));
                }
            }
            if (stacked > 1)
                e.setDroppedExp(e.getDroppedExp() * stacked);

            final int lootingLevel = e.getEntity().getKiller() == null ? 0 : EnchantmentManager.getBonusMonsterLootEnchantmentLevel(((CraftPlayer) e.getEntity().getKiller()).getHandle());
            if (e.getEntity().getType() == EntityType.WITCH) {
                final int dropCount = IntStream.range(0, stacked).map(i -> r.nextInt(7) == 0 ? 1 : 0).sum();
                if (dropCount > 0) {
                    if (dropDeathLoot(e.getEntity(), dropCount, e.getEntity().getKiller() != null, e.getDrops(), lootingLevel))
                        e.getDrops().clear();
                    e.getDrops().add(new ItemStack(Material.GHAST_TEAR, dropCount));
                } else e.getDrops().clear();
                return;
            }
            if (stacked > 1) {
                if (dropDeathLoot(e.getEntity(), stacked, e.getEntity().getKiller() != null, e.getDrops(), lootingLevel))
                    e.getDrops().clear();
            }
        } else if (e.getEntity().getType() == EntityType.IRON_GOLEM) {
            e.getDrops().removeIf(item -> item.getType() == Material.IRON_INGOT);
            if (r.nextInt(4) == 0) e.getDrops().add(new ItemStack(Material.IRON_INGOT, 1));
        }
    }

    private boolean dropDeathLoot(final Entity e, final int times, boolean deathByPlayer, final List<ItemStack> drops, final int looting) {
        if (droppedItemTest) {
            final Map<MaterialData, Integer> itemsToDrop;
            if (e.getType() == EntityType.PIG_ZOMBIE) {
                itemsToDrop = calculatePigZombieDeathLoot(times);
            } else if (e.getType() == EntityType.SKELETON) {
                Skeleton skeleton = (Skeleton) e;
                itemsToDrop = calculateSkeletonDrop(skeleton.getSkeletonType(), times, looting, deathByPlayer);
            } else if (e.getType() == EntityType.SHEEP) {
                final EntitySheep entitySheep = ((CraftSheep) e).getHandle();
                itemsToDrop = calculateSheepDrop(entitySheep.isBurning(), times, looting, entitySheep.getColor().getColorIndex());
            } else {
                itemsToDrop = new HashMap<>();
                for (ItemStack drop : drops) {
                    MaterialData materialData = drop.getData();
                    itemsToDrop.merge(materialData, drop.getAmount() * times, Integer::sum);
                }
            }

            if (deathByPlayer == false) { // if no player kills the mob we can "queue the drops" and drop them bit by bit to avoid item lag
                final long chunkKey = LongHash.toLong(e.getLocation().getBlockX() >> 4, e.getLocation().getBlockZ() >> 4);
                final Map<QueuedDroppedItem, Integer> queuedItems = this.queuedDroppedItems.computeIfAbsent(chunkKey, aLong -> {
                    this.chunksWithDroppedItems.add(chunkKey);
                    return new HashMap<>();
                });
                itemsToDrop.forEach((materialData, amount) -> {
                    QueuedDroppedItem queuedDroppedItem = new QueuedDroppedItem(new SimpleLocation(e.getLocation()), materialData);
                    queuedItems.merge(queuedDroppedItem, amount, Integer::sum);
                });
            } else {
                itemsToDrop.forEach((materialData, amount) -> {
                    if (amount <= 64) {
                        e.getLocation().getWorld().dropItemNaturally(e.getLocation(), materialData.toItemStack(amount));
                    } else {
                        while (amount > 0) {
                            final int toDrop = Math.min(64, amount);
                            e.getLocation().getWorld().dropItemNaturally(e.getLocation(), materialData.toItemStack(toDrop));
                            amount -= toDrop;
                        }
                    }
                });
            }
            return true;
        }
        if (dropDeathLootMethod == null) return false;
        final EntityLiving en = (EntityLiving) ((CraftEntity) e).getHandle();

        for (int i = 0; i < times; i++) {
            try {
                dropDeathLootMethod.invoke(en, true, 1);
            } catch (final Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    private Map<MaterialData, Integer> calculatePigZombieDeathLoot(int amount) {
        final Map<MaterialData, Integer> drops = new HashMap<>();
        int var2 = 1;
        final MaterialData rottenFlesh = new MaterialData(Material.ROTTEN_FLESH);
        final MaterialData goldNuggets = new MaterialData(Material.GOLD_NUGGET);
        for (int i = 0; i < amount; i++) {
            int itemAmount = this.r.nextInt(2 + var2);
            drops.merge(rottenFlesh, itemAmount, Integer::sum);
            itemAmount = this.r.nextInt(2 + var2);
            drops.merge(goldNuggets, itemAmount, Integer::sum);
        }
        return drops;
    }

    private Map<MaterialData, Integer> calculateSkeletonDrop(Skeleton.SkeletonType type, int amount, final int looting, boolean player) {
        final Map<MaterialData, Integer> drops = new HashMap<>();
        final MaterialData bone = new MaterialData(Material.BONE);
        final MaterialData skull = new MaterialData(Material.SKULL_ITEM, (byte) 1);
        final MaterialData coal = new MaterialData(Material.COAL);
        final MaterialData arrow = new MaterialData(Material.ARROW);
        int j, k;
        if (player && this.r.nextFloat() < 0.025F + (float) looting * 0.01F) { // WITHER SKULL
            drops.merge(skull, 1, Integer::sum);
        }
        for (int i = 0; i < amount; i++) {
            if (type == Skeleton.SkeletonType.WITHER) {

                j = this.r.nextInt(3 + looting) - 1;
                drops.merge(coal, j, Integer::sum);
            } else {
                j = this.r.nextInt(3 + looting);
                drops.merge(arrow, 1, Integer::sum);
            }
            j = this.r.nextInt(3 + looting);
            drops.merge(bone, j, Integer::sum);
        }
        return drops;
    }

    private Map<MaterialData, Integer> calculateSheepDrop(boolean burning, int amount, final int looting, final int colorIndex) {
        final Map<MaterialData, Integer> drops = new HashMap<>();
        final MaterialData wool = new MaterialData(Material.WOOL, (byte) colorIndex);
        final MaterialData mutton = new MaterialData(Material.MUTTON);
        final MaterialData cookedMutton = new MaterialData(Material.COOKED_MUTTON);
        int j;
        for (int i = 0; i < amount; i++) {
            j = this.r.nextInt(2) + 1 + this.r.nextInt(1 + looting);
            if (burning) {
                drops.merge(cookedMutton, j, Integer::sum);
            } else {
                drops.merge(mutton, j, Integer::sum);
            }
        }
        return drops;
    }

    // MINECART LAG FIX

    private final int maxvehiclesperchunk = 25;

    public void checkVehicleLag(final Server sr) {
        for (final World w : sr.getWorlds()) {
            checkChunkLimit(w, Minecart.class);
            checkChunkLimit(w, Boat.class);
        }
    }

    public void checkChunkLimit(final World w, final Class<? extends Entity> en) {
        final Map<Chunk, Integer> cartspam = new HashMap<>();
        w.getEntitiesByClass(en).forEach(xe -> {
            if (((CraftEntity) xe).getHandle() instanceof Rideable == false) {
                int amount = cartspam.getOrDefault(xe.getLocation().getChunk(), 0);
                cartspam.put(xe.getLocation().getChunk(), amount + 1);
                if (amount > maxvehiclesperchunk) {
                    xe.remove();
                }
            }
        });
    }

    // FARM LAG FIX

    private final int maxentsperchunk = 50;

    public void checkFarmLag(final Server sr) {
        final Map<World, Map<LivingEntity, Location>> hashMap = new HashMap<>();
        final Set<LivingEntity> hashSet = new HashSet<>();
        for (final World world : sr.getWorlds()) {
            hashMap.put(world, getLivingEntitiesLocationsMap(world, false));
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                final HashSet<EntityGroup> hashSet2 = new HashSet<>();
                for (final Map<LivingEntity, Location> map : hashMap.values()) {
                    hashSet2.addAll(getGroups(map, 3));
                }
                for (final EntityGroup entityGroup : hashSet2) {
                    if (entityGroup.size() > maxentsperchunk) {
                        hashSet.addAll(getExceedingEntities(entityGroup.getEntities(), entityGroup.size() - maxentsperchunk));
                    }
                }
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (final LivingEntity livingEntity : hashSet) {
                            if (livingEntity != null && livingEntity.getType() != EntityType.PLAYER) {
                                livingEntity.remove();
                            }
                        }
                    }
                }.runTask(ms);
            }
        }.runTaskAsynchronously(ms);
    }

    private Map<LivingEntity, Location> getLivingEntitiesLocationsMap(final World world, final boolean excludetamed) {
        final Map<LivingEntity, Location> hashMap = new HashMap<>();
        for (final LivingEntity livingEntity : world.getLivingEntities()) {
            if (excludetamed && isTamed(livingEntity) || livingEntity.getType() == EntityType.PLAYER || livingEntity.getType() == EntityType.ARMOR_STAND) {
                continue;
            }
            hashMap.put(livingEntity, livingEntity.getLocation());
        }
        return hashMap;
    }

    private boolean isTamed(final LivingEntity livingEntity) {
        return livingEntity instanceof Tameable && ((Tameable) livingEntity).isTamed();
    }

    private Set<EntityGroup> getGroups(final Map<LivingEntity, Location> map, final double n) {
        final Set<EntityGroup> hashSet = new HashSet<>();
        if (n == 0.0) {
            return hashSet;
        }
        if (n < 0.0) {
            final EntityGroup entityGroup = new EntityGroup(0.0);
            for (final Map.Entry<LivingEntity, Location> entry : map.entrySet()) {
                entityGroup.add(entry.getKey(), entry.getValue());
            }
            hashSet.add(entityGroup);
            return hashSet;
        }
        for (final Map.Entry<LivingEntity, Location> entry2 : map.entrySet()) {
            final LivingEntity livingEntity = entry2.getKey();
            final Location location = entry2.getValue();
            EntityGroup entityGroup2 = null;
            final Set<EntityGroup> hashSet2 = new HashSet<>();
            for (final EntityGroup entityGroup3 : hashSet) {
                if (entityGroup3.isInRadius(location)) {
                    if (entityGroup2 == null) {
                        entityGroup2 = entityGroup3;
                    } else {
                        hashSet2.add(entityGroup3);
                    }
                }
            }
            if (entityGroup2 == null) {
                final EntityGroup entityGroup4 = new EntityGroup(n);
                entityGroup4.add(livingEntity, location);
                hashSet.add(entityGroup4);
            } else {
                entityGroup2.add(livingEntity, location);
                if (hashSet2.size() > 0) {
                    for (final EntityGroup aHashSet2 : hashSet2) {
                        aHashSet2.transferTo(entityGroup2);
                    }
                }
                purgeEmptyGroups(hashSet);
            }
        }
        return hashSet;
    }

    private void purgeEmptyGroups(final Iterable<EntityGroup> iterable) {
        final Iterator<EntityGroup> iterator = iterable.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().size() == 0) {
                iterator.remove();
            }
        }
    }

    private Set<LivingEntity> getExceedingEntities(final Set<LivingEntity> set, int n) {
        final Set<LivingEntity> hashSet = new HashSet<>();
        if (n <= 0) {
            return hashSet;
        }
        final LinkedList<LivingEntity> list = new LinkedList<>(set);
        list.sort(new EntityComparator());
        while (n > 0 && list.isEmpty() == false) {
            hashSet.add(list.pollFirst());
            --n;
        }
        return hashSet;
    }

    class EntityGroup implements Comparable<EntityGroup> {
        private final Map<LivingEntity, Location> entities;
        private final double radiusSquared;

        public EntityGroup(final double n) {
            this.entities = new HashMap<>();
            this.radiusSquared = n * n;
        }

        public void add(final LivingEntity livingEntity, final Location location) {
            this.entities.put(livingEntity, location);
        }

        public boolean isInRadius(final Location location) {
            for (final Location location1 : this.entities.values()) {
                if (location1.distanceSquared(location) <= this.radiusSquared) {
                    return true;
                }
            }
            return false;
        }

        public void transferTo(final EntityGroup entityGroup) {
            entityGroup.entities.putAll(this.entities);
            this.entities.clear();
        }

        public Set<LivingEntity> getEntities() {
            return this.entities.keySet();
        }

        public int size() {
            return this.entities.size();
        }

        @Override
        public int compareTo(final EntityGroup entityGroup) {
            return this.size() > entityGroup.size() ? -1 : 1;
        }
    }

    class EntityComparator implements Comparator<LivingEntity> {
        @Override
        public int compare(final LivingEntity livingEntity, final LivingEntity livingEntity2) {
            if (livingEntity == livingEntity2) {
                return 0;
            }
            if (livingEntity instanceof Tameable && livingEntity2 instanceof Tameable) {
                final int compareBooleans = this.compareBooleans(((Tameable) livingEntity).isTamed(), ((Tameable) livingEntity2).isTamed());
                if (compareBooleans != 0) {
                    return compareBooleans;
                }
            }
            if (livingEntity.getType() == EntityType.HORSE && livingEntity2.getType() == EntityType.HORSE && livingEntity instanceof Horse && livingEntity2 instanceof Horse) {
                final int compareBooleans2 = this.compareBooleans(((Horse) livingEntity).getInventory().getSaddle() != null, ((Horse) livingEntity2).getInventory().getSaddle() != null);
                if (compareBooleans2 != 0) {
                    return compareBooleans2;
                }
            } else if (livingEntity.getType() == EntityType.PIG && livingEntity2.getType() == EntityType.PIG) {
                final int compareBooleans3 = this.compareBooleans(((Pig) livingEntity).hasSaddle(), ((Pig) livingEntity2).hasSaddle());
                if (compareBooleans3 != 0) {
                    return compareBooleans3;
                }
            }
            final int compareBooleans4 = this.compareBooleans(livingEntity.getCustomName() != null, livingEntity2.getCustomName() != null);
            if (compareBooleans4 != 0) {
                return compareBooleans4;
            }
            if (livingEntity.getTicksLived() > livingEntity2.getTicksLived()) {
                return -1;
            }
            if (livingEntity.getTicksLived() < livingEntity2.getTicksLived()) {
                return 1;
            }
            return livingEntity.getEntityId() - livingEntity2.getEntityId();
        }

        private int compareBooleans(final boolean b, final boolean b2) {
            if (b && b2 == false) {
                return 1;
            }
            if (b == false && b2) {
                return -1;
            }
            return 0;
        }
    }

    // REDSTONE LAG FIX

    private long lastTime = 0;
    private int redcount;
    private final int maxDevices = 750;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPistonRetract(final BlockPistonRetractEvent e) {
        if (this.lastTime + 1000L < System.currentTimeMillis()) {
            this.lastTime = System.currentTimeMillis();
            this.redcount = 0;
        }
        this.redcount += 1;
        if (this.redcount > this.maxDevices) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPistonExtend(final BlockPistonExtendEvent e) {
        if (this.lastTime + 1000L < System.currentTimeMillis()) {
            this.lastTime = System.currentTimeMillis();
            this.redcount = 0;
        }
        this.redcount += 1;
        if (this.redcount > this.maxDevices) {
            e.setCancelled(true);
        }
    }

    final Map<Chunk, Integer> wirespam = new HashMap<>();
    final Map<Chunk, Integer> devicespam = new HashMap<>();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onRedstone(final BlockRedstoneEvent e) {
        if (admin.REDSTONE.isActive() == false) {
            e.setNewCurrent(0);
            return;
        }
        if (this.lastTime + 1000L < System.currentTimeMillis()) {
            this.lastTime = System.currentTimeMillis();
            this.redcount = 0;
            devicespam.clear();
            wirespam.clear();
        }
        if (e.getBlock().getType() == Material.REDSTONE_WIRE) {
            int amount = wirespam.getOrDefault(e.getBlock().getChunk(), 0);
            wirespam.put(e.getBlock().getChunk(), amount + 1);
            if (amount > 250) {
                e.setNewCurrent(0);
                return;
            }
        }
        switch (e.getBlock().getType()) {
            case PISTON_STICKY_BASE:
            case PISTON_BASE:
            case WOODEN_DOOR:
            case IRON_DOOR_BLOCK:
            case FENCE_GATE:
            case REDSTONE_LAMP_OFF:
            case REDSTONE_LAMP_ON:
            case IRON_TRAPDOOR:
            case SPRUCE_FENCE_GATE:
            case BIRCH_FENCE_GATE:
            case JUNGLE_FENCE_GATE:
            case DARK_OAK_FENCE_GATE:
            case ACACIA_FENCE_GATE:
            case SPRUCE_DOOR:
            case BIRCH_DOOR:
            case JUNGLE_DOOR:
            case ACACIA_DOOR:
            case DARK_OAK_DOOR: {
                int amount = devicespam.getOrDefault(e.getBlock().getChunk(), 0);
                devicespam.put(e.getBlock().getChunk(), amount + 1);
                this.redcount += 1; // Global Limit
                break;
            }
            case REDSTONE_WIRE:
            case POWERED_RAIL:
            case LEVER:
            case STONE_PLATE:
            case WOOD_PLATE:
            case STONE_BUTTON:
            case WOOD_BUTTON:
            case GOLD_PLATE:
            case IRON_PLATE: {
                int amount = devicespam.getOrDefault(e.getBlock().getChunk(), 0);
                if (amount > 40) {
                    e.setNewCurrent(0);
                    return;
                } else if (this.redcount > this.maxDevices) { // Global Limit
                    e.setNewCurrent(0);
                }
                break;
            }
            default:
                break;
        }
    }

    // PACKET SPAM LAG FIX

    @EventHandler
    public void onItemHeld(final PlayerItemHeldEvent e) {
        final String name = e.getPlayer().getName();
        Long avg = switchavg.get(name);
        Long last = switchlast.get(name);
        Integer i = switchcount.get(name);
        if (i == null) i = 0;
        if (avg == null) avg = 0L;
        if (last == null) last = 0L;
        if (i > 200) {
            if (avg / 200L < 50000L) {
                e.getPlayer().kickPlayer("§cStop!");
                return;
            }
            switchcount.remove(name);
            switchavg.remove(name);
        } else {
            if (last != 0) {
                switchavg.put(name, avg + (System.nanoTime() - last));
            }
            switchcount.put(name, i + 1);
            switchlast.put(name, System.nanoTime());
        }
    }

    @EventHandler
    public void onAnimation(final PlayerAnimationEvent e) {
        final String name = e.getPlayer().getName();
        Long avg = hitsavg.get(name);
        Long last = hitslast.get(name);
        Integer i = hitscount.get(name);
        if (i == null) i = 0;
        if (avg == null) avg = 0L;
        if (last == null) last = 0L;
        if (i > 200) {
            if (avg / 200L < 50000L) {
                e.getPlayer().kickPlayer("§cStop!");
                return;
            }
            hitscount.remove(name);
            hitsavg.remove(name);
        } else {
            if (last != 0) {
                hitsavg.put(name, avg + (System.nanoTime() - last));
            }
            hitscount.put(name, i + 1);
            hitslast.put(name, System.nanoTime());
        }
    }

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent e) {
        final String name = e.getPlayer().getName();
        Long avg = placeavg.get(name);
        Long last = placelast.get(name);
        Integer i = placecount.get(name);
        if (i == null) i = 0;
        if (avg == null) avg = 0L;
        if (last == null) last = 0L;
        if (i > 200) {
            if (avg / 200L < 50000L) {
                e.getPlayer().kickPlayer("§cStop!");
                return;
            }
            placecount.remove(name);
            placeavg.remove(name);
        } else {
            if (last != 0) {
                placeavg.put(name, avg + (System.nanoTime() - last));
            }
            placecount.put(name, i + 1);
            placelast.put(name, System.nanoTime());
        }
    }

    @EventHandler
    public void onCreativeInventory(final InventoryCreativeEvent e) {
        final String name = e.getWhoClicked().getName();
        Long avg = placeavg.get(name);
        Long last = placelast.get(name);
        Integer i = placecount.get(name);
        if (i == null) i = 0;
        if (avg == null) avg = 0L;
        if (last == null) last = 0L;
        if (i > 200) {
            if (avg / 200L < 50000L) {
                ((Player) e.getWhoClicked()).kickPlayer("§cStop!");
                return;
            }
            placecount.remove(name);
            placeavg.remove(name);
        } else {
            if (last != 0) {
                placeavg.put(name, avg + (System.nanoTime() - last));
            }
            placecount.put(name, i + 1);
            placelast.put(name, System.nanoTime());
        }
    }

    public void resetPacketCooldown() {
        switchavg.clear();
        switchcount.clear();
        switchlast.clear();
        hitsavg.clear();
        hitscount.clear();
        hitslast.clear();
        placeavg.clear();
        placecount.clear();
        placelast.clear();
    }

    // CHUNK CLEANUP
    private final int ignoreDistance = 15;
    private TickLimiter limiter;
    private int cleanuptask;

    public void startChunkCleanup(final MuxSystem ms) {
        this.limiter = new TickLimiter(15);
        int interval = 600;
        this.cleanuptask = new BukkitRunnable() {
            @Override
            public void run() {
                limiter.initTick();
                final Set<ChunkInfo> loadedChunks = new HashSet<>();
                final Set<Player> players = new HashSet<>();
                for (final World world : ms.getServer().getWorlds()) {
                    for (final Chunk chunk : world.getLoadedChunks()) {
                        loadedChunks.add(new ChunkInfo(chunk));
                    }
                    players.addAll(world.getPlayers());
                }
                final int initiallyLoaded = loadedChunks.size();
                final Set<ChunkInfo> ignoredChunks = new HashSet<>(), centerChunks = new HashSet<>();
                for (final Player player : players) {
                    final Chunk chunk = player.getLocation().getChunk();
                    final int chunkX = chunk.getX(), chunkZ = chunk.getZ();
                    final ChunkInfo centerChunk = new ChunkInfo(chunk);
                    if (centerChunks.contains(centerChunk)) {
                        continue;
                    }
                    centerChunks.add(centerChunk);
                    final int radius = ignoreDistance, radiusSquared = ignoreDistance * ignoreDistance;
                    for (int dx = -radius; dx <= radius; ++dx) {
                        for (int dz = -radius; dz <= radius; ++dz) {
                            final double d2 = dx * dx + dz * dz;
                            if (d2 <= radiusSquared) {
                                ignoredChunks.add(new ChunkInfo(chunk.getWorld(), chunkX + dx, chunkZ + dz));
                            }
                        }
                    }
                }
                loadedChunks.removeAll(ignoredChunks);
                if (ms.isDebug())
                    System.out.println("ChunkCleanup> Ignored " + (initiallyLoaded - loadedChunks.size()) + " chunks out of initial " + initiallyLoaded + " in " + limiter.elapsedTime() + "ms");
                int cleanedChunks = 0;
                for (final ChunkInfo ci : loadedChunks) {
                    if (limiter.shouldContinue() == false) {
                        break;
                    }
                    final ChunkUnloadEvent event = new ChunkUnloadEvent(ci.get());
                    ms.getServer().getPluginManager().callEvent(event);
                    if (event.isCancelled() || ci.unload(true) == false) {
                        continue;
                    }
                    ++cleanedChunks;
                }
                if (ms.isDebug())
                    System.out.println("ChunkCleanup> Unloaded " + cleanedChunks + "/" + loadedChunks.size() + " non-ignored chunks in " + limiter.elapsedTime() + "ms");
            }
        }.runTaskTimer(ms, interval, interval).getTaskId();
    }

    public void stopChunkCleanup(final MuxSystem ms) {
        ms.getServer().getScheduler().cancelTask(cleanuptask);
    }

    final class QueuedDroppedItem {
        private SimpleLocation location;
        private MaterialData materialData;

        public QueuedDroppedItem(SimpleLocation location, MaterialData materialData) {
            this.location = location;
            this.materialData = materialData;
        }

        public boolean drop(int amount) {
            if (isStopLag())
                return false;
            Item droppedItem = this.location.bukkitLocation.getWorld().dropItem(this.location.bukkitLocation, new ItemStack(this.materialData.getItemType(), amount, this.materialData.getData()));
            net.minecraft.server.v1_8_R3.Entity entityItem = ((CraftItem) droppedItem).getHandle();
            if (materialData.getItemType() == Material.ROTTEN_FLESH) { // remove moneyfarm dropped items faster
                try {
                    itemAgeField.set(entityItem, 4000);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    itemAgeField.set(entityItem, 2000);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            QueuedDroppedItem that = (QueuedDroppedItem) o;
            return materialData.equals(that.materialData) && this.location.equals(that.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(materialData);
        }
    }

    final class TickLimiter {
        private long startTime;
        private final int maxTick;

        public TickLimiter(final int maxTick) {
            this.maxTick = maxTick;
        }

        public void initTick() {
            this.startTime = System.currentTimeMillis();
        }

        public boolean shouldContinue() {
            return elapsedTime() < this.maxTick;
        }

        public long elapsedTime() {
            return System.currentTimeMillis() - this.startTime;
        }
    }

    final class ChunkInfo {
        private final World world;
        public final int x, z;

        public ChunkInfo(final Chunk chunk) {
            this.x = chunk.getX();
            this.z = chunk.getZ();
            this.world = chunk.getWorld();
        }

        public ChunkInfo(final World world, final int x, final int z) {
            this.world = world;
            this.x = x;
            this.z = z;
        }

        @Override
        public String toString() {
            return this.world.getName() + ":" + this.x + "," + this.z;
        }

        public Chunk get() {
            return this.world.getChunkAt(this.x, this.z);
        }

        public boolean unload(final boolean save) {
            return this.world.unloadChunk(this.x, this.z, save, false);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final ChunkInfo chunkInfo = (ChunkInfo) o;
            if (this.x != chunkInfo.x) {
                return false;
            }
            if (this.z != chunkInfo.z) {
                return false;
            }
            return Objects.equals(this.world, chunkInfo.world);
        }

        @Override
        public int hashCode() {
            int result = this.world != null ? this.world.hashCode() : 0;
            result = 31 * result + this.x;
            result = 31 * result + this.z;
            return result;
        }
    }

    final class SimpleLocation {
        private final String world;
        private final int x, y, z;
        private final Location bukkitLocation;

        public SimpleLocation(Location bukkitLocation) {
            this.bukkitLocation = bukkitLocation;
            this.world = bukkitLocation.getWorld().getName();
            this.x = bukkitLocation.getBlockX();
            this.y = bukkitLocation.getBlockY();
            this.z = bukkitLocation.getBlockZ();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimpleLocation that = (SimpleLocation) o;
            return x == that.x && y == that.y && z == that.z && Objects.equals(world, that.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, x, y, z);
        }
    }
}