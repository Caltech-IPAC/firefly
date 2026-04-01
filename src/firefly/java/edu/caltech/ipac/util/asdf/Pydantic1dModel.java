package edu.caltech.ipac.util.asdf;

import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.table.io.SpectrumMetaInspector;
import org.asdfformat.asdf.AsdfFile;
import org.asdfformat.asdf.ndarray.NdArray;
import org.asdfformat.asdf.node.AsdfNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.util.asdf.AsdfCommon.ROMAN;
import static edu.caltech.ipac.util.asdf.AsdfCommon.ColumnInfo;
import static edu.caltech.ipac.util.asdf.AsdfCommon.isNdAry;
import static edu.caltech.ipac.util.asdf.AsdfCommon.makeTableDetails;
import static edu.caltech.ipac.util.asdf.AsdfCommon.AsdfFileInfo;

public class Pydantic1dModel implements DataAccessModel {


    public AsdfFileInfo getAsdfFileInfo(AsdfFile asdfFile) {
        String mainTreeNode= ROMAN; // todo - eventually this will have to be computed
        AsdfNode mainTag = asdfFile.getTree().get(mainTreeNode);
        if (mainTag==null) return null;
        List<String> imageNameList= new ArrayList<>();
        AsdfNode metaData= Util.Try.it(() -> mainTag.get("meta")).get();
        String telescope= Util.Try.it(() -> metaData.getString("telescope")).getOrElse("");
        String instrument= Util.Try.it(() -> metaData.get("instrument").getString("name")).getOrElse("");
        String origin= Util.Try.it(() -> metaData.getString("origin")).getOrElse("");
        String title= Util.Try.it(() -> metaData.getString("title")).getOrElse("");
        AsdfNode tablesRootNode= Util.Try.it(() -> mainTag.get("data")).get();
        var tables= findTables(tablesRootNode);
        List<String> tableNames= tables.keySet().stream().toList();
        return new AsdfFileInfo(telescope,title,instrument,origin,mainTreeNode,imageNameList,
                tablesRootNode,tables,tableNames);
    }


    public Map<String,List<String>> findTables(AsdfNode tablesRootNode) {
        Map<String, List<String>> tables = new LinkedHashMap<>();
        if (tablesRootNode == null) return tables;
        for (AsdfNode name : tablesRootNode) {
            var node = tablesRootNode.get(name);
            String tableName = name.asString();
            var columnList = new ArrayList<String>();
            for (AsdfNode colNode : node) {
                var colName = colNode.asString();
                if (isNdAry(node.get(colName),1)) columnList.add(colName);
            }
            tables.put(tableName, columnList);
        }
        return tables;
    }


    public AsdfCommon.TableDetails getTableDetails(AsdfNode tablesRootNode, String tableName, List<String> columnNames) {

        AsdfNode node= tablesRootNode.get(tableName);

        int idx = 0;
        var columnInfoList= new ArrayList<ColumnInfo>();
        int tableLength = 0;
        for (var h : columnNames) {
            NdArray<?> aryNode = node.getNdArray(h);
            if (idx == 0) tableLength = aryNode.getShape().get(0);
            var dType = aryNode.getDataType().getFamily().toString();
            columnInfoList.add(new ColumnInfo(h, dType));
            idx++;
        }
        return makeTableDetails(tableName, columnInfoList, tableLength);
    }


    public NdArray<?> getColumnNode(AsdfNode tablesRootNode, String tableName, String colName, int idx) {
        return tablesRootNode.get(tableName).getNdArray(colName);
    }

    public AsdfCommon.DataGroupInfo createDataGroup(AsdfFile asdfFile,
                                                    AsdfFileInfo asdfInfo,
                                                    int tableIdx ) throws IOException {
        var info= DataAccessModel.super.createDataGroup(asdfFile, asdfInfo, tableIdx);
        var metaNode= asdfFile.getTree().get(asdfInfo.mainTreeNode()).get("meta");
        var wlUnit= metaNode.get("unit_wl");
        var wl= new SpectrumMetaInspector.InsertEntry("wl", wlUnit!=null?wlUnit.asString():null);
        var fluxUnit= metaNode.get("unit_flux");
        var fluxUnitStr= fluxUnit!=null?fluxUnit.asString():null;
        var flux= new SpectrumMetaInspector.InsertEntry("flux", fluxUnitStr);
        var fluxErr= new SpectrumMetaInspector.InsertEntry("flux_error", fluxUnitStr);
        SpectrumMetaInspector.insertSpectralData(info.dataGroup(), wl,flux, fluxErr);
        return info;
    }

}
