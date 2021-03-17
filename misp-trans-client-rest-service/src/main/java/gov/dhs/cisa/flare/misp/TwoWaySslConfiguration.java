package gov.dhs.cisa.flare.misp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Code resource : https://mrkandreev.name/blog/java-two-way-ssl/
 */

@Configuration
@PropertySource(value="file:config/application.properties")
public class TwoWaySslConfiguration {
	private static final Logger log = LoggerFactory.getLogger(Taxii11RequestSubmit.class);

	@Value("${client.ssl.key-store}")
	private Resource keyStoreData;

	@Value("${server.ssl.key-password}")
	private String keyStorePassword;

	@Value("${server.ssl.key-password}")
	private String keyPassword;

	@Value("${2way.ssl.auth}")
	private boolean certAuth;

	@Bean
	public RestTemplate restTemplate() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
		if(!certAuth) {
			log.info("Do not send client certificate.");
			return new RestTemplate();
		}

		log.info("Sending client certificate.......");
		KeyStore keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(new BufferedInputStream(keyStoreData.getInputStream()), keyStorePassword.toCharArray());

		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder()
						.loadTrustMaterial(null, new TrustSelfSignedStrategy())
						.loadKeyMaterial(keyStore, keyPassword.toCharArray()).build()
				, NoopHostnameVerifier.INSTANCE);
		CloseableHttpClient httpClient = HttpClients.custom()
				.setSSLSocketFactory(socketFactory)
				.setMaxConnTotal(1)
				.setMaxConnPerRoute(5)
				.build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(10000);
		requestFactory.setConnectionRequestTimeout(10000);

		return new RestTemplate(requestFactory);
	}
}
