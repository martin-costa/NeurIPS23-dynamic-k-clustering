import java.util.*;
import java.io.*;

/*

an implementation of the kmeans++ algorithm

*/

class KMeansPlusPlus {

  // the points that we want to cluster
  private float[][] points;

  // the weights of the points to be clustered
  private float[] weights;

  // the keys of the points to be clustered
  private int[] keys;

  // the metric
  private Metric metric;

  // the paramter k
  private int k;

  // the number of points
  private int n;

  // dimension of the data
  private int d;

  // number of post processing iterations
  private int iterations;

  // the clusters we want to find
  private int[][] clusters;

  // the centers of these clusters
  private int[] clusterCenters;

  // set the metric and the value k
  KMeansPlusPlus(int k, Metric metric, int iterations) {
    this.metric = metric;
    this.k = k;
    this.iterations = iterations;
  }

  KMeansPlusPlus(int k, Metric metric) {
    this.metric = metric;
    this.k = k;
    this.iterations = 2;
  }

  // METHODS TO CALL THE CLUSTERING

  public TreeMap<Integer, Integer> clusterUniform(TreeMap<Integer, float[]> points, TreeMap<Integer, Integer> startingConfig) {

    // set all the weights to be 1
    TreeMap<Integer, Float> weights = new TreeMap<Integer, Float>();

    Set<Integer> keySet = points.keySet();

    for (Integer key : keySet) {
      weights.put(key, 1.0f);
    }

    return cluster(points, weights, startingConfig);
  }

  public TreeMap<Integer, Integer> clusterUniform(TreeMap<Integer, float[]> points) {
    return clusterUniform(points, null);
  }

  public TreeMap<Integer, Integer> cluster(TreeMap<Integer, float[]> points, TreeMap<Integer, Float> weights) {
    return cluster(points, weights, null);
  }

  public TreeMap<Integer, Integer> cluster(TreeMap<Integer, float[]> points, TreeMap<Integer, Float> weights, TreeMap<Integer, Integer> startingConfig) {

    float[][] pointsArr = new float[points.size()][];
    float[] weightsArr = new float[points.size()];
    int[] keysArr = new int[points.size()];

    int n = points.size();

    Integer[] tempKeys = points.keySet().toArray(new Integer[0]);

    for (int i = 0; i < n; i++) {
      pointsArr[i] = points.get(tempKeys[i]);
      weightsArr[i] = weights.get(tempKeys[i]);
      keysArr[i] = tempKeys[i];
    }

    float[][] startingConfigArr = null;

    if (startingConfig != null) {

      startingConfigArr = new float[startingConfig.size()][];

      tempKeys = startingConfig.keySet().toArray(new Integer[0]);

      for (int i = 0; i < startingConfigArr.length; i++) {
        startingConfigArr[i] = points.get(tempKeys[i]);
      }
    }

    return cluster(pointsArr, weightsArr, keysArr, startingConfigArr);
  }

  public TreeMap<Integer, Integer> cluster(float[][] points, float[] weights, int[] keys) {
    return cluster(points, weights, keys, null);
  }

  public TreeMap<Integer, Integer> cluster(float[][] points, float[] weights, int[] keys, float[][] startingConfig) {

    // set the points and the weights
    this.points = points;
    this.weights = weights;
    this.keys = keys;

    // the number of points
    this.n = points.length;

    // set the dimension of the data
    if (this.n > 0) {
      this.d = points[0].length;
    }
    else {
      return new TreeMap<Integer, Integer>();
    }

    return kmeansplusplus(iterations, startingConfig);
  }

  /*

  IMPLEMENTATION

  */

  // run using the seeding
  public TreeMap<Integer, Integer> kmeansplusplus(int iterations) {
    return kmeansplusplus(iterations, null);
  }

  // implementation of bicriteria approximation using kmeans++
  public TreeMap<Integer, Integer> kmeansplusplus(int iterations, float[][] startingConfig) {

    // if we have at most k points return each point as a center
    if (n <= k) {
      return returnAll();
    }

    if (startingConfig == null) {
      // seed good starting centers and create clusters
      seedStartingCenters();
    }
    else {

      // create the initial clusters
      createClusters(startingConfig);
    }

    // run k iterations of kmeans
    kmeans(iterations);

    return createSolution();
  }

  // create a trivial solution if n is too small
  private TreeMap<Integer, Integer > returnAll() {

    this.clusters = new int[n][1];

    // create the clusters and centers
    this.clusterCenters = new int[n];

    // create solution
    TreeMap<Integer, Integer> solution = new TreeMap<Integer, Integer>();

    int i = 0;
    for (int key : keys) {
      solution.put(key, key);
      this.clusters[i][0] = i;
      this.clusterCenters[i] = i;
      i++;
    }
    return solution;
  }

