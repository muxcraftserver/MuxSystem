package me.muxteam.events;

import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MuxGuns extends Event implements Listener {
    private final List<Location> spawns = new ArrayList<>();
    private final Location spec = new Location(ms.getWarpsWorld(), -270D, 45D, 594D, 0F, 0F);
    private final Map<Player, Integer> points = new HashMap<>();
    private final Map<Player, Long> spawnProtection = new HashMap<>();
    private final MuxScoreboard.ScoreboardType sb;
    private int pointsToWin = 30;
    private boolean win = false;

    public MuxGuns(final MuxEvents e) {
        super(e);
        name = "MuxGuns";
        item = new ItemStack(Material.DIAMOND_BARDING);
        maxPlayers = 250;
        canSpectate = true;
        sb = ms.getScoreboard().createScoreboard("§7§lMux§d§lGuns");
        sb.setLook((p, board) -> {
            final List<Map.Entry<Player, Integer>> sorted = points.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList());
            Collections.reverse(sorted);
            sb.setLine(board, " ");
            final boolean spectator = spectators.containsKey(p.getUniqueId()) == false;
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
                    sb.setSection(board, null,"§d#" + (i) + " §6" + pname + " §d» §a" + top.getValue(), false);
                    continue;
                } else if (self == false && i == 5 && spectator == false) break;
                sb.setSection(board, null,"§d#" + (i) + " §f" + pname + " §d» §a" + top.getValue(), false);
            }
            if (self == false && spectator == false) {
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
        spawns.add(new Location(w, -311.5, 44.0, 530.5, 270F, 4.5F));
        spawns.add(new Location(w, -228.5,44.0,530.5,90F,-1.0F));
        spawns.add(new Location(w, -311.5,44.0,657.5,-90F,1.8F));
        spawns.add(new Location(w, -228.5,44.0,657.5,90F,1.65F));
        spawns.add(new Location(w, -273.5, 36.0, 662.5, 199F, -8.55F));
        spawns.add(new Location(w, -265.5, 36.0, 525.5, 26F, -0.75F));
        spawns.add(new Location(w, -223.5, 36.0, 611.5, 90F, -3.6F));
        spawns.add(new Location(w, -316.5, 36.0, 611.5, -90F, -1.5F));
        spawns.add(new Location(w, -242.5, 36.0, 560.5, 19F, 3.45F));
        spawns.add(new Location(w, -297.5, 36.0, 561.5, -19F, -0.145F));
        spawns.add(new Location(w, -297.5, 36.0, 625.5, 207F, 2.1F));
        spawns.add(new Location(w, -242.5, 36.0, 625.5, 148F, -1.2F));
        spawns.add(new Location(w, -276.5, 36.0, 608.5, 289F, -8.5F));
        spawns.add(new Location(w, -304.0, 36.0, 593.5, 344F, 10.5F));
        spawns.add(new Location(w, -236.5, 36.0, 593.5, 27F, 8F));
        spawns.add(new Location(w, -232.7, 36.0, 650.0, 139F, -1.5F));
        spawns.add(new Location(w, -308.0, 36.0, 537.5, 326F, 1.35F));
        spawns.add(new Location(w, -264.5, 36.0, 552.5, 346F, -0.9F));
        spawns.add(new Location(w, -301.5, 41.0, 600.5, 243F, 6.45F));
        spawns.add(new Location(w, -269.5, 36.0, 596.5, 163F, 0.75F));
        maxSpectatorDistance = 85D;
        maxDuration = 900000;
    }

    private void win(final Player p) {
        win = true;
        resetPlayer(p);
        ms.sendTitle(p, "§d§lMuxGuns", 10, 80, 10);
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
        ms.broadcastMessage("§d§lMuxGuns>§7 Der Spieler §d" + p.getName() + " §7hat §agewonnen§7!", null, MuxSystem.Priority.LOW);
        final List<Map.Entry<Player, Integer>> sorted = points.entrySet().stream().sorted(Comparator.comparingInt(Map.Entry::getValue)).collect(Collectors.toList());
        Collections.reverse(sorted);
        final List<Player> sortedPlayers = new ArrayList<>();
        for (final Map.Entry<Player, Integer> playerIntegerEntry : sorted) {
            sortedPlayers.add(playerIntegerEntry.getKey());
        }
        sortedPlayers.forEach(all -> {
            if (all == p) return;
            int pos = sortedPlayers.indexOf(all) + 1;
            ms.sendTitle(all, "§d§lMuxGuns", 15, 80, 10);
            ms.sendSubTitle(all, "§fDein Platz: §d#" + pos, 15, 80, 10);
        });
        stopEvent(false);
    }

    private ItemStack getGun() {
        return ms.renameItemStack(new ItemStack(Material.DIAMOND_BARDING, 1), "§f§lMux§b§lGun");
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
        spawnProtection.clear();
        win = false;
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
        final int online = ms.getServer().getOnlinePlayers().size();
        if (online > 200) {
            pointsToWin = 200;
        } else if (online > 50) {
            pointsToWin = 150;
        } else {
            pointsToWin = 50;
        }
    }

    @Override
    public void quit(final Player p) {
        resetPlayer(p);
        points.remove(p);
        spawnProtection.remove(p);
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
                "§7Schieße mit deiner Gun so viele Spieler",
                "§7ab wie nur möglich. Derjenige, der als",
                "§7Erstes §d" + pointsToWin + " Punkte §7erreicht, gewinnt.",
                "",
                "§7Teilnehmer: §d" + players.size()
        };
    }

    @Override
    public void updateCountDown(final int sec) {
        if (sec == 0) {
            players.forEach(p -> {
                ms.sendScoreboard(p, sb);
                p.getInventory().addItem(getGun());
            });
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamageByEntity(final EntityDamageByEntityEvent e) {
        if ((e.getEntity() instanceof Player) == false) return;
        final Player p = (Player) e.getEntity();
        if (players.contains(p) == false) return;
        e.setCancelled(true);
        if (canjoin) return;
        Player d = null;
        Long spawnProtection = this.spawnProtection.get(p);
        if (spawnProtection != null && spawnProtection > System.currentTimeMillis()) return;
        if (e.getDamager().getType() == EntityType.SNOWBALL) {
            final Projectile projectile = (Projectile) e.getDamager();
            if (projectile.getShooter() instanceof Player) {
                d = (Player) projectile.getShooter();
            }
        }
        if (d == null || d == p || players.contains(d) == false) {
            return;
        }
        spawnProtection = this.spawnProtection.get(d);
        if (spawnProtection != null && spawnProtection > System.currentTimeMillis()) return;
        ms.showItemBar(d, "§fDu hast §d" + p.getName() + " §fabgeschossen. §a(+1)");
        ms.showItemBar(p, "§cDu wurdest von §d" + d.getName() + " §cabgeschossen.");
        d.playSound(d.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
        final int pointsAsInt = points.get(d) + 1;
        points.put(d, pointsAsInt);
        this.spawnProtection.put(p, System.currentTimeMillis() + 3000);
        ms.forcePlayer(p, spawns.get(r.nextInt(spawns.size())));
        players.forEach(all -> ms.sendScoreboard(all, sb));
        if (pointsAsInt == pointsToWin) {
            win(d);
        }
    }
    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        final Player p = e.getPlayer();
        if (players.contains(p) == false) return;
        if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && e.getItem() != null && e.getItem().getType() == Material.DIAMOND_BARDING) {
            p.launchProjectile(Snowball.class);
            p.playSound(p.getLocation(), Sound.SHOOT_ARROW, 1F, 1F);
        }
    }
}