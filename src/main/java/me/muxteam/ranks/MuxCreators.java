package me.muxteam.ranks;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.muxteam.extras.MuxBoosters.BoosterType;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

public final class MuxCreators {
	private MuxSystem ms;
	private final Set<UUID> recording = new HashSet<>(), flyspawn = new CopyOnWriteArraySet<>();
	private final ItemStack youtubehead;

	public MuxCreators(final MuxSystem ms) {
		this.ms = ms;
		this.youtubehead = ms.getHeadFromURL("https://textures.minecraft.net/texture/bb7688da46586b859a1cde40cae1cdbc15abe35615c4bc5296fad09394105d0", "");
	}
	public void close() {
		this.ms = null;
	}
	public void checkFlying() {
		final Server sr = ms.getServer();
		for (final UUID uuid : flyspawn) {
			final Player p = sr.getPlayer(uuid);
			if (p == null) {
				flyspawn.remove(uuid);
				continue;
			}
			if (ms.hasNormalFlight(p)) continue;
			final Location l = p.getLocation();
			final ProtectedRegion spawnrg = ms.getSpawnRegion();
			final boolean canfly = (spawnrg != null && p.getWorld().equals(ms.getGameSpawn().getWorld()) && spawnrg.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ()));
			if (p.getAllowFlight() && canfly == false) {
				p.setAllowFlight(false);
				p.setFlying(false);
			} else if (p.getAllowFlight() == false && canfly) {
				p.setAllowFlight(true);
				p.setFlying(true);
				p.setFlySpeed(0.05F);
			}
			if (p.isFlying()) {
				p.getWorld().spigot().playEffect(l.subtract(0D, 0.5D, 0D), Effect.CLOUD, 0, 0, (float) 0.1, (float) 0.1, (float) 0.1, (float) 0.01, 3, 50);
			}
		}
	}
	public void handleInventory(final Player p, final ItemStack i) {
		if (i.getItemMeta().hasLore() && i.getItemMeta().getLore().size() > 3 && i.getItemMeta().getLore().get(3).startsWith("§c")) {
			p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
			p.updateInventory();
			return;
		}
		final Material m = i.getType();
		if (m == Material.RECORD_7) {
			toggleRecording(p.getUniqueId());
		} else if (m == Material.SKULL_ITEM) {
			p.closeInventory();
			p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.5F, 1F);
			p.getInventory().addItem(ms.renameItemStack(ms.getHead(p.getName()), "§dCreator §7» §a" + p.getName()));
			ms.getKits().checkKitCooldown(p.getUniqueId(), "Creator", true);
			return;
		} else if (m == Material.FEATHER) {
			if (toggleFlySpawn(p.getUniqueId())) {
				p.setAllowFlight(true);
				p.setFlying(true);
				p.setFlySpeed(0.05F);
			} else {
				p.setAllowFlight(false);
				p.setFlying(false);
				p.setFlySpeed(0.1F);
			}
		} else {
			p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
			return;
		}
		handleCreator(p);
		p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
	}
	public boolean handleCreator(final Player p) {
		if (p.hasPermission("muxsystem.creator") == false) {
			ms.openBook(ms.book("Creator", "§5§lCreator\n\n§0Dieser Rang ist für prominente Creators reserviert die häufig MuxCraft Videos hochladen oder viele Spieler anwerben.\n\nVorteile:\n- 30% Cashback\n- §8[§5Creator§8]§0 Präfix\n- Eigenes Menü"), p);
			return true;
		}
		final UUID uuid = p.getUniqueId();
		final String time = ms.getKits().checkKitCooldown(uuid, "Creator", false);
		final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lMuxCreator§0 | /creator");
		final Location l = p.getLocation();
		final ProtectedRegion spawnrg = ms.getSpawnRegion();
		final boolean nofly = (spawnrg != null && spawnrg.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) == false;
		final boolean fly = isFlySpawn(uuid), rec = isRecording(p);
		final ItemStack flyitem = ms.addLore(ms.renameItemStack(new ItemStack(Material.FEATHER), "§f§lFlugmodus" + (fly ? "§a §laktiviert" : ""),
				"§7Hiermit kannst du am Spawn", "§7durch die Gegend fliegen.", ""),
				ms.getBooster().hasBooster(p, BoosterType.FLY) ? new String[] { "§cDu nutzt gerade einen Fly Booster." } :
				p.getAllowFlight() && fly == false ? new String[] { "§cDu hast bereits ein Flugmodus aktiv." } :
						nofly ? new String[] { "§cDu kannst es nur am Spawn aktivieren." } :
				new String[] { "§7Klicke, um den Flugmodus", "§7zu " + (fly ? "§cdeaktivieren" : "§aaktivieren") + "§7." });
		inv.setItem(10, fly ? ms.addGlow(flyitem) : flyitem);
		inv.setItem(13, ms.addLore(ms.renameItemStack(youtubehead, "§f§lDein Kopf", "§7Sammle oder verlose deinen", "§7Kopf an andere Spieler.", ""), time == null ? new String[] { "§7Klicke, um deinen Kopf", "§7zu §aerhalten§7." } : new String[] { String.format("§cWarte §6%s§c, um deinen Kopf", time), "§cwieder zu erhalten."}));
		final ItemStack recorditem = ms.renameItemStack(new ItemStack(Material.RECORD_7), "§f§lAufnahmemodus" + (rec ? "§a §laktiviert" : ""), "§7MSGs von fremden Spielern und", "§7alle Anfragen werden ignoriert.", "", "§7Klicke, um den Aufnahmemodus", "§7zu " + (rec ? "§cdeaktivieren" : "§aaktivieren") + "§7.");
		final ItemMeta im = recorditem.getItemMeta();
		im.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		recorditem.setItemMeta(im);
		inv.setItem(16, rec ? ms.addGlow(recorditem) : recorditem);
		if (ms.getActiveInv(p.getName()) != InvType.CREATOR) p.closeInventory();
		p.openInventory(inv);
		ms.setActiveInv(p.getName(), InvType.CREATOR);
		return true;
	}
	public boolean isRecording(final Player p) {
		return recording.contains(p.getUniqueId());
	}
	public void toggleRecording(final UUID uuid) {
		if (recording.remove(uuid) == false) recording.add(uuid);
	}
	public void removeFlySpawn(final UUID uuid) {
		flyspawn.remove(uuid);
	}
	public boolean isFlySpawn(final UUID uuid) {
		return flyspawn.contains(uuid);
	}
	public boolean toggleFlySpawn(final UUID uuid) {
		if (flyspawn.remove(uuid) == false) {
			flyspawn.add(uuid);
			return true;
		}
		return false;
	}
}