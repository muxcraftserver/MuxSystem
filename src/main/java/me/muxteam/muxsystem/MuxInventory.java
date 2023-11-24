package me.muxteam.muxsystem;

import me.muxteam.base.PlayerBase;
import me.muxteam.basic.ConfirmInventory;
import me.muxteam.basic.MuxActions.AnvilAction;
import me.muxteam.basic.MuxOfflinePlayer;
import me.muxteam.basic.NMSReflection;
import net.minecraft.server.v1_8_R3.MobSpawnerAbstract;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.craftbukkit.v1_8_R3.block.CraftCreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.SpawnEgg;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


public final class MuxInventory implements Listener {
    private final Map<String, InvType> inv = new HashMap<>();
    private final Map<String, MuxOfflinePlayer> editinv = new HashMap<>();
    public final Map<String, AnvilAction> anvils = new HashMap<>();
    public final Map<String, ConfirmInventory.ConfirmActions> confirminvs = new HashMap<>();
    private final Map<String, CreatureSpawner> spawner = new HashMap<>();
    public final Map<String, CreatureSpawner> lastspawnerdelay = new HashMap<>();
    private Inventory spawnerinv, ptimeinv, donateinvgems;

    private boolean NACHOSPIGOT = false;

    private final MuxSystem ms;

    public MuxInventory(final MuxSystem ms) {
        this.ms = ms;
        ms.getServer().getPluginManager().registerEvents(this, ms);
        setup();
        try {
            MobSpawnerAbstract.class.getDeclaredField("tickDelay");
        } catch (NoSuchFieldException ignored) {
            NACHOSPIGOT = true;
        }

    }

    public void close() {
        final Server sr = ms.getServer();
        inv.keySet().parallelStream().map(sr::getPlayer).forEach(p -> {
            if (p != null) p.closeInventory();
        });
    }

    public void setInv(final String pname, final InvType it) {
        inv.put(pname, it);
    }

    public InvType getInv(final String pname) {
        return inv.get(pname);
    }

    public Map<String, InvType> getInvs() {
        return inv;
    }

    public void setEditInv(final String pname, final MuxOfflinePlayer of) {
        editinv.put(pname, of);
    }

    public void setSpawner(final String pname, final CreatureSpawner s) {
        spawner.put(pname, s);
    }

    public void setAnvil(final String pname, final AnvilAction a) {
        anvils.put(pname, a);
    }

    public void setConfirm(final String pname, final ConfirmInventory.ConfirmActions a) {
        confirminvs.put(pname, a);
    }

    public void openSpawner(final Player p) {
        p.closeInventory();
        p.openInventory(spawnerinv);
        ms.setActiveInv(p.getName(), InvType.SPAWNER);
    }

    public void openPTime(final Player p) {
        final InvType it = ms.getActiveInv(p.getName());
        if (it != InvType.PTIME && it != InvType.MENU) p.closeInventory();
        p.openInventory(ptimeinv);
        ms.setActiveInv(p.getName(), InvType.PTIME);
    }

