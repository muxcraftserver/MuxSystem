package me.muxteam.holidays;

import me.muxteam.basic.NMSReflection;
import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.EntityVillager;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MuxChristmas implements Listener, MuxHoliday {
    private MuxSystem ms;

    private final CopyOnWriteArrayList<Villager> pinguin = new CopyOnWriteArrayList<>();
    private final Map<Villager, ArrayList<ArmorStand>> elements = new HashMap<>();
    private final Random r = new Random();

    public MuxChristmas(final MuxSystem ms, boolean fake) {
        if (fake) return;
        this.ms = ms;
        new BukkitRunnable() {
            final Location spawn = ms.getGameSpawn();

            @Override
            public void run() {
                short living = 0;
                for (final Villager e : pinguin)
                    if (e.isDead() || e.isValid() == false) {
                        remove(e);
                    } else if (e.getWorld() != spawn.getWorld() || e.getLocation().distance(spawn) >= 45.0D) {
                        remove(e);
                        e.getWorld().playEffect(e.getLocation(), Effect.SNOWBALL_BREAK, 0);
                    } else {
                        onMove(e);
                        living = (short) (living + 1);
                    }
                for (short i = (short) (15 - living); i > 0; i = (short) (i - 1)) {
                    spawnMob(spawn.clone().add((17 + r.nextInt(8)) * (r.nextBoolean() ? -1D : 1D), 0.0D, (17 + r.nextInt(8)) * (r.nextBoolean() ? -1D : 1D)));
                }
            }
        }.runTaskTimer(ms, 20L, 100L);
        new BukkitRunnable() {
            @Override
            public void run() {
                onUpdate();
            }
        }.runTaskTimer(ms, 100L, 2L);
        ms.getServer().getPluginManager().registerEvents(this, ms);
    }

    @Override
    public void close() {
        removeAll();
        this.ms = null;
    }

    @EventHandler
    public void onInteract(final PlayerInteractAtEntityEvent e) {
        if (e.getRightClicked().getType() == EntityType.ARMOR_STAND) {
            final ArmorStand a = (ArmorStand) e.getRightClicked();
            if (a.isVisible() == false && a.getEquipment().getHelmet() != null && (a.getEquipment().getHelmet().getType() == Material.WOOL || a.getEquipment().getHelmet().getType() == Material.SNOW_BLOCK || a.getEquipment().getHelmet().getType() == Material.SKULL_ITEM)) {
                e.getPlayer().playSound(a.getLocation(), Sound.CAT_HISS, 0.1F, 1F);
                a.getWorld().playEffect(a.getLocation().clone().add(0, 1.1D, 0), Effect.HEART, 20);
            }
        }
    }

    private void spawnMob(final Location l) {
        final Villager z = l.getWorld().spawn(l, Villager.class);
        z.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 9999999, 1, false, false));
        z.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 9999999, 1, false, false));
        z.setAge(-24000);
        z.setAgeLock(true);
        z.setMaxHealth(2048.0D);
        z.setHealth(2048.0D);
        z.setCanPickupItems(false);
        z.setRemoveWhenFarAway(false);
        ms.removeSounds(z);
        final net.minecraft.server.v1_8_R3.Entity nms = ((CraftEntity) z).getHandle();
        ((EntityVillager) nms).persistent = true;
        NMSReflection.setObject(net.minecraft.server.v1_8_R3.Entity.class, "invulnerable", nms, true);
        spawn(z);
    }

    private void spawn(final Villager z) {
        pinguin.add(z);
        final ItemStack skull1 = ms.getHead("Penguin");

        final ArrayList<ArmorStand> list = new ArrayList<>();
        for (int i = 0; i <= 5; i++) {
            final ArmorStand a1 = (ArmorStand) z.getWorld().spawnEntity(z.getLocation().add(0.0D, -1.25D, 0.0D), EntityType.ARMOR_STAND);
            if (i == 1) {
                a1.setHelmet(new ItemStack(Material.WOOL, 1, (byte) 15));
            }
            if (i == 2) {
                a1.setHelmet(new ItemStack(Material.SNOW_BLOCK));
            }
            if (i == 3) {
                a1.setItemInHand(new ItemStack(Material.STONE_SLAB2));
            }
            if (i == 4) {
                a1.setItemInHand(new ItemStack(Material.STONE_SLAB2));
            }
            if (i != 1) {
                a1.setSmall(true);
            }
            a1.setGravity(false);
            if (i == 0) {
                a1.setHelmet(skull1);
            }
            a1.setVisible(false);
            a1.setMarker(false);

            list.add(a1);
        }
        elements.put(z, list);
    }

    private void remove(final Villager z) {
        if (pinguin.contains(z)) {
            pinguin.remove(z);
            final ArrayList<ArmorStand> list = elements.get(z);
            for (final ArmorStand a : list) {
                a.remove();
            }
            elements.remove(z);
            z.remove();
        }
    }

    private void onMove(final Villager s) {
        if (pinguin.contains(s) && elements.containsKey(s)) {
            final ArmorStand head = elements.get(s).get(0), bauch = elements.get(s).get(1),
                    bauchmitte = elements.get(s).get(2), foot1 = elements.get(s).get(3), foot2 = elements.get(s).get(4);

            head.teleport(s.getLocation().add(0.0D, -0.01D, 0.0D));
            head.getLocation().setDirection(s.getLocation().getDirection());

            final Vector v = head.getLocation().getDirection().clone();
            final Location loc = s.getLocation().clone();
            bauch.teleport(s.getLocation().clone().add(0.0D, -1.25D, 0.0D));
            bauchmitte.teleport(loc.clone().add(v.getX() * 0.2D, -0.4D, v.getZ() * 0.2D));

            final Location loci = loc.clone().add(v.getX() * 0.2D, -0.35D, v.getZ() * 0.2D);
            loci.setYaw(loc.getYaw() - 90.0F);
            foot1.teleport(loci);
            foot2.teleport(loc.clone().add(v.getX() * 0.2D, -0.35D, v.getZ() * 0.2D));
        }
    }

    private void onUpdate() {
        for (final Villager z : pinguin) {
            if (z == null || z.isDead()) {
                remove(z);
            }
            onMove(z);
        }
    }

    private void removeAll() {
        for (final Villager z : pinguin) {
            remove(z);
        }
    }
}