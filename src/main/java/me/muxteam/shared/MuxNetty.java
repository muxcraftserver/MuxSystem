package me.muxteam.shared;

import de.vantrex.simplenetty.annotations.exceptions.AnnotationNotFoundException;
import de.vantrex.simplenetty.client.SimpleClient;
import de.vantrex.simplenetty.client.settings.SimpleClientSettings;
import de.vantrex.simplenetty.listener.SimplePacketListener;
import de.vantrex.simplenetty.listener.SimpleSessionListener;
import de.vantrex.simplenetty.listener.handler.SimplePacketHandler;
import de.vantrex.simplenetty.packet.SimplePacket;
import de.vantrex.simplenetty.packet.exceptions.PacketAlreadyRegisteredException;
import de.vantrex.simplenetty.packet.exceptions.PacketIdAlreadyRegisteredException;
import de.vantrex.simplenetty.protocol.Protocol;
import de.vantrex.simplenetty.protocol.impl.NumericProtocol;
import de.vantrex.simplenetty.server.SimpleServer;
import de.vantrex.simplenetty.server.settings.SimpleServerSettings;
import de.vantrex.simplenetty.session.Session;
import me.muxteam.basic.MuxActions;
import me.muxteam.extras.MuxExtraUser;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class MuxNetty {

    private static final int MIN_PORT = 38105;
    private static final int MAX_PORT = 38130;

    private final MuxSystem ms;
    private MuxShared shared;
    private EndPoint<?, NumericProtocol> endPoint;

    public MuxNetty(final MuxSystem ms) {
        this.ms = ms;
    }

    public void sendPacket(final SimplePacket packet) {
        if (this.endPoint == null)
            return;
        this.endPoint.send(packet);
    }

    public void setup(final MuxShared shared) {
        this.shared = shared;
        try {
            this.endPoint = establishEndpoint();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.endPoint.getProtocol().registerListener(new MuxPacketListener(ms, endPoint));
        endPoint.getProtocol().registerSessionListener(new MuxSessionListener(ms, endPoint));
        ms.getLogger().info("Netty Endpoint erstellt!");
    }

    public EndPoint<?, NumericProtocol> establishEndpoint() throws IOException {
        EndPoint<?, NumericProtocol> endPoint;
        final String nettyAddressData = ms.getDB().getSharedData("NettyServerAddress");
        if (nettyAddressData == null || nettyAddressData.equalsIgnoreCase(shared.getMachineIp())) {
            // NEW DAEMON
            try {
                ms.getDB().setSharedData("NettyServerAddress", shared.getMachineIp());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            final int port = generatePort();
            ms.getDB().setSharedData("NettyServerPort", String.valueOf(port));

            this.shared.setDaemon(true);
            endPoint = new NettyServer(new SimpleServer(new SimpleServerSettings()
                    .bossThreads(2)
                    .workerThreads(2)
                    .logger(false)
                    .port(port)
                    .protocol(new NumericProtocol())
            ));
            // simpleServer.getSettings().
        } else {
            this.shared.setDaemon(false);
            final int port = Integer.parseInt(ms.getDB().getSharedData("NettyServerPort"));
            endPoint = new NettyClient(new SimpleClient(new SimpleClientSettings()
                    .threads(2)
                    .port(port)
                    .address(nettyAddressData)
                    .logger(false)
                    .protocol(new NumericProtocol())));
        }
        try {
            endPoint.getProtocol().registerPackets(Arrays.asList(
                    MuxSharedPackets.PacketResetVotes.class,
                    MuxSharedPackets.PacketGiveVote.class,
                    MuxSharedPackets.PacketUpdateCachedName.class,
                    MuxSharedPackets.PacketUpdateExtraCommand.class,
                    MuxSharedPackets.PacketUpdateExtraPerk.class,
                    MuxSharedPackets.PacketUpdateExtraPet.class,
                    MuxSharedPackets.PacketUpdateExtraMount.class,
                    MuxSharedPackets.PacketUpdateExtraEmoji.class,
                    MuxSharedPackets.PacketUpdateExtraEnderChest.class,
                    MuxSharedPackets.PacketUpdateRank.class,
                    MuxSharedPackets.PacketAddChest.class,
                    MuxSharedPackets.PacketRemoveChest.class,
                    MuxSharedPackets.PacketPlayerJoinServer.class,
                    MuxSharedPackets.PacketPlayerQuitServer.class,
                    MuxSharedPackets.PacketPlayerVanish.class,
                    MuxSharedPackets.PacketReloadTagData.class,
                    MuxSharedPackets.PacketWhitelistDeactivate.class
            ));
        } catch (final AnnotationNotFoundException | PacketAlreadyRegisteredException |
                       PacketIdAlreadyRegisteredException e) {
            throw new RuntimeException(e);
        }
        return endPoint;
    }

    private int generatePort() {
        for (int port = MIN_PORT; port < MAX_PORT; port++) {
            if (portIsAvailable(port)) return port;
        }
        return 38102;
    }

    private boolean portIsAvailable(int port) {
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }
        return false;
    }


    public EndPoint<?, NumericProtocol> getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(final EndPoint<?, NumericProtocol> endPoint) {
        this.endPoint = endPoint;
    }

    public class MuxPacketListener implements SimplePacketListener {
        private final MuxSystem ms;
        private final EndPoint<?, NumericProtocol> endPoint;

        public MuxPacketListener(final MuxSystem ms, final EndPoint<?, NumericProtocol> endPoint) {
            this.ms = ms;
            this.endPoint = endPoint;
        }

        @SimplePacketHandler
        public void handlePacket(final SimplePacket packet, final Session session) {
            if (endPoint instanceof NettyServer) {
                endPoint.getProtocol().getSessions().stream()
                        .filter(session1 -> session1 != session)
                        .forEach(session1 -> session1.send(packet));
            }
        }

        @SimplePacketHandler
        public void onVoteReset(final MuxSharedPackets.PacketResetVotes ignoredPacket) {
            if (shared.isDaemon()) return;
            runSync(() -> ms.getVotes().reset());
        }

        @SimplePacketHandler
        public void onVoteGive(final MuxSharedPackets.PacketGiveVote packet) {
            ms.queueNotImportantSyncAction(() -> ms.getVotes().addVote(packet.getUuid()));
        }

        @SimplePacketHandler
        public void onNameUpdate(final MuxSharedPackets.PacketUpdateCachedName packet) {
            ms.queueNotImportantSyncAction(() -> shared.updateCachedName(packet.getPlayer(), packet.getNewName()));
        }

        @SimplePacketHandler
        public void onExtraCommand(final MuxSharedPackets.PacketUpdateExtraCommand packet) {
            runSync(() -> {
                final Player player = ms.getServer().getPlayer(packet.getUUID());
                if (player == null) return;
                final MuxExtraUser u = ms.getExtras().getExtraUser(packet.getUUID());
                if (u.getName().equals("N/A")) return;
                if (packet.isAdd()) {
                    u.getExtraCMDs().add(packet.getCommand());
                } else {
                    u.getExtraCMDs().remove(packet.getCommand());
                }
                u.setChanged(true);
            });
            if (packet.getExecutor() != null)
                ms.getHistory().addHistory(packet.getUUID(), packet.getExecutor(), "TEAMACTION", "EXTRACMD" + (packet.isAdd() ? " ADD" : " REMOVE"), packet.getCommand());
        }

        @SimplePacketHandler
        public void onExtraPerk(final MuxSharedPackets.PacketUpdateExtraPerk packet) {
            runSync(() -> {
                final Player player = ms.getServer().getPlayer(packet.getUUID());
                if (player == null) return;
                final MuxExtraUser u = ms.getExtras().getExtraUser(packet.getUUID());
                if (u.getName().equals("N/A")) return;
                if (packet.isAdd()) {
                    u.getPerks().addOwned(packet.getPerk());
                } else {
                    u.getPerks().getOwned().remove(packet.getPerk());
                    if (u.getPerks().getActive().remove(packet.getPerk())) {
                        ms.getPerks().removeActivePerk(player, packet.getPerk());
                    }
                }
                u.setChanged(true);
            });
            if (packet.getExecutor() != null)
                ms.getHistory().addHistory(packet.getUUID(), packet.getExecutor(), "TEAMACTION", "PERKS" + (packet.isAdd() ? " ADD" : " REMOVE"), ms.getPerks().getPerkName(packet.getPerk()));
        }

        @SimplePacketHandler
        public void onExtraPet(final MuxSharedPackets.PacketUpdateExtraPet packet) {
            runSync(() -> {
                final Player player = ms.getServer().getPlayer(packet.getUUID());
                if (player == null) return;
                final MuxExtraUser u = ms.getExtras().getExtraUser(packet.getUUID());
                if (u.getName().equals("N/A")) return;
                if (packet.isAdd())
                    u.getPets().getOwned().add(packet.getPet());
                else {
                    if (u.getPets().getActive() == packet.getPet()) {
                        ms.getPets().setPet(player, null, false);
                        u.getPets().setActive(null);
                    }
                    u.getPets().getOwned().remove(packet.getPet());
                }
                u.setChanged(true);
            });
            if (packet.getExecutor() != null)
                ms.getHistory().addHistory(packet.getUUID(), packet.getExecutor(), "TEAMACTION", "PETS" + (packet.isAdd() ? " ADD" : " REMOVE"), ms.getPets().getPetName(packet.getPet()));
        }

        @SimplePacketHandler
        public void onExtraMount(final MuxSharedPackets.PacketUpdateExtraMount packet) {
            runSync(() -> {
                final Player player = ms.getServer().getPlayer(packet.getUUID());
                if (player == null) return;
                final MuxExtraUser u = ms.getExtras().getExtraUser(packet.getUUID());
                if (u.getName().equals("N/A")) return;
                if (packet.isAdd())
                    u.getMounts().addOwned(packet.getMount());
                else {
                    if (u.getMounts().getActive() == packet.getMount()) {
                        ms.getMounts().setMount(player, packet.getMount(), false);
                        u.getMounts().setActive((short) -1);
                    }
                }
                u.setChanged(true);
            });
            if (packet.getExecutor() != null)
                ms.getHistory().addHistory(packet.getUUID(), packet.getExecutor(), "TEAMACTION", "MOUNTS" + (packet.isAdd() ? " ADD" : " REMOVE"), ms.getMounts().getMountName(packet.getMount()));
        }

        @SimplePacketHandler
        public void onExtraEmoji(final MuxSharedPackets.PacketUpdateExtraEmoji packet) {
            runSync(() -> {
                final Player player = ms.getServer().getPlayer(packet.getUUID());
                if (player == null) return;
                final MuxExtraUser u = ms.getExtras().getExtraUser(packet.getUUID());
                if (u.getName().equals("N/A")) return;
                if (packet.isAdd())
                    u.getEmojis().add(packet.getEmoji());
                else {
                    if (ms.getEmojis().getActiveEmoji(player.getUniqueId()) == (int) packet.getEmoji()) {
                        ms.getEmojis().setEmoji(player, -1);
                    }
                    u.getEmojis().remove(packet.getEmoji());
                }
                u.setChanged(true);
            });
            if (packet.getExecutor() != null)
                ms.getHistory().addHistory(packet.getUUID(), packet.getExecutor(), "TEAMACTION", "EMOJIS" + (packet.isAdd() ? " ADD" : " REMOVE"), ms.getEmojis().getEmojiName(packet.getEmoji()));
        }

        @SimplePacketHandler
        public void onExtraEnderChest(final MuxSharedPackets.PacketUpdateExtraEnderChest packet) {
            runSync(() -> {
                final Player player = ms.getServer().getPlayer(packet.getUUID());
                if (player == null) return;
                final MuxExtraUser u = ms.getExtras().getExtraUser(packet.getUUID());
                if (u.getName().equals("N/A")) return;
                ms.getExtras().setExtraEnderChest(u.getUUID(), u, packet.isAdd());
                u.setChanged(true);
            });
            if (packet.getExecutor() != null)
                ms.getHistory().addHistory(packet.getUUID(), packet.getExecutor(), "TEAMACTION", "EXTRAENDER" + (packet.isAdd() ? " ADD" : " REMOVE"), "ENDERCHEST");
        }

        @SimplePacketHandler
        public void onRankUpdate(final MuxSharedPackets.PacketUpdateRank packet) {
            runSync(() -> {
                final Player player = ms.getServer().getPlayer(packet.getUUID());
                if (player == null) return;
                ms.getPerms().changePlayerGroup(packet.getUUID(), player.getName(), packet.getUpdatedRank(), ms.getServer().getConsoleSender(), true);
            });
        }

        private final Set<SimplePacket> chestDelay = new HashSet<>();

        @SimplePacketHandler
        public void onChestAdd(final MuxSharedPackets.PacketAddChest packet) {
            if (chestDelay.contains(packet) == false)
                ms.getHistory().addHistory(packet.getUUID(), packet.getExecutor(), "TEAMACTION", "CRATE ADD", packet.getChestType().name());
            chestDelay.remove(packet);
            final Player player = ms.getServer().getPlayer(packet.getUUID());
            if (player == null) return;
            runSync(() -> {
                if (ms.getChests().containsChestUser(packet.getUUID()) == false) { // try it delayed
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            chestDelay.add(packet);
                            onChestAdd(packet);
                        }
                    }.runTaskLater(ms, 20L * 3L);
                    return;
                }
                ms.getChests().getChestUser(packet.getUUID()).addChest(packet.getChestType(), packet.isBypass());
            });
        }

        @SimplePacketHandler
        public void onChestRemove(final MuxSharedPackets.PacketRemoveChest packet) {
            if (chestDelay.contains(packet) == false)
                ms.getHistory().addHistory(packet.getUUID(), packet.getExecutor(), "TEAMACTION", "CRATE REMOVE", packet.getChestName());
            chestDelay.remove(packet);
            final Player player = ms.getServer().getPlayer(packet.getUUID());
            if (player == null) return;
            runSync(() -> {
                if (ms.getChests().containsChestUser(packet.getUUID()) == false) { // try it delayed
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            chestDelay.add(packet);
                            onChestRemove(packet);
                        }
                    }.runTaskLater(ms, 20L * 3L);
                    return;
                }
                ms.getChests().getChestUser(packet.getUUID()).removeChest(packet.getSlot());
            });
        }

        @SimplePacketHandler
        public void onQuit(final MuxSharedPackets.PacketPlayerQuitServer packet) {
            shared.onSharedQuit(packet.getPlayer());
        }

        @SimplePacketHandler
        public void onJoin(final MuxSharedPackets.PacketPlayerJoinServer packet) {
            shared.onSharedJoin(packet.getPlayer(), packet.getServer());
        }

        @SimplePacketHandler
        public void onVanish(final MuxSharedPackets.PacketPlayerVanish packet) {
            shared.onSharedVanish(packet.getPlayer(), packet.isRemove());
        }

        @SimplePacketHandler
        public void onReloadTag(final MuxSharedPackets.PacketReloadTagData packet) {
            runSync(() -> ms.getCustomRank().reloadTag(packet.getPlayer()));
        }

        @SimplePacketHandler
        public void onServerWhitelistDeactivate(final MuxSharedPackets.PacketWhitelistDeactivate packet) {
            runSync(() -> ms.getPerms().setWhitelistActivateTime(System.currentTimeMillis()));
        }

        private void runSync(final MuxActions.Action action) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    action.call();
                }
            }.runTask(ms);
        }
    }

    public class MuxSessionListener implements SimpleSessionListener {
        private final MuxSystem ms;
        private int killSwitch = 0;
        private EndPoint<?, NumericProtocol> endPoint;

        public MuxSessionListener(final MuxSystem ms, final EndPoint<?, NumericProtocol> endPoint) {
            this.ms = ms;
            this.endPoint = endPoint;
        }

        @Override
        public void onDisconnect(Session session) {
            System.out.println("Session disconnect: " + session);
            if (endPoint instanceof NettyClient && ms.isShuttingDown() == false) {
                endPoint.getProtocol().setClientSession(null);
                endPoint = null;
                establishNewConnectionOnDisconnect();
            }
        }

        @Override
        public void onConnect(final Session session) {
            System.out.println("Session connect: " + session);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (endPoint.isConnected())
                        killSwitch = 0;
                }
            }.runTaskLater(ms, 20L * 5L);
        }

        private void establishNewConnectionOnDisconnect() {
            if (ms.isShuttingDown()) return;
            this.ms.getLogger().info("Trying to establish new connection to shared manager.");
            this.killSwitch++;
            if (this.killSwitch == 5) { // --> THIS NEVER HAPPENS SINCE WE RESET, BUT IF SOMETHING WEIRD HAPPENS WE SHOULD BETTER SHUTDOWN THE SERVER
                ms.logoutAll("CONSOLE", ms.getLang("restart"));
                ms.getDB().deleteSharedData("NettyServerAddress");
                ms.getServer().shutdown();
                return;
            }
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (ms.isEnabled() == false) return;
                    try {
                        endPoint = shared.getNetty().establishEndpoint();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    shared.getNetty().setEndPoint(endPoint);
                    endPoint.getProtocol().registerSessionListener(MuxSessionListener.this);
                    endPoint.getProtocol().registerListener(new MuxPacketListener(ms, endPoint));
                }
            }.runTaskLater(ms, ThreadLocalRandom.current().nextLong(40));
        }
    }

    public interface EndPoint<T, P> {
        void close();

        void connect();

        void changeSettings(T t);

        boolean isConnected();

        void stop();

        void send(final SimplePacket packet);

        Protocol<P> getProtocol();
    }


    public class NettyServer implements EndPoint<SimpleServerSettings, NumericProtocol> {
        private final SimpleServer server;

        public NettyServer(final SimpleServer server) {
            this.server = server;
        }

        @Override
        public void close() {
            synchronized (server) {
                server.close();
            }
        }

        @Override
        public void connect() {
        }

        @Override
        public void changeSettings(final SimpleServerSettings simpleServerSettings) {
        }

        @Override
        public boolean isConnected() {
            return server != null && server.getChannel() != null && server.getChannel().isActive();
        }

        @Override
        public void stop() {
        }

        @Override
        public void send(final SimplePacket packet) {
            if (this.server == null || this.server.getChannel() == null || this.server.getChannel().isActive() == false)
                return;
            this.server.send(packet);
        }

        @Override
        public Protocol<NumericProtocol> getProtocol() {
            return (Protocol<NumericProtocol>) this.server.getSettings().protocol();
        }
    }

    public class NettyClient implements EndPoint<SimpleClientSettings, NumericProtocol> {
        private final SimpleClient client;

        public NettyClient(final SimpleClient client) {
            this.client = client;
        }

        @Override
        public void close() {
            if (this.client.getChannel() != null && this.client.getChannel().isActive())
                this.client.close();
        }

        @Override
        public void connect() {
            this.client.connect();
        }

        @Override
        public void changeSettings(final SimpleClientSettings simpleClientSettings) {
            this.client.getSettings().port(simpleClientSettings.port());
        }

        @Override
        public boolean isConnected() {
            return this.client != null && this.client.getChannel() != null && this.client.getChannel().isActive();
        }

        @Override
        public void stop() {
            client.getWorker().shutdownGracefully();
            client.getSettings().protocol().setClientSession(null);
            client.getSettings().protocol().close();
        }

        @Override
        public void send(final SimplePacket packet) {
            if (this.client == null || this.client.getChannel() == null || this.client.getChannel().isActive() == false) return;
            client.send(packet);
        }

        @Override
        public Protocol<NumericProtocol> getProtocol() {
            return (Protocol<NumericProtocol>) this.client.getSettings().protocol();
        }
    }
}