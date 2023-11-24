package me.muxteam.events;

import com.boydti.fawe.object.number.MutableLong;
import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.shop.MuxMining;
import org.apache.commons.io.FileUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class MuxSurvivalGames extends Event implements Listener {
    private boolean move, deathmatch;
    private final List<Short> breakableblocks = new ArrayList<>(Arrays.asList(new Short[]{18, 30, 31, 37, 38, 39, 40, 59, 106, 141, 175})),
            placeableblocks = new ArrayList<>(Arrays.asList(new Short[]{30, 46, 92})),
            normalitems = new ArrayList<>(Arrays.asList(new Short[]{30, 46, 260, 261, 262, 265, 266, 268, 271, 275, 280, 281, 283, 288, 296, 298, 299, 300, 301, 302, 303, 304, 305, 314, 315, 316, 317, 319, 334, 346, 349, 352, 357, 363, 365, 384, 391})),
            gooditems = new ArrayList<>(Arrays.asList(new Short[]{264, 272, 282, 306, 307, 308, 309, 320, 350, 354, 364, 393, 400}));
    private final int normalsize = 37, goodsize = 13;
    private final List<Location> spawnlocations = new ArrayList<>();
    private final Map<Player, Location> usedLocations = new HashMap<>();
    private World dmworld = null;
    private final File sgFile;
    private final FileConfiguration sgYML;
    private final Map<Location, Inventory> chests = new HashMap<>();
    private Location loc = null, middle = null, sdm = null;
    private final MuxScoreboard.ScoreboardType sb;

    public MuxSurvivalGames(final MuxEvents e) {
        super(e);
        name = "MuxSurvivalGames";
        item = new ItemStack(Material.GOLD_SWORD);
        canSpectate = true;
        maxSpectatorDistance = 400D;
        canMoveItems = true;
        breakBlocks = true;
        canDrop = true;
        pvp = true;
        noHunger = false;
        sgFile = new File(ms.getDataFolder() + "/event", "sg.yml");
        sgYML = YamlConfiguration.loadConfiguration(sgFile);
        sb = ms.getScoreboard().createScoreboard("§7§lMux§d§lSurvivalGames");
        sb.setLook((p, board) -> {
            sb.setLine(board, "§b");
            sb.setSection(board, "§c§lVerbleibend        ", players.size() + " Spieler", false);
            sb.setLine(board, "");
            if (deathmatch) {
                sb.setSection(board, "§6§lEnde", "in " + ms.getTime(firstiteration ? 360 : Math.max(deathmatchseconds, 0)), false);
            } else if (sgseconds >= 1770) {
                sb.setSection(board, "§a§lSchutzphase", "noch " + ms.getTime((sgseconds - 1770)), false);
            } else if (sgseconds >= 1080 && refill == false) {
                sb.setSection(board, "§a§lNachfüllung", "in " + ms.getTime((sgseconds - 1080)), false);
            } else {
                sb.setSection(board, "§e§lDeathmatch", "in " + ms.getTime(Math.max(sgseconds, dmcount)), false);
            }
        });
        spectatorSB = sb;
    }

    private void death(final Player p, final boolean quit) {
        if (quit) {
            death(p);
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                death(p);
            }
        }.runTaskLater(ms, 5L);
    }

    @Override
    public void stop() {
        final Server s = ms.getServer();
        spawnlocations.clear();
        usedLocations.clear();
        if (dmtask != null) dmtask.cancel();
        if (dmcdtask != null) {
            dmcdtask.cancel();
            dmcdtask = null;
        }
        if (gametask != null) gametask.cancel();
        if (refilltask != null) refilltask.cancel();
        loc.getWorld().getEntities().stream().filter(entity -> entity.getType() == EntityType.FALLING_BLOCK).forEach(org.bukkit.entity.Entity::remove);
        if (dmworld != null) {
            dmworld.getEntities().stream().filter(entity -> entity != null && entity.getType() == EntityType.DROPPED_ITEM).forEach(org.bukkit.entity.Entity::remove);
            dmworld.getPlayers().forEach(player -> ms.forcePlayer(player, ms.getGameSpawn()));
            s.unloadWorld(dmworld, false);
        }
        loc.getWorld().getPlayers().forEach(player -> ms.forcePlayer(player, ms.getGameSpawn()));
        s.unloadWorld(loc.getWorld(), false);
        File worldFolder = new File(ms.getServer().getWorldContainer() + "/MuxSurvivalGames");
        try {
            FileUtils.deleteDirectory(worldFolder);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        worldFolder = new File(ms.getServer().getWorldContainer() + "/MuxSurvivalGamesDM");
        if (worldFolder.exists())
            try {
                FileUtils.deleteDirectory(worldFolder);
            } catch (final IOException e) {
                e.printStackTrace();
            }
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Auf dieser Karte sind Kisten versteckt!",
                "§7Finde sie und rüste dich auf, damit du",
                "§7alle anderen Spieler besiegen kannst.",
                "",
                "§7Überlebende: §d" + players.size()
        };
    }
    @Override
    public String[] getAdminInformation() {
        return new String[] {
                "§7Setup Befehl: §d/sg"
        };
    }

    private void generateSpawnLocations(final World world) {
        final Location center = middle;
        final double increment = (2 * Math.PI) / 300;
        for (int i = 0; i < 300; i++) {
            final double angle = i * increment, x = center.getX() + (22 * Math.cos(angle)), z = center.getZ() + (22 * Math.sin(angle));
            final Location l = new Location(world, x, middle.getBlockY(), z);
            l.setYaw(getDirection(l, middle));
            spawnlocations.add(l.add(0, 2, 0));
        }
    }

    private Float getDirection(final Location source, final Location destination) {
        final Vector inBetween = destination.clone().subtract(source).toVector(), lookVec = source.getDirection();
        double angleDir = (Math.atan2(inBetween.getZ(), inBetween.getX()) / 2 / Math.PI * 360 + 360) % 360, angleLook = (Math.atan2(lookVec.getZ(), lookVec.getX()) / 2 / Math.PI * 360 + 360) % 360;
        return (float) ((angleDir - angleLook + 360) % 360);
    }

    @Override
    public int getMaxPlayers() {
        return 300;
    }

    @Override
    public Location getSpectateLocation() {
        return deathmatch ? sdm : loc;
    }

    @Override
    public void start() {
        sgseconds = 1800;
        refill = false;
        move = false;
        deathmatch = false;

        ms.unzip(ms.getServer().getWorldContainer() + "/MuxSurvivalGames.zip");
        ms.unzip(ms.getServer().getWorldContainer() + "/MuxSurvivalGamesDM.zip");
        final World w = ms.getServer().createWorld(new WorldCreator("MuxSurvivalGames").generateStructures(false).type(org.bukkit.WorldType.FLAT));
        w.getEntities().forEach(org.bukkit.entity.Entity::remove);
        w.setGameRuleValue("doMobSpawning", "false");
        w.setGameRuleValue("doFireTick", "false");
        w.setGameRuleValue("mobGriefing", "false");
        w.setAutoSave(false);
        w.setTime(0L);
        middle = new Location(w, -4, 70, 323);
        generateSpawnLocations(w);
        loc = spawnlocations.get(0);
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das MuxSurvivalGames Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        startCountdown(60, false, false);
        spawnEventNPC("§d§lEvent beitreten");
        w.getEntities().stream().filter(entity -> entity.getType() == EntityType.FALLING_BLOCK).forEach(org.bukkit.entity.Entity::remove);
    }

    @Override
    public boolean join(final Player p) {
        equipPlayer(p);
        final boolean b = spawnlocations.isEmpty();
        final Location pos = b ? loc : spawnlocations.get(r.nextInt(spawnlocations.size()));
        if (pos.getChunk().isLoaded() == false) {
            pos.getChunk().load();
        }
        spawnlocations.remove(pos);
        if (b)
            usedLocations.put(p, pos);
        ms.forcePlayer(p, pos.clone().add(0, 4, 0));
        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, Integer.MAX_VALUE, 200));
        return true;
    }

    @Override
    public void quit(final Player p) {
        if (usedLocations.containsKey(p)) spawnlocations.add(usedLocations.remove(p));
        if (move) {
            final PlayerInventory pi = p.getInventory();
            final List<ItemStack> drops = new ArrayList<>();
            for (final ItemStack i : pi.getContents()) {
                if (i != null && i.getType() != Material.AIR) drops.add(i);
            }
            for (final ItemStack i : pi.getArmorContents()) {
                if (i != null && i.getType() != Material.AIR) drops.add(i);
            }
            drops.stream().filter(itemStack -> itemStack.getType() == Material.NETHER_STAR == false).forEach(itemStack -> p.getWorld().dropItemNaturally(p.getLocation(), itemStack));
            players.forEach(player -> {
                player.showPlayer(p);
                p.showPlayer(player);
            });
            death(p, true);
        } else {
            players.forEach(player -> {
                player.showPlayer(p);
                p.showPlayer(player);
            });
            resetPlayer(p);
        }
    }

    private void death(final Player p) {
        resetPlayer(p);
        if (canjoin || cancelled) return;
        sendEventMessage("§7Der Spieler §d" + p.getName() + " §7ist §causgeschieden§7.");
        ms.showItemBar(p, "§cDu bist aus dem Event ausgeschieden.");
        players.forEach(player -> ms.sendScoreboard(player, sb));
        if (players.size() == 4 && deathmatch == false && dmcdtask == null) {
            gametask.cancel();
            dmcdtask(ms.getServer());
        } else if (players.size() == 1) {
            final Player winner = players.iterator().next();
            ms.sendTitle(winner, "§d§lMuxSG", 10, 80, 10);
            ms.sendSubTitle(winner, "§fDu hast das Event §agewonnen§f!", 10, 80, 10);
            ms.broadcastMessage("§d§lMuxSurvivalGames>§7 Der Spieler §d" + winner.getName() + " §7hat §agewonnen§7!", null, MuxSystem.Priority.LOW);
            resetPlayer(winner);
            if (startedByPlayer == false) {
                final MutableLong coins = new MutableLong();
                coins.set(giveReward(winner, 2000, MuxMining.OreTypes.DIAMOND, false));
                ms.runLater(new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (winner.isOnline() == false) return;
                        ms.sendScoreboard(winner);
                        ms.sendSubTitle(winner, "§fDeine Belohnung: §e§l" + ms.getNumberFormat(coins.get()) + " MuxCoins", 0, 60, 10);
                    }
                }, 60L);
                ms.broadcastMessage("§d§lMuxSurvivalGames>§7 Der Gewinn der Woche: §e" + ms.getNumberFormat(coins.get()) + " MuxCoins", null, MuxSystem.Priority.LOW);
            }
            stopEvent(true);
        }
    }

    @Override
    public boolean canDrop() {
        return canDrop && canjoin == false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreProcess(final PlayerCommandPreprocessEvent e) {
        if (ms.isTrusted(e.getPlayer().getName()) == false) return;
        final Player p = e.getPlayer();
        final String cmd = e.getMessage().toLowerCase().split(" ")[0];
        final String[] args = e.getMessage().toLowerCase().replace(cmd + " ", "").split(" ");
        if (cmd.equalsIgnoreCase("/sg")) {
            e.setCancelled(true);
            if (p.getWorld().getName().equals(loc.getWorld().getName()) == false) {
                ms.showItemBar(p, "§cDu musst dich in der Event Welt dafür befinden.");
            } else if (args[0].equalsIgnoreCase("c") || args[0].equalsIgnoreCase("chest")) {
                if (args.length < 3) {
                    ms.showItemBar(p, ms.usage("/sg c [chest-number]"));
                    return;
                }
                final Block b = p.getTargetBlock((HashSet<Byte>) null, 5);
                if (b.getType() != Material.PRISMARINE && b.getData() != 2) {
                    ms.showItemBar(p, "§cThe block you are targeting is not a crate!");
                    return;
                }
                try {
                    final byte cnum = Byte.parseByte(args[1]);
                    if (cnum < 1 || cnum > 100) {
                        throw new NumberFormatException();
                    }
                    final Location l = b.getLocation();
                    final String path = "map";
                    sgYML.set(path + ".fallingcrates." + cnum + ".x", l.getBlockX());
                    sgYML.set(path + ".fallingcrates." + cnum + ".y", l.getBlockY());
                    sgYML.set(path + ".fallingcrates." + cnum + ".z", l.getBlockZ());
                    sgYML.save(sgFile);
                    ms.showItemBar(p, "§fChest Location §d" + cnum + " §fhas been set!");
                } catch (final NumberFormatException ex) {
                    ms.showItemBar(p,"§cPlease enter a valid crate number.");
                } catch (final Exception ex) {
                    ms.showItemBar(p,"§cAn error occured! Look in the console!");
                    ex.printStackTrace();
                }
            } else {
                ms.showItemBar(p, ms.usage("/sg c [chest-number]"));
            }
        }
    }

    final List<Location> fallchests = new ArrayList<>();
    private short sgseconds = 1800;
    private BukkitTask gametask, refilltask, dmcdtask, dmtask;
    private byte hidecd = 3;
    private boolean fallingchests = false;

    @Override
    public void updateCountDown(final int sec) {
        if (sec == 0) {
            if (players.size() < 2) return;
            move = true;
            players.forEach(player -> {
                player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
                ms.sendScoreboard(player, sb);
            });
            if (fallingchests == false) {
                spawnFallingChests();
            }
            fallingchests = false;
            hidePlayers();
            game(ms.getServer());
            sendEventMessage("§d§lMuxSG>§a Für die nächsten §e30 Sekunden§a läuft eine Schutzphase.");
        } else if (sec == 30) {
            spawnFallingChests();
        }
        if (hidecd == 0) {
            hidecd = 4;
            hidePlayers();
        } else hidecd--;
    }

    private void hidePlayers() {
        for (final Player pl : players) {
            for (final Player pl2 : players) {
                if (pl == pl2 || pl.canSee(pl2) == false) continue;
                if (pl.getLocation().distance(pl2.getLocation()) <= 1.3)
                    pl.hidePlayer(pl2);
            }
        }
    }

    private void spawnFallingChests() {
        fallchests.clear();
        final String cpath = "map.fallingcrates";
        for (final String crate : sgYML.getConfigurationSection(cpath).getKeys(false)) {
            final Location l = new Location(loc.getWorld(), sgYML.getDouble(cpath + "." + crate + ".x"), sgYML.getDouble(cpath + "." + crate + ".y"), sgYML.getDouble(cpath + "." + crate + ".z"));
            fallchests.add(l);
        }
        for (final Location l : fallchests) {
            final FallingBlock block = loc.getWorld().spawnFallingBlock(l.add(0, 20, 0), 168, (byte) 2);
            block.setDropItem(false);
            block.setHurtEntities(false);
            l.subtract(0, 20, 0);
        }
        fallingchests = true;
    }

    private void game(final org.bukkit.Server s) {
        gametask = new BukkitRunnable() {
            private byte cooldown = 3;

            @Override
            public void run() {
                if (sgseconds == 0) {
                    dmcdtask(s);
                    cancel();
                } else if (sgseconds == 1080) {
                    refilltask(s);
                } else if (sgseconds == 1380) {
                    sendEventMessage("§d§lMuxSG>§7 Alle Supply Kisten werden §ain 5 Minuten§7 nachgefüllt.");
                } else if (sgseconds == 1770) {
                    sendEventBar("§fDie Schutzphase ist §ejetzt vorbei§f!", null);
                    players.forEach(player -> {
                        ms.sendTitle(player, "§d§lMuxSG", 0, 130, 20);
                        ms.sendSubTitle(player, "§cSchaden ist jetzt aktiviert!", 0, 60, 20);
                        player.playSound(player.getLocation(), Sound.ENDERDRAGON_GROWL, 1.5F, 2.5F);
                    });
                } else if (sgseconds > 1770 && sgseconds < 1775) {
                    sendEventBar("§fDie Schutzphase endet in §e" + ms.getTime((sgseconds - 1770)) + "§f.", Sound.NOTE_STICKS);
                }
                if (cooldown > 0) {
                    cooldown--;
                    if (cooldown == 0) {
                        players.forEach(player -> players.forEach(player::showPlayer));
                    }
                }
                sgseconds--;
                players.forEach(player -> ms.sendScoreboard(player, sb));
            }
        }.runTaskTimer(ms, 20L, 20L);
    }

    private boolean refill = false;

    private void refilltask(final org.bukkit.Server s) {
        refilltask = new BukkitRunnable() {
            byte count = 2;

            @Override
            public void run() {
                players.forEach(player -> ms.sendScoreboard(player, sb));
                if (count == 0) {
                    for (final Location l : chests.keySet()) {
                        final Inventory i = s.createInventory(null, 27, "Supply Kiste");
                        for (byte n = 0; n < 2; n++) {
                            final ItemStack is = new ItemStack(gooditems.get(r.nextInt(goodsize)));
                            if (is.getType().isEdible()) {
                                is.setAmount(2);
                            }
                            i.setItem(r.nextInt(27), is);
                        }
                        for (byte n = 0; n < 2; n++) {
                            final short id = normalitems.get(r.nextInt(normalsize));
                            final ItemStack is = new ItemStack(id);
                            if (id == 280) {
                                is.setAmount(2);
                            } else if (id == 262 || id == 296) {
                                is.setAmount(4);
                            } else if (is.getType().isEdible()) {
                                is.setAmount(3);
                            }
                            i.setItem(r.nextInt(27), is);
                        }
                        chests.put(l, i);
                        l.getWorld().playEffect(l, org.bukkit.Effect.MOBSPAWNER_FLAMES, 1);
                    }
                    players.forEach(player -> {
                        ms.sendTitle(player, "§d§lMuxSG", 0, 90, 20);
                        ms.sendSubTitle(player, "§aAlle Supply Kisten wurden nachgefüllt.", 0, 90, 20);
                        player.playSound(player.getLocation(), Sound.HORSE_SADDLE, 1F, 1F);
                    });
                    sendEventMessage("§d§lMuxSG>§7 Alle Supply Kisten wurden §anachgefüllt§7.");
                    refill = true;
                    cancel();
                } else {
                    for (final Player pl : players) {
                        pl.playSound(pl.getLocation(), Sound.ORB_PICKUP, 1.0F, 1.0F);
                    }
                }
                count--;
            }
        }.runTaskTimer(ms, 0L, 20L);
    }

    private byte dmcount = 60;

    private void dmcdtask(final Server s) {
        sendEventBar("§fDeathmatch beginnt in §e60 Sekunden§f.", Sound.CLICK);
        dmcount = 60;
        dmcdtask = new BukkitRunnable() {
            final List<Location> dmpositions = new ArrayList<>();
            final List<Double> xlist = new ArrayList<>(Arrays.asList(0.5, 6.5, 13.5, 23.5, 25.5, 22.5, 14.5, 0.5, -7.5, -16.5, -20.5, -21.5, -19.5, -8.5));
            final List<Double> zlist = new ArrayList<>(Arrays.asList(17.5, 16.5, 13.5, 7.5, 0.5, -7.5, -14.5, -16.5, -15.5, -11.5, -6.5, 0.5, 10.5, 16.5));
            final List<Float> yawlist = new ArrayList<>(Arrays.asList(180.0F, 153.0F, 126.0F, 107.0F, 90.0F, 70.0F, 47.0F, 0.0F, -30.0F, -60.0F, -73.0F, -90.0F, -113.0F, -146.0F));

            @Override
            public void run() {
                if (dmcount == 10) {
                    for (final Player player : players) {
                        ms.sendTitle(player, "§d§lMuxSG", 0, 90, 20);
                        ms.sendSubTitle(player, "§eDu wirst gleich zum Deathmatch teleportiert.", 0, 90, 20);
                        player.playSound(player.getLocation(), Sound.WITHER_IDLE, 1, 1);
                    }
                }
                if (dmcount % 10 == 0 || dmcount < 10) {
                    sendEventBar("§fDeathmatch beginnt in §e" + dmcount + " Sekunde" + (dmcount > 1 ? "n" : "") + "§f.", Sound.CLICK);
                    if (dmcount == 0) {
                        sendEventBar("§fDeathmatch hat §ejetzt begonnen§f!", Sound.ENDERDRAGON_GROWL);
                        final World w = s.createWorld(new WorldCreator("MuxSurvivalGamesDM").type(org.bukkit.WorldType.FLAT));
                        w.setGameRuleValue("doFireTick", "false");
                        w.setGameRuleValue("doMobSpawning", "false");
                        w.setGameRuleValue("doMobLoot", "false");
                        w.setGameRuleValue("doTileDrops", "false");
                        w.setGameRuleValue("mobGriefing", "false");
                        w.setAutoSave(false);
                        w.setTime(0L);
                        dmworld = w;
                        int i = 0;
                        for (byte count = 0; count < players.size(); count++) {
                            if (i == 14) {
                                i = 0;
                            }
                            dmpositions.add(new Location(w, xlist.get(i), 85.1, zlist.get(i), yawlist.get(i), 0F));
                            i++;
                        }
                        sdm = new Location(w, 0.5, 93.1, 4.5, 0F, 0F);
                        for (final Player pl : s.getOnlinePlayers()) {
                            if (spectators.containsKey(pl.getUniqueId())) {
                                spectators.put(pl.getUniqueId(), sdm);
                                pl.setMetadata("muxspectatetp", new FixedMetadataValue(ms, false));
                                pl.teleport(sdm, PlayerTeleportEvent.TeleportCause.END_PORTAL);
                                pl.removeMetadata("muxspectatetp", ms);
                            } else {
                                if (players.contains(pl) == false) continue;
                                final Location l = dmpositions.get(r.nextInt(dmpositions.size()));
                                dmpositions.remove(l);
                                ms.forcePlayer(pl, l);
                                pl.playSound(pl.getLocation(), Sound.ENDERDRAGON_DEATH, 1F, 0.6F);
                            }
                        }
                        s.unloadWorld(loc.getWorld(), false);
                        deathmatch();
                        cancel();
                    }
                }
                dmcount--;
                players.forEach(player -> ms.sendScoreboard(player, sb));
            }
        }.runTaskTimer(ms, 0, 20L);
    }

    private int deathmatchseconds = 300;
    private boolean firstiteration = true;

    private void deathmatch() {
        deathmatchseconds = 300;
        deathmatch = true;
        firstiteration = true;
        dmtask = new BukkitRunnable() {

            @Override
            public void run() {
                if (deathmatchseconds == 0) {
                    sendEventMessage("§d§lMuxSurvivalGames>§7 Die Zeit ist abgelaufen.");
                    sendEventMessage("§d§lMuxSurvivalGames>§7 Ein zufälliger Spieler gewinnt.");
                    cancel();
                    stopEvent(true);
                } else {
                    if (deathmatchseconds % 60 == 0) {
                        final int minutes = (deathmatchseconds / 60);
                        sendEventBar("§fNoch §d " + minutes + " Minute" + (minutes > 1 ? "n" : "") + " §fbis zum Ende.", Sound.NOTE_STICKS);
                    }
                    firstiteration = false;
                    players.forEach(player -> ms.sendScoreboard(player, sb));
                    deathmatchseconds--;
                }
            }
        }.runTaskTimer(ms, 20L, 20L);
    }

    @EventHandler
    public void onIgnite(final BlockIgniteEvent e) {
        final Player p = e.getPlayer();
        if (p != null && players.contains(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFood(final FoodLevelChangeEvent e) {
        if (players.contains((Player) e.getEntity()) && move == false) {
            e.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        final Player p = e.getPlayer();
        if (players.contains(p) == false || e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        final Block b = e.getClickedBlock();
        if (b.getType() == Material.PRISMARINE && b.getData() == 2) {
            if (canjoin) return;
            final Location l = b.getLocation();
            Inventory i = chests.get(l);
            if (i != null) {
                p.openInventory(i);
            } else {
                i = ms.getServer().createInventory(null, 27, "Supply Kiste");
                if (r.nextInt(101) <= 15) {
                    for (byte n = 0; n < 2; n++) {
                        final ItemStack is = new ItemStack(gooditems.get(r.nextInt(goodsize)));
                        if (is.getType().isEdible()) {
                            is.setAmount(2);
                        }
                        i.setItem(r.nextInt(27), is);
                    }
                }
                for (byte n = 0; n < 4; n++) {
                    final short id = normalitems.get(r.nextInt(normalsize));
                    final ItemStack is = new ItemStack(id);
                    if (id == 280) {
                        is.setAmount(2);
                    } else if (id == 262 || id == 296) {
                        is.setAmount(4);
                    } else if (is.getType().isEdible()) {
                        is.setAmount(3);
                    }
                    i.setItem(r.nextInt(27), is);
                }
                chests.put(l, i);
                p.openInventory(i);
            }
        }
    }

    @EventHandler
    public void onRedstone(final BlockRedstoneEvent e) {
        if (e.getBlock().getWorld().getName().equals(loc.getWorld().getName()) && e.getBlock().getType() == Material.REDSTONE_LAMP_ON) {
            e.setNewCurrent(5);
        }
    }

    @EventHandler
    public void onExp(final ExpBottleEvent e) {
        if (e.getEntity().getWorld().equals(loc.getWorld()) == false || e.getEntity().getShooter() instanceof Player == false)
            return;
        ((Player) e.getEntity().getShooter()).giveExp(e.getExperience());
        e.setExperience(0);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(final EntityDamageEvent e) {
        final org.bukkit.entity.Entity en = e.getEntity();
        if (en instanceof Player && players.contains(en)) {
            if (canjoin || sgseconds >= 1770) {
                e.setCancelled(true);
            } else if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION || e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                e.setCancelled(false);
            }
        }
    }

    @EventHandler
    public void onDamage(final EntityDamageByEntityEvent e) {
        final Entity dmg = e.getDamager();
        if (dmg instanceof Player && players.contains((Player) dmg) && canjoin) {
            e.setCancelled(true);
            ms.showItemBar((Player) dmg, "§cWarte bis das Event startet.");
        }
    }

    @EventHandler
    public void onCombust(final EntityCombustEvent e) {
        final org.bukkit.entity.Entity en = e.getEntity();
        if (en instanceof Player && players.contains((Player) en) && canjoin) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(final BlockBreakEvent e) {
        final Player p = e.getPlayer();
        if (players.contains(p) == false) return;
        final short id = (short) e.getBlock().getTypeId();
        if (breakableblocks.contains(id) == false || canjoin) {
            e.setCancelled(true);
        } else if (id == 18) {
            e.setCancelled(true);
            if (deathmatch == false) {
                e.getBlock().setType(Material.AIR);
            }
        }
    }

    @EventHandler
    public void onPlace(final BlockPlaceEvent e) {
        final Player p = e.getPlayer();
        if (players.contains(p) == false) return;
        final short id = (short) e.getBlockPlaced().getTypeId();
        if (placeableblocks.contains(id) == false || canjoin) {
            e.setCancelled(true);
        } else if (id == 46) {
            p.getInventory().removeItem(new ItemStack(Material.TNT));
            e.setBuild(false);
            p.getWorld().spawn(e.getBlockPlaced().getLocation().add(0, 0.2, 0), org.bukkit.entity.TNTPrimed.class).setFuseTicks(40);
        }
    }

    @EventHandler
    public void onDeath(final PlayerDeathEvent e) {
        if (players.contains(e.getEntity()) == false) {
            return;
        }
        e.getDrops().removeIf(itemStack -> itemStack.getType() == Material.NETHER_STAR);
        death(e.getEntity(), false);
    }

    @EventHandler
    public void onMove(final PlayerMoveEvent e) {
        if (players.contains(e.getPlayer()) && move == false) {
            final Location from = e.getFrom(), to = e.getTo();
            if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
                e.setTo(from);
            }
        }
    }

    @EventHandler
    public void onPickup(final PlayerPickupItemEvent e) {
        if (players.contains(e.getPlayer()) && canjoin) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBed(final PlayerBedEnterEvent e) {
        e.setCancelled(true);
    }

    @EventHandler
    public void onFish(final PlayerFishEvent e) {
        final org.bukkit.entity.Entity en = e.getCaught();
        if (en instanceof Player && players.contains(en)) {
            en.setVelocity(e.getPlayer().getLocation().getDirection().multiply(-0.5D));
        }
    }
}