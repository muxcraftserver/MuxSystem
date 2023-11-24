package me.muxteam.casino;

import me.muxteam.basic.MuxFirework;
import me.muxteam.muxsystem.MuxInventory;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BlackJackGame {
    public int gn;
    public Map<Player, Inventory> currentInv = new HashMap<>();
    public final Map<Player, Integer> stakes = new HashMap<>(), realstakes = new HashMap<>();
    private final Map<Player, List<String>> cardDecks = new HashMap<>(), playerCards = new HashMap<>();
    private final Map<Player, Integer> playerCardAmount = new HashMap<>();
    private final Map<Player, String> firstBankCards = new HashMap<>();
    private final ItemStack GRAY_PANE, GREEN_PANE;
    private final MuxCasino c;
    private final Random r = new Random();
    private final BlackJack bj;

    public BlackJackGame(final Location loc, final MuxCasino ca, final BlackJack bj, final int gn) {
        this.c = ca;
        this.gn = gn;
        this.bj = bj;
        this.GRAY_PANE = c.ms.renameItemStack(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15), "§c");
        this.GREEN_PANE = c.ms.renameItemStack(new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 13), "§c");
        c.ms.getNPCS().addVillager(0, loc, BlockFace.EAST, "§a§lBlackJack", this::openBlackJack);
    }

    public void quit(final Player p) {
        int chips = realstakes.remove(p);
        if (p.hasMetadata("systemkick") || c.isEnabled() == false) {
            c.addChips(p, chips, "BlackJack");
            c.getStats().BLACKJACK_GEWINN -= chips;
        }
        cardDecks.remove(p);
        playerCards.remove(p);
        playerCardAmount.remove(p);
        currentInv.remove(p);
        stakes.remove(p);
        firstBankCards.remove(p);
    }

    public void openInv(final Player p) {
        int stake = stakes.getOrDefault(p, 0);
        if (stake == 0 || stake > c.getChips(p)) {
            stake = 500;
            stakes.put(p, 500);
        }
        final Inventory inv = c.ms.getServer().createInventory(null, 54, "§0§lMuxCasino§0 | BlackJack");
        final ItemStack go = c.ms.renameItemStack(new ItemStack(Material.EMERALD_BLOCK, 1), "§a§lErstellen",
                "§7Bringe deinen Kartenwert möglichst nah", "§7an die 21, ohne sie zu überschreiten.", "", "§7Klicke, um das Spiel zu §aerstellen§7.");
        c.createButton(inv, 21, go);
        inv.setItem(4, c.ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 13), "§f§lEinsatz: §d§l" + c.ms.getNumberFormat(stake) + " Chips", "",
                "§aLinksklick§7, für §d+" + c.ms.getNumberFormat(c.getNextStakeChange(stake, true)) + " Chips",
                "§cRechtsklick§7, für §d-" + c.ms.getNumberFormat(c.getNextStakeChange(stake, false)) + " Chips"));
        if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
        p.openInventory(inv);
        c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
    }
    public void nextCard(final Player p) {
        openPlayerInv(p, false);
        final List<String> pcards = playerCards.get(p);
        pcards.add(getRandomCard(p));
        playerCards.put(p, pcards);
        new BukkitRunnable() {
            @Override public void run() {
                if (realstakes.containsKey(p) == false) {
                    reset(p);
                    cancel();
                    return;
                }
                p.playSound(p.getLocation(), Sound.SHEEP_SHEAR, 1F, 0.2F);
                openPlayerInv(p, true);
            }
        }.runTaskLater(c.ms, 10);
    }
    public void openBankInv(final Player p) {
        final Inventory inv = c.ms.getServer().createInventory(null, 9 * 6, "§0§lMuxCasino§0 | BlackJack");
        addPanes(false, inv, realstakes.get(p));
        updatePlayerCards(p, inv);

        final List<String> bankCards = new ArrayList<>();
        bankCards.add(firstBankCards.get(p));
        final int cardValue = updateBankCards(inv, bankCards);

        inv.setItem(4, c.ms.renameItemStack(new ItemStack(Material.SKULL_ITEM, cardValue, (short) 2), "§a§lDie Bank ist am Zug", "§7Kartenwert der Bank: §d" + cardValue));
        final ItemStack clone = c.ms.getHead(p.getName());
        final Integer cardamt = playerCardAmount.get(p);
        if (cardamt != null) clone.setAmount(cardamt);
        inv.setItem(22, c.ms.addGlow(c.ms.renameItemStack(clone, "§a§l" + p.getName(), "§7Dein Kartenwert: §d" + playerCardAmount.get(p))));
        startBankTask(p, 20, inv, 0, bankCards);
        currentInv.put(p, inv);
        if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
        p.openInventory(inv);
        c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
    }
    private void startBankTask(final Player p, final long ticks, final Inventory inv, final int cardValue, final List<String> bankCards) {
        new BukkitRunnable() {
            private boolean first = true;
            private int sCardValue = 0;
            @Override public void run() {
                if (realstakes.containsKey(p) == false) {
                    reset(p);
                    cancel();
                    return;
                }
                if (first) {
                    p.playSound(p.getLocation(), Sound.VILLAGER_YES, 1F, 1F);
                    first = false;
                } else {
                    if (checkCardValue(p, cardValue, false) == false) {
                        if (sCardValue >= 17) {
                            checkCardValue(p, sCardValue, true);
                            cancel();
                            return;
                        }
                        bankCards.add(getRandomCard(p));
                        sCardValue = updateBankCards(inv, bankCards);
                        p.playSound(p.getLocation(), Sound.SHEEP_SHEAR, 1F, 0.2F);
                        inv.setItem(4, c.ms.renameItemStack(new ItemStack(Material.SKULL_ITEM, sCardValue, (short) 2), "§a§lDie Bank ist am Zug", "§7Kartenwert der Bank: §d" + sCardValue));
                    }
                }
            }
        }.runTaskTimer(c.ms, 20L, ticks);
    }

    private Integer updateBankCards(final Inventory inv, final List<String> bankCards) {
        int sCardValue = 0, i = 0;
        for (final String s : bankCards) {
            final int worth = getWorth(cardWithoutColor(s), sCardValue);
            sCardValue += worth;
            i++;
            if (i <= 14) {
                inv.setItem(i <= 7 ? (i + 9) : (i + 9 + 2), c.ms.renameItemStack(new ItemStack(Material.PAPER, worth, (short) 0), "§f" + s,
                        "§7Diese Karte hat einen", "§7Wert von §d" + worth + "§7."));
            }
        }
        return sCardValue;
    }
    public void updatePlayerCards(final Player p, final Inventory inv) {
        final List<String> cards = playerCards.get(p);
        if (cards == null) return;
        int i = 0, totalWorth = 0;
        for (final String s : cards) {
            final int worth = getWorth(cardWithoutColor(s), totalWorth);
            totalWorth += worth;
            final ItemStack card = c.ms.renameItemStack(new ItemStack(Material.PAPER, worth), "§f" + s,
                    "§7Diese Karte hat einen", "§7Wert von §d" + worth + "§7.");
            i++;
            if (i <= 14) {
                inv.setItem(i <= 7 ? i + 27 : i + 27 + 2, card);
            }
        }
    }
    private boolean checkCardValue(final Player p, final int cardValue, final boolean f) {
        if (f) {
            if (cardValue >= 17 && cardValue == playerCardAmount.get(p)) {
                pass(p, cardValue);
                return true;
            } else if (cardValue > 21) {
                onWin(p, true, playerCardAmount.get(p));
                return true;
            } else if (cardValue == 21 || cardValue > playerCardAmount.get(p)) {
                onWin(p, false, cardValue);
                return true;
            } else {
                onWin(p, true, playerCardAmount.get(p));
                return true;
            }
        } else {
            if (cardValue > 21) {
                onWin(p, true, playerCardAmount.get(p));
                return true;
            }
            if (cardValue >= 17 && cardValue == playerCardAmount.get(p)) {
                pass(p, cardValue);
                return true;
            } else if (cardValue == 21 || cardValue > playerCardAmount.get(p)) {
                onWin(p, false, cardValue);
                return true;
            }
        }
        return false;
    }
    /*private boolean isSoft17(final List<String> bankcards) {
        int sum = 0;
        boolean hasAce = false;
        for (final String card : bankcards) {
            int cardValue = getWorth(card, sum);
            if (cardValue == 11) {
                hasAce = true;
            }
            sum += cardValue;
        }
        return sum == 17 && hasAce;
    }*/

    public void openFirstBank(final Player p) {
        reset(p);
        final Inventory inv = c.ms.getServer().createInventory(null, 9 * 6, "§0§lMuxCasino§0 | BlackJack");
        addPanes(false, inv, realstakes.get(p));
        inv.setItem(4, c.ms.addGlow(c.ms.renameItemStack(new ItemStack(Material.SKULL_ITEM, 1, (short) 2), "§a§lDie Bank ist am Zug",
                "§7Kartenwert der Bank: §d" + 0)));
        final ItemStack clone = c.ms.getHead(p.getName());
        final Integer cardamt = playerCardAmount.get(p);
        if (cardamt != null) clone.setAmount(cardamt);
        inv.setItem(22, c.ms.addGlow(c.ms.renameItemStack(clone, "§a§l" + p.getName(), "§7Dein Kartenwert: §d" + 0)));

        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 0.2F);
        new BukkitRunnable() {
            private boolean first = true;
            @Override public void run() {
                if (realstakes.containsKey(p) == false) {
                    reset(p);
                    cancel();
                    return;
                }
                if (first) {
                    final String cs = getRandomCard(p);
                    firstBankCards.put(p, cs);
                    inv.setItem(1 + 9, c.ms.renameItemStack(new ItemStack(Material.PAPER, getWorth(cardWithoutColor(cs), 0)), "§f" + cs,
                            "§7Diese Karte hat einen", "§7Wert von §d" + getWorth(cardWithoutColor(cs), 0) + "§7."));
                    p.playSound(p.getLocation(), Sound.SHEEP_SHEAR, 1F, 0.2F);
                    first = false;
                } else {
                    cancel();
                    startGame(p);
                }
            }
        }.runTaskTimer(c.ms, 20L, 40L);
        if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
        p.openInventory(inv);
        c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
        currentInv.put(p, inv);
    }
    public void openPlayerInv(final Player p, final boolean button) {
        final Inventory inv = c.ms.getServer().createInventory(null, 9 * 6, "§0§lMuxCasino§0 | BlackJack");
        addPanes(true, inv, realstakes.get(p));
        inv.setItem(4, c.ms.renameItemStack(new ItemStack(Material.SKULL_ITEM, getWorth(cardWithoutColor(firstBankCards.get(p)), 0), (short) 2), "§a§lBank",
                "§7Kartenwert der Bank: §d" + getWorth(cardWithoutColor(firstBankCards.get(p)), 0)));
        int cardValue = 0;

        final List<String> cards = playerCards.get(p);
        if (cards != null) {
            for (final String s : cards) {
                cardValue += getWorth(cardWithoutColor(s), cardValue);
            }
        }

        final int finalCardValue = cardValue;
        playerCardAmount.put(p, finalCardValue);
        updateBankCards(inv, Collections.singletonList(firstBankCards.get(p)));
        final ItemStack clone = c.ms.getHead(p.getName());
        if (cardValue > 0) clone.setAmount(playerCardAmount.get(p));
        inv.setItem(22, c.ms.addGlow(c.ms.renameItemStack(clone, "§a§lDu bist am Zug", "§7Dein Kartenwert: §d" + cardValue)));
        if (button) {
            new BukkitRunnable() {
                @Override public void run() {
                    if (realstakes.containsKey(p) == false) {
                        reset(p);
                        return;
                    }
                    inv.setItem(6 + 5 * 9, c.ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (short) 13), "§a§lKarte ziehen", "§aKlicke§7, um eine weitere", "§7Karte zu ziehen."));
                    inv.setItem(2 + 5 * 9, c.ms.renameItemStack(new ItemStack(Material.BARRIER), "§c§lZug beenden", "§cKlicke§7, um deinen Zug", "§7zu beenden."));
                    currentInv.put(p, inv);
                    if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
                    p.openInventory(inv);
                    c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
                    if (finalCardValue == 21) {
                        onWin(p, true, finalCardValue);
                    } else if (finalCardValue > 21) {
                        onWin(p, false, finalCardValue);
                    }
                }
            }.runTaskLater(c.ms, 10);
        }
        updatePlayerCards(p, inv);
        currentInv.put(p, inv);
        if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
        p.openInventory(inv);
        c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
    }

    private void addPanes(final boolean player, final Inventory inv, final long stake) {
        final ItemStack pane1 = player ? GRAY_PANE : GREEN_PANE, pane2 = player ? GREEN_PANE : GRAY_PANE;
        for (int s = 0; s < 9; s++) {
            inv.setItem(s, pane1);
        }
        for (int s = 18; s < 27; s++) {
            inv.setItem(s, pane2);
        }
        inv.setItem(0, c.ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (short) 5), "§a§lMöglicher Gewinn:§d " + c.ms.getNumberFormat(stake * 2) + " Chips",
                "§7Bei einem §aBlackjack§7 (10 & 11)", "§7würdest du §d" + c.ms.getNumberFormat(stake * 3L) + " Chips §7gewinnen."));
    }
    private int getWorth(final String card, final int currentTotal) {
        if (card == null) return 0;
        String cardvalue = ChatColor.stripColor(card).replace("Bube", "10").replace("Dame", "10").replace("König", "10").
                        replace("Ass", "11").replace("❤", "").replace("✦", "").
                        replace("✢", "").replace("Ω", "").replace(" ", "");
        int value = Integer.parseInt(cardvalue);
        if (cardvalue.equals("11")) {
            if (currentTotal + 11 > 21) {
                value = 1;
            } else {
                value = 11;
            }
        }
        return value;
    }
    private String cardWithoutColor(final String card) {
        if (card == null) return null;
        final String pig = "Ω", herz = "❤", karo = "✦", kreuz = "✢";
        return ChatColor.stripColor(card).replace(pig, "").replace(herz, "").replace(karo, "").replace(kreuz, "");
    }
    private String getRandomCard(final Player p) {
        final List<String> cards = cardDecks.get(p);
        final int card = r.nextInt(cards.size());
        final String ca = cards.get(card);
        cards.remove(card);
        return ca;
    }
    public void startGame(final Player p) {
        final List<String> cs = new ArrayList<>();
        playerCards.put(p, cs);
        openPlayerInv(p, false);
        p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        new BukkitRunnable() {
            private short runs = 0;
            @Override public void run() {
                if (realstakes.containsKey(p) == false) {
                    reset(p);
                    cancel();
                    return;
                }
                cs.add(getRandomCard(p));
                p.playSound(p.getLocation(), Sound.SHEEP_SHEAR, 1F, 0.2F);
                if (runs == 0) {
                    openPlayerInv(p, false);
                    runs++;
                } else {
                    cancel();
                    openPlayerInv(p, true);
                }
            }
        }.runTaskTimer(c.ms, 10L, 20L);
    }
    private void pass(final Player p, final int cardamount) { // When the Bank has the same card amount & it's >= 17
        final int chips = realstakes.get(p);
        if (p.isOnline()) {
            if (p.isOnline()) MuxFirework.spawnColor(p.getLocation(), Color.YELLOW);
            c.addChips(p, chips, "BlackJack");
            c.ms.showItemBar(p, "§eGeld zurück!§f Ihr hattet den gleichen Kartenwert. [§e" + cardamount + "§f].");
            p.closeInventory();
        } else {
            new BukkitRunnable() {
                @Override public void run() {
                    c.addChipsOffline(p, chips, "BlackJack");
                }
            }.runTaskAsynchronously(c.ms);
        }
        cardDecks.remove(p);
        c.getStats().BLACKJACK_VERLUST += chips;
        c.getStats().BLACKJACK_GAMES++;
        currentInv.remove(p);
        realstakes.remove(p);
    }
    private void onWin(final Player p, final boolean won, final int cardamount) {
        if (won) {
            final boolean blackjack = cardamount == 21 && playerCards.get(p).size() == 2;
            final int win = (int) (realstakes.get(p) * (blackjack ? 3D : 2D)); // When the player hits a blackjack: 3x Payout
            c.ms.showItemBar(p, "§aDu hast gewonnen! §d(+" + c.ms.getNumberFormat(win) + " Chips)");
            p.closeInventory();
            if (p.isOnline()) {
                if (blackjack) c.ms.sendNormalTitle(p, "§a§lBlackjack! (3x Gewinn)", 10, 30, 10);
                MuxFirework.spawnColor(p.getLocation(), Color.GREEN);
                c.addChips(p, win, "BlackJack");
            } else {
                new BukkitRunnable() {
                    @Override public void run() {
                        c.addChipsOffline(p, win, "BlackJack");
                    }
                }.runTaskAsynchronously(c.ms);
            }
            c.getStats().BLACKJACK_VERLUST += win;
            c.getStats().BLACKJACK_GAMES_WON++;
        } else {
            if (cardamount > 21) {
                c.ms.showItemBar(p, "§fDein Kartenwert [§c" + cardamount + "§f] war über 21.");
            } else {
                c.ms.showItemBar(p, "§fDie Bank [§c" + cardamount + "§f] hatte einen höheren Kartenwert.");
            }
            if (p.isOnline()) MuxFirework.spawnColor(p.getLocation(), Color.RED);
        }
        p.closeInventory();
        c.getStats().BLACKJACK_GAMES++;
        cardDecks.remove(p);
        currentInv.remove(p);
        realstakes.remove(p);
    }
    public void openBlackJack(final Player p) {
        if (c.inCasino(p) == false) return;
        bj.gamenr.put(p, gn);
        final Inventory current = currentInv.get(p);
        if (current != null) {
            if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
            p.openInventory(current);
            c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
        } else if (cardDecks.containsKey(p) == false) {
            openInv(p);
        } else {
            openPlayerInv(p, true);
        }
    }
    private void reset(final Player p) {
        cardDecks.remove(p);
        playerCards.remove(p);
        playerCardAmount.remove(p);
        firstBankCards.remove(p);
        currentInv.remove(p);
        if (c.inCasino(p) == false) return;
        final List<String> cards = new ArrayList<>();
        for (int a = 0; a < 4; a++) { // The amount of carddecks we have
            for (int f = 0; f < 4; f++) {
                for (int i = 2; i <= 14; i++) {
                    String color = "§c❤";
                    if (f == 1) {
                        color = "§c✦";
                    } else if (f == 2) {
                        color = "§8✢";
                    } else if (f == 3) {
                        color = "§8Ω";
                    }
                    if (i <= 10) {
                        cards.add(color + " §f§l" + i + " " + color);
                    } else if (i == 11) {
                        cards.add(color + " §f§lBube " + color);
                    } else if (i == 12) {
                        cards.add(color + " §f§lDame " + color);
                    } else if (i == 13) {
                        cards.add(color + " §f§lKönig " + color);
                    } else {
                        cards.add(color + " §f§lAss " + color);
                    }
                }
            }
        }
        cardDecks.put(p, cards);
    }
}