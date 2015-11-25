package edu.caltech.ipac.visualize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.caltech.ipac.firefly.data.form.PositionFieldDef;
import edu.caltech.ipac.firefly.data.form.PositionFieldDef.ClientPositionResolverHelper;
import edu.caltech.ipac.firefly.server.visualize.CtxControl;
import edu.caltech.ipac.firefly.server.visualize.PlotClientCtx;
import edu.caltech.ipac.firefly.server.visualize.WebPlotFactory;
import edu.caltech.ipac.firefly.util.PositionParser;
import edu.caltech.ipac.firefly.visualize.FootprintFactory;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.FootprintFactory.FOOTPRINT;
import edu.caltech.ipac.firefly.visualize.FootprintFactory.INSTRUMENTS;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.VisUtil.CentralPointRetval;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.FootprintObj;
import edu.caltech.ipac.firefly.visualize.draw.RegionConnection;
import edu.caltech.ipac.firefly.visualize.draw.ShapeDataObj;
import edu.caltech.ipac.firefly.visualize.draw.ShapeDataObj.ShapeType;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.dd.RegionLines;
import edu.caltech.ipac.util.dd.ValidationException;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.GeomException;
import edu.caltech.ipac.visualize.plot.WorldPt;

public class FootprintFactoryTest {
	private static FootprintFactory footprintFactory;

	@Before
	public void setMeUp() throws FailedRequestException, GeomException {
		footprintFactory = new FootprintFactory();
//		WebPlotRequest dssRequest = WebPlotRequest.makeSloanDSSRequest(new WorldPt(0,0),"r",200/3600);
//		
//		WebPlotInitializer wpInit[]=  WebPlotFactory.createNew(null, dssRequest);
//		footprintFactory.setWebPlot(new MockPlot(wpInit[0],false));
	}

