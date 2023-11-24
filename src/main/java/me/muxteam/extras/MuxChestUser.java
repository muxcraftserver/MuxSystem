package me.muxteam.extras;

import me.muxteam.extras.MuxChests.ChestType;

import java.util.List;
import java.util.UUID;

public final class MuxChestUser {
	private final UUID uuid;
	private final List<ChestType> chests;
	private boolean changed, daily;

	public MuxChestUser(final UUID uuid, final List<ChestType> chests, final boolean daily) {
		this.uuid = uuid;
		this.chests = chests;
		this.daily = daily;
	}

	public UUID getUUID() {
		return uuid;
	}

	public boolean hasChanged() {
		return changed;
	}

	public void saved() {
		this.changed = false;
	}

	public List<ChestType> getChests() {
		return chests;
	}

	public void addChest(final ChestType type) {
		addChest(type, false);
	}

	public void addChest(final ChestType type, final boolean bypass) {
		if (chests.size() >= 10 && bypass == false) return;
		chests.add(type);
		this.changed = true;
	}

	public void removeChest(final int pos) {
		chests.remove(pos);
		this.changed = true;
	}

	public boolean getDaily() {
		return daily;
	}

	public void setDaily(final boolean daily) {
		this.daily = daily;
	}
}