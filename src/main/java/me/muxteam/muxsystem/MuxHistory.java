package me.muxteam.muxsystem;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import me.muxteam.basic.ConfirmInventory;
import me.muxteam.basic.MuxActions;
import me.muxteam.basic.MuxAnvil;
import me.muxteam.basic.Pageifier;
import me.muxteam.muxteam.BanEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class MuxHistory {
    private final MuxSystem ms;
    private boolean shutdown = false;
    private final List<CachedHistory> cachedHistory = new CopyOnWriteArrayList<>();
    private final Map<Player, CachedHistory> teleportCache = new HashMap<>();
    private final Map<UUID, Set<CoinHistory>> coinHistoryCache = new ConcurrentHashMap<>();

    public MuxHistory(final MuxSystem ms) {
        this.ms = ms;
        new BukkitRunnable() {
            @Override
            public void run() {
                saveCoinHistory();
                ms.getDB().saveHistory(cachedHistory);
            }
        }.runTaskTimerAsynchronously(ms, 20L * 20L, 20L * 10L);
    }

    public void close() {
        this.shutdown = true;
        saveCoinHistory();
        ms.getDB().saveHistory(cachedHistory);
    }

    private void saveCoinHistory() {
        final Set<CachedHistory> temp = new HashSet<>();
        for (final Map.Entry<UUID, Set<MuxHistory.CoinHistory>> entry : coinHistoryCache.entrySet()) {
            final Set<CoinHistory> toRemove = new HashSet<>();
            for (final CoinHistory coinHistory : entry.getValue()) {
                if (coinHistory.getLastChange() > System.currentTimeMillis() - 180000 && shutdown == false) continue;
                toRemove.add(coinHistory);
                temp.add(new CachedHistory(entry.getKey(), null, HistoryType.MUXCOINS.keyWords[0], String.valueOf(coinHistory.getCoins()), coinHistory.str));
            }
            coinHistoryCache.get(entry.getKey()).removeAll(toRemove);
        }
        coinHistoryCache.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        if (Bukkit.isPrimaryThread()) {
            cachedHistory.addAll(temp);
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                cachedHistory.addAll(temp);
            }
        }.runTask(ms);
    }

    public boolean handleCommand(final Player p, final String[] args) {
        if (p.hasPermission("muxsystem.team") == false) {
            ms.sendNoCMDMessage(p);
            return true;
        } else if (p.hasPermission("muxsystem.history") == false && args.length < 1) {
            ms.showItemBar(p, ms.usage("/history [spieler]"));
            return true;
        } else if (args.length > 0) {
            final OfflinePlayer ply = ms.getPlayer(args[0], true);
            if (ply == null) {
                ms.showItemBar(p, ms.hnotfound);
                return true;
            }
            final MuxUser u = (ply.getName() != null ? null : ms.getDB().loadPlayer(ply.getUniqueId()));
            String name = ply.getName();
            if (name == null && u == null) {
                ms.showItemBar(p, ms.hnotfound);
                return true;
            }
            if (name == null) name = u.getName();
            openHistory(p, MuxHistory.HistoryType.GENERAL, ply.getUniqueId(), name, 0);
            return true;
        }
        openGeneralHistory(p, 0);
        return true;
    }

    private void addPunishHistory(final Map<Long, ItemStack> times, UUID uuid, final Player staff) {
        this.addPunishHistory(times, uuid, staff, null);
    }
    private void addPunishHistory(final Map<Long, ItemStack> times, UUID uuid, final Player staff, final String searchTerm) {
        final Server sr = ms.getServer();
        final Set<UUID> permbans = new HashSet<>(), permmutes = new HashSet<>(), pvpbans = new HashSet<>(), tempmutes = new HashSet<>(), tempbans = new HashSet<>(), minebans = new HashSet<>();
        final boolean who = staff.hasPermission("muxsystem.history"), allplayers = uuid == null;
        OfflinePlayer op = (allplayers ? null : sr.getOfflinePlayer(uuid));
        MuxUser u = (allplayers ? null : op.isOnline() ? ms.getMuxUser(op.getName()) : ms.getDB().loadPlayer(uuid));
        final List<BanEntry> entries = allplayers ? searchTerm == null ? ms.getDB().getBanEntries(100) : ms.getDB().searchBanEntries(searchTerm, 100) : ms.getDB().getBanEntries(uuid, 100);
        final ZoneId zoneId = ZoneId.of("CET");
        for (final BanEntry be : entries) {
            if (allplayers) {
                uuid = be.getUUID();
                op = sr.getOfflinePlayer(uuid);
                u = op.isOnline() ? ms.getMuxUser(op.getName()) : ms.getDB().loadPlayer(uuid);
            }
            ItemStack item;
            final String[] date = be.getTime().split(" ");
            final String action = be.getAction();
            String color = "§4";
            String duration = null;
            final List<String> extras = new ArrayList<>();
            if (action.equalsIgnoreCase("permban")) {
                item = ms.renameItemStack(new ItemStack(Material.IRON_FENCE), "§4§lPermanente Sperre");
                if (be.getReason().startsWith("Usertransfer")) {
                    extras.add("§7Grund: §4" + be.getReason());
                    extras.add("");
                }
                if (permbans.contains(uuid) == false && u != null && u.isPermban()) {
                    item = ms.addGlow(item);
                    permbans.add(uuid);
                }
            } else if (action.equalsIgnoreCase("permmute")) {
                color = "§9";
                item = ms.renameItemStack(new ItemStack(Material.LAPIS_BLOCK), "§9§lPermanenter Mute");
                if (be.getExtraInfo() != null) {
                    final String[] extrainfo = be.getExtraInfo().split(" \\| ");
                    if (extrainfo.length == 2) {
                        extras.add("§7Letzte Nachrichten: §e");
                        extras.addAll(ms.getLoreFromLongText(extrainfo[1].substring(1, extrainfo[1].length() - 1), "§f"));
                        extras.add("");
                    }
                }
                if (permmutes.contains(uuid) == false && u != null && u.isPermMute()) {
                    item = ms.addGlow(item);
                    permmutes.add(uuid);
                }
            } else if (action.equalsIgnoreCase("kick")) {
                color = "§9";
                item = ms.renameItemStack(new ItemStack(Material.IRON_BOOTS), "§9§lKick vom Server");
            } else if (action.equalsIgnoreCase("mineban")) {
                color = "§f";
                item = ms.renameItemStack(new ItemStack(Material.IRON_PICKAXE), "§f§lMining Verbot");
                if (minebans.contains(uuid) == false && ms.getAntiBot().isMineBanned(uuid)) {
                    item = ms.addGlow(item);
                    minebans.add(uuid);
                }
            } else if (action.equalsIgnoreCase("clientmod")) {
                color = "§6";
                item = ms.renameItemStack(new ItemStack(Material.IRON_SWORD), "§6§lPvP Ausschluss");
                if (pvpbans.contains(uuid) == false && u != null && u.isPvPbanned()) {
                    item = ms.addGlow(item);
                    pvpbans.add(uuid);
                    if (be.getRemoveCauser() == null)
                        duration = "§cnoch " + ms.getTime((int) ((u.getPvPUnbanTime() - System.currentTimeMillis()) / 1000));
                }
                final String[] extrainfo = be.getExtraInfo().split(" \\| ");
                if (extrainfo.length == 2) {
                    extras.add("§7Werte: §6");
                    extras.addAll(Arrays.stream(extrainfo[1].substring(1, extrainfo[1].length() - 1).replace(be.getReason(), "§c" + be.getReason() + "§f").split(", ")).collect(Collectors.toList()));
                    extras.add("");
                }
            } else if (action.equalsIgnoreCase("general")) {
                color = "§c";
                item = ms.renameItemStack(new ItemStack(Material.LAVA_BUCKET), "§c§lSuspendierung");
                if (tempbans.contains(uuid) == false && permbans.contains(uuid) == false && u != null && u.isTimebanned()) {
                    item = ms.addGlow(item);
                    tempbans.add(uuid);
                    if (be.getRemoveCauser() == null)
                        duration = "§cnoch " + ms.getTime((int) ((u.getUnbanTime() - System.currentTimeMillis()) / 1000));
                }
            } else if (action.equalsIgnoreCase("chat")) {
                color = "§e";
                item = ms.renameItemStack(new ItemStack(Material.SIGN), "§e§lChat Verlangsamung");
                if (tempmutes.contains(uuid) == false && permmutes.contains(uuid) == false && u != null && u.isTimeMuted()) {
                    item = ms.addGlow(item);
                    tempmutes.add(uuid);
                    if (be.getRemoveCauser() == null)
                        duration = "§cnoch " + ms.getTime((int) ((u.getUnmuteTime() - System.currentTimeMillis()) / 1000L));
                }
                final String[] extrainfo = be.getExtraInfo().split(" \\| ");
                if (extrainfo.length == 2) {
                    extras.add("§7Letzte Nachrichten: §e");
                    extras.addAll(ms.getLoreFromLongText(extrainfo[1].substring(1, extrainfo[1].length() - 1), "§f"));
                    extras.add("");
                }
            } else if (action.equalsIgnoreCase("warnung")) {
                color = "§f";
                item = ms.renameItemStack(new ItemStack(Material.PAPER), "§f§lWarnung");
                if (be.getExtraInfo() != null) {
                    final String[] extrainfo = be.getExtraInfo().split(" \\| ");
                    if (extrainfo.length == 2) {
                        extras.add("§7Letzte Nachrichten: §e");
                        extras.addAll(ms.getLoreFromLongText(extrainfo[1].substring(1, extrainfo[1].length() - 1), "§f"));
                        extras.add("");
                    }
                }
            } else {
                color = "§a";
                item = ms.renameItemStack(new ItemStack(Material.GLASS_BOTTLE), "§a§lUnbekannt");
            }
            item = ms.addLore(item, "§7Betroffener: " + color + be.getName());
            item = ms.addLore(item, "§0" + be.getID());
            if (extras.isEmpty() == false)
                item = ms.addLore(item, extras);
            if (duration != null) {
                item = ms.addLore(item, "§7Dauer: §c" + duration, "");
            }
            if (who) ms.addLore(item, "§7Verursacht von: §f" + be.getCauser());
            item = ms.addLore(item, "§7am " + date[0].replace("-", ".") + " um " + date[1].substring(0, 5));
            if (be.getRemoveCauser() != null) {
                final String[] removeDate = be.getRemoveTime() == null ? new String[]{"", ""} : be.getRemoveTime().split(" ");
                item = ms.addLore(item, "");
                item = ms.addLore(item, "§7Aufgehoben von: §a" + (who ? be.getRemoveCauser() : "Teammitglied"));
                if (be.getRemoveTime() != null)
                    item = ms.addLore(item, "§7am " + removeDate[0].replace("-", ".") + " um " + removeDate[1].substring(0, 5));
                item.setAmount(0);
            }
            if (ms.hasGlow(item)) item = ms.addLore(item, "", "§a§oKlicke zum Aufheben.");
            final Timestamp timestamp = Timestamp.valueOf(be.getUnformattedTime());
            times.put(timestamp.toLocalDateTime().atZone(zoneId).toInstant().toEpochMilli(), item);
        }
    }

    public void openGeneralHistory(final Player p, final int page) {
        this.openGeneralHistory(p, page, null);
    }

    public void openGeneralHistory(final Player p, final int page, final String searchTerm) {
        final Inventory inv = ms.getServer().createInventory(null, 54, "§0§lMuxHistory" +
                (searchTerm == null ? "" : "§0 - §o\"" + (searchTerm.length() < 11 ? searchTerm + "\"" : (searchTerm.substring(0, 11) + "\"..."))));
        final Pageifier<ItemStack> pages = new Pageifier<>(36);
        new BukkitRunnable() {
            final Server sr = ms.getServer();

            @Override
            public void run() {
                final Map<Long, ItemStack> times = new HashMap<>();
                addPunishHistory(times, null, p, searchTerm);

                final LinkedList<Object[]> history = new LinkedList<>();
                for (final String keyWord : HistoryType.GENERAL.getKeyWords()) {
                    history.addAll(searchTerm == null ? ms.getDB().getGeneralHistory(keyWord, 200) : ms.getDB().getGeneralHistory(keyWord, 200, searchTerm));
                }
                final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                sdf.setTimeZone(TimeZone.getTimeZone("CET"));
                for (final Object[] o : history) {
                    final UUID victim = (UUID) o[0];
                    final String type = (String) o[1], param = (String) o[2];
                    final long timestamp = (long) o[3];
                    final int id = (int) o[5];
                    ItemStack i = getItemFromType(type);
                    final List<String> lore = new ArrayList<>();
                    if (victim != null) {
                        lore.add("§7Betroffener: §f" + sr.getOfflinePlayer((UUID) o[4]).getName());
                    }
                    if (param != null) {
                        if (victim != null) lore.add("§0" + id);
                        lore.add("§7Parameter: §f" + param);
                    }
                    lore.add("§0" + id);
                    if (victim != null) {
                        lore.add("§7Verursacht von: §f" + sr.getOfflinePlayer(victim).getName());
                    } else {
                        lore.add("§7Verursacht von: §f" + sr.getOfflinePlayer((UUID) o[4]).getName());
                    }
                    final String[] date = sdf.format(new Date(timestamp)).split(" ");
                    lore.add("§7am " + date[0].replace("-", ".") + " um " + date[1].substring(0, 5));
                    i = ms.renameItemStack(i, "§f§l" + type);
                    times.put(timestamp, ms.addLore(i, lore));
                }
                Lists.reverse(times.entrySet().stream().sorted(Comparator.comparingLong(Map.Entry::getKey))
                        .collect(Collectors.toList())).forEach(entry -> pages.addItem(entry.getValue()));
                ms.addPageButtons(inv, page, pages);
            }
        }.runTaskAsynchronously(ms);
        if (p.isOp()) inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        ItemStack desc;
        if (searchTerm != null) {
            desc = ms.renameItemStack(new ItemStack(Material.BOOK), "§f§lVerlauf §0" + page, "§7Hiermit werden alle Aktionen gelistet,", "§7die von Teamler ausgeführt wurden.",
                    "", "§7Gesucht: §f" + searchTerm, "§0" + searchTerm,  "§7Klicke, um im Verlauf zu §fsuchen§7.");
            desc = ms.addGlow(desc);
        } else {
            desc = ms.renameItemStack(new ItemStack(Material.BOOK), "§f§lVerlauf §0" + page, "§7Hiermit werden alle Aktionen gelistet,", "§7die von Teamler ausgeführt wurden.",
                    "",  "§7Klicke, um im Verlauf zu §fsuchen§7.");
        }
        inv.setItem(4, desc);
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.HISTORY && it != MuxInventory.InvType.ADMIN && it != MuxInventory.InvType.CONFIRM) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.HISTORY);
    }

    private ItemStack getItemFromType(final String type) {
        if (type.equals("EVENT")) {
            return new ItemStack(Material.STAINED_CLAY, 1, (byte) 2);
        } else if (type.startsWith("CRATE")) {
            return new ItemStack(Material.CHEST);
        } else if (type.startsWith("EXTRAENDER") || type.startsWith("PETS") || type.startsWith("MOUNTS") || type.startsWith("EXTRACMD") || type.startsWith("EMOJIS") || type.startsWith("PERKS")) {
            return new ItemStack(Material.GOLD_BARDING);
        } else if (type.equals("SETHOME") || type.equals("DELHOME") || type.equals("CLEARHOMES")) {
            return new ItemStack(Material.COMPASS);
        } else if (type.equals("RANK") || type.equals("OPERATOR")) {
            return new ItemStack(Material.NAME_TAG);
        } else if (type.equals("TIME")) {
            return new ItemStack(Material.WATCH);
        } else if (type.startsWith("WHITELIST")) {
            return new ItemStack(Material.BEDROCK);
        } else if (type.startsWith("/TP")) {
            return new ItemStack(Material.ENDER_PEARL);
        } else if (type.equals("RESTART")) {
            return new ItemStack(Material.BOWL);
        } else if (type.startsWith("DUEL") || type.startsWith("CLANWAR") || type.startsWith("1VS1")) {
            return new ItemStack(Material.IRON_SWORD);
        } else if (type.startsWith("ADMIN")) {
            return new ItemStack(Material.DIODE);
        } else if (type.equals("RESET") || type.equals("BASE RESET") || type.equals("STATS RESET")) {
            return new ItemStack(Material.TNT);
        } else if (type.startsWith("CHATFILTER")) {
            return new ItemStack(Material.HOPPER);
        } else if (type.equals("WORLDEDIT")) {
            return new ItemStack(Material.WOOD_AXE);
        } else if (type.equals("BUILD")) {
            return new ItemStack(Material.GRASS);
        } else if (type.equals("VOTEGIVE")) {
            return new ItemStack(Material.IRON_DOOR);
        } else if (type.equals("CRASH")) {
            return new ItemStack(Material.ANVIL, 1, (byte) 2);
        } else if (type.equals("EVENT KICK")) {
            return new ItemStack(Material.LEATHER_BOOTS);
        } else if (type.equals("SPAWNTP")) {
            return new ItemStack(Material.BEACON);
        } else if (type.equals("FEED")) {
            return new ItemStack(Material.COOKED_CHICKEN);
        } else if (type.equals("HEAL")) {
            return new ItemStack(Material.GOLDEN_APPLE);
        } else if (type.equals("FLY")) {
            return new ItemStack(Material.FEATHER);
        } else if (type.equals("POLL")) {
            return new ItemStack(Material.PUMPKIN_PIE);
        } else if (type.equals("SHOP") || type.equals("PREMIUMSHOP")) {
            return new ItemStack(Material.STORAGE_MINECART);
        } else if (type.equals("CLAN")) {
            return new ItemStack(Material.CHAINMAIL_HELMET);
        } else if (type.equals("BROADCAST")) {
            return new ItemStack(Material.SIGN);
        }
        return new ItemStack(Material.PAPER, 1, (byte) 0);
    }

    public void openCategoryInventory(final Player p, final String name, final HistoryType current) {
        final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lMuxHistory§0 | " + name);
        int slot = 18;
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.HOPPER), "§f§lKategorie"));
        for (final HistoryType type : HistoryType.values()) {
            final String perm = type.getPermission();
            if (p.getName().equals(name) == false && (perm != null && (p.hasPermission(perm) == false || (perm.equals("TRUSTED") && ms.isTrusted(p.getName()) == false))))
                continue;
            final ItemStack item = ms.renameItemStack(new ItemStack(type == HistoryType.GENERAL ? Material.BOOK : type.getIcon(), 1, type.getData()), "§f§l" + type.getDisplayName());
            inv.setItem(slot, current == type ? ms.addGlow(item) : item);
            slot++;
        }
        if (ms.getActiveInv(p.getName()) != MuxInventory.InvType.HISTORY) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.HISTORY);
    }

    public void openHistory(final Player p, final HistoryType type, final UUID uuid, final String name, final int page) {
        final Inventory inv = ms.getServer().createInventory(null, 54, "§0§lMuxHistory§0 | " + name);
        final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        final SimpleDateFormat timestampSdf = new SimpleDateFormat("HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("CET"));
        timestampSdf.setTimeZone(TimeZone.getTimeZone("CET"));
        final Pageifier<ItemStack> pages = new Pageifier<>(36);
        final Map<Long, ItemStack> times = new HashMap<>();
        new BukkitRunnable() {
            final Server sr = ms.getServer();
            @Override
            public void run() {
                if (type == HistoryType.GENERAL) {
                    addPunishHistory(times, uuid, p);
                }
                final LinkedList<Object[]> history = new LinkedList<>();
                for (final String keyWord : type.getKeyWords()) {
                    history.addAll(ms.getDB().getHistory(uuid, keyWord, 200, type.isServerBound()));
                }
                if (type.getKeyWords().length > 1) {
                    history.sort(Comparator.comparingLong(value -> (long) value[3]));
                    Collections.reverse(history);
                }
                final String color = type.getColor();
                String currentDateStr = null;
                final List<String> finalLore = new ArrayList<>();

                for (final Object[] o : history) {
                    if (type == HistoryType.SUPPORTS) {
                        final long timestamp = (long) o[3];
                        final Date date = new Date(timestamp);
                        final String dateStr = sdf.format(date) + " " + timestampSdf.format(date);
                        final String entry = (String) o[1], param = (String) o[2];
                        pages.addItem(ms.renameItemStack(new ItemStack(type.getIcon(), 1, type.getData()), "§7§l" + dateStr + (p.isOp() && param != null ? "§e ❤" : ""), Arrays.asList(entry.split("\n"))));
                    } else if (type != HistoryType.PAYMENTS && type != HistoryType.GENERAL) {
                        final long timestamp = (long) o[3];
                        final Date date = new Date(timestamp);
                        final String dateStr = sdf.format(date);
                        if (currentDateStr == null) currentDateStr = dateStr;
                        if (currentDateStr.equals(dateStr) == false) {
                            if (finalLore.isEmpty() == false)
                                pages.addItem(ms.renameItemStack(new ItemStack(type.getIcon(), 1, type.getData()), "§7§l" + currentDateStr, finalLore));
                            finalLore.clear();
                        }
                        currentDateStr = dateStr;
                        final StringBuilder sb = new StringBuilder("§7" + timestampSdf.format(date)).append(" §r");
                        String entry = (String) o[1];
                        String victimadj = "mit";

                        if (type == HistoryType.DEATHS) {
                            final String type = (String) o[4];
                            if (type.equals("PVP")) {
                                final String param = (String) o[2];
                                final UUID victim = (UUID) o[0];
                                if (entry.startsWith("-") == false) continue;
                                sb.append("§cim ").append(param).append(" gegen ").append(sr.getOfflinePlayer(victim).getName()).append(" ");
                            }
                        }
                        if (type == HistoryType.TELEPORTS) {
                            final String type = (String) o[4];
                            victimadj = "zu";
                            if (type.equals("HOME")) {
                                sb.append("Home").append(" ");
                                victimadj = "von";
                            } else if (entry.equals("Base")) {
                                victimadj = "von";
                            }
                            sb.append(entry);
                        } else {
                            final String possibleNumberEntry = entry == null ? (String.valueOf(o[2])) : entry.contains(" ") && entry.split(" ").length >= 1 ? entry.split(" ")[0] : entry;
                            try {
                                final long l = Long.parseLong(possibleNumberEntry);
                                String c;
                                if (l < 0)
                                    c = ChatColor.RED.toString();
                                else if (l > 0)
                                    c = ChatColor.GREEN + "+";
                                else continue;
                                sb.append(c).append(ms.getSFormat(l));
                            } catch (final Exception ignored) {
                                try {
                                    final double d = Double.parseDouble(possibleNumberEntry);
                                    String c;
                                    if (d < 0)
                                        c = ChatColor.RED.toString();
                                    else if (d > 0)
                                        c = ChatColor.GREEN + "+";
                                    else c = ChatColor.WHITE + "+";
                                    sb.append(c).append(ms.getSFormat(d));
                                } catch (final Exception ignored2) {
                                    if (type == HistoryType.DEATHS) {
                                        UUID victim = (UUID) o[0];
                                        if (victim != null) {
                                            sb.append("§cgetötet von ").append(sr.getOfflinePlayer(victim).getName());
                                        } else sb.append("§c").append(entry);
                                    } else sb.append(entry);
                                }
                            }
                        }
                        if (type == HistoryType.MUXCOINS)
                            sb.append(" MuxCoins");
                        else if (type == HistoryType.GEMS)
                            sb.append(" Gems");
                        else if (type == HistoryType.CASINO)
                            sb.append(" Chips");
                        if (type != HistoryType.DEATHS) {
                            String param = (String) o[2];
                            if (type == HistoryType.TELEPORTS) {
                                try {
                                    final String[] split = param.split(":");
                                    param = split[0] + " " + split[1] + " " + split[2] + " " + split[3];
                                } catch (final Exception ignored) {
                                }
                            }
                            final UUID victim = (UUID) o[0];
                            sb.append("§r");
                            if (param != null)
                                sb.append(" §8| §7").append(param);
                            if (victim != null) {
                                sb.append(" ").append(victimadj).append(" ").append(sr.getOfflinePlayer(victim).getName());
                            }
                        }
                        finalLore.add(sb.toString());
                        if (finalLore.size() == 10) {
                            pages.addItem(ms.renameItemStack(new ItemStack(type.getIcon(), 1, type.getData()), "§7§l" + currentDateStr, finalLore));
                            finalLore.clear();
                        }
                    } else {
                        final UUID victim = (UUID) o[0];
                        if (type == HistoryType.GENERAL && victim == null) continue;
                        final String entry = (String) o[1];
                        String param = (String) o[2];
                        if (type == HistoryType.PAYMENTS) {
                            int i = 0;
                            String status;
                            try {
                                i = Integer.parseInt(param.split(" ")[1]);
                            } catch (final Exception ignored) {
                            }
                            switch (i) {
                                case 1:
                                    status = " (§cChargeback§f)";
                                    break;
                                case 2:
                                    status = " (§eRefund§f)";
                                    break;
                                default:
                                    status = " (§aComplete§f)";
                            }
                            param = "§7Preis: §f" + param.split(" ")[0] + " EUR" + status;
                        }
                        final long timestamp = (long) o[3];
                        ItemStack i = new ItemStack(type.getIcon(), 1, type.getData());
                        if (type == HistoryType.GENERAL) {
                            i = getItemFromType(entry);
                        }
                        final List<String> lore = new ArrayList<>();
                        if (param != null) {
                            lore.add(type == HistoryType.PAYMENTS ? param : ("§7Parameter: §f" + param));
                        }
                        final int id = (int) o[5];
                        lore.add("§0" + id);
                        if (type == HistoryType.GENERAL) {
                            lore.add("§7Verursacht von: §f" + sr.getOfflinePlayer(victim).getName());
                        }
                        final String date = sdf.format(new Date(timestamp)), time = timestampSdf.format(new Date(timestamp));
                        lore.add("§7am " + date + " um " + time);
                        i = ms.renameItemStack(i, color + "§l" + entry);
                        if (type == HistoryType.GENERAL) {
                            times.put(timestamp, ms.addLore(i, lore));
                        } else
                            pages.addItem(ms.addLore(i, lore));
                    }
                }
                if (finalLore.isEmpty() == false) {
                    pages.addItem(ms.renameItemStack(new ItemStack(type.getIcon(), 1, type.getData()), "§7§l" + currentDateStr, finalLore));
                }
                if (times.isEmpty() == false) {
                    Lists.reverse(times.entrySet().stream().sorted(Comparator.comparingLong(Map.Entry::getKey)).collect(Collectors.toList())).forEach(entry -> pages.addItem(entry.getValue()));
                }
                ms.addPageButtons(inv, page, pages);
                inv.setItem(4, ms.renameItemStack(ms.getHead(name), "§f§l" + name));
            }
        }.runTaskAsynchronously(ms);
        inv.setItem(5, ms.renameItemStack(new ItemStack(Material.HOPPER), "§f§lKategorie", "§0" + type.name().toLowerCase() + " " + page, "§7Klicke§7, um die §fKategorie zu ändern§7."));
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.HISTORY && it != MuxInventory.InvType.USERCONTROL && it != MuxInventory.InvType.CONFIRM && it != MuxInventory.InvType.X) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.HISTORY);
    }

    private void handleCategoryInventory(final Player p, final int slot, final ItemStack i, final String pname) {
        if (slot == 0) {
            openHistory(p, (HistoryType) p.getMetadata("chistoryt").get(0).value(), UUID.fromString(p.getMetadata("chistoryid").get(0).asString()), pname, 0);
        } else {
            final HistoryType type = getTypeByIcon(i.getType());
            if (type == null) return;
            openHistory(p, type, UUID.fromString(p.getMetadata("chistoryid").get(0).asString()), pname, 0);
        }
        clearMetadata(p);
    }

    public void handleInventory(final Player p, final ItemStack i, final int slot, final Inventory inv, final boolean rightclick) {
        final String title = inv.getTitle();
        final Material m = i.getType();
        if (slot == 4) {
            if (m == Material.BOOK) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                if (ms.hasGlow(i)) {
                    clearMetadata(p);
                    openGeneralHistory(p, 0);
                } else {
                    new MuxAnvil(ms, (input, p1) -> {
                        clearMetadata(p);
                        openGeneralHistory(p, 0, ms.retardCleaner(input, "Suchen:"));
                    }).show(p, "Suchen: ");
                }
                return;
            }
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        } else if (title.contains("|") == false) {
            final int page = Integer.parseInt(ChatColor.stripColor(inv.getItem(4).getItemMeta().getDisplayName()).split(" ")[1]);
            if (m == Material.ITEM_FRAME && slot < 9) {
                p.performCommand("a");
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            } else if (m == Material.SKULL_ITEM && slot < 9) {
                p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                final ItemStack desc = inv.getItem(4);
                openGeneralHistory(p, slot == 7 ? page - 1 : page + 1, desc.hasItemMeta() && desc.getItemMeta().hasLore() && desc.getItemMeta().getLore().size() == 6 ? ChatColor.stripColor(desc.getItemMeta().getLore().get(4)) : null);
            } else {
                final ItemMeta im = i.getItemMeta();
                if (ms.hasGlow(i) && i.hasItemMeta() && im.hasLore() && Iterables.get(im.getLore(), im.getLore().size() - 1).startsWith("§a")) {
                    final String pname = im.getLore().stream().filter(s -> s.startsWith("§7Betroffener:")).map(s -> ChatColor.stripColor(s.split(" ")[1])).findFirst().orElse(null);
                    if (pname == null) return;
                    if (ms.getBans().undoActivePunishFromHistory(p, pname,  i)) {
                        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
                        openGeneralHistory(p, page);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                openGeneralHistory(p, page);
                            }
                        }.runTaskLater(ms, 1L);
                    }
                    return;
                } else if (rightclick && ms.isFullTrusted(p.getName())) {
                    tryToDeleteEntry(p, i, page, null, null, null);
                    return;
                }
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            }
            return;
        }
        final String name = ChatColor.stripColor(title).split("\\| ")[1];
        if (m == Material.ITEM_FRAME && slot == 0 && inv.getSize() > 36) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            if (p.hasPermission("muxsystem.team") == false) {
                p.performCommand("x");
                return;
            }
            p.performCommand("o " + name);
            return;
        } else if (inv.getSize() < 40) {
            handleCategoryInventory(p, slot, i, name);
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            return;
        }
        final String[] split = inv.getItem(5).getItemMeta().getLore().get(0).split(" ");
        final int page = Integer.parseInt(split[1]);
        final UUID uuid = ms.getPlayer(name).getUniqueId();
        final HistoryType type = HistoryType.valueOf(ChatColor.stripColor(split[0].toUpperCase()));
        if (slot == 5) {
            p.setMetadata("chistoryt", new FixedMetadataValue(ms, type));
            p.setMetadata("chistoryid", new FixedMetadataValue(ms, uuid));
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            openCategoryInventory(p, name, type);
        } else if (m == Material.SKULL_ITEM && (slot == 7 || slot == 8)) {
            p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
            openHistory(p, type, uuid, name, slot == 7 ? page - 1 : page + 1);
        } else if (type == HistoryType.GENERAL && ms.getBans().undoActivePunishFromHistory(p, name, i)) {
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
            new BukkitRunnable() {
                @Override
                public void run() {
                    openHistory(p, type, uuid, name, page);
                }
            }.runTaskLater(ms, 1L);
        } else if (type == HistoryType.GENERAL && rightclick && ms.isFullTrusted(p.getName())) {
            tryToDeleteEntry(p, i, page, type, uuid, name);
        } else {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
        }
    }

    private void tryToDeleteEntry(final Player p, final ItemStack i, final int page, final HistoryType type, final UUID uuid, final String name) {
        final ItemMeta im = i.getItemMeta();
        final String one = Iterables.get(im.getLore(), 1), zero = Iterables.get(im.getLore(), 0);
        final int id = one.startsWith("§0") ? Integer.parseInt(ChatColor.stripColor(one)) : zero.startsWith("§0") ? Integer.parseInt(ChatColor.stripColor(zero)) : -1;
        if (id == -1) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        }
        p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        final MuxActions.PlayerAction confirm = player -> {
            final boolean del = ms.getDB().deleteHistoryEntry(id);
            if (del == false) {
                if (type != null) openHistory(p, type, uuid, name, page);
                else openGeneralHistory(player, page);
                player.playSound(player.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                return;
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (type != null) openHistory(p, type, uuid, name, page);
                    else openGeneralHistory(player, page);
                }
            }.runTaskLater(ms, 1L);
        }, cancel = player -> {
            if (type != null) openHistory(p, type, uuid, name, page);
            else openGeneralHistory(player, page);
            player.playSound(player.getLocation(), Sound.NOTE_BASS, 1F, 1F);
        };
        new ConfirmInventory(ms, confirm, cancel).show(p, "§0§lEintrag löschen", "§aBestätigen", "§cAbbrechen", new ItemStack(i.getType(), 1, i.getDurability()), im.getDisplayName());
    }

    private void clearMetadata(final Player p) {
        p.removeMetadata("chistoryt", ms);
        p.removeMetadata("chistoryid", ms);
    }

    public HistoryType getTypeByIcon(final Material icon) {
        return Arrays.stream(HistoryType.values()).filter(type -> type.getIcon() == icon).findFirst().orElse(HistoryType.GENERAL);
    }

    public List<CachedHistory> getCachedHistory() {
        return cachedHistory;
    }

    public Map<Player, CachedHistory> getTeleportCache() {
        return teleportCache;
    }

    public void addCoinHistory(final UUID uuid, final String str, final double coins) {
        final Set<MuxHistory.CoinHistory> set = coinHistoryCache.computeIfAbsent(uuid, k -> new HashSet<>());
        final CoinHistory history = set.stream().filter(coinHistory -> coinHistory.str.equals(str)).findFirst().orElse(null);
        if (history != null) {
            history.addCoins(coins);
            history.setLastChange(System.currentTimeMillis());
        } else
            set.add(new CoinHistory(str, coins));
    }

    public void addTeleportCache(final Player p, final UUID uuid, final UUID uuid2, final String type, final String entry, final String param) {
        teleportCache.put(p, new CachedHistory(uuid, uuid2, type, entry, param));
    }

    public void addPaymentHistory(final UUID uuid, final UUID uuid2, final String type, final String entry, final String param, final long timestamp) {
        cachedHistory.add(new CachedHistory(uuid, uuid2, type, entry, param, timestamp));
    }

    public void addHistory(final UUID uuid, final UUID uuid2, final String type, final String entry, final String param) {
        cachedHistory.add(new CachedHistory(uuid, uuid2, type, entry, param));
    }

    public enum HistoryType {
        GENERAL("Allgemein", "§c", null, Material.PAPER, true, "TEAMACTION"),
        MUXCOINS("MuxCoins", "§e", null, Material.DOUBLE_PLANT, true, "COINS"),
        DEATHS("Tode", "§c", null, Material.BONE, true, "DEATH", "PVP"),
        COMMANDS("Befehle", "§f", "muxhistory.commands", Material.COMMAND, true, "CMD"),
        TELEPORTS("Teleports", "§f", "muxhistory.teleports", Material.ENDER_PEARL, true, "HOME", "TPA", "TELEPORT"),
        CASINO("Casino", "§d", "muxhistory.casino", Material.INK_SACK, (byte) 13, true, "CASINO"),
        GEMS("Gems", "§a", "muxhistory.gems", Material.EMERALD, true, "GEMS"),
        PAYMENTS("Payments", "§a", "TRUSTED", Material.CHEST, false, "PAYMENT"),
        SUPPORTS("Supports", "§2", null, Material.SLIME_BALL, false, "SUPPORT");

        private final String displayName, color, permission;
        private final String[] keyWords;
        private final byte data;
        private final Material icon;
        private final boolean serverBound;

        HistoryType(final String displayName, final String color, final String permission, final Material icon, final boolean serverBound, final String... keyWords) {
            this.displayName = displayName;
            this.keyWords = keyWords;
            this.permission = permission;
            this.icon = icon;
            this.color = color;
            this.data = 0;
            this.serverBound = serverBound;
        }

        HistoryType(final String displayName, final String color, final String permission, final Material icon, final byte data, final boolean serverBound, final String... keyWords) {
            this.displayName = displayName;
            this.keyWords = keyWords;
            this.permission = permission;
            this.icon = icon;
            this.color = color;
            this.data = data;
            this.serverBound = serverBound;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getPermission() {
            return permission;
        }

        public Material getIcon() {
            return icon;
        }

        public String getColor() {
            return color;
        }

        public String[] getKeyWords() {
            return keyWords;
        }

        public byte getData() {
            return data;
        }

        public boolean isServerBound() {
            return serverBound;
        }
    }

    public class CoinHistory {
        private final String str;
        private double coins;
        private long lastchange;

        public CoinHistory(final String str, final double coins) {
            this.str = str;
            this.coins = coins;
            this.lastchange = System.currentTimeMillis();
        }

        public void addCoins(final double coins) {
            this.coins += coins;
        }

        public void setLastChange(final long lastChange) {
            this.lastchange = lastChange;
        }

        public long getLastChange() {
            return lastchange;
        }

        public double getCoins() {
            return coins;
        }
    }

    public class CachedHistory {
        private final UUID uuid, uuid2;
        private final String type, entry;
        private String param;
        private final long timestamp;

        public CachedHistory(final UUID uuid, final UUID uuid2, final String type, final String entry, final String param) {
            this.uuid = uuid;
            this.uuid2 = uuid2;
            this.type = type;
            this.entry = entry;
            this.param = param;
            this.timestamp = System.currentTimeMillis();
        }

        public CachedHistory(final UUID uuid, final UUID uuid2, final String type, final String entry, final String param, final long timestamp) {
            this.uuid = uuid;
            this.uuid2 = uuid2;
            this.type = type;
            this.entry = entry;
            this.param = param;
            this.timestamp = timestamp;
        }

        public void setParam(final String param) {
            this.param = param;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getEntry() {
            return entry;
        }

        public String getParam() {
            return param;
        }

        public String getType() {
            return type;
        }

        public UUID getUUID() {
            return uuid;
        }

        public UUID getUUID2() {
            return uuid2;
        }
    }
}