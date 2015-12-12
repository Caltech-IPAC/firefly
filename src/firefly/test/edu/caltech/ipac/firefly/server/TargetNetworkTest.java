package edu.caltech.ipac.firefly.server;

import java.io.IOException;

import edu.caltech.ipac.astro.net.Simbad4Client;
import edu.caltech.ipac.astro.net.SimbadException;
import edu.caltech.ipac.astro.net.SimbadObject;
import edu.caltech.ipac.util.download.FailedRequestException;

public class TargetNetworkTest {

	public static void main(String[] args) throws FailedRequestException, SimbadException, IOException {
		Simbad4Client a = new Simbad4Client();
//		NedNameResolver r = new NedNameResolver();
		
		SimbadObject searchByName = a.searchByName("m81");
		System.out.println(searchByName.getName());
		
		
		
		
	}
}