package edu.caltech.ipac.util;


import edu.caltech.ipac.firefly.core.FileAnalysisReport;
import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.io.TableParseHandler;
import edu.stsci.roman.datamodels.RomanDatamodels;
import edu.stsci.roman.datamodels.metadata.FitsWcs;
import edu.stsci.roman.datamodels.model.RomanImageModel;
import edu.stsci.roman.datamodels.model.RomanModel;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.NullDataHDU;
import org.asdfformat.asdf.Asdf;
import org.asdfformat.asdf.AsdfFile;
import org.asdfformat.asdf.ndarray.DataTypeFamilyType;
import org.asdfformat.asdf.ndarray.NdArray;
import org.asdfformat.asdf.ndarray.Shape;
import org.asdfformat.asdf.node.AsdfNode;
import org.asdfformat.asdf.node.impl.NdArrayAsdfNode;
import org.asdfformat.asdf.node.impl.StringAsdfNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.core.FileAnalysisReport.Type.Image;
import static edu.caltech.ipac.firefly.core.FileAnalysisReport.Type.Table;
import static edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil.closeFits;

/**
 * @author Trey Roby
 *
 */
public class ASDFUtil {


    private static final String ROMAN= "roman";

    public static FileAnalysisReport analyze(File infile, FileAnalysisReport.ReportType type) throws Exception {
        FileAnalysisReport report = new FileAnalysisReport(type, FormatUtil.Format.ASDF.name(), infile.length(), infile.getPath());
        Path path= infile.toPath();
        try (AsdfFile asdfFile = Asdf.open(path)) {
            AsdfFileInfo asdfInfo= getAsdfFileInfo(asdfFile);
            if (asdfInfo == null || !asdfInfo.mainTreeNode.equals(ROMAN)) return null;
            AsdfNode roman = asdfFile.getTree().get(asdfInfo.mainTreeNode);
            try (final RomanModel<?> model = RomanDatamodels.open(path)) {
                if (model instanceof RomanImageModel<?> imageModel) {
                    FitsWcs fitsWcs= Util.Try.it(() -> imageModel.getMeta().getFitsWcs()).get();
                    buildReport(report,roman, asdfInfo,fitsWcs);
                }
                else {
                    buildReport(report,roman,asdfInfo,null);
                }
            }
            catch (Exception e) {
                buildReport(report,roman,asdfInfo,null);
            }
        }
        return report;
    }

    private static void buildReport(FileAnalysisReport report, AsdfNode rootDataNode, AsdfFileInfo asdfInfo, FitsWcs fitsWcs) {
        int i=0;
        for(String extName : asdfInfo.imageNameList) {
            var arrayNode= (NdArrayAsdfNode)rootDataNode.get(extName);
            FileAnalysisReport.Part part = new FileAnalysisReport.Part(Image, "asdf image");
            part.setIndex(i++);
            part.setDesc(extName+axisDesc(getAxisInfo(arrayNode.asNdArray())));
            part.setDetails(getDetails(fitsWcs,arrayNode, asdfInfo,extName));
            report.addPart(part);
        }
        addTables(asdfInfo, report,i);
    }


    public static void convertAsdfAstroPyTableToDataGroup(TableParseHandler handler, File file, int tableIdx) throws IOException {
        Path path= file.toPath();
        try (AsdfFile asdfFile = Asdf.open(path)) {
            AsdfFileInfo asdfInfo= getAsdfFileInfo(asdfFile);

            DataGroup dg = createDataGroup(asdfInfo, tableIdx);
            int startingTableIdx= asdfInfo.imageNameList.size();
            String tName= asdfInfo.tableNames().get(tableIdx-startingTableIdx);
            List<String> colNames= asdfInfo.tables.get(tName);
            int tableLength= getColDataNode(asdfInfo.tablesRootNode(), tName, 0).getShape().get(0);
            handler.start();
            handler.startTable(0);
            handler.header(dg);

            for(int rowIdx=0;(rowIdx<tableLength);rowIdx++) {
                Object[] row= new Object[colNames.size()];
                for(int i=0;i<colNames.size();i++) {
                    NdArray<?> aryNode= getColDataNode(asdfInfo.tablesRootNode(), tName, i);
                    row[i]= getAryNodeValue(aryNode,rowIdx);
                }
                handler.data(row);
            }
        } catch (Exception e) {
            throw new IOException(e.toString(),e);
        } finally {
            handler.endTable(0);
            handler.end();
        }
    }



