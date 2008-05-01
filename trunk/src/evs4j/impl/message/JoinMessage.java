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

import evs4j.Processor;
import evs4j.impl.ProcessorSet;
import evs4j.impl.SRPConnection;
import evs4j.impl.SRPState;

public class JoinMessage extends Message {

    /**
     * Returns the Processor object of the processor
     * that initiated the message.
     */
    private Processor sender;
    
    public Processor getSender() {
	return sender;
    }
    
    /**
     * The Processor objects of the processors
     * that the sender is considering for
     * membership in the new configuration.
     */
    private ProcessorSet candidates;

    public ProcessorSet getCandidates() {
	return candidates;
    }

    /**
     * The Processor objects of the processors
     * that the sender has determined to have failed.
     */
    private ProcessorSet failed;

    public ProcessorSet getFailed() {
	return failed;
    }

    /**
     * The largest configuration sequence number
     * of a configuration id known to the sender.
     */
    private int maxConfigurationNumber;
    
    public int getMaxConfigurationNumber() {
	return maxConfigurationNumber;
    }

    public JoinMessage(int magic,
		       Buffer buffer,
		       Processor sender,
		       ProcessorSet candidates,
		       ProcessorSet failed,
		       int maxConfigurationNumber) {
	super(magic, TYPE_JOIN_MESSAGE, buffer);
	this.sender = sender;
	this.candidates = candidates;
	this.failed = failed;
	this.maxConfigurationNumber = maxConfigurationNumber;
    }

    public JoinMessage(Buffer buffer,
		       Processor sender,
		       ProcessorSet candidates,
		       ProcessorSet failed,
		       int maxConfigurationNumber) {
	this(MAGIC_NUMBER,
	     buffer,
	     sender,
	     candidates,
	     failed,
	     maxConfigurationNumber);
    }

    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append("JoinMessage = {");
	buf.append("\n         magic = ");
	buf.append(magic);
	buf.append("\n          type = ");
	buf.append(type);
	buf.append("\n        sender = ");
	buf.append(sender);
	buf.append("\n    candidates = ");
	buf.append(candidates);
	buf.append("\n        failed = ");
	buf.append(failed);
	buf.append("\n maxConfigurationNumber = ");
	buf.append(maxConfigurationNumber);
	return buf.toString();
    }

    /**
     * This method is called by SRPConnection.
     */
    public void execute(SRPConnection conn, SRPState state) {
	if (DEBUG) conn.log("Received join message");
	state.joinMessageReceived(this);
    }

    public boolean equals(Object object) {
	JoinMessage m1 = this;
	JoinMessage m2 = (JoinMessage) object;
	boolean b =
	    m1.getMagic() == m2.getMagic() &&
	    m1.getType() == m2.getType() &&
	    m1.getSender().equals(m2.getSender()) &&
	    m1.getCandidates().equals(m2.getCandidates()) &&
	    m1.getFailed().equals(m2.getFailed()) &&
	    m1.getMaxConfigurationNumber() == m2.getMaxConfigurationNumber();
	return b;
    }

}



