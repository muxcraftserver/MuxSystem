package me.muxteam.casino;

import me.muxteam.basic.MuxFirework;
import me.muxteam.basic.MuxHolograms;
import me.muxteam.muxsystem.MuxInventory;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class SixFieldsGame {
	private boolean runrandom;
	private int cooldown = 20, step = 0;
	private final Location loc;
	private final Random r = new Random();
	public final Map<Integer, Integer> chipsonfield = new HashMap<>();
	public final Map<Player, Integer> stakes = new HashMap<>(), setonfield = new HashMap<>();
	public final List<Block> spielfeld = new ArrayList<>();
	private final List<Player> players = new ArrayList<>();
	public final List<ArmorStand> chipsontable = new ArrayList<>();
	private final MuxHolograms.Hologram status;
	private final MuxCasino c;
	private boolean cancelled;

	public SixFieldsGame(final Location loc, final MuxCasino c) {
		this.loc = loc;
		this.c = c;
		spielfeld.add(loc.clone().add(0, 0, 2).getBlock());
		spielfeld.add(loc.clone().add(0, 0, 1).getBlock());
		spielfeld.add(loc.getBlock());
		spielfeld.add(loc.clone().add(1, 0, 2).getBlock());
		spielfeld.add(loc.clone().add(1, 0, 1).getBlock());
		spielfeld.add(loc.clone().add(1, 0, 0).getBlock());
		status = c.ms.getHolograms().addHologram("casinopokerstatus" + loc.getX() + loc.getZ(), loc.clone().add(0.5, -0.2, 1), "§aWarte auf Spieler");
		for (final Block b : spielfeld) {
			b.setData((byte) 13);
		}
		for (int i = 1; i <= 6; i++) {
			chipsonfield.put(i, 0);
		}
	}

	public Location getLocation() {
		return loc;
	}

	public boolean isPlaying(final Player p) {
		return players.contains(p);
	}

	public void joinPlayer(final Player p) {
		if (players.contains(p) == false) {
			if (players.size() >= 4) {
				c.ms.showItemBar(p, "§cDieser Tisch ist voll besetzt.");
				p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
				return;
			}
			players.add(p);
		}
		int stake = stakes.getOrDefault(p, 0);
		if (stake == 0 || stake > c.getChips(p)) {
			stakes.put(p, 500);
		}
		openInv(p);
	}

	public void quit(final Player p) {
		setonfield.remove(p);
		players.remove(p);
	}

	public void cancel() {
		this.cancelled = true;
	}

	public void onUpdate() {
		if (cancelled) return;
		final Set<Player> remove = new HashSet<>();
		for (final Player p : players) {
			if (p == null || (p.isOnline() == false) || (p != c.ms.getServer().getPlayer(p.getUniqueId()))) continue;
			final boolean set = setonfield.containsKey(p);
			if (p.getWorld() != loc.getWorld() && set == false) {
				remove.add(p);
				continue;
			}
			if (p.getLocation().getWorld() == loc.getWorld() && (p.getLocation().distance(loc) > 5 || p.isInsideVehicle() == false)) {
				if (set == false) remove.add(p);
			}
		}
		for (final Player p : remove) {
			players.remove(p);
			c.ms.showItemBar(p, "§cDu hast den Spieltisch verlassen.");
			p.closeInventory();
			stakes.remove(p);
		}
		remove.clear();
		step++;
		boolean ready = false;
		for (final Integer i : chipsonfield.values()) {
			if (i > 0) {
				ready = true;
				break;
			}
		}
		if (step >= 2) {
			step = 0;
			// Every Second
			if (ready && runrandom == false) {
				if (cooldown > 0) {
					cooldown = cooldown - 1;
					status.setMessage("§aZiehung in §7" + cooldown);
				} else {
					runrandom = true;
					new BukkitRunnable() {
						@Override public void run() {
							if (cancelled) return;
							runrandom = false;
							int blocknr = 0;
							if (loc.clone().add(0, 0, 2).getBlock().getData() == (byte) 5) {
								blocknr = 1;
								MuxFirework.spawnColor(loc.clone().add(0, 0, 2), Color.GREEN);
							} else if (loc.clone().add(0, 0, 1).getBlock().getData() == (byte) 5) {
								blocknr = 2;
								MuxFirework.spawnColor(loc.clone().add(0, 0, 1), Color.GREEN);
							} else if (loc.clone().add(0, 0, 0).getBlock().getData() == (byte) 5) {
								blocknr = 3;
								MuxFirework.spawnColor(loc.clone().add(0, 0, 0), Color.GREEN);
							} else if (loc.clone().add(1, 0, 2).getBlock().getData() == (byte) 5) {
								blocknr = 4;
								MuxFirework.spawnColor(loc.clone().add(1, 0, 2), Color.GREEN);
							} else if (loc.clone().add(1, 0, 1).getBlock().getData() == (byte) 5) {
								blocknr = 5;
								MuxFirework.spawnColor(loc.clone().add(1, 0, 1), Color.GREEN);
							} else if (loc.clone().add(1, 0, 0).getBlock().getData() == (byte) 5) {
								blocknr = 6;
								MuxFirework.spawnColor(loc.clone().add(1, 0, 0), Color.GREEN);
							}
							step += spielfeld.size();
							if (blocknr == 1) c.getStats().SIXFIELDS_WON_FELD1 += 1;
							else if (blocknr == 2) c.getStats().SIXFIELDS_WON_FELD2 += 1;
							else if (blocknr == 3) c.getStats().SIXFIELDS_WON_FELD3 += 1;
							else if (blocknr == 4) c.getStats().SIXFIELDS_WON_FELD4 += 1;
							else if (blocknr == 5) c.getStats().SIXFIELDS_WON_FELD5 += 1;
							else if (blocknr == 6) c.getStats().SIXFIELDS_WON__FELD6 += 1;
							Map<Player, Integer> toSave = new HashMap<>();

							for (final Map.Entry<Player, Integer> entry : setonfield.entrySet()) {
								final Player p = entry.getKey();
								final Integer set = entry.getValue();
								if (p != null && set == blocknr) {
									c.getStats().SIXFIELDS_VERLUST += chipsonfield.get(blocknr) * 5.9;
									for (final Player o : players) {
										if (o != null && o.isOnline()) {
											o.sendMessage("§5§lMuxCasino>§d " + p.getName() + "§7 gewinnt §a§l6 Felder §7(§d+" + c.ms.getNumberFormat((int) (chipsonfield.get(blocknr) * 5.9)) + " Chips§7)!");
											status.setMessage("§aGewinner: §f" + p.getName());
										}
									}
									if ((p.isOnline() == false) || (c.inCasino(p) == false)) {
										toSave.put(p, (int) (chipsonfield.get(blocknr) * 5.9));
									} else {
										c.addChips(p, (int) (chipsonfield.get(blocknr) * 5.9), "6 Felder");
									}
								}
							}
							if (toSave.isEmpty() == false) {
								new BukkitRunnable() {
									@Override
									public void run() {
										for (final Map.Entry<Player, Integer> entry : toSave.entrySet()) {
											if (entry.getKey().isOnline() && c.inCasino(entry.getKey())) {
												c.addChips(entry.getKey(), entry.getValue(), "6 Felder");
												continue;
											}
											c.addChipsOffline(entry.getKey(), entry.getValue(), "6 Felder");
										}
									}
								}.runTaskAsynchronously(c.ms);
							}
							reset();
						}
					}.runTaskLater(c.ms, 80L + 20L * r.nextInt(6));
				}
			} else if (runrandom) {
				status.setMessage("§aAuflösung...");
				for (final Player p : players) {
					if (p.isOnline()) p.closeInventory();
				}
			} else if (players.isEmpty() == false && players.size() < 4) {
				status.setMessage("§aSpieler §7" + players.size() + " / 4");
			} else if (players.isEmpty()) {
				status.setMessage("§aWarte auf Spieler");
			}
		}
		if (runrandom) {
			Block last = null;
			for (final Block bb : spielfeld) {
				if (bb.getData() == (byte) 5) last = bb;
				bb.setData((byte) 13);
			}
			Block b = last == null ? spielfeld.get(r.nextInt(spielfeld.size())) : last;
			while (last != null && last.equals(b)) {
				b = spielfeld.get(r.nextInt(spielfeld.size()));
			}
			b.setData((byte) 5);
			b.getWorld().playSound(b.getLocation(), Sound.CLICK, 1F, 10F);
		}
	}

	public void openInv(final Player p) {
		final Inventory inv = c.ms.getServer().createInventory(null, 36, "§0§lMuxCasino§0 | 6 Felder");
		for (int i = 1; i <= 6; i++) {
			String name = "";
			for (final Map.Entry<Player, Integer> entry : setonfield.entrySet()) {
				if (entry.getValue() == i) {
					name = entry.getKey().getName();
				}
			}
			ItemStack green = c.ms.renameItemStack(new ItemStack(Material.WOOL, 1, (byte) 13), "§a§lFeld " + i, "");
			if (name.isEmpty()) green = c.ms.addLore(green, "§7Klicke um auf dieses Feld zu §asetzen§7.");
			else green = c.ms.addLore(green, "§f" + name + " - §d" + chipsonfield.get(i) + " Chips");
			final boolean set = chipsonfield.get(i) > 0;
			if (i == 1) inv.setItem(2 + 9 , set ? c.ms.addGlow(green) : green);
			else if (i == 2) inv.setItem(3 + 9, set ? c.ms.addGlow(green) : green);
			else if (i == 3) inv.setItem(4 + 9 , set ? c.ms.addGlow(green) : green);
			else if (i == 4) inv.setItem(2 + 9 + 9, set ? c.ms.addGlow(green) : green);
			else if (i == 5) inv.setItem(3 + 9 + 9, set ? c.ms.addGlow(green) : green);
			else if (i == 6) inv.setItem(4 + 9 + 9, set ? c.ms.addGlow(green) : green);
		}
		if (setonfield.containsKey(p) == false) {
			final int stake = stakes.get(p);
			inv.setItem(15, c.ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 13), "§f§lEinsatz: §d§l" + c.ms.getNumberFormat(stake) + " Chips", "", "§aLinksklick§7, für §d+" + c.ms.getNumberFormat(c.getNextStakeChange(stake, true)) + " Chips", "§cRechtsklick§7, für §d-" + c.ms.getNumberFormat(c.getNextStakeChange(stake, false)) + " Chips"));
		}
		if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
		p.openInventory(inv);
		c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
	}

	public void reset() {
		players.removeIf(player -> player == null || player.isOnline() == false || player != c.ms.getServer().getPlayer(player.getUniqueId()));
		for (int i = 1; i <= 6; i++) {
			chipsonfield.put(i, 0);
		}
		cooldown = 20;
		runrandom = false;
		for (final Block b : spielfeld) {
			b.setData((byte) 13);
		}
		for (final ArmorStand a : chipsontable) {
			a.remove();
		}
		setonfield.clear();
	}
}