package me.muxteam.events;

import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MuxChamber extends Event implements Listener {
    private final List<Location> spawns = new ArrayList<>();
    private final Location spec = new Location(ms.getWarpsWorld(), -572, 63D, 534D, 0F, 0F);
    private final Map<Player, Integer> points = new HashMap<>();
    private final MuxScoreboard.ScoreboardType sb;
    private int pointsToWin = 30;
    private boolean win = false;

    public MuxChamber(final MuxEvents e) {
        super(e);
        name = "MuxChamber";
        item = new ItemStack(Material.ARROW);
        maxPlayers = 150;
        canSpectate = true;
        pvp = true;
        sb = ms.getScoreboard().createScoreboard("§7§lMux§d§lChamber");
        sb.setLook((p, board) -> {
            final List<Map.Entry<Player, Integer>> sorted = points.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList());
            Collections.reverse(sorted);
            sb.setLine(board, " ");
            final boolean notSpectating = spectators.containsKey(p.getUniqueId()) == false;
            boolean self = false;
            int i = 0;
            for (final Map.Entry<Player, Integer> top : sorted) {
                if (i == 5) break;
                i++;
                String pname = top.getKey().getName();
                if (pname.length() >= 14) {
                    pname = pname.substring(0, 11) + "..";
                }
                if (top.getKey().getName().equals(p.getName())) {
                    self = true;
                    sb.setSection(board, null,"§d#" + (i) + " §6" +pname + " §d» §a" + top.getValue(), false);
                    continue;
                } else if (self == false && i == 5 && notSpectating == false) break;
                sb.setSection(board, null,"§d#" + (i) + " §f" + pname + " §d» §a" + top.getValue(), false);
            }
            if (self == false && notSpectating == false) {
                sorted.stream().filter(entry -> entry.getKey().getName().equals(p.getName())).findFirst().ifPresent(entry -> {
                    final int pos = sorted.indexOf(entry) + 1;
                    String pname = entry.getKey().getName();
                    if (pname.length() >= 14) {
                        pname = pname.substring(0, 11) + "..";
                    }
                    sb.setSection(board, null, "§d#" + pos + " §6" + pname + " §d» §a" + entry.getValue(), false);
                });
            }
        });
        spectatorSB = sb;
        final World w = ms.getWarpsWorld();
        spawns.add(new Location(w, -610, 62, 494, 317.7F, 11.3F));
        spawns.add(new Location(w, -611, 62, 528, 245.5F, 1.80F));
        spawns.add(new Location(w, -546, 43, 564, 154.5F, -1.5F));
        spawns.add(new Location(w, -592, 42, 507, 335F, 1.35F));
        spawns.add(new Location(w, -573, 43, 517, 12F, -1.5F));
        spawns.add(new Location(w, -567, 48, 545, 214F, 7.5F));
        spawns.add(new Location(w, -591, 64, 527, 301F, 23.1F));
        spawns.add(new Location(w, -571, 48, 566, 144F, 11.7F));
        spawns.add(new Location(w, -531, 41, 505, 60F, 4.35F));
        spawns.add(new Location(w, -543, 52, 503, 29.5F, 13.8F));
        spawns.add(new Location(w, -578, 41, 522, 290F, -11.85F));
        spawns.add(new Location(w, -551, 63, 523, 33.7F, 2.55F));
        spawns.add(new Location(w, -550, 62, 577, 191.5F, -22.35F));
        spawns.add(new Location(w, -584, 48, 516, 300.5F, 12.45F));
        spawns.add(new Location(w, -535, 47, 532, 83.5F, -1.045F));
        spawns.add(new Location(w, -587, 43, 502, 0.90F, 2.10F));
        spawns.add(new Location(w, -549, 42, 520, 53.85F, 2.70F));
        spawns.add(new Location(w, -572, 41, 544, 150.60F, -3.15F));
        spawns.add(new Location(w, -601, 43, 574, 187.20F, 4.05F));
        spawns.add(new Location(w, -577, 65, 528, 23.55F, 43.5F));
        spawns.add(new Location(w, -567, 55, 534, 316.65F, 10.20F));
        maxDuration = 1200000;
    }

    public void win(final Player p) {
        if (win) return;
        win = true;
        resetPlayer(p);
        ms.sendTitle(p, "§d§lMuxChamber", 10, 80, 10);
        ms.sendSubTitle(p, "§fDu hast das Event §agewonnen§f!", 10, 80, 10);
        if (startedByPlayer == false) {
            ms.runLater(new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline() == false) return;
                    ms.sendSubTitle(p, "§fDeine Belohnung: §e§l" + ms.getNumberFormat(giveReward(p, 150)) + " MuxCoins", 0, 60, 10);
                }
            }, 60L);
        }
        ms.broadcastMessage("§d§lMuxChamber>§7 Der Spieler §d" + p.getName() + " §7hat §agewonnen§7!", null, MuxSystem.Priority.LOW);
        final List<Map.Entry<Player, Integer>> sorted = points.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList());
        Collections.reverse(sorted);
        final List<Player> sortedPlayers = new ArrayList<>();
        for (final Map.Entry<Player, Integer> playerIntegerEntry : sorted) {
            sortedPlayers.add(playerIntegerEntry.getKey());
        }
        sortedPlayers.forEach(all -> {
            if (all == p) return;
            int pos = sortedPlayers.indexOf(all) + 1;
            ms.sendTitle(all, "§d§lMuxChamber", 15, 80, 10);
            ms.sendSubTitle(all, "§fDein Platz: §d#" + pos, 15, 80, 10);
        });
        stopEvent(false);
    }

    @Override
    public Location getSpectateLocation() {
        return spec;
    }

    @Override
    public boolean join(final Player p) {
        equipPlayer(p);
        ms.forcePlayer(p, spawns.get(r.nextInt(spawns.size())));
        points.put(p, 0);
        return true;
    }

    @Override
    public void stop() {
        points.clear();
        win = false;
    }

    @Override
    public void start() {
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das MuxChamber §7Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        startCountdown(60, false, false);
        spawnEventNPC("§d§lEvent beitreten");
        final int online = ms.getServer().getOnlinePlayers().size();
        if (online > 200) {
            pointsToWin = 150;
        } else if (online > 50) {
            pointsToWin = 100;
        } else {
            pointsToWin = 30;
        }
    }

    @Override
    public void quit(final Player p) {
        resetPlayer(p);
        points.remove(p);
        if (canjoin || cancelled) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                players.forEach(all -> ms.sendScoreboard(all, sb));
            }
        }.runTaskLater(ms, 5L);
        if (players.size() == 1 && win == false) {
            win(players.iterator().next());
        }
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Treffe andere Spieler mit deinem Bogen",
                "§7oder besiege sie mit einem Schwert. Wer",
                "§7zuerst §d" + pointsToWin + " Punkte §7erreicht, gewinnt.",
                "",
                "§7Teilnehmer: §d" + players.size()
        };
    }

    @Override
    public void updateCountDown(final int sec) {
        if (sec == 0) {
            players.forEach(p -> {
                ms.forcePlayer(p, spawns.get(r.nextInt(spawns.size())));
                ms.sendScoreboard(p, sb);
                p.getInventory().addItem(new ItemStack(ms.renameItemStack(new ItemStack(Material.WOOD_SWORD), "§a§lMux§6§lSchwert")));
                p.getInventory().addItem(new ItemStack(ms.renameItemStack(new ItemStack(Material.BOW), "§a§lMux§f§lBogen")));
                p.getInventory().addItem(new ItemStack(Material.ARROW));
            });
        }
    }

    @EventHandler
    public void onDamageByEntity(final EntityDamageByEntityEvent e) {
        if ((e.getEntity() instanceof Player) == false) return;
        final Player p = (Player) e.getEntity();
        if (players.contains(p) == false) return;
        if (canjoin) {
            e.setCancelled(true);
            return;
        }
        Player d;
        boolean arrow = false;
        if (e.getDamager().getType() == EntityType.ARROW) {
            Projectile projectile = (Projectile) e.getDamager();
            if (projectile.getShooter() instanceof Player) {
                d = (Player) projectile.getShooter();
                arrow = true;
            } else return;
        } else if (e.getDamager().getType() == EntityType.PLAYER) {
            d = (Player) e.getDamager();
        } else {
            e.setCancelled(true);
            return;
        }
        if (d == null || d == p || players.contains(d) == false) {
            e.setCancelled(true);
            return;
        }
        if (arrow || p.getHealth() - e.getFinalDamage() <= 0D) {
            int pointsAsInt = 0;
            if (arrow) {
                ms.showItemBar(d, "§fDu hast §d" + p.getName() + " §fabgeschossen. §a(+1)");
                ms.showItemBar(p, "§cDu wurdest von §d" + d.getName() + " §cabgeschossen.");
                points.put(d, points.get(d) + 1);
                pointsAsInt = points.get(d);
            } else if (p.getHealth() - e.getFinalDamage() <= 0D) {
                ms.showItemBar(d, "§fDu hast §d" + p.getName() + " §fbesiegt. §a(+1)");
                ms.showItemBar(p, "§cDu wurdest von §d" + d.getName() + " §cbesiegt.");
                points.put(d, points.get(d) + 1);
                pointsAsInt = points.get(d);
            }
            e.setDamage(0D);
            p.setHealth(20);
            d.setHealth(20);
            ms.forcePlayer(p, spawns.get(r.nextInt(spawns.size())));
            p.getInventory().clear();
            p.getInventory().addItem(new ItemStack(ms.renameItemStack(new ItemStack(Material.WOOD_SWORD), "§f§lMux§6§lSchwert")));
            p.getInventory().addItem(new ItemStack(ms.renameItemStack(new ItemStack(Material.BOW), "§f§lMux§6§lBogen")));
            p.getInventory().addItem(new ItemStack(Material.ARROW));
            d.getInventory().addItem(new ItemStack(Material.ARROW));
            players.forEach(all -> ms.sendScoreboard(all, sb));
            if (pointsAsInt == pointsToWin) {
                win(d);
            }
        }
    }

    @EventHandler
    public void onProjectileHit(final ProjectileHitEvent e) {
        if (e.getEntityType() != EntityType.ARROW) return;
        final Arrow arrow = (Arrow) e.getEntity();
        if (arrow.getShooter() instanceof Player) {
            final Player p = (Player) arrow.getShooter();
            if (players.contains(p)) {
                e.getEntity().remove();
            }
        }
    }
}