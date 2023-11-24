package me.muxteam.muxteam;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxSystem.Priority;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.ranks.MuxRanks;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

public final class MuxTeam {
    private MuxSystem ms;
    private int applicationsopen, applicationsbuilders, applicationsdevs;
    private final Set<String> supafkplayers = new HashSet<>();
    private final Map<UUID, Long> playtime = new HashMap<>();
    private final Map<String, Long> afktime = new HashMap<>();
    private final String[] staffgroups = new String[]{"§4Owner", "§3Developer", "§3Builder", "§cGameAdmin§6+", "§cGameAdmin", "§aSupporter§6+", "§aSupporter"};

    public MuxTeam(final MuxSystem ms) {
        this.ms = ms;
        final List<Object[]> applications = ms.getDB().getStaffApplications(false);
        updateApplicationsCount(applications);
        if (ms.getHashYAML().contains("teamplaytime")) {
            final Gson gson = new Gson();
            playtime.putAll(gson.fromJson(ms.getHashYAML().getString("teamplaytime"), new TypeToken<HashMap<UUID, Long>>() {}.getType()));
        }
    }

    public void close() {
        final Gson gson = new Gson();
        ms.getHashYAML().set("teamplaytime", gson.toJson(playtime));
        this.ms = null;
    }

    public boolean isAFK(final String pname) {
        return supafkplayers.contains(pname);
    }

    public void removeAFK(final String pname) {
        supafkplayers.remove(pname);
    }

    public boolean handleBuildCommand(final Player p) {
        if (p.hasPermission("muxsystem.builder") == false) {
            ms.sendNoCMDMessage(p);
            return true;
        }
        ms.sendPlayerToServer(p, "build");
        return true;
    }

    public void addAFKTime(final String pname, final Long add) {
        afktime.put(pname, afktime.getOrDefault(pname, 0L) + add);
    }

    public void resetPlayTime() {
        playtime.clear();
        afktime.clear();
        final Server sr = ms.getServer();
        ms.getAFKSince().keySet().stream().filter(s -> sr.getPlayer(s) != null && sr.getPlayer(s).hasPermission("muxsystem.team")).forEach(s -> ms.getAFKSince().put(s, System.currentTimeMillis()));
    }

    public long getPlaytime(final OfflinePlayer p) {
        if (p.isOnline()) {
            final MuxUser u = ms.getMuxUser(p.getName());
            final Long since = ms.getAFKSince().get(p.getName());
            final long currentPlaytime = System.currentTimeMillis() - u.getLoginTimestamp(),
                    afk = afktime.getOrDefault(p.getName(), 0L) + (since != null ? (System.currentTimeMillis() - since) : 0L);
            return playtime.getOrDefault(p.getUniqueId(), 0L) + (currentPlaytime - afk);
        }
        return playtime.get(p.getUniqueId());
    }

    public void handleQuit(final Player p, final MuxUser u) {
        if (u == null) return;
        if (p.hasPermission("muxsystem.team")) {
            final Long afk = afktime.remove(p.getName());
            final long ptime = System.currentTimeMillis() - u.getLoginTimestamp() - (afk != null ? afk : 0L);
            final long toPut = (this.playtime.getOrDefault(p.getUniqueId(), 0L) + ptime);
            this.playtime.put(p.getUniqueId(), toPut);
        }
    }

