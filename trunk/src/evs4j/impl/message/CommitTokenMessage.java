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

import java.net.DatagramPacket;
import evs4j.Processor;
import evs4j.impl.ProcessorSet;
import evs4j.impl.SRPConnection;
import evs4j.impl.SRPState;

public class CommitTokenMessage extends TokenMessage {
    
    public static class CommitInfo {
	
	private Processor processor;
	
	public Processor getProcessor() {
	    return processor;
	}
	
	/**
	 * The processor's old configuration id.
	 */
	private long configurationId;
	
	public long getConfigurationId() {
	    return configurationId;
	}
	
	public void setConfigurationId(long configurationId) {
	    this.configurationId = configurationId;
	}
	
	/**
	 * The processor's lowMessageId on
	 * the old configuration.
	 */
	private int lowMessageId;
	
	public int getLowMessageId() {
	    return lowMessageId;
	}
	
	public void setLowMessageId(int lowMessageId) {
	    this.lowMessageId = lowMessageId;
	}
	
	public CommitInfo(Processor processor, 
			  long configurationId,
			  int lowMessageId) {
	    this.processor = processor;
	    this.configurationId = configurationId;
	    this.lowMessageId = lowMessageId;
	}
	
	public CommitInfo(Processor processor) {
	    this(processor,
		 0L,
		 0);
	}
	
	public String toString() {
	    StringBuffer buf = new StringBuffer();
	    buf.append("CommitInfo = {");
	    buf.append("\n      processor = ");  
	    buf.append(processor);
	    buf.append("\n        configurationId = ");
	    buf.append(configurationId);
	    buf.append("\n  lowMessageId = ");
	    buf.append(lowMessageId);
	    buf.append("\n}");
	    return buf.toString();
	}
	
	public boolean equals(Object object) {
	    //do not catch ClassCastException
	    CommitInfo i1 = this;
	    CommitInfo i2 = (CommitInfo) object;
	    boolean b = 
		i1.getProcessor().equals(i2.getProcessor()) &&
		i1.getConfigurationId() == i2.getConfigurationId() &&
		i1.getLowMessageId() == i2.getLowMessageId();
	    return b;
	}
	
    }
    
    private CommitInfo[] info;
    
    public CommitInfo[] getInfo() {
	return info;
    }
    
    /**
     * Returns the CommitInfo object for <em>processor</em>.
     */
    public CommitInfo getCommitInfo(Processor processor) {
	CommitInfo r = null;
	for (int i = 0; i<info.length; i++) {
	    CommitInfo tmp = info[i];
	    if (processor.equals(tmp.getProcessor())) {
		r = tmp;
		break;
	    }
	}
	return r;
    }
    
    public CommitTokenMessage(int magic,
			      Buffer buffer,
			      long configurationId,
			      int id,
			      Processor destination,
			      CommitInfo[] info) {
	super(magic,
	      Message.TYPE_COMMIT_TOKEN,
	      buffer,
	      configurationId,
	      id,
	      destination);
	this.info = info;
    }

    public CommitTokenMessage(Buffer buffer,
			      long configurationId,
			      int id,
			      CommitInfo[] info) {
	this(MAGIC_NUMBER,
	     buffer,
	     configurationId,
	     id,
	     null,
	     info);
    }
    
    /**
     * Returns the set of Processor objects in the info
     * array in the form of a ProcessorSet.
     */
    public ProcessorSet getProcessors() {
	ProcessorSet processors = new ProcessorSet();
	for (int i=0; i<info.length; i++) {
	    processors.add(info[i].getProcessor());
	}
	return processors;
    }

    /**
     * Returns the ProcessorSet of the processors listed
     * on this token whose old configuration was <em>configurationId</em>.
     */
    public ProcessorSet getTransProcessors(long configurationId) {
	ProcessorSet transProcessors = new ProcessorSet();
	for (int i = 0; i<info.length; i++) {
	    CommitInfo tmp = info[i];
	    if (tmp.getConfigurationId() == configurationId) {
		transProcessors.add(tmp.getProcessor());
	    }
	}
	return transProcessors;
    }

    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append("CommitTokenMessage = {");
	buf.append("\n        magic = ");
	buf.append(magic);
	buf.append("\n         type = ");
	buf.append(type);
	buf.append("\n       configurationId = ");
	buf.append(configurationId);
	buf.append("\n           id = ");  
	buf.append(id);
	buf.append("\n  destination = ");
	buf.append(destination);
	buf.append("\n         info = ");
	for (int i = 0; i < info.length; i++) {
	    buf.append('\n');
	    buf.append(info[i]);
	}
	buf.append("\n}");
	return buf.toString();
    }

    /**
     * This method is called by SRPConnection.
     */
    public void execute(SRPConnection conn, SRPState state) {
	Processor processor = conn.getProcessor();
	if (processor.equals(destination)) {
	    //got the commit token
	    if (DEBUG) conn.log("Received commit token " + this);
	    state.commitTokenReceived(this);
	}
    }

    public boolean equals(Object object) {
	CommitTokenMessage m1 = this;
	CommitTokenMessage m2 = (CommitTokenMessage) object;
	boolean b =
	    m1.getMagic() == m2.getMagic() &&
	    m1.getType() == m2.getType() &&
	    m1.getConfigurationId() == m2.getConfigurationId() &&
	    m1.getId() == m2.getId() &&
	    m1.getDestination() == m2.getDestination();
	//compare commit info in tokens
	CommitInfo[] a1 = m1.getInfo();
	CommitInfo[] a2 = m2.getInfo();
	if (a1.length == a2.length) {
	    for (int i = 0; i < a1.length; i++) {
		b = b && a1[i].equals(a2[i]);
	    }
	} else {
	    b = false;
	}
	return b;
    }

}

