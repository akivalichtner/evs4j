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
import evs4j.impl.CompileTimeMacro;
import evs4j.impl.SRPConnection;
import evs4j.impl.SRPState;

public class RegularMessage extends Message implements evs4j.Message {

    /**
     * The Processor object of the processor that
     * initiated the message.
     */
    private Processor sender;

    public Processor getSender() {
	return sender;
    }
    
    /**
     * The configuration id of the configuration on which the message
     * was originated.
     */
    private long configurationId;

    public long getConfigurationId() {
	return configurationId;
    }

    public void setConfigurationId(long configurationId) {
	this.configurationId = configurationId;
    }

    /**
     * The message sequence number for this message.
     */
    private int id;

    public int getId() {
	return id;
    }

    public void setId(int id) {
	this.id = id;
    }

    /**
     * <em>true</em> iff the payload of this RegularMessage
     * itself contains a RegularMessage re-sent from a previous
     * configuration.
     */
    private boolean recovered;

    public boolean getRecovered() {
	return recovered;
    }

    public void setRecovered(boolean recovered) {
	this.recovered = recovered;
    }

    /**
     * <em>true</em> iff this message should be
     * delivered as safe (that is after being
     * acknowledged by all processors in the configuration).
     */
    private boolean safe;

    public boolean getSafe() {
	return safe;
    }

    /**
     * The length of the payload data.
     */
    private int length;
    
    public int getLength() {
	return length;
    }

    public void setLength(int length) {
	this.length = length;
    }
    
    /**
     * The maximum size of the payload, as defined
     * in the protocol specification.
     */
    public static final int MAX_PAYLOAD_SIZE = 1450;

    /**
     * The total length of the headers (includes the length
     * of the payload array).
     */
    public static final int HEADER_LENGTH = 25;
    
    public RegularMessage(int magic,
			  Buffer buffer,
			  Processor sender,
			  long configurationId,
			  int id,
			  boolean recovered,
			  boolean safe,
			  int length) {
	super(magic, TYPE_REGULAR_MESSAGE, buffer);
	this.sender = sender;
	this.configurationId = configurationId;
	this.id = id;
	this.recovered = recovered;
	this.safe = safe;
	this.length = length;
    }

    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append("RegularMessage = {");
	buf.append("\n     magic = ");
	buf.append(magic);
	buf.append("\n      type = ");
	buf.append(type);
	buf.append("\n    sender = ");
	buf.append(sender);
	buf.append("\n    configurationId = ");
	buf.append(configurationId);
	buf.append("\n        id = ");
	buf.append(id);
	buf.append("\n recovered = ");
	buf.append(recovered);
	buf.append("\n      safe = ");
	buf.append(safe);
	buf.append("\n      length = ");
	buf.append(length);
	buf.append("\n}");
	return buf.toString();
    }

    /**
     * Returns true iff the message was or will be
     * delivered as safe.
     */
    public boolean isSafe() {
	return safe;
    }

    /**
     * Returns the buffer containing the data.
     */
    public byte[] getData() {
	return buffer.getData();
    }

    /**
     * Returns the index of the first byte of the
     * message (usually the beginning of the first 
     * protocol header).
     */
    public int getOffset() {
	return HEADER_LENGTH;
    }

    /**
     * This method is called by SRPConnection.
     */
    public void execute(SRPConnection conn, SRPState state) {
	Processor processor = conn.getProcessor();
	//ignore your own messages
	if (!processor.equals(sender)) {
	    if (conn.getConfigurationId() == configurationId) {
		state.regularMessageReceived(this);
	    } else {
		//got a message from outside
		//the installed configuration
		if (DEBUG) conn.log("Received foreign message");
		state.foreignMessageReceived(this);
	    }
	}
    }

    public boolean equals(Object object) {
	//do not catch ClassCastException
	RegularMessage m1 = this;
	RegularMessage m2 = (RegularMessage) object;
	boolean b = 
	    m1.getMagic() == m2.getMagic() &&
	    m1.getType() == m2.getType() &&
	    m1.getSender().equals(m2.getSender()) &&
	    m1.getConfigurationId() == m2.getConfigurationId() &&
	    m1.getId() == m2.getId() &&
	    m1.getRecovered() == m2.getRecovered() &&
	    m1.getSafe() == m2.getSafe();
	//compare message content
	int len1 = m1.getLength();
	int len2 = m2.getLength();
	if (len1 == len2) {
	    int offset1 = m1.getOffset();
	    int offset2 = m2.getOffset();
	    byte[] data1 = m1.getData();
	    byte[] data2 = m2.getData();
	    for (int i = 0; i < len1; i++) {
		b = b && data1[offset1 + i] == data2[offset2 + i];
	    }
	} else {
	    b = false;
	}
	return b;
    }

}

