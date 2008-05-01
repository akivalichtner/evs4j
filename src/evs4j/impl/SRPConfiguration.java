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

import java.util.SortedSet;
import java.io.IOException;
import evs4j.Processor;
import evs4j.Configuration;
import evs4j.impl.message.RegularMessage;

public class SRPConfiguration implements Configuration {

    private SRPConnection conn;

    /**
     * The sequence of received messages that are
     * waiting to be delivered.
     */
    private ReceivedList received;
    
    public ReceivedList getReceived() {
	return received;
    }

    public int getMaxDelivered() {
	return received.getMaxDelivered();
    }

    public void receive(RegularMessage message) {
	received.add(message);
    }

    public int getLowMessageId() {
	return received.getLowMessageId();
    }

    private RegularTokenHandler handler;

    public RegularTokenHandler getHandler() {
	return handler;
    }

    private ProcessorSet processors;

    public ProcessorSet getProcessorSet() {
	return processors;
    }

    public SortedSet getProcessors() {
	return processors.getProcessors();
    }

    public Processor getNextProcessor(Processor processor) {
	return processors.getNextProcessor(processor);
    }

    public Processor getCoordinator() {
	return processors.getCoordinator();
    }

    private boolean transitional;

    public boolean isTransitional() {
	return transitional;
    }

    private long id;

    public long getId() {
	return id;
    }

    public SRPConfiguration(SRPConnection conn, 
		ProcessorSet processors,
		long id, 
		boolean transitional) {
	this.processors = processors.copy();
	this.id = id;
	this.received = new ReceivedList(conn);
	this.handler = new RegularTokenHandler(conn, this); 
	this.transitional = transitional;
    }
    
    public String toString() {
	StringBuffer buf = new StringBuffer();
	buf.append("[");
	buf.append(id);
	buf.append(" ");
	buf.append(processors);
	buf.append("]");
	return buf.toString();
    }

    /**
     * Takes a configuration id, breaks it down into its processor id
     * part and configuration number part, and returns processor id.
     */
    public static int getConfigurationProcessor(long id) {
	long host = (id >>> 32) & 0xFFFFFFFF;
	long configuration = id & 0xFFFFFFFF;
	return (int) host;
    }

    /**
     * Takes a configuration id, breaks it down into its processor id
     * part and configuration number part, and returns the configuration number.
     */
    public static int getConfigurationNumber(long id) {
	long host = (id >>> 32) & 0xFFFFFFFF;
	long configuration = id & 0xFFFFFFFF;
	return (int) configuration;
    }

    /**
     * Takes a processor id and a configuration number, and returns
     * a configuration id.
     */
    public static long toConfigurationId(int processor, int number) {
	long host = (long) processor;
	long configuration = (long) number;
	long id = (host << 32) | configuration;
	return id;
    }

}