	private String getFootprintString() {
		String hst = "POLYGON    0.04443055   0.07030828   0.10070267   0.06821097   0.10157212   0.04029993   0.04611388   0.04307220   0.04443055   0.07030828 POLYGON    0.04305555   0.09917487   0.09989434   0.09782198   0.10071656   0.06894430   0.04444999   0.07100272   0.04305555   0.09917487 POLYGON    0.06131664   0.12724694   0.05330554   0.12808307   0.05328332   0.13514413   0.06136109   0.13432190   0.06131664   0.12724694 POLYGON    0.06194720   0.12593028   0.05245554   0.12682196   0.05239443   0.13538303   0.06199720   0.13445245   0.06194720   0.12593028 CIRCLE    0.06462497  -0.06595826   0.00069444 CIRCLE    0.06462497  -0.06595826   0.00069444 POLYGON    0.12901798   0.11616805   0.13973823   0.10302369   0.14912465   0.08889596   0.15708764   0.07391970   0.16355120   0.05823786   0.16845363   0.04200014   0.17174815   0.02536152   0.17340331   0.00848082   0.17340331  -0.00848082   0.17174815  -0.02536152   0.16845363  -0.04200014   0.16355120  -0.05823786   0.15708764  -0.07391970   0.14912465  -0.08889596   0.13973823  -0.10302369   0.12901798  -0.11616805   0.17030352  -0.15334139   0.18445419  -0.13599087   0.19684420  -0.11734230   0.20735529  -0.09757369   0.21588714  -0.07687373   0.22235832  -0.05544000   0.22670706  -0.03347709   0.22889186  -0.01119465   0.22889186   0.01119465   0.22670706   0.03347709   0.22235832   0.05544000   0.21588714   0.07687373   0.20735529   0.09757369   0.19684420   0.11734230   0.18445419   0.13599087   0.17030352   0.15334139   0.12901798   0.11616805 POLYGON    0.11616835  -0.12901772   0.10302400  -0.13973800   0.08889626  -0.14912447   0.07391997  -0.15708751   0.05823810  -0.16355111   0.04200032  -0.16845359   0.02536164  -0.17174814   0.00848086  -0.17340331  -0.00848086  -0.17340331  -0.02536164  -0.17174814  -0.04200032  -0.16845359  -0.05823810  -0.16355111  -0.07391997  -0.15708751  -0.08889626  -0.14912447  -0.10302400  -0.13973800  -0.11616835  -0.12901772  -0.15334206  -0.17030291  -0.13599157  -0.18445367  -0.11734299  -0.19684379  -0.09757433  -0.20735499  -0.07687427  -0.21588695  -0.05544042  -0.22235822  -0.03347736  -0.22670702  -0.01119474  -0.22889185   0.01119474  -0.22889185   0.03347736  -0.22670702   0.05544042  -0.22235822   0.07687427  -0.21588695   0.09757433  -0.20735499   0.11734299  -0.19684379   0.13599157  -0.18445367   0.15334206  -0.17030291   0.11616835  -0.12901772 POLYGON   -0.12901798  -0.11616805  -0.13973823  -0.10302369  -0.14912465  -0.08889596  -0.15708764  -0.07391970  -0.16355120  -0.05823786  -0.16845363  -0.04200014  -0.17174815  -0.02536152  -0.17340331  -0.00848082  -0.17340331   0.00848082  -0.17174815   0.02536152  -0.16845363   0.04200014  -0.16355120   0.05823786  -0.15708764   0.07391970  -0.14912465   0.08889596  -0.13973823   0.10302369  -0.12901798   0.11616805  -0.17030352   0.15334139  -0.18445419   0.13599087  -0.19684420   0.11734230  -0.20735529   0.09757369  -0.21588714   0.07687373  -0.22235832   0.05544000  -0.22670706   0.03347709  -0.22889186   0.01119465  -0.22889186  -0.01119465  -0.22670706  -0.03347709  -0.22235832  -0.05544000  -0.21588714  -0.07687373  -0.20735529  -0.09757369  -0.19684420  -0.11734230  -0.18445419  -0.13599087  -0.17030352  -0.15334139  -0.12901798  -0.11616805 POLYGON   -0.07989995   0.08093320  -0.08204995   0.08310819  -0.08422772   0.08095541  -0.08207772   0.07878042  -0.07989995   0.08093320 POLYGON   -0.08398605   0.08639984  -0.08778882   0.09014426  -0.09156381   0.08631094  -0.08776104   0.08256651  -0.08398605   0.08639984 POLYGON   -0.05819165   0.06560549  -0.06835552   0.07573879  -0.07851384   0.06554713  -0.06834997   0.05541661  -0.05819165   0.06560549 CIRCLE   -0.05935553  -0.06245550   0.01979722 CIRCLE   -0.05935553  -0.06245550   0.01979722 CIRCLE   -0.05935553  -0.06245550   0.01979722 POLYGON    0.01545556  -0.01782778  -0.01446667   0.01608611   0.00152500   0.03198333   0.03108889  -0.00191111   0.01545556  -0.01782778 POLYGON   -0.00058333  -0.03417222  -0.03089722  -0.00026389  -0.01472778   0.01583889   0.01521944  -0.01804722  -0.00058333  -0.03417222 POLYGON    0.00193333  -0.02526111  -0.02477500   0.00130833  -0.00116944   0.02586667   0.02621944  -0.00136389   0.00193333  -0.02526111 POLYGON    0.00193333  -0.02526111  -0.02477500   0.00130833  -0.00116944   0.02586667   0.02621944  -0.00136389   0.00193333  -0.02526111";
		/*
		 * String jwst =
		 * " POLYGON    0.03794999  -0.17407720   0.03843333  -0.21384895   0.07796106  -0.21439047   0.07665551  -0.17417432   0.03794999  -0.17407720"
		 * +
		 * " POLYGON   -0.01246111  -0.17438280  -0.01299444  -0.21437121   0.02619722  -0.21371843   0.02595278  -0.17398279  -0.01246111  -0.17438280"
		 * +
		 * " POLYGON   -0.13494975  -0.08756358  -0.13742196  -0.11857171  -0.10607210  -0.12121628  -0.10358878  -0.08992200  -0.13494975  -0.08756358"
		 * +
		 * " POLYGON   -0.13951917  -0.08917466  -0.14044138  -0.08903022  -0.14029416  -0.08801078  -0.13937751  -0.08814134  -0.13951917  -0.08917466 "
		 * +
		 * " POLYGON   -0.13951917  -0.08921356  -0.14045528  -0.08907188  -0.14030250  -0.08805522  -0.13937473  -0.08818856  -0.13951917  -0.08921356 "
		 * +
		 * " POLYGON   -0.13951639  -0.08913022  -0.14041917  -0.08899411  -0.14026638  -0.08797745  -0.13936639  -0.08810245  -0.13951639  -0.08913022 "
		 * +
		 * " POLYGON   -0.13943306  -0.08944411  -0.14059694  -0.08928577  -0.14038027  -0.08797744  -0.13923306  -0.08814412  -0.13943306  -0.08944411 "
		 * +
		 * " POLYGON   -0.13947472  -0.08951077  -0.14063860  -0.08935521  -0.14041917  -0.08804411  -0.13927195  -0.08821356  -0.13947472  -0.08951077 "
		 * +
		 * " POLYGON   -0.13941917  -0.08949410  -0.14056639  -0.08934133  -0.14035249  -0.08803022  -0.13920806  -0.08819967  -0.13941917  -0.08949410 "
		 * +
		 * " POLYGON   -0.13937751  -0.08964411  -0.14094694  -0.08941355  -0.14077194  -0.08770800  -0.13922195  -0.08790245  -0.13937751  -0.08964411 "
		 * +
		 * " POLYGON   -0.13938306  -0.08969967  -0.14096360  -0.08946632  -0.14076916  -0.08776356  -0.13922195  -0.08795800  -0.13938306  -0.08969967 "
		 * +
		 * " POLYGON   -0.13938028  -0.08969967  -0.14092749  -0.08946632  -0.14073861  -0.08776356  -0.13921362  -0.08795800  -0.13938028  -0.08969967 "
		 * +
		 * " POLYGON   -0.13901084  -0.09011910  -0.14103860  -0.08987188  -0.14059694  -0.08768578  -0.13863306  -0.08800800  -0.13901084  -0.09011910 "
		 * +
		 * " POLYGON   -0.13901917  -0.09009966  -0.14104138  -0.08985798  -0.14057749  -0.08767189  -0.13861639  -0.08798856  -0.13901917  -0.09009966 "
		 * +
		 * " POLYGON   -0.13902472  -0.09008299  -0.14099972  -0.08982187  -0.14058027  -0.08765244  -0.13862751  -0.08797467  -0.13902472  -0.09008299 "
		 * +
		 * " POLYGON   -0.09936379  -0.17507142  -0.09899435  -0.21256815  -0.06178609  -0.21235445  -0.06216109  -0.17488824  -0.09936379  -0.17507142 "
		 * +
		 * " POLYGON    0.02464166  -0.13766916   0.02470000  -0.15551627   0.04253888  -0.15534680   0.04223888  -0.13754692   0.02464166  -0.13766916 "
		 * +
		 * " POLYGON    0.02463889  -0.11893871   0.02460833  -0.13651917   0.04220277  -0.13641637   0.04204166  -0.11888313   0.02463889  -0.11893871 "
		 * +
		 * " POLYGON    0.00560278  -0.13766640   0.00542222  -0.15561073   0.02334444  -0.15555794   0.02327778  -0.13771084   0.00560278  -0.13766640 "
		 * +
		 * " POLYGON    0.00578333  -0.11889427   0.00566944  -0.13654697   0.02332778  -0.13650806   0.02324722  -0.11894982   0.00578333  -0.11889427 "
		 * +
		 * " POLYGON    0.00623056  -0.11906927   0.00585833  -0.15510796   0.04201666  -0.15488846   0.04153333  -0.11912202   0.00623056  -0.11906927 "
		 * +
		 * " POLYGON   -0.04223610  -0.11834147  -0.04244444  -0.13584693  -0.02489167  -0.13598862  -0.02487222  -0.11843593  -0.04223610  -0.11834147 "
		 * +
		 * " POLYGON   -0.04231944  -0.13697193  -0.04269722  -0.15475235  -0.02488055  -0.15501905  -0.02474444  -0.13718862  -0.04231944  -0.13697193 "
		 * +
		 * " POLYGON   -0.02353611  -0.11844982  -0.02348333  -0.13599140  -0.00585278  -0.13591641  -0.00609722  -0.11828594  -0.02353611  -0.11844982 "
		 * +
		 * " POLYGON   -0.02353889  -0.13714972  -0.02355556  -0.15499127  -0.00565278  -0.15501351  -0.00588333  -0.13708029  -0.02353889  -0.13714972 "
		 * +
		 * " POLYGON   -0.04251110  -0.11859980  -0.04293055  -0.15443569  -0.00671944  -0.15462739  -0.00715000  -0.11853316  -0.04251110  -0.11859980 "
		 * +
		 * " POLYGON    0.12323870  -0.14307715   0.08351106  -0.09825257   0.04008055  -0.13705249   0.07901661  -0.18294088   0.12323870  -0.14307715 "
		 * +
		 * " POLYGON    0.17122726  -0.10106889   0.13027200  -0.05699150   0.08659438  -0.09551925   0.12639146  -0.14028271   0.17122726  -0.10106889 "
		 * +
		 * " POLYGON    0.14853299  -0.12195218   0.10709987  -0.07638871   0.06203886  -0.11632755   0.10235822  -0.16286597   0.14853299  -0.12195218"
		 * ;
		 * 
		 */
		return hst;
	}

