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

import evs4j.Processor;
import evs4j.impl.CompileTimeMacro;
import evs4j.impl.message.RegularTokenMessage;
import evs4j.impl.message.CommitTokenMessage;
import evs4j.impl.message.CommitTokenMessage.CommitInfo;
import evs4j.impl.message.RegularMessage;
import evs4j.impl.message.JoinMessage;

public class SRPCommit implements SRPState, CompileTimeMacro {

    public SRPCommit(SRPConnection conn,
		     SRPConfiguration configuration,
		     CommitTokenMessage token,
		     Processor processor) {
	this.conn = conn;
	this.configuration = configuration;
	this.processor = processor;
	this.nextProcessors = token.getProcessors();
	this.nextConfigurationId = token.getConfigurationId();
	CommitInfo info = token.getCommitInfo(processor);
	info.setConfigurationId(configuration.getId());
	info.setLowMessageId(configuration.getLowMessageId());
	conn.forwardToken(token, nextProcessors.getNextProcessor(processor));
	conn.cancelJoinTimeout();
	conn.cancelConsensusTimeout();
	conn.resetTokenLossTimeout();
	conn.resetTokenDroppedTimeout();
    }

    private SRPConnection conn;

    private SRPConfiguration configuration;

    private Processor processor;

    public void regularTokenReceived(RegularTokenMessage message) {
	//discard (do nothing)
    }

    public void foreignMessageReceived(RegularMessage message) {
	//discard message (done)
    }

    /**
     * The configuration id from the most recently
     * accepted commit token.
     */    
    private long nextConfigurationId;

    /**
     * The ProcessorSet of the processors on
     * the new configuration.
     */
    private ProcessorSet nextProcessors;
    
    public void joinMessageReceived(JoinMessage message) {
	Processor sender = message.getSender();
	int messageConfigurationNumber = message.getMaxConfigurationNumber();
	int nextConfigurationNumber = SRPConfiguration.getConfigurationNumber(nextConfigurationId);
	if (nextProcessors.contains(sender) && 
	    messageConfigurationNumber >= nextConfigurationNumber) {
	    conn.gather(message);
	}
    }
    
    public void commitTokenReceived(CommitTokenMessage token) {
	long tokenConfigurationId = token.getConfigurationId();
	if (tokenConfigurationId == nextConfigurationId) {
	    conn.shiftToRecovery(token);
	}
    }
    
    public void joinTimeoutExpired() {
	conn.broadcastJoinMessage();
	conn.resetJoinTimeout();
    }

    public void tokenLossTimeoutExpired() {
	conn.discover();
    }

    public void regularMessageReceived(RegularMessage message) {
	conn.cancelTokenDroppedTimeout();
	configuration.receive(message);
    }

    public void tokenDroppedTimeoutExpired() {
	conn.forwardToken();
	conn.resetTokenDroppedTimeout();
    }
    
    public void consensusTimeoutExpired() {
	//do nothing
    }

    public String toString() {
	return "COMMIT";
    }

}
