/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.rpc;

import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.server.ServCommand;
import edu.caltech.ipac.firefly.server.ServerCommandAccess;
import edu.caltech.ipac.firefly.server.SrvParam;
import edu.caltech.ipac.firefly.server.visualize.DirectStretchUtils.CompressType;
import edu.caltech.ipac.firefly.server.visualize.VisJsonSerializer;
import edu.caltech.ipac.firefly.server.visualize.VisServerOps;
import edu.caltech.ipac.firefly.server.visualize.imagesources.ImageMasterData;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.PixelValue;
import edu.caltech.ipac.visualize.plot.plotdata.FitsExtract;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.List;

/**
 * @author Trey Roby
 * Date: 2/8/12
 */
public class VisServerCommands {

    public static class FileFluxCmdJson extends ServCommand {
        public String doCommand(SrvParam sp) throws IllegalArgumentException {
            PlotState[] stateAry= sp.getStateAry();
            List<PixelValue.Result> res= VisServerOps.getFlux(stateAry,sp.getRequiredImagePt("pt"));
            return VisJsonSerializer.createPixelResultJson(res,stateAry[0].getBands(),stateAry.length);
        }

    }

    public static class GetWebPlotCmd extends ServCommand {

        public String doCommand(SrvParam sp) throws IllegalArgumentException {
            WebPlotRequest red  =     sp.getOptionalWebPlotRequest(ServerParams.RED_REQUEST);
            WebPlotRequest green=     sp.getOptionalWebPlotRequest(ServerParams.GREEN_REQUEST);
            WebPlotRequest blue =     sp.getOptionalWebPlotRequest(ServerParams.BLUE_REQUEST);
            WebPlotRequest nobandReq= sp.getOptionalWebPlotRequest(ServerParams.NOBAND_REQUEST);

            boolean threeColor;

            if (nobandReq != null) threeColor= false;
            else if (red != null || green != null || blue != null) threeColor = true;
            else throw new IllegalArgumentException("No request specified");


            WebPlotResult result = threeColor ? VisServerOps.create3ColorPlot(red, green, blue) :
                                                VisServerOps.createPlot(nobandReq);
            return VisJsonSerializer.createResultJson(result);
        }
    }

    public static class GetWebPlotGroupCmd extends ServCommand {

        public String doCommand(SrvParam sp) throws IllegalArgumentException {
            String requestKey = sp.getRequired(ServerParams.PROGRESS_KEY);
            List<WebPlotResult> resultList= VisServerOps.createPlotGroup(sp.getRequestList(),requestKey);
            return VisJsonSerializer.createPlotGroupResultJson(resultList,requestKey);
        }
    }

// keep around for now but will probably not use
//    public static class FloatAryCmd extends ServerCommandAccess.HttpCommand {
//
//        public void processRequest(HttpServletRequest req, HttpServletResponse res, SrvParam sp) throws Exception {
//
//            PlotState state= sp.getState();
//            Band band = Band.parse(sp.getRequired(ServerParams.BAND));
//            float [] float1D= VisServerOps.getFloatDataArray(state,band);
//            res.setContentType("application/octet-stream");
//
//            ByteBuffer byteBuf = ByteBuffer.allocateDirect(float1D.length * Float.BYTES); //4 bytes per float
//            byteBuf.order(ByteOrder.nativeOrder());
//            byteBuf.order(ByteOrder.LITTLE_ENDIAN);
//            FloatBuffer buffer = byteBuf.asFloatBuffer();
//            buffer.put(float1D);
//            buffer.position(0);
//            WritableByteChannel chan= Channels.newChannel(res.getOutputStream());
//            chan.write(byteBuf);
//            chan.close();
//        }
//    }

    public static class ByteAryCmd extends ServerCommandAccess.HttpCommand {

