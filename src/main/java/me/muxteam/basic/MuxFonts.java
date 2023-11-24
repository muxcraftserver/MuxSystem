package me.muxteam.basic;

import me.muxteam.muxsystem.MuxSystem;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public final class MuxFonts {
    private final MuxSystem ms;

    public MuxFonts(final MuxSystem ms) {
        this.ms = ms;
    }

    public String centerBook(final String message) {
        return centerText(message, 54);
    }

    public String centerText(final String message) {
        return centerText(message, 154);
    }
    private String centerText(String message, final int maxlength) {
        if (message == null || message.equals("")) return "";
        message = ChatColor.translateAlternateColorCodes('&', message);

        int messagePxSize = 0;
        boolean previousCode = false, isBold = false;

        for (final char c : message.toCharArray()) {
            if (c == '§') {
                previousCode = true;
            } else if (previousCode == true) {
                previousCode = false;
                isBold = c == 'l' || c == 'L';
            } else {
                final DefaultFontInfo dFI = getDefaultFontInfo(c);
                messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }
        final int halvedMessageSize = messagePxSize / 2, spaceLength = DefaultFontInfo.SPACE.getLength() + 1;
        int compensated = 0, toCompensate = maxlength - halvedMessageSize;
        final StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }
        return sb + message;
    }
    public void showParticleText(final Location loc, final String text, final Player pl) {
        final String a = "20111210001210001211111210001",
                b = "211112100012111121000121111",
                c = "201111212121201111",
                d = "2111121000121000121000121111",
                e = "211111212111121211111",
                f = "211111212111112121",
                g = "2011112121011121000120111",
                h = "210001210001211111210001210001",
                i = "211111200120012001211111",
                j = "211111200001200001200001210001201110",
                k = "2100012101121121011210001",
                l = "21212121211111",
                m = "211011210101210101210101210001",
                n = "210001211001210101210011210001",
                o = "2011121000121000121000120111",
                p = "21111210001211112121",
                q = "2011210012100121001201101",
                r = "211112100012111121001210001",
                s = "201111212011120000121111",
                t = "2111112001200120012001",
                u = "21000121000121000121000120111",
                v = "210001210001210001201012001",
                w = "21000121000121010121010120101",
                x = "2101210120121012101",
                y = "21000120101200120012001",
                z = "211111200012001201211111",
                A = "20111210001210001211111210001",
                B = "211112100012111121000121111",
                C = "201111212121201111",
                D = "2111121000121000121000121111",
                E = "211111212111121211111",
                F = "211111212111112121",
                G = "2011112121011121000120111",
                H = "210001210001211111210001210001",
                I = "211111200120012001211111",
                J = "211111200001200001200001210001201110",
                K = "2100012101121121011210001",
                L = "21212121211111",
                M = "211011210101210101210101210001",
                N = "210001211001210101210011210001",
                O = "2011121000121000121000120111",
                P = "21111210001211112121",
                Q = "2011210012100121001201101",
                R = "211112100012111121001210001",
                S = "201111212011120000121111",
                T = "2111112001200120012001",
                U = "21000121000121000121000120111",
                V = "210001210001210001201012001",
                W = "21000121000121010121010120101",
                X = "2101210120121012101",
                Y = "21000120101200120012001",
                Z = "211111200012001201211111",
                dot = "200200200200201",
                question = "0111002100012000120012200100",
                exclamation = "2012012012201",
                komma = "2022220121",
                zero = "2011121001121010121100120111",
                one = "20012011210120012001",
                two = "201112100012000112011211111",
                three = "201112100012001121000120111",
                four = "21210012111112000120001",
                five = "211111212111120000121111",
                six = "20111212111121000120111",
                seven = "21111120001200120012001",
                eight = "201112100012011121000120111",
                nine = "2011121000120111120000120111",
                scharfs = "201110210001210112100012101121",
                underscore = "2222211111",
                slash = "20000120001200120121",
                ht = "201012111112010121111120101";

        double altx = loc.getX();

        for (int ii = 0; ii < text.length(); ii++) {
            final char ch = text.charAt(ii);
            String code = "";

            switch (ch) {
                case '_':
                    code = underscore;
                    break;
                case '#':
                    code = ht;
                    break;
                case 'ß':
                    code = scharfs;
                    break;
                case '/':
                    code = slash;
                    break;
                case '?':
                    code = question;
                    break;
                case '!':
                    code = exclamation;
                    break;
                case ',':
                    code = komma;
                    break;
                case '.':
                    code = dot;
                    break;
                case 'a':
                    code = a;
                    break;
                case 'b':
                    code = b;
                    break;
                case 'c':
                    code = c;
                    break;
                case 'd':
                    code = d;
                    break;
                case 'e':
                    code = e;
                    break;
                case 'f':
                    code = f;
                    break;
                case 'g':
                    code = g;
                    break;
                case 'h':
                    code = h;
                    break;
                case 'i':
                    code = i;
                    break;
                case 'j':
                    code = j;
                    break;
                case 'k':
                    code = k;
                    break;
                case 'l':
                    code = l;
                    break;
                case 'm':
                    code = m;
                    break;
                case 'n':
                    code = n;
                    break;
                case 'o':
                    code = o;
                    break;
                case 'p':
                    code = p;
                    break;
                case 'q':
                    code = q;
                    break;
                case 'r':
                    code = r;
                    break;
                case 's':
                    code = s;
                    break;
                case 't':
                    code = t;
                    break;
                case 'u':
                    code = u;
                    break;
                case 'v':
                    code = v;
                    break;
                case 'w':
                    code = w;
                    break;
                case 'x':
                    code = x;
                    break;
                case 'y':
                    code = y;
                    break;
                case 'z':
                    code = z;
                    break;
                case 'A':
                    code = A;
                    break;
                case 'B':
                    code = B;
                    break;
                case 'C':
                    code = C;
                    break;
                case 'D':
                    code = D;
                    break;
                case 'E':
                    code = E;
                    break;
                case 'F':
                    code = F;
                    break;
                case 'G':
                    code = G;
                    break;
                case 'H':
                    code = H;
                    break;
                case 'I':
                    code = I;
                    break;
                case 'J':
                    code = J;
                    break;
                case 'K':
                    code = K;
                    break;
                case 'L':
                    code = L;
                    break;
                case 'M':
                    code = M;
                    break;
                case 'N':
                    code = N;
                    break;
                case 'O':
                    code = O;
                    break;
                case 'P':
                    code = P;
                    break;
                case 'Q':
                    code = Q;
                    break;
                case 'R':
                    code = R;
                    break;
                case 'S':
                    code = S;
                    break;
                case 'T':
                    code = T;
                    break;
                case 'U':
                    code = U;
                    break;
                case 'V':
                    code = V;
                    break;
                case 'W':
                    code = W;
                    break;
                case 'X':
                    code = X;
                    break;
                case 'Y':
                    code = Y;
                    break;
                case 'Z':
                    code = Z;
                    break;
                case '0':
                    code = zero;
                    break;
                case '1':
                    code = one;
                    break;
                case '2':
                    code = two;
                    break;
                case '3':
                    code = three;
                    break;
                case '4':
                    code = four;
                    break;
                case '5':
                    code = five;
                    break;
                case '6':
                    code = six;
                    break;
                case '7':
                    code = seven;
                    break;
                case '8':
                    code = eight;
                    break;
                case '9':
                    code = nine;
                    break;
                default:
                    break;
            }
            double curx = 0, cury = 0;
            for (int iii = 0; iii < code.length(); iii++) {
                char ch1 = code.charAt(iii);
                if (ch1 == '1') {
                    if (pl != null)
                        ms.playEffect(pl, EnumParticle.REDSTONE, new Location(loc.getWorld(), altx + curx, loc.getY() - cury, loc.getZ()), 0, 0, 0, 0, 10);
                    else
                        ms.playEffect(EnumParticle.REDSTONE, new Location(loc.getWorld(), altx + curx, loc.getY() - cury, loc.getZ()), 0, 0, 0, 0, 10);
                    curx += 0.2;
                }
                if (ch1 == '0') {
                    curx += 0.2;
                }
                if (ch1 == '2') {
                    cury += 0.2;
                    curx = 0;
                }
            }
            altx += 0.2 * 6;
        }
    }
    private DefaultFontInfo getDefaultFontInfo(char c) {
        for (final DefaultFontInfo dFI : DefaultFontInfo.values()) {
            if (dFI.getCharacter() == c) return dFI;
        }
        return DefaultFontInfo.DEFAULT;
    }

    public enum DefaultFontInfo {
        A('A', 5),
        a('a', 5),
        B('B', 5),
        b('b', 5),
        C('C', 5),
        c('c', 5),
        D('D', 5),
        d('d', 5),
        E('E', 5),
        e('e', 5),
        F('F', 5),
        f('f', 4),
        G('G', 5),
        g('g', 5),
        H('H', 5),
        h('h', 5),
        I('I', 3),
        i('i', 1),
        J('J', 5),
        j('j', 5),
        K('K', 5),
        k('k', 4),
        L('L', 5),
        l('l', 1),
        M('M', 5),
        m('m', 5),
        N('N', 5),
        n('n', 5),
        O('O', 5),
        o('o', 5),
        P('P', 5),
        p('p', 5),
        Q('Q', 5),
        q('q', 5),
        R('R', 5),
        r('r', 5),
        S('S', 5),
        s('s', 5),
        T('T', 5),
        t('t', 4),
        U('U', 5),
        u('u', 5),
        V('V', 5),
        v('v', 5),
        W('W', 5),
        w('w', 5),
        X('X', 5),
        x('x', 5),
        Y('Y', 5),
        y('y', 5),
        Z('Z', 5),
        z('z', 5),
        NUM_1('1', 5),
        NUM_2('2', 5),
        NUM_3('3', 5),
        NUM_4('4', 5),
        NUM_5('5', 5),
        NUM_6('6', 5),
        NUM_7('7', 5),
        NUM_8('8', 5),
        NUM_9('9', 5),
        NUM_0('0', 5),
        EXCLAMATION_POINT('!', 1),
        AT_SYMBOL('@', 6),
        NUM_SIGN('#', 5),
        DOLLAR_SIGN('$', 5),
        PERCENT('%', 5),
        UP_ARROW('^', 5),
        AMPERSAND('&', 5),
        ASTERISK('*', 5),
        LEFT_PARENTHESIS('(', 4),
        RIGHT_PERENTHESIS(')', 4),
        MINUS('-', 5),
        UNDERSCORE('_', 5),
        PLUS_SIGN('+', 5),
        EQUALS_SIGN('=', 5),
        LEFT_CURL_BRACE('{', 4),
        RIGHT_CURL_BRACE('}', 4),
        LEFT_BRACKET('[', 3),
        RIGHT_BRACKET(']', 3),
        COLON(':', 1),
        SEMI_COLON(';', 1),
        DOUBLE_QUOTE('"', 3),
        SINGLE_QUOTE('\'', 1),
        LEFT_ARROW('<', 4),
        RIGHT_ARROW('>', 4),
        QUESTION_MARK('?', 5),
        SLASH('/', 5),
        BACK_SLASH('\\', 5),
        LINE('|', 1),
        TILDE('~', 5),
        TICK('`', 2),
        PERIOD('.', 1),
        COMMA(',', 1),
        SPACE(' ', 3),
        DEFAULT('a', 4);

        private final char character;
        private final int length;

        DefaultFontInfo(final char character, final int length) {
            this.character = character;
            this.length = length;
        }

        public char getCharacter() {
            return character;
        }
        public int getLength() {
            return length;
        }
        public int getBoldLength() {
            if (this == DefaultFontInfo.SPACE) return this.getLength();
            return length + 1;
        }
    }
}