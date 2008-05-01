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
 * Useful for implementing chains of connections. Subclasses have to implement
 * at least the send() and onMessage methods.
 */
public abstract class ConnectionDecorator implements Connection, Listener {

    protected Connection conn;

    /**
     * Constructs a ConnectionDecorator around an 
     * underlying Connection.
     */
    public ConnectionDecorator(Connection conn) {
	this.conn = conn;
	this.processor = conn.getProcessor();
	conn.setListener(this);
    }

    private Processor processor;
    
    /**
     * Calls createMessage(boolean) on the underlying connection.
     */
    public Message createMessage(boolean safe) {
	return conn.createMessage(safe);
    }

    /**
     * Calls getMaxMessageSize() on the underlying connection.
     */
    public int getMaxMessageSize() {
	return conn.getMaxMessageSize();
    }

    public Processor getProcessor() {
	return processor;
    }

    /**
     * Calls open() on the underlying connection.
     */
    public void open() throws IOException {
	conn.open();
    }

    /**
     * Calls close() on the underlying connection.
     */
    public void close() throws IOException {
	conn.close();
    }

    /**
     * Calls reset() on the underlying connection.
     */
    public void reset() throws IOException {
	conn.reset();
    }

    protected Listener listener;
    
    public void setListener(Listener listener) {
	this.listener = listener;
    }

    public abstract void send(Message message) throws IOException;

    /**
     * The configuration that was installed last by
     * the underlying connection.
     */
    private Configuration configuration;

    public void onConfiguration(Configuration configuration) {
	this.configuration = configuration;
	listener.onConfiguration(configuration);
    }

    public abstract void onMessage(Message message);

    public void onAlert(Alert alert) {
	listener.onAlert(alert);
    }

}
