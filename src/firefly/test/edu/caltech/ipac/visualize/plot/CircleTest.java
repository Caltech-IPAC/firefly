package edu.caltech.ipac.visualize.plot;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by zhang on 12/16/16.
 */
public class CircleTest {

    private Circle circle;
    private WorldPt worldPt;
    private static double lon = 337.9204167; //a point in f3.fits
    private static double lat = 337.9204167;
    private static double radius = 3.626;
    private static double delta = 1.0e-11;
    @Before
    /**
     * An one dimensional array is created and it is used to run the unit test for Histogram's public methods
     */
    public void setUp()  {
        worldPt = new WorldPt(lon,lat);
        circle = new Circle(worldPt,radius);
    }
    @After
    /**
     * Release the memories
     */
    public void tearDown() {
       circle=null;
    }
    @Test
    public void testGetCenter(){
        Assert.assertNotNull(circle);
        Assert.assertEquals(circle.getCenter().getLon(), worldPt.getLon(), delta);
        Assert.assertEquals(circle.getCenter().getLat(), worldPt.getLat(), delta);
    }
    @Test
    public void testGetRadius(){
        Assert.assertEquals(circle.getRadius(), radius, delta);
    }
}