	@Test
	public void testFpString() {
		FOOTPRINT fp = FOOTPRINT.HST;
		List<Region> footprintRegions = footprintFactory.getFootprintAsRegionsFromString(getFootprintString(),
				new WorldPt(0, 0), false);// FootprintFactory.getFootprintAsRegions(fp,
									// new WorldPt(0,0));

		MockPlot plot = new MockPlot();
		RegionConnection regConnection = new RegionConnection(footprintRegions);
		int i = 0, c = 0;
		for (Region r : footprintRegions) {

			DrawObj drawObj = regConnection.makeRegionDrawObject(r, plot, false);
			System.out.println(r.toString());
			System.out.println(drawObj.toString());
			if (drawObj instanceof FootprintObj) {
				i++;
			} else {
				if (((ShapeDataObj) drawObj).getShape().equals(ShapeType.Circle)) {
					c++;
				}
			}
		}
		org.junit.Assert.assertTrue(c == 5); // 2 circles are shape data obj and
												// not footprint obj
	}

	@Test
	public void testInstrumentsChildren() {

		String stcFromFootprint = FootprintFactory.getStcFromFootprint(FOOTPRINT.JWST);
		//System.out.println(stcFromFootprint);
		String vals[] = new String[] { "FGS", "MIRI", "NIRCAM", "NIS", "NIRSPEC"};
		FOOTPRINT[] fp = FOOTPRINT.values();
		for (int f = 0; f < fp.length; f++) {
			INSTRUMENTS[] values = FootprintFactory.getInstruments(fp[f]);// .values();
			// System.out.println(fp[f].name());
			for (int i = 0; i < values.length; i++) {
				assertEquals(values[i].name(), vals[i]);
			}
		}
	}

