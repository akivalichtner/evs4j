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

package evs4j.impl.timeout;

import evs4j.impl.SRPConnection;
import evs4j.impl.SRPState;

public class ConsensusTimeout extends Timeout {

    public ConsensusTimeout(long timeout) {
	super(timeout);
    }

    public void execute(SRPConnection conn, SRPState state, long now) {
	if (hasExpired(now)) {
	    if (DEBUG) conn.log("Consensus timeout expired");
	    state.consensusTimeoutExpired();
	}
    }

}
