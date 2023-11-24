package me.muxteam.casino;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RockPaperScissors implements Listener {
    private final Set<RockPaperScissorsGame> games = new HashSet<>();
    public final Map<Player, RockPaperScissorsGame> gamenr = new HashMap<>();
    private final MuxCasino c;

    public RockPaperScissors(final MuxCasino c) {
        this.c = c;
        setup();
        c.ms.getServer().getPluginManager().registerEvents(this, c.ms);
    }

    public void close() {
        HandlerList.unregisterAll(this);
    }

    private void setup() {
        games.add(new RockPaperScissorsGame(new Location(c.getWorld(), -41.5, 12, -152.5), c, this));
        games.add(new RockPaperScissorsGame(new Location(c.getWorld(), -41.5, 12, -167.5), c, this));
        games.add(new RockPaperScissorsGame(new Location(c.getWorld(), 52.5, 12, -167.5), c, this));
        games.add(new RockPaperScissorsGame(new Location(c.getWorld(), 52.5, 12, -152.5), c, this));
        games.add(new RockPaperScissorsGame(new Location(c.getWorld(), -30.5, 25, -134.5), c, this));
        games.add(new RockPaperScissorsGame(new Location(c.getWorld(), 41.5, 25, -134.5), c, this));
    }

    public void quit(final Player p, final boolean reset) {
        for (final RockPaperScissorsGame g : games) {
            boolean isReset = reset && g.p1 == p || g.p2 == p;
            g.quit(p);
            if (isReset) g.resetGame();
        }
    }
    public void onUpdate() {
        for (final RockPaperScissorsGame g : games) {
            g.onUpdate();
        }
    }
    public void onInteract(final Player p, final Sign s) {
        if (s.getLine(0).equalsIgnoreCase("[ssp]") || s.getLine(0).equalsIgnoreCase("§1§lSchere, Stein")) {
            s.setLine(0, "§1§lSchere, Stein");
            s.setLine(1, "§1§lPapier");
            s.setLine(2, "§8-*-");
            s.setLine(3, "Klicke zum Öffnen");
            s.update();
            final Location bloc = s.getLocation();
            for (final RockPaperScissorsGame g : games) {
                if (g.getLocation().distance(bloc) < 3.5D) {
                    if (p.isInsideVehicle()) {
                        g.interact(p);
                    } else {
                        c.ms.showItemBar(p, "§cSetze dich hin, um mitzuspielen.");
                        p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
                    }
                }
            }
        }
    }
    public void onInvClick(final Player p, final ItemStack i, final boolean rightclick, final String title, final Inventory inv) {
        if (title.contains("Schere Stein") == false) return;
        RockPaperScissorsGame game = null;
        for (final RockPaperScissorsGame g : games) {
            if (gamenr.containsKey(p) && gamenr.get(p) == g) {
                game = g;
            }
        }
        if (game == null) {
            return;
        }
        p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
        final Material m = i.getType();
        if (inv.getType() == InventoryType.HOPPER) {
            if (m == Material.PAPER) {
                if (p == game.p1) game.choicep1 = 3;
                if (p == game.p2) game.choicep2 = 3;
            }
            if (m == Material.STONE) {
                if (p == game.p1) game.choicep1 = 2;
                if (p == game.p2) game.choicep2 = 2;
            }
            if (m == Material.SHEARS) {
                if (p == game.p1) game.choicep1 = 1;
                if (p == game.p2) game.choicep2 = 1;
            }
            p.closeInventory();
            game.checkStart();
            return;
        }

        if (m == Material.INK_SACK) {
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
                if (newstake > c.getChips(p) || newstake > 1000000000) {
                    p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
                    return;
                }
                game.stakes.put(p, newstake);
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
            }
            game.openInv(p);
        } else if (m == Material.EMERALD_BLOCK) {
            final String display = i.getItemMeta().getDisplayName();
            if (display.contains("Beitreten")) {
                if (game.p1 == null) {
                    p.closeInventory();
                    c.ms.showItemBar(p, "§cDein Mitspieler ist weg.");
                    p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
                    return;
                } else if (game.p2 != null) {
                    p.closeInventory();
                    c.ms.showItemBar(p, "§cDieses Spiel ist bereits besetzt.");
                    p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
                    return;
                }
                int stake = game.stakes.get(game.p1);
                if (c.canBuy(p, stake, "Schere Stein Papier") == false) {
                    return;
                }
                game.stakes.put(p, stake);
                game.realstakes.put(p, stake);
                c.getStats().RPS_GESETZT += stake;
                game.p2 = p;
                c.ms.showItemBar(p, "§aEinsatz von §d" + c.ms.getNumberFormat(stake) + " Chips §agesetzt.");
                game.start(RockPaperScissorsGame.GameState.CHOOSING);
            } else if (display.contains("Erstellen")) {
                if (game.p1 != null) {
                    p.closeInventory();
                    c.ms.showItemBar(p, "§cDieses Schere Stein Papier Spiel ist schon erstellt.");
                    p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
                } else if (game.p2 == null) {
                    int stake = game.stakes.get(p);
                    if (c.canBuy(p, stake, "Schere Stein Papier") == false) {
                        return;
                    }
                    game.stakes.put(p, stake);
                    game.realstakes.put(p, stake);
                    c.getStats().RPS_GESETZT += stake;
                    game.p1 = p;
                    p.closeInventory();
                    c.ms.showItemBar(p, "§aEinsatz von §d" + c.ms.getNumberFormat(stake) + " Chips §agesetzt.");
                    c.ms.sendNormalTitle(p, "§eWarte nun auf einen Mitspieler.", 10, 40, 10);
                } else {
                    game.resetGame();
                }
            }
        }
    }
    public boolean onDrop(final Player p) {
        final RockPaperScissorsGame game = gamenr.get(p);
        if (game != null) {
            final RockPaperScissorsGame.GameState state = game.state;
            return state == RockPaperScissorsGame.GameState.RUNNING || state == RockPaperScissorsGame.GameState.CHOOSING;
        }
        return false;
    }
    public boolean onPickup(final Player p) {
        return onDrop(p);
    }
    @EventHandler
    public void onInvClick(final InventoryClickEvent e) {
        final Player p = (Player) e.getWhoClicked();
        if (c.inCasino(p) && gamenr.containsKey(p)) {
            e.setCancelled(true);
        }
    }
    @EventHandler
    public void onInvDrag(final InventoryDragEvent e) {
        final Player p = (Player) e.getWhoClicked();
        final RockPaperScissorsGame game = gamenr.get(p);
        if (game != null) {
            final RockPaperScissorsGame.GameState state = game.state;
            if (state == RockPaperScissorsGame.GameState.RUNNING || state == RockPaperScissorsGame.GameState.CHOOSING) {
                e.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onInvClose(final InventoryCloseEvent e) {
        final Player p = (Player) e.getPlayer();
        final RockPaperScissorsGame game = gamenr.get(p);
        if (game != null) {
            final RockPaperScissorsGame.GameState state = game.state;
            if (state == RockPaperScissorsGame.GameState.CHOOSING && e.getInventory().getType() == InventoryType.HOPPER) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if ((game.p1 == p && game.choicep1 == 0) || (game.p2 == p && game.choicep2 == 0)) {
                            game.openChooseInventory(p);
                        }
                    }
                }.runTaskLater(c.ms, 10L);
            } else if (state == RockPaperScissorsGame.GameState.WAITING && e.getInventory().getName().endsWith("Schere Stein Papier") && game.p1 != p) {
                gamenr.remove(p);
            }
        }
    }
}