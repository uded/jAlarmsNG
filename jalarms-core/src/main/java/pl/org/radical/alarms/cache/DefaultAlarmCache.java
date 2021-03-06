package pl.org.radical.alarms.cache;

import pl.org.radical.alarms.AlarmChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.digest.DigestUtils;

/**
 * This is the default cache, which uses a map to store the last date a message was sent for a given
 * channel/msg/source cobination.
 * 
 * @author Enrique Zamudio
 */
public class DefaultAlarmCache implements AlarmCache {

	private final Map<String, Long> lastSends = new ConcurrentHashMap<String, Long>();
	private int defint = 120000;

	/**
	 * Sets the default resend interval, for storing alarms unrelated to a specific channel,
	 * in milliseconds. Default is 2 minutes.
	 */
	public void setDefaultInterval(final int value) {
		defint = value;
	}

	public int getDefaultInterval() {
		return defint;
	}

	@Override
	public void store(final AlarmChannel channel, final String source, final String message) {
		if (channel == null || channel.getMinResendInterval() > 0) {
			final String k = channel == null ? String.format("ALL:%s:%s", source == null ? "" : source, DigestUtils.md5Hex(message))
			        : String.format("chan%s:%s:%s", channel.hashCode(), source == null ? "" : source, DigestUtils.md5Hex(message));
			lastSends.put(k, System.currentTimeMillis());
		}
	}

	@Override
	public boolean shouldResend(final AlarmChannel channel, final String source, final String message) {
		boolean resend = true;
		if (channel == null || channel.getMinResendInterval() > 0) {
			final String k = channel == null ? String.format("ALL:%s:%s", source == null ? "" : source, DigestUtils.md5Hex(message))
					: String.format("chan%s:%s:%s", channel.hashCode(), source == null ? "" : source, DigestUtils.md5Hex(message));
			final Long then = lastSends.get(k);
			// Check the last time this same message was sent
			if (then != null) {
				// If it's too recent, don't send it through this channel
				resend = System.currentTimeMillis() - then >= (channel == null ? defint : channel.getMinResendInterval());
			}
		}
		return resend;
	}

	@Override
	public String toString() {
		return String.format("Default(%d keys)", lastSends.size());
	}

	@Override
	public void shutdown() {
	}

}
