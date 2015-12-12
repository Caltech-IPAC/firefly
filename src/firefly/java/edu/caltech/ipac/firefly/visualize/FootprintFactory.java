/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.visualize.VisUtil.CentralPointRetval;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.dd.RegionAnnulus;
import edu.caltech.ipac.util.dd.RegionLines;
import edu.caltech.ipac.util.dd.RegionValue;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * The class footprint define combination STC regions based on a reference target at 0,0 result of 
 * http://gsss.stsci.edu/webservices/footprints/webform.aspx
 * The polygons and regions created are deltax, deltay distance
 * @author Emmanuel Joliet
 */
public class FootprintFactory  {

	
	public enum FOOTPRINT { JWST, WFIRST, HST};
	
	/**
	 * World point ra,dec polygons result from Webservice
	 * http://gsss.stsci.edu/webservices/footprints/webform.aspx on target 0,0
	 * 
	 * @author ejoliet
	 *
	 */
	public enum INSTRUMENTS {

		FGS(FOOTPRINT.JWST,
				" POLYGON    0.03794999  -0.17407720   0.03843333  -0.21384895   0.07796106  -0.21439047   0.07665551  -0.17417432   0.03794999  -0.17407720 "	
		        + " POLYGON   -0.01246111  -0.17438280  -0.01299444  -0.21437121   0.02619722  -0.21371843   0.02595278  -0.17398279  -0.01246111  -0.17438280 "),
		
		MIRI(FOOTPRINT.JWST," POLYGON   -0.13494975  -0.08756358  -0.13742196  -0.11857171  -0.10607210  -0.12121628  -0.10358878  -0.08992200  -0.13494975  -0.08756358 "
			+" POLYGON   -0.13951917  -0.08917466  -0.14044138  -0.08903022  -0.14029416  -0.08801078  -0.13937751  -0.08814134  -0.13951917  -0.08917466 "
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
			+ " POLYGON   -0.13902472  -0.09008299  -0.14099972  -0.08982187  -0.14058027  -0.08765244  -0.13862751  -0.08797467  -0.13902472  -0.09008299 "),
		
		NIRCAM(FOOTPRINT.JWST,
				" POLYGON    0.02464166  -0.13766916   0.02470000  -0.15551627   0.04253888  -0.15534680   0.04223888  -0.13754692   0.02464166  -0.13766916 "
				+ "POLYGON    0.02463889  -0.11893871   0.02460833  -0.13651917   0.04220277  -0.13641637   0.04204166  -0.11888313   0.02463889  -0.11893871 "
				+ "POLYGON    0.00560278  -0.13766640   0.00542222  -0.15561073   0.02334444  -0.15555794   0.02327778  -0.13771084   0.00560278  -0.13766640 "
				+ "POLYGON    0.00578333  -0.11889427   0.00566944  -0.13654697   0.02332778  -0.13650806   0.02324722  -0.11894982   0.00578333  -0.11889427 "
				+ "POLYGON    0.00623056  -0.11906927   0.00585833  -0.15510796   0.04201666  -0.15488846   0.04153333  -0.11912202   0.00623056  -0.11906927"
				+ " POLYGON   -0.04223610  -0.11834147  -0.04244444  -0.13584693  -0.02489167  -0.13598862  -0.02487222  -0.11843593  -0.04223610  -0.11834147 "
				+ "POLYGON   -0.04231944  -0.13697193  -0.04269722  -0.15475235  -0.02488055  -0.15501905  -0.02474444  -0.13718862  -0.04231944  -0.13697193 "
				+ "POLYGON   -0.02353611  -0.11844982  -0.02348333  -0.13599140  -0.00585278  -0.13591641  -0.00609722  -0.11828594  -0.02353611  -0.11844982 "
				+ "POLYGON   -0.02353889  -0.13714972  -0.02355556  -0.15499127  -0.00565278  -0.15501351  -0.00588333  -0.13708029  -0.02353889  -0.13714972 "
				+ "POLYGON   -0.04251110  -0.11859980  -0.04293055  -0.15443569  -0.00671944  -0.15462739  -0.00715000  -0.11853316  -0.04251110  -0.11859980"),
		
