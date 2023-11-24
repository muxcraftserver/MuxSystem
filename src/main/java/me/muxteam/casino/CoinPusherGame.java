package me.muxteam.casino;

import me.muxteam.basic.MuxHolograms;
import me.muxteam.basic.NMSReflection;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityVillager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftVillager;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CoinPusherGame {
    protected final int gamenr, stake;
    private int bankAccount;
    private boolean isPushingBack = true;
    private double cooldown = 1D;
    private final Random r = new Random();
    protected final Block button;
    private Player currentPlayer = null;
    private final Map<Villager, ArmorStand> illustration1 = new ConcurrentHashMap<>(), illustration2 = new ConcurrentHashMap<>(), illustration3 = new ConcurrentHashMap<>();
    private final ArmorStand p1, p2, p3, p4, p5, p6;
    private final List<ArmorStand> construct = new ArrayList<>();
    private final Location pusherLocation, redstoneBlock, scaledLocation;
    private final MuxHolograms.Hologram hologram;
    private final Map<Villager, Integer> values = new HashMap<>();
    private final MuxCasino c;

    protected CoinPusherGame(final MuxCasino c, final Location loc, final Location redstone, final int gamenr, final int stake) {
        this.stake = stake;
        this.c = c;
        this.gamenr = gamenr;
        this.redstoneBlock = redstone;
        for (int x = redstoneBlock.getBlockX() - 16; x < redstoneBlock.getBlockX() + 16; x++) {
            for (int z = redstone.getBlockZ() - 16; z < redstoneBlock.getBlockZ() + 16; z++) {
                final Chunk chunk = redstoneBlock.getWorld().getChunkAt(x, z);
                c.ms.getHolograms().saveChunk(new Location(c.getWorld(), x, 0, z));
                if (chunk.isLoaded() == false) chunk.load();
                redstoneBlock.getWorld().getNearbyEntities(redstoneBlock, 30, 30, 30).forEach(entity -> {
                    if (entity instanceof Villager) entity.remove();
                });
            }
        }
        this.scaledLocation = redstone.clone().add(5, 0, -3);
        this.pusherLocation = loc.clone();
        this.button = loc.clone().add(1, 0, 0).getBlock();
        this.hologram = c.ms.getHolograms().addHologram("coinpusher" + gamenr, loc.clone().add(0, 0, 0), "§cLade...");

        final EulerAngle blockHandPose = new EulerAngle(Math.toRadians(-43), Math.toRadians(-41.5), Math.toRadians(19.5));
        final List<Double> list = new ArrayList<>();
        list.add(0.0);
        list.add(0.2);
        list.add(-0.18);
        list.add(-0.4 + 0.02);
        list.add(-0.55);
        list.add(-0.7);
        final double axis = 0.45D;
        for (final double s : list) {
            // Right
            spawnArmorStand(pusherLocation.clone().add(0.45, 0.35 + s, -0.75), blockHandPose, Material.WOOD, (short) 5);
            spawnArmorStand(pusherLocation.clone().add(0.25, 0.35 + s, -0.75), blockHandPose, Material.WOOD, (short) 5);
            spawnArmorStand(pusherLocation.clone().add(0.08, 0.35 + s, -0.75), blockHandPose, Material.WOOD, (short) 5);
            spawnArmorStand(pusherLocation.clone().add(-0.1, 0.35 + s, -0.75), blockHandPose, Material.WOOD, (short) 5);
            spawnArmorStand(pusherLocation.clone().add(-0.3, 0.35 + s, -0.75), blockHandPose, Material.WOOD, (short) 5);
            spawnArmorStand(pusherLocation.clone().add(-0.48, 0.35 + s, -0.75), blockHandPose, Material.WOOD, (short) 5);

            // Left
            spawnArmorStand(pusherLocation.clone().add(0.45, 0.35 + s, axis), blockHandPose, Material.WOOD, (short) 5);
            spawnArmorStand(pusherLocation.clone().add(0.25, 0.35 + s, axis), blockHandPose, Material.WOOD, (short) 5);
            spawnArmorStand(pusherLocation.clone().add(0.08, 0.35 + s, axis), blockHandPose, Material.WOOD, (short) 5);
            spawnArmorStand(pusherLocation.clone().add(-0.1, 0.35 + s, axis), blockHandPose, Material.WOOD, (short) 5);
            spawnArmorStand(pusherLocation.clone().add(-0.3, 0.35 + s, axis), blockHandPose, Material.WOOD, (short) 5);
            spawnArmorStand(pusherLocation.clone().add(-0.48, 0.35 + s, axis), blockHandPose, Material.WOOD, (short) 5);
        }
        spawnArmorStand(pusherLocation.clone().add(-0.55, 0.45, -0.55), blockHandPose, Material.COAL_BLOCK, (short) 0);
        spawnArmorStand(pusherLocation.clone().add(-0.55, 0.45, -0.35), blockHandPose, Material.COAL_BLOCK, (short) 0);
        spawnArmorStand(pusherLocation.clone().add(-0.55, 0.45, -0.25), blockHandPose, Material.COAL_BLOCK, (short) 0);
        spawnArmorStand(pusherLocation.clone().add(-0.55, 0.45, -0.05), blockHandPose, Material.COAL_BLOCK, (short) 0);
        spawnArmorStand(pusherLocation.clone().add(-0.55, 0.45, 0.15), blockHandPose, Material.COAL_BLOCK, (short) 0);
        spawnArmorStand(pusherLocation.clone().add(-0.55, 0.45, 0.27), blockHandPose, Material.COAL_BLOCK, (short) 0);
        p1 = spawnArmorStand(pusherLocation.clone().add(-0.55, 0.45, -0.55), blockHandPose, Material.COAL_BLOCK, (short) 0);
        p2 = spawnArmorStand(pusherLocation.clone().add(-0.55, 0.45, -0.35), blockHandPose, Material.COAL_BLOCK, (short) 0);
        p3 = spawnArmorStand(pusherLocation.clone().add(-0.55, 0.45, -0.25), blockHandPose, Material.COAL_BLOCK, (short) 0);
        p4 = spawnArmorStand(pusherLocation.clone().add(-0.55, 0.45, -0.05), blockHandPose, Material.COAL_BLOCK, (short) 0);
        p5 = spawnArmorStand(pusherLocation.clone().add(-0.55, 0.45, 0.15), blockHandPose, Material.COAL_BLOCK, (short) 0);
        p6 = spawnArmorStand(pusherLocation.clone().add(-0.55, 0.45, 0.27), blockHandPose, Material.COAL_BLOCK, (short) 0);

        // Behind & MuxLogo
        list.clear();
        list.add(-0.5);
        list.add(-0.68);
        list.add(-0.5);
        list.add(-0.34);
        list.add(-0.16);
        list.add(0.0);
        list.add(0.2);
        list.add(0.38);
        list.add(0.38 + 0.2);
        list.add(0.35 + 0.4);
        list.add(0.35 + 0.6);
        list.add(0.3 + 0.8);
        for (final double s : list) {
            spawnArmorStand(pusherLocation.clone().add(-0.62, 0.35 + (s), axis - 0.06), blockHandPose, Material.WOOD, (short) 5);
            spawnArmorStand(pusherLocation.clone().add(-0.62, 0.35 + (s), -0.51), blockHandPose, Material.WOOD, (short) 5);
            spawnArmorStand(pusherLocation.clone().add(-0.62, 0.35 + (s), -0.75 + 0.06), blockHandPose, Material.WOOD, (short) 5);
            spawnArmorStand(pusherLocation.clone().add(-0.62, 0.35 + (s), 0.21), blockHandPose, Material.WOOD, (short) 5);
            if (s == 0.38 || s == 0.38 + 0.2 || s == 0.35 + 0.6)
                spawnArmorStand(pusherLocation.clone().add(-0.52, 0.35 + (s), 0.05), blockHandPose, Material.EMERALD_BLOCK, (short) 0);
            spawnArmorStand(pusherLocation.clone().add(-0.62, 0.35 + (s), 0.05), blockHandPose, Material.WOOD, (short) 5);
            if (s == 0.38 + 0.2 || s == 0.35 + 0.4)
                spawnArmorStand(pusherLocation.clone().add(-0.52, 0.35 + (s), -0.15), blockHandPose, Material.EMERALD_BLOCK, (short) 0);
            spawnArmorStand(pusherLocation.clone().add(-0.62, 0.35 + (s), -0.15), blockHandPose, Material.WOOD, (short) 5);
            if (s == 0.38 || s == 0.38 + 0.2 || s == 0.35 + 0.6)
                spawnArmorStand(pusherLocation.clone().add(-0.52, 0.35 + (s), -0.32), blockHandPose, Material.EMERALD_BLOCK, (short) 0);
            spawnArmorStand(pusherLocation.clone().add(-0.62, 0.35 + (s), -0.32), blockHandPose, Material.WOOD, (short) 5);
        }
        this.bankAccount = 0;
        final AtomicInteger integer = new AtomicInteger();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (integer.getAndIncrement() == 30)
                    return;
                if (values.keySet().size() < 25) {
                    int value;
                    int min = stake / 5;
                    value = r.nextInt(stake - min) + min;
                    spawnVillager(value, false);
                    bankAccount -= value;
                } else {
                    if (values.keySet().size() < 30) {
                        int max = stake * 20;
                        int value = r.nextInt(max - stake) + stake;
                        if (value > stake * 20) {
                            value = stake * 20;
                        }
                        spawnVillager(value, false);
                        bankAccount -= value;
                    }
                }
            }
        }.runTaskTimer(c.ms, 1L, 5L);
    }

    protected void close() {
        for (final Villager a : illustration1.keySet()) {
            bankAccount += values.get(a);
        }
        for (final ArmorStand a : illustration2.values()) {
            a.remove();
        }
        for (final ArmorStand a : illustration3.values()) {
            a.remove();
        }
        for (final ArmorStand a : construct) {
            a.remove();
        }
        c.ms.getHolograms().removeHologram("coinpusher" + gamenr);
        currentPlayer = null;
    }

    private void spawnVillager(final int value, final boolean high) {
        pusherLocation.getWorld().playSound(pusherLocation, Sound.HORSE_ARMOR, 1F, 1F);
        final int s = r.nextInt(39) + 1;
        Location co = redstoneBlock.clone().add(5, 2, 1 - (s / 10.0));
        if (high == false) co = redstoneBlock.clone().add(5, 0, 1 - (s / 10.0));
        co.getWorld().loadChunk(co.getChunk());
        final Villager v = (Villager) co.getWorld().spawnEntity(co, EntityType.VILLAGER);
        final EntityVillager ev = ((CraftVillager) v).getHandle();
        NMSReflection.setObject(Entity.class, "uniqueID", ev, c.ms.getIdentificationId());
        v.damage(1);
        final Location to = getScaledLocation(v.getLocation());

        final ArmorStand a = spawnArmorStand(to, new EulerAngle(Math.toRadians(-20), Math.toRadians(0), Math.toRadians(0)),
                new ItemStack(Material.INK_SACK, 1, (short) (value > stake ? 5 : 13)), false);
        c.ms.removeSounds(v);

        v.setAdult();
        v.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, Integer.MAX_VALUE, 8));
        illustration1.put(v, a);
        values.put(v, value);

        if (value > stake) {
            final ArmorStand a1 = spawnArmorStand(to.clone().add(0, .06, 0),
                    new EulerAngle(Math.toRadians(-20), Math.toRadians(0), Math.toRadians(0)),
                    new ItemStack(Material.INK_SACK, 1, (short) (value > stake * 10 ? 5 : 13)), false);
            illustration2.put(v, a1);
            if (value > stake * 6) {
                final ArmorStand a2 = spawnArmorStand(to.clone().add(0, .12, 0),
                        new EulerAngle(Math.toRadians(-20), Math.toRadians(0), Math.toRadians(0)),
                        new ItemStack(Material.INK_SACK, 1, (short) (value > stake * 10 ? 5 : 13)), false);
                illustration3.put(v, a2);
            }
        }
    }

    protected void updateMachine() {
        if (isPushingBack) {
            isPushingBack = false;
            p1.teleport(p1.getLocation().clone().add(0.20, 0, 0));
            p2.teleport(p2.getLocation().clone().add(0.20, 0, 0));
            p3.teleport(p3.getLocation().clone().add(0.20, 0, 0));
            p4.teleport(p4.getLocation().clone().add(0.20, 0, 0));
            p5.teleport(p5.getLocation().clone().add(0.20, 0, 0));
            p6.teleport(p6.getLocation().clone().add(0.20, 0, 0));
            redstoneBlock.getBlock().setType(Material.REDSTONE_BLOCK);
            for (final Player p : c.getWorld().getPlayers()) {
                final Location pushloc = pusherLocation.clone().add(-0.5, 1, 0), backloc = pusherLocation.clone().add(5, 1, 0);
                if (p.getLocation().distance(pushloc) < 1D && p.getLocation().getX() > pushloc.getX()) {
                    final Vector v = backloc.toVector().subtract(pushloc.toVector());
                    p.setVelocity(v.normalize().multiply(1.1D));
                }
            }
        } else {
            isPushingBack = true;
            p1.teleport(p1.getLocation().clone().add(-0.20, 0, 0));
            p2.teleport(p2.getLocation().clone().add(-0.20, 0, 0));
            p3.teleport(p3.getLocation().clone().add(-0.20, 0, 0));
            p4.teleport(p4.getLocation().clone().add(-0.20, 0, 0));
            p5.teleport(p5.getLocation().clone().add(-0.20, 0, 0));
            p6.teleport(p6.getLocation().clone().add(-0.20, 0, 0));
            redstoneBlock.getBlock().setType(Material.AIR);
        }
    }

    protected void updateStartAmount() {
        if (bankAccount > stake) {
            if (values.size() < 25) {
                spawnVillager(stake, false);
                bankAccount -= stake;
            } else {
                if (values.size() < 30) {
                    int max = bankAccount;
                    if (bankAccount > stake * 20) max = stake * 20;
                    int value = r.nextInt(max - stake) + stake;
                    if (value > stake * 20) {
                        value = stake * 20;
                    }
                    spawnVillager(value, false);
                    bankAccount -= value;
                }
            }
        }
    }

    protected void tryToPlay(final Player p) {
        if (c.ms.getAntiLags().isStopLag()) {
            c.ms.showItemBar(p, "§cAutomat kann derzeit nicht genutzt werden.");
            return;
        }
        if (cooldown > 0.0D) {
            c.ms.showItemBar(p, "§cDieser Automat ist gerade in Benutzung.");
            return;
        }
        if (c.canBuy(p, stake, "CoinPusher")) {
            currentPlayer = p;
            cooldown = 7;
            c.ms.showItemBar(p, "§aEinsatz von §d" + c.ms.getNumberFormat(stake) + " Chips §agesetzt.");
            int value;
            int min = stake / 5;
            value = r.nextInt(stake - min) + min;
            c.getStats().COIN_PUSHER_GEWINN += stake;
            bankAccount += stake - value;
            spawnVillager(value, true);
            if (bankAccount > stake * 20) {
                final int max = stake * 20;
                min = stake * 5;
                value = r.nextInt(max - min) + min;
                spawnVillager(value, true);
                bankAccount -= value;
            }
            c.ms.getAnalytics().getAnalytics().addPlayedCasinoGame("Coin Pusher");
        }
    }

    protected void update() {
        for (final Map.Entry<Villager, ArmorStand> entry : illustration1.entrySet()) {
            final Villager v = entry.getKey();
            if (v == null || v.isDead()) {
                entry.getValue().remove();
                if (v == null) continue;
            }
            final Location to = getScaledLocation(v.getLocation());
            final ArmorStand a1 = entry.getValue(), a2 = illustration2.get(v), a3 = illustration3.get(v);

            a1.teleport(to);
            if (a2 != null) a2.teleport(to.clone().add(0, 0.06, 0));
            if (a3 != null) a3.teleport(to.clone().add(0, 0.12, 0));
            if (v.getLocation().getBlockY() < redstoneBlock.getBlockY() - 5) {
                if (v.getLocation().distance(redstoneBlock) < 12) {
                    bankAccount += values.get(v);
                } else {
                    if (currentPlayer == null) {
                        bankAccount += values.get(v);
                    } else {
                        final int chips = values.get(v);
                        if (currentPlayer.isOnline()) {
                            c.ms.showItemBar(currentPlayer, "§aDu hast gewonnen! §d(+" + c.ms.getNumberFormat(chips) + " Chips)");
                            currentPlayer.playSound(currentPlayer.getLocation(), Sound.NOTE_PLING, 1f, 7f);
                            c.addChips(currentPlayer, chips, "CoinPusher");
                        } else {
                            c.addChips(currentPlayer.getUniqueId(), chips, "CoinPusher");
                            currentPlayer = null;
                        }
                        c.getStats().COIN_PUSHER_VERLUST += chips;
                    }
                }
                if (a3 != null) {
                    a3.remove();
                }
                if (a2 != null) {
                    a2.remove();
                }
                a1.remove();
                illustration1.remove(v);
                illustration3.remove(v);
                illustration2.remove(v);
                values.remove(v);
                v.remove();
            }
        }
        if (bankAccount > stake && values.keySet().size() < 25) {
            hologram.setMessage("§c§lAuffüllen...");
        } else if (cooldown > 0) {
            String plus = "";
            if (cooldown % 1.0D == 0) plus = ".0";
            hologram.setMessage("§cWarte §f" + c.round(cooldown, 1) + plus + "s");
            cooldown -= 0.1D;
            if (cooldown < 0) cooldown = 0;
        } else {
            hologram.setMessage("§a§lCOINPUSHER");
        }
    }

    private Location getScaledLocation(final Location baseLocation) {
        double x = (baseLocation.getX() - scaledLocation.getX()) / 6, y = (scaledLocation.getY() - baseLocation.getY()) / 7,
                z = (baseLocation.getZ() - scaledLocation.getZ()) / 6;
        return pusherLocation.clone().add(-0.3, 0.5, -0.4).add(x, -y, z);
    }

    protected List<ArmorStand> getConstruct() {
        return construct;
    }

    protected Map<Villager, ArmorStand> getIllustration1() {
        return illustration1;
    }

    protected Map<Villager, ArmorStand> getIllustration2() {
        return illustration2;
    }

    protected Map<Villager, ArmorStand> getIllustration3() {
        return illustration3;
    }

    private ArmorStand spawnArmorStand(final Location loc, final EulerAngle angle, final Material hand, final short data) {
        return spawnArmorStand(loc, angle, hand == null ? null : new ItemStack(hand, 1, data), true);
    }

    private ArmorStand spawnArmorStand(final Location loc, final EulerAngle angle, final ItemStack hand, boolean addToConstruct) {
        final ArmorStand a = (ArmorStand) loc.getWorld().spawnEntity(loc.clone(), EntityType.ARMOR_STAND);
        a.setVisible(false);
        NMSReflection.setObject(Entity.class, "invulnerable", ((CraftArmorStand) a).getHandle(), true);
        a.setGravity(false);
        a.setSmall(true);
        if (hand != null) a.setItemInHand(hand);
        if (angle != null) a.setRightArmPose(angle);
        a.setArms(true);
        if (addToConstruct) construct.add(a);
        return a;
    }
}