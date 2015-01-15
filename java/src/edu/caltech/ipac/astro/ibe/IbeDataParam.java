/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.ibe;

/**
 * Date: 4/17/14
 *
 * @author loi
 * @version $Id: $
 */
public class IbeDataParam {

    // this is the relative file path after tableName
    private String filePath;

    private boolean doZip;
    // cutout related
    private boolean doCutout;
    private String center;
    private String size;

    public IbeDataParam() {
    }

    /**
     * This is the relative path to the data file.
     * This is what the full URL look like.
     * http://{ibe-host}/ibe/data/{mission}/{data-set}/{table-name}/{file-path}
     * Optionally, you can append this {file-path} to IbeDataSource#getBaseFilesystemPath() to access
     * the file locally.  Local file access may not exist for every datasource.  In the case when it
     * does not exists, it will try to retrieve it vis URL.
     * @return
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * returns the file name portion of the FilePath
     * @return
     */
    public String getFileName() {
        if (filePath != null) {
            int sidx = filePath.contains("/") ? filePath.lastIndexOf("/")+1 : 0;
            int eidx = filePath.contains("?") ? filePath.lastIndexOf("?") : filePath.length();
            return filePath.substring(sidx, eidx);
        }
        return null;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isDoZip() {
        return doZip;
    }

    public void setDoZip(boolean doZip) {
        this.doZip = doZip;
    }

    public boolean isDoCutout() {
        return doCutout;
    }

    /**
     * Examples of what can be specify for size and center.
     * size=0.1
     * size=200px
     * size=100,200px
     * size=3arcmin
     * size=30,45arcsec
     *
     * center=10,10deg
     * center=10,10
     * center=3.141,1.56rad
     * center=300.5,120px
     *
     * @param doCutout
     * @param center
     * @param size
     */
    public void setCutout(boolean doCutout, String center, String size) {
        this.doCutout = doCutout;
        this.center = center;
        this.size = size;
    }

    public void clearCutout(boolean doCutout, String center, String size) {
        this.doCutout = false;
        this.center = null;
        this.size = null;
    }

    public String getCenter() {
        return center;
    }

    public String getSize() {
        return size;
    }
}
