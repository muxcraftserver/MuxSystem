package me.muxteam.muxsystem;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Settings {
		private int entrycost, heals = 4, ops = 8, activepage;
		private boolean bodyfix = true, verified = false, menu = true, music = true, blood = true, chatfilter = true, sortfriendstrophies = false;
		private final Set<UUID> friendalert = new HashSet<>();

		public Settings() {}
		public int getEntryCost() {
			return entrycost;
		}
		public boolean hasBodyFix() {
			return bodyfix;
		}
		public int getOPs() {
			return ops;
		}
		public int getHeals() { return heals; }
		public boolean needsVerified() { return verified; }
		public int getActivePage() {
			return activepage;
		}
		public void setEntryCost(final int entrycost) {
			this.entrycost = entrycost;
		}
		public void setBodyFix(final boolean bodyfix) {
			this.bodyfix = bodyfix;
		}
		public void setOPs(final int ops) {
			this.ops = ops;
		}
		public void setHeals(final int heals) { this.heals = heals; }
		public void setVerified(final boolean verified) { this.verified = verified; }
		public Set<UUID> getAlertFriends() {
			return friendalert;
		}
		public boolean hasMenu() {
			return menu;
		}
		public void setMenu(final boolean menu) {
			this.menu = menu;
		}
		public boolean hasMusic() {
			return music;
		}
		public void setMusic(final boolean music) {
			this.music = music;
		}
		public boolean hasBloodEffect() {
			return blood;
		}
		public void setBlood(final boolean blood) {
			this.blood = blood;
		}
		public void setActivePage(final int activepage) {
			this.activepage = activepage;
		}
		public void setShowFriendsByTrophies(boolean sortfriendstrophies) {
		this.sortfriendstrophies = sortfriendstrophies;
	}
		public boolean showFriendsByTrophies() {
		return sortfriendstrophies;
	}
		public boolean hasChatFilter() {
		return chatfilter;
	}
	    public void setChatFilter(boolean chatfilter) {
			this.chatfilter = chatfilter;
		}
}