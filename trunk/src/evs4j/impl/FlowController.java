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

import evs4j.impl.message.RegularTokenMessage;

public class FlowController {

    /**
     * The number of processors in this configuration.
     */
    private int processorCount;

    /**
     * The number of new messages waiting to be
     * broadcast by this processor when it forwarded the
     * token on the previous rotation.
     */    
    private int previousBacklog;
    
    public int getPreviousBacklog() {
	return previousBacklog;
    }

    /**
     * Number of messages broadcast by this processor
     * when it last received the token.
     */
    private int previousBroadcast;

    public int getPreviousBroadcast() {
	return previousBroadcast;
    }
    
    public FlowController(int processorCount) {
	this.processorCount = processorCount;
    }
    
    /**
     * Updates the flow control parameters for this 
     * processor and on the token and returns the
     * number of messages this processor should
     * broadcast this time around.
     */
    public int update(int backlog, RegularTokenMessage token) {
	//add retransmission to backlog
	backlog += token.getMissed().size();
	//get parameters from last rotation
	int totalBroadcast = token.getTotalBroadcast();
	int totalBacklog = token.getTotalBacklog();
	//get current window
	int window = (int) token.getWindow();
	//compute and apply bounds
	int bound1 = window / processorCount;
	if (bound1 < 1) {
	    bound1 = 1;
	}
	int bound2 = window - totalBroadcast;
	if (bound2 < 0) {
	    bound2 = 0;
	}
	int bound3 = 0;
	int nextTotalBacklog = totalBacklog + backlog - previousBacklog;
	if (nextTotalBacklog < 0) {
	    nextTotalBacklog = 0;
	}
	if (nextTotalBacklog > 0) {
	    bound3 = (int) (((float) (window * backlog)) / 
			    ((float) nextTotalBacklog));
	}
	//'count' must be less than all three bounds
	int count = bound1;
	if (bound2 > 0 && bound2 < count) {
	    count = bound2;
	}
	if (bound3 > 0 && bound3 < count) {
	    count = bound3;
	}
	//do not return a number greater
	//than the backlog
	if (backlog < count) {
	    count = backlog;
	}
	//update state and token
	previousBacklog = backlog;
	int nextTotalBroadcast = totalBroadcast + count - previousBroadcast;
	if (nextTotalBroadcast < 0) {
	    nextTotalBroadcast = 0;
	}
	token.setTotalBroadcast(nextTotalBroadcast);
	token.setTotalBacklog(nextTotalBacklog);
	previousBroadcast = count;
	return count;
    }

}
