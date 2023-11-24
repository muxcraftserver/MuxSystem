package me.muxteam.shared;

import io.netty.util.internal.ConcurrentSet;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MuxShared {
    private final MuxSystem ms;
    private boolean isDaemon = false;

    private final MuxNetty netty;

    private final Map<UUID, String> uuidNameCache = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> onlinePlayers = new ConcurrentHashMap<>(); // player uuid & server uuid
    private final Set<String> playersInVanish = new ConcurrentSet<>();

    public MuxShared(final MuxSystem ms) {
        this.ms = ms;
        this.netty = new MuxNetty(ms);
        netty.setup(this);
    }

    public void setDaemon(boolean daemon) {
        System.out.println("setDaemon to " + daemon);
        isDaemon = daemon;
    }

    public boolean isDaemon() {
        return isDaemon;
    }

    public MuxNetty getNetty() {
        return netty;
    }

    public void shutdown() {
        if (isDaemon) ms.getDB().deleteSharedData("NettyServerAddress");
        if (this.netty != null && this.netty.getEndPoint() != null && this.netty.getEndPoint().isConnected())
            this.netty.getEndPoint().close();
    }

    public void updateCachedName(final UUID uuid, final String newName) {
        if (this.uuidNameCache.containsKey(uuid))
            this.addNameToCache(uuid, newName);
    }

    public void broadcastCachedNameUpdate(final UUID uuid, final String newName) {
        this.netty.sendPacket(new MuxSharedPackets.PacketUpdateCachedName(uuid, newName));
    }

    public String getNameFromUUID(final UUID uuid) {
        String cachedName = uuidNameCache.get(uuid);
        if (cachedName == null) {
            cachedName = this.fetchName(uuid);
            if (cachedName != null)
                uuidNameCache.put(uuid, cachedName);
        }
        return cachedName;
    }

    private String fetchName(final UUID uuid) {
        final Player player = ms.getServer().getPlayer(uuid);
        return player != null ? player.getName() : this.ms.getDB().getPlayerName(uuid); // no need to fetch the name from the db if the player is online
    }

    public void addNameToCache(final UUID uuid, final String name) {
        this.uuidNameCache.put(uuid, name);
    }

    public void removeNameFromCache(final UUID uuid, final String name) {
        this.uuidNameCache.remove(uuid, name);
    }

    public String getMachineIp() throws IOException {
        final URL aws = new URL("https://checkip.amazonaws.com");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(aws.openStream(), StandardCharsets.UTF_8))) {
            return in.readLine();
        }
    }

    public void onSharedJoin(final UUID player, final UUID server) {
        this.onlinePlayers.put(player, server);
    }

    public void onSharedQuit(final UUID player) {
        this.onlinePlayers.remove(player);
    }

    public void onSharedVanish(final String player, boolean remove) {
        if (remove)
            this.playersInVanish.remove(player);
        else
            this.playersInVanish.add(player);
    }

    public Map<UUID, UUID> getOnlinePlayers() {
        return onlinePlayers;
    }

    public Set<String> getPlayersInVanish() {
        return playersInVanish;
    }

    public String getServerName(final UUID player) {
        return this.onlinePlayers.containsKey(player) ? ms.getServers().get(this.onlinePlayers.get(player)) : null;
    }
}