package me.muxteam.social;

import me.muxteam.basic.ConfirmInventory;
import me.muxteam.basic.MuxActions.PlayerAction;
import me.muxteam.basic.MuxAnvil;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.muxsystem.MySQL;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World.Environment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MuxClans {
    private MuxSystem ms;
    private final MySQL db;
    private Inventory definv, membinv, modinv, adminv;
    private final ItemStack plusHead;
    private Location npcloc;
    private final String TEAM_CLAN = "MuxTeam";

    public MuxClans(final MuxSystem ms) {
        this.ms = ms;
        this.db = ms.getDB();
        this.plusHead = ms.getHeadFromURL("https://textures.minecraft.net/texture/60b55f74681c68283a1c1ce51f1c83b52e2971c91ee34efcb598df3990a7e7", "plus");
        setup();
    }

    public void close() {
        this.ms = null;
    }

    private void setup() {
        final Server sr = ms.getServer();
        definv = sr.createInventory(null, 27, "§0§lMuxClans§0 | /clan");
        membinv = sr.createInventory(null, 45, "§0§lMuxClans§0 | /clan");
        modinv = sr.createInventory(null, 45, "§0§lMuxClans§0 | /clan");
        adminv = sr.createInventory(null, 45, "§0§lMuxClans§0 | /clan");
        final ItemStack erstellen = ms.renameItemStack(new ItemStack(Material.EMERALD_BLOCK), "§3§lClan§a §lerstellen", "§7Gründe ein Team gemeinsam mit", "§7deinen Freunden und Kollegen.", "", "§aKlicke§7, um einen Clan zu erstellen."),
                infosvon = ms.renameItemStack(new ItemStack(Material.COMPASS), "§3§lClan suchen", "§7Alle Mitglieder und Statistiken eines", "§7Clans werden hier aufgelistet.", "", "§7Klicke, um ein §3Clan zu suchen§7."),
                top = ms.renameItemStack(new ItemStack(Material.GOLD_INGOT), "§3§lStats", "§7Sortiert nach Trophäen sind die", "§7besten Clans hier aufgelistet.", "", "§3Klicke§7, um dahin zu wechseln."),
                deinclan = ms.renameItemStack(new ItemStack(Material.CHAINMAIL_HELMET), "§3§lAllgemeines", "§7Alle deine Clanmitglieder kannst", "§7du unter anderem hier finden.");
        definv.setItem(10, erstellen);
        definv.setItem(13, top);
        definv.setItem(16, infosvon);
        membinv.setItem(1, deinclan);
        membinv.setItem(4, top);
        membinv.setItem(7, infosvon);
        modinv.setContents(membinv.getContents());
        adminv.setContents(membinv.getContents());
        modinv.setItem(44, ms.renameItemStack(new ItemStack(Material.WORKBENCH), "§3§lClanort§5 [CLANMOD]", "§7Der Ort von den vertraulichen", "§7Clanmitgliedern liegt dort.", "", "§3Klicke§7, um dorthin zu gelangen."));
        adminv.setItem(44, ms.renameItemStack(new ItemStack(Material.WORKBENCH), "§3§lClanort§5 [CLANMOD]", "§7Der Ort von den vertraulichen", "§7Clanmitgliedern liegt dort.", "", "§3Linksklick§7, um dorthin zu gelangen.", "§eRechtsklick§7, um ihn zu überschreiben."));
        npcloc = new Location(ms.getGameSpawn().getWorld(), -6.5D, 76D, 7.5D);
        ms.getHolograms().addIndividualHologram("clanholo", npcloc.clone().add(0, 0.35, 0), (holo, p) -> {
            final UUID uuid = p.getUniqueId();
            final String clan = db.getClanFromPlayer(uuid);
            if (clan == null) holo.setName(p, "");
            else {
                final int rank = cachedranking.getOrDefault(clan, 0);
                cachedranking.put(clan, rank);
                final String chattext = db.getChatText(uuid), level = "§fLevel: " + (chattext.startsWith("§4") ? "§4Rot" : chattext.startsWith("§6") ? "§6Gold" : chattext.startsWith("§5") ? "§5Lila" : chattext.startsWith("§a") ? "§aGrün" : "§3Blau");
                holo.setName(p, level + "§f | Platz: " + (rank == 1 ? "§e#" : rank == 2 ? "§f#" : rank == 3 ? "§6#" : "§3#") + (rank == -1 ? "-" : rank));
            }
        }, true);
       ms.getHolograms().addIndividualHologram("clanholo2", npcloc.clone(), (holo, p) -> {
            final UUID uuid = p.getUniqueId();
            final String clan = db.getClanFromPlayer(uuid);
            if (clan == null) holo.setName(p, "");
            else {
                final int ctrophies = cachedtrophies.getOrDefault(clan, 0);
                cachedtrophies.put(clan, ctrophies);
                final String chattext = db.getChatText(uuid), trophsleft = chattext.startsWith("§4") || chattext.startsWith("§6") ? "§aHöchstes Level erreicht." : chattext.startsWith("§5") ? "§fNoch §b"  + (12500 - ctrophies) + " Trophäen §fbis §6Gold" : chattext.startsWith("§a") ? "§fNoch §b"  + (7500 - ctrophies) + " Trophäen §fbis §5Lila" : "§fNoch §b"  + (5000 - ctrophies) + " Trophäen §fbis §aGrün";
                holo.setName(p, trophsleft);
            }
        }, true);
       new BukkitRunnable() {
           @Override public void run() {
               for (final String clan : cachedranking.keySet()) {
                   if (ms.checkGeneralCooldown(clan, "CLANRANKINGCACHE", 30000L, false) == false) {
                       final int rank = db.getClanRanking(clan);
                       cachedranking.put(clan, rank);
                       ms.checkGeneralCooldown(clan, "CLANRANKINGCACHE", 30000L, true);
                   }
               }
               for (final String clan : cachedtrophies.keySet()) {
                   if (ms.checkGeneralCooldown(clan, "CLANTROPHIESCACHE", 30000L, false) == false) {
                       final int trophs = db.getClanTrophies(clan);
                       cachedtrophies.put(clan, trophs);
                       ms.checkGeneralCooldown(clan, "CLANTROPHIESCACHE", 30000L, true);
                   }
               }
           }
       }.runTaskTimerAsynchronously(ms, 20L, 60L);
    }

    public Location getNPCLoc() {
        return npcloc;
    }

    private final Map<String, Integer> cachedranking = new ConcurrentHashMap<>(), cachedtrophies = new ConcurrentHashMap<>();

    public String geTeamClan() {
        return TEAM_CLAN;
    }

    private void openMenu(final Player pl) {
        final InvType it = ms.getActiveInv(pl.getName());
        if (it != InvType.CLANS && it != InvType.CLANWAR && it != InvType.CONFIRM) pl.closeInventory();
        final UUID uuid = pl.getUniqueId();
        final Inventory temp;
        switch (db.getClanRank(uuid)) {
            case -1:
                final List<String> invites = db.getInvitations(uuid);
                if (invites.isEmpty()) {
                    pl.openInventory(definv);
                } else {
                    final int size = invites.size();
                    temp = ms.getServer().createInventory(null, 27, "§0§lMuxClans§0 | /clan");
                    temp.setContents(definv.getContents());
                    temp.setItem(11, ms.renameItemStack(new ItemStack(Material.FEATHER, size), "§3§l" + size + " Claneinladung" + (size == 1 ? "" : "en") + "!",
                            "§7Nehme eine Einladung an, um", "§7diesen Clan beizutreten.", "", "§3Klicke§7, um diese zu sehen."));
                    pl.openInventory(temp);
                }
                break;
            case 0:
                temp = ms.getServer().createInventory(null, 45, "§0§lMuxClans§0 | /clan");
                temp.setContents(membinv.getContents());
                temp.setItem(1, ms.addGlow(ms.renameItemStack(new ItemStack(Material.CHAINMAIL_HELMET), "§3§lAllgemeines §8| " + db.getChatText(uuid), "§7Alle deine Clanmitglieder kannst", "§7du unter anderem hier finden.")));
                addPlayerItemsOfClan(temp, pl, db.getClanFromPlayer(pl.getUniqueId()), true);
                pl.openInventory(temp);
                break;
            case 1:
                temp = ms.getServer().createInventory(null, 45, "§0§lMuxClans§0 | /clan");
                temp.setContents(modinv.getContents());
                temp.setItem(1, ms.addGlow(ms.renameItemStack(new ItemStack(Material.CHAINMAIL_HELMET), "§3§lAllgemeines §8| " + db.getChatText(uuid), "§7Alle deine Clanmitglieder kannst", "§7du unter anderem hier finden.")));
                addPlayerItemsOfClan(temp, pl, db.getClanFromPlayer(pl.getUniqueId()), true);
                pl.openInventory(temp);
                break;
            case 2:
                temp = ms.getServer().createInventory(null, 45, "§0§lMuxClans§0 | /clan");
                temp.setContents(adminv.getContents());
                temp.setItem(1, ms.addGlow(ms.renameItemStack(new ItemStack(Material.CHAINMAIL_HELMET), "§3§lAllgemeines §8| " + db.getChatText(uuid), "§7Alle deine Clanmitglieder kannst", "§7du unter anderem hier finden.")));
                addPlayerItemsOfClan(temp, pl, db.getClanFromPlayer(pl.getUniqueId()), true);
                pl.openInventory(temp);
                break;
            default:
                break;
        }
        ms.setActiveInv(pl.getName(), InvType.CLANS);
    }
    public void openPlayerMenu(final Player p, final OfflinePlayer ply) {
        final String plname = ply.getName();
        final InvType it = ms.getActiveInv(p.getName());
        if (it != InvType.CLANS && it != InvType.CLANWAR && it != InvType.CONFIRM && it != InvType.PROFILE && it != InvType.PVP) p.closeInventory();
        final UUID uuid = p.getUniqueId();
        final boolean online = ply.isOnline() && p.canSee(ply.getPlayer());
        final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lMuxClans§0 | " + plname);
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        final MuxUser u = ply.isOnline() ? ms.getMuxUser(ply.getName()) : db.loadPlayer(uuid);
        final int trophies = (u == null ? 0 : u.getTrophies());
        final int plrank = db.getClanRank(ply.getUniqueId());
        inv.setItem(4, ms.addGlow(ms.renameItemStack(online ? ms.getHead(plname) : new ItemStack(Material.SKULL_ITEM, 1, (byte) 0), "§f§l" + plname,
                "§7Rang: " + (plrank == 1 ? "§5Clanmoderator" : plrank == 2 ? "§cClanadmin" : "§9Clanmitglied"),
                "§7Status: " + ms.getPlayerStatus(p, ply, u, false), "§7Trophäen: §b" + trophies)));
        final int rank = db.getClanRank(uuid);
        if (p.getName().equals(plname)) {
            if (rank == 2) {
                inv.setItem(20, ms.renameItemStack(new ItemStack(Material.BOOK_AND_QUILL), "§3§lClan umbenennen §c[CLANADMIN]", "§7Hiermit kannst du deinem Clan", "§7einen neuen Namen geben.", "", "§7Klicke, um deinen Clan §3umzubenennen§7."));
                inv.setItem(24, ms.renameItemStack(new ItemStack(Material.TNT), "§3§lClan§c §llöschen §c[CLANADMIN]", "§7Löse deinen Clan komplett auf oder", "§7gib ihn an jemanden weiter.", "", "§cKlicke§7, um dich zu entscheiden."));
            }
            else inv.setItem(22, ms.renameItemStack(new ItemStack(Material.REDSTONE_BLOCK), "§3§lClan§c §lverlassen", "§7Beende hiermit deine Mitgliedschaft", "§7und trete von diesem Clan aus.", "", "§7Klicke, um deinen Clan zu §cverlassen§7."));
            p.openInventory(inv);
            ms.setActiveInv(p.getName(), InvType.CLANS);
            return;
        } else if (rank == 0 || rank <= plrank) {
            inv.setItem(online ? 20 : 22, ms.renameItemStack(new ItemStack(Material.BOOK), "§b§lProfil sehen", "§7Schaue dir das Profil", "§7dieses Spielers an.", "", "§bKlicke§7, um diesen zu sehen."));
            if (online) {
                final boolean notfriends = ms.getFriends().areFriends(p.getName(), ply.getUniqueId()) == false;
                if (notfriends) {
                    inv.setItem(24, ms.renameItemStack(new ItemStack(Material.CAKE), "§e§lAls Freund hinzufügen", "§7Füge hiermit diesen Spieler zu", "§7deinen MuxFreunden hinzu.", "", "§eKlicke§7, um die Anfrage zu senden."));
                } else {
                    inv.setItem(24, ms.renameItemStack(new ItemStack(Material.WOOD_SWORD), "§6§lFreundschaftskampf starten", "§7Beginne ein Duell mit deinem Freund,", "§7ohne Trophäen zu riskieren.", "", "§6Klicke§7, um mit ihm zu kämpfen."));
                }
            }
        } else if (rank == 1) {
            inv.setItem(online ? 19 : 20, ms.renameItemStack(new ItemStack(Material.BOOK), "§b§lProfil sehen", "§7Schaue dir das Profil", "§7dieses Spielers an.", "", "§bKlicke§7, um diesen zu sehen."));
            if (online) {
                final boolean notfriends = ms.getFriends().areFriends(p.getName(), ply.getUniqueId()) == false;
                if (notfriends) {
                    inv.setItem(22, ms.renameItemStack(new ItemStack(Material.CAKE), "§e§lAls Freund hinzufügen", "§7Füge hiermit diesen Spieler zu", "§7deinen MuxFreunden hinzu.", "", "§eKlicke§7, um die Anfrage zu senden."));
                } else {
                    inv.setItem(22, ms.renameItemStack(new ItemStack(Material.WOOD_SWORD), "§6§lFreundschaftskampf starten", "§7Beginne ein Duell mit deinem Freund,", "§7ohne Trophäen zu riskieren.", "", "§6Klicke§7, um mit ihm zu kämpfen."));
                }
            }
            inv.setItem(online ? 25 : 24, ms.renameItemStack(new ItemStack(Material.IRON_BOOTS), "§3§lSpieler kicken§5 [CLANMOD]", "§7Hiermit kannst du den Spieler", "§7von deinem Clan entfernen.", "", "§7Klicke, um diesen Spieler aus", "§7deinem Clan zu §centfernen§7."));
        } else if (rank == 2) {
            inv.setItem(19, ms.renameItemStack(new ItemStack(Material.BOOK), "§b§lProfil sehen", "§7Schaue dir das Profil", "§7dieses Spielers an.", "", "§bKlicke§7, um diesen zu sehen."));
            if (online) {
                final boolean notfriends = ms.getFriends().areFriends(p.getName(), ply.getUniqueId()) == false;
                if (notfriends) {
                    inv.setItem(21, ms.renameItemStack(new ItemStack(Material.CAKE), "§e§lAls Freund hinzufügen", "§7Füge hiermit diesen Spieler zu", "§7deinen MuxFreunden hinzu.", "", "§eKlicke§7, um die Anfrage zu senden."));
                } else {
                    inv.setItem(21, ms.renameItemStack(new ItemStack(Material.WOOD_SWORD), "§6§lFreundschaftskampf starten", "§7Beginne ein Duell mit deinem Freund,", "§7ohne Trophäen zu riskieren.", "", "§6Klicke§7, um mit ihm zu kämpfen."));
                }
            }
            inv.setItem(online ? 23 : 22, ms.renameItemStack(new ItemStack(Material.IRON_BOOTS), "§3§lSpieler kicken§5 [CLANMOD]", "§7Hiermit kannst du den Spieler", "§7von deinem Clan entfernen.", "", "§7Klicke, um diesen Spieler aus", "§7deinem Clan zu §centfernen§7."));
            inv.setItem(25, ms.renameItemStack(new ItemStack(Material.NAME_TAG), "§3§lClanrang ändern§c [CLANADMIN]", "§7Gib den Spielern, denen du wirklich", "§7vertraust, einen höheren Rang.", "", "§7Klicke, um diesen Spieler auf dem", "§7Rang '" + (plrank == 1 ? "§9Clanmitglied" : "§5Clanmoderator") + "§7' zu setzen."));
        }
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.CLANS);
    }

    public boolean handleClanChat(final Player p, final String msg) {
        if (msg.startsWith("#") == false || ms.getBans().isMuted(p.getName())) {
            return false;
        }
        final List<UUID> members = db.getClanMembersOfPlayer(p.getUniqueId());
        if (members == null) {
            return false;
        }
        final int rank = db.getClanRank(p.getUniqueId());
        final String omsg = msg.startsWith("#") ? msg.substring(1) : msg, cmsg = "§3§lClanChat>" + (rank == 2 ? "§c" : rank == 1 ? "§5" : "§9") + " " + p.getName() + "§8: §7" + omsg,
                amsg = "§3CC> §8" + p.getName() + ": §7" + ChatColor.stripColor(omsg);
        final String ad = ms.getChat().isAdvertising(p, omsg);
        // MuxTeam
        final String pclan = db.getClanFromPlayer(p.getUniqueId());
        if (pclan.equals(TEAM_CLAN) && ms.getChat().handleTeamChat(p, msg.replaceFirst("#", "!"))) {
            return true;
        }
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            if (pl.isOp() && ms.getCreators().isRecording(pl) == false) {
                final String clan = db.getClanFromPlayer(pl.getUniqueId());
                if (clan != null && clan.equals(pclan) && ad == null) continue;
                pl.sendMessage((ad != null ? "§8[§c✖§8] " : "") + amsg);
            }
        }
        if (ad != null) {
            p.sendMessage(cmsg);
            return true;
        }
        for (final UUID plr : members) {
            final Player pl = ms.getServer().getPlayer(plr);
            if (pl != null) pl.sendMessage(cmsg);
        }
        return true;
    }

    public void leaveTeamClan(final OfflinePlayer ply, final String name) {
        final UUID uuid = ply.getUniqueId();
        if (isInTeamClan(ply) && (ply.isOp() == false && ms.isTrusted(name) == false && ms.isFullTrusted(name) == false)) {
            ms.getDB().leaveClan(uuid);
        }
    }

    public boolean isInTeamClan(final OfflinePlayer op) {
        final String clan = ms.getDB().getClanFromPlayer(op.getUniqueId());
        return clan != null && clan.equals(TEAM_CLAN);
    }

    public void handleInventory(final Player pl, final ItemStack item, final boolean rightclick, final Inventory cinv) {
        final Material m = item.getType();
        final String invname = cinv.getName();
        final ItemMeta im = item.getItemMeta();
        if (m == Material.CHAINMAIL_HELMET && item.getItemMeta().getDisplayName().startsWith("§3§lAllgemeines")) {
            if (ms.hasGlow(item)) {
                pl.playSound(pl.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            pl.playSound(pl.getLocation(), Sound.CLICK, 1F, 1F);
            openMenu(pl);
            return;
        } else if (m == Material.SKULL_ITEM) {
            if (im.getLore().size() == 5) {
                final OfflinePlayer ply = ms.getPlayer(ChatColor.stripColor(im.getDisplayName()));
                openPlayerMenu(pl, ply);
                pl.playSound(pl.getLocation(), Sound.CLICK, 1F, 1F);
                return;
            } else if (im.hasDisplayName() && im.getDisplayName().contains("einladen")) {
                new MuxAnvil(ms, (input, player) -> invitePlayer(pl, ms.retardCleaner(input, "Spielername: "))).show(pl, "Spielername: ", new ItemStack(Material.SKULL_ITEM, 1, (byte) 3));
                pl.playSound(pl.getLocation(), Sound.CLICK, 1F, 1F);
                return;
            } else if (im.hasLore() == false || im.getLore().size() != 6) {
                pl.playSound(pl.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            final UUID uuid = pl.getUniqueId();
            final int rank = db.getClanRank(uuid);
            pl.closeInventory();
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            switch (rank) {
                case -1:
                    ms.showItemBar(pl, "§cDu bist in keinem Clan.");
                    break;
                case 0:
                case 1:
                    ms.showItemBar(pl, "§cDu bist nicht berechtigt, den Clan weiterzugeben.");
                    break;
                default:
                    final OfflinePlayer ply = ms.getPlayer(ChatColor.stripColor(im.getDisplayName()));
                    if (ply == null) {
                        ms.showItemBar(pl, ms.hnotfound);
                        break;
                    }
                    final UUID puuid = ply.getUniqueId();
                    final String clan = db.getClanFromPlayer(puuid);
                    if (clan == null || clan.equals(db.getClanFromPlayer(uuid)) == false) {
                        ms.showItemBar(pl, "§cDer Spieler ist nicht in deinem Clan.");
                        break;
                    }
                    final String brmsg = "§3§lMuxClans>§7 Der Spieler §c" + pl.getName() + " §7hat den Clan §cverlassen §7und " + (db.getClanRank(puuid) == 1 ? "§5" : "§9") + ply.getName() + " §7als Clanadmin ernannt.",
                            offmsg = "§c" + ply.getName() + " §7ist nun neuer Clanadmin.";
                    db.setRank(uuid, (short) 0);
                    db.setRank(puuid, (short) 2);
                    final Server sr = ms.getServer();
                    final Player player = sr.getPlayer(puuid);
                    if (player != null) player.closeInventory();
                    for (final UUID s : db.getClanMembers(db.getClanFromPlayer(uuid))) {
                        final Player ple = sr.getPlayer(s);
                        if (ple != null) ple.sendMessage(brmsg);
                        else sendOffline(sr, s, offmsg);
                    }
                    db.leaveClan(uuid);
                    ms.sendScoreboard(pl);
                    break;
            }
            return;
        } else if (m == Material.WORKBENCH) {
            final String cname = invname.split(" \\| ")[1];
            if (pl.isOp() && cname.equals("/clan") == false) {
                pl.closeInventory();
                final String clan = db.getClanCase(cname);
                if (clan == null) {
                    ms.showItemBar(pl, "§cEs gibt diesen Clan nicht.");
                    return;
                }
                toClanBase(pl, db.getClanBase(clan), clan);
                return;
            }
            if (rightclick) {
                switch (db.getClanRank(pl.getUniqueId())) {
                    case -1:
                        pl.closeInventory();
                        ms.showItemBar(pl, "§cDu bist in keinem Clan.");
                        return;
                    case 2:
                        final PlayerAction success = p -> {
                            final Location l = pl.getLocation();
                            if (l.getWorld() == ms.getBase().getWorld() || l.getWorld().equals(ms.getWarpsWorld()) || l.getWorld().getName().equals("MuxCasino") || l.getWorld().getEnvironment() == Environment.THE_END || l.getY() < 1D) {
                                ms.showItemBar(pl, "§cDu kannst hier keinen Clanort setzen.");
                                return;
                            }
                            db.setClanBase(db.getClanFromPlayer(pl.getUniqueId()), l);
                            pl.playSound(pl.getLocation(), Sound.PISTON_EXTEND, 1F, 1F);
                            ms.showItemBar(pl, "§fDeinen §3Clanort §fwurde hier gesetzt.");
                            pl.closeInventory();
                        }, cancel = p -> p.performCommand("clan");
                        new ConfirmInventory(ms, success, cancel).show(pl, "§0§lClanort setzen", "§aBestätigen", "§cAbbrechen");
                        pl.playSound(pl.getLocation(), Sound.CLICK, 1F, 1F);
                        return;
                    default:
                        break;
                }
            }
            pl.closeInventory();
            toClanBase(pl, db.getClanBase(pl.getUniqueId()), db.getClanFromPlayer(pl.getUniqueId()));
            return;
        } else if (m == Material.SULPHUR) {
            final String clan = db.getClanFromPlayer(pl.getUniqueId());
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            if (clan == null) {
                ms.showItemBar(pl, "§cDu bist in keinem Clan.");
                return;
            }
            deleteClan(pl, pl.getUniqueId());
            pl.closeInventory();
            return;
        } else if (m == Material.STAINED_CLAY) {
            final String clan = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            acceptInvite(pl, clan, rightclick);
            return;
        } else if (m == Material.CAKE) {
            final ItemMeta profile = cinv.getItem(4).getItemMeta();
            final String plname = ChatColor.stripColor(profile.getDisplayName());
            final OfflinePlayer op = ms.getPlayer(plname);
            if (op == null) {
                ms.showItemBar(pl, ms.hnotfound);
                return;
            }
            pl.performCommand("friend " + op.getName());
            pl.closeInventory();
            return;
        } else if (m == Material.WOOD_SWORD) {
            final ItemMeta profile = cinv.getItem(4).getItemMeta();
            final String plname = ChatColor.stripColor(profile.getDisplayName());
            final OfflinePlayer op = ms.getPlayer(plname);
            if (op == null) {
                ms.showItemBar(pl, ms.hnotfound);
                return;
            }
            if (pl.hasMetadata("sfriends")) pl.removeMetadata("sfriends", ms);
            if (pl.hasMetadata("spmenu")) pl.removeMetadata("spmenu", ms);
            if (pl.hasMetadata("sclans")) pl.removeMetadata("sclans", ms);
            pl.setMetadata("sclans", new FixedMetadataValue(ms, plname));
            ms.getPvP().openDuelInv(pl, 0, 2);
            pl.playSound(pl.getLocation(), Sound.CLICK, 1F, 1F);
            return;
        } else if (m == Material.BOOK) {
            final ItemMeta profile = cinv.getItem(4).getItemMeta();
            final String plname = ChatColor.stripColor(profile.getDisplayName());
            final OfflinePlayer op = ms.getPlayer(plname);
            if (op == null) {
                ms.showItemBar(pl, ms.hnotfound);
                return;
            }
            ms.getProfiles().showPlayerProfile(pl, plname, "Clan");
            pl.playSound(pl.getLocation(), Sound.CLICK, 1F, 1F);
            return;
        } else if (m == Material.IRON_BOOTS) {
            final String clan = db.getClanFromPlayer(pl.getUniqueId());
            if (clan == null) {
                ms.showItemBar(pl, "§cDu bist in keinem Clan.");
                return;
            }
            final ItemMeta profile = cinv.getItem(4).getItemMeta();
            final String plname = ChatColor.stripColor(profile.getDisplayName());
            final OfflinePlayer op = ms.getPlayer(plname);
            if (op == null) {
                ms.showItemBar(pl, ms.hnotfound);
                return;
            } else if (db.inOneClan(pl.getUniqueId(), op.getUniqueId()) == false) {
                ms.showItemBar(pl, "§cDieser Spieler ist nicht in deinem Clan.");
                return;
            }
            final UUID uuid = pl.getUniqueId();
            final int rank = db.getClanRank(uuid);
            switch (rank) {
                case -1:
                    ms.showItemBar(pl, "§cDu bist in keinem Clan.");
                    break;
                case 0:
                    ms.showItemBar(pl, "§cDu bist nicht berechtigt, Mitglieder aus dem Clan zu kicken.");
                    break;
                default:
                    final String name = pl.getName();
                    if (op.getName().equalsIgnoreCase(name)) {
                        pl.playSound(pl.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                        break;
                    }
                    final int krank = db.getClanRank(op.getUniqueId());
                    if (rank <= krank) {
                        pl.playSound(pl.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                        break;
                    }
                    final String brmsg = "§3§lMuxClans>§7 Der Spieler " + (krank == 1 ? "§5" : "§9") + op.getName() + " §7wurde von " + (rank == 2 ? "§c" : "§5") + name + " §7aus dem Clan §cgekickt§7.",
                            offmsg = (krank == 1 ? "§5" : "§9") + op.getName() + " §7wurde von " + (rank == 2 ? "§c" : "§5") + name + " §7aus dem Clan §cgekickt§7.";
                    pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                    final Server sr = ms.getServer();
                    final Player ply = op.getPlayer();
                    db.leaveClan(op.getUniqueId());
                    if (ply != null) {
                        ply.sendMessage("§3§lMuxClans>§7 Du wurdest aus dem Clan §cgekickt§7.");
                        ms.sendScoreboard(ply);
                    } else sendOffline(sr, op.getUniqueId(), "§7Du wurdest aus dem Clan §cgekickt§7.");
                    for (final UUID s : db.getClanMembers(db.getClanFromPlayer(uuid))) {
                        final Player ple = sr.getPlayer(s);
                        if (ple != null) ple.sendMessage(brmsg);
                        else sendOffline(sr, s, offmsg);
                    }
                    break;
            }
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            openMenu(pl);
            return;
        } else if (m == Material.NAME_TAG) {
            final String clan = db.getClanFromPlayer(pl.getUniqueId());
            if (clan == null) {
                ms.showItemBar(pl, "§cDu bist in keinem Clan.");
                return;
            }
            final ItemMeta profile = cinv.getItem(4).getItemMeta();
            final String plname = ChatColor.stripColor(profile.getDisplayName());
            final OfflinePlayer op = ms.getPlayer(plname);
            if (op == null) {
                ms.showItemBar(pl, ms.hnotfound);
                return;
            } else if (db.inOneClan(pl.getUniqueId(), op.getUniqueId()) == false) {
                ms.showItemBar(pl, "§cDieser Spieler ist nicht in deinem Clan.");
                return;
            }
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            final int oldrank = db.getClanRank(op.getUniqueId());
            short ranknew = oldrank == 0 ? (short) 1 : (short) 0;
            db.setRank(op.getUniqueId(), ranknew);
            final String msg = "§3§lMuxClans>§7 Der Spieler " + (oldrank == 1 ? "§5" : "§9") + op.getName() + " §7ist " + (ranknew == 1 ? "nun §5Clanmoderator§7." : "wieder §9Clanmitglied§7."),
            offmsg = "§7Du bist " + (ranknew == 1 ? "nun §5Clanmoderator§7." : "wieder §9Clanmitglied§7.");
            final Server sr = ms.getServer();
            if (op.isOnline() == false) sendOffline(sr, op.getUniqueId(), offmsg);
            for (final UUID s : db.getClanMembers(clan)) {
                final Player ple = ms.getServer().getPlayer(s);
                if (ple != null) ple.sendMessage(msg);
            }
            openPlayerMenu(pl, op);
            return;
        } else if (m == Material.ITEM_FRAME) {
            pl.playSound(pl.getLocation(), Sound.CLICK, 1F, 1F);
            if (invname.contains("Einladungen")) {
                openMenu(pl);
                return;
            } else if (invname.contains("Clan löschen")) {
                openPlayerMenu(pl, pl);
                return;
            } else if (invname.contains("Clan weitergeben")) {
                openDeleteMenu(pl);
                return;
            } else if (item.getItemMeta().getDisplayName().contains("Stats")) {
                showTopClans(pl);
                return;
            } else if (item.getItemMeta().getDisplayName().contains("(")) {
                final String pname = item.getItemMeta().getDisplayName().replace("(", "").replace(")", "").replace(ms.getLang("back"), "").replace(" ", "");
                ms.getProfiles().showPlayerProfile(pl, (pname.contains("Mein") ? pl.getName() : pname), "CACHE");
                return;
            }
            openMenu(pl);
            return;
        } else if (im.hasLore() && im.getLore().removeIf(s -> s.toLowerCase().contains("klick")) == false) {
            pl.playSound(pl.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        }
        pl.playSound(pl.getLocation(), Sound.CLICK, 1F, 1F);
        if (m == Material.FEATHER) {
            openInvites(pl);
        } else if (m == Material.GOLD_INGOT) {
            showTopClans(pl);
        } else if (m == Material.COMPASS) {
            pl.playSound(pl.getLocation(), Sound.CLICK, 1F, 1F);
            new MuxAnvil(ms, (input, player) -> {
                showClanInfos(player, db.getClanCase(ms.retardCleaner(input, "Name des Clans: ")), false);
                player.playSound(player.getLocation(), Sound.CLICK, 1F, 1F);
            }).show(pl, "Name des Clans: ", new ItemStack(Material.CHAINMAIL_HELMET));
        } else if (m == Material.DIAMOND_SWORD) {
            final String cname = invname.split(" \\| ")[1];
            switch (db.getClanRank(pl.getUniqueId())) {
                case -1:
                    pl.closeInventory();
                    ms.showItemBar(pl, "§cDu bist in keinem Clan.");
                    break;
                case 2:
                    if (pl.hasMetadata("clanwar")) pl.removeMetadata("clanwar", ms);
                    pl.setMetadata("clanwar", new FixedMetadataValue(ms, cname));
                    pl.performCommand("cw");
                    break;
                default:
                    pl.closeInventory();
                    ms.showItemBar(pl, "§cDu bist nicht berechtigt, einen Clanwar zu starten.");
                    break;
            }
        } else if (m == Material.REDSTONE_BLOCK) {
            final PlayerAction confirm = player -> {
                final UUID uuid = player.getUniqueId();
                switch (db.getClanRank(uuid)) {
                    case -1:
                        ms.showItemBar(player, "§cDu bist in keinem Clan.");
                        break;
                    case 2:
                        deleteClan(pl, uuid);
                        break;
                    default:
                        final int rank = db.getClanRank(player.getUniqueId());
                        final String brmsg = "§3§lMuxClans>§7 Der Spieler " + (rank == 1 ? "§5" : "§9") + player.getName() + " §7hat den Clan §cverlassen§7.",
                        offmsg = (rank == 1 ? "§5" : "§9") + player.getName() + " §7hat den Clan §cverlassen§7.";
                        final Server sr = ms.getServer();
                        for (final UUID s : db.getClanMembers(db.getClanFromPlayer(uuid))) {
                            final Player ply = sr.getPlayer(s);
                            if (ply != null) {
                                ply.sendMessage(brmsg);
                            } else sendOffline(sr, s, offmsg);
                        }
                        db.leaveClan(uuid);
                        ms.sendScoreboard(player);
                        break;
                }
                player.closeInventory();
            }, cancel = player -> openPlayerMenu(player, player);
            new ConfirmInventory(ms, confirm, cancel).show(pl, "§0§lClan verlassen", "§aBestätigen", "§cAbbrechen");
        } else if (m == Material.EMERALD_BLOCK) {
            if (pl.hasPermission("muxsystem.teamclanonly") && ms.isTrusted(pl.getName()) == false) {
                ms.showItemBar(pl, "§cDu kannst keinen Clan erstellen.");
                return;
            }
            new MuxAnvil(ms, (input, player) -> {
                Location l = player.getLocation();
                player.closeInventory();
                final boolean invalid = l.getWorld().getName().equals(ms.getBase().getWorld().getName()) || l.getWorld().equals(ms.getWarpsWorld()) || l.getWorld().getEnvironment() == Environment.THE_END;
                if (invalid) {
                    l = ms.getGameSpawn().clone();
                }
                final String clan = ms.retardCleaner(input, "Name des Clans: ").trim().replaceAll("[^a-zA-Z\\d $!?_-]", "");
                if (clanNameAllowed(player, clan) == false) {
                    return;
                } else if (db.getClanFromPlayer(player.getUniqueId()) != null) {
                    ms.showItemBar(player, "§cDu musst deinen jetzigen Clan verlassen, um einen neuen zu gründen.");
                } else if (db.createClan(clan, l)) {
                    db.addPlayerToClan(player.getUniqueId(), clan, 2, false);
                    ms.sendScoreboard(player);
                    ms.showItemBar(player, "§fDein Clan §3" + clan + " §fwurde erfolgreich §aerstellt§f.");
                    player.playSound(l, Sound.NOTE_PLING, 1F, 1.6F);
                    if (invalid == false) player.sendMessage("§3§lMuxClans>§7 Der Clanort wurde auf §3deiner Position §7gesetzt.");
                    return;
                } else {
                    ms.showItemBar(player, "§cEin Clan mit diesem Name existiert schon.");
                }
                player.playSound(player.getLocation(), Sound.NOTE_BASS, 1F, 1F);
            }).show(pl, "Name des Clans: ", new ItemStack(Material.CHAINMAIL_HELMET));
        } else if (m == Material.GOLD_HELMET && invname.contains("löschen")) {
            final String clan = db.getClanFromPlayer(pl.getUniqueId());
            if (clan == null) {
                ms.showItemBar(pl, "§cDu bist in keinem Clan.");
                return;
            }
            final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxClans§0 | Clan weitergeben");
            inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
            inv.setItem(4, ms.renameItemStack(new ItemStack(Material.GOLD_HELMET), "§3§lClan weitergeben", "§7Verlasse deinen Clan und bestimme", "§7jemanden als deinen Nachfolger."));
            final List<UUID> molist = db.getRankMembers(clan, 1), melist = db.getRankMembers(clan, 0);
            short pos = 18;
            final Server srv = ms.getServer();
            for (final UUID uuid : molist) {
                final OfflinePlayer off = srv.getOfflinePlayer(uuid);
                final boolean online = off.isOnline();
                final MuxUser u = off.isOnline() ? ms.getMuxUser(off.getName()) : db.loadPlayer(uuid);
                final int trophies = (u == null ? 0 : u.getTrophies());
                final boolean pvpbanned = (u != null && u.isPvPbanned()), unranked = (u == null || u.getLastRankedMatch() < System.currentTimeMillis() - 172800000L);
                inv.setItem(pos, ms.renameItemStack(online ? ms.getHead(off.getName()) : new ItemStack(Material.SKULL_ITEM), "§f§l" + off.getName(), "§7Rang: §5Clanmoderator", "§7Status: " + (online ? "§aOnline" : "§cOffline"), "§7Trophäen: §b" + (pvpbanned ? "§cAugeschlossen" : unranked ? "§cUnranked" : trophies), "", "§fKlicke§7, um diesen Spieler", "§7deinen Clan zu vergeben."));
                pos++;
            }
            for (final UUID uuid : melist) {
                final OfflinePlayer off = srv.getOfflinePlayer(uuid);
                final boolean online = off.isOnline();
                final MuxUser u = off.isOnline() ? ms.getMuxUser(off.getName()) : db.loadPlayer(uuid);
                final int trophies = (u == null ? 0 : u.getTrophies());
                final boolean pvpbanned = (u != null && u.isPvPbanned()), unranked = (u == null || u.getLastRankedMatch() < System.currentTimeMillis() - 172800000L);
                inv.setItem(pos, ms.renameItemStack(online ? ms.getHead(off.getName()) : new ItemStack(Material.SKULL_ITEM), "§f§l" + off.getName(), "§7Rang: §9Clanmitglied", "§7Status: " + (online ? "§aOnline" : "§cOffline"), "§7Trophäen: §b" + (pvpbanned ? "§cAugeschlossen" : unranked ? "§cUnranked" : trophies), "", "§fKlicke§7, um diesen Spieler", "§7deinen Clan zu vergeben."));
                pos++;
            }
            pl.openInventory(inv);
            ms.setActiveInv(pl.getName(), InvType.CLANS);
        } else if (m == Material.GOLD_HELMET || m == Material.IRON_HELMET || m == Material.LEATHER_HELMET || m == Material.CHAINMAIL_HELMET) {
            final String display = item.getItemMeta().getDisplayName();
            if (display.contains("#") == false) {
                pl.playSound(pl.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            final String clan = ChatColor.stripColor(display.split(" ")[1]);
            showClanInfos(pl, db.getClanCase(clan), true);
        } else if (m == Material.BOOK_AND_QUILL) {
            new MuxAnvil(ms, (input, player) -> {
                Location l = player.getLocation();
                player.closeInventory();
                final String oldclan = db.getClanFromPlayer(player.getUniqueId());
                final String clan = ms.retardCleaner(input, "Name des Clans: ").trim().replaceAll("[^a-zA-Z\\d $!?_-]", "");
                if (clanNameAllowed(player, clan) == false) {
                    return;
                } else if (db.renameClan(oldclan, clan)) {
                    ms.sendScoreboard(player);
                    final Server sr = ms.getServer();
                    final List<UUID> clanMembers = db.getClanMembers(clan);
                    for (final UUID s : clanMembers) {
                        ms.sendScoreboard(sr.getPlayer(s));
                    }
                    ms.showItemBar(player, "§fDein Clan §3" + clan + " §fwurde erfolgreich §aumbenannt§f.");
                    player.playSound(l, Sound.NOTE_PLING, 1F, 1.6F);
                    return;
                } else {
                    ms.showItemBar(player, "§cEin Clan mit diesem Name existiert schon.");
                }
                player.playSound(l, Sound.NOTE_BASS, 1F, 1F);
            }).show(pl, "Name des Clans: ", new ItemStack(Material.CHAINMAIL_HELMET));
        } else if (m == Material.TNT) {
            if (ms.isTrusted(pl.getName()) && cinv.getSize() > 36) {
                final String cname = invname.split(" \\| ")[1];
                final String clan = db.getClanCase(cname);
                pl.playSound(pl.getLocation(), Sound.CLICK, 1F, 1F);
                if (clan == null) {
                    ms.showItemBar(pl, "§cEs gibt diesen Clan nicht.");
                    return;
                }
                final PlayerAction success = p -> {
                    final String c = db.getClanCase(cname);
                    if (c == null) {
                        ms.showItemBar(pl, "§cEs gibt diesen Clan nicht.");
                        return;
                    }
                    final Server sr = ms.getServer();
                    final String delmsg = "§3§lMuxClans>§7 Das §4§l" + TEAM_CLAN + "§7 hat den Clan §caufgelöst§7.",
                            offmsg = "§7Das §4§l" + TEAM_CLAN + "§7 hat den Clan §caufgelöst§7.";
                    for (final UUID s : db.getClanMembers(c)) {
                        final Player player = sr.getPlayer(s);
                        if (player != null) {
                            player.sendMessage(delmsg);
                        } else sendOffline(sr, s, offmsg);
                    }
                    db.deleteClan(c);
                    for (final Player player : sr.getOnlinePlayers()) {
                        if (player.isOp()) {
                            player.sendMessage("§3§lMuxClans>§c §l" + pl.getName() + " §7hat den Clan §3§l" + c + "§7 gelöscht.");
                        }
                    }
                    ms.getHistory().addHistory(pl.getUniqueId(), null, "TEAMACTION", "CLAN DELETE", c);
                    pl.closeInventory();
                }, cancel = p -> showClanInfos(p, clan, false);
                new ConfirmInventory(ms, success, cancel).show(pl, "§0§lClan " + clan + " löschen", "§aBestätigen", "§cAbbrechen");
                return;
            }
            openDeleteMenu(pl);
        } else {
            pl.closeInventory();
        }
    }
    private boolean clanNameAllowed(final Player player, final String clan) {
        for (final String rank : ms.getPerms().getGroupNames()) {
            if (clan.equals(rank)) {
                player.playSound(player.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                ms.showItemBar(player, "§cDu kannst keinen Rangnamen als Clan benutzen.");
                return false;
            }
        }
        if (clan.equalsIgnoreCase("ort") || clan.equalsIgnoreCase("base") || clan.equalsIgnoreCase("help") || clan.equalsIgnoreCase("hilfe")) {
            ms.showItemBar(player, "§cDieser Name ist nicht erlaubt.");
            return false;
        } else if ((clan.length() < 2 || clan.length() > 5) && clan.equals(TEAM_CLAN) == false) {
            ms.showItemBar(player, "§cEin Name muss §62-5 §cZeichen haben.");
            return false;
        }
        return true;
    }
    private void sendOffline(final Server sr, final UUID uuid, final String msg) {
        final OfflinePlayer ply = sr.getOfflinePlayer(uuid);
        if (ply != null) ms.getChat().sendOfflineMessage(ply, "MuxClans", msg, true);
    }
    private void openDeleteMenu(final Player pl) {
        final String clan = db.getClanFromPlayer(pl.getUniqueId());
        if (clan == null) {
            ms.showItemBar(pl, "§cDu bist in keinem Clan.");
            return;
        }
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxClans§0 | Clan löschen");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.TNT), "§3§lClan§c §llöschen", "§7Löse deinen Clan komplett auf oder", "§7gib ihn an jemanden weiter."));
        inv.setItem(20, ms.renameItemStack(new ItemStack(Material.GOLD_HELMET), "§3§lClan weitergeben", "§7Verlasse deinen Clan und bestimme", "§7jemanden als deinen Nachfolger.", "", db.getClanMembers(clan).size() < 2 ? "§cDu hast noch keine Mitglieder." : "§3Klicke§7, um diesen zu bestimmen."));
        inv.setItem(24, ms.renameItemStack(new ItemStack(Material.SULPHUR), "§3§lClan§c §lauflösen", "§7Hiermit entfernst du alle Spieler", "§7im Clan, bist du dir dabei sicher?", "", "§7Klicke, um deinen Clan §caufzulösen§7."));
        pl.openInventory(inv);
        ms.setActiveInv(pl.getName(), InvType.CLANS);
    }
    private void openInvites(final Player pl) {
        final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lMuxClans§0 | Einladungen");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        final List<String> invites = db.getInvitations(pl.getUniqueId());
        if (invites.isEmpty()) {
            pl.playSound(pl.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        }
        final int size = invites.size();
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.FEATHER, size), "§3§l" + size + " Claneinladung" + (size == 1 ? "" : "en") + "!", "§7Nehme eine Einladung an, um", "§7diesen Clan beizutreten."));
        short pos = 18;
        for (final String s : invites) {
            if (pos == 26) break;
            inv.setItem(pos, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY), "§f§l" + s, "§7Dieser Clan möchte, dass", "§7du ihnen beitrittst.", "", "§7Linksklick, um §aanzunehmen", "§7Rechtsklick, um §cabzulehnen"));
            pos++;
        }
        pl.openInventory(inv);
        ms.setActiveInv(pl.getName(), InvType.CLANS);
    }
    private void deleteClan(final Player p, final UUID uuid) {
        final String clan = db.getClanFromPlayer(uuid);
        final String delmsg = "§3§lMuxClans>§7 Der Spieler §c" + p.getName() + " §7hat den Clan §caufgelöst§7.",
        offmsg = "§c" + p.getName() + " §7hat den Clan " + clan + " §caufgelöst§7.";
        final Server sr = ms.getServer();
        final List<UUID> clanMembers = db.getClanMembers(clan);
        db.deleteClan(clan);
        for (final UUID s : clanMembers) {
            final Player player = ms.getServer().getPlayer(s);
            if (player != null) {
                player.sendMessage(delmsg);
                ms.sendScoreboard(player);
            } else sendOffline(sr, s, offmsg);
        }
        ms.showItemBar(p, "§fDein Clan wurde erfolgreich §cgelöscht§f.");
    }

    private void showTopClans(final Player pl) {
        final List<Object[]> ranking = db.getClanTop10();
        byte i = 1;
        short pos = 20;
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxClans§0 | Stats");
        final UUID uuid = pl.getUniqueId();
        inv.setItem(1, ms.renameItemStack(new ItemStack(Material.CHAINMAIL_HELMET), "§3§lAllgemeines", "§7Alle deine Clanmitglieder kannst", "§7du unter anderem hier finden.", "", "§3Klicke§7, um dahin zu wechseln."));
        inv.setItem(4, ms.addGlow(ms.renameItemStack(new ItemStack(Material.GOLD_INGOT), "§3§lStats", "§7Sortiert nach Trophäen sind die", "§7besten Clans hier aufgelistet.")));
        inv.setItem(7, ms.renameItemStack(new ItemStack(Material.COMPASS), "§3§lClan suchen", "§7Alle Mitglieder und Statistiken eines", "§7Clans werden hier aufgelistet.", "", "§7Klicke, um ein §3Clan zu suchen§7."));
        final String clan = db.getClanFromPlayer(uuid);
        boolean ownClan = false;
        if (ranking == null || ranking.isEmpty()) {
            inv.setItem(22, ms.renameItemStack(new ItemStack(Material.BARRIER), "§c§lKein Ranking", "§7Derzeit wurde noch kein Clan", "§7in diesem Ranking eingestuft."));
        } else {
            for (final Object[] c : ranking) {
                final List<UUID> mitglieder = db.getClanMembers((String) c[0]);
                if (c[0].equals(TEAM_CLAN)) continue;

                if (c[0].equals(clan)) {
                    ownClan = true;
                    placeOwnClanItem(inv, clan, pos);
                } else {
                    final ItemStack is = ms.renameItemStack(new ItemStack(i == 1 ? Material.GOLD_HELMET : i == 2 ? Material.IRON_HELMET : i == 3 ? Material.LEATHER_HELMET : Material.CHAINMAIL_HELMET),
                            (i == 1 ? "§e#§l1 §e§l" : i == 2 ? "§f#§l2 §f§l" : i == 3 ? "§6#§l3 §6§l" : "§3#§l" + i + " §3§l") + c[0],
                            "§7Trophäen: §b" + (c[2].equals(-1) ? "§cUnranked" : c[2]), "§7Mitglieder: §9" + mitglieder.size() + "§7/15", "", "§3Klicke§7, um die Infos über", (c[0].equals(clan) ? "§7deinen Clan zu erhalten." : "§7diesen Clan zu erhalten."));
                    inv.setItem(pos, is);
                }
                i++;
                if (pos == 24) pos = 29;
                else pos++;
            }
            if (ownClan == false && clan != null) {
                placeOwnClanItem(inv, clan, pos - 1);
            }
        }
        inv.setItem(44, ms.renameItemStack(new ItemStack(Material.ARMOR_STAND), "§3§lLevels",
                "§6Gold §7(ab 12.500 Trophäen)", "§5Lila §7(ab 7.500 Trophäen)", "§aGrün §7(ab 5.000 Trophäen)", "§3Blau §7(ab 0 Trophäen)"));
        if (ms.getActiveInv(pl.getName()) != InvType.CLANS) pl.closeInventory();
        pl.openInventory(inv);
        ms.setActiveInv(pl.getName(), InvType.CLANS);
    }

    private void placeOwnClanItem(final Inventory inv, final String clan, final int pos) {
        final List<UUID> mit = db.getClanMembers(clan);
        final int ctrophies = db.getClanTrophies(clan), rank = db.getClanRanking(clan);
        final OfflinePlayer powner = ms.getServer().getOfflinePlayer(db.getRankMembers(clan, 2).get(0));
        final String chattext = db.getChatText(powner.getUniqueId());
        inv.setItem(44, ms.renameItemStack(new ItemStack(Material.ARMOR_STAND), "§3§lLevels",
                "§7Level: §4" + (chattext.startsWith("§4") ? "Rot §7(team)" : chattext.startsWith("§6") ? "§6Gold §7(max)" : chattext.startsWith("§5") ? "§5Lila" + (" §7(noch " + (12500 - ctrophies) + ")") : chattext.startsWith("§a") ? "§aGrün" + (" §7(noch " + (7500 - ctrophies) + ")") : "§3Blau" + (" §7(noch " + (5000 - ctrophies) + ")")),
                "", "§6Gold §7(ab 12.500 Trophäen)", "§5Lila §7(ab 7.500 Trophäen)", "§aGrün §7(ab 5.000 Trophäen)", "§3Blau §7(ab 0 Trophäen)"));
        inv.setItem(pos, ms.addGlow(ms.renameItemStack(new ItemStack(Material.CHAINMAIL_HELMET), ("§3#§l" + rank + " §3§l") + clan,
                "§7Trophäen: §b" + (ctrophies == -1 ? "§cUnranked" : ctrophies),
                "§7Mitglieder: §9" + mit.size() + "§7/15",
                "",
                "§3Klicke§7, um die Infos über", "§7deinen Clan zu erhalten."
        )));
    }

    private void showClanInfos(final Player p, final String clan, final boolean top10) {
        if (clan == null) {
            ms.showItemBar(p, "§cDer Clan existiert nicht.");
            return;
        }
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxClans§0 | " + clan);
        addPlayerItemsOfClan(inv, p, clan, false);
        final InvType it = ms.getActiveInv(p.getName());
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back") + (top10 ? " (Stats)" :
                it == InvType.PROFILE ? " (" + p.getOpenInventory().getTopInventory().getTitle().split("§0 \\| ")[1] + ")" : "")));
        if (it != InvType.CLANS && it != InvType.PROFILE && it != InvType.MUXANVIL && it != InvType.CONFIRM && it != InvType.CLANWAR) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.CLANS);
    }

    private void addPlayerItemsOfClan(final Inventory inv, final Player p, final String clan, final boolean own) {
        final Server srv = ms.getServer();
        final OfflinePlayer powner = srv.getOfflinePlayer(db.getRankMembers(clan, 2).get(0));
        final List<UUID> molist = db.getRankMembers(clan, 1), melist = db.getRankMembers(clan, 0);
        final boolean ponline = powner.isOnline() && p.canSee(powner.getPlayer());
        final String poname = powner.getName();
        final MuxUser pu = powner.isOnline() ? ms.getMuxUser(poname) : db.loadPlayer(powner.getUniqueId());
        final int ptrophies = (pu == null ? 0 : pu.getTrophies());
        final boolean ppvpbanned = (pu != null && pu.isPvPbanned()), punranked = (pu == null || pu.getLastRankedMatch() < System.currentTimeMillis() - 172800000L);
        final ItemStack pitem = ms.renameItemStack(ponline ? ms.getHead(poname) : new ItemStack(Material.SKULL_ITEM), "§f§l" + poname, "§7Rang: §cClanadmin",
                "§7Status: " + (ponline ? "§aOnline" : "§cOffline"), "§7Trophäen: §b" + (ppvpbanned ? "§cAusgeschlossen" : punranked ? "§cUnranked" : ptrophies));
        inv.setItem(18, own ? ms.addLore(pitem, "", "§7Klicke, um mehr Infos zu erhalten.") : pitem);
        short pos = 19;
        for (final UUID uuid : molist) {
            final OfflinePlayer d = srv.getOfflinePlayer(uuid);
            final boolean online = d.isOnline() && p.canSee(d.getPlayer());
            final String moname = d.getName();
            final MuxUser u = d.isOnline() ? ms.getMuxUser(moname) : db.loadPlayer(d.getUniqueId());
            final int trophies = (u == null ? 0 : u.getTrophies());
            final boolean pvpbanned = (u != null && u.isPvPbanned()), unranked = (u == null || u.getLastRankedMatch() < System.currentTimeMillis() - 172800000L);
            final ItemStack item = ms.renameItemStack(online ? ms.getHead(moname) : new ItemStack(Material.SKULL_ITEM), "§f§l" + moname, "§7Rang: §5Clanmoderator",
                    "§7Status: " + (online ? "§aOnline" : "§cOffline"), "§7Trophäen: §b" + (pvpbanned ? "§cAusgeschlossen" : unranked ? "§cUnranked" : trophies));
            inv.setItem(pos, own ? ms.addLore(item, "", "§7Klicke, um mehr Infos zu erhalten.") : item);
            pos++;
        }
        for (final UUID uuid : melist) {
            if (pos == 45) break;
            final OfflinePlayer d = srv.getOfflinePlayer(uuid);
            final boolean online = d.isOnline() && p.canSee(d.getPlayer());
            final String mename = d.getName();
            final MuxUser u = d.isOnline() ? ms.getMuxUser(mename) : db.loadPlayer(d.getUniqueId());
            final int trophies = (u == null ? 0 : u.getTrophies());
            final boolean pvpbanned = (u != null && u.isPvPbanned()), unranked = (u == null || u.getLastRankedMatch() < System.currentTimeMillis() - 172800000L);
            final ItemStack item = ms.renameItemStack(online ? ms.getHead(mename) : new ItemStack(Material.SKULL_ITEM), "§f§l" + mename, "§7Rang: §9Clanmitglied",
                    "§7Status: " + (online ? "§aOnline" : "§cOffline"), "§7Trophäen: §b" + (pvpbanned ? "§cAusgeschlossen" : unranked ? "§cUnranked" : trophies));
            inv.setItem(pos, own ? ms.addLore(item, "", "§7Klicke, um mehr Infos zu erhalten.") : item);
            pos++;
        }
        if (own) {
            final UUID uuid = p.getUniqueId();
            final List<UUID> members = db.getClanMembersOfPlayer(uuid);
            if (db.getClanRank(uuid) > 0 && (members == null || members.size() < 15)) {
                final ItemStack einladen = ms.renameItemStack(plusHead.clone(), "§3§lSpieler einladen§5 [CLANMOD]", "§7Vergrößer deinen Clan, indem", "§7du neue Spieler hineinbringst.", "", "§3Klicke§7, um diese zu einladen.");
                inv.setItem(pos, einladen);
            }
        } else {
            final List<UUID> mitglieder = db.getClanMembers(clan);
            final int ctrophies = db.getClanTrophies(clan), rank = db.getClanRanking(clan);
            final String chattext = db.getChatText(powner.getUniqueId()), level = "§7Level: §4" + (chattext.startsWith("§4") ? "Rot §7(team)" : chattext.startsWith("§6") ? "§6Gold §7(max)" : chattext.startsWith("§5") ? "§5Lila" + (" §7(noch " + (12500 - ctrophies) + ")") : chattext.startsWith("§a") ? "§aGrün" + (" §7(noch " + (7500 - ctrophies) + ")") : "§3Blau" + (" §7(noch " + (5000 - ctrophies) + ")"));
            final String[] s = clan.equals(TEAM_CLAN) ? new String[]{"§7Trophäen: §b" + (ctrophies == -1 ? "§cUnranked" : ctrophies), level,
                    "§7Mitglieder: §9" + mitglieder.size() + "§7/15"}
                    : new String[]{"§7Trophäen: §b" + (ctrophies == -1 ? "§cUnranked" : ctrophies), level,
                    "§7Mitglieder: §9" + mitglieder.size() + "§7/15",
                    "§7Platz: " + (rank == 1 ? "§e#" : rank == 2 ? "§f#" : rank == 3 ? "§6#" : "§3#") + rank};
            inv.setItem(4, ms.renameItemStack(new ItemStack(Material.CHAINMAIL_HELMET), "§3§l" + clan, s));
            final int clanrank = db.getClanRank(p.getUniqueId());
            final List<UUID> members = db.getClanMembersOfPlayer(p.getUniqueId());
            if (clanrank == 2 && ms.getDB().getClanFromPlayer(p.getUniqueId()).equals(clan) == false) inv.setItem(8, ms.renameItemStack(new ItemStack(Material.DIAMOND_SWORD), "§3§lClanwar erstellen§c [CLANADMIN]", "§7Kämpfe mit deinem Clan", "§7gegen diesen Clan.",
                    "", members == null || members.size() < 2 ? "§cDu hast noch keine Mitglieder." : "§3Klicke§7, um einen zu beginnen."));
            final boolean trusted = ms.isTrusted(p.getName());
            if (p.isOp()) {
                inv.setItem(trusted ? 43 : 44, ms.renameItemStack(new ItemStack(Material.WORKBENCH), "§c§lClanort", "§7Der Ort von den vertraulichen", "§7Clanmitgliedern liegt dort.", "", "§cKlicke§7, um dorthin zu gelangen."));
                if (trusted) inv.setItem(44, ms.renameItemStack(new ItemStack(Material.TNT), "§c§lClan löschen", "§7Löse diesen Clan komplett auf", "§7wenn er die Spielregeln verstößt.", "", "§cKlicke§7, um ihn zu löschen."));
            }
        }
    }

    private void acceptInvite(final Player p, final String clan, final boolean rightclick) {
        if (db.getClanFromPlayer(p.getUniqueId()) != null) {
            p.closeInventory();
            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
            ms.showItemBar(p, "§cDu musst zuerst deinen Clan verlassen.");
            return;
        } else if (p.hasPermission("muxsystem.teamclanonly") && clan.equals(TEAM_CLAN) == false) {
            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
            ms.showItemBar(p, "§cDu kannst keine anderen Clans beitreten.");
            return;
        }
        if (db.isInvited(p.getUniqueId(), clan)) {
            if (rightclick) {
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                db.deleteInvitation(p.getUniqueId(), clan);
                if (db.getInvitations(p.getUniqueId()).isEmpty()) {
                    openMenu(p);
                    return;
                }
                openInvites(p);
                return;
            }
            final List<UUID> members = db.getClanMembers(db.getClanCase(clan));
            if (members.size() >= 15 && clan.equals(TEAM_CLAN) == false) {
                p.closeInventory();
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                ms.showItemBar(p, "§cDer Clan hat die maximale Anzahl an Spielern erreicht.");
                return;
            }
            db.deleteInvitations(p.getUniqueId());
            db.leaveClan(p.getUniqueId());
            joinClan(p, clan, members.size());
        } else {
            ms.showItemBar(p, "§cDu wurdest von diesem Clan nicht eingeladen.");
        }
    }

    public void joinClan(final Player p, final String clan, final int members) {
        if (db.addPlayerToClan(p.getUniqueId(), clan, 0, false) == false) {
            p.closeInventory();
            p.sendMessage("§cEs ist ein Fehler aufgetreten. (MuxClans -1)");
            System.err.println("MuxSystem> An error occured. (MuxClans -1)");
            return;
        }
        p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 2F);
        final Server sr = ms.getServer();
        final String joinmsg = "§3§lMuxClans>§7 Der Spieler §9" + p.getName() + " §7ist dem Clan §abeigetreten§7.",
                offmsg = "§9" + p.getName() + "§7 ist dem Clan §abeigetreten§7.";
        for (final UUID s : db.getClanMembers(db.getClanFromPlayer(p.getUniqueId()))) {
            final Player pl = sr.getPlayer(s);
            if (pl != null) {
                pl.sendMessage(joinmsg);
            } else sendOffline(sr, s, offmsg);
        }
        final Player admin = ms.getServer().getPlayer(db.getRankMembers(clan, 2).get(0));
        if (admin != null && members == 2) {
            admin.sendMessage("§3§lMuxClans>§7 Benutze §3#nachricht §7um im ClanChat zu schreiben.");
        }
        p.sendMessage("§3§lMuxClans>§7 Benutze §3#nachricht §7um im ClanChat zu schreiben.");
        ms.sendScoreboard(p);
        ms.showItemBar(p, "§fDu bist dem Clan erfolgreich §abeigetreten§f.");
        openMenu(p);
    }

    private void toClanBase(final Player p, final Location base, final String cname) {
        if (ms.hasVoted(p) == false) {
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            ms.sendNoVoteMessage(p);
            return;
        }
        final UUID uuid = p.getUniqueId();
        final String clan = db.getClanFromPlayer(uuid);
        final boolean staff = clan != null && clan.equalsIgnoreCase(TEAM_CLAN);
        if (base == null) {
            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
            ms.showItemBar(p, staff ? "§cDer Clanort existiert nicht." : "§cDu bist in keinem Clan.");
            return;
        } else if (ms.inBattle(p.getName(), p.getLocation())) {
            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
            ms.showItemBar(p, ms.hnotinfight);
            return;
        }
        if (db.getClanRank(uuid) > 0 || staff) {
            if (p.hasPermission("muxsystem.notpcooldown")) {
                ms.setLastLocation(p);
                ms.teleportPlayer(p, base);
                ms.showItemBar(p, "§fDu wurdest zum §3Clanort §fteleportiert.");
            } else if (ms.startTPCountdown(p, base)) {
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                ms.showItemBar(p, "§fTeleportiere zum §3Clanort§f...");
            }
            final int x = base.getBlockX(), y = base.getBlockY(), z = base.getBlockZ();
            if (staff) {
                ms.getHistory().addHistory(p.getUniqueId(), null, "TELEPORT", "Clanbase (" + cname + ")", base.getWorld().getName() + " " + x + " " + y + " " + z);
            } else {
                ms.getHistory().addHistory(p.getUniqueId(), null, "TELEPORT", "Clanbase", base.getWorld().getName() + " " + x + " " + y + " " + z);
            }
        } else {
            ms.showItemBar(p, "§cDu hast keine Berechtigung auf den Clanort.");
        }
    }

    private void invitePlayer(final Player p, final String name) {
        if (name.equalsIgnoreCase(p.getName())) {
            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
            ms.showItemBar(p, "§cDu kannst dich nicht selbst einladen.");
            return;
        }
        final UUID uuid = p.getUniqueId();
        final int rank = db.getClanRank(uuid);
        switch (rank) {
            case -1:
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                ms.showItemBar(p, "§cDu bist in keinem Clan.");
                break;
            case 0:
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                ms.showItemBar(p, "§cDu bist nicht berechtigt, neue Mitglieder in den Clan einzuladen.");
                break;
            default:
                final Player pl = ms.getServer().getPlayer(name);
                if (pl == null || p.canSee(pl) == false) {
                    ms.showItemBar(p, ms.hnotonline);
                    p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                    return;
                } else if (db.getClanFromPlayer(pl.getUniqueId()) != null) {
                    ms.showItemBar(p, "§cDieser Spieler ist bereits in einem Clan.");
                    p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                    return;
                }
                final String clan = db.getClanFromPlayer(uuid);
                final List<UUID> members = db.getClanMembers(clan);
                if (members.size() >= 15 && clan.equalsIgnoreCase(TEAM_CLAN) == false) {
                    ms.showItemBar(p, "§cDieser Clan hat die maximale Anzahl an Spielern erreicht.");
                    p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                    return;
                } else if (db.isInvited(pl.getUniqueId(), clan)) {
                    ms.showItemBar(p, "§cDieser Spieler wurde bereits eingeladen.");
                    p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                    return;
                }
                final String invitemsg = "§3§lMuxClans>§7 Der Spieler §9" + pl.getName() + " §7wurde von §5" + p.getName() + " §7zum Clan §feingeladen§7.",
                        offmsg = "§f" + pl.getName() + "§7 wurde von §5" + p.getName()  + " §7zum Clan §feingeladen§7.";
                final Server sr = ms.getServer();
                if (rank != 2) {
                    final OfflinePlayer powner = ms.getServer().getOfflinePlayer(db.getRankMembers(clan, 2).get(0));
                    final Player plowner = powner.getPlayer();
                    if (plowner != null) plowner.sendMessage(invitemsg);
                    else sendOffline(sr, powner.getUniqueId(), offmsg);
                }
                db.addPlayerToClan(pl.getUniqueId(), clan, 0, true);
                pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                ms.chatClickHoverRun(pl, "§3§lMuxClans>§7 Du wurdest von §3" + p.getName() + " §7zu " + db.getChatText(uuid) + "§7 eingeladen.", "§3§oKlicke, um anzunehmen.", "/c accept " + clan);
                ms.chatClickHoverRun(pl, "§3§lMuxClans>§7 Klicke §3hier§7, um diese Anfrage anzunehmen.", "§3§oKlicke, um anzunehmen.", "/c accept " + clan);
                ms.showItemBar(p, "§fDer Spieler §3" + pl.getName() + " §fwurde zum Clan §aeingeladen§f.");
                break;
        }
    }

    public boolean handleCommand(final Player p, final String[] args) {
        if (args.length == 0) {
            openMenu(p);
            return true;
        } else if (args.length == 1 && (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("hilfe"))) {
            p.sendMessage(new String[]{
                    ms.header("CLANS", (byte) 10, "§3"),
                    "  §6/c [clan]: §7Mitglieder sehen",
                    "  §6/c einladen [spieler]: §7Spieler einladen",
                    "  §6/c ort: §7Zum Clanort",
                    "  §6#[nachricht]: §7Clanchat",
                    ms.footer((byte) 10, (byte) 5, "§3")
            });
            return true;
        } else if (args[0].equalsIgnoreCase("accept")) {
            if (args.length != 2) {
                ms.showItemBar(p, ms.usage("/c accept [clan]"));
                return true;
            }
            acceptInvite(p, args[1], false);
            return true;
        } else if (args[0].equalsIgnoreCase("einladen") || args[0].equalsIgnoreCase("invite")) {
            if (args.length != 2) {
                ms.showItemBar(p, ms.usage("/c einladen [spieler]"));
                return true;
            }
            invitePlayer(p, args[1]);
            return true;
        } else if (args[0].equalsIgnoreCase("base") || args[0].equalsIgnoreCase("ort")) {
            toClanBase(p, db.getClanBase(p.getUniqueId()), db.getClanFromPlayer(p.getUniqueId()));
            return true;
        }
        final String clan = db.getClanCase(args[0]);
        showClanInfos(p, clan, false);
        if (clan == null) ms.showItemBar(p, "§cDieser Clanbefehl ist unbekannt.");
        if (ms.checkGeneralCooldown(p.getName(), "CLAN", 60000L, true)) return true;
        ms.chatClickHoverRun(p, "§3§lMuxClans>§7 Klicke §3hier§7, für die Liste aller Clanbefehle.", "§3§oKlicke um die Liste zu sehen", "/c help");
        return true;
    }
}