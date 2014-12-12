package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.client.ClientLog;
import edu.caltech.ipac.planner.AORDisplay;

import java.awt.Color;
import java.awt.Shape;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Maintains a set of shapes that can be combined in any combination.
 * These shapes are all drawing in reference to the center of the focal plane
 *
 * @author Trey Roby
 * @version $Id: ViewingOverlay.java,v 1.5 2012/11/15 22:43:29 roby Exp $
 *
 */
public class ViewingOverlay {

   private Map<String,ShapeInfo> _shapes= new HashMap<String,ShapeInfo>();

   public ViewingOverlay() {
       DynamicShapeFactory dyFact= DynamicShapeFactory.getInstance();
       if (dyFact.size()>0) {
           for(Map.Entry<String,DynamicShapeInfo> entry : dyFact.getEntrySet()) {
               _shapes.put(entry.getKey(), entry.getValue());
           }
       }
   }


   /**
    * Add a shape and a associated color.
    * @param name the name of the shape
    * @param shape the shape to add
    * @param color the color of the shape
    */
   public void loadShape(String name, Shape shape, Color color) {
      _shapes.put(name, new ShapeInfo(shape, color) );
   }

   /**
    * Return an array of shapes given an array of names.
    * @param nameList an array of shape names
    * @return and array of ShapeInfo made from the nameList array
    */
   public ShapeInfo[] makeShape(String nameList[]) {
       return makeShape(nameList,null);
   }

    public ShapeInfo[] makeShape(String nameList[], AORDisplay aor) {
        ShapeInfo retval[]= new ShapeInfo[nameList.length];
        ShapeInfo si;
        for(int i=0; (i<nameList.length); i++) {
            si= _shapes.get(nameList[i]);
            if (si!=null) {
                if (si instanceof DynamicShapeInfo) {
                    si= ((DynamicShapeInfo)si).makeAorVersion(aor);
                }
            }
            retval[i]= si;
        }
        return retval;
    }

   /**
    * Return an array of shapes with all the names in this object
    * @return and array of ShapeInfo made from all the shapes
    */
   public ShapeInfo[] makeShapeWithAll() {
       Set<Map.Entry<String,ShapeInfo>> set= _shapes.entrySet();
       String s[]= new String[set.size()];
       int j=0;
       for(Map.Entry<String,ShapeInfo> entry: set) {
            s[j++]= entry.getKey();
       }
       return makeShape(s);
   }

   /**
    * return the color from a shape name
    * @param name the name of the shape
    * @return Color the color for the shape
    */
   public Color getColor(String name) {
       ShapeInfo si= _shapes.get(name);
       Color retval;
       if (si!=null) {
           retval= si.getColor();
       }
       else {
           ClientLog.warning("Could not find shape name: "+name,
                             "Returning black");
           retval= Color.BLACK;
       }
       return retval;
   }

   /**
    * set the color on a shape name
    * @param name the name of the shape
    * @param c the color to set
    */
   public void setColor(String name, Color c) {
       ShapeInfo si= _shapes.get(name);
       if (si!=null) {
           si.setColor(c);
       }
       else {
           ClientLog.warning("Could not find shape name: "+name,
                             "not setting color");
       }
   }


/*
 * public GeneralPath makeShape(String name) {
 *     GeneralPath retShape= new GeneralPath(GeneralPath.WIND_NON_ZERO, 150);
 *     Shape s= (Shape)_shapes.get(name); 
 *     retShape.append(s, false);
 *     return retShape;
 * }
 */

/*
 * public GeneralPath addToShape(GeneralPath shape, String name) {
 *     Shape s= (Shape)_shapes.get(name); 
 *     shape.append(s, false);
 *     return shape;
 * }
 */



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
