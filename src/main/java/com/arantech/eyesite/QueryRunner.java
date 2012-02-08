package com.arantech.eyesite;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import com.arantech.eyesite.bindings.*;

public class QueryRunner {
	
	private static ObjectFactory objFactory;
	private Reportset myReportSet;
	private Connection connection = null;
	private Connection adminConnection = null;
	private String customerName = null;
	private long timeStamp = 0L;
	private String hostname = null;
	
	public QueryRunner() {
		// create a reportset
		objFactory = new ObjectFactory();
		myReportSet = objFactory.createReportset();
	}

	public String marshal() {
		System.out.println();
		System.out.println("**** at start of marshal() ****");
		System.out.println();
		String fileName = null;
        try {
//	            JAXBElement<Report> reportOutput = report;
            JAXBContext jc = JAXBContext.newInstance( "com.arantech.eyesite.bindings" );
            Marshaller m = jc.createMarshaller();
            m.marshal( myReportSet, System.out );
//            ZipOutputStream zipFileOutputStream = new ZipOutputStream( new FileOutputStream("SystemReport.xml.zip"));
//            GZIPOutputStream gzipFileOutputStream = new GZIPOutputStream( new FileOutputStream("SystemReport.xml.gz"), 2000);
            fileName = "SystemReport" + "_" +
						this.customerName + "_" +
						this.hostname.trim() + "_" +
						this.timeStamp + ".xml";
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            m.marshal( myReportSet, fileOutputStream );
        } catch( JAXBException jbex ){
	       	 System.out.println("Caught a JAXBException while trying to marshal the report to file.");
	       	 System.out.println("Here's the info.: " + jbex);
        }
         catch ( IOException ioex ) {
        	 System.out.println("Caught an IOException while trying to marshal the report to file.");
        	 System.out.println("Here's the info.: " + ioex);
         }
        System.out.println();
        System.out.println();
        System.out.println("**** at end of marshal() ****");
        
        // gzip the file
        fileName = gzipFile(fileName);
        return fileName;
	}
	

	public void populateSystemInformation() {
		// TODO Auto-generated method stub
		objFactory = new ObjectFactory();
		DeploymentInformation deploymentInfo = objFactory.createDeploymentInformation();
		Runtime myRuntime = Runtime.getRuntime();
		StringBuffer hostname = new StringBuffer();
		try {
			Process myProcess = myRuntime.exec("hostname");
			InputStreamReader myISReader = new InputStreamReader(myProcess.getInputStream());
			while (true) {
				int temp = myISReader.read();
				if (temp == -1) break;
				hostname.append((char)temp);
			}
		} catch (IOException ioex) {
			hostname.append("Got this IO Exception trying to retrieve the hostname: " + ioex);
		}
		this.customerName = PropertyManager.getProperty("customer.name");
		this.hostname = hostname.toString();
		this.timeStamp = System.currentTimeMillis();
		deploymentInfo.setHostname(this.hostname);
		deploymentInfo.setCustomer(this.customerName);
		deploymentInfo.setTimestamp(this.timeStamp);
		myReportSet.setDeploymentInformation(deploymentInfo);
	}
	
