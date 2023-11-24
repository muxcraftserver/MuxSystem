package me.muxteam.casino;

import me.muxteam.basic.MuxFirework;
import me.muxteam.basic.MuxHolograms;
import me.muxteam.muxsystem.MuxInventory;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import java.util.*;

public class RouletteGame {
	private boolean runrandom;
	private int cooldown = 15, step = 0, chosen = 1;
	private final Location loc;
	private final Random r = new Random();
	private final ArmorStand pointer;
	private final List<Integer> rotateints = new ArrayList<>();
	private final List<Player> players = new ArrayList<>();
	public final Map<Player, Integer> stakes = new HashMap<>(), realstakes = new HashMap<>(), colorset = new HashMap<>();
	private final MuxHolograms.Hologram status;
	private final MuxCasino c;
	private boolean cancelled;


	public RouletteGame(final Location loc, final MuxCasino c) {
		this.loc = loc.clone();
		this.c = c;
		status = c.ms.getHolograms().addHologram("casinoroulettestatus" + loc.getBlockX() + loc.getBlockZ(), loc.clone().add(0, -0.2, 0), "§aWarte auf Spieler");
		pointer = (ArmorStand) loc.getWorld().spawnEntity(loc.clone().add(0.3, 0.35, 0), EntityType.ARMOR_STAND);
		pointer.setVisible(false);
		pointer.setGravity(false);
		pointer.setArms(true);
		pointer.setSmall(false);
		pointer.setItemInHand(new ItemStack(Material.BLAZE_ROD));
		pointer.setRightArmPose(new EulerAngle(0, 1, 0));
		for (int i = 0; i < 21; i++) {
			rotateints.add(i);
		}
		for (int i = 0; i < 12; i++) {
			if (i == 0) spawnWool(0.75, 0.1, 0.24, (byte) 14);
			else if (i == 1) spawnWool(0.75, 0.1, -0.24, (byte) 15);
			else if (i == 2) spawnWool(-0.75, 0.1, 0.24, (byte) 15);
			else if (i == 3) spawnWool(-0.75, 0.1, -0.24, (byte) 14);
			else if (i == 4) spawnWool(0.24, 0.1, 0.75, (byte) 14);
			else if (i == 5) spawnWool(0.24, 0.1, -0.75, (byte) 13);
			else if (i == 6) spawnWool(-0.24, 0.1, 0.75, (byte) 15);
			else if (i == 7) spawnWool(-0.24, 0.1, -0.75, (byte) 14);
			else if (i == 8) spawnWool(-0.68, 0.1, -0.68, (byte) 15);
			else if (i == 9) spawnWool(-0.68, 0.1, 0.68, (byte) 14);
			else if (i == 10) spawnWool(0.68, 0.1, 0.68, (byte) 15);
			else if (i == 11) spawnWool(0.68, 0.1, -0.68, (byte) 14);
		}
	}

	private void spawnWool(final double x, final double y, final double z, final byte data) {
		final ArmorStand as = (ArmorStand) loc.getWorld().spawnEntity(loc.clone().add(x, y, z), EntityType.ARMOR_STAND);
		as.setSmall(true);
		as.setHelmet(new ItemStack(Material.WOOL, 1, data));
		as.setGravity(false);
		as.setVisible(false);
	}

	public Location getLocation() {
		return loc;
	}

	public boolean isPlaying(final Player p) {
		return players.contains(p);
	}

	public void joinPlayer(final Player p) {
		if (players.contains(p) == false) {
			if (players.size() >= 8) {
				c.ms.showItemBar(p, "§cDieser Tisch ist voll besetzt.");
				p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
				return;
			}
			players.add(p);
		}
		if (runrandom) {
			c.ms.showItemBar(p, "§cDas Spiel läuft derzeit.");
			p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
			return;
		}
		int stake = stakes.getOrDefault(p, 0);
		if (stake == 0 || stake > c.getChips(p)) {
			stakes.put(p, 500);
		}
		openInv(p);
	}

	public void quit(final Player p) {
		stakes.remove(p);
		players.remove(p);
	}

	public void cancel() {
		cancelled = true;
	}

