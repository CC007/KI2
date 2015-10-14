package com.github.cc007.clustering;
import java.util.*;

public class KMeans extends ClusteringAlgorithm
{
	// Number of clusters
	private int k;

	// Dimensionality of the vectors
	private int dim;
	
	// Threshold above which the corresponding html is prefetched
	private double prefetchThreshold;
	
	// Array of k clusters, class cluster is used for easy bookkeeping
	private Cluster[] clusters;
	
	// This class represents the clusters, it contains the prototype (the mean of all it's members)
	// and memberlists with the ID's (which are Integer objects) of the datapoints that are member of that cluster.
	// You also want to remember the previous members so you can check if the clusters are stable.
	static class Cluster
	{
		float[] prototype;

		Set<Integer> currentMembers;
		Set<Integer> previousMembers;
		  
		public Cluster()
		{
			currentMembers = new HashSet<Integer>();
			previousMembers = new HashSet<Integer>();
		}
	}
	// These vectors contains the feature vectors you need; the feature vectors are float arrays.
	// Remember that you have to cast them first, since vectors return objects.
	private Vector<float[]> trainData;
	private Vector<float[]> testData;

	// Results of test()
	private double hitrate;
	private double accuracy;
	
	public KMeans(int k, Vector<float[]> trainData, Vector<float[]> testData, int dim)
	{
		this.k = k;
		this.trainData = trainData;
		this.testData = testData; 
		this.dim = dim;
		prefetchThreshold = 0.5;
		
		// Here k new cluster are initialized
		clusters = new Cluster[k];
		for (int ic = 0; ic < k; ic++)
			clusters[ic] = new Cluster();
	}

// return the distance between two examples
    private float getDistance(float[] exampleA, float[] exampleB) {
        float distance = 0;
        for (int i = 0; i < exampleA.length; i++) {
            distance = distance + (float) Math.pow(exampleA[i] - exampleB[i], 2);
        }
        return (float) Math.sqrt(distance);
    }

    // finds the closest cluster mean for an example
    private int getClosestCluster(int example) {
        int closestCluster = 0;
        float closestPrototypeDistance = getDistance( (float[]) trainData.get(example), clusters[0].prototype);
        for (int cluster = 1; cluster < k; cluster++) {
            float prototypeDistance = getDistance( (float[]) trainData.get(example), clusters[cluster].prototype);
            if (prototypeDistance < closestPrototypeDistance) {
                closestCluster = cluster;
                closestPrototypeDistance = prototypeDistance;
            }
        }
        return closestCluster;
    }

    // calculate the prototype for a cluster
    private float[] calculatePrototype(int cluster) {
        float[] prototype = new float[trainData.get(0).length];
        int numMembers = clusters[cluster].currentMembers.size();
        for (int memberId = 0; memberId < numMembers; memberId++) {
            float[] member = trainData.get(memberId);
            for (int i = 0; i < prototype.length; i++) {
                prototype[i] = prototype[i] + member[i];
            }
        }
        for (int i = 0; i < prototype.length; i++) {
            prototype[i] = prototype[i] / numMembers;
        }
        return prototype;
    }

	public boolean train()
	{
	 	//implement k-means algorithm here:
		// Step 1: Select an initial random partioning with k clusters
		Random rng = new Random();
		for (int example = 0; example < trainData.capacity(); example++) {
		    clusters[rng.nextInt(k)].currentMembers.add(example);
		}
        
		boolean change = true;
		while (change == true) {
		    change = false;
		    for (int cluster = 0; cluster < k; cluster++) {
			clusters[cluster].prototype = calculatePrototype(cluster);
		    }
		    // Step 2: Generate a new partition by assigning each datapoint to its closest cluster center
		    for (int cluster = 0; cluster < k; cluster++) {
			for (int member = 0; member < clusters[cluster].currentMembers.size(); member++) {
			    int closestCluster = getClosestCluster(member);
			    if (closestCluster != cluster) {
				clusters[cluster].currentMembers.remove(member);
				clusters[cluster].previousMembers.add(member);
				clusters[closestCluster].currentMembers.add(member);
				if (clusters[closestCluster].previousMembers.contains(member)) {
				    change = false;
				}
			    }
			}
		    }
		}
		// Step 3: recalculate cluster centers
		// Step 4: repeat until clustermembership stabilizes
		return false;
	}

	public boolean test()
	{
		// iterate along all clients. Assumption: the same clients are in the same order as in the testData
		// for each client find the cluster of which it is a member
		// get the actual testData (the vector) of this client
		// iterate along all dimensions
		// and count prefetched htmls
		// count number of hits
		// count number of requests
		// set the global variables hitrate and accuracy to their appropriate value
        int hits = 0;
        int prefetched = 0;
        int requests = 0;
        Iterator iter;
        for (int i = 0; i < this.k; i++)
        {
          for (iter = this.clusters[i].currentMembers.iterator(); iter.hasNext(); )
          {
            int clientID = ((Integer)iter.next()).intValue();
            float[] test = (float[])this.testData.get(clientID);
            for (int i2 = 0; i2 < this.dim; i2++)
            {
              if (this.clusters[i].prototype[i2] > this.prefetchThreshold)
              {
                prefetched++;
                if ((int)Math.rint(test[i2]) == 1)
                  hits++;
              }
              if ((int)Math.rint(test[i2]) == 1) {
                requests++;
              }
            }
          }

        }

        this.hitrate = (hits / requests);
        this.accuracy = (hits / prefetched);
		return true;
	}


	// The following members are called by RunClustering, in order to present information to the user
	public void showTest()
	{
		System.out.println("Prefetch threshold=" + this.prefetchThreshold);
		System.out.println("Hitrate: " + this.hitrate);
		System.out.println("Accuracy: " + this.accuracy);
		System.out.println("Hitrate+Accuracy=" + (this.hitrate + this.accuracy));
	}
	
	public void showMembers()
	{
		for (int i = 0; i < k; i++)
			System.out.println("\nMembers cluster["+i+"] :" + clusters[i].currentMembers);
	}
	
	public void showPrototypes()
	{
		for (int ic = 0; ic < k; ic++) {
			System.out.print("\nPrototype cluster["+ic+"] :");
			
			for (int ip = 0; ip < dim; ip++)
				System.out.print(clusters[ic].prototype[ip] + " ");
			
			System.out.println();
		 }
	}

	// With this function you can set the prefetch threshold.
	public void setPrefetchThreshold(double prefetchThreshold)
	{
		this.prefetchThreshold = prefetchThreshold;
	}
}
