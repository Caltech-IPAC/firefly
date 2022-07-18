/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize.imageretrieve;

import edu.caltech.ipac.astro.ibe.datasource.AtlasIbeDataSource;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.server.query.ibe.IbeQueryArtifact;
import edu.caltech.ipac.firefly.server.visualize.LockingVisNetwork;
import edu.caltech.ipac.firefly.server.visualize.PlotServUtils;
import edu.caltech.ipac.firefly.server.visualize.imagesources.ImageMasterData;
import edu.caltech.ipac.firefly.server.visualize.imagesources.ImageMasterDataEntry;
import edu.caltech.ipac.firefly.util.MathUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.net.AtlasIbeImageGetter;
import edu.caltech.ipac.visualize.net.AtlasImageParams;
import edu.caltech.ipac.visualize.net.DssImageGetter;
import edu.caltech.ipac.visualize.net.DssImageParams;
import edu.caltech.ipac.visualize.net.IbeImageGetter;
import edu.caltech.ipac.visualize.net.ImageServiceParams;
import edu.caltech.ipac.visualize.net.PtfImageParams;
import edu.caltech.ipac.visualize.net.SloanDssImageGetter;
import edu.caltech.ipac.visualize.net.SloanDssImageParams;
import edu.caltech.ipac.visualize.net.TwoMassImageParams;
import edu.caltech.ipac.visualize.net.WiseImageParams;
import edu.caltech.ipac.visualize.net.ZtfImageParams;
import edu.caltech.ipac.visualize.plot.Circle;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.List;

import static edu.caltech.ipac.firefly.server.visualize.imagesources.ImageMasterDataEntry.PLOT_REQUEST_PARAMS;
import static edu.caltech.ipac.firefly.visualize.WebPlotRequest.FILTER;


@FileRetrieverImpl(id ="SERVICE")
public class ServiceRetriever implements FileRetriever {

    public static final String WISE_3A = "3a";
    private static final JSONArray masterInfo= ImageMasterData.getJson(new String[]{"ALL"}, null);


    public FileInfo getFile(WebPlotRequest r) throws FailedRequestException {
        FileInfo fi= getRawFile(r);
        return addAttributes(fi, r);
    }

    public FileInfo getRawFile(WebPlotRequest r) throws FailedRequestException {
        switch (r.getServiceType()) {
            case ISSA:
            case IRIS: return getISSAorIRISPlotViaAtlas(r,r.getServiceType()== WebPlotRequest.ServiceType.IRIS);
            case TWOMASS: return get2MassPlot(r);
            case MSX: return getMSXPlotViaAtlas(r);
            case DSS: return getDssPlot(r);
            case SDSS: return getSloanDSSPlot(r);
            case WISE: return getWisePlot(r);
            case ZTF: return getZtfPlot(r);
            case PTF: return getPtfPlot(r);
            case AKARI:
            case SEIP:
            case ATLAS: return getAtlasPlot(r);
            default: throw new FailedRequestException("Unsupported Service");
        }
    }


    private static FileInfo addAttributes(FileInfo fi, WebPlotRequest r) {
        if (r.getServiceType()==null) return fi;
        for(Object o : masterInfo) {
            JSONObject e= (JSONObject)o;
            JSONObject reqP= (JSONObject) e.get(PLOT_REQUEST_PARAMS);
            if (reqP!=null) {
                String serviceStr= (String)reqP.get("Service");
                String key= (String)reqP.get("SurveyKey");
                if (ComparisonUtil.equals(serviceStr, r.getServiceType().toString()) && ComparisonUtil.equals(key,r.getSurveyKey())) {
                    fi.putAttribute(WebPlotRequest.WAVE_LENGTH_UM, (String)e.get(ImageMasterDataEntry.PARAMS.WAVELENGTH.getKey()));
                    fi.putAttribute(WebPlotRequest.WAVE_LENGTH, (String)e.get(ImageMasterDataEntry.PARAMS.WAVELENGTH_DESC.getKey()));
                    fi.putAttribute(WebPlotRequest.DATA_HELP_URL, (String)e.get(ImageMasterDataEntry.PARAMS.HELP_URL.getKey()));
                    fi.putAttribute(WebPlotRequest.PROJ_TYPE_DESC, (String)e.get(ImageMasterDataEntry.PARAMS.PROJECT_TYPE_KEY.getKey()));
                    fi.putAttribute(WebPlotRequest.WAVE_TYPE, (String)e.get(ImageMasterDataEntry.PARAMS.WAVE_TYPE.getKey()));
                    break;
                }
            }
        }
        return fi;
    }

