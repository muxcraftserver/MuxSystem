package me.muxteam.events;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class MuxGetDown extends Event implements Listener {
    private Location genloc, loc;
    private final LinkedHashMap<UUID, Long> finished = new LinkedHashMap<>();
    private final MuxScoreboard.ScoreboardType sb;
    private final Map<Player, Integer> lastY = new HashMap<>(), distance = new HashMap<>();
    private final DecimalFormat decimalFormat = new DecimalFormat("#0.000");
    private long start;

    public MuxGetDown(final MuxEvents e) {
        super(e);
        name = "MuxGetDown";
        item = new ItemStack(Material.GOLD_BOOTS);
        canSpectate = false;
        maxPlayers = 300;
        playerHider = 1;
        maxSpectatorDistance = 150D;
        sb = ms.getScoreboard().createScoreboard("§7§lMux§d§lGetDown");
        sb.setLook((p, board) -> {
            final List<Map.Entry<Player, Integer>> sorted = distance.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList());
            sb.setLine(board, " ");
            boolean self = false;
            final boolean notSpectating = spectators.containsKey(p.getUniqueId()) == false;
            int i = 0;
            for (final Map.Entry<Player, Integer> top : sorted) {
                if (i == 5) break;
                i++;
                String pname = top.getKey().getName();
                if (pname.length() >= 14) {
                    pname = pname.substring(0, 11) + "..";
                }
                if (top.getKey().getName().equals(p.getName())) {
                    self = true;
                    sb.setSection(board, null, "§d#" + (i) + " §6" + pname + " §d» §a" + (top.getValue() <= 0 ? "✔" : top.getValue()), false);
                    continue;
                } else if (self == false && i == 5 && notSpectating == false) break;
                sb.setSection(board, null, "§d#" + (i) + " §f" + pname + " §d» §a" + (top.getValue() <= 0 ? "✔" : top.getValue()), false);
            }
            if (self == false && notSpectating == false) {
                sorted.stream().filter(entry -> entry.getKey().getName().equals(p.getName())).findFirst().ifPresent(entry -> {
                    final int pos = sorted.indexOf(entry) + 1;
                    String pname = entry.getKey().getName();
                    if (pname.length() >= 14) {
                        pname = pname.substring(0, 11) + "..";
                    }
                    sb.setSection(board, null, "§d#" + pos + " §6" + pname + " §d» §a" + (entry.getValue() <= 0 ? "✔" : entry.getValue()), false);
                });
            }
        });
        maxDuration = 900000;
    }

    private void death(final Player p) {
        p.setFallDistance(0F);
        p.setHealth(20D);
        ms.forcePlayer(p, loc);
    }

    @Override
    public void stop() {
        lastY.clear();
        distance.clear();
        ms.getServer().unloadWorld(loc.getWorld(), true);
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Erreiche den Boden, ohne zu sterben.",
                "§7Goldblöcke geben dir Goldäpfel, durch",
                "§7leuchtende Blöcke kriegst du Effekte.",
                "",
                "§7Teilnehmer: §d" + players.size()
        };
    }

    @Override
    public Location getSpectateLocation() {
        return loc;
    }

    @Override
    public long getUpdateTime() {
        return 5L;
    }

    @Override
    public void update() {
        for (final Player p : players) {
            if (players.contains(p) == false) continue;
            final Block b = p.getLocation().add(0, -1, 0).getBlock();
            final Material m = b.getType();
            if (m == Material.GOLD_BLOCK) {
                b.setType(Material.COAL_BLOCK);
                ms.sendNormalTitle(p, "§e§lGoldener Apfel", 10, 40, 10);
                p.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
            } else if (m == Material.SEA_LANTERN) {
                b.setType(Material.COAL_BLOCK);
                final int surprise = r.nextInt(11);
                String show;
                if (surprise == 1) {
                    show = "§d§lRegeneration";
                    p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 3, 3));
                } else if (surprise == 2) {
                    show = "§c§lHeilung";
                    p.setHealth(20D);
                } else if (surprise == 3) {
                    show = "§6§lAbsorption";
                    p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 10, 3));
                } else if (surprise == 4) {
                    show = "§8§lBlindheit";
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 5, 3));
                } else if (surprise == 5) {
                    show = "§5§lÜbelkeit";
                    p.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 15, 3));
                } else if (surprise == 6) {
                    show = "§0§lPech";
                    b.setType(Material.AIR);
                } else if (surprise == 7) {
                    show = "§c§lMega Sprung";
                    p.setVelocity(new Vector(0, 5, 0));
                } else if (surprise == 8) {
                    show = "§7§lLangsamkeit";
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 5, 3));
                } else if (surprise == 9) {
                    show = "§f§l5 Schneebälle";
                    p.getInventory().addItem(new ItemStack(Material.SNOW_BALL, 5));
                } else if (surprise == 10) {
                    show = "§6§lAngel";
                    p.getInventory().addItem(new ItemStack(Material.FISHING_ROD));
                } else {
                    show = "§7§lNichts";
                }
                ms.sendNormalTitle(p, show, 10, 40, 10);
            }
            final int lastYpoint = this.lastY.getOrDefault(p, 0);
            this.lastY.put(p, p.getLocation().getBlockY());
            if (p.getLocation().add(0, -1, 0).getBlock().getType() != Material.AIR) {
                final int oldDistance = distance.getOrDefault(p, -2), dis = p.getLocation().getBlockY() - 41;
                if (oldDistance != dis) distance.put(p, dis);
            }
            if (p.getLocation().getBlockY() < 42 && lastYpoint == p.getLocation().getBlockY()) {
                if (finished.containsKey(p.getUniqueId())) return;
                canjoin = false;
                finished.put(p.getUniqueId(), System.currentTimeMillis());
                final int rank = finished.size();
                if (rank == 1) {
                    ms.sendTitle(p, "§d§lMuxGetDown", 10, 80, 10);
                    ms.sendSubTitle(p, "§fDu hast das Event §agewonnen§f!", 10, 80, 10);
                } else {
                    ms.sendTitle(p, "§d§lMuxGetDown", 15, 80, 10);
                    ms.sendSubTitle(p, "§fDein Platz: §d#" + rank, 15, 80, 10);
                }
                sendEventMessage("§7Der Spieler §d" + p.getName() + " §7ist unten §aangekommen§7! §7(#" + rank + ")");
                p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 1F, 1F);
                if (startedByPlayer == false) {
                    ms.runLater(new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (p.isOnline() == false) return;
                            final int reward = rank == 1 ? 150 : rank == 2 ? 100 : rank == 3 ? 50 : rank == 4 ? 20 : 10;
                            ms.sendSubTitle(p, "§fDeine Belohnung: §e§l" + ms.getNumberFormat(giveReward(p, reward)) + " MuxCoins", 0, 60, 10);
                        }
                    }, 60L);
                }
                if (rank == 5 || players.size() <= rank) {
                    ms.broadcastMessage("§d§lMuxEvent>§7 Das MuxGetDown Event ist nun zuende.", null, MuxSystem.Priority.LOW);
                    int pos = 1;
                    for (final Map.Entry<UUID, Long> en : finished.entrySet()) {
                        ms.broadcastMessage("§d§lMuxEvent>§7 Spieler §a#" + pos + "§7: §d" + ms.getServer().getOfflinePlayer(en.getKey()).getName() + "§7 (" + decimalFormat.format((en.getValue() - start) / 1000D).replace(".", ",") + " Sekunden)", null, MuxSystem.Priority.LOW);
                        pos++;
                    }
                    stopEvent(false);
                } else {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            quit(p);
                        }
                    }.runTaskLater(ms, 60L);
                }
            }
        }
    }

    @Override
    public void generate() {
        final Location corner = genloc.clone().add(0, -150, 0).clone();
        final Block firstb = genloc.clone().add(-1, 0, 0).getBlock();
        final boolean first = firstb == null || firstb.getType() == Material.AIR;
        final EditSession session = ms.getFastBuilder(corner.getWorld().getName());
        for (int x = -40; x <= 39; x++) {
            for (int y = 0; y < 160; y++) {
                for (int z = -40; z <= 39; z++) {
                    if (z == -40 || z == 39 || x == -40 || x == 39) {
                        if (first) {
                            final Location location = corner.clone().add(x, y, z);
                            session.setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), y >= 150 ? new BaseBlock(BlockID.BARRIER) : new BaseBlock(BlockID.STAINED_CLAY, 15));
                        }
                    } else if (y < 150) {
                        Location l = corner.clone().add(x, y, z);
                        session.setBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ(), new BaseBlock(BlockID.AIR));
                    }
                }
            }
        }
        final int theme = r.nextInt(4);
        for (int x = -39; x < 39; x++) {
            for (int y = 0; y < 150; y++) {
                for (int z = -39; z < 39; z++) {
                    final Location l = corner.clone().add(x, y, z);
                    final int empty = r.nextInt(100);
                    if (empty == 0) {
                        final int special = r.nextInt(100);
                        if (special == 0) {
                            session.setBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ(), new BaseBlock(BlockID.GOLD_BLOCK));
                        } else if (special == 1 || special == 2 || special == 3) {
                            session.setBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ(), new BaseBlock(BlockID.SEA_LANTERN));
                        } else {
                            final int block = r.nextInt(4);
                            if (theme == 0) {
                                if (block == 0 || block == 1) {
                                    session.setBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ(), new BaseBlock(BlockID.LEAVES, 4));
                                } else if (block == 2) {
                                    session.setBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ(), new BaseBlock(BlockID.LOG));
                                } else {
                                    session.setBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ(), new BaseBlock(BlockID.WOOD));
                                }
                            } else if (theme == 1) {
                                if (block == 0 || block == 1) {
                                    session.setBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ(), new BaseBlock(BlockID.SANDSTONE));
                                } else if (block == 2) {
                                    session.setBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ(), new BaseBlock(BlockID.SPONGE));
                                } else {
                                    session.setBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ(), new BaseBlock(BlockID.END_STONE));
                                }
                            } else if (theme == 2) {
                                if (block == 0 || block == 1) {
                                    session.setBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ(), new BaseBlock(BlockID.STAINED_GLASS, 2));
                                } else if (block == 2) {
                                    session.setBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ(), new BaseBlock(BlockID.STAINED_GLASS, 11));
                                } else {
                                    session.setBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ(), new BaseBlock(BlockID.STAINED_GLASS, 3));
                                }
                            } else {
                                if (block == 0 || block == 1) {
                                    session.setBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ(), new BaseBlock(BlockID.DIAMOND_ORE));
                                } else if (block == 2) {
                                    session.setBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ(), new BaseBlock(BlockID.IRON_ORE));
                                } else {
                                    session.setBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ(), new BaseBlock(BlockID.EMERALD_ORE));
                                }
                            }
                        }
                    }
                    if (y == 0) {
                        session.setBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ(), new BaseBlock(BlockID.HARDENED_CLAY));
                    }
                }
            }
        }
        for (int i = -1; i < 2; i++) {
            for (int n = -1; n < 2; n++) {
                final Location l = genloc.clone().add(i, 0, n);
                session.setBlock(l.getBlockX(), l.getBlockY(), l.getBlockZ(), new BaseBlock(BlockID.BEDROCK));
            }
        }
        session.flushQueue();
        finished.clear();
    }

    @Override
    public void start() {
        ms.getServer().createWorld(new WorldCreator("MuxGetDown").generateStructures(false).type(org.bukkit.WorldType.FLAT));
        genloc = new Location(ms.getServer().getWorld("MuxGetDown"), 0D, 190D, 0D, 0F, 0F);
        loc = genloc.clone().add(0, 2, 0);
        final WorldBorder wb = loc.getWorld().getWorldBorder();
        wb.setSize(475D);
        wb.setCenter(0D, 0D);
        generate();
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das MuxGetDown Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        spawnEventNPC("§d§lEvent beitreten");
        start = System.currentTimeMillis();
    }

    @Override
    public boolean join(final Player p) {
        distance.put(p, 150);
        equipPlayer(p);
        ms.forcePlayer(p, loc);
        final ItemStack tp = ms.renameItemStack(new ItemStack(Material.SLIME_BALL), "§f§lMux§a§lTeleport", "§7Teleportiert dich zurück", "§7zum Spawnpunkt.");
        p.getInventory().setItem(0, tp);
        p.getInventory().setHeldItemSlot(0);
        ms.sendScoreboard(p, sb);
        return true;
    }

    @Override
    public void quit(final Player p) {
        resetPlayer(p);
        lastY.remove(p);
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        final Player p = e.getPlayer();
        if (players.contains(p) == false) {
            return;
        }
        if (e.getAction() != Action.PHYSICAL && e.getMaterial() != null && e.getMaterial() == Material.SLIME_BALL) {
            death(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(final EntityDamageEvent e) {
        if (e.getEntityType() == EntityType.PLAYER) {
            final Player p = (Player) e.getEntity();
            if (players.contains(p) == false) {
                return;
            }
            if (e.getCause() == EntityDamageEvent.DamageCause.FALL || e.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                e.setCancelled(false);
                if (e.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
                    e.setDamage(0D);
                    return;
                }
                if (p.getHealth() - e.getFinalDamage() <= 0D) {
                    e.setCancelled(true);
                    death(p);
                }
            } else {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBreak(final BlockBreakEvent e) {
        if (players.contains(e.getPlayer()) == false) {
            return;
        }
        e.setCancelled(true);
    }
}