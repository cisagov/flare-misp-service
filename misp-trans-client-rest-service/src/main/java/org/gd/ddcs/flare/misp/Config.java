package org.gd.ddcs.flare.misp;

import java.io.File;
import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
import java.util.Properties;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStreamReader;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.PropertiesConfigurationLayout;

/*
 * Reads config properties from config file
 */
public class Config {
	private static Properties defaultProps = new Properties();
	static {
		try {
			FileInputStream in = new FileInputStream("config/config.properties");
			defaultProps.load(in);
			in.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static String getProperty(String key) {
		return defaultProps.getProperty(key);
	}
	
	public static void setProperty(String key, String value) {
		
		try {
			File file = new File("config/config.properties");

	        PropertiesConfiguration config = new PropertiesConfiguration();
	        PropertiesConfigurationLayout layout = new PropertiesConfigurationLayout(config);
	        
	        //Read Properties from file
	        layout.load(new InputStreamReader(new FileInputStream(file)));
	        
	        //Set Property Value
	        config.setProperty(key, value);
	        
	        //Write Properties out to file
	        layout.save(new FileWriter("config/config.properties", false));
	        
	        //Refresh loaded Properties
	        loadConfig();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	protected static void loadConfig() {
		try {
			FileInputStream in = new FileInputStream("config/config.properties");
			defaultProps.load(in);
			in.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}