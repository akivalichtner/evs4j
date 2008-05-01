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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Iterator;
import evs4j.Processor;
import evs4j.impl.message.CommitTokenMessage.CommitInfo;

public class ProcessorSet implements Cloneable {

    public ProcessorSet() {
	processors = new TreeSet();
    }

    public ProcessorSet(Processor processor) {
	this();
	add(processor);
    }

    /**
     * Contains Processor objects.
     */
    private SortedSet processors;

    public int getCount() {
	return processors.size();
    }

    public Iterator iterator() {
	return processors.iterator();
    }
    
    /**
     * Adds the processor id unless it's already in 
     * the set.
     */
    public void add(Processor processor) {
	processors.add(processor);
    }

    /**
     * Merges <em>set</em> into this set.
     */
    public void add(ProcessorSet set) {
	processors.addAll(set.getProcessors());
    }    

    public boolean equals(Object object) {
	//do not catch ClassCastException
	ProcessorSet s2 = (ProcessorSet) object;
	ProcessorSet s1 = this;
	return s1.getProcessors().equals(s2.getProcessors());
    }
    
    /**
     * Returns a ProcessorSet with all the processors in this
     * set that are not also in the set <em>set</em>.
     */
    public ProcessorSet minus(ProcessorSet set) {
	ProcessorSet r = new ProcessorSet();
	Iterator iterator = iterator();
	while (iterator.hasNext()) {
	    Processor processor = (Processor) iterator.next();
	    if (!set.contains(processor)) {
		r.add(processor);
	    }
	}
	return r;
    }

    /**
     * Returns a ProcessorSet with all the processors in this
     * set that are also in the set <em>set</em>.
     */
    public ProcessorSet intersect(ProcessorSet set) {
	ProcessorSet r = new ProcessorSet();
	Iterator iterator = iterator();
	while (iterator.hasNext()) {
	    Processor processor = (Processor) iterator.next();
	    if (set.contains(processor)) {
		r.add(processor);
	    }
	}
	return r;
    }

    /**
     * Returns true iff all the elements in <em>set</em>
     * are also in this ProcessorSet.
     */
    public boolean contains(ProcessorSet set) {
	return processors.containsAll(set.getProcessors());
    }

    public boolean contains(Processor processor) {
	return processors.contains(processor);
    }

    /**
     * Returns true iff the set is empty.
     */
    public boolean isEmpty() {
	return getCount() == 0;
    }

    /**
     * Returns the lowest processor id.
     */
    public Processor getCoordinator() {
	return (Processor) processors.first();
    }
    
    /**
     * Returns the Processor object in the set that comes
     * immediately after <em>processor</em>, or the
     * first processor id if there is none.
     */
    public Processor getNextProcessor(Processor processor) {
	SortedSet tail = processors.tailSet(processor);
	Processor nextProcessor;
	if (tail.size() > 1) {
	    Iterator iterator = tail.iterator();
	    iterator.next();
	    nextProcessor = (Processor) iterator.next();
	} else if (tail.size() == 1) {
	    nextProcessor = (Processor) processors.first();
	} else {
	    throw new RuntimeException("Processor not in set: " + processor);
	}
	return nextProcessor;
    }

    public ProcessorSet copy() {
	ProcessorSet s = null;
	try {
	    s = (ProcessorSet) clone();
	} catch (CloneNotSupportedException e) {
	    throw new RuntimeException("BUG");
	}
	return s;
    }

    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append('{');
	Iterator iterator = iterator();
	while (iterator.hasNext()) {
	    Processor processor = (Processor) iterator.next();
	    buf.append(processor);
	    if (iterator.hasNext()) {
		buf.append(", ");
	    }
	}
	buf.append('}');
	return buf.toString();
    }

    /**
     * Returns an array of CommitInfo objects to be placed
     * in a new commit token. In each object only the processor id is
     * defined. The other fields are either null or arbitrary,
     * as they need to be set by the respective processors.
     * The CommitInfo object for the leader is the first 
     * of the array.
     */
    public CommitInfo[] getInfoArray() {
	CommitInfo[] r = new CommitInfo[getCount()];
	//sorted list of elements
	Iterator iterator = iterator();
	int i = 0;
	while (iterator.hasNext()) {
	    Processor processor = (Processor) iterator.next();
	    r[i] = new CommitInfo(processor);
	    i++;
	}
	return r;
    }

    public SortedSet getProcessors() {
	return processors;
    }

}

