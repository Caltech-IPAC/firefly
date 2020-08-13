package edu.caltech.ipac.firefly.data.sofia;

import edu.caltech.ipac.table.DataType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Define SOFIA spectra model in terms of VO fields
 * Default fields for this model are Flux and Wavelength
 */
public class SofiaSpectraModel implements VOSpectraModel {

    public static final SPECTRA_FIELDS[] defaultSofiaSpectraCols =
            new VOSpectraModel.SPECTRA_FIELDS[]{SPECTRA_FIELDS.WAVELENGTH, SPECTRA_FIELDS.FLUX, SPECTRA_FIELDS.ERROR_FLUX, VOSpectraModel.SPECTRA_FIELDS.ATMOS_TRANSMISSION, SPECTRA_FIELDS.INST_RESP_CURVE,
                    SPECTRA_FIELDS.FREQUENCY, SPECTRA_FIELDS.VELOCITY}; // make an enum SPECTRA_COLS("Flux",unit,ucd,type)..?;

    private final Map<String, DataType> voCols;
    private SPECTRA_FIELDS[] spectraCols;

    public SofiaSpectraModel() {
        this(defaultSofiaSpectraCols);
    }

    /**
     * Define spectra model with specific columns
     * Constructor with particular data type fields
     *
     * @param fields
     */
    public SofiaSpectraModel(VOSpectraModel.SPECTRA_FIELDS[] fields) {
        spectraCols = fields;

        voCols = new LinkedHashMap<>(); //Keep order of the column default set above, important to extract the data rom images row as SOFIA model defined.
        for (SPECTRA_FIELDS param : spectraCols) {
            DataType dt = new DataType(param.getKey(), param.getTitle(), param.getMetaClass());
            dt.setDesc(param.getDescription());
            dt.setUnits(param.getUnits());
            dt.setUCD(param.getUcd());
            dt.setUType(param.getUtype());
//            dt.setWidth();
//            dt.setPrecision();
            voCols.put(param.getKey(), dt);
        }
    }
    /**
     * Define spectra model from instrument enum
     * Constructor with particular data type fields
     *
     * @param inst {@link SpectraInstrument}
     */
    public SofiaSpectraModel(SpectraInstrument inst) {
        this(inst.getSpectraCols());
    }

    @Override
    public Map<String, DataType> getMeta() {
        return voCols;
    }

    @Override
    public void setUnits(SPECTRA_FIELDS field, String units) {
        voCols.get(field.getKey()).setUnits(units);
    }

    @Override
    public void setUcd(SPECTRA_FIELDS field, String ucd) {
        voCols.get(field.getKey()).setUCD(ucd);
    }
    /*
    VOTable Example from NED:
    <VOTABLE version="v1.0">
    <RESOURCE utype="sed:SED">
        <TABLE name="./DATA/NGC_3034:S:HI:r1980_votable.xml" utype="sed:Segment">
        <GROUP utype="sed:Segment.SpectralCoord">
            <FIELDref ref="Frequency"/>
        </GROUP>
        <GROUP utype="sed:Segment.Flux">
            <FIELDref ref="Flux"/>
        </GROUP>
        <FIELD ID="Frequency" unit="Hz" datatype="double" name="Frequency" utype="sed:Segment.Points.SpectralCoord.Value"/>
        <FIELD ID="Flux" unit="W/m^2/Hz" datatype="double" name="Flux" utype="sed:Segment.Points.Flux.Value"/>
     */

    public enum INSTRUMENTS {

