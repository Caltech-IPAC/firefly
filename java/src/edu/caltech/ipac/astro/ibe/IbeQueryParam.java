package edu.caltech.ipac.astro.ibe;

import edu.caltech.ipac.util.StringUtils;

/**
 * Date: 4/17/14
 *
 * @author loi
 * @version $Id: $
 */
public class IbeQueryParam {

    public enum  Intersect {COVERS, ENCLOSED, CENTER, OVERLAPS}

    private String pos;
    private String refBy;           // not referenced in IBE documentation, but used in WISE's source search.
    private String size;
    private String columns;
    private String where;
    private Intersect intersect;
    private boolean mcen = false;       // most centered

    public IbeQueryParam() {
    }

    public IbeQueryParam(String pos, String size) {
        this(pos, size, null, null, Intersect.CENTER, true);
    }

    public IbeQueryParam(String pos, String size, String columns, String where, Intersect intersect, boolean mcen) {
        this.pos = pos;
        this.size = size;
        this.columns = columns;
        this.where = where;
        this.intersect = intersect;
        this.mcen = mcen;
    }

    public String getPos() {
        return pos;
    }

    /**
     * @param pos POS=ra,dec; ie.  POS=10,-89.5
     */
    public void setPos(String pos) {
        this.pos = pos;
    }

    /**
     * Used instead of POS.  This is not documented, but used by WISE
     * @return
     */
    public String getRefBy() {
        return refBy;
    }

    public void setRefBy(String refBy) {
        this.refBy = refBy;
    }

    public String getSize() {
        return size;
    }

    /**
     * @param size SIZE=width[,height];  ie.  SIZE=0.1 or SIZE=1,2
     */
    public void setSize(String size) {
        this.size = size;
    }

    public String getColumns() {
        return columns;
    }

    /**
     * @param columns  columns=name1,name2,....
     */
    public void setColumns(String columns) {
        this.columns = columns;
    }

    public String getWhere() {
        return where;
    }

    /**
     * @param where  where=SQL; ie.  band IN (1,2,3)
     */
    public void setWhere(String where) {
        this.where = where;
    }

    public Intersect getIntersect() {
        return intersect;
    }

    public void setIntersect(Intersect intersect) {
        this.intersect = intersect;
    }

    public boolean isMcen() {
        return mcen;
    }

    public void setMcen(boolean mcen) {
        this.mcen = mcen;
    }

    public boolean isValid() {
        if ( (StringUtils.isEmpty(pos) && StringUtils.isEmpty(where)) ||
             (!StringUtils.isEmpty(refBy) && !StringUtils.isEmpty(pos)) ) {
            return false;
        }
        return true;
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