    public static DataGroup convertAsdfAstroPyTableToDataGroup(File file, TableServerRequest request, int tableIdx) throws IOException {
        Path path= file.toPath();
        try (AsdfFile asdfFile = Asdf.open(path)) {
            AsdfFileInfo asdfInfo= getAsdfFileInfo(asdfFile);
            DataGroup dg = createDataGroup(asdfInfo, tableIdx);
            int startingTableIdx= asdfInfo.imageNameList.size();
            String tName= asdfInfo.tableNames().get(tableIdx-startingTableIdx);
            List<String> colNames= asdfInfo.tables.get(tName);
            int tableLength= getColDataNode(asdfInfo.tablesRootNode(), tName, 0).getShape().get(0);

            DataType[] colsDtAry= dg.getDataDefinitions();
            for(int rowIdx=0;(rowIdx<tableLength);rowIdx++) {
                DataObject row = new DataObject(dg);
                for(int i=0;i<colNames.size();i++) {
                    NdArray<?> aryNode= getColDataNode(asdfInfo.tablesRootNode(), tName, i);
                    Object v= getAryNodeValue(aryNode,rowIdx);
                    row.setDataElement(colsDtAry[i], v);
                }
                dg.add(row);
            }
            return dg;
        } catch (Exception e) {
            throw new IOException(e.toString(),e);
        }
    }

    private static DataGroup createDataGroup(AsdfFileInfo asdfInfo, int tableIdx) throws IOException {
        if (asdfInfo == null || asdfInfo.tablesRootNode()==null) throw new IOException("could not read table");
        int startingTableIdx= asdfInfo.imageNameList.size();
        if (tableIdx>=startingTableIdx+asdfInfo.tables.size()) throw new IOException("table index exceeds number of tables");
        String tName= asdfInfo.tableNames().get(tableIdx-startingTableIdx);
        List<String> colNames= asdfInfo.tables.get(tName);

        DataType[] colsDtAry = new DataType[colNames.size()];
        for(int i=0;i<colNames.size();i++) {
            NdArray<?> aryNode= getColDataNode(asdfInfo.tablesRootNode(), tName, i);
            var dType= aryNode.getDataType().getFamily();
            colsDtAry[i]= new DataType(colNames.get(i), convertAsdfDataTypeToClass(dType));
        }
        return new DataGroup(tName, colsDtAry);
    }


    public static File createFitsVersionOfRomandAsdfFile(File infile) throws FitsException, IOException {
        Path path= infile.toPath();
        try (AsdfFile asdfFile = Asdf.open(path)) {
            AsdfNode roman = asdfFile.getTree().get(ROMAN);
            if (roman==null) return null;
            AsdfFileInfo asdfInfo= getAsdfFileInfo(asdfFile);
            if (asdfInfo==null) throw new IOException("could evaluate ASDF file");
            try (final RomanModel<?> model = RomanDatamodels.open(path)) {
                if (model instanceof RomanImageModel<?> imageModel) {
                    FitsWcs fitsWcs= Util.Try.it(() -> imageModel.getMeta().getFitsWcs()).get();
                    var baseAxisInfo= getAxisInfo(imageModel.getData());
                    return makeFITS(infile,roman,asdfInfo,baseAxisInfo,fitsWcs);
                }
                else {
                    return makeFITS(infile,roman,asdfInfo);
                }
            }
            catch (RuntimeException e) {
                return makeFITS(infile,roman,asdfInfo);
            }
        } catch (RuntimeException e) {
            throw new IOException(e.toString(),e);
        }
    }


    private static File makeFITS(File infile, AsdfNode rootDataNode, AsdfFileInfo asdfInfo) throws IOException {
        return makeFITS(infile,rootDataNode,asdfInfo,null,null);
    }

