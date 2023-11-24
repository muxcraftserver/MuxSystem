package me.muxteam.casino;

import me.muxteam.basic.MuxFirework;
import me.muxteam.basic.MuxHolograms;
import me.muxteam.muxsystem.MuxInventory;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.*;

public class Crash {
	private boolean winning = false, running = false, warmup = false;
	private int chartstep = 0, delay = 0, cooldown = 21;
	private double multi = 0D;
	private Location loc, displayloc, beginchart;
	private final Random r = new Random();
	private final Map<Player, Integer> stakes = new HashMap<>(), realstakes = new HashMap<>();
	private final Map<String, Double> gotout = new HashMap<>();
	private final List<Double> last5rounds = new ArrayList<>(), lastroundsfull = new ArrayList<>();
	private final Set<MuxHolograms.Hologram> lastrounds = new HashSet<>(), playerdisplay = new HashSet<>();
	private final List<ArmorStand> chart = new ArrayList<>();
	private final Set<Sign> signs = new HashSet<>();
	private final Set<Player> players = new HashSet<>();
	private MuxHolograms.Hologram status = null;
	private final MuxHolograms h;
	private final MuxCasino c;

	public Crash(final MuxCasino c) {
		this.c = c;
		this.h = c.ms.getHolograms();
		setup();
	}

	private void setup() {
		loc = new Location(c.getWorld(), 5.5, 11, -179.5);
		status = c.ms.getHolograms().addHologram("crashinfo", loc, "§cWird geladen...");
		displayloc = new Location(c.getWorld(), 8.5, 11, -179.5);
		beginchart = new Location(c.getWorld(), 2.5, 11, -179.8);
		int radius = 5;
		for (int x = displayloc.getBlockX() - radius; x <= displayloc.getBlockX() + radius; x++) {
			for (int y = displayloc.getBlockY() - radius; y <= displayloc.getBlockY() + radius; y++) {
				for (int z = displayloc.getBlockZ() - radius; z <= displayloc.getBlockZ() + radius; z++) {
					final Block block = displayloc.getWorld().getBlockAt(x, y, z);
					if (block != null && block.getState() instanceof Sign) {
						signs.add((Sign) block.getState());
					}
				}
			}
		}
	}

	private void updateArmorStands() {
		for (final MuxHolograms.Hologram holo : playerdisplay) {
			h.removeHologramsWithDisplayName(holo.getMessage());
		}
		playerdisplay.clear();
		int step = 0;
		if (players.isEmpty() == false) playerdisplay.add(h.addHologram("crashspieler", displayloc.clone().add(0, 0.4, 0), "§3Spieler:"));
		for (final Player p : players) {
			status.teleport(loc.clone().add(-2, 0, 0));
			for (final MuxHolograms.Hologram a : lastrounds) {
				final Location locis = status.getLocation().clone();
				locis.setY(a.getLocation().getY());
				a.teleport(locis);
			}
			String info = "";
			final int en = realstakes.getOrDefault(p, -1);
			final double exitmulti = gotout.getOrDefault(p.getName(), -1D);
			if (en != -1) {
				info = "§7" + p.getName() + ":§f " + en + " Chips";
			} else if (exitmulti != -1D) {
				info = "§a" + p.getName() + ": §7[" + getColor(exitmulti) + exitmulti + "x§7]";
			}
			playerdisplay.add(h.addHologram("crash-" + p.getName(), displayloc.clone().add(0, -0.4 * step, 0), info));
			step++;
			if (step > 10) break;
		}
	}

	private String getColor(final double multi) {
		String color = "§c";
		if (multi == 1.0) color = "§e";
		else if (multi > 1.0) color = "§a";
		return color;
	}

	private void showMultiplier() {
		status.setMessage(getColor(multi) + multi + "x");
		final String color = getColor(multi);
		final String[] lines = new String[] { color + "▛▀▀▀▀▀▀▀▀▜", "Klicke hier für", getColor(multi) + "§lCASHOUT", color + "▙▄▄▄▄▄▄▄▄▟"};
		setSigns(lines);
	}

	public void pushAway() {
		for (final Player p : c.getWorld().getPlayers()) {
			for (final Sign s : signs) {
				final Location sign = s.getLocation();
				if (p.getLocation().distance(sign) < 1.5D) {
					final Vector v = c.getSpawn().toVector().subtract(sign.toVector());
					p.setVelocity(v.normalize().multiply(1.1D));
				}
			}
		}
	}

