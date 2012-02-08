/**
 * 
 */
package com.arantech.eyesite;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import javax.xml.bind.*;

import com.arantech.eyesite.bindings.*;

/**
 * @author garylyons
 *
 */
public class ReportClass {

	private static ObjectFactory objFactory;
	private Report report;
	
	public ReportClass() { 
		objFactory = new ObjectFactory();
		report = objFactory.createReport();
	}
	
	public Report getReport() {
		return this.report;
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
	
	public void populateReport( String type, String queryString, ResultSet rs) {
		populateReportType(type);
		populateQueryString(queryString);
		populateResultset(rs);
	}
	
	public void populateReport( String type, String queryString, String exception) {
		populateReportType(type);
		populateQueryString(queryString);
		populateException(exception);
	}

	private void populateReportType(String type) {
		report.setType(type);
	}
	
	private void populateQueryString( String queryString) {
		report.setQuery(queryString);
	}
	
	private void populateResultset(	ResultSet rs) {
		Resultset xmlRS = objFactory.createResultset();
		report.setResultset(xmlRS);
		
		try {
			ResultSetMetaData rsMeta = rs.getMetaData();
			int columnCount = rsMeta.getColumnCount();
			while (rs.next()) {
				Row row = objFactory.createRow();
				Column column;
				for (int i=1; i <= columnCount; i++) {
					String colName = "";
					Object colValueObject = null;
					String colValue = "";
					String colType = "";
					column = objFactory.createColumn();
					colName = rsMeta.getColumnName(i);
					colValueObject = rs.getObject(i);
					if (colValueObject != null) colValue = colValueObject.toString();
					colType = rsMeta.getColumnTypeName(i);
					
					System.out.println("colName: " + colName);
					System.out.println("colValue: " + colValue);
					System.out.println("colType: " + colType);
					
					column.setName(colName);
					column.setValue(colValue);
					column.setType(colType);
					row.getColumn().add(column);
				}
				xmlRS.getRow().add(row);
			}
			rs.close();
		} catch (SQLException sqlEx) {
			System.out.println("Caught an SQL exception trying to retrieve the aggregation population data from the Database.");
			System.out.println("error code is: " + sqlEx.getErrorCode());
			System.out.println("Stacktrace: " + sqlEx.getStackTrace().toString());
		}
	}
	
	private void populateException(	String exception) {
		report.setCaughtException(exception);
	}
	
}