		NIS(FOOTPRINT.JWST," POLYGON   -0.09936379  -0.17507142  -0.09899435  -0.21256815  -0.06178609  -0.21235445  -0.06216109  -0.17488824  -0.09936379  -0.17507142 "),
		
		NIRSPEC(FOOTPRINT.JWST," POLYGON    0.12323870  -0.14307715   0.08351106  -0.09825257   0.04008055  -0.13705249   0.07901661  -0.18294088   0.12323870  -0.14307715 "
				+ " POLYGON    0.17122726  -0.10106889   0.13027200  -0.05699150   0.08659438  -0.09551925   0.12639146  -0.14028271   0.17122726  -0.10106889 "
				+ " POLYGON    0.14853299  -0.12195218   0.10709987  -0.07638871   0.06203886  -0.11632755   0.10235822  -0.16286597   0.14853299  -0.12195218 "),
		
		//WFIRST(FOOTPRINT.WFIRST, wfirst),	
		//HST(FOOTPRINT.HST, hst);	
		;
		
		private String stc;
		private FOOTPRINT mission;
		
		INSTRUMENTS(FOOTPRINT fp, String stcDef) {
			this.stc = stcDef;
			this.mission = fp;
		}
		public String getStc() {
			return this.stc;
		}

		public FOOTPRINT getMission() {
			return this.mission;
		}
	}
	
	private WebPlot plot;
	private WorldPt centerPoly;
	
	public FootprintFactory() {
	}

	
	public static INSTRUMENTS[] getInstruments(FOOTPRINT parent){
		INSTRUMENTS[] allChildren = INSTRUMENTS.values();
		List<INSTRUMENTS> list = new ArrayList<INSTRUMENTS>();
		for (int i = 0; i < allChildren.length; i++) {
			if(allChildren[i].getMission().equals(parent)){
				list.add(allChildren[i]);
			}
		}
		return list.toArray(new INSTRUMENTS[list.size()]);
	}
	
	public static String getStcFromInstrument(INSTRUMENTS inst){
		String stc = "";
		INSTRUMENTS[] values = getInstruments(inst.getMission());
		for (int i = 0; i < values.length; i++) {
			if(inst.equals(values[i])){
				return values[i].getStc();
			}
		}
		return stc;
	}
	
	public static String getStcFromFootprint(FOOTPRINT parent){
		String stc = "";
		INSTRUMENTS[] values = getInstruments(parent);
		for (int i = 0; i < values.length; i++) {
			if(values[i].getStc()!=null){
				stc += values[i].getStc();
			}
		}
		return stc;
	}
	
	/*
	 * Search result of target ra,dec=0,0 from http://gsss.stsci.edu/webservices/footprints/webform.aspx:
	 * These are x,y tuple of world coordinates representing polygons, SHULB be number of lines composing the polygon = number of value / 2.
	 * FIXME: the code should handle STC in general such as CIRCLE for instance
	 */	
	static String jwstPolys = 
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
	 
