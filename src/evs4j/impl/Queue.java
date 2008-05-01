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

import java.util.Vector;

public class Queue {

    private static class Entry {
	public Object value;
	public Entry previous;
	public Entry next;
    }
    
    private int length;

    public int length() {
	return length;
    }

    /**
     * First element of the queue.
     */
    private Entry first;

    /**
     * Last element of the queue.
     */
    private Entry last;
    
    /**
     * Free list.
     */
    private Entry free;

    public Queue(int length) {
	//create free list
	for (int i=0; i<length; i++) {
	    Entry entry = new Entry();
	    entry.next = free;
	    free = entry;
	}
    }

    public static final int DEFAULT_QUEUE_SIZE = 50;

    public Queue() {
	this(DEFAULT_QUEUE_SIZE);
    }

    /**
     * Removes all the elements from the queue.
     */
    public synchronized void clear() {
	long forever = 0L;
	while (remove(forever) != null) {
	    //do nothing
	}
    }

    /**
     * Adds an object at the end of the queue.
     */
    public synchronized void add(Object object) {
	//get a free entry
	Entry entry = null;
	if (free == null) {
	    entry = new Entry();
	} else {
	    entry = free;
	    free = entry.next;
	}
	entry.value = object;
	entry.previous = null;
	entry.next = first;
	if (first != null) {
	    first.previous = entry;
	}
	first = entry;
	if (last == null) {
	    last = entry;
	}
	length++;
	//wake up threads waiting for this
	notify();
    }

    /**
     * Removes the element at the beginning of the queue.
     * This method blocks until an element is available
     * to be removed or until <em>duration</em> ms have passed.
     * To wait forever, call this method with a duration of 0.
     * To not wait at all, call this method with a negative duration.
     */
    public synchronized Object remove(long duration) {
	Object object = null;
	long period;
	if (duration > 0) {
	    period = duration;
	} else {
	    //use any large number
	    period = 1000;
	}
	while (object == null) {
	    if (length > 0) {
		//get last element
		Entry entry = last;
		object = entry.value;
		Entry previous = entry.previous;
		if (previous != null) {
		    previous.next = null;
		}
		last = previous;
		//add used entry to free list
		entry.next = free;
		free = entry;
		length--;
		break;
	    }
	    if (duration >= 0) {
		try {
		    //wait for an element to be added		    
		    wait(period);
		} catch (InterruptedException e) {
		    //ignore
		}
	    }
	    if (duration != 0) {
		break;
	    }
	}
	return object;
    }

    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append("(");
	for (Entry e = first; e != null; e = e.next) {
	    buf.append(e.value);
	    if (e.next != null) {
		buf.append(", ");
	    }
	}
	buf.append(")");
	return buf.toString();
    }

}