        public void processRequest(HttpServletRequest req, HttpServletResponse res, SrvParam sp) throws Exception {

            PlotState state= sp.getState();
            boolean mask= sp.getOptionalBoolean(ServerParams.MASK_DATA,false);
            int maskBits= sp.getOptionalInt(ServerParams.MASK_BITS,0);
            int tileSize= sp.getRequiredInt(ServerParams.TILE_SIZE);
            String compress= sp.getOptional(ServerParams.DATA_COMPRESS, "FULL");
            CompressType ct;
            try {
                ct = CompressType.valueOf(compress.toUpperCase());
            } catch (IllegalArgumentException e) {
                ct= CompressType.FULL;
            }

            byte[] data = VisServerOps.getByteStretchArray(state,tileSize,mask,maskBits,ct);

            res.setContentType("application/octet-stream");
            ByteBuffer byteBuf = ByteBuffer.wrap(data);
            byteBuf.position(0);
            WritableByteChannel chan= Channels.newChannel(res.getOutputStream());
            chan.write(byteBuf);
            chan.close();
        }
    }



    public static class ExtractionCmd extends ServerCommandAccess.HttpCommand {
        public void processRequest(HttpServletRequest req, HttpServletResponse res, SrvParam sp) throws Exception {

            PlotState state= sp.getState();
            String exType= sp.getRequired(ServerParams.EXTRACTION_TYPE);
            boolean useFloat = sp.getOptionalInt(ServerParams.EXTRACTION_FLOAT_SIZE,64)==32;
            int ptSize= sp.getOptionalInt(ServerParams.POINT_SIZE,1);
            int ptSizeX= sp.getOptionalInt(ServerParams.POINT_SIZE_X,1);
            int ptSizeY= sp.getOptionalInt(ServerParams.POINT_SIZE_Y,1);
            int hduNum= sp.getRequiredInt(ServerParams.HDU_NUM);
            FitsExtract.CombineType ct= Enum.valueOf(FitsExtract.CombineType.class,sp.getOptional(ServerParams.COMBINE_OP,"AVG"));

            List<Number> extractList= switch (exType) {
                case "z-axis" ->
                    VisServerOps.getZAxisAry(state,
                            sp.getRequiredImagePt(ServerParams.PT),
                            hduNum, ptSize, ct);

                case "line" ->
                    VisServerOps.getLineDataAry(state,
                            sp.getRequiredImagePt(ServerParams.PT),
                            sp.getRequiredImagePt(ServerParams.PT2),
                            sp.getRequiredInt(ServerParams.PLANE),
                            hduNum, ptSize, ct);

                case "points" ->
                        VisServerOps.getPointDataAry(state,
                                sp.getRequiredImagePtAry(ServerParams.PTARY),
                                sp.getRequiredInt(ServerParams.PLANE),
                                hduNum, ptSizeX, ptSizeY, ct);
                default -> throw new IllegalArgumentException(ServerParams.EXTRACTION_TYPE + " is not supported");
            };

            res.setContentType("application/octet-stream");
            int valueSize= useFloat ? Float.BYTES : Double.BYTES; //4 bytes per float, 8 bytes per double
            ByteBuffer byteBuf = ByteBuffer.allocateDirect(extractList.size() * valueSize);
            byteBuf.order(ByteOrder.nativeOrder());
            byteBuf.order(ByteOrder.LITTLE_ENDIAN);
            if (useFloat) {
                int loc=0;
                float [] floatExtractAry= new float[extractList.size()];
                for(Number n : extractList) floatExtractAry[loc++]= n.floatValue();
                byteBuf.asFloatBuffer().put(floatExtractAry);
            }
            else {
                double [] doubleExtractAry= extractList.stream().mapToDouble(Number::doubleValue).toArray();
                byteBuf.asDoubleBuffer().put(doubleExtractAry);
            }
            byteBuf.position(0);
            WritableByteChannel chan= Channels.newChannel(res.getOutputStream());
            chan.write(byteBuf);
            chan.close();
        }
    }

    public static class Crop extends ServCommand {

