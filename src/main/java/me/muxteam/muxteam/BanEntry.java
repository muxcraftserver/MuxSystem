package me.muxteam.muxteam;

import java.util.UUID;

public final class BanEntry {
	private final int id;
	private final UUID uuid;
	private final String name, action, extrainfo, reason, time, causer, removecauser, removetime, unformattedTime;

	public BanEntry(final int id, final UUID uuid, final String name, final String action, final String extrainfo, final String reason, final String time, final String causer,
					final String removecauser, String removetime, String unformattedTime) {
		this.id = id;
		this.uuid = uuid;
		this.name = name;
		this.action = action;
		this.extrainfo = extrainfo;
		this.reason = reason;
		this.time = time;
		this.causer = causer;
		this.removecauser = removecauser;
		this.removetime = removetime;
		this.unformattedTime = unformattedTime;
	}

	public int getID() {
		return id;
	}
	public UUID getUUID() {
		return uuid;
	}
	public String getName() {
		return name;
	}
	public String getAction() {
		return action;
	}
	public String getExtraInfo() {
		return extrainfo;
	}
	public String getTime() {
		return time;
	}
	public String getUnformattedTime() {
		return unformattedTime;
	}
	public String getReason() {
		return reason;
	}
	public String getCauser() {
		return causer;
	}
	public String getRemoveCauser() {
		return removecauser;
	}
	public String getRemoveTime() {
		return removetime;
	}
}
