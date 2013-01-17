package edu.caltech.ipac.firefly.ui;

import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.data.form.DateFieldDef;
import edu.caltech.ipac.firefly.data.form.DecimalFieldDef;
import edu.caltech.ipac.firefly.data.form.DegreeFieldDef;
import edu.caltech.ipac.firefly.data.form.DoubleFieldDef;
import edu.caltech.ipac.firefly.data.form.FieldSetDef;
import edu.caltech.ipac.firefly.data.form.FileUploadFieldDef;
import edu.caltech.ipac.firefly.data.form.FloatFieldDef;
import edu.caltech.ipac.firefly.data.form.IntFieldDef;
import edu.caltech.ipac.firefly.data.form.IntListFieldDef;
import edu.caltech.ipac.firefly.data.form.LatFieldDef;
import edu.caltech.ipac.firefly.data.form.LonFieldDef;
import edu.caltech.ipac.firefly.data.form.MultiCoordFieldDef;
import edu.caltech.ipac.firefly.data.form.PositionFieldDef;
import edu.caltech.ipac.firefly.data.form.StringListFieldDef;
import edu.caltech.ipac.firefly.util.WebProp;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.action.ActionConst;
import edu.caltech.ipac.util.dd.EmailFieldDef;
import edu.caltech.ipac.util.dd.EnumFieldDef;
import edu.caltech.ipac.util.dd.FieldDef;
import edu.caltech.ipac.util.dd.FieldDefSource;
import edu.caltech.ipac.util.dd.PasswordFieldDef;
import edu.caltech.ipac.util.dd.PatternFieldDef;
import edu.caltech.ipac.util.dd.RangeFieldDef;
import edu.caltech.ipac.util.dd.StringFieldDef;
import edu.caltech.ipac.util.dd.UIAttributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Date: Nov 9, 2007
 *
 * @author loi Trey
 * @version $Id: FieldDefCreator.java,v 1.36 2012/02/25 02:56:59 tatianag Exp $
 */
public class FieldDefCreator {

    public static FieldSetDef makeFieldSetDef(String cmdName) {
        FieldSetDef fsd = new FieldSetDef(WebProp.getName(cmdName));
        fsd.setBaseProp(cmdName);
        String[] fields = WebProp.getItems(cmdName);
        for(String f : fields) {
            FieldDef fd = makeFieldDef(f);
            if (fd != null) {
                fsd.addFieldDef(fd);
            }
        }
        return fsd;
    }

    public static FieldSetDef makeFieldSetDef(String... propNames) {
        FieldSetDef fsd = new FieldSetDef();
        for(String f : propNames) {
            FieldDef fd = makeFieldDef(f);
            if (fd != null) {
                fsd.addFieldDef(fd);
            }
        }
        return fsd;
    }

    public static FieldSetDef makeFieldSetDef(FieldDefSource... fdSources) {
        FieldSetDef fsd = new FieldSetDef();
        for(FieldDefSource src : fdSources) {
            FieldDef fd = makeFieldDef(src);
            if (fd != null) {
                fsd.addFieldDef(fd);
            }
        }
        return fsd;
    }

    public static FieldDef makeFieldDef(String propName) {
        return makeFieldDef(new WebPropFieldDefSource(propName));
    }


    public static FieldDef makeFieldDef(FieldDefSource src) {
        String type = src.getDataType();
        FieldDef fd = null;
        if (type == null || type.equals(ActionConst.STRING)) {
             fd = makeStringFieldDef(src);
        } else if (type.equals(ActionConst.HIDDEN)){
             fd = makeStringFieldDef(src);
        } else if (type.equals(ActionConst.MULTI_COORD)){
             fd = makeMultiCoordFieldDef(src);
        } else if (type.equals(ActionConst.INTEGER)) {
            fd = makeIntFieldDef(src);
        } else if (type.equals(ActionConst.STRING_LIST)) {
            fd = makeStringListFieldDef(src);
        } else if (type.equals(ActionConst.INT_LIST)) {
            fd = makeIntListFieldDef(src);
        } else if (type.equals(ActionConst.DOUBLE)) {
            fd = makeDoubleFieldDef(src);
        } else if (type.equals(ActionConst.FLOAT)) {
            fd = makeFloatFieldDef(src);
        } else if (type.equals(ActionConst.ENUM_STRING)) {
            fd = makeEnumFieldDef(src);
        } else if (type.equals(ActionConst.FILE)) {
            fd = makeFileFieldDef(src);
        } else if (type.equals(ActionConst.DATE)) {
            fd = makeDateFieldDef(src);
        } else if (type.equals(ActionConst.DEGREE)) {
            fd = makeDegreeFieldDef(src);
        } else if (type.equals(ActionConst.EMAIL)) {
            fd = makeEmailFieldDef(src);
        } else if (type.equals(ActionConst.PASSWORD)) {
            fd = makePasswordFieldDef(src);
        } else if (type.equals(ActionConst.PATTERN)) {
            fd = makePatternFieldDef(src);
        } else if (type.equals(ActionConst.LON)) {  // float field that allows sexagesimal input (HMS)
            fd = makeLonFieldDef(src);
        } else if (type.equals(ActionConst.LAT)) {  // float field that allows sexagesimal input (DMS)
            fd = makeLatFieldDef(src) ;
        } else if (type.equals(ActionConst.POS_STRING)) {  // position string
            fd = makePositionStringFieldDef(src) ;
        }
        return fd;
    }

