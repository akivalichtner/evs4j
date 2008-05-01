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

/**
 * This class is used by the coordinator to adjust the window
 * at every token rotation. It uses a Jacobson-style
 * congestion avoidance and control algorithm.
 */

public class WindowController {

    private float maxWindowSize;

    public WindowController(float maxWindowSize) {
	this.maxWindowSize = maxWindowSize;
    }

    public static final float STARTING_WINDOW = 1F;
    public static final float WINDOW_ADVANCE = 1F;
    public static final float WINDOW_REDUCE_FACTOR = 0.5F;
    
    public void update(RegularTokenMessage token) {
	float threshold = token.getThreshold();
	float window = token.getWindow();
	if (window < STARTING_WINDOW) {
	    window = STARTING_WINDOW;
	}
	//decide if the network is congested
	//works well in testing
	int maxMessageId = token.getMaxMessageId();
	int lowMessageId = token.getLowMessageId();
	boolean congested = maxMessageId > lowMessageId;
	if (congested) {
	    threshold = window * WINDOW_REDUCE_FACTOR;
	    window = STARTING_WINDOW;
	} else {
	    if (window > maxWindowSize) {
		//enforce administrator limit on window size
		//to keep latency down
		window = maxWindowSize;
	    } else if (window < threshold) {
		window += WINDOW_ADVANCE;
	    } else {
		window += 1F / window;
	    }
	}
	token.setWindow(window);
	token.setThreshold(threshold);
    }

}
