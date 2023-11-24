package me.muxteam.casino;

import me.muxteam.basic.MuxFirework;
import me.muxteam.muxsystem.MuxInventory;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class GuessTheNumberGame {
    public final Set<Player> startinv = new HashSet<>();
    public final Map<UUID, Inventory> inventories = new HashMap<>();
    public final Map<UUID, Integer> guesses = new HashMap<>();
    public final Map<Player, Integer> realstakes = new HashMap<>();
    public int realstake = 500, winnings = 0;
    public boolean started = false;
    private final Location loc;
    private final int maxcountdown = 20;
    private int countdown = maxcountdown, idleseconds = 0;
    private final Random r = new Random();
    private final MuxCasino c;

    public GuessTheNumberGame(final Location loc, final MuxCasino c) {
        this.loc = loc.clone();
        this.c = c;
        c.ms.getNPCS().addVillager(0, loc, BlockFace.SOUTH, "§a§lZahlen Raten", p -> {
            if (started == false) openStartInv(p);
            else openInv(p);
        });
    }

    public void close() {
        for (final UUID uuid : guesses.keySet()) {
            c.addChipsOffline(uuid, realstake, "Zahlen Raten");
        }
        reset();
    }

    public void quit(final Player p) {
        if (guesses.size() == 1 && guesses.containsKey(p.getUniqueId())) {
            guesses.remove(p.getUniqueId());
            c.ms.showItemBar(p, "§cDu hast das Zahlen Raten Spiel verlassen.");
            c.addChips(p, realstake, "Zahlen Raten");
            inventories.remove(p.getUniqueId());
        }
    }
    public void openStartInv(final Player p) {
        final Inventory inv = c.ms.getServer().createInventory(null, 54, "§0§lMuxCasino§0 | Zahlen Raten");
        final ItemStack go = c.ms.renameItemStack(new ItemStack(Material.EMERALD_BLOCK, 1), "§a§lErstellen",
                "§7Klicke, um das Spiel", "§7zu §aerstellen§7.");
        c.createButton(inv, 21, go);
        inv.setItem(4, c.ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 13), "§f§lEinsatz: §d§l" + c.ms.getNumberFormat(realstakes.getOrDefault(p, 500)) + " Chips", "",
                "§aLinksklick§7, für §d+" + c.ms.getNumberFormat(c.getNextStakeChange(realstakes.getOrDefault(p, 500), true)) + " Chips",
                "§cRechtsklick§7, für §d-" + c.ms.getNumberFormat(c.getNextStakeChange(realstakes.getOrDefault(p, 500), false)) + " Chips"));
        if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
        p.openInventory(inv);
        startinv.add(p);
        c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
    }
    public void openInv(final Player p) {
        final Inventory inv = c.ms.getServer().createInventory(null, 54, "§0§lMuxCasino§0 | Zahlen Raten");
        int currentTipp = -1;
        if (guesses.containsKey(p.getUniqueId())) currentTipp = guesses.get(p.getUniqueId());
        for (int i = 0; i < 50; i++) {
            inv.addItem(c.ms.renameItemStack(currentTipp == (i + 1) ? c.ms.addGlow(new ItemStack(Material.MAP)) : new ItemStack(Material.PAPER), "§a§l" + (i + 1),
                    "", currentTipp == (i + 1) ? "§aDies ist dein derzeitiger Tipp." : currentTipp != -1 ? "§cDu hast bereits eine Auswahl getroffen." :
                            "§7Klicke, um §d" + c.ms.getNumberFormat(realstake) + " Chips §7zu §asetzen§7."));
        }
        final ItemStack wait = c.ms.renameItemStack(new ItemStack(Material.REDSTONE_BLOCK, 1), "§c§lWarte auf Spieler",
                "§7Sobald mindestens 2 Spieler einen Tipp", "§7abgegeben haben, läuft der Countdown."),
                cooldown = c.ms.renameItemStack(new ItemStack(Material.EMERALD_BLOCK, countdown), "§f§lAuflösung in §a§l" + countdown,
                        "§7Nach diesem Countdown,", "§7wird eine zufällige Zahl gelost.");
        if (countdown == maxcountdown) inv.setItem(inv.getSize() - 2, wait);
        else inv.setItem(inv.getSize() - 2, cooldown);

        inventories.put(p.getUniqueId(), inv);
        if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
        p.openInventory(inv);
        c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
    }
    public void onUpdate() {
        realstakes.entrySet().removeIf(entry -> entry.getKey().isOnline() == false);
        if (started == false) return;
        if (guesses.keySet().size() > 1) {
            if (countdown > 0) {
                for (final Inventory inv : inventories.values()) {
                    final ItemStack cooldown = c.ms.renameItemStack(new ItemStack(Material.EMERALD_BLOCK, countdown, (short) 0), "§f§lAuflösung in §a§l" + countdown,
                            "§7Nach diesem Countdown,", "§7wird eine zufällige Zahl gelost.");
                    inv.setItem(inv.getSize() - 2, cooldown);
                }
                countdown--;
            } else {
                final int finalZahl = r.nextInt(50) + 1;
                final List<UUID> currentwinner = new ArrayList<>();
                int gewinnerzahlplayer = 0, kleinerAbstand = 51;
                for (final Map.Entry<UUID, Integer> entry : guesses.entrySet()) {
                    final UUID u = entry.getKey();
                    final Player p = c.ms.getServer().getPlayer(u);
                    if (p != null) p.closeInventory();
                    int pZahl = entry.getValue();
                    if (pZahl < finalZahl) {
                        if (kleinerAbstand >= finalZahl - pZahl) {
                            if (kleinerAbstand != finalZahl - pZahl) currentwinner.clear();
                            kleinerAbstand = finalZahl - pZahl;
                            gewinnerzahlplayer = pZahl;
                            currentwinner.add(u);
                        }
                    } else {
                        if (kleinerAbstand >= pZahl - finalZahl) {
                            if (kleinerAbstand != finalZahl - pZahl) currentwinner.clear();
                            kleinerAbstand = pZahl - finalZahl;
                            gewinnerzahlplayer = pZahl;
                            currentwinner.add(u);
                        }
                    }
                }
                if (currentwinner.isEmpty() == false) {
                    StringBuilder gewinner = null;
                    final int winnerAmount = currentwinner.size();
                    for (final UUID uuid : currentwinner) {
                        final Player p = c.ms.getServer().getPlayer(uuid);
                        final int won = winnings / winnerAmount;
                        c.addChips(uuid, won, "Zahlen Raten");
                        if (p != null) {
                            if (gewinner == null)
                                gewinner = new StringBuilder(p.getName());
                            else
                                gewinner.append("§7, §d").append(p.getName());
                            p.sendMessage("§d§lMuxCasino>§a §lGewonnen!§7 Du hast §d" + c.ms.getNumberFormat(won) + " Chips §7gewonnen!");
                            MuxFirework.spawnColor(p.getLocation(), Color.GREEN);
                        } else {
                            final OfflinePlayer op = c.ms.getServer().getOfflinePlayer(uuid);
                            if (gewinner == null)
                                gewinner = new StringBuilder(op.getName());
                            else
                                gewinner.append("§7, §d").append(op.getName());
                        }
                    }
                    for (final Map.Entry<UUID, Integer> entry : guesses.entrySet()) {
                        final Player p = c.ms.getServer().getPlayer(entry.getKey());
                        if (p != null) {
                            p.sendMessage("§d§lMuxCasino>§7 Die Zahl war §a" + finalZahl + "§7. Deine Zahl war §a" + entry.getValue() + "§7.");
                            if (winnerAmount > 1)
                                p.sendMessage("§d§lMuxCasino>§7 Gewonnen haben §d" + gewinner + " §7mit der Zahl §a" + gewinnerzahlplayer + "§7.");
                            else
                                p.sendMessage("§d§lMuxCasino>§7 Gewonnen hat §d" + gewinner + " §7mit der Zahl §a" + gewinnerzahlplayer + "§7.");
                        }
                    }
                }
                c.getStats().GUESS_THE_NUMBER_GAMES++;
                reset();
            }
        } else if (guesses.size() == 1) {
            final Player player = c.ms.getServer().getPlayer(guesses.keySet().stream().findFirst().orElse(null));
            if (player == null)
                reset();
            else if (player.getWorld() != c.getWorld() || player.getLocation().distance(loc) > 5) {
                c.ms.showItemBar(player, "§cDu hast das Zahlen Raten Spiel verlassen.");
                c.addChips(player.getUniqueId(), realstake, "Zahlen Raten");
                guesses.remove(player.getUniqueId());
                inventories.remove(player.getUniqueId());
            }
        } else {
            final List<AbstractMap.SimpleEntry<Player, Inventory>> list = inventories.entrySet().stream().map(
                    entry -> new AbstractMap.SimpleEntry<>(c.ms.getServer().getPlayer(entry.getKey()), entry.getValue())).filter(entry -> entry.getValue() != null).collect(Collectors.toList());
            if (list.isEmpty() || list.stream().noneMatch(entry -> entry.getValue().getTitle().equals(entry.getKey().getOpenInventory().getTopInventory().getTitle()))) {
                if (idleseconds++ == 10)
                    reset();
            }
        }
    }
    private void reset() {
        guesses.clear();
        countdown = maxcountdown;
        idleseconds = 0;
        winnings = 0;
        realstake = 500;
        started = false;
        inventories.entrySet().stream().map(entry -> new AbstractMap.SimpleEntry<>(c.ms.getServer().getPlayer(entry.getKey()), entry.getValue()))
                .filter(entry -> entry.getValue() != null).filter(entry -> entry.getValue().getTitle().equals(entry.getKey().getOpenInventory().getTopInventory().getTitle()))
                .forEach(entry -> openStartInv(entry.getKey()));
        inventories.clear();
    }
}