    public static StringFieldDef makeStringFieldDef(FieldDefSource src) {
        StringFieldDef fd = new StringFieldDef();
        setStringFieldAttributes(fd, src);
        return fd;
    }

    //todo:makePositionStringFieldDef
    public static PositionFieldDef makePositionStringFieldDef(FieldDefSource src) {
        PositionFieldDef fd = new PositionFieldDef();
        setStringFieldAttributes(fd, src);
        return fd;
    }
    public static EmailFieldDef makeEmailFieldDef(FieldDefSource src) {
        EmailFieldDef fd = new EmailFieldDef();
        setStringFieldAttributes(fd, src);
        return fd;
    }

    public static PasswordFieldDef makePasswordFieldDef(FieldDefSource src) {
        PasswordFieldDef fd = new PasswordFieldDef();
        setStringFieldAttributes(fd, src);
        return fd;
    }

    public static PatternFieldDef makePatternFieldDef(FieldDefSource src) {
        PatternFieldDef fd = new PatternFieldDef();
        setStringFieldAttributes(fd, src);
        fd.setPattern(src.getPattern());
        return fd;
    }

    public static DegreeFieldDef makeDegreeFieldDef(FieldDefSource src) {
         DegreeFieldDef fd = new DegreeFieldDef ();
        setStringFieldAttributes(fd, src);
        setRangedAttributes(fd, src);
        setDoubleAttributes(fd, src);

        String units = src.getUnits();

        if (units!=null) {
            units= units.trim();
            if (ActionConst.UNIT_DEGREE.equalsIgnoreCase(units)) {
                fd.setUnits(DegreeFieldDef.Units.DEGREE);
            }
            else if (ActionConst.UNIT_ARCMIN.equalsIgnoreCase(units)) {
                fd.setUnits(DegreeFieldDef.Units.ARCMIN);
            }
            else if (ActionConst.UNIT_ARCSEC.equalsIgnoreCase(units)) {
                fd.setUnits(DegreeFieldDef.Units.ARCSEC);
            }
            else {  // if we don't recognize the string
                fd.setUnits(DegreeFieldDef.Units.DEGREE);
            }
        }
        else {
            fd.setUnits(DegreeFieldDef.Units.DEGREE);
        }

        String internalUnits = src.getInternalUnits();
        if (internalUnits != null) {
            internalUnits= internalUnits.trim();
            if (ActionConst.UNIT_DEGREE.equalsIgnoreCase(internalUnits)) {
                fd.setInternalUnits(DegreeFieldDef.Units.DEGREE);
            }
            else if (ActionConst.UNIT_ARCMIN.equalsIgnoreCase(internalUnits)) {
                fd.setInternalUnits(DegreeFieldDef.Units.ARCMIN);
            }
            else if (ActionConst.UNIT_ARCSEC.equalsIgnoreCase(internalUnits)) {
                fd.setInternalUnits(DegreeFieldDef.Units.ARCSEC);
            }
            // do not set - use default if the string is not recognized
        }
        return fd;
    }

    public static IntFieldDef makeIntFieldDef(FieldDefSource src) {

        IntFieldDef fd = new IntFieldDef();
        setRangedAttributes(fd, src);

        int max= getIntVal(src.getMaxValue(), Integer.MIN_VALUE);
        if (max != Integer.MIN_VALUE) {
            fd.setMaxValue(max);
        }

        int min = getIntVal(src.getMinValue(), Integer.MAX_VALUE);
        if (min != Integer.MAX_VALUE) {
            fd.setMinValue(min);
        }
        return fd;
    }

    public static StringListFieldDef makeStringListFieldDef(FieldDefSource src) {

        StringListFieldDef fd = new StringListFieldDef();
        setRangedAttributes(fd, src);

        int max= getIntVal(src.getMaxValue(), Integer.MIN_VALUE);
        if (max != Integer.MIN_VALUE) {
            fd.setMaxValue(max);
        }

        int min = getIntVal(src.getMinValue(), Integer.MAX_VALUE);
        if (min != Integer.MAX_VALUE) {
            fd.setMinValue(min);
        }
        return fd;
    }

