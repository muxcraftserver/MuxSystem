package me.muxteam.casino;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GuessTheNumber {
    private final List<GuessTheNumberGame> games = new ArrayList<>();
    private final MuxCasino c;

    public GuessTheNumber(final MuxCasino c) {
        this.c = c;
        setup();
    }

    private void setup() {
        games.add(new GuessTheNumberGame(new Location(c.getWorld(), 42.5, 22, -184.5), c));
    }

    public void close() {
        for (final GuessTheNumberGame g : games) {
            g.close();
        }
    }

    public void quit(final Player p) {
        for (final GuessTheNumberGame g : games) {
            g.quit(p);
        }
    }

    public void onUpdate() {
        for (final GuessTheNumberGame g : games) {
            g.onUpdate();
        }
    }

    public void onInvClick(final Player p, final ItemStack i, final int rawslot, final String title, final Inventory inv, boolean rightclick) {
        if (title.contains("Zahlen Raten") == false) return;
        GuessTheNumberGame game = null;
        for (final GuessTheNumberGame g : games) {
            if (g.inventories.containsValue(inv) || g.startinv.contains(p)) {
                game = g;
            }
        }
        if (game == null) {
            return;
        }
        final Material m = i.getType();
        if (m == Material.INK_SACK) {
            if (game.started) {
                game.openInv(p);
                c.ms.showItemBar(p, "§cDas Spiel hat bereits begonnen.");
                return;
            }
            final int stake = game.realstakes.getOrDefault(p, 500);
            if (rightclick) {
                if (stake >= 1000) {
                    game.realstakes.put(p, stake - c.getNextStakeChange(stake, false));
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                } else {
                    c.ms.showItemBar(p, "§cDer Einsatz muss mindestens §d500 Chips §cbetragen.");
                    p.closeInventory();
                    p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
                    return;
                }
            } else {
                final int newstake = stake + c.getNextStakeChange(stake, true);
                if (newstake > c.getChips(p) || newstake > 1000000000) {
                    p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
                    return;
                }
                game.realstakes.put(p, newstake);
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
            }
            game.openStartInv(p);
        } else if (m == Material.EMERALD_BLOCK) {
            if (game.started && inv.getItem(22).getType() != Material.EMERALD_BLOCK) {
                game.openInv(p);
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                c.ms.showItemBar(p, "§cDas Spiel hat bereits begonnen.");
                return;
            }
            final int stake = game.realstakes.getOrDefault(p, 500);
            if (stake > c.getChips(p)) {
                c.ms.showItemBar(p, "§cDu hast nicht genug Chips.");
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                p.closeInventory();
                return;
            }
            game.realstake = game.realstakes.getOrDefault(p, 500);
            game.started = true;
            c.ms.getAnalytics().getAnalytics().addPlayedCasinoGame("Guess the Number");
            game.startinv.stream().filter(Objects::nonNull).filter(OfflinePlayer::isOnline).forEach(HumanEntity::closeInventory);
            game.openInv(p);
            game.startinv.clear();
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
        } else if (m == Material.MAP || m == Material.REDSTONE_BLOCK) {
            p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
        } else if (m == Material.PAPER) {
            if (game.started == false) {
                c.ms.showItemBar(p, "§cDas Spiel hat noch nicht begonnen.");
                game.openStartInv(p);
                return;
            } else if (game.guesses.containsKey(p.getUniqueId())) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return;
            } else if (c.canBuy(p, game.realstake, "Zahlen Raten") == false) {
                return;
            }
            c.ms.showItemBar(p, "§aDu hast deinen Einsatz gesetzt.");
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
            c.getStats().GUESS_THE_NUMBER_EINSATZ += game.realstake;
            game.winnings += game.realstake;
            game.guesses.put(p.getUniqueId(), rawslot + 1);
            game.openInv(p);
        }
    }
}