package edu.caltech.ipac.util.asdf;

import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.table.DataGroup;
import org.asdfformat.asdf.AsdfFile;
import org.asdfformat.asdf.node.AsdfNode;
import org.asdfformat.asdf.node.impl.NdArrayAsdfNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static edu.caltech.ipac.util.asdf.AsdfCommon.AsdfAsFitsHeaderEntry;
import static edu.caltech.ipac.util.asdf.AsdfCommon.AsdfFileInfo;
import static edu.caltech.ipac.util.asdf.AsdfCommon.ROMAN;
import static edu.caltech.ipac.util.asdf.AsdfCommon.isNdAry;

public class Pydantic2dModel implements DataAccessModel {


    public AsdfCommon.AsdfFileInfo getAsdfFileInfo(AsdfFile asdfFile) {
        String mainTreeNode= ROMAN; // todo - eventually this will have to be computed
        AsdfNode mainTag = asdfFile.getTree().get(mainTreeNode);
        if (mainTag==null) return null;
        List<String> imageNameList= new ArrayList<>();
        AsdfNode metaData= Util.Try.it(() -> mainTag.get("meta")).get();
        String telescope= Util.Try.it(() -> metaData.getString("telescope")).getOrElse("");
        String instrument= Util.Try.it(() -> metaData.get("instrument").getString("name")).getOrElse("");
        String origin= Util.Try.it(() -> metaData.getString("origin")).getOrElse("");
        String title= Util.Try.it(() -> metaData.getString("title")).getOrElse("");

        for (AsdfNode key : mainTag.get("data")) {
            for(AsdfNode im : mainTag.get("data",key)) {
                if (isNdAry(mainTag.get("data",key,im.asString()),2)) imageNameList.add(im.asString());
            }
        }


        return new AsdfFileInfo(
                telescope,title,instrument,origin,mainTreeNode,imageNameList,
                null,Collections.emptyMap(),Collections.emptyList());
    }



    public List<AsdfCommon.ImageDetails> getImageInfo(AsdfFile asdfFile, AsdfFileInfo asdfInfo) {
        AsdfNode rootDataNode = asdfFile.getTree().get(asdfInfo.mainTreeNode(), "data");
        var metaNode= asdfFile.getTree().get(asdfInfo.mainTreeNode(), "meta");
        var fluxUnit= metaNode.get("unit_flux");
        var fluxUnitStr= fluxUnit!=null?fluxUnit.asString():null;
        var wlUnit= metaNode.get("unit_wl");
        var wlUnitStr= wlUnit!=null?wlUnit.asString():null;
        List<AsdfAsFitsHeaderEntry> fluxList= List.of(new AsdfAsFitsHeaderEntry("BUNIT",fluxUnitStr,"from asdf file unit_flux"));
        List<AsdfAsFitsHeaderEntry> wlList= List.of(new AsdfAsFitsHeaderEntry("BUNIT",wlUnitStr,"from asdf file unit_wl"));



        List<AsdfCommon.ImageDetails> imageDetailsList = new ArrayList<>();

        AsdfNode mainTag = asdfFile.getTree().get(asdfInfo.mainTreeNode());
        for (AsdfNode key : mainTag.get("data")) {
            for(AsdfNode im : mainTag.get("data",key)) {
                var node= mainTag.get("data",key,im.asString());
                if (node instanceof NdArrayAsdfNode ndNode) {
                    if (isNdAry(node,2)) {
                        var extName= im.asString();
                        DataGroup dg = AsdfCommon.getDetails(null, ndNode, asdfInfo, extName,
                                extName.contains("spectrum") ? wlList : fluxList
                                );
                        AsdfCommon.AxisInfo axisInfo = AsdfCommon.getAxisInfo(ndNode.asNdArray());
                        imageDetailsList.add(new AsdfCommon.ImageDetails(extName, dg, axisInfo,
                                extName + AsdfCommon.axisDesc(axisInfo)));
                    }
                }
            }
        }
        return imageDetailsList;
    }



    public File createFitsFile(AsdfFile asdfFile, File infile) throws IOException {
        AsdfCommon.AsdfFileInfo asdfInfo = getAsdfFileInfo(asdfFile);
        if (asdfInfo == null) throw new IOException("could evaluate ASDF file");

        List<AsdfCommon.ImageNode> imageNodeList= new ArrayList<>();
        var metaNode= asdfFile.getTree().get(asdfInfo.mainTreeNode()).get("meta");
        var fluxUnit= metaNode.get("unit_flux");
        var fluxUnitStr= fluxUnit!=null?fluxUnit.asString():null;
        var wlUnit= metaNode.get("unit_wl");
        var wlUnitStr= wlUnit!=null?wlUnit.asString():null;
        List<AsdfAsFitsHeaderEntry> fluxList= List.of(new AsdfAsFitsHeaderEntry("BUNIT",fluxUnitStr,"from asdf file unit_flux"));
        List<AsdfAsFitsHeaderEntry> wlList= List.of(new AsdfAsFitsHeaderEntry("BUNIT",wlUnitStr,"from asdf file unit_wl"));


        AsdfNode mainTag = asdfFile.getTree().get(asdfInfo.mainTreeNode());
        for (AsdfNode key : mainTag.get("data")) {
            for(AsdfNode im : mainTag.get("data",key)) {
                if (mainTag.get("data",key,im.asString()) instanceof NdArrayAsdfNode ndNode) {
                    if (isNdAry(ndNode,2)) imageNodeList.add(new AsdfCommon.ImageNode( im.asString(),ndNode));
                }
            }
        }
        return AsdfCommon.makeFITS(infile, asdfInfo, imageNodeList,
                (extName) -> extName.contains("spectrum") ? wlList : fluxList);
    }


}
