package me.muxteam.extras;

import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.EntityLiving;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityMountEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class MuxCars implements Listener {
	private MuxSystem ms;
	private Field jump = null;

	public MuxCars(final MuxSystem ms) {
		this.ms = ms;
		setup();
		ms.getServer().getPluginManager().registerEvents(this, ms);
	}

	public void close() {
		this.ms = null;
	}

	private void setup() {
		try {
			jump = EntityLiving.class.getDeclaredField("aY");
			jump.setAccessible(true);
		} catch (final NoSuchFieldException ex) {
			System.err.println("MuxSystem> Could not set up MuxCars.\n" + ex.getMessage());
		}
	}

	private ItemStack getMaterial(final String type) {
		if (type.equals("BLACK")) return new ItemStack(Material.STAINED_CLAY, 1, (byte) 15);
		else return new ItemStack(Material.AIR);
	}

	private ItemStack getWheelItem(final String type) {
		if (type.equals("BLACK")) return ms.getHeadFromURL("https://textures.minecraft.net/texture/444cd5d2fd2fe397ddb927e184f19ecbd0fe24af6ef68852c172ae447cd4", "VehiclePart");
		else return new ItemStack(Material.AIR);
	}

	private ItemStack getSeatItem(final String type) {
		if (type.equals("BLACK")) return new ItemStack(Material.WOOD_STEP, 1, (byte) 5);
		else return new ItemStack(Material.AIR);
	}

	public ArmorStand spawnStandBike(final Location loc, final Player p, final String type) {
		final ArrayList<ArmorStand> list = loadStructure(loc, "Bike.schem");
		final Location location = loc.clone().add(new Vector(0.5D, -0.5D, 0.8D));
		location.setYaw(0.0F);
		location.setPitch(0.0F);
		final ArmorStand armorstand1 = (ArmorStand) location.getWorld().spawnEntity(location.clone().add(new Vector(0.0D, 0.5D, 0.0D)), EntityType.ARMOR_STAND);
		setGravity(armorstand1, false);
		armorstand1.setVisible(false);

		final ArmorStand armorstand2 = (ArmorStand) location.getWorld().spawnEntity(location, EntityType.ARMOR_STAND);
		setGravity(armorstand2, false);
		armorstand2.setVisible(false);
		armorstand2.setCustomName("BikePart;" + armorstand1.getUniqueId());

		final ArmorStand armorstand3 = (ArmorStand) location.getWorld().spawnEntity(location.clone().subtract(new Vector(0.0D, 1.4D, 0.0D)), EntityType.ARMOR_STAND);
		setGravity(armorstand3, false);
		armorstand3.setVisible(false);
		armorstand3.setCustomName("BikePart;" + armorstand1.getUniqueId());
		armorstand3.setPassenger(armorstand2);
		final String str = "Bike;" + type + ";" + p.getUniqueId().toString() + ";" + armorstand2.getUniqueId() + ";" + armorstand3.getUniqueId() + ";";
		for (final ArmorStand stand : list) {
			stand.setCustomName("BikePart;" + armorstand1.getUniqueId());
			removeSlots(stand);
			if (stand.getHelmet().getType() == Material.COAL_BLOCK) {
				stand.setHelmet(getMaterial(type));
			} else if (stand.getHelmet().getType() == Material.SKULL_ITEM) {
				stand.setHelmet(getWheelItem(type));
			} else if (stand.getHelmet().getType() == Material.STEP) {
				stand.setHelmet(getSeatItem(type));
			}
		}
		armorstand1.setCustomName(str);
		removeSlots(armorstand1);
		removeSlots(armorstand2);
		removeSlots(armorstand3);
		return armorstand1;
	}

	public Bike spawnBike(final Location loc, final Player p, final String type) {
		final ArmorStand stand1 = spawnStandBike(loc, p, type);
		final String[] array1 = stand1.getCustomName().split(";");
		final String str2 = array1[3];
		ArmorStand stand2 = null;
		Bike bike = null;
		for (final Entity en : stand1.getNearbyEntities(5.0D, 5.0D, 5.0D)) {
			if (en instanceof ArmorStand && en.getUniqueId().toString().equals(str2)) {
				stand2 = (ArmorStand) en;
				break;
			}
		}
		if (stand2 != null && stand2.getPassenger() == null) {
			ArmorStand stand3 = null;
			final ArrayList<ArmorStand> list = new ArrayList<>();
			for (final Entity en : stand1.getNearbyEntities(5.0D, 5.0D, 5.0D)) {
				if (en instanceof ArmorStand) {
					if (en.getUniqueId().toString().equals(array1[4])) {
						stand3 = (ArmorStand) en;
						list.add(stand3);
					} else if (en.getCustomName() != null && en.getCustomName().startsWith("BikePart;")) {
						final String str3 = en.getCustomName();
						final String[] arrayOfString2 = str3.split(";");
						final String str4 = arrayOfString2[1];
						if (str4.equals(stand1.getUniqueId().toString())) {
							list.add((ArmorStand) en);
						}
					}
				}
			}
			stand2.setPassenger(p);
			bike = new Bike(stand1, stand2, stand3, list, p);
		}
		return bike;
	}

	private void removeSlots(final ArmorStand stand) {
		final net.minecraft.server.v1_8_R3.Entity en = ((CraftEntity) stand).getHandle();
		final NBTTagCompound nbt = new NBTTagCompound();
		en.c(nbt);
		nbt.setInt("DisabledSlots", 2096896);
		en.f(nbt);
	}

	private void setGravity(final ArmorStand stand, final boolean b) {
		stand.setGravity(b);
		((CraftEntity) stand).getHandle().noclip = b^true;
	}

	private ArrayList<ArmorStand> loadStructure(final Location paramLocation, final String name) {
		final InputStreamReader in = new InputStreamReader(ms.getResource(name), StandardCharsets.UTF_8);
		final BufferedReader reader = new BufferedReader(in);
		final YamlConfiguration config = YamlConfiguration.loadConfiguration(reader);
		final ArrayList<ArmorStand> list = new ArrayList<>();
		short i = 0;
		while (config.contains(String.valueOf(i))) {
			final ItemStack itemstack1 = config.getItemStack(i + ".helmet"), itemstack2 = config.getItemStack(i + ".leggings"), itemstack3 = config.getItemStack(i + ".chestplate"), itemstack4 = config.getItemStack(i + ".boots"), itemstack5 = config.getItemStack(i + ".hand");
			final boolean bool1 = config.getBoolean(i + ".visible"), bool2 = config.getBoolean(i + ".small"), bool3 = config.getBoolean(i + ".arms"), bool4 = config.getBoolean(i + ".plate");
			final EulerAngle eulerangle1 = new EulerAngle(config.getDouble(i + ".body.x"), config.getDouble(i + ".body.y"), config.getDouble(i + ".body.z")), eulerangle2 = new EulerAngle(config.getDouble(i + ".head.x"), config.getDouble(i + ".head.y"), config.getDouble(i + ".head.z")), eulerangle3 = new EulerAngle(config.getDouble(i + ".leftarm.x"), config.getDouble(i + ".leftarm.y"), config.getDouble(i + ".leftarm.z")), eulerangle4 = new EulerAngle(config.getDouble(i + ".rightarm.x"), config.getDouble(i + ".rightarm.y"), config.getDouble(i + ".rightarm.z")), eulerangle5 = new EulerAngle(config.getDouble(i + ".leftleg.x"), config.getDouble(i + ".leftleg.y"), config.getDouble(i + ".leftleg.z")), eulerangle6 = new EulerAngle(config.getDouble(i + ".rightleg.x"), config.getDouble(i + ".rightleg.y"), config.getDouble(i + ".rightleg.z"));
			final float f1 = Float.parseFloat(config.get(i + ".pitch").toString()), f2 = Float.parseFloat(config.get(i + ".yaw").toString());
			final double d1 = config.getDouble(i + ".position.x"), d2 = config.getDouble(i + ".position.y"), d3 = config.getDouble(i + ".position.z");
			final Location loc = new Location(paramLocation.getWorld(), paramLocation.getX() - d1, paramLocation.getY() - d2, paramLocation.getZ() - d3);
			loc.setPitch(f1);
			loc.setYaw(f2);
			final ArmorStand armorstand = (ArmorStand) loc.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
			setGravity(armorstand, false);
			armorstand.teleport(loc);
			armorstand.setHelmet(itemstack1);
			armorstand.setLeggings(itemstack2);
			armorstand.setChestplate(itemstack3);
			armorstand.setBoots(itemstack4);
			armorstand.setItemInHand(itemstack5);
			armorstand.setVisible(bool1);
			armorstand.setSmall(bool2);
			armorstand.setArms(bool3);
			armorstand.setBasePlate(bool4);
			armorstand.setBodyPose(eulerangle1);
			armorstand.setHeadPose(eulerangle2);
			armorstand.setLeftArmPose(eulerangle3);
			armorstand.setRightArmPose(eulerangle4);
			armorstand.setLeftLegPose(eulerangle5);
			armorstand.setRightLegPose(eulerangle6);
			list.add(armorstand);
			i++;
		}
		try {
			in.close();
			reader.close();
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return list;
	}

	@EventHandler
	public void protectVehiclesFromMinecarts(final EntityMountEvent e) {
		if (e.getEntity() instanceof ArmorStand) {
			final String str = e.getEntity().getCustomName();
			if (str == null || e.getMount() instanceof Minecart == false && e.getMount() instanceof Boat == false) {
				return;
			}
			if (str.startsWith("BikePart;") || str.startsWith("Bike;") || str.startsWith("Car;")) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void protectVehiclesFromPistons1(final BlockPistonExtendEvent e) {
		for (final Block b : e.getBlocks()) {
			for (final Entity en : getNearbyStands(b, 0.5D)) {
				final String name = en.getCustomName();
				if (name != null && (name.startsWith("BikePart;") || name.startsWith("Bike;") || name.startsWith("Car;"))) {
					e.setCancelled(true);
					return;
				}
			}
		}
	}

	@EventHandler
	public void protectVehiclesFromPistons2(final BlockPistonRetractEvent e) {
		for (final Block b : e.getBlocks()) {
			for (final Entity en : getNearbyStands(b, 0.5D)) {
				final String name = en.getCustomName();
				if (name != null && (name.startsWith("BikePart;") || name.startsWith("Bike;") || name.startsWith("Car;") || name.startsWith("CarPart;"))) {
					e.setCancelled(true);
					return;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void protectBikes(final PlayerInteractAtEntityEvent e) {
		if (e.getRightClicked() instanceof ArmorStand) {
			final ArmorStand stand = (ArmorStand) e.getRightClicked();
			if (stand.getCustomName() != null && (stand.getCustomName().startsWith("BikePart;") || stand.getCustomName().startsWith("Bike;"))) {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void protectBikes2(final PlayerArmorStandManipulateEvent e) {
		final ArmorStand stand = e.getRightClicked();
		final String name = stand.getCustomName();
		if (name != null && (name.startsWith("BikePart;") || name.startsWith("Bike;"))) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void protectFromBreaking(final EntityDamageEvent e) {
		if (e.getEntity() instanceof ArmorStand == false) return;
		final ArmorStand stand = (ArmorStand) e.getEntity();
		final String name = stand.getCustomName();
		if (name != null && (name.startsWith("BikePart;") || name.startsWith("Bike;"))) {
			e.setCancelled(true);
			if (stand.getPassenger() instanceof Player && e.getCause() == EntityDamageEvent.DamageCause.VOID) {
				ms.getMounts().deactivateMount((Player) stand.getPassenger());
			}
		}
	}

	private List<Entity> getNearbyStands(final Block block, final double distance) {
		final Location loc = block.getLocation().clone().add(0.5D, 0.5D, 0.5D);
		final Collection<Entity> nearbyEntities = loc.getWorld().getNearbyEntities(loc, distance, distance, distance);
		return nearbyEntities.stream().filter(ArmorStand.class::isInstance).collect(Collectors.toList());
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onInteractBike(final PlayerInteractAtEntityEvent event) {
		if (event.getRightClicked() instanceof ArmorStand && event.getPlayer().getGameMode() != GameMode.SPECTATOR && event.getRightClicked().isDead() == false) {
			final ArmorStand stand1 = (ArmorStand) event.getRightClicked();
			if (stand1.getCustomName() != null && (stand1.getCustomName().startsWith("BikePart;") || stand1.getCustomName().startsWith("Bike;"))) {
				ArmorStand stand2 = null;
				if (stand1.getCustomName().startsWith("BikePart;")) {
					final String customname = stand1.getCustomName();
					final String[] arrayOfString1 = customname.split(";");
					final String name = arrayOfString1[1];
					for (final Entity en : stand1.getNearbyEntities(5.0D, 5.0D, 5.0D)) {
						if (en instanceof ArmorStand && en.getUniqueId().toString().equals(name)) {
							stand2 = (ArmorStand) en;
							break;
						}
					}
				} else {
					stand2 = stand1;
				}
				if (stand2 != null) {
					final String[] arrayOfString2 = stand2.getCustomName().split(";");
					if (arrayOfString2.length < 3) return;
					final String name2 = arrayOfString2[2];
					final Player p = event.getPlayer();
					final boolean friend = ms.getFriends().areFriends(p.getName(), UUID.fromString(name2));
					if (p.getUniqueId().toString().equals(name2) == false && friend == false) {
						ms.showItemBar(p, "§cDu bist nicht mit dem Spieler befreundet.");
						return;
					} else if (ms.inBattle(p.getName(), p.getLocation())) {
						ms.showItemBar(p, ms.hnotinfight);
						return;
					}
					ArmorStand stand4 = null;
					final String bikeuuid = arrayOfString2[3];
					if (friend) {
						for (final Entity en : stand2.getNearbyEntities(5.0D, 5.0D, 5.0D)) {
							if (en.getCustomName() != null && en.getCustomName().contains("friend") && en.getCustomName().contains(bikeuuid)) {
								stand4 = (ArmorStand) en;
								break;
							}
						}
						if (stand4 != null && stand4.getPassenger() == null) {
							ms.getMounts().deactivateMount(p);
							stand4.setPassenger(p);
						}
						return;
					}
					for (final Entity en : stand2.getNearbyEntities(5.0D, 5.0D, 5.0D)) {
						if (en instanceof ArmorStand && en.getUniqueId().toString().equals(arrayOfString2[3])) {
							stand4 = (ArmorStand) en;
							break;
						}
					}
					if (stand4 != null && stand4.getPassenger() == null && arrayOfString2[1].equals("BLACK")) {
						ms.getMounts().mount(p);
					}
				}
			}
		}
	}

	public class Bike {
		private int taskID = -1;
		private final List<ArmorStand> stands;
		private final ArmorStand mainstand, seatstand, movilitystand;
		private final ArmorStand seatfriend, movilityfriend;
		private final HashMap<ArmorStand, Vector> vectors = new HashMap<>();
		private final double initialZ;
		private final double acc = 0.006, maxspeed = 0.6;

		public Bike(final ArmorStand stand1, final ArmorStand stand2, final ArmorStand stand3, final List<ArmorStand> list, final Player p) {
			this.mainstand = stand1;
			this.stands = list;
			this.seatstand = stand2;
			this.movilitystand = stand3;
			this.initialZ = stand1.getLocation().getDirection().getZ();
			for (final ArmorStand stand : list) {
				vectors.put(stand, getPositionVector(stand));
			}
			seatfriend = (ArmorStand) seatstand.getLocation().getWorld().spawnEntity(seatstand.getLocation().clone().subtract(0D, 0D, 0.5D), EntityType.ARMOR_STAND);
			setGravity(seatfriend, false);
			seatfriend.setVisible(false);
			seatfriend.setCustomName("BikePart;" + seatstand.getUniqueId() + ";friend");

			movilityfriend = (ArmorStand) seatfriend.getLocation().getWorld().spawnEntity(seatfriend.getLocation().subtract(0, 1.4D, 0D), EntityType.ARMOR_STAND);
			setGravity(movilityfriend, false);
			movilityfriend.setVisible(false);
			movilityfriend.setCustomName("BikePart;" + seatstand.getUniqueId());
			movilityfriend.setPassenger(seatfriend);
			removeSlots(seatfriend);
			removeSlots(movilityfriend);
			vectors.put(movilityfriend, getPositionVector(movilityfriend));

			setBikeName(p);
			startRide(p);
		}

		private void startRide(final Player p) {
			setGravity(mainstand, true);
			this.taskID = new BukkitRunnable() {
				double addX, addZ;
				final double toAdd = acc;
				Location lastLoc = mainstand.getLocation().clone();
				int ticksLastLoc = 0, lastJumpTicks = 0;

				@Override public void run() {
					if (seatstand.getPassenger() == null) {
						dismounted(p);
						return;
					}
					if (p.isOnline() == false) {
						seatstand.eject();
						dismounted(p);
						return;
					}
					final Material m = mainstand.getLocation().getBlock().getType();
					if (m == Material.WATER || m == Material.STATIONARY_WATER || m == Material.LAVA || m == Material.STATIONARY_LAVA) {
						ms.getMounts().deactivateMount(p);
						return;
					}
					if (lastLoc.distance(mainstand.getLocation()) == 0.0D) {
						ticksLastLoc += 1;
						if (ticksLastLoc > 3) {
							addX = addZ = 0.0D;
						}
					} else {
						ticksLastLoc = 0;
						lastLoc = mainstand.getLocation();
					}
					final float[] arrayOfFloat = sidesFront(p);
					final float f = arrayOfFloat[0];
					final double d1 = arrayOfFloat[1];
					if (lastJumpTicks > 0) {
						lastJumpTicks -= 1;
					}
					if (mainstand.isOnGround() && lastJumpTicks == 0 && isSpacePressed(p)) {
						mainstand.setVelocity(new Vector(mainstand.getVelocity().getX(), 0.5D, mainstand.getVelocity().getZ()));
						lastJumpTicks = 10;
					}
					final Vector v = mainstand.getLocation().getDirection();
					if (d1 > 0.0D) {
						final double d2 = v.getX(), d3 = v.getZ();
						if (d2 * addX < maxspeed && d3 * addZ < maxspeed && d2 * addX > -maxspeed && d3 * addZ > -maxspeed) {
							addX += toAdd;
							addZ += toAdd;
						}
					} else {
						if (addX > 0.0D - toAdd) {
							if (d1 < 0.0D) {
								addX -= toAdd * (d1 * -3.5D);
							} else {
								addX -= toAdd;
							}
						}
						if (addZ > 0.0D - toAdd) {
							if (d1 < 0.0D) {
								addZ -= toAdd * (d1 * -3.5D);
							} else {
								addZ -= toAdd;
							}
						}
						if (addX < 0.0D) {
							addX = 0.0D;
						}
						if (addZ < 0.0D) {
							addZ = 0.0D;
						}
					}
					mainstand.setVelocity(new Vector(v.getX() * addX, mainstand.getVelocity().getY(), v.getZ() * addZ));
					setYaw(mainstand, getYaw(mainstand) - f);
					setYaw(seatstand, getYaw(mainstand));
					setYaw(seatfriend, getYaw(mainstand));

					movilitystand.eject();
					movilityfriend.eject();
					for (final ArmorStand stand : vectors.keySet()) {
						positionMainStand(stand);
					}
					movilitystand.setPassenger(seatstand);
					movilityfriend.setPassenger(seatfriend);
				}
			}.runTaskTimer(ms, 1L, 1L).getTaskId();
		}

		private boolean isSpacePressed(final Player p) {
			try {
				return jump.getBoolean(((CraftPlayer) p).getHandle());
			} catch (final IllegalArgumentException | IllegalAccessException  e) {
				e.printStackTrace();
			}
			return false;
		}

		private float[] sidesFront(final Player p) {
			final float[] array = new float[2];
			array[0] = ((CraftPlayer) p).getHandle().aZ * 6.0F;
			array[1] = ((CraftPlayer) p).getHandle().ba;
			return array;
		}

		private float getYaw(final Entity en) {
			return ((CraftEntity) en).getHandle().yaw;
		}

		private void setYaw(final Entity en, final float yaw) {
			((CraftEntity) en).getHandle().yaw = yaw;
		}

		private Vector getPositionVector(final ArmorStand stand) {
			final Location loc1 = mainstand.getLocation(), loc2 = stand.getLocation();
			final double d1 = loc1.getX() - loc2.getX(), d2 = loc1.getY() - loc2.getY(), d3 = loc1.getZ() - loc2.getZ();
			return new Vector(d1, d2, d3);
		}

		private void positionMainStand(final ArmorStand stand) {
			final Location loc = mainstand.getLocation().clone();
			final Vector vec1 = loc.getDirection().clone(), vec2 = vectors.get(stand).clone();
			final double d = vec2.getZ();
			vec2.setX(vec1.getX() * d / initialZ);
			vec2.setZ(vec1.getZ() * d / initialZ);
			loc.setPitch(mainstand.getLocation().getPitch());
			loc.setYaw(mainstand.getLocation().getYaw());
			stand.teleport(loc.subtract(vec2));
		}

		private void setBikeName(final Player p) {
			final String name = p.getName(), lastchar = name.substring(name.length() - 1).toLowerCase();
			final boolean x = lastchar.equals("s") || lastchar.equals("x") || lastchar.equals("z");
			seatstand.setCustomName("§a" + name + "'" + (x ? "" : "s") + " §7Bike");
			seatstand.setCustomNameVisible(false);
		}

		private void dismounted(final Player p) {
			if (taskID != -1) {
				Bukkit.getScheduler().cancelTask(taskID);
				taskID = -1;
			}
			seatstand.setCustomNameVisible(true);
			if (mainstand.isOnGround() == false) {
				ms.getMounts().removeMount(p.getUniqueId());
				ms.showItemBar(p, "§cDein Bike wurde deaktiviert, da du in der Luft ausgestiegen bist.");
				return;
			}
			setGravity(mainstand, false);
			setGravity(movilitystand, false);
			movilitystand.setPassenger(seatstand);

			setGravity(movilityfriend, false);
			movilityfriend.setPassenger(seatfriend);

			for (final ArmorStand stand : stands) {
				positionMainStand(stand);
			}
		}

		public void mount(final Player p) {
			seatstand.setCustomNameVisible(false);
			seatstand.setPassenger(p);
			if (taskID == -1) startRide(p);
		}

		public void remove() {
			for (final ArmorStand stand : stands) {
				stand.remove();
			}
			seatstand.remove();
			seatfriend.remove();
			mainstand.remove();
			if (taskID != -1) {
				Bukkit.getScheduler().cancelTask(taskID);
				taskID = -1;
			}
			movilitystand.remove();
			movilityfriend.remove();
		}
	}
}