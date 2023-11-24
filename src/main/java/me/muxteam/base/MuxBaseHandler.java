package me.muxteam.base;

import com.google.common.collect.ImmutableMap;
import me.muxteam.basic.MuxAnvil;
import me.muxteam.extras.MuxMounts;
import me.muxteam.muxsystem.MuxInventory;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.shop.MuxShop;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.util.LongHash;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class handles every {@link PlayerBase} event and command
 */
public class MuxBaseHandler implements Listener {
    private final Random r = new Random();
    private final MuxSystem ms;
    private final MuxBase muxBase;
    private final Queue<PlayerBase> valueCheckQueue = new ConcurrentLinkedQueue<>();
    private final Set<Long> loadedChunks = new HashSet<>();
    private final Map<Player, Long> showbackmsg = new ConcurrentHashMap<>();

    public MuxBaseHandler(final MuxSystem ms, final MuxBase muxBase) {
        this.ms = ms;
        this.muxBase = muxBase;
        ms.getServer().getPluginManager().registerEvents(this, ms);
        new BukkitRunnable() {
            @Override
            public void run() {
                final List<PlayerBase> toCheck = new ArrayList<>();
                while (valueCheckQueue.isEmpty() == false)
                    toCheck.add(valueCheckQueue.poll());
                if (toCheck.isEmpty() == false) {
                    try {
                        ms.getForkJoinPool().submit(() -> {
                            toCheck.parallelStream().forEach(PlayerBase::checkValue);
                            toCheck.clear();
                        }).get();
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }
                for (final Map.Entry<Player, Long> entry : showbackmsg.entrySet()) {
                    final Player pl = entry.getKey();
                    if (pl == null || pl.getGameMode() != GameMode.SPECTATOR || pl.getWorld().equals(ms.getBase().getWorld()) == false || muxBase.getVisitors().containsKey(pl) == false) {
                        showbackmsg.remove(pl);
                    } else if (System.currentTimeMillis() > entry.getValue()) {
                        pl.sendMessage(" ");
                        ms.chatClickHoverRun(pl, "§7Klicke §6hier§7, um wieder zum Spawn zu gelangen.", "§6§oKlicke um zum Spawn zu gelangen", "/spawn");
                        showbackmsg.remove(pl);
                    }
                }
            }
        }.runTaskTimerAsynchronously(ms, 5L, 20L);
    }

    public void unloadChunk(final long chunk) {
        loadedChunks.remove(chunk);
    }

    public void addLoadedChunk(final long chunk) {
        loadedChunks.add(chunk);
    }

    public boolean handleCommand(final Player p, final String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("schematic")) {
            final PlayerBase playerBase = muxBase.getFromPlayer(p);
            final PlayerBase base = muxBase.getCurrentLocations().get(p);
            if (playerBase == null) {
                ms.showItemBar(p, "§cDu hast noch keine Base.");
                return true;
            } else if (base == null || base != playerBase) {
                ms.showItemBar(p, "§cDu musst dich auf deiner Base befinden.");
                return true;
            }
            playerBase.saveAsSchematic(p);
            return true;
        }
        if (muxBase.isEnabled() == false && p.isOp() == false) {
            ms.showItemBar(p, "§cDie Basen sind momentan deaktiviert.");
            return true;
        } else if (ms.inBattle(p.getName(), p.getLocation())) {
            ms.showItemBar(p, ms.hnotinfight);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("helper")) {
            muxBase.openHelper(p);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("blocks")) {
            final PlayerBase base = muxBase.getCurrentLocations().get(p);
            if (base == null) {
                ms.showItemBar(p, "§cDu musst dich in einer Base befinden.");
                return true;
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    final List<PlayerBaseBlock> baseBlocks = new ArrayList<>();
                    final MuxShop shop = ms.getShop();
                    final List<String> halfBlocks = muxBase.getBlockRegistry().HALF_BLOCKS;
                    ImmutableMap.copyOf(base.getBlocks()).forEach((id, amount) -> baseBlocks.add(new PlayerBaseBlock(id, amount, (shop.getCheapestPrice(id, false)), halfBlocks)));
                    baseBlocks.sort(Comparator.comparingDouble(PlayerBaseBlock::getPriceForAmount).reversed());
                    final OfflinePlayer op = ms.getServer().getOfflinePlayer(base.getOwner());
                    final Inventory inventory = ms.getServer().createInventory(null, 54, base.isGriefable() && p.isOp() == false ? "§0§lGriefbare Base" : "§0§lBase von " + op.getName());
                    for (int i = 0; i < inventory.getSize(); i++) {
                        if (baseBlocks.size() <= i) break;
                        final PlayerBaseBlock baseBlock = baseBlocks.get(i);
                        if (baseBlock.getPricePerBlock() <= 0) continue;
                        final String[] idAsSplit = baseBlock.getId().split(":");
                        final ItemStack itemStack = new ItemStack(Integer.parseInt(idAsSplit[0]), baseBlock.getAmount() <= 128 ? (int) baseBlock.getAmount() : 128);
                        if (Integer.parseInt(idAsSplit[1]) != 0)
                            itemStack.setDurability((short) Integer.parseInt(idAsSplit[1]));
                        inventory.setItem(i, ms.addLore(itemStack, "§7Anzahl: §f" + ms.getSFormat(baseBlock.getAmount()) + (baseBlock.getAmount() == 1 ? " Block" : " Blöcke"), "§7Marktpreis: §e" + ms.getSFormat(baseBlock.getPricePerBlock()) + (baseBlock.getPricePerBlock() == 1 ? " MuxCoin" : " MuxCoins"), "", "§7Gesamtwert: §b" + ms.getLFormat((long) baseBlock.getPriceForAmount())));
                    }
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ms.getActiveInv(p.getName()) != MuxInventory.InvType.NOINTERACT) p.closeInventory();
                            p.openInventory(inventory);
                            ms.setActiveInv(p.getName(), MuxInventory.InvType.NOINTERACT);
                        }
                    }.runTask(ms);
                }
            }.runTaskAsynchronously(ms);
            return true;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("accept")) {
            try {
                final UUID uuid = UUID.fromString(args[1]);
                if (muxBase.hasInviteFrom(p, uuid)) {
                    int amount = 0;
                    for (final PlayerBase base : muxBase.getPlayerBases().values()) {
                        if (base.isTrusted(p) && base.getOwner().equals(p.getUniqueId()) == false) {
                            amount++;
                            if (amount == 5) break;
                        }
                    }
                    if (amount == 5) {
                        muxBase.clearAllInvites(p);
                        ms.showItemBar(p, "§cDu bist berets in 5 verschiedenen Basen.");
                        return true;
                    }
                    final Player inviter = ms.getServer().getPlayer(uuid);
                    if (inviter != null) {
                        final PlayerBase base = muxBase.getFromPlayer(inviter);
                        if (base == null) return true;
                        base.addTrusted(p.getUniqueId());
                        inviter.sendMessage("§a§lMuxBase>§7 Der Spieler §a" + p.getName() + " §7ist nun in deinem Team.");
                        ms.showItemBar(p, "§fDu bist dem Team erfolgreich §abeigetreten§f.");
                        final MuxUser u = ms.getMuxUser(p.getName());
                        final int places = u.getHomes().size() + amount;
                        if (places < 2) {
                            p.sendMessage("§f§lÜber dem §nMenü§f §lkannst du zur Base gelangen.");
                        } else {
                            p.sendMessage("§f§lÜber dem §nMenü§f > §lOrte kannst du zur Base gelangen.");
                        }
                        inviter.playSound(inviter.getLocation(), Sound.LEVEL_UP, 1F, 2F);
                        p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 2F);
                        muxBase.removeInvite(p, uuid);
                        if (ms.getActiveInv(inviter.getName()) == MuxInventory.InvType.MENU) {
                            if (inviter.getOpenInventory().getTitle().contains("Teambase")) {
                                ms.getMenu().openTeamBaseInventory(inviter);
                            }
                        }
                    }
                    return true;
                }
            } catch (final Exception ignored) { }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("tempaccept")) {
            try {
                final UUID uuid = UUID.fromString(args[1]);
                if (muxBase.hasTempInviteFrom(p, uuid)) {
                    int amount = 0;
                    for (final PlayerBase base : muxBase.getPlayerBases().values()) {
                        if (base.isTrusted(p) && base.getOwner().equals(p.getUniqueId()) == false) {
                            amount++;
                            if (amount == 5) break;
                        }
                    }
                    if (amount == 5) {
                        muxBase.clearAllInvites(p);
                        ms.showItemBar(p, "§cDu bist bereits in 5 verschiedenen Basen.");
                        return true;
                    }
                    final Player inviter = ms.getServer().getPlayer(uuid);
                    if (inviter != null) {
                        final PlayerBase base = muxBase.getFromPlayer(inviter);
                        if (base == null) return true;
                        base.addTempTrusted(p.getUniqueId(), System.currentTimeMillis() + 7200000);
                        inviter.sendMessage("§a§lMuxBase>§7 Der Spieler §a" + p.getName() + " §7ist nun in deinem Team.");
                        inviter.sendMessage("§a§lMuxBase>§7 Nach §e2 Stunden §7wird sein Zugang ablaufen.");
                        ms.showItemBar(p, "§fDu bist dem Team erfolgreich §abeigetreten§f.");
                        final MuxUser u = ms.getMuxUser(p.getName());
                        final int places = u.getHomes().size() + amount;
                        if (places < 2) {
                            ms.showLater(p, "§fÜber dem §nMenü§f kannst du zur Base gelangen.");
                        } else {
                            ms.showLater(p, "§fÜber dem §nMenü§f > Orte kannst du zur Base gelangen.");
                        }
                        inviter.playSound(inviter.getLocation(), Sound.NOTE_PLING, 10, 1.6F);
                        p.playSound(p.getLocation(), Sound.NOTE_PLING, 10, 1.6F);
                        muxBase.removeInvite(p, uuid);
                        if (ms.getActiveInv(inviter.getName()) == MuxInventory.InvType.MENU && (inviter.getOpenInventory().getTitle().contains("Teambase"))) {
                            ms.getMenu().openTeamBaseInventory(inviter);
                        }
                    }
                    return true;
                }
            } catch (final Exception ignored) { }
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("visit")) {
            final OfflinePlayer offlinePlayer = ms.getPlayer(args[1]);
            if (offlinePlayer == null) {
                ms.showItemBar(p, ms.hnotfound);
                return true;
            }
            if (p.getGameMode() == GameMode.SPECTATOR && p.getWorld().getName().equals(muxBase.getWorld().getName()) == false) {
                if (ms.getEvents().getCurrent() != null) {
                    ms.getEvents().getCurrent().getSpectators().remove(p.getUniqueId());
                }
                ms.getPvP().getArenas().forEach(arena -> arena.getSpectators().remove(p.getUniqueId()));
                ms.getPvP().get1vs1Arenas().forEach(arena -> arena.getSpectators().remove(p.getUniqueId()));
                ms.getClanWar().getArenas().forEach(arena -> arena.getSpectators().remove(p.getUniqueId()));
            }
            final PlayerBase base = muxBase.getFromUUID(offlinePlayer.getUniqueId());
            if (base == null) {
                ms.showItemBar(p, "§cDer Spieler §6" + offlinePlayer.getName() + " §chat keine Base.");
                return true;
            }
            // TODO why not set lastloc here?
            p.teleport(base.getSpawn(), PlayerTeleportEvent.TeleportCause.END_PORTAL); // No forcePlayer because we need to pass TeleportCause
            p.setGameMode(GameMode.SPECTATOR);
            if (muxBase.getVisitors().containsKey(p) == false) sendScoreboard(p, base);
            muxBase.getVisitors().put(p, base);
            showbackmsg.put(p, System.currentTimeMillis() + 15000L);
            if (base.getOwner().equals(p.getUniqueId())) {
                ms.chatClickHoverRun(p, "§7Klicke§a hier§7, um deine Base zu betreten statt zu besuchen.", "§a§oKlicke um deine Base zu betreten", "/base");
            } else {
                ms.chatClickHoverRun(p, "§7Klicke§3 hier§7, um die Blöcke der Base einzusehen.", "§3§oKlicke um die Blöcke zu sehen", "/base blocks");
            }
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("tp")) {
            final OfflinePlayer offlinePlayer = ms.getPlayer(args[1]);
            if (offlinePlayer == null) {
                ms.showItemBar(p, ms.hnotfound);
                return true;
            }
            final PlayerBase base = muxBase.getFromUUID(offlinePlayer.getUniqueId());
            if (base == null) {
                ms.showItemBar(p, "§cDer Spieler §6" + offlinePlayer.getName() + " §chat keine Base.");
                return true;
            } else if (ms.hasVoted(p) == false) {
                ms.chatClickHoverLink(p, ms.getLang("vote.tounlock"), ms.getLang("vote.hoverunlock"), "https://muxcraft.eu/vote/?ign=" + p.getName());
                ms.chatClickHoverLink(p, ms.getLang("vote.tounlock2"), ms.getLang("vote.hoverunlock"), "https://muxcraft.eu/vote/?ign=" + p.getName());
                return true;
            } else if (base.getOwner().equals(p.getUniqueId())) {
                handleCommand(p, new String[0]);
                return true;
            } else if (base.isTrusted(p) == false) {
                if (p.isOp() == false) {
                    p.performCommand("base visit " + offlinePlayer.getName());
                    return true;
                } else {
                    ms.getHistory().addHistory(p.getUniqueId(), base.getOwner(), "TELEPORT", "Base", null);
                }
            }
            final Location teleportLocation = muxBase.getSafeSpawn(base);
            if (p.hasPermission("muxsystem.notpcooldown")) {
                ms.setLastLocation(p);
                ms.teleportPlayer(p, teleportLocation);
                ms.showItemBar(p, "§fDu wurdest zur §aBase von " + offlinePlayer.getName() + " §fteleportiert.");
            } else {
                if (ms.startTPCountdown(p, teleportLocation)) {
                    ms.showItemBar(p, "§fTeleportiere zur §aBase von " + offlinePlayer.getName() + "§f...");
                }
            }
            return true;
        }
        if (args.length == 1 && ms.getPlayer(args[0]) != null) {
            handleCommand(p, new String[]{"visit", args[0]});
            return true;
        }
        PlayerBase base = muxBase.getFromPlayer(p);
        if (base == null) {
            p.performCommand("warp base");
            return true;
        } else if (ms.hasVoted(p) == false && ms.getNewbies().isProtectedNewbie(p) == false) {
            ms.chatClickHoverLink(p, ms.getLang("vote.tounlock"), ms.getLang("vote.hoverunlock"), "https://muxcraft.eu/vote/?ign=" + p.getName());
            ms.chatClickHoverLink(p, ms.getLang("vote.tounlock2"), ms.getLang("vote.hoverunlock"), "https://muxcraft.eu/vote/?ign=" + p.getName());
            return true;
        }
        final Location teleportLocation = muxBase.getSafeSpawn(base);
        boolean longdelay = false;
        if (p.hasPermission("muxsystem.notpcooldown")) {
            ms.setLastLocation(p);
            ms.teleportPlayer(p, teleportLocation);
            ms.showItemBar(p, "§fDu wurdest zur §aBase §fteleportiert.");
        } else {
            if (ms.startTPCountdown(p, teleportLocation)) {
                ms.showItemBar(p, "§fTeleportiere zur §aBase§f...");
            }
            longdelay = true;
        }
        if (r.nextInt(101) <= 10) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline() && muxBase.getCurrentLocations().containsKey(p))
                        p.sendMessage("§a§lHinweis:§e §lWenn du 3 Tage hintereinander offline bist, können andere Spieler deine Base griefen.");
                }
            }.runTaskLater(ms, longdelay ? 20L * 4L : 20L * 2L);
        }
        base.extendedNotification(p);
        return true;
    }

    @EventHandler
    public void onChunkUnload(final ChunkUnloadEvent e) {
        if (e.getWorld() != muxBase.getWorld()) return;
        final Chunk chunk = e.getChunk();
        if (loadedChunks.contains(LongHash.toLong(chunk.getX(), chunk.getZ()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent e) {
        final Player p = e.getPlayer();
        final Server sr = ms.getServer();
        final PlayerBase base = muxBase.getFromUUID(p.getUniqueId());
        if (base != null) {
            base.setLastPlayed(System.currentTimeMillis());
            base.getTempTrusted().keySet().forEach(uuid -> {
                final Player pl = sr.getPlayer(uuid);
                pl.sendMessage("§a§lMuxBase>§7 Dein Zugang zur Teambase ist §eabgelaufen§7.");
                pl.sendMessage("§a§lMuxBase>§7 Der Spieler §a" + p.getName() + " §7hat sich ausgeloggt.");
            });
            base.getTempTrusted().clear();
        }
        muxBase.getCurrentLocations().remove(p);
        if (muxBase.getVisitors().containsKey(p)) { // TODO remove here?
            p.setGameMode(GameMode.SURVIVAL);
            ms.forcePlayer(p, ms.getGameSpawn());
        }
        muxBase.clearAllInvites(p);
        muxBase.clearAllSendInvites(p.getUniqueId());
        for (final PlayerBase b : muxBase.getPlayerBases().values()) {
            if (b.getTempTrusted().containsKey(p.getUniqueId())) {
                b.removeTempTrusted(p.getUniqueId());
                final Player baseOwner = sr.getPlayer(b.getOwner());
                if (baseOwner != null) {
                    baseOwner.sendMessage("§a§lMuxBase>§7 Der Spieler §a" + p.getName() + " §7hat sich ausgeloggt.");
                    baseOwner.sendMessage("§a§lMuxBase>§7 Sein Zugang zur Teambase wurde §centfernt§7.");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(final PlayerJoinEvent e) {
        final Player p = e.getPlayer();
        muxBase.baseJoinCheck(p);
    }

    private void sendScoreboard(final Player p, final PlayerBase base) {
        muxBase.sendScoreboard(p, base);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTeleport(final PlayerTeleportEvent e) {
        PlayerBase base = null;
        final Player player = e.getPlayer();
        final Location to = e.getTo(), from = e.getFrom();
        if (to.getWorld() == muxBase.getWorld()) {
            base = getPlayerBaseAtLocation(to);
        } else {
            muxBase.getVisitors().remove(player);
        }
        if (base == null) {
            if (from.getWorld() != muxBase.getWorld()) return;
            muxBase.getCurrentLocations().remove(player);
            ms.resetScoreboard(player);
        } else {
            muxBase.getCurrentLocations().put(player, base);
            ms.getAnalytics().getAnalytics().getBasePlayers().add(player.getUniqueId());
            if (base.getOwner().equals(player.getUniqueId()) && base.isGriefed()) {
                ms.sendTitle(player, "§a§lNeustart", 20, 20, 20);
                ms.sendSubTitle(player, "§7Die Inaktivität bietet dir eine neue Chance.", 20, 20, 20);
                base.setGriefed(false);
            }
            if (to.getWorld().equals(from.getWorld()) && to.distance(from) > 50D)
                sendScoreboard(player, base);
        }
        if (base != null && (base.hasLoadedChunks() == false)) {
            base.loadChunks();
        }
    }

    public PlayerBase getPlayerBaseAtLocation(final Location location) {
        final int x = location.getBlockX();
        final SortedMap<Integer, List<PlayerBase>> xCandidateBases = muxBase.getPlayerBasesByX().subMap(x - 250, x + 250);
        for (final List<PlayerBase> basesAtX : xCandidateBases.values()) {
            for (final PlayerBase base : basesAtX) {
                if (base.contains(location)) {
                    return base;
                }
            }
        }
        return null;
    }

    public PlayerBase getTrustedPlayerBaseAtLocation(final Location location, final UUID player) {
        final int x = location.getBlockX();
        final SortedMap<Integer, List<PlayerBase>> xCandidateBases = muxBase.getPlayerBasesByX().subMap(x - 250, x + 250);
        for (final List<PlayerBase> basesAtX : xCandidateBases.values()) {
            for (final PlayerBase base : basesAtX) {
                if (base.isTrusted(player) && base.contains(location)) {
                    return base;
                }
            }
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockInteract(final PlayerInteractEvent e) {
        final Block clicked = e.getClickedBlock();
        if (clicked == null || clicked.getWorld() != muxBase.getWorld() || clicked.getType() == null) {
            return;
        }
        final Material m = clicked.getType();
        final Player p = e.getPlayer();
        if (m == Material.DRAGON_EGG && (e.getAction() != Action.RIGHT_CLICK_BLOCK || p.isSneaking() == false)) {
            e.setCancelled(true);
        }
        if (e.getAction() != Action.LEFT_CLICK_BLOCK) return;
        PlayerBase base;
        final Material h = e.getMaterial();
        final boolean notpickaxe = h != Material.WOOD_PICKAXE && h != Material.STONE_PICKAXE && h != Material.IRON_PICKAXE && h != Material.GOLD_PICKAXE && h != Material.DIAMOND_PICKAXE;
        if (m == Material.BARRIER || m == Material.ENDER_PORTAL_FRAME || m == Material.DRAGON_EGG || m == Material.BEDROCK) {
            if (notpickaxe) {
                ms.showItemBar(p, "§cNutze eine Spitzhacke, um es zu zerstören.");
                return;
            } else if (p.getItemInHand() != null && (ms.checkGeneralCooldown(p.getName(), "BASEBEDROCKDESTROY", 500L, true) == false)
                    && (base = muxBase.getCurrentLocations().get(p)) != null) {
                if (base.contains(clicked.getLocation(), true) && (base.isTrusted(p) || base.isGriefable())) {
                    clicked.setType(Material.AIR);
                    clicked.getWorld().dropItemNaturally(clicked.getLocation(), new ItemStack(m));
                }
            }
        }

        if (((m == Material.WOOL && clicked.getData() == 14) || m == Material.GOLD_BLOCK) && (base = muxBase.getCurrentLocations().get(p)) != null) {
            if (base.contains(clicked.getLocation(), true) == false) {
                p.performCommand("base blocks");
            }
        }
    }

    @EventHandler
    public void onBlockFromTo(final BlockFromToEvent e) {
        if (e.getBlock().getWorld() != muxBase.getWorld()) {
            return;
        }
        final PlayerBase base = getPlayerBaseAtLocation(e.getBlock().getLocation());
        if (base != null && base.contains(e.getToBlock().getLocation(), true) == false) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPiston(final BlockPistonExtendEvent e) {
        if (e.getBlock().getWorld() != muxBase.getWorld()) {
            return;
        }
        final PlayerBase base = getPlayerBaseAtLocation(e.getBlock().getLocation());
        if (base == null) { // Should never happen
            e.setCancelled(true);
            return;
        }
        for (final Block block : e.getBlocks()) {
            if (base.contains(block.getLocation(), true) == false) {
                e.setCancelled(true);
                break;
            }
            final Location location = block.getLocation().clone().add(e.getDirection().getModX(), e.getDirection().getModY(), e.getDirection().getModZ());
            if (base.contains(location, true) == false) {
                e.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onPiston(final BlockPistonRetractEvent e) {
        if (e.getBlock().getWorld() != muxBase.getWorld()) {
            return;
        }
        final PlayerBase base = getPlayerBaseAtLocation(e.getBlock().getLocation());
        if (base == null) { // Should never happen
            e.setCancelled(true);
            return;
        }
        for (final Block block : e.getBlocks()) {
            if (base.contains(block.getLocation(), true) == false) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onGrow(final BlockGrowEvent e) {
        if (e.getBlock().getWorld() != muxBase.getWorld()) {
            return;
        }
        final PlayerBase base = getPlayerBaseAtLocation(e.getBlock().getLocation());
        if (base == null || base.contains(e.getBlock().getLocation(), true) == false) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityChangeBlock(final EntityChangeBlockEvent e) {
        final Block b = e.getBlock();
        if (b.getWorld() != muxBase.getWorld())
            return;
        final EntityType entityType = e.getEntityType();
        if (entityType == EntityType.SHEEP) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntitySpawn(final EntitySpawnEvent e) {
        if (e.getEntity().getWorld() != muxBase.getWorld())
            return;
        if (e.getEntityType() == EntityType.WITHER)
            e.setCancelled(true);
    }

    @EventHandler
    public void onHangingPlace(final HangingPlaceEvent e) {
        if (e.getEntity().getWorld() != muxBase.getWorld()) {
            return;
        }
        final Player player = e.getPlayer();
        final PlayerBase base = muxBase.getCurrentLocations().get(e.getPlayer());
        final Location blockLock = e.getEntity().getLocation();
        if (base == null || (base.isTrusted(player) == false && base.isGriefable()) || base.contains(blockLock, true) == false) {
            ms.playEffect(player, EnumParticle.CLOUD, e.getBlock().getLocation(), 0, 0, 0, 1, 2);
            e.setCancelled(true);
            ms.showItemBar(player, "§cDu kannst hier nicht bauen.");
            return;
        }
        if (base.getLastValueCheck() < System.currentTimeMillis() - 30000L) {
            addToValueCheckQueue(base);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(final EntityDamageEvent e) {
        if (e.getEntity().getWorld() == muxBase.getWorld() && e.getEntity() instanceof Player) {
            final Player player = (Player) e.getEntity();
            final EntityDamageEvent.DamageCause cause = e.getCause();
            if (cause == EntityDamageEvent.DamageCause.VOID
                    || cause == EntityDamageEvent.DamageCause.LAVA
                    || cause == EntityDamageEvent.DamageCause.FIRE
                    || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                    || cause == EntityDamageEvent.DamageCause.SUFFOCATION) {
                PlayerBase base = muxBase.getCurrentLocations().get(player);
                if (cause == EntityDamageEvent.DamageCause.VOID) {
                    if (player.getGameMode() == GameMode.SPECTATOR) {
                        e.setCancelled(true);
                        base = muxBase.getVisitors().get(player);
                        if (base == null) {
                            ms.forcePlayer(player, ms.getGameSpawn());
                            return;
                        }
                        player.teleport(base.getSpawn(), PlayerTeleportEvent.TeleportCause.END_PORTAL);
                        return;
                    }
                    if (base != null) {
                        ms.forcePlayer(player, base.getSpawn());
                    } else {
                        ms.forcePlayer(player, ms.getGameSpawn());
                    }
                } else {
                    if (base != null && base.isGriefable())
                        e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onDebuffSplash(final PotionSplashEvent e) {
        if (e.getEntity().getWorld() != muxBase.getWorld()) {
            return;
        }
        final ThrownPotion potion = e.getPotion();
        final boolean weakness = potion.getEffects().parallelStream().anyMatch(potionEffect -> potionEffect.getType().equals(PotionEffectType.WEAKNESS)),
                poison = potion.getEffects().parallelStream().anyMatch(potionEffect -> potionEffect.getType().equals(PotionEffectType.POISON)),
                damage = potion.getEffects().parallelStream().anyMatch(potionEffect -> potionEffect.getType().equals(PotionEffectType.HARM));
        if ((weakness || poison || damage) && e.getEntity() instanceof LivingEntity) {
            e.setIntensity((LivingEntity) e.getEntity(), 0F);
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent e) {
        if (e.getEntity().getWorld() != muxBase.getWorld() || e.getEntity() instanceof ItemFrame == false) {
            return;
        }
        if (e.getDamager() instanceof FallingBlock) {
            e.setCancelled(true);
            return;
        }
        Player player;
        if (e.getDamager() instanceof Player) {
            player = (Player) e.getDamager();
        } else if (e.getDamager() instanceof Projectile && ((Projectile) e.getDamager()).getShooter() instanceof Player) {
            player = (Player) ((Projectile) e.getDamager()).getShooter();
        } else return;
        final PlayerBase base = muxBase.getCurrentLocations().get(player);
        final Location location = e.getEntity().getLocation();
        if (base == null || (base.isTrusted(player) == false && base.isGriefable() == false) || base.contains(location, true) == false) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemFrameInteract(final PlayerInteractEntityEvent e) {
        if (e.getRightClicked().getWorld() != muxBase.getWorld() || e.getRightClicked() instanceof ItemFrame == false) {
            return;
        }
        final Player player = e.getPlayer();
        final PlayerBase base = muxBase.getCurrentLocations().get(player);
        final Location blockLock = e.getRightClicked().getLocation();
        if (base == null || (base.isTrusted(player) == false && base.isGriefable() == false) || base.contains(blockLock, true) == false) {
            e.setCancelled(true);
            ms.playEffect(player, EnumParticle.CLOUD, blockLock, 0, 0, 0, 1, 2);
            ms.showItemBar(player, "§cDu kannst hier nicht bauen.");
            return;
        }
        if (base.getLastValueCheck() < System.currentTimeMillis() - 30000L) {
            addToValueCheckQueue(base);
        }
    }

    @EventHandler
    public void onHangingBreak(final HangingBreakByEntityEvent e) {
        if (e.getEntity().getWorld() != muxBase.getWorld() || e.getRemover() instanceof Player == false) {
            return;
        }
        final Player player = (Player) e.getRemover();
        final PlayerBase base = muxBase.getCurrentLocations().get(player);
        final Location blockLock = e.getEntity().getLocation();
        if (base == null || (base.isTrusted(player) == false && base.isGriefable() == false) || base.contains(blockLock, true) == false) {
            ms.playEffect(player, EnumParticle.CLOUD, e.getEntity().getLocation(), 0, 0, 0, 1, 2);
            e.setCancelled(true);
            ms.showItemBar(player, "§cDu kannst hier nicht bauen.");
            return;
        }
        if (base.getLastValueCheck() < System.currentTimeMillis() - 30000L) {
            addToValueCheckQueue(base);
        }
    }

    @EventHandler
    public void onBucketEmpty(final PlayerBucketEmptyEvent e) {
        if (e.getBlockClicked().getWorld() != muxBase.getWorld()) {
            return;
        }
        final Player player = e.getPlayer();
        final PlayerBase base = muxBase.getCurrentLocations().get(e.getPlayer());
        final Location blockLock = e.getBlockClicked().getLocation();
        if (base == null || (base.isTrusted(player) == false && base.isGriefable() == false) || base.contains(blockLock, true) == false) {
            ms.playEffect(player, EnumParticle.CLOUD, e.getBlockClicked().getLocation(), 0, 0, 0, 1, 2);
            e.setCancelled(true);
            if (base != null) sendNoBuildMessage(player, base);
            else ms.showItemBar(player, "§cDu kannst hier nicht bauen.");
            return;
        }
        if (base.getLastValueCheck() < System.currentTimeMillis() - 30000L) {
            addToValueCheckQueue(base);
        }
    }

    @EventHandler
    public void onBucketFill(final PlayerBucketFillEvent e) {
        if (e.getBlockClicked().getWorld() != muxBase.getWorld()) {
            return;
        }
        final Player p = e.getPlayer();
        final PlayerBase base = muxBase.getCurrentLocations().get(e.getPlayer());
        final Location blockLock = e.getBlockClicked().getLocation();
        if (base == null || (base.isTrusted(p) == false && base.isGriefable() == false) || base.contains(blockLock, true) == false) {
            ms.playEffect(p, EnumParticle.CLOUD, e.getBlockClicked().getLocation(), 0, 0, 0, 1, 2);
            ms.showItemBar(p, "§cDu kannst hier nicht bauen.");
            e.setCancelled(true);
            return;
        }
        if (base.getLastValueCheck() < System.currentTimeMillis() - 30000L) {
            addToValueCheckQueue(base);
        }
    }

    @EventHandler
    public void onBlockBreak(final BlockBreakEvent e) {
        if (e.getBlock().getWorld() != muxBase.getWorld()) {
            return;
        }
        final Player player = e.getPlayer();
        final PlayerBase base = muxBase.getCurrentLocations().get(e.getPlayer());
        final Location blockLock = e.getBlock().getLocation();
        if (base == null || (base.isTrusted(player) == false && base.isGriefable() == false) || base.contains(blockLock, true) == false) {
            ms.playEffect(player, EnumParticle.CLOUD, e.getBlock().getLocation(), 0, 0, 0, 1, 2);
            if (base != null) sendNoBuildMessage(player, base);
            else ms.showItemBar(player, "§cDu kannst hier nicht bauen.");
            e.setCancelled(true);
            return;
        }
        if (e.getBlock().getType() == Material.MONSTER_EGGS && e.getBlock().getData() == 0) {
            e.setCancelled(true);
            e.getBlock().setType(Material.AIR);
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(Material.MONSTER_EGGS, 1, (byte) 6));
        }
        if (base.getLastValueCheck() < System.currentTimeMillis() - 30000L) {
            addToValueCheckQueue(base);
        }
    }

    @EventHandler
    public void onBlockPlace(final BlockPlaceEvent e) {
        if (e.getBlock().getWorld() != muxBase.getWorld()) {
            return;
        }
        final Player player = e.getPlayer();
        final PlayerBase base = muxBase.getCurrentLocations().get(e.getPlayer());
        final Location blockLock = e.getBlockPlaced().getLocation();
        if (base == null || (base.isTrusted(player) == false && base.isGriefable() == false) || base.contains(blockLock, true) == false) {
            ms.playEffect(player, EnumParticle.CLOUD, e.getBlock().getLocation(), 0, 0, 0, 1, 2);
            if (base != null) sendNoBuildMessage(player, base);
            else ms.showItemBar(player, "§cDu kannst hier nicht bauen.");
            e.setCancelled(true);
            return;
        }
        if (e.getBlockPlaced().getType() == Material.SAPLING || e.getBlockPlaced().getType() == Material.RED_MUSHROOM || e.getBlock().getType() == Material.BED_BLOCK) {
            if (base.canPlaceGrowableHere(e.getBlockPlaced().getLocation()) == false) {
                e.setCancelled(true);
                ms.playEffect(player, EnumParticle.CLOUD, e.getBlock().getLocation(), 0, 0, 0, 1, 2);
                ms.showItemBar(player, "§cBitte platziere dieses Item weiter in der Mitte.");
            }
        }
        if (base.getLastValueCheck() < System.currentTimeMillis() - 30000L) {
            addToValueCheckQueue(base);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(final PlayerInteractEvent e) {
        final Block clicked = e.getClickedBlock();
        if (clicked == null || e.getClickedBlock().getWorld() != muxBase.getWorld() || clicked.getType() == null)
            return;
        final Player player = e.getPlayer();
        if (muxBase.getBlockRegistry().NOT_INTERACT_ABLE.contains(clicked.getType())) {
            final PlayerBase base = muxBase.getCurrentLocations().get(player);
            final Location blockLoc = e.getClickedBlock().getLocation();
            if (base == null || (base.isTrusted(player) == false && base.isGriefable() == false) || base.contains(blockLoc, true) == false) {
                e.setCancelled(true);
            }
        } else if (e.getItem() != null && e.getItem().getType() != null &&
                (e.getItem().getType().name().contains("MINECART") || e.getItem().getType() == Material.BOAT)) {
            final PlayerBase base = muxBase.getCurrentLocations().get(player);
            final Location blockLoc = e.getClickedBlock().getLocation();
            if (base == null || (base.isTrusted(player) == false && base.isGriefable() == false) || base.contains(blockLoc, true) == false) {
                e.setCancelled(true);
                player.updateInventory();
            }
        }
    }

    @EventHandler
    public void onVehicleEnter(final VehicleEnterEvent e) {
        if (e.getVehicle().getWorld() != muxBase.getWorld()) return;
        Player damager;
        if (e.getEntered() instanceof Player) {
            damager = (Player) e.getEntered();
        } else if (e.getEntered() instanceof Projectile && ((Projectile) e.getEntered()).getShooter() instanceof Player) {
            damager = (Player) ((Projectile) e.getEntered()).getShooter();
        } else {
            e.setCancelled(true);
            return;
        }
        final Entity entity = e.getVehicle();
        final net.minecraft.server.v1_8_R3.Entity em = ((CraftEntity) entity).getHandle();
        if (em instanceof MuxMounts.Rideable == false) return;
        String name;
        if (entity.getType() == EntityType.MINECART) {
            name = entity.getName();
        } else {
            name = ChatColor.stripColor(entity.getCustomName().split(" ")[0].replace("'s", "").replace("'", ""));
        }
        if (name != null && ms.getMounts().getMountType(damager.getUniqueId()) != -1 && name.equals(damager.getName())) return;
        final PlayerBase base = muxBase.getCurrentLocations().get(damager);
        if (base == null || (base.isTrusted(damager) == false && base.isGriefable() == false) || base.contains(entity.getLocation(), true) == false) {
            e.setCancelled(true);
            return;
        }
        if (base.getLastValueCheck() < System.currentTimeMillis() - 30000L) {
            addToValueCheckQueue(base);
        }
    }

    @EventHandler
    public void onVehicleDestroy(final VehicleDestroyEvent e) {
        if (e.getVehicle().getWorld() != muxBase.getWorld()) return;
        Player damager;
        if (e.getAttacker() instanceof Player) {
            damager = (Player) e.getAttacker();
        } else if (e.getAttacker() instanceof Projectile && ((Projectile) e.getAttacker()).getShooter() instanceof Player) {
            damager = (Player) ((Projectile) e.getAttacker()).getShooter();
        } else {
            e.setCancelled(true);
            return;
        }
        final Entity entity = e.getVehicle();
        PlayerBase base = muxBase.getCurrentLocations().get(damager);
        if (base == null) {
            base = getPlayerBaseAtLocation(entity.getLocation());
        }
        if (base == null || (base.isTrusted(damager) == false && base.isGriefable() == false) || base.contains(entity.getLocation(), true) == false) {
            e.setCancelled(true);
            ms.showItemBar(damager, "§cDu kannst hier nicht bauen.");
            return;
        }
        if (base.getLastValueCheck() < System.currentTimeMillis() - 30000L) {
            addToValueCheckQueue(base);
        }
    }

    public void handleInventory(final Player p, final ItemStack is, final int slot, final Inventory inv) {
        if (inv.getTitle().endsWith("/basen") || inv.getTitle().endsWith("/griefen")) {
            if (slot <= 4 && is.getType() != null && ms.hasGlow(is)) return;
            if (slot == 1) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                muxBase.openGeneralInventory(p, MuxBase.BaseInventoryType.GRIEFABLE);
            } else if (slot == 4) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                muxBase.openGeneralInventory(p, MuxBase.BaseInventoryType.TOP);
            } else if (slot == 7) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                new MuxAnvil(ms, ((input, p1) -> {
                    String playerName = ms.retardCleaner(input, "Spielername: ");
                    p.closeInventory();
                    p.performCommand("base visit " + playerName);
                })).show(p, "Spielername: ", new ItemStack(Material.SKULL_ITEM, 1, (byte) 3));
            } else if (is.getType() != null && is.getType() == Material.SKULL_ITEM) {
                if (is.getDurability() == 3) {
                    final ItemMeta im = is.getItemMeta();
                    final String name = im.getDisplayName().split(" ")[1];
                    if (im.hasLore() && im.getLore().get(im.getLore().size() - 1).contains("nicht erstellt")) {
                        p.closeInventory();
                        if (ms.getBase().isEnabled() == false && p.isOp() == false) {
                            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                            ms.showItemBar(p, "§cDie Basen sind momentan deaktiviert.");
                            return;
                        }
                        if (ms.getBase().createPlayerBase(p)) {
                            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                        }
                    } else p.performCommand("base visit " + name);
                } else { // GRIEF
                    final int griefId = Integer.parseInt(ChatColor.stripColor(is.getItemMeta().getDisplayName().split(" ")[2].replace("#", "")));
                    final PlayerBase base = muxBase.getGriefing().getBaseByGriefId(griefId);
                    if (base == null) {
                        muxBase.openGeneralInventory(p, MuxBase.BaseInventoryType.GRIEFABLE);
                        return;
                    } else if (base.getGrief() == -1) {
                        base.setGrief(System.currentTimeMillis() + 1800000L); // 30 Minuten
                        //  base.setGrief(System.currentTimeMillis() + 60000); // 1 Minute ( DEBUG)
                        muxBase.getGriefing().updateGriefInventory();
                    }
                    if (base.getGrief() <= System.currentTimeMillis()) {
                        muxBase.openGeneralInventory(p, MuxBase.BaseInventoryType.GRIEFABLE);
                        return;
                    }
                    p.closeInventory();
                    ms.forcePlayer(p, muxBase.getSafeSpawn(base));
                    ms.getNewbies().handleNewbieGriefing(p);
                }
            }
        }
    }

    protected void addToValueCheckQueue(final PlayerBase base) {
        if (valueCheckQueue.contains(base)) return;
        base.setLastValueCheck(System.currentTimeMillis());
        valueCheckQueue.add(base);
    }

    private void sendNoBuildMessage(final Player p, final PlayerBase base) {
        if (base.getOwner().equals(p.getUniqueId())) {
            if (base.getSize() < 96) {
                if (ms.hasVoted(p)) {
                    ms.showItemBar(p, "§fVote jeden Tag, um deine Base zu erweitern.");
                } else {
                    ms.chatClickHoverLink(p, "§6§lMuxCraft>§7 Vote, §7um deine Base zu erweitern.", "§6§oKlicke um zu voten", "https://muxcraft.eu/vote/?ign=" + p.getName());
                    ms.chatClickHoverLink(p, ms.getLang("vote.tounlock2"), ms.getLang("vote.hoverunlock"), "https://muxcraft.eu/vote/?ign=" + p.getName());
                }
            } else {
                ms.showItemBar(p, "§cDeine Base ist bereits maximal erweitert.");
            }
        } else {
            ms.showItemBar(p, "§cDu kannst hier nicht bauen.");
        }
    }
}