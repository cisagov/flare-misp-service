package org.gd.ddcs.flare.misp.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EncryptionTest {
	@Test
	public void test() {
		String password = "password";
		String encrypted = EncryptionUtil.encrypt(password);
		assertEquals(password, EncryptionUtil.decrypt(encrypted));
	}
}
