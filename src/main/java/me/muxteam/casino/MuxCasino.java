package me.muxteam.casino;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.xxmicloxx.NoteBlockAPI.event.SongEndEvent;
import me.muxteam.basic.MuxActions;
import me.muxteam.basic.MuxHolograms;
import me.muxteam.basic.MuxMusic;
import me.muxteam.basic.MuxNPCs;
import me.muxteam.basic.NMSReflection;
import me.muxteam.basic.MuxScoreboard;
import me.muxteam.extras.MuxExtraUser;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.muxsystem.Settings;
import me.muxteam.ranks.MuxRanks;
import net.minecraft.server.v1_8_R3.EntityInsentient;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public final class MuxCasino implements Listener {
    public MuxSystem ms;
    private boolean stripperuse = false;
    private Location loc, roof, lift, goldentry, ultraentry, musicnotes, dancefloor, dancers, npcloc;
    private final Random r = new Random();
    private Sheep dance1 = null;
    private Cow dance2 = null;
    private Villager dance3 = null;
    private UUID topplayer;
    private MuxScoreboard.ScoreboardType sb;
    private org.bukkit.scoreboard.Scoreboard sbchips;
    private List<DyeColor> colors;
    private final Set<UUID> bribed = new HashSet<>();
    private final Set<MuxHolograms.Hologram> topholos = new HashSet<>();
    private final ConcurrentHashMap<Player, MuxCasinoUser> players = new ConcurrentHashMap<>();
    private Map<UUID, Long> withdrawlimits = new HashMap<>();
    private final SlotMachine slotmachine;
    private final Crash crash;
    private final ScratchCards scratch;
    private final WheelOfFortune wheel;
    private final Roulette roulette;
    private final SixFields sixfields;
    private final CoinFlip coinflip;
    private final ItemFlip itemflip;
    private final RockPaperScissors rockpaperscissors;
    private final GuessTheNumber guessnumber;
    private final BlackJack blackjack;
    private final TexasHoldem texasholdem;
    private final CoinPusher coinpusher;
    private final CasinoStats stats;
    private ProtectedRegion ultraregion, goldregion;
    private final MuxMusic music;
    private final Map<Material, Map.Entry<ItemStack, Integer>> bar = new EnumMap<>(Material.class);

    private final double WITHDRAW_FEES = 0.0025;

    public MuxCasino(final MuxSystem ms) {
        this.ms = ms;
        final Gson gson = new Gson();
        final FileConfiguration hashYML = ms.getHashYAML();
        if (hashYML.contains("casinolimit")) {
            withdrawlimits = gson.fromJson(hashYML.getString("casinolimit"), new TypeToken<HashMap<UUID, Long>>() {
            }.getType());
        }
        music = new MuxMusic(ms);
        setup();
        stats = new CasinoStats(this);
        slotmachine = new SlotMachine(this);
        crash = new Crash(this);
        scratch = new ScratchCards(this);
        wheel = new WheelOfFortune(this);
        roulette = new Roulette(this);
        sixfields = new SixFields(this);
        coinflip = new CoinFlip(this);
        itemflip = new ItemFlip(this);
        rockpaperscissors = new RockPaperScissors(this);
        guessnumber = new GuessTheNumber(this);
        blackjack = new BlackJack(this);
        texasholdem = new TexasHoldem(this);
        coinpusher = new CoinPusher(this);
        startSchedulers();
        ms.getServer().getPluginManager().registerEvents(this, ms);
    }

    public void close() {
        final Gson gson = new Gson();
        final FileConfiguration hashYML = ms.getHashYAML();
        hashYML.set("casinolimit", gson.toJson(withdrawlimits));
        slotmachine.close();
        wheel.close();
        roulette.close();
        sixfields.close();
        guessnumber.close();
        coinflip.close();
        rockpaperscissors.close();
        texasholdem.close();
        coinpusher.close();
        stats.close();
        music.close();
        this.ms = null;
    }

    public void reset(final boolean week) {
        withdrawlimits.clear();
        for (final Map.Entry<Player, MuxCasinoUser> entry : players.entrySet()) {
            if (entry.getValue().getEnergy() < 100) {
                entry.getKey().sendMessage("§5§lMuxCasino>§7 Deine Energie wurde wieder aufgeladen.");
                entry.getValue().setEnergy(100);
                sendScoreboard(entry.getKey());
            }
        }
        ms.getDB().resetCasino(week);
    }

    public boolean handleCommand(final Player p) {
        join(p);
        return true;
    }

    public void handleInventory(final Player p, final ItemStack i, final int rawslot, final boolean rightclick, final Inventory inv) {
        if (inCasino(p) == false) {
            p.closeInventory();
            return;
        }
        final Material m = i.getType();
        final MuxCasinoUser cu = players.get(p);
        long c = cu.getChips();
        final String title = p.getOpenInventory().getTopInventory().getTitle();
        if (m == Material.INK_SACK && title.contains("Bank")) {
            final MuxUser u = ms.getMuxUser(p.getName());
            final int multiplier = u.getCoins() > 4999999 ? 100 : u.getCoins() > 499999 ? 10 : 1;
            int coins = 0;
            if (i.getAmount() == 1) {
                coins = 1000 * multiplier;
            } else if (i.getAmount() == 5) {
                coins = 5000 * multiplier;
            } else if (i.getAmount() == 20) {
                coins = 20000 * multiplier;
            }
            if (coins > u.getCoins()) {
                ms.showItemBar(p, "§cDu hast nicht genug MuxCoins für die Chips.");
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                ms.chatClickHoverLink(p, ms.getLang("shop.clickformuxcoins"), ms.getLang("shop.hoverformuxcoins"), "https://shop.muxcraft.eu/?ign=" + p.getName());
                p.closeInventory();
                return;
            }
            changeCoins(p, -coins);
            c += coins * 10;
            getStats().CHIPS_BOUGHT += coins * 10;
            p.playSound(p.getLocation(), Sound.NOTE_PIANO, 1F, 10F);
            cu.setChips(c);
            ms.showItemBar(p, "§fDu hast §d" + ms.getNumberFormat(coins * 10L) + " §fChips gekauft.");
            chipsEffect(Material.INK_SACK);
            sendScoreboard(p);
            p.closeInventory();
        } else if (m == Material.DOUBLE_PLANT && title.contains("Bank")) {
            if (c == 0) {
                p.closeInventory();
                ms.showItemBar(p, "§cDu hast keine MuxCoins zum abbuchen.");
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                return;
            }
            final long alreadywithdrawn = withdrawlimits.getOrDefault(p.getUniqueId(), 0L);
            final long cashout = Math.min(getDailyWithdrawLimit() - alreadywithdrawn, c);
            if (cashout < 1) {
                p.closeInventory();
                ms.showItemBar(p, "§cDu hast das Abhebungslimit erreicht für heute.");
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                return;
            }
            c -= cashout;
            getStats().CHIPS_WITHDRAWN += cashout;
            cu.setChips(c);
            final int coins = (int) ((cashout * (1 - WITHDRAW_FEES)) / 10);
            final int bankfees = (int) (cashout * WITHDRAW_FEES);
            getStats().BANKACCOUNT += bankfees;
            getStats().BANKFEES += bankfees;
            withdrawlimits.put(p.getUniqueId(), alreadywithdrawn + cashout);
            changeCoins(p, coins);
            p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.4F, 1F);
            p.sendMessage(String.format("§5§lMuxCasino>§7 Dir wurden §e%s MuxCoins §7gutgeschrieben.", ms.getNumberFormat(coins)));
            chipsEffect(Material.DOUBLE_PLANT);
            sendScoreboard(p);
            p.closeInventory();
        } else if (bar.get(m) != null && title.contains("Bar")) {
            final Map.Entry<ItemStack, Integer> offers = bar.get(m);
            if (canBuy(p, offers.getValue(), "Barkeeper", false) == false) return;
            getStats().BARKEEPER += offers.getValue();
            p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.4F, 1F);
            int e = cu.getEnergy();
            e = Math.min(100, e + (offers.getValue() / 10));
            cu.setEnergy(e);
            sendScoreboard(p);
            p.closeInventory();
            if (m == Material.COOKED_BEEF || m == Material.POTATO_ITEM) {
                p.setFoodLevel(20);
                p.setSaturation(20);
                p.playSound(p.getLocation(), Sound.EAT, 1F, 1F);
            } else if (m == Material.GOLDEN_CARROT || m == Material.WATER_BUCKET) {
                if (p.hasPotionEffect(PotionEffectType.SPEED) == false && m == Material.GOLDEN_CARROT)
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 10));
                p.playSound(p.getLocation(), Sound.GLASS, 1F, 1F);
                p.playSound(p.getLocation(), Sound.DRINK, 1F, 1F);
            } else if (m == Material.POTION) {
                p.removePotionEffect(PotionEffectType.CONFUSION);
                p.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 200, 10));
                p.playSound(p.getLocation(), Sound.GLASS, 1F, 1F);
                p.playSound(p.getLocation(), Sound.DRINK, 1F, 1F);
            }
            ms.showItemBar(p, "§fDu hast ein §d" + ChatColor.stripColor(offers.getKey().getItemMeta().getDisplayName()) + " §fgekauft.");
        } else if (m == Material.EMPTY_MAP && title.contains("Psst")) {
            if (bribed.contains(p.getUniqueId())) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            } else if (canBuy(p, 50000, "Gold Bereich", false) == false) return;
            p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.4F, 1F);
            bribed.add(p.getUniqueId());
            for (final Player pl : players.keySet()) {
                if (pl.hasPermission("muxsystem.gold")) {
                    pl.sendMessage("§5§lMuxCasino>§7 Der Spieler §d" + p.getName() + " §7hat die Guards bestochen.");
                }
            }
            ms.sendNormalTitle(p, "§aHerzlich willkommen, Mafiaboss.", 10, 50, 10);
            p.closeInventory();
        } else if (m == Material.RECORD_9 && title.contains("Musik")) {
            final String song = ChatColor.stripColor(i.getItemMeta().getDisplayName());
            if (music.isPlaying()) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            } else if (canBuy(p, 1000, "Musik", false) == false) return;
            p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.4F, 1F);
            music.playSong(song, players.keySet(), false);
            ms.showItemBar(p, "§fDer Song §3" + music.getSongName() + " §fspielt jetzt ab.");
            p.closeInventory();
        } else if ((m == Material.RECORD_8 || m == Material.RECORD_11) && title.contains("Musik")) {
            final Settings s = ms.getMuxUser(p.getName()).getSettings();
            s.setMusic(s.hasMusic() ^ true);
            if (s.hasMusic()) music.add(p);
            else music.remove(p);
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            openMusic(p);
        } else if (m == Material.JUKEBOX && title.contains("Musik")) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
        } else {
            if (texasholdem.onInvClick(p, i, rightclick, title)) return;
            if (checkNoEnergy(p)) {
                p.closeInventory();
                return;
            }
            crash.onInvClick(p, i, rightclick, title);
            scratch.onInvClick(p, i, rawslot, title);
            roulette.onInvClick(p, i, rightclick);
            sixfields.onInvClick(p, i, rawslot, rightclick);
            coinflip.onInvClick(p, i, rightclick, title);
            itemflip.onInvClick(p, i, rawslot, title);
            rockpaperscissors.onInvClick(p, i, rightclick, title, inv);
            blackjack.onInvClick(p, i, rightclick, title);
            guessnumber.onInvClick(p, i, rawslot, title, inv, rightclick);
        }
    }

    public void handleQuit(final Player p, final boolean teleport) {
        quit(p, teleport);
    }

    public void disable() {
        for (final Player pl : players.keySet()) {
            quit(pl, true);
        }
    }

    public Location getNPCLoc() {
        return npcloc;
    }


    public boolean isEnabled() {
        return ms.getAdmin().CASINO.isActive();
    }

    public void respawn() {
        if (dance1 == null || dance2 == null || dance3 == null || dance1.isValid() == false || dance2.isValid() == false || dance3.isValid() == false) {
            dance1 = (Sheep) dancers.getWorld().spawnEntity(dancers.clone().add(-0.3D, 0D, 1D), EntityType.SHEEP);
            dance2 = (Cow) dancers.getWorld().spawnEntity(dancers.clone().add(0.2D, 0D, -1D), EntityType.COW);
            dance3 = (Villager) dancers.getWorld().spawnEntity(dancers, EntityType.VILLAGER);
            dance3.setAdult();
            dance3.setProfession(Villager.Profession.BUTCHER);
            dance1.setAdult();
            dance1.setColor(DyeColor.CYAN);
            dance2.setBaby();
            freezeEntity(dance1);
            freezeEntity(dance2);
            freezeEntity(dance3);
        }
    }

    private void quit(final Player p, final boolean teleport) {
        final MuxCasinoUser cu = players.remove(p);
        if (cu != null) {
            scratch.quit(p);
            guessnumber.quit(p);
            rockpaperscissors.quit(p, true);
            crash.cashout(p, true);
            blackjack.quit(p);
            texasholdem.quit(p);
            itemflip.getFrom(p).ifPresent(ItemFlipGame::reset);
            if (music.isPlaying()) music.remove(p);
            ms.getDB().saveCasino(cu);
            if (teleport) ms.forcePlayer(p, ms.getGameSpawn());
            ms.resetScoreboard(p);
            ms.getScoreboard().setHealth(p);
            final MuxExtraUser u = ms.getExtras().getExtraUser(p.getUniqueId());
            if (u.getPerks().getActive().contains((byte) 2)) {
                ms.getPerks().activatePerk(p, (byte) 2);
            }
        }
    }

    public void join(final Player p) {
        if (isEnabled() == false && p.isOp() == false) {
            ms.showItemBar(p, "§cDas Casino ist derzeit geschlossen.");
            return;
        } else if (p.isOp() == false) {
            final ZoneId zone = ZoneId.of("CET");
            final LocalDateTime current = LocalDateTime.now(zone), _18DateTime = LocalDateTime.now(zone).withHour(18).withMinute(0);
            final DayOfWeek dow = current.getDayOfWeek();
            if (dow.getValue() >= DayOfWeek.MONDAY.getValue() && current.getDayOfWeek().getValue() <= DayOfWeek.THURSDAY.getValue()) {
                if (current.isBefore(_18DateTime) || current.isAfter(LocalDateTime.now(zone).withHour(23).withMinute(0))) {
                    ms.showItemBar(p, "§cDas Casino ist heute von 18-23 Uhr geöffnet.");
                    return;
                }
            } else if (dow.getValue() == DayOfWeek.FRIDAY.getValue()) {
                if (current.isBefore(_18DateTime)) {
                    ms.showItemBar(p, "§cDas Casino öffnet erst um 18 Uhr.");
                    return;
                }
            } else if (dow.getValue() == DayOfWeek.SATURDAY.getValue()) {
                if (current.isBefore(_18DateTime) && current.isAfter(LocalDateTime.now(zone).withHour(2).withMinute(0))) {
                    ms.showItemBar(p, "§cDas Casino öffnet erst um 18 Uhr.");
                    return;
                }
            } else { // SUNDAY
                if ((current.isBefore(_18DateTime) || current.isAfter(LocalDateTime.now(zone).withHour(23).withMinute(0))) && current.isAfter(LocalDateTime.now(zone).withHour(2).withMinute(0))) {
                    ms.showItemBar(p, "§cDas Casino ist heute von 18-23 Uhr geöffnet.");
                    return;
                }
            }
        }
        final MuxUser u = ms.getMuxUser(p.getName());
        if (ms.hasVoted(p) == false && u.getOnlineTime() > 10800000L) {
            ms.sendNoVoteMessage(p);
            return;
        } else if (inCasino(p)) {
            ms.showItemBar(p, "§cDu bist bereits im Casino.");
            return;
        }
        final RegionManager rgmanager = WGBukkit.getRegionManager(p.getWorld());
        if (rgmanager.getApplicableRegions(p.getLocation()).size() > 0 && rgmanager.getApplicableRegions(p.getLocation()).getFlag(DefaultFlag.PVP) != StateFlag.State.DENY) {
            ms.showItemBar(p, "§cDieser Befehl ist hier nicht erlaubt.");
            return;
        }
        if (ms.checkGeneralCooldown(p.getName(), "CASINOJOIN", 2000L, true)) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (ms.getMuxUser(p.getName()).getSettings().hasMusic() && music.isPlaying()) music.add(p);
                players.put(p, ms.getDB().loadCasino(p.getUniqueId()));
                p.setScoreboard(sbchips);
                sendScoreboard(p);
            }
        }.runTaskAsynchronously(ms);
        ms.getPerks().removeActivePerk(p, (byte) 2);
        ms.getMounts().deactivateMount(p);
        ms.setLastLocation(p);
        ms.teleportPlayer(p, loc);
        ms.getNewbies().handleNewbieCasino(p);
        ms.getAnalytics().getAnalytics().addCasinoPlayer(p.getUniqueId());
        ms.showItemBar(p, "§fDu wurdest zum §6Casino §fteleportiert.");
    }

    public void announceCasinoOpening() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (final Player pl : ms.getServer().getOnlinePlayers()) {
                    ms.chatClickHoverRun(pl, "§5§lMuxCasino>§a Das Casino ist jetzt geöffnet.", "§d§oKlicke um beizutreten", "/casino");
                    ms.chatClickHoverRun(pl, "§5§lMuxCasino>§7 Klicke §5hier§7, um beizutreten.", "§d§oKlicke um beizutreten", "/casino");
                    pl.playSound(pl.getLocation(), Sound.ZOMBIE_METAL, 1F, 0.1F);
                }
            }
        }.runTaskLater(ms, 20L * 5L);
    }

    public boolean inCasino(final Player p) {
        return players.containsKey(p);
    }

    public boolean canBuy(final Player p, final int price, final String service) {
        return canBuy(p, price, service, true);
    }

    public boolean canBuy(final Player p, final int price, final String service, final boolean game) {
        final MuxCasinoUser cu = players.get(p);
        long c = cu.getChips();
        if (price > c) {
            ms.showItemBar(p, "§cDu hast nicht genug Chips.");
            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
            p.closeInventory();
            return false;
        }
        ms.getHistory().addHistory(p.getUniqueId(), null, "CASINO", "-" + price, service);
        c -= price;
        if (game) cu.setWeeklyWins(cu.getWeeklyWins() - price);
        getStats().BANKACCOUNT += price;
        ms.getAnalytics().getAnalytics().addGambledChips(price);
        cu.setChips(c);
        sendScoreboard(p);
        return true;
    }

    public void addChips(final UUID uuid, final int add, final String service) {
        final Player p = ms.getServer().getPlayer(uuid);
        if (p != null && players.containsKey(p))
            addChips(p, add, service);
        else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    addChipsOffline(uuid, add, service);
                }
            }.runTaskAsynchronously(ms);
        }
    }

    public void addChips(final Player p, final int add, final String service) {
        final MuxCasinoUser cu = players.get(p);
        if (cu == null) return;
        cu.setWeeklyWins(cu.getWeeklyWins() + add);
        long c = cu.getChips();
        getStats().BANKACCOUNT -= add;
        c += add;
        cu.setChips(c);
        ms.getHistory().addHistory(p.getUniqueId(), null, "CASINO", "+" + add, service);
        sendScoreboard(p);
    }

    public void addChipsOffline(final Player p, final int add, final String service) {
        addChipsOffline(p.getUniqueId(), add, service);
    }

    public void addChipsOffline(final UUID uuid, final int add, final String service) {
        final MuxCasinoUser cu = ms.getDB().loadCasino(uuid);
        cu.setWeeklyWins(cu.getWeeklyWins() + add);
        long c = cu.getChips();
        getStats().BANKACCOUNT -= add;
        c += add;
        cu.setChips(c);
        ms.getHistory().addHistory(uuid, null, "CASINO", "+" + c, service);
        ms.getDB().saveCasino(cu);
    }

    public MuxCasinoUser getCasinoUser(final UUID uuid) {
        final Player pl = ms.getServer().getPlayer(uuid);
        if (pl != null && players.containsKey(pl)) return players.get(pl);
        return ms.getDB().loadCasino(uuid);
    }

    public MuxCasinoUser getCasinoUser(final Player pl) {
        return players.get(pl);
    }

    public Location getSpawn() {
        return loc;
    }

    public World getWorld() {
        return loc.getWorld();
    }

    public void createButton(final Inventory i, int pos, final ItemStack item) {
        ms.createButton(i, pos, item);
    }

    @EventHandler
    public void onInteractAtEntity(final PlayerInteractAtEntityEvent e) {
        final Player p = e.getPlayer();
        final Entity clicked = e.getRightClicked();
        if (p.getWorld() != getWorld() || inCasino(p) == false || clicked instanceof ArmorStand == false) return;
        if (checkNoEnergy(p) || coinpusher.onInteractAtEntity(p, clicked)) e.setCancelled(true);
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        final Block b = e.getClickedBlock();
        if ((e.getAction() == Action.LEFT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_BLOCK) && inCasino(e.getPlayer())) {
            final Player p = e.getPlayer();
            if (b.getState() instanceof Sign) {
                final Sign s = (Sign) e.getClickedBlock().getState();
                if (ms.checkGeneralCooldown(p.getName(), "CASINOSIGN", 1000L, true)) return;
                if (s.getLine(0).contains("§lGeldregen") == false) {
                    texasholdem.onInteract(p, s);
                    if (checkNoEnergy(p)) return;
                    slotmachine.onInteract(p, s);
                    crash.onInteract(p, s);
                    wheel.onInteract(p, s);
                    roulette.onInteract(p, s);
                    sixfields.onInteract(p, s);
                    coinflip.onInteract(p, s);
                    rockpaperscissors.onInteract(p, s);
                    coinpusher.onInteract(p, s);
                    return;
                }
                if (stripperuse) {
                    ms.showItemBar(p, "§cEs läuft bereits ein Geldregen.");
                    p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
                    return;
                }
                if (canBuy(p, 2000, "Stripper", false) == false) return;
                stripperuse = true;
                moneyRain(Material.DOUBLE_PLANT, b.getLocation(), Material.GOLD_BLOCK);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        stripperuse = false;
                    }
                }.runTaskLater(ms, 200L);
            } else if (b.getType() == Material.STONE_BUTTON) {
                final Location l = b.getLocation();
                if (l.getBlockX() != -17 || l.getBlockZ() != -145) {
                    return;
                } else if (e.getPlayer().isOp() == false) {
                    ms.showItemBar(e.getPlayer(), "§cDu bist nicht dafür berechtigt.");
                    return;
                }
                stats.openCasinoStats(p);
            }
            return;
        }
        if (e.getAction() != Action.PHYSICAL) return;
        final Player p = e.getPlayer();
        if (b != null && b.getType() == Material.GOLD_PLATE && inCasino(p)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 10));
            ms.forcePlayer(p, roof);
            p.playSound(p.getLocation(), Sound.ENDERMAN_TELEPORT, 1F, 10F);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(final PlayerDropItemEvent e) {
        final Player p = e.getPlayer();
        if (inCasino(p) && (rockpaperscissors.onDrop(p) || texasholdem.onDrop(p))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(final PlayerPickupItemEvent e) {
        if (particledrops.contains(e.getItem())) e.setCancelled(true);
        final Player p = e.getPlayer();
        if (inCasino(p) && rockpaperscissors.onPickup(p)) {
            e.setCancelled(true);
        }
    }

    public void save() {
        players.values().parallelStream().filter(Objects::nonNull).forEach(ms.getDB()::saveCasino);
    }

    public double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }

    public long getChips(final Player p) {
        final MuxCasinoUser cu = players.get(p);
        return cu.getChips();
    }

    public int getNextStakeChange(final int stake, final boolean more) {
        return more ? (stake > 49999999 ? 10000000 : stake > 4999999 ? 1000000 : stake > 499999 ? 100000 : stake > 99999 ? 50000 : stake > 19999 ? 10000 : stake > 4999 ? 5000 : stake > 999 ? 1000 : 500) : (stake > 599999 ? 100000 : stake > 149999 ? 50000 : stake > 29999 ? 10000 : stake > 9999 ? 5000 : stake > 1999 ? 1000 : 500);
    }

    public void broadcast(final String message) {
        for (final Player pl : players.keySet()) {
            pl.sendMessage(message);
        }
    }

    public void addEnergy(final Player p, final int energy) {
        final MuxCasinoUser cu = players.get(p);
        cu.setEnergy(cu.getEnergy() + energy);
        if (cu.getEnergy() >= 100) cu.setEnergy(100);
    }

    public boolean checkNoEnergy(final Player p) {
        final MuxCasinoUser cu = players.get(p);
        if (cu.getEnergy() == 0) {
            ms.showItemBar(p, "§cDu hast keine Energie mehr.");
            ms.sendNormalTitle(p, "§fHole dir etwas bei der Bar.", 20, 60, 20);
            return true;
        }
        if (r.nextInt(4) == 0) cu.setEnergy(cu.getEnergy() - 1);
        return false;
    }

    public void moneyRain(final Material m, final Location l, final Material block) {
        final ArmorStand a = (ArmorStand) l.getWorld().spawnEntity(l.clone().add(0.5, 0, 0.5), EntityType.ARMOR_STAND);
        a.setVisible(false);
        a.setHelmet(new ItemStack(block));
        a.setVelocity(new Vector(0, 1, 0));
        a.setMarker(true);
        new BukkitRunnable() {
            @Override
            public void run() {
                l.getWorld().playSound(l, Sound.EXPLODE, 1F, 1F);
                ms.itemRain(l.clone(), m);
                a.remove();
            }
        }.runTaskLater(ms, 26L);
    }

    public CasinoStats getStats() {
        return stats;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(final PlayerInteractEntityEvent e) {
        final Entity en = e.getRightClicked();
        if (en == dance1 || en == dance2 || en == dance3) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageByEntity(final EntityDamageByEntityEvent e) {
        if (e.getEntity().getType() == EntityType.ARMOR_STAND && e.getEntity().getLocation().getWorld().equals(loc.getWorld())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(final EntityDamageEvent e) {
        if (e.getEntity().getType() == EntityType.PLAYER && e.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION && inCasino((Player) e.getEntity()))
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onSongEnd(final SongEndEvent ignored) {
        dance1.teleport(dancers.clone().add(-0.3, 0, 1));
        dance2.teleport(dancers.clone().add(0.2, 0, -1.0));
        dance3.teleport(dancers);
        for (int x = 0; x <= 3; x++) {
            for (int z = 0; z <= 4; z++) {
                dancefloor.clone().add(x, 0, -z).getBlock().setType(Material.SANDSTONE);
            }
        }
    }

    public void sendScoreboard(final Player p) {
        ms.sendScoreboard(p, sb);
        final long chips = players.get(p).getChips();
        sbchips.getObjective("Chips").getScore(p.getName()).setScore(chips > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) chips);
    }

    public long getDailyWithdrawLimit() {
        final long supply = ms.getActiveChipsSupply();
        final double percentage = 0.04D;
        long result = (long) Math.ceil((supply * percentage));
        int n = (int) Math.ceil(Math.log10(result + 1D));
        n = Math.max(0, n - 1);
        result = (long) Math.ceil(result / Math.pow(10, n)) * (long) Math.pow(10, n);
        return Math.max(result, 100000L);
    }

    private void changeCoins(final Player p, final int coins) {
        ms.getHistory().addHistory(p.getUniqueId(), null, "COINS", String.valueOf(coins), "Casino");
        final MuxUser u = ms.getMuxUser(p.getName());
        u.addCoins(coins);
    }

    private void openBank(final Player p) {
        if (inCasino(p) == false) return;
        final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lMuxCasino§0 | Bank");
        final MuxUser u = ms.getMuxUser(p.getName());
        final long multiplier = u.getCoins() > 4999999 ? 100L : u.getCoins() > 499999 ? 10L : 1L;
        inv.setItem(10, ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 9), "§f§l" + ms.getNumberFormat(10000 * multiplier) + " Chips",
                "§7Preis: §e" + ms.getNumberFormat(1000 * multiplier) + " MuxCoins", "", "§7Klicke, um sie zu §fkaufen§7."));
        inv.setItem(12, ms.renameItemStack(new ItemStack(Material.INK_SACK, 5, (byte) 13), "§f§l" + ms.getNumberFormat(50000 * multiplier) + " Chips",
                "§7Preis: §e" + ms.getNumberFormat(5000 * multiplier) + " MuxCoins", "", "§7Klicke, um sie zu §fkaufen§7."));
        inv.setItem(14, ms.renameItemStack(new ItemStack(Material.INK_SACK, 20, (byte) 5), "§f§l" + ms.getNumberFormat(200000 * multiplier) + " Chips",
                "§7Preis: §e" + ms.getNumberFormat(20000 * multiplier) + " MuxCoins", "", "§7Klicke, um sie zu §fkaufen§7."));
        inv.setItem(16, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT), "§e§lMuxCoins abheben", "§7Tausche alle deine Chips",
                "§7wieder in MuxCoins um.", "", "§7Gebühr: §e0,25%", "", "§7Klicke, um sie zu §everkaufen§7."));
        if (ms.getActiveInv(p.getName()) != InvType.CASINO) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.CASINO);
    }

    private void openMusic(final Player p) {
        if (inCasino(p) == false) return;
        final Inventory inv = ms.getServer().createInventory(null, 36, "§0§lMuxCasino§0 | Musik");

        if (ms.getActiveInv(p.getName()) != InvType.CASINO) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.CASINO);

        short pos = 18;
        final File folder = new File(ms.getDataFolder() + "/music");
        if (folder.exists() == false) return;
        final boolean playing = music.isPlaying();
        final File[] files = folder.listFiles();
        if (files != null) {
            for (final File s : files) {
                final ItemStack song = ms.renameItemStack(new ItemStack(Material.RECORD_9, 1), "§f§l" + s.getName().replace(".nbs", ""), "§7Preis: §d1.000 Chips", "", playing ? "§cWarte bis der jetzige" : "§7Klicke, um diesen Song", playing ? "§cSong zuende ist." : "§7jetzt §fabzuspielen§7.");
                final ItemMeta songm = song.getItemMeta();
                songm.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
                song.setItemMeta(songm);
                inv.setItem(pos, song);
                pos++;
            }
        }
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.JUKEBOX), "§3§lMusik",
                playing ? new String[]{"§7Spiele deine Lieblingsmusik für", "§7alle Spieler im Casino ab.",
                        "", "§7Jetzige Musik:", "§3" + music.getSongName()} :
                        new String[]{"§7Spiele deine Lieblingsmusik für", "§7alle Spieler im Casino ab."}));
        final boolean hasmusic = ms.getMuxUser(p.getName()).getSettings().hasMusic();
        final ItemStack setting = ms.renameItemStack(new ItemStack(hasmusic ? Material.RECORD_8 : Material.RECORD_11),
                "§3§lTon" + (hasmusic ? "" : "§c §ldeaktiviert"), "§7Du kannst die Musik im Casino", "§7jederzeit für dich ausschalten.",
                "", "§7Klicke, um die Musik", "§7zu " + (hasmusic ? "§cdeaktivieren" : "§aaktivieren") + "§7.");
        final ItemMeta sm = setting.getItemMeta();
        sm.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        setting.setItemMeta(sm);
        inv.setItem(8, hasmusic == false ? ms.addGlow(setting) : setting);
    }

    private void openBarKeeper(final Player p) {
        if (inCasino(p) == false) return;
        final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lMuxCasino§0 | Bar");
        int pos = 9;
        for (final Map.Entry<ItemStack, Integer> entry : bar.values()) {
            inv.setItem(pos, ms.renameItemStack(entry.getKey().clone(), entry.getKey().getItemMeta().getDisplayName(),
                    "§7Preis: §d" + entry.getValue() + " Chips", "§7Energie: §3+" + (entry.getValue() / 10), "", "§7Klicke, um es zu §fkaufen§7."));
            pos += 2;
        }
        if (ms.getActiveInv(p.getName()) != InvType.CASINO) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.CASINO);
    }

    private void openBribe(final Player p) {
        if (inCasino(p) == false) return;
        else if (bribed.contains(p.getUniqueId()) || p.hasPermission("muxsystem.gold")) {
            ms.showItemBar(p, "§aHereinspatziert...");
            return;
        }
        final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lPsst, willst du rein?");
        inv.setItem(13, ms.renameItemStack(new ItemStack(Material.EMPTY_MAP), "§c§lBestechen", "§7Preis: §d50.000 Chips", "", "§7Klicke, um den", "§7Guard zu §cbestechen§7."));
        if (ms.getActiveInv(p.getName()) != InvType.CASINO) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.CASINO);
    }

    private void startSchedulers() {
        new BukkitRunnable() {
            @Override
            public void run() {
                ms.playEffect(EnumParticle.FLAME, lift, 0.5F, 0.5F, 0.5F, 0.0001F, 1);
                ms.playEffect(EnumParticle.FIREWORKS_SPARK, goldentry, 0.1F, 1.5F, 1.5F, 0.0001F, 1);
                ms.playEffect(EnumParticle.FIREWORKS_SPARK, ultraentry, 0.1F, 1.5F, 1.5F, 0.0001F, 1);
                slotmachine.onUpdate();
                coinflip.onUpdateHalf();
                itemflip.onUpdateHalf();
                wheel.onUpdate();
                rockpaperscissors.onUpdate();
                coinpusher.onUpdate();
            }
        }.runTaskTimer(ms, 10L, 2L);
        new BukkitRunnable() {
            @Override
            public void run() {
                roulette.onUpdate();
            }
        }.runTaskTimer(ms, 20L, 1L);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (music.isPlaying()) {
                    ms.playEffect(EnumParticle.NOTE, musicnotes, 0.5F, 0.5F, 0.5F, 0.0001F, 5);
                    danceFloor();
                }
                sendParticles();
                crash.onUpdate();
                sixfields.onUpdate();
                scratch.onUpdate();
                coinflip.onUpdate();
                itemflip.onUpdate();
            }
        }.runTaskTimer(ms, 20L, 10L);
        new BukkitRunnable() {
            @Override
            public void run() {
                guessnumber.onUpdate();
                texasholdem.onUpdate();
                for (final Player pl : players.keySet()) {
                    if (pl == null || pl.isOnline() == false) continue;
                    final Location l = pl.getLocation();
                    if (pl.hasPermission("muxsystem.ultra") == false && ultraregion != null && ultraregion.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) {
                        final Location ultra = ultraentry.clone().subtract(5, 2, 0);
                        ultra.setYaw(-90F);
                        ms.forcePlayer(pl, ultra);
                    } else if (pl.hasPermission("muxsystem.gold") == false && goldregion != null && goldregion.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ()) && bribed.contains(pl.getUniqueId()) == false) {
                        final Location gold = goldentry.clone().add(5, -2, 0);
                        gold.setYaw(90F);
                        ms.forcePlayer(pl, gold);
                    }
                    if (ms.getScoreboard().getCurrent(pl) != sb) {
                        sendScoreboard(pl);
                    }
                    crash.pushAway();
                }
            }
        }.runTaskTimer(ms, 20L, 20L);
        new BukkitRunnable() {
            // there is an auto restart @ 5 in the morning, so we can just cancel this task
            @Override
            public void run() {
                final ZoneId zone = ZoneId.of("CET");
                final LocalDateTime current = LocalDateTime.now(zone);
                final DayOfWeek dow = current.getDayOfWeek();
                final LocalDateTime _18DateTime = LocalDateTime.now().withHour(18).withMinute(0);
                if (dow.getValue() >= DayOfWeek.MONDAY.getValue() && current.getDayOfWeek().getValue() <= DayOfWeek.THURSDAY.getValue()) {
                    final LocalDateTime end = LocalDateTime.now().withHour(23).withMinute(0);
                    if (current.isAfter(end)) {
                        closeCasinoAfterDate();
                        cancel();
                    }
                } else if (dow.getValue() == DayOfWeek.FRIDAY.getValue() || dow.getValue() == DayOfWeek.SATURDAY.getValue()) {
                    if ((dow == DayOfWeek.SATURDAY && (current.isBefore(LocalDateTime.now(zone).withHour(18).withMinute(0))) && current.isAfter(LocalDateTime.now(zone).withHour(2).withMinute(0)))
                            || (dow == DayOfWeek.FRIDAY && current.isAfter(LocalDateTime.now(zone).withHour(2).withMinute(0)) && current.isBefore(LocalDateTime.now(zone).withHour(3)))) {
                        closeCasinoAfterDate();
                        cancel();
                    }
                } else if (dow.getValue() == DayOfWeek.SUNDAY.getValue()) {
                    if (current.isBefore(_18DateTime)) {
                        if (current.isAfter(LocalDateTime.now().withHour(2).withMinute(0)) && current.isBefore(LocalDateTime.now(zone).withHour(3))) {
                            closeCasinoAfterDate();
                            cancel();
                        }
                    } else if (current.isAfter(LocalDateTime.now(zone).withHour(23).withMinute(0))) {
                        closeCasinoAfterDate();
                        cancel();
                    }
                    if (current.isAfter(LocalDateTime.now(zone).withHour(2).withMinute(0)) && current.isBefore(LocalDateTime.now(zone).withHour(3).withMinute(0))) {
                        closeCasinoAfterDate();
                        cancel();
                    }
                }
            }
        }.runTaskTimerAsynchronously(ms, 200L, 200L);
    }

    public void updateHologram() {
        final MuxHolograms.Hologram hologram = ms.getHolograms().getPluginHolos().get("casinoinfo");
        if (isEnabled() == false) {
            hologram.setMessage("§cWartungen");
            return;
        }
        boolean open = true;
        final ZoneId zone = ZoneId.of("CET");
        final LocalDateTime current = LocalDateTime.now(zone), _18DateTime = LocalDateTime.now(zone).withHour(18).withMinute(0);
        final DayOfWeek dow = current.getDayOfWeek();
        if (dow.getValue() >= DayOfWeek.MONDAY.getValue() && current.getDayOfWeek().getValue() <= DayOfWeek.THURSDAY.getValue()) {
            if (current.isBefore(_18DateTime) || current.isAfter(LocalDateTime.now(zone).withHour(23).withMinute(0))) {
                open = false;
            }
        } else if (dow.getValue() == DayOfWeek.FRIDAY.getValue()) {
            if (current.isBefore(_18DateTime)) {
                open = false;
            }
        } else if (dow.getValue() == DayOfWeek.SATURDAY.getValue()) {
            if (current.isBefore(_18DateTime) && current.isAfter(LocalDateTime.now(zone).withHour(2).withMinute(0))) {
                open = false;
            }
        } else { // SUNDAY
            if ((current.isBefore(_18DateTime) || current.isAfter(LocalDateTime.now(zone).withHour(23).withMinute(0))) && current.isAfter(LocalDateTime.now(zone).withHour(2).withMinute(0))) {
                open = false;
            }
        }
        if (open) {
            hologram.setMessage("§aJetzt geöffnet");
        } else {
            if (current.getHour() == 17 || current.getHour() == 18) {
                final LocalDateTime to = LocalDateTime.now(zone).withHour(current.getHour() + 1).withMinute(0).withSecond(0);
                hologram.setMessage("§6In " + ms.clockTimeToString(to.get(ChronoField.SECOND_OF_DAY) - current.get(ChronoField.SECOND_OF_DAY)));
            } else if (current.getHour() > 4) {
                hologram.setMessage("§6Ab 18 Uhr");
            } else {
                hologram.setMessage("§cGeschlossen");
            }
        }
    }


    private void closeCasinoAfterDate() {
        if (Bukkit.isPrimaryThread() == false) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    closeCasinoAfterDate();
                }
            }.runTask(ms);
            return;
        }
        ms.getHolograms().getPluginHolos().get("casinoinfo").setMessage("§cGeschlossen");
        ImmutableMap.copyOf(players).forEach((p, cu) -> {
            ms.sendTitle(p, "§5§lMuxCasino", 2, 20 * 5, 20);
            ms.sendSubTitle(p, "§fDas Casino wurde geschlossen.", 2, 20 * 5, 20);
            p.sendMessage("§5§lMuxCasino>§a Das Casino hat für heute geschlossen. Komme morgen wieder!");
            quit(p, true);
        });
    }

    private final CopyOnWriteArraySet<Item> particledrops = new CopyOnWriteArraySet<>();

    private void sendParticles() {
        if (topplayer == null) return;
        final Player p = ms.getServer().getPlayer(topplayer);
        if (p == null || ms.isVanish(p)) return;
        if (inCasino(p)) {
            final int ra = r.nextInt(3);
            final ItemStack item = ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (short) (ra == 0 ? 9 : ra == 1 ? 5 : 13)), String.valueOf(r.nextDouble()));
            final Item i = p.getWorld().dropItemNaturally(p.getLocation().clone().add(0, 0.6, 0), item);
            particledrops.add(i);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (i != null) {
                        particledrops.remove(i);
                        i.remove();
                    }
                }
            }.runTaskLater(ms, 20L);
        }
    }

    private boolean danceup = true;

    private void danceFloor() {
        if (music.isPlaying() == false) return;
        Collections.shuffle(colors);
        dance1.setColor(colors.get(0));
        for (int x = 0; x <= 3; x++) {
            for (int z = 0; z <= 4; z++) {
                final Block b = dancefloor.clone().add(x, 0, -z).getBlock();
                b.setType(Material.STAINED_GLASS);
                b.setData((byte) r.nextInt(14));
            }
        }
        if (danceup) {
            danceup = false;
            final Location d = dancers.clone();
            d.setPitch(40F);
            dance1.teleport(d.clone().add(-0.3, 0, 1));
            dance2.teleport(d.clone().add(0.2, 0, -1.0));
            dance3.teleport(d.clone());
        } else {
            danceup = true;
            final Location d = dancers.clone();
            d.setPitch(-40F);
            dance1.teleport(d.clone().add(-0.3, 0, 1));
            dance2.teleport(d.clone().add(0.2, 0, -1.0));
            dance3.teleport(d.clone());
        }
    }

    public void updateTop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                final List<MuxCasinoUser> topweekly = ms.getDB().getWeekly5(), top5 = ms.getDB().getTop5();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (final MuxHolograms.Hologram holo : topholos) {
                            holo.remove();
                        }
                        topholos.clear();
                        int step = 0;
                        final MuxHolograms h = ms.getHolograms();
                        final Location weekly = new Location(getWorld(), -3D, 11D, -174.5D), alltime = new Location(getWorld(), 14D, 11D, -174.5D);
                        topholos.add(h.addHologram("topweekly", weekly.clone().add(0, 0.4, 0), "§d§lTop 5 der Woche"));
                        topholos.add(h.addHologram("topall", alltime.clone().add(0, 0.4, 0), "§d§lTop 5 Reichste"));
                        if (topweekly.isEmpty() == false) topplayer = topweekly.get(0).getUUID();
                        for (final MuxCasinoUser cu : topweekly) {
                            final OfflinePlayer op = ms.getServer().getOfflinePlayer(cu.getUUID());
                            if (op == null) continue;
                            final String text = "§f" + op.getName() + " - §d" + ms.getNumberFormat(cu.getWeeklyWins());
                            step++;
                            topholos.add(h.addHologram("topweekly" + step, weekly.clone().add(0, -0.4 * step, 0), text));
                        }
                        step = 0;
                        for (final MuxCasinoUser cu : top5) {
                            final OfflinePlayer op = ms.getServer().getOfflinePlayer(cu.getUUID());
                            if (op == null) continue;
                            final String text = "§f" + op.getName() + " - §d" + ms.getNumberFormat(cu.getChips());
                            step++;
                            topholos.add(h.addHologram("topall" + step, alltime.clone().add(0, -0.4 * step, 0), text));
                        }
                    }
                }.runTask(ms);
            }
        }.runTaskAsynchronously(ms);
    }

    private void setup() {
        final World w = ms.getServer().createWorld(new WorldCreator("MuxCasino").generateStructures(false));
        w.setTime(13050L);
        w.setGameRuleValue("doDaylightCycle", "false");
        w.setGameRuleValue("doMobSpawning", "false");
        w.setGameRuleValue("reducedDebugInfo", "true");
        final WorldBorder wb = w.getWorldBorder();
        wb.setCenter(0D, 0D);
        wb.setSize(400D * 2D);
        this.loc = new Location(w, 22D, 5D, -103.5D, 147F, -27F);
        this.roof = new Location(w, 16.5D, 20D, -164.5D, 144F, 0F);
        this.lift = new Location(w, 13, 10, -155, 0F, 0F);
        this.goldentry = new Location(w, -25.5, 24, -171.5, 0F, 0F);
        this.ultraentry = new Location(w, 36.5, 24, -171.5, 0F, 0F);
        this.musicnotes = new Location(w, 0.5, 8, -167.5, -118F, 0F);
        this.dancefloor = new Location(w, -3.5, 8, -160.5);
        final RegionManager rg = WGBukkit.getRegionManager(w);
        ultraregion = rg.getRegionExact("casinoultra");
        goldregion = rg.getRegionExact("casinogold");
        for (final Entity e : loc.getWorld().getEntities()) {
            if (e instanceof Player || e instanceof ItemFrame || e instanceof Painting) continue;
            e.remove();
        }
        final MuxHolograms h = ms.getHolograms();
        npcloc = new Location(ms.getGameSpawn().getWorld(), 5.5D, 77D, -7.5D);
        h.addHologram("casinoinfo", npcloc.clone(), "§dLade...");
        h.addHologram("casinodate1", new Location(w, 1.5, 4.5, -114.5), "§a§lÖffnungszeiten:");
        h.addHologram("casinodate2", new Location(w, 1.5, 4.0, -114.5), "§fJeden Tag von §a18 bis 23 Uhr");
        h.addHologram("casinodate3", new Location(w, 1.5, 3.5, -114.5), "§fFreitag & Samstag von §a18 bis 2 Uhr");
        h.addHologram("casino", new Location(w, 5.5, 9.75, -151.5), "§a§lMuxCasino");
        h.addHologram("casinolift", lift.clone().subtract(0, 1.25, 0), "§e§l▲ §6§lLift §e§l▲");
        h.addHologram("casinotospawn", new Location(w, -0.5, 8.75, -154), "§b§l▼ §9§lZum Spawn §b§l▼");
        h.addHologram("casinogold", goldentry.clone().subtract(0, 2.25, 0), "§e§lGOLD§a §lonly");
        h.addHologram("casinoultra", ultraentry.clone().subtract(0, 2.25, 0), "§c§lULTRA§a §lonly");
        Set<Location> locs = Sets.newHashSet(
                new Location(w, 34D, 22D, -169.5D, 110F, 0F),
                new Location(w, 34D, 22D, -173.5D, 75F, 0F),
                new Location(w, 2.5D, 9D, -149.5D, -22F, 0F),
                new Location(w, 8.5D, 9D, -149.5D, 15F, 0F));
        final MuxNPCs npcs = ms.getNPCS();
        for (final Location l : locs) {
            npcs.addIronGolem(l, yawToFace(l.getYaw()),
                    "§a§lMuxGuard", p -> ms.showItemBar(p, "§cHallo."));
        }
        npcs.addIronGolem(new Location(w, -4.85D, 23.5D, -153D, 90F, 0F), BlockFace.WEST_NORTH_WEST, "§4§lGiancarlo", p -> {
            ms.sendTitle(p, "§4§lGiancarlo", 10, 40, 10);
            ms.sendSubTitle(p, "§fIch zähle gerade mein Geld.", 10, 40, 10);
        });
        final MuxActions.PlayerAction djaction = p -> {
            if (music.isPlaying()) {
                ms.sendTitle(p, "§d§lDJ", 10, 40, 10);
                ms.sendSubTitle(p, "§fLet's go!", 10, 40, 10);
            } else {
                openMusic(p);
            }
        };
        npcs.addIronGolem(new Location(w, -23D, 22D, -169.5D, -110F, 0F), BlockFace.EAST, "§a§lMuxGuard", this::openBribe);
        npcs.addIronGolem(new Location(w, -23D, 22D, -173.5D, -75F, 0F), BlockFace.EAST, "§a§lMuxGuard", this::openBribe);
        npcs.addIronGolem(new Location(w, 40.5D, 13.5D, -136.5D), BlockFace.NORTH, "§d§lDJ", djaction);
        npcs.addIronGolem(new Location(w, -29.5D, 13.5D, -136.5D), BlockFace.NORTH, "§d§lDJ", djaction);
        npcs.addVillager(4, new Location(w, 11.5D, 8D, -165.5D, 90F, 0F), BlockFace.WEST,
                "§d§lBank", this::openBank);
        spawnBankShelf(new Location(w, 12.5D, 7D, -165.5D));
        npcs.addVillager(4, musicnotes, BlockFace.NORTH_EAST,
                "§d§lMusik", this::openMusic);
        npcs.addVillager(4, new Location(w, 24.5, 4, -101.5, 0F, 0F), BlockFace.NORTH_WEST,
                "§d§lZurück", p -> p.performCommand("spawn"));
        locs = Sets.newHashSet(new Location(w, -29.5, 11, -155.75, -180F, 0F),
                new Location(w, -26.25, 11, -152.5, -90F, 0F),
                new Location(w, -29.5, 11, -149.25, 0F, 0F),
                new Location(w, -32.75, 11, -152.5, 90F, 0F),
                new Location(w, 37.25, 11, -152.5, 90F, 0F),
                new Location(w, 40.5, 11, -155.75, 180F, 0F),
                new Location(w, 43.75, 11, -152.5, -90F, 0F),
                new Location(w, 40.5, 11, -149.25, 0F, 0F),
                new Location(w, 5.5, 21, -184.25, 0F, 0F),
                new Location(w, 50.25, 23, -167.5, 90F, 0F),
                new Location(w, 50.25, 23, -162.5, 90F, 0F),
                new Location(w, -39.25, 23, -167.5, -90F, 0F),
                new Location(w, -39.25, 23, -162.5, -90F, 0F));
        for (final Location l : locs) {
            npcs.addVillager(3, l, yawToFace(l.getYaw()),
                    "§d§lBarkeeper", this::openBarKeeper);
        }
        final ItemStack whiskey = ms.renameItemStack(new ItemStack(Material.POTION, 1, (short) 8259), "§6§lWhiskeybecher");
        final ItemMeta im = whiskey.getItemMeta();
        im.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        whiskey.setItemMeta(im);
        bar.put(Material.POTION, new AbstractMap.SimpleEntry<>(whiskey, 200));
        bar.put(Material.GOLDEN_CARROT, new AbstractMap.SimpleEntry<>(ms.renameItemStack(new ItemStack(Material.GOLDEN_CARROT, 1), "§6§lEspresso"), 500));
        bar.put(Material.WATER_BUCKET, new AbstractMap.SimpleEntry<>(ms.renameItemStack(new ItemStack(Material.WATER_BUCKET, 1), "§9§lFiji Wasser"), 400));
        bar.put(Material.POTATO_ITEM, new AbstractMap.SimpleEntry<>(ms.renameItemStack(new ItemStack(Material.POTATO_ITEM, 1), "§6§lPommes"), 300));
        bar.put(Material.COOKED_BEEF, new AbstractMap.SimpleEntry<>(ms.renameItemStack(new ItemStack(Material.COOKED_BEEF, 1), "§c§lSteak"), 1000));
        sb = ms.getScoreboard().createScoreboard("§7§lMux§5§lCasino");
        sb.setLook((p, board) -> {
            final MuxUser u = ms.getMuxUser(p.getName());
            final MuxCasinoUser cu = players.get(p);
            final UUID uuid = p.getUniqueId();
            final MuxRanks.PermissionsGroup group = ms.getPerms().getGroupOf(uuid);
            final long c = cu.getChips();
            sb.setLine(board, "     ");
            sb.setSection(board, "§3§lEnergie    ", cu.getEnergy() + " ", true);
            sb.setLine(board, "    ");
            sb.setSection(board, ms.isBeta() ? "§d§lTestChips" : "§d§lChips", ms.getNumberFormat(c) + "  ", true);
            sb.setLine(board, "   ");
            sb.setSection(board, ms.isBeta() ? "§e§lTestCoins" : "§e§lMuxCoins", ms.getNumberFormat(u.getCoins()), true);
            sb.setLine(board, "  ");
            sb.setSection(board, ms.getLang("sb.rank"), group == null ? ms.getLang("sb.norank") : group.getName(), true);
        });
        sbchips = ms.getScoreboard().getNewScoreboard();
        final org.bukkit.scoreboard.Objective obj = sbchips.registerNewObjective("Chips", "dummy");
        obj.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.BELOW_NAME);
        obj.setDisplayName("§dChips");
        this.dancers = new Location(w, -1.5, 9, -162.5, -90F, 0F);
        this.colors = Arrays.asList(DyeColor.GREEN, DyeColor.PINK, DyeColor.PURPLE, DyeColor.WHITE, DyeColor.RED, DyeColor.ORANGE, DyeColor.BLUE);
        respawn();
        npcs.removeOld();
    }

    private void freezeEntity(final Entity en) {
        ms.removeSounds(en);
        ms.noAI(en);
        final net.minecraft.server.v1_8_R3.Entity nms = ((CraftEntity) en).getHandle();
        ((EntityInsentient) nms).persistent = true;
        NMSReflection.setObject(net.minecraft.server.v1_8_R3.Entity.class, "invulnerable", nms, true);
    }

    private BlockFace yawToFace(float yaw) {
        return radial[Math.round(yaw / 45f) & 0x7].getOppositeFace();
    }

    private final BlockFace[] radial = {BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST};

    private void chipsEffect(final Material m) {
        final Location l = new Location(loc.getWorld(), 10.0D, 8D, -165.5D, 90F, 0F);
        final List<ArmorStand> chips1 = new ArrayList<>(), chips2 = new ArrayList<>(), chips3 = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            final ArmorStand a = (ArmorStand) l.getWorld().spawnEntity(l.clone().add(0.2, 0.2 + (0.03 * i), 0.2), EntityType.ARMOR_STAND);
            a.setVisible(false);
            a.setGravity(false);
            a.setItemInHand(new ItemStack(m, 1, m == Material.INK_SACK ? (byte) 13 : (byte) 0));
            a.setRightArmPose(new EulerAngle(0.0, 3.0, 0.0));
            if (i > 5) chips1.add(a);
            else chips2.add(a);
        }
        for (int i = 0; i < 4; i++) {
            final ArmorStand a = (ArmorStand) l.getWorld().spawnEntity(l.clone().add(0.4, 0.2 + (0.03 * i), 0.6), EntityType.ARMOR_STAND);
            a.setVisible(false);
            a.setGravity(false);
            a.setItemInHand(new ItemStack(m, 1, (byte) 10));
            a.setRightArmPose(new EulerAngle(0.0, 3.0, 0.0));
            if (i > 2) chips2.add(a);
            else chips3.add(a);
        }
        for (int i = 0; i < 1; i++) {
            final ArmorStand a = (ArmorStand) l.getWorld().spawnEntity(l.clone().add(0.5, 0.2 + (0.03 * i), 0.2), EntityType.ARMOR_STAND);
            a.setVisible(false);
            a.setGravity(false);
            a.setItemInHand(new ItemStack(m, 1, m == Material.INK_SACK ? (byte) 5 : (byte) 0));
            a.setRightArmPose(new EulerAngle(0.0, 3.0, 0.0));
            chips3.add(a);
        }
        l.getWorld().playSound(l, Sound.HORSE_ARMOR, 1F, 1F);
        new BukkitRunnable() {
            @Override
            public void run() {
                chips1.forEach(Entity::remove);
                chips1.clear();
                l.getWorld().playSound(l, Sound.HORSE_ARMOR, 1F, 1F);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        chips2.forEach(Entity::remove);
                        chips2.clear();
                        l.getWorld().playSound(l, Sound.HORSE_ARMOR, 1F, 1F);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                chips3.forEach(Entity::remove);
                                chips3.clear();
                                l.getWorld().playSound(l, Sound.HORSE_ARMOR, 1F, 1F);
                            }
                        }.runTaskLater(ms, 20L);
                    }
                }.runTaskLater(ms, 20L);
            }
        }.runTaskLater(ms, 20L);
    }

    private void spawnBankShelf(final Location l) {
        spawnArmorStand(new Location(l.getWorld(), l.getX() + 0.2, l.getY() + 0.6, l.getZ() - 1.2, 0F, 0F),
                false, new ItemStack(Material.WOOD_STEP), null, new EulerAngle(-0.3490658503988659, 0, 0));
        spawnArmorStand(new Location(l.getWorld(), l.getX() + 0.2, l.getY() + 0.6, l.getZ() - 0.6, 0F, 0F),
                false, new ItemStack(Material.WOOD_STEP), null, new EulerAngle(-0.3490658503988659, 0, 0));
        spawnArmorStand(new Location(l.getWorld(), l.getX() + 0.2, l.getY() + 0.6, l.getZ() - 0, 0F, 0F),
                false, new ItemStack(Material.WOOD_STEP), null, new EulerAngle(-0.3490658503988659, 0, 0));
        spawnArmorStand(new Location(l.getWorld(), l.getX() + 0.2, l.getY() + 1.3, l.getZ() - 1.2, 0F, 0F),
                false, new ItemStack(Material.WOOD_STEP), null, new EulerAngle(-0.3490658503988659, 0, 0));
        spawnArmorStand(new Location(l.getWorld(), l.getX() + 0.2, l.getY() + 1.3, l.getZ() - 0.6, 0F, 0F),
                false, new ItemStack(Material.WOOD_STEP), null, new EulerAngle(-0.3490658503988659, 0, 0));
        spawnArmorStand(new Location(l.getWorld(), l.getX() + 0.2, l.getY() + 1.3, l.getZ() - 0, 0F, 0F),
                false, new ItemStack(Material.WOOD_STEP), null, new EulerAngle(-0.3490658503988659, 0, 0));
        spawnArmorStand(new Location(l.getWorld(), l.getX() + 0.3, l.getY() + 1.5, l.getZ() - 1.1, 70F, 0F),
                true, new ItemStack(Material.CHEST), null, new EulerAngle(-0.3490658503988659, 0, 0));
        spawnArmorStand(new Location(l.getWorld(), l.getX() + 0.2, l.getY() + 2.2, l.getZ() - 0.2, 110F, 0F),
                true, new ItemStack(Material.JUKEBOX), null, new EulerAngle(-0.3490658503988659, 0, 0));
        spawnArmorStand(new Location(l.getWorld(), l.getX() + 0.6, l.getY() + 1.5, l.getZ() - 0.3, 0F, 0F),
                false, null, new ItemStack(Material.DOUBLE_PLANT), new EulerAngle(-0.0490658468970245, -1.1672803927470447E-9, 0));
        spawnArmorStand(new Location(l.getWorld(), l.getX() + 0.6, l.getY() + 1.5, l.getZ() - 0.4, 0F, 0F),
                false, null, new ItemStack(Material.DOUBLE_PLANT), new EulerAngle(-0.14906584806430498, -1.1672803927470447E-9, 0));
        spawnArmorStand(new Location(l.getWorld(), l.getX() + 0.6, l.getY() + 1.5, l.getZ() - 0.2, 70F, 0F),
                false, null, new ItemStack(Material.DOUBLE_PLANT), new EulerAngle(-0.0490658468970245, -1.1672803927470447E-9, -0.5000000058364024));
        spawnArmorStand(new Location(l.getWorld(), l.getX() + 0.1, l.getY() + 2.5, l.getZ() - 0.6, 180F, 0F),
                true, null, new ItemStack(Material.ENDER_PORTAL_FRAME), new EulerAngle(-0.7504915783575618, -0.7243116395776468, 0.3403392041388943));
        spawnArmorStand(new Location(l.getWorld(), l.getX() + 0.1, l.getY() + 2.5, l.getZ() - 0.9, 180F, 0F),
                true, null, new ItemStack(Material.ENDER_PORTAL_FRAME), new EulerAngle(-0.7504915783575618, -0.7243116395776468, 0.3403392041388943));
        spawnArmorStand(new Location(l.getWorld(), l.getX() - 0.2, l.getY() + 1.5, l.getZ() - 1.0, 300F, 0F),
                false, null, new ItemStack(Material.YELLOW_FLOWER), new EulerAngle(-1.850491225012997, -0.7243116395776468, 0.04033916734753923));
        spawnArmorStand(new Location(l.getWorld(), l.getX() - 0.2, l.getY() + 1.5, l.getZ() - 1.3, 310F, 0F),
                false, null, new ItemStack(Material.RED_ROSE, 1, (short) 2), new EulerAngle(-1.7504912571352302, -0.7243116395776468, 0.24033916968210017));
    }

    private void spawnArmorStand(final Location loc, final boolean small, final ItemStack helm, final ItemStack hand, final EulerAngle rightarmpose) {
        final ArmorStand a = (ArmorStand) loc.getWorld().spawnEntity(loc.clone(), EntityType.ARMOR_STAND);
        a.setVisible(false);
        a.setSmall(small);
        a.setCustomNameVisible(false);
        a.setGravity(false);
        a.getEquipment().setHelmet(helm == null ? new ItemStack(Material.AIR) : helm);
        a.getEquipment().setItemInHand(hand == null ? new ItemStack(Material.AIR) : hand);
        a.setRightArmPose(rightarmpose);
    }
    /* TODO Creeper CCTV im Casino (Elias)

     @EventHandler
    public void onEntityDamage(EntityDamageEvent entityDamageEvent) {
        final Entity entity = entityDamageEvent.getEntity();
        if (entity.getType() != EntityType.CREEPER) {
            return;
        }

        final Creeper creeper = (Creeper) entity;
        if (creeper.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            entityDamageEvent.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockClick(final PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
            return;
        }

        final BlockState state = event.getClickedBlock().getState();
        if (!(state instanceof Skull) || !((Skull) state).getOwner().equals("MHF_Cam")) {
            return;
        }

        final Player player = event.getPlayer();
        positions.put(player, player.getLocation());

        final Location loc = event.getClickedBlock().getLocation();
        player.setGameMode(GameMode.SPECTATOR);
        player.setSpectatorTarget(player);

        final Creeper creeper = (Creeper) loc.getWorld().spawnEntity(loc, EntityType.CREEPER);
        creeper.setPowered(false);
        creeper.setCanPickupItems(false);
        creeper.getEquipment().clear();
        creeper.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, true, false));
        creeper.setTarget(null);
        this.noAI(creeper);

        player.setSpectatorTarget(creeper);
        event.setCancelled(true);
    }

    public void noAI(final Entity entity) {
        final net.minecraft.server.v1_8_R3.Entity nmsEntity = ((CraftEntity) entity).getHandle();
        NBTTagCompound tag = nmsEntity.getNBTTag();
        if (tag == null) {
            tag = new NBTTagCompound();
        }
        nmsEntity.c(tag);
        tag.setInt("NoAI", 1);
        nmsEntity.f(tag);
    }

    @EventHandler
    public void onPlayerToggleSneak(final PlayerToggleSneakEvent event) {
        final Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SPECTATOR) {
            return;
        }

        player.setGameMode(GameMode.SURVIVAL);
        for (final Entity entity : player.getWorld().getEntities()) {
            if (!(entity instanceof Creeper)) {
                continue;
            }

            final Creeper creeper = (Creeper) entity;
            if (creeper.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                creeper.remove();
            }
        }

        player.teleport(this.positions.get(player));
        this.positions.remove(player);
    }

     */
}