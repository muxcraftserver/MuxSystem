package me.muxteam.casino;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import me.muxteam.basic.MuxHolograms;
import me.muxteam.muxsystem.MuxInventory;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class TexasHoldemGame {
    private final Random r = new Random();
    public final int gn;
    public boolean isRunning = false;
    private final Location boardLocation, npcLocation;
    private final Location[] chairLocations;
    private final List<String> cards = new ArrayList<>();
    private final Map<Player, PokerDeck> decks = new HashMap<>();
    public final Map<Player, Integer> stakes = new HashMap<>(), realstakes = new HashMap<>();
    private final List<ArmorStand> cardsa = new ArrayList<>();
    private final List<Player> players = new ArrayList<>(4);
    public final Map<Player, String> lastAction = new HashMap<>();
    public int currentAction = 0; // 0 = check, > 1 raise
    public boolean checkEnable = true;
    public final GameData data;
    private final Map<Player, Inventory> overviewInventories = new HashMap<>();
    private final List<Sign> signs = new ArrayList<>();

    private final MuxHolograms.Hologram[] infoHolograms = new MuxHolograms.Hologram[6];
    private MuxHolograms.Hologram statusHologram, winHologram, winningsHologram, cardsHologram, roundHologram;
    private final MuxHolograms.Hologram[] cardHolograms = new MuxHolograms.Hologram[5];

    private final TexasHoldem th;
    public final MuxCasino c;

    public TexasHoldemGame(final int gn, final TexasHoldem th, final Location[] chairLocations, final Location npcLoc, final Location boardLocation) {
        this.gn = gn;
        this.data = new GameData();
        this.th = th;
        this.c = th.c;
        this.chairLocations = chairLocations;
        this.boardLocation = boardLocation;
        this.npcLocation = npcLoc;
        for (int i = 0; i < chairLocations.length; i++) {
            chairLocations[i] = chairLocations[i].getBlock().getLocation();
        }
        spawnPaper(boardLocation.clone().add(3.6, 0, 1));
        spawnPaper(boardLocation.clone().add(4.6, 0, 1));
        spawnPaper(boardLocation.clone().add(5.6, 0, 1));
        spawnPaper(boardLocation.clone().add(6.6, 0, 1));
        spawnPaper(boardLocation.clone().add(7.6, 0, 1));
        c.ms.getNPCS().addVillager(0, npcLoc, BlockFace.SOUTH, "§a§lTexas Holdem", p -> {
            if (c.inCasino(p) == false) return;
            if (isRunning == false) {
                openInv(p);
            } else {
                if (data.gameState.ordinal() <= GameState.WAITING_COOLDOWN.ordinal())
                    Arrays.stream(chairLocations).filter(location -> c.ms.getChairs().containsKey(location) == false).findFirst().ifPresent(freeChair -> openJoinInv(p));
            }
        });
        initInfoHologram();
        for (final Location chairLocation : chairLocations) {
            for (int x = -3; x < 3; x++) {
                for (int z = -3; z < 3; z++) {
                    for (int y = -1; y < 1; y++) {
                        final Location location = chairLocation.clone().add(x, y, z);
                        if (location.getBlock().getType() == Material.WALL_SIGN || location.getBlock().getType() == Material.SIGN_POST) {
                            final Sign sign = (Sign) location.getBlock().getState();
                            if (signs.contains(sign) == false) signs.add(sign);
                        }
                    }
                }
            }
        }
    }

    private void initInfoHologram() {
        final Location location = boardLocation.clone().add(6, 1, 0);
        final String[] infoText = new String[]{
                "§a§lTexas Holdem",
                "§fBei diesem Spiel hat jeder Spieler §e2 Karten§f.",
                "§fDazu kommen im Verlauf des Spiels §e5 weitere.",
                "§f§a§lZiel§f:§r §fBeste §eKombination §faus allen Karten",
                "§fEs gibt §e4 Runden §fin denen §eerhöht §foder §egecheckt §fwird.",
                "§fNur §eeiner§f kann gewinnen."
        };
        for (int i = 0; i < infoText.length; i++) {
            infoHolograms[i] = c.ms.getHolograms().addHologram("thi-" + gn + i, location.clone(), infoText[i]);
            location.setY(location.getY() - 0.3);
        }
    }

    private void showInfoHologram() {
        for (final MuxHolograms.Hologram infoHologram : infoHolograms) {
            infoHologram.show();
        }
    }

    private void hideInfoHologram() {
        for (final MuxHolograms.Hologram infoHologram : infoHolograms) {
            infoHologram.hide();
        }
    }

    private void spawnPaper(final Location location) {
        final ArmorStand a = (ArmorStand) location.getWorld().spawnEntity(location.clone().add(0.65, 0.2, 0), EntityType.ARMOR_STAND);
        a.setVisible(false);
        a.setGravity(false);
        a.setItemInHand(new ItemStack(Material.PAPER, 1));
        a.setRightArmPose(new EulerAngle(0.0, 3.0, 0.0));
        cardsa.add(a);
    }

    public void openJoinInv(final Player p) {
        final Inventory inv = c.ms.getServer().createInventory(null, 54, "§0§lMuxCasino§0 | Texas Holdem");
        final ItemStack go = c.ms.renameItemStack(new ItemStack(Material.EMERALD_BLOCK, 1), "§a§lBeitreten",
                "§7Einsatz: §d" + c.ms.getNumberFormat(data.getStake()) + " Chips", "", "§7Klicke§7, um dem Spiel §abeizutreten§7.");
        c.createButton(inv, 21, go);
        if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
        p.openInventory(inv);
        c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
        th.gamenr.put(p, gn);
        final PokerHistory history = th.gamehistory.get(p);
        if (history != null && history.playerNames.size() > 1) {
            inv.setItem(inv.getSize() - 9, history.getStatsFor(p, true));
        }
    }

    public void openInv(final Player p) {
        int stake = stakes.getOrDefault(p, 0);
        if (stake == 0 || stake > c.getChips(p)) {
            stake = 500;
            stakes.put(p, 500);
        }
        final Inventory inv = c.ms.getServer().createInventory(null, 54, "§0§lMuxCasino§0 | Texas Holdem");
        final ItemStack go = c.ms.renameItemStack(new ItemStack(Material.EMERALD_BLOCK, 1), "§a§lErstellen",
                (isRunning == false ? (new String[]{"§7Klicke, um das Spiel", "§7zu §aerstellen§7."})
                        : (new String[]{"§7Einsatz: §d" + c.ms.getNumberFormat(data.getStake()) + " Chips", "", "§7Klicke§7, um dem Spiel §abeizutreten§7."})));
        c.createButton(inv, 21, go);
        if (isRunning == false)
            inv.setItem(4, c.ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 13), "§f§lEinsatz: §d§l" + c.ms.getNumberFormat(stake) + " Chips", "",
                    "§aLinksklick§7, für §d+" + c.ms.getNumberFormat(c.getNextStakeChange(stake, true)) + " Chips",
                    "§cRechtsklick§7, für §d-" + c.ms.getNumberFormat(c.getNextStakeChange(stake, false)) + " Chips"));
        if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
        p.openInventory(inv);
        c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
        th.gamenr.put(p, gn);
        final PokerHistory history = th.gamehistory.get(p);
        if (history != null) {
            inv.setItem(inv.getSize() - 9, history.getStatsFor(p, true));
        }
    }

    public void openOverviewInv(final Player p) {
        final Inventory inv = c.ms.getServer().createInventory(null, 9 * 3, "§0§lMuxCasino§0 | Texas Holdem");
        if (checkEnable == false)
            inv.setItem(1 + 2 * 9, c.ms.renameItemStack(new ItemStack(Material.REDSTONE_BLOCK), "§c§lAufgeben",
                    "§7Durch das Aufgeben verlierst du", "§7alle bisher gesetzten Chips.", "", "§7Klicke§7, um §caufzugeben§7."));

        final int playerStake = realstakes.getOrDefault(p, 500);
        if (playerStake > data.stake * 5) {
            realstakes.put(p, 500);
            openOverviewInv(p);
            return;
        }
        int roundInsert = 0;
        if (currentAction > 1) roundInsert = currentAction;

        if (data.currentPlayer != null && data.currentPlayer.equals(p) && roundInsert > data.playerSet.getOrDefault(p, 0)) {
            inv.setItem(4 + 2 * 9, c.ms.renameItemStack(new ItemStack(Material.GOLD_BLOCK),
                    "§e§lNachziehen§7 [ §d" + (c.ms.getNumberFormat(currentAction - data.playerSet.getOrDefault(p, 0))) + " Chips §7]",
                    "§cSolltest du nicht nachziehen", "§cgibst du automatisch auf.",
                    "", "§7Klicke, um mit §d" + (c.ms.getNumberFormat(currentAction - data.playerSet.getOrDefault(p, 0))) + " Chips §enachzuziehen§7."));
        }

        String costs = c.ms.getNumberFormat((playerStake - data.playerSet.getOrDefault(p, 0) + roundInsert)) + " Chips";
        if (data.pot < 1000000000) {
            inv.setItem(7 + 9, c.ms.renameItemStack(
                new ItemStack(Material.INK_SACK, 1, (byte) 13),
                "§f§lEinsatz: §d§l" + c.ms.getNumberFormat(realstakes.get(p)) + " " + "Chips",
                "", "§aLinksklick§7, für §d+" + c.ms.getNumberFormat(c.getNextStakeChange(realstakes.get(p), true)) + " Chips",
                "§cRechtsklick§7, für §d-" + c.ms.getNumberFormat(c.getNextStakeChange(realstakes.get(p), false)) + " Chips"));

             inv.setItem(7 + 2 * 9, c.ms.renameItemStack(new ItemStack(Material.EMERALD_BLOCK), "§a§lErhöhen",
                    "§7Kosten: §d" + costs, "", "§7Klicke, um auf §d" + (c.ms.getNumberFormat(playerStake + roundInsert)) + " Chips", "§7zu §aerhöhen§7."));
         }
        if (checkEnable) {
            inv.setItem(4 + 2 * 9, c.ms.renameItemStack(new ItemStack(Material.NETHER_STAR), "§f§lChecken", "§7Wenn du checkst, gibst du an",
                    "§7den nächsten Spieler weiter.", "", "§7Klicke, um zu §fchecken§7."));
        }
        final String comb = (String) getCombination(p).get(0);
        final boolean current = data.currentPlayer != null && data.currentPlayer.equals(p);
        final ItemStack cooldown = c.ms.renameItemStack(current ? new ItemStack(Material.WATCH, Math.max(0, data.time)) : new ItemStack(Material.BOOK),
                current ? "§f§lNoch §a§l" + data.time + " Sekunden §f§lam Zug" : "§f§lPoker Hände",
                (comb.contains("Royal") ? "§a" : "§7") + "Royal Flush - Höchster Straight Flush",
                (comb.contains("Straight Flush") ? "§a" : "§7") + "Straight Flush - 5 gleichfarbige Karten in Reihe",
                (comb.contains("4 Gleiche") ? "§a" : "§7") + "4 Gleiche - 4 gleichrangige Karten",
                (comb.contains("Full House") ? "§a" : "§7") + "Full House - 3 und 2 gleichrangige Karten",
                (comb.equals("Flush") ? "§a" : "§7") + "Flush - 5 gleichfarbige Karten",
                (comb.equals("Straight") ? "§a" : "§7") + "Straight - 5 Karten in Reihe",
                (comb.contains("3 Gleiche") ? "§a" : "§7") + "3 Gleiche - 3 gleichrangige Karten",
                (comb.contains("2 Paare") ? "§a" : "§7") + "2 Paare - 2 Mal 2 gleichrangige Karten",
                (comb.contains("1 Paar") ? "§a" : "§7") + "1 Paar - 2 gleichrangige Karten",
                (comb.contains("Höchste Karte") ? "§a" : "§7") + "Höchste Karte - Karte mit dem höchsten Wert");

        inv.setItem(4, cooldown);
        if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
        p.openInventory(inv);
        overviewInventories.put(p, inv);
        c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
    }

    public boolean join(final Player p) {
        final Location freeChair = Arrays.stream(chairLocations).filter(location -> c.ms.getChairs().containsKey(location) == false).findFirst().orElse(null);
        if (freeChair != null) {
            if (isRunning && c.canBuy(p, data.stake, "Texas Holdem") == false) {
                p.closeInventory();
                return false;
            }
            th.gamenr.put(p, gn);
            addChairAtLocation(p, freeChair);
            players.add(p);
            realstakes.put(p, data.stake);
            if (statusHologram == null) {
                hideInfoHologram();
                statusHologram = c.ms.getHolograms().addHologram("ths" + gn, boardLocation.clone().add(6, 2.3, 0), "§aWarte auf Spieler");
            }
            return true;
        }
        c.ms.showItemBar(p, "§cDas Spiel ist bereits voll.");
        return false;
    }

    public void quit(final Player p) {
        if (p.hasMetadata("systemkick")) return;
        if (th.gamenr.containsKey(p) == false || th.gamenr.get(p) != gn) return;
        th.gamenr.remove(p);
        if (players.remove(p) == false) return;
        if (c.isEnabled() == false) {
            int chipsToAdd = data.stake;
            if (data.totalPlayerInserts.containsKey(p)) chipsToAdd += data.totalPlayerInserts.get(p);
            c.addChips(p.getUniqueId(), chipsToAdd, "Texas Holdem");
            data.playerSet.remove(p);
            if (players.isEmpty()) reset();
            return;
        }
        final Location l = npcLocation.clone().add( r.nextInt(3) * (r.nextBoolean() ? 1D : -1D), 0D, 3D);
        l.setYaw(l.getYaw() + 180F);
        p.teleport(l);
        data.playerSet.remove(p);
        lastAction.remove(p);
        for (int i = 0; i < data.playerHolograms.length; i++) {
            if (data.playerHolograms[i] == null) continue;
            c.ms.getHolograms().removeHologramsWithDisplayName(data.playerHolograms[i].getMessage());
            data.playerHolograms[i].remove();
            data.playerHolograms[i] = null;
        }
        updateHolograms();

        if (data.currentPlayer == p && players.isEmpty() == false) {
            if (players.size() >= data.currentPlayerIndex) data.currentPlayerIndex = 0;
            Player newPlayer = players.get(data.currentPlayerIndex);
            if (checkEnable == false && (lastAction.values().stream().allMatch(s -> s.contains("Erhöht") == false || s.contains("Nachgezogen")))) {
                int iterations = 0;
                while (newPlayer == null || (lastAction.containsKey(newPlayer) && (lastAction.get(newPlayer).contains("Erhöht") || lastAction.get(newPlayer).contains("Nachgezogen")))) {
                    if (iterations > players.size()) break;
                    data.currentPlayerIndex++;
                    if (players.size() >= data.currentPlayerIndex) data.currentPlayerIndex = 0;
                    newPlayer = players.get(data.currentPlayerIndex);
                    iterations++;
                }
            }
        }
        if (data.gameState.ordinal() <= GameState.WAITING_COOLDOWN.ordinal() && (realstakes.containsKey(p) || stakes.containsKey(p))) {
            c.addChips(p.getUniqueId(), data.stake, "Texas Holdem");
            data.gameState = GameState.WAITING;
        }
        if (th.gamenr.values().stream().noneMatch(integer -> integer == gn))
            isRunning = false;
        if (data.gameState != GameState.ENDING && data.gameState.ordinal() > GameState.WAITING_COOLDOWN.ordinal()) {
            if (players.size() == 1) {
                evaluation();
            }
        }
        if (players.isEmpty() && data.gameState.ordinal() <= GameState.WAITING_COOLDOWN.ordinal()) {
            reset();
        }
        if (data.pot >= data.stake) data.pot -= data.stake;
        clearSigns(p);
        stakes.remove(p);
        realstakes.remove(p);
    }

    public void close() {
        players.forEach(th.gamenr::remove);
        isRunning = false;
        players.forEach(player -> {
            int chipsToAdd = data.stake;
            if (data.totalPlayerInserts.containsKey(player))
                chipsToAdd += data.totalPlayerInserts.get(player);
            c.addChipsOffline(player.getUniqueId(), chipsToAdd, "Texas Holdem");
        });

        final List<Player> toEject = new ArrayList<>(players);

        cardsa.forEach(Entity::remove);
        cardsa.clear();
        players.clear();
        reset();
        toEject.forEach(player -> {
            if (player != null && player.isOnline()) {
                player.leaveVehicle();
            }
        });
    }

    private void addChairAtLocation(final Player player, final Location l) {
        final Block b = l.getBlock();
        float yaw = 0F;
        if (b.getData() == 1 || b.getData() == 9) {
            yaw = -90F;
        } else if (b.getData() == 3 || b.getData() == 11) {
            yaw = 0F;
        } else if (b.getData() == 0 || b.getData() == 8) {
            yaw = 90F;
        } else if (b.getData() == 2 || b.getData() == 10) {
            yaw = 180F;
        }
        l.setYaw(yaw);
        if (c.ms.getChairs().containsKey(l)) {
            return;
        } else if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }
        final ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(l.clone().add(0.5D, -1.2D, 0.5D), EntityType.ARMOR_STAND);
        stand.setGravity(false);
        stand.setVisible(false);
        player.teleport(l);
        stand.setPassenger(player);
        c.ms.getChairs().put(l, stand);
    }

    public void evaluation() {
        isRunning = false;
        try {
            final List<Object> winnings = getWinner();
            final Player winner = (Player) Objects.requireNonNull(winnings).get(0);
            final String with = (String) winnings.get(1);
            roundHologram.setMessage("§aGewinner: " + winner.getName());
            statusHologram.setMessage("§f" + with);
            data.gameState = GameState.ENDING;
            updateHolograms();
            c.getStats().POKER_GAMES++;
            c.getStats().POKER_EINSATZ += data.pot;
            c.addChips(winner, data.pot, "Texas Holdem");
            new PokerHistory(this, data, new CopyOnWriteArrayList<>(players), winner.getName());
            for (final Player s : players) {
                if (s.equals(winner) == false) {
                    c.ms.sendTitle(s, "§c§lVerloren", 0, 20 * 2, 5);
                    c.ms.sendSubTitle(s, "§f" + winner.getName() + " gewinnt mit: §a" + with, 0, 20 * 2, 5);
                } else {
                    c.ms.sendTitle(s, "§a§lGewonnen!", 0, 20 * 2, 5);
                    c.ms.sendSubTitle(s, "§fmit: §a" + with, 0, 20 * 2, 5);
                    s.sendMessage("§d§lMuxCasino>§a §lGewonnen!§7 Du hast §d" + c.ms.getNumberFormat(data.pot) + " Chips §7gewonnen!");
                }
            }
            new BukkitRunnable() {
                @Override public void run() {
                    ImmutableSet.copyOf(players).forEach(TexasHoldemGame.this::quit);
                    reset();
                }
            }.runTaskLater(c.ms, 60L);
        } catch (final Exception e) {
            players.forEach(this::quit);
            reset();
            e.printStackTrace();
        }
    }

    private List<Object> getWinner() {
        Player winner = null;
        int score = 0, score2 = 0;
        for (final Player p : players) {
            final int playerScore = Integer.parseInt(String.valueOf(getCombination(p).get(1))) * 1000 + Integer.parseInt(String.valueOf(getCombination(p).get(2))) * 100 + Integer.parseInt(String.valueOf(getCombination(p).get(3)));
            if (winner == null || playerScore > score) {
                winner = p;
                score = playerScore;
            }

        }
        for (final Player p : players) {
            final int playerScore = Integer.parseInt(String.valueOf(getCombination(p).get(1))) * 1000 + Integer.parseInt(String.valueOf(getCombination(p).get(2))) * 100 + Integer.parseInt(String.valueOf(getCombination(p).get(3)));
            if (playerScore == score && getCardValue(getPokerDeck(p).getCard(2)) > score2) {
                winner = p;
                score2 = getCardValue(getPokerDeck(p).getCard(2));
            }
        }
        if (winner != null) return Arrays.asList(winner, getCombination(winner).get(0));
        else return Collections.emptyList();
    }

    public void surrender(final Player pl) {
        quit(pl);
        pl.closeInventory();
        c.ms.sendTitle(pl, "§c§lVerloren!", 0, 20 * 2, 5);
        c.ms.sendSubTitle(pl, "§fDu hast §caufgegeben.", 0, 20 * 2, 5);
        pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 0.5f, 0.5f);
        data.gameState = GameState.PLAYER_SWITCH;
    }

    public void onUpdate() {
        if (isRunning == false) return;
        if (data.gameState == GameState.WAITING) {
            if (players.size() > 1) {
                data.gameState = GameState.WAITING_COOLDOWN;
                data.time = 11;
                for (final Player p : players) {
                    if (p != null) p.playSound(p.getLocation(), Sound.NOTE_PLING, 1f, 1f);
                }
            } else if (players.isEmpty()) {
                reset();
                return;
            }
            updateHolograms();
            if (statusHologram.getMessage().equalsIgnoreCase("§aWarte auf Spieler") == false)
                statusHologram.setMessage("§aWarte auf Spieler");
        } else if (data.gameState == GameState.WAITING_COOLDOWN) {
            checkReset();
            if (data.gameState == GameState.WAITING) return;
            data.pot = data.stake * players.size();
            updateHolograms();
            if (data.time-- > 1) {
                statusHologram.setMessage("§aSpiel startet in §7" + data.time);
                if (data.time <= 3) {
                    final Location slime1 = boardLocation.clone().add(-0.5, 0, 0), slime2 = boardLocation.clone().add(13.5, 0, 0);
                    for (final Player player : players) {
                        if (player.isOnline() == false) continue;
                        player.playSound(player.getLocation(), Sound.NOTE_STICKS, 1F, 10F);
                        c.ms.playEffect(player, EnumParticle.SLIME, slime1, 0.1F, 3F, 0.1F, 0.00001F, 140);
                        c.ms.playEffect(player, EnumParticle.SLIME, slime2, 0.1F, 3F, 0.1F, 0.00001F, 140);
                    }
                }
            } else {
                hideInfoHologram();
                c.ms.getAnalytics().getAnalytics().addPlayedCasinoGame("Texas Holdem");
                statusHologram.setMessage("§aSpiel gestartet");
                data.gameState = GameState.RESET;
                data.round = 0;
                resetGameDeck();
                players.forEach(player -> data.playerSet.put(player, 0));
                data.time = 0;
                if (roundHologram == null) {
                    roundHologram = c.ms.getHolograms().addHologram("thgr" + gn, boardLocation.clone().add(6, 2.6, 0), "§7Runde ( §e" + data.round + " §7/ §a4 §7)");
                }
                if (cardsHologram == null) {
                    cardsHologram = c.ms.getHolograms().addHologram("thgcc" + gn, boardLocation.clone().add(10.5, 2.3, 0), "§a§lDeine Karten");
                }
                for (final Player player : players) {
                    if (player.isOnline() == false) continue;
                    player.playSound(player.getLocation(), Sound.NOTE_PLING, 1F, 1F);
                    createPokerDeck(player);
                    final PokerDeck deck = decks.get(player);
                    deck.sendHolograms(player);
                }
            }
        } else if (data.gameState == GameState.RUNNING) {
            checkReset();
            if (data.gameState == GameState.WAITING)
                return;
            if (data.currentPlayer == null) {
                if (data.currentPlayerIndex >= players.size()) {
                    data.currentPlayerIndex = 0;
                    if (lastAction.values().stream().anyMatch(s -> s.contains("Erhöht") || s.contains("Nachgezogen"))
                            && lastAction.values().stream().allMatch(s -> s.contains("Erhöht") || s.contains("Nachgezogen")) == false) {
                        return;
                    }
                    data.gameState = GameState.RESET;
                    return;
                }
                data.currentPlayer = players.get(data.currentPlayerIndex);
                data.time = 30;

                c.ms.sendTitle(data.currentPlayer, "§a§lDu bist am Zug", 5, 20 * 3, 20);
                try {
                    players.forEach(player -> updateSign(player, player == data.currentPlayer));
                } catch (final Exception ignored) {
                }
                ImmutableMap.copyOf(overviewInventories).values().forEach(inventory -> ImmutableSet.copyOf(inventory.getViewers()).forEach(humanEntity -> openOverviewInv((Player) humanEntity)));
            } else {
                if (data.currentPlayer.isOnline() == false) {
                    data.gameState = GameState.PLAYER_SWITCH;
                    return;
                }
                if (data.time-- > 1) {
                    statusHologram.setMessage("§e" + data.time + " §f" + data.currentPlayer.getName() + " §aist am Zug");
                    updateCombinations();
                    updateHolograms();
                    return;
                }
                if (checkEnable) {
                    currentAction = 0;
                    data.gameState = GameState.PLAYER_SWITCH;
                    statusHologram.setMessage("§f" + data.currentPlayer.getName() + " hat§a gecheckt");
                    lastAction.put(data.currentPlayer, " §eGecheckt ");
                } else {
                    surrender(data.currentPlayer);
                    data.gameState = GameState.PLAYER_SWITCH;
                    statusHologram.setMessage("§f" + data.currentPlayer.getName() + " hat§c aufgegeben");
                    lastAction.put(data.currentPlayer, " §cAufgegeben ");
                }
                updateHolograms();
                for (final Player o : players) {
                    if (o != null) {
                        o.playSound(o.getLocation(), Sound.NOTE_PLING, 1f, 1f);
                    }
                }
            }
        } else if (data.gameState == GameState.RESET) {
            data.currentPlayerIndex = 0;
            data.round++;
            checkEnable = false;
            roundHologram.setMessage("§7Runde ( §e" + data.round + " §7/ §a4 §7)");
            if (data.round == 2) {
                int xLocation = 4;
                for (int i = 0; i < 3; i++) {
                    String cardString = getRandomCard();
                    data.globalCards[i] = cardString;
                    cardHolograms[i] = c.ms.getHolograms().addHologram("thgc" + gn + i, boardLocation.clone().add(xLocation, xLocation == 5 ? -0.5 : -1, 0.5), cardString);
                    xLocation++;
                }
            } else if (data.round == 3) {
                final String cardString = getRandomCard();
                data.globalCards[3] = cardString;
                cardHolograms[3] = c.ms.getHolograms().addHologram("thgc" + gn + 3, boardLocation.clone().add(7, -0.5, 0.5), cardString);
            } else if (data.round == 4) {
                final String cardString = getRandomCard();
                data.globalCards[4] = cardString;
                cardHolograms[4] = c.ms.getHolograms().addHologram("thgc" + gn + 4, boardLocation.clone().add(8, -1, 0.5), cardString);
            } else if (data.round == 5) {
                data.gameState = GameState.ENDING;
                evaluation();
                return;
            }
            for (final Player player : players) {
                final PokerDeck deck = decks.get(player);
                if (deck == null) continue;
                deck.updateHologram(player, deck.combi);
            }
            updateHolograms();
            statusHologram.setMessage("§7");
            lastAction.clear();
            data.currentPlayerIndex = 0;
            checkEnable = true;
            currentAction = 0;
            players.forEach(player -> data.playerSet.put(player, 0));
            data.time = 30;
            for (final Player o : players) {
                o.playSound(o.getLocation(), Sound.PISTON_EXTEND, 0.2f, 0.4f);
            }
            data.gameState = GameState.RUNNING;
            ImmutableMap.copyOf(overviewInventories).values().forEach(inventory -> ImmutableSet.copyOf(inventory.getViewers()).forEach(humanEntity -> openOverviewInv((Player) humanEntity)));
        } else if (data.gameState == GameState.PLAYER_SWITCH) {
            checkReset();
            updateHolograms();
            data.gameState = GameState.RUNNING;
            data.time = 30;
            data.currentPlayer = null;
            data.currentPlayerIndex++;
            for (final Player player : players) {
                final PokerDeck deck = decks.get(player);
                if (deck == null) continue;
                deck.updateHologram(player, deck.combi);
            }
            if (checkEnable == false) {
                boolean canSwitchRound = currentAction > 1 && data.playerSet.entrySet().stream().allMatch(entry -> data.playerSet.getOrDefault(entry.getKey(), 0) == currentAction);
                if (canSwitchRound) {
                    data.gameState = GameState.RESET;
                    return;
                } else {
                    boolean foundPlayer = false;
                    if (currentAction > 1) {
                        for (final Player player : players.stream().sorted(Comparator.comparingInt(value -> data.playerSet.getOrDefault(value, 0))).collect(Collectors.toList())) {
                            if ((data.playerSet.containsKey(player) == false) || data.playerSet.get(player) < currentAction) {
                                data.currentPlayerIndex = players.indexOf(player);
                                data.currentPlayer = player;
                                c.ms.sendTitle(data.currentPlayer, "§a§lDu bist am Zug", 5, 20 * 3, 20);
                                try {
                                    players.forEach(pl -> updateSign(pl, pl == data.currentPlayer));
                                } catch (final Exception ignored) {
                                }
                                foundPlayer = true;
                                break;
                            }
                        }
                    }
                    if (foundPlayer == false) {
                        data.currentPlayerIndex = 0;
                        data.currentPlayer = null;
                        data.gameState = GameState.RESET;
                    }
                }
            }
            ImmutableMap.copyOf(overviewInventories).values().forEach(inventory -> ImmutableSet.copyOf(inventory.getViewers()).forEach(humanEntity -> openOverviewInv((Player) humanEntity)));
        }
        updateCombinations();
    }

    private void updateCombinations() {
        overviewInventories.forEach((p, inv) -> {
            if (data.currentPlayer != null && data.currentPlayer.equals(p)) {
                final String comb = (String) getCombination(p).get(0);
                final ItemStack cooldown = c.ms.renameItemStack(new ItemStack(Material.WATCH, Math.max(0, data.time)),
                        "§f§lNoch §a§l" + data.time + " Sekunden §f§lam Zug",
                        (comb.contains("Royal") ? "§a" : "§7") + "Royal Flush - Höchster Straight Flush",
                        (comb.contains("Straight Flush") ? "§a" : "§7") + "Straight Flush - 5 gleichfarbige Karten in Reihe",
                        (comb.contains("4 Gleiche") ? "§a" : "§7") + "4 Gleiche - 4 gleichrangige Karten",
                        (comb.contains("Full House") ? "§a" : "§7") + "Full House - 3 und 2 gleichrangige Karten",
                        (comb.equals("Flush") ? "§a" : "§7") + "Flush - 5 gleichfarbige Karten",
                        (comb.equals("Straight") ? "§a" : "§7") + "Straight - 5 Karten in Reihe",
                        (comb.contains("3 Gleiche") ? "§a" : "§7") + "3 Gleiche - 3 gleichrangige Karten",
                        (comb.contains("2 Paare") ? "§a" : "§7") + "2 Paare - 2 Mal 2 gleichrangige Karten",
                        (comb.contains("1 Paar") ? "§a" : "§7") + "1 Paar - 2 gleichrangige Karten",
                        (comb.contains("Höchste Karte") ? "§a" : "§7") + "Höchste Karte - Karte mit dem höchsten Wert");
                inv.setItem(4, cooldown);
            }
        });
    }

    private void clearSigns(final Player player) {
        signs.forEach(sign -> {
            final EntityPlayer ep = ((CraftPlayer) player).getHandle();
            final net.minecraft.server.v1_8_R3.World world = ((CraftWorld) sign.getWorld()).getHandle();
            final BlockPosition bs = new BlockPosition(sign.getX(), sign.getY(), sign.getZ());
            ep.playerConnection.sendPacket(new net.minecraft.server.v1_8_R3.PacketPlayOutUpdateSign(world, bs, c.ms.getComponentFromSignLines(sign.getLines())));
        });
    }

    private void updateSign(final Player player, final boolean active) {
        final Optional<Sign> optional = signs.stream().min(Comparator.comparingDouble(value -> player.getLocation().distance(value.getLocation())));
        optional.ifPresent(sign -> {
            final String[] lines = new String[4];
            final EntityPlayer ep = ((CraftPlayer) player).getHandle();
            final net.minecraft.server.v1_8_R3.World world = ((CraftWorld) sign.getWorld()).getHandle();
            final BlockPosition bs = new BlockPosition(sign.getX(), sign.getY(), sign.getZ());
            if (active) {
                lines[0] = "§2▛▀▀▀▀▀▀▀▀▜";
                lines[1] = "§2Du bist am Zug.";
                lines[2] = "Klicke §lhier§0!";
                lines[3] = "§2▙▄▄▄▄▄▄▄▄▟";
            } else {
                lines[0] = "";
                lines[1] = "";
                lines[2] = "";
                lines[3] = "";
            }
            ep.playerConnection.sendPacket(new net.minecraft.server.v1_8_R3.PacketPlayOutUpdateSign(world, bs, c.ms.getComponentFromSignLines(lines)));
        });
    }

    public void updateHolograms() {
        if (winHologram == null) {
            winHologram = c.ms.getHolograms().addHologram("thgwh" + gn, boardLocation.clone().add(1.5, 2.3, 0), "§a§lGEWINN:");
        }
        if (winningsHologram == null) {
            winningsHologram = c.ms.getHolograms().addHologram("thgwih" + gn, boardLocation.clone().add(1.5, 1.9, 0), "§f" + c.ms.getNumberFormat(data.pot) + " §dChips");
        }
        double yLoc = 1;
        for (int i = 0; i < data.playerHolograms.length; i++) {
            if (players.size() == i) break;
            final Player player = players.get(i);
            if (player.isOnline() == false) {
                if (data.playerHolograms[i] != null) {
                    data.playerHolograms[i] = null;
                    c.ms.getHolograms().removeHologram("thp" + gn + i);
                }
                continue;
            }
            final String color = data.gameState.ordinal() <= GameState.WAITING_COOLDOWN.ordinal() ? "§7" : players.indexOf(player) == data.currentPlayerIndex ? "§a" : "§c",
                    message = color + ">" + " §f" + player.getName() + " " + (lastAction.containsKey(player) ? ("(" + lastAction.get(player) + "§f) ") : "") + color + "<";
            if (data.playerHolograms[i] == null) {
                data.playerHolograms[i] = c.ms.getHolograms().addHologram("thp" + gn + i, boardLocation.clone().add(6, yLoc, 0), message);
            } else {
                data.playerHolograms[i].setMessage(message);
            }
            yLoc += 0.3;
        }
        winningsHologram.setMessage("§f" + c.ms.getNumberFormat(data.pot) + " §dChips");
        for (final Player pl : players) {
            final PokerDeck deck = decks.get(pl);
            if (deck == null) continue;
            deck.updateHologram(pl, deck.combi);
        }
    }

    private void createPokerDeck(final Player p) {
        decks.put(p, new PokerDeck(getRandomCard(), getRandomCard()));
    }

    private PokerDeck getPokerDeck(final Player p) {
        return decks.get(p);
    }

    private String getRandomCard() {
        final int card = r.nextInt(cards.size());
        return cards.remove(card);
    }

    private void resetGameDeck() {
        cards.clear();
        for (int f = 0; f < 4; f++) {
            for (int i = 2; i <= 14; i++) {
                String color = "§c❤";
                if (f == 1) color = "§c✦";
                else if (f == 2) color = "§8✢";
                else if (f == 3) color = "§8Ω";

                if (i <= 10) cards.add(color + " §f§l" + i + " " + color);
                else if (i == 11) cards.add(color + " §f§lBube " + color);
                else if (i == 12) cards.add(color + " §f§lDame " + color);
                else if (i == 13) cards.add(color + " §f§lKönig " + color);
                else if (i == 14) cards.add(color + " §f§lAss " + color);
            }
        }
    }

    public void reset() {
        decks.values().forEach(pokerDeck -> {
            c.ms.getHolograms().removeIndividualHologram(pokerDeck.h1);
            c.ms.getHolograms().removeIndividualHologram(pokerDeck.h2);
            c.ms.getHolograms().removeIndividualHologram(pokerDeck.combi);
        });
        for (final MuxHolograms.Hologram cardHologram : cardHolograms) {
            if (cardHologram != null) c.ms.getHolograms().removeHologramsWithDisplayName(cardHologram.getMessage());
        }
        for (final MuxHolograms.Hologram playerHologram : data.playerHolograms) {
            if (playerHologram != null) c.ms.getHolograms().removeHologramsWithDisplayName(playerHologram.getMessage());
        }
        if (roundHologram != null) c.ms.getHolograms().removeHologramsWithDisplayName(roundHologram.getMessage());
        if (cardsHologram != null) c.ms.getHolograms().removeHologramsWithDisplayName(cardsHologram.getMessage());
        if (statusHologram != null) c.ms.getHolograms().removeHologramsWithDisplayName(statusHologram.getMessage());
        if (winningsHologram != null) c.ms.getHolograms().removeHologramsWithDisplayName(winningsHologram.getMessage());
        if (winHologram != null) c.ms.getHolograms().removeHologramsWithDisplayName(winHologram.getMessage());
        players.clear();
        data.currentPlayerIndex = 0;
        data.totalPlayerInserts.clear();
        data.currentPlayer = null;
        data.playerSet.clear();
        lastAction.clear();
        currentAction = 0;
        decks.clear();
        cards.clear();
        data.gameState = GameState.WAITING;
        showInfoHologram();
        Arrays.fill(cardHolograms, null);
        Arrays.fill(data.globalCards, null);
        Arrays.fill(data.playerHolograms, null);
        this.roundHologram = this.cardsHologram = this.statusHologram = this.winningsHologram = this.winHologram = null;
        isRunning = false;
    }

    private void checkReset() {
        if (players.size() == 1 && data.gameState != GameState.ENDING) {
            if (data.gameState.ordinal() >= 2) {
                final Player winner = players.get(0);
                c.ms.sendTitle(winner, "§a§lGewonnen", 0, 20, 5);
                c.ms.sendTitle(winner, "§f" + c.ms.getNumberFormat(data.pot) + " §dChips", 0, 20, 5);
                winner.playSound(winner.getLocation(), Sound.ENDERDRAGON_GROWL, 1f, 1f);
                winner.playSound(winner.getLocation(), Sound.FIREWORK_LAUNCH, 1f, 1f);
                c.ms.showItemBar(winner, "§cDein Gegner hat aufgegeben.");
                quit(winner);
                c.addChips(winner, data.pot, "Texas Holdem");
                reset();
            }
            data.gameState = GameState.WAITING;
            statusHologram.setMessage("§aWarte auf Spieler");
            return;
        }
        if (players.isEmpty()) {
            reset();
        }
    }

    private List<Object> getCombination(final Player p) {
        final PokerDeck playerDeck = decks.get(p);
        final List<String> allCards = new ArrayList<>(Arrays.asList(playerDeck.c1, playerDeck.c2));
        for (final String globalCard : data.globalCards) {
            if (globalCard != null) {
                allCards.add(globalCard);
            }
        }

        int category = 0; // 0 = highcard , 1 = pair
        int highscore = getCardValue(allCards.get(0)), highcardscore = getCardValue(allCards.get(0));
        String with = "Höchste Karte " + allCards.get(0);
        if (cardHigherThan(allCards.get(1), allCards.get(0))) {
            with = "Höchste Karte " + allCards.get(1);
            highscore = getCardValue(allCards.get(1));
            highcardscore = getCardValue(allCards.get(1));
        }
        // Pair Check
        final Map<Integer, Integer> paircheck = new HashMap<>();
        for (final String card : allCards) {
            final int value = getCardValue(card);
            if (paircheck.containsKey(value)) paircheck.put(value, paircheck.get(value) + 1);
            else paircheck.put(value, 1);
        }

        for (final Map.Entry<Integer, Integer> entry : paircheck.entrySet()) {
            final int s = entry.getKey(), pairs = entry.getValue();
            if (pairs == 2) {
                category = 1;
                highscore = s;
                with = "1 Paar";
                for (final Map.Entry<Integer, Integer> entry2 : paircheck.entrySet()) {
                    final int a = entry2.getKey(), pairs2 = entry2.getValue();
                    if (a != s && pairs2 == 2) {
                        category = 2;
                        highscore = Math.max(a, s);
                        with = "2 Paare";
                    }
                    if (a != s && pairs2 == 3) {
                        category = 6;
                        highscore = Math.max(a, s);
                        with = "Full House";
                    }
                }
            }
            if (category < 3 && pairs == 3) {
                category = 3;
                highscore = s;
                with = "3 Gleiche";
            }
            if (pairs == 4) {
                category = 7;
                highscore = s;
                with = "4 Gleiche";
            }
        }

        // Check Flush
        final Map<String, Integer> colorCounter = new HashMap<>();
        colorCounter.put("❤", 0);
        colorCounter.put("✦", 0);
        colorCounter.put("✢", 0);
        colorCounter.put("Ω", 0);
        for (final String ca : allCards) {
            if (ca.contains("❤")) {
                colorCounter.put("❤", colorCounter.get("❤") + 1);
            }
            if (ca.contains("✦")) {
                colorCounter.put("✦", colorCounter.get("✦") + 1);
            }
            if (ca.contains("✢")) {
                colorCounter.put("✢", colorCounter.get("✢") + 1);
            }
            if (ca.contains("Ω")) {
                colorCounter.put("Ω", colorCounter.get("Ω") + 1);
            }
        }
        String flushcolor = "Ω";
        for (final Map.Entry<String, Integer> entry : colorCounter.entrySet()) {
            final String s = entry.getKey();
            if (entry.getValue() >= 5) {
                category = 5;
                with = "Flush";
                flushcolor = s;
            }
        }

        // Check Street
        int streetLength = 0;
        boolean equalsFlushColor = true;
        for (int i = 1; i < 14; i++) {
            if (streetLength < 5) {
                boolean found = false;
                for (final String s : allCards) {
                    if (found == false && ((i == 1 && getCardValue(s) == 14) || getCardValue(s) == i)) {
                        streetLength++;
                        found = true;
                        if (s.contains(flushcolor) == false) equalsFlushColor = false;
                    }
                }
                if (found == false) {
                    streetLength = 0;
                    equalsFlushColor = true;
                }
            }
        }
        if (streetLength >= 5) {
            if (category == 5 && equalsFlushColor) {
                category = 8;
                with = "Straight Flush";

                final List<List<String>> royalFlush = new ArrayList<>();
                final List<String> colors = Arrays.asList("§c❤", "§c✦", "§8✢", "§8Ω");
                for (final String ca : colors) {
                    final List<String> royalColors = new ArrayList<>();
                    royalColors.add(ca + " §f§l10 "+ ca);
                    royalColors.add(ca + " §f§lBube "+ ca);
                    royalColors.add(ca + " §f§lDame "+ ca);
                    royalColors.add(ca + " §f§lKönig "+ ca);
                    royalColors.add(ca + " §f§lAss "+ ca);
                    royalFlush.add(royalColors);
                }
                for (final List<String> colorflush : royalFlush) {
                    boolean foundOther = false;
                    for (final String s : colorflush) {
                        if (allCards.contains(s) == false) {
                            foundOther = true;
                            break;
                        }
                    }
                    if (foundOther == false) {
                        category = 9;
                        with = "Royal Flush";
                        break;
                    }
                }
            } else {
                if (category < 4) {
                    category = 4;
                    with = "Straight";
                }
            }
        }
        return Arrays.asList(with, category, highscore, highcardscore);
    }

    private int getCardValue(final String card) {
        return Integer.parseInt(ChatColor.stripColor(card).replace("Bube", "11").replace("Dame", "12").replace("König", "13").
                replace("Ass", "14").replace("❤", "").replace("✦", "")
                .replace("✢", "").replace("Ω", "").replace(" ", ""));
    }

    private boolean cardHigherThan(final String card, final String sec) {
        final Map<String, Integer> highcards = new HashMap<>();
        for (int f = 0; f < 4; f++) {
            for (int i = 2; i <= 14; i++) {
                String color = "§c❤";
                if (f == 1) color = "§c✦";
                else if (f == 2) color = "§8✢";
                else if (f == 3) color = "§8Ω";

                if (i <= 10) highcards.put(color + " §f§l" + i + " " + color, 13 + i);
                else if (i == 11) highcards.put(color + " §f§lBube " + color, 13 + i);
                else if (i == 12) highcards.put(color + " §f§lDame " + color, 13 + i);
                else if (i == 13) highcards.put(color + " §f§lKönig " + color, 13 + i);
                else if (i == 14) highcards.put(color + " §f§lAss " + color, 13 + i);
            }
        }
        return highcards.containsKey(card) && highcards.containsKey(sec) && highcards.get(card) > highcards.get(sec);
    }

    public MuxHolograms.Hologram getStatusHologram() {
        return statusHologram;
    }

    public List<Player> getPlayers() {
        return players;
    }

    class PokerDeck {
        public String c1, c2;
        public MuxHolograms.IndividualHologram h1, h2, combi = c.ms.getHolograms().addIndividualHologram("tholdem", boardLocation.clone().add(10.5, 1.1, 0), (holo, p) -> {
            if (data.gameState.ordinal() < GameState.RUNNING.ordinal())
                holo.setName(p, "§7§oDerzeitige Streak: §cKeine");
            else
                holo.setName(p, "§7§oStreak: §d" + getCombination(p).get(0));
        }, false);

        public PokerDeck(final String card1, final String card2) {
            this.c1 = card1;
            this.c2 = card2;
            this.h1 = c.ms.getHolograms().addIndividualHologram("tholdem", boardLocation.clone().add(10.5, 1.9, 0), (holo, p) -> holo.setName(p, c1), false);
            this.h2 = c.ms.getHolograms().addIndividualHologram("tholdem", boardLocation.clone().add(10.5, 1.6, 0), (holo, p) -> holo.setName(p, c2), false);
        }

        public void sendHolograms(final Player player) {
            sendHologram(player, h1);
            sendHologram(player, h2);
            sendHologram(player, combi);
        }

        private void sendHologram(final Player player, final MuxHolograms.IndividualHologram hologram) {
            hologram.getPlayersTracking().add(player);
            hologram.send(player);
            hologram.getAction().call(hologram, player);
        }

        public void updateHologram(final Player player, final MuxHolograms.IndividualHologram hologram) {
            if (hologram.getPlayersTracking().contains(player) == false) {
                hologram.getPlayersTracking().add(player);
                hologram.send(player);
            }
            hologram.forceRefresh();
        }

        public String getCard(final int cardNr) {
            return cardNr == 1 ? c1 : cardNr == 2 ? c2 : null;
        }
    }

    class GameData {
        private int time = 0, round = 0;
        private GameState gameState = GameState.WAITING;
        private final String[] globalCards = new String[5];
        private final MuxHolograms.Hologram[] playerHolograms = new MuxHolograms.Hologram[4];
        private int pot, stake, currentPlayerIndex = 0;
        private Player currentPlayer = null;
        private final Map<Player, Integer> playerSet = new HashMap<>(), totalPlayerInserts = new HashMap<>();

        public int getPot() {
            return pot;
        }

        public Map<Player, Integer> getTotalPlayerInserts() {
            return totalPlayerInserts;
        }

        public Map<Player, Integer> getPlayerSet() {
            return playerSet;
        }



        public void setPot(final int pot) {
            boolean update = this.pot == 0;
            this.pot = pot;
            if (update) updateHolograms();
        }

        public Player getCurrentPlayer() {
            return currentPlayer;
        }

        public void setStake(final int stake) {
            this.stake = stake;
        }

        public void setGameState(final GameState gameState) {
            this.gameState = gameState;
        }

        public int getStake() {
            return stake;
        }

        public GameState getGameState() {
            return gameState;
        }
    }

    enum GameState {
        WAITING,
        WAITING_COOLDOWN,
        RUNNING,
        RESET,
        PLAYER_SWITCH,
        ENDING
    }

    class PokerHistory {
        private final Set<String> playerNames = new HashSet<>(), tableCards = new HashSet<>();
        private final Map<String, String> playerCard1 = new HashMap<>(), playerCard2 = new HashMap<>(), playerCombination = new HashMap<>();
        private final int pot;
        private final String winner;

        public PokerHistory(final TexasHoldemGame game, final GameData data, final List<Player> players, final String winner) {
            tableCards.addAll(Arrays.asList(data.globalCards));
            this.winner = winner;
            this.pot = data.pot;
            for (final Player p : players) {
                final String pname = p.getName();
                playerNames.add(pname);
                playerCard1.put(pname, game.getPokerDeck(p).getCard(1));
                playerCard2.put(pname, game.getPokerDeck(p).getCard(2));
                playerCombination.put(pname, (String) game.getCombination(p).get(0));
                game.th.gamehistory.put(p, this);
            }
        }

        public ItemStack getStatsFor(final Player p, final boolean withbuy) {
            final Date now = new Date();
            final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
            format.setTimeZone(TimeZone.getTimeZone("CET"));

            final ItemStack lastRound = new ItemStack(Material.PAPER);
            final ItemMeta lastRoundMeta = lastRound.getItemMeta();
            lastRoundMeta.setDisplayName("§f" + p.getName() + "'s Pokerrunde am §d" + format.format(now));
            final List<String> list = new ArrayList<>();
            list.add("§fGewinn: §d" + c.ms.getNumberFormat(pot) + " Chips");
            list.add("§fTischkarten:");
            int step = 1;
            for (final String card : tableCards) {
                if (card == null) {
                    list.add("§dKeine");
                    continue;
                }
                list.add("§d" + step + ". " + card);
                step++;
            }
            list.add("");
            list.add("§fSpieler:");
            for (final String player : playerNames) {
                list.add("§d" + player + (player.equals(winner) ? " §a§lGEWINNER" : ""));
                list.add("    §fKarten: " + playerCard1.get(player) + "§f, " + playerCard2.get(player));
                list.add("    §fBeste Kombination: §d" + playerCombination.get(player));
            }
            if (withbuy) {
                list.add("");
                list.add("§aKaufe §7diese Erinnerung für §d10.000 Chips§7.");
            }
            lastRoundMeta.setLore(list);
            lastRoundMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            lastRound.setItemMeta(lastRoundMeta);
            return lastRound;
        }
    }
}