	public void onUpdate() {
		if (cancelled) return;
		step++;
		if (step >= 20) {
			step = 0;
			final Set<Player> remove = new HashSet<>();
			for (final Player p : players) {
				if (p == null || (p.isOnline() == false) || (p != c.ms.getServer().getPlayer(p.getUniqueId()))) continue;
				if (p.getWorld() != loc.getWorld() && realstakes.containsKey(p) == false) {
					remove.add(p);
					continue;
				}
				if ((p.getLocation().getWorld() == loc.getWorld() && p.getLocation().distance(loc) > 4 || p.isInsideVehicle() == false) && realstakes.containsKey(p) == false) {
					remove.add(p);
				}
			}
			for (final Player p : remove) {
				players.remove(p);
				c.ms.showItemBar(p, "§cDu hast den Roulettetisch verlassen.");
				p.closeInventory();
				stakes.remove(p);
			}
			remove.clear();
			boolean ready = false;
			for (Integer i : realstakes.values()) {
				if (i > 0) {
					ready = true;
					break;
				}
			}
			// Every Second
			if (ready && runrandom == false) {
				if (cooldown > 0) {
					cooldown = cooldown - 1;
					status.setMessage("§aDrehung in §7" + cooldown);
				} else{
					c.ms.getAnalytics().getAnalytics().addPlayedCasinoGame("Roulette");
					runrandom = true;
					new BukkitRunnable() {
						@Override public void run() {
							if (cancelled) return;
							rotateints.remove(20);
							rotateints.remove(18);
							rotateints.remove(16);
							rotateints.remove(14);
							rotateints.remove(12);
							rotateints.remove(10);
							rotateints.remove(8);
							rotateints.remove(6);
							rotateints.remove(4);
							rotateints.remove(2);
							rotateints.remove(0);
							new BukkitRunnable() {
								@Override public void run() {
									if (cancelled) return;
									rotateints.remove(8);
									rotateints.remove(6);
									rotateints.remove(4);
									rotateints.remove(2);
									rotateints.remove(0);
									new BukkitRunnable() {
										@Override public void run() {
											if (cancelled) return;
											rotateints.clear();
											runrandom = false;
											List<Integer> reds = new ArrayList<>(), black = new ArrayList<>(), green = new ArrayList<>();
											black.add(1);
											reds.add(2);
											reds.add(3);
											black.add(4);
											reds.add(5);
											reds.add(6);
											reds.add(7);
											reds.add(8);
											reds.add(9); // maybe green
											reds.add(10);
											reds.add(11);
											reds.add(12);
											black.add(13);
											black.add(14);
											black.add(15);
											green.add(16);
											black.add(17);
											black.add(18);
											black.add(19);
											int winner = chosen;
											int winningnumber;
											if (reds.contains(winner)) {
												MuxFirework.spawnColor(pointer.getLocation().clone().add(0, 1, 0), Color.RED);
												winningnumber = 1;
												c.getStats().ROULETTE_ROT += 1;
											} else if (black.contains(winner)) {
												MuxFirework.spawnColor(loc.clone(), Color.BLACK);
												winningnumber = 2;
												c.getStats().ROULETTE_BLACK += 1;
											} else if (green.contains(winner)) {
												MuxFirework.spawnColor(loc.clone(), Color.GREEN);
												winningnumber = 3;
												c.getStats().ROULETTE_GREEN += 1;
											} else {
												winningnumber = 1;
											}
											final Map<Player, Integer> toSave = new HashMap<>();
											for (final Map.Entry<Player, Integer> entry : colorset.entrySet()) {
												final Player p = entry.getKey();
												final Integer color = entry.getValue();
												if (p != null && color == winningnumber) {
													int summe = realstakes.get(p);
													if (reds.contains(winner)) {
														MuxFirework.spawnColor(pointer.getLocation().clone().add(0, 1, 0), Color.RED);
														winningnumber = 1;
														summe = (int) (summe * 1.8);
													} else if (black.contains(winner)) {
														MuxFirework.spawnColor(loc.clone(), Color.BLACK);
														winningnumber = 2;
														summe = (int) (summe * 2.35);
													} else if (green.contains(winner)) {
														MuxFirework.spawnColor(loc.clone(), Color.GREEN);
														winningnumber = 3;
														summe = summe * 19;
													}
													for (final Player pl : players) {
														if (pl != null && pl.isOnline()) {
															pl.sendMessage("§5§lMuxCasino> §d" + p.getName() + "§7 gewinnt Roulette §7(§d" + c.ms.getNumberFormat(summe) + " Chips§7)!");
														}
													}
													c.getStats().ROULETTE_VERLUST += summe;
													if ((p.isOnline() == false) || (c.inCasino(p) == false)) {
														toSave.put(p, summe);
													} else c.addChips(p, summe, "Roulette");
												}
											}

											if (toSave.isEmpty() == false) {
												new BukkitRunnable() {
													@Override
													public void run() {
														for (final Map.Entry<Player, Integer> entry : toSave.entrySet()) {
															if (entry.getKey().isOnline() && c.inCasino(entry.getKey())) {
																c.addChips(entry.getKey(), entry.getValue(), "Roulette");
																continue;
															}
															c.addChipsOffline(entry.getKey(), entry.getValue(), "Roulette");
														}
													}
												}.runTaskAsynchronously(c.ms);
											}
											reset();
										}
									}.runTaskLater(c.ms, 20L * (2L + r.nextInt(3)));
								}
							}.runTaskLater(c.ms, 20L * (1L + r.nextInt(2)));
						}
					}.runTaskLater(c.ms, 20L * (3L + r.nextInt(5)));
				}
			} else if (runrandom) {
				status.setMessage("§aAuflösung...");
				for (final Player p : players) {
					p.closeInventory();
				}
			} else if (players.isEmpty() == false && players.size() < 8) {
				status.setMessage("§aSpieler §7" + players.size() + " / 8");
			} else if (players.isEmpty()) {
				status.setMessage("§aWarte auf Spieler");
			}
		}
		if (runrandom && rotateints.contains(step)) {
			chosen++;
			if (chosen == 20) chosen = 1;
			pointer.setRightArmPose(new EulerAngle(0, chosen, 0));
			pointer.getWorld().playSound(pointer.getLocation(), Sound.CLICK, 1F, 10F);
		}
	}