    public static StringListFieldDef makeIntListFieldDef(FieldDefSource src) {

        IntListFieldDef fd = new IntListFieldDef();
        setRangedAttributes(fd, src);

        int max= getIntVal(src.getMaxValue(), Integer.MIN_VALUE);
        if (max != Integer.MIN_VALUE) {
            fd.setMaxValue(max);
        }

        int min = getIntVal(src.getMinValue(), Integer.MAX_VALUE);
        if (min != Integer.MAX_VALUE) {
            fd.setMinValue(min);
        }
        return fd;
    }

    public static FloatFieldDef makeFloatFieldDef(FieldDefSource src) {
        FloatFieldDef fd = new FloatFieldDef();
        setRangedAttributes(fd, src);
        setFloatAttributes(fd, src);
        return fd;
    }

    public static MultiCoordFieldDef makeMultiCoordFieldDef(FieldDefSource src){
        MultiCoordFieldDef fd = new MultiCoordFieldDef();
        setMultiCoordFieldAttributes(fd, src);
        return fd;
    }

    public static DoubleFieldDef makeDoubleFieldDef(FieldDefSource src) {
        DoubleFieldDef fd = new DoubleFieldDef();
        setRangedAttributes(fd, src);
        setDoubleAttributes(fd, src);
        return fd;
    }

    public static LonFieldDef makeLonFieldDef(FieldDefSource src) {

        LonFieldDef fd = new LonFieldDef();
        setRangedAttributes(fd, src);

        float max = getFloatVal(src.getMaxValue(), Float.MIN_VALUE);
        if (max != Integer.MIN_VALUE) {
            fd.setMaxValue(max);
        } else {
            fd.setMaxValue(360f);
        }

        float min = getFloatVal(src.getMinValue(), Float.MAX_VALUE);
        if (min != Integer.MAX_VALUE) {
            fd.setMinValue(min);
        }else {
            fd.setMinValue(0f);
        }
        return fd;
    }

    public static LatFieldDef makeLatFieldDef(FieldDefSource src) {

        LatFieldDef fd = new LatFieldDef();
        setRangedAttributes(fd, src);

        float max = getFloatVal(src.getMaxValue(), Float.MIN_VALUE);
        if (max != Integer.MIN_VALUE) {
            fd.setMaxValue(max);
        } else {
            fd.setMaxValue(90f);
        }
        float min = getFloatVal(src.getMinValue(), Float.MAX_VALUE);
        if (min != Integer.MAX_VALUE) {
            fd.setMinValue(min);
        } else {
            fd.setMinValue(-90f);
        }
        return fd;
    }



    public static DateFieldDef makeDateFieldDef(FieldDefSource src) {
        String fmt = src.getPattern();
        String min = src.getMinValue();
        String max = src.getMaxValue();
        DateFieldDef fd = new DateFieldDef(fmt, min, max);
        setRangedAttributes(fd, src);
        return fd;
    }

    public static EnumFieldDef makeEnumFieldDef(FieldDefSource src) {
        EnumFieldDef fd = new EnumFieldDef();
        setStringFieldAttributes(fd, src);
        fd.addItems(getItemsValues(src));
        if (fd.getDefaultValueAsString()==null && fd.getEnumValues().size()>0) {
            fd.setDefaultValue(fd.getEnumValues().get(0).getName());
        }
        String orientation = src.getOrientation();
        if (orientation != null) {
            if (orientation.charAt(0) == 'V' || orientation.charAt(0) == 'v') {
                fd.setOrientation(EnumFieldDef.Orientation.Vertical);
            } else {
                fd.setOrientation(EnumFieldDef.Orientation.Horizontal);
            }
        }
        return fd;
    }

    public static FileUploadFieldDef makeFileFieldDef(FieldDefSource src) {
        FileUploadFieldDef def = new FileUploadFieldDef(src.getName(), src.getTitle(), src.getErrMsg(),
                                        src.getShortDesc(), getIntVal(src.getMaxWidth(), 0),
                                        getIntVal(src.getPreferWidth(),0), Boolean.parseBoolean(src.isNullAllow()), src.getSize());
        return def;
    }

    private static void setRangedAttributes(RangeFieldDef fd, FieldDefSource src) {
        setStringFieldAttributes(fd, src);

        fd.setMaxBoundType(src.getMaxBoundType());
        fd.setMinBoundType(src.getMinBoundType());
    }


    private static void setFloatAttributes(FloatFieldDef fd, FieldDefSource src) {
        float max = getFloatVal(src.getMaxValue(), Float.MIN_VALUE);
        if (max != Float.MIN_VALUE) {
            fd.setMaxValue(max);
        }

        float min = getFloatVal(src.getMinValue(), Float.MAX_VALUE);
        if (min != Float.MAX_VALUE) {
            fd.setMinValue(min);
        }
        setPrecisionAttributes(fd,src);
    }

