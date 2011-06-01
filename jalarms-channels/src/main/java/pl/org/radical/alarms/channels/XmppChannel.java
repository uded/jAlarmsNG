package pl.org.radical.alarms.channels;

import pl.org.radical.alarms.AbstractAlarmChannel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

/**
 * Sends an alarm to an xmpp chat server, e.g. google talk.
 * You can configure a list of contacts and/or a list of groups per alarm source.
 * The alarm will be sent to each contact in contactsBySource and to each contact that
 * belongs to a group in groupsBySource.
 * Written during Hackergarten Mexico 2011
 * 
 * @author Luis Crespo (luisfcrespo)
 * @author Gerardo Aquino (gaquinog)
 * @author Jose Antonio Vargas (jaen18)
 * @author Erick Camacho (ecamacho)
 */
public class XmppChannel extends AbstractAlarmChannel {

	/**
	 * username of the xmpp account with no domain, e.g. user
	 */
	private String username;

	/**
	 * password of the xmpp account
	 */
	private String password;

	/**
	 * domain of the account, e.g. gmail.com
	 */
	private String domain;

	/**
	 * xmpp server, e.g. talk.google.com
	 */
	private String host;

	/**
	 * port of the xmpp server, e.g. 5222
	 */
	private Integer port;

	/**
	 * Enables SASLAuthentication. You have to disabled this
	 * property if you are connecting to google talk.
	 */
	private boolean SASLAuthenticationEnabled;

	/**
	 * Contacts to which the alarm will be sent grouped by source.
	 * The contact must be a valid jabber username with its domain, e.g. user@google.com
	 */
	private Map<String, List<String>> contactsBySource;

	/**
	 * Groups to which the alarm will be sent grouped by source.
	 * The groups must exist in the configuration of the xmpp account
	 * used to send the messages.
	 */
	private Map<String, List<String>> groupsBySource;

	private XMPPConnection xmpp;

	public void setContactsBySource(final Map<String, List<String>> contactsBySource) {
		this.contactsBySource = contactsBySource;
	}

	public void setGroupsBySource(final Map<String, List<String>> groupsBySource) {
		this.groupsBySource = groupsBySource;
	}

	@PostConstruct
	public void init() {
		final ConnectionConfiguration config = new ConnectionConfiguration(host, port, domain);
		config.setSASLAuthenticationEnabled(SASLAuthenticationEnabled);
		xmpp = new XMPPConnection(config);
		try {
			xmpp.connect();
			addLogOutShutdownHook();
			xmpp.login(username, password);
		} catch (final XMPPException e) {
			log.error("Cannot connect to the XMPP server " + host + " with port " + port + " using account " + username + " in the domain "
			        + domain, e);
			xmpp = null;
		}
	}

	private void addLogOutShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				xmpp.disconnect();
			}
		});
	}

	@Override
	protected Runnable createSendTask(final String msg, final String source) {
		return new XmppTask(msg, source);
	}

	@Override
	protected boolean hasSource(final String alarmSource) {
		return contactsBySource == null || contactsBySource.containsKey(alarmSource) || groupsBySource == null
		        || groupsBySource.containsKey(alarmSource);

	}

	private class XmppTask implements Runnable {

		private final String msg;
		private final String src;

		private XmppTask(final String message, final String source) {
			msg = message;
			src = source;
		}

		@Override
		public void run() {

			if (xmpp != null) {
				if (xmpp.isConnected() && xmpp.isAuthenticated()) {
					boolean sendToAll = true;
					if (src != null) {
						if (contactsBySource != null && contactsBySource.containsKey(src)) {
							sendToList(contactsBySource.get(src));
							sendToAll = false;
						}
						if (groupsBySource != null && groupsBySource.containsKey(src)) {
							sendToGroups(groupsBySource.get(src));
							sendToAll = false;
						}
					}

					if (sendToAll) {
						sendToAll();
					}

				} else {
					log.error("Error sending message to xmpp host and recipient, connection is closed or"
					        + "the user is not authenticated ");
				}
			}

		}

		private void sendToGroups(final List<String> groups) {
			final Roster roster = xmpp.getRoster();
			for (final String group : groups) {
				sendToGroup(roster, group);
			}
		}

		private void sendToGroup(final Roster roster, final String groupName) {
			final RosterGroup group = roster.getGroup(groupName);
			if (group != null) {
				sendToList(rosterEntriesToContactList(group.getEntries()));
			}

		}

		private void sendToList(final Collection<String> contacts) {
			for (final String contact : contacts) {
				sendMessageToContact(contact);
			}
		}

		private void sendToAll() {
			final Roster roster = xmpp.getRoster();
			sendToList(rosterEntriesToContactList(roster.getEntries()));
		}

		private void sendMessageToContact(final String contact) {
			final ChatManager chatmanager = xmpp.getChatManager();

			final Chat alarmChat = chatmanager.createChat(contact, new MessageListener() {
				@Override
				public void processMessage(final Chat chat, final Message message) {
				}
			});
			try {
				alarmChat.sendMessage(msg);

			} catch (final XMPPException e) {
				log.error("Error sending message to xmpp contact " + contact, e);
			}
		}

		private List<String> rosterEntriesToContactList(final Collection<RosterEntry> entries) {
			final List<String> contacts = new ArrayList<String>(entries.size());
			for (final RosterEntry entry : entries) {
				contacts.add(entry.getUser());
			}
			return contacts;
		}

	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public void setDomain(final String domain) {
		this.domain = domain;
	}

	public void setHost(final String host) {
		this.host = host;
	}

	public void setPort(final Integer port) {
		this.port = port;
	}

	public void setSASLAuthenticationEnabled(final boolean SASLAuthenticationEnabled) {
		this.SASLAuthenticationEnabled = SASLAuthenticationEnabled;
	}
}
