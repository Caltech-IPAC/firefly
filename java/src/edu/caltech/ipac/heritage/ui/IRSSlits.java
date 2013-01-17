package edu.caltech.ipac.heritage.ui;

import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.firefly.visualize.VisUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author booth
 * @version $Id: IRSSlits.java,v 1.2 2010/04/05 17:12:47 roby Exp $
 */
public class IRSSlits {

    public static List <WorldPt[]> expandIRSAperture(WorldPt[] pt)
    {
	ArrayList result = new ArrayList<WorldPt[]>();

	/* find shortest edge from the first point  and the long edge adjacent to the shortest edge */
	/*  Here is a picture of the corners:

	1-----------6--7-----------2
	|           |  |           |
	|           |  |           |
	0-----------5--4-----------3

	*/
	double ra0 = pt[0].getLon();  // arbitrarily pick corner 0
	double dec0 = pt[0].getLat();
	double ra1, dec1, ra2, dec2, ra3, dec3;

	double edge_length01 = VisUtil.computeDistance(ra0, dec0, pt[1].getLon(), pt[1].getLat());
	double edge_length03 = VisUtil.computeDistance(ra0, dec0, pt[3].getLon(), pt[3].getLat());
	System.out.println("edge01 = " + edge_length01 + "  edge03 = " + edge_length03 );
	/* find the shortest */
	    if (edge_length01 < edge_length03)
	    {
		/* edge01 is shortest  */
		ra1 = pt[1].getLon();
		dec1 = pt[1].getLat();
		ra2 = pt[2].getLon();
		dec2 = pt[2].getLat();
		ra3 = pt[3].getLon();
		dec3 = pt[3].getLat();
	    }
	    else
	    {
		/* edge03 is shortest  */
		ra1 = pt[3].getLon();
		dec1 = pt[3].getLat();
		ra2 = pt[2].getLon();  // (arbitrary - 2 and 3 may be reversed)
		dec2 = pt[2].getLat();
		ra3 = pt[1].getLon();  // (arbitrary)
		dec3 = pt[1].getLat();
	    }

	System.out.println("Sorted Input aperture corners:");
	System.out.println(ra0 + "      " + dec0);
	System.out.println(ra1 + "      " + dec1);
	System.out.println(ra2 + "      " + dec2);
	System.out.println(ra3 + "      " + dec3);

	double long_edge = VisUtil.computeDistance(ra0, dec0, ra3, dec3);
	System.out.println("long_edge = " + long_edge + " deg,  or " + 
	    long_edge * 3600 + " arcsec");

	/* which IRS aperture is this            */
	/* long_edge  aperture                   */
	/* 11.3"      SH                         */
	/* 18.7"      LH                         */
	/* 57.0"      SH with only one aperture  */
	/* 136"       SH with two apertures      */
	/* 168"       LL with only one aperture  */
	/* 360"       LL with two apertures      */

	if (long_edge < (100.0 / 3600))  //SH, LH, or SL with only one aperture 
	{
	    System.out.println("A");
	    /* only one aperture */
	    result.add(pt);  // just this one aperture
	}
	else if ((long_edge > (150.0 / 3600)) && 
	(long_edge < (200.0 / 3600)))      //LL with only one aperture
	{
	    System.out.println("B");
	    /* only one aperture */
	    result.add(pt);  // just this one aperture
	}
	else 
	{
	    double edge05;
	    double edge04;
	    if (long_edge < (150.0 / 3600))  //150 arcsec = .041667 deg
	    {
		System.out.println("B");
		/* it's SL with two apertures */
		edge05 = 57.0 / 3600;
		edge04 = 79.0 / 3600;
	    }
	    else
	    {
		System.out.println("C");
		/* it's LL with two apertures */
		edge05 = 168.0 / 3600;
		edge04 = 192.0 / 3600;
	    }

	    double pa03 = VisUtil.getPositionAngle(ra0, dec0, ra3, dec3);
	    System.out.println("pa03 = " + pa03);
	    WorldPt pos5 = VisUtil.getNewPosition(ra0, dec0, edge05, pa03);
	    System.out.println("pos5.ra = " + pos5.getLon() + " pos5.dec = " + pos5.getLat());
	    WorldPt pos4 = VisUtil.getNewPosition(ra0, dec0, edge04, pa03);
	    double pa12 = VisUtil.getPositionAngle(ra1, dec1, ra2, dec2);
	    WorldPt pos6 = VisUtil.getNewPosition(ra1, dec1, edge05, pa12);
	    WorldPt pos7 = VisUtil.getNewPosition(ra1, dec1, edge04, pa12);
	    WorldPt left_box[] = new WorldPt[4];
	    left_box[0] = new WorldPt(ra0, dec0);
	    left_box[1] = new WorldPt(ra1, dec1);
	    left_box[2] = new WorldPt(pos6.getLon(), pos6.getLat());
	    left_box[3] = new WorldPt(pos5.getLon(), pos5.getLat());
	    result.add(left_box); 
	    WorldPt right_box[] = new WorldPt[4];
	    right_box[0] = new WorldPt(pos4.getLon(), pos4.getLat());
	    right_box[1] = new WorldPt(pos7.getLon(), pos7.getLat());
	    right_box[2] = new WorldPt(ra2, dec2);
	    right_box[3] = new WorldPt(ra3, dec3);
	    result.add(right_box); 
	}
	return result;
    }

