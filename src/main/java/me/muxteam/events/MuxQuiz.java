package me.muxteam.events;

import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NonJoinableEvent
public class MuxQuiz extends Event implements Listener {
    private boolean guessed = false;
    public boolean running = false;
    private boolean shopQuestion;
    private String question = null, answer = null;
    private BukkitTask task;
    private final Map<Player, Long> cooldown = new HashMap<>();

    public MuxQuiz(final MuxEvents e) {
        super(e);
        name = "MuxQuiz";
        item = new ItemStack(Material.BOOK_AND_QUILL);
        chatOnly = true;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(final String answer) {
        this.answer = answer;
    }

    public void setQuestion(final String question) {
        this.question = question;
    }

    @Override
    public String[] getDescription() {
        if (running) {
            return new String[]{
                    "§7Im Chat erscheint eine Frage! Sei der",
                    "§7Erste, der sie so schnell wie möglich",
                    "§7korrekt beantwortet.",
                    "",
                    "§7Frage: §d" + question
            };
        }
        return new String[]{
                "§7Im Chat erscheint eine Frage! Sei der",
                "§7Erste, der sie so schnell wie möglich",
                "§7korrekt beantwortet.",
        };
    }

    @Override
    public String[] getAdminInformation() {
        return new String[]{
                "§7Antwort: §d" + (answer == null ? "Keine" : shopQuestion == false ? answer : ms.getShop().getCheapestPrice(answer))
        };
    }

    @Override
    public boolean join(final Player p) {
        p.sendMessage("§d§lMuxEvent>§7 Derzeit läuft das §d" + getName() + "§7 Event.");
        p.sendMessage("§d§lMuxEvent>§7 Tippe die Antwort zu der Frage §d'" + question + "'§7.");
        return false;
    }

    @Override
    public void start() {
        guessed = false;
        running = true;
        if (question == null || answer == null) {
            if (r.nextInt(100) + 1 < 50) {
                question = "Welche Zahl zwischen 1-100 suchen wir?";
                answer = String.valueOf(r.nextInt(100) + 1);
            } else {
                shopQuestion = true;
                String itemId;
                int tries = 0;
                do {
                    itemId = ms.getShop().getItemIDs().get(r.nextInt(ms.getShop().getItemIDs().size()));
                    if (tries > 30) break;
                    tries++;
                } while (ms.getShop().getCheapestPrice(itemId) <= 0);
                if (ms.getShop().getCheapestPrice(itemId) <= 0) {
                    shopQuestion = false;
                    question = "Welche Zahl zwischen 1-100 suchen wir?";
                    answer = String.valueOf(r.nextInt(100) + 1);
                } else {
                    question = "Wie viel MuxCoins kostet " + ms.getShop().getItemName(itemId) + " im Shop?";
                    answer = itemId;
                }
            }
        }
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            pl.sendMessage("§d§lMuxEvent>§e " + question);
            pl.sendMessage("§d§lMuxEvent>§7 Beantworte diese Frage im Chat.");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (guessed == false) {
                    if (shopQuestion) {
                        double cheapest = ms.getShop().getCheapestPrice(answer);
                        answer = (cheapest < 10000 ? ms.getNumberFormat(cheapest) : ms.getSFormat(cheapest));
                    }
                    ms.broadcastMessage("§d§lMuxEvent>§7 Kein Spieler hat die richtige Antwort erraten.", null, MuxSystem.Priority.LOW);
                    ms.broadcastMessage("§d§lMuxEvent>§7 Die Antwort auf die Frage §d'" + question + "'§7 war §d'" + answer + "'§7.", null, MuxSystem.Priority.LOW);
                }
                stopEvent(true);
            }
        }.runTaskLater(ms, 600L);
    }

    @Override
    public void stop() {
        question = null;
        answer = null;
        if (task != null) task.cancel();
        task = null;
        running = false;
        cooldown.clear();
        shopQuestion = false;
    }

    @EventHandler
    public void onChat(final AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();
        if (e.getMessage().startsWith("#") && ms.getDB().getClanFromPlayer(p.getUniqueId()) != null) {
            return;
        }
        if (cooldown.getOrDefault(p, 0L) > System.currentTimeMillis()) {
            e.setCancelled(true);
            ms.showItemBar(p, "§cWarte kurz und versuche es gleich erneut.");
        } else if (guessed == false && isCorrectAnswer(e.getMessage())) {
            e.setCancelled(true);
            guessed = true;
            ms.showItemBar(p, "§aDu hast die richtige Antwort erraten.");
            p.playSound(p.getLocation(), Sound.LEVEL_UP, 1, 1);
            if (startedByPlayer == false) {
                p.sendMessage("§d§lMuxEvent>§7 Dir wurden §e" + ms.getNumberFormat(giveReward(p, 100)) + " MuxCoins §7gutgeschrieben.");
            }
            if (shopQuestion) {
                double cheapest = ms.getShop().getCheapestPrice(answer);
                answer = (cheapest < 10000 ? ms.getNumberFormat(cheapest) : ms.getSFormat(cheapest));
            }
            ms.broadcastMessage("§d§lMuxEvent>§7 Der Spieler §d" + p.getName() + " §7hat §agewonnen§7!", null, MuxSystem.Priority.LOW);
            ms.broadcastMessage("§d§lMuxEvent>§7 Die Antwort auf die Frage §d'" + question + "'§7 war §d'" + answer + "'§7.", null, MuxSystem.Priority.LOW);
            new BukkitRunnable() {
                @Override
                public void run() {
                    task.cancel();
                    stopEvent(true);
                }
            }.runTask(ms);
        } else {
            cooldown.put(p, System.currentTimeMillis() + 1000L);
            e.setCancelled(true);
            ms.showItemBar(p, "§cFalsche Antwort! Versuche es erneut.");
            p.playSound(p.getLocation(), Sound.NOTE_BASS, 1F, 1F);
        }
    }

    private boolean isCorrectAnswer(String answer) {
        if (shopQuestion == false)
            return answer.equalsIgnoreCase(ChatColor.stripColor(this.answer));
        else {
            double cheapest = ms.getShop().getCheapestPrice(this.answer);
            final String priceStr = (cheapest < 10000 ? ms.getNumberFormat(cheapest) : ms.getSFormat(cheapest));
            answer = answer.replace(",", ".").replace(" ", "");
            answer = Pattern.compile("muxcoins", Pattern.LITERAL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(answer)
                    .replaceAll(Matcher.quoteReplacement(""));
            answer = Pattern.compile("muxcoin", Pattern.LITERAL | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(answer)
                    .replaceAll(Matcher.quoteReplacement(""));
            if (answer.matches("\\d*")) {
                final double d = Double.parseDouble(answer);
                answer = (d < 10000 ? ms.getNumberFormat(d) : ms.getSFormat(d));
            }
            return answer.equalsIgnoreCase(priceStr);
        }
    }
}