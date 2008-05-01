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
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.LinkedList;
import evs4j.impl.Queue;
import evs4j.Listener;
import evs4j.Processor;
import evs4j.Connection;
import evs4j.impl.CompileTimeMacro;
import evs4j.impl.timeout.Timeout;
import evs4j.impl.timeout.TokenDroppedTimeout;
import evs4j.impl.timeout.TokenLossTimeout;
import evs4j.impl.timeout.JoinTimeout;
import evs4j.impl.timeout.ConsensusTimeout;
import evs4j.impl.message.Message;
import evs4j.impl.message.TokenMessage;
import evs4j.impl.message.RegularTokenMessage;
import evs4j.impl.message.CommitTokenMessage;
import evs4j.impl.message.RegularMessage;
import evs4j.impl.message.JoinMessage;
import evs4j.impl.message.Buffer;

/**
 * This class implements the state machine for the Totem Single Ring 
 * Protocol, using a design pattern which the Gof calls 'State'. 
 * Using the Gof terminology, this class is the <em>context</em>.
 * <p>
 * The abstract class SRPState is the <em>state</em> superclass,
 * and the classes SRPOperational, SRPGather, SRPCommit and SRPRecovery
 * are the states of the state machine. This pattern works well in this
 * case because the Single Ring Protocol can be modeled as a state
 * machine with 4 states and 6 events (to each event there corresponds
 * an abstract method of SRPState). The SRPConnection dispatches events 
 * (like messages arriving and timeouts expiring) to the current
 * SRPState object.
 * <p>
 * This implementation of the protocol is based on the article:
 * <pre>
 * "The Totem Single-Ring Ordering and Membership Protocol",
 * Y. Amir, L. E. Moser, P. M. Melliar-Smith, D. A. Agarwal,
 * and P. Ciarfella, 
 * ACM Transactions on Computer Systems 13, 4 (November 1995), 311-342.
 * </pre>
 * <p>
 * The article gives an accurate description of the protocol, including
 * some pseudocode for the event-handling procedures, organized by 
 * state.
 */
