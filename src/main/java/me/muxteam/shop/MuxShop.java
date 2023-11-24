package me.muxteam.shop;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import me.muxteam.basic.MuxAnvil;
import me.muxteam.basic.MuxHolograms;
import me.muxteam.basic.Pageifier;
import me.muxteam.muxsystem.MuxInventory;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MuxShop {
    private MuxSystem ms;
    private final int DAILY_PAY_LIMIT = 5000000;
    private final Location shoploc;
    private final Map<String, Double> cheapest = new HashMap<>();
    private final Map<String, Long> cooldown = new HashMap<>();
    // Cache
    private final Map<UUID, List<ShopItem>> cache = new HashMap<>(), soldcache = new HashMap<>();
    private final Map<UUID, Double> earningscache = new HashMap<>();
    private final Map<String, Long> liquidity = new HashMap<>();
    private final Map<String, AbstractMap.SimpleEntry<Double, Long>> lastknownprice = new HashMap<>();
    private final Map<String, Double> lastdayprices = new HashMap<>(), sevendaysagoprices = new HashMap<>(), lasthourprices = new HashMap<>();
    // Save
    private Map<String, Integer> paylimit = new HashMap<>();
    private Map<String, Long> volume = new HashMap<>(), mostsearched = new HashMap<>();
    private final Set<String> largetransactions = new HashSet<>();
    // Load once
    private final Set<MuxHolograms.Hologram> topholos = new HashSet<>();
    private final List<String> itemids = new ArrayList<>();
    private final List<Short> disabledpotions = Arrays.asList(new Short[]{8203, 8235, 8267, 16395, 16427, 16459, 16384, 8192, 64, 32, 16});
    private final Map<Short, Short> replacepotions = new HashMap<>();
    private final Map<String, Location> itemframelocs = new HashMap<>();
    private final Map<ShopCategory, List<String>> categories = new EnumMap<>(ShopCategory.class);
    private Inventory earninv;
    private final File shopfile;
    private final FileConfiguration shopcfg;

    public MuxShop(final MuxSystem ms) {
        this.ms = ms;
        this.shopfile = new File(ms.getDataFolder(), "shop.yml");
        this.shopcfg = YamlConfiguration.loadConfiguration(shopfile);
        final Gson gson = new Gson();
        final FileConfiguration hashYML = ms.getHashYAML();
        if (hashYML.contains("paylimit")) {
            paylimit = gson.fromJson(hashYML.getString("paylimit"), new TypeToken<HashMap<String, Integer>>() {}.getType());
        }
        if (hashYML.contains("shopvolume")) {
            volume = gson.fromJson(hashYML.getString("shopvolume"), new TypeToken<HashMap<String, Long>>() {}.getType());
        }
        if (hashYML.contains("shopmostsearched")) {
            mostsearched = gson.fromJson(hashYML.getString("shopmostsearched"), new TypeToken<HashMap<String, Long>>() {}.getType());
        }
        this.shoploc = new Location(ms.getWarpsWorld(), -94.5, 20, 432.5);
        checkShopConfig();
        setup();
    }

    public void close() {
        final Gson gson = new Gson();
        final FileConfiguration hashYML = ms.getHashYAML();
        hashYML.set("paylimit", gson.toJson(paylimit));
        hashYML.set("shopvolume", gson.toJson(volume));
        hashYML.set("shopmostsearched", gson.toJson(mostsearched));
        saveLargeTransactions();
        this.ms = null;
    }

    public void pushAway() {
        if (ms.getShopRegion() == null) return;
        for (final Player p : ms.getServer().getOnlinePlayers()) {
            final Location l = p.getLocation();
            if (p.getWorld().getName().equals(shoploc.getWorld().getName()) && ms.getShopRegion().contains(l.getBlockX(), l.getBlockY(), l.getBlockZ())) {
                for (final Entity entity : p.getNearbyEntities(0.8D, 0.8D, 0.8D)) {
                    if (entity instanceof ItemFrame == false) continue;
                    final ItemFrame frame = (ItemFrame) entity;
                    if (frame.getItem() != null && frame.getItem().getType() != null) {
                        final String fullId = frame.getItem().getType().getId() + ":" + frame.getItem().getDurability();
                        final Location fL = itemframelocs.get(fullId);
                        if (fL == null) continue;
                        ms.forcePlayer(p, fL);
                        break;
                    }
                }
            }
        }
    }

    public void handleJoin(final Player p) {
        final Location l = p.getLocation();
        if (l.getWorld().getName().equals(shoploc.getWorld().getName()) && ms.getShopRegion().contains(l.getBlockX(), l.getBlockY(), l.getBlockZ()))
            ms.sendScoreboard(p, ms.getShopSB());
    }

    public void updateLiquidity() {
        new BukkitRunnable() {
            @Override
            public void run() {
                final Map<String, Long> newLiquidity = ms.getDB().getShopItemsLiquidity();
                liquidity.clear();
                liquidity.putAll(newLiquidity);
            }
        }.runTaskAsynchronously(ms);
    }

    public void updateLast24HoursValue() {
        final long _24HoursValue = ms.getDB().getTotalVolumeLast24Hours() + volume.values().stream().mapToLong(Long::longValue).sum();
        new BukkitRunnable() {
            @Override
            public void run() {
                final MuxHolograms.Hologram hologram = ms.getHolograms().getPluginHolos().get("shopvolume");
                if (hologram == null) {
                    ms.getHolograms().addHologram("shopvolumetitle", new Location(shoploc.getWorld(), -117.5, 31, 432.5), "§a§lKaufvolumen der letzten 24 Stunden");
                    ms.getHolograms().addHologram("shopvolume", new Location(shoploc.getWorld(), -117.5, 30.5, 432.5), "§e" + ms.getNumberFormat(_24HoursValue) + " MuxCoins");
                } else {
                    hologram.setMessage("§e" + ms.getNumberFormat(_24HoursValue) + " MuxCoins");
                }
            }
        }.runTask(ms);
    }

    public void saveLastHour() {
        final long curr = System.currentTimeMillis(), lastDay = curr - 86400000L, sevenDaysAgo = curr - (86400000L * 7L), lastHour = curr - 3600000L;
        for (final String fullid : itemids) {
            final long v = volume.getOrDefault(fullid, 0L);
            final double price = getCheapestPrice(fullid);
            ms.getDB().saveShopPrices(curr, fullid, price, v);
            lasthourprices.put(fullid, ms.getDB().getPriceAtTime(fullid, lastHour));
            lastdayprices.put(fullid, ms.getDB().getPriceAtTime(fullid, lastDay));
            sevendaysagoprices.put(fullid, ms.getDB().getPriceAtTime(fullid, sevenDaysAgo));
            lastknownprice.put(fullid, ms.getDB().getLastKnownPrice(fullid));
        }
        ms.getDB().saveShopPrices(curr, "GEM", ms.getGemShop().getExactCoinExchangeRate(1), volume.getOrDefault("GEM", 0L));
        volume.clear();
    }

    public void addVolume(final String fullid, final long vol) {
        volume.put(fullid, volume.getOrDefault(fullid, 0L) + vol);
    }

    public void resetLimits() {
        paylimit.clear();
    }

    public int getDailyPay(final Player p) {
        return DAILY_PAY_LIMIT;
    }

    public void setPayLimit(final String pname, final int amount) {
        paylimit.put(pname, amount);
    }

    public Map<String, Integer> getPayLimits() {
        return paylimit;
    }

    public void reloadShopFile() throws Exception {
        shopcfg.load(shopfile);
        cfgcache.clear();
    }

    public void logLargeTransaction(final String s) {
        largetransactions.add(s);
    }

    public void saveLargeTransactions() {
        try (final FileOutputStream fos = new FileOutputStream("largetransactions.log", true)) {
            try (final OutputStreamWriter osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
                try (final BufferedWriter bw = new BufferedWriter(osw); final PrintWriter out = new PrintWriter(bw)) {
                    largetransactions.forEach(out::println);
                    largetransactions.clear();
                }
            }
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    private void checkShopConfig() {
        if (shopfile.exists() == false) {
            try {
                shopcfg.createSection("1:0");
                shopcfg.set("1:0.nameDE", "Stein");
                shopcfg.set("1:0.nameEN", "Stone");
                shopcfg.save(shopfile);
            } catch (final Exception e) {
                System.err.println("MuxSystem> shop.yml could not be created!");
            }
        }
    }

    public void openSearch(final Player p) {
        if (p.getWorld() != this.shoploc.getWorld() || this.shoploc.distance(p.getLocation()) >= 120) {
            ms.showItemBar(p, "§cDu musst dich im Shop befinden.");
            return;
        }
        final Inventory inv = ms.getServer().createInventory(null, 54, "§0§lMuxShop§0 | Suchen");
        inv.setItem(13, ms.renameItemStack(new ItemStack(Material.COMPASS), ShopCategory.MOSTSEARCHED.getDisplayName(), "", "§fKlicke§7, um diese zu sehen."));
        inv.setItem(11, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 5), ShopCategory.TOPGAINERS.getDisplayName(), "", "§aKlicke§7, um diese zu sehen."));
        inv.setItem(15, ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 14), ShopCategory.TOPLOSERS.getDisplayName(), "", "§cKlicke§7, um diese zu sehen."));
        inv.setItem(21, ms.renameItemStack(new ItemStack(Material.BRICK), ShopCategory.BLOCKS.getDisplayName(), "", "§fKlicke§7, um diese zu sehen."));
        inv.setItem(22, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT, 1, (byte) 5), ShopCategory.DECORATION.getDisplayName(), "", "§fKlicke§7, um diese zu sehen."));
        inv.setItem(23, ms.renameItemStack(new ItemStack(Material.POTION), ShopCategory.BREWING.getDisplayName(), "", "§fKlicke§7, um diese zu sehen."));
        inv.setItem(30, ms.renameItemStack(new ItemStack(Material.GOLD_SWORD), ShopCategory.COMBAT_AND_TOOLS.getDisplayName(), "", "§fKlicke§7, um diese zu sehen."));
        inv.setItem(31, ms.renameItemStack(new ItemStack(Material.CARPET, 1, (byte) 1), ShopCategory.COLORFUL.getDisplayName(), "", "§fKlicke§7, um diese zu sehen."));
        inv.setItem(32, ms.renameItemStack(new ItemStack(Material.STICK), ShopCategory.MATERIALS.getDisplayName(), "", "§fKlicke§7, um diese zu sehen."));
        inv.setItem(39, ms.renameItemStack(new ItemStack(Material.REDSTONE), ShopCategory.REDSTONE.getDisplayName(), "", "§fKlicke§7, um diese zu sehen."));
        inv.setItem(40, ms.renameItemStack(new ItemStack(Material.LAVA_BUCKET), ShopCategory.MISCELLANEOUS.getDisplayName(), "", "§fKlicke§7, um diese zu sehen."));
        inv.setItem(41, ms.renameItemStack(new ItemStack(Material.APPLE), ShopCategory.FOOD.getDisplayName(), "", "§fKlicke§7, um diese zu sehen."));
        if (p.getItemInHand() != null) {
            ItemStack i = p.getItemInHand().clone();
            i.setAmount(1);
            final Material m = i.getType();
            final String itemId = i.getType().getId() + ":" + (m.getMaxDurability() > 0 ? 0 : i.getDurability());
            if (itemids.contains(itemId)) {
                inv.setItem(53, buildShopItem(itemId, System.currentTimeMillis()));
            }
        }
        if (ms.getActiveInv(p.getName()) != MuxInventory.InvType.SHOPSEARCH) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.SHOPSEARCH);
    }

    public void handleSearch(final Player p, final ItemStack i, final int slot, final Inventory inv) {
        if (inv.getTitle().contains("Suchen")) {
            final ShopCategory category = getCategoryByDisplayName(i.getItemMeta().getDisplayName());
            if (slot == 53) {
                final String fullid = i.getTypeId() + ":" + i.getDurability();
                p.closeInventory();
                final Location l = itemframelocs.get(fullid);
                ms.forcePlayer(p, l);
                p.playSound(l, Sound.ENDERMAN_TELEPORT, 0.4F, 1F);
                mostsearched.put(fullid, mostsearched.getOrDefault(fullid, 0L) + 1L);
                if (ms.checkGeneralCooldown(p.getName(), "SHOPSEARCH", 8000L, true) == false)
                    ms.runLater(new BukkitRunnable() {
                        @Override
                        public void run() {
                            ms.chatClickHoverRun(p, "§6§lMuxShop>§7 Klicke §6hier§7, um ein weiteres Item zu suchen.", "§6§oKlicke zum öffnen", "/shop search");
                        }
                    }, 30L);
                return;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            openSearchCategory(p, 0, category);
        } else if (slot == 0) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            openSearch(p);
            return;
        } else if (slot == 4) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        } else if (slot == 5 && i.getType() == Material.WATCH) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            int selected = p.hasMetadata("shoptopsel") ? p.getMetadata("shoptopsel").get(0).asInt() : 1;
            selected++;
            p.removeMetadata("shoptopsel", ms);
            p.setMetadata("shoptopsel", new FixedMetadataValue(ms, selected));
            final ShopCategory category;
            final int page;
            final ItemStack catitem = inv.getItem(4);
            if (catitem == null || catitem.getType() == null) return;
            if (catitem.getType() != ShopCategory.MOSTSEARCHED.getMaterial()) {
                page = Integer.parseInt(catitem.getItemMeta().getDisplayName().split(": ")[1]);
                category = getCategoryByDisplayName(catitem.getItemMeta().getDisplayName().split(" §0Page: ")[0]);
            } else {
                page = 0;
                category = ShopCategory.MOSTSEARCHED;
            }
            openSearchCategory(p, page, category);
            return;
        }
        final ShopCategory category;
        final int page;
        final ItemStack catitem = inv.getItem(4);
        if (catitem == null || catitem.getType() == null) return;
        if (catitem.getType() != ShopCategory.MOSTSEARCHED.getMaterial()) {
            page = Integer.parseInt(catitem.getItemMeta().getDisplayName().split(": ")[1]);
            category = getCategoryByDisplayName(catitem.getItemMeta().getDisplayName().split(" §0Page: ")[0]);
        } else {
            page = 0;
            category = ShopCategory.MOSTSEARCHED;
        }
        if (i.getType() == Material.SKULL_ITEM) {
            if (slot == 7) {
                p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                openSearchCategory(p, page - 1, category);
                return;
            } else if (slot == 8) {
                p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                openSearchCategory(p, page + 1, category);
                return;
            }
        }
        final String fullid = i.getTypeId() + ":" + i.getDurability();
        p.closeInventory();
        final Location l = itemframelocs.get(fullid);
        ms.forcePlayer(p, l);
        p.playSound(l, Sound.ENDERMAN_TELEPORT, 0.4F, 1F);
        mostsearched.put(fullid, mostsearched.getOrDefault(fullid, 0L) + 1L);
        if (ms.checkGeneralCooldown(p.getName(), "SHOPSEARCH", 8000L, true) == false)
            ms.runLater(new BukkitRunnable() {
                @Override
                public void run() {
                    ms.chatClickHoverRun(p, "§6§lMuxShop>§7 Klicke §6hier§7, um ein weiteres Item zu suchen.", "§6§oKlicke zum öffnen", "/shop search");
                }
            }, 30L);
    }

    public void openSearchCategory(final Player p, int page, final ShopCategory category) {
        final long current = System.currentTimeMillis();
        final Inventory inv = ms.getServer().createInventory(null, 54, "§0§lMuxShop§0 | " + ChatColor.stripColor(category.getDisplayName()));
        if (category == ShopCategory.MOSTSEARCHED) {
            int step = 0;
            short pos = 18;
            for (Map.Entry<String, Long> entry : mostsearched.entrySet()
                    .stream().sorted(Comparator.comparingLong(Map.Entry::getValue))
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .collect(Collectors.toList())) {
                if (step == 36) break;
                inv.setItem(pos, buildShopItem(entry.getKey(), current));
                pos++;
                step++;
            }
            inv.setItem(4, ms.renameItemStack(new ItemStack(category.getMaterial(), 1, category.getDurability()), category.getDisplayName()));
            inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
            if (ms.getActiveInv(p.getName()) != MuxInventory.InvType.SHOPSEARCH) p.closeInventory();
            p.openInventory(inv);
            ms.setActiveInv(p.getName(), MuxInventory.InvType.SHOPSEARCH);
            return;
        } else if (category == ShopCategory.TOPGAINERS || category == ShopCategory.TOPLOSERS) {
            final Pageifier<ItemStack> pages = new Pageifier<>(36);
            if (category == ShopCategory.TOPLOSERS) {
                for (final Map.Entry<String, Double> entry : getPerChange(updateTopGainLoseInventory(p, inv)).entrySet()
                        .stream().filter(entry -> entry.getValue() < 0)
                        .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                        .collect(Collectors.toList())) {
                    pages.addItem(getTopGainLoseItem(entry, current, false));
                }
            } else {
                for (final Map.Entry<String, Double> entry : getPerChange(updateTopGainLoseInventory(p, inv)).entrySet()
                        .stream()
                        .filter(entry -> entry.getValue() > 0)
                        .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                        .collect(Collectors.toList())) {
                    pages.addItem(getTopGainLoseItem(entry, current, true));
                }
            }
            openSearchInventory(p, inv, category, pages, page);
            return;
        }
        final Pageifier<ItemStack> pages = new Pageifier<>(36);
        final List<String> items = categories.get(category);
        if (items != null) {
            for (final String itemId : items) {
                pages.addItem(buildShopItem(itemId, current));
            }
        }
        if (page < 0) page = 0;
        else if (page > pages.getPageSize() - 1) page = pages.getPages().size() - 1;
        openSearchInventory(p, inv, category, pages, page);
    }

    private void openSearchInventory(final Player p, final Inventory inv, final ShopCategory category, final Pageifier<ItemStack> pages, int page) {
        page = ms.addPageButtons(inv, page, pages);
        inv.setItem(4, ms.renameItemStack(new ItemStack(category.getMaterial(), 1, category.getDurability()), category.getDisplayName() + " §0Page: " + page));
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        if (ms.getActiveInv(p.getName()) != MuxInventory.InvType.SHOPSEARCH) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.SHOPSEARCH);
    }

    private ItemStack buildShopItem(final String itemId, final long current) {
        final int id = Integer.parseInt(itemId.split(":")[0]), d = Integer.parseInt(itemId.split(":")[1]);
        final ItemStack i = new ItemStack(id, 1, (short) d);
        if (i.getType() == Material.POTION) {
            if (d > 10000) {
                i.setDurability((short) (d | 0x4000));
            }
            ms.addLore(i, "");
        }
        final double cheapest = getCheapestPrice(itemId);
        String price = "§7Das Item ist §cnicht verfügbar§7.";
        if (cheapest != -1)
            price = "§7Preis: §e" + (cheapest < 10000 ? ms.getNumberFormat(cheapest) : ms.getSFormat(cheapest)) + " MuxCoin" + (cheapest != 1 ? "s" : "");
        final String percentage, sevenpercentage, liq;
        final long itemliq = liquidity.getOrDefault(itemId, 0L);
        final double lastdayprice = lastdayprices.get(itemId), sevendaysagoprice = sevendaysagoprices.get(itemId);
        final AbstractMap.SimpleEntry<Double, Long> entry = lastknownprice.get(itemId);
        final double per = ((cheapest - lastdayprice) / lastdayprice) * 100;
        percentage = cheapest != -1 ? "§724 Stunden: " + (lastdayprice == -1 ? "§f-" : (per == 0 ? "§f" : (per < 0 ? "§c" : "§a+")) + ms.getDecimalFormat(per) + "%") :
                (entry.getKey() != -1 ? ("§7Vor " + ms.getTime((int) ((current - entry.getValue()) / 1000L)) + ": §e"
                        + (entry.getKey() < 10000 ? ms.getNumberFormat(entry.getKey()) : ms.getSFormat(entry.getKey())) + " MuxCoin" + (entry.getKey() != 1 ? "s" : ""))
                        : "");
        final double sevenper = ((cheapest - sevendaysagoprice) / sevendaysagoprice) * 100;
        sevenpercentage = "§77 Tage: " + (sevendaysagoprice == -1 ? "§f-" : (sevenper == 0 ? "§f" : (sevenper < 0 ? "§c" : "§a+")) + ms.getDecimalFormat(sevenper) + "%");
        liq = "§7Menge: §f" + ms.getNumberFormat(itemliq) + " Stück";
        if (cheapest != -1)
            ms.addLore(i, price, percentage, sevenpercentage, liq, "", "§fKlicke§7, um dich zu teleportieren.");
        else ms.addLore(i, price, percentage);
        return i;
    }

    private ItemStack getTopGainLoseItem(final Map.Entry<String, Double> entry, final long current, final boolean gain) {
        final String itemId = entry.getKey();
        final int id = Integer.parseInt(itemId.split(":")[0]), d = Integer.parseInt(itemId.split(":")[1]);
        final ItemStack i = new ItemStack(id, 1, (short) d);
        if (i.getType() == Material.POTION && d > 10000) {
            i.setDurability((short) (d | 0x4000));
        }
        final double cheapest = getCheapestPrice(itemId);
        String price = "§7Das Item ist derzeit §cnicht verfügbar§7.";
        if (cheapest != -1)
            price = "§7Preis: §e" + (cheapest < 10000 ? ms.getNumberFormat(cheapest) : ms.getSFormat(cheapest)) + " MuxCoin" + (cheapest != 1 ? "s" : "");
        final String percentage, sevenpercentage, hourpercentage, liq;
        final long itemliq = liquidity.getOrDefault(itemId, 0L);
        final double lastdayprice = lastdayprices.get(itemId), sevendaysagoprice = sevendaysagoprices.get(itemId), hourprice = lasthourprices.getOrDefault(itemId, (double) 0);
        final AbstractMap.SimpleEntry<Double, Long> entry2 = lastknownprice.get(itemId);
        final double per = ((cheapest - lastdayprice) / lastdayprice) * 100;
        percentage = cheapest != -1 ? "§724 Stunden: " + (lastdayprice == -1 ? "§f-" : (per == 0 ? "§f" : (per < 0 ? "§c" : "§a+")) + (gain ? ms.getDecimalFormat(per) : ms.getDownDecimalFormat(per)) + "%") :
                (entry2.getKey() != -1 ? ("§7Vor " + ms.getTime((int) ((current - entry2.getValue()) / 1000L)) + ": §e"
                        + (entry2.getKey() < 10000 ? ms.getNumberFormat(entry2.getKey()) : ms.getSFormat(entry2.getKey())) + " MuxCoin" + (entry2.getKey() != 1 ? "s" : ""))
                        : "");
        final double sevenper = ((cheapest - sevendaysagoprice) / sevendaysagoprice) * 100, hourper = ((cheapest - hourprice) / hourprice) * 100;
        sevenpercentage = "§77 Tage: " + (sevendaysagoprice == -1 ? "§f-" : (sevenper == 0 ? "§f" : (sevenper < 0 ? "§c" : "§a+")) + (gain ? ms.getDecimalFormat(sevenper) : ms.getDownDecimalFormat(sevenper)) + "%");
        liq = "§7Menge: §f" + ms.getNumberFormat(itemliq) + " Stück";
        hourpercentage = "§71 Stunde: " + (hourprice == -1 ? "§f-" : (hourper == 0 ? "§f" : (hourper < 0 ? "§c" : "§a+")) + (gain ? ms.getDecimalFormat(hourper) : ms.getDownDecimalFormat(hourper)) + "%");
        if (cheapest != -1)
            ms.addLore(i, price, hourpercentage, percentage, sevenpercentage, liq, "", "§fKlicke§7, um dich zu teleportieren.");
        else ms.addLore(i, price, percentage);
        return i;
    }

    private Map<String, Double> getPerChange(final LinkedHashMap<String, Double> map) {
        final Map<String, Double> change = new HashMap<>();
        for (final Map.Entry<String, AbstractMap.SimpleEntry<Double, Long>> entry : lastknownprice.entrySet()) {
            final String fullid = entry.getKey();
            final double lastprice = map.get(fullid), price = getCheapestPrice(fullid);
            if (lastprice == -1 || price == -1) continue;
            final double per = ((price - lastprice) / lastprice) * 100;
            change.put(fullid, per);
        }
        return change;
    }

    private LinkedHashMap<String, Double> updateTopGainLoseInventory(final Player p, final Inventory inv) {
        final String[] timeOption = new String[]{"1 Stunde", "24 Stunden", "7 Tage"};
        int selected = p.hasMetadata("shoptopsel") ? p.getMetadata("shoptopsel").get(0).asInt() : 1;
        if (selected >= timeOption.length) {
            selected = 0;
            p.removeMetadata("shoptopsel", ms);
            p.setMetadata("shoptopsel", new FixedMetadataValue(ms, selected));
        }
        final List<String> timeLore = new ArrayList<>(Arrays.asList("§7Bestimme den Zeitabschnitt, worüber du", "§7die größten Änderungen sehen willst.", "", "§7Klicke§7, um den Zeitraum zu §everändern§7."));
        final ItemStack timeIcon = new ItemStack(Material.WATCH);
        final ItemMeta timeMeta = timeIcon.getItemMeta();
        timeMeta.setDisplayName(ms.getSelection(timeOption, "§e", selected));
        timeMeta.setLore(timeLore);
        timeIcon.setItemMeta(timeMeta);
        inv.setItem(5, timeIcon);
        return new LinkedHashMap<>(selected == 0 ? lasthourprices : selected == 1 ? lastdayprices : sevendaysagoprices)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
    }
    public boolean handleSellCommand(final Player p, final String[] args) {
        return openSell(p, args, true);
    }

    public boolean openSell(final Player p, final String[] args, final boolean cmd) {
        if (cmd && ms.hasVoted(p) == false) {
            ms.sendNoVoteMessage(p);
            return true;
        } else if (ms.inBattle(p.getName(), p.getLocation())) {
            ms.showItemBar(p, ms.hnotinfight);
            return true;
        }
        final MuxUser u = ms.getMuxUser(p.getName());
        if (cmd == false && ms.checkGeneralCooldown(p.getName(), "SHOPPROFITS", 120000L, false) == false && u.getProfits() >= 1.0D) {
            claimProfits(p, u);
            return true;
        }
        if (args != null && args.length > 0 && ms.isTrusted(p.getName())) {
            final Map.Entry<String, OfflinePlayer> entry = ms.getPlayerAndName(args[0]);
            final OfflinePlayer pl = entry.getValue();
            if (pl == null) {
                ms.showItemBar(p, ms.hnotfound);
                return true;
            }
            openSellingInventory(p, pl.getUniqueId(), pl.getName(), 0, null);
            return true;
        }
        final ItemStack item = p.getItemInHand();
        final Material m = item.getType();
        if (m == Material.POTION) {
            final short repl = replacepotions.getOrDefault(item.getDurability(), (short) 0);
            if (repl != 0) {
                item.setDurability(repl);
                p.setItemInHand(item);
            }
        }
        final ItemMeta im = item.getItemMeta();
        if (ms.isRenamedByPlayer(item)) {
            im.setDisplayName(null);
            item.setItemMeta(im);
            p.setItemInHand(item);
        }
        final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lMuxShop§0 | Verkaufen");
        final double cheapest = m == Material.AIR ? -1 : getCheapestPrice(item.getTypeId() + ":" + item.getDurability());
        final int prem = ms.getMarket().getItemsForUUID(p.getUniqueId()).size();
        final boolean selling = hasItems(p.getUniqueId()) || prem > 0;
        final short dur = item.getDurability();
        final boolean more = item.getAmount() > 1;
        final String locale = getItemName(item);
        final double totalprofits = getTotalEarnings(p.getUniqueId()), profits = u.getProfits();
        final int sellslot = totalprofits > 0 && u.getProfits() == 0 && selling == false ? 15 : totalprofits == 0 && u.getProfits() == 0 && selling ? 11 : 13;
        if (m == Material.AIR) {
            inv.setItem(sellslot, ms.renameItemStack(new ItemStack(Material.BARRIER), "§c§lKein Item", "§7Halte ein Item in deiner Hand,", "§7um es verkaufen zu können."));
        } else if (ms.getUltra().isUltraItem(item) || ms.isMenuItem(item) || m == Material.ARMOR_STAND || m == Material.STORAGE_MINECART || m == Material.HOPPER_MINECART || (m == Material.MONSTER_EGGS && dur != (short) 6) ||
                (m == Material.ANVIL && dur != 0) || m == Material.SLIME_BLOCK || m == Material.ENCHANTED_BOOK || m == Material.EXPLOSIVE_MINECART ||
                m == Material.BOOK_AND_QUILL || (m.getMaxDurability() != 0 && dur != 0) || disabledpotions.contains(dur) ||
                (m == Material.MOB_SPAWNER && (item.getItemMeta().hasDisplayName() == false || item.getItemMeta().getDisplayName().contains("§") == false))) {
            inv.setItem(sellslot, ms.renameItemStack(new ItemStack(Material.BARRIER), "§c§lUngültiges Item", "§7Dieses Item kann nicht im", "§7Shop verkauft werden."));
        } else if (im.hasEnchants() || ms.hasGlow(item) || (im.hasDisplayName() && im.getDisplayName().contains("§") && m != Material.MOB_SPAWNER) || (m == Material.SKULL_ITEM && dur == (short) 3)
                || m == Material.MONSTER_EGG ||
                (im instanceof BannerMeta && ((BannerMeta) im).getPatterns().isEmpty() == false) || (im instanceof FireworkMeta && ((FireworkMeta) im).getEffects().isEmpty() == false) || locale == null ||
                (im instanceof FireworkEffectMeta && ((FireworkEffectMeta) im).hasEffect()) || m == Material.MAP || (im instanceof LeatherArmorMeta && ((LeatherArmorMeta) im).getColor().equals(Color.fromRGB(0xA06540)) == false)) {
            final int limit = (p.hasPermission("muxsystem.ultra") ? 15 : 5);
            if (prem >= limit) {
                inv.setItem(sellslot, ms.renameItemStack(new ItemStack(Material.BARRIER), "§c§lLimit erreicht", "§7Du kannst maximal §c" + limit + " Items §7im", "§7Premium Markt verkaufen."));
            } else {
                final List<String> lore = new ArrayList<>(Arrays.asList("§7Diese" + (more ? "" : "s") + " Item" + (more ? "s" : "") + " kannst du nur im", "§7Premium Markt verkaufen.",
                        "", "§7Klicke hier, um einen eigenen", "§ePreis pro Stück §7festzulegen."));
                if (im.hasEnchants()) lore.add(0, "");
                inv.setItem(sellslot, ms.renameItemStack(item.clone(), "§e§lPremium Item verkaufen", lore));
            }
        } else if (cheapest != -1) {
            final String pr = cheapest < 10000 ? ms.getNumberFormat(cheapest) : ms.getSFormat(cheapest);
            int count = 0;
            for (final ItemStack is : p.getInventory()) {
                if (is != null && is.getType() == m && is.getData().getData() == dur) {
                    if ((is.getEnchantments().isEmpty() == false || ms.getUltra().isUltraItem(is) || ms.isMenuItem(is))) continue;
                    count++;
                }
            }
            inv.setItem(sellslot, ms.renameItemStack(item.clone(), "§a§l" + (item.getAmount() > 1 ? "Items" : "Item") + " verkaufen",
                    "§7Marktpreis: §e" + pr + " MuxCoins" + (more ? " §8x" + count : ""), "",
                    "§7Linksklick, um §azum Marktpreis §7" + (more ? "die" : "das"), "§7" + (more ? "Items" : "Item") + " zum Verkauf zu stellen.", "",
                    "§7Rechtsklick, um einen eigenen", "§aPreis pro Stück §7festzulegen."));
            if (args != null && args.length > 0) {
                if (ms.isNumeric(args[0])) {
                    sellHandForCoins(p, args[0], false);
                    return true;
                } else if (args[0].equals("markt")) {
                    sellHand(p, cheapest);
                    return true;
                } else {
                    ms.showItemBar(p, ms.usage("/sell [coins | markt]"));
                    return true;
                }
            }
        } else {
            inv.setItem(sellslot, ms.renameItemStack(item.clone(), "§a§l" + (item.getAmount() > 1 ? "Items" : "Item") + " verkaufen",
                    "§7Zurzeit stellt noch niemand", "§7das Item zum Verkauf.", "", "§7Klicke hier, um einen eigenen", "§aPreis pro Stück §7festzulegen."));
        }
        if (selling)
            inv.setItem(totalprofits == 0 ? 15 : 16, ms.renameItemStack(new ItemStack(Material.STORAGE_MINECART), ms.getLang("market.myoffers"),
                    "§7Hier siehst du alle deine Items,", "§7die noch zum Verkauf stehen.", "", "§fKlicke§7, um diese zu sehen."));
        if (profits >= 1.00D)
            inv.setItem(selling ? 7 : 16, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT), "§e§l" + ms.getNumberFormat(profits) + " MuxCoins",
                    "§7Die gesamten Einnahmen all deiner", "§7Verkäufe kannst du hier abholen.", "", "§7Klicke, um sie §ejetzt einzusammeln§7."));
        if (totalprofits > 0)
            inv.setItem(10, ms.renameItemStack(new ItemStack(Material.BOOK), "§f§lEinnahmen", "§7Schaue nach, mit welchen Items", "§7du am besten verdient hast.", "", "§fKlicke§7, um diese zu sehen§7."));
        if (ms.getActiveInv(p.getName()) != MuxInventory.InvType.SHOP) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.SHOP);
        return true;
    }

    public void updateTop() {
        new BukkitRunnable() {
            @Override
            public void run() {
                final List<ShopItem> bestsellers = ms.getDB().getBestSellers();
                final Map<String, Long> topsold = ms.getDB().get5ItemsWithMostVolume24H().entrySet()
                        .stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        for (final MuxHolograms.Hologram holo : topholos) {
                            holo.remove();
                        }
                        topholos.clear();
                        int step = 0;
                        final MuxHolograms h = ms.getHolograms();
                        final Location items = new Location(ms.getWarpsWorld(), -76.5D, 32D, 444.5D), sellers = new Location(ms.getWarpsWorld(), -112.5D, 32D, 444.5D);
                        topholos.add(h.addHologram("topitems", items.clone().add(0, 0.4, 0), "§a§lBeliebteste Items"));
                        topholos.add(h.addHologram("bestsellers", sellers.clone().add(0, 0.4, 0), "§a§lTeuerste Blöcke"));
                        for (final Map.Entry<String, Long> entry : topsold.entrySet()) {
                            if (step == 5) break;
                            final String text = "§f" + getItemName(entry.getKey()) + " - §a" + ms.getSFormat(entry.getValue()) + " MuxCoins";
                            step++;
                            topholos.add(h.addHologram("topitems" + step, items.clone().add(0, -0.4 * step, 0), text));
                        }
                        step = 0;
                        for (final ShopItem si : bestsellers) {
                            if (step == 5) break;
                            final ItemStack is = si.getItemStack();
                            final byte data = is.getData().getData();
                            if (is.getType().isBlock() == false || (is.getType() == Material.DOUBLE_PLANT && (data == (byte) 2 || data == (byte) 3))) continue;
                            final String text = "§f" + getItemName(is) + " - §a" + ms.getNumberFormat(si.getPrice()) + " MuxCoins";
                            step++;
                            topholos.add(h.addHologram("topsellers" + step, sellers.clone().add(0, -0.4 * step, 0), text));
                        }
                    }
                }.runTask(ms);
            }
        }.runTaskAsynchronously(ms);
    }

    public void handleSell(final Player p, final ItemStack i, final int slot, final Inventory inv, final boolean rightclick) {
        final String title = inv.getTitle();
        if (title.contains("Verkaufen")) {
            final String display = i.getItemMeta().getDisplayName();
            if (i.getType() == Material.STORAGE_MINECART && display.contains("verkaufen") == false) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                openSellingInventory(p, p.getUniqueId(), null, 0, null);
                return;
            }
            if (i.getType() == Material.BARRIER && display.startsWith("§c")) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            } else if (i.getType() == Material.DOUBLE_PLANT && display.contains("verkaufen") == false) {
                claimProfits(p, ms.getMuxUser(p.getName()));
                p.closeInventory();
                return;
            } else if (i.getType() == Material.BOOK && display.contains("verkaufen") == false) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                openEarningsInventory(p, 0);
                return;
            }
            final boolean premium = display.startsWith("§e§l");
            final double cheapest = getCheapestPrice(i.getTypeId() + ":" + i.getDurability());
            if (cheapest == -1 || rightclick || premium) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                new MuxAnvil(ms, (input, player) -> sellHandForCoins(p, input, premium)).show(p, "MuxCoins: 0", new ItemStack(Material.DOUBLE_PLANT));
            } else if (slot == 11 || slot == 15 || slot == 13) {
                sellHand(p, cheapest);
            }
        } else {
            final boolean my = title.contains("Verkäufe") || title.contains("Teure") || title.contains("Einnahmen");
            final String name = my ? null : title.split("\\| ")[1].replace(" ", "");
            final OfflinePlayer op = (my == false ? ms.getPlayer(name) : p);
            if (slot < 9) {
                if (slot == 7) {
                    p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                    if (title.contains("Einnahmen")) {
                        openEarningsInventory(p, getPageNumber(inv.getItem(4)) - 1);
                        return;
                    }
                    final String cat = getCategory(inv.getItem(4));
                    openSellingInventory(p, op.getUniqueId(), name, getPageNumber(inv.getItem(4)) - 1, cat);
                } else if (slot == 8) {
                    p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                    if (title.contains("Einnahmen")) {
                        openEarningsInventory(p, getPageNumber(inv.getItem(4)) + 1);
                        return;
                    }
                    final String cat = getCategory(inv.getItem(4));
                    openSellingInventory(p, op.getUniqueId(), name, getPageNumber(inv.getItem(4)) + 1, cat);
                } else if (slot == 0) {
                    p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                    final String cat = title.endsWith("| P") || title.contains("Premium Verkäufe") || title.contains("Teure") ? "P" : getCategory(inv.getItem(4));
                    if (cat != null) openSellingInventory(p, op.getUniqueId(), name, 0, null);
                    else if (my == false) ms.getOverview().openDataManagement(p, op);
                    else openSell(p, null, false);
                } else if (slot == 5) {
                    final String display = i.getItemMeta().getDisplayName();
                    p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                    if (display.contains("Teure")) {
                        openSellingInventory(p, op.getUniqueId(), name, 0, "EXPENSIVE");
                        return;
                    }
                    final String cat = getCategory(inv.getItem(4));
                    changePrice(p, cat, getPageNumber(inv.getItem(4)) + 1, false);
                } else {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                }
                return;
            }
            if (title.contains("Einnahmen")) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            final boolean premium = title.endsWith("| P") || title.contains("Premium Verkäufe");
            if (i.getType() == Material.INK_SACK && i.getDurability() == (short) 11 && ms.hasGlow(i)) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                if (my) openPremiumSelling(p, p.getUniqueId(), null);
                else openPremiumSelling(p, op.getUniqueId(), name);
                return;
            } else if (premium == false && i.getItemMeta().getLore().get(0).contains("Preis") == false) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                openSellingInventory(p, op.getUniqueId(), name, 0, i.getTypeId() + ":" + i.getDurability());
                return;
            }
            if (p.getInventory().firstEmpty() == -1) {
                p.closeInventory();
                ms.showItemBar(p, "§cDein Inventar ist voll.");
                return;
            }
            final UUID uuid = my ? p.getUniqueId() : op.getUniqueId();
            if (premium) {
                for (final String s : i.getItemMeta().getLore()) {
                    if (s.startsWith("§0Id: ")) {
                        final String id = s.split(" ")[1];
                        final PremiumItem mi = ms.getMarket().getItemFromId(Long.parseLong(ChatColor.stripColor(id)));
                        if (mi != null && mi.isValid() == false) {
                            updatePremiumItem(mi);
                            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
                        } else {
                            removePremiumItem(p, mi);
                            p.playSound(p.getLocation(), Sound.CHICKEN_EGG_POP, 5, 5);
                        }
                    }
                }
                openPremiumSelling(p, uuid, title.endsWith("| P") ? name : null);
                return;
            }
            final double price = Double.parseDouble(ms.removeNonCentDigits(ChatColor.stripColor(i.getItemMeta().getLore().get(0))));
            if (title.contains("Teure Items")) {
                final String fullid = i.getTypeId() + ":" + i.getDurability();
                if (rightclick) {
                    p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                    changePrice(p, fullid, getPageNumber(inv.getItem(4)) + 1, true);
                    return;
                }
                final double cheapest = getCheapestPrice(fullid);
                changePrices(p.getUniqueId(), i.getTypeId() + ":" + i.getDurability(), cheapest);
                openSellingInventory(p, p.getUniqueId(), null, getPageNumber(inv.getItem(4)) + 1, "EXPENSIVE");
                p.playSound(p.getLocation(), Sound.SUCCESSFUL_HIT, 1F, 1.6F);
                return;
            } else {
                if (my == false && ms.isFullTrusted(p.getName()) == false) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                for (final ShopItem item : getItemsOf(uuid, i.getTypeId() + ":" + i.getDurability(), price)) {
                    if (p.getInventory().firstEmpty() == -1) {
                        ms.showItemBar(p, "§eDein Inventar ist voll, somit wurden nur einige Items entfernt.");
                        break;
                    }
                    final ItemStack is = item.getItemStack();
                    is.setAmount(item.getAmount());
                    p.getInventory().addItem(is);
                    if (item.getItemStack().getType() == Material.POTION) {
                        ms.stackItems(p, Material.POTION, false);
                        p.updateInventory();
                    }
                    if (my == false)
                        ms.getHistory().addHistory(uuid, p.getUniqueId(), "TEAMACTION", "SHOP REMOVE", is.getAmount() + "x " + is.getType());
                    removeItem(uuid, null, item);
                }
                p.playSound(p.getLocation(), Sound.CHICKEN_EGG_POP, 5, 5);
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    updateShop();
                }
            }.runTaskAsynchronously(ms);
            final String cat = getCategory(inv.getItem(4));
            openSellingInventory(p, op.getUniqueId(), name, getPageNumber(inv.getItem(4)), cat);
        }
    }

    private void changePrice(final Player p, final String id, final int page, final boolean expensive) {
        new MuxAnvil(ms, (input, player) -> {
            int coins;
            try {
                coins = Integer.parseInt(ms.removeNonDigits(ms.retardCleaner(input, "MuxCoins: 0")));
            } catch (final NumberFormatException e) {
                ms.showItemBar(player, ms.hnonumber);
                return;
            }
            if (coins > getMaxPrice()) {
                ms.showItemBar(player, String.format(ms.getLang("market.maxworth"), ms.getNumberFormat(getMaxPrice())));
                return;
            } else if (coins < 1) {
                ms.showItemBar(player, ms.getLang("market.minworth"));
                return;
            }
            changePrices(p.getUniqueId(), id, coins);
            openSellingInventory(p, p.getUniqueId(), null, page, expensive ? "EXPENSIVE" : id);
            p.playSound(p.getLocation(), Sound.SUCCESSFUL_HIT, 1F, 1.6F);
        }).show(p, "MuxCoins: 0", new ItemStack(Material.DOUBLE_PLANT));
    }

    private void claimProfits(final Player p, final MuxUser u) {
        final double profits = u.getProfits();
        u.claimProfits();
        ms.sendTitle(p, "§e§lMuxShop", 10, 40, 10);
        ms.sendSubTitle(p, "§fDu hast §e" + ms.getNumberFormat(profits) + " MuxCoin" + (profits != 1L ? "s" : "") + " §fverdient.", 10, 40, 10);
        p.playSound(p.getLocation(), Sound.NOTE_PIANO, 2F, 1F);
        ms.saveMuxUser(u);
        ms.sendScoreboard(p);
        ms.checkGeneralCooldown(p.getName(), "SHOPPROFITS", 120000L, true);
        ms.getHistory().addHistory(p.getUniqueId(), null, "COINS", String.valueOf(profits), "Shop");
    }

    private void sellHandForCoins(final Player p, final String input, final boolean premium) {
        if (premium == false) {
            double coins;
            try {
                coins = Double.parseDouble(ms.removeNonCentDigits(ms.retardCleaner(input, "MuxCoins: 0")));
            } catch (final NumberFormatException e) {
                ms.showItemBar(p, ms.hnonumber);
                return;
            }
            if (coins >= 1D) {
                try {
                    coins = Integer.parseInt(ms.removeNonDigits(ms.retardCleaner(input, "MuxCoins: 0")));
                } catch (final NumberFormatException e) {
                    ms.showItemBar(p, "§cCent Beträge sind nur unter einem MuxCoin möglich.");
                    return;
                }
            }
            if (BigDecimal.valueOf(coins).scale() > 2)
                coins = BigDecimal.valueOf(coins).setScale(2, RoundingMode.UP).doubleValue();
            if (coins > getMaxPrice()) {
                ms.showItemBar(p, String.format(ms.getLang("market.maxworth"), ms.getNumberFormat(getMaxPrice())));
                return;
            } else if (coins < 0.01D) {
                ms.showItemBar(p, "§cDer minimale Wert ist 0.01 MuxCoins.");
                return;
            }
            sellHand(p, coins);
        } else {
            final ItemStack item = p.getItemInHand();
            if (item == null || item.getType() == Material.AIR) {
                ms.showItemBar(p, ms.getLang("market.notininventory"));
                return;
            }
            int coinsAsInt;
            try {
                coinsAsInt = Integer.parseInt(ms.removeNonDigits(ms.retardCleaner(input, "MuxCoins: 0")));
            } catch (final NumberFormatException e) {
                ms.showItemBar(p, ms.hnonumber);
                return;
            }
            if (coinsAsInt > getMaxPrice()) {
                ms.showItemBar(p, String.format(ms.getLang("market.maxworth"), ms.getNumberFormat(getMaxPrice())));
                return;
            } else if (coinsAsInt <= 1) {
                ms.showItemBar(p, ms.getLang("market.minworth"));
                return;
            }
            if (ms.getMarket().addPremiumItem(p, item.clone(), coinsAsInt * item.getAmount()) == false) {
                ms.showItemBar(p, "§cDas Item konnte nicht verkauft werden.");
                return;
            }
            p.setItemInHand(null);
            p.updateInventory();
            ms.getShop().openPremiumSelling(p, p.getUniqueId(), null);
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
        }
    }

    public long getMaxPrice() {
        final long supply = ms.getActiveMuxCoinsSupply();
        final double percentage = 0.03D;
        long result = (long) Math.ceil((supply * percentage));
        int n = (int) Math.ceil(Math.log10(result + 1D));
        n = Math.max(0, n - 1);
        result = (long) Math.ceil(result / Math.pow(10, n)) * (long) Math.pow(10, n);
        return Math.max(result, 1000L);
    }

    private void sellHand(final Player p, final double price) {
        final ItemStack item = p.getItemInHand();
        p.closeInventory();
        if (item == null || item.getType() == Material.AIR) {
            ms.showItemBar(p, "§cDu musst ein Item in der Hand halten.");
            return;
        }
        p.playSound(p.getLocation(), Sound.CHICKEN_EGG_POP, 5, 5);
        final Set<ItemStack> remove = new HashSet<>();
        int totalamount = 0;
        ms.stackItems(p, item.getType(), false);
        for (final ItemStack i : p.getInventory().getContents()) {
            if (i == null || i.getType() != item.getType() || i.getDurability() != item.getDurability()) continue;
            if (i.getEnchantments().isEmpty() == false || ms.getUltra().isUltraItem(i) || ms.isMenuItem(i)) continue;
            final ItemStack copy = i.clone();
            int amount = i.getAmount();
            totalamount += amount;
            final ShopItem si = new ShopItem(-1, null, copy.getTypeId() + ":" + copy.getDurability(), price, amount);
            if (addItem(p.getUniqueId(), si) == false) {
                continue;
            }
            remove.add(i);
        }
        ms.showItemBar(p, (totalamount == 1 ? "§fDas Item wurde" : "§fDeine Items wurden") + " erfolgreich zum Shop §ahinzugefügt§f.");
        for (final ItemStack i : remove) {
            p.getInventory().remove(i);
        }
        p.updateInventory();
        new BukkitRunnable() {
            @Override
            public void run() {
                updateShop();
            }
        }.runTaskAsynchronously(ms);
    }

    private int getPageNumber(final ItemStack is) {
        return Integer.parseInt(is.getItemMeta().getDisplayName().split(" ")[3]);
    }

    private String getCategory(final ItemStack is) {
        final String[] split = is.getItemMeta().getDisplayName().split(" ");
        return split.length < 5 ? null : is.getItemMeta().getDisplayName().split(" ")[4];
    }

    public List<String> getItemIDs() {
        return itemids;
    }

    private void openEarningsInventory(final Player p, int page) {
        final Inventory inv = ms.getServer().createInventory(null, 27, "§0§lMuxShop§0 | Einnahmen");
        final Pageifier<ItemStack> pages = new Pageifier<>(9);
        double totalearned = getTotalEarnings(p.getUniqueId());
        for (final ShopItem item : getSoldItems(p.getUniqueId())) {
            ItemStack cat;
            if (item.getId() == 0) {
                cat = ms.renameItemStack(ms.addGlow(new ItemStack(Material.INK_SACK, 1, (byte) 11)), "§e§lPremium Verkäufe",
                        "§7Gewinn: §e" + ms.getNumberFormat(item.getPrice()) + " MuxCoins", "§7Anzahl: §f" + item.getAmount() + " Items");
            } else {
                cat = ms.renameItemStack(item.getItemStack(), "", "§7Gewinn: §e" + ms.getNumberFormat(item.getPrice()) + " MuxCoins",
                        "§7Anzahl: §f" + (item.getAmount() > 63 ? (int) Math.floor(item.getAmount() / 64D) : item.getAmount()) + " " + (item.getAmount() > 63 ? "Stacks" : "Stück"));
            }
            pages.addItem(cat);
        }
        page = ms.addPageButtons(inv, page, pages);
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.BOOK), "§f§lEinnahmen" + " §0. Page " + page, "§7Schaue nach, mit welchen Items", "§7du am besten verdient hast.",
                "", "§7Gesamtgewinn: §e" + (totalearned > 999999 ? ms.getSFormat(totalearned) : ms.getNumberFormat(totalearned)) + " MuxCoins"));
        if (ms.getActiveInv(p.getName()) != MuxInventory.InvType.SHOP) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.SHOP);
    }

    public void openSellingInventory(final Player p, final UUID uuid, final String name, int page, final String id) {
        final boolean expensive = id != null && id.equals("EXPENSIVE");
        final Inventory inv = ms.getServer().createInventory(null, 54, "§0§lMuxShop§0 | " + (expensive ? "Teure Items" : name != null ? name : "Meine Verkäufe"));
        final Pageifier<ItemStack> pages = id == null ? getItems(uuid) : expensive ? getExpensiveItems(uuid) : getItems(uuid, id);
        page = Math.min(page, pages.getPages().size() - 1);
        final List<ItemStack> items = pages.getPage(page);
        if (id != null && items.isEmpty()) {
            openSellingInventory(p, uuid, name, 0, null);
            return;
        }
        for (int i = 0; i < 18; i++) {
            inv.setItem(i, new ItemStack(Material.BARRIER));
        }
        for (final ItemStack item : items) {
            inv.addItem(item);
        }
        for (int i = 0; i < 18; i++) {
            inv.setItem(i, null);
        }
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        if (name != null)
            inv.setItem(4, ms.renameItemStack(new ItemStack(Material.STORAGE_MINECART), "§f§l" + name + " §0_ Page " + page + (id != null ? " " + id : ""), "§7Hier siehst du alle Items von dem", "§7Spieler, die noch zum Verkauf stehen."));
        else if (expensive)
            inv.setItem(4, ms.renameItemStack(new ItemStack(Material.GOLD_NUGGET), "§e§lTeure Items" + " §0Page " + page,
                    "§7Einige der Items befinden sich", "§7derzeit über dem Marktpreis."));
        else
            inv.setItem(4, ms.renameItemStack(new ItemStack(Material.STORAGE_MINECART), ms.getLang("market.myoffers") + " §0Page " + page + (id != null ? " " + id : ""), "§7Hier siehst du alle deine Items,", "§7die noch zum Verkauf stehen."));
        if (id == null && name == null && hasExpensiveItems(uuid)) {
            inv.setItem(5, ms.renameItemStack(new ItemStack(Material.GOLD_NUGGET), "§e§lTeure Items",
                    "§7Einige der Items befinden sich", "§7derzeit über dem Marktpreis.", "", "§eKlicke§7, um diese anzuzeigen."));
        }
        if (id != null && name == null && expensive == false) {
            final ItemStack first = items.get(0);
            final double cheapest = getCheapestPrice(first.getTypeId() + ":" + first.getDurability());
            if (getMostExpensiveItemOf(p.getUniqueId(), first.getTypeId(), first.getDurability()) > cheapest)
                inv.setItem(5, ms.renameItemStack(new ItemStack(Material.DOUBLE_PLANT), "§e§lNeuer Preis", "§7Dein Preis liegt derzeit über", "§7dem jetzigen Marktpreis.",
                        "", "§7Marktpreis: §e" + ms.getNumberFormat(cheapest) + " MuxCoins", "", "§7Klicke, um ihn §eneu zu setzen§7."));
        }
        if (page != 0)
            inv.setItem(7, ms.getHeadFromURL("https://textures.minecraft.net/texture/bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9", ms.getLang("market.previouspage")));
        if (page < pages.getPages().size() - 1)
            inv.setItem(8, ms.getHeadFromURL("https://textures.minecraft.net/texture/19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf", ms.getLang("market.nextpage")));
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.SHOP && it != MuxInventory.InvType.USERCONTROL) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.SHOP);
    }

    public void openPremiumSelling(final Player p, final UUID uuid, final String name) {
        final Inventory inv = ms.getServer().createInventory(null, 54, "§0§lMuxShop§0 | " + (name != null ? name + " | P" : "Premium Verkäufe"));
        final Pageifier<PremiumItem> pages = ms.getMarket().getPremiumItemsForPlayerAsPage(uuid);
        final List<PremiumItem> items = pages.getPage(0);
        if (items.isEmpty()) {
            openSellingInventory(p, uuid, name, 0, null);
            return;
        }
        int i = 18;
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 11), name != null ? "§e§l" + name + "§0_ Page " : "§e§lPremium Verkäufe", name != null ?
                new String[]{"§7Die Premium Items vom Spieler", "§7sind hier komplett aufgelistet."} :
                new String[]{"§7Deine Items vom Premium Markt", "§7sind hier komplett aufgelistet."}));
        for (final PremiumItem mi : items) {
            if (mi.getItem() == null) {
                continue;
            }
            final ItemStack item = mi.getItem().clone();
            final boolean valid = mi.isValid();
            inv.setItem(i, ms.renameItemStackAddLore(item,
                    item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null ? item.getItemMeta().getDisplayName() : "",
                    "", "§7Dein Preis: §e" + ms.getNumberFormat(mi.getPrice()) + " MuxCoins", "",
                    valid ? "§7" + ms.getLang("market.expiresin") + ": §f" + ms.getMarket().getExpiry(mi) : "§c" + ms.getLang("market.expired"),
                    "§0Id: " + mi.getKeyId(), "§7Klicke, um " + (item.getAmount() > 1 ? "diese Items" : "dieses Item") + (valid ? " aus" : " erneut"), valid ? "§7dem Markt zu §centfernen§7." : "§7in dem Markt zu §averkaufen§7."));
            i++;
        }
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.SHOP) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.SHOP);
    }

    private double getTotalEarnings(final UUID uuid) {
        return getTotalEarnings(uuid, false);
    }

    private double getTotalEarnings(final UUID uuid, final boolean recache) {
        Double totalearnings = earningscache.get(uuid);
        if (totalearnings == null || recache) {
            totalearnings = ms.getDB().getTotalEarnings(uuid);
            earningscache.put(uuid, totalearnings);
        }
        return totalearnings;
    }

    public List<ShopItem> getSoldItems(final UUID uuid) {
        return getSoldItems(uuid, false);
    }

    private List<ShopItem> getSoldItems(final UUID uuid, final boolean recache) {
        List<ShopItem> items = soldcache.get(uuid);
        if (items == null || recache) {
            items = ms.getDB().getSoldItems(uuid);
            soldcache.put(uuid, items);
        }
        return items;
    }

    private boolean hasItems(final UUID uuid) {
        return !getItemsOf(uuid).isEmpty();
    }

    public List<ShopItem> getItemsOf(final UUID uuid) {
        return cache.computeIfAbsent(uuid, u -> ms.getDB().getShopItemsOf(u));
    }

    private List<ShopItem> getItemsOf(final UUID uuid, final String fullid, final double price) {
        final List<ShopItem> items = getItemsOf(uuid), result = new ArrayList<>();
        for (final ShopItem item : items) {
            if (item.getFullId().equals(fullid) && item.getPrice() == price) result.add(item);
        }
        return result;
    }

    private Pageifier<ItemStack> getItems(final UUID uuid) {
        return getItems(uuid, null);
    }

    private Pageifier<ItemStack> getItems(final UUID uuid, final String id) {
        final Pageifier<ItemStack> is = new Pageifier<>(36);
        double lastprice = 0;
        int amount = 1, lastid = 0;
        short lastdurability = 0;
        final Set<String> categories = new HashSet<>();
        ItemStack itemStack = null;
        final int premium = ms.getMarket().getPremiumItemsForPlayerAsPage(uuid).getPage(0).size();
        if (id == null && premium > 0) {
            is.addItem(ms.addGlow(ms.renameItemStack(new ItemStack(Material.INK_SACK, premium, (byte) 11), "§e§lPremium Verkäufe",
                    "§7Deine Items vom Premium Markt", "§7sind hier komplett aufgelistet.", "", "§eKlicke§7, um diese zu sehen.")));
        }
        for (final ShopItem shopItem : getItemsOf(uuid)) {
            final String fullid = shopItem.getFullId();
            if ((id == null || fullid.equals(id))) {
                final int itemid = shopItem.getId();
                final short durability = shopItem.getDurability();
                if (id == null && categories.contains(fullid)) {
                    if (itemStack != null && itemid == itemStack.getTypeId() && durability == itemStack.getDurability())
                        itemStack = null;
                    continue;
                }
                if (lastprice == shopItem.getPrice() && lastid == itemid && lastdurability == durability && amount < 64) {
                    amount += shopItem.getAmount();
                    continue;
                } else if (itemStack != null) {
                    itemStack.setAmount(Math.min(64, amount));
                    is.addItem(itemStack);
                    if (amount - 64 > 0) {
                        final ItemStack clone = itemStack.clone();
                        clone.setAmount(amount - 64);
                        is.addItem(clone);
                    }
                }
                final double sameamount = id != null ? 2 : getSameItemsAmount(uuid, shopItem.getPrice(), itemid, durability);
                if (id == null) {
                    final Map.Entry<Double, Boolean> invinfo = getSameItemsDiffPrice(uuid, itemid, durability);
                    if (sameamount > 64 || invinfo.getValue()) {
                        final ItemStack cat = ms.renameItemStack(shopItem.getItemStack(), "", "§7Du stellst derzeit mehrere", "§7Stacks davon zum Verkauf.", "", "§fKlicke§7, für eine Übersicht.");
                        cat.setAmount((int) Math.max(1, Math.min(64, invinfo.getKey())));
                        is.addItem(cat);
                        categories.add(fullid);
                        if (sameamount > 64) itemStack = null;
                        continue;
                    }
                }
                double cheapest = getCheapestPrice(fullid);
                final boolean more = sameamount > 1;
                itemStack = ms.renameItemStack(shopItem.getItemStack(), "", "§7Dein Preis: §e" + ms.getNumberFormat(shopItem.getPrice()) + " MuxCoins",
                        "§7Marktpreis: §e" + ms.getNumberFormat(cheapest) + " MuxCoins", "", "§7Klicke, um " + (more ? "diese Items" : "dieses Item") + " aus", "§7dem Markt zu §centfernen§7.");
                lastprice = shopItem.getPrice();
                lastid = itemid;
                lastdurability = durability;
                amount = shopItem.getAmount();
            }
        }
        if (itemStack != null) {
            itemStack.setAmount(Math.min(64, amount));
            is.addItem(itemStack);
            if (amount - 64 > 0) {
                final ItemStack clone = itemStack.clone();
                clone.setAmount(amount - 64);
                is.addItem(clone);
            }
        }
        return is;
    }

    private Pageifier<ItemStack> getExpensiveItems(final UUID uuid) {
        final Pageifier<ItemStack> is = new Pageifier<>(36);
        double lastprice = 0;
        int amount = 1, lastid = 0;
        short lastdurability = 0;
        final Set<String> categs = new HashSet<>();
        ItemStack itemStack = null;
        for (final ShopItem shopItem : getItemsOf(uuid)) {
            final String fullid = shopItem.getFullId();
            final double cheapest = getCheapestPrice(fullid);
            if (shopItem.getPrice() <= cheapest) continue;
            final int itemid = shopItem.getId();
            final short durability = shopItem.getDurability();
            if (categs.contains(fullid)) {
                if (itemStack != null && itemid == itemStack.getTypeId() && durability == itemStack.getDurability())
                    itemStack = null;
                continue;
            }
            if (lastprice == shopItem.getPrice() && lastid == itemid && lastdurability == durability && amount < 64) {
                amount += shopItem.getAmount();
                continue;
            } else if (itemStack != null) {
                itemStack.setAmount(1);
                is.addItem(itemStack);
            }
            final double sameamount = getSameItemsAmount(uuid, shopItem.getPrice(), itemid, durability);
            final Map.Entry<Double, Boolean> invinfo = getSameItemsDiffPrice(uuid, itemid, durability);
            if (sameamount > 64 || invinfo.getValue()) {
                final ItemStack cat = ms.renameItemStack(shopItem.getItemStack(), "", "§7Dein Preis: §e" + ms.getNumberFormat(getMostExpensiveItemOf(uuid, itemid, durability)) + " MuxCoins",
                        "§7Marktpreis: §e" + ms.getNumberFormat(cheapest) + " MuxCoins", "", "§7Linksklick, um dieses Item auf", "§7den §eMarktpreis zu setzen§7.", "",
                        "§7Rechtsklick, um für dieses Item", "§7einen §eneuen Preis festzulegen§7.");
                cat.setAmount(1);
                is.addItem(cat);
                categs.add(fullid);
                if (sameamount > 64) itemStack = null;
                continue;
            }
            itemStack = ms.renameItemStack(shopItem.getItemStack(), "", "§7Dein Preis: §e" + ms.getNumberFormat(shopItem.getPrice()) + " MuxCoins",
                    "§7Marktpreis: §e" + ms.getNumberFormat(cheapest) + " MuxCoins", "", "§7Linksklick, um dieses Item auf", "§7den §eMarktpreis zu setzen§7.", "",
                    "§7Rechtsklick, um für dieses Item", "§7einen §eneuen Preis festzulegen§7.");
            lastprice = shopItem.getPrice();
            lastid = itemid;
            lastdurability = durability;
            amount = shopItem.getAmount();
        }
        if (itemStack != null) {
            itemStack.setAmount(1);
            is.addItem(itemStack);
        }
        return is;
    }

    private boolean hasExpensiveItems(final UUID uuid) {
        for (final ShopItem item : getItemsOf(uuid)) {
            final double cheapest = getCheapestPrice(item.getId() + ":" + item.getDurability());
            if (item.getPrice() > cheapest) {
                return true;
            }
        }
        return false;
    }

    private Map.Entry<Double, Boolean> getSameItemsDiffPrice(final UUID uuid, final int id, final short dur) {
        double lastprice = 0, totalamt = 0;
        boolean diff = false;
        for (final ShopItem item : getItemsOf(uuid)) {
            if (item.getId() == id && item.getDurability() == dur) {
                if (lastprice != 0 && lastprice != item.getPrice()) diff = true;
                lastprice = item.getPrice();
                totalamt += item.getAmount();
            }
        }
        return new AbstractMap.SimpleEntry<>(totalamt, diff);
    }

    private double getSameItemsAmount(final UUID uuid, double price, final int id, final short dur) {
        int sameamount = 0;
        for (final ShopItem item : getItemsOf(uuid)) {
            if (item.getPrice() == price && item.getId() == id && item.getDurability() == dur) {
                sameamount += item.getAmount();
            }
        }
        return sameamount;
    }

    private double getMostExpensiveItemOf(final UUID uuid, final double id, final short dur) {
        double maximumPrice = -1;
        for (final ShopItem item : getItemsOf(uuid)) {
            if (item.getId() == id && item.getDurability() == dur && (maximumPrice == -1 || item.getPrice() > maximumPrice)) {
                maximumPrice = item.getPrice();
            }
        }
        return maximumPrice;
    }

    private void changePrices(final UUID uuid, final String id, final double price) {
        getItemsOf(uuid).forEach(item -> {
            if (item.getFullId().equals(id)) {
                item.setPrice(price);
            }
        });
        ms.getDB().changePrices(uuid, id, price);
        getCheapestPrice(id, true);
    }

    private boolean addItem(final UUID uuid, final ShopItem item) {
        if (ms.getDB().addItem(uuid, item) == false) {
            return false;
        }
        getItemsOf(uuid).add(item);
        getCheapestPrice(item.getFullId(), true);
        return true;
    }

    private boolean removeItem(final UUID uuid, final MuxUser u, final ShopItem item) {
        if (ms.getDB().removeItem(item) == false) {
            return false;
        }
        final String fullid = item.getFullId();
        final List<ShopItem> items = getItemsOf(uuid);
        items.removeIf(i -> i.getKeyId() == item.getKeyId());
        if (u != null && items.stream().anyMatch(i -> i.getFullId().equals(fullid)) == false) {
            final OfflinePlayer op = ms.getServer().getOfflinePlayer(uuid);
            ms.getChat().sendOfflineMessageOrChat(op, u, "MuxShop", "§7Dein Item " + getItemName(item.getItemStack()) + " wurde komplett abgekauft.", "§e", null);
        }
        getCheapestPrice(fullid, true);
        return true;
    }

    private void updateAmount(final UUID uuid, final ShopItem item) {
        getItemsOf(uuid).forEach(i -> {
            if (i.getKeyId() == item.getKeyId()) i.setAmount(item.getAmount());
        });
        ms.getDB().changeItem(item);
    }

    public double getCheapestPrice(final String fullid) {
        return getCheapestPrice(fullid, false);
    }

    public double getCheapestPrice(final String fullid, final boolean recache) {
        double c = recache ? -2D : cheapest.getOrDefault(fullid, -2D);
        if (c == -2D) {
            c = ms.getDB().getCheapestItem(fullid);
            cheapest.put(fullid, c);
        }
        return c;
    }

    private void updatePremiumItem(final PremiumItem pi) {
        pi.update();
        ms.getDB().updatePremiumItem(pi);
    }

    public Location getShopLocation() {
        return shoploc;
    }

    private void removePremiumItem(final Player p, final PremiumItem pi) {
        if (pi == null) {
            ms.showItemBar(p, ms.getLang("market.notfound"));
            return;
        }
        final boolean owner = pi.getOwner().toString().equalsIgnoreCase(p.getUniqueId().toString());
        if (owner == false && ms.isFullTrusted(p.getName()) == false) {
            ms.showItemBar(p, ms.getLang("market.notowner"));
            return;
        }
        ms.getMarket().removePremiumItem(p, pi, owner);
        if (owner == false)
            ms.getHistory().addHistory(pi.getOwner(), p.getUniqueId(), "TEAMACTION", "PREMIUMSHOP REMOVE", pi.getItem().getAmount() + "x" + pi.getItem().getType());
    }

    private final Map<String, String> cfgcache = new HashMap<>();

    private void updateShop() {
        final boolean firstload = itemids.isEmpty();
        if (firstload && Bukkit.isPrimaryThread() == false) return;
        if (firstload) {
            for (int x = this.shoploc.getBlockX() - 85; x < this.shoploc.getBlockX() + 85; x++) {
                for (int z = this.shoploc.getBlockZ() - 85; z < this.shoploc.getBlockZ() + 85; z++) {
                    final Location l = new Location(this.shoploc.getWorld(), x, this.shoploc.getBlockY(), z);
                    l.getChunk().load(false);
                }
            }
        }
        for (final Entity en : this.shoploc.getWorld().getNearbyEntities(this.shoploc, 80, 80, 80)) {
            if (en instanceof ItemFrame) {
                final ItemFrame frame = (ItemFrame) en;
                final Location signloc = frame.getLocation().clone().subtract(0, 1, 0);
                final ItemStack item = frame.getItem();
                if (item == null || item.getType() == Material.AIR || (signloc.getBlock().getState() instanceof Sign) == false)
                    continue;
                final double cheapest = getCheapestPrice(item.getTypeId() + ":" + item.getDurability());
                if (firstload) {
                    final String dur = String.valueOf(item.getDurability()), id = item.getTypeId() + ":" + dur;
                    itemids.add(id);
                    Location l = frame.getLocation();
                    switch (frame.getFacing()) {
                        case EAST: {
                            l = l.add(+2, -1, 0);
                            l.setYaw(90);
                            break;
                        }
                        case WEST: {
                            l = l.add(-2, -1, 0);
                            l.setYaw(-90);
                            break;
                        }
                        case NORTH: {
                            l = l.add(0, -1, -2);
                            l.setYaw(0);
                            break;
                        }
                        case SOUTH: {
                            l = l.add(0, -1, +2);
                            l.setYaw(180);
                            break;
                        }
                        default:
                            break;
                    }
                    itemframelocs.put(id, l);
                    final Location l2 = frame.getLocation();
                    ShopCategory category;
                    if (l2.getZ() > 448 && l2.getX() < -107) {
                        category = ShopCategory.FOOD;
                    } else if (l2.getZ() > 448 && l2.getX() > -82) {
                        category = ShopCategory.MISCELLANEOUS;
                    } else if (l2.getZ() > 433 && l2.getX() < -120) {
                        category = ShopCategory.MATERIALS;
                    } else if (l2.getZ() < 431 && l2.getX() < -117) {
                        category = ShopCategory.COMBAT_AND_TOOLS;
                    } else if (l2.getZ() < 417 && l2.getX() < -107) {
                        category = ShopCategory.BREWING;
                    } else if (l2.getZ() > 390 && l2.getX() < -84) {
                        category = ShopCategory.BLOCKS;
                    } else if (l2.getZ() > 390 && l2.getX() < -72) {
                        category = ShopCategory.DECORATION;
                    } else if (l2.getZ() < 431 && l2.getX() < -40) {
                        category = ShopCategory.COLORFUL;
                    } else if (l2.getZ() < 443 && l2.getX() < -40) {
                        category = ShopCategory.REDSTONE;
                    } else {
                        category = ShopCategory.MISCELLANEOUS;
                    }
                    categories.putIfAbsent(category, new ArrayList<>());
                    categories.get(category).add(id);
                }
                String price = "§4nicht verfügbar";
                if (cheapest != -1)
                    price = (cheapest < 10000 ? ms.getNumberFormat(cheapest) : ms.getSFormat(cheapest)) + " MuxCoin" + (cheapest != 1 ? "s" : "");
                final BlockState blockState = signloc.getBlock().getState();
                final Sign sign = (Sign) blockState;
                sign.setLine(0, "§8-*-");
                final String name = getItemName(item);
                sign.setLine(1, name != null ? name : "");
                sign.setLine(2, "§2" + price);
                sign.setLine(3, "§8-*-");
                sign.update();
            }
        }
        if (firstload) {
            final long yesterday = System.currentTimeMillis() - 86400000L, sevendaysago = System.currentTimeMillis() - (86400000L * 7L), lasthour = System.currentTimeMillis() - 3600000L;
            new BukkitRunnable() {
                @Override public void run() {
                    for (final String id : itemids) {
                        lasthourprices.put(id, ms.getDB().getPriceAtTime(id, lasthour));
                        lastdayprices.put(id, ms.getDB().getPriceAtTime(id, yesterday));
                        sevendaysagoprices.put(id, ms.getDB().getPriceAtTime(id, sevendaysago));
                        lastknownprice.put(id, ms.getDB().getLastKnownPrice(id));
                    }
                }
            }.runTaskAsynchronously(ms);

            categories.values().forEach(list -> list.sort(Comparator.comparingInt(value -> Integer.parseInt(value.split(":")[0]))));

            final List<String> found = categories.get(ShopCategory.BREWING);
            final List<String> temp = found != null ? new ArrayList<>(found) : new ArrayList<>(), potions = new ArrayList<>();
            if (found != null) found.clear();
            temp.forEach(s -> {
                final int id = Integer.parseInt(s.split(":")[0]);
                final ItemStack i = new ItemStack(id);
                if (i.getType() != Material.POTION) {
                    categories.get(ShopCategory.BREWING).add(s);
                } else {
                    potions.add(s);
                }
            });
            potions.sort(Comparator.comparingInt(s -> Integer.parseInt(s.split(":")[1])));
            Collections.reverse(potions);
            if (found != null) categories.get(ShopCategory.BREWING).addAll(potions);
        }
    }
    public String getItemName(final String fullid) {
        final String path = fullid + ".name" + ms.getLanguage().getLocale();
        return cfgcache.computeIfAbsent(path, shopcfg::getString);
    }

    public String getItemName(final ItemStack item) {
        return getItemName(item.getTypeId() + ":" + item.getDurability());
    }

    public boolean onEntityInteract(final Player p, final Entity en) {
        final Location l = p.getLocation();
        if (l.getWorld().equals(shoploc.getWorld()) == false || ms.getShopRegion() == null || ms.getShopRegion().contains(l.getBlockX(), l.getBlockY(), l.getBlockZ()) == false) return false;
        final ItemFrame frame = (ItemFrame) en;
        final ItemStack content = frame.getItem();
        if (content == null || content.getType() == Material.AIR) return false;
        if (ms.checkGeneralCooldown(p.getName(), "SHOPBUY", 800L, true)) return true;
        boolean stack = p.isSneaking();
        final String fullid = content.getTypeId() + ":" + content.getDurability();
        final double cheapest = getCheapestPrice(fullid);
        if (cheapest == -1) {
            ms.showItemBar(p, "§cDas Item ist nicht verfügbar.");
            return true;
        }
        final List<ShopItem> items = ms.getDB().getShopItems(content.getTypeId() + ":" + content.getDurability(), cheapest);
        int amountforsale = 0;
        for (final ShopItem si : items) {
            amountforsale += si.getAmount();
        }
        if (items.isEmpty()) {
            ms.showItemBar(p, "§cDas Item ist nicht verfügbar.");
            return true;
        }
        int amount = stack ? 64 : 1;
        if (amountforsale <= amount) amount = amountforsale;
        final double price = amount * cheapest;
        final MuxUser u = ms.getMuxUser(p.getName());
        final long cd = cooldown.getOrDefault(fullid, 0L);
        if (cd > System.currentTimeMillis()) {
            p.playSound(l, Sound.NOTE_STICKS, 1F, 1F);
            ms.sendNormalTitle(p, "§fDer Preis liegt jetzt bei §e" + ms.getNumberFormat(cheapest) + " MuxCoins§f.", 5, 40, 5);
            return true;
        } else if (p.getInventory().firstEmpty() == -1) {
            ms.showItemBar(p, "§cDein Inventar ist voll.");
            return true;
        } else if (price > u.getCoins()) {
            ms.showItemBar(p, ms.getLang("market.notenough"));
            ms.chatClickHoverLink(p, ms.getLang("shop.clickformuxcoins"), ms.getLang("shop.hoverformuxcoins"), "https://shop.muxcraft.eu/?ign=" + p.getName());
            return true;
        }
        volume.put(fullid, (long) (volume.getOrDefault(fullid, 0L) + price));
        final ItemStack item = items.get(0).getItemStack();
        item.setAmount(amount);
        if (price < 1) {
            u.removeCents(price);
        } else {
            final int muxcoins = Math.toIntExact(Math.round(price));
            double rest = price - muxcoins;
            u.addCoins(-muxcoins);
            if (rest >= 0.00D) {
                u.removeCents(rest);
            }
        }
        ms.getHistory().addCoinHistory(p.getUniqueId(), "Shop", price >= 1 ? Integer.parseInt("-" + (int) price) : Double.parseDouble("-" + price));
        if (amountforsale - amount == 0) {
            cooldown.put(fullid, System.currentTimeMillis() + 3000L);
        }
        ms.sendScoreboard(p);
        p.playSound(l, Sound.ORB_PICKUP, 0.4F, 1F);
        final String name = getItemName(item);
        ms.showItemBar(p, "§fDu hast §a" + amount + "x " + name + " §ffür §e" + ms.getNumberFormat(price) + " MuxCoins §fsoeben §agekauft§f.");
        p.getInventory().addItem(item);
        removeItems(p.getName(), amount, items);
        return true;
    }

    public void openEarn(final Player p) {
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.EARN && it != MuxInventory.InvType.GEMSHOP) p.closeInventory();
        p.openInventory(earninv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.EARN);
    }

    public void openEarnOres(final Player p) {
        if (ms.getActiveInv(p.getName()) != MuxInventory.InvType.EARN) p.closeInventory();
        final Inventory earnoreinv = ms.getServer().createInventory(null, 27, "§0§lErze abbauen");
        earnoreinv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        earnoreinv.setItem(4, ms.renameItemStack(new ItemStack(Material.STONE_PICKAXE), "§f§lErze abbauen", "§7Baue in der freien Wildnis Erze", "§7für eine direkte Belohnung ab."));
        final int ironprice = (int) (ms.getActiveMuxCoinsSupply() * MuxMining.OreTypes.IRON.getPercentage() / 100), goldprice = (int) (ms.getActiveMuxCoinsSupply() * MuxMining.OreTypes.GOLD.getPercentage() / 100),
                diamondprice = (int) (ms.getActiveMuxCoinsSupply() * MuxMining.OreTypes.DIAMOND.getPercentage() / 100), emeraldprice = (int) (ms.getActiveMuxCoinsSupply() * MuxMining.OreTypes.EMERALD.getPercentage() / 100);
        earnoreinv.setItem(19, ms.renameItemStack(new ItemStack(Material.IRON_ORE), "§f§lEisenerz", "§7Belohnung: §e" + ms.getNumberFormat(ironprice) + " MuxCoin" + (ironprice > 1 ? "s" : "")));
        earnoreinv.setItem(21, ms.renameItemStack(new ItemStack(Material.GOLD_ORE), "§e§lGolderz", "§7Belohnung: §e" + ms.getNumberFormat(goldprice) + " MuxCoin" + (goldprice > 1 ? "s" : "")));
        earnoreinv.setItem(23, ms.renameItemStack(new ItemStack(Material.DIAMOND_ORE), "§b§lDiamanterz", "§7Belohnung: §e" + ms.getNumberFormat(diamondprice) + " MuxCoin" + (diamondprice > 1 ? "s" : "")));
        earnoreinv.setItem(25, ms.renameItemStack(new ItemStack(Material.EMERALD_ORE), "§a§lSmaragderz", "§7Belohnung: §e" + ms.getNumberFormat(emeraldprice) + " MuxCoin" + (emeraldprice > 1 ? "s" : "")));
        p.openInventory(earnoreinv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.EARN);
    }

    public void handleEarnInventory(final Player p, final ItemStack i) {
        final Material m = i.getType();
        final ItemMeta im = i.getItemMeta();
        if (m == Material.ITEM_FRAME) {
            openEarn(p);
        } else if (im.hasLore() && im.getLore().removeIf(s -> s.toLowerCase().contains("klick")) == false) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        } else if (m == Material.STORAGE_MINECART) {
            p.closeInventory();
            p.performCommand("shop");
            return;
        } else if (m == Material.GOLD_SWORD) {
            p.closeInventory();
            p.performCommand("1vs1");
            return;
        } else if (m == Material.STONE_PICKAXE) {
            openEarnOres(p);
        } else if (m == Material.FIREWORK) {
            p.closeInventory();
            p.performCommand("event");
        } else if (m == Material.EMERALD) {
            ms.getGemShop().openInventory(p, MuxGemShop.GemShopCategory.EXCHANGE);
        } else if (m == Material.EXP_BOTTLE) {
            p.closeInventory();
            p.performCommand("vote");
        } else if (m == Material.INK_SACK) {
            p.closeInventory();
            p.performCommand("casino");
            return;
        } else {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        }
        p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
    }

    private void setup() {
        earninv = ms.getServer().createInventory(null, 27, "§0§lWie bekomme ich MuxCoins?");
        earninv.setItem(10, ms.renameItemStack(new ItemStack(Material.EXP_BOTTLE), "§b§lVoten", "§7Du erhältst jeden Tag kostenlose", "§7MuxCoins, sobald du gevotet hast.", "", "§bKlicke§7, um für MuxCraft zu voten."));
        earninv.setItem(11, ms.renameItemStack(new ItemStack(Material.STONE_PICKAXE), "§f§lErze abbauen", "§7Baue in der freien Wildnis Erze", "§7für eine direkte Belohnung ab.", "", "§fKlicke§7, um die Erze zu sehen."));
        earninv.setItem(12, ms.renameItemStack(new ItemStack(Material.EMERALD), "§a§lGems umtauschen", "§7Im Gemshop kannst du deine Gems", "§7für MuxCoins sofort umtauschen.", "", "§aKlicke§7, um den Gemshop zu öffnen."));
        earninv.setItem(13, ms.renameItemStack(new ItemStack(Material.STORAGE_MINECART), "§e§lItems verkaufen", "§7Stelle verschiedenste Items im Shop", "§7für andere Spieler zum Verkauf.", "", "§eKlicke§7, um zum Shop zu gelangen."));
        earninv.setItem(14, ms.renameItemStack(new ItemStack(Material.GOLD_SWORD), "§c§lIm 1vs1 siegen", "§7Nur der Gewinner des Duells mit", "§7Kits bekommt einen Siegespreis.", "", "§cKlicke§7, um im 1vs1 zu kämpfen."));
        earninv.setItem(15, ms.renameItemStack(new ItemStack(Material.FIREWORK), "§d§lBei Events mitmachen", "§7Es finden regelmäßige Events statt,", "§7die sehr große Gewinne auszahlen.", "", "§dKlicke§7, um am Event teilzunehmen."));
        earninv.setItem(16, ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (short) 5), "§5§lIm Casino spielen", "§7Mit etwas Glück kannst du aus einem", "§7kleinen Einsatz viel Profit ziehen.", "", "§5Klicke§7, um den Casino zu betreten."));

        new BukkitRunnable() {
            @Override
            public void run() {
                updateShop();
            }
        }.runTaskLater(ms, 80L);
        replacepotions.put((short) 24588, (short) 16396);
        replacepotions.put((short) 24581, (short) 16389);
        replacepotions.put((short) 24620, (short) 16428);
        replacepotions.put((short) 24613, (short) 16421);
        replacepotions.put((short) 8227, (short) 8195);
        replacepotions.put((short) 16419, (short) 16387);
        replacepotions.put((short) 8261, (short) 8197);
        replacepotions.put((short) 16453, (short) 16389);
        replacepotions.put((short) 8230, (short) 8198);
        replacepotions.put((short) 16422, (short) 16390);
        replacepotions.put((short) 8232, (short) 8200);
        replacepotions.put((short) 16424, (short) 16392);
        replacepotions.put((short) 8234, (short) 8202);
        replacepotions.put((short) 16426, (short) 16394);
        replacepotions.put((short) 8268, (short) 8204);
        replacepotions.put((short) 16460, (short) 16396);
        replacepotions.put((short) 8237, (short) 8205);
        replacepotions.put((short) 16429, (short) 16397);
        replacepotions.put((short) 8238, (short) 8206);
        replacepotions.put((short) 16430, (short) 16398);
        replacepotions.put((short) 16341, (short) 8197);
        replacepotions.put((short) 16307, (short) 8195);
        replacepotions.put((short) 24616, (short) 16392);
        replacepotions.put((short) 32766, (short) 16462);
    }

    private void removeItems(final String buyer, final long amount, final List<ShopItem> items) {
        ShopItem item = items.get(0);
        String fullid = item != null ? item.getFullId() : null;
        final Map<UUID, MuxUser> save = new HashMap<>();
        final Map<UUID, ShopItem> sold = new HashMap<>();

        for (int i = 0; i < amount; i++) {
            if (item == null) {
                item = items.get(0);
                fullid = item != null ? item.getFullId() : null;
            }
            if (item == null) continue;
            item.removeOne();
            MuxUser u = save.get(item.getUUID());
            if (u == null) {
                final OfflinePlayer off = ms.getServer().getOfflinePlayer(item.getUUID());
                u = off.isOnline() ? ms.getMuxUser(off.getName()) : ms.getDB().loadPlayer(off.getUniqueId());
                if (u != null) {
                    save.put(u.getUUID(), u);
                    if (item.getPrice() > 90000) {
                        logLargeTransaction(buyer + " » " + u.getName() + " (" + ms.getNumberFormat(item.getPrice()) + ")");
                    }
                }
            }
            final ShopItem si = sold.getOrDefault(item.getUUID(), new ShopItem(-1, null, fullid, 0, 0));
            si.setPrice(si.getPrice() + item.getPrice());
            si.setAmount(si.getAmount() + 1);
            sold.put(item.getUUID(), si);
            if (u != null) u.addProfits(item.getPrice());
            if (item.getAmount() == 0) {
                removeItem(item.getUUID(), u, item);
                items.remove(item);
                item = null;
            }
        }
        if (item != null && item.getAmount() > 0) {
            updateAmount(item.getUUID(), item);
        }
        for (final MuxUser u : save.values()) {
            ms.saveMuxUser(u);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                updateShop();
                for (final Map.Entry<UUID, ShopItem> entry : sold.entrySet()) {
                    final UUID uuid = entry.getKey();
                    final ShopItem si = entry.getValue();
                    ms.getDB().soldItem(uuid, si);
                    if (earningscache.containsKey(uuid)) getTotalEarnings(uuid, true);
                    if (soldcache.containsKey(uuid)) getSoldItems(uuid, true);
                }
            }
        }.runTaskAsynchronously(ms);
    }

    public ShopCategory getCategoryByDisplayName(final String name) {
        return Arrays.stream(ShopCategory.values()).filter(category -> category.getDisplayName().equalsIgnoreCase(name)).findFirst().orElse(ShopCategory.MISCELLANEOUS);
    }

    enum ShopCategory {
        BLOCKS("§f§lBlöcke", Material.BRICK, (short) 0), DECORATION("§f§lDekoration", Material.DOUBLE_PLANT, (short) 5), REDSTONE("§f§lRedstone", Material.REDSTONE, (short) 0), MISCELLANEOUS("§f§lSonstiges", Material.LAVA_BUCKET, (short) 0), FOOD("§f§lEssen", Material.APPLE, (short) 0), COMBAT_AND_TOOLS("§f§lKampf & Werkzeug", Material.GOLD_SWORD, (short) 0), BREWING("§f§lTränke", Material.POTION, (short) 0), MATERIALS("§f§lWerkstoffe", Material.STICK, (short) 0), COLORFUL("§f§lFarbiges", Material.CARPET, (short) 1), MOSTSEARCHED("§f§lMeistgesucht", Material.COMPASS, (short) 0), TOPGAINERS("§a§lTop Gewinner", Material.STAINED_CLAY, (short) 5), TOPLOSERS("§c§lTop Verlierer", Material.STAINED_CLAY, (short) 14);

        final String display;
        final Material material;
        final short dur;

        ShopCategory(final String display, final Material material, final short dur) {
            this.display = display;
            this.material = material;
            this.dur = dur;
        }

        public String getDisplayName() {
            return display;
        }

        public Material getMaterial() {
            return material;
        }

        public short getDurability() {
            return dur;
        }
    }
}