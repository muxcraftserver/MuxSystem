package me.muxteam.shop;

import me.muxteam.basic.ConfirmInventory;
import me.muxteam.basic.MuxActions.PlayerAction;
import me.muxteam.basic.Pageifier;
import me.muxteam.muxsystem.MuxInventory.InvType;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class MuxMarket {
    private MuxSystem ms;
    private final CopyOnWriteArrayList<PremiumItem> premiumitems = new CopyOnWriteArrayList<>();

    public MuxMarket(final MuxSystem ms) {
        this.ms = ms;
        premiumitems.addAll(ms.getDB().loadPremiumItems());
    }

    public void close() {
        this.ms = null;
    }

    public void handleInventory(final Player p, final ItemStack is, final int slot, final Inventory inv) {
        final String invname = inv.getName();
        if (invname.equalsIgnoreCase("§0§lPremium Markt")) {
            if (slot == 5) {
                boolean sortedByPrice = inv.getItem(5).getItemMeta().getDisplayName().startsWith("§f§l") == false;
                String[] showOptions = new String[]{"Ablauf", "Preis"};
                int selected = sortedByPrice ? 0 : 1;
                final ItemStack showType = ms.renameItemStack(new ItemStack(Material.BLAZE_ROD),
                        ms.getSelection(showOptions, "§f", selected),
                        "§7Lege fest, nach welchem Kriterium die", "§7Items im Markt sortiert werden sollen.",
                        "",
                        "§7Klicke, um alle anders zu §fsortieren§7.");
                inv.setItem(5, showType);
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                final Pageifier<PremiumItem> items = getPremiumItems(sortedByPrice == false);

                inv.setItem(4, ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 11), ms.getLang("market.offers") + " §0Page " + 1, ms.getLang("market.offerslore1"), ms.getLang("market.offerslore2")));
                if (getNumber(inv.getItem(4)) <= items.getPages().size() - 1)
                    inv.setItem(8, ms.getHeadFromURL("https://textures.minecraft.net/texture/19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf", ms.getLang("market.nextpage")));
                inv.setItem(7, new ItemStack(Material.AIR)); // we are on page 1 again so there will be no previous page
                if (items.getPages().size() - 1 < getNumber(inv.getItem(4))) inv.setItem(8, null);

                for (int i = 18; i < 54; i++) {
                    inv.setItem(i, new ItemStack(Material.AIR));
                }
                int i = 18;
                for (final PremiumItem mi : items.getPage(getNumber(inv.getItem(4)) - 1)) {
                    if (mi.getItem() == null) {
                        continue;
                    }
                    final ItemStack item = mi.getItem().clone();
                    inv.setItem(i, ms.renameItemStackAddLore(item,
                            item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null ?
                                    item.getItemMeta().getDisplayName() : "", "",
                            "§7" + ms.getLang("market.price") + ": §e§l" + ms.getNumberFormat(mi.getPrice()) + " MuxCoins",
                            "",
                            "§7" + ms.getLang("market.expiresin") + ": §f" + getExpiry(mi),
                            "§0Id: " + mi.getKeyId(),
                            "§7" + ms.getLang("market.offerfrom") + " " + ms.getServer().getOfflinePlayer(mi.getOwner()).getName()));
                    i++;
                }

                return;
            }
            boolean sortedByPrice = inv.getItem(5).getItemMeta().getDisplayName().startsWith("§f§l") == false;
            final Pageifier<PremiumItem> items = getPremiumItems(sortedByPrice);
            if (slot == 4) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            } else if (slot == 7) {
                if (getNumber(inv.getItem(4)) - 1 <= 0) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                inv.setItem(4, ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 11), ms.getLang("market.offers") + " §0Page " + (getNumber(inv.getItem(4)) - 1), ms.getLang("market.offerslore1"), ms.getLang("market.offerslore2")));
                if ((getNumber(inv.getItem(4)) - 1) == 0) inv.setItem(7, null);
                if (getNumber(inv.getItem(4)) <= items.getPages().size() - 1)
                    inv.setItem(8, ms.getHeadFromURL("https://textures.minecraft.net/texture/19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf", ms.getLang("market.nextpage")));
                for (int i = 18; i < 54; i++) {
                    inv.setItem(i, new ItemStack(Material.AIR));
                }
                int i = 18;
                for (final PremiumItem mi : items.getPage(getNumber(inv.getItem(4)) - 1)) {
                    if (mi.getItem() == null) {
                        continue;
                    }
                    final ItemStack item = mi.getItem().clone();
                    inv.setItem(i, ms.renameItemStackAddLore(item,
                            item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null ?
                                    item.getItemMeta().getDisplayName() : "", "",
                            "§7" + ms.getLang("market.price") + ": §e§l" + ms.getNumberFormat(mi.getPrice()) + " MuxCoins",
                            "",
                            "§7" + ms.getLang("market.expiresin") + ": §f" + getExpiry(mi),
                            "§0Id: " + mi.getKeyId(),
                            "§7" + ms.getLang("market.offerfrom") + " " + ms.getServer().getOfflinePlayer(mi.getOwner()).getName()));
                    i++;
                }
                return;
            } else if (slot == 8) {
                if (items.getPages().size() - 1 < getNumber(inv.getItem(4))) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return;
                }
                p.playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 1F);
                inv.setItem(4, ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 11), ms.getLang("market.offers") + " §0Page " + (getNumber(inv.getItem(4)) + 1), ms.getLang("market.offerslore1"), ms.getLang("market.offerslore2")));
                inv.setItem(7, ms.getHeadFromURL("https://textures.minecraft.net/texture/bd69e06e5dadfd84e5f3d1c21063f2553b2fa945ee1d4d7152fdc5425bc12a9", ms.getLang("market.previouspage")));
                if (items.getPages().size() - 1 < getNumber(inv.getItem(4))) inv.setItem(8, null);

                for (int i = 18; i < 54; i++) {
                    inv.setItem(i, new ItemStack(Material.AIR));
                }
                int i = 18;
                for (final PremiumItem mi : items.getPage(getNumber(inv.getItem(4)) - 1)) {
                    if (mi.getItem() == null) {
                        continue;
                    }
                    final ItemStack item = mi.getItem().clone();
                    inv.setItem(i, ms.renameItemStackAddLore(item,
                            item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null ?
                                    item.getItemMeta().getDisplayName() : "", "",
                            "§7" + ms.getLang("market.price") + ": §e§l" + ms.getNumberFormat(mi.getPrice()) + " MuxCoins",
                            "",
                            "§7" + ms.getLang("market.expiresin") + ": §f" + getExpiry(mi),
                            "§0Id: " + mi.getKeyId(),
                            "§7" + ms.getLang("market.offerfrom") + " " + ms.getServer().getOfflinePlayer(mi.getOwner()).getName()));
                    i++;
                }
                return;
            }
            if (is.hasItemMeta() == false || is.getItemMeta().hasLore() == false) {
                return;
            }
            for (final String s : is.getItemMeta().getLore()) {
                if (s.startsWith("§0Id: ")) {
                    p.performCommand("market buy " + s.split(" ")[1]);
                }
            }
        }
    }

    public boolean handleCommand(final Player p, final String[] args) {
        if (ms.inBattle(p.getName(), p.getLocation())) {
            ms.showItemBar(p, ms.hnotinfight);
            return true;
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("buy"))) {
            final PremiumItem mi = getItemFromId(Long.parseLong(ChatColor.stripColor(args[1])));
            if (mi == null) {
                ms.showItemBar(p, ms.getLang("market.notfound"));
                return true;
            } else if (mi.getOwner().toString().equalsIgnoreCase(p.getUniqueId().toString())) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return true;
            }
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            final PlayerAction confirm = player -> {
                player.closeInventory();
                if (premiumitems.contains(mi) == false) {
                    ms.showItemBar(player, ms.getLang("market.notinmarket"));
                    return;
                }
                final MuxUser u = ms.getMuxUser(player.getName());
                if (u.getCoins() < mi.getPrice()) {
                    ms.showItemBar(player, ms.getLang("market.notenough"));
                    return;
                }
                final OfflinePlayer off = ms.getServer().getOfflinePlayer(mi.getOwner());
                MuxUser u2 = off.isOnline() ? ms.getMuxUser(off.getName()) : ms.getDB().loadPlayer(off.getUniqueId());
                if (u2 == null /*|| ((ms.getPerms().hasGroup(player.getUniqueId()) == false && player.getEnderChest().firstEmpty() == 0 && u.getKills() == 0) || u2.getIp().equals(u.getIp()))*/) {
                    ms.showItemBar(player, "§cDu kannst das derzeit nicht kaufen.");
                    return;
                }
                u.setCoins(u.getCoins() - mi.getPrice());
                ms.getHistory().addCoinHistory(player.getUniqueId(), "Premium Markt", Integer.parseInt("-" + mi.getPrice()));
                if (mi.getPrice() > 90000) {
                    ms.getShop().logLargeTransaction(player.getName() + " » " + u2.getName() + " (" + ms.getNumberFormat(mi.getPrice()) + ")");
                }
                int profit = mi.getPrice();
                ms.sendScoreboard(player);
                if (off.isOnline() == false) {
                    u2 = ms.getChat().sendOfflineMessage(off, "MuxShop", "§7Dein Premium Item wurde §agekauft§7. §7Du hast §e" + profit + " MuxCoins §7erhalten.", false);
                }
                if (u2 != null) {
                    u2.addCoins(profit);
                    ms.getHistory().addCoinHistory(u2.getUUID(), "Premium Markt", mi.getPrice());
                    ms.getDB().soldItem(u2.getUUID(), new ShopItem(-1, null, "0:0", profit, 1));
                    ms.saveMuxUser(u2);
                }
                if (off.isOnline()) {
                    final Player pl = off.getPlayer();
                    pl.playSound(pl.getLocation(), Sound.NOTE_PIANO, 2F, 1F);
                    ms.sendScoreboard(pl);
                    pl.sendMessage(ms.getLang("market.someonepurchased"));
                    pl.sendMessage(String.format(ms.getLang("market.yougotmuxcoins"), profit + ""));
                }
                removePremiumItem(p, mi, true);
                ms.showItemBar(player, ms.getLang("market.itempurchased"));
            }, cancel = player -> player.performCommand("market");
            new ConfirmInventory(ms, confirm, cancel).show(p, ms.getLang("market.buyitem"), ms.getLang("confirm") + " (§a" + ms.getNumberFormat(mi.getPrice()) + " MuxCoins)", ms.getLang("abort"));
            return true;
        }
        final Pageifier<PremiumItem> items = getPremiumItems(false);
        final Inventory inv = ms.getServer().createInventory(null, 54, "§0§lPremium Markt");
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 11), ms.getLang("market.offers") + " §0Page 1", ms.getLang("market.offerslore1"), ms.getLang("market.offerslore2")));
        if (items.getPages().size() - 1 > 0)
            inv.setItem(8, ms.getHeadFromURL("https://textures.minecraft.net/texture/19bf3292e126a105b54eba713aa1b152d541a1d8938829c56364d178ed22bf", ms.getLang("market.nextpage")));
        int i = 18;
        for (final PremiumItem mi : items.getPage(0)) {
            if (mi.getItem() == null) {
                continue;
            }
            final ItemStack item = mi.getItem().clone();
            inv.setItem(i, ms.renameItemStackAddLore(item,
                    item.getItemMeta() != null && item.getItemMeta().getDisplayName() != null ?
                            item.getItemMeta().getDisplayName() : "", "",
                    "§7" + ms.getLang("market.price") + ": §e§l" + ms.getNumberFormat(mi.getPrice()) + " MuxCoins",
                    "",
                    "§7" + ms.getLang("market.expiresin") + ": §f" + getExpiry(mi),
                    "§0Id: " + mi.getKeyId(),
                    "§7" + ms.getLang("market.offerfrom") + " " + ms.getServer().getOfflinePlayer(mi.getOwner()).getName()));
            i++;
        }
        String[] showOptions = new String[]{"Ablauf", "Preis"};
        int selected = 0;

        final ItemStack showType = ms.renameItemStack(new ItemStack(Material.BLAZE_ROD),
                ms.getSelection(showOptions, "§f", selected),
                "§7Lege fest, nach welchem Kriterium die", "§7Items im Markt sortiert werden sollen.",
                "",
                "§7Klicke, um alle anders zu §fsortieren§7.");
        inv.setItem(5, showType);
        final InvType it = ms.getActiveInv(p.getName());
        if (it != InvType.MARKET && it != InvType.CONFIRM) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), InvType.MARKET);
        return true;
    }

    private Pageifier<PremiumItem> getPremiumItems(final boolean sortedByPrice) {
        final List<PremiumItem> temp = new ArrayList<>();
        for (final PremiumItem mi : premiumitems) {
            if (mi.isValid()) temp.add(mi);
        }
        if (sortedByPrice == false) {
            temp.sort(Comparator.comparingLong(value -> (86500000 * 2 + value.getUpdated() - System.currentTimeMillis())));
        } else
            temp.sort(Comparator.comparingInt(PremiumItem::getPrice));
        final Pageifier<PremiumItem> pages = new Pageifier<>(4 * 9);
        for (final PremiumItem mi : temp) {
            pages.addItem(mi);
        }
        return pages;
    }

    public Pageifier<PremiumItem> getPremiumItemsForPlayerAsPage(final UUID uuid) {
        final Pageifier<PremiumItem> pages = new Pageifier<>(4 * 9);
        for (final PremiumItem mi : premiumitems) {
            if (mi.getOwner().toString().equalsIgnoreCase(uuid.toString()))
                pages.addItem(mi);
        }
        return pages;
    }

    public PremiumItem getItemFromId(final long id) {
        for (final PremiumItem mi : premiumitems) {
            if (mi.getKeyId() == id) {
                return mi;
            }
        }
        return null;
    }

    public List<PremiumItem> getItemsForUUID(final UUID uuid) {
        final List<PremiumItem> items = new ArrayList<>();
        for (final PremiumItem mi : premiumitems) {
            if (mi.getOwner().toString().equalsIgnoreCase(uuid.toString())) {
                items.add(mi);
            }
        }
        return items;
    }

    public boolean addPremiumItem(final Player p, final ItemStack is, final int price) {
        final PremiumItem mi = new PremiumItem(-1, p.getUniqueId(), is, price, System.currentTimeMillis());
        if (ms.getDB().addPremiumItem(p.getUniqueId(), mi) == false) {
            return false;
        }
        premiumitems.add(mi);
        return true;
    }

    public void removePremiumItem(final Player p, final PremiumItem item, final boolean giveitem) {
        if (ms.getDB().removePremiumItem(item) == false) {
            return;
        }
        if (premiumitems.contains(item)) {
            premiumitems.remove(item);
            if (giveitem == false) return;
            if (p.getInventory().firstEmpty() == -1) {
                p.getLocation().getWorld().dropItem(p.getLocation(), item.getItem().clone());
                ms.showItemBar(p, ms.getLang("market.itemdropped"));
            } else {
                p.getInventory().addItem(item.getItem().clone());
            }
            p.updateInventory();
            p.playSound(p.getLocation(), Sound.CHICKEN_EGG_POP, 5, 5);
        }
    }

    private int getNumber(final ItemStack is) {
        return Integer.parseInt(is.getItemMeta().getDisplayName().split(" ")[2]);
    }

    public String getExpiry(final PremiumItem item) {
        if (item.isValid() == false) return ms.getLang("market.expired");
        return ms.getTime((int) ((86500000 * 2 + item.getUpdated() - System.currentTimeMillis()) / 1000L));
    }
}