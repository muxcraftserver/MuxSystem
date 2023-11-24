package me.muxteam.pvp;

import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class MuxPvPStats {
    private MuxSystem ms;
    private final List<String> deathmsg = new ArrayList<>(), killmsg = new ArrayList<>();
    private final Random r = new Random();

    public MuxPvPStats(final MuxSystem ms) {
        this.ms = ms;
        setup();
    }

    public void close() {
        this.ms = null;
    }

    private void setup() {
        Collections.addAll(killmsg,
                "§bDu hast §6%ply §bgetötet!",
                "§bDu hast §6%ply §bkaputtgemacht!",
                "§bDu hast §6%ply §bzerfetzt!",
                "§bDu hast §6%ply §bgemobbt!",
                "§bDu hast §6%ply §bdas Leben genommen.",
                "§bDu hast §6%ply §bvoll gepwnd. Leider hast du ihn dabei umgebracht.",
                "§bDu hast §6%ply §bgekillt und seine Familie wird nun verhungern müssen.",
                "§bDu hast §6%ply §bgetötet. Ein weiteres Gebot gebrochen.",
                "§bDu hast §6%ply §bgetötet. In dem Himmel kommst du wohl nicht mehr.",
                "§bDu hast §6%ply §bunter die Erde gebracht. Nun schaut er sich die Kartoffeln von unten an.",
                "§bDu warst auf §6%ply §bsauer!",
                "§bDu bist für den Tod von §6%ply §bverantwortlich.",
                "§6%ply §bwar dir im Weg!",
                "§6%ply §bist dank dir von uns gegangen. He'll be back, also nimm dich in Acht.",
                "§6%ply §bbedankt sich für den Death!",
                "§6%ply §bsollte lieber SinglePlayer spielen!"); // SIZE: 16
        Collections.addAll(deathmsg,
                "§6%ply §bhat dich kaputtgemacht!",
                "§6%ply §bhat dich plattgemacht!",
                "§6%ply §bhat dich geowned!",
                "§bDu wurdest von §6%ply §bgetötet!",
                "§bDu wurdest von §6%ply §bgeschlachtet!",
                "§6%ply §bhat Fleischwurst aus dir gemacht.",
                "§6%ply §bwar sauer auf dich!",
                "§6%ply §bhat dich zerfetzt!",
                "§6%ply §bwünscht dir viel Spass im Krankenhaus!",
                "§6%ply §bist das Schwert ausgerutscht!",
                "§6%ply §bbedankt sich für den Kill!",
                "§6%ply §bkam, sah und siegte!",
                "§bDu warst §6%ply §bim Weg!",
                "§6%ply: §bDu solltest lieber SinglePlayer spielen!"); // SIZE: 14
    }

    public void updateTop() {
        new BukkitRunnable() {
            @Override public void run() {
                final List<Object[]> ranking = ms.getDB().getTopTrophies(3);
                if (ranking != null && ranking.isEmpty() == false) {
                    topplayer = (UUID) ranking.get(0)[0];
                }
                if (ms.getBase().getTop10Ranking().isEmpty() == false)
                    topbaseplayer = ms.getBase().getTop10Ranking().get(0).getOwner();
            }
        }.runTaskAsynchronously(ms);
    }

    public int getKillTrophies(final Player killed, final Player killer) {
        final String kname = killed.getName(), krname = killer.getName();
        final MuxUser u = ms.getMuxUser(kname), u2 = ms.getMuxUser(krname);
        if (u == null || u2 == null) return 0;
        int trophies = u.getTrophies(), trophies2 = u2.getTrophies(), plus = (int) Math.max(trophies / getStatsRange(trophies2), 1);
        if (trophies <= 0 && trophies2 > 1) {
            plus = 0;
        } else if (plus > 60) {
            plus = 60;
        }
        int krbonus = getTrophiesBonus(trophies2, plus);
        final boolean sameip = killer.getAddress().getAddress().getHostAddress().equals(killed.getAddress().getAddress().getHostAddress());
        if (sameip) {
            plus = 0;
            krbonus = 0;
        }
        return plus + krbonus;
    }

    public void handleKill(final Player killed, final Player killer, final boolean scoreboard, boolean ranked) {
        final String kname = killed.getName(), krname = killer.getName();
        if (kname.equals(krname)) {
            ms.showItemBar(killed, "§cDu hast dich selber umgebracht!");
            return;
        }
        final MuxUser u = ms.getMuxUser(kname), u2 = ms.getMuxUser(krname);
        if (u == null || u2 == null) return;
        int trophies = u.getTrophies(), trophies2 = u2.getTrophies(), plus = (int) Math.max(trophies / getStatsRange(trophies2), 1);
        if (trophies <= 0 && trophies2 > 1) {
            trophies = plus = 0;
        } else if (plus > 60) {
            plus = 60;
        }
        int krbonus = getTrophiesBonus(trophies2, plus);
        u.addDeath();
        final boolean sameip = killer.getAddress().getAddress().getHostAddress().equals(killed.getAddress().getAddress().getHostAddress());
        if (sameip == false) {
            u2.addKill();
            if (ranked) {
                u.setTrophies(trophies -= plus);
                u2.setTrophies(trophies2 += plus + krbonus);
            }
            if (scoreboard) {
                ms.sendScoreboard(killed);
                ms.sendScoreboard(killer);
            }
            final int lost = -plus, won = plus + krbonus;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (ranked) {
                        final String clankilled = ms.getDB().getClanFromPlayer(killed.getUniqueId()),
                                clankiller = ms.getDB().getClanFromPlayer(killer.getUniqueId());
                        if (clankilled != null) ms.getDB().updateTrophyCache(clankilled);
                        if (clankiller != null) ms.getDB().updateTrophyCache(clankiller);
                        //ms.getDB().updateTrophyCache(killed.getUniqueId(), lost);
                        // ms.getDB().updateTrophyCache(killer.getUniqueId(), won);
                    }
                    final Environment env = killed.getWorld().getEnvironment();
                    final String type = ms.in1vs1(killed) ? "1vs1" : ms.inDuel(killed) ? "Duell" : ms.inWar(killed) ? "Clanwar" :
                            env == Environment.THE_END ? "End" : env == Environment.NETHER ? "Nether" : "PvP";
                    ms.getDB().addPvPHistory(u.getUUID(), u2.getUUID(), ranked ? lost : 0, type);
                    ms.getDB().addPvPHistory(u2.getUUID(), u.getUUID(), ranked ? won : 0, type);
                }
            }.runTaskAsynchronously(ms);
        } else {
            plus = 0;
            krbonus = 0;
        }
        killed.sendMessage(deathmsg.get(r.nextInt(14)).replace("%ply", krname) + (ranked ? " §c" + (plus == 0 ? "§7" : "") + "(-" + plus + ")" : ""));
        killer.sendMessage(killmsg.get(r.nextInt(16)).replace("%ply", kname) + (ranked ? " §a" + (plus == 0 ? "§7" : "") + "(+" + (plus + krbonus) + ")" : ""));
        if (ranked) {
            checkLeague(killer, trophies2, plus + krbonus);
            checkLeague(killed, trophies, -plus);
        }
    }

    public String getLeague(final int trophies, boolean active, boolean pl) {
        if (trophies < 350) return "";
        else if (pl && active == false) return "Unranked";
        else if (trophies < 750) return "Bronze";
        else if (trophies < 1000) return "Silber";
        else if (trophies < 1500) return "Gold";
        else return "Master";
    }

    public String getColoredBoldLeague(final int trophies) {
        if (trophies < 350) return "§7§lNoch keine Liga";
        else if (trophies < 750) return "§6§lBronze Liga";
        else if (trophies < 1000) return "§f§lSilber Liga";
        else if (trophies < 1500) return "§e§lGold Liga";
        else return "§b§lMaster Liga";
    }

    private int getTrophiesBonus(final int trophies, final int plus) {
        return plus < 2 || trophies > 1000 ? 0 : (int) (plus * 0.3);
    }

    private void checkLeague(final Player p, final int now, final int plus) {
        final int before = now - plus;
        boolean up = false;
        final String liga;
        int uptrophies = 0;
        if (before < 350 && now >= 350) {
            liga = "§6§lBronze";
            up = true;
            uptrophies = 350;
        } else if (now < 350 && before >= 350) {
            liga = "";
        } else if (before < 750 && now >= 750) {
            liga = "§f§lSilber";
            up = true;
            uptrophies = 750;
        } else if (now < 750 && before >= 750) {
            liga = "§6§lBronze";
        } else if (before < 1000 && now >= 1000) {
            liga = "§e§lGold";
            up = true;
            uptrophies = 1000;
        } else if (now < 1000 && before >= 1000) {
            liga = "§f§lSilber";
        } else if (before < 1500 && now >= 1500) {
            liga = "§b§lMaster";
            up = true;
            uptrophies = 1500;
        } else if (now < 1500 && before >= 1500) {
            liga = "§e§lGold";
        } else {
            return;
        }
        if (up) {
            p.sendMessage(new String[]{
                    ms.header(null, (byte) 13, "§4"),
                    "  §4§lLiga Aufstieg",
                    "",
                    "  §7Herzlichen Glückwunsch, §a" + p.getName() + "§7!",
                    "  §7Da du jetzt §bmehr als " + uptrophies + " Trophäen §7hast, bist",
                    "  §7du nun ab sofort in der §6§l" + liga + " Liga§7!",
                    ms.footer((byte) 13, (byte) 0, "§4")
            });
            p.playSound(p.getLocation(), Sound.ENDERDRAGON_DEATH, 1F, 1F);
        } else if (now < 350) {
            p.sendMessage("§4§lMuxLiga>§e Du bist nicht mehr eingestuft. Kämpfe, um wieder nach oben zu kommen.");
        } else {
            p.sendMessage("§4§lMuxLiga>§7 Du bist wieder in der §6§l" + liga + " Liga§7. Kämpfe, um wieder nach oben zu kommen.");
        }
    }

    private double getStatsRange(final int trophies) {
        if (trophies < 50) return 2D;
        else if (trophies < 250) return 5D;
        else if (trophies < 500) return 20D;
        else if (trophies < 1000) return 40D;
        else if (trophies < 2000) return 100D;
        else if (trophies < 3000) return 200D;
        else if (trophies < 4000) return 400D;
        else if (trophies < 4500) return 900D;
        else return 2500D;
    }

    // TOP PLAYER EFFECT
    private UUID topplayer;
    private Location laststep = null;

    private UUID topbaseplayer;
    private Location lastbasesep;

    private final boolean t = true, o = false;
    public final boolean[][] shape = {{o, o, o, o, o, o, o, o, o, o, o, o, o, o, o, o, o, o}, {o, t, t, t, t, o, o, o, o, o, o, o, t, t, t, t, o, o}, {o, o, t, t, t, t, t, o, o, o, t, t, t, t, t, o, o, o}, {o, o, o, t, t, t, t, t, t, t, t, t, t, t, o, o, o, o}, {o, o, o, o, t, t, t, t, t, t, t, t, t, o, o, o, o, o}, {o, o, o, o, t, t, t, t, o, t, t, t, t, o, o, o, o, o}, {o, o, o, o, o, t, t, t, o, t, t, t, o, o, o, o, o, o}, {o, o, o, o, o, t, t, o, o, o, t, t, o, o, o, o, o, o}, {o, o, o, o, t, t, o, o, o, o, o, t, t, o, o, o, o, o}};

    public void sendParticles() {
        if (topbaseplayer != null) {
            final Player p = ms.getServer().getPlayer(topbaseplayer);
            if (p != null) {
                if (ms.inEvent(p) == false && ms.isVanish(p) == false) {
                    final Location l = lastbasesep, ploc = p.getLocation();
                    if (l != null && l.getWorld().equals(ploc.getWorld()) && l.distance(ploc) <= 0.2 && p.isOnGround() && ms.inBattle(p.getName(), p.getLocation()) == false) {
                        drawParticles(ploc, EnumParticle.VILLAGER_HAPPY, 0.001F, 0.001F, 0.001F, 0.00001F, 1);
                    }
                    lastbasesep = ploc;
                }
            }
        }
        if (topplayer != null) {
            final Player p = ms.getServer().getPlayer(topplayer);
            if (p != null) {
                if (ms.inEvent(p) == false && ms.isVanish(p) == false) {
                    final Location l = laststep, ploc = p.getLocation();
                    if (l != null && l.getWorld().equals(ploc.getWorld()) && l.distance(ploc) <= 0.2 && p.isOnGround() && ms.inBattle(p.getName(), p.getLocation()) == false) {
                        drawParticles(ploc, EnumParticle.FLAME, 0.001F, 0.001F, 0.001F, 0.00001F, 1);
                    }
                    laststep = ploc;
                }
            }
        }
    }

    private void drawParticles(final Location l, final EnumParticle particle, final float xOff, final float yOff, final float zOff, final float speed, final int amount) {
        final double space = 0.2D, defX = l.getX() - space * shape[0].length / 2.0D + space;
        double x = defX, y = l.clone().getY() + 2.0D, angle = -((l.getYaw() + 180.0F) / 60.0F);
        angle += (l.getYaw() < -180.0F ? 3.25D : 2.985D);

        for (final boolean[] booleans : shape) {
            for (final boolean b : booleans) {
                if (b) {
                    final Location target = l.clone();
                    target.setX(x);
                    target.setY(y);

                    Vector v = target.toVector().subtract(l.toVector());
                    final Vector v2 = getBackVector(l);
                    v = rotateAroundAxisY(v, angle);
                    v2.setY(0).multiply(-0.2D);

                    l.add(v);
                    l.add(v2);
                    for (int k = 0; k < 3; k++) ms.playEffect(particle, l, xOff, yOff, zOff, speed, amount);
                    l.subtract(v2);
                    l.subtract(v);
                }
                x += space;
            }
            y -= space;
            x = defX;
        }
    }
    private org.bukkit.util.Vector rotateAroundAxisY(final org.bukkit.util.Vector v, final double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle), x = v.getX() * cos + v.getZ() * sin, z = v.getX() * -sin + v.getZ() * cos;
        return v.setX(x).setZ(z);
    }
    private org.bukkit.util.Vector getBackVector(final Location loc) {
        float newZ = (float) (loc.getZ() + Math.sin(Math.toRadians(loc.getYaw() + 90.0F))),
                newX = (float) (loc.getX() + Math.cos(Math.toRadians(loc.getYaw() + 90.0F)));
        return new Vector(newX - loc.getX(), 0.0D, newZ - loc.getZ());
    }
}