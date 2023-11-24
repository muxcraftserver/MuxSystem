package me.muxteam.muxteam;

import com.google.common.collect.Iterables;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxSystem.Priority;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.muxsystem.MySQL;
import me.muxteam.ranks.MuxRanks;
import me.muxteam.ranks.MuxRanks.PermissionsGroup;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MuxBans {
    private MuxSystem ms;
    private final MySQL db;
    private final Map<String, Integer> power = new HashMap<>();
    private final ConcurrentHashMap<String, Long> recentpvpbans = new ConcurrentHashMap<>();
    private final Set<String> pvpbanips = new HashSet<>();

    public MuxBans(final MuxSystem ms) {
        this.ms = ms;
        this.db = ms.getDB();
        setup();
    }

    public void close() {
        this.ms = null;
    }

    private void setup() {
        power.put("owner", 1);
        power.put("gameadmin+", 2);
        power.put("gameadmin", 3);
        power.put("developer+", 3);
        power.put("builder+", 3);
        power.put("supporter+", 4);
        power.put("developer", 5);
        power.put("builder", 5);
        power.put("supporter", 5);
        power.put("friend", 6);
    }

    public boolean handleBanCommands(final String scmd, final CommandSender sender, final String[] args) {
        if (scmd.equalsIgnoreCase("punish")) {
            if (sender instanceof Player == false) { // NUR FÜR KONSOLE
                switch (args[1]) {
                    case "Cheat":
                        if (ms.getAdmin().ANTICHEAT.isActive() == false) {
                            return true;
                        }
                        final String r = ms.getAntiCheat().addBan();
                        applyPvPBan(sender, args[0], r);
                        return true;
                    case "Bot":
                        applyPermaMute(sender, args[0], "AUTO-DETECT: Bot");
                        return true;
                    case "Signs":
                        applyTimeBan(sender, args[0], 600, "General", "Schilder Werbung");
                        return true;
                    default:
                        return true;
                }
            }
            final Player p = (Player) sender;
            if (p.hasPermission("muxsystem.team") == false) {
                ms.sendNoCMDMessage(p);
                return true;
            } else if (args.length < 1) {
                ms.showItemBar(p, ms.usage("/strafen [spieler]"));
                return true;
            }
            final Entry<String, OfflinePlayer> entry = ms.getPlayerAndName(args[0]);
            final OfflinePlayer ply = entry.getValue();
            final String name = entry.getKey();
            if (ply == null) {
                ms.showItemBar(p, ms.hnotfound);
                return true;
            }
            final Inventory punishinv = ms.getServer().createInventory(null, 54, "§0§lMuxStrafen§0 | " + name);
            punishinv.setItem(4, ms.renameItemStack(ply.isOnline() ? ms.getHead(name) : new ItemStack(Material.SKULL_ITEM, 1, (byte) 0), "§f§l" + name));
            addHistory(punishinv, ply, p.hasPermission("muxsystem.history"));
            p.closeInventory();
            p.openInventory(punishinv);
            ms.setActiveInv(p.getName(), InvType.NOINTERACT);
            return true;
        }
        return false;
    }

    public void handleJoin(final Player p, final MuxUser u) {
        if (u.isPvPbanned()) {
            pvpbanips.add(p.getAddress().getAddress().getHostAddress());
        }
    }

    public boolean isMuted(final String pname) {
        final MuxUser u = ms.getMuxUser(pname);
        return u.isPermMute() || u.isTimeMuted();
    }

    public String getMuteTime(final String pname) {
        final MuxUser u = ms.getMuxUser(pname);
        if (u.isPermMute()) return "immer";
        else if (u.isTimeMuted()) return ms.getTime((int) ((u.getUnmuteTime() - System.currentTimeMillis()) / 1000L));
        return null;
    }

    public void removeExpired() {
        for (final Player p : ms.getServer().getOnlinePlayers()) {
            if (p.isOnline() == false) continue;
            final MuxUser u = ms.getMuxUser(p.getName());
            if (u == null) continue;
            final long mutetime = u.getUnmuteTime();
            if (mutetime != -1 && System.currentTimeMillis() >= mutetime) {
                p.sendMessage("§9§lMute>§7 Du kannst nun wieder im globalen Chat schreiben.");
                u.applyTimeMute(-1);
            }
            final long pvpbantime = u.getPvPUnbanTime();
            if (pvpbantime > 0 && System.currentTimeMillis() >= pvpbantime) {
                p.sendMessage("§6§lMuxCraft>§7 Du kannst nun wieder kämpfen.");
                u.applyPvPBan(0);
                boolean otheraccpvpbanned = false;
                final String ip = p.getAddress().getAddress().getHostAddress();
                for (final Player pl : ms.getServer().getOnlinePlayers()) {
                    if (p != pl && pl.getAddress().getAddress().getHostAddress().equals(ip) && ms.getMuxUser(pl.getName()).isPvPbanned()) {
                        otheraccpvpbanned = true;
                    }
                }
                if (otheraccpvpbanned == false) pvpbanips.remove(p.getAddress().getAddress().getHostAddress());
            }
        }
    }

    public void removeRecentPvPBanMutes() {
        recentpvpbans.forEach((key, value) -> {
            if (System.currentTimeMillis() >= value) {
                recentpvpbans.remove(key);
            }
        });
    }

    public boolean isRecentPvPBan(final String pname) {
        return recentpvpbans.containsKey(pname);
    }

    public boolean checkPvPBan(final Player p, final boolean msg) {
        final MuxUser u = ms.getMuxUser(p.getName());
        if (u != null && u.isPvPbanned()) {
            if (msg)
                ms.showItemBar(p, "§cDu bist noch für §f" + ms.getTime((int) ((u.getPvPUnbanTime() - System.currentTimeMillis()) / 1000L)) + " §cvom PvP ausgeschlossen.");
            return true;
        } else if (pvpbanips.contains(p.getAddress().getAddress().getHostAddress())) {
            final PermissionsGroup group = ms.getPerms().getGroupOf(p.getUniqueId());
            if (group != null) return false;
            if (msg) ms.showItemBar(p, "§cDu bist temporär vom PvP ausgeschlossen.");
            return true;
        }
        return false;
    }

    public boolean undoActivePunishFromHistory(final Player p, final String pname, final ItemStack item) {
        try {
            final List<String> lore = item.getItemMeta().getLore();
            if (ms.hasGlow(item) == false) return false;
            else if (lore.contains("§7Verursacht von: §f" + p.getName()) == false && p.isOp() == false) return false;
            final Material m = item.getType();
            if (m == Material.SIGN || m == Material.LAPIS_BLOCK) {
                final boolean perm = m == Material.LAPIS_BLOCK;
                if (delMute(p, pname, perm) == false) return true;
                db.removeBanEntry(Integer.parseInt(ChatColor.stripColor(Iterables.get(lore, 1))), p.getName());
            } else if (m == Material.IRON_FENCE || m == Material.LAVA_BUCKET) {
                final boolean perm = m == Material.IRON_FENCE;
                if (delBan(p, pname, perm) == false) return true;
                db.removeBanEntry(Integer.parseInt(ChatColor.stripColor(Iterables.get(lore, 1))), p.getName());
            } else if (m == Material.IRON_SWORD) {
                if (delPvPBan(p, pname) == false) return true;
                db.removeBanEntry(Integer.parseInt(ChatColor.stripColor(Iterables.get(lore, 1))), p.getName());
            } else if (m == Material.IRON_PICKAXE) {
                if (delMineBan(p, pname) == false) return true;
                db.removeBanEntry(Integer.parseInt(ChatColor.stripColor(Iterables.get(lore, 1))), p.getName());
            }
        } catch (final Exception ignored) {
        }
        return true;
    }

    /*private void addWarning(final CommandSender sender, final String name, final String reason) {
        final Entry<String, OfflinePlayer> entry = ms.getPlayerAndName(name);
        final OfflinePlayer ply = entry.getValue();
        if (cantPunish(sender, ply, false)) return;
        db.addBanEntry(ply.getUniqueId(), entry.getKey(), "Warnung", "X | " + ms.getChat().getLastChats(ply.getUniqueId()).toString(), reason, sender.getName());
        ms.broadcastMessage("§fWarn§8> §f" + entry.getKey() + " §7wurde von §f" + sender.getName() + " §7gewarnt.", "muxsystem.team", Priority.NORMAL);
        ms.broadcastMessage("§fWarn§8> §7Grund: §f" + reason, "muxsystem.team", Priority.NORMAL);
        ms.getReports().informReporters(ply.getUniqueId(), "Chat");
        if (ply.isOnline()) {
            final Player pl = ply.getPlayer();
            pl.sendMessage(" ");
            pl.sendMessage("§f§lWARNUNG> §7Du hast eine §fChat-Warnung §7erhalten. Grund: §f" + reason);
            pl.sendMessage(" ");
        }
    }*/

    public boolean applyTimeBan(final CommandSender sender, final String name, final int sec, final String typ, final String reason) {
        final Entry<String, OfflinePlayer> entry = ms.getPlayerAndName(name);
        final OfflinePlayer ply = entry.getValue();
        if (cantPunish(sender, ply, true)) return false;
        final MuxUser u = ply.isOnline() ? ms.getMuxUser(ply.getName()) : db.loadPlayer(ply.getUniqueId());
        if (u.isPermban()) {
            if (sender instanceof Player) ms.showItemBar((Player) sender, "§cDieser Spieler ist permanent gesperrt.");
            return false;
        }
        u.applyTimeban(sec);
        db.addBanEntry(ply.getUniqueId(), entry.getKey(), typ, getShorthandTime(sec), reason, sender.getName());
        if (ply.isOnline()) {
            ms.removeFromBattle(ply.getName());
            ply.getPlayer().setMetadata("systemkick", new FixedMetadataValue(ms, true));
            ply.getPlayer().kickPlayer("§cDu wurdest für §6" + ms.getTime(sec) + " §csuspendiert. §cGrund: §6" + reason);
        } else {
            ms.saveMuxUser(u);
        }
        if (reason.equals("Schilder Werbung") == false)
            ms.broadcastMessage("§cDer Spieler §7" + entry.getKey() + " §cwurde temporär suspendiert.", "muxsystem.history", Priority.NORMAL);
        return true;
    }

    public void applyPvPBan(final CommandSender sender, final String name, final String reason) {
        final OfflinePlayer ply = ms.getPlayer(name);
        if (cantPunish(sender, ply, false)) return;
        final MuxUser u = ply.isOnline() ? ms.getMuxUser(ply.getName()) : db.loadPlayer(ply.getUniqueId());
        if (u.isPvPbanned()) {
            if (sender instanceof Player)
                ms.showItemBar((Player) sender, "§cDieser Spieler ist bereits vom PvP ausgeschlossen.");
            return;
        } else if (ms.isTrusted(sender.getName()) == false && tooEarlyToBan(sender, ply.getName())) {
            return;
        }
        final int sec = u.getPvPUnbanTime() == -1 ? 86400 : 1209600;
        u.applyPvPBan(sec);
        final String violations = ms.getAntiCheat().getViolationsString(name);
        db.addBanEntry(ply.getUniqueId(), ply.getName(), "ClientMod", getShorthandTime(sec) + (violations.isEmpty() ? "" : " | " + violations), reason, sender.getName());
        ms.getReports().informReporters(ply.getUniqueId(), "Cheat");
        pvpbanips.add(u.getIp());
        if (ply.isOnline()) {
            final Player pl = ply.getPlayer();
            pl.sendMessage(ms.header((byte) 13, "§c"));
            pl.sendMessage("  §c§lPvP-Sperre");
            pl.sendMessage(" ");
            pl.sendMessage("  §7Du kannst für §c" + ms.getTime(sec) + " §7anderen Spielern");
            pl.sendMessage("  §7keinen Schaden im PvP zufügen.");
            pl.sendMessage(" ");
            ms.chatClickHoverLink(pl, "  §f§lKlicke §a§lhier§f§l, für eine weitere Chance.", "§a§oKlicke für eine weitere Chance\n\n§4§lACHTUNG:§f Die nächste Sperre wird §414 Tage §fbetragen.\n§fStarte jetzt Minecraft ohne Hackclient neu.", "https://muxcraft.eu/entbannung/?ign=" + pl.getName());
            pl.sendMessage(ms.footer((byte) 13, "§c"));
            recentpvpbans.put(pl.getName(), System.currentTimeMillis() + 180000L); // BLOCK CHAT MESSAGES FOR 3 MINUTES
        } else {
            ms.saveMuxUser(u);
        }
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            if (pl.getName().equals(ply.getName()) == false)
                pl.sendMessage("§cDer Spieler §7" + ply.getName() + "§c wurde vom PvP ausgeschlossen.");
        }
    }

    public void applyTimeMute(final CommandSender sender, final String name, final int sec, final String reason) {
        final OfflinePlayer ply = ms.getPlayer(name);
        if (cantPunish(sender, ply, false)) return;
        final MuxUser u = ply.isOnline() ? ms.getMuxUser(ply.getName()) : db.loadPlayer(ply.getUniqueId());
        if (u.isPermMute()) {
            if (sender instanceof Player) ms.showItemBar((Player) sender, "§cDieser Spieler ist permanent gestummt.");
            return;
        }
        u.applyTimeMute(sec);
        final String pname = ply.getName(), time = getShorthandTime(sec), lastchats = ms.getChat().getLastChats(ply.getUniqueId()).toString(),
                reporthistory = ms.getReports().getChatHistory(ply.getUniqueId());
        db.addBanEntry(ply.getUniqueId(), pname, "Chat", time + " | " + (reason.equals("Report") ? reporthistory : lastchats), reason, sender.getName());
        ms.broadcastMessage("§eDer Spieler §7" + pname + " §ewurde im Chat verlangsamt§7 (" + time + ").", "muxsystem.history", Priority.NORMAL);
        ms.getReports().informReporters(ply.getUniqueId(), "Chat");
        if (ply.isOnline()) {
            final Player pl = ply.getPlayer();
            pl.sendMessage(new String[]{
                    ms.header((byte) 12, "§e"),
                    "  §e§lChat Verlangsamung",
                    " ",
                    "  §7Aufgrund deiner Nachrichten wurde dein",
                    "  §7Chat für §e" + ms.getTime(sec) + " §7verlangsamt.",
            });
            pl.sendMessage(ms.footer((byte) 12, "§e"));
        } else {
            ms.saveMuxUser(u);
        }
    }

    private void applyPermaMute(final CommandSender sender, final String name, final String reason) {
        final Entry<String, OfflinePlayer> entry = ms.getPlayerAndName(name);
        final OfflinePlayer ply = entry.getValue();
        if (cantPunish(sender, ply, false)) return;
        final MuxUser u = ply.isOnline() ? ms.getMuxUser(ply.getName()) : db.loadPlayer(ply.getUniqueId());
        if (u.isPermMute()) {
            if (sender instanceof Player)
                ms.showItemBar((Player) sender, "§cDieser Spieler ist bereits permanent gestummt.");
            return;
        }
        u.setPermMute(true);
        final String lastchats = ms.getChat().getLastChats(ply.getUniqueId()).toString(), reporthistory = ms.getReports().getChatHistory(ply.getUniqueId());
        db.addBanEntry(ply.getUniqueId(), entry.getKey(), "PermMute", "X | " + (reason.equals("Report") ? reporthistory : lastchats), reason, sender.getName());
        ms.broadcastMessage("§7Der Spieler §9" + entry.getKey() + " §7wurde von " + sender.getName() + " §9gemutet§7. (Permanent)", "muxsystem.history", Priority.NORMAL);
        ms.getReports().informReporters(ply.getUniqueId(), "Chat");
        if (ply.isOnline()) {
            final Player pl = ply.getPlayer();
            pl.sendMessage(new String[]{
                    ms.header((byte) 11, "§9"),
                    "  §9§lMute",
                    " ",
                    "  §7Du kannst §9permanent §7im globalen Chat",
                    "  §7nicht mehr schreiben.",
                    " ",
                    "  §9Grund: §7" + reason,
                    "",
            });
            ms.chatClickHoverLink(pl, "  §7Klicke §9hier§7, falls das ein Fehlmute war.", "§9§oKlicke bei einen Fehlmute", "https://muxcraft.eu/entbannung/?ign=" + pl.getName());
            pl.sendMessage(ms.footer((byte) 11, "§9"));
        } else {
            ms.saveMuxUser(u);
        }
    }

    public boolean applyPermaBan(final CommandSender sender, final UUID uuid, final String reason) {
        final Player pl = ms.getServer().getPlayer(uuid);
        final String name = pl != null ? pl.getName() : ms.getDB().getPlayerName(uuid);
        if (name == null) return false;
        final OfflinePlayer ply = ms.getServer().getOfflinePlayer(uuid);
        if (cantPunish(sender, ply, false)) return false;
        final MuxUser u = ply.isOnline() ? ms.getMuxUser(ply.getName()) : db.loadPlayer(ply.getUniqueId());
        if (u.isPermban()) {
            if (sender instanceof Player)
                ms.showItemBar((Player) sender, "§cDieser Spieler ist bereits permanent gesperrt.");
            return false;
        }
        u.setPermban(true);
        db.addBanEntry(ply.getUniqueId(), name, "PermBan", "", reason, sender.getName());
        if (ply.isOnline()) {
            ply.getPlayer().setMetadata("systemkick", new FixedMetadataValue(ms, true));
            ply.getPlayer().kickPlayer("§cDu wurdest permanent von " + ms.getServerName() + " gesperrt. Grund: §7" + reason);
        } else {
            ms.saveMuxUser(u);
        }
        ms.broadcastMessage("§4Der Spieler §7" + name + "§4 wurde permanent gesperrt.", "muxsystem.history", Priority.NORMAL);
        return true;
    }

    private boolean delMute(final CommandSender sender, final String name, final boolean perm) {
        final Entry<String, OfflinePlayer> entry = ms.getPlayerAndName(name);
        final OfflinePlayer ply = entry.getValue();
        if (cantPunish(sender, ply, false)) return false;
        final MuxUser u = ply.isOnline() ? ms.getMuxUser(ply.getName()) : db.loadPlayer(ply.getUniqueId());
        if (u.isPermMute() == false && u.isTimeMuted() == false) {
            if (sender instanceof Player) ms.showItemBar((Player) sender, "§cDieser Spieler ist nicht gestummt.");
            return false;
        }
        if (perm) u.setPermMute(false);
        u.applyTimeMute(-1);
        if (ply.isOnline()) {
            ply.getPlayer().sendMessage("§6§lMuxCraft>§a Du kannst nun wieder im globalen Chat schreiben.");
        } else {
            ms.saveMuxUser(u);
        }
        sender.sendMessage("§aDer Spieler §7" + entry.getKey() + " §akann jetzt wieder schreiben.");
        return true;
    }

    private boolean delBan(final CommandSender sender, final String name, final boolean perm) {
        final Entry<String, OfflinePlayer> entry = ms.getPlayerAndName(name);
        final OfflinePlayer ply = entry.getValue();
        if (cantPunish(sender, ply, false)) return false;
        final MuxUser u = ply.isOnline() ? ms.getMuxUser(entry.getKey()) : db.loadPlayer(ply.getUniqueId());
        if (u.isPermban() == false && u.isTimebanned() == false) {
            if (sender instanceof Player) ms.showItemBar((Player) sender, "§cDieser Spieler besitzt keine Sperre.");
            return false;
        }
        if (perm) u.setPermban(false);
        u.applyTimeban(-1);
        if (ply.isOnline() == false) ms.saveMuxUser(u);
        sender.sendMessage("§aDer Spieler §7" + entry.getKey() + " §awurde erfolgreich entsperrt.");
        return true;
    }

    private boolean delMineBan(final CommandSender sender, final String name) {
        final OfflinePlayer ply = ms.getPlayer(name);
        if (ms.getBans().cantPunish(sender, ply, false)) return false;
        if (ms.getAntiBot().isMineBanned(ply.getUniqueId()) == false) {
            sender.sendMessage("§cDieser Spieler besitzt kein Mining Verbot.");
            return false;
        }
        ms.getAntiBot().removeMineBan(ply);
        if (ply.isOnline()) {
            ply.getPlayer().sendMessage("§6§lMuxCraft>§a Dein Mining Verbot wurde aufgehoben.");
        }
        sender.sendMessage("§aDer Spieler §7" + ply.getName() + " §ahat nun keine Mining Verbot mehr.");
        return true;
    }

    private boolean delPvPBan(final CommandSender sender, final String name) {
        final OfflinePlayer ply = ms.getPlayer(name);
        if (cantPunish(sender, ply, false)) return false;
        final MuxUser u = ply.isOnline() ? ms.getMuxUser(ply.getName()) : db.loadPlayer(ply.getUniqueId());
        if (u.isPvPbanned() == false) {
            sender.sendMessage("§cDieser Spieler besitzt kein PvP Ausschluss.");
            return false;
        }
        u.applyPvPBan(0);
        pvpbanips.remove(u.getIp());
        if (ply.isOnline() == false) {
            ms.saveMuxUser(u);
        } else {
            recentpvpbans.remove(ply.getName());
            ply.getPlayer().sendMessage("§6§lMuxCraft>§a Deine PvP Ausschluss wurde aufgehoben.");
        }
        sender.sendMessage("§aDer Spieler §7" + ply.getName() + " §ahat nun kein PvP Ausschluss mehr.");
        return true;
    }

    public boolean cantPunish(final CommandSender sender, final OfflinePlayer ply, final boolean checkdonate) {
        return cantPunish(sender, ply, checkdonate, 0);
    }

    public boolean cantPunish(final CommandSender sender, final OfflinePlayer ply, final boolean checkdonate, final int sec) {
        if (ply == null) {
            sender.sendMessage(ms.hnotfound);
            return true;
        } else if (ply.getName() == null) { // Never joined since Reset
            return false;
        } else if (sender.getName().equalsIgnoreCase(ply.getName())) {
            if (sender instanceof Player)
                ms.showItemBar((Player) sender, "§cDu kannst das nicht an dir selber anwenden.");
            return true;
        } else if (canKick(sender, ply) == false) {
            if (sender instanceof Player) ms.showItemBar((Player) sender, "§cDir fehlt die Berechtigung.");
            return true;
        } else
            return ms.isTrusted(sender.getName()) == false && checkdonate && sec > 43200 && tooEarlyToBan(sender, ply.getName());
    }

    private boolean canKick(final CommandSender kicker, final OfflinePlayer victim) {
        final MuxRanks perms = ms.getPerms();
        if (kicker.isOp() && victim.isOp() == false) return true;
        try {
            return power.get(perms.getUserData(ms.getServer().getPlayer(kicker.getName()).getUniqueId()).getTeamGroup().toLowerCase()) < power.get(perms.getUserData(victim.getUniqueId()).getTeamGroup().toLowerCase());
        } catch (final Exception e) {
            return true;
        }
    }

    private boolean tooEarlyToBan(final CommandSender sender, final String name) {
        final long time = ms.getPayments().getLastPurchaseTime(name);
        if (time + 2592000L * 1000L > System.currentTimeMillis()) {
            if (sender instanceof Player)
                ms.showItemBar((Player) sender, "§cGib dem Spieler ein bisschen frische Luft.");
            return true;
        }
        return false;
    }

    private String getShorthandTime(final int sec) {
        return ms.getTime(sec)
                .replace(" Monate", "mo")
                .replace(" Monat", "mo")
                .replace(" Tage", "d")
                .replace(" Tag", "d")
                .replace(" Stunden", "h")
                .replace(" Stunde", "h")
                .replace(" Minuten", "m")
                .replace(" Minute", "m")
                .replace(" Sekunden", "s")
                .replace(" Sekunde", "s");
    }

    private void addHistory(final Inventory inv, final OfflinePlayer ply, final boolean who) {
        final MuxUser u = ply.isOnline() ? ms.getMuxUser(ply.getName()) : db.loadPlayer(ply.getUniqueId());
        boolean permban = false, permmute = false, tempban = false, tempmute = false, pvpban = false;
        short i = (short) 18;
        for (final BanEntry be : db.getBanEntries(ply.getUniqueId(), 30)) {
            ItemStack item;
            final String[] date = be.getTime().split(" ");
            final String action = be.getAction();
            if (action.equalsIgnoreCase("permban")) {
                item = ms.renameItemStack(new ItemStack(Material.REDSTONE_BLOCK), "§4§lPermanenter Ban");
                if (permban == false && (permban = u.isPermban())) item = ms.addGlow(item);
            } else if (action.equalsIgnoreCase("permmute")) {
                item = ms.renameItemStack(new ItemStack(Material.LAPIS_BLOCK), "§9§lPermanenter Mute");
                if (be.getExtraInfo() != null) {
                    final String[] extrainfo = be.getExtraInfo().split(" \\| ");
                    if (extrainfo.length == 2) {
                        item = ms.addLore(item, "§eLetzte Nachrichten: §7");
                        item = ms.addLore(item, ms.getLoreFromLongText(extrainfo[1].substring(1, extrainfo[1].length() - 1), "§f"));
                        item = ms.addLore(item, "");
                    }
                }
                if (permmute == false && (permmute = u.isPermMute())) item = ms.addGlow(item);
            } else if (action.equalsIgnoreCase("kick")) {
                item = ms.renameItemStack(new ItemStack(Material.LEATHER_BOOTS), "§9§lKick");
            } else if (action.equalsIgnoreCase("clientmod")) {
                item = ms.renameItemStack(new ItemStack(Material.IRON_SWORD), "§a§lClient Mod");
                if (pvpban == false && (pvpban = u.isPvPbanned())) item = ms.addGlow(item);
                final String[] extrainfo = be.getExtraInfo().split(" \\| ");
                if (extrainfo.length == 2) {
                    item = ms.addLore(item, "§eWerte: §7");
                    item = ms.addLore(item, ms.getLoreFromLongText(extrainfo[1].substring(1, extrainfo[1].length() - 1), "§f"));
                    item = ms.addLore(item, "");
                }
            } else if (action.equalsIgnoreCase("general")) {
                item = ms.renameItemStack(new ItemStack(Material.HOPPER), "§a§lAllgemeines Vergehen");
                if (tempban == false && permban == false && (tempban = u.isTimebanned())) item = ms.addGlow(item);
                item = ms.addLore(item, "§eDauer: §f" + be.getExtraInfo(), "");
            } else if (action.equalsIgnoreCase("chat")) {
                item = ms.renameItemStack(new ItemStack(Material.BOOK_AND_QUILL), "§a§lChat Vergehen");
                if (tempmute == false && permmute == false && (tempmute = u.isTimeMuted())) item = ms.addGlow(item);
                final String[] extrainfo = be.getExtraInfo().split(" \\| ");
                item = ms.addLore(item, "§eDauer: §f" + extrainfo[0], "");
                if (extrainfo.length == 2) {
                    item = ms.addLore(item, "§eLetzte Nachrichten: §7");
                    item = ms.addLore(item, ms.getLoreFromLongText(extrainfo[1].substring(1, extrainfo[1].length() - 1), "§f"));
                    item = ms.addLore(item, "");
                }
            } else if (action.equalsIgnoreCase("warnung")) {
                item = ms.renameItemStack(new ItemStack(Material.PAPER), "§f§lWarnung");
                if (be.getExtraInfo() != null) {
                    final String[] extrainfo = be.getExtraInfo().split(" \\| ");
                    if (extrainfo.length == 2) {
                        item = ms.addLore(item, "§eLetzte Nachrichten: §7");
                        item = ms.addLore(item, ms.getLoreFromLongText(extrainfo[1].substring(1, extrainfo[1].length() - 1), "§f"));
                        item = ms.addLore(item, "");
                    }
                }
            } else {
                item = ms.renameItemStack(new ItemStack(Material.GLASS_BOTTLE), "§a§lUnbekannt");
            }
            item = ms.addLore(item, ms.getLoreFromLongText("§eGrund: §f" + be.getReason(), "§f"));
            if (who) ms.addLore(item, "", "§eTeammitglied: §f" + be.getCauser());
            item = ms.addLore(item, "", "§eZeit: §f" + date[0].replace("-", ".") + " | " + date[1].substring(0, 5), "§0" + be.getID());
            if (be.getRemoveCauser() != null) {
                if (who) item = ms.addLore(item, "§cEntfernt von: §f" + be.getRemoveCauser());
                item.setAmount(0);
            }
            inv.setItem(i, item);
            i++;
            if (i >= 45) break;
        }
    }
}