    private static File makeFITS(File infile, AsdfNode rootDataNode, AsdfFileInfo asdfInfo, AxisInfo baseAxisInfo, FitsWcs fitsWcs) throws IOException {
        String fBase= FileUtil.getBase(infile);
        File retFile= new File(infile.getParent()+"/"+ fBase+"---from-asdf"+".fits");
        Fits fits= new Fits();
        fits.addHDU(new NullDataHDU());
        for(String extName : asdfInfo.imageNameList) {
            var arrayNode= (NdArrayAsdfNode)rootDataNode.get(extName);
            AxisInfo axisInfo= getAxisInfo(arrayNode.asNdArray());
            FitsWcs wcsToUse= imagesMatch(baseAxisInfo,axisInfo) ? fitsWcs : null;
            var inHeaders= makeFitsLikeHeadersUsingRomanImageModel(wcsToUse, arrayNode, asdfInfo,extName, false);
            Object ary= getImageAry(axisInfo.naxis,arrayNode);
            var hdu= Fits.makeHDU(ary);
            Header header= hdu.getHeader();
            for(var h : inHeaders) {
                header.addValue(h.key,h.value, "derived from roman asdf");
            }
            fits.addHDU(hdu);
        }
        fits.write(retFile);
        closeFits(fits);
        return retFile;
    }

