/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

/**
 * Created by IntelliJ IDEA.
 * User: balandra
 * Date: Jan 14, 2010
 * Time: 4:15:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class TagToFile extends BaseHttpServlet {

    protected void processRequest(HttpServletRequest req, HttpServletResponse res) throws Exception {
        String tagName= req.getParameter("tagName");
        String tagDesc= req.getParameter("tagDesc");
        if(tagDesc == null){
            tagDesc = "";
        }
        String tagUrl= req.getParameter("tagUrl");

        BufferedWriter output = new BufferedWriter(new OutputStreamWriter(res.getOutputStream()));

        if(tagName == null || tagUrl == null){
            throw new ServletException("Error in creating file");    
        } else {
            try{
                output.write("Tag Name: " + tagName);
                output.newLine();
                output.write("Tag Description: " + tagDesc);
                output.newLine();
                output.write("Tag URL: " + tagUrl);
 
                res.addHeader("content-type", "text/plain");
                res.addHeader("Content-Disposition",
                                  "attachment; filename="+tagName+".txt");
            }  catch (Exception e){
                  throw new Exception(e.getMessage());
            } finally {
                output.close();
            }
        }
    }
}
