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
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED ?AS-IS? TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
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
