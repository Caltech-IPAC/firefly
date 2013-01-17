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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
