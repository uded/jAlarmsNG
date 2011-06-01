package pl.org.radical.alarms.channels;

import pl.org.radical.alarms.AbstractAlarmChannel;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This alarm channel executes a command-line program, either passing the alarm message as a parameter to it as an argument of ${alarm},
 * or feeding the alarm message to its STDIN.
 * 
 * @author Enrique Zamudio
 */
public class CommandLineChannel extends AbstractAlarmChannel {

	private String cmd[];
	private Map<String, String[]> src_cmds;

	/**
	 * Sets the command to execute, as a single statement to be execute. The alarm message is simply
	 * appended as an argument.
	 */
	public void setCommand(final String value) {
		cmd = new String[] { value, "${alarm}" };
	}

	/**
	 * Sets the command to execute, with each argument as a separate element.
	 * One of the elements can be a ${alarm} variable which will be replaced with the alarm message;
	 * if the first elements starts with "STDIN:" then that prefix is removed and the alarm message
	 * is fed to the program's STDIN.
	 */
	public void setCommandWithArgs(final List<String> value) {
		cmd = new String[value.size()];
		value.toArray(cmd);
	}

	/**
	 * Sets a map with different commands to be executed depending on the alarm source.
	 * Each value must be a list of strings, the first being the command to execute and the others
	 * the arguments.
	 */
	public void setCommandsWithArgsBySource(final Map<String, String[]> value) {
		src_cmds = value;
	}

	/**
	 * Sets a map with different commands to be executed depending on the alarm source.
	 * Each value will be interpreted as a single command and when executed, the alarm message
	 * will be passed as a single argument.
	 */
	public void setCommandsBySource(final Map<String, String> value) {
		final HashMap<String, String[]> x = new HashMap<String, String[]>(value.size());
		for (final Map.Entry<String, String> e : value.entrySet()) {
			x.put(e.getKey(), new String[] { e.getValue(), "${alarm}" });
		}
		src_cmds = x;
	}

	@Override
	protected Runnable createSendTask(final String msg, final String source) {
		// Get the default command, or the command for the specified source
		String[] _cmd = cmd;
		if (source != null && src_cmds != null && src_cmds.containsKey(source)) {
			_cmd = src_cmds.get(source);
		}
		return new CmdTask(_cmd, msg);
	}

	@Override
	protected boolean hasSource(final String alarmSource) {
		return src_cmds == null || src_cmds.containsKey(alarmSource);
	}

	private class CmdTask implements Runnable {

		private final String[] command;
		private final String alarm;

		private CmdTask(final String[] theCommand, final String msg) {
			command = theCommand;
			alarm = msg;
		}

		@Override
		public void run() {
			// Escape the alarm
			if (command[0].startsWith("STDIN:")) {
				try {
					command[0] = command[0].substring(6);
					final Process p = Runtime.getRuntime().exec(command);
					p.getOutputStream().write(alarm.getBytes());
					p.getOutputStream().close();
				} catch (final IOException ex) {
					log.error(String.format("Unable to execute STDIN command for alarm '%s'", alarm), ex);
				}
			} else {
				for (int i = 0; i < command.length; i++) {
					if (command[i].equals("${alarm}")) {
						command[i] = alarm;
					}
				}
				try {
					Runtime.getRuntime().exec(command);
				} catch (final IOException ex) {
					log.error("Unable to execute command for alarm '{}'", alarm, ex);
				}
			}
		}

	}

}
