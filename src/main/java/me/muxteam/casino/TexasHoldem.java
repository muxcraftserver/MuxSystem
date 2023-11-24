package me.muxteam.casino;

import me.muxteam.muxsystem.MuxInventory;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Sign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TexasHoldem implements Listener {
    public final Map<Player, TexasHoldemGame.PokerHistory> gamehistory = new HashMap<>();
    private final List<TexasHoldemGame> games = new ArrayList<>();
    public final Map<Player, Integer> gamenr = new HashMap<>();
    public final MuxCasino c;

    public TexasHoldem(final MuxCasino c) {
        this.c = c;
        games.add(new TexasHoldemGame(0, this, new Location[] {
                        new Location(c.getWorld(), 41, 10, -178),
                        new Location(c.getWorld(), 42, 10, -178),
                        new Location(c.getWorld(), 39, 10, -178),
                        new Location(c.getWorld(), 38, 11, -178) },
                new Location(c.getWorld(), 40.5, 10, -175.5),
                new Location(c.getWorld(), 34.5, 10, -182.1)));
        c.ms.getServer().getPluginManager().registerEvents(this, c.ms);
    }

    public void close() {
        HandlerList.unregisterAll(this);
        games.forEach(TexasHoldemGame::close);
    }

    public void quit(final Player p) {
        games.forEach(game -> game.quit(p));
    }
    public void onUpdate() {
        games.forEach(TexasHoldemGame::onUpdate);
    }
    public boolean onInvClick(final Player p, final ItemStack i, final boolean rightclick, final String title) {
        if (title.contains("Texas Holdem") == false) return false;
        TexasHoldemGame game = null;
        final int gn = gamenr.getOrDefault(p, -1);
        if (gn == -1) return false;
        for (final TexasHoldemGame g : games) {
            if (g.gn == gn) game = g;
        }
        if (game == null) return false;
        final Material m = i.getType();
        if (m == Material.REDSTONE_BLOCK) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 1F);
            if (game.isRunning && game.data.getGameState().ordinal() > TexasHoldemGame.GameState.WAITING_COOLDOWN.ordinal()) {
                final Player current = game.data.getCurrentPlayer();
                if (current != null) {
                    game.getStatusHologram().setMessage("§f" + current.getName() + " hat§c aufgegeben");
                    game.lastAction.put(current, " §cAufgegeben ");
                }
                for (final Player o : game.getPlayers()) {
                    if (o != null) {
                        o.playSound(o.getLocation(), Sound.GHAST_MOAN, 0.5F, 5F);
                    }
                }
                game.updateHolograms();
                p.closeInventory();
                game.surrender(p);
                return true;
            }
            game.quit(p);
        } else if (m == Material.PAPER && gamehistory.containsKey(p) && game.getPlayers().contains(p) == false) {
            if (c.canBuy(p, 10000, "Texas Holdem",false) == false) {
                return true;
            }
            final TexasHoldemGame.PokerHistory history = gamehistory.get(p);
            p.getInventory().addItem(history.getStatsFor(p, false));
            p.closeInventory();
            p.playSound(p.getLocation(), Sound.HORSE_ARMOR, 0.8F, 0.8F);
            c.ms.showItemBar(p, "§aDu hast die Erinnerung gekauft.");
        } else if (game.isRunning && game.data.getGameState().ordinal() > TexasHoldemGame.GameState.WAITING_COOLDOWN.ordinal()) {
            if (game.data.getCurrentPlayer() != p) {
                c.ms.showItemBar(p, "§eDu bist nicht am Zug.");
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                p.closeInventory();
                return true;
            }
            switch (m) {
                case NETHER_STAR: {
                    if (game.data.getCurrentPlayer() != null && game.checkEnable) {
                        game.currentAction = 0;
                        game.data.setGameState(TexasHoldemGame.GameState.PLAYER_SWITCH);
                        game.getStatusHologram().setMessage("§f" + game.data.getCurrentPlayer().getName() + " hat§a gecheckt");
                        game.lastAction.put(game.data.getCurrentPlayer(), " §eGecheckt ");
                        game.updateHolograms();
                        p.closeInventory();
                        game.getPlayers().forEach(all -> all.playSound(all.getLocation(), Sound.NOTE_PLING, 1F, 1F));
                    }
                    break;
                }
                case INK_SACK: {
                    if (rightclick) {
                        if (game.realstakes.get(p) <= 500)
                            game.realstakes.put(p, 500);
                        else
                            game.realstakes.put(p, game.realstakes.get(p) - c.getNextStakeChange(game.realstakes.get(p), false));
                    } else {
                        final int newstake = game.realstakes.get(p) + c.getNextStakeChange(game.realstakes.get(p), true);
                        if (newstake > c.getChips(p) || newstake > 10000000) {
                            p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
                            return true;
                        } else {
                            game.realstakes.put(p, game.realstakes.get(p) + c.getNextStakeChange(game.realstakes.get(p), true));
                        }
                    }
                    game.openOverviewInv(p);
                    p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
                    break;
                }
                case GOLD_BLOCK: {
                    if (game.currentAction > 1) {
                        int hasToPay = game.currentAction - game.data.getPlayerSet().getOrDefault(p, 0);
                        if (c.canBuy(p, hasToPay, "Texas Holdem")) {
                            game.data.setPot(game.data.getPot() + hasToPay);
                            game.data.getPlayerSet().put(p, game.data.getPlayerSet().getOrDefault(p, 0) + hasToPay);
                            game.data.setGameState(TexasHoldemGame.GameState.PLAYER_SWITCH);
                            p.closeInventory();
                            game.getStatusHologram().setMessage("§f" + game.data.getCurrentPlayer().getName() + " hat§a nachgezogen");
                            game.lastAction.put(game.data.getCurrentPlayer(), " §f+" + c.ms.getNumberFormat(game.currentAction) + " §aNachgezogen ");
                            game.getPlayers().forEach(player -> player.playSound(player.getLocation(), Sound.MAGMACUBE_JUMP, 1F, 2F));
                            game.data.getTotalPlayerInserts().put(p, game.data.getTotalPlayerInserts().getOrDefault(p, 0) + hasToPay);
                            game.updateHolograms();
                        }
                    }
                    break;
                }
                case EMERALD_BLOCK: {
                    for (final Player all : game.getPlayers()) {
                        if (c.getChips(all) + game.data.getPlayerSet().getOrDefault(all, 0) < game.realstakes.get(p) + game.currentAction) {
                            p.closeInventory();
                            c.ms.showItemBar(p, "§cEs haben nicht alle Spieler so viele Chips.");
                            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                            return true;
                        }
                    }
                    if (c.canBuy(p, game.realstakes.get(p) - game.data.getPlayerSet().getOrDefault(p, 0) + game.currentAction, "Texas Holdem")) {
                        game.currentAction = game.realstakes.get(p) + game.currentAction;
                        game.data.setPot(game.data.getPot() - game.data.getPlayerSet().getOrDefault(p, 0) + game.currentAction);
                        game.data.getPlayerSet().put(p, game.currentAction);
                        p.closeInventory();
                        game.getStatusHologram().setMessage("§f" + p.getName() + " hat §aerhöht");
                        game.lastAction.put(p, " §f+" + c.ms.getNumberFormat(game.currentAction) + " §aErhöht ");
                        game.checkEnable = false;
                        game.getPlayers().forEach(all -> all.playSound(all.getLocation(), Sound.MAGMACUBE_JUMP, 1F, 2F));
                        game.lastAction.entrySet().removeIf(entry -> entry.getValue().contains("Gecheckt"));
                        game.data.getTotalPlayerInserts().put(p, game.data.getTotalPlayerInserts().getOrDefault(p, 0) + game.currentAction - game.data.getPlayerSet().getOrDefault(p, 0));
                        game.updateHolograms();
                        game.data.setGameState(TexasHoldemGame.GameState.PLAYER_SWITCH);
                    }
                    break;
                }
                default: {
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                    return true;
                }
            }
            return true;
        }
        if (m == Material.INK_SACK) {
            if (game.isRunning) {
                p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                return true;
            }
            final int stake = game.stakes.get(p);
            if (rightclick) {
                if (stake >= 1000) {
                    game.stakes.put(p, stake - c.getNextStakeChange(stake, false));
                    p.playSound(p.getLocation(), Sound.NOTE_STICKS, 1F, 1F);
                } else {
                    c.ms.showItemBar(p, "§cDer Einsatz muss mindestens §d500 Chips §cbetragen.");
                    p.closeInventory();
                    p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
                    return true;
                }
            } else {
                final int newstake = stake + c.getNextStakeChange(stake, true);
                if (newstake > c.getChips(p) || newstake > 500000000) {
                    p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
                    return true;
                }
                game.stakes.put(p, newstake);
                p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
            }
            game.openInv(p);
        } else if (m == Material.EMERALD_BLOCK) {
            p.playSound(p.getLocation(), Sound.CLICK, 1F, 10F);
            if (game.isRunning && (game.data.getGameState() == TexasHoldemGame.GameState.WAITING || game.data.getGameState() == TexasHoldemGame.GameState.WAITING_COOLDOWN)) {
                if (game.join(p) == false) {
                    return true;
                }
                game.updateHolograms();
                return true;
            } else if (game.isRunning) {
                p.closeInventory();
                return true;
            }
            int stake = game.stakes.get(p);
            if (c.canBuy(p, stake, "Texas Holdem") == false) {
                return true;
            }
            game.data.setStake(stake);
            game.join(p);
            game.data.setPot(game.data.getStake() * game.getPlayers().size());
            game.updateHolograms();
            game.isRunning = true;
        }
        return true;
    }
    public void onInteract(final Player p, final Sign ignoredSign) {
        final int pgame = gamenr.getOrDefault(p, -1);
        if (pgame == -1) return;
        TexasHoldemGame game = null;
        for (final TexasHoldemGame g : games) {
            if (g.gn == gamenr.get(p)) {
                game = g;
                break;
            }
        }
        if (game == null || game.data.getGameState().ordinal() <= TexasHoldemGame.GameState.WAITING_COOLDOWN.ordinal()) return;
        game.openOverviewInv(p);
    }
    public boolean onDrop(final Player p) {
        final int pgame = gamenr.getOrDefault(p, -1);
        return pgame != -1;
    }
    private void openLeaveGame(final Player p) {
        final Inventory inventory = c.ms.getServer().createInventory(null, 45, "§0§lMuxCasino§0 | Texas Holdem");
        c.createButton(inventory, 12, c.ms.renameItemStack(new ItemStack(Material.REDSTONE_BLOCK), "§c§lSpiel verlassen", "", "§7Klicke, um das Spiel zu §cverlassen§7."));
        if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
        p.openInventory(inventory);
        c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
    }
    @EventHandler
    public void onVehicleExit(final EntityDismountEvent e) {
        if (e.getEntity() instanceof Player && e.getDismounted() instanceof ArmorStand) {
            final Player p = (Player) e.getEntity();
            if (p.hasMetadata("systemkick"))
                return;
            final int pgame = gamenr.getOrDefault(p, -1);
            if (pgame != -1) {
                TexasHoldemGame game = null;
                for (final TexasHoldemGame g : games) {
                    if (g.gn == pgame) {
                        game = g;
                        break;
                    }
                }
                if (game == null || game.isRunning == false) return;
                openLeaveGame(p);
                p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
                e.setCancelled(true);
            }
        }
    }
}