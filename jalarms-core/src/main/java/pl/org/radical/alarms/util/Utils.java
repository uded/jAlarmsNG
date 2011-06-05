package pl.org.radical.alarms.util;

/**
 * A class containing some utility functions.
 * 
 * @author Enrique Zamudio
 */
public class Utils {

	/** Replaces all occurrences of var with value, inside the specified string. */
	@Deprecated
	public static String replaceAll(final String var, final String value, final String string) {
		if (string.indexOf(var) >= 0) {
			final StringBuilder buf = new StringBuilder(string);
			int pos = buf.indexOf(var);
			while (pos >= 0) {
				buf.replace(pos, pos + value.length(), value);
				pos = buf.indexOf(var, pos + value.length());
			}
			return buf.toString();
		} else {
			return string;
		}
	}

}
