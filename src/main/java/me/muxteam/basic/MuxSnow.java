package me.muxteam.basic;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunkBulk;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class MuxSnow implements Listener {
    private MuxSystem ms;

    private final HashMap<UUID, PacketChanger> changer = new HashMap<>();
    private final Field CHUNKX_MAPCHUNK, CHUNKZ_MAPCHUNK, CHUNK_MAPCHUNK, CHUNKBULKX_MAPCHUNK, CHUNKBULKZ_MAPCHUNK, CHUNKBULK_MAPCHUNK;
    private final SnowController snowcontroller;

    private boolean christmas = false;

    public MuxSnow(final MuxSystem ms) {
        this.ms = ms;
        this.CHUNKX_MAPCHUNK = NMSReflection.getField(PacketPlayOutMapChunk.class, "a");
        this.CHUNKZ_MAPCHUNK = NMSReflection.getField(PacketPlayOutMapChunk.class, "b");
        this.CHUNK_MAPCHUNK = NMSReflection.getField(PacketPlayOutMapChunk.class, "c");
        this.CHUNKBULKX_MAPCHUNK = NMSReflection.getField(PacketPlayOutMapChunkBulk.class, "a");
        this.CHUNKBULKZ_MAPCHUNK = NMSReflection.getField(PacketPlayOutMapChunkBulk.class, "b");
        this.CHUNKBULK_MAPCHUNK = NMSReflection.getField(PacketPlayOutMapChunkBulk.class, "c");
        this.snowcontroller = new SnowController();
        ms.getServer().getPluginManager().registerEvents(this, ms);
    }

    public void close() {
        this.ms = null;
    }

    public void activateChristmas() {
        ms.getServer().getPluginManager().registerEvents(new MuxChristmasEvents(), ms);
        christmas = true;
    }

    public void activateSnow(final Player p) {
        final UUID uuid = p.getUniqueId();
        if (snowcontroller.snowPlayers.contains(uuid) == false) {
            final PacketChanger c = changer.remove(uuid);
            if (c != null) c.uninject();
            snowcontroller.snowPlayers.add(uuid);
            changer.put(p.getUniqueId(), new PacketChanger(p, true));
            final Location l = p.getLocation();
            if (p.isInsideVehicle()) {
                p.leaveVehicle();
            }
            ms.forcePlayer(p, l.getWorld().equals(ms.getWarpsWorld()) ? ms.getFarmWorld().getSpawnLocation() : ms.getWarpsWorld().getSpawnLocation());
            ms.forcePlayer(p, l);
            ms.getHolograms().getIndividualHolograms().forEach(h -> {
                if (h.getPlayersTracking().contains(p)) {
                    h.send(p);
                    h.getAction().call(h, p);
                }
            });
        }
    }

    public void deactivateSnow(final Player p) {
        final UUID uuid = p.getUniqueId();
        boolean change = false;
        if (snowcontroller.snowPlayers.remove(uuid)) {
            final PacketChanger c = changer.remove(uuid);
            if (c != null) c.uninject();
            change = true;
        }
        if (christmas) {
            changer.put(p.getUniqueId(), new PacketChanger(p, true));
            final Chunk chunk = p.getLocation().getChunk();
            if (snowcontroller.shouldChangeBiome(p.getWorld(), chunk.getX(), chunk.getZ(), p)) {
                p.setPlayerWeather(WeatherType.DOWNFALL);
            }
            change = true;
        }
        if (change) {
            final Location l = p.getLocation();
            if (p.isInsideVehicle()) {
                p.leaveVehicle();
            }
            ms.forcePlayer(p, l.getWorld().equals(ms.getWarpsWorld()) ? ms.getFarmWorld().getSpawnLocation() : ms.getWarpsWorld().getSpawnLocation());
            ms.forcePlayer(p, l);
            ms.getHolograms().getIndividualHolograms().forEach(h -> {
                if (h.getPlayersTracking().contains(p)) {
                    h.send(p);
                    h.getAction().call(h, p);
                }
            });
        }
    }

    public class MuxChristmasEvents implements Listener {
        public MuxChristmasEvents() {

        }
        @EventHandler
        public void onPlayerJoin(final PlayerJoinEvent e) {
            final Player p = e.getPlayer();
            if (snowcontroller.snowPlayers.contains(p.getUniqueId())) return;
            changer.put(p.getUniqueId(), new PacketChanger(p, false));
            final Chunk chunk = p.getLocation().getChunk();
            if (snowcontroller.shouldChangeBiome(p.getWorld(), chunk.getX(), chunk.getZ(), p)) {
                p.setPlayerWeather(WeatherType.DOWNFALL);
            }
        }
        @EventHandler
        public void onTeleport(final PlayerTeleportEvent e) {
            if (e.getTo() == null) return;
            final Player p = e.getPlayer();
            if (snowcontroller.snowPlayers.contains(p.getUniqueId())) return;
            final Location from = e.getFrom(), to = e.getTo();
            final boolean fromShouldSnow = snowcontroller.shouldForceSnow(from), toShouldSnow = snowcontroller.shouldForceSnow(to);
            if (fromShouldSnow && toShouldSnow == false) {
                p.setPlayerWeather(WeatherType.CLEAR);
            }
            if (fromShouldSnow == false && toShouldSnow) {
                p.setPlayerWeather(WeatherType.DOWNFALL);
            }
        }
        @EventHandler
        public void onMove(final PlayerMoveEvent e) {
            final Player p = e.getPlayer();
            if (snowcontroller.snowPlayers.contains(p.getUniqueId())) return;
            final Location from = e.getFrom(), to = e.getTo();
            final boolean fromShould = snowcontroller.shouldForceSnow(from), toShould = snowcontroller.shouldForceSnow(to);
            if (fromShould && toShould == false && p.getPlayerWeather() != WeatherType.CLEAR) {
                p.setPlayerWeather(WeatherType.CLEAR);
            }
            if (fromShould == false && toShould) {
                p.setPlayerWeather(WeatherType.DOWNFALL);
            }
        }
        @EventHandler
        public void onWorldChange(final PlayerChangedWorldEvent e) {
            final Player p = e.getPlayer();
            if (snowcontroller.snowPlayers.contains(p.getUniqueId())) return;
            if (snowcontroller.shouldForceSnow(p.getLocation())) {
                p.setPlayerWeather(WeatherType.DOWNFALL);
            }
        }
        @EventHandler
        public void onRespawn(final PlayerRespawnEvent e) {
            final Player p = e.getPlayer();
            if (snowcontroller.snowPlayers.contains(p.getUniqueId())) return;
            new BukkitRunnable() {
                @Override
                public void run() {
                    final Chunk chunk = p.getLocation().getChunk();
                    if (snowcontroller.shouldChangeBiome(p.getWorld(), chunk.getX(), chunk.getZ(), p)) {
                        p.setPlayerWeather(WeatherType.DOWNFALL);
                    }
                }
            }.runTaskLater(ms, 20L);
        }
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent e) {
        final Player p = e.getPlayer();
        if (snowcontroller.snowPlayers.contains(p.getUniqueId())) {
            changer.put(p.getUniqueId(), new PacketChanger(p, true));
            p.setPlayerWeather(WeatherType.DOWNFALL);
        }
    }

    @EventHandler
    public void onRespawn(final PlayerRespawnEvent e) {
        final Player p = e.getPlayer();
        if (snowcontroller.snowPlayers.contains(p.getUniqueId()) == false) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                p.setPlayerWeather(WeatherType.DOWNFALL);
            }
        }.runTaskLater(ms, 20L);
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent e) {
        final Player p = e.getPlayer();
        final PacketChanger c = changer.remove(p.getUniqueId());
        if (c != null) c.uninject();
        p.resetPlayerWeather();
    }

    @EventHandler
    public void onWorldChange(final PlayerChangedWorldEvent e) {
        if (snowcontroller.snowPlayers.contains(e.getPlayer().getUniqueId())) {
            e.getPlayer().setPlayerWeather(WeatherType.DOWNFALL);
        }
    }
    class PacketChanger {
        private final Player player;
        public Channel channel;
        private final boolean allworlds;

        public PacketChanger(final Player player, final boolean allworlds) {
            this.player = player;
            this.allworlds = allworlds;
            this.inject();
        }

        public void inject() {
            final CraftPlayer craftPlayer = (CraftPlayer) this.player;
            this.channel = craftPlayer.getHandle().playerConnection.networkManager.channel;
            if (channel.pipeline().get("FdILeCOpDer") == null) channel.pipeline().addBefore("packet_handler", "FdILeCOpDer", new ChannelDuplexHandler() {
                @Override
                public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
                    if (player == null || msg == null) return;
                    if (msg instanceof Packet) {
                        if (msg instanceof PacketPlayOutMapChunk) {
                            if (allworlds || player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                                final Consumer<BiomeHandler> biomeBridgeConsumer = snowcontroller::handle;
                                final PacketPlayOutMapChunk mapChunkPacket = (PacketPlayOutMapChunk) msg;
                                PacketPlayOutMapChunk.ChunkMap chunkMap = null;
                                try {
                                    chunkMap = (PacketPlayOutMapChunk.ChunkMap) CHUNK_MAPCHUNK.get(mapChunkPacket);
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                }
                                if (chunkMap != null && chunkMap.a.length >= 49408) {
                                    final int chunkX, chunkZ;
                                    try {
                                        chunkX = CHUNKX_MAPCHUNK.getInt(mapChunkPacket);
                                        chunkZ = CHUNKZ_MAPCHUNK.getInt(mapChunkPacket);
                                    } catch (final Exception e) {
                                        e.printStackTrace();
                                        return;
                                    }
                                    byte[] biomeData = Arrays.copyOfRange(chunkMap.a, chunkMap.a.length - 256, chunkMap.a.length);
                                    biomeBridgeConsumer.accept(new BiomeHandler() {
                                        @Override
                                        public int getChunkX() {
                                            return chunkX;
                                        }
                                        @Override
                                        public int getChunkZ() {
                                            return chunkZ;
                                        }
                                        @Override
                                        public Player getPlayer() {
                                            return player;
                                        }
                                        @Override
                                        public void setBiome(int x, int z) {
                                            biomeData[(z & 15) << 4 | x & 15] = 11;
                                        }
                                        @Override
                                        public void setBiome(int x, int y, int z) {
                                            this.setBiome(x, z);
                                        }
                                    });
                                    modifyChunk(chunkMap, biomeData);
                                }
                            }
                        } else if (msg instanceof PacketPlayOutMapChunkBulk) {
                            if (allworlds || player.getWorld().getEnvironment() == World.Environment.NORMAL) {
                                final Consumer<BiomeHandler> biomeBridgeConsumer = snowcontroller::handle;
                                final PacketPlayOutMapChunkBulk mapChunkBulkPacket = (PacketPlayOutMapChunkBulk) msg;
                                int[] chunkXArray, chunkZArray;
                                try {
                                    chunkXArray = (int[]) CHUNKBULKX_MAPCHUNK.get(mapChunkBulkPacket);
                                    chunkZArray = (int[]) CHUNKBULKZ_MAPCHUNK.get(mapChunkBulkPacket);
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                    chunkXArray = new int[0];
                                    chunkZArray = new int[0];
                                }
                                PacketPlayOutMapChunk.ChunkMap[] chunkMapArray = null;
                                try {
                                    chunkMapArray = (PacketPlayOutMapChunk.ChunkMap[]) CHUNKBULK_MAPCHUNK.get(mapChunkBulkPacket);
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                }
                                for (int i = 0; i < chunkZArray.length; i++) {
                                    final int chunkX = chunkXArray[i], chunkZ = chunkZArray[i];
                                    final PacketPlayOutMapChunk.ChunkMap chunkMap = chunkMapArray != null ? chunkMapArray[i] : null;
                                    if (chunkMap != null && chunkMap.a.length >= 49408) {
                                        byte[] biomeData = Arrays.copyOfRange(chunkMap.a, chunkMap.a.length - 256, chunkMap.a.length);
                                        biomeBridgeConsumer.accept(new BiomeHandler() {
                                            @Override
                                            public int getChunkX() {
                                                return chunkX;
                                            }
                                            @Override
                                            public int getChunkZ() {
                                                return chunkZ;
                                            }
                                            @Override
                                            public Player getPlayer() {
                                                return player;
                                            }
                                            @Override
                                            public void setBiome(int x, int z) {
                                                biomeData[(z & 15) << 4 | x & 15] = 12;
                                            }
                                            @Override
                                            public void setBiome(int x, int y, int z) {
                                                this.setBiome(x, z);
                                            }
                                        });
                                        modifyChunk(chunkMap, biomeData);
                                    }
                                }
                            }
                        }
                    }
                    super.write(ctx, msg, promise);
                }
            });
        }
        public void uninject() {
            channel.eventLoop().execute(() -> {
                try {
                    if (channel.pipeline().get("FdILeCOpDer") != null) channel.pipeline().remove("FdILeCOpDer");
                } catch (final Exception e) {
                    System.out.println("MuxSystem> Error at removing channel from player. Ignoring...");
                }
            });
        }
    }
    private class SnowController {
        private final List<UUID> snowPlayers = new ArrayList<>();
        private final Set<RegionEntry> regions = new HashSet<>();

        public SnowController() {
            new BukkitRunnable() {
                @Override
                public void run() {
                    regions.add(new RegionEntry(ms.getGameSpawn().getWorld(), ms.getSpawnRegion()));
                }
            }.runTaskLater(ms, 20L); // delayed so everything is loaded
        }

        public void handle(final BiomeHandler handler) {
            final World world = handler.getPlayer().getWorld();
            if (shouldChangeBiome(world, handler.getChunkX(), handler.getChunkZ(), handler.getPlayer())) {
                for (int x = 0; x < 16; x++)
                    for (int z = 0; z < 16; z++)
                        handler.setBiome(x, z);
            }
        }
        private boolean shouldChangeBiome(final World world, final int x, final int z, final Player p) {
            if (regions.isEmpty() || snowPlayers.contains(p.getUniqueId())) return true;
            for (final RegionEntry entry : regions) {
                if (entry.world == world && isInChunk(entry.pointOne, entry.pointTwo, x, z))
                    return true;
            }
            return false;
        }
        public boolean shouldForceSnow(final Location loc) {
            if (regions.isEmpty()) return true;
            final int blockX = loc.getBlockX(), blockY = loc.getBlockY(), blockZ = loc.getBlockZ();
            for (final RegionEntry entry : regions) {
                if (entry.world == loc.getWorld() && entry.region.contains(blockX, blockY, blockZ))
                    return true;
            }
            return false;
        }
        private boolean isInChunk(final com.sk89q.worldedit.Vector pointOne, final com.sk89q.worldedit.Vector pointTwo, final int chunkX, final int chunkZ) {
            double minX = (int) Math.min(pointOne.getX(), pointTwo.getX()) >> 4, maxX = (int) Math.max(pointOne.getX(), pointTwo.getX()) >> 4;
            double minZ = (int) Math.min(pointOne.getZ(), pointTwo.getZ()) >> 4, maxZ = (int) Math.max(pointOne.getZ(), pointTwo.getZ()) >> 4;
            return chunkX >= minX && chunkX <= maxX && chunkZ >= minZ && chunkZ <= maxZ;
        }

        private class RegionEntry {
            private World world;
            private ProtectedRegion region;
            private com.sk89q.worldedit.Vector pointOne, pointTwo;

            public RegionEntry(final World world, final ProtectedRegion region) {
                if (region == null) return;
                this.world = world;
                this.region = region;
                pointOne = region.getMaximumPoint().add(10, 0, 10); // 10 Blocks in every direction
                pointTwo = region.getMinimumPoint().add(-10, 0, -10);
                regions.add(this);
            }
        }
    }
    private void modifyChunk(final PacketPlayOutMapChunk.ChunkMap chunkMap, byte[] newbiome) {
        if (newbiome.length != 256) {
            throw new IllegalArgumentException("biome size is not 256!");
        } else if (chunkMap.a.length >= 49408) {
            int startIndex = chunkMap.a.length - 256;
            System.arraycopy(newbiome, 0, chunkMap.a, startIndex, newbiome.length);
        }
    }
    interface BiomeHandler {
        int getChunkX();
        int getChunkZ();
        Player getPlayer();
        void setBiome(int x, int z);
        void setBiome(int x, int y, int z);
    }
}