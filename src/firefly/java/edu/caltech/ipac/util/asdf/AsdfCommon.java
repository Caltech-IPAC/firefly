package edu.caltech.ipac.util.asdf;

import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil;
import edu.stsci.roman.datamodels.metadata.FitsWcs;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.fits.NullDataHDU;
import org.asdfformat.asdf.ndarray.DataTypeFamilyType;
import org.asdfformat.asdf.ndarray.NdArray;
import org.asdfformat.asdf.ndarray.Shape;
import org.asdfformat.asdf.node.AsdfNode;
import org.asdfformat.asdf.node.impl.NdArrayAsdfNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Trey Roby
 *
 */
public class AsdfCommon {



    public static File makeFITS(File infile, AsdfFileInfo asdfInfo, List<ImageNode> imageNodeList,
                                Function<String,List<AsdfAsFitsHeaderEntry>> getAdditionalHeaders
                                ) throws IOException {
        return makeFITS(infile,asdfInfo,imageNodeList,getAdditionalHeaders, null,null);
    }


    public static File makeFITS(File infile, AsdfFileInfo asdfInfo, List<ImageNode> imageNodeList,
                                Function<String,List<AsdfAsFitsHeaderEntry>> getAdditionalHeaders,
                                AxisInfo baseAxisInfo, FitsWcs fitsWcs) throws IOException {
        String fBase= FileUtil.getBase(infile);
        File retFile= new File(infile.getParent()+"/"+ fBase+"---from-asdf"+".fits");
        Fits fits= new Fits();
        fits.addHDU(new NullDataHDU());
        for(var imageItem: imageNodeList) {
            NdArrayAsdfNode arrayNode= imageItem.ndNode();
            var axisInfo= getAxisInfo(arrayNode.asNdArray());
            FitsWcs wcsToUse= imagesMatch(baseAxisInfo,axisInfo) ? fitsWcs : null;
            var inHeaders= makeFitsLikeHeadersUsingRomanImageModel(wcsToUse, arrayNode, asdfInfo,imageItem.extName(), false);

            Object ary= getImageAry(axisInfo.naxis(),arrayNode);
            var hdu= Fits.makeHDU(ary);
            Header header= hdu.getHeader();
            for(var h : inHeaders) {
                header.addValue(h.key(),h.value(), "derived from roman asdf");
            }


            if (getAdditionalHeaders!=null) {
                var hdrs= Util.Try.it(() -> getAdditionalHeaders.apply(imageItem.extName())).getOrElse(Collections.emptyList());
                for(var h : hdrs) header.addValue(h.key(),h.value(), h.comment());
            }
            fits.addHDU(hdu);
        }
        fits.write(retFile);
        FitsReadUtil.closeFits(fits);
        return retFile;
    }




    static DataGroup getDetails(FitsWcs fitsWcs, NdArrayAsdfNode arrayNode, AsdfFileInfo fileInfo, String extName,
                                List<AsdfAsFitsHeaderEntry> additionalHeaders) {
        DataType[] cols = new DataType[]{
                new DataType("#", Integer.class),
                new DataType("key", String.class),
                new DataType("value", String.class),
                new DataType("comment", String.class)
        };
        DataGroup dg = new DataGroup("Header of extension with index " + 0, cols);

        var asdfHeaders = makeHeadersUsingRomanImageModel(fitsWcs, arrayNode, fileInfo, extName);
        for (var h : asdfHeaders) {
            DataObject row = new DataObject(dg);
            row.setDataElement(cols[0], dg.size());
            row.setDataElement(cols[1], h.key());
            row.setDataElement(cols[2], h.value());
            row.setDataElement(cols[3], "");
            dg.add(row);
        }
        var headers = makeFitsLikeHeadersUsingRomanImageModel(fitsWcs, arrayNode, fileInfo, extName, true);
        for (var h : headers) {
            DataObject row = new DataObject(dg);
            row.setDataElement(cols[0], dg.size());
            row.setDataElement(cols[1], h.keyF());
            row.setDataElement(cols[2], h.value());
            row.setDataElement(cols[3], h.comment());
            dg.add(row);
        }
        if (additionalHeaders!=null) {
            for (var h : additionalHeaders) {
                DataObject row = new DataObject(dg);
                row.setDataElement(cols[0], dg.size());
                row.setDataElement(cols[1], h.keyF());
                row.setDataElement(cols[2], h.value());
                row.setDataElement(cols[3], h.comment());
                dg.add(row);
            }
        }
        return dg;
    }

