package me.muxteam.basic;

import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.IScoreboardCriteria;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardDisplayObjective;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardObjective;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardScore;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardTeam;
import net.minecraft.server.v1_8_R3.PlayerConnection;
import net.minecraft.server.v1_8_R3.ScoreboardScore;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.scoreboard.CraftScoreboard;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@SuppressWarnings("ALL")
public class MuxScoreboard {

    private final String[] nextEntries = {"a", "b", "c", "d", "e", "f"};
    private final int MAX_PREFIX_SUFFIX_LENGTH = 16;

    private final Field scoreboardObjectiveNameField = NMSReflection.getField(PacketPlayOutScoreboardObjective.class, "a");
    private final Field scoreboardObjectiveDisplayNameField = NMSReflection.getField(PacketPlayOutScoreboardObjective.class, "b");
    private final Field scoreboardObjectiveHealthDisplayField = NMSReflection.getField(PacketPlayOutScoreboardObjective.class, "c");
    private final Field scoreboardObjectiveActionField = NMSReflection.getField(PacketPlayOutScoreboardObjective.class, "d");
    private final Field scoreboardDisplayObjectiveNameField = NMSReflection.getField(PacketPlayOutScoreboardDisplayObjective.class, "b");
    private final Field scoreboardDisplayObjectiveSlotField = NMSReflection.getField(PacketPlayOutScoreboardDisplayObjective.class, "a");
    private final Field scoreboardTeamNameField = NMSReflection.getField(PacketPlayOutScoreboardTeam.class, "a");
    private final Field scoreboardTeamPrefixField = NMSReflection.getField(PacketPlayOutScoreboardTeam.class, "c");
    private final Field scoreboardTeamSuffixField = NMSReflection.getField(PacketPlayOutScoreboardTeam.class, "d");
    private final Field scoreboardTeamNameSetField = NMSReflection.getField(PacketPlayOutScoreboardTeam.class, "g");
    private final Field scoreboardTeamChatFormatField = NMSReflection.getField(PacketPlayOutScoreboardTeam.class, "f");
    private final Field scoreboardTeamOptionDataField = NMSReflection.getField(PacketPlayOutScoreboardTeam.class, "i");
    private final Field scoreboardTeamActionField = NMSReflection.getField(PacketPlayOutScoreboardTeam.class, "h");
    private final Field scoreboardScoreEntryField = NMSReflection.getField(PacketPlayOutScoreboardScore.class, "a");
    private final Field scoreboardScoreObjectiveField = NMSReflection.getField(PacketPlayOutScoreboardScore.class, "b");
    private final Field scoreboardScoreScoreField = NMSReflection.getField(PacketPlayOutScoreboardScore.class, "c");
    private final Field scoreboardScoreActionField = NMSReflection.getField(PacketPlayOutScoreboardScore.class, "d");

    private int defaultTeamOptionData = 0;

    private final MuxSystem ms;
    private final Map<Player, PlayerBoard> playerBoards = new HashMap<>();
    public org.bukkit.scoreboard.Scoreboard hsb;
    private final LinkedHashSet<Scoreboard> belowname = new LinkedHashSet<>();
    private final Iterator<Scoreboard> nextsb;
    private ScoreboardType standardsb;
    private final Map<Player, ScoreboardType> currentTypes = new ConcurrentHashMap<>();
    private final Map<Player, Integer> currentAnimations = new ConcurrentHashMap<>();

    public MuxScoreboard(MuxSystem ms) {
        this.ms = ms;
        for (int i = 0; i < 5; i++) {
            belowname.add(ms.getServer().getScoreboardManager().getNewScoreboard());
        }
        this.nextsb = belowname.iterator();
        hsb = getNewScoreboard();
        final org.bukkit.scoreboard.Objective obj = hsb.registerNewObjective(ms.getLang("hearts"), "health");
        obj.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.BELOW_NAME);
        obj.setDisplayName("§a" + ms.getLang("hearts"));

