package me.muxteam.muxteam;

import ac.grim.grimac.AbstractCheck;
import ac.grim.grimac.GrimUser;
import ac.grim.grimac.events.FlagEvent;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.access.IViolationInfo;
import fr.neatmonster.nocheatplus.hooks.NCPHook;
import fr.neatmonster.nocheatplus.hooks.NCPHookManager;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;

public final class MuxAntiCheat implements Listener {
    private String latestkick;
    private final Set<String> verified = new HashSet<>(), forge = new HashSet<>(), labymod = new HashSet<>();
    private final List<String> anticheatkicks = new ArrayList<>();
    private final Map<String, Map.Entry<String, Double>> violations = new HashMap<>();
    private final Set<String> checktypes = new HashSet<>();
    private final Map<String, Long> bandelay = new HashMap<>();
    private final Random r = new Random();
    private final Map<String, Long> banned = new HashMap<>();

    private final Set<MuxAntiCheat.ClickTest> cpstests = new HashSet<>();
    private BukkitTask cpstask = null;
    private MuxSystem ms;

    public MuxAntiCheat(final MuxSystem ms) {
        this.ms = ms;
        NCPHookManager.addHook(CheckType.ALL, new NCPHook() {
            @Override
            public boolean onCheckFailure(final CheckType checktype, final Player p, final IViolationInfo vi) {
                // https://github.com/NoCheatPlus/NoCheatPlus/blob/2450636f3c10a985ac20612ae10a788e4f30aaa5/NCPCore/src/main/java/fr/neatmonster/nocheatplus/command/admin/InfoCommand.java#L47
                /*final ViolationHistory h = ViolationHistory.getHistory(p);
                if (h == null) return false;
                final ViolationHistory.ViolationLevel vl = h.getViolationLevel(checktype);
                if (vl == null) return false;
                final double vlsum = vl.sumVL;
                Bukkit.broadcastMessage("FAILURE " + checktype.getName() + " V " +  vi.getAddedVl() + " T " + vi.getTotalVl());
                */
                if (checktype == CheckType.COMBINED_IMPROBABLE) {
                    if (p.getItemInHand().getType() == Material.POTION) {
                        return true;
                    } else return !ms.inBattle(p.getName(), p.getLocation());
                }
                return false;
            }

            @Override
            public String getHookVersion() {
                return "0.1";
            }

            @Override
            public String getHookName() {
                return "HealBugFix";
            }
        });
        ms.getServer().getPluginManager().registerEvents(this, ms);
        final Gson gson = new Gson();
        if (ms.getHashYAML().isString("bandelay")) {
            this.bandelay.putAll(gson.fromJson(ms.getHashYAML().getString("bandelay"), new TypeToken<HashMap<String, Long>>() {
            }.getType()));
        }
        if (ms.getHashYAML().isString("bannedplayers")) {
            this.banned.putAll(gson.fromJson(ms.getHashYAML().getString("bannedplayers"), new TypeToken<HashMap<String, Long>>() {
            }.getType()));
            this.banned.entrySet().removeIf(entry -> entry.getValue() < System.currentTimeMillis());
        }
    }

    public void checkDelayedBans() {
        bandelay.entrySet().removeIf(entry -> {
            if (entry.getValue() > System.currentTimeMillis())
                return false;
            final Player p = ms.getServer().getPlayer(entry.getKey());
            if (p != null && ms.inGame(p)) {
                ms.getGames().punishCheater(p);
            }
            this.banned.put(entry.getKey(), System.currentTimeMillis() + 960000L);
            ms.getBans().handleBanCommands("punish", ms.getServer().getConsoleSender(), new String[]{entry.getKey(), "Cheat"});
            return true;
        });
    }

