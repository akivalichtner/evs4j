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

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import evs4j.impl.CompileTimeMacro;
import evs4j.impl.message.Message;
import evs4j.impl.message.IllegalMessageException;
import evs4j.impl.message.JoinMessage;
import evs4j.impl.message.RegularMessage;
import evs4j.impl.message.CommitTokenMessage;
import evs4j.impl.message.RegularTokenMessage;
import evs4j.impl.message.MessageWriter;
import evs4j.impl.message.MessageReader;
import evs4j.impl.message.Buffer;

public class SocketAdapter implements CompileTimeMacro {
    
    public SocketAdapter(SRPConnection conn) {
	this.conn = conn;
	InetAddress ip = conn.getIP();
	NetworkInterface nic = conn.getNic();
	int port = conn.getPort();
	int socketTimeout = conn.getSocketTimeout();
	try {
	    socket = new MulticastSocket(port);
	    socket.setNetworkInterface(nic);
	    socket.setSoTimeout(socketTimeout);
	    //hint to OS to _enable_ loop-back mode
	    socket.setLoopbackMode(false);
	    socket.joinGroup(ip);
	} catch (IOException e) {
	    e.printStackTrace();
	    throw new RuntimeException("Failed to create receive socket");
	}
	receivePacket = new DatagramPacket(new byte[0], 0);
	reader = new MessageReader();
	sendPacket = new DatagramPacket(new byte[0], 0, ip, port);
	writer = new MessageWriter();
    }
    
    private SRPConnection conn;
    
    private MulticastSocket socket;
    
    private DatagramPacket sendPacket;

    private MessageWriter writer;

    public void send(Message message) {
	if (DEBUG) {
	    if (message instanceof JoinMessage) {
		conn.log("Broadcasting join message: " + message);
	    } else if (message instanceof RegularMessage) {
		conn.log("Broadcasting message: " + ((RegularMessage) message).getId());
	    }
	}
	try {
	    Buffer buffer = message.getBuffer();
	    writer.writeMessage(message);
	    sendPacket.setData(buffer.getData());
	    sendPacket.setLength(buffer.getLength());
	    socket.send(sendPacket);
	} catch (IOException e) {
	    e.printStackTrace();
	    throw new RuntimeException("Failed to multicast");
	}
    }

    private DatagramPacket receivePacket;
    
    private MessageReader reader;
    
    public Message receive() {
	Message message = null;
	try {
	    Buffer buffer = new Buffer(0);
	    //read reusable packet
	    byte[] data = buffer.getData();
	    receivePacket.setData(data);
	    receivePacket.setLength(data.length);
	    socket.receive(receivePacket);
	    if (DEBUG) {
		conn.log("received packet");
	    }
	    //parse message
	    buffer.setLength(receivePacket.getLength());
	    message = reader.readMessage(buffer);
	} catch (InterruptedIOException e) {
	    //socket timeout expired
	    if (DEBUG) {
		conn.log("Socket timeout expired");
	    }
	} catch (IllegalMessageException e) {
	    //skip this message
	    //this should not happen
	    if (DEBUG) {
		e.printStackTrace();
		conn.log("Illegal message - skipping");
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new RuntimeException("Receiver failed");
	}
	return message;
    }

    public void close() throws IOException {
	socket.close();
    }

}


