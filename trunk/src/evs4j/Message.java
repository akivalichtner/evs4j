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

/**
 * Interface for totally-ordered messages.
 */
public interface Message {

    /**
     * Returns true iff the message was or will be
     * delivered as safe (i.e. it is not delivered until all the processors
     * have acknowledged receipt.)
     */
    public boolean isSafe();

    /**
     * Returns the unique id of this message, or is undefined
     * if the Message has not been sent yet. This id is unique 
     * within its configuration, and is the same for all processors.
     */
    public int getId();

    /**
     * Returns the buffer containing the data.
     */
    public byte[] getData();

    /**
     * Returns the position of the first byte of the
     * message.
     */
    public int getOffset();

    /**
     * Returns the number of useful bytes in the array 
     * starting at the offset.
     */
    public int getLength();

    /**
     * Returns the Processor object of the processor that 
     * originated this message.
     */
    public Processor getSender();

    /**
     * Sets the number of useful bytes in the array
     * starting at the offset.
     */
    public void setLength(int length);

}