	static String wfirst=
			" POLYGON    0.25259727  -0.63652538   0.18535222  -0.53098462   0.29090550  -0.46374047   0.35814936  -0.56928187   0.25259727  -0.63652538 "
			+" POLYGON    0.17129449  -0.50892057   0.10404886  -0.40337378   0.20960315  -0.33612898   0.27684796  -0.44167629   0.17129449  -0.50892057   "
			+" POLYGON    0.32395386  -0.74851356   0.25670956  -0.64297952   0.36226158  -0.57573605   0.42950439  -0.68127084   0.32395386  -0.74851356   "
			+" POLYGON    0.12249390  -0.67947994   0.05524804  -0.57393880   0.16080275  -0.50669700   0.22804799  -0.61223917   0.12249390  -0.67947994   "
			+" POLYGON    0.04119016  -0.55187464  -0.02605586  -0.44632766   0.07949922  -0.37908478   0.14674498  -0.48463268   0.04119016  -0.55187464   "
			+" POLYGON    0.19385168  -0.79146890   0.12660618  -0.68593418   0.23216022  -0.61869347   0.29940479  -0.72422929   0.19385168  -0.79146890   "
			+" POLYGON   -0.02788750  -0.69060754  -0.09513344  -0.58506478   0.01042158  -0.51782582   0.07766756  -0.62336988  -0.02788750  -0.69060754   "
			+" POLYGON   -0.10919120  -0.56300044  -0.17643680  -0.45745257  -0.07088221  -0.39021211  -0.00363622  -0.49576118  -0.10919120  -0.56300044   "
			+" POLYGON    0.04347085  -0.80259864  -0.02377517  -0.69706183   0.08177992  -0.62982418   0.14902566  -0.73536247   0.04347085  -0.80259864   "
			+" POLYGON    0.36460428  -0.56516954   0.29736051  -0.45962815   0.40291165  -0.39238234   0.47015377  -0.49792399   0.36460428  -0.56516954   "
			+" POLYGON    0.28330305  -0.43756393   0.21605831  -0.33201656   0.32161107  -0.26477056   0.38885450  -0.37031807   0.28330305  -0.43756393   "
			+" POLYGON    0.43595930  -0.67715847   0.36871660  -0.57162372   0.47426597  -0.50437816   0.54150670  -0.60991330   0.43595930  -0.67715847   "
			+" POLYGON    0.45851509  -0.46541299   0.39127277  -0.35986989   0.49682155  -0.29262341   0.56406181  -0.39816640   0.45851509  -0.46541299   "
			+" POLYGON    0.37721562  -0.33780533   0.30997202  -0.23225680   0.41552284  -0.16501065   0.48276473  -0.27055894   0.37721562  -0.33780533   "
			+" POLYGON    0.52986835  -0.57740415   0.46262738  -0.47186719   0.56817392  -0.40462059   0.63541252  -0.51015748   0.52986835  -0.57740415   "
			+" POLYGON    0.53214873  -0.33383070   0.46490781  -0.22828506   0.57045424  -0.16103935   0.63769278  -0.26658443   0.53214873  -0.33383070   "
			+" POLYGON    0.45085085  -0.20622018   0.38360840  -0.10066994   0.48915736  -0.03342506   0.55639778  -0.13897459   0.45085085  -0.20622018   "
			+" POLYGON    0.60350015  -0.44582541   0.53626082  -0.34028512   0.64180475  -0.27303880   0.70904143  -0.37857861   0.60350015  -0.44582541   "
			+" CIRCLE    0.08333327  -0.10416644   0.00416667 "
			+ "CIRCLE   -0.36110633   0.22221669   0.00555556";
	