    @EventHandler
    public void onGrimFlag(final FlagEvent e) {
        if (!ms.getAdmin().ANTICHEAT.isActive()) {
            e.setCancelled(true);
            return;
        }
        final GrimUser u = e.getPlayer();
        if (bandelay.containsKey(u.getName()))
            return;
        final Long banned = this.banned.get(u.getName());
        if (banned != null && banned > System.currentTimeMillis())
            return;

        final Player p = ms.getServer().getPlayer(u.getUniqueId());
        final AbstractCheck check = e.getCheck();
        if (p.getWorld().getName().equals(ms.getBase().getWorld().getName())) {
            e.setCancelled(true);
            return;
        }
        if (ms.getBans().checkPvPBan(p, false))
            return;
        if (e.getViolations() > 3) // prevent falses
            violations.put(p.getName() + " " + check.getCheckName(), new AbstractMap.SimpleEntry<>(getCurrentDate(), e.getViolations()));
        if (e.getViolations() > 5)
            broadcastSuspect(p, check.getCheckName(), e.getViolations());
        if (check.getCheckName().equalsIgnoreCase("Reach")
                || check.getCheckName().equalsIgnoreCase("AntiKB")
                || check.getCheckName().equalsIgnoreCase("NoSlowA (Prediction)")) {

            if ((!ms.inGame(p) && !ms.inEvent(p) && !ms.inWar(p) && !ms.inDuel(p) && !ms.in1vs1(p) && !ms.inBattle(p.getName(), p.getLocation()))) {
                e.setCancelled(true);
                return;
            }
            final int maxViolations = getMaxViolations(check.getCheckName());
            if (maxViolations == -1)
                return;
            if (e.getViolations() >= maxViolations) {
                final int min = 30_100;
                //final int min = 60000; // test value
                //final int max = 120000; // test value
                final int max = ms.in1vs1(p) ? 120_000 : 900000; // 1v1 weniger delay weil mehr abfuck
                final int delay = r.nextInt(max - min) + min;
                bandelay.put(u.getName(), System.currentTimeMillis() + delay);
            }
        }
    }

    private int getMaxViolations(String checkname) {
        if (checkname.equalsIgnoreCase("Reach"))
            return 15;
        else if (checkname.equalsIgnoreCase("AntiKB"))
            return 30;
        else if (checkname.equalsIgnoreCase("NoSlowA (Prediction)"))
            return 30;
        return -1;
    }

    public void close() {
        final Gson gson = new Gson();
        ms.getHashYAML().set("bannedplayers", gson.toJson(this.banned));
        ms.getHashYAML().set("bandelay", gson.toJson(this.bandelay));
        ms.saveHashFile();
        this.ms = null;
    }

    public boolean isVerified(final Player p) {
        return verified.contains(p.getName());
    }

    public void addVerified(final String name) {
        if (forge.contains(name) || !labymod.contains(name)) return;
        verified.add(name);
    }

    public void remove(final String name) {
        verified.remove(name);
        forge.remove(name);
        labymod.remove(name);
    }

    public boolean hasForge(final String name) {
        return forge.contains(name);
    }

    public void setLabyMod(final Player p) {
        labymod.add(p.getName());
    }

    public void setForge(final Player p) {
        forge.add(p.getName());
    }

    public void handleQuit(final Player p) {
        remove(p.getName());
        final ClickTest test = getCPSTest(p);
        if (test != null) {
            test.getAlertedPlayers().forEach(alerted -> ms.showItemBar(alerted, "§6" + p.getName() + " §chat sich ausgeloggt."));
            test.getAlertedPlayers().clear();
            removeCPSTest(p);
        }
        cpstests.forEach(clickTest -> clickTest.getAlertedPlayers().remove(p));
    }

