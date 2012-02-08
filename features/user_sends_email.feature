Feature: user sends email
	As a system monitor
	I want to send statistics files
	To arantech
	So that they can be analysed
	
	Scenario: deliver file
		Given I have to deliver results.xml
		When I choose to send it
		Then the file should be compressed
		And the file should be encrypted
		And the file should be attached
		And the email should be sent
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		# Given the action is <action>
		# when the subject is <subject>
		# then the greeting is <greeting>