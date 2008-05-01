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

package evs4j.tool.benchmark;

import evs4j.Alert;

/**
 * Used internally by MonitorConnection to measure recent throughput
 * for the totem single-ring protocol.
 */
public class ThroughputMeter {
    
    public int getThroughput() {
	return throughput;
    }

    private static final long DEFAULT_INTERVAL = 1000;

    public ThroughputMeter() {
	interval = DEFAULT_INTERVAL;
    }

    private long lastTime;

    private int lastId;

    private long interval;

    private int throughput;

    public void update(int id, long currentTime) {
	if (currentTime >= lastTime + interval) {
	    double inc = (double) (id - lastId);
	    double time = (double) (currentTime - lastTime);
	    throughput = (int) (inc / time * 1000D);
	    lastTime = currentTime;
	    lastId = id;
	}
    }

}

