package me.muxteam.pvp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import me.muxteam.basic.MuxPlayerNPC;
import me.muxteam.basic.MuxPlayerNPC.Animation;
import me.muxteam.basic.NMSReflection;
import me.muxteam.pvp.MuxGames.GameType;
import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftVillager;
import org.bukkit.craftbukkit.v1_8_R3.util.UnsafeList;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MuxPvPBots implements Listener {
	private MuxSystem ms;
	private final MuxGames games;
	private short botnumber = 1500;
	private final Random r = new Random();
	private final ConcurrentHashMap<Integer, PvPBot> pvpbots = new ConcurrentHashMap<>();

	public MuxPvPBots(final MuxSystem ms) {
		this.ms = ms;
		this.games = ms.getGames();
		startBotSchedulers();
		ms.getServer().getPluginManager().registerEvents(this, ms);
	}
	public void close() {
		pvpbots.values().parallelStream().forEach((PvPBot::delete));
		pvpbots.clear();
		this.ms = null;
	}
	private void startBotSchedulers() {
		new BukkitRunnable() {
			@Override public void run() {
				for (final PvPBot bot : pvpbots.values()) {
					if (bot.targeting == false) continue;
					bot.teleport(bot.getVillagerLocation());
				}
			}
		}.runTaskTimer(ms, 20L, 1L);
		new BukkitRunnable() {
			@Override public void run() {
				for (final PvPBot bot : pvpbots.values()) {
					if (bot.targeting == false) continue;
					else if (bot.getFighter() == null) {
						bot.delete();
						pvpbots.values().remove(bot);
						continue;
					}
					if (bot.getItemInHand() == Material.POTION) continue;
					if (bot.getVillager().hasLineOfSight(bot.getFighter()) && bot.getVillagerLocation().distance(bot.getFighter().getLocation()) < bot.range
							&& (bot.health > 8 || bot.getHealPots() == 0)) {
						bot.playAnimation(bot.getFighter(), Animation.SWING_ARM);
					}
					if (bot.getVillager().hasLineOfSight(bot.getFighter()) && bot.getVillagerLocation().distance(bot.getFighter().getLocation()) < bot.range && System.currentTimeMillis() - bot.lastattack > bot.attackdelay) {
						bot.lastattack = System.currentTimeMillis();
						short damage = (bot.level < 2 ? (short) 1 : bot.level < 3 ? (short) 2 : (short) 3);
						final boolean criticals = bot.hasCriticals() && bot.getVillager().getFallDistance() > 0.0F && bot.getVillager().isOnGround() == false;
						if (criticals) {
							damage *= 1.5;
							damage += 1;
						}
						final EntityDamageEvent e = new EntityDamageEvent(bot.getFighter(), DamageCause.SUICIDE, damage);
						ms.getServer().getPluginManager().callEvent(e);
						if (e.isCancelled() == false) bot.damage(bot.getFighter(), damage);
					}
				}
			}
		}.runTaskTimer(ms, 10L, 5L);
		new BukkitRunnable() {
			@Override public void run() {
				for (final PvPBot bot : pvpbots.values()) {
					if (bot.getFighter() == null) {
						bot.delete();
						pvpbots.values().remove(bot);
						continue;
					}
					if (bot.getFighter().getHealth() < 10D && bot.level < 3 &&
							ms.getMaterialCount(bot.getFighter().getInventory(), Material.POTION, (byte) 16421) == 20) {
						ms.sendNormalTitle(bot.getFighter(), "§d§lHeile dich mit dem Trank!", 10, 10, 5);
					}
					final Location fighterloc = bot.getFighter().getLocation(), botloc = bot.getVillagerLocation();
					if (bot.healing) {
						final Vector v = botloc.toVector().subtract(fighterloc.toVector()).normalize();
						bot.setTarget(v.toLocation(botloc.getWorld()), 0.2F, false);
						continue;
					}
					bot.setTarget(fighterloc, 0.2F, false);
					if (bot.hasCriticals() && bot.getVillager().isOnGround() && fighterloc.getWorld().equals(botloc.getWorld()) && botloc.distance(fighterloc) < bot.range) {
						bot.getVillager().setVelocity(new Vector(0D, 0.4D, 0D));
					}
					//inal boolean riskmode = bot.getFighter().getHealth() <= bot.health;
					if ((bot.health <= 8 + r.nextInt(5) /*&& riskmode == false*/) && bot.getHealPots() > 0) {
						bot.setItemInHand(bot.getFighter(), Material.POTION, (short) 16421);
						bot.healing = true;
						new BukkitRunnable() {
							@Override public void run() {
								if (bot.getFighter() == null) return;
								bot.getVillager().teleport(bot.getVillagerLocation());
								bot.getFighter().playSound(bot.getVillagerLocation(), Sound.SHOOT_ARROW, 0.5F, 0.4F / (r.nextFloat() * 0.4F + 0.8F));
								final ThrownPotion potion = bot.getVillager().launchProjectile(ThrownPotion.class);
								potion.setItem(new ItemStack(Material.POTION, 1, (short) 16421));
								potions.put(potion.getEntityId(), bot.getFighter());
								bot.healpots--;
								bot.health += 3;
							}
						}.runTaskLater(ms, 2L);
						new BukkitRunnable() {
							@Override public void run() {
								if (bot.getFighter() == null) return;
								bot.updateHealth();
								bot.getVillager().teleport(bot.getVillagerLocation());
								bot.getFighter().playSound(bot.getVillagerLocation(), Sound.SHOOT_ARROW, 0.5F, 0.4F / (r.nextFloat() * 0.4F + 0.8F));
								final ThrownPotion potion = bot.getVillager().launchProjectile(ThrownPotion.class);
								potion.setItem(new ItemStack(Material.POTION, 1, (short) 16421));
								potions.put(potion.getEntityId(), bot.getFighter());
								bot.healpots--;
								ms.showItemBar(bot.getFighter(), ms.healthToString(bot.health) + " §8| §fNoch §d" + bot.getHealPots() + " §fHeiltränke");
							}
						}.runTaskLater(ms, 8L);
						new BukkitRunnable() {
							@Override public void run() {
								if (bot.getFighter() == null) return;
								bot.setItemInHand(bot.getFighter(), bot.level < 2 ? Material.STONE_SWORD : bot.level < 3 ? Material.IRON_SWORD : Material.DIAMOND_SWORD, (short) 0);
								bot.health += 3;
								bot.updateHealth();
								ms.showItemBar(bot.getFighter(), ms.healthToString(bot.health) + " §8| §fNoch §d" + bot.getHealPots() + " §fHeiltränke");
							}
						}.runTaskLater(ms, 14L);
						new BukkitRunnable() {
							@Override public void run() {
								if (bot.getFighter() == null) return;
								bot.healing = false;
							}
						}.runTaskLater(ms, 14L /* + 2L +  bot.healdelay*/); // Add delay to make it easier
					}
				}
			}
		}.runTaskTimer(ms, 10L, 10L);
	}
	public PvPBot addPvPBot(final Player p, final Location loc, final int level) {
		final PvPBot bot = new PvPBot(p, "Weihnachtsmann", loc.clone(), level < 2 ? Material.STONE_SWORD : level < 3 ? Material.IRON_SWORD : Material.DIAMOND_SWORD, "Bot " + botnumber , level);
		botnumber++;
		pvpbots.put(bot.getEntityID(), bot);
		return bot;
	}
	public boolean handlePvPBot(final Player p, final org.bukkit.entity.Entity en, final PacketPlayInUseEntity packet) {
		PvPBot bot = null;
		for (final PvPBot b : pvpbots.values()) {
			if (b != null && b.getVillager() != null && b.getFighter() != null && b.getVillager().getEntityId() == en.getEntityId() && b.getFighter() == p) {
				bot = b;
			}
		}
		if (bot == null) return false;
		handlePvPBot(p, bot.getEntityID(), packet);
		return true;
	}
	public void handlePvPBot(final Player p, final int entid, final PacketPlayInUseEntity packet) {
		if (games.inGame(p) != GameType.TRAINING) return;
		if (pvpbots.containsKey(entid) == false) {
			new BukkitRunnable() {
				@Override
				public void run() {
					Villager bot = null;
					for (final Villager v : ms.getWarpsWorld().getEntitiesByClass(Villager.class)) {
						if (v.getEntityId() == entid) {
							bot = v;
							break;
						}
					}
					if (bot != null) handlePvPBot(p, bot, packet);
				}
			}.runTask(ms);
			return;
		}
		final PvPBot bot = pvpbots.get(entid);
		if (packet != null) {
			final String action = Objects.requireNonNull(NMSReflection.getObject(PacketPlayInUseEntity.class, "action", packet)).toString();
			if (action.equals("ATTACK") == false) return;
		}
		if (System.currentTimeMillis() - bot.lasthit > 400L) {
			bot.playAnimation(p, Animation.TAKE_DAMAGE);
			final EntityPlayer ep = ((CraftPlayer)p).getHandle();
			final boolean criticals = bot.hasCriticals() && ep.fallDistance > 0.0F && ep.onGround == false && ep.hasEffect(MobEffectList.BLINDNESS) == false && ep.vehicle == null;
			bot.lasthit = System.currentTimeMillis();
			short damage = 3;
			if (criticals) {
				damage *= 1.5;
				damage += 1;
			}
			bot.health -= damage;
			knockBack(bot.getVillager(), p, 3.0F);
			if (bot.health <= 0) {
				new BukkitRunnable() {
					@Override public void run() {
						if (bot.getVillager() == null) {
							cancel();
							return;
						}
						ms.playEffect(p, EnumParticle.CLOUD, bot.getVillagerLocation(), 0.4F, 0.4F, 0.4F, 0.0001F, 20);
						if (games.damageEvent(p, null)) {
							return;
						} else {
							bot.removeFromTablist(p);
							bot.despawn(p);
							bot.delete();
						}
						pvpbots.values().remove(bot);
					}
				}.runTask(ms);
			} else {
				if (bot.getFighter() == null) return;
				bot.updateHealth();
				ms.showItemBar(p, ms.healthToString(bot.health) + " §8| §fNoch §d" + bot.getHealPots() + " §fHeiltränke");
				new BukkitRunnable() {
					@Override public void run() {
						if (bot.getVillager() == null) {
							cancel();
							return;
						} else if (criticals) {
							double random = Math.random();
							if (random < 0.37D) random = 0.45D;
							ms.playEffect(p, EnumParticle.CRIT, bot.getVillagerLocation().clone(), (float) random, 1F, (float) random, 0F, 50);
						}
						p.playSound(p.getLocation(), Sound.HURT_FLESH, 1F, 1F);
					}
				}.runTask(ms);
			}
		}
	}
	public void knockBack(final LivingEntity target, final LivingEntity attacker, float damage) {
		if (target == null || attacker == null) return;
		final EntityLiving el = ((CraftLivingEntity) target).getHandle(), aEL = ((CraftLivingEntity) attacker).getHandle();
		el.velocityChanged = r.nextDouble() >= el.getAttributeInstance(GenericAttributes.c).getValue();
		double d0 = aEL.locX - el.locX, d1;
		for (d1 = aEL.locZ - el.locZ; d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D) {
			d0 = (Math.random() - Math.random()) * 0.01D;
		}
		el.aw = (float) (MathHelper.b(d1, d0) * 180.0D / Math.PI - (double) el.yaw);
		el.a(aEL, damage, d0, d1);
	}
	class PvPBot extends MuxPlayerNPC {
		private boolean targeting = true, healing = false;
		private long lasthit, lastattack;
		private short health = 20, healpots = 20;
		private Villager villager;
		private Player fighter;
		private final short level;
		private final double range;
		private final long attackdelay, healdelay;
		private final boolean criticals;

		// STANDARD
		// DAMAGE = 3
		// HEALS = 10
		// RANGE = 3.2
		// ATTACKDELAY = 0.5 SECONDS, HEALDELAY = 0L
		public PvPBot(final Player p, final String name, final Location loc, final Material inHand, final String nametag, final int level) {
			super(name, "§8Bot", loc, inHand, nametag);
			final EntityVillager v = new EntityVillager(((CraftWorld) loc.getWorld()).getHandle());
			v.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
			((CraftWorld) loc.getWorld()).getHandle().addEntity(v);
			v.b(true);
			v.setInvisible(true);
			this.villager = (Villager) v.getBukkitEntity();

			NMSReflection.setObject(net.minecraft.server.v1_8_R3.Entity.class, "uniqueID", v, ms.getIdentificationId());
			this.villager.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 1000000000, 1, false, false));
			this.villager.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 1000000000, 3, false, false));
			this.villager.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 1000000000, 3, false, false));
			this.villager.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE, 1000000000, 100, false, false));
			this.villager.setMaxHealth(2048D);
			this.villager.setHealth(2048D);
			this.villager.setNoDamageTicks(1000000000);

			this.fighter = p;
			this.spawn(fighter);
			updateHealth();
			this.setHelmet(fighter, Material.DIAMOND_HELMET);
			this.setChestplate(fighter, Material.DIAMOND_CHESTPLATE);
			this.setLeggings(fighter, Material.DIAMOND_LEGGINGS);
			this.setBoots(fighter, Material.DIAMOND_BOOTS);
			removePathFinders(villager);

			this.level = (short) level;
			this.range = 2.8 + (level * 0.02);
			this.attackdelay = 580L - (level * 6L);
			this.healdelay = (long) (0.13 * level);
			this.criticals = true;
		}
		public void delete() {
			if (villager != null) villager.remove();
			villager = null;
			fighter = null;
			targeting = false;
		}
		public Player getFighter() {
			return fighter;
		}
		public Location getVillagerLocation() {
			if (healing) {
				final Location l = villager.getLocation();
				l.setPitch(90F);
				return l;
			}
			return villager.getLocation();
		}
		public int getHealPots() {
			return healpots;
		}
		public boolean hasCriticals() {
			return criticals;
		}
		@Override public void damage(final LivingEntity en, final double damage) {
			knockBack(en, villager, (float) damage);
			super.damage(en, damage);
		}
		public void updateHealth() {
			ms.getScoreboard().updateHealthOf(fighter, getNameTag(), health);
			super.updateHealth(fighter, health);
		}
		private Location target;
		public void changeTarget(final Location loc, final float speed) {
			final EntityVillager envi = ((CraftVillager) villager).getHandle();
			this.villager.setHealth(2048D);
			envi.getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).setValue(speed);
			envi.targetSelector.a(3, new FightPath(envi, speed, loc.getX(), loc.getY(), loc.getZ()));
		}
		public Villager getVillager() {
			return villager;
		}
		public void setTarget(Location loc, final float speed, final boolean force) {
			if (villager != null && villager.getLocation().getWorld().getName().equals(loc.getWorld().getName()) && villager.getLocation().distance(loc) > 15.95) {
				if (force) target = loc;
				if (target != null) {
					final Location vloc = villager.getLocation();
					while (vloc.distance(loc) > 15.95) {
						loc = new Location(loc.getWorld(), (loc.getX() + vloc.getX()) / 2, (loc.getY() + vloc.getY()) / 2, (loc.getZ() + vloc.getZ()) / 2);
					}
					changeTarget(loc, speed);
				}
				return;
			}
			if (target != null) target = null;
			changeTarget(loc, speed);
		}
		private void removePathFinders(final org.bukkit.entity.Entity en) {
			final EntityCreature entity = (EntityCreature) ((CraftEntity) en).getHandle();
			final PathfinderGoalSelector goal = (PathfinderGoalSelector) NMSReflection.getObject(EntityInsentient.class, "goalSelector", entity);
			final PathfinderGoalSelector target = (PathfinderGoalSelector) NMSReflection.getObject(EntityInsentient.class, "targetSelector", entity);
			((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "b", goal))).clear();
			((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "c", goal))).clear();
			((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "b", target))).clear();
			((UnsafeList<PathfinderGoal>) Objects.requireNonNull(NMSReflection.getObject(PathfinderGoalSelector.class, "c", target))).clear();
		}
		public void teleport(final Location loc) {
			this.teleport(fighter, loc, villager.isOnGround());
		}
		class FightPath extends PathfinderGoal {
			private final EntityCreature b;
			protected final double a;
			private final double c, d, e;

			public FightPath(final EntityCreature entitycreature, final double d0, final double x, final double y, final double z) {
				this.b = entitycreature;
				this.a = d0;
				this.d = y;
				this.c = x;
				this.e = z;
			}
			@Override public boolean a() {
				final Vec3D vec3d = RandomPositionGenerator.a(this.b, 5, 4);
				return vec3d != null;
			}
			@Override public void c() {
				final Vec3D vec3d = RandomPositionGenerator.a(this.b, 5, 4);
				if (vec3d == null) return;
				this.b.getNavigation().a(c, d, e, 2);
			}
			@Override public boolean b() {
				if ((this.b.ticksLived - this.b.hurtTimestamp) > 100) {
					this.b.b((EntityLiving) null);
					return false;
				}
				return this.b.getNavigation().m() == false;
			}
		}
	}
	@EventHandler
	public void onInteract(final PlayerInteractEvent e) {
		if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_AIR) return;
		else if (e.getMaterial() != Material.POTION) return;
		else if (games.inGame(e.getPlayer()) != GameType.TRAINING) return;
		thrower = e.getPlayer();
	}
	@EventHandler
	public void onProjectile(final ProjectileLaunchEvent e) {
		if (e.getEntity() == null || e.getEntityType() != EntityType.SPLASH_POTION || e.getEntity().getShooter() instanceof Player == false) return;
		final Player p = (Player) e.getEntity().getShooter();
		if (games.inGame(p) == GameType.TRAINING) {
			potions.put(e.getEntity().getEntityId(), p);
		}
	}
	@EventHandler
	public void onRegenerate(final EntityRegainHealthEvent e) {
		if (e.getEntity() instanceof Player == false) return;
		final Player p = (Player) e.getEntity();
		if (e.getRegainReason() != RegainReason.SATIATED) return;
		PvPBot bot = getBotOfPlayer(p);
		if (bot == null) return;
		bot.health = (short) Math.min(20, bot.health + 1);
		bot.updateHealth();
	}
	@EventHandler
	public void onSplash(final PotionSplashEvent e) {
		final int id = e.getEntity().getEntityId();
		final Player p = potions.remove(id);
		if (p != null) {
			final boolean bot = e.getEntity().getShooter() != p;
			for (final LivingEntity le : e.getAffectedEntities()) {
				if (le instanceof Player && le != p /*|| bot*/) e.setIntensity(le, 0D);
				else if (le instanceof Player == false && bot == false) {
					final PvPBot b = getBotOfPlayer(p);
					if (b == null || b.getVillager().getEntityId() != le.getEntityId()) continue;
					b.health = (short) Math.min(20, b.health + 3); // You heal the bot
				} else if (le instanceof Player && bot) {
					final PvPBot b = getBotOfPlayer(p);
					if (b != null && b.level <= 3) p.setHealth(Math.min(20D, p.getHealth() + 1D));
					e.setIntensity(le, 0D);
				}
			}
			lastpotion = p;
		}
	}
	private PvPBot getBotOfPlayer(final Player p) {
		PvPBot bot = null;
		for (final PvPBot b : pvpbots.values()) {
			if (b.getFighter() == p) bot = b;
		}
		return bot;
	}
	private final HashMap<UUID, PacketBlocker> blocker = new HashMap<>();
	public final Map<Integer, Player> potions = new HashMap<>();
	private Player lastpotion, thrower;

	public void blockPotionPackets(final Player p) {
		blocker.put(p.getUniqueId(), new PacketBlocker(p));
	}
	public void unblockPotionPackets(final Player p) {
		blocker.remove(p.getUniqueId()).unblock();
	}
	class PacketBlocker {
		private final Player player;
		public Channel channel;

		public PacketBlocker(final Player player) {
			this.player = player;
			this.block();
		}

		public void block() {
			final CraftPlayer craftPlayer = (CraftPlayer) this.player;
			this.channel = craftPlayer.getHandle().playerConnection.networkManager.channel;
			if (channel.pipeline().get("EdILeCOpDer") == null) channel.pipeline().addBefore("packet_handler", "EdILeCOpDer", new ChannelDuplexHandler() {
				@Override
				public void write(final ChannelHandlerContext ctx, final Object msg, final ChannelPromise promise) throws Exception {
					if (player == null || msg == null) return;
					if (msg instanceof Packet) {
						final Packet packet = (Packet) msg;
						if (packet instanceof PacketPlayOutSpawnEntity) {
							final PacketPlayOutSpawnEntity spawn = (PacketPlayOutSpawnEntity) packet;
							final int type = (int) NMSReflection.getObject(PacketPlayOutSpawnEntity.class, "j", spawn);
							final int entityid = (int) NMSReflection.getObject(PacketPlayOutSpawnEntity.class, "a", spawn);
							if (type == 73 && potions.get(entityid) != player) {
								return;
							}
						} else if (packet instanceof PacketPlayOutWorldEvent) {
							final PacketPlayOutWorldEvent world = (PacketPlayOutWorldEvent) packet;
							final int type = (int) NMSReflection.getObject(PacketPlayOutWorldEvent.class, "a", world);
							if (type == 2002 && lastpotion != player) {
								return;
							}
						} else if (packet instanceof PacketPlayOutNamedSoundEffect) {
							final PacketPlayOutNamedSoundEffect sound = (PacketPlayOutNamedSoundEffect) packet;
							final String s = (String) NMSReflection.getObject(PacketPlayOutNamedSoundEffect.class, "a", sound);
							if (s != null && s.equals("random.bow") && thrower != player) {
								return;
							}
						}
					}
					super.write(ctx, msg, promise);
				}
			});
		}
		public void unblock() {
			channel.eventLoop().execute(() -> {
				try {
					if (channel.pipeline().get("EdILeCOpDer") != null) channel.pipeline().remove("EdILeCOpDer");
				} catch (final Exception e) {
					System.out.println("MuxSystem> Error at removing channel from player. Ignoring...");
				}
			});
		}
	}
}