package com.solab.alarms;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests the memcached client connected to a local instance; to avoid complicating your build
 * unnecessarily, if the memcached server doesn't exist then it just returns a default cache.
 * 
 * @author Enrique Zamudio
 */
public class TestMemcached implements UnitTestChannel.ChanDelegate {

	private AlarmMemcachedClient mc;
	private final Logger log = LoggerFactory.getLogger(getClass());
	protected AlarmSender sender = new AlarmSender();
	protected UnitTestChannel chan1 = new UnitTestChannel();
	protected UnitTestChannel chan2 = new UnitTestChannel();
	protected int c1t = 1000;
	protected int c2t = 1500;
	protected int w1 = 600;

	/** Creates the actual cache to be tested. This allows for easy testing of other caches which should
	 * have the exact same behavior, so you only need to override this method in a subclass. */
	protected AlarmCache createCache() {
		c1t = 4000;
		c2t = 6000;
		w1 = 2400;
		try {
			String slist = System.getProperty("memcaches");
			mc = new AlarmMemcachedClient();
			mc.setDefaultInterval(c1t*3);
			if (slist == null) {
				Socket sock = new Socket("127.0.0.1", 11211);
				sock.close();
				mc.setServers(Collections.singletonList("127.0.0.1"));
			} else {
				String[] s2 = slist.split(",");
				ArrayList<String> s3 = new ArrayList<String>(s2.length);
				for (String s : s2) {
					s3.add(s);
				}
				mc.setServers(s3);
			}
			mc.init();
			return mc;
		} catch (IOException ex) {
			System.out.println("No memcached available, skipping test");
			mc = null;
			return null;
		}
	}

	@Before
	public void setup() {
		sender.setAlarmCache(createCache());
		chan1.resend = c1t;
		chan2.resend = c2t;
		chan1.delegate = this;
		chan2.delegate = this;
		ArrayList<AlarmChannel> chans = new ArrayList<AlarmChannel>(2);
		chans.add(chan1);
		chans.add(chan2);
		sender.setAlarmChannels(chans);
	}

