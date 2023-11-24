package me.muxteam.casino;

import me.muxteam.muxsystem.MuxInventory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;


public class ItemFlip {
	private final List<ItemFlipGame> games = new ArrayList<>();
	private Inventory inv, invnohand;
	private final MuxCasino c;

	public ItemFlip(final MuxCasino c) {
		this.c = c;
		setup();
	}

	public void setup() {
		final Location loc = new Location(c.getWorld(), -14.5, 8, -157.5, -145F, 0F);
		for (int i = 1; i < 8; i++) {
			final ItemFlipGame g1 = new ItemFlipGame(loc, c);
			games.add(g1);
		}
		inv = c.ms.getServer().createInventory(null, 54, "§0§lMuxCasino§0 | ItemFlip");
		invnohand = c.ms.getServer().createInventory(null, 54, "§0§lMuxCasino§0 | ItemFlip");
		c.ms.getNPCS().addVillager(0, loc.clone(), BlockFace.NORTH_EAST, "§a§lItemFlip", this::openInv);
	}

	public void onUpdate() {
		for (final ItemFlipGame g : games) {
			g.onUpdate();
		}
	}

	public Optional<ItemFlipGame> getFrom(final Player p) {
		return games.stream().filter(itemFlipGame -> itemFlipGame.p1 == p || itemFlipGame.p2 == p).findFirst();
	}

	public void onUpdateHalf() {
		for (final ItemFlipGame g : games) {
			g.onUpdateHalf();
		}
	}

	public void onInvClick(final Player p, final ItemStack i, final int rawslot, final String title) {
		if (title.contains("ItemFlip") == false) return;
		final Material m = i.getType();
		if (m == Material.SKULL_ITEM && title.contains("Raum") == false) {
			p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
			final ItemStack hand = p.getItemInHand();
			if (hand == null || hand.getType() == Material.AIR || c.ms.getUltra().isUltraItem(hand) || c.ms.isMenuItem(hand)) {
				c.ms.showItemBar(p, "§cNimm ein Item in die Hand.");
				p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
				p.closeInventory();
				return;
			}
			final ItemStack einsatz = p.getItemInHand().clone();
			final ItemFlipGame game = games.get(rawslot - 19);
			if (game == null) {
				return;
			}
			if (game.p1 != null && game.p1 != p) {
				if (game.p2 != null) {
					p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
					return;
				}
				game.i2 = einsatz.clone();
				game.p2 = p;
				game.openInv(game.p1);
			} else {
				if (game.p2 != null) {
					game.reset();
				}
				game.i1 = einsatz.clone();
				game.p1 = p;
			}
			game.openInv(p);
			updateInvs();
			return;
		}
		ItemFlipGame game = null;
		for (final ItemFlipGame g : games) {
			if ((g.p1 != null && g.p1.equals(p)) || (g.p2 != null && g.p2.equals(p))) game = g;
		}
		if (game == null) {
			return;
		}
		if (m == Material.STAINED_CLAY && i.getDurability() == (short) 13) {
			p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
			if (game.p1.equals(p)) {
				game.accept1 = true;
			}
			if (game.p2.equals(p)) {
				game.accept2 = true;
			}
			game.openInv(game.p1);
			game.openInv(game.p2);
			if (game.accept1 && game.accept2 && game.runrandom == false) {
				game.runrandom = true;
				game.p1.setItemInHand(new ItemStack(Material.AIR));
				game.p1.setItemInHand(null);
				game.p2.setItemInHand(new ItemStack(Material.AIR));
				game.p2.setItemInHand(null);
				game.roll();
			}
		}
	}

	private void updateInvs() {
		updateInv(true);
		updateInv(false);
	}

	private void updateInv(final boolean empty) {
		int step = 0;
		for (final ItemFlipGame iflip : games) {
			step++;
			final ItemStack skull1 = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
			final SkullMeta meta1 = (SkullMeta) skull1.getItemMeta();
			if (iflip.p1 != null) {
				meta1.setOwner(iflip.p1.getName());
				if (iflip.p2 != null) {
					meta1.setDisplayName("§3§l" + iflip.p1.getName() + "§7 gegen §3§l" + iflip.p2.getName());
				} else {
					meta1.setDisplayName("§3§l" + iflip.p1.getName() + "§a §lherausfordern");
					if (empty) {
						meta1.setLore(Arrays.asList("", "§cNehme das einsetzende Item in die", "§cHand, um dem Spiel beizutreten."));
					} else {
						meta1.setLore(Arrays.asList("", "§7Klicke, um dem Spiel §abeizutreten§7.", "", "§cACHTUNG! §7Als Einsatz wird das", "§7Item in deiner Hand genommen."));
					}
				}
			} else {
				meta1.setDisplayName("§f§lSpiel " + step + "§a §lerstellen");
				if (empty) {
					meta1.setLore(Arrays.asList("", "§cNehme das einsetzende Item in die", "§cHand, um ein Spiel zu erstellen."));
				} else {
					meta1.setLore(Arrays.asList("", "§7Klicke, um ein Spiel zu §aerstellen§7.", "", "§cACHTUNG! §7Als Einsatz wird das", "§7Item in deiner Hand genommen."));
				}
			}
			skull1.setItemMeta(meta1);
			if (empty) invnohand.setItem(step + 18, skull1);
			else inv.setItem(step + 18, skull1);
		}
	}

	public void openInv(final Player p) {
		if (c.inCasino(p) == false) return;
		updateInvs();
		if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
		p.openInventory(p.getItemInHand() == null || p.getItemInHand().getType() == Material.AIR ? invnohand : inv);
		c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
	}
}