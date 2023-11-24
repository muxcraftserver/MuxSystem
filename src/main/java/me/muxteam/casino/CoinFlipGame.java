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
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class CoinFlipGame {
	public int gamenum;
	public boolean runrandom = false, winning = false;
	public Player p1, p2;
	public Map<Player, Integer> realstakes = new HashMap<>();
	private boolean slow = false;
	private final Location loc;
	private final List<ArmorStand> akreis = new ArrayList<>();
	private final MuxHolograms.Hologram windisplay;
	private final Random r = new Random();
	private final MuxCasino c;
	private final CoinFlip cf;

	public CoinFlipGame(int gamenum, final Location loc, final boolean direction, final MuxCasino c, final CoinFlip cf) {
		this.gamenum = gamenum;
		this.loc = loc;
		this.c = c;
		this.cf = cf;
		for (int i = 0; i < 12; i++) {
			Location spawn = loc.clone().add(0, 0, 0);
			if (i == 0) spawn = loc.clone().add(0, 2, direction ? 0.5 : -0.5);
			else if (i == 1) spawn = loc.clone().add(0, 2, 0);
			else if (i == 2) spawn = loc.clone().add(0, 2, direction ? -0.5 : 0.5);
			else if (direction ? i == 3 : i == 9) spawn = loc.clone().add(0, direction ? 1.5 : 0.5, -1);
			else if (direction ? i == 4 : i == 10) spawn = loc.clone().add(0, 1, -1);
			else if (direction ? i == 5 : i == 11) spawn = loc.clone().add(0, direction ? 0.5 : 1.5, -1);
			else if (i == 6) spawn = loc.clone().add(0, 0, direction ? -0.5 : 0.5);
			else if (i == 7) spawn = loc.clone().add(0, 0, 0);
			else if (i == 8) spawn = loc.clone().add(0, 0, direction ? 0.5 : -0.5);
			else if (direction ? i == 9 : i == 3) spawn = loc.clone().add(0, direction ? 0.5 : 1.5, 1);
			else if (direction ? i == 10 : i == 4) spawn = loc.clone().add(0, 1, 1);
			else if (direction ? i == 11 : i == 5) spawn = loc.clone().add(0, direction ? 1.5 : 0.5, 1);
			spawn = spawn.clone().add(-0.4, 0.8, 0);
			spawn.setYaw(direction ? -90 : 90);
			final ArmorStand a = (ArmorStand) loc.getWorld().spawnEntity(spawn, EntityType.ARMOR_STAND);
			a.setSmall(true);
			a.setGravity(false);
			a.setVisible(false);
			a.setHelmet(new ItemStack(Material.SKULL_ITEM, 1));
			akreis.add(a);
			if (i == 1) {
				a.setCustomName("§aCoinflip");
				a.setCustomNameVisible(true);
			}
		}
		windisplay = c.ms.getHolograms().addHologram("coinflipwin" + gamenum, loc.clone().add(-0.3, 2.3, 0), "");
		windisplay.hide();
	}

	public Location getLocation() {
		return loc;
	}

	public void onUpdateHalf() {
		if (runrandom && winning == false && slow == false) {
			updateCircle();
		}
	}

	public void close() {
		realstakes.forEach((p, realstake) -> c.addChipsOffline(p, realstake, "CoinFlip"));
	}

	public void onUpdate() {
		if (runrandom == false && winning == false && (p1 != null && (c.inCasino(p1) == false || p1.getLocation().distance(loc) > 4))) {
			c.addChips(p1, cf.stakes.get(p1), "CoinFlip");
			c.getStats().COINFLIP_GESETZT -= cf.stakes.get(p1);
			c.ms.showItemBar(p1, "§cDu hast Coinflip verlassen.");
			p1 = null;
			reset();
		}
		if (runrandom && winning == false) {
			if (akreis.get(1).getCustomName().contains("Coinflip")) {
				akreis.get(1).setCustomName("§a§lV");
				int as = 0;
				final ItemStack skull1 = new ItemStack(Material.SKULL_ITEM, 1, (short) 3), skull11 = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
				final SkullMeta meta1 = (SkullMeta) skull1.getItemMeta(), meta11 = (SkullMeta) skull11.getItemMeta();
				if (p1 != null) {
					windisplay.setMessage("§3Gewinn: §f" + c.ms.getNumberFormat(realstakes.get(p1) * 2));
					windisplay.show();
					meta11.setDisplayName(p1.getName());
					meta11.setOwner(p1.getName());
				}
				if (p2 != null) {
					meta1.setDisplayName(p2.getName());
					meta1.setOwner(p2.getName());
				}
				int random = r.nextInt(2);
				if (random == 0) {
					skull1.setItemMeta(meta11);
					skull11.setItemMeta(meta1);
				} else {
					skull1.setItemMeta(meta1);
					skull11.setItemMeta(meta11);
				}
				for (final ArmorStand a : akreis) {
					if (as <= 5) a.setHelmet(skull1);
					else a.setHelmet(skull11);
					as++;
				}
				new BukkitRunnable() {
					@Override
					public void run() {
						slow = true;
						new BukkitRunnable() {
							@Override
							public void run() {
								runrandom = false;
								winning = true;
								final String winnerName = akreis.get(1).getHelmet().getItemMeta().getDisplayName();
								final Player win = c.ms.getServer().getPlayer(winnerName);
								loc.getWorld().playSound(loc, Sound.NOTE_PLING, 1F, 10F);
								if (win != null) {
									MuxFirework.spawnColor(win.getLocation(), Color.GREEN);
									if (win.isOnline() && c.inCasino(win)) {
										c.addChips(win, realstakes.get(win) * 2, "CoinFlip");
									} else {
										new BukkitRunnable() {
											@Override
											public void run() {
												c.addChipsOffline(win, realstakes.get(win) * 2, "CoinFlip");
											}
										}.runTaskAsynchronously(c.ms);
									}
									if (p1 != null) {
										if (p1.equals(win)) c.getStats().COINFLIP_GEWINNER_P1 += 1;
										c.ms.showItemBar(p1, "§d" + winnerName + " §ahat gewonnen! §7(§d" + c.ms.getNumberFormat(realstakes.get(win) * 2) + " Chips§7)");
									}
									if (p2 != null) {
										if (p2.equals(win)) c.getStats().COINFLIP_GEWINNER_P2 += 1;
										c.ms.showItemBar(p2, "§d" + winnerName + " §ahat gewonnen! §d(" + c.ms.getNumberFormat(realstakes.get(win) * 2) + " Chips)");
									}
								} else {

									new BukkitRunnable() {
										@Override
										public void run() {
											c.addChipsOffline(c.ms.getPlayer(winnerName).getUniqueId(), realstakes.entrySet().stream().filter(entry -> entry.getKey().getName().equals(winnerName)).map(Map.Entry::getValue).findFirst().orElse(0) * 2, "CoinFlip");
										}
									}.runTaskAsynchronously(c.ms);

									if (p1 != null) {
										c.ms.showItemBar(p1, "§d" + winnerName + " §ahat gewonnen!");
									}
									if (p2 != null) {
										c.ms.showItemBar(p2, "§d" + winnerName + " §ahat gewonnen!");
									}
								}
								p1 = null;
								p2 = null;
								new BukkitRunnable() {
									@Override
									public void run() {
										reset();
										winning = false;
									}
								}.runTaskLater(c.ms, 20L * 3L);
							}
						}.runTaskLater(c.ms, 20L * (4 + r.nextInt(12)));
					}
				}.runTaskLater(c.ms, 20L * (2 + r.nextInt(2)));
			} else if (slow) {
				updateCircle();
			}
		}
	}

	private void updateCircle() {
		final ItemStack head1 = akreis.get(0).getHelmet().clone();
		akreis.get(0).setHelmet(akreis.get(11).getHelmet());
		akreis.get(11).setHelmet(akreis.get(10).getHelmet());
		akreis.get(10).setHelmet(akreis.get(9).getHelmet());
		akreis.get(9).setHelmet(akreis.get(8).getHelmet());
		akreis.get(8).setHelmet(akreis.get(7).getHelmet());
		akreis.get(7).setHelmet(akreis.get(6).getHelmet());
		akreis.get(6).setHelmet(akreis.get(5).getHelmet());
		akreis.get(5).setHelmet(akreis.get(4).getHelmet());
		akreis.get(4).setHelmet(akreis.get(3).getHelmet());
		akreis.get(3).setHelmet(akreis.get(2).getHelmet());
		akreis.get(2).setHelmet(akreis.get(1).getHelmet());
		akreis.get(1).setHelmet(head1);
		loc.getWorld().playSound(loc, Sound.CLICK, 1F, 10F);
	}

	public void openInv(final Player p) {
		final Inventory inv = c.ms.getServer().createInventory(null, 54, "§0§lMuxCasino§0 | CoinFlip");
		final ItemStack go = c.ms.renameItemStack(new ItemStack(Material.EMERALD_BLOCK, 1), p1 == null ? "§a§lErstellen" : "§a§lBeitreten", p1 == null ? new String[] { "§7Klicke, um das Spiel", "§7zu §aerstellen§7."} : new String[] { "§7Einsatz: §d" + c.ms.getNumberFormat(realstakes.get(p1)) + " Chips", "", "§7Klicke, um dem Spiel §abeizutreten§7." });
		c.createButton(inv, 21, go);
		if (p1 == null) {
			final int stake = cf.stakes.get(p);
			inv.setItem(4, c.ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 13), "§f§lEinsatz: §d§l" + c.ms.getNumberFormat(stake) + " Chips", "", "§aLinksklick§7, für §d+" + c.ms.getNumberFormat(c.getNextStakeChange(stake, true)) + " Chips", "§cRechtsklick§7, für §d-" + c.ms.getNumberFormat(c.getNextStakeChange(stake, false)) + " Chips"));
		}
		if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
		p.openInventory(inv);
		c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
	}

	public void reset() {
		realstakes.clear();
		runrandom = false;
		slow = false;
		for (final ArmorStand a : akreis) {
			a.setHelmet(new ItemStack(Material.SKULL_ITEM, 3));
		}
		akreis.get(1).setCustomName("§aCoinflip");
		windisplay.hide();
	}
}