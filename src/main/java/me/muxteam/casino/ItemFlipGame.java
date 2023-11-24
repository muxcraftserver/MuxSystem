package me.muxteam.casino;

import me.muxteam.basic.MuxFirework;
import me.muxteam.muxsystem.MuxInventory;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ItemFlipGame {
	public boolean runrandom, accept1, accept2;
	public ItemStack i1, i2;
	public Player p1, p2;
	private boolean slow, winning;
	private String name1 = null, name2 = null;
	private final List<String> roll = new ArrayList<>();
	private final Location loc;
	private final Random r = new Random();
	private final MuxCasino c;

	public ItemFlipGame(final Location loc, final MuxCasino c) {
		this.loc = loc;
		this.c = c;
	}

	public void onUpdateHalf() {
		if (runrandom && winning == false && slow == false) {
			if (roll.size() >= 7) {
				roll.remove(0);
			}
			String name = name1;
			if (r.nextInt(2) == 0) {
				name = name2;
			}
			roll.add(name);
			if (p1 != null) {
				p1.getWorld().playSound(p1.getLocation(), Sound.CLICK, 1F, 10F);
				openInv(p1);
			}
			if (p2 != null) {
				p2.getWorld().playSound(p2.getLocation(), Sound.CLICK, 1F, 10F);
				openInv(p2);
			}
		}
	}

	public void onUpdate() {
		if (runrandom == false && winning == false) {
			if (p1 != null && (c.inCasino(p1) == false || p1.getLocation().distance(loc) > 4 || p1.getOpenInventory() == null || p1.getOpenInventory().getTopInventory().getTitle().contains("Raum") == false)) {
				c.ms.showItemBar(p1, "§cDu hast ItemFlip verlassen.");
				p1.closeInventory();
				if (p2 != null) {
					p2.closeInventory();
					c.ms.showItemBar(p2, "§6" + p1.getName() + " §chat ItemFlip verlassen.");
				}
				reset();
			}
			if (p2 != null && (c.inCasino(p2) == false || p2.getLocation().distance(loc) > 4 || p2.getOpenInventory() == null || p2.getOpenInventory().getTopInventory().getTitle().contains("Raum") == false)) {
				c.ms.showItemBar(p2, "§cDu hast ItemFlip verlassen.");
				p2.closeInventory();
				if (p1 != null) {
					p1.closeInventory();
					c.ms.showItemBar(p1, "§6" + p2.getName() + " §chat ItemFlip verlassen.");
				}
				reset();
			}
		}
		if (runrandom && winning == false && slow) {
			if (roll.size() >= 7) {
				roll.remove(0);
			}
			String name = name1;
			if (r.nextInt(2) == 0) {
				name = name2;
			}
			roll.add(name);
			if (p1 != null) {
				p1.getWorld().playSound(p1.getLocation(), Sound.CLICK, 1F, 10F);
				openInv(p1);
			}
			if (p2 != null) {
				p2.getWorld().playSound(p2.getLocation(), Sound.CLICK, 1F, 10F);
				openInv(p2);
			}
		}
	}

	public void openInv(final Player p) {
		Inventory inv = c.ms.getServer().createInventory(null, 54, "§0§lMuxCasino§0 | ItemFlip | Raum");
		final InventoryView openinv = p.getOpenInventory();
		final boolean alreadyopen = openinv != null && openinv.getTopInventory() != null && openinv.getTopInventory().getTitle().contains("Raum");
		if (alreadyopen) {
			inv = p.getOpenInventory().getTopInventory();
		}
		final ItemStack skull1 = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
		final SkullMeta meta1 = (SkullMeta) skull1.getItemMeta();
		meta1.setOwner(p1.getName());
		meta1.setDisplayName("§7Einsatz von §3" + p1.getName());
		skull1.setItemMeta(meta1);
		inv.setItem(11, skull1);

		final ItemStack skull11 = new ItemStack(Material.SKULL_ITEM, 1, (byte) 3);
		final SkullMeta meta11 = (SkullMeta) skull11.getItemMeta();
		if (p2 != null) {
			meta11.setOwner(p2.getName());
			meta11.setDisplayName("§7Einsatz von §3" + p2.getName());
		} else {
			meta11.setDisplayName("§cWarte auf Spieler");
		}
		skull11.setItemMeta(meta11);
		inv.setItem(15, skull11);
		if (i1 == null) {
			reset();
			return;
		}
		inv.setItem(20, i1);
		if (p2 != null) {
			if (i2 == null) {
				reset();
				return;
			}
			inv.setItem(24, i2);
			final ItemStack accept = c.ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 13), "§a§lBestätigen", "§7Klicke, um den Einsatz zu §abestätigen§7."), acc = c.ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 5), "§a§lAkzeptiert"), not = c.ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 9), "§c§lNoch nicht akzeptiert");
			inv.setItem(19, not);
			inv.setItem(21, not);
			inv.setItem(23, not);
			inv.setItem(25, not);
			if (p.equals(p1)) {
				inv.setItem(19, accept);
				inv.setItem(21, accept);
			} else {
				inv.setItem(23, accept);
				inv.setItem(25, accept);
			}
			if (accept1) {
				inv.setItem(19, acc);
				inv.setItem(21, acc);
			}
			if (accept2) {
				inv.setItem(23, acc);
				inv.setItem(25, acc);
			}
		}
		final ItemStack glas = c.ms.renameItemStack(new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 15), "§f"), win = c.ms.renameItemStack(new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 5), "§a§lGewinner");
		for (int i = 27; i < inv.getSize(); i++) {
			inv.setItem(i, glas);
		}
		inv.setItem(31, win);
		inv.setItem(49, win);
		if (roll.isEmpty() == false) {
			int size = roll.size();
			int step = 0;
			if (size >= 7) {
				for (final String s : roll) {
					step++;
					inv.setItem(step + 36, c.ms.renameItemStack(c.ms.getHead(s), "§3" + s));
				}
			} else {
				for (final String s : roll) {
					step++;
					inv.setItem((7 - size) + step + 36, c.ms.renameItemStack(c.ms.getHead(s), "§3" + s));
				}
			}
		}
		if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
		if (alreadyopen == false) p.openInventory(inv);
		p.updateInventory();
		c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
	}

	public void roll() {
		winning = false;
		slow = false;
		c.ms.getAnalytics().getAnalytics().addPlayedCasinoGame("Item Flip");
		name1 = p1.getName();
		name2 = p2.getName();
		new BukkitRunnable() {
			@Override
			public void run() {
				slow = true;
				new BukkitRunnable() {
					@Override
					public void run() {
						if (roll.isEmpty()) {
							cancel();
							return;
						}
						runrandom = false;
						winning = true;
						final Player winner = c.ms.getServer().getPlayer(roll.get(3));
						c.ms.showItemBar(p1, "§d" + roll.get(3) + " §ahat gewonnen!");
						c.ms.showItemBar(p2, "§d" + roll.get(3) + " §ahat gewonnen!");
						if (winner != null) {
							winner.getInventory().addItem(i1);
							winner.getInventory().addItem(i2);
							MuxFirework.spawnColor(winner.getLocation(), Color.GREEN);
							winner.playSound(winner.getLocation(), Sound.LEVEL_UP, 1F, 10F);
							if (winner.equals(p1)) c.getStats().ITEMFLIP_GEWINNER_P1 += 1;
							else c.getStats().ITEMFLIP_GEWINNER_P2 += 1;
						}
						new BukkitRunnable() {
							@Override
							public void run() {
								p1.closeInventory();
								p2.closeInventory();
								p1 = null;
								p2 = null;
								reset();
							}
						}.runTaskLater(c.ms, 20L);
					}
				}.runTaskLater(c.ms, 20L * (2L + r.nextInt(4)));
			}
		}.runTaskLater(c.ms, 20L * (6L + r.nextInt(3)));
	}

	public void reset() {
		if (p1 != null) {
			if (runrandom) {
				p1.getInventory().addItem(i1);
			}
			p1.closeInventory();
			c.ms.showItemBar(p1, "§cDieses Spiel wurde abgebrochen.");
			p1.playSound(p1.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
		}
		if (p2 != null) {
			if (runrandom) {
				p2.getInventory().addItem(i2);
			}
			p2.closeInventory();
			c.ms.showItemBar(p2, "§cDieses Spiel wurde abgebrochen.");
			p2.playSound(p2.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
		}
		accept1 = false;
		accept2 = false;
		p1 = null;
		p2 = null;
		i1 = null;
		i2 = null;
		runrandom = false;
		slow = false;
		winning = false;
		roll.clear();
		name1 = null;
		name2 = null;
	}
}