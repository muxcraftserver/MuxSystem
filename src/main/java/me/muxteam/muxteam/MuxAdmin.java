package me.muxteam.muxteam;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import me.muxteam.base.PlayerBase;
import me.muxteam.basic.ConfirmInventory;
import me.muxteam.basic.MuxAntiBot;
import me.muxteam.basic.MuxAnvil;
import me.muxteam.basic.MuxBossBar;
import me.muxteam.basic.MuxChatInput;
import me.muxteam.extras.MuxBoosters.BoosterType;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxSystem.Priority;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.muxsystem.MySQL;
import me.muxteam.pvp.MuxClanWar;
import me.muxteam.pvp.MuxGames;
import me.muxteam.pvp.MuxPvP;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class MuxAdmin {
    public AdminOption PVP, ANTICHEAT, TRAINING, _1VS1, DUELS, CLANWAR, PETS, MOUNTS, EMOJIS, BASE, CASINO, CHESTS, GEMSHOP, ULTRA, AFFILIATE, FARMWORLD,
            BOOSTERS, AUTOEVENT, BASEGENERATE, EMAILS, HOLOGRAMS, VOTES, ANTIVPN, ANTIBOT, BOSSBAR, NEWBIEBROADCAST, AUTOBROADCASTS, SYSTEMRELOAD,
            DATASAVE, RELOADRANKS, AUTOSTOPLAG, REDSTONE, FIREWORKS, DROPSANDEXP, FORCEAWAY, CHECKFARMS, CHUNKUNLOAD, KILLMOBS, MINECARTSANDBOATS;

    private final Map<String, AdminOption> settingscache = new HashMap<>();
    private final List<String> settingshistory = new ArrayList<>();

    private MuxSystem ms;

    private final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");

    public MuxAdmin(final MuxSystem ms) {
        this.ms = ms;
        format.setTimeZone(TimeZone.getTimeZone("CET"));
        AUTOSTOPLAG = new AdminOption(AdminOptionCategory.PERFORMANCE, "Automatisches Stoplag", Material.ICE, true);
        REDSTONE = new AdminOption(AdminOptionCategory.PERFORMANCE, "Redstone", Material.REDSTONE, true);
        FIREWORKS = new AdminOption(AdminOptionCategory.PERFORMANCE, "Feuerwerke", Material.FIREWORK, true);
        DROPSANDEXP = new AdminOption(AdminOptionCategory.PERFORMANCE, "Drops & EXP-Flaschen", Material.EXP_BOTTLE, true,
                "§7Rechtsklick, um sie §nin dieser Welt§7 zu §eentfernen§7.");
        FORCEAWAY = new AdminOption(AdminOptionCategory.PERFORMANCE, "Automatisch Spawn entlasten", Material.BEACON, true);
        CHECKFARMS = new AdminOption(AdminOptionCategory.PERFORMANCE, "Automatisch Mobfarms leeren", Material.WHEAT, true, aBoolean -> {
            if (aBoolean) ms.getAntiLags().checkFarmLag(ms.getServer());
        });
        CHUNKUNLOAD = new AdminOption(AdminOptionCategory.PERFORMANCE, "Automatisch Chunks entladen", Material.EXPLOSIVE_MINECART, false, aBoolean -> {
            if (aBoolean) {
                ms.getAntiLags().startChunkCleanup(ms);
            } else {
                ms.getAntiLags().stopChunkCleanup(ms);
            }
        });
        KILLMOBS = new AdminOption(AdminOptionCategory.PERFORMANCE, "§e§lMobs töten", Material.RABBIT_HIDE, (short) 0, (p, bool) -> {
            final int mobs = ms.getAntiLags().killAll(p.getWorld());
            ms.showItemBar(p, "§fEs wurden §6" + mobs + " §fMobs §cgelöscht§f.");
        }, "", "§7Klicke, um sie §nin dieser Welt§7 zu §etöten§7.");
        MINECARTSANDBOATS = new AdminOption(AdminOptionCategory.PERFORMANCE, "§e§lMinecarts und Boote entfernen", Material.MINECART, (short) 0, (p, bool) -> {
            final int minecarts = ms.removeEntities(MuxSystem.EntType.MINECART, null, p, -1), boats = ms.removeEntities(MuxSystem.EntType.BOAT, null, p, -1);
            ms.showItemBar(p, "§fEs wurden §6" + minecarts + " §fMinecarts und §6" + boats + " §fBoote §cgelöscht§f.");
        }, "", "§7Klicke, um sie §nin dieser Welt§7 zu §eentfernen§7.");

        PVP = new AdminOption(AdminOptionCategory.PVP, "Globales PvP", Material.BOW, true, (player, aBoolean) -> togglePvP(player));
        PVP.setActive(pvpIsActive());
        ANTICHEAT = new AdminOption(AdminOptionCategory.PVP, "Anticheat", Material.IRON_HELMET, true);
        TRAINING = new AdminOption(AdminOptionCategory.PVP, "Training", Material.LEATHER_CHESTPLATE, true, aBoolean -> ms.getGames().getGame(MuxGames.GameType.TRAINING).setOpen(aBoolean));
        _1VS1 = new AdminOption(AdminOptionCategory.PVP, "1vs1", Material.GOLD_SWORD, true, "§fRechtsklick§7, um Arenen zu beenden.");
        DUELS = new AdminOption(AdminOptionCategory.PVP, "Duelle", Material.WOOD_SWORD, true, "§fRechtsklick§7, um Arenen zu beenden.");
        CLANWAR = new AdminOption(AdminOptionCategory.PVP, "Clanwar", Material.DIAMOND_SWORD, (short) 0,true,"§fRechtsklick§7, um Arenen zu beenden.");
        PETS = new AdminOption(AdminOptionCategory.EXTRAS, "Pets", Material.MONSTER_EGG, (short) 120, true, aBoolean -> {
            if (aBoolean == false)
                ms.getPets().deactivatePets();
        });
        MOUNTS = new AdminOption(AdminOptionCategory.EXTRAS, "Mounts", Material.IRON_BARDING, (short) 0, true, aBoolean -> {
            if (aBoolean == false)
                ms.getMounts().deactivateMounts();
        });
        EMOJIS = new AdminOption(AdminOptionCategory.EXTRAS, "Emojis", Material.SKULL_ITEM, (short) 3, true, aBoolean -> {
            if (aBoolean == false)
                ms.getEmojis().deactivateEmojis();

        });
        BASE = new AdminOption(AdminOptionCategory.FEATURES, "Basen", Material.SAPLING, true);
        CASINO = new AdminOption(AdminOptionCategory.FEATURES, "Casino", Material.INK_SACK, (short) 13, true, aBoolean -> {
            if (aBoolean == false)
                ms.getCasino().disable();
        });
        CHESTS = new AdminOption(AdminOptionCategory.FEATURES, "MuxKisten", Material.CHEST, true);
        GEMSHOP = new AdminOption(AdminOptionCategory.FEATURES, "GemShop", Material.EMERALD, true, aBoolean -> {
            if (aBoolean == false) {
                final Server sr = ms.getServer();
                ms.getInv().getInvs().entrySet().stream().filter(entry -> entry.getValue() == InvType.GEMSHOP)
                        .map(Map.Entry::getKey).map(sr::getPlayer).filter(Objects::nonNull).forEach(Player::closeInventory);
            }
        });
        ULTRA = new AdminOption(AdminOptionCategory.FEATURES, "ULTRA", Material.STAINED_CLAY, (short) 14, true);
        AFFILIATE = new AdminOption(AdminOptionCategory.FEATURES, "MuxWerben", Material.RED_MUSHROOM, true);
        BOOSTERS = new AdminOption(AdminOptionCategory.FEATURES, "Booster", Material.FIREWORK_CHARGE, (short) 0, true, aBoolean -> toggleBoosters());
        AUTOEVENT = new AdminOption(AdminOptionCategory.FEATURES, "Automatische Events", Material.STAINED_CLAY, (short) 2, true);
        BASEGENERATE = new AdminOption(AdminOptionCategory.FEATURES, "Basen generieren", Material.CLAY_BALL, true);
        EMAILS = new AdminOption(AdminOptionCategory.FEATURES, "Emails", Material.EMPTY_MAP, true, aBoolean -> {
            if (aBoolean == true) {
                ms.getEmails().loadEmailClient();
            }
        });
        FARMWORLD = new AdminOption(AdminOptionCategory.FEATURES, "Wildnis", Material.LONG_GRASS, (byte) 2, true, aBoolean -> {
            if (aBoolean == false)
                ms.getFarmWorld().getPlayers().forEach(player -> ms.forcePlayer(player, ms.getGameSpawn()));
        });
        HOLOGRAMS = new AdminOption(AdminOptionCategory.INFRASTRUCTURE, "Hologramme", Material.STAINED_GLASS, true, this::toggleHolograms);
        VOTES = new AdminOption(AdminOptionCategory.INFRASTRUCTURE, "Votes", Material.IRON_DOOR, true, (player, aBoolean) -> ms.getVotes().toggleVotes(player, aBoolean));
        ANTIVPN = new AdminOption(AdminOptionCategory.INFRASTRUCTURE, "Anti-VPN", "§7Mitglieder mit VPN werden gekickt.", Material.FENCE, (short) 0,  false, aBoolean -> toggleAntiVPN());
        ANTIBOT = new AdminOption(AdminOptionCategory.INFRASTRUCTURE, "Anti-Bot", "§7Alle Zweitaccounts werden gekickt.", Material.DISPENSER, (short) 0,  false, aBoolean -> toggleAntiBot());
        BOSSBAR = new AdminOption(AdminOptionCategory.INFRASTRUCTURE, "Bossbar", Material.NETHER_BRICK_ITEM, (short) 0, true, aBoolean -> toggleBossBar());
        NEWBIEBROADCAST = new AdminOption(AdminOptionCategory.INFRASTRUCTURE, "Neulingsbroadcast", Material.RAW_FISH, true);
        AUTOBROADCASTS = new AdminOption(AdminOptionCategory.INFRASTRUCTURE, "Werbebroadcast", Material.PAPER, true);

        SYSTEMRELOAD = new AdminOption(AdminOptionCategory.INFRASTRUCTURE, "§e§lSystem neu laden", Material.FISHING_ROD, (short) 0, (p, bool) -> {
            ms.reloadConfig();
            final FileConfiguration config = ms.getConfig();
            ms.getGames().loadLocs();
            ms.getVotes().reload();
            ms.loadConfigStuff(config);
            ms.getNPCS().respawn();
            ms.getCasino().respawn();
            if (ms.getEvents().isRunning() == false) ms.getGiants().respawn();
            ms.getStats().updateTop();
            ms.getHolograms().reload();
            try {
                ms.getShop().reloadShopFile();
            } catch (final Exception e) {
                e.printStackTrace();
            }
            ms.showItemBar(p, "§fDas §6MuxSystem §fwurde neu §ageladen§f.");
        }, "", "§7Klicke, um dieses §eneuzuladen§7.");
        DATASAVE = new AdminOption(AdminOptionCategory.INFRASTRUCTURE, "§e§lDaten abspeichern", Material.CAULDRON_ITEM, (short) 0, (p, bool) -> {
            final MySQL db = ms.getDB();
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (final Player online : ms.getServer().getOnlinePlayers()) {
                        final MuxUser u = ms.getMuxUser(online.getName());
                        if (u != null) {
                            db.savePlayer(u);
                            if (u.isGlobalUser()) db.saveGlobalPlayer(u);
                            online.saveData();
                        }
                    }
                    ms.getServer().savePlayers();
                    ms.getCasino().save();
                    ms.getChests().save();
                    db.saveLiquidity();
                    ms.showItemBar(p, "§fDie Spielerdaten wurden erfolgreich §agespeichert§f.");
                }
            }.runTaskAsynchronously(ms);
        }, "", "§7Klicke, um diese §eabzuspeichern§7.");
        RELOADRANKS = new AdminOption(AdminOptionCategory.INFRASTRUCTURE, "§e§lRänge aktualisieren", Material.NAME_TAG, (short) 0, (p, bool) -> {
            ms.broadcastMessage("§c§lMuxCraft>§7 Ränge wurden aktualisiert.", "muxsystem.op", Priority.NORMAL);
            ms.getPerms().reload();
        }, "", "§7Klicke, um diese zu §eaktualisieren§7.");
        setup();
    }

    private void setup() {
        if (ms.getHashYAML().get("adminoptionshistory") == null) return;
        final Gson gson = new Gson();
        settingshistory.addAll(gson.fromJson(ms.getHashYAML().getString("adminoptionshistory"), new TypeToken<ArrayList<String>>() {}.getType()));
    }

    public void close() {
        final Gson gson = new Gson();
        this.ms.getHashYAML().set("adminoptionshistory", gson.toJson(settingshistory));
        this.ms = null;
    }

    public boolean handleCommand(final Player p) {
        if (p.isOp() == false) {
            ms.sendNoCMDMessage(p);
            return true;
        }
        final InvType it = ms.getActiveInv(p.getName());
        if (it != InvType.ADMIN && it != InvType.HOLOGRAMS && it != InvType.POLLS && it != InvType.MENU && it != InvType.HISTORY && it != InvType.CONFIRM && it != InvType.FEEDBACK)
            p.closeInventory();
        p.openInventory(getAdminInv());
        ms.setActiveInv(p.getName(), InvType.ADMIN);
        return true;
    }

    public void resetHolograms() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (HOLOGRAMS.active) {
                    toggleHolograms(false);
                    toggleHolograms(true);
                }
            }
        }.runTask(ms);
    }

    public void handleInventory(final Player p, final int slot, final ItemStack i, final String title, final boolean rightclick) {
        this.handleInventory(p, slot, i, title, rightclick, -1);
    }

    public void handleInventory(final Player p, final int slot, final ItemStack i, final String title, final boolean rightclick, final int size) {
        if (slot == 4) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        }
        final Material m = i.getType();
        final String id = m.name() + ":" + i.getDurability();
        if (title != null && title.endsWith("Season")) {
            ms.getSeason().handleSeasonInventory(p, m, rightclick);
            return;
        } else if (size == 27) {
            if (m == Material.ITEM_FRAME) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.openInventory(getPvPSettings());
                ms.setActiveInv(p.getName(), InvType.ADMIN);
                return;
            } else if (m == Material.EMPTY_MAP && slot > 17 && title != null) {
                final String displayName = ChatColor.stripColor(i.getItemMeta().getDisplayName());
                final String[] splitted = ChatColor.stripColor(i.getItemMeta().getLore().get(0)).split(" ");
                if (title.endsWith("Duelle")) {
                    final String map = displayName.toLowerCase(), playerA = splitted[0], playerB = splitted[2];
                    for (final MuxPvP.Arena a : ms.getPvP().getArenas()) {
                        if (a.getMap().equals(map) && (a.getPlayerA().getName().equals(playerA) && a.getPlayerB().getName().equals(playerB))) {
                            ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "DUEL STOP", a.getPlayerA().getName() + " vs " + a.getPlayerB().getName());
                            a.disableArena();
                            p.openInventory(getDuelsInventory());
                            ms.setActiveInv(p.getName(), InvType.ADMIN);
                            return;
                        }
                    }
                } else if (title.endsWith("1vs1")) {
                    final String map = displayName.toLowerCase(), playerA = splitted[0], playerB = splitted[2];
                    for (final MuxPvP.RankedArena a : ms.getPvP().get1vs1Arenas()) {
                        if (a.getMap().equals(map) && (a.getPlayerA().getName().equals(playerA) && a.getPlayerB().getName().equals(playerB))) {
                            ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "1VS1 STOP", a.getPlayerA().getName() + " vs " + a.getPlayerB().getName());
                            a.disableArena();
                            p.openInventory(get1vs1Inventory());
                            ms.setActiveInv(p.getName(), InvType.ADMIN);
                            return;
                        }
                    }
                } else if (title.endsWith("Clanwar")) {
                    final String map = displayName.toLowerCase(), clanA = splitted[0], clanB = splitted[2];
                    for (final MuxClanWar.Arena a : ms.getClanWar().getArenas()) {
                        if (a.getMap().equals(map) && (a.getClanNameA().equals(clanA) && a.getClanNameB().equals(clanB))) {
                            ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "CLANWAR STOP", a.getClanNameA() + " vs " + a.getClanNameB());
                            a.disableArena();
                            p.openInventory(getClanwarInventory());
                            ms.setActiveInv(p.getName(), InvType.ADMIN);
                            return;
                        }
                    }
                }
            }
        }
        if (title != null && title.endsWith("Statistiken")) {
            if (m != Material.ITEM_FRAME) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
        } else if (title != null && title.endsWith("News")) {
            if (m == Material.NAME_TAG) {
                new MuxAnvil(ms, (input, player) -> {
                    final String ntitle = input.replace("Titel: ", "");
                    ms.setNewsTitle(ntitle);
                    ms.getConfig().set("news.title", ntitle);
                    ms.saveConfig();
                    openNews(player, false);
                    player.playSound(player.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                }).show(p, "Titel", new ItemStack(Material.NAME_TAG));
            } else if (m == Material.BOOK_AND_QUILL) {
                new MuxChatInput(ms, (input, player) -> {
                    ms.setNewsText(input);
                    ms.getConfig().set("news.text", input);
                    ms.saveConfig();
                    openNews(player, false);
                    player.playSound(player.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                }).show(p, "§f§lGebe den Text der News ein:");
            } else if (m == Material.TRIPWIRE_HOOK) {
                new MuxAnvil(ms, (input, player) -> {
                    final String link = ms.retardCleaner(input, "muxcraft.eu/");
                    ms.setNewsLink(link);
                    ms.getConfig().set("news.link", link);
                    ms.saveConfig();
                    player.playSound(player.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                    new MuxAnvil(ms, (input2, player2) -> {
                        ms.setNewsLinkText(input2 + " >");
                        ms.getConfig().set("news.linktext", input2 + " >");
                        ms.saveConfig();
                        openNews(player2, false);
                        player2.playSound(player2.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                    }).show(p, "Zu dem Link", new ItemStack(Material.EMPTY_MAP));
                }).show(p, "muxcraft.eu/", new ItemStack(Material.EMPTY_MAP));
            } else if (m == Material.EYE_OF_ENDER) {
                ms.sendNews(p, ms.getMuxUser(p.getName()), true);
                p.playSound(p.getLocation(), Sound.ENDERDRAGON_WINGS, 1F, 1.6F);
                return;
            } else if (m == Material.ITEM_FRAME) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.openInventory(getInfrastructureSettings());
                ms.setActiveInv(p.getName(), InvType.ADMIN);
                return;
            } else if (m == Material.SIGN || m == Material.STAINED_CLAY) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            } else if (m == Material.BARRIER) {
                ms.setNewsLink("");
                ms.setNewsText("");
                ms.setNewsLinkText("");
                ms.setNewsTitle("");
                ms.getConfig().set("news.link", "");
                ms.getConfig().set("news.linktext", "");
                ms.getConfig().set("news.text", "");
                ms.getDB().resetNews();
                for (final Player pl : ms.getServer().getOnlinePlayers()) {
                    ms.getMuxUser(pl.getName()).setNews(false);
                }
                ms.saveConfig();
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                openNews(p, false);
            }
            ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "ADMIN > NEWS", null);
            return;
        } else if (title != null && title.endsWith("Wartungen")) {
            if (m == Material.STAINED_CLAY) {
                final short data = i.getDurability();
                ms.getServer().setWhitelist(true);
                p.performCommand("a");
                if (data == (byte) 4) {
                    ms.logoutAll(p.getName(), "Wartungen");
                    ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "WHITELIST", "ON + LOGOUTALL");
                } else if (i.getDurability() == (byte) 5) {
                    ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "WHITELIST", "ON");
                }
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                return;
            }
        } else if (title != null && title.endsWith("Einstellungen")) {
            if (slot > 18 && slot < 28 && settingscache.containsKey(id)) {
                final AdminOption option = settingscache.get(id);
                if (option.equals(DUELS) && rightclick) {
                    p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                    p.openInventory(getDuelsInventory());
                    ms.setActiveInv(p.getName(), InvType.ADMIN);
                    return;
                } else if (option.equals(_1VS1) && rightclick) {
                    p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                    p.openInventory(get1vs1Inventory());
                    ms.setActiveInv(p.getName(), InvType.ADMIN);
                    return;
                } else if (option.equals(CLANWAR) && rightclick) {
                    p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                    p.openInventory(getClanwarInventory());
                    ms.setActiveInv(p.getName(), InvType.ADMIN);
                    return;
                }
                option.toggle(p, rightclick);
                final String stripped = ChatColor.stripColor(option.name);
                if (settingshistory.size() == 500) settingshistory.remove(499);
                settingshistory.add(0, stripped);
                ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "ADMIN > " + stripped, option.toggleOption ? option.active ? "Aktiviert" : "Deaktiviert" : null);
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                p.openInventory(getAdminSettings());
                ms.setActiveInv(p.getName(), InvType.ADMIN);
                return;
            }
            if (m == Material.ITEM_FRAME) {
                handleCommand(p);
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                return;
            }
            final Inventory open = m == Material.PAINTING ? getFeatureSettings() : m == Material.IRON_SWORD ? getPvPSettings() : m == Material.GOLD_BARDING ?
                    getExtraSettings() : m == Material.BUCKET ? getInfrastructureSettings() : null;
            if (open != null) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.openInventory(open);
                ms.setActiveInv(p.getName(), InvType.ADMIN);
            }
            return;
        } else if (title != null && settingscache.containsKey(id) && title.endsWith("/a") == false) {
            final AdminOption option = settingscache.get(id);
            if (option.equals(DUELS) && rightclick) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.openInventory(getDuelsInventory());
                ms.setActiveInv(p.getName(), InvType.ADMIN);
                return;
            } else if (option.equals(_1VS1) && rightclick) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.openInventory(get1vs1Inventory());
                ms.setActiveInv(p.getName(), InvType.ADMIN);
                return;
            } else if (option.equals(CLANWAR) && rightclick) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.openInventory(getClanwarInventory());
                ms.setActiveInv(p.getName(), InvType.ADMIN);
                return;
            }
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            if (option.equals(DROPSANDEXP) && rightclick) {
                final int items = ms.removeEntities(MuxSystem.EntType.ITEM, null, p, -1), exp = ms.removeEntities(MuxSystem.EntType.XP, null, p, -1);
                ms.showItemBar(p, "§fEs wurden §6" + exp + " §fEXP und §6" + items + " §fItems §cgelöscht§f.");
                ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "ADMIN > Item Clear", null);
                return;
            }
            option.toggle(p, rightclick);
            final String stripped = ChatColor.stripColor(option.name);
            if (option.category != AdminOptionCategory.PERFORMANCE) {
                if (settingshistory.size() == 500) settingshistory.remove(499);
                settingshistory.add(0, stripped);
            }
            ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "ADMIN > " + stripped, option.toggleOption ? option.active ? "Aktiviert" : "Deaktiviert" : null);
            p.openInventory(title.endsWith("PvP") ? getPvPSettings() : title.endsWith("Features") ? getFeatureSettings() : title.endsWith("Extras") ? getExtraSettings() : title.endsWith("Performance") ? getPerformanceSettings() : getInfrastructureSettings());
            ms.setActiveInv(p.getName(), InvType.ADMIN);
            return;
        }
        switch (m) {
            case DIODE:
                if (title != null) p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.openInventory(getAdminSettings());
                ms.setActiveInv(p.getName(), InvType.ADMIN);
                return;
            case POWERED_RAIL:
                if (title != null && title.contains("Performance")) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                if (title != null) p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.openInventory(getPerformanceSettings());
                ms.setActiveInv(p.getName(), InvType.ADMIN);
                return;
            case GOLDEN_CARROT:
                if (title != null && title.contains("Statistiken")) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                if (title != null) p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.openInventory(getAdminAnalyticsInv());
                ms.setActiveInv(p.getName(), InvType.ADMIN);
                return;
            case LEATHER: {
                openSpecialRanks(p, title);
                return;
            }
            // MAIN MENU
            case COMPASS:
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                p.performCommand("warps");
                p.closeInventory();
                return;
            case PUMPKIN_PIE:
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.performCommand("poll history");
                return;
            case RED_ROSE:
                ms.getFeedback().openFeedback(p, true);
                return;
            case BOOK:
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.performCommand("history");
                return;
            case STAINED_GLASS:
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.performCommand("holo");
                return;
            case BOWL:
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                new ConfirmInventory(ms, player -> {
                    ms.resetLimit("STOP");
                    ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "RESTART", null);
                    ms.logoutAll("CONSOLE", ms.getLang("restart"));
                    ms.getServer().shutdown();
                }, player -> player.performCommand("a")).show(p, "§0§lServer neustarten", "§aBestätigen", "§cAbbrechen");
                return;
            case BEDROCK: {
                if (ms.getServer().hasWhitelist()) {
                    ms.getServer().setWhitelist(false);
                    p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                    ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "WHITELIST", "OFF");
                    p.performCommand("a");
                } else {
                    if (title != null) p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                    final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxAdmin§0 | Wartungen");
                    inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
                    inv.setItem(4, ms.renameItemStack(new ItemStack(Material.BEDROCK), "§f§lWartungen",
                            "§7In diesem Fall können keine weiteren", "§7Spieler mehr den Server betreten."));
                    ms.createButton(inv, 19, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 5), "§aNur Wartungen aktivieren"));
                    ms.createButton(inv, 23, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 4), "§eWartungen & Alle Spieler ausloggen"));
                    if (ms.getActiveInv(p.getName()) != InvType.ADMIN) p.closeInventory();
                    p.openInventory(inv);
                    ms.setActiveInv(p.getName(), InvType.ADMIN);
                }
                return;
            }
            case ITEM_FRAME:
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                if (title != null && title.contains("/a")) {
                    p.performCommand("menu");
                    return;
                }
                p.openInventory(title != null && (title.endsWith("PvP") || title.endsWith("Features") || title.endsWith("Extras") || title.endsWith("Infrastruktur")) ? getAdminSettings() : getAdminInv());
                ms.setActiveInv(p.getName(), InvType.ADMIN);
                return;
            // CUSTOM OPTIONS
            case SIGN:
                if (ms.isTrusted(p.getName()) == false) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                openNews(p, true);
                return;
            case ARMOR_STAND:
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                if (ms.getSeason().getSeasonEnd() > System.currentTimeMillis()) {
                    ms.getSeason().stopSeason(p, false);
                    return;
                }
                ms.getSeason().resetSeasonStart(p);
                ms.getSeason().openSeasonStartInventory(p);
                return;
            case WOOD_STAIRS:
                if (ms.isTrusted(p.getName()) == false) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                if (ms.getJoinMSG().equals(".")) {
                    new MuxChatInput(ms, (input, player) -> {
                        ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "ADMIN > JOINMSG", input.equals(".") ? "REMOVE" : ChatColor.translateAlternateColorCodes('&', input));
                        ms.getConfig().set("joinmsg", input);
                        ms.saveConfig();
                        ms.setJoinMSG(ChatColor.translateAlternateColorCodes('&', input));
                        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                        if (input.equals(".")) {
                            ms.showItemBar(p, "§fDie Join Nachricht wurde §centfernt§f.");
                            return;
                        }
                        player.sendMessage(ms.getJoinMSG());
                    }).show(p, "§f§lGebe die neue Join Nachricht ein:");
                    return;
                } else {
                    ms.setJoinMSG(".");
                    ms.getConfig().set("joinmsg", ".");
                    ms.saveConfig();
                    p.openInventory(getInfrastructureSettings());
                    ms.setActiveInv(p.getName(), InvType.ADMIN);
                }
                break;
            case IRON_TRAPDOOR:
                addSlots(rightclick ? -5 : 5);
                ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "ADMIN > Slots", String.valueOf(ms.getSlots()));
                p.openInventory(getInfrastructureSettings());
                ms.setActiveInv(p.getName(), InvType.ADMIN);
                break;
            case EYE_OF_ENDER:
                setPlayerView(p, rightclick);
                break;
            default:
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
        }
        if (title == null) return;
        else if (title.endsWith("Einstellungen")) {
            handleInventory(p, -1, new ItemStack(Material.DIODE), null, false);
        } else if (title.endsWith("Performance")) {
            handleInventory(p, -1, new ItemStack(Material.POWERED_RAIL), null, false);
        } else if (title.endsWith("Statistiken")) {
            handleInventory(p, -1, new ItemStack(Material.GOLDEN_CARROT), null, false);
        }
        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
    }

    private void openSpecialRanks(final Player p, final String title) {
        if (title != null) p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxAdmin§0 | Besondere Ränge");
        final Set<UUID> f = ms.getPerms().getMembersOfGroup("Friend"), y = ms.getPerms().getMembersOfGroup("Creator"), y2 = ms.getPerms().getMembersOfGroup("Creator+");
        final Set<String> freunde = new HashSet<>(), ytber = new HashSet<>();
        f.forEach(uuid -> {
            final String n = ms.getServer().getOfflinePlayer(uuid).getName();
            if (n != null) freunde.add(n);
        });
        y.forEach(uuid -> {
            final String n = ms.getServer().getOfflinePlayer(uuid).getName();
            if (n != null) ytber.add(n);
        });
        y2.forEach(uuid -> {
            final String n = ms.getServer().getOfflinePlayer(uuid).getName();
            if (n != null) ytber.add(n);
        });
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.LEATHER), "§c§lBesondere Ränge", "§7Diejenigen, die spezielle Ränge oder", "§7Rechte haben, werden aufgelistet."));
        inv.setItem(19, ms.addLore(ms.renameItemStack(new ItemStack(Material.MELON), "§c§lTrusted"), ms.getLoreFromLongText(getTrusted(), "")));
        inv.setItem(21, ms.addLore(ms.renameItemStack(new ItemStack(Material.GOLDEN_APPLE), "§e§lOperator"), ms.getLoreFromLongText(getOPs(), "")));
        inv.setItem(23, ms.addLore(ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 12), "§b§lFriend"), ms.getLoreFromLongText("§3§o" + String.join("§7, §3§o", freunde), "")));
        inv.setItem(25, ms.addLore(ms.renameItemStack(new ItemStack(Material.RED_ROSE, 1, (byte) 2), "§d§lCreator"), ms.getLoreFromLongText("§5§o" + String.join("§7, §5§o", ytber), "")));
        if (ms.getActiveInv(p.getName()) != InvType.ADMIN) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.ADMIN);
    }

    private Inventory getAdminInv() {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxAdmin§0 | /a");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 1), "§c§lAdministration",
                "§7Manage den Server, damit dieser", "§7absolut einwandfrei funktioniert."));
        inv.setItem(8, ms.renameItemStack(new ItemStack(Material.GOLDEN_CARROT), "§6§lStatistiken",
                "§7Alle wichtigen Daten zum Server", "§7werden anschaulich dargestellt.", "", "§6Klicke§7, um diese zu sehen."));
        final boolean activatedoptions = settingscache.values().stream().anyMatch(option -> option.isChanged() && option.category != AdminOptionCategory.PERFORMANCE);
        inv.setItem(19, ms.renameItemStack(activatedoptions ? ms.addGlow(new ItemStack(Material.DIODE)) : new ItemStack(Material.DIODE), "§c§lEinstellungen",
                "§7Wenn es notwendig ist, kannst du hier", "§7die Modifikation des Servers ändern.", "", "§cKlicke§7, um alle Optionen zu sehen."));
        final List<String> performanceoptions = buildLore(AdminOptionCategory.PERFORMANCE);
        performanceoptions.set(performanceoptions.size() - 1, "§cKlicke§7, um alle Optionen zu sehen.");
        inv.setItem(20, ms.addLore(ms.renameItemStack(performanceoptions.size() > 2 ? ms.addGlow(new ItemStack(Material.POWERED_RAIL)) : new ItemStack(Material.POWERED_RAIL), "§c§lPerformance",
                "§7Verbessere in diesem Bereich sofort", "§7die generelle Leistung des Servers."), performanceoptions));
        inv.setItem(22, ms.renameItemStack(new ItemStack(Material.BOOK), "§c§lVerlauf",
                "§7Hiermit werden alle Aktionen gelistet,", "§7die von Teamler ausgeführt wurden.", "", "§cKlicke§7, um den Verlauf zu sehen."));
        inv.setItem(24, ms.renameItemStack(new ItemStack(Material.PUMPKIN_PIE), "§c§lUmfragen",
                "§7Alle Umfragen, die zuletzt gestellt", "§7wurden, sind komplett aufgelistet.", "", "§cKlicke§7, um die Umfragen zu sehen."));
        final int feedbacks = ms.getFeedback().getOpenFeedbacks();
        inv.setItem(25, ms.renameItemStack(new ItemStack(Material.RED_ROSE, Math.max(1, Math.min(64, feedbacks)), (byte) 5),
                "§c§lFeedback" + (feedbacks > 0 ? " §7» §c" + feedbacks : ""), "§7Die Vorschläge der Spieler helfen", "§7den Server besser anzupassen.", "",
                feedbacks == 0 ? "§aEs gibt keine offene Vorschläge." : "§cKlicke§7, um den Feedback zu sehen."));
        inv.setItem(37, ms.renameItemStack(new ItemStack(Material.BOWL), "§e§lNeustart",
                "§7Benutze die Option nur in Notsituationen", "§7oder bei einer geringen Nutzerzahl.", "", "§7Klicke, um den Server §eneuzustarten§7."));
        final boolean whitelist = ms.getServer().hasWhitelist();
        inv.setItem(38, ms.renameItemStack(whitelist ? ms.addGlow(new ItemStack(Material.BEDROCK)) : new ItemStack(Material.BEDROCK), "§f§lWartungen" + (whitelist ? " §a§laktiviert" : ""),
                "§7In diesem Fall können keine weiteren", "§7Spieler mehr den Server betreten.", "", "§7Klicke, um sie jetzt zu " + (whitelist ? "§cde" : "§f") + "aktivieren§7."));
        inv.setItem(40, ms.renameItemStack(new ItemStack(Material.LEATHER), "§c§lBesondere Ränge",
                "§7Diejenigen, die spezielle Ränge oder", "§7Rechte haben, werden aufgelistet.", "", "§cKlicke§7, um die Spieler zu sehen."));
        inv.setItem(42, ms.renameItemStack(new ItemStack(Material.STAINED_GLASS), "§c§lHologramme",
                "§7Hier sind alle Hologramme aufgelistet mit", "§7der Möglichkeit, neue zu erstellen.", "", "§cKlicke§7, um die Hologramme zu sehen."));
        inv.setItem(43, ms.renameItemStack(new ItemStack(Material.COMPASS), "§c§lWarps",
                "§7In diesem Bereich sind alle Orte", "§7die für Spieler zugänglich sind.", "", "§cKlicke§7, um die Warps zu sehen."));
        return inv;
    }

    private Inventory getAdminSettings() {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxAdmin§0 | Einstellungen");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));

        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.DIODE), "§c§lEinstellungen",
                "§7Wenn es notwendig ist, kannst du hier", "§7die Modifikation des Servers ändern."));

        final List<String> infrastructure = buildLore(AdminOptionCategory.INFRASTRUCTURE), features = buildLore(AdminOptionCategory.FEATURES),
                pvp = buildLore(AdminOptionCategory.PVP), extras = buildLore(AdminOptionCategory.EXTRAS);
        inv.setItem(37, ms.renameItemStack(infrastructure.size() > 2 ? ms.addGlow(new ItemStack(Material.BUCKET)) : new ItemStack(Material.BUCKET),
                "§c§lInfrastruktur", buildLore(AdminOptionCategory.INFRASTRUCTURE)));
        inv.setItem(39, ms.renameItemStack(features.size() > 2 ? ms.addGlow(new ItemStack(Material.PAINTING)) : new ItemStack(Material.PAINTING),
                "§c§lFeatures", buildLore(AdminOptionCategory.FEATURES)));
        inv.setItem(41, ms.renameItemStack(pvp.size() > 2 ? ms.addGlow(new ItemStack(Material.IRON_SWORD)) : new ItemStack(Material.IRON_SWORD),
                "§c§lPvP", buildLore(AdminOptionCategory.PVP)));
        inv.setItem(43, ms.renameItemStack(extras.size() > 2 ? ms.addGlow(new ItemStack(Material.GOLD_BARDING)) : new ItemStack(Material.GOLD_BARDING),
                "§c§lExtras", buildLore(AdminOptionCategory.EXTRAS)));

        ms.getForkJoinPool().execute(() -> {
            final ImmutableList<String> list = ImmutableList.copyOf(this.settingshistory);
            final AdminOptionComparator comparator = new AdminOptionComparator();
            int slot = 19;
            for (final AdminOption option : settingscache.values()
                    .stream()
                    .filter(option -> option.category != AdminOptionCategory.PERFORMANCE)
                    .sorted((o1, o2) -> comparator.compare(o1, o2, getUsedAmount(o1, list), getUsedAmount(o2, list)))
                    .limit(7)
                    .collect(Collectors.toList())) {
                inv.setItem(slot, option.getItem());
                slot++;
            }
        });
        return inv;
    }

    class AdminOptionComparator {
        public int compare(final AdminOption o1, final AdminOption o2, final long o1Used, final long o2Used) {
            if (o1.isChanged() && o2.isChanged() == false)
                return -1;
            if (o1.isChanged() == false && o2.isChanged())
                return 1;
            if (o1.isChanged() && o2.isChanged())
                return 0;
            return Long.compare(o2Used, o1Used);
        }
    }

    private long getUsedAmount(final AdminOption option, final ImmutableList<String> list) {
        final String stripped = ChatColor.stripColor(option.name);
        return list.stream().filter(s -> s.equals(stripped)).count();
    }

    private List<String> buildLore(final AdminOptionCategory category) {
        final List<String> lore = new ArrayList<>(Collections.singletonList(""));
        lore.addAll(settingscache.values().stream()
                .filter(option -> option.isChanged() && option.category == category)
                .map(option -> {
                    final String activeString = (option.defstate && option.active) ? "" : (option.defstate == false && option.active) ? "§f §aaktiviert" : (option.defstate) ? "§f §cdeaktiviert" : "";
                    return "§f" + option.name + activeString;
                })
                .collect(Collectors.toList()));
        if (lore.size() > 1) lore.addAll(Arrays.asList("", "§cKlicke§7, um die Optionen zu sehen."));
        else lore.add("§cKlicke§7, um die Optionen zu sehen.");
        return lore;
    }

    private Inventory getFeatureSettings() {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxAdmin§0 | Features");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.PAINTING), "§c§lFeatures"));
        inv.setItem(19, BASE.getItem());
        inv.setItem(20, CASINO.getItem());
        inv.setItem(21, CHESTS.getItem());
        inv.setItem(22, GEMSHOP.getItem());
        inv.setItem(23, ULTRA.getItem());
        inv.setItem(24, AFFILIATE.getItem());
        inv.setItem(25, BOOSTERS.getItem());
        inv.setItem(28, AUTOEVENT.getItem());
        inv.setItem(29, BASEGENERATE.getItem());
        inv.setItem(30, EMAILS.getItem());
        inv.setItem(31, FARMWORLD.getItem());
        return inv;
    }

    private Inventory getPvPSettings() {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxAdmin§0 | PvP");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.IRON_SWORD), "§c§lPvP"));
        inv.setItem(19, PVP.getItem());
        inv.setItem(20, ANTICHEAT.getItem());
        inv.setItem(21, TRAINING.getItem());
        inv.setItem(23, _1VS1.getItem());
        inv.setItem(24, DUELS.getItem());
        inv.setItem(25, CLANWAR.getItem());
        return inv;
    }

    private Inventory get1vs1Inventory() {
        final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lMuxAdmin§0 | 1vs1");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.setLore(ms.renameItemStack(_1VS1.getItem(), "§f§l1vs1")));
        int slot = 18;
        for (final MuxPvP.RankedArena arena : ms.getPvP().get1vs1Arenas()) {
            if (arena.isFinished() || arena.getPlayerB() == null || arena.getPlayerA() == null) continue;
            inv.setItem(slot, ms.renameItemStack(new ItemStack(Material.EMPTY_MAP), "§f§l" + arena.getMap().toUpperCase(), "§7" + arena.getPlayerA().getName() + " vs " + arena.getPlayerB().getName(), "", "§7Klicke, um §cabzubrechen§7."));
            slot++;
        }
        if (slot == 18) inv.setItem(22, ms.renameItemStack(new ItemStack(Material.BARRIER), "§c§lKeine Arenen für 1vs1 besetzt"));
        return inv;
    }

    private Inventory getDuelsInventory() {
        final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lMuxAdmin§0 | Duelle");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.setLore(ms.renameItemStack(DUELS.getItem(), "§f§lDuelle")));
        int slot = 18;
        for (final MuxPvP.Arena arena : ms.getPvP().getArenas()) {
            if (arena.isFinished() || arena.getPlayerB() == null || arena.getPlayerA() == null) continue;
            inv.setItem(slot, ms.renameItemStack(new ItemStack(Material.EMPTY_MAP), "§f§l" + arena.getMap().toUpperCase(), "§7" + arena.getPlayerA().getName() + " vs " + arena.getPlayerB().getName(), "", "§7Klicke, um §cabzubrechen§7."));
            slot++;
        }
        if (slot == 18) inv.setItem(22, ms.renameItemStack(new ItemStack(Material.BARRIER), "§c§lKeine Arenen für Duell besetzt"));
        return inv;
    }

    private Inventory getClanwarInventory() {
        final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lMuxAdmin§0 | Clanwar");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.setLore(ms.renameItemStack(CLANWAR.getItem(), "§f§lClanwar")));
        int slot = 18;
        for (final MuxClanWar.Arena arena : ms.getClanWar().getArenas()) {
            if (arena.isFinished()) continue;
            inv.setItem(slot, ms.renameItemStack(new ItemStack(Material.EMPTY_MAP), "§f§l" + arena.getMap().toUpperCase(), "§7" + arena.getClanNameA() + " vs " + arena.getClanNameB(), "§7Klicke, um §cabzubrechen§7."));
            slot++;
        }
        if (slot == 18) inv.setItem(22, ms.renameItemStack(new ItemStack(Material.BARRIER), "§c§lKeine Arenen für ClanWar besetzt"));
        return inv;
    }

    private Inventory getExtraSettings() {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxAdmin§0 | Extras");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.GOLD_BARDING), "§c§lExtras"));
        inv.setItem(19, PETS.getItem());
        inv.setItem(22, MOUNTS.getItem());
        inv.setItem(25, EMOJIS.getItem());
        return inv;
    }

    public Inventory getInfrastructureSettings() {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxAdmin§0 | Infrastruktur");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.BUCKET), "§c§lInfrastruktur"));
        inv.setItem(19, ms.renameItemStack(new ItemStack(Material.IRON_TRAPDOOR), "§f§lServer Slots §7» §8" + ms.getServer().getOnlinePlayers().size() + "/§6" + ms.getSlots(), "", "§7Linksklick, um §a5 hinzuzufügen§7.", "§7Rechtsklick, um §c5 zu entfernen§7."));
        inv.setItem(20, ms.renameItemStack(newsAreActive() ? ms.addGlow(new ItemStack(Material.SIGN)) : new ItemStack(Material.SIGN), "§f§lNews" + (newsAreActive() ? "§a §laktiviert" : ""), "", "§fKlicke§7, um zu öffnen."));
        inv.setItem(21, ms.renameItemStack(new ItemStack(Material.WOOD_STAIRS), "§f§lJoinnachricht", (ms.getJoinMSG().equals(".") == false ? Arrays.asList("", "§f" + ms.getJoinMSG(), "", "§7Klicke, um zu §centfernen§7.") : Arrays.asList("", "§7Klicke, um zu §fsetzen§7."))));
        inv.setItem(22, HOLOGRAMS.getItem());
        inv.setItem(23, SYSTEMRELOAD.getItem());
        inv.setItem(24, DATASAVE.getItem());
        inv.setItem(25, RELOADRANKS.getItem());
        inv.setItem(28, VOTES.getItem());
        inv.setItem(29, ANTIVPN.getItem());
        inv.setItem(30, ANTIBOT.getItem());
        inv.setItem(31, BOSSBAR.getItem());
        inv.setItem(32, NEWBIEBROADCAST.getItem());
        inv.setItem(33, AUTOBROADCASTS.getItem());
        final boolean seasonstarted = ms.getSeason().getSeasonEnd() > System.currentTimeMillis();
        final ItemStack season = ms.renameItemStack(new ItemStack(Material.ARMOR_STAND), "§f§lSeason" + (seasonstarted == false ? "" : " §a§lgestartet"),
                "", (seasonstarted == false ? "§7Klicke, um eine neue zu §astarten§7." : ("§7Sie läuft bis zum §a" + format.format(new Date(ms.getSeason().getSeasonEnd())) + "§7.")));
        if (seasonstarted) {
            ms.addLore(season, "", "§7Klicke§7, um sie zu §cbeenden§7.");
        }
        inv.setItem(34, seasonstarted ? ms.addGlow(season) : season);
        return inv;
    }

    private Inventory getPerformanceSettings() {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxAdmin§0 | Performance");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.POWERED_RAIL), "§c§lPerformance",
                "§7Verbessere in diesem Bereich sofort", "§7die generelle Leistung des Servers."));
        final int range = ((CraftWorld) ms.getServer().getWorlds().get(0)).getHandle().spigotConfig.playerTrackingRange;
        inv.setItem(19, ms.renameItemStack(range != 16 ? ms.addGlow(new ItemStack(Material.EYE_OF_ENDER)) : new ItemStack(Material.EYE_OF_ENDER),
                "§f§lSichtweite §7» §6" + range, "", "§aLinksklick§7, um zu erhöhen.", "§cRechtsklick§7, um zu verringern."));
        inv.setItem(20, AUTOSTOPLAG.getItem());
        inv.setItem(21, REDSTONE.getItem());
        inv.setItem(22, FIREWORKS.getItem());
        inv.setItem(23, DROPSANDEXP.getItem());
        inv.setItem(24, KILLMOBS.getItem());
        inv.setItem(25, MINECARTSANDBOATS.getItem());
        inv.setItem(28, FORCEAWAY.getItem());
        inv.setItem(29, CHECKFARMS.getItem());
        inv.setItem(30, CHUNKUNLOAD.getItem());
        return inv;
    }

    public class AdminOption {
        private final AdminOptionCategory category;
        private final String name;
        private final boolean defstate, toggleOption;
        private final Material material;
        private final short data;
        private final List<String> loreText;
        private boolean active;
        private String infoText;
        private Consumer<Boolean> onToggle = null;
        private BiConsumer<Player, Boolean> onClick;

        public AdminOption(final AdminOptionCategory category, final String name, final Material m, final boolean defstate, final Consumer<Boolean> onToggle, final String... loreText) {
            this(category, name, m, (short) 0, defstate, onToggle, loreText);
        }

        public AdminOption(final AdminOptionCategory category, final String name, final Material m, final boolean defstate, final String... loreText) {
            this(category, name, m, (short) 0, defstate, loreText);
        }

        public AdminOption(final AdminOptionCategory category, final String name, final Material m, final short data, final boolean defstate, final Consumer<Boolean> onToggle, final String... loreText) {
            this(category, name, m, data, defstate, loreText);
            this.onToggle = onToggle;
        }

        public AdminOption(final AdminOptionCategory category, final String name, final Material m, final boolean defstate, final BiConsumer<Player, Boolean> onClick, final String... loreText) {
            this(category, name, m, (short) 0, defstate, loreText);
            this.onClick = onClick;
        }

        public AdminOption(final AdminOptionCategory category, final String name, final String infoText, final Material m, final short data, final boolean defstate, final Consumer<Boolean> onToggle, final String... loreText) {
            this(category, name, m, data, defstate, onToggle, loreText);
            this.infoText = infoText;
        }

        public AdminOption(final AdminOptionCategory category, final String name, final Material m, final short data, final boolean defstate, final String... loreText) {
            this.name = name;
            this.material = m;
            this.data = data;
            this.loreText = Arrays.asList(loreText);
            this.infoText = null;
            this.category = category;
            this.toggleOption = true;
            this.defstate = defstate;
            this.active = defstate;
            settingscache.put(material.name() + ":" + data, this);
        }

        public AdminOption(final AdminOptionCategory category, final String name, final Material m, final short data, final BiConsumer<Player, Boolean> onClick, final String... loreText) {
            this.name = name;
            this.material = m;
            this.data = data;
            this.loreText = Arrays.asList(loreText);
            this.infoText = null;
            this.category = category;
            this.toggleOption = false;
            this.defstate = false;
            this.onClick = onClick;
            settingscache.put(material.name() + ":" + data, this);
        }

        public ItemStack getItem() {
            if (toggleOption == false) {
                return ms.renameItemStack(new ItemStack(material, 1, data), name, this.loreText);
            }
            final String activeString = (defstate && active) ? "" : (defstate == false && active) ? "§f §a§laktiviert" : (defstate) ? "§f §c§ldeaktiviert" : "",
                    clickToActivate = "§7" + (loreText.isEmpty() == false ? "Linksklick" : "Klicke") + ", um zu " + (active ? "§cde" : "§a") + "aktivieren§7.";
            final List<String> lore = new ArrayList<>();
            if (infoText != null) lore.add(infoText);
            lore.add("");
            lore.add(clickToActivate);
            if (loreText.isEmpty() == false) lore.addAll(loreText);
            ItemStack item = ms.renameItemStack(name.equals("Emojis") ? ms.getEmojis().GRIN4("§f§lEmojis") : new ItemStack(material, 1, data), "§f§l" + name + activeString, lore);
            if (activeString.isEmpty() == false) {
                item = ms.addGlow(item);
            }
            return item;
        }

        public boolean isChanged() {
            return active != defstate;
        }

        public boolean isActive() {
            return this.active;
        }

        public void toggle(final Player p, final boolean rightclick) {
            if (this.onClick != null)
                this.onClick.accept(p, rightclick);
            if (this.toggleOption == false) {
                return;
            }
            this.active ^= true;
            if (this.onToggle != null)
                this.onToggle.accept(this.active);
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }

    enum AdminOptionCategory {
        FEATURES,
        PVP,
        EXTRAS,
        INFRASTRUCTURE,
        PERFORMANCE
    }

    private final Map<String, Integer> popularlocs = new HashMap<>();

    public void resetCurrentLocations() {
        popularlocs.clear();
    }

    public void addCurrentLocation(final String location) {
        final int index = location.indexOf("§");
        String loc = location.substring(index + 2);
        if (loc.contains("Base")) loc = "Base";
        else if (loc.contains("Warp")) loc = "Warp";
        popularlocs.put(loc, popularlocs.getOrDefault(loc, 0) + 1);
    }
    private Map<String, Integer> getSortedLocations() {
        return popularlocs.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private Inventory getAdminAnalyticsInv() {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxAdmin§0 | Statistiken");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.GOLDEN_CARROT), "§6§lStatistiken",
                "§7Alle wichtigen Daten zum Server", "§7werden anschaulich dargestellt."));
        new BukkitRunnable() {
            @Override public void run() {
                final Object[] globalstats = ms.getDB().getServerStats();
                final MuxAnalytics ma = ms.getAnalytics();
                inv.setItem(22, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT), "§e§lWährungen",
                        "§7Geldmenge: §e" + ms.getSFormat((long) globalstats[1]) + "§7, Aktive: §e" + ms.getSFormat(ms.getActiveMuxCoinsSupply()),
                        "§7Chipmenge: §d" + ms.getSFormat((long) globalstats[2]) + "§7, Aktive: §d" + ms.getSFormat(ms.getActiveChipsSupply()),
                        "§7Gemmenge: §a" + ms.getSFormat((long) globalstats[4]) + "§7, Gespendet: §a" + ms.getSFormat(ma.getPurchasedGems()),
                        "§7GemShop: §a" + ms.getSFormat(ma.getGemProfits()),
                        "§7Ausgaben:",
                        "  §71vs1: §c" + ms.getSFormat(ma.getPvPExpenses()) + "§7, Events: §c" + ms.getSFormat(ma.getEventExpenses()) + "§7, Kisten: §c" + ms.getSFormat(ma.getChestExpenses()),
                        "  §7Erze: §c" + ms.getSFormat(ma.getMineGenerated()) + "§7, Vote: §c" + ms.getSFormat(ma.getVoteExpenses()) + "§7, Werben: §c" + ms.getSFormat(ma.getAffiliateExpenses())));
                final Analytics a = ma.getAnalytics();
                final int today = a.getDayOfYear();
                final Date date = new Date(System.currentTimeMillis());
                final String dateStr = format.format(date);
                inv.setItem(19, ms.renameItemStack(new ItemStack(Material.BOOK), "§f§l" + dateStr,
                        "§7Beigetreten: §f" + a.getJoinedPlayers().size() + " Spieler",
                        "§7Neu: §f" + a.getNewPlayers().size() + " Spieler" + " (" + ms.getDecimalFormat(a.getPercentageNew()) + "%)",
                        "§7Gevoted: §f" + ms.getVotes().getVoted() + " Spieler" + " (" + ms.getDecimalFormat(getPercentageVoted(ms.getVotes().getVoted(), a.getPlayers().size())) + "%)",
                        "§7Geworben: §f" + a.getAffiliate() + " Spieler",
                        "§7Spielzeit:§f " + ms.timeToString(a.getAveragePlayTime(), false)));
                // final Analytics ay = ay = ma.loadAnalytics(a.getDayOfYear() - 1);
                // final int returning = a.getReturnPlayers(ay.getNewPlayers());
                // "§7Neulinge von Gestern: §f" + returning + " (" + ms.getDecimalFormat(ay.getPercentageReturn(returning)) + "%)"
                final List<String> locslore = new ArrayList<>();
                for (final Map.Entry<String, Integer> entry : getSortedLocations().entrySet()) {
                    locslore.add(String.format("§7%s - §f%d Spieler", entry.getKey(), entry.getValue()));
                }
                locslore.addAll(Arrays.asList("", "§7Spieler AFK: §f" + Bukkit.getOnlinePlayers().stream().filter(player -> ms.isAFK(player.getName())).count() + " §dSpieler"));
                inv.setItem(25, ms.addLore(ms.renameItemStack(new ItemStack(Material.COMPASS), "§f§lBeliebte Orte"), locslore));

                // LAST 10 DAYS
                final List<String> lore = new ArrayList<>(), lore2 = new ArrayList<>(), lore3 = new ArrayList<>(), lore4 = new ArrayList<>(),
                        lore5 = new ArrayList<>(), lore6 = new ArrayList<>(), lore7 = new ArrayList<>(), lore8 = new ArrayList<>(),
                        lore9 = new ArrayList<>(), lore10 = new ArrayList<>(), lore11 = new ArrayList<>();
                final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("CET"));
                final SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM", Locale.GERMANY);
                sdf.setTimeZone(TimeZone.getTimeZone("CET"));
                for (int n = a.getDayOfYear() - 10; n <= today; n++) {
                    final Analytics an = ma.loadAnalytics(n), ayest = ma.loadAnalytics(n - 1);
                    if (an == null) continue;
                    c.set(Calendar.DAY_OF_YEAR, n);
                    final String time = sdf.format(c.getTime());
                    lore.add("§f" + time + " §7- Neu: §f" + an.getNewPlayers().size() + " §7Beigetreten: §f" + an.getJoinedPlayers().size());
                    lore2.add("§f" + time + " §7- Spielzeit:§f " + ms.timeToString(an.getAveragePlayTime(), false));
                    lore3.add("§f" + time + " §7- Neu: §f" + ms.getDecimalFormat(an.getPercentageNew()) + "% §7Zurück: §f" + ms.getDecimalFormat(ayest.getPercentageReturn(an.getReturnPlayers(ayest.getNewPlayers()))) + "%");
                    lore5.add("§f" + time + " §7- Spieler: §f" + an.getChestPlayers().size() + " §7Geöffnet: §f" + an.getChestOpens());
                    lore4.add("§f" + time + " §7- Spieler: §f" + an.getEventPlayers().size() + " §7Coins: §f" + ms.getNumberFormat(an.getEventcoins()));
                    lore6.add("§f" + time + " §7- Kicks: §f" + (an.getACKicks().isEmpty() ? 0 : an.getACKicks().values().stream().mapToInt(i -> i).sum()) + " §7Sperren: §f" + (an.getACBans().isEmpty() ? 0 : an.getACBans().values().stream().mapToInt(i -> i).sum()));
                    lore7.add("§f" + time + " §7- Spieler: §f" + an.getCasinoPlayers().size() + " §7Spiele: §f" + an.getTotalPlayedCasinoGames() + " §7Chips: §f" + ms.getNumberFormat(an.getGambledChips()) + " §7Meistgespielt: §f"+ an.getMostplayedCasinoGame());
                    lore8.add("§f" + time + " §7- Geworben: §f" + an.getAffiliate() + " §7Verdient: §f" + ms.getNumberFormat(an.getAffiliateEarned()) + " EUR");
                    lore9.add("§f" + time + " §7- Spieler: §f" + an.getOnevsOnePlayers().size() + " §7Kämpfe: §f" + an.getPlayedOnevsOneGames());
                    lore10.add("§f" + time + " §7- Spieler: §f" + an.getGemShop().size() + " §7Ausgegeben: §f" + ms.getNumberFormat(an.getGemsSpent()) + " §7Top Kategorie: §f" + an.getGemShopCategories().entrySet().stream().max(Comparator.comparingLong(Map.Entry::getValue)).map(Map.Entry::getKey).orElse("Keine"));
                    lore11.add("§f" + time + " §7- Spieler: §f" + an.getBasePlayers().size() + " §7Gegrieft: §f" + an.getGriefed());
                }
                lore.add("");
                lore.add("§7Insgesamte Spieler: §c" + ms.getNumberFormat((long) globalstats[0]) + " §7(seit 2013)");
                lore11.add("");
                lore11.add("§7Insgesamte Basen: §c" + ms.getNumberFormat((long) globalstats[3]));
                lore11.add("§7Geladene Basen: §c" + ImmutableMap.copyOf(ms.getBase().getPlayerBases()).values().stream().filter(PlayerBase::isLoadedChunks).count());
                inv.setItem(28, ms.addLore(ms.renameItemStack(new ItemStack(Material.SKULL_ITEM,  1, (byte) 3), "§f§lSpielerzahlen"), lore));
                inv.setItem(29, ms.addLore(ms.renameItemStack(new ItemStack(Material.CAKE), "§f§lNeulinge"), lore3));
                inv.setItem(30, ms.addLore(ms.renameItemStack(new ItemStack(Material.WATCH), "§f§lDurchnittliche Spielzeit"), lore2));
                inv.setItem(32, ms.addLore(ms.renameItemStack(new ItemStack(Material.IRON_HELMET), "§f§lAnticheat"), lore6));
                inv.setItem(33, ms.addLore(ms.renameItemStack(new ItemStack(Material.RED_MUSHROOM), "§c§lWerben"), lore8));
                inv.setItem(34, ms.addLore(ms.renameItemStack(new ItemStack(Material.EMERALD), "§a§lGemShop"), lore10));
                inv.setItem(37, ms.addLore(ms.renameItemStack(new ItemStack(Material.GRASS), "§a§lBasen"), lore11));
                inv.setItem(38, ms.addLore(ms.renameItemStack(new ItemStack(Material.GOLD_SWORD), "§c§l1vs1"), lore9));
                inv.setItem(40, ms.addLore(ms.renameItemStack(new ItemStack(Material.CHEST), "§6§lMuxKisten"), lore5));
                inv.setItem(42, ms.addLore(ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 13), "§5§lCasino"), lore7));
                inv.setItem(43, ms.addLore(ms.renameItemStack(new ItemStack(Material.FIREWORK), "§d§lEvents"), lore4));
            }
        }.runTaskAsynchronously(ms);
        return inv;
    }
    private double getPercentageVoted(final int voted, final int joined) {
        if (joined == 0) return 0D;
        return (double) voted / joined * 100D;
    }

    public String getOPs() {
        final StringBuilder ops = new StringBuilder();
        for (final OfflinePlayer pl : ms.getServer().getOperators()) {
            ops.append(ops.length() == 0 ? " §6§o" + pl.getName() : "§7, §6§o" + pl.getName());
        }
        return ops.toString();
    }

    public String getTrusted() {
        final StringBuilder trusteds = new StringBuilder();
        for (final String name : ms.getTrusted()) {
            trusteds.append(trusteds.length() == 0 ? " §4§o" + name : "§7, §4§o" + name);
        }
        return trusteds.toString();
    }

    public boolean pvpIsActive() {
        return ms.getFarmWorld().getPVP();
    }

    public boolean newsAreActive() {
        return ms.getNewsTitle().isEmpty() == false;
    }

    private void addSlots(final int slots) {
        if (ms.getSlots() + slots < 0) return;
        ms.setSlots((short) (ms.getSlots() + slots));
        ms.getConfig().set("slots", ms.getSlots());
        ms.saveConfig();
    }

    private void setPlayerView(final Player p, final boolean rightclick) {
        int r = ms.getAntiLags().range;
        if (rightclick) r /= 2;
        else r *= 2;
        if (r > 32) r = 32;
        else if (r < 4) r = 4;
        ms.getAntiLags().range = r;
        for (final World w : ms.getServer().getWorlds()) {
            final WorldServer ws = ((CraftWorld) w).getHandle();
            ws.spigotConfig.playerTrackingRange = r;
            for (final Player pl : w.getPlayers()) {
                final EntityPlayer ep = ((CraftPlayer) pl).getHandle();
                ws.getTracker().untrackPlayer(ep);
            }
            for (final Player pl : w.getPlayers()) {
                final EntityPlayer ep = ((CraftPlayer) pl).getHandle();
                ws.getTracker().track(ep);
            }
        }
        ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "ADMIN > Player View", String.valueOf(r));
    }

    private void openNews(final Player p, final boolean sound) {
        if (sound) p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxAdmin§0 | News");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.SIGN), "§f§lNews"));
        final ItemStack title = ms.renameItemStack(new ItemStack(Material.NAME_TAG), "§f§lTitel" + (ms.getNewsTitle().isEmpty() ? "" : "§7 » §r" + ms.getNewsTitle()), "", "§7Klicke, um den Titel zu §f" + (ms.getNewsTitle().isEmpty() ? "setzen" : "ändern") + "§7.");
        inv.setItem(19, ms.getNewsTitle().isEmpty() == false ? ms.addGlow(title) : title);
        ItemStack text = ms.renameItemStack(new ItemStack(Material.BOOK_AND_QUILL), "§f§lText");
        if (ms.getNewsText().isEmpty() == false) {
            text = ms.addGlow(text);
            text = ms.addLore(text,  ms.getLoreFromLongText(ms.getNewsText(), "§f"));
            text = ms.addLore(text, "", "§7Klicke, um den Text zu §fändern§7.");
        } else {
            text = ms.addLore(text, "", "§7Klicke, um den Text zu §fsetzen§8.");
        }
        inv.setItem(20, text);
        ItemStack link = ms.renameItemStack(new ItemStack(Material.TRIPWIRE_HOOK), "§f§lLink" + (ms.getNewsLink().isEmpty() ? "" : " §7» §r" + ms.getNewsLink()), "");

        if (ms.getNewsLinkText().isEmpty() == false || ms.getNewsLink().isEmpty() == false) {
         link = ms.addLore(link, "§7Linktext: §r" + ms.getNewsLinkText(), "", "§7Klicke, um den Link zu §fändern§7.");
        } else {
            link = ms.addLore(link, "§7Klicke, um den Link zu §fsetzen§7.");
        }
        inv.setItem(21, ms.getNewsLinkText().isEmpty() || ms.getNewsLink().isEmpty()? link : ms.addGlow(link));
        inv.setItem(23, ms.renameItemStack(new ItemStack(Material.EYE_OF_ENDER), "§f§lVorschau", "", "§7Klicke, um die News zu §fsehen§7."));
        if (ms.getNewsLink().isEmpty() == false && ms.getNewsTitle().isEmpty() == false && ms.getNewsText().isEmpty() == false && ms.getNewsLinkText().isEmpty() == false) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    inv.setItem(25, ms.addGlow(ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 5), "§a§lNews veröffentlicht", "§7Gesehen von §a" + ms.getNumberFormat(ms.getDB().getNewsCount()) + " Spielern§7.")));
                }
            }.runTaskAsynchronously(ms);
        } else {
            final ItemStack error = ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 14), "§c§lNews veröffentlichen", "");
            if (ms.getNewsTitle().isEmpty()) ms.addLore(error, "§cDer Titel ist nicht festgelegt.");
            if (ms.getNewsText().isEmpty()) ms.addLore(error, "§cDer Text ist nicht festgesetzt.");
            if (ms.getNewsLink().isEmpty()) ms.addLore(error, "§cDer Link ist nicht festgelegt.");
            inv.setItem(25, error);
        }
        inv.setItem(40, ms.renameItemStack(new ItemStack(Material.BARRIER), "§c§lNews zurücksetzen", "", "§7Klicke, um sie §czurückzusetzen§7."));
        if (ms.getActiveInv(p.getName()) != InvType.ADMIN && ms.getActiveInv(p.getName()) != InvType.MUXANVIL) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.ADMIN);
    }

    public void toggleHolograms(final boolean b) {
        if (b == false) {
            ms.getHolograms().removeHolograms(true);
        } else {
            ms.getHolograms().removeHolograms(true);
            ms.getHolograms().spawnHolograms(false, false);
            ms.getNPCS().removeOld();
            ms.getNPCS().respawn();
        }
    }

    private void togglePvP(final Player p) {
        if (ms.getFarmWorld().getPVP()) {
            for (final World w : ms.getServer().getWorlds()) {
                w.setPVP(false);
            }
            ms.broadcastMessage("§6§lMuxCraft>§7 Das PvP wurde §cdeaktiviert§7.", null, Priority.HIGH);
            ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "ADMIN > PVP", "OFF");
        } else {
            for (final World w : ms.getServer().getWorlds()) {
                w.setPVP(true);
            }
            ms.broadcastMessage("§6§lMuxCraft>§7 Das PvP wurde §aaktiviert§7.", null, Priority.HIGH);
            ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "ADMIN > PVP", "ON");
        }
    }

    private void toggleBossBar() {
        final MuxBossBar b = ms.getBossBar();
        for (final Player p : ms.getServer().getOnlinePlayers()) {
            if (BOSSBAR.active == false) b.removePlayer(p);
            else b.addPlayer(p);
        }
    }

    private void toggleBoosters() {
        if (this.BOOSTERS.active == false) {
            for (final Player pl : ms.getServer().getOnlinePlayers()) {
                if (ms.getBooster().hasBooster(pl, BoosterType.FLY)) {
                    pl.setAllowFlight(false);
                    pl.setFlying(false);
                    pl.setFlySpeed(0.1F);
                    ms.sendNormalTitle(pl, "§fDein " + BoosterType.FLY.getName() + " wurde §epausiert§f.", 0, 60, 10);
                }
            }
        }
    }

    private void toggleAntiBot() {
        if (ANTIBOT.active) {
            final MuxAntiBot antibot = ms.getAntiBot();
            for (final Player pl : ms.getServer().getOnlinePlayers()) {
                final int accs = ms.howManyAccounts(pl, pl.getAddress().getAddress().getHostAddress());
                if (antibot.isBot(pl, accs)) ms.logoutPlayer(pl, "§cZweitaccounts wurden temporär deaktiviert.");
            }
        }
    }

    private void toggleAntiVPN() {
        if (ANTIVPN.active) {
            final Set<Player> onlinePlayers = new HashSet<>(ms.getServer().getOnlinePlayers());
            ms.getForkJoinPool().execute(() -> {
                final Set<Player> toKick = new HashSet<>();
                for (final Player pl : onlinePlayers) {
                    final int vpn = ms.isVPN(pl.getAddress().getAddress().getHostAddress());
                    if (vpn > 0 && pl.hasPermission("muxsystem.vip") == false) {
                        toKick.add(pl);
                    }
                }
                if (!toKick.isEmpty()) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            toKick.forEach(pl -> ms.logoutPlayer(pl, "§cBitte schalte dein VPN aus."));
                        }
                    }.runTask(ms);
                }
            });

        }
    }
}