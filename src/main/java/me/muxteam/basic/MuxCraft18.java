package me.muxteam.basic;

import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.ContainerEnchantTable;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldBorder.EnumWorldBorderAction;
import net.minecraft.server.v1_8_R3.WorldBorder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftInventoryView;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

public final class MuxCraft18 implements Listener {
	private MuxSystem ms;
	private final Random r = new Random();

	public MuxCraft18(final MuxSystem ms) {
		this.ms = ms;
		ms.getServer().getPluginManager().registerEvents(this, ms);
	}
	public void close() {
		this.ms = null;
	}

	// OLD ENCHANTING

	@EventHandler
	public void onInvClose(final InventoryCloseEvent e) { // Remove lapis, so it doesn't drop on the ground.
		if (e.getInventory().getType() == InventoryType.ENCHANTING) {
			e.getInventory().setItem(1, null);
		}
	}
	@EventHandler
	public void onInvOpen(final InventoryOpenEvent e) { // Give lapis
		if (e.getInventory().getType() == InventoryType.ENCHANTING) {
			e.getInventory().setItem(1, new ItemStack(Material.INK_SACK, 64, (short) 4));
		}
	}
	@EventHandler(ignoreCancelled = true)
	public void onInvClick(final InventoryClickEvent e) { // Prevent them from stealing lapis
		if (e.getClickedInventory() != null) {
			if (e.getClickedInventory().getType() == InventoryType.ENCHANTING) {
				if (e.getRawSlot() == 1) {
					e.setCancelled(true);
					((Player) e.getWhoClicked()).updateInventory();
				}
			}
		}
		if (e.getInventory().getType() == InventoryType.BREWING) { 	// DISABLE JUMP POTION
			if (e.getCurrentItem() != null && e.getCurrentItem().getType() == Material.RABBIT_FOOT || e.getAction() == InventoryAction.HOTBAR_SWAP) {
				e.setCancelled(true);
			}
		}
	}
	@EventHandler
	public void onEnchant(final EnchantItemEvent e) { // Fix up removing 1,2,3 levels depending on tier, and restock.
		new BukkitRunnable() {
			@Override public void run() {
				final ItemStack item = e.getInventory().getItem(1);
				if (item == null) return;
				e.getEnchanter().setLevel(e.getEnchanter().getLevel() - (e.getExpLevelCost() - (64 - item.getAmount())));
				e.getInventory().setItem(1, new ItemStack(Material.INK_SACK, 64, (short) 4));
			}
		}.runTaskLater(ms, 1L);
	}
	@EventHandler
	public void onPrepareEnchant(final PrepareItemEnchantEvent e) {
		if (e.getItem().getAmount() > 1) {
			e.getItem().setAmount(1);
		}
		final Material type = e.getItem().getType();
		if (type == Material.DIAMOND_HOE || type == Material.IRON_HOE || type == Material.GOLD_HOE || type == Material.STONE_HOE || type == Material.WOOD_HOE) {
			return;
		}
		final CraftInventoryView view = (CraftInventoryView) e.getView();
		final ContainerEnchantTable table = (ContainerEnchantTable) view.getHandle();
		final int books = Math.min(e.getEnchantmentBonus(), 15); // Books
		final int base = r.nextInt(8) + 1 + (books >> 1) + r.nextInt(books + 1); // Fixed
		table.costs[0] = Math.max(base / 3, 1);
		table.costs[1] = base * 2 / 3 + 1;
		table.costs[2] = Math.max(base, books * 2); // Fixed
		new BukkitRunnable() { // Remove the display of what enchantment you will get
			@Override public void run() {
				table.h[0] = -1;
				table.h[1] = -1;
				table.h[2] = -1;
			}
		}.runTaskLater(ms, 1L);
	}

	// DISABLE JUMP POTION

	@EventHandler
	public void onBrew(final BrewEvent e) {
		if (e.getContents().contains(Material.RABBIT_FOOT)) {
			e.setCancelled(true);
		}
	}

	// OLD FISHING

