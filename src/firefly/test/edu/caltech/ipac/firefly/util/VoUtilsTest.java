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
		Assert.assertEquals(endpoints.size(), 463);
		for (VOResourceEndpoint voResourceEndpoint : endpoints) {
			//System.out.println(voResourceEndpoint.getShortName()+":"+voResourceEndpoint.getUrl());
		}
	}
}
