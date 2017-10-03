/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

/**
 * Should take care of Atlas IBE which metadata is relying on band_name, schema name, file_type and table name
 * principal is a column that serves to distinguish between main image to display and otheres, among a file_type
 */
public class AtlasImageParams extends IrsaImageParams {

    // This example is for SEIP
    private String file_type = "science"; // i.e for spitzer but bare in mind that can change from schema to another
    private String _instrument; //TODO NOT USED FOR NOW but
    private String schema = "spitzer";
    private String ds = null; //i.e. "seip" if using hard coded definition, see edu.caltech.ipac.astro.ibe.datasource.AtlasIbeDataSource.DS;
    private String table = "seip_science";
    private String xtraFilter = ""; // i.e fname like '%.mosaic.fits' or file_tye=science

    public AtlasImageParams() {
        setType(IrsaTypes.ATLAS);
    }

    public void setInstrument(String b) {
        this._instrument = b;
    }

    public String getInstrument() {
        return this._instrument;
    }

    public String getUniqueString() {
        return super.getUniqueString() + "-" + getSchema() + "-" + getTable() + "-" + getBand() + getSize();
    }

    public String toString() {
        return getUniqueString();
    }

    public void setSchema(String schem) {
        this.schema = schem;
    }

    public String getSchema() {
        return this.schema;
    }

    public String getTable() {
        return this.table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getXtraFilter() {
        return this.xtraFilter;
    }

    public void setXtraFilter(String xtraFilter) {
        this.xtraFilter = xtraFilter;
    }

    public String getDs() {
        return this.ds;
    }

    public void setDs(String ds) {
        this.ds = ds;
    }

    public String getFileType() {
        return this.file_type;
    }

    public void setFileType(String file_type) {
        this.file_type = file_type;
    }
}
