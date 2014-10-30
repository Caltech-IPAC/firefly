package edu.caltech.ipac.firefly.server.catquery;

import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.client.net.URLDownload;
import edu.caltech.ipac.firefly.core.EndUserException;
import edu.caltech.ipac.firefly.data.CatalogRequest;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.SDSSRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.IpacTablePartProcessor;
import edu.caltech.ipac.firefly.server.query.ParamDoc;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.DsvToDataGroup;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.firefly.server.util.multipart.MultiPartPostBuilder;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.target.PositionUtil;
import edu.caltech.ipac.util.*;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;
import org.apache.commons.csv.CSVFormat;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.*;

import static edu.caltech.ipac.firefly.util.DataSetParser.VISI_TAG;
import static edu.caltech.ipac.firefly.util.DataSetParser.makeAttribKey;

/**
 * @author tatianag
 */
@SearchProcessorImpl(id = "SDSSQuery", params =
        {@ParamDoc(name = SDSSRequest.RADIUS_ARCMIN, desc = "float, the radius in arcminutes"),
                @ParamDoc(name = ReqConst.USER_TARGET_WORLD_PT, desc = "WorldPt string: ra;dec;coord_sys"),
                @ParamDoc(name = SDSSRequest.FILE_NAME, desc = "for upload, not yet used"),
                @ParamDoc(name = SDSSRequest.NEAREST_ONLY, desc = "return only one match per target")
        })
public class SDSSQuery extends IpacTablePartProcessor {

    public final static String SERVICE_URL="http://skyserver.sdss3.org/dr10/en/tools/search/x_sql.aspx?";
    public final static String SERVICE_URL_UPLOAD="http://skyserver.sdss3.org/public/en/tools/crossid/x_crossid.aspx?";

    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private MultiPartPostBuilder _postBuilder = null;

    /*
         SDSS imaging information: objId,run,rerun,camcol,field
         Photometric pipeline overview: mode,nChild,type,clean,flags
         Photometry: psfMag_{ugriz},psfMagErr_{ugriz},modelMag_{ugriz},modelMagErr_{ugriz}
         Astrometry: ra,dec,raErr,decErr
         Extinction: extinction_{ugriz}

         Morphology: ? waiting for Vandana
         There are lot of columns dealing with the deVaucoulers and exponential fits..
         Note that these fits are performed for each of the five bands.
     */
    private static String SELECT_COLUMNS=
            "ra,dec,raErr,decErr,p.objId,p.run,p.rerun,p.camcol,p.field,"+
            "dbo.fPhotoModeN(mode) as mode,nChild,dbo.fPhotoTypeN(p.type) as type,clean,flags,"+
            "psfMag_u,psfMag_g,psfMag_r,psfMag_i,psfMag_z,psfMagErr_u,psfMagErr_g,psfMagErr_r,psfMagErr_i,psfMagErr_z,"+
            "modelMag_u,modelMag_g,modelMag_r,modelMag_i,modelMag_z,modelMagErr_u,modelMagErr_g,modelMagErr_r,modelMagErr_i,modelMagErr_z,"+
            "extinction_u,extinction_g,extinction_r,extinction_i,extinction_z,mjd";


    /*
     raErr and decErr are not in PhotoTag view. This means we should use PhotoObj.

     Using function: fGetNearbyObjEq(ra,dec,r)
     ra, dec in degrees; r in arcminutes
     There is no limit on the number of objects returned,
     but there are about 40 per sq arcmin.

     */
    private static String SINGLE_TGT_SQL = "SELECT "+SELECT_COLUMNS+",distance"+
            " FROM PhotoObj as p JOIN %FUNCTION%(%RA%,%DEC%,%RAD_ARCMIN%) AS R ON P.objID=R.objID"+
            " ORDER BY distance";
    private static String NEARBY = "dbo.fGetNearbyObjEq";
    private static String NEAREST = "dbo.fGetNearestObjEq";

    public static String UPLOAD_SQL = "SELECT u.up_id,"+SELECT_COLUMNS+
            " FROM #upload u"+
            " JOIN #x x ON x.up_id = u.up_id"+
            " JOIN PhotoObj p ON p.objID = x.objID"+
            " ORDER BY x.up_id";

    private static String BOX_TGT_SQL = "SELECT "+SELECT_COLUMNS+
            " from PhotoObj as p JOIN dbo.fGetObjFromRectEq(%RA_MIN%,%DEC_MIN%,%RA_MAX%,%DEC_MAX%) AS R ON P.objID=R.objID";