	public void agingPopulationQuery() {
		boolean isAdminPrivilegeRequired = false;
		String aging_population_query = "";
		
		String[] periods = {"daily", "week", "month"};  // NB these strings are used in the queries below!
		
		for (String period:periods) {
			// get a list of weekly tables
			String user_period_agg_tables_query = "SELECT DISTINCT(user_" + period + "_agg_table_name) table_name " +
			"FROM aggregate_record WHERE index_type = 'imsi'";
			List<String> periods_tables = new ArrayList<String>();
			
			// Retrieve and store the weekly table names
			try {
				ResultSet periods_tablesRS = getConnection(isAdminPrivilegeRequired).createStatement().executeQuery(user_period_agg_tables_query);
				while (periods_tablesRS.next()) {
					periods_tables.add(periods_tablesRS.getString("table_name"));
				}
				periods_tablesRS.close();
			} catch (SQLException e) {
				System.out.println("Caught an SQL exception trying to retrieve the weekly table names from the Database.");
				e.printStackTrace();
			}
			
			for (Object table : periods_tables.toArray() ) {
				// get the event count and imsi count for a week or month
				aging_population_query = MessageFormat.format(
						"SELECT " + /*agg_table_name*/
						"DISTINCT(TimeIDUtils.dayID_to_char(date_id)) as {1}, date_id, " + /*week or month*/
						"COUNT(imsi_id) as Unique_IMSIs, SUM(counter) as event_count " +
						"FROM {0} " + /*agg_table_name*/
						"GROUP BY date_id " +
						"ORDER BY date_id DESC", table.toString(), period);
				
				System.out.println("aging_population_query is: " + aging_population_query);
				
				ReportClass myReport = new ReportClass();  // new report for each query
				try {
					PreparedStatement aging_population_statement = this.connection.prepareStatement(aging_population_query);
					ResultSet aging_populationRS = aging_population_statement.executeQuery();
					
					myReport.populateReport("Aging Population " + period.toUpperCase(), aging_population_query, aging_populationRS);
				} catch (SQLException sqlEx) {
					// first populate the report so the exception is collected for diagnosis
					myReport.populateReport("Aging Population " + period.toUpperCase(), aging_population_query, sqlEx.getMessage() + ";" + sqlEx.toString() + ";" + sqlEx.getStackTrace().toString());
					
					System.out.println("Caught an SQL exception trying to retrieve the aggregation population data from the Database.");
					System.out.println("error code is: " + sqlEx.getErrorCode());
					sqlEx.printStackTrace();
				} finally {
					myReportSet.getReport().add(myReport.getReport());
				}
			}
		}
	}

	public void agingParametersQuery() {
		boolean isAdminPrivilegeRequired = false;
		String agingParametersQuery = "select ar.table_prefix as \"Interface\", " +  
				  "ar.index_type as \"Index\", " +  
				  "ap.entity_type, " +
				  "ap.entity_b_perd_user_ret_num_dy as Live_User_Days, " +
				  "ap.entity_b_perd_group_ret_num_dy as Live_Group_Days, " +
				  "ap.entity_daily_user_ret_num_dy as Daily_User_Days, " +
				  "ap.entity_daily_group_ret_num_dy as Daily_Group_Days, " +
				  "ap.entity_wkly_user_ret_num_dy as Weekly_User_Days, " +
				  "ap.entity_wkly_group_ret_num_dy as Weekly_Group_Days, " +
				  "ap.entity_mthly_user_ret_num_dy as Monthly_User_Days, " +
				  "ap.entity_mthly_group_ret_num_dy as Monthly_Group_Days, " +
				  "ap.event_ret_num_dy as Event_Days " +
				"from aging_properties ap, aggregate_record ar " +
				"where ap.agg_record_def_id = ar.aggregate_record_id " +
				"or ap.agg_record_def_id = -1";
				
		ReportClass myReport = new ReportClass();  // new report for each query
		try {
			PreparedStatement aging_parameters_statement = getConnection(isAdminPrivilegeRequired).prepareStatement(agingParametersQuery);
			ResultSet aging_parametersRS = aging_parameters_statement.executeQuery();
			
			myReport.populateReport("Aging Parameters", agingParametersQuery, aging_parametersRS);
		} catch (SQLException sqlEx) {
			// first populate the report so the exception is collected for diagnosis
			myReport.populateReport("Aging Parameters", agingParametersQuery, sqlEx.getMessage() + ";" + sqlEx.toString() + ";" + sqlEx.getStackTrace().toString());
			
			System.out.println("Caught an SQL exception trying to retrieve the aging parameters data from the Database.");
			System.out.println("error code is: " + sqlEx.getErrorCode());
			sqlEx.printStackTrace();
		} finally {
			myReportSet.getReport().add(myReport.getReport());
		}
		
	}
	
