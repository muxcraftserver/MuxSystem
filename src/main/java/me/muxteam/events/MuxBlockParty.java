package me.muxteam.events;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import me.muxteam.basic.MuxMusic;
import me.muxteam.basic.MuxScoreboard;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class MuxBlockParty extends Event implements Listener {
    private final Location loc = new Location(ms.getWarpsWorld(), -418.5D, 30D, 438.5D);
    private final List<Integer> groundContainsBytes = new ArrayList<>();
    private final DecimalFormat decimalFormat = new DecimalFormat("#0.0");
    private int level, step, status, searchedbyte;
    private final MuxMusic music;
    private long timemillis;
    private final MuxScoreboard.ScoreboardType sb;

    public MuxBlockParty(final MuxEvents e) {
        super(e);
        name = "MuxBlockParty";
        item = new ItemStack(Material.RECORD_6);
        final ItemMeta im = item.getItemMeta();
        im.addItemFlags(ItemFlag.values());
        item.setItemMeta(im);
        canSpectate = true;
        maxPlayers = 150;
        music = new MuxMusic(ms);
        maxDuration = 600000;
        maxSpectatorDistance = 50D;
        sb = ms.getScoreboard().createScoreboard("§7§lMux§d§lBlockParty");
        sb.setLook((p, board) -> {
            sb.setLine(board, "");
            sb.setSection(board, "§c§lVerbleibend", players.size() + " Spieler", false);
            sb.setLine(board, "  ");
            sb.setSection(board, "§3§lSchwierigkeit", "Level " + (level - 3), false);
            sb.setLine(board, "   ");
            sb.setSection(board, "§b§lGeschwindigkeit", decimalFormat.format((9000 - Math.min(level * 400, 7000)) / 1000.0F) + " Sekunden", false);
        });
        spectatorSB = sb;
    }

    private void death(final Player p) {
        final Location l = p.getLocation();
        resetPlayer(p);
        music.remove(p);
        if (canjoin || cancelled) return;
        p.getWorld().strikeLightningEffect(l);
        sendEventMessage("§7Der Spieler §d" + p.getName() + " §7ist §causgeschieden§7.");
        ms.showItemBar(p, "§cDu bist aus dem Event ausgeschieden.");
        ms.sendTitle(p, "§d§lMuxBlockParty", 10, 80, 10);
        ms.sendSubTitle(p, "§fDein Platz: §d#" + (players.size() + 1), 10, 80, 10);
        if (players.size() == 1) {
            final Player winner = players.iterator().next();
            resetPlayer(winner);
            music.remove(winner);
            ms.sendTitle(winner, "§d§lMuxBlockParty", 10, 80, 10);
            ms.sendSubTitle(winner, "§fDu hast das Event §agewonnen§f!", 10, 80, 10);
            if (startedByPlayer == false) {
                ms.runLater(new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (winner.isOnline() == false) return;

                        ms.sendSubTitle(winner.getPlayer(), "§fDeine Belohnung: §e§l" + ms.getNumberFormat(giveReward(winner, 150)) + " MuxCoins", 0, 60, 10);
                    }
                }, 60L);
            }
            ms.broadcastMessage("§d§lMuxBlockParty>§7 Der Spieler §d" + winner.getName() + " §7hat §agewonnen§7!", null, MuxSystem.Priority.LOW);
            stopEvent(true);
        }
    }

    private void generateColors() {
        final EditSession session = ms.getFastBuilder(loc.getWorld().getName());
        groundContainsBytes.clear();
        final List<Integer> possibleBytes = new ArrayList<>();
        int differentBlocks = r.nextInt(7) + 2;
        for (int i = 0; i < differentBlocks; i++) {
            possibleBytes.add(r.nextInt(16));
        }
        for (int x = -20; x < 20; x++) {
            for (int z = -20; z < 20; z++) {
                final Location location = loc.clone().add(x, 0, z);
                // More Random
                int rand = r.nextInt(10), bytes = possibleBytes.get(0);
                if (rand == 0) {
                    int stepbyte = possibleBytes.get(r.nextInt(possibleBytes.size() - 1));
                    searchedbyte = stepbyte;
                    bytes = stepbyte;
                }
                int size = r.nextInt(3);
                if (groundContainsBytes.contains(bytes) == false) groundContainsBytes.add(bytes);
                session.setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), new BaseBlock(BlockID.STAINED_CLAY, bytes));
                for (double ix = -size; ix < size; ix++) {
                    for (double iz = -size; iz < size; iz++) {
                        final Location extraBlock = loc.clone().add(x + ix, 0, z + iz).clone();
                        if (extraBlock.getBlockX() > loc.getBlockX() - 20 && extraBlock.getBlockX() < loc.getBlockX() + 20 && extraBlock.getBlockZ() > loc.getBlockZ() - 20 && extraBlock.getBlockZ() < loc.getBlockZ() + 20) {
                            session.setBlock(extraBlock.getBlockX(), extraBlock.getBlockY(), extraBlock.getBlockZ(), new BaseBlock(BlockID.STAINED_CLAY, bytes));
                        }
                    }
                }
            }
        }
        session.flushQueue();
        if (groundContainsBytes.size() <= 1) {
            status = 0;
        } else {
            status = 2;
            timemillis = System.currentTimeMillis();
            possibleBytes.clear();
        }
    }

    @Override
    public String[] getDescription() {
        return new String[]{
                "§7Es ist Zeit zu tanzen! Sobald die Musik",
                "§7aufhört zu spielen, musst du auf der",
                "§7richtigen Farbe des Blockes stehen.",
                "",
                "§7Teilnehmer: §d" + players.size()
        };
    }

    @Override
    public long getUpdateTime() {
        return 5L;
    }

    @Override
    public Location getSpectateLocation() {
        return loc;
    }

    @Override
    public void updateCountDown(final int sec) {
        if (sec == 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    players.forEach(player -> ms.sendScoreboard(player, sb));
                }
            }.runTaskLater(ms, 5L);
            if (music.isPlaying() == false) {
                music.playSong(music.getRandomSongName(), players, true);
            } else {
                players.forEach(music::add);
            }
        }
    }

    @Override
    public void update() {
        for (final Player pl : players) {
            if (pl.getLocation().getBlockY() < loc.getBlockY() - 3) {
                if (canjoin) ms.forcePlayer(pl, loc.clone().add(0, 4, 0));
                else {
                    if (players.contains(pl) == false) continue;
                    death(pl);
                }
            }
        }
        if (canjoin) return;
        if (music.isPlaying() == false && status != 3) {
            music.playSong(music.getRandomSongName(), players, true);
        }
        if (status == 3) {
            if (System.currentTimeMillis() - timemillis > 3500L) {
                status = 0;
                level++;
                for (final Player pl : players) {
                    pl.getInventory().setItem(4, new ItemStack(Material.AIR));
                    pl.playSound(pl.getLocation(), Sound.LEVEL_UP, 0.1F, 1F);
                }
            }
        } else if (status == 2) {
            long time = (10000 - ((System.currentTimeMillis() + (Math.min(level * 400, 7000))) - timemillis));
            int timesep = (int) (time / 1000);
            if (timesep < step) {
                if (timesep == 0) {
                    sendEventBar("§c✖ §f§lSTOP§f §c✖", Sound.ORB_PICKUP);
                    players.forEach(music::remove);
                } else
                    sendEventBar("§fStelle dich auf den passenden Block §8§l|§f §d" + timesep + " SEKUNDE" + (timesep == 1 ? "" : "N"), Sound.ORB_PICKUP);
            }
            step = timesep;
            if ((System.currentTimeMillis() + (Math.min(level * 400, 7000))) - timemillis > 9000) {
                status = 3;
                for (int x = -20; x < 20; x++) {
                    for (int z = -20; z < 20; z++) {
                        final Block b = loc.getWorld().getBlockAt(loc.clone().add(x, 0, z));
                        if (b != null && b.getData() != searchedbyte) {
                            b.setType(Material.AIR);
                        }
                    }
                }
                timemillis = System.currentTimeMillis();
            }
        } else if (status == 0) {
            players.forEach(music::add);
            status = 1;
            generateColors();
            for (final Player pl : players) {
                ms.sendScoreboard(pl, sb);
                final ItemStack block = ms.renameItemStack(new ItemStack(Material.STAINED_CLAY, 1, (byte) searchedbyte), "§d§lMuxBlockParty");
                pl.getInventory().setItem(4, block);
                pl.updateInventory();
            }
        }
    }

    @Override
    public void generate() {
        final World w = loc.getWorld();
        final EditSession session = ms.getFastBuilder(w.getName());
        for (int x = -20; x < 20; x++) {
            for (int z = -20; z < 20; z++) {
                final Location location = loc.clone().add(x, 0, z);
                session.setBlock(location.getBlockX(), location.getBlockY(), location.getBlockZ(), new BaseBlock(BlockID.STAINED_CLAY));
            }
        }
        status = step = 0;
        level = 4;
        session.flushQueue();
    }

    @Override
    public void start() {
        generate();
        for (final Player pl : ms.getServer().getOnlinePlayers()) {
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Das MuxBlockParty Event hat begonnen.", "§d§oKlicke um teilzunehmen", "/event join");
            ms.chatClickHoverRun(pl, "§d§lMuxEvent>§7 Klicke §dhier §7um teilzunehmen.", "§d§oKlicke um teilzunehmen", "/event join");
            pl.playSound(pl.getLocation(), Sound.NOTE_PLING, 1F, 0.1F);
        }
        startCountdown(40, false, false);
        spawnEventNPC("§d§lEvent beitreten");
    }

    @Override
    public boolean join(final Player p) {
        ms.forcePlayer(p, loc.clone().add(0, 4, 0));
        equipPlayer(p); // do it later to give adventure mode
        return true;
    }

    @Override
    public void quit(final Player p) {
        death(p);
    }
}