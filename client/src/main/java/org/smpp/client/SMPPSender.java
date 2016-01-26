package org.smpp.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import org.smpp.Data;
import org.smpp.Session;
import org.smpp.TCPIPConnection;
import org.smpp.pdu.Address;
import org.smpp.pdu.AddressRange;
import org.smpp.pdu.BindRequest;
import org.smpp.pdu.BindResponse;
import org.smpp.pdu.BindTransmitter;
import org.smpp.pdu.EnquireLink;
import org.smpp.pdu.EnquireLinkResp;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.SubmitSMResp;
import org.smpp.pdu.UnbindResp;
import org.smpp.pdu.WrongLengthOfStringException;

/**
 * Class <code>SMPPTest</code> shows how to use the SMPP toolkit.
 * You can bound and unbind from the SMSC, you can send every possible 
 * pdu and wait for a pdu sent from the SMSC.
 *
 * @author Logica Mobile Networks SMPP Open Source Team
 * @version $Revision: 1.2 $
 */
public class SMPPSender {

	/**
	 * File with default settings for the application.
	 */
	// static String propsFilePath = "./smppsender.cfg";
	static String propsFilePath = "../etc/smppsender.cfg";

	/**
	 * This is the SMPP session used for communication with SMSC.
	 */
	static Session session = null;

	/**
	 * Contains the parameters and default values for this test
	 * application such as system id, password, default npi and ton
	 * of sender etc.
	 */
	Properties properties = new Properties();

	/**
	 * If the application is bound to the SMSC.
	 */
	boolean bound = false;

	/**
	 * Address of the SMSC.
	 */
	String ipAddress = null;

	/**
	 * The port number to bind to on the SMSC server.
	 */
	int port = 0;

	/**
	 * The name which identifies you to SMSC.
	 */
	String systemId = null;

	/**
	 * The password for authentication to SMSC.
	 */
	String password = null;

	/**
	 * How you want to bind to the SMSC: transmitter (t), receiver (r) or
	 * transciever (tr). Transciever can both send messages and receive
	 * messages. Note, that if you bind as receiverma you can still receive
	 * responses to you requests (submissions).
	 */
	String bindOption = "t";

	/**
	 * The range of addresses the smpp session will serve.
	 */
	AddressRange addressRange = new AddressRange();

	/*
	 * for information about these variables have a look in SMPP 3.4
	 * specification
	 */
	String systemType = "";
	String serviceType = "";
	Address sourceAddress = new Address();
	Address destAddress = new Address();
	String scheduleDeliveryTime = "";
	String validityPeriod = "";
	String shortMessage = "";
	int numberOfDestination = 1;
	String messageId = "";

//    byte esmClass = 0;
    // Set on true UDHI
//    byte esmClass = (byte) 64; // 0100 0000 // app to movile
     byte esmClass = (byte) 192; // 1100 0000

	byte protocolId = 0;
	byte priorityFlag = 0;
	byte registeredDelivery = 0;
	byte replaceIfPresentFlag = 0;
	byte dataCoding = 0;
	byte smDefaultMsgId = 0;

    /**
     * Request to SMSC an delivery confirmation to SME.
     */
    byte registered_delivery = 0x1;

	/**
	 * If you attemt to receive message, how long will the application
	 * wait for data.
	 */
	long receiveTimeout = Data.RECEIVE_BLOCKING;

	/**
	 * Initialises the application, lods default values for
	 * connection to SMSC and for various PDU fields.
	 */
	public SMPPSender() throws IOException {
		loadProperties(propsFilePath);
	}

