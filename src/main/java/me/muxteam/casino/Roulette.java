package me.muxteam.casino;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class Roulette {
	private final Set<RouletteGame> games = new HashSet<>();
	private final MuxCasino c;

	public Roulette(final MuxCasino c) {
		this.c = c;
		setup();
	}
	private void setup() {
		games.add(new RouletteGame(new Location(c.getWorld(), -21, 9, -166), c));
		games.add(new RouletteGame(new Location(c.getWorld(), -21, 9, -159), c));
		games.add(new RouletteGame(new Location(c.getWorld(), 32, 9, -166), c));
		games.add(new RouletteGame(new Location(c.getWorld(), 32, 9, -159), c));
		games.add(new RouletteGame(new Location(c.getWorld(), 48, 22, -139), c));
		games.add(new RouletteGame(new Location(c.getWorld(), -37, 21, -178), c));
	}
	public void onUpdate() {
		for (final RouletteGame g : games) {
			g.onUpdate();
		}
	}
	public void quit(final Player p) {
		for (final RouletteGame g : games) {
			g.quit(p);
		}
	}

	public void close() {
		for (final RouletteGame g : games) {
			for (final Map.Entry<Player, Integer> entry : g.realstakes.entrySet()) {
				c.addChipsOffline(entry.getKey(), entry.getValue(), "Roulette");
				c.getStats().ROULETTE_GEWINN -= entry.getValue();
			}
			g.cancel();
		}
	}

	public void onInteract(final Player p, final Sign s) {
		if (s.getLine(0).equalsIgnoreCase("[roulette]") || s.getLine(0).equalsIgnoreCase("§1§lRoulette") || s.getLine(0).equals("§9§lRoulette")) {
			s.setLine(0, "§1§lRoulette");
			s.setLine(1, "§8-*-");
			s.setLine(2, "§l500+ Chips");
			s.setLine(3, "Klicke zum Öffnen");
			s.update();
			final Location bloc = s.getLocation();
			for (final RouletteGame g : games) {
				if (g.getLocation().distance(bloc) < 3.5D) {
					final Block stair = p.getLocation().clone().add(0, 1, 0).getBlock();
					if (stair != null && stair.getType() == Material.DARK_OAK_STAIRS) {
						g.joinPlayer(p);
					} else {
						c.ms.showItemBar(p, "§cSetze dich hin, um mitzuspielen.");
						p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
					}
				}
			}
		}
	}
	public void onInvClick(final Player p, final ItemStack i, final boolean rightclick) {
		RouletteGame game = null;
		for (final RouletteGame g : games) {
			if (g.isPlaying(p)) {
				game = g;
			}
		}
		if (game == null) {
			return;
		}
		p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
		final Material m = i.getType();
		if (m == Material.INK_SACK) {
			final int stake = game.stakes.get(p);
			if (rightclick) {
				if (stake >= 1000) {
					game.stakes.put(p, stake - c.getNextStakeChange(stake, false));
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
				game.stakes.put(p, newstake);
				p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
			}
			game.openInv(p);
		} else if (m == Material.WOOL) {
			if (game.colorset.keySet().parallelStream().anyMatch(player -> player.getUniqueId().equals(p.getUniqueId()))) {
				c.ms.showItemBar(p, "§cDu hast bereits auf eine Farbe getippt.");
				p.closeInventory();
				p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
				return;
			}
			int einsatz = game.stakes.get(p);
			if (c.canBuy(p, einsatz, "Roulette") == false) {
				return;
			}
			c.getStats().ROULETTE_GEWINN += einsatz;
			int set = 1;
			if (i.getDurability() == (short) 15) {
				set = 2;
			} else if (i.getDurability() == (short) 13) {
				set = 3;
			}
			game.colorset.put(p, set);
			game.realstakes.put(p, einsatz);
			p.closeInventory();
			c.ms.showItemBar(p, "§aDu hast deinen Einsatz gesetzt.");
			p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
		}
	}
}