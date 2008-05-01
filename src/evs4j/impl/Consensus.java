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

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import evs4j.Processor;

public class Consensus {

    private Set processors;

    private Consensus() {
	processors = new HashSet();
	maxConfigurationNumber = 0;
    }

    /**
     * This constructor creates a Consensus with one
     * processor, which should be the current processor.
     */
    public Consensus(Processor processor, int maxConfigurationNumber) {
	this();
	add(processor, maxConfigurationNumber);
    }
    
    private int maxConfigurationNumber;

    public int getMaxConfigurationNumber() {
	return maxConfigurationNumber;
    }

    /**
     * Adds a processor from which we have consensus.
     */
    public void add(Processor processor, int maxConfigurationNumber) {
	processors.add(processor);
	//update max configuration id
	if (this.maxConfigurationNumber < maxConfigurationNumber) {
	    this.maxConfigurationNumber = maxConfigurationNumber;
	}
    }

    public void remove(Processor processor) {
	processors.remove(processor);
    }
    
    public boolean get(Processor processor) {
	return processors.contains(processor);
    }

    /**
     * Returns true iff the each of the processors in
     * the set <em>diff</em> (which should be the
     * difference of the set of candidates and the set
     * of failed processors) is listed in this Consensus.
     */
    public boolean check(ProcessorSet diff) {
	return processors.containsAll(diff.getProcessors());
    }

}
