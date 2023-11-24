package me.muxteam.shared;

import de.vantrex.simplenetty.annotations.PacketId;
import de.vantrex.simplenetty.packet.SimplePacket;
import io.netty.buffer.ByteBuf;
import me.muxteam.extras.MuxChests;
import org.bukkit.entity.EntityType;

import java.util.UUID;

public class MuxSharedPackets {

    @PacketId(id = 1)
    public static class PacketResetVotes extends SimplePacket {
        
        @Override
        public void write(final ByteBuf byteBuf) {
        }

        @Override
        public void read(final ByteBuf byteBuf) {
        }
    }

    @PacketId(id = 2)
    public static class PacketUpdateCachedName extends SimplePacket {

        private UUID player;
        private String newName;

        public PacketUpdateCachedName() {
        }

        public PacketUpdateCachedName(final UUID player, final String newName) {
            this.player = player;
            this.newName = newName;
        }

        @Override
        public void write(final ByteBuf byteBuf) {
            writeUUID(byteBuf, player);
            writeString(byteBuf, newName);
        }

        @Override
        public void read(final ByteBuf byteBuf) {
            this.player = readUUID(byteBuf);
            this.newName = readString(byteBuf);
        }

        public String getNewName() {
            return newName;
        }

        public UUID getPlayer() {
            return player;
        }
    }

    @PacketId(id = 3)
    public static class PacketUpdateExtraPet extends SimplePacket {
        private UUID uuid;
        private boolean add;
        private EntityType pet;
        private UUID executor;

        public PacketUpdateExtraPet() {
            this.executor = null;
        }

        public PacketUpdateExtraPet(final UUID uuid, final boolean add, final EntityType pet, final UUID executor) {
            this.uuid = uuid;
            this.add = add;
            this.pet = pet;
            this.executor = executor;
        }

        @Override
        public void write(final ByteBuf byteBuf) {
            writeUUID(byteBuf, uuid);
            byteBuf.writeBoolean(add);
            writeString(byteBuf, pet.name());
            byteBuf.writeBoolean(executor != null);
            if (executor != null)
                writeUUID(byteBuf, executor);
        }

        @Override
        public void read(final ByteBuf byteBuf) {
            this.uuid = readUUID(byteBuf);
            this.add = byteBuf.readBoolean();
            this.pet = EntityType.valueOf(readString(byteBuf));
            if (byteBuf.readBoolean())
                this.executor = readUUID(byteBuf);
        }

        public EntityType getPet() {
            return pet;
        }

        public UUID getUUID() {
            return uuid;
        }

        public boolean isAdd() {
            return add;
        }

        public UUID getExecutor() {
            return executor;
        }
    }

    @PacketId(id = 4)
    public static class PacketUpdateExtraMount extends SimplePacket {
        private UUID uuid;
        private boolean add;
        private short mount;
        private final UUID executor;

        public PacketUpdateExtraMount() {
            this.executor = null;
        }

        public PacketUpdateExtraMount(final UUID uuid, boolean add, short mount, UUID executor) {
            this.uuid = uuid;
            this.add = add;
            this.mount = mount;
            this.executor = executor;
        }

        @Override
        public void write(final ByteBuf byteBuf) {
            writeUUID(byteBuf, uuid);
            byteBuf.writeBoolean(add);
            byteBuf.writeShort(mount);
            byteBuf.writeBoolean(executor != null);
            if (executor != null)
                writeUUID(byteBuf, executor);
        }

        @Override
        public void read(final ByteBuf byteBuf) {
            this.uuid = readUUID(byteBuf);
            this.add = byteBuf.readBoolean();
            this.mount = byteBuf.readShort();
            if (byteBuf.readBoolean())
                this.uuid = readUUID(byteBuf);
        }

        public short getMount() {
            return mount;
        }

        public UUID getUUID() {
            return uuid;
        }

        public boolean isAdd() {
            return add;
        }


        public UUID getExecutor() {
            return executor;
        }
    }