public class SRPConnection implements Runnable, 
				      CompileTimeMacro, 
				      Connection {

    public static final String PROP_CONSENSUS_TIMEOUT = "consensusTimeout";
    public static final String PROP_JOIN_TIMEOUT = "joinTimeout";
    public static final String PROP_TOKEN_DROPPED_TIMEOUT = "tokenDroppedTimeout";
    public static final String PROP_TOKEN_LOSS_TIMEOUT = "tokenLossTimeout";
    public static final String PROP_WINDOW_SIZE = "windowSize";
    public static final String PROP_IP = "ip";
    public static final String PROP_NIC = "nic";
    public static final String PROP_PORT = "port";
    public static final String PROP_DEBUG = "debug";
    
    /**
     * Default consensus timeout (ms).
     */
    public static final long DEFAULT_CONSENSUS_TIMEOUT = 1000;

    /**
     * Default join timeout (ms).
     */
    public static final long DEFAULT_JOIN_TIMEOUT = 3;

    /**
     * Default token dropped timeout (ms).
     */
    public static final long DEFAULT_TOKEN_DROPPED_TIMEOUT = 3;

    /**
     * Default token loss timeout (ms).
     */
    public static final long DEFAULT_TOKEN_LOSS_TIMEOUT = 1000;

    /**
     * Default max window size.
     */
    public static final int DEFAULT_WINDOW_SIZE = 30;
   
    public void shiftToGather() {
	SRPGather s = new SRPGather(this,
				    configuration, 
				    processor);
	setState(s);
    }

    public void shiftToCommit(CommitTokenMessage token) {
	SRPCommit s = new SRPCommit(this,
				    configuration,
				    token,
				    processor);
	setState(s);
    }

    public void shiftToRecovery(CommitTokenMessage token) {
	SRPRecovery s = new SRPRecovery(this,
					configuration,
					token,
					processor);
	setState(s);
    }

    public void shiftToOperational() {
	SRPOperational s = new SRPOperational(this, 
					      configuration,
					      processor);
	setState(s);
    }

    /**
     * Sends out an updated JoinMessage, shifting
     * to the GATHER state if necessary.
     */
    public void lostProcessor(Processor processor) {
	if (!(state instanceof SRPGather)) {
	    shiftToGather();
	}
	SRPGather s = (SRPGather) state;	
	s.addFailed(processor);
	s.discover();
    }

    /**
     * Sends out an updated JoinMessage, shifting
     * to the GATHER state if necessary.
     */
    public void foundProcessor(Processor processor) {
	if (!(state instanceof SRPGather)) {
	    shiftToGather();
	}
	SRPGather s = (SRPGather) state;
	s.addCandidate(processor);
	s.discover();
    }
    
    public void discover() {
	if (!(state instanceof SRPGather)) {
	    shiftToGather();
	}
	SRPGather s = (SRPGather) state;
	s.discover();
    }
    
    public void gather(JoinMessage message) {
	if (!(state instanceof SRPGather)) {
	    shiftToGather();
	}
	SRPGather s = (SRPGather) state;
	s.gather(message);
	s.discover();
    }
    
    private JoinMessage cachedJoinMessage;

    public void broadcastJoinMessage() {
	if (cachedJoinMessage != null) {
	    broadcastJoinMessage(cachedJoinMessage);
	}
    }
    
    public void broadcastJoinMessage(JoinMessage message) {
	this.cachedJoinMessage = message;
	socket.send(message);
    }

    private Timeout tokenLossTimeout;

    public void setTokenLossTimeout(long duration) {
	tokenLossTimeout.setDuration(duration);
    }

    public void resetTokenLossTimeout() {
	tokenLossTimeout.reset();
    }

    public void cancelTokenLossTimeout() {
	tokenLossTimeout.cancel();
    }
    
    private Timeout tokenDroppedTimeout;

    public void setTokenDroppedTimeout(long duration) {
	tokenDroppedTimeout.setDuration(duration);
    }

    public void resetTokenDroppedTimeout() {
	tokenDroppedTimeout.reset();
    }

    public void cancelTokenDroppedTimeout() {
	tokenDroppedTimeout.cancel();
    }

    private Timeout joinTimeout;

    public void setJoinTimeout(long duration) {
	joinTimeout.setDuration(duration);
    }

    public void resetJoinTimeout() {
	joinTimeout.reset();
    }

    public void cancelJoinTimeout() {
	joinTimeout.cancel();
    }
    
    private Timeout consensusTimeout;

    public void setConsensusTimeout(long duration) {
	consensusTimeout.setDuration(duration);
    }

    public void resetConsensusTimeout() {
	consensusTimeout.reset();
    }
    
    public void cancelConsensusTimeout() {
	consensusTimeout.cancel();
    }

    /**
     * The maximum window size to be used. Note that
     * under a large load, a large window size means
     * longer latency, because each processors can send
     * a large number of messages before forwarding the
     * token.
     */
    private int windowSize;
    
    public void setWindowSize(int windowSize) {
	this.windowSize = windowSize;
    }

    public int getWindowSize() {
	return windowSize;
    }

    /**
     * Default value for the maximum size of the input buffer
     * for regular messages.
     */
    public static final int DEFAULT_INPUT_BUFFER_SIZE = 1000;

    /**
     * Default socket timeout in milliseconds.
     * It should be a small number, so that if we
     * are not receiving messages we move on
     * to other tasks.
     */
    private int socketTimeout;

    public int getSocketTimeout() {
	return socketTimeout;
    }

    public void setSocketTimeout(int socketTimeout) {
	this.socketTimeout = socketTimeout;
    }

    public static final int DEFAULT_SOCKET_TIMEOUT = 1;

    /**
     * IP address of the multicast group
     * for this configuration.
     */
    private InetAddress ip;

    public InetAddress getIP() {
	return ip;
    }

    /**
     * Network interface used for multicasting.
     */
    private NetworkInterface nic;

    public NetworkInterface getNic() {
	return nic;
    }

    /**
     * Port number to be used for both the
     * multicast sockets.
     */
    private int port;

    public int getPort() {
	return port;
    }

    /**
     * Maximum size of input buffer for regular
     * messages. This number affects the probability
     * that this processor will drop a packet, as
     * well the throughput of the configuration.
     */
    private int maxAccepted;

    public void setMaxAccepted(int maxAccepted) {
	this.maxAccepted = maxAccepted;
    }

    private boolean debug;

    public void setDebug(boolean debug) {
	this.debug = debug;
    } 	  

    private long storedConfigurationId;

    public evs4j.Message createMessage(boolean safe) {
	RegularMessage m = new RegularMessage(Message.MAGIC_NUMBER,
					      new Buffer(0),
					      processor,
					      0L,
					      0,
					      false,
					      safe,
					      0);
	return m;
    }

    public int getMaxMessageSize() {
	return RegularMessage.MAX_PAYLOAD_SIZE;
    }
    
    private Listener listener;

    public Listener getListener() {
	return listener;
    }

    public void setListener(Listener listener) {
	this.listener = listener;
    }

    public SRPConnection(long storedConfigurationId,
			 Processor processor,
			 String props) {
	this.storedConfigurationId = storedConfigurationId;
	this.processor = processor;
	this.sent = new Queue();
	//defaults
	tokenDroppedTimeout = new TokenDroppedTimeout(DEFAULT_TOKEN_DROPPED_TIMEOUT);
	tokenLossTimeout = new TokenLossTimeout(DEFAULT_TOKEN_LOSS_TIMEOUT);
	joinTimeout = new JoinTimeout(DEFAULT_JOIN_TIMEOUT);
	consensusTimeout = new ConsensusTimeout(DEFAULT_CONSENSUS_TIMEOUT);
	if (DEBUG) log("Alarms created");
	setMaxAccepted(DEFAULT_INPUT_BUFFER_SIZE);
	setSocketTimeout(DEFAULT_SOCKET_TIMEOUT);
	setMaxSent(DEFAULT_MAX_SENT);
	setWindowSize(DEFAULT_WINDOW_SIZE);
	//passing Listener to constructor would preclude
	//creating chains of Connection+Listener objects
	setListener(new DefaultListener());
	//parse properties
	if (props == null) {
	    throw new IllegalArgumentException("Property string is null");
	}
	String[] pairs = props.split("&");
	for (int i = 0; i < pairs.length; i++) {
	    String[] tmp = pairs[i].split("=");
	    try {
		String name = tmp[0];
		String value = tmp[1];
		if (name.equals(PROP_CONSENSUS_TIMEOUT)) {
		    setConsensusTimeout(Long.parseLong(value));
		} else if (name.equals(PROP_JOIN_TIMEOUT)) {
		    setJoinTimeout(Long.parseLong(value));
		} else if (name.equals(PROP_TOKEN_DROPPED_TIMEOUT)) {
		    setTokenDroppedTimeout(Long.parseLong(value));
		} else if (name.equals(PROP_TOKEN_LOSS_TIMEOUT)) {
		    setTokenLossTimeout(Long.parseLong(value));
		} else if (name.equals(PROP_WINDOW_SIZE)) {
		    setWindowSize(Integer.parseInt(value));
		} else if (name.equals(PROP_IP)) {
		    try {
			ip = InetAddress.getByName(value);
			if (!ip.isMulticastAddress()) {
			    throw new IllegalArgumentException("Not a multicast address: " + value);
			}
		    } catch (Exception e) {
			throw new IllegalArgumentException("Invalid ip: " + value, e);
		    }
		} else if (name.equals(PROP_NIC)) {
		    try {
			nic = NetworkInterface.getByName(value);
			if (nic == null) {
			    String[] netAndMask = value.split("/");
			    nic = Util.findNetworkInterface(netAndMask[0], netAndMask[1]);
			    if (nic == null) {
				throw new IllegalArgumentException("No such nic: " + value);
			    }
			}
		    } catch (SocketException e) {
			throw new IllegalArgumentException("Bad nic?: " + value, e);
		    }
		} else if (name.equals(PROP_PORT)) {
		    port = Integer.parseInt(value);
		} else if (name.equals(PROP_DEBUG)) {
		    setDebug((new Boolean(value)).booleanValue());
		} else {
		    throw new IllegalArgumentException("Invalid property: " + name);
		}
	    } catch (ArrayIndexOutOfBoundsException e) {
		throw new IllegalArgumentException("Invalid property string: " + props);
	    }
	}
	if (port == 0) {
	    throw new IllegalArgumentException("Missing required property: " + PROP_PORT);
	}
	if (ip == null) {
	    throw new IllegalArgumentException("Missing required property: " + PROP_IP);
	}
	if (nic == null) {
	    LinkedList nics = new LinkedList();
	    try {
		Enumeration e = NetworkInterface.getNetworkInterfaces();
		while (e.hasMoreElements()) {
		    NetworkInterface nic = (NetworkInterface) e.nextElement(); 
		    if (!((InetAddress) nic.getInetAddresses().nextElement()).isLoopbackAddress()) {
			nics.add(nic);
		    }
		}
		if (nics.size() > 1) {
		    throw new IllegalArgumentException("You have more than one network adapter. " +
						       "You must specify one using property: " +
						       PROP_NIC);
		} else if (nics.size() == 0) {
		    throw new RuntimeException("You have no usable network adapters");
		} else {
		    nic = (NetworkInterface) nics.get(0);
		}
	    } catch (SocketException e) {
		throw new IllegalArgumentException("Cannot determine network adapter. " +
						   "Please pass it in explicitly.", e);
	    }
	}
    }

    private SocketAdapter socket;

    public SocketAdapter getSocketAdapter() {
	return socket;
    }

    private TokenMessage cachedToken;
    private Processor nextProcessor;

    public void forwardToken() {
	forwardToken(cachedToken, nextProcessor);
    }

    public void forwardToken(TokenMessage token, Processor nextProcessor) {	
	this.cachedToken = token;
	this.nextProcessor = nextProcessor;
	token.setDestination(nextProcessor);
	if (DEBUG) {
	    if (token instanceof RegularTokenMessage) {
		log("Forwarding regular token to: " + nextProcessor);
	    } else if (token instanceof CommitTokenMessage) {
		log("Forwarding commit token: " + token);
	    }
	}
	socket.send(token);
    }

    private SRPState state;

    public SRPState getState() {
	return state;
    }

    public void setState(SRPState state) {
	this.state = state;
	if (DEBUG) log("Switched to " + state);
    }

    private Processor processor;
    
    public Processor getProcessor() {
	return processor;
    }

    private SRPConfiguration configuration;

    public long getConfigurationId() {
	return configuration.getId();
    }

    public void install(SRPConfiguration configuration) {
	if (DEBUG) log("Installing configuration: " + configuration);
	this.configuration = configuration;
    }

    /**
     * Maximum number messages in the out queue
     * before we start blocking.
     */
    private int maxSent;

    public void setMaxSent(int maxSent) {
	this.maxSent = maxSent;
    }

    public static final int DEFAULT_MAX_SENT = 100;

    /**
     * The queue of outgoing RegularMessages.
     */
    private Queue sent;

    public Queue getSent() {
	return sent;
    }

    public void send(evs4j.Message message) {
	Message m = (Message) message;
	while (true) {
	    if (sent.length() < maxSent) {
		//there is room in the queue
		sent.add(m);
		break;
	    } else {
		//queue is full 
		//queue grows only in
		//COMMIT and RECOVERY states
		try {
		    //any small duration works here
		    Thread.sleep(1);
		} catch (InterruptedException e) {
		    //ignore
		}
	    }
	}
    }

    private Thread thread;

    private boolean terminate;

    public void close() {
	if (DEBUG) log("Closing connection");
	terminate = true;
	//thread will clean up
	//upon stopping
    }

    private boolean reset;

    public void reset() {
	this.reset = true;
    }

    public void open() throws IOException {
	if (DEBUG) log("joinTimeout = " + joinTimeout + "ms");
	if (DEBUG) log("tokenDroppedTimeout = " + tokenDroppedTimeout + "ms");
	if (DEBUG) log("tokenLossTimeout = " + tokenLossTimeout + "ms");
	if (DEBUG) log("consensusTimeout = " + consensusTimeout + "ms");
	//create broadcast socket
	socket = new SocketAdapter(this);
	if (DEBUG) log("Created socket");
	//use stored configuration id or create new one
	long configurationId = storedConfigurationId;
	if (configurationId == 0) {
	    configurationId = SRPConfiguration.toConfigurationId(processor.getValue(), 1);
	}
	boolean transitional = false;
	SRPConfiguration configuration = new SRPConfiguration(this, new ProcessorSet(processor), configurationId, transitional);
	install(configuration);
	//start thread
	thread = new Thread(this);
	thread.start();
    }

    public void run() {
	//run membership protocol
	reset();
	if (DEBUG) log("Connection thread started");
	while (true) {
	    if (reset) {
		//need a configuration
		reset = false;
		discover();
	    }
	    if (terminate) {
		//close socket
		try {
		    socket.close();
		} catch (Exception e) {
		    e.printStackTrace();
		    //ignore
		}
		if (DEBUG) log("Connection closed");
		//exit main loop
		break;
	    }
	    Message message = socket.receive();
	    if (message != null) {
		message.execute(this, state);
	    } else {
		//no messages waiting to be processed
		//check timeouts		
		long now = System.currentTimeMillis();
		tokenDroppedTimeout.execute(this, state, now);
		tokenLossTimeout.execute(this, state, now);
		joinTimeout.execute(this, state, now);
		consensusTimeout.execute(this, state, now);
	    }
	}
    }

    public void log(String s) {
	if (debug) {
	    String tmp = "";
	    tmp = " (" + state + ")";
	    System.err.println(processor + tmp + " " + s);
	}
    }

}

