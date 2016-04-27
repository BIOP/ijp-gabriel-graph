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
 * A pluggin that use and image to create at a define position, a circular ROIs with specified diameter and names, in a parallel way!
 *
 * @author Romain Guiet (from minimal plugin fiji crew! AND A LOT of help from Olivier Burri)
 */
public class Parallel_Roi_Creator implements PlugIn {  

	public void run(String arg) {
		//	we want that pluggin to retrieve informations from the current image to create circular ROIs
		//	each row corresponds to a ROI
		//	each column contains informations 
		// 		column 0 : x 
		//		column 1 : y
		//		column 2 : diameter
		//		column 3 : an index that defines the name (from a list)
		final ImagePlus roi_coord_imp 	= IJ.getImage();									//	get the active image
		final int rows = roi_coord_imp.getHeight();											//	the number of ROI to create corresponds to the number of row


		boolean		is_cat = false ;														//	because of parallel processing we have to declare a the temporary variables 
		String[] 	temp_cat_names_array = new String[1] ;									//	is_cat and temp_cat_names_array. 

		if (roi_coord_imp.getWidth() > 3 ){													//	if the image contains more than 3 columns
			GenericDialog gd = new GenericDialog("Parameters");								//	Create a generic dialog 
			gd.addStringField("ROIs categories (comma separated, more than 4 charac.)", "");//	to get the index of the names of the ROIs 
			gd.showDialog();																//	(the pixel value should correspond to the position in the comma separated list above.)
			if (gd.wasCanceled())  return ; 												//	to handle cancellation

			String	cat_names	= gd.getNextString();										//	get the entered String.
			if ( !cat_names.isEmpty() ) {													//	if it's not empty and the image contains more than 3 columns
				temp_cat_names_array = cat_names.split(",");								//	we split the string into an array
				is_cat = true ;																//	and defines is_cat as true
			}
		}
		final boolean use_cat = is_cat;														//	make the final variables from the temporary ones
		final String[] cat_names_array = temp_cat_names_array;								// 				

		if ( RoiManager.getInstance() == null )	new RoiManager(); 							//  if the ROI Manager is not open, open it!
		final RoiManager	rm 	=	RoiManager.getInstance();								//	and get it as rm

		final	AtomicInteger 	ai 		= new AtomicInteger(0);								// for parallel processing, we need the ai
		long					start	= System.currentTimeMillis();						// to measure the time required
		final	Thread[] 		threads = newThreadArray();  								// create the thread array

		for (int ithread = 0; ithread < threads.length; ithread++) {  						// 

			// Concurrently run in as many threads as CPUs  

			threads[ithread] = new Thread() {  

				{ setPriority(Thread.NORM_PRIORITY); }  

				public void run() {  

					// Each thread processes a few items in the total list  
					// Each loop iteration within the run method  
					// has a unique 'i' number to work with  
					// and to use as index in the results array:  


					for (int i = ai.getAndIncrement(); i < rows; i = ai.getAndIncrement()) {  
						int x_center 	= (int) Float.intBitsToFloat(roi_coord_imp.getProcessor().getPixel( 0,  i)) ;	// get x (pixel are float , convert it to int  )
						int y_center 	= (int) Float.intBitsToFloat(roi_coord_imp.getProcessor().getPixel( 1,  i)) ;	// and y 
						float diameter 	=  		Float.intBitsToFloat(roi_coord_imp.getProcessor().getPixel( 2,  i)) ; 	// diameter is a float keep it as a float
						float radius	= diameter /2 ; 																// define radius	

						String roi_name = "ROI-"+IJ.pad(i,6) ;															// otherwise by default the name will be ROI-nbr																			
						if (use_cat){																					// but if we use cat_name from column
							int cat_nbr = (int) Float.intBitsToFloat(roi_coord_imp.getProcessor().getPixel( 3,  i)) ; 	// from the image get the index in the list of name
							roi_name 	= roi_name+"-"+cat_names_array[cat_nbr]; 										// here, we use 1st the number of the ROI, then its name beacauzse of sorting at the end
						}
						//IJ.log("Roi "+i+"="+ x_center +", "+ y_center +", radius = " + radius+ ", name "+roi_name);

						Roi roi = new OvalRoi( (x_center - radius) , (y_center - radius), diameter, diameter) ;			//	define the roi
						roi.setName(roi_name);																			//	and set its name	
						rm.addRoi(roi);																					//  before creating it
					}
				}};  
		} 


		startAndJoin(threads);				// DO THE MAGIC ! 

		long end=System.currentTimeMillis(); 
		IJ.log("ROI creation time in msec: "+(end-start) );

		rm.runCommand("Sort"); 		// the parallel processing scrambles the order of the ROIs, so we sort them here.
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
		// open the blobs sample
		ImagePlus image = IJ.openImage("http://imagej.net/images/blobs.gif");
		image.show();

		ImagePlus imageParam = IJ.openImage("I:/UsersProjects/Sophie_Wurth/Myelin_Axon_hug_Quantifier/ParalleleFit/TestRois/test_cat.tif");
		imageParam.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