    private FileInfo getSloanDSSPlot(WebPlotRequest request) throws FailedRequestException {
        String bandStr = request.getSurveyKey();
        Circle circle = PlotServUtils.getRequestArea(request);
        SloanDssImageParams.SDSSBand band;
        try {
            band = Enum.valueOf(SloanDssImageParams.SDSSBand.class,bandStr);
        } catch (Exception e) {
            band= SloanDssImageParams.SDSSBand.r;
        }
        // this is really size not radius, i am just using Circle to hold the params
        float sizeInDegrees = (float)circle.radius();
        if (sizeInDegrees > 1) sizeInDegrees = 1F;
        if (sizeInDegrees < .02) sizeInDegrees = .02F;

        SloanDssImageParams params = new SloanDssImageParams(request.getProgressKey(), request.getPlotId());
        params.setBand(band);
        //When the size is NaN, use the default size defined in SloanDssImageParams to get the full image
        if (!Float.isNaN(sizeInDegrees)) {
            params.setSizeInDeg(sizeInDegrees);
        }
        params.setWorldPt(circle.center());
        FileInfo fi = LockingVisNetwork.retrieve(params, (p,f) -> SloanDssImageGetter.get((SloanDssImageParams) p,f));
        fi.setDesc(ServiceDesc.get(request));
        return fi;
    }

    private FileInfo getDssPlot(WebPlotRequest request) throws FailedRequestException {
        String surveyKey = request.getSurveyKey();
        Circle circle = PlotServUtils.getRequestArea(request);
        DssImageParams params = new DssImageParams(request.getProgressKey(), request.getPlotId());
        params.setTimeout(15000); // time out - 15 sec
        params.setWorldPt(circle.center());
        float arcMin = (float) MathUtil.convert(MathUtil.Units.DEGREE, MathUtil.Units.ARCMIN, circle.radius());
        params.setWidth(arcMin);// this is really size not radius, i am just using Circle to hold the params
        params.setHeight(arcMin);// this is really size not radius, i am just using Circle to hold the params
        params.setSurvey(surveyKey);
        FileInfo fi= LockingVisNetwork.retrieve(params, (p,f) -> DssImageGetter.get((DssImageParams) p,f));
        fi.setDesc(ServiceDesc.get(request));
        return fi;
    }

    private FileInfo get2MassPlot(WebPlotRequest request) throws FailedRequestException {
        Circle circle = PlotServUtils.getRequestArea(request);
        // this is really size not radius, i am just using Circle to hold the params
        float sizeInArcSec = (float) MathUtil.convert(MathUtil.Units.DEGREE, MathUtil.Units.ARCSEC, circle.radius());
        circle = new Circle(circle.center(), sizeInArcSec);
        List<RelatedData> rdList= IbeQueryArtifact.get2MassRelatedData(circle.center(), circle.radius()+"");
        TwoMassImageParams params = new TwoMassImageParams(request.getProgressKey(), request.getPlotId());
        params.setWorldPt(circle.center());
        params.setDataset(request.getSurveyKey());
        params.setBand(request.getSurveyBand());
        params.setSize((float)circle.radius());
        return retrieveViaIbe(request,params,rdList);
    }

    private FileInfo getISSAorIRISPlotViaAtlas(WebPlotRequest request, boolean isIris) throws FailedRequestException {
        WebPlotRequest copyR= request.makeCopy();
        copyR.setServiceType(WebPlotRequest.ServiceType.ATLAS);
        copyR.setSurveyKey(isIris ? "iras.iris_images" : "iras.issa_images");
        copyR.setParam(FILTER, "file_type like '%science%'");
        if (request.getSurveyKey().equals("12")) copyR.setSurveyBand("IRAS12");
        if (request.getSurveyKey().equals("25")) copyR.setSurveyBand("IRAS25");
        if (request.getSurveyKey().equals("60")) copyR.setSurveyBand("IRAS60");
        if (request.getSurveyKey().equals("100")) copyR.setSurveyBand("IRAS100");
        return getAtlasPlot(copyR);
    }

