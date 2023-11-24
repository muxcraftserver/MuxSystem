package me.muxteam.events;

import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.material.Wool;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MuxGunWar extends Event implements Listener {
    private Location warspawnblue, warspawnred, warflagblue, warflagred;
    private MuxEvents.Team winner;
    private int bluepoints, redpoints;
    private Player blueflagplayer, redflagplayer;
    private int maxscore = 300;
    private final ConcurrentHashMap<String, MuxEvents.Team> teams = new ConcurrentHashMap<>();
    private final Map<Player, Long> nodamage = new HashMap<>();
    private final MuxScoreboard.ScoreboardType sb;

    public MuxGunWar(final MuxEvents e) {
        super(e);
        name = "MuxGunWar";
        item = new ItemStack(Material.GOLD_BARDING);
        maxPlayers = 250;
        maxDuration = 1500000;
        breakBlocks = true;
        sb = ms.getScoreboard().createScoreboard("§7§lMux§d§lGunWar");
        sb.setLook((p, board) -> {
            sb.setLine(board, "");
            final MuxEvents.Team t = teams.get(p.getName());
            final boolean blue = t == MuxEvents.Team.BLUE;
            sb.setSection(board, (blue ? "§9" : "§c") + "§lDein Team", "§f" + (blue ? bluepoints : redpoints) + " Punkte", false);
            sb.setLine(board, " ");
            sb.setSection(board, (blue ? "§c§lTeam Rot" : "§9§lTeam Blau"), "§f" + (blue ? redpoints : bluepoints) + " Punkte ", false);
        });
    }

    private void checkScore() {
        players.forEach(p -> ms.sendScoreboard(p, sb));
        if (redpoints >= maxscore) {
            ms.broadcastMessage("§d§lMuxGunWar>§7 Team §cROT §7hat §agewonnen§7!", null, MuxSystem.Priority.NORMAL);
            winner = MuxEvents.Team.RED;
            win();
        } else if (bluepoints >= maxscore) {
            ms.broadcastMessage("§d§lMuxGunWar>§7 Team §9BLAU §7hat §agewonnen§7!", null, MuxSystem.Priority.NORMAL);
            winner = MuxEvents.Team.BLUE;
            win();
        }
    }
    private void lostFlag(final Player p) {
        if (blueflagplayer == p) {
            blueflagplayer = null;
            setFlag(warflagblue, DyeColor.BLUE);
            sendEventMessage("§d§lMuxGunWar>§7 Die §9BLAUE FLAGGE §7wurde zurückerobert.");
        } else if (redflagplayer == p) {
            redflagplayer = null;
            setFlag(warflagred, DyeColor.RED);
            sendEventMessage("§d§lMuxGunWar>§7 Die §cROTE FLAGGE §7wurde zurückerobert.");
        }
    }
    private void setFlag(final Location flagloc, final DyeColor color) {
        final Block flag = flagloc.getBlock();
        flag.setType(Material.WOOL);
        final BlockState bs = flag.getState();
        bs.setData(new Wool(color));
        bs.update();
    }
    private void win() {
        for (final Map.Entry<String, MuxEvents.Team> entry : teams.entrySet()) {
            final Player p = ms.getServer().getPlayer(entry.getKey());
            final MuxEvents.Team t = entry.getValue();
            resetPlayer(p);
            if (t == winner) {
                ms.sendTitle(p, "§d§lMuxGunWar", 10, 120, 10);
                ms.sendSubTitle(p, "§fDein Team hat §agewonnen§f!", 10, 60, 10);
                p.playSound(p.getLocation(), Sound.FIREWORK_LARGE_BLAST2, 1F, 1F);
                p.playSound(p.getLocation(), Sound.FIREWORK_TWINKLE, 1F, 1F);
                if (startedByPlayer == false) {
                    final long coins = giveReward(p, 8);
                    ms.runLater(new BukkitRunnable() {
                        @Override
                        public void run() {
                            ms.sendSubTitle(p, "§fDeine Belohnung: §e§l" + ms.getNumberFormat(coins) + " MuxCoin" + (coins > 1 ? "s" : ""), 0, 60, 10);
                        }
                    }, 60L);
                }
            } else if (winner != null) {
                ms.sendTitle(p, "§d§lMuxGunWar", 10, 120, 10);
                ms.sendSubTitle(p, "§fDein Team hat §cverloren§f.", 10, 60, 10);
                if (startedByPlayer == false) {
                    final long coins = giveReward(p, 1);
                    ms.runLater(new BukkitRunnable() {
                        @Override
                        public void run() {
                            ms.sendSubTitle(p, "§fDein Trostpreis: §e§l" + ms.getNumberFormat(coins) + " MuxCoin" + (coins > 1 ? "s" : ""), 0, 60, 10);
                        }
                    }, 60L);
                }
            }
        }
        stopEvent(false);
    }

    @Override
    public void stop() {
        blueflagplayer = redflagplayer = null;
        setFlag(warflagblue, DyeColor.BLUE);
        setFlag(warflagred, DyeColor.RED);
        teams.clear();
        nodamage.clear();
        bluepoints = redpoints = 0;
        winner = null;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Besiege die Spieler vom anderen Team",
                "§7und klaue ihre Flagge. Das Team, was",
                "§7als Erstes §d" + maxscore + " Punkte §7hat, gewinnt.",
                "",
                "§7Teilnehmer: §d" + players.size(),
                "§7Punkte: §9" + bluepoints + " §7/ §c" + redpoints
        };
    }

    @Override
    public void start() {
        final boolean open = (warspawnblue = ms.getLoc("pbspawnblue")) != null && (warspawnred = ms.getLoc("pbspawnred")) != null
                && (warflagblue = ms.getLoc("pbflagblue")) != null && (warflagred = ms.getLoc("pbflagred")) != null;
        if (open == false) {
            stopEvent(true);
            return;
        }
        final int online = ms.getServer().getOnlinePlayers().size();
        if (online > 200) {
            maxscore = 500;
        } else if (online > 100) {
            maxscore = 200;
        } else {
            maxscore = 100;
        }
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das " + getName() + " Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        spawnEventNPC("§d§lEvent beitreten");
    }

    @Override
    public boolean join(final Player p) {
        equipPlayer(p);
        if (players.contains(p) == false) {
            int red = 0, blue = 0;
            for (final MuxEvents.Team t : teams.values()) {
                if (t == MuxEvents.Team.BLUE) blue++;
                else red++;
            }
            final MuxEvents.Team t = blue <= red ? MuxEvents.Team.BLUE : MuxEvents.Team.RED;
            teams.put(p.getName(), t);
        }
        final MuxEvents.Team t = teams.get(p.getName());
        ms.forcePlayer(p, t == MuxEvents.Team.BLUE ? warspawnblue : warspawnred);
        final PlayerInventory pi = p.getInventory();
        pi.setHelmet(new ItemStack(Material.WOOL, 1, t == MuxEvents.Team.BLUE ? (byte) 11 : (byte) 14));
        pi.setItem(0, ms.renameItemStack(new ItemStack(Material.IRON_BARDING, 1), "§f§lMux§b§lGun"));
        p.updateInventory();
        p.getInventory().setHeldItemSlot(0);
        ms.sendScoreboard(p, sb);
        nodamage.put(p, System.currentTimeMillis() + 3000L);
        return true;
    }

    @Override
    public void quit(final Player p) {
        resetPlayer(p);
        teams.remove(p.getName());
        lostFlag(p);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(final EntityDamageEvent e) {
        if (e.getEntity() instanceof Player == false || players.contains((Player) e.getEntity()) == false) {
            return;
        }
        final Player p = (Player) e.getEntity();
        if (nodamage.getOrDefault(p, 0L) > System.currentTimeMillis()) {
            e.setCancelled(true);
            return;
        }
        Player damager;
        final MuxEvents.Team t = teams.get(p.getName());
        if (e instanceof EntityDamageByEntityEvent) {
            final EntityDamageByEntityEvent damageByEntity = (EntityDamageByEntityEvent) e;
            if (damageByEntity.getDamager() instanceof Projectile) {
                final Projectile proj = (Projectile) damageByEntity.getDamager();
                if (proj.getShooter() instanceof Player) {
                    damager = (Player) proj.getShooter();
                    e.setCancelled(true);
                    if (damager != null && (players.contains(damager) == false || teams.get(damager.getName()) == t)) {
                        return;
                    }
                    if (damager != null) {
                        ms.showItemBar(damager, "§fDu hast " + (t == MuxEvents.Team.BLUE ? "§9" : "§c") + e.getEntity().getName() + " §fbesiegt. " + (t == MuxEvents.Team.BLUE ? "§c" : "§9") + ("(+1)"));
                        damager.playSound(damager.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
                    }
                    join(p);
                    if (t == MuxEvents.Team.BLUE) redpoints++;
                    else if (t == MuxEvents.Team.RED) bluepoints++;
                    lostFlag(p);
                    checkScore();
                }
            } else {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        if (players.contains(e.getPlayer()) == false) {
            return;
        }
        final Player p = e.getPlayer();
        if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) && e.getItem() != null && e.getItem().getType() == Material.IRON_BARDING) {
            p.launchProjectile(Snowball.class);
            p.playSound(p.getLocation(), Sound.SHOOT_ARROW, 1F, 1F);
        }
        if (e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            final Block b = e.getClickedBlock();
            if (p == blueflagplayer && b.getLocation().equals(warflagred)) {
                blueflagplayer = null;
                setFlag(warflagblue, DyeColor.BLUE);
                ms.spawnFirework(warflagred, Color.RED);
                warflagred.getWorld().playSound(warflagred, Sound.FIREWORK_LARGE_BLAST, 1F, 1F);
                sendEventMessage("§d§lMuxGunWar>§7 Team §cROT §7hat die §9BLAUE FLAGGE §7erobert! §c(+10)");
                redpoints += 10;
            } else if (p == redflagplayer && b.getLocation().equals(warflagblue)) {
                redflagplayer = null;
                setFlag(warflagred, DyeColor.RED);
                ms.spawnFirework(warflagred, Color.BLUE);
                warflagblue.getWorld().playSound(warflagblue, Sound.FIREWORK_LARGE_BLAST, 1F, 1F);
                sendEventMessage("§d§lMuxGunWar>§7 Team §9BLAU §7hat die §cROTE FLAGGE §7erobert! §9(+10)");
                bluepoints += 10;
            } else {
                return;
            }
            join(p);
            p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 1F);
            checkScore();
        }
    }

    @EventHandler
    public void onDeath(final PlayerDeathEvent e) {
        if (players.contains(e.getEntity()) == false) {
            return;
        }
        if (e.getEntity().getKiller() != null) {
            final Player pl = e.getEntity().getKiller();
            final MuxEvents.Team t = teams.get(e.getEntity().getName());
            ms.showItemBar(pl, "§fDu hast " + (t == MuxEvents.Team.BLUE ? "§9" : "§c") + e.getEntity().getName() + " §fbesiegt. " + (t == MuxEvents.Team.BLUE ? "§c" : "§9") + ("(+1)"));
            pl.playSound(pl.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                join(e.getEntity());
            }
        }.runTaskLater(ms, 5L);
        e.getDrops().clear();
    }

    @EventHandler
    public void onBreak(final BlockBreakEvent e) {
        if (players.contains(e.getPlayer()) == false) {
            return;
        }
        e.setCancelled(true);
        final Block b = e.getBlock();
        final Player p = e.getPlayer();
        if (b.getLocation().equals(warflagblue)) {
            if (teams.get(p.getName()) != MuxEvents.Team.RED) {
                ms.showItemBar(p, "§cDu kannst nicht die Flagge deines Teams klauen.");
                return;
            }
            b.setType(Material.AIR);
            p.getInventory().clear();
            p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.WOOL, 1, (byte) 11), "§9§lBlaue Flagge", "§7Dies ist die Flagge des blauen Teams."));
            blueflagplayer = p;
            warflagblue.getWorld().playSound(warflagblue, Sound.ZOMBIE_INFECT, 1F, 1F);
            ms.playEffect(EnumParticle.VILLAGER_HAPPY, warflagblue, 1, 1, 1, 0, 5);
            sendEventMessage("§d§lMuxGunWar>§7 Der Spieler §c" + p.getName() + " §7hat die §9BLAUE FLAGGE §7geklaut!");
            ms.sendTitle(p, "§f", 10, 80, 10);
            ms.sendSubTitle(p, "§fLauf zur Flagge §cdeines Teams§f,", 10, 40, 10);
            ms.runLater(new BukkitRunnable() {
                @Override
                public void run() {
                    ms.sendSubTitle(p, "§fund schlag diese an!", 0, 40, 10);
                }
            }, 40L);
        } else if (b.getLocation().equals(warflagred)) {
            if (teams.get(p.getName()) != MuxEvents.Team.BLUE) {
                ms.showItemBar(p, "§cDu kannst nicht die Flagge deines Teams klauen.");
                return;
            }
            b.setType(Material.AIR);
            p.getInventory().clear();
            p.getInventory().addItem(ms.renameItemStack(new ItemStack(Material.WOOL, 1, (byte) 14), "§c§lRote Flagge", "§7Dies ist die Flagge des roten Teams."));
            redflagplayer = p;
            ms.playEffect(EnumParticle.VILLAGER_HAPPY, warflagred, 1, 1, 1, 0, 5);
            warflagred.getWorld().playSound(warflagred, Sound.ZOMBIE_INFECT, 1F, 1F);
            sendEventMessage("§d§lMuxGunWar>§7 Der Spieler §9" + p.getName() + " §7hat die §cROTE FLAGGE §7geklaut!");
            ms.sendTitle(p, "§f", 10, 80, 10);
            ms.sendSubTitle(p, "§fLauf zur Flagge §9deines Teams§f,", 10, 40, 10);
            ms.runLater(new BukkitRunnable() {
                @Override
                public void run() {
                    ms.sendSubTitle(p, "§fund schlag diese an!", 0, 40, 10);
                }
            }, 40L);
        }
    }
}