package me.muxteam.holidays;

import com.sk89q.worldguard.bukkit.WGBukkit;
import me.muxteam.basic.NMSReflection;
import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.EntitySkeleton;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MuxHalloween implements MuxHoliday, Listener {
	private MuxSystem ms;
	private BlockRestore blockrestore;
	private final Random r = new Random();
	private final CopyOnWriteArrayList<LivingEntity> mobs = new CopyOnWriteArrayList<>();

	public MuxHalloween(final MuxSystem ms, boolean fake) {
		if (fake) return;
		this.ms = ms;
		this.blockrestore = new BlockRestore(ms);
		ms.getGameSpawn().getWorld().setTime(18000L);
		new BukkitRunnable() {
			@Override public void run() {
				checkHalloween();
			}
		}.runTaskTimer(ms, 10L, 5L);
		final Location spawn = ms.getGameSpawn();
		new BukkitRunnable() {
			@Override public void run() {
				short living = 0;
				for (final LivingEntity e : mobs)
					if (e.isDead() || e.isValid() == false) {
						mobs.remove(e);
					} else if (e.getWorld() != spawn.getWorld() || e.getLocation().distance(spawn) >= 45.0D) {
						mobs.remove(e);
						e.remove();
						e.getWorld().playEffect(e.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
					} else {
						living = (short) (living + 1);
					}
				for (short i = (short) (15 - living); i > 0; i = (short) (i - 1)) {
					spawnMob(spawn.clone().add((15 + r.nextInt(10)) * (r.nextBoolean() ? -1D : 1D), -1.0D, (15 + r.nextInt(10)) * (r.nextBoolean() ? -1D : 1D)));
				}
				if (r.nextInt(36) == 0) {
					spawn.getWorld().strikeLightning(spawn.clone().add(60 * (r.nextBoolean() ? -1D : 1D), 0, 60 * (r.nextBoolean() ? -1D : 1D)));
				}
			}
		}.runTaskTimer(ms, 20L, 100L);
		ms.getServer().getPluginManager().registerEvents(this, ms);
	}
	@Override
	public void close() {
		for (final LivingEntity e : this.mobs) {
			e.remove();
		}
		this.mobs.clear();
		this.ms = null;
	}
	@EventHandler
	public void onDamage(final EntityDamageEvent e) {
		if (e.getEntity() instanceof Player && e instanceof EntityDamageByEntityEvent) {
			final EntityDamageByEntityEvent dmge = (EntityDamageByEntityEvent) e;
			final Entity edmg = dmge.getDamager();
			if (edmg instanceof Skeleton && edmg.getCustomName() != null && edmg.getCustomName().equals("§ePumpkin Minion")) {
				e.setCancelled(true);
			}
		}
	}
	public void checkHalloween() {
		for (final Player player : ms.getServer().getOnlinePlayers()) {
			final Location l = player.getLocation();
			if (WGBukkit.getRegionManager(l.getWorld()).getApplicableRegions(l).size() > 0) {
				for (final Block block : getInRadius(l, 3.0D).keySet()) {
					if (block.getType() == Material.PUMPKIN) {
						blockrestore.add(block, 91, block.getData(), 2000);
					}
				}
			}
		}
		blockrestore.onExpireBlocks();
		for (final LivingEntity ent : this.mobs) {
			if (ent instanceof Creature) {
				final Creature skel = (Creature) ent;
				if (skel.getTarget() instanceof Player && offset(skel, skel.getTarget()) < 6.0D) {
					skel.getEquipment().setHelmet(new ItemStack(Material.JACK_O_LANTERN));
				} else {
					skel.getEquipment().setHelmet(new ItemStack(Material.PUMPKIN));
				}
			}
		}
	}
	public void spawnMob(final Location l) {
		final Skeleton s = l.getWorld().spawn(l, Skeleton.class);
		s.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 9999999, 0, false, false));
		s.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 9999999, 1, false, false));
		s.setMaxHealth(2048.0D);
		s.setHealth(2048.0D);
		s.setCanPickupItems(false);
		s.setRemoveWhenFarAway(false);
		s.getEquipment().setArmorContents(null);
		s.getEquipment().setItemInHand(null);
		s.setCustomName("§ePumpkin Minion");
		final net.minecraft.server.v1_8_R3.Entity nms = ((CraftEntity) s).getHandle();
		((EntitySkeleton)nms).persistent = true;
		NMSReflection.setObject(net.minecraft.server.v1_8_R3.Entity.class, "invulnerable", nms, true);
		if (Math.random() > 0.5D) {
			s.setSkeletonType(Skeleton.SkeletonType.WITHER);
		}
		s.getEquipment().setHelmet(new ItemStack(Material.PUMPKIN));
		s.getEquipment().setItemInHand(null);
		this.mobs.add(s);
	}
	public Map<Block, Double> getInRadius(final Location loc, final double radius) {
		final Map<Block, Double> blockList = new HashMap<>();
		final int iR = (int) radius + 1;

		for (int x = -iR ; x <= iR ; x++) {
			for (int z = -iR ; z <= iR ; z++) {
				for (int y = -iR ; y <= iR ; y++) {
					if (Math.abs(y) > 256) continue;

					final Block curBlock = loc.getWorld().getBlockAt((int) (loc.getX() + x), (int) (loc.getY() + y), (int) (loc.getZ() + z));
					final double offset = offset(loc, curBlock.getLocation().add(0.5, 0.5, 0.5));

					if (offset <= radius) blockList.put(curBlock, 1 - offset / radius);
				}
			}
		}
		return blockList;
	}
	public double offset(final Location a, final Location b) {
		final Vector v1 = a.toVector(), v2 = b.toVector();
		return v1.subtract(v2).length();
	}
	public double offset(final Entity a, final Entity b) {
		final Vector v1 = a.getLocation().toVector(), v2 = b.getLocation().toVector();
		return v1.subtract(v2).length();
	}
	class BlockRestore implements Listener {
		private final HashMap<Block, BlockRestoreData> blocks = new HashMap<>();

		public BlockRestore(final MuxSystem ms) {
			ms.getServer().getPluginManager().registerEvents(this, ms);
		}
		@EventHandler(priority = EventPriority.LOW)
		public void onBlockBreak(final BlockBreakEvent event) {
			if (contains(event.getBlock())) {
				event.setCancelled(true);
			}
		}
		@EventHandler(priority = EventPriority.LOW)
		public void onBlockPlace(final BlockPlaceEvent event) {
			if (contains(event.getBlockPlaced())) {
				event.setCancelled(true);
			}
		}
		@EventHandler(priority = EventPriority.LOW)
		public void onPiston(final BlockPistonExtendEvent event) {
			if (event.isCancelled()) {
				return;
			}
			Block push = event.getBlock();
			for (int i = 0; i < 13; i++) {
				push = push.getRelative(event.getDirection());
				if (push.getType() == Material.AIR) {
					return;
				}
				if (contains(push)) {
					push.getWorld().playEffect(push.getLocation(), Effect.STEP_SOUND, push.getType());
					event.setCancelled(true);
					return;
				}
			}
		}
		public void onExpireBlocks() {
			final List<Block> toRemove = new ArrayList<>();
			for (final BlockRestoreData cur : this.blocks.values()) {
				if (cur.expire()) {
					toRemove.add(cur.block);
				}
			}
			for (final Block cur : toRemove) {
				this.blocks.remove(cur);
			}
		}
		public void add(final Block block, final int toID, final byte toData, final long expireTime) {
			if (contains(block) == false)
				getBlocks().put(block, new BlockRestoreData(block, toID, toData, expireTime));
			else {
				getData(block).update(toID, toData, expireTime);
			}
		}
		public boolean contains(final Block block) {
			return getBlocks().containsKey(block);
		}
		public BlockRestoreData getData(final Block block) {
			if (this.blocks.containsKey(block))
				return this.blocks.get(block);
			return null;
		}
		public Map<Block, BlockRestoreData> getBlocks() {
			return this.blocks;
		}
	}
	class BlockRestoreData {
		protected Block block;
		protected int fromID, toID;
		protected byte fromData, toData;
		protected long expireDelay, epoch;

		public BlockRestoreData(final Block block, final int toID, final byte toData, final long expireDelay) {
			this.block = block;
			this.fromID = block.getTypeId();
			this.fromData = block.getData();
			this.toID = toID;
			this.toData = toData;
			this.expireDelay = expireDelay;
			this.epoch = System.currentTimeMillis();
			set();
		}
		public boolean expire() {
			if (System.currentTimeMillis() - this.epoch < this.expireDelay) {
				return false;
			}
			restore();
			return true;
		}
		public void update(final int toIDIn, final byte toDataIn) {
			this.toID = toIDIn;
			this.toData = toDataIn;
			set();
		}
		public void update(final int toID, final byte addData, final long expireTime) {
			if (toID == 78) {
				if (this.toID == 78)
					this.toData = (byte) Math.min(7, this.toData + addData);
				else {
					this.toData = addData;
				}
			}
			this.toID = toID;
			set();
			this.expireDelay = expireTime;
			this.epoch = System.currentTimeMillis();
		}
		public void set() {
			if (this.toID == 78 && this.toData == 7) {
				this.block.setTypeIdAndData(80, (byte) 0, true);
			} else {
				this.block.setTypeIdAndData(this.toID, this.toData, true);
			}
		}
		public void restore() {
			this.block.setTypeIdAndData(this.fromID, this.fromData, true);
		}
	}
}