package me.muxteam.shop;

import me.muxteam.basic.MuxAnvil;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MuxTrade {
	private MuxSystem ms;
	private MuxShop shop;
	private final Map<String, HashSet<String>> requests = new HashMap<>();
	private final Map<String, Trade> trades = new HashMap<>();

	public MuxTrade(final MuxSystem ms) {
		this.ms = ms;
		this.shop = ms.getShop();
	}
	public void close() {
		this.ms = null;
		this.shop = null;
	}

	public boolean isTrading(final String name) {
		return trades.containsKey(name);
	}
	public void cancelTrade(final String pname) {
		final Trade t = trades.get(pname);
		if (t != null) {
			t.cancel();
		}
	}
	public void removeRequests(final String pname) {
		requests.remove(pname);
	}
	public boolean handleCommand(final Player p, final String[] args) {
		if (ms.hasVoted(p) == false) {
			ms.sendNoVoteMessage(p);
			return true;
		} else if (args.length == 0) {
			ms.showItemBar(p, ms.usage("/" + ms.getLang("trade.trade") + " [" + ms.getLang("player") + "]"));
			return true;
		}
		final Player pl = ms.getServer().getPlayer(args[0]);
		if (pl == null || p.canSee(pl) == false) {
			ms.showItemBar(p, ms.hnotonline);
			return true;
		} else if (pl.getName().equals(p.getName())) {
			ms.showItemBar(p, ms.getLang("trade.notyourself"));
			return true;
		} else if (ms.inGame(pl)) {
			ms.showItemBar(p, "§cDer Spieler spielt gerade ein Minigame.");
			return true;
		} else if (ms.inWar(pl)) {
			ms.showItemBar(p, "§cDer Spieler ist gerade im Clanwar.");
			return true;
		} else if (ms.inDuel(pl)) {
			ms.showItemBar(p, "§cDer Spieler ist gerade in einem Duell.");
			return true;
		} else if (ms.in1vs1(pl)) {
			ms.showItemBar(p, "§cDer Spieler ist gerade in einem 1vs1.");
			return true;
		} else if (ms.inEvent(pl)) {
			ms.showItemBar(p, "§cDer Spieler ist gerade in einem Event.");
			return true;
		} else if (ms.getCreators().isRecording(pl)) {
			ms.showItemBar(p, "§cDer Spieler kann gerade keine Anfragen erhalten.");
			return true;
		} else if (ms.inBattle(p.getName(), p.getLocation())) {
			ms.showItemBar(p, ms.hnotinfight);
			return true;
		} else if (ms.inBattle(pl.getName(), pl.getLocation())) {
			ms.showItemBar(p, ms.getLang("playerinfight"));
			return true;
		}
		final HashSet<String> list = requests.getOrDefault(p.getName(), new HashSet<>());
		if (list.contains(pl.getName()) == false) {
			final HashSet<String> list2 = requests.getOrDefault(pl.getName(), new HashSet<>());
			if (list2.contains(p.getName())) {
				ms.showItemBar(p, ms.getLang("trade.alreadysent"));
				return true;
			}
			list2.add(p.getName());
			requests.put(pl.getName(), list2);
			ms.chatClickHoverRun(pl, String.format(ms.getLang("trade.wantstotrade"), p.getName()), ms.getLang("trade.hovertotrade"), "/trade " + p.getName());
			ms.chatClickHoverRun(pl, ms.getLang("trade.clicktotrade"), ms.getLang("trade.hovertotrade"), "/trade " + p.getName());
			ms.showItemBar(p, ms.getLang("trade.requestsent"));
			pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
			p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
			return true;
		}
		requests.remove(pl.getName());
		requests.remove(p.getName());
		final Trade t = new Trade(p.getName(), pl.getName());
		t.openInventory(p);
		t.openInventory(pl);
		p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 2F);
		pl.playSound(pl.getLocation(), Sound.LEVEL_UP, 1F, 2F);
		return true;
	}
	public void handleInventory(final Player p, final ItemStack i, final InventoryClickEvent e) {
		final String name = p.getName();
		final Trade t = trades.get(name);
		final boolean first = name.equals(t.getName());
		final int slot = e.getRawSlot();
		if (first) {
			if (slot > 8 && slot < 13 || slot > 17 && slot < 22 || slot > 26 && slot < 31 || slot > 35 && slot < 40) {
				handleItemRemove(p, i, e, t, t.getName2());
			} else if (slot == 45 && i.getDurability() == 5) {
				e.setCancelled(true);
				if (t.getLastChange() + 3000L > System.currentTimeMillis()) {
					p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
					return;
				}
				t.setFirstAccept(true);
				p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 0.7F);
				ms.getServer().getPlayer(t.getName2()).playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 0.7F);
			} else if (slot == 45) {
				if (i.getDurability() == 0) {
					e.setCancelled(true);
					t.setFirstAccept(false);
					p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
					final Player pl = ms.getServer().getPlayer(t.getName2());
					pl.playSound(pl.getLocation(), Sound.NOTE_BASS, 1F, 1F);
				} else {
					e.setCancelled(true);
					t.cancel();
				}
			} else if (slot == 46) {
				e.setCancelled(true);
				t.cancel();
			} else if (slot == 0) {
				handleCoins(p, e, t, true);
			} else if (slot > 53) {
				handleItemAdd(p, i, e, t, 0);
			} else {
				p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
				e.setCancelled(true);
			}
		} else {
			if (slot > 13 && slot < 18 || slot > 22 && slot < 27 || slot > 31 && slot < 36 || slot > 40 && slot < 45) {
				handleItemRemove(p, i, e, t, t.getName());
			} else if (slot == 52 && i.getDurability() == 5) {
				e.setCancelled(true);
				if (t.getLastChange() + 3000L > System.currentTimeMillis()) {
					p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
					return;
				}
				t.setSecondAccept(true);
				p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 0.7F);
				final Player pl = ms.getServer().getPlayer(t.getName());
				pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.7F);
			} else if (slot == 53) {
				if (i.getDurability() == 0) {
					e.setCancelled(true);
					t.setSecondAccept(false);
					p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
					final Player pl = ms.getServer().getPlayer(t.getName());
					pl.playSound(pl.getLocation(), Sound.NOTE_BASS, 1F, 1F);
				} else {
					e.setCancelled(true);
					t.cancel();
				}
			} else if (slot == 8) {
				handleCoins(p, e, t, false);
			} else if (slot > 53) {
				handleItemAdd(p, i, e, t, 5);
			} else {
				p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
				e.setCancelled(true);
			}
		}
	}
	final class Trade {
		private final String name, name2;
		private Inventory inv;
		private long lastchange = System.currentTimeMillis();
		private boolean firstaccept, secondaccept, finish;
		private int coinsamount, coinsamount2;
		private ItemStack coins = ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT), ms.getLang("trade.offermuxcoins")),
				notaccepted = ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 0), ms.getLang("trade.notaccepted")),
				accepted = ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 5), ms.getLang("trade.accepted")),
				accept = ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 5), ms.getLang("trade.accept")),
				decline = ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 14), ms.getLang("trade.decline")),
				abort = ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 0), ms.getLang("trade.abort"));

		public Trade(final String name, final String name2) {
			this.name = name;
			this.name2 = name2;
			this.inv = ms.getServer().createInventory(null, 54, name + getSpace() + name2);
			final ItemStack bar = ms.renameItemStack(new ItemStack(Material.IRON_FENCE), "§f");
			inv.setItem(0, coins);
			inv.setItem(8, coins);
			inv.setItem(3, notaccepted);
			inv.setItem(5, notaccepted);
			for (short i = 0; i < 6; i++) {
				inv.setItem(4 + i * 9, bar);
			}
			inv.setItem(45, accept);
			inv.setItem(46, decline);
			inv.setItem(52, accept);
			inv.setItem(53, decline);
		}
		public String getName() {
			return name;
		}
		public String getName2() {
			return name2;
		}
		public Inventory getInventory() {
			return inv;
		}
		public void openInventory(final Player p) {
			p.closeInventory();
			ms.setActiveInv(p.getName(), InvType.TRADE);
			p.openInventory(inv);
			trades.put(p.getName(), this);
		}
		public void setFirstAccept(final boolean firstaccept) {
			this.firstaccept = firstaccept;
			if (firstaccept) {
				inv.setItem(3, accepted);
				inv.setItem(45, abort);
				inv.setItem(46, null);
				checkIfBoth();
			} else {
				inv.setItem(3, notaccepted);
				inv.setItem(45, accept);
				inv.setItem(46, decline);
			}
		}
		public void setSecondAccept(final boolean secondaccept) {
			this.secondaccept = secondaccept;
			if (secondaccept) {
				inv.setItem(5, accepted);
				inv.setItem(52, null);
				inv.setItem(53, abort);
				checkIfBoth();
			} else {
				inv.setItem(5, notaccepted);
				inv.setItem(52, accept);
				inv.setItem(53, decline);
			}
		}
		public void resetAccepted(final boolean wait) {
			setFirstAccept(false);
			setSecondAccept(false);
			if (wait) lastchange = System.currentTimeMillis();
		}
		public long getLastChange() {
			return lastchange;
		}
		public void removeCoins(final boolean first) {
			if (first) {
				inv.setItem(0, coins);
				coinsamount = 0;
			} else {
				inv.setItem(8, coins);
				coinsamount2 = 0;
			}
		}
		public int getCoins(final boolean first) {
			return first ? coinsamount : coinsamount2;
		}
		public void setCoins(final String input, final Player p, final boolean first) {
			final String pname = p.getName();
			ms.setActiveInv(pname, InvType.TRADE);
			p.openInventory(inv);
			int geld;
			try {
				geld = Integer.parseInt(ms.removeNonDigits(input));
			} catch (final NumberFormatException e) {
				ms.showItemBar(p, ms.hnonumber);
				return;
			}
			final MuxUser u = ms.getMuxUser(pname);
			if (geld < 2) {
				ms.showItemBar(p, ms.getLang("trade.minamount"));
				p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
				return;
			} else if (geld > u.getCoins()) {
				ms.showItemBar(p, ms.getLang("shop.notenough"));
				p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
				return;
			}
			final int alreadypaid = shop.getPayLimits().getOrDefault(pname, 0);
			if (alreadypaid + geld > shop.getDailyPay(p)) {
				ms.showItemBar(p, String.format(ms.getLang("shop.paylimit"), shop.getDailyPay(p) - alreadypaid));
				return;
			}
			resetAccepted(false);
			p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.4F, 1F);
			final ItemStack coinsput = ms.renameItemStack(new ItemStack(Material.GOLD_INGOT), ms.getLang("trade.offermuxcoins"));
			final ItemMeta im = coins.getItemMeta();
			im.setDisplayName("§e§l" + geld + " MuxCoins");
			coinsput.setItemMeta(im);
			if (first) {
				inv.setItem(0, ms.addGlow(coinsput));
				coinsamount = geld;
			} else {
				inv.setItem(8, ms.addGlow(coinsput));
				coinsamount2 = geld;
			}
		}
		public void finish(final boolean success) {
			if (finish) return;
			finish = true;
			final Player p = ms.getServer().getPlayer(name), pl = ms.getServer().getPlayer(name2);
			if (success) {
				giveItemsTo(p, 5);
				giveItemsTo(pl, 0);
				if (coinsamount > 0)  giveCoinsFrom(p, true);
				if (coinsamount2 > 0) giveCoinsFrom(pl, false);
				ms.showItemBar(p, ms.getLang("trade.finished"));
				ms.showItemBar(pl, ms.getLang("trade.finished"));
			} else {
				giveItemsTo(p, 0);
				giveItemsTo(pl, 5);
				ms.showItemBar(p, ms.getLang("trade.cancelled"));
				ms.showItemBar(pl, ms.getLang("trade.cancelled"));
				p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
				pl.playSound(pl.getLocation(), Sound.NOTE_BASS, 1F, 1F);
			}
			trades.remove(p.getName());
			trades.remove(pl.getName());
			p.closeInventory();
			pl.closeInventory();
			this.inv = null;
			this.notaccepted = null;
			this.accepted = null;
			this.accept = null;
			this.decline = null;
			this.abort = null;
			this.coins = null;
		}
		public void cancel() {
			finish(false);
		}
		private void checkIfBoth() {
			if (firstaccept && secondaccept) {
				finish(true);
			}
		}
		private void giveCoinsFrom(final Player p, final boolean first) {
			final String pname = p.getName();
			final MuxUser u = ms.getMuxUser(pname), u2;
			int geld;
			if (first) {
				u2 = ms.getMuxUser(getName2());
				geld = coinsamount;
			} else {
				u2 = ms.getMuxUser(getName());
				geld = coinsamount2;
			}
			u.addCoins(-geld);
			int profit = geld;
			u2.addCoins(profit);
			ms.sendScoreboard(ms.getServer().getPlayer(u2.getName()));
			ms.sendScoreboard(p);
			shop.setPayLimit(pname, shop.getPayLimits().getOrDefault(pname, 0) + geld);
			ms.getHistory().addHistory(p.getUniqueId(), u2.getUUID(), "COINS","-" + geld, "Trade");
			ms.getHistory().addHistory(u2.getUUID(), p.getUniqueId(), "COINS", String.valueOf(geld), "Trade");
			if (geld > 90000) {
				shop.logLargeTransaction(pname + " » " + u2.getName() + " (" + geld + ")");
			}
		}
		private void giveItemsTo(final Player p, final int a) {
			short slot = 0;
			final PlayerInventory pi = p.getInventory();
			short emptyslots = ms.getEmptyInvSlots(pi);
			final CopyOnWriteArrayList<ItemStack> dropitems = new CopyOnWriteArrayList<>();
			for (final ItemStack i : getInventory()) {
				if ((slot > 8 + a && slot < 13 + a || slot > 17 + a && slot < 22 + a || slot > 26 + a && slot < 31 + a || slot > 35 + a && slot < 40 + a)
						&& i != null && i.getType() != Material.AIR) {
					if (emptyslots != (short) 0) {
						pi.addItem(i);
						emptyslots--;
					} else {
						dropitems.add(i);
					}
				}
				slot++;
			}
			p.updateInventory();
			if (dropitems.isEmpty() == false) {
				ms.showItemBar(p, "§cDein Inventar war voll und die Items wurden auf den Boden gedroppt.");
				final World w = p.getWorld();
				final Location l = p.getLocation();
				for (final ItemStack i : dropitems) {
					w.dropItemNaturally(l, i);
				}
			}
		}
		private String getSpace() {
			if (name.length() > name2.length()) return new String(new char[16 - name2.length()]).replace("\0", " ");
			else return new String(new char[16 - name.length()]).replace("\0", " ");
		}
	}
	private void handleCoins(final Player p, final InventoryClickEvent e, final Trade t, final boolean first) {
		e.setCancelled(true);
		if (t.getCoins(first) != 0) {
			t.resetAccepted(true);
			t.removeCoins(first);
			p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
			final Player pl = ms.getServer().getPlayer(first ? t.getName2() : t.getName());
			pl.playSound(pl.getLocation(), Sound.NOTE_BASS, 1F, 1F);
		} else {
			p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
			final int coinsleft = shop.getDailyPay(p) - shop.getPayLimits().getOrDefault(p.getName(), 0);
			new MuxAnvil(ms, (input, player) -> t.setCoins(ms.retardCleaner(input, "MuxCoins: 0"), player, first)).show(p, "MuxCoins: 0",
					ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT), "", "§7§oDu kannst heute noch", "§e§o" + ms.getNumberFormat(coinsleft) + " MuxCoins §7§oüberweisen."));
		}
	}
	private void handleItemRemove(final Player p, final ItemStack i, final InventoryClickEvent e, final Trade t, final String name) {
		final int freeslot = findFreeInventorySlot(p.getInventory().getContents());
		if (freeslot == -1) {
			e.setCancelled(true);
			return;
		}
		t.resetAccepted(true);
		p.getInventory().setItem(freeslot, i.clone());
		e.getClickedInventory().setItem(e.getSlot(), null);
		p.updateInventory();
		p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
		final Player pl = ms.getServer().getPlayer(name);
		pl.playSound(pl.getLocation(), Sound.NOTE_BASS, 1F, 1F);
	}
	private void handleItemAdd(final Player p, final ItemStack i, final InventoryClickEvent e, final Trade t, final int add) {
		final int freeslot = findFreeTradeSlot(add, t.getInventory().getContents());
		if (freeslot == -1) {
			e.setCancelled(true);
			return;
		}
		t.resetAccepted(false);
		t.getInventory().setItem(freeslot, i.clone());
		p.getInventory().setItem(e.getSlot(), null);
		p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.4F, 1F);
		p.updateInventory();
	}
	private short findFreeTradeSlot(final int a, final ItemStack[] items) {
		short slot = 0;
		for (final ItemStack i : items) {
			if ((slot > 8 + a && slot < 13 + a || slot > 17 + a && slot < 22 + a || slot > 26 + a && slot < 31 + a || slot > 35 + a && slot < 40 + a)
					&& (i == null || i.getType() == Material.AIR)) return slot;
			slot++;
		}
		return -1;
	}
	private short findFreeInventorySlot(final ItemStack[] items) {
		short slot = 0;
		for (final ItemStack i : items) {
			if (i == null || i.getType() == Material.AIR) return slot;
			slot++;
		}
		return -1;
	}
}