	static String hst = ""
			+ "POLYGON    0.04443055   0.07030828   0.10070267   0.06821097   0.10157212   0.04029993   0.04611388   0.04307220   0.04443055   0.07030828   "
			+" POLYGON    0.04305555   0.09917487   0.09989434   0.09782198   0.10071656   0.06894430   0.04444999   0.07100272   0.04305555   0.09917487   "
			+" POLYGON    0.06131664   0.12724694   0.05330554   0.12808307   0.05328332   0.13514413   0.06136109   0.13432190   0.06131664   0.12724694   "
			+" POLYGON    0.06194720   0.12593028   0.05245554   0.12682196   0.05239443   0.13538303   0.06199720   0.13445245   0.06194720   0.12593028 "
			+" CIRCLE    0.06462497  -0.06595826   0.00069444 "
			+" CIRCLE    0.06462497  -0.06595826   0.00069444   "
			+" POLYGON    0.12901798   0.11616805   0.13973823   0.10302369   0.14912465   0.08889596   0.15708764   0.07391970   0.16355120   0.05823786   0.16845363   0.04200014   0.17174815   0.02536152   0.17340331   0.00848082   0.17340331  -0.00848082   0.17174815  -0.02536152   0.16845363  -0.04200014   0.16355120  -0.05823786   0.15708764  -0.07391970   0.14912465  -0.08889596   0.13973823  -0.10302369   0.12901798  -0.11616805   0.17030352  -0.15334139   0.18445419  -0.13599087   0.19684420  -0.11734230   0.20735529  -0.09757369   0.21588714  -0.07687373   0.22235832  -0.05544000   0.22670706  -0.03347709   0.22889186  -0.01119465   0.22889186   0.01119465   0.22670706   0.03347709   0.22235832   0.05544000   0.21588714   0.07687373   0.20735529   0.09757369   0.19684420   0.11734230   0.18445419   0.13599087   0.17030352   0.15334139   0.12901798   0.11616805   "
			+" POLYGON    0.11616835  -0.12901772   0.10302400  -0.13973800   0.08889626  -0.14912447   0.07391997  -0.15708751   0.05823810  -0.16355111   0.04200032  -0.16845359   0.02536164  -0.17174814   0.00848086  -0.17340331  -0.00848086  -0.17340331  -0.02536164  -0.17174814  -0.04200032  -0.16845359  -0.05823810  -0.16355111  -0.07391997  -0.15708751  -0.08889626  -0.14912447  -0.10302400  -0.13973800  -0.11616835  -0.12901772  -0.15334206  -0.17030291  -0.13599157  -0.18445367  -0.11734299  -0.19684379  -0.09757433  -0.20735499  -0.07687427  -0.21588695  -0.05544042  -0.22235822  -0.03347736  -0.22670702  -0.01119474  -0.22889185   0.01119474  -0.22889185   0.03347736  -0.22670702   0.05544042  -0.22235822   0.07687427  -0.21588695   0.09757433  -0.20735499   0.11734299  -0.19684379   0.13599157  -0.18445367   0.15334206  -0.17030291   0.11616835  -0.12901772   "
			+" POLYGON   -0.12901798  -0.11616805  -0.13973823  -0.10302369  -0.14912465  -0.08889596  -0.15708764  -0.07391970  -0.16355120  -0.05823786  -0.16845363  -0.04200014  -0.17174815  -0.02536152  -0.17340331  -0.00848082  -0.17340331   0.00848082  -0.17174815   0.02536152  -0.16845363   0.04200014  -0.16355120   0.05823786  -0.15708764   0.07391970  -0.14912465   0.08889596  -0.13973823   0.10302369  -0.12901798   0.11616805  -0.17030352   0.15334139  -0.18445419   0.13599087  -0.19684420   0.11734230  -0.20735529   0.09757369  -0.21588714   0.07687373  -0.22235832   0.05544000  -0.22670706   0.03347709  -0.22889186   0.01119465  -0.22889186  -0.01119465  -0.22670706  -0.03347709  -0.22235832  -0.05544000  -0.21588714  -0.07687373  -0.20735529  -0.09757369  -0.19684420  -0.11734230  -0.18445419  -0.13599087  -0.17030352  -0.15334139  -0.12901798  -0.11616805   "
			+" POLYGON   -0.07989995   0.08093320  -0.08204995   0.08310819  -0.08422772   0.08095541  -0.08207772   0.07878042  -0.07989995   0.08093320   "
			+" POLYGON   -0.08398605   0.08639984  -0.08778882   0.09014426  -0.09156381   0.08631094  -0.08776104   0.08256651  -0.08398605   0.08639984   "
			+" POLYGON   -0.05819165   0.06560549  -0.06835552   0.07573879  -0.07851384   0.06554713  -0.06834997   0.05541661  -0.05819165   0.06560549   "
			+ " CIRCLE   -0.05935553  -0.06245550   0.01979722 "
			+ " CIRCLE   -0.05935553  -0.06245550   0.01979722 "
			+ " CIRCLE   -0.05935553  -0.06245550   0.01979722   "
			+" POLYGON    0.01545556  -0.01782778  -0.01446667   0.01608611   0.00152500   0.03198333   0.03108889  -0.00191111   0.01545556  -0.01782778   "
			+" POLYGON   -0.00058333  -0.03417222  -0.03089722  -0.00026389  -0.01472778   0.01583889   0.01521944  -0.01804722  -0.00058333  -0.03417222   "
			+" POLYGON    0.00193333  -0.02526111  -0.02477500   0.00130833  -0.00116944   0.02586667   0.02621944  -0.00136389   0.00193333  -0.02526111   "
			+" POLYGON    0.00193333  -0.02526111  -0.02477500   0.00130833  -0.00116944   0.02586667   0.02621944  -0.00136389   0.00193333  -0.02526111";
	