    @Override
    protected File loadDataFile(TableServerRequest request) throws IOException, DataAccessException {


        File outFile;
        try {
            // votable has utf-16 encoding, which mismatches the returned content type
            // this confuses tools
            File csv = createFile(request, ".csv");
            String uploadFname = request.getParam(SDSSRequest.FILE_NAME);
            if (StringUtils.isEmpty(uploadFname)) {

                URL url = createGetURL(request);

                URLConnection conn = URLDownload.makeConnection(url);
                conn.setRequestProperty("Accept", "*/*");

                URLDownload.getDataToFile(conn, csv);
            } else {
                URL url = new URL(SERVICE_URL_UPLOAD);
                // use uploadFname
                //POST http://skyserver.sdss3.org/public/en/tools/crossid/x_crossid.aspx

                _postBuilder = new MultiPartPostBuilder(url.toString());
                if (_postBuilder == null) {
                    throw new EndUserException("Failed to create HTTP POST request",
                            "URL "+SERVICE_URL_UPLOAD);
                }

                insertPostParams(request, uploadFname);
                BufferedOutputStream writer = new BufferedOutputStream(new FileOutputStream(csv), 10240);
                try {
                    _postBuilder.post(writer);
                } finally {
                    writer.close();
                }

            }

            // check for errors in returned file
            evaluateCVS(csv);

            DataGroup dg = DsvToDataGroup.parse(csv, CSVFormat.DEFAULT.withCommentStart('#'));
            if (dg == null) {
                    _log.briefInfo("no data found for search");
                    return null;
            }

            if (!StringUtils.isEmpty(uploadFname) && dg.containsKey("up_id")) {
                // increment up_id(uploaded id) by 1 if it's an multi object search
                DataType upId = dg.getDataDefintion("up_id");
                for(DataObject row : dg) {
                    int id = StringUtils.getInt(String.valueOf(row.getDataElement(upId)), -1);
                    if (id >= 0) {
                        row.setDataElement(upId, id + 1);
                    }
                }
            }


            outFile = createFile(request, ".tbl");
            IpacTableWriter.save(outFile, dg);

        } catch (MalformedURLException e) {
            _log.error(e, "Bad URL");
            throw makeException(e, "SDSS Catalog Query Failed - bad url");
        } catch (IOException e) {
            _log.error(e, e.toString());
            throw makeException(e, "SDSS Catalog Query Failed - network Error");
        } catch (EndUserException e) {
            _log.error(e, e.toString());
            throw makeException(e, "SDSS Catalog Query Failed - network Error");
        } catch (Exception e) {
            _log.error(e, e.toString());
            throw makeException(e, "SDSS Catalog Query Failed");
        }
        return outFile;
    }

