
import evs4j.*;

public class Example implements Listener {
    
    public void onMessage(Message message) {
	String text = new String(message.getData(),
				 message.getOffset(),
				 message.getLength());
	System.err.println("received message from " + message.getSender() + " : " + text);
    }
    
    public void onConfiguration(Configuration configuration) {
	if (configuration.isTransitional()) {
	    System.err.println("installed transitional configuration: " + configuration);
	} else {
	    System.err.println("installed regular configuration: " + configuration);
	}
    }

    public void onAlert(Alert alert) { }

    public static void main(String[] args) throws Exception {

	//this should be on stable storage as per article
	long lastKnownConfigurationId = 0L;

	//group member '1'
	Processor proc = new Processor(1);

	//totem-specific configuration parameters
	String props = "port=9100&ip=225.0.0.1&nic=192.168.254.0/255.255.255.0";

	Connection conn =
	    new evs4j.impl.SRPConnection(lastKnownConfigurationId,
					 proc,
					 props);

	//the default listener just discards messages
	conn.setListener(new Example());
	conn.open();
	
	//send a message
	boolean agreed = false;
	boolean safe = true;
	Message message = conn.createMessage(agreed);
	byte[] data = "Hello World!".getBytes();
	System.arraycopy(data,
			 0,
			 message.getData(),
			 message.getOffset(),
			 data.length);
	message.setLength(data.length);
	conn.send(message);
    }

}
