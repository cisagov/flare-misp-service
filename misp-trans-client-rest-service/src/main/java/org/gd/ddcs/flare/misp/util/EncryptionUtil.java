package org.gd.ddcs.flare.misp.util;

import org.gd.ddcs.flare.misp.Config;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Base64;

public class EncryptionUtil extends Config {

	private static final String seed = "35a06da398";
	
	private EncryptionUtil() {
		
	}
	
	public static String decrypt (String string) {
		return getPlainTextPassword(string, Config.getProperty("encKey"));
	}
	
	public static String encrypt (String string) {
		return getEncryptedPassword(string, Config.getProperty("encKey"));
	}

	public static String getEncryptedPassword(String plainTextPassword, String encKy) {
		if (plainTextPassword == null) {
			System.err.println("Password is null!");
			return null;
		}
		if (encKy == null) {
			System.err.println("Encryption key is null!");
			return null;
		}

		String encryptedString;
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");

			byte[] seedBytes = Base64.getEncoder().encode(seed.getBytes(StandardCharsets.UTF_8));
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] encryptionKeyBytes = md.digest(encKy.getBytes(StandardCharsets.UTF_8));

			AlgorithmParameterSpec iv = new IvParameterSpec(seedBytes);
			SecretKeySpec key = new SecretKeySpec(encryptionKeyBytes, "AES");
			cipher.init(Cipher.ENCRYPT_MODE, key, iv);
			byte[] plainTextPasswordBytes = Base64.getEncoder().encode(plainTextPassword.getBytes(StandardCharsets.UTF_8));
			byte[] encryptedBytes = cipher.doFinal(plainTextPasswordBytes);
			encryptedString = Base64.getEncoder().encodeToString(encryptedBytes);
		} catch (InvalidAlgorithmParameterException |
				NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | NoSuchProviderException
				| IllegalBlockSizeException e) {
			System.err.println("Could not encrypt password for storage - " + e.getMessage());
			return null;
		} catch (InvalidKeyException e) {
			System.err.println("Could not encrypt password for storage. Make sure Java Cryptography Extension (JCE) Unlimited Strength is installed in your JAVA's jre/lib/security directory. Message: " + e.getMessage());
			return null;
		}

		return encryptedString;
	}

	public static String getPlainTextPassword(String hashedPassword, String encKy) {
		if (hashedPassword == null) {
			System.err.println("Password is null!");
			return null;
		}
		if (encKy == null) {
			System.err.println("Encryption key is null!");
			return null;
		}

		String decryptedString;
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "SunJCE");
			byte[] seedBytes = Base64.getEncoder().encode(seed.getBytes(StandardCharsets.UTF_8));

			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] encryptionKeyBytes = md.digest(encKy.getBytes(StandardCharsets.UTF_8));

			AlgorithmParameterSpec iv = new IvParameterSpec(seedBytes);
			SecretKeySpec key = new SecretKeySpec(encryptionKeyBytes, "AES");
			cipher.init(Cipher.DECRYPT_MODE, key, iv);
			byte[] hashedPasswordBytes = Base64.getDecoder().decode(hashedPassword);
			byte[] decryptedBytes = cipher.doFinal(hashedPasswordBytes);
			decryptedString = new String(Base64.getDecoder().decode(decryptedBytes), StandardCharsets.UTF_8);

		} catch (InvalidAlgorithmParameterException |
				NoSuchPaddingException | NoSuchAlgorithmException | BadPaddingException | NoSuchProviderException
				| IllegalBlockSizeException e) {
			System.err.println("Could not decrypt password for storage - " + e.getMessage());
			throw new RuntimeException(e);
		} catch (InvalidKeyException e) {
			String errorMessage = "Could not decrypt password for storage - " + e.getMessage() + ". Make sure Java Cryptography Extension (JCE) Unlimited Strength is installed in your JAVA's jre/lib/security directory.";
			System.err.println(errorMessage);
			throw new RuntimeException(errorMessage, e);
		}

		return decryptedString;
	}
}
