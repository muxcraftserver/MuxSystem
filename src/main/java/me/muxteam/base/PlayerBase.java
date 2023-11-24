package me.muxteam.base;

import com.google.common.util.concurrent.AtomicDouble;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.registry.WorldData;
import me.muxteam.basic.MuxActions;
import me.muxteam.basic.MuxLocation;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.shop.MuxShop;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Skull;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.util.LongHash;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerBase {
    private final MuxSystem ms;
    private final MuxBase muxBase;

    private final UUID owner;
    private final Location location, baseMin, baseMax;
    private Location spawn, minPos, maxPos;
    private int size, griefId = -1;
    private double value;
    private long lastPlayed, lastValueCheck, grief = -1;
    private boolean upgrading = false, griefed = false, griefable = false;
    private boolean checkingValue, resetToday;
    private final List<UUID> trusted = new ArrayList<>();
    private final Map<UUID, Long> tempTrusted = new HashMap<>();
    private Map<String, Long> blocks = new HashMap<>();
    private final Map<String, Long> blocksLR = new HashMap<>();
    private transient List<Long> chunks = new ArrayList<>();
    private transient boolean isLoadedChunks = false;
    private transient boolean notifyextended = false, kickinfo = false;
    private transient boolean changed = false;
    private transient long lastChunksLoaded = -1;
    private transient boolean isLoadingChunks = false;

    public PlayerBase(final MuxSystem ms, final MuxBase mb, final UUID owner, final int size, final Location location, final Location baseMin, final Location baseMax, final Location minPos, final Location maxPos) {
        this.ms = ms;
        this.muxBase = mb;
        this.owner = owner;
        this.location = location;
        this.size = size;
        this.baseMin = baseMin;
        this.baseMax = baseMax;
        this.minPos = minPos;
        this.maxPos = maxPos;
    }

    public void generateOutline() {
        final int y = location.getBlockY() - 1;
        if (location.getChunk().isLoaded() == false) {
            location.getChunk().load();
        }
        ms.getForkJoinPool().execute(() -> {
            final List<Location> locations = new ArrayList<>();
            for (int x = minPos.getBlockX() - 1; x < maxPos.getBlockX() + 2; x++) {
                for (int z = minPos.getBlockZ() - 1; z < maxPos.getBlockZ() + 2; z++) {
                    if (isInside(x, z, minPos, maxPos)) continue;
                    final Location l = new Location(muxBase.getWorld(), x, y, z);
                    locations.add(l);
                }
            }
            final boolean hasGoldRank = ms.hasGold(owner);
            new BukkitRunnable() {
                @Override
                public void run() {
                    locations.forEach(l -> {
                        if (l.getChunk().isLoaded() == false) l.getChunk().load();
                        l.getBlock().setType(hasGoldRank ? Material.GOLD_BLOCK : Material.WOOL);
                        l.getBlock().setData((byte) (hasGoldRank ? 0 : 14));
                        final Location location2 = l.clone().add(0, 1, 0);
                        if (location2.getBlock() != null && location2.getBlock().getType() != null &&
                                (location2.getBlock().getType() == Material.LONG_GRASS || location2.getBlock().getType() == Material.GRASS)) {
                            location2.getBlock().setType(Material.AIR);
                            location2.getBlock().setData((byte) 0);
                        }
                    });
                }
            }.runTask(ms);
        });
    }

    public void removeOutline() {
        final int y = location.getBlockY() - 1;
        if (location.getChunk().isLoaded() == false) {
            location.getChunk().load();
        }
        for (int x = minPos.getBlockX() - 1; x < maxPos.getBlockX() + 2; x++) {
            for (int z = minPos.getBlockZ() - 1; z < maxPos.getBlockZ() + 2; z++) {
                if (isInside(x, z, minPos, maxPos)) continue;
                final Location l = new Location(muxBase.getWorld(), x, y, z);
                if (l.getChunk().isLoaded() == false) l.getChunk().load();
                l.getBlock().setType(Material.GRASS);
            }
        }
    }

    public void reset() {
        reset(this::loadChunks); // TODO should be instant here too
    }

    private void removeHomes(final Server sr, final UUID uuid) {
        final Player p = sr.getPlayer(uuid);
        final MuxUser u;
        if (p != null) {
            u = ms.getMuxUser(p.getName());
        } else {
            u = ms.getDB().loadPlayer(uuid);
            if (u != null) u.setHomes(ms.getDB().loadHomes(uuid));
        }
        if (u == null) return;
        final Set<String> toRemove = new HashSet<>();
        u.getHomes().forEach((name, home) -> {
            if (home.getWorld().equals(muxBase.getWorld().getName()) && contains(home)) {
                toRemove.add(name);
            }
        });
        toRemove.forEach(u::removeHome);
        if (toRemove.isEmpty() == false)
            new BukkitRunnable() {
                @Override
                public void run() {
                    ms.saveMuxUser(u);
                }
            }.runTaskAsynchronously(ms);
        toRemove.clear();
    }

    public void reset(final MuxActions.Action action) {
        // We paste the whole base new @ here & set the size
        resetToday = true;
        upgrading = true;
        final int currSize = getSize();
        setSpawn(location.clone().add(0.5, 0, 0.5));
        muxBase.getCurrentLocations().forEach((player, base) -> {
            if (base == this) {
                if (muxBase.getVisitors().containsKey(player)) return;
                muxBase.getVisitors().put(player, this);
                ms.forcePlayer(player, getSpawn());
                player.setGameMode(GameMode.SPECTATOR);
            }
        });
        ms.getBooster().removeBoostersWithin(minPos.getBlockX(), maxPos.getBlockX(), minPos.getBlockZ(), maxPos.getBlockZ(), muxBase.getWorld().getName());
        final Server sr = ms.getServer();
        getTrusted().forEach(uuid -> removeHomes(sr, uuid));
        getTempTrusted().keySet().forEach(uuid -> removeHomes(sr, uuid));
        removeHomes(sr, owner);
        new Thread(() -> {
            ms.getDB().removeBlocksWithin(muxBase.getWorld().getName(), baseMin.getBlockX(), baseMax.getBlockX(), baseMin.getBlockZ(), baseMax.getBlockZ());
            muxBase.pasteBaseAt(location);
            new BukkitRunnable() {
                @Override
                public void run() {
                    setSize(currSize);
                    final int y = location.getBlockY();
                    for (int x = minPos.getBlockX() - 1; x < maxPos.getBlockX() + 2; x++) {
                        for (int z = minPos.getBlockZ() - 1; z < maxPos.getBlockZ() + 2; z++) {
                            final Location l = new Location(muxBase.getWorld(), x, y, z);
                            if (l.getBlock() != null && l.getBlock().getType() != null &&
                                    (l.getBlock().getType() == Material.LONG_GRASS || l.getBlock().getType() == Material.GRASS)) {
                                l.getBlock().setType(Material.AIR);
                                l.getBlock().setData((byte) 0);
                            }
                        }
                    }
                    muxBase.getVisitors().entrySet().stream().filter(entry -> entry.getValue() == PlayerBase.this).forEach(entry -> {
                        if (isTrusted(entry.getKey())) {
                            muxBase.getVisitors().remove(entry.getKey());
                            entry.getKey().setGameMode(GameMode.SURVIVAL);
                            ms.forcePlayer(entry.getKey(), getSpawn());
                        } else {
                            muxBase.getVisitors().remove(entry.getKey());
                            entry.getKey().setGameMode(GameMode.SURVIVAL);
                            ms.forcePlayer(entry.getKey(), ms.getGameSpawn());
                        }
                    });
                    upgrading = false;
                    if (action != null)
                        action.call();
                }
            }.runTaskLater(ms, 20L * 3L);
        }).start();
    }

    public void setMaxPos(final Location maxPos) {
        this.maxPos = maxPos;
        this.changed = true;
    }

    public void setMinPos(final Location minPos) {
        this.minPos = minPos;
        this.changed = true;
    }

    private transient MuxActions.PlayerAction npcaction = null;
    private transient boolean npc;

    public void loadChunks() {
        final boolean isPlayerInLocation = muxBase.getCurrentLocations().containsValue(this);
        if (npcaction == null) {
            this.npcaction = new CustomNPCAction();
        }
        if (isPlayerInLocation && npc == false) {
            ms.getNPCS().addVillager(0, getHelperNpcLocation(), BlockFace.NORTH, griefable ? "§c§lBase verlassen" : "§a§lHelfer", npcaction);
            npc = true;
        }
        if (hasLoadedChunks() || isLoadedChunks()) return;
        isLoadedChunks = true;

        final Iterator<Long> chunkIterator = getChunks().iterator();
        loadChunksWithDelay(chunkIterator);

        /*getChunks().forEach(chunk -> {
            muxBase.getBaseHandler().addLoadedChunk(chunk);
            final Chunk bukkitChunk = muxBase.getWorld().getChunkAt(LongHash.msw(chunk), LongHash.lsw(chunk));
            if (bukkitChunk.isLoaded() == false) bukkitChunk.load();
        });*/
    }
    private void loadChunksWithDelay(final Iterator<Long> chunkIterator) {
        if (isLoadingChunks)
            return;
        this.isLoadingChunks = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (chunkIterator.hasNext()) {
                    long chunk = chunkIterator.next();
                    muxBase.getWorld().getChunkAtAsync(LongHash.msw(chunk), LongHash.lsw(chunk), bukkitChunk -> {
                        if (bukkitChunk.isLoaded() == false) {
                            bukkitChunk.load();
                        }
                    });
                } else {
                    isLoadingChunks = false;
                    cancel();
                }
            }
        }.runTaskTimer(ms, 20L, 15L);
    }

    private Location getHelperNpcLocation() {
        return location.clone().add(0.5, 0, (getSize() / 2D) + 5D);
    }

    public void setGriefVillager() {
        if (npcaction != null && npc) {
            ms.getNPCS().removeNPC(npcaction, false);
            npc = false;
        }
        final boolean isPlayerInLocation = muxBase.getCurrentLocations().containsValue(this);
        if (isPlayerInLocation) {
            ms.getNPCS().addVillager(0, getHelperNpcLocation(), BlockFace.NORTH, griefable ? "§c§lBase verlassen" : "§a§lHelfer", npcaction);
            npc = true;
        }
    }

    public void unloadChunks() {
        final boolean isPlayerInLocation = muxBase.getCurrentLocations().containsValue(this);
        if (npcaction != null && npc && isPlayerInLocation == false) {
            ms.getNPCS().removeNPC(npcaction, false);
            npc = false;
        }
        if (chunks.isEmpty()) return;
        this.checkingValue = true;
        chunks.forEach(chunk -> muxBase.getBaseHandler().unloadChunk(chunk));
        this.lastChunksLoaded = System.currentTimeMillis();
        isLoadedChunks = false;
        this.checkingValue = false;
    }

    public List<Long> getChunks() {
        if (chunks.isEmpty() == false) return this.chunks;
        final Set<Long> result = new HashSet<>();
        int minX = Math.min(baseMin.getBlockX(), baseMax.getBlockX()), maxX = Math.max(baseMin.getBlockX(), baseMax.getBlockX()),
                minZ = Math.min(baseMin.getBlockZ(), baseMax.getBlockZ()), maxZ = Math.max(baseMax.getBlockZ(), baseMin.getBlockZ());
        minX -= 16; // do
        maxX += 16; // not
        minZ -= 16; // load
        maxZ += 16; // all chunks
        for (int x = minX; x < maxX; x += 16) {
            for (int z = minZ; z < maxZ; z += 16) {
                final long key = LongHash.toLong(x >> 4, z >> 4);
                muxBase.getBaseHandler().addLoadedChunk(key);
                result.add(key);
            }
        }
        return this.chunks = new ArrayList<>(result);
    }

    public void setSize(final int size) {
        upgrading = true;
        removeOutline();
        this.size = size;
        final int toAdd = (size - 1) / 2, toRemove = -toAdd;
        maxPos = location.clone().add(toAdd, 200, toAdd);
        minPos = location.clone().add(toRemove, -50, toRemove);
        generateOutline();
        upgrading = false;
        notifyextended = true;
        this.changed = true;
    }

    public boolean toldKickInfo() {
        return kickinfo;
    }

    public void tellKickInfo(final Player p) {
        if (kickinfo == false) {
            kickinfo = true;
            p.sendMessage("§f§lEin fremder Spieler ist auf deiner Base.");
            p.sendMessage("§f§lDu kannst ihn mit Rechtsklick von deiner Base kicken.");
        }
    }

    public void extendedNotification(final Player p) {
        if (notifyextended) {
            ms.sendTitle(p, "§a§lMuxBase", 10, 60, 10);
            ms.sendSubTitle(p, "§fDeine Base wurde erweitert, weil du gevotet hast.", 10, 60, 10);
            notifyextended = false;
        }
    }

    public boolean hasLoadedChunks() {
        return isLoadedChunks;
    }

    public void addTrusted(final UUID uuid) {
        trusted.add(uuid);
        this.changed = true;
    }

    public void addTrusted(final OfflinePlayer op, final Player player) {
        if (op == null) {
            ms.showItemBar(player, ms.hnotfound);
            return;
        }
        final UUID uuid = op.getUniqueId();
        tempTrusted.remove(uuid);
        if (isTrusted(op.getUniqueId())) {
            //String color = trusted.contains(op.getUniqueId()) ? "§6permanenten" : "§etemporären";
            ms.showItemBar(player, "§cDieser Spieler hat bereits Zugang zur Base.");
            return;
        }
        addTrusted(uuid);
        this.changed = true;
    }

    public void addTempTrusted(final OfflinePlayer op, final Player player) {
        if (op == null || op.isOnline() == false || player.canSee(op.getPlayer()) == false) {
            ms.showItemBar(player, ms.hnotonline);
            return;
        }
        final UUID uuid = op.getUniqueId();
        if (isTrusted(uuid)) {
            //String color = trusted.contains(uuid) ? "§6permanenten" : "§etemporären";
            ms.showItemBar(player, "§cDieser Spieler hat bereits Zugang zur Base.");
            return;
        }
        addTempTrusted(uuid, System.currentTimeMillis() + 7200000);
        this.changed = true;
    }

    public void addTempTrusted(final UUID uuid, final long time) {
        tempTrusted.put(uuid, time);
    }

    public long getTempTrustedRemaining(final UUID uuid) {
        return tempTrusted.get(uuid);
    }

    public int getTempTrustedSize() {
        return tempTrusted.size();
    }

    public void removeTempTrusted(final UUID uuid) {
        tempTrusted.remove(uuid);
        final Player player = ms.getServer().getPlayer(uuid);
        if (player != null && muxBase.getCurrentLocations().get(player) == this) {
            ms.forcePlayer(player, ms.getGameSpawn());
        }
        removeHomes(ms.getServer(), uuid);
        this.changed = true;
    }

    public void removeTrusted(final UUID uuid) {
        trusted.remove(uuid);
        final Player player = ms.getServer().getPlayer(uuid);
        if (player != null && muxBase.getCurrentLocations().get(player) == this) {
            ms.forcePlayer(player, ms.getGameSpawn());
        }
        removeHomes(ms.getServer(), uuid);
        this.changed = true;
    }

    public void saveAsSchematic(final Player player) {
        final int currid = ms.getDB().canSaveBaseSchematic(player.getUniqueId());
        if (currid != -1) {
            ms.chatClickHoverLink(player, "§7Klicke hier, um deine Base §aherunterzuladen§7.", "§aKlicke zum herunterladen.", "https://muxcraft.eu/base?download=" + currid);
            return;
        }
        final com.sk89q.worldedit.world.World weWorld = new BukkitWorld(muxBase.getWorld());
        final WorldData worldData = weWorld.getWorldData();
        final org.bukkit.util.Vector min = baseMin.toVector(), max = baseMax.toVector();
        final Vector minVector = new Vector(min.getX(), 0, min.getZ()), maxVector = new Vector(max.getX(), 256, max.getZ());
        final CuboidRegion region = new CuboidRegion(weWorld, minVector, maxVector);
        final Clipboard clipboard = new BlockArrayClipboard(region);
        int x = (int) (clipboard.getMaximumPoint().getX() + clipboard.getMinimumPoint().getX()),
                z = (int) (clipboard.getMaximumPoint().getZ() + clipboard.getMinimumPoint().getZ()),
                y = (int) (clipboard.getMaximumPoint().getY() + clipboard.getMinimumPoint().getY());
        x /= 2.0D;
        z /= 2.0D;
        y /= 2.0D;
        clipboard.setOrigin(new Vector(x, y, z));
        final Extent source = WorldEdit.getInstance().getEditSessionFactory().getEditSession(weWorld, -1);
        final ForwardExtentCopy copy = new ForwardExtentCopy(source, region, minVector, clipboard, minVector);
        copy.setCopyBiomes(true);
        copy.setCopyEntities(true);
        ms.getNPCS().removeNPC(npcaction, false);
        npc = false;
        final File file = new File(ms.getDataFolder(), "base/" + owner.toString() + ".schematic");
        try {
            if (file.exists() == false) file.createNewFile();
            EditSession session = (EditSession) source;
            session.addNotifyTask(() -> {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ms.getNPCS().addVillager(0, getHelperNpcLocation(), BlockFace.NORTH, "§a§lHelfer", npcaction);
                        npc = true;
                    }
                }.runTaskLater(ms, 20L);
                ms.getForkJoinPool().execute(() -> {
                    try {
                        ClipboardFormat.SCHEMATIC.getWriter(Files.newOutputStream(file.toPath())).write(clipboard, worldData);
                    } catch (final IOException e) {
                        e.printStackTrace();
                    } finally {
                        final int id = ms.getDB().saveBaseSchematic(this.owner, file);
                        if (id == -1) {
                            ms.showItemBar(player, "§cEs ist ein Fehler aufgetreten.");
                        } else {
                            ms.showItemBar(player, "§aDeine Base wurde exportiert.");
                            ms.chatClickHoverLink(player, "§7Klicke hier, um deine Base §aherunterzuladen§7.", "§aKlicke zum herunterladen.", "https://muxcraft.eu/base?download=" + id);
                        }
                    }
                });
            });
            ms.getForkJoinPool().execute(() -> Operations.completeSmart(copy, session::flushQueue, false));
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public boolean hasResetToday() {
        return resetToday;
    }

    public void setResetToday(final boolean resetToday) {
        this.resetToday = resetToday;
        this.changed = true;
    }

    public boolean isTrusted(final Player player) {
        return isTrusted(player.getUniqueId());
    }

    public long getLastValueCheck() {
        return lastValueCheck;
    }

    public void setLastValueCheck(final long lastValueCheck) {
        this.lastValueCheck = lastValueCheck;
    }

    public boolean isTrusted(final UUID uuid) {
        tempTrusted.keySet().removeIf(id -> tempTrusted.get(id) < System.currentTimeMillis());
        return owner.equals(uuid) || trusted.contains(uuid) || tempTrusted.containsKey(uuid);
    }

    public boolean contains(final Location location) {
        return contains(location, false);
    }

    public long getLastPlayed() {
        return lastPlayed;
    }

    public boolean contains(final Location location, final boolean build) {
        if (build) {
            return (location.getWorld().getName().equalsIgnoreCase(muxBase.getWorld().getName())
                    && (location.getBlockY() >= getMinPos().getY()
                    && location.getBlockY() <= getMaxPos().getY())
                    && (location.getBlockX() >= getMinPos().getX()
                    && location.getBlockX() <= getMaxPos().getX())
                    && (location.getBlockZ() >= getMinPos().getZ()
                    && location.getBlockZ() <= getMaxPos().getZ()));
        } else {
            return (location.getWorld().getName().equalsIgnoreCase(muxBase.getWorld().getName())
                    && (location.getBlockX() >= getBaseMin().getX()
                    && location.getBlockX() <= getBaseMax().getX())
                    && (location.getBlockZ() >= getBaseMin().getZ()
                    && location.getBlockZ() <= getBaseMax().getZ()));
        }
    }

    public boolean contains(final MuxLocation location) {
        return (location.getWorld().equalsIgnoreCase(muxBase.getWorld().getName())
                && (location.getX() >= getBaseMin().getX()
                && location.getX() <= getBaseMax().getX())
                && (location.getZ() >= getBaseMin().getZ()
                && location.getZ() <= getBaseMax().getZ()));
    }

    public boolean canPlaceGrowableHere(final Location location) {
        return (location.getWorld().getName().equalsIgnoreCase(muxBase.getWorld().getName())
                && (location.getBlockY() >= getMinPos().getY()
                && location.getBlockY() <= getMaxPos().getY())
                && (location.getBlockX() >= getMinPos().getX() + 5
                && location.getBlockX() <= getMaxPos().getX() - 5)
                && (location.getBlockZ() >= getMinPos().getZ() + 5
                && location.getBlockZ() <= getMaxPos().getZ() - 5));
    }

    private boolean isInside(final int x, final int z, final Location min, final Location max) {
        final int maxX = Math.max(min.getBlockX(), max.getBlockX()), minX = Math.min(min.getBlockX(), max.getBlockX()),
                maxZ = Math.max(min.getBlockZ(), max.getBlockZ()), minZ = Math.min(min.getBlockZ(), max.getBlockZ());
        return (x >= minX) && (x <= maxX) && (z >= minZ) && (z <= maxZ);
    }

    public boolean isUpgrading() {
        return upgrading;
    }

    public boolean isLoadedChunks() {
        return isLoadedChunks;
    }

    public boolean isCheckingValue() {
        return checkingValue;
    }

    private boolean canCheck(boolean isPlayerInLocation) {
        if (isPlayerInLocation)
            return true;
        if (isLoadedChunks == false)
            return false;
        return isLoadingChunks == false;
    }

    public void checkValue() {
        if (upgrading || checkingValue) return;
        this.checkingValue = true;
        final MuxShop shop = ms.getShop();
        final boolean isPlayerInLocation = muxBase.getCurrentLocations().containsValue(this);
        final MuxBaseBlockRegistry registry = muxBase.getBlockRegistry();
        if (canCheck(isPlayerInLocation) == false) {
            if (this.lastChunksLoaded != -1 && this.lastChunksLoaded < System.currentTimeMillis() - 900000L) { // 15 minuten
                //  if (this.lastChunksLoaded != -1 && this.lastChunksLoaded < System.currentTimeMillis() - 30000) { // for testing activate this
                muxBase.addToChunkCleanup(this);
                this.lastChunksLoaded = -1;
            }
            if (blocks == null || blocks.isEmpty()) {
                this.checkingValue = false;
                return;
            }
            final AtomicDouble v = new AtomicDouble();
            final List<String> halfBlocks = registry.HALF_BLOCKS;
            blocks.forEach((s, aLong) -> {
                double price = shop.getCheapestPrice(s, false);
                if (aLong >= 2) {
                    for (final String hb : halfBlocks) {
                        if (s.startsWith(hb)) {
                            aLong = aLong / 2;
                            break;
                        }
                    }
                }
                price = price < 0 ? 0 : price;
                v.addAndGet(price * aLong);
            });
            setValue(v.get());
            this.checkingValue = false;
            return;
        }
        BlockPosition blockPosition;
        net.minecraft.server.v1_8_R3.Chunk chunk;
        Block block;
        int id, data;
        String blockId;
        String rpl;
        final Map<String, Long> tempBlocks = new ConcurrentHashMap<>();
        final WorldServer worldServer = ((CraftWorld) muxBase.getWorld()).getHandle();
        final Map<Integer, String> blockIDsToReplace = registry.BLOCK_IDS_TO_REPLACE;
        final Map<String, String> blockIDsToExactReplace = registry.BLOCK_IDS_TO_EXACT_REPLACE;
        final Set<Integer> blockIDsToSimpleReplace = registry.BLOCK_IDS_TO_SIMPLE_REPLACE;
        final int maxX = maxPos.getBlockX(), maxY = maxPos.getBlockY(), maxZ = maxPos.getBlockZ();
        final AtomicBoolean foundUncheckedBanner = new AtomicBoolean(false);
        for (int x = minPos.getBlockX(); x <= maxX; x++) {
            for (int z = minPos.getBlockZ(); z <= maxZ; z++) {
                for (int y = minPos.getBlockY(); y < maxY; y++) {
                    blockPosition = new BlockPosition(x, y, z);
                    chunk = worldServer.getChunkAtWorldCoords(blockPosition);
                    if (chunk == null || chunk.bukkitChunk == null || chunk.bukkitChunk.isLoaded() == false) {
                        break;
                    }
                    block = chunk.bukkitChunk.getBlock(x, y, z);
                    id = block.getTypeId();
                    if (id == 0) {
                        continue;
                    }
                    data = block.getData();
                    blockId = id + ":" + data;
                    if (id == 96) {
                        blockId = "96:0";
                    } else if (id == 145) {
                        blockId = "145:0";
                    } else if (id == 68 || id == 63) {
                        blockId = "323:0";
                    } else if (id == 144) {
                        final Skull skull = (Skull) block.getState();
                        if (skull != null) blockId = "397:" + skull.getSkullType().ordinal();
                    } else if (id == 43) {
                        blockId = blockId.replace("43:", "44:");
                        tempBlocks.put(blockId, tempBlocks.getOrDefault(blockId, 0L) + 2L);
                        continue;
                    } else if (id == 181) {
                        blockId = blockId.replace("181:", "182:");
                        tempBlocks.put(blockId, tempBlocks.getOrDefault(blockId, 0L) + 2L);
                        continue;
                    } else if (id == 125) {
                        blockId = blockId.replace("125:", "126:");
                        tempBlocks.put(blockId, tempBlocks.getOrDefault(blockId, 0L) + 2L);
                        continue;
                    } else if ((id == 126 || id == 44 || id == 182) && data >= 8) {
                        blockId = id + ":" + (data - 8);
                    }
                    if (blockIDsToSimpleReplace.contains(id)) {
                        blockId = id + ":0";
                    }
                    if (id == 18 || id == 161 || id == 17 || id == 162 || id == 97 || id == 175) {
                        rpl = blockIDsToExactReplace.get(blockId);
                    } else if (id == 176 || id == 177) {
                        final String str = muxBase.getPlacedBanners().get(blockPosition.asLong());
                        if (str == null) {
                            rpl = null;
                            muxBase.getUncheckedBannerPositions().add(blockPosition);
                            foundUncheckedBanner.set(true);
                        } else rpl = str;
                    } else { // Not any ids of above
                        rpl = blockIDsToReplace.get(id);
                    }
                    if (rpl != null) blockId = rpl;
                    tempBlocks.put(blockId, tempBlocks.getOrDefault(blockId, 0L) + 1L);
                }
            }
        }
        blocks = tempBlocks;
        if (blocks.isEmpty()) {
            setValue(0);
        } else {
            final AtomicDouble v = new AtomicDouble();
            final List<String> halfBlocks = muxBase.getBlockRegistry().HALF_BLOCKS;
            blocks.forEach((s, aLong) -> {
                double price = shop.getCheapestPrice(s, false);
                if (aLong >= 2) {
                    for (final String hb : halfBlocks) {
                        if (s.startsWith(hb)) {
                            aLong = aLong / 2;
                            break;
                        }
                    }
                }
                price = price < 0 ? 0 : price;
                v.addAndGet(price * aLong);
            });
            setValue(v.get());
        }
        checkingValue = false;
        if (isLoadedChunks && isPlayerInLocation == false && foundUncheckedBanner.get() == false && isLoadingChunks == false) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    unloadChunks();
                }
            }.runTask(ms);
        }
    }

    public void setSpawn(final Location spawn) {
        this.spawn = spawn;
        this.changed = true;
    }

    public void setGrief(final long grief) {
        this.grief = grief;
        this.changed = true;
    }

    public void setGriefed(final boolean griefed) {
        this.griefed = griefed;
        this.changed = true;

    }

    public void setGriefable(final boolean griefable) {
        this.griefable = griefable;
        this.changed = true;
    }

    public void setGriefId(final int griefId) {
        this.griefId = griefId;
        this.changed = true;
    }

    public void setLastPlayed(final long lastPlayed) {
        this.lastPlayed = lastPlayed;
        this.changed = true;
    }

    public UUID getOwner() {
        return owner;
    }

    public int getSize() {
        return size;
    }

    public void setValue(final double value) {
        this.value = value;
        //  this.changed = true;
    }

    public boolean isChanged() {
        return this.changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public double getValue() {
        return value;
    }

    public Location getBaseMax() {
        return baseMax;
    }

    public Location getBaseMin() {
        return baseMin;
    }

    public Location getLocation() {
        return location;
    }

    public Location getMaxPos() {
        return maxPos;
    }

    public Location getMinPos() {
        return minPos;
    }

    public Location getSpawn() {
        return spawn;
    }

    public Map<UUID, Long> getTempTrusted() {
        final List<UUID> toNotify = new ArrayList<>();
        tempTrusted.entrySet().removeIf(entry -> {
            if (entry.getValue() < System.currentTimeMillis()) {
                toNotify.add(entry.getKey());
                return true;
            }
            return false;
        });
        final Server sr = ms.getServer();
        toNotify.forEach(uuid -> {
            final Player removed = sr.getPlayer(uuid);
            if (removed != null) {
                final String plname = sr.getOfflinePlayer(uuid).getName();
                ms.chatClickHoverShow(removed, "§a§lMuxBase>§7 Dein Zugang zur Teambase ist §eabgelaufen§7.", "§a§oKlicke um ihn zu fragen", "/msg " + plname + " Hey, könntest du mir wieder Zugang geben zu deiner Base?");
                ms.chatClickHoverShow(removed, "§a§lMuxBase>§7 Klicke §ahier§7, um §a" + plname + " §7erneut zu fragen.", "§a§oKlicke um ihn zu fragen", "/msg " + plname + " Hey, könntest du mir wieder Zugang geben zu deiner Base?");
            }
        });
        return tempTrusted;
    }

    public List<UUID> getTrusted() {
        return trusted;
    }

    public int getTrustedSize() {
        return trusted.size();
    }

    public Map<String, Long> getBlocks() {
        return blocks;
    }

    public Map<String, Long> getBlocksLR() {
        return blocksLR;
    }

    public int getGriefId() {
        return griefId;
    }

    public boolean isGriefable() {
        return griefable;
    }

    public long getGrief() {
        return grief;
    }

    public boolean isGriefed() {
        return griefed;
    }

    public class CustomNPCAction implements MuxActions.PlayerAction {
        @Override
        public void call(final Player p) {
            p.performCommand(griefable ? "spawn" : "base helper");
        }
    }
}