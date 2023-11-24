package me.muxteam.extras;

import com.sk89q.worldguard.bukkit.ConfigurationManager;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.muxteam.basic.MuxLocation;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public final class MuxBoosters implements Listener {
    private final Random r = new Random();
    private final ConcurrentMap<UUID, Set<Booster>> booster;
    private final Map<MuxLocation, Location> cacheloc = new HashMap<>();
    private final Map<Location, BoosterType> cachetype = new HashMap<>();
    private final ConfigurationManager cm = WGBukkit.getPlugin().getGlobalStateManager();
    private MuxSystem ms;

    public MuxBoosters(final MuxSystem ms) {
        this.ms = ms;
        booster = ms.getDB().loadBoosters();
        booster.values().forEach(list -> list.forEach(b -> b.load(this)));
        ms.getServer().getPluginManager().registerEvents(this, ms);
    }

    public void close() {
        booster.values().forEach(list -> list.forEach(b -> b.unload(this)));
        ms.getDB().saveBoosters(booster);
        this.ms = null;
    }

    public void checkBooster() {
        final Server sr = ms.getServer();
        for (final Map.Entry<UUID, Set<Booster>> entry : booster.entrySet()) {
            final UUID uuid = entry.getKey();
            final Player pl = sr.getPlayer(uuid);
            final Set<Booster> activated = entry.getValue();
            final Iterator<Booster> iterator = activated.iterator();
            boolean change = false;
            while (iterator.hasNext()) {
                final Booster b = iterator.next();
                if (ms.getAdmin().BOOSTERS.isActive() && b.isExpired(pl, this)) {
                    b.expire(pl, this);
                    iterator.remove();
                    change = true;
                }
            }
            if (change) {
                booster.put(uuid, activated);
                if (activated.isEmpty()) {
                    booster.remove(uuid);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            ms.getDB().removeBoosterData(uuid);
                        }
                    }.runTaskAsynchronously(ms);
                } else {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            ms.getDB().saveBooster(uuid, booster.get(uuid));
                        }
                    }.runTaskAsynchronously(ms);
                }
            }
        }
    }

    public void removeBoostersWithin(final int minX, final int maxX, final int minZ, final int maxZ, final String world) {
        cacheloc.values().stream().filter(l -> l.getWorld().getName().equals(world) && l.getBlockX() <= maxX && l.getBlockX() >= minX && l.getBlockZ() <= maxZ && l.getBlockZ() >= minZ
                ).forEach(location -> onSpawnerBreak(location.getBlock()));
    }

    public Set<BoosterType> getBooster(final Player p) {
        final Set<Booster> activated = booster.get(p.getUniqueId());
        if (activated == null) return Collections.emptySet();
        final Set<BoosterType> personal = new HashSet<>();
        for (final Booster b : activated) {
            if (b.getType() != BoosterType.SPAWNER && b.getType() != BoosterType.MEGASPAWNER) personal.add(b.getType());
        }
        if (personal.isEmpty()) return Collections.emptySet();
        return personal;
    }

    public boolean hasBooster(final Player p, final BoosterType type) {
        final Set<Booster> activated = booster.get(p.getUniqueId());
        if (activated == null) return false;
        for (final Booster b : activated) {
            if (b.getType() == type) return true;
        }
        return false;
    }

    public BoosterType hasBooster(final Location loc) {
        return cachetype.getOrDefault(loc, null);
    }

    public Collection<Location> getBoostedLocs() {
        return cacheloc.values();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(final PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL || e.getItem() == null || e.getItem().getType() != Material.FIREWORK_CHARGE) return;
        final ItemMeta im = e.getItem().getItemMeta();
        if (im == null || im.hasDisplayName() == false || im.hasLore() == false) return;
        final String type = im.getDisplayName();
        if (type.contains("§") == false) return;
        final Player p = e.getPlayer();
        final int time = Integer.parseInt(ChatColor.stripColor(im.getLore().get(1)));
        final Set<Booster> activated = booster.getOrDefault(p.getUniqueId(), new HashSet<>());
        e.setCancelled(true);
        BoosterType btype = null;
        if (type.contains("Fly")) {
            btype = BoosterType.FLY;
            final Location l = p.getLocation();
            final ApplicableRegionSet regions = WGBukkit.getRegionManager(p.getWorld()).getApplicableRegions(p.getLocation());
            final ProtectedRegion spawnrg = ms.getSpawnRegion();
            if ((regions.size() > 0 && spawnrg != null && spawnrg.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ()) == false) || l.getWorld().getEnvironment() == World.Environment.THE_END) {
                ms.showItemBar(p, "§cDu kannst den Booster hier nicht benutzen.");
                return;
            } else if (hasBooster(p, btype)) {
                ms.showItemBar(p, "§cDu hast bereits einen " + btype.getName() + " aktiviert.");
                return;
            } else if (p.getAllowFlight()) {
                ms.showItemBar(p, "§cDu hast bereits ein Flugmodus aktiv.");
                return;
            } else if (ms.getAdmin().BOOSTERS.isActive() == false) {
                ms.showItemBar(p, "§cBooster sind temporär deaktiviert.");
                return;
            }
            ms.getCreators().removeFlySpawn(p.getUniqueId());
            ms.getCustomRank().removeFly(p.getUniqueId());
            p.setAllowFlight(true);
            p.setFlying(true);
            p.setFlySpeed(0.03F);
            activated.add(new Booster(btype, time));
        } else if (type.contains("Spawner")) {
            if (type.contains("Mega Spawner")) btype = BoosterType.MEGASPAWNER;
            else btype = BoosterType.SPAWNER;
            Block b;
            try {
                b = p.getTargetBlock((Set<Material>) null, 10);
            } catch (final IllegalStateException ex) {
                ms.showItemBar(p, ms.getLang("cmd.spawnererror1"));
                return;
            }
            if (b == null || b.getType() != Material.MOB_SPAWNER) {
                ms.showItemBar(p, ms.getLang("cmd.spawnererror2"));
                return;
            }
            if (hasBooster(b.getLocation()) != null) {
                ms.showItemBar(p, "§cEs läuft bereits ein Booster bei dem Spawner.");
                return;
            }
            final Booster bo = new Booster(btype, b.getLocation(), time);
            bo.load(this);
            activated.add(bo);
        } else if (type.contains("Global")) {
            if (type.contains("XP")) btype = BoosterType.GLOBALXP;
            if (ms.getEvents().isRunning()) {
                ms.showItemBar(p, "§cEs läuft derzeit schon ein Event.");
                return;
            } else if (ms.getEvents().getExpBoost() > (byte) 0) {
                ms.showItemBar(p, "§cEs ist bereits ein globaler XP Booster aktiv.");
                return;
            }
            ms.getEvents().setExpBoost(r.nextInt(10) < 2 ? (byte) 2 : (byte) 1, true);
            ms.broadcastMessage("§5§lBooster>§7 Der Spieler §d" + p.getName() + " §7boostet alle Spieler!", null, MuxSystem.Priority.NORMAL);
            ms.broadcastMessage("§5§lBooster>§7 Für §d" + ms.getTime(time) + " §7gilt überall §a" + ms.getMultiplier(ms.getEvents().getExpBoost()) + " EXP§7.", null, MuxSystem.Priority.NORMAL);
            activated.add(new Booster(btype, time));
        } else if (type.contains("XP")) {
            btype = BoosterType.XP;
            if (hasBooster(p, btype)) {
                ms.showItemBar(p, "§cDu hast bereits einen " + btype.getName() + " aktiviert.");
                return;
            }
            activated.add(new Booster(btype, time));
        }
        ms.removeOneItemFromHand(p);
        if (btype == null) return;
        ms.showItemBar(p, "§fDu hast den §6" + btype.getName() + " §faktiviert.");
        booster.put(p.getUniqueId(), activated);
        new BukkitRunnable() {
            @Override
            public void run() {
                ms.getDB().saveBooster(p.getUniqueId(), booster.get(p.getUniqueId()));
            }
        }.runTaskAsynchronously(ms);
        p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 1F);
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleFlight(final PlayerToggleFlightEvent e) {
        final Player p = e.getPlayer();
        if (ms.hasNormalFlight(p) == false)
            p.playSound(p.getLocation(), e.isFlying() ? Sound.FIREWORK_LAUNCH : Sound.FIREWORK_BLAST, 1F, 1F);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBreak(final BlockBreakEvent e) {
        onSpawnerBreak(e.getBlock());
    }

    public void onSpawnerBreak(final Block b) {
        if (b.getType() == Material.MOB_SPAWNER && hasBooster(b.getLocation()) != null) {
            final MuxLocation loc = new MuxLocation(b.getLocation());
            for (final Map.Entry<UUID, Set<Booster>> entry : booster.entrySet()) {
                final UUID uuid = entry.getKey();
                final Set<Booster> activated = entry.getValue();
                final Iterator<Booster> iterator = activated.iterator();
                boolean change = false;
                while (iterator.hasNext()) {
                    final Booster bo = iterator.next();
                    if ((bo.getType() == BoosterType.SPAWNER || bo.getType() == BoosterType.MEGASPAWNER) && bo.getLocation().equals(loc)) {
                        bo.expire(null, this);
                        iterator.remove();
                        change = true;
                    }
                }
                if (change) booster.put(uuid, activated);
            }
        }
    }

    private Location getLoc(final MuxLocation loc) {
        return cacheloc.computeIfAbsent(loc, MuxLocation::getLocation);
    }

    public class Booster {
        private final BoosterType type;
        private MuxLocation loc;
        private int secremaining;

        public Booster(final BoosterType type, final int time) {
            this.type = type;
            this.secremaining = time;
        }

        public Booster(final BoosterType type, final Location loc, final int time) {
            this.type = type;
            this.loc = new MuxLocation(loc);
            this.secremaining = time;
        }

        public void load(final MuxBoosters mb) {
            if (type == BoosterType.GLOBALXP) {
                mb.ms.getEvents().setExpBoost((byte) 1, true);
            }
            if (this.loc == null) return;
            final Location l = mb.getLoc(this.loc);
            if (mb.cachetype.containsKey(l) == false) {
                mb.cachetype.put(l, type);
            }
            final Block b = l.getBlock();
            Location free = null;
            for (final BlockFace bf : Arrays.asList(BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.DOWN, BlockFace.UP)) {
                final Block relative = b.getRelative(bf);
                if (relative.getType() == Material.AIR) {
                    free = relative.getLocation();
                    break;
                }
            }
            if (free == null) free = b.getRelative(BlockFace.WEST).getLocation();
            mb.ms.getHolograms().addHologram("b" + loc.hashCode(), free.add(0.5, -2, 0.5), "§a" + mb.ms.getTime(secremaining));
        }

        public void unload(final MuxBoosters mb) {
            if (this.loc == null) return;
            mb.ms.getHolograms().removeHologram("b" + loc.hashCode());
        }

        public boolean isExpired(final Player p, final MuxBoosters mb) {
            if (type == BoosterType.SPAWNER || type == BoosterType.MEGASPAWNER) {
                if (mb.cm.activityHaltToggle) return secremaining <= 0;
                mb.ms.getHolograms().setHologramMessage("b" + loc.hashCode(), "§a" + mb.ms.getTime(secremaining));
                secremaining--;
            } else if (type == BoosterType.GLOBALXP) {
                secremaining--;
            }
            if (p == null) return secremaining <= 0;
            if (type == BoosterType.FLY) {
                final Location l = p.getLocation();
                final ProtectedRegion spawnrg = mb.ms.getSpawnRegion();
                final boolean canfly = ((spawnrg != null && p.getWorld().equals(mb.ms.getGameSpawn().getWorld()) && spawnrg.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ()))
                        || (WGBukkit.getRegionManager(l.getWorld()).getApplicableRegions(l).size() == 0 && mb.ms.inEvent(p) == false)) && l.getY() >= 0 && l.getY() <= 256 &&
                        l.getWorld().getEnvironment() != World.Environment.THE_END;
                if (p.getAllowFlight() && canfly == false) {
                    p.setAllowFlight(false);
                    p.setFlying(false);
                } else if (p.getAllowFlight() == false && canfly) {
                    p.setAllowFlight(true);
                    p.setFlying(true);
                    p.setFlySpeed(0.03F);
                }
                if (p.isFlying()) {
                    p.getWorld().spigot().playEffect(l.subtract(0D, 0.5D, 0D), Effect.CLOUD, 0, 0, (float) 0.1, (float) 0.1, (float) 0.1, (float) 0.01, 3, 50);
                }
            }
            if (type == BoosterType.FLY || type == BoosterType.XP) {
                secremaining--;
                if (secremaining == 60 || secremaining == 600) {
                    mb.ms.sendNormalTitle(p, "§fDein " + type.getName() + " läuft in §6" + mb.ms.getTime(secremaining) + " §fab.", 0, 60, 10);
                }
            }
            return secremaining <= 0;
        }

        public void expire(final Player p, final MuxBoosters mb) {
            if (p != null && (type == BoosterType.FLY || type == BoosterType.XP)) {
                mb.ms.sendNormalTitle(p, "§fDein " + type.getName() + " ist §cabgelaufen§f.", 0, 60, 10);
                if (type == BoosterType.FLY) {
                    p.setAllowFlight(false);
                    p.setFlying(false);
                    p.setFlySpeed(0.1F);
                }
            }
            if (type == BoosterType.GLOBALXP) {
                mb.ms.getEvents().setExpBoost((byte) 0, false);
            }
            if (loc != null) {
                final Location l = mb.getLoc(loc);
                mb.ms.getHolograms().removeHologram("b" + loc.hashCode());
                mb.cachetype.remove(l);
                mb.cacheloc.remove(loc);
            }
        }

        public BoosterType getType() {
            return type;
        }

        public MuxLocation getLocation() {
            return loc;
        }
    }

    public enum BoosterType {
        FLY("Fly Booster"), XP("EXP Booster"), SPAWNER("Spawner Booster"), MEGASPAWNER("Mega Spawner Booster"),
        GLOBALXP("Globaler XP Booster");

        private final String name;

        BoosterType(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}