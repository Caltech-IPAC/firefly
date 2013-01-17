package edu.caltech.ipac.heritage.server.vo;

import edu.caltech.ipac.util.DataObject;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.text.DecimalFormat;

/**
 * @author tatianag
 *         $Id: CommonFieldMap.java,v 1.3 2010/07/01 20:21:12 tatianag Exp $
 */
public class CommonFieldMap {

    static List<MappedField> commonFields;
    static {
        commonFields = createFields();
    }

    public CommonFieldMap() {
    }

    /**
     * must have ucd="VOX:Image_Title", with datatype="char", and arraysize="*"
     * should have ucd="INST_ID", with datatype="char", and arraysize="*"
     * should have ucd="VOX:Image_MJDateObs", with datatype="double",
     * must have ucd="POS_EQ_RA_MAIN", with datatype="double"
     * must have ucd="POS_EQ_DEC_MAIN", with datatype="double"
     * must have ucd="VOX:Image_Naxes", with datatype="int"
     * must have ucd="VOX:Image_Naxis", with datatype="int", and arraysize="*"
     * must have ucd="VOX:Image_Scale", with datatype="double", and arraysize="*"
     * must have ucd="VOX:Image_Format", with datatype="char", and arraysize="*"
     * must have ucd="VOX:Image_AccessReference", with datatype="char" and arraysize="*" url in <![CDATA[...]]>
     * should have ucd="VOX:Image_FileSize", with datatype="int"
     */
    //bcdid |dceinsid | dceid | plscriptid| reqkey | campid| heritagefilename                                                                               | ptid| ptcomment                                                                                                                                                                                                                    | externalname                                                              | filesize| checksum                       | totalancilfilesize| primaryfield| channum| scet                  | instrument| aperture| fovname                                    | wavelength            | minwavelength| maxwavelength| reqmode      | ra                | dec                  | cra        | cdec       | crval1            | crval2               | ctype1     | ctype2     | crpix1| crpix2| naxis1| naxis2| cdelt1                | cdelt2               | crota2             | epoch            | equinox| ra1               | dec1                  | ra2               | dec2                 | ra3                 | dec3                  | ra4                 | dec4                 | hdu| x                          | y                       | z                      | archivefilename                                                                               |

    static List<MappedField> createFields() {
        List<MappedField> fields;
        MappedField field;

        fields = new ArrayList<MappedField>();
        fields.add(new MappedField("VOX:Image_Title", "char", "*", "externalname"));
        fields.add(new MappedField("INST_ID", "char", "*", "wavelength"));
        //"VOX:Image_MJDateObs" must be Modified Julian Date
        //fields.add(new MappedField("VOX:Image_MJDateObs", "double", null, "epoch"));
        fields.add(new MappedField("POS_EQ_RA_MAIN", "double", null, "ra", "degrees"));
        fields.add(new MappedField("POS_EQ_DEC_MAIN", "double", null, "dec", "degrees"));

        field = new MappedField("VOX:Image_Naxes", "int", null);
        field.setMapper(new VOFieldValueMapper() {
            public String getMappedValue(DataObject row) {
                return "2";
            }

            public int getMappedWidth() {
                return 8;
            }
        });
        fields.add(field);

        field = new MappedField("VOX:Image_Naxis", "int", "*");
        field.setMapper(new VOFieldValueMapper() {
            public String getMappedValue(DataObject row) {
                try {
                    String naxis1 = row.getDataElement("naxis1").toString().trim();
                    String naxis2 = row.getDataElement("naxis2").toString().trim();
                    return naxis1+" "+naxis2;
                } catch (Exception e) { return ""; }
            }

            public int getMappedWidth() {
                return 13;  
            }
        });
        fields.add(field);

        field = new MappedField("VOX:Image_Scale", "double", "*");
        field.setMapper(new VOFieldValueMapper() {
            public String getMappedValue(DataObject row) {
                try {
                    DecimalFormat df = new DecimalFormat("0.##########");
                    Double cdelt1 = Double.parseDouble(row.getDataElement("cdelt1").toString());
                    Double cdelt2 = Double.parseDouble(row.getDataElement("cdelt2").toString());
                    return df.format(cdelt1)+" "+df.format(cdelt2);
                } catch (Exception e) { return ""; }
            }

            public int getMappedWidth() {
                return 30;
            }
        });
        fields.add(field);


        field = new MappedField("VOX:Image_Format", "char", "*");
        field.setMapper(new VOFieldValueMapper() {
            public String getMappedValue(DataObject row) {
                try {
                    String name = row.getDataElement("externalname").toString().trim();
                    if (name.endsWith("fits"))
                        return "image/fits";
                    else if (name.endsWith(".tbl") || name.endsWith(".txt") || name.endsWith(".log"))
                        return "text/plain";
                    else if (name.endsWith(".gif"))
                        return "image/gif";
                    else if (name.endsWith(".jpg"))
                        return "image/jpeg";
                    else {
                        return "application/octet-stream";
                    }
                } catch (Exception e) { return ""; }
            }

            public int getMappedWidth() {
                return 25;
            }
        });
        fields.add(field);


        fields.add(new MappedField("VOX:Image_FileSize", "int", null, "filesize", "bytes"));

        return fields;
    }

    public List<MappedField> getFields() {
        return Collections.unmodifiableList(commonFields);
    }
    
}
