package edu.caltech.ipac.firefly.server.servlets;

import edu.caltech.ipac.util.CollectionUtil;
import edu.caltech.ipac.voservices.server.RemoteDataProvider;
import edu.caltech.ipac.voservices.server.VODataProvider;
import edu.caltech.ipac.voservices.server.VOTableWriter;
import edu.caltech.ipac.voservices.server.configmapper.Config;
import edu.caltech.ipac.voservices.server.servlet.VOServices;
import edu.caltech.ipac.voservices.server.tablemapper.TableMapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: tlau
 * Date: 3/27/13
 * Time: 7:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class BaseVoServices extends VOServices {
/*
    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

            // extract parameters
            Map origParamMap = req.getParameterMap();
            Map<String, String> paramMap = new HashMap<String,String>();

            // parameters could be upper or lower case
            for (Object p : origParamMap.keySet()) {
                if (p instanceof String) {
                    paramMap.put(((String)p).toUpperCase(), (((String[])origParamMap.get(p))[0]).trim());
                }
            }

            String mimeType = "text/xml";
            res.setContentType(mimeType);
            //String encoding = null;
            //if (encoding != null) { res.setHeader("Content-encoding", encoding); }
            //boolean inline = true;
            //res.setHeader("Content-disposition",(inline ? "inline" : "attachment")+"; filename=test");


            // The parameter map should contain parameter with the name SERVICE and DATASET to indicate the name of
            // the desired service and dataset. Each service/dataset pair has a corresponding table mapping file, with
            // contains the URL to the data service, returning IPAC table (to be mapped into VOTable),
            // and the definition of VO output (service parameters and field mappings).
            String service;
            if(!paramMap.containsKey(PARAM_SERVICE)) {
                List<String> services = getAvailableServices();
                VOTableWriter.sendError(new PrintStream(res.getOutputStream()), "Missing " + PARAM_SERVICE + " parameter in the URL. Supported (SERVICE DATASET) pairs: " + CollectionUtil.toString(services) + ".");
                return;
            } else {
                service = paramMap.get(PARAM_SERVICE);
            }

           String dataset;
            if(!paramMap.containsKey(PARAM_DATASET)) {
                List<String> services = getAvailableServices();
                VOTableWriter.sendError(new PrintStream(res.getOutputStream()), "Missing "+PARAM_DATASET+" parameter in the URL. Supported (SERVICE DATASET) pairs: "+CollectionUtil.toString(services)+".");
                return;
            } else {
                dataset = paramMap.get(PARAM_DATASET);
            }

            Config config = getConfigMapper().getConfig(service, dataset);
            if (config == null) {
                List<String> services = getAvailableServices();
                VOTableWriter.sendError(new PrintStream(res.getOutputStream()), "Unsupported SERVICE or DATASET. Supported (SERVICE DATASET) pairs: "+CollectionUtil.toString(services)+".");
                return;
            }
            TableMapper tableMapper;
            try {
                tableMapper = getTableMapper(config);
            } catch (Exception e) {
                VOTableWriter.sendError(new PrintStream(res.getOutputStream()), e.getMessage());
                return;
            }


            if (tableMapper != null) {
                String format = getFormat(paramMap, tableMapper);
                VODataProvider dataProvider = new RemoteDataProvider(tableMapper, paramMap);
                dataProvider.setTestMode(format.equalsIgnoreCase(FORMAT_TEST));
                VOTableWriter voWriter = new VOTableWriter(dataProvider);

                if (format.equalsIgnoreCase(FORMAT_METADATA)) {
                    voWriter.sendMetadata(new PrintStream(res.getOutputStream()), tableMapper);
                } else {
                    voWriter.sendData(new PrintStream(res.getOutputStream()), paramMap);
                }
            } else {
                VOTableWriter.sendError(new PrintStream(res.getOutputStream()), "Can not obtain mapping info for service "+service);
            }

        } */
}
