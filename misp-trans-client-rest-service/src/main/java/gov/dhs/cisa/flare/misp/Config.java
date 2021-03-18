package gov.dhs.cisa.flare.misp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Properties;

import javax.el.PropertyNotFoundException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.PropertiesConfigurationLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads config properties from config file
 */
public class Config {
	private static Logger log = LoggerFactory.getLogger(Config.class);
	private static final Properties DEFAULT_PROPS = new Properties();
	private static final String CONFIG_DIR = "config";
	private static final String CONFIG_FILE = "config.properties";

	public static final int DEFAULT_POLL_TIME_IN_MINUTES = 2;

	static {
		try (FileInputStream in = new FileInputStream(new File(CONFIG_DIR, CONFIG_FILE))) {
			DEFAULT_PROPS.load(in);
		} catch (FileNotFoundException e) {
			log.error("Error! {}\\{} could not be found!", CONFIG_DIR, CONFIG_FILE, e);
		} catch (IOException e) {
			log.error("Loading of configuration properties failed: IOException", e);
		}
	}

	public static String getProperty(String key) {
		if(key != null && !key.isEmpty()) {
			return DEFAULT_PROPS.getProperty(key);
		}
		 
		log.error("Config:getProperty:key is null or empty.");
		throw new PropertyNotFoundException();
	}

	/**
	 * Note: Writes to config.properties file
	 */
	static void setProperty(String key, String value) {
		FileWriter fw = null;
		try (FileInputStream fis = new FileInputStream(new File(CONFIG_DIR, CONFIG_FILE));
				InputStreamReader isr = new InputStreamReader(fis)) {

			PropertiesConfiguration config = new PropertiesConfiguration();
			PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout(config);

			// Read Properties from file
			layout.load(isr);

			// Set Property Value
			config.setProperty(key, value);

			// Write Properties out to file
			fw = new FileWriter(new File(CONFIG_DIR, CONFIG_FILE), false);
			layout.save(fw);

			// Refresh loaded Properties
			loadConfig();
		} catch (ConfigurationException e) {
			log.error("ConfigurationException occurred in setProperty()", e);
		} catch (FileNotFoundException e) {
			log.error("Error! {}\\{} could not be found!", CONFIG_DIR, CONFIG_FILE, e);
		} catch (IOException e) {
			log.error("IOException occurred in setProperty()", e);
		} finally {
			if (fw != null) {
				safeCloseFW(fw);
			}
		}
	}

	static void loadConfig() {
		try (FileInputStream in = new FileInputStream(new File(CONFIG_DIR, CONFIG_FILE))) {
			DEFAULT_PROPS.load(in);
		} catch (FileNotFoundException e) {
			log.error("Error! {}\\{} could not be found!", CONFIG_DIR, CONFIG_FILE, e);
		} catch (IOException e) {
			log.error("IOException occurred in loadConfig()", e);
		}
	}

	private static void safeCloseFW(FileWriter fw) {
		if (fw != null) {
			try {
				fw.close();
			} catch (IOException e) {
				log.error("safeCloseFW", e);
			}
		}
	}

	public static int getPollTimeInMinutes() {
		String quartzFrequencyStr = getProperty("mtc.quartz.frequency");

		if (quartzFrequencyStr != null && !quartzFrequencyStr.isEmpty()) {
			return Integer.parseInt(quartzFrequencyStr);
		}

		return DEFAULT_POLL_TIME_IN_MINUTES;
	}

	public static Instant getStartTime() {
		String startTime = getProperty("mtc.start.time");
		Instant start = Instant.EPOCH;
		if (startTime != null && !startTime.isEmpty()) {
			start = Instant.parse(startTime);
			start.atZone(ZoneId.of("UTC"));
		}
		return start;
	}

	public static Instant getEndTime() {
		Instant dateStop = Instant.now();
		dateStop.atZone(ZoneId.of("UTC"));
		return dateStop;
	}

	public static String getOutputFilePath(String stixId) {
		String destination_dir = Config.getProperty("mtc.destination.directory");
		File df = new File(destination_dir);
		if(!destination_dir.isEmpty() && df.isDirectory()) {
			return  destination_dir + stixId + ".xml";
		}
		
		log.error("Config:gtOutputFilePath:Direcotry {} not found for store staxii data.", df.getPath());
		return null;
	}
}
