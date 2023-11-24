package me.muxteam.muxteam;

import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.ranks.MuxRanks.PermissionsGroup;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxSystem.Priority;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class MuxSupport {
    private MuxSystem ms;
    private final Random r = new Random();
    private final DecimalFormat df = new DecimalFormat("#0.00");
    private final List<ItemStack> supheads;
    private final HashMap<String, Long> suplist = new HashMap<>();
    private final Map<UUID, ExpectedSupporterData> expectedSupporter = new HashMap<>();
    private final Map<UUID, String> supbans = new HashMap<>();
    private final Set<UUID> seenfaq = new HashSet<>();
    private final Map<Player, Player> support = new HashMap<>();
    private final Map<UUID, UUID> supmove = new HashMap<>();
    private final Map<String, SupportInfo> supinfo = new HashMap<>();
    private final Map<String, Integer> supids = new HashMap<>();
    private final Map<String, Map.Entry<UUID, SupportInfo>> suplike = new HashMap<>();

    public MuxSupport(final MuxSystem ms) {
        this.ms = ms;
        // Minecraft Heads for numbers 1-7
        this.supheads = Arrays.asList(ms.getHeadFromURL("https://textures.minecraft.net/texture/71bc2bcfb2bd3759e6b1e86fc7a79585e1127dd357fc202893f9de241bc9e530", ""),
                ms.getHeadFromURL("https://textures.minecraft.net/texture/4cd9eeee883468881d83848a46bf3012485c23f75753b8fbe8487341419847", ""),
                ms.getHeadFromURL("https://textures.minecraft.net/texture/1d4eae13933860a6df5e8e955693b95a8c3b15c36b8b587532ac0996bc37e5", ""),
                ms.getHeadFromURL("https://textures.minecraft.net/texture/d2e78fb22424232dc27b81fbcb47fd24c1acf76098753f2d9c28598287db5", ""),
                ms.getHeadFromURL("https://textures.minecraft.net/texture/6d57e3bc88a65730e31a14e3f41e038a5ecf0891a6c243643b8e5476ae2", ""),
                ms.getHeadFromURL("https://textures.minecraft.net/texture/334b36de7d679b8bbc725499adaef24dc518f5ae23e716981e1dcc6b2720ab", ""),
                ms.getHeadFromURL("https://textures.minecraft.net/texture/6db6eb25d1faabe30cf444dc633b5832475e38096b7e2402a3ec476dd7b9", ""));
    }

    public void close() {
        this.ms = null;
    }

    public void handleExpectedSupporters() {
        final Server sr = ms.getServer();
        expectedSupporter.entrySet().removeIf(entry -> {
            final Player p = sr.getPlayer(entry.getKey());
            if (p == null) return true;
            if (entry.getValue().getTimestamp() <= System.currentTimeMillis() || sr.getPlayer(entry.getValue().getSupporter()) == null) {
                final AtomicInteger i = new AtomicInteger(0);
                final String name = p.getName();
                final Player expected = sr.getPlayer(entry.getValue().getSupporter());
              //  suplist.replace(name, System.currentTimeMillis()); @ Peter, ja / nein?
                ms.getServer().getOnlinePlayers().stream().filter(player -> player.hasPermission("muxsystem.support"))
                        .filter(player -> expected == null || player.getName().equals(expected.getName()) == false)
                        .filter(supporter -> (i.getAndIncrement() != 0 || (ms.getTeam().isAFK(supporter.getName()) == false && p.canSee(supporter) && ms.isTrusted(supporter.getName()) == false)))
                        .forEach(onlineSupporter -> {
                            for (byte b = 0; b < 3; b++) onlineSupporter.playSound(onlineSupporter.getLocation(), Sound.ORB_PICKUP, 0.5F, 1F);
                            ms.chatClickHoverRun(onlineSupporter, String.format(ms.getLang("support.needshelp"), name), ms.getLang("support.clicktosupport"), "/sup " + name);
                        });
                return true;
            }
            return false;
        });
    }

    public boolean handleCommand(final Player p, final String[] args) {
        if (args.length == 2 && args[0].equals("like")) {
            UUID uuid;
            try {
                uuid = UUID.fromString(ms.fromArgs(args, 1));
            } catch (final Exception ex) {
                return true;
            }
            if (handleSupportLike(p, uuid) == false) p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return true;
        } else if (p.hasPermission("muxsystem.support") == false || (args.length == 1 || args.length == 2) == false) {
            if (p.hasPermission("muxsystem.support")) {
                final InvType it = ms.getActiveInv(p.getName());
                final boolean quick = it == InvType.TEAM || it == InvType.MENU;
                final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lMuxSupport§0 | /support");
                if (quick) inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back") + (it == InvType.TEAM ? " (Team)" : "")));
                inv.setItem(p.isOp() && quick == false ? 1 : 4, ms.addGlow(ms.renameItemStack(new ItemStack(Material.SLIME_BALL), "§2§lMuxSupport",
                        p.isOp() ? new String[]{"§2- §7/sup [spieler] §2» §7Support beginnen", "§2- §7/sup c §2» §7Support schließen",
                                "§2- §7/sup move [supporter] §2» §7Weiterleiten", "§2- §7/sup ban [spieler | list] §2» §7Ausschließen",
                                "§2- §7%nachricht §2» §7Globale Nachricht schreiben",
                                "§2- §c/sup stats [name] §2» §7Support-Statistik"} : new String[]{"§2- §7/sup [spieler] §2» §7Support beginnen",
                                "§2- §7/sup c §2» §7Support schließen", "§2- §7/sup move [supporter] §2» §7Weiterleiten", "§2- §7/sup ban [spieler | list] §2» §7Ausschließen", "§2- §7%nachricht §2» §7Globale Nachricht schreiben"})));
                if (p.isOp() && quick == false) {
                    inv.setItem(7, ms.renameItemStack(new ItemStack(Material.GOLD_INGOT), "§2§lTop Supporter§c [ADMIN]",
                            "§7Die Kategorie zeigt, wer vom Team", "§7sich sehr angestrengt hat.", "", "§7Klicke, um die §2besten §7zu sehen."));
                    inv.setItem(4, ms.renameItemStack(new ItemStack(Material.BOOK), "§2§lKontaktformular§c [ADMIN]", "§7Hier lassen sich Anfragen verwalten,",
                            "§7die per Webseite abgeschickt wurden.", "", "§7Klicke, um die §2Anfragen §7zu sehen."));
                }
                String name = null;
                if (support.containsKey(p)) {
                    name = support.get(p).getName();
                } else if (support.containsValue(p)) {
                    final Player supporter = getSupporterOf(p);
                    if (supporter != null) name = supporter.getName();
                }
                if (name != null) {
                    inv.setItem(22, ms.renameItemStack(ms.getHead(name), "§2§l" + name, "§7Mit diesem Spieler befindest", "§7du dich §2jetzt im Support§7."));
                } else {
                    byte count = 1;
                    short pos = 19;
                    for (final String s : suplist.keySet()) {
                        if (pos == 26) break;
                        final Player sP = ms.getServer().getPlayer(s);
                        if (sP == null) continue;
                        final UUID sId = sP.getUniqueId();
                        final ExpectedSupporterData data = expectedSupporter.get(sId);
                        if (data != null && data.getSupporter().equals(p.getUniqueId()) == false) continue;
                        inv.setItem(pos, ms.renameItemStack(supheads.get(count - 1), "§2§l" + s, "", "§7Klicke, um den Spieler", "§7zu §2supporten§7."));
                        pos++;
                        count++;
                    }
                }
                if (it != InvType.SUPPORT && it != InvType.MENU && quick == false) p.closeInventory();
                p.openInventory(inv);
                ms.setActiveInv(p.getName(), InvType.SUPPORT);
                return true;
            } else if (support.containsValue(p)) {
                final Player supporter = getSupporterOf(p);
                if (supporter == null) return true;
                ms.showItemBar(p, String.format(ms.getLang("support.pcurrentsupport"), supporter.getName()));
                return true;
            } else if (suplist.containsKey(p.getName())) {
                ms.showItemBar(p, ms.getLang("support.alreadysent"));
                return true;
            } else if (seenfaq.remove(p.getUniqueId()) == false) {
                seenfaq.add(p.getUniqueId());
                ms.openBook(ms.mixedBook("§2§lSupport", "Klicke unten, um die häufig gestellten Fragen zu lesen.",
                        new Object[]{"https://muxcraft.eu/support", "Zu den Fragen >\n\n\n", "Link öffnen", ClickEvent.Action.OPEN_URL},
                        new Object[]{"/support", "§2§nChat-Support §2§nbeantragen", "Jetzt beantragen", ClickEvent.Action.RUN_COMMAND}), p);
                return true;
            } else if (supbans.containsKey(p.getUniqueId()) || supbans.containsValue(p.getAddress().getAddress().getHostAddress())) {
                ms.showItemBar(p, ms.getLang("support.yourebanned"));
                return true;
            }
            final String name = p.getName();
            final AtomicInteger i = new AtomicInteger(0);
            final List<Player> possibleSupporters = ms.getServer().getOnlinePlayers().stream().filter(player -> player.hasPermission("muxsystem.support"))
                    .filter(supporter -> (i.getAndIncrement() != 0 || (ms.getTeam().isAFK(supporter.getName()) == false || p.canSee(supporter) || ms.isTrusted(supporter.getName()) == false)))
                    .collect(Collectors.toList());
            if (possibleSupporters.isEmpty()) {
                ms.showItemBar(p, ms.getLang("support.nostaff"));
                return true;
            }
            final Player supporter = possibleSupporters.remove(r.nextInt(possibleSupporters.size()));
            if (supporter == null) return true;
            for (byte b = 0; b < 3; b++) supporter.playSound(supporter.getLocation(), Sound.ORB_PICKUP, 0.5F, 1F);
            ms.chatClickHoverRun(supporter, String.format(ms.getLang("support.needshelp"), name), ms.getLang("support.clicktosupport"), "/sup " + name);

            p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.5F, 1F);
            suplist.put(name, System.currentTimeMillis());
            int id = ms.getDB().addSupportEntry(name);
            supids.put(p.getName(), id);
            p.sendMessage(new String[]{
                    ms.header((byte) 14, "§2"),
                    "  §2§lMuxSupport",
                    "",
                    "  " + ms.getLang("support.hasbeensent"),
                    "  " + ms.getLang("support.hasbeensent2"),
                    ms.footer((byte) 14, "§2")
            });
            expectedSupporter.put(p.getUniqueId(), new ExpectedSupporterData(System.currentTimeMillis() + 10_000L, supporter.getUniqueId()));
            return true;
        } else if (args[0].equalsIgnoreCase("c") || args[0].equalsIgnoreCase("close")) {
            final Player target;
            if (support.containsKey(p)) {
                target = support.remove(p);
                p.sendMessage("§2§lMuxSupport>§7 " + ms.getLang("support.finished"));
                if (supmove.containsKey(p.getUniqueId()) == false) {
                    suplike.put(target.getName(), new AbstractMap.SimpleEntry<>(p.getUniqueId(), supinfo.get(target.getName())));
                    target.sendMessage(new String[]{
                            ms.header((byte) 14, "§2"),
                            "  " + ms.getLang("support.thanks1"),
                            "  " + ms.getLang("support.thanks2"),
                            "  ",
                    });
                    ms.chatClickHoverRun(target, "  §7Hat dir der Support weitergeholfen? Klicke §2hier§7.", "§2§oKlicke um zu danken", "/support like " + p.getUniqueId());
                    target.sendMessage(ms.footer((byte) 14, "§2"));
                }
            } else if (support.containsValue(p)) { // For Staff members helping each other
                target = getSupporterOf(p);
                ms.showItemBar(target, String.format(ms.getLang("support.closed"), p.getName()));
                support.remove(target);
                ms.showItemBar(p, ms.getLang("support.youclosed"));
            } else {
                ms.showItemBar(p, ms.getLang("support.notinsupport"));
                return true;
            }
            if (target == null) return true;
            saveSupport(p.getUniqueId(), target.getName());
            return true;
        } else if (args[0].equalsIgnoreCase("kontakt")) {
            if (p.isOp() == false) return true;
            final boolean team = ms.getActiveInv(p.getName()) == InvType.TEAM;
            final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lMuxSupport§0 | Kontaktformular");
            short i = 19;
            if (team) inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back") + " (Team)"));
            if (team == false)
                inv.setItem(1, ms.renameItemStack(new ItemStack(Material.SLIME_BALL), "§2§lMuxSupport", "§7Schaue hier nach, welche Spieler", "§7noch nicht supportet wurden.", "", "§7Klicke, um die §2Supports §7zu sehen."));
            inv.setItem(4, ms.addGlow(ms.renameItemStack(new ItemStack(Material.BOOK), "§2§lKontaktformular§c [ADMIN]", "§7Hier lassen sich Anfragen verwalten,", "§7die per Webseite abgeschickt wurden.")));
            inv.setItem(5, ms.renameItemStack(new ItemStack(Material.BOOKSHELF), "§2§lArchiv", "", "§7Klicke, um den §2Archiv §7einzusehen."));
            if (team == false)
                inv.setItem(7, ms.renameItemStack(new ItemStack(Material.GOLD_INGOT), "§2§lTop Supporter§c [ADMIN]", "§7Die Kategorie zeigt, wer vom Team", "§7sich sehr angestrengt hat.", "", "§7Klicke, um die §2besten §7zu sehen."));
            final boolean fulltrusted = ms.isFullTrusted(p.getName());
            final List<Object[]> messages = ms.getDB().getContactMessages(false);
            if (messages.isEmpty() == false) {
                byte count = 1;
                final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                sdf.setTimeZone(TimeZone.getTimeZone("CET"));
                for (final Object[] obj : messages) {
                    final int id = (Integer) obj[5];
                    final String msg = (String) obj[4], time = sdf.format(new Date((Long) obj[2]));
                    inv.setItem(i, ms.addLore(ms.addLore(ms.renameItemStack(supheads.get(count - 1), "§2§lKontakt §2#§l" + count + " §0" + id, "§7Spieler: §f" + obj[0], (fulltrusted ? "§7Email: §f" + obj[1] : ""),
                            "§7Zeitpunkt: §f" + time, "§7Thema: §f" + obj[3], "§7Nachricht:"), ms.getLoreFromLongText(msg, "§f")), "", "§2Linksklick§7, um den Spieler anzuschreiben.", "§cRechtsklick§7, um die Anfrage zu schliessen§7."));
                    i++;
                    count++;
                    if (i == 26) break;
                }
            }
            if (ms.getActiveInv(p.getName()) != InvType.SUPPORT && team == false) p.closeInventory();
            p.openInventory(inv);
            ms.setActiveInv(p.getName(), InvType.SUPPORT);
            return true;
        } else if (args[0].equalsIgnoreCase("move")) {
            if (args.length == 1) {
                ms.showItemBar(p, ms.usage("/sup move [supporter]"));
                return true;
            }
            final Player target = ms.getServer().getPlayer(args[1]);
            if (target == null || p.canSee(target) == false) {
                ms.showItemBar(p, ms.hnotonline);
                return true;
            } else if (target.getName().equals(p.getName())) {
                ms.showItemBar(p, ms.getLang("support.notyourself"));
                return true;
            } else if (support.containsKey(p) == false) {
                ms.showItemBar(p, ms.getLang("support.notinsupport"));
                return true;
            } else if (target.hasPermission("muxsystem.support") == false) {
                ms.showItemBar(p, "§cDer Spieler ist kein Supporter.");
                return true;
            } else if (support.containsValue(target)) {
                final Player supporter = getSupportedOf(target);
                if (supporter == null) return true;
                ms.showItemBar(p, String.format(ms.getLang("support.beingsupported"), supporter.getName()));
                return true;
            } else if (support.containsKey(target)) {
                ms.showItemBar(p, String.format(ms.getLang("support.supporterbusy"), support.get(target).getName()));
                return true;
            }
            final String name = support.get(p).getName();
            ms.chatClickHoverRun(target, "§2§lMuxSupport>§7 Der Spieler §2" + name + " §7benötigt Hilfe.", "§2§lMuxSupport\n§7Supporter: §2" + p.getName() + "\n§7Spieler: §6" + name + "\n\n§2§oKlicke zum übernehmen.", "/sup " + name);
            ms.chatClickHoverRun(target, "§2§lMuxSupport>§7 Klicke §2hier§7, um den Support zu übernehmen.", "§2§lMuxSupport\n§7Supporter: §2" + p.getName() + "\n§7Spieler: §6" + name + "\n\n§2§oKlicke zum übernehmen.", "/sup " + name);
            ms.showItemBar(p, "§fDeine §aWeiterleitungsanfrage §fwurde verschickt.");
            for (byte b = 0; b < 3; b++) target.playSound(target.getLocation(), Sound.ORB_PICKUP, 0.5F, 1F);
            supmove.put(p.getUniqueId(), target.getUniqueId());
            return true;
        } else if (args[0].equalsIgnoreCase("ban")) {
            if (args.length == 1) {
                ms.showItemBar(p, ms.usage("/sup ban [" + ms.getLang("player") + " | list]"));
                return true;
            } else if (args[1].equalsIgnoreCase("list")) {
                final StringBuilder book = new StringBuilder("     §7§lMux§2§lSupport\n\n§0" + ms.getLang("support.banbook1") + "\n§0" + ms.getLang("support.banbook2") + "\n§0" + ms.getLang("support.banbook3") + "\n\n§8            -*-\n\n");
                byte count = 0;
                for (final UUID uuid : supbans.keySet()) {
                    final String s = ms.getServer().getOfflinePlayer(uuid).getName();
                    count++;
                    book.append("§0- §4").append(s).append("\n");
                }
                if (count == 0) ms.showItemBar(p, ms.getLang("support.nobanned"));
                else ms.openBook(ms.book("§aMuxSupport", book.toString()), p);
                return true;
            }
            final OfflinePlayer op = ms.getPlayer(args[1]);
            if (op == null) {
                ms.showItemBar(p, ms.hnotfound);
                return true;
            }
            final String pname = op.getName();
            if (ms.getBans().cantPunish(p, op, false)) return true;
            else if (supbans.remove(op.getUniqueId()) != null) {
                ms.showItemBar(p, String.format(ms.getLang("support.unbanned"), pname));
                return true;
            }
            supbans.put(op.getUniqueId(), p.getAddress().getAddress().getHostName());
            ms.broadcastMessage(String.format(ms.getLang("support.banned"), pname, p.getName()), "muxsystem.support", Priority.HIGH);
            return true;
        } else if (args[0].equalsIgnoreCase("stats") && p.isOp()) {
            if (args.length == 1) {
                final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lMuxSupport§0 | Top Supporter");
                inv.setItem(1, ms.renameItemStack(new ItemStack(Material.SLIME_BALL), "§2§lMuxSupport", "§7Schaue hier nach, welche Spieler",
                        "§7noch nicht supportet wurden.", "", "§7Klicke, um die §2Supports §7zu sehen."));
                Object[] stats = ms.getDB().getSupportStats();
                if (stats == null) stats = new Object[]{0, 0, new LinkedHashMap<UUID, Integer>(), 0L};
                inv.setItem(7, ms.addGlow(ms.renameItemStack(new ItemStack(Material.GOLD_INGOT), "§2§lTop Supporter§c [ADMIN]",
                        "§7Die Kategorie zeigt, wer vom Team", "§7sich sehr angestrengt hat.", "",
                        "§7Erledigte Supports: §2" + stats[0] + "§7/" + stats[1],
                        "§7Reaktionszeit: §2" + ms.getTime((int) ((Long) stats[3] / 1000L)))));
                inv.setItem(4, ms.renameItemStack(new ItemStack(Material.BOOK), "§2§lKontaktformular§c [ADMIN]", "§7Hier lassen sich Anfragen verwalten,",
                        "§7die per Webseite abgeschickt wurden.", "", "§7Klicke, um die §2Anfragen §7zu sehen."));
                final LinkedHashMap<UUID, Integer> list = (LinkedHashMap<UUID, Integer>) stats[2];
                new BukkitRunnable() {
                    short pos = 19, i = 0;

                    @Override
                    public void run() {
                        for (final Map.Entry<UUID, Integer> entry : list.entrySet()) {
                            if (pos == 26) break;
                            i++;
                            final UUID uuid = entry.getKey();
                            final OfflinePlayer op = ms.getServer().getOfflinePlayer(uuid);
                            if (op == null) continue;
                            final PermissionsGroup group = ms.getPerms().getGroupOf(op.getUniqueId());
                            final Integer[] countAndLikes = ms.getDB().getSupportLikeAndSupportCount(op.getUniqueId());
                            final double likePercentage = ((double) countAndLikes[1] / (double) countAndLikes[0] * 100);
                            final String color = i == 1 ? "§e" : i == 2 ? "§f" : i == 3 ? "§6" : "§2";
                            inv.setItem(pos, ms.renameItemStack(op.isOnline() ? ms.getHead(op.getName()) : new ItemStack(Material.SKULL_ITEM, 1, (byte) 0), color + "#§l" + i + " " + color + "§l" + op.getName(),
                                    "§7Rang: " + (group == null || group.getPrefix() == null ? "-" : ChatColor.translateAlternateColorCodes('&', group.getPrefix())),
                                    "§7Supports (diesen Monat): §2" + entry.getValue(), "§7Bewertung: §2" + df.format(likePercentage) + "%",
                                    "", color + "Klicke§7, um den Verlauf der", "§7Supports nachzuschauen."));
                            pos++;
                        }
                    }
                }.runTaskAsynchronously(ms);
                if (ms.getActiveInv(p.getName()) != InvType.SUPPORT) p.closeInventory();
                p.openInventory(inv);
                ms.setActiveInv(p.getName(), InvType.SUPPORT);
                return true;
            }
            final OfflinePlayer op = ms.getPlayer(args[1]);
            if (op == null) {
                ms.showItemBar(p, ms.hnotfound);
                return true;
            }
            final Inventory inv = ms.getServer().createInventory(null, 54, "§0§lMuxSupport§0 | " + op.getName());
            inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), "§7Zurück (Support)"));
            new BukkitRunnable() {
                @Override
                public void run() {
                    final List<Object[]> supports = ms.getDB().getSupportStats(op.getUniqueId());
                    final Integer[] countAndLikes = ms.getDB().getSupportLikeAndSupportCount(op.getUniqueId());
                    final double likePercentage = ((double) countAndLikes[1] / (double) countAndLikes[0] * 100);
                    short i = 9;
                    final short size = (short) supports.size();
                    final PermissionsGroup group = ms.getPerms().getGroupOf(op.getUniqueId());
                    inv.setItem(4, ms.renameItemStack(op.isOnline() ? ms.getHead(op.getName()) : new ItemStack(Material.SKULL_ITEM, 1, (byte) 0), "§2§l" + op.getName(),
                            "§7Rang: " + (group == null || group.getPrefix() == null ? "-" : ChatColor.translateAlternateColorCodes('&', group.getPrefix())),
                            "§7Supports (diesen Monat): §2" + supports.size(), "§7Bewertung: §2" + df.format(likePercentage) + "%"));
                    for (final Object[] s : supports) {
                        final List<String> conversation = (List<String>) s[1];
                        final String time = (String) s[0];
                        final boolean liked = (Boolean) s[2];
                        inv.setItem(i, ms.addLore(ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 5), "§2§lSupport §2#§l" + (size - i + 9) + (liked ? "§e ❤" : "") + (time != null ? "§7 (" + time + ")" : "")), conversation));
                        i++;
                        if (i >= 54) break;
                    }
                }
            }.runTaskAsynchronously(ms);
            if (ms.getActiveInv(p.getName()) != InvType.SUPPORT) p.closeInventory();
            p.openInventory(inv);
            ms.setActiveInv(p.getName(), InvType.SUPPORT);
            return true;
        }
        boolean takeover = false;
        final Player target = ms.getServer().getPlayer(args[0]);
        if (target == null || p.canSee(target) == false) {
            ms.showItemBar(p, ms.hnotonline);
            return true;
        } else if (support.containsKey(p)) {
            ms.showItemBar(p, String.format(ms.getLang("support.alreadyinsupport"), support.get(p).getName()));
            return true;
        } else if (support.containsValue(target)) {
            final Player supporter = getSupporterOf(target);
            if (supporter == null) return true;
            final UUID requested = supmove.get(supporter.getUniqueId());
            if (requested == null || requested.equals(p.getUniqueId()) == false) {
                ms.showItemBar(p, String.format(ms.getLang("support.beingsupported"), supporter.getName()));
                return true;
            }
            takeover = true;
            supporter.performCommand("sup c");
            supporter.sendMessage("§2§lMuxSupport>§7 Der Support wurde erfolgreich §aübernommen§7.");
        } else if (support.containsKey(target)) {
            ms.showItemBar(p, String.format(ms.getLang("support.supporterbusy"), support.get(target).getName()));
            return true;
        } else if (target.getName().equals(p.getName())) {
            ms.showItemBar(p, ms.getLang("support.notyourself"));
            return true;
        } else if (expectedSupporter.containsKey(target.getUniqueId())) {
            final ExpectedSupporterData data = expectedSupporter.get(target.getUniqueId());
            if (data.getSupporter().equals(p.getUniqueId()) == false) {
                p.sendMessage("§2§lMuxSupport§7> " + "§cDieser Support-Chat wurde einem anderen Supporter zugewiesen.");
                return true;
            }
            expectedSupporter.remove(target.getUniqueId());
        }
        final String name = target.getName();
        final long reaction = suplist.containsKey(name) ? System.currentTimeMillis() - suplist.remove(name) : -1;
        final SupportInfo supportInfo;
        supinfo.put(name, supportInfo = new SupportInfo(reaction));
        if (supids.containsKey(target.getName()))
            supportInfo.id = supids.remove(target.getName());
        support.put(p, target);
        if (takeover == false) ms.clearChat(target);
        p.sendMessage(String.format(ms.getLang("support.nowinsupport"), name));
        target.sendMessage(String.format(ms.getLang("support.nowinsupport"), p.getName()));
        if (takeover == false && p.isOp() == false) {
            target.sendMessage("§4§lBeachte:§7 Supporter sind normale Spieler.");
            target.sendMessage("             §7Teleportiere sie nicht in deiner Base!");
        }
        if (reaction != -1) ms.runLater(new BukkitRunnable() {
            @Override
            public void run() {
                p.chat("Hallo " + target.getName() + ", wie kann ich dir behilflich sein?");
            }
        }, 20L);
        for (byte b = 0; b < 3; b++) target.playSound(target.getLocation(), Sound.ORB_PICKUP, 0.4F, 1F);
        return true;
    }

    public boolean handleSupportLike(final Player p, final UUID target) {
        final Map.Entry<UUID, SupportInfo> likes = suplike.get(p.getName());
        if (likes == null || likes.getKey().equals(target) == false) {
            return false;
        }
        final SupportInfo supportInfo = suplike.remove(p.getName()).getValue();
        if (supportInfo.getId() != -1) {
            ms.showItemBar(p, "§fDu hast den Support §apositiv bewertet§f.");
            p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 2F);
            ms.getDB().saveSupportLike(supportInfo.getId(), true, p.getUniqueId(), target);
            return true;
        }
        return false;
    }

    public void handleQuit(final Player p) {
        final String pname = p.getName();
        if (support.containsKey(p)) {
            final Player target = support.get(p);
            target.sendMessage("§2§lMuxSupport>§7 " + ms.getLang("support.suploggedout"));
            support.remove(p);
            saveSupport(p.getUniqueId(), target.getName());
        } else if (support.containsValue(p)) {
            final Player target = getSupporterOf(p);
            if (target != null) {
                target.sendMessage(String.format("§2§lMuxSupport>§7 " + ms.getLang("support.plyloggedout"), pname));
                target.playSound(target.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                new BukkitRunnable() { // Keep the Supporter in Support-Chat to avoid accidental global messages
                    @Override
                    public void run() {
                        support.remove(target);
                        saveSupport(target.getUniqueId(), pname);
                    }
                }.runTaskLater(ms, 50L);
            }
        }
        suplist.remove(pname);
        seenfaq.remove(p.getUniqueId());
    }

    public void resetFAQ() {
        seenfaq.clear();
    }

    public void handleInventory(final Player p, final ItemStack i, final boolean rightclick) {
        final Material m = i.getType();
        if (m == Material.SLIME_BALL && ms.hasGlow(i) == false) {
            p.performCommand("sup");
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            return;
        } else if (m == Material.GOLD_INGOT && ms.hasGlow(i) == false) {
            p.performCommand("sup stats");
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            return;
        } else if (m == Material.BOOK && ms.hasGlow(i) == false) {
            p.performCommand("sup kontakt");
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            return;
        } else if (m == Material.BOOKSHELF && ms.hasGlow(i) == false) {
            openArchivedContactSupports(p);
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            return;
        } else if (m == Material.SKULL_ITEM) {
            final ItemMeta im = i.getItemMeta();
            if (im.getLore().size() == 3) {
                p.performCommand("sup " + ChatColor.stripColor(im.getDisplayName()));
                p.closeInventory();
                return;
            } else if (im.getLore().size() == 6 && im.getLore().get(5).contains("nachzuschauen")) {
                p.performCommand("sup stats " + ChatColor.stripColor(im.getDisplayName()).replace(" ", "").substring(2));
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                return;
            } else if (im.getLore().size() > 5) {
                final List<String> lore = im.getLore();
                final String plname = ChatColor.stripColor(lore.get(0).split(" ")[1]);
                final int fid = Integer.parseInt(ChatColor.stripColor(i.getItemMeta().getDisplayName().split(" §0")[1]));
                if (lore.get(lore.size() - 1).contains("schliessen")) {
                    if (rightclick) {
                        ms.getDB().setContactDone(fid, true);
                        p.performCommand("sup kontakt");
                    } else {
                        p.sendMessage(ms.header((byte) 12, "§2"));
                        p.sendMessage("  §2§lMuxSupport");
                        p.sendMessage(" ");
                        ms.chatClickHoverShow(p, "  §7Klicke §2hier§7, um den Spieler anzuschreiben.", "§2§oKlicke um anzuschreiben", "/smsg " + plname + " ");
                        ms.chatClickHoverShow(p, "  §7Klicke §2hier§7, um per Email zu antworten.", "§2§oKlicke um anzuschreiben", "/emsg " + plname + " ");
                        if (ms.isFullTrusted(p.getName())) {
                            final String email = ChatColor.stripColor(im.getLore().get(1).split(" ")[1]);
                            ms.chatClickHoverShow(p, "  §7Klicke §2hier§7, um die Email zu kopieren.", "§2§oKlicke um Email zu sehen", email);
                        }
                        p.sendMessage(ms.footer((byte) 12, "§2"));
                        p.closeInventory();
                    }
                } else {
                    ms.getDB().setContactDone(fid, false);
                    openArchivedContactSupports(p);
                }
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                return;
            }
        } else if (m == Material.ITEM_FRAME) {
            final String display = i.getItemMeta().getDisplayName();
            if (display.contains("(Support)")) {
                p.performCommand("sup stats");
            } else if (display.contains("(Team)")) {
                p.performCommand("team");
            } else {
                p.performCommand("menu");
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            return;
        }
        p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
    }

    private void openArchivedContactSupports(final Player p) {
        if (p.isOp() == false) return;
        final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lMuxSupport§0 | Archiv");
        short i = 19;
        inv.setItem(1, ms.renameItemStack(new ItemStack(Material.SLIME_BALL), "§2§lMuxSupport", "§7Schaue hier nach, welche Spieler", "§7noch nicht supportet wurden.", "", "§7Klicke, um die §2Supports §7zu sehen."));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.BOOK), "§2§lKontaktformular§c [ADMIN]", "§7Hier lassen sich Anfragen verwalten,", "§7die per Webseite abgeschickt wurden.", "", "§7Klicke, um die §2Anfragen §7zu sehen."));
        inv.setItem(5, ms.addGlow(ms.renameItemStack(new ItemStack(Material.BOOKSHELF), "§2§lArchiv")));
        inv.setItem(7, ms.renameItemStack(new ItemStack(Material.GOLD_INGOT), "§2§lTop Supporter§c [ADMIN]", "§7Die Kategorie zeigt, wer vom Team", "§7sich sehr angestrengt hat.", "", "§7Klicke, um die §2besten §7zu sehen."));
        final boolean fulltrusted = ms.isFullTrusted(p.getName());
        final List<Object[]> messages = ms.getDB().getContactMessages(true);
        if (messages.isEmpty() == false) {
            byte count = 1;
            final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            sdf.setTimeZone(TimeZone.getTimeZone("CET"));
            for (final Object[] obj : messages) {
                final int id = (Integer) obj[5];
                final String msg = (String) obj[4], time = sdf.format(new Date((Long) obj[2]));
                inv.setItem(i, ms.addLore(ms.addLore(ms.renameItemStack(supheads.get(count - 1), "§2§lKontakt §2#§l" + count + " §0" + id, "§7Spieler: §f" + obj[0], (fulltrusted ? "§7Email: §f" + obj[1] : ""),
                        "§7Zeitpunkt: §f" + time, "§7Thema: §f" + obj[3], "§7Nachricht:"), ms.getLoreFromLongText(msg, "§f")), "",  "§fKlicke§7, um die Anfrage wieder zu eröffnen§7."));
                i++;
                count++;
                if (i == 26) break;
            }
        }
        if (ms.getActiveInv(p.getName()) != InvType.SUPPORT) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.SUPPORT);
    }

    public void sendSupportSummary(final Player p) {
        final int supports = getSupportsNeeded(p.isOp());
        if (p.hasPermission("muxsystem.support") && supports > 0) {
            p.sendMessage("§2§lMuxSupport>§7 Info: " + String.format(ms.getLang("support.unanswered"), supports) + (supports > 1 ? ms.getLang("support.plural") : "") + ".");
        }
    }

    public String getSupportMSG(final boolean admin) {
        final int supports = getSupportsNeeded(admin);
        if (supports == 0) return null;
        return String.format(ms.getLang("support.unanswered"), supports).replace("§7", "§f").replace("§2", "§a") + (supports > 1 ? ms.getLang("support.plural") : "");
    }

    public int getSupportsNeeded(final boolean admin) {
        return suplist.size() + (admin ? ms.getDB().getContactMessages(false).size() : 0);
    }

    public boolean isBeingSupported(final Player p) {
        return support.containsValue(p);
    }

    public boolean isSupporting(final Player p) {
        return support.containsKey(p);
    }

    public Player getSupportedOf(final Player p) {
        return support.get(p);
    }

    public Collection<Player> getSupported() {
        return support.values();
    }

    public void addToConversation(final String supported, final String msg) {
        supinfo.get(supported).addToConversation(msg);
    }

    public Player getSupporterOf(final Player p) {
        for (final Map.Entry<Player, Player> entry : support.entrySet()) {
            if (entry.getValue() == p) return entry.getKey();
        }
        return null;
    }

    private void saveSupport(final UUID supporter, final String supported) {
        final SupportInfo si = supinfo.remove(supported);
        if (si != null) {
            int id = -1;
            if (si.getReaction() == -1) {
                id = ms.getDB().addFullSupportEntry(supported, supporter, si.getConversation());
            } else ms.getDB().addSupporterEntry(supported, supporter, si.getConversation(), si.getReaction());
            if (id != -1)
                si.id = id;
            OfflinePlayer op = ms.getPlayer(supported);
            if (op == null) return;
            final UUID supportedId = op.getUniqueId();
            final String conversation = String.join("\n", si.getConversation());
            ms.getHistory().addHistory(supportedId, supporter, "SUPPORT", conversation, null);
        }
        supmove.remove(supporter);
    }

    class SupportInfo {
        private final long reaction;
        private final List<String> conversation;
        private int id = -1;

        public SupportInfo(final long reaction) {
            this.reaction = reaction;
            this.conversation = new ArrayList<>();
        }

        public long getReaction() {
            return reaction;
        }

        public List<String> getConversation() {
            return conversation;
        }

        public int getId() {
            return id;
        }

        public void addToConversation(final String msg) {
            conversation.add(msg);
        }
    }

    class ExpectedSupporterData {
        private final long timestamp;
        private final UUID supporter;

        ExpectedSupporterData(final long timestamp, final UUID supporter) {
            this.timestamp = timestamp;
            this.supporter = supporter;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public UUID getSupporter() {
            return supporter;
        }
    }
}