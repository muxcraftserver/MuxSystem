package me.muxteam.events;

import me.muxteam.muxsystem.MuxSystem;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NonJoinableEvent
public class MuxGuess extends Event {
    private String chosen;
    private final List<Player> guessed = new ArrayList<>();
    private boolean stopped = false;

    public MuxGuess(final MuxEvents e) {
        super(e);
        name = "MuxRaten";
        item = new ItemStack(Material.NAME_TAG);
        chatOnly = true;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Errate den richtigen Block zwischen",
                "§7den drei Optionen im Chat. Er wurde",
                "§7vorher zufällig ausgewählt."
        };
    }

    @Override
    public String[] getAdminInformation() {
        return new String[]{
                "§7Teilnehmer: §d" + guessed.size()
        };
    }

    @Override
    public void stop() {
        stopped = true;
    }

    @Override
    public void start() {
        stopped = false;
        guessed.clear();
        final List<String> blocks = new ArrayList<>(Arrays.asList("Stein", "Gras", "Holz", "Kaktus", "Kuchen", "Bett", "Bedrock", "Sand", "Sandstein",
                "Glas", "Werkbank", "Ofen", "Diamantblock", "Goldblock", "Eisenblock", "Kohle", "Braustand", "Kies", "Werfer", "Schild", "Spender", "Kiste",
                "Wasser", "Lava", "Leiter", "Kessel", "Ton", "Emeraldblock")), chosenblocks = new ArrayList<>();
        for (byte i = 0; i < 3; i++) {
            String block = blocks.get(r.nextInt(blocks.size() - 1));
            while (chosenblocks.contains(block)) {
                block = blocks.get(r.nextInt(blocks.size() - 1));
            }
            chosenblocks.add(block);
        }
        chosen = chosenblocks.get(r.nextInt(3));
        final TextComponent textComponent = new TextComponent(TextComponent.fromLegacyText("§d§lMuxEvent>§r §7Klicke:   "));
        for (final String s : chosenblocks) {
            final TextComponent blockComponent = new TextComponent(TextComponent.fromLegacyText("§d" + s + "   "));
            blockComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/event action " + s));
            blockComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§d" + s).create()));
            textComponent.addExtra(blockComponent);
        }
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            pl.sendMessage("§d§lMuxEvent>§7 Welcher zufällige Block wurde ausgewählt?");
            pl.spigot().sendMessage(textComponent);
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        final boolean unmute = ms.getChat().isMute1() == false;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (stopped == false) {
                    finish(unmute);
                }
            }
        }.runTaskLater(ms, 320L);
        ms.getChat().muteEverything();
    }

    @Override
    public boolean join(final Player p) {
        p.sendMessage("§d§lMuxEvent>§7 Derzeit läuft das §d" + getName() + "§7 Event.");
        p.sendMessage("§d§lMuxEvent>§7 Errate den §dausgewählten Block§7.");
        return false;
    }

    @Override
    public void action(final Player p, final String[] args) {
        final String guess = args[1];
        if (guessed.contains(p)) return;
        if (guess.equals(chosen)) {
            players.add(p);
        }
        ms.showItemBar(p, "§fDu hast §d" + args[1] + " §fausgewählt.");
        guessed.add(p);
    }

    private void finish(boolean unmute) {
        ms.broadcastMessage("§d§lMuxEvent>§7 Der ausgewählte Block war...", null, MuxSystem.Priority.NORMAL);
        new BukkitRunnable() {
            @Override
            public void run() {
                ms.broadcastMessage("§d§lMuxEvent>§7 " + chosen + "§7! §a" + players.size() + " Spieler lag" + (players.size() == 1 ? "" : "en") + " richtig.", null, MuxSystem.Priority.NORMAL);
                for (final Player pl : players) {
                    if (startedByPlayer == false) {
                        pl.sendMessage("§d§lMuxEvent>§7 Dir wurden §e" + ms.getNumberFormat(giveReward(pl, 5)) + " MuxCoins §7gutgeschrieben.");
                    }
                    ms.sendScoreboard(pl);
                }
                guessed.clear();
                stopEvent(true);
                if (unmute) ms.getChat().unmuteEverything();
            }
        }.runTaskLater(ms, 60L);
    }
}