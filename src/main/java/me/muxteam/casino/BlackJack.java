package me.muxteam.casino;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlackJack {
    private final List<BlackJackGame> games = new ArrayList<>();
    public final Map<Player, Integer> gamenr = new HashMap<>();
    private final MuxCasino c;

    public BlackJack(final MuxCasino c) {
        this.c = c;
        setup();
    }

    public void setup() {
        games.add(new BlackJackGame(new Location(c.getWorld(), -14.5, 8, -161.5), c, this, 1));
    }

    public void quit(final Player p) {
        for (final BlackJackGame g : games) {
            if (g.realstakes.containsKey(p)) {
                g.quit(p);
            }
        }
        gamenr.remove(p);
    }

    public void onInvClick(final Player p, final ItemStack i, final boolean rightclick, final String title) {
        if (title.contains("BlackJack") == false) return;
        BlackJackGame game = null;
        final int gn = gamenr.getOrDefault(p, -1);
        if (gn == -1) return;
        for (final BlackJackGame g : games) {
            if (g.gn == gn) game = g;
        }
        if (game == null) return;
        final Material m = i.getType();
        if (m == Material.INK_SACK) {
            if (game.currentInv.containsKey(p) == false) {
                final int stake = game.stakes.get(p);
                if (rightclick) {
                    if (stake >= 1000) {
                        game.stakes.put(p, stake - c.getNextStakeChange(stake, false));
                        p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    } else {
                        c.ms.showItemBar(p, "§cDer Einsatz muss mindestens §d500 Chips §cbetragen.");
                        p.closeInventory();
                        p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
                        return;
                    }
                } else {
                    final int newstake = stake + c.getNextStakeChange(stake, true);
                    if (newstake > c.getChips(p) || newstake > 10000000) {
                        p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
                        return;
                    }
                    game.stakes.put(p, newstake);
                    p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
                }
                game.openInv(p);
                return;
            }
        } else if (m == Material.EMERALD_BLOCK) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
            final int stake = game.stakes.get(p);
            if (c.canBuy(p, stake, "BlackJack") == false) {
                return;
            }
            game.realstakes.put(p, stake);
            c.ms.getAnalytics().getAnalytics().addPlayedCasinoGame("Blackjack");
            c.getStats().BLACKJACK_GEWINN += stake;
            game.openFirstBank(p);
            return;
        } else if (m == Material.STAINED_CLAY) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
            game.nextCard(p);
            return;
        } else if (m == Material.BARRIER) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
            game.openBankInv(p);
            return;
        }
        p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
    }
}
