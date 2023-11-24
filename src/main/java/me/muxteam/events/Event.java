package me.muxteam.events;

import me.muxteam.basic.MuxActions;
import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxInventory;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.shop.MuxMining;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

public abstract class Event {
    // Event Start Options
    boolean leaveInventoryOpen = false, canSpectate = false, canMoveItems = false, canDrop = false, keepInventory = false,
            pvp = false, chatOnly = false, noHunger = true, scoreboard = false, waitingScoreboard = true, noJoinMSG = false,
            breakBlocks = false;
    int maxPlayers = -1, playerHider = -1;
    double maxSpectatorDistance = 70D;
    long maxDuration = -1;
    String name = "Event";
    ItemStack item = new ItemStack(Material.BARRIER);
    protected MuxScoreboard.ScoreboardType spectatorSB;

    // Access & Edit
    boolean canjoin = false;
    short seconds;
    final CopyOnWriteArraySet<Player> players = new CopyOnWriteArraySet<>();

    // Access only
    protected final ConcurrentHashMap<UUID, Location> spectators = new ConcurrentHashMap<>();
    boolean cancelled = false, startedByPlayer;
    int wait = -1, playerCountAtStart = 0;
    long cancelTime = -1;
    final MuxEvents e;
    final MuxSystem ms;
    final Random r = new Random();

    // Internal
    private boolean stopping = false;
    private MuxScoreboard.ScoreboardType standardsb;
    private final MuxActions.PlayerAction joinaction = p -> p.performCommand("event join");
    private final Map<String, Integer> backexp = new HashMap<>(), backfood = new HashMap<>();
    private final Map<String, ItemStack[]> backarmor = new HashMap<>(), backinv = new HashMap<>();
    private final Map<String, List<String>> hiddenplayers = new HashMap<>();
    private final Set<String> hiding = new HashSet<>();
    private final ItemStack hideitem, showitem;
    private int eventcoins = 0;
    private BukkitTask timer, countdown, endtimer;
    private Listener hideListener;


