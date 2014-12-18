package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.util.Assert;

import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.BasicStroke;
import java.util.HashMap;
import java.util.Map;

public class SkyShapeFactory {

    private Map<String,SkyShape>    _shapes= new HashMap<String,SkyShape>(15);
    static private SkyShapeFactory    _theInstance= null;

    private SkyShapeFactory() {
       _shapes.put("square", makeSquare() ); // call method to make a sq 
       _shapes.put("circle", makeCircle() ); // call method to make a circle 
       _shapes.put("x",      makeX() );      // call method to make a x
       _shapes.put("bigX",   makeBigX() );   // call method to make a large x
       _shapes.put("cross",  makeCross() );  // call method to make a cross
       _shapes.put("bigSquare", makeBigSquare() ); // call method to make a big square
       _shapes.put("diamond", makeDiamond() ); // call method to make a diamond
       _shapes.put("arrow", makeArrow() ); // call method to make an arrow
       _shapes.put("boxcircle", makeBoxCircle() ); // call method to make a boxcircle
       _shapes.put("dot", makeDot() ); // call method to make a boxcircle
       // ...
       // ...
       // ...
       // ...
    }

    /**
     * Return the only instance of the SkyShapeFactory
     */
    public static SkyShapeFactory getInstance() {
         if (_theInstance == null) _theInstance= new SkyShapeFactory();
         return _theInstance;
    }

    public SkyShape getSkyShape(String which) {
       SkyShape s= null;
       if (_shapes.containsKey(which))
            s= _shapes.get(which);
       else
            Assert.tst(false, "Shape " + which+ " does not exist");
       return s;
    }


    protected Map  makeMap(int cap) { return new HashMap(cap); }

    SkyShape makeSquare() {
        Rectangle2D rec= new Rectangle2D.Double(0,0, 7,7);
        return new SkyShape(rec);
    }

    SkyShape makeDot() {
        Rectangle2D rec= new Rectangle2D.Double(0,0, 0,0);
        return new SkyShape(rec);
    }

    SkyShape makeBigSquare() {
        Rectangle2D rec= new Rectangle2D.Double(0,0, 9,9);
        BasicStroke s= new BasicStroke(2,BasicStroke.CAP_ROUND,
                                       BasicStroke.JOIN_BEVEL);
        return new SkyShape(rec,s);
    }

    SkyShape makeCircle() {
        Ellipse2D circle= new Ellipse2D.Double(0,0, 9,9);
        return new SkyShape(circle);
    }

    SkyShape makeBoxCircle() {
        Ellipse2D circle= new Ellipse2D.Double(0,0, 9,9);
        Rectangle2D rec= new Rectangle2D.Double(0,0, 7,7);
        GeneralPath gp= new GeneralPath();
	gp.append(circle, false);
	gp.append(rec, false);
        return new SkyShape(gp);
    }

    SkyShape makeX() {
        GeneralPath gp= new GeneralPath();
        gp.moveTo(0,0);
        gp.lineTo(8,8);
        gp.moveTo(0,8);
        gp.lineTo(8,0);
        return new SkyShape(gp);
    }

    SkyShape makeDiamond() {
        GeneralPath gp= new GeneralPath();
        gp.moveTo(4,0);
        gp.lineTo(8,4);
        gp.lineTo(4,8);
        gp.lineTo(0,4);
        gp.lineTo(4,0);
        return new SkyShape(gp);
    }

    SkyShape makeArrow() {
        GeneralPath gp= new GeneralPath();
        gp.moveTo(-4,8);
        gp.lineTo(4,0);
        gp.lineTo(0,0);
        gp.moveTo(4,0);
        gp.lineTo(4,4);
        return new SkyShape(gp);
    }
    SkyShape XmakeArrow() {
        GeneralPath gp= new GeneralPath();
        gp.moveTo(0,8);
        gp.lineTo(8,0);
        gp.lineTo(4,0);
        gp.moveTo(8,0);
        gp.lineTo(8,4);
        return new SkyShape(gp);
    }

    SkyShape makeBigX() {
//        GeneralPath gp= new GeneralPath();
//        gp.moveTo(0,0);
//        gp.lineTo(12,12);
//        gp.moveTo(0,12);
//        gp.lineTo(12,0);
//
//
//        gp.moveTo(0,1);
//        gp.lineTo(11,12);
//        gp.moveTo(0,11);
//        gp.lineTo(11,0);
//
//        gp.moveTo(1,0);
//        gp.lineTo(12,11);
//        gp.moveTo(1,12);
//        gp.lineTo(12,1);
//
//        return new SkyShape(gp);
        GeneralPath gp= new GeneralPath();
        gp.moveTo(0,0);
        gp.lineTo(12,12);
        gp.moveTo(0,12);
        gp.lineTo(12,0);

        BasicStroke s= new BasicStroke(3,BasicStroke.CAP_ROUND,
                                       BasicStroke.JOIN_BEVEL);
        return new SkyShape(gp,s);
    }

    SkyShape makeCross() {
        GeneralPath gp= new GeneralPath();
        gp.moveTo(0,5);
        gp.lineTo(9,5);
        gp.moveTo(5,0);
        gp.lineTo(5,9);
        return new SkyShape(gp);
    }
}



