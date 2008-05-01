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

package evs4j.impl.message;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.io.IOException;
import evs4j.impl.CompileTimeMacro;
import evs4j.impl.SRPConnection;
import evs4j.impl.SRPState;

public abstract class Message implements CompileTimeMacro {

    public static final int TYPE_REGULAR_MESSAGE      = 1;
    public static final int TYPE_REGULAR_TOKEN        = 2;
    public static final int TYPE_JOIN_MESSAGE         = 3;
    public static final int TYPE_COMMIT_TOKEN         = 4;

    /**
     * This must not be changed unless we redesign the
     * protocol implementation.
     */
    public static final int MAGIC_NUMBER = 271828;
    
    /**
     * The protocol number for the message.
     */
    protected int magic;

    public int getMagic() {
	return magic;
    }

    /**
     * The type of the message.
     * This is one of the types listed herein.
     */
    protected int type;
    
    public int getType() {
	return type;
    }

    /**
     * The Buffer containing the byte
     * array for this message.
     */
    protected Buffer buffer;

    public Buffer getBuffer() {
	return buffer;
    }

    protected Message(int magic,
		      int type,
		      Buffer buffer) {
	this.magic = magic;
	this.type = type;
	this.buffer = buffer;
    }

    /**
     * The maximum size of a packet. This is defined by the 
     * protocol.
     */
    public static final int MAX_PACKET_SIZE = 1500;

    public abstract void execute(SRPConnection conn, SRPState state);
    
}
