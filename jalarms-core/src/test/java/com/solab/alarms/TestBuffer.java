package com.solab.alarms;

import java.util.Collections;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Test the buffering mechanism introduced in 1.5
 * 
 * @author Enrique Zamudio
 */
public class TestBuffer implements UnitTestChannel.ChanDelegate {

	private Logger log = LoggerFactory.getLogger(getClass());
	private AlarmSender sender;
	private UnitTestChannel chan = new UnitTestChannel();
	private int state;

	@Before
	public void setup() {
		sender = new AlarmSender();
		sender.setAlarmChannels(Collections.singletonList((AlarmChannel)chan));
		chan.delegate = this;
	}

	@Test
	public void testNoBuffer() {
		log.info("Sending alarm without time buffer {}", String.format("%TT", new Date()));
		state = 1;
		chan.stamp = System.currentTimeMillis();
		sender.sendAlarmAlways("test");
		chan.waitForSend();
		assert System.currentTimeMillis() - chan.lastSent < 1000;
	}

	//@Test
	public void testWithBuffer() {
		//First, with no buffer, an alarm should be sent immediately
		state = 2;
		log.info("Setting up 65s buffer {}", String.format("%TT", new Date()));
		sender.setAlarmTimeBuffer(65000);
		sender.init();
		log.info("Sending 1 alarm with 65s buffer {}", String.format("%TT", new Date()));
		chan.prepare();
		sender.sendAlarmAlways("test");
		log.info("Waiting to get alarm, should be sent after 30s delay {}", String.format("%TT", new Date()));
		chan.waitForSend();
		long t1 = System.currentTimeMillis();
		assert t1 - chan.lastSent < 1000;
		assert t1 - chan.stamp >= 30000;
		log.info("Sending alarm 1/3 with buffer, wait 7 seconds {}", String.format("%TT", new Date()));
		state = 3;
		chan.stamp = System.currentTimeMillis();
		sender.sendAlarmAlways("test");
		try { Thread.sleep(7000); } catch (InterruptedException ex) {}
		log.info("Sending alarm 2/3 with buffer, wait 7 seconds {}", String.format("%TT", new Date()));
		sender.sendAlarmAlways("test");
		try { Thread.sleep(7000); } catch (InterruptedException ex) {}
		log.info("Sending alarm 3/3 with buffer, wait for send {}", String.format("%TT", new Date()));
		chan.sent.set(false);
		sender.sendAlarmAlways("test");
		chan.waitForSend();
		t1 = System.currentTimeMillis();
		assert t1 - chan.lastSent < 1000;
		assert t1 - chan.stamp >= 65000;
	}

	@Override
	public void alarmReceived(String msg, long when) {
		long now = System.currentTimeMillis();
		if (state == 1) {
			assert "test".equals(msg);
		} else if (state == 2) {
			assert "test".equals(msg);
			assert now - chan.stamp < 1000000000l && now - chan.stamp >= 30000;
		} else if (state == 3) {
			assert "test (3x)".equals(msg);
			assert now - chan.stamp < 1000000000l && now - chan.stamp >= 45000;
		}
	}

}
