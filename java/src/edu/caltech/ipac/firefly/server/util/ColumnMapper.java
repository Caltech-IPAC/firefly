/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.util.AppProperties;

import java.util.Properties;

/**
 * Date: Nov 10, 2008
 *
 * @author loi
 * @version $Id: ColumnMapper.java,v 1.1 2008/11/11 23:44:58 loi Exp $
 */
public class ColumnMapper {
    static Properties mapper = new Properties();

    static {
        AppProperties.loadClassPropertiesToPdb(ColumnMapper.class, mapper, true);
    }

    public static String getTitle(String colName) {
        return mapper.getProperty(colName, colName);
    }

}
