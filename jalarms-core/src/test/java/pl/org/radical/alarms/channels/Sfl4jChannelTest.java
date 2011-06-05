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

public class Sfl4jChannelTest {
	private final File file = new File("target/test.log");

	@Test
	public void testNoSourceAlarm() throws IOException, InterruptedException {
		List<AlarmChannel> channels = new ArrayList<AlarmChannel>(1);
		Sfl4jChannel channel = new Sfl4jChannel();
		channels.add(channel);

		AlarmSender sender = new AlarmSender();
		sender.setAlarmChannels(channels);

		sender.sendAlarm("Test message, no source");

		Thread.sleep(1000L);

		String lines = FileUtils.readFileToString(file);
		assertTrue("No lines with specified text in log file", lines.contains("ALARM - Test message, no source"));
	}

	@Test
	public void testAlarmWithSource() throws IOException, InterruptedException {
		List<AlarmChannel> channels = new ArrayList<AlarmChannel>(1);
		Sfl4jChannel channel = new Sfl4jChannel();
		channels.add(channel);

		AlarmSender sender = new AlarmSender();
		sender.setAlarmChannels(channels);

		sender.sendAlarm("Test message, with source", "TestSource");

		Thread.sleep(1000L);

		String lines = FileUtils.readFileToString(file);
		assertTrue("No lines with specified text in log file", lines.contains("ALARM.src - Test message, with source"));
	}
}
