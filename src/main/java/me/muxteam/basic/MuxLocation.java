package me.muxteam.basic;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class MuxLocation {
	protected final double x, y, z;
	protected final int yaw, pitch;
	protected final String world;

	public MuxLocation(final String world, final double x, final double y, final double z, final int yaw, final int pitch) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
	}
	public MuxLocation(final String world, final double x, final double y, final double z) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = 0;
		this.pitch = 0;
	}

	public MuxLocation(final Location l) {
		this(l.getWorld().getName(), Math.floor(l.getX() * 100) / 100, Math.floor(l.getY() * 100) / 100, Math.floor(l.getZ() * 100) / 100, Math.round(l.getYaw()) % 360, Math.round(l.getPitch()) % 360);
	}

	public Location getLocation() {
		final World w = Bukkit.getWorld(world);
		if (w == null) return null;
		return new Location(w, x, y, z, yaw, pitch);
	}
	public double getX() {
		return x;
	}
	public double getY() {
		return y;
	}
	public double getZ() {
		return z;
	}
	public String getWorld() {
		return world;
	}
	@Override
	public boolean equals(final Object obj) {
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		final MuxLocation other = (MuxLocation) obj;

		if (other.world != null && (this.world == null || this.world.equals(other.world) == false)) {
			return false;
		} else if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(other.x)) {
			return false;
		} else if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(other.y)) {
			return false;
		} else if (Double.doubleToLongBits(this.z) != Double.doubleToLongBits(other.z)) {
			return false;
		} else if (Float.floatToIntBits(this.pitch) != Float.floatToIntBits(other.pitch)) {
			return false;
		} else return Float.floatToIntBits(this.yaw) == Float.floatToIntBits(other.yaw);
	}
	@Override
	public int hashCode() {
		int hash = 3;
		hash = 19 * hash + (this.world != null ? this.world.hashCode() : 0);
		hash = 19 * hash + (int) (Double.doubleToLongBits(this.x) ^ Double.doubleToLongBits(this.x) >>> 32);
		hash = 19 * hash + (int) (Double.doubleToLongBits(this.y) ^ Double.doubleToLongBits(this.y) >>> 32);
		hash = 19 * hash + (int) (Double.doubleToLongBits(this.z) ^ Double.doubleToLongBits(this.z) >>> 32);
		hash = 19 * hash + Float.floatToIntBits(this.pitch);
		hash = 19 * hash + Float.floatToIntBits(this.yaw);
		return hash;
	}
}