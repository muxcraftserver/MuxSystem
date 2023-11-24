package me.muxteam.events;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;

import java.util.Iterator;

public class MuxSplegg extends Event implements Listener {
    private final Location loc = new Location(ms.getWarpsWorld(), 400D, 26D, 400D);

    public MuxSplegg(final MuxEvents e) {
        super(e);
        name = "MuxSplegg";
        item = new ItemStack(Material.STONE_SPADE);
        canSpectate = true;
        scoreboard = true;
        maxPlayers = 200;
        maxDuration = 480000;
        maxSpectatorDistance = 85D;
    }

    private void death(final Player p) {
        resetPlayer(p);
        if (canjoin || cancelled) return;
        sendEventMessage("§7Der Spieler §d" + p.getName() + "§7 ist §causgeschieden§7.");
        ms.showItemBar(p, "§cDu bist aus dem Event ausgeschieden.");
        ms.sendTitle(p, "§d§lMuxSplegg", 10, 80, 10);
        ms.sendSubTitle(p, "§fDein Platz: §d#" + (players.size() + 1), 10, 80, 10);
        if (players.size() == 1) {
            final Player winner = players.iterator().next();
            resetPlayer(winner);
            ms.sendTitle(winner, "§d§lMuxSplegg", 10, 80, 10);
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
            ms.broadcastMessage("§d§lMuxSplegg>§7 Der Spieler §d" + winner.getName() + " §7hat §agewonnen§7!", null, MuxSystem.Priority.LOW);
            stopEvent(true);
        }
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Schieße Blöcke weg mit deiner Schaufel",
                "§7und versuche die gegnerischen Spieler",
                "§7runterfallen zu lassen.",
                "",
                "§7Teilnehmer: §d" + players.size()
        };
    }

    @Override
    public long getUpdateTime() {
        return 5L;
    }

    @Override
    public Location getSpectateLocation() {
        return loc;
    }

    @Override
    public void update() {
        for (final Player pl : players) {
            if (pl.getLocation().getBlockY() < loc.getBlockY() - 5) {
                if (players.contains(pl) == false) continue;
                death(pl);
            }
        }
    }

    @Override
    public void generate() {
        final World w = loc.getWorld();
        final EditSession session = ms.getFastBuilder(w.getName());
        for (int x = -60; x < 60; x++) {
            for (int z = -60; z < 60; z++) {
                final Location location = loc.clone().add(x, 0, z), location2 = loc.clone().add(x, -5, z);
                session.setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), new BaseBlock(r.nextInt(200) == 0 ? BlockID.TNT : BlockID.SNOW_BLOCK));
                session.setBlock(location2.getBlockX(), location2.getBlockY(), location2.getBlockZ(), new BaseBlock(r.nextInt(200) == 0 ? BlockID.TNT : BlockID.SNOW_BLOCK));
            }
        }
        session.flushQueue();
    }

    @Override
    public void start() {
        generate();
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das MuxSplegg §7Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        startCountdown(60, false, true);
        spawnEventNPC("§d§lEvent beitreten");
    }

    @Override
    public boolean join(final Player p) {
        equipPlayer(p);
        ms.forcePlayer(p, loc.clone().add(0, 4, 0));
        final ItemStack spade = new ItemStack(Material.STONE_SPADE);
        spade.addUnsafeEnchantment(Enchantment.DURABILITY, 100);
        final ItemMeta spadem = spade.getItemMeta();
        spadem.setDisplayName("§f§lMux§b§lSchaufel");
        spadem.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        spadem.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        spade.setItemMeta(spadem);
        p.getInventory().setItem(0, spade);
        p.getInventory().setHeldItemSlot(0);
        return true;
    }

    @Override
    public void quit(final Player p) {
        death(p);
    }

    @EventHandler
    public void onBreak(final BlockBreakEvent e) {
        if (players.contains(e.getPlayer()) == false) {
            return;
        }
        e.setCancelled(true);
        if (canjoin) {
            e.getPlayer().updateInventory();
            return;
        } else if (e.getBlock().getType() != Material.SNOW_BLOCK) {
            return;
        }
        e.getBlock().setType(Material.AIR);
        e.getBlock().getWorld().playSound(e.getBlock().getLocation(), Sound.DIG_SNOW, 1F, 1F);
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_AIR) return;
        final Player p = e.getPlayer();
        if (e.getMaterial() == Material.STONE_SPADE && players.contains(p) && canjoin == false) {
            if (ms.checkGeneralCooldown(p.getName(), "SPLEGG", 200L, true)) return;
            p.launchProjectile(Egg.class);
            p.playSound(p.getLocation(), Sound.SHOOT_ARROW, 1F, 1F);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onProjectile(final ProjectileHitEvent e) {
        if (e.getEntityType() != EntityType.EGG) return;
        final Projectile egg = e.getEntity();
        final Player pl = egg.getShooter() instanceof Player ? (Player) egg.getShooter() : null;
        if (pl == null) {
            return;
        }
        final BlockIterator bi = new BlockIterator(egg.getWorld(), egg.getLocation().toVector(), egg.getVelocity().normalize(), 0, 4);
        Block b = null;
        while (bi.hasNext()) {
            b = bi.next();
            if (b.getType().equals(Material.AIR) == false) {
                break;
            }
        }
        if (b == null) return;
        final Material m = b.getType();
        if (m != Material.SNOW_BLOCK && m != Material.TNT) return;
        b.setType(Material.AIR);
        b.getWorld().playSound(b.getLocation(), Sound.LAVA_POP, 1.0F, 1.0F);
        if (m.equals(Material.TNT)) {
            b.getWorld().createExplosion(b.getLocation(), 2.0F);
        }
    }

    @EventHandler
    public void onEgg(final PlayerEggThrowEvent e) {
        if (players.contains(e.getPlayer())) {
            e.setHatching(false);
        }
    }

    @EventHandler
    public void onExplode(final BlockExplodeEvent e) {
        final Iterator<Block> it = e.blockList().iterator();
        e.setYield(0F);
        while (it.hasNext()) {
            final Block b = it.next();
            if (b.getType() != Material.SNOW_BLOCK) it.remove();
            else b.setType(Material.AIR);
        }
    }
}