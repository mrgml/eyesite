package com.arantech.eyesite;

public class ReportRunner {

	/**
	 * Invokes the Report tool.
	 * Interprets args as a list of files to send.
	 * If no args are passed then runs reports and sends them.
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Just invoked main");
		TrafficMail myMail = new TrafficMail();
		if (args.length==0) {
			QueryRunner myQueryRunner = new QueryRunner();
			myQueryRunner.populateSystemInformation();
			myQueryRunner.agingPopulationQuery();
			myQueryRunner.agingParametersQuery();
			myQueryRunner.trafficQuery();
			myQueryRunner.collectionStatsQuery();
			myQueryRunner.modelFootprintQuery();
			myQueryRunner.asmQuery();
			myQueryRunner.dfQuery();
			myMail.attach(myQueryRunner.marshal());
			myMail.send();
//			myReport.create();
//			myReport.marshal();
			// Formatter?
		}
		else {
			for (String file: args) {
				myMail.attach(file);		
				myMail.send();
			}
		}
	}

}
