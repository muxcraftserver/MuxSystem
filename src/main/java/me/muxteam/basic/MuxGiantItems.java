package me.muxteam.basic;

import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityGiantZombie;
import net.minecraft.server.v1_8_R3.MobEffect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Giant;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class MuxGiantItems {
	private MuxSystem ms;
	private final List<MuxGiant> giantitems = new ArrayList<>();

	public MuxGiantItems(final MuxSystem ms) {
		this.ms = ms;
		removeOld();
	}
	public void close() {
		this.ms = null;
	}
	public void respawn() {
		giantitems.forEach(fv -> {
			final EntityGiantZombie entity = fv.entity;
			if (entity.dead) {
				entity.dead = false;
				entity.addEffect(new MobEffect(14, 9999999, 0, true, false));
				entity.world.addEntity(entity, SpawnReason.CUSTOM);
			} else if (entity.hasEffect(14) == false) {
				entity.addEffect(new MobEffect(14, 9999999, 0, true, false));
			}
		});
	}
	public void removeSpawnGiant() {
		giantitems.get(0).entity.dead = true;
	}
	public void addGiant(final Location l, final ItemStack item) {
		giantitems.add(new MuxGiant(l, item));
	}
	public void removeOld() {
		for (final World x : ms.getServer().getWorlds()) {
			x.getEntitiesByClass(Giant.class).forEach(org.bukkit.entity.Entity::remove);
		}
	}
	final class MuxGiant {
		public final EntityGiantZombie entity;

		public MuxGiant(final Location l, final ItemStack item) {
			entity = new EntityGiantZombie(((CraftWorld) l.getWorld()).getHandle());

			entity.setPositionRotation(l.getX() + 2D, l.getY(), l.getZ(), l.getYaw(), l.getPitch());
			entity.setEquipment(0, CraftItemStack.asNMSCopy(item));

			l.getChunk().load(false);
			NMSReflection.setObject(Entity.class, "invulnerable", entity, true);
			NMSReflection.setObject(Entity.class, "uniqueID", entity, ms.getIdentificationId());
			entity.addEffect(new MobEffect(14, 9999999, 0, true, false));
			entity.canPickUpLoot = false;
			entity.persistent = true;
			entity.world.addEntity(entity, SpawnReason.CUSTOM);
			ms.noAI(entity.getBukkitEntity());
			entity.b(true);
		}
	}
}