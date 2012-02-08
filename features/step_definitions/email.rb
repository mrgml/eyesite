require 'spec/expectations'
include_class 'com.arantech.eyesite.Mail'

Given /^I have to deliver (.*)$/ do |file|
  @filename = file
end

When /^I choose to send it$/ do
  @email = Mail.new
  @email.send(@filename)
end

Then /^the file should be compressed$/ do
  pending
end