	private void setSigns(final String[] lines) {
		for (final Sign sign : signs) {
			if (lines == null) {
				sign.update();
				continue;
			}
			final net.minecraft.server.v1_8_R3.World world = ((CraftWorld) sign.getWorld()).getHandle();
			final BlockPosition bs = new BlockPosition(sign.getX(), sign.getY(), sign.getZ());
			for (final Player p : players) {
				final EntityPlayer cp = ((CraftPlayer) p).getHandle();
				cp.playerConnection.sendPacket(new net.minecraft.server.v1_8_R3.PacketPlayOutUpdateSign(world, bs, c.ms.getComponentFromSignLines(lines)));
			}
		}
	}

	public void onUpdate() {
		delay++;
		if (delay >= 2) {
			delay = 0;
			if (warmup && cooldown > 0) cooldown--;
			if (warmup) {
				status.setMessage("§aNächste Runde startet in §f" + cooldown);
			} else {
				if (players.isEmpty() == false) {
					final Set<Player> remove = new HashSet<>();
					for (final Player p : players) {
						if ((p.getLocation().getWorld().equals(loc.getWorld()) == false || p.getLocation().distance(loc) > 9) && realstakes.containsKey(p) == false) {
							remove.add(p);
						}
					}
					for (final Player p : remove) {
						c.ms.showItemBar(p, "§cDu hast Crash verlassen.");
						players.remove(p);
					}
					if (remove.isEmpty() == false) {
						updateArmorStands();
					}
					remove.clear();
				} else {
					status.teleport(loc.clone());
					for (final MuxHolograms.Hologram a : lastrounds) {
						final Location locis = status.getLocation().clone();
						locis.setY(a.getLocation().getY());
						a.teleport(locis);
					}
					for (final MuxHolograms.Hologram a : playerdisplay) {
						h.removeHologramsWithDisplayName(a.getMessage());
					}
					playerdisplay.clear();
				}
				if (running) {
					showMultiplier();
				}
			}
			if (cooldown == 21 && warmup == false) {
				cooldown = 20;
				warmup = true;
				for (final MuxHolograms.Hologram a : lastrounds) {
					h.removeHologramsWithDisplayName(a.getMessage());
				}
				lastrounds.clear();
				lastrounds.add(h.addHologram("crashlist", status.getLocation().clone().add(0, -0.65, 0).add(0, multi, 0), "§fLetzte §dRunden"));
				int step = 0;
				for (final Double d : last5rounds) {
					step++;
					lastrounds.add(h.addHologram("crashlast" + step, status.getLocation().clone().add(0, -3.05 + (0.4 * step), 0).add(0, multi, 0), "§f[" + getColor(d) + d + "x§f]"));
				}
				new BukkitRunnable() {
					@Override
					public void run() {
						for (final MuxHolograms.Hologram a : lastrounds) {
							h.removeHologramsWithDisplayName(a.getMessage());
						}
						lastrounds.clear();
						updateArmorStands();
						warmup = false;
						if (running || winning) {
							return;
						}
						running = true;
						if (players.isEmpty() == false) c.ms.getAnalytics().getAnalytics().addPlayedCasinoGame("Crash");
						for (final Player p : players) {
							c.ms.showItemBar(p, "§aCrash Runde startet.");
							p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 10F);
						}
						new BukkitRunnable() {
							@Override
							public void run() {
								winning = true;
								running = false;
								for (final Player p : players) {
									if (realstakes.containsKey(p)) {
										if (multi == 1.0) MuxFirework.spawnColor(p.getLocation(), Color.YELLOW);
										if (multi > 1.0) MuxFirework.spawnColor(p.getLocation(), Color.GREEN);
										if (multi < 1.0) MuxFirework.spawnColor(p.getLocation(), Color.RED);
										p.closeInventory();
										c.ms.showItemBar(p, "§c§lCRASH!§c Du hast leider nichts gewonnen.");
										p.playSound(p.getLocation(), Sound.EXPLODE, 1F, 1F);
										gotout.put(p.getName(), c.round(0.0, 2));
										realstakes.remove(p);
									}
								}
								if (c.getStats().CRASH_HIGHEST < multi) c.getStats().CRASH_HIGHEST = multi;
								if (multi > 1.00) c.getStats().CRASH_RUNDEN_GEWONNEN += 1;
								else c.getStats().CRASH_RUNDEN_VERLUST += 1;
								lastroundsfull.add(multi);
								double stepi = 0.0;
								for (final Double d : lastroundsfull) {
									stepi += d;
								}
								c.getStats().CRASH_DURCHSCHNITT = stepi / lastroundsfull.size();
								updateArmorStands();
								setSigns(null);
								chart.get(chart.size() - 1).setItemInHand(new ItemStack(Material.REDSTONE_BLOCK));
								new BukkitRunnable() {
									@Override
									public void run() {
										last5rounds.add(multi);
										if (last5rounds.size() > 5) {
											last5rounds.remove(0);
										}
										if (last5rounds.size() > 5) {
											last5rounds.remove(0);
										}
										if (last5rounds.size() > 5) {
											last5rounds.remove(0);
										}
										realstakes.clear();
										gotout.clear();
										winning = false;
										for (final ArmorStand a : chart) {
											a.remove();
										}
										chart.clear();
										chartstep = 0;
										multi = 0.0;
										warmup = false;
										cooldown = 21;
										players.clear();
										updateArmorStands();
									}
								}.runTaskLater(c.ms, 60L);
							}
						}.runTaskLater(c.ms, (20L * (r.nextInt(11) + 6L)) - 10L);
					}
				}.runTaskLater(c.ms, 400L);
			}
		}
		if (running && winning == false) {
			beginchart.setYaw(180F);
			final ArmorStand a = (ArmorStand) displayloc.getWorld().spawnEntity(beginchart.clone().add(0.2 * chartstep, -2.5, -0.2).add(0, multi, 0), EntityType.ARMOR_STAND);
			a.setGravity(false);
			a.setVisible(false);
			a.setSmall(true);
			a.setItemInHand(new ItemStack(Material.DIAMOND_BLOCK, 1, (short) 0));
			a.setRightArmPose(new EulerAngle(50, 10.2, 0));
			chart.add(a);
			chartstep++;
			multi = multi + (((multi / 10) + 0.01) * (r.nextInt(2) + 1));
			multi = c.round(multi, 2);
			showMultiplier();
			for (final Player p : players) {
				p.playSound(p.getLocation(), Sound.NOTE_SNARE_DRUM, 1F, (float) multi);
			}
			for (final Player p : realstakes.keySet()) {
				if (p.getOpenInventory() != null && p.getOpenInventory().getTopInventory() != null && p.getOpenInventory().getTopInventory().getTitle().contains("Crash")) {
					openCrash(p);
				}
			}
		}
	}

	public void cashout(final Player p, final boolean quit) {
		if (realstakes.containsKey(p) == false) {
			if (quit == false) c.ms.showItemBar(p, "§cDu hast keinen Einsatz gesetzt.");
			return;
		}
		if (c.inCasino(p) == false) {
			c.getStats().CRASH_VERLUST += realstakes.get(p);
			int toAdd = realstakes.get(p);
			new BukkitRunnable() {
				@Override
				public void run() {
					c.addChipsOffline(p, toAdd, "Crash");
				}
			}.runTaskAsynchronously(c.ms);
		} else if (warmup && players.contains(p)) {
			c.getStats().CRASH_VERLUST += realstakes.get(p);
			p.closeInventory();
			c.ms.showItemBar(p, "§cDu hast Crash verlassen.");
			p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
			players.remove(p);
			c.addChips(p, realstakes.get(p), "Crash");
		} else {
			if (multi == 1.0) MuxFirework.spawnColor(p.getLocation(), Color.YELLOW);
			else if (multi > 1.0) MuxFirework.spawnColor(p.getLocation(), Color.GREEN);
			else if (multi < 1.0) MuxFirework.spawnColor(p.getLocation(), Color.RED);
			p.closeInventory();
			p.sendMessage("§a§lCASHOUT! §aDu hast §d" + c.ms.getNumberFormat((int) c.round(realstakes.get(p) * multi, 0)) + " Chips §agewonnen! §f[" + getColor(multi) + multi + "x§f]");
			c.getStats().CRASH_VERLUST += c.round(realstakes.get(p) * multi, 0);
			p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 10F);
			c.addChips(p, (int) c.round(realstakes.get(p) * multi, 0), "Crash");
		}
		gotout.put(p.getName(), multi);
		realstakes.remove(p);
		updateArmorStands();
	}

	public void joinGame(final Player p) {
		if (winning || running || players.size() > 10) {
			c.ms.showItemBar(p, "§cWarte bis zur nächsten Runde.");
			p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
			p.closeInventory();
		} else if (gotout.containsKey(p.getName()) || (realstakes.containsKey(p) == false && players.contains(p) == false)) {
			if (c.canBuy(p, stakes.get(p), "Crash") == false) {
				return;
			}
			realstakes.remove(p);
			players.add(p);
			realstakes.put(p, stakes.get(p));
			c.getStats().CRASH_GEWINN += stakes.get(p);
			updateArmorStands();
			p.closeInventory();
			p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 10F);
			c.ms.showItemBar(p, "§aDu bist Crash beigetreten.");
			openCrash(p);
		}
	}

	public void onInvClick(final Player p, final ItemStack i, final boolean rightclick, final String title) {
		if (title.contains("Crash") == false) return;
		final Material m = i.getType();
		if (i.getType() == Material.INK_SACK) {
			if (running) {
				c.ms.showItemBar(p, "§cDie Runde hat bereits begonnen.");
				p.closeInventory();
				return;
			}
			final int stake = stakes.get(p);
			if (rightclick) {
				if (stake >= 1000) {
					stakes.put(p, stake - c.getNextStakeChange(stake, false));
					p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
				} else {
					c.ms.showItemBar(p, "§cDer Einsatz muss mindestens §d500 Chips §cbetragen.");
					p.closeInventory();
					p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
					return;
				}
			} else {
				final int newstake = stake + c.getNextStakeChange(stake, true);
				if (newstake > c.getChips(p) || newstake > 10000000) {
					p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
					return;
				}
				stakes.put(p, newstake);
				p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
			}
			openCrash(p);
		} else if (m == Material.REDSTONE_BLOCK) {
			cashout(p, true);
		} else if (m == Material.EMERALD_BLOCK) {
			joinGame(p);
		}
	}

	public void onInteract(final Player p, final Sign s) {
		if (s.getLine(0).equalsIgnoreCase("[crash]") || s.getLine(0).equalsIgnoreCase("§1§lCRASH")) {
			s.setLine(0, "§1§lCRASH");
			s.setLine(1, "§8-*-");
			s.setLine(2, "§l500+ Chips");
			s.setLine(3, "Klicke zum Öffnen");
			if (players.isEmpty()) s.update();
			if (players.contains(p) == false && players.size() > 10) {
				c.ms.showItemBar(p, "§cEs sind bereits 10 Spieler im Crash Spiel.");
				return;
			} else if (winning || (running && players.contains(p) == false)) {
				c.ms.showItemBar(p, "§cWarte bis zur nächsten Runde.");
				p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
				return;
			}
			if (players.contains(p) && running) {
				cashout(p, true);
				return;
			}
			openCrash(p);
		}
	}

	private void openCrash(final Player p) {
		Inventory inv = c.ms.getServer().createInventory(null, 54, "§0§lMuxCasino§0 | Crash");
		final boolean alreadyopen = p.getOpenInventory() != null && p.getOpenInventory().getTopInventory() != null && p.getOpenInventory().getTopInventory().getTitle().contains("Crash");
		if (alreadyopen) {
			inv = p.getOpenInventory().getTopInventory();
		}
		int stake = stakes.getOrDefault(p, 0);
		if (stake == 0 || stake > c.getChips(p)) {
			stake = 500;
			stakes.put(p, 500);
		}
		final int realstake = realstakes.getOrDefault(p, -1);
		final ItemStack join = c.ms.renameItemStack(new ItemStack(Material.EMERALD_BLOCK), "§a§lBeitreten", "§7Einsatz: §d" + c.ms.getNumberFormat(stake) + " Chips", "", "§7Klicke, um dem Spiel ", "§abeizutreten§7."),
				leave = c.ms.renameItemStack(new ItemStack(Material.REDSTONE_BLOCK), "§c§lVerlassen", "", "§7Klicke, um das Spiel", "§7zu §cverlassen§7."),
				cashout = c.ms.renameItemStack(new ItemStack(Material.REDSTONE_BLOCK), "§c§lCash out", "", "§7Klicke, um deinen Betrag ", getColor(multi) + multi + "x " + "§7auszuzahlen.");
		c.createButton(inv, 21, realstake == -1 ? join : warmup ? leave : cashout);
		if (realstakes.containsKey(p) == false) {
			inv.setItem(4, c.ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 13), "§f§lEinsatz: §d§l" + c.ms.getNumberFormat(stake) + " Chips", "", "§aLinksklick§7, für §d+" + c.getNextStakeChange(stake, true) + " Chips", "§cRechtsklick§7, für §d-" + c.getNextStakeChange(stake, false) + " Chips"));
		}
		if (alreadyopen) {
			p.updateInventory();
			return;
		}
		if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
		p.openInventory(inv);
		c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
	}
}