/**
 * 
 */
package com.arantech.eyesite;

import java.sql.ResultSet;
import java.util.*;
import javax.xml.bind.*;

import com.arantech.eyesite.bindings.*;

/**
 * @author garylyons
 *
 */
public class ReportClass {

	private static ObjectFactory objFactory;
	private static Report report;
	
	public ReportClass() { 
		objFactory = new ObjectFactory();
		report = objFactory.createReport();
	}
	
	public void create() {
		report.setQuery("Query String");
		report.setCaughtException("Sample Exception");
		Resultset xmlRS = objFactory.createResultset();
		report.setResultset(xmlRS);
		Row row = objFactory.createRow();
		Column column = objFactory.createColumn();
		column.setName("colname");
		column.setValue("colvalue");
		column.setType("coltype");
		xmlRS.getRow().add(row);
	}
	
	public void marshal() {
		System.out.println("**** at start of marshal() ****");
        try {
//            JAXBElement<Report> reportOutput = report;
            JAXBContext jc = JAXBContext.newInstance( "com.arantech.eyesite.bindings" );
            Marshaller m = jc.createMarshaller();
            m.marshal( report, System.out );
        } catch( JAXBException jbe ){
            // ...
        }
        System.out.println("**** at end of marshal() ****");
    }
	
}
