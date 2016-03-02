package edu.caltech.ipac.firefly.util;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.caltech.ipac.util.VoRegistryUtil;
import edu.caltech.ipac.util.dd.VOResourceEndpoint;

public class VoUtilsTest {

	private List<VOResourceEndpoint> endpoints;

	@Test
	public void testRegistryEndPoint(){
		endpoints = VoRegistryUtil.getEndpoints("ConeSearch", "spitzer");
		Assert.assertEquals(endpoints.size(), 146);
		endpoints = VoRegistryUtil.getEndpoints("ConeSearch", "iphas");
		Assert.assertEquals(endpoints.size(), 16);
	}
}
