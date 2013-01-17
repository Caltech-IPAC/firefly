package edu.caltech.ipac.firefly.core.background;
/**
 * User: roby
 * Date: 3/12/12
 * Time: 3:46 PM
 */


import edu.caltech.ipac.firefly.data.packagedata.PackagedBundle;
import edu.caltech.ipac.firefly.data.packagedata.PackagedReport;
import edu.caltech.ipac.firefly.data.packagedata.RawDataSetBundle;
import edu.caltech.ipac.firefly.data.packagedata.SearchBundle;
import edu.caltech.ipac.util.StringUtils;

/**
 * @author Trey Roby
 */
public class ReportSerializer {

    public static final String PART_TOKEN = "--PartToken";
    public static final String REPORT_TOKEN = "--ReportToken";
    public static final String LIST_TOKEN = "List";
    public static final String TOKEN_END = "--";


    public static String serialize(BackgroundReport r) {
        return serialize(r, 0);
    }


    public static String serialize(BackgroundReport r, int level) {

        String retval = null;
        String token = REPORT_TOKEN + level + TOKEN_END;
        String listToken = REPORT_TOKEN + level + LIST_TOKEN + TOKEN_END;

        StringBuilder sb = new StringBuilder(1000);
        sb.append("[");
        for (int i = 0; i < r.getPartCount(); i++) {
            sb.append(serializePart(r.get(i), level));
            if (i < r.getPartCount() - 1) sb.append(listToken);
        }
        sb.append("]").append(token);
        sb.append(r.getState().toString()).append(token);
        sb.append(r.getID()).append(token);
        sb.append(r.isDone()).append(token);
        sb.append(r.getFileKey()).append(token);
        sb.append(r.getDataSource()).append(token);

        sb.append("[");
        for (int i = 0; i < r.getNumMessages(); i++) {
            sb.append(r.getMessage(i));
            if (i < r.getNumMessages() - 1) sb.append(listToken);
        }
        sb.append("]").append(token);

        sb.append("[");
        int size = r.getAllAttributes().size();
        int i = 0;
        for (BackgroundReport.JobAttributes a : r.getAllAttributes()) {
            sb.append(a.toString());
            if (i < size - 1) sb.append(listToken);
            i++;
        }
        sb.append("]");

        String name;
        if (r instanceof BackgroundSearchReport) {
            name = "BackgroundSearchReport";

        } else if (r instanceof CompositeReport) {
            name = "CompositeReport";
        } else if (r instanceof PackagedReport) {
            sb.append(token);
            sb.append(((PackagedReport) r).getTotalSizeInByte());
            name = "PackagedReport";
        } else {
            name = "BackgroundReport";
        }
        retval = name + "[" + sb.toString() + "]";

        return retval;
    }

    private static String serializePart(BackgroundPart part, int level) {
        String retval = null;
        if (part instanceof BackgroundReport) {
            retval = serialize((BackgroundReport) part, level + 1);
        } else if (part instanceof PackagedBundle) {
            PackagedBundle pb = (PackagedBundle) part;
            String center = StringUtils.combine(PART_TOKEN, pb.getUrl(), pb.getDesc(), pb.getPackageIdx() + "",
                                                pb.getFirstFileIdx() + "", pb.getNumFiles() + "",
                                                pb.getProcessedFiles() + "", pb.getTotalBytes() + "",
                                                pb.getProcessedBytes() + "", pb.getUncompressedBytes() + "",
                                                pb.getCompressedBytes() + "", pb.getState().toString());
            retval = "PackagedBundle[" + center + "]";
        } else if (part instanceof RawDataSetBundle) {
            RawDataSetBundle rb = (RawDataSetBundle) part;
            String center = StringUtils.combine(PART_TOKEN, rb.getRequest().toString(),
                                                rb.getRawDataSet().serialize(),
                                                rb.getState().toString());
            retval = "RawDataSetBundle[" + center + "]";
        } else if (part instanceof SearchBundle) {
            SearchBundle sb = (SearchBundle) part;
            String center = StringUtils.combine(PART_TOKEN, sb.getClientRequest().toString(),
                                                sb.getServerRequest().toString(),
                                                sb.getState().toString());
            retval = "SearchBundle[" + center + "]";
        } else if (part instanceof DefaultBackgroundPart) {
            DefaultBackgroundPart dp = (DefaultBackgroundPart) part;
            retval = "DefaultBackgroundPart[" + dp.toString() + "]";
        } else if (part instanceof DummyBackgroundPart) {
            DummyBackgroundPart dp = (DummyBackgroundPart) part;
            retval = "DummyBackgroundPart[" + dp.toString() + "]";
        }

        return retval;
    }


}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
