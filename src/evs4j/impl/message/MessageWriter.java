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

import java.util.Iterator;
import java.util.Set;
import evs4j.Processor;
import evs4j.impl.ProcessorSet;
import evs4j.impl.message.CommitTokenMessage.CommitInfo;

public class MessageWriter extends DataWriter {

    public MessageWriter() {
    }
    
    public void writeMessage(Message m) {
	Buffer buffer = m.buffer; 
	reset(buffer.getData(), 0);
	writeInt(m.magic);
	int type = m.type;
	writeByte((byte) type);
	switch (type) {
	case Message.TYPE_REGULAR_TOKEN:
	    //do not break
	case Message.TYPE_COMMIT_TOKEN:
	    writeTokenMessage((TokenMessage) m);
	    break;
	case Message.TYPE_REGULAR_MESSAGE:
	    writeRegularMessage((RegularMessage) m);
	    break;
	case Message.TYPE_JOIN_MESSAGE:
	    writeJoinMessage((JoinMessage) m);
	    break;
	default:
	    throw new RuntimeException("Unknown message type: " + type);
	}
	//set length of data written
	buffer.setLength(offset);
    }
    
    private void writeRegularMessage(RegularMessage m) {
	writeProcessor(m.getSender());
	writeLong(m.getConfigurationId());
	writeInt(m.getId());
	writeBoolean(m.getRecovered());
	writeBoolean(m.getSafe());
	writeShort((short) m.getLength());
	//last value of offset is 
	//taken to be the packet length
	offset += m.getLength();
	//bytes are already written by the application
    }

    private void writeJoinMessage(JoinMessage m) {
	writeProcessor(m.getSender());
	writeProcessorSet(m.getCandidates());
	writeProcessorSet(m.getFailed());
	writeInt(m.getMaxConfigurationNumber());
    }

    private void writeTokenMessage(TokenMessage m) {
	writeLong(m.getConfigurationId());
	writeInt(m.getId());
	writeProcessor(m.getDestination());
	int type = m.type;
	switch (type) {
	case Message.TYPE_REGULAR_TOKEN:
	    writeRegularTokenMessage((RegularTokenMessage) m);
	    break;
	case Message.TYPE_COMMIT_TOKEN:
	    writeCommitTokenMessage((CommitTokenMessage) m);
	    break;
	default:
	    throw new RuntimeException("Unknown token type: " + type);
	}
    }

    private void writeRegularTokenMessage(RegularTokenMessage m) {
	writeInt(m.getMaxMessageId());
	writeInt(m.getLowMessageId());
	writeProcessor(m.getSlowProcessor());
	Set missed = m.getMissed();
	int len = missed.size();
	int max = RegularTokenMessage.MAX_MISSED_MESSAGES;
	if (len > max) {
	    len = max;
	}
	writeArrayLength(len);
	Iterator iterator = missed.iterator();
	while (iterator.hasNext()) {
	    writeInt(((Integer) iterator.next()).intValue());	    
	}
	writeInt(m.getTotalBroadcast());
	writeInt(m.getTotalBacklog());	
	writeFloat(m.getWindow());
	writeFloat(m.getThreshold());
    }

    private void writeCommitTokenMessage(CommitTokenMessage m) {
	CommitInfo[] info = m.getInfo();
	int len = info.length;
	writeArrayLength(len);
	for (int i = 0; i < len; i++) {
	    writeCommitInfo(info[i]);
	}
    }

    private void writeProcessor(Processor processor) {
	int value;
	if (processor != null) {
	    value = processor.getValue();
	} else {
	    value = 0;
	}
	writeInt(value);
    }

    private void writeProcessorSet(ProcessorSet processors) {
	int len = processors.getCount();
	writeArrayLength(len);
	Iterator iterator = processors.iterator();
	while (iterator.hasNext()) {
	    writeProcessor((Processor) iterator.next());
	}
    }

    private void writeCommitInfo(CommitInfo info) {
	writeProcessor(info.getProcessor());
	writeLong(info.getConfigurationId());
	writeInt(info.getLowMessageId());
    }

    private void writeArrayLength(int len) {
	writeShort((short) len);
    }

}



