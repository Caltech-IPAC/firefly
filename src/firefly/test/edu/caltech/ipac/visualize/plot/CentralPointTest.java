package edu.caltech.ipac.visualize.plot;


import edu.caltech.ipac.firefly.visualize.VisUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Created by zhang on 2/9/17.
 */
public class CentralPointTest {

    private CentralPoint centralPoint;
    private static double delta = 1.e-10;
    private ArrayList<WorldPt> wptList = new ArrayList<>();


    @Before
    public void setUp()  {


        /*prepare the testing data.  Select a point in f3.fits image and uses it as a center point.
           Then use radius = 10 to calculate a few points.  Using this know result to write the unit test cases
         */
        double lon = 329.1889167;//deg
        double lat = 62.2563889;//deg

        WorldPt centerPt = new WorldPt(lon, lat);
        double radius = 10.0;//deg

        VisUtil.Corners corners  = VisUtil. getCorners(centerPt,  radius*3600.0);

        wptList.add(corners.getLowerLeft());
        wptList.add(corners.getLowerRight());
        wptList.add(corners.getUpperLeft());
        wptList.add(corners.getUpperRight());


        //create an instance of CentralPoint
        centralPoint = new CentralPoint();

    }
    @After
    /**
     * Release the memories
     */
    public void tearDown() {

        centralPoint =null;
        wptList=null;
    }

    @Test
    public void testFindCircle() throws CircleException {

        Circle circle = centralPoint.find_circle(wptList);
        WorldPt center = circle.getCenter();
        double radius = circle.getRadius();

        Assert.assertEquals(329.1889167, center.getLon(), delta);
        Assert.assertEquals(62.2563889, center.getLat(), delta);

        for (int i=0;i<wptList.size();i++){
            double distance = VisUtil.computeDistance(center, wptList.get(i));
            double diff = radius - distance;
            Assert.assertTrue(diff>=0);
        }


    }

    @Test
    public void testFindCenter() throws CircleException {

        Circle circle = centralPoint.find_center(
                wptList.get(0).getLon(),
                wptList.get(0).getLat(),
                wptList.get(1).getLon(),
                wptList.get(1).getLat(),
                wptList.get(2).getLon(),
                wptList.get(2).getLat()
                );

        Assert.assertEquals(329.1889167, circle.getCenter().getLon(), delta);
        Assert.assertEquals(62.2563889, circle.getCenter().getLat(), delta);

    }

}
