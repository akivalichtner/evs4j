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
import evs4j.impl.message.RegularTokenMessage;

/**
 * This class is used to detect if a processor
 * has stopped receiving messages (but is still
 * forwarding the token.)
 */

public class ProgressTracker {

    /**
     * Equals the number of rotations that the lowMessageId
     * has to stay constant for the processor to determine that
     * the slow processor has failed.
     */
    private int maxRotations;

    public ProgressTracker(int maxRotations) {
	this.maxRotations = maxRotations;
	reset();
    }

    public void reset() {
	previousId = 0;
	rotations = 0;
    }

    /**
     * Equals the value of the token's lowMessageId 
     * on the previous rotation.
     */
    private int previousId;

    /**
     * The slow processor on the previous rotation.
     */
    private Processor previousSlowProcessor;

    /**
     * Equals the number of times that the processor
     * has received a token with an unchanged lowMessageId 
     * and with a lowMessageId not equal to maxMessageId.
     * Used to determine if a processor is consistently
     * failing to receive a message.
     */
    private int rotations;

    /**
     * Updates the state of the object and checks if 
     * the lowMessageId of the slowest processor has
     * stayed the same for too many token rotations.
     */
    public void update(RegularTokenMessage token, 
		       Processor processor,
		       int lowMessageId,
		       int maxMessageId) throws NoProgressException {
	int tokenLowMessageId = token.getLowMessageId();
	Processor slowProcessor = token.getSlowProcessor();
	if (lowMessageId < tokenLowMessageId ||
	    slowProcessor == null ||
	    processor.equals(slowProcessor)) {
	    tokenLowMessageId = lowMessageId;
	    if (tokenLowMessageId == maxMessageId) {
		slowProcessor = null;
	    } else {
		slowProcessor = processor;
	    }
	}
	if (tokenLowMessageId == previousId &&
	    slowProcessor != null &&
	    previousSlowProcessor != null &&
	    slowProcessor.equals(previousSlowProcessor)) {
	    rotations++;
	} else {
	    rotations = 0;
	}
	previousId = tokenLowMessageId;
	previousSlowProcessor = slowProcessor;
	token.setLowMessageId(tokenLowMessageId);
	token.setSlowProcessor(slowProcessor);
	if (rotations > maxRotations) {
	    throw new NoProgressException(slowProcessor);
	}
    }

}