    protected Event(final MuxEvents e) {
        this.e = e;
        this.ms = e.ms;
        hideitem = ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (short) 10), "§fSpieler sind §asichtbar");
        showitem = ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (short) 8), "§fSpieler sind §7unsichtbar");
    }

    public void start() {
    }

    public void stop() {
    }

    public boolean join(final Player p) {
        return false;
    }

    public void quit(final Player p) {
    }

    public String[] getDescription() {
        return new String[]{};
    }

    public String[] getAdminInformation() {
        return new String[]{};
    }

    public String getClickToJoin() {
        return "§7Klicke, um am Event §dteilzunehmen§7.";
    }

    public long getUpdateTime() {
        return -1L;
    }

    public void update() {
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public Location getSpectateLocation() {
        return null;
    }

    public void updateCountDown(final int sek) {
    }

    public void generate() {
    }

    public void action(final Player p, final String[] args) {
    }

    public boolean canDrop() {
        return canDrop;
    }

    public boolean timeOver() {
        return false;
    }

    // Don't override

    public String getName() {
        return name;
    }

    public ItemStack getItem() {
        return item;
    }

    public boolean isPvP() {
        return pvp;
    }

    public boolean canMoveItems() {
        return canMoveItems;
    }

    public double getMaxSpectatorDistance() {
        return maxSpectatorDistance;
    }

    public boolean isStartedByPlayer() {
        return startedByPlayer;
    }

    public boolean isStopping() {
        return stopping;
    }

    public void updateSpectators() {
        spectators.keySet().forEach(uuid -> {
            final Player player = ms.getServer().getPlayer(uuid);
            if (player != null) ms.sendScoreboard(player, spectatorSB);
        });
    }

    public MuxScoreboard.ScoreboardType getSB() {
        return standardsb;
    }

    public ConcurrentMap<UUID, Location> getSpectators() {
        return spectators;
    }

    private void addCoins(final Player p, final int coins, final boolean scoreboard) {
        final MuxUser u = ms.getMuxUser(p.getName());
        u.addCoins(coins);
        ms.getHistory().addHistory(p.getUniqueId(), null, "COINS", String.valueOf(coins), "Event");
        if (scoreboard) ms.sendScoreboard(p);
        this.eventcoins += coins;
        ms.getAnalytics().addEventExpenses(coins);
        ms.getAnalytics().getAnalytics().addEventCoins(coins);
        ms.saveMuxUser(u);
    }

    public void addCoins(final Player p, final int coins) {
        addCoins(p, coins, true);
    }

    protected long giveReward(final Player p, final int diamondAmount) {
        return giveReward(p, diamondAmount, MuxMining.OreTypes.DIAMOND);
    }

    protected long giveReward(final Player p, final int diamondAmount, final MuxMining.OreTypes type) {
        return giveReward(p, diamondAmount, type, true);
    }

    protected long giveReward(final Player p, final int diamondAmount, final MuxMining.OreTypes type, final boolean scoreboard) {
        final long activeMuxCoinsSupply = ms.getActiveMuxCoinsSupply();
        int coins = (int) (activeMuxCoinsSupply * type.getPercentage() / 100) * diamondAmount;
        if (coins < 1) coins = 1;
        addCoins(p, coins, scoreboard);
        return coins;
    }

    public void startEvent() {
        start();
        if (waitingScoreboard || scoreboard) setupScoreboard();
        cancelled = false;
        canjoin = true;
        cancelTime = -1;
        if (getUpdateTime() != -1) {
            timer = new BukkitRunnable() {
                @Override
                public void run() {
                    update();
                    updateSpectators();
                }
            }.runTaskTimer(ms, 60L, getUpdateTime());
        } else {
            timer = new BukkitRunnable() {
                @Override
                public void run() {
                    updateSpectators();
                }
            }.runTaskTimer(ms, 60L, 20L);
        }

        if (maxDuration != -1) {
            cancelTime = System.currentTimeMillis() + maxDuration;
            endtimer = new BukkitRunnable() {
                @Override
                public void run() {
                    if (seconds > 0) { // cancel time immer hoch setzten wenn start countdown an ist
                        cancelTime = System.currentTimeMillis() + maxDuration;
                        return;
                    }
                    if (cancelTime <= System.currentTimeMillis()) {
                        if (timeOver() == false)
                            stopEvent(false, true);
                    } else
                        players.forEach(ms::sendScoreboard);
                }
            }.runTaskTimer(ms, 60L, 2L);
        }

        if (playerHider != -1) {
            ms.getServer().getPluginManager().registerEvents(hideListener = new Listener() {
                @EventHandler
                public void onInteract(final PlayerInteractEvent e) {
                    final Player player = e.getPlayer();
                    if (playerHider == -1 || players.contains(player) == false) return;
                    if (e.getItem() != null && e.getItem().getType() == Material.INK_SACK) {
                        e.setCancelled(true);
                        if (ms.checkGeneralCooldown(player.getName(), "EVENTHIDE", 5000L, false)) {
                            ms.showItemBar(player, "§cWarte etwas...");
                            return;
                        }
                        ms.checkGeneralCooldown(player.getName(), "EVENTHIDE", 5000L, true);
                        if (hiding.contains(player.getName()) == false) {
                            hidePlayers(player);
                            player.setItemInHand(showitem);
                        } else {
                            final List<String> hidden = hiddenplayers.get(player.getName());
                            if (hidden == null) return;
                            final Server sr = ms.getServer();
                            hidden.stream().map(sr::getPlayer).filter(Objects::nonNull).forEach(player::showPlayer);
                            hidden.clear();
                            hiding.remove(player.getName());
                            player.setItemInHand(hideitem);
                        }
                    }
                }
            }, ms);
        }
    }

    public void stopEvent(final boolean rapidteleport) {
        stopEvent(rapidteleport, false);
    }

    public void stopEvent(final boolean rapidteleport, final boolean cancelled) {
        final Event ev = e.getCurrent();
        if (ev == null) return;
        final int coinsToSave = eventcoins;
        if (item.getType() != Material.CAKE) { // IF NOT CUSTOM EVENT
            if (rapidteleport && cancelled) {
                ms.getDB().addEventCoins(name, coinsToSave);
            } else {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ms.getDB().addEventCoins(name, coinsToSave);
                    }
                }.runTaskAsynchronously(ms);
            }
        }
        if (endtimer != null) {
            endtimer.cancel();
            endtimer = null;
        }
        cancelTime = -1;
        eventcoins = 0;
        e.getEffects().disable(); // For multiple Events, check if no other events running & disable
        ms.getNPCS().removeNPC(joinaction, true);
        ms.getGiants().respawn();
        if (hideListener != null) HandlerList.unregisterAll(hideListener);
        final Server sr = ms.getServer();
        for (final UUID uuid : spectators.keySet()) {
            final Player pl = sr.getPlayer(uuid);
            if (pl != null) {
                final MuxUser u = ms.getMuxUser(pl.getName());
                final Location lastloc = u.getLastLocation(),
                        l = lastloc != null && lastloc.getWorld() != null ? lastloc : ms.getGameSpawn();
                ms.forcePlayer(pl, l);
            }
        }
        for (final Map.Entry<String, List<String>> entry : hiddenplayers.entrySet()) {
            final Player player = sr.getPlayer(entry.getKey());
            for (final String s : entry.getValue()) {
                final Player other = sr.getPlayer(s);
                if (other != null) player.showPlayer(other);
            }
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (countdown != null) {
            countdown.cancel();
            countdown = null;
        }
        this.cancelled = cancelled;
        if (rapidteleport) {
            for (final Player pl : players) {
                quit(pl);
            }
            stop();
            if (ev instanceof Listener) {
                HandlerList.unregisterAll((Listener) ev);
            }
            players.clear();
            checkSpectators();
            e.clearCurrent();
        } else {
            stopping = true;
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (players.isEmpty()) {
                        if (e.getCurrent() != null) {
                            stop();
                            if (e.getCurrent() instanceof Listener) {
                                HandlerList.unregisterAll((Listener) e.getCurrent());
                            }
                        }
                        players.clear();
                        checkSpectators();
                        e.clearCurrent();
                        stopping = false;
                        for (final Player pl : ms.getServer().getOnlinePlayers()) {
                            if (pl.isOp() && ms.getActiveInv(pl.getName()) == MuxInventory.InvType.EVENT) {
                                pl.performCommand("event");
                            }
                        }
                        cancel();
                        return;
                    }
                    final Player p = players.iterator().next();
                    if (p != null) {
                        quit(p);
                    }
                    players.remove(p);
                }
            }.runTaskTimer(ms, 0L, 10L);
        }
    }

    public void setupScoreboard() {
        standardsb = ms.getScoreboard().createScoreboard("§r§7§lMux§d§l" + getName().replace("Mux", ""));
        standardsb.setLook((p, board) -> {
            if (wait != -1) {
                standardsb.setLine(board, "");
                standardsb.setSection(board, "§a§lTeilnehmer    ", players.size() + " Spieler", false);
                standardsb.setLine(board, " ");
                standardsb.setSection(board, "§6§lStart", seconds > 10 ? "in Kürze..." : "in " + ms.getTime(seconds), false);
            } else {
                standardsb.setLine(board, "  §r");
                standardsb.setSection(board, "§c§lVerbleibend   ", players.size() + " Spieler", false);
                if (endtimer != null) {
                    standardsb.setLine(board, "    ");
                    standardsb.setSection(board, "§6§lZeit", "§fnoch " + ms.getTime((int) ((cancelTime - System.currentTimeMillis()) / 1000)), false);
                }
            }
        });
        if (spectatorSB == null) {
            spectatorSB = ms.getScoreboard().createScoreboard("§7§lMux§d§l" + getName().replace("Mux", ""));
            spectatorSB.setLook((p, board) -> {
                if (wait != -1) {
                    spectatorSB.setLine(board, "");
                    spectatorSB.setSection(board, "§a§lTeilnehmer    ", players.size() + " Spieler", false);
                    spectatorSB.setLine(board, " ");
                    spectatorSB.setSection(board, "§6§lStart", seconds > 10 ? "in Kürze..." : "in " + ms.getTime(seconds), false);
                } else {
                    spectatorSB.setLine(board, "  ");
                    spectatorSB.setSection(board, "§c§lVerbleibend   ", players.size() + " Spieler", false);
                    if (endtimer != null) {
                        spectatorSB.setLine(board, "    ");
                        spectatorSB.setSection(board, "§6§lZeit", "§fnoch " + ms.getTime((int) ((cancelTime - System.currentTimeMillis()) / 1000)), false);
                    }
                }
            });
        }
    }

    public void checkSpectators() {
        for (final Map.Entry<UUID, Location> entry : spectators.entrySet()) {
            final Player pl = ms.getServer().getPlayer(entry.getKey());
            if (pl == null || pl.getGameMode() != GameMode.SPECTATOR || pl.getWorld().equals(ms.getBase().getWorld())) {
                spectators.remove(entry.getKey());
                if (pl != null)
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            ms.resetScoreboard(pl);
                        }
                    }.runTask(ms);
            } else if (pl.getLocation().getWorld().getName().equals(entry.getValue().getWorld().getName()) == false || pl.getLocation().distance(entry.getValue()) > maxSpectatorDistance || pl.getLocation().getBlockY() < 13) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        pl.teleport(entry.getValue(), PlayerTeleportEvent.TeleportCause.END_PORTAL);
                    }
                }.runTaskLater(ms, 0L);
            }
        }
    }

    private void updateActionBarWaiting(final boolean darkcolors) {
        if (wait == 0) wait = 1;
        if (wait == 1) {
            sendEventBar((darkcolors ? "§4" : "§c") + "Das Event wird in Kürze beginnen.", null);
            wait = 2;
        } else {
            sendEventBar((darkcolors ? "§1" : "§d") + "Das Event wird in Kürze beginnen.", null);
            wait = 1;
        }
    }

    public void startCountdown(int sec, final boolean canjoinafter, final boolean darkcolors) {
        seconds = (short) sec;
        countdown = new BukkitRunnable() {
            @Override
            public void run() {
                updateCountDown(seconds);
                if (seconds > 0) {
                    if (seconds <= 10) {
                        wait = 0;
                        sendEventBar((darkcolors ? "§0" : "§f") + "Das Event wird " + (darkcolors ? "§2" : "§a") + "in " + seconds + " Sekunde" + (seconds == 1 ? "" : "n") + " " + (darkcolors ? "§0" : "§f") + "starten.", Sound.NOTE_STICKS);
                    } else {
                        updateActionBarWaiting(darkcolors);
                    }
                    players.forEach(player -> ms.sendScoreboard(player, standardsb));
                    seconds--;
                    return;
                } else if (players.size() <= 1) {
                    sendEventBar("§cEs sind nicht genug Spieler da!", Sound.NOTE_BASS);
                    stopEvent(true, false);
                    cancel();
                    return;
                }
                wait = -1;
                canjoin = canjoinafter;
                playerCountAtStart = players.size();
                sendEventBar((darkcolors ? "§0" : "§f") + "Das Event startet jetzt!", Sound.LEVEL_UP);
                if (scoreboard) {
                    players.forEach(player -> {
                        ms.resetScoreboard(player); // Otherwise, buggy scoreboard
                        ms.sendScoreboard(player, standardsb);
                    });
                }
                cancel();
            }
        }.runTaskTimer(ms, 0L, 20L);
    }

    public void spawnEventNPC(final String display) {
        e.getEffects().enable();
        ms.getNPCS().addVillager(2, new Location(ms.getGameSpawn().getWorld(), 19.5D, 77D, -0.5D), BlockFace.EAST, display, joinaction, (h, p) ->
                h.setName(p, canjoin ? display : canSpectate ? "§d§lEvent zuschauen" : "§d§lEvent Infos"));
    }

    public void sendEventMessage(final String msg) {
        for (final Player pl : players) {
            pl.sendMessage(msg);
        }
        for (final UUID uuid : spectators.keySet()) {
            final Player pl = ms.getServer().getPlayer(uuid);
            pl.sendMessage(msg);
        }
    }

    public void sendEventBar(final String msg, final Sound s) {
        for (final Player pl : players) {
            ms.showItemBar(pl, msg);
            if (s != null) pl.playSound(pl.getLocation(), s, 1F, 1F);
        }
    }

    public void resetPlayer(final Player p) {
        final String pname = p.getName();
        ms.sendScoreboard(p, ms.getScoreboard().getStandardsb()); // davor war hier resetScoreboard
        if (p.isInsideVehicle()) {
            p.leaveVehicle();
        }
        if (p.isDead()) p.spigot().respawn();
        else p.setHealth(20D);
        p.setFoodLevel(20);
        p.closeInventory();
        if (keepInventory == false) {
            final PlayerInventory pi = p.getInventory();
            final ItemStack[] inv = backinv.get(pname);
            final Integer level = backexp.get(pname);
            final Integer food = backfood.get(pname);
            pi.setArmorContents(backarmor.get(pname));
            pi.setContents(inv == null ? new ItemStack[0] : inv);
            p.setLevel(level == null ? 0 : level);
            p.setFoodLevel(food == null ? 20 : food);
            backarmor.remove(pname);
            backinv.remove(pname);
            backexp.remove(pname);
            backfood.remove(pname);
        }
        p.getActivePotionEffects().forEach(pe -> p.removePotionEffect(pe.getType()));
        ms.getPerks().deactivatePerks(p.getName());
        ms.getPerks().activatePerks(p);
        players.remove(p);
        p.updateInventory();
        final MuxUser u = ms.getMuxUser(p.getName());
        final Location lastloc = u.getLastLocation(), l = lastloc != null && lastloc.getWorld() != null ? lastloc : ms.getGameSpawn();
        ms.forcePlayer(p, l);
        if (p.getFireTicks() > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline())
                        p.setFireTicks(0);
                }
            }.runTaskLater(ms, 1L);
        }
        if (canSpectate && players.size() > 1 && cancelled == false && timer != null) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    ms.chatClickHoverRun(p, "§d§lMuxEvent>§7 Klicke §dhier§7, um beim Event zuzuschauen.", "§d§oKlicke zum Zuschauen", "/event join");
                }
            }.runTaskLater(ms, 40L);
        }
        final Server sr = ms.getServer();
        if (scoreboard) players.forEach(player -> ms.sendScoreboard(player, standardsb));
        for (Map.Entry<String, List<String>> entry : hiddenplayers.entrySet()) {
            final Player other = sr.getPlayer(entry.getKey());
            if (other == null) continue;
            entry.getValue().removeIf(s -> {
                if (s.equals(p.getName())) {
                    other.showPlayer(p);
                    return true;
                }
                return false;
            });
        }
        final List<String> hidden = hiddenplayers.get(p.getName());
        if (hidden != null)
            hidden.stream().map(sr::getPlayer).filter(Objects::nonNull).forEach(p::showPlayer);
        hiddenplayers.remove(p.getName());
        hiding.remove(p.getName());
    }

    public void equipPlayer(final Player p) {
        p.closeInventory();
        final PlayerInventory pi = p.getInventory();
        final String pname = p.getName();
        if (keepInventory == false && backinv.containsKey(pname) == false) {
            backinv.put(pname, pi.getContents());
            backarmor.put(pname, pi.getArmorContents());
            backexp.put(pname, p.getLevel());
            backfood.put(pname, p.getFoodLevel());
        }
        if (ms.isVanish(p)) {
            p.performCommand("vanish");
        }
        ms.getMenu().disableForcefield(p);
        for (final PotionEffect pe : p.getActivePotionEffects()) {
            p.removePotionEffect(pe.getType());
        }
        if (keepInventory == false) {
            ms.getEmojis().stopEmoji(p);
            pi.clear();
            pi.setArmorContents(null);
            p.setExp(0F);
            p.setLevel(0);
        }
        p.setWalkSpeed(0.2F);
        p.setHealth(20D);
        p.setFoodLevel(20);
        if (p.getFireTicks() > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline()) p.setFireTicks(0);
                }
            }.runTaskLater(ms, 1L);
        }
        p.setAllowFlight(false);
        p.setFlying(false);
        p.setGameMode(breakBlocks ? GameMode.SURVIVAL : GameMode.ADVENTURE);
        ms.removeGodMode(p.getName());
        ms.getPerks().removeActivePerk(p, (byte) 1);
        if (noHunger) ms.getPerks().activatePerk(p, (byte) 1);
        ms.getMounts().deactivateMount(p);
        if (pvp) ms.getPets().deactivatePet(p);
        if (playerHider != -1) {
            p.getInventory().setItem(playerHider, showitem);
            hidePlayers(p);
        }
        p.updateInventory();
        for (final String s : hiding) {
            final Player other = ms.getServer().getPlayer(s);
            if (other != null && players.contains(other)) {
                hiddenplayers.get(other.getName()).add(p.getName());
                other.hidePlayer(p);
            }
        }
    }

    private void hidePlayers(final Player player) {
        hiddenplayers.putIfAbsent(player.getName(), new ArrayList<>());
        final List<String> list = hiddenplayers.get(player.getName());
        hiding.add(player.getName());
        for (final Player other : players) {
            if (other == player || players.contains(other) == false) continue;
            player.hidePlayer(other);
            list.add(other.getName());
        }
    }
}