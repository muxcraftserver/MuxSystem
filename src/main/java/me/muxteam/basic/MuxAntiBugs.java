package me.muxteam.basic;

import com.sk89q.worldguard.bukkit.ConfigurationManager;
import com.sk89q.worldguard.bukkit.WGBukkit;
import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.Vec3D;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.Iterator;

public final class MuxAntiBugs implements Listener {
    private final ConfigurationManager cm = WGBukkit.getPlugin().getGlobalStateManager();
    private final MuxSystem ms;

    public MuxAntiBugs(final MuxSystem ms) {
        this.ms = ms;
        removeBuggyItems(ms.getServer());
        ms.getServer().getPluginManager().registerEvents(this, ms);
    }

    // BUGGY ITEMS
    private void removeBuggyItems(final Server sr) {
        final Iterator<Recipe> it = sr.recipeIterator();
        while (it.hasNext()) {
            final Recipe recipe = it.next();
            if (recipe == null) continue;
            final Material m = recipe.getResult().getType();
            if (m == Material.BOOK_AND_QUILL || m == Material.ARMOR_STAND || m == Material.STORAGE_MINECART || m == Material.HOPPER_MINECART) {
                it.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(final InventoryClickEvent e) {
        if (e.getClickedInventory() != null && e.getClickedInventory().getType() == InventoryType.ENCHANTING && e.getClick() == ClickType.NUMBER_KEY) {
            final Player p = (Player) e.getWhoClicked();
            final int slot = e.getHotbarButton();
            final ItemStack i = p.getInventory().getItem(slot);
            if (i == null || i.getAmount() == 1) return;
            e.setCancelled(true);
            ms.showItemBar(p, "§cLege deine Items ohne Hotkeys in den Enchanter.");
        }
    }

    // DUPLICATION FIX
    @EventHandler
    public void onDispense(final BlockDispenseEvent e) {
        if (e.getItem() != null && (e.getItem().getAmount() > 64 || e.getItem().getAmount() < 1)) {
            e.setCancelled(true);
        }
        final InventoryHolder d = (InventoryHolder) e.getBlock().getState();
        if (d != null && d.getInventory() != null) {
            for (final ItemStack i : d.getInventory().getContents()) {
                if (i != null && i.getType() != Material.AIR && (i.getAmount() < 1 || i.getAmount() > 64)) {
                    e.setCancelled(true);
                    break;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(final PlayerDropItemEvent e) {
        final ItemStack item = e.getItemDrop().getItemStack().clone();
        if (item.getAmount() > 64) {
            item.setAmount(64);
            e.getItemDrop().setItemStack(item);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemSpawn(final ItemSpawnEvent e) {
        if (e.getEntity().getItemStack().getAmount() < 1) {
            e.setCancelled(true);
        }
    }

    // DUPLICATION + PETS THROUGH PORTALS
    @EventHandler
    public void onPortal(final EntityPortalEvent e) {
        final EntityType en = e.getEntityType();
        if (en != EntityType.PLAYER) {
            e.setCancelled(true);
        }
    }

    // HIGH ENCHANT
    @EventHandler(ignoreCancelled = true)
    public void onBreak(final BlockBreakEvent e) {
        final Player p = e.getPlayer();
        final ItemStack hand = p.getItemInHand();
        if (hand != null && hand.getEnchantmentLevel(Enchantment.LOOT_BONUS_BLOCKS) > 5) {
            hand.removeEnchantment(Enchantment.LOOT_BONUS_BLOCKS);
            p.updateInventory();
            e.setCancelled(true);
        }
    }

    // SIGN CRASH FIX
    @EventHandler(ignoreCancelled = true)
    public void onSign(final SignChangeEvent e) {
        for (byte i = 0; i < e.getLines().length; i++) {
            if (e.getLine(i).length() > 50) {
                e.setCancelled(true);
                e.getPlayer().kickPlayer(ms.getLang("sign.toomanycharacters"));
            }
        }
    }

    // PISTON (SAND GENERATOR) FIX
    @EventHandler(ignoreCancelled = true)
    public void onPiston(final BlockPistonExtendEvent e) {
        for (final Block block : e.getBlocks()) {
            final Material m = block.getType();
            if (m.hasGravity()) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPiston(final BlockPistonRetractEvent e) {
        for (final Block b : e.getBlocks()) {
            final Material m = b.getType();
            if (m.hasGravity()) {
                e.setCancelled(true);
            }
        }
    }

    // ARCHITECTURE BREAK FIX
    @EventHandler(ignoreCancelled = true)
    public void onPhysics(final BlockPhysicsEvent e) {
        final Material m = e.getBlock().getType();
        if (m == Material.LONG_GRASS || m == Material.DOUBLE_PLANT || m == Material.RED_ROSE || m == Material.YELLOW_FLOWER || m == Material.SAPLING || m == Material.NETHER_WARTS || m == Material.CROPS ||
                m == Material.CARROT || m == Material.POTATO || m == Material.WOOD_DOOR || m == Material.IRON_DOOR || m == Material.WOODEN_DOOR || m == Material.ACACIA_DOOR || m == Material.BIRCH_DOOR ||
                m == Material.DARK_OAK_DOOR || m == Material.JUNGLE_DOOR || m == Material.SPRUCE_DOOR ||
                // BEDROCK BREAK FIX
                m == Material.DRAGON_EGG || m == Material.TRAP_DOOR) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityChangeBlock(final EntityChangeBlockEvent e) {
        if (e.getBlock().getType() == Material.DRAGON_EGG || e.getBlock().getType() == Material.TRAP_DOOR) {
            e.setCancelled(true);
        }
    }

    // NETHER ROOF & ANTI-CHEAT-BUG FIX
    @EventHandler(ignoreCancelled = true)
    public void onInteract(final PlayerInteractEvent e) {
        if (e.getClickedBlock() != null) {
            final Location l = e.getClickedBlock().getLocation();
            if (l.getWorld().getEnvironment() == Environment.NETHER && l.getY() >= 127.0D) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTeleport(final PlayerTeleportEvent e) {
        final Location l = e.getTo();
        if (l != null && l.getWorld().getEnvironment() == World.Environment.NETHER && l.getY() >= 127.0D) {
            e.setCancelled(true);
        }
    }

    // SORTING MACHINE FIX
    @EventHandler(ignoreCancelled = true)
    public void onHopperMoveEvent(final InventoryMoveItemEvent e) {
        if (e.getInitiator().getType() == InventoryType.HOPPER && cm.activityHaltToggle) {
            e.setCancelled(true);
        }
    }

    // HIT BETWEEN WALLS FIX
    @EventHandler
    public void onHit(final EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player) {
            final EntityPlayer ep = ((CraftPlayer) e.getDamager()).getHandle();
            final Entity en = ((CraftEntity) e.getEntity()).getHandle();
            if (ep.world.rayTrace(new Vec3D(ep.locX, ep.locY + ep.getHeadHeight(), ep.locZ),
                    new Vec3D(en.locX, en.locY + en.getHeadHeight(), en.locZ), false, true, false) != null &&
                    ep.world.rayTrace(new Vec3D(ep.locX, ep.locY + ep.getHeadHeight(), ep.locZ),
                            new Vec3D(en.locX, en.locY, en.locZ), false, true, false) != null) {
                e.setCancelled(true);
            }
        }
    }
}