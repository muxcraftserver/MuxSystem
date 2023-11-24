package me.muxteam.events;

import com.google.common.util.concurrent.AtomicDouble;
import me.muxteam.basic.NMSReflection;
import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.shop.MuxMining;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MuxBoss extends Event implements Listener {
    private final Location loc = new Location(ms.getWarpsWorld(), 112.5D, 33D, 225.5D, 96.4F, 37F);
    private final List<Entity> bossList = new ArrayList<>();
    private final List<EntityType> entityTypes = Arrays.asList(EntityType.GIANT, EntityType.WITHER, EntityType.IRON_GOLEM);
    private final Map<Player, Double> damage = new HashMap<>();
    private EntityType bosstype;
    private final MuxScoreboard.ScoreboardType sb;

    public MuxBoss(final MuxEvents e) {
        super(e);
        name = "MuxBoss";
        item = new ItemStack(Material.SKULL_ITEM);
        canSpectate = true;
        pvp = true;
        maxPlayers = 300;
        maxSpectatorDistance = 35D;
        sb = ms.getScoreboard().createScoreboard("§7§lMux§d§lBoss");
        sb.setLook((p, board) -> {
            sb.setLine(board, "");
            sb.setSection(board, "§c§lGegner", "noch " + bossList.size() + (bossList.size() == 1 ? " Boss" : " Bosse"), false);
            if (players.contains(p)) {
                sb.setLine(board, " ");
                sb.setSection(board, "§4§lDein Schaden", ms.getNumberFormat((int) (double) damage.get(p)), false);
            }
        });
        spectatorSB = sb;
        maxDuration = 900000;
    }

    @Override
    public void start() {
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das MuxBoss §7Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        startCountdown(60, false, false);
        spawnEventNPC("§d§lEvent beitreten");
    }

    @Override
    public void updateCountDown(final int sec) {
        if (sec == 0) {
            EntityType type = entityTypes.get(r.nextInt(entityTypes.size()));
            if (players.size() < 30) {
                while (type == EntityType.WITHER) {
                    type = entityTypes.get(r.nextInt(entityTypes.size()));
                }
            }
            bosstype = type;
            Entity boss;
            for (int i = 0; i < (players.size() < 10 ? 2 : players.size() < 100 ? 5 : 10); i++) {
                boss = loc.getWorld().spawnEntity(loc.clone().add(0, 1, 0), type);
                boss.setCustomName("§d§lMuxBoss");
                boss.setCustomNameVisible(true);
                bossList.add(boss);
                final net.minecraft.server.v1_8_R3.Entity eboss = ((CraftEntity) boss).getHandle();
                NMSReflection.setObject(net.minecraft.server.v1_8_R3.Entity.class, "uniqueID", eboss, ms.getIdentificationId());
                final LivingEntity bossAsLivingEntity = (LivingEntity) boss;
                bossAsLivingEntity.setMaxHealth(2048);
                bossAsLivingEntity.setHealth(2048);
                bossAsLivingEntity.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 2, true));
            }
            players.forEach(p -> {
                if (bosstype != null && bosstype == EntityType.WITHER) {
                    final PlayerInventory pi = p.getInventory();
                    final ItemStack bow = ms.renameItemStack(new ItemStack(Material.BOW), "§f§lMux§a§lBogen");
                    bow.addUnsafeEnchantment(Enchantment.DURABILITY, 10000);
                    bow.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
                    final ItemMeta bowMeta = bow.getItemMeta();
                    bowMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    bowMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                    bowMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    bowMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
                    bow.setItemMeta(bowMeta);
                    pi.setItem(1, bow);
                    pi.setItem(9, new ItemStack(Material.ARROW));
                }
                ms.sendScoreboard(p, sb);
            });
        }
    }

    @Override
    public void update() {
        if (bosstype == EntityType.GIANT) {
            bossList.forEach(entity -> {
                final int rInt = r.nextInt(100);
                if (rInt <= 20) {
                    entity.getWorld().createExplosion(entity.getLocation().getX(), entity.getLocation().getY(), entity.getLocation().getZ(), 5F, false, false);
                }
            });
        }
    }

    @Override
    public long getUpdateTime() {
        return 40L;
    }

    @Override
    public Location getSpectateLocation() {
        return loc;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Die ultimativen MuxBosse sind gekommen!",
                "§7Besiege sie und nutze dein Schwert, um",
                "§7den maximalen Schaden anzurichten.",
                "",
                "§7Teilnehmer: §d" + players.size()
        };
    }

    @Override
    public boolean join(final Player p) {
        equipPlayer(p);
        damage.putIfAbsent(p, 0D);
        ms.forcePlayer(p, loc);
        final PlayerInventory pi = p.getInventory();
        pi.setHelmet(new ItemStack(Material.LEATHER_HELMET));
        pi.setBoots(new ItemStack(Material.LEATHER_BOOTS));
        pi.setLeggings(new ItemStack(Material.LEATHER_LEGGINGS));
        pi.setChestplate(new ItemStack(Material.LEATHER_CHESTPLATE));
        final ItemStack sword = ms.renameItemStack(new ItemStack(Material.WOOD_SWORD), "§f§lMux§a§lSchwert");
        sword.addUnsafeEnchantment(Enchantment.DURABILITY, 10000);
        final ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        swordMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        swordMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        swordMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
        sword.setItemMeta(swordMeta);
        pi.setItem(0, sword);
        if (bosstype != null) {
            ms.sendScoreboard(p, sb);
            if (bosstype == EntityType.WITHER) {
                final ItemStack bow = ms.renameItemStack(new ItemStack(Material.BOW), "§f§lMux§a§lBogen");
                bow.addUnsafeEnchantment(Enchantment.DURABILITY, 10000);
                bow.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
                final ItemMeta bowMeta = bow.getItemMeta();
                bowMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                bowMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
                bowMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                bowMeta.addItemFlags(ItemFlag.HIDE_DESTROYS);
                bow.setItemMeta(bowMeta);
                pi.setItem(1, bow);
                pi.setItem(9, new ItemStack(Material.ARROW));
            }
        }
        return true;
    }

    @Override
    public void quit(final Player p) {
        resetPlayer(p);
    }

    @Override
    public void stop() {
        damage.clear();
        bossList.forEach(org.bukkit.entity.Entity::remove);
        bossList.clear();
    }

    @EventHandler
    public void onBossDeath(final EntityDeathEvent e) {
        if (bossList.contains(e.getEntity())) {
            e.getDrops().clear();
            e.setDroppedExp(0);
            final Player k = e.getEntity().getKiller();
            if (k != null) {
                ms.sendTitle(k, "§a§lBonus", 10, 60, 10);
                ms.sendSubTitle(k, "§fDu hast den §dMuxBoss §fgetötet!", 10, 60, 10);
                if (startedByPlayer == false) {
                    ms.showItemBar(k, "§fDeine Belohnung: §e" + ms.getNumberFormat(giveReward(k, 10)) + " MuxCoins");
                }
            }
            bossList.remove(e.getEntity());
            if (bossList.isEmpty()) {
                ms.broadcastMessage("§d§lMuxEvent>§7 Alle MuxBosse wurden endgültig §abesiegt§7.", null, MuxSystem.Priority.LOW);
                final AtomicDouble totalDamage = new AtomicDouble(0);
                damage.values().forEach(totalDamage::addAndGet);
                for (final Map.Entry<Player, Double> entry : damage.entrySet()) {
                    final double dmg = entry.getValue();
                    final double percent = dmg / totalDamage.get() * 100;
                    final Player pl = entry.getKey();
                    ms.sendTitle(pl, "§d§lMuxBoss", 10, 80, 10);
                    ms.sendSubTitle(pl, "§fDein Schaden: §d" + String.format("%.0f", dmg) + " §f(" + ms.getDecimalFormat(percent) + "%)", 10, 80, 10);
                    if (startedByPlayer == false) {
                        ms.runLater(new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (pl.isOnline() == false) return;
                                final int coins = Integer.parseInt(String.format("%.0f", dmg));
                                ms.sendSubTitle(pl, "§fDeine Belohnung: §e§l" + ms.getNumberFormat(giveReward(pl, coins, MuxMining.OreTypes.IRON)) + " MuxCoins", 0, 60, 10);
                            }
                        }, 60L);
                    }
                }
                stopEvent(false);
            } else {
                sendEventMessage("§7Ein MuxBoss wurde §abesiegt§7. Noch §d" + (bossList.size() == 1 ? "ein weiterer §7ist" : bossList.size() + " weitere §7sind") + " übrig!");
                players.forEach(player -> ms.sendScoreboard(player, sb));
            }
        }
    }

    @EventHandler
    public void onProjectileHit(final ProjectileHitEvent e) {
        if (e.getEntityType() != EntityType.ARROW) return;
        final Arrow arrow = (Arrow) e.getEntity();
        if (arrow.getShooter() instanceof Player) {
            final Player p = (Player) arrow.getShooter();
            if (players.contains(p)) {
                e.getEntity().remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(final EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION && (e.getEntity() instanceof Player && players.contains((Player) e.getEntity()))) {
            e.setCancelled(false);
            final Player p = (Player) e.getEntity();
            if (p.getHealth() - e.getFinalDamage() <= 0) {
                e.setCancelled(true);
                join(p);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
        if (e.getEntity() instanceof Player && players.contains((Player) e.getEntity())) {
            if ((bossList.contains(e.getDamager()) || e.getDamager().getType() == EntityType.WITHER_SKULL)) {
                e.setCancelled(false);
                final Player p = (Player) e.getEntity();
                if (p.getHealth() - e.getFinalDamage() <= 0) {
                    e.setCancelled(true);
                    join((Player) e.getEntity());
                }
            } else {
                e.setCancelled(true);
            }
        } else if (bossList.contains(e.getEntity())) {
            Player p = null;
            if (e.getDamager() instanceof Player && players.contains((Player) e.getDamager())) {
                p = (Player) e.getDamager();
            } else if (e.getDamager() instanceof Projectile) {
                e.setCancelled(false);
                final Projectile proj = (Projectile) e.getDamager();
                p = proj.getShooter() instanceof Player ? (Player) proj.getShooter() : null;
            }
            if (p != null) {
                e.setCancelled(false);
                damage.put(p, (damage.get(p) + e.getFinalDamage()));
                ms.showItemBar(p, "§a+" + String.format("%.0f", e.getFinalDamage()) + " Schaden §f(" + String.format("%.0f", (((LivingEntity) e.getEntity()).getHealth() - e.getFinalDamage())) + " verleibend)");
            }
        }
    }
}