package me.muxteam.events;

import java.util.List;
import java.util.UUID;

public class SearchEventUser {
	private final UUID uuid;
	private short count, coinscount;
	private final List<Short> found, coinsfound;

	public SearchEventUser(final UUID uuid, final List<Short> found, final List<Short> coinsfound) {
		this.uuid = uuid;
		this.found = found;
		this.coinsfound = coinsfound;
		this.count = (short) found.size();
		this.coinscount = (short) coinsfound.size();
	}
	public UUID getUUID() {
		return uuid;
	}
	public short getScore() {
		return count;
	}
	public List<Short> getFound() {
		return found;
	}
	public void addFound(final short id) {
		found.add(id);
		count++;
	}
	public List<Short> getCoinsFound() {
		return coinsfound;
	}
	public void addCoinFound(final short id) {
		coinsfound.add(id);
		coinscount++;
	}
	public short getCoinScore() {
		return coinscount;
	}
}