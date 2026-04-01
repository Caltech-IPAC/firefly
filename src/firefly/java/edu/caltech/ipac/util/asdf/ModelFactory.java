package edu.caltech.ipac.util.asdf;

import org.asdfformat.asdf.AsdfFile;
import org.asdfformat.asdf.node.AsdfNode;

import static edu.caltech.ipac.util.asdf.AsdfCommon.ROMAN;

public class ModelFactory {

    private static String romanPipelineStart= "stsci.edu/datamodels/roman/tags";
    private static String pydanticStart= "asdf-pydantic";

    private static String rSegm= "stsci.edu/datamodels/roman/tags/mosaic_segmentation_map";
    private static String rWfiCoadd= "stsci.edu/datamodels/roman/tags/wfi_mosaic";
    private static String rWfiUncal= "stsci.edu/datamodels/roman/tags/wfi_science_raw";
    private static String rWfiCal= "stsci.edu/datamodels/roman/tags/wfi_image";
    private static String rWfiWcs= "stsci.edu/datamodels/roman/tags/wfi_wcs";
    private static String p2Spec1d= "asdf-pydantic/examples/tags/g2dp-1d-spectra";
    private static String p2Spec2d= "asdf-pydantic/examples/tags/g2dp-2d-spectra";
    private static String p2SpecLoc= "asdf-pydantic/examples/tags/g2dp-locationtable";
    private static String p2dp= "asdf-pydantic/examples/tags/g2dp-meta";


    public static DataAccessModel get(AsdfFile asdfFile) {

        String ROMAN= "roman";
        AsdfNode mainTag = asdfFile.getTree().get(ROMAN);
        if (mainTag==null) return null;
        var tagStr= mainTag.getTag();
        if (tagStr.contains(romanPipelineStart)) {
            return new RomanPipelineModel();
        }
        else if (tagStr.contains(pydanticStart)) {
            if (tagStr.contains(p2Spec1d)) return new Pydantic1dModel();
            else if (tagStr.contains(p2Spec2d)) return new Pydantic2dModel();
            return null;
        }
        return null;
    }

    public static String getError(AsdfFile asdfFile) {
        if (get(asdfFile) != null) return "";
        AsdfNode mainTag = asdfFile.getTree().get(ROMAN);
        if (mainTag!=null) {
            var tagStr= mainTag.getTag();
            return "ASDF roman model: " + tagStr + " is not yet supported";
        }
        else {
            return "Unrecognized version of ASDF file";
        }
    }
    

}
