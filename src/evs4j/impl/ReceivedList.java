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
import java.util.Enumeration;
import java.util.Set;
import java.util.TreeSet;
import evs4j.Listener;
import evs4j.impl.CompileTimeMacro;
import evs4j.impl.message.RegularMessage;

public class ReceivedList implements CompileTimeMacro {

    public ReceivedList(SRPConnection conn) {
	this.conn = conn;
	this.listener = conn.getListener();
	this.length = DEFAULT_LENGTH;
	//create free list
	for (int i=0; i<length; i++) {
	    Entry entry = new Entry();
	    entry.next = free;
	    free = entry;
	}
    }

    public static final int DEFAULT_LENGTH = 50;
    
    private SRPConnection conn;

    /**
     * The object to which we deliver messages.
     */
    private Listener listener;
    
    private static class Entry {
	public int messageId;
	public RegularMessage message;
	public Entry previous;
	public Entry next;
    }
    
    private int length;

    public int length() {
	return length;
    }

    /**
     * First element of the buffer.
     */
    private Entry first;

    /**
     * Last element of the buffer.
     */
    private Entry last;

    /**
     * Free list.
     */
    private Entry free;

    /**
     * Low water mark: the largest id such that 
     * this processor has received all messages
     * with ids less than or equal to this id.
     */
    private int lowMessageId;
    
    public int getLowMessageId() {
	return lowMessageId;
    }
    
    /**
     * Adds <em>message</em> to the list in order of
     * message id, updates <em>lowMessageId</em>, 
     * and delivers all the messages according to
     * Extended Virtual Synchrony.
     */
    public void add(RegularMessage message) {
	int messageId = message.getId();
	Entry k;
	for (k = last; k != null && k.previous != null; k = k.previous) {
	    if (k.messageId <= messageId) {
		break;
	    }
	}
	if (k != null && k.messageId == messageId) {
	    //redundant
	    //go to end of method
	} else {
	    //insert new message
	    //get an Entry object
	    Entry entry = null;
	    if (free == null) {
		entry = new Entry();
	    } else {
		entry = free;
		free = entry.next;
	    }
	    //initialize Entry object
	    entry.messageId = messageId;
	    entry.message = message;
	    //set next/previous pointers below
	    Entry previous = null;
	    Entry next = null;
	    if (k != null) {
		int id = k.messageId;
		if (id > messageId) {
		    next = k;
		    previous = k.previous;
		    if (k == first) {
			first = entry;
		    }
		} else if (id < messageId) {
		    previous = k;
		    next = k.next;
		    if (k == last) {
			last = entry;
		    }
		}
	    }
	    entry.next = next;
	    entry.previous = previous;
	    //update neighbors
	    if (next != null) {
		next.previous = entry;
	    }
	    if (previous != null) {
		previous.next = entry;
	    }
	    if (first == null) {
		first = entry;
	    }
	    if (last == null) {
		last = entry;
	    }
	    //update list length
	    this.length++;
	    //update safe message id
	    int previousId = lowMessageId;
	    int tmp = previousId;
	    for (k = entry; k != null; k = k.next) {
		int id = k.messageId;
		if (id == previousId + 1) {
		    tmp = id;
		    previousId = tmp;
		} else {
		    //found a gap
		    break;
		}
	    }
	    this.lowMessageId = tmp;
	    deliver();
	}
    }

    /**
     * Returns the message for the given id, if any.
     */
    public RegularMessage get(int id) {
	RegularMessage message = null;
	for (Entry k = first; k != null; k = k.next) {
	    if (k.messageId == id) {
		message = k.message;
		break;
	    }
	}
	return message;
    }

    public RegularMessage get(Integer id) {
	return get(id.intValue());
    }

    public Enumeration getMessages() {
	Vector vector = new Vector();
	for (Entry k = first; k != null; k = k.next) {
	    vector.addElement(k.message);
	}
	return vector.elements();
    }

    /**
     * The id of the last message delivered
     * to the application.
     */
    private int maxDelivered;

    public int getMaxDelivered() {
	return maxDelivered;
    }

    /**
     * The lowMessageId field of the token from
     * two rotations ago. This is the largest 
     * id of any message that can be delivered 
     * as safe by this processor.
     */
    private int safeMessageId;    
    
    public void setSafeMessageId(int safeMessageId) {
	this.safeMessageId = safeMessageId;
    }

    /** 
     * This method delivers all messages that 
     * are ready to be delivered according to
     * Extended Virtual Synchrony.
     */
    private void deliver() {
	int min = maxDelivered + 1;
	int previousId = min - 1;
	int last = 0;
	for (Entry k = first; k != null; k = k.next) {
	    int messageId = k.messageId;
	    //start delivering after the last message
	    //already delivered
	    if (messageId >= min) {
		//check that messages are in a sequence
		if (messageId == previousId + 1) {
		    RegularMessage message = k.message;
		    boolean agreed = !message.getSafe();
		    //if message is to be delivered as safe,
		    //check that all processors have already
		    //received the message
		    if (agreed || messageId <= safeMessageId) {
			last = messageId;
			boolean recovered = message.getRecovered();
			if (!recovered) {
			    listener.onMessage(message);
			} else {
			    //discard (done)
			}
		    }
		    previousId = messageId;
		} else {
		    //found gap
		    break;
		}
	    }
	}
	if (last > 0) {
	    maxDelivered = last;
	}
	if (DEBUG) conn.log("maxDelivered = " + maxDelivered);
    }

    /**
     * Removes all messages with ids from 1 to the 
     * lesser of maxDelivered and safeMessageId, 
     * (inclusive).
     */
    public void prune() {
	int max = safeMessageId;
	if (maxDelivered < max) {
	    //only prune delivered messages
	    max = maxDelivered;
	}
	//extract list of recylable links
	Entry start = null;
	Entry end = null;	
	if (first != null && first.messageId <= max) {
	    start = first;
	    for (Entry k = start; k != null; k = k.next) {
		if (k.messageId <= max) {
		    end = k;
		    //release reference to message
		    k.message = null;
		}
	    }
	}
	if (end != null) {
	    first = end.next;
	    if (first == null) {
		last = null;
	    } else {
		first.previous = null;
	    }
	    //add recycled to free list
	    if (free != null) {
		free.previous = end;
	    }
	    end.next = free;
	    start.previous = null;
	    free = start;
	}
    }

    /**
     * Returns a Set containing all the ids we
     * are missing between <em>lowMessageId</em> and 
     * <em>maxMessageId</em>.
     */
    public Set getMissed(int maxMessageId) {
	Set missed = new TreeSet();
	for (int id = lowMessageId + 1; id <= maxMessageId; id++) {
	    missed.add(new Integer(id));
	}
	//remove ids for messages we do have
	for (Entry k = first; k != null; k = k.next) {
	    missed.remove(new Integer(k.messageId));
	}
	return missed;
    }

    /**
     * Returns a list of ids.
     */
    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append("\nmessages: ");
	for (Entry k = first; k != null; k = k.next) {
	    buf.append(k.messageId);
	    buf.append(", ");
	}
	buf.append("\nfree: ");
	for (Entry k = free; k != null; k = k.next) {
	    buf.append(k.messageId);
	    buf.append(", ");
	}
	return buf.toString();
    }

}





