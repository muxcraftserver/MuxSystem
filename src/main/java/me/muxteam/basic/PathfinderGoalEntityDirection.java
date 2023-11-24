package me.muxteam.basic;

import net.minecraft.server.v1_8_R3.EntityInsentient;
import net.minecraft.server.v1_8_R3.PathfinderGoal;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class PathfinderGoalEntityDirection extends PathfinderGoal {

	/**
	 *  @author Paul Gschwendtner <paulgschwendtner@me.com>
	 *  @version 0.1
	 *  @category Minecraft.NET Packet Interception Library
	 */

	EntityInsentient e;
	BlockFace bf;

	public PathfinderGoalEntityDirection(final EntityInsentient e, final BlockFace bf) {
		this.bf = bf;
		this.e = e;
	}

	@Override
	public boolean a() {
		return true;
	}

	@Override
	public void e() {
		final Location l = new Location(e.world.getWorld(), e.locX, e.locY, e.locZ).getBlock().getRelative(bf).getLocation();
		e.getControllerLook().a(l.getBlockX() + .5, e.locY + e.getHeadHeight(), l.getBlockZ() + .5, 10F, this.e.bQ());
	}
}