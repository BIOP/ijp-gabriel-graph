/*
 * To the extent possible under law, the Fiji developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.concurrent.atomic.AtomicInteger;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.frame.RoiManager;
import ij.process.ImageProcessor;

/**
 * Parallel_Roi_Creator
 *
 * A plugin to create ROIs in a parallel way.
 *
 * @author Romain Guiet (from minimal plugin fiji crew! )
 */
public class Parallel_Roi_Creator implements PlugIn {  

	public void run(String arg) {  
		final RoiManager	rm 			= 	RoiManager.getInstance();
		final ImagePlus 	imp_ori 	= 	WindowManager.getImage(1);
		final ImagePlus 	imp_input 	= 	WindowManager.getImage(2);
		IJ.log("ori:"	+imp_ori	);
		IJ.log("input:"	+imp_input	);

		final int rows = imp_input.getHeight();
		IJ.log("rows:"	+rows	);

		final AtomicInteger ai = new AtomicInteger(0);

		long start=System.currentTimeMillis();

		final Thread[] threads = newThreadArray();  

		for (int ithread = 0; ithread < threads.length; ithread++) {  

			// Concurrently run in as many threads as CPUs  

			threads[ithread] = new Thread() {  

				{ setPriority(Thread.NORM_PRIORITY); }  

				public void run() {  

					// Each thread processes a few items in the total list  
					// Each loop iteration within the run method  
					// has a unique 'i' number to work with  
					// and to use as index in the results array:  


					for (int i = ai.getAndIncrement(); i < rows; i = ai.getAndIncrement()) {  
						int x_center 	= (int) Float.intBitsToFloat(imp_input.getProcessor().getPixel( 0,  i)) ;
						int y_center 	= (int) Float.intBitsToFloat(imp_input.getProcessor().getPixel( 1,  i)) ;
						float diameter 	=  		Float.intBitsToFloat(imp_input.getProcessor().getPixel( 2,  i)) ; // keep it as a float
						float radius	= diameter /2 ; 
						//int cat_nbr 	= (int) Float.intBitsToFloat(imp_input.getProcessor().getPixel( 4,  i)) ;

						//IJ.log("Roi "+i+"="+ x_center +", "+ y_center +", radius = " + radius);

						Roi roi = new OvalRoi( (x_center - radius) , (y_center - radius), diameter, diameter) ;

						roi.setName("ROI-"+IJ.pad(i,6) );
						rm.addRoi(roi);							
					}
				}};  
		} 


		startAndJoin(threads);  
		long end=System.currentTimeMillis();    
		IJ.log("Processing time convolution in msec: "+(end-start) );

		rm.runCommand("Sort");

	}  


	/** Create a Thread[] array as large as the number of processors available. 
	 * From Stephan Preibisch's Multithreading.java class. See: 
	 * http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD 
	 */  
	private Thread[] newThreadArray() {  
		int n_cpus = Runtime.getRuntime().availableProcessors();  
		return new Thread[n_cpus];  
	}  

	/** Start all given threads and wait on each of them until all are done. 
	 * From Stephan Preibisch's Multithreading.java class. See: 
	 * http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD 
	 */  
	public static void startAndJoin(Thread[] threads)  
	{  
		for (int ithread = 0; ithread < threads.length; ++ithread)  
		{  
			threads[ithread].setPriority(Thread.NORM_PRIORITY);  
			threads[ithread].start();  
		}  

		try  
		{     
			for (int ithread = 0; ithread < threads.length; ++ithread)  
				threads[ithread].join();  
		} catch (InterruptedException ie)  
		{  
			throw new RuntimeException(ie);  
		}  
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
		Class<?> clazz = Parallel_Roi_Creator.class;
		String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
		String pluginsDir = url.substring("file:".length(), url.length() - clazz.getName().length() - ".class".length());
		System.setProperty("plugins.dir", pluginsDir);

		// start ImageJ
		new ImageJ();
		RoiManager rm = new RoiManager();
		rm.setVisible(true);
		// open the Clown sample
		ImagePlus image = IJ.openImage("http://imagej.net/images/blobs.gif");
		image.show();

		ImagePlus imageParam = IJ.openImage("I:/UsersProjects/Sophie_Wurth/Myelin_Axon_hug_Quantifier/ParalleleFit/TestRois/test.tif");
		imageParam.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
