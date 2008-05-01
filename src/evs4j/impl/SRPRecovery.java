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

import java.util.Enumeration;
import evs4j.Listener;
import evs4j.Processor;
import evs4j.Configuration;
import evs4j.impl.Queue;
import evs4j.impl.CompileTimeMacro;
import evs4j.impl.message.Message;
import evs4j.impl.message.IllegalMessageException;
import evs4j.impl.message.RegularTokenMessage;
import evs4j.impl.message.CommitTokenMessage;
import evs4j.impl.message.CommitTokenMessage.CommitInfo;
import evs4j.impl.message.RegularMessage;
import evs4j.impl.message.JoinMessage;
import evs4j.impl.message.MessageReader;
import evs4j.impl.message.Buffer;

public class SRPRecovery implements SRPState, CompileTimeMacro {

    public SRPRecovery(SRPConnection conn,
		       SRPConfiguration previousConfiguration,
		       CommitTokenMessage token,
		       Processor processor) {
	this.conn = conn;
	ProcessorSet nextProcessors = token.getProcessors();
	conn.forwardToken(token, nextProcessors.getNextProcessor(processor));
	this.processor = processor;
	this.previousConfiguration = previousConfiguration;
	this.previousHandler = previousConfiguration.getHandler();
	long nextConfigurationId = token.getConfigurationId();
	boolean transitional = false;
	this.nextConfiguration = new SRPConfiguration(conn, nextProcessors, nextConfigurationId, transitional);
	this.sent = new Queue();
	//will switch back if we have a failure
	conn.install(this.nextConfiguration);
	this.nextHandler = nextConfiguration.getHandler();
	int minLowMessageId = 0;
	CommitInfo[] list = token.getInfo();
	for (int i=0; i<list.length; i++) {
	    CommitInfo tmp = list[i];
	    int infoLowMessageId = tmp.getLowMessageId();
	    if (i == 0) {
		minLowMessageId = infoLowMessageId;
	    } else {
		if (infoLowMessageId < minLowMessageId) {
		    minLowMessageId = infoLowMessageId;
		}
	    }
	    //copy old messages to be resent
	    //on the next configuration
	    ReceivedList received = previousConfiguration.getReceived();
	    Enumeration enumeration = received.getMessages();
	    while (enumeration.hasMoreElements()) {
		RegularMessage m = (RegularMessage) enumeration.nextElement();
		if (m.getId() > minLowMessageId) {
		    //wrap recovered message
		    //and send it in agreed order
		    boolean safe = false;
		    RegularMessage wrapper = (RegularMessage) conn.createMessage(safe);
		    Buffer buffer = m.getBuffer();
		    int length = buffer.getLength();
		    System.arraycopy(buffer.getData(), 0,
				     wrapper.getData(), wrapper.getOffset(),
				     length);
		    wrapper.setLength(length);
		    wrapper.setRecovered(true);
		    sent.add(wrapper);
		}
	    }
	}
	conn.resetTokenLossTimeout();
	conn.resetTokenDroppedTimeout();
    }

    private SRPConnection conn;

    /**
     * This Processor object for this processor.
     */
    private Processor processor;

    /**
     * The configuration we are trying to replace.
     */
    private SRPConfiguration previousConfiguration;

    private RegularTokenHandler previousHandler;

    /**
     * The configuration we are trying to install.
     */    
    private SRPConfiguration nextConfiguration;

    private RegularTokenHandler nextHandler;

    /**
     * The ProcessorSet of the processors transitioning
     * from this processors's old configuration to the next one.
     */
    private ProcessorSet transProcessors;

    /**
     * The number of successive token rotations in which 
     * this processor has received the token with 
     * <em>backlog</em> equal to zero.
     */
    private int backlogCount;

    private Queue sent;

