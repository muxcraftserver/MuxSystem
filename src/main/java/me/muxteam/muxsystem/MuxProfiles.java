package me.muxteam.muxsystem;

import com.google.common.collect.Iterables;
import me.muxteam.base.PlayerBase;
import me.muxteam.basic.MuxChatInput;
import me.muxteam.casino.MuxCasinoUser;
import me.muxteam.extras.MuxBoosters;
import me.muxteam.extras.MuxExtraUser;
import me.muxteam.extras.MuxExtras;
import me.muxteam.extras.MuxPerks;
import me.muxteam.marketing.MuxEmails;
import me.muxteam.ranks.MuxRanks;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class MuxProfiles {
    private MuxSystem ms;
    private final Map<UUID, String> backcache = new HashMap<>();
    private final MuxExtras extras;
    private final MuxPerks perks;
    private final MuxRanks perms;

    public MuxProfiles(final MuxSystem ms) {
        this.ms = ms;
        this.extras = ms.getExtras();
        this.perks = ms.getPerks();
        this.perms = ms.getPerms();
    }

    public void close() {
        this.ms = null;
    }

    public void showPlayerProfile(final Player p, final String name, final String back) {
        showPlayerProfile(p, name, back, false);
    }

    public void showFullPlayerProfile(final Player p, final String name, final String back) {
        showPlayerProfile(p, name, back, true);
    }

    private void showPlayerProfile(final Player p, final String name, final String back, final boolean full) {
        final OfflinePlayer pl = ms.getPlayer(name, true);
        if (pl == null) {
            ms.showItemBar(p, ms.hnotfound);
            return;
        }
        if (back.equals("CACHE")) {
            showProfile(p, pl, backcache.get(p.getUniqueId()), full);
            return;
        }
        showProfile(p, pl, back, full);
        backcache.put(p.getUniqueId(), back);
    }

    public void handleInventory(final Player p, final ItemStack i, final Inventory cinv) {
        final Material m = i.getType();
        final ItemMeta im = i.getItemMeta();
        if (m == Material.ITEM_FRAME) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            final String display = im.getDisplayName();
            if (display.contains("Freund")) {
                p.performCommand("f");
            } else if (display.contains("Clan")) {
                final String name = cinv.getItem(4).getItemMeta().getDisplayName().split("von ")[1];
                final OfflinePlayer pl = ms.getPlayer(name);
                if (pl == null) {
                    p.closeInventory();
                    ms.showItemBar(p, ms.hnotfound);
                    return;
                }
                ms.getClans().openPlayerMenu(p, pl);
            } else if (display.contains("Spieler")) {
                final String name = cinv.getItem(4).getItemMeta().getDisplayName().split("von ")[1];
                final Player pl = ms.getServer().getPlayer(name);
                if (pl != null) ms.getInv().openPlayerMenu(p, pl);
                else {
                    p.closeInventory();
                    ms.showItemBar(p, ms.hnotonline);
                }
            } else if (display.contains("Stats")) {
                p.performCommand("pvp");
            } else if (display.contains("Kills")) {
                p.performCommand("kills");
            } else if (display.contains("Menü")) {
                ms.getMenu().openSettings(p);
            }
        } else if (m == Material.RED_ROSE || m == Material.FLOWER_POT_ITEM) {
            if (ms.getEmails().isEnabled() == false) {
                ms.showItemBar(p, "§cDas Verifizieren von Emails ist momentan deaktiviert.");
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                p.closeInventory();
                return;
            }
            final MuxEmails.MuxMailUser mu = ms.getEmails().getUser(p.getUniqueId());

            if (mu == null || i.getItemMeta().getLore().get(i.getItemMeta().getLore().size() - 1).startsWith("§c")) {
                p.playSound(p.getLocation(), Sound.FIRE_IGNITE, 1F, 1F);
                return;
            }

            final AtomicReference<MuxChatInput> reference = new AtomicReference<>();

            final MuxChatInput chatInput = new MuxChatInput(this.ms, (input, p1) -> ms.getForkJoinPool().execute(() -> {
                mu.setProfileNewEmail(true);
                final MuxEmails.MailVerifyResult result = ms.getEmails().tryToVerifyEmail(p, input.toLowerCase(), mu);
                if (result != MuxEmails.MailVerifyResult.ERROR) {
                    reference.get().cancel();
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            ms.getChat().cancelChatInput(p, result == MuxEmails.MailVerifyResult.ERROR_CANCEL);
                        }
                    }.runTask(ms);
                } else {
                    ms.chatClickHoverRun(p, reference.get().getCancelReason(), reference.get().getCancelReason(), "/cancelmuxchatinput");
                }
            }));
            reference.set(chatInput);
            chatInput.show(p, "§f§lGebe deine neue Email Adresse ein:", true, "§7Klicke, um den Prozess abzubrechen.", 3, true);
        } else if (m == Material.CHAINMAIL_HELMET) {
            final String cname = ChatColor.stripColor(im.getDisplayName());
            if (im.getLore() != null && im.getLore().size() > 3) {
                p.performCommand("c einladen " + ChatColor.stripColor(cinv.getItem(4).getItemMeta().getDisplayName()));
                p.closeInventory();
                return;
            } else if (cname.contains(" ")) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            p.performCommand("c " + cname);
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        } else if (m == Material.SUGAR) {
            final String fname = ChatColor.stripColor(cinv.getItem(4).getItemMeta().getDisplayName().replace("§e ✯", ""));
            final OfflinePlayer pl = ms.getPlayer(fname);
            if (pl == null) return;
            final Set<UUID> alertfriends = ms.getMuxUser(p.getName()).getSettings().getAlertFriends();
            if (alertfriends.remove(pl.getUniqueId()) == false) {
                alertfriends.add(pl.getUniqueId());
            }
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            ms.getProfiles().showFullPlayerProfile(p, fname, "Freunde");
        } else if (m == Material.CAKE) {
            p.performCommand("friend " + ChatColor.stripColor(cinv.getItem(4).getItemMeta().getDisplayName()));
            p.closeInventory();
        } else if (m == Material.WOOD_SWORD) {
            if (p.hasMetadata("sfriends")) p.removeMetadata("sfriends", ms);
            if (p.hasMetadata("spmenu")) p.removeMetadata("spmenu", ms);
            if (p.hasMetadata("sclans")) p.removeMetadata("sclans", ms);
            p.setMetadata("sfriends", new FixedMetadataValue(ms, cinv.getItem(4).getItemMeta().getDisplayName().substring(4)));
            ms.getPvP().openDuelInv(p, 0, 1);
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        } else if (m == Material.TNT) {
            p.closeInventory();
            final String fname = ChatColor.stripColor(cinv.getItem(4).getItemMeta().getDisplayName());
            final OfflinePlayer pl = ms.getPlayer(fname);
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            if (pl == null) {
                ms.showItemBar(p, ms.hnotfound);
                return;
            }
            final String pname = p.getName();
            final Set<UUID> flist = ms.getFriends().getFriends(pname);
            if (flist.remove(pl.getUniqueId())) {
                ms.getDB().delFriend(p.getUniqueId(), pl.getUniqueId());
                ms.getMuxUser(p.getName()).getSettings().getAlertFriends().remove(pl.getUniqueId());
                ms.showItemBar(p, "§fDer Spieler §6" + fname + " §fwurde von deinen Freunden §centfernt§f.");
                ms.showLater(p, "§fIhr könnt euch nun §cwieder angreifen§f.");
                if (pl.isOnline()) {
                    final Player player = pl.getPlayer();
                    player.playSound(player.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                    ms.getFriends().getFriends(fname).remove(p.getUniqueId());
                    ms.getMuxUser(pl.getName()).getSettings().getAlertFriends().remove(p.getUniqueId());
                    player.sendMessage("§e§lMuxFreunde>§7 Der Spieler §6" + pname + " §7hat dich §centfernt§7.");
                    player.sendMessage("§e§lMuxFreunde>§7 Ihr könnt euch nun §cwieder angreifen§7.");
                } else {
                    final MuxUser u = ms.getChat().sendOfflineMessage(pl, "MuxFreunde", "§7Der Spieler §e" + p.getName() + " §7kann §7dich §7nun §7wieder §cangreifen§7.", false);
                    if (u == null) return;
                    u.getSettings().getAlertFriends().remove(p.getUniqueId());
                    ms.saveMuxUser(u);
                }
            }
        } else if (m == Material.WHEAT) {
            p.closeInventory();
            p.performCommand("trade " + ChatColor.stripColor(cinv.getItem(4).getItemMeta().getDisplayName()));
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
        } else if (im.hasLore() && im.getLore().removeIf(s -> s.toLowerCase().contains("klick")) == false) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
        }
    }

    private void showProfile(final Player p, final OfflinePlayer pl, final String back, final boolean full) {
        if (ms.checkGeneralCooldown(p.getName(), "STATS", 1000L, true)) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                final MuxUser u = pl.isOnline() ? ms.getMuxUser(pl.getName()) : ms.getDB().loadPlayer(pl.getUniqueId());
                final String plname = (u != null ? u.getName() : pl.getName());
                if (u == null) {
                    ms.showItemBar(p, ms.hnotfound);
                    return;
                }
                try {
                    final UUID uuid = u.getUUID();
                    if (ms.inBattle(p.getName(), p.getLocation())) {
                        if (pl.isOnline() == false) {
                            ms.showItemBar(p, ms.hnotonline);
                            return;
                        }
                        final Set<Byte> active = extras.hasPerks(uuid) ? extras.getPerks(uuid).getActive() : new HashSet<>();
                        final int psize = active.size();
                        p.sendMessage(ms.header((byte) 8, "§b"));
                        final boolean verified = ms.getAntiCheat().isVerified(pl.getPlayer());
                        final String clan = ms.getDB().getClanFromPlayer(uuid);
                        final MuxRanks.PermissionsUser pu = perms.getUserData(uuid);
                        final MuxRanks.PermissionsGroup group = pu == null ? null : perms.getGroup(pu.getDisplayedGroup());
                        final MuxRanks.PermissionsGroup rankgroup = pu == null || group.isTeamGroup() == false || pu.getGroup() == null ? null : perms.getGroup(pu.getGroup());
                        p.sendMessage("  §7Name: §b" + plname + (verified ? "§a✔" : ""));
                        p.sendMessage("  §7" + ms.getLang("rank") + ": " + (group != null ? (ChatColor.translateAlternateColorCodes('&', group.getPrefix().replace("&e", "&6")) +
                                (group.isTeamGroup() && rankgroup != null ? " §8| " + ChatColor.translateAlternateColorCodes('&', rankgroup.getPrefix().replace("&e", "&6")) : "") ) : "§9" + ms.getLang("member")));
                        p.sendMessage("  §7Trophäen: §b" + u.getTrophies());
                        p.sendMessage("  §7Clan: §b" + (clan != null ? clan : "§b§okein Clan"));
                        p.sendMessage("  §7Perks: §a" + (active.isEmpty() ? "§7§o" + ms.getLang("none") : perks.getPerkName(Iterables.get(active, 0)) + (psize > 1 ? "§7, " + perks.getPerkName(Iterables.get(active, 1)) : "")));
                        if (psize > 2) {
                            ms.chatClickHoverShow(p, "§a           " + String.format(ms.getLang("cmd.userandmore"), psize - 2) + (psize == 3 ? "s" : "") + "...", "§a§o" + active.stream().skip(2).map(perks::getPerkName).collect(Collectors.joining("§7, §a§o")), "/perks");
                        }
                        p.sendMessage(ms.footer((byte) 8, "§b"));
                        return;
                    }
                    final boolean own = p.getName().equals(plname);
                    final boolean friends = own == false && ms.getFriends().areFriends(p.getName(), uuid);
                    final Inventory inv = ms.getServer().createInventory(null, full ? 45 : 27, "§0§lMux" + (full && friends ? "Freunde" : "Profil") + "§0 | " + (own ? "Mein Profil" : plname));
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            final MuxInventory.InvType it = ms.getActiveInv(p.getName());
                            if (it != MuxInventory.InvType.PROFILE && it != MuxInventory.InvType.FRIENDS && it != MuxInventory.InvType.PMENU && it != MuxInventory.InvType.PVP && it != MuxInventory.InvType.MENU && it != MuxInventory.InvType.CLANS && it != MuxInventory.InvType.MUXANVIL)
                                p.closeInventory();
                            p.openInventory(inv);
                            ms.setActiveInv(p.getName(), MuxInventory.InvType.PROFILE);
                        }
                    }.runTask(ms);
                    final boolean on = pl.isOnline() && p.canSee(pl.getPlayer());
                    // EMAIL
                    if (full && own) {
                        final MuxEmails.MuxMailUser mu = ms.getEmails().getUser(p.getUniqueId());
                        if (mu != null && mu.getEmail() != null) {
                            final String email = mu.getEmail();
                            String hiddenmail;
                            if (email.length() > 6) {
                                final int numChars = Math.min(3, email.indexOf("@")), numStars = email.indexOf("@") - numChars;
                                hiddenmail = email.substring(0, numChars) + new String(new char[numStars]).replace("\0", "*") + email.substring(email.indexOf("@"));
                            } else {
                                hiddenmail = email;
                            }
                            inv.setItem(inv.getSize() - 5, ms.renameItemStack(new ItemStack(Material.RED_ROSE, 1, (byte) 8), "§a§lVerifiziert§a ✔",
                                    "§7Unter folgender Mailadresse ist", "§7dein Account derzeit bestätigt:", "",
                                    "§a" + hiddenmail, "", (ms.checkGeneralCooldown(p.getName(), "EMAILVERIFY", 300000L, false)) ? "§cDu kannst dich in 5 Minuten erneut verifizieren." :
                                            mu.getCurrentTries() == 3 && mu.getLastVerifyTime() > System.currentTimeMillis() - 7200000L ? "§cDu hast zu oft probiert dich zu verifizieren." : "§7Klicke, falls du sie §aändern willst§7."));
                        } else {
                            inv.setItem(inv.getSize() - 5, ms.renameItemStack(new ItemStack(Material.FLOWER_POT_ITEM), "§7§lNicht verifiziert",
                                    "§7Dein Account wurde bisher noch", "§7nicht von dir offiziell bestätigt", "", "§7Klicke, um direkt zu §averifizieren§7."));
                        }
                    }
                    // OPTIONS
                    if (full && own == false) {
                        final String clan = ms.getDB().getClanFromPlayer(pl.getUniqueId()), hasclan = ms.getDB().getClanFromPlayer(p.getUniqueId());
                        final boolean inviteclan = on && hasclan != null && clan == null && ms.getDB().getClanRank(p.getUniqueId()) > 0;
                        if (friends) {
                            inv.setItem(inviteclan ? 43 : on ? 42 : 40, ms.renameItemStack(new ItemStack(Material.TNT), "§c§lFreund entfernen", "§7Hiermit brichst du die Freundschaft", "§7zwischen euch offiziell ab.", "", "§7Klicke, um diesen zu §centfernen§7."));
                            final boolean alert = ms.getMuxUser(p.getName()).getSettings().getAlertFriends().contains(pl.getUniqueId());
                            final ItemStack alertitem = ms.renameItemStack(new ItemStack(Material.SUGAR), alert ? "§f§lEnger Freund§e ✯" : "§f§lEnger Freund", "§7Wenn dieser dann online kommt,", "§7wirst du sofort benachrichtigt.", "",
                                    alert ? "§7Klicke und §centferne §7diesen Spieler" : "§7Klicke und §amarkiere §7diesen Spieler", alert ? "§7von deinen engen Freunden." : "§7mit §e✯ §7als deinen engen Freund.");
                            inv.setItem(8, alert ? ms.addGlow(alertitem) : alertitem);
                        }
                        if (on) {
                            if (friends)
                                inv.setItem(inviteclan ? 41 : 40, ms.renameItemStack(new ItemStack(Material.WOOD_SWORD), "§6§lFreundschaftskampf starten", "§7Beginne ein Duell mit deinem Freund,", "§7ohne Trophäen zu riskieren.", "", "§6Klicke§7, um mit ihm zu kämpfen."));
                            else
                                inv.setItem(42, ms.renameItemStack(new ItemStack(Material.CAKE), "§e§lAls Freund hinzufügen", "§7Füge hiermit diesen Spieler zu", "§7deinen MuxFreunden hinzu.", "", "§eKlicke§7, um die Anfrage zu senden."));
                            if (inviteclan)
                                inv.setItem(friends ? 37 : 38, ms.renameItemStack(new ItemStack(Material.CHAINMAIL_HELMET), "§3§lZum Clan einladen", "§7Vergrößer deinen Clan, indem du", "§7diesen Spieler hineinbringst.", "", "§3Klicke§7, um ihn einzuladen."));
                            inv.setItem(inviteclan && friends ? 39 : inviteclan ? 40 : 38, ms.renameItemStack(new ItemStack(Material.WHEAT), "§e§lHandel starten", "§7Tauscht Items und MuxCoins", "§7im fairen Deal miteinander.", "", "§eKlicke§7, um mit ihm zu handeln."));
                        }
                    }
                    String online;
                    if (u.getOnlineTime() == 0) {
                        online = "Noch nicht definiert";
                    } else if (on) {
                        online = ms.getTime((int) ((System.currentTimeMillis() - u.getLoginTimestamp() + u.getOnlineTime()) / 1000L));
                    } else {
                        online = ms.getTime((int) (u.getOnlineTime() / 1000L));
                    }
                    final double kdr = u.getKills() / (double) (u.getDeaths() == 0 ? 1 : u.getDeaths());
                    final int rank = u.isPvPbanned() ? -2 : u.getLastRankedMatch() < System.currentTimeMillis() - 172800000L ? -1 : ms.getDB().getStatsRank(uuid);
                    inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back") + (back != null ? " (" + back + ")" : "")));
                    final String status = ms.getPlayerStatus(p, pl, u, friends), clan = ms.getDB().getClanFromPlayer(uuid);
                    if (full) {
                        inv.setItem(4, ms.renameItemStack(on ? ms.getHead(plname) : new ItemStack(Material.SKULL_ITEM, 1, (byte) 0), "§f§l" + (own ? "Mein Profil" : plname),
                                own ? new String[]{"§7Hier siehst du deine eigenen", "§7bisherigen Statistiken."} : new String[]{"§7Status: " + status}));
                    } else {
                        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.BOOK), "§b§l" + (own ? "Mein Profil" : "Profil von " + plname), own ?
                                "§7Hier siehst du deine eigenen" : "§7Hier siehst du die ganzen", "§7bisherigen Statistiken."));
                    }
                    final int trophies = u.getTrophies();
                    final MuxRanks.PermissionsUser pu = perms.getUserData(uuid);
                    final MuxRanks.PermissionsGroup group = pu == null ? null : perms.getGroup(pu.getDisplayedGroup()), rankgroup = pu == null || group.isTeamGroup() == false || pu.getGroup() == null ? null : perms.getGroup(pu.getGroup());
                    final String ranklore = "§7" + ms.getLang("rank") + ": §8" + (group != null ? (StringUtils.stripEnd(ChatColor.translateAlternateColorCodes('&', group.getPrefix()), " ") +
                            (group.isTeamGroup() && rankgroup != null ? " §8| " + ChatColor.translateAlternateColorCodes('&', rankgroup.getPrefix()) : "")) : "§9" + ms.getLang("member"));
                    final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
                    sdf.setTimeZone(TimeZone.getTimeZone("CET"));
                    ItemStack infoitem;
                    if (full) {
                        infoitem = ms.renameItemStack(new ItemStack(Material.BOOK), "§f§l" + plname, ranklore,
                                "§7Spielzeit: §b" + online, "§7Beitrittsdatum: §b" + sdf.format(new Date(u.getFirstLogin())));
                    } else {
                        infoitem = ms.renameItemStack(ms.getHead(plname), "§f§l" + plname, "§7Status: " + status, ranklore,
                                "§7Spielzeit: §b" + online, "§7Beitrittsdatum: §b" + sdf.format(new Date(u.getFirstLogin())));
                    }
                    inv.setItem(19, infoitem);
                    inv.setItem(23, ms.renameItemStack(new ItemStack(Material.CHAINMAIL_HELMET), clan == null ? "§7§lKein Clan" : "§3§l" + clan, clan == null ? new String[0] : new String[]{"", "§3Klicke§7, um Claninfos zu sehen."}));
                    inv.setItem(20, ms.renameItemStack(new ItemStack(Material.DIAMOND_CHESTPLATE), "§b§lKampf Stats", "§7Platz: " + (rank == -2 ? "§cAusgeschlossen" : rank == -1 ? "§cUnranked" : (rank == 1 ? "§e#" : rank == 2 ? "§f#" : rank == 3 ? "§6#" : "§b#") + ms.getNumberFormat(rank)) + " §8| §7Trophäen: §b" + trophies,
                            "§7Kills: §b" + u.getKills() + " §8| §7Tode: §b" + u.getDeaths() + " §8| §7K/D: §b" + ms.getDecimalFormat(kdr)));
                    final List<Object[]> pvphistory = ms.getDB().getPvPHistory(uuid, "PVP", 5);
                    final List<String> lore = new ArrayList<>();
                    if (pvphistory != null) {
                        for (final Object[] fight : pvphistory) {
                            final OfflinePlayer off = ms.getServer().getOfflinePlayer((UUID) fight[0]);
                            final int wonlost = (int) fight[1];
                            final String type = (String) fight[2];
                            lore.add("§7" + plname + " §7vs §b" + (off.getName() != null ? off.getName() : "Spieler") + " §7im " + type +
                                    (wonlost == 0 ? "" : " §8| " + (wonlost < 0 ? "§c" : "§a+") + wonlost));
                        }
                    }
                    final MuxExtraUser eu = pl.isOnline() ? extras.getExtraUser(uuid) : ms.getDB().loadExtras(uuid);
                    final Set<Byte> pown = eu == null ? null : eu.getPerks().getOwned();
                    final boolean hasperks = pown != null && pown.isEmpty() == false;
                    final Set<Byte> active = hasperks ? eu.getPerks().getActive() : new HashSet<>(), owned = hasperks ? pown : new HashSet<>();
                    final String ptext = owned.stream().map(b -> (active.contains(b) ? "§6" : "§7") + perks.getPerkName(b)).collect(Collectors.joining("§7, "));
                    ItemStack pitem = ms.renameItemStack(new ItemStack(Material.BLAZE_POWDER), "§6§lPerks");
                    if (owned.isEmpty()) {
                        pitem = ms.addLore(pitem, "§7§o" + ms.getLang("none"));
                    } else {
                        pitem = ms.addLore(pitem, "§7Sammlung: §6" + owned.size() + "§7/§6" + extras.getPerks(), "");
                        pitem = ms.addLore(pitem, ms.getLoreFromLongText(ptext, null));
                    }
                    inv.setItem(24, pitem);
                    if (lore.isEmpty()) {
                        lore.add("§7Kämpfe zunächst, um in der");
                        lore.add("§7Liga aufsteigen zu können.");
                    }
                    final PlayerBase base = ms.getBase().getFromUUID(uuid);
                    final int rankb = base == null ? -1 : ms.getBase().getBaseRanking(base);
                    inv.setItem(22, ms.renameItemStack(new ItemStack(Material.SAPLING, 1), "§a§lBase", "§7Platz: " + (rankb == -1 ? "§a#-" : (rankb == 1 ? "§e#" : rankb == 2 ? "§f#" : rankb == 3 ? "§6#" : "§a#") + ms.getNumberFormat(rankb)), "§7Basewert: §a" + (rankb == -1 ? "-" : ms.getNumberFormat(base.getValue()))));
                    final MuxCasinoUser cu = ms.getDB().loadCasino(uuid);
                    inv.setItem(25, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT, 1), "§e§lWertsachen", "§7MuxCoins: §e" + ms.getNumberFormat(u.getCoins()), "§7Chips: §d" + ms.getNumberFormat(cu.getChips()), "§7Gems: §a" + ms.getNumberFormat(u.getGems())));
                    inv.setItem(21, ms.addLore(ms.renameItemStack(new ItemStack(Material.ARMOR_STAND), ms.getStats().getColoredBoldLeague(trophies)), lore));
                    if (on) {
                        final Player player = pl.getPlayer();
                        final Set<MuxBoosters.BoosterType> activebooster = ms.getBooster().getBooster(player);
                        if (activebooster.isEmpty() == false) {
                            inv.setItem(8, ms.renameItemStack(ms.getFireworkCharge(Color.GRAY, true), "§5§lBooster", "§7" + activebooster.stream().map(MuxBoosters.BoosterType::getName).collect(Collectors.joining("§7, "))));
                        }
                    }
                } catch (final Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.runTaskAsynchronously(ms);
    }
}