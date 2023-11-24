package me.muxteam.extras;

import me.muxteam.basic.NMSReflection;
import me.muxteam.basic.PathfinderGoalPlayerFollow;
import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityAgeable;
import net.minecraft.server.v1_8_R3.EntityBat;
import net.minecraft.server.v1_8_R3.EntityHuman;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.EntityVillager;
import net.minecraft.server.v1_8_R3.EntityWither;
import net.minecraft.server.v1_8_R3.EntityZombie;
import net.minecraft.server.v1_8_R3.GenericAttributes;
import net.minecraft.server.v1_8_R3.MobEffect;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.PathfinderGoal;
import net.minecraft.server.v1_8_R3.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_8_R3.PathfinderGoalSelector;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftAgeable;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftFallingSand;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.util.UnsafeList;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MuxPets implements Listener {
    private MuxSystem ms;
    private final ConcurrentHashMap<UUID, Pet> pets = new ConcurrentHashMap<>();

    public MuxPets(final MuxSystem ms) {
        this.ms = ms;
        ms.getServer().getPluginManager().registerEvents(this, ms);
    }

    public boolean isPet(final LivingEntity entity) {
        for (final Pet p : pets.values()) {
            if (p.entity != null && p.entity.hashCode() == ((CraftLivingEntity) entity).getHandle().hashCode())
                return true;
        }
        return false;
    }

    public void close() {
        for (final Pet pet : pets.values()) {
            pet.remove();
            if (pet.getOwner() != null) {
                ms.showItemBar(pet.getOwner(), ms.getLang("extras.petremoved"));
            }
        }
        this.ms = null;
    }

    public void handleQuit(final UUID uuid) {
        removePet(uuid);
    }

    public void deactivatePet(final Player p) {
        if (removePet(p.getUniqueId())) {
            ms.showItemBar(p, ms.getLang("extras.petremoved"));
        }
    }

    public void deactivatePets() {
        for (final UUID uuid : pets.keySet()) {
            removePet(uuid);
        }
    }

    public void updatePets() {
        for (final Player p : ms.getServer().getOnlinePlayers()) {
            final Location l = p.getLocation();
            if (p.getGameMode() == GameMode.SPECTATOR || ms.in1vs1(p) || p.isDead() || ms.inDuel(p) || ms.isVanish(p) || ms.inBattle(p.getName(), l) || ms.inWar(p) || ms.inGame(p) || ms.inEvent(p) || ms.inCasino(p) || (p.getWorld().equals(ms.getWarpsWorld()) && ms.getShopRegion() != null && ms.getShopRegion().contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())))
                continue;
            final MuxExtraUser extraUser = ms.getExtras().getExtraUser(p.getUniqueId());
            if (pets.containsKey(p.getUniqueId()) == false && extraUser.getPets().getActive() != null) {
                setPet(p, extraUser.getPets().getActive(), false);
            }
        }
    }

    public void setWitherNames(final String name) {
        pets.values().stream().filter(p -> p.getType() == EntityType.WITHER).forEach(pet -> pet.entity.setCustomName(name));
    }

    public void handleTeleport(final Player p) {
        final EntityType et = getPetType(p.getUniqueId());
        if (et != null) {
            setPet(p, et, false);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (ms.inEvent(p) || p.getGameMode() == GameMode.SPECTATOR)
                        return;
                    setPet(p, et, true);
                }
            }.runTaskLater(ms, 20L);
        }
    }

    public boolean setPet(final Player p, final EntityType type, boolean click) {
        final UUID uuid = p.getUniqueId();
        if (pets.containsKey(uuid)) {
            final Pet tmp = pets.get(uuid);
            tmp.remove();
            pets.remove(uuid);
            if (tmp.getType() == type) {
                return true;
            }
        }
        if (ms.getAdmin().PETS.isActive() == false) {
            if (click) {
                ms.showItemBar(p, ms.getLang("extras.petstemporary"));
                p.closeInventory();
            }
            return false;
        }
        final Pet pet = new Pet(type, p, true);
        if (pet.spawn() == false) {
            if (click)
                ms.showItemBar(p, ms.getLang("extras.petnotspawned"));
            return false;
        }
        pets.put(uuid, pet);
        return true;
    }

    public boolean removePet(final UUID uuid) {
        final Pet p = pets.remove(uuid);
        if (p != null) {
            p.remove();
            return true;
        }
        return false;
    }

    public EntityType getPetType(final UUID uuid) {
        final Pet pet = pets.get(uuid);
        if (pet != null) {
            return pet.getType();
        }
        return null;
    }

    public String getPetName(final EntityType et) {
        switch (et) {
            case WITHER:
                return "Wither";
            case PIG:
                return ms.getLang("pig");
            case SHEEP:
                return ms.getLang("sheep");
            case COW:
                return ms.getLang("cow");
            case CHICKEN:
                return ms.getLang("chicken");
            case WOLF:
                return ms.getLang("wolf");
            case MUSHROOM_COW:
                return ms.getLang("mushroomcow");
            case OCELOT:
                return ms.getLang("ocelot");
            case RABBIT:
                return ms.getLang("rabbit");
            case PIG_ZOMBIE:
                return "Pigman";
            case ZOMBIE:
                return "Mini Me";
            case VILLAGER:
                return "Elf";
            case BAT:
                return "Fledermaus";
            case SILVERFISH:
                return "Kuchen";
            default:
                return "Animal";
        }
    }

    // BLOCK PET
    @EventHandler
    public void onEntityChangeBlock(final EntityChangeBlockEvent e) {
        if (e.getEntity() instanceof FallingBlock) {
            final FallingBlock fb = (FallingBlock) e.getEntity();
            if (fb.getMaterial() != Material.CAKE && fb.getMaterial() != Material.CAKE_BLOCK) return;
            e.setCancelled(true);
            ((CraftFallingSand) fb).getHandle().dead = false;
        }
    }

    class Pet {
        private EntityType type;
        private EntityLiving entity;
        private Player owner;
        private final boolean baby;

        public Pet(final EntityType type, final Player owner, final boolean baby) {
            this.type = type;
            this.owner = owner;
            this.baby = baby;
        }

        public boolean spawn() {
            try {
                final Entity en = owner.getWorld().spawnEntity(this.owner.getLocation(), this.type);
                entity = (EntityLiving) ((CraftEntity) en).getHandle();

                setPetProperties((EntityInsentient) entity);

                if (entity.isAlive() == false) return false;

                if (type == EntityType.WITHER) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (entity.isAlive()) {
                                entity.getDataWatcher().watch(20, 600);
                            } else {
                                this.cancel();
                            }
                        }
                    }.runTaskTimer(ms, 0L, 1L);
                    entity.b(true);
                    entity.setCustomNameVisible(false);
                    return true;
                } else if (type == EntityType.ZOMBIE) {
                    entity.setEquipment(4, CraftItemStack.asNMSCopy(ms.getHead(owner.getName())));
                    entity.setEquipment(3, CraftItemStack.asNMSCopy(new ItemStack(Material.LEATHER_CHESTPLATE)));
                    entity.setEquipment(2, CraftItemStack.asNMSCopy(new ItemStack(Material.LEATHER_LEGGINGS)));
                    entity.setEquipment(1, CraftItemStack.asNMSCopy(new ItemStack(Material.LEATHER_BOOTS)));
                    ((EntityZombie) ((CraftEntity) en).getHandle()).setBaby(true);
                    entity.b(true);
                } else if (type == EntityType.BAT) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            final Location l = owner.getLocation();
                            NMSReflection.setObject(EntityBat.class, "a", entity, new BlockPosition(l.getBlockX(), l.getBlockY() + 2, l.getBlockZ()));
                        }
                    }.runTaskTimer(ms, 0L, 20L);
                } else if (type == EntityType.SILVERFISH) {
                    final FallingBlock cake = en.getWorld().spawnFallingBlock(en.getLocation().add(0, 3, 0), Material.CAKE_BLOCK, (byte) 0);
                    cake.setCustomName(entity.getCustomName());
                    cake.setCustomNameVisible(true);
                    final net.minecraft.server.v1_8_R3.Entity cakeEntity = ((CraftEntity) cake).getHandle();
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (entity.isAlive()) {
                                NBTTagCompound tag = cakeEntity.getNBTTag();
                                if (tag == null) {
                                    tag = new NBTTagCompound();
                                }
                                cakeEntity.c(tag);
                                tag.setInt("Time", -1);
                                cakeEntity.f(tag);
                            } else {
                                this.cancel();
                            }
                        }
                    }.runTaskTimer(ms, 0L, 100L);
                    en.setPassenger(cake);
                    ((LivingEntity) en).addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 9999999, 0));
                    entity.b(true);
                } else if (type == EntityType.PIG_ZOMBIE) {
                    ((EntityZombie) ((CraftEntity) en).getHandle()).setBaby(true);
                }

                if (baby && en instanceof CraftAgeable) {
                    ((EntityAgeable) ((CraftEntity) en).getHandle()).setAge(-24000);
                    ((EntityAgeable) ((CraftEntity) en).getHandle()).ageLocked = true;
                }
                return true;
            } catch (final Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        public void setPetProperties(final EntityInsentient entity) {
            final PathfinderGoalSelector goal = (PathfinderGoalSelector) NMSReflection.getObject(EntityInsentient.class, "goalSelector", entity),
                    target = (PathfinderGoalSelector) NMSReflection.getObject(EntityInsentient.class, "targetSelector", entity);

            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "b", goal))).clear();
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "c", goal))).clear();
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "b", target))).clear();
            ((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "c", target))).clear();

            entity.getAttributeInstance(GenericAttributes.FOLLOW_RANGE).setValue(70.0D);

            if (goal != null) {
                goal.a(0, new PathfinderGoalLookAtPlayer(entity, EntityHuman.class, 6.0F));
                goal.a(1, new PathfinderGoalPlayerFollow(entity, this.owner, entity instanceof EntityWither || entity instanceof EntityVillager ? 0.6D : 1.4D));
            }
            if (type != EntityType.WITHER) {
                final String name = owner.getName(), lastchar = name.substring(name.length() - 1).toLowerCase();
                final boolean x = lastchar.equals("s") || lastchar.equals("x") || lastchar.equals("z");
                entity.setCustomName("§a" + name + "'" + (x ? "" : "s") + " §7" + getPetName(type));
                entity.setCustomNameVisible(true);
            }

            entity.addEffect(new MobEffect(8, 9999999, 0, true, false));

            NMSReflection.setObject(net.minecraft.server.v1_8_R3.Entity.class, "uniqueID", entity, ms.getIdentificationId());
            NMSReflection.setObject(net.minecraft.server.v1_8_R3.Entity.class, "invulnerable", entity, true);
        }

        public void remove() {
            if (entity != null) {
                if (entity.passenger != null) entity.passenger.die();
                entity.die();
            }
        }

        public void setType(final EntityType type) {
            this.type = type;
        }

        public EntityType getType() {
            return this.type;
        }

        public void setOwner(final Player owner) {
            this.owner = owner;
        }

        public Player getOwner() {
            return this.owner;
        }
    }

    public final class PetStore {
        private EntityType active;
        private final Set<EntityType> owned;

        public PetStore(final Set<EntityType> owned, final EntityType active) {
            this.owned = owned;
            this.active = active;
        }

        public EntityType getActive() {
            return active;
        }

        public void setActive(final EntityType active) {
            this.active = active;
        }

        public Set<EntityType> getOwned() {
            return owned;
        }

        public Integer getSize() {
            return this.owned.size();
        }
    }
}