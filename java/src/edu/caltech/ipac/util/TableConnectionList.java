/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util;

public interface TableConnectionList {
    static public final String BULK_UPDATE        = "bulkUpdate";
    static public final String ADD                = "add";
    static public final String REMOVE             = "remove";
    static public final String CURRENT            = "current";
    static public final String ALL_ENTRIES_UPDATED= "allEntriesUpdated";
    static public final String ENTRY_UPDATED      = "entryUpdated";


    public int size();
    public int indexOf(Object o);

}
