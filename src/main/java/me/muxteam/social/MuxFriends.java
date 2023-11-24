package me.muxteam.social;

import me.muxteam.basic.MuxAnvil;
import me.muxteam.basic.Pageifier;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class MuxFriends {
    private MuxSystem ms;
    private final Map<String, Set<UUID>> friends = new HashMap<>();
    private final Map<String, HashSet<String>> requests = new HashMap<>();

    public MuxFriends(final MuxSystem ms) {
        this.ms = ms;
    }

    public void close() {
        this.ms = null;
    }

    public void setFriends(final String pname, final Set<UUID> flist) {
        friends.put(pname, flist);
    }

    public Set<UUID> getFriends(final String pname) {
        return friends.get(pname);
    }

    public boolean areFriends(final String pname, final UUID uuid2) {
        return friends.get(pname) != null && friends.get(pname).contains(uuid2);
    }

    public void handleQuit(final String pname) {
        requests.remove(pname);
        friends.remove(pname);
    }

    public void handleJoin(final Player p) {
        sendFriendAlert(p, null);
        if (p.hasPlayedBefore()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    setFriends(p.getName(), ms.getDB().getFriends(p.getUniqueId()));
                }
            }.runTaskAsynchronously(ms);
        } else {
            setFriends(p.getName(), new HashSet<>());
        }
    }

    public void sendFriendAlert(final Player p, final Set<String> exclude) {
        final long lastseen = p.getLastPlayed();
        if (lastseen + 180000L > System.currentTimeMillis()) return;
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            final MuxUser u = ms.getMuxUser(pl.getName());
            if (exclude != null && exclude.contains(pl.getName())) continue;
            if (u != null && u.getSettings().getAlertFriends().contains(p.getUniqueId())) {
                if (pl.canSee(p) == false) continue;
                ms.sendNormalTitle(pl, "§fDein Freund §e" + p.getName() + " §fist nun online.", 10, 60, 10);
            }
        }
    }

    public boolean handleCommand(final Player p, final String[] args) {
        int page = 0;
        if (args.length > 0) {
            page = ms.isNumeric(args[0]) ? Integer.parseInt(args[0]) : -2;
            if (page == -2 || page > 89) {
                addAsFriend(args[0], p);
                return true;
            }
        }
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxFreunde§0 | /freunde");
        final Set<UUID> flist = friends.getOrDefault(p.getName(), new HashSet<>());
        final Pageifier<ItemStack> pages = new Pageifier<>(27);
        short on = 0, max = 0;
        flist.add(p.getUniqueId());
        final Server srv = ms.getServer();
        final Map<UUID, Long> values = new HashMap<>(), lastonline = new HashMap<>();
        final Map<UUID, MuxUser> muxusers = new HashMap<>();
        MuxUser u = ms.getMuxUser(p.getName());
        final boolean showtrophies = u.getSettings().showFriendsByTrophies();
        for (final UUID uuid : flist) {
            if (uuid == null) continue;
            final OfflinePlayer d = srv.getOfflinePlayer(uuid);
            final MuxUser us;
            if (d.isOnline()) us = ms.getMuxUser(d.getName());
            else us = ms.getDB().loadPlayer(uuid);
            if (us == null) continue;
            muxusers.put(uuid, us);
            lastonline.put(uuid, uuid.equals(p.getUniqueId()) ? System.currentTimeMillis() + 1000L : us.getLoginTimestamp()); // Always show player as first
            values.put(uuid, showtrophies ? (long) us.getTrophies() : ms.getBase().hasBase(uuid) ? (long) ms.getBase().getFromUUID(uuid).getValue() : -1L);
        }
        final Set<UUID> alertfriends = u.getSettings().getAlertFriends();
        final Map<UUID, Long> tosort = new HashMap<>(lastonline);
        for (final Map.Entry<UUID, Long> entry : tosort.entrySet().stream(). // SORT BY LAST LOGIN
                sorted(Collections.reverseOrder(java.util.Comparator.comparingLong(Map.Entry::getValue))).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new)).entrySet()) {
            final UUID uuid = entry.getKey();
            final OfflinePlayer d = srv.getOfflinePlayer(uuid);
            if (d.getName() == null) continue;
            final boolean online = d.isOnline() && p.canSee(d.getPlayer()), you = d.getName().equals(p.getName());
            final String clan = ms.getDB().getClanFromPlayer(uuid);
            final boolean alert = alertfriends.contains(uuid);
            final long value = values.get(uuid);
            pages.addItem(ms.renameItemStack(online ? ms.getHead(d.getName()) : new ItemStack(Material.SKULL_ITEM, 1, (byte) 0), you ? "§a§l" + d.getName() : ("§f§l" + d.getName() + (alert ? "§e ✯" : "")),
                    "§7Status: " + ms.getPlayerStatus(p, d, muxusers.get(uuid), true),
                    showtrophies ? "§7Trophäen: §b" + value : "§7Basewert: §b" + (value == -1L ? "-" : ms.getNumberFormat(value)),
                    "§7Clan: §3" + (clan == null ? "-" : clan), "", "§7Klicke, um " + (you ? "dein" : "sein") + " Profil", "§7zu sehen."));
            if (you == false) {
                max++;
                if (online) on++;
            }
        }
        pages.addItem(ms.renameItemStack(ms.getHeadFromURL("https://textures.minecraft.net/texture/121d8d9ae5278e26bc4399923d25ccb9173e83748e9bad6df76314ba94369e", ""), "§e§lAnfrage senden",
                "§7Füge hiermit einen Spieler zu", "§7deinen MuxFreunden hinzu.", "",
                flist.size() >= 100 ? "§cDu hast die maximale Anzahl erreicht." : "§eKlicke§7, um diese zu senden."));
        page = ms.addPageButtons(inv, page, pages);
        flist.remove(p.getUniqueId());
        inv.setItem(5, ms.renameItemStack(new ItemStack(Material.BLAZE_ROD), u.getSettings().showFriendsByTrophies() ? "§b§lTrophäen§8 | §7§lBasewert" : "§7§lTrophäen§8 | §b§lBasewert", "§7Lege fest, nach welchen Stats du und deine", "§7Freunde immer angezeigt werden sollen.", "", "§7Klicke, um alle anders §banzuzeigen§7."));
        openInventory(p, inv, false, page, on, max);
        return true;
    }

    private void openInventory(final Player p, final Inventory inv, final boolean request, final int page, final int on, final int max) {
        final int req = requests.getOrDefault(p.getName(), new HashSet<>()).size();
        final ItemStack i = ms.renameItemStack(new ItemStack(Material.CAKE), "§e§lMuxFreunde " + "§0Page " + page, "§7Hier siehst du die Spieler, mit", "§7denen du befreundet §7bist."),
                i2 = ms.renameItemStack(new ItemStack(Material.FEATHER), "§e§l" + req + " offene Anfrage" + (req > 1 ? "n" : ""), "§7Hier sind die Spieler gelistet,", "§7die dich als Freund wollen.");
        if (max > 1) ms.addLore(i, "", (on > 0 ? "§a" : "§c") + on + "§7/" + max + " Freunde sind online.");
        if (request) ms.addLore(i, "", "§eKlicke§7, um dahin zu wechseln.");
        else ms.addLore(i2, "", "§eKlicke§7, um dahin zu wechseln.");
        inv.setItem(4, request == false ? ms.addGlow(i) : i);
        if (req > 0) inv.setItem(5, request ? ms.addGlow(i2) : i2);
        final InvType it = ms.getActiveInv(p.getName());
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        if (it != InvType.FRIENDS && it != InvType.PROFILE && it != InvType.MENU) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.FRIENDS);
    }

    public void handleInventory(final Player p, final ItemStack i, final Inventory cinv, final int rawslot, final boolean rightclick) {
        final String pname = p.getName();
        if (rawslot < 9) {
            if (ms.hasGlow(i)) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            final Material m = i.getType();
            if (m == Material.BLAZE_ROD) {
                int page = getPageNumber(cinv.getItem(4));
                MuxUser u = ms.getMuxUser(p.getName());
                if (ms.checkGeneralCooldown(p.getName(), "FRIENDSWITCH", 1000, true)) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1L, 1L);
                    return;
                }
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                u.getSettings().setShowFriendsByTrophies(u.getSettings().showFriendsByTrophies() == false);
                p.performCommand("f " + (page - 1));
                return;
            } else if (m == Material.SKULL_ITEM) {
                p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                int page = getPageNumber(cinv.getItem(4));
                if (rawslot == 7) p.performCommand("f " + (page - 1));
                else if (rawslot == 8) p.performCommand("f " + (page + 1));
                return;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            if (m == Material.FEATHER) {
                final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxFreunde§0 | Anfragen");
                final HashSet<String> list = requests.get(p.getName());
                short pos = 18;
                if (list != null) {
                    for (final String s : list) {
                        if (pos == 45) break;
                        inv.setItem(pos, ms.renameItemStack(new ItemStack(Material.PAPER), "§f§l" + s, "§7Dieser Spieler möchte mit", "§7dir befreundet sein.", "", "§aLinksklick§7, um anzunehmen.", "§cRechtsklick§7, um abzulehnen."));
                        pos++;
                    }
                }
                openInventory(p, inv, true, 0, 0, 0);
            } else if (m == Material.CAKE) {
                p.performCommand("f");
            } else if (m == Material.ITEM_FRAME) {
                p.performCommand(cinv.getTitle().contains("/") ? "menu" : "f");
            }
            return;
        }
        final Material m = i.getType();
        if (m == Material.PAPER) {
            p.closeInventory();
            final String fname = i.getItemMeta().getDisplayName().substring(4);
            final HashSet<String> flist = requests.get(pname);
            if (flist == null || flist.remove(fname) == false) {
                return;
            }
            if (rightclick) {
                ms.showItemBar(p, "§fDie Anfrage von §6" + fname + " §fwurde §cabgelehnt§f.");
                final Player fp = ms.getServer().getPlayer(fname);
                if (fp != null) {
                    fp.sendMessage("§e§lMuxFreunde>§7 Der Spieler §6" + pname + " §7hat §cabgelehnt§7.");
                }
            } else {
                acceptRequest(p, fname);
            }
        } else if (m == Material.SKULL_ITEM) {
            if (i.getItemMeta().getDisplayName().contains("Anfrage senden")) {
                if (i.getItemMeta().getLore().get(3).startsWith("§c")) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                new MuxAnvil(ms, (input, player) -> addAsFriend(ms.retardCleaner(input, "Spielername: "), player)).show(p, "Spielername: ", new ItemStack(Material.SKULL_ITEM, 1, (byte) 3));
                return;
            }
            final String fname = i.getItemMeta().getDisplayName().substring(4).replace("§e ✯", "");
            ms.getProfiles().showFullPlayerProfile(p, fname, "Freunde");
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        }
    }

    private int getPageNumber(final ItemStack is) {
        return Integer.parseInt(is.getItemMeta().getDisplayName().split(" ")[2]);
    }

    private void acceptRequest(final Player p, final String fname) {
        final String pname = p.getName();
        final OfflinePlayer op = ms.getPlayer(fname);
        final UUID uuid = p.getUniqueId(), uuid2 = op.getUniqueId();
        Set<UUID> list = friends.getOrDefault(pname, new HashSet<>());
        if (list.size() >= 100) {
            ms.showItemBar(p, "§cDu hast die maximale Anzahl an Freunden erreicht.");
            return;
        }
        list.add(uuid2);
        ms.getDB().addFriend(uuid, uuid2);
        friends.put(pname, list);
        p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 2F);
        ms.showItemBar(p, "§fDie Anfrage von §6" + fname + " §fwurde §aangenommen§f.");
        ms.showLater(p, "§fIhr könnt euch nun §anicht mehr angreifen§f.");
        final Player fp = ms.getServer().getPlayer(fname);
        if (fp != null) {
            list = friends.getOrDefault(fname, new HashSet<>());
            list.add(uuid);
            friends.put(fname, list);
            fp.playSound(fp.getLocation(), Sound.LEVEL_UP, 1F, 2F);
            fp.sendMessage("§e§lMuxFreunde> §7Der Spieler §6" + pname + " §7ist nun dein Freund.");
            fp.sendMessage("§e§lMuxFreunde>§7 Ihr könnt euch nun §anicht mehr angreifen§7.");
        } else {
            ms.getChat().sendOfflineMessage(op, "MuxFreunde", "§7Der Spieler §e" + p.getName() + " §7hat §7deine §7Anfrage §aangenommen§7.", true);
        }
    }

    private void addAsFriend(final String input, final Player p) {
        final OfflinePlayer pl = ms.getPlayer(input);
        if (pl == null) {
            ms.showItemBar(p, ms.hnotfound);
            return;
        }
        final String fname = pl.getName(), name = p.getName();
        final Set<UUID> flist = friends.get(name);
        if (flist.contains(pl.getUniqueId())) {
            ms.getProfiles().showFullPlayerProfile(p, fname, "Freunde");
            return;
        }
        final Player opl = pl.getPlayer();
        if (pl.getName().equals(p.getName())) {
            ms.showItemBar(p, "§cDu kannst dich nicht selbst als Freund haben.");
            return;
        } else if (flist.size() >= 100) {
            ms.showItemBar(p, "§cDu hast die maximale Anzahl an Freunden erreicht.");
            return;
        } else if (requests.get(fname) != null && requests.get(fname).contains(name)) {
            ms.showItemBar(p, "§cDu hast dem Spieler bereits eine Anfrage gesendet.");
            return;
        } else if (requests.get(name) != null && requests.get(name).remove(fname)) {
            acceptRequest(p, fname);
            return;
        } else if (pl.isOnline() == false || p.canSee(opl) == false) {
            ms.showItemBar(p, ms.hnotonline);
            return;
        } else if (ms.getCreators().isRecording(opl)) {
            ms.showItemBar(p, "§cDer Spieler kann gerade keine Anfragen erhalten.");
            return;
        }
        final HashSet<String> reqlist = requests.getOrDefault(fname, new HashSet<>());
        reqlist.add(name);
        requests.put(fname, reqlist);
        ms.showItemBar(p, "§fDeine Anfrage an §6" + fname + " §fwurde §agesendet§f.");
        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
        opl.playSound(opl.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
        ms.chatClickHoverRun(opl, "§e§lMuxFreunde>§7 Der Spieler §6" + name + " §7möchte dich als Freund.", "§e§oKlicke um anzunehmen", "/f " + name);
        ms.chatClickHoverRun(opl, "§e§lMuxFreunde>§7 Klicke §ehier§7, um diese Anfrage anzunehmen.", "§e§oKlicke um anzunehmen", "/f " + name);
    }
}