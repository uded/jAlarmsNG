package pl.org.radical.alarms.channels;

import pl.org.radical.alarms.AbstractAlarmChannel;

import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

public class NotifoChannel extends AbstractAlarmChannel {

	/**
	 * Contacts to which the alarm will be sent grouped by source.
	 */
	@Setter
	private Map<String, List<String>> contactsBySource;

	/**
	 * A username name which will be used by Notifo client to connect to API service
	 */
	@Setter
	public String serviceUser;

	/**
	 * API token to authorize the use of the service.
	 */
	@Setter
	public String apiToken;

	@Override
	protected Runnable createSendTask(final String msg, final String src) {
		return new NotifoTask(msg, src);
	}

	@Override
	protected boolean hasSource(final String alarmSource) {
		return contactsBySource == null || contactsBySource.containsKey(alarmSource);
	}

	/**
	 * This class is used by the MailChannel; it sends out an alarm with the JavaMailSender set in the
	 * MailChannel.
	 * 
	 * @author Enrique Zamudio
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
			HttpPost post = new HttpPost("https://api.notifo.com/v1/send_notification");

			for (Entry<String, List<String>> entries: contactsBySource.entrySet()) {
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				params.add(new BasicNameValuePair("to", ""));

			}
		}

	}
}
