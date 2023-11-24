package me.muxteam.casino;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CoinFlip {
	private final List<CoinFlipGame> games = new ArrayList<>();
	public final Map<Player, Integer> stakes = new HashMap<>();
	private final Map<Player, Integer> gamenr = new HashMap<>();
	private final MuxCasino c;

	public CoinFlip(final MuxCasino c) {
		this.c = c;
		setup();
	}
	public void setup() {
		games.add(new CoinFlipGame(1, new Location(c.getWorld(), -41.5, 11, -156.5), true, c, this));
		games.add(new CoinFlipGame(2, new Location(c.getWorld(), -41.5, 11, -163.5), true, c, this));
		games.add(new CoinFlipGame(3, new Location(c.getWorld(), 53.2, 11, -163.5), false, c, this));
		games.add(new CoinFlipGame(4, new Location(c.getWorld(), 53.2, 11, -156.5), false, c, this));
		games.add(new CoinFlipGame(5, new Location(c.getWorld(), -39.2, 23.5, -147.5), true, c, this));
		games.add(new CoinFlipGame(6, new Location(c.getWorld(), 51, 23.5, -145.5), false, c, this));
	}
	public void onInteract(final Player p, final Sign s) {
		if (s.getLine(0).equalsIgnoreCase("[CoinFlip]") || s.getLine(0).equalsIgnoreCase("§1§lCOINFLIP") || s.getLine(0).equalsIgnoreCase("§9§lCOINFLIP")) {
			s.setLine(0, "§1§lCOINFLIP");
			s.setLine(1, "§8-*-");
			s.setLine(2, "§l500+ Chips");
			s.setLine(3, "Klicke zum Öffnen");
			s.update();
			final Location l = p.getLocation();
			int game = -1;
			for (final CoinFlipGame g : games) {
				if ((g.p1 != null && g.p1.equals(p)) || (g.p2 != null && g.p2.equals(p))) {
					c.ms.showItemBar(p, "§cWarte auf einen Mitspieler.");
					p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
					return;
				} else if (g.getLocation().distance(l) < 3.5D) {
					game = g.gamenum;
				}
			}
			gamenr.put(p, game);
			for (final CoinFlipGame g : games) {
				if (g.gamenum == game) {
					int stake = stakes.getOrDefault(p, 0);
					if (stake == 0 || stake > c.getChips(p)) {
						stakes.put(p, 500);
					}
					if ((g.p1 != null && g.p2 != null) || g.runrandom || g.winning) {
						c.ms.showItemBar(p, "§cDieses Spiel ist derzeit besetzt.");
						p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
						return;
					}
					if (g.p1 == null || g.p1 != p) {
						g.openInv(p);
					} else {
						c.ms.showItemBar(p, "§cWarte auf einen Mitspieler.");
					}
				}
			}
		}
	}
	public void onUpdate() {
		for (final CoinFlipGame g : games) {
			g.onUpdate();
		}
	}
	public void onUpdateHalf() {
		for (final CoinFlipGame g : games) {
			g.onUpdateHalf();
		}
	}

	public void close() {
		for (final CoinFlipGame g : games) {
			g.close();
		}
	}

	public void onInvClick(final Player p, final ItemStack i, final boolean rightclick, final String title) {
		if (title.contains("CoinFlip") == false) return;
		CoinFlipGame game = null;
		final int gn = gamenr.getOrDefault(p, -1);
		if (gn == -1) return;
		for (final CoinFlipGame g : games) {
			if (g.gamenum == gn) game = g;
		}
		if (game == null) return;
		p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
		final Material m = i.getType();
		if (m == Material.INK_SACK) {
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
				if (newstake > c.getChips(p) || newstake > 1000000000) {
					p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
					return;
				}
				stakes.put(p, newstake);
				p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
			}
			game.openInv(p);
		} else if (m == Material.EMERALD_BLOCK) {
			final String display = i.getItemMeta().getDisplayName();
			if (display.contains("Beitreten")) {
				if (game.p1 == null) {
					p.closeInventory();
					c.ms.showItemBar(p, "§cDein Mitspieler ist weg.");
					p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
					return;
				} else if (game.p2 != null) {
					p.closeInventory();
					c.ms.showItemBar(p, "§cDieses Spiel ist bereits besetzt.");
					p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
					return;
				}
				int stake = stakes.get(game.p1);
				if (c.canBuy(p, stake, "CoinFlip") == false) {
					return;
				}
				game.realstakes.put(p, stake);
				c.getStats().COINFLIP_GESETZT += stake;
				game.p2 = p;
				game.runrandom = true;
				c.ms.getAnalytics().getAnalytics().addPlayedCasinoGame("Coinflip");
				p.closeInventory();
				c.ms.showItemBar(p, "§aEinsatz von §d" + c.ms.getNumberFormat(stake) + " Chips §agesetzt.");
			} else if (display.contains("Erstellen")) {
				if (game.p1 != null) {
					p.closeInventory();
					c.ms.showItemBar(p, "§cDieser Coinflip ist schon erstellt.");
					p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
				} else if (game.p2 == null) {
					int stake = stakes.get(p);
					if (c.canBuy(p, stake, "CoinFlip") == false) {
						return;
					}
					game.realstakes.put(p, stake);
					c.getStats().COINFLIP_GESETZT += stake;
					game.p1 = p;
					p.closeInventory();
					c.ms.showItemBar(p, "§aEinsatz von §d" + c.ms.getNumberFormat(stake) + " Chips §agesetzt.");
					c.ms.sendNormalTitle(p, "§eWarte nun auf einen Mitspieler.", 10, 40, 10);
				} else {
					game.reset();
				}
			}
		}
	}
}