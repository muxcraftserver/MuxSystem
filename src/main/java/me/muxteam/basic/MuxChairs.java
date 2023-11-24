package me.muxteam.basic;

import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MuxChairs implements Listener {
    private MuxSystem ms;
    private final ConcurrentHashMap<Location, ArmorStand> chairs = new ConcurrentHashMap<>();

    public MuxChairs(final MuxSystem ms) {
        this.ms = ms;
        ms.getServer().getPluginManager().registerEvents(this, ms);
    }
    public void close() {
        for (final ArmorStand chair : chairs.values()) {
            if (chair != null) {
                chair.remove();
            }
        }
        this.ms = null;
    }
    public ConcurrentMap<Location, ArmorStand> getChairs() {
        return chairs;
    }
    public void clearEmpty() {
        for (final Map.Entry<Location, ArmorStand> entry : chairs.entrySet()) {
            final ArmorStand a = entry.getValue();
            if (a == null || a.isDead() || a.getPassenger() == null) {
                if (a != null) a.remove();
                chairs.remove(entry.getKey());
            }
        }
    }
    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        final Action a = e.getAction();
        if (a == Action.RIGHT_CLICK_BLOCK) {
            final Block b = e.getClickedBlock();
            final Material t = b.getType();
            final Player p = e.getPlayer();
             if ((t == Material.DARK_OAK_STAIRS || t == Material.SPRUCE_WOOD_STAIRS || t == Material.WOOD_STAIRS) && p.isInsideVehicle() == false && p.getItemInHand().getType() == Material.AIR) {
                if ((b.getData() < 4 || (b.getData() > 7 && b.getData() < 12)) == false) return;
                if (ms.checkGeneralCooldown(p.getName(), "STAIRS", 3000L, true)) return;
                final Location l = b.getLocation();
                final ApplicableRegionSet rgs = WGBukkit.getRegionManager(l.getWorld()).getApplicableRegions(l);
                if (rgs.size() == 0 || rgs.allows(DefaultFlag.PVP)) return;
                float yaw = 0F;
                if (b.getData() == 1 || b.getData() == 9) {
                    yaw = -90F;
                } else if (b.getData() == 3 || b.getData() == 11) {
                    yaw = 0F;
                } else if (b.getData() == 0 || b.getData() == 8) {
                    yaw = 90F;
                } else if (b.getData() == 2 || b.getData() == 10) {
                    yaw = 180F;
                }
                l.setYaw(yaw);
                if (chairs.containsKey(l)) {
                    ms.showItemBar(p, "§cDieser Stuhl ist besetzt.");
                    return;
                } else if (p.isInsideVehicle()) {
                    return;
                }
                final ArmorStand stand = (ArmorStand) p.getWorld().spawnEntity(l.clone().add(0.5D, -1.2D, 0.5D), EntityType.ARMOR_STAND);
                stand.setGravity(false);
                stand.setVisible(false);
                stand.setPassenger(p);
                e.setCancelled(true);
                chairs.put(l, stand);
            }
        }
    }
}