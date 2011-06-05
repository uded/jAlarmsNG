package pl.org.radical.alarms.channels;

import pl.org.radical.alarms.AbstractAlarmChannel;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import de.huxhorn.lilith.slf4j.Logger;
import de.huxhorn.lilith.slf4j.Logger.Level;
import de.huxhorn.lilith.slf4j.LoggerFactory;

public class Sfl4jChannel extends AbstractAlarmChannel {

	private String src;

	@Setter
	private Level level = Level.WARN;

	/**
	 * Sets a value that will be matched against the sources received in {@link #createSendTask(String, String)};
	 * by default it's null so that all alarms are printed out, but you can set a value here to print only
	 * the messages from the specified source.
	 * 
	 * @param src
	 *            source name
	 */
	public void setAlarmSource(final String src) {
		this.src = src;
	}

	@Override
	protected Runnable createSendTask(final String msg, final String source) {
		return new SimpleTask(msg, source);
	}

	@Override
	protected boolean hasSource(final String alarmSource) {
		return src != null && src.equals(alarmSource);
	}

	@RequiredArgsConstructor
	@AllArgsConstructor
	private class SimpleTask implements Runnable {
		@NonNull
		private final String msg;

		private final String src;

		private Logger log;

		@Override
		public void run() {
			if (src == null) {
				log = LoggerFactory.getLogger("ALARM");
				log.log(level, msg);
			} else {
				log = LoggerFactory.getLogger("ALARM." + src);
				log.log(level, msg);
			}
		}
	}
}
