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

import java.util.TreeSet;
import java.net.InetAddress;
import evs4j.Processor;
import evs4j.impl.ProcessorSet;
import evs4j.impl.SRPConfiguration;
import evs4j.impl.message.CommitTokenMessage.CommitInfo;
import evs4j.impl.message.Message;
import evs4j.impl.message.MessageWriter;
import evs4j.impl.message.MessageReader;
import evs4j.impl.message.IllegalMessageException;
import evs4j.impl.message.Buffer;
import evs4j.impl.message.RegularMessage;
import evs4j.impl.message.JoinMessage;
import evs4j.impl.message.CommitTokenMessage;
import evs4j.impl.message.RegularTokenMessage;

public class MessageWriterTest {

   public static void main(String[] args) {
	MessageWriterTest test = new MessageWriterTest();
	test.testRegularMessage();
	test.testRegularTokenMessage();
	test.testCommitTokenMessage();
	test.testJoinMessage();
    }

    private MessageWriter writer;
    private MessageReader reader;
    private Buffer buffer;

    public MessageWriterTest() {
	writer = new MessageWriter();
	reader = new MessageReader();
	buffer = new Buffer(0);       
    }

    private void checkWriteRead(Message m1) {
	writer.writeMessage(m1);
	Message m2 = null;
	try {
	    m2 = reader.readMessage(m1.getBuffer());
	} catch (IllegalMessageException e) {
	    e.printStackTrace();
	    throw new RuntimeException("Test failed", e);
	}
	if (!m1.equals(m2)) {
	    throw new RuntimeException("Test failed");
	}
    }
    
    public void testRegularMessage() {
	Processor sender = new Processor(1);
	long configurationId = SRPConfiguration.toConfigurationId(1, 42);
	int id = 43834;
	boolean safe = true;
	RegularMessage m = new RegularMessage(Message.MAGIC_NUMBER,
					      new Buffer(0),
					      sender,
					      0L,
					      0,
					      false,
					      safe,
					      0);
	boolean recovered = false;
	m.setConfigurationId(configurationId);
	m.setId(id);
	byte[] data = "TEST_DATA".getBytes();
	int length = data.length;
	System.arraycopy(data, 0,
			 m.getData(), m.getOffset(),
			 length);
	m.setLength(length);
	checkWriteRead(m);
    }

    public void testJoinMessage() {
	Processor cand1 = new Processor(2);
	Processor cand2 = new Processor(3);
	ProcessorSet candidates = new ProcessorSet();
	candidates.add(cand1);
	candidates.add(cand2);
	Processor fail1 = new Processor(4);
	Processor fail2 = new Processor(5);
	ProcessorSet failed = new ProcessorSet();
	failed.add(fail1);
	failed.add(fail2);
	int maxConfigurationNumber = 95;
	Processor sender = new Processor(1);
	JoinMessage m = new JoinMessage(Message.MAGIC_NUMBER,
					buffer,
					sender,
					candidates,
					failed,
					maxConfigurationNumber);
	checkWriteRead(m);
    }

    public void testCommitTokenMessage() {
	long configurationId = SRPConfiguration.toConfigurationId(1, 42);
	CommitInfo[] info = new CommitInfo[2];
	Processor cand1 = new Processor(2);
	Processor cand2 = new Processor(3);
	info[0] = new CommitInfo(cand1, configurationId, 0);
	info[1] = new CommitInfo(cand2, 0, 0);
	int tokenId = 1;
	CommitTokenMessage m = new CommitTokenMessage(buffer,
						      configurationId,
						      tokenId,
						      info);
	checkWriteRead(m);
    }

    public void testRegularTokenMessage() {
	long configurationId = SRPConfiguration.toConfigurationId(1, 42);
	int id = 43834;
	Processor dest = new Processor(2);
	Processor slow = new Processor(3);
	RegularTokenMessage m = new RegularTokenMessage(Message.MAGIC_NUMBER,
							buffer,
							configurationId,
							id,
							dest,
							10,     
							5,
							slow,
							new TreeSet(),
							100,
							80,
							13.5F,
							23.6F);
	checkWriteRead(m);
    }

}