    public void regularTokenReceived(RegularTokenMessage token) {
	RegularTokenMessage nextToken = null;
	try {
	    nextToken = nextHandler.handle(token, sent);
	} catch (NoProgressException e) {
	    Processor processor = e.getProcessor();
	    if (DEBUG) conn.log("Processor " + processor + " failed");
	    conn.lostProcessor(processor);
	    return;
	} catch (TokenResetException e) {
	    conn.log("Token id limit reached");
	    conn.reset();
	    return;
	} catch (MessageResetException e) {
	    conn.log("Message id limit reached");
	    conn.reset();
	    return;
	}
	if (nextToken != null) {
	    int backlog = token.getTotalBacklog();
	    if (DEBUG) conn.log("Configuration backlog is " + backlog);
	    if (backlog == 0) {
		backlogCount++;
	    } else {
		backlogCount = 0;
	    }
	    if (DEBUG) conn.log("backlogCount = " + backlogCount);
	    boolean ready = false;
	    if (backlogCount == 2) {
		//deliver first configuration change message
		//sequence number of transitional configuration equals
		//two less than the sequence number of the configuration
		//we are installing
		long nextConfigurationId = nextConfiguration.getId();
		long transConfigurationId = 
		    SRPConfiguration.toConfigurationId(SRPConfiguration.getConfigurationProcessor(nextConfigurationId),
						       SRPConfiguration.getConfigurationNumber(nextConfigurationId) - 2);
		Listener listener = conn.getListener();
		ProcessorSet previous = previousConfiguration.getProcessorSet();
		ProcessorSet next = nextConfiguration.getProcessorSet();
		ProcessorSet transProcessors = previous.intersect(next);
		ReceivedList previousReceived = previousConfiguration.getReceived();
		SRPConfiguration transConfiguration =
		    new SRPConfiguration(conn, transProcessors, transConfigurationId, true);
		listener.onConfiguration(transConfiguration);
		//deliver messages that could not be delivered in 
		//the previous configuration (because the gaps
		//spoil the agreed and safe order within the old
		//membership)
		Enumeration messages = previousReceived.getMessages();
		while (messages.hasMoreElements()) {
		    RegularMessage m = (RegularMessage) messages.nextElement();
		    //deliver only messages from transProcessors
		    //because of causality
		    if (transProcessors.contains(m.getSender())) {
			listener.onMessage(m);
		    }
		}
		//deliver second configuration change message
		ProcessorSet nextProcessors = nextConfiguration.getProcessorSet();
		SRPConfiguration nextConfiguration = new SRPConfiguration(conn, nextProcessors, nextConfigurationId, false);
		listener.onConfiguration(nextConfiguration);
		ready = true;
	    }
	    Processor nextProcessor = nextConfiguration.getNextProcessor(processor);
	    conn.forwardToken(token, nextProcessor);
	    conn.resetTokenLossTimeout();
	    conn.resetTokenDroppedTimeout();
	    if (ready) {
		conn.shiftToOperational();
	    }
	}
    }

    public void regularMessageReceived(RegularMessage message) {
	conn.cancelTokenDroppedTimeout();
	nextConfiguration.receive(message);
	if (message.getRecovered()) {
	    //in Receiver we cancel the token retransmission timeout
	    //and add the message to the last installed configuration
	    //for all (recovered and non-recovered) regular messages
	    RegularMessage inner = null;
	    //get a fresh Buffer, and copy the other one
	    //into it (we use a copy because a Buffer must be
	    //associated with only one Message object)
	    Buffer buffer = message.getBuffer().copy();
	    MessageReader reader = new MessageReader();
	    try {
		inner = (RegularMessage) reader.readMessage(buffer);
	    } catch (IllegalMessageException e) {
		e.printStackTrace();
	    }
	    if (inner.getConfigurationId() == previousConfiguration.getId()) {
		previousConfiguration.receive(inner);
	    }
	}
    }

    public void foreignMessageReceived(RegularMessage m) {
	//do nothing
    }

    public void joinMessageReceived(JoinMessage message) {
	Processor sender = message.getSender();
	int messageConfigurationNumber = message.getMaxConfigurationNumber();
	long nextConfigurationId = nextConfiguration.getId();
	int nextConfigurationNumber = SRPConfiguration.getConfigurationNumber(nextConfigurationId);
	ProcessorSet nextProcessors = nextConfiguration.getProcessorSet();
	if (nextProcessors.contains(sender) && 
	    messageConfigurationNumber >= nextConfigurationNumber) {
	    conn.install(previousConfiguration);
	    conn.gather(message);
	}
    }

    public void commitTokenReceived(CommitTokenMessage commitToken) {
	ProcessorSet processors = commitToken.getProcessors();
	long tokenConfigurationId = commitToken.getConfigurationId();
	long nextConfigurationId = nextConfiguration.getId();
	if (tokenConfigurationId == nextConfigurationId) {
	    if (processors.getCoordinator().equals(processor)) {
		Buffer buffer = new Buffer(0);
		RegularTokenMessage regularToken = null;
		regularToken = new RegularTokenMessage(buffer,
						       commitToken.getConfigurationId(),
						       commitToken.getId());
		Processor nextProcessor = processors.getNextProcessor(processor);
		conn.forwardToken(regularToken, nextProcessor);
		conn.resetTokenDroppedTimeout();
		conn.resetTokenLossTimeout();
	    }
	}
    }

    public void tokenLossTimeoutExpired() {
	conn.install(previousConfiguration);
	conn.discover();
    }

    public void tokenDroppedTimeoutExpired() {
	conn.forwardToken();
	conn.resetTokenDroppedTimeout();
    }

    public void consensusTimeoutExpired() {
	//do nothing
    }

    public void joinTimeoutExpired() {
	//do nothing
    }

    public String toString() {
	return "RECOVERY";
    }

}
