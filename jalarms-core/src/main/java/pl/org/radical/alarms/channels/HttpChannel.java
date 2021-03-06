package pl.org.radical.alarms.channels;

import pl.org.radical.alarms.AbstractAlarmChannel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * This channel can send an alarm over an HTTP GET or POST request. It does not wait for a response unless
 * specified to do so.
 * 
 * @author Enrique Zamudio
 */
public class HttpChannel extends AbstractAlarmChannel {

	private String expResp;
	private String url;
	private URL cacheUrl;
	private String postData;
	private Set<String> sources;

	/**
	 * If this property is set, the channel waits for a response and scans it for this particular string,
	 * warning if not found.
	 */
	public void setExpectedResponse(final String value) {
		expResp = value;
	}

	/**
	 * You can set a list of sources in this property so that only alarm messages matching any of the sources
	 * are be posted as status updates. If you don't set a value here, all alarm messages are posted, with the
	 * usual rules about min time interval. This is useful if you want to use a Twitter account to only post
	 * alarm messages from certain sources.
	 */
	public void setAlarmSource(final Set<String> value) {
		sources = value;
	}

	/**
	 * Sets the URL to send the alarm to. It can contain "${alarm}" and "${source}", in which case they will
	 * be replaced by the alarm message and the specified source, when an actual alarm is sent.
	 */
	@Resource
	public void setUrl(final String value) throws MalformedURLException {
		url = value;
	}

	/**
	 * Sets the POST data for the HTTP request. If set, this means the HTTP POST method will be used; by default,
	 * HTTP GET is used. The POST string can contain "${alarm}" and "${source}", which will be replaced with the
	 * alarm message and specified source, when an actual alarm is sent.
	 */
	public void setPostData(final String value) {
		postData = value;
	}

	@Override
	protected Runnable createSendTask(final String msg, final String source) {
		if (sources != null && !sources.contains(source)) {
			return null;
		}
		return new HttpTask(msg, source);
	}

	@Override
	protected boolean hasSource(final String alarmSource) {
		return sources != null && sources.contains(alarmSource);
	}

	/**
	 * Initializes the cached URL if the URL value has no alarm or source variables, and validates that there is postData if
	 * the URL is going to be cached.
	 * 
	 * @throws MalformedURLException
	 *             if the url is fixed (no ${alarm} or ${source}) and it cannot be converted to a valid URL.
	 */
	@PostConstruct
	public void init() throws MalformedURLException {
		if (url.indexOf("${alarm}") < 0 && url.indexOf("${source}") < 0) {
			if (postData == null) {
				throw new IllegalStateException("POST data is needed if URL has no variables");
			}
			cacheUrl = new URL(url);
		}
	}

	/**
	 * This task makes the HTTP connection to the specified URL, replacing the alarm and source variables where necessary.
	 * 
	 * @author Enrique Zamudio
	 */
	private class HttpTask implements Runnable {

		private final String alarm;
		private final String source;

		private HttpTask(final String mensaje, final String fuente) {
			alarm = mensaje;
			source = fuente;
		}

		@Override
		public void run() {
			URL myurl = cacheUrl;
			String mypost = postData;
			if (mypost != null) {
				// Replace vars in postData
				try {
					mypost = mypost.replace("${alarm}", URLEncoder.encode(alarm, "UTF-8"));
					mypost = mypost.replace("${source}", URLEncoder.encode(source, "UTF-8"));
				} catch (final UnsupportedEncodingException ex) {
					log.error("Cannot encode alarm or source in POST data", ex);
					return;
				}
			}
			if (myurl == null) {
				try {
					// Replace vars in url
					String _url = url.replace("${alarm}", URLEncoder.encode(alarm, "UTF-8"));
					_url = _url.replace("${source}", URLEncoder.encode(source, "UTF-8"));
					myurl = new URL(_url);
				} catch (final MalformedURLException ex) {
					log.error("Resulting URL is invalid", ex);
				} catch (final UnsupportedEncodingException ex) {
					log.error("Cannot encode alarm or source in URL", ex);
				}
			}
			if (myurl != null) {
				try {
					InputStream ins = null;
					if (mypost == null) {
						// Just connect and get the stream for the response
						ins = myurl.openStream();
					} else {
						// Connect, send the POST
						final URLConnection conn = myurl.openConnection();
						conn.setDoOutput(true);
						if (conn instanceof HttpURLConnection) {
							((HttpURLConnection) conn).setRequestMethod("POST");
							conn.addRequestProperty("Content-Length", Integer.toString(mypost.length()));
						}
						final OutputStream outs = conn.getOutputStream();
						outs.write(mypost.getBytes());
						outs.flush();
						ins = conn.getInputStream();
					}
					// Now get the response, if needed
					final StringBuilder sb = new StringBuilder(4096);
					if (expResp != null) {
						final byte[] buf = new byte[1024];
						int leidos = ins.read(buf);
						while (leidos > 0) {
							final String _r = new String(buf, 0, leidos);
							sb.append(_r);
							if (sb.length() >= 4096) {
								leidos = -1;
							} else {
								leidos = ins.read(buf);
							}
						}
						if (sb.indexOf(expResp) < 0) {
							log.warn("Did not get expected response sending alarm over URL {}", myurl);
						}
					}
					ins.close();
				} catch (final IOException ex) {
					log.error("Sending alarm over URL", ex);
				}
			}
		}

	}

}