	@Test
	public void testSplit() {
		FOOTPRINT fp = FOOTPRINT.JWST;
		String stcFromFootprint = FootprintFactory.getStcFromFootprint(FOOTPRINT.JWST);// FootprintFactory.getFootprintStcStringDef(fp)
		String[] split = stcFromFootprint.split("\\s");
		int polys = 0, circle = 0, pickle = 0;
		HashMap<String, List<Double>> map = new HashMap<>();
		HashMap<String, List<Double>> mapCir = new HashMap<>();
		HashMap<String, List<Double>> mapPick = new HashMap<>();
		ArrayList<Double> lst = null;
		for (int i = 0; i < split.length; i++) {
			String val = split[i].trim();
			if (val.length() > 0) {
				if (val.startsWith("POL")) {
					lst = new ArrayList<Double>();
					map.put(val + polys, lst);
					polys++;
				} else if (val.startsWith("CIR")) {
					lst = new ArrayList<Double>();
					mapCir.put(val + circle, lst);
					circle++;
				} else if (val.startsWith("PI")) {
					lst = new ArrayList<Double>();
					mapPick.put(val + pickle, lst);
					pickle++;
				} else {
					// System.out.println(polys+": "+trim);
					lst.add(Double.parseDouble(val));
				}

			}
		}
		if (fp.equals(FOOTPRINT.JWST)) {
			Assert.assertTrue(circle == 0);
			Assert.assertTrue("Wrong " + polys, polys == 29);
			Set<String> keySet = mapCir.keySet();
			for (String string : keySet) {
				Assert.assertTrue("Wrong " + map.get(string).size(), map.get(string).size() == 8);// should
			}
		} else if (fp.equals(FOOTPRINT.WFIRST)) {
			Assert.assertTrue(circle == 1);
			Assert.assertTrue("Wrong " + polys, polys == 18);
			Set<String> keySet = mapCir.keySet();
			for (String string : keySet) {
				Assert.assertTrue("Wrong " + mapCir.get(string).size(), mapCir.get(string).size() == 3);// should
			}
		} else if (fp.equals(FOOTPRINT.HST)) {
			Assert.assertTrue(circle == 5);
			Assert.assertTrue("Wrong " + polys, polys == 13);
			Set<String> keySet = mapCir.keySet();
			for (String string : keySet) {
				Assert.assertTrue("Wrong " + mapCir.get(string).size(), mapCir.get(string).size() == 3);// should
			}
		}
		WorldPt refCenter = new WorldPt(0, 90);
		FOOTPRINT[] values = FOOTPRINT.values();
		for (FOOTPRINT footprint : values) {

			footprintFactory.getFootprintAsRegions(footprint, refCenter, false);
		}
		Set<String> keySet = map.keySet();
		for (String string : keySet) {
			List<Double> list = map.get(string);
			Double[] array = list.toArray(new Double[list.size()]);
			WorldPt[] pts = new WorldPt[array.length / 2];
			for (int i = 0; i < array.length / 2; i++) {
				WorldPt pt0 = new WorldPt(array[2 * i].doubleValue(), array[2 * i + 1].doubleValue());
				pts[i] = VisUtil.calculatePosition(refCenter, pt0.getLon() * 3600, pt0.getLat() * 3600);
				System.out.println(pts[i].toString());
			}
		}
	}

