package com.arantech.eyesite;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class PropertyManager {
	
	static Properties systemConfiguration = new Properties();
	static String installDefaultsFile = "/opt/arantech/install.defaults";
	static String featuresFile = "/opt/arantech/conf/features/features-ext.properties";
	
	PropertyManager() {
	}
	
	public static String getProperty(String prop) {
		String returnProp = systemConfiguration.getProperty(prop);
		System.out.println("Just requested property: " + prop);
		System.out.println("It's set to: " + returnProp);
		if (returnProp == null) {
			loadInstallDefaults();
			loadFeatures();
		}
		returnProp = systemConfiguration.getProperty(prop);
		return returnProp==null ? "unavailable" : returnProp;
	}
	
	// load up the install.defaults property file so we can interrogate for
	// the mail server, etc.
	private static void loadInstallDefaults() {
		try {
			systemConfiguration.load(new FileInputStream(installDefaultsFile));
		} catch (FileNotFoundException e) {
			System.out.println("Couldn't find install.defaults, it was expected to be here: " + installDefaultsFile);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Couldn't read the install.defaults file, looks like there was some kind of IO problem?");
			e.printStackTrace();
		}
	}
	
	// load up the features-ext.properties file so we can interrogate for
	// customer.name, etc.
	private static void loadFeatures() {
		try {
			systemConfiguration.load(new FileInputStream(featuresFile));
		} catch (FileNotFoundException e) {
			System.out.println("Couldn't find features-ext.properties, it was expected to be here: " + featuresFile);
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Couldn't read the features-ext.properties file, looks like there was some kind of IO problem?");
			e.printStackTrace();
		}
	}
	
	
}
