package me.muxteam.events;

import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MuxTNT extends Event implements Listener {
    private final List<Location> spawns = new ArrayList<>();
    private final Location spec = new Location(ms.getWarpsWorld(), -397.5D, 21D, 243.5D, -90F, 0F);

    public MuxTNT(final MuxEvents e) {
        super(e);
        name = "MuxTNT";
        item = new ItemStack(Material.BOW);
        canSpectate = true;
        pvp = true;
        scoreboard = true;
        maxPlayers = 150;
        maxDuration = 900000;
        maxSpectatorDistance = 110D;
        final World w = ms.getWarpsWorld();
        spawns.add(new Location(w, -368.5D, 18D, 283.5D, 90F, 0F));
        spawns.add(new Location(w, -397.5D, 17D, 201.5D, -20F, 0F));
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
        ms.sendTitle(p, "§d§lMuxTNT", 10, 80, 10);
        ms.sendSubTitle(p, "§fDein Platz: §d#" + (players.size() + 1), 10, 80, 10);
        if (players.size() == 1) {
            final Player winner = players.iterator().next();
            resetPlayer(winner);
            ms.sendTitle(winner, "§d§lMuxTNT", 10, 80, 10);
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
            ms.broadcastMessage("§d§lMuxTNT>§7 Der Spieler §d" + winner.getName() + " §7hat §agewonnen§7!", null, MuxSystem.Priority.LOW);
            stopEvent(true);
        }
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Versuche alle gegnerischen Spieler mit",
                "§7explosiven Bogenschüssen und großer",
                "§7Sprungkraft zu eliminieren.",
                "",
                "§7Teilnehmer: §d" + players.size()
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
            if (m == Material.WATER || m == Material.STATIONARY_WATER) {
                ms.forcePlayer(pl, spawns.get(r.nextInt(spawns.size())));
            }
        }
    }

    @Override
    public Location getSpectateLocation() {
        return spec;
    }

    @Override
    public void start() {
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das MuxTNT §7Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        startCountdown(60, false, false);
        spawnEventNPC("§d§lEvent beitreten");
    }

    @Override
    public boolean join(final Player p) {
        equipPlayer(p);
        ms.forcePlayer(p, spawns.get(r.nextInt(spawns.size())));
        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 86400000, 6));
        final PlayerInventory pi = p.getInventory();
        pi.setHelmet(new ItemStack(Material.LEATHER_HELMET));
        pi.setBoots(new ItemStack(Material.LEATHER_BOOTS));
        pi.setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
        pi.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        final ItemStack bow = new ItemStack(Material.BOW);
        bow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
        bow.addUnsafeEnchantment(Enchantment.DURABILITY, 100);
        final ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        bowMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        bow.setItemMeta(bowMeta);
        pi.setItem(0, ms.renameItemStack(bow, "§f§lMux§6§lBogen"));
        pi.setItem(9, new ItemStack(Material.ARROW));
        pi.setHeldItemSlot(0);
        return true;
    }

    @Override
    public void quit(final Player p) {
        death(p);
    }

    @EventHandler
    public void onExplosion(final BlockExplodeEvent e) {
        final Iterator<Block> it = e.blockList().iterator();
        while (it.hasNext()) it.remove();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(final EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            final Player p = (Player) e.getEntity();
            if (players.contains(p) == false) return;
            if (e.getCause().equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) {
                e.setCancelled(false);
                e.setDamage(5D);
            } else {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(final PlayerDeathEvent e) {
        final Player p = e.getEntity();
        if (players.contains(p)) {
            e.getDrops().clear();
            e.setDroppedExp(0);
            delayedDeath(p);
        }
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        final Player p = e.getPlayer();
        if (players.contains(p) == false) return;
        if (canjoin) {
            e.setCancelled(true);
            return;
        }
        if (e.getMaterial() != Material.BOW) e.setCancelled(true);
    }

    @EventHandler
    public void onProjectileHit(final ProjectileHitEvent e) {
        if (e.getEntity() instanceof Arrow) {
            final Arrow arrow = (Arrow) e.getEntity();
            if (arrow.getShooter() instanceof Player == false) {
                return;
            }
            final Player p = (Player) arrow.getShooter();
            if (players.contains(p)) {
                final Location l = arrow.getLocation();
                p.getWorld().createExplosion(l.getX(), l.getY(), l.getZ(), 2F, false, false);
            }
        }
    }
}