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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@NonJoinableEvent
public class MuxFastChat extends Event implements Listener {
    private String word;
    private long start;
    private int num = 0;

    public MuxFastChat(final MuxEvents e) {
        super(e);
        name = "MuxSchnelltipper";
        item = new ItemStack(Material.PAPER);
        chatOnly = true;
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Halte auf die Nachricht, die im Chat",
                "§7angezeigt wird. Gib dann das Wort",
                "§7so schnell wie möglich ein.",
        };
    }

    @Override
    public void start() {
        final List<String> words = Arrays.asList("Vogel", "Vulkan", "Liebe", "Tanzen", "Haare", "Glas", "Domino", "Würfel", "Computer", "Bart", "Wind", "Regen", "Minecraft", "MuxCraft", "Kämpfen", "Clown", "Miner", "Creeper", "Ghast", "Spinne", "Roller", "Strand", "Kalt", "Schwanger", "Foto", "Schnell", "Mario", "Luigi", "Schildkröte", "Türknopf",
                "Teleskop", "Planet", "Mountain Bike", "Mond", "Komet", "Blume", "Chef", "Elefant", "Shotgun", "Pistole", "James Bond", "Geld", "Salz und Pfeffer", "Hubschrauber", "Luftballon", "Schreien", "Muskel", "Mager", "Zombie", "Lava", "Schlange", "Boot", "Fenster", "Lollipop", "Handschellen", "Polizei", "Uppercut");
        word = words.get(r.nextInt(words.size()));
        num = 0;
        final String[] chars = {"R", "G", "V", "B", "Z", "A"};
        final String rndchar = chars[r.nextInt(chars.length)];
        final String noise = String.join("", Collections.nCopies(r.nextInt(3) + 1, rndchar)),
                noise2 = IntStream.range(0, noise.length()).mapToObj(i -> i == noise.length() - 1 ? noise.substring(i) : noise.charAt(i) + (r.nextInt(10) < 2 ? "§d§0" : "")).collect(Collectors.joining());
        final String secret = word.chars().map(c -> (c >= 0x21 && c <= 0x7E) ? c + 0xFEE0 : c).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverShow(pl, "§d§lMuxEvent>§7 Schnelltipper hat jetzt begonnen.", "§d§0" + noise2 + noise2 + "§d" + secret +  "§0" + noise2 + "§d", "");
            ms.chatClickHoverShow(pl, "§d§lMuxEvent>§7 Halte §dhier §7und schreibe §ddas Wort §7im Chat.", "§d§0" + noise2 + noise2 + "§d" + secret + "§0" + noise2 + "§d", "");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        final boolean mute = ms.getChat().isMute1();
        ms.getChat().unmuteEverything();
        start = System.currentTimeMillis();
        new BukkitRunnable() {
            @Override
            public void run() {
                stopEvent(true);
                if (mute) ms.getChat().muteEverything();
            }
        }.runTaskLater(ms, 200L);
    }

    @Override
    public boolean join(final Player p) {
        p.sendMessage("§d§lMuxEvent>§7 Derzeit läuft das §d" + getName() + "§7 Event.");
        p.sendMessage("§d§lMuxEvent>§7 Tippe §ddas Wort §7so schnell wie möglich im Chat.");
        return false;
    }

    @EventHandler
    public void onChat(final AsyncPlayerChatEvent e) {
        e.setCancelled(true);
        final Player p = e.getPlayer();
        if (e.getMessage().contains(word)) {
            if (players.contains(p)) {
                return;
            }
            players.add(p);
            num++;
            if (startedByPlayer == false) {
                final int reward = num == 1 ? 80 : num == 2 ? 40 : num == 3 ? 20 : 5;
                p.sendMessage("§d§lMuxEvent>§7 Dir wurden §e" + ms.getNumberFormat(giveReward(p, reward)) + " MuxCoins §7gutgeschrieben.");
                new BukkitRunnable() {
                    @Override public void run() { // To avoid async
                        ms.sendScoreboard(p);
                    }
                }.runTask(ms);
            }
            ms.showItemBar(p, "§fDu hast §ddas Wort §frichtig geschrieben. (§d#" + num + "§f)");
            final long time = System.currentTimeMillis() - start;
            if (num <= 3) {
                final int n = num;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ms.broadcastMessage("§d§lMuxEvent>§7 Spieler §a#" + n + "§7: §d" + p.getName() + " §7(" + String.valueOf(time / 1000D).replace(".", ",") + " Sekunden)", null, MuxSystem.Priority.HIGH);
                    }
                }.runTaskLater(ms, 200L);
            }
        } else {
            ms.showItemBar(p, "§cFalsche Antwort! Versuche es erneut.");
            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
        }
    }
}