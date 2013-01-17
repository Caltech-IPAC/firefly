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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
