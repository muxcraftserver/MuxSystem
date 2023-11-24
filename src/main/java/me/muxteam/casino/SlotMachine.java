package me.muxteam.casino;

import me.muxteam.basic.MuxFirework;
import org.bukkit.ChatColor;
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
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class SlotMachine {
	private final Set<Location> sign = new HashSet<>();
	private final List<ArmorStand> slots = new ArrayList<>();
	private final List<Material> type1 = new ArrayList<>(), type2 = new ArrayList<>(), type3 = new ArrayList<>(),
			type4 = new ArrayList<>(), type5 = new ArrayList<>();
	private final Random r = new Random();
	private final MuxCasino c;
	private final Map<Player, Integer> stakes = new HashMap<>();

	public SlotMachine(final MuxCasino c) {
		this.c = c;
		setup();
	}
	public void close() {
		for (final ArmorStand a : slots) {
			a.remove();
		}
		stakes.forEach((p, chips) -> {
			if (c.inCasino(p))
				c.addChips(p, chips, "Slot Machine");
			else
				c.addChipsOffline(p, chips, "Slot Machine");
			c.getStats().SLOT_GEWINN -= chips;
		});
	}
	public void onUpdate() {
		for (final ArmorStand a : slots) {
			a.setItemInHand(getRandomMat(Integer.parseInt(a.getCustomName())));
			a.getWorld().playSound(a.getLocation(), Sound.NOTE_STICKS, 0.5F, 0.1F);
		}
	}
	public void onInteract(final Player p, final Sign s) {
		if (s.getLine(0).equalsIgnoreCase("[slot]") || s.getLine(0).equalsIgnoreCase("§1§lSLOT MACHINE") || s.getLine(0).equalsIgnoreCase("§9§lSLOT MACHINE")) {
			if (sign.contains(s.getLocation())) {
				c.ms.showItemBar(p, "§cAutomat ist derzeit belegt.");
				p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
				return;
			} else if (p.isInsideVehicle() == false) {
				c.ms.showItemBar(p, "§cDu musst dich zuerst hinsetzen.");
				p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
				return;
			}
			final int line = s.getLine(0).startsWith("§9") ? 3 : 2;
			int chips = Integer.parseInt(ChatColor.stripColor(s.getLine(line).replace("Chips", "").replace(" ", "")));
			s.setLine(0, "§1§lSLOT MACHINE");
			s.setLine(1, "§8-*-");
			s.setLine(2, "§l" + chips + " Chips");
			s.setLine(3, "Klicke zum Spielen");
			s.update();
			int t = 0;
			if (chips == 10000) t = 1;
			if (chips == 250) t = 2;
			if (chips == 500) t = 3;
			if (chips == 5000) t = 4;
			if (chips == 5) t = 5;
			final int type = t;
			s.update();
			final int preis = Integer.parseInt(s.getLine(2).replace("§l", "").replace(" Chips", ""));
			if (c.canBuy(p, preis, "Slot Machine") == false) {
				return;
			}
			stakes.put(p, preis);
			c.ms.getAnalytics().getAnalytics().addPlayedCasinoGame("Slots");
			c.getStats().SLOT_GEWINN += preis;
			final Location l = s.getLocation();
			sign.add(l);
			final ArmorStand a1 = addSlot(s, type, 0, 0.72, 0.3), a2 = addSlot(s, type, 1, 0.51, 0.51), a3 = addSlot(s, type, 2, 0.3, 0.72);
			new BukkitRunnable() {
				@Override
				public void run() {
					slots.remove(a1);
					l.getWorld().playSound(l, Sound.CLICK, 1F, 5F);
					new BukkitRunnable() {
						@Override
						public void run() {
							slots.remove(a2);
							l.getWorld().playSound(l, Sound.CLICK, 1F, 5F);
							new BukkitRunnable() {
								@Override
								public void run() {
									slots.remove(a3);
									l.getWorld().playSound(l, Sound.CLICK, 1F, 5F);
									new BukkitRunnable() {
										@Override
										public void run() {
											boolean won = false;
											if (a1.getItemInHand() != null) {
												final Material m = a1.getItemInHand().getType();
												if (a2.getItemInHand().getType() == m && a3.getItemInHand().getType() == m) {
													won = true;
												}
											}
											c.getStats().SLOT_RUNDEN_GESPIELT += 1;
											if (won) {
												l.getWorld().playSound(l, Sound.LEVEL_UP, 1F, 10F);
												MuxFirework.spawnColor(l, Color.GREEN);
												c.getStats().SLOT_RUNDEN_GEWONNEN += 1;
												final int win = preis * (type == 1 || type == 4 ? 24 : 15);
												c.getStats().SLOT_VERLUST += win;
												if (p.isOnline() && c.inCasino(p)) {
													c.addChips(p, win, "Slot Machine");
													c.ms.showItemBar(p, "§aDu hast gewonnen! §d(+" + c.ms.getNumberFormat(win) + " Chips)");
												} else {
													c.addChipsOffline(p, win, "Slot Machine");
												}
											} else {
												l.getWorld().playSound(l, Sound.NOTE_BASS_GUITAR, 1F, 1F);
											}
											stakes.remove(p);
											a1.remove();
											a2.remove();
											a3.remove();
											sign.remove(s.getLocation());
										}
									}.runTaskLater(c.ms, 20L);
								}
							}.runTaskLater(c.ms, 20L);
						}
					}.runTaskLater(c.ms, 30L);
				}
			}.runTaskLater(c.ms, 40L);
		}
	}
	private void setup() {
		type1.add(Material.COAL_BLOCK);
		type1.add(Material.DIAMOND_BLOCK);
		type1.add(Material.REDSTONE_BLOCK);
		type1.add(Material.LAPIS_BLOCK);
		type1.add(Material.EMERALD_BLOCK);
		type2.add(Material.SAND);
		type2.add(Material.RED_SANDSTONE);
		type2.add(Material.GRAVEL);
		type2.add(Material.GOLD_BLOCK);
		type3.add(Material.GRASS);
		type3.add(Material.LEAVES);
		type3.add(Material.CACTUS);
		type3.add(Material.HAY_BLOCK);
		type4.add(Material.OBSIDIAN);
		type4.add(Material.BEDROCK);
		type4.add(Material.SEA_LANTERN);
		type4.add(Material.EMERALD_ORE);
		type4.add(Material.SOUL_SAND);
		type5.add(Material.PRISMARINE);
		type5.add(Material.PACKED_ICE);
		type5.add(Material.BEACON);
		type5.add(Material.LAPIS_ORE);
	}
	private ArmorStand addSlot(final Sign s, final int type, final double posa, final double posb, final double posc) {
		final Location l = s.getLocation();
		final byte data = s.getData().getData();
		Location loc = null;
		if (data == (byte) 4) {
			loc = l.clone().add(0.7, 1, 0.3 + 0.21 * posa);
			loc.setYaw(90);
		} else if (data == (byte) 2) {
			loc = l.clone().add(posb, 1, 0.7);
			loc.setYaw(180);
		} else if (data == (byte) 3) {
			loc = l.clone().add(posc, 1, 0.3);
			loc.setYaw(0);
		} else if (data == (byte) 5) {
			loc = l.clone().add(0.3, 1, posb);
			loc.setYaw(-90);
		}
		final ArmorStand a = (ArmorStand) l.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
		a.setVisible(false);
		a.setGravity(false);
		a.setSmall(true);
		a.setItemInHand(new ItemStack(Material.STONE, 1, (byte) 10));
		a.setRightArmPose(new EulerAngle(50, 10.2, 0));
		a.setCustomName(String.valueOf(type));
		slots.add(a);
		return a;
	}
	private ItemStack getRandomMat(final int type) {
		if (type == 1) {
			return new ItemStack(type1.get(r.nextInt(type1.size())));
		} else if (type == 2) {
			return new ItemStack(type2.get(r.nextInt(type2.size())));
		} else if (type == 3) {
			return new ItemStack(type3.get(r.nextInt(type3.size())));
		} else if (type == 4) {
			return new ItemStack(type4.get(r.nextInt(type4.size())));
		} else if (type == 5) {
			return new ItemStack(type5.get(r.nextInt(type5.size())));
		}
		return new ItemStack(Material.BARRIER);
	}
}