    public boolean openTrash(final Player p) {
        if (p.getGameMode() == GameMode.CREATIVE) {
            new ConfirmInventory(ms, (pl -> {
                pl.playSound(pl.getLocation(), Sound.BAT_LOOP, 1F, 1F);
                pl.getInventory().clear();
                pl.closeInventory();
                ms.getMenu().addMenuItem(pl, false);
                ms.showItemBar(pl, "§fDein Inventar wurde §ageleert§f.");
            }), pl -> {
                pl.playSound(pl.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                pl.closeInventory();
            }).show(p, "§0§lInventar leeren?", "§aBestätigen", "§cAbbrechen");
            return true;
        }
        final Inventory trashinv = ms.getServer().createInventory(null, 45, ms.getLang("cmd.trashinv"));
        if (ms.getActiveInv(p.getName()) != InvType.TRASH && ms.getActiveInv(p.getName()) != InvType.ULTRA)
            p.closeInventory();
        p.openInventory(trashinv);
        ms.setActiveInv(p.getName(), InvType.TRASH);
        return true;
    }

    public boolean openBenefits(final Player p) {
        final Inventory donateinv = ms.getServer().createInventory(null, 27, "§0§lMuxVorteile");
        final boolean ultra = p.hasPermission("muxsystem.ultra");
        if (ultra == false)
            donateinv.setItem(10, ms.renameItemStack(new ItemStack(Material.DIAMOND_CHESTPLATE), ms.getLang("benefits.ranks"), ms.getLang("benefits.rankslore1"), ms.getLang("benefits.rankslore2"), ms.getLang("benefits.rankslore3"), "", ms.getLang("benefits.rankslore4")));
        donateinv.setItem(ultra ? 11 : 13, ms.renameItemStack(new ItemStack(Material.EMERALD), "§a§lGems", "§7Mit dieser Währung kannst du unter", "§7anderem alles im Gemshop kaufen", "§7oder gegen MuxCoins umtauschen.", "", "§7Klicke, um §aGems §7zu erwerben."));
        donateinv.setItem(ultra ? 15 : 16, ms.renameItemStack(new ItemStack(Material.BLAZE_POWDER), "§6§lPerks", ms.getLang("benefits.perkslore1"), ms.getLang("benefits.perkslore2"), ms.getLang("benefits.perkslore3"), "", ms.getLang("benefits.perkslore4")));
        if (ms.getActiveInv(p.getName()) != InvType.BENEFITS && ms.getActiveInv(p.getName()) != InvType.EXTRAS)
            p.closeInventory();
        p.openInventory(donateinv);
        ms.setActiveInv(p.getName(), InvType.BENEFITS);
        return true;
    }

    public boolean isBeingEdited(final Player p, final boolean close) {
        if (editinv.isEmpty()) return false;
        boolean editing = false;
        for (final Map.Entry<String, MuxOfflinePlayer> entry : editinv.entrySet()) {
            if (entry.getValue().getUUID().equals(p.getUniqueId())) {
                editing = true;
                if (close) {
                    final Player pl = ms.getServer().getPlayer(entry.getKey());
                    pl.closeInventory();
                }
            }
        }
        return editing;
    }

    public void handleQuit(final String pname) {
        inv.remove(pname); // Just to make sure
        anvils.remove(pname);
        spawner.remove(pname);
        editinv.remove(pname);
        confirminvs.remove(pname);
    }

    @EventHandler
    public void onInvClose(final InventoryCloseEvent e) {
        final String pname = e.getPlayer().getName();
        final InvType it = inv.remove(pname);
        if (it != null) {
            final Player p = (Player) e.getPlayer();
            if (anvils.remove(pname) != null) {
                e.getInventory().clear();
                ms.getTrades().cancelTrade(pname);
            } else if (it == InvType.TRADE) {
                ms.getTrades().cancelTrade(pname);
            } else if (it == InvType.TRASH) {
                if (e.getInventory().firstEmpty() != 0) {
                    p.playSound(p.getLocation(), Sound.BAT_LOOP, 1F, 1F);
                }
            } else if (it == InvType.MINECAPTCHA) {
                ms.getAntiBot().handleMineCaptchaClose(p, e.getInventory());
            } else if (it == InvType.MENU) {
                ms.getMenu().getBenefits().onClosePremium(p);
            } else if (editinv.containsKey(pname) && ms.isFullTrusted(pname)) {
                final MuxOfflinePlayer of = editinv.remove(pname);
                if (of != null) {
                    if (e.getInventory().getSize() == 27) of.setEnderChest(e.getInventory());
                    else if (e.getInventory().getSize() == 54)
                        ms.getExtraEnderChest().setEnderchest(p, of.getUUID(), of.getName(), e.getInventory());
                    else if (it == InvType.PLAYERCHEST) of.setInventory(e.getInventory());
                }
            }
            ms.getPvP().onCloseInv(p);
            spawner.remove(pname);
            confirminvs.remove(pname);
        } else if (e.getInventory().getTitle().equals(ms.getLang("extraenderchest"))) {
            ms.getExtraEnderChest().handleClose((Player) e.getPlayer(), e.getInventory());
        }
    }

    @EventHandler
    public void onInvDrag(final InventoryDragEvent e) {
        if (e.getWhoClicked().getGameMode() == GameMode.CREATIVE) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInvClick(final InventoryClickEvent e) {
        final Player p = (Player) e.getWhoClicked();
        final InvType it = inv.get(p.getName());
        ms.updateAFK(p);
        if (e.getAction() == InventoryAction.UNKNOWN || e.getClick() == ClickType.MIDDLE || e.getAction() == InventoryAction.CLONE_STACK ||
                (e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD && e.getClick() == ClickType.NUMBER_KEY)) {
            e.setCancelled(true);
            p.updateInventory();
            return;
        } else if (it != null) {
            final ItemStack i = e.getCurrentItem();
            if (it == InvType.PLAYERCHEST || it == InvType.ARMOR) {
                if (ms.isFullTrusted(p.getName()) == false) {
                    e.setCancelled(true);
                    p.updateInventory();
                }
                return;
            } else if (it == InvType.TRADE) {
                if (i == null || i.getType() == Material.AIR || ms.getUltra().isUltraItem(i) || ms.isMenuItem(i)) {
                    e.setCancelled(true);
                    return;
                }
                ms.getTrades().handleInventory(p, i, e);
                return;
            } else if (it == InvType.SHOP && e.getRawSlot() > 53) {
                if (i != null && i.getType() != Material.AIR) {
                    e.setCancelled(true);
                    final int slot = e.getSlot(), handslot = p.getInventory().getHeldItemSlot();
                    final ItemStack clicked = p.getInventory().getItem(slot).clone(), hand = p.getInventory().getItem(handslot);
                    if (ms.isMenuItem(i) || (hand != null && ms.isMenuItem(hand))) return;
                    p.getInventory().setItem(slot, null);
                    p.getInventory().setItem(slot, hand);
                    p.getInventory().setItem(handslot, clicked);
                    p.performCommand("sell");
                }
                return;
            } else if (it == InvType.MENU && e.getRawSlot() > 44) {
                if (i == null || i.getType() == Material.AIR) {
                    e.setCancelled(true);
                    return;
                }
                e.setCancelled(true);
                ms.getMenu().getBenefits().handleBelowInventory(p, i, e);
                return;
            } else if (it == InvType.EVENT) {
                if (e.getInventory().getSize() == 54 && e.getInventory().getTitle().contains("GiveAll")) {
                    if (p.isOp() == false) {
                        e.setCancelled(true);
                    } else {
                        if (e.getRawSlot() < 9 || e.isShiftClick()) e.setCancelled(true);
                        ms.getEvents().handleInventory(p, i, e.getInventory(), e.getRawSlot(), e.isRightClick());
                    }
                    return;
                }
            } else if (it == InvType.TRASH) return;
            e.setCancelled(true);
            if (i != null && i.getType() != Material.AIR && e.getRawSlot() < e.getInventory().getSize()) {
                if (it == InvType.MENU) {
                    ms.getMenu().handleInventory(p, i, e.getClickedInventory().getTitle(), e.isRightClick());
                } else if (it == InvType.CLANS) {
                    ms.getClans().handleInventory(p, i, e.isRightClick(), e.getClickedInventory());
                } else if (it == InvType.WARPS) {
                    ms.getWarps().handleInventory(p, i);
                } else if (it == InvType.POLLS) {
                    ms.getPolls().handleInventory(p, i, e.getRawSlot(), e.getClickedInventory());
                } else if (it == InvType.HOMES) {
                    ms.getHomes().handleInventory(p, i, e.getClickedInventory(), e.getRawSlot(), e.isRightClick());
                } else if (it == InvType.AFFILIATE) {
                    ms.getAffiliate().handleInventory(p, i, e.getRawSlot(), e.getClickedInventory());
                } else if (it == InvType.PVP) {
                    ms.getPvP().handleInventory(p, i, e.getClickedInventory());
                } else if (it == InvType.CLANWAR) {
                    ms.getClanWar().handleInventory(p, i);
                } else if (it == InvType.EVENT) {
                    ms.getEvents().handleInventory(p, i, e.getClickedInventory(), e.getRawSlot(), e.isRightClick());
                } else if (it == InvType.FRIENDS) {
                    ms.getFriends().handleInventory(p, i, e.getClickedInventory(), e.getRawSlot(), e.isRightClick());
                } else if (it == InvType.PROFILE) {
                    ms.getProfiles().handleInventory(p, i, e.getClickedInventory());
                } else if (it == InvType.MUXCHESTS) {
                    ms.getChests().handleInventory(p, i, e.getRawSlot(), e.getClickedInventory().getTitle(), e.getClickedInventory().getSize());
                } else if (it == InvType.EARN) {
                    ms.getShop().handleEarnInventory(p, i);
                } else if (it == InvType.SHOP) {
                    ms.getShop().handleSell(p, i, e.getRawSlot(), e.getClickedInventory(), e.isRightClick());
                } else if (it == InvType.SHOPSEARCH) {
                    ms.getShop().handleSearch(p, i, e.getRawSlot(), e.getClickedInventory());
                } else if (it == InvType.MINECAPTCHA) {
                    ms.getAntiBot().handleMineCaptchaInv(p, e.getRawSlot());
                } else if (it == InvType.BASES) {
                    ms.getBase().getBaseHandler().handleInventory(p, i, e.getRawSlot(), e.getClickedInventory());
                } else if (it == InvType.TIMINGS) {
                    ms.getTimings().handleInventory(p, i, e.getClickedInventory(), e.getRawSlot());
                } else if (it == InvType.FILTER) {
                    ms.getChatFilter().handleInventory(p, i, e.isRightClick(), e.getClickedInventory().getSize());
                } else if (it == InvType.HISTORY) {
                    ms.getHistory().handleInventory(p, i, e.getRawSlot(), e.getClickedInventory(), e.isRightClick());
                } else if (it == InvType.MARKET) {
                    ms.getMarket().handleInventory(p, i, e.getRawSlot(), e.getInventory());
                } else if (it == InvType.SUPPORT) {
                    ms.getSupport().handleInventory(p, i, e.isRightClick());
                } else if (it == InvType.EXTRAS) {
                    ms.getExtras().handleInventory(p, i, e.getClickedInventory().getName(), e.getRawSlot());
                } else if (it == InvType.ULTRA) {
                    ms.getUltra().handleInventory(p, i);
                } else if (it == InvType.GOLD) {
                    ms.getGold().handleInventory(p, i);
                } else if (it == InvType.X) {
                    ms.getCustomRank().handleInventory(p, i, e.getSlot(), e.getClickedInventory().getTitle(), e.isRightClick());
                } else if (it == InvType.CREATOR) {
                    ms.getCreators().handleInventory(p, i);
                } else if (it == InvType.CASINO) {
                    ms.getCasino().handleInventory(p, i, e.getRawSlot(), e.isRightClick(), e.getClickedInventory());
                } else if (it == InvType.ADMIN) {
                    ms.getAdmin().handleInventory(p, e.getRawSlot(), i, e.getClickedInventory().getName(), e.isRightClick(), e.getClickedInventory().getSize());
                } else if (it == InvType.FEEDBACK) {
                    ms.getFeedback().handleInventory(p, i, e.getClickedInventory(), e.getRawSlot(), e.isRightClick());
                } else if (it == InvType.HOLOGRAMS) {
                    ms.getHolograms().handleInventory(p, i, e.getRawSlot(), e.getClickedInventory());
                } else if (it == InvType.USERCONTROL) {
                    ms.getOverview().handleInventory(p, i, e.getClickedInventory());
                } else if (it == InvType.TEAM) {
                    ms.getTeam().handleInventory(p, i, e.getClickedInventory().getName());
                } else if (it == InvType.REPORTS) {
                    ms.getReports().handleInventory(p, i, e.getClickedInventory().getName(), e.getClickedInventory());
                } else if (it == InvType.GEMSHOP) {
                    ms.getGemShop().handleInventory(p, i, e.getRawSlot(), e.getClick(), e.getClickedInventory());
                } else if (it == InvType.PMENU) {
                    handlePlayerMenuInventory(p, i, e.getClickedInventory().getTitle());
                } else if (it == InvType.BENEFITS) {
                    handleBenefitsInventory(p, i, e.getClickedInventory(), e.getRawSlot());
                } else if (it == InvType.SPAWNER) {
                    handleSpawnerInv(p, i);
                } else if (it == InvType.PTIME) {
                    handlePTimeInv(p, i.getType());
                } else if (it == InvType.MACRO) {
                    ms.getGold().handleMacroInv(p, i, e.getClickedInventory());
                } else if (it == InvType.PWEATHER) {
                    ms.getGold().handlePWeatherInv(p, i.getType());
                } else if (it == InvType.CONFIRM) {
                    handleConfirmInv(p, i);
                } else if (it == InvType.MUXANVIL) {
                    handleAnvilInv(p, i, e.getInventory(), e.getRawSlot());
                } else if (it == InvType.NOINTERACT || it == InvType.MESSAGES) {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                }
            } else if (e.getCursor() != null) {
                p.updateInventory();
            }
            return;
        }
        final InventoryType ivt = e.getInventory().getType();
        if (ivt == InventoryType.PLAYER || ivt == InventoryType.CRAFTING) {
            if (ms.getGames().inGame(p) != null || (ms.getEvent(p) != null && ms.getEvent(p).canMoveItems() == false)) {
                ms.showItemBar(p, "§cDu kannst deine Rüstung nicht ablegen.");
                e.setCancelled(true);
                p.updateInventory();
            }
        } else if (ivt == InventoryType.ANVIL && e.getCurrentItem() != null) {
            final ItemStack current = e.getCurrentItem();
            final Inventory topinv = e.getView().getTopInventory();
            boolean colored = false;
            for (final ItemStack i : topinv.getContents()) {
                if (i == null || i.getType() == Material.AIR) continue;
                final ItemMeta im = i.getItemMeta();
                if (im != null && im.hasDisplayName() && (im.getDisplayName().contains("§") || im.getDisplayName().equals(ms.getLang("extraenderchest")))) {
                    colored = true;
                }
            }
            final ItemMeta curim = current.getItemMeta();
            if (curim != null && curim.hasDisplayName() && ms.getChat().isNotSafe(p, curim.getDisplayName().toLowerCase(), false).isEmpty() == false) {
                curim.setDisplayName(null);
                current.setItemMeta(curim);
            }
            if (colored) {
                ms.showItemBar(p, "§cDu kannst keine farbigen Items umbenennen!");
            } else if (current.getType() == Material.ENCHANTED_BOOK || topinv.contains(Material.ENCHANTED_BOOK)) {
                ms.showItemBar(p, ms.getLang("noenchantedbooks"));
            } else if (current.getAmount() > 1) {
                ms.showItemBar(p, ms.getLang("anvil1item"));
            } else {
                return;
            }
            e.setCancelled(true);
            p.updateInventory();
            p.setExp(p.getExp());
        } else if (ivt == InventoryType.BREWING) {
            ms.stackItems(p, Material.POTION, false);
        }
    }

    private void handleBenefitsInventory(final Player p, final ItemStack i, final Inventory inv, final int rawslot) {
        final Material m = i.getType();
        if (rawslot == 4) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        } else if (inv.getItem(0) == null) {
            switch (m) {
                case DIAMOND_CHESTPLATE:
                    p.playSound(p.getLocation(), Sound.CLICK, 0.4F, 1F);
                    final double sale = 10.00D * (p.hasPermission("muxsystem.vip") ? 1 + (p.hasPermission("muxsystem.elite") ? 1.5 + (p.hasPermission("muxsystem.epic") ? 2.5 : 0) : 0) : 0);
                    final Inventory rankinv = ms.getServer().createInventory(null, 27, "§0§lMuxVorteile§0 | Ränge");
                    rankinv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
                    rankinv.setItem(4, ms.renameItemStack(new ItemStack(Material.DIAMOND_CHESTPLATE), ms.getLang("benefits.ranks"), ms.getLang("benefits.rankslore1"), ms.getLang("benefits.rankslore2"), ms.getLang("benefits.rankslore3")));
                    rankinv.setItem(20, ms.renameItemStack(new ItemStack(Material.CHEST), "§b§lVIP", p.hasPermission("muxsystem.vip") ? new String[]{"§aDu hast diesen Rang freigeschaltet."} :
                            new String[]{"§7Preis: §f" + ms.getDecimalFormat(9.99 - sale) + " Euro" + (sale > 0D ? " §7(Upgrade)" : ""), "", ms.getLang("benefits.irankslore1"), ms.getLang("benefits.irankslore2")}));
                    rankinv.setItem(21, ms.renameItemStack(new ItemStack(Material.CHEST, 2), "§e§lELITE", p.hasPermission("muxsystem.elite") ? new String[]{"§aDu hast diesen Rang freigeschaltet."} :
                            new String[]{"§7Preis: §f" + ms.getDecimalFormat(24.99 - sale) + " Euro" + (sale > 0D ? " §7(Upgrade)" : ""), "", ms.getLang("benefits.irankslore1"), ms.getLang("benefits.irankslore2")}));
                    rankinv.setItem(22, ms.renameItemStack(new ItemStack(Material.CHEST, 3), "§8§lEPIC", p.hasPermission("muxsystem.epic") ? new String[]{"§aDu hast diesen Rang freigeschaltet."} :
                            new String[]{"§7Preis: §f" + ms.getDecimalFormat(49.99 - sale) + " Euro" + (sale > 0D ? " §7(Upgrade)" : ""), "", ms.getLang("benefits.irankslore1"), ms.getLang("benefits.irankslore2")}));
                    rankinv.setItem(23, ms.renameItemStack(new ItemStack(Material.CHEST, 4), "§c§lULTRA", p.hasPermission("muxsystem.ultra") ? new String[]{"§aDu hast diesen Rang freigeschaltet."} :
                            new String[]{"§7Preis: §f" + ms.getDecimalFormat(79.99 - sale) + " Euro" + (sale > 0D ? " §7(Upgrade)" : ""), "", ms.getLang("benefits.irankslore1"), ms.getLang("benefits.irankslore2")}));
                    rankinv.setItem(24, ms.renameItemStack(new ItemStack(Material.CHEST, 5), "§e§lGOLD", ms.hasGold(p.getUniqueId()) ? new String[]{"§aDu hast diesen Rang freigeschaltet."} :
                            new String[]{"§7Preis: §a4.000 Gems", "", ms.getLang("benefits.irankslore1"), ms.getLang("benefits.irankslore2")}));
                    p.openInventory(rankinv);
                    break;
                case EMERALD:
                    p.playSound(p.getLocation(), Sound.CLICK, 0.4F, 1F);
                    p.openInventory(donateinvgems);
                    break;
                case BLAZE_POWDER:
                    p.playSound(p.getLocation(), Sound.CLICK, 0.4F, 1F);
                    ms.getExtras().handleInventory(p, new ItemStack(Material.BLAZE_POWDER), "Vorteile");
                    return;
                default:
                    break;
            }
            ms.setActiveInv(p.getName(), InvType.BENEFITS);
            return;
        } else if (m == Material.ITEM_FRAME) {
            p.playSound(p.getLocation(), Sound.CLICK, 0.4F, 1F);
            p.performCommand("rank");
            return;
        }
        if (m != Material.CHEST || i.getItemMeta().getLore().size() == 1) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        }
        p.closeInventory();
        p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.4F, 1F);
        final int rang = i.getAmount();
        if (inv.getTitle().contains("MuxCoins")) {
            buyCoins(p, i.getItemMeta().getDisplayName());
        } else if (inv.getTitle().contains("Gems")) {
            buyGems(p, i.getItemMeta().getDisplayName());
        } else if (rang == 1) {
            buyRank(p, "VIP", "§c");
        } else if (rang == 2) {
            buyRank(p, "ELITE", "§e");
        } else if (rang == 3) {
            buyRank(p, "EPIC", "§8");
        } else if (rang == 4) {
            buyRank(p, "ULTRA", "§c");
        }
    }

    private void buyCoins(final Player p, final String title) {
        p.sendMessage(ms.header((byte) 11, "§e"));
        ms.chatClickHoverLink(p, "  " + title, "§e§oKlicke um MuxCoins zu erwerben", "https://shop.muxcraft.eu/?ign=" + p.getName());
        p.sendMessage(" ");
        ms.chatClickHoverLink(p, "  §7Klicke §ehier§7, um MuxCoins zu erwerben.", "§e§oKlicke um MuxCoins zu erwerben", "https://shop.muxcraft.eu/?ign=" + p.getName());
        p.sendMessage(ms.footer((byte) 11, "§e"));
    }

    private void buyGems(final Player p, final String title) {
        p.sendMessage(ms.header((byte) 10, "§a"));
        ms.chatClickHoverLink(p, "  " + title, "§a§oKlicke um Gems zu erwerben", "https://shop.muxcraft.eu/?ign=" + p.getName());
        p.sendMessage(" ");
        ms.chatClickHoverLink(p, "  §7Klicke §ahier§7, um Gems zu erwerben.", "§a§oKlicke um Gems zu erwerben", "https://shop.muxcraft.eu/?ign=" + p.getName());
        p.sendMessage(ms.footer((byte) 10, "§a"));
    }

    private void buyRank(final Player p, final String rank, final String color) {
        p.sendMessage(ms.header((byte) 11, color));
        ms.chatClickHoverLink(p, "  " + color + "§l" + rank, color + "§o" + ms.getLang("benefits.hoverforranks"), "https://shop.muxcraft.eu/?ign=" + p.getName());
        p.sendMessage(" ");
        ms.chatClickHoverLink(p, "  " + String.format(ms.getLang("benefits.clickforranks"), color), color + "§o" + ms.getLang("benefits.hoverforranks"), "https://shop.muxcraft.eu/?ign=" + p.getName());
        p.sendMessage(ms.footer((byte) 11, color));
    }

    public void openPlayerMenu(final Player p, final Player pl) {
        if (ms.inGame(p) || ms.inEvent(p) || ms.inDuel(p) || ms.inWar(p) || ms.in1vs1(p)) return;
        if (p.hasMetadata("pmenu")) p.removeMetadata("pmenu", ms);
        p.setMetadata("pmenu", new FixedMetadataValue(ms, pl));
        final MuxUser u = ms.getMuxUser(pl.getName());
        final int trophies = u.getTrophies();
        final String clan = ms.getDB().getClanFromPlayer(pl.getUniqueId()), hasclan = ms.getDB().getClanFromPlayer(p.getUniqueId());
        final boolean notfriends = ms.getFriends().areFriends(p.getName(), pl.getUniqueId()) == false, inviteclan = hasclan != null && clan == null && ms.getDB().getClanRank(p.getUniqueId()) > 0;
        final Inventory pmenu = ms.getServer().createInventory(null, 27, "§0§l" + pl.getName());
        if (ms.getMuxUser(p.getName()).getSettings().showFriendsByTrophies())
            pmenu.setItem(4, ms.addGlow(ms.renameItemStack(ms.getHead(pl.getName()), "§f§l" + pl.getName(),
                    "§7Status: " + ms.getPlayerStatus(p, pl, u, true), "§7Trophäen: §b" + trophies, "§7Clan: §3" + (clan == null ? "-" : clan))));
        else
            pmenu.setItem(4, ms.addGlow(ms.renameItemStack(ms.getHead(pl.getName()), "§f§l" + pl.getName(),
                    "§7Status: " + ms.getPlayerStatus(p, pl, u, true), "§7Basewert: §b" + (ms.getBase().hasBase(pl.getUniqueId()) ? ms.getLFormat((long) ms.getBase().getFromPlayer(pl).getValue()) : "-"), "§7Clan: §3" + (clan == null ? "-" : clan))));
        final boolean alert = ms.getMuxUser(p.getName()).getSettings().getAlertFriends().contains(pl.getUniqueId());
        if (ms.getFriends().areFriends(p.getName(), pl.getUniqueId())) {
            final ItemStack alertitem = ms.renameItemStack(new ItemStack(Material.SUGAR), alert ? "§f§lEnger Freund§e ✯" : "§f§lEnger Freund", "§7Wenn dieser dann online kommt,", "§7wirst du sofort benachrichtigt.", "", alert ? "§7Klicke und §centferne §7diesen Spieler" : "§7Klicke und §amarkiere §7diesen Spieler", alert ? "§7von deinen engen Freunden." : "§7mit §e✯ §7als deinen engen Freund.");
            pmenu.setItem(8, alert ? ms.addGlow(alertitem) : alertitem);
        }
        pmenu.setItem(19, ms.renameItemStack(new ItemStack(Material.BOOK), "§b§lProfil sehen", "§7Schaue dir das Profil", "§7dieses Spielers an.", "", "§bKlicke§7, um diesen zu sehen."));
        pmenu.setItem(inviteclan ? 21 : 22, ms.renameItemStack(new ItemStack(Material.WHEAT), "§e§lHandel starten", "§7Tauscht Items und MuxCoins", "§7im fairen Deal miteinander.", "", "§eKlicke§7, um mit ihm zu handeln."));
        if (notfriends) {
            pmenu.setItem(inviteclan ? 23 : 25, ms.renameItemStack(new ItemStack(Material.CAKE), "§e§lAls Freund hinzufügen", "§7Füge hiermit diesen Spieler zu", "§7deinen MuxFreunden hinzu.", "", "§eKlicke§7, um die Anfrage zu senden."));
        } else {
            pmenu.setItem(inviteclan ? 23 : 25, ms.renameItemStack(new ItemStack(Material.WOOD_SWORD), "§6§lFreundschaftskampf starten", "§7Beginne ein Duell mit deinem Freund,", "§7ohne Trophäen zu riskieren.", "", "§6Klicke§7, um mit ihm zu kämpfen."));
        }
        if (inviteclan)
            pmenu.setItem(25, ms.renameItemStack(new ItemStack(Material.CHAINMAIL_HELMET), "§3§lZum Clan einladen", "§7Vergrößer deinen Clan, indem du", "§7diesen Spieler hineinbringst.", "", "§3Klicke§7, um ihn einzuladen."));
        final PlayerBase base = ms.getBase().getFromPlayer(p), plbase = ms.getBase().getCurrentLocations().get(pl), pbase = ms.getBase().getCurrentLocations().get(p);
        if (p.getWorld().equals(ms.getBase().getWorld()) && pbase != null && pbase.equals(base) && plbase != null && plbase.equals(base) && base.isTrusted(pl.getUniqueId()) == false && pl.isOp() == false) {
            pmenu.setItem(5, ms.renameItemStack(new ItemStack(Material.LEATHER_BOOTS), "§c§lKicken", "§7Wenn dich der Spieler stört, kannst", "§7du ihn von deiner Base entfernen.", "", "§7Klicke, um ihn von hier zu §ckicken§7."));
        }
        final InvType it = ms.getActiveInv(p.getName());
        if (it != InvType.PROFILE && it != InvType.PVP && it != InvType.PMENU) p.closeInventory();
        p.openInventory(pmenu);
        ms.setActiveInv(p.getName(), InvType.PMENU);
    }

    public void handlePlayerMenuInventory(final Player p, final ItemStack i, final String title) {
        final Material m = i.getType();
        final Player pl = ms.getServer().getPlayer(ChatColor.stripColor(title));
        if (pl == null) {
            p.closeInventory();
            ms.showItemBar(p, ms.hnotonline);
            return;
        }
        if (m == Material.WHEAT) {
            p.performCommand("trade " + pl.getName());
        } else if (m == Material.CHAINMAIL_HELMET) {
            p.performCommand("c einladen " + pl.getName());
        } else if (m == Material.CAKE) {
            p.performCommand("friend " + pl.getName());
        } else if (m == Material.SUGAR) {
            final Set<UUID> alertfriends = ms.getMuxUser(p.getName()).getSettings().getAlertFriends();
            if (alertfriends.remove(pl.getUniqueId()) == false) {
                alertfriends.add(pl.getUniqueId());
            }
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.6F);
            openPlayerMenu(p, pl);
            return;
        } else if (m == Material.LEATHER_BOOTS) {
            final PlayerBase base = ms.getBase().getFromPlayer(p), plbase = ms.getBase().getCurrentLocations().get(pl), pbase = ms.getBase().getCurrentLocations().get(p);
            if (p.getWorld().equals(ms.getBase().getWorld()) && pbase != null && pbase.equals(base) && plbase != null && plbase.equals(base) && base.isTrusted(pl.getUniqueId()) == false) {
                ms.forcePlayer(pl, ms.getGameSpawn());
                ms.showItemBar(p, "§fDer Spieler wurde aus deiner Base §cgekickt§f.");
                pl.sendMessage("§a§lMuxBase>§c Du wurdest aus der Base gekickt.");
            }
        } else if (m == Material.WOOD_SWORD) {
            if (p.hasMetadata("sfriends")) p.removeMetadata("sfriends", ms);
            if (p.hasMetadata("sclans")) p.removeMetadata("sclans", ms);
            if (p.hasMetadata("spmenu")) p.removeMetadata("spmenu", ms);
            p.setMetadata("spmenu", new FixedMetadataValue(ms, pl.getName()));
            ms.getPvP().openDuelInv(p, 0, 0);
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            return;
        } else if (m == Material.IRON_SWORD) {
            p.performCommand("duel " + pl.getName());
        } else if (m == Material.BOOK) {
            ms.getProfiles().showPlayerProfile(p, pl.getName(), "Spieler");
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            return;
        } else {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        }
        p.closeInventory();
    }

    private void handlePTimeInv(final Player p, final Material m) {
        switch (m) {
            case MAGMA_CREAM:
                p.setPlayerTime(0L, false);
                ms.showItemBar(p, "§fDeine Zeit wurde auf §f'§eTag§f' §fgesetzt.");
                break;
            case SNOW_BALL:
                p.setPlayerTime(18000L, false);
                ms.showItemBar(p, "§fDeine Zeit wurde auf §f'§8Nacht§f' §fgesetzt.");
                break;
            case SLIME_BALL:
                p.resetPlayerTime();
                ms.showItemBar(p, "§fDeine Zeit wurde §azurückgesetzt§f.");
                break;
            case WATCH:
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            case ITEM_FRAME:
                ms.getMenu().getBenefits().openPremiumInv(p);
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                return;
            default:
                break;
        }
        p.closeInventory();
        p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.5F, 1F);
    }

    private void handleSpawnerInv(final Player p, final ItemStack i) {
        final String pname = p.getName();
        final CreatureSpawner sp = spawner.get(pname);
        if (sp.getBlock().getType() == Material.MOB_SPAWNER) {
            final CreatureSpawner last = lastspawnerdelay.put(pname, sp);
            sp.setSpawnedType(EntityType.fromId(i.getDurability()));
            sp.update(true);
            if (NACHOSPIGOT == false && last != null) {
                final int spawnDelay = last.getDelay(), tickDelay = (int) NMSReflection.getObject(MobSpawnerAbstract.class, "tickDelay", ((CraftCreatureSpawner) last).getTileEntity().getSpawner());
                NMSReflection.setObject(MobSpawnerAbstract.class, "tickDelay", ((CraftCreatureSpawner) sp).getTileEntity().getSpawner(), tickDelay);
                sp.setDelay(spawnDelay);
            }
            ms.showItemBar(p, String.format(ms.getLang("changedspawner"), ChatColor.stripColor(i.getItemMeta().getDisplayName())));
            p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.5F, 1F);
        }
        spawner.remove(pname);
        p.closeInventory();
    }

    private void handleConfirmInv(final Player p, final ItemStack i) {
        final ConfirmInventory.ConfirmActions ca = confirminvs.get(p.getName());
        if (ca == null) {
            p.closeInventory();
        } else if (i.getDurability() == 5) {
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 0.7F);
            if (ca.confirmaction != null) ca.confirmaction.call(p);
        } else if (i.getType() == Material.STAINED_CLAY) {
            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
            if (ca.cancelaction != null) ca.cancelaction.call(p);
        } else {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        }
        confirminvs.remove(p.getName());
    }

    private void handleAnvilInv(final Player p, final ItemStack i, final Inventory inv, final int rawslot) {
        if (rawslot == 2) {
            final String output = i.hasItemMeta() ? i.getItemMeta().getDisplayName() : null;
            if (output == null) return;
            final AnvilAction action = anvils.remove(p.getName());
            inv.clear();
            p.closeInventory();
            p.setExp(p.getExp());
            action.call(output, p);
            return;
        }
        p.updateInventory();
    }

    private void setup() {
        final Server sr = ms.getServer();
        spawnerinv = sr.createInventory(null, 54, ms.getLang("cmd.spawnerinv"));
        spawnerinv.setItem(14, ms.renameItemStack(new SpawnEgg(EntityType.IRON_GOLEM).toItemStack(1), "§7§l" + ms.getLang("irongolem"), "", ms.getLang("clickirongolem")));
        spawnerinv.setItem(15, ms.renameItemStack(new SpawnEgg(EntityType.PIG).toItemStack(1), "§d§l" + ms.getLang("pig"), "", ms.getLang("clickpig")));
        spawnerinv.setItem(16, ms.renameItemStack(new SpawnEgg(EntityType.SHEEP).toItemStack(1), "§f§l" + ms.getLang("sheep"), "", ms.getLang("clicksheep")));
        spawnerinv.setItem(23, ms.renameItemStack(new SpawnEgg(EntityType.COW).toItemStack(1), "§7§l" + ms.getLang("cow"), "", ms.getLang("clickcow")));
        spawnerinv.setItem(24, ms.renameItemStack(new SpawnEgg(EntityType.CHICKEN).toItemStack(1), "§c§l" + ms.getLang("chicken"), "", ms.getLang("clickchicken")));
        spawnerinv.setItem(25, ms.renameItemStack(new SpawnEgg(EntityType.MUSHROOM_COW).toItemStack(1), "§c§l" + ms.getLang("mushroomcow"), "", ms.getLang("clickmushroomcow")));
        spawnerinv.setItem(32, ms.renameItemStack(new SpawnEgg(EntityType.OCELOT).toItemStack(1), "§e§l" + ms.getLang("ocelot"), "", ms.getLang("clickocelot")));
        spawnerinv.setItem(33, ms.renameItemStack(new SpawnEgg(EntityType.HORSE).toItemStack(1), "§6§l" + ms.getLang("horse"), "", ms.getLang("clickhorse")));
        spawnerinv.setItem(34, ms.renameItemStack(new SpawnEgg(EntityType.WOLF).toItemStack(1), "§f§l" + ms.getLang("wolf"), "", ms.getLang("clickwolf")));
        spawnerinv.setItem(42, ms.renameItemStack(new SpawnEgg(EntityType.RABBIT).toItemStack(1), "§6§l" + ms.getLang("rabbit"), "", ms.getLang("clickrabbit")));

        spawnerinv.setItem(10, ms.renameItemStack(new SpawnEgg(EntityType.SKELETON).toItemStack(1), "§7§l" + ms.getLang("skeleton"), "", ms.getLang("clickskeleton")));
        spawnerinv.setItem(11, ms.renameItemStack(new SpawnEgg(EntityType.SPIDER).toItemStack(1), "§4§l" + ms.getLang("spider"), "", ms.getLang("clickspider")));
        spawnerinv.setItem(12, ms.renameItemStack(new SpawnEgg(EntityType.ZOMBIE).toItemStack(1), "§3§l" + ms.getLang("zombie"), "", ms.getLang("clickzombie")));
        spawnerinv.setItem(19, ms.renameItemStack(new SpawnEgg(EntityType.SLIME).toItemStack(1), "§a§l" + ms.getLang("slime"), "", ms.getLang("clickslime")));
        spawnerinv.setItem(20, ms.renameItemStack(new SpawnEgg(EntityType.MAGMA_CUBE).toItemStack(1), "§6§l" + ms.getLang("magmacube"), "", ms.getLang("clickmagmacube")));
        spawnerinv.setItem(21, ms.renameItemStack(new SpawnEgg(EntityType.BLAZE).toItemStack(1), "§e§l" + ms.getLang("blaze"), "", ms.getLang("clickblaze")));
        spawnerinv.setItem(28, ms.renameItemStack(new SpawnEgg(EntityType.WITCH).toItemStack(1), "§2§l" + ms.getLang("witch"), "", ms.getLang("clickwitch")));
        spawnerinv.setItem(29, ms.renameItemStack(new SpawnEgg(EntityType.PIG_ZOMBIE).toItemStack(1), "§2§l" + ms.getLang("zombiepigman"), "", ms.getLang("clickzombiepigman")));
        spawnerinv.setItem(30, ms.renameItemStack(new SpawnEgg(EntityType.ENDERMAN).toItemStack(1), "§8§l" + ms.getLang("enderman"), "", ms.getLang("clickenderman")));
        spawnerinv.setItem(38, ms.renameItemStack(new SpawnEgg(EntityType.CAVE_SPIDER).toItemStack(1), "§9§l" + ms.getLang("cavespider"), "", ms.getLang("clickcavespider")));
        ptimeinv = sr.createInventory(null, 45, ms.getLang("cmd.ptimeinv"));
        ptimeinv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        ptimeinv.setItem(4, ms.renameItemStack(new ItemStack(Material.WATCH), ms.getLang("cmd.ptime"), ms.getLang("cmd.ptimelore3"), ms.getLang("cmd.ptimelore4")));
        ptimeinv.setItem(19, ms.renameItemStack(new ItemStack(Material.MAGMA_CREAM), ms.getLang("cmd.ptimeday"), "", ms.getLang("cmd.ptimelore"), ms.getLang("cmd.ptimelore1")));
        ptimeinv.setItem(22, ms.renameItemStack(new ItemStack(Material.SNOW_BALL), ms.getLang("cmd.ptimenight"), "", ms.getLang("cmd.ptimelore"), ms.getLang("cmd.ptimelore2")));
        ptimeinv.setItem(25, ms.renameItemStack(new ItemStack(Material.SLIME_BALL), "§a§lStandard", "", "§7Klicke, um die Zeit", "§azurückzusetzen§7."));
        donateinvgems = sr.createInventory(null, 27, "§0§lMuxVorteile§0 | Gems");
        donateinvgems.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        donateinvgems.setItem(4, ms.renameItemStack(new ItemStack(Material.EMERALD), "§a§lGems", "§7Mit dieser Währung kannst du unter", "§7anderem alles im Gemshop kaufen", "§7oder gegen MuxCoins umtauschen."));
        donateinvgems.setItem(19, ms.renameItemStack(new ItemStack(Material.CHEST), "§a§l1.000 Gems", "§7Preis: §f4.99 Euro", "", "§7Klicke, um mehr Infos zu", "§7diesem Paket zu erhalten."));
        donateinvgems.setItem(21, ms.renameItemStack(new ItemStack(Material.CHEST, 2), "§a§l2.200 Gems", "§7Preis: §f9.99 Euro", "", "§7Klicke, um mehr Infos zu", "§7diesem Paket zu erhalten."));
        donateinvgems.setItem(23, ms.renameItemStack(new ItemStack(Material.CHEST, 5), "§a§l5.500 Gems", "§7Preis: §f24.99 Euro", "", "§7Klicke, um mehr Infos zu", "§7diesem Paket zu erhalten."));
        donateinvgems.setItem(25, ms.renameItemStack(new ItemStack(Material.CHEST, 12), "§a§l12.000 Gems§7 + §e§lMuxCoins", "§7Preis: §f49.99 Euro", "", "§7Klicke, um mehr Infos zu", "§7diesem Paket zu erhalten."));
    }

    public enum InvType {
        PLAYERCHEST, WARPS, EXTRAS, TRASH, ARMOR, SPAWNER, CONFIRM, PVP, FRIENDS, PROFILE, MUXANVIL, PTIME, PWEATHER, HOMES, CLANS, BENEFITS, TRADE, MUXCHESTS,
        ADMIN, HOLOGRAMS, SHOP, SHOPSEARCH, EVENT, ULTRA, GOLD, X, MACRO, USERCONTROL, CLANWAR, CASINO, PMENU, MENU, CREATOR, TEAM, REPORTS, MARKET, MESSAGES, SUPPORT,
        NOINTERACT, HISTORY, BASES, FILTER, MINECAPTCHA, GEMSHOP, AFFILIATE, EARN, POLLS, FEEDBACK, TIMINGS
    }
}