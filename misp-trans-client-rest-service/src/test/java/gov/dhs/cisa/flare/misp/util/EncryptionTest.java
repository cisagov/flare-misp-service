package gov.dhs.cisa.flare.misp.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EncryptionTest {
	@Test
	public void test() {
		String password = "password";
		String encrypted = EncryptionUtil.encrypt(password);
		assertEquals(password, EncryptionUtil.decrypt(encrypted));
	}
}
