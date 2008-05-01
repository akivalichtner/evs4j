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

import java.util.Iterator;
import java.util.Set;
import evs4j.Processor;
import evs4j.impl.Queue;
import evs4j.impl.CompileTimeMacro;
import evs4j.impl.message.RegularMessage;
import evs4j.impl.message.RegularTokenMessage;

public class RegularTokenHandler implements CompileTimeMacro {

    public static final int MESSAGE_ID_LIMIT = Integer.MAX_VALUE;
    
    public static final int TOKEN_ID_LIMIT = Integer.MAX_VALUE;
    
    public RegularTokenHandler(SRPConnection conn, SRPConfiguration configuration) {
	this.conn = conn;
	this.processor = conn.getProcessor();
	this.socket = conn.getSocketAdapter();
	this.coordinator = configuration.getCoordinator();
	this.configuration = configuration;
	this.configurationId = configuration.getId();
	int processorCount = configuration.getProcessorSet().getCount();
	this.flowController = new FlowController(processorCount);
	int windowSize = conn.getWindowSize();
	this.windowController = new WindowController((float) windowSize);
	this.progressTracker = new ProgressTracker(DEFAULT_FAIL_TO_RECEIVE);
    }

    private SRPConnection conn;

    private SocketAdapter socket;

    private SRPConfiguration configuration;

    /**
     * This Processor object for this processor.
     */
    private Processor processor;

    /**
     * The id of the coordinator.
     */
    private Processor coordinator;

    /**
     * The id of the current configuration.
     */    
    private long configurationId;

    /**
     * The object in charge of controlling the
     * number of messages this processor should
     * broadcast at each token rotation.
     */
    private FlowController flowController;

    /**
     * The object in charge of finding the optimal
     * window size given the current network conditions.
     */
    private WindowController windowController;

    /**
     * This is the default number of times that a processor
     * must fail to receive a message before we decide
     * to expel it from the configuration.
     */
    public static final int DEFAULT_FAIL_TO_RECEIVE = 1000;

    /**
     * This object is used to detect processors
     * who are consistently failing to receive
     * a message.
     */
    private ProgressTracker progressTracker;

    public void resetProgressTracker() {
	progressTracker.reset();
    }

    /**
     * The id of the last token we processed.
     */
    private int lastTokenId;

    /**
     * The lowMessageId field of the last
     * token received.
     */
    private int tokenLowMessageId;
    
    /**
     * Takes the token that just arrived and uses the
     * information therein to broadcast missed messages
     * and new messages. The Queue must contain RegularMessage 
     * objects that are ready to be broadcast, except for the 
     * message id, which is assigned by this method. The forwarding 
     * of the token to the next processor is left to the calling
     * method (the method returns the token to be forwarded). 
     * This method throws a NoProgressException if the slow
     * processor on the configuration has failed to receive a message too
     * many times.
     */
    public RegularTokenMessage handle(RegularTokenMessage token, Queue sent)
	throws NoProgressException, MessageResetException, TokenResetException {
	//id of token just received
	int tokenId = token.getId();
	if (DEBUG) conn.log("Token id: " + tokenId);
	RegularTokenMessage nextToken = null;
	if (configurationId != token.getConfigurationId()) {
	    //configuration id is wrong
	    if (DEBUG) conn.log("Received token for other configuration: " + token.getConfigurationId());
	} else if (tokenId <= lastTokenId) {
	    //or obsolete token
	    if (DEBUG) conn.log("Received obsolete token: " + 
				token.getId() + " <= " + lastTokenId);
	} else {
	    lastTokenId = tokenId;
	    //adjust window size
	    windowController.update(token);
	    //broadcast new messages using flow control
	    int backlog = sent.length();
	    int allotted = flowController.update(backlog, token);
	    if (DEBUG) conn.log("Allotted transmissions: " + allotted);
	    int retransmitted = repair(token, allotted);
	    if (DEBUG) conn.log("Retransmissions: " + retransmitted);
	    allotted = allotted - retransmitted;
	    int maxMessageId = token.getMaxMessageId();
	    if (DEBUG) conn.log("maxMessageId: " + maxMessageId);
	    ReceivedList received = configuration.getReceived();
	    for (int i=0; i<allotted; i++) {
		//do not wait for new messages
		long duration = -1;
		RegularMessage m = (RegularMessage) sent.remove(duration);
		if (m != null) {
		    if (maxMessageId < MESSAGE_ID_LIMIT) {
			maxMessageId++;
			m.setId(maxMessageId);
			m.setConfigurationId(configurationId);
			socket.send(m);
			configuration.receive(m);
		    } else {
			//no more ids
			//will make a new configuration
			throw new MessageResetException();
		    }
		} else {
		    //no more
		    break;
		}
	    }
	    //we assume that the SRPConnection object has
	    //emptied the input buffer of all its messages
	    if (DEBUG) conn.log("Received list: " + received.toString());
	    token.setMaxMessageId(maxMessageId);
	    if (DEBUG) conn.log("Token lowMessageId: " + token.getLowMessageId());
	    int lowMessageId = received.getLowMessageId();
	    if (DEBUG) conn.log("lowMessageId: " + lowMessageId);
	    //throws NoProgressException
	    progressTracker.update(token, 
				   processor,
				   lowMessageId,
				   maxMessageId);
	    if (tokenId < TOKEN_ID_LIMIT) {
		token.setId(tokenId + 1);
		received.setSafeMessageId(Math.min(this.tokenLowMessageId, token.getLowMessageId()));
		//save this for next time
		this.tokenLowMessageId = token.getLowMessageId();
		received.prune();
		nextToken = token;
	    } else {
		//token id limit reached
		//will make a new configuration
		throw new TokenResetException();
	    }
	}
	return nextToken;
    }

    /**
     * Broadcasts the requested retransmissions
     * and updates the token with the new retransmission
     * requests.
     */
    private int repair(RegularTokenMessage token, int allotted) {
	//broadcast requested retransmissions
	Set tokenMissed = token.getMissed();
	int resent = 0;
	ReceivedList received = configuration.getReceived();
	Iterator iterator = tokenMissed.iterator();
	while (iterator.hasNext()) {
	    Integer id = (Integer) iterator.next();
	    RegularMessage m = received.get(id);
	    if (m != null) {
		if (DEBUG) conn.log("Retransmitting message: " + id);
		socket.send(m);
		tokenMissed.remove(id);
		resent++;
		if (resent == allotted) {
		    break;
		}
	    }
	}
	//add our own retransmission requests
	Set missed = received.getMissed(token.getMaxMessageId());
	tokenMissed.addAll(missed);
	return resent;
    }
    
}




