package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.util.DataGroup;

import java.util.List;

/**
 * @author tatianag
 * $Id: BcdQuery.java,v 1.6 2010/08/06 22:07:23 roby Exp $
 */
public abstract class BcdQuery extends HeritageQuery {

    public String getTemplateName() {
        return "bcdproducts_dd";
    }

    public DataGroup.Attribute[] getAttributes() {
        return Utils.getBcdAttributes();
    }

    @Override
    public void prepareTableMeta(TableMeta defaults, List<edu.caltech.ipac.util.DataType> columns, ServerRequest request) {
        super.prepareTableMeta(defaults, columns, request);
        defaults.setAttribute(DATA_TYPE, DataType.BCD.toString());
    }
}
