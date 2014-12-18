package edu.caltech.ipac.firefly.core.background;
/**
 * User: roby
 * Date: 9/11/12
 * Time: 9:13 AM
 */


import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.packagedata.PackagedBundle;
import edu.caltech.ipac.firefly.data.packagedata.PackagedReport;
import edu.caltech.ipac.firefly.data.packagedata.SearchBundle;
import edu.caltech.ipac.util.StringUtils;

/**
 * @author Trey Roby
 */
public class BackgroundPartSerializer {

    private final static String SPLIT_TOKEN= "--BGPartSer--";

    public static String serialize(BackgroundPart bp) {
        StringBuilder sb= new StringBuilder(350);
        if (bp instanceof BackgroundSearchReport) {
            throw new RuntimeException("not supported.");
        }
        else if (bp instanceof CompositeJob) {
            throw new RuntimeException("not supported.");
        }
        else if (bp instanceof PackagedBundle) {
            PackagedBundle pb= (PackagedBundle)bp;
            sb.append("PackagedBundle:");
            sb.append(bp.getState().toString()).append(SPLIT_TOKEN);
            sb.append(pb.getUrl()).append(SPLIT_TOKEN);
            sb.append(pb.getDesc()).append(SPLIT_TOKEN);
            sb.append(pb.getPackageIdx()).append(SPLIT_TOKEN);
            sb.append(pb.getFirstFileIdx()).append(SPLIT_TOKEN);
            sb.append(pb.getNumFiles()).append(SPLIT_TOKEN);
            sb.append(pb.getProcessedFiles()).append(SPLIT_TOKEN);
            sb.append(pb.getTotalBytes()).append(SPLIT_TOKEN);
            sb.append(pb.getProcessedBytes()).append(SPLIT_TOKEN);
            sb.append(pb.getUncompressedBytes()).append(SPLIT_TOKEN);
            sb.append(pb.getCompressedBytes()).append(SPLIT_TOKEN);
        }
        else if (bp instanceof PackagedReport) {
            throw new RuntimeException("not supported.");
        }
        else if (bp instanceof BackgroundReport) {
            throw new RuntimeException("not supported.");
        }
        else if (bp instanceof DummyBackgroundPart) {
            sb.append("DummyBackgroundPart:").append(bp.getState().toString());
        }
        else if (bp instanceof SearchBundle) {
            SearchBundle serBun= (SearchBundle)bp;
            sb.append("SearchBundle:");
            sb.append(bp.getState().toString()).append(SPLIT_TOKEN);
            sb.append(serBun.getServerRequest().toString()).append(SPLIT_TOKEN);
            if (serBun.getClientRequest()!=null) {
                sb.append(serBun.getClientRequest().toString()).append(SPLIT_TOKEN);
            }
            else {
                sb.append("null").append(SPLIT_TOKEN);
            }

        }
        else if (bp instanceof DefaultBackgroundPart) {
            sb.append("DefaultBackgroundPart:").append(bp.getState().toString());
        }

        return sb.toString();
    }


    public static BackgroundPart parse(String s) {
        BackgroundPart  retval;
        if (s.startsWith("PackagedBundle:")) {
            s= s.substring("PackagedBundle:".length());
            String sAry[]= StringUtils.parseHelper(s, 12, SPLIT_TOKEN);
            int i= 0;
            BackgroundState state= Enum.valueOf(BackgroundState.class, sAry[i++]);
            String url= StringUtils.checkNull(sAry[i++]);
            String desc= StringUtils.checkNull(sAry[i++]);
            int idx= StringUtils.getInt(sAry[i++], 0);
            int ffIdx= StringUtils.getInt(sAry[i++], 0);
            int numFiles= StringUtils.getInt(sAry[i++], 0);
            int procFiles= StringUtils.getInt(sAry[i++], 0);
            long totalBytes= StringUtils.getLong(sAry[i++], 0);
            long procBytes= StringUtils.getLong(sAry[i++], 0);
            long uncompressBytes= StringUtils.getLong(sAry[i++], 0);
            long compressBytes= StringUtils.getLong(sAry[i++],0);
            PackagedBundle pb= new PackagedBundle(idx,ffIdx,numFiles,totalBytes);
            pb.setState(state);
            pb.setUrl(url);
            pb.setDesc(desc);
            pb.setProcessedFiles(procFiles);
            pb.setNumFiles(numFiles);
            pb.setProcessedBytes(procBytes);
            pb.setUncompressedBytes(uncompressBytes);
            pb.setCompressedBytes(compressBytes);
            retval= pb;
        }
        else if (s.startsWith("SearchBundle:")) {
            s= s.substring("SearchBundle:".length());
            String sAry[]= StringUtils.parseHelper(s, 4, SPLIT_TOKEN);
            int i= 0;
            BackgroundState state= Enum.valueOf(BackgroundState.class, sAry[i++]);
            TableServerRequest req= ServerRequest.parse(sAry[i++], new TableServerRequest());
            String cReqStr= StringUtils.checkNull(sAry[i++]);
            Request cReq= ServerRequest.parse(cReqStr, new Request());
            retval= new SearchBundle(req,cReq,state);
        }
        else if (s.startsWith("DummyBackgroundPart:")) {
            s= s.substring("DummyBackgroundPart:".length());
            BackgroundState state= Enum.valueOf(BackgroundState.class, s);
            retval= new DummyBackgroundPart(state);
        }
        else if (s.startsWith("DefaultBackgroundPart:")) {
            s= s.substring("DefaultBackgroundPart:".length());
            BackgroundState state= Enum.valueOf(BackgroundState.class, s);
            retval= new DefaultBackgroundPart(state);
        }
        else {
            retval= null;
        }
        return retval;

    }

}

