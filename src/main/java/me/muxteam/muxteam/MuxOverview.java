package me.muxteam.muxteam;

import me.muxteam.base.PlayerBase;
import me.muxteam.basic.ConfirmInventory;
import me.muxteam.basic.MuxActions.PlayerAction;
import me.muxteam.basic.MuxOfflinePlayer;
import me.muxteam.events.Event;
import me.muxteam.marketing.MuxEmails;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.ranks.MuxRanks;
import me.muxteam.shared.MuxSharedPackets;
import net.minecraft.server.v1_8_R3.DataWatcher;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class MuxOverview {
    private MuxSystem ms;


    private Object protocolManager;
    private Method protocolVersionMethod;

    public MuxOverview(final MuxSystem ms) {
        this.ms = ms;
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    Class<?> protocolLibrary = Class.forName("us.myles.ViaVersion.api.Via");
                    protocolManager =  protocolLibrary.getMethod("getAPI").invoke(new Class[0]);
                    protocolVersionMethod = protocolManager.getClass().getDeclaredMethod("getPlayerVersion", UUID.class);
                    protocolVersionMethod.setAccessible(true);
                } catch (NoSuchMethodException | ClassNotFoundException | InvocationTargetException |
                         IllegalAccessException e) {
                    throw new RuntimeException("ViaVersion is not installed!", e);
                }
            }
        }.runTask(ms); // wir delayn einen tick, so dass das plugin trotzdem enablen würde
    }

    public void close() {
        this.ms = null;
    }

    public boolean handleCommand(final Player p, final String[] args) {
        if (p.hasPermission("muxsystem.team") == false) {
            ms.sendNoCMDMessage(p);
            return true;
        } else if (args.length == 0) {
            ms.showItemBar(p, ms.usage("/o [spieler]"));
            return true;
        }
        openUserControl(p, args[0]);
        return true;
    }

    public void handleInventory(final Player p, final ItemStack i, final Inventory inv) {
        final Material m = i.getType();
        final List<String> lore = i.getItemMeta().getLore();
        final ItemStack middleitem = inv.getItem(4);
        final Material category = middleitem.getType();
        final ItemMeta profile = middleitem.getItemMeta();
        final String plname = middleitem.getType() == Material.SKULL_ITEM ? ChatColor.stripColor(profile.getDisplayName()) : inv.getTitle().split(" \\| ")[1];
        final OfflinePlayer ply = ms.getPlayer(plname);
        if (ply == null) {
            ms.showItemBar(p, ms.hnotfound);
            return;
        }
        final boolean online = ply.isOnline();
        switch (m) {
            case STAINED_CLAY:
                if (category == Material.SAPLING) {
                    final short data = i.getDurability();
                    if (data == (byte) 4) {
                        resetBaseGrief(p, plname);
                    } else if (i.getDurability() == (byte) 5) {
                        resetBase(p, plname);
                    }
                    p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                    p.closeInventory();
                    return;
                }
                final String rankName = ChatColor.stripColor(i.getItemMeta().getLore().get(0));
                if (ms.hasGlow(i)) {
                    final MuxRanks.PermissionsGroup group = ms.getPerms().getGroup(rankName);
                    if (group != null) {
                        ms.getPerms().changePlayerGroup(ply.getUniqueId(), ply.getName(), group.isTeamGroup() ? "teamnull" : "Default", p);
                    }
                } else {
                    ms.getPerms().changePlayerGroup(ply.getUniqueId(), ply.getName(), rankName, p);
                }
                openUserGroup(p, ply);
                break;
            case NAME_TAG:
                if (inv.getItem(40) != null && inv.getItem(40).getType() == Material.NAME_TAG) {
                    ms.getCustomRank().resetTag(ply.getUniqueId());
                    p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                    ms.showItemBar(p, "§fDer Tag von §6" + plname + " §fwurde erfolgreich §azurückgesetzt§f.");
                    p.closeInventory();
                } else {
                    p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                    openUserGroup(p, ply);
                }
                return;
            case SKULL_ITEM: {
                if (lore.get(lore.size() - 1).toLowerCase().contains("accounts") == false) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        final MuxUser u = online ? ms.getMuxUser(plname) : ms.getDB().loadPlayer(ply.getUniqueId());
                        if (u == null) return;
                        final Server sr = ms.getServer();
                        final List<String> accounts = ms.getDB().getPlayersWithSameIP(u.getIp(), plname);
                        p.sendMessage(new String[]{
                                ms.header((byte) 15, "§7"),
                                "  §f§lAccounts von " + plname,
                                " ",
                                "  §f§o" + accounts.stream().map(s -> sr.getPlayer(s) != null ? "§a§o" + s : s).limit(150).collect(Collectors.joining("§7, §f§o")),
                                ms.footer((byte) 15, "§7")
                        });
                        p.closeInventory();
                    }
                }.runTaskAsynchronously(ms);
                break;
            }
            case SIGN:
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.performCommand("report " + plname + " Chat");
                return;
            case IRON_SWORD:
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.performCommand("report " + plname + " Cheat");
                return;
            case BOOK:
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.performCommand("history " + plname);
                return;
            case COMPASS:
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.performCommand("homes " + plname);
                return;
            case STORAGE_MINECART:
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.performCommand("sell " + plname);
                return;
            case GOLD_BARDING:
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.performCommand("extras " + plname);
                return;
            case CHEST:
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.performCommand("crate " + plname);
                return;
            case ENDER_PEARL:
                final Location l = new MuxOfflinePlayer(ply).getLocation();
                if (l == null) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                ms.setLastLocation(p);
                ms.teleportPlayer(p, l);
                return;
            case IRON_BOOTS:
                if (ply.getPlayer() == null || kickPlayer(p, ply.getPlayer()) == false) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                break;
            case FEATHER:
                if (toggleFly(p, plname) == false) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                break;
            case COOKED_CHICKEN:
                if (feed(p, plname) == false) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                break;
            case GOLDEN_APPLE:
                if (inv.getName().endsWith("Rang ändern")) {
                    if (ply.isOp()) {
                        ply.setOp(false);
                        if (ply.getUniqueId() != p.getUniqueId())
                            ms.getHistory().addHistory(ply.getUniqueId(), p.getUniqueId(), "TEAMACTION", "OPERATOR", "REMOVE");
                        ms.showItemBar(p, "§fDer Spieler §6" + ply.getName() + " §fist nun §cnicht mehr OP§f.");
                    } else {
                        ply.setOp(true);
                        if (ply.getUniqueId() != p.getUniqueId())
                            ms.getHistory().addHistory(ply.getUniqueId(), p.getUniqueId(), "TEAMACTION", "OPERATOR", "ADD");
                        ms.showItemBar(p, "§fDer Spieler §6" + ply.getName() + " §fist §anun OP§f.");
                    }
                    openUserGroup(p, ply);
                    break;
                }
                if (heal(p, plname) == false) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                break;
            case IRON_DOOR:
                if (giveVote(p, plname) == false) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                break;
            case BOOKSHELF:
                if (category == Material.BOOKSHELF) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                openDataManagement(p, ply);
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                return;
            case REDSTONE:
                if (category == Material.REDSTONE) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                openAdministrative(p, ply);
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                return;
            case BEACON:
                if (ms.checkGeneralCooldown(plname, "SpawnTP", 5000L, true) || toSpawn(p, plname) == false) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                break;
            case LEATHER_BOOTS:
                kickFromEvent(p, plname);
                break;
            case WOOD_AXE:
                if (toggleWE(p, plname) == false) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                break;
            case GRASS:
                if (toggleWG(p, plname) == false) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                break;
            case ANVIL: {
                final PlayerAction confirm = player -> crashPlayer(p, plname), cancel = player -> openAdministrative(p, ply);
                new ConfirmInventory(ms, confirm, cancel).show(p, "§0§lSpieler crashen", ms.getLang("confirm"), ms.getLang("abort"));
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                return;
            }
            case IRON_FENCE: {
                if (lore.get(lore.size() - 1).toLowerCase().startsWith("§a")) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                final PlayerAction confirm = player -> permBan(p, ply.getUniqueId()), cancel = player -> openAdministrative(p, ply);
                new ConfirmInventory(ms, confirm, cancel).show(p, "§0§lSpieler sperren", ms.getLang("confirm"), ms.getLang("abort"));
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                return;
            }
            case LAVA_BUCKET: {
                if (lore.get(lore.size() - 1).toLowerCase().startsWith("§a")) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                final PlayerAction confirm = player -> tempBan(p, plname), cancel = player -> openAdministrative(p, ply);
                new ConfirmInventory(ms, confirm, cancel).show(p, "§0§lSpieler suspendieren", ms.getLang("confirm"), ms.getLang("abort"));
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                return;
            }
            case TNT:
                if (category == Material.TNT) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                openReset(p, ply);
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                return;
            case SULPHUR: {
                final PlayerAction confirm = player -> resetPlayer(p, plname), cancel = player -> openReset(p, ply);
                new ConfirmInventory(ms, confirm, cancel).show(p, "§0§lSpielerdaten zurücksetzen", ms.getLang("confirm"), ms.getLang("abort"));
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                return;
            }
            case SAPLING: {
                if (category == Material.SAPLING) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                openBaseReset(p, ply);
                return;
            }
            case ARMOR_STAND: {
                final PlayerAction confirm = player -> {
                    resetPvPStats(p, plname);
                    player.closeInventory();
                }, cancel = player -> openReset(p, ply);
                new ConfirmInventory(ms, confirm, cancel).show(p, "§0§lKampfstats zurücksetzen", ms.getLang("confirm"), ms.getLang("abort"));
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                return;
            }
            case ITEM_FRAME:
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                if (category == Material.TNT || category == Material.SKULL_ITEM) {
                    openAdministrative(p, ply);
                    return;
                } else if (category == Material.SAPLING) {
                    openReset(p, ply);
                    return;
                }
                openUserControl(p, plname);
                return;
            default:
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
        }
        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
    }

    private void openBaseReset(final Player p, final OfflinePlayer pl) {
        final Inventory binv = ms.getServer().createInventory(null, 45, "§0§lMuxOverview§0 | " + pl.getName());
        binv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        binv.setItem(4, ms.renameItemStack(new ItemStack(Material.SAPLING), "§c§lBase zurücksetzen",
                "§7Die gesamte Base des Spielers, und", "§7damit auch ihr Wert wird gelöscht."));
        ms.createButton(binv, 19, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 5), "§aBase zurücksetzen"));
        ms.createButton(binv, 23, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 4), "§eBase griefbar setzen"));
        if (ms.getActiveInv(p.getName()) != InvType.USERCONTROL) p.closeInventory();
        p.openInventory(binv);
        ms.setActiveInv(p.getName(), InvType.USERCONTROL);
    }

    public void openUserGroup(final Player p, final OfflinePlayer pl) {
        final boolean online = pl.isOnline();
        final Inventory inv = ms.getServer().createInventory(null, 54, "§0§lMuxOverview§0 | Rang ändern");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(8, ms.renameItemStack(new ItemStack(Material.GOLDEN_APPLE, 1, (short) (pl.isOp() ? 1 : 0)), "§f§lOperator§r" + (pl.isOp() ? " §a§laktiviert" : ""),
                "", "§7Klicke, um sein OP zu " + (pl.isOp() ? "§cdeaktivieren§7." : "§aaktivieren§7.")));
        final MuxRanks perms = ms.getPerms();
        final MuxRanks.PermissionsUser pu = perms.getUserData(pl.getUniqueId());
        final List<MuxRanks.PermissionsGroup> staffGroups = perms.getGroupNames().stream().map(perms::getGroup)
                .filter(MuxRanks.PermissionsGroup::isTeamGroup)
                .sorted(Comparator.comparingInt(MuxRanks.PermissionsGroup::getWeight).reversed())
                .collect(Collectors.toList()),
                normalGroups = perms.getGroupNames().stream().map(perms::getGroup)
                        .filter(permissionsGroup -> permissionsGroup.isTeamGroup() == false)
                        .sorted(Comparator.comparingInt(MuxRanks.PermissionsGroup::getWeight).reversed())
                        .collect(Collectors.toList());
        int slot = 18;
        final boolean fulltrusted = ms.isFullTrusted(p.getName());
        if (fulltrusted) {
            for (final MuxRanks.PermissionsGroup group : normalGroups) {
                final String gname = group.getName();
                if (slot == 54) break;
                else if (gname.equals("Default")) continue;
                final String color = ChatColor.translateAlternateColorCodes('&', group.getColor()),
                        groupColor = ChatColor.getLastColors(color).replace("§", "");
                final ItemStack itemStack = new ItemStack(Material.STAINED_CLAY, 1, ms.getColorFromChatColor(groupColor).getWoolData());
                final boolean hasrank = pu != null && pu.getGroup() != null && pu.getGroup().equals(gname);
                inv.setItem(slot, ms.renameItemStack(hasrank ? ms.addGlow(itemStack) : itemStack,
                        ChatColor.translateAlternateColorCodes('&', group.getPrefix().replace(" ", "")), "§0" + group.getName(),
                        "§7Klicke, um den Rang zu " + (hasrank ? "§centfernen" : "§fvergeben") + "§7."));
                slot++;
            }
            slot = 36;
        }
        for (final MuxRanks.PermissionsGroup group : staffGroups) {
            if (slot == 54) break;
            final String gname = group.getName().toLowerCase();
            if (fulltrusted == false && (gname.contains("friend") || gname.contains("developer") || gname.contains("builder") || gname.contains("owner"))) {
                continue;
            }
            final String color = ChatColor.translateAlternateColorCodes('&', group.getColor()),
                    groupColor = ChatColor.getLastColors(color).replace("§", "");
            final ItemStack itemStack = new ItemStack(Material.STAINED_CLAY, 1, ms.getColorFromChatColor(groupColor).getWoolData());
            final boolean hasrank = pu != null && pu.getTeamGroup() != null && pu.getTeamGroup().equals(group.getName());
            inv.setItem(slot, ms.renameItemStack(hasrank ? ms.addGlow(itemStack) : itemStack,
                    ChatColor.translateAlternateColorCodes('&', group.getPrefix().replace(" ", "")), "§0" + group.getName(),
                    "§7Klicke, um den Rang zu " + (hasrank ? "§centfernen" : "§fvergeben") + "§7."));
            slot++;
        }
        final String teamrank = pu != null && pu.getTeamGroup() != null ? ChatColor.translateAlternateColorCodes('&', perms.getGroup(pu.getTeamGroup()).getPrefix()) : "§8-",
                userrank = pu != null && pu.getGroup() != null ? ChatColor.translateAlternateColorCodes('&', perms.getGroup(pu.getGroup()).getPrefix()) : "§9" + ms.getLang("member");
        final ItemStack head = ms.renameItemStack(online && p.canSee(pl.getPlayer()) ? ms.getHead(pl.getName()) : new ItemStack(Material.SKULL_ITEM), "§f§l" + pl.getName(),
                "§7Spielerrang: " + userrank, "§7Teamrang: " + teamrank);
        inv.setItem(4, head);
        if (ms.getActiveInv(p.getName()) != InvType.USERCONTROL && ms.getActiveInv(p.getName()) != InvType.CONFIRM)
            p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.USERCONTROL);
    }

    private void openReset(final Player p, final OfflinePlayer ply) {
        if (ms.isFullTrusted(p.getName()) == false) {
            handleCommand(p, new String[]{ply.getName()});
            return;
        }
        final Inventory resetinv = ms.getServer().createInventory(null, 45, "§0§lMuxOverview§0 | " + ply.getName());
        resetinv.setItem(4, ms.renameItemStack(new ItemStack(Material.TNT), "§c§lZurücksetzen"));
        resetinv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        resetinv.setItem(19, ms.renameItemStack(new ItemStack(Material.ARMOR_STAND), "§c§lKampfstats zurücksetzen",
                "§7Alle Statistiken (Tode, Kills, Trophäen)", "§7sind dann wie die von neuen Spielern.", "", "§7Klicke, um diese §czurückzusetzen§7."));
        resetinv.setItem(22, ms.renameItemStack(new ItemStack(Material.SAPLING), "§c§lBase zurücksetzen",
                "§7Die gesamte Base des Spielers, und", "§7damit auch ihr Wert wird gelöscht.", "", "§7Klicke, um diese §czurückzusetzen§7."));
        resetinv.setItem(25, ms.renameItemStack(new ItemStack(Material.SULPHUR), "§c§lSpielerdaten zurücksetzen",
                "§7Setze u.a. Inventar und Enderkiste", "§7von diesem Spieler sofort zurück.", "", "§7Klicke, um diese §czurückzusetzen§7."));
        resetinv.setItem(40, ms.renameItemStack(new ItemStack(Material.NAME_TAG), "§c§lTag zurücksetzen", "", "§7Klicke, um diesen §czurückzusetzen§7."));
        final InvType it = ms.getActiveInv(p.getName());
        if (it != InvType.USERCONTROL && it != InvType.CONFIRM) p.closeInventory();
        p.openInventory(resetinv);
        ms.setActiveInv(p.getName(), InvType.USERCONTROL);
    }

    public void openAdministrative(final Player p, final OfflinePlayer ply) {
        if (ms.isTrusted(p.getName()) == false) {
            handleCommand(p, new String[]{ply.getName()});
            return;
        }
        final UUID uuid = ply.getUniqueId();
        final boolean online = ply.isOnline() && ms.isFullTrusted(p.getName());
        final MuxUser u = online ? ms.getMuxUser(ply.getName()) : ms.getDB().loadPlayer(uuid);
        final String plname = (u != null ? u.getName() : ply.getName());
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxOverview§0 | " + plname);
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.REDSTONE), "§f§lAdministrative Optionen",
                "§7Ändere z.B. hier den Rang des Spielers", "§7oder gebe ihm temporäre Baurechte."));
        final MuxRanks perms = ms.getPerms();
        final Player pl = online ? ply.getPlayer() : null;
        final boolean suspended = u != null && u.isTimebanned(), permabanned = u != null && u.isPermban(),
                wgperms = ms.isWGTrusted(plname), weperms = ms.isWETrusted(plname),
                wg = pl != null && perms.hasWorldGuard(pl), we = pl != null && perms.hasWorldEdit(pl);
        inv.setItem(online ? 39 : 37, ms.renameItemStack(suspended ? ms.addGlow(new ItemStack(Material.LAVA_BUCKET)) : new ItemStack(Material.LAVA_BUCKET),
                "§c§lTemporäre Suspendierung", "§7Nur in absoluten Notfällen wie Bugusing", "§7darf diese Aktion angewendet werden.", "",
                permabanned ? "§aDer Spieler ist permanent gesperrt." : suspended ? "§aDer Spieler ist bereits suspendiert." : "§7Klicke, um den Spieler §czu suspendieren§7."));
        final MuxRanks.PermissionsUser pu = perms.getUserData(ply.getUniqueId());
        final String teamrank = pu != null && pu.getTeamGroup() != null ? ChatColor.translateAlternateColorCodes('&', perms.getGroup(pu.getTeamGroup()).getPrefix()) : "§8-",
                userrank = pu != null && pu.getGroup() != null ? ChatColor.translateAlternateColorCodes('&', perms.getGroup(pu.getGroup()).getPrefix()) : "§9" + ms.getLang("member");
        inv.setItem(online ? 25 : 22, ms.renameItemStack(new ItemStack(Material.NAME_TAG), "§f§lRänge verändern",
                "§7Spielerrang: " + userrank, "§7Teamrang: " + teamrank, "", "§fKlicke§7, um sie zu verändern."));
        inv.setItem(online ? 41 : 40, ms.renameItemStack(permabanned ? ms.addGlow(new ItemStack(Material.IRON_FENCE)) : new ItemStack(Material.IRON_FENCE),
                "§4§lPermanent sperren", "",
                permabanned ? "§aDer Spieler ist bereits permanent gesperrt." : "§7Klicke, um den Spieler §4permanent zu sperren§7."));
        inv.setItem(43, ms.renameItemStack(new ItemStack(Material.TNT), "§c§lZurücksetzen", "", "§7Klicke, um den Spieler §czurückzusetzen§7."));
        if (online) {
            inv.setItem(37, ms.renameItemStack(new ItemStack(Material.ANVIL, 1, (byte) 2), "§c§lCrashen", "", "§7Klicke, um den Spieler zu §ccrashen§7."));
            inv.setItem(22, ms.renameItemStack(we || weperms ? ms.addGlow(new ItemStack(Material.WOOD_AXE)) : new ItemStack(Material.WOOD_AXE),
                    "§f§lWorldEdit" + (we || weperms ? " §a§laktiviert" : ""), "§7Damit erhält der Spieler für kurze Zeit die", "§7Möglichkeit, Landschaften zu verändern.", "",
                    weperms ? "§aSein WorldEdit ist die ganze Zeit aktiviert." : "§7Klicke, um sein WorldEdit zu " + (we ? "§cdeaktivieren" : "§aaktivieren") + "§7."));
            inv.setItem(19, ms.renameItemStack(wg ? ms.addGlow(new ItemStack(Material.GRASS)) : new ItemStack(Material.GRASS),
                    "§f§lBaurechte" + (wg ? " §a§laktiviert" : ""), "§7Damit kann der Spieler kurzfristig", "§7in dem Creative Modus überall bauen.", "",
                    wgperms ? "§cDer Spieler hat immer Baurechte." : "§7Klicke, um Baurechte zu " + (wg ? "§cdeaktivieren" : "§aaktivieren") + "§7."));
        }
        final InvType it = ms.getActiveInv(p.getName());
        if (it != InvType.USERCONTROL && it != InvType.HOMES && it != InvType.EXTRAS && it != InvType.SHOP && it != InvType.MUXCHESTS && it != InvType.CONFIRM)
            p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.USERCONTROL);
    }

    public void openDataManagement(final Player p, final OfflinePlayer ply) {
        if (ms.isTrusted(p.getName()) == false) {
            handleCommand(p, new String[]{ply.getName()});
            return;
        }
        final UUID uuid = ply.getUniqueId();
        final boolean online = ply.isOnline();
        final MuxUser u = online ? ms.getMuxUser(ply.getName()) : ms.getDB().loadPlayer(uuid);
        final String plname = (u != null ? u.getName() : ply.getName());
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxOverview§0 | " + plname);
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.BOOKSHELF), "§f§lManagement von Daten",
                "§7Alle Daten wie Extras, Homes oder Kisten", "§7des Spielers können modifiziert werden."));
        inv.setItem(19, ms.renameItemStack(new ItemStack(Material.COMPASS), "§f§lOrte", "", "§fKlicke§7, um die Orte zu sehen."));
        inv.setItem(21, ms.renameItemStack(new ItemStack(Material.STORAGE_MINECART), "§f§lVerkäufe", "", "§fKlicke§7, um die Verkäufe zu sehen."));
        inv.setItem(23, ms.renameItemStack(new ItemStack(Material.CHEST), "§f§lKisten", "", "§fKlicke§7, um die Kisten zu sehen."));
        inv.setItem(25, ms.renameItemStack(new ItemStack(Material.GOLD_BARDING), "§f§lExtras", "", "§fKlicke§7, um die Extras zu sehen."));
        final InvType it = ms.getActiveInv(p.getName());
        if (it != InvType.USERCONTROL && it != InvType.HOMES && it != InvType.EXTRAS && it != InvType.SHOP && it != InvType.MUXCHESTS)
            p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.USERCONTROL);
    }

    private void openUserControl(final Player p, final String str) {
        final OfflinePlayer ply = ms.getPlayer(str, true);
        if (ply == null) {
            ms.showItemBar(p, ms.hnotfound);
            return;
        }
        final UUID uuid = ply.getUniqueId();
        final boolean online = ply.isOnline();
        final MuxUser u = online ? ms.getMuxUser(ply.getName()) : ms.getDB().loadPlayer(uuid);
        final String plname = (u != null ? u.getName() : ply.getName());
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxOverview§0 | " + plname);
        final Player pl = online ? ply.getPlayer() : null;

        final Set<Byte> activeperks = ms.getExtras().getPerks(p.getUniqueId()).getActive();
        final boolean hasfly = pl != null && (ms.hasGlobalFly(pl) || pl.getAllowFlight()),
                fullhunger = pl != null && pl.getFoodLevel() == 20, fullhealth = pl != null && pl.getHealth() == 20D && (pl.getActivePotionEffects().isEmpty() || activeperks.isEmpty() == false),
                creative = pl != null && pl.getGameMode() == GameMode.CREATIVE, cansee = pl != null && p.canSee(pl), trusted = ms.isTrusted(p.getName()), fulltrusted = ms.isFullTrusted(p.getName()),
                globalchat = ms.getChat().hasDisabledChat(plname) == false, chatfilter = u != null && u.getSettings().hasChatFilter(), recording = pl != null && ms.getCreators().isRecording(pl);
        final int voted = ms.hasVoted(ply) ? 1 : ms.getVotes().hasFirstVote(uuid) ? 2 : 0;

        final String ip = u == null ? null : u.getIp().replace("/", "");
        final List<UUID> uuidaccounts = ms.getDB().getUUIDsWithSameIP(ip, plname);
        boolean otheraccsvoted = false;
        for (final UUID acc : uuidaccounts) {
            if (ms.getVotes().hasVoted(acc)) {
                otheraccsvoted = true;
                break;
            }
        }
        final boolean votenotice = otheraccsvoted && ms.hasVoted(p) == false;
        inv.setItem(4, ms.renameItemStack(cansee ? ms.getHead(plname) : new ItemStack(Material.SKULL_ITEM), "§f§l" + plname));

        new BukkitRunnable() {
            @Override
            public void run() {
                final ItemStack head = ms.renameItemStack(cansee ? ms.getHead(plname) : new ItemStack(Material.SKULL_ITEM), "§f§l" + plname);
                final List<String> lore = new ArrayList<>();
                final int vpn = ip == null ? 0 : ms.isVPN(ip);
                if (cansee) {
                    final String status = ms.getPlayerStatus(p, ply, u, true);
                    lore.add("§7Status: " + status + (status.contains("Online") ? " (seit " : " §a(online seit ") + ms.timeToString(System.currentTimeMillis() - (u == null ? ply.getFirstPlayed() : u.getLoginTimestamp()), false) + ")");
                    lore.add(" ");
                    lore.add("§7Version: §f" + getVersion(pl));
                    lore.add("§7EXP: §f" + pl.getTotalExperience() + " (Level " + pl.getLevel() + ")");
                    lore.add("§7Gesundheit: §f" + (int) pl.getHealth() + "/" + (int) pl.getMaxHealth() + "§7 | Hunger: §f" + pl.getFoodLevel() + "/20");
                    lore.add("§7Gamemode: " + (creative ? "§a" : "§c") + pl.getGameMode().toString() + "§7 | Fly: " + (hasfly ? "§aJA" : "§cNEIN"));
                    lore.add("§7Globalchat: " + (globalchat ? "§aAN" : "§cAUS") + "§7 | Chatfilter: " + (chatfilter ? "§aAN" : "§cAUS"));
                    lore.add("§7Neulingschutz: " + (ms.getNewbies().isProtectedNewbie(uuid) ? "§aJA" : "§cNEIN") + (recording ? "§7 | Fokus: §aJA" : ""));
                } else {
                    final String status = ms.getPlayerStatus(p, ply, u, false);
                    lore.add("§7Status: " + status + (status.contains("Offline") ? " (seit " : " §c(offline seit ") + ms.timeToString(System.currentTimeMillis() - ply.getLastPlayed(), false) + ")");
                    lore.add(" ");
                }
                lore.add("§7" + ms.getLang("cmd.uservoted") + ": §7" + (voted == 1 ? "§a" + ms.getLang("langyes").toUpperCase() : voted == 2 ? "§e1 mal" : "§c" + ms.getLang("langno").toUpperCase() + (votenotice ? " §a(aber für Zweitaccount)" : "")));
                final boolean teamrecording = ms.getCreators().isRecording(p);
                if (fulltrusted && teamrecording == false) {
                    final MuxEmails.MuxMailUser mu = pl != null ? ms.getEmails().getUser(uuid) : ms.getEmails().new MuxMailUser(uuid, "", false, null);
                    if (pl == null) ms.getDB().loadMuxMailUser(mu);
                    if (mu.getEmail() != null) {
                        lore.add("§7Email: §f" + mu.getEmail());
                    }
                }
                if (ip != null) {
                    lore.add("§7VPN: " + (vpn == 1 ? "§aJA" : vpn == 2 ? "§e404" : "§cNEIN") + (trusted && teamrecording == false ? "§7 | IP: §f" + ip : ""));
                }
                if (p.hasPermission("muxsystem.whoisaccounts")) {
                    final List<String> accounts = ms.getDB().getPlayersWithSameIP(ip, plname);
                    final int asize = accounts.size();
                    final Server sr = ms.getServer();
                    lore.add("§7Accounts: §f§o" + (accounts.isEmpty() ? "§a§o" + ms.getLang("none") : accounts.stream().limit(3).map(s -> sr.getPlayer(s) != null ? "§a§o" + s : s).collect(Collectors.joining("§7, §f§o"))));
                    if (asize > 3) {
                        lore.add("§f             " + String.format(ms.getLang("cmd.userandmore"), asize - 3) + (asize == 4 ? "s" : "") + "...");
                    }
                    if (accounts.isEmpty() == false) {
                        lore.add("   ");
                        lore.add("§7Klicke, um alle Accounts zu sehen.");
                    }
                }
                inv.setItem(4, ms.addLore(head, lore));
            }
        }.runTaskAsynchronously(ms);

        final ItemStack hunger = ms.renameItemStack(fullhunger ? ms.addGlow(new ItemStack(Material.COOKED_CHICKEN)) : new ItemStack(Material.COOKED_CHICKEN),
                "§f§lHunger sättigen", "§7Damit kann insbesondere Neulingen", "§7mit wenig Hunger geholfen werden.", "",
                fullhunger ? "§aDer Spieler ist schon gesättigt." : "§7Klicke, um den Spieler zu §fsättigen§7.");
        final boolean votelimit = ms.checkLimit(p.getName(), "SUPVOTEGIVE", trusted ? 5 : 3, false);
        final ItemStack vote = ms.renameItemStack(voted == 1 ? ms.addGlow(new ItemStack(Material.IRON_DOOR)) : new ItemStack(Material.IRON_DOOR),
                "§f§lVote freischalten", votenotice ? new String[]{
                        "§7Die Option ist nur für diejenigen, die", "§7zurzeit nicht für sich voten können.", "",
                        "§eSein 2. Account hat bereits gevotet.", "", (votelimit ? "§cDu hast bereits zu viele Votes verteilt." : "§7Klicke, um dennoch für ihn zu §fvoten§7.")} :
                        new String[]{"§7Die Option ist nur für diejenigen, die", "§7zurzeit nicht für sich voten können.", "",
                                voted == 1 ? "§aDieser Spieler hat bereits gevotet." : votelimit ? "§cDu hast bereits zu viele Votes verteilt." : "§7Klicke, um für den Spieler zu §fvoten§7."});
        final List<String> hlore = new ArrayList<>(Arrays.asList("§7Hier werden alle Ereignisse gelistet,", "§7die mit dem Spieler zusammenhängen.", ""));
        if (u != null) {
            if (u.isTimeMuted())
                hlore.add("§eChat verlangsamt (noch " + ms.getTime((int) ((u.getUnmuteTime() - System.currentTimeMillis()) / 1000L)) + ")");
            if (u.isPermMute()) hlore.add("§9Der Spieler ist permanent gestummt.");
            if (u.isPvPbanned())
                hlore.add("§6PvP Ausschluss (noch " + ms.getTime((int) ((u.getPvPUnbanTime() - System.currentTimeMillis()) / 1000L)) + ")");
            if (ms.getAntiBot().isMineBanned(uuid)) hlore.add("§fMining Verbot (bis Neustart)");
            if (u.isTimebanned())
                hlore.add("§cSuspendierung (noch " + ms.getTime((int) ((u.getUnbanTime() - System.currentTimeMillis()) / 1000L)) + ")");
            if (u.isPermban()) hlore.add("§4Der Spieler ist permanent gesperrt.");
            if (hlore.size() > 3) hlore.add(" ");
        }
        hlore.add("§fKlicke§7, um den Verlauf einzusehen.");
        final ItemStack history = ms.renameItemStack(hlore.size() > 4 ? ms.addGlow(new ItemStack(Material.BOOK)) : new ItemStack(Material.BOOK), "§f§lVerlauf", hlore);
        final ItemStack chatreports = ms.renameItemStack(new ItemStack(Material.SIGN),
                "§f§lChatverhalten", "§7Sollte dieser Spieler negativ auffallen,", "§7dann findest du hier die Beschwerden.", "",
                "§fKlicke§7, um die Chatreports zu sehen.");
        final ItemStack heal = ms.renameItemStack(fullhealth ? new ItemStack(Material.GOLDEN_APPLE, 1, (byte) 1) : new ItemStack(Material.GOLDEN_APPLE),
                "§f§lSofort heilen", "§7Dies entfernt alle Effekte, während", "§7komplett geheilt und gesättigt wird.", "",
                fullhealth ? "§aDieser Spieler ist in heilem Zustand." : "§7Klicke, um den Spieler zu §fheilen§7.");
        final ItemStack tospawn = ms.renameItemStack(new ItemStack(Material.BEACON), "§f§lZum Spawn teleportieren",
                "§7In Notfällen kann der Spieler direkt", "§7zum Hauptpunkt teleportiert werden.", "", "§7Klicke, um ihn §fzum Spawn zu bringen.");
        final ItemStack cheatreports = ms.renameItemStack(new ItemStack(Material.IRON_SWORD),
                "§f§lKampfverhalten", "§7Wenn andere Spieler ihn melden, dann", "§7kannst du den Ursachen nachgehen.", "",
                "§fKlicke§7, um die Cheatreports zu sehen.");
        final ItemStack fly = ms.renameItemStack(hasfly || creative ? ms.addGlow(new ItemStack(Material.FEATHER)) : new ItemStack(Material.FEATHER),
                "§f§lFlugmodus" + (hasfly ? " §a§laktiviert" : ""), "§7Man kann temporär fliegen und ist", "§7zugleich vor Angriffen geschützt.", "",
                pl != null && pl.hasPermission("muxsystem.fly") ? "§aDieser Spieler hat darauf Rechte." : creative ? "§aDer Spieler hat bereits Creative-Modus." : hasfly ? "§7Klicke, um ihn nun zu §cdeaktivieren§7." : "§7Klicke, um ihn kurz zu §aaktivieren§7.");
        final ItemStack homes = ms.renameItemStack(new ItemStack(Material.COMPASS), "§f§lOrte auflisten",
                "§7Der direkte Zugriff auf die Base oder", "§7Homes des Spielers sind hier möglich.", "", "§fKlicke§7, um alle seine Orte aufzulisten.");
        final boolean suspended = u != null && u.isTimebanned(), permabanned = u != null && u.isPermban();
        final ItemStack tempban = ms.renameItemStack(suspended ? ms.addGlow(new ItemStack(Material.LAVA_BUCKET)) : new ItemStack(Material.LAVA_BUCKET),
                "§c§lTemporäre Suspendierung", "§7Nur in absoluten Notfällen wie Bugusing", "§7darf diese Aktion angewendet werden.", "",
                permabanned ? "§aDer Spieler ist permanent gesperrt." : suspended ? "§aDer Spieler ist bereits suspendiert." : "§7Klicke, um den Spieler §czu suspendieren§7.");
        final List<String> lastlore = new ArrayList<>();
        final Location l = ply.getName() == null ? null : new MuxOfflinePlayer(ply).getLocation();
        if (l != null) {
            lastlore.add("§7Der Spieler befand sich zuletzt dort:");
            lastlore.add("§7X: " + l.getBlockX() + " Y: " + l.getBlockY() + " Z: " + l.getBlockZ() + " (" + l.getWorld().getName() + ")");
            lastlore.add("");
            lastlore.add("§7Klicke, um dich dahin zu §fteleportieren§7.");
        } else {
            lastlore.add("§7Normalerweise stünde hier der Ort,");
            lastlore.add("§7wo sich der Spieler zuletzt befand.");
            lastlore.add("");
            lastlore.add("§aDer Spieler war bisher nicht online.");
        }
        final ItemStack lastlocation = ms.renameItemStack(l == null ? ms.addGlow(new ItemStack(Material.ENDER_PEARL)) : new ItemStack(Material.ENDER_PEARL),
                "§f§lLetzte Position", lastlore);
        final ItemStack administrative = ms.renameItemStack(new ItemStack(Material.REDSTONE), "§f§lAdministrative Optionen",
                "§7Ändere z.B. hier den Rang des Spielers", "§7oder gebe ihm temporäre Baurechte.", "", "§7Klicke, um alle die Optionen zu §fsehen§7.");
        final ItemStack datamanage = ms.renameItemStack(new ItemStack(Material.BOOKSHELF), "§f§lManagement von Daten",
                "§7Alle Daten wie Extras, Homes oder Kisten", "§7des Spielers können modifiziert werden.", "", "§7Klicke, um die Daten von ihn zu §fmanagen§7.");
        final MuxRanks.PermissionsGroup group = ms.getPerms().getGroupOf(p.getUniqueId());
        final String rank = group == null ? "Default" : group.getName();
        if ((trusted == false || p.isOp() == false) && (rank.contains("Dev") || rank.contains("Builder")) && rank.endsWith("+")) {
            if (cansee) {
                if (ms.getServer().hasWhitelist()) {
                    inv.setItem(8, ms.renameItemStack(new ItemStack(Material.IRON_BOOTS), "§9§lVom Server kicken",
                            "§7Diese Option siehst du momentan nur,", "§7weil Wartungsarbeiten aktiviert sind.", "", "§7Klicke, um ihn §9vom Server zu kicken§7."));
                }
                inv.setItem(22, tospawn);
                inv.setItem(37, fly);
            } else {
                inv.setItem(37, lastlocation);
            }
            inv.setItem(19, vote);
            inv.setItem(25, history);
            inv.setItem(40, homes);
            inv.setItem(43, cheatreports);
        } else if (p.isOp()) {
            if (cansee) {
                final Event event = ms.getEvent(pl);
                if (event != null && ms.getEvents().isKickable(event)) {
                    inv.setItem(8, ms.renameItemStack(new ItemStack(Material.LEATHER_BOOTS), "§d§lVom Event kicken",
                            "§7Falls der Spieler mit unfairen Mitteln", "§7teilnimmt, kannst du in ausschließen.", "", "§7Klicke, um ihn §dvom Event zu kicken§7."));
                } else if (ms.getServer().hasWhitelist()) {
                    inv.setItem(8, ms.renameItemStack(new ItemStack(Material.IRON_BOOTS), "§9§lVom Server kicken",
                            "§7Diese Option siehst du momentan nur,", "§7weil Wartungsarbeiten aktiviert sind.", "", "§7Klicke, um ihn §9vom Server zu kicken§7."));
                }
                inv.setItem(20, hunger);
                inv.setItem(21, heal);
                inv.setItem(22, tospawn);
                inv.setItem(23, chatreports);
                inv.setItem(24, cheatreports);
                inv.setItem(37, fly);
            } else {
                inv.setItem(21, chatreports);
                inv.setItem(23, cheatreports);
                inv.setItem(37, lastlocation);
            }
            inv.setItem(19, vote);
            inv.setItem(25, history);
            if (trusted) inv.setItem(40, administrative);
            else inv.setItem(40, homes);
            if (trusted) inv.setItem(43, datamanage);
            else inv.setItem(43, tempban);
        } else if (p.hasPermission("muxsystem.supplus")) {
            if (online && ms.getSupport().getSupporterOf(pl) == p) {
                inv.setItem(19, hunger);
                inv.setItem(21, vote);
                inv.setItem(23, chatreports);
                inv.setItem(25, history);
            } else {
                inv.setItem(20, chatreports);
                inv.setItem(24, history);
            }
        } else if (p.hasPermission("muxsystem.team")) {
            if (online && ms.getSupport().getSupporterOf(pl) == p) {
                inv.setItem(19, hunger);
                inv.setItem(22, vote);
                inv.setItem(25, history);
            } else {
                inv.setItem(22, history);
            }
        }
        final InvType it = ms.getActiveInv(p.getName());
        if (it != InvType.USERCONTROL && it != InvType.CONFIRM && it != InvType.HISTORY && it != InvType.REPORTS && it != InvType.HOMES)
            p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.USERCONTROL);
    }

    private void resetPlayer(final Player p, final String plname) {
        final OfflinePlayer pl = ms.getPlayer(plname);
        if (pl == null) {
            ms.showItemBar(p, ms.hnotfound);
            return;
        } else if (pl.isOnline()) {
            ms.removeFromBattle(plname);
            pl.getPlayer().kickPlayer("§cEs gab einen Fehler. Bitte den Server neu beitreten!");
        }
        final File file = new File(ms.getFarmWorld().getWorldFolder() + "/playerdata/" + pl.getUniqueId().toString() + ".dat");
        p.closeInventory();
        if (file.delete() == false) {
            ms.showItemBar(p, "§cDer Spieler §6" + plname + " §ckonnte nicht gelöscht werden.");
            return;
        }
        ms.getHistory().addHistory(pl.getUniqueId(), p.getUniqueId(), "TEAMACTION", "RESET", null);
        ms.showItemBar(p, "§fDer Spieler §6" + plname + " §fwurde erfolgreich §azurückgesetzt§f.");
    }

    private String getVersion(final Player p) {
        try {
            return PlayerVersion.getVersionFromRaw(this.protocolVersionMethod == null ? 47 : (Integer) this.protocolVersionMethod.invoke(protocolManager, p.getUniqueId())).getVersionName();
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private void resetBase(final Player p, final String plname) {
        final OfflinePlayer pl = ms.getPlayer(plname);
        if (pl == null) {
            ms.showItemBar(p, ms.hnotfound);
            return;
        } else if (ms.getBase().hasBase(pl.getUniqueId()) == false) {
            ms.showItemBar(p, "§cDer Spieler besitzt keine Base.");
            return;
        }
        final PlayerBase b = ms.getBase().getFromUUID(pl.getUniqueId());
        final boolean r = b.hasResetToday();
        b.reset();
        b.setResetToday(r);
        ms.getHistory().addHistory(pl.getUniqueId(), p.getUniqueId(), "TEAMACTION", "BASE RESET", null);
        ms.showItemBar(p, "§fDie Base von §6" + plname + " §fwurde erfolgreich §azurückgesetzt§f.");
    }

    private void resetBaseGrief(final Player p, final String plname) {
        final OfflinePlayer pl = ms.getPlayer(plname);
        if (pl == null) {
            ms.showItemBar(p, ms.hnotfound);
            return;
        } else if (ms.getBase().hasBase(pl.getUniqueId()) == false) {
            ms.showItemBar(p, "§cDer Spieler besitzt keine Base.");
            return;
        }
        final PlayerBase b = ms.getBase().getFromUUID(pl.getUniqueId());
        if (b.isGriefed() || b.isGriefable()) {
            ms.showItemBar(p, "§cDie Base wurde bereits gegrieft.");
            return;
        } else if (ms.getBase().isEnabled() == false) {
            ms.showItemBar(p, "§cDie Basen sind momentan deaktiviert.");
            return;
        } else if (ms.getServer().hasWhitelist()) {
            ms.showItemBar(p, "§cWartungen sind gerade aktiviert.");
            return;
        } else if (pl.isOnline()) {
            ms.showItemBar(p, "§cDer Spieler ist gerade online.");
            return;
        }
        b.setLastPlayed(System.currentTimeMillis() - 259200000L);
        ms.getHistory().addHistory(pl.getUniqueId(), p.getUniqueId(), "TEAMACTION", "BASE RESET", null);
        ms.showItemBar(p, "§fDie Base von §6" + plname + " §fwird in Kürze §agriefbar§f.");
    }

    private void resetPvPStats(final Player p, final String plname) {
        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
        final OfflinePlayer pl = ms.getPlayer(plname);
        if (pl == null) {
            ms.showItemBar(p, ms.hnotfound);
            return;
        }
        final String name = pl.getName();
        final MuxUser u = pl.isOnline() ? ms.getMuxUser(name) : ms.getDB().loadPlayer(pl.getUniqueId());
        if (u == null) {
            ms.showItemBar(p, ms.hnotfound);
            return;
        }
        u.resetStats();
        ms.saveMuxUser(u);
        if (pl.isOnline()) {
            ms.sendScoreboard(pl.getPlayer());
        }
        ms.getHistory().addHistory(pl.getUniqueId(), p.getUniqueId(), "TEAMACTION", "STATS RESET", null);
        ms.showItemBar(p, "§fDie Kampfstats von §6" + name + " §fwurden §azurückgesetzt§f.");
    }

    private boolean toggleWE(final Player p, final String plname) {
        final Player victim = ms.getServer().getPlayer(plname);
        if (victim == null || p.canSee(victim) == false) {
            ms.showItemBar(p, ms.hnotonline);
            return true;
        }
        boolean add = true;
        if (ms.isWETrusted(plname)) {
            return false;
        } else if (ms.getPerms().addWorldEditPerms(plname) != null) {
            ms.getPerms().removeWorldEdit(plname, true);
            add = false;
        }
        ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "WORLDEDIT", add ? "ADD" : "REMOVE");
        openAdministrative(p, victim);
        return true;
    }

    private boolean toggleWG(final Player p, final String plname) {
        final Player victim = ms.getServer().getPlayer(plname);
        if (victim == null || p.canSee(victim) == false) {
            ms.showItemBar(p, ms.hnotonline);
            return true;
        }
        boolean add;
        if (ms.isWGTrusted(plname)) {
            return false;
        } else if (ms.getPerms().addWorldGuardPerms(plname) != null) {
            ms.getPerms().removeWorldGuard(plname, true);
            victim.setGameMode(GameMode.SURVIVAL);
            add = false;
        } else {
            add = true;
            victim.setGameMode(GameMode.CREATIVE);
        }
        ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "BUILD", add ? "ADD" : "REMOVE");
        openAdministrative(p, victim);
        return true;
    }

    private void tempBan(final Player p, final String plname) {
        p.closeInventory();
        ms.getBans().applyTimeBan(p, plname, 172800, "General", "Verdächtige Aktivität");
    }

    private void permBan(final Player p, final UUID victim) {
        p.closeInventory();
        ms.getBans().applyPermaBan(p, victim, "Servermissbrauch");
    }

    private boolean kickPlayer(final Player p, final Player pl) {
        if (ms.getServer().hasWhitelist() == false) {
            ms.showItemBar(pl, "§cDer Befehl ist nur während Wartungen verfügbar.");
            return false;
        } else if (pl == null || p.canSee(pl) == false) {
            ms.showItemBar(p, ms.hnotonline);
            return false;
        } else if (ms.getBans().cantPunish(p, pl, false)) {
            return false;
        }
        final String reason = "Wartungen";
        ms.getDB().addBanEntry(pl.getUniqueId(), pl.getName(), "Kick", "", reason, p.getName());
        ms.removeFromBattle(pl.getName());
        pl.setMetadata("systemkick", new FixedMetadataValue(ms, true));
        pl.kickPlayer("§7Du wurdest vom Server gekickt. §7Grund: §b" + reason);
        ms.broadcastMessage("§7Der Spieler §9" + pl.getName() + "§7 wurde von " + p.getName() + " gekickt.", "muxsystem.team", MuxSystem.Priority.NORMAL);
        p.closeInventory();
        return true;
    }

    private boolean giveVote(final Player p, final String plname) {
        final OfflinePlayer pl = ms.getPlayer(plname);
        if (pl == null) {
            ms.showItemBar(p, ms.hnotfound);
            return true;
        } else if (ms.hasVoted(pl) || ms.checkLimit(p.getName(), "SUPVOTEGIVE", ms.isTrusted(p.getName()) ? 5 : 3, false)) {
            return false;
        }
        p.closeInventory();
        ms.getVotes().onVote(plname, true);
        ms.getVotes().onVote(plname, true);
        ms.checkLimit(p.getName(), "SUPVOTEGIVE", ms.isTrusted(p.getName()) ? 5 : 3, true);
        ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketGiveVote(pl.getUniqueId()));
        ms.getHistory().addHistory(pl.getUniqueId(), p.getUniqueId(), "TEAMACTION", "VOTEGIVE", null);
        ms.showItemBar(p, "§fDu hast dem Spieler §6" + pl.getName() + " §feinen Vote §ahinzugefügt§f.");
        return true;
    }

    private void crashPlayer(final Player p, final String plname) {
        if (ms.isFullTrusted(p.getName()) == false) {
            return;
        }
        final Player victim = ms.getServer().getPlayer(plname);
        if (victim == null || p.canSee(victim) == false) {
            ms.showItemBar(p, ms.hnotonline);
            return;
        } else if (ms.getBans().cantPunish(p, victim, false)) {
            return;
        }
        ms.removeFromBattle(victim.getName());
        ms.getHistory().addHistory(victim.getUniqueId(), p.getUniqueId(), "TEAMACTION", "CRASH", null);
        final EntityPlayer ep = ((CraftPlayer) victim).getHandle();
        final DataWatcher w = new DataWatcher(ep);
        w.a(2, 10);
        w.a(10, 10);
        ep.playerConnection.sendPacket(new net.minecraft.server.v1_8_R3.PacketPlayOutEntityMetadata(ep.getId(), w, true));
        ms.showItemBar(p, "§fDer Client wurde §agecrasht§f.");
    }

    private void kickFromEvent(final Player p, final String plname) {
        final Player victim = ms.getServer().getPlayer(plname);
        if (victim == null || p.canSee(victim) == false) {
            ms.showItemBar(p, ms.hnotonline);
            return;
        }
        final Event ev = ms.getEvent(victim);
        if (ev == null) {
            ms.showItemBar(p, "§cDer Spieler ist nicht im Event.");
            return;
        } else if (ms.getEvents().isKickable(ev) == false) {
            ms.showItemBar(p, "§cDie Aktion ist nicht für dieses Event verfügbar.");
            return;
        }
        ms.getEvents().quitEvent(victim, false);
        ms.showItemBar(victim, "§fDu wurdest vom Event §causgeschlossen§f.");
        ms.getHistory().addHistory(victim.getUniqueId(), p.getUniqueId(), "TEAMACTION", "EVENT KICK", ev.getName());
        ms.showItemBar(p, "§fDer Spieler §d" + victim.getName() + " §fwurde vom Event §causgeschlossen§f.");
        ms.broadcastMessage("§dDer Spieler §7" + victim.getName() + "§d wurde vom Event gekickt.", "muxsystem.team", MuxSystem.Priority.NORMAL);
        p.closeInventory();
    }

    private boolean toSpawn(final Player p, final String plname) {
        final Player victim = ms.getServer().getPlayer(plname);
        if (victim == null || p.canSee(victim) == false) {
            ms.showItemBar(p, ms.hnotonline);
            return false;
        } else if (ms.getBans().cantPunish(p, victim, false)) {
            return false;
        } else if (ms.inBattle(plname, victim.getLocation()) && ms.isTrusted(p.getName()) == false) {
            ms.showItemBar(p, ms.hnotinfight);
            return false;
        }
        ms.forcePlayer(victim, ms.getGameSpawn());
        ms.showItemBar(p, "§fDu hast §6" + plname + "§f erfolgreich zum §6Spawn§f geschickt.");
        ms.getHistory().addHistory(victim.getUniqueId(), p.getUniqueId(), "TEAMACTION", "SPAWNTP", null);
        return true;
    }

    private boolean feed(final Player p, final String plname) {
        final Player victim = ms.getServer().getPlayer(plname);
        if (victim == null || p.canSee(victim) == false) {
            ms.showItemBar(p, ms.hnotonline);
            return true;
        } else if (victim.getFoodLevel() == 20) {
            return false;
        }
        ms.feed(victim);
        ms.showItemBar(victim, "§fDein Hunger wurde von §6" + p.getName() + " §fgestillt.");
        ms.getHistory().addHistory(victim.getUniqueId(), p.getUniqueId(), "TEAMACTION", "FEED", null);
        openUserControl(p, plname);
        return true;
    }

    private boolean heal(final Player p, final String plname) {
        final Player victim = ms.getServer().getPlayer(plname);
        if (victim == null || p.canSee(victim) == false) {
            ms.showItemBar(p, ms.hnotonline);
            return true;
        }
        final Set<Byte> activeperks = ms.getExtras().getPerks(p.getUniqueId()).getActive();
        if (victim.getHealth() == 20D && (victim.getActivePotionEffects().isEmpty() || activeperks.isEmpty() == false)) {
            return false;
        }
        ms.heal(victim);
        ms.showItemBar(victim, "§fDu wurdest von §6" + p.getName() + " §fsoeben §ageheilt§f.");
        ms.getHistory().addHistory(victim.getUniqueId(), p.getUniqueId(), "TEAMACTION", "HEAL", null);
        openUserControl(p, plname);
        return true;
    }

    private boolean toggleFly(final Player p, final String plname) {
        final Player victim = ms.getServer().getPlayer(plname);
        if (victim == null || p.canSee(victim) == false) {
            ms.showItemBar(p, ms.hnotonline);
            return true;
        } else if (victim.getGameMode() == GameMode.CREATIVE || victim.hasPermission("muxsystem.fly")) {
            return false;
        }
        final boolean flying = ms.removeGlobalFly(plname);
        victim.setAllowFlight(flying == false);
        victim.setFlying(flying == false);
        if (flying) {
            ms.showItemBar(victim, "§fDein Flugmodus wurde §cdeaktiviert§f.");
            ms.removeGodMode(plname);
        } else {
            ms.getHistory().addHistory(victim.getUniqueId(), p.getUniqueId(), "TEAMACTION", "FLY", null);
            ms.addGlobalFly(plname);
            ms.addGodMode(plname);
            ms.showItemBar(victim, "§fDein Flugmodus wurde §aaktiviert§f.");
        }
        openUserControl(p, plname);
        return true;
    }

    public enum PlayerVersion {
        v1_8(47, "1.8.x"),
        v1_9(107, "1.9"),
        v1_9_1(108, "1.9.1"),
        v1_9_2(109, "1.9.2"),
        v1_9_3(110, "1.9.3(.4)"),
        v1_10(210, "1.10.x"),
        v1_11(315, "1.11"),
        v1_11_2(316, "1.11.2"),
        v1_12(335, "1.12"),
        v1_12_1(338, "1.12.2"),
        v1_12_2(340, "1.12.2"),
        v1_13(393, "1.13"),
        v1_13_1(401, "1.13.1"),
        v1_13_2(404, "1.13.4"),
        v1_14(477, "1.14"),
        v1_14_1(480, "1.14.1"),
        v1_14_2(485, "1.14.2"),
        v1_14_3(490, "1.14.3"),
        v1_14_4(498, "1.14.3"),
        v1_15(573, "1.15"),
        v1_15_1(575, "1.15.1"),
        v1_15_2(578, "1.15.2"),
        v1_16(735, "1.16"),
        v1_16_1(735, "1.16.1"),
        v1_16_2(751, "1.16.2"),
        v1_16_3(753, "1.16.3"),
        v1_16_4(754, "1.16.4/1.16.5"),
        v1_17(755, "1.17"),
        v1_17_1(756, "1.17.1"),
        v1_18(757, "1.18(.1)"),
        v1_18_2(758, "1.18.2"),
        v1_19(759, "1.19"),
        v1_19_2(760, "1.19.2"),
        v1_19_3(761, "1.19.3"),
        v1_19_4(762, "1.19.3"),
        ;

        private final int rawVersion;
        private final String versionName;

        PlayerVersion(int rawVersion, String versionName) {
            this.rawVersion = rawVersion;
            this.versionName = versionName;
        }

        public static PlayerVersion getVersionFromRaw(Integer input) {
            System.out.println("getting from " + input);
            return Arrays.stream(values())
                    .filter(playerVersion -> playerVersion.rawVersion == input)
                    .findFirst()
                    .orElse(v1_8);
        }

        public int getRawVersion() {
            return rawVersion;
        }

        public String getVersionName() {
            return versionName;
        }
    }
}