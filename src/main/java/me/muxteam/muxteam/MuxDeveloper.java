package me.muxteam.muxteam;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.NetworkManager;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class MuxDeveloper {
    /***
     * DEVELOPER TOOLS
     */
    private MuxSystem ms;
    private final Map<Player, PacketDuplexDebugger> packetDebuggers = new HashMap<>();

    public MuxDeveloper(final MuxSystem ms) {
        this.ms = ms;
    }

    public boolean handleCommand(final String scmd, final Player p, final String[] args) {
        if (scmd.equalsIgnoreCase("getloc")) {
            if (p.hasPermission("muxsystem.developer") == false) {
                ms.sendNoCMDMessage(p);
                return true;
            }
            final Location l = p.getLocation();
            ms.showItemBar(p, "§fX: §6" + l.getBlockX() + " §fY: §6" + l.getBlockY() + " §fZ: §6" + l.getBlockZ());
            ms.chatClickHoverShow(p, "§7Klicke §3hier§7, um die Position zu kopieren.", "§3§oKlicke zum kopieren", l.getBlockX() + "D, " + l.getBlockY() + "D, " + l.getBlockZ() + "D, " + l.getYaw() + "F, " + l.getPitch() + "F");
            return true;
        } else if (scmd.equalsIgnoreCase("uuid")) {
            if (p.hasPermission("muxsystem.developer") == false) {
                ms.sendNoCMDMessage(p);
                return true;
            } else if (args.length == 0) {
                ms.showItemBar(p, ms.usage("/uuid [name]"));
                return true;
            }
            final OfflinePlayer player = ms.getServer().getOfflinePlayer(args[0]);
            ms.chatClickHoverShow(p, "§7Klicke §3hier§7, um die UUID von " + player.getName() + " zu kopieren.", "§3§oKlicke zum kopieren", player.getUniqueId().toString().replace("-", ""));
            return true;
        } else if (scmd.equalsIgnoreCase("getblock")) {
            if (p.hasPermission("muxsystem.developer") == false) {
                ms.sendNoCMDMessage(p);
                return true;
            }
            final Block block = p.getLocation().add(0, -1, 0).getBlock();
            p.sendMessage("§7Du stehst auf: §f" + block.getType().name() + " (" + block.getTypeId() + ":" + block.getData() + ")");
            return true;
        } else if (scmd.equalsIgnoreCase("getentities")) {
            if (p.hasPermission("muxsystem.developer") == false) {
                ms.sendNoCMDMessage(p);
                return true;
            }
            p.sendMessage("§7Entities in deiner Nähe: §c" + p.getNearbyEntities(5, 5, 5)
                    .stream()
                    .filter(entity -> entity.getType() != EntityType.PLAYER)
                            .filter(entity -> entity instanceof LivingEntity)
                            .filter(entity -> (entity instanceof ArmorStand) == false)
                            .map(entity -> (LivingEntity) entity)
                            .map(livingEntity -> (CraftLivingEntity) livingEntity)
                    .map(entity -> entity.getType().name() + " (" + p.getLocation().distance(entity.getLocation()) + (entity.getHandle().isInvisible() ? ", §4§lINVIS§c" : "") + ")")
                    .collect(Collectors.joining(", ")));
            return true;
        } else if (scmd.equalsIgnoreCase("debugpackets")) {
            if (p.hasPermission("muxsystem.developer") == false) {
                ms.sendNoCMDMessage(p);
                return true;
            }
            if (this.packetDebuggers.containsKey(p)) {
                this.packetDebuggers.remove(p).remove();
                p.sendMessage("§a§lRemoved packet debugger");
                return true;
            }
            CraftPlayer craftPlayer = (CraftPlayer) p;
            if (args.length == 1 && args[0].equalsIgnoreCase("in")) {
                PacketDuplexDebugger debugger = new PacketDuplexDebugger(p, craftPlayer.getHandle().playerConnection.networkManager);
                craftPlayer.getHandle().playerConnection.networkManager.channel.pipeline()
                        .addFirst("test1", debugger.adapter(
                                new ChannelInboundHandlerAdapter() {

                                    @Override
                                    public void channelRead(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
                                        super.channelRead(channelHandlerContext, o);
                                        debugger.handleFirst();
                                    }
                                }
                        ))
                        .addLast("test2", debugger.adapter(
                                new ChannelDuplexHandler() {
                                    @Override
                                    public void read(ChannelHandlerContext channelHandlerContext) throws Exception {
                                        super.read(channelHandlerContext);
                                        debugger.handleSecond();
                                    }
                                }
                        ));
                p.sendMessage("§a§ladded packet in debugger!");
                this.packetDebuggers.put(p, debugger);
                return true;
            } else if (args.length == 1 && args[0].equalsIgnoreCase("out")) {
                PacketDuplexDebugger debugger = new PacketDuplexDebugger(p, craftPlayer.getHandle().playerConnection.networkManager);
                craftPlayer.getHandle().playerConnection.networkManager.channel.pipeline()
                        .addFirst("test1", debugger.adapter(
                                new ChannelOutboundHandlerAdapter() {
                                    @Override
                                    public void write(ChannelHandlerContext channelHandlerContext, Object o, ChannelPromise channelPromise) throws Exception {
                                        super.write(channelHandlerContext, o, channelPromise);
                                        debugger.handleFirst();
                                    }
                                }
                        ))
                        .addLast("test2", debugger.adapter(
                                new ChannelDuplexHandler() {

                                    @Override
                                    public void write(ChannelHandlerContext channelHandlerContext, Object o, ChannelPromise channelPromise) throws Exception {
                                        debugger.handleSecond();
                                        super.write(channelHandlerContext, o, channelPromise);
                                    }
                                }
                        ));
                p.sendMessage("§a§ladded packet out debugger!");
                this.packetDebuggers.put(p, debugger);
            }

        } else if (scmd.equalsIgnoreCase("spawnertest")) {
            if (p.hasPermission("muxsystem.developer") == false) {
                ms.sendNoCMDMessage(p);
                return true;
            }
            ms.getAntiLags().spawnerTest = !ms.getAntiLags().spawnerTest;
            p.sendMessage("set spawner test to " + ms.getAntiLags().spawnerTest);
            return true;
        } else if (scmd.equalsIgnoreCase("droppeditemtest")) {
            if (p.hasPermission("muxsystem.developer") == false) {
                ms.sendNoCMDMessage(p);
                return true;
            }
            ms.getAntiLags().droppedItemTest = !ms.getAntiLags().droppedItemTest;
            p.sendMessage("set dropped item test to " + ms.getAntiLags().droppedItemTest);
            return true;
        }
        return false;
    }

    public void playAllSounds(final Player p) {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (final Sound s : Sound.values()) {
                    p.playSound(p.getLocation(), s, 1F, 1F);
                    p.sendMessage(s.name());
                    if (p.isSneaking()) break;
                    try {
                        Thread.sleep(1000);
                    } catch (final Exception ignored) {
                    }
                }
            }
        }.runTaskAsynchronously(ms);
    }

    // SCREAM
    /*
    target.playSound(target.getLocation(), Sound.GHAST_SCREAM, 10.0F, 1.0F);
                        target.playSound(target.getLocation(), Sound.GHAST_SCREAM2, 10.0F, 1.0F);
                        target.playSound(target.getLocation(), Sound.GHAST_MOAN, 10.0F, 1.0F);
                        target.playSound(target.getLocation(), Sound.GHAST_DEATH, 10.0F, 1.0F);
                        target.playSound(target.getLocation(), Sound.BAT_DEATH, 10.0F, 1.0F);
                        target.playSound(target.getLocation(), Sound.ANVIL_BREAK, 10.0F, 1.0F);
                        target.playSound(target.getLocation(), Sound.BLAZE_DEATH, 10.0F, 1.0F);
                        target.playSound(target.getLocation(), Sound.CAT_HISS, 10.0F, 1.0F);
                        target.playSound(target.getLocation(), Sound.CREEPER_DEATH, 10.0F, 1.0F);
                        target.playSound(target.getLocation(), Sound.CREEPER_HISS, 10.0F, 1.0F);
                        target.playSound(target.getLocation(), Sound.ENDERDRAGON_GROWL, 10.0F, 1.0F);
                        target.playSound(target.getLocation(), Sound.ENDERDRAGON_DEATH, 10.0F, 1.0F);
                        target.playSound(target.getLocation(), Sound.EXPLODE, 10.0F, 1.0F);
                        target.playSound(target.getLocation(), Sound.ENDERMAN_SCREAM, 10.0F, 1.0F);
                        target.playSound(target.getLocation(), Sound.SILVERFISH_KILL, 10.0F, 1.0F);


       PARANOIA

                                    Location tloc = this.target.getLocation();
                                    Location sloc = new Location(tloc.getWorld(), tloc.getX(), tloc.getY(), tloc.getZ());
                                    sloc.setX(sloc.getX() + (double)(rand.nextInt(8) - 4));
                                    sloc.setY(sloc.getY() + (double)(rand.nextInt(8) - 4));
                                    sloc.setZ(sloc.getZ() + (double)(rand.nextInt(8) - 4));
                                    double dist = Math.abs(tloc.getX() - sloc.getX()) + Math.abs(tloc.getY() - sloc.getY()) + Math.abs(tloc.getZ() - sloc.getZ());
                                    this.target.playSound(sloc, Sound.FUSE, (float)(32.0D / dist), 1.0F);
                                    this.target.playEffect(sloc, Effect.SMOKE, 4);


       ENDERCRYSTAL
                Location ori = ((Player)sender).getTargetBlock((HashSet)null, 100).getLocation().add(0.0D, 1.0D, 0.0D);
               Location fire = ori.clone().add(0.0D, 1.0D, 0.0D);
               if (ori.getBlock().isEmpty() && fire.getBlock().isEmpty()) {
                  ori.getWorld().spawn(ori.add(0.5D, 0.0D, 0.5D), EnderCrystal.class);
                  ori.getBlock().setType(Material.BEDROCK);
                  fire.getBlock().setType(Material.FIRE);



      GET TOR ADDRESSES

           try {
         URL oracle = new URL("http://exitlist.torproject.org/exit-addresses");
         URLConnection yc = oracle.openConnection();
         BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

         while(true) {
            String inputLine;
            if ((inputLine = in.readLine()) == null) {
               in.close();
               break;
            }

            if (inputLine.split(" ")[0].trim().toLowerCase().startsWith("exitaddress")) {
               String IP = inputLine.split(" ")[1].trim();
               TempBans.add(IP);
            }

            try {
               Thread.sleep(1L);
            } catch (InterruptedException var7) {
            }
         }
      } catch (IOException var8) {
         var8.printStackTrace();
         return;
      }



      REFRESH CHUNK


    private void refreshChunksAroundPlayer(final Player player, final int radius) {
        final Location location = player.getLocation();
        final Chunk centerChunk = location.getChunk();
        final int centerX = centerChunk.getX(), centerZ = centerChunk.getZ();
        final World world = location.getWorld();
        final EntityPlayer ep =  ((CraftPlayer) player).getHandle();
        final Set<Entity> entities = new HashSet<>();
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                final Chunk chunk = world.getChunkAt(x, z);
                final CraftChunk craftChunk = (CraftChunk) chunk;
                final PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(craftChunk.getHandle(), true, 65535);
                final Entity[] chunkentities = chunk.getEntities();
                entities.addAll(Arrays.asList(chunkentities));
                for (final Entity entity : chunkentities) {
                    final CraftEntity craftEntity = (CraftEntity) entity;
                    final net.minecraft.server.v1_8_R3.Entity nmsEntity = craftEntity.getHandle();
                    final PacketPlayOutEntityDestroy destroyPacket = new PacketPlayOutEntityDestroy(nmsEntity.getId());
                    ep.playerConnection.sendPacket(destroyPacket);
                }
                ep.playerConnection.sendPacket(packet);
            }
        }
        for (final Entity entity : entities) {
            final CraftEntity craftEntity = (CraftEntity) entity;
            final net.minecraft.server.v1_8_R3.Entity nmsEntity = craftEntity.getHandle();
            if (entity instanceof Player) {
                if (entity.getUniqueId().equals(player.getUniqueId()) == false) {
                    final PacketPlayOutNamedEntitySpawn playerPacket = new PacketPlayOutNamedEntitySpawn((EntityHuman) nmsEntity);
                    ep.playerConnection.sendPacket(playerPacket);
                }
            } else if (nmsEntity instanceof EntityLiving) {
                final PacketPlayOutSpawnEntityLiving entityPacket = new PacketPlayOutSpawnEntityLiving((EntityLiving) nmsEntity);
                ep.playerConnection.sendPacket(entityPacket);
            } else if (nmsEntity instanceof EntityPainting) {
                final PacketPlayOutSpawnEntityPainting paintingPacket = new PacketPlayOutSpawnEntityPainting((EntityPainting) nmsEntity);
                ep.playerConnection.sendPacket(paintingPacket);
            } else if (nmsEntity instanceof EntityItemFrame) {
                final EntityItemFrame itemFrame = (EntityItemFrame) nmsEntity;
                final PacketPlayOutSpawnEntity itemFramePacket = new PacketPlayOutSpawnEntity(nmsEntity, 71, itemFrame.blockPosition.getZ() << 4);
                ep.playerConnection.sendPacket(itemFramePacket);
            } else if (nmsEntity instanceof EntityMinecartAbstract) {
                final PacketPlayOutSpawnEntity minecartPacket = new PacketPlayOutSpawnEntity(nmsEntity, 10, ((EntityMinecartAbstract) nmsEntity).getType());
                ep.playerConnection.sendPacket(minecartPacket);
            } else if (nmsEntity instanceof EntityFallingBlock) {
                EntityFallingBlock fallingBlock = (EntityFallingBlock) nmsEntity;
                int blockId = Block.getId(fallingBlock.getBlock().getBlock());
                int data = fallingBlock.getBlock().getBlock().toLegacyData(fallingBlock.getBlock());
                int combinedId = (blockId & 4095) | (data & 15) << 12;
                final PacketPlayOutSpawnEntity fallingBlockPacket = new PacketPlayOutSpawnEntity(nmsEntity, 70, combinedId);
                ep.playerConnection.sendPacket(fallingBlockPacket);
            } else {
                player.sendMessage("No Entity: " + entity.getType().name());
            }
        }
        resendTileEntities(player, TileEntityFurnace.class, 1);
        resendTileEntities(player, TileEntityChest.class, 1);
        resendTileEntities(player, TileEntityEnderChest.class, 1);
        resendTileEntities(player, TileEntitySkull.class, 4);
        resendTileEntities(player, TileEntitySign.class, 9);
        resendTileEntities(player, TileEntityMobSpawner.class, 1);
        resendTileEntities(player, TileEntityNote.class, 3);
        resendTileEntities(player, TileEntityPiston.class, 0);
        resendTileEntities(player, TileEntityFlowerPot.class, 5);
        resendTileEntities(player, TileEntityBrewingStand.class, 2);
        resendTileEntities(player, TileEntityBeacon.class, 3);
        resendTileEntities(player, TileEntityHopper.class, 1);
        resendTileEntities(player, TileEntityDropper.class, 1);
        resendTileEntities(player, TileEntityDispenser.class, 1);
        resendTileEntities(player, TileEntityBanner.class, 6);
        resendTileEntities(player, TileEntityComparator.class, 2);
        resendTileEntities(player, TileEntityCommand.class, 2);
        ms.getHolograms().getIndividualHolograms().forEach(h -> {
            if (h.getPlayersTracking().contains(player)) {
                h.send(player);
                h.getAction().call(h, player);
            }
        });
    }
    private void resendTileEntities(Player player, Class<? extends TileEntity> tileEntityClass, int action) {
        WorldServer worldServer = ((CraftWorld) player.getWorld()).getHandle();
        List<TileEntity> tileEntities = new ArrayList<>();
        for (Object te : worldServer.tileEntityList.toArray()) {
            if (te.getClass().equals(tileEntityClass)) {
                tileEntities.add((TileEntity) te);
            }
        }
        EntityPlayer ep = ((CraftPlayer) player).getHandle();

        for (TileEntity tileEntity : tileEntities) {
            NBTTagCompound nbtTagCompound = new NBTTagCompound();
            tileEntity.b(nbtTagCompound);
            BlockPosition blockPosition = tileEntity.getPosition();
            PacketPlayOutTileEntityData tileEntityDataPacket = new PacketPlayOutTileEntityData(blockPosition, action, nbtTagCompound);
            ep.playerConnection.sendPacket(tileEntityDataPacket);
        }
    }
     */
    public void close() {
        this.ms = null;
    }


    private static class PacketDuplexDebugger {

        private final Player player;
        private final NetworkManager networkManager;
        private final Set<ChannelHandler> adapters;
        private long start = System.currentTimeMillis();
        private final List<Long> times = new ArrayList<>();
        private final List<Integer> ms = new ArrayList<>();

        public PacketDuplexDebugger(Player player, NetworkManager networkManager) {
            this.player = player;
            this.networkManager = networkManager;
            this.adapters = new HashSet<>();
        }

        public ChannelHandler adapter(ChannelHandler adapter) {
            this.adapters.add(adapter);
            return adapter;
        }

        public void remove() {
            adapters.forEach(channelOutboundHandlerAdapter -> this.networkManager.channel.pipeline().remove(channelOutboundHandlerAdapter));
            new BukkitRunnable() {
                @Override
                public void run() {
                    final long median = times.stream().filter(Objects::nonNull).mapToLong(value -> value).sum() / times.size();
                    final long max = times.stream().filter(Objects::nonNull).max(Comparator.comparingLong(value -> value)).orElse(0L);
                    final long min = times.stream().filter(Objects::nonNull).min(Comparator.comparingLong(value -> value)).orElse(0L);
                    final int pingMedian = ms.stream().filter(Objects::nonNull).mapToInt(value -> value).sum() / ms.size();
                    final int pingMax = ms.stream().filter(Objects::nonNull).max(Comparator.comparingInt(value -> value)).orElse(0);
                    final int pingMin = ms.stream().filter(Objects::nonNull).min(Comparator.comparingInt(value -> value)).orElse(0);
                    MuxSystem.getAPI().showItemBar(player, "§a§lPACKETS: §cMedian: §6" + median + "ms§c. Max: §6" + max + "ms§c. Min: §6" + min + "ms§c.");
                    MuxSystem.getAPI().showLater(player, "§a§lPING: §cMedian: §6" + pingMedian + "ms§c. Max: §6" + pingMax + "ms§c. Min: §6" + pingMin + "ms§c.");
                }
            }.runTaskLater(MuxSystem.getAPI(), 20 * 2);
        }

        public void handleFirst() {
            start = System.currentTimeMillis();
        }

        public void handleSecond() {
            final long time = System.currentTimeMillis() - start;
            MuxSystem.getAPI().getForkJoinPool().execute(() -> {
                MuxSystem.getAPI().showItemBar(player, "§cTook §6" + time + "ms §cto handle inbound packet.");
                times.add(time);
                ms.add(player.spigot().getPing());
            });
        }
    }
}