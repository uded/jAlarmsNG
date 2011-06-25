package pl.org.radical.alarms.channels;

import pl.org.radical.alarms.AbstractAlarmChannel;

import lombok.Setter;

import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.notifo.client.NotifoClient;
import com.notifo.client.NotifoException;
import com.notifo.client.NotifoHttpClient;
import com.notifo.client.NotifoMessage;

public class NotifoChannel extends AbstractAlarmChannel {
	private final Logger log = LoggerFactory.getLogger(this.getClass());

	/**
	 * Contacts to which the alarm will be sent.
	 */
	private List<String> contacts;

	/**
	 * Contacts to which the alarm will be sent grouped by source.
	 */
	@Setter
	private Map<String, List<String>> contactsBySource;

	/**
	 * A username name which will be used by Notifo client to connect to API service
	 */
	@Setter
	private String serviceUser;

	/**
	 * API token to authorize the use of the service.
	 */
	@Setter
	private String apiToken;

	@Setter
	private String subject; // FIXME Dodać default

	private NotifoClient client;

	@PostConstruct
	public void init() {
		client = new NotifoHttpClient(serviceUser, apiToken);
	}

	@Override
	protected Runnable createSendTask(final String msg, final String src) {
		return new NotifoTask(msg, src);
	}

	@Override
	protected boolean hasSource(final String alarmSource) {
		return contactsBySource == null || contactsBySource.containsKey(alarmSource);
	}

	/**
	 * This class is used by the NotifoChannel; it sends out an alarm with the API
	 * 
	 * @author <a href="mailto:lukasz.rzanek@radical.com.pl">Łukasz Rżanek</a>
	 * @author © 2011 Radical Creations
	 */
	private class NotifoTask implements Runnable {
		private final String msg;
		private final String src;

		private NotifoTask(final String msg, final String src) {
			this.msg = msg;
			this.src = src;
		}

		@Override
		public void run() {
			List<String> destinations;
			if (src != null && contactsBySource.containsKey(src)) {
				destinations = contactsBySource.get(src);
			} else {
				destinations = contacts;
			}

			NotifoMessage nmsg;
			for (String dest : destinations) {
				nmsg = new NotifoMessage(dest, msg);
				nmsg.setLabel("jAlarms");
				nmsg.setSubject(subject);
				try {
					client.sendMessage(nmsg);
				} catch (NotifoException e) {
					log.error("Sending Notifo alarm to '{}'", dest);
				}
			}
		}
	}
}
