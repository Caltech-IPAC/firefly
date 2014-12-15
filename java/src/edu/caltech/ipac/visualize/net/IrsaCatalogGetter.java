package edu.caltech.ipac.visualize.net;


import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.HostPort;
import edu.caltech.ipac.util.download.NetworkManager;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.irsa.CatalogDocument;
import edu.caltech.ipac.irsa.HoldingsDocument;

import org.apache.xmlbeans.XmlOptions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;


/**
 * @author Xiuqin Wu, modeled after Trey Roby's IrsaImageGetter
 * @version $Id: IrsaCatalogGetter.java,v 1.11 2011/06/10 21:04:09 xiuqin Exp $
 */
public class IrsaCatalogGetter {


	private static CatalogDocument.Catalog[] _catList = getIrsaCatList();

	private static CatalogDocument.Catalog[] getIrsaCatList() {

		CatalogDocument.Catalog[] catalog;
		try {
			XmlOptions xmlOptions = new XmlOptions();
			HashMap<String, String> substituteNamespaceList =
					new HashMap<String, String>();
			substituteNamespaceList.put("", "http://irsa.ipac.caltech.edu");
			xmlOptions.setLoadSubstituteNamespaces(substituteNamespaceList);
			xmlOptions.setSavePrettyPrint();
			xmlOptions.setSavePrettyPrintIndent(4);


			URL url = new URL("http://irsa.ipac.caltech.edu/cgi-bin/Oasis/CatList/nph-catlist?");

			String data = URLDownload.getStringFromURL(url, null);
			HoldingsDocument holdings = parseHoldings(data, xmlOptions);
			//HoldingsDocument holdings = HoldingsDocument.Factory.parse(data,
			//                                                    xmlOptions);

			catalog = holdings.getHoldings().getCatalogArray();

			System.out.println("got IRSA DB meta data, one entry listed:");

			//for(int i = 0; i < catalog.length; i++) {
			String catname = catalog[0].getCatname();
			String database = catalog[0].getDatabase();
			String server = catalog[0].getServer();
			System.out.println("catname = " + catname);
			System.out.println("database = " + database);
			System.out.println("server = " + server);
			System.out.println("*********");
			//}

		} catch (Exception e) {
			System.out.println("can't get IRSA database holding meta data.");
			System.out.println(e.getMessage());
			catalog = null;
		}

		return catalog;
	}


	public static void lowlevelGetCatalog(IrsaCatalogParams params,
	                                      File outFile)
			throws FailedRequestException,
			IOException {
		ClientLog.message("Retrieving  catalogs");

		String cgiapp = "/cgi-bin/Oasis/CatSearch//nph-catsearch";

		URLParms parms = makeURLParms(params);

		String file = outFile.getPath();
		HostPort hp = NetworkManager.getInstance().getServer(NetworkManager.IRSA);

		IrsaUtil.getURL(false, hp, cgiapp, parms, file);  //not image
	}


