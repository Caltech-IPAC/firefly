package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.util.DataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * User: roby
 * Date: Apr 16, 2010
 * Time: 4:03:40 PM
 */


/**
 * @author Trey Roby
 */
public abstract class MetaInsertTableProcessor extends IpacTablePartProcessor {

    private final Map<String,String> _extraMeta= new HashMap<String,String>(7);

    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        // todo: get all the meta data from the xml file and insert it here

              // now add any extra meta data that did not come from the xml file put was added
              // by the process
        for(Map.Entry<String,String> entry : _extraMeta.entrySet()) {
            meta.setAttribute(entry.getKey(), entry.getValue());
        }
    }




}