  // find a good starting point for kmeans
  private void seedStartingCenters() {

    // total weight of points
    float totalWeight = 0;

    for (int i = 0; i < n; i++) {
      totalWeight += weights[i];
    }

    // create array with sampling probabilities
    float[] probs = new float[n];

    for (int i = 0; i < n; i++) {
      probs[i] = weights[i]/totalWeight;
    }

    // create random number generator
    Random rng = new Random();

    float[][] samplePoints = new float[k][d];

    // distances from samples points
    float[] dist = new float[n];
    Arrays.fill(dist, Float.POSITIVE_INFINITY);

    for (int i = 0; i < k; i++) {
      samplePoints[i] = points[dSquaredWeighting(rng, probs, dist)];
    }

    // create the initial clusters
    createClusters(samplePoints);
  }

  // sample a point according to D^2 weighting
  private int dSquaredWeighting(Random rng, float[] probs, float[] dist) {

    float r = rng.nextFloat();
    float s = 0;

    int sample = 0;

    // sample a point from the distribution defined by probs
    for (int i = 0; i < n; i++) {
      s += probs[i];
      if (r <= s) {
        sample = i;
        break;
      }
    }

    // compute the new distances and probabilities
    float totalDSquared = 0;

    for (int i = 0; i < n; i++) {
      dist[i] = Math.min(dist[i], metric.d(points[i], points[sample]));
      totalDSquared += weights[i]*dist[i]*dist[i];
    }

    // if every points is already at a point thats been sampled
    if (totalDSquared <= 0) {
      probs[0] = 1;
      for (int i = 1; i < n; i++) {
        probs[i] = 0;
      }
      return sample;
    }

    for (int i = 0; i < n; i++) {
      probs[i] = weights[i]*dist[i]*dist[i]/totalDSquared;
    }

    return sample;
  }

  // get the point in cluster i closest to the center of mass of cluster i
  private int getClusterCenter(int i) {

    float[] centerOfMass = clusterCenterOfMass(i);

    if (clusters[i].length == 0) {
      return 0;
    }

    int closestPoint = clusters[i][0];
    float dist = Float.POSITIVE_INFINITY;

    for (int j : clusters[i]) {
      float d = metric.d(points[j], centerOfMass);
      if (d < dist) {
        closestPoint = j;
        dist = d;
      }
    }

    return closestPoint;
  }

  // standard kmeans
  private void kmeans(int iterations) {

    // run iterations many interations of kmeans heuristic
    for (int i = 0; i < iterations; i++) {
      kmeansIteration();
    }
  }

  // one iteration of kmeans
  private void kmeansIteration() {

    // find the centers of mass
    float[][] newCenters = new float[k][d];

    for (int i = 0; i < k; i++) {

      // get cluster center of mass
      newCenters[i] = clusterCenterOfMass(i);
    }

    // create the new clusters
    createClusters(newCenters);
  }

  // given the new centers create the clusters
  private void createClusters(float[][] newCenters) {

    // if we have < k new centers, set some other arbitrary ones
    if (newCenters.length < k) {

      float[][] tempNewCenters = newCenters;
      newCenters = new float[k][];

      for (int i = 0; i < tempNewCenters.length; i++) {
        newCenters[i] = tempNewCenters[i];
      }

      for (int i = tempNewCenters.length; i < k; i++) {
        newCenters[i] = points[i];
      }
    }

    @SuppressWarnings("unchecked")
    ArrayList<Integer>[] tempClusters = new ArrayList[k];

    // reset the clusters
    for (int i = 0; i < k; i++) {
      tempClusters[i] = new ArrayList<Integer>();
    }

    // re-allocate points to clusters
    for (int i = 0; i < n; i++) {

      float dist = Float.POSITIVE_INFINITY;
      int l = 0;

      for (int j = 0; j < k; j++) {

        float d = metric.d(newCenters[j], points[i]);

        // is point is closer to cluster center j than l
        if (d < dist) {
          dist = d;
          l = j;
        }
      }

      // place point in cluster l
      tempClusters[l].add(i);
    }

    clusters = new int[k][];

    for (int i = 0; i < k; i++) {
      clusters[i] = new int[tempClusters[i].size()];

      for (int j = 0; j < clusters[i].length; j++) {
        clusters[i][j] = (int)(tempClusters[i].get(j));
      }
    }
  }

  // returns the center of mass of cluster i
  private float[] clusterCenterOfMass(int i) {

    // the center of mass
    float[] center = new float[d];

    // the points in the cluster
    int[] cluster = clusters[i];

    // total weight of points in this cluster
    float totalWeight = 0;

    for (int p : cluster) {

      // get the point and its weight
      float[] x = points[p];
      float w = weights[p];

      totalWeight += w;

      for (int j = 0; j < d; j++) {
        center[j] += w*x[j];
      }
    }

    for (int j = 0; j < d; j++) {
      center[j] /= totalWeight;
    }

    return center;
  }

  // get the centers from the clusters and return them
  private TreeMap<Integer, Integer> createSolution() {

    TreeMap<Integer, Integer> solution = new TreeMap<Integer, Integer>();

    // put cluster centers
    this.clusterCenters = new int[k];

    for (int i = 0; i < k; i++) {
      int p = getClusterCenter(i);
      solution.put(keys[p], keys[p]);
      this.clusterCenters[i] = p;
    }

    return solution;
  }

  // get the clusters
  public int[][] getClusters() {
    return clusters;
  }

  // get the cluster centers
  public int[] getClusterCenters() {
    return clusterCenters;
  }
}
