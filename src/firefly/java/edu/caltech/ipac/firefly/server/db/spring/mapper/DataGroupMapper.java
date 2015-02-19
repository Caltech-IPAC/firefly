/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.db.spring.mapper;

import edu.caltech.ipac.util.DataGroup;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Date: Oct 7, 2008
 *
 * @author loi
 * @version $Id: DataGroupMapper.java,v 1.1 2009/01/23 01:44:14 loi Exp $
 */
public class DataGroupMapper implements ParameterizedRowMapper<DataGroup> {
    public DataGroup mapRow(ResultSet resultSet, int i) throws SQLException {
        return DataGroupUtil.processResults(resultSet);
    }
}
