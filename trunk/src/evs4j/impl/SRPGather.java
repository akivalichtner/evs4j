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
import java.net.InetAddress;
import evs4j.Processor;
import evs4j.impl.Queue;
import evs4j.impl.CompileTimeMacro;
import evs4j.impl.message.Buffer;
import evs4j.impl.message.RegularTokenMessage;
import evs4j.impl.message.CommitTokenMessage;
import evs4j.impl.message.CommitTokenMessage.CommitInfo;
import evs4j.impl.message.RegularMessage;
import evs4j.impl.message.JoinMessage;

public class SRPGather implements SRPState, CompileTimeMacro {

    public SRPGather(SRPConnection conn, 
		     SRPConfiguration configuration,
		     Processor processor) {
	this.conn = conn;
	this.sent = conn.getSent();
	this.handler = configuration.getHandler();
	this.configuration = configuration;
	this.configurationId = configuration.getId();
	this.processor = processor;
	this.candidates = configuration.getProcessorSet().copy();
	this.failed = new ProcessorSet();
	int maxConfigurationNumber = SRPConfiguration.getConfigurationNumber(configurationId);
	this.consensus = new Consensus(processor, maxConfigurationNumber);
    }
    
    private SRPConnection conn;

    private RegularTokenHandler handler;

    private SRPConfiguration configuration;
   
    private long configurationId;

    private Consensus consensus;

    /**
     * The ProcessorSet of the processors
     * that this processor is considering for
     * membership on a new configuration.
     */
    private ProcessorSet candidates;

    public void addCandidate(Processor processor) {
	candidates.add(processor);
    }

    /**
     * The ProcessorSet of the processors
     * that this processor has determined to
     * have failed.
     */
    private ProcessorSet failed;

    public void addFailed(Processor processor) {
	failed.add(processor);
    }

    /**
     * The Processor object for this processor.
     */
    private Processor processor;

    public void discover() {
	int maxConfigurationNumber = SRPConfiguration.getConfigurationNumber(configurationId);
	Buffer buffer = new Buffer(0);
	JoinMessage message = new JoinMessage(buffer,
					      processor,
					      candidates,
					      failed,
					      maxConfigurationNumber);
	conn.broadcastJoinMessage(message);
	conn.cancelTokenDroppedTimeout();
	conn.cancelTokenLossTimeout();
	conn.resetJoinTimeout();
	conn.resetConsensusTimeout();
    }

    private Queue sent;

    public void regularTokenReceived(RegularTokenMessage token) {
	RegularTokenMessage nextToken = null;
	try {
	    nextToken = handler.handle(token, sent);
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
	    Processor nextProcessor = configuration.getNextProcessor(processor);
	    conn.forwardToken(token, nextProcessor);
	    conn.resetTokenLossTimeout();
	    conn.resetTokenDroppedTimeout();
	}
    }    

    public void foreignMessageReceived(RegularMessage message) {
	Processor sender = message.getSender();
	if (!candidates.contains(sender)) {
	    if (DEBUG) conn.log("Foreign message is from: " + 
				message.getConfigurationId());
	    conn.foundProcessor(sender);
	}
    }

    public void joinMessageReceived(JoinMessage message) {
	if (gather(message)) {
	    discover();
	}
    }

    /**
     * Absorbs the candidate processors and failed processors
     * and return true if the sets contain processors we do not
     * already know to be alive or dead.
     */
    public boolean gather(JoinMessage message) {
	boolean broadcast = false;
	Processor sender = message.getSender();
	ProcessorSet messageCandidates = message.getCandidates();
	ProcessorSet messageFailed = message.getFailed();
	if (messageCandidates.equals(candidates) &&
	    messageFailed.equals(failed)) {
	    consensus.add(sender, message.getMaxConfigurationNumber());
	    ProcessorSet diff = candidates.minus(failed);
	    boolean all = consensus.check(diff);
	    if (all && !diff.isEmpty() && processor.equals(diff.getCoordinator())) {
		//generate id for next configuration
		//add 4 to max
		int nextConfigurationNumber = consensus.getMaxConfigurationNumber() + 4;
		//build processor info array
		CommitInfo[] info = diff.getInfoArray();
		CommitTokenMessage token;
		long nextConfigurationId = SRPConfiguration.toConfigurationId(processor.getValue(), nextConfigurationNumber);
		int tokenId = 1;
		Buffer buffer = new Buffer(0);
		token = new CommitTokenMessage(buffer,
					       nextConfigurationId,
					       tokenId,
					       info);
		conn.shiftToCommit(token);
	    }
	} else if (candidates.contains(messageCandidates) &&
		   failed.contains(messageFailed)) {
	    //go to end of method
	} else if (failed.contains(sender)) {
	    //go to end of method
	} else {
	    candidates.add(messageCandidates);
	    if (messageFailed.contains(processor)) {
		failed.add(sender);
	    } else {
		failed.add(messageFailed);
	    }
	    broadcast = true;
	}
	return broadcast;
    }

    public void commitTokenReceived(CommitTokenMessage token) {
	ProcessorSet diff = candidates.minus(failed);
	ProcessorSet tokenProcessors = token.getProcessors();
	//check that the token is starting a configuration
	//that comes after the configuration we know
	int configurationNumber = SRPConfiguration.getConfigurationNumber(configurationId);
	int tokenConfigurationNumber = SRPConfiguration.getConfigurationNumber(token.getConfigurationId());
	if (diff.equals(tokenProcessors) && 
	    tokenConfigurationNumber > configurationNumber) {
	    conn.shiftToCommit(token);
	}
    }

    public void joinTimeoutExpired() {
	conn.broadcastJoinMessage();
	conn.resetJoinTimeout();
    }

    public void consensusTimeoutExpired() {
	ProcessorSet diff = candidates.minus(failed);
	if (!consensus.check(diff)) {
	    Iterator iterator = diff.iterator();
	    while (iterator.hasNext()) {
		Processor tmp = (Processor) iterator.next();
		if (!consensus.get(tmp)) {
		    failed.add(tmp);
		}
	    }
	    discover();
	} else {
	    Iterator iterator = diff.iterator();
	    while (iterator.hasNext()) {
		Processor tmp = (Processor) iterator.next();
		consensus.remove(tmp);
	    }
	    consensus.add(processor, SRPConfiguration.getConfigurationNumber(configurationId));
	    conn.resetTokenLossTimeout();
	}
    }
    
    public void tokenLossTimeoutExpired() {
	consensusTimeoutExpired();
	discover();
    }

    public void regularMessageReceived(RegularMessage message) {
	conn.cancelTokenDroppedTimeout();
	configuration.receive(message);
    }

    public void tokenDroppedTimeoutExpired() {
	conn.forwardToken();
	conn.resetTokenDroppedTimeout();
    }

    public String toString() {
	return "GATHER";
    }

}


