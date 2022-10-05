package gov.dhs.cisa.flare.misp;

import java.io.StringWriter;
import java.time.Instant;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class Taxii11Request {

	private static Logger log = LoggerFactory.getLogger(Taxii11Response.class);
	public static final String XMLNS_TAXII_11 = "xmlns_taxii_11";
	public static final String XMLNS_BINDING = "http://taxii.mitre.org/messages/taxii_xml_binding-1.1";
	public static final String POLL_REQUEST = "taxii_11:Poll_Request";
	public static final String EXCLUSIVE_BEGIN_TIMESTAMP = "taxii_11:Exclusive_Begin_Timestamp";
	public static final String INCLUSIVE_BEGIN_TIMESTAMP = "taxii_11:Inclusive_End_Timestamp";
	public static final String POLL_PARAMETERS = "taxii_11:Poll_Parameters";
	public static final String RESPONSE_TYPE = "taxii_11:Response_Type";
	public static final String MESSAGE_ID = "message_id";
	public static final String COLLECTION_NAME = "collection_name";
	//public static final String COLLECTION_NAME = "multi-binding-fixed";

	public static HttpHeaders makeHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_XML);
		headers.set("X-TAXII-Content-Type", "urn:taxii.mitre.org:message:xml:1.1");
		headers.set("X-TAXII-Accept", "urn:taxii.mitre.org:message:xml:1.1");
		headers.set("X-TAXII-Services", "urn:taxii.mitre.org:services:1.1");
		headers.set("X-TAXII-Protocol", "urn:taxii.mitre.org:protocol:http:1.0");
		return headers;
	}

	public static String toXml(Node node) throws TransformerException {
		DOMSource domSource = new DOMSource(node);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		transformer.transform(domSource, result);
		return writer.toString();

	}

	public static String makeBody(Instant dateStart, Instant dateStop, String collectionName, Boolean sync,
			String respType) throws ParserConfigurationException, TransformerException {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = dbf.newDocumentBuilder();
		Document doc = builder.newDocument();

		// create the root element node
		Element root = doc.createElementNS(XMLNS_BINDING, POLL_REQUEST);
		root.setAttribute(XMLNS_TAXII_11, XMLNS_BINDING);
		root.setAttribute(MESSAGE_ID, "1");
		root.setAttribute(COLLECTION_NAME, collectionName);

		Element node = doc.createElement(EXCLUSIVE_BEGIN_TIMESTAMP);
		Text text = doc.createTextNode(dateStart.toString());
		node.appendChild(text);
		root.appendChild(node);

		node = doc.createElement(INCLUSIVE_BEGIN_TIMESTAMP);
		text = doc.createTextNode(dateStop.toString());
		node.appendChild(text);
		root.appendChild(node);

		node = doc.createElement(POLL_PARAMETERS);
		node.setAttribute("allow_async", sync.toString());
		Element respTypeElem = doc.createElementNS(XMLNS_TAXII_11, RESPONSE_TYPE);
		text = doc.createTextNode(respType);
		respTypeElem.appendChild(text);
		node.appendChild(respTypeElem);
		root.appendChild(node);

		String body = toXml(root);
		log.info("request body:\n\n{}", body);
		return body;
	}
}
