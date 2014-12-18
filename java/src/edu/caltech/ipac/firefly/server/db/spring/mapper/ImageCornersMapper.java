package edu.caltech.ipac.firefly.server.db.spring.mapper;

import edu.caltech.ipac.visualize.plot.ImageCorners;
import edu.caltech.ipac.visualize.plot.WorldPt;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Date: Apr 28, 2009
 *
 * @author loi
 * @version $Id: ImageCornersMapper.java,v 1.2 2009/05/01 22:39:36 loi Exp $
 */
public class ImageCornersMapper implements ParameterizedRowMapper<ImageCorners> {
    public ImageCorners mapRow(ResultSet rs, int i) throws SQLException {
        ImageCorners corners = new ImageCorners();
        int cols = rs.getMetaData().getColumnCount();
        for (int idx = 1; idx < cols + 1; idx = idx + 2) {
            double lon = rs.getDouble(idx);
            double lat = rs.getDouble(idx+1);

            if (!(lon == -1.0 && lat == -1.0)) {
                corners.addCorners(new WorldPt(lon, lat));
            }
        }
        return corners;
    }
}
