package me.muxteam.shop;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import me.muxteam.basic.ConfirmInventory;
import me.muxteam.extras.MuxChests;
import me.muxteam.extras.MuxEmojis;
import me.muxteam.extras.MuxExtraUser;
import me.muxteam.extras.MuxMounts;
import me.muxteam.extras.MuxPets;
import me.muxteam.muxsystem.MuxInventory;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import me.muxteam.ranks.MuxRanks;
import net.minecraft.server.v1_8_R3.Tuple;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.SpawnEgg;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MuxGemShop {
    private final MuxSystem ms;
    private final GemItemComparator gemItemComparator = new GemItemComparator();
    private final Gson gson = new Gson();
    private final Map<GemShopCategory, List<GemItem>> items = new EnumMap<>(GemShopCategory.class);
    private final Map<GemShopCategory, List<List<GemItem>>> rowedItems = new EnumMap<>(GemShopCategory.class);
    private final Map<GemItem, Integer> amountBought = new HashMap<>();
    private final List<GemItem> gemItems = new ArrayList<>();
    private final List<ConditionalGemItem> conditionalItems = new ArrayList<>();
    private GemItem lowestItem;
    private long gemLiquidity = 500, coinsLiquidity = 1000000L;

    public MuxGemShop(final MuxSystem ms) {
        this.ms = ms;
        addItems();
        registerConditionalItems();
        final FileConfiguration hashYML = ms.getHashYAML();
        if (hashYML.contains("gemitemsbought")) {
            @SuppressWarnings("UnstableApiUsage")
            final HashMap<String, Integer> itemsbought = gson.fromJson(hashYML.getString("gemitemsbought"), new TypeToken<HashMap<String, Integer>>() {}.getType());
            this.items.values().forEach(items1 -> items1.forEach(gemItem -> {
                final Integer amt = itemsbought.get(gemItem.name);
                if (amt != null)
                    this.amountBought.put(gemItem, amt);
            }));
        }
        for (final GemShopCategory category : items.keySet()) {
            if (category == GemShopCategory.RANKS || category == GemShopCategory.UPGRADES) continue;
            initRows(category);
        }
        this.items.forEach((key, value) -> this.gemItems.addAll(value));
        new BukkitRunnable() {
            @Override
            public void run() {
                // noinspection OptionalGetWithoutIsPresent
                lowestItem = gemItems.stream().filter(gemItem -> (gemItem instanceof RankItem) == false && gemItem instanceof ExtraEnderChestItem == false).min(gemItemComparator).get();
            }
        }.runTaskTimer(ms, 0L, 200L);
        // LIQUIDITY
        final Tuple<Long, Long> liquidity = ms.getDB().loadLiquidity();
        if (liquidity != null) {
            setGemLiquidity(liquidity.a());
            setCoinsLiquidity(liquidity.b());
        }
    }

    public void close() {
        items.clear();
        rowedItems.clear();
        final HashMap<String, Integer> bought = new HashMap<>();
        this.amountBought.forEach((key, value) -> bought.put(key.name, value));
        ms.getHashYAML().set("gemitemsbought", gson.toJson(bought));
        ms.getDB().saveLiquidity();
    }

    public long getCoinsLiquidity() {
        return coinsLiquidity;
    }

    public long getGemLiquidity() {
        return gemLiquidity;
    }

    public double getGemExchangeRate(double gems) { // The amount of coins that you get for gems
        if (gemLiquidity <= gems) {
            int multiplier = (int) ((gems / gemLiquidity) * 2) + 1;
            gemLiquidity *= multiplier;
            coinsLiquidity *= multiplier;
        }
        return Math.floor(coinsLiquidity - ((gemLiquidity * coinsLiquidity) / (gemLiquidity + gems)));
    }

    public double getExactCoinExchangeRate(double wantedgems) { // The amount of coins that you need for X gems
        if (gemLiquidity <= wantedgems) {
            int multiplier = (int) ((wantedgems / gemLiquidity) * 2) + 1;
            gemLiquidity *= multiplier;
            coinsLiquidity *= multiplier;
        }
        return Math.ceil(((gemLiquidity * coinsLiquidity) / (gemLiquidity - wantedgems)) - coinsLiquidity);
    }

    public void setCoinsLiquidity(long coinsLiquidity) {
        this.coinsLiquidity = Math.max(coinsLiquidity, 1);
    }

    public void setGemLiquidity(long gemLiquidity) {
        this.gemLiquidity = gemLiquidity;
    }

    private void initRows(final GemShopCategory category) {
        final int maxPerRow = 7;
        int current = 0, index = 0;
        final List<List<GemItem>> rowed = new ArrayList<>();
        rowed.add(new ArrayList<>());
        final List<GemItem> list = this.items.get(category);
        for (final GemItem item : list) {
            current++;
            rowed.get(index).add(item);
            if (current == maxPerRow) {
                rowed.add(new ArrayList<>());
                index++;
                current = 0;
            }
        }
        this.rowedItems.put(category, rowed);
    }

    public boolean handleCommand(final Player p) {
        if (this.isEnabled() == false) {
            ms.showItemBar(p, "§cDer Gemshop ist momentan deaktiviert.");
            return true;
        }
        openInventory(p, null);
        return true;
    }

    public void openInventory(final Player p, final GemShopCategory category) {
        final Inventory inventory = ms.getServer().createInventory(null, 45, "§0§lMuxGems§0 | " + ms.getNumberFormat(ms.getMuxUser(p.getName()).getGems()) + " Gems");
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (category == null) {
            ms.getAnalytics().getAnalytics().getGemShop().add(p.getUniqueId());
            inventory.setItem(4, ms.renameItemStack(new ItemStack(Material.EMERALD),
                    "§a§l" + ms.getNumberFormat(ms.getMuxUser(p.getName()).getGems()) + (ms.isBeta() ? " TestGems" : " Gems"),
                    "§7Du erhältst diese entweder pro Vote", "§7oder über unseren Onlineshop.", "", "§7Klicke, um §aweitere Gems zu erwerben§7."));
            addHeader(inventory);
            int slot = 19, amount = 7;

            final List<GemItem> conditionals = this.conditionalItems.stream().filter(conditionalGemItem -> conditionalGemItem.getItem() != null && conditionalGemItem.shouldDisplay(p))
                    .map(ConditionalGemItem::getItem).collect(Collectors.toList());
            for (GemItem conditionalItem : conditionals) {
                if (conditionalItem == null) continue;
                inventory.setItem(slot, conditionalItem.getDisplayItem(p));
                slot++;
            }
            amount -= conditionals.size();
            if (lowestItem.isOwned(p) == false) {
                amount--;
                inventory.setItem(slot + amount, lowestItem.getDisplayItem(p));
            }

            for (final GemItem top7Item : gemItems.stream().filter(gemItem -> gemItem.isOwned(p) == false)
                    .filter(gemItem -> (gemItem instanceof RankItem) == false)
                    .filter(gemItem -> conditionals.contains(gemItem) == false)
                    .filter(gemItem -> gemItem != lowestItem)
                    .sorted(gemItemComparator.reversed())
                    .limit(amount).toArray(GemItem[]::new)) {
                if (top7Item == null) continue;
                inventory.setItem(slot, top7Item.getDisplayItem(p));
                slot++;
            }
        } else {
            inventory.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back") + (it == MuxInventory.InvType.EARN ? " (MuxCoins)" : "")));
            inventory.setItem(4, category.getItemStack(ms).clone());
            if (category == GemShopCategory.EXCHANGE) {
                int base = 19, mod = 2;
                final int userGems = ms.getMuxUser(p.getName()).getGems();
                for (int i = 0; i < 4; i++) {
                    int gems;
                    switch (i) {
                        case 1:
                            gems = 5;
                            break;
                        case 2:
                            gems = 10;
                            break;
                        case 3:
                            gems = 50;
                            break;
                        default:
                            gems = 1;
                    }
                    int gemstack = gems;
                    if (userGems >= 50000)
                        gems *= 1000;
                    else if (userGems >= 5000)
                        gems *= 100;
                    else if (userGems >= 500)
                        gems *= 10;
                    final long gemsToExchange = (long) getExactCoinExchangeRate(gems);
                    long coinsToExchange = (long) getGemExchangeRate(gems);

                    inventory.setItem(base, ms.renameItemStack(ms.addLore(new ItemStack(Material.DOUBLE_PLANT, gemstack),
                                    "§7Preis: §a" + ms.getNumberFormat(gems) + (gems == 1 ? " Gem" : " Gems"), "", "§7Klicke§7, um sie zu §akaufen§7."),
                            "§f§l" + ms.getNumberFormat(coinsToExchange) + " MuxCoins"));

                    inventory.setItem(base + 18, ms.renameItemStack(ms.addLore(new ItemStack(Material.EMERALD, gemstack),
                                    "§7Preis: §e" + ms.getNumberFormat(gemsToExchange) + (coinsToExchange == 1 ? " MuxCoin" : " MuxCoins"), "", "§7Klicke§7, um sie zu §akaufen§7."),
                            "§f§l" + ms.getNumberFormat(gems) + (gems == 1 ? " Gem" : " Gems")));
                    base += mod;
                }
            } else {
                final List<GemItem> gitems = items.get(category);
                if (gitems == null || gitems.isEmpty()) {
                    inventory.setItem(31, ms.renameItemStack(new ItemStack(Material.BARRIER), "§c§lEs gibt keine Items in dieser Kategorie."));
                } else {
                    int row = 0;
                    for (final List<GemItem> rows : this.rowedItems.get(category)) {
                        buildRow(p, inventory, rows, row);
                        row++;
                    }
                }
            }
        }
        if (it != MuxInventory.InvType.GEMSHOP && it != MuxInventory.InvType.EARN && it != MuxInventory.InvType.CONFIRM)
            p.closeInventory();
        p.openInventory(inventory);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.GEMSHOP);
        p.removeMetadata("gemshopcategory", ms);
        p.setMetadata("gemshopcategory", new FixedMetadataValue(ms, category));
    }

    public boolean isEnabled() {
        return ms.getAdmin().GEMSHOP.isActive();
    }

    public void handleInventory(final Player p, final ItemStack is, final int slot, final ClickType clickType, final Inventory inv) {
        final GemShopCategory currentCategory = slot < 18 ? GemShopCategory.ITEMS : (GemShopCategory) p.getMetadata("gemshopcategory").get(0).value();
        if (slot == 0 && is.getType() == Material.ITEM_FRAME) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            if (is.getItemMeta().getDisplayName().contains("MuxCoins")) {
                ms.getShop().openEarn(p);
                return;
            }
            openInventory(p, null);
        } else if (slot == 4) {
            if (is.getType() == Material.EMERALD) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                p.performCommand("buy");
                return;
            }
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
        } else if (slot >= 35 && inv.getItem(4).getType() == Material.EMERALD) {
            final Material clickedMaterial = is.getType();
            if (clickedMaterial == null) return;
            GemShopCategory clicked = null;
            for (final GemShopCategory value : GemShopCategory.values()) {
                if (value.getItemStack(ms).getType().equals(clickedMaterial)) {
                    clicked = value;
                    break;
                }
            }
            if (clicked == null || clicked == currentCategory) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            openInventory(p, clicked);
        } else {
            if (currentCategory == GemShopCategory.EXCHANGE) {
                final Material type = is.getType();
                if (type == Material.DOUBLE_PLANT) {
                    final MuxUser user = ms.getMuxUser(p.getName());
                    final long price = Long.parseLong(ChatColor.stripColor(is.getItemMeta().getLore().get(0)).replaceAll("[:.,a-zA-Z ]*", ""));
                    final long amount = (long) getGemExchangeRate(price);
                    if (user.getGems() < price) {
                        p.closeInventory();
                        ms.showItemBar(p, "§cDu hast nicht genug Gems für diesen Tausch.");
                        ms.chatClickHoverLink(p, "§2§lGemShop>§7 Klicke §ahier§7, um mehr §2Gems §7zu erwerben.", "§a§oKlicke für mehr Gems", "https://shop.muxcraft.eu/?ign=" + p.getName());
                        p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                        return;
                    }
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1, 1);
                    user.addGems((int) -price);
                    user.addCoins(Integer.min(Integer.MAX_VALUE, (int) amount));
                    ms.getShop().addVolume("GEM", amount);
                    setCoinsLiquidity(getCoinsLiquidity() - amount);
                    setGemLiquidity(getGemLiquidity() + price);
                    ms.saveMuxUser(user);
                    ms.getHistory().addHistory(p.getUniqueId(), null, "GEMS", "-" + price, "GemShop Exchange");
                    ms.getHistory().addHistory(p.getUniqueId(), null, "COINS", "+" + amount, "GemShop Exchange");
                    p.closeInventory();
                    ms.sendScoreboard(p);
                    ms.showItemBar(p, "§f");
                    p.removeMetadata("gemshopcategory", ms);
                    ms.showItemBar(p, "§fDu hast §e" + ms.getNumberFormat(amount) + " MuxCoins§f für §a" + ms.getNumberFormat(price) + " Gems §fsoeben §agekauft§f.");
                    for (final Player onlinePlayer : ms.getServer().getOnlinePlayers()) {
                        if (onlinePlayer != p && ms.getScoreboard().getCurrent(onlinePlayer) == ms.getShopSB())
                            ms.sendScoreboard(onlinePlayer);
                        if (onlinePlayer.hasMetadata("gemshopcategory") == false) continue;
                        final List<MetadataValue> values = onlinePlayer.getMetadata("gemshopcategory");
                        if (values.isEmpty() == false && values.get(0).value() == GemShopCategory.EXCHANGE && onlinePlayer.getOpenInventory() != null &&
                                onlinePlayer.getOpenInventory().getTopInventory() != null && onlinePlayer.getOpenInventory().getTopInventory().getTitle() != null &&
                                onlinePlayer.getOpenInventory().getTopInventory().getTitle().contains("MuxGem"))
                            openInventory(onlinePlayer, GemShopCategory.EXCHANGE);
                    }
                } else if (type == Material.EMERALD) {
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1, 1);
                    final MuxUser user = ms.getMuxUser(p.getName());
                    final int amount = Integer.parseInt(ChatColor.stripColor(is.getItemMeta().getDisplayName()).replaceAll("[:.,a-zA-Z ]*", ""));
                    final long price = (long) getExactCoinExchangeRate(amount);
                    if (user.getCoins() < price) {
                        p.closeInventory();
                        ms.showItemBar(p, "§cDu hast nicht genug MuxCoins für diesen Tausch.");
                        ms.chatClickHoverLink(p, "§2§lGemShop>§7 Klicke §ahier§7, um mehr §aGems §7zu erwerben.", "§a§oKlicke für mehr Gems", "https://shop.muxcraft.eu/?ign=" + p.getName());
                        p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                        return;
                    }
                    user.addCoins((int) -price);
                    ms.getShop().addVolume("GEM", price);
                    user.addGems(amount);
                    setCoinsLiquidity(getCoinsLiquidity() + price);
                    setGemLiquidity(getGemLiquidity() - amount);
                    ms.saveMuxUser(user);
                    ms.getHistory().addHistory(p.getUniqueId(), null, "COINS", "-" + price, "GemShop Exchange");
                    ms.getHistory().addHistory(p.getUniqueId(), null, "GEMS", "+" + amount, "GemShop Exchange");
                    ms.sendScoreboard(p);
                    ms.showItemBar(p, "§fDu hast §a" + ms.getNumberFormat(amount) + " Gems§f für §e" + ms.getNumberFormat(price) + " MuxCoins §fsoeben §agekauft§f.");
                    p.closeInventory();
                    p.removeMetadata("gemshopcategory", ms);
                    for (final Player onlinePlayer : ms.getServer().getOnlinePlayers()) {
                        if (onlinePlayer != p && ms.getScoreboard().getCurrent(onlinePlayer) == ms.getShopSB())
                            ms.sendScoreboard(onlinePlayer);
                        if (onlinePlayer.hasMetadata("gemshopcategory") == false) continue;
                        final List<MetadataValue> values = onlinePlayer.getMetadata("gemshopcategory");
                        if (values.isEmpty() == false && values.get(0).value() == GemShopCategory.EXCHANGE && onlinePlayer.getOpenInventory() != null &&
                                onlinePlayer.getOpenInventory().getTopInventory() != null && onlinePlayer.getOpenInventory().getTopInventory().getTitle() != null &&
                                onlinePlayer.getOpenInventory().getTopInventory().getTitle().contains("MuxGem"))
                            openInventory(onlinePlayer, GemShopCategory.EXCHANGE);
                    }
                }
                return;
            }
            final GemItem gemItem = getFromItemStack(currentCategory, is);
            if (gemItem == null) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                ms.showItemBar(p, "§cEs ist ein Fehler aufgetreten.");
                p.closeInventory();
                return;
            } else if (gemItem.isOwned(p)) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                ms.showItemBar(p, "§aDu besitzt das bereits.");
                return;
            } else if (ms.isBeta() && gemItem.getCategory().isBeta() == false) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                ms.showItemBar(p, "§cDu kannst dies während der Beta nicht kaufen.");
                return;
            }
            final int amount = gemItem.getAmount(clickType);
            final MuxUser user = ms.getMuxUser(p.getName());
            final int price = amount * gemItem.getPrice(p);
            final boolean fulltrustedbuy = user.getGems() < price && ms.isFullTrusted(p.getName());
            if (user.getGems() >= price || fulltrustedbuy) {
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                new ConfirmInventory(ms, pl -> {
                    if (user.getGems() < price && fulltrustedbuy == false) {
                        openInventory(pl, currentCategory);
                        return;
                    }
                    gemItem.receive(p, amount);
                    if (fulltrustedbuy == false) {
                        ms.getAnalytics().getAnalytics().addGemShopCategory(gemItem.getCategory().getCategoryName(), price);
                        user.setGems(user.getGems() - price);
                        ms.getAnalytics().addGemProfits(price);
                        ms.getAnalytics().getAnalytics().addSpentGems(price);
                        amountBought.put(gemItem, amountBought.getOrDefault(gemItem, 0) + 1);
                    }
                    ms.saveMuxUser(user);
                    ms.getHistory().addHistory(p.getUniqueId(), null, "GEMS", fulltrustedbuy ? "0" : "-" + price, ChatColor.stripColor(gemItem.name));
                    ms.sendScoreboard(p);
                    ms.showItemBar(p, "§fDu hast §a" + (amount > 1 ? amount + "x " : "") + gemItem.displayItem.getItemMeta().getDisplayName() + " §ffür §a" + ms.getNumberFormat(price) + " Gems §fsoeben §agekauft§f.");
                    if (currentCategory != GemShopCategory.ITEMS && currentCategory != GemShopCategory.CHESTS)
                        p.closeInventory();
                    else openInventory(p, currentCategory);
                }, pl -> openInventory(pl, currentCategory))
                        .show(p, "§0§l" + (amount > 1 ? amount + "x " : "") + ChatColor.stripColor(gemItem.name) + " kaufen", "§aBestätigen (" + ms.getNumberFormat(price) + " Gems)", "§cAbbrechen");
            } else {
                p.closeInventory();
                ms.showItemBar(p, "§cDu hast nicht genug Gems für dieses Item.");
                ms.chatClickHoverLink(p, "§2§lGemShop>§7 Klicke §ahier§7, um mehr §2Gems §7zu erwerben.", "§a§oKlicke für mehr Gems", "https://shop.muxcraft.eu/?ign=" + p.getName());
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
            }
        }
    }

    private GemItem getFromItemStack(final GemShopCategory category, final ItemStack itemStack) {
        final List<GemItem> gitems = items.get(category);
        return gitems != null ? gitems.stream()
                .filter(gemItem -> gemItem.displayItem.getItemMeta()
                        .getDisplayName().equalsIgnoreCase(itemStack.getItemMeta().getDisplayName()))
                .findFirst()
                .orElse(null) : gemItems.stream()
                .filter(gemItem -> gemItem != null && gemItem.displayItem.getItemMeta()
                        .getDisplayName().equalsIgnoreCase(itemStack.getItemMeta().getDisplayName()))
                .findFirst().orElse(null);
    }

    private void buildRow(final Player player, final Inventory inventory, final List<GemItem> items, final int row) {
        final int start = 22 + row * 9 - 3;
        int index = 0;
        for (int slot = start; slot < start + 7; slot++) {
            if (items.size() <= index) continue;
            inventory.setItem(slot, items.get(index).getDisplayItem(player));
            index++;
        }
    }


    private void addHeader(final Inventory inventory) {
        int slot = 37;
        for (final GemShopCategory category : GemShopCategory.values()) {
            if (category == GemShopCategory.RANKS || category == GemShopCategory.UPGRADES) continue;
            ItemStack itemStack = ms.addLore(category.getItemStack(ms).clone(), "", category.getItemStack(ms).getItemMeta().getDisplayName().substring(0, 2) + "Klicke§7, um die Kategorie zu öffnen.");
            inventory.setItem(slot, itemStack);
            slot++;
        }
    }

    private void addItems() {

        // RANKS
        addShopItem(new RankItem(GemShopCategory.RANKS, 4000, "§e§lGOLD Rang", ms.renameItemStack(new ItemStack(Material.GOLD_BLOCK), "§e§lGOLD Rang", "§7Dauer: §a30 Tage", "", "§e- §7Werkzeuge reparieren", "§e- §7Doppelte MuxCoins beim Minen", "§e- §7Base exportieren", "§e- §7GOLD Casino Bereich", "§e- §7Chat Freiheit (weniger Cooldowns)", "§e- §7Chatfarbe einstellen", "§e- §7Farbige MSGs", "§e- §7Keine Werbung im Chat", "§e- §7Goldener Baserand & Partikel", "§e- §7/pweather & /macro", "§e- §7Mount: Fliegende Schlange", ""), "Gold"));
        addShopItem(new RankItem(GemShopCategory.RANKS, 40000, "§8§lX Rang", ms.renameItemStack(new ItemStack(Material.SEA_LANTERN), "§f§lX Rang", "§7Dauer: §a30 Tage", "", "§eExklusiver Rang", "§f- §7Setze deinen eigenen Rang & Farbe fest", "§f- §7Automatischer Vote", "§f- §7Unendlich Fly", "§f- §7Rüstungen & Schwerter umbennenen", "§f- §7Unlimitierte Köpfe", "§f- §7GOLD inklusiv", ""), "X"));

        addShopItem(new ExtraEnderChestItem(GemShopCategory.UPGRADES, 4000, "§5§lDoppelte Enderkiste", ms.renameItemStack(new ItemStack(Material.ENDER_CHEST), "§5§lDoppelte Enderkiste", "§7Ein permanentes Upgrade welches", "§7deine Enderkiste für immer verdoppelt.", "")));

        // ITEMS
        final ItemStack fP = new ItemStack(Material.DIAMOND_PICKAXE), fA = new ItemStack(Material.DIAMOND_AXE), fK = new ItemStack(Material.POTATO_ITEM),
                fMP = new ItemStack(Material.DIAMOND_PICKAXE), fMA = new ItemStack(Material.DIAMOND_AXE), fMS = new ItemStack(Material.DIAMOND_SPADE),
                fS = new ItemStack(Material.DIAMOND_SWORD), fArmor = new ItemStack(Material.DIAMOND_CHESTPLATE);
        fP.addUnsafeEnchantment(Enchantment.DIG_SPEED, 6);
        fP.addUnsafeEnchantment(Enchantment.DURABILITY, 4);
        fP.addEnchantment(Enchantment.LOOT_BONUS_BLOCKS, 3);
        fA.addEnchantment(Enchantment.DIG_SPEED, 5);
        fA.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 5);
        fA.addUnsafeEnchantment(Enchantment.KNOCKBACK, 2);
        fA.addEnchantment(Enchantment.DURABILITY, 3);
        fK.addUnsafeEnchantment(Enchantment.KNOCKBACK, 10);
        fMP.addEnchantment(Enchantment.DIG_SPEED, 5);
        fMA.addEnchantment(Enchantment.DIG_SPEED, 5);
        fMS.addEnchantment(Enchantment.DIG_SPEED, 5);
        fS.addEnchantment(Enchantment.DAMAGE_ALL, 5);
        fS.addEnchantment(Enchantment.DAMAGE_UNDEAD, 5);
        fS.addEnchantment(Enchantment.DAMAGE_ARTHROPODS, 5);
        fS.addEnchantment(Enchantment.FIRE_ASPECT, 2);
        fS.addEnchantment(Enchantment.LOOT_BONUS_MOBS, 3);
        fS.addEnchantment(Enchantment.DURABILITY, 3);
        fArmor.addEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        fArmor.addEnchantment(Enchantment.PROTECTION_FIRE, 4);
        fArmor.addEnchantment(Enchantment.PROTECTION_EXPLOSIONS, 4);
        fArmor.addEnchantment(Enchantment.PROTECTION_PROJECTILE, 4);
        fArmor.addEnchantment(Enchantment.DURABILITY, 3);
        addShopItem(new GemItem(GemShopCategory.ITEMS, 5, "Bedrock", ms.renameItemStack(new ItemStack(Material.BEDROCK), "§f§lBedrock")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                if (receiver.getInventory().firstEmpty() == -1) {
                    receiver.getWorld().dropItemNaturally(receiver.getLocation(), new ItemStack(Material.BEDROCK, amount));
                    receiver.sendMessage("§aDein Inventar war voll und der Bedrock wurde auf den Boden gedroppt.");
                } else {
                    receiver.getInventory().addItem(new ItemStack(Material.BEDROCK, amount));
                }
            }
        });
        addShopItem(new GemItem(GemShopCategory.ITEMS, 500, "Barriere", ms.renameItemStack(new ItemStack(Material.BARRIER), "§f§lBarriere")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                if (receiver.getInventory().firstEmpty() == -1) {
                    receiver.getWorld().dropItemNaturally(receiver.getLocation(), new ItemStack(Material.BARRIER, amount));
                    receiver.sendMessage("§aDein Inventar war voll und die Barriere wurde auf den Boden gedroppt.");
                } else {
                    receiver.getInventory().addItem(new ItemStack(Material.BARRIER, amount));
                }
            }
        });

        addShopItem(new GemItem(GemShopCategory.ITEMS, 200, "Matrix Block", ms.renameItemStack(new ItemStack(Material.MONSTER_EGGS, 1, (short) 6), "§d§lMatrix Block")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                if (receiver.getInventory().firstEmpty() == -1) {
                    receiver.getWorld().dropItemNaturally(receiver.getLocation(), new ItemStack(Material.MONSTER_EGGS, amount, (short) 6));
                    receiver.sendMessage("§aDein Inventar war voll und der Matrix Block wurde auf den Boden gedroppt.");
                } else {
                    receiver.getInventory().addItem(new ItemStack(Material.MONSTER_EGGS, amount, (short) 6));
                }
            }
        });

        addShopItem(new GemItem(GemShopCategory.ITEMS, 200, "Fly Booster", ms.renameItemStack(ms.getFireworkCharge(Color.RED), "§c§lFly Booster", "§7Dauer: §a30 Minuten", "")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                ms.getExtras().addItem(receiver, "flybooster", amount);
            }
        });

        addShopItem(new GemItem(GemShopCategory.ITEMS, 100, "XP Booster", ms.renameItemStack(ms.getFireworkCharge(Color.LIME), "§a§lEXP Booster", "§7Dauer: §a1 Stunde", "")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                ms.getExtras().addItem(receiver, "xpbooster", amount);
            }
        });

        addShopItem(new GemItem(GemShopCategory.ITEMS, 25, "Spawner Booster", ms.renameItemStack(ms.getFireworkCharge(Color.YELLOW), "§a§lSpawner Booster", "§7Dauer: §a2 Tage", "")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                ms.getExtras().addItem(receiver, "spawnerbooster", amount);
            }
        });

        addShopItem(new GemItem(GemShopCategory.ITEMS, 150, "Mega Spawner Booster", ms.renameItemStack(ms.getFireworkCharge(Color.ORANGE), "§a§lMega Spawner Booster", "§7Dauer: §a15 Tage", "")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                ms.getExtras().addItem(receiver, "megaspawnerbooster", amount);
            }
        });

        addShopItem(new GemItem(GemShopCategory.ITEMS, 300, "§a§lMuxSpawner", ms.renameItemStack(new ItemStack(Material.MOB_SPAWNER), "§a§lMuxSpawner")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                ms.getExtras().addItem(receiver, "spawner", amount);
            }
        });

        addShopItem(new GemItem(GemShopCategory.ITEMS, 100, "Netherwasser", ms.renameItemStack(new ItemStack(Material.WATER_BUCKET), "§9§lNether Wasser")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                ms.getExtras().addItem(receiver, "netherwater", amount);
            }
        });

        addShopItem(new GemItem(GemShopCategory.ITEMS, 300, "Rückstoß 10 Kartoffel", ms.renameItemStack(fK.clone(), "§6§lRückstoß 10 Kartoffel", "")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                ms.getExtras().addItem(receiver, "potato", amount);
            }
        });
        addShopItem(new GemItem(GemShopCategory.ITEMS, 1000, "Multiblock Schaufel", ms.renameItemStack(fMS.clone(), "§b§lMultiblock Schaufel", "")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                ms.getExtras().addItem(receiver, "multishovel", amount);
            }
        });
        addShopItem(new GemItem(GemShopCategory.ITEMS, 1000, "Multiblock Axt", ms.renameItemStack(fMA.clone(), "§b§lMultiblock Axt", "")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                ms.getExtras().addItem(receiver, "multiaxe", amount);
            }
        });

        addShopItem(new GemItem(GemShopCategory.ITEMS, 1000, "Multiblock Spitzhacke", ms.renameItemStack(fMP.clone(), "§b§lMultiblock Spitzhacke", "")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                ms.getExtras().addItem(receiver, "multipickaxe", amount);
            }
        });

        addShopItem(new GemItem(GemShopCategory.ITEMS, 400, "Super Spitzhacke", ms.renameItemStack(fP.clone(), "§b§lSuper Spitzhacke", "")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                ms.getExtras().addItem(receiver, "superpickaxe", amount);
            }
        });

        addShopItem(new GemItem(GemShopCategory.ITEMS, 400, "Full Rüstung", ms.renameItemStack(fArmor.clone(), "§b§lFull Rüstung", "")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                ms.getExtras().addItem(receiver, "armor", amount);
            }
        });
        addShopItem(new GemItem(GemShopCategory.ITEMS, 250, "Full Schwert", ms.renameItemStack(fS.clone(), "§b§lFull Schwert", "")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                ms.getExtras().addItem(receiver, "sword", amount);
            }
        });

        addShopItem(new GemItem(GemShopCategory.ITEMS, 250, "Blutbendiger Stock", ms.addGlow(ms.renameItemStack(new ItemStack(Material.STICK), "§b§lBlutbendiger Stock"))) {
            @Override
            public void receive(final Player receiver, final int amount) {
                ms.getExtras().addItem(receiver, "bloodstick", amount);
            }
        });
        addShopItem(new GemItem(GemShopCategory.ITEMS, 5, "64x EXP Flaschen", ms.renameItemStack(new ItemStack(Material.EXP_BOTTLE, 64), "§a§l64x EXP Flaschen")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                if (receiver.getInventory().firstEmpty() == -1) {
                    receiver.getWorld().dropItemNaturally(receiver.getLocation(), new ItemStack(Material.EXP_BOTTLE, 64));
                    receiver.sendMessage("§aDein Inventar war voll und die EXP Flaschen wurde auf den Boden gedroppt.");
                } else {
                    receiver.getInventory().addItem(new ItemStack(Material.EXP_BOTTLE, 64));
                }
            }

            @Override
            public int getAmount(final ClickType clickType) {
                return 1;
            }
        });

        addShopItem(new GemItem(GemShopCategory.ITEMS, 200, "Eigener Kopf", ms.renameItemStack(new ItemStack(Material.SKULL_ITEM, 1, (byte) 3), "§a§lEigener Kopf")) {
            @Override
            public void receive(final Player receiver, final int amount) {
                final ItemStack skull = ms.renameItemStack(ms.getHead(receiver.getName()), "§aKopf von " + receiver.getName());
                if (receiver.getInventory().firstEmpty() == -1) {
                    receiver.getWorld().dropItemNaturally(receiver.getLocation(), skull);
                    receiver.sendMessage("§aDein Inventar war voll und der Kopf wurde auf den Boden gedroppt.");
                } else {
                    receiver.getInventory().addItem(skull);
                }
            }
        });

        // PETS
        addShopItem(new PetItem(GemShopCategory.PETS, 1500, "Schwein",
                ms.renameItemStack(new SpawnEgg(EntityType.PIG).toItemStack(1), "§6§lSchwein"), EntityType.PIG));
        addShopItem(new PetItem(GemShopCategory.PETS, 1500, "Schaf",
                ms.renameItemStack(new SpawnEgg(EntityType.SHEEP).toItemStack(1), "§6§lSchaf"), EntityType.SHEEP));
        addShopItem(new PetItem(GemShopCategory.PETS, 1000, "Kuh",
                ms.renameItemStack(new SpawnEgg(EntityType.COW).toItemStack(1), "§6§lKuh"), EntityType.COW));
        addShopItem(new PetItem(GemShopCategory.PETS, 2500, "Huhn",
                ms.renameItemStack(new SpawnEgg(EntityType.CHICKEN).toItemStack(1), "§6§lHuhn"), EntityType.CHICKEN));
        addShopItem(new PetItem(GemShopCategory.PETS, 2500, "Hund",
                ms.renameItemStack(new SpawnEgg(EntityType.WOLF).toItemStack(1), "§6§lHund"), EntityType.WOLF));
        addShopItem(new PetItem(GemShopCategory.PETS, 2000, "Pilzkuh",
                ms.renameItemStack(new SpawnEgg(EntityType.MUSHROOM_COW).toItemStack(1), "§6§lPilzkuh"), EntityType.MUSHROOM_COW));
        addShopItem(new PetItem(GemShopCategory.PETS, 2500, "Katze",
                ms.renameItemStack(new SpawnEgg(EntityType.OCELOT).toItemStack(1), "§6§lKatze"), EntityType.OCELOT));
        addShopItem(new PetItem(GemShopCategory.PETS, 1500, "Elf",
                ms.renameItemStack(new SpawnEgg(EntityType.VILLAGER).toItemStack(1), "§6§lElf"), EntityType.VILLAGER));
        addShopItem(new PetItem(GemShopCategory.PETS, 1500, "Hase",
                ms.renameItemStack(new SpawnEgg(EntityType.RABBIT).toItemStack(1), "§6§lHase"), EntityType.RABBIT));
        addShopItem(new PetItem(GemShopCategory.PETS, 2000, "Pigman",
                ms.renameItemStack(new SpawnEgg(EntityType.PIG_ZOMBIE).toItemStack(1), "§6§lPigman"), EntityType.PIG_ZOMBIE));
        addShopItem(new PetItem(GemShopCategory.PETS, 2500, "Mini Me",
                ms.renameItemStack(new SpawnEgg(EntityType.ZOMBIE).toItemStack(1), "§6§lMini Me"), EntityType.ZOMBIE));
        addShopItem(new PetItem(GemShopCategory.PETS, 2000, "Fledermaus",
                ms.renameItemStack(new SpawnEgg(EntityType.BAT).toItemStack(1), "§6§lFledermaus"), EntityType.BAT));
        addShopItem(new PetItem(GemShopCategory.PETS, 1500, "Kuchen",
                ms.renameItemStack(new SpawnEgg(EntityType.CHICKEN).toItemStack(1), "§6§lKuchen"), EntityType.SILVERFISH));
        addShopItem(new PetItem(GemShopCategory.PETS, 7500, "Wither",
                ms.renameItemStack(new SpawnEgg(EntityType.WITHER).toItemStack(1), "§6§lWither"), EntityType.WITHER));

        // CHESTS
        addChestItem(500, MuxChests.ChestType.TOP10);
        addChestItem(200, MuxChests.ChestType.TOP20);
        addChestItem(100, MuxChests.ChestType.TOP30);

        // MOUNTS
        addShopItem(new MountItem(GemShopCategory.MOUNTS, 1500, "§6§lPferd des Schreckens", ms.renameItemStack(new ItemStack(Material.BONE), "§6§lPferd des Schreckens"), (short) 1));
        addShopItem(new MountItem(GemShopCategory.MOUNTS, 1000, "§6§lEisgletscher Pferd", ms.renameItemStack(new ItemStack(Material.SNOW_BALL), "§6§lEisgletscher Pferd"), (short) 2));
        addShopItem(new MountItem(GemShopCategory.MOUNTS, 500, "§6§lPackesel", ms.renameItemStack(new ItemStack(Material.HAY_BLOCK), "§6§lPackesel"), (short) 3));
        addShopItem(new MountItem(GemShopCategory.MOUNTS, 2000, "§6§lGiftgrüner Schleim", ms.renameItemStack(new ItemStack(Material.SLIME_BALL), "§6§lGiftgrüner Schleim"), (short) 4));
        addShopItem(new MountItem(GemShopCategory.MOUNTS, 1500, "§6§lMinecart", ms.renameItemStack(new ItemStack(Material.MINECART), "§6§lMinecart"), (short) 5));
        addShopItem(new MountItem(GemShopCategory.MOUNTS, 2200, "§6§lKletternde Spinne", ms.renameItemStack(new ItemStack(Material.WEB), "§6§lKletternde Spinne"), (short) 6));
        addShopItem(new MountItem(GemShopCategory.MOUNTS, 1500, "§6§lUntotes Ritterpferd", ms.renameItemStack(new ItemStack(Material.ROTTEN_FLESH), "§6§lUntotes Ritterpferd"), (short) 7));
        addShopItem(new MountItem(GemShopCategory.MOUNTS, 2500, "§6§lFliegendes Rentier", ms.renameItemStack(new ItemStack(Material.GOLDEN_CARROT), "§6§lFliegendes Ritterpferd"), (short) 8));
        addShopItem(new MountItem(GemShopCategory.MOUNTS, 1500, "§6§Röpsendes Schwein", ms.renameItemStack(new ItemStack(Material.PORK), "§6§lRöpsendes Schwein"), (short) 10));
        addShopItem(new MountItem(GemShopCategory.MOUNTS, 2000, "§6§lFeuerschleim", ms.renameItemStack(new ItemStack(Material.BLAZE_POWDER), "§6§lFeuerschleim"), (short) 11));
        addShopItem(new MountItem(GemShopCategory.MOUNTS, 2000, "§6§lLiebendes Schaf", ms.renameItemStack(new ItemStack(Material.RED_ROSE), "§6§lLiebendes Schaf"), (short) 12));
        addShopItem(new MountItem(GemShopCategory.MOUNTS, 2200, "§6§lTitan", ms.renameItemStack(new ItemStack(Material.IRON_INGOT), "§6§lTitan"), (short) 13));
        addShopItem(new MountItem(GemShopCategory.MOUNTS, 2000, "§6§lChaotischer Guardian", ms.renameItemStack(new ItemStack(Material.PRISMARINE_SHARD), "§6§lChaotischer Guardian"), (short) 15));
        addShopItem(new MountItem(GemShopCategory.MOUNTS, 1500, "§6§lMilkende Pilzkuh", ms.renameItemStack(new ItemStack(Material.HUGE_MUSHROOM_2), "§6§lMilkende Pilzkuh"), (short) 16));

        // EXTRA COMMANDS
        addShopItem(new ExtraCommandItem(GemShopCategory.EXTRA_COMMANDS, 1000, "§6§l/anvil", ms.renameItemStack(new ItemStack(Material.ANVIL), "§6§l/anvil", "§7Amboss überall öffnen", ""), "anvil"));
        addShopItem(new ExtraCommandItem(GemShopCategory.EXTRA_COMMANDS, 1000, "§6§l/enchant", ms.renameItemStack(new ItemStack(Material.ENCHANTMENT_TABLE), "§6§l/enchant", "§7Zaubertisch überall öffnen", ""), "enchant"));
        addShopItem(new ExtraCommandItem(GemShopCategory.EXTRA_COMMANDS, 1500, "§6§l/cook", ms.renameItemStack(new ItemStack(Material.COOKED_BEEF), "§6§l/cook", "§7Essen sofort kochen.", "§7(Sofortiger Ofen)", ""), "cook"));
        addShopItem(new ExtraCommandItem(GemShopCategory.EXTRA_COMMANDS, 2500, "§6§l/bodysee", ms.renameItemStack(new ItemStack(Material.DIAMOND_CHESTPLATE), "§6§l/bodysee", "§7Rüstung betrachten", ""), "bodysee"));
        addShopItem(new ExtraCommandItem(GemShopCategory.EXTRA_COMMANDS, 2500, "§6§l/goldswitch", ms.renameItemStack(new ItemStack(Material.GOLD_INGOT), "§6§l/goldswitch", "§7Alle 9 Goldnuggets in", "§7Goldbarren umtauschen.", ""), "goldswitch"));
        addShopItem(new ExtraCommandItem(GemShopCategory.EXTRA_COMMANDS, 2000, "§6§l/smelt", ms.renameItemStack(new ItemStack(Material.COAL), "§6§l/smelt", "§7Items sofort schmelzen.", "§7(Sofortiger Ofen)", ""), "smelt"));
        addShopItem(new ExtraCommandItem(GemShopCategory.EXTRA_COMMANDS, 5000, "§6§l/fill", ms.renameItemStack(new ItemStack(Material.POTION), "§6§l/fill", "§7Glasflaschen sofort füllen.", ""), "fill"));
        addShopItem(new ExtraCommandItem(GemShopCategory.EXTRA_COMMANDS, 4000, "§6§l/bottle", ms.renameItemStack(new ItemStack(Material.GLASS_BOTTLE), "§6§l/bottle", "§7Alle Glasblöcke in", "§7Glasflaschen umtauschen.", ""), "bottle"));
        addShopItem(new ExtraCommandItem(GemShopCategory.EXTRA_COMMANDS, 2500, "§6§l/xray", ms.renameItemStack(new ItemStack(Material.REDSTONE_ORE), "§6§l/xray", "§7Alle Erze unter dir", "§7sofort sehen.", ""), "xray"));
        addShopItem(new ExtraCommandItem(GemShopCategory.EXTRA_COMMANDS, 2500, "§6§l/near", ms.renameItemStack(new ItemStack(Material.COMPASS), "§6§l/near", "§7Alle Spieler in deiner", "§7Nähe anzeigen.", ""), "near"));
        addShopItem(new ExtraCommandItem(GemShopCategory.EXTRA_COMMANDS, 2500, "§6§l/bottlexp", ms.renameItemStack(new ItemStack(Material.EXP_BOTTLE), "§6§l/bottlexp", "§7Konvertiere deine Level", "§7zu einer Flasche.", ""), "bottlexp"));

        // EMOJIS
        addEmoji(500, "Wütend");
        addEmoji(500, "Weinend");
        addEmoji(500, "Liebend");
        addEmoji(500, "Goofy");
        addEmoji(500, "DealWithIt");
        addEmoji(500, "RIP");
        addEmoji(500, "Grinsend");
        addEmoji(500, "Müde");
        addEmoji(500, "Frech");
        addEmoji(500, "Traurig");
        addEmoji(500, "Fröhlich");
        addEmoji(500, "Spicy");
        addEmoji(500, "Kuss");
        addEmoji(1000, "Pepe");
    }

    private void registerConditionalItems() {
        this.items.values().forEach(gemItems1 -> gemItems1.stream().filter(gemItem -> gemItem.category == GemShopCategory.RANKS)
                .filter(RankItem.class::isInstance)
                .map(gemItem -> (RankItem) gemItem)
                .forEach(rankItem -> {
                    Function<Player, Boolean> function = null;
                    if (rankItem.rankName.equalsIgnoreCase("Gold")) {
                        function = player -> player.hasPermission("muxsystem.epic") && ms.hasGold(player.getUniqueId()) == false;
                    } else if (rankItem.rankName.equalsIgnoreCase("X")) {
                        function = player -> player.hasPermission("muxsystem.epic") && ms.hasX(player.getUniqueId()) == false;
                    }
                    this.conditionalItems.add(new ConditionalGemItem(rankItem, function));
                }));
        this.items.values().forEach(gemItems1 -> gemItems1.stream().filter(gemItem -> gemItem.category == GemShopCategory.UPGRADES)
                .forEach(gemItem -> this.conditionalItems.add(new ConditionalGemItem(gemItem, player -> gemItem.isOwned(player) == false))));
    }

    /*
    private GemItem findGemItem(final String name) {
        return this.gemItems.stream().filter(gemItem -> gemItem.name.equals(name)).findFirst().orElse(null);
    }
     */

    private void addEmoji(final int price, final String type) {
        final MuxEmojis emojis = ms.getEmojis();
        ItemStack itemStack;
        String name;
        switch (type) {
            case "Wütend":
                name = "§6§lWütend";
                itemStack = ms.renameItemStack(emojis.RAGE1(""), "§6§lWütend");
                break;
            case "Weinend":
                name = "§6§lWeinend";
                itemStack = ms.renameItemStack(emojis.CRY1(""), "§6§lWeinend");
                break;
            case "Liebend":
                name = "§6§lLiebend";
                itemStack = ms.renameItemStack(emojis.LOVE1(""), "§6§lLiebend");
                break;
            case "Goofy":
                name = "§6§lGoofy";
                itemStack = ms.renameItemStack(emojis.GOOFY1(""), "§6§lGoofy");
                break;
            case "DealWithIt":
                name = "§6§lDealWithIt";
                itemStack = ms.renameItemStack(emojis.TOOCOOL1(""), "§6§lDealWithIt");
                break;
            case "RIP":
                name = "§6§lRIP";
                itemStack = ms.renameItemStack(emojis.RIP1(""), "§6§lRIP");
                break;
            case "Grinsend":
                name = "§6§lGrinsend";
                itemStack = ms.renameItemStack(emojis.GRIN4(""), "§6§lGrinsend");
                break;
            case "Müde":
                name = "§6§lMüde";
                itemStack = ms.renameItemStack(emojis.SLEEPY9(""), "§6§lMüde");
                break;
            case "Frech":
                name = "§6§lFrech";
                itemStack = ms.renameItemStack(emojis.CHEEKY8(""), "§6§lFrech");
                break;
            case "Traurig":
                name = "§6§lTraurig";
                itemStack = ms.renameItemStack(emojis.FROWN8(""), "§6§lTraurig");
                break;
            case "Fröhlich":
                name = "§6§lFröhlich";
                itemStack = ms.renameItemStack(emojis.SMILE3(""), "§6§lFröhlich");
                break;
            case "Spicy":
                name = "§6§lSpicy";
                itemStack = ms.renameItemStack(emojis.SPICY9(""), "§6§lSpicy");
                break;
            case "Kuss":
                name = "§6§lKuss";
                itemStack = ms.renameItemStack(emojis.KISS2(""), "§6§lKuss");
                break;
            case "Pepe":
                name = "§6§lPepe";
                itemStack = ms.renameItemStack(emojis.PEPE1(""), "§6§lPepe");
                break;
            default:
                return;
        }
        addShopItem(new EmojiItem(GemShopCategory.EMOJIS, price, name, itemStack, emojis.getEmojiFromName(ChatColor.stripColor(name))));
    }

    private void addChestItem(final int price, final MuxChests.ChestType type) {
        addShopItem(new ChestItem(GemShopCategory.CHESTS, price, type.getName(),
                ms.renameItemStack(ms.getHeadFromURL(type.getTexture(), type.getName()),
                        type.getName()), type));
    }

    private void addShopItem(final GemItem item) {
        items.putIfAbsent(item.getCategory(), new ArrayList<>());
        items.get(item.getCategory()).add(item);
    }

    public enum GemShopCategory {
        ITEMS("Items", Material.LEATHER, "§b§lItems",
                Arrays.asList("§7Hier findest du spezielle Items, die", "§7du nirgendwo sonst finden kannst,", "§7wie z.B. Booster oder Spawner.")),

        CHESTS("MuxKisten", Material.CHEST, "§b§lKisten",
                Arrays.asList("§7Aus ihnen erhältst du wertvolle Items,", "§7die du im Shop verkaufen oder selber", "§7behalten kannst.")),

        EXTRA_COMMANDS("Befehle", Material.COMMAND, "§b§lBefehle",
                Arrays.asList("§7Die zusätzlichen Befehle helfen", "§7dir sehr und bieten Funktionen", "§7an, die sonst keiner hat."), false),

        MOUNTS("MuxMounts", Material.IRON_BARDING, "§b§lMounts",
                Arrays.asList("§7Strenge deine Beine nicht mehr an,", "§7indem du von einem Reittier", "§7schnell transportiert wirst."), false),

        PETS("MuxPets", Material.BONE, "§b§lPets",
                Arrays.asList("§7Dein persönlicher Begleiter folgt", "§7dir, wo immer du auch hingehst,", "§7und gehorcht dir."), false),

        EMOJIS("MuxEmojis", Material.SKULL_ITEM, "§b§lEmojis",
                Arrays.asList("§7Drücke all deine Gefühle mit Emojis", "§7aus. Diese sind deshalb besonders", "§7weil sie die ganze Zeit animiert sind."), false),
        RANKS("MuxRänge", Material.BOOK, "§b§lRänge", Collections.singletonList("§7Kaufe Ränge"), false),
        UPGRADES("Upgrades", Material.BOOK, "§b§lUpgrades", Collections.singletonList("§7Kaufe Upgrades"), false),

        EXCHANGE("Gem Exchange", Material.ANVIL, "§2§lUmtauschen",
                Arrays.asList("§7Tausche deine MuxCoins gegen Gems", "§7und umgekehrt ein. Der Wechselkurs", "§7passt sich der Nachfrage an."));

        private final String categoryName;
        private ItemStack itemStack;
        private final boolean beta;

        private final Material m;
        private final String itemname;
        private final List<String> lore;

        GemShopCategory(final String name, final Material m, final String itemname, final List<String> lore) {
            this(name, m, itemname, lore, true);
        }

        GemShopCategory(final String name, final Material m, final String itemname, final List<String> lore, final boolean beta) {
            this.m = m;
            this.itemname = itemname;
            this.lore = lore;
            this.categoryName = name;
            this.beta = beta;
        }

        public ItemStack getItemStack(final MuxSystem ms) {
            if (itemStack != null) return itemStack;
            if (this == EMOJIS) {
                itemStack = ms.renameItemStack(ms.getEmojis().GRIN4("§b§lEmojis"), itemname, lore);
            } else {
                itemStack = ms.renameItemStack(new ItemStack(m), itemname, lore);
            }
            if (this == EXCHANGE) {
                itemStack = ms.addGlow(itemStack);
            }
            return itemStack;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public boolean isBeta() {
            return beta;
        }
    }

    class GemItemComparator implements Comparator<GemItem> {
        @Override
        public int compare(final GemItem o1, final GemItem o2) {
            final long x = (long) o1.price * amountBought.getOrDefault(o1, 0),
                    y = (long) o2.price * amountBought.getOrDefault(o2, 0);
            if (x < y) {
                return -1;
            } else if (x > y)
                return 1;
            else return Integer.compare(o1.price, o2.price);
        }
    }

    class ConditionalGemItem {
        private final GemItem item;
        private final Function<Player, Boolean> function;

        private ConditionalGemItem(GemItem item, Function<Player, Boolean> function) {
            this.item = item;
            this.function = function;
        }

        public boolean shouldDisplay(final Player player) {
            if (this.function == null)
                return item.isOwned(player) == false;
            return this.function.apply(player);
        }

        public GemItem getItem() {
            return item;
        }
    }

    abstract class GemItem {
        private final GemShopCategory category;
        private final int price;
        private final String name;
        protected final ItemStack displayItem;

        protected GemItem(final GemShopCategory category, final int price, final String name, final ItemStack displayItem) {
            this.category = category;
            this.price = price;
            this.name = name;
            this.displayItem = ms.addLore(displayItem, "§7Preis: §a" + ms.getNumberFormat(price) + " Gems");
        }

        public ItemStack getDisplayItem() {
            final ItemStack item = this.displayItem.clone();
            if (lowestItem == this) {
                final List<String> lore = new ArrayList<>();
                for (final String line : this.displayItem.getItemMeta().getLore()) {
                    if (line.contains("Preis"))
                        break;
                    lore.add(line);
                }
                lore.add("§7Preis: §c§m" + ms.getNumberFormat(price) + "§a " + ms.getNumberFormat(getPrice()) + " Gems §2(-25%)");
                final ItemMeta meta = item.getItemMeta();
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            return item;
        }

        public GemShopCategory getCategory() {
            return category;
        }

        public ItemStack getDisplayItem(final Player player) {
            return ms.addLore(getDisplayItem(), "", "§7Klicke§7, um dieses Item zu §akaufen§7.");

        }

        public int getPrice(final Player player) {
            return getPrice();
        }

        public int getPrice() {
            return lowestItem == this ? Math.max(1, (75 * price) / 100) : price;
        }

        public int getAmount(final ClickType clickType) {
            return clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT ? 32 : 1;
        }

        public abstract void receive(final Player receiver, final int amount);

        public boolean canBuy(final Player player) {
            final MuxUser user = ms.getMuxUser(player.getName());
            return user.getGems() >= price;
        }

        public boolean isOwned(final Player player) {
            return false;
        }
    }

    class PetItem extends GemItem {
        private final EntityType type;

        protected PetItem(final GemShopCategory category, final int price, final String name, final ItemStack displayItem, final EntityType type) {
            super(category, price, name, displayItem);
            this.type = type;
        }

        @Override
        public void receive(final Player receiver, final int amount) {
            if (isOwned(receiver)) return;
            final MuxExtraUser extraUser = ms.getExtras().getExtraUser(receiver.getUniqueId());
            final MuxPets.PetStore petStore = extraUser.getPets();
            petStore.getOwned().add(type);
            extraUser.setPets(petStore);
            ms.getExtras().saveExtraUser(extraUser);
        }

        @Override
        public ItemStack getDisplayItem(final Player player) {
            final boolean owned = isOwned(player);
            final ItemStack displayItem = owned ? ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (short) 10), super.getDisplayItem().getItemMeta().getDisplayName()) : super.getDisplayItem();
            return ms.addLore(displayItem, owned ? new String[]{"", "§aDu besitzt dieses Pet bereits." }
                    : new String[]{"", "§7Klicke, um dieses Pet zu §akaufen§7." });
        }

        @Override
        public int getAmount(final ClickType clickType) {
            return 1;
        }

        @Override
        public boolean isOwned(final Player player) {
            return ms.getExtras().getExtraUser(player.getUniqueId()).getPets().getOwned().contains(this.type);
        }
    }

    class RankItem extends GemItem {
        private final String rankName;

        protected RankItem(final GemShopCategory category, final int price, final String name, final ItemStack displayItem, final String rankName) {
            super(category, price, name, displayItem);
            this.rankName = rankName;
        }

        @Override
        public void receive(final Player receiver, final int amount) {
            if (rankName.equals("Gold") || rankName.equals("X")) {
                final MuxRanks.PermissionsUser user = ms.getPerms().getUserData(receiver.getUniqueId());
                if (rankName.equalsIgnoreCase("X"))
                    user.setGroup(user.getExpireData() != null ? user.getExpireData().getOldRank() : user.getGroup()); // sonst wird auf Ultra danach wieder gesetzt
                user.setExpireData(new MuxRanks.RankExpireData(System.currentTimeMillis() + 2592000000L, user.getGroup() == null ? "Default" : user.getGroup()));
            }
            ms.getPerms().changePlayerGroup(receiver.getUniqueId(), receiver.getName(), rankName, ms.getServer().getConsoleSender());
            final Firework firework = receiver.getWorld().spawn(receiver.getLocation(), Firework.class);
            final FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .withColor(rankName.equals("Gold") ? Color.YELLOW : Color.WHITE)
                    .with(FireworkEffect.Type.BALL_LARGE)
                    .trail(true)
                    .build());
            meta.setPower(0);
            firework.setFireworkMeta(meta);
            receiver.playSound(receiver.getLocation(), Sound.LEVEL_UP, 1F, 1F);
        }

        @Override
        public boolean canBuy(final Player player) {
            final MuxRanks.PermissionsUser user = ms.getPerms().getUserData(player.getUniqueId());
            final MuxRanks.PermissionsGroup group = user == null ? null : ms.getPerms().getGroup(user.getGroup());
            boolean canBuy = group == null || group.getWeight() >= ms.getPerms().getGroup(rankName).getWeight();
            if (canBuy == false) {
                ms.showItemBar(player, "§cDu besitzt diesen oder einen höheren Rang bereits.");
            }
            return canBuy;
        }


        @Override
        public ItemStack getDisplayItem(final Player player) {
            final ItemStack displayItem = super.getDisplayItem();
            if (this.rankName.equalsIgnoreCase("X")) {
                final List<String> lore = new ArrayList<>();
                for (final String line : this.displayItem.getItemMeta().getLore()) {
                    if (line.contains("Preis")) {
                        break;
                    }
                    lore.add(line);
                }
                final int price = this.getPrice(player);
                final double percentage = (100 - ((100D / super.getPrice()) * price));
                    lore.add(percentage > 0 ? "§7Preis: §c§m" + MuxGemShop.this.ms.getNumberFormat(super.getPrice()) + "§a " + MuxGemShop.this.ms.getNumberFormat(price) + " Gems §2(-" + BigDecimal.valueOf(percentage).setScale(2, RoundingMode.HALF_UP).doubleValue() + "%)" :
                            "§7Preis: §a" + MuxGemShop.this.ms.getNumberFormat(price) + " Gems");
                final ItemMeta meta = displayItem.getItemMeta();
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            return MuxGemShop.this.ms.addLore(displayItem, "", "§7Klicke, um mehr Infos zu §ferfahren§7.");
        }

        @Override
        public int getPrice(final Player player) {
            if (this.rankName.equalsIgnoreCase("X")) {
                final MuxRanks.PermissionsUser user = MuxGemShop.this.ms.getPerms().getUserData(player.getUniqueId());
                if (user != null && user.getExpireData() != null && user.getGroup().equalsIgnoreCase("Gold")) {
                    // current + 30 Tage
                    final long leftOverDays = ((user.getExpireData().getExpireTime() - System.currentTimeMillis()) / 86400000L) - 30L;
                    if (leftOverDays > 0L) {
                        return Math.max(4000, (int) (super.getPrice(player) - 133L * leftOverDays));
                    }
                }
            }
            return super.getPrice(player);
        }

    }

    class ExtraEnderChestItem extends GemItem {
        protected ExtraEnderChestItem(final GemShopCategory category, final int price, final String name, final ItemStack displayItem) {
            super(category, price, name, displayItem);
        }

        @Override
        public void receive(final Player receiver, final int amount) {
            if (isOwned(receiver)) return;
            final MuxExtraUser extraUser = ms.getExtras().getExtraUser(receiver.getUniqueId());
            ms.getExtras().setExtraEnderChest(receiver.getUniqueId(), extraUser, true);
            ms.getExtras().saveExtraUser(extraUser);
        }

        @Override
        public int getAmount(final ClickType clickType) {
            return 1;
        }

        @Override
        public ItemStack getDisplayItem(final Player player) {
            final boolean owned = isOwned(player);
            final ItemStack displayItem = owned ? ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (short) 10), super.getDisplayItem().getItemMeta().getDisplayName()) : super.getDisplayItem();
            return ms.addLore(displayItem, owned ? new String[]{"", "§aDu besitzt bereits eine Doppelte Enderkiste." }
                    : new String[]{"", "§7Klicke, um diesen Upgrade zu §akaufen§7." });
        }

        @Override
        public boolean isOwned(final Player player) {
            return ms.getExtras().getExtraUser(player.getUniqueId()).getEnderChest() == (byte) 1;
        }
    }

    class MountItem extends GemItem {
        private final short id;

        protected MountItem(final GemShopCategory category, final int price, final String name, final ItemStack displayItem, final short id) {
            super(category, price, name, displayItem);
            this.id = id;
        }

        @Override
        public void receive(final Player receiver, final int amount) {
            if (isOwned(receiver)) return;
            final MuxExtraUser extraUser = ms.getExtras().getExtraUser(receiver.getUniqueId());
            final MuxMounts.MountStore mountStore = extraUser.getMounts();
            mountStore.getOwned().add(id);
            extraUser.setMounts(mountStore);
            ms.getExtras().saveExtraUser(extraUser);
        }

        @Override
        public int getAmount(final ClickType clickType) {
            return 1;
        }

        @Override
        public ItemStack getDisplayItem(final Player player) {
            final boolean owned = isOwned(player);
            final ItemStack displayItem = owned ? ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (short) 10), super.getDisplayItem().getItemMeta().getDisplayName()) : super.getDisplayItem();
            return ms.addLore(displayItem, owned ? new String[]{"", "§aDu besitzt dieses Mount bereits." }
                    : new String[]{"", "§7Klicke, um dieses Mount zu §akaufen§7." });
        }

        @Override
        public boolean isOwned(final Player player) {
            return ms.getExtras().getExtraUser(player.getUniqueId()).getMounts().getOwned().contains(id);
        }
    }

    class EmojiItem extends GemItem {
        private final short id;

        protected EmojiItem(final GemShopCategory category, final int price, final String name, final ItemStack displayItem, final short id) {
            super(category, price, name, displayItem);
            this.id = id;
        }

        @Override
        public void receive(final Player receiver, final int amount) {
            if (isOwned(receiver)) return;
            final MuxExtraUser extraUser = ms.getExtras().getExtraUser(receiver.getUniqueId());
            final Set<Short> emojis = extraUser.getEmojis();
            emojis.add(id);
            extraUser.setEmojis(emojis);
            ms.getExtras().saveExtraUser(extraUser);
        }

        @Override
        public int getAmount(final ClickType clickType) {
            return 1;
        }

        @Override
        public ItemStack getDisplayItem(final Player player) {
            final boolean owned = this.isOwned(player);
            final ItemStack displayItem = owned ? ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (short) 10), super.getDisplayItem().getItemMeta().getDisplayName()) : super.getDisplayItem();
            return ms.addLore(displayItem, owned ? new String[]{"", "§aDu besitzt dieses Emoji bereits." }
                    : new String[]{"", "§7Klicke, um dieses Emoji zu §akaufen§7." });
        }

        @Override
        public boolean isOwned(final Player player) {
            return ms.getExtras().getExtraUser(player.getUniqueId()).getEmojis().contains(id);
        }
    }

    class ExtraCommandItem extends GemItem {
        private final String command;

        protected ExtraCommandItem(final GemShopCategory category, final int price, final String name, final ItemStack displayItem, final String command) {
            super(category, price, name, displayItem);
            this.command = command;
        }

        @Override
        public void receive(final Player receiver, final int amount) {
            if (isOwned(receiver)) return;
            final MuxExtraUser extraUser = ms.getExtras().getExtraUser(receiver.getUniqueId());
            final Set<String> commands = extraUser.getExtraCMDs();
            commands.add(command);
            extraUser.setExtraCMDs(commands);
            ms.getExtras().saveExtraUser(extraUser);
        }

        @Override
        public int getAmount(final ClickType clickType) {
            return 1;
        }

        @Override
        public ItemStack getDisplayItem(final Player player) {
            final boolean owned = isOwned(player);
            final ItemStack displayItem = owned ? ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (short) 10), super.getDisplayItem().getItemMeta().getDisplayName()) : super.getDisplayItem();
            return ms.addLore(displayItem, owned ? new String[]{"", "§aDu besitzt diesen Befehl bereits." }
                    : new String[]{"", "§7Klicke, um diesen Befehl zu §akaufen§7." });
        }

        @Override
        public boolean isOwned(final Player player) {
            return ms.getExtras().getExtraUser(player.getUniqueId()).getExtraCMDs().contains(command);
        }
    }

    class ChestItem extends GemItem {
        private final MuxChests.ChestType chest;

        protected ChestItem(final GemShopCategory category, final int price, final String name, final ItemStack displayItem, final MuxChests.ChestType chest) {
            super(category, price, name, displayItem);
            this.chest = chest;
        }

        @Override
        public int getAmount(final ClickType clickType) {
            return 1;
        }

        @Override
        public ItemStack getDisplayItem(final Player player) {
            return ms.addLore(super.getDisplayItem(), "", "§7Klicke, um diese Kiste zu §akaufen§7.");
        }

        @Override
        public void receive(final Player receiver, final int amount) {
            ms.getChests().addChest(receiver.getUniqueId(), chest);
        }
    }
}