	public void trafficQuery() {
		boolean isAdminPrivilegeRequired = false;
		String agg_tables_query = "SELECT DISTINCT(group_basic_agg_table_name)  from AGGREGATE_RECORD WHERE index_type = 'imsi'";
		
		// Retrieve and store the aggregate table names
		List<String> agg_tables = new ArrayList<String>();
		try {
			ResultSet agg_tablesRS = getConnection(isAdminPrivilegeRequired).createStatement().executeQuery(agg_tables_query);
			while (agg_tablesRS.next()) {
				agg_tables.add(agg_tablesRS.getString("group_basic_agg_table_name"));
			}
			agg_tablesRS.close();
		} catch (SQLException e) {
			System.out.println("Caught an SQL exception trying to retrieve the aggregation table names from the Database.");
			e.printStackTrace();
		}
		
		// Create Traffic Query
		StringBuffer trafficQuery = new StringBuffer(	"SELECT timeidutils.timeid_to_char(" + agg_tables.get(0) + ".time_id), " +
														agg_tables.get(0) + ".time_id ");
		for (String table:agg_tables) {
			trafficQuery.append(", " + table + ".counter " + table);
		}
		/*
		 * The FROM clause here uses FULL JOIN which will retreive ALL values from both tables joined on
		 * the condition/s in the "ON" section.
		 * What we're after here is all the traffic observed in the last 30 days for every model, if 
		 * some model is deployed and NOT collecting data then we would not want that to 
		 * prevent us from retrieving information for the other models, hence the need for the FULL JOIN.
		 * example result:
		 * FULL JOIN G102G_imsi_15_group 
		 * ON (inet_a_imsi_15_group.time_id = G102G_imsi_15_group.time_id 
		 * AND inet_a_imsi_15_group.group_id = G102G_imsi_15_group.group_id) 
		 */
		trafficQuery.append(" FROM ");
		for (int i=0; i < agg_tables.size();i++) {
			if (i==0) {
				trafficQuery.append(agg_tables.get(0)); 
				continue;
			}
			trafficQuery.append(" FULL JOIN " + agg_tables.get(i) + 
								" ON (" + agg_tables.get(0) + ".time_id = " + agg_tables.get(i) + ".time_id" +
								" AND " + agg_tables.get(0) + ".group_id = " + agg_tables.get(i) + ".group_id)");
		}
		trafficQuery.append(" WHERE ");
		trafficQuery.append(agg_tables.get(0) + ".group_id = 0 "); 
		trafficQuery.append("AND " + agg_tables.get(0) + ".time_id >= (select min(time_id) from minute_time_dimension where daystamp=to_char(sysdate -30,'dd-mon-yy')) ");
		trafficQuery.append("ORDER BY " + agg_tables.get(0) + ".time_id DESC ");
		// Show the people what we're doing
		System.out.println("The traffic query is: " + trafficQuery);
		
		// Run Query and generate Report
		ReportClass myReport = new ReportClass();  // new report for each query
		try {
			PreparedStatement aging_population_statement = getConnection(isAdminPrivilegeRequired).prepareStatement(trafficQuery.toString());
			ResultSet trafficQueryRS = aging_population_statement.executeQuery();
			
			myReport.populateReport("Traffic", trafficQuery.toString(), trafficQueryRS);
		} catch (SQLException sqlEx) {
			// first populate the report so the exception is collected for diagnosis
			myReport.populateReport("Traffic", trafficQuery.toString(), sqlEx.getMessage() + ";" + sqlEx.toString() + ";" + sqlEx.getStackTrace().toString());
			
			System.out.println("Caught an SQL exception trying to retrieve the traffic data from the Database.");
			System.out.println("error code is: " + sqlEx.getErrorCode());
			sqlEx.printStackTrace();
		} finally {
			myReportSet.getReport().add(myReport.getReport());
		}
	}
	
