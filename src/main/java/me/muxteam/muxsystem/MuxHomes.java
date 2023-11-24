package me.muxteam.muxsystem;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.muxteam.base.PlayerBase;
import me.muxteam.basic.ConfirmInventory;
import me.muxteam.basic.MuxActions.PlayerAction;
import me.muxteam.basic.MuxAnvil;
import me.muxteam.basic.MuxLocation;
import me.muxteam.basic.Pageifier;
import me.muxteam.muxsystem.MuxInventory.InvType;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class MuxHomes {
    private MuxSystem ms;
    private final Set<Player> overwrite = new HashSet<>();

    public MuxHomes(final MuxSystem ms) {
        this.ms = ms;
    }

    public void close() {
        this.ms = null;
    }

    public boolean handleSetHome(final Player p, final String[] args) {
        String homename = "home";
        MuxUser u = ms.getMuxUser(p.getName());
        final short homes = (short) (p.hasPermission("muxsystem.morehomes.unlimited") ? 100 : p.hasPermission("muxsystem.morehomes.elite") ? 10 : p.hasPermission("muxsystem.morehomes.vip") ? 5 : 1);
        if (args.length == 1 || p.isOp() == false && args.length != 0) {
            if (homes > 1) homename = args[0].toLowerCase().trim();
            if (u.getHomes().size() >= homes) {
                tooManyHomes(p, homes);
                return true;
            }
        } else if (args.length == 2) {
            homename = args[1].toLowerCase().trim();
            final OfflinePlayer victim = ms.getPlayer(args[0]);
            if (victim == null) {
                ms.showItemBar(p, ms.hnotfound);
                return true;
            } else if (victim.isOnline()) {
                u = ms.getMuxUser(victim.getName());
            } else {
                u = ms.getDB().loadPlayer(victim.getUniqueId());
                if (u != null) u.setHomes(ms.getDB().loadHomes(victim.getUniqueId()));
            }
            if (u == null) {
                ms.showItemBar(p, ms.hnotfound);
                return true;
            }
            ms.getHistory().addHistory(victim.getUniqueId(), p.getUniqueId(), "TEAMACTION", "SETHOME", homename);
        }
        if (homename.length() > 25) {
            ms.showItemBar(p, "§cDer Name des Homes ist viel zu lang.");
            return true;
        } else if (ms.notSafeGSON(homename)) {
            ms.showItemBar(p, "§cDas Home darf keine spezielle Zeichen enthalten.");
            return true;
        }
        final Location l = p.getLocation();
        final ProtectedRegion fspawnrg = ms.getFullSpawnRegion();
        final PlayerBase baseloc = ms.getBase().getCurrentLocations().get(p);
        final String wname = l.getWorld().getName();
        final boolean defhome = homename.equals("home"), inspawn = l.getWorld().equals(ms.getGameSpawn().getWorld()) && fspawnrg != null && fspawnrg.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ()),
                overnether = l.getWorld().getEnvironment() == Environment.NETHER && l.getY() >= 127.0D, baseuntrusted = baseloc != null && baseloc.isTrusted(p) == false;
        if ((overnether || wname.equals("warps") || wname.equals("MuxCasino") || l.getWorld().getEnvironment() == Environment.THE_END || baseuntrusted || inspawn || l.getY() < 1D) && p.isOp() == false) {
            ms.showItemBar(p, "§cDu kannst hier kein Zuhause setzen.");
            return true;
        } else if (defhome && u.getHome("home") == null && u.getHomes().size() >= homes) {
            tooManyHomes(p, homes);
            return true;
        }
        if (u.getHomes().size() == 0) {
            homename = "home";
            ms.getNewbies().handleNewbieSetHome(p);
        }
        if (u.getHome(homename) != null) { // Prevent accidental overwrite
            if (overwrite.remove(p) == false) {
                new ConfirmInventory(ms, pl -> {
                    pl.closeInventory();
                    handleSetHome(p, args);
                }, pl -> {
                    pl.closeInventory();
                    overwrite.remove(p);
                }).show(p, "§0§lZuhause überschreiben", "§aBestätigen", "§cAbbrechen",
                        new ItemStack(Material.WORKBENCH), "§f§l" + homename);
                overwrite.add(p);
                return true;
            }
        }
        ms.showItemBar(p, "§fDein Zuhause " + (args.length > 0 ? "'§6" + homename + "§f' " : "") + "§fwurde " + (u.getHome(homename) != null ? "§eüberschrieben" : "§agesetzt") + "§f.");
        u.addHome(homename, new Home(l));
        if (args.length == 2 && p.isOp()) ms.saveMuxUser(u);
        p.playSound(p.getLocation(), Sound.PISTON_EXTEND, 1F, 1F);
        return true;
    }

    public boolean handleHomes(final Player p, final String[] args, final String label) {
        MuxUser u = ms.getMuxUser(p.getName());
        Set<String> homes = u.getHomes().keySet();
        if (ms.hasVoted(p) == false && homes.isEmpty() == false) {
            ms.sendNoVoteMessage(p);
            return true;
        } else if (ms.inBattle(p.getName(), p.getLocation())) {
            ms.showItemBar(p, ms.hnotinfight);
            return true;
        } else if (label.equalsIgnoreCase("delhome")) {
            if (p.hasPermission("muxsystem.morehomes.vip") == false) {
                ms.showItemBar(p, "§cÜberschreibe dein Zuhause mit §f/sethome§c.");
                return true;
            }
            p.performCommand("home");
            return true;
        }
        List<PlayerBase> pbList = ms.getBase().getTrusted(u.getUUID());
        final Map<PlayerBase, Short> saplingIds = new HashMap<>();
        final boolean isop = p.isOp(), viewotherhomes = isop && (label.equalsIgnoreCase("homes") || args.length >= 2);
        String homename = "", tpmessage = "", tpmessage2 = "";
        boolean places = pbList.size() > 1;
        int page = 0;
        if (args.length == 0) {
            if (homes.size() == 1 && pbList.stream().allMatch(playerBase -> playerBase.getOwner().equals(p.getUniqueId())))
                homename = (String) homes.toArray()[0];
            tpmessage = "§fDu wurdest zu §6deinem Zuhause §fteleportiert.";
            tpmessage2 = "§fTeleportiere zu §6deinem Zuhause§f...";
        } else if (args.length == 1 && viewotherhomes == false) {
            homename = args[0].toLowerCase();
            tpmessage = "§fDu wurdest zum Zuhause '§6" + homename + "§f' teleportiert.";
            tpmessage2 = "§fTeleportiere zum Zuhause '§6" + homename + "§f'...";
        } else if (viewotherhomes) {
            if (args.length > 1) homename = args[1].toLowerCase();
            final OfflinePlayer victim = ms.getPlayer(args[0]);
            if (victim == null) {
                ms.showItemBar(p, ms.hnotfound);
                return true;
            }
            if (victim.isOnline()) {
                u = ms.getMuxUser(victim.getName());
            } else {
                u = ms.getDB().loadPlayer(victim.getUniqueId());
                if (u != null) u.setHomes(ms.getDB().loadHomes(victim.getUniqueId()));
            }
            if (u == null) {
                ms.showItemBar(p, ms.hnotfound);
                return true;
            }
            pbList = ms.getBase().getTrusted(u.getUUID());
            places = pbList.size() > 1;
            homes = u.getHomes().keySet();
            tpmessage = "§fDu wurdest zum Zuhause §6'" + homename + "' §fvon §f" + victim.getName() + " §fteleportiert.";
        }
        if ((args.length > 1 && args[0].equals("page")) || (args.length > 2 && args[1].equals("page"))) {
            final int a = args[0].equals("page") ? 1 : 2;
            page = ms.isNumeric(args[a]) ? Integer.parseInt(args[a]) : -2;
            if (page == -2 || page > 89) {
                page = 0;
            }
        }
        final Home h = u.getHome(homename);
        if (homename.length() > 0 && h != null) {
            final Location l = h.getLocation();
            if (l == null) {
                ms.showItemBar(p, "§cFehler: Welt existiert nicht.");
                return true;
            } else if (l.getWorld().getName().equals(ms.getBase().getWorld().getName()) && ms.getBase().isEnabled() == false && p.isOp() == false) {
                ms.showItemBar(p, "§cDie Basen sind momentan deaktiviert.");
                return true;
            }
            if (viewotherhomes) {
                ms.getHistory().addHistory(p.getUniqueId(), u.getUUID(), "HOME", homename, ms.blockLocationToStringNoYawPitch(l));
            }
            if (p.hasPermission("muxsystem.notpcooldown")) {
                ms.setLastLocation(p);
                ms.teleportPlayer(p, l);
                ms.showItemBar(p, tpmessage);
                if (viewotherhomes == false)
                    ms.getHistory().addHistory(p.getUniqueId(), null, "HOME", homename, ms.blockLocationToStringNoYawPitch(l));
                return true;
            } else if (ms.startTPCountdown(p, l)) {
                ms.showItemBar(p, tpmessage2);
                if (viewotherhomes == false)
                    ms.getHistory().addTeleportCache(p, p.getUniqueId(), null, "HOME", homename, ms.blockLocationToStringNoYawPitch(l));
            }
            return true;
        } else if (args.length == 1 && viewotherhomes == false) {
            ms.showItemBar(p, "§cDieses Zuhause existiert nicht.");
            return true;
        } else if (homes.isEmpty() && places == false) {
            if (viewotherhomes) {
                ms.showItemBar(p, "§cDer Spieler hat noch kein Zuhause gesetzt.");
            } else {
                ms.resetLimit("RANDOMTELEPORT");
                p.performCommand("rtp");
            }
            return true;
        }
        final Pageifier<ItemStack> pages = new Pageifier<>(27);
        short saplingId = 0;
        final Server sr = ms.getServer();
        final PlayerBase mainbase = ms.getBase().getFromUUID(u.getUUID());
        if (viewotherhomes && args.length > 0 && mainbase != null) {
            final List<UUID> trusted = mainbase.getTrusted();
            final String teambase = "§7Teambase mit " + trusted.stream().map(uuid -> "§f" + ms.getServer().getOfflinePlayer(uuid).getName() + "§7").collect(Collectors.joining(", "));
            final List<String> lore = trusted.isEmpty() ? new ArrayList<>() : new ArrayList<>(ms.getLoreFromLongText(teambase, "§f"));
            lore.addAll(Arrays.asList("", "§7Linksklick, um dich zu §fteleportieren§7."));
            pages.addItem(ms.renameItemStack(new ItemStack(Material.SAPLING), "§a§lBase", lore));
        }
        for (final PlayerBase base : pbList) {
            if (base.getOwner().equals(u.getUUID())) continue;
            final OfflinePlayer op = sr.getOfflinePlayer(base.getOwner());
            saplingId++;
            if (u.getUUID().equals(p.getUniqueId()))
                pages.addItem(ms.renameItemStack(new ItemStack(Material.SAPLING, 1, saplingId),
                        "§2§lBase von " + op.getName(), "",
                        "§7Linksklick, um dich zu §fteleportieren§7.",
                        "§7Rechtsklick, um Zugang zu §centfernen§7."));
            else
                pages.addItem(ms.renameItemStack(new ItemStack(Material.SAPLING, 1, saplingId),
                        "§2§lBase von " + op.getName(), "",
                        "§7Linksklick, um dich zu §fteleportieren§7."));
            saplingIds.put(base, saplingId);
        }
        for (final String home : homes) {
            final Home ho = u.getHome(home);
            final Location hl = ho.getLocation();
            final String lowerw = ho.getWorld().toLowerCase();
            final boolean nether = lowerw.contains("nether"), base = lowerw.contains("base"), deep = (hl != null && hl.getY() < 40D);
            final String b = (nether || base || hl == null ? "" : hl.getBlock().getBiome().name());
            short baseSaplingId = 0;
            if (base) {
                final Location hoLoc = ho.getLocation();
                for (final Map.Entry<PlayerBase, Short> entry : saplingIds.entrySet()) {
                    if (entry.getKey().contains(hoLoc)) {
                        baseSaplingId = entry.getValue();
                        break;
                    }
                }
            }
            final ItemStack item = base ? new ItemStack(Material.SAPLING, 1, baseSaplingId) :
                    nether ? new ItemStack(Material.NETHER_STALK) :
                            deep ? new ItemStack(Material.STONE) :
                                    b.contains("DESERT") || b.contains("MESA") || b.equals("BEACH") ? new ItemStack(Material.DEAD_BUSH) :
                                            b.contains("SWAMP") ? new ItemStack(Material.WATER_LILY) :
                                                    b.contains("JUNGLE") ? new ItemStack(Material.VINE) :
                                                            b.contains("EXTREME") ? new ItemStack(Material.CLAY_BALL) :
                                                                    b.contains("TAIGA") ? new ItemStack(Material.DOUBLE_PLANT, 1, (byte) 2) :
                                                                            b.contains("MUSHROOM") ? new ItemStack(Material.RED_MUSHROOM) :
                                                                                    b.contains("FROZEN") || b.contains("ICE") ? new ItemStack(Material.SNOW_BALL) :
                                                                                            b.contains("OCEAN") || b.contains("RIVER") ? new ItemStack(Material.BOAT) :
                                                                                                    new ItemStack(Material.LONG_GRASS, 1, (byte) 2);
            pages.addItem(ms.renameItemStack(item, "§f§l" + home, "", "§fLinksklick§7, um zu teleportieren.", home.equals("home") && (viewotherhomes == false || args.length == 0) ? "§eRechtsklick§7, um zu überschreiben§7." : "§cRechtsklick§7, um zu entfernen."));
        }
        final Inventory inv;
        if (viewotherhomes && args.length > 0 && args[0].equals("page") == false) {
            inv = ms.getServer().createInventory(null, 45, "§0§lOrte von " + u.getName());
            if (homes.isEmpty() == false)
                inv.setItem(5, ms.renameItemStack(new ItemStack(Material.TNT), "§c§lAlle Homes löschen", "§7Lösche alle Homes von diesem Spieler.", "", "§cKlicke§7, um alle Homes zu löschen."));
        } else {
            inv = ms.getServer().createInventory(null, 45, places ? "§0§lMuxMenü§0 | Orte" : "§0§lMuxHomes§0 | /homes");
            final PlayerBase base = ms.getBase().getCurrentLocations().get(p);
            final boolean cansethome = p.getWorld() != ms.getBase().getWorld() || base == null || (base.isTrusted(p) && base.getTempTrusted().containsKey(p.getUniqueId()) == false);
            final ItemStack bed = ms.renameItemStack(new ItemStack(Material.BED), "§6§lHome setzen", "§7Setze hiermit sofort an dem Punkt,", "§7wo du gerade stehst, ein Zuhause.", "", cansethome ? "§6Klicke§7, um einen Home zu setzen." : "§cDu kannst hier kein Home setzen.");
            pages.addItem(cansethome ? ms.addGlow(bed) : bed);
        }
        page = ms.addPageButtons(inv, page, pages);
        if (places || viewotherhomes) {
            inv.setItem(4, ms.renameItemStack(new ItemStack(Material.COMPASS), "§f§lOrte §0Page " + page, "§7Alle deine Homes und Teambasen", "§7gehören zu dieser Kategorie."));
        } else {
            inv.setItem(4, ms.renameItemStack(new ItemStack(Material.DARK_OAK_DOOR_ITEM), "§6§lMuxHomes §0Page " + page, "§7Mit dieser Funktion gelangst du", "§7jederzeit zurück an einen Ort."));
        }
        final InvType it = ms.getActiveInv(p.getName());
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        if (it != InvType.HOMES && it != InvType.CONFIRM && it != InvType.MENU && it != InvType.USERCONTROL)
            p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.HOMES);
        return true;
    }

    public void handleInventory(final Player p, final ItemStack i, final Inventory cinv, final int rawslot, final boolean rightclick) {
        final String pname = p.getName(), title = cinv.getTitle();
        final Material m = i.getType();
        if (rawslot < 9) {
            if (m == Material.TNT && title.contains("|") == false) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                final String plname = title.split(" ")[2];
                final PlayerAction confirm = player -> {
                    player.closeInventory();
                    clearHomes(p, plname);
                }, cancel = player -> player.performCommand("homes " + plname);
                new ConfirmInventory(ms, confirm, cancel).show(p, "§0§lAlle Zuhause entfernen", "§aBestätigen", "§cAbbrechen");
                return;
            } else if (m == Material.ITEM_FRAME) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                if (title.contains("|") == false) {
                    final String plname = title.split(" ")[2];
                    final OfflinePlayer op = ms.getPlayer(plname);
                    if (op == null) return;
                    ms.getOverview().openDataManagement(p, op);
                    return;
                }
                p.performCommand("menu");
                return;
            } else if (m == Material.SKULL_ITEM) {
                p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                final int page = getPageNumber(cinv.getItem(4));
                String plname = null;
                if (title.contains("|") == false) {
                    plname = title.substring(13);
                }
                if (rawslot == 7)
                    p.performCommand("home " + (plname != null ? plname : "") + " page " + (page - 1));
                else if (rawslot == 8)
                    p.performCommand("home " + (plname != null ? plname : "") + " page " + (page + 1));
                return;
            }
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        } else if (m == Material.BED) {
            if (ms.hasGlow(i) == false) {
                p.playSound(p.getLocation(), Sound.ANVIL_BREAK, 1f, 1F);
                return;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            new MuxAnvil(ms, (input, player) -> player.performCommand("sethome " + ms.retardCleaner(input, "Name des Homes: "))).show(p, "Name des Homes: ", new ItemStack(Material.BED));
            return;
        } else if (m == Material.SAPLING && i.getDurability() > 0 && i.getItemMeta().getDisplayName().contains(" ")) {
            final String owner = i.getItemMeta().getDisplayName().split(" ")[2];
            if (rightclick) {
                if (i.getItemMeta().hasLore() && i.getItemMeta().getLore().size() != 3) {
                    p.performCommand("base tp " + owner);
                    p.closeInventory();
                    return;
                }
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                final PlayerAction confirm = player -> {
                    final OfflinePlayer op = ms.getPlayer(owner);
                    if (ms.getBase().hasBase(op.getUniqueId())) {
                        final PlayerBase b = ms.getBase().getFromUUID(op.getUniqueId());
                        if (b.getTempTrusted().containsKey(p.getUniqueId())) {
                            b.removeTempTrusted(p.getUniqueId());
                            final Player baseOwner = ms.getServer().getPlayer(owner);
                            if (baseOwner != null) {
                                baseOwner.sendMessage("§a§lMuxBase>§7 Der Spieler §a" + p.getName() + " §7hat seinen Zugang §centfernt§7.");
                            }
                            ms.showItemBar(p, "§fDer Zugang zur §aBase von " + op.getName() + " §fwurde §centfernt§f.");
                        } else if (b.getTrusted().contains(p.getUniqueId())) {
                            b.removeTrusted(p.getUniqueId());
                            final Player baseOwner = ms.getServer().getPlayer(owner);
                            if (baseOwner != null) {
                                baseOwner.sendMessage("§a§lMuxBase>§7 Der Spieler §a" + p.getName() + " §7hat seinen Zugang §centfernt§7.");
                            }
                            ms.showItemBar(p, "§fDer Zugang zur §aBase von " + op.getName() + " §fwurde §centfernt§f.");
                        }
                    }
                    final MuxUser u = ms.getMuxUser(player.getName());
                    if (u.getHomes().size() > 1) {
                        p.performCommand("homes");
                    } else {
                        p.closeInventory();
                    }
                }, cancel = player -> player.performCommand("home");
                new ConfirmInventory(ms, confirm, cancel).show(p, "§0§lZugang entfernen", "§aBestätigen", "§cAbbrechen", new ItemStack(i.getType(), 1, i.getDurability()), i.getItemMeta().getDisplayName());
            } else {
                p.performCommand("base tp " + owner);
                p.closeInventory();
            }
            return;
        } else if (m == Material.SAPLING && i.getItemMeta().getDisplayName().startsWith("§a§lBase")) {
            final String plname = title.split(" ")[2];
            p.performCommand("base tp " + plname);
            return;
        } else if (m == Material.CLAY_BALL || m == Material.VINE || m == Material.BOAT || m == Material.SAPLING || m == Material.NETHER_STALK || m == Material.STONE || m == Material.DEAD_BUSH || m == Material.WATER_LILY || m == Material.DOUBLE_PLANT || m == Material.RED_MUSHROOM || m == Material.SNOW_BALL || m == Material.LONG_GRASS) {
            final String home = ChatColor.stripColor(i.getItemMeta().getDisplayName());
            final boolean otherplayer = title.contains("|") == false;
            if (rightclick) {
                if (otherplayer) {
                    final String plname = title.substring(13);
                    final PlayerAction confirm = player -> {
                        p.closeInventory();
                        final boolean success = delHome(p, plname, home);
                        if (success == false) p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                    }, cancel = player -> player.performCommand("homes " + plname);
                    new ConfirmInventory(ms, confirm, cancel).show(p, "§0§lZuhause entfernen", "§aBestätigen", "§cAbbrechen", new ItemStack(m), "§f§l" + home + " von " + plname);
                    p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                    return;
                }
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                final MuxUser u = ms.getMuxUser(pname);
                if (u.getHome(home) == null) {
                    p.closeInventory();
                    ms.showItemBar(p, "§cDieses Zuhause existiert nicht mehr.");
                    return;
                }
                final PlayerAction confirm = player -> {
                    player.closeInventory();
                    if (home.equals("home")) { // Overriding home "home"
                        player.performCommand("sethome");
                        return;
                    }
                    u.removeHome(home);
                    ms.showItemBar(player, "§fDein Zuhause '§6" + home + "§f' wurde §cgelöscht§f.");
                }, cancel = player -> player.performCommand("home");
                new ConfirmInventory(ms, confirm, cancel).show(p, home.equals("home") ? "§0§lZuhause überschreiben" : "§0§lZuhause entfernen", "§aBestätigen", "§cAbbrechen", new ItemStack(m), "§f§l" + home);
                return;
            }
            p.closeInventory();
            if (otherplayer) {
                final String plname = title.split(" ")[2];
                p.performCommand("home " + plname + " " + home);
                return;
            }
            p.performCommand("home " + home);
            return;
        }
        p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
    }

    public boolean delHome(final Player p, final String name, final String h) {
        if (ms.isTrusted(p.getName()) == false) return false;
        final OfflinePlayer victim = ms.getPlayer(name);
        final String home = h.toLowerCase();
        if (victim == null) {
            ms.showItemBar(p, ms.hnotfound);
            return false;
        }
        final String pname = victim.getName();
        final MuxUser u;
        if (victim.isOnline()) {
            u = ms.getMuxUser(pname);
        } else {
            u = ms.getDB().loadPlayer(victim.getUniqueId());
            if (u != null) u.setHomes(ms.getDB().loadHomes(victim.getUniqueId()));
        }
        if (u == null) {
            ms.showItemBar(p, ms.hnotfound);
            return false;
        } else if (u.getHome(home) == null) {
            ms.showItemBar(p, "§cDieses Zuhause existiert nicht.");
            return false;
        }
        u.removeHome(home);
        ms.saveMuxUser(u);
        ms.getHistory().addHistory(victim.getUniqueId(), p.getUniqueId(), "TEAMACTION", "DELHOME", home);
        ms.showItemBar(p, "§fZuhause §6'" + home + "' §fvon " + pname + " wurde §cgelöscht§f.");
        return true;
    }

    public void removeIllegalHomes(final String pname, final UUID uuid) {
        final OfflinePlayer victim = ms.getPlayer(pname);
        if (victim == null || victim.isOp() || ms.isTrusted(pname) || ms.isFullTrusted(pname)) {
            return;
        }
        final MuxUser u;
        final boolean online = victim.isOnline();
        if (online) {
            u = ms.getMuxUser(pname);
        } else {
            u = ms.getDB().loadPlayer(victim.getUniqueId());
            if (u != null) u.setHomes(ms.getDB().loadHomes(victim.getUniqueId()));
        }
        if (u == null) return;
        final ProtectedRegion fspawnrg = ms.getFullSpawnRegion();
        final Set<String> toRemove = new HashSet<>();
        final List<PlayerBase> bases = ImmutableList.copyOf(ms.getBase().getPlayerBases().values());
        final String spawnworld = ms.getGameSpawn().getWorld().getName(), netherworld = ms.getNetherSpawn().getWorld().getName(), endworld = ms.getEndWorld().getName();
        ms.getForkJoinPool().execute(() -> {
            for (Map.Entry<String, Home> entry : ImmutableMap.copyOf(u.getHomes()).entrySet()) {
                final Home h = entry.getValue();
                final String wname = entry.getValue().getWorld();
                final PlayerBase untrustedBase = bases.stream()
                        .filter(playerBase -> playerBase.isTrusted(uuid) == false)
                        .filter(playerBase -> playerBase.contains(h)).findFirst().orElse(null);
                final boolean inspawn = wname.equals(spawnworld) && fspawnrg != null && fspawnrg.contains((int) h.getX(), (int) h.getY(), (int) h.getZ()),
                        overnether = wname.equals(netherworld) && h.getY() >= 127.0D, baseuntrusted = untrustedBase != null;
                if ((overnether || wname.equals("warps") || wname.equals("MuxCasino") || wname.equals(endworld) || baseuntrusted || inspawn || h.getY() < 1D) && victim.isOp() == false) {
                    toRemove.add(entry.getKey());
                }
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (online == false && victim.isOnline()) { // If player comes online during async check (should rarely happen)
                        final MuxUser u2 = ms.getMuxUser(pname);
                        toRemove.forEach(u2::removeHome);
                        saveMuxUser(u2);
                    } else {
                        toRemove.forEach(u::removeHome);
                        saveMuxUser(u);
                    }
                }
            }.runTask(ms);
        });
    }

    private void saveMuxUser(final MuxUser u) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ms.saveMuxUser(u);
            }
        }.runTaskAsynchronously(ms);
    }

    public void clearHomes(final Player p, final String name) {
        if (ms.isTrusted(p.getName()) == false) return;
        final OfflinePlayer victim = ms.getPlayer(name);
        if (victim == null) {
            ms.showItemBar(p, ms.hnotfound);
            return;
        }
        final String pname = victim.getName();
        final MuxUser u;
        if (victim.isOnline()) {
            u = ms.getMuxUser(pname);
        } else {
            u = ms.getDB().loadPlayer(victim.getUniqueId());
            if (u != null) u.setHomes(ms.getDB().loadHomes(victim.getUniqueId()));
        }
        if (u == null) {
            ms.showItemBar(p, ms.hnotfound);
            return;
        }
        u.clearHomes();
        ms.saveMuxUser(u);
        ms.getHistory().addHistory(victim.getUniqueId(), p.getUniqueId(), "TEAMACTION", "CLEARHOMES", null);
        ms.showItemBar(p, "§fHomes von §6" + pname + " §fwurden §cgelöscht§f.");
    }

    private int getPageNumber(final ItemStack is) {
        return Integer.parseInt(is.getItemMeta().getDisplayName().split(" ")[2]);
    }

    private void tooManyHomes(final Player p, final short homes) {
        if (homes == 1) {
            sendNoRankMessage(p, "VIP");
            return;
        }
        ms.showItemBar(p, "§cDu hast bereits zu viele Homes gesetzt.");
        if (homes == 5) sendNoRankMessage(p, "ELITE");
        else if (homes == 10) sendNoRankMessage(p, "EPIC");
        else if (homes == 100) ms.showItemBar(p, "§cDu hast das maximale Limit an Homes erreicht.");
    }

    private void sendNoRankMessage(final Player p, final String r) {
        final String rank = ChatColor.translateAlternateColorCodes('&', ms.getPerms().getGroup(r).getPrefix().replace(" ", ""));
        ms.chatClickHoverLink(p, "§6§lMuxCraft>§7 Für mehr Homes benötigst du den Rang " + rank + "§7.", ms.getLang("ranks.hoverforrank"), "https://shop.muxcraft.eu/?ign=" + p.getName());
        ms.chatClickHoverLink(p, ms.getLang("ranks.clickforrank"), ms.getLang("ranks.hoverforrank"), "https://shop.muxcraft.eu/?ign=" + p.getName());
    }

    public final class Home extends MuxLocation {
        public Home(final Location l) {
            super(l);
        }
    }
}