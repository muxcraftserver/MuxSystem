package me.muxteam.casino;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

import java.util.*;

public class SixFields {
	private final Set<SixFieldsGame> games = new HashSet<>();
	private final Random r = new Random();
	private final MuxCasino c;

	public SixFields(final MuxCasino c) {
		this.c = c;
		setup();
	}
	private void setup() {
		games.add(new SixFieldsGame(new Location(c.getWorld(), -22.5, 10, -143.5), c));
		games.add(new SixFieldsGame(new Location(c.getWorld(), -39.5, 10, -144.5), c));
		games.add(new SixFieldsGame(new Location(c.getWorld(), -35.5, 10, -180.5), c));
		games.add(new SixFieldsGame(new Location(c.getWorld(), 49.5, 10, -144.5), c));
		games.add(new SixFieldsGame(new Location(c.getWorld(), 32.5, 10, -143.5), c));
		games.add(new SixFieldsGame(new Location(c.getWorld(), -37.5, 22, -184.5), c));
		games.add(new SixFieldsGame(new Location(c.getWorld(), -24.5, 23, -137.5), c));
		games.add(new SixFieldsGame(new Location(c.getWorld(), 47.5, 22, -184.5), c));
		games.add(new SixFieldsGame(new Location(c.getWorld(), 47.5, 22, -179.5), c));
	}
	public void onUpdate() {
		for (final SixFieldsGame g : games) {
			g.onUpdate();
		}
	}
	public void quit(final Player p) {
		for (final SixFieldsGame g : games) {
			g.quit(p);
		}
	}

	public void close() {
		for (final SixFieldsGame g : games) {
			for (final Map.Entry<Player, Integer> entry : g.setonfield.entrySet()) {
				final int einsatz = g.chipsonfield.get(g.setonfield.get(entry.getKey()));
				c.addChipsOffline(entry.getKey(), einsatz, "6 Felder");
			}
			g.cancel();
		}
	}

	public void onInteract(final Player p, final Sign s) {
		if (s.getLine(0).equalsIgnoreCase("[sixfields]") || s.getLine(0).equalsIgnoreCase("§1§l6 FELDER") || s.getLine(0).equalsIgnoreCase("§9§lPOKER") || s.getLine(0).equalsIgnoreCase("§1§lPOKER")) {
			s.setLine(0, "§1§l6 FELDER");
			s.setLine(1, "§8-*-");
			s.setLine(2, "§l500+ Chips");
			s.setLine(3, "Klicke zum Öffnen");
			s.update();
			final Location bloc = s.getLocation();
			for (final SixFieldsGame g : games) {
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
	public void onInvClick(final Player p, final ItemStack i, final int rawslot, final boolean rightclick) {
		SixFieldsGame game = null;
		for (final SixFieldsGame g : games) {
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
			if (game.setonfield.keySet().parallelStream().anyMatch(player -> player.getUniqueId().equals(p.getUniqueId()))) {
				c.ms.showItemBar(p, "§cDu hast bereits auf ein Feld getippt.");
				p.closeInventory();
				p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
				return;
			} else if (c.ms.hasGlow(i)) {
				p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
				return;
			}
			int set = 1;
			Location chips = game.spielfeld.get(set).getLocation();
			if (rawslot == 11) {
				set = 1;
				chips = game.getLocation().clone().add(0, 0, 2);
			} else if (rawslot == 12) {
				set = 2;
				chips = game.getLocation().clone().add(0, 0, 1);
			} else if (rawslot == 13) {
				set = 3;
				chips = game.getLocation().clone();
			} else if (rawslot == 20) {
				set = 4;
				chips = game.getLocation().clone().add(1, 0, 2);
			} else if (rawslot == 21) {
				set = 5;
				chips = game.getLocation().clone().add(1, 0, 1);
			} else if (rawslot == 22) {
				set = 6;
				chips = game.getLocation().clone().add(1, 0, 0);
			}
			boolean alreadyused = false;
			for (final int a : game.setonfield.values()) {
				if (a == set) {
					alreadyused = true;
					break;
				}
			}
			if (alreadyused) {
				p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
				game.openInv(p);
				return;
			}
			int einsatz = game.stakes.get(p);
			if (c.canBuy(p, einsatz, "6 Felder") == false) {
				return;
			}
			c.getStats().SIXFIELDS_GEWINN += einsatz;

			game.setonfield.put(p, set);
			game.chipsonfield.put(set, game.chipsonfield.get(set) + einsatz);
			if (chips != null) {
				final Location loc = chips.clone();
				final int r1 = r.nextInt(5) + 2, r2 = r.nextInt(5) + 2, r3 = r.nextInt(5) + 2;
				for (int n = 0; n < r1; n++) {
					spawnChips(game.chipsontable, loc, 0.2, n, 0.2, (byte) 13);
				}
				for (int n = 0; n < r2; n++) {
					spawnChips(game.chipsontable, loc, 0.4, n, 0.6, (byte) 10);
				}
				for (int n = 0; n < r3; n++) {
					spawnChips(game.chipsontable, loc, 0.5, n, 0.2, (byte) 5);
				}
			}
			p.closeInventory();
			c.ms.showItemBar(p, "§aDu hast deinen Einsatz gesetzt.");
			c.ms.getAnalytics().getAnalytics().addPlayedCasinoGame("Sechs Felder");
			p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
		}
	}
	private void spawnChips(final List<ArmorStand> ar, final Location loc, final double x, final int b, final double z, final byte data) {
		final ArmorStand a = (ArmorStand) loc.getWorld().spawnEntity(loc.clone().add(x, 0.2 + (0.03 * b), z), EntityType.ARMOR_STAND);
		a.setVisible(false);
		a.setGravity(false);
		a.setItemInHand(new ItemStack(Material.INK_SACK, 1, data));
		a.setRightArmPose(new EulerAngle(0.0, 3.0, 0.0));
		ar.add(a);
	}
}