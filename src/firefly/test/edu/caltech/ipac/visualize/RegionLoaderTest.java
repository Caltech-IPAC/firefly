package edu.caltech.ipac.visualize;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.RegionConnection;
import edu.caltech.ipac.util.RegionFactory;
import edu.caltech.ipac.util.RegionParser;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.visualize.plot.WorldPt;

public class RegionLoaderTest {

	@Test
	public void loadDs9String() throws IOException {

		String footprintDefinitionImageSpace = "" + "# Region file format: DS9 version 4.1\n" + "# comment\n"
		// + "global color=green dashlist=8 3 width=1 font=\"helvetica 10
		// normal\" select=1 highlite=1 dash=0 fixed=0 edit=1 move=1 delete=1
		// include=1 source=1"
				+ "global color=red\n" + "image\n" // coord sys
		// + "point(0,0) # point=circle 20 color=blue\n"
				+ "box(50,0,100,80,0)";

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

		RegionConnection reg = new RegionConnection(result.getRegionList());
		List<DrawObj> drawData = new ArrayList<>();
		// List<WorldPt> wptLst = new ArrayList<>();
		MockPlot plot = new MockPlot();
		for (Region r : result.getRegionList()) {
			DrawObj drawObj = reg.makeRegionDrawObject(r, plot, false);
			// wptLst.add(r.getPt());
			if (drawObj != null)
				drawData.add(drawObj);
		}

		for (DrawObj r : drawData) {
			// Assert.assertTrue(r instanceof PointDataObj);
			System.out.println(r.toString());
		}
	}

	@Test
	public void testPositionAngle() {

		System.out.println(VisUtil.getPositionAngle(new WorldPt(40.63917590447316, 42.56781951371009),
				new WorldPt(40.50254674815805, 42.66801248522869)));

		System.out.println(VisUtil.getPositionAngle(new WorldPt(40.607766408497454, 42.57682008796501),
				new WorldPt(40.46795100609633, 42.58202492532826)));
		;
		;
		
	}

	class MockPlot extends WebPlot {

		private double cdelt1 = -2.801912890205E-4; // example from FC 'm34'
													// search DSS2 red

		public MockPlot(WebPlotInitializer wpInit, boolean asOverlay) {
			super(wpInit, asOverlay);
			// TODO Auto-generated constructor stub
		}

		public MockPlot() {

		}

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
