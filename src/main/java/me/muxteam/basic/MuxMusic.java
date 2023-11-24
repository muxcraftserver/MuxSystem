package me.muxteam.basic;

import com.xxmicloxx.NoteBlockAPI.NoteBlockAPI;
import com.xxmicloxx.NoteBlockAPI.event.SongEndEvent;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import com.xxmicloxx.NoteBlockAPI.songplayer.SongPlayer;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class MuxMusic implements Listener {
    private final List<String> history = new ArrayList<>();
    private SongPlayer sp;
    private String song;
    private final Random r = new Random();
    private final MuxSystem ms;

    public MuxMusic(final MuxSystem ms) {
        this.ms = ms;
        ms.getServer().getPluginManager().registerEvents(this, ms);
    }
    public void close() {}
    public void remove(final Player p) {
        NoteBlockAPI.setPlayerVolume(p, (byte) 0);
    }
    public void add(final Player p) {
        if (sp != null) sp.addPlayer(p);
        NoteBlockAPI.setPlayerVolume(p, (byte) 70);
    }
    public void playSong(final String song, final Collection<Player> list, final boolean force) {
        final Song s = NBSDecoder.parse(new File(ms.getDataFolder() + "/music", song + ".nbs"));
        if (s != null) {
            sp = new RadioSongPlayer(s);
            this.song = song;
            sp.setAutoDestroy(true);
            for (final Player pl : list) {
                if (force || ms.getMuxUser(pl.getName()).getSettings().hasMusic()) add(pl);
            }
            sp.setPlaying(true);
        }
    }
    @EventHandler
    public void onSongEnd(final SongEndEvent e) {
        if (e.getSongPlayer() == sp) sp = null;
    }
    public boolean isPlaying() {
        return sp != null;
    }
    public String getSongName() {
        return sp == null ? "" : sp.getSong().getTitle() == null || sp.getSong().getTitle().isEmpty() ? song : sp.getSong().getTitle();
    }
    public String getRandomSongName() {
        final List<String> songs = new ArrayList<>();
        final File folder = new File(ms.getDataFolder() + "/music");
        final File[] files = folder.listFiles();
        if (files == null) return "";
        for (final File s : files) {
            songs.add(s.getName().replace(".nbs", ""));
        }
        if (history.size() == songs.size()) history.clear();
        final String ret = songs.get(r.nextInt(songs.size()));
        if (history.isEmpty() == false && history.contains(ret)) {
            return getRandomSongName();
        }
        history.add(ret);
        return ret;
    }
}
