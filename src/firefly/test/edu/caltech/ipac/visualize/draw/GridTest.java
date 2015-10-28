package edu.caltech.ipac.visualize.draw;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.WorldPt;
import junit.framework.Assert;
import nom.tam.fits.Fits;

public class GridTest {

	private static Component grid = null;
	private static String filename = "./bad.fits";
	static CoordinateSys csys = CoordinateSys.SUPERGALACTIC;
	private static ImagePlot plot;

	@BeforeClass
	public static void buildPlot() throws Exception {

		grid = new GridLayer(csys).getGrid();
		Fits fits = new Fits(filename);
		// fits = new Fits("./good.fits");

		FitsRead[] frAry = FitsRead.createFitsReadArray(fits);
		ActiveFitsReadGroup fg = new ActiveFitsReadGroup();
		fg.setFitsRead(Band.RED, frAry[0]);
		plot = new ImagePlot(null, fg, 0, false, Band.RED, 0, new RangeValues());
		// plot.preProcessImageTiles(fg);
		((Grid) grid).setPlot(plot);

	}

	@Test
	public void testCenterGal() throws Exception {
		// buildPlot();
		double sharedLon = 0.0;
		double sharedLat = 0.0;// 458.22963691644395;-20.42265552817031 pixels
		// Assert.assertTrue(plot.pointInPlot(new WorldPt(sharedLon, sharedLat,
		// CoordinateSys.GALACTIC)));
		sharedLon = 266.5;
		sharedLat = -28.8;

		Assert.assertTrue(plot.pointInPlot(new WorldPt(sharedLon, sharedLat, CoordinateSys.EQ_J2000)));

		sharedLon = 0;
		sharedLat = 9.004616421917772E-4;
		WorldPt wpt = new WorldPt(sharedLon, sharedLat, CoordinateSys.GALACTIC);
		Assert.assertFalse(plot.pointInPlot(wpt));

		Assert.assertFalse(plot.pointInPlot(VisUtil.convertToJ2000(wpt)));
	}

	// @Override
	// public String getModuleName() {
	// return "edu.caltech.ipac.firefly.FireFly";
	// }
	public static void main(String[] a) {
		JFrame window = new JFrame();
		try {
			buildPlot();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JButton galButton = new JButton("Galactic");
		JButton eqButton = new JButton("Eq2000");
		eqButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				((Grid) grid).setCoordSystem(CoordinateSys.EQ_J2000);
				window.getContentPane().repaint();
				window.setTitle(filename + "[" + ((Grid) grid).getCoordSystem().toString() + "]");
			}
		});
		galButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				((Grid) grid).setCoordSystem(CoordinateSys.GALACTIC);
				window.getContentPane().repaint();
				window.setTitle(filename + "[" + ((Grid) grid).getCoordSystem().toString() + "]");
			}
		});
		window.getContentPane().setLayout(new BorderLayout());
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setBounds(30, 30, 600, 600);
		window.setTitle(filename + "[" + csys.toString() + "]");
		window.getContentPane().add(grid, BorderLayout.CENTER);
		window.getContentPane().add(galButton, BorderLayout.WEST);
		window.getContentPane().add(eqButton, BorderLayout.EAST);
		window.setVisible(true);
	}

	@Test
	public void testSimple() { // <span style="color:black;">**(3)**</span>
		org.junit.Assert.assertTrue(true);
	}
}
