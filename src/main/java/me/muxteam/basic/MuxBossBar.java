package me.muxteam.basic;

import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public final class MuxBossBar {
	private MuxSystem ms;
	private String bosstitle;
	private final ConcurrentHashMap<String, EntityWither> withers = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, String> titles = new ConcurrentHashMap<>();

	public MuxBossBar(final MuxSystem ms) {
		this.ms = ms;
	}
	public void close() {
		for (final Player pl : ms.getServer().getOnlinePlayers()) {
			removePlayer(pl);
		}
		this.ms = null;
	}
	public void loadConfig(final FileConfiguration config) {
		this.bosstitle = config.getString("bossbar") + (ms.isBeta() ? " §3§lbeta" : "");
	}
	public void sendBossBar() {
		for (final Entry<String, EntityWither> entry : withers.entrySet()) {
			final EntityWither wither = entry.getValue();
			final Player p = ms.getServer().getPlayer(entry.getKey());
			if (p == null) {
				withers.remove(entry.getKey());
				titles.remove(entry.getKey());
				continue;
			}
			final Location l = getWitherLocation(p.getLocation());
			wither.setLocation(l.getX(), l.getY(), l.getZ(), 0, 0);
			final PacketPlayOutEntityTeleport packet = new PacketPlayOutEntityTeleport(wither);
			((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
		}
	}
	public void addPlayer(final Player p) {
		if (withers.containsKey(p.getName())) return;
		final EntityWither wither = new EntityWither(((CraftWorld) p.getWorld()).getHandle());
		final Location l = getWitherLocation(p.getLocation());
		final String t = titles.get(p.getName());
		wither.setCustomName(t == null ? bosstitle : t);
		wither.setInvisible(true);
		wither.setLocation(l.getX(), l.getY(), l.getZ(), 0, 0);
		final PacketPlayOutSpawnEntityLiving packet = new PacketPlayOutSpawnEntityLiving(wither);
		((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
		withers.put(p.getName(), wither);
	}
	public void removePlayer(final Player p) {
		final EntityWither wither = withers.remove(p.getName());
		if (wither == null) return;
		final PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(wither.getId());
		((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
	}
	public void setTitle(final Player p, final String title) {
		titles.put(p.getName(), title);
		final EntityWither wither = withers.get(p.getName());
		if (wither == null) return;
		wither.setCustomName(title);
		wither.setHealth(wither.getMaxHealth());
		final PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(wither.getId(), wither.getDataWatcher(), true);
		((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
	}
	private Location getWitherLocation(final Location l) {
		return l.add(l.getDirection().multiply(23));
	}
}