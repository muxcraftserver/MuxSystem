package me.muxteam.ranks;

import me.muxteam.basic.MuxAnvil;
import me.muxteam.extras.MuxExtraCommands;
import me.muxteam.muxsystem.MuxInventory;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class MuxBenefits {
    private final Map<Material, BenefitItem> benefitItems = new EnumMap<>(Material.class);
    private final List<BenefitItem> sortedBenefitItems;
    private final MuxSystem ms;

    public MuxBenefits(final MuxSystem ms) {
        this.ms = ms;
        registerBenefitItems();
        this.sortedBenefitItems = new ArrayList<>(benefitItems.values());
        this.sortedBenefitItems.sort(Comparator.comparingInt(BenefitItem::priority));
    }

    public void openPremiumInv(final Player p) {
        final Inventory inv = ms.getServer().createInventory(null, 45, "§0§lMuxMenü§0 | Vorteile");
        inv.setItem(0, ms.renameItemStack(new ItemStack(Material.ITEM_FRAME), ms.getLang("back")));
        inv.setItem(4, ms.renameItemStack(new ItemStack(Material.DIAMOND), "§b§lVorteile", "§7Alle Funktionen, die dein Rang", "§7bietet, siehst du hierdurch."));
        final List<ItemStack> toShow = new ArrayList<>();
        for (final BenefitItem benefitItem : sortedBenefitItems) {
            if (benefitItem.canShow(p)) {
                toShow.add(benefitItem.getItem(p));
                benefitItem.onDisplay(p);
            }
        }
        if (toShow.size() < 8) {
            int middle = 22;
            if (toShow.size() == 2) {
                inv.setItem(20, toShow.get(0));
                inv.setItem(24, toShow.get(1));
            } else if (toShow.size() == 3) {
                inv.setItem(19, toShow.get(0));
                inv.setItem(22, toShow.get(1));
                inv.setItem(25, toShow.get(2));
            } else if (toShow.size() == 4) {
                inv.setItem(19, toShow.get(0));
                inv.setItem(21, toShow.get(1));
                inv.setItem(23, toShow.get(2));
                inv.setItem(25, toShow.get(3));
            } else if (toShow.size() == 6) {
                inv.setItem(19, toShow.get(0));
                inv.setItem(20, toShow.get(1));
                inv.setItem(21, toShow.get(2));
                inv.setItem(23, toShow.get(3));
                inv.setItem(24, toShow.get(4));
                inv.setItem(25, toShow.get(5));
            } else if (toShow.size() == 1) {
                inv.setItem(middle, toShow.get(0));
            } else {
                int right;
                int left = right = (toShow.size() - 1) / 2;
                right = right + middle + 1;
                left = middle - left;
                int index = 0;
                for (int slot = left; slot < right; slot++) {
                    ItemStack icon = toShow.get(index);
                    inv.setItem(slot, icon);
                    index++;
                }
            }
        } else {
            int slot = 18;
            for (ItemStack itemStack : toShow) {
                inv.setItem(slot, itemStack);
                slot++;
            }
        }
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.MENU && it != MuxInventory.InvType.PTIME && it != MuxInventory.InvType.GOLD && it != MuxInventory.InvType.X) p.closeInventory();
        p.openInventory(inv);
        ms.setActiveInv(p.getName(), MuxInventory.InvType.MENU);
    }

    public boolean handleKitCommand(final Player p) {
        if (p.hasPermission("muxsystem.vip")) openPremiumInv(p);
        else p.performCommand("shop");
        return true;
    }

    public void onClosePremium(final Player player) {
        if (player.hasMetadata("menunoclear") == false) {
            sortedBenefitItems.forEach(benefitItem -> benefitItem.onClose(player));
        }
        player.removeMetadata("menunoclear", ms);
    }

    public void handleInventory(final Player p, final ItemStack i, final boolean rightclick) {
        final Material m = i.getType();
        if (m == Material.ITEM_FRAME) {
            ms.getMenu().handleCommand(p, new String[0]);
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            return;
        } else if (m == Material.DIAMOND) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
            return;
        }
        final BenefitItem item = benefitItems.get(m);
        if (item != null) {
            final List<String> lore = i.getItemMeta().getLore();
            if (lore.isEmpty() == false && lore.get(lore.size() - 1).contains("§c") && m != Material.STAINED_CLAY) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            if (item.playSoundOnClick()) p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            item.onClick(p, rightclick);
            if (ms.getInv().getInv(p.getName()) == MuxInventory.InvType.MENU) {
                if (item.closeOnClick())
                    p.closeInventory();
                else if (item.dontUpdateOnClick() == false)
                    openPremiumInv(p);
            }
        }
    }
    public void handleBelowInventory(final Player p, final ItemStack item, final InventoryClickEvent e) {
        final Material m = item.getType();
        if (p.hasMetadata("menuclickinvfill") && item.getType() == Material.GLASS_BOTTLE) {
            item.setType(Material.POTION);
            p.updateInventory();
            p.playSound(p.getLocation(), Sound.SPLASH, 1F, 2F);
            if (ms.getMaterialCount(p.getInventory(), Material.GLASS_BOTTLE, (byte) 0) < 1) {
                openPremiumInv(p);
            }
            return;
        }
        if (p.hasMetadata("menuclickinvhat") && (m.isBlock() || m == Material.SKULL_ITEM)) {
            if (p.getInventory().getHelmet() != null) {
                final ItemStack helm = p.getInventory().getHelmet();
                p.getInventory().setHelmet(e.getCurrentItem());
                e.setCurrentItem(helm);
                p.updateInventory();
            } else {
                p.getInventory().setHelmet(e.getCurrentItem());
                e.setCurrentItem(new ItemStack(Material.AIR));
                p.updateInventory();
            }
            p.playSound(p.getLocation(), Sound.LAVA_POP, 1F, 2F);
            new BukkitRunnable() {
                @Override
                public void run() {
                    p.removeMetadata("menuclickinvhat", ms);
                    openPremiumInv(p);
                }
            }.runTaskLater(ms, 1L);
            return;
        }
        final MuxExtraCommands extracmds = ms.getExtraCMDs();
        if (p.hasMetadata("menuclickinvsmelt") && extracmds.canSmeltItem(m)) {
            e.setCurrentItem(extracmds.getSmeltedItemStack(item));
            p.updateInventory();
            p.playSound(p.getLocation(), Sound.FIZZ, 1F, 1F);
            if (getSlotsOfSmeltableItems(ms.getExtraCMDs(), p) == 0)
                openPremiumInv(p);
            return;
        }
        if (p.hasMetadata("menuclickinvscook") && extracmds.isCookable(item)) {
            e.setCurrentItem(extracmds.getCookedItemStack(item));
            p.updateInventory();
            p.playSound(p.getLocation(), Sound.FIZZ, 1F, 1F);
            if (getSlotsOfCookableItems(ms.getExtraCMDs(), p) == 0)
                openPremiumInv(p);
        }
    }


    private void registerBenefitItems() {
        final MuxExtraCommands extracmds = ms.getExtraCMDs();
        benefitItems.put(Material.ENDER_PEARL, new BenefitItemBuilder().permission("muxsystem.tpahere").itemStack(p -> ms.renameItemStack(new ItemStack(Material.ENDER_PEARL), "§b§lTeleportieren",
                        "§7Gelange hiermit zu anderen Spielern", "§7oder lade sie ein, zu dir zu kommen.", "", "§7Linksklick, um §bdich zu teleportieren§7.", "§7Rechtsklick, um §bandere einzuladen§7."))
                .clickAction((p, rightclick) -> createSimpleAnvilAction(p, "Spieler: ", rightclick ? "tpahere" : "tpa", false))
                .priority(0)
                .build());
        benefitItems.put(Material.IRON_DOOR, new BenefitItemBuilder().permission("muxsystem.back").itemStack(p -> ms.renameItemStack(new ItemStack(Material.IRON_DOOR), "§b§lZurück teleportieren",
                        "§7Mit dieser Funktion kannst du zum", "§7vorherigen Ort zurück gelangen.", "", "§7Klicke§7, um dich zu §bteleportieren§7."))
                .clickAction((p, rightclick) -> {
                    p.performCommand("back");
                    p.closeInventory();
                })
                .playSoundOnClick(false)
                .priority(1)
                .closeOnClick(true)
                .build());
        benefitItems.put(Material.GRASS, new BenefitItemBuilder().permission("muxsystem.stack").itemStack(p -> ms.renameItemStack(new ItemStack(Material.GRASS, 64), "§b§lItems stacken",
                        "§7Stapel all deine Items, um", "§7für Ordnung zu sorgen.", "", (canStackItems(p) ? "§7Klicke§7, um sie zu §bstapeln§7." : "§cAlle Items sind gestapelt.")))
                .clickAction((p, rightclick) -> p.performCommand("stack"))
                .playSoundOnClick(false)
                .priority(2)
                .build());
        benefitItems.put(Material.INK_SACK, new BenefitItemBuilder().permission("muxsystem.vip").itemStack(p -> ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 6), "§b§lFarben sehen",
                        "§7Mithilfe von '&' kannst du", "§7im Chat farbig schreiben", "", "§7Klicke für die §bÜbersicht§7."))
                .clickAction((p, rightclick) -> {
                    p.closeInventory();
                    p.performCommand("farben");
                })
                .priority(3)
                .build());
        benefitItems.put(Material.APPLE, new BenefitItemBuilder().permission("muxsystem.feed").itemStack(p -> ms.renameItemStack(new ItemStack(Material.APPLE), "§b§lHunger stillen",
                        "§7Esse nun, wann immer du möchtest,", "§7um nie wieder hungern zu müssen.", "", "§7Klicke, um dich sofort zu §bsättigen§7."))
                .clickAction((p, rightclick) -> {
                    if (ms.checkGeneralCooldown(p.getName(), "HUNGERSPAM", 500L, true)) {
                        p.playSound(p.getLocation(), Sound.FIRE_IGNITE, 1F, 1F);
                        return;
                    }
                    p.performCommand("feed");
                })
                .priority(4)
                .playSoundOnClick(false)
                .build());
        benefitItems.put(Material.WATCH, new BenefitItemBuilder().permission("muxsystem.ptime").itemStack(p -> ms.renameItemStack(new ItemStack(Material.WATCH), "§b§lZeit umstellen",
                        "§7Ob Tag oder Nacht, wähle", "§7deine eigene Uhrzeit aus.", "", "§7Klicke, um sie zu §bändern§7."))
                .clickAction((p, rightclick) -> p.performCommand("ptime"))
                .priority(5)
                .build());
        benefitItems.put(Material.REDSTONE, new BenefitItemBuilder().permission("muxsystem.heal").itemStack(p -> {
                    final String time = ms.getKits().checkKitCooldown(p.getUniqueId(), "Heal", false);
                    return ms.renameItemStack(new ItemStack(Material.REDSTONE), "§b§lHeilen", "§7In Notfällen kannst du dich", "§7damit vor dem Tod retten.", "",
                            (time != null && p.hasPermission("muxsystem.heal.forever") == false ? String.format("§cWarte §6%s§c zum heilen.", time) : "§7Klicke, um dich zu §bheilen§7."));
                }).clickAction((p, rightclick) -> p.performCommand("heal"))
                .playSoundOnClick(false)
                .priority(6)
                .build());
        benefitItems.put(Material.DIAMOND_CHESTPLATE, new BenefitItemBuilder().permission("muxsystem.elite").itemStack(p -> {
                    final boolean fulltrusted = ms.isFullTrusted(p.getName()), fullinv = p.getInventory().firstEmpty() == -1;
                    String time = ms.getKits().checkKitCooldown(p.getUniqueId(), "Tools", false);
                    time = ms.checkLimit(p.getName(), "Tools", 4, false) ? "bis morgen" : time;
                    return ms.addLore(ms.setLore(ms.renameItemStack(new ItemStack(Material.DIAMOND_CHESTPLATE), "§b§lTools"), "§7Verzaubere diese Diamantitems für", "§7den Kampf auf sehr hohem Level.", ""),
                            fullinv ? new String[] {ms.getLang("invfull")} : (time == null || fulltrusted ? new String[] { ms.getLang("kit.clickforkit") } : new String[] {String.format("§cWarte §6%s§c für diesen Kit.", time)}));
                }).clickAction((p, rightclick) -> {
                    if (ms.getKits().giveKit(p, Material.DIAMOND_CHESTPLATE, true) == false) {
                        p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                        return;
                    }
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.5F, 1F);
                    ms.showItemBar(p, String.format(ms.getLang("kits.receivedkit"), "§bTools"));
                })
                .playSoundOnClick(false)
                .priority(7)
                .build());
        benefitItems.put(Material.WORKBENCH, new BenefitItemBuilder().permission("muxsystem.workbench").itemStack(p -> ms.renameItemStack(new ItemStack(Material.WORKBENCH), "§b§lWerkbank",
                        "§7Diese hilft, wenn man spontan", "§7unterwegs craften möchte.", "", "§7Klicke, um diese zu §bbenutzen§7."))
                .clickAction((p, rightclick) -> p.performCommand("workbench"))
                .priority(8)
                .build());
        benefitItems.put(Material.IRON_PLATE, new BenefitItemBuilder().permission("muxsystem.top").itemStack(p -> ms.renameItemStack(new ItemStack(Material.IRON_PLATE), "§b§lOben teleportieren",
                        "§7Hier bringst du dich selbst zu dem", "§7höchsten Punkt, der über dir ist.", "",
                        ms.getBase().isTrustedAtLocation(p) == false ? "§cDas geht nur in deinen Basen." : "§7Klicke, um dich zu §bteleportieren§7."))
                .clickAction((p, rightclick) -> {
                    p.performCommand("top");
                    p.closeInventory();
                })
                .playSoundOnClick(false)
                .priority(9)
                .build());
        benefitItems.put(Material.EYE_OF_ENDER, new BenefitItemBuilder().permission("muxsystem.invsee").itemStack(p -> ms.renameItemStack(new ItemStack(Material.EYE_OF_ENDER), "§b§lInventar sehen",
                        "§7Beobachte, welche Items andere", "§7Spieler derzeit mit sich tragen.", "", "§7Klicke§7, um ein §bInventar zu sehen§7."))
                .clickAction((p, rightclick) -> createSimpleAnvilAction(p, "Spieler: ", "invsee", true))
                .priority(10)
                .build());
        benefitItems.put(Material.ENDER_CHEST, new BenefitItemBuilder().permission("muxsystem.enderchest").itemStack(p -> ms.renameItemStack(new ItemStack(Material.ENDER_CHEST),
                        ms.getExtras().hasExtraEnderChest(p.getUniqueId()) ? "§5§lErweiterte Enderkiste" : "§b§lEnderkiste",
                        "§7Lagere hier deine Sachen ab oder", "§7schau nach, was andere besitzen.", "", "§7Linksklick, um §bdeine zu öffnen§7.", "§7Rechtsklick, um §bandere zu öffnen§7."))
                .clickAction((p, rightclick) -> {
                    if (rightclick) {
                        p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
                        createSimpleAnvilAction(p, "Spieler: ", "enderchest", true);
                    } else {
                        p.playSound(p.getLocation(), Sound.CHEST_OPEN, 1F, 1F);
                        p.performCommand("enderchest");
                    }
                })
                .priority(11)
                .playSoundOnClick(false)
                .build());
        benefitItems.put(Material.LEATHER_HELMET, new BenefitItemBuilder().permission("muxsystem.hat").itemStack(p ->
                        ms.renameItemStack(p.hasMetadata("menuclickinvhat") && hasBlockForHatInInventory(p) ? ms.addGlow(new ItemStack(Material.LEATHER_HELMET)) : new ItemStack(Material.LEATHER_HELMET), "§b§lHut tragen",
                                p.hasMetadata("menuclickinvhat") ? new String[] {"§7Du kannst dir ein passendes Item", "§7auf deinem eigenen Kopf setzen§7.", "", "§aKlicke auf irgendein Block §a§lunten", "§aim Inventar, um es aufzusetzen."} :
                                        new String[] { "§7Du kannst dir ein passendes Item", "§7auf deinem eigenen Kopf setzen§7.", "", (hasBlockForHatInInventory(p) == false) ? "§cKein passender Block im Inventar." : "§7Klicke, um einen Hut §bauszuwählen§7."}))
                .clickAction((p, rightclick) -> {
                    if (p.hasMetadata("menuclickinvhat")) {
                        p.removeMetadata("menuclickinvhat", ms);
                        openPremiumInv(p);
                        return;
                    }
                    p.removeMetadata("menunoclear", ms);
                    onClosePremium(p);
                    p.setMetadata("menuclickinvhat", new FixedMetadataValue(ms, true));
                    p.setMetadata("menunoclear", new FixedMetadataValue(ms, true));
                    openPremiumInv(p);
                    p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
                })
                .dontUpdateOnClick(true)
                .onDisplayAndClose(p -> {
                }, p -> p.removeMetadata("menuclickinvhat", ms))
                .priority(12)
                .playSoundOnClick(false)
                .build());
        benefitItems.put(Material.BREWING_STAND_ITEM, new BenefitItemBuilder().permission("muxsystem.epic").itemStack(p -> {
                    final boolean fulltrusted = ms.isFullTrusted(p.getName()), fullinv = p.getInventory().firstEmpty() == -1;
                    final String time = ms.getKits().checkKitCooldown(p.getUniqueId(), "Potion", false);
                    return ms.addLore(ms.setLore(ms.renameItemStack(new ItemStack(Material.BREWING_STAND_ITEM), "§b§lPotions"), "§7Die Tränke, die du hier erhältst, sind", "§7im Kampf außerordentlich hilfreich.", ""),
                            fullinv ? new String[] {ms.getLang("invfull")} : (time == null || fulltrusted ? new String[] { ms.getLang("kit.clickforkit") } : new String[] {String.format("§cWarte §6%s§c für diesen Kit.", time)}));
                })
                .clickAction((p, rightclick) -> {
                    if (ms.getKits().giveKit(p, Material.BREWING_STAND_ITEM, true) == false) {
                        p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                        return;
                    }
                    p.playSound(p.getLocation(), Sound.ORB_PICKUP, 0.5F, 1F);
                    ms.showItemBar(p, String.format(ms.getLang("kits.receivedkit"), "§bPotions"));
                })
                .playSoundOnClick(false)
                .priority(13)
                .build());
        benefitItems.put(Material.STAINED_CLAY, new BenefitItemBuilder().permission("muxsystem.ultra").itemStack(p -> ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) 14), "§c§lUltra",
                        "§7Hiermit gelangst du direkt zu", "§7dem exklusiven Ultra-Menü.", "", "§cKlicke§7, um dieses zu öffnen."))
                .clickAction((p, rightclick) -> p.performCommand("ultra"))
                .priority(14)
                .build());
        benefitItems.put(Material.GOLD_BLOCK, new BenefitItemBuilder().itemStack(p -> ms.renameItemStack(new ItemStack(Material.GOLD_BLOCK), "§e§lGold",
                        "§7Hiermit gelangst du direkt zu", "§7dem exklusiven Gold-Menü.", "", "§eKlicke§7, um dieses zu öffnen."))
                .clickAction((p, rightclick) -> p.performCommand("gold"))
                .canShow(p -> ms.hasGold(p.getUniqueId()) && ms.hasX(p.getUniqueId()) == false)
                .priority(15)
                .build());
        benefitItems.put(Material.SEA_LANTERN, new BenefitItemBuilder().itemStack(p -> ms.renameItemStack(new ItemStack(Material.SEA_LANTERN), "§f§lX",
                        "§7Hiermit gelangst du direkt zu", "§7dem exklusiven X-Menü.", "", "§fKlicke§7, um dieses zu öffnen."))
                .clickAction((p, rightclick) -> p.performCommand("x"))
                .canShow(p -> ms.hasX(p.getUniqueId()))
                .priority(15)
                .build());
        benefitItems.put(Material.ANVIL, new BenefitItemBuilder().itemStack(p -> ms.renameItemStack(new ItemStack(Material.ANVIL), "§6§lAmboss",
                        "§7Jederzeit und überall ist dieser", "§7für Reparatur zu gebrauchen.", "", "§7Klicke§7, um diesen zu §6benutzen§7."))
                .clickAction((p, rightclick) -> p.performCommand("anvil"))
                .canShow(p -> ms.getExtras().hasntCommand(p.getUniqueId(), "anvil") == false)
                .priority(16)
                .build());
        benefitItems.put(Material.ENCHANTMENT_TABLE, new BenefitItemBuilder().itemStack(p -> ms.renameItemStack(new ItemStack(Material.ENCHANTMENT_TABLE), "§6§lZaubertisch",
                        "§7Damit du nicht ständig zum Spawn", "§7musst, benutze ihn direkt hier.", "", "§7Klicke§7, um diesen zu §6verwenden§7."))
                .clickAction((p, rightclick) -> p.performCommand("etable"))
                .canShow(p -> ms.getExtras().hasntCommand(p.getUniqueId(), "enchant") == false)
                .priority(17)
                .build());
        benefitItems.put(Material.COOKED_BEEF, new BenefitItemBuilder().itemStack(p -> {
                    final int cookableSlots = getSlotsOfCookableItems(ms.getExtraCMDs(), p);
                    return ms.renameItemStack(cookableSlots > 0 && p.hasMetadata("menuclickinvscook") ? ms.addGlow(new ItemStack(Material.COOKED_BEEF)) : new ItemStack(Material.COOKED_BEEF), "§6§lKochen",
                            cookableSlots == 0 ? new String[] { "§7Erhöhe den Wert deines Essens,", "§7indem du die Lebensmittel kochst.", "", "§cDu hast leider nichts kochbares."} :
                                    p.hasMetadata("menuclickinvscook") == false ? new String[] { "§7Erhöhe den Wert deines Essens,", "§7indem du die Lebensmittel kochst.", "", "§7Klicke, um dieses nun zu §6kochen§7."} :
                                            new String[] { "§7Erhöhe den Wert deines Essens,", "§7indem du die Lebensmittel kochst.", "", "§aKlicke nun auf alle kochbaren", "§aItems §a§lunten§a in dem Inventar."});
                }).canShow(p -> ms.getExtras().hasntCommand(p.getUniqueId(), "cook") == false)
                .onDisplayAndClose(p -> {
                }, p -> p.removeMetadata("menuclickinvscook", ms))
                .clickAction((p, rightclick) -> {
                    if (p.hasMetadata("menuclickinvscook")) {
                        p.removeMetadata("menuclickinvscook", ms);
                        openPremiumInv(p);
                        return;
                    }
                    p.removeMetadata("menunoclear", ms);
                    onClosePremium(p);
                    p.setMetadata("menunoclear", new FixedMetadataValue(ms, true));
                    p.setMetadata("menuclickinvscook", new FixedMetadataValue(ms, true));
                    openPremiumInv(p);
                    p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
                })
                .dontUpdateOnClick(true)
                .priority(18)
                .playSoundOnClick(false)
                .build());
        benefitItems.put(Material.CHAINMAIL_CHESTPLATE, new BenefitItemBuilder().itemStack(p -> ms.renameItemStack(new ItemStack(Material.CHAINMAIL_CHESTPLATE), "§6§lRüstung sehen",
                        "§7Zeige dir alle Rüstungsteile", "§7anderer Spieler hiermit an.", "", "§7Klicke§7, um diese zu §6sehen§7."))
                .canShow(p -> ms.getExtras().hasntCommand(p.getUniqueId(), "bodysee") == false)
                .clickAction((p, rightclick) -> this.createSimpleAnvilAction(p, "Spieler: ", "bodysee", true))
                .priority(19)
                .build());
        benefitItems.put(Material.GOLD_INGOT, new BenefitItemBuilder().itemStack(p -> ms.renameItemStack(new ItemStack(Material.GOLD_INGOT), "§6§lGoldswitch",
                        "§7Verwandle die gesamten Goldnuggets,", "§7die du hast direkt zu Goldbarren um.", "",
                        (ms.getMaterialCount(p.getInventory(), Material.GOLD_NUGGET, (byte) 0) > 8 ? "§7Klicke, um alle direkt zu §6craften§7." : "§cDu hast nicht genug Goldnuggets.")))
                .canShow(p -> ms.getExtras().hasntCommand(p.getUniqueId(), "goldswitch") == false)
                .clickAction((p, rightclick) -> p.performCommand("goldswitch"))
                .priority(20)
                .build());
        benefitItems.put(Material.FURNACE, new BenefitItemBuilder().itemStack(p -> {
                    final int smeltables = getSlotsOfSmeltableItems(extracmds, p);
                    return ms.renameItemStack(smeltables > 0 && p.hasMetadata("menuclickinvsmelt") ? ms.addGlow(new ItemStack(Material.FURNACE)) : new ItemStack(Material.FURNACE), "§6§lSchmelzen",
                            smeltables == 0 ? new String[] { "§7Ab sofort wird kein Ofen nötig", "§7sein, wenn du hiermit schmilzt.", "", "§cDu hast nichts schmelzbares."} :
                                    p.hasMetadata("menuclickinvsmelt") == false ? new String[] { "§7Ab sofort brauchst du keinen Ofen", "§7mehr, wenn du hiermit schmilzt.", "", "§7Klicke§7, um nun zu §6schmelzen§7."} :
                                            new String[] { "§7Ab sofort brauchst du keinen Ofen", "§7mehr, wenn du hiermit schmilzt.", "", "§aKlicke auf alle schmelzbaren", "§aItems §a§lunten§a in dem Inventar."});
                }).canShow(p -> ms.getExtras().hasntCommand(p.getUniqueId(), "smelt") == false)
                .clickAction((p, rightclick) -> {
                    if (p.hasMetadata("menuclickinvsmelt")) {
                        p.removeMetadata("menuclickinvsmelt", ms);
                        openPremiumInv(p);
                        return;
                    }
                    p.removeMetadata("menunoclear", ms);
                    onClosePremium(p);
                    p.setMetadata("menunoclear", new FixedMetadataValue(ms, true));
                    p.setMetadata("menuclickinvsmelt", new FixedMetadataValue(ms, true));
                    openPremiumInv(p);
                    p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
                })
                .dontUpdateOnClick(true)
                .onDisplayAndClose(p -> {
                }, p -> p.removeMetadata("menuclickinvsmelt", ms))
                .priority(21)
                .playSoundOnClick(false)
                .build());
        benefitItems.put(Material.POTION, new BenefitItemBuilder().itemStack(p -> {
                    final boolean hasGlassBottle = p.getInventory().contains(Material.GLASS_BOTTLE);
                    final ItemStack glow = new ItemStack(Material.POTION, 1, (byte) 77);
                    final ItemMeta im = glow.getItemMeta();
                    im.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
                    glow.setItemMeta(im);
                    return ms.renameItemStack(p.hasMetadata("menuclickinvfill") && hasGlassBottle ? glow : new ItemStack(Material.POTION), "§6§lGlasflaschen füllen",
                            hasGlassBottle ? p.hasMetadata("menuclickinvfill") ?
                                    new String[] {"§7Diese Funktion erleichtert es dir", "§7sehr beim Brauen von Tränken.", "", "§aKlicke auf alle Glasflaschen", "§a§lunten §ain dem Inventar." }
                                    : new String [] { "§7Diese Funktion erleichtert es dir", "§7sehr beim Brauen von Tränken.", "", "§7Klicke, um diese nun zu §6füllen§7." }
                                    : new String[] {"§7Diese Funktion erleichtert es dir", "§7sehr beim Brauen von Tränken.", "", "§cDu hast keine Glasflaschen." });
                })
                .onDisplayAndClose(p -> {
                }, p -> p.removeMetadata("menuclickinvfill", ms))
                .clickAction((p, rightclick) -> {
                    if (p.hasMetadata("menuclickinvfill")) {
                        p.removeMetadata("menuclickinvfill", ms);
                        openPremiumInv(p);
                        return;
                    }
                    p.removeMetadata("menunoclear", ms);
                    onClosePremium(p);
                    p.setMetadata("menuclickinvfill", new FixedMetadataValue(ms, true));
                    p.setMetadata("menunoclear", new FixedMetadataValue(ms, true));
                    openPremiumInv(p);
                    p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
                })
                .priority(22)
                .playSoundOnClick(false)
                .dontUpdateOnClick(true)
                .canShow(p -> ms.getExtras().hasntCommand(p.getUniqueId(), "fill") == false)
                .build());
        benefitItems.put(Material.GLASS_BOTTLE, new BenefitItemBuilder().itemStack(p -> ms.renameItemStack(new ItemStack(Material.GLASS_BOTTLE), "§6§lGlasflaschen herstellen",
                        "§7Möglichst alle Glasblöcke werden", "§7somit zu Glasflaschen gecraftet.", "", (ms.getMaterialCount(p.getInventory(), Material.GLASS, (byte) 0) > 0 ? "§7Klicke§7, um diese nun zu §6craften§7." : "§cDu hast nicht genug Glasblöcke.")))
                .clickAction((p, rightclick) -> p.performCommand("bottle"))
                .canShow(p -> ms.getExtras().hasntCommand(p.getUniqueId(), "bottle") == false)
                .priority(23)
                .build());
        benefitItems.put(Material.REDSTONE_ORE, new BenefitItemBuilder().itemStack(p -> ms.renameItemStack(new ItemStack(Material.REDSTONE_ORE), "§6§lX-Ray",
                        "§7Lasse dir alle gesamten Erze, die", "§7sich unter dir befinden, anzeigen§7.", "", "§7Klicke§7, um diese dir §6darzustellen§7."))
                .canShow(p -> ms.getExtras().hasntCommand(p.getUniqueId(), "xray") == false)
                .clickAction((p, rightclick) -> {
                    p.performCommand("xray");
                    p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
                })
                .closeOnClick(true)
                .priority(24)
                .playSoundOnClick(false)
                .build());
        benefitItems.put(Material.COMPASS, new BenefitItemBuilder().itemStack(p -> ms.renameItemStack(new ItemStack(Material.COMPASS), "§6§lRadar",
                        "§7Sollten Spieler in deiner Nähe sein,", "§7so werden sie dir direkt angezeigt.", "", "§7Klicke, um alle Spieler §6anzuzeigen§7."))
                .canShow(p -> ms.getExtras().hasntCommand(p.getUniqueId(), "near") == false)
                .clickAction((p, rightclick) -> {
                    p.performCommand("near");
                    p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
                })
                .closeOnClick(true)
                .priority(25)
                .playSoundOnClick(false)
                .build());
        benefitItems.put(Material.EXP_BOTTLE, new BenefitItemBuilder().itemStack(p -> {
                    final boolean hasBottles = ms.getMaterialCount(p.getInventory(), Material.GLASS_BOTTLE, (byte) 0) > 0, invfull = p.getInventory().firstEmpty() == -1;
                    return ms.renameItemStack(new ItemStack(Material.EXP_BOTTLE), "§6§lXP speichern", "§7Hiermit kannst du dein XP Level", "§7in einer Glasflasche speichern.", "",
                            (p.getLevel() < 1 ? "§cDu hast kein Erfahrungslevel." : invfull ? "§cDein Inventar ist voll." : hasBottles ? "§7Klicke, um dein XP zu §6speichern§7." : "§cDu hast keine Glasflaschen."));
                }).canShow(p -> ms.getExtras().hasntCommand(p.getUniqueId(), "bottlexp") == false)
                .clickAction((p, rightclick) -> {
                    if (ms.getMaterialCount(p.getInventory(), Material.GLASS_BOTTLE, (byte) 0) > 0) {
                        p.getInventory().removeItem(new ItemStack(Material.GLASS_BOTTLE));
                    }
                    final int expToSave = Math.min(p.getTotalExperience(), 5000), leftoverExp = p.getTotalExperience() - expToSave;
                    p.setExp(0);
                    p.setLevel(0);
                    p.setTotalExperience(0);
                    p.giveExp(leftoverExp);
                    final ItemStack bottle = ms.renameItemStack(new ItemStack(Material.EXP_BOTTLE), "§a§lMuxEXP", "§7Gespeicherte EXP: §a" + expToSave);
                    p.getInventory().addItem(bottle);
                    p.updateInventory();
                    p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 1F);
                })
                .priority(26)
                .playSoundOnClick(false)
                .build());
    }

    private boolean hasBlockForHatInInventory(final Player p) {
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            final ItemStack item = p.getInventory().getItem(i);
            if (item != null && item.getType() != Material.AIR && (item.getType().isBlock() || item.getType() == Material.SKULL_ITEM))
                return true;
        }
        return false;
    }

    private boolean canStackItems(final Player p) {
        final ItemStack[] items = p.getInventory().getContents();
        final short size = (short) items.length;
        boolean stackable = false;
        for (int i = 0; i < size; i++) {
            final ItemStack item = items[i];
            if (item == null || item.getAmount() <= 0 || item.getAmount() > 63) {
                continue;
            }
            int needed = 64 - item.getAmount();
            for (int j = i + 1; j < size; j++) {
                final ItemStack item2 = items[j];
                if (item2 != null && item2.getAmount() > 0) {
                    if (item.getType() == item2.getType() && item.getDurability() == item2.getDurability() && ((item.getItemMeta() == null && item2.getItemMeta() == null) || item.getItemMeta() != null && item.getItemMeta().equals(item2.getItemMeta()))) {
                        if (item2.getAmount() > needed) {
                            stackable = true;
                            break;
                        }
                        stackable = true;
                    }
                }
            }
        }
        return stackable;
    }

    private int getSlotsOfSmeltableItems(final MuxExtraCommands extracmds, final Player p) {
        byte slots = 0;
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item != null && extracmds.canSmeltItem(item.getType()))
                slots++;
        }
        return slots;
    }

    private int getSlotsOfCookableItems(final MuxExtraCommands extracmds, final Player p) {
        byte slots = 0;
        for (int i = 0; i < p.getInventory().getSize(); i++) {
            final ItemStack item = p.getInventory().getItem(i);
            if (item != null && extracmds.isCookable(item))
                slots++;
        }
        return slots;
    }

    private void createSimpleAnvilAction(final Player player, final String text, final String command, final boolean sound) {
        new MuxAnvil(ms, (input, p) -> {
            input = ms.retardCleaner(input, text);
            p.closeInventory();
            p.performCommand(command + " " + input);
            if (sound) p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        }).show(player, text);
    }

    public interface BenefitItem {

        ItemStack getItem(final Player player);

        String permissions();

        void onClick(final Player player, final Boolean rightclick);

        void onDisplay(final Player player);

        void onClose(final Player player);

        boolean closeOnClick();

        boolean dontUpdateOnClick();

        boolean playSoundOnClick();

        int priority();

        default boolean canShow(final Player p) {
            return p.hasPermission(permissions());
        }
    }

    protected class BenefitItemBuilder {
        private Function<Player, ItemStack> itemStack;
        private String permission;
        private BiConsumer<Player, Boolean> clickAction;
        private Function<Player, Boolean> canShow = null;
        private Consumer<Player> onDisplay, onClose;
        private boolean closeOnClick, dontUpdateOnClick, playSoundOnClick = true;
        private int priority;

        public BenefitItemBuilder itemStack(final Function<Player, ItemStack> itemStack) {
            this.itemStack = itemStack;
            return this;
        }

        public BenefitItemBuilder permission(final String permission) {
            this.permission = permission;
            return this;
        }

        public BenefitItemBuilder priority(final int priority) {
            this.priority = priority;
            return this;
        }

        public BenefitItemBuilder clickAction(final BiConsumer<Player, Boolean> clickAction) {
            this.clickAction = clickAction;
            return this;
        }

        public BenefitItemBuilder closeOnClick(final boolean closeOnClick) {
            this.closeOnClick = closeOnClick;
            return this;
        }

        public BenefitItemBuilder playSoundOnClick(final boolean playSoundOnClick) {
            this.playSoundOnClick = playSoundOnClick;
            return this;
        }

        public BenefitItemBuilder dontUpdateOnClick(final boolean dontUpdateOnClick) {
            this.dontUpdateOnClick = dontUpdateOnClick;
            return this;
        }

        public BenefitItemBuilder canShow(final Function<Player, Boolean> canShow) {
            this.canShow = canShow;
            return this;
        }

        public BenefitItemBuilder onDisplayAndClose(final Consumer<Player> onDisplay, final Consumer<Player> onClose) {
            this.onDisplay = onDisplay;
            this.onClose = onClose;
            return this;
        }

        public BenefitItem build() {
            if (canShow != null) {
                return new BenefitItem() {
                    @Override
                    public ItemStack getItem(final Player player) {
                        return itemStack.apply(player);
                    }

                    @Override
                    public String permissions() {
                        return permission;
                    }

                    @Override
                    public void onClick(final Player player, final Boolean rightclick) {
                        clickAction.accept(player, rightclick);
                    }

                    @Override
                    public void onDisplay(Player player) {
                        if (onDisplay != null)
                            onDisplay.accept(player);
                    }

                    @Override
                    public void onClose(Player player) {
                        if (onClose != null)
                            onClose.accept(player);
                    }

                    @Override
                    public boolean closeOnClick() {
                        return closeOnClick;
                    }

                    @Override
                    public boolean dontUpdateOnClick() {
                        return dontUpdateOnClick;
                    }

                    @Override
                    public boolean playSoundOnClick() {
                        return playSoundOnClick;
                    }

                    @Override
                    public int priority() {
                        return priority;
                    }

                    @Override
                    public boolean canShow(final Player p) {
                        return canShow.apply(p);
                    }
                };
            }
            return new BenefitItem() {
                @Override
                public ItemStack getItem(final Player player) {
                    return itemStack.apply(player);
                }

                @Override
                public String permissions() {
                    return permission;
                }

                @Override
                public void onClick(final Player player, final Boolean rightclick) {
                    clickAction.accept(player, rightclick);
                }

                @Override
                public void onDisplay(final Player player) {
                    if (onDisplay != null)
                        onDisplay.accept(player);
                }

                @Override
                public void onClose(final Player player) {
                    if (onClose != null)
                        onClose.accept(player);
                }

                @Override
                public boolean closeOnClick() {
                    return closeOnClick;
                }

                @Override
                public boolean dontUpdateOnClick() {
                    return dontUpdateOnClick;
                }

                @Override
                public boolean playSoundOnClick() {
                    return playSoundOnClick;
                }

                @Override
                public int priority() {
                    return priority;
                }
            };
        }
    }
}