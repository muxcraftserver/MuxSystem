package me.muxteam.casino;

import me.muxteam.basic.MuxFirework;
import me.muxteam.basic.MuxHolograms;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Sign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WheelOfFortune {
	private boolean running = false, winning = false;
	private int amount = 50000;
	private Player player = null;
	private Location loc;
	private MuxHolograms.Hologram jackpot = null;
	private final List<ArmorStand> acircle = new ArrayList<>();
	private final Random r = new Random();
	private final MuxCasino c;

	public WheelOfFortune(final MuxCasino c) {
		this.c = c;
		setup();
	}

	public void close() {
		if (player != null) {
			c.addChipsOffline(player, 10000, "Glücksrad");
			c.getStats().GRAD_GEWINN -= 10000;
			amount -= 5000;
		}
	}

	private void setup() {
		loc = new Location(c.getWorld(), 21.5, 9, -154.5);
		acircle.clear();
		for (int i = 0; i < 12; i++) {
			Location spawn;
			if (i == 0) spawn = loc.clone().add(0.5, 2, 0);
			else if (i == 1) spawn = loc.clone().add(0, 2, 0);
			else if (i == 2) spawn = loc.clone().add(-0.5, 2, 0);
			else if (i == 3) spawn = loc.clone().add(-1, 1.5, 0);
			else if (i == 4) spawn = loc.clone().add(-1, 1, 0);
			else if (i == 5) spawn = loc.clone().add(-1, 0.5, 0);
			else if (i == 6) spawn = loc.clone().add(-0.5, 0, 0);
			else if (i == 7) spawn = loc.clone().add(-0, 0, 0);
			else if (i == 8) spawn = loc.clone().add(0.5, 0, 0);
			else if (i == 9) spawn = loc.clone().add(1, 0.5, 0);
			else if (i == 10) spawn = loc.clone().add(1, 1, 0);
			else spawn = loc.clone().add(1, 1.5, 0);
			spawn = spawn.clone().add(0, -0.2, 0);
			spawn.setYaw(-90);
			final ArmorStand a = (ArmorStand) loc.getWorld().spawnEntity(spawn, EntityType.ARMOR_STAND);
			a.setSmall(true);
			a.setGravity(false);
			a.setVisible(false);
			a.setHelmet(new ItemStack(Material.CLAY, 0));
			acircle.add(a);
			if (i == 1) {
				a.setCustomName("§a§l⬇");
				a.setCustomNameVisible(true);
				a.setHelmet(new ItemStack(Material.EMERALD_BLOCK));
			}
		}
		final Location dloc = new Location(c.getWorld(), 25.5, 9, -158.5);
		c.ms.getHolograms().addHologram("crashwheel", dloc.clone(), "§fGlücksrad §a§lJACKPOT§f:");
		jackpot = c.ms.getHolograms().addHologram("crashwheelinfo", dloc.clone().add(0, -0.4, 0), "§cWird geladen...");
	}
	public void onUpdate() {
		final String msg = "§d" + c.ms.getNumberFormat(amount) + " Chips";
		if (jackpot != null && jackpot.getMessage().equals(msg) == false)
			jackpot.setMessage(msg);
		if (running && winning == false) {
			final ItemStack head1 = acircle.get(0).getHelmet().clone();
			acircle.get(0).setHelmet(acircle.get(11).getHelmet());
			acircle.get(11).setHelmet(acircle.get(10).getHelmet());
			acircle.get(10).setHelmet(acircle.get(9).getHelmet());
			acircle.get(9).setHelmet(acircle.get(8).getHelmet());
			acircle.get(8).setHelmet(acircle.get(7).getHelmet());
			acircle.get(7).setHelmet(acircle.get(6).getHelmet());
			acircle.get(6).setHelmet(acircle.get(5).getHelmet());
			acircle.get(5).setHelmet(acircle.get(4).getHelmet());
			acircle.get(4).setHelmet(acircle.get(3).getHelmet());
			acircle.get(3).setHelmet(acircle.get(2).getHelmet());
			acircle.get(2).setHelmet(acircle.get(1).getHelmet());
			acircle.get(1).setHelmet(head1);
			loc.getWorld().playSound(loc, Sound.CLICK, 1F, 10F);
		}
	}
	public void onInteract(final Player p, final Sign s) {
		if (s.getLine(0).equalsIgnoreCase("[jackpot]") || s.getLine(0).equalsIgnoreCase("§1§lJACKPOT")) {
			s.setLine(0, "§1§lJACKPOT");
			s.setLine(1, "§8-*-");
			s.setLine(2, "§l10.000 Chips");
			s.setLine(3, "Klicke zum Drehen");
			s.update();
			if (player != null) {
				if (player.equals(p) == false) {
					c.ms.showItemBar(p, "§cJemand anderes ist gerade am Zug.");
					p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
				} else {
					c.ms.showItemBar(p, "§cWarte bis deine Runde zuende ist.");
				}
				return;
			} else if (c.canBuy(p, 10000, "Glücksrad") == false) {
				return;
			}
			c.getStats().GRAD_GEWINN += 10000;
			c.ms.getAnalytics().getAnalytics().addPlayedCasinoGame("Wheel Of Fortune");
			player = p;
			running = true;
			c.ms.showItemBar(p, "§aEinsatz von §d10.000 Chips §agesetzt.");
			loc.getWorld().playSound(loc, Sound.NOTE_PLING, 1F, 1F);
			amount = amount + 5000;
			new BukkitRunnable() {
				@Override
				public void run() {
					running = false;
					winning = true;
					if (acircle.get(1).getHelmet().getType() == Material.EMERALD_BLOCK) {
						c.getStats().GRAD_VERLUST += amount;
						loc.getWorld().playSound(loc, Sound.LEVEL_UP, 1F, 1F);
						c.moneyRain(Material.EMERALD, loc.clone(), Material.EMERALD_BLOCK);
						c.getStats().GRAD_RUNDEN_GEWONNEN += 1;
						if (p.isOnline() && c.inCasino(p)) {
							c.addChips(p, amount, "Glücksrad");
							c.broadcast("§5§lMuxCasino>§d " + p.getName() + "§7 gewinnt beim §a§lGlücksrad §7(§d" + c.ms.getNumberFormat(amount) + " Chips§7)!");
							MuxFirework.spawnColor(p.getLocation(), Color.GREEN);
						} else {
							c.addChipsOffline(p, amount, "Glücksrad");
						}
						amount = 50000;
					} else {
						loc.getWorld().playSound(loc, Sound.NOTE_BASS_GUITAR, 1F, 1F);
						if (p.isOnline()) c.ms.showItemBar(p, "§cLeider nicht gewonnen.");
					}
					new BukkitRunnable() {
						@Override public void run() {
							winning = false;
							player = null;
							loc.getWorld().playSound(loc, Sound.NOTE_STICKS, 1F, 1F);
						}
					}.runTaskLater(c.ms, 20L);
				}
			}.runTaskLater(c.ms, 20L * (2L + r.nextInt(2) + r.nextInt(3)));
		}
	}
}