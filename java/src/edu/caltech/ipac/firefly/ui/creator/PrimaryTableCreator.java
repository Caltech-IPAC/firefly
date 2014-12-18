package edu.caltech.ipac.firefly.ui.creator;

import edu.caltech.ipac.firefly.data.TableServerRequest;

import java.util.Map;

/**
 * Date: Mar 25, 2010
 *
 * @author loi, Trey
 * @version $Id: PrimaryTableCreator.java,v 1.1 2010/04/24 01:13:04 loi Exp $
 */
public interface PrimaryTableCreator extends UICreator {

    PrimaryTableUI create(TableServerRequest req, Map<String, String> params);
}