    public void broadcastSuspect(final Player p, final String hack, final double violations) {
        if (!ms.inBattle(p.getName(), p.getLocation())) return;
        if (hack.contains("AutoClicker") && violations > 4) {
            if (hack.contains(" B") || hack.contains(" C")) return;
            if (ms.checkGeneralCooldown(p.getName(), "AUTOCLICKERWARN", 60000L, true)) return;
            p.sendMessage("§4§lWarnung>§c Es ist unmöglich, so viel zu klicken.");
            p.sendMessage("§4§lWarnung>§c Deaktiviere §n§ljetzt§c deine Hilfsmittel.");
        }
        if (ms.getMuxUser(p.getName()).isPvPbanned()) return;
        if (ms.checkGeneralCooldown(p.getName(), "ANTICHEATSUSPECT", 60000L, true)) return;
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            if (!pl.hasPermission("muxsystem.anticheat")) continue;
            ms.chatClickHoverRun(pl, "§c§lAntiCheat>§7 Der Spieler §c" + p.getName() + " §7könnte cheaten.", "§c§oKlicke für mehr Infos", "/report " + p.getName());
        }
    }

    public int getKicks() {
        return anticheatkicks.size();
    }

    public String addBan() {
        if (latestkick == null) return "";
        anticheatkicks.add(latestkick);
        final String[] split = latestkick.split(" - ");
        ms.getAnalytics().getAnalytics().addAntiCheatBan(split[1]);
        return split[1];
    }

    public void addRealKick(final Player p) {
        anticheatkicks.add(latestkick);
        final String[] split = latestkick.split(" - ");
        ms.getAnalytics().getAnalytics().addAntiCheatKick(split[1]);
        broadcastSuspect(p, split[1], -2);
    }

    public List<String> getViolations(final String plname) {
        final List<String> list = new ArrayList<>();
        violations.entrySet().stream().filter(entry -> entry.getKey().contains(plname)).forEach(entry -> {
            list.add("§8» §7" + entry.getKey().split(" ")[1] +   " (" + entry.getValue().getValue() + ")");
        });
        anticheatkicks.stream().filter(cheat -> cheat.contains(plname.toLowerCase())).forEach(cheat -> {
            final String[] split = cheat.split(" - ");
            list.add("§7" + split[2] + "§8» §f" + split[1] + " (Kick)");
        });
        return list;
    }

    public String getViolationsString(final String plname) {
        final List<String> list = new ArrayList<>();
        violations.entrySet().stream().filter(entry -> entry.getKey().contains(plname)).forEach(entry -> {
            list.add(entry.getKey().split(" ")[1] +   " (" + entry.getValue().getValue() + ")");
        });
        return list.toString();
    }

    public List<String> getKickString() {
        final List<String> cheats = new ArrayList<>();
        for (final String type : checktypes) {
            final long count = anticheatkicks.stream().filter(cheat -> cheat != null && type != null && cheat.contains(type)).count();
            if (count == 0) continue;
            cheats.add("§7" + type + ": §f" + count);
        }
        return cheats;
    }

    /*@EventHandler
    public void onPlayerViolation(final ViolationEvent e) {
        final Check check = check;
        final Player p = e.getPlayer();
        if (p == null) return;
        if (ms.getAdmin().ANTICHEAT.isActive() == false) {
            e.setCancelled(true);
            return;
        } else if (check.getType().contains("AutoClicker")) {
            if (check.getSubType().equals("C") && ms.inBattle(p.getName(), p.getLocation()) == false) {
                e.setCancelled(true);
                return;
            } else if (check.getSubType().equals("J") && e.getViolations() >= 3 && ms.inBattle(p.getName(), p.getLocation()) == false) {
                final PunishEvent ev = new PunishEvent(p, check, false, Collections.singletonList("punish {player} Cheat"));
                ms.getServer().getPluginManager().callEvent(ev);
            }
        } else if (check.getType().equals("Misc")) {
            if (check.getSubType().equals("H") && e.getViolations() >= 10 && ms.inBattle(p.getName(), p.getLocation()) == false) {
                final PunishEvent ev = new PunishEvent(p, check, false, Collections.singletonList("punish {player} Cheat"));
                ms.getServer().getPluginManager().callEvent(ev);
            }
        }
        violations.put(p.getName() + " " + check.getType() + " " + check.getSubType(),  new AbstractMap.SimpleEntry<>(getCurrentDate(), e.getViolations()));
        broadcastSuspect(p, check.getType() + " " + check.getSubType(), e.getViolations());
    }

    @EventHandler
    public void onViolationPunish(final PunishEvent e) {
        final Player p = e.getPlayer();
        final Check check = check;
        latestkick = e.getPlayer().getName().toLowerCase() + " - " + check.getType() + " " + check.getSubType() +  " - " + getCurrentDate();
        checktypes.add(check.getType());
        if (ms.inGame(p) || ms.inEvent(p) || ms.inBattle(p.getName(), p.getLocation()) == false) {
            if (ms.inGame(p)) {
                ms.getGames().punishCheater(p);
            }
            new BukkitRunnable() {
                @Override public void run() {
                    p.kickPlayer("§cDeaktiviere deine Cheats um weiter zu spielen.");
                }
            }.runTask(ms);
            addRealKick(p);
            e.setCancelled(true);
        }
    }*/

    private String getCurrentDate() {
        final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("CET"));
        return sdf.format(new Date());
    }

    // CPS
    public void removeCPS(final Player p) {
        ClickTest test = null;
        for (final ClickTest clickTest : cpstests) {
            if (clickTest.getAlertedPlayers().contains(p)) {
                ms.showItemBar(p, "§fKlicks pro Sekunden Anzeige §causgeschalten§f.");
                clickTest.getAlertedPlayers().remove(p);
                test = clickTest;
                break;
            }
        }
        if (test != null && test.getAlertedPlayers().isEmpty()) {
            removeCPSTest(test.getChecked());
        }
    }

    public void watchCPS(final Player p, final Player target) {
        if (target == null || !p.canSee(target)) {
            ms.showItemBar(p, ms.hnotonline);
            return;
        }
        ClickTest test = getCPSTest(target);
        if (test == null) test = addCPSTest(target);
        test.getAlertedPlayers().add(p);
    }

    public ClickTest addCPSTest(final Player p) {
        final ClickTest test = new ClickTest(p);
        cpstests.add(test);
        if (cpstask == null) {
            cpstask = new BukkitRunnable() {
                @Override
                public void run() {
                    cpstests.forEach(ClickTest::tick);
                }
            }.runTaskTimer(ms, 1L, 1L);
        }
        return test;
    }

    public void removeCPSTest(final Player p) {
        this.cpstests.removeIf(clickTest -> clickTest.getChecked() == p);
        if (cpstask != null && this.cpstests.isEmpty()) {
            cpstask.cancel();
            cpstask = null;
        }
    }

    public ClickTest getCPSTest(final Player p) {
        return cpstests.stream().filter(clickTest -> clickTest.getChecked() == p).findFirst().orElse(null);
    }

    public void addClick(final Player p) {
        final ClickTest test = getCPSTest(p);
        if (test != null) test.addClick();
    }

    class ClickTest {
        private final Player checked;
        private final List<Long> clicks = new ArrayList<>();
        private final Set<Player> alertedPlayers = new HashSet<>();

        ClickTest(final Player checked) {
            this.checked = checked;
        }

        public void tick() {
            int cps = getCPS();
            alertedPlayers.forEach(player -> ms.showItemBar(player, "§6" + checked.getName() + " §7» " + "§f§l" + cps + " §fKlicks pro Sekunde"));
        }

        public Player getChecked() {
            return checked;
        }

        public void addClick() {
            clicks.add(System.currentTimeMillis());
        }

        public List<Long> getClicks() {
            return clicks;
        }

        public Set<Player> getAlertedPlayers() {
            return alertedPlayers;
        }

        private int getCPS() {
            clicks.removeIf(time -> time < System.currentTimeMillis() - 1000);
            return clicks.size();
        }
    }
}