	@Test
	public void testCosineRa() {
		// FGS
		String polRA00 = "POLYGON    0.03794999  -0.17407720   0.03843333  -0.21384895   0.07796106  -0.21439047   0.07665551  -0.17417432   0.03794999  -0.17407720 "
				+ " POLYGON   -0.01246111  -0.17438280  -0.01299444  -0.21437121   0.02619722  -0.21371843   0.02595278  -0.17398279  -0.01246111  -0.17438280 ";
		

		double dec = 40;
		String polDEC40 = "POLYGON    0.04941422  39.82591228   0.05001450  39.78614026   0.10145236  39.78556517   0.09981202  39.82578277   0.04941422  39.82591228";
		String polDec90 = "POLYGON   12.29839928  89.82183415  10.18848870  89.78272485  19.98322603  89.77187466  23.75460655  89.80970354  12.29839928  89.82183415";
		String footprintStcStringDef = INSTRUMENTS.FGS.getStc();
		Assert.assertTrue(footprintStcStringDef.trim(), polRA00.trim().equals(footprintStcStringDef.trim()));
		String def = FootprintFactory.getFootprintStcStringDef(FOOTPRINT.JWST, INSTRUMENTS.MIRI);
		Assert.assertTrue(def.trim(), def.equals(INSTRUMENTS.MIRI.getStc()));

		List<Region> listref = footprintFactory.getFootprintAsRegionsFromString(polRA00, new WorldPt(0, 0), false);
		for (Region region : listref) {
			WorldPt[] ptAry = ((RegionLines) region).getPtAry();
			for (int i = 0; i < ptAry.length - 1; i++) {
				System.out.println("Dist " + i + " " + ptAry[i].toString() + ":"
						+ VisUtil.computeDistance(ptAry[i], ptAry[i + 1]));
			}
		}
		List<Region> list2 = footprintFactory.getFootprintAsRegionsFromString(polDEC40, new WorldPt(0, 0), false);
		for (Region region : list2) {
			WorldPt[] ptAry = ((RegionLines) region).getPtAry();

			for (int i = 0; i < ptAry.length - 1; i++) {
				System.out.println("Dist " + i + " " + ptAry[i].toString() + ":"
						+ VisUtil.computeDistance(ptAry[i], ptAry[i + 1]));
			}
		}
		dec = 90;
		list2 = footprintFactory.getFootprintAsRegionsFromString(polDec90, new WorldPt(0, 0), false);
		for (Region region : list2) {
			WorldPt[] ptAry = ((RegionLines) region).getPtAry();
			for (int i = 0; i < ptAry.length - 1; i++) {
				System.out.println("Dist " + i + " " + ptAry[i].toString() + ":"
						+ VisUtil.computeDistance(ptAry[i], ptAry[i + 1]));
			}
		}
	}