	public void collectionStatsQuery() {
		boolean isAdminPrivilegeRequired = false;
		String collectionStatsQuery = "SELECT " +
									  "to_char(time_stamp,'yyyy-mm-dd hh24')  as \"Time\", sum(numfilescollected) as \"Files Collected\", " +
									  "sum(numobjectrecordscreated) as \"Object Recs Created\", " +
									  "round(sum(numobjectrecordscreated)/60) as \"Obj Recs Created/Min\", " +
									  "max(numfilespending) as \"Max Files Pending\", " +
									  "sum(numrecordsprocessed) as \"Recs Processed\", " +
									  "sum(numrecordsfilteredout) as \"Recs Filtered\", " +
									  "sum(numrecordsbadlyformed) as \"Badly Formed Recs\" " +
									  "FROM counter_value " + 
									  "WHERE Service_Type = 'SlaveCollectionService' " +
									  "AND time_stamp between trunc(sysdate - 30) " + 
									  "AND to_date(sysdate) GROUP BY to_char(time_stamp,'yyyy-mm-dd hh24') ORDER BY " +
									  "to_char(time_stamp,'yyyy-mm-dd hh24') DESC";
		
		System.out.println("The Collection Stat.s Query is: " + collectionStatsQuery);
		
		// Run Query and generate Report
		ReportClass myReport = new ReportClass();  // new report for each query
		try {
			PreparedStatement aging_population_statement = getConnection(isAdminPrivilegeRequired).prepareStatement(collectionStatsQuery);
			ResultSet trafficQueryRS = aging_population_statement.executeQuery();
			
			myReport.populateReport("Collection Stat.s", collectionStatsQuery, trafficQueryRS);
		} catch (SQLException sqlEx) {
			// first populate the report so the exception is collected for diagnosis
			myReport.populateReport("Collection Stat.s", collectionStatsQuery, sqlEx.getMessage() + ";" + sqlEx.toString() + ";" + sqlEx.getStackTrace().toString());
			
			System.out.println("Caught an SQL exception trying to retrieve the Collection Stat.s data from the Database.");
			System.out.println("error code is: " + sqlEx.getErrorCode());
			sqlEx.printStackTrace();
		} finally {
			myReportSet.getReport().add(myReport.getReport());
		}
	}
	
	public void modelFootprintQuery() {
		boolean isAdminPrivilegeRequired = false;
		String modelFootprintQuery = new String("SELECT " +
									  "ar.table_prefix agg_prefix, " +
									  "ar.index_type agg_index, " +
									  "sum(case when upper(segment_name) like '%15%' then bytes end)/1024/1024/1024 live, " +
									  "sum(case when upper(segment_name) like '%HOUR%' then bytes end)/1024/1024/1024 hourly, " +
									  "sum(case when upper(segment_name) like '%DAILY%' then bytes end)/1024/1024/1024 daily, " +
									  "sum(case when upper(segment_name) like '%WEEK%' then bytes end)/1024/1024/1024 weekly, " +
									  "sum(case when upper(segment_name) like '%MONTH%' then bytes end)/1024/1024/1024 monthly, " +
									  "sum(case when upper(segment_name) like '%ERR_EVNTS%' then bytes end)/1024/1024/1024 err_evts " +
									  "FROM  " +
									  "user_segments us " +
									  "FULL JOIN  " +
									  "aggregate_record ar  " +
									  "ON  " +
									  "(lower(us.segment_name) like lower(ar.table_prefix)||'%'||lower(upper(ar.index_type))||'%') " +
									  "WHERE " +
									  "us.segment_type <> 'Temporary' " +
									  "AND lower(ar.table_prefix) IN (SELECT distinct table_prefix FROM aggregate_record)" +
									  "GROUP BY ar.table_prefix, ar.index_type " +
									  "ORDER BY agg_prefix, agg_index"
									  );

		System.out.println("The Model Footprint Query is: " + modelFootprintQuery);
		
		// Run Query and generate Report
		ReportClass myReport = new ReportClass();  // new report for each query
		try {
			PreparedStatement model_footprint_statement = getConnection(isAdminPrivilegeRequired).prepareStatement(modelFootprintQuery);
			ResultSet modelFootprintRS = model_footprint_statement.executeQuery();
			
			myReport.populateReport("Model Footprint", modelFootprintQuery, modelFootprintRS);
		} catch (SQLException sqlEx) {
			// first populate the report so the exception is collected for diagnosis
			myReport.populateReport("Model Footprint", modelFootprintQuery, sqlEx.getMessage() + ";" + sqlEx.toString() + ";" + sqlEx.getStackTrace().toString());
			
			System.out.println("Caught an SQL exception trying to retrieve the Model Footprint data from the Database.");
			System.out.println("error code is: " + sqlEx.getErrorCode());
			sqlEx.printStackTrace();
		} finally {
			myReportSet.getReport().add(myReport.getReport());
		}
	}
	