	private HashMap<String,List<Double>> map;
	private double[] offset;
	
	public HashMap<String, List<Double>> getRegionMap() {
		return map;
	}
	
	/**
	 * Get polygons strings in absolute ra,dec world point defined as follow for n number of vertices:
	 * 
	 * POLYGON x1 y1 x2 y2 ... xn yn, 
	 * 
	 * where 'POLYGON', can be also 'CIRCLE' is the shape type, xn,yn the absolute world coordinate ra,dec in J2000 equ.
	 * If CIRCLE, xi yi radius has to be ound instead
	 *
	 * @param fp {@link FOOTPRINT} missio footprint full FoV
	 * @return region string
	 */
	public static String getFootprintStcStringDef(FOOTPRINT fp) {

		switch (fp) {
		case HST:
			//Not yet ready by instruments
			return hst;
		case JWST:
			return getStcFromFootprint(fp);// should be same as jwstPolys
		case WFIRST:
			//Not yet ready by instruments
			return wfirst;
		default:
			return null;

		}

	}
	
	/**
	 * Same as {@link #getFootprintStcStringDef(FOOTPRINT)} but given a particular instrument
	 * @param fp
	 * @param inst
	 * @return string region definition
	 */
	public static String getFootprintStcStringDef(FOOTPRINT fp, INSTRUMENTS inst) {

		switch (fp) {
		case HST:
			getStcFromFootprint(fp);
		case JWST:
			return getStcFromInstrument(inst);
//			return jwstPolys;
		case WFIRST:
			return getStcFromFootprint(fp);
		default:
			return null;

		}

	}
	public List<Region> getFootprintAsRegionsFromString(String stc, WorldPt refCenter, boolean moveToRelCenter) {
		
		return getRegionFromStc(stc,refCenter,moveToRelCenter);
	}
	
	/**
	 * Get the regions from the STC regions defined as a result of the generated geometry from gsss.stsci.edu
	 * @param fp the {@link FOOTPRINT} value
	 * @param refCenter
	 *            as our geometry is relative to the target used for generating
	 *            the STC, we need to pass the new ref center when overlaying on
	 *            a paritcular image not referenced to the one used previously
	 *            to generate the geometry
	 * @return
	 */
	public List<Region> getFootprintAsRegions(FOOTPRINT fp, WorldPt refCenter, boolean moveToRelativeCenter) {
		
		String def = getFootprintStcStringDef(fp);
		List<Region> fpRegions = getRegionFromStc(def,refCenter, moveToRelativeCenter); //Full footprint, don't recenter
		return fpRegions;
	}
	
	public List<Region> getFootprintAsRegions(FOOTPRINT fp, INSTRUMENTS inst, WorldPt refCenter, boolean moveToRelativeCenter) {
		
		String def = getFootprintStcStringDef(fp, inst);
		List<Region> fpRegions = getRegionFromStc(def,refCenter, moveToRelativeCenter); // Recenter to center of the polygons
		return fpRegions;
	}
	
