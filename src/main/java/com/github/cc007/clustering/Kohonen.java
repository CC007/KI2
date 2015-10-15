package com.github.cc007.clustering;

import java.util.*;
import java.util.stream.DoubleStream;

/**
 * @author Elbert Fliek (s1917188)
 * @author Rik Schaaf (s2391198)
*/
public class Kohonen extends ClusteringAlgorithm {

	// Size of clustersmap
	private int n;

	// Number of epochs
	private int epochs;

	// Dimensionality of the vectors
	private int dim;

	// Threshold above which the corresponding html is prefetched
	private double prefetchThreshold;

	private double initialLearningRate;

	// This class represents the clusters, it contains the prototype (the mean of all it's members)
	// and a memberlist with the ID's (Integer objects) of the datapoints that are member of that cluster.  
	private Cluster[][] clusters;

	// Vector which contains the train/test data
	private Vector<float[]> trainData;
	private Vector<float[]> testData;

	// Results of test()
	private double hitrate;
	private double accuracy;

	static class Cluster {

		float[] prototype;

		Set<Integer> currentMembers;

		public Cluster() {
			currentMembers = new HashSet<Integer>();
		}
	}

	public Kohonen(int n, int epochs, Vector<float[]> trainData, Vector<float[]> testData, int dim) {
		this.n = n;
		this.epochs = epochs;
		prefetchThreshold = 0.5;
		initialLearningRate = 0.8;
		this.trainData = trainData;
		this.testData = testData;
		this.dim = dim;

		Random rnd = new Random();

		// Here n*n new cluster are initialized
		clusters = new Cluster[n][n];
		for (int i = 0; i < n; i++) {
			for (int i2 = 0; i2 < n; i2++) {
				clusters[i][i2] = new Cluster();
				clusters[i][i2].prototype = new float[dim];
				for (int i3 = 0; i3 < 10; i3++) {
					clusters[i][i2].prototype[i3] = rnd.nextFloat();
				}
			}
		}
	}

	/// calculate the euclidean distance by using the square root of the sum of squares
	private float euclideanDistance(float[] a, float[] b) {
		double[] diffSq = new double[a.length];
		for (int i = 0; i < diffSq.length; i++) {
			diffSq[i] = Math.pow(a[i] - b[i], 2);
		}
		return (float) Math.sqrt(DoubleStream.of(diffSq).sum());
	}

	/// return the current learning rate based on the current epoch
	private float currentLearingRate(int epoch) {
		return (float) (initialLearningRate * (1 - (float) epoch / epochs));
	}

	/// return the current neigborhood size based on the current epoch
	private float currentNeighborhoodSize(int epoch) {
		return n / 2 * (1 - epoch / epochs);
	}

	/// return the neighborhood of a cluster based on the epoch number
	private Vector<float[]> getNeighborhood(Cluster cluster, int epoch) {
		float radius = currentNeighborhoodSize(epoch);
		Vector neighborhood = new Vector();
		for (float[] trainData1 : trainData) {
			/// add only to the neighborhood if the euclidean distance to the prototype is smaller than the radius
			if (euclideanDistance(cluster.prototype, trainData1) < radius) {
				neighborhood.add(trainData1);
			}
		}
		return neighborhood;
	}

	/// calculate the mean vector of a group of vectors
	private float[] meanVector(Vector<float[]> vectors) {
		float[] result = new float[vectors.get(0).length];
		/// add all vectors to the result vector
		for (float[] vector : vectors) {
			for (int i = 0; i < vector.length; i++) {
				result[i] += vector[i];
			}
		}
		
		/// divide the result vector by the amount of vectors
		for (int i = 0; i < result.length; i++) {
			result[i] /= vectors.size();
		}
		return result;
	}

	/// calculate the new prototype based on the vectors in the neigborhood of the prototype.
	private void updatePrototype(Cluster cluster, Vector<float[]> neighborhood, int epoch) {
		float learningRate = currentLearingRate(epoch);
		float[] mean = meanVector(neighborhood);
		
		for (int i = 0; i < cluster.prototype.length; i++) {
			cluster.prototype[i] = (1 - learningRate) * cluster.prototype[i] + learningRate * mean[i];
		}
	}