	public void asmQuery() {
		boolean isAdminPrivilegeRequired = true;
		// NB this query needs admin privileges
		String asmQuery = 	"SELECT a.name DiskGroup, b.disk_number \"Disk#\", b.name DiskName, " + 
							"b.total_mb, b.free_mb, b.path, b.header_status " +
							"FROM v$asm_disk b, v$asm_diskgroup a " +
							"WHERE a.group_number (+)= b.group_number " +
							"ORDER BY b.group_number, b.disk_number, b.name";
				
		ReportClass myReport = new ReportClass();  // new report for each query
		try {
			PreparedStatement asm_statement = getConnection(isAdminPrivilegeRequired).prepareStatement(asmQuery);
			ResultSet asmRS = asm_statement.executeQuery();
			
			myReport.populateReport("ASM Information", asmQuery, asmRS);
		} catch (SQLException sqlEx) {
			// first populate the report so the exception is collected for diagnosis
			myReport.populateReport("ASM Information", asmQuery, sqlEx.getMessage() + ";" + sqlEx.toString() + ";" + sqlEx.getStackTrace().toString());
			
			System.out.println("Caught an SQL exception trying to retrieve the ASM data from the Database.");
			System.out.println("error code is: " + sqlEx.getErrorCode());
			sqlEx.printStackTrace();
		} finally {
			myReportSet.getReport().add(myReport.getReport());
		}
	}
	
	public void dfQuery() {
		boolean isAdminPrivilegeRequired = false;
		String username = PropertyManager.getProperty("db.owner.username");
		String dfQuery = 	"SELECT " + username + "_dbmon_utils.get_df_minus_lk from dual";

		ReportClass myReport = new ReportClass();  // new report for each query
		try {
			PreparedStatement df_statement = getConnection(isAdminPrivilegeRequired).prepareStatement(dfQuery);
			ResultSet dfRS = df_statement.executeQuery();
			
			myReport.populateReport("df -lk query", dfQuery, dfRS);
		} catch (SQLException sqlEx) {
			// first populate the report so the exception is collected for diagnosis
			myReport.populateReport("df -lk query", dfQuery, sqlEx.getMessage() + ";" + sqlEx.toString() + ";" + sqlEx.getStackTrace().toString());
			
			System.out.println("Caught an SQL exception trying to retrieve the ASM data from the Database.");
			System.out.println("error code is: " + sqlEx.getErrorCode());
			sqlEx.printStackTrace();
		} finally {
			myReportSet.getReport().add(myReport.getReport());
		}
	}
	
	
	public void uniqueSubscriberQuery(String period, Vector<String> table_names, Connection connection) {
		
		
		// Retrieve and store the aggregate counts and unique IMSI's
		try {
			for (Object table : table_names.toArray() ) {
				// get the event count and imsi count for a week or month
				String aging_population_query = MessageFormat.format(
						"SELECT " + /*agg_table_name*/
						"DISTINCT(TimeIDUtils.dayID_to_char(date_id)) as {1}, date_id, " + /*week or month*/
						"COUNT(imsi_id) as Unique_IMSIs, SUM(counter) as event_count " +
						"FROM {0} " + /*agg_table_name*/
						"GROUP BY date_id " +
				"ORDER BY date_id DESC", table.toString(), period);
				
				System.out.println("aging_population_query is: " + aging_population_query);
				
				PreparedStatement aging_population_statement = connection.prepareStatement(aging_population_query);
//				aging_population_statement.setString(1, table.toString());
//				aging_population_statement.setString(2, period);
//				aging_population_statement.setString(3, table.toString());
//				System.out.println("period is: " + period);
				ResultSet aging_populationRS = aging_population_statement.executeQuery();
				while (aging_populationRS.next()) {
					System.out.println(aging_populationRS.getString(period));
					System.out.println(aging_populationRS.getLong("date_id"));
					System.out.println(aging_populationRS.getLong("Unique_IMSIs"));
					System.out.println(aging_populationRS.getLong("event_count"));
				}
				aging_populationRS.close();
			}
		} catch (SQLException e) {
			System.out.println("Caught an SQL exception trying to retrieve the aggregation population data from the Database.");
			System.out.println("error code is: " + e.getErrorCode());
			e.printStackTrace();
		}
	}
	
