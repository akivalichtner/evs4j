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

package evs4j.impl.message;

import java.util.Set;
import java.util.TreeSet;
import java.net.DatagramPacket;
import evs4j.Processor;
import evs4j.impl.SRPTokenAlert;
import evs4j.impl.WindowController;
import evs4j.impl.SRPConnection;
import evs4j.impl.SRPState;

public class RegularTokenMessage extends TokenMessage {

    /**
     * The maximum number of missed messages ids to be
     * listed in the token, as defined in the protocol
     * specification.
     */
    public static final int MAX_MISSED_MESSAGES = 362;

    /**
     * The largest id of any message that
     * has been broadcast on the configuration.
     */
    private int maxMessageId;

    public int getMaxMessageId() {
	return maxMessageId;
    }

    public void setMaxMessageId(int maxMessageId) {
	this.maxMessageId = maxMessageId;
    }

    /**
     * The largest id such that all processors have
     * received all messages with ids less than or equal
     * to its id. Used to determine if a message 
     * can be delivered <em>safely</em>.
     */
    private int lowMessageId;

    public int getLowMessageId() {
	return lowMessageId;
    }
    
    public void setLowMessageId(int lowMessageId) {
	this.lowMessageId = lowMessageId;
    }

    /**
     * The Processor object of the last processor that set
     * the lowMessageId to a value less that the maxMessageId.
     */
    private Processor slowProcessor;

    public Processor getSlowProcessor() {
	return slowProcessor;
    }

    public void setSlowProcessor(Processor slowProcessor) {
	this.slowProcessor = slowProcessor;
    }

    /**
     * The retransmission requests.
     */
    private Set missed;
    
    public Set getMissed() {
	return missed;
    }

    public void setMissed(Set missed) {
	this.missed = missed;
    }

    /**
     * A count of the number of messages broadcast
     * by all processors during the previous rotation of
     * the token.
     */
    private int totalBroadcast;

    public int getTotalBroadcast() {
	return totalBroadcast;
    }

    public void setTotalBroadcast(int totalBroadcast) {
	this.totalBroadcast = totalBroadcast;
    }

    /**
     * The sum of the number of messages waiting to
     * be transmitted by each processor at the time at which
     * the processor forwarded the token during the previous
     * rotation.
     */
    private int totalBacklog;

    public int getTotalBacklog() {
	return totalBacklog;
    }

    public void setTotalBacklog(int totalBacklog) {
	this.totalBacklog = totalBacklog;
    }

    /**
     * The value of the window size to be used
     * in this token rotation.
     */
    private float window;

    public float getWindow() {
	return window;
    }

    public void setWindow(float window) {
	this.window = window;
    }

    /**
     * The window size threshold used when
     * growing the window size.
     */
    private float threshold;

    public float getThreshold() {
	return threshold;
    }

    public void setThreshold(float threshold) {
	this.threshold = threshold;
    }

    public RegularTokenMessage(int magic,
			       Buffer buffer,
			       long configurationId,
			       int id,
			       Processor destination,
			       int maxMessageId,
			       int lowMessageId,
			       Processor slowProcessor,
			       Set missed,
			       int totalBroadcast,
			       int totalBacklog,
			       float window,
			       float threshold) {
	super(magic, 
	      Message.TYPE_REGULAR_TOKEN,
	      buffer,
	      configurationId,
	      id,
	      destination);
	this.maxMessageId = maxMessageId;
	this.lowMessageId = lowMessageId;
	this.slowProcessor = slowProcessor;
	this.missed = missed;
	this.totalBroadcast = totalBroadcast;
	this.totalBacklog = totalBacklog;
	this.window = window;
	this.threshold = threshold;
    }
    
    /**
     * Use this constructor when starting a configuration.
     */
    public RegularTokenMessage(Buffer buffer,
			       long configurationId,
			       int id) {
	this(MAGIC_NUMBER,
	     buffer,
	     configurationId,
	     id,
	     null,
	     0,     
	     0,
	     null,
	     new TreeSet(),
	     0,
	     0,
	     0F,
	     0F);
    }

    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append("RegularTokenMessage = {");
	buf.append("\n          magic = ");
	buf.append(magic);
	buf.append("\n           type = ");
	buf.append(type);
	buf.append("\n         configurationId = ");
	buf.append(configurationId);
	buf.append("\n             id = ");  
	buf.append(id);
	buf.append("\n    destination = ");  
	buf.append(destination);
	buf.append("\n   maxMessageId = ");  
	buf.append(maxMessageId);
	buf.append("\n   lowMessageId = ");  
	buf.append(lowMessageId);
	buf.append("\n         missed = ");  
	buf.append(missed);
	buf.append("\n totalBroadcast = ");  
	buf.append(totalBroadcast);
	buf.append("\n   totalBacklog = ");  
	buf.append(totalBacklog);
	buf.append("\n         window = ");  
	buf.append(window);
	buf.append("\n      threshold = ");  
	buf.append(threshold);
	buf.append("\n");  
	return buf.toString();
    }

    /**
     * This method is called by SRPConnection.
     */
    public void execute(SRPConnection conn, SRPState state) {
	Processor processor = conn.getProcessor();
	if (destination.equals(processor)) {
	    //received regular token
	    if (DEBUG) conn.log("Received regular token " + this);
	    state.regularTokenReceived(this);
	    //send up diagnostic information
	    SRPTokenAlert alert = new SRPTokenAlert(lowMessageId,
						    (int) window,
						    missed.size());
	    conn.getListener().onAlert(alert);
	}
    }

    public boolean equals(Object object) {
	RegularTokenMessage m1 = this;
	RegularTokenMessage m2 = (RegularTokenMessage) object;
	boolean b =
	    m1.getMagic() == m2.getMagic() &&
	    m1.getType() == m2.getType() &&
	    m1.getConfigurationId() == m2.getConfigurationId() &&
	    m1.getId() == m2.getId() &&
	    m1.getDestination().equals(m2.getDestination()) &&
	    m1.getMaxMessageId() == m2.getMaxMessageId() &&
	    m1.getLowMessageId() == m2.getLowMessageId() &&
	    m1.getSlowProcessor().equals(m2.getSlowProcessor()) &&
	    m1.getMissed().equals(m2.getMissed()) &&
	    m1.getTotalBroadcast() == m2.getTotalBroadcast() &&
	    m1.getTotalBacklog() == m2.getTotalBacklog() &&
	    m1.getWindow() == m2.getWindow() &&
	    m1.getThreshold() == m2.getThreshold();
	return b;
    }

}
