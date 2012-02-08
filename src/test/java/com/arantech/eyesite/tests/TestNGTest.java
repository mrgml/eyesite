package com.arantech.eyesite.tests;

import org.testng.annotations.*;

public class TestNGTest
{
	@Test
	public void thisTestWillPass() {
		assert true: "This test should pass.";
	}
	
	@Test
	public void thisTestWillFail() {
		assert false: "I meant this test to fail.";
	}
	
	@Configuration(beforeTestClass=true)
	public void doBeforeTests() {
		System.out.println("about to doBeforeTests()");
	}
	
}