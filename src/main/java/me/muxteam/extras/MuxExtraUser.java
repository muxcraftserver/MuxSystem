package me.muxteam.extras;

import me.muxteam.extras.MuxMounts.MountStore;
import me.muxteam.extras.MuxPerks.PerkStore;
import me.muxteam.extras.MuxPets.PetStore;

import java.util.Set;
import java.util.UUID;

public final class MuxExtraUser {
	private final UUID uuid;
	private String name;
	private PerkStore perks;
	private MountStore mounts;
	private PetStore pets;
	private Set<String> extracmds;
	private Set<Short> emojis;
	private byte enderchest;
	private boolean changed = false;

	public MuxExtraUser(final UUID uuid, final String name, final PerkStore perks,  final MountStore mounts, final PetStore pets,
						final Set<String> extracmds, final Set<Short> emojis, final byte enderchest) {
		this.uuid = uuid;
		this.name = name;
		this.perks = perks;
		this.mounts = mounts;
		this.pets = pets;
		this.extracmds = extracmds;
		this.emojis = emojis;
		this.enderchest = enderchest;
	}

	public UUID getUUID() {
		return uuid;
	}
	public String getName() {
		return name;
	}
	public void setName(final String name) {
		if (this.name.equals(name) == false) {
			this.name = name;
			this.changed = true;
		}
	}
	public boolean hasChanged() {
		return changed;
	}
	public void saved() {
		this.changed = false;
	}
	public PerkStore getPerks() {
		return perks;
	}
	public void setPerks(final PerkStore perks) {
		this.perks = perks;
		this.changed = true;
	}
	public Set<String> getExtraCMDs() {
		return extracmds;
	}
	public void setExtraCMDs(final Set<String> extracmds) {
		this.extracmds = extracmds;
		this.changed = true;
	}
	public PetStore getPets() {
		return pets;
	}
	public void setPets(final PetStore pets) {
		this.pets = pets;
		this.changed = true;
	}

	public MountStore getMounts() {
		return mounts;
	}
	public void setMounts(final MountStore mounts) {
		this.mounts = mounts;
		this.changed = true;
	}
	public Set<Short> getEmojis() {
		return emojis;
	}
	public void setEmojis(final Set<Short> emojis) {
		this.emojis = emojis;
		this.changed = true;
	}
	public byte getEnderChest() {
		return enderchest;
	}
	public void setEnderChest(final byte enderchest) {
		this.enderchest = enderchest;
		this.changed = true;
	}
	public void setChanged(boolean changed) {
		this.changed = changed;
	}
}