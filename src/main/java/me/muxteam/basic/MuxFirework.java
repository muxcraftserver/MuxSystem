package me.muxteam.basic;

import net.minecraft.server.v1_8_R3.EntityFireworks;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityStatus;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.Random;

public class MuxFirework extends EntityFireworks {
	final Player[] players;
	static final Random r = new Random();

	public MuxFirework(final net.minecraft.server.v1_8_R3.World world, final Player... p) {
		super(world);
		this.players = p;
		this.a(0.25F, 0.25F);
	}

	boolean gone = false;

	@Override
	public void t_() {
		if (gone) {
			return;
		}
		if (this.world.isClientSide == false) {
			gone = true;
			if (players != null) {
				if (players.length > 0) {
					for (final Player player : players) {
						((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityStatus(this, (byte) 17));
					}
				} else world.broadcastEntityEffect(this, (byte) 17);
			}
			this.die();
		}
	}

	public static void spawnRandomBigBall(final Location loc) {
		final FireworkEffect.Builder builder = FireworkEffect.builder();
		builder.with(Type.BALL_LARGE);
		FireworkEffect effect = null;
		final int zufall = r.nextInt(8);
		switch (zufall) {
			case 0:
				effect = builder.flicker(true).trail(true).withColor(Color.AQUA).build();
				break;
			case 1:
				effect = builder.flicker(true).trail(true).withColor(Color.BLACK).build();
				break;
			case 2:
				effect = builder.flicker(true).trail(true).withColor(Color.GREEN).build();
				break;
			case 3:
				effect = builder.flicker(true).trail(true).withColor(Color.GRAY).build();
				break;
			case 4:
				effect = builder.flicker(true).trail(true).withColor(Color.RED).build();
				break;
			case 5:
				effect = builder.flicker(true).trail(true).withColor(Color.ORANGE).build();
				break;
			case 6:
				effect = builder.flicker(true).trail(true).withColor(Color.BLUE).build();
				break;
			case 7:
				effect = builder.flicker(true).trail(true).withColor(Color.YELLOW).build();
				break;
		}
		MuxFirework.spawn(loc.add(0.5, 1, 0.5), effect);
	}
	public static void spawnCreeper(final Location loc) {
		final FireworkEffect.Builder builder = FireworkEffect.builder();
		builder.with(Type.CREEPER);
		final FireworkEffect effect = builder.flicker(false).trail(true).withColor(Color.GREEN).withColor(Color.PURPLE).build();
		MuxFirework.spawn(loc.add(0.5, 1, 0.5), effect);
	}
	public static void spawnColor(final Location loc, final Color c) {
		final FireworkEffect.Builder builder = FireworkEffect.builder();
		builder.with(Type.BALL);
		final FireworkEffect effect = builder.flicker(false).trail(true).withColor(c).build();
		MuxFirework.spawn(loc.add(0.5, 1, 0.5), effect);
	}
	public static void spawnBigGreen(final Location loc) {
		final FireworkEffect.Builder builder = FireworkEffect.builder();
		builder.with(Type.BALL_LARGE);
		final FireworkEffect effect = builder.flicker(false).trail(true).withColor(Color.LIME).build();
		MuxFirework.spawn(loc.clone().add(0.5, 1, 0.5), effect);
	}
	public static void spawnRandom(final Location loc) {
		final FireworkEffect.Builder builder = FireworkEffect.builder();
		builder.with(Type.BALL);
		FireworkEffect effect = null;
		int zufall = r.nextInt(8);
		switch (zufall) {
			case 0:
				effect = builder.flicker(false).trail(true).withColor(Color.AQUA).build();
				break;
			case 1:
				effect = builder.flicker(false).trail(true).withColor(Color.LIME).build();
				break;
			case 2:
				effect = builder.flicker(false).trail(true).withColor(Color.GREEN).build();
				break;
			case 3:
				effect = builder.flicker(false).trail(true).withColor(Color.FUCHSIA).build();
				break;
			case 4:
				effect = builder.flicker(false).trail(true).withColor(Color.RED).build();
				break;
			case 5:
				effect = builder.flicker(false).trail(true).withColor(Color.ORANGE).build();
				break;
			case 6:
				effect = builder.flicker(false).trail(true).withColor(Color.BLUE).build();
				break;
			case 7:
				effect = builder.flicker(false).trail(true).withColor(Color.YELLOW).build();
				break;
		}
		MuxFirework.spawn(loc.add(0.5, 1, 0.5), effect);

	}
	public static void spawn(final Location location, final FireworkEffect effect, final Player... players) {
		try {
			final MuxFirework firework = new MuxFirework(((CraftWorld) location.getWorld()).getHandle(), players);
			final FireworkMeta meta = ((Firework) firework.getBukkitEntity()).getFireworkMeta();
			meta.addEffect(effect);
			((Firework) firework.getBukkitEntity()).setFireworkMeta(meta);
			firework.setPosition(location.getX(), location.getY(), location.getZ());
			if (((CraftWorld) location.getWorld()).getHandle().addEntity(firework)) {
				firework.setInvisible(true);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}