    @PacketId(id = 5)
    public static class PacketUpdateExtraPerk extends SimplePacket {
        private UUID uuid;
        private boolean add;
        private byte perk;
        private UUID executor;

        public PacketUpdateExtraPerk() {
            this.executor = null;
        }

        public PacketUpdateExtraPerk(final UUID uuid, boolean add, byte perk, UUID executor) {
            this.uuid = uuid;
            this.add = add;
            this.perk = perk;
            this.executor = executor;
        }

        @Override
        public void write(final ByteBuf byteBuf) {
            writeUUID(byteBuf, uuid);
            byteBuf.writeBoolean(add);
            byteBuf.writeByte(perk);
            byteBuf.writeBoolean(executor != null);
            if (executor != null)
                writeUUID(byteBuf, executor);
        }

        @Override
        public void read(final ByteBuf byteBuf) {
            this.uuid = readUUID(byteBuf);
            this.add = byteBuf.readBoolean();
            this.perk = byteBuf.readByte();
            if (byteBuf.readBoolean())
                this.executor = readUUID(byteBuf);
        }

        public byte getPerk() {
            return perk;
        }

        public UUID getUUID() {
            return uuid;
        }

        public boolean isAdd() {
            return add;
        }

        public UUID getExecutor() {
            return executor;
        }
    }

    @PacketId(id = 6)
    public static class PacketUpdateExtraCommand extends SimplePacket {
        private UUID uuid;
        private boolean add;
        private String command;
        private UUID executor;

        public PacketUpdateExtraCommand() {
            this.executor = null;
        }

        public PacketUpdateExtraCommand(final UUID uuid, final boolean add, final String command, final UUID executor) {
            this.uuid = uuid;
            this.add = add;
            this.command = command;
            this.executor = executor;
        }

        @Override
        public void write(final ByteBuf byteBuf) {
            writeUUID(byteBuf, uuid);
            byteBuf.writeBoolean(add);
            writeString(byteBuf, command);
            byteBuf.writeBoolean(executor != null);
            if (executor != null)
                writeUUID(byteBuf, executor);
        }

        @Override
        public void read(final ByteBuf byteBuf) {
            this.uuid = readUUID(byteBuf);
            this.add = byteBuf.readBoolean();
            this.command = readString(byteBuf);
            if (byteBuf.readBoolean())
                this.executor = readUUID(byteBuf);
        }

        public String getCommand() {
            return command;
        }

        public UUID getUUID() {
            return uuid;
        }

        public boolean isAdd() {
            return add;
        }

        public UUID getExecutor() {
            return executor;
        }
    }

    @PacketId(id = 7)
    public static class PacketUpdateExtraEmoji extends SimplePacket {
        private UUID uuid;
        private UUID executor;
        private boolean add;
        private short emoji;

        public PacketUpdateExtraEmoji() {
            this.executor = null;
        }

        public PacketUpdateExtraEmoji(final UUID uuid, final boolean add, final short emoji, final UUID executor) {
            this.uuid = uuid;
            this.add = add;
            this.emoji = emoji;
            this.executor = executor;
        }

        @Override
        public void write(final ByteBuf byteBuf) {
            writeUUID(byteBuf, uuid);
            byteBuf.writeBoolean(add);
            byteBuf.writeShort(emoji);
            byteBuf.writeBoolean(executor != null);
            if (executor != null)
                writeUUID(byteBuf, executor);
        }

        @Override
        public void read(final ByteBuf byteBuf) {
            this.uuid = readUUID(byteBuf);
            this.add = byteBuf.readBoolean();
            this.emoji = byteBuf.readShort();
            if (byteBuf.readBoolean())
                this.executor = readUUID(byteBuf);
        }

        public short getEmoji() {
            return emoji;
        }

        public UUID getUUID() {
            return uuid;
        }

        public boolean isAdd() {
            return add;
        }

        public UUID getExecutor() {
            return executor;
        }
    }

    @PacketId(id = 8)
    public static class PacketGiveVote extends SimplePacket {
        private UUID uuid;