        FORCAST("FORCAST",
                new HashMap<String, String>() {{
                    put("IMAGING", "IMAGING");
                }},
                new HashMap<String, String>() {{
                    put("SW", "SW");
                    put("LW", "LW");
                }},
                new VOSpectraModel.SPECTRA_FIELDS[]{SPECTRA_FIELDS.WAVELENGTH, SPECTRA_FIELDS.FLUX, SPECTRA_FIELDS.ERROR_FLUX, VOSpectraModel.SPECTRA_FIELDS.ATMOS_TRANSMISSION, SPECTRA_FIELDS.INST_RESP_CURVE}
        ),
        FIFILS("FIFI-LS"),
        HAWC("HAWC_PLUS"),
        EXES("EXES",null, null, new VOSpectraModel.SPECTRA_FIELDS[]{SPECTRA_FIELDS.WAVENUMBER, SPECTRA_FIELDS.FLUX, SPECTRA_FIELDS.ERROR_FLUX, VOSpectraModel.SPECTRA_FIELDS.ATMOS_TRANSMISSION}),
        FLITECAM("FLITECAM",null,null,new VOSpectraModel.SPECTRA_FIELDS[]{SPECTRA_FIELDS.WAVELENGTH, SPECTRA_FIELDS.FLUX, SPECTRA_FIELDS.ERROR_FLUX}),
        FPI("FPI_PLUS"),
        GREAT("GREAT", null, null,new  VOSpectraModel.SPECTRA_FIELDS[]{SPECTRA_FIELDS.FREQUENCY, SPECTRA_FIELDS.VELOCITY, SPECTRA_FIELDS.FLUX});

        private final SPECTRA_FIELDS[] spectraFields;
        private String name;
        private Map<String, String> config;
        private Map<String, String> camera;

        INSTRUMENTS(String name) {
            this(name, null, null,null);
        }

        INSTRUMENTS(String name, Map<String, String> config) {
            this(name, config, null,null);
        }

        INSTRUMENTS(String name, Map<String, String> config, Map<String, String> camera, VOSpectraModel.SPECTRA_FIELDS[] cols) {
            this.name = name;
            this.config = config;
            this.camera = camera;
            this.spectraFields = cols;
        }

        public String getName() {
            return this.name;
        }

        public Map<String, String> getConfig() {
            return this.config;
        }

        public Map<String, String> getCamera() {
            return this.camera;
        }

        public SPECTRA_FIELDS[] getSpectraFields() {
            return spectraFields;
        }
    }

    /*
       SOFIA product where spectra 1d (flux vs wavelength tipially is coded in an image with at least 2 rows, first wavlenegtth and 2 flux.
        */
    public enum SpectraInstrument {

        FORCAST(SofiaSpectraModel.INSTRUMENTS.FORCAST, SPECTRA_FIELDS.WAVELENGTH, SPECTRA_FIELDS.FLUX),
        EXES(SofiaSpectraModel.INSTRUMENTS.EXES, SPECTRA_FIELDS.WAVENUMBER, SPECTRA_FIELDS.FLUX),
        FLITECAM(SofiaSpectraModel.INSTRUMENTS.FLITECAM, SPECTRA_FIELDS.WAVELENGTH, SPECTRA_FIELDS.FLUX),
        GREAT(SofiaSpectraModel.INSTRUMENTS.GREAT, SPECTRA_FIELDS.FREQUENCY, SPECTRA_FIELDS.FLUX);



        //GREAT, FIFILS... more complicated.

        private final SPECTRA_FIELDS idxXaxis, idxYaxis;
        private final SofiaSpectraModel.INSTRUMENTS instrument;

        SpectraInstrument(SofiaSpectraModel.INSTRUMENTS inst, SPECTRA_FIELDS x, SPECTRA_FIELDS y) {
            this.idxXaxis = x;
            this.idxYaxis = y;
            this.instrument = inst;
        }

        public static SpectraInstrument getInstrument(String inst) {
            return valueOf(inst.toUpperCase());
        }

        public SPECTRA_FIELDS[] getSpectraCols() {
            return this.instrument.getSpectraFields();
        }

        public SPECTRA_FIELDS getXaxis() {
            return idxXaxis;
        }

        public SPECTRA_FIELDS getYaxis() {
            return idxYaxis;
        }
    }
}
