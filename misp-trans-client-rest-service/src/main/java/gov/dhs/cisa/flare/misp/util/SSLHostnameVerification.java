package gov.dhs.cisa.flare.misp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.net.ssl.*;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;


public class SSLHostnameVerification {

	private static final Logger log = LoggerFactory.getLogger(SSLHostnameVerification.class);

	@Value("${server.ssl.hostname.verify}")
	private static boolean hostnameVerify;

	public static void executeConfigration () {

		if (hostnameVerify) {
			log.info("SSL Hostname Verification On.");
			return;
		}

		TrustManager[] trustAllCerts = new TrustManager[]{new X509ExtendedTrustManager() {
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {

			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {

			}

			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {

			}

			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {

			}

			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}

		}};

		SSLContext sc = null;
		try {
			sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	}
}
