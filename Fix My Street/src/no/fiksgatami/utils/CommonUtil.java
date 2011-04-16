package no.fiksgatami.utils;

/**
 * @author Roy Sindre Norangshol <roy.sindre 'at' norangshol.no>
 */
public class CommonUtil {
    public static boolean isStringNullOrEmpty(String string) {
        return !(string!=null && !string.trim().equalsIgnoreCase(""));
    }
}
