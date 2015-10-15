package com.github.cc007.clustering;
/**
 * @author Elbert Fliek (s1917188)
 * @author Rik Schaaf (s2391198)
*/

import java.util.*;

public class KMeans extends ClusteringAlgorithm {

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
	static class Cluster {

		float[] prototype;

		Set<Integer> currentMembers;
		Set<Integer> previousMembers;

		public Cluster() {
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

	public KMeans(int k, Vector<float[]> trainData, Vector<float[]> testData, int dim) {
		this.k = k;
		this.trainData = trainData;
		this.testData = testData;
		this.dim = dim;
		prefetchThreshold = 0.5;

		// Here k new cluster are initialized
		clusters = new Cluster[k];
		for (int ic = 0; ic < k; ic++) {
			clusters[ic] = new Cluster();
		}
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
		float closestPrototypeDistance = getDistance((float[]) trainData.get(example), clusters[0].prototype);
		for (int cluster = 1; cluster < k; cluster++) {
			float prototypeDistance = getDistance((float[]) trainData.get(example), clusters[cluster].prototype);
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

	public boolean train() {
		//implement k-means algorithm here:
		// Step 1: Select an initial random partioning with k clusters
		Random rng = new Random();
		for (int example = 0; example < trainData.capacity(); example++) {
			clusters[rng.nextInt(k)].currentMembers.add(example);
		}

		/// keep iterating untill no changes occur
		boolean change = true;
		while (change == true) {
			change = false;
			for (int cluster = 0; cluster < k; cluster++) {
				///  calculate cluster centers
				clusters[cluster].prototype = calculatePrototype(cluster);
			}
			// Step 2: Generate a new partition by assigning each datapoint to its closest cluster center
			for (int cluster = 0; cluster < k; cluster++) {
				for (int member = 0; member < clusters[cluster].currentMembers.size(); member++) {
					int closestCluster = getClosestCluster(member);
					if (closestCluster != cluster) {
						/// remove from old cluster and add to new cluster
						clusters[cluster].currentMembers.remove(member);
						clusters[cluster].previousMembers.add(member);
						clusters[closestCluster].currentMembers.add(member);
						/// if something changed during this iteration, continue iterating
						if (!clusters[closestCluster].previousMembers.contains(member)) {
							change = true;
						}
					}
				}
			}
		}
		return false;
	}

	public boolean test() {

		/// initialize counters to 0
		int hits = 0;
		int prefetched = 0;
		int requests = 0;

		// iterate along all clients. Assumption: the same clients are in the same order as in the testData
		// for each client find the cluster of which it is a member
		Iterator<Integer> iter;
		for (int i = 0; i < this.k; i++) {
			for (int client : this.clusters[i].currentMembers) {
				///  get the actual testData (the vector) of this client
				float[] test = this.testData.get(clientID);
				for (int j = 0; j < this.dim; j++) {
					if (this.clusters[i].prototype[j] > this.prefetchThreshold) {
						/// and count prefetched htmls
						prefetched++;
						if (Math.round(test[j]) == 1) {
							/// count number of hits
							hits++;
						}
					}
					if (Math.round(test[j]) == 1) {
						///count number of requests
						requests++;
					}
				}
			}
		}

		// set the global variables hitrate and accuracy to their appropriate value
		this.hitrate = ((double)hits / requests);
		this.accuracy = ((double)hits / prefetched);
		return true;
	}

	// The following members are called by RunClustering, in order to present information to the user
	public void showTest() {
		System.out.println("Prefetch threshold=" + this.prefetchThreshold);
		System.out.println("Hitrate: " + this.hitrate);
		System.out.println("Accuracy: " + this.accuracy);
		System.out.println("Hitrate+Accuracy=" + (this.hitrate + this.accuracy));
	}

	public void showMembers() {
		for (int i = 0; i < k; i++) {
			System.out.println("\nMembers cluster[" + i + "] :" + clusters[i].currentMembers);
		}
	}

	public void showPrototypes() {
		for (int ic = 0; ic < k; ic++) {
			System.out.print("\nPrototype cluster[" + ic + "] :");

			for (int ip = 0; ip < dim; ip++) {
				System.out.print(clusters[ic].prototype[ip] + " ");
			}

			System.out.println();
		}
	}

	// With this function you can set the prefetch threshold.
	public void setPrefetchThreshold(double prefetchThreshold) {
		this.prefetchThreshold = prefetchThreshold;
	}
}
