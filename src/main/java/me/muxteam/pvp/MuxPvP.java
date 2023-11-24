package me.muxteam.pvp;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import me.muxteam.basic.ConfirmInventory;
import me.muxteam.basic.MuxAnvil;
import me.muxteam.basic.MuxHolograms;
import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxInventory;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.muxsystem.Settings;
import net.minecraft.server.v1_8_R3.Tuple;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public final class MuxPvP {
    private MuxSystem ms;
    private final Random r = new Random();
    private final List<String> maps = new ArrayList<>(), rankedMaps = new ArrayList<>();
    private final Map<UUID, Inventory> pvpinvs = new HashMap<>();
    private Inventory pvpinv;
    private MuxScoreboard.ScoreboardType pvpsb, rankedsb;
    private final ItemStack cyanHead, orangeHead, redHead, darkRedHead, pvpBanHead;
    // DUEL
    private final ConcurrentHashMap<UUID, DuelRequest> prequests = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Arena> arenas = new CopyOnWriteArrayList<>();
    // 1VS1 (RANKED)
    private int fightsrunning = 0;
    private final Queue<Player> rankedQueue = new LinkedBlockingQueue<>();
    private final CopyOnWriteArraySet<RankedArena> rankedArenas = new CopyOnWriteArraySet<>();
    private final Map<EnumRankedKitRarity, List<RankedKit>> rankedKits = new EnumMap<>(EnumRankedKitRarity.class);
    private final Set<Player> rankedBookCache = new HashSet<>();
    private final Map<Player, Integer[]> backexpandfood = new HashMap<>();
    private LocalDateTime[] rankedTimes;
    private BukkitTask rankedCloseTask = null;
    // 1VS1 RANKINGS
    private MuxHolograms.Hologram hologram;
    private Map<Integer, UUID> top3Last8Hours = new HashMap<>();
    private Map<Integer, Map<UUID, RankingEntry>> rankingEntries = new HashMap<>();
    private final Map<String, MuxHolograms.Hologram> holograms = new HashMap<>();
    private final Map<Byte, Object[]> top3Trophs = new HashMap<>();

    public MuxPvP(final MuxSystem ms) {
        this.ms = ms;
        this.cyanHead = ms.getHeadFromURL("https://textures.minecraft.net/texture/9dab10a2f4f3702213a64cf941ed1f743b43135abc38ee11949d7272f36489", "cyan");
        this.orangeHead = ms.getHeadFromURL("https://textures.minecraft.net/texture/a9db1760dab1ed801a594a63cdc4a2e3b3e1d12e8056a702044774a52b1d18", "orange");
        this.redHead = ms.getHeadFromURL("https://textures.minecraft.net/texture/5fde3bfce2d8cb724de8556e5ec21b7f15f584684ab785214add164be7624b", "red");
        this.darkRedHead = ms.getHeadFromURL("https://textures.minecraft.net/texture/3cc470ae2631efdfaf967b369413bc2451cd7a39465da7836a6c7a14e877", "darkRed");
        this.pvpBanHead = ms.getHeadFromURL("https://textures.minecraft.net/texture/a5d76d90b378083d14775680505ddb1e6c2c6dcf4dde7f9b1f58809bec6c65c8", "pvpBan");
        setup();
        final FileConfiguration hashYML = ms.getHashYAML();
        if (hashYML.get("pvpRankingEntries") != null && hashYML.get("pvpTop3Last8Hours") != null) {
            final Gson gson = new Gson();
            rankingEntries = gson.fromJson(hashYML.getString("pvpRankingEntries"), new TypeToken<HashMap<Integer, HashMap<UUID, RankingEntry>>>() {
            }.getType());
            top3Last8Hours = gson.fromJson(hashYML.getString("pvpTop3Last8Hours"), new TypeToken<HashMap<Integer, UUID>>() {
            }.getType());
        }
        if (hashYML.contains("PvPWinsDate")) {
            final long wintime = hashYML.getLong("PvPWinsDate");
            if ((wintime + 172800000L) > System.currentTimeMillis()) {
                final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));
                calendar.setTime(new Date(wintime - 300000L));
                Location lastPos;
                ms.getHolograms().addHologram("PvPWin", lastPos = new Location(ms.getGameSpawn().getWorld(), 21D, 80D, -7D), "§c§lPvP Gewinner im " + calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.GERMANY));
                final List<Tuple<UUID, Integer>> topPlayers = new Gson().fromJson(hashYML.getString("PvPWins"), new TypeToken<ArrayList<Tuple<UUID, Integer>>>() {
                }.getType());
                for (byte b = 0; b < topPlayers.size(); b++) {
                    final Tuple<UUID, Integer> tuple = topPlayers.get(b);
                    final OfflinePlayer d = ms.getServer().getOfflinePlayer(tuple.a());
                    final boolean beta = ms.isBeta();
                    final String cashreward = " §a➡ §l" + (b == 0 ? (beta ? "25" : "50") : b == 1 ? (beta ? "10" : "20") : (beta ? "5" : "10")) + " EUR",
                            text = "§f" + d.getName() + " - §c" + ms.getNumberFormat(tuple.b()) + cashreward;
                    holograms.put("PvPWin" + b, ms.getHolograms().addHologram("PvPWin" + b, (lastPos = lastPos.add(0, -0.4, 0)).clone(), text));
                }
            } else {
                hashYML.set("PvPWinsDate", null);
                hashYML.set("PvPWins", null);
                hashYML.set("pvpRankingEntries", null);
                hashYML.set("pvpTop3Last8Hours", null);
                rankingEntries.clear();
            }
        }
        ms.getHolograms().addIndividualHologram("1v1holo", new Location(ms.getGameSpawn().getWorld(), 4.5D, 77D, -0.5D), (holo, p) -> {
            if (is1vs1Enabled() == false) {
                holo.setName(p, "§cWartungen");
                return;
            }
            final LocalDateTime current = LocalDateTime.now(ZoneId.of("CET"));
            holo.setName(p, p.isSneaking() ? "" : ((current.isBefore(rankedTimes[0]) || current.isAfter(rankedTimes[1])) ? "§6Ab 14 Uhr" : ""));
        }, true);
    }

    public void close() {
        ms.getAdmin().DUELS.setActive(false);
        for (final Arena a : arenas) {
            a.disableArena();
        }
        for (final RankedArena ra : rankedArenas) {
            ra.disableArena();
        }
        if (rankingEntries.isEmpty() == false) {
            final Gson gson = new Gson();
            ms.getHashYAML().set("pvpRankingEntries", gson.toJson(rankingEntries));
            ms.getHashYAML().set("pvpTop3Last8Hours", gson.toJson(top3Last8Hours));
        }
        this.ms = null;
    }

    private void refreshRankingHologram() {
        final List<Object[]> ranking = ms.getDB().getTopTrophies(3);
        if (ranking == null || ranking.isEmpty()) {
            top3Trophs.clear();
        } else {
            byte i = 0;
            for (final Object[] ply : ranking) {
                top3Trophs.put(i, ply);
                i++;
            }
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                final MuxSeason season = ms.getSeason();
                if (season.getMonthEnd() - System.currentTimeMillis() > 0 && season.getMonthEnd() - System.currentTimeMillis() <= 28_800_000L) {
                    int rank = 0;
                    for (int pos = 0; pos < Math.min(3, top3Trophs.size()); pos++) {
                        if (ranking == null) continue;
                        calculateTop(rank, ranking.get(pos));
                        rank++;
                    }
                    final String mainHoloText = "§c§lTop PvP§f - §e§lLängster Platzhalter§f - §c§lGewinner in " + ms.getTime((int) ((season.getMonthEnd() - System.currentTimeMillis()) / 1000));
                    if (hologram != null) {
                        hologram.setMessage(mainHoloText);
                    } else {
                        hologram = ms.getHolograms().addHologram("TopPvP", new Location(ms.getGameSpawn().getWorld(), 2.5, 79, -12.5), mainHoloText);
                    }
                    Location lastPos = hologram.getLocation().clone();
                    final Map<Integer, UUID> rankings = new HashMap<>();
                    for (final Map.Entry<Integer, Map<UUID, RankingEntry>> entry : rankingEntries.entrySet()) {
                        final int pos = entry.getKey();
                        final Tuple<RankingEntry, String> tuple = entry.getValue().values().stream().map(rankingEntry -> {
                                    RankingEntry entry1;
                                    if (top3Last8Hours.get(pos).equals(rankingEntry.uuid)) {
                                        entry1 = new RankingEntry(rankingEntry.getUUID(), rankingEntry.isActive() ? rankingEntry.getCurrentStart() : 0, rankingEntry.isActive() ? (rankingEntry.getTotalTime() + (System.currentTimeMillis() - rankingEntry.getCurrentStart())) : rankingEntry.getTotalTime());
                                        entry1.setActive(rankingEntry.isActive());
                                        entry1.setTrophs(rankingEntry.getTrophs());
                                    } else entry1 = rankingEntry;
                                    return entry1;
                                })
                                .filter(rankingEntry -> rankings.containsKey(pos - 1) == false || rankings.get(pos - 1).equals(rankingEntry.getUUID()) == false)
                                .map(rankingEntry -> new Tuple<>(rankingEntry, ms.getServer().getOfflinePlayer(rankingEntry.getUUID()).getName()))
                                .max(Comparator.comparingLong(value -> value.a().getTotalTime())).orElse(null);
                        if (tuple == null) continue;
                        final String offlinePlayerName = tuple.b();
                        final RankingEntry rankingEntry = tuple.a();
                        rankings.put(pos, rankingEntry.getUUID());
                        final boolean beta = ms.isBeta();
                        final String cashreward = " §a➡ §l" + (pos == 0 ? (beta ? "25" : "50") : pos == 1 ? (beta ? "10" : "20") : (beta ? "5" : "10")) + " EUR",
                                text = "§f" + offlinePlayerName + " - " + " §c" + (ms.getTime((int) (rankingEntry.getTotalTime() / 1000))) + "§f - §c" + ms.getNumberFormat(rankingEntry.getTrophs()) + cashreward;
                        final MuxHolograms.Hologram holo = holograms.get("TopPvP" + pos);
                        if (holo != null) {
                            holo.setMessage(text);
                            lastPos = holo.getLocation();
                        } else {
                            holograms.put("TopPvP" + pos, ms.getHolograms().addHologram("TopPvP" + pos, (lastPos = lastPos.add(0, -0.4, 0)).clone(), text));
                        }
                    }
                } else {
                    if (hologram == null) {
                        hologram = ms.getHolograms().addHologram("TopPvP", new Location(ms.getGameSpawn().getWorld(), 2.5, 79, -12.5), "§c§lTop PvP");
                        Location lastPos = hologram.getLocation().clone();
                        for (byte step = 0; step < top3Trophs.size(); step++) {
                            Object[] ply = top3Trophs.get(step);
                            if (ply == null) continue;
                            final OfflinePlayer d = ms.getServer().getOfflinePlayer((UUID) ply[0]);
                            final boolean beta = ms.isBeta();
                            final String cashreward = " §a➡ §l" + (step == 0 ? (beta ? "25" : "50") : step == 1 ? (beta ? "10" : "20") : (beta ? "5" : "10")) + " EUR",
                                    text = "§f" + d.getName() + " - §c" + ms.getNumberFormat((int) ply[1]) + cashreward;
                            holograms.put("TopPvP" + step, ms.getHolograms().addHologram("TopPvP" + step, (lastPos = lastPos.add(0, -0.4, 0)).clone(), text));
                        }
                        return;
                    }
                    hologram.setMessage("§c§lTop PvP");
                    for (byte step = 0; step < top3Trophs.size(); step++) {
                        final Object[] ply = top3Trophs.get(step);
                        if (ply == null) continue;
                        final MuxHolograms.Hologram holo = holograms.get("TopPvP" + step);
                        if (holo != null) {
                            final OfflinePlayer d = ms.getServer().getOfflinePlayer((UUID) ply[0]);
                            final boolean beta = ms.isBeta();
                            final String cashreward = " §a➡ §l" + (step == 0 ? (beta ? "25" : "50") : step == 1 ? (beta ? "10" : "20") : (beta ? "5" : "10")) + " EUR",
                                    text = "§f" + d.getName() + " - §c" + ms.getNumberFormat((int) ply[1]) + cashreward;
                            holo.setMessage(text);
                        } else {
                            holograms.values().forEach(MuxHolograms.Hologram::remove);
                            hologram.remove();
                            ms.getHolograms().removeHologramsWithName("TopPvP");
                            holograms.clear();
                            hologram = null;
                            refreshRankingHologram();
                            break;
                        }
                    }
                }
            }
        }.runTask(ms);
    }

    public void checkTop() {
        final long winningTime = System.currentTimeMillis();
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        calendar.setTime(new Date(winningTime - 300000L));
        for (final Player player : ms.getServer().getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENDERDRAGON_DEATH, 1F, 0.3F);
        }
        ms.broadcastMessage("§c", null, MuxSystem.Priority.HIGHEST);
        ms.broadcastMessage("§c§lPvP Gewinner des Monats §f§l" + calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.GERMANY) + " " + calendar.get(Calendar.YEAR), null, MuxSystem.Priority.HIGHEST);
        Location lastPos = new Location(ms.getGameSpawn().getWorld(), 21D, 80D, -7D);
        ms.getHolograms().addHologram("PvPWin", lastPos, "§c§lPvP Gewinner im " + calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.GERMANY));
        final List<Tuple<UUID, Integer>> topPlayers = new ArrayList<>();
        final Map<Integer, UUID> rankings = new HashMap<>();
        for (int pos = 0; pos < 3; pos++) {
            final Map<UUID, RankingEntry> entries = rankingEntries.get(pos);
            if (entries != null) {
                final int finalPos = pos;
                final Tuple<RankingEntry, String> tuple = entries.values().stream().map(rankingEntry -> {
                            RankingEntry entry1;
                            if (top3Last8Hours.get(finalPos).equals(rankingEntry.uuid)) {
                                entry1 = new RankingEntry(rankingEntry.getUUID(), rankingEntry.isActive() ? rankingEntry.getCurrentStart() : 0,
                                        rankingEntry.isActive() ? (rankingEntry.getTotalTime() + (System.currentTimeMillis() - rankingEntry.getCurrentStart())) : rankingEntry.getTotalTime());
                                entry1.setActive(rankingEntry.isActive());
                                entry1.setTrophs(rankingEntry.getTrophs());
                            } else entry1 = rankingEntry;
                            return entry1;
                        })
                        .filter(rankingEntry -> rankings.containsKey(finalPos - 1) == false || rankings.get(finalPos - 1).equals(rankingEntry.getUUID()) == false)
                        .map(rankingEntry -> new Tuple<>(rankingEntry, ms.getServer().getOfflinePlayer(rankingEntry.getUUID()).getName()))
                        .max(Comparator.comparingLong(value -> value.a().getTotalTime())).orElse(null);
                if (tuple == null) continue;
                final RankingEntry rankingEntry = tuple.a();
                final String name = tuple.b();
                if (name == null) continue;
                rankings.put(finalPos, rankingEntry.getUUID());
                final boolean beta = ms.isBeta();
                final String cashreward = " §a➡ §l" + (pos == 0 ? (beta ? "25" : "50") : pos == 1 ? (beta ? "10" : "20") : (beta ? "5" : "10")) + " EUR",
                        text = "§f" + name + " - §c" + ms.getNumberFormat(rankingEntry.getTrophs()) + cashreward,
                        chattext = "  §c" + ms.getNumberFormat(rankingEntry.getTrophs()) + " Trophäen - §f" + name + cashreward;
                topPlayers.add(new Tuple<>(rankingEntry.getUUID(), rankingEntry.getTrophs()));
                holograms.put("PvPWin" + pos, ms.getHolograms().addHologram("PvPWin" + pos, (lastPos = lastPos.clone().add(0, -0.4, 0)), text));
                ms.broadcastMessage(chattext, null, MuxSystem.Priority.HIGHEST);
            }
        }
        ms.getHashYAML().set("PvPWinsDate", winningTime);
        ms.getHashYAML().set("PvPWins", new Gson().toJson(topPlayers));
    }

    private void calculateTop(final int pos, final Object[] object) {
        final UUID player = (UUID) object[0];
        final int trophs = (int) object[1];
        rankingEntries.putIfAbsent(pos, new HashMap<>());
        RankingEntry current = null;
        final UUID top3player = top3Last8Hours.get(pos);
        if (top3player != null) {
            current = rankingEntries.get(pos).get(top3player);
        }
        if (current == null) {
            final RankingEntry entry = new RankingEntry(player, System.currentTimeMillis(), 0L);
            top3Last8Hours.put(pos, player);
            rankingEntries.get(pos).put(player, entry);
            entry.setTrophs(trophs);
        } else if (current.getUUID().equals(player) == false) {
            RankingEntry entry = rankingEntries.get(pos).get(player);
            if (entry != null) {
                entry.setCurrentStart(System.currentTimeMillis());
                current.setTotalTime(current.getTotalTime() + (System.currentTimeMillis() - current.getCurrentStart()));
                current.setCurrentStart(0);
                current.setActive(false);
                top3Last8Hours.put(pos, player);
            } else {
                entry = new RankingEntry(player, System.currentTimeMillis(), 0L);
                current.setTotalTime(current.getTotalTime() + (System.currentTimeMillis() - current.getCurrentStart()));
                current.setCurrentStart(0);
                current.setActive(false);
                rankingEntries.get(pos).put(player, entry);
                top3Last8Hours.put(pos, player);
                entry.setTrophs(trophs);
            }
        } else {
            current.setActive(true);
            if (current.getTrophs() < trophs)
                current.setTrophs(trophs);
        }
    }

    class RankingEntry {
        private final UUID uuid;
        private long currentStart, totalTime;
        private int trophs;
        private boolean active = true;

        RankingEntry(final UUID uuid, final long currentStart, final long totalTime) {
            this.uuid = uuid;
            this.currentStart = currentStart;
            this.totalTime = totalTime;
        }

        public void setTrophs(final int trophs) {
            this.trophs = trophs;
        }

        public int getTrophs() {
            return trophs;
        }

        public void setActive(final boolean active) {
            this.active = active;
        }

        public boolean isActive() {
            return active;
        }

        public UUID getUUID() {
            return uuid;
        }

        public long getCurrentStart() {
            return currentStart;
        }

        public long getTotalTime() {
            return totalTime;
        }

        public void setCurrentStart(final long currentStart) {
            this.currentStart = currentStart;
        }

        public void setTotalTime(final long totalTime) {
            this.totalTime = totalTime;
        }

        @Override
        public String toString() {
            return "RankingEntry{" +
                    "uuid=" + uuid +
                    ", currentStart=" + currentStart +
                    ", trophs=" + trophs +
                    ", totalTime=" + totalTime +
                    ", active=" + active +
                    '}';
        }
    }

    public void onCloseInv(final Player p) {
        pvpinvs.remove(p.getUniqueId());
    }

    public boolean handleCommand(final Player p, final String[] args, final String label) {
        if (inDuel(p) && p.isOp() == false) {
            ms.showItemBar(p, "§cDu bist bereits in einem Duell.");
            return true;
        } else if (in1vs1(p) && p.isOp() == false) {
            ms.showItemBar(p, "§cDu bist bereits in einem 1vs1.");
        }
        if (label.equalsIgnoreCase("zuschauen") || label.equalsIgnoreCase("spectate")) {
            openSpectateInv(p);
            return true;
        } else if (args.length == 0) {
            if (label.equals("pvp") || label.equals("ranking")) {
                openStatsInv(p);
                return true;
            } else if (label.equalsIgnoreCase("1vs1")) {
                joinRankedQueue(p);
                return true;
            } else if (label.equals("kills")) {
                openKills(p);
                return true;
            }
            ms.showItemBar(p, ms.usage("/duel [spieler]"));
            return true;
        } else if (label.equalsIgnoreCase("1vs1")) {
            if (args.length == 2 && args[0].equals("spectate")) {
                final String map = args[1].toLowerCase();
                for (final RankedArena a : rankedArenas) {
                    if (a.getMap().toLowerCase().equals(map)) {
                        a.spectate(p);
                        return true;
                    }
                }
                ms.showItemBar(p, "§cDieses 1vs1 läuft nicht mehr.");
                return true;
            } else if (args.length == 1 && args[0].equals("leave")) {
                rankedQueue.remove(p);
                ms.showItemBar(p, "§fDu hast die Warteschlange §cverlassen§f.");
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                return true;
            } else if (args.length == 2 && args[0].equals("unlock")) {
                if (p.hasMetadata("1vs1unlock")) {
                    int i = -1;
                    try {
                        i = Integer.parseInt(args[1]);
                    } catch (final NumberFormatException ignored) {
                    }
                    if (p.getMetadata("1vs1unlock").get(0).asInt() == i) {
                        p.removeMetadata("1vs1unlock", ms);
                        rankedBookCache.add(p);
                        joinRankedQueue(p);
                        return true;
                    }
                }
            }
            if (p.isOp() == false) {
                joinRankedQueue(p);
                return true;
            } else if (ms.isTrusted(p.getName()) == false) {
                p.sendMessage(new String[]{
                        "§c1vs1 Arenen: §7" + (rankedMaps.isEmpty() ? "Noch keine" : String.join(", ", rankedMaps)),
                });
                return true;
            } else if (args.length == 2 && args[0].equalsIgnoreCase("create")) {
                if (rankedMaps.stream().anyMatch(s -> s.equalsIgnoreCase(args[1]))) {
                    ms.showItemBar(p, "§cDie Arena existiert bereits.");
                    return true;
                }
                rankedMaps.add(args[1]);
                ms.getConfig().set("rankedmaps", rankedMaps);
                ms.saveConfig();
                ms.showItemBar(p, "§fDie Arena wurde §aerstellt§f. Setze nun die Positionen.");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
                if (rankedMaps.contains(args[1]) == false) {
                    ms.showItemBar(p, "§cDie Arena existiert nicht. Achte auf Groß & Kleinschreibung.");
                    return true;
                }
                maps.remove(args[1]);
                ms.getConfig().set("rankedmaps", maps);
                ms.saveConfig();
                ms.showItemBar(p, "§fDie Arena wurde §centfernt§f.");
                return true;
            } else if (args.length == 3 && args[0].equalsIgnoreCase("setgate")) {
                if (rankedMaps.contains(args[1]) == false) {
                    ms.showItemBar(p, "§cDie Arena existiert nicht. Achte auf Groß & Kleinschreibung.");
                    return true;
                }
                if ((args[2].equals("1") || args[2].equals("2")) == false) {
                    ms.showItemBar(p, "§cGebe einen gültigen Torpunkt an. (1 oder 2)");
                    return true;
                }
                ms.setLoc("ranked" + args[1].toLowerCase() + "gate" + args[2], p.getLocation());
                ms.saveLocs(p);
                ms.showItemBar(p, "§fDas Tor §6" + args[2] + " §fin Arena §6" + args[1] + " §fwurde §agesetzt§f.");
                return true;
            } else if (args.length == 3 && args[0].equalsIgnoreCase("setspawn")) {
                if (rankedMaps.contains(args[1]) == false) {
                    ms.showItemBar(p, "§cDie Arena existiert nicht. Achte auf Groß & Kleinschreibung.");
                    return true;
                }
                if ((args[2].equals("1") || args[2].equals("2")) == false) {
                    ms.showItemBar(p, "§cGebe einen gültigen Spawnpunkt an. (1 oder 2)");
                    return true;
                }
                ms.setLoc("ranked" + args[1].toLowerCase() + "spawn" + args[2], p.getLocation());
                ms.saveLocs(p);
                ms.showItemBar(p, "§fDer Spawnpunkt §6" + args[2] + " §fin Arena §6" + args[1] + " §fwurde §agesetzt§f.");
                return true;
            } else if (args.length == 2 && args[0].equalsIgnoreCase("setspec")) {
                if (rankedMaps.contains(args[1]) == false) {
                    ms.showItemBar(p, "§cDie Arena existiert nicht. Achte auf Groß & Kleinschreibung.");
                    return true;
                }
                ms.setLoc("ranked" + args[1].toLowerCase() + "spectate", p.getLocation());
                ms.saveLocs(p);
                ms.showItemBar(p, "§fDer Zuschauerplatz §fin Arena §6" + args[1] + " §fwurde §agesetzt§f.");
            } else {
                p.sendMessage(new String[]{
                        "  §61vs1 Admin Befehle:",
                        "  §c/1vs1 [create | remove] [map]§7: Arena erstellen",
                        "  §c/1vs1 setspawn [map] [1 | 2]§7: Spawn setzen",
                        "  §c/1vs1 setgate [map] [1 | 2]§7: Tor setzen",
                        "  §c/1vs1 setspec [map]§7: Zuschauerplatz setzen",
                        "  §cArenen: §7" + (rankedMaps.isEmpty() ? "Noch keine" : String.join(", ", rankedMaps)),
                        ""
                });
            }
            return true;
        } else if (args.length == 1 && args[0].equals("help") == false) {
            if (isDuelEnabled() == false && p.isOp() == false) {
                ms.showItemBar(p, "§cDuell ist derzeit nicht verfügbar.");
                return true;
            } else if (ms.getAdmin().pvpIsActive() == false && p.isOp() == false) {
                ms.showItemBar(p, "§cPvP ist momentan deaktiviert.");
                return true;
            }
            final Settings s = ms.getMuxUser(p.getName()).getSettings();
            final Player to = ms.getServer().getPlayer(ms.fromArgs(args, 0));
            if (to == null || p.canSee(to) == false) {
                ms.showItemBar(p, ms.hnotonline);
                return true;
            }
            final MuxUser u = ms.getMuxUser(p.getName()), u2 = ms.getMuxUser(to.getName());
            if (p.getName().equals(to.getName()) || u2.getIp().equals(u.getIp())) {
                ms.showItemBar(p, "§cDu kannst dich nicht selbst einladen.");
                return true;
            } else if (u.getCoins() < s.getEntryCost()) {
                ms.showItemBar(p, "§cDu besitzt nicht genügend MuxCoins.");
                return true;
            } else if (ms.getCreators().isRecording(to)) {
                ms.showItemBar(p, "§cDer Spieler kann gerade keine Anfragen erhalten.");
                return true;
            } else if (label.equals("fduel") == false) {
                p.setMetadata("dcmd", new FixedMetadataValue(ms, to.getName()));
                openDuelInv(p, 0, 0);
                return true;
            } else if (ms.checkGeneralCooldown(p.getName(), "DUEL", 60000L, false)) {
                ms.showItemBar(p, "§cWarte etwas um eine weitere Anfrage zu schicken.");
                return true;
            } else if (p.hasPermission("muxsystem.vip") == false && ms.isVPN(p.getAddress().getAddress().getHostAddress()) == 1) {
                ms.sendNoVPNMessage(p, "VIP");
                return true;
            }
            if (p.hasMetadata("dcmd") == false && p.hasMetadata("sfriends") == false && p.hasMetadata("spmenu") == false && p.hasMetadata("sclans") == false) {
                p.setMetadata("dcmd", new FixedMetadataValue(ms, to.getName()));
                openDuelInv(p, 0, 0);
                return true;
            }
            final String pname = p.getName();
            final boolean friend = true;
            requestDuel(p, s);
            ms.checkGeneralCooldown(p.getName(), "DUEL", 60000L, true);
            ms.showItemBar(p, friend ? "§fDie Freundschaftskampf Anfrage wurde §agesendet§f." : "§fDie Duell Anfrage wurde §agesendet§f.");
            ms.chatClickHoverRun(to, "§6§lDuell>§7 Der Spieler §6" + pname + " " + (friend ? "§7will ein Freundschaftsduell." : "§7möchte dich duellieren."), "§6§lDuell gegen " + pname + "\n§7Bodyfix: " + (s.hasBodyFix() ? "§aaktiviert" : "§cdeaktiviert") + "\n§7OP-Äpfel: " + (s.getOPs() == 0 ? "§cdeaktiviert" : "§f" + s.getOPs() + " Stück") + "\n§7Heiltränke: §f" + s.getHeals() + " Stack" + (s.getHeals() > 1 ? "s" : "") + "\n§7Beitrittskosten: §e" + (s.getEntryCost() == 0 ? "keine" : s.getEntryCost() + " MuxCoins") + "\n\n" + "§6§oKlicke, um anzunehmen.", "/duel accept " + p.getName() + (friend ? " f" : ""));
            ms.chatClickHoverRun(to, "§6§lDuell>§7 Klicke §6hier§7, um die Herausforderung anzunehmen.", "§6§lDuell gegen " + pname + "\n§7Bodyfix: " + (s.hasBodyFix() ? "§aaktiviert" : "§cdeaktiviert") + "\n§7OP-Äpfel: " + (s.getOPs() == 0 ? "§cdeaktiviert" : "§f" + s.getOPs() + " Stück") + "\n§7Heiltränke: §f" + s.getHeals() + " Stack" + (s.getHeals() > 1 ? "s" : "") + "\n§7Beitrittskosten: §e" + (s.getEntryCost() == 0 ? "keine" : s.getEntryCost() + " MuxCoins") + "\n\n" + "§6§oKlicke, um anzunehmen.", "/duel accept " + p.getName() + (friend ? " f" : ""));
            to.playSound(to.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            return true;
        } else if (args[0].equals("accept")) {
            if (ms.getAdmin().DUELS.isActive() == false && p.isOp() == false) {
                ms.showItemBar(p, "§cDuell ist derzeit nicht verfügbar.");
                return true;
            }
            final Player pl = ms.getServer().getPlayer(args[1]);
            if (pl == null) {
                ms.showItemBar(p, "§cDer Spieler ist offline gegangen.");
                return true;
            }
            if (p.hasPermission("muxsystem.vip") == false && ms.isVPN(p.getAddress().getAddress().getHostAddress()) == 1) {
                ms.sendNoVPNMessage(p, "VIP");
                return true;
            }
            acceptDuel(pl, p, args.length == 3 && args[2].contains("c"), true);
            return true;
        } else if (args[0].equals("spectate")) {
            final String map = args[1].toLowerCase();
            for (final Arena a : arenas) {
                if (a.getMap().toLowerCase().equals(map)) {
                    a.spectate(p);
                    return true;
                }
            }
            ms.showItemBar(p, "§cDieses Duell läuft nicht mehr.");
            return true;
        } else if (p.isOp() == false) {
            return true;
        } else if (ms.isTrusted(p.getName()) == false) {
            p.sendMessage(new String[]{
                    "§cDuel Arenen: §7" + (maps.isEmpty() ? "Noch keine" : String.join(", ", maps)),
            });
            return true;
        } else if (args.length == 2 && args[0].equals("create")) {
            if (maps.stream().anyMatch(s -> s.equalsIgnoreCase(args[1]))) {
                ms.showItemBar(p, "§cDie Arena existiert bereits.");
                return true;
            }
            maps.add(args[1]);
            ms.getConfig().set("duelmaps", maps);
            ms.saveConfig();
            ms.showItemBar(p, "§fDie Arena wurde §aerstellt§f. Setze nun die Positionen.");
            return true;
        } else if (args.length < 2) {
            p.sendMessage(new String[]{
                    "  §6Duel Admin Befehle:",
                    "  §c/duel [create | remove] [map]§7: Arena erstellen",
                    "  §c/duel setspawn [map] [1 | 2]§7: Spawn setzen",
                    "  §c/duel setgate [map] [1 | 2]§7: Tor setzen",
                    "  §c/duel setspec [map]§7: Zuschauerplatz setzen",
                    "  §cArenen: §7" + (maps.isEmpty() ? "Noch keine" : String.join(", ", maps)),
                    ""
            });
            return true;
        }
        if (maps.contains(args[1]) == false) {
            ms.showItemBar(p, "§cDie Arena existiert nicht. Achte auf Groß & Kleinschreibung.");
            return true;
        } else if (args[0].equals("remove")) {
            maps.remove(args[1]);
            ms.getConfig().set("duelmaps", maps);
            ms.saveConfig();
            ms.showItemBar(p, "§fDie Arena wurde §centfernt§f.");
            return true;
        } else if (args[0].equals("setspec")) {
            ms.setLoc("duel" + args[1].toLowerCase() + "spectate", p.getLocation());
            ms.saveLocs(p);
            ms.showItemBar(p, "§fDer Zuschauerplatz §fin Arena §6" + args[1] + " §fwurde §agesetzt§f.");
        } else if (args.length < 3) {
            return true;
        } else if (args[0].equals("setspawn")) {
            if ((args[2].equals("1") || args[2].equals("2")) == false) {
                ms.showItemBar(p, "§cGebe einen gültigen Spawnpunkt an. (1 oder 2)");
                return true;
            }
            ms.setLoc("duel" + args[1].toLowerCase() + "spawn" + args[2], p.getLocation());
            ms.saveLocs(p);
            ms.showItemBar(p, "§fDer Spawnpunkt §6" + args[2] + " §fin Arena §6" + args[1] + " §fwurde §agesetzt§f.");
        } else if (args[0].equals("setgate")) {
            if ((args[2].equals("1") || args[2].equals("2")) == false) {
                ms.showItemBar(p, "§cGebe einen gültigen Torpunkt an. (1 oder 2)");
                return true;
            }
            ms.setLoc("duel" + args[1].toLowerCase() + "gate" + args[2], p.getLocation());
            ms.saveLocs(p);
            ms.showItemBar(p, "§fDas Tor §6" + args[2] + " §fin Arena §6" + args[1] + " §fwurde §agesetzt§f.");
        }
        return true;
    }

    public void handleInventory(final Player p, final ItemStack i, final Inventory inv) {
        final Material m = i.getType();
        final ItemMeta im = i.getItemMeta();
        final MuxUser u = ms.getMuxUser(p.getName());
        final Settings s = u.getSettings();
        if (m == Material.EYE_OF_ENDER && ms.hasGlow(i) == false && im.getDisplayName().contains("Liga") == false) {
            openSpectateInv(p);
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        } else if (m == Material.GOLD_INGOT && ms.hasGlow(i) == false) {
            openStatsInv(p);
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        } else if (m == Material.COMPASS) {
            new MuxAnvil(ms, ((input, p1) -> {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                String plname = ms.retardCleaner(input, "Spielername: ");
                ms.getProfiles().showFullPlayerProfile(p, plname, "Stats");
            })).show(p, "Spielername: ", new ItemStack(Material.SKULL_ITEM, 1, (byte) 3));
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        } else if (m == Material.ARMOR_STAND && im.getLore().size() > 2) {
            openLeagueInv(p);
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        } else if (m == Material.SKULL_ITEM) {
            if (inv.getTitle().endsWith("/zuschauen")) {
                final String displayName = i.getItemMeta().getDisplayName();
                if (displayName == null) return;
                if (displayName.startsWith("§3§l")) {
                    final String clan = ChatColor.stripColor(displayName).split(" ")[0];
                    for (final MuxClanWar.Arena arena : ms.getClanWar().getArenas()) {
                        if (arena.getClanNameA().equals(clan) || arena.getClanNameB().equals(clan)) {
                            p.performCommand("cw spectate " + arena.getMap());
                            return;
                        }
                    }
                } else if (displayName.startsWith("§6§l")) {
                    final String playerName = ChatColor.stripColor(displayName).split(" ")[0];
                    for (final Arena arena : arenas) {
                        if (arena.playerA == null || arena.playerB == null) continue;
                        if (arena.playerA.getName().equals(playerName) || arena.playerB.getName().equals(playerName)) {
                            p.performCommand("duel spectate " + arena.getMap());
                            return;
                        }
                    }
                } else if (displayName.startsWith("§c§l")) {
                    final String playerName = ChatColor.stripColor(displayName).split(" ")[0];
                    for (final RankedArena arena : rankedArenas) {
                        if (arena.playerA == null || arena.playerB == null) continue;
                        if (arena.playerA.getName().equals(playerName) || arena.playerB.getName().equals(playerName)) {
                            p.performCommand("1vs1 spectate " + arena.getMap());
                            return;
                        }
                    }
                }
            } else {
                final String back = (inv.getTitle().contains("kills")) ? "Kills" : "Stats";
                ms.getProfiles().showFullPlayerProfile(p, i.getItemMeta().getDisplayName().split(" ")[1], back);
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            }
        } else if (m == Material.STAINED_CLAY) {
            if (i.getDurability() == 4) {
                final String map = ChatColor.stripColor(i.getItemMeta().getLore().get(1));
                p.performCommand("duel spectate " + map);
            } else if (i.getDurability() == 9) {
                final String map = ChatColor.stripColor(i.getItemMeta().getLore().get(1));
                p.performCommand("cw spectate " + map);
            } else if (i.getDurability() == 1) {
                final RankedArena a = get1vs1Arena(ms.getServer().getPlayer(ChatColor.stripColor(i.getItemMeta().getLore().get(0).split(" ")[0])));
                if (a == null) return;
                p.performCommand("1vs1 spectate " + a.getMap());
            } else if (i.getDurability() == 5) {
                p.closeInventory();
                if (p.hasMetadata("dcmd") == false && p.hasMetadata("sfriends") == false && p.hasMetadata("spmenu") == false && p.hasMetadata("sclans") == false)
                    return;
                final String plname = p.hasMetadata("sfriends") ? p.getMetadata("sfriends").get(0).asString() : p.hasMetadata("spmenu") ? p.getMetadata("spmenu").get(0).asString() :
                        p.hasMetadata("sclans") ? p.getMetadata("sclans").get(0).asString() : p.hasMetadata("dcmd") ? p.getMetadata("dcmd").get(0).asString() : null;
                if (plname == null) return;
                p.performCommand("fduel " + plname);
                p.removeMetadata("sfriends", ms);
                p.removeMetadata("spmenu", ms);
                p.removeMetadata("sclans", ms);
                p.removeMetadata("dcmd", ms);
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            }
        } else if (m == Material.DOUBLE_PLANT) {
            new MuxAnvil(ms, (input, player) -> {
                int coins;
                try {
                    coins = Integer.parseInt(ms.removeNonDigits(input));
                } catch (final NumberFormatException ex) {
                    ms.showItemBar(player, ms.getLang("shop.notvalid"));
                    return;
                }
                if (coins < 0) {
                    ms.showItemBar(player, "§cDer Betrag kann nicht negativ sein.");
                    return;
                } else if (ms.getMuxUser(player.getName()).getCoins() < coins) {
                    ms.showItemBar(player, "§cDu besitzt nicht genügend MuxCoins.");
                    return;
                }
                s.setEntryCost(coins);
                openDuelInv(player, 0, p.hasMetadata("spmenu") ? 0 : p.hasMetadata("sfriends") ? 1 : 2);
                player.playSound(player.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            }).show(p, "MuxCoins: " + s.getEntryCost(), new ItemStack(Material.DOUBLE_PLANT));
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        } else if (m == Material.CHAINMAIL_CHESTPLATE) {
            s.setBodyFix(s.hasBodyFix() ^ true);
            openDuelInv(p, 0, p.hasMetadata("spmenu") ? 0 : p.hasMetadata("sfriends") ? 1 : 2);
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
        } else if (m == Material.GOLDEN_APPLE) {
            if (s.getActivePage() == 1) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            openDuelInv(p, 1, p.hasMetadata("spmenu") ? 0 : p.hasMetadata("sfriends") ? 1 : 2);
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        } else if (m == Material.POTION) {
            if (i.getDurability() == 16421) {
                if (s.getActivePage() == 2) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                openDuelInv(p, 2, p.hasMetadata("spmenu") ? 0 : p.hasMetadata("sfriends") ? 1 : 2);
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            } else {
                s.setHeals(i.getAmount());
                openDuelInv(p, 0, p.hasMetadata("spmenu") ? 0 : p.hasMetadata("sfriends") ? 1 : 2);
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            }
        } else if (m == Material.APPLE) {
            s.setOPs(i.getAmount());
            openDuelInv(p, 0, p.hasMetadata("spmenu") ? 0 : p.hasMetadata("sfriends") ? 1 : 2);
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
        } else if (m == Material.BARRIER) {
            if (s.getActivePage() == 1) {
                s.setOPs(0);
                openDuelInv(p, 0, p.hasMetadata("spmenu") ? 0 : p.hasMetadata("sfriends") ? 1 : 2);
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            }
        } else if (m == Material.ITEM_FRAME) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            if (im.getDisplayName().contains("Einstellungen")) {
                openDuelInv(p, 0, p.hasMetadata("spmenu") ? 0 : p.hasMetadata("sfriends") ? 1 : 2);
            } else if (im.getDisplayName().contains("Freund")) {
                ms.getFriends().handleCommand(p, p.hasMetadata("sfriends") ? new String[]{p.getMetadata("sfriends").get(0).asString()} : new String[]{});
            } else if (im.getDisplayName().contains("Clan")) {
                final String plname = p.hasMetadata("sclans") ? p.getMetadata("sclans").get(0).asString() : null;
                if (plname == null) return;
                final OfflinePlayer op = ms.getPlayer(plname);
                if (op == null) return;
                ms.getClans().openPlayerMenu(p, op);
            } else if (im.getDisplayName().contains("Spieler")) {
                Player pl = null;
                if (p.hasMetadata("pmenu")) {
                    pl = (Player) p.getMetadata("pmenu").get(0).value();
                }
                if (pl != null)
                    ms.getInv().openPlayerMenu(p, pl);
                else
                    p.closeInventory();
            } else
                openStatsInv(p);
        } else {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
        }
    }

    private void setup() {
        pvpinv = ms.getServer().createInventory(null, 45, "§0§lMuxPvP§0 | /zuschauen");
        pvpinv.setItem(1, ms.addGlow(ms.renameItemStack(new ItemStack(Material.EYE_OF_ENDER, Math.max(fightsrunning, 1)), "§4§lZuschauen", "§7Die Liste enthält alle aktiven Kämpfe", "§7denen du in Ruhe zuschauen kannst.")));
        pvpinv.setItem(4, ms.renameItemStack(new ItemStack(Material.GOLD_INGOT), "§4§lTop Kämpfer", "§7Sortiert nach der Anzahl der Trophäen", "§7befinden sich hier die besten Kämpfer.", "", "§7Klicke§7, um die §4Top Kämpfer zu sehen§7."));
        pvpinv.setItem(7, ms.renameItemStack(new ItemStack(Material.COMPASS), "§4§lProfil suchen", "§7Betrachte mit dieser Funktion die", "§7Stats eines beliebigen Spielers.", "", "§7Klicke§7, um ein §4Profil zu suchen§7."));
        pvpsb = ms.getScoreboard().createScoreboard("§7§lMux§6§lDuell");
        pvpsb.setLook((p, board) -> {
            pvpsb.setLine(board, " ");
            int lowestpcnt = 100;
            short armorcount = 0;
            for (final ItemStack armor : p.getInventory().getArmorContents()) {
                if (armor != null && armor.getType() != Material.AIR && ms.isArmor(armor.getType())) {
                    armorcount++;
                    int durability = 100 - (int) (((double) armor.getDurability() / (double) armor.getType().getMaxDurability()) * 100D);
                    if (durability < lowestpcnt) lowestpcnt = durability;
                }
            }
            pvpsb.setSection(board, "§3§lRüstung     ", (armorcount == 0 ? "Keine" : armorcount < 4 ? "Unvollständig" : "Zustand: " + (lowestpcnt < 15 ? "§c" : "") + lowestpcnt + "%"), false);
            pvpsb.setLine(board, "  ");
            final Arena a = getArena(p);
            if (a != null) {
                pvpsb.setSection(board, "§e§lPreis", a.getEntryCost() == 0 ? "Keinen" : ms.getNumberFormat(a.getEntryCost() * 2L) + " MuxCoins", false);
                pvpsb.setLine(board, "   ");
                final Player pl = (a.isPlayerA(p) ? a.getPlayerB() : a.getPlayerA());
                String league = "";
                if (pl != null) {
                    final MuxUser u2 = ms.getMuxUser(pl.getName());
                    final String l = ms.getStats().getLeague(u2.getTrophies(), u2.getLastRankedMatch() > System.currentTimeMillis() - 172800000, u2.getLastRankedMatch() != 0).replace("Unranked", "");
                    if (l.isEmpty() == false) league = " (" + l + ")";
                }
                pvpsb.setSection(board, "§4§lGegner", pl == null ? "Keinen" : pl.getName() + (league.isEmpty() ? " " : league), false);
            }
        });
        rankedsb = ms.getScoreboard().createScoreboard("§7§lMux§c§l1vs1");
        rankedsb.setLook((p, board) -> {
            rankedsb.setLine(board, " ");
            int lowestpcnt = 100;
            short armorcount = 0;
            for (final ItemStack armor : p.getInventory().getArmorContents()) {
                if (armor != null && armor.getType() != Material.AIR && ms.isArmor(armor.getType())) {
                    armorcount++;
                    int durability = 100 - (int) (((double) armor.getDurability() / (double) armor.getType().getMaxDurability()) * 100D);
                    if (durability < lowestpcnt) lowestpcnt = durability;
                }
            }
            rankedsb.setSection(board, "§3§lRüstung     ", (armorcount == 0 ? "Keine" : armorcount < 4 ? "Unvollständig" : "Zustand: " + (lowestpcnt < 15 ? "§c" : "") + lowestpcnt + "%"), false);
            rankedsb.setLine(board, "  ");
            final RankedArena rA = get1vs1Arena(p);
            if (rA != null) {
                final Player pl = (rA.isPlayerA(p) ? rA.playerB : rA.playerA);
                String league = "";
                if (pl != null) {
                    final MuxUser u2 = ms.getMuxUser(pl.getName());
                    final String l = ms.getStats().getLeague(u2.getTrophies(), u2.getLastRankedMatch() > System.currentTimeMillis() - 172800000, u2.getLastRankedMatch() != 0).replace("Unranked", "");
                    if (l.isEmpty() == false) league = " (" + l + ")";
                }
                rankedsb.setSection(board, "§4§lGegner", pl == null ? "Keinen" : pl.getName() + (league.isEmpty() ? " " : league), false);
                rankedsb.setLine(board, "    ");
                final int killtrophs = pl == null ? 0 : ms.getStats().getKillTrophies(pl, p);
                rankedsb.setSection(board, "§a§lGewinn", killtrophs + " Trophäen", false);
                if (rA.getKit() != null) {
                    rankedsb.setLine(board, "     ");
                    rankedsb.setSection(board, "§d§lKit", rA.getKit().name, false);
                }
            }
        });
        Arrays.stream(EnumRankedKitRarity.values()).forEach(rarity -> rankedKits.put(rarity, new ArrayList<>()));
        // rankedKits.put( EnumRankedKitRarity.COMMON, new ArrayList<>());
        new BukkitRunnable() {
            @Override
            public void run() {
                for (final Class<?> aClass : MuxPvP.class.getDeclaredClasses()) {
                    if (aClass.getSuperclass() == null) return;
                    if (aClass.getSuperclass() == RankedKit.class && aClass != RankedKit.class) {
                        try {
                            Constructor<?> constructor = Arrays.stream(aClass.getConstructors()).filter(constructor1 -> constructor1.getParameterCount() == 1).
                                    findFirst().orElse(null);
                            if (constructor == null) return;
                            constructor.newInstance(MuxPvP.this);
                        } catch (final InvocationTargetException | InstantiationException | IllegalAccessException e) {
                            System.out.println("MuxSystem> Could not load Kit " + aClass.getName());
                        }
                    }
                }
            }
        }.runTask(ms);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (final RankedArena arena : rankedArenas) {
                    arena.tick();
                }
            }
        }.runTaskTimer(ms, 20L, 1L);
        new BukkitRunnable() {
            @Override
            public void run() {
                refreshRankingHologram();
            }
        }.runTaskTimerAsynchronously(ms, 30L, 300L);
        this.rankedTimes = new LocalDateTime[2];
        this.rankedTimes[0] = LocalDateTime.now().withHour(14).withMinute(0);
        this.rankedTimes[1] = LocalDateTime.now().withHour(22).withMinute(0);
    }

    private void updateSpectateInv() {
        final Map<Integer, ItemStack> items = new HashMap<>();
        int i = 18;
        for (final MuxClanWar.Arena a : ms.getClanWar().getArenas().stream().sorted(Comparator.comparingInt(MuxClanWar.Arena::getEntryCost)).collect(Collectors.toList())) {
            if (i >= 45) break;
            if (a.isFinished()) continue;
            items.put(i, ms.renameItemStack(cyanHead.clone(), "§3§l" + a.getClanNameB() + "§7 gegen §3§l" + a.getClanNameA(), "§7Bodyfix: " + (a.hasBodyFix() ? "§aaktiviert" : "§cdeaktiviert"), "§7OP-Äpfel: " + (a.hasOPs() ? "§aaktiviert" : "§cdeaktiviert"), "§7Heiltränke: §f" + a.getHeals() + " Stack" + (a.getHeals() > 1 ? "s" : ""), "§7Einsatz: §e" + ms.getNumberFormat(a.getEntryCost()) + " MuxCoins", "", "§7Klicke, um den Clanwar zu §3sehen§7."));
            i++;
        }
        for (final Arena a : arenas.stream().sorted(Comparator.comparingInt(Arena::getEntryCost)).collect(Collectors.toList())) {
            if (i >= 45) break;
            if (a.isFinished()) continue;
            items.put(i, ms.renameItemStack(orangeHead.clone(), "§6§l" + a.getPlayerA().getName() + "§7 gegen §6§l" + a.getPlayerB().getName(), "§7Bodyfix: " + (a.hasBodyFix() ? "§aaktiviert" : "§cdeaktiviert"), "§7OP-Äpfel: " + (a.getOPs() == 0 ? "§cdeaktiviert" : "§f" + a.getOPs() + " Stück"), "§7Heiltränke: §f" + a.getHeals() + " Stack" + (a.getHeals() > 1 ? "s" : ""), "§7Gewinn: §e" + (a.getEntryCost() == 0 ? "keinen" : ms.getNumberFormat(a.getEntryCost()) + " MuxCoins"), "", "§7Klicke, um das Duell zu §6sehen§7."));
            i++;
        }
        for (final RankedArena a : rankedArenas.stream().sorted(Comparator.comparingInt(arena -> {
            if (arena.playerA == null || arena.playerB == null) return -1;
            final MuxUser u1 = ms.getMuxUser(arena.playerA.getName()), u2 = ms.getMuxUser(arena.playerB.getName());
            if (u1 == null || u2 == null) return -1;
            return (u1.getTrophies() + u2.getTrophies()) / 2;
        })).collect(Collectors.toList())) {
            if (i >= 45) break;
            if (a.playerB == null || a.playerA == null || a.isFinished()) continue;
            items.put(i, ms.renameItemStack(darkRedHead.clone(), "§c§l" + a.playerA.getName() + "§7 gegen §c§l" + a.playerB.getName(), "§7Kit: §a" + a.getKit().name, "", "§7Klicke, um den Kampf zu §csehen§7."));
            i++;
        }
        fightsrunning = 0;
        for (final Map.Entry<UUID, Inventory> entry : pvpinvs.entrySet()) {
            final Inventory inv = entry.getValue();
            for (int n = 18; n < 45; n++) {
                inv.setItem(n, null);
            }
            for (final Map.Entry<Integer, ItemStack> entry2 : items.entrySet()) {
                inv.setItem(entry2.getKey(), entry2.getValue());
                fightsrunning++;
            }
        }
    }

    public void checkSpectators() {
        for (final Arena a : arenas) {
            for (final UUID uuid : a.getSpectators()) {
                final Player pl = ms.getServer().getPlayer(uuid);
                final Location loc = a.getSpectateLoc();
                if (pl == null || pl.getGameMode() != GameMode.SPECTATOR || pl.getWorld().equals(ms.getBase().getWorld())) {
                    a.getSpectators().remove(uuid);
                } else if (pl.getLocation().getWorld().getName().equals(loc.getWorld().getName()) == false || pl.getLocation().distance(loc) > 50D || pl.getLocation().getBlockY() < 13D) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            pl.teleport(loc, PlayerTeleportEvent.TeleportCause.END_PORTAL);
                        }
                    }.runTaskLater(ms, 0L);
                }
            }
        }
        for (final RankedArena a : rankedArenas) {
            for (final UUID uuid : a.getSpectators()) {
                final Player pl = ms.getServer().getPlayer(uuid);
                final Location loc = a.getSpectateLoc();
                if (pl == null || pl.getGameMode() != GameMode.SPECTATOR || pl.getWorld().equals(ms.getBase().getWorld())) {
                    a.getSpectators().remove(uuid);
                } else if (pl.getLocation().getWorld().getName().equals(loc.getWorld().getName()) == false || pl.getLocation().distance(loc) > 50D || pl.getLocation().getBlockY() < 13D) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            pl.teleport(loc, PlayerTeleportEvent.TeleportCause.END_PORTAL);
                        }
                    }.runTaskLater(ms, 0L);
                }
            }
        }
    }

    private void sendToLastLocation(final Player p) {
        final MuxUser u = ms.getMuxUser(p.getName());
        final Location lastloc = u == null ? null : u.getLastLocation(), l = lastloc != null && lastloc.getWorld() != null ? lastloc : ms.getGameSpawn();
        ms.forcePlayer(p, l);
    }

    public boolean inDuel(final Player p) {
        for (final Arena a : arenas) {
            if (a.isPlayerA(p) || a.isPlayerB(p)) {
                return true;
            }
        }
        return false;
    }

    public boolean in1vs1(final Player p) {
        for (final RankedArena a : rankedArenas) {
            if (a.isPlayerA(p) || a.isPlayerB(p)) {
                return true;
            }
        }
        return false;
    }

    public Arena getArena(final Player p) {
        for (final Arena a : arenas) {
            if (a.isPlayerA(p) || a.isPlayerB(p)) {
                return a;
            }
        }
        return null;
    }

    public RankedArena get1vs1Arena(final Player p) {
        for (final RankedArena a : rankedArenas) {
            if (a.isPlayerA(p) || a.isPlayerB(p)) {
                return a;
            }
        }
        return null;
    }

    public Set<RankedArena> get1vs1Arenas() {
        return rankedArenas;
    }

    public List<Arena> getArenas() {
        return arenas;
    }

    public void sendScoreboard(final Player p, final EnumPvPScoreboardType type) {
        ms.sendScoreboard(p, type == EnumPvPScoreboardType.RANKED ? rankedsb : pvpsb);
    }

    public void loadConfig(final FileConfiguration config) {
        maps.clear();
        rankedMaps.clear();
        maps.addAll(config.getStringList("duelmaps"));
        rankedMaps.addAll(config.getStringList("rankedmaps"));
    }

    public boolean is1vs1Enabled() {
        return this.ms.getAdmin()._1VS1.isActive();
    }

    public boolean isDuelEnabled() {
        return this.ms.getAdmin().DUELS.isActive();
    }

    public void openSpectateInv(final Player p) {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxPvP§0 | /zuschauen");
        inv.setContents(pvpinv.getContents());
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.PVP && it != MuxInventory.InvType.MUXANVIL) p.closeInventory();
        p.openInventory(inv);
        pvpinvs.put(p.getUniqueId(), inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.PVP);
        updateSpectateInv();
    }

    public void openLeagueInv(final Player p) {
        pvpinvs.remove(p.getUniqueId());
        final Inventory iliga = ms.getServer().createInventory(null, 45, "§0§lMuxPvP§0 | Ligen");
        iliga.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        iliga.setItem(4, ms.renameItemStack(new ItemStack(Material.ARMOR_STAND), "§4§lLigen", "§7Mit mehr Trophäen und einer höheren", "§7Liga erhältst du mehr Belohnungen."));
        final MuxUser u = ms.getMuxUser(p.getName());
        final int trophies = u.getTrophies(), liga = trophies < 350 ? 0 : 1 + (trophies < 750 ? 0 : 1 + (trophies < 1000 ? 0 : 1 + (trophies < 1500 ? 0 : 1)));
        final ItemStack bronze = ms.renameItemStack(new ItemStack(Material.WOOD_SWORD), "§6§lBronze Liga" + (liga < 1 ? "§7 (ab 350 Trophäen)" : ""),
                "§7In dieser Liga erhältst du pro Kampf",
                "§7als Belohnung §61,5x mehr MuxCoins§7.");
        ms.addLore(bronze, liga == 1 ? new String[]{"", "§aDu befindest dich in dieser Liga."} : new String[0]);
        final ItemStack silber = ms.renameItemStack(new ItemStack(Material.IRON_SWORD), "§f§lSilber Liga" + (liga < 2 ? "§7 (ab 750 Trophäen)" : ""),
                "§7In dieser Liga erhältst du pro Kampf",
                "§7als Belohnung §f2x mehr MuxCoins§7.");
        ms.addLore(silber, liga == 2 ? new String[]{"", "§aDu befindest dich in dieser Liga."} : new String[0]);
        final ItemStack gold = ms.renameItemStack(new ItemStack(Material.GOLD_SWORD), "§e§lGold Liga" + (liga < 3 ? "§7 (ab 1.000 Trophäen)" : ""),
                "§7In dieser Liga erhältst du pro Kampf",
                "§7als Belohnung §e2,5x mehr MuxCoins§7.");
        ms.addLore(gold, liga == 3 ? new String[]{"", "§aDu befindest dich in dieser Liga."} : new String[0]);
        final ItemStack master = ms.renameItemStack(new ItemStack(Material.DIAMOND_SWORD), "§b§lMaster Liga" + (liga < 4 ? "§7 (ab 1.500 Trophäen)" : ""),
                "§7In dieser Liga erhältst du pro Kampf",
                "§7als Belohnung §b3x mehr MuxCoins§7.");
        ms.addLore(master, liga == 4 ? new String[]{"", "§aDu befindest dich in dieser Liga."} : new String[0]);
        iliga.setItem(19, liga == 1 ? ms.addGlow(bronze) : bronze);
        iliga.setItem(21, liga == 2 ? ms.addGlow(silber) : silber);
        iliga.setItem(23, liga == 3 ? ms.addGlow(gold) : gold);
        iliga.setItem(25, liga == 4 ? ms.addGlow(master) : master);
        iliga.setItem(40, ms.renameItemStack(new ItemStack(Material.WATCH), "§c§lKein Ranking bei Inaktivität", "§7Wenn du 3 Tage lang nicht kämpfst,", "§7wirst du aus dem Ranking versteckt."));
        if (ms.getActiveInv(p.getName()) != MuxInventory.InvType.PVP && ms.getActiveInv(p.getName()) != MuxInventory.InvType.EXTRAS)
            p.closeInventory();
        p.openInventory(iliga);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.PVP);
    }

    private void openKills(final Player p) {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxPvP§0 | /kills");
        inv.setItem(4, ms.addGlow(ms.renameItemStack(new ItemStack(Material.DIAMOND_SWORD), "§4§lKills", "§7Hier werden die Spieler mit den", "§7meisten Kills im PvP aufgelistet.")));
        new BukkitRunnable() {
            @Override
            public void run() {
                final List<Object[]> ranking = ms.getDB().getTopKills(10);
                if (ranking == null || ranking.isEmpty()) {
                    inv.setItem(22, ms.renameItemStack(new ItemStack(Material.BARRIER), "§c§lKein Ranking", "§7Derzeit wurde noch kein Spieler", "§7in diesem Ranking eingestuft."));
                } else {
                    byte i = 1;
                    short slot = 20;
                    boolean self = false;
                    int lastKills = 0;
                    for (final Object[] ply : ranking) {
                        final OfflinePlayer d = ms.getServer().getOfflinePlayer((UUID) ply[0]);
                        if (d == null) continue;
                        if (d.getName().equals(p.getName())) self = true;
                        final String clan = ms.getDB().getClanFromPlayer(d.getUniqueId());
                        final List<String> lore = new ArrayList<>(Arrays.asList("§7Kills: §b" + ply[1], "§7Clan: §3" + (clan == null ? "-" : clan), ""));
                        lore.addAll((d.getName().equals(p.getName()) ? Arrays.asList("§7Klicke, um dein eigenes", "§7Profil zu §4betrachten§7.") : Arrays.asList("§7Klicke, um das Profil des", "§7Spielers zu §4betrachten§7.")));
                        final ItemStack itemStack = ms.getHead(d.getName());
                        final ItemMeta meta = itemStack.getItemMeta();
                        meta.setDisplayName((i == 1 ? "§e#§l1 " : i == 2 ? "§f#§l2 " : i == 3 ? "§6#§l3 " : "§4#§l" + i + " ") + d.getName());
                        meta.setLore(lore);
                        lastKills = (int) ply[1];
                        itemStack.setItemMeta(meta);
                        inv.setItem(slot, itemStack);
                        i++;
                        slot++;
                        if (slot == 25) slot = 29;
                    }
                    if (slot > 33) slot = 33;
                    if (self == false) {
                        final MuxUser u = ms.getMuxUser(p.getName());
                        final int kills = u.getKills();
                        int rank = lastKills == kills ? i : ms.getDB().getKillsRank(p.getUniqueId());
                        final boolean pvpbanned = u.isPvPbanned();
                        final String clan = ms.getDB().getClanFromPlayer(p.getUniqueId());
                        if (pvpbanned) {
                            inv.setItem(slot, ms.renameItemStack(pvpBanHead.clone(), "§c#§l? " + p.getName(), "§7Kills: §b" + ms.getNumberFormat(kills), "§7Clan: §3" + (clan == null ? "-" : clan), "", "§cDu bist noch für §6" + ms.getTime((int) ((u.getPvPUnbanTime() - System.currentTimeMillis()) / 1000L)), "§cvom PvP ausgeschlossen."));
                        } else {
                            inv.setItem(slot, ms.renameItemStack(ms.getHead(p.getName()), "§4#§l" + rank + " " + p.getName(), "§7Kills: §b" + ms.getNumberFormat(kills), "§7Clan: §3" + (clan == null ? "-" : clan), "", "§7Klicke, um dein eigenes", "§7Profil zu §4betrachten§7."));
                        }
                    }
                }
            }
        }.runTaskAsynchronously(ms);
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.PVP && it != MuxInventory.InvType.PROFILE) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.PVP);
    }

    private void openStatsInv(final Player p) {
        pvpinvs.remove(p.getUniqueId());
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxPvP§0 | /pvp");
        inv.setItem(1, ms.renameItemStack(new ItemStack(Material.EYE_OF_ENDER, Math.max(fightsrunning, 1)), "§4§lZuschauen", "§7Die Liste enthält alle aktiven Kämpfe", "§7denen du in Ruhe zuschauen kannst.", "", "§7Klicke, um die ganze §4Liste zu sehen§7."));
        inv.setItem(4, ms.addGlow(ms.renameItemStack(new ItemStack(Material.GOLD_INGOT), "§4§lTop Kämpfer", "§7Sortiert nach der Anzahl der Trophäen", "§7befinden sich hier die besten Kämpfer.")));
        inv.setItem(7, ms.renameItemStack(new ItemStack(Material.COMPASS), "§4§lProfil suchen", "§7Betrachte mit dieser Funktion die", "§7Stats eines beliebigen Spielers.", "", "§7Klicke§7, um ein §4Profil zu suchen§7."));
        new BukkitRunnable() {
            @Override
            public void run() {
                final List<Object[]> ranking = ms.getDB().getTopTrophies(10);
                if (ranking == null || ranking.isEmpty()) {
                    inv.setItem(22, ms.renameItemStack(new ItemStack(Material.BARRIER), "§c§lKein Ranking", "§7Derzeit wurde noch kein Spieler", "§7in diesem Ranking eingestuft."));
                } else {
                    byte i = 1;
                    short slot = 20;
                    boolean self = false;
                    for (final Object[] ply : ranking) {
                        final OfflinePlayer d = ms.getServer().getOfflinePlayer((UUID) ply[0]);
                        if (d == null) continue;
                        if (d.getName().equals(p.getName())) self = true;
                        final String clan = ms.getDB().getClanFromPlayer(d.getUniqueId());
                        final List<String> lore = new ArrayList<>(Arrays.asList("§7Trophäen: §b" + ply[1], "§7Clan: §3" + (clan == null ? "-" : clan), ""));
                        lore.addAll((d.getName().equals(p.getName()) ? Arrays.asList("§7Klicke, um dein eigenes", "§7Profil zu §4betrachten§7.") : Arrays.asList("§7Klicke, um das Profil des", "§7Spielers zu §4betrachten§7.")));
                        final ItemStack itemStack = ms.getHead(d.getName());
                        final ItemMeta meta = itemStack.getItemMeta();
                        meta.setDisplayName((i == 1 ? "§e#§l1 " : i == 2 ? "§f#§l2 " : i == 3 ? "§6#§l3 " : "§4#§l" + i + " ") + d.getName());
                        meta.setLore(lore);
                        itemStack.setItemMeta(meta);
                        inv.setItem(slot, itemStack);
                        i++;
                        slot++;
                        if (slot == 25) slot = 29;
                    }
                    if (slot > 33) slot = 33;
                    if (self == false) {
                        final MuxUser u = ms.getMuxUser(p.getName());
                        final int rank = u.getLastRankedMatch() < System.currentTimeMillis() - 172800000L ? -1 : ms.getDB().getStatsRank(p.getUniqueId());
                        final boolean pvpbanned = u.isPvPbanned();
                        final String clan = ms.getDB().getClanFromPlayer(p.getUniqueId());
                        if (pvpbanned) {
                            inv.setItem(slot, ms.renameItemStack(pvpBanHead.clone(), "§c#§l? " + p.getName(), "§7Trophäen: §b" + ms.getNumberFormat(u.getTrophies()), "§7Clan: §3" + (clan == null ? "-" : clan), "", "§cDu bist noch für §6" + ms.getTime((int) ((u.getPvPUnbanTime() - System.currentTimeMillis()) / 1000L)), "§cvom PvP ausgeschlossen."));
                        } else if (rank == -1) {
                            inv.setItem(slot, ms.renameItemStack(redHead.clone(), "§c#§l? " + p.getName(), "§7Trophäen: §b" + ms.getNumberFormat(u.getTrophies()), "§7Clan: §3" + (clan == null ? "-" : clan), "", "§cDu musst beim 1vs1 teilnehmen,", "§cum wieder im Ranking zu sein."));
                        } else {
                            inv.setItem(slot, ms.renameItemStack(ms.getHead(p.getName()), "§4#§l" + rank + " " + p.getName(), "§7Trophäen: §b" + ms.getNumberFormat(u.getTrophies()), "§7Clan: §3" + (clan == null ? "-" : clan), "", "§7Klicke, um dein eigenes", "§7Profil zu §4betrachten§7."));
                        }
                    }
                }
            }
        }.runTaskAsynchronously(ms);
        inv.setItem(44, ms.renameItemStack(new ItemStack(Material.ARMOR_STAND), "§4§lLigen", "§bMaster §7(ab 1.500 Trophäen)", "§eGold §7(ab 1.000 Trophäen)",
                "§fSilber §7(ab 750 Trophäen)", "§6Bronze §7(ab 350 Trophäen)", "", "§4Klicke§7, um mehr zu erfahren."));
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.PVP && it != MuxInventory.InvType.PROFILE) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.PVP);
    }

    public boolean handleQuit(final Player p) {
        prequests.remove(p.getUniqueId());
        rankedQueue.remove(p);
        final RankedArena rankedArena = get1vs1Arena(p);
        return rankedArena != null ? rankedArena.handleDeath(p, true) : handleDuelDeath(p, true);
    }

    public boolean spectateTo(final Player p, final String game, final Location l) {
        if (ms.inBattle(p.getName(), p.getLocation())) {
            ms.showItemBar(p, ms.hnotinfight);
            return false;
        } else if (ms.inWar(p)) {
            ms.showItemBar(p, "§cDu bist derzeit in einem ClanWar.");
            return false;
        } else if (ms.in1vs1(p)) {
            ms.showItemBar(p, "§cDu bist derzeit in einem 1vs1.");
            return false;
        } else if (ms.inDuel(p)) {
            ms.showItemBar(p, "§cDu bist derzeit in einem Duell.");
            return false;
        } else if (ms.inGame(p)) {
            ms.showItemBar(p, "§cDu spielst gerade ein Minigame.");
            return false;
        } else if (ms.inEvent(p)) {
            ms.showItemBar(p, "§cDu bist derzeit in einem Event.");
            return false;
        } else if (ms.isBeingTeleported(p.getName())) {
            ms.showItemBar(p, "§cDu wirst gerade teleportiert.");
            return false;
        } else if (p.getGameMode() == GameMode.SPECTATOR) {
            ms.showItemBar(p, "§cDu bist schon im Zuschauermodus.");
            return false;
        }
        ms.getMounts().deactivateMount(p);
        ms.getPets().deactivatePet(p);
        if (p.isInsideVehicle()) {
            p.leaveVehicle();
        }
        ms.setLastLocation(p);
        ms.forcePlayer(p, l);
        p.playSound(l, Sound.ENDERMAN_TELEPORT, 0.2F, 1F);
        p.setGameMode(GameMode.SPECTATOR);
        ms.showItemBar(p, "§fDu wurdest zum §6" + game + " §fteleportiert.");
        return true;
    }

    public enum EnumPvPScoreboardType {
        RANKED,
        DUEL
    }

    /*********
     DUEL
     *********/

    public class Arena {
        private boolean bodyfix = true, finished = false, friendly = false;
        private final int entrycost, ops, heals;
        private final long timestarted;
        private long lasthit = System.currentTimeMillis();
        private final String map;
        private Player playerA = null, playerB = null, winner = null;
        private Location locA, locB, gateA, gateB, spectate;
        private final CopyOnWriteArrayList<UUID> spectators = new CopyOnWriteArrayList<>();

        public Arena(final String map, final boolean bodyfix, final int ops, final int heals, final int entrycost) {
            this.map = map.toLowerCase();
            this.bodyfix = bodyfix;
            this.ops = ops;
            this.heals = heals;
            this.entrycost = entrycost;
            this.timestarted = System.currentTimeMillis();
        }

        public boolean hasBodyFix() {
            return bodyfix;
        }

        public int getOPs() {
            return ops;
        }

        public int getHeals() {
            return heals;
        }

        public int getEntryCost() {
            return entrycost;
        }

        public void setFriendly(final boolean friendly) {
            this.friendly = friendly;
        }

        public long getTimeStarted() {
            return timestarted;
        }

        public boolean isFriendly() {
            return friendly;
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

        public Player getPlayerA() {
            return playerA;
        }

        public Player getPlayerB() {
            return playerB;
        }

        public void updateLastHit() {
            this.lasthit = System.currentTimeMillis();
        }

        public long getLastHit() {
            return lasthit;
        }

        public List<UUID> getSpectators() {
            return spectators;
        }

        public Location getSpectateLoc() {
            return spectate;
        }

        public boolean isPlayerA(final Player p) {
            return playerA == p;
        }

        public boolean isPlayerB(final Player p) {
            return playerB == p;
        }

        public boolean lostA(final Player p) {
            if (playerA == p) {
                winner = playerB;
                return true;
            }
            return false;
        }

        public boolean lostB(final Player p) {
            if (playerB == p) {
                winner = playerA;
                return true;
            }
            return false;
        }

        public void setPlayerA(final Player p) {
            playerA = p;
            p.playSound(p.getLocation(), "mob.guardian.elder.idle", 1F, 1F);
        }

        public void setPlayerB(final Player p) {
            playerB = p;
            p.playSound(p.getLocation(), "mob.guardian.elder.idle", 1F, 1F);
        }

        public void spectate(final Player p) {
            if (spectateTo(p, "Duell", spectate)) {
                spectators.add(p.getUniqueId());
            }
        }

        public void sendPlayerMessage(final String message) {
            playerA.sendMessage(message);
            playerB.sendMessage(message);
        }

        public void sendTitle(final String message, int showtime) {
            if (showtime == 0) showtime = 22;
            ms.sendNormalTitle(playerA, message, 0, showtime, 10);
            ms.sendNormalTitle(playerB, message, 0, showtime, 10);
        }

        public void sendBigTitle(final String message) {
            ms.sendTitle(playerA, message, 0, 22, 10);
            ms.sendTitle(playerB, message, 0, 22, 10);
        }

        public void playSound(final Sound sound, final float pitch) {
            playerA.playSound(playerA.getLocation(), sound, 1F, pitch);
            playerB.playSound(playerB.getLocation(), sound, 1F, pitch);
        }

        public void clearArena(final boolean win, final boolean instant, final boolean expired) {
            if (win) {
                if (winner == playerA) {
                    ms.sendTitle(playerA, "§6§lDuell", 10, 60, 10);
                    ms.sendSubTitle(playerA, "§fDu hast den Kampf §agewonnen§f!", 10, 60, 10);
                    if (entrycost > 0) {
                        final MuxUser u = ms.getMuxUser(playerA.getName());
                        final int coins = entrycost * 2;
                        u.addCoins(coins);
                        ms.getHistory().addHistory(playerA.getUniqueId(), null, "COINS", String.valueOf(coins), "Duel");
                        ms.saveMuxUser(u);
                        playerA.sendMessage("§6§lDuell>§7 Dein Gewinn: §e§l" + ms.getNumberFormat(coins) + " MuxCoins");
                    }
                    playerB.sendMessage("§6§lDuell>§c Du hast den Kampf verloren.");
                } else if (winner == playerB) {
                    ms.sendTitle(playerB, "§6§lDuell", 10, 60, 10);
                    ms.sendSubTitle(playerB, "§fDu hast den Kampf §agewonnen§f!", 10, 60, 10);
                    if (entrycost > 0) {
                        final MuxUser u = ms.getMuxUser(playerB.getName());
                        final int coins = entrycost * 2;
                        u.addCoins(coins);
                        ms.getHistory().addHistory(playerB.getUniqueId(), null, "COINS", String.valueOf(coins), "Duel");
                        ms.saveMuxUser(u);
                        playerB.sendMessage("§6§lDuell>§7 Dein Gewinn: §e§l" + ms.getNumberFormat(coins) + " MuxCoins");
                    }
                    playerA.sendMessage("§6§lDuell>§c Du hast den Kampf verloren.");
                }
                for (final Player pl : ms.getServer().getOnlinePlayers()) {
                    if (playerA != pl && playerB != pl) {
                        pl.sendMessage("§6§lDuell>§7 Der Spieler §6" + winner.getName() + "§7 hat den Kampf gewonnen.");
                    } else {
                        ms.removeFromBattle(pl.getName());
                        ms.resetScoreboard(pl);
                        if (pl.getFireTicks() > 0) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (pl.isOnline())
                                        pl.setFireTicks(0);
                                }
                            }.runTaskLater(ms, 1L);
                        }
                    }
                }
            } else if (expired == false) {
                sendPlayerMessage("§6§lDuell>§7 Der Kampf wurde von einem Admin §cgestoppt§7.");
            }
            finished = true;
            if (instant) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        stopGame(win);
                    }
                }.runTask(ms);
            } else {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        stopGame(win);
                    }
                }.runTaskLater(ms, 300L);
            }
        }

        private void stopGame(final boolean win) {
            if (win == false) {
                ms.getMuxUser(playerA.getName()).addCoins(entrycost);
                ms.getHistory().addHistory(playerA.getUniqueId(), null, "COINS", String.valueOf(entrycost), "Duel");
                ms.resetScoreboard(playerA);
                ms.getMuxUser(playerB.getName()).addCoins(entrycost);
                ms.getHistory().addHistory(playerB.getUniqueId(), null, "COINS", String.valueOf(entrycost), "Duel");
                ms.resetScoreboard(playerB);
            }
            for (final UUID uuid : spectators) {
                final Player pl = ms.getServer().getPlayer(uuid);
                if (pl != null) {
                    sendToLastLocation(pl);
                }
            }
            if (playerA == winner) {
                ms.forcePlayer(playerA, ms.getGameSpawn());
            } else if (playerB == winner) {
                ms.forcePlayer(playerB, ms.getGameSpawn());
            } else {
                ms.forcePlayer(playerA, ms.getGameSpawn());
                ms.forcePlayer(playerB, ms.getGameSpawn());
            }
            playerB = null;
            playerA = null;
            arenas.remove(this);
            updateSpectateInv();
        }

        public void startGame() {
            ms.getMuxUser(playerA.getName()).addCoins(-entrycost);
            ms.getHistory().addHistory(playerA.getUniqueId(), null, "COINS", String.valueOf(-entrycost), "Duel");
            sendScoreboard(playerA, EnumPvPScoreboardType.DUEL);
            ms.getMuxUser(playerB.getName()).addCoins(-entrycost);
            ms.getHistory().addHistory(playerB.getUniqueId(), null, "COINS", String.valueOf(-entrycost), "Duel");
            sendScoreboard(playerB, EnumPvPScoreboardType.DUEL);
            final String mapname = "duel" + map.replace("ä", "a").replace("ö", "o").replace("ü", "u");
            if (ops > 0) {
                ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "region flag " + mapname + " -w " + locA.getWorld().getName() + " pistons allow");
            } else {
                ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "region flag " + mapname + " -w " + locA.getWorld().getName() + " pistons deny");
            }
            if (bodyfix) {
                ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "region flag " + mapname + " -w " + locA.getWorld().getName() + " blocked-cmds");
            } else {
                ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "region flag " + mapname + " -w " + locA.getWorld().getName() + " blocked-cmds fix,repair");
            }
            for (final Item item : locA.getWorld().getEntitiesByClass(Item.class)) {
                if (item.getLocation().distance(locA) < 70 || item.getLocation().distance(locB) < 70) {
                    item.remove();
                }
            }
            new BukkitRunnable() {
                short seconds = 5;

                @Override
                public void run() {
                    if (playerA == null || playerB == null) {
                        cancel();
                        return;
                    } else if (seconds > 0) {
                        playSound(Sound.NOTE_STICKS, 0F);
                        sendTitle("§fDer Kampf beginnt in §6" + seconds + " Sekunde" + (seconds > 1 ? "n" : "") + "§f...", 0);
                        seconds--;
                        return;
                    }
                    for (final Player pl : ms.getServer().getOnlinePlayers()) {
                        if (playerA != pl && playerB != pl) {
                            ms.chatClickHoverRun(pl, "§6§lDuell>§7 Der Spieler §c" + playerA.getName() + " §7kämpft gegen §b" + playerB.getName() + "§7.",
                                    "§6§lDuell: §c" + playerA.getName() + " §7gegen §b" + playerB.getName() + "\n§7Map: §f" + StringUtils.capitalize(map) + "\n§7Bodyfix: " + (bodyfix ? "§aaktiviert" : "§cdeaktiviert") + "\n§7OP-Äpfel: " + (ops == 0 ? "§cdeaktiviert" : "§f" + ops + " Stück") + "\n§7Heiltränke: §f" + heals + " Stack" + (heals > 1 ? "s" : "") + "\n§7Gewinn: §e" + (entrycost == 0 ? "keinen" : ms.getNumberFormat(entrycost) + " MuxCoins") + "\n\n" + "§6§oKlicke, um zuzugucken.", "/duel spectate " + map);
                            ms.chatClickHoverRun(pl, "§6§lDuell>§7 Klicke §6hier§7, um diesen Kampf zu sehen.",
                                    "§6§lDuell: §c" + playerA.getName() + " §7gegen §b" + playerB.getName() + "\n§7Map: §f" + StringUtils.capitalize(map) + "\n§7Bodyfix: " + (bodyfix ? "§aaktiviert" : "§cdeaktiviert") + "\n§7OP-Äpfel: " + (ops == 0 ? "§cdeaktiviert" : "§f" + ops + " Stück") + "\n§7Heiltränke: §f" + heals + " Stack" + (heals > 1 ? "s" : "") + "\n§7Gewinn: §e" + (entrycost == 0 ? "keinen" : ms.getNumberFormat(entrycost) + " MuxCoins") + "\n\n" + "§6§oKlicke, um zuzugucken.", "/duel spectate " + map);
                        }
                    }
                    sendTitle("§fDer Kampf beginnt §6jetzt§f, viel Glück!", 0);
                    updateSpectateInv();
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
            gate.clone().add(1, 0, 0).getBlock().setType(m);
            gate.clone().add(-1, 0, 0).getBlock().setType(m);
            gate.clone().add(2, 0, 0).getBlock().setType(m);
            gate.clone().add(-2, 0, 0).getBlock().setType(m);
            gate.clone().add(1, 1, 0).getBlock().setType(m);
            gate.clone().add(-1, 1, 0).getBlock().setType(m);
            gate.clone().add(2, 1, 0).getBlock().setType(m);
            gate.clone().add(-2, 1, 0).getBlock().setType(m);
            gate.clone().add(0, 2, 0).getBlock().setType(m);
            gate.clone().add(1, 2, 0).getBlock().setType(m);
            gate.clone().add(-1, 2, 0).getBlock().setType(m);
            gate.clone().add(2, 2, 0).getBlock().setType(m);
            gate.clone().add(-2, 2, 0).getBlock().setType(m);
        }

        public boolean loadLocations() {
            this.locA = ms.getLoc("duel" + this.map + "spawn1");
            this.locB = ms.getLoc("duel" + this.map + "spawn2");
            this.gateA = ms.getLoc("duel" + this.map + "gate1");
            this.gateB = ms.getLoc("duel" + this.map + "gate2");
            this.spectate = ms.getLoc("duel" + this.map + "spectate");
            if (this.locA == null || this.locB == null || this.gateA == null || this.gateB == null || this.spectate == null) {
                System.err.println("MuxSystem> Error: Could not load Duel Locations of the Map " + this.map);
                return false;
            }
            setGates(false);
            return true;
        }
    }

    public boolean handleDuelDeath(final Player p, final boolean quit) {
        for (final Arena a : arenas) {
            if (a.isFinished() && (a.isPlayerA(p) || a.isPlayerB(p))) {
                if (quit) ms.forcePlayer(p, ms.getGameSpawn());
                continue;
            } else if (a.lostA(p) == false && a.lostB(p) == false) {
                continue;
            }
            if (quit == false) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        p.spigot().respawn();
                    }
                }.runTaskLater(ms, 5L);
            }
            a.playSound(Sound.AMBIENCE_THUNDER, 1F);
            a.clearArena(true, false, false);
            return true;
        }
        return false;
    }

    public void checkTime() {
        for (final Arena a : arenas) {
            if (System.currentTimeMillis() > a.getTimeStarted() + 1800000L) {
                a.sendPlayerMessage("§6§lDuell>§c Der Kampf wurde gestoppt.");
                a.sendPlayerMessage("§6§lDuell>§c Grund: Über 30 Minuten.");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        a.clearArena(false, true, true);
                    }
                }.runTask(ms);
            } else if (System.currentTimeMillis() > a.getLastHit() + 30000L) {
                if (ms.checkGeneralCooldown(a.getPlayerA().getName(), "DUELNOHIT", 120000L, true) == false) {
                    a.sendTitle("§fUm den Kampf abzubrechen, §cbewege dich nicht mehr§7.", 40);
                    a.sendPlayerMessage("§6§lDuell>§7 Um den Kampf abzubrechen, §cbewege dich nicht mehr§7.");
                }
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (a.isFinished()) {
                            cancel();
                            return;
                        }
                        final Location locA = a.getPlayerA().getLocation(), locB = a.getPlayerB().getLocation();
                        new BukkitRunnable() {
                            int count = 5;

                            @Override
                            public void run() {
                                if (a.isFinished()) cancel();
                                final Location locA2 = a.getPlayerA().getLocation(), locB2 = a.getPlayerB().getLocation();
                                if (locA.getBlockX() != locA2.getBlockX() || locA.getBlockZ() != locA2.getBlockZ()
                                        || locB.getBlockX() != locB2.getBlockX() || locB.getBlockZ() != locB2.getBlockZ()) {
                                    cancel();
                                    return;
                                }
                                if (count == 0) {
                                    a.sendTitle("§fDas Duell wurde §cabgebrochen§f.", 40);
                                    a.playSound(Sound.NOTE_BASS, 1F);
                                    a.clearArena(false, true, true);
                                    cancel();
                                    return;
                                }
                                a.playSound(Sound.NOTE_STICKS, 0F);
                                a.sendBigTitle("§6§l" + count);
                                count--;
                            }
                        }.runTaskTimer(ms, 0L, 20L);
                    }
                }.runTaskLater(ms, 60L);
            }
        }
        prequests.values().removeIf(req -> req.getTime() + 300000L < System.currentTimeMillis());
    }

    class DuelRequest {
        private int entrycost, ops, heals;
        private final long time;
        private boolean bodyfix;

        public DuelRequest() {
            time = System.currentTimeMillis();
        }

        public long getTime() {
            return time;
        }

        public int getEntryCost() {
            return entrycost;
        }

        public int getOPs() {
            return ops;
        }

        public int getHeals() {
            return heals;
        }

        public boolean hasBodyFix() {
            return bodyfix;
        }

        public void setEntryCost(final int entrycost) {
            this.entrycost = entrycost;
        }

        public void setOPs(final int ops) {
            this.ops = ops;
        }

        public void setHeals(final int heals) {
            this.heals = heals;
        }

        public void setBodyFix(final boolean bodyfix) {
            this.bodyfix = bodyfix;
        }
    }

    private void acceptDuel(final Player from, final Player to, final boolean confirm, final boolean friend) {
        final DuelRequest req = prequests.get(from.getUniqueId());
        if (req == null) {
            ms.showItemBar(to, "§cDu hast keine Anfrage.");
            return;
        } else if (in1vs1(to)) {
            ms.showItemBar(to, "§cDu bist bereits in einem 1vs1.");
            return;
        } else if (in1vs1(from)) {
            ms.showItemBar(to, "§cDer Herausforderer ist bereits in einem 1vs1.");
            return;
        } else if (inDuel(to)) {
            ms.showItemBar(to, "§cDu bist bereits in einem Duell.");
            return;
        } else if (inDuel(from)) {
            ms.showItemBar(to, "§cDer Herausforderer ist bereits in einem Duell.");
            return;
        } else if (ms.inWar(to)) {
            ms.showItemBar(to, "§cDu bist gerade im ClanWar.");
            return;
        } else if (ms.inWar(from)) {
            ms.showItemBar(to, "§cDer Herausforderer ist gerade im ClanWar.");
            return;
        } else if (ms.getBans().checkPvPBan(to, true)) {
            return;
        } else if (ms.getBans().checkPvPBan(from, false)) {
            ms.showItemBar(to, "§cDer Herausforderer ist derzeit vom PvP ausgeschlossen.");
            return;
        } else if (ms.getNewbies().isProtectedNewbie(to)) {
            ms.showItemBar(to, "§cDu kannst als Neuling noch nicht einem Duell beitreten.");
            return;
        } else if (ms.getNewbies().isProtectedNewbie(from)) {
            ms.showItemBar(from, "§cDer Herausforderer kann nicht als Neuling beitreten.");
            return;
        } else if (ms.getTrades().isTrading(to.getName())) {
            ms.showItemBar(to, "§cDu bist gerade noch am handeln.");
            return;
        } else if (ms.getTrades().isTrading(from.getName())) {
            ms.showItemBar(from, "§cDer Herausforderer handelt gerade mit jemanden.");
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
        String unusedMap = null;
        for (final String map : maps) {
            if (arenas.stream().noneMatch(arena -> arena.getMap().equalsIgnoreCase(map))) {
                unusedMap = map;
                break;
            }
        }
        if (unusedMap == null) {
            from.sendMessage("§c§lDerzeit sind alle Arenen besetzt.");
            ms.showItemBar(to, "§cDerzeit sind alle Arenen besetzt.");
            return;
        } else if (ms.getMuxUser(to.getName()).getCoins() < req.getEntryCost()) {
            ms.showItemBar(to, "§cDu besitzt nicht genügend MuxCoins.");
            return;
        } else if (ms.getMuxUser(from.getName()).getCoins() < req.getEntryCost()) {
            ms.showItemBar(to, "§cDer Herausforderer hat leider nicht genug MuxCoins.");
            from.sendMessage("§c§lFehler:§c Du besitzt nicht genügend MuxCoins für den Duell.");
            return;
        } else if (req.getOPs() != 0 && ms.getMaterialCount(to.getInventory(), Material.GOLDEN_APPLE, (byte) 1) > req.getOPs()) {
            ms.showItemBar(to, "§cEs sind höchstens " + req.getOPs() + " OP-Äpfel erlaubt.");
            return;
        } else if (req.getOPs() != 0 && ms.getMaterialCount(from.getInventory(), Material.GOLDEN_APPLE, (byte) 1) > req.getOPs()) {
            ms.showItemBar(to, "§cDer Herausforderer hat zu viele OP-Äpfel im Inventar.");
            from.sendMessage("§c§lEs sind höchstens " + req.getOPs() + " OP-Äpfel erlaubt.");
            return;
        }
        final int healsfrom = ms.getMaterialCount(from.getInventory(), Material.POTION, (byte) 16389) +
                ms.getMaterialCount(from.getInventory(), Material.POTION, (byte) 16421) +
                ms.getMaterialCount(from.getInventory(), Material.POTION, (byte) 16453),
                healsto = ms.getMaterialCount(to.getInventory(), Material.POTION, (byte) 16389) +
                        ms.getMaterialCount(to.getInventory(), Material.POTION, (byte) 16421) +
                        ms.getMaterialCount(to.getInventory(), Material.POTION, (byte) 16453);
        if (healsto / 64D > (double) req.getHeals()) {
            ms.showItemBar(to, "§cEs " + (req.getHeals() > 1 ? "sind" : "ist") + " höchstens " + req.getHeals() + " Stack" + (req.getHeals() > 1 ? "s" : "") + " Heiltränke erlaubt.");
            return;
        } else if (healsfrom / 64D > (double) req.getHeals()) {
            ms.showItemBar(to, "§cDer Herausforderer hat zu viele Heiltränke im Inventar.");
            from.sendMessage("§c§lEs " + (req.getHeals() > 1 ? "sind" : "ist") + " höchstens " + req.getHeals() + " Stack" + (req.getHeals() > 1 ? "s" : "") + " Heiltränke erlaubt.");
            return;
        }
        final boolean expensive = ((double) req.getEntryCost() / (double) ms.getMuxUser(to.getName()).getCoins()) > 0.10D;
        if (expensive && confirm == false) {
            new ConfirmInventory(ms, pl -> {
                pl.performCommand("duel accept " + from.getName() + (friend ? " cf" : " c"));
                pl.closeInventory();
            }, HumanEntity::closeInventory).show(to,
                    "§0§lDuell bestätigen", "§aBestätigen", "§cAbbrechen", new ItemStack(Material.WOOD_SWORD),
                    "§6§lDuell gegen " + from.getName(), "§7Bodyfix: " + (req.hasBodyFix() ? "§aaktiviert" : "§cdeaktiviert"), "§7OP-Äpfel: " + (req.getOPs() == 0 ? "§cdeaktiviert" : "§f" + req.getOPs() + " Stück"), "§7Heiltränke: §f" + req.getHeals() + " Stack" + (req.getHeals() > 1 ? "s" : ""), "§7Beitrittskosten: §e" + (req.getEntryCost() == 0 ? "keine" : req.getEntryCost() + " MuxCoins"));
            return;
        }
        final Arena a = new Arena(unusedMap, req.hasBodyFix(), req.getOPs(), req.getHeals(), req.getEntryCost());
        if (a.loadLocations() == false) {
            to.sendMessage("§6§lDuell>§c Es gab einen Fehler. Melde dies einen Admin.");
            from.sendMessage("§6§lDuell>§c Es gab einen Fehler. Melde dies einen Admin.");
            return;
        }
        if (friend) a.setFriendly(true);
        from.sendMessage("§6§lDuell>§7 Du bist mit §6" + to.getName() + "§7 im Duell §abeigetreten§7.");
        ms.showItemBar(to, "§fDu bist dem Duell §abeigetreten§f.");
        if (a.getOPs() == 0) {
            from.removePotionEffect(PotionEffectType.ABSORPTION);
            from.removePotionEffect(PotionEffectType.REGENERATION);
            from.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            to.removePotionEffect(PotionEffectType.ABSORPTION);
            to.removePotionEffect(PotionEffectType.REGENERATION);
            to.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
        }
        from.closeInventory();
        to.closeInventory();
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
        if (to.isInsideVehicle()) {
            to.leaveVehicle();
        }
        if (from.isInsideVehicle()) {
            from.leaveVehicle();
        }
        from.setAllowFlight(false);
        from.setFlying(false);
        to.setAllowFlight(false);
        to.setFlying(false);
        from.setFireTicks(0);
        to.setFireTicks(0);
        from.setWalkSpeed(0.2F);
        to.setWalkSpeed(0.2F);
        ms.forcePlayer(from, a.getLocA());
        ms.forcePlayer(to, a.getLocB());
        a.setPlayerA(from);
        a.setPlayerB(to);
        arenas.add(a);
        prequests.remove(from.getUniqueId());
        rankedQueue.remove(from);
        rankedQueue.remove(to);
        a.startGame();
    }

    private void requestDuel(final Player p, final Settings settings) {
        final UUID uuid = p.getUniqueId();
        prequests.remove(uuid);
        final DuelRequest an = new DuelRequest();
        an.setBodyFix(settings.hasBodyFix());
        an.setEntryCost(settings.getEntryCost());
        an.setOPs(settings.getOPs());
        an.setHeals(settings.getHeals());
        prequests.put(uuid, an);
    }

    public void openDuelInv(final Player p, final int page, int type) {
        // type 0 = pmenu, 1 = friends, 2 = clan
        pvpinvs.remove(p.getUniqueId());
        Inventory inv = null;
        final MuxUser u = ms.getMuxUser(p.getName());
        final Settings s = u.getSettings();
        if (page == 0) {
            inv = ms.getServer().createInventory(null, 45, "§0§lMuxPvP§0 | Duell");
            inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), "§7Zurück (" + (type == 2 ? "Clan" : type == 1 ? "Freund" : "Spieler") + ")"));
            inv.setItem(4, ms.renameItemStack(new ItemStack(Material.WOOD_SWORD), "§6§lDuell", "§7Hier kannst du dein eigenes Duell", "§7genauso gestalten wie du willst."));
            final ItemStack item2 = ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT), "§6§lEinsatz:" + (s.getEntryCost() > 0 ? "§e §l" + ms.getNumberFormat(s.getEntryCost()) + " MuxCoins" : "§c §lkeinen"),
                    "§7Lege fest, wie viele MuxCoins eingesetzt", "§7werden. Der Gewinner erhält dann alles.", "", "§7Klicke, um den Einsatz zu §6verändern§7."),
                    heal = ms.renameItemStack(new ItemStack(Material.POTION, 1, (short) 16421), "§6§lHeiltränke:" + (s.getHeals() == 0 ? "§c §ldeaktiviert" : "§f §l" + s.getHeals() + " Stack" + (s.getHeals() > 1 ? "s" : "")),
                            "§7Bestimme die maximale Anzahl an", "§7Heiltränken für jeden Spieler.", "", "§6Klicke§7, um die Option zu bearbeiten."),
                    bodyfix = ms.renameItemStack(new ItemStack(Material.CHAINMAIL_CHESTPLATE), "§6§lBodyfix:" + (s.hasBodyFix() ? "§a §laktiviert" : "§c §ldeaktiviert"),
                            "§7Ohne Bodyfix müsste die Rüstung vor", "§7dem Reparieren ausgezogen werden.", "", "§7Klicke, um die Option zu " + (s.hasBodyFix() ? "§cdeaktivieren§7." : "§aaktivieren§7."));
            inv.setItem(20, s.getEntryCost() > 0 ? ms.addGlow(item2) : item2);
            inv.setItem(24, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 5), "§a§lStarten",
                    "",
                    "§cWichtig: §7Jeder muss seine Sachen bereit",
                    "§7haben, um sofort kämpfen zu können.",
                    "",
                    "§aKlicke§7, um den Spieler herauszufordern."));
            final ItemMeta im = heal.getItemMeta();
            im.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            heal.setItemMeta(im);
            inv.setItem(37, heal);
            inv.setItem(40, s.hasBodyFix() ? ms.addGlow(bodyfix) : bodyfix);
            inv.setItem(43, ms.renameItemStack(new ItemStack(Material.GOLDEN_APPLE, 1, s.getOPs() == 0 ? (byte) 0 : (byte) 1), "§6§lOP-Äpfel:" + (s.getOPs() == 0 ? "§c §ldeaktiviert" : "§f §l" + s.getOPs() + " Stück"),
                    "§7Bestimme, wie viele OP-Äpfel höchstens", "§7beide beim Kampf mitnehmen dürfen.", "", "§6Klicke§7, um zu die Anzahl zu verändern."));
        } else if (page == 1) {
            inv = ms.getServer().createInventory(null, 45, "§0§lMuxPvP§0 | OP-Äpfel");
            inv.setItem(4, ms.renameItemStack(new ItemStack(Material.GOLDEN_APPLE, 1, (byte) 1), "§6§lOP-Äpfel",
                    "§7Bestimme, wie viele OP-Äpfel höchstens", "§7beide beim Kampf mitnehmen dürfen."));
            final ItemStack i = ms.renameItemStack(new ItemStack(Material.BARRIER), "§6§lDeaktiviert", "", "§7Klicke, um die Option §6auszuwählen§7."),
                    i2 = ms.renameItemStack(new ItemStack(Material.APPLE, 2), "§6§l2 OP-Äpfel", "", "§7Klicke, um die Option §6auszuwählen§7."),
                    i3 = ms.renameItemStack(new ItemStack(Material.APPLE, 4), "§6§l4 OP-Äpfel", "", "§7Klicke, um die Option §6auszuwählen§7."),
                    i4 = ms.renameItemStack(new ItemStack(Material.APPLE, 8), "§6§l8 OP-Äpfel", "", "§7Klicke, um die Option §6auszuwählen§7."),
                    i5 = ms.renameItemStack(new ItemStack(Material.APPLE, 16), "§6§l16 OP-Äpfel", "", "§7Klicke, um die Option §6auszuwählen§7.");
            inv.setItem(18, i);
            inv.setItem(20, i2);
            inv.setItem(22, i3);
            inv.setItem(24, i4);
            inv.setItem(26, i5);
        } else if (page == 2) {
            inv = ms.getServer().createInventory(null, 45, "§0§lMuxPvP§0 | Heiltränke");
            final ItemStack heal = ms.renameItemStack(new ItemStack(Material.POTION, 1, (short) 16421), "§6§lHeiltränke",
                    "§7Bestimme die maximale Anzahl an", "§7Heiltränken für jeden Spieler.");
            final ItemMeta im = heal.getItemMeta();
            im.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
            heal.setItemMeta(im);
            inv.setItem(4, heal);
            inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), "§7Zurück (Einstellungen)"));
            final ItemStack i = ms.renameItemStack(new ItemStack(Material.POTION), "§6§l1 Stack", "", "§7Klicke, um die Option §6auszuwählen§7."),
                    i2 = ms.renameItemStack(new ItemStack(Material.POTION, 2), "§6§l2 Stacks", "", "§7Klicke, um die Option §6auszuwählen§7."),
                    i3 = ms.renameItemStack(new ItemStack(Material.POTION, 4), "§6§l4 Stacks", "", "§7Klicke, um die Option §6auszuwählen§7."),
                    i4 = ms.renameItemStack(new ItemStack(Material.POTION, 6), "§6§l6 Stacks", "", "§7Klicke, um die Option §6auszuwählen§7."),
                    i5 = ms.renameItemStack(new ItemStack(Material.POTION, 8), "§6§l8 Stacks", "", "§7Klicke, um die Option §6auszuwählen§7.");
            inv.setItem(18, i);
            inv.setItem(20, i2);
            inv.setItem(22, i3);
            inv.setItem(24, i4);
            inv.setItem(26, i5);
        }
        if (inv == null) return;
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.PVP && it != MuxInventory.InvType.FRIENDS && it != MuxInventory.InvType.CLANS && it != MuxInventory.InvType.PMENU && it != MuxInventory.InvType.MUXANVIL)
            p.closeInventory();
        s.setActivePage(page);
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.PVP);
    }

    /*******
     RANKED
     *******/

    public class RankedArena {
        private Location locA, locB, gateA, gateB, spectate;
        private final String map;
        private final RankedKit kit;

        private Player playerA, playerB, winner;
        private SavedPlayerInventory inventoryA, inventoryB;
        private long startTime, lastTick, lasthit;
        private byte endingState = 0;
        private int wonCoins = -1;
        private boolean started = false, finished = false;

        private final CopyOnWriteArrayList<UUID> spectators = new CopyOnWriteArrayList<>();

        RankedArena(final String map, final RankedKit kit) {
            this.map = map.toLowerCase();
            this.kit = kit;
            this.lastTick = System.currentTimeMillis();
            loadLocations();
        }

        public void tick() {
            if (kit == null || finished || playerA == null || playerB == null) return;
            if (lastTick > System.currentTimeMillis() - kit.tickTime) return;
            lastTick = System.currentTimeMillis();

            switch (endingState) {
                case 1:
                    if (startTime + kit.stopTime <= System.currentTimeMillis()) {
                        endingState = 2;
                        final Player p = r.nextInt(2) == 0 ? playerA : playerB;

                        sendPlayerMessage("§c§lMux1vs1>§c Da nach " + ms.timeToString(kit.stopTime, false) + " §ckein Spieler gewonnen hat, gewinnt ein zufälliger Spieler.");
                        handleDeath(p, false);
                        return;
                    }
                    break;
                case 0:
                    if (startTime + (kit.stopTime / 2) <= System.currentTimeMillis()) {
                        sendTitle("§cDas 1vs1 wird in " + ms.timeToString((kit.stopTime / 2), false) + " §cbeendet.", 0);
                        endingState = 1;
                    }
                    break;
                default:
                    break;
            }
            kit.onTick(this);
        }

        public RankedKit getKit() {
            return kit;
        }

        public Location getLocA() {
            return locA;
        }

        public Location getLocB() {
            return locB;
        }

        public String getMap() {
            return map;
        }

        public boolean isFinished() {
            return finished;
        }

        public void updateLastHit() {
            this.lasthit = System.currentTimeMillis();
        }

        public long getLastHit() {
            return lasthit;
        }

        public List<UUID> getSpectators() {
            return spectators;
        }

        public Location getSpectateLoc() {
            return spectate;
        }

        public boolean isPlayerA(final Player p) {
            return playerA == p;
        }

        public boolean isPlayerB(final Player p) {
            return playerB == p;
        }

        public boolean lostA(final Player p) {
            if (playerA == p) {
                winner = playerB;
                return true;
            }
            return false;
        }

        public Player getPlayerA() {
            return playerA;
        }

        public Player getPlayerB() {
            return playerB;
        }

        public boolean lostB(final Player p) {
            if (playerB == p) {
                winner = playerA;
                return true;
            }
            return false;
        }

        public void spectate(final Player p) {
            if (spectateTo(p, "1vs1", spectate)) {
                spectators.add(p.getUniqueId());
            }
        }

        public void sendPlayerMessage(final String message) {
            if (playerA != null) playerA.sendMessage(message);
            if (playerB != null) playerB.sendMessage(message);
        }

        public void sendTitle(final String message, int showtime) {
            if (showtime == 0) showtime = 22;
            ms.sendNormalTitle(playerA, message, 0, showtime, 10);
            ms.sendNormalTitle(playerB, message, 0, showtime, 10);
        }

        public void sendBigTitle(final String message) {
            ms.sendTitle(playerA, message, 0, 22, 10);
            ms.sendTitle(playerB, message, 0, 22, 10);
        }

        public void playSound(final Sound sound, final float pitch) {
            playerA.playSound(playerA.getLocation(), sound, 1F, pitch);
            playerB.playSound(playerB.getLocation(), sound, 1F, pitch);
        }

        public void clearArena(final boolean win, final boolean instant, final boolean expired, final boolean quit) {
            if (win) {
                if (winner == playerA) {
                    ms.sendTitle(playerA, "§c§lMux1vs1", 10, 60, 10);
                    ms.sendSubTitle(playerA, "§fDu hast den Kampf §agewonnen§f!", 10, 60, 10);
                    ms.showItemBar(playerB, "§cDu hast den Kampf verloren.");
                    if (wonCoins > 0 && quit == false)
                        ms.runLater(new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (playerA.isOnline() == false) return;
                                ms.sendSubTitle(playerA.getPlayer(), "§fDeine Belohnung: §e§l" + ms.getNumberFormat(wonCoins) + " MuxCoins", 0, 60, 10);
                            }
                        }, 60L);
                } else if (winner == playerB) {
                    ms.sendTitle(playerB, "§c§lMux1vs1", 10, 60, 10);
                    ms.sendSubTitle(playerB, "§fDu hast den Kampf §agewonnen§f!", 10, 60, 10);
                    ms.showItemBar(playerA, "§cDu hast den Kampf verloren.");
                    if (wonCoins > 0 && quit == false)
                        ms.runLater(new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (playerB.isOnline() == false) return;
                                ms.sendSubTitle(playerB.getPlayer(), "§fDeine Belohnung: §e§l" + ms.getNumberFormat(wonCoins) + " MuxCoins", 0, 60, 10);
                            }
                        }, 60L);
                }
            } else if (expired == false) {
                sendPlayerMessage("§c§lMux1vs1>§7 Der Kampf wurde von einem Admin §cgestoppt§7.");
            }
            final Integer[] dataA = backexpandfood.remove(playerA), dataB = backexpandfood.remove(playerB);
            if (playerA != null) {
                ms.removeFromBattle(playerA.getName());
                if (dataA != null) {
                    playerA.setFoodLevel(dataA[0]);
                    playerA.setLevel(dataA[1]);
                }
                playerA.getActivePotionEffects().stream().filter(potionEffect -> potionEffect.getType().equals(PotionEffectType.NIGHT_VISION) == false &&
                        potionEffect.getType().equals(PotionEffectType.JUMP) == false).forEach(potionEffect -> playerA.removePotionEffect(potionEffect.getType()));
                ms.getPerks().deactivatePerks(playerA.getName());
                ms.getPerks().activatePerks(playerA);
                if (playerA == winner) {
                    playerA.setNoDamageTicks(20);
                    if (playerA.getFireTicks() > 0) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (playerA.isOnline()) {
                                    playerA.setFireTicks(0);
                                }
                            }
                        }.runTaskLater(ms, 1L);
                    }
                    playerA.setHealth(20D);
                }
            }
            if (playerB != null) {
                ms.removeFromBattle(playerB.getName());
                if (dataB != null) {
                    playerB.setFoodLevel(dataB[0]);
                    playerB.setLevel(dataB[1]);
                }
                playerB.getActivePotionEffects().stream().filter(potionEffect -> potionEffect.getType().equals(PotionEffectType.NIGHT_VISION) == false &&
                        potionEffect.getType().equals(PotionEffectType.JUMP) == false).forEach(potionEffect -> playerB.removePotionEffect(potionEffect.getType()));
                ms.getPerks().deactivatePerks(playerB.getName());
                ms.getPerks().activatePerks(playerB);
                if (playerB == winner) {
                    playerB.setNoDamageTicks(20);
                    if (playerB.getFireTicks() > 0) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (playerB.isOnline())
                                    playerB.setFireTicks(0);
                            }
                        }.runTaskLater(ms, 1L);
                    }
                    playerB.setHealth(20D);
                }
            }
            finished = true;
            if (instant) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        stopGame(win);
                    }
                }.runTask(ms);
            } else {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        stopGame(win);
                    }
                }.runTaskLater(ms, 100L);
            }
        }

        private void stopGame(final boolean win) {
            if (win == false) {
                if (playerA != null && playerA.isOnline())
                    ms.resetScoreboard(playerA);
                if (playerB != null && playerB.isOnline())
                    ms.resetScoreboard(playerB);
            }
            for (final UUID uuid : spectators) {
                final Player pl = ms.getServer().getPlayer(uuid);
                if (pl != null) {
                    sendToLastLocation(pl);
                }
            }
            if (playerA == winner && playerA != null) {
                sendToLastLocation(playerA);
            } else if (playerB == winner && playerB != null) {
                sendToLastLocation(playerB);
            } else {
                if (playerA != null)
                    sendToLastLocation(playerA);
                if (playerB != null)
                    sendToLastLocation(playerB);
            }
            if (inventoryA != null) {
                if (playerA != null && playerA.isOnline())
                    inventoryA.receive(playerA);
                inventoryA = null;
            }
            if (inventoryB != null) {
                if (playerB != null && playerB.isOnline())
                    inventoryB.receive(playerB);
                inventoryB = null;
            }
            if (ms.getAdmin()._1VS1.isActive()) {
                if (playerA.isOnline())
                    ms.chatClickHoverRun(playerA, "§c§lMux1vs1>§7 Klicke §chier§7, um nochmal zu kämpfen.", "§c§oKlicke zum Kämpfen", "/1vs1");
                if (playerB.isOnline())
                    ms.chatClickHoverRun(playerB, "§c§lMux1vs1>§7 Klicke §chier§7, um nochmal zu kämpfen.", "§c§oKlicke zum Kämpfen", "/1vs1");
            }
            kit.onArenaEnd(this);
            playerB = null;
            playerA = null;
            rankedArenas.remove(this);
            updateSpectateInv();
            handleRankedQueue();
        }

        public void startGame() {
            startTime = System.currentTimeMillis();
            kit.receiveKit(playerA);
            kit.receiveKit(playerB);
            kit.onEquip(this);
            if (kit.strength) {
                if (ms.getExtras().getExtraUser(playerA.getUniqueId()).getPerks().getOwned().contains((byte) 7))
                    ms.getPerks().activatePerk(playerA, (byte) 7);
                if (ms.getExtras().getExtraUser(playerB.getUniqueId()).getPerks().getOwned().contains((byte) 7))
                    ms.getPerks().activatePerk(playerB, (byte) 7);
            }
            sendScoreboard(playerA, EnumPvPScoreboardType.RANKED);
            sendScoreboard(playerB, EnumPvPScoreboardType.RANKED);
            final String mapname = "ranked" + map.replace("ä", "a").replace("ö", "o").replace("ü", "u");
            if (kit.bodyfix) {
                ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "region flag " + mapname + " -w " + locA.getWorld().getName() + " blocked-cmds");
            } else {
                ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "region flag " + mapname + " -w " + locA.getWorld().getName() + " blocked-cmds fix,repair");
            }
            for (final Item item : locA.getWorld().getEntitiesByClass(Item.class)) {
                if (item.getLocation().distance(locA) < 70 || item.getLocation().distance(locB) < 70) {
                    item.remove();
                }
            }
            new BukkitRunnable() {
                short seconds = 5;

                @Override
                public void run() {
                    if (playerA == null || playerB == null) {
                        cancel();
                        return;
                    } else if (seconds > 0) {
                        playSound(Sound.NOTE_STICKS, 0F);
                        sendTitle("§fDer Kampf beginnt in §6" + seconds + " Sekunde" + (seconds > 1 ? "n" : "") + "§f...", 0);
                        seconds--;
                        return;
                    }
                    sendTitle("§fDer Kampf beginnt §6jetzt§f, viel Glück!", 0);
                    updateSpectateInv();
                    setGates(true);
                    started = true;
                    playSound(Sound.ZOMBIE_WOODBREAK, 1F);
                    cancel();
                }
            }.runTaskTimer(ms, 0L, 20L);
        }

        public void disableArena() {
            setGates(false);
            clearArena(false, true, false, false);
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
            gate.clone().add(0, 1, 1).getBlock().setType(m);
            gate.clone().add(0, 1, -1).getBlock().setType(m);
            gate.clone().add(0, 1, 2).getBlock().setType(m);
            gate.clone().add(0, 1, -2).getBlock().setType(m);
            gate.clone().add(0, 2, 0).getBlock().setType(m);
            gate.clone().add(0, 2, 1).getBlock().setType(m);
            gate.clone().add(0, 2, -1).getBlock().setType(m);
            gate.clone().add(0, 2, 2).getBlock().setType(m);
            gate.clone().add(0, 2, -2).getBlock().setType(m);
        }

        public boolean loadLocations() {
            this.locA = ms.getLoc("ranked" + this.map + "spawn1");
            this.locB = ms.getLoc("ranked" + this.map + "spawn2");
            this.gateA = ms.getLoc("ranked" + this.map + "gate1");
            this.gateB = ms.getLoc("ranked" + this.map + "gate2");
            this.spectate = ms.getLoc("ranked" + this.map + "spectate");
            if (this.locA == null || this.locB == null || this.gateA == null || this.gateB == null || this.spectate == null) {
                System.err.println("MuxSystem> Error: Could not load 1vs1 Map Locations of the Map " + this.map.toLowerCase());
                return false;
            }
            setGates(false);
            return true;
        }

        public boolean handleDeath(final Player p, final boolean quit) {
            if (isFinished()) {
                if (quit) {
                    sendToLastLocation(p);
                    if (isPlayerA(p) && inventoryA != null) {
                        inventoryA.receive(p);
                        inventoryA = null;
                    } else if (isPlayerB(p) && inventoryB != null) {
                        inventoryB.receive(p);
                        inventoryB = null;
                    }
                }
                return false;
            } else if (lostA(p) == false && lostB(p) == false) {
                return false;
            }
            ms.resetScoreboard(playerA == p ? playerB : playerA);
            ms.getStats().handleKill(p, playerA == p ? playerB : playerA, false, true);
            if (quit == false) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        p.spigot().respawn();
                    }
                }.runTaskLater(ms, 5L);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (isPlayerA(p) && inventoryA != null) {
                            inventoryA.receive(p);
                            inventoryA = null;
                        } else if (isPlayerB(p) && inventoryB != null) {
                            inventoryB.receive(p);
                            inventoryB = null;
                        }
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                sendToLastLocation(p);
                            }
                        }.runTaskLater(ms, 20L);
                    }
                }.runTaskLater(ms, 10L);
            } else {
                if (isPlayerA(p) && inventoryA != null) {
                    inventoryA.receive(p);
                    inventoryA = null;
                } else if (isPlayerB(p) && inventoryB != null) {
                    inventoryB.receive(p);
                    inventoryB = null;
                }
                if (isPlayerA(p) || isPlayerB(p)) {
                    sendToLastLocation(p);
                }
            }
            if (isPlayerA(p) == false) {
                playerA.getInventory().clear();
                playerA.getInventory().setArmorContents(null);
            } else {
                playerB.getInventory().clear();
                playerB.getInventory().setArmorContents(null);
            }
            if (winner != null) {
                final MuxUser u = ms.getMuxUser(winner.getName());
                int coinsToAdd = (int) (ms.getActiveMuxCoinsSupply() * 0.0005D / 100);
                if (quit == false) {
                    switch (ms.getStats().getLeague(u.getTrophies(), u.getLastRankedMatch() > System.currentTimeMillis() - 172800000, u.getLastRankedMatch() != 0).toLowerCase()) {
                        case "bronze":
                            coinsToAdd *= 1.5;
                            break;
                        case "silber":
                            coinsToAdd *= 2.0;
                            break;
                        case "gold":
                            coinsToAdd *= 2.5;
                            break;
                        case "master":
                            coinsToAdd *= 3;
                            break;
                        default:
                            break;
                    }
                    u.addCoins(wonCoins = coinsToAdd);
                    ms.getHistory().addHistory(winner.getUniqueId(), null, "COINS", String.valueOf(wonCoins), "1vs1");
                    ms.getAnalytics().addPvPExpenses(coinsToAdd);
                }
                u.updateLastRankedMatch();
                ms.saveMuxUser(u);
                final MuxUser u2 = ms.getMuxUser(isPlayerA(winner) ? playerB.getName() : playerA.getName());
                u2.updateLastRankedMatch();
                ms.saveMuxUser(u2);
            }
            playSound(Sound.AMBIENCE_THUNDER, 1F);
            clearArena(true, false, false, quit);
            return false;
        }
    }


    class SavedPlayerInventory {
        private ItemStack[] armorContents, contents;

        public SavedPlayerInventory(final Player toSave) {
            this.armorContents = toSave.getInventory().getArmorContents().clone();
            this.contents = toSave.getInventory().getContents().clone();
        }

        public void receive(final Player receiver) {
            receiver.getInventory().setContents(contents);
            receiver.getInventory().setArmorContents(armorContents);
            this.contents = null;
            this.armorContents = null;
        }
    }

    public void joinRankedQueue(final Player p) {
        if (ms.getAdmin()._1VS1.isActive() == false && p.isOp() == false) {
            ms.showItemBar(p, "§c1vs1 ist derzeit nicht verfügbar.");
            return;
        } else if (ms.getAdmin().pvpIsActive() == false && p.isOp() == false) {
            ms.showItemBar(p, "§cPvP ist momentan deaktiviert.");
            return;
        } else if (p.hasPermission("muxsystem.vip") == false && ms.isVPN(p.getAddress().getAddress().getHostAddress()) == 1) {
            ms.sendNoVPNMessage(p, "VIP");
            return;
        }
        final LocalDateTime current = LocalDateTime.now(ZoneId.of("CET"));

        if (ms.isDebug() == false) {
            if (current.isBefore(rankedTimes[0]) || current.isAfter(rankedTimes[1])) {
                ms.showItemBar(p, "§cDie 1vs1 Warteschlange ist täglich von 14:00-22:00 Uhr geöffnet.");
                return;
            } else if (rankedCloseTask == null) {
                rankedCloseTask = new BukkitRunnable() {
                    final ZonedDateTime dateTime = ZonedDateTime.of(rankedTimes[1].getYear(), rankedTimes[1].getMonth().getValue(), rankedTimes[1].getDayOfMonth(), rankedTimes[1].getHour(), rankedTimes[1].getMinute(), rankedTimes[1].getSecond(), 0, ZoneId.of("CET"));
                    final long endTime = dateTime.toInstant().toEpochMilli();

                    @Override
                    public void run() {
                        if (endTime > System.currentTimeMillis()) {
                            return;
                        }
                        rankedQueue.forEach(player -> player.sendMessage("§c§lMux1vs1>§7 1vs1 wurde §cgeschlossen§7, du wurdest aus der Warteschlange entfernt."));
                        rankedQueue.clear();
                        cancel();
                    }
                }.runTaskTimer(ms, 100L, 200L);
            }
        }
        if (rankedQueue.remove(p)) {
            ms.showItemBar(p, "§fDu hast die Warteschlange §cverlassen§f.");
            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
            return;
        }
        final MuxUser u = ms.getMuxUser(p.getName());
        if (ms.getBans().checkPvPBan(p, true)) {
            return;
        }
        if (ms.hasVoted(p) == false && (ms.getNewbies().isNewbie(p) == false || ms.getDB().getPlayersWithSameIP(u.getIp(), p.getName()).size() > 1)) {
            if (ms.checkGeneralCooldown(p.getName(), "1v1NEEDVOTE", 5000, true)) {
                ms.chatClickHoverLink(p, ms.getLang("vote.tounlock"), ms.getLang("vote.hoverunlock"), "https://muxcraft.eu/vote/?ign=" + p.getName());
                ms.chatClickHoverLink(p, ms.getLang("vote.tounlock2"), ms.getLang("vote.hoverunlock"), "https://muxcraft.eu/vote/?ign=" + p.getName());
            }
            return;
        }
        if (u.getLastRankedMatch() <= 0 && rankedBookCache.contains(p) == false) {
            final int integer = r.nextInt(5000);
            if (p.hasMetadata("1vs1unlock")) p.removeMetadata("1vs1unlock", ms);
            p.setMetadata("1vs1unlock", new FixedMetadataValue(ms, integer));
            final ItemStack book = ms.cmdBook("§4§l1vs1", "In diesem Spielmodus kämpfst du mit einem zufälligen Kit gegen einen anderen Spieler. Der Gewinner erhält Trophäen und MuxCoins.", new Object[]{"/1vs1 unlock " + integer, "Klicke hier!", "Klicke zum Teilnehmen"});
            ms.openBook(book, p);
            return;
        }
        rankedBookCache.remove(p);
        String unusedMap = null;
        for (final String map : rankedMaps) {
            if (rankedArenas.stream().noneMatch(arena -> arena.getMap().equals(map))) {
                unusedMap = map;
                break;
            }
        }
        if (rankedQueue.size() < 3 || unusedMap == null) {
            ms.chatClickHoverRun(p, "§c§lMux1vs1>§7 Klicke §chier§7, um die Warteschlange zu §cverlassen§7.", "§c§oKlicke zum Verlassen", "/1vs1 leave");
        }
        rankedQueue.add(p);
        handleRankedQueue();
        if (rankedQueue.contains(p)) {
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 0.7F);
            ms.showItemBar(p, "§fDu bist der Warteschlange §abeigetreten§f.");
        }
    }

    public void handleRankedQueue() {
        rankedQueue.removeIf(player -> player == null || player.isOnline() == false || in1vs1(player) || ms.inBattle(player.getName(), player.getLocation()) || ms.inWar(player) || ms.inDuel(player) || ms.inEvent(player) ||
                ms.isBeingTeleported(player.getName()) || ms.inGame(player) || player.isDead() || ms.getBans().checkPvPBan(player, false) || ms.inCasino(player) || player.getGameMode() == GameMode.SPECTATOR
                || ms.getTrades().isTrading(player.getName()));
        if ((rankedQueue.size() < (ms.isDebug() ? 2 : 3))) return;
        String unusedMap = null;
        final List<String> shuffledMaps = new ArrayList<>(rankedMaps);
        Collections.shuffle(shuffledMaps);
        for (final String map : shuffledMaps) {
            if (rankedArenas.stream().noneMatch(arena -> arena.getMap().equalsIgnoreCase(map))) {
                unusedMap = map;
                break;
            }
        }
        if (unusedMap == null || rankedKits.isEmpty()) return;
        EnumRankedKitRarity rarity;
        final int chance = r.nextInt(101);
        if (chance <= EnumRankedKitRarity.LEGENDARY.getChance())
            rarity = EnumRankedKitRarity.LEGENDARY;
        else if (chance <= EnumRankedKitRarity.RARE.getChance())
            rarity = EnumRankedKitRarity.RARE;
        else if (chance <= EnumRankedKitRarity.UNCOMMON.getChance())
            rarity = EnumRankedKitRarity.UNCOMMON;
        else
            rarity = EnumRankedKitRarity.COMMON;
        final RankedKit kit = rankedKits.get(rarity).get(r.nextInt(rankedKits.get(rarity).size()));
        final RankedArena arena = new RankedArena(unusedMap, kit);
        final Player playerA = rankedQueue.remove();
        final List<Player> remainingPlayers = new ArrayList<>(rankedQueue);
        final Player playerB = remainingPlayers.get(r.nextInt(remainingPlayers.size()));
        rankedQueue.remove(playerB);
        this.prepare1vs1Teleport(playerA);
        this.prepare1vs1Teleport(playerB);
        arena.playerA = playerA;
        arena.playerB = playerB;
        rankedArenas.add(arena);
        ms.forcePlayer(playerA, arena.getLocA());
        ms.forcePlayer(playerB, arena.getLocB());
        arena.inventoryA = new SavedPlayerInventory(playerA);
        arena.inventoryB = new SavedPlayerInventory(playerB);
        ms.getAnalytics().getAnalytics().addOneVsOnePlayer(playerA.getUniqueId());
        ms.getAnalytics().getAnalytics().addOneVsOnePlayer(playerB.getUniqueId());
        ms.getAnalytics().getAnalytics().incrementOnevsOneGames();
        arena.startGame();
    }

    public void prepare1vs1Teleport(final Player p) {
        p.closeInventory();
        if (ms.isVanish(p)) {
            p.performCommand("vanish");
        }
        ms.getMenu().disableForcefield(p);
        if (p.isInsideVehicle()) {
            p.leaveVehicle();
        }

        p.setAllowFlight(false);
        p.setFlying(false);
        p.setGameMode(GameMode.SURVIVAL);
        ms.removeGodMode(p.getName());

        p.setFireTicks(0);
        backexpandfood.put(p, new Integer[]{p.getFoodLevel(), p.getLevel()});
        p.setExp(0F);
        p.setLevel(0);
        p.setSaturation(20);
        p.setFoodLevel(20);
        p.setWalkSpeed(0.2F);
        p.setHealth(20D);
        p.setNoDamageTicks(20);
        ms.getEmojis().stopEmoji(p);
        ms.getPerks().activatePerk(p, (byte) 1);
        p.getActivePotionEffects().stream().filter(potionEffect -> potionEffect.getType().equals(PotionEffectType.NIGHT_VISION) == false
                && potionEffect.getType().equals(PotionEffectType.JUMP) == false).forEach(potionEffect -> p.removePotionEffect(potionEffect.getType()));
        ms.setLastLocation(p);
    }

    public abstract class RankedKit {
        private final String name;
        protected final KitInventory inventory;
        private final long tickTime, stopTime;
        private final boolean bodyfix;
        protected boolean strength = false;

        public RankedKit(final String name, final long tickTime, final boolean bodyfix, final EnumRankedKitRarity rarity, final long stopTime) {
            this.name = name;
            this.tickTime = tickTime;
            this.bodyfix = bodyfix;
            this.inventory = new KitInventory();
            this.stopTime = stopTime;
            rankedKits.get(rarity).add(this);
            setup();
        }

        private void receiveKit(final Player player) {
            player.getInventory().setContents(inventory.inventory);
            player.getInventory().setArmorContents(inventory.armor);
        }

        abstract void setup();

        public void onDamageByEntity(final EntityDamageByEntityEvent event, Player player, Player damager, RankedArena arena) {
        }

        public void onDamage(final EntityDamageEvent event, final Player player, final RankedArena arena) {
        }

        void onTick(final RankedArena arena) {
        }

        void onEquip(final RankedArena arena) {
        }

        void onArenaEnd(final RankedArena arena) {
        }

        public void onBlockPlace(final BlockPlaceEvent e, final Player player, final ItemStack i, final Material m, final RankedArena arena) {
        }

        class KitInventory {
            private ItemStack[] inventory, armor;

            private KitInventory() {
                this.inventory = new ItemStack[36];
                this.armor = new ItemStack[4];
                Arrays.fill(inventory, 0, inventory.length, new ItemStack(Material.AIR));
                Arrays.fill(armor, 0, armor.length, new ItemStack(Material.AIR));
            }

            public void setInventory(final ItemStack[] inventory) {
                this.inventory = inventory;
            }

            public void setArmor(final ItemStack[] armor) {
                this.armor = armor;
            }

            public ItemStack[] getInventory() {
                return inventory.clone();
            }

            public ItemStack[] getArmor() {
                return armor.clone();
            }
        }
    }

    enum EnumRankedKitRarity {
        COMMON(50),
        UNCOMMON(30),
        RARE(25),
        LEGENDARY(5);

        private final int chance;

        EnumRankedKitRarity(final int chance) {
            this.chance = chance;
        }

        public int getChance() {
            return chance;
        }
    }

    public class StackedKit extends RankedKit {
        public StackedKit() {
            super("Stacked", 5000L, true, EnumRankedKitRarity.COMMON, 600000L);
            strength = true;
        }

        @Override
        void setup() {
            final ItemStack[] inventory = this.inventory.inventory, armor = this.inventory.armor;
            armor[3] = new ItemStack(Material.DIAMOND_HELMET);
            armor[2] = new ItemStack(Material.DIAMOND_CHESTPLATE);
            armor[1] = new ItemStack(Material.DIAMOND_LEGGINGS);
            armor[0] = new ItemStack(Material.DIAMOND_BOOTS);
            for (final ItemStack itemStack : armor) {
                itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                itemStack.addEnchantment(Enchantment.DURABILITY, 3);
            }
            final ItemStack diamondSword = new ItemStack(Material.DIAMOND_SWORD),
                    healPot = new ItemStack(Material.POTION, 64, (short) 16421),
                    regenerationPot = new ItemStack(Material.POTION, 64, (short) 16417),
                    speedPot = new ItemStack(Material.POTION, 64, (short) 16418),
                    strengthPot = new ItemStack(Material.POTION, 64, (short) 16425),
                    fireResiPot = new ItemStack(Material.POTION, 64, (short) 16451);
            diamondSword.addEnchantment(Enchantment.DAMAGE_ALL, 5);
            diamondSword.addEnchantment(Enchantment.DURABILITY, 3);
            diamondSword.addEnchantment(Enchantment.FIRE_ASPECT, 2);
            inventory[0] = diamondSword;
            inventory[1] = healPot.clone();
            inventory[2] = healPot.clone();
            inventory[4] = fireResiPot;
            inventory[5] = strengthPot;
            inventory[6] = regenerationPot;
            inventory[7] = speedPot;
        }
    }

    public class OneStackHealKit extends RankedKit {
        public OneStackHealKit() {
            super("1 Stack", 5000L, true, EnumRankedKitRarity.COMMON, 600000L);
            strength = true;
        }

        @Override
        void setup() {
            final ItemStack[] inventory = this.inventory.inventory, armor = this.inventory.armor;
            armor[3] = new ItemStack(Material.DIAMOND_HELMET);
            armor[2] = new ItemStack(Material.DIAMOND_CHESTPLATE);
            armor[1] = new ItemStack(Material.DIAMOND_LEGGINGS);
            armor[0] = new ItemStack(Material.DIAMOND_BOOTS);
            for (final ItemStack itemStack : armor) {
                itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                itemStack.addEnchantment(Enchantment.DURABILITY, 3);
            }
            final ItemStack diamondSword = new ItemStack(Material.DIAMOND_SWORD),
                    healPot = new ItemStack(Material.POTION, 64, (short) 16421),
                    regenerationPot = new ItemStack(Material.POTION, 64, (short) 16417),
                    speedPot = new ItemStack(Material.POTION, 64, (short) 16418),
                    strengthPot = new ItemStack(Material.POTION, 64, (short) 16425),
                    fireResiPot = new ItemStack(Material.POTION, 64, (short) 16451);
            diamondSword.addEnchantment(Enchantment.DAMAGE_ALL, 5);
            diamondSword.addEnchantment(Enchantment.DURABILITY, 3);
            diamondSword.addEnchantment(Enchantment.FIRE_ASPECT, 2);
            inventory[0] = diamondSword;
            inventory[1] = healPot.clone();
            inventory[4] = fireResiPot;
            inventory[5] = strengthPot;
            inventory[6] = regenerationPot;
            inventory[7] = speedPot;
        }
    }

    public class LeatherKit extends RankedKit {
        public LeatherKit() {
            super("Leder Rüstung", 2000L, true, EnumRankedKitRarity.COMMON, 300000L);
        }

        @Override
        void setup() {
            final ItemStack[] inventory = this.inventory.inventory, armor = this.inventory.armor;
            armor[3] = new ItemStack(Material.LEATHER_HELMET);
            armor[2] = new ItemStack(Material.LEATHER_CHESTPLATE);
            armor[1] = new ItemStack(Material.LEATHER_LEGGINGS);
            armor[0] = new ItemStack(Material.LEATHER_BOOTS);
            for (final ItemStack itemStack : armor) {
                itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                itemStack.addEnchantment(Enchantment.DURABILITY, 3);
            }
            final ItemStack sword = new ItemStack(Material.WOOD_SWORD);
            inventory[0] = sword;
        }
    }

    public class BowKit extends RankedKit {
        public BowKit() {
            super("Bogen", 2000L, true, EnumRankedKitRarity.UNCOMMON, 300000L);
        }

        @Override
        void setup() {
            final ItemStack[] inventory = this.inventory.inventory, armor = this.inventory.armor;
            armor[3] = new ItemStack(Material.LEATHER_HELMET);
            armor[2] = new ItemStack(Material.LEATHER_CHESTPLATE);
            armor[1] = new ItemStack(Material.LEATHER_LEGGINGS);
            armor[0] = new ItemStack(Material.LEATHER_BOOTS);
            for (final ItemStack itemStack : armor) {
                itemStack.addEnchantment(Enchantment.DURABILITY, 2);
            }
            final ItemStack bow = new ItemStack(Material.BOW);
            bow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
            inventory[0] = bow;
            inventory[9] = new ItemStack(Material.ARROW);
        }
    }

    public class DamagePotionKit extends RankedKit {
        public DamagePotionKit() {
            super("DamagePots", 2000L, true, EnumRankedKitRarity.LEGENDARY, 300000L);
        }

        @Override
        void setup() {
            final ItemStack[] inventory = this.inventory.inventory, armor = this.inventory.armor;
            armor[3] = new ItemStack(Material.DIAMOND_HELMET);
            armor[2] = new ItemStack(Material.DIAMOND_CHESTPLATE);
            armor[1] = new ItemStack(Material.DIAMOND_LEGGINGS);
            armor[0] = new ItemStack(Material.DIAMOND_BOOTS);
            for (final ItemStack itemStack : armor) {
                itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                itemStack.addEnchantment(Enchantment.DURABILITY, 3);
            }
            inventory[0] = new ItemStack(Material.POTION, 64, (short) 16428);
            inventory[1] = new ItemStack(Material.POTION, 64, (short) 16428);
            inventory[2] = new ItemStack(Material.POTION, 64, (short) 16428);
            inventory[3] = new ItemStack(Material.POTION, 64, (short) 16428);
            inventory[4] = new ItemStack(Material.POTION, 64, (short) 16428);
        }
    }

    public class KnockbackKit extends RankedKit {
        public KnockbackKit() {
            super("Knockback II", 5000L, true, EnumRankedKitRarity.RARE, 600000L);
            strength = true;
        }

        @Override
        void setup() {
            final ItemStack[] inventory = this.inventory.inventory, armor = this.inventory.armor;
            armor[3] = new ItemStack(Material.DIAMOND_HELMET);
            armor[2] = new ItemStack(Material.DIAMOND_CHESTPLATE);
            armor[1] = new ItemStack(Material.DIAMOND_LEGGINGS);
            armor[0] = new ItemStack(Material.DIAMOND_BOOTS);
            for (final ItemStack itemStack : armor) {
                itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                itemStack.addEnchantment(Enchantment.DURABILITY, 3);
            }
            final ItemStack diamondSword = new ItemStack(Material.DIAMOND_SWORD),
                    healPot = new ItemStack(Material.POTION, 64, (short) 16421),
                    regenerationPot = new ItemStack(Material.POTION, 64, (short) 16417),
                    speedPot = new ItemStack(Material.POTION, 64, (short) 16418),
                    strengthPot = new ItemStack(Material.POTION, 64, (short) 16425),
                    fireResiPot = new ItemStack(Material.POTION, 64, (short) 16451);
            diamondSword.addEnchantment(Enchantment.DAMAGE_ALL, 5);
            diamondSword.addEnchantment(Enchantment.DURABILITY, 3);
            diamondSword.addEnchantment(Enchantment.KNOCKBACK, 2);
            diamondSword.addEnchantment(Enchantment.FIRE_ASPECT, 2);
            inventory[0] = diamondSword;
            inventory[1] = healPot.clone();
            inventory[2] = healPot.clone();
            inventory[4] = fireResiPot;
            inventory[5] = strengthPot;
            inventory[6] = regenerationPot;
            inventory[7] = speedPot;
        }
    }

    public class IFixKit extends RankedKit {
        public IFixKit() {
            super("iFix", 5000L, false, EnumRankedKitRarity.UNCOMMON, 600000L);
            strength = true;
        }

        @Override
        void setup() {
            final ItemStack[] inventory = this.inventory.inventory, armor = this.inventory.armor;
            armor[3] = new ItemStack(Material.DIAMOND_HELMET);
            armor[2] = new ItemStack(Material.DIAMOND_CHESTPLATE);
            armor[1] = new ItemStack(Material.DIAMOND_LEGGINGS);
            armor[0] = new ItemStack(Material.DIAMOND_BOOTS);
            for (final ItemStack itemStack : armor) {
                itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                itemStack.addEnchantment(Enchantment.DURABILITY, 3);
            }
            final ItemStack diamondSword = new ItemStack(Material.DIAMOND_SWORD);
            diamondSword.addEnchantment(Enchantment.DAMAGE_ALL, 5);
            diamondSword.addEnchantment(Enchantment.DURABILITY, 3);
            diamondSword.addEnchantment(Enchantment.FIRE_ASPECT, 2);
            final ItemStack healPot = new ItemStack(Material.POTION, 64, (short) 16421),
                    regenerationPot = new ItemStack(Material.POTION, 64, (short) 16417),
                    speedPot = new ItemStack(Material.POTION, 64, (short) 16418),
                    strengthPot = new ItemStack(Material.POTION, 64, (short) 16425),
                    fireResiPot = new ItemStack(Material.POTION, 64, (short) 16451);
            inventory[0] = diamondSword;
            inventory[1] = healPot.clone();
            inventory[2] = healPot.clone();
            inventory[4] = fireResiPot;
            inventory[5] = strengthPot;
            inventory[6] = regenerationPot;
            inventory[7] = speedPot;
        }

        @Override
        void onEquip(final RankedArena arena) {
            arena.sendPlayerMessage("§c§lMux1vs1>§c §4§lAchtung!§c Bei diesem Kit musst du deine Rüstung zum reparieren ausziehen.");
        }
    }

    public class OneHitKit extends RankedKit {
        public OneHitKit() {
            super("Erster Schlag", 2000L, true, EnumRankedKitRarity.RARE, 300000L);
        }

        @Override
        void setup() {
            final ItemStack[] inventory = this.inventory.inventory;
            final ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
            sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 10);
            inventory[0] = sword;
        }

        @Override
        void onEquip(final RankedArena arena) {
            arena.sendPlayerMessage("§c§lMux1vs1>§7 Achtung! Der §4erste Schlag tötet dich.");
        }
    }

    public class BoxingKit extends RankedKit {
        private final Map<Player, Integer> hits = new HashMap<>();
        private final Map<Player, Long> lastHit = new HashMap<>();

        public BoxingKit() {
            super("Boxing", 200L, true, EnumRankedKitRarity.COMMON, 300000L);
        }

        @Override
        void setup() {
            final ItemStack[] inventory = this.inventory.inventory;
            final ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
            sword.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 1);
            inventory[0] = sword;
            inventory[7] = new ItemStack(Material.POTION, 64, (short) 16418);
        }

        @Override
        public void onDamageByEntity(final EntityDamageByEntityEvent event, final Player player, final Player damager, final RankedArena arena) {
            if (event.isCancelled()) return;
            event.setDamage(0);
            final Integer hit = hits.get(damager);
            if (hit == null) return;
            Long lastHit = this.lastHit.get(player);
            if (lastHit != null && lastHit > System.currentTimeMillis()) return;
            this.lastHit.put(player, System.currentTimeMillis() + 340);
            int total;
            hits.put(damager, total = hit + 1);
            if (total == 100) {
                event.setDamage(100D);
            }
        }

        @Override
        public void onDamage(final EntityDamageEvent event, final Player player, final RankedArena arena) {
            if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK)
                event.setCancelled(true);
        }

        @Override
        void onTick(final RankedArena arena) {
            if (arena.playerA == null || arena.playerA.isOnline() == false || arena.playerB == null || arena.playerB.isOnline() == false)
                return;
            ms.showItemBar(arena.playerA, "§a" + arena.playerA.getName() + "> §f" + hits.get(arena.playerA) + "    §c" + arena.playerB.getName() + "> §f" + hits.get(arena.playerB));
            ms.showItemBar(arena.playerB, "§a" + arena.playerB.getName() + "> §f" + hits.get(arena.playerB) + "    §c" + arena.playerA.getName() + "> §f" + hits.get(arena.playerA));
        }

        @Override
        void onEquip(final RankedArena arena) {
            hits.put(arena.playerA, 0);
            hits.put(arena.playerB, 0);
            arena.sendPlayerMessage("§c§lMux1vs1>§7 Der erste Spieler mit §a100 Schlägen §7gewinnt.");
        }

        @Override
        void onArenaEnd(final RankedArena arena) {
            hits.remove(arena.playerA);
            hits.remove(arena.playerB);
            lastHit.remove(arena.playerA);
            lastHit.remove(arena.playerB);
        }
    }

    public class OneVsOneKit extends RankedKit {
        public OneVsOneKit() {
            super("1vs1", 2000L, true, EnumRankedKitRarity.UNCOMMON, 300000L);
        }

        @Override
        void setup() {
            final ItemStack iH = new ItemStack(Material.LEATHER_HELMET), iC = new ItemStack(Material.LEATHER_CHESTPLATE), iL = new ItemStack(Material.LEATHER_LEGGINGS), iB = new ItemStack(Material.LEATHER_BOOTS),
                    iS = new ItemStack(Material.STONE_SWORD), iBow = new ItemStack(Material.BOW), iArrow = new ItemStack(Material.ARROW), iSnowball = new ItemStack(Material.SNOW_BALL, 2);
            iH.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 3);
            iH.addEnchantment(Enchantment.OXYGEN, 3);
            iC.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
            iL.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
            iB.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
            iB.addEnchantment(Enchantment.PROTECTION_FALL, 1);
            iS.addEnchantment(Enchantment.DAMAGE_UNDEAD, 2);
            iS.addEnchantment(Enchantment.FIRE_ASPECT, 1);
            iBow.addEnchantment(Enchantment.ARROW_FIRE, 1);
            iBow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
            iBow.addEnchantment(Enchantment.ARROW_KNOCKBACK, 1);
            final ItemStack[] inventory = this.inventory.inventory, armor = this.inventory.armor;
            armor[3] = iH;
            armor[2] = iC;
            armor[1] = iL;
            armor[0] = iB;
            inventory[0] = iS;
            inventory[1] = iBow;
            inventory[2] = iArrow;
            inventory[3] = iSnowball;
        }
    }

    public class WoodSwordKit extends RankedKit {
        public WoodSwordKit() {
            super("Holz Schwert", 2000L, true, EnumRankedKitRarity.UNCOMMON, 300000L);
        }

        @Override
        void setup() {
            final ItemStack[] inventory = this.inventory.inventory;
            inventory[0] = new ItemStack(Material.WOOD_SWORD);
        }
    }

    public class AxePvPKit extends RankedKit {
        public AxePvPKit() {
            super("Schärfe 5 Axt", 5000L, true, EnumRankedKitRarity.COMMON, 600000L);
            strength = true;
        }

        @Override
        void setup() {
            final ItemStack[] inventory = this.inventory.inventory, armor = this.inventory.armor;
            armor[3] = new ItemStack(Material.DIAMOND_HELMET);
            armor[2] = new ItemStack(Material.DIAMOND_CHESTPLATE);
            armor[1] = new ItemStack(Material.DIAMOND_LEGGINGS);
            armor[0] = new ItemStack(Material.DIAMOND_BOOTS);
            for (final ItemStack itemStack : armor) {
                itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
                itemStack.addEnchantment(Enchantment.DURABILITY, 3);
            }
            final ItemStack diamondAxe = new ItemStack(Material.DIAMOND_AXE),
                    healPot = new ItemStack(Material.POTION, 64, (short) 16421),
                    regenerationPot = new ItemStack(Material.POTION, 64, (short) 16417),
                    speedPot = new ItemStack(Material.POTION, 64, (short) 16418),
                    strengthPot = new ItemStack(Material.POTION, 64, (short) 16425),
                    fireResiPot = new ItemStack(Material.POTION, 64, (short) 16451);
            diamondAxe.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 5);
            diamondAxe.addEnchantment(Enchantment.DURABILITY, 3);
            diamondAxe.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
            inventory[0] = diamondAxe;
            inventory[1] = healPot.clone();
            inventory[2] = healPot.clone();
            inventory[4] = fireResiPot;
            inventory[5] = strengthPot;
            inventory[6] = regenerationPot;
            inventory[7] = speedPot;
        }
    }

    public class ChainmailKit extends RankedKit {
        public ChainmailKit() {
            super("Kettenrüstung", 5000L, true, EnumRankedKitRarity.RARE, 300000L);
        }

        @Override
        void setup() {
            final ItemStack[] inventory = this.inventory.inventory, armor = this.inventory.armor;
            armor[3] = new ItemStack(Material.CHAINMAIL_HELMET);
            armor[2] = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
            armor[1] = new ItemStack(Material.CHAINMAIL_LEGGINGS);
            armor[0] = new ItemStack(Material.CHAINMAIL_BOOTS);
            for (final ItemStack itemStack : armor) {
                itemStack.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 1);
                itemStack.addEnchantment(Enchantment.DURABILITY, 2);
            }
            final ItemStack ironSword = new ItemStack(Material.IRON_SWORD), healPot = new ItemStack(Material.POTION, 2, (short) 16421),
                    regenerationPot = new ItemStack(Material.POTION, 1, (short) 16417);
            ironSword.addEnchantment(Enchantment.DAMAGE_ALL, 1);
            ironSword.addEnchantment(Enchantment.DURABILITY, 3);
            inventory[0] = ironSword;
            inventory[1] = healPot.clone();
            inventory[2] = healPot.clone();
            inventory[7] = regenerationPot;
            inventory[3] = new ItemStack(Material.TNT, 3);
        }

        @Override
        public void onDamage(final EntityDamageEvent event, final Player player, final RankedArena arena) {
            if (arena.finished || arena.started == false) {
                event.setCancelled(true);
            } else if (event.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) || event.getCause().equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) {
                event.setCancelled(false);
            }
        }

        @Override
        void onEquip(final RankedArena arena) {
            arena.playerA.setGameMode(GameMode.SURVIVAL);
            arena.playerB.setGameMode(GameMode.SURVIVAL);
        }

        @Override
        public void onBlockPlace(final BlockPlaceEvent e, final Player p, final ItemStack i, final Material m, final RankedArena arena) {
            if (arena.finished || arena.started == false) {
                e.setCancelled(true);
                return;
            }
            e.setCancelled(true);
            e.setBuild(false);
            p.getInventory().removeItem(new ItemStack(Material.TNT));
            p.getWorld().spawn(e.getBlockPlaced().getLocation().add(0, 0.2, 0), org.bukkit.entity.TNTPrimed.class).setFuseTicks(40);
        }
    }

    public class IronKit extends RankedKit {
        public IronKit() {
            super("Eisen Rüstung", 5000L, true, EnumRankedKitRarity.UNCOMMON, 300000L);
        }

        @Override
        void setup() {
            final ItemStack[] inventory = this.inventory.inventory, armor = this.inventory.armor;
            armor[3] = new ItemStack(Material.IRON_HELMET);
            armor[2] = new ItemStack(Material.IRON_CHESTPLATE);
            armor[1] = new ItemStack(Material.IRON_LEGGINGS);
            armor[0] = new ItemStack(Material.IRON_BOOTS);
            for (final ItemStack itemStack : armor) {
                itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
            }
            final ItemStack ironSword = new ItemStack(Material.IRON_SWORD), healPot = new ItemStack(Material.POTION, 5, (short) 16421);
            ironSword.addEnchantment(Enchantment.DAMAGE_ALL, 2);
            ironSword.addEnchantment(Enchantment.DURABILITY, 1);
            inventory[0] = ironSword;
            inventory[1] = new ItemStack(Material.FISHING_ROD);
            inventory[2] = healPot.clone();
            inventory[7] = new ItemStack(Material.GOLDEN_APPLE, 16);
        }
    }

    public class SoupKit extends RankedKit {
        public SoupKit() {
            super("SoupPvP", 5000L, true, EnumRankedKitRarity.UNCOMMON, 180000L);
        }

        @Override
        void setup() {
            final ItemStack[] inventory = this.inventory.inventory, armor = this.inventory.armor;
            armor[3] = new ItemStack(Material.IRON_HELMET);
            armor[2] = new ItemStack(Material.IRON_CHESTPLATE);
            armor[1] = new ItemStack(Material.IRON_LEGGINGS);
            armor[0] = new ItemStack(Material.IRON_BOOTS);
            for (final ItemStack itemStack : armor) {
                itemStack.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
            }
            final ItemStack ironSword = new ItemStack(Material.DIAMOND_SWORD);
            ironSword.addEnchantment(Enchantment.DAMAGE_ALL, 2);
            ironSword.addEnchantment(Enchantment.DURABILITY, 1);
            inventory[0] = ironSword;
            final ItemStack soup = new ItemStack(Material.MUSHROOM_SOUP);
            for (int i = 1; i < 9; i++) {
                inventory[i] = soup.clone();
            }
        }
    }
}