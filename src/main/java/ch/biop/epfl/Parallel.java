package ch.biop.epfl;

import java.util.List;
import java.util.LinkedList;

public class Parallel {
	/** Create a Thread[] array as large as the number of processors available. 
	 * From Stephan Preibisch's Multithreading.java class. See: 
	 * http://repo.or.cz/w/trakem2.git?a=blob;f=mpi/fruitfly/general/MultiThreading.java;hb=HEAD 
	 */  
	public static Thread[] newThreadArray() {  
		int n_cpus = Runtime.getRuntime().availableProcessors();  
		return newThreadArray(n_cpus);  
	}  
	
	public static Thread[] newThreadArray(int n_cpus) {  
		return new Thread[n_cpus];  
	}  
	
	public static Thread[] newThreadArray(boolean is_parallel) {
		if (is_parallel) 
			return newThreadArray();
		else
			return newThreadArray(1);
			
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


}
