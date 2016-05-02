/*
 * To the extent possible under law, the Fiji developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import ch.biop.epfl.Parallel;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.Line;
import ij.gui.OvalRoi;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ch.biop.epfl.Parallel;
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

		ResultsTable distances = getGabrielGraph(imp, positions, is_show_overlay, is_parallel);

		if( is_show_result) {
			distances.show("Gabriel Graph Results");
		}
	}
	
	
	private ResultsTable getGabrielGraph(final ImagePlus imp, final ArrayList<Point2D> positions, boolean is_show_overlay, boolean is_parallel) {
		// Prepare parallel processing, each core will process one point
		
		final AtomicInteger ap = new AtomicInteger(0);
		final int n = positions.size();
		final Overlay ov = new Overlay();
		final ArrayList<ResultsTable> frt = new ArrayList<ResultsTable>(n);

		final	Thread[] 		threads = Parallel.newThreadArray(is_parallel);		// create the thread array

		for (int ithread = 0; ithread < threads.length; ithread++) {  				// declare each thread 

			threads[ithread] = new Thread() {  

				{ setPriority(Thread.NORM_PRIORITY); }  

				public void run() {  

					double d = 0;
					Point2D.Double c = new Point2D.Double();

					// Each thread processes a few items in the total list  
					// Each loop iteration within the run method  
					// has a unique 'i' number to work with  
					// and to use as index in the results array:  
					for (int i=ap.getAndIncrement(); i<n; i = ap.getAndIncrement()) {
						ResultsTable rt = new ResultsTable();

						for (int j=i+1; j<n; j++) {
							// Diameter
							d = positions.get(j).distance(positions.get(i));

							// Center
							c.setLocation( (positions.get(j).getX()+positions.get(i).getX())/2, (positions.get(j).getY()+positions.get(i).getY())/2 );

							boolean nope = false;

							// Check that there is no other point inside
							for (int k=0; k<n; k++) {
								if(k!=i && k!=j) {
									if(positions.get(k).distance(c) < d/2) {
										nope = true;
										break;
									}
								}
							}
							if (!nope){
								if (is_show_overlay)
									ov.add(new Line(positions.get(j).getX(), positions.get(j).getY(), positions.get(i).getX(), positions.get(i).getY()));
								rt.incrementCounter();
								rt.addValue("Point 1", i);
								rt.addValue("Point 2", j);
								rt.addValue("X Point 1", positions.get(i).getX());
								rt.addValue("Y Point 1", positions.get(i).getY());
								rt.addValue("X Point 2", positions.get(j).getX());
								rt.addValue("Y Point 2", positions.get(j).getY());
								rt.addValue("Distance",d);

							}
						}
						if (rt.getCounter() > 0)
							frt.add(rt);
					}
				} 
			};
		}

		long start	= System.currentTimeMillis();						// to measure the time required

		Parallel.startAndJoin(threads);				// DO THE MAGIC ! 

		long end=System.currentTimeMillis(); 
		IJ.log("Execution time: "+(end-start)+" ms" );		

		
		// show overlay on image
		if(is_show_overlay) {
			imp.setOverlay(ov);
		}

		// Sort results
		Collections.sort(frt, new Comparator<ResultsTable>() {
			@Override
			public int compare(ResultsTable o1, ResultsTable o2) {
				return (int) (o1.getValue("Point 1", 0) - o2.getValue("Point 1", 0));
			}
		});

		
		// Cleanup results
		final ResultsTable rt = new ResultsTable();

		// Flatten to one results table
		for( ResultsTable r : frt) {
			for(int i=0; i<r.getCounter(); i++) {
				rt.incrementCounter();
				for (int j=0; j<=r.getLastColumn(); j++) {
					rt.addValue(j, r.getValueAsDouble(j, i));
				}
			}
		}
		// Recover headings
		for(int i=0; i<=rt.getLastColumn(); i++) {
			String str = frt.get(0).getColumnHeading(i);
			rt.setHeading(i, str);
		}

		return rt;
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
