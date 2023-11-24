package me.muxteam.extras;

import com.google.common.collect.ImmutableSet;
import me.muxteam.basic.NMSReflection;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.basic.PathfinderGoalPlayerFollow;
import net.minecraft.server.v1_8_R3.World;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Material;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R3.util.UnsafeList;
import org.bukkit.entity.Entity;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleUpdateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class MuxMounts implements Listener {
    private MuxSystem ms;
    private final MuxCars extensioncars;
    private final ConcurrentHashMap<UUID, Mount> mounts = new ConcurrentHashMap<>();
    private final List<String> mounted = new ArrayList<>();

    public MuxMounts(final MuxSystem ms) {
        this.ms = ms;
        this.extensioncars = new MuxCars(ms);
        ms.registerCustomEntity(RideableSpider.class, "Spider", 52);
        ms.registerCustomEntity(RideableSlime.class, "Slime", 55);
        ms.registerCustomEntity(RideableHorse.class, "Horse", 100);
        ms.registerCustomEntity(RideablePig.class, "Pig", 90);
        ms.registerCustomEntity(RideableMagmaCube.class, "LavaSlime", 62);
        ms.registerCustomEntity(RideableSheep.class, "Sheep", 91);
        ms.registerCustomEntity(RideableIronGolem.class, "IronGolem", 99);
        ms.registerCustomEntity(FlyingSnake.class, "FlyingSnake", 62);
        ms.registerCustomEntity(RideableGuardian.class, "Guardian", 68);
        ms.registerCustomEntity(RideableMushroomCow.class, "MushroomCow", 96);
        ms.getServer().getPluginManager().registerEvents(this, ms);
        new BukkitRunnable() {
            @Override
            public void run() {
                mounts.values().forEach(mount -> {
                    if (mount.snake != null)
                        mount.snake.move();
                });
            }
        }.runTaskTimer(ms, 20L, 1L);
    }

    public void close() {
        for (final Mount m : mounts.values()) {
            ms.showItemBar(m.getOwner(), "§cDein Mount wurde deaktiviert.");
            m.remove();
        }
        extensioncars.close();
        ms.unregisterCustomEntity(RideableSpider.class);
        ms.unregisterCustomEntity(RideableSlime.class);
        ms.unregisterCustomEntity(RideableHorse.class);
        ms.unregisterCustomEntity(RideablePig.class);
        ms.unregisterCustomEntity(RideableMagmaCube.class);
        ms.unregisterCustomEntity(RideableSheep.class);
        ms.unregisterCustomEntity(RideableIronGolem.class);
        ms.unregisterCustomEntity(FlyingSnake.class);
        ms.unregisterCustomEntity(RideableGuardian.class);
        ms.unregisterCustomEntity(RideableMushroomCow.class);
        this.ms = null;
    }

    public void handleQuit(final UUID uuid) {
        removeMount(uuid);
    }

    public void deactivateMount(final Player p) {
        if (removeMount(p.getUniqueId())) {
            ms.showItemBar(p, "§cDein Mount wurde deaktiviert.");
        }
    }

    public boolean isMount(final LivingEntity entity) {
        for (final MuxMounts.Mount m : mounts.values()) {
            if (m.entity != null && m.entity.hashCode() == ((CraftLivingEntity) entity).getHandle().hashCode())
                return true;
        }
        return false;
    }

    public void deactivateMounts() {
        for (final UUID uuid : mounts.keySet()) {
            removeMount(uuid);
        }
    }

    public void updateMounts() {
        for (final Player p : ms.getServer().getOnlinePlayers()) {
            final Location l = p.getLocation();
            if (p.getGameMode() == GameMode.SPECTATOR || ms.in1vs1(p) || p.isDead() || ms.inDuel(p) || ms.getTrades().isTrading(p.getName()) || ms.isVanish(p) || ms.inBattle(p.getName(), l) || ms.inWar(p) || ms.inGame(p) || ms.inEvent(p) || ms.inCasino(p) || (p.getWorld().equals(ms.getWarpsWorld()) && ms.getShopRegion() != null && ms.getShopRegion().contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())))
                continue;
            final MuxExtraUser extraUser = ms.getExtras().getExtraUser(p.getUniqueId());
            if (mounts.containsKey(p.getUniqueId()) == false && extraUser.getMounts().getActive() != -1) {
                setMount(p, extraUser.getMounts().getActive(), false);
            }
        }
    }

    public void handleTeleport(final Player p, final TeleportCause c) {
        final short num = getMountType(p.getUniqueId());
        if (num != -1) {
            if (c != TeleportCause.UNKNOWN) {
                setMount(p, num, false);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if ((p.getWorld().equals(ms.getWarpsWorld()) && ms.getShopRegion() != null && ms.getShopRegion().contains(p.getLocation().getBlockX(), p.getLocation().getBlockY(), p.getLocation().getBlockZ()))
                                || ms.inEvent(p) || ms.in1vs1(p) || ms.inDuel(p) || p.getGameMode() == GameMode.SPECTATOR) {
                            return;
                        }
                        final Mount m = setMount(p, num, false);
                        if (m != null) {
                            m.setPassenger(p);
                            mounted.add(p.getName());
                        }
                    }
                }.runTaskLater(ms, 20L);
            } else if (p.getVehicle() != null) {
                mounted.add(p.getName());
            } else {
                mounted.remove(p.getName());
            }
        }
    }

    public Mount setMount(final Player p, final short num, final boolean clicked) {
        final UUID uuid = p.getUniqueId();
        if (mounts.containsKey(uuid)) {
            final Mount tmp = mounts.get(uuid);
            tmp.remove();
            mounts.remove(uuid);
            if (tmp.getID() == num) {
                return null;
            }
        }
        final Location l = p.getLocation();
        if (ms.getAdmin().MOUNTS.isActive() == false) {
            if (clicked) {
                ms.showItemBar(p, "§cMounts sind derzeit temporär deaktiviert.");
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                p.closeInventory();
            }
            return null;
        }
        final boolean inwarpworld = l.getWorld().equals(ms.getWarpsWorld());
        boolean ok = true;
        final short radius = 2;
        for (short x = -radius; x < radius; x++) {
            for (short y = 0; y < 3; y++) {
                for (short z = -radius; z < radius; z++) {
                    final Material mat = l.clone().add(x, y, z).getBlock().getType();
                    if (mat == Material.BEDROCK || mat == Material.BARRIER || inwarpworld && mat != Material.AIR) {
                        ok = false;
                    }
                }
            }
        }
        if (ok == false) {
            if (clicked) {
                ms.showItemBar(p, "§cHier ist nicht genug Platz, um dein Mount zu spawnen.");
            }
            return null;
        }
        final Mount m = new Mount(num, p);
        if (m.spawn() == false) {
            if (clicked) ms.showItemBar(p, "§cDein Mount konnte nicht gespawnt werden.");
            return null;
        }
        mounts.put(uuid, m);
        return m;
    }

    public boolean removeMount(final UUID uuid) {
        final Mount m = mounts.remove(uuid);
        if (m != null) {
            m.remove();
            return true;
        }
        final Player p = ms.getServer().getPlayer(uuid);
        if (p != null && p.isInsideVehicle()) { // For Friend Mounts (Bike)
            p.leaveVehicle();
        }
        return false;
    }

    public short getMountType(final UUID uuid) {
        final Mount m = mounts.get(uuid);
        if (m != null) {
            return m.getID();
        }
        return -1;
    }

    public void mount(final Player p) {
        final Mount m = mounts.get(p.getUniqueId());
        if (m != null) {
            m.setPassenger(p);
        }
    }

    public String getMountName(final short num) {
        switch (num) {
            case 1:
                return "Pferd des Schreckens";
            case 2:
                return "Eisgletscher Pferd";
            case 3:
                return "Packesel";
            case 4:
                return "Giftgrüner Schleim";
            case 5:
                return "Minecart";
            case 6:
                return "Kletternde Spinne";
            case 7:
                return "Untotes Ritterpferd";
            case 8:
                return "Fliegendes Rentier";
            case 9:
                return "Bike";
            case 10:
                return "Röpsendes Schwein";
            case 11:
                return "Feuerschleim";
            case 12:
                return "Liebendes Schaf";
            case 13:
                return "Titan";
            case 14:
                return "Fliegende Schlange";
            case 15:
                return "Chaotischer Guardian";
            case 16:
                return "Milkende Pilzkuh";
            case 17:
                return "Liga Mount";
            default:
                return "Mount";
        }
    }

    public short getMountFromMaterial(final Material m) {
        switch (m) {
            case BONE:
                return 1;
            case SNOW_BALL:
                return 2;
            case HAY_BLOCK:
                return 3;
            case SLIME_BALL:
                return 4;
            case MINECART:
                return 5;
            case WEB:
                return 6;
            case ROTTEN_FLESH:
                return 7;
            case GOLDEN_CARROT:
                return 8;
            case COAL:
                return 9;
            case PORK:
                return 10;
            case BLAZE_POWDER:
                return 11;
            case RED_ROSE:
                return 12;
            case IRON_INGOT:
                return 13;
            case MAGMA_CREAM:
                return 14;
            case PRISMARINE_SHARD:
                return 15;
            case HUGE_MUSHROOM_2:
                return 16;
            case IRON_BARDING:
                return 17;
            default:
                return -1;
        }
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_AIR && e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        final short num = getMountType(e.getPlayer().getUniqueId());
        if (num == 13) { // Iron Golem
            final EntityInsentient en = mounts.get(e.getPlayer().getUniqueId()).getEntity();
            if (en instanceof EntityIronGolem) {
                final EntityIronGolem golem = (EntityIronGolem) en;
                golem.world.broadcastEntityEffect(golem, (byte) 4);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(final PlayerInteractEntityEvent e) {
        final Entity en = e.getRightClicked();
        final net.minecraft.server.v1_8_R3.Entity em = ((CraftEntity) en).getHandle();
        if (em instanceof Rideable == false || en.getPassenger() != null) return;
        e.setCancelled(true);
        final Player p = e.getPlayer();
        final String name;
        if (en.getType() == EntityType.MINECART) {
            name = em.getName();
        } else {
            name = ChatColor.stripColor(em.getCustomName().split(" ")[0].replace("'s", "").replace("'", ""));
        }
        if (getMountType(p.getUniqueId()) == -1 || name.equals(p.getName()) == false) {
            ms.showItemBar(p, "§cDas ist nicht dein Mount.");
            return;
        }
        en.setPassenger(p);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeash(final PlayerLeashEntityEvent e) {
        final Entity en = e.getEntity();
        final net.minecraft.server.v1_8_R3.Entity em = ((CraftEntity) en).getHandle();
        if (em instanceof Rideable == false) return;
        final Player p = e.getPlayer();
        final String name;
        if (en.getType() == EntityType.MINECART) {
            name = em.getName();
        } else {
            name = ChatColor.stripColor(em.getCustomName().split(" ")[0].replace("'s", "").replace("'", ""));
        }
        if (getMountType(p.getUniqueId()) == -1 || name.equals(p.getName()) == false) {
            ms.showItemBar(p, "§cDas ist nicht dein Mount.");
            e.setCancelled(true);
            p.updateInventory();
        }
    }

    class Mount {
        private final short id;
        private EntityInsentient entity;
        private RideableMinecart minecart = null;
        private MuxCars.Bike bike = null;
        private FlyingSnake snake = null;
        private Player owner;

        public Mount(final short num, final Player owner) {
            this.id = num;
            this.owner = owner;
        }

        public boolean spawn() {
            if (id == 1) {
                entity = new RideableHorse(((CraftWorld) owner.getWorld()).getHandle());
                final Location loc = owner.getLocation();
                entity.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                final Horse h = (Horse) entity.getBukkitEntity();
                h.setAdult();
                h.setCarryingChest(false);
                h.setBreed(true);
                h.setTamed(true);
                h.setStyle(Horse.Style.NONE);
                h.setVariant(Horse.Variant.SKELETON_HORSE);
                h.getInventory().setSaddle(new ItemStack(Material.SADDLE));
                entity.world.addEntity(entity, SpawnReason.CUSTOM);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (entity.isAlive()) {
                            final Location l = entity.getBukkitEntity().getLocation();
                            l.getWorld().playEffect(l.add(0, 1, 0), Effect.MOBSPAWNER_FLAMES, 7);
                        } else {
                            this.cancel();
                        }
                    }
                }.runTaskTimer(ms, 0L, 10L);
            } else if (id == 2) {
                entity = new RideableHorse(((CraftWorld) owner.getWorld()).getHandle());
                final Location loc = owner.getLocation();
                entity.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                final Horse h = (Horse) entity.getBukkitEntity();
                h.setAdult();
                h.setCarryingChest(false);
                h.setBreed(true);
                h.setTamed(true);
                h.setStyle(Horse.Style.WHITEFIELD);
                h.setColor(Horse.Color.WHITE);
                h.setVariant(Horse.Variant.HORSE);
                h.getInventory().setSaddle(new ItemStack(Material.SADDLE));
                entity.world.addEntity(entity, SpawnReason.CUSTOM);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (entity.isAlive()) {
                            final Location l = entity.getBukkitEntity().getLocation();
                            l.getWorld().spigot().playEffect(l.add(0, 1, 0), Effect.SNOW_SHOVEL, 0, 0, 0.25F, 0.25F, 0.25F, 0.1F, 10, 2);
                        } else {
                            this.cancel();
                        }
                    }
                }.runTaskTimer(ms, 0L, 10L);
            } else if (id == 3) {
                entity = new RideableHorse(((CraftWorld) owner.getWorld()).getHandle());
                final Location loc = owner.getLocation();
                entity.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                final Horse h = (Horse) entity.getBukkitEntity();
                h.setAdult();
                h.setCarryingChest(true);
                h.setBreed(true);
                h.setTamed(true);
                h.setStyle(Horse.Style.NONE);
                h.setVariant(Horse.Variant.DONKEY);
                h.getInventory().setSaddle(new ItemStack(Material.SADDLE));
                entity.world.addEntity(entity, SpawnReason.CUSTOM);
            } else if (id == 4) {
                entity = new RideableSlime(((CraftWorld) owner.getWorld()).getHandle());
                final Location loc = owner.getLocation();
                entity.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                ((EntitySlime) entity).setSize(1);
                entity.world.addEntity(entity, SpawnReason.CUSTOM);
            } else if (id == 5) {
                minecart = new RideableMinecart(((CraftWorld) owner.getWorld()).getHandle());
                final Location loc = owner.getLocation();
                minecart.setLocation(loc.getX(), loc.getY() + 1D, loc.getZ(), loc.getYaw(), loc.getPitch());
                minecart.setCustomName(owner.getName());
                minecart.world.addEntity(minecart, SpawnReason.CUSTOM);
                return true;
            } else if (id == 6) {
                entity = new RideableSpider(((CraftWorld) owner.getWorld()).getHandle());
                final Location loc = owner.getLocation();
                entity.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                entity.world.addEntity(entity, SpawnReason.CUSTOM);
            } else if (id == 7) {
                entity = new RideableHorse(((CraftWorld) owner.getWorld()).getHandle());
                final Location loc = owner.getLocation();
                entity.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                final Horse h = (Horse) entity.getBukkitEntity();
                h.setAdult();
                h.setCarryingChest(false);
                h.setBreed(true);
                h.setTamed(true);
                h.setStyle(Horse.Style.NONE);
                h.setVariant(Horse.Variant.UNDEAD_HORSE);
                h.getInventory().setSaddle(new ItemStack(Material.SADDLE));
                entity.world.addEntity(entity, SpawnReason.CUSTOM);
            } else if (id == 8) {
                entity = new RideableHorse(((CraftWorld) owner.getWorld()).getHandle());
                final Location loc = owner.getLocation();
                entity.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                final Horse h = (Horse) entity.getBukkitEntity();
                h.setAdult();
                h.setCarryingChest(false);
                h.setBreed(true);
                h.setTamed(true);
                h.setStyle(Horse.Style.WHITE_DOTS);
                h.setColor(Horse.Color.CREAMY);
                h.setVariant(Horse.Variant.HORSE);
                h.getInventory().setSaddle(new ItemStack(Material.SADDLE));
                entity.world.addEntity(entity, SpawnReason.CUSTOM);
                h.setJumpStrength(6D);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (entity.isAlive()) {
                            if (entity.onGround == false) {
                                final Location l = entity.getBukkitEntity().getLocation();
                                l.getWorld().spigot().playEffect(l.add(0, 1, 0), Effect.CLOUD, 0, 0, 0.25F, 0.25F, 0.25F, 0.1F, 7, 2);
                            }
                        } else {
                            this.cancel();
                        }
                    }
                }.runTaskTimer(ms, 0L, 10L);
            } else if (id == 9) {
                if (((LivingEntity) owner).isOnGround() == false) return false;
                bike = extensioncars.spawnBike(owner.getLocation(), owner, "BLACK");
                return true;
            } else if (id == 10) {
                entity = new RideablePig(((CraftWorld) owner.getWorld()).getHandle());
                final Location loc = owner.getLocation();
                entity.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                entity.world.addEntity(entity, SpawnReason.CUSTOM);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (entity.isAlive()) {
                            ms.showItemBar(owner, "§fDein Schwein hat §egerülpst.");
                            final Location l = entity.getBukkitEntity().getLocation();
                            l.getWorld().playSound(l, Sound.BURP, 1F, 1F);
                            ms.playEffect(EnumParticle.CLOUD, l.add(0D, 1D, 0D), 0, 0, 0, 0.1F, 10);
                        } else {
                            this.cancel();
                        }
                    }
                }.runTaskTimer(ms, 1200L, 1200L);
            } else if (id == 11) {
                entity = new RideableMagmaCube(((CraftWorld) owner.getWorld()).getHandle());
                final Location loc = owner.getLocation();
                entity.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                ((EntitySlime) entity).setSize(2);
                entity.world.addEntity(entity, SpawnReason.CUSTOM);
            } else if (id == 12) {
                entity = new RideableSheep(((CraftWorld) owner.getWorld()).getHandle());
                final Location loc = owner.getLocation();
                entity.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                entity.world.addEntity(entity, SpawnReason.CUSTOM);
                new BukkitRunnable() {
                    boolean color = false;

                    @Override
                    public void run() {
                        if (entity.isAlive()) {
                            final CraftEntity en = entity.getBukkitEntity();
                            final Location l = en.getLocation();
                            ((Sheep) en).setColor(color ? DyeColor.RED : DyeColor.PINK);
                            color ^= true;
                            l.getWorld().spigot().playEffect(l.add(0, 1, 0), Effect.HEART, 0, 0, 0.25F, 0.25F, 0.25F, 0.1F, 7, 2);
                        } else {
                            this.cancel();
                        }
                    }
                }.runTaskTimer(ms, 0L, 20L);
            } else if (id == 13) {
                entity = new RideableIronGolem(((CraftWorld) owner.getWorld()).getHandle());
                final Location loc = owner.getLocation();
                entity.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                entity.world.addEntity(entity, SpawnReason.CUSTOM);
            } else if (id == 14) {
                entity = snake = new FlyingSnake(((CraftWorld) owner.getWorld()).getHandle());
                final Location loc = owner.getLocation();
                entity.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                final MagmaCube cube = (MagmaCube) snake.getBukkitEntity();
                cube.setSize(2);
                final List<ArmorStand> list = new ArrayList<>();
                for (int i = 0; i < 30; i++) {
                    final ArmorStand s = cube.getWorld().spawn(cube.getLocation().add(0, -1.25, 0), ArmorStand.class);
                    s.setGravity(false);
                    s.setVisible(false);
                    s.setHelmet(new ItemStack(Material.NETHER_BRICK));
                    list.add(s);
                }
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (snake != null && snake.isAlive() == false) {
                            snake.snake.forEach(Entity::remove);
                            snake = null;
                        }
                    }
                }.runTaskLater(ms, 20L);
                snake.snake = list;
                entity.world.addEntity(snake, SpawnReason.CUSTOM);
            } else if (id == 15) {
                entity = new RideableGuardian(((CraftWorld) owner.getWorld()).getHandle());
                final Location loc = owner.getLocation();
                entity.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                entity.world.addEntity(entity, SpawnReason.CUSTOM);
            } else if (id == 16) {
                entity = new RideableMushroomCow(((CraftWorld) owner.getWorld()).getHandle());
                final Location loc = owner.getLocation();
                entity.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                entity.world.addEntity(entity, SpawnReason.CUSTOM);
            } else if (id == 17) {
                entity = new RideableHorse(((CraftWorld) owner.getWorld()).getHandle());
                final Location loc = owner.getLocation();
                entity.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                final Horse h = (Horse) entity.getBukkitEntity();
                h.setAdult();
                h.setCarryingChest(false);
                h.setBreed(true);
                h.setTamed(true);
                final MuxUser u = ms.getMuxUser(owner.getName());
                final String league = ms.getStats().getLeague(u.getTrophies(), u.getLastRankedMatch() >= System.currentTimeMillis() - 172800000, u.getLastRankedMatch() != 0);
                h.getInventory().setArmor(
                        league.equals("Silber") ? new ItemStack(Material.IRON_BARDING) : league.equals("Gold") ? new ItemStack(Material.GOLD_BARDING) :
                                league.equals("Master") ? new ItemStack(Material.DIAMOND_BARDING) : new ItemStack(Material.AIR));
                h.setStyle(Horse.Style.WHITE_DOTS);
                h.setColor(Horse.Color.GRAY);
                h.setVariant(Horse.Variant.HORSE);
                h.getInventory().setSaddle(new ItemStack(Material.SADDLE));
                entity.world.addEntity(entity, SpawnReason.CUSTOM);
            } else {
                return false;
            }
            setMountProperties(entity);
            return entity.isAlive();
        }

        public void setMountProperties(final EntityInsentient entity) {
            final PathfinderGoalSelector goal = (PathfinderGoalSelector) NMSReflection.getObject(EntityInsentient.class, "goalSelector", entity),
                    target = (PathfinderGoalSelector) NMSReflection.getObject(EntityInsentient.class, "targetSelector", entity);

            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "c", goal))).clear();
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "b", goal))).clear();
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "c", target))).clear();
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "b", target))).clear();

            entity.getAttributeInstance(GenericAttributes.FOLLOW_RANGE).setValue(70.0D);
            if (goal != null) {
                goal.a(0, new PathfinderGoalLookAtPlayer(entity, EntityHuman.class, 6.0F));
            }
            if (snake != null) {
                if (goal != null) goal.a(1, new PathfinderGoalPlayerFollow(entity, this.owner, 1.4D) {
                    @Override
                    public void e() {
                        if (owner.getLocation().distance(new Location(owner.getWorld(), entity.locX, entity.locY, entity.locZ)) < 3) {
                            return;
                        }
                        final Location l = owner.getLocation();
                        final PathEntity pe = entity.getNavigation().a(l.getX() + 1.45, l.getY(), l.getZ() + 1);
                        entity.getNavigation().a(pe, 1.4);
                        if (snake.lastPassenger < System.currentTimeMillis() - 10000L)
                            snake.lastPassenger = System.currentTimeMillis() - 1000L;
                    }
                });
            } else if (goal != null) goal.a(1, new PathfinderGoalPlayerFollow(entity, this.owner, 1.4D));

            final String name = owner.getName(), lastchar = name.substring(name.length() - 1).toLowerCase();
            final boolean x = lastchar.equals("s") || lastchar.equals("x") || lastchar.equals("z");
            entity.setCustomName("§a" + name + "'" + (x ? "" : "s") + " §7" + getMountName(id));
            entity.setCustomNameVisible(true);

            entity.addEffect(new MobEffect(8, 9999999, 0, true, false));

            NMSReflection.setObject(net.minecraft.server.v1_8_R3.Entity.class, "uniqueID", entity, ms.getIdentificationId());
            NMSReflection.setObject(net.minecraft.server.v1_8_R3.Entity.class, "invulnerable", entity, true);
        }

        public short getID() {
            return id;
        }

        public void remove() {
            if (snake != null) {
                snake.die();
                snake.snake.forEach(Entity::remove);
                snake = null;
            } else if (entity != null) entity.die();
            else if (minecart != null) minecart.die();
            else if (bike != null) bike.remove();
        }

        public void setPassenger(final Player p) {
            if (entity != null) entity.getBukkitEntity().setPassenger(p);
            else if (minecart != null) minecart.getBukkitEntity().setPassenger(p);
            else if (bike != null) bike.mount(p);
        }

        public EntityInsentient getEntity() {
            return entity;
        }

        public void setOwner(final Player owner) {
            this.owner = owner;
        }

        public Player getOwner() {
            return this.owner;
        }
    }

    // MOUNT BUG FIX
    private final ImmutableSet<Integer> FULL_BLOCKS = ImmutableSet.of(
            1, 2, 3, 4, 5, 7, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 35, 41, 42, 43, 45, 46, 47, 48,
            49, 52, 56, 57, 58, 61, 62, 73, 74, 79, 80, 82, 84, 86, 87, 89, 91, 95, 97, 98, 99, 100, 103, 110, 112, 121,
            125, 129, 133, 137, 152, 153, 155, 158, 159, 161, 162, 166, 168, 169, 170, 172, 173, 174, 179, 181
    );

    private boolean isFullBlock(final Material m) {
        return FULL_BLOCKS.contains(m.getId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleExit(final VehicleExitEvent e) {
        if (e.getExited() instanceof Player == false) return;
        final Player p = (Player) e.getExited();
        final Location vloc = e.getVehicle().getLocation();
        Location l = p.getLocation();
        if (l.getY() > 250.0D) {
            l.add(0, 10, 0);
        } else if (isFullBlock(vloc.add(0.0D, 1.0D, 0.0D).getBlock().getType()) == false) {
            if (isFullBlock(vloc.getBlock().getType()) == false) {
                l = new Location(vloc.getWorld(), vloc.getBlockX() + 0.5, vloc.getBlockY(), vloc.getBlockZ() + 0.5, l.getYaw(), l.getPitch());
            } else {
                l.subtract(0, 1, 0);
            }
        }
        final Location tploc = l, original = p.getLocation();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (original.getWorld().equals(p.getLocation().getWorld()) == false || original.distance(p.getLocation()) > 2D)
                    return;
                p.teleport(tploc, TeleportCause.UNKNOWN);
            }
        }.runTask(ms);
    }

    // MINECART MOUNT
    @EventHandler
    public void onVehicleUpdate(final VehicleUpdateEvent e) {
        final Vehicle vehicle = e.getVehicle();
        final Entity passenger = vehicle.getPassenger();

        if (passenger instanceof Player == false) return;

        final Player p = (Player) passenger;

        final Material onblockm = vehicle.getLocation().subtract(0D, 1D, 0D).getBlock().getType(),
                almostblockm = vehicle.getLocation().subtract(0D, 2D, 0D).getBlock().getType(),
                almostblockm2 = vehicle.getLocation().add(0D, 2D, 0D).getBlock().getType();
        final boolean onblock = onblockm == Material.BEDROCK || onblockm == Material.BARRIER,
                almostblock = almostblockm == Material.BEDROCK || almostblockm2 == Material.BEDROCK;
        if (onblock || almostblock) {
            vehicle.eject();
            if (getMountType(p.getUniqueId()) != 5 || vehicle instanceof Boat) vehicle.remove();
            else deactivateMount(p);
            p.teleport(p.getLocation().add(0D, 1D, 0D));
            p.playEffect(p.getLocation(), Effect.EXTINGUISH, null);
            return;
        }

        if (vehicle instanceof Minecart == false) return;

        if (getMountType(p.getUniqueId()) != 5) return;

        if (p.isInsideVehicle() == false) return;

        final Minecart cart = (Minecart) vehicle;
        final double speed = 120.0D;
        final Vector playerVector = p.getVelocity();
        final Block under = vehicle.getLocation().clone().add(0.0D, -1.0D, 0.0D).getBlock();

        final Material materialUnder = under.getType();
        if (materialUnder.toString().contains("RAILS")) {
            return;
        }
        final Vector vector = playerVector.setX(p.getEyeLocation().getDirection().getX() / 140.0D * speed).setZ(p.getEyeLocation().getDirection().getZ() / 140.0D * speed);
        final float direction = p.getLocation().getYaw();
        vector.setY(0.0D);
        cart.getLocation().setYaw(direction);
        if (materialUnder == Material.AIR) {
            vector.setY(-0.8D);
        }
        final Location loc = vehicle.getLocation();
        if (loc.getY() - loc.getBlockY() > 0.5D && loc.getBlock().getType() != Material.AIR) {
            loc.setY(loc.getBlockY() + 1D);
            vector.setY(0.6F);
        }
        final Block next = getNextBlock(loc, vector);
        if (next != null && next.getType() != Material.AIR && isValid(next) && (next.getLocation().add(0.0D, 1.0D, 0.0D).getBlock().getType() == Material.AIR || isValid(next.getLocation().add(0.0D, 1.0D, 0.0D).getBlock()) == false)) {
            vector.setY(vector.getY() + 1.100000023841858D);
        }
        cart.setVelocity(vector);
        if (cart.getLocation().subtract(0.0D, 0.0D, 0.0D).getBlock().isLiquid()) {
            cart.eject();
            deactivateMount(p);
            p.playEffect(p.getLocation(), Effect.EXTINGUISH, null);
        }
    }

    @EventHandler
    public void onVehicleDestroy(final VehicleDestroyEvent e) {
        final net.minecraft.server.v1_8_R3.Entity em = ((CraftEntity) e.getVehicle()).getHandle();
        if (em instanceof Rideable == false) return;
        e.setCancelled(true);
    }

    private Block getNextBlock(final Location loc, final Vector v) {
        final int maxDistance = 2;
        final List<Block> blocks = new ArrayList<>();
        final Iterator<Block> itr = new BlockIterator(loc.getWorld(), loc.toVector(), v, 0.5D, maxDistance);
        while (itr.hasNext()) {
            final Block block = itr.next();
            if (isValid(block) && block.getLocation().equals(loc.getBlock().getLocation()) == false) {
                blocks.add(block);
            }
        }
        if (blocks.size() < 2) {
            return null;
        }
        return blocks.get(1);
    }

    private boolean isValid(final Block b) {
        final Material m = b.getType();
        return m != Material.YELLOW_FLOWER && m != Material.AIR && m != Material.LONG_GRASS && b.isLiquid() == false && m != Material.SAPLING && m != Material.DOUBLE_PLANT;
    }

    // OTHER MOUNTS

    public class RideableGuardian extends EntityGuardian implements Rideable {
        private Field bn, jump;

        public RideableGuardian(final World world) {
            super(world);
            try {
                bn = EntityLiving.class.getDeclaredField("bn");
                bn.setAccessible(true);
                jump = EntityLiving.class.getDeclaredField("aY");
                jump.setAccessible(true);
                Field field = EntityInsentient.class.getDeclaredField("lookController");
                field.setAccessible(true);
                field.set(this, new RideableGuardianLookController(this));
            } catch (final NoSuchFieldException | IllegalAccessException ex) {
                ex.printStackTrace();
            }
            b(true);
            this.moveController = new RideableGuardianMoveController(this);
        }

        @Override
        public void initAttributes() {
            super.initAttributes();
            this.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(0.3F);
        }

        @Override
        public void g(float f, float f1) {
            if (this.passenger == null || (this.passenger instanceof EntityHuman) == false) {
                double d0;
                float f2;
                if (this.bM()) {
                    float f3;
                    float f4;
                    if (this.V()) { // IN WATER
                        d0 = this.locY;
                        f3 = 0.8F;
                        f4 = 0.02F;
                        f2 = EnchantmentManager.b(this);
                        if (f2 > 3.0F) {
                            f2 = 3.0F;
                        }
                        if (this.onGround == false) {
                            f2 *= 0.5F;
                        }
                        if (f2 > 0.0F) {
                            f3 += (0.54600006F - f3) * f2 / 3.0F;
                            f4 += (this.bI() - f4) * f2 / 3.0F;
                        }
                        this.a(f, f1, f4);
                        this.move(this.motX, this.motY, this.motZ);
                        this.motX *= f3;
                        this.motY *= 0.800000011920929D;
                        this.motZ *= f3;
                        this.motY -= 0.02D;
                        if (this.positionChanged && this.c(this.motX, this.motY + 0.6000000238418579D - this.locY + d0, this.motZ)) {
                            this.motY = 0.30000001192092896D;
                        }
                    } else if (this.ab()) { // IN WATER
                        d0 = this.locY;
                        this.a(f, f1, 0.02F);
                        this.move(this.motX, this.motY, this.motZ);
                        this.motX *= 0.5D;
                        this.motY *= 0.5D;
                        this.motZ *= 0.5D;
                        this.motY -= 0.02D;
                        if (this.positionChanged && this.c(this.motX, this.motY + 0.6000000238418579D - this.locY + d0, this.motZ)) {
                            this.motY = 0.30000001192092896D;
                        }
                    } else {
                        float f5 = 0.91F;
                        if (this.onGround) {
                            f5 = this.world.getType(new BlockPosition(MathHelper.floor(this.locX), MathHelper.floor(this.getBoundingBox().b) - 1, MathHelper.floor(this.locZ))).getBlock().frictionFactor * 0.91F;
                        }

                        float f6 = 0.16277136F / (f5 * f5 * f5);
                        if (this.onGround) {
                            f3 = this.bI() * f6;
                        } else {
                            f3 = this.aM;
                        }

                        this.a(f, f1, f3);
                        f5 = 0.91F;
                        if (this.onGround) {
                            f5 = this.world.getType(new BlockPosition(MathHelper.floor(this.locX), MathHelper.floor(this.getBoundingBox().b) - 1, MathHelper.floor(this.locZ))).getBlock().frictionFactor * 0.91F;
                        }

                        boolean jumped = false;
                        if (this.k_()) { // LADDER
                            f4 = 0.15F;
                            this.motX = MathHelper.a(this.motX, -f4, f4);
                            this.motZ = MathHelper.a(this.motZ, -f4, f4);
                            this.fallDistance = 0.0F;
                            if (this.motY < -0.15D) {
                                this.motY = -0.15D;
                            }
                        } else if (shouldJump()) {
                            this.fallDistance = 0.0F;
                            this.motY += 0.6F;
                            jumped = true;
                        }
                        this.move(this.motX, this.motY, this.motZ);
                        if (jumped) {
                            final Vector v = getBukkitEntity().getLocation().getDirection().multiply(1.15);
                            this.motX = v.getX();
                            this.motZ = v.getZ();
                        }
                        if (this.positionChanged && this.k_()) {
                            this.motY = 0.2D;
                        }

                        if (this.world.isClientSide == false || this.world.isLoaded(new BlockPosition((int) this.locX, 0, (int) this.locZ)) && this.world.getChunkAtWorldCoords(new BlockPosition((int) this.locX, 0, (int) this.locZ)).o()) {
                            this.motY -= 0.08D;
                        } else if (this.locY > 0.0D) {
                            this.motY = -0.1D;
                        } else {
                            this.motY = 0.0D;
                        }

                        this.motY *= 0.9800000190734863D;
                        this.motX *= f5;
                        this.motZ *= f5;
                    }
                }

                this.aA = this.aB;
                d0 = this.locX - this.lastX;
                double d1 = this.locZ - this.lastZ;
                f2 = MathHelper.sqrt(d0 * d0 + d1 * d1) * 4.0F;
                if (f2 > 1.0F) {
                    f2 = 1.0F;
                }

                this.aB += (f2 - this.aB) * 0.4F;
                this.aC += this.aB;
                this.S = 0.5F;
                return;
            }

            final EntityHuman human = (EntityHuman) this.passenger;
            if (mounted.contains(human.getBukkitEntity().getName()) == false) {
                super.g(f, f1);
                this.S = 0.5F;
                return;
            }
            this.S = 1.0F;
            this.lastYaw = this.yaw = this.passenger.yaw;
            this.pitch = this.passenger.pitch * 0.5F;
            this.setYawPitch(this.yaw, this.pitch);
            this.aK = this.aI = this.yaw;
            f = ((EntityLiving) this.passenger).aZ * 0.5F;
            f1 = ((EntityLiving) this.passenger).ba;
            if (f1 <= 0.0F) {
                f1 *= 0.25F;
            }
            f *= 0.75F;
            this.k(0.2F); // THE SPEED
            super.g(f, f1);
            if (this.getBukkitEntity().getLocation().subtract(0.0D, 0.0D, 0.0D).getBlock().isLiquid()) {
                Player p = (Player) this.passenger.getBukkitEntity();
                this.getBukkitEntity().eject();
                deactivateMount(p);
                p.playEffect(p.getLocation(), Effect.EXTINGUISH, null);
                return;
            }
            if (jump != null && this.onGround) {
                try {
                    if (jump.getBoolean(this.passenger)) {
                        this.motY += 0.5F;
                        return;
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            if (this.motX != 0 || this.motZ != 0) {
                final Block under = getBukkitEntity().getLocation().clone().add(0.0D, -0.5D, 0.0D).getBlock();
                if (under != null && under.getType() != Material.AIR) {
                    this.motY += 0.1;
                    this.motZ *= 1.01;
                    this.motX *= 1.01;
                }
            }
        }

        private boolean shouldJump() {
            if (onGround == false) return false;
            final Location loc = getBukkitEntity().getLocation().add(getBukkitEntity().getLocation().getDirection());
            loc.setY(locY);
            final net.minecraft.server.v1_8_R3.Block nmsBlock = this.world.getType(
                    new BlockPosition(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())).getBlock();
            return (nmsBlock.w());
        }

        @Override
        public void m() {
            if (this.passenger != null && (this.passenger instanceof EntityHuman) == false) return;

            if (getBn() > 0) {
                decrementBn();
            }
            if (this.bc > 0) {
                double d0 = this.locX + (this.bd - this.locX) / (double) this.bc;
                double d1 = this.locY + (this.be - this.locY) / (double) this.bc;
                double d2 = this.locZ + (this.bf - this.locZ) / (double) this.bc;
                double d3 = MathHelper.g(this.bg - (double) this.yaw);
                this.yaw = (float) ((double) this.yaw + d3 / (double) this.bc);
                this.pitch = (float) ((double) this.pitch + (this.bh - (double) this.pitch) / (double) this.bc);
                --this.bc;
                this.setPosition(d0, d1, d2);
                this.setYawPitch(this.yaw, this.pitch);
            } else if (this.bM() == false) {
                this.motX *= 0.98D;
                this.motY *= 0.98D;
                this.motZ *= 0.98D;
            }

            if (Math.abs(this.motX) < 0.005D) {
                this.motX = 0.0D;
            }

            if (Math.abs(this.motY) < 0.005D) {
                this.motY = 0.0D;
            }

            if (Math.abs(this.motZ) < 0.005D) {
                this.motZ = 0.0D;
            }

            this.world.methodProfiler.a("ai");
            if (this.bD()) {
                this.aY = false;
                this.aZ = 0.0F;
                this.ba = 0.0F;
                this.bb = 0.0F;
            } else if (this.bM()) {
                this.world.methodProfiler.a("newAi");
                this.doTick();
                this.world.methodProfiler.b();
            }

            this.world.methodProfiler.b();
            this.world.methodProfiler.a("jump");
            if (this.aY) {
                if (this.V()) {
                    this.bG();
                } else if (this.ab()) {
                    this.bH();
                } else if (this.onGround && this.getBn() == 0) {
                    this.bF();
                    this.setBn(10);
                }
            } else {
                setBn(0);
            }

            this.world.methodProfiler.b();
            this.world.methodProfiler.a("travel");
            this.aZ *= 0.98F;
            this.ba *= 0.98F;
            this.bb *= 0.9F;
            this.g(this.aZ, this.ba);
            this.world.methodProfiler.b();
            this.world.methodProfiler.a("push");
            if (this.world.isClientSide == false) {
                this.bL();
            }

            this.world.methodProfiler.b();
        }

        private int getBn() {
            try {
                return (int) bn.get(this);
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
            return 0;
        }

        private void decrementBn() {
            try {
                bn.set(this, getBn() - 1);
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }

        private void setBn(int bn) {
            try {
                this.bn.set(this, bn);
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }

        // DISABLE GUARDIAN SOUNDS & EFFECTS

        @Override
        public void makeSound(final String s, final float f, final float f1) {}

        @Override
        protected void E() {}

        @Override
        public boolean r(final net.minecraft.server.v1_8_R3.Entity en) {
            return false;
        }

        @Override
        public void collide(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public void o(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public void m(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public Entity getEntity() {
            return this.getBukkitEntity();
        }

        class RideableGuardianLookController extends ControllerLook {
            public RideableGuardianLookController(EntityInsentient entityInsentient) {
                super(entityInsentient);
            }

            @Override
            public void a() {

            }
        }

        class RideableGuardianMoveController extends ControllerMove {
            private final RideableGuardian g;
            private Method lMethod;

            public RideableGuardianMoveController(final RideableGuardian g) {
                super(g);
                this.g = g;
                try {
                    lMethod = EntityGuardian.class.getDeclaredMethod("l", boolean.class);
                    lMethod.setAccessible(true);
                } catch (final NoSuchMethodException ex) {
                    ex.printStackTrace();
                }
            }

            @Override
            public void c() {
                if (this.f && this.g.getNavigation().m() == false) {
                    double var1 = this.b - this.g.locX;
                    double var3 = this.c - this.g.locY;
                    double var5 = this.d - this.g.locZ;
                    double var7 = var1 * var1 + var3 * var3 + var5 * var5;
                    var7 = MathHelper.sqrt(var7);
                    var3 /= var7;
                    float var9 = (float) (MathHelper.b(var5, var1) * 180.0D / Math.PI) - 90.0F;
                    this.g.yaw = this.a(this.g.yaw, var9, 30.0F);
                    this.g.aI = this.g.yaw;
                    float var10 = (float) (this.e * this.g.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).getValue());
                    this.g.k(this.g.bI() + (var10 - this.g.bI()) * 0.125F);
                    double var11 = Math.sin((double) (this.g.ticksLived + this.g.getId()) * 0.5D) * 0.05D;
                    double var13 = Math.cos(this.g.yaw * 3.1415927F / 180.0F);
                    double var15 = Math.sin(this.g.yaw * 3.1415927F / 180.0F);
                    g.motX += var11 * var13;
                    g.motZ += var11 * var15;
                    var11 = Math.sin((double) (this.g.ticksLived + this.g.getId()) * 0.75D) * 0.05D;
                    g.motY += var11 * (var15 + var13) * 0.25D;
                    g.motY += (double) this.g.bI() * var3 * 0.1D;

                    ControllerLook controllerLook = this.g.getControllerLook();
                    double var18 = this.g.locX + var1 / var7 * 2.0D;
                    double var20 = (double) this.g.getHeadHeight() + this.g.locY + var3 / var7;
                    double var22 = this.g.locZ + var5 / var7 * 2.0D;
                    double var24 = controllerLook.e();
                    double var26 = controllerLook.f();
                    double var28 = controllerLook.g();
                    if (controllerLook.b() == false) {
                        var24 = var18;
                        var26 = var20;
                        var28 = var22;
                    }
                    this.g.getControllerLook().a(var24 + (var18 - var24) * 0.125D, var26 + (var20 - var26) * 0.125D, var28 + (var22 - var28) * 0.125D, 10.0F, 40.0F);
                    invokeLMethod(true);
                } else {
                    this.g.k(0.0F);
                    invokeLMethod(false);
                }
            }

            private void invokeLMethod(boolean bool) {
                try {
                    lMethod.invoke(g, bool);
                } catch (final IllegalAccessException | InvocationTargetException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public class FlyingSnake extends EntityMagmaCube implements Rideable {
        public List<ArmorStand> snake = null;
        private long lastPassenger = -1;
        private int ticksDown = 10, possibleAirTicks = 0;

        public FlyingSnake(final World world) {
            super(world);
            try {
                final Field slimeMoveController = EntityInsentient.class.getDeclaredField("moveController");
                slimeMoveController.setAccessible(true);
                slimeMoveController.set(this, new ControllerMoveSlime(this));
            } catch (final Exception e) {
                e.printStackTrace();
            }
            b(true);
        }

        public void move() {
            if (getBukkitEntity().getPassenger() instanceof Player == false) {
                if (this.snake.isEmpty() == false) {
                    this.snake.forEach(Entity::remove);
                    this.snake.clear();
                    b(false);
                }
                return;
            }

            final Player p = (Player) getBukkitEntity().getPassenger();
            if (getBukkitEntity().getPassenger() == p) {
                final MagmaCube cube = (MagmaCube) getBukkitEntity();
                cube.teleport(p.getLocation());
                getBukkitEntity().setPassenger(p);
                cube.getLocation().setYaw(p.getLocation().getYaw());
                Vector vector;
                final Location location = getBukkitEntity().getLocation();
                if ((ms.getSpawnRegion().contains(location.getBlockX(), location.getBlockY(), location.getBlockZ()) == false
                        || ms.getGameSpawn().getWorld() != p.getWorld()) && (ms.getBase().isTrustedAtLocation(p) == false)) {
                    vector = p.getLocation().getDirection().normalize().multiply(0.2);
                    final Block under = getBukkitEntity().getLocation().clone().add(0.0D, -1.0D, 0.0D).getBlock();

                    vector.setY(0);
                    if (ticksDown-- == 0) {
                        final Location loc = getEntity().getLocation().add(0, 0, 0);
                        final Block next = getNextBlock(loc, vector);
                        if (next != null && next.getType() != Material.AIR && isValid(next)
                                && (next.getLocation().add(0.0D, 1.0D, 0.0D).getBlock().getType() == Material.AIR
                                || isValid(next.getLocation().add(0.0D, 1.0D, 0.0D).getBlock()) == false)) {
                            vector.setY(vector.getY() + 1.100000023841858D);
                            possibleAirTicks = 1;
                        } else if (possibleAirTicks > 0) {
                            vector.setY(-0.5);
                            possibleAirTicks--;
                            ticksDown = 1;
                            getBukkitEntity().setVelocity(vector);
                            return;
                        } else if (under.getType() == Material.AIR) {
                            final Block underUnder = under.getLocation().clone().add(0.0D, -1.0D, 0.0D).getBlock();
                            if (underUnder.getType() == Material.AIR) {
                                possibleAirTicks = 30;
                                ticksDown = 1;
                                return;
                            }
                            possibleAirTicks = 2;
                        }
                        ticksDown = 10;
                    }
                } else {
                    vector = p.getLocation().getDirection().multiply(0.3);
                }
                getBukkitEntity().setVelocity(vector);
            }
            moveTail();
            lastPassenger = System.currentTimeMillis();
        }

        @Override
        public void K() {}

        private void moveTail() {
            boolean passenger = bukkitEntity.getPassenger() != null;
            if (snake.isEmpty()) {
                for (int i = 0; i < 30; i++) {
                    ArmorStand s = getBukkitEntity().getWorld().spawn(getBukkitEntity().getLocation().add(0, -1.25, 0), ArmorStand.class);
                    s.setGravity(false);
                    s.setVisible(false);
                    s.setHelmet(new ItemStack(Material.NETHER_BRICK));
                    snake.add(s);
                }
                b(true);
            }
            for (int i = snake.size() - 1; i >= 0; i--) {
                if (i == 0) {
                    snake.get(i).teleport(getBukkitEntity().getLocation().add(0, -1.25, 0));
                    if (passenger)
                        ms.playEffect(EnumParticle.FLAME, getBukkitEntity().getLocation(), 0.5f, 0.5f, 0.5f, 0.0001f, 5);
                } else
                    snake.get(i).teleport(snake.get(i - 1).getLocation());
            }
        }

        @Override
        public boolean r(final net.minecraft.server.v1_8_R3.Entity entity) {
            return false;
        }

        @Override
        public void o(final net.minecraft.server.v1_8_R3.Entity entity) {}

        @Override
        protected void e(final EntityLiving entityliving) {}

        @Override
        public void collide(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public Entity getEntity() {
            return this.getBukkitEntity();
        }

        class ControllerMoveSlime extends ControllerMove {
            private float g;
            private int h;
            private final EntitySlime i;
            private boolean j;

            public ControllerMoveSlime(EntitySlime entityslime) {
                super(entityslime);
                this.i = entityslime;
            }

            public void a(float f, boolean flag) {
                this.g = f;
                this.j = flag;
            }

            public void a(double d0) {
                this.e = d0;
                this.f = true;
            }

            @Override
            public void c() {
                if (lastPassenger < System.currentTimeMillis() - 10_000L)
                    return;
                this.a.yaw = this.a(this.a.yaw, this.g, 30.0F);
                this.a.aK = this.a.yaw;
                this.a.aI = this.a.yaw;
                if (this.f == false) {
                    this.a.n(0.0F);
                } else {
                    this.f = false;
                    if (this.a.onGround) {
                        this.a.k((float) (this.e * this.a.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).getValue()));
                        if (this.h-- <= 0) {
                            this.h = 40;
                            if (this.j) {
                                this.h /= 3;
                            }
                        } else {
                            this.i.aZ = this.i.ba = 0.0F;
                            this.a.k(0.0F);
                        }
                    } else {
                        this.a.k((float) (this.e * this.a.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).getValue()));
                    }
                }
            }
        }
    }

    public class RideablePig extends EntityPig implements Rideable {
        private Field jump = null;

        public RideablePig(final World world) {
            super(world);
            try {
                jump = EntityLiving.class.getDeclaredField("aY");
                jump.setAccessible(true);
            } catch (final NoSuchFieldException ignored) {
            }
        }

        @Override
        public void g(float f, float f1) {
            if (this.passenger == null || this.passenger instanceof EntityHuman == false) {
                super.g(f, f1);
                this.S = 0.5F;
                return;
            }

            final EntityHuman human = (EntityHuman) this.passenger;
            if (mounted.contains(human.getBukkitEntity().getName()) == false) {
                super.g(f, f1);
                this.S = 0.5F;
                return;
            }
            this.S = 1.0F;
            this.lastYaw = this.yaw = this.passenger.yaw;
            this.pitch = this.passenger.pitch * 0.5F;
            this.setYawPitch(this.yaw, this.pitch);
            this.aK = this.aI = this.yaw;
            f = ((EntityLiving) this.passenger).aZ * 0.5F;
            f1 = ((EntityLiving) this.passenger).ba;
            if (f1 <= 0.0F) {
                f1 *= 0.25F;
            }
            f *= 0.75F;
            this.k(0.2F); // THE SPEED
            super.g(f, f1);
            if (jump != null && this.onGround) {
                try {
                    if (jump.getBoolean(this.passenger)) {
                        this.motY += 0.5F;
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public boolean r(final net.minecraft.server.v1_8_R3.Entity entity) {
            return false;
        }

        @Override
        public void collide(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public void o(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public Entity getEntity() {
            return this.getBukkitEntity();
        }
    }

    public class RideableSheep extends EntitySheep implements Rideable {
        private Field jump = null;

        public RideableSheep(final World world) {
            super(world);
            try {
                jump = EntityLiving.class.getDeclaredField("aY");
                jump.setAccessible(true);
            } catch (final NoSuchFieldException ignored) {
            }
        }

        @Override
        public void g(float f, float f1) {
            if (this.passenger == null || this.passenger instanceof EntityHuman == false) {
                super.g(f, f1);
                this.S = 0.5F;
                return;
            }

            final EntityHuman human = (EntityHuman) this.passenger;
            if (mounted.contains(human.getBukkitEntity().getName()) == false) {
                super.g(f, f1);
                this.S = 0.5F;
                return;
            }
            this.S = 1.0F;
            this.lastYaw = this.yaw = this.passenger.yaw;
            this.pitch = this.passenger.pitch * 0.5F;
            this.setYawPitch(this.yaw, this.pitch);
            this.aK = this.aI = this.yaw;
            f = ((EntityLiving) this.passenger).aZ * 0.5F;
            f1 = ((EntityLiving) this.passenger).ba;
            if (f1 <= 0.0F) {
                f1 *= 0.25F;
            }
            f *= 0.75F;
            this.k(0.2F); // THE SPEED
            super.g(f, f1);
            if (jump != null && this.onGround) {
                try {
                    if (jump.getBoolean(this.passenger)) {
                        this.motY += 0.5F;
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public boolean r(final net.minecraft.server.v1_8_R3.Entity entity) {
            return false;
        }

        @Override
        public void collide(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public void o(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public Entity getEntity() {
            return this.getBukkitEntity();
        }
    }

    public class RideableMushroomCow extends EntityMushroomCow implements Rideable {
        private Field jump = null;

        public RideableMushroomCow(final World world) {
            super(world);
            try {
                jump = EntityLiving.class.getDeclaredField("aY");
                jump.setAccessible(true);
            } catch (final NoSuchFieldException ignored) {
            }
        }

        @Override
        public void g(float f, float f1) {
            if (this.passenger == null || this.passenger instanceof EntityHuman == false) {
                super.g(f, f1);
                this.S = 0.5F;
                return;
            }

            final EntityHuman human = (EntityHuman) this.passenger;
            if (mounted.contains(human.getBukkitEntity().getName()) == false) {
                super.g(f, f1);
                this.S = 0.5F;
                return;
            }
            this.S = 1.0F;
            this.lastYaw = this.yaw = this.passenger.yaw;
            this.pitch = this.passenger.pitch * 0.5F;
            this.setYawPitch(this.yaw, this.pitch);
            this.aK = this.aI = this.yaw;
            f = ((EntityLiving) this.passenger).aZ * 0.5F;
            f1 = ((EntityLiving) this.passenger).ba;
            if (f1 <= 0.0F) {
                f1 *= 0.25F;
            }
            f *= 0.75F;
            this.k(0.2F); // THE SPEED
            super.g(f, f1);
            if (jump != null && this.onGround) {
                try {
                    if (jump.getBoolean(this.passenger)) {
                        this.motY += 0.5F;
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public boolean r(final net.minecraft.server.v1_8_R3.Entity entity) {
            return false;
        }

        @Override
        public void collide(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public void o(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public Entity getEntity() {
            return this.getBukkitEntity();
        }
    }

    public class RideableSpider extends EntitySpider implements Rideable {
        private Field jump = null;

        public RideableSpider(final World world) {
            super(world);
            try {
                jump = EntityLiving.class.getDeclaredField("aY");
                jump.setAccessible(true);
            } catch (final NoSuchFieldException ignored) {
            }
        }

        @Override
        public void g(float f, float f1) {
            if (this.passenger == null || this.passenger instanceof EntityHuman == false) {
                super.g(f, f1);
                this.S = 0.5F;
                return;
            }

            final EntityHuman human = (EntityHuman) this.passenger;
            if (mounted.contains(human.getBukkitEntity().getName()) == false) {
                super.g(f, f1);
                this.S = 0.5F;
                return;
            }
            this.S = 1.0F;
            this.lastYaw = this.yaw = this.passenger.yaw;
            this.pitch = this.passenger.pitch * 0.5F;
            this.setYawPitch(this.yaw, this.pitch);
            this.aK = this.aI = this.yaw;
            f = ((EntityLiving) this.passenger).aZ * 0.5F;
            f1 = ((EntityLiving) this.passenger).ba;
            if (f1 <= 0.0F) {
                f1 *= 0.25F;
            }
            f *= 0.75F;
            this.k(0.2F); // THE SPEED
            super.g(f, f1);
            if (jump != null && this.onGround) {
                try {
                    if (jump.getBoolean(this.passenger)) {
                        this.motY += 0.5F;
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public boolean r(final net.minecraft.server.v1_8_R3.Entity entity) {
            return false;
        }

        @Override
        public void collide(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public void o(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public Entity getEntity() {
            return this.getBukkitEntity();
        }
    }

    public class RideableMagmaCube extends EntityMagmaCube implements Rideable {
        private Field jump = null;

        public RideableMagmaCube(final World world) {
            super(world);
            try {
                jump = EntityLiving.class.getDeclaredField("aY");
                jump.setAccessible(true);
            } catch (final NoSuchFieldException ignored) {
            }
        }

        @Override
        public void g(float f, float f1) {
            if (this.passenger == null || this.passenger instanceof EntityHuman == false) {
                super.g(f, f1);
                this.S = 0.5F;
                return;
            }

            final EntityHuman human = (EntityHuman) this.passenger;
            if (mounted.contains(human.getBukkitEntity().getName()) == false) {
                super.g(f, f1);
                this.S = 0.5F;
                return;
            }
            this.S = 1.0F;
            this.lastYaw = this.yaw = this.passenger.yaw;
            this.pitch = this.passenger.pitch * 0.5F;
            this.setYawPitch(this.yaw, this.pitch);
            this.aK = this.aI = this.yaw;
            f = ((EntityLiving) this.passenger).aZ * 0.5F;
            f1 = ((EntityLiving) this.passenger).ba;
            if (f1 <= 0.0F) {
                f1 *= 0.25F;
            }
            f *= 0.75F;
            this.k(0.2F); // THE SPEED
            super.g(f, f1);
            if (jump != null && this.onGround) {
                this.motY = 0.5F; // SLIME SHOULD ALWAYS JUMP
                try {
                    if (jump.getBoolean(this.passenger)) {
                        this.motY += 0.5F;
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public boolean r(final net.minecraft.server.v1_8_R3.Entity entity) {
            return false;
        }

        @Override
        protected void e(final EntityLiving entityliving) {}

        @Override
        public void collide(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public void o(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public Entity getEntity() {
            return this.getBukkitEntity();
        }
    }

    public class RideableSlime extends EntitySlime implements Rideable {
        private Field jump = null;

        public RideableSlime(final World world) {
            super(world);
            try {
                jump = EntityLiving.class.getDeclaredField("aY");
                jump.setAccessible(true);
            } catch (final NoSuchFieldException ignored) {
            }
        }

        @Override
        public void g(float f, float f1) {
            if (this.passenger == null || this.passenger instanceof EntityHuman == false) {
                super.g(f, f1);
                this.S = 0.5F;
                return;
            }

            final EntityHuman human = (EntityHuman) this.passenger;
            if (mounted.contains(human.getBukkitEntity().getName()) == false) {
                super.g(f, f1);
                this.S = 0.5F;
                return;
            }
            this.S = 1.0F;
            this.lastYaw = this.yaw = this.passenger.yaw;
            this.pitch = this.passenger.pitch * 0.5F;
            this.setYawPitch(this.yaw, this.pitch);
            this.aK = this.aI = this.yaw;
            f = ((EntityLiving) this.passenger).aZ * 0.5F;
            f1 = ((EntityLiving) this.passenger).ba;
            if (f1 <= 0.0F) {
                f1 *= 0.25F;
            }
            f *= 0.75F;
            this.k(0.2F); // THE SPEED
            super.g(f, f1);
            if (jump != null && this.onGround) {
                this.motY = 0.5F; // SLIME SHOULD ALWAYS JUMP
                try {
                    if (jump.getBoolean(this.passenger)) {
                        this.motY += 0.5F;
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public boolean r(final net.minecraft.server.v1_8_R3.Entity entity) {
            return false;
        }

        @Override
        protected void e(final EntityLiving entityliving) {}

        @Override
        public void collide(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public void o(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public Entity getEntity() {
            return this.getBukkitEntity();
        }
    }

    public class RideableIronGolem extends EntityIronGolem implements Rideable {
        private Field jump = null;

        public RideableIronGolem(final World world) {
            super(world);
            try {
                jump = EntityLiving.class.getDeclaredField("aY");
                jump.setAccessible(true);
            } catch (final NoSuchFieldException ignored) {
            }
        }

        @Override
        public void g(float f, float f1) {
            if (this.passenger == null || this.passenger instanceof EntityHuman == false) {
                super.g(f, f1);
                this.S = 0.5F;
                return;
            }

            final EntityHuman human = (EntityHuman) this.passenger;
            if (mounted.contains(human.getBukkitEntity().getName()) == false) {
                super.g(f, f1);
                this.S = 0.5F;
                return;
            }
            this.S = 1.0F;
            this.lastYaw = this.yaw = this.passenger.yaw;
            this.pitch = this.passenger.pitch * 0.5F;
            this.setYawPitch(this.yaw, this.pitch);
            this.aK = this.aI = this.yaw;
            f = ((EntityLiving) this.passenger).aZ * 0.5F;
            f1 = ((EntityLiving) this.passenger).ba;
            if (f1 <= 0.0F) {
                f1 *= 0.25F;
            }
            f *= 0.75F;
            this.k(0.2F); // THE SPEED
            super.g(f, f1);
            if (jump != null && this.onGround) {
                try {
                    if (jump.getBoolean(this.passenger)) {
                        this.motY += 0.5F;
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public boolean r(final net.minecraft.server.v1_8_R3.Entity entity) {
            return false;
        }

        @Override
        public void collide(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public void o(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public Entity getEntity() {
            return this.getBukkitEntity();
        }
    }

    public class RideableHorse extends EntityHorse implements Rideable {
        public RideableHorse(final World world) {
            super(world);
        }

        @Override
        public void collide(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public void o(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public void g(final EntityHuman entityhuman) {}

        @Override
        public Entity getEntity() {
            return this.getBukkitEntity();
        }
    }

    public class RideableMinecart extends EntityMinecartRideable implements Rideable {
        public RideableMinecart(final World world) {
            super(world);
        }

        @Override
        public void collide(final net.minecraft.server.v1_8_R3.Entity en) {}

        @Override
        public Entity getEntity() {
            return this.getBukkitEntity();
        }
    }

    public interface Rideable {
        Entity getEntity();
    }

    public final class MountStore {
        private short active;
        private final Set<Short> owned;

        public MountStore(final Set<Short> owned, short active) {
            this.owned = owned;
            this.active = active;
        }

        public short getActive() {
            return active;
        }

        public void setActive(final short active) {
            this.active = active;
        }

        public Set<Short> getOwned() {
            return owned;
        }

        public void addOwned(final short perk) {
            owned.add(perk);
        }

        public Integer getSize() {
            return this.owned.size();
        }
    }
}