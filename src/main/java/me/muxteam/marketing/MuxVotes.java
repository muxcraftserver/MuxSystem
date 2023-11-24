package me.muxteam.marketing;

import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import me.muxteam.base.PlayerBase;
import me.muxteam.basic.MuxChatInput;
import me.muxteam.muxsystem.MuxSystem;
import me.muxteam.muxsystem.MySQL;
import me.muxteam.shared.MuxSharedPackets;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import javax.crypto.Cipher;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public final class MuxVotes {
    private final List<UUID> votes;
    private final List<UUID> firstvote;
    private List<String> votecmds = new ArrayList<>();
    private final List<ItemStack> voteitems = new ArrayList<>();
    private final List<String> votemsg = new ArrayList<>();
    private VoteThread votetask;
    private int votetaskID;
    private final MySQL db;
    private MuxSystem ms;

    public MuxVotes(final MuxSystem ms) {
        this.ms = ms;
        this.db = ms.getDB();
        final Server sr = ms.getServer();
        sr.getMessenger().registerIncomingPluginChannel(ms, "VoteAll", ms);
        sr.getMessenger().registerIncomingPluginChannel(ms, "VoteStatus", ms);
        firstvote = db.getFirstVotes();
        votes = db.getVotes();
        ms.getAdmin().VOTES.setActive(toggleVotes(null, true));
    }

    public void close() {
        if (votetask != null) {
            votetask.shutdown();
        }
        final Server sr = ms.getServer();
        sr.getMessenger().unregisterIncomingPluginChannel(ms, "VoteAll", ms);
        sr.getMessenger().unregisterIncomingPluginChannel(ms, "VoteStatus", ms);
        this.ms = null;
    }

    public void onPluginMessage(final String channel, final DataInputStream dis) {
        if (channel.equals("VoteAll")) voteAll();
        else if (channel.equals("VoteStatus")) {
            try {
                setVotes(dis.readBoolean());
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void loadConfig(final FileConfiguration config) {
        votecmds = config.getStringList("vote.commands");
        for (final String s : config.getStringList("vote.items")) {
            final ItemStack it = ms.getItemStack(s.split(" "), 0);
            if (it != null) {
                voteitems.add(it);
            }
        }
        for (final String s : config.getStringList("vote.message")) {
            votemsg.add(ChatColor.translateAlternateColorCodes('&', s));
        }
    }

    public void reload() {
        votemsg.clear();
        voteitems.clear();
    }

    private boolean justreset = false;
    public void reset() {
        if (justreset) {
            ms.broadcastMessage("§c§lFehler: Weiterer Versuch Votes zu resetten!", "muxsystem.op", MuxSystem.Priority.HIGH);
            return;
        }
        if (ms.getShared().isDaemon()) {
            db.resetVotes();
            ms.getShared().getNetty().sendPacket(new MuxSharedPackets.PacketResetVotes());
        }
        votes.clear();
        firstvote.clear();
        ms.getServer().getOnlinePlayers().forEach(p -> {
            ms.chatClickHoverLink(p, String.format(ms.getLang("vote.voteagain"), ms.getServerName()), ms.getLang("vote.hovervote"), "https://muxcraft.eu/vote/?ign=" + p.getName());
            ms.sendTitle(p, "§7§lMux§a§lCraft", 10, 60, 10);
            ms.sendSubTitle(p, ms.getLang("dailyandvotereset"), 10, 60, 10);
            checkifAutoVote(p);
            if (WGBukkit.getRegionManager(p.getWorld()).getApplicableRegions(p.getLocation()).allows(DefaultFlag.PVP))
                ms.getTempFix().add(p.getUniqueId());
        });
        justreset = true;
    }
    public void checkifAutoVote(final Player p) {
        final String pname = p.getName();
        if ((ms.isTrusted(pname) || ms.hasX(p.getUniqueId())) && hasVoted(p) == false) {
            onVote(pname, true);
            onVote(pname, true);
        }
    }

    public boolean hasVoted(final UUID uuid) {
        return votes.contains(uuid);
    }

    public boolean hasVoted(final OfflinePlayer p) {
        return votes.contains(p.getUniqueId()) || votesAreActive() == false;
    }

    public boolean hasFirstVote(final UUID uuid) {
        return firstvote.contains(uuid);
    }

    public boolean votesAreActive() {
        return ms.getAdmin().VOTES.isActive();
    }

    public int getVoted() {
        return votes.size();
    }

    public void setVotes(final boolean enabled) {
        ms.getAdmin().VOTES.setActive(enabled);
        toggleVotes(null, enabled);
    }

    public void voteAll() {
        ms.getServer().getOnlinePlayers().forEach(pl -> {
            final UUID uuid = pl.getUniqueId();
            if (votes.contains(uuid) == false) {
                onVote(pl.getName(), true);
                onVote(pl.getName(), true);
            }
        });
    }

    public void addVote(final UUID uuid) {
        if (ms.getShared().isDaemon()) {
            db.addVote(uuid);
            db.addSecondVote(uuid);
        }
        if (firstvote.contains(uuid) == false)
            firstvote.add(uuid);
        if (votes.contains(uuid) == false)
            votes.add(uuid);
    }

    public boolean handleCommand(final Player p, final String str) {
        if (hasVoted(p)) {
            ms.openBook(ms.linkBook("§lVote", "Danke, du hast bereits gevotet.", new Object[]{"https://muxcraft.eu/", "Zur Website >", "Link öffnen"}), p);
            return true;
        }
        final MuxEmails.MuxMailUser mailUser = ms.getEmails().getUser(p.getUniqueId());
        if (str != null && (str.equals("emailverify"))) {
            if (mailUser.isVerified() == false) {
                final AtomicReference<MuxChatInput> reference = new AtomicReference<>();
                final MuxChatInput chatInput = new MuxChatInput(this.ms, (input, p1) -> {
                    final MuxEmails.MailVerifyResult r = ms.getEmails().tryToVerifyEmail(p1, input.toLowerCase(), mailUser);
                    if (r != MuxEmails.MailVerifyResult.ERROR)
                        reference.get().cancel();
                });
                reference.set(chatInput);
                chatInput.show(p, "§f§lGebe deine Email Adresse ein:", true, "§7Klicke, um den Prozess abzubrechen.", 3);
                return true;
            }
        }
        if (ms.getEmails().isEnabled() == false || (mailUser != null && mailUser.isVerified())) {
            this.openDefaultVoteBook(p);
        } else {
            ms.resetCooldown(p.getName(), "CMDSPAM");
            final ItemStack book = ms.mixedBook("§lVote", "Vote für unglaubliche Belohnungen.\nDu kannst maximal zwei mal am Tag voten.",
                    new Object[]{"https://muxcraft.eu/vote/?ign=" + p.getName(), "Voten >\n\n", "Link öffnen", ClickEvent.Action.OPEN_URL},
                    new Object[]{"/vote emailverify", "§2§nKeine Lust zu voten? §2§nVerifiziere deine Email.", "Email verifizieren", ClickEvent.Action.RUN_COMMAND});
            ms.openBook(book, p);
        }
        return true;
    }

    private void openDefaultVoteBook(final Player p) {
        ms.openBook(ms.linkBook("§lVote", "Vote für unglaubliche Belohnungen.\nDu kannst maximal zwei mal am Tag voten.", new Object[]{"https://muxcraft.eu/vote/?ign=" + p.getName(), "Voten >", "Link öffnen"}), p);
    }

    public void onVote(final String n, final boolean votegive) {
        if (Bukkit.isPrimaryThread() == false) {
            new BukkitRunnable() {
                @Override public void run() {
                    onVote(n, votegive);
                }
            }.runTask(ms);
            return;
        }
        final Server sr = ms.getServer();
        final Map.Entry<String, OfflinePlayer> entry = ms.getPlayerAndName(n);
        final String name = entry.getKey();
        final OfflinePlayer op = entry.getValue();
        if (op == null) return;
        final Player p = op.getPlayer();
        final String pname = p != null ? p.getName() : name;
        final UUID uuid = op.getUniqueId();
        if (firstvote.contains(uuid) == false && votes.contains(uuid) == false) {
            firstvote.add(uuid);
            if (ms.getShared().isDaemon() || votegive)
                db.addVote(uuid);
            if (votegive == false && p != null) {
                p.sendMessage(" ");
                p.sendMessage("§b§lFAST FERTIG!");
                p.playSound(p.getLocation(), Sound.ENDERDRAGON_WINGS, 1F, 1F);
                ms.chatClickHoverLink(p, "§7Klicke um nochmal zu voten und Belohnungen freizuschalten.", ms.getLang("vote.hovervote"), "https://muxcraft.eu/vote/?ign=" + p.getName());
                p.sendMessage(" ");
            }
            return;
        } else if (votes.contains(uuid) == false) {
            if (ms.getShared().isDaemon() || votegive)
                db.addSecondVote(uuid);
            votes.add(uuid);
            if (p != null && ms.inEvent(p)) ms.getEvents().quitEvent(p, false);
        } else {
            return;
        }
        final ConsoleCommandSender cs = sr.getConsoleSender();
        for (final String s : votecmds) {
            sr.dispatchCommand(cs, s.replace("[NAME]", pname));
        }
        if (ms.getBase().hasBase(uuid)) {
            final PlayerBase base = ms.getBase().getFromUUID(uuid);
            if (base.getSize() < 96) {
                base.setSize(base.getSize() + 2);
                if (p != null) {
                    final PlayerBase pb = ms.getBase().getCurrentLocations().get(p);
                    if (pb != null && pb.getOwner().equals(p.getUniqueId())) base.extendedNotification(p);
                }
            }
        }
        if (p != null) {
            if (votegive) {
                if (ms.isVanish(p)) return;
                broadcastVote(pname);
                return;
            }
            votemsg.forEach(p::sendMessage);
            for (final ItemStack item : voteitems) {
                p.getInventory().addItem(item.clone());
            }
            if (ms.isVanish(p) == false) {
                final World w = p.getWorld();
                final Location l = p.getLocation().add(0, 0.5D, 0);
                w.playEffect(l, Effect.VILLAGER_THUNDERCLOUD, 0);
                w.playEffect(l, Effect.HEART, 0);
                w.playEffect(l, Effect.CLOUD, 0);
                w.playEffect(l, Effect.EXPLOSION, 0);
                w.playEffect(l, Effect.EXPLOSION_LARGE, 0);
                w.playEffect(l, Effect.SMOKE, 0);
                w.playEffect(l, Effect.WITCH_MAGIC, 0);
                broadcastVote(pname);
            }
        }
    }

    private void broadcastVote(final String name) {
        ms.getServer().getOnlinePlayers().forEach(pl -> {
            if (ms.hasVoted(pl) == false) {
                if (pl.getName().equals(name) == false)
                    pl.sendMessage(String.format(ms.getLang("vote.justvoted"), name, ms.getServerName()));
                ms.chatClickHoverLink(pl, ms.getLang("vote.clickvote3"), ms.getLang("vote.hovervote2"), "https://muxcraft.eu/vote/?ign=" + pl.getName());
            }
        });
    }

    private KeyPair generateRSA() throws Exception {
        final KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
        keygen.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4));
        final KeyPair kp = keygen.generateKeyPair();
        try (final FileOutputStream out = new FileOutputStream(ms.getDataFolder() + "/vote/public.key")) {
            out.write(javax.xml.bind.DatatypeConverter.printBase64Binary(new X509EncodedKeySpec(kp.getPublic().getEncoded()).getEncoded()).getBytes(StandardCharsets.UTF_8));
        }
        try (final FileOutputStream out = new FileOutputStream(ms.getDataFolder() + "/vote/private.key")) {
            out.write(javax.xml.bind.DatatypeConverter.printBase64Binary(new PKCS8EncodedKeySpec(kp.getPrivate().getEncoded()).getEncoded()).getBytes(StandardCharsets.UTF_8));
        }
        return kp;
    }

    private KeyPair loadRSA() throws Exception {
        byte[] encodedPublicKey, encodedPrivateKey;
        try (final FileInputStream in = new FileInputStream(ms.getDataFolder() + "/vote/public.key")) {
            encodedPublicKey = new byte[(int) new File(ms.getDataFolder() + "/vote/public.key").length()];
            final int bytesRead = in.read(encodedPublicKey);
            if (bytesRead != encodedPublicKey.length) {
                throw new IOException("Not all bytes were read from public key file.");
            }
            encodedPublicKey = javax.xml.bind.DatatypeConverter.parseBase64Binary(new String(encodedPublicKey, StandardCharsets.UTF_8));
        }
        try (final FileInputStream in = new FileInputStream(ms.getDataFolder() + "/vote/private.key")) {
            encodedPrivateKey = new byte[(int) new File(ms.getDataFolder() + "/vote/private.key").length()];
            final int bytesRead = in.read(encodedPrivateKey);
            if (bytesRead != encodedPrivateKey.length) {
                throw new IOException("Not all bytes were read from private key file.");
            }
            encodedPrivateKey = javax.xml.bind.DatatypeConverter.parseBase64Binary(new String(encodedPrivateKey, StandardCharsets.UTF_8));
        }
        final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return new KeyPair(keyFactory.generatePublic(new X509EncodedKeySpec(encodedPublicKey)), keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encodedPrivateKey)));
    }

    public boolean toggleVotes(final Player p, final boolean enable) {
        if (votetask != null) {
            votetask.shutdown();
            ms.getServer().getScheduler().cancelTask(votetaskID);
            if (enable == false) return false;
        }
        final File votefolder = new File(ms.getDataFolder() + "/vote");
        KeyPair kp;
        try {
            if (votefolder.exists()) {
                kp = loadRSA();
            } else {
                if (votefolder.mkdir() == false) {
                    if (p != null) ms.showItemBar(p,"§cError: The Vote folder could not be created.");
                    System.err.println("MuxSystem> The Vote folder could not be created.");
                    return false;
                }
                kp = generateRSA();
            }
        } catch (final Exception e) {
            if (p != null) ms.showItemBar(p, "§cError: The Votekey could not be loaded.");
            System.err.println("MuxSystem> The Votekey could not be loaded.");
            return false;
        }
        try {
            votetask = new VoteThread(ms.getServer().getIp(), kp);
            votetaskID = ms.getServer().getScheduler().runTaskAsynchronously(ms, votetask).getTaskId();
        } catch (final Exception e) {
            if (p != null) ms.showItemBar(p,"§cError: There was a connection error.");
            return false;
        }
        return true;
    }

    class VoteThread implements Runnable {
        private ServerSocket server;
        private final KeyPair keypair;
        private boolean running = true;

        public VoteThread(final String ip, final KeyPair keypair) {
            this.keypair = keypair;
            try {
                this.server = new ServerSocket();
                this.server.bind(new InetSocketAddress(ip, 8193));
            } catch (final Exception e) {
                this.running = false;
                System.err.println("MuxSystem> Error while starting the Vote Thread.");
            }
        }

        public void shutdown() {
            this.running = false;
            if (this.server == null) return;
            try {
                this.server.close();
            } catch (final Exception e) {
                System.err.println("MuxSystem> Error while stopping the Vote Thread.");
            }
        }

        @Override
        public void run() {
            while (this.running) {
                try (final Socket socket = this.server.accept()) {
                    socket.setSoTimeout(5000);
                    try (final OutputStreamWriter outstream = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)) {
                        try (final BufferedWriter writer = new BufferedWriter(outstream)) {
                            try (final InputStream in = socket.getInputStream()) {
                                writer.write("VOTIFIER 1.9");
                                writer.newLine();
                                writer.flush();
                                byte[] block = new byte[256];
                                final int bytesRead = in.read(block, 0, block.length);
                                if (bytesRead != block.length) {
                                    throw new IOException("Not all bytes were read from the input stream.");
                                }
                                block = decrypt(block, keypair.getPrivate());
                                int position = 0;
                                final String opcode = readString(block, position);
                                position += opcode.length() + 1;
                                if (opcode.equals("VOTE") == false) {
                                    throw new Exception();
                                }
                                final String serviceName = readString(block, position);
                                position += serviceName.length() + 1;
                                final String username = readString(block, position);
                                position += username.length() + 1;
                                final String address = readString(block, position);
                                position += address.length() + 1;
                                final String timeStamp = readString(block, position);
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        onVote(username, false);
                                    }
                                }.runTask(ms);
                                System.out.println("MuxVote> " + username);
                            }
                        }
                    }
                } catch (final Exception ignored) {
                }
            }
        }

        private byte[] decrypt(final byte[] data, final PrivateKey key) throws Exception {
            final Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(2, key);
            return cipher.doFinal(data);
        }

        private String readString(final byte[] data, final int offset) {
            final StringBuilder builder = new StringBuilder();
            for (int i = offset; i < data.length && data[i] != 10; i++) {
                builder.append((char) data[i]);
            }
            return builder.toString();
        }
    }
}