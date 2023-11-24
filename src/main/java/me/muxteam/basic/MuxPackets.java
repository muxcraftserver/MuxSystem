package me.muxteam.basic;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayInUseEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class MuxPackets {
	private final HashMap<UUID, PacketReader> reader = new HashMap<>();
	private final MuxNPCs npcs;
	private final Field idfield;

	public MuxPackets(final MuxSystem ms) {
		npcs = ms.getNPCS();
		this.idfield = NMSReflection.getField(PacketPlayInUseEntity.class, "a");
	}

	public void handleJoin(final Player p) {
		reader.put(p.getUniqueId(), new PacketReader(p));
	}
	public void handleQuit(final Player p) {
		final PacketReader pr = reader.remove(p.getUniqueId());
		if (pr != null) pr.uninject();
	}

	class PacketReader {
		private final Player player;
		public Channel channel;

		public PacketReader(final Player player) {
			this.player = player;
			this.inject();
		}

		private void inject() {
			final CraftPlayer craftPlayer = (CraftPlayer) this.player;
			this.channel = craftPlayer.getHandle().playerConnection.networkManager.channel;
			try {
				if (channel.pipeline().get("JDoeoEodpEprL") == null) this.channel.pipeline().addAfter("decoder", "JDoeoEodpEprL", new MessageToMessageDecoder<Packet<?>>() {
					@Override
					protected void decode(final ChannelHandlerContext chc, final Packet<?> packet, final List<Object> list) {
						if (packet != null) {
							if (packet instanceof PacketPlayInUseEntity) { // PLAYER NPCs
								final PacketPlayInUseEntity usePacket = (PacketPlayInUseEntity) packet;
								try {
									if (idfield != null) {
										final int entid = (int) idfield.get(packet);
										npcs.handlePlayerNPC(player, entid, usePacket);
									}
								} catch (final Exception ex) {
									ex.printStackTrace();
								}
							}
						}
						list.add(packet);
					}
				});
			} catch (final Exception e) {
				e.printStackTrace();
				System.out.println("MuxSystem> Error at intercepting player '" + player.getName() + "'. Ignoring...");
			}
		}
		public void uninject() {
			channel.eventLoop().execute(() -> {
				try {
					if (channel.pipeline().get("JDoeoEodpEprL") != null) channel.pipeline().remove("JDoeoEodpEprL");
				} catch (final Exception e) {
					System.out.println("MuxSystem> Error at removing channel from player. Ignoring...");
				}
			});
		}
	}
}