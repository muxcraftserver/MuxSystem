package me.muxteam.base;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.object.schematic.Schematic;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import me.muxteam.basic.MuxHolograms;
import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxInventory;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.pvp.MuxSeason;
import me.muxteam.ranks.MuxRanks;
import me.muxteam.shop.MuxShop;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Tuple;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class MuxBase {
    private boolean WORLD_IS_DISABLED = false;
    private final boolean loaded;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final MuxBaseHandler baseHandler;
    private final MuxGriefing baseRaid;
    private final MuxBaseBlockRegistry blockRegistry;
    private final MuxSystem ms;
    private final ItemStack redSkull;
    private Schematic schematic;

    private MuxScoreboard.ScoreboardType sb, griefSb, visitSb;

    private List<Location> freeBases = new ArrayList<>();
    private final List<Player> generating = new ArrayList<>();
    private final SortedMap<Integer, List<PlayerBase>> playerBasesByX = new TreeMap<>();
    private final Map<UUID, PlayerBase> playerBases = new ConcurrentHashMap<>();
    private final Map<Player, PlayerBase> currentLocations = new HashMap<>(), visitors = new ConcurrentHashMap<>();
    private final Map<Player, Map<UUID, Long>> invites = new HashMap<>(), tempInvites = new HashMap<>();
    private Map<PlayerBase, Integer> baseRanking = Maps.newHashMap();
    private List<PlayerBase> top10Ranking = new ArrayList<>();
    private final Set<PlayerBase> chunkCleanup = new CopyOnWriteArraySet<>();

    private final File basefile;
    private final FileConfiguration baseYML;

    private World world;
    private BukkitWorld worldAsBukkitWorld;
    private int lastX, lastZ;
    private final int minX = -49000, maxX = 49000, minZ = -49000, yLocation = 93; // maxZ is unlimited

    private int usedBases = 0, bases = 0;
    private long generalGeneratingCooldown, whitelist = -1;

    private MuxHolograms.Hologram hologram;
    private Map<Integer, UUID> top3Last8Hours = new HashMap<>();
    private Map<Integer, Map<UUID, RankingEntry>> rankingEntries = new HashMap<>();
    private final Map<String, MuxHolograms.Hologram> holograms = new HashMap<>();

    private final Queue<BlockPosition> uncheckedBannerPositions = new ConcurrentLinkedQueue<>();
    private final Map<Long, String> placedBanners = new HashMap<>();

    private boolean handling;

    public MuxBase(final MuxSystem ms) {
        this.ms = ms;
        this.blockRegistry = new MuxBaseBlockRegistry();
        this.basefile = new File(ms.getDataFolder(), "bases.yml");
        this.baseYML = YamlConfiguration.loadConfiguration(basefile);
        this.redSkull = ms.getHeadFromURL("https://textures.minecraft.net/texture/5fde3bfce2d8cb724de8556e5ec21b7f15f584684ab785214add164be7624b", "red");
        try {
            schematic = FaweAPI.load(new File(ms.getDataFolder() + "/base", "base.schematic"));
        } catch (final IOException e) {
            e.printStackTrace();
        }
        this.baseHandler = new MuxBaseHandler(ms, this);
        initScoreboard();
        generateWorld();

        ms.getDB().loadBases(this);
        this.baseRaid = new MuxGriefing(ms, this);
        startTimer();
        if (baseYML.get("rankingEntries") != null && baseYML.get("top3Last8Hours") != null) {
            final Gson gson = new Gson();
            rankingEntries = gson.fromJson(baseYML.getString("rankingEntries"), new TypeToken<HashMap<Integer, HashMap<UUID, RankingEntry>>>() {
            }.getType());
            top3Last8Hours = gson.fromJson(baseYML.getString("top3Last8Hours"), new TypeToken<HashMap<Integer, UUID>>() {
            }.getType());
        }
        if (baseYML.contains("winningTime")) {
            final long wintime = baseYML.getLong("winningTime");
            if ((wintime + 172800000L) > System.currentTimeMillis()) {
                final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));
                calendar.setTime(new Date(wintime - 300000L));
                Location lastPos;
                ms.getHolograms().addHologram("BaseWin", lastPos = new Location(ms.getGameSpawn().getWorld(), 21D, 80D, 6D), "§b§lBase Gewinner im " + calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.GERMANY));
                final List<String> winnersAsStringList = baseYML.getStringList("winners");
                short step = 0;
                for (int pos = 0; pos < Math.min(3, winnersAsStringList.size()); pos++) {
                    final PlayerBase base = playerBases.get(UUID.fromString(winnersAsStringList.get(pos)));
                    if (base == null) {
                        step++;
                        continue;
                    }
                    final long value = baseYML.getLong("winvalues." + base.getOwner().toString());
                    final OfflinePlayer op = ms.getServer().getOfflinePlayer(base.getOwner());
                    final boolean beta = ms.isBeta();
                    final String cashreward = " §a➡ §l" + (pos == 0 ? (beta ? "50" : "250") : pos == 1 ? (beta ? "20" : "100") : (beta ? "10" : "50")) + " EUR",
                            text = "§f" + op.getName() + " - §b" + ms.getLFormat(value) + cashreward;
                    holograms.put("BaseWin" + step, ms.getHolograms().addHologram("BaseWin" + step, (lastPos = lastPos.add(0, -0.4, 0)).clone(), text));
                    step++;
                }
            } else {
                baseYML.set("winningTime", null);
                baseYML.set("winners", null);
                baseYML.set("winvalues", null);
                baseYML.set("rankingEntries", null);
                baseYML.set("top3Last8Hours", null);
                rankingEntries.clear();
            }
        }
        loaded = true;
        new BukkitRunnable() {
            final Gson gson = new Gson();

            @Override
            public void run() {
                final String ncl = baseYML.getString("needChunkLoading");
                if (ncl == null) return;
                final List<String> needChunkLoading = gson.fromJson(ncl, new TypeToken<ArrayList<String>>() {
                }.getType());
                needChunkLoading.stream().map(UUID::fromString).map(playerBases::get).filter(Objects::nonNull).forEach(PlayerBase::loadChunks);
            }
        }.runTaskLater(ms, 20L);
        new BukkitRunnable() {
            @Override
            public void run() {
                ms.getDB().deleteOldBaseSchematics(); // server has to be loaded.
            }
        }.runTaskLaterAsynchronously(ms, 20L * 5L);
    }

    private void startTimer() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                ms.getTimings().ofAndStart("baseFullHandle");
                handle();
                if (isEnabled()) baseRaid.handleRaids();
                refreshRankingHologram();
                baseRaid.updateGriefInventory();
                ImmutableSet.copyOf(this.playerBases.values()).parallelStream()
                        .filter(PlayerBase::isChanged)
                        .forEach(playerBase -> {
                            playerBase.setChanged(false);
                            ms.getDB().saveBase(playerBase);
                        });
                if (uncheckedBannerPositions.isEmpty())
                    return;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        BlockPosition uncheckedPos;
                        while (uncheckedBannerPositions.isEmpty() == false) {
                            uncheckedPos = uncheckedBannerPositions.poll();
                            final Block block = world.getBlockAt(uncheckedPos.getX(), uncheckedPos.getY(), uncheckedPos.getZ());
                            final int id = block.getTypeId();
                            if (id == 176 || id == 177) {
                                final Banner banner = (Banner) block.getState();
                                final long longHash = uncheckedPos.asLong();
                                if (banner != null) {
                                    placedBanners.put(longHash, Material.BANNER.getId() + ":" + banner.getBaseColor().getDyeData());
                                } else
                                    placedBanners.remove(longHash);
                            }
                        }
                    }
                }.runTask(ms);
            } catch (Exception e) {
                ms.getLogger().warning("ERROR WHILE HANDLING BASES");
                e.printStackTrace();
            } finally {
                ms.getTimings().stop("baseFullHandle");
            }
        }, 2, 15, TimeUnit.SECONDS);
    }

    public void cleanupChunks() {
        if (this.handling)
            return;
        this.chunkCleanup.removeIf(playerBase -> {
            if (playerBase.isCheckingValue() || playerBase.isUpgrading() || playerBase.hasLoadedChunks() || currentLocations.containsValue(playerBase))
                return false;
            playerBase.getChunks().clear();
            return true;
        });
    }

    public void addToChunkCleanup(final PlayerBase base) {
        this.chunkCleanup.add(base);
    }

    public Queue<BlockPosition> getUncheckedBannerPositions() {
        return uncheckedBannerPositions;
    }

    public Map<Long, String> getPlacedBanners() {
        return placedBanners;
    }

    public void close() {
        scheduler.shutdown();
        if (loaded == false) return;
        if (ms.getServer().hasWhitelist() && (whitelist == -1)) {
            whitelist = System.currentTimeMillis();
            baseYML.set("whitelist", whitelist);
            try {
                baseYML.save(basefile);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
        playerBases.values().forEach(playerBase -> ms.getDB().saveBase(playerBase));
        saveToFile();
    }

    private void saveToFile() {
        final Gson gson = new Gson();
        baseYML.set("needChunkLoading", gson.toJson(playerBases.values().stream().filter(PlayerBase::hasLoadedChunks).map(playerBase -> playerBase.getOwner().toString()).collect(Collectors.toList())));
        baseYML.set("lastX", lastX);
        baseYML.set("lastZ", lastZ);
        baseYML.set("freeBases", gson.toJson(freeBases.stream().map(this::baseLocationToString).collect(Collectors.toList())));
        baseYML.set("bases", bases);
        baseYML.set("usedBases", usedBases);
        baseYML.set("whitelist", whitelist);
        if (rankingEntries.isEmpty() == false) {
            baseYML.set("rankingEntries", gson.toJson(rankingEntries));
            baseYML.set("top3Last8Hours", gson.toJson(top3Last8Hours));
        }
        try {
            baseYML.save(basefile);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshRankingHologram() {
        if (top10Ranking.isEmpty()) return;
        if (Bukkit.isPrimaryThread() == false) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    refreshRankingHologram();
                }
            }.runTask(ms);
            return;
        }
        final Server sr = ms.getServer();
        final MuxSeason season = ms.getSeason();
        if (season.getMonthEnd() - System.currentTimeMillis() > 0 && season.getMonthEnd() - System.currentTimeMillis() <= 28_800_000L) {
            int rank = 0;
            for (int pos = 0; pos < Math.min(3, top10Ranking.size()); pos++) {
                final PlayerBase base = top10Ranking.get(pos);
                calculateTop(rank, base);
                rank++;
            }
            final String mainHoloText = "§b§lTop Basen§f - §e§lLängster Platzhalter§f - §b§lGewinner in " + ms.getTime((int) ((season.getMonthEnd() - System.currentTimeMillis()) / 1000));
            if (hologram != null) {
                hologram.setMessage(mainHoloText);
            } else {
                hologram = ms.getHolograms().addHologram("TopBasen", new Location(ms.getGameSpawn().getWorld(), 2.5, 79, 11.5), mainHoloText);
            }
            Location lastPos = hologram.getLocation().clone();
            final Map<Integer, PlayerBase> rankings = new HashMap<>();
            for (final Map.Entry<Integer, Map<UUID, RankingEntry>> entry : rankingEntries.entrySet()) {
                final int pos = entry.getKey();
                final Tuple<RankingEntry, PlayerBase> tuple = entry.getValue().values().stream().map(rankingEntry -> {
                            RankingEntry entry1;
                            if (top3Last8Hours.get(pos).equals(rankingEntry.getUUID())) {
                                entry1 = new RankingEntry(rankingEntry.getUUID(), rankingEntry.isActive() ? rankingEntry.getCurrentStart() : 0,
                                        rankingEntry.isActive() ? (rankingEntry.getTotalTime() + (System.currentTimeMillis() - rankingEntry.getCurrentStart())) : rankingEntry.getTotalTime());
                                entry1.setActive(rankingEntry.isActive());
                                entry1.setValue(rankingEntry.getValue());
                            } else entry1 = rankingEntry;
                            return entry1;
                        })
                        .filter(rankingEntry -> rankings.containsKey(pos - 1) == false || rankings.get(pos - 1).getOwner().equals(rankingEntry.getUUID()) == false)
                        .map(rankingEntry -> new Tuple<>(rankingEntry, playerBases.get(rankingEntry.getUUID())))
                        .max(Comparator.comparingDouble(value -> value.a().getTotalTime())).orElse(null);
                if (tuple == null) continue;
                if (tuple.a().getValue() < tuple.b().getValue())
                    tuple.a().setValue(tuple.b().getValue());
                final PlayerBase base = tuple.b();
                rankings.put(pos, base);
                final RankingEntry rankingEntry = tuple.a();
                final OfflinePlayer op = sr.getOfflinePlayer(base.getOwner());
                final boolean beta = ms.isBeta();
                final String cashreward = " §a➡ §l" + (pos == 0 ? (beta ? "50" : "250") : pos == 1 ? (beta ? "20" : "100") : (beta ? "10" : "50")) + " EUR",
                        text = "§f" + op.getName() + " - " + " §e" + (ms.getTime((int) (rankingEntry.getTotalTime() / 1000))) + "§f - §b" + ms.getLFormat((long) rankingEntry.getValue()) + cashreward;
                final MuxHolograms.Hologram holo = holograms.get("TopBasen" + pos);
                if (holo != null) {
                    holo.setMessage(text);
                    lastPos = holo.getLocation();
                } else {
                    holograms.put("TopBasen" + pos, ms.getHolograms().addHologram("TopBasen" + pos, (lastPos = lastPos.add(0, -0.4, 0)).clone(), text));
                }
            }
        } else {
            ms.getSeason().newMonth();
            if (hologram == null) {
                hologram = ms.getHolograms().addHologram("TopBasen", new Location(ms.getGameSpawn().getWorld(), 2.5, 79, 11.5), "§b§lTop Basen");
                Location lastPos = hologram.getLocation().clone();
                int step = 0;
                for (int pos = 0; pos < Math.min(3, top10Ranking.size()); pos++) {
                    final PlayerBase base = top10Ranking.get(pos);
                    if (base == null) {
                        step++;
                        continue;
                    }
                    final OfflinePlayer op = sr.getOfflinePlayer(base.getOwner());
                    final boolean beta = ms.isBeta();
                    final String cashreward = " §a➡ §l" + (pos == 0 ? (beta ? "50" : "250") : pos == 1 ? (beta ? "20" : "100") : (beta ? "10" : "50")) + " EUR",
                            text = "§f" + op.getName() + " - §b" + ms.getLFormat((long) base.getValue()) + cashreward;
                    holograms.put("TopBasen" + step, ms.getHolograms().addHologram("TopBasen" + step, (lastPos = lastPos.add(0, -0.4, 0)).clone(), text));
                    step++;
                }
                return;
            }
            hologram.setMessage("§b§lTop Basen");
            for (int pos = 0; pos < Math.min(3, top10Ranking.size()); pos++) {
                final PlayerBase base = top10Ranking.get(pos);
                if (base == null) continue;
                final MuxHolograms.Hologram holo = holograms.get("TopBasen" + pos);
                if (holo != null) {
                    final OfflinePlayer op = sr.getOfflinePlayer(base.getOwner());
                    final boolean beta = ms.isBeta();
                    final String cashreward = " §a➡ §l" + (pos == 0 ? (beta ? "50" : "250") : pos == 1 ? (beta ? "20" : "100") : (beta ? "10" : "50")) + " EUR",
                            text = "§f" + op.getName() + " - §b" + ms.getLFormat((long) base.getValue()) + cashreward;
                    holo.setMessage(text);
                } else {
                    holograms.values().forEach(MuxHolograms.Hologram::remove);
                    hologram.remove();
                    ms.getHolograms().removeHologramsWithName("TopBasen");
                    holograms.clear();
                    hologram = null;
                    refreshRankingHologram();
                    break;
                }
            }
        }
    }

    public void checkTop() {
        final long winningTime = System.currentTimeMillis();
        final Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        calendar.setTime(new Date(winningTime - 300000L));
        ms.broadcastMessage("§b", null, MuxSystem.Priority.HIGHEST);
        ms.broadcastMessage("§b§lBase Gewinner des Monats §f§l" + calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.GERMANY) + " " + calendar.get(Calendar.YEAR), null, MuxSystem.Priority.HIGHEST);
        Location lastPos = new Location(ms.getGameSpawn().getWorld(), 21D, 80D, 6D);
        ms.getHolograms().addHologram("BaseWin", lastPos, "§b§lBase Gewinner im " + calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.GERMANY));
        final Server sr = ms.getServer();
        final List<PlayerBase> winningList = new ArrayList<>();
        final Map<Integer, PlayerBase> rankings = new HashMap<>();
        for (int pos = 0; pos < 3; pos++) {
            if (rankingEntries.containsKey(pos)) {
                final int finalPos = pos;
                final Tuple<RankingEntry, PlayerBase> tuple = rankingEntries.get(pos).values().stream().map(rankingEntry -> {
                            RankingEntry entry1;
                            if (top3Last8Hours.get(finalPos).equals(rankingEntry.uuid)) {
                                entry1 = new RankingEntry(rankingEntry.getUUID(), rankingEntry.isActive() ? rankingEntry.getCurrentStart() : 0,
                                        rankingEntry.isActive() ? (rankingEntry.getTotalTime() + (System.currentTimeMillis() - rankingEntry.getCurrentStart())) : rankingEntry.getTotalTime());
                                entry1.setActive(rankingEntry.isActive());
                                entry1.setValue(rankingEntry.getValue());
                            } else entry1 = rankingEntry;
                            return entry1;
                        })
                        .filter(rankingEntry -> rankings.containsKey(finalPos - 1) == false || rankings.get(finalPos - 1).getOwner().equals(rankingEntry.getUUID()) == false)
                        .map(rankingEntry -> new Tuple<>(rankingEntry, playerBases.get(rankingEntry.getUUID())))
                        .max(Comparator.comparingDouble(value -> value.a().getTotalTime())).orElse(null);
                if (tuple == null) continue;
                final PlayerBase base = tuple.b();
                final RankingEntry rankingEntry = tuple.a();
                if (base == null) continue;
                rankings.put(pos, base);
                final OfflinePlayer op = sr.getOfflinePlayer(base.getOwner());
                final boolean beta = ms.isBeta();
                final String cashreward = " §a➡ §l" + (pos == 0 ? (beta ? "50" : "250") : pos == 1 ? (beta ? "20" : "100") : (beta ? "10" : "50")) + " EUR",
                        text = "§f" + op.getName() + " - " + " §c" + (ms.getTime((int) (rankingEntry.getTotalTime() / 1000))) + "§f - §b" + ms.getLFormat((long) rankingEntry.getValue()) + cashreward,
                        chattext = "  §b" + ms.getNumberFormat(rankingEntry.getValue()) + " Basewert - §f" + op.getName() + cashreward;
                winningList.add(base);
                holograms.put("BaseWin" + pos, ms.getHolograms().addHologram("BaseWin" + pos, (lastPos = lastPos.clone().add(0, -0.4, 0)), text));
                ms.broadcastMessage(chattext, null, MuxSystem.Priority.HIGHEST);
                baseYML.set("winvalues." + base.getOwner().toString(), rankingEntry.getValue());
            }
        }
        baseYML.set("winners", winningList.stream().map(PlayerBase::getOwner).map(UUID::toString).collect(Collectors.toList()));
        baseYML.set("winningTime", winningTime);
    }

    private void calculateTop(final int pos, final PlayerBase base) {
        rankingEntries.putIfAbsent(pos, new HashMap<>());
        RankingEntry current = null;
        if (top3Last8Hours.containsKey(pos)) {
            current = rankingEntries.get(pos).get(top3Last8Hours.get(pos));
        }
        if (current == null) {
            final RankingEntry entry = new RankingEntry(base.getOwner(), System.currentTimeMillis(), 0L);
            top3Last8Hours.put(pos, base.getOwner());
            rankingEntries.get(pos).put(base.getOwner(), entry);
            entry.setValue(base.getValue());
        } else if (current.getUUID().equals(base.getOwner()) == false) {
            final RankingEntry entry;
            if (rankingEntries.get(pos).containsKey(base.getOwner())) {
                entry = rankingEntries.get(pos).get(base.getOwner());
                entry.setCurrentStart(System.currentTimeMillis());
                current.setTotalTime(current.getTotalTime() + (System.currentTimeMillis() - current.getCurrentStart()));
                current.setCurrentStart(0);
                current.setActive(false);
                top3Last8Hours.put(pos, base.getOwner());
            } else {
                entry = new RankingEntry(base.getOwner(), System.currentTimeMillis(), 0L);
                current.setTotalTime(current.getTotalTime() + (System.currentTimeMillis() - current.getCurrentStart()));
                current.setCurrentStart(0);
                current.setActive(false);
                rankingEntries.get(pos).put(base.getOwner(), entry);
                top3Last8Hours.put(pos, base.getOwner());
                entry.setValue(base.getValue());
            }
        } else {
            current.setActive(true);
            if (current.getValue() < base.getValue())
                current.setValue(base.getValue());
        }
    }

    class RankingEntry {
        private final UUID uuid;
        private long currentStart, totalTime;
        private double value;
        private boolean active = true;

        RankingEntry(final UUID uuid, final long currentStart, final long totalTime) {
            this.uuid = uuid;
            this.currentStart = currentStart;
            this.totalTime = totalTime;
        }

        public void setValue(final double value) {
            this.value = value;
        }

        public double getValue() {
            return value;
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
                    ", value=" + value +
                    ", totalTime=" + totalTime +
                    ", active=" + active +
                    '}';
        }
    }

    private void handle() {
        this.handling = true;
        ms.getTimings().ofAndStart("baseFullValueCheck");
        Map<UUID, PlayerBase> pBases = ImmutableMap.copyOf(this.playerBases);
        pBases.values().parallelStream().forEach(PlayerBase::checkValue);
        ms.getTimings().stop("baseFullValueCheck");
        final Map<PlayerBase, Integer> tempRanking = new HashMap<>();
        final AtomicInteger index = new AtomicInteger(0);
        final List<PlayerBase> tempTop10 = new ArrayList<>();
        final Map<UUID, List<PlayerBase>> tempTrustedToRemove = new HashMap<>();
        pBases.values().parallelStream().sorted(Comparator.comparingDouble(PlayerBase::getValue).reversed()).collect(Collectors.toList()).forEach(playerBase -> {
            if (playerBase.getTempTrusted().isEmpty() == false) {
                playerBase.getTempTrusted().keySet().forEach(uuid -> {
                    if (playerBase.getTempTrustedRemaining(uuid) < System.currentTimeMillis()) {
                        tempTrustedToRemove.putIfAbsent(uuid, Lists.newArrayList());
                        tempTrustedToRemove.get(uuid).add(playerBase);
                        final Player baseOwner = ms.getServer().getPlayer(playerBase.getOwner());
                        if (baseOwner != null) {
                            final OfflinePlayer op = ms.getServer().getOfflinePlayer(uuid);
                            baseOwner.sendMessage("§a§lMuxBase>§7 Der Zugang von §a" + op.getName() + " §7ist §eabgelaufen§7.");
                        }
                    }
                });
            }
            tempRanking.put(playerBase, index.getAndIncrement());
            if (index.get() <= 10) tempTop10.add(playerBase);
        });
        if (tempTrustedToRemove.isEmpty() == false) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    tempTrustedToRemove.forEach((uuid, pbs) -> pbs.forEach(playerBase -> playerBase.removeTempTrusted(uuid)));
                }
            }.runTask(ms);
        }

        baseRanking = tempRanking;
        top10Ranking = tempTop10;
        final Map<Player, PlayerBase> copiedMap = ImmutableMap.copyOf(currentLocations);
        if (copiedMap.isEmpty() == false) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (final Map.Entry<Player, PlayerBase> entry : copiedMap.entrySet()) {
                        final Player pl = entry.getKey();
                        if (pl.isOnline() == false) continue;
                        final PlayerBase pb = entry.getValue();
                        sendScoreboard(pl, pb);
                        giveKickInfo(pl, pb);
                    }
                }
            }.runTask(ms);
        }
        handleWhitelist();
        this.handling = false;
    }

    private void giveKickInfo(final Player p, final PlayerBase pb) {
        if (pb.getOwner().equals(p.getUniqueId())) {
            if (pb.toldKickInfo()) return;
            final MutableBoolean mb = new MutableBoolean(false);
            getCurrentLocations().forEach((player, base) -> {
                if (base == pb && getVisitors().containsKey(player) == false && pb.isTrusted(player) == false && player.isOp() == false) {
                    mb.setValue(true);
                }
            });
            if (mb.booleanValue()) {
                pb.tellKickInfo(p);
            }
        }
    }

    private void handleWhitelist() {
        if (ms.getServer().hasWhitelist() == false) {
            if (whitelist != -1) {
                final long toAdd = System.currentTimeMillis() - whitelist;
                if (toAdd >= 300000L) {
                    playerBases.values().forEach(playerBase -> playerBase.setLastPlayed(playerBase.getLastPlayed() + toAdd));
                }
                whitelist = -1;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        baseYML.set("whitelist", whitelist);
                        try {
                            baseYML.save(basefile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.runTask(ms);
                playerBases.values().forEach(playerBase -> ms.getDB().saveBase(playerBase));
            }
        } else {
            if (whitelist == -1) {
                whitelist = System.currentTimeMillis();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        baseYML.set("whitelist", whitelist);
                        try {
                            baseYML.save(basefile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.runTask(ms);
            }
        }
    }

    private void initScoreboard() {
        sb = ms.getScoreboard().createScoreboard("§7§lMux§a§lBase");
        sb.setLook((p, board) -> {
            final PlayerBase base = getCurrentLocations().get(p);
            if (base == null) return;
            final MuxUser u = ms.getMuxUser(p.getName());
            if (u == null) return;
            final MuxRanks.PermissionsGroup group = ms.getPerms().getGroupOf(p.getUniqueId());
            sb.setLine(board, " ");
            sb.setSection(board, "§b§lWert          ", ms.getLFormat((long) base.getValue()), false);
            sb.setLine(board, "§d");
            sb.setSection(board, "§3§lRanking", "§fPlatz #" + ms.getNumberFormat(getBaseRanking(base)), false);
            sb.setLine(board, "§e");
            sb.setSection(board, ms.isBeta() ? "§e§lTestCoins" : "§e§lMuxCoins", ms.getNumberFormat(u.getCoins()), true);
            sb.setLine(board, "§c");
            sb.setSection(board, ms.getLang("sb.rank"), group == null ? ms.getLang("sb.norank") : group.getName(), true);
        });
        griefSb = ms.getScoreboard().createScoreboard("§7§lMux§c§lBase");
        griefSb.setLook((p, board) -> {
            final PlayerBase base = getCurrentLocations().get(p);
            griefSb.setLine(board, " ");
            griefSb.setSection(board, "§b§lWert          ", ms.getLFormat((long) base.getValue()), false);
            griefSb.setLine(board, "§e");
            griefSb.setSection(board, "§3§lGriefer", currentLocations.values().stream().filter(playerBase -> playerBase == base).count() + " Spieler", false);
            griefSb.setLine(board, "§d");
            final List<PlayerBaseBlock> baseBlocks = new ArrayList<>();
            if (base.getBlocks().isEmpty() == false) {
                final MuxShop shop = ms.getShop();
                final List<String> halfBlocks = getBlockRegistry().HALF_BLOCKS;
                ImmutableMap.copyOf(base.getBlocks()).forEach((id, amount) -> baseBlocks.add(new PlayerBaseBlock(id, amount, (shop.getCheapestPrice(id, false)), halfBlocks)));
                baseBlocks.sort(Comparator.comparingDouble(PlayerBaseBlock::getPriceForAmount).reversed());
                final PlayerBaseBlock baseBlock = baseBlocks.get(0);
                griefSb.setSection(board, "§5§lSpitzenblock", shop.getItemName(baseBlock.getId()), true);
            } else {
                griefSb.setSection(board, "§5§lSpitzenblock", "Kein Block", true);
            }
            final long timeLeft = base.getGrief() - System.currentTimeMillis();
            if (timeLeft > 0) {
                griefSb.setLine(board, "§c");
                final String timeString = "noch " + ms.getTime((int) (timeLeft / 1000));
                griefSb.setSection(board, "§a§lZugang", timeString, false);
            }
        });
        visitSb = ms.getScoreboard().createScoreboard("§7§lMux§3§lBase");
        visitSb.setLook((p, board) -> {
            final PlayerBase base = getCurrentLocations().get(p);
            if (base == null) return;
            visitSb.setLine(board, " ");
            visitSb.setSection(board, "§b§lWert          ", ms.getLFormat((long) base.getValue()), false);
            visitSb.setLine(board, "§d");
            visitSb.setSection(board, "§3§lRanking", "§fPlatz #" + ms.getNumberFormat(getBaseRanking(base)), false);
            visitSb.setLine(board, "§e");
            final List<PlayerBaseBlock> baseBlocks = new ArrayList<>();
            if (base.getBlocks().isEmpty() == false) {
                final List<String> halfBlocks = getBlockRegistry().HALF_BLOCKS;
                final MuxShop shop = ms.getShop();
                ImmutableMap.copyOf(base.getBlocks()).forEach((id, amount) -> baseBlocks.add(new PlayerBaseBlock(id, amount, (shop.getCheapestPrice(id, false)), halfBlocks)));
                baseBlocks.sort(Comparator.comparingDouble(PlayerBaseBlock::getPriceForAmount).reversed());
                final PlayerBaseBlock baseBlock = baseBlocks.get(0);
                visitSb.setSection(board, "§5§lSpitzenblock", shop.getItemName(baseBlock.getId()), true);
            } else {
                visitSb.setSection(board, "§5§lSpitzenblock", "Kein Block", true);
            }
            visitSb.setLine(board, "§c");
            visitSb.setSection(board, "§c§lBesitzer", "§f" + ms.getServer().getOfflinePlayer(base.getOwner()).getName(), false);
        });
    }

    private void generateWorld() {
        final Server sr = ms.getServer();
        final File[] files = sr.getWorldContainer().listFiles();
        if (files != null && Arrays.stream(files).noneMatch(file -> file.isDirectory() && file.getName().equals("MuxBase"))) {
            world = sr.createWorld(WorldCreator.name("MuxBase").generator(new MuxBaseWorldGenerator()));
            worldAsBukkitWorld = new BukkitWorld(world);
            // GENERATE 5000 BASES @ FIRST START
            lastX = minX;
            lastZ = minZ;
            pasteNewBase(5000, 15000, true);
        } else {
            worldAsBukkitWorld = new BukkitWorld((world = sr.createWorld(WorldCreator.name("MuxBase").generator(new MuxBaseWorldGenerator()))));
            lastX = baseYML.getInt("lastX");
            lastZ = baseYML.getInt("lastZ");
            bases = baseYML.getInt("bases");
            usedBases = baseYML.getInt("usedBases");
            whitelist = baseYML.getLong("whitelist");
            final Gson gson = new Gson();
            final List<String> fbases = gson.fromJson(baseYML.getString("freeBases"), new TypeToken<ArrayList<String>>() {
            }.getType());
            freeBases = fbases.stream().map(s -> baseStringToLocation(world, s)).collect(Collectors.toList());
            final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("CET"));
            final short hour = (short) cal.get(Calendar.HOUR_OF_DAY);
            if (freeBases.size() < 300 && (hour >= 4 && hour <= 6)) {
                pasteNewBase(500, 10000, false);
            }
        }
    }

    private void generateNewLocation() {
        lastX += 250;
        if (lastX >= maxX) {
            lastX = minX;
            lastZ += 250;
        }
    }

    public void pasteBaseAt(final Location location) {
        com.sk89q.worldedit.Vector vector = new com.sk89q.worldedit.Vector(location.getX(), location.getY(), location.getZ());
        schematic.paste(worldAsBukkitWorld, vector);
    }

    private void pasteNewBase(final int toPaste, final int maxChunks, final boolean disableWorld) {
        if (disableWorld) {
            WORLD_IS_DISABLED = true;
            world.getPlayers().forEach(player -> ms.forcePlayer(player, ms.getGameSpawn()));
        }
        final AtomicBoolean unloading = new AtomicBoolean(false);
        final AtomicInteger chunks = new AtomicInteger(-1);
        final AtomicInteger taskId = new AtomicInteger(new BukkitRunnable() {
            @Override
            public void run() {
                if (unloading.get() == false)
                    chunks.set(world.getLoadedChunks().length);
            }
        }.runTaskTimer(ms, 20L * 5L, 20).getTaskId());
        new Thread(() -> {
            final Server sr = ms.getServer();
            for (int i = 0; i < toPaste; i++) {
                if (chunks.get() > maxChunks) {
                    try {
                        Thread.sleep(5000L);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            unloading.set(true);
                            for (final Chunk chunk : world.getLoadedChunks()) {
                                if (Arrays.stream(chunk.getEntities()).anyMatch(Player.class::isInstance))
                                    continue;
                                chunk.unload();
                            }
                            unloading.set(false);
                        }
                    }.runTask(ms);
                    try {
                        Thread.sleep(5000);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                final Location loc = new Location(world, lastX, yLocation, lastZ);
                generateNewLocation();
                pasteBaseAt(loc);
                bases++;
                freeBases.add(loc);
                final int finalI = i;
                sr.getOnlinePlayers().stream().filter(Player::isOp)
                        .forEach(player -> ms.showItemBar(player, "§fEs wurden §a" + (finalI + 1) + "§f/" + toPaste + " Basen eingefügt."));
            }
            sr.getScheduler().runTaskLater(ms, () -> {
                for (final Chunk chunk : world.getLoadedChunks()) {
                    if (Arrays.stream(chunk.getEntities()).anyMatch(Player.class::isInstance)) continue;
                    chunk.unload();
                }
                sr.getScheduler().cancelTask(taskId.get());
                saveToFile();
                if (disableWorld) WORLD_IS_DISABLED = false;
            }, 20L * 10L);
        }).start();
    }

    public boolean createPlayerBase(final Player p) {
        if (ms.getAdmin().BASE.isActive() == false) {
            ms.showItemBar(p, "§cBasen sind momentan deaktiviert.");
            return false;
        } else if (ms.getAdmin().BASEGENERATE.isActive() == false) {
            ms.showItemBar(p, "§cDu kannst momentan keine Base generieren, bitte probiere es gleich erneut.");
            return false;
        }
        if (generating.contains(p)) return false;
        final int defaultSize = 32;
        final Location minPos, maxPos, baseMin, baseMax;
        final int toAdd = (defaultSize - 1) / 2, toRemove = -toAdd;
        if (freeBases.isEmpty() == false) {
            final Location baseLocation = freeBases.get(0);
            freeBases.remove(baseLocation);
            maxPos = baseLocation.clone().add(toAdd, 200, toAdd);
            minPos = baseLocation.clone().add(toRemove, -50, toRemove);
            baseMax = baseLocation.clone().add(120, 0, 120);
            baseMin = baseLocation.clone().add(-120, 0, -120);
            final PlayerBase base = new PlayerBase(ms, this, p.getUniqueId(), defaultSize, baseLocation, baseMin, baseMax, minPos, maxPos);
            finishBaseAdd(p, base);
            return true;
        } else {
            // Generate new one
            if (generalGeneratingCooldown > System.currentTimeMillis()) {
                p.sendMessage("§a§lMuxBase>§c Es wird momentan bereits eine Base generiert, versuche es später erneut.");
                return false;
            }
            generalGeneratingCooldown = System.currentTimeMillis() + 5000L;
            generating.add(p);
            final Location baseLocation = new Location(world, lastX, yLocation, lastZ);
            generateNewLocation();
            bases++;
            maxPos = baseLocation.clone().add(toAdd, 200, toAdd);
            minPos = baseLocation.clone().add(toRemove, -50, toRemove);
            baseMax = baseLocation.clone().add(120, 0, 120);
            baseMin = baseLocation.clone().add(-120, 0, -120);
            new Thread(() -> {
                pasteBaseAt(baseLocation);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        final PlayerBase base = new PlayerBase(ms, MuxBase.this, p.getUniqueId(), defaultSize, baseLocation, baseMin, baseMax, minPos, maxPos);
                        finishBaseAdd(p, base);
                    }
                }.runTaskLater(ms, 20L * 2L);
            }).start();
            return false;
        }
    }

    private void finishBaseAdd(final Player player, final PlayerBase base) {
        base.setSpawn(base.getLocation().clone().add(0.5D, 0D, 0.5D));
        base.setLastPlayed(System.currentTimeMillis());
        playerBases.put(player.getUniqueId(), base);
        List<PlayerBase> basesAtX = playerBasesByX.computeIfAbsent(base.getBaseMin().getBlockX(), k -> new ArrayList<>());
        basesAtX.add(base);
        usedBases++;
        if (player.isOnline()) {
            ms.forcePlayer(player, base.getSpawn());
            ms.getNewbies().handleNewbieBase(player);
        }
        base.generateOutline();
        baseHandler.addToValueCheckQueue(base);
        generating.remove(player);
        new BukkitRunnable() {
            @Override
            public void run() {
                ms.getDB().saveBase(base);
                saveToFile();
            }
        }.runTaskAsynchronously(ms);
    }

    public void baseJoinCheck(final Player p) {
        if (p.getWorld() != world) return;
        if (WORLD_IS_DISABLED || isEnabled() == false) {
            ms.forcePlayer(p, ms.getGameSpawn());
            return;
        }
        PlayerBase base = baseHandler.getTrustedPlayerBaseAtLocation(p.getLocation(), p.getUniqueId());
        if (base != null && base.isTrusted(p) == false && base.isGriefable() == false) {
            base = null;
        }
        if (base == null) {
            ms.forcePlayer(p, ms.getGameSpawn());
        } else {
            currentLocations.put(p, base);
            ms.getAnalytics().getAnalytics().getBasePlayers().add(p.getUniqueId());
            if (base.getOwner().equals(p.getUniqueId()) && base.isGriefed()) {
                ms.sendTitle(p, "§a§lNeustart", 20, 20, 20);
                ms.sendSubTitle(p, "§7Die Inaktivität bietet dir eine neue Chance.", 20, 20, 20);
                ms.forcePlayer(p, base.getSpawn());
                base.setGriefed(false);
            }
            sendScoreboard(p, base);
            base.loadChunks();

        }
    }

    public Location baseStringToLocation(final World w, final String s) {
        if (s == null || s.trim().equals("")) return null;
        final String[] parts = s.split(":");
        if (parts.length == 3)
            return new Location(w, Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
        return null;
    }

    public String baseLocationToString(final Location l) {
        if (l == null) return "";
        return l.getX() + ":" + l.getY() + ":" + l.getZ();
    }

    public PlayerBase addBase(final UUID uuid, final int size, final Location baseLocation, final Location minPos, final Location maxPos) {
        PlayerBase base;
        final Location baseMax = baseLocation.clone().add(120, 0, 120), baseMin = baseLocation.clone().add(-120, 0, -120);
        playerBases.put(uuid, base = new PlayerBase(ms, this, uuid, size, baseLocation, baseMin, baseMax, minPos, maxPos));
        List<PlayerBase> basesAtX = playerBasesByX.computeIfAbsent(base.getBaseMin().getBlockX(), k -> new ArrayList<>());
        basesAtX.add(base);
        return base;
    }

    public boolean handleCommand(final Player p, final String label) {
        if (label.toLowerCase().startsWith("grief")) {
            return openGeneralInventory(p, MuxBase.BaseInventoryType.GRIEFABLE);
        } else {
            return openGeneralInventory(p, MuxBase.BaseInventoryType.TOP);
        }
    }

    public boolean openGeneralInventory(final Player p, final BaseInventoryType category) {
        if (isEnabled() == false && p.isOp() == false) {
            ms.showItemBar(p, "§cDie Basen sind momentan deaktiviert.");
            return true;
        }
        if (category == BaseInventoryType.TOP) {
            final Inventory inventory = ms.getServer().createInventory(null, 45, "§0§lMuxBasen§0 | /basen");
            int griefSize = (int) playerBases.values().stream().filter(PlayerBase::isGriefable).count();
            if (griefSize == 0) griefSize = 1;
            final ItemStack grief = ms.renameItemStack(new ItemStack(Material.EXPLOSIVE_MINECART, Math.min(griefSize, 36)),
                    "§3§lGriefbare Basen", "§7Die Liste enthält alle Basen, die aufgrund", "§7von Inaktivität temporär verfügbar sind.", "", "§7Klicke, §7um die vollständige §3Liste zu sehen§7.");
            inventory.setItem(1, grief);
            final ItemStack top = ms.renameItemStack(new ItemStack(Material.SPECKLED_MELON),
                    "§3§lTop Basen", "§7Sortiert nach dem Gesamtwert der Base", "§7befinden sich hier die teuersten Basen.");
            inventory.setItem(4, ms.addGlow(top));
            inventory.setItem(7, ms.renameItemStack(new ItemStack(Material.COMPASS), "§3§lBase suchen", "§7Besuche mit dieser Funktion die",
                    "§7Base eines beliebigen Spielers.", "", "§7Klicke, um eine §3Base zu suchen§7."));
            int slot = 20;
            boolean self = false;
            for (int i = 0; i < 10; i++) {
                if (top10Ranking.size() <= i) break;
                final String color = i > 2 ? "§3" : (i == 0 ? "§e" : (i == 1 ? "§f" : "§6"));
                final PlayerBase base = top10Ranking.get(i);
                final OfflinePlayer op = ms.getServer().getOfflinePlayer(base.getOwner());
                if (op == null) continue;
                if (base.getOwner().equals(p.getUniqueId())) self = true;
                final ItemStack skull = ms.renameItemStack(new ItemStack(Material.SKULL_ITEM, 1, (short) 3), color + "#§l" + (i + 1) + " " + op.getName(),
                        base.getOwner().equals(p.getUniqueId()) ?
                                new String[]{
                                        "§7Basewert: §b" + ms.getNumberFormat((long) base.getValue()),
                                        "", "§7Klicke, um dich in deine", "§7Base zu §3teleportieren§7."
                                } : new String[]{
                                "§7Basewert: §b" + ms.getNumberFormat((long) base.getValue()),
                                "", "§7Klicke, um die Base des", "§7Spielers zu §3besuchen§7."
                        });
                final SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                skullMeta.setOwner(op.getName());
                skull.setItemMeta(skullMeta);
                inventory.setItem(slot, p.getUniqueId().equals(base.getOwner()) ? ms.addGlow(skull) : skull);
                slot++;
                if (slot == 25) slot = 29;
            }
            if (self == false) {
                if (slot > 33) slot = 33;
                final PlayerBase base = playerBases.get(p.getUniqueId());
                if (base == null) {
                    final ItemStack skull = ms.renameItemStack(redSkull.clone(), "§3" + "#?§l" + " " + p.getName(),
                            "§7Basewert: §b-", "", "§cDu musst eine Base erstellen,", "§cum danach im Ranking zu sein.");
                    final SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                    skull.setItemMeta(skullMeta);
                    inventory.setItem(slot, skull);
                } else {
                    final ItemStack skull = ms.renameItemStack(new ItemStack(Material.SKULL_ITEM, 1, (short) 3), "§3" + "#§l" + (getBaseRanking(base)) + " " + p.getName(),
                            "§7Basewert: §b" + ms.getNumberFormat((long) base.getValue()), "",
                            "§7Klicke, um dich in deine", "§7Base zu §3teleportieren§7.");
                    final SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                    skullMeta.setOwner(p.getName());
                    skull.setItemMeta(skullMeta);
                    inventory.setItem(slot, p.getUniqueId().equals(base.getOwner()) ? ms.addGlow(skull) : skull);
                }
            }
            if (ms.getActiveInv(p.getName()) != MuxInventory.InvType.BASES) p.closeInventory();
            p.openInventory(inventory);
        } else {
            if (ms.getActiveInv(p.getName()) != MuxInventory.InvType.BASES) p.closeInventory();
            p.openInventory(baseRaid.getGriefInventory());
        }
        ms.setActiveInv(p.getName(), MuxInventory.InvType.BASES);
        return true;
    }

    public void openHelper(final Player p) {
        final PlayerBase base = ms.getBase().getFromPlayer(p);
        if (base == null || base.contains(p.getLocation()) == false) {
            ms.showItemBar(p, "§cDer Helfer ist nur dem Besitzer zuständig.");
            return;
        }
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lHelfer");
        inv.setItem(15, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT, 1, (short) 5), "§f§lTeambase", "§7Kooperiere mit anderen Spielern, indem", "§7du ihnen Zugang auf deine Base gibst.", "", "§fKlicke§7, um dein Team zu verwalten."));
        inv.setItem(33, ms.renameItemStack(new ItemStack(Material.TNT), "§c§lBase zurücksetzen", "§7Hiermit entfernst du alles auf deiner", "§7Base, die Größe bleibt dabei erhalten.", "", (getFromPlayer(p).hasResetToday() ? "§cDie Base wurde schon zurückgesetzt." : "§7Klicke, um die Base §czurückzusetzen§7.")));
        inv.setItem(29, ms.renameItemStack(new ItemStack(Material.BED), "§a§lSpawnpunkt ändern", "§7In deinem §f§lMenü§7 §7kannst du über §fEinstellungen", "§7den Spawnpunkt deiner Base neu setzen.", "", "§7Klicke, um das §7§lMenü§7 in deiner §fHand zu halten§7."));
        inv.setItem(11, ms.renameItemStack(new ItemStack(Material.GRASS), "§b§lBase Blöcke", "§7Haue auf den Rand deiner Base, um die", "§7einzelnen §bBlockwerte §7sehen zu können.", "", "§7Klicke, um zur Grenze zu §bgelangen§7."));
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.MENU && it != MuxInventory.InvType.CONFIRM) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.MENU);
    }

    public void checkVisitors() {
        for (final Map.Entry<Player, PlayerBase> entry : visitors.entrySet()) {
            final Player pl = entry.getKey();
            if (pl == null || pl.getGameMode() != GameMode.SPECTATOR) {
                visitors.remove(entry.getKey());
            } else if (pl.getLocation().getWorld().getName().equals(world.getName()) == false || entry.getValue().getLocation().distance(pl.getLocation()) > 100D || pl.getLocation().getBlockY() < 13) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        pl.teleport(entry.getValue().getSpawn(), PlayerTeleportEvent.TeleportCause.END_PORTAL);
                    }
                }.runTaskLater(ms, 0L);
            }
        }
        currentLocations.forEach((player, playerBase) -> {
            if (playerBase.contains(player.getLocation()) == false && player.getGameMode() != GameMode.SPECTATOR) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ms.forcePlayer(player, getSafeSpawn(playerBase));
                    }
                }.runTask(ms);
            }
        });
    }

    public SortedMap<Integer, List<PlayerBase>> getPlayerBasesByX() {
        return playerBasesByX;
    }

    public Location getSafeSpawn(final PlayerBase base) {
        Location basespawn = base.getSpawn().clone();
        if (locationIsNotSafe(basespawn)) {
            final Location save = base.getMinPos().clone().add(-2, 2, -2);
            final Vector v1 = base.getLocation().toVector(), v2 = save.toVector();
            save.setYaw(v1.angle(v2) - 45);
            save.setY(base.getLocation().getY() + 2);
            basespawn = save;
        }
        return basespawn;
    }

    private boolean locationIsNotSafe(final Location loc) {
        return loc.getBlock().getType().isSolid() || loc.add(0, 1, 0).getBlock().getType().isSolid();
    }

    public void invitePlayer(final Player p, final Player inviter, final boolean temp) {
        if (p == inviter) {
            ms.showItemBar(inviter, "§cDu kannst dich nicht selbst hinzufügen.");
            return;
        } else if (p == null || inviter.canSee(p.getPlayer()) == false) {
            ms.showItemBar(inviter, ms.hnotonline);
            return;
        }
        final PlayerBase base = getFromPlayer(inviter);
        if (base.isTrusted(p.getUniqueId())) {
            ms.showItemBar(inviter, "§cDer Spieler hat bereits Zugang zur Base.");
            return;
        }
        int amount = 0;
        for (final PlayerBase b : playerBases.values()) {
            if (b.isTrusted(p) && b.getOwner().equals(p.getUniqueId()) == false) {
                amount++;
                if (amount == 5) break;
            }
        }
        if (amount == 5) {
            clearAllInvites(p);
            ms.showItemBar(inviter, "§cDer Spieler §6" + p.getName() + " §cist bereits in zu vielen Basen.");
            return;
        }
        final UUID iuuid = inviter.getUniqueId();
        final Map<UUID, Long> templist = tempInvites.get(p), ilist = invites.get(p);
        if (templist != null) templist.entrySet().removeIf(entry -> entry.getValue() < System.currentTimeMillis());
        if (ilist != null) ilist.entrySet().removeIf(entry -> entry.getValue() < System.currentTimeMillis());
        if (temp && (templist != null && templist.containsKey(iuuid)) || (ilist != null && ilist.containsKey(iuuid))) {
            ms.showItemBar(inviter, "§cDu hast diesen Spieler bereits Zugang angeboten.");
            return;
        }
        if (temp == false && ilist != null && ilist.containsKey(iuuid)) {
            ms.showItemBar(inviter, "§cDu hast diesen Spieler bereits Zugang angeboten.");
            return;
        }
        removeInvite(p, iuuid);
        if (temp) {
            tempInvites.putIfAbsent(p, new HashMap<>());
            tempInvites.get(p).put(iuuid, System.currentTimeMillis() + 120000);
            ms.chatClickHoverRun(p, "§a§lMuxBase>§7 Der Spieler §a" + inviter.getName() + " §7lädt dich zum Team ein.", "§a§oKlicke um anzunehmen", "/base tempaccept " + iuuid);
            ms.chatClickHoverRun(p, "§a§lMuxBase>§7 Klicke §ahier§7, um §etemporär §7Zugang zu erhalten.", "§a§oKlicke um anzunehmen", "/base tempaccept " + iuuid);
        } else {
            invites.putIfAbsent(p, new HashMap<>());
            invites.get(p).put(iuuid, System.currentTimeMillis() + 120000);
            ms.chatClickHoverRun(p, "§a§lMuxBase>§7 Der Spieler §a" + inviter.getName() + " §7lädt dich zum Team ein.", "§a§oKlicke um anzunehmen", "/base accept " + iuuid);
            ms.chatClickHoverRun(p, "§a§lMuxBase>§7 Klicke §ahier§7, um Zugang zu erhalten.", "§a§oKlicke um anzunehmen", "/base accept " + iuuid);
        }
        inviter.playSound(inviter.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
        ms.showItemBar(inviter, "§a" + p.getName() + " §fwurde zu deiner Base eingeladen.");
    }

    public void sendScoreboard(final Player p, final PlayerBase base) {
        if (p.getGameMode() == GameMode.SPECTATOR) {
            ms.sendScoreboard(p, visitSb);
        } else if (base != null && base.isGriefable()) {
            ms.sendScoreboard(p, griefSb);
        } else {
            ms.sendScoreboard(p, sb);
        }
    }

    public boolean hasTempInviteFrom(final Player p, final UUID inviter) {
        tempInvites.get(p).entrySet().removeIf(entry -> entry.getValue() < System.currentTimeMillis());
        return tempInvites.get(p).containsKey(inviter);
    }

    public boolean hasInviteFrom(final Player p, final UUID inviter) {
        invites.get(p).entrySet().removeIf(entry -> entry.getValue() < System.currentTimeMillis());
        return invites.get(p).containsKey(inviter);
    }

    public void clearAllSendInvites(final UUID uuid) {
        invites.values().forEach(map -> map.keySet().removeIf(id -> id.equals(uuid)));
        tempInvites.values().forEach(map -> map.keySet().removeIf(id -> id.equals(uuid)));
    }

    public void clearAllInvites(final Player p) {
        invites.remove(p);
        tempInvites.remove(p);
    }

    public void removeInvite(final Player p, final UUID invite) {
        if (tempInvites.containsKey(p))
            tempInvites.get(p).remove(invite);
        if (invites.containsKey(p))
            invites.get(p).remove(invite);
    }

    public List<PlayerBase> getTrusted(final UUID uuid) {
        final List<PlayerBase> result = new ArrayList<>();
        for (final PlayerBase base : playerBases.values()) {
            if (base.isTrusted(uuid))
                result.add(base);
        }
        return result;
    }

    public boolean isTrustedAtLocation(final Player p) {
        return currentLocations.containsKey(p) && currentLocations.get(p).isTrusted(p);
    }

    public boolean isInGriefableBase(final Player player) {
        final PlayerBase base = currentLocations.get(player);
        return base != null && base.isGriefable();
    }

    public boolean hasBase(final UUID uuid) {
        return playerBases.containsKey(uuid);
    }

    public boolean hasBase(final Player player) {
        return hasBase(player.getUniqueId());
    }

    public PlayerBase getFromUUID(final UUID uuid) {
        return playerBases.getOrDefault(uuid, null);
    }

    public PlayerBase getFromPlayer(final Player player) {
        return player == null ? null : getFromUUID(player.getUniqueId());
    }

    public Map<UUID, PlayerBase> getPlayerBases() {
        return playerBases;
    }

    public Map<Player, PlayerBase> getCurrentLocations() {
        return currentLocations;
    }

    public Map<Player, PlayerBase> getVisitors() {
        return visitors;
    }

    public int getBaseRanking(final PlayerBase base) {
        return (baseRanking.containsKey(base) ? baseRanking.get(base) + 1 : baseRanking.size());
    }

    public List<PlayerBase> getTop10Ranking() {
        return top10Ranking;
    }

    public World getWorld() {
        return world;
    }

    public boolean isEnabled() {
        return ms.getAdmin().BASE.isActive();
    }

    public MuxGriefing getGriefing() {
        return baseRaid;
    }

    public MuxBaseHandler getBaseHandler() {
        return baseHandler;
    }

    public MuxBaseBlockRegistry getBlockRegistry() {
        return blockRegistry;
    }

    public enum BaseInventoryType {
        GRIEFABLE,
        TOP
    }

    class MuxBaseWorldGenerator extends ChunkGenerator {
        @Override
        public List<BlockPopulator> getDefaultPopulators(final World world) {
            return new ArrayList<>();
        }

        @Override
        public ChunkData generateChunkData(final World world, final Random random, final int chunkX, final int chunkZ, final BiomeGrid grid) {
            return createChunkData(world);
        }
    }
}