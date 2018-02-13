package org.gd.ddcs.flare.misp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import org.apache.commons.configuration.ConfigurationException;

//import javax.naming.ConfigurationException;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.PropertiesConfigurationLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/*
 * Reads config properties from config file
 */
public class Config {
    private static Logger log = LoggerFactory.getLogger(Config.class);
	private static Properties defaultProps = new Properties();
	static {
		try (FileInputStream in = new FileInputStream("config/config.properties")){
			defaultProps.load(in);
		}
		catch(FileNotFoundException e) {
			log.error("Loading of configuration properties failed: FileNotFoundException",e);
		}
		catch(IOException e) {
			log.error("Loading of configuration properties failed: IOException",e);
		}
	}
	
	protected static String getProperty(String key) {
		return defaultProps.getProperty(key);
	}
	
	protected static void setProperty(String key, String value) {
		FileWriter fw = null; 
		File file = new File("config/config.properties");
		
		try ( FileInputStream fis = new FileInputStream(file);
			  InputStreamReader isr = new InputStreamReader(fis);){

	        PropertiesConfiguration config = new PropertiesConfiguration();
	        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout(config);
	        
	        //Read Properties from file
	        layout.load(isr);
	        
	        //Set Property Value
	        config.setProperty(key, value);
	        
	        //Write Properties out to file
			fw = new FileWriter("config/config.properties",false);
	        layout.save(fw);
	        
	        //Refresh loaded Properties
	        loadConfig();
		}
		catch(ConfigurationException e) {
			log.error("ConfigurationException occurred in setProperty()",e);
		}
		catch(FileNotFoundException e) {
			log.error("FileNotFoundException occurred in setProperty()",e);
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
	
	protected static void loadConfig() {
		try (FileInputStream in = new FileInputStream("config/config.properties");){
			defaultProps.load(in);
		}
		catch(FileNotFoundException e) {
    		log.error("Exception occurred in loadConfig()",e);
		}
		catch(Exception e) {
    		log.error("Exception occurred in loadConfig()",e);
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