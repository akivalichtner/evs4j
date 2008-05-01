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

package evs4j.tool.benchmark;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Vector;
import evs4j.Alert;
import evs4j.Connection;
import evs4j.Listener;
import evs4j.Message;
import evs4j.Processor;
import evs4j.Configuration;
import evs4j.impl.SRPConnection;

/**
 * A stripped-down tool to give you an idea of the throughput and latency
 * you will be able to get out of your machines. This class can simulate a ring
 * of 'processors' sending and receiving messages full-blast.
 * Run this class from the command line to see usage and/or read the README file.
 */
public class Main {

    public static final String OPT_PROCS = "-procs";
    public static final String OPT_TIME = "-time";
    public static final String OPT_PROPS = "-props";

    public static void main(String[] args) throws Exception {
	Vector processors = new Vector();
	long time = 0;
	String props = null;
	try {
	    for (int i=0; i<args.length; i++) {
		String arg = args[i];
		if (arg.equals(OPT_PROCS)) {
		    while (i < args.length - 1) {
			arg = args[++i];
			if (arg.startsWith("-")) {
			    --i;
			    break;
			} else {
			    processors.addElement(arg);
			}
		    }
		    if (processors.size() == 0) {
			printUsage();
		    }
		} else if (arg.equals(OPT_TIME)) {
		    time = Long.parseLong(args[++i]) * 1000L;
		} else if (arg.equals(OPT_PROPS)) {
		    props = args[++i];
		} else {
		    dump("Illegal option: " + arg);
		    printUsage();
		}
	    }
	} catch (ArrayIndexOutOfBoundsException e) {
	    printUsage();
	}
	Enumeration enumeration = processors.elements();
	Vector apps = new Vector();
	boolean monitor = true;
	while (enumeration.hasMoreElements()) {
	    String tmp = (String) enumeration.nextElement();
	    Processor processor = new Processor(Integer.parseInt(tmp));
	    long configurationID = 0L;
	    try {
		App app = new App(configurationID, processor, props, monitor);
		apps.addElement(app);
	    } catch (IllegalArgumentException e) {
		dump(e.getMessage());
		System.exit(1);
	    }
	    //monitor first connection only
	    monitor = false;
	}
	if (time > 0) {
	    //wait, then kill each thread
	    try {
		Thread.sleep(time);
	    } catch (InterruptedException e) {
		//done
	    }
	    //stop each TestApp in an orderly fashion instead
	    //of exiting, so we can run the profiler
	    for (int i = 0; i < apps.size(); i++) {
		App app = (App) apps.elementAt(i);
		app.close();
	    }
	}
    }

    private static void printUsage() {
	dump("java " + Main.class.getName());
	dump("required:");
	dumpOption(OPT_PROCS, "processor ids (integers)");
	dumpOption(OPT_TIME, "total running time (s)");
	dumpOption(OPT_PROPS, "connection property string (in quotes)");
	System.exit(1);
    }

    private static void dumpOption(String name, String comment) {
	int tab = 20;
	StringBuffer buf = new StringBuffer();
	buf.append(name);
	while (buf.length() < tab) {
	    buf.append(' ');
	}
	buf.append('<');
	buf.append(comment);
	buf.append('>');
	dump(buf.toString());
    }

    private static void dump(String s) {
	System.err.println(s);
    }

    private static class App extends Thread implements Listener {

	protected Connection conn;
	
	public void close() throws IOException {
	    conn.close();
	}
	
	protected Processor processor;

	public App(long configurationID,
		   Processor processor,
		   String props,
		   boolean monitor) throws Exception {
	    this.processor = processor;
	    conn = new SRPConnection(configurationID, processor, props);
	    if (monitor) {
		PrintWriter out = new PrintWriter(System.out);
		MonitorConnection monitorConn = new MonitorConnection(conn, out);
		conn = monitorConn;
	    }
	    conn.setListener(this);
	    conn.open();
	    setDaemon(true);
	    start();
	}
	
	public void run() {
	    while (true) {
		if (configuration != null) {
		    break;
		}
		try {
		    Thread.sleep(1);
		} catch (InterruptedException e) {
		    //ready to transmit
		}
	    }
	    byte[] data = new byte[conn.getMaxMessageSize()];
	    boolean safe = false;
	    while (true) {
		Message message = conn.createMessage(safe);
		int length = data.length;
		System.arraycopy(data, 0,
				 message.getData(), message.getOffset(),
				 length);
		message.setLength(length);
		try {
		    conn.send(message);
		} catch (IOException e) {
		    e.printStackTrace();
		    throw new RuntimeException("Failed to send");
		}
	    }
	}
	
	private Configuration configuration;
	
	public void onConfiguration(Configuration configuration) {
	    this.configuration = configuration;
	    if (configuration.isTransitional()) {
		dump("Installed transitional configuration: " + configuration);
	    } else {
		dump("Installed regular configuration: " + configuration);
	    }
	}
	
	public void onAlert(Alert alert) {
	    //discard (done)
	}

	public void onMessage(Message m) {
	    //discard (done)
	}
	
    }

}