        public PacketGiveVote() {
        }

        public PacketGiveVote(final UUID uuid) {
            this.uuid = uuid;
        }

        @Override
        public void write(final ByteBuf byteBuf) {
            writeUUID(byteBuf, uuid);
        }

        @Override
        public void read(final ByteBuf byteBuf) {
            this.uuid = readUUID(byteBuf);
        }

        public UUID getUuid() {
            return uuid;
        }
    }

    @PacketId(id = 9)
    public static class PacketUpdateRank extends SimplePacket {

        private UUID uuid;
        private String updatedRank;

        public PacketUpdateRank() {
        }

        public PacketUpdateRank(final UUID uuid, final String updatedRank) {
            this.uuid = uuid;
            this.updatedRank = updatedRank;
        }

        @Override
        public void write(final ByteBuf byteBuf) {
            writeUUID(byteBuf, uuid);
            writeString(byteBuf, updatedRank == null ? "RANK_IS_NULL" : updatedRank);
        }

        @Override
        public void read(final ByteBuf byteBuf) {
            this.uuid = readUUID(byteBuf);
            this.updatedRank = readString(byteBuf);
            if (this.updatedRank.equals("RANK_IS_NULL"))
                this.updatedRank = null;
        }

        public UUID getUUID() {
            return uuid;
        }

        public String getUpdatedRank() {
            return updatedRank;
        }
    }


    @PacketId(id = 10)
    public static class PacketAddChest extends SimplePacket {

        private UUID uuid;
        private MuxChests.ChestType chestType;
        private boolean bypass;
        private UUID executor;

        public PacketAddChest() {
        }

        public PacketAddChest(final UUID uuid, final MuxChests.ChestType chestType, final boolean bypass, final UUID executor) {
            this.uuid = uuid;
            this.chestType = chestType;
            this.bypass = bypass;
            this.executor = executor;
        }

        @Override
        public void write(final ByteBuf byteBuf) {
            writeUUID(byteBuf, uuid);
            writeString(byteBuf, chestType.name());
            byteBuf.writeBoolean(bypass);
            byteBuf.writeBoolean(executor != null);
            if (executor != null)
                writeUUID(byteBuf, executor);
        }

        @Override
        public void read(final ByteBuf byteBuf) {
            this.uuid = readUUID(byteBuf);
            this.chestType = MuxChests.ChestType.valueOf(readString(byteBuf));
            this.bypass = byteBuf.readBoolean();
            if (byteBuf.readBoolean())
                this.executor = readUUID(byteBuf);
        }

        public UUID getUUID() {
            return uuid;
        }

        public MuxChests.ChestType getChestType() {
            return chestType;
        }

        public boolean isBypass() {
            return bypass;
        }

        public UUID getExecutor() {
            return executor;
        }
    }

    @PacketId(id = 11)
    public static class PacketRemoveChest extends SimplePacket {
        private UUID uuid;
        private int slot;
        private UUID executor;
        private String chestName;

        public PacketRemoveChest() {
        }

        public PacketRemoveChest(final UUID uuid, final int slot, final UUID executor, final String chestName) {
            this.uuid = uuid;
            this.slot = slot;
            this.executor = executor;
            this.chestName = chestName;
        }

        @Override
        public void write(final ByteBuf byteBuf) {
            writeUUID(byteBuf, uuid);
            byteBuf.writeInt(slot);
            writeString(byteBuf, chestName);
            byteBuf.writeBoolean(executor != null);
            if (executor != null)
                writeUUID(byteBuf, executor);
        }

        @Override
        public void read(final ByteBuf byteBuf) {
            this.uuid = readUUID(byteBuf);
            this.slot = byteBuf.readInt();
            this.chestName = readString(byteBuf);
            if (byteBuf.readBoolean())
                this.executor = readUUID(byteBuf);
        }

        public UUID getUUID() {
            return uuid;
        }

        public int getSlot() {
            return slot;
        }

        public UUID getExecutor() {
            return executor;
        }

