package ch.biop.epfl;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.measure.ResultsTable;

public class GabrielGraph {
	
	public static ResultsTable getGabrielGraph(final ImagePlus imp, final ArrayList<Point2D> positions, final boolean is_show_overlay, final boolean is_parallel) {
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
}
