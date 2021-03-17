package gov.dhs.cisa.flare.misp;

import java.time.Instant;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.junit.Test;

public class Taxii11RequestTest {
	@Test
	public void testGetCollectionsRequest() throws ParserConfigurationException, TransformerException {
		//String request = Taxii11Request.makeBody(Instant.EPOCH, Instant.now(), "test_coll", false, "FULL");
	}
}
