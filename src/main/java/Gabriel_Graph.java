/*
 * To the extent possible under law, the Fiji developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.awt.geom.Point2D;
import java.util.ArrayList;

import ch.biop.epfl.GabrielGraph;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
/**
 * Gabriel Graph class to recover a results table with all neighbors from a Gabriel Graph
 * inspired from http://imagej.1557.x6.nabble.com/Delaunay-Voronoi-intersected-with-boundary-td5016272.html
 *
 * Works in parallel!
 *
 * @author Olivier Burri (from minimal plugin fiji crew!)
 * 
 */
public class Gabriel_Graph implements PlugIn {  

	private boolean is_show_result;
	private boolean is_show_overlay;
	private boolean is_parallel; 

	private ArrayList<Point2D> positions = new ArrayList<Point2D>();

	public void run(String arg) {
		
		final ImagePlus imp = IJ.getImage();
		
		// Expect a PointRoi
		Roi p_roi = imp.getRoi();
		if(p_roi.getType() == Roi.POINT) {
			for(int i = 0; i <p_roi.getFloatPolygon().npoints; i++ )
				positions.add(new Point2D.Double(p_roi.getFloatPolygon().xpoints[i], p_roi.getFloatPolygon().ypoints[i]));	
		}
		if( displayDialog() ) return;

		//IJ.log("Let's begin");

		ResultsTable distances = GabrielGraph.getGabrielGraph(imp, positions, is_show_overlay, is_parallel);

		if( is_show_result) {
			distances.show("Gabriel Graph Results");
		}
	}
	
	private boolean displayDialog() {
		// TODO Auto-generated method stub
		GenericDialog gd = new GenericDialog("Gabriel Graph");
		gd.addCheckbox("Results Table Output", true);
		gd.addCheckbox("Overlay Output", true);
		gd.addCheckbox("Parallel Processing", true);
		gd.showDialog();
		if (gd.wasCanceled())  return true; 								//	to handle cancellation

		is_show_result	= gd.getNextBoolean();											
		is_show_overlay	= gd.getNextBoolean();
		is_parallel		= gd.getNextBoolean();
		return false;

	}  

	/**
	 * Main method for debugging.
	 *
	 * For debugging, it is convenient to have a method that starts ImageJ, loads an
	 * image and calls the plugin, e.g. after setting breakpoints.
	 *
	 * @param args unused
	 */
	public static void main(String[] args) {
		// set the plugins.dir property to make the plugin appear in the Plugins menu
		Class<?> clazz = Gabriel_Graph.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();
		ImagePlus imp = IJ.createImage("Untitled", "8-bit random", 2048, 2048, 1);
		imp.show();
		IJ.run(imp, "Find Maxima...", "noise=80 output=[Point Selection]");


		// run the plugin with
		IJ.runPlugIn(clazz.getName(), "");	//  - dialog



	}
}
