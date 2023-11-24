package me.muxteam.marketing;

import io.netty.util.internal.ConcurrentSet;
import me.muxteam.basic.MuxActions;
import me.muxteam.basic.MuxChatInput;
import me.muxteam.basic.Stacktrace;
import me.muxteam.muxsystem.MuxSystem;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public class MuxEmails {
    private final Pattern EMAIL_PATTERN = Pattern.compile("^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@"
            + "[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$");

    private final Set<String> validDomains = new HashSet<>();
    private final MuxSystem ms;
    private Session session, supsession;
    private final Map<UUID, MuxMailUser> mailusers = new ConcurrentHashMap<>();
    private final String verifyHTMLEmail, verifyTXTEmail, supHTMLEmail, supTXTEmail;
    private final EmailThread thread = new EmailThread();
    private final Set<String> cooldown = new ConcurrentSet<>();

    public MuxEmails(final MuxSystem ms) {
        this.ms = ms;
        verifyHTMLEmail = this.loadEmail("email.html");
        verifyTXTEmail = this.loadEmail("email.txt");
        supHTMLEmail = this.loadEmail("emailsupport.html");
        supTXTEmail = this.loadEmail("emailsupport.txt");
        this.addDefaults();
        this.loadValidDomains();
        loadEmailClient();
    }

    public void loadEmailClient() {
        try {
            final Properties prop = new Properties();
            prop.put("mail.transport.protocol", "smtp");
            prop.put("mail.smtp.auth", this.ms.getConfig().getString("email.auth_enabled"));
            prop.put("mail.smtp.starttls.enable", this.ms.getConfig().getString("email.starttls_enabled"));
            prop.put("mail.smtp.host", this.ms.getConfig().getString("email.host"));
            prop.put("mail.smtp.port", this.ms.getConfig().getString("email.port"));
            prop.put("mail.smtp.ssl.trust", this.ms.getConfig().getString("email.ssl_trust"));
            this.session = Session.getInstance(prop, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(ms.getConfig().getString("email.info.username"), ms.getConfig().getString("email.info.password"));
                }
            });
            this.supsession = Session.getInstance(prop, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(ms.getConfig().getString("email.support.username"), ms.getConfig().getString("email.support.password"));
                }
            });
            session.getTransport().connect();
            supsession.getTransport().connect();
            this.ms.getAdmin().EMAILS.setActive(true);
            session.getTransport().close();
            supsession.getTransport().close();
        } catch (final Exception ex) {
            System.err.println("MuxSystem> Email Client could not be initiated.\n" + ex.getMessage());
            this.ms.getAdmin().EMAILS.setActive(false);
        }
    }

    public void handleJoin(final Player p) {
        new MuxMailUser(p.getUniqueId(), p.getName(), true, u -> this.mailusers.put(p.getUniqueId(), u));
    }

    public void checkMails() {
        final Set<MuxMailUser> toSave = new HashSet<>();
        for (final Player p : ms.getServer().getOnlinePlayers()) {
            final MuxMailUser u = getUser(p.getUniqueId());
            if (u.isVerified() && u.getLastReminder() < System.currentTimeMillis() - 2592000000L) {
                if (u.getLastReminder() == -1) {
                    u.setLastReminder(System.currentTimeMillis() - (2505600000L));
                } else {
                    u.setLastReminder(System.currentTimeMillis());
                    p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1F);
                    ms.chatClickHoverRun(p, "§f§lIst deine verifizierte E-Mail noch gültig?", "§f§oKlicke zum ändern", "/profil " + p.getName());
                    ms.chatClickHoverRun(p, "§f§lKlicke §a§lhier§f§l, um sie zu ändern.", "§f§oKlicke zum ändern", "/profil " + p.getName());
                }
                toSave.add(u);
            }
        }
        if (toSave.isEmpty() == false)
            new BukkitRunnable() {
                @Override
                public void run() {
                    toSave.forEach(u -> ms.getDB().saveMuxMailUser(u));
                }
            }.runTaskAsynchronously(ms);
    }

    public void handleQuit(final Player p) {
        this.mailusers.remove(p.getUniqueId()); // no need to save, we´re saving instant @ change
    }

    public MailVerifyResult handleVerifyChat(final Player p, final String chatMessage) {
        final MuxMailUser u = mailusers.get(p.getUniqueId());
        if (u == null) return MailVerifyResult.ERROR;
        if (u.getVerifyCode() == null) return u.isVerified() ? MailVerifyResult.ALREADY_VERIFIED : MailVerifyResult.ERROR_CANCEL;
        if (chatMessage.equals(u.getVerifyCode())) {
            if (ms.getDB().emailAlreadyUsed(u.getTempEmail())) {
                ms.showItemBar(p, "§cDie eingegebene Email wurde bereits verifiziert.");
                u.setTempEmail(null);
                u.setCurrentTries(0);
                u.setVerifyCode(null);
                u.setLastVerifyTime(-1);
                ms.getDB().saveMuxMailUser(u);
                return MailVerifyResult.ERROR_CANCEL;
            }
            final String oldEmail = u.getEmail();
            u.setVerified(true);
            u.setVerifyCode(null);
            u.setCurrentTries(0);
            ms.getDB().addEmailToList(u.getTempEmail(), p.getName());
            u.setEmail(u.getTempEmail());
            u.setTempEmail(null);
            u.setLastReminder(System.currentTimeMillis());
            u.setLastVerifyTime(-1);
            ms.getDB().saveMuxMailUser(u);
            p.playSound(p.getLocation(), Sound.LEVEL_UP, 1F, 1F);
            p.sendMessage("§a§lDein Account wurde erfolgreich verifiziert.§a ✔");
            new BukkitRunnable() {
                @Override
                public void run() {
                    ms.getChat().cancelChatInput(p, true);
                    if (oldEmail == null)
                        giveBenefits(p);
                }
            }.runTask(ms);
            return MailVerifyResult.SUCCESS;
        } else {
            ms.showItemBar(p, "§cDer Code ist falsch. Bitte überprüfe ihn noch einmal.");
        }
        return MailVerifyResult.ERROR;
    }

    private void giveBenefits(final Player p) {
        if (ms.getVotes().hasVoted(p.getUniqueId()) == false) {
            ms.getVotes().onVote(p.getName(), true);
            ms.getVotes().onVote(p.getName(), true);
        }
    }

    public boolean handleCommand(final Player p, String[] args) {
        if (ms.isTrusted(p.getName()) && args.length >= 2) {
            if (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {
                final boolean add = args[0].equalsIgnoreCase("add");
                final String domain = args[1].toLowerCase();
                if (domain.contains(".") == false) {
                    ms.showItemBar(p, "§cDie Email Domain ist nicht gültig.");
                    return true;
                } else if (add) {
                    if (validDomains.contains(domain)) {
                        ms.showItemBar(p, "§cDie Email Domain ist bereits vorhanden.");
                        return true;
                    }
                    validDomains.add(domain);
                    ms.getDB().addEmailDomain(domain);
                    ms.showItemBar(p, "§fEmail Domain §6" + domain + " §fwurde §ahinzugefügt§f.");
                    ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "EMAILDOMAIN ADD", args[1].toLowerCase());
                } else {
                    if (validDomains.contains(domain) == false) {
                        ms.showItemBar(p, "§cDie Email Domain ist nicht vorhanden.");
                        return true;
                    }
                    validDomains.remove(domain);
                    ms.getDB().removeEmailDomain(domain);
                    ms.showItemBar(p, "§fEmail Domain §6" + domain + " §fwurde §centfernt§f.");
                    ms.getHistory().addHistory(p.getUniqueId(), null, "TEAMACTION", "EMAILDOMAIN REMOVE", args[1].toLowerCase());
                }
            } else {
                ms.showItemBar(p, ms.usage("/email [add | remove] [domain]"));
            }
            return true;
        }
        if (ms.checkGeneralCooldown(p.getName(), "CMDEMAIL", 5000L, true)) {
            return true;
        }
        ms.getProfiles().showFullPlayerProfile(p, p.getName(), "Menü");
        return true;
    }

    public MuxMailUser getUser(final UUID uuid) {
        return mailusers.get(uuid);
    }

    public MailVerifyResult tryToVerifyEmail(final Player player, final String email, final MuxMailUser user) {
        if (this.cooldown.contains(player.getName())) {
            ms.showItemBar(player, "§cDu verifizierst dich bereits, bitte versuche es gleich erneut.");
            return MailVerifyResult.ERROR_CANCEL;
        }
        if (user.isLoading()) return MailVerifyResult.ERROR;
        if (user.isVerified() && user.isProfileNewEmail() == false) {
            ms.showItemBar(player, "§aDu hast deine Email bereits verifiziert.");
            return MailVerifyResult.ALREADY_VERIFIED;
        }
        user.setProfileNewEmail(false);
        if (user.getCurrentTries() == 3) {
            if (user.getLastVerifyTime() > System.currentTimeMillis() - 7200000L) {
                ms.showItemBar(player, "§cVersuche es in §6" + ms.timeToString((user.getLastVerifyTime() + 7200000L) - System.currentTimeMillis(), false) + " §cerneut.");
                return MailVerifyResult.ERROR_CANCEL;
            }

            user.setCurrentTries(0);
        } else if (ms.checkGeneralCooldown(player.getName(), "EMAILVERIFY", 300000L, false)) {
            ms.showItemBar(player, "§cBitte warte §6" + "5 Minuten" + " §cbevor du es erneut versuchst.");
            return MailVerifyResult.ERROR_CANCEL;
        }
        if (isValidEmail(email)) {
            if (ms.getDB().emailAlreadyUsed(email)) {
                ms.showItemBar(player, "§cDie eingegebene Email wird bereits verwendet.");
                return MailVerifyResult.ERROR;
            }
            final String code = ms.getRandomWord().toUpperCase();
            this.cooldown.add(player.getName());
            this.thread.addAction(ms, new EmailAction(player.getName(), () -> {
                try {
                    sendEmail(session, verifyHTMLEmail, verifyTXTEmail, ms.getConfig().getString("email.info.username"), email, "MuxCraft - Verifizierungscode für " + player.getName(), code, player.getName());
                } catch (final MessagingException | UnsupportedEncodingException e) {
                    player.sendMessage("§6§lEmail>§c Es ist ein Fehler aufgetreten, bitte melde dich bei einem Admin.");
                    Stacktrace.print(new RuntimeException(e));
                }
            }));
            if (user.getCurrentTries() == 0) {
                user.setLastVerifyTime(System.currentTimeMillis());
            }
            ms.checkGeneralCooldown(player.getName(), "EMAILVERIFY", 30_000L, true);
            user.setVerifyCode(code);
            user.setTempEmail(email);
            if (user.getEmail() == null) user.setVerified(false);
            user.setCurrentTries(user.getCurrentTries() + 1);
            player.sendMessage(" ");
            player.sendMessage("§a§lFast verifiziert... §7§o" + email);
            player.playSound(player.getLocation(), Sound.ENDERDRAGON_WINGS, 1F, 1F);
            final AtomicReference<MuxChatInput> reference = new AtomicReference<>();
            new BukkitRunnable() {
                @Override
                public void run() {
                    final MuxChatInput chatInput = new MuxChatInput(ms, (input, p1) -> {
                        final MailVerifyResult r = handleVerifyChat(p1, input);
                        if (r != MailVerifyResult.ERROR) {
                            reference.get().cancel();
                        }
                    });
                    chatInput.show(player, "§f§lGebe den Code ein, der dir per Mail gesendet wurde:", true, "§7Klicke, um den Prozess abzubrechen.", 5);
                    reference.set(chatInput);
                }
            }.runTaskLater(ms, 2L);
            player.sendMessage("§7(Wenn du die Mail nicht findest, schaue im Spamordner nach)");
            ms.getDB().saveMuxMailUser(user);
            return MailVerifyResult.SUCCESS;
        } else {
            ms.showItemBar(player, "§cDie eingegebene Email ist ungültig.");
        }
        return MailVerifyResult.ERROR;
    }

    private void addDefaults() {
        final List<String> defaultdomains = Arrays.asList("aol.com", "att.net", "comcast.net", "facebook.com", "gmail.com", "gmx.com", "googlemail.com", "google.com", "hotmail.com", "hotmail.co.uk", "mac.com", "me.com", "mail.com", "msn.com",
                "live.com", "sbcglobal.net", "verizon.net", "yahoo.com", "yahoo.co.uk", "email.com", "fastmail.fm", "games.com", "gmx.net", "gmx.eu", "hush.com", "hushmail.com", "icloud.com", "iname.com", "inbox.com",
                "lavabit.com", "love.com", "outlook.com", "pobox.com", "mailbox.org", "disroot.org", "startmail.com", "skiff.com", "protonmail.ch", "protonmail.com", "proton.me", "pm.me", "tutanota.de", "tutanota.com", "tutamail.com", "tuta.io", "keemail.me", "rocketmail.com",
                "safe-mail.net", "wow.com", "ygm.com", "ymail.com", "zoho.com", "yandex.com", "bellsouth.net", "charter.net", "cox.net", "earthlink.net", "juno.com", "btinternet.com", "virginmedia.com", "blueyonder.co.uk",
                "freeserve.co.uk", "live.co.uk", "live.nl", "live.at", "live.ca", "ntlworld.com", "o2.co.uk", "orange.net", "sky.com", "talktalk.co.uk", "tiscali.co.uk", "virgin.net", "wanadoo.co.uk", "bt.com", "sina.com", "sina.cn", "qq.com",
                "naver.com", "hanmail.net", "daum.net", "nate.com", "yahoo.co.jp", "yahoo.co.kr", "yahoo.co.id", "yahoo.co.in", "yahoo.com.sg", "yahoo.com.ph", "163.com", "yeah.net", "126.com", "21cn.com",
                "aliyun.com", "foxmail.com", "hotmail.fr", "live.fr", "laposte.net", "yahoo.fr", "wanadoo.fr", "orange.fr", "gmx.fr", "sfr.fr", "neuf.fr", "free.fr", "gmx.de", "hotmail.de", "live.de", "online.de",
                "t-online.de", "arcor.de", "web.de", "versanet.de", "ok.de", "netcologne.de", "vodafone.de", "hispeed.ch", "onlinehome.de", "unitybox.de", "outlook.de", "yahoo.de", "email.de", "mail.de", "posteo.de", "posteo.net", "posteo.org", "posteo.at", "posteo.ch", "freenet.de", "gmx.at", "mail.ch", "bluewin.ch", "gmx.ch",
                "yahoo.at", "aon.at", "chello.at", "kabelbw.de", "greenmail.ch", "a1.net", "aol.de", "libero.it", "virgilio.it", "hotmail.it", "aol.it", "tiscali.it", "alice-dsl.net", "alice.de", "alice.it", "live.it", "yahoo.it", "email.it", "tin.it", "poste.it", "teletu.it", "mail.ru",
                "rambler.ru", "yandex.ru", "ya.ru", "list.ru", "pt.lu", "sunrise.ch", "hotmail.be", "live.be", "skynet.be", "voo.be", "tvcablenet.be", "telenet.be", "hotmail.ch", "hotmail.com.ar", "live.com.ar", "yahoo.com.ar", "fibertel.com.ar",
                "speedy.com.ar", "arnet.com.ar", "yahoo.com.mx", "live.com.mx", "hotmail.es", "hotmail.com.mx", "prodigy.net.mx", "yahoo.ca", "hotmail.ca", "bell.net", "shaw.ca", "sympatico.ca", "rogers.com",
                "yahoo.com.br", "hotmail.com.br", "outlook.com.br", "uol.com.br", "bol.com.br", "terra.com.br", "ig.com.br", "itelefonica.com.br", "r7.com", "zipmail.com.br", "globo.com", "globomail.com", "oi.com.br", "live.ca");
        ms.getDB().addDefaultDomains(defaultdomains);
    }

    private String loadEmail(final String file) {
        final StringBuilder builder = new StringBuilder();
        final Scanner scanner = new Scanner(ms.getResource(file), StandardCharsets.UTF_8.name());
        while (scanner.hasNextLine()) {
            builder.append(scanner.nextLine());
        }
        return builder.toString();
    }

    private void loadValidDomains() {
        validDomains.addAll(ms.getDB().getValidEmailDomains());
    }

    private boolean isValidEmail(final String string) {
        return EMAIL_PATTERN.matcher(string).matches() && this.endsWithValidDomain(string.toLowerCase());
    }

    private boolean endsWithValidDomain(final String string) {
        for (final String validDomain : this.validDomains) {
            if (string.endsWith(validDomain)) return true;
        }
        return false;
    }

    public void sendSupportEmail(final Player player, final String pname, final String to, final String msg) {
        this.thread.addAction(ms, new EmailAction(player.getName(), () -> {
            try {
                sendEmail(supsession, supHTMLEmail, supTXTEmail, ms.getConfig().getString("email.support.username"), to, "[MuxSupport] Neue Antwort", msg, pname);
            } catch (final MessagingException | UnsupportedEncodingException e) {
                player.sendMessage("§6§lEmail>§c Es ist ein Fehler aufgetreten, bitte melde dich bei einem Admin.");
                throw new RuntimeException(e);
            }
        }));
    }

    private void sendEmail(final Session session, final String HTMLEmail, final String TXTEmail, final String from, final String to,
                           final String subject, final String msg, final String playerName) throws MessagingException, UnsupportedEncodingException {
        System.out.println("MuxSystem> Sending Email to " + to);
        final ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        final Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from, "MuxCraft"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setSentDate(new Date());

        final MimeBodyPart plainTextBodyPart = new MimeBodyPart();
        plainTextBodyPart.setContent(TXTEmail.replace("$msg", msg).replace("$playername", playerName), "text/plain; charset=utf-8");
        final MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(HTMLEmail.replace("$msg", msg).replace("$playername", playerName), "text/html; charset=utf-8");
        final Multipart multipart = new MimeMultipart("alternative");
        multipart.addBodyPart(plainTextBodyPart);
        multipart.addBodyPart(mimeBodyPart);
        message.setContent(multipart);
        Transport.send(message);

        Thread.currentThread().setContextClassLoader(oldClassLoader);
    }

    public boolean isEnabled() {
        return this.ms.getAdmin().EMAILS.isActive();
    }

    public enum MailVerifyResult {
        ALREADY_VERIFIED,
        ERROR_CANCEL,
        ERROR,
        SUCCESS
    }

    public class MuxMailUser {
        private final UUID uuid;
        private final String name;
        private boolean newsletter, verified;
        private String email, tempEmail, verifyCode;
        private int currentTries = 0;
        private long lastVerifyTime, lastVerifyReminder;
        private final Consumer<MuxMailUser> loadCallback;
        private final transient boolean alwaysCallCallback;
        private transient boolean isLoading, profileNewEmail;

        public MuxMailUser(final UUID uuid, final String name, final boolean alwaysCallCallback, final Consumer<MuxMailUser> loadCallback) {
            this.uuid = uuid;
            this.name = name;
            this.alwaysCallCallback = alwaysCallCallback;
            this.loadCallback = loadCallback;
            load();
        }

        private void load() {
            this.isLoading = true;
            ms.getForkJoinPool().execute(() -> {
                if ((ms.getDB().loadMuxMailUser(this) || this.alwaysCallCallback) && this.loadCallback != null) {
                    this.isLoading = false;
                    loadCallback.accept(this);
                }
            });
        }

        protected boolean isLoading() {
            return isLoading;
        }

        public UUID getUUID() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public void setProfileNewEmail(final boolean profileNewEmail) {
            this.profileNewEmail = profileNewEmail;
        }

        public boolean isProfileNewEmail() {
            return profileNewEmail;
        }

        public boolean isNewsletter() {
            return newsletter;
        }

        public void setNewsletter(final boolean newsletter) {
            this.newsletter = newsletter;
        }

        public boolean isVerified() {
            return verified;
        }

        public String getVerifyCode() {
            return verifyCode;
        }

        public void setVerifyCode(final String verifyCode) {
            this.verifyCode = verifyCode;
        }

        public void setTempEmail(final String tempEmail) {
            this.tempEmail = tempEmail;
        }

        public String getTempEmail() {
            return tempEmail;
        }

        public long getLastVerifyTime() {
            return lastVerifyTime;
        }

        public void setLastVerifyTime(final long lastVerifyTime) {
            this.lastVerifyTime = lastVerifyTime;
        }

        public void setVerified(final boolean verified) {
            this.verified = verified;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(final String email) {
            this.email = email;
        }

        public void setCurrentTries(final int currentTries) {
            this.currentTries = currentTries;
        }

        public int getCurrentTries() {
            return currentTries;
        }

        public long getLastReminder() {
            return lastVerifyReminder;
        }

        public void setLastReminder(long lastVerifyReminder) {
            this.lastVerifyReminder = lastVerifyReminder;
        }
    }

    private class EmailThread extends Thread {
        private final Queue<EmailAction> actions = new LinkedBlockingQueue<>();
        private final AtomicLong cooldown = new AtomicLong(0);
        private final AtomicBoolean isCooldown = new AtomicBoolean(false);

        public EmailThread() {
            super.setDaemon(true);
            super.start();
        }

        @Override
        public void run() {
            while (super.isInterrupted() == false) {
                blockThread();
                while (actions.isEmpty() == false) {
                    final EmailAction action = actions.poll();
                    if (action != null) {
                        action.action.call();
                        MuxEmails.this.cooldown.remove(action.name);
                    }
                }
            }
        }

        public synchronized void addAction(final MuxSystem ms, final EmailAction action) {
            this.actions.add(action);
            if (isCooldown.get()) return;
            if (cooldown.get() > System.currentTimeMillis() - 5000L) {
                isCooldown.set(true);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        isCooldown.set(false);
                        MuxEmails.EmailThread.super.notify();
                    }
                }.runTaskLaterAsynchronously(ms, 20L * 5L);
                return;
            }
            cooldown.set(System.currentTimeMillis());
            super.notify();
        }

        private synchronized void blockThread() {
            try {
                while (this.actions.isEmpty() || (isCooldown.get())) {
                    super.wait();
                }
            } catch (final Exception ignored) {}
        }
    }

    class EmailAction {
        private final String name;
        private final MuxActions.Action action;

        private EmailAction(final String name, final MuxActions.Action action) {
            this.name = name;
            this.action = action;
        }
    }
}