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
import java.text.MessageFormat;
import evs4j.Connection;
import evs4j.ConnectionDecorator;
import evs4j.Message;
import evs4j.Alert;
import evs4j.impl.SRPTokenAlert;

/**
 * This class dumps throuhgput and latency information to standard error. It assumes
 * that it is wrapped around an SRPConnection (or a wrapper thereof.)
 */
public class MonitorConnection extends ConnectionDecorator {

    private PrintWriter out;

    private ThroughputMeter throughputMeter;

    public ThroughputMeter getThroughputMeter() {
	return throughputMeter;
    }

    private TokenRotationMeter tokenRotationMeter;
    
    public TokenRotationMeter getTokenRotationMeter() {
	return tokenRotationMeter;
    }

    private WindowMeter windowMeter;

    public WindowMeter getWindowMeter() {
	return windowMeter;
    }

    private RetransmissionMeter retransmissionMeter;

    public RetransmissionMeter getRetransmissionMeter() {
	return retransmissionMeter;
    }

    public MonitorConnection(Connection conn, PrintWriter out) {
	super(conn);
	this.out = out;
	this.throughputMeter = new ThroughputMeter();
	this.tokenRotationMeter = new TokenRotationMeter();
	this.windowMeter = new WindowMeter();
	this.retransmissionMeter = new RetransmissionMeter();
	this.interval = DEFAULT_INTERVAL;
	this.lastTime = System.currentTimeMillis();
    }

    public void send(Message message) throws IOException {
	//forward message down
	conn.send(message);
    }

    public void onMessage(Message message) {
	listener.onMessage(message);
    }

    public void onAlert(Alert alert) {
	if (alert instanceof SRPTokenAlert) {
	    SRPTokenAlert tokenAlert = (SRPTokenAlert) alert;
	    long currentTime = System.currentTimeMillis();	    
	    tokenRotationMeter.update(currentTime);
	    windowMeter.update(tokenAlert.getWindow());
	    throughputMeter.update(tokenAlert.getLowMessageId(), currentTime);
	    retransmissionMeter.update(tokenAlert.getMissed());
	    if (isDue(currentTime)) {
		dump();
	    }
	}
	listener.onAlert(alert);
    }
    
    private static final String DUMP_FORMAT =
	"   throughput = {0,number,integer} messages/s\n" +
	"rotation time = {1,number,###.##} ms\n" +
	"       window = {2,number,integer} messages\n" + 
	"retransmitted = {3,number,###.##} messages\n";

    private void dump() {
	Object[] args = new Object[] { 
	    new Integer(throughputMeter.getThroughput()),
	    new Integer(tokenRotationMeter.getRotationTime()),
	    new Integer(windowMeter.getWindow()),
	    new Double(retransmissionMeter.getMissedAndClear())
	};
	print(MessageFormat.format(DUMP_FORMAT, args));
    }
    
    private void print(String s) {
	out.println(s);
	out.flush();
    }

    private static final long DEFAULT_INTERVAL = 5000;

    private long interval;

    private long lastTime;

    private boolean isDue(long currentTime) {
	boolean due;
	if (currentTime >= lastTime + interval) {
	    due = true;
	    lastTime = currentTime;
	} else {
	    due = false;
	}
	return due;
    }

}