    private void evaluateCVS(File csv) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(csv), IpacTableUtil.FILE_IO_BUFFER_SIZE);
        try {
            String line = reader.readLine();
            if (line.startsWith("<html>")) {
                throw new IOException("Error obtaining SDSS catalog data");
            }
        } finally {
            FileUtil.silentClose(reader);
        }
    }

    /*
         For crossid match (upload):
         Service: http://skyserver.sdss3.org/public/en/tools/crossid/x_crossid.aspx
         parameters:
         format="csv" output format
         searchType="photo"
         photoScope="nearPrim" or "allPrim"
         photoUpType="ra-dec"
         radius="0.5" (in arcminutes)
         firstcol=0 (index of ra)
         upload "filename"
         uquery SQL query like
           SELECT
               u.*,p.objId,p.run,p.rerun,p.camcol,p.field,dbo.fPhotoModeN(mode) as mode,nChild,dbo.fPhotoTypeN(p.type) as type,clean,flags,psfMag_u,psfMag_g,psfMag_r,psfMag_i,psfMag_z,psfMagErr_u,psfMagErr_g,psfMagErr_r,psfMagErr_i,psfMagErr_z,modelMag_u,modelMag_g,modelMag_r,modelMag_i,modelMag_z,modelMagErr_u,modelMagErr_g,modelMagErr_r,modelMagErr_i,modelMagErr_z,ra,dec,raErr,decErr,extinction_u,extinction_g,extinction_r,extinction_i,extinction_z,mjd
           FROM #upload u
               JOIN #x x ON x.up_id = u.up_id
               JOIN PhotoObj p ON p.objID = x.objID
           ORDER BY x.up_id
     */
    private  void insertPostParams(TableServerRequest request, String uploadFname) throws EndUserException, IOException {
        File uploadFile = VisContext.convertToFile(uploadFname);
        if (uploadFile.canRead()) {
            File sdssUFile = getSDSSUploadFile(uploadFile);
            _postBuilder.addFile("targets",sdssUFile);
        } else {
            throw new EndUserException("SDSS catalog search failed",
                    "Can not read uploaded file: "+ uploadFname);
        }

        String radiusArcMin = request.getParam(SDSSRequest.RADIUS_ARCMIN);
        if (StringUtils.isEmpty(radiusArcMin)) {
            throw new EndUserException("SDSS catalog search failed",
                    "Missing required parameter "+ SDSSRequest.RADIUS_ARCMIN);
        }
        boolean nearestOnly = request.getBooleanParam(SDSSRequest.NEAREST_ONLY);

        _postBuilder.addParam("format", "csv");
        _postBuilder.addParam("searchType", "photo");
        _postBuilder.addParam("photoScope", nearestOnly ? "nearPrim":"allPrim");
        _postBuilder.addParam("radius",radiusArcMin);
        _postBuilder.addParam("photoUpType", "ra-dec");
        _postBuilder.addParam("uquery",UPLOAD_SQL);
        _postBuilder.addParam("firstcol", "0");

    }

    private File getSDSSUploadFile(File uploadFile) throws IOException {
        DataGroup uDg = DataGroupReader.readAnyFormat(uploadFile);
        DataType raType = uDg.getDataDefintion("ra");
        DataType decType = uDg.getDataDefintion("dec");
        DataType.FormatInfo raFmt = raType.getFormatInfo();
        DataType.FormatInfo decFmt = decType.getFormatInfo();
        File sdssUFile = File.createTempFile("sdss_upload", ".csv", ServerContext.getTempWorkDir());
        BufferedWriter writer = new BufferedWriter(new FileWriter(sdssUFile));
        try {
            writer.write("ra,dec\n");
            Iterator i= uDg.iterator();
            DataObject dob;
            while(i.hasNext()) {
                dob = (DataObject)i.next();
                String line = raFmt.formatDataOnly(dob.getDataElement(raType))+","+decFmt.formatDataOnly(dob.getDataElement(decType))+"\n";
                writer.write(line);
            }
        } catch (Exception e) {
            throw new IOException("Unable to parse uploaded file");
        } finally {
            writer.close();
        }
        return sdssUFile;
    }

    /**
     http://skyserver.sdss3.org/dr10/en/tools/search/x_sql.aspx?format=html
     &cmd=SELECT p.objId,p.run,p.rerun,p.camcol,p.field,
                 dbo.fPhotoModeN(mode) as mode,nChild,dbo.fPhotoTypeN(p.type) as type,clean,flags,
                 psfMag_u,psfMag_g,psfMag_r,psfMag_i,psfMag_z,
                 psfMagErr_u,psfMagErr_g,psfMagErr_r,psfMagErr_i,psfMagErr_z,
                 modelMag_u,modelMag_g,modelMag_r,modelMag_i,modelMag_z,
                 modelMagErr_u,modelMagErr_g,modelMagErr_r,modelMagErr_i,modelMagErr_z,
                 ra,dec,raErr,decErr,
                 extinction_u,extinction_g,extinction_r,extinction_i,extinction_z,
                 mjd,distance
      FROM PhotoObj as p JOIN dbo.fGetNearbyObjEq(185.0,-0.5,1) AS R ON P.objID=R.objID ORDER BY distance

     http://skyserver.sdss3.org/dr10/en/tools/search/x_sql.aspx?format=csv&cmd=SELECT p.objId,p.run,p.rerun,p.camcol,p.field,mjd,distance FROM PhotoObj as p JOIN dbo.fGetNearbyObjEq(185.0,-0.5,1) AS R ON P.objID=R.objID ORDER BY distance


    */
    private URL createGetURL(TableServerRequest request) throws EndUserException, MalformedURLException {
        // use ReqConst.USER_TARGET_WORLD_PT and radiusArcMin
        WorldPt pt = request.getWorldPtParam(ReqConst.USER_TARGET_WORLD_PT);
        pt = VisUtil.convertToJ2000(pt);
        if (pt == null) {
            throw new EndUserException("SDSS catalog search failed",
                    "Missing required parameter "+ ReqConst.USER_TARGET_WORLD_PT);
        }
        String radiusArcMin = request.getParam(SDSSRequest.RADIUS_ARCMIN);
        if (StringUtils.isEmpty(radiusArcMin)) {
            throw new EndUserException("SDSS catalog search failed",
                    "Missing required parameter "+ SDSSRequest.RADIUS_ARCMIN);
        }
        String sql;

        String method = request.getParam(CatalogRequest.SEARCH_METHOD);
        if (CatalogRequest.Method.BOX.getDesc().equals(String.valueOf(method))) {
            double radiusArcsec = request.getDoubleParam(SDSSRequest.RADIUS_ARCMIN) * 60;
            PositionUtil.Corners corners = PositionUtil.getCorners(pt.getLon(), pt.getLat(), radiusArcsec);
            String upperLeft = String.format(Locale.US, "%8.6f,%8.6f", corners.getUpperLeft().getLon(), corners.getUpperLeft().getLat());
            String lowerRight = String.format(Locale.US, "%8.6f,%8.6f", corners.getLowerRight().getLon(), corners.getLowerRight().getLat());
            sql = BOX_TGT_SQL.replace("%RA_MAX%,%DEC_MAX%", upperLeft).replace("%RA_MIN%,%DEC_MIN%",lowerRight);
        } else {
            String raDec = String.format(Locale.US, "%8.6f,%8.6f", pt.getLon(), pt.getLat());
            boolean nearestOnly = request.getBooleanParam(SDSSRequest.NEAREST_ONLY);
            String function = nearestOnly ? NEAREST : NEARBY;
            sql = SINGLE_TGT_SQL.replace("%RA%,%DEC%",raDec).replace("%RAD_ARCMIN%",radiusArcMin).replace("%FUNCTION%", function);
        }

        try {
            sql=URLEncoder.encode(sql, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            _log.warn(e, "an exception here should never happen, using UTF-8");
        }

        String urlStr = SERVICE_URL+"format=csv"+"&cmd="+sql;
        return new URL(urlStr);
    }



    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {

        super.prepareTableMeta(meta, columns, request);
        TableMeta.LonLatColumns llc;

        String lonCol = null, latCol = null;
        for (DataType col : columns) {
            if (col.getKeyName().equalsIgnoreCase("ra")) lonCol = col.getKeyName();
            if (col.getKeyName().equalsIgnoreCase("dec")) latCol = col.getKeyName();


            if (!StringUtils.isEmpty(lonCol) && !StringUtils.isEmpty(latCol)) {
                llc = new TableMeta.LonLatColumns(lonCol, latCol, CoordinateSys.EQ_J2000);
                meta.setLonLatColumnAttr(MetaConst.CATALOG_COORD_COLS, llc);
                break;
            }
        }
        boolean catalogDataFound= (lonCol!=null && latCol!=null);
        if (catalogDataFound) {
            meta.setAttribute(MetaConst.CATALOG_OVERLAY_TYPE, "SDSS");
            meta.setAttribute(MetaConst.DATA_PRIMARY, "False");
        }
    }


    @Override
    protected File postProcessData(File dgFile, TableServerRequest request) throws Exception {

        String uploadFname = request.getParam(SDSSRequest.FILE_NAME);
        if (!StringUtils.isEmpty(uploadFname)) {
            DataGroup upDg = DataGroupReader.read(VisContext.convertToFile(uploadFname));

            final DataGroup resDg = DataGroupReader.read(dgFile);
            if (!StringUtils.isEmpty(resDg.getAttribute("joined"))) {
                return dgFile;
            } else {
                resDg.addAttributes(new DataGroup.Attribute("joined", "true"));
            }

            Comparator<DataObject> comparator = new Comparator<DataObject>() {
                public int compare(DataObject row1, DataObject row2) {
                    return getVal(row1).compareTo(getVal(row2));
                }
            };

            ArrayList<DataType> upDefsToSave = new ArrayList();
            for (DataType dt : upDg.getDataDefinitions()) {
                String key = dt.getKeyName();
                if (!key.equals(CatalogRequest.UPDLOAD_ROW_ID)) dt.setKeyName(QueryUtil.getUploadedCName(dt.getKeyName()));
                if (!resDg.containsKey(dt.getKeyName())) {
                    upDefsToSave.add((DataType)dt.clone());
                }
            }
//            upDg.shrinkToFitData(true);

            boolean nearestOnly = request.getBooleanParam(SDSSRequest.NEAREST_ONLY);

            DataGroup results = DataGroupQuery.join(upDg, upDefsToSave.toArray(new DataType[upDefsToSave.size()]), resDg, null, comparator, !nearestOnly, true);
            results.addAttributes(new DataGroup.Attribute(makeAttribKey(VISI_TAG, "up_id"), "hide"));
            DataGroupQuery.sort(results, DataGroupQuery.SortDir.ASC, true, CatalogRequest.UPDLOAD_ROW_ID);
            results.shrinkToFitData(true);
            IpacTableWriter.save(dgFile, results);
        }
        return dgFile;
    }

    private String getVal(DataObject row) {
        String cname = row.containsKey("up_id") ? "up_id" : CatalogRequest.UPDLOAD_ROW_ID;
        return String.valueOf(row.getDataElement(cname));
    }

    public static void main(String [] args) {
        String url = "http://skyserver.sdss3.org/dr10/en/tools/search/x_sql.aspx?format=csv&cmd=SELECT%20p.objId,p.run,p.rerun,p.camcol,p.field,mjd,distance%20FROM%20PhotoObj%20as%20p%20JOIN%20dbo.fGetNearbyObjEq(185.0,-0.5,1)%20AS%20R%20ON%20P.objID=R.objID%20ORDER%20BY%20distance";
        URLConnection conn;
        try {
            conn = URLDownload.makeConnection(new URL(url));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        conn.setRequestProperty("Accept", "text/plain");

        try {
            URLDownload.getDataToFile(conn, new File("/tmp/a.csv"));
        } catch (FailedRequestException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