	/**
	 * Sets global SMPP library debug and event objects.
	 * Runs the application.
	 * @see SmppObject#setDebug(Debug)
	 * @see SmppObject#setEvent(Event)
	 */
	public static void main(String args[]) {
        System.out.println("***** " + System.getProperty("user.dir") + " *****");
		// Parse the command line
		String sender = null;
		byte senderTon = (byte) 0;
		byte senderNpi = (byte) 0;
		String dest = null;
		String message = null;
		
		for(int i=0; i<args.length; i++) {
			if(args[i].startsWith("-")) {
				String opt = args[i].substring(1);
				if(opt.compareToIgnoreCase("sender") == 0) {
					sender = args[++i];
				} else if(opt.compareToIgnoreCase("senderTon") == 0) {
					senderTon = Byte.parseByte(args[++i]);
				} else if(opt.compareToIgnoreCase("senderNpi") == 0) {
					senderNpi = Byte.parseByte(args[++i]);
				} else if(opt.compareToIgnoreCase("dest") == 0) {
					dest = args[++i];
				} else if(opt.compareToIgnoreCase("destination") == 0) {
					dest = args[++i];
				} else if(opt.compareToIgnoreCase("message") == 0) {
					message = args[++i];
				} else if(opt.compareToIgnoreCase("file") == 0) {
					propsFilePath = args[++i];
				}
			}
		}

		if((dest == null) || (message == null)) {
			System.out.println("Usage: SMPPSender -dest <dest number on international format> -message <the message, within qoutes if contains whitespaces> [-sender <sender id> [-senderTon <sender ton>] [-senderNpi <sender npi>]]");
			System.exit(0);
		}

		// Dest may contain comma-separated numbers
		Collection<String> destinations = new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(dest,",");
		while(st.hasMoreTokens()){
			String d = st.nextToken();
			destinations.add(d);
		}
		
		System.out.println("Initialising...");
		SMPPSender smppSender = null;
		try {
			smppSender = new SMPPSender();
		} catch (IOException e) {
			System.out.println("Exception initialising SMPPSender " + e);
		}

		System.out.println("Sending: \"" + message + "\" to " + dest);
		// TODO only check bind
		if (smppSender != null) {
		    smppSender.bind();
		    System.out.println("bind()...");
		    try {
                Thread.sleep(3000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
		    
		    if (smppSender.bound) {
		        smppSender.unbind();
		        System.out.println("unbind()...");
		    }
		}
//		if (smppSender != null) {
//			smppSender.bind();
//
//			if(smppSender.bound) {
//				Iterator<String> it = destinations.iterator();
//				while(it.hasNext()) {
//					String d = it.next();
//					smppSender.submit(d, message, sender, senderTon, senderNpi);
//				}
//				smppSender.unbind();
//			}
//		}
	}

	/**
	 * The first method called to start communication
	 * betwen an ESME and a SMSC. A new instance of <code>TCPIPConnection</code>
	 * is created and the IP address and port obtained from user are passed
	 * to this instance. New <code>Session</code> is created which uses the created
	 * <code>TCPIPConnection</code>.
	 * All the parameters required for a bind are set to the <code>BindRequest</code>
	 * and this request is passed to the <code>Session</code>'s <code>bind</code>
	 * method. If the call is successful, the application should be bound to the SMSC.
	 *
	 * See "SMPP Protocol Specification 3.4, 4.1 BIND Operation."
	 * @see BindRequest
	 * @see BindResponse
	 * @see TCPIPConnection
	 * @see Session#bind(BindRequest)
	 * @see Session#bind(BindRequest,ServerPDUEventListener)
	 */
	private void bind() {
		try {
			if (bound) {
				System.out.println("Already bound, unbind first.");
				return;
			}

			BindRequest request = null;
			BindResponse response = null;

			request = new BindTransmitter();

			TCPIPConnection connection = new TCPIPConnection(ipAddress, port);
			connection.setReceiveTimeout(20 * 1000);
			session = new Session(connection);

			// set values
			request.setSystemId(systemId);
			request.setPassword(password);
			request.setSystemType(systemType);
			request.setInterfaceVersion((byte) 0x34);
			request.setAddressRange(addressRange);

			// send the request
			System.out.println("Bind request " + request.debugString());
			response = session.bind(request);
			System.out.println("Bind response " + response.debugString());
			if (response.getCommandStatus() == Data.ESME_ROK) {
				bound = true;
			} else {
				System.out.println("Bind failed, code " + response.getCommandStatus());
			}
		} catch (Exception e) {
			System.out.println("Bind operation failed. " + e);
		}
	}

	/**
	 * Ubinds (logs out) from the SMSC and closes the connection.
	 *
	 * See "SMPP Protocol Specification 3.4, 4.2 UNBIND Operation."
	 * @see Session#unbind()
	 * @see Unbind
	 * @see UnbindResp
	 */
	private void unbind() {
		try {

			if (!bound) {
				System.out.println("Not bound, cannot unbind.");
				return;
			}

			// send the request
			System.out.println("Going to unbind.");
			if (session.getReceiver().isReceiver()) {
				System.out.println("It can take a while to stop the receiver.");
			}
			UnbindResp response = session.unbind();
			System.out.println("Unbind response " + response.debugString());
			bound = false;
		} catch (Exception e) {
			System.out.println("Unbind operation failed. " + e);
		}
	}

	/**
	 * Creates a new instance of <code>SubmitSM</code> class, lets you set
	 * subset of fields of it. This PDU is used to send SMS message
	 * to a device.
	 *
	 * See "SMPP Protocol Specification 3.4, 4.4 SUBMIT_SM Operation."
	 * @see Session#submit(SubmitSM)
	 * @see SubmitSM
	 * @see SubmitSMResp
	 */
	private void submit(String destAddress, String shortMessage, String sender, byte senderTon, byte senderNpi) {
		try {
			SubmitSM request = new SubmitSM();
			SubmitSMResp response;

			// set values
			request.setServiceType(serviceType);

			if(sender != null) {
				if(sender.startsWith("+")) {
					sender = sender.substring(1);
					senderTon = 1;
					senderNpi = 1;
				}
				if(!sender.matches("\\d+")) {
					senderTon = 5;
					senderNpi = 0;
				}
					
				if(senderTon == 5) {
					request.setSourceAddr(new Address(senderTon, senderNpi, sender, 11));
				} else {
					request.setSourceAddr(new Address(senderTon, senderNpi, sender));
				}
			} else {
				request.setSourceAddr(sourceAddress);
			}

			if(destAddress.startsWith("+")) {
				destAddress = destAddress.substring(1);
			}
			request.setDestAddr(new Address((byte)1, (byte)1, destAddress));
			request.setReplaceIfPresentFlag(replaceIfPresentFlag);



            // TODO
            // 06 05 04 23 F4 00 00
            //byte[] msg = {(byte)0x06, (byte)0x05, (byte)0x04, (byte)0x23, (byte)0xF4, (byte)0x00, (byte)0x00};
//            byte[] msg = {0x06, 0x05, 0x04, 0x15, 0x79, 0x00, 0x00, 0x52, 0x45, 0x47, 0x2d, 0x52, 0x45, 0x53, 0x50, 0x3f, 0x76, 0x3d, 0x33, 0x3b,
//					0x72, 0x3d, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x3b, 0x6e, 0x3d, 0x2b, 0x35, 0x39, 0x31, 0x37, 0x30, 0x37, 0x31,
//					0x30, 0x31, 0x39, 0x34, 0x3b, 0x73, 0x3d, 0x4f, 0x4d, 0x54, 0x45, 0x53, 0x54, 0x31, 0x32, 0x33, 0x34, 0x41, 0x42, 0x43, 0x44,
//					0x41, 0x42, 0x43, 0x44, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x41, 0x42, 0x43, 0x44, 0x41, 0x42, 0x43,
//					0x44, 0x41, 0x42, 0x43, 0x44, 0x39, 0x38, 0x37, 0x36, 0x35, 0x34, 0x33, 0x32, 0x31, 0x31, 0x32, 0x33, 0x78, 0x31, 0x34, 0x31,
//					0x35, 0x34, 0x39};
            byte[] msg = {
                    0x06, 0x05, 0x04, 0x15, 0x79, 0x00, 0x00, 0x52, 0x45, 0x47, 0x2d, 0x52, 0x45, 0x53, 0x50, 0x3f, 0x76, 0x3d, 0x33, 0x3b,
                    0x72, 0x3d, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x3b, 0x6e, 0x3d, 0x2b, 0x35, 0x39, 0x31, 0x37, 0x30, 0x30,
                    0x30, 0x30, 0x30, 0x30, 0x31, 0x3b, 0x73, 0x3d, 0x4f, 0x4d, 0x54, 0x45, 0x53, 0x54, 0x31, 0x32, 0x33, 0x34, 0x41, 0x42,
                    0x43, 0x44, 0x41, 0x42, 0x43, 0x44, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x41, 0x42, 0x43, 0x44,
                    0x41, 0x42, 0x43, 0x44, 0x41, 0x42, 0x43, 0x44, 0x39, 0x38, 0x37, 0x36, 0x35, 0x34, 0x33, 0x32, 0x31, 0x31, 0x32, 0x33,
                    0x78, 0x31, 0x34, 0x31, 0x35, 0x31, 0x37};

            String msgStr = new String(msg);


//			request.setShortMessage(shortMessage,Data.ENC_UTF8);
            request.setShortMessage(msgStr, Data.ENC_GSM7BIT);

            // byte array message message
            byte[] arrMsg = msgStr.getBytes();
            for (Byte c : arrMsg) {
                System.out.print((int) (c & 0xFF) + " - " + Integer.toHexString((int) (c & 0xFF)));
            }
            System.out.println("");

            Date now = new Date();
            String strvalidityPeriod = getValidityPeriod(new Date(now.getTime() + 3*60*1000));
            System.out.println("<--------- " + strvalidityPeriod + " ----------->");
			request.setScheduleDeliveryTime(scheduleDeliveryTime);
//			request.setValidityPeriod(validityPeriod);
            request.setValidityPeriod(strvalidityPeriod);
			// TODO
			request.setEsmClass(esmClass);
			request.setProtocolId(protocolId);
			request.setPriorityFlag(priorityFlag);
			request.setRegisteredDelivery(registeredDelivery);
			request.setDataCoding(dataCoding);
			request.setSmDefaultMsgId(smDefaultMsgId);

            // set registered_delivery
            request.setRegisteredDelivery(registered_delivery);

			// send the request
			request.assignSequenceNumber(true);
			System.out.println("Submit request " + request.debugString());
			// Check async call??
			response = session.submit(request);
			System.out.println("Submit response " + response.debugString());
			messageId = response.getMessageId();
			enquireLink();
		} catch (Exception e) {
			System.out.println("Submit operation failed. " + e);
		}
	}

    private String getValidityPeriod(Date tmp) {
        DateFormat df = new SimpleDateFormat("yyMMddhhmmss000-");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(tmp);
    }

	/**
	 * Creates a new instance of <code>EnquireSM</code> class.
	 * This PDU is used to check that application level of the other party
	 * is alive. It can be sent both by SMSC and ESME.
	 *
	 * See "SMPP Protocol Specification 3.4, 4.11 ENQUIRE_LINK Operation."
	 * @see Session#enquireLink(EnquireLink)
	 * @see EnquireLink
	 * @see EnquireLinkResp
	 */
	private void enquireLink() {
		try {
			EnquireLink request = new EnquireLink();
			EnquireLinkResp response;
			System.out.println("Enquire Link request " + request.debugString());
			response = session.enquireLink(request);
			System.out.println("Enquire Link response " + response.debugString());
		} catch (Exception e) {
			System.out.println("Enquire Link operation failed. " + e);
		}
	}

	/**
	 * Loads configuration parameters from the file with the given name.
	 * Sets private variable to the loaded values.
	 */
	private void loadProperties(String fileName) throws IOException {
		System.out.println("Reading configuration file " + fileName + "...");
		FileInputStream propsFile = new FileInputStream(fileName);
		properties.load(propsFile);
		propsFile.close();
		System.out.println("Setting default parameters...");
		byte ton;
		byte npi;
		String addr;
		String bindMode;
		int rcvTimeout;

		ipAddress = properties.getProperty("ip-address");
		port = getIntProperty("port", port);
		systemId = properties.getProperty("system-id");
		password = properties.getProperty("password");

		ton = getByteProperty("addr-ton", addressRange.getTon());
		npi = getByteProperty("addr-npi", addressRange.getNpi());
		addr = properties.getProperty("address-range", addressRange.getAddressRange());
		addressRange.setTon(ton);
		addressRange.setNpi(npi);
		try {
			addressRange.setAddressRange(addr);
		} catch (WrongLengthOfStringException e) {
			System.out.println("The length of address-range parameter is wrong.");
		}

		ton = getByteProperty("source-ton", sourceAddress.getTon());
		npi = getByteProperty("source-npi", sourceAddress.getNpi());
		addr = properties.getProperty("source-address", sourceAddress.getAddress());
		setAddressParameter("source-address", sourceAddress, ton, npi, addr);

		ton = getByteProperty("destination-ton", destAddress.getTon());
		npi = getByteProperty("destination-npi", destAddress.getNpi());
		addr = properties.getProperty("destination-address", destAddress.getAddress());
		setAddressParameter("destination-address", destAddress, ton, npi, addr);

		serviceType = properties.getProperty("service-type", serviceType);
		systemType = properties.getProperty("system-type", systemType);
		bindMode = properties.getProperty("bind-mode", bindOption);
		if (bindMode.equalsIgnoreCase("transmitter")) {
			bindMode = "t";
		} else if (bindMode.equalsIgnoreCase("receiver")) {
			bindMode = "r";
		} else if (bindMode.equalsIgnoreCase("transciever")) {
			bindMode = "tr";
		} else if (
			!bindMode.equalsIgnoreCase("t") && !bindMode.equalsIgnoreCase("r") && !bindMode.equalsIgnoreCase("tr")) {
			System.out.println(
				"The value of bind-mode parameter in "
					+ "the configuration file "
					+ fileName
					+ " is wrong. "
					+ "Setting the default");
			bindMode = "t";
		}
		bindOption = bindMode;
        System.out.println("===== " + bindOption + " =====");

		// receive timeout in the cfg file is in seconds, we need milliseconds
		// also conversion from -1 which indicates infinite blocking
		// in the cfg file to Data.RECEIVE_BLOCKING which indicates infinite
		// blocking in the library is needed.
		if (receiveTimeout == Data.RECEIVE_BLOCKING) {
			rcvTimeout = -1;
		} else {
			rcvTimeout = ((int) receiveTimeout) / 1000;
		}
		rcvTimeout = getIntProperty("receive-timeout", rcvTimeout);
		if (rcvTimeout == -1) {
			receiveTimeout = Data.RECEIVE_BLOCKING;
		} else {
			receiveTimeout = rcvTimeout * 1000;
		}
	}

	/**
	 * Gets a property and converts it into byte.
	 */
	private byte getByteProperty(String propName, byte defaultValue) {
		return Byte.parseByte(properties.getProperty(propName, Byte.toString(defaultValue)));
	}

	/**
	 * Gets a property and converts it into integer.
	 */
	private int getIntProperty(String propName, int defaultValue) {
		return Integer.parseInt(properties.getProperty(propName, Integer.toString(defaultValue)));
	}

	/**
	 * Sets attributes of <code>Address</code> to the provided values.
	 */
	private void setAddressParameter(String descr, Address address, byte ton, byte npi, String addr) {
		address.setTon(ton);
		address.setNpi(npi);
		try {
			address.setAddress(addr);
		} catch (WrongLengthOfStringException e) {
			System.out.println("The length of " + descr + " parameter is wrong.");
		}
	}
}
/*
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2004/09/12 12:37:40  sverkera
 * Added
 *
 */
