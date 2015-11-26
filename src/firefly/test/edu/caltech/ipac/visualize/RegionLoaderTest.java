package edu.caltech.ipac.visualize;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import edu.caltech.ipac.firefly.util.MathUtil;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.FootprintObj;
import edu.caltech.ipac.firefly.visualize.draw.RegionConnection;
import edu.caltech.ipac.util.RegionFactory;
import edu.caltech.ipac.util.RegionParser;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.dd.RegionLines;
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
		footprintDefinitionImageSpace = ""

		+ "J2000;polygon 40.61604915124965 42.80918792708867 40.712377144607814 42.93672942109403 40.88357410412381 42.865366469565274 40.78798704601202 42.73923576788347 40.61604915124965 42.80918792708867";

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
		MockPlott plot = new MockPlott();
		//WorldPt worldCoords = plot.getWorldCoords(new ScreenPt(0, 0));
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
	private String getFootprintString() {
		String jwstPolys = 
				  " POLYGON    0.03794999  -0.17407720   0.03843333  -0.21384895   0.07796106  -0.21439047   0.07665551  -0.17417432   0.03794999  -0.17407720"
							+ " POLYGON   -0.01246111  -0.17438280  -0.01299444  -0.21437121   0.02619722  -0.21371843   0.02595278  -0.17398279  -0.01246111  -0.17438280"
							+ " POLYGON   -0.13494975  -0.08756358  -0.13742196  -0.11857171  -0.10607210  -0.12121628  -0.10358878  -0.08992200  -0.13494975  -0.08756358"
							+ " POLYGON   -0.13951917  -0.08917466  -0.14044138  -0.08903022  -0.14029416  -0.08801078  -0.13937751  -0.08814134  -0.13951917  -0.08917466 "
							+ " POLYGON   -0.13951917  -0.08921356  -0.14045528  -0.08907188  -0.14030250  -0.08805522  -0.13937473  -0.08818856  -0.13951917  -0.08921356 "
							+ " POLYGON   -0.13951639  -0.08913022  -0.14041917  -0.08899411  -0.14026638  -0.08797745  -0.13936639  -0.08810245  -0.13951639  -0.08913022 "
							+ " POLYGON   -0.13943306  -0.08944411  -0.14059694  -0.08928577  -0.14038027  -0.08797744  -0.13923306  -0.08814412  -0.13943306  -0.08944411 "
							+ " POLYGON   -0.13947472  -0.08951077  -0.14063860  -0.08935521  -0.14041917  -0.08804411  -0.13927195  -0.08821356  -0.13947472  -0.08951077 "
							+ " POLYGON   -0.13941917  -0.08949410  -0.14056639  -0.08934133  -0.14035249  -0.08803022  -0.13920806  -0.08819967  -0.13941917  -0.08949410 "
							+ " POLYGON   -0.13937751  -0.08964411  -0.14094694  -0.08941355  -0.14077194  -0.08770800  -0.13922195  -0.08790245  -0.13937751  -0.08964411 "
							+ " POLYGON   -0.13938306  -0.08969967  -0.14096360  -0.08946632  -0.14076916  -0.08776356  -0.13922195  -0.08795800  -0.13938306  -0.08969967 "
							+ " POLYGON   -0.13938028  -0.08969967  -0.14092749  -0.08946632  -0.14073861  -0.08776356  -0.13921362  -0.08795800  -0.13938028  -0.08969967 "
							+ " POLYGON   -0.13901084  -0.09011910  -0.14103860  -0.08987188  -0.14059694  -0.08768578  -0.13863306  -0.08800800  -0.13901084  -0.09011910 "
							+ " POLYGON   -0.13901917  -0.09009966  -0.14104138  -0.08985798  -0.14057749  -0.08767189  -0.13861639  -0.08798856  -0.13901917  -0.09009966 "
							+ " POLYGON   -0.13902472  -0.09008299  -0.14099972  -0.08982187  -0.14058027  -0.08765244  -0.13862751  -0.08797467  -0.13902472  -0.09008299 "
							+ " POLYGON   -0.09936379  -0.17507142  -0.09899435  -0.21256815  -0.06178609  -0.21235445  -0.06216109  -0.17488824  -0.09936379  -0.17507142 "
							+ " POLYGON    0.02464166  -0.13766916   0.02470000  -0.15551627   0.04253888  -0.15534680   0.04223888  -0.13754692   0.02464166  -0.13766916 "
							+ " POLYGON    0.02463889  -0.11893871   0.02460833  -0.13651917   0.04220277  -0.13641637   0.04204166  -0.11888313   0.02463889  -0.11893871 "
							+ " POLYGON    0.00560278  -0.13766640   0.00542222  -0.15561073   0.02334444  -0.15555794   0.02327778  -0.13771084   0.00560278  -0.13766640 "
							+ " POLYGON    0.00578333  -0.11889427   0.00566944  -0.13654697   0.02332778  -0.13650806   0.02324722  -0.11894982   0.00578333  -0.11889427 "
							+ " POLYGON    0.00623056  -0.11906927   0.00585833  -0.15510796   0.04201666  -0.15488846   0.04153333  -0.11912202   0.00623056  -0.11906927 "
							+ " POLYGON   -0.04223610  -0.11834147  -0.04244444  -0.13584693  -0.02489167  -0.13598862  -0.02487222  -0.11843593  -0.04223610  -0.11834147 "
							+ " POLYGON   -0.04231944  -0.13697193  -0.04269722  -0.15475235  -0.02488055  -0.15501905  -0.02474444  -0.13718862  -0.04231944  -0.13697193 "
							+ " POLYGON   -0.02353611  -0.11844982  -0.02348333  -0.13599140  -0.00585278  -0.13591641  -0.00609722  -0.11828594  -0.02353611  -0.11844982 "
							+ " POLYGON   -0.02353889  -0.13714972  -0.02355556  -0.15499127  -0.00565278  -0.15501351  -0.00588333  -0.13708029  -0.02353889  -0.13714972 "
							+ " POLYGON   -0.04251110  -0.11859980  -0.04293055  -0.15443569  -0.00671944  -0.15462739  -0.00715000  -0.11853316  -0.04251110  -0.11859980 "
							+ " POLYGON    0.12323870  -0.14307715   0.08351106  -0.09825257   0.04008055  -0.13705249   0.07901661  -0.18294088   0.12323870  -0.14307715 "
							+ " POLYGON    0.17122726  -0.10106889   0.13027200  -0.05699150   0.08659438  -0.09551925   0.12639146  -0.14028271   0.17122726  -0.10106889 "
							+ " POLYGON    0.14853299  -0.12195218   0.10709987  -0.07638871   0.06203886  -0.11632755   0.10235822  -0.16286597   0.14853299  -0.12195218";
		return jwstPolys;
	}
	@Test
	public void parseJwstPolygonString(){
		
		
		String[] split = getFootprintString().split("\\s");
		int polys=0;
		HashMap<String, List<Double>> map = new HashMap<>();
		ArrayList<Double> lst = null;
		for (int i = 0; i < split.length; i++) {
			String trim = split[i].trim();
			if(trim.length()>0){
				if(trim.startsWith("POL")){
					lst = new ArrayList<Double>();
					map.put("POL"+polys, lst);
					polys++;
				}else{
					//System.out.println(polys+": "+trim);
					lst.add(Double.parseDouble(trim));
				}
				
			}			
		}
		WorldPt newCenter = new WorldPt(40.5, 42.81);
		System.out.println("Center "+ newCenter.toString());
		List<Region> footprintRegions = new ArrayList<>();
		Set<String> keySet = map.keySet();
		for (String string : keySet) {
			List<Double> list = map.get(string);
			Double[] array = list.toArray(new Double[list.size()]);
			WorldPt[] pts = new WorldPt[array.length / 2];
			for (int i = 0; i < array.length / 2; i++) {
				WorldPt pt = new WorldPt(array[2 * i].doubleValue(), array[2 * i + 1].doubleValue());
//				newCenter = pt;
				System.out.println(string +" "+ pt.toString());
				double computeDistance = VisUtil.computeDistance(pt, newCenter); //deg
				double distDeg =  MathUtil.convert(MathUtil.Units.DEGREE, MathUtil.Units.DEGREE, computeDistance);
				double phi = VisUtil.getPositionAngle(pt,newCenter);
				
				WorldPt newPt =  VisUtil.getNewPosition(pt.getLon(), pt.getLat(), distDeg, phi);
				//VisUtil.calculatePosition(pt, newCenter.getLon(), newCenter.getLat());
//				pts[i] = new WorldPt(array[2 * i].doubleValue()+newPt.getLon(), array[2 * i + 1].doubleValue()+newPt.getLat());
				pts[i] = newPt;
				System.out.println(string +" "+ pts[i].toString());
			}
			for (int i = 0; i < array.length / 2-1; i++) {
					System.out.println(i +" "+ VisUtil.computeDistance(pts[i], pts[i+1]));
			}
			
			footprintRegions.add(new RegionLines(pts));
		}
		
		MockPlott plot = new MockPlott();
		// Not a footprint obj
		RegionLines xcross = new RegionLines(new WorldPt(-10, 0), /* Add this extra point to make a polygon plot.getWorldCoords(new ScreenPt(0, 0)),*/
				new WorldPt(10, 0)); // rl.isPolygon() is called when drawing...
		footprintRegions.add(xcross);
		RegionConnection regConnection = new RegionConnection(footprintRegions);
		int i = 0;
		for (Region r : footprintRegions) {
			
			DrawObj drawObj = regConnection.makeRegionDrawObject(r, plot, false);
			System.out.println(r.toString());
			System.out.println(drawObj.toString());
			if(drawObj instanceof FootprintObj){
				i++;
			}
		}
		org.junit.Assert.assertTrue(i == footprintRegions.size()-1);
		
	}

	/**
	 * Used for building regions - only matter height and zoom level
	 * @author ejoliet
	 *
	 */
	class MockPlott extends WebPlot {

		private double cdelt1 = -2.801912890205E-4; // example from FC 'm34'
													// search DSS2 red

		public MockPlott(WebPlotInitializer wpInit, boolean asOverlay) {
			super(wpInit, asOverlay);
			// TODO Auto-generated constructor stub
		}

		public MockPlott() {

		}

		@Override
		public int getImageHeight() {
			return 1;
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