    /**
    *  main is used for testing purposes only.
    *
    */

    public static void main(String args[])
    {
	System.out.println("Entering IRSSlits.java");
	WorldPt[] pt = new WorldPt[4];

	// SL aperture (two) 
	pt[0] = new WorldPt( 286.5444396300, 7.403593210000);
	pt[1] = new WorldPt( 286.5453656200, 7.404054850000);
	pt[2] = new WorldPt( 286.5624751600, 7.370301870000);
	pt[3] = new WorldPt( 286.5615492300, 7.369840270000);
	// SL aperture (one) 
	//pt[0] = new WorldPt( 294.2585640700, 7.582647710000);
	//pt[1] = new WorldPt( 294.2594184200, 7.583179500000);
	//pt[2] = new WorldPt( 294.2679123400, 7.569770470000);
	//pt[3] = new WorldPt( 294.2670580000, 7.569238700000);

	// SH aperture (one) 
	//pt[0] = new WorldPt( 280.4135284000, 61.54476443000);
	//pt[1] = new WorldPt( 280.4132263500, 61.54606203000);
	//pt[2] = new WorldPt( 280.4197743200, 61.54640789000);
	//pt[3] = new WorldPt( 280.4200761000, 61.54511028000);

	// LH aperture (one) 
	//pt[0] = new WorldPt( 337.3452273500, -20.9069561400);
	//pt[1] = new WorldPt( 337.3421849100, -20.9057606700);
	//pt[2] = new WorldPt( 337.3447558400, -20.9000507800);
	//pt[3] = new WorldPt( 337.3477981900, -20.9012462100);

	// LL aperture (one) 
	//pt[0] = new WorldPt( 295.2539154400, 10.92582329000);
	//pt[1] = new WorldPt( 295.2525724700, 10.92842487000);
	//pt[2] = new WorldPt( 295.2949695000, 10.94951985000);
	//pt[3] = new WorldPt( 295.2963121800, 10.94691809000);

	// LL aperture (two) 
	//pt[0] = new WorldPt( 286.5057398900, 7.370726190000);
	//pt[1] = new WorldPt( 286.5046935100, 7.373511370000);
	//pt[2] = new WorldPt( 286.5991892200, 7.408415810000);
	//pt[3] = new WorldPt( 286.6002350900, 7.405630410000);

	// LL aperture (two, near Galectic pole ) 
	//pt[0] = new WorldPt( 192.867  ,  27.1677           );
	//pt[1] = new WorldPt( 192.851371, 27.068672         );
	//pt[2] = new WorldPt( 192.848127, 27.069078         );
	//pt[3] = new WorldPt( 192.863753, 27.168106         );

	// LL aperture (two, near pole ) 
	//pt[0] = new WorldPt(113.302188, 89.960072         );
	//pt[1] = new WorldPt( 117.487781, 89.960048         );
	//pt[2] = new WorldPt( 293.240236, 89.939886         );
	//pt[3] = new WorldPt( 296.021115, 89.939901         );

	System.out.println("Input aperture corners:");
	System.out.println("|ra              |dec                 |");
	for (int i=0; i < 4; i++)
	{
	    System.out.println(pt[i].getLon() + "      " + pt[i].getLat());
	}

	List <WorldPt[]> results = expandIRSAperture(pt);

	System.out.println("Number of apertures returned = " + results.size());

	for (int i=0; i < results.size(); i++)
	{
	    WorldPt one_aperture[] = results.get(i);
	    for (int j=0; j < 4; j++)
	    {
		WorldPt one_corner = one_aperture[j];
		System.out.println("aperture [" + i + "]  corner[" + j + "]  ra = " + one_corner.getLon() + "  dec = " + one_corner.getLat());
	    }
	    System.out.println("ve " + one_aperture[0].getLon() +
		" " + one_aperture[0].getLat() +
		" " + one_aperture[1].getLon() +
		" " + one_aperture[1].getLat());
	    System.out.println("ve " + one_aperture[1].getLon() +
		" " + one_aperture[1].getLat() +
		" " + one_aperture[2].getLon() +
		" " + one_aperture[2].getLat());
	    System.out.println("ve " + one_aperture[2].getLon() +
		" " + one_aperture[2].getLat() +
		" " + one_aperture[3].getLon() +
		" " + one_aperture[3].getLat());
	    System.out.println("ve " + one_aperture[3].getLon() +
		" " + one_aperture[3].getLat() +
		" " + one_aperture[0].getLon() +
		" " + one_aperture[0].getLat());
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
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
