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

import java.util.Set;
import java.util.TreeSet;
import evs4j.Processor;
import evs4j.impl.ProcessorSet;
import evs4j.impl.message.CommitTokenMessage.CommitInfo;

public class MessageReader extends DataReader {

    public Message readMessage(Buffer buffer) 
	throws IllegalMessageException {
	reset(buffer.getData(), 0);
	int magic = readInt();
	//reject messages with wrong magic number 
	//(wrong protocol or protocol version)
	if (magic != Message.MAGIC_NUMBER) {
	    throw new IllegalMessageException("Wrong magic number: " + magic);
	}
	int type = (int) readByte();
	Message m = null;
	switch (type) {
	case Message.TYPE_REGULAR_TOKEN:
	    //do not break
	case Message.TYPE_COMMIT_TOKEN:
	    m = readTokenMessage(magic, type, buffer);
	    break;
	case Message.TYPE_REGULAR_MESSAGE:
	    m = readRegularMessage(magic, buffer);
	    break;
	case Message.TYPE_JOIN_MESSAGE:
	    m = readJoinMessage(magic, buffer);
	    break;
	default:
	    throw new IllegalMessageException("Unknown message type: " + type);
	}
	return m;
    }

    private RegularMessage readRegularMessage(int magic, Buffer buffer) {
	Processor sender = readProcessor();
	long configurationId = readLong();
	int id = readInt();
	boolean recovered = readBoolean();
	boolean safe = readBoolean();
	int length = (int) readShort();
	RegularMessage m = new RegularMessage(magic,
					      buffer,
					      sender,
					      configurationId,
					      id,
					      recovered,
					      safe,
					      length);
	return m;
    }

    private JoinMessage readJoinMessage(int magic, Buffer buffer) {
	Processor sender = readProcessor();
	ProcessorSet candidates = readProcessorSet();
	ProcessorSet failed = readProcessorSet();
	int maxConfigurationNumber = readInt();
	return new JoinMessage(magic,
			       buffer,
			       sender,
			       candidates,
			       failed,
			       maxConfigurationNumber);
    }

    private TokenMessage readTokenMessage(int magic, 
					  int type,
					  Buffer buffer) {
	long configurationId = readLong();
	int id = readInt();
	Processor destination = readProcessor();
	TokenMessage m = null;
	switch (type) {
	case Message.TYPE_REGULAR_TOKEN:
	    m = readRegularTokenMessage(magic, buffer, configurationId, id, destination);
	    break;
	case Message.TYPE_COMMIT_TOKEN:
	    m = readCommitTokenMessage(magic, buffer, configurationId, id, destination);
	    break;
	default:
	    throw new RuntimeException("Unknown token type: " + type);
	}
	return m;
    }

    private RegularTokenMessage readRegularTokenMessage(int magic,
							Buffer buffer,
							long configurationId,
							int id,
							Processor destination) {
	int maxMessageId = readInt();
	int lowMessageId = readInt();
	Processor slowProcessor = readProcessor();
	Set missed = new TreeSet();
	int len = readArrayLength();
	for (int i = 0; i < len; i++) {
	    missed.add(new Integer(readInt()));
	}
	int totalBroadcast = (int) readInt();
	int totalBacklog = (int) readInt();
	float window = readFloat();
	float threshold = readFloat();
	return new RegularTokenMessage(magic,
				       buffer,
				       configurationId,
				       id,
				       destination,
				       maxMessageId,
				       lowMessageId,
				       slowProcessor,
				       missed,
				       totalBroadcast,
				       totalBacklog,
				       window,
				       threshold);
    }

    private CommitTokenMessage readCommitTokenMessage(int magic,
						      Buffer buffer,
						      long configurationId,
						      int id,
						      Processor destination) {
	int len = readArrayLength();
	CommitInfo[] info = new CommitInfo[len];
	for (int i = 0; i < len; i++) {
	    info[i] = readCommitInfo();
	}
	return new CommitTokenMessage(magic,
				      buffer,
				      configurationId,
				      id,
				      destination,
				      info);
    }

    private Processor readProcessor() {
	int value = readInt();
	Processor processor;
	if (value != 0) {
	    processor = new Processor(value);
	} else {
	    processor = null;
	}
	return processor;
    }

    private ProcessorSet readProcessorSet() {
	int len = readArrayLength();
	ProcessorSet processors = new ProcessorSet();
	for (int i=0; i<len; i++) {
	    processors.add(readProcessor());
	}
	return processors;
    }

    private CommitInfo readCommitInfo() {
	Processor processor = readProcessor();
	long configurationId = readLong();
	int lowMessageId = readInt();
	return new CommitInfo(processor,
			      configurationId,
			      lowMessageId);
    }

    private int readArrayLength() {
	return (int) readShort();
    }
    
}