	private List<Region> getRegionFromStc(String def, WorldPt refCenter, boolean moveToRelativeCenter ) {
		List<Region> fpRegions = new ArrayList<Region>();
		String[] split = def.split("\\s");
		int polys=0, circle = 0, pickle = 0;
		map = new HashMap<>();
		ArrayList<Double> lst = null;
		for (int i = 0; i < split.length; i++) {
			String val = split[i].trim();
			if(val.length()>0){
				if(val.startsWith("POL")){
					lst = new ArrayList<Double>();
					map.put(val+polys, lst);
					polys++;
				}else if(val.startsWith("CIR")){
					lst = new ArrayList<Double>();
					map.put(val+circle, lst);
					circle++;
				}else if(val.startsWith("PI")){
					lst = new ArrayList<Double>();
					map.put(val+pickle, lst);
					pickle++;
				}else{
					lst.add(Double.parseDouble(val));
				}
				
			}			
		}
		
		// POLYGONS
		//First calculate the footprint translated to a reference target and store the world points in a list:
		
		List<WorldPt[]> lstWp = new ArrayList<>();
		Set<String> keySet = map.keySet();
		for (String string : keySet) {
			List<Double> list = map.get(string);
			Double[] array = list.toArray(new Double[list.size()]);			
			if(string.startsWith("POL")){
				lstWp.add(getWorldPoints(array,refCenter));
			}
		}
		
		// Calculate the center of polygon
		List<WorldPt> lstWpt = new ArrayList<>();
		for (WorldPt[] wpts : lstWp) {
			for (int i = 0; i < wpts.length; i++) {
				lstWpt.add(wpts[i]);
			}
		}
		WorldPt[] ptArray = lstWpt.toArray(new WorldPt[]{});	
		
		centerPoly = getCenter(ptArray); // center is relative to the footprint center (0,0) in our input case

		offset = new double[] { 0, 0 };
		if (moveToRelativeCenter) {
			// Should be x,y=0,0 if full footprint: polygon center = refCenter
			offset = getCenterOffset(centerPoly, refCenter); // offset of the
																// center
																// polygon with
																// respect to
																// the center of
																// the full
																// footprint
		}
		// Move to relative center and create regions
		// For full footprint - relative center = reference target
		// For individual aperture, relative center is the center of the polygon
		keySet = map.keySet();
		for (String string : keySet) {
			List<Double> list = map.get(string);
			Double[] array = list.toArray(new Double[list.size()]);
			if (string.startsWith("POL")) {
				fpRegions.add(getPolygonRegionLines(array, centerPoly, refCenter, moveToRelativeCenter));
			} else if (string.startsWith("CIR")) {
				fpRegions.add(getCircle(array, refCenter, moveToRelativeCenter));
			} else if (string.startsWith("PIC")) {
				fpRegions.add(getAnnulus(array, refCenter));
			} else {
				throw new RuntimeException(" STC " + string + " not defined in " + FootprintFactory.class.getName());
			}
		}
//		double dist = 135;
//		RegionValue width = new RegionValue(dist, Unit.ARCSEC);
//		RegionValue height = width;
//		RegionBox box = new RegionBox(refCenter, new RegionDimension(width, height));
//		
//		fpRegions.add(box);
//		
		return fpRegions;		
	}

	public WorldPt getWorldCoordCenter(){
		return centerPoly;
	}
	
	public double[] getOffsetCenter(){
		return offset;
	}
	
	
	private Region getAnnulus(Double[] array, WorldPt refCenter) {
		return null;
	}

	private Region getCircle(Double[] array, WorldPt refTarget, boolean moveBackCenter) {
		WorldPt pt0 = new WorldPt(array[0], array[1]);
		WorldPt ptRef= VisUtil.calculatePosition(refTarget, pt0.getLon()*3600d, pt0.getLat()*3600d); // that's ok because offsetRa, offsetDec are from 0,0!
		double[] cRel = new double[]{0,0};
		double radius = array[2];
		WorldPt pt;
		if (moveBackCenter) {
			cRel = getCenterOffset(ptRef, refTarget);

			double screenXPt = plot.getScreenCoords(ptRef).getX() - cRel[0];
			double screenYPt = plot.getScreenCoords(ptRef).getY() - cRel[1];
			pt = plot.getWorldCoords(new ScreenPt(screenXPt, screenYPt));
		} else {
			pt = ptRef;
		}
		
		return new RegionAnnulus(pt, new RegionValue(radius, RegionValue.Unit.DEGREE));

	}
	
