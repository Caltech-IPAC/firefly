package edu.caltech.ipac.visualize;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.PointDataObj;
import edu.caltech.ipac.firefly.visualize.draw.RegionConnection;
import edu.caltech.ipac.util.RegionFactory;
import edu.caltech.ipac.util.RegionParser;
import edu.caltech.ipac.util.dd.Region;

public class RegionLoaderTest {

	@Test
	public void loadDs9String() throws IOException {

		String footprintDefinitionImageSpace = "" + "# Region file format: DS9 version 4.1\n" 
				+ "# comment\n"
				//+ "global color=green dashlist=8 3 width=1 font=\"helvetica 10 normal\" select=1 highlite=1 dash=0 fixed=0 edit=1 move=1 delete=1 include=1 source=1"
				+ "global color=red\n"
				+ "image\n" // coord sys
				+ "point(43.379079,54.053459) # point=circle 4 color=blue\n"
				+ "polygon(37.573011,59.673106,43.488162,46.000362,49.149382,48.448864,43.236227,62.122449)";
		
		
		InputStream is = new ByteArrayInputStream(footprintDefinitionImageSpace.getBytes());
		// read it with BufferedReader
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		RegionParser parser = new RegionParser();
		RegionFactory.ParseRet result = parser.processFile(br);
		
		
		for (Region r : result.getRegionList())
			System.out.println(r.toString());
		for (String s : result.getMsgList())
			System.out.println(s);

		System.out.println("Output:");
		System.out.println("------");
		for (Region r : result.getRegionList())
			System.out.println(r.serialize());

		RegionConnection reg = new RegionConnection("test footprint", result.getRegionList());
		List<DrawObj> drawData = new ArrayList<>();
		MockPlot plot = new MockPlot(null, true);
		for (Region r : result.getRegionList()) {
			DrawObj drawObj = reg.makeRegionDrawObject(r, plot, false);

			if (drawObj != null)
				drawData.add(drawObj);
		}

		for (DrawObj r : drawData) {
//			Assert.assertTrue(r instanceof PointDataObj);
			System.out.println(r.toString());
		}
	}

	class MockPlot extends WebPlot {

		private double cdelt1 = -2.801912890205E-4; // example from FC 'm34'
													// search DSS2 red

		public MockPlot(WebPlotInitializer wpInit, boolean asOverlay) {
			super(wpInit, asOverlay);
			// TODO Auto-generated constructor stub
		}

//		public MockPlot() {
//
//		}

		@Override
		public float getZoomFact() {
			return 1;
		}

		public double getImagePixelScaleInDeg() {
			return Math.abs(cdelt1);// * 3600.0/3600.0; see projection used in
									// WebPlot
		}

	}
}