	public boolean train() {
		/// loop over all epochs
		for (int epoch = 0; epoch < epochs; epoch++) {
			/// loop over all members
			for (int member = 0; member < trainData.size(); member++) {
				float[] trainData1 = trainData.get(member);
				float closest = Float.MAX_VALUE;
				int closestI = 0;
				int closestJ = 0;

				/// find the closest cluster using euclidean distance
				for (int i = 0; i < clusters.length; i++) {
					Cluster[] cluster = clusters[i];
					for (int j = 0; j < cluster.length; j++) {
						Cluster cluster1 = cluster[j];
						float distance = euclideanDistance(cluster1.prototype, trainData1);
						if (distance < closest) {
							closest = distance;
							closestI = i;
							closestJ = j;
						}
					}
				}

				if (epoch == epochs - 1) {
					/// in the last epoch, add the members to the cluster that was found
					clusters[closestI][closestJ].currentMembers.add(member);
				} else {
					/// else calculate the neigborhood for the cluster that was found and update the prototype based on that
					Vector<float[]> neighborhood = getNeighborhood(clusters[closestI][closestJ], epoch);
					updatePrototype(clusters[closestI][closestJ], neighborhood, epoch);
				}
			}
		}
		// Step 1: initialize map with random vectors (A good place to do this, is in the initialisation of the clusters)
		// Repeat 'epochs' times:
		// Step 2: Calculate the squareSize and the learningRate, these decrease lineary with the number of epochs.
		// Step 3: Every input vector is presented to the map (always in the same order)
		// For each vector its Best Matching Unit is found, and :
		// Step 4: All nodes within the neighbourhood of the BMU are changed, you don't have to use distance relative learning.
		// Since training kohonen maps can take quite a while, presenting the user with a progress bar would be nice

		return true;
	}

	public boolean test() {
		// iterate along all clients
		// for each client find the cluster of which it is a member
		// get the actual testData (the vector) of this client
		// iterate along all dimensions
		// and count prefetched htmls
		// count number of hits
		// count number of requests
		// set the global variables hitrate and accuracy to their appropriate value
		
		/// similar as in KMeans, but looping over a 2D cluster array
		int hits = 0;
		int prefetched = 0;
		int requests = 0;
		Iterator<Integer> iter;
		for (Cluster[] cluster1 : clusters) {
			for (Cluster cluster : cluster1) {
				for (int client : cluster.currentMembers) {
					float[] test = this.testData.get(client);
					for (int i2 = 0; i2 < this.dim; i2++) {
						if (cluster.prototype[i2] > this.prefetchThreshold) {
							prefetched++;
							if (Math.round(test[i2]) == 1) {
								hits++;
							}
						}
						if (Math.round(test[i2]) == 1) {
							requests++;
						}
					}
				}
			}
		}
		this.hitrate = ((double) hits / requests);
		this.accuracy = ((double) hits / prefetched);
		return true;
	}

	public void showTest() {
		System.out.println("Initial learning Rate=" + initialLearningRate);
		System.out.println("Prefetch threshold=" + prefetchThreshold);
		System.out.println("Hitrate: " + hitrate);
		System.out.println("Accuracy: " + accuracy);
		System.out.println("Hitrate+Accuracy=" + (hitrate + accuracy));
	}

	public void showMembers() {
		for (int i = 0; i < n; i++) {
			for (int i2 = 0; i2 < n; i2++) {
				System.out.println("\nMembers cluster[" + i + "][" + i2 + "] :" + clusters[i][i2].currentMembers);
			}
		}
	}

	public void showPrototypes() {
		for (int i = 0; i < n; i++) {
			for (int i2 = 0; i2 < n; i2++) {
				System.out.print("\nPrototype cluster[" + i + "][" + i2 + "] :");

				for (int i3 = 0; i3 < dim; i3++) {
					System.out.print(" " + clusters[i][i2].prototype[i3]);
				}

				System.out.println();
			}
		}
	}

	public void setPrefetchThreshold(double prefetchThreshold) {
		this.prefetchThreshold = prefetchThreshold;
	}
}