	public void openInv(final Player p) {
		final Inventory inv = c.ms.getServer().createInventory(null, 27, "§0§lMuxCasino§0 | Roulette");
		final int set = colorset.getOrDefault(p, -1);
		ItemStack red = c.ms.renameItemStack(new ItemStack(Material.WOOL, 1, (byte) 14), "§c§lRot" + (set == 1 ? "§a §lausgewählt" : "")), black = c.ms.renameItemStack(new ItemStack(Material.WOOL, 1, (byte) 15), "§8§lSchwarz" + (set == 2 ? "§a §lausgewählt" : "")), green = c.ms.renameItemStack(new ItemStack(Material.WOOL, 1, (byte) 13), "§a§lGrün" + (set == 3 ? "§a §lausgewählt" : ""));
		for (final Player pl : players) {
			final int pset = colorset.getOrDefault(pl, -1), stake = realstakes.getOrDefault(pl, 0);
			if (pset == 1) red = c.ms.addLore(red, "§f" + pl.getName() + " - §d" + c.ms.getNumberFormat(stake) + " Chips");
			else if (pset == 2) black = c.ms.addLore(black, "§f" + pl.getName() + " - §d" + c.ms.getNumberFormat(stake) + " Chips");
			else if (pset == 3) green = c.ms.addLore(green, "§f" + pl.getName() + " - §d" + c.ms.getNumberFormat(stake) + " Chips");
		}
		red = c.ms.addLore(red, "", "§7Gewinnchance: §f53%", "§7Gewinn: §c1.8x", "", "§7Klicke, um auf Rot zu §csetzen§7.");
		black = c.ms.addLore(black, "", "§7Gewinnchance: §f42%", "§7Gewinn: §c2.35x", "", "§7Klicke, um auf Schwarz zu §8setzen§7.");
		green = c.ms.addLore(green, "", "§7Gewinnchance: §f5%", "§7Gewinn: §c19x", "", "§7Klicke, um auf Grün zu §asetzen§7.");
		inv.setItem(11, set == 1 ? c.ms.addGlow(red) : red);
		inv.setItem(12, set == 2 ? c.ms.addGlow(black) : black);
		inv.setItem(13, set == 3 ? c.ms.addGlow(green) : green);
		if (set == -1) {
			final int stake = stakes.get(p);
			inv.setItem(15, c.ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 13), "§f§lEinsatz: §d§l" + c.ms.getNumberFormat(stake) + " Chips", "", "§aLinksklick§7, für §d+" + c.ms.getNumberFormat(c.getNextStakeChange(stake, true)) + " Chips", "§cRechtsklick§7, für §d-" + c.ms.getNumberFormat(c.getNextStakeChange(stake, false)) + " Chips"));
		}
		if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
		p.openInventory(inv);
		c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
	}

	private void reset() {
		players.removeIf(player -> player == null || player.isOnline() == false || player != c.ms.getServer().getPlayer(player.getUniqueId()));
		cooldown = 15;
		runrandom = false;
		colorset.clear();
		for (int i = 0; i < 21; i++) {
			rotateints.add(i);
		}
		realstakes.clear();
	}
}