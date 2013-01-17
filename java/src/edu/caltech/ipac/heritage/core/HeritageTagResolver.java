package edu.caltech.ipac.heritage.core;

import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.commands.TagCmd;
import edu.caltech.ipac.heritage.commands.SearchByRequestIDCmd;


/**
 * Spitzer specific tag resolver to resolve historical tags
 * with tag name ADS/Sa.Spitzer#AORKEY
 * @author tatianag
 *         $Id: HeritageTagResolver.java,v 1.1 2009/11/19 19:48:27 tatianag Exp $
 */
public class HeritageTagResolver extends TagCmd.DefaultTagResolver {

    public HeritageTagResolver() { super(); }

    private boolean supportsTagFormat(String tagname) {
        return tagname.matches("^ADS\\/Sa\\.Spitzer\\#\\d+$");
    }

    public void resolve(String tagname) {
        if (supportsTagFormat(tagname)) {
            String [] parts = tagname.split("#");
            String reqkey = parts[1];
            Request req = SearchByRequestIDCmd.createRequest(reqkey);
            req.setIsDrilldown(true);
            req.setIsDrilldownRoot(false);
            Application.getInstance().processRequest(req);
        } else {
            super.resolve(tagname);
        }
    }
}
