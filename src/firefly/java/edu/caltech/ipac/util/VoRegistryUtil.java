/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

import edu.caltech.ipac.util.dd.VOResourceEndpoint;
import uk.ac.starlink.registry.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * @author tatianag
 * Using Mark Taylor's regclient.jar and Ray Plante's ivoa registry rayreg.jar
 * Latest registries can be found here: http://rofr.ivoa.net/
 */
public class VoRegistryUtil {

    // NVO registry does not seem to support adqls search, only keywords
    public static String NVO_REGISTRY = "http://vao.stsci.edu/directory/ristandardservice.asmx?";

    public static String ASTROGRID_REGISTRY = "http://registry.astrogrid.org/"
                                + "astrogrid-registry/services/RegistryQueryv1_0";
    public static String EURO_REGISTRY = "http://registry.euro-vo.org/services/RegistrySearch";

    /* ex. type="ConeSearch", keywords="2mass point catalog" */
    public static List<VOResourceEndpoint> getEndpoints(String type, String keywords) {
        try {
        	return getEndpoints(NVO_REGISTRY, type, keywords);
        } catch (RegistryQueryException e1) {
            System.out.println("ERROR: NVO registry search failed: will try EURO "+e1.getMessage());
            try {
            	return getEndpoints(EURO_REGISTRY, type, keywords);
            } catch (RegistryQueryException e2) {
                System.out.println("ERROR: EURO registry search failed: will try ASTROGRID");
                try {
                    return getEndpoints(ASTROGRID_REGISTRY, type, keywords);
                } catch (RegistryQueryException e3) {
                    System.out.println("ERROR: ASTROGRID registry search failed");
                    return new ArrayList<VOResourceEndpoint>(0);
                }
            }
        }
    }

    public static List<VOResourceEndpoint> getEndpoints(String registry, String type, String keywords) {

        List<VOResourceEndpoint> endpoints = new ArrayList<VOResourceEndpoint>();

        try {
            URL registryUrl = new URL(registry);

            SoapClient sclient = new SoapClient(registryUrl);
            // show http traffic for debugging
            //sclient.setEchoStream(System.out);
            BasicRegistryClient rclient = new BasicRegistryClient( sclient );


			SoapRequest req;
			// if (registry.equals(EURO_REGISTRY)) {
			// use adqls to search only the title - it gives much better results
			String adqls = "( capability/@standardID = 'ivo://ivoa.net/std/" + type + "' ) ";

			String[] words = keywords.split("\\s+");
			for (String w : words) {
				// adqls += " and ( (identifier like '%" + w + "%' OR title like
				// '%" + w + "%') )";
				adqls += " and ( (shortName LIKE '%" + w + "%') OR ( content/description LIKE '%" + w
						+ "%') OR (identifier like '%" + w + "%') OR (title like '%" + w + "%') )";
			}
			adqls += " and ( content/contentLevel like '%research%' )";
			// Construct the SOAP request
			req = RegistryRequestFactory.adqlsSearch(adqls);
			// } else {
			// // keywords search is not precise, since the search must cover
			// identifier,
			// // content/description, title, content/subject etc.
			// // but is the only way to search astrogrid and nvo with this
			// interface
			// req =
			// RegistryRequestFactory.keywordSearch(keywords.split("\\s+"),
			// false);
			// }

            /* Make the request in such a way that the results are streamed. */
            //Iterator<BasicResource> it = rclient.getResourceIterator( req );

            //while ( it.hasNext() ) {
            //    BasicResource res = it.next();
            //    BasicCapability[] caps = res.getCapabilities();
            //    for (BasicCapability cap : caps) {
            //        if (cap.getXsiType() != null && cap.getXsiType().endsWith(type)) {
            //            endpoints.add(new VOResourceEndpoint(res.getIdentifier(), res.getTitle(), cap.getAccessUrl()));
            //        }
            //    }
            //}

            List<BasicResource> resLst =  rclient.getResourceList(req);
            for (BasicResource res : resLst) {
                BasicCapability[] caps = res.getCapabilities();
                for (BasicCapability cap : caps) {
                    if (cap.getXsiType() != null && cap.getXsiType().endsWith(type)) {
                       String desc = cap.getDescription()!=null?cap.getDescription():res.getIdentifier();
                       endpoints.add(new VOResourceEndpoint(res.getIdentifier(), res.getTitle(), res.getShortName(), cap.getAccessUrl(), desc));
                    }
                }
            }
        } catch ( MalformedURLException e ) {
            e.printStackTrace();
            throw new RegistryQueryException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RegistryQueryException(e);
        }
        return endpoints;
    }

    public static void main(String args[]) {

        String keywords = "iphas";
        List<VOResourceEndpoint> endpoints = getEndpoints("ConeSearch", keywords);
        for (VOResourceEndpoint ep : endpoints) {
            System.out.println(ep.getId() + " \"" + ep.getTitle() + "\" "+ep.getDescription()+ " "
                    + "\n\t" + ep.getUrl() + "\n\t");

        }
    }
}