    private static List<AsdfHeaderEntry> makeHeadersUsingRomanImageModel(FitsWcs fitsWcs, NdArrayAsdfNode arrayNode,
                                                                         AsdfFileInfo fileInfo, String extName) {
        List<AsdfHeaderEntry> list = new ArrayList<>();
        AxisInfo axis = getAxisInfo(arrayNode.asNdArray());
        list.add(new AsdfHeaderEntry("name", extName));
        list.add(new AsdfHeaderEntry("data type", arrayNode.asNdArray().getDataType().getFamily().name()));
        list.add(new AsdfHeaderEntry("telescope", fileInfo.telescope()));
        list.add(new AsdfHeaderEntry("instrument", fileInfo.instrument()));
        list.add(new AsdfHeaderEntry("axis", axisDesc(axis)));
        if (fitsWcs == null) return list;

        // if we know projection information, add mre
        list.add(new AsdfHeaderEntry("projection", fitsWcs.getProjection().name()));
        var cdeltAry = fitsWcs.getCdelt().asDoubleNdArray();
        list.add(new AsdfHeaderEntry("cdelt", String.format("[%f,%f]", cdeltAry.get(0), cdeltAry.get(1))));
        var crValAry = fitsWcs.getCrval().asDoubleNdArray();
        list.add(new AsdfHeaderEntry("crVal", String.format("[%f,%f]", crValAry.get(0), crValAry.get(1))));
        var crPixAry = fitsWcs.getCrpix().asDoubleNdArray();
        list.add(new AsdfHeaderEntry("crPix", String.format("[%f,%f]", crPixAry.get(0), crPixAry.get(1))));
        int pc1Max = fitsWcs.getPc().getShape().get(0);
        int pc2Max = fitsWcs.getPc().getShape().get(0);
        list.add(new AsdfHeaderEntry("pc", String.format("%dx%d array", pc1Max, pc2Max)));
        return list;
    }

    static List<AsdfAsFitsHeaderEntry> makeFitsLikeHeadersUsingRomanImageModel(FitsWcs fitsWcs,
                                                                               NdArrayAsdfNode arrayNode,
                                                                               AsdfFileInfo fileInfo,
                                                                               String extName,
                                                                               boolean includeImageTypeInfo) {
        List<AsdfAsFitsHeaderEntry> list = new ArrayList<>();
        AxisInfo axis = getAxisInfo(arrayNode.asNdArray());
        //todo DATE

        if (includeImageTypeInfo) list.add(new AsdfAsFitsHeaderEntry("XTENSION", "IMAGE", ""));
        list.add(new AsdfAsFitsHeaderEntry("EXTNAME", extName, ""));
        if (includeImageTypeInfo)
            list.add(new AsdfAsFitsHeaderEntry("BITPIX", dataTypeToFits(arrayNode), ""));
        list.add(new AsdfAsFitsHeaderEntry("TELESCOP", fileInfo.telescope(), ""));
        list.add(new AsdfAsFitsHeaderEntry("INSTRUME", fileInfo.instrument(), ""));
        list.add(new AsdfAsFitsHeaderEntry("ORIGIN", fileInfo.origin(), ""));
        if (includeImageTypeInfo) {
            list.add(new AsdfAsFitsHeaderEntry("NAXIS", axis.naxis() + "", ""));
            list.add(new AsdfAsFitsHeaderEntry("NAXIS1", axis.width() + "", ""));
            list.add(new AsdfAsFitsHeaderEntry("NAXIS2", axis.height() + "", ""));
            if (axis.naxis() == 3) list.add(new AsdfAsFitsHeaderEntry("NAXIS3", axis.depth() + "", ""));
        }
        if (fitsWcs != null) {
            var proj = "-" + fitsWcs.getProjection().name();
            list.add(new AsdfAsFitsHeaderEntry("CTYPE1", "RA--" + proj, ""));
            list.add(new AsdfAsFitsHeaderEntry("CTYPE2", "DEC-" + proj, ""));
            list.add(new AsdfAsFitsHeaderEntry("RADESYS", "ICRS", ""));  //todo this should be in fitsWcs but it is not
            list.add(new AsdfAsFitsHeaderEntry("CDELT1", fitsWcs.getCdelt().asDoubleNdArray().get(0) + "", ""));
            list.add(new AsdfAsFitsHeaderEntry("CDELT2", fitsWcs.getCdelt().asDoubleNdArray().get(1) + "", ""));
            list.add(new AsdfAsFitsHeaderEntry("CRVAL1", fitsWcs.getCrval().asDoubleNdArray().get(0) + "", ""));
            list.add(new AsdfAsFitsHeaderEntry("CRVAL2", fitsWcs.getCrval().asDoubleNdArray().get(1) + "", ""));
            list.add(new AsdfAsFitsHeaderEntry("CRPIX1", fitsWcs.getCrpix().asDoubleNdArray().get(0) + "", ""));
            list.add(new AsdfAsFitsHeaderEntry("CRPIX2", fitsWcs.getCrpix().asDoubleNdArray().get(1) + "", ""));
            int pc1Max = fitsWcs.getPc().getShape().get(0);
            int pc2Max = fitsWcs.getPc().getShape().get(0);
            for (int i = 1; (i <= pc1Max); i++) {
                for (int j = 1; (j <= pc2Max); j++) {
                    list.add(new AsdfAsFitsHeaderEntry("PC" + i + "_" + j, fitsWcs.getPc().get(i - 1, j - 1) + "", ""));
                }
            }
        }
        return list;
    }



