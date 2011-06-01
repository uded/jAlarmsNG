package pl.org.radical.alarms.channels;

import pl.org.radical.alarms.AbstractAlarmChannel;

import java.util.Date;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * An AlarmChannel that sends its alarms via email, using Spring's JavaMail support.
 * A SimpleMailMessage must be used as a template for the messages that will be sent;
 * the to, from and subject properties will be preserved, and only the text will be
 * modified to include the time at which the alarm is sent. The text can contain a
 * ${msg} variable which will be replaced by the alarm message. If text does not contain
 * said variable, the whole text will be replaced by the alarm message (with the time
 * still included).
 * You can define different mail templates depending on the alarm source, by using the templatesBySource
 * property; simply set a map where the keys are the alarm sources and the values are SimpleMailMessage instances.
 * Each SimpleMailMessage can have a different text and different recipients (even different senders, if your SMTP
 * account allows it). This way, different mails can be sent depending on the alarm source.
 * 
 * @author Enrique Zamudio
 */
public class MailChannel extends AbstractAlarmChannel {

	private JavaMailSender mailer;
	private SimpleMailMessage example;
	private Map<String, SimpleMailMessage> sourceTemplates;

	/**
	 * Specifies a set of templates to be used, one for each different source. This is optional,
	 * as the default template can be used for undefined or null sources.
	 */
	public void setTemplatesBySource(final Map<String, SimpleMailMessage> value) {
		sourceTemplates = value;
	}

	/**
	 * Specifies a SimpleMailMessage to be used as a template for messages to be sent.
	 * If the template's body contains the string "${msg}" then it is replaced by the
	 * alarm message; otherwise, the mail body is replaced by the alarm message, along with
	 * the time the message was sent.
	 */
	@Resource
	public void setMailTemplate(final SimpleMailMessage value) {
		example = value;
	}

	/**
	 * Specified the JavaMailSender to use for sending the alarm mails. Must be properly configured
	 * so that emails are actually sent.
	 */
	@Resource
	public void setJavaMailSender(final JavaMailSender value) {
		mailer = value;
	}

	@Override
	protected Runnable createSendTask(final String msg, final String src) {
		return new MailTask(msg, src);
	}

	@Override
	protected boolean hasSource(final String alarmSource) {
		return sourceTemplates == null || sourceTemplates.containsKey(alarmSource);
	}

	/**
	 * This class is used by the MailChannel; it sends out an alarm with the JavaMailSender set in the
	 * MailChannel.
	 * 
	 * @author Enrique Zamudio
	 */
	private class MailTask implements Runnable {
		private final String msg;
		private final String src;

		private MailTask(final String message, final String src) {
			msg = message;
			this.src = src;
		}

		@Override
		public void run() {
			SimpleMailMessage original = null;
			if (src != null && sourceTemplates != null) {
				original = sourceTemplates.get(src);
			}
			if (original == null) {
				original = example;
			}
			final SimpleMailMessage mail = new SimpleMailMessage(original);
			final int where = mail.getText().indexOf("${msg}");
			if (where >= 0) {
				mail.setText(String.format("(%TT) %s%s%s", new Date(), mail.getText().substring(0, where), msg,
				        mail.getText().substring(where + 6)));
			} else {
				mail.setText(String.format("(%TT) %s", new Date(), msg));
			}
			try {
				mailer.send(mail);
			} catch (final MailException ex) {
				log.error("MailChannel cannot send alarm '{}'", msg, ex);
			}
		}

	}

}
