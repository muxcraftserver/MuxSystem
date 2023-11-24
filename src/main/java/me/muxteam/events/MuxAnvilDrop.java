package me.muxteam.events;

import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class MuxAnvilDrop extends Event implements Listener {
    private final Location loc = new Location(ms.getWarpsWorld(), 552, 18, 579);
    private final List<Location> anvillocations = new ArrayList<>();
    private final List<FallingBlock> fallingBlocks = new ArrayList<>();
    private int anvilcount = 1, roundsWithAllFields = 0;
    private final MuxScoreboard.ScoreboardType sb;

    public MuxAnvilDrop(final MuxEvents e) {
        super(e);
        name = "MuxAmboss";
        item = new ItemStack(Material.ANVIL);
        canSpectate = true;
        maxPlayers = 150;
        playerHider = 0;
        maxSpectatorDistance = 21D;
        sb = ms.getScoreboard().createScoreboard("§7§lMux§d§lAmboss");
        sb.setLook((p, board) -> {
            sb.setLine(board, "");
            sb.setSection(board, "§c§lVerbleibend", players.size() + " Spieler", false);
            sb.setLine(board, "  ");
            sb.setSection(board, "§3§lSchwierigkeit", anvilcount + " Amboss" + (anvilcount > 1 ? "e" : ""), false);
        });
        spectatorSB = sb;
    }

    private void delayedDeath(final Player p) {
        new BukkitRunnable() {
            @Override
            public void run() {
                death(p);
            }
        }.runTaskLater(ms, 5L);
    }

    private void death(final Player p) {
        resetPlayer(p);
        if (canjoin || cancelled) return;
        sendEventMessage("§7Der Spieler §d" + p.getName() + " §7ist §causgeschieden§7.");
        ms.showItemBar(p, "§cDu bist aus dem Event ausgeschieden.");
        ms.sendTitle(p, "§d§lMuxAmboss", 10, 80, 10);
        ms.sendSubTitle(p, "§fDein Platz: §d#" + (players.size() + 1) + " §7(" + anvilcount + " Ambosse)", 10, 80, 10);
        final boolean won = players.isEmpty();
        if (anvilcount > 2 && startedByPlayer == false) {
            ms.runLater(new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline() == false) return;
                    ms.sendSubTitle(p, "§fDeine Belohnung: §e§l" + ms.getNumberFormat(giveReward(p, Math.min(5, 5 + ((anvilcount - 100) / 2)))) + " MuxCoins", 0, 60, 10);
                }
            }, 60L);
        }
        if (won) {
            if (anvilcount > 2) ms.broadcastMessage("§d§lMuxAmboss>§7 Der Spieler §d" + p.getName() + " §7hat §agewonnen§7! (" + anvilcount + ")", null, MuxSystem.Priority.LOW);
            stopEvent(true);
        }
    }
    private void removeAnvils(final World w, final Location c) {
        final int cx = c.getBlockX(), cy = c.getBlockY(), cz = c.getBlockZ(), rad = 15;
        final int rSquared = rad * rad;
        for (int x = cx - rad; x <= cx + rad; x++) {
            for (int z = cz - rad; z <= cz + rad; z++) {
                if ((cx - x) * (cx - x) + (cz - z) * (cz - z) <= rSquared) {
                    w.getBlockAt(x, cy, z).setType(Material.AIR);
                }
            }
        }
    }
    private void genSpawns(final World w, final Location c) {
        anvillocations.clear();
        final int cx = c.getBlockX(), cy = c.getBlockY(), cz = c.getBlockZ(), rad = 15;
        final int rSquared = rad * rad;
        for (int x = cx - rad; x <= cx + rad; x++) {
            for (int z = cz - rad; z <= cz + rad; z++) {
                if ((cx - x) * (cx - x) + (cz - z) * (cz - z) <= rSquared) {
                    anvillocations.add(w.getBlockAt(x, cy, z).getLocation());
                }
            }
        }
    }

    @Override
    public void start() {
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das MuxAmboss Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        genSpawns(loc.getWorld(), loc.clone().add(0, 50, 0));
        removeAnvils(loc.getWorld(), loc.clone().add(0, -1, 0));
        startCountdown(60, false, false);
        spawnEventNPC("§d§lEvent beitreten");
    }

    @Override
    public void stop() {
        anvilcount = 1;
        fallingBlocks.forEach(org.bukkit.entity.Entity::remove);
        fallingBlocks.clear();
        roundsWithAllFields = 0;
    }

    @Override
    public Location getSpectateLocation() {
        return loc;
    }

    @Override
    public long getUpdateTime() {
        return 40L;
    }

    @Override
    public void updateCountDown(final int sec) {
        if (sec == 0) {
            for (final Player pl : players) {
                ms.sendNormalTitle(pl, "§aSchaue nach oben!", 10, 40, 10);
            }
        }
    }

    @Override
    public void update() {
        if (canjoin) return;
        int i = 0;
        final List<Location> clonedLocations = new ArrayList<>(anvillocations);
        while (i < anvilcount) {
            final Location randomLocation = clonedLocations.remove(r.nextInt(clonedLocations.size()));
            final FallingBlock block = randomLocation.getWorld().spawnFallingBlock(randomLocation, Material.ANVIL, (byte) 0);
            block.setHurtEntities(true);
            block.setDropItem(false);
            fallingBlocks.add(block);
            i++;
        }
        players.forEach(player -> ms.sendScoreboard(player, sb));
        if (clonedLocations.isEmpty()) {
            if (++roundsWithAllFields == 20) {
                players.forEach(this::death);
            }
        } else {
            anvilcount++;
        }
    }

    @Override
    public boolean join(final Player p) {
        equipPlayer(p);
        ms.forcePlayer(p, loc.clone().add(0.5, 1, 0.5));
        return true;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Achtung! Von der Luft fallen Ambosse!",
                "§7Vermeide so lange es geht, von denen",
                "§7direkt getroffen zu werden.",
                "",
                "§7Teilnehmer: §d" + players.size()
        };
    }

    @Override
    public void quit(final Player p) {
        death(p);
    }

    @EventHandler
    public void onEntityDamage(final EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && players.contains((Player) e.getEntity())) {
            if (e.getCause() == EntityDamageEvent.DamageCause.FALLING_BLOCK) {
                e.setDamage(0D);
                delayedDeath((Player) e.getEntity());
            } else {
                e.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onEntityChangeBlock(final EntityChangeBlockEvent e) {
        if (e.getEntity() instanceof FallingBlock == false) return;
        final FallingBlock fb = (FallingBlock) e.getEntity();
        if (fallingBlocks.remove(fb)) {
            e.setCancelled(true);
        }
    }
}