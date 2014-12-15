package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.ClientLog;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.HostPort;
import edu.caltech.ipac.util.download.NetworkManager;
import edu.caltech.ipac.util.download.URLDownload;
import edu.caltech.ipac.visualize.draw.FixedObjectGroup;
import edu.caltech.ipac.visualize.draw.FixedObjectGroupUtils;
import org.apache.xmlbeans.XmlOptions;
import org.usVo.xml.voTable.VOTABLEDocument;


import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;

/**
 * This class gets Ned fits files or list of available images.
 */
public class IsoImageGetter {


	private static final String CGI_CMD = "/aio/jsp/siap.jsp?POS=";

	/**
	 * Searches for all available images for a particular object.
	 * Results include ImageInformation instances that describe
	 * the images, rather than the images themselves.
	 * The search will be performed synchronously with the results
	 * returned immediately.
	 *
	 * @param	params the params with the location to search
	 */
	public static FixedObjectGroup lowlevelSearchForImages(
			IsoImageListParams params)
			throws FailedRequestException,
			IOException {

		FixedObjectGroup fixGroup;

		ClientLog.message("Requesting list of images for \"" +
				params.getIsoObjectString() + "\"...");

		HostPort hp = NetworkManager.getInstance().getServer(
				NetworkManager.ISO_SERVER);

		try {


			String pos = params.getIsoObjectString();
			String urlStr = "http://" + hp.getHost() + ":" + hp.getPort() + CGI_CMD + pos;
			System.out.printf("url: %s%n", urlStr);

			URL url = new URL(urlStr);

			String data = URLDownload.getStringFromURL(url, null);
			// System.out.println(data);

			XmlOptions xmlOptions = new XmlOptions();
			HashMap<String, String> substituteNamespaceList =
					new HashMap<String, String>();
			substituteNamespaceList.put("", "http://us-vo.org/xml/VOTable.xsd");
			xmlOptions.setLoadSubstituteNamespaces(substituteNamespaceList);
			xmlOptions.setSavePrettyPrint();
			xmlOptions.setSavePrettyPrintIndent(4);

			VOTABLEDocument voTableDoc = parseVoTable(data, xmlOptions);
//        VOTABLEDocument voTableDoc = VOTABLEDocument.Factory.parse(
//                                                     data,xmlOptions);
//        PrintWriter outF= new PrintWriter(new File("vo.dat"));
//        outF.println(voTableDoc.toString());

			//System.out.println(voTableDoc.toString());

			fixGroup = FixedObjectGroupUtils.makeFixedObjectGroup(
					voTableDoc);

		} catch (Exception e) {
			throw new FailedRequestException("parseError",
					"parse Error more Details", e);
		}
		return fixGroup;
	}


	private static VOTABLEDocument parseVoTable(String data,
	                                            XmlOptions xmlOptions)
			throws Exception {

		// The next lines are done with reflections to avoid
		// having to have weblogic.jar in the compile
		//VOTABLEDocument voTableDoc = VOTABLEDocument.Factory.parse(
		//                              data,xmlOptions);
		Class fClass = VOTABLEDocument.Factory.class;
		Method parseCall = fClass.getMethod("parse",
				String.class, XmlOptions.class);
		return (VOTABLEDocument) parseCall.invoke(fClass, data, xmlOptions);
	}


	public static void main(String args[]) {
		try {
			lowlevelSearchForImages(new IsoImageListParams(10, 41));
		} catch (Exception e) {
			e.printStackTrace();
		}
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
