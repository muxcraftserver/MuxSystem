package me.muxteam.events;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class MuxCustom extends Event implements Listener {
    // Ideas: Labyrinth, Wahr Falsch, Climb, Swim fast, Obstacle, Snake, Drop
    // Other: Schatzsuche
    private Location loc = ms.getGameSpawn().clone();

    public MuxCustom(final MuxEvents e) {
        super(e);
        name = "Eigenes Event";
        item = new ItemStack(Material.CAKE);
        canMoveItems = true;
        maxPlayers = 500;
    }

    public void setLoc(final Location l) {
        loc = l.clone();
        ms.setLoc("eventspawn", l);
    }

    public void toggleKeepInventory() {
        keepInventory ^= true;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Dies ist ein Event was spontan von",
                "§7dem MuxTeam erstellt wurde und",
                "§7nicht normal stattfindet.",
                "",
                "§7Teilnehmer: §d" + players.size()
        };
    }

    @Override
    public void start() {
        final Location l = ms.getLoc("eventspawn");
        if (l != null) loc = l;
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das " + name + " §7Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        spawnEventNPC("§d§lEvent beitreten");
    }

    @Override
    public boolean join(final Player p) {
        equipPlayer(p);
        ms.forcePlayer(p, loc.clone().add(0, 4, 0));
        ms.getMounts().deactivateMount(p);
        if (keepInventory == false)
            p.getInventory().setHelmet(new ItemStack(Material.STAINED_GLASS, 1, (byte) r.nextInt(15)));
        p.getInventory().setHeldItemSlot(0);
        return true;
    }

    @Override
    public void quit(final Player p) {
        resetPlayer(p);
    }

    @EventHandler
    public void onDeath(final PlayerDeathEvent e) {
        if (players.contains(e.getEntity()) == false) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                quit(e.getEntity());
            }
        }.runTaskLater(ms, 5L);
        e.getDrops().clear();
    }

    @EventHandler
    public void onDamage(final EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && players.contains((Player) e.getEntity()) && keepInventory) {
            e.setCancelled(true);
            e.setDamage(0D);
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