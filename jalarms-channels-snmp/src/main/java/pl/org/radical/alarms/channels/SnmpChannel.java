package pl.org.radical.alarms.channels;

import pl.org.radical.alarms.AbstractAlarmChannel;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.snmp4j.CommunityTarget;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.IpAddress;
import org.snmp4j.smi.TcpAddress;
import org.snmp4j.smi.UdpAddress;

public class SnmpChannel extends AbstractAlarmChannel {

	public enum AddressType {
		UDP, TCP, IP
	};

	private String address;

	private AddressType addressType;
	private Address targetAddres;
	private CommunityTarget communityTarget;

	@PostConstruct
	public void init() {
		try {
			if (address != null && !address.isEmpty()) {
				switch (addressType) {
					case UDP:
						targetAddres = new UdpAddress(address);
						break;
					case TCP:
						targetAddres = new TcpAddress(address);
						break;
					case IP:
						targetAddres = new IpAddress(address);
					default:
						break;
				}
			} else {
				log.error("Address parameter was not defined for SNMP channel. SNMP alarms will not be sent!");
			}

			communityTarget = new CommunityTarget(targetAddres);
		} catch (IOException e) {
			log.error("Connecting to SNMP {}:{} as {}; SNMP alarms will not be sent.", host, port, uname);
		}
	}

	@Override
	protected Runnable createSendTask(final String msg, final String source) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected boolean hasSource(final String alarmSource) {
		// TODO Auto-generated method stub
		return false;
	}

}