        public String doCommand(SrvParam sp) throws IllegalArgumentException {
            
            ImagePt pt1= sp.getRequiredImagePt(ServerParams.PT1);
            ImagePt pt2= sp.getRequiredImagePt(ServerParams.PT2);
            boolean cropMultiAll= sp.getOptionalBoolean(ServerParams.CRO_MULTI_ALL, false);
            if (sp.contains(ServerParams.STATE)) {
                PlotState state= sp.getState();
                WebPlotResult result = VisServerOps.crop(state, pt1, pt2, cropMultiAll);
                return VisJsonSerializer.createResultJson(result);
            }
            else {
                PlotState[] stateAry= sp.getStateAry();
                WebPlotResult result = VisServerOps.crop(stateAry, pt1, pt2, cropMultiAll);
                return VisJsonSerializer.createResultJson(result);
            }
        }
    }

    public static class AreaStat extends ServCommand {

        public String doCommand(SrvParam sp) throws IllegalArgumentException {
            
            PlotState state= sp.getState();
            ImagePt pt1= sp.getRequiredImagePt(ServerParams.PT1);
            ImagePt pt2= sp.getRequiredImagePt(ServerParams.PT2);
            ImagePt pt3= sp.getRequiredImagePt(ServerParams.PT3);
            ImagePt pt4= sp.getRequiredImagePt(ServerParams.PT4);
            String  shape = sp.getOptional(ServerParams.GEOSHAPE);
            String  rotation = sp.getOptional(ServerParams.ROTATION);
            if (shape == null) shape = "rect";
            if (rotation == null) rotation = "0";  // image rotation angle

            double rAngle = Math.toRadians(Double.parseDouble(rotation));

            WebPlotResult result = VisServerOps.getAreaStatistics(state, pt1, pt2, pt3, pt4, shape, rAngle);
            return VisJsonSerializer.createResultJson(result);
        }
    }




    public static class ColorHistogram extends ServCommand {
        public String doCommand(SrvParam sp) throws IllegalArgumentException {
            Band band= Band.parse(sp.getRequired(ServerParams.BAND));
            WebPlotResult result = VisServerOps.getColorHistogram(sp.getState(), band);
            return VisJsonSerializer.createResultJson(result);
        }
    }


    public static class DS9Region extends ServCommand {
        private static final String footprintKey = "${footprintDef}";
        private static final String footprintFileKey="${footprintFile}";

        public String doCommand(SrvParam sp) throws IllegalArgumentException, IOException {
            String fileKey = sp.getRequired(ServerParams.FILE_KEY);
            WebPlotResult result;

            if (fileKey.contains(footprintKey)) {
                result = VisServerOps.getFootprintRegion(fileKey.substring(footprintKey.length()));
            } else if (fileKey.contains(footprintFileKey)) {
                result = VisServerOps.getRelocatableRegions(fileKey.substring(footprintFileKey.length()));
            } else {
                result = VisServerOps.getDS9Region(fileKey);
            }
            return VisJsonSerializer.createResultJson(result);
        }
    }

    public static class SaveDS9Region extends ServCommand {

        public String doCommand(SrvParam sp) throws IllegalArgumentException {
            String data = sp.getRequired(ServerParams.REGION_DATA);
            WebPlotResult result= VisServerOps.saveDS9RegionFile(data);
            return VisJsonSerializer.createResultJson(result);
        }
    }

    public static class GetMasterImageData extends ServCommand {

        public String doCommand(SrvParam sp) throws IllegalArgumentException {
            String imageSources = sp.getRequired(ServerParams.IMAGE_SOURCES);
            String sortOrder = sp.getOptional(ServerParams.SORT_ORDER);
            String[] imageSourcesAry = imageSources.split(",");
            String[] sortOrderAry= (sortOrder!=null) ? sortOrder.split(",") : null;
            JSONArray result= ImageMasterData.getJson(imageSourcesAry, sortOrderAry);
            JSONObject obj= new JSONObject();
            obj.put("success", true);
            obj.put("data", result);
            return obj.toJSONString();
        }
    }
}