    public static final String ROMAN= "roman";


    public static AxisInfo getAxisInfo(NdArray imageModel) {
        Shape shape= imageModel.getShape();
        return shape.getRank()==3
                ? new AsdfCommon.AxisInfo(3, shape.get(2), shape.get(1), shape.get(0))
                : new AsdfCommon.AxisInfo(2, shape.get(1), shape.get(0), 0);
    }


    public static String axisDesc(AxisInfo axis) {
        if (axis.naxis()>=3 && axis.depth()>1) {
            return String.format(" (cube %d x %d x %d)",axis.width(),axis.height(),axis.depth());
        }
        else {
            return String.format(" (%d x %d)",axis.width(),axis.height());
        }
    }

    public static String dataTypeToFits(NdArrayAsdfNode data) {
        var dt= data.asNdArray().getDataType().getFamily().name();
        return switch (dt){
            case "FLOAT" -> "-32";
            case "DOUBLE" -> "-64";
            case "INT", "UINT" -> "32";
            default -> "8";
        };
    }



    public static AsdfCommon.TableDetails makeTableDetails(String tableName, List<ColumnInfo> columnInfoList, int tableLength) {
        DataType[] cols = new DataType[]{
                new DataType("#", Integer.class),
                new DataType("key", String.class),
                new DataType("value", String.class),
                new DataType("comment", String.class)
        };
        DataGroup dg = new DataGroup("table Details " + 0, cols);

        for (var columnInfo : columnInfoList) {
            DataObject row = new DataObject(dg);
            row.setDataElement(cols[0], dg.size());
            row.setDataElement(cols[1], columnInfo.name());
            try {
                row.setDataElement(cols[2], columnInfo.dType());
            } catch (Exception e) {
                row.setDataElement(cols[2], "other");
            }
            row.setDataElement(cols[3], "");
            dg.add(row);
        }
        return new AsdfCommon.TableDetails(dg, tableLength, columnInfoList.size());
    }



    public static Class<?> convertAsdfDataTypeToClass(DataTypeFamilyType asdfDataType) {
        return switch (asdfDataType) {
            case BOOL -> Boolean.class;
            case FLOAT, COMPLEX -> Double.class;
            case TUPLE, ASCII, UCS4 -> String.class;
            case INT, UINT -> Long.class;
        };
    }

    public static Object getAryNodeValue(NdArray<?> aryNode, int index) {
        return switch (aryNode.getDataType().getFamily()) {
            case ASCII, UCS4  -> Util.Try.it(() -> aryNode.asStringNdArray().get(index)).getOrElse("");
            case BOOL -> Util.Try.it(() -> aryNode.asBooleanNdArray().get(index)).getOrElse(false); // todo - not sure what the failure response should be
            case FLOAT, COMPLEX  -> Util.Try.it(() -> aryNode.asDoubleNdArray().get(index)).getOrElse(Double.NaN);
            case INT, UINT  ->
                    Util.Try
                            .it(() -> Long.parseLong(aryNode.asBigIntegerNdArray().get(index).toString()))
                            .getOrElse(-1L); // todo - not sure what the failure response should be
            case TUPLE -> getTupleValue(aryNode, index);
        };
    }


