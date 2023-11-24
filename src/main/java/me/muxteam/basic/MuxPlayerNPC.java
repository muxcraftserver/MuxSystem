package me.muxteam.basic;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.util.UUIDTypeAdapter;
import me.muxteam.basic.MuxActions.PlayerAction;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_8_R3.DataWatcher;
import net.minecraft.server.v1_8_R3.EntityPotion;
import net.minecraft.server.v1_8_R3.PacketPlayOutAnimation;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityEquipment;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityHeadRotation;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityStatus;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityTeleport;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedEntitySpawn;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedSoundEffect;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntity;
import net.minecraft.server.v1_8_R3.WorldSettings;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftChatMessage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class MuxPlayerNPC {
	private final int entityID;
	private final String name, nametag;
	private String tablist;
	private DataWatcher watcher;
	private GameProfile profile;
	private Material helmet, chestplate, leggings, boots;
	private Material inHand;
	private Location loc;
	private final String skinName;
	private PlayerAction action = null;

	public MuxPlayerNPC(final String skinName, final String name, final String tablist, final int entityID, final Location location, final Material inHand, final String nametag) {
		this.loc = location;
		this.tablist = ChatColor.translateAlternateColorCodes('&', tablist);
		this.name = ChatColor.translateAlternateColorCodes('&', name);
		this.entityID = entityID;
		this.inHand = inHand;
		this.skinName = skinName;
		this.watcher = new DataWatcher(null);
		this.nametag = nametag;
		watcher.a(6, (float) 20);
		// watcher.a(10, (byte) 127); // Second layer skin
	}
	public MuxPlayerNPC(final String name, final String tablist, final Location location, final Material inHand, final String nametag) {
		this(null, name, tablist, ThreadLocalRandom.current().nextInt(10000), location, inHand, nametag);
	}

	public void refreshNPC() {
		final Set<Player> cansee = getNearbyPlayers();
		final Iterator<Player> iter = getPlayersTracking().iterator();
		while (iter.hasNext()) {
			final Player p = iter.next();
			removeFromTablist(p); // Causes errors if two NPCs have same name
			if (cansee.contains(p) == false) {
				iter.remove();
				if (p.getWorld() == loc.getWorld()) {
					despawn(p);
				}
			}
		}
		for (final Player p : cansee) {
			if (getPlayersTracking().contains(p) == false) {
				getPlayersTracking().add(p);
				spawn(p);
				teleport(p, loc, true);
			}
		}
	}
	private final Set<Player> playerstracking = new HashSet<>();

	private Set<Player> getPlayersTracking() {
		return playerstracking;
	}
	private Set<Player> getNearbyPlayers() {
		final Set<Player> nearby = new HashSet<>();
		for (final Player p : loc.getWorld().getPlayers()) {
			if (isVisible(p)) {
				nearby.add(p);
			}
		}
		return nearby;
	}
	private boolean isVisible(final Player p) {
		if (loc.getWorld() == p.getWorld()) {
			return loc.distance(p.getLocation()) < 40;
		}
		return false;
	}

	public void spawn(final Player p) {
		try {
			final PacketPlayOutNamedEntitySpawn packet = new PacketPlayOutNamedEntitySpawn();
			addToTablist(p);
			setValue(packet, "a", entityID);
			setValue(packet, "b", this.profile.getId());
			setValue(packet, "c", toFixedPoint(loc.getX()));
			setValue(packet, "d", toFixedPoint(loc.getY()));
			setValue(packet, "e", toFixedPoint(loc.getZ()));
			setValue(packet, "f", toPackedByte(loc.getYaw()));
			setValue(packet, "g", toPackedByte(loc.getPitch()));
			setValue(packet, "h", inHand == null ? 0 : inHand.getId());
			setValue(packet, "i", watcher);
			final PacketPlayOutEntityMetadata packet2 = new PacketPlayOutEntityMetadata(entityID, watcher, true);
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet2);
			changePlayerlistName(p, tablist);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	public void despawn(final Player p) {
		final PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(this.entityID);
		((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
	}
	public void teleport(final Player p, final Location loc, final boolean ground) {
		try {
			final PacketPlayOutEntityTeleport packet = new PacketPlayOutEntityTeleport(this.entityID,
					toFixedPoint(loc.getX()), toFixedPoint(loc.getY()), toFixedPoint(loc.getZ()),
				toPackedByte(loc.getYaw()), toPackedByte(loc.getPitch()), ground);
			final PacketPlayOutEntityHeadRotation packet2 = new PacketPlayOutEntityHeadRotation();
			setValue(packet2, "a", entityID);
			setValue(packet2, "b", toPackedByte(loc.getYaw()));
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet2);
			this.loc = loc;
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	public void playAnimation(final Player p, final Animation a) {
		try {
			final PacketPlayOutAnimation packet = new PacketPlayOutAnimation();
			setValue(packet, "a", entityID);
			setValue(packet, "b", a.getValue());
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	public void throwPotion(final Player p, final Location loc) {
		try {
			final EntityPotion pot = new EntityPotion(((CraftWorld) p.getWorld()).getHandle());
			pot.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
			pot.setPotionValue(16421);
			pot.shoot(0D, 0.2D, 0D, 0F, 0F);
			final PacketPlayOutSpawnEntity packet = new PacketPlayOutSpawnEntity(pot, 73, 16421);
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	public void updateHealth(final Player p, final double health) {
		watcher = new DataWatcher(null);
		watcher.a(6, (float) health);
		final PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(entityID, watcher, false);
		((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
	}
	public void playDead(final Player p) {
		try {
			final PacketPlayOutEntityStatus packet = new PacketPlayOutEntityStatus();
			setValue(packet, "a", entityID);
			setValue(packet, "b", (byte) 3);
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	public void playSound(final Player p, final String s) {
		final Location l = getLocation();
		final PacketPlayOutNamedSoundEffect packet = new PacketPlayOutNamedSoundEffect(s, l.getX(), l.getY(), l.getZ(), 1F, 1F);
		((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
	}
	public void damage(final LivingEntity e, final double damage) {
		if (e instanceof Player == false) return;
		final Player p = (Player) e;
		if (p.getGameMode() != GameMode.CREATIVE) {
			p.damage(damage);
		}
	}
	public void removeFromTablist(final Player p) {
		try {
			final PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.REMOVE_PLAYER);
			final PacketPlayOutPlayerInfo.PlayerInfoData data = packet.new PlayerInfoData(this.profile, -1, null, null);
			final List<PacketPlayOutPlayerInfo.PlayerInfoData> players = (List<PacketPlayOutPlayerInfo.PlayerInfoData>) this.getValue(packet, "b");
			players.add(data);
			this.setValue(packet, "b", players);
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	public void changePlayerlistName(final Player p, final String name) {
		try {
			final PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_DISPLAY_NAME);
			final PacketPlayOutPlayerInfo.PlayerInfoData data = packet.new PlayerInfoData(this.profile, 0, WorldSettings.EnumGamemode.NOT_SET, CraftChatMessage.fromString(name)[0]);
			final List<PacketPlayOutPlayerInfo.PlayerInfoData> players = (List<PacketPlayOutPlayerInfo.PlayerInfoData>) this.getValue(packet, "b");
			players.add(data);
			this.setValue(packet, "b", players);
			this.tablist = name;
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	private void addToTablist(final Player p) {
		try {
			final PacketPlayOutPlayerInfo packet = new PacketPlayOutPlayerInfo();
			final GameProfile profile = this.profile = this.getProfile();
			final PacketPlayOutPlayerInfo.PlayerInfoData data = packet.new PlayerInfoData(profile, 1, WorldSettings.EnumGamemode.NOT_SET, CraftChatMessage.fromString(tablist)[0]);
			final List<PacketPlayOutPlayerInfo.PlayerInfoData> players = (List<PacketPlayOutPlayerInfo.PlayerInfoData>) getValue(packet, "b");
			players.add(data);
			setValue(packet, "a", PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER);
			setValue(packet, "b", players);
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	public void setItemInHand(final Player p, final Material material, final short data) {
		try {
			final PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment();
			this.setValue(packet, "a", this.entityID);
			this.setValue(packet, "b", 0);
			this.setValue(packet, "c", material == Material.AIR || material == null ? CraftItemStack.asNMSCopy(new ItemStack(Material.AIR)) : CraftItemStack.asNMSCopy(new ItemStack(material, 1, data)));
			this.inHand = material;
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public Material getItemInHand() {
		return this.inHand;
	}
	public void setHelmet(final Player p, final Material material) {
		try {
			final PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment();
			this.setValue(packet, "a", this.entityID);
			this.setValue(packet, "b", 4);
			this.setValue(packet, "c", material == Material.AIR || material == null ? CraftItemStack.asNMSCopy(new ItemStack(Material.AIR)) : CraftItemStack.asNMSCopy(new ItemStack(material)));
			this.helmet = material;
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	public Material getHelmet() {
		return this.helmet;
	}
	public void setChestplate(final Player p, final Material material) {
		try {
			final PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment();
			this.setValue(packet, "a", this.entityID);
			this.setValue(packet, "b", 3);
			this.setValue(packet, "c", material == Material.AIR || material == null ? CraftItemStack.asNMSCopy(new ItemStack(Material.AIR)) : CraftItemStack.asNMSCopy(new ItemStack(material)));
			this.chestplate = material;
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	public Material getChestplate() {
		return this.chestplate;
	}
	public void setLeggings(final Player p, final Material material) {
		try {
			final PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment();
			this.setValue(packet, "a", this.entityID);
			this.setValue(packet, "b", 2);
			this.setValue(packet, "c", material == Material.AIR || material == null ? CraftItemStack.asNMSCopy(new ItemStack(Material.AIR)) : CraftItemStack.asNMSCopy(new ItemStack(material)));
			this.leggings = material;
				((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	public Material getLeggings() {
		return this.leggings;
	}
	public void setBoots(final Player p, final Material material) {
		try {
			final PacketPlayOutEntityEquipment packet = new PacketPlayOutEntityEquipment();
			this.setValue(packet, "a", this.entityID);
			this.setValue(packet, "b", 1);
			this.setValue(packet, "c", material == Material.AIR || material == null ? CraftItemStack.asNMSCopy(new ItemStack(Material.AIR)) : CraftItemStack.asNMSCopy(new ItemStack(material)));
			this.boots = material;
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
	public Material getBoots() {
		return this.boots;
	}
	public int getEntityID() {
		return this.entityID;
	}
	public UUID getUUID() {
		return this.profile.getId();
	}
	public Location getLocation() {
		return this.loc;
	}
	public String getName() {
		return this.name;
	}
	public String getNameTag() {
		return this.nametag;
	}
	public String getPlayerlistName() {
		return this.tablist;
	}
	public void setAction(final PlayerAction action) {
		this.action = action;
	}
	public void executeAction(final Player p) {
		action.call(p);
	}
	private void setValue(final Object instance, final String field, final Object value){
		NMSReflection.setObject(instance.getClass(), field, instance, value);
	}
	private Object getValue(final Object instance, final String field) {
		return NMSReflection.getObject(instance.getClass(), field, instance);
	}
	private int toFixedPoint(final double d) {
		return (int) (d * 32.0);
	}
	private byte toPackedByte(final float f) {
		return (byte) (int) (f * 256.0F / 360.0F);
	}
	private GameProfile getProfile() {
		try {
			final GameProfile profile = GameProfileBuilder.fetch(UUIDFetcher.getUUID(ChatColor.stripColor(this.name)));
			final Field name = profile.getClass().getDeclaredField("name");
			name.setAccessible(true);
			name.set(profile, this.nametag);
			return profile;
		} catch (final Exception e) {
			e.printStackTrace();
			return getFakeProfile();
		}
	}
	private GameProfile getFakeProfile() {
		try {
			final GameProfile profile = GameProfileBuilder.fetch(UUIDFetcher.getUUID(ChatColor.stripColor(this.skinName)));
			final Field name = profile.getClass().getDeclaredField("name");
			name.setAccessible(true);
			name.set(profile, this.name);
			return profile;
		} catch (final Exception e) {
			e.printStackTrace();
			return new GameProfile(UUID.randomUUID(), this.name);
		}
	}
	private static class GameProfileBuilder {
		private static final String SERVICE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=false";
		private static final Gson gson = new GsonBuilder().disableHtmlEscaping().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).registerTypeAdapter(GameProfile.class, new GameProfileSerializer()).registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer()).create();
		private static final HashMap<UUID, CachedProfile> cache = new HashMap<>();
		private static final long cacheTime = -1;

		/**
		 * Don't run in main thread!
		 * Fetches the GameProfile from the Mojang servers
		 *
		 * @param uuid
		 *            The player uuid
		 * @return The GameProfile
		 * @throws IOException
		 *             If something wents wrong while fetching
		 * @see GameProfile
		 */
		public static GameProfile fetch(final UUID uuid) throws IOException {
			if (cache.containsKey(uuid) && cache.get(uuid).isValid()) {
				return cache.get(uuid).profile;
			}
			final URL url = new URL(String.format(SERVICE_URL, UUIDTypeAdapter.fromUUID(uuid)));
			final InputStreamReader reader = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8);
			final GameProfile result = gson.fromJson(new JsonParser().parse(reader), GameProfile.class);
			cache.put(uuid, new CachedProfile(result));
			return result;
		}
		/* No longer works
		public static GameProfile fetch(final UUID uuid) throws IOException {
			if (cache.containsKey(uuid) && cache.get(uuid).isValid()) {
				return cache.get(uuid).profile;
			} else {
				final HttpURLConnection connection = (HttpURLConnection) new URL(String.format(SERVICE_URL, UUIDTypeAdapter.fromUUID(uuid))).openConnection();
				connection.setReadTimeout(5000);
				if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
					final String json = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
					final GameProfile result = gson.fromJson(json, GameProfile.class);
					cache.put(uuid, new CachedProfile(result));
					return result;
				} else {
					if (cache.containsKey(uuid)) {
						return cache.get(uuid).profile;
					}
					final JsonObject error = (JsonObject) new JsonParser().parse(new BufferedReader(new InputStreamReader(connection.getErrorStream())).readLine());
					throw new IOException(error.get("error").getAsString() + ": " + error.get("errorMessage").getAsString());
				}
			}
		}
		*/
		private static class GameProfileSerializer implements JsonSerializer<GameProfile>, JsonDeserializer<GameProfile> {
			@Override
			public GameProfile deserialize(final JsonElement json, final Type type, final JsonDeserializationContext context) throws JsonParseException{
				final JsonObject object = (JsonObject) json;
				final UUID id = object.has("id") ? (UUID) context.deserialize(object.get("id"), UUID.class) : null;
				final String name = object.has("name") ? object.getAsJsonPrimitive("name").getAsString() : null;
				final GameProfile profile = new GameProfile(id, name);
				if (object.has("properties")) {
					for (final Map.Entry<String, Property> prop : ((PropertyMap) context.deserialize(object.get("properties"), PropertyMap.class)).entries()) {
						profile.getProperties().put(prop.getKey(), prop.getValue());
					}
				}
				return profile;
			}

			@Override
			public JsonElement serialize(final GameProfile profile, final Type type, final JsonSerializationContext context) {
				final JsonObject result = new JsonObject();
				if (profile.getId() != null)
					result.add("id", context.serialize(profile.getId()));
				if (profile.getName() != null)
					result.addProperty("name", profile.getName());
				if (profile.getProperties().isEmpty() == false)
					result.add("properties", context.serialize(profile.getProperties()));
				return result;
			}
		}
		private static class CachedProfile {
			private final long timestamp = System.currentTimeMillis();
			private final GameProfile profile;

			public CachedProfile(final GameProfile profile) {
				this.profile = profile;
			}

			public boolean isValid() {
				return cacheTime < 0 || System.currentTimeMillis() - timestamp < cacheTime;
			}
		}
	}
	public enum Animation {
		SWING_ARM(0),
		TAKE_DAMAGE(1),
		LEAVE_BED(2),
		EAT_FOOD(3),
		CRITICAL_EFFECT(4),
		MAGIC_CRITICAL_EFFECT(5);

		private final Integer value;
		Animation(final Integer value) {
			this.value = value;
		}
		public Integer getValue() {
			return value;
		}
	}
	private static class UUIDFetcher {
		private static final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
		private static final String UUID_URL = "https://api.mojang.com/users/profiles/minecraft/%s?at=%d";
		private static final Map<String, UUID> uuidCache = new HashMap<>();
		private static final Map<UUID, String> nameCache = new HashMap<>();
		private String name;
		private UUID id;

		public static UUID getUUID(String name) {
			name = name.toLowerCase();
			if (uuidCache.containsKey(name)) {
				return uuidCache.get(name);
			}
			try {
				final HttpURLConnection connection = (HttpURLConnection) new URL(String.format(UUID_URL, name, System.currentTimeMillis() / 1000)).openConnection();
				connection.setReadTimeout(5000);
				final UUIDFetcher data = gson.fromJson(new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)), UUIDFetcher.class);
				uuidCache.put(name, data.id);
				nameCache.put(data.id, data.name);
				return data.id;
			} catch (final Exception e) {}
			return null;
		}
	}
}