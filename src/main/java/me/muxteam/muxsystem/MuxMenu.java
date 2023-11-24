package me.muxteam.muxsystem;

import me.muxteam.base.PlayerBase;
import me.muxteam.basic.ConfirmInventory;
import me.muxteam.basic.MuxActions;
import me.muxteam.basic.MuxAnvil;
import me.muxteam.extras.MuxExtraUser;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxteam.MuxTeam;
import me.muxteam.pvp.MuxPvP;
import me.muxteam.ranks.MuxBenefits;
import me.muxteam.ranks.MuxRanks;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MuxMenu {
    private final MuxSystem ms;
    private final Set<Player> forcefield = new HashSet<>();
    private final MuxBenefits benefits;

    public MuxMenu(final MuxSystem ms) {
        this.ms = ms;
        this.benefits = new MuxBenefits(ms);
    }

    public MuxBenefits getBenefits() {
        return benefits;
    }

    public void handleQuit(final Player p) {
        forcefield.remove(p);
    }

    public boolean hasForcefield(final Player p) {
        return forcefield.contains(p);
    }

    public void disableForcefield(final Player p) {
        forcefield.remove(p);
    }

    public boolean handleCommand(final Player p, final String[] args) {
        final boolean _1vs1 = ms.in1vs1(p);
        if (ms.inBattle(p.getName(), p.getLocation()) && _1vs1 == false) {
            ms.showItemBar(p, ms.hnotinfight);
            return true;
        } else if (args.length > 0 && args[0].contains("settings")) {
            openSettings(p);
            return true;
        }
        final InvType it = ms.getActiveInv(p.getName());
        if (it != InvType.MENU && it != InvType.HOMES && it != InvType.FRIENDS && it != InvType.EXTRAS && it != InvType.PROFILE && it != InvType.TEAM && it != InvType.SUPPORT && it != InvType.ADMIN)
            p.closeInventory();
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxMenü");
        if (ms.inEvent(p)) {
            inv.setItem(22, ms.renameItemStack(new ItemStack(Material.LEATHER_BOOTS), "§d§lEvent verlassen", "", "§7Klicke, um das Event zu §dverlassen§7."));
            p.openInventory(inv);
            ms.setActiveInv(p.getName(), InvType.MENU);
            return true;
        } else if (_1vs1) {
            inv.setItem(22, ms.renameItemStack(new ItemStack(Material.GOLD_SWORD), "§c§lKampf aufgeben", "", "§7Klicke, um das 1vs1 zu §cverlassen§7."));
            p.openInventory(inv);
            ms.setActiveInv(p.getName(), InvType.MENU);
            return true;
        } else if (ms.inGame(p)) {
            inv.setItem(22, ms.renameItemStack(new ItemStack(Material.LEATHER_CHESTPLATE), "§f§lTraining verlassen", "", "§7Klicke, um das Training zu §fverlassen§7."));
            p.openInventory(inv);
            ms.setActiveInv(p.getName(), InvType.MENU);
            return true;
        }
        if (ms.hasVoted(p) == false && ms.getNewbies().isNewbie(p) == false) {
            p.performCommand("vote");
            return true;
        }
        final MuxUser u = ms.getMuxUser(p.getName());
        final UUID uuid = p.getUniqueId();
        final ItemStack spawn = ms.renameItemStack(new ItemStack(Material.BEACON), "§f§lSpawn", "§7Hiermit gelangst du direkt zu dem", "§7Hauptpunkt des ganzen Servers.", "", "§7Klicke§7, um dich zu §fteleportieren§7."),
                shop = ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT), "§e§lShop", "§7Kaufe oder verkaufe hier nahezu", "§7alle Items mithilfe von MuxCoins.", "", "§7Klicke§7, um dich zu §eteleportieren§7."),
                settings = ms.renameItemStack(new ItemStack(Material.REDSTONE_COMPARATOR), "§f§lEinstellungen", "§7Es lassen sich verschiedene", "§7Optionen hier festlegen.", "", "§fKlicke§7, um diese zu verändern."),
                friends = ms.renameItemStack(new ItemStack(Material.CAKE), "§e§lFreunde", "§7Hier siehst du die Spieler, mit", "§7denen du befreundet §7bist.", "", "§eKlicke§7, um diese zu sehen.");
        final List<PlayerBase> trusted = ms.getBase().getTrusted(uuid).stream().filter(base -> base.getOwner().equals(uuid) == false).collect(Collectors.toList());
        final int homes = u.getHomes().size(), places = homes + trusted.size();
        final MuxExtraUser extraUser = ms.getExtras().getExtraUser(uuid);
        final boolean extras = extraUser.getPerks().getOwned().isEmpty() == false || extraUser.getExtraCMDs().isEmpty() == false || extraUser.getEmojis().isEmpty() == false || extraUser.getPets().getOwned().isEmpty() == false || extraUser.getMounts().getOwned().isEmpty() == false,
                ownBase = ms.getBase().hasBase(uuid);
        final ItemStack base = ms.renameItemStack(new ItemStack(Material.SAPLING, 1, (short) 0), "§a§lBase", ownBase ? new String[]{
                "§7Mit dieser Funktion gelangst du",
                "§7jederzeit zurück zu der Basis.",
                "",
                "§7Klicke, um dich zu §ateleportieren§7."
        } : new String[]{
                "§7Die Basis ist der sicherste Ort,",
                "§7um ungestört bauen zu können.",
                "",
                "§7Klicke, um diese zu §agenerieren§7."
        });
        if (places == 0) {
            inv.setItem(10, base);
            inv.setItem(13, spawn);
            inv.setItem(16, shop);
        } else {
            ItemStack place;
            if (homes == 0 && trusted.size() == 1) {
                final PlayerBase trustedBase = trusted.get(0);
                final OfflinePlayer op = ms.getServer().getOfflinePlayer(trustedBase.getOwner());
                place = ms.renameItemStack(new ItemStack(Material.SAPLING, 1, (short) 1), "§2§lBase von " + op.getName(), "§7Erreiche somit die Basis deines", "§7Kollegen, der dir Zugang gab.", "", "§7Klicke, um dich zu §2teleportieren§7.");
            } else if (homes == 1 && trusted.isEmpty()) {
                final short maxHomes = (short) (p.hasPermission("muxsystem.morehomes.unlimited") ? 100 : p.hasPermission("muxsystem.morehomes.elite") ? 10 : p.hasPermission("muxsystem.morehomes.vip") ? 5 : 1);
                if (maxHomes != 1) {
                    place = ms.renameItemStack(new ItemStack(Material.DARK_OAK_DOOR_ITEM), "§6§lHome", "§7Erreiche somit deinen ausgewählten", "§7Punkt in der gesamten freien Welt.", "", "§7Linksklick, um dich zu §fteleportieren§7.", "§7Rechtsklick, um ein §aneues zu setzen§7.");
                } else {
                    place = ms.renameItemStack(new ItemStack(Material.DARK_OAK_DOOR_ITEM), "§6§lHome", "§7Erreiche somit deinen ausgewählten", "§7Punkt in der gesamten freien Welt.", "", "§7Linksklick, um dich zu §fteleportieren§7.", "§7Rechtsklick, um ihn §eumzusetzen§7.");
                }
            } else {
                place = ms.renameItemStack(new ItemStack(Material.COMPASS), "§f§lOrte", "§7Alle deine Homes und Teambasen", "§7gehören zu dieser Kategorie.", "", "§fKlicke§7, um die Liste zu sehen.");
            }
            inv.setItem(10, base);
            inv.setItem(12, place);
            inv.setItem(14, spawn);
            inv.setItem(16, shop);
        }
        if (p.hasPermission("muxsystem.team")) {
            final MuxRanks.PermissionsGroup group = ms.getPerms().getGroupOf(uuid);
            final String gname = group == null ? "" : group.getName().toLowerCase();
            if (gname.contains("dev")) {
                inv.setItem(28, ms.renameItemStack(new ItemStack(Material.PISTON_BASE), "§3§lTestserver",
                        "§7Schaue hierauf, ob deine Plugins", "§7absolut fehlerfrei funktionieren.", "", "§7Klicke, um diesen §3beizutreten§7."));
            } else if (gname.contains("build")) {
                inv.setItem(28, ms.renameItemStack(new ItemStack(Material.BRICK), "§3§lBauserver",
                        "§7Erstelle und plane hierauf neue", "§7Konstruktionen für die Zukunft.", "", "§7Klicke, um diesen §3beizutreten§7."));
            } else if (p.isOp() || gname.contains("admin") || gname.contains("owner") || ms.isTrusted(p.getName())) {
                inv.setItem(28, ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 1), "§c§lAdministration",
                        "§7Manage den Server, damit dieser", "§7absolut einwandfrei funktioniert.", "", "§cKlicke§7, um diese zu sehen."));
            } else {
                final int supports = ms.getSupport().getSupportsNeeded(p.isOp());
                inv.setItem(28, ms.renameItemStack(new ItemStack(Material.SLIME_BALL, Math.max(1, supports)), "§a§lSupports" + (supports > 0 ? " §7» §a" + supports : ""),
                        "§7Hilf anderen Spielern, indem du", "§7ihre Fragen und Probleme löst.", "", "§aKlicke§7, um diese zu sehen."));
            }
            inv.setItem(30, ms.renameItemStack(new ItemStack(Material.WATER_LILY), "§f§lMuxTeam", "§7Du findest in dem Bereich alle", "§7wichtigen Infos und Aufgaben.", "", "§7Klicke, um diese zu §fsehen§7."));
            inv.setItem(31, friends);
            inv.setItem(32, ms.renameItemStack(new ItemStack(Material.DIAMOND), "§b§lVorteile", "§7Alle Funktionen, die dein Rang", "§7bietet, siehst du hierdurch.", "", "§bKlicke§7, um diese zu sehen."));
            inv.setItem(33, ms.renameItemStack(new ItemStack(Material.GOLD_BARDING), "§6§lExtras", "§7Diese kannst du jederzeit und", "§7überall beliebig aktivieren.", "", "§6Klicke§7, um diese zu sehen."));
            inv.setItem(34, settings);
        } else if (extras == false && p.hasPermission("muxsystem.vip") == false) {
            inv.setItem(29, friends);
            inv.setItem(33, settings);
        } else if (extras == false && (p.hasPermission("muxsystem.vip"))) {
            inv.setItem(28, friends);
            inv.setItem(31, ms.renameItemStack(new ItemStack(Material.DIAMOND), "§b§lVorteile", "§7Alle Funktionen, die dein Rang", "§7bietet, siehst du hierdurch.", "", "§bKlicke§7, um diese zu sehen."));
            inv.setItem(34, settings);
        } else if (extras && extraUser.getExtraCMDs().isEmpty() == false) {
            inv.setItem(28, friends);
            inv.setItem(30, ms.renameItemStack(new ItemStack(Material.DIAMOND), "§b§lVorteile", "§7Alle Funktionen, die dein Rang", "§7bietet, siehst du hierdurch.", "", "§bKlicke§7, um diese zu sehen."));
            inv.setItem(32, ms.renameItemStack(new ItemStack(Material.GOLD_BARDING), "§6§lExtras", "§7Diese kannst du jederzeit und", "§7überall beliebig aktivieren.", "", "§6Klicke§7, um diese zu sehen."));
            inv.setItem(34, settings);
        } else if (extras && p.hasPermission("muxsystem.vip") == false) {
            inv.setItem(28, friends);
            inv.setItem(31, ms.renameItemStack(new ItemStack(Material.GOLD_BARDING), "§6§lExtras", "§7Diese kannst du jederzeit und", "§7überall beliebig aktivieren.", "", "§6Klicke§7, um diese zu sehen."));
            inv.setItem(34, settings);
        } else {
            inv.setItem(28, friends);
            inv.setItem(30, ms.renameItemStack(new ItemStack(Material.DIAMOND), "§b§lVorteile", "§7Alle Funktionen, die dein Rang", "§7bietet, siehst du hierdurch.", "", "§bKlicke§7, um diese zu sehen."));
            inv.setItem(32, ms.renameItemStack(new ItemStack(Material.GOLD_BARDING), "§6§lExtras", "§7Diese kannst du jederzeit und", "§7überall beliebig aktivieren.", "", "§6Klicke§7, um diese zu sehen."));
            inv.setItem(34, settings);
        }
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.MENU);
        return true;
    }

    public void openTeamBaseInventory(final Player player) {
        final PlayerBase base = ms.getBase().getFromPlayer(player);
        final boolean helper = player.getOpenInventory().getTitle().contains("Helfer") || player.hasMetadata("BaseHelper");
        player.removeMetadata("BaseHelper", ms);
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§l" + (helper ? "Helfer" : "MuxMenü") + "§0 | Teambase");
        final InvType it = ms.getActiveInv(player.getName());
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back") + (helper ? " (Helfer)" : "")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT, 1, (short) 5), "§f§lTeambase", "§7Kooperiere mit anderen Spielern, indem", "§7du ihnen Zugang auf deine Base gibst."));
        boolean canAddTempTrusted;
        boolean canAddTrusted = base.getTrustedSize() < 5;
        if (canAddTrusted) {
            int size = base.getTempTrustedSize() + base.getTrustedSize();
            canAddTempTrusted = size < 5;
        } else {
            canAddTempTrusted = base.getTempTrustedSize() < 2;
        }
        inv.setItem(7, ms.renameItemStack(new ItemStack(Material.WATCH), "§e§lTemporären Zugang geben",
                canAddTempTrusted ? new String[]{
                        "§7Damit gibst du dem Spieler für 2 Stunden",
                        "§7§nkomplette Baurechte§7 auf deiner Base.",
                        "",
                        "§7Klicke, um jemanden §eZugang zu geben§7."
                } : new String[]{
                        "§7Damit gibst du dem Spieler für 2 Stunden",
                        "§7§nkomplette Baurechte§7 auf deiner Base.",
                        "",
                        "§cDu kannst niemanden mehr Zugang geben."
                }));
        inv.setItem(8, ms.renameItemStack(new ItemStack(Material.BOOK_AND_QUILL), "§6§lPermanenten Zugang geben",
                canAddTrusted ? new String[]{
                        "§7Dieser Spieler kann deine Base solange",
                        "§7bearbeiten, §7§nbis du ihm die Rechte nimmst§7.",
                        "",
                        "§7Klicke, um jemanden §6Zugang zu geben§7."
                } : new String[]{
                        "§7Dieser Spieler kann deine Base solange",
                        "§7bearbeiten, §7§nbis du ihm die Rechte nimmst§7.",
                        "",
                        "§cDu kannst niemanden mehr Zugang geben."
                }));
        int slot = 18;
        final Server sr = ms.getServer();
        for (final UUID uuid : base.getTrusted()) {
            final OfflinePlayer op = sr.getOfflinePlayer(uuid);
            final ItemStack skull = op.isOnline() && player.canSee(op.getPlayer()) ? ms.getHead(op.getName()) : new ItemStack(Material.SKULL_ITEM);
            inv.setItem(slot, ms.renameItemStack(skull, "§f§l" + op.getName(), "§7Zugang: §6Permanent", "", "§7Klicke, um diesem Spieler", "§7die Rechte zu §centziehen§7."));
            slot++;
        }
        for (final Map.Entry<UUID, Long> entry : base.getTempTrusted().entrySet()) {
            final OfflinePlayer op = sr.getOfflinePlayer(entry.getKey());
            final long timeLeft = base.getTempTrustedRemaining(op.getUniqueId()) - System.currentTimeMillis();
            final String timeColor = timeLeft < 300000 ? "§4" : (timeLeft < 600000 ? "§c" : (timeLeft < 1500000 ? "§6" : "§e")),
                    timeString = "noch " + ms.getTime((int) (timeLeft / 1000));
            final ItemStack skull = op.isOnline() && player.canSee(op.getPlayer()) ? ms.getHead(op.getName()) : new ItemStack(Material.SKULL_ITEM);
            inv.setItem(slot, ms.renameItemStack(skull, "§f§l" + op.getName(), "§7Zugang: " + timeColor + timeString, "", "§7Klicke, um diesem Spieler", "§7die Rechte zu §centziehen§7."));
            slot++;
        }
        if (it != InvType.MENU && it != InvType.MUXANVIL) player.closeInventory();
        player.openInventory(inv);
        ms.setActiveInv(player.getName(), InvType.MENU);
    }

    public void handleTeamBaseInventory(final Player p, final ItemStack i) {
        final Material m = i.getType();
        if (m == Material.ITEM_FRAME) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            if (i.getItemMeta().getDisplayName().contains("(")) {
                ms.getBase().openHelper(p);
                return;
            }
            openSettings(p);
            return;
        } else if (m == Material.DOUBLE_PLANT) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        }
        final PlayerBase base = ms.getBase().getFromPlayer(p);
        boolean canAddTempTrusted, canAddTrusted = base.getTrustedSize() < 5;
        if (canAddTrusted) {
            final int size = base.getTempTrustedSize() + base.getTrustedSize();
            canAddTempTrusted = size < 5;
        } else {
            canAddTempTrusted = base.getTempTrustedSize() < 2;
        }
        if (m == Material.WATCH) {
            if (canAddTempTrusted == false) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            new MuxAnvil(ms, ((input, p1) -> {
                final String playerName = ms.retardCleaner(input, "Spielername: ");
                final Player op = ms.getServer().getPlayer(playerName);
                ms.getBase().invitePlayer(op, p1, true);
                p.setMetadata("BaseHelper", new FixedMetadataValue(ms, true));
                openTeamBaseInventory(p1);
            })).show(p, "Spielername: ", new ItemStack(Material.SKULL_ITEM, 1, (byte) 3));
        } else if (m == Material.BOOK_AND_QUILL) {
            if (canAddTrusted == false) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            new MuxAnvil(ms, ((input, p1) -> {
                final String playerName = ms.retardCleaner(input, "Spielername: ");
                final Player pl = ms.getServer().getPlayer(playerName);
                ms.getBase().invitePlayer(pl, p1, false);
                p.setMetadata("BaseHelper", new FixedMetadataValue(ms, true));
                openTeamBaseInventory(p1);
            })).show(p, "Spielername: ", new ItemStack(Material.SKULL_ITEM, 1, (byte) 3));
        } else if (m == Material.SKULL_ITEM) {
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
            final String name = ChatColor.stripColor(i.getItemMeta().getDisplayName());
            boolean temp = i.getItemMeta().getLore().get(0).contains("Permanent") == false;
            final OfflinePlayer op = ms.getPlayer(name);
            if (temp) {
                base.removeTempTrusted(op.getUniqueId());
                if (op.isOnline()) {
                    final Player pl = op.getPlayer();
                    pl.playSound(pl.getLocation(), Sound.NOTE_BASS, 30, 30);
                    pl.sendMessage("§a§lMuxBase>§7 Dein Zugang zur Teambase wurde dir §centzogen§7.");
                    pl.sendMessage("§a§lMuxBase>§7 Du kannst §a" + p.getName() + "§7 nicht mehr unterstützen.");
                }
            } else {
                base.removeTrusted(op.getUniqueId());
                if (op.isOnline()) {
                    final Player pl = op.getPlayer();
                    pl.playSound(pl.getLocation(), Sound.NOTE_BASS, 30, 30);
                    pl.sendMessage("§a§lMuxBase>§7 Dein Zugang zur Teambase wurde dir §centzogen§7.");
                    pl.sendMessage("§a§lMuxBase>§7 Du kannst §a" + p.getName() + "§7 nicht mehr unterstützen.");
                } else {
                    ms.getChat().sendOfflineMessage(op, "MuxBase", "§7Dein Zugang zur Teambase wurde dir §centzogen§7. §7Du kannst §a" + p.getName() + " §7nicht mehr unterstützen.", true);
                }
            }
            openTeamBaseInventory(p);
        }
    }

    public void handleInventory(final Player p, final ItemStack i, final String title, final boolean rightclick) {
        if (title.equals("§0§lMuxMenü§0 | Vorteile")) {
            benefits.handleInventory(p, i, rightclick);
            return;
        }
        if (title.contains("Teambase")) {
            handleTeamBaseInventory(p, i);
            return;
        }
        final short durability = i.getDurability();
        final Material m = i.getType();
        if (m == Material.BEACON) {
            if (ms.inBattle(p.getName(), p.getLocation())) {
                ms.showItemBar(p, ms.hnotinfight);
                p.closeInventory();
                return;
            }
            p.closeInventory();
            p.performCommand("spawn");
        } else if (m == Material.COMPASS) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            p.performCommand("homes");
        } else if (m == Material.DOUBLE_PLANT) {
            if (durability == 0) {
                if (ms.inBattle(p.getName(), p.getLocation())) {
                    ms.showItemBar(p, ms.hnotinfight);
                    p.closeInventory();
                    return;
                }
                p.closeInventory();
                p.performCommand("shop");
            } else if (durability == 5) {
                openTeamBaseInventory(p);
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            }
        } else if (m == Material.BOAT) {
            if (ms.getTeam().toggleAFK(p) == false) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            openSettings(p);
        } else if (m == Material.BED) {
            final boolean settings = title.contains("|");
            if (settings == false) {
                p.closeInventory();
                p.playSound(p.getLocation(), Sound.VILLAGER_YES, 1F, 1F);
                p.getInventory().setHeldItemSlot(8);
                p.updateInventory();
                return;
            }
            final PlayerBase base = ms.getBase().getFromPlayer(p);
            if (base == null || base.contains(p.getLocation()) == false) {
                openSettings(p);
                return;
            }
            base.setSpawn(p.getLocation());
            p.closeInventory();
            p.playSound(p.getLocation(), Sound.PISTON_EXTEND, 1F, 1F);
            ms.showItemBar(p, "§fDein §aSpawnpunkt §fwurde hier gesetzt.");
        } else if (m == Material.TNT) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            final PlayerBase base = ms.getBase().getFromPlayer(p);
            if (base == null || base.contains(p.getLocation()) == false) {
                openSettings(p);
                return;
            } else if (base.isGriefable()) {
                ms.showItemBar(p, "§cDeine Base ist momentan griefbar.");
                ms.showLater(p, "§cDu kannst in dieser Zeit die Base nicht zurücksetzen.");
                p.closeInventory();
                return;
            } else if (base.hasResetToday()) {
                ms.showItemBar(p, "§cDu hast bereits vor kurzem deine Base zurückgesetzt.");
                return;
            }
            final boolean settings = title.contains("|");
            final MuxActions.PlayerAction confirm = player -> base.reset(), cancel = settings ? player -> openSettings(p) : player -> ms.getBase().openHelper(player);
            new ConfirmInventory(ms, confirm, cancel).show(p, "§0§lBase zurücksetzen", ms.getLang("confirm") + " §c(Permanent)", ms.getLang("abort"));
        } else if (m == Material.GRASS) {
            final PlayerBase base = ms.getBase().getFromPlayer(p);
            final Location l = base.getMaxPos().getWorld().getHighestBlockAt(base.getMaxPos().clone()).getLocation().add(0, 1, 0);
            l.setPitch(50F);
            ms.forcePlayer(p, l);
            p.playSound(l, Sound.ENDERMAN_TELEPORT, 0.4F, 1F);
        } else if (m == Material.LEATHER_BOOTS) {
            p.performCommand("event leave");
        } else if (m == Material.LEATHER_CHESTPLATE) {
            p.performCommand("games leave");
        } else if (m == Material.GOLD_SWORD) {
            final MuxPvP.RankedArena r = ms.getPvP().get1vs1Arena(p);
            p.closeInventory();
            if (r != null) r.handleDeath(p, false);
        } else if (m == Material.DARK_OAK_DOOR_ITEM) {
            if (rightclick) {
                final int maxHomes = (p.hasPermission("muxsystem.morehomes.unlimited") ? 100 : p.hasPermission("muxsystem.morehomes.elite") ? 10 : p.hasPermission("muxsystem.morehomes.vip") ? 5 : 1);
                if (maxHomes == 1) {
                    p.performCommand("sethome");
                } else {
                    new MuxAnvil(ms, (input, player) -> player.performCommand("sethome " + ms.retardCleaner(input, "Name des Homes: "))).show(p, "Name des Homes: ", new ItemStack(Material.BED));
                }
                return;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            p.performCommand("home");
            p.closeInventory();
        } else if (m == Material.HOPPER) {
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            final Settings settings = ms.getMuxUser(p.getName()).getSettings();
            settings.setChatFilter(settings.hasChatFilter() == false);
            openSettings(p);
        } else if (m == Material.EMPTY_MAP) {
            final boolean disabled = ms.getChat().hasDisabledChat(p.getName());
            if (ms.checkGeneralCooldown(p.getName(), "GLOBALCHAT", 5000L, true)) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            p.sendMessage(" ");
            if (disabled) {
                ms.getChat().enableChat((p.getName()));
                p.sendMessage("§a§lDein Chat ist jetzt wieder aktiviert.");
            } else {
                ms.getChat().disableChat(p.getName());
                p.sendMessage("§c§lDein Chat ist jetzt deaktiviert.");
                ms.chatClickHoverRun(p, "§f§lKlicke hier, um die Einstellung zu ändern.", "§f§oEinstellungen öffnen", "/menu settings");
            }
            openSettings(p);
        } else if (m == Material.CAKE) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            p.performCommand("f");
        } else if (m == Material.SKULL_ITEM) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            ms.getProfiles().showFullPlayerProfile(p, p.getName(), "Menü");
        } else if (m == Material.CHAINMAIL_HELMET) {
            p.performCommand("c");
        } else if (m == Material.GOLD_BARDING) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            ms.getExtras().openExtras(p, false);
        } else if (m == Material.SAPLING) {
            if (ms.inBattle(p.getName(), p.getLocation())) {
                ms.showItemBar(p, ms.hnotinfight);
                p.closeInventory();
            } else if (durability == 0) {
                if (ms.getBase().hasBase(p)) {
                    p.performCommand("base");
                    p.closeInventory();
                    if (ms.getBase().isEnabled() == false && p.isOp() == false) {
                        p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                    }
                } else {
                    p.closeInventory();
                    if (ms.getBase().isEnabled() == false && p.isOp() == false) {
                        p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                        ms.showItemBar(p, "§cDie Basen sind momentan deaktiviert.");
                        return;
                    }
                    if (ms.getBase().createPlayerBase(p)) {
                        p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                    }
                }
            } else {
                final String plname = i.getItemMeta().getDisplayName().split(" ")[2];
                p.performCommand("base tp " + plname);
                p.closeInventory();
            }
        } else if (m == Material.REDSTONE_COMPARATOR) {
            if (title.contains("Einstellungen")) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            openSettings(p);
        } else if (m == Material.SLIME_BALL) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            p.performCommand("support");
        } else if (m == Material.MAGMA_CREAM) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            p.performCommand("contact");
        } else if (m == Material.REDSTONE) {
            final Settings s = ms.getMuxUser(p.getName()).getSettings();
            s.setBlood(s.hasBloodEffect() ^ true);
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            openSettings(p);
        } else if (m == Material.ITEM_FRAME) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            if (title.contains("Mehr")) {
                openSettings(p);
                return;
            }
            p.performCommand("menu");
        } else if (m == Material.PRISMARINE_SHARD) {
            if (title.contains("Mehr")) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            openExtraSettings(p);
        } else if (m == Material.DIAMOND) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            benefits.openPremiumInv(p);
        } else if (m == Material.WATER_LILY) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            ms.getTeam().handleCommand(p);
        } else if (m == Material.RECORD_7) {
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            ms.getCreators().toggleRecording(p.getUniqueId());
            openExtraSettings(p);
        } else if (m == Material.FEATHER) {
            if (p.getGameMode() == GameMode.CREATIVE) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1.F);
                return;
            }
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            p.performCommand("fly");
            openExtraSettings(p);
        } else if (m == Material.STAINED_GLASS) {
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            p.performCommand("vanish");
            openExtraSettings(p);
        } else if (m == Material.INK_SACK && durability == 6) {
            if (ms.getChat().getPrefix(p.getUniqueId()) != null) {
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                ms.getChat().setChatColor(p, null);
                openExtraSettings(p);
                return;
            }
            new MuxAnvil(ms, (input, pl) -> {
                input = ms.retardCleaner(input, "Chatfarbe: ");
                pl.closeInventory();
                if (ms.getChat().setChatColor(pl, input) == false) {
                    pl.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                } else {
                    pl.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                }
                openExtraSettings(p);
            }).show(p, "Chatfarbe: ");
        } else if (m == Material.INK_SACK && durability == 1) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            p.performCommand("admin");
        } else if (m == Material.BRICK) {
            ms.sendPlayerToServer(p, "build");
        } else if (m == Material.PISTON_BASE) {
            ms.sendPlayerToServer(p, "test");
        } else if (m == Material.FIREWORK_CHARGE) {
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            if (forcefield.remove(p) == false) {
                forcefield.add(p);
            }
            openExtraSettings(p);
        } else if (m == Material.NETHER_STAR) {
            final Settings s = ms.getMuxUser(p.getName()).getSettings();
            if (s.hasMenu() == false && p.getInventory().getItem(8) != null) {
                p.closeInventory();
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                ms.showItemBar(p, "§cLeere die §nletzte Stelle§c in deinem Inventar.");
                return;
            }
            s.setMenu(s.hasMenu() ^ true);
            p.getInventory().setItem(8, new ItemStack(Material.AIR));
            addMenuItem(p, true);
            p.updateInventory();
            openSettings(p);
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
        }
    }

    public void openExtraSettings(final Player p) {
        final String pname = p.getName();
        final InvType it = ms.getActiveInv(pname);
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxMenü§0 | Mehr Einstellungen");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.PRISMARINE_SHARD), "§b§lMehr Einstellungen", "§7Hier gibt es zusätzliche Optionen,", "§7die nicht für alle zugänglich sind."));
        final boolean rec = ms.getCreators().isRecording(p), vanish = ms.isVanish(p), fly = ms.hasGlobalFly(p);
        final String chatprefix = ms.getChat().getPrefix(p.getUniqueId());
        final ItemStack ff = ms.renameItemStack(ms.getFireworkCharge(Color.PURPLE, false), "§f§lForcefield" + (hasForcefield(p) ? "§a §laktiviert" : ""),
                "§7Andere Spieler können dann nicht", "§7mehr sich in deiner Nähe begeben.",
                "", "§7Klicke, um diesen zu " + (hasForcefield(p) ? "§cdeaktivieren" : "§aaktivieren") + "§7.");
        final ItemStack v = ms.renameItemStack(new ItemStack(Material.STAINED_GLASS), "§f§lVanish§r" + (vanish ? " §a§laktiviert" : ""), "§7Mit dieser Option bis du für alle", "§7anderen Spieler nicht sichtbar.", "", "§7Klicke, um diesen zu " + (vanish ? "§cdeaktivieren" : "§aaktivieren") + "§7."),
                f = ms.renameItemStack(new ItemStack(Material.FEATHER), "§f§lFlugmodus§r" + (fly ? " §a§laktiviert" : ""), "§7Man kann temporär fliegen und ist", "§7zugleich vor Angriffen geschützt.", "",
                        (p.getGameMode() == GameMode.CREATIVE ? "§cDu bist bereits im Creative-Modus." : "§7Klicke, um diesen zu " + (fly ? "§cdeaktivieren" : "§aaktivieren") + "§7.")),
                chatcolor = ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (short) 6), "§f§lChatfarbe" + (chatprefix != null ? ":§r " + chatprefix.replace("§", "&") : ""), "§7Diese wird vor jeder Nachricht", "§7im Chat automatisch verwendet.", "", "§7Klicke, um diese zu " + (chatprefix == null ? "§fsetzen" : "§centfernen") + "§7.");
        final ItemStack focus = ms.renameItemStack(new ItemStack(Material.RECORD_7), "§f§lFokus" + (rec ? "§a §laktiviert" : ""), "§7MSGs von fremden Spielern und", "§7alle Anfragen werden ignoriert.", "", "§7Klicke, um diesen zu " + (rec ? "§cdeaktivieren" : "§aaktivieren") + "§7.");
        final ItemMeta im = focus.getItemMeta();
        im.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        focus.setItemMeta(im);
        final MuxRanks.PermissionsGroup group = ms.getPerms().getGroupOf(p.getUniqueId());
        final String gname = group == null ? "" : group.getName().toLowerCase();
        final boolean creators = p.hasPermission("muxsystem.moresettings") == false && (gname.contains("dev") || gname.contains("builder"));
        if (p.hasPermission("muxsystem.moresettings")) {
            inv.setItem(19, fly ? ms.addGlow(f) : f);
            inv.setItem(20, vanish ? ms.addGlow(v) : v);
            inv.setItem(24, rec ? ms.addGlow(focus) : focus);
            inv.setItem(25, hasForcefield(p) ? ms.addGlow(ff) : ff);
        } else if (creators) {
            inv.setItem(24, rec ? ms.addGlow(focus) : focus);
        }
        inv.setItem(creators ? 20 : 22, chatprefix != null ? ms.addGlow(chatcolor) : chatcolor);
        if (it != InvType.MENU && it != InvType.CONFIRM) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(pname, InvType.MENU);
    }

    public void openSettings(final Player p) {
        final String pname = p.getName();
        final InvType it = ms.getActiveInv(pname);
        if (it != InvType.MENU && it != InvType.PROFILE) p.closeInventory();
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxMenü§0 | Einstellungen");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.REDSTONE_COMPARATOR), "§f§lEinstellungen", "§7Es lassen sich verschiedene", "§7Optionen hier festlegen."));
        if (p.hasPermission("muxsystem.team") || p.hasPermission("muxsystem.gold")) {
            inv.setItem(8, ms.renameItemStack(new ItemStack(Material.PRISMARINE_SHARD), "§b§lMehr Einstellungen", "§7Hier gibt es zusätzliche Optionen,", "§7die nicht für alle zugänglich sind.", "", "§bKlicke§7, um diese zu sehen."));
        }
        final Settings s = ms.getMuxUser(pname).getSettings();
        final boolean inbase = ms.getBase().hasBase(p) && ms.getBase().getFromPlayer(p).contains(p.getLocation());
        if (inbase) {
            inv.setItem(19, ms.renameItemStack(new ItemStack(Material.BED), "§a§lSpawnpunkt setzen", "§7Hiermit bestimmst du die Stelle,", "§7wo man in deiner Base spawnt.", "", "§7Klicke, §7um den Punkt zu §asetzen§7."));
            inv.setItem(22, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT, 1, (short) 5), "§f§lTeambase", "§7Kooperiere mit anderen Spielern, indem", "§7du ihnen Zugang auf deine Base gibst.", "", "§fKlicke§7, um dein Team zu verwalten."));
            inv.setItem(25, ms.renameItemStack(new ItemStack(Material.TNT), "§c§lBase zurücksetzen", "§7Hiermit entfernst du alles auf deiner", "§7Base, die Größe bleibt dabei erhalten.", "", (ms.getBase().getFromPlayer(p).hasResetToday() ? "§cDie Base wurde schon zurückgesetzt." : "§7Klicke, um die Base §czurückzusetzen§7.")));
        }
        final ItemStack blood = ms.renameItemStack(new ItemStack(Material.REDSTONE), "§f§lBluteffekt" + (s.hasBloodEffect() ? "§a §laktiviert" : ""),
                "§7Dein Bildschirm wird rötlich, wenn du", "§7wenig Herzen hast beim Kämpfen.",
                "", "§7Klicke, um diesen zu " + (s.hasBloodEffect() ? "§cdeaktivieren" : "§aaktivieren") + "§7."),
                filter = ms.renameItemStack(new ItemStack(Material.HOPPER), "§f§lChatfilter" + (s.hasChatFilter() == false ? "§c §ldeaktiviert" : ""),
                        "§7Dieser blendet Beleidigungen und", "§7vulgäre Ausdrücke für dich aus.", "", "§7Klicke, um diesen zu " + (s.hasChatFilter() ? "§cdeaktivieren" : "§aaktivieren") + "§7."),
                chat = ms.renameItemStack(new ItemStack(Material.EMPTY_MAP), "§f§lGlobalchat" + (ms.getChat().hasDisabledChat(pname) ? "§c §ldeaktiviert" : ""),
                        "§7Hier kannst du temporär den globalen", "§7Chat nur für dich selbst ausschalten.",
                        "", "§7Klicke, um diesen §7zu " + (ms.getChat().hasDisabledChat(pname) ? "§aaktivieren" : "§cdeaktivieren") + "§7.");
        inv.setItem(inbase ? 37 : 19, ms.getChat().hasDisabledChat(pname) ? ms.addGlow(chat) : chat);
        inv.setItem(inbase ? 38 : 20, s.hasChatFilter() ? filter : ms.addGlow(filter));
        inv.setItem(inbase ? 39 : 21, s.hasBloodEffect() ? ms.addGlow(blood) : blood);
        inv.setItem(inbase ? 41 : 23, ms.renameItemStack(ms.getHead(pname), "§f§lProfil", "§7Hier siehst du deine eigenen", "§7bisherigen Statistiken.", "", "§fKlicke§7, um diese zu sehen."));
        inv.setItem(inbase ? 42 : 24, ms.renameItemStack(new ItemStack(Material.MAGMA_CREAM), "§f§lSocial & Kontakt", "§7Hier findest du die wichtigsten", "§7Links und Social Media Seiten.", "", "§7Klicke, um die Liste zu §fsehen§7."));
        if (p.hasPermission("muxsystem.team")) {
            final MuxTeam mt = ms.getTeam();
            inv.setItem(inbase ? 43 : 25, ms.renameItemStack(new ItemStack(Material.BOAT), "§f§lStatus: " + (mt.isAFK(p.getName()) ? ms.getLang("team.afkaway") : ms.getLang("team.afkactive")), mt.isAFK(p.getName()) ?
                    new String[]{ms.getLang("team.afklore1"), ms.getLang("team.afklore2"), "", ms.getLang("team.afkloreactive")} :
                    new String[]{ms.getLang("team.afklore1"), ms.getLang("team.afklore2"), "", ms.getLang("team.afkloreafk")}));
        } else {
            inv.setItem(inbase ? 43 : 25, ms.renameItemStack(new ItemStack(Material.SLIME_BALL), "§a§lSupport kontaktieren", "§7Du kannst jederzeit den MuxSupport", "§7für Hilfe oder Fragen kontaktieren.", "", "§7Klicke, um jetzt Support zu §aerhalten§7."));
        }
        //inv.setItem(40, ms.renameItemStack(new ItemStack(Material.NETHER_STAR), "§f§lStern im Inventar" + (s.hasMenu() ? "" : "§c §ldeaktiviert"), "§7Du kannst es jederzeit mit", "§7dem Befehl §f/menu§7 öffnen.", "", "§7Klicke, um das Menü", "§7zu " + (s.hasMenu() ? "§cdeaktivieren" : "§aaktivieren") + "§7."));
        p.openInventory(inv);
        ms.setActiveInv(pname, InvType.MENU);
    }

    public void checkForcefield() {
        for (final Player p : ms.getServer().getOnlinePlayers()) {
            if (forcefield.contains(p)) {
                for (final Player other : ms.getServer().getOnlinePlayers()) {
                    if (p.equals(other) || other.isOp() || offset(other, p) > 4 || other.getGameMode() == GameMode.SPECTATOR)
                        continue;
                    Entity bottom = other;
                    while (bottom.getVehicle() != null) bottom = bottom.getVehicle();
                    velocity(bottom, getTrajectory2D(p, bottom));
                    other.getWorld().playSound(other.getLocation(), Sound.ZOMBIE_INFECT, 1F, 1F);
                }
            }
        }
    }

    private double offset(final Entity a, final Entity b) {
        return a.getLocation().toVector().subtract(b.getLocation().toVector()).length();
    }

    private Vector getTrajectory2D(final Entity from, final Entity to) {
        return to.getLocation().toVector().subtract(from.getLocation().toVector()).setY(0).normalize();
    }

    private void velocity(final Entity ent, final Vector vec) {
        final double yMax = 10D;
        if (Double.isNaN(vec.getX()) || Double.isNaN(vec.getY()) || Double.isNaN(vec.getZ()) || vec.length() == 0)
            return;
        vec.setY(0.8);
        vec.normalize();
        vec.multiply(1.6);
        vec.setY(vec.getY());
        if (vec.getY() > yMax) vec.setY(yMax);
        ent.setFallDistance(0);
        ent.setVelocity(vec);
    }

    public void setMenu(final Player p, final boolean show) {
        switchItem(p, show);
    }

    public void addMenuItem(final Player p, final boolean held) {
        final MuxUser u = ms.getMuxUser(p.getName());
        if (u.getSettings().hasMenu()) {
            p.getInventory().setItem(8, ms.renameItemStack(new ItemStack(Material.NETHER_STAR), ms.getLang("menu.menu")));
            if (held) p.getInventory().setHeldItemSlot(8);
        }
    }

    public void switchItem(final Player p, final boolean pvp) {
        final Inventory inv = p.getInventory();
        final ItemStack slot8 = inv.getItem(8);
        final MuxUser u = ms.getMuxUser(p.getName());
        if ((p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) && p.getAllowFlight() == false) {
            p.setGameMode(pvp && ms.in1vs1(p) == false ? GameMode.ADVENTURE : GameMode.SURVIVAL);
        }
        if (u != null && u.getSettings().hasMenu() == false) {
            return;
        }
        if (pvp) {
            if (slot8 == null) return;
            if (slot8.getType() != Material.NETHER_STAR) return;
            if (ms.in1vs1(p) == false) ms.showItemBar(p, "§fDu befindest dich nun im §cPvP Bereich§f.");
            inv.setItem(8, new ItemStack(Material.AIR));
            final Map.Entry<ItemStack, Integer> bestitem = getUsefulItem(inv);
            if (bestitem.getKey() == null) {
                return;
            }
            inv.setItem(bestitem.getValue(), null);
            inv.setItem(8, bestitem.getKey());
            p.updateInventory();
        } else if (slot8 == null || slot8.getType() == Material.AIR) {
            addMenuItem(p, false);
            if (ms.inEvent(p) == false || ms.inWar(p) == false || ms.in1vs1(p) == false || ms.inDuel(p) == false)
                ms.showItemBar(p, "§fDu bist wieder im §ageschützten Bereich§f.");
            p.updateInventory();
        } else if (slot8.getType() != Material.NETHER_STAR || slot8.getItemMeta().hasDisplayName() == false) {
            int slot = getEmptySlot(p);
            if (slot == -1) { // full inventory
                return;
            }
            if (ms.inEvent(p) == false || ms.inWar(p) == false || ms.in1vs1(p) == false || ms.inDuel(p) == false)
                ms.showItemBar(p, "§fDu bist wieder im §ageschützten Bereich§f.");
            inv.setItem(slot, inv.getItem(8));
            addMenuItem(p, false);
            p.updateInventory();
        }
    }

    private Map.Entry<ItemStack, Integer> getUsefulItem(final Inventory inv) {
        ItemStack choose = null;
        int pos = 0;
        for (int i = 9; i < inv.getSize(); i++) {
            final ItemStack it = inv.getItem(i);
            if (it != null && (choose == null || choose.getType() != Material.POTION || choose.getDurability() != 16421)) {
                choose = it;
                pos = i;
            }
        }
        return new AbstractMap.SimpleEntry<>(choose, pos);
    }

    private int getEmptySlot(final Player p) {
        int slot = 9;
        final PlayerInventory pi = p.getInventory();
        if (pi.firstEmpty() == -1) return -1;
        for (int i = 9; i < pi.getSize(); i++) {
            if (pi.getItem(i) == null || pi.getItem(i).getType() == Material.AIR) {
                slot = i;
                break;
            }
        }
        return slot;
    }
}