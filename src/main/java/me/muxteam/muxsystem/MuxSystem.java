package me.muxteam.muxsystem;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.util.EditSessionBuilder;
import com.google.common.collect.EvictingQueue;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.muxteam.base.MuxBase;
import me.muxteam.base.PlayerBase;
import me.muxteam.basic.*;
import me.muxteam.basic.MuxScoreboard.ScoreboardType;
import me.muxteam.casino.MuxCasino;
import me.muxteam.events.Event;
import me.muxteam.events.MuxEvents;
import me.muxteam.events.MuxTournament;
import me.muxteam.extras.MuxBigEnderChest;
import me.muxteam.extras.MuxBoosters;
import me.muxteam.extras.MuxBoosters.BoosterType;
import me.muxteam.extras.MuxChests;
import me.muxteam.extras.MuxEmojis;
import me.muxteam.extras.MuxExtraCommands;
import me.muxteam.extras.MuxExtras;
import me.muxteam.extras.MuxMounts;
import me.muxteam.extras.MuxPerks;
import me.muxteam.extras.MuxPets;
import me.muxteam.holidays.MuxAdvent;
import me.muxteam.holidays.MuxBirthday;
import me.muxteam.holidays.MuxChristmas;
import me.muxteam.holidays.MuxEaster;
import me.muxteam.holidays.MuxHalloween;
import me.muxteam.holidays.MuxHoliday;
import me.muxteam.holidays.MuxSummer;
import me.muxteam.marketing.MuxAffiliate;
import me.muxteam.marketing.MuxEmails;
import me.muxteam.marketing.MuxGiftCodes;
import me.muxteam.marketing.MuxNewbies;
import me.muxteam.marketing.MuxTips;
import me.muxteam.marketing.MuxVotes;
import me.muxteam.muxsystem.MuxHomes.Home;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxteam.MuxAdmin;
import me.muxteam.muxteam.MuxAnalytics;
import me.muxteam.muxteam.MuxAntiCheat;
import me.muxteam.muxteam.MuxBans;
import me.muxteam.muxteam.MuxChatFilter;
import me.muxteam.muxteam.MuxDeveloper;
import me.muxteam.muxteam.MuxFeedback;
import me.muxteam.muxteam.MuxOverview;
import me.muxteam.muxteam.MuxPolls;
import me.muxteam.muxteam.MuxReports;
import me.muxteam.muxteam.MuxSupport;
import me.muxteam.muxteam.MuxTeam;
import me.muxteam.muxteam.MuxUserTransfer;
import me.muxteam.muxteam.MuxVanish;
import me.muxteam.pvp.MuxClanWar;
import me.muxteam.pvp.MuxGames;
import me.muxteam.pvp.MuxPvP;
import me.muxteam.pvp.MuxPvPBots;
import me.muxteam.pvp.MuxPvPStats;
import me.muxteam.pvp.MuxSeason;
import me.muxteam.ranks.MuxCreators;
import me.muxteam.ranks.MuxCustomRank;
import me.muxteam.ranks.MuxGold;
import me.muxteam.ranks.MuxKits;
import me.muxteam.ranks.MuxRanks;
import me.muxteam.ranks.MuxRanks.PermissionsGroup;
import me.muxteam.ranks.MuxRanks.PermissionsUser;
import me.muxteam.ranks.MuxUltra;
import me.muxteam.shared.MuxShared;
import me.muxteam.shared.MuxSharedPackets;
import me.muxteam.shop.MuxGemShop;
import me.muxteam.shop.MuxMarket;
import me.muxteam.shop.MuxMining;
import me.muxteam.shop.MuxShop;
import me.muxteam.shop.MuxTrade;
import me.muxteam.social.MuxChat;
import me.muxteam.social.MuxClans;
import me.muxteam.social.MuxFriends;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import net.minecraft.server.v1_8_R3.*;
import net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle.EnumTitleAction;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.math.IntRange;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.lang3.RandomStringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftItem;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftMetaBook;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton.SkeletonType;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Villager;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

