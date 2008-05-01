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

package evs4j;

import java.util.SortedSet;

/**
 * Interface for a processor 'configuration', also known in virtual synchrony circles
 * as a 'view' or a 'group'. A configuration is an ordered list of processes (or threads)
 * which can send and receive totally-ordered messages.
 */
public interface Configuration {

    /**
     * Returns the unique id of this configuration.
     */
    public long getId();

    /**
     * Returns a SortedSet containing the Processor
     * objects of the processors in this configuration.
     */
    public SortedSet getProcessors();

    /**
     * Returns the Processor object of the coordinator 
     * for this configuration.
     */
    public Processor getCoordinator();

    /**
     * Returns true iff this configuration is transitional, i.e.
     * if it is being installed for the purpose of synching
     * up the processors before installing a new regular configuration.
     */
    public boolean isTransitional();

}
