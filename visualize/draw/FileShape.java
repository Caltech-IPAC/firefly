package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.gui.SwingSupport;

import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Arc2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.StringTokenizer;

/**
 * This class reads a file of shape information in world coordinates and
 * creates a ViewingOverlay class which represents all the shapes
 *
 * @author Trey Roby
 * @version $Id: FileShape.java,v 1.3 2006/12/13 00:23:41 roby Exp $
 *
 */
public class FileShape {
                // define all the strings that will come from the file
    private static final String SCALE = "scale";
    private static final String CIRCLE= "circle";
    private static final String ARC   = "arc";
    private static final String MOVE  = "move";
    private static final String DRAW  = "draw";
    private static final String OFFSET= "offset";
    private static final String SET   = "set";
    private static final String COLOR = "color";
    private static final String SHAPE = "shape";
    private static final String FLIP  = "flip";

    private float _scale= 1.0F;
    private float _flipY= 1.0F;
    private float _flipX= 1.0F;
    private Color _workingColor= Color.red;

  public FileShape() { }

  /**
   * Make the ViewingOverlay by reading in shapes from a stream and parsing
   * them.
   * @param stream the input stream
   * @return ViewingOverlay the shape infomation
   */
  public ViewingOverlay makeViewingOverlay(InputStream stream) {
      ViewingOverlay viewingOverlay= new ViewingOverlay ();
      GeneralPath gp= null;
      LineNumberReader reader= null;
      _scale= 1.0F;
      _flipX= 1.0F;
      _flipY= 1.0F;
      try {
                 // read in one line at a time and parse it
         reader= new LineNumberReader( new InputStreamReader(stream));
         String line= reader.readLine();
         while (line != null) {
             if (line != null) gp= addLine(line, gp, viewingOverlay);
             line= reader.readLine();
         }
      }
      catch (IOException e) {
         try { reader.close(); } catch (IOException e1) {}
      }
      return viewingOverlay;
  }

  /**
   * Make the ViewingOverlay by reading in shapes from a file and parsing
   * them.
   * @param f the input file
   * @return ViewingOverlay the shape infomation
   */
  public ViewingOverlay makeViewingOverlay(File f) 
                                   throws FileNotFoundException {
      FileInputStream s= new FileInputStream(f);
      return makeViewingOverlay(s);
  }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


  /**
   * Parse a string from the file.
   * @param line line from the file
   * @param gp the shape that is being built
   * @param viewingOverlay as shapes are built they are added to this object
   */
  private GeneralPath addLine(String         line, 
                              GeneralPath    gp, 
                              ViewingOverlay viewingOverlay) {

       if (line.charAt(0) == '#') return gp;  // is first char a comment ??

       StringTokenizer st= new StringTokenizer(line);
       try {
           String tok= st.nextToken(); 
           if (tok.equals(SCALE)) {
                _scale= Float.parseFloat(st.nextToken());
           }
           else if (tok.equals(CIRCLE)) {
                doCircle(st,gp);
           }
           else if (tok.equals(ARC)) {
               doArc(st,gp);
           }
           else if (tok.equals(MOVE)) {
                doMove(st,gp);
           }
           else if (tok.equals(DRAW)) {
                doDraw(st,gp);
           }
           else if (tok.equals(OFFSET)) {
                // do nothing
           }
           else if (tok.equals(SET)) {
                gp= doSet(st, line, gp, viewingOverlay);
           }
           else if (tok.equals(FLIP)) {
                doFlip(st);
           }
           else {
                showUnreconizedError(line);
           }
       } catch (Exception e) {
              // if anything unforsceen goes wrong show an error for this line
           showUnreconizedError(line);
       }
       return gp;
  }

  private void showUnreconizedError(String line) {
       System.out.println("FileShape: ignoring unreconized line:");
       System.out.println("FileShape: " + line);
  }

  /**
   * create a circle shape and add it to the GeneralPath
   * @param st the tokenizer where the parameters come from
   * @param gp     the GeneralPath to add to
   */
  private void doCircle(StringTokenizer st, GeneralPath gp) {
      float p1= Float.parseFloat(st.nextToken());
      float p2= Float.parseFloat(st.nextToken());
      float p3= Float.parseFloat(st.nextToken());
      p2*= _flipX;
      p3*= _flipY;
      p2-= p1;  
      p3-= p1;
      p1*= 2;
      Ellipse2D circle= new Ellipse2D.Double(p2*_scale, p3*_scale, 
                                   p1*_scale, p1*_scale);
      gp.append( circle, false);
  }
    /**
     * create a circle shape and add it to the GeneralPath
     * @param st the tokenizer where the parameters come from
     * @param gp     the GeneralPath to add to
     */
    private void doArc(StringTokenizer st, GeneralPath gp) {
        double p1= Double.parseDouble(st.nextToken());
        double x= Double.parseDouble(st.nextToken());
        double y= Double.parseDouble(st.nextToken());
        double alpha1= Double.parseDouble(st.nextToken());
        double alpha2= Double.parseDouble(st.nextToken());
        x*= _flipX;
        y*= _flipY;
        x-= p1;
        y-= p1;
        p1*= 2;
        Arc2D arc= new Arc2D.Double(x*_scale, y*_scale,
                                   p1*_scale, p1*_scale,
                                   alpha1, alpha2, Arc2D.OPEN);
        gp.append( arc, false);
    }

  /**
   * do a move in the GeneralPath
   * @param st the tokenizer where the parameters come from
   * @param gp     the GeneralPath to add to
   */
  private void doMove(StringTokenizer st, GeneralPath gp) {
      float p1= Float.parseFloat(st.nextToken());
      float p2= Float.parseFloat(st.nextToken());
      gp.moveTo( p1*_scale*_flipX, p2*_scale*_flipY);
  }

  /**
   * draw a line in the GeneralPath
   * @param st the tokenizer where the parameters come from
   * @param gp     the GeneralPath to add to
   */
  private void doDraw(StringTokenizer st, GeneralPath gp) {
      float p1= Float.parseFloat(st.nextToken());
      float p2= Float.parseFloat(st.nextToken());
      gp.lineTo( p1*_scale*_flipX, p2*_scale*_flipY);
  }

  /**
   * do a flip
   * @param st the tokenizer where the parameters come from
   */
  private void doFlip(StringTokenizer st) {
      String value= st.nextToken();
      if (value.compareToIgnoreCase("y")==0) {
             _flipY= -1.0F;
      }
      if (value.compareToIgnoreCase("x")==0) {
             _flipX= -1.0F;
      }
  }

  /**
   * do the set
   * @param st the tokenizer where the parameters come from
   * @return GernalPath we might redefine general path
   */
  private GeneralPath doSet(StringTokenizer st, 
                            String          line,
                            GeneralPath     gp,
                            ViewingOverlay  viewingOverlay) {
      String value= st.nextToken();
      if (value.equals(COLOR)) {
            String color=st.nextToken();
            _workingColor= SwingSupport.getColor(color);
            if (_workingColor==null)_workingColor= Color.black;
      }
      else if (value.equals(SHAPE)) {
            String name=st.nextToken();
            gp= new GeneralPath(GeneralPath.WIND_NON_ZERO, 10);
            viewingOverlay.loadShape(name, gp, _workingColor);
      }
      else {
            showUnreconizedError(line);
      }
      return gp;
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
