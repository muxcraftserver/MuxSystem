package me.muxteam.events;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.blocks.BaseBlock;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ConcurrentHashMap;

public class MuxTNTRun extends Event implements Listener {
    private final Location loc = new Location(ms.getWarpsWorld(), -45.5D, 53D, 212D);
    private final ConcurrentHashMap<Block, Long> removeblocks = new ConcurrentHashMap<>();

    public MuxTNTRun(final MuxEvents e) {
        super(e);
        name = "MuxTNTRun";
        item = new ItemStack(Material.TNT);
        canSpectate = true;
        scoreboard = true;
        maxPlayers = 150;
        maxDuration = 900000;
        maxSpectatorDistance = 50D;
    }

    private boolean death(final Player p) {
        resetPlayer(p);
        if (canjoin || cancelled) return true;
        sendEventMessage("§7Der Spieler §d" + p.getName() + " §7ist §causgeschieden§7.");
        ms.showItemBar(p, "§cDu bist aus dem Event ausgeschieden.");
        ms.sendTitle(p, "§d§lMuxTNTRun", 10, 80, 10);
        ms.sendSubTitle(p, "§fDein Platz: §d#" + (players.size() + 1), 10, 80, 10);
        if (players.size() == 1) {
            final Player winner = players.iterator().next();
            resetPlayer(winner);
            ms.sendTitle(winner, "§d§lMuxTNTRun", 10, 80, 10);
            ms.sendSubTitle(winner, "§fDu hast das Event §agewonnen§f!", 10, 80, 10);
            if (startedByPlayer == false) {
                ms.runLater(new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (winner.isOnline() == false) return;
                        ms.sendSubTitle(winner.getPlayer(), "§fDeine Belohnung: §e§l" + ms.getNumberFormat(giveReward(winner, 150)) + " MuxCoins", 0, 60, 10);
                    }
                }, 60L);
            }
            ms.broadcastMessage("§d§lMuxTNTRun>§7 Der Spieler §d" + winner.getName() + " §7hat §agewonnen§7!", null, MuxSystem.Priority.LOW);
            stopEvent(true);
            return true;
        }
        return false;
    }

    private Block getStandingBlock(final Location l) {
        final Block b1 = l.clone().add(0.3, -1, -0.3).getBlock(), b2 = l.clone().add(-0.3, -1, -0.3).getBlock(),
                b3 = l.clone().add(0.3, -1, 0.3).getBlock(), b4 = l.clone().add(-0.3, -1, 0.3).getBlock();
        if (b1.getType() != Material.AIR) return b1;
        else if (b2.getType() != Material.AIR) return b2;
        else if (b3.getType() != Material.AIR) return b3;
        else if (b4.getType() != Material.AIR) return b4;
        return null;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Versuche nicht runterzufallen, indem du",
                "§7auf diejenigen Blöcken läufst, die noch",
                "§7übrig geblieben sind.",
                "",
                "§7Teilnehmer: §d" + players.size()
        };
    }

    @Override
    public long getUpdateTime() {
        return 1L;
    }

    @Override
    public Location getSpectateLocation() {
        return loc;
    }

    int count = 0;
    @Override
    public void update() {
        if (canjoin) return;
        removeblocks.entrySet().removeIf(entry -> {
            if (entry.getValue() < System.currentTimeMillis()) {
                entry.getKey().setType(Material.AIR);
                return true;
            }
            return false;
        });
        count++;
        if (count == 10) {
            count = 0;
            for (final Player pl : players) {
                if (pl.getLocation().getBlockY() < 28) {
                    if (death(pl)) break;
                    continue;
                }
                if (canjoin == false && pl.isSprinting() == false) {
                    final Block under = getStandingBlock(pl.getLocation());
                    if (under == null || (under.getType() != Material.DIRT && under.getType() != Material.GRASS && under.getType() != Material.STONE &&
                            under.getType() != Material.IRON_ORE && under.getType() != Material.COAL_ORE && under.getType() != Material.COAL_BLOCK &&
                            under.getType() != Material.COBBLESTONE)) continue;
                    removeblocks.put(under, System.currentTimeMillis() + 225L);
                }
            }
        }
    }

    @Override
    public void generate() {
        final World w = loc.getWorld();
        final EditSession session = ms.getFastBuilder(w.getName());
        genCircle(w, loc.clone().subtract(0, 1, 0), Material.GRASS, Material.DIRT, Material.GRASS, session);
        genCircle(w, loc.clone().subtract(0, 12, 0), Material.STONE, Material.IRON_ORE, Material.COAL_ORE, session);
        genCircle(w, loc.clone().subtract(0, 22, 0), Material.COBBLESTONE, Material.COAL_BLOCK, Material.STONE, session);
        session.flushQueue();
    }

    private void genCircle(final World w, final Location c, final Material m1, final Material m2, final Material m3, final EditSession session) {
        int cx = c.getBlockX(), cy = c.getBlockY(), cz = c.getBlockZ(), rad = 39;
        int rSquared = rad * rad;
        for (int x = cx - rad; x <= cx + rad; x++) {
            for (int z = cz - rad; z <= cz + rad; z++) {
                if ((cx - x) * (cx - x) + (cz - z) * (cz - z) <= rSquared) {
                    final Location location = new Location(w, x, cy, z);
                    session.setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), new BaseBlock(r.nextBoolean() ? m1.getId() : r.nextDouble() < 0.8 ? m2.getId() : m3.getId()));
                }
            }
        }
    }

    @Override
    public void start() {
        generate();
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das MuxTNTRun §7Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        startCountdown(60, false, false);
        spawnEventNPC("§d§lEvent beitreten");
    }


    @Override
    public boolean join(final Player p) {
        ms.forcePlayer(p, loc.clone().add(0, 4, 0));
        equipPlayer(p); // do it later to give adventure mode
        return true;
    }

    @Override
    public void quit(final Player p) {
        death(p);
    }

    @EventHandler
    public void onMove(final PlayerMoveEvent e) {
        if (canjoin == false && players.contains(e.getPlayer())) {
            final Location from = e.getFrom(), to = e.getTo();
            if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
                removeblocks.put(from.getBlock().getRelative(BlockFace.DOWN), System.currentTimeMillis() + 225L);
            }
        }
    }
}