package me.muxteam.shop;

import me.muxteam.basic.NMSReflection;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.Tuple;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftArmorStand;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class MuxMining implements Listener {
    private final MuxSystem ms;
    private final List<Tuple<ArmorStand, Long>> mineHoloList = new ArrayList<>();
    private final Set<UUID> noholos = new HashSet<>();

    public MuxMining(final MuxSystem ms) {
        this.ms = ms;
        Arrays.stream(OreTypes.values()).forEach(type -> oreTypes.put(type.material, type));
        ms.getServer().getPluginManager().registerEvents(this, ms);
    }

    public void clearHolos() {
        mineHoloList.removeIf(tuple -> {
            if (tuple.b() <= System.currentTimeMillis()) {
                tuple.a().remove();
                return true;
            }
            return false;
        });
    }

    public void handleBreak(final Player p, final Block b, final ItemStack hand) {
        final Material m = b.getType();
        final OreTypes type = oreTypes.get(m);
        if (b.getWorld() == ms.getFarmWorld() && type != null) {
            if (ms.hasVoted(p) == false) {
                if (ms.checkGeneralCooldown(p.getName(), "ORENEEDVOTE", 30000L, true)) {
                    ms.chatClickHoverLink(p, "§6§lMuxCraft>§7 Vote§7, §7um §7beim §7Erze §7Minen §eMuxCoins §7zu §7erhalten.", ms.getLang("vote.hoverunlock"), "https://muxcraft.eu/vote/?ign=" + p.getName());
                    ms.chatClickHoverLink(p, ms.getLang("vote.tounlock2"), ms.getLang("vote.hoverunlock"), "https://muxcraft.eu/vote/?ign=" + p.getName());
                }
                return;
            }
            if (hand != null && hand.getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0) return;
            if (ms.getActiveMuxCoinsSupply() > 0) {
                final MuxUser u = ms.getMuxUser(p.getName());
                int coins = (int) (ms.getActiveMuxCoinsSupply() * type.percentage / 100);
                if (coins < 1) coins = 1;
                if (ms.hasGold(p.getUniqueId()))
                    coins *= 2;
                u.addCoins(coins);
                ms.getHistory().addCoinHistory(u.getUUID(), "Mining", coins);
                ms.getAnalytics().addMineGenerated(coins);
                ms.sendScoreboard(p);
                if (noholos.contains(p.getUniqueId())) {
                    ms.showItemBar(p, "§e+" + coins + " MuxCoin" + (coins > 1 ? "s" : ""));
                } else {
                    spawnTempHologram(b.getLocation(), "§e+" + coins + " MuxCoin" + (coins > 1 ? "s" : ""));
                }
            }
        }
    }

    // 1.8 HOLOGRAM FIX
    @EventHandler
    public void onInteractAtEntity(final PlayerInteractAtEntityEvent e) {
        final org.bukkit.entity.Entity clicked = e.getRightClicked();
        if (clicked.getWorld() != ms.getFarmWorld() || clicked instanceof ArmorStand == false) return;
        final ArmorStand armorStand = (ArmorStand) e.getRightClicked();
        final String cname = armorStand.getCustomName();
        if (cname != null && cname.contains("MuxCoins")) {
            noholos.add(e.getPlayer().getUniqueId());
        }
    }

    private void spawnTempHologram(final Location loc, final String text) {
        final ArmorStand dam = (ArmorStand) loc.getWorld().spawnEntity(getDisplayLoc(loc.add(0.5, 0, 0.5)), EntityType.ARMOR_STAND);
        dam.setMarker(true);
        dam.setVisible(false);
        dam.setCustomNameVisible(true);
        dam.setCustomName(text);
        dam.setGravity(false);
        EntityArmorStand handle = ((CraftArmorStand) dam).getHandle();
        NMSReflection.setObject(Entity.class, "uniqueID", handle, ms.getIdentificationId());
        NMSReflection.setObject(Entity.class, "invulnerable", handle, true);
        mineHoloList.add(new Tuple<>(dam, System.currentTimeMillis() + 2000));
    }
    private Location getDisplayLoc(final Location from) {
        final Location l = from.clone();
        final Block b = l.getBlock();
        if (b.getRelative(0, 1, 0).getType() == Material.AIR) {
            return l.add(0, 1, 0);
        } else if (b.getRelative(0, -1, 0).getType() == Material.AIR) {
            return l.add(0, -1, 0);
        } else if (b.getRelative(1, 0, 0).getType() == Material.AIR) {
            return l.add(1, 0, 0);
        } else if (b.getRelative(-1, 0, 0).getType() == Material.AIR) {
            return l.add(-1, 0, 0);
        } else if (b.getRelative(0, 0, 1).getType() == Material.AIR) {
            return l.add(0, 0, 1);
        } else if (b.getRelative(0, 0, -1).getType() == Material.AIR) {
            return l.add(0, 0, -1);
        }
        return from;
    }
    private final Map<Material, OreTypes> oreTypes = new EnumMap<>(Material.class);

    public enum OreTypes {
        IRON(0.00001D, Material.IRON_ORE),
        GOLD(0.00003D, Material.GOLD_ORE),
        EMERALD(0.0001D, Material.EMERALD_ORE),
        DIAMOND(0.00005D, Material.DIAMOND_ORE);

        private final double percentage;
        private final Material material;

        OreTypes(final double percentage, final Material material) {
            this.percentage = percentage;
            this.material = material;
        }

        public double getPercentage() {
            return percentage;
        }

        public Material getMaterial() {
            return material;
        }
    }
}