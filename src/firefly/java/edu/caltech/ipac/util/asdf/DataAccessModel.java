package edu.caltech.ipac.util.asdf;

import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.table.io.TableParseHandler;
import org.asdfformat.asdf.AsdfFile;
import org.asdfformat.asdf.ndarray.NdArray;
import org.asdfformat.asdf.node.AsdfNode;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static edu.caltech.ipac.util.asdf.AsdfCommon.AsdfFileInfo;

public interface DataAccessModel {


    AsdfFileInfo getAsdfFileInfo(AsdfFile asdfFile);

    default AsdfCommon.TableDetails getTableDetails(AsdfNode tablesRootNode, String tableName, List<String> columnNames) {return null;}

    default NdArray<?> getColumnNode(AsdfNode tablesRootNode, String tableName, String cName, int columnIdx) {return null;}

    default List<AsdfCommon.ImageDetails> getImageInfo(AsdfFile asdfFile, AsdfFileInfo asdfInfo) {return Collections.emptyList();}


    default void tableToDataGroup(TableParseHandler handler, AsdfFileInfo asdfInfo, AsdfFile asdfFile, int tableIdx) throws IOException {
        try {
            AsdfCommon.DataGroupInfo info = this.createDataGroup(asdfFile, asdfInfo, tableIdx);
            handler.start();
            handler.startTable(0);
            handler.header(info.dataGroup());

            for(int rowIdx=0;(rowIdx<info.tableLength());rowIdx++) {
                Object[] row= new Object[info.colNames().size()];
                for(int i=0;i<info.colNames().size();i++) {
                    NdArray<?> aryNode= this.getColumnNode(asdfInfo.tablesRootNode(), info.dataGroup().getTitle(),
                            asdfInfo.tables().get(info.dataGroup().getTitle()).get(i),
                            i);
                    row[i]= AsdfCommon.getAryNodeValue(aryNode,rowIdx);
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

    default DataGroup tableToDataGroup(File file, AsdfFile asdfFile, AsdfFileInfo asdfInfo, TableServerRequest request, int tableIdx) throws IOException {
        try {
            AsdfCommon.DataGroupInfo info = this.createDataGroup(asdfFile, asdfInfo, tableIdx);
            DataGroup dg= info.dataGroup();

            DataType[] colsDtAry= dg.getDataDefinitions();
            for(int rowIdx=0;(rowIdx<info.tableLength());rowIdx++) {
                DataObject row = new DataObject(dg);
                for(int i=0;i<info.colNames().size();i++) {
                    NdArray<?> aryNode= this.getColumnNode(asdfInfo.tablesRootNode(),
                            dg.getTitle(),
                            asdfInfo.tables().get(dg.getTitle()).get(i),
                            i);
                    Object v= AsdfCommon.getAryNodeValue(aryNode,rowIdx);
                    row.setDataElement(colsDtAry[i], v);
                }
                dg.add(row);
            }
            return dg;
        } catch (Exception e) {
            throw new IOException(e.toString(),e);
        }
    }

    default AsdfCommon.DataGroupInfo createDataGroup( AsdfFile asdfFile,
                                                      AsdfFileInfo asdfInfo,
                                                      int tableIdx ) throws IOException {
        if (asdfInfo == null || asdfInfo.tablesRootNode()==null) throw new IOException("could not read table");
        int startingTableIdx= asdfInfo.imageNameList().size();
        if (tableIdx>=startingTableIdx+asdfInfo.tables().size()) throw new IOException("table index exceeds number of tables");
        String tName= asdfInfo.tableNames().get(tableIdx-startingTableIdx);
        List<String> colNames= asdfInfo.tables().get(tName);

        DataType[] colsDtAry = new DataType[colNames.size()];
        int i=0;
        for(String colName : colNames) {
            NdArray<?> aryNode= this.getColumnNode(asdfInfo.tablesRootNode(), tName, colName, i);
            var dType= aryNode.getDataType().getFamily();
            colsDtAry[i]= new DataType(colNames.get(i), AsdfCommon.convertAsdfDataTypeToClass(dType));
            i++;
        }
        var dg= new DataGroup(tName, colsDtAry);
        int tableLength= this.getColumnNode(asdfInfo.tablesRootNode(), tName, colNames.getFirst(), 0).getShape().get(0);
        return new AsdfCommon.DataGroupInfo(dg,tableLength,colNames, startingTableIdx);
    }

    default File createFitsFile(AsdfFile asdfFile, File infile) throws IOException {
        return null;
    }

}