	static URLParms makeURLParms(IrsaCatalogParams params) {
		String dbServer = "@rmt_stone";
		// name of the database to be searched
		String dbName = "iras";
		// name of the catalog to be searched
		String catName = params.getCatalogName();
		String selectString = "";
		String whereString = null;
		String[]  catInfo = new String[2];

		if (catName.equals("iraspsc")) {
			dbName = "iras";
			catInfo = getCatInfo(catName);
			if (catInfo != null) {
				dbName = catInfo[0];
				dbServer = catInfo[1];
			}
			selectString = "select pscname,ra,dec, " +
					"fnu_12,fnu_25,fnu_60,fnu_100," +
					"fqual_12, fqual_25, fqual_60,  fqual_100, " +
					"relunc_12, relunc_25, relunc_60,  relunc_100, " +
					"major, minor, posang," +
					"cirr1, cirr2, cirr3";

		} else if (catName.equals("irasfsc")) {
			dbName = "iras";
			catInfo = getCatInfo(catName);
			if (catInfo != null) {
				dbName = catInfo[0];
				dbServer = catInfo[1];
			}
			selectString = "select fscname,ra,dec, " +
					"fnu_12,fnu_25,fnu_60,fnu_100," +
					"fqual_12, fqual_25, fqual_60,  fqual_100, " +
					"relunc_12, relunc_25, relunc_60,  relunc_100, " +
					"uncmajor, uncminor, posang," +
					"cirrus";

		} else if (catName.equals("pt_src_cat")) {
			dbServer = "@rmt_stone";
			dbName = "fp_2mass";
			catName = "fp_psc";
			catInfo = getCatInfo(catName);
			if (catInfo != null) {
				dbName = catInfo[0];
				dbServer = catInfo[1];
			}
			selectString = "select ra,dec,designation,j_m,h_m,k_m";
		} else if (catName.equals(CatalogConstants.EXT_SRC_CAT_2MASS)) {
			dbServer = "@rmt_stone";
			dbName = "fp_2mass";
			catName = "fp_xsc";
			catInfo = getCatInfo(catName);
			if (catInfo != null) {
				dbName = catInfo[0];
				dbServer = catInfo[1];
			}
			selectString = "select ra,dec,designation,j_m,h_m,k_m";
		} else if (catName.equals("msx")) {
			dbServer = "@rmt_boulder";
			dbName = "msx";
			catName = "msxc6";
			catInfo = getCatInfo(catName);
			if (catInfo != null) {
				dbName = catInfo[0];
				dbServer = catInfo[1];
			}
			selectString = "select name, ra, dec, b1, b2, a, c, d, e";   // ????  XW modify here ???
		} else if (catName.equals("peakup_2mass")) {
			dbServer = "@rmt_stone";
			dbName = "fp_2mass";
			catName = "fp_psc";
			catInfo = getCatInfo(catName);
			if (catInfo != null) {
				dbName = catInfo[0];
				dbServer = catInfo[1];
			}
			selectString = "select ra,dec,designation,j_m,j_cmsig,h_m,h_cmsig," +
					" k_m, k_cmsig, rd_flg,bl_flg,cc_flg, a, ext_key, " +
					" gal_contam, dist_opt, vr_m_opt, mp_flg, jdate";

			/*
					  whereString = " where j_m>5.0 and h_m>5.0 and k_m>5.0 and k_m<6.62 and " +
					  "id_opt !=null and extd_flg=0 and dist_opt<5.0 and j_msig <0.1 and "+
					  "h_msig<0.1 and k_msig<0.1 and abs(j_m-vr_m_opt)<5.0 and mp_flg=0";
					  */
			whereString = " where gal_contam=0 and dist_opt<5.0 and j_cmsig <0.1 and " +
					"h_cmsig<0.1 and k_cmsig<0.1 and abs(j_m-vr_m_opt)<5.0 and mp_flg=0";
		} else if (catName.equals("peakup_pcrs")) {
			dbServer = "@rmt_dbms21";
			catName = "pcrs";
			dbName = "public";
			selectString = "select ra,dec,starid,prpmtnra,prpmtndc,vmag,q, epoch";
			whereString = " where v=0";
        } else if (catName.equals(CatalogConstants.ALLWISE)) {
            catInfo = getCatInfo(catName);
            if (catInfo != null) {
                dbName = catInfo[0];
                dbServer = catInfo[1];
            }
            selectString = "select designation,ra,dec,ph_qual,"+
                    "w1mpro,w1sigmpro,w2mpro,w2sigmpro,"+
                    "w3mpro,w3sigmpro,w4mpro,w4sigmpro";
        } else  if (catName.equals(CatalogConstants.SEIP)) {
            catInfo = getCatInfo(catName);
            if (catInfo != null) {
                dbName = catInfo[0];
                dbServer = catInfo[1];
            }
            selectString = "select ObjID,ra,dec,l,b,NMatches,NBands,I1_F_Ap1,I1_dF_Ap1,I2_F_Ap1,I2_dF_Ap1,"+
                    "I3_F_Ap1,I3_dF_Ap1,I4_F_Ap1,I4_dF_Ap1,M1_F_PSF,M1_dF_PSF";
        }
		/* not searchable by  position   2/1/2005 XW
			   else if (catName.equals("pt_src_art")) { //CR6192, 2mass artifact catalog
				  dbServer = "@rmt_stone";
				  dbName = "twomass";
				  selectString = "select ra,dec,cc_flg";
				  }
			   */
		else
			ClientLog.warning("not supported catalog search");
		// Other catalogs
		URLParms parms = new URLParms();
		parms.add("objstr", params.getIrsaObjectString());
		parms.add("server", dbServer);
		parms.add("database", dbName);
		parms.add("catalog", catName);
		parms.add("within", params.getSize() + " deg");
		if (whereString != null)
			parms.add("sql", selectString + " from " + catName + whereString);
		else
			parms.add("sql", selectString + " from " + catName);


		return parms;
	}

	private static String[] getCatInfo(String catName) {
		String catInfo[] = null;
		if (_catList != null) {
			catInfo = new String[2];
			for (int i = 0; i < _catList.length; i++) {
				if (catName.equals(_catList[i].getCatname())) {
					catInfo[0] = _catList[i].getDatabase();
					catInfo[1] = _catList[i].getServer();
					break;
				}
			}
		}
		return catInfo;
	}


	private static HoldingsDocument parseHoldings(String data,
	                                              XmlOptions xmlOptions)
			throws Exception {

		// The next lines are done with reflections to avoid
		// having to have weblogic.jar in the compile
		//HoldingsDocument holdings = HoldingsDocument.Factory.parse(data,
		//                                                       xmlOptions);

		Class fClass = HoldingsDocument.Factory.class;
		Method parseCall = fClass.getMethod("parse",
				String.class, XmlOptions.class);
		HoldingsDocument holdings = (HoldingsDocument) parseCall.invoke(
				fClass, data, xmlOptions);
		return holdings;
	}
}
/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
 * HOWEVER USED.
 *
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 *
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
 * OF THE SOFTWARE.
 */
