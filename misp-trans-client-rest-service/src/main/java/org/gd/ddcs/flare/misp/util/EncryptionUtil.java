package org.gd.ddcs.flare.misp.util;

import org.gd.ddcs.flare.misp.Config;
import xor.flare.utils.crypto.PasswordUtil;

public class EncryptionUtil extends Config {
	
	private EncryptionUtil() {
		
	}
	
	public static String decrypt (String string) {
		return PasswordUtil.getPlainTextPassword(string, Config.getProperty("encKey"));
	}
	
	public static String encrypt (String string) {
		return PasswordUtil.getEncryptedPassword(string, Config.getProperty("encKey"));
	}
}