    private static void setDoubleAttributes(DoubleFieldDef fd, FieldDefSource src) {
        double max = getDoubleVal(src.getMaxValue(), Double.MIN_VALUE);
        if (max != Double.MIN_VALUE) {
            fd.setMaxValue(max);
        }

        double min = getDoubleVal(src.getMinValue(), Double.MAX_VALUE);
        if (min != Double.MAX_VALUE) {
            fd.setMinValue(min);
        }
        setPrecisionAttributes(fd,src);
    }

    private static void setPrecisionAttributes(DecimalFieldDef fd, FieldDefSource src) {
        int precision = getIntVal(src.getPrecision(),0);
        if (precision > 0) fd.setPrecision(precision);
    }

    private static void setMultiCoordFieldAttributes(MultiCoordFieldDef fd, FieldDefSource src){
        setUIAttributes(fd, src);
        setStringFieldAttributes(fd, src);
        fd.setRaMaxBoundType(src.getItemMaxBoundType("ra"));
        fd.setRaMinBoundType(src.getItemMinBoundType("ra"));
        fd.setDecMaxBoundType(src.getItemMaxBoundType("dec"));
        fd.setDecMinBoundType(src.getItemMinBoundType("dec"));

        float raMax = getFloatVal(src.getItemMaxValue("ra"), Float.MIN_VALUE);
        if (raMax != Float.MIN_VALUE) {
            fd.setRaMaxValue(raMax);
        } else {
            fd.setRaMaxValue(360f);
        }

        float raMin = getFloatVal(src.getItemMinValue("ra"), Float.MAX_VALUE);
        if (raMin != Float.MAX_VALUE) {
            fd.setRaMinValue(raMin);
        }else {
            fd.setRaMinValue(0f);
        }

        float decMax = getFloatVal(src.getItemMaxValue("dec"), Float.MIN_VALUE);
        if (decMax != Float.MIN_VALUE) {
            fd.setDecMaxValue(decMax);
        } else {
            fd.setDecMaxValue(90f);
        }

        float decMin = getFloatVal(src.getItemMinValue("dec"), Float.MAX_VALUE);
        if (decMin != Float.MAX_VALUE) {
            fd.setDecMinValue(decMin);
        } else {
            fd.setDecMinValue(-90f);
        }
    }

    public static void setStringFieldAttributes(StringFieldDef fd, FieldDefSource src) {

        setUIAttributes(fd, src);
        fd.setId(src.getId());
        fd.setDefaultValue(src.getDefaultValue());
        fd.setPreferenceKey(src.getPreferenceKey());
        fd.setErrMsg(src.getErrMsg());
        fd.setMask(  getVal(src.getMask(), fd.getDefaultMask()) );
        fd.setNullAllow( getBoolVal(src.isNullAllow(), false));
        fd.setTextImmutable(getBoolVal(src.isTextImmutable(), true));

        int width= getIntVal(src.getMaxWidth(),0);
        if (width!=0) fd.setMaxWidth(width);


        int pwidth= getIntVal(src.getPreferWidth(),0);
        if (pwidth != 0) fd.setPreferWidth(pwidth);

        fd.setPreferenceRetrieve(new StringFieldDef.PreferenceRetrieve() {
            public String getPref(String key) { return Preferences.get(key); }
        });

    }

    private static void setUIAttributes(UIAttributes uia, FieldDefSource src) {
        uia.setName(src.getName());
        uia.setLabel(src.getTitle());
        uia.setDesc(src.getDesc());
        uia.setShortDesc(src.getShortDesc());
        uia.setIcon(src.getIcon());
    }

    private static List<EnumFieldDef.Item> getItemsValues(FieldDefSource src) {
        List<EnumFieldDef.Item> retvals = new ArrayList<EnumFieldDef.Item>();
        for(String item : src.getItems()) {
            String title = src.getItemTitle(item);
            //String value = src.getItemValue(item); // todo: fix? (JNS)
            int intValue = getIntVal(src.getItemIntValue(item),Integer.MAX_VALUE);
            if (intValue!=Integer.MAX_VALUE) {
                retvals.add(new EnumFieldDef.Item(item, intValue, title));
            } else {
                retvals.add(new EnumFieldDef.Item(item, title)); // todo: fix? (JNS)
            }
        }
        return retvals;
    }


    private static String getVal(String s, String def) {
        return (s!=null) ?  s : def;
    }

    private static boolean getBoolVal(String s, boolean def) {
        boolean retval= def;
        if (s!=null)  retval= Boolean.parseBoolean(s);
        return retval;
    }


    private static int getIntVal(String s, int def) {
        return StringUtils.getInt(s,def);
    }

    private static float getFloatVal(String s, float def) {
        return StringUtils.getFloat(s,def);
    }

    private static double getDoubleVal(String s, double def) {
        return StringUtils.getDouble(s,def);
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
