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

import evs4j.Processor;

public abstract class TokenMessage extends Message {

    /**
     * The id of the configuration on which the token
     * is circulating.
     */
    protected long configurationId;

    public long getConfigurationId() {
	return configurationId;
    }

    /**
     * The token's sequence number.
     */
    protected int id;
    
    public int getId() {
	return id;
    }

    public void setId(int id) {
	this.id = id;
    }

    /**
     * The Processor object of the processor to which 
     * this token is directed.
     */
    protected Processor destination;
    
    public Processor getDestination() {
	return destination;
    }

    public void setDestination(Processor destination) {
	this.destination = destination;
    }

    protected TokenMessage(int magic,
			   int type,
			   Buffer buffer,
			   long configurationId,
			   int id,
			   Processor destination) {
	super(magic, type, buffer);
	this.configurationId = configurationId;
	this.id = id;
	this.destination = destination;
    }

}



