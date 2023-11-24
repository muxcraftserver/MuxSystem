package me.muxteam.basic;

import com.sk89q.worldguard.bukkit.ConfigurationManager;
import com.sk89q.worldguard.bukkit.WGBukkit;
import me.muxteam.basic.MuxActions.HologramAction;
import me.muxteam.basic.MuxActions.PlayerAction;
import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.*;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.util.UnsafeList;
import org.bukkit.entity.*;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MuxNPCs implements Listener {
    private MuxSystem ms;
    private final CopyOnWriteArrayList<MuxNPC> npcs = new CopyOnWriteArrayList<>();
    private final ConcurrentHashMap<Integer, MuxPlayerNPC> playernpcs = new ConcurrentHashMap<>();
    private final Set<Player> npcinteract = new HashSet<>();

    public MuxNPCs(final MuxSystem ms) {
        this.ms = ms;
        ms.registerCustomEntity(MuxVillager.class, "Villager", 120);
        ms.registerCustomEntity(MuxCreeper.class, "Creeper", 50);
        ms.registerCustomEntity(MuxWitch.class, "Witch", 66);
        ms.registerCustomEntity(MuxSkeleton.class, "Skeleton", 51);
        ms.registerCustomEntity(MuxSheep.class, "Sheep", 91);
        ms.registerCustomEntity(MuxMooshroom.class, "MushroomCow", 96);
        ms.registerCustomEntity(MuxPigZombie.class, "PigZombie", 57);
        ms.registerCustomEntity(MuxIronGolem.class, "IronGolem", 99);
        ms.registerCustomEntity(MuxWolf.class, "Wolf", 95);
        new BukkitRunnable() {
            @Override
            public void run() {
                refreshPlayerNPCs();
            }
        }.runTaskTimer(ms, 10L, 40L);
        removeOld();
        ms.getServer().getPluginManager().registerEvents(this, ms);
    }

    public void close() {
        playernpcs.clear();
        ms.unregisterCustomEntity(MuxVillager.class);
        ms.unregisterCustomEntity(MuxCreeper.class);
        ms.unregisterCustomEntity(MuxWitch.class);
        ms.unregisterCustomEntity(MuxSkeleton.class);
        ms.unregisterCustomEntity(MuxSheep.class);
        ms.unregisterCustomEntity(MuxMooshroom.class);
        ms.unregisterCustomEntity(MuxPigZombie.class);
        ms.unregisterCustomEntity(MuxIronGolem.class);
        ms.unregisterCustomEntity(MuxWolf.class);
        this.ms = null;
    }

    public void respawn() {
        npcs.forEach(fv -> {
            if (fv.getEntity().dead) {
                final Entity en = fv.getEntity();
                en.dead = false;
                en.world.addEntity(en);
                final Location l = fv.getLocation();
                en.setPosition(l.getX(), l.getY(), l.getZ());
            }
        });
    }

    public void removeNPC(final PlayerAction action, boolean individual) {
        npcs.removeIf(mv -> {
            if (mv.getAction() == action) {
                if (individual)
                    ms.getHolograms().removeIndividualHologram("npc" + mv.getEntity().getId());
                else
                    ms.getHolograms().removeHologram("npc" + mv.getEntity().getId());
                mv.getEntity().dead = true;
                return true;
            }
            return false;
        });
    }

    public void changeName(final PlayerAction action, final String name) {
        npcs.forEach(mv -> {
            if (mv.getAction().equals(action)) {
                final MuxHolograms.Hologram holo = mv.getHologram();
                if (holo != null) holo.setMessage(name);
                mv.setHologram(name, null);
            }
        });
    }

    public void addSheep(final EnumColor color, final Location l, final BlockFace bf, final String name, final PlayerAction action) {
        final MuxSheep npc = new MuxSheep(color, l, bf);
        addNPC(npc, name, action, null);
    }

    public void addWolf(final Location l, final BlockFace bf, final String name, final PlayerAction action) {
        final MuxWolf npc = new MuxWolf(l, bf);
        addNPC(npc, name, action, null);
    }

    public void addMooshroom(final Location l, final BlockFace bf, final String name, final PlayerAction action) {
        final MuxMooshroom npc = new MuxMooshroom(l, bf);
        addNPC(npc, name, action, null);
    }

    public void addVillager(final int profession, final Location l, final BlockFace bf, final String name, final PlayerAction action) {
        addVillager(profession, l, bf, name, action, null);
    }

    public void addVillager(final int profession, final Location l, final BlockFace bf, final String name, final PlayerAction action, final HologramAction haction) {
        final ConfigurationManager cm = WGBukkit.getPlugin().getGlobalStateManager();
        final boolean wasactive = cm.activityHaltToggle;
        cm.activityHaltToggle = false;
        final MuxVillager npc = new MuxVillager(profession, l, bf);
        addNPC(npc, name, action, haction);
        if (wasactive) cm.activityHaltToggle = true;
    }

    public void addPigZombie(final ItemStack hand, final Location l, final BlockFace bf, final String name, final PlayerAction action) {
        final MuxPigZombie npc = new MuxPigZombie(hand, l, bf);
        addNPC(npc, name, action, null);
    }

    public void addPigZombie(final ItemStack hand, final Location l, final BlockFace bf, final String name, final PlayerAction action, final HologramAction haction) {
        final ConfigurationManager cm = WGBukkit.getPlugin().getGlobalStateManager();
        final boolean wasactive = cm.activityHaltToggle;
        cm.activityHaltToggle = false;
        final MuxPigZombie npc = new MuxPigZombie(hand, l, bf);
        addNPC(npc, name, action, haction);
        if (wasactive) cm.activityHaltToggle = true;
    }

    public void addWitch(final Location l, final BlockFace bf, final String name, final PlayerAction action) {
        final MuxWitch npc = new MuxWitch(0, l, bf);
        addNPC(npc, name, action, null);
    }

    public void addSkeleton(final SkeletonType type, final ItemStack hand, final Location l, final BlockFace bf, final String name, final PlayerAction action) {
        final MuxSkeleton npc = new MuxSkeleton(type, hand, l, bf);
        addNPC(npc, name, action, null);
    }

    public void addCreeper(final boolean lightning, final Location l, final BlockFace bf, final String name, final PlayerAction action) {
        final ConfigurationManager cm = WGBukkit.getPlugin().getGlobalStateManager();
        final boolean wasactive = cm.activityHaltToggle;
        cm.activityHaltToggle = false;
        final MuxCreeper npc = new MuxCreeper(lightning, l, bf);
        addNPC(npc, name, action, null);
        if (wasactive) cm.activityHaltToggle = true;
    }

    public void addIronGolem(final Location l, final BlockFace bf, final String name, final PlayerAction action) {
        final MuxIronGolem npc = new MuxIronGolem(l, bf);
        addNPC(npc, name, action, null);
    }

    public void spawnPlayerNPC(final String skin, final String nametag,
                               final String tablist, final Location loc, final ItemStack hand, final PlayerAction action) {
        final MuxPlayerNPC s = new MuxPlayerNPC(skin, tablist, loc.clone().add(0, 0, -1), hand.getType(), nametag);
        playernpcs.put(s.getEntityID(), s);
        s.setAction(action);
    }

    public void removeOld() {
        final UUID uuid = ms.getIdentificationId();
        for (final World x : ms.getServer().getWorlds()) {
            x.getEntitiesByClass(Villager.class).forEach(xe -> {
                if (((CraftVillager) xe).getHandle().getUniqueID().equals(uuid) && ((CraftVillager) xe).getHandle() instanceof MuxVillager == false) {
                    xe.remove();
                }
            });
            x.getEntitiesByClass(Creeper.class).forEach(xe -> {
                if (((CraftCreeper) xe).getHandle().getUniqueID().equals(uuid) && ((CraftCreeper) xe).getHandle() instanceof MuxCreeper == false) {
                    xe.remove();
                }
            });
            x.getEntitiesByClass(Witch.class).forEach(xe -> {
                if (((CraftWitch) xe).getHandle().getUniqueID().equals(uuid) && ((CraftWitch) xe).getHandle() instanceof MuxWitch == false) {
                    xe.remove();
                }
            });
            x.getEntitiesByClass(Sheep.class).forEach(xe -> {
                if (((CraftSheep) xe).getHandle().getUniqueID().equals(uuid) && ((CraftSheep) xe).getHandle() instanceof MuxSheep == false) {
                    xe.remove();
                }
            });
            x.getEntitiesByClass(MushroomCow.class).forEach(xe -> {
                if (((CraftMushroomCow) xe).getHandle().getUniqueID().equals(uuid) && ((CraftMushroomCow) xe).getHandle() instanceof MuxMooshroom == false) {
                    xe.remove();
                }
            });
            x.getEntitiesByClass(PigZombie.class).forEach(xe -> {
                if (((CraftPigZombie) xe).getHandle().getUniqueID().equals(uuid) && ((CraftPigZombie) xe).getHandle() instanceof MuxPigZombie == false) {
                    xe.remove();
                }
            });
            x.getEntitiesByClass(Skeleton.class).forEach(xe -> {
                if (((CraftSkeleton) xe).getHandle().getUniqueID().equals(uuid) && ((CraftSkeleton) xe).getHandle() instanceof MuxSkeleton == false) {
                    xe.remove();
                }
            });
            x.getEntitiesByClass(IronGolem.class).forEach(xe -> {
                if (((CraftIronGolem) xe).getHandle().getUniqueID().equals(uuid) && ((CraftIronGolem) xe).getHandle() instanceof MuxIronGolem == false) {
                    xe.remove();
                }
            });
            x.getEntitiesByClass(Wolf.class).forEach(xe -> {
                if (((CraftWolf) xe).getHandle().getUniqueID().equals(uuid) && ((CraftWolf) xe).getHandle() instanceof MuxWolf == false) {
                    xe.remove();
                }
            });
        }
    }

    public void handleNPC(final MuxNPC npc, final Player p) {
        if (ms.checkGeneralCooldown(p.getName(), "NPC", 300L, true)) return;
        npc.executeAction(p);
    }

    public void handlePlayerNPC(final Player p, final int entid, final PacketPlayInUseEntity packet) {
        final MuxPlayerNPC s = playernpcs.get(entid);
        if (s != null) {
            npcinteract.add(p);
            if (ms.checkGeneralCooldown(p.getName(), "NPC", 1500L, true)) return;
            new BukkitRunnable() {
                @Override
                public void run() {
                    s.executeAction(p);
                }
            }.runTask(ms);
            ms.getAnalytics().getAnalytics().addNPCUse(s.getName());
        } else {
            ms.getPvPBots().handlePvPBot(p, entid, packet);
        }
    }

    private void addNPC(final MuxNPC npc, final String name, final PlayerAction action, final HologramAction haction) {
        npc.setAction(action);
        npcs.add(npc);
        final MuxHolograms holograms = ms.getHolograms();
        if (haction != null) {
            holograms.addIndividualHologram("npc" + npc.getEntity().getId(), npc.getHologramLocation(), haction);
            holograms.saveChunk(npc.getHologramLocation()); // Hologram is fake, still need to save chunk for NPC
            npc.setHologram(name, null);
            return;
        }
        npc.setHologram(name, holograms.addHologram("npc" + npc.getEntity().getId(), npc.getHologramLocation(), name));
    }

    private void refreshPlayerNPCs() {
        for (final MuxPlayerNPC npc : playernpcs.values()) {
            npc.refreshNPC();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(final PlayerInteractEvent e) {
        if (npcinteract.remove(e.getPlayer())) {
            e.setUseItemInHand(Event.Result.DENY);
            e.getPlayer().updateInventory();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(final PlayerInteractEntityEvent e) {
        final org.bukkit.entity.Entity en = e.getRightClicked();
        final net.minecraft.server.v1_8_R3.Entity em = ((CraftEntity) en).getHandle();
        if (em instanceof MuxNPC == false) return;
        e.setCancelled(true);
        this.npcinteract.add(e.getPlayer());
        final MuxNPC npc = (MuxNPC) em;
        handleNPC(npc, e.getPlayer());
    }

    final class MuxVillager extends EntityVillager implements MuxNPC {
        private PlayerAction action = null;
        private MuxHolograms.Hologram holo = null;
        private String hname;
        private final Location loc;

        public MuxVillager(final int profession, final Location l, final BlockFace bf) {
            super(((CraftWorld) l.getWorld()).getHandle());
            this.setProfession(profession);
            this.loc = l;
            this.setPosition(l.getX(), l.getY(), l.getZ());

            // REMOVE GOALS
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "b", goalSelector))).clear();
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "c", goalSelector))).clear();
            // ADD CUSTOM GOAL
            goalSelector.a(0, new PathfinderGoalFloat(this));
            goalSelector.a(10, new PathfinderGoalEntityDirection(this, bf));

            l.getChunk().load(false);
            persistent = true;
            b(true);
            uniqueID = ms.getIdentificationId();
            world.addEntity(this);
        }

        @Override
        public void setAction(final PlayerAction action) {
            this.action = action;
        }

        @Override
        public PlayerAction getAction() {
            return action;
        }

        @Override
        public void executeAction(final Player p) {
            action.call(p);
            ms.getAnalytics().getAnalytics().addNPCUse(hname);
        }

        @Override
        public MuxHolograms.Hologram getHologram() {
            return holo;
        }

        @Override
        public void setHologram(final String name, final MuxHolograms.Hologram h) {
            this.hname = ChatColor.stripColor(name);
            if (h != null) this.holo = h;
        }

        @Override
        public Location getHologramLocation() {
            return new Location(world.getWorld(), locX, locY, locZ).subtract(0, (ageLocked ? 0.75 : 0) + 0.32, 0);
        }

        @Override
        public Location getLocation() {
            return loc;
        }

        public MuxVillager setBaby() {
            this.setAge(-24000);
            ageLocked = true;
            return this;
        }

        /**
         * OVERRIDE
         **/
        @Override
        public boolean damageEntity(final net.minecraft.server.v1_8_R3.DamageSource ds, final float f) {
            if (ds.getEntity() instanceof EntityHuman)
                handleNPC(this, ((EntityPlayer) ds.getEntity()).getBukkitEntity());
            return false;
        }

        @Override
        public void g(final double a, final double b, final double c) {
        }

        @Override
        public Entity getEntity() {
            return this;
        }
    }

    final class MuxPigZombie extends EntityPigZombie implements MuxNPC {
        private PlayerAction action = null;
        private MuxHolograms.Hologram holo = null;
        private String hname;
        private final Location loc;

        public MuxPigZombie(final ItemStack hand, final Location l, final BlockFace bf) {
            super(((CraftWorld) l.getWorld()).getHandle());
            this.setEquipment(0, CraftItemStack.asNMSCopy(hand));
            this.loc = l;
            this.setPosition(l.getX(), l.getY(), l.getZ());

            // REMOVE GOALS
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "b", goalSelector))).clear();
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "c", goalSelector))).clear();
            // ADD CUSTOM GOAL
            goalSelector.a(0, new PathfinderGoalFloat(this));
            goalSelector.a(10, new PathfinderGoalEntityDirection(this, bf));

            l.getChunk().load(false);
            persistent = true;
            b(true);
            uniqueID = ms.getIdentificationId();
            world.addEntity(this);
        }

        @Override
        public void setAction(final PlayerAction action) {
            this.action = action;
        }

        @Override
        public PlayerAction getAction() {
            return action;
        }

        @Override
        public void executeAction(final Player p) {
            action.call(p);
            ms.getAnalytics().getAnalytics().addNPCUse(hname);
        }

        @Override
        public MuxHolograms.Hologram getHologram() {
            return holo;
        }

        @Override
        public void setHologram(final String name, final MuxHolograms.Hologram h) {
            this.hname = ChatColor.stripColor(name);
            if (h != null) this.holo = h;
        }

        @Override
        public Location getHologramLocation() {
            return new Location(world.getWorld(), locX, locY, locZ).subtract(0, 0.32, 0);
        }

        @Override
        public Location getLocation() {
            return loc;
        }

        /**
         * OVERRIDE
         **/
        @Override
        public boolean damageEntity(final net.minecraft.server.v1_8_R3.DamageSource ds, final float f) {
            if (ds.getEntity() instanceof EntityHuman)
                handleNPC(this, ((EntityPlayer) ds.getEntity()).getBukkitEntity());
            return false;
        }

        @Override
        public void g(final double a, final double b, final double c) {
        }

        @Override
        public Entity getEntity() {
            return this;
        }
    }

    final class MuxSkeleton extends EntitySkeleton implements MuxNPC {
        private PlayerAction action = null;
        private MuxHolograms.Hologram holo = null;
        private String hname;
        private final Location loc;

        public MuxSkeleton(final SkeletonType type, final ItemStack hand, final Location l, final BlockFace bf) {
            super(((CraftWorld) l.getWorld()).getHandle());
            this.setEquipment(0, CraftItemStack.asNMSCopy(hand));
            this.setSkeletonType(type != null ? 1 : 0);
            this.loc = l;
            this.setPosition(l.getX(), l.getY(), l.getZ());

            // REMOVE GOALS
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "b", goalSelector))).clear();
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "c", goalSelector))).clear();
            // ADD CUSTOM GOAL
            goalSelector.a(0, new PathfinderGoalFloat(this));
            goalSelector.a(10, new PathfinderGoalEntityDirection(this, bf));

            l.getChunk().load(false);
            persistent = true;
            b(true);
            uniqueID = ms.getIdentificationId();
            world.addEntity(this);
        }

        @Override
        public void setAction(final PlayerAction action) {
            this.action = action;
        }

        @Override
        public PlayerAction getAction() {
            return action;
        }

        @Override
        public void executeAction(final Player p) {
            action.call(p);
            ms.getAnalytics().getAnalytics().addNPCUse(hname);
        }

        @Override
        public MuxHolograms.Hologram getHologram() {
            return holo;
        }

        @Override
        public void setHologram(final String name, final MuxHolograms.Hologram h) {
            this.hname = ChatColor.stripColor(name);
            if (h != null) this.holo = h;
        }

        @Override
        public Location getHologramLocation() {
            return new Location(world.getWorld(), locX, locY, locZ).subtract(0, getSkeletonType() == 1 ? -0.1 : 0.32, 0);
        }

        @Override
        public Location getLocation() {
            return loc;
        }

        /**
         * OVERRIDE
         **/
        @Override
        public boolean damageEntity(final net.minecraft.server.v1_8_R3.DamageSource ds, final float f) {
            if (ds.getEntity() instanceof EntityHuman)
                handleNPC(this, ((EntityPlayer) ds.getEntity()).getBukkitEntity());
            return false;
        }

        @Override
        public void g(final double a, final double b, final double c) {
        }

        @Override
        public Entity getEntity() {
            return this;
        }
    }

    final class MuxCreeper extends EntityCreeper implements MuxNPC {
        private PlayerAction action = null;
        private MuxHolograms.Hologram holo = null;
        private String hname;
        private final Location loc;

        public MuxCreeper(final boolean lightning, final Location l, final BlockFace bf) {
            super(((CraftWorld) l.getWorld()).getHandle());
            this.setPowered(lightning);
            this.loc = l;
            this.setPosition(l.getX(), l.getY(), l.getZ());

            // REMOVE GOALS
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "b", goalSelector))).clear();
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "c", goalSelector))).clear();
            // ADD CUSTOM GOAL
            goalSelector.a(0, new PathfinderGoalFloat(this));
            goalSelector.a(10, new PathfinderGoalEntityDirection(this, bf));

            l.getChunk().load(false);
            persistent = true;
            b(true);
            uniqueID = ms.getIdentificationId();
            world.addEntity(this);
        }

        @Override
        public void setAction(final PlayerAction action) {
            this.action = action;
        }

        @Override
        public PlayerAction getAction() {
            return action;
        }

        @Override
        public void executeAction(final Player p) {
            action.call(p);
            ms.getAnalytics().getAnalytics().addNPCUse(hname);
        }

        @Override
        public MuxHolograms.Hologram getHologram() {
            return holo;
        }

        @Override
        public void setHologram(final String name, final MuxHolograms.Hologram h) {
            this.hname = ChatColor.stripColor(name);
            if (h != null) this.holo = h;
        }

        @Override
        public Location getHologramLocation() {
            return new Location(world.getWorld(), locX, locY, locZ).subtract(0, 0.52, 0);
        }

        @Override
        public Location getLocation() {
            return loc;
        }

        /**
         * OVERRIDE
         **/
        @Override
        public boolean damageEntity(final net.minecraft.server.v1_8_R3.DamageSource ds, final float f) {
            if (ds.getEntity() instanceof EntityHuman)
                handleNPC(this, ((EntityPlayer) ds.getEntity()).getBukkitEntity());
            return false;
        }

        @Override
        public void g(final double a, final double b, final double c) {
        }

        @Override
        public Entity getEntity() {
            return this;
        }
    }

    final class MuxWitch extends EntityWitch implements MuxNPC {
        private PlayerAction action = null;
        private MuxHolograms.Hologram holo = null;
        private String hname;
        private final Location loc;

        public MuxWitch(final int ignore, final Location l, final BlockFace bf) {
            super(((CraftWorld) l.getWorld()).getHandle());
            this.loc = l;
            this.setPosition(l.getX(), l.getY(), l.getZ());

            // REMOVE GOALS
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "b", goalSelector))).clear();
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "c", goalSelector))).clear();
            // ADD CUSTOM GOAL
            goalSelector.a(0, new PathfinderGoalFloat(this));
            goalSelector.a(10, new PathfinderGoalEntityDirection(this, bf));

            l.getChunk().load(false);
            persistent = true;
            b(true);
            uniqueID = ms.getIdentificationId();
            world.addEntity(this);
        }

        @Override
        public void setAction(final PlayerAction action) {
            this.action = action;
        }

        @Override
        public PlayerAction getAction() {
            return action;
        }

        @Override
        public void executeAction(final Player p) {
            action.call(p);
            ms.getAnalytics().getAnalytics().addNPCUse(hname);
        }

        @Override
        public MuxHolograms.Hologram getHologram() {
            return holo;
        }

        @Override
        public void setHologram(final String name, final MuxHolograms.Hologram h) {
            this.hname = ChatColor.stripColor(name);
            if (h != null) this.holo = h;
        }

        @Override
        public Location getHologramLocation() {
            return new Location(world.getWorld(), locX, locY, locZ).subtract(0, -0.1, 0);
        }

        @Override
        public Location getLocation() {
            return loc;
        }

        /**
         * OVERRIDE
         **/
        @Override
        public boolean damageEntity(final net.minecraft.server.v1_8_R3.DamageSource ds, final float f) {
            if (ds.getEntity() instanceof EntityHuman)
                handleNPC(this, ((EntityPlayer) ds.getEntity()).getBukkitEntity());
            return false;
        }

        @Override
        public void g(final double a, final double b, final double c) {
        }

        @Override
        public Entity getEntity() {
            return this;
        }
    }

    final class MuxSheep extends EntitySheep implements MuxNPC {
        private PlayerAction action = null;
        private MuxHolograms.Hologram holo = null;
        private String hname;
        private final Location loc;

        public MuxSheep(final EnumColor color, final Location l, final BlockFace bf) {
            super(((CraftWorld) l.getWorld()).getHandle());
            this.loc = l;
            this.setPosition(l.getX(), l.getY(), l.getZ());
            this.setColor(color);

            // REMOVE GOALS
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "b", goalSelector))).clear();
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "c", goalSelector))).clear();
            // ADD CUSTOM GOAL
            goalSelector.a(0, new PathfinderGoalFloat(this));
            goalSelector.a(10, new PathfinderGoalEntityDirection(this, bf));

            l.getChunk().load(false);
            persistent = true;
            b(true);
            uniqueID = ms.getIdentificationId();
            world.addEntity(this);
        }

        @Override
        public void setAction(final PlayerAction action) {
            this.action = action;
        }

        @Override
        public PlayerAction getAction() {
            return action;
        }

        @Override
        public void executeAction(final Player p) {
            action.call(p);
            ms.getAnalytics().getAnalytics().addNPCUse(hname);
        }

        @Override
        public MuxHolograms.Hologram getHologram() {
            return holo;
        }

        @Override
        public void setHologram(final String name, final MuxHolograms.Hologram h) {
            this.hname = ChatColor.stripColor(name);
            if (h != null) this.holo = h;
        }

        @Override
        public Location getHologramLocation() {
            return new Location(world.getWorld(), locX, locY, locZ).subtract(0, 0.8, 0);
        }

        @Override
        public Location getLocation() {
            return loc;
        }

        /**
         * OVERRIDE
         **/
        @Override
        public boolean damageEntity(final net.minecraft.server.v1_8_R3.DamageSource ds, final float f) {
            if (ds.getEntity() instanceof EntityHuman)
                handleNPC(this, ((EntityPlayer) ds.getEntity()).getBukkitEntity());
            return false;
        }

        @Override
        public void g(final double a, final double b, final double c) {
        }

        @Override
        public Entity getEntity() {
            return this;
        }
    }
    final class MuxWolf extends EntityWolf implements MuxNPC {
        private PlayerAction action = null;
        private MuxHolograms.Hologram holo = null;
        private String hname;
        private final Location loc;

        public MuxWolf(final Location l, final BlockFace bf) {
            super(((CraftWorld) l.getWorld()).getHandle());
            this.loc = l;
            this.setPosition(l.getX(), l.getY(), l.getZ());

            // REMOVE GOALS
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "b", goalSelector))).clear();
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "c", goalSelector))).clear();
            // ADD CUSTOM GOAL
            goalSelector.a(0, new PathfinderGoalFloat(this));
            goalSelector.a(10, new PathfinderGoalEntityDirection(this, bf));

            l.getChunk().load(false);
            persistent = true;
            setTamed(true);
            b(true);
            uniqueID = ms.getIdentificationId();
            world.addEntity(this);
        }

        @Override
        public void setAction(final PlayerAction action) {
            this.action = action;
        }

        @Override
        public PlayerAction getAction() {
            return action;
        }

        @Override
        public void executeAction(final Player p) {
            action.call(p);
            ms.getAnalytics().getAnalytics().addNPCUse(hname);
        }

        @Override
        public MuxHolograms.Hologram getHologram() {
            return holo;
        }

        @Override
        public void setHologram(final String name, final MuxHolograms.Hologram h) {
            this.hname = ChatColor.stripColor(name);
            if (h != null) this.holo = h;
        }

        @Override
        public Location getHologramLocation() {
            return new Location(world.getWorld(), locX, locY, locZ).subtract(0, 1.1D, 0);
        }

        @Override
        public Location getLocation() {
            return loc;
        }

        /**
         * OVERRIDE
         **/
        @Override
        public boolean damageEntity(final net.minecraft.server.v1_8_R3.DamageSource ds, final float f) {
            if (ds.getEntity() instanceof EntityHuman)
                handleNPC(this, ((EntityPlayer) ds.getEntity()).getBukkitEntity());
            return false;
        }

        @Override
        public void g(final double a, final double b, final double c) {
        }
        @Override
        public Entity getEntity() {
            return this;
        }
    }
    final class MuxMooshroom extends EntityMushroomCow implements MuxNPC {
        private PlayerAction action = null;
        private MuxHolograms.Hologram holo = null;
        private String hname;
        private final Location loc;

        public MuxMooshroom(final Location l, final BlockFace bf) {
            super(((CraftWorld) l.getWorld()).getHandle());
            this.loc = l;
            this.setPosition(l.getX(), l.getY(), l.getZ());

            // REMOVE GOALS
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "b", goalSelector))).clear();
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "c", goalSelector))).clear();
            // ADD CUSTOM GOAL
            goalSelector.a(0, new PathfinderGoalFloat(this));
            goalSelector.a(10, new PathfinderGoalEntityDirection(this, bf));

            l.getChunk().load(false);
            persistent = true;
            b(true);
            uniqueID = ms.getIdentificationId();
            world.addEntity(this);
        }

        @Override
        public void setAction(final PlayerAction action) {
            this.action = action;
        }

        @Override
        public PlayerAction getAction() {
            return action;
        }

        @Override
        public void executeAction(final Player p) {
            action.call(p);
            ms.getAnalytics().getAnalytics().addNPCUse(hname);
        }

        @Override
        public MuxHolograms.Hologram getHologram() {
            return holo;
        }

        @Override
        public void setHologram(final String name, final MuxHolograms.Hologram h) {
            this.hname = ChatColor.stripColor(name);
            if (h != null) this.holo = h;
        }

        @Override
        public Location getHologramLocation() {
            return new Location(world.getWorld(), locX, locY, locZ).subtract(0, 0.6, 0);
        }

        @Override
        public Location getLocation() {
            return loc;
        }

        /**
         * OVERRIDE
         **/
        @Override
        public boolean damageEntity(final net.minecraft.server.v1_8_R3.DamageSource ds, final float f) {
            if (ds.getEntity() instanceof EntityHuman)
                handleNPC(this, ((EntityPlayer) ds.getEntity()).getBukkitEntity());
            return false;
        }

        @Override
        public void g(final double a, final double b, final double c) {
        }

        @Override
        public Entity getEntity() {
            return this;
        }
    }

    final class MuxIronGolem extends EntityIronGolem implements MuxNPC {
        private PlayerAction action = null;
        private MuxHolograms.Hologram holo = null;
        private String hname;
        private final Location loc;

        public MuxIronGolem(final Location l, final BlockFace bf) {
            super(((CraftWorld) l.getWorld()).getHandle());
            this.loc = l;
            this.setPosition(l.getX(), l.getY(), l.getZ());

            // REMOVE GOALS
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "b", goalSelector))).clear();
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "c", goalSelector))).clear();
            // ADD CUSTOM GOAL
            goalSelector.a(0, new PathfinderGoalFloat(this));
            goalSelector.a(10, new PathfinderGoalEntityDirection(this, bf));

            l.getChunk().load(false);
            persistent = true;
            b(true);
            uniqueID = ms.getIdentificationId();
            world.addEntity(this);
        }

        @Override
        public void setAction(final PlayerAction action) {
            this.action = action;
        }

        @Override
        public PlayerAction getAction() {
            return action;
        }

        @Override
        public void executeAction(final Player p) {
            action.call(p);
            ms.getAnalytics().getAnalytics().addNPCUse(hname);
        }

        @Override
        public MuxHolograms.Hologram getHologram() {
            return holo;
        }

        @Override
        public void setHologram(final String name, final MuxHolograms.Hologram h) {
            this.hname = ChatColor.stripColor(name);
            if (h != null) this.holo = h;
        }

        @Override
        public Location getHologramLocation() {
            return new Location(world.getWorld(), locX, locY, locZ).add(0, 0.35, 0);
        }

        @Override
        public Location getLocation() {
            return loc;
        }

        /**
         * OVERRIDE
         **/
        @Override
        public boolean damageEntity(final net.minecraft.server.v1_8_R3.DamageSource ds, final float f) {
            if (ds.getEntity() instanceof EntityHuman)
                handleNPC(this, ((EntityPlayer) ds.getEntity()).getBukkitEntity());
            return false;
        }

        @Override
        public void g(final double a, final double b, final double c) {
        }

        @Override
        public Entity getEntity() {
            return this;
        }
    }

    public interface MuxNPC {
        Entity getEntity();

        void executeAction(Player p);

        void setHologram(final String name, final MuxHolograms.Hologram holo);

        MuxHolograms.Hologram getHologram();

        Location getLocation();

        Location getHologramLocation();

        PlayerAction getAction();

        void setAction(final PlayerAction action);
    }
}