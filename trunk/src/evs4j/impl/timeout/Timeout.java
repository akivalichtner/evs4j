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

import evs4j.impl.CompileTimeMacro;
import evs4j.impl.SRPConnection;
import evs4j.impl.SRPState;

public abstract class Timeout implements CompileTimeMacro {
    
    private long duration;

    /**
     * Returns the intended duration of the Timeout
     * in milliseconds.
     */
    public long getDuration() {
	return duration;
    }

    /**
     * Resets the intended duration of the Timeout
     * in milliseconds, for use the next time
     * the begin() method is called.
     */
    public void setDuration(long duration) {
	this.duration = duration;
    }

    public Timeout(long duration) {
	this.duration = duration;
	this.set = false;
    }

    private boolean set;

    private long expires;

    /**
     * Returns true iff the timeout has expired.
     */
    public boolean hasExpired(long now) {
	boolean b = set && (now >= expires);
	return b;
    }

    /**
     * Begins a new waiting cycle. This method starts a new
     * waiting cycle regardless of whether this Timeout is 
     * already waiting.
     */
    public void reset() {
	this.set = true;
	this.expires = (System.currentTimeMillis() + duration);
    }
    
    /**
     * Cancels current waiting cycle. This method may be
     * called whether or not this Timeout is actually
     * waiting or not.
     */
    public void cancel() {
	this.set = false;
    }

    public String toString() {
	return String.valueOf(duration);
    }

    public abstract void execute(SRPConnection conn, SRPState state, long now);
    
}