        defaultTeamOptionData |= 1;
        defaultTeamOptionData |= 2;
        for (Field field : MuxScoreboard.class.getFields()) {
            if (field.getType() == Field.class) {
                try {
                    Field f = (Field) field.get(MuxScoreboard.class);
                    f.setAccessible(true);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public Set<Scoreboard> getScoreboards() {
        return belowname;
    }

    public ScoreboardType getStandardsb() {
        return standardsb;
    }

    public org.bukkit.scoreboard.Scoreboard getNewScoreboard() {
        return nextsb.next();
    }

    public Map<Player, PlayerBoard> getPlayerBoards() {
        return playerBoards;
    }

    public void setStandardsb(ScoreboardType standardsb) {
        this.standardsb = standardsb;
    }

    public void handleJoin(final Player p) {
        setHealth(p);
        sendScoreboard(p, standardsb);
    }

    public ScoreboardType getCurrent(final Player p) {
        return this.currentTypes.get(p);
    }

    public void sendScoreboard(final Player p, ScoreboardType type) {
        if (type == null) {
            sendScoreboard(p, standardsb);
            return;
        }
        final ScoreboardType current = currentTypes.get(p);
        if (current != type)
            currentTypes.put(p, type);
        type.send(p, current);
    }

    public void sendScoreboard(final Player p) {
        ScoreboardType type = this.currentTypes.get(p);
        if (type != null) {
            type.send(p, type);
            return;
        }
        standardsb.send(p, null);
    }

    public void resetScoreboard(final Player p) {
        sendScoreboard(p, standardsb);
    }

    public void handleQuit(final Player p) {
        this.currentTypes.remove(p);
        final PlayerBoard board = this.playerBoards.remove(p);
        if (board == null)
            return;
        try {
            board.remove();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        this.currentAnimations.remove(p);
    }

    public ScoreboardType createScoreboard(final String name) {
        return new ScoreboardType(name);
    }

    public void setHealth(final Player p) {
        final Objective obj = hsb.getObjective(DisplaySlot.BELOW_NAME);
        p.setScoreboard(hsb);
        obj.getScore(p.getName()).setScore((int) p.getHealth());
    }

    public void updateHealthOf(final Player p, final String pname, final double health) {
        final EntityPlayer ep = ((CraftPlayer) p).getHandle();
        final net.minecraft.server.v1_8_R3.Scoreboard scoreboard = ((CraftScoreboard) hsb).getHandle();
        final ScoreboardScore scoreboardScore = new ScoreboardScore(scoreboard, scoreboard.getObjectives().iterator().next(), pname);
        scoreboardScore.setScore((int) health);
        final PacketPlayOutScoreboardScore packet = new PacketPlayOutScoreboardScore(scoreboardScore);
        ep.playerConnection.sendPacket(packet);
    }

    public class PlayerBoard {
        private final Player player;
        private String title = "MuxCraft";
        private final String[] lines = new String[16];
        private boolean created = false;

        public PlayerBoard(Player player, final String title) {
            this.player = player;
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public String getTextAt(int index) {
            return lines[index];
        }

        public int addLine(String text) throws IllegalAccessException {
            int index = 0;
            for (int i = 0; i < lines.length; i++) {
                if (lines[i] == null) {
                    index = i;
                    break;
                }
            }
            setLine(index, text);
            return index;
        }

        public void setLine(int line, String text) throws IllegalAccessException {
            if (line < 0 || line >= 16)
                return;
            if (lines[line] != null && lines[line].equals(text))
                return;

            String prefix, suffix;
            if (text.length() > MAX_PREFIX_SUFFIX_LENGTH) {
                String first = text.substring(0, MAX_PREFIX_SUFFIX_LENGTH);
                String second = text.substring(MAX_PREFIX_SUFFIX_LENGTH);
                if (first.endsWith(String.valueOf(ChatColor.COLOR_CHAR))) {
                    first = first.substring(0, first.length() - 1);
                    second = ChatColor.COLOR_CHAR + second;
                }
                String lastColors = ChatColor.getLastColors(first);
                second = lastColors + second;
                prefix = first;
                suffix = StringUtils.left(second, MAX_PREFIX_SUFFIX_LENGTH);
            } else {
                suffix = "";
                prefix = text;
            }
            sendLine(line, prefix, suffix);
            this.lines[line] = text;
        }

        public void clear(int line) throws IllegalAccessException {
            if (line >= 0 && line <= 16 && lines[line] != null) {
                sendClear(line);
                lines[line] = null;
            }
            if (Arrays.stream(lines).allMatch(Objects::isNull)) {
                sendDestroy();
            }
        }

        public void remove() throws IllegalAccessException {
            for (int i = 1; i < 15; i++) {
                clear(i);
            }
            sendDestroy();
        }

        public void setLines(List<String> lines) throws IllegalAccessException {
            int currentLine = 1;
            for (int i = lines.size() - 1; i >= 0; --i) {
                setLine(currentLine, lines.get(i));
                currentLine++;
            }
            for (int i = lines.size(); i < 15; i++) {
                if (this.lines[currentLine] != null)
                    this.clear(currentLine);
                currentLine++;
            }
        }

        public void createBoard() throws IllegalAccessException {
            PacketPlayOutScoreboardObjective packetA = new PacketPlayOutScoreboardObjective();
            scoreboardObjectiveNameField.set(packetA, player.getName());
            scoreboardObjectiveDisplayNameField.set(packetA, this.title);
            scoreboardObjectiveHealthDisplayField.set(packetA, IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER);
            scoreboardObjectiveActionField.set(packetA, 0);

            PacketPlayOutScoreboardDisplayObjective packetB = new PacketPlayOutScoreboardDisplayObjective();
            scoreboardDisplayObjectiveSlotField.set(packetB, 1);
            scoreboardDisplayObjectiveNameField.set(packetB, player.getName());
            sendPackets(packetA, packetB);
            this.created = true;
        }


        public void sendLine(int line, String prefix, String suffix) throws IllegalAccessException {
            if (created == false) {
                createBoard();
            }
            PacketPlayOutScoreboardTeam packet = getOrRegisterTeam(line);

            scoreboardTeamPrefixField.set(packet, prefix);
            scoreboardTeamSuffixField.set(packet, suffix);
            sendPacket(packet);

        }

        public void sendClear(int line) throws IllegalAccessException {
            PacketPlayOutScoreboardScore packetA = new PacketPlayOutScoreboardScore();
            scoreboardScoreEntryField.set(packetA, getEntry(line));
            scoreboardScoreObjectiveField.set(packetA, player.getName());
            scoreboardScoreScoreField.set(packetA, line);
            scoreboardScoreActionField.set(packetA, PacketPlayOutScoreboardScore.EnumScoreboardAction.REMOVE);

            PacketPlayOutScoreboardTeam packetB = this.getOrRegisterTeam(line);
            scoreboardTeamActionField.set(packetB, 1);
            sendPackets(packetA, packetB);
        }

        public void setTitle(String title) throws IllegalAccessException {
            if (title == null)
                title = "";
            if (this.title.equals(title))
                return;
            this.title = title;
            PacketPlayOutScoreboardObjective packetA = new PacketPlayOutScoreboardObjective();
            scoreboardObjectiveNameField.set(packetA, player.getName());
            scoreboardObjectiveDisplayNameField.set(packetA, title);
            scoreboardObjectiveActionField.set(packetA, 2);
            scoreboardObjectiveHealthDisplayField.set(packetA, IScoreboardCriteria.EnumScoreboardHealthDisplay.INTEGER);
            sendPacket(packetA);
        }

        public void sendDestroy() throws IllegalAccessException {
            PacketPlayOutScoreboardObjective packetA = new PacketPlayOutScoreboardObjective();
            scoreboardObjectiveNameField.set(packetA, player.getName());
            scoreboardObjectiveActionField.set(packetA, 1);

            PacketPlayOutScoreboardDisplayObjective packetB = new PacketPlayOutScoreboardDisplayObjective();
            scoreboardDisplayObjectiveNameField.set(packetB, player.getName());
            scoreboardDisplayObjectiveSlotField.set(packetB, 1);
            sendPackets(packetA, packetB);
            created = false;
        }

        private PacketPlayOutScoreboardTeam getOrRegisterTeam(int line) throws IllegalAccessException {
            PacketPlayOutScoreboardTeam packetB = new PacketPlayOutScoreboardTeam();
            scoreboardTeamNameField.set(packetB, "sb-" + line);
            scoreboardTeamChatFormatField.set(packetB, 0);
            scoreboardTeamOptionDataField.set(packetB, defaultTeamOptionData);
            if (this.lines[line] != null) {
                scoreboardTeamActionField.set(packetB, 2);
            } else {
                this.lines[line] = "";
                scoreboardTeamActionField.set(packetB, 0);
                scoreboardTeamNameSetField.set(packetB, Collections.singletonList(getEntry(line)));
                PacketPlayOutScoreboardScore packetA = new PacketPlayOutScoreboardScore();
                scoreboardScoreEntryField.set(packetA, getEntry(line));
                scoreboardScoreObjectiveField.set(packetA, player.getName());
                scoreboardScoreScoreField.set(packetA, line);
                scoreboardScoreActionField.set(packetA, PacketPlayOutScoreboardScore.EnumScoreboardAction.CHANGE);
                sendPacket(packetA);
            }
            return packetB;
        }

        private void sendPackets(final Packet<?>... packets) {
            final PlayerConnection playerConnection = ((CraftPlayer) player).getHandle().playerConnection;
            for (Packet<?> packet : packets) {
                playerConnection.sendPacket(packet);
            }
        }

        private void sendPacket(final Packet<?> packet) {
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
        }
    }

    public class ScoreboardType {
        private final String displayname, color;
        private int pos = 15;
        private BiConsumer<Player, PlayerBoard> update;
        private List<String> currentLines;

        public ScoreboardType(final String name) {
            this.displayname = name;
            this.color = "§" + name.charAt(name.lastIndexOf('§') - 1);
        }


        public void send(final Player p, final ScoreboardType last) {
            this.currentLines = new ArrayList<>();
            PlayerBoard board = playerBoards.get(p);
            if (last == null) {
                playerBoards.put(p, board = new PlayerBoard(p, this.displayname));
            } else if (last != this) {
                try {
                    board.setTitle(this.displayname);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                try {
                    for (int i = 0; i < board.lines.length; i++) {
                        board.clear(i);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            this.pos = 15;
            if (update != null) update.accept(p, board);
            currentLines.add("");
            currentLines.add(color + "muxcraft.eu");
            try {
                board.setLines(currentLines);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        public void setLook(final BiConsumer<Player, PlayerBoard> look) {
            this.update = look;
        }


        public void setSection(final PlayerBoard board, final String section, final String value, final boolean animations) {
            if (section != null && section.isEmpty() == false) {
                setLine(board, section);
            }
            if (updateAndRemoveScore(board, value, pos, animations)) {
                return;
            }
            setLine(board, value);
        }

        public void setLine(final PlayerBoard board, final String text) {
            currentLines.add(text);
            pos--;
        }


        private boolean updateAndRemoveScore(final PlayerBoard board, final String value, int score, final boolean animations) {
            // score--;
            final int finalScore = score - 1;
            String oldvalue = board.getTextAt(finalScore);
            if (animations == false || oldvalue == null || (oldvalue != null && oldvalue.equals(value))) {
                return false; // line wird normal gesetzt
            }
            //     final int finalScore = score--; // not sure if we need this
            String finalOldvalue = oldvalue;
            if (oldvalue.matches(".*\\d.*") && value.matches(".*\\d.*")) {
                final long oldnumber = Long.parseLong(ms.removeNonDigits(oldvalue)), newnumber = Long.parseLong(ms.removeNonDigits(value));
                if (newnumber > oldnumber) {
                    int taskId = new BukkitRunnable() {
                        long ocoins = oldnumber;
                        final long interval = Math.max(1, (newnumber - oldnumber) / 40);

                        @Override
                        public void run() {
                            if (board == null || board.player.isOnline() == false || currentTypes.get(board.player) != ScoreboardType.this) {
                                cancel();
                                return;
                            }
                            final Integer currentTask = currentAnimations.get(board.player);
                            if (currentTask == null || currentTask != this.getTaskId()) {
                                cancel();
                                return;
                            }
                            try {
                                board.setLine(finalScore, "§a" + ms.getNumberFormat(ocoins));
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }

                            if (ocoins == oldnumber) {
                                try {
                                    board.setLine(finalScore, finalOldvalue);
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                            }
                            ocoins += interval;
                            if (ocoins >= newnumber) {
                                try {
                                    board.setLine(finalScore, value);
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                                currentAnimations.remove(board.player);
                                cancel();
                            } else {
                                try {
                                    board.setLine(finalScore, "§a" + ms.getNumberFormat(ocoins));
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                }
                                board.player.playSound(board.player.getLocation(), Sound.CLICK, 0.4F, 2F);
                            }
                        }
                    }.runTaskTimer(ms, 1L, 1L).getTaskId();
                    currentAnimations.put(board.player, taskId);
                    setLine(board, oldvalue);
                    return true;
                }
                return false;
            } else if (value.contains("Kein Clan") == false && value.contains("Default") == false) {
                int taskId = new BukkitRunnable() {
                    String text = "";
                    double length;
                    final double interval = Math.max(0.1, (value.length()) / 40D);

                    @Override
                    public void run() {
                        if (board == null || board.player.isOnline() == false || currentTypes.get(board.player) != ScoreboardType.this) {
                            cancel();
                            return;
                        }
                        final Integer currentTask = currentAnimations.get(board.player);

                        if (currentTask == null || currentTask != this.getTaskId()) {
                            cancel();
                            return;
                        }
                        try {
                            board.setLine(finalScore, "§a" + text);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        if (length == 0) {
                            try {
                                board.setLine(finalScore, finalOldvalue);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                        length += interval;
                        if ((int) length > value.length()) {
                            try {
                                board.setLine(finalScore, value);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                            currentAnimations.remove(board.player);
                            cancel();
                            return;
                        }
                        text = value.substring(0, (int) length);
                        try {
                            board.setLine(finalScore, "§a" + text);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        if ((int) length > (int) (length - interval))
                            board.player.playSound(board.player.getLocation(), Sound.CLICK, 0.4F, 2F);
                    }
                }.runTaskTimer(ms, 1L, 1L).getTaskId();
                currentAnimations.put(board.player, taskId);
                setLine(board, "");
                return true;
            }
            return false;
        }
    }

    public String getEntry(Integer line) {
        if (line > 0 && line <= 16)
            if (line <= 10)
                return ChatColor.COLOR_CHAR + "" + (line - 1) + ChatColor.WHITE;
            else {
                return ChatColor.COLOR_CHAR + nextEntries[line - 11] + ChatColor.WHITE;
            }
        return "";
    }
}