package me.muxteam.ranks;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MuxKits {
	private MuxSystem ms;
	private Map<UUID, HashMap<String, Long>> kitcooldowns = new HashMap<>();

	public MuxKits(final MuxSystem ms) {
		this.ms = ms;
		final Gson gson = new Gson();
		final FileConfiguration hashYML = ms.getHashYAML();
		if (hashYML.contains("kitcooldowns")) {
			kitcooldowns = gson.fromJson(hashYML.getString("kitcooldowns"), new TypeToken<HashMap<UUID, HashMap<String, Long>>>() { }.getType());
		}
	}
	public void close() {
		final long currentime = System.currentTimeMillis();
		kitcooldowns.entrySet().removeIf(entry -> entry.getValue().entrySet().removeIf(entry2 -> entry2.getValue() + getCooldownTime(entry2.getKey()) < currentime) && entry.getValue().isEmpty());
		saveKits(false);
		this.ms = null;
	}

	public String checkKitCooldown(final UUID uuid, final String type, final boolean save) {
		final HashMap<String, Long> map = kitcooldowns.getOrDefault(uuid, new HashMap<>());
		final long cooldown = getCooldownTime(type);
		final Long current = map.get(type);
		if (current != null && current + cooldown > System.currentTimeMillis()) {
			return ms.getTime((int) ((current + cooldown - System.currentTimeMillis()) / 1000));
		}
		if (save) {
			map.put(type, System.currentTimeMillis());
			kitcooldowns.put(uuid, map);
		}
		return null;
	}
	public boolean giveKit(final Player p, final Material kit, final boolean save) {
		final PlayerInventory pi = p.getInventory();
		if (pi.firstEmpty() == -1) {
			ms.showItemBar(p, ms.getLang("invfull"));
			return false;
		}
		switch (kit) {
		case IRON_CHESTPLATE: // Starter Kit
			pi.addItem(new ItemStack(Material.IRON_HELMET));
			pi.addItem(new ItemStack(Material.IRON_CHESTPLATE));
			pi.addItem(new ItemStack(Material.IRON_LEGGINGS));
			pi.addItem(new ItemStack(Material.IRON_BOOTS));
			final ItemStack iS = new ItemStack(Material.IRON_SWORD);
			iS.addEnchantment(Enchantment.DAMAGE_ALL, 2);
			iS.addEnchantment(Enchantment.FIRE_ASPECT, 2);
			pi.addItem(iS);
			final ItemStack iB = new ItemStack(Material.BOW);
			iB.addEnchantment(Enchantment.ARROW_DAMAGE, 1);
			if (p.hasPermission("muxsystem.elite") == false) pi.addItem(iB);
			pi.addItem(new ItemStack(Material.IRON_PICKAXE));
			pi.addItem(new ItemStack(Material.IRON_AXE));
			pi.addItem(new ItemStack(Material.IRON_SPADE));
			pi.addItem(new ItemStack(Material.GOLDEN_APPLE, 5));
			if (p.hasPermission("muxsystem.elite") == false) pi.addItem(new ItemStack(Material.ARROW, 32));
			if (save) checkKitCooldown(p.getUniqueId(), "Starter",  true);
			break;
		case DIAMOND_CHESTPLATE:
			pi.addItem(new ItemStack(Material.DIAMOND_HELMET));
			pi.addItem(new ItemStack(Material.DIAMOND_CHESTPLATE));
			pi.addItem(new ItemStack(Material.DIAMOND_LEGGINGS));
			pi.addItem(new ItemStack(Material.DIAMOND_BOOTS));
			pi.addItem(new ItemStack(Material.DIAMOND_SWORD));
			pi.addItem(new ItemStack(Material.DIAMOND_PICKAXE));
			pi.addItem(new ItemStack(Material.DIAMOND_AXE));
			pi.addItem(new ItemStack(Material.DIAMOND_SPADE));
			final ItemStack iB2 = new ItemStack(Material.BOW);
			iB2.addEnchantment(Enchantment.ARROW_DAMAGE, 2);
			pi.addItem(iB2);
			pi.addItem(new ItemStack(Material.ARROW, 32));
			checkKitCooldown(p.getUniqueId(), "Tools", true);
			ms.checkLimit(p.getName(), "Tools", 4, true);
			break;
		case BREWING_STAND_ITEM:
			pi.addItem(new ItemStack(Material.POTION, 16, (short) 16421));
			pi.addItem(new ItemStack(Material.POTION, 16, (short) 16417));
			pi.addItem(new ItemStack(Material.POTION, 16, (short) 16418));
			pi.addItem(new ItemStack(Material.POTION, 16, (short) 16425));
			pi.addItem(new ItemStack(Material.POTION, 16, (short) 16451));
			checkKitCooldown(p.getUniqueId(), "Potion", true);
			break;
		default: return true;
		}
		saveKits(true);
		return true;
	}
	private void saveKits(final boolean async) {
		final Gson gson = new Gson();
		ms.getHashYAML().set("kitcooldowns", gson.toJson(kitcooldowns));
		if (async == false) {
			ms.saveHashFile();
			return;
		}
		new BukkitRunnable() {
			@Override
			public void run() {
				ms.saveHashFile();
			}
		}.runTaskAsynchronously(ms);
	}
	private long getCooldownTime(final String type) {
		switch (type) {
		case "Heal":
		case "Tools":
		case "Potion":
			return 3600000L;
		case "UltraArmor":
		case "UltraSword":
		case "UltraApple":
			return 86400000L;
		case "UltraTools":
		case "Creator":
			return 172800000L;
		case "UltraPotato":
			return 259200000L;
		default:
			return 0L;
		}
	}
}