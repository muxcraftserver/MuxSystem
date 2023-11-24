package me.muxteam.events;

import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

@NonJoinableEvent
public class MuxLastChat extends Event implements Listener {
    private Player last;

    public MuxLastChat(final MuxEvents e) {
        super(e);
        name = "MuxChatChaos";
        item = new ItemStack(Material.EMPTY_MAP);
    }

    @Override
    public void stop() {
        if (last != null) {
            if (startedByPlayer == false) {
                last.sendMessage("§d§lMuxEvent>§7 Dir wurden §e" + ms.getNumberFormat(giveReward(last, 100)) + " MuxCoins §7gutgeschrieben.");
            }
            ms.broadcastMessage("§d§lMuxEvent>§7 Der Spieler §d" + last.getName() + " §7hat §agewonnen§7!", null, MuxSystem.Priority.HIGH);
        }
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Der allerletzte Spieler, der im Chat",
                "§7etwas schreibt, gewinnt das Event.",
                "§7Spammen erhöht deine Chancen.",
        };
    }

    @Override
    public void start() {
        last = null;
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            pl.sendMessage("§d§lMuxEvent>§7 Der globale Chat wird gleich aktiviert.");
            pl.sendMessage("§d§lMuxEvent>§7 Sei der §oletzte§7, der §dim Chat schreibt§7.");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        final boolean unmute = ms.getChat().isMute1() == false;
        new BukkitRunnable() {
            @Override
            public void run() {
                ms.getChat().unmuteEverything();
                ms.runLater(new BukkitRunnable() {
                    @Override
                    public void run() {
                        ms.getChat().muteEverything();
                        stopEvent(true);
                        if (unmute) {
                            ms.runLater(new BukkitRunnable() {
                                @Override
                                public void run() {
                                    ms.getChat().unmuteEverything();
                                }
                            }, 400L);
                        }
                    }
                }, 20L * (r.nextInt(13) + 3));
            }
        }.runTaskLater(ms, 80L);
    }

    @Override
    public boolean join(final Player p) {
        p.sendMessage("§d§lMuxEvent>§7 Derzeit läuft das §d" + getName() + "§7 Event.");
        p.sendMessage("§d§lMuxEvent>§7 Sei der §dletzte §7der im Chat schreibt.");
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onChat(final AsyncPlayerChatEvent e) {
        if (e.getPlayer().hasPermission("muxsystem.unmutable")) return;
        last = e.getPlayer();
    }
}