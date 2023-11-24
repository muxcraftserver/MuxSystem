package me.muxteam.events;

import me.muxteam.muxsystem.MuxInventory;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MuxUser;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@NonJoinableEvent
public class MuxButton extends Event {
    private int sec, jackpot = 0;
    private final Set<UUID> clicked = new HashSet<>();
    private Player winner = null;

    public MuxButton(final MuxEvents e) {
        super(e);
        name = "MuxButton";
        item = new ItemStack(Material.SEA_LANTERN);
        leaveInventoryOpen = true;
        final FileConfiguration hashYML = ms.getHashYAML();
        if (hashYML.contains("eventbutton")) {
            jackpot = hashYML.getInt("eventbutton");
            sec = 120;
            handleStart(60);
        } else {
            sec = 60;
        }
    }

    private void handleStart(final int delay) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (ms.getEvents().startEvent(null, MuxButton.this) == false) {
                    handleStart(120);
                }
            }
        }.runTaskLater(ms, delay);
    }

    @Override
    public void stop() {
        final int coinswon = jackpot;
        if (sec > 0) {
            ms.getHashYAML().set("eventbutton", jackpot);
            return;
        } else {
            ms.getHashYAML().set("eventbutton", null);
            jackpot = 0;
            sec = 60;
        }
        if (winner == null) return;
        else {
            ms.broadcastMessage("§d§lMuxEvent>§7 Der Spieler §d" + winner.getName() + " §7hat bei Button §agewonnen§7!", null, MuxSystem.Priority.HIGH);
            winner.playSound(winner.getLocation(), Sound.ENDERDRAGON_DEATH, 1F, 0.1F);
            final MuxUser u = ms.getMuxUser(winner.getName());
            u.addCoins(coinswon);
            ms.getHistory().addHistory(winner.getUniqueId(), null, "COINS", String.valueOf(coinswon), "Event");
            ms.saveMuxUser(u);
            ms.sendScoreboard(winner);
            winner.sendMessage("§d§lMuxEvent>§7 Dir wurden §e" + ms.getNumberFormat(coinswon) + " MuxCoins §7gutgeschrieben.");
        }
        winner = null;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Sei der Letzte, der den Knopf drückt,",
                "§7bevor der Timer abläuft. Man kann ihn",
                "§7nur ein einziges Mal drücken.",
                "",
                "§7Timer: §d" + ms.timeToString(sec * 1000L, false, true),
                "§7Letzter Käufer: §d" + (winner == null ? "Keiner" : winner.getName()),
                "",
                "§7Knopfpreis: §e50 MuxCoins",
                "§7Jackpot: §e" + ms.getNumberFormat(jackpot) + " MuxCoins"
        };
    }

    @Override
    public String getClickToJoin() {
        return "§dKlicke§7, um den Knopfdruck zu kaufen.";
    }

    @Override
    public long getUpdateTime() {
        return 20L;
    }

    @Override
    public void update() {
        sec--;
        updateItem();
        if (sec == 0) {
            stopEvent(true);
        }
    }

    @Override
    public void start() {
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das Button Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        spawnEventNPC("§d§lButton Event");
    }

    @Override
    public boolean join(final Player p) {
        if (clicked.contains(p.getUniqueId()) && players.contains(p) == false) players.add(p);
        final MuxInventory.InvType it = ms.getActiveInv(p.getName());
        if (it != MuxInventory.InvType.EVENT) {
            p.performCommand("event");
            return false;
        } else if (clicked.contains(p.getUniqueId())) {
            ms.showItemBar(p, "§cDu hast den Knopf bereits gedrückt.");
            return false;
        } else if (ms.hasVoted(p) == false) {
            ms.sendNoVoteMessage(p);
            return false;
        }
        final MuxUser u = ms.getMuxUser(p.getName());
        if (u.getCoins() < 50) {
            ms.showItemBar(p, "§cDu hast nicht genügend MuxCoins, um den Knopf zu drücken.");
            return false;
        }
        p.playSound(p.getLocation(), Sound.ORB_PICKUP, 1F, 1F);
        ms.showItemBar(p, "§fDu hast den §dKnopf §fgedrückt.");
        u.addCoins(-50);
        clicked.add(p.getUniqueId());
        jackpot += 50;
        sec = 60;
        winner = p;
        updateItem();
        ms.sendScoreboard(p);
        return false;
    }

    private void updateItem() {
        final ItemStack evitem = ms.addLore(ms.renameItemStack(getItem().clone(), "§d§l" + getName(), getDescription()), "", getClickToJoin());
        e.pinv.setItem(13, evitem);
        e.opinv.setItem(4, evitem);
    }
}