	@Test
	public void testDist(){
		INSTRUMENTS inst = INSTRUMENTS.NIS;
		List<Region> list = footprintFactory.getFootprintAsRegions(
				FOOTPRINT.JWST, 
				inst,
				new WorldPt(45, 45), false);
		List<Region> list2 = footprintFactory.getFootprintAsRegions(
				FOOTPRINT.JWST, 
				inst,
				new WorldPt(0,0), false);
		
		WorldPt worldCoordCenters = footprintFactory.getWorldCoordCenter();
		assertEquals("Should found lon = "+worldCoordCenters.getLon() ,worldCoordCenters.getLon(), 359.919,1E-2);
		assertEquals("Should found lat = "+worldCoordCenters.getLat() ,worldCoordCenters.getLat(),-0.1937, 1E-2);
		
		double computeDistance = VisUtil.computeDistance(worldCoordCenters, new WorldPt(0,0));
		Assert.assertEquals("Should found dist = "+computeDistance ,computeDistance,0.209, 1E-2);
		
		footprintFactory.getFootprintAsRegions(
				FOOTPRINT.JWST, 
				new WorldPt(0,0), false);
		worldCoordCenters = footprintFactory.getWorldCoordCenter();
		assertEquals("Should found lon = "+worldCoordCenters.getLon() ,worldCoordCenters.getLon(), 359.999,1E-2);
		assertEquals("Should found lat = "+worldCoordCenters.getLat() ,worldCoordCenters.getLat(),-0.135, 1E-2);
		
		footprintFactory.getFootprintAsRegions(
				FOOTPRINT.JWST, INSTRUMENTS.NIRSPEC,
				new WorldPt(0,0), false);
		worldCoordCenters = footprintFactory.getWorldCoordCenter();
		assertEquals("Should found lon = "+worldCoordCenters.getLon() ,worldCoordCenters.getLon(), 0.105,1E-2);
		assertEquals("Should found lat = "+worldCoordCenters.getLat() ,worldCoordCenters.getLat(),-0.119, 1E-2);
		computeDistance = VisUtil.computeDistance(worldCoordCenters, new WorldPt(0,0));
		assertEquals("Should found dist = "+computeDistance ,computeDistance,0.159, 1E-2);
		
		double[] dist = null, dist1 = null;
		WorldPt[] ptAry = null, ptAry1 = null;
		for (Region region : list) {
			System.out.println(list.size());
			ptAry = ((RegionLines) region).getPtAry();
			dist = new double[ptAry.length];			
		}
		
		for (Region region : list2) {
			System.out.println(list2.size());
			ptAry1 = ((RegionLines) region).getPtAry();
			dist1 = new double[ptAry1.length];
		}
		
		for (int i = 0; i < dist1.length-1; i++) {
			dist[i] = VisUtil.computeDistance(ptAry[i], ptAry[i + 1]);
			dist1[i] = VisUtil.computeDistance(ptAry1[i], ptAry1[i + 1]);
			System.out.println(dist[i]*3600+ ", "+dist1[i]*3600);
		}
		List<WorldPt> lst = new ArrayList<>();
		for (int i = 0; i < ptAry1.length; i++) {
			lst.add(ptAry1[i]);
		}
		CentralPointRetval cp = VisUtil.computeCentralPointAndRadius(lst);
		System.out.println(cp.getWorldPt()+", "+cp.getRadius()*3600);//arcsec
	}
	
	
	@Test
	public void testConvertToNewReference() {
		// FGS1
		String polRA00 = "POLYGON    0.03794999  -0.17407720   0.03843333  -0.21384895   0.07796106  -0.21439047   0.07665551  -0.17417432   0.03794999  -0.17407720";
//		String polDec90 = "POLYGON   12.29839928  89.82183415  10.18848870  89.78272485  19.98322603  89.77187466  23.75460655  89.80970354  12.29839928  89.82183415";
		WorldPt refCenter = new WorldPt(0, 90);

		footprintFactory.getFootprintAsRegionsFromString(polRA00, new WorldPt(0, 0), false);
		WorldPt worldCoordCenter = footprintFactory.getWorldCoordCenter();
		System.out.println(worldCoordCenter);
		HashMap<String, List<Double>> map = footprintFactory.getRegionMap();
		Set<String> keySet = map.keySet();
		for (String string : keySet) {
			List<Double> list = map.get(string);
			Double[] array = list.toArray(new Double[list.size()]);
			WorldPt[] pts = new WorldPt[array.length / 2];
			for (int i = 0; i < array.length / 2; i++) {
				WorldPt pt0 = new WorldPt(array[2 * i].doubleValue(), array[2 * i + 1].doubleValue());
				pts[i] = VisUtil.calculatePosition(refCenter, pt0.getLon() * 3600, pt0.getLat() * 3600);
				System.out.println(pts[i].toString());
			}
		}
		
		

	}

	/**
	 * Used for building regions - only matter height and zoom level
	 * 
	 * @author ejoliet
	 *
	 */
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
