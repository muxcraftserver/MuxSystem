package me.muxteam.casino;

import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class CoinPusher {
    private final Map<Integer, CoinPusherGame> games = new HashMap<>();
    private int ticks = 0;
    private final MuxCasino c;

    public CoinPusher(final MuxCasino c) {
        this.c = c;
        //addCoinPusher(new Location(c.getWorld(), 16.5, 8, -162.5), new Location(c.getWorld(), -26.5, 50, -225.5), 500);
        //addCoinPusher(new Location(c.getWorld(), 1.5, 20, -153.5), new Location(c.getWorld(), -40.5, 50, -225.5), 5000);
    }

    public void close() {
        games.values().forEach(CoinPusherGame::close);
    }

    public void onUpdate() {
        games.values().forEach(CoinPusherGame::update);
        if (ticks++ > 10) {
            ticks = 0;
            games.values().forEach(game -> {
                game.updateMachine();
                game.updateStartAmount();
            });
        }
    }

    private void addCoinPusher(final Location loc, final Location redstoneblock, final int stake) {
        final int nr = games.size() + 1;
        games.put(nr, new CoinPusherGame(c, loc, redstoneblock, nr, stake));
    }

    public boolean onInteractAtEntity(final Player p, final Entity click) {
        if (click instanceof ArmorStand) {
            final ArmorStand clicked = (ArmorStand) click;
            for (final CoinPusherGame game : games.values()) {
                if (game.getConstruct().contains(clicked) || game.getIllustration1().containsValue(clicked) || game.getIllustration2().containsValue(clicked) || game.getIllustration3().containsValue(clicked)) {
                    game.tryToPlay(p);
                    return true;
                }
            }
        }
        return false;
    }

    public void onInteract(final Player p, final Sign sign) {
        games.values().stream().filter(game1 -> game1.button == sign.getBlock()).findFirst().ifPresent(game -> game.tryToPlay(p));
    }
}