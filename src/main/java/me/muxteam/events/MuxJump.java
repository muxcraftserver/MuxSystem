package me.muxteam.events;

import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MuxJump extends Event implements Listener {
    private Location loc;
    private final List<UUID> finished = new ArrayList<>(), justfinished = new ArrayList<>();
    private final List<Location> locs = new ArrayList<>();
    private final Map<Player, Location> checkpoints = new HashMap<>();
    private final Map<Player, Integer> totalCheckpoints = new HashMap<>();
    private final MuxScoreboard.ScoreboardType sb;

    public MuxJump(final MuxEvents e) {
        super(e);
        name = "MuxJump";
        item = new ItemStack(Material.LEATHER_BOOTS);
        maxPlayers = 500;
        playerHider = 1;
        sb = ms.getScoreboard().createScoreboard("§7§lMux§d§lJump");
        sb.setLook((p, board) -> {
            sb.setLine(board, "");
            final int checkpoint = totalCheckpoints.getOrDefault(p, 0);
            sb.setSection(board, "§a§lCheckpoint", checkpoint == 0 ? "Noch keins" : ("§fNummer " + checkpoint), false);
            sb.setLine(board, " ");
            sb.setSection(board, "§6§lEnde", (cancelTime < System.currentTimeMillis() + 500L) ? "§fnoch 0 Sekunden" : "§fnoch " + ms.getTime((int) ((cancelTime - System.currentTimeMillis()) / 1000)), false);
        });
        maxDuration = 1500000;
    }

    private void death(final Player pl) {
        Location l = checkpoints.get(pl);
        if (l != null) {
            l = l.clone();
        }
        ms.forcePlayer(pl, l != null ? l : loc);
        ms.sendScoreboard(pl, sb);
    }

    @Override
    public void stop() {
        if (finished.isEmpty() == false) {
            ms.broadcastMessage("§d§lMuxEvent>§7 Das MuxJump Event ist nun zuende.", null, MuxSystem.Priority.LOW);
        }
        locs.clear();
        checkpoints.clear();
        finished.clear();
        ms.getServer().unloadWorld(loc.getWorld(), true);
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Springe und überquere diesen Parkour,",
                "§7ohne herunterzufallen. Die Checkpoints",
                "§7helfen, das Ziel schneller zu erreichen.",
        };
    }

    @Override
    public String[] getAdminInformation() {
        return new String[]{
                "§7Teilnehmer: §d" + players.size(),
                "§7Geschafft: §d" + finished.size()
        };
    }

    @Override
    public long getUpdateTime() {
        return 5L;
    }

    @Override
    public void update() {
        for (final Player pl : players) {
            final Material m = pl.getLocation().getBlock().getType();
            if (pl.getLocation().getBlockY() < 26 || m == Material.LAVA || m == Material.WATER || m == Material.STATIONARY_LAVA || m == Material.STATIONARY_WATER && players.contains(pl)) {
                death(pl);
            }
        }
    }

    @Override
    public void generate() {
        loc = locs.get(r.nextInt(locs.size()));
    }

    @Override
    public void start() {
        ms.getServer().createWorld(new WorldCreator("MuxJump").generateStructures(false).type(org.bukkit.WorldType.FLAT));
        loc = new Location(ms.getServer().getWorld("MuxJump"), 0D, 50D, 0D, 0F, 0F);
        final WorldBorder wb = loc.getWorld().getWorldBorder();
        wb.setSize(575D);
        wb.setCenter(1900D, 1272D);
        locs.add(new Location(loc.getWorld(), 1707D, 35D, 1242D, 180F, 0F));
        locs.add(new Location(loc.getWorld(), 1843D, 35D, 1242D, 180F, 0F));
        locs.add(new Location(loc.getWorld(), 1979D, 35D, 1242D, 180F, 0F));
        locs.add(new Location(loc.getWorld(), 1945D, 35D, 1280D, 0F, 0F));
        locs.add(new Location(loc.getWorld(), 1809D, 35D, 1280D, 0F, 0F));
        generate();
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das MuxJump Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        spawnEventNPC("§d§lEvent beitreten");
    }

    @Override
    public boolean join(final Player p) {
        equipPlayer(p);
        ms.forcePlayer(p, loc);

        final ItemStack tp = ms.renameItemStack(new ItemStack(Material.SLIME_BALL), "§f§lMux§a§lTeleport", "§7Teleportiert dich zurück", "§7zu deinem Checkpoint.");
        p.getInventory().setItem(0, tp);
        p.getInventory().setHeldItemSlot(0);
        ms.sendScoreboard(p, sb);
        return true;
    }

    @Override
    public void quit(final Player p) {
        resetPlayer(p);
        checkpoints.remove(p);
        totalCheckpoints.remove(p);
        justfinished.remove(p.getUniqueId());
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        final Player p = e.getPlayer();
        if (players.contains(p) == false) {
            return;
        }
        if (e.getAction() != Action.PHYSICAL) {
            if (e.getMaterial() != null && e.getMaterial() == Material.SLIME_BALL) {
                death(e.getPlayer());
            }
            return;
        }
        final Block b = e.getClickedBlock();
        if (b == null) {
            return;
        }
        if (b.getType() == Material.IRON_PLATE) {
            final Location l = checkpoints.get(p);
            if (l == null || l.distance(b.getLocation()) >= 10) {
                final Location bl = b.getLocation();
                bl.setYaw(p.getLocation().getYaw());
                checkpoints.put(p, bl.add(0.5D, 0D, 0.5D));
                totalCheckpoints.put(p, totalCheckpoints.getOrDefault(p, 0) + 1);
                ms.sendScoreboard(p, sb);
                p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 1F);
                ms.showItemBar(p, l == null ? "§aLos geht's! Viel Glück!" : "§fDu hast den Checkpoint §aerreicht§f.");
            }
        } else if (b.getType() == Material.GOLD_PLATE) {
            final UUID uuid = p.getUniqueId();
            if (justfinished.contains(uuid)) return;
            ms.sendTitle(p, "§d§lMuxJump", 10, 60, 10);
            ms.sendSubTitle(p, "§fDu hast das Ziel §aerreicht§f!", 10, 60, 10);
            sendEventMessage("§7Der Spieler §d" + p.getName() + " §7hat es zuende §ageschafft§7!" + (finished.contains(uuid) ? "" : " (#" + (finished.size() + 1) + ")"));
            p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 1F, 1F);
            if (finished.contains(uuid)) {
                ms.showLater(p, "§cDu hast bereits die Belohnung erhalten!");
            } else {
                if (startedByPlayer == false) {
                    ms.runLater(new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (p.isOnline() == false) return;
                            ms.sendSubTitle(p, "§fDeine Belohnung: §e§l" + ms.getNumberFormat(giveReward(p, 10)) + " MuxCoins", 0, 60, 10);
                        }
                    }, 60L);
                }
                finished.add(uuid);
            }
            justfinished.add(uuid);
            new BukkitRunnable() {
                @Override
                public void run() {
                    quit(p);
                }
            }.runTaskLater(ms, 60L);
        }
    }
}