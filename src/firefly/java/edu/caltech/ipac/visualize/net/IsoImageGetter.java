/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
