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
																						//	we want that plugin to be able to Name the created ROI with different/custom names
		boolean is_cat = false ;														//	because of parrallel processing we have to declare a the temporary variables 
		String[] temp_cat_names_array = new String[1] ;									//	is_cat and temp_cat_names_array. 
		
		String[] image_list = WindowManager.getImageTitles();							//	check if two images are open
		if (image_list.length < 2)	{
			IJ.error("You need 2 images 'son' !"); // quit so no else required ! 
		}
		GenericDialog gd = new GenericDialog("Parameters");
		gd.addChoice("Display ROIs on", image_list, image_list[0]);
		gd.addChoice("ROIs coordinates", image_list, image_list[1]);
		gd.addStringField("ROIs categories (comma separated)", "");
		gd.showDialog();
		
		if (gd.wasCanceled()) {  
			return ; 
		}
		
		String display_img_name	 	= gd.getNextChoice();
		String roi_coord_img_name 	= gd.getNextChoice();
		String cat_names 			= gd.getNextString();
		
		final ImagePlus 	display_imp 	= WindowManager.getImage(display_img_name);
		final ImagePlus 	roi_coord_imp 	= WindowManager.getImage(roi_coord_img_name);
		
		
		if (!cat_names.isEmpty() && roi_coord_imp.getWidth() > 3 ) {
			IJ.log("cat_names '"	+cat_names +"'"	);
			temp_cat_names_array = cat_names.split(",");
			is_cat = true ;
		}
		
		final boolean use_cat = is_cat;
		final String[] cat_names_array = temp_cat_names_array;
		
		if ( RoiManager.getInstance() == null ){ // " un peu du hack" ! to handle if roiManger is not open ! 
			 new RoiManager();
		} 
		final RoiManager	rm 			=	RoiManager.getInstance();

		/*
		IJ.log("ori:"	+display_imp	);
		IJ.log("input:"	+roi_coord_imp	);
		*/
		final int rows = roi_coord_imp.getHeight(); // number of ROI to create
		//IJ.log("rows:"	+rows	);

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
						int x_center 	= (int) Float.intBitsToFloat(roi_coord_imp.getProcessor().getPixel( 0,  i)) ;	// from the 
						int y_center 	= (int) Float.intBitsToFloat(roi_coord_imp.getProcessor().getPixel( 1,  i)) ;	//
						float diameter 	=  		Float.intBitsToFloat(roi_coord_imp.getProcessor().getPixel( 2,  i)) ; 	// keep it as a float
						float radius	= diameter /2 ; 
						
						String roi_name ;
						if (use_cat){
							int cat_nbr = (int) Float.intBitsToFloat(roi_coord_imp.getProcessor().getPixel( 3,  i)) ; 	// from the iamge containing ROIs coordinates get the index 
							roi_name 	= IJ.pad(i,6)+"-"+cat_names_array[cat_nbr]; 									// here, we use 1st the number of the ROI, then its name from the array cat_names_array
						} else {
							roi_name = "ROI-"+IJ.pad(i,6) ;
						}
						IJ.log("Roi "+i+"="+ x_center +", "+ y_center +", radius = " + radius+ ", name "+roi_name);

						Roi roi = new OvalRoi( (x_center - radius) , (y_center - radius), diameter, diameter) ;
						roi.setName(roi_name);		
						rm.addRoi(roi);				// before creating it
					}
				}};  
		} 


		startAndJoin(threads);  
		long end=System.currentTimeMillis();    
		IJ.log("Processing time convolution in msec: "+(end-start) );

		rm.runCommand("Sort"); 		// the parallel processing scrambles the order of the ROIs, so 
		//rm.runCommand("Show All");
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
		// open the Clown sample
		ImagePlus image = IJ.openImage("http://imagej.net/images/blobs.gif");
		image.show();

		ImagePlus imageParam = IJ.openImage("I:/UsersProjects/Sophie_Wurth/Myelin_Axon_hug_Quantifier/ParalleleFit/TestRois/test_cat.tif");
		imageParam.show();

		// run the plugin
		IJ.runPlugIn(clazz.getName(), "");
	}
}