    public boolean handleCommand(final Player p) {
        if (p.hasPermission("muxsystem.team") == false) {
            short count = 0;
            for (final String str : ms.getPerms().getStaff()) {
                final Player pl = ms.getServer().getPlayerExact(str);
                if (pl != null && p.canSee(pl)) count++;
            }
            ms.showItemBar(p, "§fEs " + (count == 1 ? "ist §6ein Teammitglied §fderzeit §aonline§f." : String.format("sind §6%s Teammitglieder §fderzeit §aonline§f.", String.valueOf(count))));
            return true;
        }
        final MuxRanks.PermissionsGroup group = ms.getPerms().getGroupOf(p.getUniqueId());
        final String rank = group != null ? group.getName() : "";
        final boolean trusted = ms.isTrusted(p.getName());
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxTeam§0 | /team");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        Player temp;
        final Server sr = ms.getServer();
        final List<String> lore = new ArrayList<>();
        for (final String s : staffgroups) {
            final StringBuilder msg = new StringBuilder(s + "§8: ");
            boolean first = true;
            for (final String str : ms.getPerms().getStaff()) {
                if (ms.getPerms().getStaffRank(str).equals(ChatColor.stripColor(s))) {
                    final boolean online = (temp = sr.getPlayerExact(str)) != null && p.canSee(temp), afk = isAFK(str);
                    msg.append(first ? "" : " §8§l| ").append(afk ? "§e" : online ? "§a" : "§7").append(str);
                    first = false;
                }
            }
            if (msg.toString().endsWith(": ")) {
                msg.append("§7").append(ms.getLang("noone"));
            }
            lore.add(msg.toString());
        }
        if (ms.getDB().getClanFromPlayer(p.getUniqueId()) == null) {
            inv.setItem(44, ms.renameItemStack(new ItemStack(Material.IRON_HELMET), "§4§l" + ms.getClans().geTeamClan() + "§3 §lbeitreten",
                    "§7Diese Option ist gerade deshalb da,", "§7weil du derzeit in keinem Clan bist.", "", "§7Klicke, um dem Clan §3beizutreten§7."));
        }
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.WATER_LILY), "§f§lMuxTeam", lore));
        inv.setItem(8, ms.renameItemStack(new ItemStack(Material.COMMAND), "§f§lBefehle", "§7Hier bekommst du eine komplette", "§7Übersicht von deinen Rechten.",
                "", "§fKlicke§7, um diese zu sehen."));
        final int supports = ms.getSupport().getSupportsNeeded(p.isOp());
        inv.setItem(trusted ? 19 : p.hasPermission("muxsystem.supplus") ? 20 : 22, ms.renameItemStack(new ItemStack(Material.SLIME_BALL, Math.max(1, supports)),
                "§f§lSupports" + (supports > 0 ? " §7» §f" + supports : ""),
                "§7Hilf den Spielern, indem du all ihre", "§7Fragen und Probleme sofort löst.", "", supports == 0 ? "§aEs gibt derzeit keine Supports." : "§fKlicke§7, um die Supports zu sehen."));
        if (p.hasPermission("muxsystem.supplus")) {
            final int reports = Math.min(64, p.hasPermission("muxsystem.anticheat") ? ms.getReports().getReports() : ms.getReports().getChatReports());
            inv.setItem(trusted ? 21 : 24, ms.renameItemStack(reports == 0 ? ms.addGlow(new ItemStack(Material.EMPTY_MAP)) : new ItemStack(Material.EMPTY_MAP, Math.max(1, reports)),
                    "§f§lReports" + (reports > 0 ? " §7» §f" + reports : ""), "§7Prüfe nach, ob die Beschwerden", "§7der Spieler gerechtfertigt sind.", "",
                    reports == 0 ? "§aEs gibt derzeit keine Reports." : "§fKlicke§7, um die Reports zu sehen."));
        }
        if (trusted) {
            final int chatfilters = ms.getChatFilter().getNotConfirmed(), applications = getApplicationsOpen(rank.contains("Dev") ? Material.PISTON_BASE : rank.contains("Builder") ? Material.BRICK : null);
            inv.setItem(25, ms.renameItemStack(applications == 0 ? ms.addGlow(new ItemStack(Material.LONG_GRASS, 1, (byte) 2)) : new ItemStack(Material.LONG_GRASS, Math.max(1, Math.min(64, applications)), (byte) 2),
                    "§f§lBewerbungen" + (applications > 0 ? " §7» §f" + applications : ""), "§7Hier findest du all diejenigen Spieler,", "§7die dem MuxTeam beitreten möchten.", "",
                    applications == 0 ? "§aEs gibt keine offene Bewerbungen." : "§fKlicke§7, um die Bewerbungen zu sehen."));
            inv.setItem(23, ms.renameItemStack(chatfilters == 0 ? ms.addGlow(new ItemStack(Material.HOPPER)) : new ItemStack(Material.HOPPER, Math.max(1, chatfilters)),
                    "§f§lChatfilter" + (chatfilters > 0 ? " §7» §f" + chatfilters : ""), "§7Alle Wörter, die noch nicht überprüft", "§7wurden, werden komplett aufgelistet.", "",
                    chatfilters == 0 ? "§aEs gibt derzeit keine Vorschläge." : "§fKlicke§7, um die Vorschläge zu sehen."));
        }
        final long play = getPlaytime(p);
        final String title = play < 3600000L ? "§c§lKaum Aktivität": play < 18000000L ? "§6§lWenig Aktivität" : "§f§lHinweis zur Aktivität";
        final String timecolor = title.substring(0, 2);
        final List<String> lore2 = new ArrayList<>(Arrays.asList("§7Wenn du zeitweise inaktiv bist oder keine", "§7Spieler mehr von dir supported werden,", "§7dann bekommen automatisch neue Spieler",
                "§7deinen Platz, um anderen zu helfen.", "", "§7Aktive Zeit: " + timecolor + ms.timeToString(play, false)));
        if (p.hasPermission("muxsystem.vanish")) {
            final int supsdone = ms.getDB().getSupportLikeAndSupportCount(p.getUniqueId())[0];
            final String color = supsdone < 2 ? "§c" : supsdone < 8 ? "§6" : "§f";
            lore2.add("§7Supports: " + color + supsdone + " Spieler geholfen");
        }
        final boolean worker = rank.startsWith("Dev") || rank.startsWith("Builder");
        if (worker == false) inv.setItem(40, ms.renameItemStack(new ItemStack(Material.RED_ROSE, 1, timecolor.equals ("§c") ? (byte) 4 : timecolor.equals("§6") ? (byte) 5 : (byte) 6), title, lore2));
        final InvType it = ms.getActiveInv(p.getName());
        if (it != InvType.TEAM && it != InvType.REPORTS && it != InvType.SUPPORT && it != InvType.FILTER && it != InvType.MENU) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.TEAM);
        return true;
    }

    public void handleInventory(final Player p, final ItemStack i, final String invname) {
        final Material m = i.getType();
        if (m == Material.COMMAND && invname.contains("Befehle") == false) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxTeam§0 | Befehle");
            inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
            inv.setItem(4, ms.renameItemStack(new ItemStack(Material.COMMAND), "§f§lBefehle", "§7Hier bekommst du eine komplette", "§7Übersicht von deinen Rechten."));
            if (p.isOp()) {
                inv.setItem(8, ms.renameItemStack(new ItemStack(Material.GOLDEN_APPLE), "§e§lOperator",
                        "§e- §7/a §e» §7Admin Panel",
                        "§e- §7/event §e» §7Events starten",
                        "§e- §7/sup §e» §7Support Statistiken & Kontakte",
                        "§e- §7/smsg §e» §7MSG als 'MuxSupport' senden",
                        "§e- §7/countdown §e» §7Countdown starten",
                        "§e- §7/base tp §e» §7Gelange zu Spielerbasen",
                        "§e- §7/say §e» §7Chat Ankündigung senden",
                        "§e- §7/werben [name] §e» §7Geworbene sehen",
                        "§e- §7/id §e» §7ID von Items einsehen",
                        "§e- §7/lightning §e» §7Blitzmodus aktivieren",
                        "§e- §7/time §e» §7Serverzeit setzen",
                        "§e- §7/speed §e» §7Laufgeschwindigkeit setzen",
                        "",
                        "§e+ §7Mehr Kategorien bei History & Overview",
                        "§e+ §7Zugriff auf mehr Chatfarben",
                        "§e+ §7Vanilla Befehle wie /whitelist, /pl",
                        "§e+ §7Teleportiere zu jedem Spieler & Clanort",
                        "§e+ §7Alle gefilterten & Clan Nachrichten sehen",
                        "§e+ §7Homes überall & für andere Spieler setzen",
                        "§e+ §7Immunität vor Strafen & Forcefield",
                        "§e+ §7Stats im Casino sehen"
                ));
            }
            final MuxRanks.PermissionsGroup group = ms.getPerms().getGroupOf(p.getUniqueId());
            final String rank = group == null ? "" : group.getName();
            final boolean dev = rank.startsWith("Dev"), builder = rank.startsWith("Builder");
            final String color = dev || builder ? "§3" : "§2";
            if (p.hasPermission("muxsystem.support")) {
                inv.setItem(19, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, dev || builder ? (byte) 9 : (byte) 5),
                        color + "§l" + (dev ? "Developer" : builder ? "Builder" : "Supporter"),
                        (dev ? color + "- §7/uuid, /getloc, /muxtimings " + color + "» §7Dev Befehle" : builder ? color + "- §7/build " + color + "» §7Zum Bauserver" :
                                color + "- §7/sup " + color + "» §7Support-Menü"), color + "- §7/team " + color + "» §7Team-Menü", color + "- §7/history [name] " + color + "» §7Verlauf sehen",
                        color + "- §7/o [name] " + color + "» §7Spieler Übersicht", color + "- §7!nachricht " + color + "» §7TeamChat", "", color + "+ §7Chatfarbe setzen (Einstellungen)",
                        color + "+ §7Offline MSGs an jeden", color + "+ §7Farbige MSGs §7(/farben)"));
            }
            if (p.hasPermission("muxsystem.supplus")) {
                inv.setItem(21, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 5), "§2§lSupporter§6§l+",
                        "§2- §7/cc §2» §7Chat leeren", "§2- §7/report §2» §7Chatreports", "§2- §7/o [name] §2» §7Chat verlangsamen", "§2- §7/chatfilter §2» §7Wörter filtern",
                        "§2- §7/history §2» §7Globaler Verlauf",
                        "", "§2+ §7Spam Schutz umgehen", "§2+ §7Bei deaktiviertem Chat schreiben"));
            }
            if (p.hasPermission("muxsystem.vanish")) {
                final boolean plus = p.isOp();
                final List<String> lore = new ArrayList<>();
                lore.add("§c- §7/fly §c» §7Flug- & Godmodus");
                lore.add("§c- §7/o [name] §c» §7Mehr Optionen");
                lore.add("§c- §7/report §c» §7Cheatreports");
                lore.add("§c- §7/tp §c» §7Teleportieren");
                lore.add("§c- §7/jump | /top §c» §7Teleportieren");
                lore.add("§c- §7/vanish §c» §7Unsichtbar machen");
                lore.add("§c- §7/speed §c» §7Schneller fliegen");
                lore.add("§c- §7/heal §c» §7Heilung ohne Wartezeit");
                lore.add("§c- §7/warps §c» §7Warpliste");
                if (plus) {
                    lore.add("§6- §7/tphere §c» §7Zu dir teleportieren");
                    lore.add("§6- §7/head §c» §7Eigener Kopf");
                    lore.add("§6- §7/silent §c» §7Chat de(aktivieren)");
                }
                lore.add("");
                lore.add("§c+ §7Mehr Einstellungen im Menü");
                lore.add("§c+ §7Gesamte MSGs lesen");
                lore.add("§c+ §7Farbig auf Schilder schreiben");
                lore.add("§c+ §7Zugriff auf AntiCheat");
                lore.add("§c+ §7Zugriff auf mehr Chatfarben");
                if (plus) {
                    lore.add("§6+ §7Zweitaccounts sehen");
                    lore.add("§6+ §7Kein Chatfilter");
                }
                inv.setItem(23, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 14), "§c§lGameAdmin", lore.toArray(new String[0])));
            }
            if (ms.isTrusted(p.getName())) {
                inv.setItem(25, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 15), "§4§lTrusted",
                        "§4- §7/holo §4» §7Hologramme",
                        "§4- §7/poll §4» §7Umfragen starten",
                        "§4- §7/team §4» §7Bewerbungen & Chatfilter",
                        "§4- §7/tppos §4» §7Koordinaten Teleport",
                        "§4- §7/head [name] §4» §7Kopf von Spieler",
                        "§4- §7/enchant §4» §7Items enchanten",
                        "§4- §7/tp [name] [name2] §4» §7Teleportieren",
                        "§4- §7/rename §4» §7Items umbenennen",
                        "§4- §7/email add §4» §7Domains hinzufügen",
                        "§4- §7/speed §4» §7Speed von Spielern setzen",
                        "§4- §7/set §4» §7Alle Maps bearbeiten",
                        "",
                        "§4+ §7Administrativ & Daten in Overview",
                        "§4+ §7Alle Kisten sehen & Kisten vergeben",
                        "§4+ §7Zahlungen sehen bei History",
                        "§4+ §7MSGs nicht lesbar von OPs",
                        "§4+ §7JoinMSG im Admin Menü setzen",
                        "§4+ §7Clans löschen",
                        "§4+ §7Limits: 5 Flyspeed, 5 Votes bei Overview",
                        "§4+ §7Zugriff auf mehr Chatfarben",
                        "§4+ §7Alle Bukkit Befehle",
                        "§4+ §7Homes löschen von anderen Spielern"));
            }
            if (ms.getActiveInv(p.getName()) != InvType.TEAM) p.closeInventory();
            p.openInventory(inv);
            ms.setActiveInv(p.getName(), InvType.TEAM);
            return;
        } else if (m == Material.IRON_HELMET) {
            if (ms.getDB().getClanCase(ms.getClans().geTeamClan()) == null) {
                p.closeInventory();
                ms.showItemBar(p, "§cDer Clan wurde noch nicht erstellt.");
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                return;
            }
            ms.getClans().joinClan(p, ms.getClans().geTeamClan(), -1);
            p.performCommand("c");
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            return;
        } else if (m == Material.HOPPER) {
            if (i.getItemMeta().getLore().get(3).startsWith("§a")) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            ms.getChatFilter().openFilterConfirmInventory(p);
            return;
        } else if (m == Material.EMPTY_MAP && invname.contains("Reports") == false) {
            if (i.getItemMeta().getLore().get(3).startsWith("§a")) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            p.performCommand("report");
            return;
        } else if (m == Material.SLIME_BALL && invname.contains("Support") == false) {
            if (i.getItemMeta().getLore().get(3).startsWith("§a")) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            p.performCommand("support" + (p.isOp() && ms.getDB().getContactMessages(false).isEmpty() == false ? " kontakt" : ""));
            return;
        } else if (m == Material.LONG_GRASS && invname.contains("Bewerbungen") == false) {
            if (invname.isEmpty() == false) p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxTeam§0 | Bewerbungen");
            inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
            inv.setItem(4, ms.renameItemStack(new ItemStack(Material.LONG_GRASS, 1, (byte) 2), "§f§lBewerbungen", "§7Hier findest du all diejenigen Spieler,", "§7die dem MuxTeam beitreten möchten."));
            inv.setItem(8, ms.renameItemStack(new ItemStack(Material.CHEST), "§f§lArchiv"));
            final List<Object[]> apps = ms.getDB().getStaffApplications(false);
            final MuxRanks.PermissionsGroup group = ms.getPerms().getGroupOf(p.getUniqueId());
            final String rank = group == null ? "" : group.getName();
            final Material filter = rank.contains("Builder") ? Material.BRICK : rank.contains("Dev") ? Material.PISTON_BASE : null;
            if (apps != null) {
                applicationsInventory(inv, apps, (short) 18, filter, false);
            }
            updateApplicationsCount(apps);
            if (ms.getActiveInv(p.getName()) != InvType.TEAM) p.closeInventory();
            p.openInventory(inv);
            ms.setActiveInv(p.getName(), InvType.TEAM);
            return;
        } else if (m == Material.CHEST && invname.contains("archiv") == false) {
            if (invname.contains("Bewerbungen") == false && invname.isEmpty() == false) {
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                final String plname = ChatColor.stripColor(invname.split(" \\| ")[1]);
                ms.getDB().setStaffDone(plname, true);
                handleInventory(p, new ItemStack(Material.LONG_GRASS), "");
                return;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxTeam§0 | Bewerbungsarchiv");
            inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
            inv.setItem(4, ms.renameItemStack(new ItemStack(Material.CHEST), "§f§lArchiv"));
            final List<Object[]> apps = ms.getDB().getStaffApplications(true);
            final MuxRanks.PermissionsGroup group = ms.getPerms().getGroupOf(p.getUniqueId());
            final String rank = group == null ? "" : group.getName();
            final Material filter = rank.contains("Builder") ? Material.BRICK : rank.contains("Dev") ? Material.PISTON_BASE : null;
            if (apps != null) {
                applicationsInventory(inv, apps, (short) 18, filter, true);
            }
            if (ms.getActiveInv(p.getName()) != InvType.TEAM) p.closeInventory();
            p.openInventory(inv);
            ms.setActiveInv(p.getName(), InvType.TEAM);
            return;
        } else if (m == Material.PAINTING) {
            final String link = ChatColor.stripColor(i.getItemMeta().getLore().get(1));
            ms.openBook(ms.linkBook("Referenz", "Unten findest du die Referenz vom Spieler.", new Object[]{link, "Zur Referenz >", "Link öffnen"}), p);
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            return;
        } else if (m == Material.BOOK_AND_QUILL) {
            p.sendMessage(ms.header((byte) 13, "§f"));
            p.sendMessage("  §f§lMuxBewerbung");
            p.sendMessage(" ");
            final String plname = ChatColor.stripColor(invname.split(" \\| ")[1]);
            ms.chatClickHoverShow(p, "  §7Klicke §fhier§7, um den Spieler anzuschreiben.", "§f§oKlicke um anzuschreiben", "/msg " + plname + " ");
            String discord = null;
            final List<Object[]> apps = ms.getDB().getStaffApplications(plname);
            if (apps == null) return;
            for (final String s : ((String) apps.get(0)[4]).split("\n")) {
                final String[] split = s.substring(1).split(":", 2);
                final String category = split[0];
                if (category.contains("Discord")) discord = split[1].substring(1);
            }
            if (discord != null && discord.equals("Ja") == false && discord.equals("Nein") == false) {
                ms.chatClickHoverShow(p, "  §7Klicke §fhier§7, um den Discord Namen zu kopieren.", "§f§oKlicke um den Discord zu sehen", discord);
            }
            p.sendMessage(ms.footer((byte) 13, "§f"));
            p.closeInventory();
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            return;
        } else if ((m == Material.SLIME_BLOCK || m == Material.BRICK || m == Material.PISTON_BASE) && i.hasItemMeta()) {
            final List<String> lore = i.getItemMeta().getLore();
            final String plname = ChatColor.stripColor(i.getItemMeta().getDisplayName());
            if (lore.get(lore.size() - 1).contains("bearbeiten")) {
                p.performCommand("bewerbung " + plname);
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                return;
            } else if (lore.get(lore.size() - 1).contains("archiv")) {
                ms.getDB().setStaffDone(plname, true);
                handleInventory(p, new ItemStack(Material.LONG_GRASS), "");
            } else {
                ms.getDB().setStaffDone(ChatColor.stripColor(i.getItemMeta().getDisplayName()), false);
                handleInventory(p, new ItemStack(Material.CHEST), "");
            }
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            return;
        } else if (m == Material.ITEM_FRAME) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            if (invname.contains("/team")) {
                p.performCommand("menu");
                return;
            }
            if (invname.contains("archiv") || i.getItemMeta().getDisplayName().contains("Bewerbungen")) {
                handleInventory(p, new ItemStack(Material.LONG_GRASS), "");
                return;
            }
        } else {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        }
        handleCommand(p);
    }

    public boolean handlePlayerApplications(final Player p, final String[] args) {
        Entry<String, OfflinePlayer> entry = ms.getPlayerAndName(args[0]);
        if (entry.getKey() == null) {
            entry = new AbstractMap.SimpleEntry<>(args[0], null);
        }
        final List<Object[]> openapps = ms.getDB().getStaffApplications(false);
        final List<Object[]> apps = ms.getDB().getStaffApplications(entry.getKey());
        if (apps == null || apps.isEmpty()) {
            ms.showItemBar(p, "§cDieser Spieler hat noch keine Bewerbung eingereicht.");
            return true;
        }
        final boolean openapp = openapps != null && apps.stream().map(b -> b[0]).anyMatch(openapps.stream().map(b2 -> b2[0]).collect(Collectors.toSet())::contains);
        final Inventory inv = ms.getServer().createInventory(null, openapp ? 45 : 27, "§0§lMuxTeam§0 | " + entry.getKey());
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), "§7Zurück (Bewerbungen)"));
        inv.setItem(4, ms.renameItemStack(ms.getHead(entry.getKey()), "§f§l" + entry.getKey()));
        String reference = null;
        for (final String s : ((String) apps.get(0)[4]).split("\n")) {
            final String[] split = s.substring(1).split(":", 2);
            final String category = split[0];
            if (category.contains("Referenz")) reference = split[1].substring(1);
        }
        final int startpos = openapp ? 36 : 18;
        final MuxRanks.PermissionsGroup group = ms.getPerms().getGroupOf(p.getUniqueId());
        final String rank = group == null ? "" : group.getName();
        final Material filter = rank.contains("Builder") ? Material.BRICK : rank.contains("Dev") ? Material.PISTON_BASE : null;
        applicationsInventory(inv, apps, (short) startpos, filter, true);
        if (openapp) {
            final boolean ref = reference != null;
            if (ref) inv.setItem(19, ms.renameItemStack(new ItemStack(Material.PAINTING), "§f§lReferenz ansehen",
                    "§7Link: §f" + reference.substring(0, Math.min(reference.length(), 20)), "§0" + reference, "§fKlicke§7, um die Referenz", "§7zu sehen."));
            inv.setItem(ref ? 22 : 20, ms.renameItemStack(new ItemStack(Material.BOOK_AND_QUILL), "§f§lSpieler anschreiben", "", "§fKlicke§7, um den Spieler", "§7anzuschreiben."));
            inv.setItem(ref ? 25 : 24, ms.renameItemStack(new ItemStack(Material.CHEST), "§f§lBewerbung archivieren", "", "§fKlicke§7, um die Bewerbung", "§7zu archivieren."));
        } else {
            if (inv.getItem(19) == null) {
                inv.setItem(22, inv.getItem(18));
                inv.setItem(18, null);
            } else if (inv.getItem(20) == null) {
                inv.setItem(20, inv.getItem(18));
                inv.setItem(24, inv.getItem(19));
                inv.setItem(18, null);
                inv.setItem(19, null);
            }
        }
        if (ms.getActiveInv(p.getName()) != InvType.TEAM) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.TEAM);
        return true;
    }

    private void applicationsInventory(final Inventory inv, final List<Object[]> apps, final short startpos, final Material filter, final boolean archived) {
        short pos = startpos;
        final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("CET"));
        for (final Object[] obj : apps) {
            final String msg = (String) obj[4], time = sdf.format(new Date((Long) obj[2]));
            Material type = Material.SLIME_BLOCK;
            final List<String> lore = new ArrayList<>();
            for (final String s : msg.split("\n")) {
                final String[] split = s.substring(1).split(":");
                final String category = split[0];
                String color = "§f", value = split[1];
                if (category.contains("Persönlichkeit")) {
                    if (value.contains("Emotional")) color = "§e";
                } else if (category.contains("Manipulativ") || category.startsWith("Narzis") || category.contains("Psychopath")) {
                    final double score = Double.parseDouble(value.split("/")[0]);
                    if (score >= 3) color = "§4";
                    else if (score >= 2.5) color = "§c";
                    else if (score >= 2.0) color = "§e";
                } else if (category.contains("Zeit gebraucht")) {
                    final int zeit = Integer.parseInt(value.split(" ")[1]);
                    value = " " + ms.getTime(zeit);
                    if (zeit < 100) color = "§c";
                    else if (zeit < 200) color = "§e";
                } else if (category.contains("Beworben als")) {
                    final String rank = value.replace(" ", "");
                    if (rank.equals("Builder")) type = Material.BRICK;
                    else if (rank.equals("Developer")) type = Material.PISTON_BASE;
                    else type = Material.SLIME_BLOCK;
                } else if (category.contains("Referenz")) continue;
                lore.add("§7" + category + ":" + color + value);
            }
            lore.remove(0);
            lore.remove(0);
            if (filter != null && type != filter) continue;
            inv.setItem(pos, ms.addLore(ms.addLore(ms.renameItemStack(new ItemStack(type), "§f§l" + obj[0],
                    "§7Zeitpunkt: §f" + time), lore), startpos > 18 ? new String[]{} : new String[]{"", "§fKlicke§7, um die Bewerbung", archived ? "§7wieder zu eröffnen." : "§7zu bearbeiten."}));
            pos++;
            if (pos >= inv.getSize()) break;
        }
    }

    private void updateApplicationsCount(final List<Object[]> apps) {
        if (apps != null) {
            applicationsopen = apps.size();
            applicationsdevs = applicationsbuilders = 0;
            for (final Object[] obj : apps) {
                final String msg = (String) obj[4];
                if (msg.contains("Beworben als: Builder")) {
                    applicationsbuilders++;
                } else if (msg.contains("Beworben als: Developer")) {
                    applicationsdevs++;
                }
            }
        } else {
            applicationsopen = applicationsbuilders = applicationsdevs = 0;
        }
    }

    private int getApplicationsOpen(final Material filter) {
        return filter == Material.BRICK ? applicationsbuilders : filter == Material.PISTON_BASE ? applicationsdevs : applicationsopen;
    }

    public boolean toggleAFK(final Player p) {
        if (p.hasPermission("muxsystem.team") == false) return false;
        final String name = p.getName();
        if (ms.checkGeneralCooldown(p.getName(), "AFK", 5000L, true)) return false;
        if (supafkplayers.remove(name)) {
            ms.broadcastMessage("§6§lStatus>§7 " + String.format(ms.getLang("teamnotafk"), name), "muxsystem.team", Priority.NORMAL);
        } else {
            supafkplayers.add(name);
            ms.broadcastMessage("§6§lStatus>§7 " + String.format(ms.getLang("teamafk"), name), "muxsystem.team", Priority.NORMAL);
        }
        return true;
    }
}