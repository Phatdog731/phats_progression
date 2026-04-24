package com.phatdog.phatsprogression;

import java.util.regex.Pattern;

/**
 * Simple glob-to-regex matcher. Supports '*' (zero or more chars) and '?' (one char).
 * Everything else is matched literally.
 */
public final class GlobMatcher {

    private GlobMatcher() {}

    /** Returns true if the subject matches the glob pattern. */
    public static boolean matches(String pattern, String subject) {
        if (pattern == null || subject == null) return false;
        // Fast path: no wildcards means exact match
        if (pattern.indexOf('*') < 0 && pattern.indexOf('?') < 0) {
            return pattern.equals(subject);
        }
        Pattern regex = Pattern.compile(globToRegex(pattern));
        return regex.matcher(subject).matches();
    }

    private static String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder(glob.length() + 8);
        sb.append('^');
        for (int i = 0; i < glob.length(); i++) {
            char c = glob.charAt(i);
            switch (c) {
                case '*' -> sb.append(".*");
                case '?' -> sb.append('.');
                case '.', '(', ')', '[', ']', '{', '}', '+', '|', '^', '$', '\\' -> {
                    sb.append('\\').append(c);
                }
                default -> sb.append(c);
            }
        }
        sb.append('$');
        return sb.toString();
    }
}