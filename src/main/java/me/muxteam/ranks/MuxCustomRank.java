package me.muxteam.ranks;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.muxteam.basic.ConfirmInventory;
import me.muxteam.basic.MuxAnvil;
import me.muxteam.basic.Pageifier;
import me.muxteam.extras.MuxBoosters;
import me.muxteam.muxsystem.MuxHistory;
import me.muxteam.muxsystem.MuxInventory;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.shared.MuxSharedPackets;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class MuxCustomRank {
    private MuxSystem ms;
    private final Set<UUID> fly = new CopyOnWriteArraySet<>();
    private final List<String> tagBlacklist = Arrays.asList("owner", "admin", "dev", "sup", "build", "creator");
    private final List<Character> symbolList = Arrays.asList('❖', '⚡', '⚒', '⚓', '⚔', '⚕', '⚖', '⚘', '⚙', '⚚', '⚛', '⚜', '⚝', '⚠', '-', '*', '☬', '⊕', '»', '=', '✦',
            '✪', '✫', '✬', '✭', '✮', '✯', '✰', '✱', '✹', '✺', '❣', '❤', '❦', '❧', '☢', '☣', '☯', '♚',  '✲', '✳', '✴', '✵', '✶', '✷', '✸', '⎈', '☃', '☘');
    private final Map<UUID, TagData> tagData = new ConcurrentHashMap<>();

    public MuxCustomRank(final MuxSystem ms) {
        this.ms = ms;
    }

    public void close() {
        this.ms = null;
    }

    public void removeFly(final UUID uuid) {
        fly.remove(uuid);
    }

    public boolean isFly(final UUID uuid) {
        return fly.contains(uuid);
    }

    public boolean toggleFly(final UUID uuid) {
        if (fly.remove(uuid) == false) {
            fly.add(uuid);
            return true;
        }
        return false;
    }

    public void handleJoin(final UUID uuid) {
        CompletableFuture.supplyAsync(() -> ms.getDB().loadCustomRank(uuid))
                .thenAccept(data -> {
                    if (data != null) this.tagData.put(uuid, data);
                });
    }

    public void handleQuit(final UUID uuid) {
        this.tagData.remove(uuid);
    }

    public void resetTag(final UUID uuid) {
        ms.getDB().deleteCustomRank(uuid);
        reloadTag(uuid);
        ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketReloadTagData(uuid));
    }

    public void reloadTag(final UUID uuid) {
        final Player p = ms.getServer().getPlayer(uuid);
        if (p == null) return;
        if (ms.getActiveInv(p.getName()) == MuxInventory.InvType.X)
            p.closeInventory();
        handleQuit(uuid);
        handleJoin(uuid);
    }

    public void checkFlying() {
        final Server sr = ms.getServer();
        final World baseworld = ms.getBase().getWorld();
        for (final UUID uuid : fly) {
            final Player p = sr.getPlayer(uuid);
            if (p == null) {
                fly.remove(uuid);
                continue;
            }
            if (ms.hasNormalFlight(p)) continue;
            final Location l = p.getLocation();
            final World w = p.getWorld();
            final ProtectedRegion spawnrg = ms.getSpawnRegion(), shoprg = ms.getShopRegion();
            final boolean canfly = (w.equals(baseworld) && ms.getBase().isTrustedAtLocation(p)) || (shoprg != null && w.equals(ms.getWarpsWorld()) && shoprg.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) ||
                    (spawnrg != null && w.equals(ms.getGameSpawn().getWorld()) && spawnrg.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ()));
            if (p.getAllowFlight() && canfly == false) {
                p.setAllowFlight(false);
                p.setFlying(false);
            } else if (p.getAllowFlight() == false && canfly) {
                p.setAllowFlight(true);
                p.setFlying(true);
                p.setFlySpeed(0.05F);
            }
            if (p.isFlying()) {
                p.getWorld().spigot().playEffect(l.subtract(0D, 0.5D, 0D), Effect.CLOUD, 0, 0, (float) 0.1, (float) 0.1, (float) 0.1, (float) 0.01, 3, 50);
            }
        }
    }

    public boolean handleCommand(final Player p, final String[] args) {
        if (ms.hasX(p.getUniqueId()) == false) {
            ms.sendNoRankMessage(p, "X");
            return true;
        } else if (args != null && args.length > 0) {
            final String s = ms.fromArgs(args, 0);
            if (args.length == 1 && p.hasMetadata("xtagdata")) {
                final TagData data = (TagData) p.getMetadata("xtagdata").get(0).value();
                if (data != null) {
                    try {
                        data.chatColor = ChatColor.valueOf(s.toUpperCase());
                    } catch (final Exception ignored) {}
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
                    openTagInventory(p);
                    return true;
                }
            }
        }
        final UUID uuid = p.getUniqueId();
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lX§0 | /x");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.SEA_LANTERN), "§f§lX",
                "§7Hier sind alle Features gelistet, die für", "§7diesen exklusiven Rang bestimmt sind."));
        inv.setItem(19, ms.renameItemStack(new ItemStack(Material.EXP_BOTTLE), "§f§lAutomatischer Vote",
                "§7Solange du diesen Rang besitzt,", "§7musst du hier nicht mehr voten.",
                "", "§aDas Feature ist freigeschaltet."));
        inv.setItem(20, ms.renameItemStack(new ItemStack(Material.GOLD_BLOCK), "§e§lGold", "§7Hiermit gelangst du direkt zu", "§7dem exklusiven Gold-Menü.", "", "§eKlicke§7, um dieses zu öffnen."));

        final Location l = p.getLocation();
        final ProtectedRegion spawnrg = ms.getSpawnRegion(), shoprg = ms.getShopRegion();
        final World w = p.getWorld();
        final World baseworld = ms.getBase().getWorld();
        final boolean canfly = (w.equals(baseworld) && ms.getBase().isTrustedAtLocation(p)) || (shoprg != null && w.equals(ms.getWarpsWorld()) && shoprg.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) ||
                (spawnrg != null && w.equals(ms.getGameSpawn().getWorld()) && spawnrg.contains(l.getBlockX(), l.getBlockY(), l.getBlockZ()));
        final boolean hasfly = isFly(uuid);
        final ItemStack flyitem = ms.addLore(ms.renameItemStack(new ItemStack(Material.FEATHER), "§f§lFlugmodus" + (hasfly ? "§a §laktiviert" : ""),
                        "§7Hiermit kannst du am Spawn", "§7durch die Gegend fliegen.", ""),
                ms.getBooster().hasBooster(p, MuxBoosters.BoosterType.FLY) ? new String[]{"§cDu nutzt gerade einen Fly Booster."} :
                        p.getAllowFlight() && hasfly == false ? new String[]{"§cDu hast bereits ein Flugmodus aktiv."} :
                                canfly == false ? new String[]{"§cDu kannst es nicht hier aktivieren."} :
                                        new String[]{"§7Klicke, um den Flugmodus", "§7zu " + (hasfly ? "§cdeaktivieren" : "§aaktivieren") + "§7."});
        inv.setItem(24, flyitem);

        final List<String> tagLore = new ArrayList<>(Arrays.asList("§7Gestalte deinen Rang im globalen", "§7Chat in deiner gewünschten Form.", ""));
        if (this.tagData.containsKey(p.getUniqueId())) {
            final TagData data = getTagData(uuid);
            tagLore.add("§7Rang: " + data.toString());
            tagLore.add("");
            if (data.lastChange > System.currentTimeMillis() - 604800000L) {
                tagLore.add("§cDu kannst deinen Tag erst");
                tagLore.add("§cin " + ms.getTime(((int) (((data.lastChange + 604800000L) - System.currentTimeMillis()) / 1000L))) + " §cwieder ändern.");
                tagLore.add("");
            } else tagLore.add("§fKlicke§7, um deinen Tag zu setzen.");
            tagLore.add("§7Rechtsklick, um zu " + (data.isActive() ? "§cdeaktivieren" : "§aaktivieren") + "§7.");
        } else {
            tagLore.add("§fKlicke§7, um deinen Tag zu setzen.");
        }
        inv.setItem(25, ms.renameItemStack(new ItemStack(Material.NAME_TAG), "§f§lTag setzen", tagLore));

        inv.setItem(31, ms.renameItemStack(new ItemStack(Material.BOOK), "§f§lDein Verlauf",
                "§7Hier hast du einen Überblick auf", "§7deine Historie auf dem Server.", "", "§fKlicke§7, um dein Verlauf zu sehen."));
        inv.setItem(37, ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 14), "§f§lMehr Chatfarben",
                "§7Als X verfügst du über mehr", "§7Chatfarben als die anderen.", "", "§7Klicke, um diese zu §fsehen§7."));
        inv.setItem(38, ms.renameItemStack(new ItemStack(Material.SKULL_ITEM, 1, (byte) 3), "§f§lUnlimitierte Köpfe",
                "§7Wähle beliebig viele Dekorationsköpfe", "§7aus dieser umfangreichen Liste aus.", "", "§fKlicke§7, um einen Kopf auszusuchen."));

        inv.setItem(42, ms.renameItemStack(new ItemStack(Material.DIAMOND_CHESTPLATE), "§f§lKampfitems umbenennen",
                "§7Mit dem Befehl /rename gibst du", "§7den Items einen Namen der Wahl.", "", "§aDas Feature ist freigeschaltet."));

        final MuxRanks.PermissionsUser pu = ms.getPerms().getUserData(uuid);
        if (pu.getExpireData() != null && pu.getExpireData().getExpireTime() > System.currentTimeMillis()) {
            final String time = (ms.getTime((int) ((pu.getExpireData().getExpireTime() - System.currentTimeMillis()) / 1000)));
            final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
            format.setTimeZone(TimeZone.getTimeZone("CET"));
            inv.setItem(43, ms.renameItemStack(new ItemStack(Material.WATCH), "§f§l" + time, "§7Dein X Rang ist noch bis zu" , "§7dem " + format.format(new Date(pu.getExpireData().getExpireTime())) + " §7gültig. Hier", "§7kannst du ihn verlängern.", "", "§fKlicke§7, für 30 weitere Tage."));
        }

        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.X && it != MuxInventory.InvType.GOLD && it != MuxInventory.InvType.MENU && it != MuxInventory.InvType.HISTORY && it != MuxInventory.InvType.CONFIRM)
            p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.X);
        return true;
    }

    public void handleInventory(final Player p, final ItemStack i, final int slot, final String title, boolean rightclick) {
        final Material m = i.getType();
        if (i.getItemMeta().hasLore() && i.getItemMeta().getLore().size() > 3 && i.getItemMeta().getLore().get(3).startsWith("§c")) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            p.updateInventory();
            return;
        }
        if (m == Material.WATCH) {
            final MuxUser u = ms.getMuxUser(p.getName());
            new ConfirmInventory(ms, p1 -> {
                final MuxRanks.PermissionsUser pu = ms.getPerms().getUserData(p.getUniqueId());
                if (pu.getExpireData() == null || pu.getGroup().equalsIgnoreCase("X") == false) {
                    handleCommand(p, null);
                    return;
                }
                if (u.getGems() < 40000) {
                    p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                    ms.showItemBar(p, "§cDu benötigst 40.000 MuxGems zum verlängern.");
                    return;
                }
                u.setGems(u.getGems() - 40000);
                pu.getExpireData().setExpireTime(pu.getExpireData().getExpireTime() + 2592000000L);
                ms.saveMuxUser(u);
                ms.sendScoreboard(p1);
                ms.getDB().savePermissionsUser(pu);
                final Firework firework = p1.getWorld().spawn(p1.getLocation(), Firework.class);
                final FireworkMeta meta = firework.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder().withColor(Color.WHITE).with(FireworkEffect.Type.BALL_LARGE).trail(true).build());
                meta.setPower(0);
                firework.setFireworkMeta(meta);
                p1.playSound(p1.getLocation(), Sound.LEVEL_UP, 1F, 1F);
                p1.closeInventory();
                ms.showItemBar(p1, "§fDu hast den §lX Rang§f erfolgreich §averlängert§f.");
            }, p1 ->  handleCommand(p1, null)).show(p, "§0§lX§0 | Verlängern (30 Tage)", "§aVerlängern (40.000 Gems)", "§cAbbrechen");
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        } else if (m == Material.SKULL_ITEM && slot <= 8) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            int page = p.hasMetadata("xsymbolpage") ? p.getMetadata("xsymbolpage").get(0).asInt() : 0;
            if (slot == 7) page--;
            else page++;
            p.removeMetadata("xsymbolpage", ms);
            p.setMetadata("xsymbolpage", new FixedMetadataValue(ms, page));
            openRankSymbolInventory(p);
        } else if (m == Material.GOLD_BLOCK) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            p.performCommand("gold");
        } else if (m == Material.BOOK) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            ms.getHistory().openHistory(p, MuxHistory.HistoryType.GENERAL, p.getUniqueId(), p.getName(), 0);
        } else if (m == Material.SKULL_ITEM) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            p.performCommand("headdb");
        } else if (m == Material.FEATHER) {
            if (toggleFly(p.getUniqueId())) {
                p.setAllowFlight(true);
                p.setFlying(true);
                p.setFlySpeed(0.05F);
            } else {
                p.setAllowFlight(false);
                p.setFlying(false);
                p.setFlySpeed(0.1F);
            }
            handleCommand(p, null);
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
        } else if (m == Material.ITEM_FRAME) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            if (title.endsWith("/x")) {
                ms.getMenu().getBenefits().openPremiumInv(p);
            } else if (title.endsWith("Tag setzen"))
                handleCommand(p, null);
            else
                openTagInventory(p);
        } else if (m == Material.NAME_TAG && slot != 4) {
            if (rightclick && (this.tagData.containsKey(p.getUniqueId()))) {
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
                final TagData data = this.tagData.get(p.getUniqueId());
                data.active = !data.active;
                ms.getForkJoinPool().execute(() -> ms.getDB().saveCustomRank(p.getUniqueId(), data));
                handleCommand(p, null);
                return;
            }
            if (this.tagData.containsKey(p.getUniqueId())) {
                final TagData data = this.tagData.get(p.getUniqueId());
                if (data.lastChange > System.currentTimeMillis() - 604800000L) {
                    p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                    ms.showItemBar(p, "§cDu kannst deinen Tag nur einmal pro Woche ändern.");
                    return;
                }
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            openTagInventory(p);
        } else if (m == Material.PAPER) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            new MuxAnvil(ms, (input, p1) -> {
                input = ms.retardCleaner(input, "Tag:");
                if (input.isEmpty() || input.length() > 9) {
                    ms.showItemBar(p, "§cDein Tag muss 1-9 Zeichen lang sein.");
                    openTagInventory(p);
                }
                if (containsBlacklistedTag(input) || ms.getChat().isNotSafe(p, input.toLowerCase(), false).isEmpty() == false) {
                    p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                    ms.showItemBar(p, "§cDein Tag enthält Wörter, die nicht erlaubt sind.");
                    openTagInventory(p);
                    return;
                }
                ((TagData) p.getMetadata("xtagdata").get(0).value()).tag = input.toUpperCase();
                p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
                openTagInventory(p);
            }).show(p, "Tag: ");
        } else if (m == Material.RECORD_8) {
            openRankSymbolInventory(p);
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        } else if (m == Material.INK_SACK && slot > 25) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            p.performCommand("colors");
        } else if (m == Material.INK_SACK) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            p.closeInventory();
            openColorBook(p);
        } else if (m == Material.NETHER_STAR) {
            final TagData data = (TagData) p.getMetadata("xtagdata").get(0).value();
            data.symbol = i.getItemMeta().getDisplayName().charAt(2);
            p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
            openTagInventory(p);
        } else if (m == Material.EYE_OF_ENDER) {
            if (ms.checkGeneralCooldown(p.getName(), "xtagpreview", 1000L, true)) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            final TagData data = (TagData) p.getMetadata("xtagdata").get(0).value();
            if (data.isUseable() == false) {
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                ms.showItemBar(p, "§cStelle zuerst alles ein.");
                return;
            }
            final String clantext = ms.getDB().getChatText(p.getUniqueId());
            p.sendMessage(data + (clantext.isEmpty() == false ? clantext + "§8*§7" : "§7") + p.getName() + "§8:§r Hallo, " + p.getName() + ".");
            p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 1F);
        } else if (m == Material.STAINED_CLAY && i.getDurability() == 13) {
            final TagData data = ((TagData) p.getMetadata("xtagdata").get(0).value()).clone();
            data.lastChange = System.currentTimeMillis();
            data.active = true;
            this.tagData.put(p.getUniqueId(), data.clone());
            p.removeMetadata("xtagdata", ms);
            p.removeMetadata("xsymbolpage", ms);
            ms.getForkJoinPool().execute(() -> ms.getDB().saveCustomRank(p.getUniqueId(), data));
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
            handleCommand(p, null);
        } else {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
        }
    }

    private void openColorBook(final Player p) {
        final List<Object[]> clickables = new ArrayList<>();
        final String clickToChoose = "§7Klicke, um diese Farbe auszuwählen.", cmd = "/x ";
        clickables.add(new Object[]{cmd + ChatColor.GREEN.name(), "§aGrün\n", clickToChoose});
        clickables.add(new Object[]{cmd + ChatColor.AQUA.name(), "§bBlau\n", clickToChoose});
        if (p.hasPermission("muxsystem.creator"))
            clickables.add(new Object[]{cmd + ChatColor.LIGHT_PURPLE.name(), "§dPink\n", clickToChoose});
        clickables.add(new Object[]{cmd + ChatColor.YELLOW.name(), "§eGelb\n", clickToChoose});
        clickables.add(new Object[]{cmd + ChatColor.WHITE.name(), "§0Weiss\n", clickToChoose});
        clickables.add(new Object[]{cmd + ChatColor.DARK_GREEN.name(), "§2Dunkelgrün\n", clickToChoose});
        clickables.add(new Object[]{cmd + ChatColor.DARK_AQUA.name(), "§3Hellblau\n", clickToChoose});
        clickables.add(new Object[]{cmd + ChatColor.GOLD.name(), "§6Gold\n", clickToChoose});
        clickables.add(new Object[]{cmd + ChatColor.GRAY.name(), "§7Grau\n", clickToChoose});
        clickables.add(new Object[]{cmd + ChatColor.DARK_GRAY.name(), "§8Dunkelgrau\n", clickToChoose});
        clickables.add(new Object[]{cmd + ChatColor.BLUE.name(), "§9Dunkelblau\n", clickToChoose});
        clickables.add(new Object[]{cmd + ChatColor.DARK_BLUE.name(), "§1Dunkelblau\n", clickToChoose});
        clickables.add(new Object[]{cmd + ChatColor.DARK_PURPLE.name(), "§5Lila", clickToChoose});
        ms.openBook(ms.createBook("§0§lFarben", "§0Wähle eine Farbe aus",
                clickables, ClickEvent.Action.RUN_COMMAND), p);
    }

    private boolean containsBlacklistedTag(final String input) {
        return tagBlacklist.stream().anyMatch(s -> input.contains(s.toLowerCase()));
    }

    public boolean hasTagData(final UUID uuid) {
        return this.tagData.containsKey(uuid) && this.tagData.get(uuid).isActive();
    }

    public TagData getTagData(final UUID uuid) {
        return this.tagData.get(uuid);
    }

    private void openTagInventory(final Player p) {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lX§0 | Tag setzen");
        if (p.hasMetadata("xtagdata") == false) {
            p.setMetadata("xtagdata", new FixedMetadataValue(ms, new TagData()));
        }
        final TagData data = (TagData) p.getMetadata("xtagdata").get(0).value();
        final boolean canUseTagData = data.isUseable();
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.NAME_TAG), "§f§lTag setzen", "§7Gestalte deinen Rang im globalen", "§7Chat in deiner gewünschten Form."));
        ItemStack tagStack = ms.renameItemStack(new ItemStack(Material.PAPER), "§f§lTag" + (data.tag != null ? "§7 » §r" + data.getTag() : ""), "", "§7Klicke, um den Tag zu §fsetzen§7.");
        if (data.tag != null) tagStack = ms.addGlow(tagStack);
        inv.setItem(19, tagStack);
        ItemStack disc = ms.renameItemStack(new ItemStack(Material.RECORD_8), "§f§lZeichen" + (data.symbol != null ? "§7 » §r" + data.getSymbol() : ""), "", "§7Klicke, um das Zeichen zu §fsetzen§7.");
        if (data.symbol != null) disc = ms.addGlow(disc);
        final ItemMeta meta = disc.getItemMeta();
        meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        disc.setItemMeta(meta);
        inv.setItem(20, disc);
        ItemStack color = ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, ms.getDyeColor(data.getChatColor() == null ? ChatColor.GRAY : data.getChatColor()).getDyeData()), (data.getChatColor() == null ? "§7" : data.getChatColor()) + "§lFarbe", "", "§7Klicke, um die Farbe zu §fsetzen§7.");
        if (data.chatColor != null) color = ms.addGlow(color);
        inv.setItem(21, color);
        ItemStack previewItem = ms.renameItemStack(new ItemStack(Material.EYE_OF_ENDER), "§f§lVorschau" + (canUseTagData ? "§7 » §r" + data : ""), canUseTagData ? Arrays.asList("", "§7Klicke, um im Chat zu §asehen§7.") : Arrays.asList("", "§cStelle zuerst alles ein."));
        if (canUseTagData) previewItem = ms.addGlow(previewItem);
        inv.setItem(23, previewItem);

        inv.setItem(25, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (short) (canUseTagData ? 13 : 14)), (canUseTagData ? "§a§l" : "§c§l") + "Tag bestätigen", "",
                canUseTagData ? "§7Klicke, um zu §abestätigen§7." : "§cStelle zuerst alles ein."));
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.X) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.X);
    }

    private void openRankSymbolInventory(final Player p) {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lTag setzen§0 | Symbol");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.RECORD_8), "§e§lZeichen"));

        final Pageifier<ItemStack> pages = new Pageifier<>(21);

        for (final Character character : symbolList)
            pages.addItem(ms.renameItemStack(new ItemStack(Material.NETHER_STAR, 1), "§f" + character, "", "§7Klicke, zum §aauswählen§7."));
        final int page = p.hasMetadata("xsymbolpage") ? p.getMetadata("xsymbolpage").get(0).asInt() : 0;
        int slot = 19;
        final int perPage = 7;
        int curr = perPage;
        for (ItemStack itemStack : pages.getPage(page)) {
            curr--;
            inv.setItem(slot, itemStack);
            if (curr == 0) {
                curr = perPage;
                slot += 3;
            } else slot++;
        }
        if (page != 0)
            inv.setItem(7, ms.getHeadFromURL("https://textures.minecraft.net/texture/bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9", ms.getLang("market.previouspage")));
        if (page < pages.getPages().size() - 1)
            inv.setItem(8, ms.getHeadFromURL("https://textures.minecraft.net/texture/19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf", ms.getLang("market.nextpage")));
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.X) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.X);
    }

    public List<Character> getSymbolList() {
        return symbolList;
    }

    public class TagData {
        private String tag;
        private Character symbol;
        private ChatColor chatColor = null;
        private long lastChange = -1;
        private boolean active;

        public TagData(final String tag, final int symbol, final ChatColor chatColor, final long lastChange, final boolean active) {
            this.tag = tag;
            this.symbol = ms.getCustomRank().getSymbolList().get(symbol);
            this.chatColor = chatColor;
            this.lastChange = lastChange;
            this.active = active;
        }

        public TagData(final String tag, final Character symbol, final ChatColor chatColor, final long lastChange, final boolean active) {
            this.tag = tag;
            this.symbol = symbol;
            this.chatColor = chatColor;
            this.lastChange = lastChange;
            this.active = active;
        }

        public TagData() {}

        protected boolean isUseable() {
            return tag != null && symbol != null && chatColor != null;
        }

        public TagData clone() {
            return new TagData(tag, symbol, chatColor, lastChange, active);
        }

        public int getSymbolIndex() {
            return getSymbolList().indexOf(this.symbol);
        }

        @Override
        public String toString() {
            return chatColor + String.valueOf(symbol) + "§l" + tag + chatColor + (symbol.equals('»') ? "«" : symbol) + "§r ";
        }

        public String getTag() {
            return tag;
        }

        public Character getSymbol() {
            return symbol;
        }

        public ChatColor getChatColor() {
            return chatColor;
        }

        public boolean isActive() {
            return active;
        }

        public long getLastChange() {
            return lastChange;
        }
    }
}