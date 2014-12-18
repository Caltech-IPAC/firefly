package edu.caltech.ipac.fuse.core;

import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.SearchDescResolver;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.creator.SearchDescResolverCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;

/**
 * Date: Sep 9, 2013
 *
 * @author loi
 * @version $Id: HeritageSearchDescResolver.java,v 1.8 2011/12/12 21:25:59 tatianag Exp $
 */
public class FuseSearchDescResolver extends SearchDescResolver implements SearchDescResolverCreator {

    public static final String ID = Application.getInstance().getAppName()+ "-" + WidgetFactory.SEARCH_DESC_RESOLVER_SUFFIX;

    public SearchDescResolver create() {
        return this;
    }

    public String getDesc(Request req) {
        return super.getDesc(req);
    }

//====================================================================
//
//====================================================================

}
