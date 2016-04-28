/*
 * To the extent possible under law, the Fiji developers have waived
 * all copyright and related or neighboring rights to this tutorial code.
 *
 * See the CC0 1.0 Universal license for details:
 *     http://creativecommons.org/publicdomain/zero/1.0/
 */

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;

/**
 * Parallel_Roi_Creator
 *
 * A pluggin that use and image to create at a define position, a circular ROIs with specified diameter and names (or a point), in a parallel way!
 *
 * @author Romain Guiet (from minimal plugin fiji crew! AND A LOT of help from Olivier Burri)
 */
public class Parallel_Roi_Creator implements PlugIn {  

	public void run(String arg) {
		//	we want that plugin to retrieve informations from the current image to create ROIs
		//	each row corresponds to a ROI
		//	each column contains informations 
		// 		column 0 : x 
		//		column 1 : y
		//		column 2 : diameter ( if equal to NaN make a point instead of a circle)
		//		column 3 : an index that defines the name (from a list provided at the start of the plugin)
		final ImagePlus roi_coord_imp 	= IJ.getImage();									//	get the active image
		final int rows = roi_coord_imp.getHeight();											//	the number of ROI to create corresponds to the number of row

		boolean		is_custom_name = false ;												//	because of parallel processing we have to declare a the temporary variables 
		String[] 	temp_suffix_array = new String[1] ;										//	is_custom_name , temp_suffix_array and temp_prefix
		String 		temp_prefix = "" ;

		if (roi_coord_imp.getWidth() > 3 ){													//	if the image contains more than 3 columns
			GenericDialog gd = new GenericDialog("Parameters");								//	Create a generic dialog 
			gd.addStringField("ROIs_prefix (limited to 1)", "");							//	get  1 prefix
			gd.addStringField("ROIs_suffixes (list separated by comma)", "");			//	get a list of suffixes (the pixel value should correspond to the position in the comma separated list above.)
			gd.addMessage("NB : prefix and suffix should be more than 4 characters long");
			gd.showDialog();																//	
			if (gd.wasCanceled())  return ; 												//	to handle cancellation

					temp_prefix	= gd.getNextString();										//	get the entered prefix String.
			String	temp_suffix	= gd.getNextString();										//	get the entered suffix String.

			if ( !temp_suffix.isEmpty() ) {													//	if temp_suffix is not empty 
				temp_suffix_array = temp_suffix.split(",");									//	we split the string into an array
				is_custom_name = true ;														//	and defines is_custom_name as true
			}
		}
		final boolean 	use_custom_name 	= is_custom_name;								//	make the final variables from the temporary ones
		final String 	prefix 		= temp_prefix;											//
		final String[]	suffix_array= temp_suffix_array;									// 				
														
		if ( RoiManager.getInstance() == null )	new RoiManager(); 							//  if the ROI Manager is not open, open it!
		final RoiManager	rm 	=	RoiManager.getInstance();								//	and get it as rm

		final	AtomicInteger 	ai 		= new AtomicInteger(0);								// for parallel processing, we need the ai
		long					start	= System.currentTimeMillis();						// use this to measure the time required for the processing
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
																														// RETRIEVE informations
						int x_center 	= (int) Float.intBitsToFloat(roi_coord_imp.getProcessor().getPixel( 0,  i)) ;	// get x (pixel are float , convert it to int  )
						int y_center 	= (int) Float.intBitsToFloat(roi_coord_imp.getProcessor().getPixel( 1,  i)) ;	// and y 
						float diameter 	=  		Float.intBitsToFloat(roi_coord_imp.getProcessor().getPixel( 2,  i)) ; 	// diameter is a float keep it as a float
						float radius	= diameter /2 ; 																// define radius	
						
						
						Roi roi;																						// CREATE THE ROI
						if (Float.isNaN(diameter)){																		// -TYPE : if diameter is NaN make a point otherwise a circle 
							roi = new PointRoi( x_center , y_center) ;													//	defines the ROI as a point
						}else{																							//  otherwise
							roi = new OvalRoi( (x_center - radius) , (y_center - radius), diameter, diameter) ;			//	defines the ROI as a circle
						}
						
						String roi_name ;																				// - NAME:																			
						if (use_custom_name){																			// if we use_custom_name from column
							int suffix_index = (int) Float.intBitsToFloat(roi_coord_imp.getProcessor().getPixel( 3,  i)) ; 	// from the image get the suffix_index
							roi_name 	= prefix+"-"+IJ.pad((i+1),6)+"-"+suffix_array[suffix_index]; 					// prefix- number of the ROI- suffix , for efficient end sorting.
						}else{
							roi_name = "ROI-"+IJ.pad(i,6);																// otherwise by default the name will be ROI-nbr
						}
						//IJ.log("Roi "+i+"="+ x_center +", "+ y_center +", radius = " + radius+ ", name "+roi_name);
						roi.setName(roi_name);																			//	Set the name	
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
		ImagePlus  image = IJ.openImage("http://imagej.net/images/blobs.gif");						// open blobs
		
		IJ.run(image, "Gaussian Blur...", "sigma=1");												// blur and invert
		IJ.run(image, "Invert LUT", "");
		image.show();
		
		IJ.run(image, "Find Maxima...", "noise=50 output=[Point Selection]");						// find local maxima on blobs and get their coordinates
		Roi all_local_maxima_roi = (PointRoi) image.getRoi();
		int[] x_coord = all_local_maxima_roi.getPolygon().xpoints;
		int[] y_coord = all_local_maxima_roi.getPolygon().ypoints;
		
		int[] mean_intensity = new int[x_coord.length];												// for each local maxima store the pixel intensities in an array
		for (int  i = 0; i < x_coord.length; i++) {
			mean_intensity[i] = (int) image.getProcessor().getPixel( x_coord[i],  y_coord[i]) ;
		}
		
		
		ImagePlus imageParam = IJ.createImage("imageParam", "8-bit black", 4, x_coord.length, 1);	// make a new image 
		Random rd = new Random();																	
		for (int  i = 0; i < x_coord.length; i++) {													// store the parameters in each column 	
			imageParam.getProcessor().set(0,i, x_coord[i]					);
			imageParam.getProcessor().set(1,i, y_coord[i]					);
			imageParam.getProcessor().set(2,i, (mean_intensity[i] / 5)		);
			imageParam.getProcessor().set(3,i, rd.nextInt(3) 				);
		}
		IJ.run(imageParam, "32-bit", "");															// convert the image to a 32-bit
		imageParam.show();
		
		
																									// run the plugin with
		//IJ.runPlugIn(clazz.getName(), "");																//  - dialog
		IJ.run("Parallel Roi Creator", "rois_prefix=metaroi rois_suffixes=category1,category2,category3");	//  - predefined roiNames
		//IJ.runPlugIn(imageParam, clazz.getName(), "rois_prefix=[metaroi] rois_suffixes=[category1,category2,category3]"); // doesn't work and we do not know why! 
		
	
			
	}
}
