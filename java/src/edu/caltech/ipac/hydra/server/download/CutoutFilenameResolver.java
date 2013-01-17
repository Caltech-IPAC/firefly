package edu.caltech.ipac.hydra.server.download;

import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;

public class CutoutFilenameResolver implements FileInfo.FileNameResolver {
    private String _cutoutData;
    private String _pathStructure;

    public CutoutFilenameResolver(String data, String path) {
        _cutoutData = data;
        _pathStructure = path;
    }

    public String getResolvedName(String input) {
        String fileName;
        String gzExt = "";
        String path = StringUtils.isEmpty(_pathStructure) ? "" : _pathStructure;

        if (FileUtil.isExtension(input, FileUtil.GZ)) {
            gzExt = "." + FileUtil.GZ;
            input = FileUtil.getBase(input);
        }

        String base = FileUtil.getBase(input);
        if (!StringUtils.isEmpty(base)) {
            fileName = path + base + _cutoutData + "." + FileUtil.getExtension(input) + gzExt;
        } else {
            fileName = path + input + _cutoutData + gzExt;
        }

        fileName = fileName.replaceAll(",", "_");
        return fileName;
    }
}