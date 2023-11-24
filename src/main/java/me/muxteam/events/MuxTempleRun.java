package me.muxteam.events;

import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class MuxTempleRun extends Event implements Listener {
    private final Location loc = new Location(ms.getWarpsWorld(), -410.5, 10, -86.5, 180, 0);
    private final Map<UUID, Long> startTimes = new HashMap<>(), playerTimes = new HashMap<>();
    private final DecimalFormat decimalFormat = new DecimalFormat("#0.000");
    private final MuxScoreboard.ScoreboardType sb;

    private int secs = 900;

    public MuxTempleRun(final MuxEvents e) {
        super(e);
        name = "MuxTempleRun";
        maxPlayers = 150;
        playerHider = 0;

        item = new ItemStack(Material.CHAINMAIL_BOOTS);
        sb = ms.getScoreboard().createScoreboard("§7§lMux§d§lTempleRun");
        sb.setLook((p, board) -> {
            sb.setLine(board, "§c");
            long time = (System.currentTimeMillis() - startTimes.getOrDefault(p.getUniqueId(), System.currentTimeMillis()));
            sb.setSection(board, "§a§lAktuelle Zeit", ((int) (time / 1000D)) + " Sekunden", false);
            sb.setLine(board, "§a");
            if (playerTimes.containsKey(p.getUniqueId())) {
                sb.setSection(board, "§2§lDeine Bestzeit", "§f" + decimalFormat.format((playerTimes.get(p.getUniqueId()) / 1000D)).replace(".", ",") + " Sekunden", false);
                sb.setLine(board, "§e");
            }
            if (playerTimes.isEmpty() == false) {
                sb.setSection(board, "§e§lBestzeit", decimalFormat.format(getBestTime().getValue() / 1000D).replace(".", ",") + " Sekunden", false);
                sb.setLine(board, "§b");
            }
            sb.setSection(board, "§6§lEnde", "§fin " + ms.getTime(secs), false);
        });
        maxDuration = 15000000;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Laufe so schnell es geht zum Tempel!",
                "§7Finde dabei den schnellsten Weg, um",
                "§7deine Zeit zu minimieren.",
                "",
                "§7Teilnehmer: §d" + players.size()
        };
    }

    @Override
    public long getUpdateTime() {
        return 20L;
    }

    @Override
    public void update() {
        secs--;
        if (secs % 60 == 0 && secs > 0) {
            sendEventBar("§fDas Event geht noch §d" + (secs / 60) + " Minuten§f.", Sound.NOTE_STICKS);
        }
        players.forEach(p -> ms.sendScoreboard(p, sb));
        if (secs == -1) {
            stopEvent(false);
        } else if (secs <= 60 && secs % 10 == 0 && secs > 0) {
            sendEventBar("§fDas Event geht noch §d" + secs + " Sekunden§f.", Sound.NOTE_STICKS);
        } else if (secs < 10 && secs > 0) {
            sendEventBar("§fDas Event geht noch §d" + secs + " Sekunden§f.", Sound.NOTE_STICKS);
        }
    }

    @Override
    public void start() {
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das MuxTempleRun §7Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        spawnEventNPC("§d§lEvent beitreten");
        secs = 900;
    }

    @Override
    public boolean join(final Player p) {
        equipPlayer(p);
        ms.forcePlayer(p, loc);
        startTimes.put(p.getUniqueId(), System.currentTimeMillis());
        return true;
    }

    @Override
    public void quit(final Player p) {
        resetPlayer(p);
    }

    @Override
    public void stop() {
        ms.broadcastMessage("§d§lMuxEvent>§7 Das MuxTempleRun Event ist nun zuende.", null, MuxSystem.Priority.LOW);
        int pos = 0;
        final Server sr = ms.getServer();
        for (final Map.Entry<UUID, Long> entry : playerTimes.entrySet().stream().sorted(Comparator.comparingLong(Map.Entry::getValue)).collect(Collectors.toList())) {
            final Player player = sr.getPlayer(entry.getKey());
            pos++;
            if (pos == 4) break;
            int baseAmount = pos == 1 ? 150 : pos == 2 ? 50 : 20;
            if (player != null) {
                ms.sendTitle(player, "§d§lMuxTempleRun", 10, 80, 10);
                if (pos == 1) {
                    ms.sendSubTitle(player, "§fDu hast das Event §agewonnen§f!", 10, 80, 10);
                } else {
                    ms.sendSubTitle(player, "§fDein Platz: §d#" + pos, 10, 80, 10);
                }
                if (startedByPlayer == false) {
                    ms.runLater(new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline() == false) return;
                            ms.sendSubTitle(player, "§fDeine Belohnung: §e§l" + ms.getNumberFormat(giveReward(player, baseAmount)) + " MuxCoins", 0, 60, 10);
                        }
                    }, 60L);
                }
            }
            ms.broadcastMessage("§d§lMuxEvent>§7 Spieler §a#" + pos + "§7: §d" + sr.getOfflinePlayer(entry.getKey()).getName() + "§7 (" + decimalFormat.format(playerTimes.get(entry.getKey()) / 1000D).replace(".", ",") + " Sekunden)", null, MuxSystem.Priority.LOW);
        }
        startTimes.clear();
        playerTimes.clear();
    }

    private Map.Entry<UUID, Long> getBestTime() {
        return playerTimes.entrySet().stream().min(Comparator.comparingLong(Map.Entry::getValue)).orElse(new Map.Entry<UUID, Long>() {
            @Override
            public UUID getKey() {
                return null;
            }

            @Override
            public Long getValue() {
                return 0L;
            }

            @Override
            public Long setValue(Long value) {
                return null;
            }
        });
    }

    @EventHandler
    public void onDamage(final EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && players.contains((Player) e.getEntity())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(final PlayerMoveEvent e) {
        final Player p = e.getPlayer();
        final Location from = e.getFrom(), to = e.getTo();
        if (to.getBlock().isLiquid() && players.contains(p)) {
            if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
                final Material m = e.getTo().getBlock().getType();
                if (m == Material.STATIONARY_WATER) {
                    ms.forcePlayer(p, loc);
                    startTimes.put(p.getUniqueId(), System.currentTimeMillis());
                    ms.sendScoreboard(p, sb);
                    return;
                }
                if (m == Material.STATIONARY_LAVA || m == Material.LAVA) {
                    final long startTime = startTimes.get(p.getUniqueId());
                    final UUID uuid = p.getUniqueId();
                    final long highscore = getBestTime().getValue(), scorenew = System.currentTimeMillis() - startTime;
                    final String name = p.getName();
                    final String trseconds = decimalFormat.format(scorenew / 1000D).replace(".", ",");
                    if (scorenew < 30000) {
                        for (final Player pl : ms.getServer().getOnlinePlayers()) {
                            if (pl.isOp())
                                pl.sendMessage("§c§lAntiCheat>§d " + name + " §chat versucht beim TempleRun zu cheaten! (" + trseconds + "s)");
                        }
                    } else if (highscore == 0 || highscore > scorenew) {
                        ms.sendTitle(p, "§e§lBestzeit", 10, 120, 10);
                        ms.sendSubTitle(p, "§a" + trseconds + " Sekunden", 10, 60, 10);
                        p.playSound(loc, Sound.FIREWORK_LARGE_BLAST2, 1F, 1F);
                        p.playSound(loc, Sound.FIREWORK_TWINKLE, 1F, 1F);
                        if (startedByPlayer == false) {
                            ms.runLater(new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (p.isOnline() == false) return;
                                    ms.sendSubTitle(p, "§fDeine Belohnung: §e§l" + giveReward(p, 2) + " MuxCoins", 0, 60, 10);
                                }
                            }, 60L);
                        }
                        sendEventMessage("§7Die §eneue Bestzeit §7hat " + name + " mit §a" + trseconds + " Sekunden§7.");
                    } else {
                        long oldTime = playerTimes.getOrDefault(p.getUniqueId(), -1L);
                        if (oldTime == -1 || oldTime > scorenew) {
                            ms.sendTitle(p, "§2§lDeine Bestzeit", 10, 120, 10);
                            ms.sendSubTitle(p, "§a" + trseconds + " Sekunden", 10, 60, 10);
                            p.playSound(loc, Sound.LEVEL_UP, 1F, 0.1F);
                        } else {
                            ms.sendNormalTitle(p, "§a" + trseconds + " Sekunden", 10, 60, 10);
                            p.playSound(loc, Sound.ORB_PICKUP, 1F, 1F);
                        }
                        if (startedByPlayer == false) {
                            p.playSound(p.getLocation(), Sound.FIREWORK_BLAST, 1F, 1F);
                            ms.runLater(new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (p.isOnline() == false) return;
                                    ms.sendSubTitle(p, "§fDeine Belohnung: §e§l" +  giveReward(p, 1) + " MuxCoins", 0, 60, 10);
                                }
                            }, 60L);
                        }
                    }
                    if (scorenew < playerTimes.getOrDefault(uuid, Long.MAX_VALUE))
                        playerTimes.put(uuid, scorenew);
                    ms.forcePlayer(p, loc);
                    startTimes.put(uuid, System.currentTimeMillis());
                    ms.sendScoreboard(p, sb);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            p.setFireTicks(0);
                        }
                    }.runTaskLater(ms, 1L);
                }
            }
        }
    }
}