package edu.caltech.ipac.firefly.data.sofia;

import edu.caltech.ipac.table.DataType;

import java.util.Map;

/**
 * A way to define spectral model to map data into VO model vocabularies
 * http://cdsweb.u-strasbg.fr/UCD/ucd1p-words.txt
 * from http://www.ivoa.net/documents/UCD1+/20180527/index.html
 * tree in browser http://cdsweb.u-strasbg.fr/UCD/tree/js/
 *
 * IVOA docs: http://www.ivoa.net/documents/index.html
 */
public interface VOSpectraModel {


    // TODO
    //  ADD HEADER to PARAMS ??
    /*
    <GROUP ID="FITS-Headers" name="FITS-Headers" ucd="meta.fits">?
    or
    <TABLE name="sofia-spectra">
        <PARAM datatype="float" name="WAVELENGTH" unit=".." value="">
        <DESCRIPTION>The spectral range</DESCRIPTION>
            <VALUES>
                <min>
                <max>
            </values>
        </PARAM>
        <PARAM datatype="float" name="ANY FROM HEADER CARD KEY!" unit=".." value="CARD_HEADER.value()">
     */
    //TODO add to this enum to cover more spectra content attributes
    public enum SPECTRA_FIELDS {
        //Y AXIS
        FLUX("flux", "Flux", Double.class,"phot.flux.density;em.MIR"), // unit should be replace by extractor
        //X AXIS
        WAVELENGTH("wavelength", "Wavelength", Double.class,"em.wavenumber;em.MIR"),
//        FREQUENCY("wavelength", "Wavelength", Double.class),
        //ERRORs
        ERROR_FLUX("error", "Error", Double.class,"stat.error;phot.flux.density;em.MIR"),
        ATMOS_TRANSMISSION("transmission", "Transmission", Double.class,"phys.transmission;em.MIR"),
        INST_RESP_CURVE("response", "Response", Double.class,"instr.det;em.MIR") ;
        // ORDERS...
        String key;
        String label;
        Class metaClass;
        String description, units, utype, ucd; // ref?, ID? group (see vizier tables)

        SPECTRA_FIELDS(String key, String label, Class c) {
            this(key, label, c, null, null, null, null);
        }
        SPECTRA_FIELDS(String key, String label, Class c, String ucd) {
            this(key, label, c, null, null, ucd, null);
        }
        SPECTRA_FIELDS(String key, String label, Class c, String des, String units, String ucd, String utype) {
            this.key = key;
            this.label = label;
            this.metaClass = c;
            this.description = des;
            this.units = units;
            this.ucd = ucd;
            this.utype = utype;

        }

       public String getKey() {
            return this.key;
        }

        String getTitle() {
            return this.label;
        }

        Class getMetaClass() {
            return this.metaClass;
        }

        String getDescription() {
            return this.description;
        }

        String getUnits() {
            return this.units;
        }

        String getUtype() {
            return this.utype;
        }

        String getUcd() {
            return this.ucd;
        }
    }

    Map<String, DataType> getMeta(); //VOTAble <FIELD>

    /**
     * Sets units
     *
     * @param field
     * @param units
     */
    void setUnits(SPECTRA_FIELDS field, String units);

    /**
     * Sets the ucd
     *
     * @param field
     * @param ucd
     */
    void setUcd(SPECTRA_FIELDS field, String ucd);

}