    private FileInfo getMSXPlotViaAtlas(WebPlotRequest request) throws FailedRequestException {
        WebPlotRequest copyR= request.makeCopy();
        copyR.setServiceType(WebPlotRequest.ServiceType.ATLAS);
        copyR.setSurveyKey("msx.msx_images");
        copyR.setParam(FILTER, "file_type='science'");
        copyR.setSurveyBand(request.getSurveyKey());
        return getAtlasPlot(copyR);
    }

    private FileInfo getAtlasPlot(WebPlotRequest r) throws FailedRequestException {
        Circle circle = PlotServUtils.getRequestArea(r);
        AtlasImageParams params = new AtlasImageParams(r.getSurveyKey(), r.getParam("table"),
                r.getProgressKey(), r.getPlotId());
        params.setWorldPt(circle.center());
        params.setBand(r.getSurveyBand());
        // New image search deals with atlas surveyKey formatted such as 'schema.table'
        String datasetAtlas = r.getSurveyKey();
        String schema, table;
        if(datasetAtlas!=null && datasetAtlas.split("\\.").length==2){
            schema = datasetAtlas.split("\\.")[0];
            table = datasetAtlas.split("\\.")[1];
        }else{
            schema = r.getParam(AtlasIbeDataSource.DATASET_KEY);
            table = r.getParam(AtlasIbeDataSource.TABLE_KEY);
        }
        params.setSchema(schema);
        params.setTable(table);
        params.setInstrument(r.getParam(AtlasIbeDataSource.INSTRUMENT_KEY));
        params.setXtraFilter(r.getParam(AtlasIbeDataSource.XTRA_KEY));
        params.setSize((float)circle.radius());
        params.setDataType(r.getParam(ImageMasterDataEntry.PARAMS.DATA_TYPE.getKey()));
        FileInfo fi = LockingVisNetwork.retrieve(params, (p,f) -> AtlasIbeImageGetter.get((AtlasImageParams) p));
        fi.setDesc(ServiceDesc.get(r));
        return fi;
    }

    private FileInfo getWisePlot(WebPlotRequest r) throws FailedRequestException {
        Circle circle = PlotServUtils.getRequestArea(r);
        WiseImageParams params = new WiseImageParams(r.getProgressKey(), r.getPlotId());
        params.setWorldPt(circle.center());
        params.setProductLevel(r.getSurveyKey());
        params.setBand(r.getSurveyBand());
        params.setSize((float)circle.radius());
        List<RelatedData> rdList= IbeQueryArtifact.getWiseRelatedData(circle.center(), circle.radius()+"", r.getSurveyBand());
        return retrieveViaIbe(r,params,rdList);
    }

    private FileInfo getZtfPlot(WebPlotRequest r) throws FailedRequestException {
        Circle circle = PlotServUtils.getRequestArea(r);
        ZtfImageParams params = new ZtfImageParams(r.getProgressKey(), r.getPlotId());
        params.setWorldPt(circle.center());
        params.setProductLevel(r.getSurveyKey());
        params.setBand(r.getSurveyBand());
        params.setSize((float)circle.radius());
        return retrieveViaIbe(r,params,null);
    }

    private FileInfo getPtfPlot(WebPlotRequest r) throws FailedRequestException {
        Circle circle = PlotServUtils.getRequestArea(r);
        PtfImageParams params = new PtfImageParams(r.getProgressKey(), r.getPlotId());
        params.setWorldPt(circle.center());
        params.setProductLevel(r.getSurveyKey());
        params.setBand(r.getSurveyBand());
        params.setSize((float)circle.radius());
        return retrieveViaIbe(r,params,null);
    }

    private FileInfo retrieveViaIbe(WebPlotRequest r, ImageServiceParams params, List<RelatedData> rdList) throws FailedRequestException {
        FileInfo fi= LockingVisNetwork.retrieve(params, (p,f) -> IbeImageGetter.get((ImageServiceParams) p));
        fi.setDesc(ServiceDesc.get(r));
        if (rdList!=null) fi.addRelatedDataList(rdList);
        return fi;
    }
}