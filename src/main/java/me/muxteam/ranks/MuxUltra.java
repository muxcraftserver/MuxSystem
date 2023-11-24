package me.muxteam.ranks;

import me.muxteam.extras.MuxMounts;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

public final class MuxUltra {
	private MuxSystem ms;
	private MuxKits kits;

	public MuxUltra(final MuxSystem ms) {
		this.ms = ms;
		this.kits = ms.getKits();
	}
	public void close() {
		this.kits = null;
		this.ms = null;
	}

	public boolean isUltraItem(final ItemStack i) {
		return i.hasItemMeta() && (i.getItemMeta().hasLore() && i.getItemMeta().getLore().removeIf(s -> s.contains("exklusiv nur für dich")));
	}
	public boolean handleCommand(final Player p) {
		if (p.hasPermission("muxsystem.ultra") == false) {
			ms.sendNoRankMessage(p, "ULTRA");
			return true;
		} else if (ms.getAdmin().ULTRA.isActive() == false) {
			ms.showItemBar(p, "§cDer Befehl ist ab §6morgen früh §cfür alle verfügbar.");
			return true;
		}
		final UUID uuid = p.getUniqueId();
		final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxUltra§0 | /ultra");
		final boolean fulltrusted = ms.isFullTrusted(p.getName());
		String time = kits.checkKitCooldown(uuid, "UltraArmor", false);
		inv.setItem(10, ms.addLore(ms.renameItemStack(new ItemStack(Material.DIAMOND_CHESTPLATE),
				"§b§lKampfrüstung", "§7Bekomme hier eine mit Schutz IV", "§7verzauberte Diamantrüstung.", ""),
				time == null || fulltrusted ? new String[] { "§7Klicke, um die Rüstung zu §aerhalten§7."} :
					new String[] {"§cWarte §6" + time + " §cum diese Rüstung", "§cwieder zu erhalten."}));
		time = kits.checkKitCooldown(uuid, "UltraSword", false);
		inv.setItem(12, ms.addLore(ms.renameItemStack(new ItemStack(Material.DIAMOND_SWORD),
				"§b§lKampfschwert", "§7Du bekommst passend zur Rüstung", "§7ein Schärfe V-Diamantschwert.", ""),
				time == null || fulltrusted ? new String[] {"§7Klicke, um das Schwert zu §aerhalten§7."} :
					new String[] {"§cWarte §6" + time + " §cum diesen Schwert", "§cwieder zu erhalten."}));
		time = kits.checkKitCooldown(uuid, "UltraApple", false);
		inv.setItem(14, ms.addLore(ms.renameItemStack(new ItemStack(Material.GOLDEN_APPLE, 1, (byte) 1),
				"§e§lOP-Äpfel", "§7Wenn du sie mit wenig Leben isst,", "§7wirst du rasend schnell geheilt.", ""),
				time == null || fulltrusted ? new String[] {"§7Klicke, um die Äpfel zu §aerhalten§7."} :
					new String[] {"§cWarte §6" + time + " §cum die Äpfel", "§cwieder zu erhalten."}));
		time = kits.checkKitCooldown(uuid, "UltraPotato", false);
		inv.setItem(16, ms.addLore(ms.renameItemStack(new ItemStack(Material.POTATO_ITEM),
				"§6§lMuxKartoffel", "§7Mit dem Klassiker werden die Gegner", "§7im Kampf meilenweit fliegen.", ""),
				time == null || fulltrusted ? new String[] {"§7Klicke, um die Kartoffel zu §aerhalten§7."} :
					new String[] {"§cWarte §6" + time + " §cum diese Kartoffel", "§cwieder zu erhalten."}));
		time = kits.checkKitCooldown(uuid, "UltraTools", false);
		inv.setItem(30, ms.addLore(ms.renameItemStack(new ItemStack(Material.DIAMOND_PICKAXE),
				"§b§lSuper Tools", "§7Bekomme hier Werkzeuge mit", "§7extremen Verzauberungen.", ""),
				time == null || fulltrusted ? new String[] {"§7Klicke, um die Tools zu §aerhalten§7."} :
						new String[] {"§cWarte §6" + time + " §cum die Tools", "§cwieder zu erhalten."}));
		final boolean mounted = ms.getMounts().getMountType(p.getUniqueId()) == (short) 9;
		final ItemStack bike = ms.addLore(ms.renameItemStack(new ItemStack(Material.IRON_BARDING),
						"§6§lBike" + (mounted ? "§a §laktiviert" : ""), "§7Dieses Mount ist exklusiv und man", "§7kann sogar mit ihm springen.", ""),
				mounted ? new String[] {"§7Klicke, um den Mount zu §cdeaktivieren§7."} :
						new String[] {"§7Klicke, um den Mount zu §aaktivieren§7."});
		inv.setItem(32, mounted ? ms.addGlow(bike) : bike);
		inv.setItem(44, ms.renameItemStack(new ItemStack(Material.BUCKET), "§f§lMuxAbfall",
				"§7Die Ultra-Items lassen sich damit", "§7aus deinem Inventar entfernen.", "", "§7Klicke, um den Abfall zu §föffnen§7."));
		final InvType it = ms.getActiveInv(p.getName());
		if (it != InvType.ULTRA && it != InvType.MENU) p.closeInventory();
		p.openInventory(inv);
		ms.setActiveInv(p.getName(), InvType.ULTRA);
		return true;
	}
	public void handleInventory(final Player p, final ItemStack i) {
		if (i.getItemMeta().hasLore() && i.getItemMeta().getLore().size() > 3 && i.getItemMeta().getLore().get(3).startsWith("§c")) {
			p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
			p.updateInventory();
			return;
		}
		final Material m = i.getType();
		switch (m) {
		case DIAMOND_CHESTPLATE:
		case DIAMOND_SWORD:
		case GOLDEN_APPLE:
		case POTATO_ITEM:
		case DIAMOND_PICKAXE:
			p.closeInventory();
			if (giveKit(p, m) == false) {
				p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
				return;
			}
			p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.5F, 1F);
			break;
		case IRON_BARDING:
			if (ms.inBattle(p.getName(), p.getLocation())) {
				ms.showItemBar(p, "§cMounts sind im Kampf deaktiviert.");
				p.closeInventory();
				return;
			} else if (ms.inCasino(p)) {
				ms.showItemBar(p, "§cMounts sind hier deaktiviert.");
				p.closeInventory();
				return;
			}
			final MuxMounts.MountStore mountStore = ms.getExtras().getExtraUser(p.getUniqueId()).getMounts();
			ms.getMounts().setMount(p, (short) 9, true);
			mountStore.setActive(mountStore.getActive() == 9 ? -1 : (short) 9);
			ms.getExtras().getExtraUser(p.getUniqueId()).setMounts(mountStore);
			p.performCommand("ultra");
			p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
			break;
		case BUCKET:
			p.performCommand("d");
			p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
			break;
		default: break;
		}
	}
	private boolean giveKit(final Player p, final Material kit) {
		final PlayerInventory pi = p.getInventory();
		if (pi.firstEmpty() == -1) {
			ms.showItemBar(p, ms.getLang("invfull"));
			return false;
		}
		switch (kit) {
		case DIAMOND_CHESTPLATE:
			final ItemStack helmet = ms.renameItemStack(new ItemStack(Material.DIAMOND_HELMET), "§7[§c§lULTRA§7] §fDiamanthelm", "", "§7Dieses ULTRA-Item ist", "§7exklusiv nur für dich.");
			helmet.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
			helmet.addEnchantment(Enchantment.DURABILITY, 1);
			final ItemStack chestpl = ms.renameItemStack(new ItemStack(Material.DIAMOND_CHESTPLATE), "§7[§c§lULTRA§7] §fDiamantbrustpanzer", "", "§7Dieses ULTRA-Item ist", "§7exklusiv nur für dich.");
			chestpl.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
			chestpl.addEnchantment(Enchantment.DURABILITY, 1);
			final ItemStack leggings = ms.renameItemStack(new ItemStack(Material.DIAMOND_LEGGINGS), "§7[§c§lULTRA§7] §fDiamantbeinschutz", "", "§7Dieses ULTRA-Item ist", "§7exklusiv nur für dich.");
			leggings.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
			leggings.addEnchantment(Enchantment.DURABILITY, 1);
			final ItemStack boots = ms.renameItemStack(new ItemStack(Material.DIAMOND_BOOTS), "§7[§c§lULTRA§7] §fDiamantstiefel", "", "§7Dieses ULTRA-Item ist", "§7exklusiv nur für dich.");
			boots.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
			boots.addEnchantment(Enchantment.DURABILITY, 1);
			pi.addItem(helmet);
			pi.addItem(chestpl);
			pi.addItem(leggings);
			pi.addItem(boots);
			kits.checkKitCooldown(p.getUniqueId(), "UltraArmor", true);
			ms.showItemBar(p, "§fDu hast eine §bKampfrüstung§f soeben §aerhalten§f.");
			break;
		case DIAMOND_PICKAXE:
			final ItemStack pick = ms.renameItemStack(new ItemStack(Material.DIAMOND_PICKAXE), "§7[§c§lULTRA§7] §fSuper Spitzhacke", "", "§7Dieses ULTRA-Item ist", "§7exklusiv nur für dich.");
			pick.addUnsafeEnchantment(Enchantment.DIG_SPEED, 6);
			final ItemStack axe = ms.renameItemStack(new ItemStack(Material.DIAMOND_AXE), "§7[§c§lULTRA§7] §fSuper Axt", "", "§7Dieses ULTRA-Item ist", "§7exklusiv nur für dich.");
			axe.addUnsafeEnchantment(Enchantment.DIG_SPEED, 6);
			final ItemStack shovel = ms.renameItemStack(new ItemStack(Material.DIAMOND_SPADE), "§7[§c§lULTRA§7] §fSuper Schaufel", "", "§7Dieses ULTRA-Item ist", "§7exklusiv nur für dich.");
			shovel.addUnsafeEnchantment(Enchantment.DIG_SPEED, 6);
			pi.addItem(pick);
			pi.addItem(axe);
			pi.addItem(shovel);
			kits.checkKitCooldown(p.getUniqueId(), "UltraTools", true);
			ms.showItemBar(p, "§fDu hast die §bSuper Tools§f soeben §aerhalten§f.");
			break;
		case DIAMOND_SWORD:
			final ItemStack sword = ms.renameItemStack(new ItemStack(Material.DIAMOND_SWORD), "§7[§c§lULTRA§7] §fDiamantschwert", "", "§7Dieses ULTRA-Item ist", "§7exklusiv nur für dich.");
			sword.addEnchantment(Enchantment.DAMAGE_ALL, 5);
			pi.addItem(sword);
			kits.checkKitCooldown(p.getUniqueId(), "UltraSword", true);
			ms.showItemBar(p, "§fDu hast ein §bKampfschwert§f soeben §aerhalten§f.");
			break;
		case GOLDEN_APPLE:
			pi.addItem(ms.renameItemStack(new ItemStack(Material.GOLDEN_APPLE, 5, (byte) 1), "§7[§c§lULTRA§7] §fOP-Apfel", "§7Dieses ULTRA-Item ist", "§7exklusiv nur für dich."));
			kits.checkKitCooldown(p.getUniqueId(), "UltraApple", true);
			ms.showItemBar(p, "§fDu hast §e5x OP-Äpfel§f soeben §aerhalten§f.");
			break;
		case POTATO_ITEM:
			final ItemStack potato = ms.renameItemStack(new ItemStack(Material.POTATO_ITEM), "§7[§c§lULTRA§7] §fMuxKartoffel", "", "§7Dieses ULTRA-Item ist", "§7exklusiv nur für dich.");
			potato.addUnsafeEnchantment(Enchantment.KNOCKBACK, 10);
			pi.addItem(potato);
			kits.checkKitCooldown(p.getUniqueId(), "UltraPotato", true);
			ms.showItemBar(p, "§fDu hast eine §6MuxKartoffel§f soeben §aerhalten§f.");
			break;
		default: return true;
		}
		return true;
	}
}