public class MuxSystem extends JavaPlugin implements org.bukkit.command.CommandExecutor, org.bukkit.event.Listener, org.bukkit.plugin.messaging.PluginMessageListener {
    private boolean errordisable, session = true, auth = true, countdown, debug = false, beta = false, shuttingDown = false;
    private byte message;
    private short slots = 300, bungeeusers;
    private int worldlimit, warpworldlimit;
    private long activeMuxCoinsSupply, activeChipsSupply;
    private final IntRange netherportalx = new IntRange(-699, -702), netherportalz = new IntRange(783, 786);
    private String version, joinmsg, servername, worldname, nethername, newstitle, newstext, newslink, newslinktext;
    private UUID serverId, muxIndentifier;
    public String hnonumber = "§cDu musst einen gültigen Betrag eingeben.", hnotonline = "§eDieser Spieler ist derzeit nicht online.",
            hnotfound = "§cDieser Spieler war noch nie auf dem Server.", hnotinfight = "§cDies ist im Kampf nicht gestattet.";
    public final String[] messages = new String[20];
    private final DecimalFormat decformat = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.US)), numformat = new DecimalFormat("#,##0", new DecimalFormatSymbols(Locale.GERMANY));
    private final Random random = new Random();
    private Field headerField, footerField = null;
    private final Set<Short> healpotions = new HashSet<>(Arrays.asList(new Short[]{16389, 16421, 16453, 24613})), dmgpotions = new HashSet<>(Arrays.asList(new Short[]{16428, 16396, 16460, 24620})),
            goodpotions = new HashSet<>(Arrays.asList(new Short[]{16389, 16421, 16453, 16386, 16418, 16450, 16385, 16417, 16449, 16387, 16419, 16451, 16390, 16422, 16454, 16393, 16425, 16457}));
    final Set<Material> fightmaterials = new HashSet<>(Arrays.asList(Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS, Material.BOW, Material.DIAMOND_SWORD, Material.DIAMOND_AXE));
    private final Set<String> tpplayers = new HashSet<>(), godplayers = new HashSet<>(), lightning = new HashSet<>(), globalfly = new HashSet<>(),
            fulltrusted = new HashSet<>(), trusted = new HashSet<>(),
            wetrusted = new HashSet<>(), wgtrusted = new HashSet<>();
    private final Map<String, Integer> tptasks = new HashMap<>();
    private final Set<UUID> tempfix = new HashSet<>(), deactivatedfly = new HashSet<>();
    private final Map<String, Long> tagged = new HashMap<>(), afkplayers = new HashMap<>(), afksince = new HashMap<>();
    private final Map<String, Enchantment> enchantments = new HashMap<>();
    private final ConcurrentHashMap<String, MuxUser> users = new ConcurrentHashMap<>();
    private final Map<String, HashMap<String, Integer>> limits = new HashMap<>();
    private final Map<String, HashMap<String, Long>> cooldowns = new HashMap<>();
    private final Map<String, HashMap<String, String>> tpa = new ConcurrentHashMap<>();
    private final Map<String, Location> activeloc = new HashMap<>(), currentpos = new HashMap<>();
    private final Map<UUID, String> servers = new HashMap<>();
    private final Queue<MuxActions.Action> notImportantSyncActions = new LinkedBlockingDeque<>();
    private final File hashfile = new File(getDataFolder(), "hashmaps.yml");
    private final FileConfiguration hashYML = YamlConfiguration.loadConfiguration(hashfile);
    private final File locfile = new File(getDataFolder(), "locations.yml");
    private final FileConfiguration locations = YamlConfiguration.loadConfiguration(locfile);
    private final ForkJoinPool forkJoinPool = new ForkJoinPool(5,
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,
            (t, e) -> e.printStackTrace(System.out), true); // https://twitter.com/Kademlias/status/1565052388859314178
    private ScoreboardType pvpsb, shopsb;
    private Location gamespawn, netherspawn, endspawn, warpspawn;
    private MySQL db;
    private MuxTimings timings;
    private MuxChairs chairs;
    private MuxFonts fonts;
    private MuxGames games;
    private MuxExtras extras;
    private MuxAdmin admin;
    private MuxOverview overview;
    private MuxTeam team;
    private MuxEvents events;
    private MuxPolls polls;
    private MuxKits kits;
    private MuxPets pets;
    private MuxMounts mounts;
    private MuxShop shop;
    private MuxHomes homes;
    private MuxClans clans;
    private MuxFriends friends;
    private MuxPvPStats stats;
    private MuxProfiles profiles;
    private MuxPayments payments;
    private MuxHolograms holograms;
    private MuxTrade trades;
    private MuxInventory inv;
    private MuxChat chat;
    private MuxSupport support;
    private MuxFeedback feedback;
    private MuxRanks perms;
    private MuxGiftCodes giftcodes;
    private MuxWarps warps;
    private MuxUltra ultra;
    private MuxGold gold;
    private MuxCustomRank customrank;
    private MuxCraft18 mux18;
    private MuxScoreboard scoreboard;
    private MuxAntiLags antilags;
    private MuxAntiBugs antibugs;
    private MuxAntiBot antibot;
    private MuxNewbies newbies;
    private MuxCreators creators;
    private MuxBans bans;
    private MuxVotes votes;
    private MuxBoosters booster;
    private MuxReports reports;
    private MuxChatFilter chatfilter;
    private MuxPvP pvp;
    private MuxPvPBots pvpbots;
    private MuxNPCs npcs;
    private MuxGiantItems giants;
    private MuxChests chests;
    private MuxAntiCheat anticheat;
    private MuxBigEnderChest extraenderchest;
    private MuxExtraCommands extracmds;
    private MuxMarket market;
    private MuxLanguage language;
    private MuxAnalytics analytics;
    private MuxVanish vanish;
    private MuxMenu menu;
    private MuxPerks perks;
    private MuxEmojis emojis;
    private MuxAdvent advent;
    private MuxBossBar bossbar;
    private MuxCasino casino;
    private MuxClanWar clanwar;
    private MuxDeveloper dev;
    private MuxTips tips;
    private MuxPackets packets;
    private MuxUserTransfer usertransfer;
    private MuxHoliday holiday;
    private MuxSnow snow;
    private MuxSeason season;
    private MuxMining mining;
    private MuxBase base;
    private MuxGemShop gemshop;
    private MuxHistory history;
    private MuxAffiliate affiliate;
    private MuxShared shared;
    private MuxEmails email;
    private ProtectedRegion spawnregion, fullspawnregion, shopregion, endregion;
    private SimpleCommandMap cmdmap;
    private World farmworld;
    private static MuxSystem plugin;
    private boolean starting = false;

    @Override
    public void onLoad() {
        starting = true;
        try {
            unregisterSpigotPotionCommand();
        } catch (final Exception ignored) {
        }
    }

    @Override
    public void onEnable() {
        final Server sr = this.getServer();
        // CONFIG
        this.saveDefaultConfig();
        final FileConfiguration config = this.getConfig();
        // SHUTDOWN
        if (config.getBoolean("shutdown")) {
            sr.shutdown();
        }
        if (config.isString("serverId")) {
            this.serverId = UUID.fromString(config.getString("serverId"));
        } else {
            this.serverId = UUID.randomUUID();
            config.set("serverId", this.serverId.toString());
            saveConfig();
        }
        // API
        plugin = this;
        // COMMANDS
        cmdmap = (SimpleCommandMap) NMSReflection.getObject(SimplePluginManager.class, "commandMap", this.getServer().getPluginManager());
        // TIMINGS
        timings = new MuxTimings(this);
        // MYSQL
        db = new MySQL(this,
                config.getString("database.url"), config.getString("database.username"), config.getString("database.password"),
                config.getString("database.shared.url"), config.getString("database.shared.username"), config.getString("database.shared.password"));
        if (db.isConnected() == false || db.isSharedConnected() == false) {
            errorDisable("MySQL Connection failed. Check config.yml");
            return;
        }
        servername = config.getString("servername");
        db.saveCurrentServerName();
        servers.putAll(this.db.getServers());
        muxIndentifier = UUID.fromString("44da0822-dacb-43b3-9358-8c850da56499");
        shared = new MuxShared(this);
        // BETA
        this.beta = config.getBoolean("beta");
        // BANS
        bans = new MuxBans(this);
        // REPORTS
        reports = new MuxReports(this);
        // LANGUAGE
        language = new MuxLanguage(this);
        // SUPPORT
        support = new MuxSupport(this);
        // SCOREBOARD
        scoreboard = new MuxScoreboard(this);
        // PERMISSIONS
        perms = new MuxRanks(this);
        if (perms.load() == false) {
            errorDisable("Permissions failed to load. Check groups.yml");
            return;
        }
        // LOCALE
        Locale.setDefault(Locale.GERMANY); // Für NumberFormats in Java
        // DEV
        dev = new MuxDeveloper(this);
        // FONTS
        fonts = new MuxFonts(this);
        // ANALYTICS
        analytics = new MuxAnalytics(this);
        // MARKET
        market = new MuxMarket(this);
        // EMOJIS
        emojis = new MuxEmojis(this);
        // EXTRAS
        extras = new MuxExtras(this);
        // PERKS
        perks = new MuxPerks(this);
        // CHAT
        chat = new MuxChat(this); // After Perms, Support
        // FILTER
        chatfilter = new MuxChatFilter(this);
        // BOSSBAR
        bossbar = new MuxBossBar(this);
        // CHAIRS
        chairs = new MuxChairs(this);
        // INVENTORY
        inv = new MuxInventory(this);
        // WORLDS
        final World warpw = sr.createWorld(new WorldCreator("warps").generateStructures(false).type(org.bukkit.WorldType.FLAT));
        final World spawnw = sr.createWorld(new WorldCreator("MuxSpawn").generateStructures(false).type(org.bukkit.WorldType.FLAT));
        // WARPS
        warps = new MuxWarps(this);
        // LOCATIONS
        checkLocationsConfig();
        final World w = sr.getWorlds().get(0);
        final World nether = sr.getWorlds().get(1);
        worldname = w.getName();
        nethername = worldname + "_nether";
        updateTotalActiveCoins();
        final World end = sr.getWorld(worldname + "_the_end");
        farmworld = w;
        gamespawn = spawnw.getSpawnLocation().add(0.5D, 0, 0.5D);
        gamespawn.setYaw(90F);
        gamespawn.setPitch(0F);
        netherspawn = warps.getWarpLocation("nether") != null ? warps.getWarpLocation("nether") : nether.getSpawnLocation();
        endspawn = end.getSpawnLocation();
        warpspawn = warpw.getSpawnLocation();
        spawnregion = WGBukkit.getRegionManager(spawnw).getRegionExact("spawn");
        fullspawnregion = WGBukkit.getRegionManager(spawnw).getRegionExact("spawnfull");
        shopregion = WGBukkit.getRegionManager(warpw).getRegionExact("shop");
        endregion = WGBukkit.getRegionManager(end).getRegionExact("end");
        // WORLD CUSTOM SETTINGS
        warpw.setTime(0L);
        warpw.setGameRuleValue("doDaylightCycle", "false");
        warpw.setGameRuleValue("doMobSpawning", "false");
        warpw.setGameRuleValue("reducedDebugInfo", "true");
        spawnw.setTime(0L);
        spawnw.setGameRuleValue("doDaylightCycle", "false");
        spawnw.setGameRuleValue("doMobSpawning", "false");
        spawnw.setGameRuleValue("reducedDebugInfo", "false");
        w.setGameRuleValue("doMobSpawning", "false");
        w.setGameRuleValue("reducedDebugInfo", "false");
        // PAYMENTS
        payments = new MuxPayments(this);
        // MINING
        mining = new MuxMining(this);
        // CHESTS
        chests = new MuxChests(this); // After Extras
        // FEEDBACK
        feedback = new MuxFeedback(this);
        // ADMIN
        admin = new MuxAdmin(this);
        // VOTES
        votes = new MuxVotes(this); // After Chests, Admin
        // SNOW
        snow = new MuxSnow(this); // Before MuxChristmas
        // HOLIDAY
        final int today = Calendar.getInstance(TimeZone.getTimeZone("CET")).get(Calendar.DAY_OF_YEAR);
        if (today > 294 && today < 312) {
            holiday = new MuxHalloween(this, false);
        } else if (today > 334 && today < 362) {
            holiday = new MuxChristmas(this, false);
            snow.activateChristmas();
        } else if (today > 80 && today < 118) {
            holiday = new MuxEaster(this);
        } else if (today > 212 && today < 230) {
            holiday = new MuxSummer(this);
        } else if (today == 65 || today == 66) {
            holiday = new MuxBirthday(this);
        }
        advent = new MuxAdvent(this);
        // GAMES
        games = new MuxGames(this);
        // CREATORS
        creators = new MuxCreators(this);
        // CLANWARS
        clanwar = new MuxClanWar(this);
        // ANTI BUGS
        antibugs = new MuxAntiBugs(this);
        // 1.8 FEATURES
        mux18 = new MuxCraft18(this);
        // HOLOGRAMS
        holograms = new MuxHolograms(this);
        // USERTRANSFER
        usertransfer = new MuxUserTransfer(this);
        // CLANS
        clans = new MuxClans(this); // After Holograms
        // PVP
        pvp = new MuxPvP(this);
        // PVP BOTS
        pvpbots = new MuxPvPBots(this); // After Games
        // SHOP
        shop = new MuxShop(this); // After Warps World
        // KITS
        kits = new MuxKits(this);
        // ULTRA
        ultra = new MuxUltra(this); // After Kits
        // GOLD
        gold = new MuxGold(this);
        customrank = new MuxCustomRank(this);
        // VANISH
        vanish = new MuxVanish(this);
        // PETS
        pets = new MuxPets(this);
        // MOUNTS
        mounts = new MuxMounts(this);
        // EXTRA COMMANDS
        extracmds = new MuxExtraCommands(this);
        // FRIENDS
        friends = new MuxFriends(this);
        // OVERVIEW
        overview = new MuxOverview(this);
        // STATS
        stats = new MuxPvPStats(this);
        // PROFILE
        profiles = new MuxProfiles(this); // After Extras, Perks, Perms
        // GIFTCODES
        giftcodes = new MuxGiftCodes(this);
        // EMAILS
        email = new MuxEmails(this); // After Admin
        // EVENTS
        events = new MuxEvents(this);
        // TRADE
        trades = new MuxTrade(this);
        // ANTI LAGS
        antilags = new MuxAntiLags(this); // After Admin
        // ANTI BOT
        antibot = new MuxAntiBot(this);
        // EXTRA ENDERCHEST
        extraenderchest = new MuxBigEnderChest(this);
        // ANTI CHEAT
        anticheat = new MuxAntiCheat(this);
        // TEAM
        team = new MuxTeam(this);
        // HOMES
        homes = new MuxHomes(this);
        // MENU
        menu = new MuxMenu(this);
        // BASE
        base = new MuxBase(this);
        // BOOSTER
        booster = new MuxBoosters(this); // After Base, Events, Holograms
        // POLLS
        polls = new MuxPolls(this);
        // NEWBIES
        newbies = new MuxNewbies(this);
        // NPCS
        npcs = new MuxNPCs(this);
        // CASINO
        casino = new MuxCasino(this);
        // PACKETS
        packets = new MuxPackets(this);
        // GIANTS
        giants = new MuxGiantItems(this);
        // TIPS
        tips = new MuxTips(this);
        // HISTORY
        history = new MuxHistory(this);
        // GEM SHOP
        gemshop = new MuxGemShop(this); // After Emojis
        // AFFILIATE
        affiliate = new MuxAffiliate(this);
        // SETUP ENCHANTS, GIANTS & NPCS
        setup();
        // SEASON
        season = new MuxSeason(this); // Before Config
        // CONFIG
        loadConfigStuff(config);
        // WORLDBORDER
        final WorldBorder wb = w.getWorldBorder(), wb2 = nether.getWorldBorder(), wb3 = warpw.getWorldBorder(), wb4 = spawnw.getWorldBorder();
        wb.setCenter(0D, 0D);
        wb.setSize(worldlimit * 2D);
        wb2.setCenter(0D, 0D);
        wb2.setSize(worldlimit * 2D);
        wb3.setCenter(0D, 0D);
        wb3.setSize(warpworldlimit * 2D);
        wb4.setCenter(0D, 0D);
        wb4.setSize(400 * 2D);

        // RELOAD
        sr.getOnlinePlayers().forEach(p -> {
            final String name = p.getName();
            final UUID uuid = p.getUniqueId();
            perms.loadPerms(p);
            friends.setFriends(name, db.getFriends(uuid));
            final MuxUser u = db.loadPlayer(uuid);
            if (u == null) {
                System.out.println("MuxSystem> Could not load player.");
                p.kickPlayer(getLang("restart"));
            } else {
                u.setHomes(db.loadHomes(uuid));
                users.put(name, u);
                bans.handleJoin(p, u);
            }
            chests.setChestData(uuid, db.loadChests(uuid));
            extras.setExtraData(uuid, db.loadExtras(uuid));
            if (admin.BOSSBAR.isActive()) bossbar.addPlayer(p);
            scoreboard.handleJoin(p);
            tips.handleJoin(p);
            base.baseJoinCheck(p);
            perks.activatePerks(p);
            activateAdminFly(p);
        });
        // TIMERS
        startTimers();
        // EVENTS
        sr.getPluginManager().registerEvents(this, this);
        // VERSION
        version = this.getDescription().getVersion();
        // FIELDS
        try {
            headerField = PacketPlayOutPlayerListHeaderFooter.class.getDeclaredField("a");
            headerField.setAccessible(true);
            footerField = PacketPlayOutPlayerListHeaderFooter.class.getDeclaredField("b");
            footerField.setAccessible(true);
        } catch (final NoSuchFieldException e) {
            e.printStackTrace();
        }
        // PLUGIN CHANNELS

        sr.getMessenger().registerIncomingPluginChannel(this, "Event", this);
        sr.getMessenger().registerIncomingPluginChannel(this, "BungeeUsers", this);
        sr.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        sr.getMessenger().registerIncomingPluginChannel(this, "WDL|INIT", this);
        sr.getMessenger().registerIncomingPluginChannel(this, "", this); // NOT VELOCITY COMPATIBLE
        sr.getMessenger().registerOutgoingPluginChannel(this, "WDL|CONTROL");
        sr.getMessenger().registerIncomingPluginChannel(this, "LOLIMAHCKER", this);
        sr.getMessenger().registerIncomingPluginChannel(this, "LMC", this);
        sr.getMessenger().registerIncomingPluginChannel(this, "MC|Brand", this);
        sr.getMessenger().registerIncomingPluginChannel(this, "Lunar-Client", this);
        System.out.println("[]===================================[]");
        System.out.println("|          MuxSystem enabled!         |");
        System.out.println("[]===================================[]");
        new BukkitRunnable() {
            @Override
            public void run() {
                starting = false;
            }
        }.runTaskLater(this, debug ? 2L : 20L * 15L);
    }

    @Override
    public void onDisable() {
        this.shuttingDown = true;
        handleNotImportantSyncActions();
        final Server sr = this.getServer();
        if (errordisable) {
            return;
        }
        // RELOAD
        if (isDebug()) broadcastMessage(getLang("reloading"), null, Priority.HIGHEST);
        for (final Player p : sr.getOnlinePlayers()) {
            packets.handleQuit(p);
            team.handleQuit(p, getMuxUser(p.getName()));
            scoreboard.handleQuit(p);
        }
        // ANTILEAVE
        tagged.clear();
        // HOLOGRAMS
        holograms.close();
        // INVENTORY
        inv.close();
        // VOTES
        votes.close();
        // BANS
        bans.close();
        // REPORTS
        reports.close();
        // CHAT
        chat.close();
        // FILTER
        chatfilter.close();
        // SUPPORT
        support.close();
        // CREATORS
        creators.close();
        // SHOP
        shop.close();
        // ULTRA
        ultra.close();
        // GOLD
        gold.close();
        customrank.close();
        // CHAIRS
        chairs.close();
        // HOLIDAY
        if (holiday != null) holiday.close();
        // MARKET
        market.close();
        // BOOSTER
        booster.close();
        // POLLS
        polls.close();
        // KITS
        kits.close();
        // BOSSBAR
        bossbar.close();
        // PETS
        pets.close();
        // MOUNTS
        mounts.close();
        // EMOJIS
        emojis.close();
        // EXTRA COMMANDS
        extracmds.close();
        // HOMES
        homes.close();
        // WARPS
        warps.close();
        // EVENTS
        events.close();
        // TRADE
        trades.close();
        // ANTI LAGS
        antilags.close();
        // 1.8 FEATURES
        mux18.close();
        // CASINO
        casino.close();
        // EXTRA ENDERCHEST
        extraenderchest.close();
        // TEAM
        team.close();
        // ANTICHEAT
        anticheat.close();
        // USERTRANSFER
        usertransfer.close();
        // ADVENT
        advent.close();
        // NEWBIES
        newbies.close();
        // BASE
        base.close();
        // VANISH
        vanish.close();
        // STATS
        stats.close();
        // ANALYTICS
        analytics.close();
        // PERKS
        perks.close();
        // EXTRAS
        extras.close();
        // CHESTS
        chests.close();
        // PAYMENTS
        payments.close();
        // CLANS
        clans.close();
        // FRIENDS
        friends.close();
        // ADMIN
        admin.close();
        // FEEDBACK
        feedback.close(); // After admin (?)
        // OVERVIEW
        overview.close();
        // PERMISSIONS
        perms.close();
        // GUTSCHEINE
        giftcodes.close();
        // ANTIBOT
        antibot.close();
        // HISTORY
        history.close();
        // ESSENTIAL
        users.values().parallelStream().filter(Objects::nonNull).forEach(db::savePlayer);
        // GAMES
        games.close();
        // CLANWARS
        clanwar.close();
        // PVP
        pvp.close();
        // PVP BOTS
        pvpbots.close();
        // NPCS
        npcs.close();
        // GEMSHOP
        gemshop.close();
        // GIANTS
        giants.close();
        // FILE
        saveHashFile();
        // TASKS
        sr.getScheduler().cancelTasks(this);
        forkJoinPool.shutdown();
        // UNREGISTER
        sr.getMessenger().unregisterIncomingPluginChannel(this, "Event", this);
        sr.getMessenger().unregisterIncomingPluginChannel(this, "BungeeUsers", this);
        // TIMINGS
        timings.close();
        // AFFILIATE
        affiliate.close();
        // SHARED
        shared.shutdown();
        // MYSQL
        db.kill();
        System.out.println("[]===================================[]");
        System.out.println("|         MuxSystem disabled!         |");
        System.out.println("[]===================================[]");

        // TP STACKTRACE

        if (stacktraceQueue.isEmpty() == false) {
            File file = new File(getDataFolder(), "teleport-stacktrace.txt");
            try (FileWriter fileWriter = new FileWriter(file)) {
                final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy 'um' HH:mm");
                while (stacktraceQueue.isEmpty() == false) {
                    Entry<Long, Entry<String, StackTraceElement[]>> elements = stacktraceQueue.remove();
                    fileWriter.write(sdf.format(new Date(elements.getKey())));
                    fileWriter.write(" " + elements.getValue().getKey() + "\n");
                    for (StackTraceElement stackTraceElement : elements.getValue().getValue()) {
                        fileWriter.write(String.valueOf(stackTraceElement));
                        fileWriter.write("\n");
                    }
                    fileWriter.write("\n\n");
                }
                fileWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    @Override
    public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
        final String scmd = cmd.getName();
        // ===========================
        // CONSOLE AND PLAYER COMMANDS
        // ===========================
        if (scmd.equalsIgnoreCase("user")) {
            if (sender instanceof Player == false) {
                if (args.length < 2) return true;
                final OfflinePlayer pl = getPlayer(args[0]);
                if (pl == null) return true;
                perms.changePlayerGroup(pl.getUniqueId(), pl.getName(), args[1], sender);
                return true;
            }
            final Player p = (Player) sender;
            if (args.length == 0) {
                showItemBar(p, usage("/profil [" + getLang("player") + "]"));
                return true;
            } else if (hasVoted(p) == false && args[0].toLowerCase().equals(p.getName()) == false) {
                sendNoVoteMessage(p);
                return true;
            }
            profiles.showFullPlayerProfile(p, args[0], "Stats");
            return true;
        } else if (scmd.equalsIgnoreCase("usertransfer")) {
            return usertransfer.handleCommand(sender, args);
        } else if (scmd.equalsIgnoreCase("exp")) {
            if ((sender instanceof Player && isDebug() == false) || sender.isOp() == false) {
                if (sender instanceof Player) sendNoCMDMessage((Player) sender);
                return true;
            } else if (args.length != 3 && args.length != 2) {
                sender.sendMessage(usage("/exp [add | set] [spieler] [anzahl]"));
                return true;
            } else {
                Player victim = null;
                if (args.length == 3) {
                    victim = this.getServer().getPlayer(args[1]);
                } else if (sender instanceof Player) {
                    victim = (Player) sender;
                }
                if (victim == null) {
                    sender.sendMessage(hnotonline);
                    return true;
                }
                int anzahl;
                try {
                    anzahl = Integer.parseInt(args[args.length - 1]);
                    if (anzahl < 0) anzahl = 0;
                } catch (final NumberFormatException e) {
                    sender.sendMessage(hnonumber);
                    return true;
                }
                if (args[0].equalsIgnoreCase("set")) {
                    victim.setExp(0);
                    victim.setLevel(0);
                    victim.setTotalExperience(0);
                }
                final PlayerExpChangeEvent event = new PlayerExpChangeEvent(victim, anzahl);
                this.getServer().getPluginManager().callEvent(event);
                victim.giveExp(event.getAmount());
                if (sender instanceof Player)
                    showItemBar((Player) sender, "§fDer Spieler §6" + victim.getName() + " §fhat jetzt §a" + victim.getTotalExperience() + " EXP§f.");
                else
                    sender.sendMessage("Der Spieler" + victim.getName() + " hat jetzt " + victim.getTotalExperience() + " EXP.");
                return true;
            }
        } else if (scmd.equalsIgnoreCase("coins")) {
            if (isTrusted(sender.getName()) == false) {
                shop.openEarn((Player) sender);
                return true;
            }
            final Player p = sender instanceof Player ? (Player) sender : null;
            if (isDebug() == false && p != null) {
                showItemBar(p, "§cDieser Befehl ist nur im Debug-Modus verfügbar.");
                return true;
            } else if (args.length != 3) {
                if (p != null) showItemBar(p, usage("/coins [add | del | set] [spieler] [anzahl]"));
                else sender.sendMessage(usage("/coins [add | del | set] [spieler] [anzahl | anzahl+gems]"));
                return true;
            }
            int coins;
            if (args[2].endsWith("gems") && sender instanceof Player == false) {
                try {
                    coins = (int) gemshop.getGemExchangeRate(Integer.parseInt(removeNonDigits(args[2])));
                    if (coins < 0) throw new NumberFormatException();
                } catch (final NumberFormatException e) {
                    return true;
                }
            } else {
                try {
                    coins = Integer.parseInt(args[2]);
                    if (coins < 0) throw new NumberFormatException();
                } catch (final NumberFormatException e) {
                    if (p != null) showItemBar(p, "§cBitte gebe einen gültigen Betrag ein.");
                    return true;
                }
            }
            final OfflinePlayer victim = getPlayer(args[1]);
            if (victim == null) {
                if (p != null) showItemBar(p, hnotfound);
                return true;
            }
            final boolean online = victim.isOnline();
            final MuxUser u = online ? getMuxUser(victim.getName()) : db.loadPlayer(victim.getUniqueId(), false);
            if (u == null) {
                if (p != null) showItemBar(p, hnotfound);
                return true;
            }
            if (args[0].equalsIgnoreCase("set")) {
                history.addHistory(victim.getUniqueId(), null, "COINS", String.valueOf(coins), "Command (set)");
                u.setCoins(coins);
            } else if (args[0].equalsIgnoreCase("add")) {
                u.addCoins(coins);
                final boolean console = sender.getName().equals("CONSOLE");
                history.addHistory(victim.getUniqueId(), null, "COINS", "+" + coins, console ? "Console" : "Command");
                if (coins != 5000 || console == false) {
                    if (online) {
                        if (console)
                            victim.getPlayer().sendMessage("  §aDir wurden §e" + getNumberFormat(coins) + " §aMuxCoins gutgeschrieben.");
                        else
                            victim.getPlayer().sendMessage("§6§lCoins>§a Dir wurden §e" + getNumberFormat(coins) + " MuxCoins §agutgeschrieben.");
                    }
                } else {
                    analytics.addVoteExpenses(coins);
                }
            } else if (args[0].equalsIgnoreCase("addall")) {
                this.getServer().dispatchCommand(getServer().getConsoleSender(), "broadcast Dank " + victim.getName() + ", erhältst du " + getNumberFormat(coins) + " MuxCoins.");
                for (final Player pl : getServer().getOnlinePlayers()) {
                    final MuxUser u2 = getMuxUser(pl.getName());
                    u2.addCoins(coins);
                    sendScoreboard(pl);
                    victim.getPlayer().sendMessage("§6§lCoins>§a Dir wurden §e" + getNumberFormat(coins) + " MuxCoins §agutgeschrieben.");
                }
                return true;
            } else if (args[0].equalsIgnoreCase("del")) {
                history.addHistory(victim.getUniqueId(), null, "COINS", "-" + coins, "Command (del)");
                u.addCoins(-coins);
            } else {
                if (p != null) showItemBar(p, usage("/coins [add | del | set] [spieler] [anzahl]"));
                else sender.sendMessage(usage("/coins [add | del | set] [spieler] [anzahl | anzahl+gems]"));
                return true;
            }
            if (online) sendScoreboard(victim.getPlayer());
            if (sender instanceof Player)
                showItemBar((Player) sender, "§fDer Kontostand von §6" + victim.getName() + " §fwurde erfolgreich §averändert§f.");
            else sender.sendMessage("Der Kontostand von " + victim.getName() + " wurde erfolgreich verändert.");
            saveMuxUser(u);
            return true;
        } else if (scmd.equalsIgnoreCase("gems")) {
            if (isTrusted(sender.getName()) == false) {
                ((Player) sender).performCommand("gemshop");
                return true;
            }
            final Player p = sender instanceof Player ? (Player) sender : null;
            if (isDebug() == false && p != null) {
                showItemBar(p, "§cDieser Befehl ist nur im Debug-Modus verfügbar.");
                return true;
            } else if (args.length != 3) {
                if (p != null) showItemBar(p, usage("/gems [add | del | set] [spieler] [anzahl]"));
                else sender.sendMessage(usage("/gems [add | del | set] [spieler] [anzahl]"));
                return true;
            }
            int gems;
            try {
                gems = Integer.parseInt(args[2]);
                if (gems < 0) throw new NumberFormatException();
            } catch (final NumberFormatException e) {
                if (p != null) showItemBar(p, "§cBitte gebe einen gültigen Betrag ein.");
                return true;
            }
            final OfflinePlayer victim = getPlayer(args[1]);
            if (victim == null) {
                if (p != null) showItemBar(p, hnotfound);
                return true;
            }
            final boolean online = victim.isOnline();
            final MuxUser u = online ? getMuxUser(victim.getName()) : db.loadPlayer(victim.getUniqueId());
            if (u == null) {
                if (p != null) showItemBar(p, hnotfound);
                return true;
            }
            if (args[0].equalsIgnoreCase("set")) {
                history.addHistory(victim.getUniqueId(), null, "GEMS", String.valueOf(gems), "Command (set)");
                u.setGems(gems);
            } else if (args[0].equalsIgnoreCase("add")) {
                u.addGems(gems);
                final boolean console = sender.getName().equals("CONSOLE");
                history.addHistory(victim.getUniqueId(), null, "GEMS", "+" + gems, console ? "Console" : "Command");
                if (gems != 10) {
                    if (p == null) getAnalytics().addPurchasedGems(gems);
                    if (online) {
                        if (console)
                            victim.getPlayer().sendMessage("  §aDir wurden §a" + getNumberFormat(gems) + " §aMuxGems gutgeschrieben.");
                        else
                            victim.getPlayer().sendMessage("§a§lGems>§a Dir wurden §a" + getNumberFormat(gems) + " MuxGems §agutgeschrieben.");
                    }
                }
            } else if (args[0].equalsIgnoreCase("del")) {
                history.addHistory(victim.getUniqueId(), null, "GEMS", "-" + gems, "Command (del)");
                u.addGems(-gems);
            } else {
                if (p != null) showItemBar(p, usage("/gems [add | del | set] [spieler] [anzahl]"));
                else sender.sendMessage(usage("/gems [add | del | set] [spieler] [anzahl]"));
                return true;
            }
            if (online) sendScoreboard(victim.getPlayer());
            if (sender instanceof Player)
                showItemBar((Player) sender, "§fDer Kontostand von §6" + victim.getName() + " §fwurde erfolgreich §averändert§f.");
            else sender.sendMessage("Der Kontostand von " + victim.getName() + " wurde erfolgreich verändert.");
            saveMuxUser(u);
            return true;
        } else if (scmd.equalsIgnoreCase("payment")) {
            return payments.handleCommand(sender, label, args);
        } else if (scmd.equalsIgnoreCase("rankupgrade")) {
            return payments.handleRankUpgradeCommand(sender, args);
        } else if (scmd.equalsIgnoreCase("crates")) {
            return chests.handleCommand(sender, args);
        } else if (scmd.equalsIgnoreCase("extras")) {
            return extras.handleExtras(sender, label, args);
        } else if (scmd.equalsIgnoreCase("broadcast")) {
            if (sender.isOp() == false) {
                if (sender instanceof Player) sendNoCMDMessage((Player) sender);
                return true;
            } else if (args.length == 0) {
                if (sender instanceof Player) showItemBar((Player) sender, usage("/broadcast [*][nachricht]"));
                else sender.sendMessage(usage("/broadcast [*][nachricht]"));
                return true;
            }
            if (args[0].equals("demo")) { // Kleiner Joke :)
                this.getServer().getOnlinePlayers().forEach(pl -> ((CraftPlayer) pl).getHandle().playerConnection.sendPacket(new PacketPlayOutGameStateChange(5, 0)));
                return true;
            } else if (args[0].equals("credits")) { // Kleiner Joke :)
                this.getServer().getOnlinePlayers().forEach(pl -> ((CraftPlayer) pl).getHandle().playerConnection.sendPacket(new PacketPlayOutGameStateChange(4, 0)));
                return true;
            }
            String msg = fromArgs(args, 0);
            final boolean donation = msg.contains("{broadcast}");
            msg = msg.replace("{broadcast}", "");
            if (msg.contains("DONTSHOW")) return true;
            broadcastMessage(msg.startsWith("*") ? ChatColor.translateAlternateColorCodes('&', msg).substring(1) : "§c§lMuxCraft>§a " + ChatColor.translateAlternateColorCodes('&', msg), null, donation ? Priority.NORMAL : Priority.HIGH);
            if (sender.getName().equals("CONSOLE") == false) {
                history.addHistory(((Player) sender).getUniqueId(), null, "TEAMACTION", "BROADCAST", msg);
            }
            return true;
        } else if (scmd.equalsIgnoreCase("give")) {
            if (isFullTrusted(sender.getName()) == false) {
                if (sender instanceof Player) sendNoCMDMessage((Player) sender);
                return true;
            } else if (args.length != 2 && args.length != 3) {
                sender.sendMessage(usage("/give [spieler] [item:data] [anzahl]"));
                return true;
            } else {
                final Player victim = this.getServer().getPlayer(args[0]);
                if (victim == null) {
                    sender.sendMessage(hnotonline);
                    return true;
                }
                final ItemStack it = getItemStack(args, 1);
                if (it == null) {
                    sender.sendMessage("§cDas Item existiert nicht.");
                    return true;
                }
                victim.getInventory().addItem(it);
                final String name = it.getType().toString().toLowerCase().replace("_", "");
                victim.sendMessage("§8> §aDu hast " + it.getAmount() + " " + name + " erhalten.");
                sender.sendMessage("§8> §aDu hast " + victim.getName() + " " + it.getAmount() + " " + name + " gegeben.");
                return true;
            }
        } else if (bans.handleBanCommands(scmd, sender, args)) {
            return true;
            // ===========
            // PLAYER ONLY
            // ===========
        } else if (sender instanceof Player == false) {
            sender.sendMessage("Du musst ein Spieler sein um diesen Befehl ausführen zu können!");
            return true;
        }
        final Player p = (Player) sender;
        if (scmd.equalsIgnoreCase("logoutall")) {
            if (p.isOp() == false) {
                sendNoCMDMessage(p);
                return true;
            } else if (args.length == 0) {
                showItemBar(p, usage("/logoutall [grund]"));
                return true;
            }
            this.getServer().setWhitelist(true);
            getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "WHITELIST", "ON + LOGOUTALL");
            logoutAll(p.getName(), fromArgs(args, 0));
            return true;
        } else if (scmd.equalsIgnoreCase("countdown")) {
            if (p.isOp() == false) {
                sendNoCMDMessage(p);
                return true;
            } else if (args.length == 0) {
                showItemBar(p, usage("/countdown [sekunden]"));
                return true;
            } else if (countdown) {
                showItemBar(p, "§cEs läuft bereits ein Countdown.");
                return true;
            }
            final short sec;
            try {
                sec = Short.parseShort(args[0]);
            } catch (final NumberFormatException e) {
                showItemBar(p, hnonumber);
                return true;
            }
            if (sec <= 1) {
                showItemBar(p, "§cDer Countdown muss mindestens 2 Sekunden dauern.");
                return true;
            } else if (sec >= 120) {
                showItemBar(p, "§cDer Countdown geht viel zu lang.");
                return true;
            }
            new BukkitRunnable() {
                short seconds = sec;

                @Override
                public void run() {
                    if (seconds > 0) {
                        if (seconds == 60 || seconds == 45 || seconds == 30 || seconds == 10 || seconds == 5) {
                            broadcastMessage("§a§lCount§c§lDown>§7 Noch " + seconds + " Sekunden.", null, Priority.NORMAL);
                        } else if (seconds <= 3) {
                            broadcastMessage("§a§lCount§c§lDown>§7 Noch " + seconds + (seconds == 1 ? " Sekunde." : " Sekunden."), null, Priority.NORMAL);
                        }
                        seconds--;
                    } else {
                        broadcastMessage("§a§lCount§c§lDown>§7 Die Zeit ist abgelaufen.", null, Priority.NORMAL);
                        try {
                            this.cancel();
                        } finally {
                            countdown = false;
                        }
                    }
                }
            }.runTaskTimer(this, 0L, 20L).getTaskId();
            countdown = true;
            showItemBar(p, "§fDer Countdown wurde §agestartet§f.");
            return true;
        } else if (scmd.equalsIgnoreCase("muxsystem")) {
            final File pluginFile = getFile();
            final Date lastModified = new Date(pluginFile.lastModified());
            final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm");
            dateFormat.setTimeZone(TimeZone.getTimeZone("CET"));
            final String formattedDate = dateFormat.format(lastModified);
            openBook(book("MuxSystem", "§0§lMuxSystem\n\n§0Installierte Version: §lv" + version + "§0 | " + formattedDate + "\n\n§0Programmiert von:" +
                    "\n§8» §0marinus1111\n§8» §0marint94\n§8» §0MrCakeMaster\n§8» §0DevVersion\n§8» §0kingjan1999\n§8» §0MrJohnnytheKing"), p);
            return true;
        } else if (scmd.equalsIgnoreCase("rules")) {
            openBook(linkBook("§lRegeln", "Klicke unten, um die offiziellen Regeln zu lesen.",
                    new Object[]{"https://muxcraft.eu/support/?ign=" + p.getName() + "#rules", "Zu den Regeln >", "Link öffnen"}), p);
            return true;
        } else if (scmd.equalsIgnoreCase("bewerben")) {
            if (args.length > 0 && isTrusted(p.getName())) {
                return team.handlePlayerApplications(p, args);
            }
            openBook(linkBook("§lBewerben", "Klicke unten, um dich auf MuxCraft zu bewerben.",
                    new Object[]{"https://muxcraft.eu/bewerben/?ign=" + p.getName(), "Jetzt bewerben >", "Link öffnen"}), p);
            return true;
        } else if (scmd.equalsIgnoreCase("contact")) {
            openBook(linkBook("§lKontakt", null,
                    new Object[]{"https://muxcraft.eu/", "Zur Website >\n\n", "Link öffnen"},
                    new Object[]{"https://muxcraft.eu/discord", "Zum Discord >\n\n", "Link öffnen"},
                    new Object[]{"https://muxcraft.eu/support/ign?=" + p.getName(), "Zum Support >\n\n", "Link öffnen"},
                    new Object[]{"https://muxcraft.eu/youtube", "Zum Youtube Kanal >\n\n", "Link öffnen"},
                    new Object[]{"", "Zum TeamSpeak >\n\n", "ts.muxcraft.eu"},
                    new Object[]{"https://muxcraft.eu/instagram", "Zum Instagram >", "Link öffnen"}
            ), p);
            return true;
        } else if (scmd.equalsIgnoreCase("buy")) {
            openBook(linkBook("§2§lOnlineshop", "Klicke unten, um zum offiziellen Shop zu gelangen.",
                    new Object[]{"https://shop.muxcraft.eu/?ign=" + p.getName(), "Zum Onlineshop >", "Link öffnen"}), p);
            return true;
        } else if (extracmds.handleExtraCommands(scmd, p, args)) {
            return true;
        } else if (dev.handleCommand(scmd, p, args)) {
            return true;
        }  else if (scmd.equalsIgnoreCase("ping")) {
            return antilags.handlePingCommand(p, args);
        } else if (scmd.equalsIgnoreCase("feedback")) {
            return feedback.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("vote")) {
            return votes.handleCommand(p, null);
        } else if (scmd.equalsIgnoreCase("gemshop")) {
            return gemshop.handleCommand(p);
        } else if (scmd.equalsIgnoreCase("lag")) {
            return antilags.handlePerformanceCommand(p);
        } else if (scmd.equalsIgnoreCase("d")) {
            return inv.openTrash(p);
        } else if (scmd.equalsIgnoreCase("clearchat")) {
            return chat.handleChatClearCommand(p);
        } else if (scmd.equalsIgnoreCase("ignore")) {
            return chat.handleIgnoreCommand(p, args);
        } else if (scmd.equalsIgnoreCase("msg")) {
            return chat.handleMSGCommand(p, args, label);
        } else if (scmd.equalsIgnoreCase("r")) {
            return chat.handleReplyCommand(p, args);
        } else if (scmd.equalsIgnoreCase("silent")) {
            return chat.handleSilentCommand(p, args);
        } else if (scmd.equalsIgnoreCase("h")) {
            return history.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("setwarp")) {
            return warps.handleSetWarp(p, args);
        } else if (scmd.equalsIgnoreCase("warp")) {
            return warps.handleCommand(p, args, label);
        } else if (scmd.equalsIgnoreCase("rtp")) {
            return warps.handleRTPCommand(p);
        } else if (scmd.equalsIgnoreCase("support")) {
            return support.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("chatfilter")) {
            return chatfilter.handleFilterCommand(p, args);
        } else if (scmd.equalsIgnoreCase("report")) {
            return reports.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("gift")) {
            return giftcodes.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("holo")) {
            return holograms.handleCommand(p);
        } else if (scmd.equalsIgnoreCase("event")) {
            return events.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("trade")) {
            return trades.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("market")) {
            return market.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("sell")) {
            return shop.handleSellCommand(p, args);
        } else if (scmd.equalsIgnoreCase("menu")) {
            return menu.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("admin")) {
            return admin.handleCommand(p);
        } else if (scmd.equalsIgnoreCase("o")) {
            return overview.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("kit")) {
            return menu.getBenefits().handleKitCommand(p);
        } else if (scmd.equalsIgnoreCase("team")) {
            return team.handleCommand(p);
        } else if (scmd.equalsIgnoreCase("tips")) {
            return tips.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("build")) {
            return team.handleBuildCommand(p);
        } else if (scmd.equalsIgnoreCase("poll")) {
            return polls.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("ultra")) {
            return ultra.handleCommand(p);
        } else if (scmd.equalsIgnoreCase("gold")) {
            return gold.handleCommand(p);
        } else if (scmd.equalsIgnoreCase("x")) {
            return customrank.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("friends")) {
            return friends.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("home")) {
            return homes.handleHomes(p, args, label);
        } else if (scmd.equalsIgnoreCase("sethome")) {
            return homes.handleSetHome(p, args);
        } else if (scmd.equalsIgnoreCase("vanish")) {
            return vanish.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("rank")) {
            if (isFullTrusted(p.getName())) {
                if (args.length == 0 && label.equalsIgnoreCase("op")) {
                    showItemBar(p, usage("/op [spieler]"));
                    return true;
                } else if (args.length > 0) {
                    final Entry<String, OfflinePlayer> entry = getPlayerAndName(args[0]);
                    final OfflinePlayer pl = entry.getValue();
                    if (pl == null) {
                        showItemBar(p, hnotfound);
                        return true;
                    }
                    overview.openUserGroup(p, pl);
                    return true;
                }
            }
            return inv.openBenefits(p);
        } else if (scmd.equalsIgnoreCase("c")) {
            return clans.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("cw")) {
            return clanwar.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("pvp")) {
            return pvp.handleCommand(p, args, label);
        } else if (scmd.equalsIgnoreCase("games")) {
            return games.handleGames(p, args);
        } else if (scmd.equalsIgnoreCase("casino")) {
            return casino.handleCommand(p);
        } else if (scmd.equalsIgnoreCase("affiliate")) {
            return affiliate.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("email")) {
            return email.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("muxtimings")) {
            return timings.handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("base")) {
            return base.getBaseHandler().handleCommand(p, args);
        } else if (scmd.equalsIgnoreCase("basen")) {
            return base.handleCommand(p, label);
        } else if (scmd.equalsIgnoreCase("shop")) {
            if (args.length == 1 && args[0].equalsIgnoreCase("search")) {
                shop.openSearch(p);
                return true;
            }
            return this.getServer().dispatchCommand(p, "warp shop");
        } else if (scmd.equalsIgnoreCase("creator")) {
            return creators.handleCreator(p);
        } else if (scmd.equalsIgnoreCase("paysafecard")) {
            return payments.handlePaySafeCommand(p, args);
        } else if (scmd.equalsIgnoreCase("btc")) {
            if (checkGeneralCooldown(p.getName(), "CMDBTC", 3000L, true)) {
                return true;
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new URL("https://blockchain.info/ticker").openStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while (true) {
                            if ((line = bufferedReader.readLine()) == null || line.contains("USD")) break;
                        }
                        bufferedReader.readLine();
                        final String lastline = bufferedReader.readLine();
                        if (lastline != null) {
                            final double btc = Double.parseDouble(lastline.split("\"last\": ")[1].split(",")[0]);
                            showItemBar(p, "§fDer Preis von Bitcoin liegt bei §6$" + getNumberFormat(btc) + "§f.");
                        }
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                }
            }.runTaskAsynchronously(this);
            return true;
            /*
             * ESSENTIALS
             */
        } else if (scmd.equalsIgnoreCase("back")) {
            if (p.hasPermission("muxsystem.back") == false) {
                sendNoRankMessage(p, "VIP");
                return true;
            }
            final String pname = p.getName();
            final MuxUser u = getMuxUser(pname);
            final Location l = u.getLastLocation();
            if (l == null || l.getWorld() == null) {
                showItemBar(p, getLang("cmd.nevertped"));
                return true;
            } else if (inBattle(pname, p.getLocation())) {
                showItemBar(p, hnotinfight);
                return true;
            }
            long cooldown = 2000L;
            final ApplicableRegionSet region = WGBukkit.getRegionManager(l.getWorld()).getApplicableRegions(l);
            if (region.size() > 0 && region.allows(DefaultFlag.PVP)) {
                cooldown = 5000L;
            }
            if (checkGeneralCooldown(pname, "BACK", cooldown, true)) {
                showItemBar(p, getLang("notsofast"));
            } else if (p.hasPermission("muxsystem.notpcooldown")) {
                setLastLocation(p);
                teleportPlayer(p, l);
                showItemBar(p, getLang("cmd.backed"));
            } else if (startTPCountdown(p, l)) {
                showItemBar(p, getLang("cmd.backing"));
            }
            return true;
        } else if (scmd.equalsIgnoreCase("enchant")) {
            if (isTrusted(p.getName()) == false) {
                p.performCommand("etable");
                return true;
            } else if (args.length == 0 || args.length > 2) {
                showItemBar(p, usage("/enchant [enchantment] [level]"));
                return true;
            } else {
                final ItemStack item = p.getInventory().getItemInHand();
                if (item == null || item.getType() == Material.AIR) {
                    showItemBar(p, "§cDu hast nichts in der Hand.");
                    return true;
                }
                int level = -1;
                if (args.length == 2) {
                    try {
                        level = Integer.parseInt(args[1]);
                    } catch (final NumberFormatException ignored) {
                    }
                }
                final Enchantment enchantment = getEnchByName(args[0]);
                if (enchantment == null) {
                    showItemBar(p, "§cEnchantment nicht gefunden.");
                    return true;
                }
                final String enchantmentName = enchantment.getName().toLowerCase();
                if (level < 0 || level > enchantment.getMaxLevel() && isFullTrusted(p.getName()) == false || level > 15)
                    level = enchantment.getMaxLevel();
                if (level == 0) {
                    item.removeEnchantment(enchantment);
                    showItemBar(p, "§fDer Enchant §6" + enchantmentName.replace("_", " ") + "§f wurde §cgelöscht§f.");
                } else {
                    item.addUnsafeEnchantment(enchantment, level);
                    showItemBar(p, "§fDer Enchant §6" + enchantmentName.replace("_", " ") + "§f wurde §ahinzugefügt§f.");
                }
                p.getInventory().setItemInHand(item);
                p.updateInventory();
                return true;
            }
        } else if (scmd.equalsIgnoreCase("enderchest")) {
            if (p.hasPermission("muxsystem.enderchest") == false) {
                sendNoRankMessage(p, "EPIC");
                return true;
            } else if (args.length == 0) {
                if (extraenderchest.openExtraEnderChest(p, true)) return true;
                p.openInventory(p.getEnderChest());
                return true;
            }
            final OfflinePlayer victim = getPlayer(args[0]);
            final String name = p.getName();
            if (victim == null) {
                showItemBar(p, hnotfound);
                return true;
            } else if (victim.getName().equals(name)) {
                if (extraenderchest.openExtraEnderChest(p, false)) return true;
                p.openInventory(p.getEnderChest());
                return true;
            }
            p.closeInventory();
            Inventory i;
            if (victim.isOnline()) {
                if (extras.hasExtraEnderChest(victim.getUniqueId())) {
                    final String echest = db.loadEnderChest(victim.getUniqueId());
                    if (echest == null) {
                        showItemBar(p, "§cDer Spieler hat noch keine Enderkiste.");
                        return true;
                    }
                    i = extraenderchest.getInventory(victim, echest);
                    if (isFullTrusted(name)) {
                        final Player pl = victim.getPlayer();
                        if (pl.getOpenInventory() != null && pl.getOpenInventory().getTitle().contains("Erweiterte Enderkiste")) {
                            pl.closeInventory();
                        }
                        final MuxOfflinePlayer of = new MuxOfflinePlayer(victim);
                        inv.setEditInv(p.getName(), of);
                    }
                } else if (isFullTrusted(name)) {
                    p.openInventory(victim.getPlayer().getEnderChest());
                    return true;
                } else {
                    i = this.getServer().createInventory(null, 27, "Endertruhe");
                    i.setContents(victim.getPlayer().getEnderChest().getContents());
                }
            } else {
                final String enderchest = db.loadEnderChest(victim.getUniqueId());
                if (enderchest != null) {
                    i = extraenderchest.getInventory(victim, enderchest);
                    if (isFullTrusted(name)) {
                        final MuxOfflinePlayer of = new MuxOfflinePlayer(victim);
                        inv.setEditInv(p.getName(), of);
                    }
                } else {
                    final MuxOfflinePlayer of = new MuxOfflinePlayer(victim);
                    i = of.getEnderChest();
                    if (isFullTrusted(name)) {
                        inv.setEditInv(p.getName(), of);
                    }
                }
            }
            p.openInventory(i);
            setActiveInv(name, InvType.PLAYERCHEST);
            return true;
        } else if (scmd.equalsIgnoreCase("colors")) {
            if (p.hasPermission("muxsystem.vip") == false) {
                sendNoRankMessage(p, "VIP");
                return true;
            }
            String text = "§0§lFarben im Chat\n\n";
            text += "§aGrün §0- &a\n" +
                    "§bBlau §0- &b\n";
            if (p.hasPermission("muxsystem.creator")) {
                text += "§dPink §0- &d §8(" + (p.isOp() ? "§e§lOP" : "§d§lCR") + "§8)\n";
            }
            text += "§eGelb §0- &e\n" +
                    "§fWeiss §0- &f\n" +
                    "§2Dunkelgrün §0- &2\n" +
                    "§3Hellblau §0- &3\n" +
                    "§6Gold §0- &6\n" +
                    "§7Grau §0- &7\n" +
                    "§8Dunkelgrau §0- &8\n" +
                    "§9Dunkelblau §0- &9\n";
            final boolean istrusted = isTrusted(p.getName());
            String teamtext = "§0§lWeitere Farben\n\n";
            if (p.isOp()) teamtext += "§cRot §0- &c\n";
            teamtext += "§0Schwarz §0- &0\n" +
                    "§1Dunkelblau §0- &1\n";
            if (istrusted) teamtext += "§4Dunkelrot §0- &4\n";
            teamtext += "§5Lila §0- &5\n";
            if (istrusted) teamtext += "§lFett§0 - &l\n";
            if (p.isOp()) {
                teamtext += "§mDurchgestrichen§0 - &m\n" +
                        "§nUnterstrichen§0 - &n\n" +
                        "§oKursiv§0 - &o\n";
            }
            if (p.hasPermission("muxchat.morecolors")) openBook(book("MuxCraft", text, teamtext), p);
            else openBook(book("MuxCraft", text), p);
            return true;
        } else if (scmd.equalsIgnoreCase("feed")) {
            if (p.hasPermission("muxsystem.feed") == false) {
                sendNoRankMessage(p, "ELITE");
                return true;
            } else if (p.getGameMode() == GameMode.SPECTATOR) {
                showItemBar(p, "§cDieser Befehl ist beim Zuschauen nicht möglich.");
                return true;
            }
            feed(p);
            showItemBar(p, getLang("cmd.eat"));
            if (perks.hasNoHunger(p.getName()) == false && random.nextInt(10) == 0) {
                chatClickHoverRun(p, getLang("cmd.eatad"), getLang("cmd.eatadhover"), "/perk");
            }
            return true;
        } else if (scmd.equalsIgnoreCase("fly")) {
            if (p.hasPermission("muxsystem.fly") == false) {
                gemshop.openInventory(p, MuxGemShop.GemShopCategory.ITEMS);
                return true;
            } else if (p.getGameMode() == GameMode.CREATIVE) {
                showItemBar(p, "§cDu hast bereits den Creative-Modus.");
                return true;
            }
            final String name = p.getName();
            final boolean flying = globalfly.remove(name);
            p.setAllowFlight(flying == false);
            p.setFlying(flying == false);
            if (flying) {
                godplayers.remove(name);
                deactivatedfly.add(p.getUniqueId());
                showItemBar(p, "§fFlugmodus wurde §cdeaktiviert§f.");
            } else {
                globalfly.add(name);
                godplayers.add(name);
                showItemBar(p, "§fFlugmodus wurde §aaktiviert§f.");
                deactivatedfly.remove(p.getUniqueId());
            }
            return true;
        } else if (scmd.equalsIgnoreCase("gamemode")) {
            if (isFullTrusted(p.getName()) == false) {
                sendNoCMDMessage(p);
                return true;
            } else if (args.length != 1 && args.length != 2) {
                showItemBar(p, usage("/gamemode [0|1|2|3] [spieler]"));
                return true;
            } else if (args.length == 1) {
                try {
                    switch (Integer.parseInt(args[0])) {
                        case 0:
                            p.updateInventory();
                            p.setGameMode(GameMode.SURVIVAL);
                            activateAdminFly(p);
                            showItemBar(p, "§fDu bist nun im §6Survival-Modus§f.");
                            break;
                        case 1:
                            p.updateInventory();
                            p.setGameMode(GameMode.CREATIVE);
                            showItemBar(p, "§fDu bist nun im §6Creative-Modus§f.");
                            break;
                        case 2:
                            p.updateInventory();
                            p.setGameMode(GameMode.ADVENTURE);
                            showItemBar(p, "§fDu bist nun im §6Adventure-Modus§f.");
                            break;
                        case 3:
                            p.updateInventory();
                            p.setGameMode(GameMode.SPECTATOR);
                            p.setFlying(true);
                            showItemBar(p, "§fDu bist nun im §6Spectator-Modus§f.");
                            break;
                        default:
                            throw new NumberFormatException();
                    }
                } catch (final NumberFormatException e) {
                    showItemBar(p, usage("/gamemode [0|1|2|3] [spieler]"));
                }
                return true;
            } else {
                final Player victim = this.getServer().getPlayer(args[1]);
                if (victim == null) {
                    showItemBar(p, hnotonline);
                    return true;
                }
                try {
                    switch (Integer.parseInt(args[0])) {
                        case 0:
                            victim.setGameMode(GameMode.SURVIVAL);
                            victim.sendMessage("§8> §7Dein Gamemode wurde auf Survival geändert von §e" + p.getName());
                            history.addHistory(victim.getUniqueId(), p.getUniqueId(), "TEAMACTION", "GM-0", null);
                            showItemBar(p, "§fDer Spieler §6" + victim.getName() + " §fist nun im Survival-Modus.");
                            break;
                        case 1:
                            victim.setGameMode(GameMode.CREATIVE);
                            victim.sendMessage("§8> §7Dein Gamemode wurde auf Creative geändert von §e" + p.getName());
                            history.addHistory(victim.getUniqueId(), p.getUniqueId(), "TEAMACTION", "GM-1", null);
                            showItemBar(p, "§fDer Spieler §6" + victim.getName() + " §fist nun im Creative-Modus.");
                            break;
                        case 2:
                            victim.setGameMode(GameMode.ADVENTURE);
                            victim.sendMessage("§8> §7Dein Gamemode wurde auf Adventure geändert von §e" + p.getName());
                            history.addHistory(victim.getUniqueId(), p.getUniqueId(), "TEAMACTION", "GM-2", null);
                            showItemBar(p, "§fDer Spieler §6" + victim.getName() + " §fist nun im Adventure-Modus.");
                            break;
                        case 3:
                            victim.setGameMode(GameMode.SPECTATOR);
                            victim.sendMessage("§8> §7Dein Gamemode wurde auf Spectator geändert von §e" + p.getName());
                            history.addHistory(victim.getUniqueId(), p.getUniqueId(), "TEAMACTION", "GM-3", null);
                            showItemBar(p, "§fDer Spieler §6" + victim.getName() + " §fist nun im Spectator-Modus.");
                            break;
                        default:
                            throw new NumberFormatException();
                    }
                } catch (final NumberFormatException e) {
                    showItemBar(p, usage("/gamemode [0|1|2|3] [spieler]"));
                }
                return true;
            }
        } else if (scmd.equalsIgnoreCase("hat")) {
            if (p.hasPermission("muxsystem.hat") == false) {
                sendNoRankMessage(p, "EPIC");
                return true;
            }
            final PlayerInventory pi = p.getInventory();
            final ItemStack hand = pi.getItemInHand();
            if (hand == null || hand.getType() == Material.AIR) {
                showItemBar(p, getLang("cmd.haterror1"));
                return true;
            } else if (hand.getType().isBlock() == false && hand.getType() != Material.SKULL_ITEM) {
                showItemBar(p, getLang("cmd.haterror2"));
                return true;
            } else if (emojis.getActiveEmoji(p.getUniqueId()) != 0) {
                showItemBar(p, "§cDu kannst diesen Befehl jetzt nicht benutzen.");
                return true;
            } else if (pi.getHelmet() != null) {
                final ItemStack helm = pi.getHelmet();
                pi.setHelmet(hand);
                pi.setItemInHand(helm);
            } else {
                pi.setHelmet(hand);
                pi.setItemInHand(new ItemStack(Material.AIR));
            }
            p.playSound(p.getLocation(), Sound.LAVA_POP, 1F, 2F);
            showItemBar(p, getLang("cmd.hat"));
            return true;
        } else if (scmd.equalsIgnoreCase("heal")) {
            if (p.hasPermission("muxsystem.heal") == false) {
                sendNoRankMessage(p, "ELITE");
                return true;
            } else if (p.getGameMode() == GameMode.SPECTATOR) {
                showItemBar(p, "§cDieser Befehl ist beim Zuschauen nicht möglich.");
                return true;
            }
            final String time = kits.checkKitCooldown(p.getUniqueId(), "Heal", true);
            if (p.hasPermission("muxsystem.heal.forever") == false && time != null) {
                showItemBar(p, String.format(getLang("cmd.healwait"), time));
                return true;
            }
            heal(p);
            showItemBar(p, getLang("cmd.heal"));
            return true;
        } else if (scmd.equalsIgnoreCase("help")) {
            if (p.hasPermission("muxsystem.vip") == false) {
                final ItemStack book = book("§0§l" + getLang("member"),
                        getLang("help.member1"), getLang("help.member2"), getLang("help.member3"), getLang("help.member4"), getLang("help.member5"), getLang("help.member6"), getLang("help.member7"));
                openBook(book, p);
                return true;
            }
            final LinkedHashSet<String> ranks = new LinkedHashSet<>();
            if (p.hasPermission("muxsystem.ultra")) {
                ranks.add(getLang("help.ultra1"));
            }
            if (p.hasPermission("muxsystem.epic")) {
                ranks.add(getLang("help.epic1"));
                ranks.add(getLang("help.epic2"));
                ranks.add(getLang("help.epic3"));
            }
            if (p.hasPermission("muxsystem.elite")) {
                ranks.add(getLang("help.elite1"));
                ranks.add(getLang("help.elite2"));
            }
            if (p.hasPermission("muxsystem.vip")) {
                ranks.add(getLang("help.vip1"));
                ranks.add(getLang("help.vip2"));
            }
            ranks.add(getLang("help.member1"));
            ranks.add(getLang("help.member2"));
            ranks.add(getLang("help.member3"));
            ranks.add(getLang("help.member4"));
            ranks.add(getLang("help.member5"));
            ranks.add(getLang("help.member6"));
            ranks.add(getLang("help.member7"));
            final ItemStack book = book("Help", ranks.toArray(new String[0]));
            openBook(book, p);
            return true;
        } else if (scmd.equalsIgnoreCase("id")) {
            if (p.isOp() == false) {
                sendNoCMDMessage(p);
                return true;
            }
            final ItemStack item = p.getItemInHand();
            p.sendMessage("§7Item: §e" + item.getType().toString() + " §7(ID: §e" + item.getTypeId() + ":" + item.getData().getData() + "§7)");
            return true;
        } else if (scmd.equalsIgnoreCase("invsee")) {
            if (p.hasPermission("muxsystem.invsee") == false) {
                sendNoRankMessage(p, "EPIC");
                return true;
            } else if (args.length != 1) {
                showItemBar(p, usage("/invsee [" + getLang("player") + "]"));
                return true;
            }
            final OfflinePlayer victim = getPlayer(args[0]);
            if (victim == null) {
                showItemBar(p, hnotfound);
                return true;
            } else if (victim.getName().equals(p.getName())) {
                showItemBar(p, getLang("cmd.invseeopenyours"));
                return true;
            } else if (victim.isOnline() && inEvent(victim.getPlayer())) {
                showItemBar(p, "§cDieser Spieler ist gerade im Event.");
                return true;
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    p.closeInventory();
                    if (victim.isOnline() && p.canSee(victim.getPlayer())) {
                        p.openInventory(victim.getPlayer().getInventory());
                    } else {
                        final MuxOfflinePlayer of = new MuxOfflinePlayer(victim);
                        p.openInventory(of.getInventory());
                        if (isFullTrusted(p.getName())) inv.setEditInv(p.getName(), of);
                    }
                    setActiveInv(p.getName(), InvType.PLAYERCHEST);
                }
            }.runTaskAsynchronously(this);
            return true;
        } else if (scmd.equalsIgnoreCase("item")) {
            if (isFullTrusted(p.getName()) == false) {
                sendNoCMDMessage(p);
                return true;
            } else if (args.length == 0 || args.length > 2) {
                showItemBar(p, usage("/i [item]"));
                return true;
            }
            final ItemStack it = getItemStack(args, 0);
            if (it == null) {
                showItemBar(p, "§cDas Item existiert nicht.");
                return true;
            }
            p.getInventory().addItem(it);
            showItemBar(p, "§fDu hast §6" + it.getAmount() + " " + it.getType().toString().toLowerCase().replace("_", "") + " §fsoeben §aerhalten§f.");
            return true;
        } else if (scmd.equalsIgnoreCase("jump")) {
            if (p.hasPermission("muxsystem.jump") == false) {
                sendNoCMDMessage(p);
                return true;
            }
            Block b = null;
            try {
                b = p.getTargetBlock((Set<Material>) null, 300);
            } catch (final Exception ignored) {
            }
            if (b == null || b.getType() == Material.AIR) {
                showItemBar(p, "§cKein Block in Sicht.");
                return true;
            }
            final Location loc = b.getLocation(), ploc = p.getLocation();
            loc.setYaw(ploc.getYaw());
            loc.setPitch(ploc.getPitch());
            loc.setY(loc.getY() + 1);
            forcePlayer(p, loc);
            return true;
        } else if (scmd.equalsIgnoreCase("kill")) {
            if (isFullTrusted(p.getName()) == false) {
                sendNoCMDMessage(p);
                return true;
            } else if (args.length != 1) {
                showItemBar(p, usage("/kill [spieler]"));
                return true;
            }
            final Player victim = this.getServer().getPlayer(args[0]);
            if (victim == null) {
                showItemBar(p, hnotonline);
                return true;
            }
            history.addHistory(victim.getUniqueId(), p.getUniqueId(), "TEAMACTION", "KILL", null);
            final EntityDamageEvent e = new EntityDamageEvent(victim.getPlayer(), p.getName().equals(victim.getName()) ? EntityDamageEvent.DamageCause.SUICIDE : EntityDamageEvent.DamageCause.CUSTOM, Double.MAX_VALUE);
            this.getServer().getPluginManager().callEvent(e);
            if (e.isCancelled() == false) victim.setHealth(0D);
            showItemBar(p, "§fDer Spieler §6" + victim.getName() + " §fwurde §cgetötet§f.");
            showItemBar(victim, "§fDu wurdest von §6" + p.getName() + " §fper Befehl getötet.");
            return true;
        } else if (scmd.equalsIgnoreCase("head")) {
            if (p.hasPermission("muxsystem.head") == false) {
                gemshop.openInventory(p, MuxGemShop.GemShopCategory.ITEMS);
                return true;
            }
            String name = p.getName();
            if (args.length > 0 && isTrusted(p.getName())) {
                name = args[0];
            }
            if (name.length() == 64) {
                p.getInventory().addItem(getHeadFromURL("https://textures.minecraft.net/texture/" + name, "§a§lURL Kopf"));
                p.updateInventory();
                showItemBar(p, "§fDer §6URL Kopf §fwurde dir ins Inventar §ahinzugefügt§f.");
                return true;
            } else if (name.length() > 16) {
                showItemBar(p, "§cDer Name ist zu lang.");
                return true;
            }
            p.getInventory().addItem(renameItemStack(getHead(name), "§aMuxKopf §7» §a" + name));
            showItemBar(p, "§fDer Kopf von §6" + name + " §fwurde dir ins Inventar §ahinzugefügt§f.");
            return true;
        } else if (scmd.equalsIgnoreCase("lightning")) {
            if (p.isOp() == false) {
                sendNoCMDMessage(p);
                return true;
            }
            final String pname = p.getName();
            if (lightning.remove(pname)) {
                showItemBar(p, "§fLightning Modus §cdeaktiviert§f.");
                return true;
            }
            lightning.add(pname);
            showItemBar(p, "§fLightning Modus §aaktiviert§f.");
            return true;
        } else if (scmd.equalsIgnoreCase("more")) {
            if (isFullTrusted(p.getName()) == false) {
                sendNoCMDMessage(p);
                return true;
            }
            final ItemStack item = p.getItemInHand();
            if (item == null || item.getType() == Material.AIR) {
                showItemBar(p, "§cDu musst etwas in der Hand haben.");
                return true;
            }
            final Material m = item.getType();
            if ((m.getMaxDurability() != 0 || m == Material.POTATO_ITEM || m == Material.MOB_SPAWNER) && isFullTrusted(p.getName()) == false) {
                showItemBar(p, "§cNetter Versuch!");
                broadcastMessage("§c§lAntiCheat> §6" + p.getName() + " §chat versucht OP-Items zu stacken mit /more!", "muxsystem.op", Priority.HIGH);
                return true;
            }
            item.setAmount(64);
            return true;
        } else if (scmd.equalsIgnoreCase("ptime")) {
            if (p.hasPermission("muxsystem.ptime") == false) {
                sendNoRankMessage(p, "ELITE");
                return true;
            }
            inv.openPTime(p);
            return true;
        } else if (scmd.equalsIgnoreCase("pweather")) {
            return gold.handleWeatherCommand(p);
        } else if (scmd.equalsIgnoreCase("pt")) {
            return gold.handleMacroCommand(p, args);
        } else if (scmd.equalsIgnoreCase("rename")) {
            if (hasX(p.getUniqueId()) == false) {
                sendNoCMDMessage(p);
                return true;
            } else if (args.length == 0) {
                showItemBar(p, usage("/rename [itemname]"));
                return true;
            }
            final ItemStack i = p.getItemInHand();
            final Material m = (i == null ? null : i.getType());
            if (i == null || m == Material.AIR) {
                showItemBar(p, "§cDu musst etwas in der Hand haben.");
                return true;
            } else if (isTrusted(p.getName()) == false && isArmor(i.getType()) == false && m.toString().contains("SWORD") == false && m.toString().contains("BOW") == false) {
                showItemBar(p, "§cDu musst ein Schwert, Bogen oder Rüstung in der Hand halten.");
                return true;
            }
            final String name = ChatColor.translateAlternateColorCodes('&', fromArgs(args, 0));
            final boolean spawner = i.getType() == Material.MOB_SPAWNER, enderkiste = name.equals(getLang("extraenderchest"));
            if (spawner || enderkiste || i.getType() == Material.SKULL_ITEM && isTrusted(p.getName()) == false) {
                showItemBar(p, "§cDas ist nicht erlaubt.");
                return true;
            } else if (name.length() > 50) {
                showItemBar(p, "§cDer Name ist zu lang!");
                return true;
            }
            renameItemStack(i, name);
            showItemBar(p, "§fDas Item wurde erfolgreich §aumbenannt§f.");
            return true;
        } else if (scmd.equalsIgnoreCase("repair")) {
            if (hasVoted(p) == false && tempfix.contains(p.getUniqueId()) == false) {
                sendNoVoteMessage(p);
                return true;
            }
            final PlayerInventory pi = p.getInventory();
            final boolean hasgold = hasGold(p.getUniqueId());
            final short count = (short) (repairItems(pi.getContents(), hasgold) + (label.equalsIgnoreCase("ifix") == false ? repairItems(pi.getArmorContents(), hasgold) : 0));
            if (count == 0) {
                showItemBar(p, hasgold ? getLang("cmd.fixnoitems") : getLang("cmd.fixnopvpitems"));
                return true;
            }
            p.playSound(p.getLocation(), Sound.ANVIL_USE, 1F, 2F);
            showItemBar(p, getLang("cmd.fixitems"));
            return true;
        } else if (scmd.equalsIgnoreCase("setspawn")) {
            if (isFullTrusted(p.getName()) == false) {
                sendNoCMDMessage(p);
                return true;
            }
            final Location l = gamespawn = p.getLocation();
            l.getWorld().setSpawnLocation(l.getBlockX(), l.getBlockY(), l.getBlockZ());
            p.playSound(p.getLocation(), Sound.PISTON_EXTEND, 1F, 1F);
            history.addHistory(p.getUniqueId(), null, "TEAMACTION", "SETSPAWN", locationToString(l));
            showItemBar(p, "§fDer Spawn wurde §agesetzt§f.");
            return true;
        } else if (scmd.equalsIgnoreCase("spawn")) {
            if (p.hasPermission("muxsystem.notpcooldown")) {
                setLastLocation(p);
                teleportPlayer(p, gamespawn);
                showItemBar(p, getLang("cmd.spawned"));
            } else if (startTPCountdown(p, gamespawn)) {
                showItemBar(p, getLang("cmd.spawning"));
            }
            return true;
        } else if (scmd.equalsIgnoreCase("spawner")) {
            if (p.hasPermission("muxsystem.spawner") == false) {
                sendNoRankMessage(p, "EPIC");
                return true;
            }
            Block b;
            try {
                b = p.getTargetBlock((Set<Material>) null, 10);
            } catch (final IllegalStateException e) {
                showItemBar(p, getLang("cmd.spawnererror1"));
                return true;
            }
            if (b == null || b.getType() != Material.MOB_SPAWNER) {
                showItemBar(p, getLang("cmd.spawnererror2"));
                return true;
            }
            if (perms.hasWorldGuard(p) == false) {
                if (WGBukkit.getRegionManager(p.getWorld()).getApplicableRegions(p.getLocation()).size() > 0
                        || (b.getLocation().getWorld().equals(base.getWorld()) && base.isTrustedAtLocation(p) == false)) {
                    showItemBar(p, getLang("changespawners"));
                    return true;
                }
            }
            inv.openSpawner(p);
            inv.setSpawner(p.getName(), (CreatureSpawner) b.getState());
            return true;
        } else if (scmd.equalsIgnoreCase("speed")) {
            if (p.hasPermission("muxsystem.speed") == false) {
                sendNoCMDMessage(p);
                return true;
            } else if (args.length != 1 && args.length != 2) {
                showItemBar(p, usage("/speed [anzahl]"));
                return true;
            } else {
                Player pl;
                if (args.length == 2) {
                    if (isTrusted(p.getName()) == false) {
                        showItemBar(p, usage("/speed [anzahl]"));
                        return true;
                    } else if ((pl = this.getServer().getPlayer(args[0])) == null) {
                        showItemBar(p, hnotonline);
                        return true;
                    }
                } else {
                    pl = p;
                }
                float speed;
                try {
                    speed = Float.parseFloat(args[args.length - 1]);
                    if (speed > 3F && isTrusted(p.getName()) == false) speed = 3F;
                    else if (speed > 5F) speed = 5F;
                    else if (speed < 0.0001F) speed = 0.0001F;
                } catch (final NumberFormatException e) {
                    showItemBar(p, hnonumber);
                    return true;
                }
                final boolean isFly = pl.isFlying() || p.isOp() == false;
                final float defaultsp = isFly ? 0.1F : 0.2F, realspeed;
                if (speed < 1F) {
                    realspeed = defaultsp * speed;
                } else {
                    final float ratio = (speed - 1F) / 9F * (1F - defaultsp);
                    realspeed = ratio + defaultsp;
                }
                if (isFly) {
                    pl.setFlySpeed(realspeed);
                    if (args.length == 1) showItemBar(p, "§fDeine Fluggeschwindigkeit ist nun §6" + speed + "§f.");
                    else {
                        showItemBar(p, "§6" + pl.getName() + "'s§f Fluggeschwindigkeit ist nun §6" + speed + "§f.");
                        showItemBar(pl, "§fDeine Fluggeschwindigkeit wurde auf §6" + speed + " §fvon §6" + p.getName() + " §fgesetzt.");
                    }
                } else {
                    pl.setWalkSpeed(realspeed);
                    if (args.length == 1) showItemBar(p, "§fDeine Laufgeschwindigkeit ist nun §6" + speed + "§f.");
                    else {
                        showItemBar(p, "§6" + pl.getName() + "'s§f Laufgeschwindigkeit ist nun §6" + speed + "§f.");
                        showItemBar(pl, "§fDeine Laufgeschwindigkeit wurde auf §6" + speed + " §fvon §6" + p.getName() + " §fgesetzt.");
                    }
                }
                return true;
            }
        } else if (scmd.equalsIgnoreCase("stack")) {
            if (p.hasPermission("muxsystem.stack") == false) {
                sendNoRankMessage(p, "VIP");
                return true;
            }
            stackItems(p, null, true);
            return true;
        } else if (scmd.equalsIgnoreCase("sudo")) {
            if (isFullTrusted(p.getName()) == false) {
                sendNoCMDMessage(p);
                return true;
            } else if (args.length < 2) {
                showItemBar(p, usage("/sudo [spieler] [befehl]"));
                return true;
            } else {
                final Player victim = this.getServer().getPlayer(args[0]);
                if (victim == null) {
                    showItemBar(p, hnotonline);
                    return true;
                }
                final String command = fromArgs(args, 1);
                history.addHistory(victim.getUniqueId(), p.getUniqueId(), "TEAMACTION", "SUDO", command);
                if (command.startsWith("/")) {
                    victim.performCommand(command.replace("/", ""));
                    showItemBar(p, "§fBefehl '§e" + command + "§f' wurde als §6" + victim.getName() + " §fausgeführt.");
                } else {
                    victim.chat(command);
                    showItemBar(p, "§fDu hast '§e" + command + "§f' als §6" + victim.getName() + " §fgeschrieben.");
                }
                return true;
            }
        } else if (scmd.equalsIgnoreCase("time")) {
            if (p.isOp() == false) {
                p.performCommand("ptime");
                return true;
            }
            if (args.length != 1) {
                showItemBar(p, usage("/time [tag | nacht | zeitangabe]"));
            } else if (args[0].equalsIgnoreCase("day") || args[0].equalsIgnoreCase("tag")) {
                p.getWorld().setTime(0L);
                sendNormalTitle(p, "§e§lHallo Sonne!", 10, 30, 10);
                getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "TIME", "DAY");
            } else if (args[0].equalsIgnoreCase("night") || args[0].equalsIgnoreCase("nacht")) {
                p.getWorld().setTime(18000L);
                sendNormalTitle(p, "§7§lGute Nacht!", 10, 30, 10);
                getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "TIME", "NIGHT");
            } else if (isNumeric(args[0])) {
                p.getWorld().setTime(Integer.parseInt(args[0]));
                sendNormalTitle(p, "§a§lZeit verändert!", 10, 30, 10);
                getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "TIME", args[0]);
            } else {
                showItemBar(p, usage("/time [tag | nacht | zeitangabe]"));
            }
            return true;
        } else if (scmd.equalsIgnoreCase("top")) {
            if (p.hasPermission("muxsystem.top") == false) {
                sendNoRankMessage(p, "EPIC");
                return true;
            } else if (p.hasPermission("muxsystem.top.everywhere") == false && base.isTrustedAtLocation(p) == false) {
                showItemBar(p, "§cDu kannst das nur in einer Base ausführen.");
                return true;
            } else if (p.getGameMode() == GameMode.SPECTATOR) {
                showItemBar(p, "§cDu kannst während dem Zuschauen diesen Befehl nicht ausführen.");
                return true;
            }
            final Location l = p.getLocation();
            l.setY(p.getWorld().getHighestBlockYAt(l.getBlockX(), l.getBlockZ()) + 1D);
            if (p.hasPermission("muxsystem.notpcooldown")) {
                setLastLocation(p);
                teleportPlayer(p, l);
                showItemBar(p, "§fDu wurdest §6nach oben §fteleportiert.");
            } else if (startTPCountdown(p, l)) {
                showItemBar(p, "§fTeleportiere §6nach oben§f...");
            }
            return true;
        } else if (scmd.equalsIgnoreCase("tp")) {
            if (p.hasPermission("muxsystem.tp") == false) {
                p.performCommand("tpa " + String.join(" ", args));
                return true;
            } else if (args.length != 1 && args.length != 2) {
                if (isTrusted(p.getName())) showItemBar(p, usage("/tp [spieler] [spieler2]"));
                else showItemBar(p, usage("/tp [spieler]"));
                return true;
            } else if (args.length == 1) {
                final Player victim = this.getServer().getPlayer(args[0]);
                if (victim == null || p.canSee(victim) == false) {
                    showItemBar(p, hnotonline);
                    return true;
                } else if (p.isOp() == false) {
                    final String wname = victim.getWorld().getName();
                    if (wname.equals("warps") == false && (wname.equals(nethername) == false || victim.getLocation().distance(netherspawn) > 500D) && (wname.equals(gamespawn.getWorld().getName()) == false)) {
                        showItemBar(p, "§cDer Spieler ist in seiner Base oder unterwegs.");
                        return true;
                    }
                }
                setLastLocation(p);
                forcePlayer(p, victim);
                showItemBar(p, "§fDu hast dich zu §6" + victim.getName() + " §fteleportiert.");
                getHistory().addHistory(p.getUniqueId(), victim.getUniqueId(), "TELEPORT", "/tp", blockLocationToStringNoYawPitch(victim.getLocation()));
                return true;
            } else if (isTrusted(p.getName())) {
                final Player victim = this.getServer().getPlayer(args[0]), victim2 = this.getServer().getPlayer(args[1]);
                if (victim == null || victim2 == null) {
                    showItemBar(p, "§eEiner der Spieler ist nicht online.");
                    return true;
                } else if (inGame(victim) || inGame(victim2)) {
                    showItemBar(p, "§cDieser Spieler spielt gerade ein Minigame.");
                    return true;
                } else if (inCasino(victim) || inCasino(victim2)) {
                    showItemBar(p, "§cDieser Spieler ist gerade im Casino.");
                    return true;
                } else if (inWar(victim) || inWar(victim2)) {
                    showItemBar(p, "§cDieser Spieler ist gerade im Clanwar.");
                    return true;
                } else if (inEvent(victim) || inEvent(victim2)) {
                    showItemBar(p, "§cDieser Spieler ist gerade im Event.");
                    return true;
                } else if (inDuel(victim) || inDuel(victim2)) {
                    showItemBar(p, "§cDieser Spieler ist gerade in einem Duell.");
                    return true;
                } else if (in1vs1(victim) || in1vs1(victim2)) {
                    showItemBar(p, "§cDieser Spieler ist gerade in einem 1vs1.");
                    return true;
                }
                setLastLocation(victim);
                forcePlayer(victim, victim2);
                if (victim2.getName().equals(p.getName())) {
                    showItemBar(victim, "§fDer Spieler §6" + p.getName() + " §fhat dich zu ihm teleportiert.");
                    showItemBar(p, "§fDer Spieler §6" + victim.getName() + " §fwurde zu dir teleportiert.");
                } else {
                    victim2.sendMessage("§e" + p.getName() + " §7hat §e" + victim.getName() + " §7zu dir teleportiert.");
                    victim.sendMessage("§e" + p.getName() + " §7hat dich zu §e" + victim2.getName() + " §7teleportiert.");
                    showItemBar(p, "§fDu hast §6" + victim.getName() + " §fzu §6" + victim2.getName() + " §fteleportiert.");
                }
                history.addHistory(p.getUniqueId(), victim.getUniqueId(), "TELEPORT", "/tp " + victim.getName() + " " + victim2.getName(), blockLocationToStringNoYawPitch(victim.getLocation()));
                history.addHistory(p.getUniqueId(), null, "TEAMACTION", "/TP " + victim.getName() + " " + victim2.getName(), blockLocationToStringNoYawPitch(victim.getLocation()));
                return true;
            }
        } else if (scmd.equalsIgnoreCase("tpa")) {
            if (args.length != 1) {
                showItemBar(p, usage("/tpa [" + getLang("player") + "]"));
                return true;
            }
            if (db.getClanFromPlayer(p.getUniqueId()) == null) {
                chatClickHoverRun(p, "§3§lMuxClans>§7 Freunde? Spielt gemeinsam in einem Clan.", "§3§oKlicke um gemeinsam zu spielen.", "/c");
                chatClickHoverRun(p, "§3§lMuxClans>§7 Klicke §3hier§7, um in einem Clan zu spielen.", "§3§oKlicke um gemeinsam zu spielen.", "/c");
            }
            final Player victim = this.getServer().getPlayer(args[0]);
            if (tpa(p, victim, "TPA")) return true;
            chatClickHoverRun(victim, String.format(getLang("cmd.tpareceived"), p.getName()), getLang("cmd.tpaccepthover"), "/tpaccept");
            chatClickHoverRun(victim, getLang("cmd.tpacceptclick"), getLang("cmd.tpaccepthover"), "/tpaccept");
            return true;
        } else if (scmd.equalsIgnoreCase("tpacancel")) {
            final String name = p.getName();
            final List<String> removed = new ArrayList<>();
            final boolean success = tpa.entrySet().removeIf(entry -> (entry.getValue().containsValue(name) && tpa.remove(entry.getKey()) != null && removed.add(entry.getKey())));
            for (final String s : removed) {
                this.getServer().getPlayer(s).sendMessage(String.format(getLang("cmd.cancelledtpa"), name));
            }
            if (success == false) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return true;
            }
            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
            showItemBar(p, getLang("cmd.tpacancelled"));
            return true;
        } else if (scmd.equalsIgnoreCase("tpaccept")) {
            final String name = p.getName();
            final Map<String, String> map = tpa.get(name);
            if (map == null) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return true;
            } else if (hasVoted(p) == false) {
                sendNoVoteMessage(p);
                return true;
            }
            final boolean b = map.containsKey("TPA");
            final Player victim = this.getServer().getPlayer(map.get(b ? "TPA" : "TPAHERE"));
            if (victim == null) {
                showItemBar(p, hnotonline);
                return true;
            } else if (inGame(victim)) {
                showItemBar(p, "§cDieser Spieler spielt gerade ein Minigame.");
                return true;
            } else if (inCasino(victim)) {
                showItemBar(p, "§cDieser Spieler ist gerade im Casino.");
                return true;
            } else if (inCasino(p)) {
                showItemBar(p, "§cDu bist gerade im Casino.");
                return true;
            } else if (inWar(victim)) {
                showItemBar(p, "§cDieser Spieler ist gerade in Clanwar.");
                return true;
            } else if (inEvent(victim)) {
                showItemBar(p, "§cDieser Spieler ist gerade in einem Event.");
                return true;
            } else if (inDuel(victim)) {
                showItemBar(p, "§cDieser Spieler ist gerade in einem Duell.");
                return true;
            } else if (in1vs1(victim)) {
                showItemBar(p, "§cDieser Spieler ist gerade in einem 1vs1.");
                return true;
            } else if (p.getGameMode() == GameMode.SPECTATOR) {
                showItemBar(p, "§cDu bist im Zuschauermodus.");
                return true;
            } else if (victim.getGameMode() == GameMode.SPECTATOR) {
                showItemBar(p, "§cDer Spieler ist derzeit im Zuschauermodus.");
                return true;
            } else if (b && inBattle(victim.getName(), victim.getLocation())) {
                showItemBar(p, getLang("playerinfight"));
                return true;
            } else if (b && (args.length == 0 || args[0].equals(victim.getName()) == false)) {
                final MuxUser u = getMuxUser(p.getName());
                final Map<String, MuxHomes.Home> hs = u.getHomes();
                final Location l = p.getLocation();
                boolean nearhome = false;
                if (hs != null) {
                    for (final Entry<String, Home> entry : hs.entrySet()) {
                        final Location homel = entry.getValue().getLocation();
                        if (homel != null && homel.getWorld().equals(l.getWorld()) && homel.distance(l) < 100D) {
                            nearhome = true;
                            break;
                        }
                    }
                }
                if (nearhome && u.getSettings().getAlertFriends().contains(victim.getUniqueId()) == false) {
                    new ConfirmInventory(this, pl -> {
                        pl.performCommand("tpaccept " + victim.getName());
                        pl.closeInventory();
                    }, HumanEntity::closeInventory).show(p,
                            "§0§lTeleport bestätigen", "§aBestätigen", "§cAbbrechen", new ItemStack(Material.COMPASS),
                            "§f§l" + victim.getName(), "§7möchte sich zu dir teleportieren.");
                    return true;
                }
            }
            tpa.remove(name);
            final boolean notpcooldown = b ? victim.hasPermission("muxsystem.notpcooldown") : p.hasPermission("muxsystem.notpcooldown");
            if (notpcooldown) {
                setLastLocation(b ? p : victim);
                teleportPlayer(b ? victim : p, b ? p.getLocation() : victim.getLocation());
                history.addHistory(b ? victim.getUniqueId() : p.getUniqueId(), b ? p.getUniqueId() : victim.getUniqueId(), "TPA", (b ? "TPA" : "TPAHERE"), blockLocationToStringNoYawPitch(b ? p.getLocation() : victim.getLocation()));
            } else if (startTPCountdown(b ? victim : p, b ? p.getLocation() : victim.getLocation()) == false) {
                return true;
            }
            history.addTeleportCache((b ? victim : p), b ? victim.getUniqueId() : p.getUniqueId(), b ? p.getUniqueId() : victim.getUniqueId(), "TPA", (b ? "TPA" : "TPAHERE"), blockLocationToStringNoYawPitch(b ? p.getLocation() : victim.getLocation()));
            if (b) p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 2F);
            else victim.playSound(victim.getLocation(), Sound.LEVEL_UP, 1F, 2F);
            victim.sendMessage(String.format(getLang("cmd.acceptedtpa"), name) + " " + (b ? getLang("cmd.tpyoutohim") : getLang("cmd.tphimtoyou")));
            showItemBar(p, String.format(getLang("cmd.tpaaccepted"), victim.getName()));
            return true;
        } else if (scmd.equalsIgnoreCase("tpahere")) {
            if (p.hasPermission("muxsystem.tpahere") == false) {
                sendNoRankMessage(p, "VIP");
                if (db.getClanFromPlayer(p.getUniqueId()) == null) {
                    chatClickHoverRun(p, "§3§lMuxClans>§7 Freunde? Spielt gemeinsam in einem Clan.", "§3§oKlicke um gemeinsam zu spielen.", "/c");
                    chatClickHoverRun(p, "§3§lMuxClans>§7 Klicke §3hier§7, um in einem Clan zu spielen.", "§3§oKlicke um gemeinsam zu spielen.", "/c");
                }
                return true;
            } else if (args.length != 1) {
                showItemBar(p, usage("/tpahere [" + getLang("player") + "]"));
                return true;
            }
            final Player victim = this.getServer().getPlayer(args[0]);
            if (tpa(p, victim, "TPAHERE")) return true;
            p.playSound(victim.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            chatClickHoverRun(victim, String.format(getLang("cmd.tpaherereceived"), p.getName()), getLang("cmd.tpaccepthover"), "/tpaccept");
            chatClickHoverRun(victim, getLang("cmd.tpacceptclick"), getLang("cmd.tpaccepthover"), "/tpaccept");
            return true;
        } else if (scmd.equalsIgnoreCase("tphere")) {
            if (p.hasPermission("muxsystem.tphere") == false) {
                p.performCommand("tpahere " + String.join(" ", args));
                return true;
            } else if (args.length == 0) {
                showItemBar(p, usage("/tphere [spieler]"));
                return true;
            } else {
                final Player victim = this.getServer().getPlayer(args[0]);
                if (victim == null || p.canSee(victim) == false) {
                    showItemBar(p, hnotonline);
                    return true;
                } else if (inGame(victim)) {
                    showItemBar(p, "§cDieser Spieler spielt gerade ein Minigame.");
                    return true;
                } else if (inCasino(victim)) {
                    showItemBar(p, "§cDieser Spieler ist gerade im Casino.");
                    return true;
                } else if (inWar(victim)) {
                    showItemBar(p, "§cDieser Spieler ist gerade in Clanwar.");
                    return true;
                } else if (inDuel(victim)) {
                    showItemBar(p, "§cDieser Spieler ist gerade in einem Duell.");
                    return true;
                } else if (in1vs1(victim)) {
                    showItemBar(p, "§cDiesser Spieler ist gerade in einem 1vs1.");
                    return true;
                } else if (inEvent(victim)) {
                    showItemBar(p, "§cDieser Spieler ist gerade in einem Event.");
                    return true;
                } else if (inBattle(victim.getName(), victim.getLocation())) {
                    showItemBar(p, getLang("playerinfight"));
                    return true;
                }
                setLastLocation(victim);
                forcePlayer(victim, p);
                showItemBar(p, "§fDer Spieler §6" + victim.getName() + " §fwurde zu dir teleportiert.");
                if (args.length == 2 && args[1].equals("-s")) return true;
                showItemBar(victim, "§fDer Spieler §6" + p.getName() + " §fhat dich zu ihm teleportiert.");
                getHistory().addHistory(victim.getUniqueId(), p.getUniqueId(), "TELEPORT", "/tphere", blockLocationToStringNoYawPitch(p.getLocation()));
                return true;
            }
        } else if (scmd.equalsIgnoreCase("tppos")) {
            if (isTrusted(p.getName()) == false) {
                p.performCommand("rtp");
                return true;
            } else if (args.length < 3) {
                showItemBar(p, usage("/tppos [x] [y] [z]"));
                return true;
            } else {
                double x, y, z;
                try {
                    x = Double.parseDouble(args[0]);
                    y = Double.parseDouble(args[1]);
                    z = Double.parseDouble(args[2]);
                } catch (final NumberFormatException e) {
                    showItemBar(p, hnonumber);
                    return true;
                }
                final World w = p.getWorld();
                final String wname = w.getName();
                final int limit = w.equals(getGameSpawn().getWorld()) || wname.equals("MuxCasino") ? 400 : w.equals(getWarpsWorld()) || w.getEnvironment() == Environment.THE_END ? warpworldlimit : worldlimit;
                if (x > limit || z > limit || y > limit || y < -limit || x < -limit || z < -limit) {
                    showItemBar(p, "§cDie Koordinaten sind zu weit.");
                    return true;
                }
                setLastLocation(p);
                teleportPlayer(p, new Location(w, x, y, z));
                history.addHistory(p.getUniqueId(), null, "TEAMACTION", "TPPOS", wname + " " + (int) x + " " + (int) y + " " + (int) z);
                history.addHistory(p.getUniqueId(), null, "TELEPORT", "/tppos", wname + " " + (int) x + " " + (int) y + " " + (int) z);
                return true;
            }
        } else if (scmd.equalsIgnoreCase("workbench")) {
            if (p.hasPermission("muxsystem.workbench") == false) {
                sendNoRankMessage(p, "EPIC");
                return true;
            }
            p.openWorkbench(null, true);
            return true;
        }
        return false;
    }

    private void unregisterSpigotPotionCommand() {
        renameCommandFromCommandMap("potion", "hcpotion", Arrays.asList("hcpots", "hcpotions"));
    }

    private void renameCommandFromCommandMap(final String oldCommand, final String newCommand, final List<String> newAliases) {
        final SimpleCommandMap commandMap = net.minecraft.server.v1_8_R3.MinecraftServer.getServer().server.getCommandMap();
        final Command command = commandMap.getCommand(oldCommand);
        if (command == null) return;
        final Map<String, Command> knownCommands = (Map<String, Command>) NMSReflection.getObject(SimpleCommandMap.class, "knownCommands", commandMap);
        if (knownCommands == null) return; // <- never null tho
        command.unregister(commandMap);
        if (newAliases != null) {
            command.getAliases().forEach(knownCommands::remove);
            command.setAliases(newAliases);
        }
        knownCommands.remove(oldCommand);
        commandMap.register(newCommand, "Spigot", command);
    }

    @EventHandler
    public void onPreLogin(final AsyncPlayerPreLoginEvent e) {
        if (this.getServer().getOnlinePlayers().parallelStream().anyMatch(p -> p.getUniqueId().equals(e.getUniqueId()))) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, getLang("alreadyonline"));
            return;
        }
        final String ip = e.getAddress().getHostAddress();
        if (admin.ANTIVPN.isActive() && hasVip(e.getUniqueId()) == false && isVPN(ip) > 0) {
            e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "§c§lVPNs sind nicht erlaubt.\n §b§lVIPs §6können auch mit VPNs den Server beitreten! §a§lshop.muxcraft.eu");
        }
        final MuxUser u = db.loadPlayer(e.getUniqueId());
        if (u != null) {
            if (u.isPermban()) {
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "§cDu bist permanent von " + servername + " gesperrt. Grund: §7" + db.getBanReason(e.getUniqueId()));
                return;
            } else if (u.isTimebanned()) {
                e.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "§cDu bist noch für §6" + timeToString(
                        u.getUnbanTime() - System.currentTimeMillis(), false) + " §csuspendiert. §cGrund: §6" + db.getBanReason(e.getUniqueId()));
                return;
            }
            users.put(e.getName(), u);
        }
    }

    @EventHandler
    public void onLogin(final PlayerLoginEvent e) {
        final Player p = e.getPlayer();
        if (e.getResult() == PlayerLoginEvent.Result.KICK_WHITELIST) {
            e.setKickMessage(getLang("maintenance"));
            users.remove(p.getName());
            return;
        }
        if (this.starting) {
            e.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            e.setKickMessage("§cDer Server startet gerade, bitte warte etwas.");
            users.remove(p.getName());
            return;
        }
        if (this.beta) {
            final PermissionsUser pu = perms.getUserData(p.getUniqueId());
            final String rang = pu == null ? null : pu.getGroup();
            final String teamrank = pu == null ? null : pu.getTeamGroup();
            if (rang == null || ((perms.getGroup(rang).getPermissions().contains("muxsystem.ultra") == false) && !(teamrank != null && perms.getGroup(teamrank).getPermissions().contains("muxsystem.team")))) {
                if (p.isWhitelisted() == false) {
                    e.setResult(PlayerLoginEvent.Result.KICK_WHITELIST);
                    e.setKickMessage("§cIn der Beta können nur Spieler mit Ultra oder höher spielen.");
                    users.remove(p.getName());
                    return;
                }
            }
        }
        if (this.getServer().getOnlinePlayers().size() >= slots) {
            final PermissionsUser pu = perms.getUserData(p.getUniqueId());
            final String rang = pu == null ? null : pu.getGroup();
            if (rang != null && rang.equals("Default") == false) {
                e.setResult(PlayerLoginEvent.Result.ALLOWED);
                inv.isBeingEdited(e.getPlayer(), true);
            } else {
                e.setResult(PlayerLoginEvent.Result.KICK_FULL);
                e.setKickMessage(getLang("serverfull"));
                users.remove(p.getName());
            }
            return;
        }
        final String ip = e.getAddress().getHostAddress();
        final int accs = howManyAccounts(p, ip);
        final String pname = p.getName();
        if (admin.ANTIBOT.isActive() && antibot.isBot(p, accs)) {
            e.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            e.setKickMessage("§cZweitaccounts können derzeit nicht beitreten.");
            users.remove(p.getName());
            return;
        } else if (pname.startsWith("491") || pname.startsWith("0491") ||
                pname.startsWith("O15") || pname.startsWith("015") || pname.startsWith("O17") || pname.startsWith("017")) {
            e.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            e.setKickMessage("§cDein Minecraft Name ist nicht erlaubt.");
            users.remove(p.getName());
            return;
        } else if (accs > 15) {
            e.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            e.setKickMessage("§cDu kannst nur maximal mit 15 Accounts online sein.");
            users.remove(p.getName());
            return;
        }
        inv.isBeingEdited(e.getPlayer(), true);
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent e) {
        final Player p = e.getPlayer();
        final UUID uuid = p.getUniqueId();
        final String pname = p.getName(), ip = p.getAddress().getAddress().getHostAddress();
        e.setJoinMessage(null);
        showTabList(p, "\n§7§lMux§a§lCraft§r\n", "\n§7 Besuche die §aoffizielle Webseite §7für unseren Shop, Discord und vieles mehr: \n§awww.muxcraft.eu\n");
        MuxUser u = getMuxUser(pname);
        final PermissionsGroup group = perms.loadPerms(p);
        joinLater(p, group, u);
        final boolean brandnew = u == null;
        cacheduuids.put(pname.toLowerCase(), uuid);
        cachednames.put(pname.toLowerCase(), pname);
        if (u == null) {
            if (p.hasPlayedBefore()) {
                p.kickPlayer(getLang("loginerror"));
                return;
            }
            u = new MuxUser(uuid, pname, System.currentTimeMillis(), p.getFirstPlayed(), 0, gamespawn, beta ? 10000 : 500, 0.00D,
                    250, 0, 0, 0, false, -1, false, -1, -1, EvictingQueue.create(50), false, new Settings(), 0L);
            u.setGems(beta ? 500 : 0);
            u.setGlobalOnline(0);
            u.setIp(p.getAddress().getAddress().getHostAddress());
            u.setGlobalUser(true);
            u.setGlobalFirstLogin(System.currentTimeMillis());
            u.setGlobalLoginTimestamp(System.currentTimeMillis());
            users.put(pname, u);
        } else {
            u.setHomes(db.loadHomes(uuid));
            if (u.getName().equals(pname) == false) {
                final String nameToUpdate = u.getName();
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        db.updateName(nameToUpdate);
                        db.updateName(pname);
                        shared.broadcastCachedNameUpdate(uuid, pname);
                    }
                }.runTaskAsynchronously(this);
            }
            u.setName(pname);
            u.setLoginTimestamp(System.currentTimeMillis());
            u.setGlobalLoginTimestamp(System.currentTimeMillis());
            u.setIp(ip);
        }
        u.setLastServer(this.serverId);
        shared.onSharedJoin(p.getUniqueId(), this.serverId);
        forkJoinPool.execute(() -> shared.getNetty().sendPacket(new MuxSharedPackets.PacketPlayerJoinServer(p.getUniqueId(), this.serverId)));
        extras.handleJoin(p);
        chests.handleJoin(p, brandnew);
        scoreboard.handleJoin(p);
        shop.handleJoin(p);
        packets.handleJoin(p);
        chatfilter.handleJoin(pname);
        bans.handleJoin(p, u);
        email.handleJoin(p);
        if (p.hasPermission("muxsystem.x")) customrank.handleJoin(p.getUniqueId());
        if (admin.BOSSBAR.isActive()) bossbar.addPlayer(p);
        vanish.handleJoin(p);
        friends.handleJoin(p);
        tips.handleJoin(p);
        final World w = p.getWorld();
        analytics.getAnalytics().addJoinedPlayer(uuid);
        if (p.getLastPlayed() < 1680820530752L) {
            p.getInventory().clear();
            p.getEnderChest().clear();
            p.getInventory().setArmorContents(null);
            p.setExp(0);
            p.setLevel(0);
            p.setFoodLevel(20);
            p.setSaturation(20F);
            p.setFireTicks(0);
            p.setHealth(20D);
            p.setTotalExperience(0);
            p.setGameMode(GameMode.SURVIVAL);
            p.teleport(gamespawn);
            menu.addMenuItem(p, true);
            if (isDebug() == false) kits.giveKit(p, Material.IRON_CHESTPLATE, false);
            p.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 16));
            p.getInventory().addItem(new ItemStack(Material.POTION, 8, (short) 8206));
        }
        if (p.hasPlayedBefore() == false) {
            p.teleport(gamespawn);
            w.playEffect(gamespawn, Effect.MOBSPAWNER_FLAMES, 1);
            w.playEffect(gamespawn, Effect.MOBSPAWNER_FLAMES, 2);
            analytics.getAnalytics().addNewPlayer(uuid);
            final MuxUser user = u;
            new Thread(() -> {
                if (p.isValid() == false || p.isOnline() == false) return;
                final int accs = howManyAccounts(p, ip);
                if ((brandnew || user.getTrophies() == 0) && antibot.checkBot(p, accs, user.getIp().replace("/", ""))) {
                    return;
                }
                newbies.handleNewbieJoin(p);
            }).start();
        } else {
            if (p.getLastPlayed() < 1680820530752L)
                newbies.handleNewbieJoin(p);
            else {
                newbies.loadNewbie(uuid);

                if (this.beta) {
                    p.sendMessage("              §fWillkommen zurück auf §7§lMux§a§lCraft§f §3§lbeta§f!");
                    p.sendMessage("         §fDeine vorherigen §eMuxCoins §fsind noch gesichert.");
                    chatClickHoverShow(p, "        §fTeile deine Bugs und Vorschläge über §a/feedback", "§a§oKlicke zum Feedback geben", "/feedback ");
                } else {
                    p.sendMessage("                   §fWillkommen zurück auf §7§lMux§a§lCraft§f!");
                    p.sendMessage("            §fRänge, Gems und Perks: §6§lshop.muxcraft.eu");
                }
                if (events.getHolidayAnnouncement() != null)
                    p.sendMessage(events.getHolidayAnnouncement());
                p.sendMessage(" ");
            }
            if (p.isOp() && isDebug()) p.sendMessage("§c§lDer Debugmodus ist auf diesem Server aktiv.");

            if (joinmsg.equals(".") == false) {
                p.sendMessage(joinmsg);
                p.sendMessage(" ");
            }
            chat.showMissedMessages(p, u, true);
            final Location l = p.getLocation();
            w.playEffect(l, Effect.MOBSPAWNER_FLAMES, 1);
            w.playEffect(l, Effect.MOBSPAWNER_FLAMES, 2);
            if (l.getX() > 50300 || l.getZ() > 50300 || l.getX() < -50300 || l.getZ() < -50300 || l.getBlock().getType() == Material.BEDROCK) {
                p.teleport(gamespawn);
            }
            final String wname = l.getWorld().getName();
            if (wname.equals("MuxCasino") || (wname.equals("warps") && l.getBlock().getRelative(BlockFace.DOWN).getType() == Material.GRASS)) {
                p.teleport(gamespawn);
            }
            if (p.hasPermission("muxsystem.speed") == false) {
                p.setWalkSpeed(0.2F);
                p.setFlySpeed(0.1F);
            }
            if (p.isDead()) {
                p.spigot().respawn();
            }
            p.removePotionEffect(PotionEffectType.JUMP);
            if (l.getBlock().getType() != Material.LAVA) p.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
            p.removePotionEffect(PotionEffectType.SPEED);
            p.removePotionEffect(PotionEffectType.NIGHT_VISION);
            p.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
            p.removePotionEffect(PotionEffectType.REGENERATION);
            p.removePotionEffect(PotionEffectType.FAST_DIGGING);
            support.sendSupportSummary(p);
            reports.sendReportSummary(p);
            perks.activatePerks(p);
            if ((p.getGameMode() != GameMode.SURVIVAL || p.getAllowFlight())) {
                p.setAllowFlight(false);
                p.setFlying(false);
                p.setGameMode(GameMode.SURVIVAL);
            }
            votes.checkifAutoVote(p);
            activateAdminFly(p);
        }
    }

    @EventHandler
    public void onQuit(final PlayerQuitEvent e) {
        e.setQuitMessage(null);
        final Player p = e.getPlayer();
        final UUID uuid = p.getUniqueId();
        final String pname = p.getName();
        final MuxUser u = getMuxUser(pname);
        final Location l = p.getLocation();
        if (u == null) return;
        if (inEvent(p) == false && games.quitEvent(p) == false && (clanwar.handleQuit(p) || pvp.handleQuit(p) || inBattle(pname, l) && p.hasPermission("muxsystem.antileavebypass") == false)) {
            final List<ItemStack> drops = new ArrayList<>();
            for (final ItemStack i : p.getInventory().getContents()) {
                if (i != null && i.getType() != Material.AIR) drops.add(i);
            }
            for (final ItemStack i : p.getInventory().getArmorContents()) {
                if (i != null && i.getType() != Material.AIR) drops.add(i);
            }
            final PlayerDeathEvent ev = new PlayerDeathEvent(p, drops, 0, 0, "");
            this.getServer().getPluginManager().callEvent(ev);
            p.teleport(gamespawn);
            p.getInventory().clear();
            p.getInventory().setArmorContents(null);
            p.setExp(0);
            p.setLevel(0);
            p.setTotalExperience(0);
        }
        team.handleQuit(p, u);
        shared.onSharedQuit(p.getUniqueId());
        vanish.handleQuit(p);
        if (isVanish(p) == false) {
            final World w = p.getWorld();
            for (byte i = 0; i < 3; i++) {
                w.playEffect(l, Effect.SMOKE, 0);
            }
        }
        forkJoinPool.execute(() -> shared.getNetty().sendPacket(new MuxSharedPackets.PacketPlayerQuitServer(p.getUniqueId())));
        if (p.getGameMode() == GameMode.SPECTATOR) {
            forcePlayer(p, getGameSpawn());
            p.setGameMode(GameMode.SURVIVAL);
        }
        tempfix.remove(p.getUniqueId());
        packets.handleQuit(p);
        perks.deactivatePerks(pname);
        bossbar.removePlayer(p);
        cancelTP(p);
        tptasks.remove(pname); // Aus sicherheit
        tagged.remove(pname);
        activeloc.remove(pname);
        trades.removeRequests(pname);
        events.quitEvent(p, false);
        casino.handleQuit(p, true);
        godplayers.remove(pname);
        pets.handleQuit(uuid);
        menu.handleQuit(p);
        tips.handleQuit(uuid);
        mounts.handleQuit(uuid);
        emojis.stopEmoji(p);
        email.handleQuit(p);
        inv.handleQuit(pname);
        anticheat.handleQuit(p);
        tpa.remove(pname);
        lightning.remove(pname);
        globalfly.remove(pname);
        team.removeAFK(pname);
        afksince.remove(pname);
        afkplayers.remove(pname);
        currentpos.remove(pname);
        support.handleQuit(p);
        friends.handleQuit(pname);
        chat.handleQuit(pname);
        scoreboard.handleQuit(p);
        newbies.handleQuit(p);
        resetCooldown(pname, "DEATH");
        resetCooldown(pname, "CMDSPAM");
        resetCooldown(pname, "SHOPPROFITS");
        //resetCoolDown(pname, "TYPE") Reset Cooldown here
        getAnalytics().getAnalytics().addPlayTime(uuid, u.getLoginTimestamp());
        u.clearMailsIfSeen();
        u.setOnlineTime(u.getOnlineTime() + System.currentTimeMillis() - u.getLoginTimestamp());
        u.setGlobalOnline(u.getGlobalOnline() + System.currentTimeMillis() - u.getLoginTimestamp());
        users.remove(pname);
        perms.removePermissions(p, pname);
        if (antibot.didntPassCheck(p)) return;
        db.savePlayer(u);
        extras.handleQuit(p);
        chests.handleQuit(pname, uuid);
    }

    private void forcePlayerToSpawnThenRun(final Cancellable e, final Player p, final String msg) {
        forcePlayer(p, gamespawn);
        base.getVisitors().remove(p);
        p.setGameMode(GameMode.SURVIVAL);
        e.setCancelled(true);
        if (msg.startsWith("/spawn") == false && msg.startsWith("/warp spawn") == false) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    p.performCommand(msg.substring(1));
                }
            }.runTaskLater(this, 11);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCMD(final PlayerCommandPreprocessEvent e) {
        final Player p = e.getPlayer();
        final String pname = p.getName(), msg = e.getMessage().toLowerCase();
        final boolean nocooldown = msg.matches("^/(fix|repair|stack|ifix|report|support).*$");
        if (p.hasPermission("muxantispam.nocooldown") == false && checkGeneralCooldown(pname, "CMDSPAM", nocooldown ? 200L : p.hasPermission("muxsystem.vip") ? 1000L : 1500L, true)) {
            e.setCancelled(true);
            return;
        }
        updateAFK(p);
        final String[] args = msg.split(" ");
        final String maincmd = args[0];
        if (msg.equalsIgnoreCase("/wb")) { // hotfix delete after
            p.performCommand("workbench");
            e.setCancelled(true);
            return;
        }
        analytics.getAnalytics().addCMDUse(maincmd);
        if (((events.getCurrent() != null && events.getCurrent().getSpectators().size() != 0 && events.getCurrent().getSpectators().containsKey(p.getUniqueId()))
                        || (pvp.get1vs1Arenas().isEmpty() == false && pvp.get1vs1Arenas().stream().anyMatch(rankedArena -> rankedArena.getSpectators().isEmpty() == false && rankedArena.getSpectators().contains(p.getUniqueId())))
                        || (pvp.getArenas().isEmpty() == false && pvp.getArenas().stream().anyMatch(arena -> arena.getSpectators().isEmpty() == false && arena.getSpectators().contains(p.getUniqueId())))
                        || (clanwar.getArenas().isEmpty() == false && clanwar.getArenas().stream().anyMatch(arena -> arena.getSpectators().isEmpty() == false && arena.getSpectators().contains(p.getUniqueId())))
        ) && msg.matches("^/(msg|mail|pm|pn|tell|whisper|w |m |r |reply|ignore|fix|ifix|repair|report|ping|lag|sup|poll).*") == false) {
            forcePlayerToSpawnThenRun(e, p, msg);
            return;
        }
        if (base.getVisitors().containsKey(p) && msg.matches("^/(msg|base |mail|pm|pn|tell|whisper|w |m |r |reply|ignore|fix|ifix|repair|report|ping|lag|sup|poll).*") == false) {
            if (msg.equals("/base blocks")) {
                base.getBaseHandler().handleCommand(p, new String[]{"blocks"});
                return;
            }
            forcePlayerToSpawnThenRun(e, p, msg);
            return;
        }
        if (msg.matches("^/(vote).*") && msg.endsWith("emailverify")) {
            this.votes.handleCommand(p, "emailverify");
            e.setCancelled(true);
            return;
        } else if (msg.startsWith("/cancelmuxchatinput") && chat.cancelChatInput(p, true)) {
            e.setCancelled(true);
            return;
        }
        if (inGame(p) && msg.matches("^/(g leave|games leave|msg|mail|pm|pn|tell|whisper|w |m |r |reply|ignore|ping|lag|sup|menu|poll).*") == false) {
            e.setCancelled(true);
            if (msg.matches("^/(fix|repair|ifix|stack).*")) return;
            p.performCommand("menu");
            return;
        } else if (inEvent(p) && msg.matches("^/(event|msg|mail|pm|pn|tell|w |m |whisper|r |reply|ignore|fix|ifix|repair|stack|report|ping|lag|sup|o |team|menu|poll).*") == false) {
            e.setCancelled(true);
            p.performCommand("menu");
            return;
        } else if (maincmd.contains(":") || cmdmap.getCommand(maincmd.replaceFirst("/", "")) == null || msg.matches("^/(resetlogs|logs|recentlogs|alerts|muxanticheat).*$") && p.hasPermission("muxsystem.anticheat") == false
                || (msg.matches("^/(\\?|plugins|spigotguard).*$") || msg.matches("^/pl")) && p.isOp() == false ||
                msg.matches("^/(remove|restart|difficulty|calc|calculate|eval|evaluate|solve|defaultgamemode|spawnpoint|setworldspawn|setidletimeout|save-off|achievement|scoreboard|spreadplayers|blockdata|entitydata|execute|replaceitem|testfor|tellraw|toggledownfall|ban|list|worldborder|trigger|clone|setblock|pardon|slay).*$")
                || (msg.matches("^/(gamerule|effect|summon|ban|slay|about|version|title|tellraw|worldedit).*$") || msg.matches("^/(we$|we\\s.*)") || msg.matches("^/ver")) && isFullTrusted(pname) == false) {
            e.setCancelled(true);
            sendNoCMDMessage(p);
            return;
        } else if (msg.matches("^/(tp|spawn|home|base|plot|warp|pvp|back|rtp|wildnis|kick|shop|c|sex|party|g|training|duel|fduel|event|sup|p ).*$") && inBattle(pname, p.getLocation()) && p.hasPermission("muxsystem.antileavebypass") == false) {
            showItemBar(p, hnotinfight);
            e.setCancelled(true);
            return;
        } else if (isCommunicationCMD(msg)) {
            if ((p.hasPermission("muxsystem.unmutable") == false && bans.isMuted(pname) && msg.startsWith("/sup") == false && msg.startsWith("/msg list") == false) ||
                    (p.hasPermission("muxsystem.msg.bypass") == false && chat.isMute2() && msg.startsWith("/msg list") == false)) {
                final MuxUser u = getMuxUser(pname);
                if (chat.isMute2() == false && u.isTimeMuted()) {
                    if (chat.getLastSlowdownChat().getOrDefault(pname, 0L) > System.currentTimeMillis()) {
                        final long left = chat.getLastSlowdownChat().get(pname) - System.currentTimeMillis();
                        showItemBar(p, "§cDu kannst in §6" + getTime((int) (left / 1000L)) + " §cwieder eine Nachricht schreiben.");
                        e.setCancelled(true);
                    }
                    return;
                }
                showItemBar(p, "§cDieser Befehl ist für die nächsten Minuten nicht verfügbar.");
                e.setCancelled(true);
            }
            return;
        } else if (inWar(p) && msg.matches("^/(fix|repair|stack|ifix|invsee|cw|eat|feed|heal|report|ping|lag|perk|perks|poll).*$") == false) {
            showItemBar(p, "§cDu kannst keine normale Befehle während ClanWar benutzen.");
            e.setCancelled(true);
            return;
        } else if (inDuel(p) && msg.matches("^/(fix|repair|stack|ifix|invsee|duel|eat|feed|heal|report|ping|lag|perk|perks|poll).*$") == false) {
            showItemBar(p, "§cDu kannst keine normale Befehle während einem Duell benutzen.");
            e.setCancelled(true);
            return;
        } else if (in1vs1(p) && msg.matches("^/(fix|repair|stack|ifix|invsee|eat|feed|report|ping|lag|poll).*$") == false) {
            e.setCancelled(true);
            p.performCommand("menu");
            return;
        } else if (maincmd.equalsIgnoreCase("/stop") && p.isOp()) {
            if (checkLimit(pname, "STOP", 1, true)) {
                logoutAll("CONSOLE", getLang("restart"));
                if (msg.contains("-s") && isTrusted(pname)) {
                    getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "RESTART", "-s");
                    this.getConfig().set("shutdown", true);
                    this.saveConfig();
                    this.getServer().shutdown();
                } else {
                    getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "RESTART", null);
                }
                return;
            }
            if (isTrusted(pname))
                showItemBar(p, "§c/stop§f nochmal für §cRestart§f   §4/stop -s§f für absoluten §4Shutdown");
            else showItemBar(p, "§fTippe §c/stop §fnochmal ein, um §cden Server§f neuzustarten.");
            e.setCancelled(true);
            return;
        } else if (msg.matches("^/(reload|rl)") && p.isOp() && isDebug() == false) {
            showItemBar(p, "§cDieser Befehl ist nur im Debug-Modus verfügbar.");
            e.setCancelled(true);
            return;
        } else if (msg.matches("^/(whitelist).*$") && e.isCancelled() == false) {
            handleWhitelistCommand(p, msg.replace(maincmd + " ", "").split(" "));
            return;
        }
        if (msg.matches("^/(fix|repair|stack|ifix|eat|feed|essen|hunger|ping|pong|test|lag|lagg|gc|poll|abfall|ci|clean|clear|mull|trash|verlauf|history|d|paysafe|psc).*$") == false) {
            history.addHistory(p.getUniqueId(), null, "CMD", msg, null);
        }
    }

    private void handleWhitelistCommand(final Player p, final String[] args) {
        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("on")) {
                if (p.hasPermission("bukkit.command.whitelist.enable")) {
                    history.addHistory(p.getUniqueId(), null, "TEAMACTION", "WHITELIST", "ON");
                }
            } else if (args[0].equalsIgnoreCase("off")) {
                if (p.hasPermission("bukkit.command.whitelist.disable")) {
                    history.addHistory(p.getUniqueId(), null, "TEAMACTION", "WHITELIST", "OFF");
                }
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("add") && p.hasPermission("bukkit.command.whitelist.add")) {
                final OfflinePlayer op = this.getServer().getOfflinePlayer(args[1]); // --> Not over MuxSystem#getPlayer because you can whitelist never-joined players
                if (op.isWhitelisted() == false)
                    history.addHistory(op.getUniqueId(), p.getUniqueId(), "TEAMACTION", "WHITELIST ADD", null);
            } else if (args[0].equalsIgnoreCase("remove") && p.hasPermission("bukkit.command.whitelist.remove")) {
                final OfflinePlayer op = this.getServer().getOfflinePlayer(args[1]);
                if (op.isWhitelisted())
                    history.addHistory(op.getUniqueId(), p.getUniqueId(), "TEAMACTION", "WHITELIST REMOVE", null);
            }
        }
    }

    @EventHandler
    public void onServerCMD(final ServerCommandEvent e) {
        if (e.getCommand().equals("stop")) {
            logoutAll("CONSOLE", getLang("restart"));
        }
    }

    @EventHandler
    public void onDeath(final PlayerDeathEvent e) {
        final Player killed = e.getEntity();
        e.setDeathMessage(null);
        tagged.remove(killed.getName());
        cancelTP(killed);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (killed.isDead() && killed.isOnline()) {
                    killed.spigot().respawn();
                }
            }
        }.runTaskLater(this, 40L);
        if (games.inGame(killed) != null || inEvent(killed)) return;
        final List<ItemStack> drops = e.getDrops();
        final List<Item> dropentities = new ArrayList<>();
        boolean hasultra = drops.removeIf(item -> ultra.isUltraItem(item)), allitems = true;
        drops.removeIf(this::isMenuItem);
        final MuxPvP.RankedArena ranked = pvp.get1vs1Arena(killed);
        final MuxPvP.Arena duel = pvp.getArena(killed);
        final boolean friendly = duel != null && duel.isFriendly(), in1vs1 = ranked != null;
        if (in1vs1) {
            drops.clear();
            e.setKeepLevel(true);
        }
        if (friendly) {
            e.setKeepLevel(true);
        }
        if (drops.isEmpty() == false && friendly == false) {
            for (final Material m : fightmaterials) {
                if (random.nextInt(100) > 84 && drops.removeIf(i -> i.getAmount() == 1 && i.getType() == m))
                    allitems = false;
                for (final ItemStack i : drops) {
                    if (i.getType() == m && random.nextInt(100) > 84 && i.getAmount() > 1) {
                        i.setAmount(i.getAmount() - 1);
                        allitems = false;
                    }
                }
            }
        }
        drops.forEach(drop -> dropentities.add(killed.getWorld().dropItem(killed.getLocation(), drop)));
        drops.clear();
        final EntityDamageEvent dmg = e.getEntity().getLastDamageCause();
        Player killer = null;
        if (dmg instanceof EntityDamageByEntityEvent) {
            final EntityDamageByEntityEvent dmge = (EntityDamageByEntityEvent) dmg;
            final EntityType et = dmge.getDamager().getType();
            if (et.equals(EntityType.ARROW)) {
                final Projectile arrow = (Arrow) dmge.getDamager();
                if (arrow.getShooter() instanceof Player) {
                    killer = (Player) arrow.getShooter();
                }
            } else if (et.equals(EntityType.SPLASH_POTION)) {
                final ThrownPotion potion = (ThrownPotion) dmge.getDamager();
                if (potion.getShooter() instanceof Player) {
                    killer = (Player) potion.getShooter();
                }
            } else if (et.equals(EntityType.PLAYER)) {
                killer = (Player) dmge.getDamager();
            }
        }
        for (final Item item : dropentities) { // ANTILOOT + STACK FIX
            final ItemStack is = item.getItemStack();
            if (killer != null)
                item.setMetadata("LootSteal", new FixedMetadataValue(this, killer.getUniqueId().toString() + " " + System.currentTimeMillis()));
            if (is.getMaxStackSize() < is.getAmount())
                item.setMetadata("amount", new FixedMetadataValue(this, is.getAmount()));
        }
        history.addHistory(killed.getUniqueId(), killer == null ? null : killer.getUniqueId(), "DEATH",
                killer == null ? dmg == null || dmg.getCause() == null ? "Unknown" : dmg.getCause().name() : "Player", killer == null ? null : killer.getUniqueId().toString());
        if (checkGeneralCooldown(killed.getName(), "DEATH", 1000L, true)) return;
        if (killer != null) {
            if (hasultra) showItemBar(killer, "§eEinige §7[§c§lULTRA§7] §eItems wurden nicht gedroppt.");
            if (allitems == false && random.nextInt(3) == 0) {
                killer.sendMessage("§eINFO: Es besteht eine 85% Wahrscheinlichkeit dass Kampfitems gedroppt werden.");
                killer.playSound(killer.getLocation(), Sound.ANVIL_BREAK, 1F, 1F);
            }
        }
        if (inWar(killed)) {
            if (killer != null) stats.handleKill(killed, killer, false, false);
            clanwar.handleDeath(killed, false);
            return;
        } else if (duel != null) {
            if (killer != null) sendScoreboard(killer);
            if (killer != null && duel.isFriendly() == false)
                stats.handleKill(killed, killer, false, false);
            pvp.handleDuelDeath(killed, false);
            return;
        } else if (in1vs1) {
            ranked.handleDeath(killed, false);
            return;
        }
        setLastLocation(killed);
        if (killed.hasPermission("muxsystem.back")) {
            chatClickHoverRun(killed, "§7Klicke §6hier§7, um zurück zu deinem Todespunkt zu gelangen.", "§6§oKlicke zum teleportieren", "/back");
        }
        if (killer == null) {
            return;
        }
        final Player pl = killer;
        if (killed != killer) killer.setHealth(20D);
        new BukkitRunnable() {
            @Override
            public void run() {
                showItemBar(pl, getLang("antiloot"));
            }
        }.runTaskLater(this, 200L);
        stats.handleKill(killed, killer, true, false);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSpectatorDamage(final EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            final Player p = (Player) e.getEntity();
            if (p.getGameMode() == GameMode.SPECTATOR)
                e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(final EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
                e.setCancelled(true);
                return;
            }
            final Player p = (Player) e.getEntity();
            if (e.getCause() != DamageCause.ENTITY_ATTACK && pvp.in1vs1(p)) {
                MuxPvP.RankedArena arena = pvp.get1vs1Arena(p);
                if (arena == null || arena.isFinished())
                    return;
                arena.getKit().onDamage(e, p, arena);
            }
            if (e.isCancelled())
                return;
            if (e.getCause() == EntityDamageEvent.DamageCause.VOID) {
                if (games.inGame(p) != null) {
                    getGames().leaveActiveGame(p);
                    e.setCancelled(true);
                } else if (inEvent(p)) {
                    events.quitEvent(p, false);
                    e.setCancelled(true);
                }
                if (p.getGameMode() == GameMode.SPECTATOR) {
                    e.setCancelled(true);
                    return;
                } else if (p.getWorld() != getWarpsWorld()) {
                    if (p.getWorld().getName().contains("nether")) {
                        forcePlayer(p, netherspawn);
                        return;
                    }
                    forcePlayer(p, p.getLocation().getWorld().getHighestBlockAt(p.getLocation()).getLocation().add(0, 1, 0));
                }
            } else if ((e.getCause() == DamageCause.LAVA || e.getCause() == DamageCause.DROWNING) && newbies.isNewbie(p)) {
                showItemBar(p, "§fAls §aNeuling §ferhältst du noch kein Schaden.");
                e.setCancelled(true);
                return;
            }
            if (e.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION && ((p.getVehicle() != null && p.getVehicle().getType() == EntityType.ARMOR_STAND) || mounts.getMountType(p.getUniqueId()) != -1)) {
                e.setCancelled(true);
                return;
            }
            if (e instanceof EntityDamageByEntityEvent) {
                Player killer = null;
                final EntityDamageByEntityEvent dmge = (EntityDamageByEntityEvent) e;
                final Entity en = dmge.getDamager();
                if ((en instanceof Arrow || en instanceof ThrownPotion) && ((Projectile) en).getShooter() instanceof Player) {
                    killer = (Player) ((Projectile) en).getShooter();
                } else if (en instanceof Player) {
                    killer = (Player) dmge.getDamager();
                }
                if (killer != null) {
                    if (inWar(p) == false && inWar(killer) || (in1vs1(p) == false && in1vs1(killer)) || inDuel(p) == false && inDuel(killer) || inGame(p) == false && inGame(killer)) {
                        if (p.getWorld().equals(getWarpsWorld()) == false) return;
                        forcePlayer(p, getGameSpawn());
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            if (games.damageEvent(p, e) == false && inEvent(p) == false) {
                final MuxPvP.Arena duelarena = pvp.getArena(p);
                final MuxPvP.RankedArena rankedarena = pvp.get1vs1Arena(p);
                final MuxClanWar.Arena cwarena = clanwar.getArena(p);
                if ((godplayers.contains(p.getName()) || (duelarena != null && duelarena.isFinished()) ||
                        (rankedarena != null && rankedarena.isFinished()) || (cwarena != null && cwarena.isFinished()))) {
                    p.setFireTicks(0);
                    p.setRemainingAir(p.getMaximumAir());
                    e.setCancelled(true);
                }
            }
        } else if (e.getEntity() instanceof Villager || e.getEntity() instanceof Snowman) {
            if (e instanceof EntityDamageByEntityEvent) {
                Player killer = null;
                final EntityDamageByEntityEvent dmge = (EntityDamageByEntityEvent) e;
                final Entity en = dmge.getDamager();
                if ((en instanceof Arrow || en instanceof ThrownPotion) && ((Projectile) en).getShooter() instanceof Player) {
                    killer = (Player) ((Projectile) en).getShooter();
                } else if (en instanceof Player) {
                    killer = (Player) dmge.getDamager();
                }
                if (killer != null) {
                    if (e.getEntity() instanceof Villager && pvpbots.handlePvPBot(killer, e.getEntity(), null)) {
                        e.setCancelled(true);
                    } else if (killer.isOp() == false) {
                        showItemBar(killer, "§7" + (e.getEntity() instanceof Villager ? "Dorfbewohner" : "Schnemann") + "> §cLass mich in Ruhe! =(");
                        e.setCancelled(true);
                    }
                    return;
                }
            }
            e.setCancelled(true);
            if (e.getEntity().getUniqueId().equals(getIdentificationId())) return;
            e.getEntity().remove();
        } else if (e.getEntity().spigot().isInvulnerable()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(final EntityDamageByEntityEvent e) {
        final Entity en = e.getEntity();
        if (e.isCancelled()) return;
        if (en instanceof Player) {
            final Player p = (Player) en;
            final String name = p.getName();
            if (godplayers.contains(name)) {
                e.setCancelled(true);
                return;
            }
            final Location loc = p.getLocation();
            final ApplicableRegionSet region = WGBukkit.getRegionManager(loc.getWorld()).getApplicableRegions(loc);
            if (region.allows(DefaultFlag.PVP) == false || region.allows(DefaultFlag.INVINCIBILITY)) {
                return;
            }
            final Entity damager = e.getDamager();
            Player attacker = null;
            boolean isPlayer = false;
            if (damager instanceof Player) {
                attacker = (Player) damager;
                isPlayer = true;
            } else if (damager instanceof Snowball && ((Snowball) damager).getShooter() instanceof Player) {
                attacker = (Player) ((Snowball) damager).getShooter();
                final Location aloc = attacker.getLocation();
                final ApplicableRegionSet aregion = WGBukkit.getRegionManager(aloc.getWorld()).getApplicableRegions(aloc);
                if (aregion.allows(DefaultFlag.PVP) == false || aregion.allows(DefaultFlag.INVINCIBILITY)) {
                    return;
                }
                attacker.playSound(attacker.getLocation(), Sound.SUCCESSFUL_HIT, 1F, 1F);
                if (getMuxUser(attacker.getName()).isPvPbanned() == false) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 400, 0));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 400, 0));
                    showItemBar(attacker, "§fDu hast §6" + (attacker == p ? "dich" : name) + " §fkurzzeitig §ablind gemacht§f.");
                    if (attacker != p)
                        showItemBar(p, "§fDu wurdest von §6" + attacker.getName() + " §fkurzzeitig §cblind gemacht§f.");
                }
                return;
            } else if (damager instanceof FishHook && ((FishHook) damager).getShooter() instanceof Player) {
                attacker = (Player) ((FishHook) damager).getShooter();
                if (region.size() > 0 && in1vs1(attacker) == false) {
                    showItemBar(attacker, "§cDu kannst die Angel hier nicht nutzen.");
                    e.setCancelled(true);
                    return;
                }
            } else if (damager instanceof Projectile && ((Projectile) damager).getShooter() instanceof Player) {
                attacker = (Player) ((Projectile) damager).getShooter();
            }
            final String aname = attacker != null ? attacker.getName() : null;
            if (attacker == null || games.inGame(attacker) != null || inEvent(attacker) || attacker == p) {
                if (attacker != null && attacker.isDead()) {
                    e.setCancelled(true);
                }
                return;
            } else if (isPlayer && db.inOneClan(attacker.getUniqueId(), p.getUniqueId()) && inDuel(p) == false && in1vs1(p) == false ||
                    attacker.getGameMode() == GameMode.CREATIVE || isVanish(attacker) || godplayers.contains(aname) && attacker.hasPermission("muxsystem.fly") == false) {
                e.setCancelled(true);
                return;
            } else if (friends.areFriends(aname, p.getUniqueId()) && inWar(p) == false && inDuel(p) == false && inEvent(p) == false && in1vs1(p) == false) {
                showItemBar(attacker, "§cDu bist mit §6" + name + " §cbefreundet.");
                e.setCancelled(true);
                return;
            } else if (in1vs1(p) == false && newbies.handleNewbieFight(p, attacker)) {
                e.setCancelled(true);
                return;
            } else if (bans.checkPvPBan(attacker, true)) {
                e.setCancelled(true);
                return;
            } else if (bans.checkPvPBan(p, true) && bans.isRecentPvPBan(name) == false && inBattle(name, p.getLocation()) == false && region.size() > 0 && inDuel(p) == false && in1vs1(p) == false && inWar(p) == false && inEvent(p) == false) {
                forcePlayer(p, gamespawn);
                showItemBar(attacker, "§cDer Spieler ist vom PvP ausgeschlossen.");
                e.setCancelled(true);
                return;
            } else if (attacker.hasPermission("muxsystem.vip") == false && isVPN(attacker.getAddress().getAddress().getHostAddress()) == 1) {
                e.setCancelled(true);
                sendNoVPNMessage(attacker, "VIP");
                return;
            }
            if (attacker.isDead()) {
                e.setCancelled(true);
                return;
            }
            pets.deactivatePet(p);
            mounts.deactivateMount(p);
            emojis.stopEmoji(p);
            p.removePotionEffect(PotionEffectType.INVISIBILITY);
            attacker.removePotionEffect(PotionEffectType.INVISIBILITY);
            cancelTP(p);
            cancelTP(attacker);
            tagged.put(name, System.currentTimeMillis());
            tagged.put(aname, System.currentTimeMillis());
            final MuxPvP.Arena duelarena = pvp.getArena(p);
            final MuxPvP.RankedArena onearena = pvp.get1vs1Arena(p);
            if (duelarena != null) {
                duelarena.updateLastHit();
                pvp.sendScoreboard(p, MuxPvP.EnumPvPScoreboardType.DUEL);
                return;
            } else if (onearena != null) {
                onearena.updateLastHit(); // should not be null
                onearena.getKit().onDamageByEntity(e, p, damager instanceof Player ? (Player) damager : null, onearena);
                pvp.sendScoreboard(p, MuxPvP.EnumPvPScoreboardType.RANKED);
                return;
            } else if (inWar(p)) {
                clanwar.sendScoreboard(p);
                return;
            }
            sendScoreboard(p, pvpsb);
            sendScoreboard(attacker, pvpsb);
        } else if (en instanceof LivingEntity) {
            final LivingEntity le = (LivingEntity) en;
            if (en.spigot().isInvulnerable()) {
                e.setCancelled(true);
                return;
            } else if (en.getType() == EntityType.WITHER || en.getType() == EntityType.IRON_GOLEM || en.getType() == EntityType.VILLAGER || antilags.getStackedTypes().contains(en.getType()) || en.getType() == EntityType.ARMOR_STAND) {
                return;
            }
            final String name = le.getCustomName();
            if (name == null || name.equals("Dinnerbone") == false && name.equals("jeb_") == false) {
                le.setCustomName(healthToString(le.getHealth()));
            }
        } else if (en instanceof ItemFrame && e.getDamager() instanceof Player) {
            final Player p = (Player) e.getDamager();
            if (perms.hasWorldGuard(p) == false && shop.onEntityInteract(p, en)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onCombust(final EntityCombustEvent e) {
        if (e.getEntity() instanceof Player) {
            final Player p = (Player) e.getEntity();
            if (games.combustEvent(p) || godplayers.contains(p.getName())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInteract(final PlayerInteractEvent e) {
        final Action a = e.getAction();
        if (a == Action.PHYSICAL) return;
        final Player p = e.getPlayer();
        final Material m = e.getMaterial();
        updateAFK(p);
        final ItemStack item = e.getItem();
        if (m == Material.NETHER_STAR && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && e.getItem().getItemMeta().getDisplayName().contains("§")) {
            p.performCommand("menu");
            e.setCancelled(true);
            return;
        } else if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) {
            if (p.hasPermission("muxsystem.gold")) gold.handleMacro(p, item, true);
            if (m == Material.GLASS_BOTTLE && p.getInventory().firstEmpty() == -1 || m == Material.FIREBALL) {
                e.setCancelled(true);
                return;
            } else if (m == Material.EXP_BOTTLE && item.hasItemMeta() && item.getItemMeta().hasLore() && item.getItemMeta().getLore().size() == 1 && item.getItemMeta().getLore().get(0).startsWith("§7Gespeicherte EXP: §a")) {
                Integer xp;
                try {
                    xp = Integer.parseInt(item.getItemMeta().getLore().get(0).replaceFirst("§7Gespeicherte EXP: §a", ""));
                } catch (final NumberFormatException ex) {
                    xp = null;
                }
                if (xp != null && xp > 0) {
                    e.setCancelled(true);
                    p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 1F);
                    extracmds.setTotalExperienceNew(p, extracmds.getPlayerExperience(p) + xp);
                    if (item.getAmount() > 1) {
                        item.setAmount(item.getAmount() - 1);
                    } else {
                        p.getInventory().remove(item);
                    }
                    p.updateInventory();
                    return;
                }
            } else if (m == Material.ENDER_PEARL) {
                showItemBar(p, "§cEnderperlen sind nicht erlaubt.");
                e.setCancelled(true);
                p.updateInventory();
                return;
            } else if (p.getGameMode() == GameMode.CREATIVE && isArmor(m)) {
                e.setCancelled(true);
                p.updateInventory();
                return;
            } else if (a == Action.RIGHT_CLICK_BLOCK) {
                final Block b = e.getClickedBlock();
                final Material t = b.getType();
                if (b.getState() instanceof Sign) {
                    if (handleSign(p, b, true))
                        e.setCancelled(true);
                    return;
                } else if (t == Material.ENDER_CHEST) {
                    if ((p.isSneaking() == false || p.getItemInHand().getType() == Material.AIR) && extraenderchest.openExtraEnderChest(p, true)) {
                        e.setCancelled(true);
                        return;
                    } else if (random.nextInt(15) == 0) { // Werbung
                        if (extras.hasExtraEnderChest(p.getUniqueId()) == false) {
                            chatClickHoverRun(p, getLang("echestad"), getLang("echestadhover"), "/gemshop");
                        }
                    }
                } else if (m == Material.ANVIL && WGBukkit.getRegionManager(p.getLocation().getWorld()).getApplicableRegions(p.getLocation()).canBuild(WGBukkit.getPlugin().wrapPlayer(p)) == false) {
                    e.setCancelled(true);
                    e.setUseInteractedBlock(PlayerInteractEvent.Result.DENY);
                } else if (isAlwaysAllowed(t)) {
                    e.setCancelled(false);
                    return;
                } else if (t == Material.TRAPPED_CHEST && chests.handleInteract(p, b)) {
                    e.setCancelled(true);
                } else if (t == Material.MOB_SPAWNER && (m == Material.AIR || m.isBlock() == false)) {
                    if (e.isCancelled()) return;
                    e.setCancelled(true);
                    p.performCommand("spawner");
                    return;
                } else if (t == Material.SKULL && (p.getItemInHand() == null || p.getItemInHand().getType() == Material.AIR)) {
                    final String head = ((Skull) b.getState()).getOwner();
                    final Location l = b.getLocation();
                    final boolean show = WGBukkit.getRegionManager(l.getWorld()).getApplicableRegions(l).size() == 0 || p.isOp();
                    if (head != null && show) showItemBar(p, "§fDas ist der Kopf von §a" + head + "§f.");
                    return;
                }
            }
        } else if (a == Action.LEFT_CLICK_BLOCK) {
            final Block b = e.getClickedBlock();
            final Material t = b.getType();
            final boolean notpickaxe = m != Material.WOOD_PICKAXE && m != Material.STONE_PICKAXE && m != Material.IRON_PICKAXE && m != Material.GOLD_PICKAXE && m != Material.DIAMOND_PICKAXE;
            if (t == Material.BEDROCK && p.getWorld() != base.getWorld()) {
                if (notpickaxe) {
                    showItemBar(p, "§cNutze eine Spitzhacke, um dein Bedrock zu zerstören.");
                    return;
                } else if (p.getInventory().firstEmpty() == -1) {
                    e.setCancelled(true);
                    showItemBar(p, "§cDein Inventar ist voll.");
                    return;
                }
                if (checkGeneralCooldown(p.getName(), "BEDROCKDESTROY", 500L, false)) {
                    e.setCancelled(true);
                    return;
                }
                checkGeneralCooldown(p.getName(), "BEDROCKDESTROY", 500L, true);
                final boolean remove = db.removeBlock(p.getUniqueId(), b.getWorld().getName(), b.getX(), b.getY(), b.getZ(), b.getTypeId());
                final boolean grief = base.isInGriefableBase(p);
                if (remove == false && grief == false) {
                    showItemBar(p, "§cDu hast dieses Bedrock nicht platziert.");
                    return;
                }
                b.setType(Material.AIR);
                p.getInventory().addItem(new ItemStack(Material.BEDROCK));
                p.updateInventory();
                p.playSound(p.getLocation(), Sound.DIG_STONE, 1F, 1F);
                if (grief == false)
                    showItemBar(p, "§aDu konntest den Bedrock abbauen, da du ihn platziert hast.");
            } else if (t == Material.TRAPPED_CHEST && chests.handleInteract(p, b) && p.getGameMode() != GameMode.CREATIVE) {
                e.setCancelled(true);
            } else if (b.getState() instanceof Sign) {
                if (handleSign(p, b, false) && p.getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
            } else if (t == Material.MOB_SPAWNER) {
                if (e.isCancelled()) return;
                else if (p.isSneaking()) {
                    e.setCancelled(true);
                    p.performCommand("spawner");
                    return;
                }
                if (notpickaxe) {
                    e.setCancelled(true);
                    showItemBar(p, "§cNutze eine Spitzhacke, um den Spawner zu zerstören.");
                    return;
                } else if (p.getInventory().firstEmpty() == -1) {
                    e.setCancelled(true);
                    showItemBar(p, "§cDein Inventar ist voll.");
                    return;
                }
                if (checkGeneralCooldown(p.getName(), "SPAWNERDESTROY", 500L, false)) {
                    e.setCancelled(true);
                    return;
                }
                checkGeneralCooldown(p.getName(), "SPAWNERDESTROY", 500L, true);
                final boolean remove = db.removeBlock(p.getUniqueId(), b.getWorld().getName(), b.getX(), b.getY(), b.getZ(), b.getTypeId());
                final boolean grief = base.isInGriefableBase(p);
                if (remove == false && grief == false) {
                    e.setCancelled(true);
                    showItemBar(p, "§cDu hast diesen Spawner nicht gesetzt.");
                    return;
                }
                booster.onSpawnerBreak(b);
                b.setType(Material.AIR);
                p.getInventory().addItem(renameItemStack(new ItemStack(Material.MOB_SPAWNER), "§a§lMuxSpawner"));
                p.updateInventory();
                p.playSound(p.getLocation(), Sound.DIG_STONE, 1F, 1F);
                if (grief == false)
                    showItemBar(p, "§aDu konntest den Spawner abbauen, da du ihn platziert hast.");
            }
        }
        if (a == Action.LEFT_CLICK_BLOCK || a == Action.LEFT_CLICK_AIR) {
            anticheat.addClick(p);
            if (p.hasPermission("muxsystem.gold")) gold.handleMacro(p, item, false);
            if (m == Material.SPIDER_EYE) {
                PotionEffect slow = null;
                for (final PotionEffect pe : p.getActivePotionEffects()) {
                    if (pe.getType().equals(PotionEffectType.SLOW)) slow = pe;
                }
                if (slow != null && slow.getAmplifier() == 10) {
                    p.removePotionEffect(PotionEffectType.SLOW);
                } else if (slow == null) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 20000, 10));
                }
            } else if (lightning.contains(p.getName())) {
                final Block b = p.getTargetBlock((Set<Material>) null, 600);
                if (b == null) return;
                p.getWorld().strikeLightning(b.getLocation());
            }
        }
        if (m == Material.MUSHROOM_SOUP && (in1vs1(p) || (events.inEvent(p) instanceof MuxTournament))) {
            e.setCancelled(true);
            if (p.getHealth() >= p.getMaxHealth() && p.getFoodLevel() == 20) {
                return;
            }
            if (p.getHealth() >= p.getMaxHealth()) {
                p.setFoodLevel(Math.min(20, p.getFoodLevel() + 7));
                p.setSaturation(Math.min(20, p.getSaturation() + 7));
            } else p.setHealth(Math.min(20D, p.getHealth() + 7D));
            e.getItem().setType(Material.BOWL);
            p.updateInventory();
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(final PlayerInteractEntityEvent e) {
        final Entity en = e.getRightClicked();
        final EntityType et = en.getType();
        if (et == EntityType.ITEM_FRAME) { // Duplication Fix ItemFrame
            final Player p = e.getPlayer();
            if (shop.onEntityInteract(e.getPlayer(), en) || p.getGameMode() == GameMode.CREATIVE && isFullTrusted(p.getName()) == false || ultra.isUltraItem(p.getItemInHand()) || isMenuItem(p.getItemInHand())) {
                e.setCancelled(true);
            }
        } else if (et == EntityType.PLAYER) {
            final Player p = e.getPlayer();
            final Location l = p.getLocation();
            final Material m = p.getItemInHand().getType();
            final boolean ignore = m == Material.EGG || m == Material.SNOW_BALL || m == Material.EXP_BOTTLE || m == Material.POTION || m == Material.BOW || m == Material.FISHING_ROD || m.toString().endsWith("SWORD") || m.isEdible();
            final ApplicableRegionSet rgs = WGBukkit.getRegionManager(l.getWorld()).getApplicableRegions(l);
            if (ignore == false && (rgs.allows(DefaultFlag.PVP) == false || rgs.size() == 0)) {
                e.setCancelled(true);
                final Player clicked = (Player) e.getRightClicked();
                if (p.canSee(clicked)) inv.openPlayerMenu(p, clicked);
            }
        } else if (et == EntityType.VILLAGER) {
            if (((LivingEntity) en).getHealth() > 100D) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onGameModeChange(final PlayerGameModeChangeEvent e) {
        if (e.getNewGameMode() == GameMode.SURVIVAL || e.getNewGameMode() == GameMode.ADVENTURE) {
            globalfly.remove(e.getPlayer().getName());
        }
        if (admin.BOSSBAR.isActive()) {
            bossbar.removePlayer(e.getPlayer());
            if (e.getNewGameMode() != GameMode.SPECTATOR) bossbar.addPlayer(e.getPlayer());
        }
    }

    @EventHandler
    public void onInvCreative(final InventoryCreativeEvent e) {
        if (e.getCursor() != null) {
            final Player p = (Player) e.getWhoClicked();
            if (isFullTrusted(p.getName()) == false) {
                final ItemStack i = e.getCursor();
                final Material m = i.getType();

                final MutableInt unsafe = new MutableInt(0);
                i.getEnchantments().forEach((key, value) -> {
                    if (value > key.getMaxLevel()) unsafe.add(1);
                });

                if (m == Material.DIAMOND || m == Material.EMERALD || m == Material.GOLD_INGOT || m == Material.IRON_INGOT
                        || m == Material.DIAMOND_BLOCK || m == Material.GOLD_BLOCK || m == Material.IRON_BLOCK || m == Material.EMERALD_BLOCK
                        || m == Material.DIAMOND_ORE || m == Material.GOLD_ORE || m == Material.IRON_ORE || m == Material.EMERALD_ORE
                        || m == Material.COAL_ORE || m == Material.LAPIS_ORE || m == Material.REDSTONE_ORE || m == Material.ENDER_PORTAL_FRAME
                        || m == Material.SPONGE || m == Material.BEDROCK || m == Material.EXP_BOTTLE || m == Material.GOLD_NUGGET || m == Material.NETHER_STAR
                        || m == Material.POTION || m == Material.ENCHANTED_BOOK || m == Material.ARROW || m == Material.GOLDEN_APPLE || m.getMaxDurability() != 0
                        || m == Material.FIREBALL || m == Material.SNOW_BALL || m == Material.EMPTY_MAP || m == Material.MONSTER_EGG || m == Material.BARRIER
                        || m == Material.ARMOR_STAND || m == Material.EXPLOSIVE_MINECART || m == Material.STORAGE_MINECART || m == Material.HOPPER_MINECART
                        || m == Material.HOPPER || i.hasItemMeta() || unsafe.intValue() > 0) {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onUltraClick(final InventoryClickEvent e) {
        final ItemStack current, cursor = e.getCursor();
        if (e.getAction().name().contains("HOTBAR")) {
            current = e.getView().getBottomInventory().getItem(e.getHotbarButton());
        } else {
            current = e.getCurrentItem();
        }
        if (current != null && isMenuItem(current) || e.getCurrentItem() != null && isMenuItem(e.getCurrentItem()) || cursor != null && isMenuItem(cursor)) {
            e.setCancelled(true);
            return;
        }
        // ULTRA Items only in normal Player Inv, TRASH (PlayerChest for Admins)
        if (current != null && (ultra.isUltraItem(current) || isMenuItem(current)) || cursor != null && (ultra.isUltraItem(cursor) || isMenuItem(cursor))) {
            final InvType it = getActiveInv(e.getWhoClicked().getName());
            if (current != null && emojis.isEmojiItem(current) || cursor != null && emojis.isEmojiItem(cursor) || e.getCurrentItem() != null && emojis.isEmojiItem(e.getCurrentItem()))
                e.setCancelled(true);
            else if (it != InvType.TRASH && it != InvType.PLAYERCHEST && e.getView().getTopInventory().getType() != InventoryType.CRAFTING &&
                    e.getView().getTopInventory().getType() != InventoryType.ENDER_CHEST && e.getInventory().getTitle().equals(getLang("extraenderchest")) == false) {
                e.setCancelled(true);
            }
        } else if (current != null && emojis.isEmojiItem(current) || e.getCurrentItem() != null && emojis.isEmojiItem(e.getCurrentItem()) || cursor != null && emojis.isEmojiItem(cursor)) {
            e.setCancelled(true);
        }
    }

    public boolean isMenuItem(final ItemStack item) {
        return item.getType() == Material.NETHER_STAR && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().contains("§f§lMenü");
    }

    public ConcurrentMap<Location, ArmorStand> getChairs() {
        return chairs.getChairs();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(final BlockBreakEvent e) {
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE) return;
        final Material m = e.getBlock().getType();
        final ItemStack item = e.getPlayer().getItemInHand();
        if (m == Material.SNOW) {
            if (item.getEnchantmentLevel(Enchantment.SILK_TOUCH) < 1) {
                e.setCancelled(true);
                e.getBlock().setType(Material.AIR);
                showItemBar(e.getPlayer(), "§fVote, um Schneebälle zu erhalten. §6/vote");
                return;
            }
            e.setCancelled(true);
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(Material.SNOW));
            e.getBlock().setType(Material.AIR);
            return;
        } else if (m == Material.SNOW_BLOCK) {
            e.setCancelled(true);
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(Material.SNOW_BLOCK));
            e.getBlock().setType(Material.AIR);
            return;
        }
        if (item != null && (item.getType() == Material.DIAMOND_PICKAXE || item.getType() == Material.DIAMOND_AXE || item.getType() == Material.DIAMOND_SPADE) && item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
                item.getItemMeta().hasLore() && item.getItemMeta().getDisplayName().contains("Multiblock ")) {
            final int usesleft = Integer.parseInt(ChatColor.stripColor(item.getItemMeta().getLore().get(3)).split(" / ")[0].replaceAll("\\D+", ""));
            final Player p = e.getPlayer();
            e.setCancelled(true);
            final String tool = item.getType() == Material.DIAMOND_PICKAXE ? "Spitzhacke" : item.getType() == Material.DIAMOND_SPADE ? "Schaufel" : "Axt";
            if (item.getAmount() > 1) {
                showItemBar(e.getPlayer(), "§cDu kannst nur eine " + tool + " aufeinmal nutzen.");
                return;
            } else if (usesleft - 1 < 1) {
                p.setItemInHand(null);
                p.playSound(p.getLocation(), Sound.ITEM_BREAK, 1F, 1F);
                p.updateInventory();
            } else {
                final ItemStack fMP = new ItemStack(item.getType(), 1, (short) (1562 - (0.3124 * (usesleft - 1))));
                fMP.addEnchantment(Enchantment.DIG_SPEED, 5);
                final ItemMeta im = fMP.getItemMeta();
                im.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                fMP.setItemMeta(im);
                p.setItemInHand(renameItemStack(fMP, "§7[§a§lMUX§7] §b§lMultiblock " + tool,
                        "§7Zerstöre alle Blöcke in deinem", "§7Umkreis mit einem Linksklick.", "", "§7Haltbarkeit: " + (usesleft - 1) + " / 5000"));
                p.updateInventory();
            }
            final Location l = e.getBlock().getLocation();
            final World w = l.getWorld();
            final RegionManager rg = WGBukkit.getRegionManager(p.getWorld());
            final int range = 1, ox = l.getBlockX(), oy = l.getBlockY(), oz = l.getBlockZ();
            final boolean inBase = base.getCurrentLocations().containsKey(p);
            final PlayerBase base = this.base.getCurrentLocations().get(p);
            for (int x = ox - range; x <= ox + range; ++x) {
                for (int y = oy - range; y <= oy + range; ++y) {
                    for (int z = oz - range; z <= oz + range; ++z) {
                        final Block b = w.getBlockAt(x, y, z);
                        final Material m2 = b.getType();
                        if (m2 == Material.BEDROCK || m2 == Material.BARRIER
                                || m2 == Material.ENDER_PORTAL_FRAME || m2 == Material.DRAGON_EGG || m2 == Material.SNOW
                                || m2 == Material.SNOW_BLOCK || m2 == Material.CHEST || m2 == Material.TRAPPED_CHEST
                                || m2 == Material.MOB_SPAWNER || rg.getApplicableRegions(b.getLocation()).size() > 0
                                || (inBase && base.contains(b.getLocation(), true) == false)) {
                            continue;
                        }
                        mining.handleBreak(p, b, p.getItemInHand());
                        b.breakNaturally();
                        w.playEffect(b.getLocation(), Effect.STEP_SOUND, b.getTypeId(), 1);
                    }
                }
            }
        }
        mining.handleBreak(e.getPlayer(), e.getBlock(), item);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(final BlockPlaceEvent e) {
        final ItemStack i = e.getItemInHand();
        if (i != null) {
            final Player p = e.getPlayer();
            final Material m = i.getType();
            final MuxPvP.RankedArena arena = pvp.get1vs1Arena(p);
            if (arena != null) {
                arena.getKit().onBlockPlace(e, p, i, m, arena);
                return;
            }
            if (e.isCancelled()) return;
            if (inGame(p) || p.getGameMode() == GameMode.CREATIVE && isFullTrusted(p.getName()) == false &&
                    (m == Material.DIAMOND_BLOCK || m == Material.GOLD_BLOCK || m == Material.IRON_BLOCK || m == Material.EMERALD_BLOCK || m == Material.BEDROCK ||
                            m == Material.MOB_SPAWNER || m == Material.DIAMOND_ORE || m == Material.GOLD_ORE || m == Material.EMERALD_ORE || m == Material.IRON_ORE)) {
                e.setCancelled(true);
                return;
            } else if (m == Material.MOB_SPAWNER) {
                final Block b = e.getBlock();
                final ItemMeta im = i.getItemMeta();
                if (im != null && im.hasDisplayName() && im.getDisplayName().startsWith("§a")) {
                    inv.openSpawner(p);
                    inv.setSpawner(p.getName(), (CreatureSpawner) b.getState());
                    if (p.getGameMode() == GameMode.CREATIVE) {
                        removeOneItemFromHand(p);
                    }
                    db.addBlock(p.getUniqueId(), b.getWorld().getName(), b.getX(), b.getY(), b.getZ(), b.getTypeId());
                    showItemBar(p, "§fDu kannst den Spawner jederzeit wieder §6versetzen§f.");
                } else if (im == null || im.hasDisplayName() == false) {
                    e.setCancelled(true);
                    p.setItemInHand(null);
                }
            } else if (m == Material.BED && p.getWorld().getEnvironment() == Environment.NETHER) {
                e.setCancelled(true);
                showItemBar(p, getLang("nobed"));
            } else if (m == Material.GOLD_PLATE || m == Material.IRON_PLATE) {
                final Block down = e.getBlock().getRelative(BlockFace.DOWN);
                if (down != null && (down.getType() == Material.BEDROCK || down.getType() == Material.OBSIDIAN)) {
                    e.setCancelled(true);
                    showItemBar(p, "§cDas kannst du nicht hier platzieren.");
                }
            } else if (m == Material.SLIME_BLOCK) {
                e.setCancelled(true);
                showItemBar(p, getLang("noslime"));
            } else if (p.getWorld().equals(farmworld) && (m == Material.IRON_ORE || m == Material.GOLD_ORE)) {
                e.setCancelled(true);
                showItemBar(p, "§cDu kannst diesen Erzblock hier nicht platzieren.");
            } else if (m == Material.ICE || m == Material.RAILS || m == Material.POWERED_RAIL || m == Material.ACTIVATOR_RAIL || m == Material.DETECTOR_RAIL) {
                final Block b = e.getBlock();
                if (b.getWorld().equals(gamespawn.getWorld()) && b.getLocation().distance(gamespawn) < 1000D && p.isOp() == false) {
                    showItemBar(p, "§cDistanziere dich vom Spawn um diesen Block zu platzieren.");
                    e.setCancelled(true);
                    return;
                }
            } else if (m == Material.PISTON_BASE || m == Material.BEDROCK || m == Material.SAPLING) {
                final Block b = e.getBlock();
                if (b.getWorld().equals(gamespawn.getWorld()) && b.getLocation().distance(gamespawn) < 500D && p.isOp() == false) {
                    showItemBar(p, "§cDistanziere dich vom Spawn um diesen Block zu platzieren.");
                    e.setCancelled(true);
                    return;
                } else if (m == Material.BEDROCK) {
                    if (p.getGameMode() == GameMode.CREATIVE) {
                        removeOneItemFromHand(p);
                    }
                    db.addBlock(p.getUniqueId(), b.getWorld().getName(), b.getX(), b.getY(), b.getZ(), b.getTypeId());
                }
            }
            final Location l = e.getBlock().getLocation();
            final String wname = l.getWorld().getName();
            final double x = l.getX(), z = l.getZ(), y = l.getY();
            final MuxUser u = getMuxUser(p.getName());
            final Map<String, Home> hs = u.getHomes();
            boolean home = false;
            String homename = "";
            for (final Map.Entry<String, Home> entry : hs.entrySet()) {
                final Home h = entry.getValue();
                final int floory = (int) Math.floor(h.getY());
                if ((int) Math.floor(h.getX()) == x && (int) Math.floor(h.getZ()) == z && floory + 2 >= y && floory - 2 <= y && h.getWorld().equals(wname)) {
                    home = true;
                    homename = entry.getKey();
                    break;
                }
            }
            if (home) {
                e.setCancelled(true);
                showItemBar(p, "§cVerschiebe dein Zuhause " + (hs.size() > 1 ? "§6" + homename + "§c " : "") + "§cum hier zu bauen.");
            } else if (x > worldlimit || z > worldlimit || x < -worldlimit || z < -worldlimit) {
                e.setCancelled(true);
                showItemBar(p, "§c§lDu hast das Limit der Welt erreicht.");
            }
        }
    }

    @EventHandler
    public void onEnchant(final EnchantItemEvent e) {
        final Player p = e.getEnchanter();
        if (p.getGameMode() == GameMode.CREATIVE && isTrusted(p.getName()) == false) {
            showItemBar(p, "§cDu kannst nur im Survival-Modus enchanten.");
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onIgnite(final BlockIgniteEvent e) {
        final Block b = e.getBlock(), b1 = b.getRelative(BlockFace.DOWN), b2 = b.getRelative(BlockFace.EAST), b3 = b.getRelative(BlockFace.WEST);
        if (b.getType() == Material.OBSIDIAN || b1 != null && b1.getType() == Material.OBSIDIAN || b2 != null && b2.getType() == Material.OBSIDIAN || b3 != null && b3.getType() == Material.OBSIDIAN) {
            if (e.getIgnitingEntity() instanceof Player) {
                showItemBar((Player) e.getIgnitingEntity(), "§cNutze dafür das Netherportal am Spawn.");
            }
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBucket(final PlayerBucketEmptyEvent e) {
        final Block b = e.getBlockClicked();
        if (b.getWorld().equals(gamespawn.getWorld()) && b.getLocation().distance(gamespawn) < 1500D && e.getPlayer().isOp() == false) {
            showItemBar(e.getPlayer(), "§cDistanziere dich vom Spawn um Lava/Wasser zu benutzen.");
            e.setCancelled(true);
        } else if (e.getBucket() == Material.WATER_BUCKET && b.getWorld().getName().equals(nethername)) {
            final ItemStack i = e.getPlayer().getItemInHand();
            if (i.hasItemMeta() && i.getItemMeta().hasLore()) {
                if (antilags.isStopLag()) {
                    e.setCancelled(true);
                    showItemBar(e.getPlayer(), "§cVersuche es später erneut.");
                    return;
                }
                b.getRelative(e.getBlockFace()).setType(Material.WATER);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(final PlayerPickupItemEvent e) {
        final Item item = e.getItem();
        final Player p = e.getPlayer();
        if (trades.isTrading(p.getName()) || isVanish(p) || getActiveInv(p.getName()) == InvType.MARKET) {
            e.setCancelled(true);
        } else if (emojis.isEmojiItem(item.getItemStack())) {
            e.setCancelled(true);
            e.getItem().remove();
        } else if (item.hasMetadata("LootSteal")) {
            final String[] values = item.getMetadata("LootSteal").get(0).asString().split(" ");
            if (p.getUniqueId().toString().equals(values[0]) == false && System.currentTimeMillis() - Long.parseLong(values[1]) < 10000L) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(final PlayerDropItemEvent e) {
        final Player p = e.getPlayer();
        if (inGame(p)) {
            e.setCancelled(true);
            showItemBar(p, "§cDu kannst keine Items in diesem Spiel droppen.");
            return;
        } else if (in1vs1(p)) {
            e.setCancelled(true);
            showItemBar(p, "§cDu kannst im 1vs1 keine Items droppen.");
            return;
        }
        final Event event = events.inEvent(p);
        if (event != null && event.canDrop() == false) {
            showItemBar(p, "§cDu kannst im Event keine Items droppen.");
            e.setCancelled(true);
            return;
        }
        final ItemStack item = e.getItemDrop().getItemStack();
        if (isMenuItem(item)) {
            e.setCancelled(true);
        } else if (ultra.isUltraItem(item)) {
            showItemBar(p, "§cDu kannst keine §7[§c§lULTRA§7] §cItems droppen.");
            e.setCancelled(true);
        } else if (emojis.isEmojiItem(item)) {
            e.getItemDrop().remove();
            p.updateInventory();
        } else if (isRenamedByPlayer(item) && WGBukkit.getRegionManager(p.getWorld()).getApplicableRegions(p.getLocation()).size() > 0) {
            showItemBar(p, "§cBenutze §6/handel §cum das Item zu droppen.");
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onShootBow(final EntityShootBowEvent e) {
        if (e.getEntity() instanceof Player == false) return;
        final Player p = (Player) e.getEntity();
        final Location l = p.getLocation();
        if (WGBukkit.getRegionManager(l.getWorld()).getApplicableRegions(l).allows(DefaultFlag.PVP) == false) {
            e.setCancelled(true);
            showItemBar(p, getLang("nobow"));
            p.updateInventory();
        }
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntitySpawn(final CreatureSpawnEvent e) {
        final Entity en = e.getEntity();
        if (en.getType() == EntityType.CHICKEN && e.getSpawnReason() == CreatureSpawnEvent.SpawnReason.MOUNT)
            e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSpawnSpawner(final SpawnerSpawnEvent e) {
        final BoosterType t = booster.hasBooster(e.getSpawner().getLocation());
        if (e.getSpawner().getSpawnedType() == EntityType.PIG_ZOMBIE) { // 20% Chance | Booster: 80% Chance
            if (random.nextInt(5) <= (t == BoosterType.SPAWNER || t == BoosterType.MEGASPAWNER ? 0 : 3))
                e.setCancelled(true);
        } else if (e.getSpawner().getSpawnedType() == EntityType.IRON_GOLEM) { // 5% Chance | Booster: 20% Chance
            if (random.nextInt(20) <= (t == BoosterType.SPAWNER || t == BoosterType.MEGASPAWNER ? 15 : 18))
                e.setCancelled(true);
        } else if (t == BoosterType.SPAWNER || t == BoosterType.MEGASPAWNER) { // Doubled Spawns
            final Location l = e.getLocation();
            l.getWorld().spawn(l, e.getEntity().getClass());
        }
    }

    @EventHandler
    public void onWorldChange(final PlayerChangedWorldEvent e) {
        final Player p = e.getPlayer();
        emojis.stopEmoji(p);
        gold.checkRain(p);
        if (admin.BOSSBAR.isActive()) {
            bossbar.removePlayer(p);
            bossbar.addPlayer(p);
        }
        /* SEEMS TO LAG

        final int before = Boolean.parseBoolean(e.getFrom().getGameRuleValue("reducedDebugInfo")) ? 22 : 23;
        final int after = Boolean.parseBoolean(p.getWorld().getGameRuleValue("reducedDebugInfo")) ? 22 : 23;
        if (before != after) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    final EntityPlayer entityplayer = ((CraftPlayer) p).getHandle();
                    entityplayer.playerConnection.sendPacket(new PacketPlayOutEntityStatus(entityplayer, (byte) after));
                }
            }.runTaskLater(this, 1L);
        }*/
    }

    @EventHandler(ignoreCancelled = true)
    public void onSplash(final PotionSplashEvent e) {
        final ThrownPotion potion = e.getPotion();
        final short data = potion.getItem().getDurability();
        boolean ignore = false;
        final Player shooter = potion.getShooter() instanceof Player ? (Player) potion.getShooter() : null;
        final boolean weakness = potion.getEffects().parallelStream().anyMatch(potionEffect -> potionEffect.getType().equals(PotionEffectType.WEAKNESS)),
                poison = potion.getEffects().parallelStream().anyMatch(potionEffect -> potionEffect.getType().equals(PotionEffectType.POISON)),
                disabledmg = WGBukkit.getRegionManager(potion.getLocation().getWorld()).getApplicableRegions(potion.getLocation()).allows(DefaultFlag.ENDER_BUILD) == false;
        for (final LivingEntity entity : e.getAffectedEntities()) {
            final String name = entity instanceof Player ? entity.getName() : null, ip = shooter == null ? "" : shooter.getAddress().getAddress().getHostAddress();
            if (name != null && (godplayers.contains(name) || isVanish((Player) entity) ||
                    ((weakness) && (perks.hasNoWeakness(name) || (shooter != null && (getMuxUser(shooter.getName()).isPvPbanned() || (shooter.hasPermission("muxsystem.vip") == false && isVPN(ip) == 1))))) ||
                    ((poison) && (perks.hasAntiPoison(name) || (shooter != null && (getMuxUser(shooter.getName()).isPvPbanned() || (shooter.hasPermission("muxsystem.vip") == false && isVPN(ip) == 1)))))) ||
                    entity.getUniqueId().equals(getIdentificationId()) && (data == 16462 || data == 16398 || data == 16430)) {
                e.setIntensity(entity, 0D);
                ignore = true;
            } else if (dmgpotions.contains(data)) {
                if (disabledmg) {
                    e.setIntensity(entity, 0D);
                } else e.setIntensity(entity, 0.4D);
            } else if (shooter != null && friends.areFriends(name, shooter.getUniqueId())) {
                if (data == 16388 || data == 16420 || data == 16452 || data == 16424 || data == 16456 || data == 16426 || data == 16458) {
                    showItemBar((Player) potion.getShooter(), "§cDu bist mit §6" + name + " §cbefreundet.");
                }
            }
        }
        if (ignore || shooter == null) return;
        final Location loc = shooter.getLocation();
        if (WGBukkit.getRegionManager(loc.getWorld()).getApplicableRegions(loc).allows(DefaultFlag.PVP) == false) {
            if (goodpotions.contains(data) == false || inEvent(shooter) || inGame(shooter) || inCasino(shooter)) {
                e.setCancelled(true);
                showItemBar(shooter, getLang("nopotions"));
            }
        } else if (random.nextInt(120) == 0) { // Werbung
            if (extras.hasPerks(shooter.getUniqueId()) == false) {
                chatClickHoverRun(shooter, getLang("perkad"), getLang("perkadhover"), "/perks");
            }
        }
    }

    @EventHandler
    public void onLeavesDecay(final LeavesDecayEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMoveEvent(final PlayerMoveEvent e) {
        final Location loc = e.getFrom();
        final Player p = e.getPlayer();
        if (loc.getBlock().isLiquid()) {
            final World w = loc.getWorld();
            final String wname = w.getName();
            if (wname.equals("warps") || wname.equals("MuxCasino")) {
                if (WGBukkit.getRegionManager(p.getWorld()).getApplicableRegions(p.getLocation()).allows(DefaultFlag.PVP) == false && inEvent(p) == false) {
                    setLastLocation(p);
                    teleportPlayer(p, gamespawn);
                }
            } else if (wname.equals(nethername)) {
                if (netherportalx.containsDouble(loc.getX()) && netherportalz.containsDouble(loc.getZ())) {
                    teleportPlayer(p, gamespawn);
                    final Location netherloc = warps.getWarpLocation("nether");
                    setLastLocation(p, netherloc == null ? w.getSpawnLocation() : netherloc);
                }
            } else if (w.getEnvironment() == Environment.THE_END) {
                teleportPlayer(p, gamespawn);
                final Location endloc = warps.getWarpLocation("end");
                setLastLocation(p, endloc == null ? w.getSpawnLocation() : endloc);
            }
        } else if (p.getLocation().getBlock().getType() == Material.BEDROCK && inEvent(p) == false && p.getGameMode() != GameMode.SPECTATOR) {
            e.setCancelled(true);
            forcePlayer(e.getPlayer(), getGameSpawn());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockFromTo(final BlockFromToEvent e) {
        final Material m = e.getBlock().getType();
        if ((m == Material.WATER || m == Material.STATIONARY_WATER) && e.getBlock().getWorld().equals(getWarpsWorld())) {
            e.setCancelled(true);
        }
    }

    private Map<String, Entry<Long, StackTraceElement[]>> teleportStacktrace = new HashMap<>();
    private final Queue<Entry<Long, Entry<String, StackTraceElement[]>>> stacktraceQueue = new ConcurrentLinkedQueue<>();

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void afterTeleport(final PlayerTeleportEvent e) {
        final Player p = e.getPlayer();
        Entry<Long, StackTraceElement[]> entry = teleportStacktrace.get(p.getName());
        if (entry != null && entry.getKey() > System.currentTimeMillis()) {
            Entry<Long, Entry<String, StackTraceElement[]>> entryToPut = new AbstractMap.SimpleEntry<>(entry.getKey(), new AbstractMap.SimpleEntry<>(p.getName(), entry.getValue()));
            stacktraceQueue.add(entryToPut);
        }
        teleportStacktrace.put(p.getName(), new AbstractMap.SimpleEntry<>(System.currentTimeMillis() + 1500, Thread.currentThread().getStackTrace()));
        if (p.hasPermission("muxsystem.fly") == false || deactivatedfly.contains(p.getUniqueId())) return;
        runLater(new BukkitRunnable() {
            @Override
            public void run() {
                if (p.isOnline() == false) return;
                if ((inEvent(p) || inDuel(p) || in1vs1(p) || inGame(p) || inWar(p))) return;
                p.setAllowFlight(true);
                p.setFlying(true);
                final String pname = p.getName();
                godplayers.add(pname);
                globalfly.add(pname);
            }
        }, 1L);
    }

    @EventHandler
    public void onTeleport(final PlayerTeleportEvent e) {
        final Player p = e.getPlayer();
        final Location l = e.getTo();
        if (p.getGameMode() == GameMode.SPECTATOR && p.isOp() == false) {
            p.setSpectatorTarget(p);
        }
        if (l == null) return;
        else if (e.getCause() == TeleportCause.SPECTATE) {
            final Location ploc = p.getLocation();
            final Event ev = events.getCurrent();
            if (ev != null && ev.getSpectators().containsKey(p.getUniqueId())) {
                final double distance = ploc.getWorld().equals(l.getWorld()) ? ploc.distance(l) : 9999D;
                if (distance <= ev.getMaxSpectatorDistance()) return;
            }
            e.setCancelled(true);
            return;
        }
        final double x = l.getX(), z = l.getZ();
        if (x > worldlimit || z > worldlimit || x < -worldlimit || z < -worldlimit) {
            e.setCancelled(true);
            showItemBar(p, "§c§lDu hast das Limit der Welt erreicht.");
            return;
        }
        if (p.hasPermission("muxsystem.team")) anticheat.removeCPS(p);
        if (p.getGameMode() == GameMode.SPECTATOR && p.hasMetadata("muxspectatetp") == false) {
            if (e.getCause() == TeleportCause.UNKNOWN) {
                e.setCancelled(true);
            } else if (e.getCause() != TeleportCause.END_PORTAL || p.getLocation().getWorld() != l.getWorld()) {
                p.setGameMode(GameMode.SURVIVAL);
            }
        } else if (casino.inCasino(p) && l.getWorld().getName().equals("MuxCasino") == false) {
            casino.handleQuit(p, false);
        } else if (scoreboard.getCurrent(p) == shopsb && (l.getWorld().getName().equals(shop.getShopLocation().getWorld().getName()) == false ||
                shopregion.contains(e.getTo().getBlockX(), e.getTo().getBlockY(), e.getTo().getBlockZ()) == false)) {
            resetScoreboard(p);
        } else if (scoreboard.getCurrent(p) != shopsb && l.getWorld().getName().equals(shop.getShopLocation().getWorld().getName()) &&
                shopregion.contains(e.getTo().getBlockX(), e.getTo().getBlockY(), e.getTo().getBlockZ())) {
            sendScoreboard(p, shopsb);
        }
        pets.handleTeleport(p);
        mounts.handleTeleport(p, e.getCause());
    }

    @EventHandler
    public void onPortal(final PlayerPortalEvent e) {
        final Player p = e.getPlayer();
        final Location l = e.getTo();
        if (l == null) {
            final Location f = e.getFrom();
            if (f != null && f.getWorld().getName().equals(gamespawn.getWorld().getName())) {
                teleportToNether(e, p);
            }
            return;
        } else if (e.getCause() == TeleportCause.END_PORTAL) {
            e.setCancelled(true);
            return;
        }
        final double x = l.getX(), z = l.getZ();
        if (x > worldlimit || z > worldlimit || x < -worldlimit || z < -worldlimit) {
            e.setCancelled(true);
            showItemBar(e.getPlayer(), "§c§lDu hast das Limit der Welt erreicht.");
            return;
        }
        e.getPortalTravelAgent().setCanCreatePortal(false);
        teleportToNether(e, p);
    }

    private void teleportToNether(final PlayerPortalEvent e, final Player p) {
        e.setTo(netherspawn.clone());
        newbies.handleNewbieNether(p);
        pets.handleTeleport(p);
        mounts.handleTeleport(p, e.getCause());
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(final FoodLevelChangeEvent e) {
        final Player p = (Player) e.getEntity();
        if (perks.hasNoHunger(p.getName())) {
            e.setFoodLevel(20);
            p.setSaturation(20F);
        } else if (e.getFoodLevel() < p.getFoodLevel()) {
            if (random.nextInt(10) > 3) {
                e.setCancelled(true);
            } else if (e.getFoodLevel() < 3) {
                if (inEvent(p) == false)
                    chatClickHoverRun(p, getLang("hunger"), getLang("hungerhover"), "/warp essen");
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEat(final PlayerItemConsumeEvent e) {
        final Player p = e.getPlayer();
        if (e.getItem().getType() == Material.GOLDEN_APPLE) {
            final Location l = p.getLocation();
            if (bans.isRecentPvPBan(p.getName())) {
                e.setCancelled(true);
                showItemBar(p, "§cDu kannst derzeit keinen OP-Apfel essen.");
            } else if (in1vs1(p) == false && WGBukkit.getRegionManager(l.getWorld()).getApplicableRegions(l).allows(DefaultFlag.PISTONS) == false) {
                e.setCancelled(true);
                showItemBar(p, getLang("noops"));
            }
        } else if (e.getItem().getType() == Material.MILK_BUCKET) {
            perks.activatePerks(p);
        } else if (e.getItem().getType() == Material.POTION && (inGame(p) || inCasino(p))) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemConsume(final PlayerItemConsumeEvent e) {
        if (e.getItem().getType() == Material.GOLDEN_APPLE && e.getItem().getDurability() == (short) 1) {
            e.setCancelled(true);
            final Player p = e.getPlayer();
            p.setFoodLevel(p.getFoodLevel() + 4);
            if (e.getItem().getAmount() == 1) {
                p.getInventory().remove(e.getItem());
            } else {
                p.getItemInHand().setAmount(p.getItemInHand().getAmount() - 1);
            }
            p.updateInventory();
            for (final PotionEffect potionEffect : p.getActivePotionEffects()) {
                if (potionEffect.getType().getName().equals("FIRE_RESISTANCE") && potionEffect.getDuration() < (20 * 60 * 5)) {
                    p.removePotionEffect(potionEffect.getType());
                } else if (potionEffect.getType().getName().equals("ABSORPTION") && potionEffect.getAmplifier() <= 3 && potionEffect.getDuration() < (20 * 60 * 2)) {
                    p.removePotionEffect(potionEffect.getType());
                } else if (potionEffect.getType().getName().equals("REGENERATION") && potionEffect.getAmplifier() <= 4 && potionEffect.getDuration() < (20 * 30)) {
                    p.removePotionEffect(potionEffect.getType());
                }
            }
            p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 20 * 60 * 5, 0), false); // 5 min fire res
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 60 * 2, 0), false); // 2 min absoption
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 30, 4), false); // 20 sec regi
            p.playSound(p.getLocation(), Sound.BURP, 20, 20);
        }
    }

    @EventHandler
    public void onExp(final PlayerExpChangeEvent e) {
        short mult = 1;
        if (perks.hasDoubleEXP(e.getPlayer().getName())) {
            mult += 1;
        }
        if (booster.hasBooster(e.getPlayer(), BoosterType.XP)) {
            mult += 5;
        }
        mult += events.getExpBoost();
        e.setAmount(e.getAmount() * mult);
    }

    @EventHandler
    public void onRespawn(final PlayerRespawnEvent e) {
        e.setRespawnLocation(gamespawn);
        perks.activatePerks(e.getPlayer());
        menu.addMenuItem(e.getPlayer(), true);
        if (admin.BOSSBAR.isActive()) {
            bossbar.removePlayer(e.getPlayer());
            new BukkitRunnable() {
                @Override
                public void run() {
                    bossbar.addPlayer(e.getPlayer());
                }
            }.runTaskLater(this, 2L);
        }
    }

    @EventHandler
    public void onPing(final ServerListPingEvent e) {
        e.setMotd("                          §7§lMux§a§lCraft\n                §6§lWartungsarbeiten");
    }

    @EventHandler
    public void onKick(final PlayerKickEvent e) {
        if (e.getReason().contains("disconnect.spam")) {
            e.setCancelled(true);
        }
        e.setLeaveMessage(null);
    }

    private void startTimers() {
        final Server sr = this.getServer();
        new BukkitRunnable() {
            @Override
            public void run() {
                gold.sendParticles();
            }
        }.runTaskTimer(this, 10L, 2L);
        new BukkitRunnable() {
            boolean slowercheck = true; // Slower Check

            @Override
            public void run() {
                if (admin.BOSSBAR.isActive()) {
                    bossbar.sendBossBar();
                }
                sendSpiralParticles();
                menu.checkForcefield();
                if (slowercheck ^= true) {
                    stats.sendParticles();
                    antilags.mergeMobs(sr);
                    for (final Player p : sr.getOnlinePlayers()) {
                        final Location l = p.getLocation();
                        final ApplicableRegionSet regions = WGBukkit.getRegionManager(l.getWorld()).getApplicableRegions(l);
                        menu.switchItem(p, (l.getWorld().getName().equals(gamespawn.getWorld().getName()) && (spawnregion != null && spawnregion.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) == false && (fullspawnregion != null && fullspawnregion.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())))
                                || inDuel(p) || inWar(p) || (l.getWorld().getEnvironment() == Environment.THE_END && regions.allows(DefaultFlag.PVP) && endregion != null && endregion.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) || regions.allows(DefaultFlag.MYCELIUM_SPREAD) == false);
                        if (p.isBlocking() && checkGeneralCooldown(p.getName(), "SWORDFIX", 5000L, true) == false && inGame(p) == false) {
                            final PlayerInventory pi = p.getInventory();
                            final Set<String> cmds = regions.getFlag(DefaultFlag.BLOCKED_CMDS);
                            boolean ifix = false;
                            if (cmds != null) {
                                for (final String s : cmds) {
                                    if (s.contains("/repair")) {
                                        ifix = true;
                                        break;
                                    }
                                }
                            }
                            if (hasVoted(p) == false && tempfix.contains(p.getUniqueId()) == false && inEvent(p) == false) {
                                sendNoVoteMessage(p);
                                continue;
                            }
                            final boolean hasgold = hasGold(p.getUniqueId());
                            final short count = (short) (repairItems(pi.getContents(), hasgold) + (ifix == false ? repairItems(pi.getArmorContents(), hasgold) : 0));
                            if (count == 0) continue;
                            p.playSound(p.getLocation(), Sound.ANVIL_USE, 1F, 2F);
                            showItemBar(p, getLang("cmd.fixitems"));
                        }
                    }
                }
            }
        }.runTaskTimer(this, 10L, 5L);
        new BukkitRunnable() {
            @Override
            public void run() {
                tips.showTips();
            }
        }.runTaskTimerAsynchronously(this, 500L, 20L);
        new BukkitRunnable() {
            @Override
            public void run() {
                booster.checkBooster();
                creators.checkFlying();
                customrank.checkFlying();
                events.checkSpectators();
                events.updateHologram();
                casino.updateHologram();
                chairs.clearEmpty();
                support.handleExpectedSupporters();
                handleNotImportantSyncActions();
                mining.clearHolos();
                perms.checkWhitelist();
                anticheat.checkDelayedBans();
                /* NOCH NICHT ENTFERNEN IST ZUM TESTEN
                getServer().getOnlinePlayers().forEach(player -> {
                    sendScoreboard(player, shopsb);
                    resetScoreboard(player);
                    sendScoreboard(player);
                });

                 */
            }
        }.runTaskTimer(this, 60L, 20L);
        new BukkitRunnable() {
            @Override
            public void run() {
                resetLimit("CHATSPAM");
            }
        }.runTaskTimer(this, 1200L, 340L);
        new BukkitRunnable() {
            @Override
            public void run() {
                payments.checkPayments();
                games.checkTime();
                clanwar.checkTime();
                perms.checkWorldEdit();
                perms.checkWorldGuard();
                npcs.respawn();
                casino.updateTop();
                stats.updateTop();
                resetLimit("STOP");
                chat.checkIgnoreList();
                if (events.isRunning() == false) giants.respawn();
                perms.checkExpiringRanks();
                base.cleanupChunks();
            }
        }.runTaskTimer(this, 600L, 1200L);
        new BukkitRunnable() {
            int save = 0;

            @Override
            public void run() {
                antibot.clearLastChat();
                resetLimit("CHATCLEAR");
                sr.savePlayers();
                if (save == 0) {
                    farmworld.save();
                    endspawn.getWorld().save();
                    save++;
                } else if (save == 1) {
                    base.getWorld().save();
                    casino.getWorld().save();
                    save++;
                } else {
                    sr.getWorlds().forEach(w -> {
                        final String wname = w.getName();
                        if (wname.equals(farmworld.getName()) == false && wname.equals(endspawn.getWorld().getName()) == false && wname.equals(base.getWorld().getName()) == false
                                && wname.equals(casino.getWorld().getName()) == false && wname.contains("Varo") == false)
                            w.save();
                    });
                    save = 0;
                }
                support.resetFAQ();
                shop.updateTop();
                email.checkMails();
                chests.refreshTopChests();
                chat.clearSpamList();
                updateTotalActiveCoins();
                forkJoinPool.execute(() -> db.updateGlobalTrophyCache());
            }
        }.runTaskTimer(this, 600L, 3600L);
        new BukkitRunnable() {
            boolean slowercheck = true;

            @Override
            public void run() {
                removeEntities(EntType.ARROW, gamespawn.getWorld(), null, -1);
                removeEntities(EntType.ARROW, farmworld, null, -1);
                shop.saveLargeTransactions();
                resetLimit("MINEBOT");
                if (slowercheck ^= true) resetLimit("PSCADD");
            }
        }.runTaskTimer(this, 2400L, 18000L);
        new BukkitRunnable() {
            boolean setup = false;
            @Override
            public void run() {
                users.values().forEach(db::savePlayer);
                db.saveLiquidity();
                casino.save();
                if (admin.AUTOBROADCASTS.isActive() == false) {
                    return;
                }
                broadcastAdvertisement("", Priority.NORMAL);
                final int r = random.nextInt(6);
                if (r == 0) {
                    broadcastAdvertisement(getLang("broadcast.ranks1"), Priority.NORMAL);
                    broadcastAdvertisement(getLang("broadcast.ranks2"), Priority.NORMAL);
                } else if (r == 1) {
                    broadcastAdvertisement(getLang("broadcast.forum1"), Priority.NORMAL);
                    broadcastAdvertisement(getLang("broadcast.forum2"), Priority.NORMAL);
                } else if (r == 2) {
                    broadcastAdvertisement(getLang("broadcast.video1"), Priority.NORMAL);
                    broadcastAdvertisement(getLang("broadcast.video2"), Priority.NORMAL);
                } else if (r == 3) {
                    broadcastAdvertisement("                      §b§lVIP§f, §e§lELITE§f, §8§lEPIC§f, §c§lULTRA", Priority.NORMAL);
                    broadcastAdvertisement(getLang("broadcast.ranks2"), Priority.NORMAL);
                } else if (r == 4) {
                    broadcastAdvertisement(getLang("broadcast.affil1"), Priority.NORMAL);
                    broadcastAdvertisement(getLang("broadcast.affil2"), Priority.NORMAL);
                } else {
                    broadcastAdvertisement(getLang("broadcast.sell1"), Priority.NORMAL);
                    broadcastAdvertisement(getLang("broadcast.sell2"), Priority.NORMAL);
                }
                broadcastAdvertisement(" ", Priority.NORMAL);

                if (stacktraceQueue.isEmpty() == false) {
                    File file = new File(getDataFolder(), "teleport-stacktrace.txt"); // fix sonst wurd die file immer neu geschrieben
                    if (setup == false) {
                        if (file.exists() == false) {
                            try {
                                file.createNewFile();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        setup = true;
                    }
                    try (FileWriter fileWriter = new FileWriter(file)) {
                        final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy 'um' HH:mm");
                        while (stacktraceQueue.isEmpty() == false) {
                            Entry<Long, Entry<String, StackTraceElement[]>> elements = stacktraceQueue.remove();
                            fileWriter.write(sdf.format(new Date(elements.getKey())));
                            fileWriter.write(" " + elements.getValue().getKey() + "\n");
                            for (StackTraceElement stackTraceElement : elements.getValue().getValue()) {
                                fileWriter.write(String.valueOf(stackTraceElement));
                                fileWriter.write("\n");
                            }
                            fileWriter.write("\n\n");
                        }
                        fileWriter.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }.runTaskTimerAsynchronously(this, 600L, 1600L);
        new BukkitRunnable() {
            @Override
            public void run() {
                pets.updatePets();
                mounts.updateMounts();
                shop.pushAway();
                shop.updateLiquidity();
                admin.resetCurrentLocations();
                sr.getOnlinePlayers().forEach(p -> {
                    final Location l = p.getLocation();
                    final boolean fighting = inBattle(p.getName(), l);
                    final PlayerBase pb = base.getCurrentLocations().get(p);
                    if (fighting == false && scoreboard.getCurrent(p) == pvpsb)
                        resetScoreboard(p);
                    if (p.getWorld().getName().equals(shop.getShopLocation().getWorld().getName()) && scoreboard.getCurrent(p) != shopsb && shopregion.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) {
                        sendScoreboard(p, shopsb);
                    }
                    String locatedin = findLocation(sr, p, l, pb, fighting);
                    final String wname = l.getWorld().getName();
                    final MuxUser u = getMuxUser(p.getName());
                    final Map<String, Home> hs = u == null ? null : u.getHomes();
                    double distance, lowestdistance = 999D;
                    if (hs != null && pb == null) {
                        for (final Entry<String, Home> entry : hs.entrySet()) {
                            final Home h = entry.getValue();
                            final Location homel = entry.getValue().getLocation();
                            if (homel != null && h.getWorld().equals(wname) && (distance = homel.distance(l)) < 100D) {
                                if (hs.size() == 1) locatedin = "bei §adeinem Zuhause";
                                else if (distance < lowestdistance) {
                                    lowestdistance = distance;
                                    locatedin = "bei deinem Zuhause §a" + entry.getKey();
                                }
                            }
                        }
                    }
                    showTabList(p, "\n§7§lMux§a§lCraft" + (isBeta() ? " §3§lbeta" : "") + "§r\n§7Du befindest dich derzeit " + locatedin + "§7.\n",
                            "\n§7 Besuche die §aoffizielle Webseite §7für unseren Shop, Discord und vieles mehr: \n§awww.muxcraft.eu\n");
                });
            }
        }.runTaskTimer(this, 20L, 60L);
        new BukkitRunnable() {
            final Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("CET"));
            private short i = 0, lasthour = (short) cal.get(Calendar.HOUR_OF_DAY);

            @Override
            public void run() {
                final Collection<? extends Player> online = sr.getOnlinePlayers();
                final int pcount = online.size() + getBungeeUsers();
                final String optitle = sr.hasWhitelist() ? "§6§lWartungsarbeiten" :
                        "§fEs sind " + (pcount >= getSlots() ? "§c" : "§6") + pcount + " / " + getSlots() + " §fSpieler auf " + getServerName() + " online.";
                String temp = "";
                if (messages[0] != null) {
                    if (messages[message] == null) {
                        message = 0;
                    }
                    temp = messages[message];
                    pets.setWitherNames(messages[message]);
                    message++;
                }
                final String m = temp;
                online.forEach(p -> {
                    if (p.getGameMode() == GameMode.SPECTATOR) return;
                    final Location l = p.getLocation();
                    final double x = l.getX(), z = l.getZ();
                    final World w = l.getWorld();
                    if (w.equals(gamespawn.getWorld()) && (x < -250D || z > 340D || z < -260D || x > 350D)) {
                        if (checkGeneralCooldown(p.getName(), "SPAWNBORDER", 60000L, false) == false) {
                            chatClickHoverRun(p, "§a§lWildnis>§7 Klicke §ahier§7, um zur Wildnis zu gelangen.", "§a§oZur Wildnis", "/rtp");
                            checkGeneralCooldown(p.getName(), "SPAWNBORDER", 60000L, true);
                        }
                    }
                    final int limit = w.equals(gamespawn.getWorld()) || w.getName().equals("MuxCasino") ? 400 : (w.equals(getWarpsWorld()) || w.getEnvironment() == Environment.THE_END) ? warpworldlimit : worldlimit;
                    if (x > limit || x < -limit || z > limit || z < -limit) {
                        runLater(new BukkitRunnable() {
                            @Override
                            public void run() {
                                w.playEffect(l, Effect.ENDER_SIGNAL, 0);
                                w.playEffect(l, Effect.ENDER_SIGNAL, 0);
                                w.playEffect(l, Effect.SMOKE, 4);
                                w.playEffect(l, Effect.SMOKE, 4);
                                w.playEffect(l, Effect.SMOKE, 4);
                                w.playEffect(l, Effect.GHAST_SHOOT, 0);
                                if (p.isInsideVehicle()) {
                                    final Entity v = p.getVehicle();
                                    p.leaveVehicle();
                                    v.remove();
                                }
                                double syncx = x, syncz = z;
                                if (x <= -limit) {
                                    syncx = -limit + 10;
                                } else if (x >= limit) {
                                    syncx = limit - 10;
                                }
                                if (z <= -limit) {
                                    syncz = -limit + 10;
                                } else if (z >= limit) {
                                    syncz = limit - 10;
                                }
                                showItemBar(p, "§c§lDu hast das Limit der Welt erreicht.");
                                p.teleport(new Location(w, syncx, w.getHighestBlockYAt((int) syncx, (int) syncz) + 0.5D, syncz));
                            }
                        }, 0L);
                    }
                    checkAFK(p, l);
                    final boolean afk = p.hasPermission("muxsystem.team") && team.isAFK(p.getName());
                    final String smsg = p.hasPermission("muxsystem.team") ? support.getSupportMSG(p.isOp()) : null;
                    final boolean supports = p.hasPermission("muxsystem.team") && smsg != null;
                    bossbar.setTitle(p, creators.isRecording(p) || inBattle(p.getName(), l) ? "§f§lServer IP: §a§lmuxcraft.eu" : p.isOp() ? optitle : afk ? "§e§lAbwesend" : supports ? smsg : messages[0] != null ? m : "");
                });
                chests.pushAway();
                advent.pushAway();
                polls.checkTime();
                warps.checkNoOP();
                pvp.checkTime();
                chat.checkTime();
                clanwar.checkSpectators();
                base.checkVisitors();
                pvp.checkSpectators();
                newbies.removeExpiredProtection();
                bans.removeExpired();
                bans.removeRecentPvPBanMutes();
                shop.updateLast24HoursValue();
                switch (i) {
                    case 3:
                        //checkMojang();
                        events.checkAutoEvent();
                        i++;
                        break;
                    case 6:
                        final Calendar cal2 = Calendar.getInstance(TimeZone.getTimeZone("CET"));
                        final short dayofweek = (short) cal2.get(Calendar.DAY_OF_WEEK), thishour = (short) cal2.get(Calendar.HOUR_OF_DAY);
                        if (thishour != lasthour)
                            shop.saveLastHour();
                        if (thishour < lasthour) {
                            shop.resetLimits();
                            casino.reset(dayofweek == Calendar.MONDAY);
                            if (shared.isDaemon())
                                votes.reset();
                            reports.clearReports();
                            final short day = (short) cal2.get(Calendar.DAY_OF_MONTH);
                            advent.nextDay(day);
                            analytics.saveTodayAnalytics(true);
                            // Reload NPCs & Holograms
                            admin.resetHolograms();
                            season.refreshHolograms();
                            if (day == 1) {
                                db.resetSupports();
                                team.resetPlayTime();
                                db.deleteShopPrices(System.currentTimeMillis() - 2678400000L);
                            }
                        } else if (thishour == 5 && lasthour == 4) { // Restart at 5
                            runLater(new BukkitRunnable() {
                                @Override
                                public void run() {
                                    logoutAll("CONSOLE", "Neustart");
                                    syncCMD("stop");
                                }
                            }, 1L);
                        } else if (thishour == 18 && lasthour == 17) {
                            casino.announceCasinoOpening();
                        }
                        lasthour = thishour;
                        i++;
                        break;
                    default:
                        i++;
                        break;
                }
                if (i > 6) {
                    i = 0;
                }
            }
        }.runTaskTimerAsynchronously(this, 0L, 200L);
    }

    private String findLocation(final Server sr, final Player p, final Location l, final PlayerBase pb, final boolean fighting) {
        final String wname = l.getWorld().getName();
        String locatedin;
        if (pb != null) {
            final UUID buuid = pb.getOwner();
            locatedin = "in " + (buuid.equals(p.getUniqueId()) ? "§adeiner Base" : pb.isGriefable() ? "einer §cgriefbaren Base" : ("der §aBase von " + sr.getOfflinePlayer(buuid).getName()));
        } else if (inGame(p)) {
            locatedin = "im §6Training";
        } else if (inEvent(p)) {
            locatedin = "im §dEvent";
        } else if (inCasino(p)) {
            locatedin = "im §5Casino";
        } else if (in1vs1(p)) {
            locatedin = "im §c1vs1";
        } else if (pvp.inDuel(p)) {
            //final boolean friendly = pvp.getArena(p).isFriendly();
            locatedin = "im §6Freundschaftsduell";
        } else if (inWar(p)) {
            locatedin = "im §3Clanwar";
        } else if (p.getGameMode() == GameMode.SPECTATOR) {
            locatedin = "im §fZuschauermodus";
        } else if (fighting) {
            locatedin = "im §cKampf";
        } else if (wname.contains("the_end")) {
            locatedin = "im §aEnd";
        } else if (wname.contains("nether")) {
            locatedin = "im §4Nether";
        } else if (wname.contains("warps")) {
            locatedin = "bei §aeinem Warp";
            if (shopregion != null && shopregion.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) {
                locatedin = "im §eShop";
            }
        } else if (spawnregion != null && spawnregion.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) {
            locatedin = "am §aSpawn";
        } else if (fullspawnregion != null && fullspawnregion.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) {
            locatedin = "im §aPvP Bereich";
        } else {
            locatedin = "in der §aWildnis";
        }
        if (locatedin.contains("Kampf")) {
            final String locatedreal = findLocation(sr, p, l, pb, false);
            admin.addCurrentLocation(locatedreal);
        } else {
            admin.addCurrentLocation(locatedin);
        }
        return locatedin;
    }

    private void updateTotalActiveCoins() {
        new BukkitRunnable() {
            @Override
            public void run() {
                activeMuxCoinsSupply = db.getTotalActiveCoins();
                activeChipsSupply = db.getTotalActiveChips();
            }
        }.runTaskAsynchronously(this);
    }

    public void loadConfigStuff(final FileConfiguration config) {
        language.loadConfig(this, config);
        pvp.loadConfig(config);
        votes.loadConfig(config);
        bossbar.loadConfig(config);
        season.loadConfig(config);
        // LANGUAGE
        hnotfound = getLang("notfound");
        hnotonline = getLang("notonline");
        hnotinfight = getLang("notinfight");
        hnonumber = getLang("nonumber");
        joinmsg = ChatColor.translateAlternateColorCodes('&', config.getString("joinmsg"));
        slots = (short) config.getInt("slots");
        debug = config.getBoolean("debug");
        worldlimit = config.getInt("worldlimit");
        warpworldlimit = config.getInt("warpworldlimit");
        setSlots(slots);
        newstitle = config.getString("news.title");
        newstext = config.getString("news.text");
        newslink = config.getString("news.link");
        newslinktext = config.getString("news.linktext");
        // SPECIAL PERMISSIONS
        fulltrusted.clear();
        trusted.clear();
        wgtrusted.clear();
        wetrusted.clear();
        trusted.addAll(config.getStringList("trusted"));
        fulltrusted.addAll(config.getStringList("fulltrusted"));
        wgtrusted.addAll(config.getStringList("worldguard"));
        wetrusted.addAll(config.getStringList("worldedit"));
        byte msize = 0;
        Arrays.fill(messages, null);
        for (final String s : config.getStringList("automessages")) {
            messages[msize] = ChatColor.translateAlternateColorCodes('&', s.length() > 63 ? s.substring(0, 63) : s);
            msize++;
        }
    }

    private void checkMojang() {
        String[] inputLine = null;
        try {
            final HttpURLConnection urlConnection = (HttpURLConnection) new URL("https://muxcraft.eu/status.php").openConnection();

            urlConnection.setConnectTimeout(4000);
            urlConnection.setReadTimeout(4000);
            urlConnection.addRequestProperty("User-Agent", "MuxSystem/" + this.version);

            try (final java.io.InputStream is = urlConnection.getInputStream();
                 final java.io.InputStreamReader isr = new java.io.InputStreamReader(is, StandardCharsets.UTF_8);
                 final java.io.BufferedReader in = new java.io.BufferedReader(isr)) {
                final String s = in.readLine();
                if (s != null) inputLine = s.split(";");
            }
        } catch (final IOException ignored) {
        }

        if (inputLine == null || inputLine.length != 2) {
            System.out.println("[Mojang] No answer while contacting the servers.");
            return;
        }

        if (inputLine[0].equals("OK")) session = true;
        else {
            if (session) broadcastMessage(getLang("sessionserver"), null, Priority.HIGH);
            session = false;
        }

        if (inputLine[1].equals("OK")) auth = true;
        else {
            if (auth) broadcastMessage(getLang("loginserver"), null, Priority.HIGH);
            auth = false;
        }
    }

    private void cancelTP(final Player p) {
        if (tpplayers.remove(p.getName())) {
            this.getServer().getScheduler().cancelTask(tptasks.remove(p.getName()));
            showItemBar(p, getLang("tpcancelled"));
            history.getTeleportCache().remove(p);
        }
    }

    public void addHistory(final UUID uuid, final UUID uuid2, final String type, final String entry, final String param) {
        history.addHistory(uuid, uuid2, type, entry, param);
    }

    public void heal(final Player p) {
        p.setHealth(20D);
        p.setFoodLevel(20);
        p.setFireTicks(0);
        p.setSaturation(10);
        for (final PotionEffect pe : p.getActivePotionEffects()) {
            if (pe.getType() == PotionEffectType.INVISIBILITY && isVanish(p)) continue;
            p.removePotionEffect(pe.getType());
        }
        p.playSound(p.getLocation(), Sound.ZOMBIE_UNFECT, 1F, 1F);
        playEffect(EnumParticle.HEART, p.getLocation(), 1, 1, 1, 0, 5);
        perks.activatePerks(p);
    }

    private void errorDisable(final String reason) {
        errordisable = true;
        System.out.println("[]===================================[]");
        System.out.println(" ERROR ENABLING");
        System.out.println(" Reason: " + reason);
        System.out.println("[]===================================[]");
        this.setEnabled(false);
    }

    public String getSelection(final String[] options, final String activecolor, final int selected) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < options.length; i++) {
            String color = (i == selected ? activecolor : "§7") + "§l";
            sb.append(color).append(options[i]);
            if (i < options.length - 1) {
                sb.append("§r§8 | ");
            }
        }
        return sb.toString();
    }

    public void resetCooldown(final String name, final String type) {
        final HashMap<String, Long> map = cooldowns.get(name);
        if (map == null) return;
        else map.remove(type);
        cooldowns.put(name, map);
    }

    public void saveHashFile() {
        try {
            hashYML.save(hashfile);
        } catch (final IOException e) {
            System.err.println("MuxSystem> Error while saving the file hashmaps.yml!");
            e.printStackTrace();
        }
    }

    public int isVPN(final String ip) {
        return antibot.isVPN(ip);
    }

    private boolean tpa(final Player p, final Player victim, final String typ) {
        if (victim == null || p.canSee(victim) == false) {
            showItemBar(p, hnotonline);
            return true;
        } else if (hasVoted(p) == false) {
            sendNoVoteMessage(p);
            return true;
        } else if (victim.getName().equals(p.getName())) {
            showItemBar(p, "§cDu kannst keine Anfrage an dich selbst schicken!");
            return true;
        } else if (checkGeneralCooldown(p.getName(), "TPA", 30000L, true)) {
            showItemBar(p, getLang("notsofast"));
            return true;
        } else if (inGame(victim)) {
            showItemBar(p, "§cDieser Spieler spielt gerade ein Minigame.");
            return true;
        } else if (inWar(victim)) {
            showItemBar(p, "§cDieser Spieler ist in einem Clanwar.");
            return true;
        } else if (in1vs1(victim)) {
            showItemBar(p, "§cDieser Spieler ist in einem 1vs1.");
            return true;
        } else if (inDuel(victim)) {
            showItemBar(p, "§cDieser Spieler ist in einem Duell.");
            return true;
        } else if (inCasino(victim)) {
            showItemBar(p, "§cDieser Spieler ist gerade im Casino.");
            return true;
        } else if (inEvent(victim)) {
            showItemBar(p, "§cDieser Spieler ist gerade in einem Event.");
            return true;
        } else if (p.getGameMode() == GameMode.SPECTATOR) {
            showItemBar(p, "§cDu bist im Zuschauermodus.");
            return true;
        } else if (creators.isRecording(victim)) {
            showItemBar(p, "§cDer Spieler kann gerade keine Anfragen erhalten.");
            return true;
        } else if (chat.isIgnoring(victim, p)) {
            showItemBar(p, getLang("cmd.msgignoringyou"));
            return true;
        }
        final HashMap<String, String> map = new HashMap<>();
        map.put(typ, p.getName());
        tpa.put(victim.getName(), map);
        new BukkitRunnable() {
            final String vname = victim.getName();

            @Override
            public void run() {
                final Map<String, String> map2 = tpa.get(vname);
                if (map2 != null && map2.equals(map)) {
                    tpa.remove(vname);
                }
            }
        }.runTaskLater(this, 2400L);
        showItemBar(p, "§fDie Anfrage an §6" + victim.getName() + " §fwurde §agesendet§f.");
        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
        chatClickHoverRun(p, "§7Klicke §6hier§7, um die Teleportierungsanfrage zurückzuziehen.", "§6§oKlicke um abzubrechen.", "/tpacancel");
        return false;
    }

    private void playSmokeEffect(final Player p) {
        final Location l = p.getLocation();
        final World w = p.getWorld();
        for (byte i = 0; i < 3; i++) {
            w.playEffect(l, Effect.ENDER_SIGNAL, 0);
            w.playEffect(l, Effect.ENDER_SIGNAL, 0);
            l.add(0D, 1D, 0D);
        }
    }

    public boolean isRenamedByPlayer(final ItemStack item) {
        final ItemMeta im = item.getItemMeta();
        if (im != null && im.hasDisplayName()) {
            final String name = im.getDisplayName().toLowerCase();
            return name.contains("§") == false;
        }
        return false;
    }

    public DyeColor getColorFromChatColor(final String color) {
        final ChatColor chatColor = ChatColor.getByChar(color);
        return getDyeColor(chatColor);
    }

    public boolean isArmor(final Material m) {
        switch (m) {
            case DIAMOND_HELMET:
            case GOLD_HELMET:
            case IRON_HELMET:
            case CHAINMAIL_HELMET:
            case LEATHER_HELMET:
            case DIAMOND_CHESTPLATE:
            case GOLD_CHESTPLATE:
            case IRON_CHESTPLATE:
            case CHAINMAIL_CHESTPLATE:
            case LEATHER_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case GOLD_LEGGINGS:
            case IRON_LEGGINGS:
            case CHAINMAIL_LEGGINGS:
            case LEATHER_LEGGINGS:
            case DIAMOND_BOOTS:
            case GOLD_BOOTS:
            case IRON_BOOTS:
            case CHAINMAIL_BOOTS:
            case LEATHER_BOOTS:
                return true;
            default:
                return false;
        }
    }

    public boolean isTool(final Material m) {
        final String tool = m.name();
        return tool.endsWith("AXE") || tool.endsWith("SPADE");
    }

    private Enchantment getEnchByName(final String name) {
        Enchantment enchantment = Enchantment.getByName(name.toUpperCase());
        if (enchantment == null) {
            enchantment = enchantments.get(name.toLowerCase());
        }
        return enchantment;
    }

    private void setup() {
        enchantments.put("alldamage", Enchantment.DAMAGE_ALL);
        enchantments.put("alldmg", Enchantment.DAMAGE_ALL);
        enchantments.put("sharpness", Enchantment.DAMAGE_ALL);
        enchantments.put("sharp", Enchantment.DAMAGE_ALL);
        enchantments.put("arthropodsdamage", Enchantment.DAMAGE_ARTHROPODS);
        enchantments.put("ardmg", Enchantment.DAMAGE_ARTHROPODS);
        enchantments.put("baneofarthropods", Enchantment.DAMAGE_ARTHROPODS);
        enchantments.put("baneofarthropod", Enchantment.DAMAGE_ARTHROPODS);
        enchantments.put("arthropod", Enchantment.DAMAGE_ARTHROPODS);
        enchantments.put("undeaddamage", Enchantment.DAMAGE_UNDEAD);
        enchantments.put("smite", Enchantment.DAMAGE_UNDEAD);
        enchantments.put("digspeed", Enchantment.DIG_SPEED);
        enchantments.put("efficiency", Enchantment.DIG_SPEED);
        enchantments.put("minespeed", Enchantment.DIG_SPEED);
        enchantments.put("cutspeed", Enchantment.DIG_SPEED);
        enchantments.put("durability", Enchantment.DURABILITY);
        enchantments.put("dura", Enchantment.DURABILITY);
        enchantments.put("unbreaking", Enchantment.DURABILITY);
        enchantments.put("thorns", Enchantment.THORNS);
        enchantments.put("thorn", Enchantment.THORNS);
        enchantments.put("fireaspect", Enchantment.FIRE_ASPECT);
        enchantments.put("fire", Enchantment.FIRE_ASPECT);
        enchantments.put("meleefire", Enchantment.FIRE_ASPECT);
        enchantments.put("meleeflame", Enchantment.FIRE_ASPECT);
        enchantments.put("knockback", Enchantment.KNOCKBACK);
        enchantments.put("blockslootbonus", Enchantment.LOOT_BONUS_BLOCKS);
        enchantments.put("fortune", Enchantment.LOOT_BONUS_BLOCKS);
        enchantments.put("mobslootbonus", Enchantment.LOOT_BONUS_MOBS);
        enchantments.put("mobloot", Enchantment.LOOT_BONUS_MOBS);
        enchantments.put("looting", Enchantment.LOOT_BONUS_MOBS);
        enchantments.put("oxygen", Enchantment.OXYGEN);
        enchantments.put("respiration", Enchantment.OXYGEN);
        enchantments.put("breathing", Enchantment.OXYGEN);
        enchantments.put("breath", Enchantment.OXYGEN);
        enchantments.put("protection", Enchantment.PROTECTION_ENVIRONMENTAL);
        enchantments.put("prot", Enchantment.PROTECTION_ENVIRONMENTAL);
        enchantments.put("protect", Enchantment.PROTECTION_ENVIRONMENTAL);
        enchantments.put("explosionsprotection", Enchantment.PROTECTION_EXPLOSIONS);
        enchantments.put("explosionprotection", Enchantment.PROTECTION_EXPLOSIONS);
        enchantments.put("expprot", Enchantment.PROTECTION_EXPLOSIONS);
        enchantments.put("blastprotection", Enchantment.PROTECTION_EXPLOSIONS);
        enchantments.put("blastprotect", Enchantment.PROTECTION_EXPLOSIONS);
        enchantments.put("fallprotection", Enchantment.PROTECTION_FALL);
        enchantments.put("fallprot", Enchantment.PROTECTION_FALL);
        enchantments.put("featherfall", Enchantment.PROTECTION_FALL);
        enchantments.put("featherfalling", Enchantment.PROTECTION_FALL);
        enchantments.put("fireprotection", Enchantment.PROTECTION_FIRE);
        enchantments.put("flameprotection", Enchantment.PROTECTION_FIRE);
        enchantments.put("fireprotect", Enchantment.PROTECTION_FIRE);
        enchantments.put("flameprotect", Enchantment.PROTECTION_FIRE);
        enchantments.put("projectileprotection", Enchantment.PROTECTION_PROJECTILE);
        enchantments.put("projprot", Enchantment.PROTECTION_PROJECTILE);
        enchantments.put("silktouch", Enchantment.SILK_TOUCH);
        enchantments.put("softtouch", Enchantment.SILK_TOUCH);
        enchantments.put("waterworker", Enchantment.WATER_WORKER);
        enchantments.put("aquaaffinity", Enchantment.WATER_WORKER);
        enchantments.put("firearrow", Enchantment.ARROW_FIRE);
        enchantments.put("flame", Enchantment.ARROW_FIRE);
        enchantments.put("flamearrow", Enchantment.ARROW_FIRE);
        enchantments.put("arrowdamage", Enchantment.ARROW_DAMAGE);
        enchantments.put("power", Enchantment.ARROW_DAMAGE);
        enchantments.put("arrowpower", Enchantment.ARROW_DAMAGE);
        enchantments.put("arrowknockback", Enchantment.ARROW_KNOCKBACK);
        enchantments.put("arrowkb", Enchantment.ARROW_KNOCKBACK);
        enchantments.put("punch", Enchantment.ARROW_KNOCKBACK);
        enchantments.put("arrowpunch", Enchantment.ARROW_KNOCKBACK);
        enchantments.put("infinitearrows", Enchantment.ARROW_INFINITE);
        enchantments.put("infarrows", Enchantment.ARROW_INFINITE);
        enchantments.put("infinity", Enchantment.ARROW_INFINITE);
        enchantments.put("infinite", Enchantment.ARROW_INFINITE);
        enchantments.put("unlimited", Enchantment.ARROW_INFINITE);
        enchantments.put("unlimitedarrows", Enchantment.ARROW_INFINITE);
        enchantments.put("luck", Enchantment.LUCK);
        enchantments.put("luckofsea", Enchantment.LUCK);
        enchantments.put("lure", Enchantment.LURE);
        enchantments.put("depthstrider", Enchantment.DEPTH_STRIDER);
        enchantments.put("depth", Enchantment.DEPTH_STRIDER);
        final World w = getGameSpawn().getWorld();
        final Material gm = Arrays.asList(Material.WOOD_SWORD, Material.IRON_SWORD, Material.GOLD_SWORD, Material.DIAMOND_SWORD).get(random.nextInt(4));
        giants.addGiant(new Location(w, 33D, 75D, 11.5D, 90F, 0F), addEnchant(new ItemStack(gm), Enchantment.DAMAGE_ALL, 1));
        giants.addGiant(new Location(w, 31.5D, 75D, -14.5D, 0F, 0F), new ItemStack(Material.POTION, 1, (short) 16421));
        giants.addGiant(new Location(w, -19D, 72.5, 1.5, 90F, 0F), new ItemStack(Material.DOUBLE_PLANT));
        giants.addGiant(new Location(w, -21.8D, 72.5D, 27.05D, -135F, 0F), new ItemStack(Material.EXP_BOTTLE));
        giants.addGiant(new Location(w, -18.8D, 72.5D, -30.05D, -45F, 0F), new ItemStack(Material.RED_MUSHROOM));
        giants.addGiant(new Location(w, -8D, 77.5D, -25D, -90F, 0F), new ItemStack(Material.NETHER_STALK));
        giants.addGiant(new Location(w, -8D, 77D, 21D, -90F, 0F), new ItemStack(Material.NAME_TAG));
        giants.addGiant(new Location(w, 82.5D, 67.5D, 8.5D, 90F, 0F), new ItemStack(Material.GOLDEN_APPLE, 1, (byte) 1));
        giants.addGiant(new Location(w, -70D, 69D, 112.5D, -135F, 0F), new ItemStack(Material.DIAMOND_CHESTPLATE, 1));
        giants.addGiant(new Location(w, -69.85D, 69D, 112.5D, -135F, 0F), new ItemStack(Material.BARRIER, 1));

        giants.addGiant(new Location(getWarpsWorld(), -112D, 20.5D, 421D), new ItemStack(Material.POTION, 1));
        giants.addGiant(new Location(getWarpsWorld(), -77.5D, 20.5D, 421D), new ItemStack(Material.DOUBLE_PLANT, 1, (byte) 5));
        giants.addGiant(new Location(getWarpsWorld(), -115.5D, 20.5D, 443.5D, 180F, 0F), new ItemStack(Material.APPLE, 1));
        giants.addGiant(new Location(getWarpsWorld(), -114.5D, 20.5D, 424D, 0F, 0F), new ItemStack(Material.GOLD_SWORD, 1));
        giants.addGiant(new Location(getWarpsWorld(), -118.5D, 20.5D, 440.5D, -180F, 0F), new ItemStack(Material.STICK, 1));
        giants.addGiant(new Location(getWarpsWorld(), -94.5D, 20.5D, 421.5D), new ItemStack(Material.BRICK, 1));
        giants.addGiant(new Location(getWarpsWorld(), -82.5D, 20.5D, 443.5D, 180F, 0F), new ItemStack(Material.LAVA_BUCKET, 1));
        giants.addGiant(new Location(getWarpsWorld(), -73.5D, 20.5D, 439.5D, 90F, 0F), new ItemStack(Material.REDSTONE, 1));
        giants.addGiant(new Location(getWarpsWorld(), -73.5D, 20.5D, 430D, 90F, 0F), new ItemStack(Material.CARPET, 1, (byte) 1));
        giants.removeOld();
        // SPAWN NPCS
        spawnNPCs();
        // SCOREBOARD
        ScoreboardType standardsb = scoreboard.createScoreboard("§7§lMux§a§lCraft");
        standardsb.setLook((p, board) -> {
            final MuxUser u = getMuxUser(p.getName());
            final UUID uuid = p.getUniqueId();
            final PermissionsGroup group = perms.getGroupOf(uuid);
            standardsb.setLine(board, " ");
            final String liga = stats.getLeague(u.getTrophies(), u.getLastRankedMatch() > System.currentTimeMillis() - 172800000, u.getLastRankedMatch() != 0);
            standardsb.setSection(board, "§b§lTrophäen    ", "§f" + u.getTrophies() + (liga.isEmpty() ? "" : " (" + liga + ")"), true);
            standardsb.setLine(board, "  ");
            final String clan = db.getClanFromPlayer(uuid);
            standardsb.setSection(board, "§3§lClan", clan == null ? "Kein Clan" : clan, true);
            standardsb.setLine(board, "   ");
            standardsb.setSection(board, isBeta() ? "§e§lTestCoins" : "§e§lMuxCoins", getNumberFormat(u.getCoins()), true);
            standardsb.setLine(board, "    ");
            standardsb.setSection(board, getLang("sb.rank"), group == null ? getLang("sb.norank") : group.getName(), true);
        });
        scoreboard.setStandardsb(standardsb);
        shopsb = scoreboard.createScoreboard("§7§lMux§e§lShop");
        shopsb.setLook((p, board) -> {
            final MuxUser u = getMuxUser(p.getName());
            final UUID uuid = p.getUniqueId();
            final PermissionsGroup group = perms.getGroupOf(uuid);
            shopsb.setLine(board, " ");
            // Use white color to allow same value of coins & gems
            shopsb.setSection(board, isBeta() ? "§a§lTestGems    " : "§a§lGems     ", "§f" + getNumberFormat(u.getGems()), true);
            shopsb.setLine(board, "  ");
            shopsb.setSection(board, "§2§lWechselkurs", "1 für " + getNumberFormat(gemshop.getExactCoinExchangeRate(1)), false);
            shopsb.setLine(board, "   ");
            shopsb.setSection(board, isBeta() ? "§e§lTestCoins" : "§e§lMuxCoins", getNumberFormat(u.getCoins()), true);
            shopsb.setLine(board, "    ");
            shopsb.setSection(board, getLang("sb.rank"), group == null ? getLang("sb.norank") : group.getName(), true);
        });
        pvpsb = scoreboard.createScoreboard("§7§lMux§c§lPvP");
        pvpsb.setLook((p, board) -> {
            final MuxUser u = getMuxUser(p.getName());
            pvpsb.setLine(board, " ");
            pvpsb.setSection(board, "§b§lKills         ", getNumberFormat(u.getKills()) + "§7", true);
            pvpsb.setLine(board, "  ");
            int lowestpcnt = 100;
            short armorcount = 0;
            for (final ItemStack armor : p.getInventory().getArmorContents()) {
                if (armor != null && armor.getType() != Material.AIR && isArmor(armor.getType())) {
                    armorcount++;
                    int durability = 100 - (int) (((double) armor.getDurability() / (double) armor.getType().getMaxDurability()) * 100D);
                    if (durability < lowestpcnt) lowestpcnt = durability;
                }
            }
            pvpsb.setSection(board, "§3§lRüstung", (armorcount == 0 ? "Keine" : armorcount < 4 ? "Unvollständig" : "Zustand: " + (lowestpcnt < 15 ? "§c" : "") + lowestpcnt + "%"), false);
            pvpsb.setLine(board, "   ");
            final Player pl = getLastHit(p);
            int opponentkills = 0;
            if (pl != null) {
                final MuxUser u2 = getMuxUser(pl.getName());
                opponentkills = u2.getKills();
            }
            pvpsb.setSection(board, "§4§lGegner", pl == null ? "Keinen" : ("§f" + pl.getName()), false);
            pvpsb.setLine(board, "    ");
            pvpsb.setSection(board, "§6§lSeine Kills", "§f" + getNumberFormat(opponentkills), false);
        });
    }

    private Location ranknpc, gemnpc;

    private void spawnNPCs() {
        final World spawnworld = getWarpsWorld(), gameworld = getGameSpawn().getWorld(), netherworld = getNetherSpawn().getWorld();
        final String[] empty = new String[0];
        ranknpc = new Location(getGameSpawn().getWorld(), 6.5D, 79D, 18D);
        gemnpc = new Location(getWarpsWorld(), -90.5D, 21D, 441.5D);
        npcs.addPigZombie(new ItemStack(Material.GOLD_SWORD), new Location(gameworld, 4.5D, 77D, -0.5D), BlockFace.EAST,
                "§c§l1vs1", p -> {
                    if (p.isSneaking())
                        p.performCommand("training");
                    else
                        pvp.joinRankedQueue(p);
                }, (h, p) -> h.setName(p, p.isSneaking() ? "§6§lTraining" : "§c§l1vs1"));
        npcs.addCreeper(false, new Location(gameworld, 5.5D, 77D, 6.5D), BlockFace.NORTH,
                "§a§lWildnis", p -> p.performCommand("rtp"));
        npcs.addVillager(0, events.getNPCLoc().clone(), BlockFace.EAST,
                "§d§lEvent", events.npcaction);
        npcs.addVillager(0, clans.getNPCLoc().clone(), BlockFace.EAST,
                "§3§lClans", p -> clans.handleCommand(p, empty));
        npcs.addVillager(3, new Location(gameworld, 2.5D, 77D, 11.5D), BlockFace.NORTH,
                "§3§lBasen", p -> p.performCommand("basen"));
        npcs.addWolf(new Location(gameworld, 1D, 77D, 11.5D), BlockFace.NORTH_NORTH_EAST,
                "§c§lGriefen", p -> p.performCommand("grief"));
        npcs.addVillager(3, new Location(gameworld, 2.5D, 77D, -12.5D), BlockFace.SOUTH,
                "§4§lPvP", p -> p.performCommand("pvp"));
        npcs.addVillager(0, new Location(gameworld, 5.5D, 77D, 1.5D), BlockFace.EAST_NORTH_EAST,
                "§a§lBase", p -> {
                    if (base.hasBase(p)) {
                        p.performCommand("base");
                    } else {
                        if (base.isEnabled() == false && p.isOp() == false) {
                            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                            showItemBar(p, "§cDie Basen sind momentan deaktiviert.");
                            return;
                        }
                        base.createPlayerBase(p);
                    }
                });
        npcs.addMooshroom(new Location(gameworld, -12.5D, 76D, -23.5D), BlockFace.SOUTH_EAST,
                "§c§lWerben", p -> p.performCommand("affiliate"));
        npcs.addVillager(1, new Location(gameworld, 5.5D, 77D, -2.5D), BlockFace.EAST_SOUTH_EAST,
                "§e§lShop", p -> p.performCommand("shop"));
        npcs.addVillager(2, casino.getNPCLoc().clone(), BlockFace.SOUTH,
                "§d§lCasino", p -> casino.join(p));
        npcs.addVillager(3, new Location(gameworld, 11.5D, 79D, 19.5D), BlockFace.WEST,
                "§d§lCreator", p -> p.performCommand("creator"));
        npcs.addSheep(net.minecraft.server.v1_8_R3.EnumColor.GREEN, new Location(gameworld, 11D, 79D, 25.5D), BlockFace.WEST,
                "§a§lSupport", p -> p.performCommand("support"));
        npcs.addVillager(4, ranknpc.clone(), BlockFace.SOUTH,
                "§a§lGems & Perks", p -> p.performCommand("rank"), (h, p) ->
                        h.setName(p, p.hasPermission("muxsystem.ultra") ? "§a§lGems & Perks" : "§a§lRänge & Gems"));
        // SHOP NPCS
        npcs.addVillager(3, new Location(spawnworld, -98.5D, 21D, 441.5D), BlockFace.NORTH,
                "§e§lPremium Markt", p -> p.performCommand("markt"));
        npcs.addVillager(3, gemnpc.clone(), BlockFace.NORTH,
                "§b§lGem Shop", p -> p.performCommand("gemshop"));
        npcs.addVillager(4, new Location(spawnworld, -94.5D, 21D, 439.5D), BlockFace.NORTH,
                "§a§lVerkaufen", p -> shop.openSell(p, null, false), (h, p) -> {
                    final MuxUser u = getMuxUser(p.getName());
                    if (u == null) return;
                    h.setName(p, u.getProfits() >= 1.0D && checkGeneralCooldown(p.getName(), "SHOPPROFITS", 120000L, false) == false ? "§a§l§nHier MuxCoins abholen" : "§a§lVerkaufen");
                });
        npcs.addIronGolem(new Location(spawnworld, -94.5, 20, 424.5), BlockFace.SOUTH, "§f§lSuchen", p -> shop.openSearch(p));
        npcs.addVillager(1, new Location(spawnworld, -73.5D, 20D, 432.5D), BlockFace.WEST, "§a§lWie bekomme ich Gems?", p -> p.performCommand("buy"));
        npcs.addVillager(1, new Location(spawnworld, -115.5D, 20D, 432.5D), BlockFace.EAST, "§e§lWie bekomme ich MuxCoins?", p -> shop.openEarn(p));
        // NETHER NPCS
        npcs.addSkeleton(SkeletonType.WITHER, null, new Location(netherworld, -690.5D, 44D, 784.5D), BlockFace.WEST,
                "§c§lWildnis", p -> p.performCommand("rtp"));
        npcs.removeOld();
        // npcs.spawnPlayerNPC("MrMikado06", "§cWeihnachtsmann", "§8Weihnachtsmann",
        //	new Location(ms.getGameSpawn().getWorld(), -51, 70, -14), new ItemStack(Material.CHEST), pl -> showItemBar(pl, "§cHuhu!"));
    }

    public void sendScoreboard(final Player p) {
        scoreboard.sendScoreboard(p);
    }

    public void resetScoreboard(final Player p) {
        scoreboard.resetScoreboard(p);
    }

    public void sendScoreboard(final Player p, final ScoreboardType type) {
        scoreboard.sendScoreboard(p, type);
    }

    private boolean sparkup;
    private float sparkheight;
    private int sparkstep;

    public void sendSpiralParticles() {
        for (byte i = 0; i < 2; i++) {
            sparkstep += 4;
            if (sparkup) {
                if (sparkheight < 2.0F) {
                    sparkheight = (float) (sparkheight + 0.05D);
                } else {
                    sparkup = false;
                }
            } else if (sparkheight > 0.0F) {
                sparkheight = (float) (sparkheight - 0.05D);
            } else {
                sparkup = true;
            }
            final double inc = 0.06283185307179587D, angle = sparkstep * inc;
            final org.bukkit.util.Vector v = new Vector();
            v.setX(Math.cos(angle) * 1.1D);
            v.setZ(Math.sin(angle) * 1.1D);
            // Locations
            for (final Location l : booster.getBoostedLocs()) {
                l.getWorld().playEffect(l.clone().add(0.5, -0.5, 0.5).add(v).add(0, sparkheight, 0), Effect.HAPPY_VILLAGER, 1);
            }
            ranknpc.getWorld().playEffect(ranknpc.clone().add(v).add(0, sparkheight, 0), Effect.HAPPY_VILLAGER, 1);
            float r = (float) 1 / 255, g = (float) 255 / 255, b = (float) 255 / 255;

            gemnpc.getWorld().spigot().playEffect(gemnpc.clone().add(v).add(0, sparkheight, 0), Effect.COLOURED_DUST, 0, 1, r, g, b, 1, 0, 30);
        }
    }

    private boolean checkLocationsConfig() {
        if (locfile.exists() == false && saveLocs(null) == false) {
            System.err.println("MuxSystem> locations.yml could not be created!");
            return false;
        }
        return true;
    }

    public boolean saveLocs(final Player p) {
        try {
            locations.save(locfile);
            return true;
        } catch (final Exception e) {
            e.printStackTrace();
            if (p != null) showItemBar(p, "§cFehler: Fehler beim Speichern der Datei.");
            return false;
        }
    }

    public Location getLoc(final String path) {
        final String world = locations.getString(path + ".world");
        return world == null ? null : new Location(this.getServer().getWorld(world), locations.getDouble(path + ".x"), locations.getDouble(path + ".y"), locations.getDouble(path + ".z"),
                (float) locations.getDouble(path + ".yaw"), (float) locations.getDouble(path + ".pitch"));
    }

    public void setLoc(final String name, final Location l) {
        locations.set(name + ".world", l.getWorld().getName());
        locations.set(name + ".x", l.getX());
        locations.set(name + ".y", l.getY());
        locations.set(name + ".z", l.getZ());
        locations.set(name + ".yaw", l.getYaw());
        locations.set(name + ".pitch", l.getPitch());
    }

    private boolean isAlwaysAllowed(final Material m) {
        return m == Material.ENDER_CHEST || m == Material.WORKBENCH || m == Material.ENCHANTMENT_TABLE;
    }

    private boolean isCommunicationCMD(final String s) {
        return s.matches("^/(msg|mail|sup|tell|whisper|pm|pn|smsg).*$") || s.startsWith("/m ") || s.startsWith("/t ") || s.startsWith("/w ") || s.startsWith("/r ");
    }

    public short howManyAccounts(final Player p, final String ip) {
        short accs = 1;
        for (final Player pl : this.getServer().getOnlinePlayers()) {
            final MuxUser u2 = getMuxUser(pl.getName());
            if (pl != p && u2 != null && u2.getIp().equals(ip)) accs++;
        }
        return accs;
    }

    public void saveMuxUser(final MuxUser u) {
        new BukkitRunnable() {
            @Override
            public void run() {
                db.savePlayer(u);
            }
        }.runTaskAsynchronously(this);
    }

    private short repairItems(final ItemStack[] items, final boolean gold) {
        short count = 0;
        for (final ItemStack i : items) {
            if (i == null) continue;
            final Material m = i.getType();
            if (m.isBlock() || i.getDurability() == 0 || m.getMaxDurability() < 1 || extras.isMultiTool(i) || i.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL) > 4 || (gold == false && (isArmor(m) == false && m.toString().contains("SWORD") == false && m.toString().contains("BOW") == false))) {
                continue;
            }
            i.setDurability((short) 0);
            count++;
        }
        return count;
    }

    public ForkJoinPool getForkJoinPool() {
        return forkJoinPool;
    }

    public void stackItems(final Player p, final Material type, final boolean msg) {
        final ItemStack[] items = p.getInventory().getContents();
        final short size = (short) items.length;
        boolean changed = false;
        for (int i = 0; i < size; i++) {
            final ItemStack item = items[i];
            if (item == null || (type != null && item.getType() != type) || item.getAmount() <= 0 || item.getAmount() > 63) {
                continue;
            }
            int needed = 64 - item.getAmount();
            for (int j = i + 1; j < size; j++) {
                final ItemStack item2 = items[j];
                if (item2 != null && item2.getAmount() > 0) {
                    if (item.getType() == item2.getType() && item.getDurability() == item2.getDurability() && ((item.getItemMeta() == null && item2.getItemMeta() == null) || item.getItemMeta() != null && item.getItemMeta().equals(item2.getItemMeta()))) {
                        if (item2.getAmount() > needed) {
                            item.setAmount(64);
                            item2.setAmount(item2.getAmount() - needed);
                            changed = true;
                            break;
                        } else {
                            items[j] = null;
                            item.setAmount(item.getAmount() + item2.getAmount());
                            needed = 64 - item.getAmount();
                        }
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            p.getInventory().setContents(items);
            if (msg) {
                p.playSound(p.getLocation(), Sound.ZOMBIE_METAL, 1F, 1F);
                showItemBar(p, getLang("cmd.stackitems"));
            }
        } else if (msg) {
            showItemBar(p, getLang("cmd.stacknoitems"));
        }
    }

    public void sendNoVPNMessage(final Player p, final String r) {
        showItemBar(p, "§cDeaktiviere dein §f§lVPN§c, um kämpfen zu können.");
        if (checkGeneralCooldown(p.getName(), "NOVIPVPN", 30000L, true)) return;
        final String rank = ChatColor.translateAlternateColorCodes('&', perms.getGroup(r).getPrefix().replace(" ", ""));
        chatClickHoverLink(p, "§6§lMuxCraft>§7 VPNs sind mit dem Rang " + rank + "§7 erlaubt.", getLang("ranks.hoverforrank"), "https://shop.muxcraft.eu/?ign=" + p.getName());
        chatClickHoverLink(p, getLang("ranks.clickforrank"), getLang("ranks.hoverforrank"), "https://shop.muxcraft.eu/?ign=" + p.getName());
    }

    public void sendNoVoteMessage(final Player p) {
        chatClickHoverLink(p, getLang("vote.tounlock"), getLang("vote.hoverunlock"), "https://muxcraft.eu/vote/?ign=" + p.getName());
        chatClickHoverLink(p, getLang("vote.tounlock2"), getLang("vote.hoverunlock"), "https://muxcraft.eu/vote/?ign=" + p.getName());
    }

    public void sendNoRankMessage(final Player p, final String r) {
        final String rank = ChatColor.translateAlternateColorCodes('&', perms.getGroup(r).getPrefix().replace(" ", ""));
        chatClickHoverLink(p, getLang("ranks.youneedrank") + rank + "§7.", getLang("ranks.hoverforrank"), "https://shop.muxcraft.eu/?ign=" + p.getName());
        chatClickHoverLink(p, getLang("ranks.clickforrank"), getLang("ranks.hoverforrank"), "https://shop.muxcraft.eu/?ign=" + p.getName());
    }

    public void sendNoCMDMessage(final Player p) {
        showItemBar(p, "§cDieser Befehl ist unbekannt.");
        if (checkGeneralCooldown(p.getName(), "CMD", 30000L, true)) return;
        chatClickHoverRun(p, "§6§lMuxCraft>§7 Klicke §6hier§7, um die Liste aller Befehle zu sehen.", "§6§oKlicke um die Liste zu sehen", "/help");
    }

    public ItemStack leatherize(final ItemStack item, final Color color) {
        final LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        meta.setColor(color);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack getFireworkCharge(final Color c, final boolean enchant) {
        final ItemStack charge = new ItemStack(Material.FIREWORK_CHARGE);
        final FireworkEffectMeta fmeta = (FireworkEffectMeta) charge.getItemMeta();
        fmeta.setEffect(FireworkEffect.builder().withColor(c).build());
        fmeta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        fmeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        charge.setItemMeta(fmeta);
        if (enchant) charge.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 1);
        return charge;
    }

    public ItemStack getFireworkCharge(final Color c) {
        return getFireworkCharge(c, true);
    }

    public void spawnFirework(final Location l, final Color c) {
        final FireworkEffect effect = FireworkEffect.builder().with(Type.BALL_LARGE).flicker(false).trail(true).withColor(c).build();
        MuxFirework.spawn(l.clone().add(0.5, 1, 0.5), effect);
    }

    public void openBook(final ItemStack book, final Player p) {
        openBook(book, p, false);
    }

    public void openBook(final ItemStack book, final Player p, final boolean isEditable) {
        final int slot = p.getInventory().getHeldItemSlot();
        final ItemStack old = p.getInventory().getItem(slot);
        p.getInventory().setItem(slot, book);

        final ByteBuf buf = Unpooled.buffer(256);
        buf.setByte(0, (byte) (isEditable ? 1 : 0));
        buf.writerIndex(1);

        final PacketPlayOutCustomPayload packet = new PacketPlayOutCustomPayload("MC|BOpen", new PacketDataSerializer(buf));
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
        p.getInventory().setItem(slot, old);
    }

    public ItemStack linkBook(final String title, final String text, final Object[]... linkinfos) { // LINK, LINKTEXT, LINKHOVER
        return createBook(title, text, Arrays.asList(linkinfos), ClickEvent.Action.OPEN_URL);
    }

    public ItemStack cmdBook(final String title, final String text, final Object[]... cmdinfos) {
        return createBook(title, text, Arrays.asList(cmdinfos), ClickEvent.Action.RUN_COMMAND);
    }

    public ItemStack mixedBook(final String title, final String text, final Object[]... infos) {
        return createBook(title, text, Arrays.asList(infos), null);
    }

    public ItemStack createBook(final String title, final String text, final List<Object[]> clickables, final ClickEvent.Action a) {
        ItemStack is = new ItemStack(Material.WRITTEN_BOOK, 1);
        final net.minecraft.server.v1_8_R3.ItemStack nmsis = CraftItemStack.asNMSCopy(is);
        final NBTTagCompound bd = new NBTTagCompound();
        bd.setString("title", "Infos");
        bd.setString("author", "§a" + getServerName());
        nmsis.setTag(bd);
        is = CraftItemStack.asBukkitCopy(nmsis);
        final TextComponent tc = new TextComponent("§l" + title + "\n\n§0" + (text == null || text.length() == 0 ? "\n" : text + "\n\n"));
        final BookMeta bm = (BookMeta) is.getItemMeta();
        final CraftMetaBook m = (CraftMetaBook) bm;
        final int clickablesPerPage = 8;
        final int totalPages = (int) Math.ceil((double) clickables.size() / clickablesPerPage);
        for (int page = 0; page < totalPages; page++) {
            final List<BaseComponent> pageComponents = new ArrayList<>();
            pageComponents.add(page == 0 ? tc : new TextComponent(""));
            for (int i = 0; i < clickablesPerPage; i++) {
                final int clickableIndex = page * clickablesPerPage + i;
                if (clickableIndex >= clickables.size()) {
                    break;
                }
                final Object[] textstruct = clickables.get(clickableIndex);
                TextComponent linktc = new TextComponent("§9§n" + textstruct[1] + "§f");
                if (a != null || textstruct.length > 3) {
                    linktc.setClickEvent(new ClickEvent(a == null ? (ClickEvent.Action) textstruct[3] : a, textstruct[0].toString()));
                }
                if (textstruct[2].toString().isEmpty() == false) {
                    linktc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(textstruct[2].toString()).create()));
                }
                pageComponents.add(linktc);
            }
            m.pages.add(IChatBaseComponent.ChatSerializer.a(ComponentSerializer.toString(new TextComponent(pageComponents.toArray(new BaseComponent[0])))));
        }
        is.setItemMeta(bm);
        return is;
    }

    public ItemStack writeableBook(final String title, final String... pages) {
        return book(title, Material.BOOK_AND_QUILL, Arrays.asList(pages));
    }

    public ItemStack book(final String title, final String... pages) {
        return book(title, Arrays.asList(pages));
    }

    public ItemStack book(final String title, final List<String> pages) {
        return book(title, Material.WRITTEN_BOOK, pages);
    }

    private ItemStack book(final String title, final Material material, final List<String> pages) {
        ItemStack is = new ItemStack(material, 1);
        final net.minecraft.server.v1_8_R3.ItemStack nmsis = CraftItemStack.asNMSCopy(is);
        final NBTTagCompound bd = new NBTTagCompound();
        bd.setString("title", title);
        bd.setString("author", "§a" + getServerName());
        final NBTTagList bp = new NBTTagList();
        for (final String text : pages) {
            bp.add(new NBTTagString(text));
        }
        bd.set("pages", bp);
        nmsis.setTag(bd);
        is = CraftItemStack.asBukkitCopy(nmsis);
        return is;
    }

    public IChatBaseComponent[] getComponentFromSignLines(final String[] lines) {
        return new IChatBaseComponent[]{
                ChatSerializer.a("{\"text\": \"" + lines[0] + "\"}"),
                ChatSerializer.a("{\"text\": \"" + lines[1] + "\"}"),
                ChatSerializer.a("{\"text\": \"" + lines[2] + "\"}"),
                ChatSerializer.a("{\"text\": \"" + lines[3] + "\"}"),
        };
    }

    public void forcePlayer(final Player p, final Location l) {
        if (p.isInsideVehicle()) {
            p.leaveVehicle();
        }
        removeFromBattle(p.getName());
        if (admin.FARMWORLD.isActive() == false && l.getWorld().getName().equals(farmworld.getName())) {
            showItemBar(p, "§cDie Wildnis ist temporär deaktiviert.");
            return;
        }
        if (p.getWorld().getName().equals(l.getWorld().getName()) == false) {
            /*
            final Location wspawn = l.getWorld().getSpawnLocation();

            holograms.saveChunk(wspawn);
             */
            final Location wspawn = l.clone().add(0, 250, 0);
            p.teleport(wspawn, TeleportCause.COMMAND);
            if (l.getBlock().getType() != Material.AIR) {
                //p.teleport(l.getWorld().getHighestBlockAt(l).getLocation(), TeleportCause.COMMAND);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        forcePlayer(p, l);
                    }
                }.runTaskLater(this, 10L);
                return;
            }
        }
        if (l.getBlock().getType() == Material.BEDROCK) {
            p.teleport(l.getWorld().getHighestBlockAt(l).getLocation(), TeleportCause.COMMAND);
        } else {
            p.teleport(l, TeleportCause.COMMAND);
        }
        p.setVelocity(new Vector(0, 0, 0));
    }

    private void forcePlayer(final Player p, final Player p2) {
        if (p.isInsideVehicle()) {
            p.leaveVehicle();
        }
        if (p.getWorld().getName().equals(p2.getWorld().getName()) == false) {
            p.teleport(p2.getWorld().getSpawnLocation(), TeleportCause.COMMAND);
        }
        p.teleport(p2, TeleportCause.COMMAND);
    }

    public void feed(final Player p) {
        p.setFoodLevel(20);
        p.setSaturation(10);
        final Location l = p.getLocation();
        p.playSound(l, Sound.BURP, 0.5F, 1F);
        playEffect(EnumParticle.VILLAGER_ANGRY, l, 1, 1, 1, 0, 5);
    }

    public void logoutAll(final String name, final String reason) {
        final String kickmsg = String.format(getLang("loggedout"), servername, reason);
        short count = 0;
        tagged.clear();
        for (final Player pl : this.getServer().getOnlinePlayers()) {
            if (pl.getName().equals(name) == false) {
                pl.setMetadata("systemkick", new FixedMetadataValue(this, true));
                logoutPlayer(pl, kickmsg);
                count++;
            }
        }
        broadcastMessage("§6§lMuxCraft>§7 Es wurden §6" + count + " §7Spieler ausgeloggt.", null, Priority.HIGHEST);
    }

    public void logoutPlayer(final Player pl, final String kickmsg) {
        tagged.remove(pl.getName());
        final MuxPvP.Arena duel = pvp.getArena(pl);
        final MuxPvP.RankedArena ranked = pvp.get1vs1Arena(pl);
        if (duel != null) {
            duel.clearArena(false, true, false);
        } else if (inWar(pl)) {
            clanwar.handleDeath(pl, false);
        } else if (ranked != null) {
            ranked.clearArena(false, true, false, true);
        }
        pl.setMetadata("systemkick", new FixedMetadataValue(this, true));
        pl.kickPlayer(kickmsg);
    }

    public Player getLastHit(final Player p) {
        String last = null;
        long time = tagged.getOrDefault(p.getName(), 0L);
        for (final Map.Entry<String, Long> entry : tagged.entrySet()) {
            if (time == entry.getValue() && entry.getKey().equals(p.getName()) == false) {
                last = entry.getKey();
                break;
            }
        }
        return last == null ? null : this.getServer().getPlayer(last);
    }

    public boolean isAFK(final String pname) {
        return afkplayers.getOrDefault(pname, 0L) == -1L;
    }

    public void updateAFK(final Player p) {
        final String pname = p.getName();
        final long afktime = afkplayers.getOrDefault(pname, 0L);
        if (afktime == -1L) {
            chat.showMissedMessages(p, getMuxUser(pname), false);
            if (p.hasPermission("muxsystem.team")) {
                final Long since = afksince.remove(pname);
                if (since != null) team.addAFKTime(pname, System.currentTimeMillis() - since);
            }
        }
        afkplayers.put(pname, System.currentTimeMillis());
    }

    private void checkAFK(final Player p, final Location l) {
        final String pname = p.getName();
        final long afktime = afkplayers.getOrDefault(pname, 0L);
        final Location loc = currentpos.getOrDefault(pname, l);
        final boolean poschanged = l.getX() != loc.getX() || l.getZ() != loc.getZ();
        currentpos.put(pname, l);
        if (afktime == 0L || poschanged) {
            updateAFK(p);
        } else if (afktime != -1L && afktime + 120000L < System.currentTimeMillis()) {
            afkplayers.put(pname, -1L);
            if (p.hasPermission("muxsystem.team")) {
                afksince.put(pname, System.currentTimeMillis());
            }
            final MuxUser u = getMuxUser(pname);
            u.clearMailsIfSeen();
        }
    }

    public Map<String, Long> getAFKSince() {
        return afksince;
    }

    public String getPlayerStatus(final Player p, final OfflinePlayer op, final MuxUser u, final boolean showsince) {
        String time = " (seit " + getTime((int) ((System.currentTimeMillis() - op.getLastPlayed()) / 1000L)) + ")";
        time = time.replace("Tage", "Tagen");
        if (time.contains("Monat") || showsince == false) time = "";
        if (u != null) {
            if (u.isPermban()) return "§4" + getLang("banned");
            else if (u.isTimebanned())
                return "§4Suspendiert" + " (" + getTime((int) ((u.getUnbanTime() - System.currentTimeMillis()) / 1000L)) + ")";
        }
        final UUID server = shared.getOnlinePlayers().get(op.getUniqueId());
        boolean onlineOnNetwork = server != null && op.isOnline() == false;
        if (onlineOnNetwork && shared.getPlayersInVanish().contains(op.getName()) && p.isOp() == false)
            onlineOnNetwork = false;
        final boolean afk = isAFK(op.getName()), cansee = op.isOnline() && p.canSee(op.getPlayer());
        return op.isOnline() && inEvent(op.getPlayer()) && cansee ? "§dIm Event" :
                op.isOnline() && (inDuel(op.getPlayer()) || in1vs1(op.getPlayer()) || inWar(op.getPlayer()) || inBattle(op.getName(), op.getPlayer().getLocation())) && cansee ? "§eIm Kampf" :
                        team.isAFK(op.getName()) && cansee || afk && cansee ? "§eAbwesend" :
                                op.isOnline() && p.canSee(op.getPlayer()) ? "§aOnline" : (onlineOnNetwork ? ("§aOnline §7(auf " + servers.get(server) + ")") : ("§cOffline" + time));
    }

    public void setSlots(final short slots) {
        this.slots = slots;
        NMSReflection.setObject(net.minecraft.server.v1_8_R3.PlayerList.class, "maxPlayers", ((org.bukkit.craftbukkit.v1_8_R3.CraftServer) this.getServer()).getHandle(), slots);
    }

    public void showItemBar(final Player p, final String msg) {
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(ChatSerializer.a("{\"text\": \"" + msg.trim() + "\"}"), (byte) 2));
    }

    public void sendTitle(final Player p, final String text, final int fadein, final int showtime, final int fadeout) {
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutTitle(fadein, showtime, fadeout));
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutTitle(EnumTitleAction.TITLE, ChatSerializer.a("{\"text\": \"" + text + "\"}"), fadein, showtime, fadeout));
    }

    public void sendSubTitle(final Player p, final String text, final int fadein, final int showtime, final int fadeout) {
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutTitle(fadein, showtime, fadeout));
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(new PacketPlayOutTitle(EnumTitleAction.SUBTITLE, ChatSerializer.a("{\"text\": \"" + text + "\"}"), fadein, showtime, fadeout));
    }

    public void sendNormalTitle(final Player p, final String text, final int fadein, final int showtime, final int fadeout) {
        sendTitle(p, "§f", fadein, showtime, fadeout);
        sendSubTitle(p, text, fadein, showtime, fadeout);
    }

    private void activateAdminFly(final Player p) {
        if (p.hasPermission("muxsystem.fly") && deactivatedfly.contains(p.getUniqueId()) == false) {
            p.setAllowFlight(true);
            p.setFlying(true);
            godplayers.add(p.getName());
            globalfly.add(p.getName());
        }
    }

    private void showTabList(final Player p, final String h, final String f) {
        final PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter();
        try {
            headerField.set(packet, ChatSerializer.a("{\"text\": \"" + h + "\"}"));
            footerField.set(packet, ChatSerializer.a("{\"text\": \"" + f + "\"}"));
        } catch (final IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
    }

    public void sendNews(final Player p, final MuxUser u, final boolean always) {
        if (always || (newstitle.isEmpty() == false && u.seenNews() == false)) {
            if (newslink.isEmpty()) {
                openBook(linkBook(newstitle, newstext, new Object[]{"", "", ""}), p);
            } else {
                openBook(linkBook(newstitle, newstext, new Object[]{"https://muxcraft.eu/" + newslink, newslinktext, "Link öffnen"}), p);
            }
            if (always == false) u.setNews(true);
        }
    }

    private void joinLater(final Player p, final PermissionsGroup group, final MuxUser u) {
        new BukkitRunnable() {
            @Override
            public void run() {
                sendTabListColoredNames(p, group);
                if (u != null) sendNews(p, u, false);
            }
        }.runTaskLater(this, 4L);
    }

    public void sendTabListColoredNames(final Player p, final PermissionsGroup group) { // Fix 1.8.0 Tablist
        if (group.getPermissions().contains("muxteam") == false) {
            p.setPlayerListName(group.getColor().replace("&", "§") + p.getName());
        }
        for (final Player pl : getServer().getOnlinePlayers()) {
            if (p.canSee(pl)) {
                final EntityPlayer ep = ((CraftPlayer) p).getHandle(), ep2 = ((CraftPlayer) pl).getHandle();
                ep.playerConnection.sendPacket(new PacketPlayOutPlayerInfo(PacketPlayOutPlayerInfo.EnumPlayerInfoAction.UPDATE_DISPLAY_NAME, ep2));
            }
        }
    }

    public void itemRain(final Location l, final Material m) {
        l.add(0.5, 0, 0.5); // Fixes Offset
        final double items = 20D;
        final double u = 360 / items;
        final double radius = 2.7, miniradius = radius / 2;
        int fcount = 0;
        final ItemStack is = new ItemStack(m);
        for (short c = 0; c < 360; c += u) {
            final int randomangle = random.nextInt(15);
            final double bX = Math.sin(Math.toRadians(c + randomangle)) * (miniradius - Math.max(Math.random(), 0.5D)), bZ = Math.cos(Math.toRadians(c + randomangle)) * (miniradius - Math.max(Math.random(), 0.5D));
            final Item im = l.getWorld().dropItemNaturally(l, renameItemStack(is, String.valueOf(fcount)));
            im.setPickupDelay(5000);
            NMSReflection.setObject(net.minecraft.server.v1_8_R3.EntityItem.class, "age", ((CraftItem) im).getHandle(), 5880);
            im.setVelocity(new Vector(bX, 2.7D, bZ).multiply(0.2D));
            fcount++;
        }
    }

    public void chatClickHoverShow(final Player p, final String text, final String hovertext, final String command) {
        final TextComponent tc = new TextComponent(TextComponent.fromLegacyText(text));
        tc.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
        tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hovertext).create()));
        p.spigot().sendMessage(tc);
    }

    public void chatClickHoverRun(final Player p, final String text, final String hovertext, final String command) {
        final TextComponent tc = new TextComponent(TextComponent.fromLegacyText(text));
        tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hovertext).create()));
        p.spigot().sendMessage(tc);
    }

    public void chatClickHoverLink(final Player p, final String text, final String hovertext, final String link) {
        final TextComponent tc = new TextComponent(TextComponent.fromLegacyText(text));
        tc.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, link));
        tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hovertext).create()));
        p.spigot().sendMessage(tc);
    }

    public void broadcastAdvertisement(final String msg, final Priority pr) {
        if (pr.getPriority() < 3) {
            if (polls.isPollRunning()) return;
            for (final Player p : this.getServer().getOnlinePlayers()) {

                if (chat.hasDisabledChat(p.getName()) == false && hasGold(p.getUniqueId()) == false) {
                    p.sendMessage(msg);
                }
            }
        } else for (final Player p : this.getServer().getOnlinePlayers()) {
            if (hasGold(p.getUniqueId()) == false)
                p.sendMessage(msg);
        }
        System.out.println(msg);
    }

    public void broadcastMessage(final String msg, final String permission, final Priority pr) {
        if (permission != null) {
            if (pr.getPriority() < 3) {
                for (final Player p : this.getServer().getOnlinePlayers()) {
                    if (p.hasPermission(permission) && chat.hasDisabledChat(p.getName()) == false) {
                        p.sendMessage(msg);
                    }
                }
            } else for (final Player p : this.getServer().getOnlinePlayers()) {
                if (p.hasPermission(permission)) {
                    p.sendMessage(msg);
                }
            }
        } else if (pr.getPriority() < 3) {
            if (polls.isPollRunning()) return;
            for (final Player p : this.getServer().getOnlinePlayers()) {
                if (chat.hasDisabledChat(p.getName()) == false) {
                    p.sendMessage(msg);
                }
            }
        } else for (final Player p : this.getServer().getOnlinePlayers()) {
            p.sendMessage(msg);
        }
        System.out.println(msg);
    }

    public String fromArgs(final String[] args, final int from) {
        final StringBuilder s = new StringBuilder(args[from]);
        for (int i = from + 1; i < args.length; i++) {
            s.append(" ").append(args[i]);
        }
        return s.toString();
    }

    public List<String> getLoreFromLongText(final String text, final String start) {
        return wrapLore(start != null ? start + text : text, 40, true);
        //return Arrays.asList(ChatPaginator.wordWrap((start != null ? start + text : text), 30));
    }

    private List<String> wrapLore(final String lore, final int maxLength, final boolean wordWrap) {
        final List<String> wrappedLore = new ArrayList<>();
        String[] lines = lore.split("\\r?\\n");
        for (final String line : lines) {
            String unwrappedLine = line.replaceFirst("\\s++$", "");
            while (unwrappedLine.length() > maxLength) {
                int splitIndex;
                if (wordWrap) {
                    splitIndex = getWrapIndex(unwrappedLine, maxLength);
                } else {
                    splitIndex = maxLength;
                }
                String newLine = unwrappedLine.substring(0, splitIndex);
                newLine = newLine.replaceFirst("\\s++$", "");
                final String chatColor = ChatColor.getLastColors(newLine);
                unwrappedLine = chatColor + unwrappedLine.substring(splitIndex);
                wrappedLore.add(newLine);
            }
            wrappedLore.add(unwrappedLine);
        }
        return wrappedLore;
    }

    private int getWrapIndex(final String line, final int maxLength) {
        final int splitIndex = Math.min(maxLength, line.length());
        int spaceIndex = splitIndex;
        while (spaceIndex >= 0) {
            if (line.charAt(spaceIndex) == ' ') {
                return spaceIndex + 1;
            } else {
                spaceIndex--;
            }
        }
        return splitIndex;
    }

    public String retardCleaner(final String text, final String placeholder) {
        return text.replace(placeholder, "").replace(" ", "").replace(":", "");
    }

    public boolean isNumeric(final String s) {
        return s.matches("[-+]?\\d*\\.?\\d+");
    }

    public boolean checkGeneralCooldown(final String name, final String type, final long cooldown, final boolean save) {
        final HashMap<String, Long> map = cooldowns.getOrDefault(name, new HashMap<>());
        if (map.containsKey(type) && map.get(type) + cooldown > System.currentTimeMillis()) {
            return true;
        }
        if (save) {
            map.put(type, System.currentTimeMillis());
            cooldowns.put(name, map);
        }
        return false;
    }


    public void resetLimit(final String type) {
        for (final Player p : this.getServer().getOnlinePlayers()) {
            final HashMap<String, Integer> map = limits.get(p.getName());
            if (map == null) continue;
            else map.remove(type);
            limits.put(p.getName(), map);
        }
    }

    public String locationToString(final Location l) {
        return locationToString(l, true);
    }

    public String locationToString(final Location l, final boolean world) {
        if (l == null) return "";
        final double roundx = Math.round(l.getX() * 10.0) / 10.0, roundy = Math.round(l.getY() * 10.0) / 10.0, roundz = Math.round(l.getZ() * 10.0) / 10.0,
                roundyaw = Math.round(l.getYaw() * 10.0) / 10.0, roundpitch = Math.round(l.getPitch() * 10.0) / 10.0;
        return (world ? (l.getWorld().getName() + ":") : "") + roundx + ":" + roundy + ":" + roundz + ":" + roundyaw + ":" + roundpitch;
    }

    public String blockLocationToStringNoYawPitch(final Location l) {
        if (l == null) return "";
        return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }

    public Location stringToBlockLocationNoYawPitch(final String s) {
        if (s == null || s.trim().equals("")) return null;
        final String[] parts = s.split(":");
        if (parts.length == 4) {
            final World world = this.getServer().getWorld(parts[0]);
            if (world != null) {
                return new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
            }
        }
        return null;
    }

    public Location stringToLocation(final String s) {
        if (s == null || s.trim().equals("")) return null;
        final String[] parts = s.split(":");
        if (parts.length == 6) {
            final World world = this.getServer().getWorld(parts[0]);
            if (world != null) {
                return new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Float.parseFloat(parts[4]), Float.parseFloat(parts[5]));
            }
        }
        return null;
    }

    public void unzip(final String fileName) {
        try (final ZipFile zipFile = new ZipFile(fileName)) {
            zipFile.stream().forEach(entry -> {
                final Path filePath = Paths.get(entry.getName());
                try {
                    if (entry.isDirectory()) {
                        Files.createDirectories(filePath);
                    } else {
                        Files.createDirectories(filePath.getParent());
                        try (final InputStream input = zipFile.getInputStream(entry)) {
                            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public UUID getServerId() {
        return serverId;
    }

    public Map<UUID, String> getServers() {
        return servers;
    }


    public int addPageButtons(final Inventory inv, int page, short start, final Pageifier<ItemStack> pages) {
        page = Math.max(0, Math.min(page, pages.getPages().size() - 1));
        short pos = start;
        for (final ItemStack entry : pages.getPage(page)) {
            inv.setItem(pos, entry);
            pos++;
        }
        if (page != 0)
            inv.setItem(7, getHeadFromURL("https://textures.minecraft.net/texture/bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9", getLang("market.previouspage")));
        if (page < pages.getPages().size() - 1)
            inv.setItem(8, getHeadFromURL("https://textures.minecraft.net/texture/19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf", getLang("market.nextpage")));
        return page;
    }

    public int addPageButtons(final Inventory inv, int page, final Pageifier<ItemStack> pages) {
        return this.addPageButtons(inv, page, (short) 18, pages);
    }

    public boolean checkLimit(final String name, final String type, final int limit, final boolean save) {
        final HashMap<String, Integer> map = limits.getOrDefault(name, new HashMap<>());
        if (map.containsKey(type) && map.get(type) >= limit) {
            return true;
        }
        if (save) {
            final int l = map.getOrDefault(type, 0);
            map.put(type, l + 1);
            limits.put(name, map);
        }
        return false;
    }

    public void sendPlayerToServer(final Player p, final String server) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server);
        p.sendPluginMessage(this, "BungeeCord", out.toByteArray());
    }

    public boolean inGame(final Player p) {
        return games.inGame(p) != null;
    }

    public boolean inWar(final Player p) {
        return clanwar.inWar(p);
    }

    public boolean inDuel(final Player p) {
        return pvp.inDuel(p);
    }

    public boolean in1vs1(final Player p) {
        return pvp.in1vs1(p);
    }

    public boolean inEvent(final Player p) {
        return events.inEvent(p) != null;
    }

    public boolean inCasino(final Player p) {
        return casino.inCasino(p);
    }

    public Event getEvent(final Player p) {
        return events.inEvent(p);
    }

    public ScoreboardType getShopSB() {
        return shopsb;
    }

    public boolean hasGlobalFly(final Player p) {
        return globalfly.contains(p.getName());
    }

    public boolean hasNormalFlight(final Player p) {
        return p.getGameMode() == GameMode.CREATIVE || p.getGameMode() == GameMode.SPECTATOR || hasGlobalFly(p);
    }

    public boolean removeGlobalFly(final String pname) {
        return globalfly.remove(pname);
    }

    public void addGlobalFly(final String pname) {
        globalfly.add(pname);
    }

    public boolean hasGodMode(final Player p) {
        return godplayers.contains(p.getName());
    }

    public boolean removeGodMode(final String pname) {
        return godplayers.remove(pname);
    }

    public void addGodMode(final String pname) {
        godplayers.add(pname);
    }

    public void setLastLocation(final Player p) {
        setLastLocation(p, p.getLocation());
    }

    public void setLastLocation(final Player p, final Location l) {
        final MuxUser u = getMuxUser(p.getName());
        final String wname = l.getWorld().getName();
        if (u == null || (p.getGameMode() == GameMode.SPECTATOR && p.isOp() == false) || wname.contains("MuxCasino"))
            return;
        if (wname.equals("warps")) {
            final Location active = activeloc.get(p.getName());
            if (active != null) {
                u.setLastLocation(active);
            }
        } else {
            u.setLastLocation(l);
        }
    }

    public void setActiveLocation(final Player p, final Location l) {
        activeloc.put(p.getName(), l);
    }

    public void teleportPlayer(final Player p, final Location l) {
        if (l == null) return;
        if (l.getWorld().equals(getWarpsWorld()) && p.getGameMode() != GameMode.SPECTATOR) {
            setActiveLocation(p, l);
        }
        final boolean particles = isVanish(p) == false && p.getGameMode() != GameMode.SPECTATOR && l.equals(p.getLocation()) == false;
        if (particles) playSmokeEffect(p);
        forcePlayer(p, l);
        if (particles) playSmokeEffect(p);
        p.playSound(l, Sound.ENDERMAN_TELEPORT, 0.4F, 1F);
    }

    public ItemStack getHead(final String name) {
        final ItemStack kopf = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        final SkullMeta meta = (SkullMeta) kopf.getItemMeta();
        meta.setOwner(name);
        kopf.setItemMeta(meta);
        return kopf;
    }

    public ItemStack getHeadFromURL(final String url, final String itemname) {
        final ItemStack kopf = new ItemStack(Material.SKULL_ITEM, 1, (short) 3);
        if (url.isEmpty()) return kopf;

        final SkullMeta headmeta = (SkullMeta) kopf.getItemMeta();
        final GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        final byte[] encodedData = Base64.encodeBase64(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes(StandardCharsets.UTF_8));
        profile.getProperties().put("textures", new Property("textures", new String(encodedData, StandardCharsets.UTF_8)));
        NMSReflection.setObject(headmeta.getClass(), "profile", headmeta, profile);
        headmeta.setDisplayName(itemname);
        kopf.setItemMeta(headmeta);
        return kopf;
    }

    public ItemStack addGlow(final ItemStack stack) {
        final net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(stack);
        NBTTagCompound tag = nmsStack.getTag();
        if (tag == null) {
            tag = new NBTTagCompound();
            nmsStack.setTag(tag);
        }
        tag.set("ench", new NBTTagList());
        return CraftItemStack.asCraftMirror(nmsStack);
    }

    public short getEmptyInvSlots(final Inventory inv) {
        short count = 0;
        for (final ItemStack i : inv.getContents()) {
            if (i == null) count++;
        }
        return count;
    }

    public short getMaterialCount(final Inventory inv, final Material m, final short data) {
        short count = 0;
        for (final ItemStack i : inv) {
            if (i != null && i.getType() == m && i.getData().getData() == data) count += i.getAmount();
        }
        return count;
    }

    public DyeColor getDyeColor(final ChatColor c) {
        if (c == null) return DyeColor.WHITE;
        switch (c) {
            case AQUA:
                return DyeColor.LIGHT_BLUE;
            case BLACK:
                return DyeColor.BLACK;
            case BLUE:
            case DARK_BLUE:
                return DyeColor.BLUE;
            case DARK_AQUA:
                return DyeColor.CYAN;
            case DARK_GRAY:
                return DyeColor.GRAY;
            case DARK_GREEN:
                return DyeColor.GREEN;
            case DARK_PURPLE:
                return DyeColor.PURPLE;
            case RED:
            case DARK_RED:
                return DyeColor.RED;
            case GOLD:
                return DyeColor.ORANGE;
            case GRAY:
                return DyeColor.SILVER;
            case GREEN:
                return DyeColor.LIME;
            case LIGHT_PURPLE:
                return DyeColor.MAGENTA;
            case YELLOW:
                return DyeColor.YELLOW;
            case WHITE:
            default:
                return DyeColor.WHITE;
        }
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

    public void removeSounds(final Entity entity) {
        final net.minecraft.server.v1_8_R3.Entity nmsEntity = ((CraftEntity) entity).getHandle();
        nmsEntity.b(true);
    }

    public int removeEntities(final EntType et, final World world, final Player p, final int radius) {
        if (p == null && radius > 0) return 0;
        final World w = p == null ? world : p.getWorld();
        final UUID muxuuid = getIdentificationId();
        final Predicate<Entity> entityTypeFilter = e -> {
            switch (et) {
                case ITEM:
                    return e instanceof Item && e.getUniqueId().equals(muxuuid) == false;
                case ARROW:
                    return e instanceof Projectile;
                case BOAT:
                    return e instanceof Boat;
                case MINECART:
                    return e instanceof Minecart;
                case XP:
                    return e instanceof ExperienceOrb;
                default:
                    return false;
            }
        };
        final Predicate<Entity> distanceFilter = radius <= 0 ? e -> true : e -> p.getLocation().distanceSquared(e.getLocation()) <= radius;
        return (int) Stream.of(w.getLoadedChunks())
                .flatMap(chunk -> Stream.of(chunk.getEntities()))
                .filter(distanceFilter.and(entityTypeFilter))
                .peek(Entity::remove)
                .count();
    }

    public boolean canBuildAtLoc(final Player p, final Location l) {
        final ApplicableRegionSet regionSet = WGBukkit.getRegionManager(l.getWorld()).getApplicableRegions(l);
        return (regionSet.size() <= 0 || isWGTrusted(p.getName())) && (l.getWorld() != base.getWorld() || base.isTrustedAtLocation(p));
    }

    private boolean handleSign(final Player p, final Block b, boolean rightclick) {
        if (rightclick && (p.getItemInHand() == null || p.getItemInHand().getType() == Material.AIR) && canBuildAtLoc(p, b.getLocation())) {
            final IChatBaseComponent[] lines = new IChatBaseComponent[4];
            final Sign s = (Sign) b.getState();
            for (int i = 0; i < 4; i++) {
                if (s.getLine(i) != null)
                    lines[i] = new ChatComponentText(s.getLine(i).replace("§", "&"));
            }
            final TileEntitySign tileEntity = (TileEntitySign) ((CraftWorld) s.getWorld()).getHandle().getTileEntity(new BlockPosition(s.getX(), s.getY(), s.getZ()));
            tileEntity.isEditable = true;
            tileEntity.a(((CraftHumanEntity) p).getHandle());
            final PacketPlayOutUpdateSign packet1 = new PacketPlayOutUpdateSign(tileEntity.getWorld(), tileEntity.getPosition(), lines);
            final PacketPlayOutOpenSignEditor packet2 = new PacketPlayOutOpenSignEditor(tileEntity.getPosition());
            final PlayerConnection pc = ((CraftPlayer) p).getHandle().playerConnection;
            pc.sendPacket(packet1);
            pc.sendPacket(packet2);
            return false;
        }
        if (b.getWorld().equals(getWarpsWorld()) && b.getWorld().getNearbyEntities(b.getLocation(), 2, 2, 2).stream().anyMatch(entity -> entity.getType() == EntityType.ITEM_FRAME)) {
            final String[] lines = ((Sign) b.getState()).getLines();
            if (p.getGameMode() == GameMode.CREATIVE || (lines.length != 4 && lines[2].contains("MuxCoins") == false))
                return false;
            showItemBar(p, "§eKlicke auf das Item im Rahmen, um es zu kaufen.");
            return true;
        }
        final String[] lines = ((Sign) b.getState()).getLines();
        final Server s = this.getServer();
        for (byte i = 1; i < 4; i++) {
            final String line = ChatColor.stripColor(lines[i]).replace("", "").replace("", "");
            if (line.startsWith("/") && lines[i].contains("§")) {
                runCommand(p, s, line);
                return true;
            }
        }
        final String line2 = lines[1];
        if (line2.equals("§l[" + getLang("giftcode.reward") + "]")) {
            giftcodes.handlePlayer(p, ChatColor.stripColor(lines[2]), false);
            return true;
        } else if (line2.equals("§1§l[Türchen]")) {
            advent.handleSign(p, (Sign) b.getState(), lines);
            return true;
        } else if (line2.equals("§l[" + getLang("trash") + "]")) {
            p.performCommand("d");
            return true;
        } else if (line2.contains("§")) {
            if (line2.contains("Discord")) {
                openBook(linkBook("§1§lDiscord", "Klicke unten, um den MuxCraft Discord beizutreten.",
                        new Object[]{"https://muxcraft.eu/discord", "Zum Discord >", "Link öffnen"}), p);
                return true;
            } else if (line2.contains("YouTube")) {
                openBook(linkBook("§4§lYouTube", "Klicke unten, um den MuxCraft Kanal zu abonnieren.",
                        new Object[]{"https://muxcraft.eu/youtube", "Zum Kanal >", "Link öffnen"}), p);
                return true;
            } else if (line2.contains("Website")) {
                openBook(linkBook("§0§lWebsite", "Klicke unten, um zur MuxCraft Website zu gelangen.",
                        new Object[]{"https://muxcraft.eu", "Zur Website >", "Link öffnen"}), p);
                return true;
            } else if (line2.contains("Onlineshop")) {
                p.performCommand("buy");
                return true;
            }
        }
        return false;
    }

    public void syncCMD(final String str) {
        final Server s = this.getServer();
        new BukkitRunnable() {
            @Override
            public void run() {
                s.dispatchCommand(s.getConsoleSender(), str);
            }
        }.runTaskLater(this, 5L);
    }

    public String getTime(final int sek) {
        if (sek / 2592000F >= 2) return sek / 2592000 + " " + getLang("months");
        else if (sek / 2592000F >= 1) return sek / 2592000 + " " + getLang("month");
        else if (sek / 86400F >= 2) return sek / 86400 + " " + getLang("days");
        else if (sek / 86400F >= 1) return sek / 86400 + " " + getLang("day");
        else if (sek / 3600F >= 2) return sek / 3600 + " " + getLang("hours");
        else if (sek / 3600F >= 1) return sek / 3600 + " " + getLang("hour");
        else if (sek / 60F >= 2) return sek / 60 + " " + getLang("minutes");
        else if (sek / 60F >= 1) return sek / 60 + " " + getLang("minute");
        else return sek + (sek == 1 ? " " + getLang("second") : " " + getLang("seconds"));
    }

    public void createButton(final Inventory i, int pos, final ItemStack item) {
        for (short n = 1; n <= 9; n++) {
            i.setItem(pos, item);
            if (n % 3 == 0) pos += 7;
            else pos++;
        }
    }

    public boolean hasGlow(final ItemStack item) {
        final net.minecraft.server.v1_8_R3.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
        if (nmsStack == null) return false;
        final NBTTagCompound tag = nmsStack.getTag();
        if (tag == null) return false;
        return tag.hasKey("ench");
    }

    public ItemStack getItemStack(final String[] args, final int start) {
        final ItemStack it = new ItemStack(Material.AIR);
        Material m;
        final String[] itemarray = args[start].split(":");
        try {
            m = Material.getMaterial(Integer.parseInt(itemarray[0]));
        } catch (final NumberFormatException e) {
            m = Material.getMaterial(itemarray[0].toUpperCase());
        }
        if (m == null) return null;
        it.setType(m);
        try {
            it.setDurability(Short.parseShort(itemarray[1]));
        } catch (final Exception ignored) {
        }
        if (args.length == start + 2) {
            try {
                short a = Short.parseShort(args[start + 1]);
                if (a < 1) a = 1;
                it.setAmount(a);
            } catch (final NumberFormatException ignored) {
            }
        }
        return it;
    }

    public void showLater(final Player p, final String text) {
        runLater(new BukkitRunnable() {
            @Override
            public void run() {
                showItemBar(p, text);
            }
        }, 60L);
    }

    public void runCommand(final Player p, final Server sr, final String cmd) {
        final PlayerCommandPreprocessEvent ev = new PlayerCommandPreprocessEvent(p, cmd);
        sr.getPluginManager().callEvent(ev);
        if (ev.isCancelled() == false) {
            p.performCommand(cmd.replace("/", ""));
            p.updateInventory();
        }
    }

    public EditSession getFastBuilder(final String worldname) {
        final EditSession wesession = new EditSessionBuilder(FaweAPI.getWorld(worldname))
                .limitUnlimited().checkMemory(false).allowedRegionsEverywhere().fastmode(true).build();
        wesession.setBlockChangeLimit(Integer.MAX_VALUE);
        return wesession;
    }

    public void runLater(final BukkitRunnable br, final long ticks) {
        br.runTaskLater(this, ticks);
    }

    public String removeNonDigits(final String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        return s.replaceAll("\\D+", "");
    }

    public String removeNonCentDigits(final String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        final String cents = s.replaceAll("[^0-9/.]+", "");
        if (cents.startsWith(".") || cents.startsWith("0.")) return cents;
        else return s.replaceAll("\\D+", "");
    }

    public boolean notSafeYAML(final String s) {
        return s.contains(".") || s.contains("=") || s.contains(">");
    }

    public boolean notSafeGSON(final String s) {
        return s.contains(";") || s.contains("\"") || s.contains("'");
    }

    public String getLang(final String c) {
        return language.getLang(c);
    }

    public String usage(final String c) {
        return "§c" + getLang("usage") + ": §f" + c;
    }

    public boolean hasGold(final UUID uuid) {
        return hasRank(uuid, "muxsystem.gold");
    }

    public boolean hasVip(final UUID uuid) {
        return hasRank(uuid, "muxsystem.vip");
    }

    public boolean hasX(final UUID uuid) {
        return hasRank(uuid, "muxsystem.x");
    }

    public boolean hasRank(final UUID uuid, final String permission) {
        final MuxRanks.PermissionsUser permUser = perms.getUserData(uuid);
        if (permUser != null) {
            final MuxRanks.PermissionsGroup group = perms.getGroup(permUser.getGroup());
            return group.getPermissions().contains(permission);
        }
        return false;
    }

    public boolean hasRankNoPerms(final UUID uuid, final String rankName) {
        final MuxRanks.PermissionsUser permUser = perms.getUserData(uuid);
        if (permUser != null)
            return permUser.getGroup() != null && permUser.getGroup().equalsIgnoreCase(rankName);
        return false;
    }

    public String getServerName() {
        return servername;
    }

    public int getBungeeUsers() {
        return bungeeusers;
    }

    public MySQL getDB() {
        return db;
    }

    public World getFarmWorld() {
        return farmworld;
    }

    public World getWarpsWorld() {
        return warpspawn.getWorld();
    }

    public Location getGameSpawn() {
        return gamespawn;
    }

    public Location getNetherSpawn() {
        return netherspawn;
    }

    public World getEndWorld() {
        return endspawn.getWorld();
    }

    public ProtectedRegion getSpawnRegion() {
        return spawnregion;
    }

    public ProtectedRegion getFullSpawnRegion() {
        return fullspawnregion;
    }

    public MuxLanguage getLanguage() {
        return language;
    }

    public int getSlots() {
        return slots;
    }

    public String getNewsText() {
        return newstext;
    }

    public void setNewsText(final String text) {
        this.newstext = text;
    }

    public String getNewsLink() {
        return newslink;
    }

    public void setNewsLink(final String link) {
        this.newslink = link;
    }

    public String getNewsLinkText() {
        return newslinktext;
    }

    public void setNewsLinkText(final String linktext) {
        this.newslinktext = linktext;
    }

    public String getNewsTitle() {
        return newstitle;
    }

    public void setNewsTitle(final String title) {
        this.newstitle = title;
    }

    public String getJoinMSG() {
        return joinmsg;
    }

    public void setJoinMSG(final String joinmsg) {
        this.joinmsg = joinmsg;
    }

    public MuxWarps getWarps() {
        return warps;
    }

    public MuxAnalytics getAnalytics() {
        return analytics;
    }

    public MuxClanWar getClanWar() {
        return clanwar;
    }

    public MuxPvP getPvP() {
        return pvp;
    }

    public MuxAffiliate getAffiliate() {
        return affiliate;
    }

    public MuxEmails getEmails() {
        return email;
    }

    public MuxPvPBots getPvPBots() {
        return pvpbots;
    }

    public MuxEvents getEvents() {
        return events;
    }

    public MuxFeedback getFeedback() {
        return feedback;
    }

    public MuxInventory getInv() {
        return inv;
    }

    public MuxSeason getSeason() {
        return season;
    }

    public MuxMarket getMarket() {
        return market;
    }

    public MuxShared getShared() {
        return shared;
    }

    public MuxHomes getHomes() {
        return homes;
    }

    public MuxGames getGames() {
        return games;
    }

    public MuxPerks getPerks() {
        return perks;
    }

    public MuxPets getPets() {
        return pets;
    }

    public MuxMounts getMounts() {
        return mounts;
    }

    public MuxExtraCommands getExtraCMDs() {
        return extracmds;
    }

    public MuxPvPStats getStats() {
        return stats;
    }

    public MuxProfiles getProfiles() {
        return profiles;
    }

    public MuxClans getClans() {
        return clans;
    }

    public MuxNPCs getNPCS() {
        return npcs;
    }

    public MuxKits getKits() {
        return kits;
    }

    public MuxBigEnderChest getExtraEnderChest() {
        return extraenderchest;
    }

    public MuxNewbies getNewbies() {
        return newbies;
    }

    public MuxPayments getPayments() {
        return payments;
    }

    public MuxRanks getPerms() {
        return perms;
    }

    public MuxBans getBans() {
        return bans;
    }

    public MuxReports getReports() {
        return reports;
    }

    public MuxFriends getFriends() {
        return friends;
    }

    public MuxScoreboard getScoreboard() {
        return scoreboard;
    }

    public Set<org.bukkit.scoreboard.Scoreboard> getScoreboards() {
        return scoreboard.getScoreboards();
    }

    public MuxOverview getOverview() {
        return overview;
    }

    public MuxAdmin getAdmin() {
        return admin;
    }

    public MuxTeam getTeam() {
        return team;
    }

    public MuxTrade getTrades() {
        return trades;
    }

    public MuxCreators getCreators() {
        return creators;
    }

    public MuxSupport getSupport() {
        return support;
    }

    public MuxEmojis getEmojis() {
        return emojis;
    }

    public MuxBoosters getBooster() {
        return booster;
    }

    public MuxBossBar getBossBar() {
        return bossbar;
    }

    public MuxPolls getPolls() {
        return polls;
    }

    public MuxAntiCheat getAntiCheat() {
        return anticheat;
    }

    public MuxAntiBot getAntiBot() {
        return antibot;
    }

    public MuxChat getChat() {
        return chat;
    }

    public MuxChatFilter getChatFilter() {
        return chatfilter;
    }

    public MuxFonts getFonts() {
        return fonts;
    }

    public MuxShop getShop() {
        return shop;
    }

    public ProtectedRegion getShopRegion() {
        return shopregion;
    }

    public MuxGiantItems getGiants() {
        return giants;
    }

    public MuxChests getChests() {
        return chests;
    }

    public MuxUltra getUltra() {
        return ultra;
    }

    public MuxGold getGold() {
        return gold;
    }

    public MuxCustomRank getCustomRank() {
        return customrank;
    }

    public MuxCasino getCasino() {
        return casino;
    }

    public MuxMenu getMenu() {
        return menu;
    }

    public MuxHoliday getHoliday() {
        return holiday;
    }

    public MuxSnow getSnow() {
        return snow;
    }

    public MuxVotes getVotes() {
        return votes;
    }

    public MuxTips getTips() {
        return tips;
    }

    public MuxBase getBase() {
        return base;
    }

    public MuxTimings getTimings() {
        return timings;
    }

    public MuxGiftCodes getGiftCodes() {
        return giftcodes;
    }

    public MuxGemShop getGemShop() {
        return gemshop;
    }

    public MuxHistory getHistory() {
        return history;
    }

    public MuxExtras getExtras() {
        return extras;
    }

    public MuxAntiBugs getAntiBugs() {
        return antibugs;
    }

    public MuxAntiLags getAntiLags() {
        return antilags;
    }

    public MuxHolograms getHolograms() {
        return holograms;
    }

    public boolean isFullTrusted(final String pname) {
        return fulltrusted.contains(pname);
    }

    public boolean isTrusted(final String pname) {
        return trusted.contains(pname) || fulltrusted.contains(pname);
    }

    public Set<String> getTrusted() {
        return trusted;
    }

    public boolean isWETrusted(final String pname) {
        return wetrusted.contains(pname);
    }

    public boolean isWGTrusted(final String pname) {
        return wgtrusted.contains(pname);
    }

    public boolean isBeingTeleported(final String pname) {
        return tpplayers.contains(pname);
    }

    public boolean hasVoted(final OfflinePlayer p) {
        return votes.hasVoted(p);
    }

    public boolean isVanish(final Player p) {
        return vanish.isVanish(p.getName());
    }

    public MuxUser getMuxUser(final String pname) {
        return users.get(pname);
    }

    public UUID getIdentificationId() { // GLOBAL MUXCRAFT ENTITY IDENTIFICATION UUID
        return muxIndentifier;
    }

    public long getActiveMuxCoinsSupply() {
        return activeMuxCoinsSupply + (activeChipsSupply / 10L);
    }

    public long getActiveChipsSupply() {
        return activeChipsSupply;
    }

    public String getMultiplier(final int mult) {
        switch (mult) {
            case 0:
                return "Normal";
            case 1:
                return "Double";
            case 2:
                return "Triple";
            case 3:
                return "Quadriple";
            case 4:
                return "Quintuple";
            default:
                return "???";
        }
    }

    public String healthToString(final double health) {
        final StringBuilder s = new StringBuilder(health >= 16D ? "§a" : health >= 8D ? "§e" : "§c");
        for (double i = 0; i < health; i = i + 2D) {
            s.append("❤");
        }
        return s.toString();
    }

    public void queueNotImportantSyncAction(final MuxActions.Action action) {
        notImportantSyncActions.add(action);
    }

    private void handleNotImportantSyncActions() {
        MuxActions.Action action;
        while ((action = notImportantSyncActions.poll()) != null) {
            try {
                action.call();
            } catch (Exception e) {
                Stacktrace.print(e);
            }
        }
    }

    public String itemStackToString(final ItemStack item) {
        String base64 = null;
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
             final BukkitObjectOutputStream bukkitOut = new BukkitObjectOutputStream(out)) {
            bukkitOut.writeObject(item);
            base64 = Base64Coder.encodeLines(out.toByteArray());
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
        return base64;
    }

    public ItemStack stringToItemStack(final String item) {
        ItemStack result = null;
        try (final ByteArrayInputStream in = new ByteArrayInputStream(Base64Coder.decodeLines(item));
             final BukkitObjectInputStream bukkitIn = new BukkitObjectInputStream(in)) {
            result = (ItemStack) bukkitIn.readObject();
        } catch (final Exception ex) {
            ex.printStackTrace();
        }
        return result;
    }

    public FileConfiguration getHashYAML() {
        return hashYML;
    }

    public Set<UUID> getTempFix() {
        return tempfix;
    }

    public String getDecimalFormat(final double d) {
        return decformat.format(d);
    }

    public String getDownDecimalFormat(final double d) {
        decformat.setRoundingMode(RoundingMode.DOWN);
        final String result = decformat.format(d);
        decformat.setRoundingMode(RoundingMode.HALF_EVEN);
        return result;
    }

    public String getNumberFormat(final long num) {
        return numformat.format(num);
    }

    public String getNumberFormat(final double num) {
        if (num >= 1.00D)
            return numformat.format(num);
        return decformat.format(num);
    }

    public String getSFormat(final long value) { // Abbreviate large number amounts
        if (value == Long.MIN_VALUE) return getSFormat(Long.MIN_VALUE + 1);
        if (value < 0) return "-" + getSFormat(-value);
        if (value < 1000) return String.valueOf(value);
        int exp = (int) (Math.log(value) / Math.log(1000));
        final DecimalFormat format = new DecimalFormat("0.#");
        if (value == 999999 || value == 999999999) exp += 1; // special cases
        return String.format("%s%c", format.format(value / Math.pow(1000, exp)), "KMBTPE".charAt(exp - 1));
    }

    public String getSFormat(final double value) { // Abbreviate large number amounts
        if (value >= 1.00D)
            return getSFormat((long) value);
        if (value == Long.MIN_VALUE) return getSFormat(Long.MIN_VALUE + 1);
        if (value < 0) return "-" + getSFormat(-value);
        if (value < 1000) return String.valueOf(value);
        int exp = (int) (Math.log(value) / Math.log(1000));
        return String.format("%s%c", decformat.format(value / Math.pow(1000, exp)), "KMBTPE".charAt(exp - 1));
    }

    public String getLFormat(final long value) {
        final String sformat = getSFormat(value);
        return sformat.replace("M", " Mio.").replace("B", " Mrd.").replace("T", " Bio.").replace("K", " Tsd.");
    }

    public String getRandomWord() {
        return RandomStringUtils.randomAlphabetic(8).replaceAll("[0O]", "1");
    }

    public boolean startTPCountdown(final Player p, final Location l) {
        final String pname = p.getName();
        if (tpplayers.contains(pname)) {
            showItemBar(p, "§cEs läuft bereits eine Teleportierung...");
            return false;
        }
        final Server s = this.getServer();
        tpplayers.add(pname);
        tptasks.put(pname, new BukkitRunnable() {
            @Override
            public void run() {
                setLastLocation(p);
                teleportPlayer(p, l);
                tpplayers.remove(pname);
                if (history.getTeleportCache().containsKey(p)) {
                    final MuxHistory.CachedHistory ch = history.getTeleportCache().remove(p);
                    history.addHistory(ch.getUUID(), ch.getUUID2(), ch.getType(), ch.getEntry(), ch.getParam());
                }
                s.getScheduler().cancelTask(tptasks.remove(pname));
            }
        }.runTaskLater(this, 40L).getTaskId());
        return true;
    }

    public void setActiveInv(final String pname, final InvType it) {
        inv.setInv(pname, it);
    }

    public InvType getActiveInv(final String pname) {
        final InvType it = inv.getInv(pname);
        return it == null ? InvType.NOINTERACT : it;
    }

    public String clockTimeToString(long time) {
        return String.format("%02d:%02d", (time % 3600) / 60, (time % 60));
    }

    public String timeToString(final long time, final boolean shorten) {
        return timeToString(time, shorten, false);
    }

    public String timeToString(final long time, final boolean shorten, final boolean showSeconds) {
        final StringBuilder msg = new StringBuilder();
        long seconds = time / 1000L;
        int count = 0;
        final int[] units = {12 * 30 * 60 * 60 * 24, 30 * 60 * 60 * 24, 60 * 60 * 24, 60 * 60, 60, 1};
        final String[][] unitLabels = {
                {" Jahr, ", " Jahre, ", "J "},
                {" Monat, ", " Monate, ", "mo "},
                {" Tag, ", " Tage, ", "d "},
                {" Stunde, ", " Stunden, ", "h "},
                {" Minute, ", " Minuten, ", "m "},
                {" Sekunde, ", " Sekunden, ", "s "}
        };
        for (int i = 0; i < units.length && count < 2; i++) {
            if (seconds >= units[i] || (i == 5 && showSeconds)) {
                final long value = seconds / units[i];
                seconds %= units[i];
                msg.append(value).append(shorten ? unitLabels[i][2] : (value == 1 ? unitLabels[i][0] : unitLabels[i][1]));
                count++;
            }
        }
        if (msg.length() == 0) {
            msg.append(shorten ? "0s" : "0 Sekunden");
        } else {
            msg.setLength(msg.length() - (shorten ? 1 : 2));
        }
        return msg.toString();
    }

    public void removeOneItemFromHand(final Player p) {
        final ItemStack item = p.getItemInHand();
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            p.setItemInHand(item);
        } else {
            p.setItemInHand(null);
        }
    }

    public ItemStack renameItemStack(final ItemStack i, final String name, final String... lore) { // No Lore? Use new String[0]
        final ItemMeta im = i.getItemMeta();
        im.setDisplayName(name);
        if (lore.length > 0) {
            im.setLore(Arrays.asList(lore));
        }
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        i.setItemMeta(im);
        return i;
    }

    public ItemStack renameItemStack(final ItemStack i, final String name, final List<String> lore) { // No Lore? Use new String[0]
        final ItemMeta im = i.getItemMeta();
        im.setDisplayName(name);
        if (lore.isEmpty() == false) {
            im.setLore(lore);
        }
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        i.setItemMeta(im);
        return i;
    }

    public ItemStack setLore(final ItemStack i, final String... lore) {
        final ItemMeta im = i.getItemMeta();
        im.setLore(Arrays.asList(lore));
        i.setItemMeta(im);
        return i;
    }

    public ItemStack renameItemStackAddLore(final ItemStack i, final String name, final String... lore) {
        final ItemMeta im = i.getItemMeta();
        im.setDisplayName(name);
        i.setItemMeta(combineLore(im, Arrays.asList(lore)));
        return i;
    }

    public ItemStack addLore(final ItemStack i, final String... lore) {
        return addLore(i, Arrays.asList(lore));
    }

    public ItemStack addLore(final ItemStack i, final List<String> lore) {
        final ItemMeta im = i.getItemMeta();
        i.setItemMeta(combineLore(im, lore));
        return i;
    }

    private ItemMeta combineLore(final ItemMeta im, final List<String> lore) {
        List<String> oldLore = im.getLore();
        if (lore.isEmpty() == false) {
            if (oldLore != null) {
                oldLore.addAll(lore);
            } else {
                oldLore = lore;
            }
            im.setLore(oldLore);
        }
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        return im;
    }

    public ItemStack addEnchant(final ItemStack i, final Enchantment e, final int level) {
        i.addUnsafeEnchantment(e, level);
        return i;
    }

    public ItemStack addEnchants(final ItemStack i, final Map<Enchantment, Integer> ench) {
        i.addUnsafeEnchantments(ench);
        return i;
    }

    public void playEffect(final Player p, final EnumParticle effect, final Location loc, final float xoff, final float yoff, final float zoff, final float speed, final int amount) {
        final float x = (float) loc.getX(), y = (float) loc.getY(), z = (float) loc.getZ();
        final PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(effect, true, x, y, z, xoff, yoff, zoff, speed, amount, null);
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
    }

    public void playEffect(final EnumParticle effect, final Location loc, final float xoff, final float yoff, final float zoff, final float speed, final int amount) {
        final float x = (float) loc.getX(), y = (float) loc.getY(), z = (float) loc.getZ();
        final PacketPlayOutWorldParticles packet = new PacketPlayOutWorldParticles(effect, true, x, y, z, xoff, yoff, zoff, speed, amount, null);
        this.getServer().getOnlinePlayers().forEach(pl -> {
            if (pl.getWorld() == loc.getWorld() && pl.getLocation().distance(loc) <= 15D) {
                ((CraftPlayer) pl).getHandle().playerConnection.sendPacket(packet);
            }
        });
    }

    public boolean inBattle(final String s, final Location l) {
        final Long time = tagged.get(s);
        return time != null && time + 6000L >= System.currentTimeMillis() && WGBukkit.getRegionManager(l.getWorld()).getApplicableRegions(l).allows(DefaultFlag.PVP);
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isBeta() {
        return beta;
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    public void removeFromBattle(final String s) {
        tagged.remove(s);
    }

    private final Map<String, UUID> cacheduuids = new HashMap<>();
    private final Map<String, String> cachednames = new HashMap<>();

    public OfflinePlayer getPlayer(final String name) {
        return getPlayer(name, false);
    }

    public OfflinePlayer getPlayer(final String name, final boolean uuidcheck) {
        UUID uuid = null;
        if (uuidcheck && (name.length() == 32 || name.length() == 36)) {
            try {
                if (name.length() == 32)
                    uuid = UUID.fromString(name.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                else uuid = UUID.fromString(name);
            } catch (final Exception ignored) {
            }
            if (uuid != null) {
                return this.getServer().getOfflinePlayer(uuid);
            }
        }
        final MuxUser u = getMuxUser(name);
        if (u == null) {
            uuid = cacheduuids.get(name.toLowerCase());
            if (uuid == null) {
                uuid = db.getPlayerUUID(name).getValue();
                cacheduuids.put(name.toLowerCase(), uuid);
            }
        } else {
            uuid = u.getUUID();
        }
        if (uuid != null) {
            final OfflinePlayer pl = this.getServer().getOfflinePlayer(uuid);
            if (pl.getName() == null) return null;
            return pl;
        }
        return null;
    }

    public Entry<String, OfflinePlayer> getPlayerAndName(final String name) {
        UUID uuid;
        String n;
        final MuxUser u = getMuxUser(name);
        if (u == null) {
            final String lowercase = name.toLowerCase();
            uuid = cacheduuids.get(lowercase);
            n = cachednames.get(lowercase);
            if (uuid == null || n == null || n.isEmpty()) {
                final Entry<String, UUID> entry = db.getPlayerUUID(name);
                uuid = entry.getValue();
                n = entry.getKey();
                cacheduuids.put(lowercase, uuid);
                cachednames.put(lowercase, n);
            }
        } else {
            uuid = u.getUUID();
            n = u.getName();
        }
        if (uuid != null) {
            final OfflinePlayer pl = this.getServer().getOfflinePlayer(uuid);
            return new AbstractMap.SimpleEntry<>(n, pl);
        }
        return new AbstractMap.SimpleEntry<>(n, null);
    }

    public void clearChat(final Player p) {
        final StringBuilder cc = new StringBuilder();
        for (short i = 0; i < 40; i++) {
            cc.append("                                                                                                           ");
            p.sendMessage("་" + cc);
        }
    }

    public String header(final byte length, final String color) {
        return header(null, length, color);
    }

    public String footer(final byte length, final String color) {
        return footer(length, (byte) 0, color);
    }

    public String header(final String title, final byte length) {
        return header(title, length, "§b");
    }

    public String header(final String title, final byte length, final String color) {
        final StringBuilder out = new StringBuilder(color + "▛");
        for (byte i = 0; i < length; i++) out.append("▀");
        if (title != null) out.append(" §6§l").append(title).append("§r ").append(color);
        for (byte i = 0; i < length; i++) out.append("▀");
        return out.append("▜").toString();
    }

    public String footer(final byte length, final byte tlength) {
        return footer(length, tlength, "§b");
    }

    public String footer(final byte length, final byte tlength, final String color) {
        final StringBuilder out = new StringBuilder(color + "▙");
        final char c = (char) 9604;
        for (byte i = 0; i < length * 2 + tlength; i++) out.append(c);
        return out.append("▟").toString();
    }

    public enum EntType {
        XP, ARROW, ITEM, MINECART, BOAT
    }

    public enum Priority {
        LOW(1), NORMAL(2), HIGH(3), HIGHEST(4);

        private final int priority;

        Priority(final int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }
    }

    public static MuxSystem getAPI() {
        return plugin;
    }

    public void registerCustomEntity(final Class<? extends net.minecraft.server.v1_8_R3.Entity> clazz, final String entityname, final int entityid) {
        ((Map<Class<? extends net.minecraft.server.v1_8_R3.Entity>, String>) Objects.requireNonNull(NMSReflection.getObject(net.minecraft.server.v1_8_R3.EntityTypes.class, "d", null))).put(clazz, entityname);
        ((Map<Class<? extends net.minecraft.server.v1_8_R3.Entity>, Integer>) Objects.requireNonNull(NMSReflection.getObject(net.minecraft.server.v1_8_R3.EntityTypes.class, "f", null))).put(clazz, entityid);
    }

    public void unregisterCustomEntity(final Class<? extends net.minecraft.server.v1_8_R3.Entity> clazz) {
        ((Map<Class<? extends net.minecraft.server.v1_8_R3.Entity>, String>) Objects.requireNonNull(NMSReflection.getObject(net.minecraft.server.v1_8_R3.EntityTypes.class, "d", null))).remove(clazz);
        ((Map<Class<? extends net.minecraft.server.v1_8_R3.Entity>, Integer>) Objects.requireNonNull(NMSReflection.getObject(net.minecraft.server.v1_8_R3.EntityTypes.class, "f", null))).remove(clazz);
    }

    @Override
    public void onPluginMessageReceived(final String channel, final Player p, final byte[] b) {
        final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(b));
        votes.onPluginMessage(channel, dis);
        switch (channel) {
            case "BungeeUsers":
                try {
                    bungeeusers = dis.readShort();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
                break;
            case "WDL|INIT":
            case "":
                p.kickPlayer("§cWorld Downloader ist nicht erlaubt");
                break;
            case "LOLIMAHCKER":
                p.kickPlayer("§cDieser Client ist nicht erlaubt");
                break;
            case "LABYMOD":
                getMuxUser(p.getName()).setClient("Labymod 2");
                break;
            case "LMC":
                getMuxUser(p.getName()).setClient("Labymod 3");
                anticheat.setLabyMod(p);
                break;
            case "5zig_Set":
                getMuxUser(p.getName()).setClient("5zig");
                break;
            case "Lunar-Client":
                getMuxUser(p.getName()).setClient("Lunar Client");
                break;
            case "MC|Brand":
                String brand = new String(b, StandardCharsets.UTF_8);
                String[] brands = brand.replaceAll("[() \\[\\]]", "").replace("\n", "").split(",");
                for (final String name : brands) {
                    if (name.contains("forge")) { // Don't allow Forge
                        anticheat.setForge(p);
                    } else if (name.equals("LiteLoader")) {
                        getMuxUser(p.getName()).setClient("LiteLoader");
                    } else if (name.contains("Minecraft-Console-Client")) {
                        p.kickPlayer("§cDieser Client ist nicht erlaubt");
                    }
                }
                break;
            default:
                break;
        }
    }
}