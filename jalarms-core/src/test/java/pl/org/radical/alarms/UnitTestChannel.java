package pl.org.radical.alarms;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import de.huxhorn.lilith.slf4j.Logger;import de.huxhorn.lilith.slf4j.LoggerFactory;

/**
 * This class is used in unit tests to receive a notification when an alarm is actually sent.
 * 
 * @author Enrique Zamudio
 */
public class UnitTestChannel implements AlarmChannel {

	private static volatile int instanceCount;
	private final Logger log = LoggerFactory.getLogger(String.format("Test-%d", ++instanceCount));
	long stamp;
	long lastSent;
	int resend = 5000;
	ChanDelegate delegate;
	AtomicBoolean sent = new AtomicBoolean();

	@Override
	public void send(final String msg, final String source) {
		final long now = System.currentTimeMillis();
		if (source == null) {
			log.info("Supposed to send alarm '{}', time diff is {}", msg, now - stamp);
		} else {
			log.info("Supposed to send alarm {}:'{}', time diff is {}", new Object[] { source, msg, now - stamp });
		}
		delegate.alarmReceived(msg, now);
		lastSent = now;
		sent.set(true);
	}

	void prepare() {
		stamp = System.currentTimeMillis();
		sent.set(false);
	}

	void waitForSend() {
		try {
			int count = 0;
			while (!sent.get()) {
				count++;
				Thread.sleep(1000);
				if (!sent.get()) {
					if (count % 10 == 0) {
						log.info("Still waiting... {}", String.format("%TT", new Date()));
					}
					if (count > 300) {
						log.info("5 minutes? I think this test has failed");
						assert false;
					}
				}
			}
		} catch (final InterruptedException ex) {
			// nothing
		}
	}

	@Override
	public int getMinResendInterval() {
		return resend;
	}

	@Override
	public void shutdown() {
	}

	static interface ChanDelegate {
		public void alarmReceived(String msg, long when);
	}

}
