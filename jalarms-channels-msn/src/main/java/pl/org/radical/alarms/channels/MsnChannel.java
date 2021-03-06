package pl.org.radical.alarms.channels;

import pl.org.radical.alarms.AbstractAlarmChannel;
import net.sf.jml.Email;
import net.sf.jml.MsnContact;
import net.sf.jml.MsnMessenger;
import net.sf.jml.MsnUserStatus;
import net.sf.jml.impl.MsnMessengerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * An AlarmChannel that sends its messages through a MSN account. To use this channel,
 * first you need to create a new MSN account and add the contacts that will get the alarms.
 * You can later add contacts through this same component.
 * You can define a list of contacts for each alarm source, using the contactsBySource property;
 * simply set a map where the keys are the alarm sources and the values are lists of users. These
 * users must already be on the account's contact list; the point of this is to be able to send
 * different alarms to different groups of users (subsets of the complete contact list), depending
 * on the alarm source.
 * 
 * @author Enrique Zamudio
 */
public class MsnChannel extends AbstractAlarmChannel {

	private MsnMessenger msn;
	private String user;
	private String pass;
	private Map<String, List<String>> sourceContacts;

	/** Sets the username (email address) for the account that will be used to send alarms. */
	@Resource
	public void setUsername(final String value) {
		user = value;
	}

	@Resource
	public void setPassword(final String value) {
		pass = value;
	}

	/**
	 * Sets a list of contacts for each alarm source. When an alarm is sent with a defined source,
	 * it is sent to the contacts defined for that source. If the source is not defined here or if it's null,
	 * then the alarm is sent to ALL contacts in the contact list.
	 * 
	 * @param value
	 *            A map that has alarm sources as keys and lists of users as values. The users
	 *            must be email addresses that can be found in the MSN user's contact list.
	 */
	public void setContactsBySource(final Map<String, List<String>> value) {
		sourceContacts = value;
	}

	/** Performs the login procedure to the MSN server. */
	@PostConstruct
	public void init() {
		try {
			msn = MsnMessengerFactory.createMsnMessenger(user, pass);
			msn.getOwner().setInitStatus(MsnUserStatus.BUSY);
			msn.setLogIncoming(false);
			msn.setLogOutgoing(false);
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					msn.logout();
				}
			});
			msn.login();
		} catch (final IllegalArgumentException ex) {
			log.error(String.format("Cannot login to MSN with account %s. MSN alarms will not be sent.", user), ex);
			msn = null;
		}
	}

	@Override
	protected Runnable createSendTask(final String msg, final String src) {
		return new MsnTask(msg, src);
	}

	@Override
	protected boolean hasSource(final String alarmSource) {
		return sourceContacts == null || sourceContacts.containsKey(alarmSource);
	}

	/**
	 * This task send the alarm message to every contact in the contact list.
	 * 
	 * @author Enrique Zamudio
	 */
	private class MsnTask implements Runnable {
		private final String msg;
		private final String src;

		private MsnTask(final String mensaje, final String source) {
			msg = mensaje;
			src = source;
		}

		@Override
		public void run() {
			if (msn != null) {
				MsnContact[] recps = msn.getContactList().getContacts();
				if (src != null && sourceContacts.containsKey(src)) {
					// Get the contacts for the source
					final List<String> who = sourceContacts.get(src);
					// Create a list and add the contacts we find
					// because some may not really be on the contact list
					recps = new MsnContact[who.size()];
					int pos = 0;
					for (final String u : who) {
						final MsnContact e = msn.getContactList().getContactByEmail(Email.parseStr(u));
						if (e != null) {
							recps[pos++] = e;
						}
					}
				}
				for (final MsnContact d : recps) {
					if (d != null) {
						try {
							msn.sendText(d.getEmail(), msg);
						} catch (final IllegalStateException ex) {
							log.error("MsnChannel sending to {}", d.getEmail());
						}
					}
				}
			}
		}

	}

	/**
	 * This method adds a contact to the account's contact list, so that contacts can be added to the account
	 * through the application.
	 */
	public void addContact(final String email) {
		log.info("MsnChannel adding contact {}", email);
		msn.addFriend(Email.parseStr(email), email);
	}

	/**
	 * This method removes the specified contact from the account's contact list, so that it won't receive
	 * any more alarms.
	 */
	public void removeContact(final String email) {
		for (final MsnContact cont : msn.getContactList().getContacts()) {
			if (cont.getEmail().equals(email)) {
				log.info("MsnChannel removing contact {} from the list", email);
				msn.removeFriend(cont.getEmail(), false);
				return;
			}
		}
	}

	/** This method returns a list with the addresses in the contact list. */
	public List<String> getContacts() {
		final List<String> l = new ArrayList<String>();
		for (final MsnContact cont : msn.getContactList().getContacts()) {
			l.add(cont.getEmail().getEmailAddress());
		}
		return l;
	}

	@Override
	@PreDestroy
	public void shutdown() {
		super.shutdown();
		msn.logout();
	}

}
