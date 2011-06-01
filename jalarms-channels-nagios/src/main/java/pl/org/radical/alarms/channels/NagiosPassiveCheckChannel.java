package pl.org.radical.alarms.channels;

import pl.org.radical.alarms.AbstractAlarmChannel;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Map;

import com.googlecode.jsendnsca.core.Level;
import com.googlecode.jsendnsca.core.MessagePayload;
import com.googlecode.jsendnsca.core.NagiosException;
import com.googlecode.jsendnsca.core.NagiosPassiveCheckSender;
import com.googlecode.jsendnsca.core.NagiosSettings;
import com.googlecode.jsendnsca.core.builders.MessagePayloadBuilder;

/**
 * A channel to send passive check data to Nagios.
 * 
 * @author Robin Bramley
 */
public class NagiosPassiveCheckChannel extends AbstractAlarmChannel {

	private String hostname;

	private NagiosSettings settings;

	private Map<String, String> sources;

	public void setSettings(final NagiosSettings settings) {
		this.settings = settings;
	}

	/**
	 * You must set source-servicecheck mappings for sources that should be sent to Nagios (as it needs a
	 * corresponding servicecheck).
	 */
	public void setSources(final Map<String, String> value) {
		sources = value;
	}

	/**
	 * Set a hostname.
	 */
	public void setHostname(final String hostname) {
		this.hostname = hostname;
	}

	@Override
	protected Runnable createSendTask(final String msg, final String source) {
		if (!hasSource(source)) {
			return null;
		}
		return new NscaTask(settings, hostname, msg, sources.get(source));
	}

	@Override
	protected boolean hasSource(final String alarmSource) {
		return sources != null && sources.containsKey(alarmSource);
	}

	/**
	 * Runnable task to invoke NSCA transmission.
	 */
	private class NscaTask implements Runnable {
		private final NagiosSettings settings;
		private final String msg;
		private final String src;
		private final String host;

		private NscaTask(final NagiosSettings settings, final String hostname, final String message, final String src) {
			this.settings = settings;
			host = hostname;
			msg = message;
			this.src = src;
		}

		@Override
		public void run() {
			try {
				final MessagePayload payload = new MessagePayloadBuilder()
				// alternatively use .withLocalHostname() or withCanonicalHostname
				        .withHostname(host).withLevel(Level.CRITICAL).withServiceName(src).withMessage(msg).create();

				final NagiosPassiveCheckSender sender = new NagiosPassiveCheckSender(settings);

				log.debug("Sending: " + payload.toString());
				sender.send(payload);
			} catch (final UnknownHostException uhe) {
				log.error("Sending alarm to Nagios", uhe);
			} catch (final NagiosException ne) {
				log.error("Sending alarm to Nagios", ne);
			} catch (final IOException ioe) {
				log.error("Sending alarm to Nagios", ioe);
			}
		}
	}
}
