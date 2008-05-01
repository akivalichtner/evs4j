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
import evs4j.impl.Queue;
import evs4j.impl.CompileTimeMacro;
import evs4j.impl.message.RegularTokenMessage;
import evs4j.impl.message.CommitTokenMessage;
import evs4j.impl.message.RegularMessage;
import evs4j.impl.message.JoinMessage;

public class SRPOperational implements SRPState, CompileTimeMacro {
    
    public SRPOperational(SRPConnection conn, 
			  SRPConfiguration configuration, 
			  Processor processor) {
	this.conn = conn;
	this.sent = conn.getSent();
	this.configuration = configuration;
	this.processor = processor;
	this.handler = configuration.getHandler();
    }

    private SRPConnection conn;

    private SRPConfiguration configuration;

    private Processor processor;

    private RegularTokenHandler handler;

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

    public void tokenLossTimeoutExpired() {
	conn.log("Shifting to GATHER because token loss timeout expired");
	conn.discover();
    }

    public void tokenDroppedTimeoutExpired() {
	conn.forwardToken();
	conn.resetTokenDroppedTimeout();
    }
    
    public void foreignMessageReceived(RegularMessage message) {
	Processor sender = message.getSender();
	conn.log("Shifting to GATHER because of foreign message");
	conn.foundProcessor(sender);
    }
    
    public void joinMessageReceived(JoinMessage message) {
	conn.gather(message);
    }

    public void regularMessageReceived(RegularMessage message) {
	conn.cancelTokenDroppedTimeout();
	configuration.receive(message);
    }

    public void commitTokenReceived(CommitTokenMessage message) {
	//discard token (done)
    }
    
    public void consensusTimeoutExpired() {
	//do nothing
    }
    
    public void joinTimeoutExpired() {
	//do nothing
    }

    public String toString() {
	return "OPERATIONAL";
    }

}