    private static boolean imagesMatch(AxisInfo baseAxisInfo, AxisInfo axisInfo) {
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

    private static Object getImageAry(int dim, NdArrayAsdfNode arrayNode) {
        var ndAry= arrayNode.asNdArray();
        var dt= ndAry.getDataType().getFamily();
        return switch (dt){
            case FLOAT -> dim==3 ? ndAry.asFloatNdArray().toArray(new float[0][0][0]) : ndAry.asFloatNdArray().toArray(new float[0][0]);
            case COMPLEX -> dim==3 ? ndAry.asDoubleNdArray().toArray(new double[0][0][0]) : ndAry.asDoubleNdArray().toArray(new double[0][0]);
            case INT, UINT -> dim==3 ? ndAry.asIntNdArray().toArray(new int[0][0][0]) : ndAry.asIntNdArray().toArray(new int[0][0]);
            default -> arrayNode.asNdArray().toRawArray();
        };
    }


    private static AsdfFileInfo getAsdfFileInfo(AsdfFile asdfFile) {
        String mainTreeNode= ROMAN; // todo - eventually this will have to be computed
        AsdfNode roman = asdfFile.getTree().get(mainTreeNode);
        if (roman==null) return null;
        List<String> imageNameList= new ArrayList<>();
        AsdfNode romanMeta= Util.Try.it(() -> roman.get("meta")).get();
        String telescope= Util.Try.it(() -> romanMeta.getString("telescope")).getOrElse("");
        String instrument= Util.Try.it(() -> romanMeta.get("instrument").getString("name")).getOrElse("");
        String origin= Util.Try.it(() -> romanMeta.getString("origin")).getOrElse("");
        AsdfNode tablesRootNode= Util.Try.it(() -> romanMeta.get("individual_image_meta")).get();


        for(AsdfNode node : roman) {
            if (node instanceof StringAsdfNode) {
                var key= node.asString();
                if (roman.get(key) instanceof NdArrayAsdfNode) {
                    imageNameList.add(key);
                }
            }
        }
        var tables= findTables(tablesRootNode);
        List<String> tableNames= tables.keySet().stream().toList();
        return new AsdfFileInfo(telescope,instrument,origin,mainTreeNode,imageNameList,tablesRootNode,tables,tableNames);
    }



    private static DataGroup getDetails(FitsWcs fitsWcs, NdArrayAsdfNode arrayNode, AsdfFileInfo fileInfo, String extName) {
        DataType[] cols = new DataType[] {
                new DataType("#", Integer.class),
                new DataType("key", String.class),
                new DataType("value", String.class),
                new DataType("comment", String.class)
        };
        DataGroup dg = new DataGroup("Header of extension with index " + 0, cols);

        var asdfHeaders= makeHeadersUsingRomanImageModel(fitsWcs, arrayNode, fileInfo,extName);
        for(var h : asdfHeaders) {
            DataObject row = new DataObject(dg);
            row.setDataElement(cols[0], dg.size());
            row.setDataElement(cols[1], h.key);
            row.setDataElement(cols[2], h.value);
            row.setDataElement(cols[3], "");
            dg.add(row);
        }
        var headers= makeFitsLikeHeadersUsingRomanImageModel(fitsWcs, arrayNode, fileInfo,extName, true);
        for(var h : headers ) {
            DataObject row = new DataObject(dg);
            row.setDataElement(cols[0], dg.size());
            row.setDataElement(cols[1], h.keyF());
            row.setDataElement(cols[2], h.value);
            row.setDataElement(cols[3], h.comment);
            dg.add(row);
        }
        return dg;
    }

    private static List<AsdfHeaderEntry> makeHeadersUsingRomanImageModel(FitsWcs fitsWcs, NdArrayAsdfNode arrayNode,
                                                                          AsdfFileInfo fileInfo, String extName) {
        List<AsdfHeaderEntry> list= new ArrayList<>();
        AxisInfo axis= getAxisInfo(arrayNode.asNdArray());
        list.add(new AsdfHeaderEntry("name",extName));
        list.add(new AsdfHeaderEntry("data type",arrayNode.asNdArray().getDataType().getFamily().name()));
        list.add(new AsdfHeaderEntry("telescope",fileInfo.telescope));
        list.add(new AsdfHeaderEntry("instrument",fileInfo.instrument));
        list.add(new AsdfHeaderEntry("axis",axisDesc(axis)));
        if (fitsWcs==null) return list;

        // if we know projection information, add mre
        list.add(new AsdfHeaderEntry("projection", fitsWcs.getProjection().name()));
        var cdeltAry= fitsWcs.getCdelt().asDoubleNdArray();
        list.add(new AsdfHeaderEntry("cdelt", String.format("[%f,%f]",cdeltAry.get(0),cdeltAry.get(1))));
        var crValAry= fitsWcs.getCrval().asDoubleNdArray();
        list.add(new AsdfHeaderEntry("crVal", String.format("[%f,%f]",crValAry.get(0),crValAry.get(1))));
        var crPixAry= fitsWcs.getCrpix().asDoubleNdArray();
        list.add(new AsdfHeaderEntry("crPix", String.format("[%f,%f]",crPixAry.get(0),crPixAry.get(1))));
        int pc1Max= fitsWcs.getPc().getShape().get(0);
        int pc2Max= fitsWcs.getPc().getShape().get(0);
        list.add(new AsdfHeaderEntry("pc", String.format("%dx%d array",pc1Max,pc2Max)));
        return list;
    }



    private static List<AsdfAsFitsHeaderEntry> makeFitsLikeHeadersUsingRomanImageModel(FitsWcs fitsWcs,
                                                                                       NdArrayAsdfNode arrayNode,
                                                                                       AsdfFileInfo fileInfo,
                                                                                       String extName,
                                                                                       boolean includeImageTypeInfo) {
        List<AsdfAsFitsHeaderEntry> list= new ArrayList<>();
        AxisInfo axis= getAxisInfo(arrayNode.asNdArray());
        //todo DATE

        if (includeImageTypeInfo) list.add(new AsdfAsFitsHeaderEntry("XTENSION","IMAGE",""));
        list.add(new AsdfAsFitsHeaderEntry("EXTNAME",extName,""));
        if (includeImageTypeInfo) list.add(new AsdfAsFitsHeaderEntry("BITPIX",dataTypeToFits(arrayNode),""));
        list.add(new AsdfAsFitsHeaderEntry("TELESCOP",fileInfo.telescope,""));
        list.add(new AsdfAsFitsHeaderEntry("INSTRUME",fileInfo.instrument,""));
        list.add(new AsdfAsFitsHeaderEntry("ORIGIN",fileInfo.origin,""));
        if (includeImageTypeInfo) {
            list.add(new AsdfAsFitsHeaderEntry("NAXIS", axis.naxis+"", ""));
            list.add(new AsdfAsFitsHeaderEntry("NAXIS1", axis.width+"", ""));
            list.add(new AsdfAsFitsHeaderEntry("NAXIS2", axis.height+"", ""));
            if (axis.naxis==3) list.add(new AsdfAsFitsHeaderEntry("NAXIS3", axis.depth+"",""));
        }
        if (fitsWcs!=null) {
            var proj= "-"+fitsWcs.getProjection().name();
            list.add(new AsdfAsFitsHeaderEntry("CTYPE1","RA--"+proj,""));
            list.add(new AsdfAsFitsHeaderEntry("CTYPE2","DEC-"+proj,""));
            list.add(new AsdfAsFitsHeaderEntry("RADESYS","ICRS",""));  //todo this should be in fitsWcs but it is not
            list.add(new AsdfAsFitsHeaderEntry("CDELT1",fitsWcs.getCdelt().asDoubleNdArray().get(0)+"",""));
            list.add(new AsdfAsFitsHeaderEntry("CDELT2",fitsWcs.getCdelt().asDoubleNdArray().get(1)+"",""));
            list.add(new AsdfAsFitsHeaderEntry("CRVAL1",fitsWcs.getCrval().asDoubleNdArray().get(0)+"",""));
            list.add(new AsdfAsFitsHeaderEntry("CRVAL2",fitsWcs.getCrval().asDoubleNdArray().get(1)+"",""));
            list.add(new AsdfAsFitsHeaderEntry("CRPIX1",fitsWcs.getCrpix().asDoubleNdArray().get(0)+"",""));
            list.add(new AsdfAsFitsHeaderEntry("CRPIX2",fitsWcs.getCrpix().asDoubleNdArray().get(1)+"",""));
            int pc1Max= fitsWcs.getPc().getShape().get(0);
            int pc2Max= fitsWcs.getPc().getShape().get(0);
            for(int i=1;(i<=pc1Max);i++) {
                for(int j=1;(j<=pc2Max);j++) {
                    list.add(new AsdfAsFitsHeaderEntry("PC"+i+"_"+j, fitsWcs.getPc().get(i-1,j-1)+"",""));
                }
            }
        }
        return list;
    }

    private static String dataTypeToFits(NdArrayAsdfNode data) {
        var dt= data.asNdArray().getDataType().getFamily().name();
        return switch (dt){
            case "FLOAT" -> "-32";
            case "DOUBLE" -> "-64";
            case "INT", "UINT" -> "32";
            default -> "8";
        };
    }

    private static AxisInfo getAxisInfo(NdArray imageModel) {
        Shape shape= imageModel.getShape();
        return shape.getRank()==3
               ? new AxisInfo(3, shape.get(2), shape.get(1), shape.get(0))
               : new AxisInfo(2, shape.get(1), shape.get(0), 0);
    }

    private static String axisDesc(AxisInfo axis) {
        if (axis.naxis>=3 && axis.depth>1) {
            return String.format(" (cube %d x %d x %d)",axis.width,axis.height,axis.depth);
        }
        else {
            return String.format(" (%d x %d)",axis.width,axis.height);
        }
    }




    public static Map<String,List<String>> findTables(AsdfNode tablesRootNode) {
        Map<String,List<String>> tables= new LinkedHashMap<>();
        if (tablesRootNode==null) return tables;
        for(AsdfNode name : tablesRootNode) {
            String desc= tablesRootNode.get(name).toString();
            if (desc.contains("astropy.org:astropy/table")) {
                tables.put(
                        name.asString(),
                        tablesRootNode.get(name).get("colnames").asList(String.class) );
            }
        }
        return tables;
    }


    private static void addTables(AsdfFileInfo asdfInfo, FileAnalysisReport report, int addTo) {
        int i= addTo;
        for(var table : asdfInfo.tables().entrySet()) {
            FileAnalysisReport.Part part = new FileAnalysisReport.Part(Table, "asdf table");
            part.setIndex(i++);
            var tableDetails= getTableDetails(asdfInfo.tablesRootNode(), table.getKey(), table.getValue());
            part.setDesc(table.getKey()+ String.format(" (%d cols x %d rows)", tableDetails.col, tableDetails.row));
            part.setDetails(tableDetails.detailDG());
            report.addPart(part);
        }
    }


    private static Class<?> convertAsdfDataTypeToClass(DataTypeFamilyType asdfDataType) {
        return switch (asdfDataType) {
            case BOOL -> Boolean.class;
            case FLOAT, COMPLEX -> Double.class;
            case TUPLE, ASCII, UCS4 -> String.class;
            case INT, UINT -> Long.class;
        };
    }

    private static Object getAryNodeValue(NdArray<?> aryNode, int index) {
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




    private static TableDetails getTableDetails(AsdfNode tablesRootNode, String tableName, List<String> columnNames) {
        DataType[] cols = new DataType[] {
                new DataType("#", Integer.class),
                new DataType("key", String.class),
                new DataType("value", String.class),
                new DataType("comment", String.class)
        };
        DataGroup dg = new DataGroup("table Details " + 0, cols);

        int idx=0;
        int tableLength=0;
        for(var h : columnNames) {
            DataObject row = new DataObject(dg);
            row.setDataElement(cols[0], dg.size());
            row.setDataElement(cols[1], h);
            try {
                NdArray<?> aryNode= getColDataNode(tablesRootNode, tableName, idx);
                if (idx==0) {
                    tableLength= aryNode.getShape().get(0);
                }
                var dType= aryNode.getDataType().getFamily().toString();
                row.setDataElement(cols[2], dType);
            } catch (Exception e) {
                row.setDataElement(cols[2], "other");
            }
            row.setDataElement(cols[3], "");
            idx++;
            dg.add(row);
        }
        return new TableDetails(dg,tableLength, columnNames.size());
    }

    private static NdArray<?> getColDataNode(AsdfNode tablesRootNode, String tableName, int idx) {
        try {
            return tablesRootNode.get(tableName).get("columns").get(idx).getNdArray("data");
        } catch (Exception e) {
            return tablesRootNode.get(tableName).get("columns").get(idx).getNdArray("value");
        }
    }

    record TableDetails(DataGroup detailDG, int row, int col) {}
    record AsdfHeaderEntry(String key, String value) {}
    record AsdfAsFitsHeaderEntry(String key, String value, String comment) {
        String keyF() { return "f::"+key; }
    }
    record AxisInfo(int naxis, long width, long height, long depth) {}
    record AsdfFileInfo(String telescope, String instrument,String origin, String mainTreeNode,
                        List<String> imageNameList, AsdfNode tablesRootNode,
                        Map<String,List<String>> tables, List<String> tableNames) {}




    //------------------------------------------------------------------------
    //-------------------- Concepts here
    //------------------------------------------------------------------------

// keep for now - this does the same thing as RomanDatamodels.open() without reopening the file
//    public static RomanModel<?> open(AsdfFile asdfFile) throws IOException {
//        Map<String, BiFunction<AsdfFile, AsdfNode, RomanModel<?>>> MODELS = new HashMap();
//        MODELS.put(MosaicModel.TAG_PREFIX, MosaicModel::new);
//        if (!asdfFile.getTree().containsKey("roman")) {
//            throw new RomanDatamodelsException("ASDF file does not appear to be a Roman file");
//        } else {
//            AsdfNode romanNode = asdfFile.getTree().get("roman");
//
//            for(Map.Entry<String, BiFunction<AsdfFile, AsdfNode, RomanModel<?>>> entry : MODELS.entrySet()) {
//                if (romanNode.getTag().startsWith(entry.getKey())) {
//                    try {
//                        return (RomanModel)((BiFunction)entry.getValue()).apply(asdfFile, romanNode);
//                    } catch (Exception e) {
//                        asdfFile.close();
//                        throw e;
//                    }
//                }
//            }
//            throw new RomanDatamodelsException(String.format("Roman tag '%s' not yet supported by this library", romanNode.getTag()));
//        }
//    }


}