        public String getChestName() {
            return chestName;
        }
    }

    @PacketId(id = 12)
    public static class PacketPlayerJoinServer extends SimplePacket {

        private UUID player;
        private UUID server;

        public PacketPlayerJoinServer(final UUID player, final UUID server) {
            this.player = player;
            this.server = server;
        }

        public PacketPlayerJoinServer() {
        }

        @Override
        public void write(final ByteBuf byteBuf) {
            writeUUID(byteBuf, player);
            writeUUID(byteBuf, server);
        }

        @Override
        public void read(final ByteBuf byteBuf) {
            this.player = readUUID(byteBuf);
            this.server = readUUID(byteBuf);
        }

        public UUID getPlayer() {
            return player;
        }

        public UUID getServer() {
            return server;
        }
    }

    @PacketId(id = 13)
    public static class PacketPlayerQuitServer extends SimplePacket {

        private UUID player;

        public PacketPlayerQuitServer(final UUID player) {
            this.player = player;
        }

        public PacketPlayerQuitServer() {
        }

        @Override
        public void write(final ByteBuf byteBuf) {
            writeUUID(byteBuf, player);
        }

        @Override
        public void read(final ByteBuf byteBuf) {
            this.player = readUUID(byteBuf);
        }

        public UUID getPlayer() {
            return player;
        }
    }
    @PacketId(id = 14)
    public static class PacketUpdateExtraEnderChest extends SimplePacket {

        private UUID uuid;
        private boolean add;
        private UUID executor;


        public PacketUpdateExtraEnderChest() {
        }

        public PacketUpdateExtraEnderChest(final UUID uuid, final boolean add, final UUID executor) {
            this.uuid = uuid;
            this.add = add;
            this.executor = executor;
        }

        @Override
        public void write(final ByteBuf byteBuf) {
            writeUUID(byteBuf, uuid);
            byteBuf.writeBoolean(add);
            byteBuf.writeBoolean(executor != null);
            if (executor != null)
                writeUUID(byteBuf, executor);
        }

        @Override
        public void read(final ByteBuf byteBuf) {
            this.uuid = readUUID(byteBuf);
            this.add = byteBuf.readBoolean();
            if (byteBuf.readBoolean())
                this.executor = readUUID(byteBuf);
        }

        public UUID getUUID() {
            return uuid;
        }

        public boolean isAdd() {
            return add;
        }

        public UUID getExecutor() {
            return executor;
        }
    }

    @PacketId(id = 15)
    public static class PacketPlayerVanish extends SimplePacket {

        private String player;
        private boolean remove;

        public PacketPlayerVanish() {}

        public PacketPlayerVanish(final String player, final boolean remove) {
            this.player = player;
            this.remove = remove;
        }

        @Override
        public void write(final ByteBuf byteBuf) {
            writeString(byteBuf, player);
            byteBuf.writeBoolean(remove);
        }

        @Override
        public void read(final ByteBuf byteBuf) {
            this.player = readString(byteBuf);
            this.remove = byteBuf.readBoolean();
        }

        public String getPlayer() {
            return player;
        }

        public boolean isRemove() {
            return remove;
        }
    }

    @PacketId(id = 16)
    public static class PacketReloadTagData extends SimplePacket {
        private UUID player;

        public PacketReloadTagData(final UUID player) {
            this.player = player;
        }

        public PacketReloadTagData() {
        }

        @Override
        public void write(final ByteBuf byteBuf) {
            writeUUID(byteBuf, player);
        }

        @Override
        public void read(final ByteBuf byteBuf) {
            player = readUUID(byteBuf);
        }

        public UUID getPlayer() {
            return player;
        }
    }

    @PacketId(id = 17)
    public static class PacketWhitelistDeactivate extends SimplePacket {
        public PacketWhitelistDeactivate() {
        }

        @Override
        public void write(final ByteBuf byteBuf) {

        }

        @Override
        public void read(final ByteBuf byteBuf) {

        }
    }
}