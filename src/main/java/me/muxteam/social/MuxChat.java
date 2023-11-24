package me.muxteam.social;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.muxteam.base.PlayerBase;
import me.muxteam.basic.ConfirmInventory;
import me.muxteam.basic.MuxChatInput;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxSystem.Priority;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.muxteam.MuxChatFilter;
import me.muxteam.muxteam.MuxChatFilter.BlacklistType;
import me.muxteam.muxteam.MuxSupport;
import me.muxteam.ranks.MuxRanks;
import me.muxteam.ranks.MuxRanks.PermissionsGroup;
import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.ChatHoverable;
import net.minecraft.server.v1_8_R3.ChatModifier;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.PlayerList;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class MuxChat implements Listener {
    private MuxSystem ms;
    private boolean s1, s2;
    private final MuxRanks perms;
    private final MuxSupport support;
    private final List<String> disabledchat = new ArrayList<>();
    private final Map<String, Long> msgspam = new HashMap<>();
    private final Map<String, String> lastmsg = new HashMap<>();
    private final Map<String, Long> lastslowdownchat = new HashMap<>();
    private final Map<UUID, EvictingQueue<String>> lastchats = new HashMap<>();
    private final ConcurrentHashMap<String, MuxChatInput> chatinput = new ConcurrentHashMap<>();
    private final Map<String, CopyOnWriteArrayList<String>> spamlist = new HashMap<>();
    private Map<UUID, String> prefix = new HashMap<>();
    private Map<UUID, Map<UUID, Long>> ignore = new HashMap<>();
    private final Pattern ips = Pattern.compile("((?<![0-9])(?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2}) ?[., ] ?(?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2}) ?[., ] ?(?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2}) ?[., ] ?(?:25[0-5]|2[0-4][0-9]|[0-1]?[0-9]{1,2})(?![0-9]))"),
            domains = Pattern.compile("(http://)|(https://)?(www)?\\S{2,}((\\.com)|(\\.net)|(\\.org)|(\\.eu)|(\\.co\\.uk)|(\\.tk)|(\\.to)|" +
                    "(\\.at)|(\\.ch)|(\\.me)|(\\.mc)|(\\.gg)|(\\.info)|(\\.de)|(\\.gs)|(\\.fr)|(\\.us)|(\\.ly)|(\\.gl)|(\\.ml)|(\\.be)|(\\.sc))");

    public MuxChat(final MuxSystem ms) {
        this.ms = ms;
        this.perms = ms.getPerms();
        this.support = ms.getSupport();
        final Gson gson = new Gson();
        final FileConfiguration hashYML = ms.getHashYAML();
        if (hashYML.contains("chatcolor")) {
            prefix = gson.fromJson(hashYML.getString("chatcolor"), new TypeToken<HashMap<UUID, String>>() {
            }.getType());
        }
        if (hashYML.contains("ignores")) {
            ignore = gson.fromJson(hashYML.getString("ignores"), new TypeToken<HashMap<UUID, HashMap<UUID, Long>>>() {
            }.getType());
        }
        ms.getServer().getPluginManager().registerEvents(this, ms);
    }

    public void close() {
        final FileConfiguration hashYML = ms.getHashYAML();
        final Gson gson = new Gson();
        hashYML.set("chatcolor", gson.toJson(prefix));
        hashYML.set("ignores", gson.toJson(ignore));
        this.ms = null;
    }

    public void handleQuit(final String name) {
        msgspam.remove(name);
        chatinput.remove(name);
        enableChat(name);
    }

    public void checkIgnoreList() {
        for (final Map.Entry<UUID, Map<UUID, Long>> entry : ignore.entrySet()) {
            entry.getValue().entrySet().removeIf(entry2 -> Math.abs(entry2.getValue()) <= System.currentTimeMillis());
        }
        ignore.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public boolean isIgnoring(final Player victim, final Player p) {
        return isIgnoring(victim, p, false);
    }

    public boolean isIgnoring(final Player victim, final Player p, final boolean global) {
        final Map<UUID, Long> ignorelist = ignore.get(victim.getUniqueId());
        if (ignorelist == null) return false;
        final Long encodedIgnoreData = ignorelist.get(p.getUniqueId());
        final long ignoreEndTime = Math.abs(encodedIgnoreData != null ? encodedIgnoreData : 0);
        final boolean isGlobalChatIgnored = encodedIgnoreData != null && encodedIgnoreData < 0;
        return ignoreEndTime > System.currentTimeMillis() && (global == false || isGlobalChatIgnored);
    }

    public void clearSpamList() {
        spamlist.clear();
        ms.resetLimit("OFFLINEMSG");
    }

    public void checkTime() {
        for (final Map.Entry<String, MuxChatInput> entry : chatinput.entrySet()) {
            if (entry.getValue().getTime() + 20000L < System.currentTimeMillis() && entry.getValue().isInfinite() == false) {
                final Player pl = ms.getServer().getPlayer(entry.getKey());
                pl.sendMessage("§7Da du nichts geschrieben hast, bist du wieder im Globalchat.");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        pl.sendMessage(" ");
                        enableChat(pl.getName());
                    }
                }.runTaskLater(ms, 20L);
                chatinput.remove(entry.getKey());
            }
        }
    }

    public EvictingQueue<String> getLastChats(final UUID uuid) {
        return lastchats.getOrDefault(uuid, EvictingQueue.create(5));
    }

    public void removePrefix(final UUID uuid) {
        prefix.remove(uuid);
    }

    public void disableChat(final String name) {
        disabledchat.add(name);
    }

    public void enableChat(final String name) {
        disabledchat.remove(name);
    }

    public void setChatInput(final String name, final MuxChatInput input) {
        chatinput.put(name, input);
    }


    public boolean cancelChatInput(final Player p, boolean msg) {
        final String name = p.getName();
        final MuxChatInput input = this.chatinput.get(name);
        if (input == null || input.getCancelReason() == null) return false;
        if (msg) p.sendMessage("§aDu bist wieder im Globalchat.");
        this.removeChatInput(name);
        this.enableChat(name);
        return true;
    }

    public void removeChatInput(final String name) {
        chatinput.remove(name);
    }

    public boolean hasDisabledChat(final String name) {
        return disabledchat.contains(name);
    }

    public void muteEverything() {
        s1 = s2 = true;
    }

    public void unmuteEverything() {
        s1 = s2 = false;
    }

    public boolean isMute2() {
        return s2;
    }

    public boolean isMute1() {
        return s1;
    }

    public String getPrefix(final UUID uuid) {
        return this.prefix.get(uuid);
    }

    public void showMissedMessages(final Player p, final MuxUser u, final boolean join) {
        if (u.getMails().isEmpty() == false) {
            if (join == false) {
                p.playSound(p.getLocation(), Sound.CHICKEN_EGG_POP, 1F, 1F);
                p.sendMessage(" ");
            }
            ms.chatClickHoverRun(p, "§6§lMSG>§7 Klicke §6hier§7, um deine verpassten Nachrichten §7zu lesen.", "§6§oKlicke zum Lesen", "/msg list");
            p.sendMessage(" ");
        }
    }

    public MuxUser sendOfflineMessage(final OfflinePlayer op, final String sender, final String msg, final boolean save) {
        final MuxUser u = op.isOnline() ? ms.getMuxUser(op.getName()) : ms.getDB().loadPlayer(op.getUniqueId());
        if (u == null) return null;
        u.addMail(System.currentTimeMillis() + ";" + sender + ";" + msg);
        if (save) ms.saveMuxUser(u);
        return u;
    }

    public boolean sendOfflineMessageOrChat(final OfflinePlayer op, final MuxUser u, final String sender, final String msg, final String color, final Player p) {
        final boolean afk = ms.isAFK(op.getName());
        if (afk) {
            if (p != null) ms.showItemBar(p, "§eHinweis: Dieser Spieler ist derzeit abwesend.");
        } else if (op.isOnline()) {
            final Player pl = op.getPlayer();
            pl.playSound(pl.getLocation(), Sound.CHICKEN_EGG_POP, 1F, 1F);
            pl.sendMessage("§8[" + color + "§o" + sender + " »§8]§f " + msg);
            if (p != null) p.sendMessage("§8[" + color + "§o» " + sender + "§8]§f " + msg);
            return true;
        }
        if (u == null || ms.notSafeGSON(msg)) {
            return false;
        }
        u.addMail(System.currentTimeMillis() + ";" + sender + ";" + msg);
        if (afk == false) ms.saveMuxUser(u);
        return true;
    }

    public boolean handleMSGCommand(final Player p, final String[] args, final String label) {
        if (args.length == 1 && args[0].equals("list")) {
            openMessagesList(p);
            return true;
        } else if (args.length < 2) {
            ms.showItemBar(p, ms.usage("/msg [" + ms.getLang("player") + "] [" + ms.getLang("message") + "]"));
            return true;
        }
        String msg = ms.fromArgs(args, 1);
        if (p.hasPermission("muxantispam.spam") == false && checkIntelliSpam(msg, p.getName())) {
            ms.showItemBar(p, ms.getLang("chat.nospam"));
            return true;
        }
        final OfflinePlayer op = ms.getPlayer(args[0]);
        if (op == null) {
            ms.showItemBar(p, ms.hnotfound);
            return true;
        }
        if (p.isOp() && label.equals("emsg")) {
            final String email = ms.getDB().getLastContactEmail(args[0]);
            if (email == null) {
                ms.showItemBar(p, "§cDer Spieler hat noch keine Email.");
                return true;
            }
            ms.getEmails().sendSupportEmail(p, args[0], email, msg);
            ms.showItemBar(p, "§fDie Email wurde erfolgreich §aabgeschickt§f.");
            return true;
        }
        final boolean sup = p.isOp() && label.equals("smsg");
        final Map<String, BlacklistType> ns = isNotSafe(p, msg.toLowerCase(), false), toReplace = new HashMap<>();
        ns.forEach((s, type) -> toReplace.put(s.replace("*", ""), type));
        ns.putAll(toReplace);
        if (p.hasPermission("muxsystem.msg.color")) msg = getColorMessage(p, msg);
        final String name = p.getName(), pname = op.getName();
        String amsg = msg;
        for (final String s : ns.keySet()) {
            amsg = amsg.replaceAll("(?i)" + Pattern.quote(s.toUpperCase()), "§c" + s.toUpperCase() + "§7");
            amsg = amsg.replaceAll("(?i)" + Pattern.quote(s), "§c" + s + "§7");
        }
        amsg = (ns.isEmpty() ? "" : "§8[§c✖§8] ") + "§8§o" + name + " » " + pname + ":§7 " + amsg;
        final String last = lastmsg.get(name);
        final boolean justwroteback = last != null && last.equals(pname);
        final MuxUser u = op.isOnline() ? ms.getMuxUser(op.getName()) : ms.getDB().loadPlayer(op.getUniqueId());
        if (u == null) {
            ms.showItemBar(p, ms.hnotfound);
            return true;
        }
        final boolean friends = ms.getFriends().areFriends(name, op.getUniqueId());
        boolean canSend = true;
        for (final BlacklistType type : ns.values()) {
            if (friends) {
                if (type == BlacklistType.ADFILTER) {
                    canSend = false;
                    break;
                }
            } else if ((type != BlacklistType.BADWORD || u.getSettings().hasChatFilter()) && type != BlacklistType.NAME) {
                canSend = false;
                break;
            }
        }
        final PlayerBase ownBase = ms.getBase().getFromPlayer(p), otherBase = ms.getBase().getFromUUID(op.getUniqueId());
        final boolean trusted = (ownBase != null && ownBase.isTrusted(op.getUniqueId())) || (otherBase != null && otherBase.isTrusted(p));
        if (op.isOnline() == false || p.canSee(op.getPlayer()) == false) {
            if (friends == false && trusted == false && p.hasPermission("muxsystem.team") == false) {
                ms.showItemBar(p, "§eOffline Nachrichten können nur an Freunde verschickt werden.");
                return true;
            } else if (ms.notSafeGSON(msg)) {
                ms.showItemBar(p, "§cDie Nachricht darf keine spezielle Zeichen enthalten.");
                return true;
            } else if (ms.checkLimit(p.getName(), "OFFLINEMSG", 5, true)) {
                ms.showItemBar(p, "§cWarte etwas, um weitere Offline Nachrichten zu versenden.");
                return true;
            }
            if (ns.isEmpty() || canSend) {
                sendOfflineMessage(op, sup ? "MuxSupport" : p.getName(), msg, true);
            }
            ms.showItemBar(p, "§fDie Nachricht wurde offline §aabgeschickt§f.");
        } else {
            final Player victim = op.getPlayer();
            final Map<UUID, Long> ignorelist = ignore.getOrDefault(victim.getUniqueId(), new HashMap<>());
            final Long encodedIgnoreData = ignorelist.get(p.getUniqueId());
            final long ignoreEndTime = Math.abs(encodedIgnoreData != null ? encodedIgnoreData : 0);
            if (ignoreEndTime > System.currentTimeMillis()) {
                if (victim.hasPermission("muxsystem.team")) ms.showItemBar(p, "§cDer Spieler kann derzeit keine Nachrichten erhalten.");
                else ms.showItemBar(p, ms.getLang("cmd.msgignoringyou"));
                return true;
            } else if (ms.getCreators().isRecording(victim) && friends == false && p.isOp() == false) {
                ms.showItemBar(p, "§cDer Spieler kann nur von Freunde Nachrichten erhalten.");
                return true;
            }
            final boolean afk = ms.isAFK(victim.getName());
            final String color = friends ? "§e" : "§6";
            if (canSend) {
                if (ms.getAntiBot().checkBotSpam(name, p.getUniqueId(), msg.toLowerCase())) return true;
                if (afk) {
                    if (ms.notSafeGSON(msg)) {
                        ms.showItemBar(p, "§cDie Nachricht kann keine spezielle Zeichen enthalten.");
                        return true;
                    }
                    sendOfflineMessage(victim, sup ? "MuxSupport" : name, msg, false);
                    ms.showItemBar(p, "§eHinweis: Dieser Spieler ist derzeit abwesend.");
                    if (sup) {
                        p.sendMessage("§8[§2§o» " + pname + "§8]§f " + msg);
                        return true;
                    }
                } else {
                    victim.playSound(victim.getLocation(), Sound.CHICKEN_EGG_POP, 1F, 1F);
                    if (sup) {
                        victim.sendMessage("§8[§2§oMuxSupport »§8]§f " + msg);
                        p.sendMessage("§8[§2§o» " + pname + "§8]§f " + msg);
                        return true;
                    } else
                        ms.chatClickHoverShow(victim, "§8[" + color + "§o" + name + " »§8]§f " + msg, ms.getLang("cmd.msgwriteback"), "/msg " + name + " ");
                }
            }
            ms.chatClickHoverShow(p, "§8[" + color + "§o» " + pname + "§8]§f " + msg, ms.getLang("cmd.msgwriteback"), "/msg " + pname + " ");
            if (p.hasPermission("muxsystem.msg.bypass") == false && ms.isTrusted(pname) && justwroteback == false) {
                ms.chatClickHoverRun(p, ms.getLang("cmd.msgadmins1"), ms.getLang("cmd.msgadminshover"), "/contact");
                ms.chatClickHoverRun(p, ms.getLang("cmd.msgadmins2"), ms.getLang("cmd.msgadminshover"), "/contact");
            }
            if (ns.isEmpty() == false && p.hasPermission("muxsystem.unmutable")) {
                if (canSend) ms.showItemBar(p, "§eNachricht versendet, aber enthält ein gefiltertes Wort.");
                else ms.showItemBar(p, "§cDeine Nachricht wurde gefiltert.");
            } else if (ms.getTeam().isAFK(pname) && justwroteback == false) {
                ms.showItemBar(p, "§eHinweis: Das Teammitglied ist derzeit abwesend.");
            } else if (ms.getBans().isMuted(pname)) {
                ms.showItemBar(p, "§eHinweis: Dieser Spieler ist derzeit gestummt.");
            }
            lastmsg.put(name, pname);
            lastmsg.put(pname, name);
            if (u.isTimeMuted())
                lastslowdownchat.put(pname, System.currentTimeMillis() + 30_0000L);
        }
        final boolean secureMessage = ms.isTrusted(name) || ms.isTrusted(pname) && justwroteback;
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            // Secure messages from a trusted player shouldn't be sent to other players that have the permission to read normal private messages. FullTrusted players can read every message.
            final boolean hasReadPermission = pl.hasPermission("muxsystem.spy") && secureMessage == false || ms.isFullTrusted(pl.getName());
            if (ms.getCreators().isRecording(pl)) continue;
            if (hasReadPermission && (ns.isEmpty() == false || pl.getName().equals(name) == false && pl.getName().equals(pname) == false)) {
                pl.sendMessage(amsg);
            }
        }
        return true;
    }


    public Map<String, Long> getLastSlowdownChat() {
        return lastslowdownchat;
    }

    public boolean handleReplyCommand(final Player p, final String[] args) {
        final String reply = lastmsg.get(p.getName());
        if (args.length == 0) {
            ms.showItemBar(p, ms.usage("/r [" + ms.getLang("message") + "]"));
            return true;
        } else if (reply == null) {
            ms.showItemBar(p, ms.getLang("cmd.msgnoreply"));
            return true;
        }
        return ms.getServer().dispatchCommand(p, "msg " + reply + " " + ms.fromArgs(args, 0));
    }

    public boolean handleIgnoreCommand(final Player p, final String[] args) {
        if (args.length == 0) {
            ms.showItemBar(p, ms.usage("/ignore [" + ms.getLang("player") + "]"));
            return true;
        }
        final Player pl = ms.getServer().getPlayer(args[0]);
        if (pl == null || p.canSee(pl) == false) {
            ms.showItemBar(p, ms.hnotonline);
            return true;
        } else if (pl.getName().equals(p.getName())) {
            ms.showItemBar(p, ms.getLang("cmd.ignoreyourself"));
            return true;
        }
        final UUID uuid = p.getUniqueId(), uuid2 = pl.getUniqueId();
        final Map<UUID, Long> list = ignore.getOrDefault(uuid, new HashMap<>());
        final Long encodedIgnoreData = list.get(uuid2);
        final long ignoreEndTime = Math.abs(encodedIgnoreData != null ? encodedIgnoreData : 0);
        if (ignoreEndTime > System.currentTimeMillis()) {
            list.remove(uuid2);
            ignore.put(uuid, list);
            ms.showItemBar(p, String.format(ms.getLang("cmd.ignorereceive"), pl.getName()));
            return true;
        } else if (pl.hasPermission("muxsystem.team")) { // Maybe from OP, Supporters can be annoying
            ms.showItemBar(p, ms.getLang("cmd.ignorenostaff"));
            return true;
        }
        new ConfirmInventory(ms, player -> {
            list.put(uuid2, -(System.currentTimeMillis() + 172800000L)); // Store as negative value to signify global chat ignore
            ignore.put(uuid, list);
            ms.showItemBar(p, String.format(ms.getLang("cmd.ignoreglobal"), pl.getName()));
            p.closeInventory();
        }, player -> {
            list.put(uuid2, System.currentTimeMillis() + 172800000L); // Store as positive value for only MSGs ignore
            ignore.put(uuid, list);
            ms.showItemBar(p, String.format(ms.getLang("cmd.ignoreblock"), pl.getName()));
            p.closeInventory();
        }).setAbortColor((byte) 9).show(p, "§0§l" + pl.getName() + " ignorieren", "§aChatnachrichten & MSGs ignorieren", "§7Nur MSGs ignorieren");
        return true;
    }

    public boolean handleSilentCommand(final Player p, final String[] args) {
        if (p.hasPermission("muxsystem.silent") == false) {
            ms.sendNoCMDMessage(p);
            return true;
        } else if (args.length == 0) {
            ms.showItemBar(p, ms.usage("/silent [1 | 2 | off | info]"));
            return true;
        } else if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("i")) {
            ms.showItemBar(p, "§fDerzeit ist GlobalMute " + (s1 ? "§6I" + (s2 ? " §fund §6II" : "") : s2 ? "§6II" : "§c§lnicht§c") + " §faktiviert.");
            return true;
        } else if (ms.getServer().getScheduler().isQueued(clearmutetask)) {
            ms.showItemBar(p, ms.getLang("cantrightnow"));
            return true;
        } else if (ms.checkGeneralCooldown("CHAT", "GLOBALMUTE", 3000L, true)) {
            ms.showItemBar(p, ms.getLang("notsofast"));
            return true;
        } else try {
            switch (Byte.parseByte(args[0])) {
                case 1:
                    ms.broadcastMessage(String.format(ms.getLang("chat.s1"), (s1 ^= true) ? ms.getLang("chat.s1off") : ms.getLang("chat.s1on")), null, Priority.NORMAL);
                    break;
                case 2:
                    ms.broadcastMessage(String.format(ms.getLang("chat.s2"), (s2 ^= true) ? "§7temporär §cde" : "§7wieder §a"), "muxsystem.silent", Priority.HIGH);
                    break;
                default:
                    throw new NumberFormatException();
            }
        } catch (final NumberFormatException e) {
            final boolean alreadyoff = s1 == false && s2 == false;
            s1 = s2 = false;
            if (alreadyoff) {
                ms.showItemBar(p, "§cDer Chat ist bereits aktiviert.");
                return true;
            }
            ms.broadcastMessage(ms.getLang("chat.activated"), null, Priority.HIGH);
        }
        return true;
    }

    private int clearmutetask;

    public boolean handleChatClearCommand(final Player p) {
        if (p.hasPermission("muxsystem.clearchat") == false) {
            ms.sendNoCMDMessage(p);
            return true;
        } else if (ms.getServer().getScheduler().isQueued(clearmutetask) || ms.checkLimit(p.getName(), "CHATCLEAR", 3, true)) {
            ms.showItemBar(p, ms.getLang("cantrightnow"));
            return true;
        }
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            if (pl.hasPermission("muxsystem.clearchat") == false && pl.getName().equals(p.getName()) == false) {
                ms.clearChat(pl);
            }
        }
        ms.broadcastMessage(ms.getLang("chatcleared"), null, Priority.HIGH);
        if (ms.checkLimit(p.getName(), "CHATCLEAR", 3, false)) {
            p.performCommand("silent 1");
        }
        if (s1 == false) {
            clearmutetask = new BukkitRunnable() {
                @Override
                public void run() {
                    s1 = false;
                }
            }.runTaskLater(ms, 100L).getTaskId();
        }
        s1 = true;
        return true;
    }

    public boolean setChatColor(final Player p, final String s) {
        if (s != null && s.isEmpty() == false) {
            final String color = getColorMessage(p, s);
            if (color.contains("&")) {
                ms.showItemBar(p, "§cDein Chatpräfix enthält Farben, die nicht erlaubt sind.");
                ms.chatClickHoverRun(p, "§6§lMuxCraft>§7 Klicke §6hier§7, um deine Farben zu sehen.", "§6§oKlicke um deine farben zu sehen", "/colors");
                return false;
            } else if (ms.getChat().isNotSafe(p, s.toLowerCase(), false).isEmpty() == false) {
                ms.showItemBar(p, "§cDein Chatpräfix enthält Wörter, die nicht erlaubt sind.");
                return false;
            }
            prefix.put(p.getUniqueId(), color);
            ms.showItemBar(p, "§fDein Chatpräfix ist nun: §6§l" + s);
        } else {
            prefix.remove(p.getUniqueId());
            ms.showItemBar(p, "§fDein Chatpräfix wurde §centfernt§f.");
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSign(final SignChangeEvent e) {
        if (e.getPlayer().hasPermission("muxsystem.advertising")) {
            for (byte i = 0; i < e.getLines().length; i++) {
                e.setLine(i, getColorMessage(e.getPlayer(), e.getLine(i)));
            }
        } else for (final String s : e.getLines()) {
            if (s.contains("§")) {
                e.setCancelled(true);
                return;
            }
            if (isAdvertising(e.getPlayer(), s) != null) {
                e.setCancelled(true);
                ms.getServer().dispatchCommand(ms.getServer().getConsoleSender(), "punish " + e.getPlayer().getName() + " Signs");
                e.getBlock().setType(Material.AIR);
            }
        }
    }

    @EventHandler
    public void onChat(final AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();
        String msg = e.getMessage();
        final String pname = p.getName();
        ms.updateAFK(p);
        final MuxChatInput input = chatinput.get(pname);
        if (input != null) {
            input.tryInput();
            final boolean cancel = input.getTries() <= input.getCurrentTries();
            if (cancel) chatinput.remove(pname);
            e.setCancelled(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    input.getAction().call(e.getMessage(), p);
                    if (input.getCurrentTries() == 1 && input.getCancelReason() != null && input.isCancelled() == false && input.isHandleCancelLocal() == false) { // Should
                        ms.chatClickHoverRun(p, input.getCancelReason(), input.getCancelReason(), "/cancelmuxchatinput");
                    }
                    if (input.isCancelled() || cancel) {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                p.sendMessage(" ");
                                if (input.getTries() > 0 && cancel) {
                                    p.sendMessage("§a§lDu bist nun wieder im Globalchat.");
                                }
                                enableChat(pname);
                            }
                        }.runTaskLater(ms, 20L);
                    }
                }
            }.runTask(ms);
            return;
        }
        if (ms.getAffiliate().handleChatInput(p, msg)) {
            e.setCancelled(true);
            return;
        }
        if (msg.matches("^7[a-zA-Z].*") || msg.startsWith("t/")) {
            e.setCancelled(true);
            ms.showItemBar(p, String.format(ms.getLang("chat.slashnoob"), msg.startsWith("7") ? ms.getLang("chat.slash7") : ms.getLang("chat.slashT")));
            return;
        } else if (p.hasPermission("muxsystem.teamchat") && (msg.matches("^1[a-zA-Z].*") || msg.contains("Connected with MineChat"))) {
            e.setCancelled(true);
            ms.showItemBar(p, "§eDu solltest ein ! statt einer 1 benutzen!");
            return;
        } else if (p.hasPermission("muxantispam.nocooldown") == false) {
            if (msgspam.get(pname) != null && System.currentTimeMillis() - msgspam.get(pname) < (p.hasPermission("muxsystem.gold") ? 600 : 1200)) {
                e.setCancelled(true);
                ms.showItemBar(p, ms.getLang("chat.dontspam"));
                msgspam.put(pname, System.currentTimeMillis());
                return;
            }
            msgspam.put(pname, System.currentTimeMillis());
        }
        if (support.isBeingSupported(p)) {
            final String sup = "§6" + pname + ": §7" + msg, supbold = "§6§l" + pname + ": §7" + msg;
            p.sendMessage(sup);
            final Player target = support.getSupporterOf(p);
            ms.getServer().getOnlinePlayers().forEach(pl -> {
                if (target == pl) pl.sendMessage(supbold);
                else if (pl.hasPermission("muxsystem.support") &&(ms.getCreators().isRecording(pl) == false || hasDisabledChat(pl.getName())))
                    pl.sendMessage(sup);
            });
            if (target != null) target.playSound(target.getLocation(), Sound.CHICKEN_EGG_POP, 1F, 1F);
            support.addToConversation(pname, sup);
            e.setCancelled(true);
            return;
        }
        final String colormsg = ChatColor.translateAlternateColorCodes('&', msg);
        if (handleTeamChat(p, colormsg)) {
            e.setCancelled(true);
            return;
        } else if (support.isSupporting(p)) {
            if (msg.startsWith("%")) {
                msg = msg.replaceFirst("%", "");
                e.setMessage(msg);
            } else {
                final String sup = "§6" + pname + ": §c" + colormsg, supbold = "§6§l" + pname + ": §c" + colormsg;
                final Player target = support.getSupportedOf(p);
                e.setCancelled(true);
                if (target == null || target.isOnline() == false) {
                    ms.showItemBar(p, ms.hnotonline);
                    return;
                }
                ms.getServer().getOnlinePlayers().forEach(pl -> {
                    if (p == pl) pl.sendMessage(supbold);
                    else if (pl.hasPermission("muxsystem.support") && (ms.getCreators().isRecording(pl) == false || hasDisabledChat(pl.getName())))
                        pl.sendMessage(sup);
                });
                target.sendMessage(sup);
                target.playSound(target.getLocation(), Sound.CHICKEN_EGG_POP, 1F, 1F);
                support.addToConversation(target.getName(), sup);
                return;
            }
        }
        if (p.hasPermission("muxantispam.spam") == false) {
            final boolean cooldown = ms.checkLimit(p.getName(), "CHATSPAM", p.hasPermission("muxsystem.gold") ? 5 : 3, false);
            if (cooldown || ms.checkLimit(p.getName(), "CHATSPAM", p.hasPermission("muxsystem.gold") ? 5 : 3, true)) {
                ms.showItemBar(p, ms.getLang("chat.dontspam"));
                e.setCancelled(true);
                return;
            } else if (p.hasPermission("muxsystem.gold") == false && checkIntelliSpam(colormsg, pname)) {
                e.setCancelled(true);
                ms.showItemBar(p, ms.getLang("chat.nospam"));
                return;
            } else if (p.hasPermission("muxsystem.gold") == false && colormsg.length() == 1 && ms.isNumeric(colormsg) == false) {
                e.setCancelled(true);
                ms.showItemBar(p, "§cSchreibe etwas mehr als nur ein Buchstabe.");
                return;
            }
        }
        final UUID uuid = p.getUniqueId();
        final String prfx = prefix.get(uuid);
        if (prfx != null && msg.startsWith("#") == false) {
            e.setMessage(prfx + msg);
        }
        msg = e.getMessage();
        String lowercase = msg.toLowerCase();
        final Map<String, MuxChatFilter.BlacklistType> ns = isNotSafe(p, lowercase, true);
        if (ns.isEmpty() == false || ms.getBans().isRecentPvPBan(pname)) {
            final List<String> toReplace = new ArrayList<>(ns.keySet());
            String amsg = e.getMessage();
            for (final String word : toReplace) {
                if (word != null) {
                    amsg = amsg.replaceAll("(?i)" + Pattern.quote(word.toUpperCase()), "§c" + word.toUpperCase() + "§f");
                    amsg = amsg.replaceAll("(?i)" + Pattern.quote(word), "§c" + word + "§f");
                }
            }
            amsg = "§8[§c✖§8] " + (ns.containsValue(BlacklistType.NAME) ? "§c" : "§7") + p.getName() + "§8: §f" + amsg;
            String finalAmsg = amsg;
            final List<Player> toAdd = new ArrayList<>();
            e.getRecipients().forEach(pl -> {
                if (pl.isOp() && ms.getCreators().isRecording(pl) == false && colormsg.startsWith("#") == false) {
                    pl.sendMessage(finalAmsg);
                    return;
                }
                final MuxUser u = ms.getMuxUser(pl.getName());
                if (ns.size() == 1 && ns.containsValue(BlacklistType.BADWORD) && u != null && u.getSettings().hasChatFilter() == false) {
                    toAdd.add(pl);
                }
            });
            e.getRecipients().clear();
            e.getRecipients().add(p);
            toAdd.forEach(pl -> e.getRecipients().add(pl));
            if (p.hasPermission("muxsystem.unmutable")) {
                ms.showItemBar(p, "§cDeine Nachricht wurde gefiltert.");
            }
        } else {
            final EvictingQueue<String> list = lastchats.getOrDefault(uuid, EvictingQueue.create(5));
            list.add(colormsg);
            lastchats.put(uuid, list);
        }
        PermissionsGroup group = perms.getGroup("Default");
        if (perms.hasGroup(uuid)) {
            group = perms.getGroupOf(uuid);
        }
        final boolean verified = ms.getAntiCheat().isVerified(p);
        final String colorprefix = group == null ? "" : ms.getCustomRank().hasTagData(uuid) ? ms.getCustomRank().getTagData(uuid).toString() : ChatColor.translateAlternateColorCodes('&', group.getPrefix());
        e.setFormat(colorprefix + "§7%1$s" + (verified ? "§a✔" : "") + "§8: §f%2$s");
        msg = getColorMessage(p, msg);
        e.setMessage(msg);
        if (ms.getClans().handleClanChat(p, msg)) {
            e.setCancelled(true);
            return;
        }
        final boolean disablechat = hasDisabledChat(pname);
        if (p.hasPermission("muxsystem.unmutable") == false || disablechat) {
            final String muted = ms.getBans().getMuteTime(pname);
            final boolean tempMute = muted != null && muted.equals("immer") == false;
            boolean canChat = true;
            if (s1 || muted != null || disablechat) {
                if (muted != null) {
                    boolean tempCanChat = false;
                    if (tempMute) {
                        final Long time = lastslowdownchat.get(p.getName());
                        if (time != null) {
                            final long left = time - System.currentTimeMillis();
                            if (left > 0) {
                                ms.showItemBar(p, "§cDu kannst in §6" + ms.getTime((int) (left / 1000L)) + " §cwieder eine Nachricht schreiben.");
                                canChat = false;
                            } else tempCanChat = true;
                        } else tempCanChat = true;
                        if (canChat)
                            lastslowdownchat.put(p.getName(), System.currentTimeMillis() + 30_0000L);
                    }
                    if (tempCanChat == false && muted.equals("immer")) {
                        canChat = false;
                        p.sendMessage(String.format(ms.getLang("chat.muted"), muted));
                        if (ms.checkGeneralCooldown(p.getName(), "UNMUTEMSG", 30000L, true) == false)
                            ms.chatClickHoverLink(p, ms.getLang("chat.getunmute"), ms.getLang("chat.getunmutelore"), "https://muxcraft.eu/entbannung/?ign=" + p.getName());
                    }
                } else {
                    if (disablechat) {
                        ms.showItemBar(p, "§cDein Chat ist deaktiviert.");
                    } else ms.showItemBar(p, "§c" + (ms.getLang("chat.chatoff")));
                    if (s1) p.sendMessage(ms.getLang("chat.tipmsgs"));
                    canChat = false;
                }
                if (canChat == false) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
        if (ms.getAntiBot().checkBotSpam(pname, uuid, lowercase)) {
            e.setCancelled(true);
            return;
        }
        if (msg.matches(".*?[A-Z]{4}.*?") && (p.hasPermission("muxantispam.caps") == false && p.hasPermission("muxsystem.gold") == false)) {
            ms.showItemBar(p, ms.getLang("chat.nocaps"));
            msg = msg.toLowerCase();
        }
        msg = checkCharachterSpam(msg);
        e.setMessage(msg);
        final long onlinetime = ms.getMuxUser(pname).getOnlineTime();
        if (msg.contains("?") && onlinetime < 64800000L) {
            ms.chatClickHoverRun(p, ms.getLang("chat.clickforquestion"), ms.getLang("chat.hoverforquestion"), "/support");
        }
        final String clantext = ms.getDB().getChatText(p.getUniqueId());
        p.setDisplayName((clantext.isEmpty() == false ? clantext + "§8*§7" : "") + pname);
        e.getRecipients().removeAll(support.getSupported());
        final Set<Player> noreceive = new HashSet<>();
        for (final Player pl : e.getRecipients()) {
            if (isIgnoring(pl, p, true)) {
                noreceive.add(pl);
            } else if (msg.contains(pl.getName())) {
                pl.sendMessage(String.format(e.getFormat(), p.getDisplayName(), e.getMessage().replace(pl.getName(), "§f§l" + pl.getName() + "§r" + ChatColor.getLastColors(msg))));
                if (p.canSee(pl) && (ms.isAFK(pl.getName()) || ms.getTeam().isAFK(pl.getName())))
                    ms.showItemBar(p, "§eHinweis: Der Spieler ist derzeit abwesend.");
                noreceive.add(pl);
            }
        }
        e.getRecipients().removeAll(noreceive);
        e.getRecipients().removeIf(pl -> hasDisabledChat(pl.getName()));
        if (e.getMessage().contains("[item]") && p.hasPermission("muxsystem.chatitem")) {
            final ItemStack is = p.getItemInHand();
            if (is == null || is.getType() == Material.AIR || ms.isMenuItem(is)) {
                ms.showItemBar(p, "§cDu hältst kein Item in der Hand.");
                e.setCancelled(true);
                return;
            }
            final net.minecraft.server.v1_8_R3.ItemStack b = CraftItemStack.asNMSCopy(is);
            final NBTTagCompound c = new NBTTagCompound();
            b.save(c);
            if (isNotSafe(p, b.getName().toLowerCase(), false).isEmpty() == false) {
                ms.showItemBar(p, "§cDas Item kann nicht gezeigt werden.");
                return;
            }
            String item;
            if (b.count < 2) {
                item = "§f» " + b.getName() + " §f«";
            } else {
                item = "§f» §7" + b.count + "x §f" + b.getName() + " §f«";
            }
            final ChatComponentText d = new ChatComponentText(String.format(e.getFormat(), p.getDisplayName(), e.getMessage()).replaceFirst(Pattern.quote("[item]"), item));
            final ChatModifier g = d.getChatModifier();
            g.setChatHoverable(new ChatHoverable(ChatHoverable.EnumHoverAction.SHOW_ITEM, new ChatComponentText(c.toString())));
            d.setChatModifier(g);
            final PlayerList h = MinecraftServer.getServer().getPlayerList();
            for (final Player i : e.getRecipients()) {
                h.getPlayer(i.getName()).sendMessage(d);
            }
            e.setCancelled(true);
        }
    }

    public String getColorMessage(final Player p, String msg) {
        if (p.hasPermission("muxchat.color")) {
            msg = msg.replaceAll("&([236789abef])", "§$1");
        }
        if (p.hasPermission("muxchat.magic")) {
            msg = msg.replace("&k", "§k");
        }
        if (p.hasPermission("muxchat.morecolors")) {
            msg = msg.replaceAll("&([015])", "§$1");
        }
        if (p.hasPermission("muxsystem.creator")) {
            msg = msg.replaceAll("&(d)", "§$1");
        }
        if (p.isOp()) {
            msg = msg.replaceAll("&([cdmno])", "§$1");
        }
        if (ms.isTrusted(p.getName())) {
            msg = msg.replaceAll("&([4l])", "§$1");
        }
        return msg;
    }

    private void openMessagesList(final Player p) {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lVerpasste Nachrichten");
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.BOOK), "§6§lVerpasste Nachrichten", "§7Hier siehst du die Nachrichten,", "§7die du letztens verpasst hast."));
        short pos = 18;
        final MuxUser u = ms.getMuxUser(p.getName());
        final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("CET"));
        final List<String> clanactivites = new ArrayList<>();
        for (final String s : u.getMails()) {
            if (pos == 45) break;
            final String[] parts = s.split(";");
            final long time = Long.parseLong(parts[0]);
            final String name = parts[1];
            if (name.equals("MuxClans")) {
                clanactivites.add(parts[2]);
                continue;
            }
            String color = name.equals("MuxBase") ? "§b" : name.equals("MuxSupport") ? "§2" : name.equals("MuxShop") ? "§e" : name.equals("MuxFreunde") ? "§e" : "§f";
            final Material type = name.equals("MuxReport") ? Material.EMPTY_MAP : name.equals("MuxShop") ? Material.DOUBLE_PLANT :
                    name.equals("MuxFreunde") ? Material.CAKE : name.equals("MuxSupport") ? Material.SLIME_BALL : name.equals("MuxBase") ? Material.SAPLING : Material.PAPER;
            if (type == Material.PAPER) {
                final OfflinePlayer op = ms.getPlayer(name);
                if (op != null) {
                    final PermissionsGroup group = perms.getGroupOf(op.getUniqueId());
                    if (group != null && group.getPermissions().contains("muxteam"))
                        color = "§4§lMuxTeam§8 | " + group.getColor().replace("&", "§");
                }
            }
            final List<String> text = ms.getLoreFromLongText(parts[2], "§f");
            final ItemStack item = ms.addLore(ms.renameItemStack(new ItemStack(type), color + "§l" + name,
                    color.equals("§f") == false ? new String[0] : new String[]{"§7Zeitpunkt: §f" + sdf.format(new Date(time)), ""}), text);
            inv.setItem(pos, item);
            pos++;
        }
        if (pos < 45 && clanactivites.isEmpty() == false) {
            inv.setItem(pos, ms.renameItemStack(new ItemStack(Material.CHAINMAIL_HELMET), "§3§lMuxClans", Lists.reverse(clanactivites.stream().limit(10).collect(Collectors.toList()))));
        }
        u.seenMails();
        if (ms.getActiveInv(p.getName()) != InvType.MESSAGES) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.MESSAGES);
    }

    public boolean handleTeamChat(final Player p, final String msg) {
        if (msg.startsWith("!") == false || p.hasPermission("muxsystem.teamchat") == false) {
            return false;
        }
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            if (pl.hasPermission("muxsystem.teamchat") == false || (pl != p && ms.getCreators().isRecording(pl))) continue;
            pl.sendMessage("§cTC§8> §a" + p.getName() + "§a: §c§o" + ChatColor.translateAlternateColorCodes('&', msg.substring(1).trim()));
        }
        return true;
    }

    private boolean checkIntelliSpam(final String msg, final String s) {
        short aehn = 0;
        if (spamlist.get(s) == null) {
            final CopyOnWriteArrayList<String> al = new CopyOnWriteArrayList<>();
            al.add(msg);
            spamlist.put(s, al);
        } else {
            for (final String spm : spamlist.get(s)) {
                if (isSimilar(spm, msg)) aehn++;
            }
            spamlist.get(s).add(msg);
            if (spamlist.get(s).size() > 6) {
                spamlist.get(s).remove(0);
            }
        }
        return aehn > 3;
    }

    private String checkCharachterSpam(String msg) {
        int count = 0;
        if (msg.isEmpty()) return msg;
        char lastchar = msg.toCharArray()[0];
        String full = "";
        for (final char c : msg.toCharArray()) {
            if (c == lastchar) {
                count++;
                full += c;
            } else {
                count = 0;
                lastchar = c;
                full = "";
            }
            if (count > 8) {
                msg = msg.replace(full, "" + c + c + c + c + c);
            }
        }
        return msg;
    }

    private String checkRepeatedSpam(final String msg) {
        final String[] sorted = msg.split(" ");
        Arrays.sort(sorted);
        final List<String> duplicates = new ArrayList<>();
        for (int i = 0; i < sorted.length - 1; i++) {
            if (sorted[i].length() > 3) { // Only take longer words
                duplicates.add(sorted[i]);
                if (Collections.frequency(duplicates, sorted[i]) > 3) return sorted[i];
            }
        }
        return null;
    }

    private String checkUnallowedCharachter(final String msg) {
        for (final char c : msg.toCharArray()) {
            final int n = c;
            if ((n > 580 && n < 650) || (n > 880 && n < 1000) || (n > 7400 && n < 8500 && n != 8364) || (n > 65290 && n < 65372) || (n > 9312 && n < 10135))
                return String.valueOf(c);
        }
        return null;
    }

    public boolean isSimilar(String spm, String msg) {
        spm = spm.toLowerCase();
        msg = msg.toLowerCase();
        if (spm.equals(msg)) return true;
        for (final String wort : spm.split(" ")) {
            if (wort.length() > 4 && msg.contains(wort)) return true;
        }
        return spm.length() > 6 && (msg.contains(spm.substring(0, 6)) || msg.contains(spm.substring(spm.length() - 6)) || msg.contains(spm.substring(spm.length() / 3, spm.length() / 3 * 2)));
    }

    private String isSpam(final String s) {
        return checkRepeatedSpam(s);
    }

    public String isAdvertising(final Player p, final String s) {
        final String text = s.toLowerCase(), text2 = text.replaceAll("[() \\[\\]]", ""); // Replace []() used to indicate dots
        if (p.hasPermission("muxsystem.advertising") == false && (ips.matcher(text).find() || domains.matcher(text2).find() && text2.contains("muxcraft") == false)) {
            return text;
        }
        return null;
    }

    public Map<String, BlacklistType> isNotSafe(final Player p, final String lowercase, final boolean checkspam) {
        final String msg = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', lowercase));
        final Map<String, BlacklistType> notsafe = ms.getChatFilter().checkBlacklists(msg, p.getName().toLowerCase());
        final String spam = checkspam ? isSpam(msg) : null, ads = isAdvertising(p, msg), unallowedchar = checkUnallowedCharachter(msg);
        if (p.hasPermission("muxantispam.blacklist") == false) {
            if (unallowedchar != null)
                notsafe.put(unallowedchar, null);
            if (spam != null)
                notsafe.put(spam, null);
            if (ads != null)
                notsafe.put(ads, BlacklistType.ADFILTER);
            return notsafe;
        }
        return new HashMap<>();
    }
}