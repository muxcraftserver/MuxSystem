package me.muxteam.events;

import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MuxTopArena extends Event implements Listener {
    private final Location loc = new Location(ms.getWarpsWorld(), -290.5D, 39D, 415.5D, -90F, -17.5F),
            spec = new Location(ms.getWarpsWorld(), -269.5D, 40D, 402.5D, 40F, 0F);
    private final Map<Player, Integer> killcount = new HashMap<>();
    private final Map<Player, Long> spawnprotection = new HashMap<>();
    private final Map<Player, Player> lastenemies = new HashMap<>();
    private boolean finished = false;
    private final MuxScoreboard.ScoreboardType sb;

    public MuxTopArena(final MuxEvents e) {
        super(e);
        name = "MuxTopArena";
        item = new ItemStack(Material.DIAMOND_SWORD);
        canSpectate = true;
        maxPlayers = 250;
        maxSpectatorDistance = 55D;
        sb = ms.getScoreboard().createScoreboard("§7§lMux§d§lTopArena");
        sb.setLook((p, board) -> {
            final List<Map.Entry<Player, Integer>> sorted = killcount.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList());
            Collections.reverse(sorted);
            sb.setLine(board, " ");
            final boolean spectator = spectators.containsKey(p.getUniqueId());
            boolean self = false;
            int i = 0;
            for (final Map.Entry<Player, Integer> top : sorted) {
                if (i == 5) break;
                i++;
                if (top.getKey().getName().equals(p.getName())) {
                    self = true;
                    sb.setSection(board, null,"§d#" + (i) + " §6" + top.getKey().getName() + " §d» §a" + top.getValue(), false);
                    continue;
                } else if (self == false && i == 5 && spectator == false) break;
                sb.setSection(board, null,"§d#" + (i) + " §f" + top.getKey().getName() + " §d» §a" + top.getValue(), false);
            }
            if (self == false && spectator == false) {
                sorted.stream().filter(entry -> entry.getKey().getName().equals(p.getName())).findFirst().ifPresent(entry -> {
                    final int pos = sorted.indexOf(entry) + 1;
                    sb.setSection(board, null, "§d#" + pos + " §6" + entry.getKey().getName() + " §d» §a" + entry.getValue(), false);
                });
            }
            sb.setLine(board, "  ");
            sb.setSection(board, "§6§lZeit", (cancelTime < System.currentTimeMillis() + 500L) ? "§fnoch 0 Sekunden" : "§fnoch " + ms.getTime((int) ((cancelTime - System.currentTimeMillis()) / 1000)), false);
        });
        spectatorSB = sb;
        maxDuration = 620000;
    }

    private void finish() {
        if (finished) return;
        finished = true;
        final List<Map.Entry<Player, Integer>> sorted = killcount.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList());
        Collections.reverse(sorted);
        final List<Player> sortedPlayers = new ArrayList<>();
        if (sorted.isEmpty() == false) {
            for (final Map.Entry<Player, Integer> playerIntegerEntry : sorted) {
                sortedPlayers.add(playerIntegerEntry.getKey());
            }
            final Map.Entry<Player, Integer> w = sorted.get(0);
            final Player winner = w.getKey();
            ms.sendTitle(winner, "§d§lMuxTopArena", 10, 80, 10);
            ms.sendSubTitle(winner, "§fDu hast das Event §agewonnen§f!", 10, 80, 10);
            if (startedByPlayer == false) {
                ms.runLater(new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (winner.isOnline() == false) return;
                        ms.sendSubTitle(winner, "§fDeine Belohnung: §e§l" + ms.getNumberFormat(giveReward(winner, 150)) + " MuxCoins", 0, 60, 10);
                    }
                }, 60L);
            }
            final int kills = w.getValue();
            ms.broadcastMessage("§d§lMuxTopArena>§7 Der Spieler §d" + winner.getName() + " §7hat §agewonnen§7! (" + kills + " Kill" + (kills == 1 ? "" : "s") + ")", null, MuxSystem.Priority.LOW);

            sortedPlayers.forEach(all -> {
                if (all == winner) return;
                int pos = sortedPlayers.indexOf(all) + 1;
                ms.sendTitle(all, "§d§lMuxTopArena", 15, 80, 10);
                ms.sendSubTitle(all, "§fDein Platz: §d#" + pos, 15, 80, 10);
            });
        }
        stopEvent(false);
    }

    private void death(final Player p, final boolean quit) {
        if (quit == false) {
            spawnprotection.put(p, System.currentTimeMillis() + 3000L);
            p.getInventory().clear();
            p.setHealth(20);
            ms.forcePlayer(p, loc.clone().add(0, 4, 0));
            giveKit(p);
            lastenemies.remove(p);
        } else {
            if (lastenemies.containsKey(p)) {
                killcount.put(lastenemies.get(p), killcount.getOrDefault(lastenemies.get(p), 0) + 1);
            }
            killcount.remove(p);
            players.forEach(all -> ms.sendScoreboard(all, sb));
            resetPlayer(p);
            if (canjoin || cancelled) return;
            if (players.size() == 1) {
                finish();
            }
        }
    }

    private void giveKit(final Player p) {
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
        p.getInventory().addItem(new ItemStack(Material.POTION, 16, (short) 16421));
        p.getInventory().addItem(new ItemStack(Material.POTION, 8, (short) 16425));
        p.getInventory().addItem(new ItemStack(Material.POTION, 8, (short) 16418));
        p.getInventory().setArmorContents(new ItemStack[]{b, l, c, h});
        p.getInventory().setHeldItemSlot(0);
    }

    @Override
    public void stop() {
        spawnprotection.clear();
        killcount.clear();
        lastenemies.clear();
    }

    @Override
    public Location getSpectateLocation() {
        return spec;
    }

    @Override
    public void updateCountDown(final int sec) {
        if (sec == 0) {
            players.forEach(p -> killcount.put(p, 0));
            players.forEach(p -> ms.sendScoreboard(p, sb));
        }
    }

    @Override
    public void start() {
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das " + getName() + " §7Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        startCountdown(60, false, false);
        spawnEventNPC("§d§lEvent beitreten");
        finished = false;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Kämpfe gegen alle anderen Spieler und",
                "§7versuche, §ddie meisten Kills §7zu erzielen.",
                "§7Du wirst bei einem Tod wieder respawnt.",
                "",
                "§7Teilnehmer: §d" + players.size()
        };
    }

    @Override
    public boolean join(final Player p) {
        equipPlayer(p);
        ms.forcePlayer(p, loc.clone().add(0, 4, 0));
        giveKit(p);
        return true;
    }

    @Override
    public long getUpdateTime() {
        return 20L;
    }

    @Override
    public boolean timeOver() {
        finish();
        return true;
    }

    @Override
    public void quit(final Player p) {
        death(p, true);
        players.forEach(all -> ms.sendScoreboard(all, sb));
    }

    @EventHandler
    public void onDamage(final EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player == false || players.contains((Player) e.getEntity()) == false ||
                e.getDamager() instanceof Player == false || players.contains((Player) e.getDamager()) == false) {
            return;
        } else if (canjoin) {
            e.setCancelled(true);
            ms.showItemBar((Player) e.getDamager(), "§cWarte bis das Event startet.");
            return;
        }
        final Player p = (Player) e.getEntity(), d = (Player) e.getDamager();
        if (spawnprotection.getOrDefault(p, 0L) > System.currentTimeMillis()) {
            ms.showItemBar((Player) e.getDamager(), "§cDer Spieler §d" + e.getEntity().getName() + " §chat noch Spawnschutz.");
            e.setCancelled(true);
            return;
        } else if (spawnprotection.getOrDefault(d, 0L) > System.currentTimeMillis()) {
            ms.showItemBar((Player) e.getDamager(), "§cDu hast derzeit noch Spawnschutz.");
            e.setCancelled(true);
            return;
        }
        lastenemies.put(p, d);
        lastenemies.put(d, p);
        if (p.getHealth() - e.getFinalDamage() <= 0D) {
            e.setCancelled(true);
            p.damage(0);
            p.setHealth(20D);
            p.setVelocity(new Vector(0, 0, 0));
            killcount.put(d, killcount.getOrDefault(d, 0) + 1);
            ms.showItemBar(d, "§fDu hast §d" + p.getName() + " §fbesiegt. §a(+1)");
            ms.showItemBar(p, "§cDu wurdest von §d" + d.getName() + " §cbesiegt.");
            death(p, false);
            players.forEach(all -> ms.sendScoreboard(all, sb));
        }
    }
}