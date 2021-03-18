package org.gd.ddcs.flare.misp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

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
    private static final String CONFIG_LOCATION = "config/config.properties";
	static {
		try (FileInputStream in = new FileInputStream(CONFIG_LOCATION)){
			DEFAULT_PROPS.load(in);
		}
		catch(FileNotFoundException e) {
			log.error("Error! {} could not be found!", CONFIG_LOCATION, e);
			System.exit(1);
		}
		catch(IOException e) {
			log.error("Loading of configuration properties failed: IOException",e);
		}
	}
	
	protected static String getProperty(String key) {
		return DEFAULT_PROPS.getProperty(key);
	}

	/**
	 * Note: Writes to config.properties file
	 */
	static void setProperty(String key, String value) {
		FileWriter fw = null;
		try ( FileInputStream fis = new FileInputStream(new File(CONFIG_LOCATION));
			  InputStreamReader isr = new InputStreamReader(fis)) {

	        PropertiesConfiguration config = new PropertiesConfiguration();
	        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout(config);
	        
	        //Read Properties from file
	        layout.load(isr);
	        
	        //Set Property Value
	        config.setProperty(key, value);
	        
	        //Write Properties out to file
			fw = new FileWriter(CONFIG_LOCATION,false);
	        layout.save(fw);
	        
	        //Refresh loaded Properties
	        loadConfig();
		}
		catch(ConfigurationException e) {
			log.error("ConfigurationException occurred in setProperty()",e);
		}
		catch(FileNotFoundException e) {
			log.error("Error! {} could not be found!", CONFIG_LOCATION, e);
		}
		catch(IOException e) {
			log.error("IOException occurred in setProperty()",e);
		}
		finally{
			if(fw != null) {
				safeCloseFW(fw);
			}
		}
	}
	
	static void loadConfig() {
		try (FileInputStream in = new FileInputStream(CONFIG_LOCATION)) {
			DEFAULT_PROPS.load(in);
		}
		catch(FileNotFoundException e) {
			log.error("Error! {} could not be found!", CONFIG_LOCATION, e);
		}
		catch(IOException e) {
    		log.error("IOException occurred in loadConfig()",e);
		}
	}

	private static void safeCloseFW(FileWriter fw) {
		if (fw != null) {
			try {
				fw.close();
			} catch (IOException e) {
				log.error("safeCloseFW",e);
			}
		}
	}
}