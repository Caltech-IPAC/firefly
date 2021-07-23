/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.net;

import edu.caltech.ipac.util.Assert;

/**
 * Should take care of Atlas IBE which metadata is relying on band_name, schema name, file_type and table name
 * principal is a column that serves to distinguish between main image to display and otheres, among a file_type
 */
public class AtlasImageParams extends ImageServiceParams {

    // This example is for SEIP
    private String file_type = "science"; // i.e for spitzer but bare in mind that can change from schema to another
    private String _instrument; //TODO NOT USED FOR NOW but
    private String schema = "spitzer";
    private String ds = null; //i.e. "seip" if using hard coded definition, see edu.caltech.ipac.astro.ibe.datasource.AtlasIbeDataSource.DS;
    private String table = "seip_science";
    private String xtraFilter = ""; // i.e fname like '%.mosaic.fits' or file_tye=science
    private String data_type = "image";
    private String _band= "12";
    private float  _size= 5.0F;

    public AtlasImageParams(String service, String table, String statusKey, String plotId ){
        super(ImageSourceTypes.ATLAS, statusKey, plotId);
        this.schema = service;
        this.table = table;
    }

    public String getUniqueStringStart() {
        String retval= null;
        switch (getType()) {
            case ISSA :
                retval= "Issa-" + super.toString() + _band + _size;
                break;
            case TWOMASS :
                retval= "2mass-" + super.toString() + _band + _size;
                break;
            case MSX :
                retval= "msx-" + super.toString() + _band + _size;
                break;
            case IRIS :
                retval= "iris-" + super.toString() + _band + _size;
                break;
            case ATLAS:
                retval = "atlas-"+ super.toString();
                break;
            default :
                Assert.tst(false); break;
        }
        return retval;
    }

    public void   setBand(String b)     { _band= b; }
    public void   setSize(float s)      { _size= s; }
    public String getBand()             { return _band; }
    public float  getSize()             { return _size; }


    public void setInstrument(String b) {
        this._instrument = b;
    }

    public String getInstrument() {
        return this._instrument;
    }

    public String getUniqueString() {
        return getUniqueStringStart() + "-" + getSchema() + "-" + getTable() + "-" + getBand() + getSize()+ (getXtraFilter()!=null?getXtraFilter().hashCode():"");
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

    public String getDataType() {
        return this.data_type;
    }

    public void setDataType(String dataType) {
        this.data_type = dataType;
    }
}