	private String gzipFile(String fileName) {
		String gzippedFileName = fileName + ".gz";
		try {
			FileInputStream fileIn = new FileInputStream(fileName);
			GZIPOutputStream gzipOut = new GZIPOutputStream(new FileOutputStream(gzippedFileName));
			byte[] buf = new byte[1024];
			int len;
			while ((len = fileIn.read(buf)) > 0) {
				gzipOut.write(buf, 0, len);
			}
			gzipOut.finish();
			gzipOut.close();
		} catch (IOException ioex) {
			System.out.println("There's just been an IO exception while attempting to gzip the report file, here's the info: " + ioex);
		}
		
		return gzippedFileName;
	}
	
	public Connection getConnection(boolean isAdmin) {
		if (isAdmin && this.adminConnection == null) {
			this.adminConnection = getConnection(PropertyManager.getProperty("db.admin.username"), PropertyManager.getProperty("db.admin.password")); 
			return this.adminConnection; 
		} else if (isAdmin && this.adminConnection != null) {
			return this.adminConnection;
		} else if (!isAdmin && this.connection == null) {
			this.connection = getConnection(PropertyManager.getProperty("db.owner.username"), PropertyManager.getProperty("db.owner.password"));
			return this.connection; 
		} else
			return this.connection;
	}
	
	//	Connect to DB
	private Connection getConnection(String user, String pass) {
		String server, portNumber, sid, username, password = null;
		// four posssible states here, two each for isAdmin and isNull
		server = PropertyManager.getProperty("db.ipaddress");
		portNumber = PropertyManager.getProperty("db.port");
		username = user;
		password = pass;
		sid = PropertyManager.getProperty("db.sid");
		
	    Connection conn = null;
	    Properties connectionProps = new Properties();
	    
	    try {
	    	Class.forName("oracle.jdbc.driver.OracleDriver");
	    	//jdbc:oracle:thin:user/password@192.158.78.188:1521/RACEMC
			conn = DriverManager.getConnection("jdbc:oracle:thin:"
																+ username + "/"
																+ password + "@" 
																+ server + ":" 
																+ portNumber + "/" 
																+ sid);
		} catch (SQLException e) {
			System.out.println("There was an SQL exception thrown when attempting to get a database connection." +
					"Please check the db properties in install.defaults.");
			e.printStackTrace();
		} catch (ClassNotFoundException cnfEx) {
			System.out.println("Couldn't find the JDBC driver.");
			cnfEx.printStackTrace();
		}
	    System.out.println("Connected to database");
	    return conn;
	}
	
}
