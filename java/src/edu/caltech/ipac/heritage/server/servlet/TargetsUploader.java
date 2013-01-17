package edu.caltech.ipac.heritage.server.servlet;

import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.servlets.BaseHttpServlet;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.planner.io.FixedSingleTargetParser;
import edu.caltech.ipac.target.Target;
import edu.caltech.ipac.targetgui.TargetList;
import edu.caltech.ipac.util.cache.StringKey;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Date: May 4, 2009
 *
 * @author loi
 * @version $Id: TargetsUploader.java,v 1.5 2011/01/18 19:03:20 balandra Exp $
 */
public class TargetsUploader extends BaseHttpServlet {
    private static final Logger.LoggerImpl LOG = Logger.getLogger();


    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {

        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);

        // Parse the request
        List /* FileItem */ items = upload.parseRequest(req);

        // Process the uploaded items
        Iterator iter = items.iterator();
        while (iter.hasNext()) {
            FileItem item = (FileItem) iter.next();

            if (item.isFormField()) {
                String name = item.getFieldName();
                String value = item.getString();
            } else {
                String fieldName = item.getFieldName();
                String fileName = item.getName();
                String contentType = item.getContentType();
                boolean isInMemory = item.isInMemory();
                long sizeInBytes = item.getSize();
                byte[] content = item.get();

                TargetList targetList = new TargetList();
                FixedSingleTargetParser parser = new FixedSingleTargetParser(null);
                String parsingErrors = "";

                ArrayList<Target> targets = new ArrayList<Target>();
                try {
                    Target target;
                    parser.parseTargets(new BufferedReader(new InputStreamReader(item.getInputStream())), targetList);
                    for (Iterator<Target> itr = targetList.iterator(); itr.hasNext(); ) {
                        target = itr.next();

                        //check for invalid targets
                        if(target.getCoords() == null || target.getCoords().length() < 1){
                            parsingErrors = parsingErrors + "Invalid Target: " + target.getName() + "<br>";
                        } else {
                            targets.add(target);
                        }
                    }                                           
                } catch (FixedSingleTargetParser.TargetParseException px) {
                    sendReturnMsg(res, 500, px.getMessage(), null);
                    return;
                } catch (IOException iox) {
                    sendReturnMsg(res, 500, "Error while reading the uploaded file", null);
                    return;
                } catch (Exception e) {
                    sendReturnMsg(res, 500, e.getMessage(), null);
                    return;
                }

                StringKey key = new StringKey(fileName, System.currentTimeMillis());
                UserCache.getInstance().put(key, targets);

                LOG.debug("File uploaded:" + fileName, "cache key:" + key.toString(),
                          "target count:" + targets.size());
                sendReturnMsg(res, 200, parsingErrors, key.toString());
            }
        }
        
    }
}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
