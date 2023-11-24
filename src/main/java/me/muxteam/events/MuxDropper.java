package me.muxteam.events;

import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MuxDropper extends Event implements Listener {
    private final List<Arena> arenas = new ArrayList<>();
    private final Map<Player, Arena> playerarena = new HashMap<>();
    private final Map<Integer, Player> positions = new HashMap<>();
    private final Set<String> playersFinished = new HashSet<>();
    private World world;
    private boolean finished = false;
    private final MuxScoreboard.ScoreboardType sb;

    public MuxDropper(final MuxEvents e) {
        super(e);
        name = "MuxDropper";
        item = new ItemStack(Material.DIAMOND_BOOTS);
        maxPlayers = 300;
        playerHider = 0;
        sb = ms.getScoreboard().createScoreboard("§7§lMux§d§lDropper");
        sb.setLook((p, board) -> {
            sb.setLine(board, "§c");
            sb.setSection(board, "§c§lLevel", "§f" + (arenas.indexOf(playerarena.get(p)) + 1) + " von " + arenas.size(), false);
            sb.setLine(board, "§b");
            sb.setSection(board, "§6§lZeit", (cancelTime < System.currentTimeMillis() + 500L) ? "§fnoch 0 Sekunden" : "§fnoch " + ms.getTime((int) ((cancelTime - System.currentTimeMillis()) / 1000)), false);
        });
        maxDuration = 900000;
    }

    private void finish(final Player p) {
        if (playersFinished.add(p.getName()) == false) {
            sendEventMessage("§7Der Spieler §d" + p.getName() + " §7hat es erneut zum Ziel §ageschafft§7.");
            ms.sendTitle(p, "§d§lMuxDropper", 10, 60, 10);
            ms.sendSubTitle(p, "§fDu hast das Ziel §aerreicht§f!", 10, 60, 10);
            p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 1F, 1F);
            resetPlayer(p);
            return;
        }
        int pos;
        positions.put(pos = (positions.size() + 1), p);
        sendEventMessage("§7Der Spieler §d" + p.getName() + " §7hat es zuende §ageschafft§7!" + " (#" + pos + ")");
        resetPlayer(p);
        ms.sendTitle(p, "§d§lMuxDropper", 10, 60, 10);
        ms.sendSubTitle(p, "§fDu hast das Ziel §aerreicht§f!", 10, 60, 10);
        p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 1F, 1F);
        if (startedByPlayer == false) {
            ms.runLater(new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline() == false) return;
                    final int reward = pos == 1 ? 150 : (pos == 2 ? 50 : (pos == 3 ? 20 : 5));
                    ms.sendSubTitle(p, "§fDeine Belohnung: §e§l" + ms.getNumberFormat(giveReward(p, reward)) + " MuxCoins", 0, 60, 10);
                }
            }, 60L);
        }
        if (players.isEmpty()) {
            stopEvent(true);
        }
    }

    @Override
    public void start() {
        world = ms.getServer().createWorld(new WorldCreator("MuxDropper").generateStructures(false).type(org.bukkit.WorldType.FLAT));
        final int num = r.nextInt(3);
        if (num == 2) {
            arenas.add(new Arena(new Location(world, -156.5, 232, 949.5, -90F, 0F), new Location(world, -154, 232, 949), Material.BIRCH_FENCE));
            arenas.add(new Arena(new Location(world, -93.5, 231, 948.5, -90F, 0F)));
            arenas.add(new Arena(new Location(world, -31.5, 231, 948.5, -90F, 0F)));
        } else if (num == 1) {
            arenas.add(new Arena(new Location(world, -346.5, 234, 884.5, -90F, 0F), new Location(world, -344, 233, 884), Material.DARK_OAK_FENCE));
            arenas.add(new Arena(new Location(world, -282.5, 233, 884.5, -90F, 0F)));
            arenas.add(new Arena(new Location(world, -219.5, 233, 884.5, -90F, 0F)));
        } else {
            arenas.add(new Arena(new Location(world, -154.5, 234, 884.5, -90F, 0F), new Location(world, -152, 233, 884), Material.NETHER_FENCE));
            arenas.add(new Arena(new Location(world, -91.5, 233, 884.5, -90F, 0F)));
            arenas.add(new Arena(new Location(world, -26.5, 233, 884.5, -90F, 0F)));
        }
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das MuxDropper §7Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        finished = false;
        startCountdown(60, true, false);
        spawnEventNPC("§d§lEvent beitreten");
        generate();
    }

    @Override
    public void stop() {
        if (positions.isEmpty() == false) {
            ms.broadcastMessage("§d§lMuxEvent>§7 Das MuxDropper Event ist nun zuende.", null, MuxSystem.Priority.LOW);
        }
        finished = true;
        arenas.clear();
        positions.clear();
        playerarena.clear();
        playersFinished.clear();
        ms.getServer().unloadWorld(world, true);
    }

    @Override
    public boolean join(final Player p) {
        if (positions.containsValue(p)) {
            ms.showItemBar(p, "§cDu hast bereits alle Level geschafft.");
            return false;
        }
        equipPlayer(p);
        final Arena arena = arenas.get(0);
        ms.forcePlayer(p, arena.getLocation());
        playerarena.put(p, arena);
        if (wait == -1) ms.sendScoreboard(p, sb);
        return true;
    }

    @Override
    public void quit(final Player p) {
        resetPlayer(p);
        if (players.isEmpty()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (finished) return;
                    stopEvent(true);
                }
            }.runTaskLater(ms, 40L);
        }
    }

    @Override
    public void generate() {
        arenas.forEach(arena -> {
            if (arena.fenceloc == null) return;
            arena.fenceloc.getBlock().setType(arena.fencemat);
        });
    }

    @Override
    public void updateCountDown(final int sec) {
        if (sec == 0) {
            arenas.forEach(arena -> {
                if (arena.fenceloc == null) return;
                arena.fenceloc.getBlock().setType(Material.AIR);
            });
            players.forEach(player -> ms.sendScoreboard(player, sb));
        }
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Sei der schnellste, der unten im Wasser",
                "§7landet, ohne ein Hindernis zu treffen.",
                "§7Erreiche durch das Portal neue Level.",
                "",
                "§7Teilnehmer: §d" + players.size()
        };
    }

    @EventHandler
    public void onPortal(final PlayerPortalEvent e) {
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL && players.contains(e.getPlayer())) {
            final Player p = e.getPlayer();
            final Arena current = getCurrent(p), next = getNext(current);
            e.setCancelled(true);
            if (next == null) {
                finish(p);
            } else {
                ms.forcePlayer(p, next.getLocation());
                playerarena.put(p, next);
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 20, 20);
                ms.sendTitle(p, "§dNächstes Level erreicht", 10, 80, 10);
                ms.sendScoreboard(p, sb);
            }
        }
    }

    @EventHandler
    public void onDamage(final EntityDamageEvent e) {
        if (e.getEntityType() == EntityType.PLAYER && e.getEntity().getWorld() == world) {
            final Player p = (Player) e.getEntity();
            if (players.contains(p)) {
                e.setCancelled(true);
                if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    ms.forcePlayer(p, getCurrent(p).getLocation());
                    if (p.getFireTicks() > 0) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (p.isOnline())
                                    p.setFireTicks(0);
                            }
                        }.runTaskLater(ms, 1L);
                    }
                }
            }
        }
    }

    private Arena getCurrent(final Player p) {
        return playerarena.get(p);
    }

    private Arena getNext(final Arena arena) {
        final int index = arenas.indexOf(arena);
        return arenas.size() > (index + 1) ? arenas.get(index + 1) : null;
    }

    class Arena {
        final Location loc, fenceloc;
        final Material fencemat;

        public Arena(final Location loc, final Location fenceloc, final Material fencemat) {
            this.loc = loc;
            this.fenceloc = fenceloc;
            this.fencemat = fencemat;
        }

        public Arena(final Location loc) {
            this.loc = loc;
            this.fenceloc = null;
            this.fencemat = null;
        }

        public Location getLocation() {
            return loc;
        }

        public Location getFenceLocation() {
            return fenceloc;
        }
    }
}