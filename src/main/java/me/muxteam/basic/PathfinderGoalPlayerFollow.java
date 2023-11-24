package me.muxteam.basic;

import net.minecraft.server.v1_8_R3.*;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;

public class PathfinderGoalPlayerFollow extends PathfinderGoal {

	/**
	 *  @author Paul Gschwendtner <paulgschwendtner@me.com>
	 *  @version 0.1
	 *  @category Minecraft.NET Packet Interception Library
	 */

	EntityInsentient entity;
	Player p;
	double speed;

	public PathfinderGoalPlayerFollow(final EntityInsentient entity, final Player p, final double speed) {
		this.entity = entity;
		this.p = p;
		this.speed = speed;
		if (this.entity.getNavigation() instanceof NavigationGuardian) {
			try {
				final Field navigationField = EntityInsentient.class.getDeclaredField("navigation");
				navigationField.setAccessible(true);
				navigationField.set(this.entity, new Navigation(this.entity, this.entity.world));
			} catch (final NoSuchFieldException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean a() {
		if (p.getLocation().distance(new Location(p.getWorld(), entity.locX, entity.locY, entity.locZ)) > 30) {
			entity.setLocation(p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ(), 0F, 0F);
			return false;
		}
		return true;
	}
	@Override
	public boolean b() {
		return false;
	}
	@Override
	public void e() {
		if (p.getLocation().distance(new Location(p.getWorld(), entity.locX, entity.locY, entity.locZ)) < 3) {
			return;
		}
		final Location l = p.getLocation();
		final PathEntity pe = this.entity.getNavigation().a(l.getX() + 1.45, l.getY(), l.getZ() + 1);
		this.entity.getNavigation().a(pe, speed);
	}
}