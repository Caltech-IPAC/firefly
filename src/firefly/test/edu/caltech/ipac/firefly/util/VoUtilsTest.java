package edu.caltech.ipac.firefly.util;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.VoRegistryUtil;
import edu.caltech.ipac.util.dd.VOResourceEndpoint;

public class VoUtilsTest {

	private List<VOResourceEndpoint> endpoints;

	@Test
	public void testRegistryEndPoint(){
		endpoints = VoRegistryUtil.getEndpoints("ConeSearch", "iphas");
		Assert.assertEquals(endpoints.size(), 15);
	}
	@Test
	public void testAnotherRegistryEndPoint(){
		endpoints = VoRegistryUtil.getEndpoints("ConeSearch", "spitzer");
		Assert.assertEquals(endpoints.size(), 783);
	}
	public static void main(String[] args) {
		
		List<VOResourceEndpoint> pts = VoRegistryUtil.getEndpoints("ConeSearch", "iphas");
		for (VOResourceEndpoint ep : pts) {
            final String shortName = ep.getShortName();
            final String url = ep.getUrl();
            System.out.println(url + ": "+ep.getTitle()+
                                (StringUtils.isEmpty(shortName) ? "" : " ["+shortName+"]"));
		}
	}
}
