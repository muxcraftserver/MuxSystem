package me.muxteam.basic;

import me.muxteam.basic.MuxActions.HologramAction;
import me.muxteam.muxsystem.MuxInventory;
import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.DataWatcher;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityArmorStand;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityDestroy;
import net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_8_R3.PacketPlayOutSpawnEntityLiving;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.apache.commons.lang3.RandomStringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public final class MuxHolograms implements Listener {
    private MuxSystem ms;
    private final Set<Chunk> chunks = new HashSet<>();
    private final List<IndividualHologram> indholos = new ArrayList<>();
    private final Map<String, Hologram> holos = new HashMap<>(), pluginholos = new HashMap<>();
    private final File holofile;
    private final FileConfiguration holoYML;

    public MuxHolograms(final MuxSystem ms) {
        this.ms = ms;
        holofile = new File(ms.getDataFolder(), "holograms.yml");
        holoYML = YamlConfiguration.loadConfiguration(holofile);
        if (checkHologramsConfig()) {
            spawnHolograms(false, true);
        }
        new BukkitRunnable() {
            short c = 0;
            @Override
            public void run() {
                for (final IndividualHologram h : indholos) {
                    h.refreshHologram();
                }
                if (c == 300) {
                    indholos.forEach(holo -> holo.getHiddenFor().removeIf(player -> player.isOnline() == false));
                    c = 0;
                    return;
                }
                c++;
            }
        }.runTaskTimer(ms, 10L, 20L);
        ms.getServer().getPluginManager().registerEvents(this, ms);
    }

    public void close() {
        removeHolograms(true);
        this.ms = null;
    }

    public void reload() {
        removeHolograms(false);
        spawnHolograms(true, false);
    }

    public void removeOld() {
        final Set<ArmorStand> stands = new HashSet<>();
        for (final Hologram h : holos.values()) {
            if (h != null && h.getStand() != null) stands.add(h.getStand());
        }
        for (final Hologram h : pluginholos.values()) {
            if (h != null && h.getStand() != null) stands.add(h.getStand());
        }
        for (final World x : ms.getServer().getWorlds()) {
            x.getEntitiesByClass(ArmorStand.class).forEach(a -> {
                boolean remove = true;
                for (final ArmorStand stand : stands) {
                    if (stand.hashCode() == a.hashCode()) {
                        remove = false;
                        break;
                    }
                }
                if (remove) {
                    if (a.isVisible() || a.isCustomNameVisible() == false || a.getPassenger() != null || a.getHelmet().getType() != Material.AIR || a.hasGravity() ||
                            a.getUniqueId().equals(ms.getIdentificationId()) == false) remove = false;
                    if (remove) a.remove();
                }
            });
        }
        stands.clear();
    }

    public void saveChunk(final Location l) {
        if (chunks.contains(l.getChunk()) == false)
            chunks.add(l.getChunk());
    }

    public List<IndividualHologram> getIndividualHolograms() {
        return indholos;
    }

    public void addIndividualHologram(final String name, final Location loc, final HologramAction action) {
        indholos.add(new IndividualHologram(name, loc, action));
    }

    public void removeIndividualHologram(final String name) {
        removeIndividualHologram(indholos.stream().filter(individualHologram -> individualHologram.name.equals(name)).findFirst().orElse(null));
    }

    public void removeIndividualHologram(final IndividualHologram hologram) {
        if (hologram != null) {
            indholos.remove(hologram);
            hologram.getPlayersTracking().forEach(hologram::remove);
            hologram.hiddenFor.clear();
        }
    }

    public IndividualHologram addIndividualHologram(final String name, final Location loc, final HologramAction action, boolean refresh) {
        IndividualHologram hologram;
        indholos.add(hologram = new IndividualHologram(name, loc, action, refresh));
        return hologram;
    }

    public Hologram addHologram(final String name, final Location loc, final String msg) {
        final Hologram h = new Hologram(loc, msg).spawn();
        pluginholos.put(name, h);
        return h;
    }

    public void removeHologram(final String name) {
        final Iterator<Entry<String, Hologram>> it = pluginholos.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<String, Hologram> entry = it.next();
            if (entry.getKey().equals(name)) {
                entry.getValue().remove();
                it.remove();
            }
        }
    }

    public void setHologramMessage(final String name, final String msg) {
        final Hologram h = pluginholos.get(name);
        if (h != null) h.setMessage(msg);
    }

    public void removeHologramsWithName(final String name) {
        pluginholos.entrySet().removeIf(h -> {
            if (h.getKey().contains(name)) {
                h.getValue().remove();
                return true;
            }
            return false;
        });
    }

    public void removeHologramsWithDisplayName(final String displayname) {
        pluginholos.entrySet().removeIf(h -> {
            if (h.getValue().getMessage().equals(displayname)) {
                h.getValue().remove();
                return true;
            }
            return false;
        });
    }

    public boolean handleCommand(final Player p) {
        if (ms.isTrusted(p.getName()) == false) {
            ms.sendNoCMDMessage(p);
            return true;
        }
        openInventory(p, 0);
        return true;
    }

    private void openInventory(final Player p, int page) {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxHologramme§0 | /h");
        final Pageifier<ItemStack> pages = new Pageifier<>(27);
        for (final Entry<String, Hologram> eset : holos.entrySet()) {
            final String msg = eset.getValue().getMessage();
            final ItemStack banner = new ItemStack(Material.BANNER);
            final BannerMeta meta = (BannerMeta) banner.getItemMeta();
            meta.setBaseColor(msg.length() > 1 ? ms.getColorFromChatColor(msg.substring(1, 2)) : DyeColor.WHITE);
            banner.setItemMeta(meta);
            pages.addItem(ms.renameItemStack(banner, msg, "§0" + eset.getKey(), "§fKlicke§7, für Infos zum Hologramm."));
        }
        pages.addItem(ms.setLore(ms.getHeadFromURL("https://textures.minecraft.net/texture/3edd20be93520949e6ce789dc4f43efaeb28c717ee6bfcbbe02780142f716", "§f§lHologramm hinzufügen"),
                "", "§fKlicke§7, um ein neues zu erstellen."));
        page = ms.addPageButtons(inv, page, pages);
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.STAINED_GLASS), "§f§lMuxHologramme §0Page:" + page,
                "§7Hier sind alle Hologramme aufgelistet mit", "§7der Möglichkeit, neue zu erstellen."));
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        if (it != MuxInventory.InvType.HOLOGRAMS && it != MuxInventory.InvType.CONFIRM && it != MuxInventory.InvType.ADMIN) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.HOLOGRAMS);
    }

    private void openHoloInventory(final Player p, final String holo) {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxHologramme§0 | " + holo);
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        final Hologram h = holos.get(holo);
        final String msg = h.getMessage();
        final ItemStack banner = new ItemStack(Material.BANNER);
        final BannerMeta meta = (BannerMeta) banner.getItemMeta();
        meta.setBaseColor(msg.length() > 1 ? ms.getColorFromChatColor(msg.substring(1, 2)) : DyeColor.WHITE);
        banner.setItemMeta(meta);
        inv.setItem(4, ms.renameItemStack(banner, msg));
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        inv.setItem(19, ms.renameItemStack(new ItemStack(Material.ENDER_PEARL), "§f§lTeleportieren", "", "§7Klicke, um dich zu §fteleportieren§7."));
        inv.setItem(22, ms.renameItemStack(new ItemStack(Material.BOOK_AND_QUILL), "§f§lUmbenennen", "", "§7Klicke, um diesen §fumzubenennen§7."));
        inv.setItem(25, ms.renameItemStack(new ItemStack(Material.TNT), "§c§lEntfernen", "", "§7Klicke, um diesen zu §centfernen§7."));
        if (it != MuxInventory.InvType.HOLOGRAMS && it != MuxInventory.InvType.CONFIRM) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.HOLOGRAMS);
    }

    public void handleInventory(final Player p, final ItemStack i, final int slot, final Inventory inv) {
        final Material m = i.getType();
        final int page = inv.getItem(4).getType() != Material.STAINED_GLASS ? -1 : Integer.parseInt(inv.getItem(4).getItemMeta().getDisplayName().split(":")[1]);
        if (m == Material.SKULL_ITEM && (slot == 7 || slot == 8)) {
            if (slot == 7) {
                p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                openInventory(p, page - 1);
            } else {
                p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                openInventory(p, page + 1);
            }
            return;
        } else if (m == Material.SKULL_ITEM) {
            new MuxChatInput(ms, (str, pl) -> {
                if (ms.notSafeYAML(str)) {
                    ms.showItemBar(p, "§cZeichen wie . oder = oder > sind nicht erlaubt.");
                    return;
                }
                String name = RandomStringUtils.randomAlphabetic(10).toUpperCase();
                while (holos.containsKey(name)) {
                    name = RandomStringUtils.randomAlphabetic(10).toUpperCase();
                }
                final Location l = p.getLocation();
                final String msg = ChatColor.translateAlternateColorCodes('&', str);
                holoYML.set("holograms." + name + ".x", l.getX());
                holoYML.set("holograms." + name + ".y", l.getY());
                holoYML.set("holograms." + name + ".z", l.getZ());
                holoYML.set("holograms." + name + ".world", l.getWorld().getName());
                holoYML.set("holograms." + name + ".message", msg);
                saveHolograms();
                holos.put(name, new Hologram(l, msg).spawn());
                ms.showItemBar(p, "§fDas Hologramm erfolgreich §aerstellt§f.");
            }).show(p, "§f§lGebe den Text vom Hologramm ein:");
            return;
        } else if (m == Material.ITEM_FRAME) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            if (page == -1) openInventory(p, 0);
            else p.performCommand("a");
            return;
        } else if (m == Material.BANNER && i.getItemMeta() != null && slot > 8) {
            final String name = ChatColor.stripColor(i.getItemMeta().getLore().get(0));
            final Hologram h = holos.get(name);
            if (h == null) {
                p.closeInventory();
                ms.showItemBar(p, "§cDieses Hologramm existiert nicht.");
            } else {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                openHoloInventory(p, name);
            }
            return;
        }
        final ItemStack item = inv.getItem(4);
        final String name = inv.getTitle().split(" \\| ")[1];
        final Hologram h = holos.get(name);
        if (m == Material.TNT) {
            final MuxActions.PlayerAction confirm = player -> {
                h.remove();
                holos.remove(name);
                holoYML.set("holograms." + name, null);
                saveHolograms();
                ms.showItemBar(p, "§fDas Hologramm wurde erfolgreich §centfernt§f.");
                p.performCommand("holo");
            }, cancel = player -> openHoloInventory(p, name);
            new ConfirmInventory(ms, confirm, cancel).show(p, "§0§lHologramm löschen", "§aBestätigen", "§cAbbrechen", item.clone(), item.getItemMeta().getDisplayName());
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        } else if (m == Material.ENDER_PEARL) {
            ms.setLastLocation(p);
            ms.forcePlayer(p, h.getLocation().subtract(0, 0.25D, 0));
            ms.showItemBar(p, "§fDu wurdest zum §6Hologramm §fteleportiert.");
            p.playSound(p.getLocation(), Sound.ENDERMAN_TELEPORT, 0.2F, 1F);
        } else if (m == Material.BOOK_AND_QUILL) {
            new MuxChatInput(ms, (str, pl) -> {
                if (ms.notSafeYAML(str)) {
                    ms.showItemBar(p, "§cZeichen wie . oder = oder > sind nicht erlaubt.");
                    return;
                }
                final String msg = ChatColor.translateAlternateColorCodes('&', str);
                holoYML.set("holograms." + name + ".message", msg);
                saveHolograms();
                holos.get(name).setMessage(msg);
                ms.showItemBar(p, "§fDas Hologramm erfolgreich §abearbeitet§f.");
            }).show(p, "§f§lGebe den neuen Text vom Hologramm ein:");
        } else {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onChunkUnload(final ChunkUnloadEvent e) {
        final World w = e.getChunk().getWorld();
        if (w == ms.getWarpsWorld() || w == ms.getGameSpawn().getWorld() || w.getName().contains("MuxCasino") || chunks.contains(e.getChunk()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onWorldLoad(final WorldLoadEvent e) {
        for (final Hologram h : holos.values()) {
            final MuxLocation mloc = h.getLoadLater();
            if (mloc != null && mloc.getWorld().equals(e.getWorld().getName())) {
                h.loc = new Location(e.getWorld(), mloc.getX(), mloc.getY(), mloc.getZ());
                h.spawn();
                h.loadlater = null;
            }
        }
    }

    public void spawnHolograms(final boolean reload, final boolean cleanup) {
        final Server sr = ms.getServer();
        if (reload) {
            holos.values().stream().filter(Hologram::isDead).forEach(Hologram::spawn);
            pluginholos.values().stream().filter(Hologram::isDead).forEach(Hologram::spawn);
            return;
        }
        if (cleanup) {
            for (final World x : sr.getWorlds()) {
                x.getEntitiesByClass(ArmorStand.class).forEach(ArmorStand::remove);
            }
        }
        for (final String name : holoYML.getConfigurationSection("holograms").getKeys(false)) {
            final World w = sr.getWorld(holoYML.getString("holograms." + name + ".world"));
            if (w == null) {
                holos.put(name, new Hologram(new MuxLocation(holoYML.getString("holograms." + name + ".world"),
                        holoYML.getDouble("holograms." + name + ".x"), holoYML.getDouble("holograms." + name + ".y"), holoYML.getDouble("holograms." + name + ".z")),
                        holoYML.getString("holograms." + name + ".message")).spawn());
            } else {
                holos.put(name, new Hologram(new Location(w, holoYML.getDouble("holograms." + name + ".x"), holoYML.getDouble("holograms." + name + ".y"), holoYML.getDouble("holograms." + name + ".z")),
                        holoYML.getString("holograms." + name + ".message")).spawn());
            }
        }
    }

    public void removeHolograms(final boolean clear) {
        holos.values().forEach(Hologram::remove);
        if (clear) holos.clear();
    }

    private void saveHolograms() {
        try {
            holoYML.save(holofile);
        } catch (final Exception e) {
            System.out.println("MuxSystem> Error while saving the holograms!");
            e.printStackTrace();
        }
    }

    private boolean checkHologramsConfig() {
        if (holofile.exists() == false) {
            try {
                holoYML.createSection("holograms");
                holoYML.save(holofile);
            } catch (final Exception e) {
                System.err.println("MuxSystem> holograms.yml could not be created!");
                return false;
            }
        }
        return true;
    }

    public Map<String, Hologram> getPluginHolos() {
        return pluginholos;
    }

    public final class Hologram {
        private Location loc;
        private String message;
        private EntityArmorStand stand;

        private MuxLocation loadlater;

        public Hologram(final Location loc, final String message) {
            this.message = message;
            this.loc = loc;
        }

        public Hologram(final MuxLocation l, final String message) {
            this.message = message;
            this.loadlater = l;
        }

        public Hologram spawn() {
            if (loc == null) return this;
            final WorldServer world = ((CraftWorld) loc.getWorld()).getHandle();

            loc.getChunk().load(false);
            if (world.worldData.getName().equals("warps") == false) chunks.add(loc.getChunk());
            stand = new EntityArmorStand(world);
            NMSReflection.setObject(Entity.class, "uniqueID", stand, ms.getIdentificationId());
            NMSReflection.setObject(Entity.class, "invulnerable", stand, true);
            stand.setCustomName(message);
            stand.setCustomNameVisible(true);
            stand.setInvisible(true);
            stand.setGravity(true); // "GravityDisabled = true"
            //stand.n(true); // No Hitbox DOESNT WORK IN 1.8.1 || Add +2D to Y
            stand.setLocation(loc.getX(), loc.getY() + 0.25D, loc.getZ(), 0F, 0F);

            world.addEntity(stand, SpawnReason.CUSTOM);
            return this;
        }

        public boolean isDead() {
            return stand == null || stand.dead;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(final String message) {
            if (this.message.equals(message))
                return;
            this.message = message;
            if (stand == null) return;
            stand.setCustomName(message);
        }

        public void teleport(final Location loc) {
            this.loc = loc;
            stand.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        }

        public Location getLocation() {
            return loc;
        }

        public void remove() {
            if (stand != null) {
                stand.world.removeEntity(stand);
                stand.die();
                stand = null;
            }
        }

        public void hide() {
            if (stand != null) stand.setCustomNameVisible(false);
        }

        public void show() {
            if (stand != null) stand.setCustomNameVisible(true);
        }

        public ArmorStand getStand() {
            if (stand == null) return null;
            return (ArmorStand) stand.getBukkitEntity();
        }

        public MuxLocation getLoadLater() {
            return loadlater;
        }
    }

    public final class IndividualHologram {
        private final EntityArmorStand stand;
        private final HologramAction action;
        private final Set<Player> playerstracking = new HashSet<>(), hiddenFor = new HashSet<>();
        private final Map<Player, String> cache = new HashMap<>();
        private final Location loc;
        private boolean refresh = true;
        private final String name;

        public IndividualHologram(final String name, final Location loc, final HologramAction action, final boolean refresh) {
            this(name, loc, action);
            this.refresh = refresh;
        }

        public IndividualHologram(final String name, final Location loc, final HologramAction action) {
            final WorldServer world = ((CraftWorld) loc.getWorld()).getHandle();
            this.name = name;
            this.loc = loc;
            this.action = action;

            loc.getChunk().load(false);

            stand = new EntityArmorStand(world);
            NMSReflection.setObject(Entity.class, "uniqueID", stand, ms.getIdentificationId());
            NMSReflection.setObject(Entity.class, "invulnerable", stand, true);
            stand.setLocation(loc.getX(), loc.getY() + 0.25D, loc.getZ(), 0.0F, 0.0F);
            stand.setCustomName("");
            stand.setCustomNameVisible(true);
            stand.setInvisible(true);
            stand.setGravity(true);
        }

        public void setName(final Player p, final String name) {
            if (name.isEmpty()) {
                if (hiddenFor.contains(p) == false) {
                    hiddenFor.add(p);
                    remove(p);
                }
                return;
            } else if (hiddenFor.contains(p)) {
                hiddenFor.remove(p);
                send(p);
            }
            if (getPlayersTracking().contains(p) == false) {
                if (name.equals(stand.getCustomName())) return;
                stand.setCustomName(name);
            } else {
                update(p, name);
            }
        }

        public void forceRefresh() {
            for (final Player p : getPlayersTracking()) {
                action.call(this, p);
            }
        }

        public void refreshHologram() {
            if (refresh == false) return;
            cache.entrySet().removeIf(entry -> entry.getKey().isOnline() == false);
            final Set<Player> cansee = getNearbyPlayers();
            final Iterator<Player> iter = getPlayersTracking().iterator();
            while (iter.hasNext()) {
                final Player p = iter.next();
                if (cansee.contains(p) == false) {
                    iter.remove();
                    if (p.getWorld() == loc.getWorld()) {
                        remove(p);
                    }
                }
            }
            for (final Player p : cansee) {
                if (getPlayersTracking().contains(p) == false) {
                    action.call(this, p);
                    if (hiddenFor.contains(p)) continue;
                    getPlayersTracking().add(p);
                    send(p);
                } else {
                    action.call(this, p);
                }
            }
        }

        public Set<Player> getPlayersTracking() {
            return playerstracking;
        }

        public Set<Player> getHiddenFor() {
            return hiddenFor;
        }

        private Set<Player> getNearbyPlayers() {
            final Set<Player> nearby = new HashSet<>();
            for (final Player p : loc.getWorld().getPlayers()) {
                if (isVisible(p)) {
                    nearby.add(p);
                }
            }
            return nearby;
        }

        private boolean isVisible(final Player p) {
            if (loc.getWorld() == p.getWorld()) {
                return loc.distance(p.getLocation()) < 40;
            }
            return false;
        }

        private void update(final Player p, final String name) {
            if (cache.containsKey(p) && cache.get(p).equals(name)) return;
            cache.put(p, name);
            final DataWatcher data = new DataWatcher(null);
            data.a(0, (byte) 32);
            data.a(2, name);
            data.a(3, (byte) 1);
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityMetadata(stand.getId(), data, true));
        }

        public HologramAction getAction() {
            return action;
        }

        public void send(final Player p) {
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutSpawnEntityLiving(stand));
        }

        public void remove(final Player p) {
            ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutEntityDestroy(stand.getId()));
            cache.remove(p);
        }
    }
}