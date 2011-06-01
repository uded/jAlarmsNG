package pl.org.radical.alarms;

/** This interface defines the behavior of an alarm channel. Alarm channels are used by
 * the AlarmSender. An AlarmChannel can have a default user list, and it can also send
 * messages to specific users for certain cases.
 * 
 * @author Enrique Zamudio
 */
public interface AlarmChannel {

	/** Sends the alarm message to the users defined for the channel.
	 * @param msg The alarm message to be sent.
	 * @param source The alarm source. A channel can send the alarm to different recipients depending on the source.
	 * This parameter can be null, which means that it should be sent to the default recipients for the channel. */
	public void send(String msg, String source);

	/** Returns the minimum time interval to resend the same message through this channel. An interval
	 * of 0 or a negative number means that the same message will always be sent. This interval is specified in milliseconds. */
	public int getMinResendInterval();

	/** Shuts the channel down, closing any open connections it has and freeing up all its resources.
	 * Once this method is invoked on a channel, the channel should not be used again. */
	public void shutdown();

}