	@Test
	public void testCache() {
		if (mc == null) {
			log.info("Skipping cache test");
			return;
		}
		log.info("Chan1 resends every {} millis, Chan2 resends every {} millis", chan1.resend, chan2.resend);

		//First, check that msg1 is sent through both channels
		log.info("Sending msg1 which should be sent immediately");
		chan1.prepare();
		chan2.prepare();
		sender.sendAlarm("msg1", "src1");
		sender.sendAlarm("msg1", "src2");
		sender.sendAlarm("msg1");
		sender.sendAlarmAlways("nochan");
		chan2.waitForSend();
		long ls1 = chan1.lastSent;
		long ls2 = chan2.lastSent;
		assert ls1 - chan1.stamp > 0 && ls1 - chan1.stamp < 1000;
		assert ls2 - chan2.stamp > 0 && ls2 - chan2.stamp < 1000;

		//Wait
		log.info("waiting #1: {} millis {}", w1, String.format("%TT", new Date()));
		try { Thread.sleep(w1); } catch (InterruptedException ex) {}
		//msg1 should be ignored by both channels
		log.info("Both channels ignore msg1 {}", String.format("%TT", new Date()));
		chan1.prepare();
		chan2.prepare();
		sender.sendAlarm("msg1", "src1");
		sender.sendAlarm("msg1", "src2");
		sender.sendAlarm("msg1");
		try { Thread.sleep(50); } catch (InterruptedException ex) {}
		assert !chan1.sent.get() && !chan2.sent.get();
		assert chan1.lastSent == ls1 && chan2.lastSent == ls2;

		//Send using 'always', should be sent
		log.info("Sending using 'always', should be sent right away {}", String.format("%TT", new Date()));
		chan1.prepare();
		chan2.prepare();
		//By now time is about w1+50
		sender.sendAlarmAlways("always");
		chan2.waitForSend();
		assert ls1 < chan1.lastSent && ls2 < chan2.lastSent;

		//Wait
		log.info("waiting #2: {} millis {}", w1, String.format("%TT", new Date()));
		try { Thread.sleep(w1); } catch (InterruptedException ex) {}
		log.info("msg1 should be sent through chan1, ignored by chan2");
		ls2 = chan2.lastSent;
		chan1.prepare();
		chan2.prepare();
		sender.sendAlarm("msg1", "src1");
		sender.sendAlarm("msg1", "src2");
		sender.sendAlarm("msg1");
		chan1.waitForSend();
		ls1 = chan1.lastSent;
		assert ls1 - chan1.stamp >= 0 && ls1 - chan1.stamp < 1000;
		//chan2 should have not sent anything
		assert !chan2.sent.get() && chan2.lastSent == ls2;

		//Check that msg2 is sent through both channels
		log.info("Sending msg2 {}", String.format("%TT", new Date()));
		chan1.prepare();
		chan2.prepare();
		sender.sendAlarm("msg2", "src1");
		sender.sendAlarm("msg2", "src2");
		chan2.waitForSend();
		ls1 = chan1.lastSent;
		ls2 = chan2.lastSent;
		assert ls1 - chan1.stamp >= 0 && ls1 - chan1.stamp < 1000;
		assert ls2 - chan2.stamp >= 0 && ls2 - chan2.stamp < 1000;

		//Wait
		log.info("waiting #3: {} millis {}", w1, String.format("%TT", new Date()));
		try { Thread.sleep(w1); } catch (InterruptedException ex) {}
		log.info("msg1 should be sent through chan2, ignored by chan1");
		chan1.prepare();
		chan2.prepare();
		sender.sendAlarm("msg1", "src1");
		sender.sendAlarm("msg1", "src2");
		sender.sendAlarm("msg1");
		chan2.waitForSend();
		ls2 = chan2.lastSent;
		assert ls2 - chan2.stamp >= 0 && ls2 - chan1.stamp < 1000;
		//chan2 should have not sent anything
		assert !chan1.sent.get() && chan1.lastSent == ls1;

		//msg2 should be ignored by both channels
		log.info("Testing that msg2 is ignored by both channels {}", String.format("%TT", new Date()));
		ls1 = chan1.lastSent;
		ls2 = chan2.lastSent;
		chan1.prepare();
		chan2.prepare();
		//Time here is 2*w2 +100
		sender.sendAlarm("msg2", "src1");
		sender.sendAlarm("msg2", "src2");
		try { Thread.sleep(50); } catch (InterruptedException ex) {}
		assert !chan1.sent.get() && !chan2.sent.get();
		assert chan1.lastSent == ls1 && chan2.lastSent == ls2;

		//Wait
		log.info("waiting #4: {} millis {}", w1, String.format("%TT", new Date()));
		try { Thread.sleep(w1); } catch (InterruptedException ex) {}

		log.info("msg1 should be sent through chan1, ignored by chan2");
		chan1.prepare();
		chan2.prepare();
		sender.sendAlarm("msg1", "src1");
		sender.sendAlarm("msg1", "src2");
		sender.sendAlarm("msg1");
		chan1.waitForSend();
		ls1 = chan1.lastSent;
		assert ls1 - chan1.stamp >= 0 && ls1 - chan1.stamp < 1000;
		//chan2 should have not sent anything
		assert !chan2.sent.get() && chan2.lastSent == ls2;

		log.info("msg2 should be sent through chan1, ignored by chan2");
		chan1.prepare();
		chan2.prepare();
		ls2 = chan2.lastSent;
		sender.sendAlarm("msg2", "src1");
		sender.sendAlarm("msg2", "src2");
		chan1.waitForSend();
		ls1 = chan1.lastSent;
		assert ls1 - chan1.stamp >= 0 && ls1 - chan1.stamp < 1000;
		//chan2 should have not sent anything
		assert !chan2.sent.get() && chan2.lastSent == ls2;

		//Wait
		log.info("waiting #5: {} millis {}", w1, String.format("%TT", new Date()));
		try { Thread.sleep(w1); } catch (InterruptedException ex) {}
		log.info("Both channels ignore msg1 {}", String.format("%TT", new Date()));
		chan1.prepare();
		chan2.prepare();
		sender.sendAlarm("msg1", "src1");
		sender.sendAlarm("msg1", "src2");
		sender.sendAlarm("msg1");
		try { Thread.sleep(50); } catch (InterruptedException ex) {}
		assert !chan1.sent.get() && !chan2.sent.get();
		assert chan1.lastSent == ls1 && chan2.lastSent == ls2;

		log.info("msg2 should be sent through chan2, ignored by chan1");
		chan1.prepare();
		chan2.prepare();
		ls1 = chan1.lastSent;
		sender.sendAlarm("msg2", "src1");
		sender.sendAlarm("msg2", "src2");
		chan2.waitForSend();
		ls2 = chan2.lastSent;
		assert ls2 - chan2.stamp >= 0 && ls2 - chan1.stamp < 1000;
		//chan2 should have not sent anything
		assert !chan1.sent.get() && chan1.lastSent == ls1;

		//Wait
		log.info("waiting #6: {} millis {}", w1, String.format("%TT", new Date()));
		try { Thread.sleep(w1); } catch (InterruptedException ex) {}
		log.info("Sending msg1 through both");
		chan1.prepare();
		chan2.prepare();
		sender.sendAlarm("msg1", "src1");
		sender.sendAlarm("msg1", "src2");
		sender.sendAlarm("msg1");
		sender.sendAlarmAlways("nochan");
		chan2.waitForSend();
		ls1 = chan1.lastSent;
		ls2 = chan2.lastSent;
		assert ls1 - chan1.stamp > 0 && ls1 - chan1.stamp < 1000;
		assert ls2 - chan2.stamp > 0 && ls2 - chan2.stamp < 1000;
		log.info("msg2 should be sent through chan1, ignored by chan2");
		chan1.prepare();
		chan2.prepare();
		ls2 = chan2.lastSent;
		sender.sendAlarm("msg2", "src1");
		sender.sendAlarm("msg2", "src2");
		chan1.waitForSend();
		ls1 = chan1.lastSent;
		assert ls1 - chan1.stamp >= 0 && ls1 - chan1.stamp < 1000;
		//chan2 should have not sent anything
		assert !chan2.sent.get() && chan2.lastSent == ls2;
	}

	@Override
	public void alarmReceived(String msg, long when) {
		//nothing is needed here
	}

	@After
	public void disconnect() {
		if (mc != null) {
			mc.disconnect();
		}
	}

}
