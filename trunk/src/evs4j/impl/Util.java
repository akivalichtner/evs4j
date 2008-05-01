/**
 *
 *  Copyright 2000-2006 Guglielmo Lichtner (lichtner_at_bway_dot_net)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package evs4j.impl;

import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class Util {

    public static final int IP_VERSION = 4;

    public static NetworkInterface findNetworkInterface(String netString, String maskString) 
	throws SocketException {
	long net = ipToLong(netString);
	long mask = ipToLong(maskString);
	NetworkInterface nic = null;
	Enumeration nics = NetworkInterface.getNetworkInterfaces();
	while (nics.hasMoreElements()) {
	    NetworkInterface tmp = (NetworkInterface) nics.nextElement();
	    Enumeration ips = tmp.getInetAddresses();
	    while (ips.hasMoreElements()) {
		InetAddress inetAddress = (InetAddress) ips.nextElement();
		if (inetAddress instanceof Inet4Address) {
		    long ip = ipToLong(inetAddress.getHostAddress());
		    if (net == (ip & mask)) {
			nic = tmp;
			break;
		    }
		}
	    }
	}
	return nic;
    }

    private static long ipToLong(String ipString) {
	long ip = 0L;
	String[] tokens = ipString.split("\\.");
	for (int i = 0; i < IP_VERSION; i++) {
	    long tmp = Long.parseLong(tokens[i]);
	    ip <<= 8;
	    ip |= tmp;
	}
	return ip;
    }

}