    private static String getTupleValue(NdArray<?> aryNode, int index) {
        try {
            //todo-  figure out how to support tuple. I think it would make like converting it to json and making a string
            //       However, I need am example
            var tupNode= aryNode.asTupleNdArray();
            return "tuple not supported";
        } catch (Exception e) {
            return "tuple not supported";
        }
    }





    public static boolean imagesMatch(AxisInfo baseAxisInfo, AxisInfo axisInfo) {
        if (baseAxisInfo==null || axisInfo==null) return false;
        if (baseAxisInfo.width()==axisInfo.width() && baseAxisInfo.height()==axisInfo.height()) {
            var d1= baseAxisInfo.depth();
            var d2= axisInfo.depth();
            if (d1==d2) return true;
            if (d1==1 && d2==0) return true;
            return d1 == 0 && d2 == 1;
        }
        return false;
    }

    public static Object getImageAry(int dim, NdArrayAsdfNode arrayNode) {
        var ndAry= arrayNode.asNdArray();
        var dt= ndAry.getDataType().getFamily();
        return switch (dt){
            case FLOAT -> getArrayForFloat(ndAry, dim);
            case COMPLEX -> dim==3 ? ndAry.asDoubleNdArray().toArray(new double[0][0][0]) : ndAry.asDoubleNdArray().toArray(new double[0][0]);
            case INT, UINT -> dim==3 ? ndAry.asIntNdArray().toArray(new int[0][0][0]) : ndAry.asIntNdArray().toArray(new int[0][0]);
            case BOOL -> getArrayForBoolean(ndAry, dim);
            default -> arrayNode.asNdArray().toRawArray();
        };
    }

    public static Object getArrayForFloat(NdArray<?> ndAry, int dim) {
        try {
            return dim==3 ?ndAry.asFloatNdArray().toArray(new float[0][0][0]) : ndAry.asFloatNdArray().toArray(new float[0][0]);
        } catch (Exception e) {
            return dim==3 ?ndAry.asDoubleNdArray().toArray(new double[0][0][0]) : ndAry.asDoubleNdArray().toArray(new double[0][0]);
        }
    }

    public static Object getArrayForBoolean(NdArray<?> ndAry, int dim) {
        Shape shape= ndAry.getShape();
        if (dim==3) {
            boolean [][][] bAry= ndAry.asBooleanNdArray().toArray(new boolean[0][0][]);
            int [][][] iAry= new int[shape.get(0)][shape.get(1)][shape.get(2)];
            for(int i=0;i<iAry.length;i++) {
                for(int j=0;j<iAry[i].length;j++) {
                    for(int k=0;j<iAry[i][j].length;k++) {
                        iAry[i][j][k] = bAry[i][j][k] ? 1 : 0;
                    }
                }
            }
            return iAry;
        }
        else {
            boolean [][] bAry= ndAry.asBooleanNdArray().toArray(new boolean[0][0]);
            int [][] iAry= new int[shape.get(0)][shape.get(1)];
            for(int i=0;i<iAry.length;i++) {
                for(int j=0;j<iAry[i].length;j++) {
                    iAry[i][j]= bAry[i][j] ? 1 : 0;
                }
            }
            return iAry;
        }
    }

    public static boolean isNdAry(AsdfNode node, int dims) {
        if (node instanceof NdArrayAsdfNode) {
            return node.asNdArray().getShape().getRank()==dims;
        }
        return false;
    }

    public record ColumnInfo(String name, String dType) {}
    public record ImageNode(String extName, NdArrayAsdfNode ndNode) {}
    public record TableDetails(DataGroup detailDG, int row, int col) {}
    public record ImageDetails(String extName, DataGroup dg, AxisInfo axisInfo, String desc) {}
    public record AsdfHeaderEntry(String key, String value) {}

    public record AsdfAsFitsHeaderEntry(String key, String value, String comment) {
        String keyF() { return "f::"+key; }
    }

    public record AxisInfo(int naxis, long width, long height, long depth) {}

    public record AsdfFileInfo(String telescope, String title, String instrument,
                               String origin, String mainTreeNode,
                               List<String> imageNameList, AsdfNode tablesRootNode,
                               Map<String,List<String>> tables, List<String> tableNames) {}

    public record DataGroupInfo(DataGroup dataGroup, int tableLength, List<String> colNames, int startingTableIdx) {}



}

