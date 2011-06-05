package pl.org.radical.alarms.channels;

import static org.junit.Assert.assertTrue;
import pl.org.radical.alarms.AlarmChannel;
import pl.org.radical.alarms.AlarmSender;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import de.huxhorn.lilith.slf4j.Logger.Level;

public class Sfl4jChannelTest {
	private final File file = new File("target/test.log");

	@Test
	public void testNoSourceAlarm() throws IOException, InterruptedException {
		String msg = "Test message, no source";

		List<AlarmChannel> channels = new ArrayList<AlarmChannel>(1);
		Sfl4jChannel channel = new Sfl4jChannel();
		channels.add(channel);

		AlarmSender sender = new AlarmSender();
		sender.setAlarmChannels(channels);

		sender.sendAlarm(msg);

		Thread.sleep(1000L);

		String lines = FileUtils.readFileToString(file);
		assertTrue("No lines with specified text in log file", lines.contains("WARN  ALARM - " + msg));
	}

	@Test
	public void testAlarmWithSource() throws IOException, InterruptedException {
		String msg = "Test message, with source";
		String src = "TestSource";

		List<AlarmChannel> channels = new ArrayList<AlarmChannel>(1);
		Sfl4jChannel channel = new Sfl4jChannel();
		channels.add(channel);

		AlarmSender sender = new AlarmSender();
		sender.setAlarmChannels(channels);

		sender.sendAlarm(msg, src);

		Thread.sleep(1000L);

		String lines = FileUtils.readFileToString(file);
		assertTrue("No lines with specified text in log file", lines.contains("WARN  ALARM." + src + " - " + msg));
	}

	@Test
	public void testAlarmNoSourceAndLevel() throws IOException, InterruptedException {
		String msg = "Test message, no source but ERROR level";

		List<AlarmChannel> channels = new ArrayList<AlarmChannel>(1);
		Sfl4jChannel channel = new Sfl4jChannel();
		channel.setLevel(Level.ERROR);
		channels.add(channel);

		AlarmSender sender = new AlarmSender();
		sender.setAlarmChannels(channels);

		sender.sendAlarm(msg);

		Thread.sleep(1000L);

		String lines = FileUtils.readFileToString(file);
		assertTrue("No lines with specified text in log file", lines.contains("ERROR ALARM - " + msg));
	}

	@Test
	public void testAlarmWithSourceAndLevel() throws IOException, InterruptedException {
		String msg = "Test message, with source and ERROR level";
		String src = "TestSource";

		List<AlarmChannel> channels = new ArrayList<AlarmChannel>(1);
		Sfl4jChannel channel = new Sfl4jChannel();
		channel.setLevel(Level.ERROR);
		channels.add(channel);

		AlarmSender sender = new AlarmSender();
		sender.setAlarmChannels(channels);

		sender.sendAlarm(msg, src);

		Thread.sleep(1000L);

		String lines = FileUtils.readFileToString(file);
		assertTrue("No lines with specified text in log file", lines.contains("ERROR ALARM." + src + " - " + msg));
	}
}
