package me.muxteam.marketing;

import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MuxNewbies implements Listener {
    private MuxSystem ms;
    private final ConcurrentHashMap<UUID, NewbieData> newbiedata = new ConcurrentHashMap<>();

    public MuxNewbies(final MuxSystem ms) {
        this.ms = ms;
        ms.getDB().deleteOldNewbieData();
        ms.getServer().getPluginManager().registerEvents(this, ms);
    }

    public void close() {
        this.ms = null;
    }

    public NewbieData getNewbie(final UUID uuid) {
        return newbiedata.get(uuid);
    }

    public void loadNewbie(final UUID uuid) {
        new BukkitRunnable() {
            @Override
            public void run() {
                final NewbieData data = ms.getDB().loadNewbieData(uuid);
                if (data != null) newbiedata.put(uuid, data);
            }
        }.runTaskAsynchronously(ms);
    }

    public void handleQuit(final Player p) {
        newbiedata.remove(p.getUniqueId());
    }

    public boolean isNewbie(final Player p) {
        final NewbieData nd = newbiedata.get(p.getUniqueId());
        return nd != null && nd.hasDoneAll() == false;
    }

    public boolean isProtectedNewbie(final Player p) {
        return isProtectedNewbie(p.getUniqueId());
    }
    public boolean isProtectedNewbie(final UUID uuid) {
        final NewbieData nd = newbiedata.get(uuid);
        return nd != null && nd.hasDoneAll() == false && nd.hasntDone(Type.PROTECTION);
    }

    public void removeExpiredProtection() {
        final Server sr = ms.getServer();
        newbiedata.forEach((key, value) -> {
            if (value.hasntDone(Type.PROTECTION) && System.currentTimeMillis() >= value.getTimestamp() + 900000L) {
                final Player p = sr.getPlayer(key);
                if (p != null) {
                    ms.sendTitle(p, "§d§lMuxNeuling", 10, 60, 10);
                    ms.sendSubTitle(p, "§fDein §dNeulingsschutz §fist abgelaufen.", 10, 60, 10);
                }
                value.setDone(ms, Type.PROTECTION, key);
            }
        });
    }

    public void handleNewbieBase(final Player p) {
        p.sendMessage(new String[]{
                ms.header((byte) 16, "§a"),
                "  §a§lWillkommen zu deiner Base!",
                "  ",
                "  §7Dein Ziel ist es, mit teuren Blöcken deinen §bBasewert §7zu",
                "  §7erhöhen. Im §eShop §7findest du die aktuellen Preise. Wenn",
                "  §7du votest, vergrößert sich die §aGrenze der Base§7.",
                "",
                "  §aHinweis§7: Wenn du 3 Tage hintereinander offline bist,",
                "  §7können andere Spieler deine Base griefen.",
                ms.footer((byte) 16, "§a")
        });
        p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.4F, 1F);
    }

    public boolean handleNewbieFight(final Player p, final Player angreifer) {
        if (ms.isDebug()) return false;
        else if (isProtectedNewbie(p)) {
            ms.showItemBar(angreifer, "§cDieser Spieler ist neu. Du kannst ihn nicht angreifen.");
            return true;
        } else if (isProtectedNewbie(angreifer)) {
            ms.showItemBar(angreifer, "§cDu bist noch ein Neuling.");
            if (ms.checkGeneralCooldown(angreifer.getName(), "NewbieMessage", 60000L, true)) return true;
            ms.chatClickHoverRun(angreifer, "§6§lMuxNeuling>§7 Baue zunächst deine Base und rüste dich auf.", "§6§oKlicke um zu teleportieren.", "/base");
            ms.chatClickHoverRun(angreifer, "§6§lMuxNeuling>§7 Klicke §6hier§7, um deine Base zu bauen.", "§6§oKlicke um zu teleportieren.", "/base");
            return true;
        }
        return false;
    }

    public void handleNewbieJoin(final Player p) {
        final String pname = p.getName();
        if (ms.getAdmin().NEWBIEBROADCAST.isActive()) {
            for (final Player pl : ms.getServer().getOnlinePlayers()) {
                if (pl != p) {
                    pl.sendMessage("§b§m-----[-§r §3" + ms.getLang("newbiewelcome") + " §e" + pname + " §b§m-]-----");
                }
            }
        }
        newbiedata.put(p.getUniqueId(), new NewbieData(System.currentTimeMillis()));
        p.sendMessage(new String[] {
                ms.header((byte) 15, "§a"),
                "  §a§lHerzlich willkommen auf MuxCraft!",
                "  ",
                "  §7MuxCraft ist der §abekannteste deutsche PvP-Server§7.",
                "  §7Baue deine eigene §aBase §7oder kämpfe im §a1vs1§7, um dir",
                "  §7den §abesten Platz§7 im globalen Ranking zu sichern.",
                "",
                "  §7Das §f§lMenü§7 in deinem Inventar zeigt dir alle wichtigen",
                "  §7Orte und Funktionen. §aViel Spaß auf MuxCraft§7!",
                ms.footer((byte) 15, "§a")
        });
        new BukkitRunnable() {
            @Override
            public void run() {
                ms.getDB().saveNewbieData(p.getUniqueId(), newbiedata.get(p.getUniqueId()));
            }
        }.runTaskAsynchronously(ms);
    }

    public void handleNewbieShop(final Player p) {
        final NewbieData nd = newbiedata.get(p.getUniqueId());
        if (nd != null && nd.hasntDone(Type.VISITSHOP)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (nd.hasntDone(Type.VISITSHOP) == false) return;
                    p.sendMessage(new String[]{
                            ms.header((byte) 16, "§e"),
                            "  §e§lWillkommen im MuxShop!",
                            "  ",
                            "  §7Du kannst hier deine Items §ezum Verkauf §7stellen, die",
                            "  §7dann von anderen Spielern gekauft werden können.",
                            "",
                            "  §7§lKlicke§7 auf das Item im Rahmen, um §e1 Stück§7 zu kaufen,",
                            "  §7für §e64 Stück §7halte die §lSNEAK§7-Taste gedrückt.",
                            ms.footer((byte) 16, "§e")
                    });
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.4F, 1F);
                    nd.setDone(ms, Type.VISITSHOP, p.getUniqueId());
                }
            }.runTaskLater(ms, p.hasPermission("muxsystem.notpcooldown") ? 10L : 50L);
        }
    }

    public void handleNewbieTraining(final Player p) {
        final MuxNewbies.NewbieData nd = newbiedata.get(p.getUniqueId());
        if (nd != null && nd.hasntDone(MuxNewbies.Type.TRAINING)) {
            p.sendMessage(new String[]{
                    ms.header((byte) 16, "§6"),
                    "  §6§lWillkommen im Training!",
                    "",
                    "  §7Trainiere gegen §6ein Bot§7, um danach gegen",
                    "  §7echte Spieler zu kämpfen. Viel Glück!",
                    ms.footer((byte) 16, "§6")
            });
            nd.setDone(ms, MuxNewbies.Type.TRAINING, p.getUniqueId());
            p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.4F, 1F);
        }
    }

    public void handleNewbieGriefing(final Player p) {
        final MuxNewbies.NewbieData nd = newbiedata.get(p.getUniqueId());
        if (nd != null && nd.hasntDone(MuxNewbies.Type.GRIEFING)) {
            p.sendMessage(new String[]{
                    ms.header((byte) 16, "§c"),
                    "  §c§lWillkommen beim Griefen!",
                    "",
                    "  §7Hier kannst du eine §cinaktive Base§7 mit deinen",
                    "  §7Werkzeugen zerstören oder Blöcke klauen.",
                    ms.footer((byte) 16, "§c")
            });
            nd.setDone(ms, MuxNewbies.Type.GRIEFING, p.getUniqueId());
            p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.4F, 1F);
        }
    }

    public void handleNewbieNether(final Player p) {
        final NewbieData nd = newbiedata.get(p.getUniqueId());
        if (nd != null && nd.hasntDone(Type.VISITNETHER)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (nd.hasntDone(Type.VISITNETHER) == false || p.getLocation().getWorld().getEnvironment() != World.Environment.NETHER) return;
                    p.sendMessage(new String[]{
                            ms.header((byte) 16, "§4"),
                            "  §5§lWillkommen im Nether!",
                            "",
                            "  §7Damit du sofort zum Bereich gelangst, wo man",
                            "  §7bauen kann, klicke auf den §c§lWildnis§7 Skelett.",
                            ms.footer((byte) 16, "§4")
                    });
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.4F, 1F);
                    nd.setDone(ms, Type.VISITNETHER, p.getUniqueId());
                }
            }.runTaskLater(ms, p.hasPermission("muxsystem.notpcooldown") ? 10L : 50L);
        }
    }

    public void handleNewbieCasino(final Player p) {
        final NewbieData nd = newbiedata.get(p.getUniqueId());
        if (nd != null && nd.hasntDone(Type.VISITCASINO)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (nd.hasntDone(Type.VISITCASINO) == false) return;
                    p.sendMessage(new String[]{
                            ms.header((byte) 16, "§5"),
                            "  §5§lWillkommen im MuxCasino!",
                            "  ",
                            "  §7Das §5realistischste Casino weltweit§7 in Minecraft.",
                            "",
                            "  §7Sobald du reingehst, kannst du rechts bei der §dBank§7 dir",
                            "  §7mit §eMuxCoins §7sehr viele §dChips §7kaufen. Mit ein wenig Glück",
                            "  §7kannst du riesige Profite erzielen. Viel Spaß!",
                            ms.footer((byte) 16, "§5")
                    });
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.4F, 1F);
                    nd.setDone(ms, Type.VISITCASINO, p.getUniqueId());
                }
            }.runTaskLater(ms, 10L);
        }
    }

    public void handleNewbieSetHome(final Player p) {
        p.sendMessage(new String[]{
                ms.header((byte) 16, "§6"),
                "  §6§lDein erstes Zuhause!",
                "",
                "  §7Über dein §f§lMenü§7 kannst du jederzeit",
                "  §7zu deinem Zuhause zurückkehren!",
                ms.footer((byte) 16, "§6")
        });
        p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.4F, 1F);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerItemBreak(final PlayerItemBreakEvent e) {
        if (isNewbie(e.getPlayer())) {
            if (e.getBrokenItem().getType() == Material.DIAMOND_SWORD) {
                e.getBrokenItem().setAmount(1);
                e.getBrokenItem().setDurability(e.getBrokenItem().getType().getMaxDurability());
                ms.sendNormalTitle(e.getPlayer(), "§fDein Schwert wurde automatisch §arepariert§f.", 0, 50, 10);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ms.sendNormalTitle(e.getPlayer(), "§fNutze den Befehl §6/fix §fnächstes mal.", 0, 50, 10);
                    }
                }.runTaskLater(ms, 60L);
            } else if (ms.isTool(e.getBrokenItem().getType())) {
                ms.chatClickHoverRun(e.getPlayer(), "§6§lMuxCraft>§7 Klicke §6hier§7, um neue Werkzeuge zu bekommen.", "§6§oKlicke, um Werkzeuge zu bekommen", "/warp tools");
            }
        }
    }

    public final class NewbieData {
        private final long timestamp;
        private final Set<Byte> done = new HashSet<>();

        public NewbieData(final long timestamp) {
            this.timestamp = timestamp;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public Set<Byte> getDone() {
            return done;
        }

        public boolean hasntDone(final Type t) {
            return done.contains((byte) t.ordinal()) == false;
        }

        public void setDone(final MuxSystem ms, final Type t, final UUID uuid) {
            done.add((byte) t.ordinal());
            new BukkitRunnable() {
                @Override
                public void run() {
                    ms.getDB().saveNewbieData(uuid, NewbieData.this);
                }
            }.runTaskAsynchronously(ms);
        }

        public boolean hasDoneAll() {
            return done.size() == Type.values().length;
        }
    }

    public enum Type {
        VISITSHOP, PROTECTION, VISITNETHER, TRAINING, VISITCASINO, GRIEFING
    }
}