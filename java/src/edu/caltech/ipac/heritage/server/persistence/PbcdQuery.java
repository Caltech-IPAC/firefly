package edu.caltech.ipac.heritage.server.persistence;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.util.DataGroup;

import java.util.List;

/**
 * @author tatianag
 * $Id: PbcdQuery.java,v 1.8 2010/08/06 22:07:23 roby Exp $
 */
public abstract class PbcdQuery extends HeritageQuery {

    public String getTemplateName() {
        return "postbcdproducts_dd";
    }

    public DataGroup.Attribute[] getAttributes() {
        return Utils.getPbcdAttributes();
    }

    public abstract BcdQuery getBcdQuery();
    
    @Override
    public void prepareTableMeta(TableMeta defaults, List<edu.caltech.ipac.util.DataType> columns, ServerRequest request) {
        super.prepareTableMeta(defaults, columns, request);
        defaults.setAttribute(DATA_TYPE, DataType.PBCD.toString());
    }
}
