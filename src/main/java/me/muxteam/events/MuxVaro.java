package me.muxteam.events;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.object.number.MutableLong;
import com.boydti.fawe.object.schematic.Schematic;
import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.shop.MuxMining;
import org.apache.commons.io.FileUtils;
import org.bukkit.Difficulty;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class MuxVaro extends Event implements Listener {
    private Location netherspawn, spawn;
    private VaroData data;
    private Schematic schematic;
    private World overworld, nether;
    private int sec = 130, tp = 0;
    private final MuxScoreboard.ScoreboardType sb;

    public MuxVaro(final MuxEvents e) {
        super(e);
        name = "MuxVaro";
        item = new ItemStack(Material.SAPLING);
        pvp = true;
        canSpectate = true;
        maxSpectatorDistance = 1450D;
        canMoveItems = true;
        canDrop = true;
        noHunger = false;
        breakBlocks = true;
        maxPlayers = 240;
        try {
            schematic = FaweAPI.load(new File(ms.getDataFolder() + "/event", "varo-portal.schematic"));
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
        final File worldFolder = new File(ms.getServer().getWorldContainer() + "/MuxVaro");
        final FileConfiguration hashYML = ms.getHashYAML();
        if (hashYML.getBoolean("varoeventused")) {
            hashYML.set("varoeventready", null);
            hashYML.set("varoeventused", null);
            ms.saveHashFile();
            try {
                FileUtils.deleteDirectory(worldFolder);
                FileUtils.deleteDirectory(new File(ms.getServer().getWorldContainer() + "/MuxVaroNether"));
            } catch (final IOException ex) {
                ex.printStackTrace();
            }
        }
        if (hashYML.getBoolean("varoeventready") == false) {
            generate();
            overworld.setDifficulty(Difficulty.NORMAL);
            nether.setDifficulty(Difficulty.NORMAL);
            overworld.save();
            nether.save();
        }
        if (overworld != null) {
            ms.getServer().unloadWorld(overworld, true);
            overworld = null;
        }
        if (nether != null) {
            ms.getServer().unloadWorld(nether, true);
            nether = null;
        }
        sb = ms.getScoreboard().createScoreboard("§7§lMux§d§lVaro");
        sb.setLook((p, board) -> {
            sb.setLine(board, "");
            sb.setSection(board, "§c§lVerbleibend", players.size() + " Spieler", false);
            sb.setLine(board, " ");
            sb.setSection(board, "§7§lBorder      ", ms.getNumberFormat((int) overworld.getWorldBorder().getSize()) + " Blöcke", false);
            sb.setLine(board, "  ");
            if (data.gameState == VaroGameState.INGAME_PROTECTION) {
                sb.setSection(board, "§a§lSchutzphase", "§fnoch " + ms.getTime(sec), false);
            } else if (data.gameState == VaroGameState.INGAME) {
                if (sec < 900) {
                    sb.setSection(board, "§6§lVerkleinerung", "§fin " + ms.getTime(900 - sec), false);
                } else {
                    int border = 2700;
                    if (sec < border) {
                        sb.setSection(board, "§e§lVerkleinerung", "§fnoch " + ms.getTime(border - sec), false);
                    } else {
                        if (tp >= 3) {
                            int base = 5270;
                            sb.setSection(board, "§c§lEnde", "§fin " + ms.getTime(base - sec), false);
                        } else {
                            int base = 3310;
                            if (sec <= base)
                                sb.setSection(board, "§6§lTeleportation", "§fin " + ms.getTime(base - sec), false);
                            else {
                                while (base < sec) base += 300;
                                sb.setSection(board, "§6§lTeleportation", "§fin " + ms.getTime(base - sec), false);
                            }
                        }
                    }
                }
            }
        });
        spectatorSB = sb;
        maxDuration = 5400000L;
    }

    private void death(final Player p) {
        resetPlayer(p);
        players.forEach(player -> ms.sendScoreboard(player, sb));
        if (cancelled || canjoin) return;
        receiveBonus(p, players.size() + 1);
        sendEventMessage("§7Der Spieler §d" + p.getName() + " §7ist §causgeschieden§7.");
        ms.showItemBar(p, "§cDu bist aus dem Event ausgeschieden.");
        ms.sendTitle(p, "§d§lMuxVaro", 10, 80, 10);
        ms.sendSubTitle(p, "§fDein Platz: §d#" + (players.size() + 1), 10, 80, 10);
        if (players.size() == 1 && (data.gameState == VaroGameState.INGAME || data.gameState == VaroGameState.INGAME_PROTECTION)) {
            final Player winner = players.iterator().next();
            data.won = true;
            resetPlayer(winner);
            ms.sendTitle(winner, "§d§lMuxVaro", 10, 80, 10);
            ms.sendSubTitle(winner, "§fDu hast das Event §agewonnen§f!", 10, 80, 10);
            ms.broadcastMessage("§d§lMuxVaro>§7 Der Spieler §d" + winner.getName() + " §7hat §agewonnen§7!", null, MuxSystem.Priority.LOW);
            receiveBonus(winner, 1);
            stopEvent(true);
        }
    }

    private void receiveBonus(final Player p, final int pos) {
        final int baseAmount = pos == 1 ? 2000 : (pos == 2 ? 100 : (pos == 3 ? 50 : 5));
        if (startedByPlayer == false) {
            final MutableLong coins = new MutableLong();
            coins.set(giveReward(p, baseAmount, MuxMining.OreTypes.DIAMOND, false));
            ms.runLater(new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline() == false) return;
                    ms.sendScoreboard(p);
                    ms.sendSubTitle(p, "§fDeine Belohnung: §e§l" + ms.getNumberFormat(coins.get()) + " MuxCoins", 0, 60, 10);
                }
            }, 60L);
            if (pos == 1) {
                ms.broadcastMessage("§d§lMuxVaro>§7 Der Gewinn der Woche: §e" + ms.getNumberFormat(coins.get()) + " MuxCoins", null, MuxSystem.Priority.LOW);
            }
        }
    }

    private boolean hasMovedSignificant(final Location pos1, final Location pos2, final double max) {
        if (max > 1 && pos1.getBlockX() == pos2.getBlockX() && pos1.getBlockZ() == pos2.getBlockZ()) return false;
        return pos1.distanceSquared(pos2) > max * max;
    }

    private void generateSpawnLocations(final World world) {
        final Location center = overworld.getSpawnLocation();
        final double increment = (2 * Math.PI) / getMaxPlayers();
        for (int i = 0; i < getMaxPlayers(); i++) {
            final double angle = i * increment, x = center.getX() + (50 * Math.cos(angle)), z = center.getZ() + (50 * Math.sin(angle));
            final Location loc = new Location(world, x, overworld.getHighestBlockYAt((int) x, (int) z), z);
            loc.setYaw(getDirection(loc, world.getSpawnLocation()));
            data.locations.add(loc.add(0.5, 0, 0.5));
            loc.clone().subtract(0, 1, 0).getBlock().setType(Material.COAL_BLOCK);
        }
    }

    private Float getDirection(final Location source, final Location destination) {
        final Vector inBetween = destination.clone().subtract(source).toVector(), lookVec = source.getDirection();
        double angleDir = (Math.atan2(inBetween.getZ(), inBetween.getX()) / 2 / Math.PI * 360 + 360) % 360, angleLook = (Math.atan2(lookVec.getZ(), lookVec.getX()) / 2 / Math.PI * 360 + 360) % 360;
        return (float) ((angleDir - angleLook + 360) % 360);
    }

    @Override
    public long getUpdateTime() {
        return 20L;
    }

    @Override
    public void update() {
        if (canjoin) {
            return;
        }
        if (data.gameState == VaroGameState.INGAME_PROTECTION) {
            for (final Player player : players) {
                ms.sendScoreboard(player, sb);
            }
            sec--;
            if (sec == 0) {
                sendEventBar("§fDie Schutzphase ist §ejetzt vorbei§f!", null);
                players.forEach(player -> {
                    ms.sendTitle(player, "§d§lMuxVaro", 0, 130, 20);
                    ms.sendSubTitle(player, "§cSchaden ist jetzt aktiviert!", 0, 60, 20);
                    player.playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 1.5F, 2.5F);
                    data.gameState = VaroGameState.INGAME;
                });
            } else if (sec % (sec < 30 ? 10 : 30) == 0) {
                sendEventBar("§fDie Schutzphase endet in §e" + sec + " Sekunden§f.", Sound.NOTE_STICKS);
            } else if (sec < 5) {
                sendEventBar("§fDie Schutzphase endet in §e" + sec + " Sekunden§f.", Sound.NOTE_STICKS);
            }
        } else {
            for (final Player player : players) {
                ms.sendScoreboard(player, sb);
            }
            sec++;

            if (sec == 900) {
                nether.getWorldBorder().setSize(50, 1800L);
                overworld.getWorldBorder().setSize(50, 1800L);
                players.forEach(player -> {
                    ms.sendTitle(player, "§d§lMuxVaro", 0, 90, 20);
                    ms.sendSubTitle(player, "§eDie Border verkleinert sich nun.", 0, 90, 20);
                    player.playSound(player.getLocation(), Sound.WITHER_IDLE, 1, 1);
                });
            }
            if (sec >= 3300) {
                if (sec == 3300 || sec == 3600 || sec == 3900) {
                    players.forEach(player -> {
                        ms.sendTitle(player, "§d§lMuxVaro", 0, 90, 20);
                        ms.sendSubTitle(player, "§6Du wirst gleich in die Mitte teleportiert.", 0, 90, 20);
                        player.playSound(player.getLocation(), Sound.WITHER_IDLE, 1, 1);
                    });
                }
                if (sec == 3310 || sec == 3610 || sec == 3910) {
                    tp++;
                    players.forEach(player -> ms.forcePlayer(player, spawn));
                }
            }
        }
    }

    @Override
    public Location getSpectateLocation() {
        return overworld.getSpawnLocation().clone().add(0D, 20D, 0D);
    }

    @Override
    public void start() {
        if (overworld == null) generate();
        data = new VaroData();
        sec = 130;
        tp = 0;
        generateSpawnLocations(overworld);
        Collections.shuffle(data.locations);
        data.unusedLocations.addAll(data.locations);
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das MuxVaro §7Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        ImmutableList.copyOf(overworld.getEntities()).stream().filter(entity -> entity.getType() == EntityType.DROPPED_ITEM).forEach(Entity::remove);
        ImmutableList.copyOf(nether.getEntities()).stream().filter(entity -> entity.getType() == EntityType.DROPPED_ITEM).forEach(Entity::remove);
        startCountdown(60, false, false);
        spawnEventNPC("§d§lEvent beitreten");
    }

    @Override
    public boolean join(final Player p) {
        Location location = data.unusedLocations.remove();
        data.usedLocations.put(p, location);
        if (location == null) return false;
        if (data.portal == false) {
            data.portal = true;
            new BukkitRunnable() {
                @Override
                public void run() {
                    generateNetherPortal();
                }
            }.runTaskLater(ms, 20L);
        }
        equipPlayer(p);
        ms.forcePlayer(p, location.clone().add(0, 3D, 0));
        return true;
    }

    @Override
    public void updateCountDown(final int sec) {
        if (sec == 0) {
            if (players.size() < 2) return;
            data.gameState = VaroGameState.INGAME_PROTECTION;
            overworld.getWorldBorder().setCenter(0, 0);
            overworld.getWorldBorder().setSize(2000);
            nether.getWorldBorder().setCenter(0, 0);
            nether.getWorldBorder().setSize(2000);
            for (final Player player : players) {
                ms.sendTitle(player, "§d§lMuxVaro", 0, 40, 20);
                ms.sendSubTitle(player, "§aDas Event hat begonnen!", 0, 40, 20);
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1.5F, 2.5F);
                ms.sendScoreboard(player, sb);
            }
            sendEventMessage("§d§lMuxVaro>§a Für die nächsten §e2 Minuten§a läuft eine Schutzphase.");
        }
    }

    @Override
    public void quit(final Player p) {
        data.unusedLocations.add(data.usedLocations.remove(p));
        if (cancelled == false && canjoin == false && data.won == false) {
            final PlayerInventory pi = p.getInventory();
            final List<ItemStack> drops = new ArrayList<>();
            for (final ItemStack i : pi.getContents()) {
                if (i != null && i.getType() != Material.AIR) drops.add(i);
            }
            for (final ItemStack i : pi.getArmorContents()) {
                if (i != null && i.getType() != Material.AIR) drops.add(i);
            }
            drops.stream().filter(itemStack -> itemStack.getType() == Material.NETHER_STAR == false).forEach(itemStack -> p.getWorld().dropItemNaturally(p.getLocation(), itemStack));
        }
        death(p);
    }

    @Override
    public void stop() {
        ms.getHashYAML().set("varoeventready", true);
        if (playerCountAtStart > 1) ms.getHashYAML().set("varoeventused", true);
        ms.saveHashFile();
        overworld.getPlayers().forEach(player -> ms.forcePlayer(player, ms.getGameSpawn()));
        nether.getPlayers().forEach(player -> ms.forcePlayer(player, ms.getGameSpawn()));
        ms.getServer().unloadWorld(overworld, false);
        ms.getServer().unloadWorld(nether, false);
        data = null;
        overworld = null;
        nether = null;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Willkommen in der natürlichen Welt! Rüste",
                "§7dich schnell auf, um am Ende der letzte",
                "§7Überlebende zu sein.",
                "",
                "§7Überlebende: §d" + players.size()
        };
    }

    @Override
    public void generate() {
        final Server sr = ms.getServer();
        if (ms.getHashYAML().getBoolean("varoeventready")) {
            generateOverworld();
            nether = sr.createWorld(new WorldCreator("MuxVaroNether").environment(World.Environment.NETHER));
            nether.setSpawnLocation(0, 60, 0);
            nether.setAutoSave(false);
            return;
        }
        int tries = 0;
        generateOverworld();

        while (containsBadBiome(overworld)) {
            sr.unloadWorld(overworld, false);
            if (sr.getWorld("MuxVaroNether") != null) sr.unloadWorld("MuxVaroNether", false);
            final File worldFolder = new File(sr.getWorldContainer() + "/MuxVaro"),
                    netherFolder = new File(sr.getWorldContainer() + "/MuxVaroNether");
            try {
                if (worldFolder.exists()) FileUtils.deleteDirectory(worldFolder);
                if (netherFolder.exists()) FileUtils.deleteDirectory(netherFolder);
            } catch (final IOException e) {
                e.printStackTrace();
            }
            if (tries == 10) break;
            tries++;
            generateOverworld();
        }
        nether = sr.createWorld(new WorldCreator("MuxVaroNether").environment(World.Environment.NETHER));
        nether.setSpawnLocation(0, 60, 0);
        nether.setAutoSave(false);
        ms.getHashYAML().set("varoeventready", true);
        ms.saveHashFile();
    }

    private void generateOverworld() {
        overworld = ms.getServer().createWorld(new WorldCreator("MuxVaro").environment(World.Environment.NORMAL));
        overworld.setAutoSave(false);
    }

    private boolean containsBadBiome(final World world) {
        final List<Biome> badBiomes = Arrays.asList(Biome.BEACH, Biome.OCEAN, Biome.DEEP_OCEAN, Biome.FROZEN_OCEAN, Biome.BEACH, Biome.COLD_BEACH, Biome.STONE_BEACH,
                Biome.ICE_MOUNTAINS, Biome.ICE_PLAINS, Biome.ICE_PLAINS_SPIKES);
        for (int x = -300; x < 300; x = x + 16) {
            for (int z = -300; z < 300; z += 16) {
                if (badBiomes.contains(world.getBiome(x, z))) return true;
            }
        }
        return false;
    }

    private void generateNetherPortal() {
        overworld.setSpawnLocation(0, overworld.getHighestBlockYAt(0, 0), 0);
        Location loc = overworld.getHighestBlockAt(0, 0).getLocation();
        loc.getWorld().loadChunk(loc.getChunk());
        com.sk89q.worldedit.Vector vector = new com.sk89q.worldedit.Vector(loc.getX(), loc.getY(), loc.getZ());
        schematic.paste(new BukkitWorld(overworld), vector);
        spawn = loc.clone().add(2, 1, 2);
        loc = nether.getSpawnLocation();
        loc.getWorld().loadChunk(loc.getChunk());
        vector = new com.sk89q.worldedit.Vector(loc.getX(), loc.getY(), loc.getZ());
        schematic.paste(new BukkitWorld(nether), vector);
        netherspawn = loc.clone().add(2, 1, 2);
    }

    @EventHandler
    public void onCraft(final CraftItemEvent e) {
        final Player player = (Player) e.getWhoClicked();
        if (players.contains(player) == false) return;
        final ItemStack result = e.getRecipe().getResult();
        if (result.getType() == Material.GOLDEN_APPLE && result.getDurability() != 0) {
            e.setCancelled(true);
            ms.showItemBar(player, "§cDieses Item ist im Varo deaktiviert.");
        }
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        if (players.contains(e.getPlayer()) && e.getClickedBlock() != null && e.getAction() == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock().getType() == Material.BREWING_STAND) {
            ms.showItemBar(e.getPlayer(), "§cBraustände sind in Varo deaktiviert.");
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFood(final FoodLevelChangeEvent e) {
        if (players.contains((Player) e.getEntity()) && data.gameState != VaroGameState.INGAME) {
            e.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onDeath(final PlayerDeathEvent e) {
        final Player p = e.getEntity();
        if (players.contains(p) == false) return;
        e.getDrops().removeIf(itemStack -> itemStack.getType() == Material.NETHER_STAR);
        new BukkitRunnable() {
            @Override
            public void run() {
                death(p);
            }
        }.runTaskLater(ms, 5L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(final PlayerMoveEvent e) {
        final Player player = e.getPlayer();
        if (players.contains(player)) {
            final Location from = e.getFrom(), to = e.getTo();
            if (data.gameState == VaroGameState.WAITING && hasMovedSignificant(to, overworld.getSpawnLocation(), 110D)) {
                e.setTo(from);
                final Location to2 = to.clone().add(0, 1, 0);
                player.playEffect(to2, Effect.SMOKE, 4);
                player.playEffect(to2, Effect.SMOKE, 4);
                player.playEffect(to2, Effect.SMOKE, 4);
                ms.showItemBar(player, "§cDas Event startet gleich.");
            }
        }
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent e) {
        final Player player = e.getPlayer();
        if (players.contains(player) == false) return;
        else if (data.gameState == VaroGameState.WAITING || tp >= 3) {
            e.setCancelled(true);
            return;
        }
        e.setCancelled(false);
        final Block block = e.getBlock();
        if (block.getType() == Material.OBSIDIAN || block.getType() == Material.PORTAL) {
            if (block.getLocation().distanceSquared(player.getWorld() == spawn.getWorld() ? spawn : netherspawn) <= 20 * 20) {
                player.playSound(player.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                ms.showItemBar(player, "§cDu kannst in der Mitte diesen Block nicht abbauen.");
                e.setCancelled(true);
                return;
            }
        }
        if (block.getLocation().distanceSquared(player.getWorld() == spawn.getWorld() ? spawn : netherspawn) <= 8 * 8) {
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 1F, 1F);
            ms.showItemBar(player, "§cDu kannst nicht direkt am Portal bauen.");
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent e) {
        final Player player = e.getPlayer();
        if (players.contains(player) == false) return;
        else if (data.gameState == VaroGameState.WAITING || tp >= 3) {
            e.setCancelled(true);
            return;
        }
        e.setCancelled(false);
        final Block block = e.getBlock();
        if (block.getType() == Material.OBSIDIAN) {
            ms.showItemBar(player, "§cDu kannst diesen Block nicht platzieren.");
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 1, 1);
            e.setCancelled(true);
        } else if (block.getLocation().distanceSquared(player.getWorld() == spawn.getWorld() ? spawn : netherspawn) <= 5 * 5) {
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 1, 1);
            ms.showItemBar(player, "§cDu kannst nicht direkt am Portal bauen.");
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(final EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            final Player player = (Player) e.getEntity();
            if (players.contains(player) == false) {
                return;
            }
            if (data.gameState == VaroGameState.WAITING || data.gameState == VaroGameState.INGAME_PROTECTION) {
                e.setCancelled(true);
            } else if (data.gameState == VaroGameState.INGAME) {
                e.setCancelled(false);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) {
            final Player player = (Player) e.getDamager();
            if (players.contains(player) && data.gameState == VaroGameState.WAITING) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPortal(final PlayerPortalEvent e) {
        final Player player = e.getPlayer();
        if (players.contains(player) == false) {
            return;
        }
        if (data.gameState == VaroGameState.WAITING || tp >= 3) {
            e.setCancelled(true);
            return;
        }
        e.setCancelled(false);
        e.setTo(player.getWorld() == nether ? spawn : netherspawn);
    }

    class VaroData {
        private VaroGameState gameState = VaroGameState.WAITING;
        private final List<Location> locations = new ArrayList<>();
        private final Queue<Location> unusedLocations = new LinkedList<>();
        private final Map<Player, Location> usedLocations = new HashMap<>();
        private boolean portal = false, won = false;
    }

    enum VaroGameState {
        WAITING, INGAME_PROTECTION, INGAME
    }
}