package me.muxteam.casino;

import me.muxteam.basic.MuxPlayerNPC;
import me.muxteam.basic.NMSReflection;
import me.muxteam.muxsystem.MuxInventory;
import net.minecraft.server.v1_8_R3.PacketPlayOutAnimation;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class RockPaperScissorsGame {
    private final RockPaperScissors rps;

    public Player p1, p2;
    public int choicep1 = 0, choicep2 = 0, step = 0, p1slot, p2slot;
    private final Location loc;
    private final MuxCasino c;
    public Map<Player, Integer> stakes = new HashMap<>();
    public Map<Player, Integer> realstakes = new HashMap<>();
    public int stake = 500;
    public GameState state = GameState.WAITING;

    public RockPaperScissorsGame(final Location loc, final MuxCasino c, final RockPaperScissors rps) {
        this.rps = rps;
        this.loc = loc.clone();
        this.c = c;
    }

    public Location getLocation() {
        return loc;
    }

    public boolean isPlaying(final Player p) {
        return p1 == p || p2 == p;
    }

    public void updateInv() {
        if (p1 != null) openInv(p1);
        if (p2 != null) openInv(p2);
    }

    public void openChooseInv() {
        if (p1 == null || p2 == null) {
            resetGame();
            return;
        }
        if (stakes.containsKey(p1) == false || stakes.containsKey(p2) == false) {
            resetGame();
            return;
        }
        openChooseInventory(p1);
        openChooseInventory(p2);
    }

    public void openChooseInventory(final Player p) {
        final Inventory inv = c.ms.getServer().createInventory(null, InventoryType.HOPPER, "§0§lMuxCasino§0 | Schere Stein Papier");
        inv.setItem(0, c.ms.renameItemStack(new ItemStack(Material.SHEARS), "§6§lSchere"));
        inv.setItem(2, c.ms.renameItemStack(new ItemStack(Material.STONE), "§7§lStein"));
        inv.setItem(4, c.ms.renameItemStack(new ItemStack(Material.PAPER), "§f§lPapier"));
        if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
        p.openInventory(inv);
        c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
    }

    public void onUpdate() {
        if (c.isEnabled() == false || (state == GameState.RUNNING && step == 3)) return;
        if (p1 != null && (c.inCasino(p1) == false || p1.getLocation().distance(loc) > 4 || p1.isInsideVehicle() == false)) {
            c.addChips(p1, stakes.get(p1), "Schere Stein Papier");
            c.getStats().RPS_GESETZT -= stakes.get(p1);
            c.ms.showItemBar(p1, "§cDu hast Schere Stein Papier verlassen.");
            rps.gamenr.remove(p1);
            p1 = null;
            resetGame();
        }
    }
    public void openInv(final Player p) {
        if (p1 == p) {
            c.ms.showItemBar(p, "§cWarte auf einen Mitspieler.");
            p.playSound(p.getLocation(), Sound.VILLAGER_NO, 1F, 1F);
            return;
        }
        if (p1 != null && (c.inCasino(p1) == false || p1.getLocation().distance(loc) > 4 || p1.isInsideVehicle() == false)) {
            c.addChips(p1, stakes.get(p1), "Schere Stein Papier");
            c.getStats().RPS_GESETZT -= stakes.get(p1);
            c.ms.showItemBar(p1, "§cDu hast Schere Stein Papier verlassen.");
            p1 = null;
            resetGame();
        }
        int staked = stakes.getOrDefault(p, 0);
        if (staked == 0 || staked > c.getChips(p)) {
            staked = 500;
            stakes.put(p, 500);
        }

        final Inventory inv = c.ms.getServer().createInventory(null, 54, "§0§lMuxCasino§0 | Schere Stein Papier");
        ItemStack go = c.ms.renameItemStack(new ItemStack(Material.EMERALD_BLOCK, 1), p1 == null ? "§a§lErstellen" : "§a§lBeitreten",
                p1 == null ? new String[]{"§7Klicke, um das Spiel", "§7zu §aerstellen§7."} :
                        new String[]{"§7Einsatz: §d" + c.ms.getNumberFormat(stakes.get(p1)) + " Chips", "", "§7Klicke, um dem Spiel §abeizutreten§7."});
        c.createButton(inv, 21, go);
        if (p1 == null) {
            inv.setItem(4, c.ms.renameItemStack(new ItemStack(Material.INK_SACK, 1, (byte) 13), "§f§lEinsatz: §d§l" + c.ms.getNumberFormat(staked) + " Chips", "",
                    "§aLinksklick§7, für §d+" + c.ms.getNumberFormat(c.getNextStakeChange(staked, true)) + " Chips",
                    "§cRechtsklick§7, für §d-" + c.ms.getNumberFormat(c.getNextStakeChange(staked, false)) + " Chips"));
        }
        if (c.ms.getActiveInv(p.getName()) != MuxInventory.InvType.CASINO) p.closeInventory();
        p.openInventory(inv);
        rps.gamenr.put(p, this);
        c.ms.setActiveInv(p.getName(), MuxInventory.InvType.CASINO);
    }
    private void playArmAnimation(final Player p) {
        final PacketPlayOutAnimation packet = new PacketPlayOutAnimation();
        setValue(packet, "a", p.getEntityId());
        setValue(packet, "b", MuxPlayerNPC.Animation.SWING_ARM.getValue());
        ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
    }
    private void setValue(final Object instance, final String field, final Object value) {
        NMSReflection.setObject(instance.getClass(), field, instance, value);
    }
    public void start(final GameState state) {
        this.state = state;
        c.ms.getAnalytics().getAnalytics().addPlayedCasinoGame("Schere Stein Papier");
        if (state == GameState.CHOOSING) {
            openChooseInv();
        } else if (state == GameState.RUNNING) {
            stake = stakes.get(p1);
            p1.closeInventory();
            p2.closeInventory();
            if (p1.getInventory().firstEmpty() == -1) {
                c.ms.showItemBar(p1, "§cDein Inventar ist voll.");
                resetGame();
                return;
            }
            if (p1.getInventory().firstEmpty() > 0) {
                p1slot = 0;
                p1.getInventory().setItem(p1.getInventory().firstEmpty(), p1.getInventory().getItem(0));
                p1.getInventory().setItem(p1slot, new ItemStack(Material.AIR));
            }
            if (p2.getInventory().firstEmpty() > 0) {
                p2slot = 0;
                p2.getInventory().setItem(p1.getInventory().firstEmpty(), p1.getInventory().getItem(0));
                p2.getInventory().setItem(p2slot, new ItemStack(Material.AIR));
            }
            runGame();
        }
    }
    private void runGame() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (p1 == null || p1.isOnline() == false) {
                    if (p2 != null && p2.isOnline() == false && realstakes.containsKey(p2))
                        c.addChips(p2, realstakes.get(p2), "Schere Stein Papier");
                    resetGame();
                    cancel();
                    return;
                }
                if (p2 == null || p2.isOnline() == false) {
                    if (p1.isOnline() && realstakes.containsKey(p1))
                        c.addChips(p1, realstakes.get(p1), "Schere Stein Papier");
                    resetGame();
                    cancel();
                    return;
                }
                c.getStats().RPS_GESETZT += stake * 2L;
                c.getStats().RPS_GAMES++;
                p1.getInventory().setHeldItemSlot(0);
                p2.getInventory().setHeldItemSlot(0);
                step++;
                p1.closeInventory();
                p2.closeInventory();
                if (step == 1) {
                    c.ms.sendTitle(p1, "§fSchere", 1, 15, 1);
                    c.ms.sendTitle(p2, "§fSchere", 1, 15, 1);
                } else if (step == 2) {
                    c.ms.sendTitle(p1, "§fStein", 1, 15, 1);
                    c.ms.sendTitle(p2, "§fStein", 1, 15, 1);
                } else if (step == 3) {
                    c.ms.sendTitle(p1, "§fPapier", 1, 15, 1);
                    c.ms.sendTitle(p2, "§fPapier", 1, 15, 1);
                    p1slot = 0;
                    p2slot = 0;
                    p1.getInventory().setItem(p1slot, choicep1 == 1 ? new ItemStack(Material.SHEARS) : choicep1 == 2 ? new ItemStack(Material.STONE) : new ItemStack(Material.PAPER));
                    p2.getInventory().setItem(p2slot, choicep2 == 1 ? new ItemStack(Material.SHEARS) : choicep2 == 2 ? new ItemStack(Material.STONE) : new ItemStack(Material.PAPER));
                    p1.getInventory().setHeldItemSlot(p1slot);
                    p2.getInventory().setHeldItemSlot(p2slot);
                    checkState();
                    cancel();
                }
                p1.playSound(p1.getLocation(), Sound.CLICK, 1F, 1F);
                p2.playSound(p2.getLocation(), Sound.CLICK, 1F, 1F);
                playArmAnimation(p1);
                playArmAnimation(p2);
            }
        }.runTaskTimer(c.ms, 20L, 15L);
    }
    public void checkStart() {
        if (p2 == p1) {
            resetGame();
            return;
        }
        if (p1 == null || p2 == null) {
            resetGame();
            return;
        }
        if (p1.isOnline() == false || p2.isOnline() == false) {
            resetGame();
            return;
        }
        if (choicep1 != 0 && choicep2 != 0) {
            start(GameState.RUNNING);
        }
    }
    private void checkState() {
        realstakes.clear();
        if (p2 == p1) {
            resetGame();
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                findWinner();
            }
        }.runTaskLater(c.ms, 35L);
    }
    private void findWinner() {
        Player winner = null;
        if (choicep1 == 1 && choicep2 == 3) winner = p1;
        else if (choicep1 == 1 && choicep2 == 2) winner = p2;
        else if (choicep1 == 2 && choicep2 == 1) winner = p1;
        else if (choicep1 == 2 && choicep2 == 3) winner = p2;
        else if (choicep1 == 3 && choicep2 == 1) winner = p2;
        else if (choicep1 == 3 && choicep2 == 2) winner = p1;
        String message = "§eUnentschieden - Keiner hat gewonnen";
        if (winner == null && (p1 == null || p2 == null)) {
            if (p1 == null) winner = p2;
            else winner = p1;
        }
        if (winner == null) {
            c.ms.showItemBar(p1, message);
            p1.playSound(p1.getLocation(), Sound.NOTE_BASS, 1F, 1F);
            p1.getInventory().setHeldItemSlot(p1slot);
            p1.getInventory().setItem(p1slot, new ItemStack(Material.AIR));
            p1.closeInventory();
            c.ms.showItemBar(p2, message);
            p2.playSound(p2.getLocation(), Sound.NOTE_BASS, 1F, 1F);
            p2.getInventory().setHeldItemSlot(p1slot);
            p2.getInventory().setItem(p2slot, new ItemStack(Material.AIR));
            p2.closeInventory();
            choicep1 = choicep2 = step = 0;
            p1slot = p2slot = 0;
            c.ms.runLater(new BukkitRunnable() {
                @Override
                public void run() {
                    start(GameState.CHOOSING);
                }
            }, 20L);
            return;
        }
        final Player pl1 = p1, pl2 = p2;
        if (p1 != null && winner == p1) {
            message = "§d" + p1.getName() + " §ahat gewonnen! §7(§d" + c.ms.getNumberFormat(stake * 2L) + " Chips§7)";
            c.ms.itemRain(p1.getLocation(), choicep1 == 1 ? Material.SHEARS : choicep1 == 2 ? Material.STONE : Material.PAPER);
            final Sound sound = choicep1 == 1 ? Sound.SHEEP_SHEAR : choicep1 == 2 ? Sound.STEP_STONE : Sound.DIG_SNOW;
            c.ms.runLater(new BukkitRunnable() {
                @Override
                public void run() {
                    if (pl1.isOnline()) pl1.playSound(pl1.getLocation(), sound, 1F, 1F);
                    if (pl2 != null) pl2.playSound(pl2.getLocation(), sound, 1F, 1F);
                }
            }, 10L);
            c.ms.runLater(new BukkitRunnable() {
                @Override
                public void run() {
                    if (pl1.isOnline()) pl1.playSound(pl1.getLocation(), sound, 1F, 1F);
                    if (pl2 != null) pl2.playSound(pl2.getLocation(), sound, 1F, 1F);
                }
            }, 30L);
            if (p1.isOnline())
                c.addChips(p1, stake * 2, "Schere Stein Papier");
            else
                c.addChipsOffline(p1.getUniqueId(), stake * 2, "Schere Stein Papier");
        }
        if (p2 != null && winner == p2) {
            message = "§d" + p2.getName() + " §ahat gewonnen! §7(§d" + c.ms.getNumberFormat(stake * 2L) + " Chips§7)";
            c.ms.itemRain(p2.getLocation(), choicep2 == 1 ? Material.SHEARS : choicep2 == 2 ? Material.STONE : Material.PAPER);
            final Sound sound = choicep2 == 1 ? Sound.SHEEP_SHEAR : choicep2 == 2 ? Sound.STEP_STONE : Sound.DIG_SNOW;
            c.ms.runLater(new BukkitRunnable() {
                @Override
                public void run() {
                    if (pl1 != null) pl1.playSound(pl1.getLocation(), sound, 1F, 1F);
                    if (pl2.isOnline()) pl2.playSound(pl2.getLocation(), sound, 1F, 1F);
                }
            }, 10L);
            c.ms.runLater(new BukkitRunnable() {
                @Override
                public void run() {
                    if (pl1 != null) pl1.playSound(pl1.getLocation(), sound, 1F, 1F);
                    if (pl2.isOnline()) pl2.playSound(pl2.getLocation(), sound, 1F, 1F);
                }
            }, 30L);
            if (p2.isOnline())
                c.addChips(p2, stake * 2, "Schere Stein Papier");
            else
                c.addChipsOffline(p2.getUniqueId(), stake * 2, "Schere Stein Papier");
        }
        if (p1 != null && p1.isOnline()) {
            rps.gamenr.remove(p1);
            c.ms.showItemBar(p1, message);
            p1.playSound(p1.getLocation(), Sound.LEVEL_UP, 1F, 0.1F);
            p1.getInventory().setHeldItemSlot(p1slot);
            p1.getInventory().setItem(p1slot, new ItemStack(Material.AIR));
            p1.closeInventory();
        }
        if (p2 != null && p2.isOnline()) {
            rps.gamenr.remove(p2);
            c.ms.showItemBar(p2, message);
            p2.playSound(p2.getLocation(), Sound.LEVEL_UP, 1F, 0.1F);
            p2.getInventory().setHeldItemSlot(p2slot);
            p2.getInventory().setItem(p2slot, new ItemStack(Material.AIR));
            p2.closeInventory();
        }
        p1 = p2 = null;
        resetGame();
    }
    public void resetGame() {
        if (p1 != null) {
            c.ms.showItemBar(p1, "§cSpiel musste abgebrochen werden.");
            if (realstakes.containsKey(p1)) {
                c.addChips(p1, realstakes.get(p1), "Schere Stein Papier");
                c.getStats().RPS_GESETZT -= realstakes.get(p1);
            }
            p1.closeInventory();
        }
        if (p2 != null) {
            c.ms.showItemBar(p2, "§cSpiel musste abgebrochen werden.");
            if (realstakes.containsKey(p2)) {
                c.addChips(p2, realstakes.get(p2), "Schere Stein Papier");
                c.getStats().RPS_GESETZT -= realstakes.get(p2);
            }
            p2.closeInventory();
        }
        p1 = p2 = null;
        choicep1 = choicep2 = 0;
        step = 0;
        stake = 500;
        stakes.clear();
        state = GameState.WAITING;
        realstakes.clear();
    }
    public void quit(final Player p) {
        if (state == GameState.RUNNING && step == 3) {
            findWinner();
            return;
        }
        if (p2 != null && p2.equals(p)) {
            p2 = null;
            if (realstakes.containsKey(p)) {
                c.addChips(p, realstakes.get(p), "Schere Stein Papier");
                c.getStats().RPS_GESETZT -= realstakes.get(p);
            }
            resetGame();
            if (step == 0) c.ms.showItemBar(p, "§cDu hast das Spiel verlassen.");
        } else if (p1 != null && p1.equals(p)) {
            p1 = null;
            if (realstakes.containsKey(p)) {
                c.addChips(p, realstakes.get(p), "Schere Stein Papier");
                c.getStats().RPS_GESETZT -= realstakes.get(p);
            }
            resetGame();
            if (step == 0) c.ms.showItemBar(p, "§cDu hast das Spiel verlassen.");
        }
        rps.gamenr.remove(p);

    }
    public void interact(final Player p) {
        if (state != GameState.WAITING) {
            c.ms.showItemBar(p, "§cDieses Spiel läuft bereits.");
            return;
        }
        if (p1 != null && p2 != null) {
            c.ms.showItemBar(p, "§cDieses Spiel ist derzeit besetzt.");
            return;
        }
        if (p.getInventory().firstEmpty() == -1) {
            c.ms.showItemBar(p, "§cDein Inventar ist voll.");
            return;
        }
        if (p.getInventory().firstEmpty() > 0) {
            p.getInventory().setItem(p.getInventory().firstEmpty(), p.getInventory().getItem(0));
            p.getInventory().setItem(0, new ItemStack(Material.AIR));
        }
        if ((p1 != null && p1.equals(p)) || (p2 != null && p2.equals(p))) {
            updateInv();
        } else {
            openInv(p);
        }
    }
    public enum GameState {
        WAITING,
        CHOOSING,
        RUNNING
    }
}