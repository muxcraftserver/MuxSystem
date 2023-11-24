package me.muxteam.pvp;

import me.muxteam.basic.MuxAnvil;
import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MuxClanWar {
    private MuxSystem ms;
    private final Map<UUID, Request> requests = new HashMap<>();
    private final CopyOnWriteArrayList<Arena> arenas = new CopyOnWriteArrayList<>();
    private final Map<MaterialData, String> maps = new HashMap<>();
    private final MuxScoreboard.ScoreboardType cwsb;

    public MuxClanWar(final MuxSystem ms) {
        this.ms = ms;
        maps.put(new MaterialData(Material.SAND, (byte) 0), "Wüste");
        maps.put(new MaterialData(Material.SNOW_BLOCK, (byte) 0), "Schnee");
        cwsb = ms.getScoreboard().createScoreboard("§7§lMux§3§lClanwar");
        cwsb.setLook((p, board) -> {
            cwsb.setLine(board, " ");
            int lowestpcnt = 100;
            short armorcount = 0;
            for (final ItemStack armor : p.getInventory().getArmorContents()) {
                if (armor != null && armor.getType() != Material.AIR && ms.isArmor(armor.getType())) {
                    armorcount++;
                    int durability = 100 - (int) (((double) armor.getDurability() / (double) armor.getType().getMaxDurability()) * 100D);
                    if (durability < lowestpcnt) lowestpcnt = durability;
                }
            }
            cwsb.setSection(board, "§b§lRüstung     ", (armorcount == 0 ? "Keine" : armorcount < 4 ? "Unvollständig" : "Zustand: " + (lowestpcnt < 15 ? "§c" : "") + lowestpcnt + "%"), false);
            cwsb.setLine(board, "  ");
            final MuxClanWar.Arena a = getArena(p);
            if (a != null) {
                cwsb.setSection(board, "§e§lPreis", a.getEntryCost() == 0 ? "Keinen" : ms.getNumberFormat(a.getEntryCost() * 2L) + " MuxCoins", false);
                cwsb.setLine(board, "   ");
                final String opponent = a.livingA.contains(p) ? a.getClanNameB() : a.getClanNameA();
                final int oppcount = a.livingA.contains(p) ? a.countLivingB() : a.countLivingA();
                final Player pl = ms.getLastHit(p);
                String league = "";
                if (pl != null) {
                    final MuxUser u2 = ms.getMuxUser(pl.getName());
                    final String l = ms.getStats().getLeague(u2.getTrophies(), u2.getLastRankedMatch() > System.currentTimeMillis() - 172800000, u2.getLastRankedMatch() != 0);
                    if (l.isEmpty() == false) league = " (" + l + ")";
                }
                cwsb.setSection(board, "§4§lGegner", pl == null ? "Keinen" : pl.getName() + (league.isEmpty() ? " " : league), false);
                cwsb.setLine(board, "    ");
                cwsb.setSection(board, "§c§l" + opponent, oppcount + " Spieler übrig", true);
            }
        });
    }

    public void close() {
        stopClanWars();
        this.ms = null;
    }

    public void stopClanWars() {
        for (final Arena a : arenas) {
            a.disableArena();
        }
    }

    public boolean isEnabled() {
        return ms.getAdmin().CLANWAR.isActive();
    }

    public List<Arena> getArenas() {
        return arenas;
    }

    public MuxClanWar.Arena getArena(final Player p) {
        for (final MuxClanWar.Arena a : arenas) {
            if (a.containsLivingA(p) || a.containsLivingB(p)) {
                return a;
            }
        }
        return null;
    }

    public void sendScoreboard(final Player p) {
        ms.sendScoreboard(p, cwsb);
    }

    public boolean handleCommand(final Player p, final String[] args) {
        final UUID uuid = p.getUniqueId();
        final String clan = ms.getDB().getClanFromPlayer(uuid);
        if (isEnabled() == false && p.isOp() == false) {
            ms.showItemBar(p, "§cClanWar ist derzeit nicht verfügbar.");
            return true;
        } else if (ms.getAdmin().pvpIsActive() == false && p.isOp() == false) {
            ms.showItemBar(p, "§cPvP ist momentan deaktiviert.");
            return true;
        } else if (inWar(p) && p.isOp() == false) {
            ms.showItemBar(p, "§cDu bist bereits im ClanWar.");
            return true;
        } else if (p.hasPermission("muxsystem.vip") == false && ms.isVPN(p.getAddress().getAddress().getHostAddress()) == 1) {
            ms.sendNoVPNMessage(p, "VIP");
            return true;
        } else if (args.length == 0) {
            if (clan == null) {
                ms.showItemBar(p, "§cErstelle zuerst einen Clan.");
                return true;
            } else if (ms.getDB().getRankMembers(clan, 2).get(0).equals(uuid) == false) {
                ms.showItemBar(p, "§cDu bist nicht berechtigt, einen Clanwar zu starten.");
                return true;
            }
            Request an = requests.get(uuid);
            if (an == null) {
                an = new Request();
                requests.put(uuid, an);
            }
            if (ms.checkGeneralCooldown(p.getName(), "CLANWAR", 60000L, false)) {
                ms.showItemBar(p, "§cWarte etwas um eine weitere Anfrage zu schicken.");
                return true;
            }
            openInv(p, 0);
            return true;
        } else if (args[0].equals("spectate")) {
            if (args.length == 1) return true;
            final String map = args[1].toLowerCase();
            for (final Arena a : arenas) {
                if (a.getMap().toLowerCase().equals(map)) {
                    a.spectate(p);
                    return true;
                }
            }
            ms.showItemBar(p, "§cDieses ClanWar läuft nicht mehr.");
            return true;
        } else if (clan == null) {
            ms.showItemBar(p, "§cDu bist in keinem Clan.");
            return true;
        } else if (args[0].equals("join") && args.length == 2) {
            final Player leader = ms.getServer().getPlayer(args[1]);
            if (leader == null) return true;
            if (ms.getDB().inOneClan(leader.getUniqueId(), p.getUniqueId()) == false) {
                ms.showItemBar(p, "§cDu bist nicht in dem gleichen Clan.");
                return true;
            }
            for (final Arena a : arenas) {
                final boolean inA = a.containsLivingA(leader), inB = a.containsLivingB(leader);
                if (inA == false && inB == false) {
                    ms.showItemBar(p, "§cDein Clan Anführer ist derzeit in keiner Arena.");
                    continue;
                }
                if (a.canJoin(p, inA) == false) continue;
                if (ms.getMuxUser(p.getName()).getCoins() < a.getEntryCost()) {
                    ms.showItemBar(p, "§cDu besitzt nicht genügend MuxCoins.");
                    return true;
                }
                if (a.hasOPs() == false) {
                    p.removePotionEffect(PotionEffectType.ABSORPTION);
                    p.removePotionEffect(PotionEffectType.REGENERATION);
                    p.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                }
                p.setGameMode(GameMode.SURVIVAL);
                ms.removeGodMode(p.getName());
                if (ms.isVanish(p)) {
                    p.performCommand("vanish");
                }
                ms.getMenu().disableForcefield(p);
                p.setAllowFlight(false);
                p.setFlying(false);
                ms.forcePlayer(p, inA ? a.getLocA() : a.getLocB());
                if (inA) a.addToClanA(p);
                else a.addToClanB(p);
                ms.showItemBar(p, "§fDu bist dem ClanWar §abeigetreten§f.");
                a.sendPlayerMessage("§3§lClanWar>§7 " + p.getName() + " ist der Arena §abeigetreten§7. §7[" + (a.countLivingA() + a.countLivingB()) + "/" + a.getSlots() + "]");
                a.checkIfCanStart();
            }
            return true;
        } else if (args[0].equals("accept")) {
            if (args.length == 1) return true;
            final Player pl = ms.getServer().getPlayer(args[1]);
            if (pl == null) {
                ms.showItemBar(p, "§cDer Clan Anführer ist offline gegangen.");
                return true;
            }
            reserve(pl, p);
            return true;
        } else if (p.isOp() == false) {
            return true;
        } else if (ms.isTrusted(p.getName()) == false) {
            p.sendMessage(new String[]{
                    "§cClanWar Arenen: §7" + (maps.size() == 0 ? "Noch keine" : String.join(", ", maps.values()))
            });
            return true;
        } else if (args.length < 2) {
            p.sendMessage(new String[]{
                    "  §3ClanWar Admin Befehle:",
                    "  §c/cw setspawn [map] [1 | 2]§7: Spawn setzen",
                    "  §c/cw setgate [map] [1 | 2]§7: Tor setzen",
                    "  §c/cw setspec [map]§7: Zuschauerplatz setzen",
                    "  §c/cw fstart [map]§7: Force Start",
                    "  §cArenen: §7" + (maps.size() == 0 ? "Noch keine" : String.join(", ", maps.values()))
            });
        } else if (args[0].equals("fstart")) {
            for (final Arena a : arenas) {
                if (a.getMap().equalsIgnoreCase(args[1])) {
                    if (a.notStarted()) {
                        a.startGame();
                        ms.showItemBar(p, "§fDie Arena wurde erfolgreich §agestartet§f.");
                    } else {
                        ms.showItemBar(p, "§cDie Arena wurde bereits gestartet.");
                    }
                    return true;
                }
            }
            ms.showItemBar(p, "§cDie Arena wurde nicht gefunden.");
        } else if (args[0].equals("setspec")) {
            ms.setLoc("clanwar" + args[1].toLowerCase() + "spectate", p.getLocation());
            ms.saveLocs(p);
            ms.showItemBar(p, "§fDer Zuschauerplatz §fin Arena §3" + args[1] + " §fwurde §agesetzt§f.");
        } else if (args.length < 3) {
            return true;
        } else if (args[0].equals("setspawn")) {
            if ((args[2].equals("1") || args[2].equals("2")) == false) {
                ms.showItemBar(p, "§cGebe einen gültigen Spawnpunkt an. (1 oder 2)");
                return true;
            }
            ms.setLoc("clanwar" + args[1].toLowerCase() + "spawn" + args[2], p.getLocation());
            ms.saveLocs(p);
            ms.showItemBar(p, "§fDer Spawnpunkt §3" + args[2] + " §fin Arena §3" + args[1] + " §fwurde §agesetzt§f.");
        } else if (args[0].equals("setgate")) {
            if ((args[2].equals("1") || args[2].equals("2")) == false) {
                ms.showItemBar(p, "§cGebe einen gültigen Torpunkt an. (1 oder 2)");
                return true;
            }
            ms.setLoc("clanwar" + args[1].toLowerCase() + "gate" + args[2], p.getLocation());
            ms.saveLocs(p);
            ms.showItemBar(p, "§fDas Tor §3" + args[2] + " §fin Arena §3" + args[1] + " §fwurde §agesetzt§f.");
        }
        return true;
    }

    private void reserve(final Player from, final Player to) {
        final Request r = requests.get(from.getUniqueId());
        if (r == null) {
            ms.showItemBar(to, "§cDu hast keine Anfrage.");
            return;
        } else if (inWar(to)) {
            ms.showItemBar(to, "§cDu bist bereits im ClanWar.");
            return;
        } else if (inWar(from)) {
            ms.showItemBar(to, "§cDer Herausforderer ist bereits im ClanWar.");
            return;
        } else if (ms.inGame(to)) {
            ms.showItemBar(to, "§cDu spielst gerade ein Minigame.");
            return;
        } else if (ms.inGame(from)) {
            ms.showItemBar(to, "§cDer Herausforderer spielt gerade ein Minigame.");
            return;
        } else if (ms.inEvent(to)) {
            ms.showItemBar(to, "§cDu bist gerade in einem Event.");
            return;
        } else if (ms.inEvent(from)) {
            ms.showItemBar(to, "§cDer Herausforderer ist gerade in einem Event.");
            return;
        } else if (ms.inBattle(from.getName(), from.getLocation())) {
            ms.showItemBar(to, "§cDer Herausforderer befindet sich gerade im Kampf.");
            return;
        } else if (ms.getTrades().isTrading(to.getName())) {
            ms.showItemBar(to, "§cDu bist gerade noch am handeln.");
            return;
        } else if (ms.getTrades().isTrading(from.getName())) {
            ms.showItemBar(from, "§cDer Herausforderer handelt gerade mit jemanden.");
            return;
        } else if (ms.isBeingTeleported(from.getName())) {
            ms.showItemBar(to, "§cDer Herausforderer wird gerade teleportiert.");
            return;
        } else if (ms.isBeingTeleported(to.getName())) {
            ms.showItemBar(to, "§cDu wirst gerade teleportiert.");
            return;
        } else if (from.isDead()) {
            ms.showItemBar(to, "§cDer Herausforderer ist derzeit tot.");
            return;
        }
        boolean free = false;
        String amap = "";
        for (final String map : maps.values()) {
            if (isMapReserved(map) == false) {
                free = true;
                amap = map;
            }
        }
        if (free == false) {
            from.sendMessage("§c§lDerzeit sind alle Arenen besetzt.");
            ms.showItemBar(to, "§cDerzeit sind alle Arenen besetzt.");
            return;
        }
        final String clanfrom = ms.getDB().getClanFromPlayer(from.getUniqueId()), clanto = ms.getDB().getClanFromPlayer(to.getUniqueId());
        if (clanfrom == null || ms.getDB().getRankMembers(clanfrom, 2).get(0).equals(from.getUniqueId()) == false) {
            ms.showItemBar(to, "§cDer Herausforderer hat sein Clan gelöscht.");
            return;
        } else if (clanto == null || ms.getDB().getRankMembers(clanto, 2).get(0).equals(to.getUniqueId()) == false) {
            ms.showItemBar(to, "§cDu hast deinen Clan gelöscht.");
            return;
        } else if (ms.getMuxUser(to.getName()).getCoins() < r.getEntryCost()) {
            ms.showItemBar(to, "§cDu besitzt nicht genügend MuxCoins.");
            return;
        } else if (ms.getMuxUser(from.getName()).getCoins() < r.getEntryCost()) {
            ms.showItemBar(to, "§cDer Herausforderer hat leider nicht genug MuxCoins.");
            from.sendMessage("§c§lFehler: §cDu besitzt nicht genügend MuxCoins für den ClanWar.");
            return;
        }
        final int healsfrom = ms.getMaterialCount(from.getInventory(), Material.POTION, (byte) 16389) +
                ms.getMaterialCount(from.getInventory(), Material.POTION, (byte) 16421) +
                ms.getMaterialCount(from.getInventory(), Material.POTION, (byte) 16453),
                healsto = ms.getMaterialCount(to.getInventory(), Material.POTION, (byte) 16389) +
                        ms.getMaterialCount(to.getInventory(), Material.POTION, (byte) 16421) +
                        ms.getMaterialCount(to.getInventory(), Material.POTION, (byte) 16453);
        if (healsto / 64D > (double) r.getHeals()) {
            ms.showItemBar(to, "§cEs sind höchstens " + r.getHeals() + " Stack" + (r.getHeals() > 1 ? "s" : "") + " Heiltränke erlaubt.");
            return;
        } else if (healsfrom / 64D > (double) r.getHeals()) {
            ms.showItemBar(to, "§cDer Herausforderer hat zu viele Heiltränke im Inventar.");
            from.sendMessage("§c§lEs sind höchstens " + r.getHeals() + "Stack" + (r.getHeals() > 1 ? "s" : "") + " Heiltränke erlaubt.");
            return;
        }
        final Arena a = new Arena(amap, clanfrom, clanto, r.getSlots() * 2, r.hasBodyFix(), r.hasOPs(), r.getHeals(), r.getEntryCost());
        if (a.loadLocations() == false) {
            to.sendMessage("§3§lClanWar>§c Es gab einen Fehler. Melde dies einen Admin.");
            from.sendMessage("§3§lClanWar>§c Es gab einen Fehler. Melde dies einen Admin.");
            return;
        }
        from.sendMessage("§3§lClanWar>§7 Du bist dem ClanWar §abeigetreten§7.");
        from.sendMessage("§3§lClanWar>§7 Deine Mitschreiter müssen §3jetzt §7beitreten.");
        ms.showItemBar(to, "§fDu bist dem ClanWar §abeigetreten§f.");
        to.sendMessage("§3§lClanWar>§7 Deine Mitschreiter müssen §3jetzt §7beitreten.");
        if (a.hasOPs() == false) {
            from.removePotionEffect(PotionEffectType.ABSORPTION);
            from.removePotionEffect(PotionEffectType.REGENERATION);
            from.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            to.removePotionEffect(PotionEffectType.ABSORPTION);
            to.removePotionEffect(PotionEffectType.REGENERATION);
            to.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
        }
        from.setGameMode(GameMode.SURVIVAL);
        to.setGameMode(GameMode.SURVIVAL);
        ms.removeGodMode(from.getName());
        ms.removeGodMode(to.getName());
        if (ms.isVanish(from)) {
            from.performCommand("vanish");
        }
        if (ms.isVanish(to)) {
            to.performCommand("vanish");
        }
        ms.getMenu().disableForcefield(from);
        ms.getMenu().disableForcefield(to);
        from.setAllowFlight(false);
        from.setFlying(false);
        to.setAllowFlight(false);
        to.setFlying(false);
        ms.forcePlayer(from, a.getLocA());
        ms.forcePlayer(to, a.getLocB());
        a.addToClanA(from);
        a.addToClanB(to);
        arenas.add(a);
        requests.remove(from.getUniqueId());
        final Server sr = ms.getServer();
        final List<UUID> membersA = ms.getDB().getClanMembersOfPlayer(from.getUniqueId()), membersB = ms.getDB().getClanMembersOfPlayer(to.getUniqueId());
        if (membersB != null) {
            for (final UUID uuid : membersB) {
                final Player pl = sr.getPlayer(uuid);
                if (pl != null && to.getUniqueId().equals(uuid) == false) {
                    ms.chatClickHoverRun(pl, "§3§lClanWar>§7 Ein ClanWar gegen §3" + clanfrom + "§7 hat §abegonnen§7.", "§3§lClanwar gegen " + clanfrom + "\n§7Spieler: §f" + r.getSlots() + " gegen " + r.getSlots() + "\n§7Map: §f" + amap + "\n§7Bodyfix: " + (r.hasBodyFix() ? "§aaktiviert" : "§cdeaktiviert") + "\n§7OP-Äpfel: " + (r.hasOPs() ? "§aaktiviert" : "§cdeaktiviert") + "\n§7Heiltränke: §f" + r.getHeals() + " Stack" + (r.getHeals() > 1 ? "s" : "") + "\n§7Beitrittskosten: §e" + r.getEntryCost() + " MuxCoins" + "\n\n" + "§3§oKlicke, um teilzunehmen.", "/cw join " + to.getName());
                    ms.chatClickHoverRun(pl, "§3§lClanWar>§7 Klicke §3hier§7, um am Kampf teilzunehmen.", "§3§lClanwar gegen " + clanfrom + "\n§7Spieler: §f" + r.getSlots() + " gegen " + r.getSlots() + "\n§7Map: §f" + amap + "\n§7Bodyfix: " + (r.hasBodyFix() ? "§aaktiviert" : "§cdeaktiviert") + "\n§7OP-Äpfel: " + (r.hasOPs() ? "§aaktiviert" : "§cdeaktiviert") + "\n§7Heiltränke: §f" + r.getHeals() + " Stack" + (r.getHeals() > 1 ? "s" : "") + "§7, " + "\n§7Beitrittskosten: §e" + r.getEntryCost() + " MuxCoins" + "\n\n" + "§3§oKlicke, um teilzunehmen.", "/cw join " + to.getName());
                    pl.playSound(pl.getLocation(), "mob.guardian.curse", 1F, 1F);
                }
            }
        }
        if (membersA != null) {
            for (final UUID uuid : membersA) {
                final Player pl = sr.getPlayer(uuid);
                if (pl != null && from.getUniqueId().equals(uuid) == false) {
                    ms.chatClickHoverRun(pl, "§3§lClanWar>§7 Ein ClanWar gegen §3" + clanto + "§7 hat §abegonnen§7.", "§3§lClanwar gegen " + clanto + "\n§7Spieler: §f" + r.getSlots() + " gegen " + r.getSlots() + "\n§7Map: §f" + amap + "\n§7Bodyfix: " + (r.hasBodyFix() ? "§aaktiviert" : "§cdeaktiviert") + "\n§7OP-Äpfel: " + (r.hasOPs() ? "§aaktiviert" : "§cdeaktiviert") + "\n§7Heiltränke: §f" + r.getHeals() + " Stack" + (r.getHeals() > 1 ? "s" : "") + "\n§7Beitrittskosten: §e" + r.getEntryCost() + " MuxCoins" + "\n\n" + "§3§oKlicke, um teilzunehmen.", "/cw join " + from.getName());
                    ms.chatClickHoverRun(pl, "§3§lClanWar>§7 Klicke §3hier§7, um am Kampf teilzunehmen.", "§3§lClanwar gegen " + clanto + "\n§7Spieler: §f" + r.getSlots() + " gegen " + r.getSlots() + "\n§7Map: §f" + amap + "\n§7Bodyfix: " + (r.hasBodyFix() ? "§aaktiviert" : "§cdeaktiviert") + "\n§7OP-Äpfel: " + (r.hasOPs() ? "§aaktiviert" : "§cdeaktiviert") + "\n§7Heiltränke: §f" + r.getHeals() + " Stack" + (r.getHeals() > 1 ? "s" : "") + "\n§7Beitrittskosten: §e" + r.getEntryCost() + " MuxCoins" + "\n\n" + "§3§oKlicke, um teilzunehmen.", "/cw join " + from.getName());
                    pl.playSound(pl.getLocation(), "mob.guardian.curse", 1F, 1F);
                }
            }
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (a.notStarted() && a.countLivingA() > 0 && a.isFinished() == false) {
                    a.sendPlayerMessage("§3§lClanWar>§c Die Reservierung dieser Map ist abgelaufen.");
                    a.sendPlayerMessage("§3§lClanWar>§c Grund: §7Nicht genügend Teilnehmer");
                    a.clearArena(false, true, true);
                }
            }
        }.runTaskLater(ms, 1200L);
    }

    private boolean isMapReserved(final String map) {
        for (final Arena arena : arenas) {
            if (arena.getMap().equalsIgnoreCase(map)) {
                return true;
            }
        }
        return false;
    }

    public void checkTime() {
        for (final Arena a : arenas) {
            if (System.currentTimeMillis() > a.getTimeStarted() + 3600000L) {
                a.sendPlayerMessage("§3§lClanWar>§c Der Kampf wurde gestoppt.");
                a.sendPlayerMessage("§3§lClanWar>§c Grund: Über 1 Stunde.");
                a.clearArena(false, true, true);
            }
        }
    }

    public void checkSpectators() {
        for (final Arena a : arenas) {
            for (final UUID uuid : a.getSpectators()) {
                final Player pl = ms.getServer().getPlayer(uuid);
                final Location loc = a.getLocSpectator();
                if (pl == null || pl.getGameMode() != GameMode.SPECTATOR || pl.getWorld().equals(ms.getBase().getWorld())) {
                    a.getSpectators().remove(uuid);
                } else if (pl.getLocation().getWorld().getName().equals(loc.getWorld().getName()) == false || pl.getLocation().distance(loc) > 50D || pl.getLocation().getBlockY() < 13) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            pl.teleport(loc, TeleportCause.END_PORTAL);
                        }
                    }.runTaskLater(ms, 0L);
                }
            }
        }
    }

    public boolean inWar(final Player p) {
        for (final Arena a : arenas) {
            if (a.containsLivingA(p) || a.containsLivingB(p)) {
                return true;
            }
        }
        return false;
    }

    public void handleInventory(final Player p, final ItemStack i) {
        final Material m = i.getType();
        final UUID uuid = p.getUniqueId();
        final Request r = requests.get(uuid);
        if (m == Material.PAPER && r.getActivePage() != 1) {
            openInv(p, 1);
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        } else if (m == Material.DOUBLE_PLANT) {
            new MuxAnvil(ms, (input, player) -> {
                int coins;
                try {
                    coins = Integer.parseInt(ms.removeNonDigits(ms.retardCleaner(input, "MuxCoins: 0")));
                } catch (final NumberFormatException ex) {
                    ms.showItemBar(player, ms.getLang("shop.notvalid"));
                    return;
                }
                if (coins < 5) {
                    ms.showItemBar(player, "§cDer Betrag muss 5 MuxCoins oder höher betragen.");
                    return;
                } else if (ms.getMuxUser(player.getName()).getCoins() < coins) {
                    ms.showItemBar(player, "§cDu besitzt nicht genügend MuxCoins.");
                    return;
                }
                r.setEntryCost(coins);
                ms.setActiveInv(player.getName(), InvType.CLANWAR);
                openInv(player, 0);
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            }).show(p, "MuxCoins: 0", new ItemStack(Material.DOUBLE_PLANT));
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        } else if (m == Material.CHAINMAIL_CHESTPLATE) {
            r.setBodyFix(r.hasBodyFix() ^ true);
            openInv(p, 0);
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
        } else if (m == Material.GOLDEN_APPLE) {
            r.setOPs(r.hasOPs() ^ true);
            openInv(p, 0);
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
        } else if (m == Material.POTION) {
            if (i.getDurability() == 16421) {
                if (r.getActivePage() == 2) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                openInv(p, 2);
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            } else {
                r.setHeals(i.getAmount());
                openInv(p, 0);
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            }
        } else if (m == Material.SKULL_ITEM) {
            final String title = i.getItemMeta().getDisplayName();
            if (title == null) return;
            final String num = ChatColor.stripColor(title).substring(0, 1);
            r.setSlots(Integer.parseInt(num));
            openInv(p, 0);
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
        } else if (m == Material.STAINED_CLAY && i.getDurability() == 5) {
            final String clan = p.hasMetadata("clanwar") ? p.getMetadata("clanwar").get(0).asString() : null;
            if (clan != null) {
                sendRequest(p, r, clan);
                return;
            }
            new MuxAnvil(ms, (input, player) -> {
                final String clanto = ms.getDB().getClanCase(ms.retardCleaner(input, "Name des Clans: "));
                sendRequest(player, r, clanto);
            }).show(p, "Name des Clans: ", new ItemStack(Material.CHAINMAIL_HELMET));
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        } else if (m == Material.BARRIER) {
            openInv(p, 0);
            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
        } else if (m == Material.ITEM_FRAME) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            final String display = i.getItemMeta().getDisplayName();
            if (display.contains("Clanwar")) {
                openInv(p, 0);
                return;
            }
            final String clan = p.hasMetadata("clanwar") ? p.getMetadata("clanwar").get(0).asString() : null;
            if (clan != null) {
                p.performCommand("c " + clan);
                return;
            }
            p.performCommand("clan");
        } else {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
        }
    }

    private void sendRequest(final Player p, final Request r, final String clanto) {
        if (clanto == null) {
            ms.showItemBar(p, "§cDieser Clan existiert nicht.");
            return;
        } else if (clanto.equals(ms.getDB().getClanFromPlayer(p.getUniqueId()))) {
            ms.showItemBar(p, "§cDu kannst dich nicht selbst einladen.");
            return;
        } else if (ms.getMuxUser(p.getName()).getCoins() < r.getEntryCost()) {
            ms.showItemBar(p, "§cDu besitzt nicht genügend MuxCoins.");
            return;
        }
        final Player leader = ms.getServer().getPlayer(ms.getDB().getRankMembers(clanto, 2).get(0));
        if (leader == null || p.canSee(leader) == false) {
            ms.showItemBar(p, "§cDer Clan Anführer ist derzeit offline.");
            return;
        }
        ms.checkGeneralCooldown(p.getName(), "CLANWAR", 60000L, true);
        final String clanfrom = ms.getDB().getClanFromPlayer(p.getUniqueId());
        final int players = r.getSlots();
        ms.showItemBar(p, "§fDie ClanWar Anfrage wurde §agesendet§f.");
        ms.chatClickHoverRun(leader, "§3§lClanWar>§7 Der Clan §3" + clanfrom + " §7möchte deinen Clan herausfordern.", "§3§lClanwar gegen " + clanfrom + "\n§7Spieler: §f" + players + " gegen " + players + "\n§7Bodyfix: " + (r.hasBodyFix() ? "§aaktiviert" : "§cdeaktiviert") + "\n§7OP-Äpfel: " + (r.hasOPs() ? "§aaktiviert" : "§cdeaktiviert") + "\n§7Heiltränke: §f" + r.getHeals() + " Stack" + (r.getHeals() > 1 ? "s" : "") + "\n" + "§7Beitrittskosten: §e" + r.getEntryCost() + " MuxCoins" + "\n\n" + "§3§oKlicke, um anzunehmen.", "/cw accept " + p.getName());
        ms.chatClickHoverRun(leader, "§3§lClanWar>§7 Klicke §3hier§7, um die Herausforderung anzunehmen.", "§3§lClanwar gegen " + clanfrom + "\n§7Spieler: §f" + players + " gegen " + players + "\n§7Bodyfix: " + (r.hasBodyFix() ? "§aaktiviert" : "§cdeaktiviert") + "\n§7OP-Äpfel: " + (r.hasOPs() ? "§aaktiviert" : "§cdeaktiviert") + "\n§7Heiltränke: §f" + r.getHeals() + " Stack" + (r.getHeals() > 1 ? "s" : "") + "\n" + "§7Beitrittskosten: §e" + r.getEntryCost() + " MuxCoins" + "\n\n" + "§3§oKlicke, um anzunehmen.", "/cw accept " + p.getName());
        leader.playSound(leader.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
    }


    public boolean handleDeath(final Player p, final boolean quit) {
        for (final Arena a : arenas) {
            if (a.isFinished()) {
                continue;
            } else if (a.notStarted() || a.removeLivingA(p) == false && a.removeLivingB(p) == false) {
                continue;
            }
            if (quit == false) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        p.spigot().respawn();
                        sendScoreboard(p);
                        ms.forcePlayer(p, ms.getGameSpawn());
                    }
                }.runTaskLater(ms, 5L);
            }
            a.sendPlayerMessage("§3§lClanWar>§7 Der Spieler §3" + p.getName() + " §7ist §causgeschieden§7.");
            if (a.countLivingA() == 0 || a.countLivingB() == 0) {
                a.playSound(Sound.ENDERDRAGON_DEATH, 1F);
                a.clearArena(true, false, false);
            } else {
                a.playSound(Sound.AMBIENCE_THUNDER, 1F);
            }
            return true;
        }
        return false;
    }

    public boolean handleQuit(final Player p) {
        requests.remove(p.getUniqueId());
        for (final Arena a : arenas) {
            if (a.isFinished() && (a.containsLivingA(p) || a.containsLivingB(p))) {
                ms.forcePlayer(p, ms.getGameSpawn());
                continue;
            } else if (a.notStarted() && (a.removeParticipantA(p) || a.removeParticipantB(p))) {
                final String clan = ms.getDB().getClanFromPlayer(p.getUniqueId()); // If one of the leaders leaves, cancel the clanwar and tp everyone
                a.sendPlayerMessage("§3§lClanWar>§7 " + p.getName() + " hat die Arena §cverlassen§7. §7[" + (a.countLivingA() + a.countLivingB()) + "/" + a.getSlots() + "]");
                ms.forcePlayer(p, ms.getGameSpawn());
                if (clan != null && ms.getDB().getRankMembers(clan, 2).get(0).equals(p.getUniqueId())) {
                    a.sendPlayerMessage("§3§lClanWar>§c ClanWar wurde abgebrochen da ein Clanadmin das Spiel verlassen hat.");
                    a.clearArena(false, true, true);
                }
                return false;
            }
            return handleDeath(p, true);
        }
        return false;
    }

    public class Arena {
        private boolean running = false, finished = false;
        private final HashSet<Player> livingA = new HashSet<>(), livingB = new HashSet<>();
        private final HashSet<OfflinePlayer> clanB = new HashSet<>(), clanA = new HashSet<>();
        private final CopyOnWriteArrayList<UUID> spectators = new CopyOnWriteArrayList<>();
        private final boolean bodyfix, ops;
        private final int entrycost, heals;
        private final long timestarted;
        private final String map, clanNameA, clanNameB;
        private Location locA, locB, gateA, gateB, spectate;

        private int slots, totalmuxcoins;

        public Arena(final String map, final String clanNameA, final String clanNameB, final int slots, final boolean bodyfix, final boolean ops, final int heals, final int entrycost) {
            this.map = map.toLowerCase();
            this.slots = slots;
            this.bodyfix = bodyfix;
            this.ops = ops;
            this.heals = heals;
            this.entrycost = entrycost;
            this.timestarted = System.currentTimeMillis();
            this.clanNameA = clanNameA;
            this.clanNameB = clanNameB;
        }

        public boolean notStarted() {
            return running == false;
        }

        public int getSlots() {
            return slots;
        }

        public int getHeals() {
            return heals;
        }

        public boolean hasBodyFix() {
            return bodyfix;
        }

        public boolean hasOPs() {
            return ops;
        }

        public int getEntryCost() {
            return entrycost;
        }

        public long getTimeStarted() {
            return timestarted;
        }

        public String getMap() {
            return map;
        }

        public boolean isFinished() {
            return finished;
        }

        public Location getLocA() {
            return locA;
        }

        public Location getLocB() {
            return locB;
        }

        public List<UUID> getSpectators() {
            return spectators;
        }

        public Location getLocSpectator() {
            return spectate;
        }

        public String getClanNameA() {
            return clanNameA;
        }

        public String getClanNameB() {
            return clanNameB;
        }

        public boolean containsLivingA(final Player p) {
            return livingA.contains(p);
        }

        public boolean containsLivingB(final Player p) {
            return livingB.contains(p);
        }

        public boolean removeLivingA(final Player p) {
            return livingA.remove(p);
        }

        public boolean removeLivingB(final Player p) {
            return livingB.remove(p);
        }

        public boolean removeParticipantA(final Player p) {
            livingA.remove(p);
            return clanA.remove(p);
        }

        public boolean removeParticipantB(final Player p) {
            livingB.remove(p);
            return clanB.remove(p);
        }

        public int countLivingA() {
            return livingA.size();
        }

        public int countLivingB() {
            return livingB.size();
        }

        public void addToClanA(final Player p) {
            livingA.add(p);
            clanA.add(p);
            p.playSound(p.getLocation(), "mob.guardian.elder.idle", 1F, 1F);
        }

        public void addToClanB(final Player p) {
            livingB.add(p);
            clanB.add(p);
            p.playSound(p.getLocation(), "mob.guardian.elder.idle", 1F, 1F);
        }

        public void spectate(final Player p) {
            if (ms.inEvent(p)) {
                ms.showItemBar(p, "§cDu bist gerade in einem Event.");
                return;
            } else if (ms.inDuel(p)) {
                ms.showItemBar(p, "§cDu bist derzeit in einem Duell.");
                return;
            } else if (ms.in1vs1(p)) {
                ms.showItemBar(p, "§cDu bist derzeit in einem 1vs1.");
                return;
            } else if (ms.inGame(p)) {
                ms.showItemBar(p, "§cDu spielst gerade ein Minigame.");
                return;
            } else if (p.getGameMode() == GameMode.SPECTATOR) {
                ms.showItemBar(p, "§cDu bist schon im Zuschauermodus.");
                return;
            }
            ms.getMounts().deactivateMount(p);
            ms.getPets().deactivatePet(p);
            ms.setLastLocation(p);
            ms.forcePlayer(p, spectate);
            p.playSound(spectate, Sound.ENDERMAN_TELEPORT, 0.2F, 1F);
            p.setGameMode(GameMode.SPECTATOR);
            spectators.add(p.getUniqueId());
            ms.showItemBar(p, "§fDu wurdest zum §3Clanwar §fteleportiert.");
        }

        public void sendPlayerMessage(final String message) {
            for (final OfflinePlayer p : clanA) {
                if (p.isOnline()) {
                    p.getPlayer().sendMessage(message);
                }
            }
            for (final OfflinePlayer p : clanB) {
                if (p.isOnline()) {
                    p.getPlayer().sendMessage(message);
                }
            }
        }

        public void sendTitle(final String message) {
            for (final OfflinePlayer p : clanA) {
                if (p.isOnline()) {
                    ms.sendNormalTitle(p.getPlayer(), message, 0, 22, 20);
                }
            }
            for (final OfflinePlayer p : clanB) {
                if (p.isOnline()) {
                    ms.sendNormalTitle(p.getPlayer(), message, 0, 22, 20);
                }
            }
        }

        public void playSound(final Sound sound, final float pitch) {
            for (final OfflinePlayer p : clanA) {
                if (p.isOnline()) {
                    p.getPlayer().playSound(p.getPlayer().getLocation(), sound, 1F, pitch);
                }
            }
            for (final OfflinePlayer p : clanB) {
                if (p.isOnline()) {
                    p.getPlayer().playSound(p.getPlayer().getLocation(), sound, 1F, pitch);
                }
            }
        }

        public void checkIfCanStart() {
            if (livingA.size() + livingB.size() == slots && running == false) {
                startGame();
            }
        }

        public boolean canJoin(final Player p, final boolean teamA) {
            if (livingA.size() + livingB.size() >= slots) {
                ms.showItemBar(p, "§cDer Clanwar ist bereits voll.");
                return false;
            } else if (teamA && livingA.size() >= slots / 2 || teamA == false && livingB.size() >= slots / 2) {
                ms.showItemBar(p, "§cEs sind bereits genug Spieler von deinem Clan.");
                return false;
            } else if (livingA.contains(p) || livingB.contains(p)) {
                ms.showItemBar(p, "§cDu bist bereits im Clanwar.");
                return false;
            } else if (ms.getNewbies().isProtectedNewbie(p)) {
                ms.showItemBar(p, "§cDu kannst als Neuling noch nicht Clanwars beitreten.");
                return false;
            } else if (running) {
                ms.showItemBar(p, "§cDer Clanwar läuft bereits.");
                return false;
            } else if (ms.inGame(p)) {
                ms.showItemBar(p, "§cDu spielst gerade ein Minigame.");
                return false;
            } else if (ms.inEvent(p)) {
                ms.showItemBar(p, "§cDu bist gerade in einem Event.");
                return false;
            } else if (ms.isBeingTeleported(p.getName())) {
                ms.showItemBar(p, "§cDu wirst gerade teleportiert.");
                return false;
            }
            final int healsfrom = ms.getMaterialCount(p.getInventory(), Material.POTION, (byte) 16389) +
                    ms.getMaterialCount(p.getInventory(), Material.POTION, (byte) 16421) +
                    ms.getMaterialCount(p.getInventory(), Material.POTION, (byte) 16453);
            if (healsfrom / 64D > (double) heals) {
                ms.showItemBar(p, "§cEs sind höchstens " + heals + " Stack" + (heals > 1 ? "s" : "") + " Heiltränke erlaubt.");
                return false;
            }
            return true;
        }

        public void clearArena(final boolean win, final boolean instant, final boolean expired) {
            if (win) {
                String winner = "-";
                if (livingB.isEmpty()) {
                    winner = ms.getDB().getClanFromPlayer(livingA.iterator().next().getUniqueId());
                    for (final OfflinePlayer p : clanA) {
                        final MuxUser u = p.isOnline() ? ms.getMuxUser(p.getName()) : ms.getDB().loadPlayer(p.getUniqueId());
                        if (p.isOnline()) {
                            ms.resetScoreboard(p.getPlayer());
                            ms.sendTitle(p.getPlayer(), "§3§lClanWar", 10, 120, 10);
                            ms.sendSubTitle(p.getPlayer(), "§fDein Clan hat den Kampf §agewonnen§f!", 10, 60, 10);
                            p.getPlayer().playSound(p.getPlayer().getLocation(), Sound.FIREWORK_BLAST, 1F, 1F);
                            ms.runLater(new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (p.isOnline() == false) return;
                                    ms.sendSubTitle(p.getPlayer(), "§fEuer Gewinn: §e§l" + ms.getNumberFormat(totalmuxcoins) + " MuxCoins", 0, 60, 10);
                                    ms.resetScoreboard(p.getPlayer());
                                }
                            }, 60L);
                        }
                        if (u != null) {
                            final int coins = totalmuxcoins / clanA.size(); // Using totalmuxcoins (because force starts are possible like 3vs1)
                            ms.getHistory().addHistory(p.getUniqueId(), null, "COINS", String.valueOf(coins), "ClanWar");
                            u.addCoins(coins);
                        }
                    }
                    for (final OfflinePlayer p : clanB) {
                        if (p.isOnline()) {
                            p.getPlayer().sendMessage("§3§lClanWar>§c Dein Clan hat den Kampf verloren.");
                            ms.resetScoreboard(p.getPlayer());
                        }
                    }
                } else if (livingA.isEmpty()) {
                    winner = ms.getDB().getClanFromPlayer(livingB.iterator().next().getUniqueId());
                    for (final OfflinePlayer p : clanB) {
                        final MuxUser u = p.isOnline() ? ms.getMuxUser(p.getName()) : ms.getDB().loadPlayer(p.getUniqueId());
                        if (p.isOnline()) {
                            ms.resetScoreboard(p.getPlayer());
                            ms.sendTitle(p.getPlayer(), "§3§lClanWar", 10, 120, 10);
                            ms.sendSubTitle(p.getPlayer(), "§fDein Clan hat den Kampf §agewonnen§f!", 10, 60, 10);
                            p.getPlayer().playSound(p.getPlayer().getLocation(), Sound.FIREWORK_BLAST, 1F, 1F);
                            ms.runLater(new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (p.isOnline() == false) return;
                                    ms.sendSubTitle(p.getPlayer(), "§fEuer Gewinn: §e§l" + ms.getNumberFormat(totalmuxcoins) + " MuxCoins", 0, 60, 10);
                                    ms.resetScoreboard(p.getPlayer());
                                }
                            }, 60L);
                        }
                        if (u != null) {
                            final int coins = totalmuxcoins / clanB.size(); // Using totalmuxcoins (because force starts are possible like 3vs1)
                            ms.getHistory().addHistory(p.getUniqueId(), null, "COINS", String.valueOf(coins), "ClanWar");
                            u.addCoins(coins);
                        }
                    }
                    for (final OfflinePlayer p : clanA) {
                        if (p.isOnline()) {
                            p.getPlayer().sendMessage("§3§lClanWar>§c Dein Clan hat den Kampf verloren.");
                            ms.resetScoreboard(p.getPlayer());
                        }
                    }
                }
                for (final Player pl : ms.getServer().getOnlinePlayers()) {
                    if (clanA.contains(pl) == false && clanB.contains(pl) == false) {
                        pl.sendMessage("§3§lClanWar>§7 Der Clan §3" + winner + "§7 hat den Kampf gewonnen.");
                    } else {
                        ms.removeFromBattle(pl.getName());
                    }
                }
            } else if (expired == false) {
                sendPlayerMessage("§3§lClanWar>§7 Der Kampf wurde von einem Admin §cgestoppt§7.");
            }
            finished = true;
            if (instant) {
                stopGame(win);
            } else {
                running = false;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        stopGame(win);
                    }
                }.runTaskLater(ms, 300L);
            }
        }

        private void stopGame(final boolean win) {
            for (final Scoreboard sb : ms.getScoreboards()) {
                final Team teamA = sb.getTeam(clanNameA), teamB = sb.getTeam(clanNameB);
                if (teamA != null) teamA.unregister();
                if (teamB != null) teamB.unregister();
            }
            for (final OfflinePlayer pl : clanA) {
                if (pl.isOnline() == false) continue;
                final Player p = pl.getPlayer();
                if (win == false && running) {
                    ms.getMuxUser(p.getName()).addCoins(entrycost);
                    ms.getHistory().addHistory(p.getUniqueId(), null, "COINS", String.valueOf(entrycost), "ClanWar");
                    ms.resetScoreboard(p);
                }
                if (livingA.contains(p)) {
                    ms.forcePlayer(p, ms.getGameSpawn());
                }
                try {
                    ms.getPerms().removePermissions(p, p.getName());
                    ms.getPerms().loadPerms(p);
                } catch (final Exception ignored) {
                }
            }
            for (final OfflinePlayer pl : clanB) {
                if (pl.isOnline() == false) continue;
                final Player p = pl.getPlayer();
                if (win == false && running) {
                    ms.getMuxUser(p.getName()).addCoins(entrycost);
                    ms.getHistory().addHistory(p.getUniqueId(), null, "COINS", String.valueOf(entrycost), "ClanWar");
                    ms.resetScoreboard(p);
                }
                if (livingB.contains(p)) {
                    ms.forcePlayer(p, ms.getGameSpawn());
                }
                try {
                    ms.getPerms().removePermissions(p, p.getName());
                    ms.getPerms().loadPerms(p);
                } catch (final Exception ignored) {
                }
            }
            for (final UUID uuid : spectators) {
                final Player pl = ms.getServer().getPlayer(uuid);
                if (pl != null) {
                    final MuxUser u = ms.getMuxUser(pl.getName());
                    final Location lastloc = u.getLastLocation(),
                            l = lastloc != null && lastloc.getWorld() != null ? lastloc : ms.getGameSpawn();
                    ms.forcePlayer(pl, l);
                }
            }
            running = false;
            clanB.clear();
            clanA.clear();
            livingB.clear();
            livingA.clear();
            arenas.remove(this);
        }

        public void startGame() {
            slots = clanB.size() + clanA.size(); // If start is forced, then you need to adjust slots accordingly
            for (final Player p : livingA) {
                ms.getMuxUser(p.getName()).addCoins(-entrycost);
                ms.getHistory().addHistory(p.getUniqueId(), null, "COINS", String.valueOf(-entrycost), "ClanWar");
                sendScoreboard(p);
                totalmuxcoins += entrycost;
            }
            for (final Player p : livingB) {
                ms.getMuxUser(p.getName()).addCoins(-entrycost);
                ms.getHistory().addHistory(p.getUniqueId(), null, "COINS", String.valueOf(-entrycost), "ClanWar");
                sendScoreboard(p);
                totalmuxcoins += entrycost;
            }
            running = true;
            final String mapname = "clanwar" + map.replace("ä", "a").replace("ö", "o").replace("ü", "u");
            if (ops) {
                ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "region flag " + mapname + " -w " + locA.getWorld().getName() + " pistons allow");
            } else {
                ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "region flag " + mapname + " -w " + locA.getWorld().getName() + " pistons deny");
            }
            if (bodyfix) {
                ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "region flag " + mapname + " -w " + locA.getWorld().getName() + " blocked-cmds");
            } else {
                ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "region flag " + mapname + " -w " + locA.getWorld().getName() + " blocked-cmds fix,repair");
            }
            new BukkitRunnable() {
                short seconds = 5;

                @Override
                public void run() {
                    if (running == false) {
                        cancel();
                        return;
                    } else if (seconds > 0) {
                        playSound(Sound.NOTE_STICKS, 0F);
                        sendTitle("§fDer Kampf beginnt in §3" + seconds + " Sekunde" + (seconds > 1 ? "n" : "") + "§f...");
                        seconds--;
                        return;
                    }
                    for (final Scoreboard sb : ms.getScoreboards()) {
                        final Team a = sb.registerNewTeam(clanNameA);
                        a.setNameTagVisibility(NameTagVisibility.ALWAYS);
                        a.setPrefix(ms.getDB().getChatText(ms.getDB().getRankMembers(clanNameA, 2).get(0)) + "§8*§f");
                        final Team b = sb.registerNewTeam(clanNameB);
                        b.setNameTagVisibility(NameTagVisibility.ALWAYS);
                        b.setPrefix(ms.getDB().getChatText(ms.getDB().getRankMembers(clanNameB, 2).get(0)) + "§8*§f");
                        for (final Player pl : ms.getServer().getOnlinePlayers()) {
                            if (livingA.contains(pl) && pl.hasPermission("muxteam") == false) {
                                a.addEntry(pl.getName());
                            } else if (livingB.contains(pl) && pl.hasPermission("muxteam") == false) {
                                b.addEntry(pl.getName());
                            }
                        }
                    }
                    for (final Player pl : ms.getServer().getOnlinePlayers()) {
                        if (livingA.contains(pl) == false && livingB.contains(pl) == false) {
                            ms.chatClickHoverRun(pl, "§3§lClanWar>§7 Der Clan §c" + clanNameB + " §7kämpft gegen den Clan §b" + clanNameA + "§7.",
                                    "§3§lClanWar: §c" + clanNameB + " §7gegen §b" + clanNameA + "\n§7Spieler: §f" + slots / 2 + " gegen " + slots / 2 + "\n§7Map: §f" + StringUtils.capitalize(map) + "\n§7Bodyfix: " + (bodyfix ? "§aaktiviert" : "§cdeaktiviert") + "\n§7OP-Äpfel: " + (ops ? "§aaktiviert" : "§cdeaktiviert") + "\n§7Heiltränke: §f" + heals + " Stack" + (heals > 1 ? "s" : "") + "\n" + "§7Jackpot: §e" + entrycost * (clanA.size() + clanB.size()) + " MuxCoins" + "\n\n" + "§3§oKlicke, um zuzugucken.", "/cw spectate " + map);
                            ms.chatClickHoverRun(pl, "§3§lClanWar>§7 Klicke §3hier§7, um diesen Kampf zu sehen.",
                                    "§3§lClanWar: §c" + clanNameB + " §7gegen §b" + clanNameA + "\n§7Spieler: §f" + slots / 2 + " gegen " + slots / 2 + "\n§7Map: §f" + StringUtils.capitalize(map) + "\n§7Bodyfix: " + (bodyfix ? "§aaktiviert" : "§cdeaktiviert") + "\n§7OP-Äpfel: " + (ops ? "§aaktiviert" : "§cdeaktiviert") + "\n§7Heiltränke: §f" + heals + " Stack" + (heals > 1 ? "s" : "") + "\n" + "§7Jackpot: §e" + entrycost * (clanA.size() + clanB.size()) + " MuxCoins" + "\n\n" + "§3§oKlicke, um zuzugucken.", "/cw spectate " + map);
                        }
                    }
                    sendTitle("§fDer Kampf beginnt §3jetzt§f, viel Glück!");
                    setGates(true);
                    playSound(Sound.ZOMBIE_WOODBREAK, 1F);
                    cancel();
                }
            }.runTaskTimer(ms, 0L, 20L);
        }

        public void disableArena() {
            setGates(false);
            clearArena(false, true, false);
        }

        public void setGates(final boolean open) {
            final Material m = open ? Material.AIR : Material.FENCE;
            setGate(gateA, m);
            setGate(gateB, m);
        }

        public void setGate(final Location gate, final Material m) {
            gate.clone().add(0, 0, 0).getBlock().setType(m);
            gate.clone().add(0, 1, 0).getBlock().setType(m);
            gate.clone().add(0, 0, 1).getBlock().setType(m);
            gate.clone().add(0, 0, -1).getBlock().setType(m);
            gate.clone().add(0, 0, 2).getBlock().setType(m);
            gate.clone().add(0, 0, -2).getBlock().setType(m);
            gate.clone().add(0, 0, 3).getBlock().setType(m);
            gate.clone().add(0, 0, -3).getBlock().setType(m);
            gate.clone().add(0, 1, 1).getBlock().setType(m);
            gate.clone().add(0, 1, -1).getBlock().setType(m);
            gate.clone().add(0, 1, 2).getBlock().setType(m);
            gate.clone().add(0, 1, -2).getBlock().setType(m);
            gate.clone().add(0, 1, 3).getBlock().setType(m);
            gate.clone().add(0, 1, -3).getBlock().setType(m);
            gate.clone().add(0, 2, 0).getBlock().setType(m);
            gate.clone().add(0, 2, 1).getBlock().setType(m);
            gate.clone().add(0, 2, -1).getBlock().setType(m);
            gate.clone().add(0, 2, 2).getBlock().setType(m);
            gate.clone().add(0, 2, -2).getBlock().setType(m);
            gate.clone().add(0, 2, 3).getBlock().setType(m);
            gate.clone().add(0, 2, -3).getBlock().setType(m);
        }

        public boolean loadLocations() {
            this.locA = ms.getLoc("clanwar" + this.map + "spawn1");
            this.locB = ms.getLoc("clanwar" + this.map + "spawn2");
            this.gateA = ms.getLoc("clanwar" + this.map + "gate1");
            this.gateB = ms.getLoc("clanwar" + this.map + "gate2");
            this.spectate = ms.getLoc("clanwar" + this.map + "spectate");
            if (this.locA == null || this.locB == null || this.gateA == null || this.gateB == null || this.spectate == null) {
                System.err.println("MuxSystem> Error: Could not load ClanWar Locations of the Map " + this.map);
                return false;
            }
            setGates(false);
            return true;
        }
    }

    class Request {
        private int entrycost, slots, activepage, heals = 4;
        private boolean bodyfix = true, ops = true;

        public Request() {
        }

        public int getEntryCost() {
            return entrycost;
        }

        public int getSlots() {
            return slots;
        }

        public boolean hasBodyFix() {
            return bodyfix;
        }

        public boolean hasOPs() {
            return ops;
        }

        public int getHeals() {
            return heals;
        }

        public int getActivePage() {
            return activepage;
        }

        public void setEntryCost(final int entrycost) {
            this.entrycost = entrycost;
        }

        public void setSlots(final int slots) {
            this.slots = slots;
        }

        public void setBodyFix(final boolean bodyfix) {
            this.bodyfix = bodyfix;
        }

        public void setOPs(final boolean ops) {
            this.ops = ops;
        }

        public void setHeals(final int heals) {
            this.heals = heals;
        }

        public void setActivePage(final int activepage) {
            this.activepage = activepage;
        }
    }

    private void openInv(final Player p, final int page) {
        Inventory inv = null;
        final Request r = requests.get(p.getUniqueId());
        final InvType it = ms.getActiveInv(p.getName());
        if (it != InvType.CLANS && it != InvType.MUXANVIL && it != InvType.CLANWAR && p.hasMetadata("clanwar")) {
            p.removeMetadata("clanwar", ms);
        }
        if (page == 0) {
            final String clan = p.hasMetadata("clanwar") ? p.getMetadata("clanwar").get(0).asString() : null;
            inv = ms.getServer().createInventory(null, 45, "§0§lMuxClans§0 | Clanwar" + (clan != null ? " | " + clan : ""));
            inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back") + (clan != null ? " (" + clan + ")" : "")));
            inv.setItem(4, ms.renameItemStack(new ItemStack(Material.DIAMOND_SWORD), "§3§lClanwar erstellen", "§7Kämpfe mit deinem Clan gegen", "§7den eines anderen Spielers."));
            final ItemStack item1 = ms.renameItemStack(new ItemStack(Material.PAPER), "§3§lSpieler" + (r.getSlots() > 0 ? ":§f §l" + r.getSlots() + " gegen " + r.getSlots() : " festsetzen"),
                    "§7Soll das ein großer oder eher ein", "§7kleiner Kampf sein? Hier kannst du", "§7über die Größe entscheiden.", "", "§3Klicke§7, um eine Anzahl festzusetzen."),
                    item3 = ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT), "§3§lEinsatz" + (r.getEntryCost() > 0 ? ":§e §l" + ms.getNumberFormat(r.getEntryCost()) + " MuxCoins" : " festlegen"),
                            "§7Jeder Teilnehmer muss genauso viele", "§7MuxCoins einsetzen, wie jeder andere.", "§7Der Gewinner-Clan erhält dann alles.", "", "§3Klicke§7, um den Einsatz festzulegen.");
            inv.setItem(19, r.getSlots() > 0 ? ms.addGlow(item1) : item1);
            inv.setItem(22, r.getEntryCost() > 0 ? ms.addGlow(item3) : item3);
            if (r.getEntryCost() == 0 || r.getSlots() == 0) {
                final ItemStack error = ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 14), "§c§lStarten", "");
                if (r.getEntryCost() == 0) ms.addLore(error, "§cDer Einsatz ist nicht festgelegt.");
                if (r.getSlots() == 0) ms.addLore(error, "§cDie Spieleranzahl ist nicht festgesetzt.");
                inv.setItem(25, error);
            } else {
                inv.setItem(25, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 5), "§a§lStarten",
                        "§7Hiermit erhält der Clanadmin des anderen",
                        "§7Clans eine Anfrage. Nimmt er an, so musst",
                        "§7du auf deine Clanmitglieder warten, welche",
                        "§7online sind und alle eine Anfrage erhalten.",
                        "§7Sobald genug Spieler da sind, geht es los.",
                        "",
                        "§cWichtig: §7Jeder muss seine Sachen bereit",
                        "§7haben, um sofort kämpfen zu können.",
                        "",
                        clan != null ? "§aKlicke§7, um den Clan herauszufordern." : "§aKlicke§7, um einen Clan herauszufordern."));
            }
            final ItemStack heal = ms.renameItemStack(new ItemStack(Material.POTION, 1, (short) 16421), "§3§lHeiltränke:" + (r.getHeals() == 0 ? "§c §ldeaktiviert" : "§f §l" + r.getHeals() + " Stack" + (r.getHeals() > 1 ? "s" : "")),
                    "§7Bestimme die maximale Anzahl an", "§7Heiltränken für jeden Spieler.", "", "§3Klicke§7, um die Option zu bearbeiten."),
                    bodyfix = ms.renameItemStack(new ItemStack(Material.CHAINMAIL_CHESTPLATE), "§3§lBodyfix:" + (r.hasBodyFix() ? "§a §laktiviert" : "§c §ldeaktiviert"),
                            "§7Ohne Bodyfix müsste die Rüstung vor", "§7dem Reparieren ausgezogen werden.", "", "§7Klicke, um die Option zu " + (r.hasBodyFix() ? "§cdeaktivieren§7." : "§aaktivieren§7."));
            final ItemMeta im = heal.getItemMeta();
            im.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            heal.setItemMeta(im);
            inv.setItem(37, heal);
            inv.setItem(40, r.hasBodyFix() ? ms.addGlow(bodyfix) : bodyfix);
            inv.setItem(43, ms.renameItemStack(new ItemStack(Material.GOLDEN_APPLE, 1, r.hasOPs() ? (byte) 1 : (byte) 0), "§3§lOP-Äpfel:" + (r.hasOPs() ? "§a §laktiviert" : "§c §ldeaktiviert"),
                    "§7Mit einem goldenen Apfel wirst du für", "§7eine kurze Zeit sehr schnell geheilt.", "", "§7Klicke, um die Option zu " + (r.hasOPs() ? "§cdeaktivieren§7." : "§aaktivieren§7.")));
        } else if (page == 1) {
            inv = ms.getServer().createInventory(null, 45, "§0§lMuxClans§0 | Anzahl festsetzen");
            inv.setItem(4, ms.renameItemStack(new ItemStack(Material.PAPER), "§3§lSpieler festsetzen",
                    "§7Soll das ein großer oder eher ein", "§7kleiner Kampf sein? Hier kannst du", "§7über die Größe entscheiden."));
            inv.setItem(19, ms.renameItemStack(new ItemStack(Material.SKULL_ITEM, 4, (byte) 3), "§f§l2 gegen 2", "", "§7Klicke, um diese Anzahl", "§7festzulegen."));
            inv.setItem(21, ms.renameItemStack(new ItemStack(Material.SKULL_ITEM, 6, (byte) 3), "§f§l3 gegen 3", "", "§7Klicke, um diese Anzahl", "§7festzulegen."));
            inv.setItem(23, ms.renameItemStack(new ItemStack(Material.SKULL_ITEM, 8, (byte) 3), "§f§l4 gegen 4", "", "§7Klicke, um diese Anzahl", "§7festzulegen."));
            inv.setItem(25, ms.renameItemStack(new ItemStack(Material.SKULL_ITEM, 10, (byte) 3), "§f§l5 gegen 5", "", "§7Klicke, um diese Anzahl", "§7festzulegen."));
        } else if (page == 2) {
            inv = ms.getServer().createInventory(null, 45, "§0§lMuxClans§0 | Heiltränke");
            final ItemStack heal = ms.renameItemStack(new ItemStack(Material.POTION, 1, (short) 16421), "§3§lHeiltränke",
                    "§7Bestimme die maximale Anzahl an", "§7Heiltränken für jeden Spieler.");
            final ItemMeta im = heal.getItemMeta();
            im.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            heal.setItemMeta(im);
            inv.setItem(4, heal);
            inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), "§7Zurück (Clanwar)"));
            final ItemStack i = ms.renameItemStack(new ItemStack(Material.POTION), "§3§l1 Stack", "", "§7Klicke, um die Option §3auszuwählen§7."),
                    i2 = ms.renameItemStack(new ItemStack(Material.POTION, 2), "§3§l2 Stacks", "", "§7Klicke, um die Option §3auszuwählen§7."),
                    i3 = ms.renameItemStack(new ItemStack(Material.POTION, 4), "§3§l4 Stacks", "", "§7Klicke, um die Option §3auszuwählen§7."),
                    i4 = ms.renameItemStack(new ItemStack(Material.POTION, 6), "§3§l6 Stacks", "", "§7Klicke, um die Option §3auszuwählen§7."),
                    i5 = ms.renameItemStack(new ItemStack(Material.POTION, 8), "§3§l8 Stacks", "", "§7Klicke, um die Option §3auszuwählen§7.");
            inv.setItem(18, i);
            inv.setItem(20, i2);
            inv.setItem(22, i3);
            inv.setItem(24, i4);
            inv.setItem(26, i5);
        }
        if (inv == null) return;
        else if (it != InvType.CLANWAR && it != InvType.CLANS && it != InvType.MUXANVIL) p.closeInventory();
        r.setActivePage(page);
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.CLANWAR);
    }
}