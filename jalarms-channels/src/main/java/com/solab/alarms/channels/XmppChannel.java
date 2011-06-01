/*
This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
*/
package com.solab.alarms.channels;

import com.solab.alarms.AbstractAlarmChannel;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * Sends an alarm to an xmpp chat server, e.g. google talk.
 * You can configure a list of contacts and/or a list of groups per alarm source.
 * The alarm will be sent to each contact in contactsBySource and to each contact that
 * belongs to a group in groupsBySource.
 *
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


    public void setContactsBySource(Map<String, List<String>> contactsBySource) {
        this.contactsBySource = contactsBySource;
    }

    public void setGroupsBySource(Map<String, List<String>> groupsBySource) {
        this.groupsBySource = groupsBySource;
    }

    @PostConstruct
    public void init() {
        ConnectionConfiguration config = new ConnectionConfiguration(host, port, domain);
        config.setSASLAuthenticationEnabled(SASLAuthenticationEnabled);
        xmpp = new XMPPConnection(config);
        try {
            xmpp.connect();            
            addLogOutShutdownHook();
            xmpp.login(username, password);           
        } catch (XMPPException e) {
            log.error("Cannot connect to the XMPP server " + this.host + " with port " + this.port
                    + " using account " + this.username + " in the domain " + this.domain, e);
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
    protected Runnable createSendTask(String msg, String source) {
        return new XmppTask(msg, source);
    }


    @Override
    protected boolean hasSource(String alarmSource) {
        return (contactsBySource == null || contactsBySource.containsKey(alarmSource) )
               || ( groupsBySource == null || groupsBySource.containsKey(alarmSource)); 

    }


    private class XmppTask implements Runnable {

        private final String msg;
        private final String src;
        


        private XmppTask(String message, String source) {
            msg = message;
            src = source;
        }


        @Override
        public void run() {
            
            if (xmpp != null) {
                if (xmpp.isConnected() && xmpp.isAuthenticated()) {
                    boolean sendToAll = true;
                    if( src != null ){
                        if ( contactsBySource != null && contactsBySource.containsKey(src) ) {
                            sendToList( contactsBySource.get(src) );
                            sendToAll = false;
                        }
                        if ( groupsBySource != null && groupsBySource.containsKey(src) ) {
                            sendToGroups( groupsBySource.get(src) );
                            sendToAll = false;
                        }
                    }

                    if( sendToAll ) {
                        sendToAll();
                    }

                } else {
                    log.error("Error sending message to xmpp host and recipient, connection is closed or" +
                            "the user is not authenticated ");
                }
            }


        }



        private void sendToGroups( List<String> groups ) {
            Roster roster = xmpp.getRoster();
            for( String group : groups ) {
                sendToGroup( roster, group );
            }
        }

        private void sendToGroup(Roster roster, String groupName ) {            
            RosterGroup group = roster.getGroup( groupName );
            if( group != null ) {
                sendToList( rosterEntriesToContactList( group.getEntries() ));
            }

        }


        private void sendToList( Collection<String> contacts ) {
            for( String contact : contacts ) {
                sendMessageToContact( contact );
            }
        }

        private void sendToAll() {
            Roster roster = xmpp.getRoster();
            sendToList( rosterEntriesToContactList( roster.getEntries() ) );
        }


        private void sendMessageToContact(String contact) {
            ChatManager chatmanager = xmpp.getChatManager();
            
            Chat alarmChat = chatmanager.createChat(contact,
                    new MessageListener() {
                        public void processMessage(Chat chat, Message message) { }
                    });
            try {
                alarmChat.sendMessage(msg);
                
            } catch (XMPPException e) {
                log.error("Error sending message to xmpp contact " + contact, e);
            }
        }

        private List<String> rosterEntriesToContactList( Collection<RosterEntry> entries ) {
            List<String> contacts = new ArrayList<String>( entries.size() );
            for( RosterEntry entry : entries ) {
                    contacts.add( entry.getUser() );
            }
            return contacts;
        }

    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setSASLAuthenticationEnabled(boolean SASLAuthenticationEnabled) {
        this.SASLAuthenticationEnabled = SASLAuthenticationEnabled;
    }
}


