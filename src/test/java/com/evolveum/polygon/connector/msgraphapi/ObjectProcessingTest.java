package com.evolveum.polygon.connector.msgraphapi;

import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit test for {@link ObjectProcessing} utilities.
 */
@Test(groups = "unit")
public class ObjectProcessingTest {

	@DataProvider(name = "badAttrNames")
	public Object[][] dp_badAttrs() {
		return new String [][] {{"$coffee"}, {"paste=bin"}, {"why?"}, {"Jimmy&Tom"}};
	}

	@Test(expectedExceptions = ConfigurationException.class)
	public void testSelector_empty() {
		ObjectProcessing.selector();
	}

	@Test(expectedExceptions = ConfigurationException.class, dataProvider = "badAttrNames")
	public void testSelector_badAttr(String attrName) {
		ObjectProcessing.selector(attrName);
	}

	@Test
	public void testSelector_valid() {
		Assert.assertEquals("$select=foo,bar,baz", ObjectProcessing.selector("foo", "bar", "baz"));
	}
}
