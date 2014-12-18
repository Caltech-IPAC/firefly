package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.core.SearchDescResolver;

/**
 * Date: Sep 22, 2011
 *
 * @author loi
 * @version $Id: SearchDescResolverCreator.java,v 1.1 2011/09/27 00:36:04 loi Exp $
 */
public interface SearchDescResolverCreator extends UICreator {
    SearchDescResolver create();
}
