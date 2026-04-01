package edu.caltech.ipac.util.asdf;


import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.table.DataGroup;
import edu.stsci.roman.datamodels.RomanDatamodels;
import edu.stsci.roman.datamodels.metadata.FitsWcs;
import edu.stsci.roman.datamodels.model.RomanImageModel;
import edu.stsci.roman.datamodels.model.RomanModel;
import org.asdfformat.asdf.AsdfFile;
import org.asdfformat.asdf.ndarray.NdArray;
import org.asdfformat.asdf.node.AsdfNode;
import org.asdfformat.asdf.node.impl.NdArrayAsdfNode;
import org.asdfformat.asdf.node.impl.StringAsdfNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.util.asdf.AsdfCommon.AsdfFileInfo;
import static edu.caltech.ipac.util.asdf.AsdfCommon.ROMAN;
import static edu.caltech.ipac.util.asdf.AsdfCommon.getAxisInfo;
import static edu.caltech.ipac.util.asdf.AsdfCommon.AxisInfo;
import static edu.caltech.ipac.util.asdf.AsdfCommon.TableDetails;
import static edu.caltech.ipac.util.asdf.AsdfCommon.ColumnInfo;

public class RomanPipelineModel implements DataAccessModel {

    private FitsWcs fitsWcs= null;
    private AxisInfo baseAxisInfo = null;
    String mainTreeNode = ROMAN; // todo - eventually this will have to be computed

    public AsdfFileInfo getAsdfFileInfo(AsdfFile asdfFile) {
        AsdfNode mainTag = asdfFile.getTree().get(mainTreeNode);
        if (mainTag == null) return null;
        List<String> imageNameList = new ArrayList<>();
        AsdfNode metaData = Util.Try.it(() -> mainTag.get("meta")).get();
        String telescope = Util.Try.it(() -> metaData.getString("telescope")).getOrElse("");
        String instrument = Util.Try.it(() -> metaData.get("instrument").getString("name")).getOrElse("");
        String origin = Util.Try.it(() -> metaData.getString("origin")).getOrElse("");
        String title = "Roman";
        AsdfNode tablesRootNode = null;

        tablesRootNode = Util.Try.it(() -> metaData.get("individual_image_meta")).get();

        for (AsdfNode node : mainTag) {
            if (node instanceof StringAsdfNode) {
                var key = node.asString();
                if (mainTag.get(key) instanceof NdArrayAsdfNode) {
                    imageNameList.add(key);
                }
            }
        }

        var tables = findTables(tablesRootNode);
        List<String> tableNames = tables.keySet().stream().toList();


        try {
            RomanModel<?> romanModel = RomanDatamodels.open(asdfFile);
            if (romanModel instanceof RomanImageModel<?> imageModel) {
                fitsWcs = Util.Try.it(() -> imageModel.getMeta().getFitsWcs()).get();
                baseAxisInfo = getAxisInfo(imageModel.getData());
            }
        } catch (Exception ignored) {
        }


        return new AsdfFileInfo(telescope, title, instrument, origin, mainTreeNode, imageNameList,
                tablesRootNode, tables, tableNames);
    }


    public Map<String, List<String>> findTables(AsdfNode tablesRootNode) {
        Map<String, List<String>> tables = new LinkedHashMap<>();
        if (tablesRootNode == null) return tables;

        for (AsdfNode name : tablesRootNode) {
            String desc = tablesRootNode.get(name).toString();
            if (desc.contains("astropy.org:astropy/table")) {
                tables.put(
                        name.asString(),
                        tablesRootNode.get(name).get("colnames").asList(String.class));
            }
        }
        return tables;
    }


    public List<AsdfCommon.ImageDetails> getImageInfo(AsdfFile asdfFile, AsdfFileInfo asdfInfo) {
        AsdfNode rootDataNode = asdfFile.getTree().get(mainTreeNode);
        List<AsdfCommon.ImageDetails> imageDetailsList = new ArrayList<>();

        for (String extName : asdfInfo.imageNameList()) {
            var arrayNode = (NdArrayAsdfNode) rootDataNode.get(extName);
            DataGroup dg = AsdfCommon.getDetails(fitsWcs, arrayNode, asdfInfo, extName,null);
            AxisInfo axisInfo = getAxisInfo(arrayNode.asNdArray());
            var imageDetails = new AsdfCommon.ImageDetails(extName, dg, axisInfo,
                    extName + AsdfCommon.axisDesc(getAxisInfo(arrayNode.asNdArray())));
            imageDetailsList.add(imageDetails);
        }
        return imageDetailsList;
    }


    public TableDetails getTableDetails(AsdfNode tablesRootNode, String tableName, List<String> columnNames) {
        int idx = 0;
        var columnInfoList = new ArrayList<ColumnInfo>();
        int tableLength = 0;
        for (var h : columnNames) {

            NdArray<?> aryNode = getColumnNode(tablesRootNode, tableName, h, idx);
            if (idx == 0) tableLength = aryNode.getShape().get(0);

            var dType = aryNode.getDataType().getFamily().toString();
            columnInfoList.add(new ColumnInfo(h, dType));
            idx++;
        }
        return AsdfCommon.makeTableDetails(tableName, columnInfoList, tableLength);

    }

    public NdArray<?> getColumnNode(AsdfNode tablesRootNode, String tableName, String colName, int idx) {
        try {
            return tablesRootNode.get(tableName).get("columns").get(idx).getNdArray("data");
        } catch (Exception e) {
            return tablesRootNode.get(tableName).get("columns").get(idx).getNdArray("value");
        }
    }

    public File createFitsFile(AsdfFile asdfFile, File infile) throws IOException {
        AsdfNode roman = asdfFile.getTree().get(ROMAN);
        AsdfFileInfo asdfInfo = getAsdfFileInfo(asdfFile);
        if (asdfInfo == null) throw new IOException("could evaluate ASDF file");

        List<AsdfCommon.ImageNode> imageNodeList= asdfInfo
                .imageNameList().stream()
                .map((n) -> new AsdfCommon.ImageNode(n, (NdArrayAsdfNode)roman.get(n)))
                .toList();

        return AsdfCommon.makeFITS(infile, asdfInfo, imageNodeList, null, baseAxisInfo, fitsWcs);
    }
}