	/**
	 * Build world points vertices of a polygon from x,y tuples defined on
	 * ra,dec = 0,0 and translate it to reference by applying the spherical
	 * transformation. Expecting array twice as number of lines in the polygon.
	 * 
	 * @param xyCoords
	 *            original x,y coords result from 0,0 target search
	 * @param refTarget
	 *            new reference target ra,dec
	 * @return world points array defining the absolute vertices of the polygon
	 */
	public WorldPt[] getWorldPoints(Double[] xyCoords, WorldPt refTarget) {
		WorldPt[] pts = new WorldPt[xyCoords.length / 2];
		WorldPt[] tempPts = new WorldPt[xyCoords.length / 2];
		for (int i = 0; i < xyCoords.length / 2; i++) {	
			tempPts[i] = new WorldPt(xyCoords[2 * i].doubleValue(), 
					xyCoords[2 * i + 1].doubleValue());
		}
		
		//Move the footprint from ra,dec=0,0 to refCenter
		for (int j = 0; j < tempPts.length; j++) {
			pts[j] = VisUtil.calculatePosition(refTarget, 
					tempPts[j].getLon() * 3600.0, 
					tempPts[j].getLat() * 3600.0);// that's ok because offsetRa, offsetDec are from 0,0!
//			pts[j] = tempPts[j]; // works only on 0.0
		}
		return pts;
		
	}
	/**
	 * Build region by moving or not polygon to relative center
	 * @param xyCoords original x,y coords result from 0,0 target search
	 * @return
	 */
	public Region getPolygonRegionLines(Double[] xyCoords, WorldPt polyCenter, WorldPt refTarget, boolean moveBackCenter) {
		WorldPt[] tmpPts = getWorldPoints(xyCoords, refTarget); // polys in ref centered on reftarget
		WorldPt[] pts = getWorldPoints(xyCoords, refTarget);
		double[] cRel = new double[]{0,0};
		if (moveBackCenter) { // if true, this is for individual aperture where we want to relocate each point relative to the polygon center
			
			cRel = getCenterOffset(polyCenter, refTarget); // offset of the center polygon with respect to the center of the full footprint
//			GwtUtil.logToServer(Level.INFO, cRel[0]+", "+cRel[1]);
			for (int i = 0; i < tmpPts.length; i++) {
//				double screenXPt = plot.getScreenCoords(pts[i]).getX() - cRel[0];
//				double screenYPt = plot.getScreenCoords(pts[i]).getY() - cRel[1];
//				pts[i] = plot.getWorldCoords(new ScreenPt(screenXPt, screenYPt));
				pts[i] = VisUtil.rotatePosition(polyCenter, refTarget, tmpPts[i]);
			}
		}
		
		RegionLines regionLines = new RegionLines(pts);
		
		return regionLines;
		
	}

	private double[] getCenterOffset(WorldPt polyCenter, WorldPt refTarget) {
		double xp = plot.getScreenCoords(polyCenter).getX();
		double xc = plot.getScreenCoords(refTarget).getX();
		double xrel = xp - xc;
		double yp = plot.getScreenCoords(polyCenter).getY();
		double yc = plot.getScreenCoords(refTarget).getY();
		double yrel = yp - yc;

		return new double[] { xrel, yrel };
	}

	public void setWebPlot(WebPlot plot){
		this.plot = plot;
	}
	
	private WorldPt getCenter(WorldPt[] pts) {
		List<WorldPt> lst = new ArrayList<>();
		for (int i = 0; i < pts.length; i++) {
			lst.add(pts[i]);
		}
		CentralPointRetval cp = VisUtil.computeCentralPointAndRadius(lst);
		return cp.getWorldPt();
	}
	
}