	@EventHandler
	public void onFish(final PlayerFishEvent e) {
		if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
			e.setCancelled(true);
			if (ms.checkGeneralCooldown(e.getPlayer().getName(), "FISH", 500L, true)) return;
			final Location l = e.getHook().getLocation(), l2 = e.getPlayer().getLocation();
			final double x = l2.getX() - l.getX(), y = l2.getY() - l.getY(), z = l2.getZ() - l.getZ();
			final Item item = (Item) e.getCaught();
			final ItemStack is = item.getItemStack();
			final short dur = is.getDurability();
			l.getWorld().dropItem(e.getHook().getLocation(), is.getType() == Material.INK_SACK ? is : new ItemStack(Material.RAW_FISH, 1, dur > 3 ? 0 : dur)).setVelocity(new Vector(x * 0.1D, y * 0.1D + Math.pow(x * x + y * y + z * z, 0.25D) * 0.08D, z * 0.1D));
		}
	}

	// OLD STACKING

	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void onPickup(final PlayerPickupItemEvent e) {
		if (e.getItem().hasMetadata("amount")) {
			e.setCancelled(true);
			final ItemStack is = e.getItem().getItemStack();
			is.setAmount(e.getItem().getMetadata("amount").get(0).asInt());
			e.getItem().remove();
			e.getPlayer().getInventory().addItem(is);
		}
	}
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void onDrop(final PlayerDropItemEvent e) {
		if (e.getItemDrop().getItemStack().getMaxStackSize() < e.getItemDrop().getItemStack().getAmount()) {
			e.getItemDrop().setMetadata("amount", new FixedMetadataValue(ms, e.getItemDrop().getItemStack().getAmount()));
		}
	}
	@EventHandler(ignoreCancelled = true)
	public void onBreak(final BlockBreakEvent e) {
		if (e.getBlock().getType() == Material.CHEST || e.getBlock().getType() == Material.TRAPPED_CHEST) {
			final Chest ch = (Chest) e.getBlock().getState();
			for (final ItemStack item : ch.getInventory().getContents()) {
				if (item != null && item.getMaxStackSize() < item.getAmount()) {
					e.getBlock().getWorld().dropItem(e.getBlock().getLocation(), item).setMetadata("amount", new FixedMetadataValue(ms, item.getAmount()));
					ch.getInventory().remove(item);
				}
			}
		}
	}
	// INVISIBLE ARMOR STANDS
	@EventHandler
	public void onArmorStand(final PlayerArmorStandManipulateEvent e) {
		if (e.getRightClicked().isVisible() == false) {
			e.setCancelled(true);
		}
	}
	// BLOOD EFFECT
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
	public void onDamage(final EntityDamageEvent e) {
		if (e.getEntity() instanceof Player) {
			final Player p = (Player) e.getEntity();
			if (ms.inEvent(p))
				return;
			int health = (int) p.getHealth();
			if (health <= 15 && ms.getMuxUser(p.getName()).getSettings().hasBloodEffect()) {
				final int maxhealth = (int) p.getMaxHealth();
				health = (int) (health - e.getDamage());
				sendBorder(p, (health * 100) / maxhealth);
			}
		}
	}
	@EventHandler
	public void onInteract(final PlayerInteractEvent e) {
		final Player p = e.getPlayer();
		if (ms.inEvent(p) || ms.getMuxUser(p.getName()).getSettings().hasBloodEffect() == false) return;
		final Material m = p.getLocation().getBlock().getType();
		if (ms.inBattle(p.getName(), p.getLocation()) == false || (e.getItem() != null && e.getItem().getType() == Material.GOLDEN_APPLE && (m == Material.WEB || m == Material.GOLD_PLATE || m == Material.IRON_PLATE))) {
			removeBorder(p);
		}
	}
	private void sendBorder(final Player p, int percentage) {
		percentage = Math.round(percentage / 2F);
		setBorder(p, percentage);
		fadeBorder(p, percentage, 5);
	}
	private void removeBorder(final Player p) {
		sendWorldBorderPacket(p, 0, 200000.0D, 200000.0D, 0L);
	}
	private void fadeBorder(final Player p, final int percentage, final long time) {
		final int dist = -10000 * percentage + 1300000;
		sendWorldBorderPacket(p, 0, 200000.0D, dist, 1000L * time + 4000L);
	}
	private void setBorder(final Player p, final int percentage) {
		final int dist = -10000 * percentage + 1300000;
		sendWorldBorderPacket(p, dist, 200000.0D, 200000.0D, 0L);
	}
	private void sendWorldBorderPacket(final Player p, final int dist, final double oldradius, final double newradius, final long delay) {
		final WorldBorder worldborder = new WorldBorder();
		worldborder.setCenter(p.getLocation().getX(), p.getLocation().getY());
		worldborder.setSize(dist);
		worldborder.setWarningDistance(dist);
		worldborder.setWarningTime(15);
		worldborder.transitionSizeBetween(oldradius, newradius, delay);
		((CraftPlayer)p).getHandle().playerConnection.sendPacket(new net.minecraft.server.v1_8_R3.PacketPlayOutWorldBorder(worldborder, EnumWorldBorderAction.INITIALIZE));
	}
}