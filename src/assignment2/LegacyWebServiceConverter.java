package assignment2;

import java.util.Arrays;

import javax.xml.soap.*;

import org.w3c.dom.NodeList;

/**
 * Converter from CNMP to SNMP.
 * This class supports a legacy system that can request the value of a CNMP object.
 * The class will lookup the corresponding SNMP value and return it to the legacy service.
 * For simplicity reasons, this implementation will not be able to receive requests,
 * but will instead query for a request on start and shutdown after an answer has been sent back.
 */
public abstract class LegacyWebServiceConverter {

	/** Simple name for the namespace */
	private static final String MANAGEMENTSERVER_NAMESPACE_PREFIX = "managementserver";
	/** URL for getObject() service */
	private static final String GET_OBJECT_URL =
			"http://ttm4128.item.ntnu.no:8080/axis2/services/ManagementServer/getObject";
	/** URL for sendValue(String, String) service */
	private static final String SEND_VALUE_URL =
			"http://ttm4128.item.ntnu.no:8080/axis2/services/ManagementServer/sendValue";

	/**
	 * Create a SOAPMessage object for communicating with the SOAP service
	 * @return	A clean SOAPMessage
	 * @throws SOAPException
	 */
	public static SOAPMessage createSoapMessage() throws SOAPException {
		final MessageFactory messageFactory = MessageFactory.newInstance();
		final SOAPMessage soapMessage = messageFactory.createMessage();

		final SOAPPart soapPart = soapMessage.getSOAPPart();
		final SOAPEnvelope envelope = soapPart.getEnvelope();

		envelope.addNamespaceDeclaration("xsd", "http://www.w3.org/2001/XMLSchema");
		envelope.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
		envelope.addNamespaceDeclaration("enc", "http://schemas.xmlsoap.org/soap/encoding/");
		envelope.addNamespaceDeclaration("env", "http://schemas.xmlsoap.org/soap/envelop/");

		envelope.addNamespaceDeclaration(MANAGEMENTSERVER_NAMESPACE_PREFIX, "http://managementserver.com");

		envelope.setEncodingStyle("http://schemas.xmlsoap.org/soap/encoding/");

		return soapMessage;
	}
	
	/**
	* Create a SOAP Connection object to transport a SOAPMessage.
	* @return	a new SOAPConnection
	* @throws UnsupportedOperationException
	* @throws SOAPException
	*/
	public static SOAPConnection getSoapConnection() throws UnsupportedOperationException, SOAPException {
		final SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
		final SOAPConnection soapConnection = soapConnectionFactory.createConnection();

		return soapConnection;
	}

	/**
	 * Get a request for a CNMP object from the legacy service
	 * @return	the CNMPRequest
	 * @throws SOAPException
	 */
	public static CNMPRequest getCNMPRequest() throws SOAPException {
		final SOAPConnection soapConnection = getSoapConnection();
		final SOAPMessage soapMessage = createSoapMessage();
		final SOAPMessage soapMessageReply = soapConnection.call(soapMessage, GET_OBJECT_URL);
		final NodeList nodes = soapMessageReply.getSOAPBody().getFirstChild().getChildNodes();
		soapConnection.close();
		assert nodes.getLength() == 2;
		assert "ns:return".equals(nodes.item(0).getNodeName());
		assert "ns:return".equals(nodes.item(1).getNodeName());
		return new CNMPRequest(
				nodes.item(0).getTextContent(),
				nodes.item(1).getTextContent()
			);
	}
	
	/**
	 * Send a value to the legacy system
	 * @param requestId	The requestId included in the CNMPRequest that triggered this send
	 * @param objectValue	The value of the requested object
	 * @return	The string returned opun sending the value
	 * @throws SOAPException
	 */
	public static String sendValue(String requestId, String objectValue) throws SOAPException {
		final SOAPMessage soapMessage = createSoapMessage();
		final SOAPBody soapBody = soapMessage.getSOAPBody();
		final SOAPElement loginElement = soapBody.addChildElement("Login", MANAGEMENTSERVER_NAMESPACE_PREFIX);

		loginElement.addChildElement("requestID", MANAGEMENTSERVER_NAMESPACE_PREFIX).addTextNode(requestId);
		loginElement.addChildElement("objectValue", MANAGEMENTSERVER_NAMESPACE_PREFIX).addTextNode(objectValue);

		soapMessage.saveChanges();

		final SOAPConnection soapConnection = getSoapConnection();
		final SOAPMessage soapMessageReply = soapConnection.call(soapMessage,SEND_VALUE_URL);
		String textContent = soapMessageReply.getSOAPBody().getTextContent();

		return textContent;
	}
	
	public static void main(String... args) {
		try {
			CNMPRequest request = getCNMPRequest();
			System.out.println(request.objectName);
			
			String[] mibArgs = Arrays.copyOf(args, args.length+2);
			mibArgs[args.length] = "-c";
			mibArgs[args.length+1] = request.objectName;
			
			MibObject snmpResult = CNMP2SNMPConverter.getMibObject(mibArgs);
			if (snmpResult != null)
				System.out.println(sendValue(request.requestID, snmpResult.value));
		} catch (SOAPException e) {
			e.printStackTrace();
		}
	}

}
