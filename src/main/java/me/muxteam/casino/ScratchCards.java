package me.muxteam.casino;

import me.muxteam.muxsystem.MuxInventory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class ScratchCards {
    private final Map<Player, Integer> tries = new HashMap<>(), difficulty = new HashMap<>();
    private final ConcurrentHashMap<Player, Integer> winnings = new ConcurrentHashMap<>();
    private final Random r = new Random();
    private Inventory main;
    private Location loc;
    private final MuxCasino c;

    public ScratchCards(final MuxCasino c) {
        this.c = c;
        setup();
    }

    private void setup() {
        loc = new Location(c.getWorld(), -10.5, 8, -153.5, 180F, 0F);
        c.ms.getNPCS().addVillager(0, loc.clone(), BlockFace.NORTH, "§a§lRubbellose", this::openScratchCards);
        this.main = c.ms.getServer().createInventory(null, 27, "§0§lMuxCasino§0 | Rubbellose");
        main.setItem(11, c.ms.renameItemStack(new ItemStack(Material.EMPTY_MAP, 1), "§3§lRubbellos:§a Einfach", "§7Preis: §d2.000 Chips", "",
                "§a§oBis zu 200.000 Chips", "", "§7Klicke um es zu §3kaufen§7."));
        main.setItem(13, c.ms.renameItemStack(new ItemStack(Material.EMPTY_MAP, 2), "§3§lRubbellos:§e Normal", "§7Preis: §d10.000 Chips", "",
                "§e§oBis zu 2.000.000 Chips", "", "§7Klicke um es zu §3kaufen§7."));
        main.setItem(15, c.ms.renameItemStack(new ItemStack(Material.EMPTY_MAP, 3), "§3§lRubbellos:§c Risiko", "§7Preis: §d10.000 Chips", "",
                "§c§oBis zu 5.000.000 Chips", "", "§7Klicke um es zu §3kaufen§7."));
    }

    public void onUpdate() {
        for (final Player p : winnings.keySet()) {
            if (p.getOpenInventory() == null || p.getOpenInventory().getTopInventory().getTitle().contains("Rubbellose") == false ||
                    (p.getWorld().equals(loc.getWorld()) == false || p.getLocation().distance(loc) > 5)) {
                c.ms.showItemBar(p, "§cRubbellos abgebrochen.");
                quit(p);
            }
        }
    }

    public void openScratchCards(final Player p) {
        if (c.inCasino(p) == false) return;
        if (winnings.containsKey(p)) {
            c.ms.showItemBar(p, "§cBitte warte etwas..");
            return;
        }
        if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
        p.openInventory(main);
        c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
    }

    public void quit(final Player p) {
        if (tries.containsKey(p) && difficulty.containsKey(p)) {
            if (p.isOnline())
                p.closeInventory();
            int t = tries.remove(p), d = difficulty.remove(p);
            if (t == 8) {
                final int chips = d == 1 ? 2000 : 10000;
                if (c.inCasino(p)) {
                    c.addChips(p, chips, "Rubbellose");
                } else {
                    c.addChipsOffline(p.getUniqueId(), chips, "Rubbellose");
                }
                if (d == 1) c.getStats().RUBBEL_GEWINN1 -= chips;
                else if (d == 2) c.getStats().RUBBEL_VERLUST2 -= chips;
                else if (d == 3) c.getStats().RUBBEL_VERLUST3 -= chips;
            }
            final Integer wins = winnings.remove(p);
            if (wins != null) {
                if (c.inCasino(p)) {
                    c.addChips(p, wins, "Rubbellose");
                } else {
                    c.addChipsOffline(p.getUniqueId(), wins, "Rubbellose");
                }
                if (d == 1) c.getStats().RUBBEL_VERLUST1 += wins;
                else if (d == 2) c.getStats().RUBBEL_VERLUST2 += wins;
                else if (d == 3) c.getStats().RUBBEL_VERLUST3 += wins;
            }
        }
    }

    private void openCard(final Player p) {
        int t = tries.getOrDefault(p, -1);
        if (t == -1) {
            tries.put(p, 8);
        } else if (t == 0) {
            final int wins = winnings.remove(p);
            c.ms.showItemBar(p, "§aGewinn: §d" + c.ms.getNumberFormat(wins) + " Chips");
            final int d = difficulty.get(p);
            if (d == 1) c.getStats().RUBBEL_VERLUST1 += wins;
            else if (d == 2) c.getStats().RUBBEL_VERLUST2 += wins;
            else if (d == 3) c.getStats().RUBBEL_VERLUST3 += wins;
            c.addChips(p, wins, "Rubbellose");
            new BukkitRunnable() {
                @Override
                public void run() {
                    quit(p);
                }
            }.runTaskLater(c.ms, 10L);
            return;
        }
        if (winnings.putIfAbsent(p, 0) == null) {
            final Inventory inv = c.ms.getServer().createInventory(null, 54, "§0§lMuxCasino§0 | Rubbellose");
            final ItemStack black = c.ms.renameItemStack(new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 15), "§a§lÖffnen", "", "§7Klicke, um dieses Feld zu §aöffnen§7."),
                    green = c.ms.renameItemStack(new ItemStack(Material.STAINED_GLASS_PANE, 1, (byte) 5), "§a§lÖffnen", "", "§7Klicke, um dieses Feld zu §aöffnen§7.");
            for (int i = 0; i < inv.getSize(); i++) {
                inv.setItem(i, black);
            }
            inv.setItem(12, green);
            inv.setItem(14, green);
            inv.setItem(22, green);
            inv.setItem(31, green);
            inv.setItem(30, green);
            inv.setItem(32, green);
            inv.setItem(39, green);
            inv.setItem(41, green);
            c.sendScoreboard(p);
            if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
            p.openInventory(inv);
            c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
        }
    }

    public void onInvClick(final Player p, final ItemStack i, final int rawslot, final String title) {
        if (title.contains("Rubbellose") == false) return;
        final Material m = i.getType();
        if (m == Material.EMPTY_MAP) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
            if (i.getAmount() == 1) {
                if (c.canBuy(p, 2000, "Rubbellose")) {
                    c.getStats().RUBBEL_GEWINN1 += 2000;
                    difficulty.put(p, 1);
                    openCard(p);
                }
            } else if (i.getAmount() == 2) {
                if (c.canBuy(p, 10000, "Rubbellose")) {
                    c.getStats().RUBBEL_GEWINN2 += 10000;
                    difficulty.put(p, 2);
                    openCard(p);
                }
            } else if (i.getAmount() == 3) {
                if (c.canBuy(p, 10000, "Rubbellose")) {
                    c.getStats().RUBBEL_GEWINN3 += 10000;
                    difficulty.put(p, 3);
                    openCard(p);
                }
            }
        } else if (m == Material.STAINED_GLASS_PANE) {
            if (r.nextInt(4) == 0) c.addEnergy(p, 1);
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
            final int tried = tries.getOrDefault(p, -1);
            if (tried < 1) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            }
            ItemStack item = c.ms.addGlow(c.ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 10), "§d100 Chips"));
            final int difficult = difficulty.get(p);
            if (difficult == 1) {
                int amount = r.nextInt(60), ran = r.nextInt(10000);
                if (ran > 5000) {
                    amount += 298;
                    if (ran > 9500) {
                        amount += 1000;
                        if (ran > 9998) {
                            amount += 200000;
                            p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 1F, 5F);
                        }
                    }
                }
                item = c.ms.addGlow(c.ms.renameItemStack(new ItemStack(Material.INK_SACK, 1,
                        (byte) (amount > 200000 ? 11 : amount > 1000 ? 5 : amount > 150 ? 13 : 9)), "§d" + amount + " Chips"));
                winnings.put(p, winnings.get(p) + amount);
            } else if (difficult == 2) {
                if (tried < 8 && r.nextInt(17) == 0) {
                    double mal = r.nextDouble();
                    mal += 1.0;
                    item = c.ms.addGlow(c.ms.renameItemStack(new ItemStack(Material.PAPER, 1), "§a§l" + c.round(mal, 2) + "x"));
                    p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 10F);
                    winnings.put(p, (int) (winnings.get(p) * mal));
                } else {
                    int amount = 300 + r.nextInt(100), ran = r.nextInt(10000);
                    if (ran > 5000) {
                        amount += 1200;
                        if (ran > 9500) {
                            amount += 3500;
                            if (ran > 9998) {
                                amount += 1000000;
                                p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 1F, 5F);
                            }
                        }
                    }
                    item = c.ms.addGlow(c.ms.renameItemStack(new ItemStack(Material.INK_SACK, 1,
                            (byte) (amount > 1000000 ? 11 : amount > 5000 ? 5 : amount > 1000 ? 13 : 9)), "§d" + amount + " Chips"));
                    winnings.put(p, winnings.get(p) + amount);
                }
            } else if (difficult == 3) {
                if (tried < 8 && (r.nextInt(6) == 0 || (tried == 1 && r.nextInt(4) == 0))) {
                    item = c.ms.addGlow(c.ms.renameItemStack(new ItemStack(Material.TNT, 1), "§c§l0x"));
                    p.playSound(p.getLocation(), Sound.EXPLODE, 1F, 5F);
                    winnings.put(p, 0);
                } else {
                    int amount = 500 + r.nextInt(900), ran = r.nextInt(10000);
                    if (ran > 4000) {
                        amount += 2400;
                        if (ran > 8000) {
                            amount += 4000;
                            if (ran > 9998) {
                                amount += 1005000;
                                p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 1F, 5F);
                            }
                        }
                    }
                    item = c.ms.addGlow(c.ms.renameItemStack(new ItemStack(Material.INK_SACK, 1,
                            (byte) (amount > 1000000 ? 11 : amount > 7000 ? 5 : amount > 3000 ? 13 : 9)), "§d" + amount + " Chips"));
                    winnings.put(p, winnings.get(p) + amount);
                }
            }
            p.getOpenInventory().getTopInventory().setItem(rawslot, item);
            p.updateInventory();
            tries.put(p, tried - 1);
            openCard(p);
        }
    }
}