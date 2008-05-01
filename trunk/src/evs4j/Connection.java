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

import java.io.IOException;

/**
 * Connection objects are used to join/rejoin a group, to send messages, and to register a
 * Listener object, which can receive messages.
 */
public interface Connection {

    /**
     * Returns a Message object to be used for sending. The
     * client must set the data in the byte array. The Message
     * returned has the senderId property already set, but no
     * meaningful message id.
     */
    public Message createMessage(boolean safe);

    /**
     * Returns the maximum number of useful bytes
     * contained in a packet (total size minus header).
     */
    public int getMaxMessageSize();

    /**
     * Returns the Processor object for this processor.
     */
    public Processor getProcessor();
    
    /**
     * This method opens the connection for sending 
     * and receiving. This method causes this 'processor'
     * to attempt to form a new configuration with other
     * processors on the network. After this the first method
     * that will be called is Listener.onConfiguration(Configuration),
     * and then messages start being delivered. A connection cannot
     * send messages until after open() is called.
     */
    public void open() throws IOException;

    /**
     * This method closes the connection. The Connection's
     * behavior is undefined after it is closed.
     */
    public void close() throws IOException;

    /**
     * This method tells the connection to create a new 
     * configuration. The new configuration is usually
     * needed because the number of messages that can
     * sent in a single configuration is limited by the
     * size of a Java int. Complicated stacks of connection
     * will also find other reasons to force new configurations
     * to be created.
     */
    public void reset() throws IOException;

    /**
     * Enqueues a message for sending.
     */
    public void send(Message message) throws IOException;
		     
    /**
     * Sets the Listener to which the connection
     * will pass asynchronous events, such as
     * incoming messages and installed configurations.
     * This method must be called before open() or some messages
     * may be missed.
     */
    public void setListener(Listener listener);

}

