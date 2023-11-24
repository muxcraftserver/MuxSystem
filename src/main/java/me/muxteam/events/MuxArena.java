package me.muxteam.events;

import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class MuxArena extends Event implements Listener {
    private final Location loc = new Location(ms.getWarpsWorld(), -290.5D, 39D, 415.5D, -90F, -17.5F),
    spec = new Location(ms.getWarpsWorld(), -269.5D, 40D, 402.5D, 40F, 0F);

    public MuxArena(final MuxEvents e) {
        super(e);
        name = "MuxArena";
        item = new ItemStack(Material.IRON_SWORD);
        canSpectate = true;
        pvp = true;
        scoreboard = true;
        maxPlayers = 250;
        maxDuration = 1200000;
        maxSpectatorDistance = 55D;
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
        if (players.size() == 1) {
            final Player winner = players.iterator().next();
            resetPlayer(winner);
            ms.sendTitle(winner, "§d§lMuxArena", 10, 80, 10);
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
            ms.broadcastMessage("§d§lMuxArena>§7 Der Spieler §d" + winner.getName() + " §7hat §agewonnen§7!", null, MuxSystem.Priority.LOW);
            stopEvent(true);
        }
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Besiege jeden anderen Spieler und sei",
                "§7am Ende der letzte Überlebende. Deine",
                "§7Hilfsmittel sind ein Schwert und Tränke.",
                "",
                "§7Überlebende: §d" + players.size()
        };
    }

    @Override
    public Location getSpectateLocation() {
        return spec;
    }

    @Override
    public void start() {
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das MuxArena Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        startCountdown(60, false, false);
        spawnEventNPC("§d§lEvent beitreten");
    }

    @Override
    public boolean join(final Player p) {
        equipPlayer(p);
        ms.forcePlayer(p, loc.clone().add(0, 4, 0));
        final ItemStack h = ms.renameItemStack(new ItemStack(Material.DIAMOND_HELMET), "§f§lMux§b§lRüstung"),
                c = ms.renameItemStack(new ItemStack(Material.DIAMOND_CHESTPLATE), "§f§lMux§b§lRüstung"),
                l = ms.renameItemStack(new ItemStack(Material.DIAMOND_LEGGINGS), "§f§lMux§b§lRüstung"),
                b = ms.renameItemStack(new ItemStack(Material.DIAMOND_BOOTS), "§f§lMux§b§lRüstung");
        final Map<Enchantment, Integer> aEnch = new HashMap<>();
        aEnch.put(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        aEnch.put(Enchantment.DURABILITY, 3);
        h.addEnchantments(aEnch);
        c.addEnchantments(aEnch);
        l.addEnchantments(aEnch);
        b.addEnchantments(aEnch);
        final ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 5);
        final ItemMeta swordm = sword.getItemMeta();
        swordm.setDisplayName("§f§lMux§b§lSchwert");
        swordm.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        swordm.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        sword.setItemMeta(swordm);
        p.getInventory().setItem(0, sword);
        p.getInventory().addItem(new ItemStack(Material.POTION, 128, (short) 16421));
        p.getInventory().addItem(new ItemStack(Material.POTION, 16, (short) 16425));
        p.getInventory().addItem(new ItemStack(Material.POTION, 16, (short) 16418));
        p.getInventory().setArmorContents(new ItemStack[]{b, l, c, h});
        p.getInventory().setHeldItemSlot(0);
        return true;
    }

    @Override
    public void quit(final Player p) {
        death(p);
    }

    @EventHandler
    public void onDamage(final EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player && players.contains((Player) e.getDamager()) && canjoin) {
            e.setCancelled(true);
            ms.showItemBar((Player) e.getDamager(), "§cWarte bis das Event startet.");
        }
    }

    @EventHandler
    public void onDeath(final PlayerDeathEvent e) {
        if (players.contains(e.getEntity()) == false) {
            return;
        }
        Player killer = null;
        final EntityDamageEvent dmg = e.getEntity().getLastDamageCause();
        if (dmg instanceof EntityDamageByEntityEvent) {
            final EntityDamageByEntityEvent dmge = (EntityDamageByEntityEvent) dmg;
            final EntityType et = dmge.getDamager().getType();
            if (et.equals(EntityType.ARROW)) {
                final Projectile arrow = (Arrow) dmge.getDamager();
                if (arrow.getShooter() instanceof Player) {
                    killer = (Player) arrow.getShooter();
                }
            } else if (et.equals(EntityType.PLAYER)) {
                killer = (Player) dmge.getDamager();
            }
        }
        if (killer != null) {
            killer.getInventory().addItem(new ItemStack(Material.POTION, 12, (short) 16421));
            killer.getInventory().addItem(new ItemStack(Material.POTION, 1, (short) 16425));
            ms.showItemBar(killer, "§d§lKILL! §f+ 12 Heiltränke");
        }
        delayedDeath(e.getEntity());
        e.getDrops().clear();
    }
}