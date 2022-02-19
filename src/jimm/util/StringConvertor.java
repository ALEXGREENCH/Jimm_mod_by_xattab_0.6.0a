
package jimm.util;

import java.io.*;
import java.util.Vector;
import jimm.*;
import jimm.comm.*;

public class StringConvertor
{
    public static String loadFromResource(String path) {
        String res = "";
        try {
            InputStream stream = "".getClass().getResourceAsStream(path);
            byte[] str = new byte[stream.available()];
            stream.read(str);
            res = Util.byteArrayToString(str, 0, str.length, Util.isDataUTF8(str, 0, str.length));
            stream.close();
        } catch (Exception e) {
        }
        return res;
    }

    // Removes all CR occurences
    public static String removeCr(String val) {
        if (val.indexOf('\r') < 0) {
            return val;
        }
        StringBuffer result = new StringBuffer();
        int size = val.length();
        for (int i = 0; i < size; i++) {
            char chr = val.charAt(i);
            if ((chr == 0) || (chr == '\r')) continue;
            result.append(chr);
        }
        return result.toString();
    }

    // Restores CRLF sequense from LF
    public static String restoreCrLf(String val) {
        StringBuffer result = new StringBuffer();
        int size = val.length();
        for (int i = 0; i < size; i++) {
            char chr = val.charAt(i);
            if (chr == '\r') continue;
            if (chr == '\n') {
                result.append("\r\n");
            } else {
                result.append(chr);
            }
        }
        return result.toString();
    }

    public static String toLowerCase(String s) {
        char[] chars = s.toCharArray();
        for(int i = s.length() - 1; i >= 0; i--) {
            chars[i] = toLowerCase(chars[i]);
        }
        String res = new String(chars);
        return res.equals(s) ? s : res;
    }
    
    public static String toUpperCase(String s) {
        char[] chars = s.toCharArray();
        for(int i = s.length() - 1; i >= 0; i--) {
            chars[i] = toUpperCase(chars[i]);
        }
        String res = new String(chars);
        return res.equals(s) ? s : res;
    }

    public static char toLowerCase(char c) {
        c = Character.toLowerCase(c);
        if (c >= 'A' && c <= 'Z' || c >= '\300'
                && c <= '\326' || c >= '\330'
                && c <= '\336' || c >= '\u0400'
                && c <= '\u042F') {
            if (c <= 'Z' || c >= '\u0410' && c <= '\u042F') {
                return (char)(c + 32);
            }
            if(c < '\u0410') {
                return (char)(c + 80);
            }
            return (char)(c + 32);
        }
        return c;
    }

    public static char toUpperCase(char c) {
        c = Character.toUpperCase(c);
        if (c >= 'a' && c <= 'z' || c >= '\337'
                && c <= '\366' || c >= '\370'
                && c <= '\377' || c >= '\u0430'
                && c <= '\u045F') {
            if (c <= 'z' || c >= '\u0430' && c <= '\u044F') {
                return (char)(c - 32);
            }
            if (c > '\u042F') {
                return (char)(c - 80);
            }
            return (char)(c - 32);
        }
        return c;
    }

    private String convertChar(String str) {
        String lowerStr = toLowerCase(str);
        for (int i = from.length - 1; i >= 0; i--) {
            if (from[i].equals(str)) {
                return to[i];
            }
            if (from[i].equals(lowerStr)) {
                return toUpperCase(to[i]);
            }
        }
        return null;
    }

    private String convertText(String str) {
        StringBuffer buf = new StringBuffer();
        int i = 0;
        while (i < str.length()) {
            String ch = "";
            int endPos = Math.min(i + maxWordLength, str.length());
            while (endPos > i) {
                ch = str.substring(i, endPos);
                String trans = convertChar(ch);
                if (trans != null) {
                    buf.append(trans);
                    break;
                } else if (ch.length() == 1) {
                    buf.append(ch);
                    break;
                }
                endPos--;
            }
            i += ch.length();
        }
        return buf.toString();
    }

    private final String name;
    private final String[] from;
    private final String[] to;
    private int maxWordLength;
    
    private String[] vectorToArray(Vector v) {
        String[] result = new String[v.size()];
        v.copyInto(result);
        return result;
    }
    private StringConvertor(String name, Vector from, Vector to) {
        this.name = name;
        this.from = vectorToArray(from);
        this.to   = vectorToArray(to);
        maxWordLength = 0;
        for (int i = 0; i < this.from.length; i++) {
            maxWordLength = Math.max(maxWordLength, this.from[i].length());
        }
    }
    
    private static final int PARSER_LINE    = 0;
    private static final int PARSER_NAME    = 1;
    private static final int PARSER_FROM    = 2;
    private static final int PARSER_TO      = 3;
    private static final int PARSER_COMMENT = 6;

    private static void convertorParser(String content, Vector convertors) {
        int state = PARSER_LINE;
        int beginPos = 0;
        String name = null;
        Vector from = new Vector();
        Vector to = new Vector();
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            switch (state) {
                case PARSER_LINE:
                    if (ch == '[') {
                        if (name != null) {
                            convertors.addElement(new StringConvertor(name, from, to));
                            from.removeAllElements();
                            to.removeAllElements();
                            name = null;
                        }
                        beginPos = i + 1;
                        state = PARSER_NAME;
                    } else if (ch == '#' || ch == ';') {
                        state = PARSER_COMMENT;
                    } else if ((ch == '\n') || (ch == '\r')
                            || (ch == ' ')|| (ch == '\t')) {
                    } else {
                        beginPos = i;
                        state = PARSER_FROM;
                    }
                    break;
                    
                case PARSER_NAME:
                    if (ch == ']') {
                        name = content.substring(beginPos, i).trim();
                        if (name.length() == 0) {
                            name = null;
                        }
                        beginPos = i + 1;
                        state = PARSER_LINE;
                    }
                    break;
                    
                case PARSER_FROM:
                    if (ch == '=') {
                        from.addElement(content.substring(beginPos, i).trim());
                        beginPos = i + 1;
                        state = PARSER_TO;
                    }
                    break;
                    
                case PARSER_TO:
                    if ((ch == '\n') || (ch == '#') || (ch == ';')) {
                        to.addElement(content.substring(beginPos, i).trim());
                        beginPos = i;
                        state = PARSER_LINE;
                    }
                    break;
                    
                case PARSER_COMMENT:
                    if (ch == '\n') {
                        state = PARSER_LINE;
                    }
                    break;
            }
        }
        if (name != null) {
            convertors.addElement(new StringConvertor(name, from, to));
        }
    }
    private static void load() {
        Vector converters = new Vector();
        try {
            String content = StringConvertor.loadFromResource("/replaces.txt");
            content = StringConvertor.removeCr(content.trim() + '\n');
            convertorParser(content, converters);
        } catch (Exception e) {
        }
        StringConvertor.converters = new StringConvertor[converters.size()];
        converters.copyInto(StringConvertor.converters);
    }

    private static StringConvertor[] converters;

    static {
        load();
    }

    private static String convert(String scheme, String str) {
        for (int i = 0; i < converters.length; i++) {
            if (scheme.equals(converters[i].name)) {
                return converters[i].convertText(str);
            }
        }
        return str;
    }
    public static String detransliterate(String str) {
        return convert("_detransliterate".substring(1), str);
    }
    
    public static String transliterate(String str) {
        return convert("_transliterate".substring(1), str);
    }
}