import java.lang.Math;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.*;

/*

implementation of our dynamic data structure for maintaining a coreset

*/

// our dynamic coreset data structure
public class DynamicMP extends DynamicAlgorithm {

  // the underlying metric space
  private TreeMap<Integer, float[]> space;

  // the nested collections of points for each layer
  private LinkedList<TreeMap<Integer, Integer>> layers;

  // the sampled medians for each layer
  private LinkedList<TreeMap<Integer, Float>> samples;

  // the clusters for each layer
  private LinkedList<TreeMap<Integer, Integer>[]> clusters;

  // the clusterd sorted by increasing distance from center
  private LinkedList<TreeMap<Float, Integer>[]> sortedClusters;

  // number of updates since reconstruction at each layer
  private LinkedList<Integer> reconTimer;

  // metric used to evaluate distances
  private Metric metric;

  // parameters for layer construction
  private int k;

  private float alpha;
  private float beta;
  private float epsilon;
  private float tau;

  // number of points taken as medians each layer
  private int sampleSize;

  // constructer
  public DynamicMP(int k, Metric metric, float alpha, float beta, float epsilon) {

    // the metric
    this.metric = metric;

    // parameters
    this.k = k;
    this.alpha = alpha;
    this.beta = beta;
    this.epsilon = epsilon;
    this.tau = beta*epsilon;

    // sampling parameter
    this.sampleSize = (int)Math.floor(alpha);

    // initialise the data structures
    this.space = new TreeMap<Integer, float[]>();

    this.layers = new LinkedList<TreeMap<Integer, Integer>>();

    this.layers.add(new TreeMap<Integer, Integer>());

    this.samples = new LinkedList<TreeMap<Integer, Float>>();

    this.clusters = new LinkedList<TreeMap<Integer, Integer>[]>();

    this.sortedClusters = new LinkedList<TreeMap<Float, Integer>[]>();

    this.reconTimer = new LinkedList<Integer>();
  }

  /*

  insert a point into the data structure

  */

  public void insert(int key, float[] point) {

    // insert point into the metric space
    this.space.put(key, point);

    // insert point into each layer
    ListIterator<TreeMap<Integer, Integer>> layerIterator = layers.listIterator();
    ListIterator<Integer> reconIterator = reconTimer.listIterator();

    for (int i = 0; i < this.depth()-1; i++) {

      // insert point into layer
      layerIterator.next().put(key, -1);

      // decrement reconstruction timer for this layer
      reconIterator.set(reconIterator.next()-1);
    }

    // insert point into unsampled layer
    layerIterator.next().put(key, -1);

    // check if we need to construct a new layer
    this.reconstructFromLayer(this.depth() - 1);

    this.checkForReconstruction();
  }

  /*

  delete a point in the data structure

  */

  public void delete(int key) {

    // delete point from the metric space
    this.space.remove(key);

    // delete point from each layer
    ListIterator<TreeMap<Integer, Integer>> layerIterator = this.layers.listIterator();
    ListIterator<Integer> reconIterator = this.reconTimer.listIterator();

    for (int i = 0; i < this.depth()-1; i++) {

      // delete point from layer
      int clusterIndex = layerIterator.next().remove(key);

      // decrement reconstruction timer for this layer
      reconIterator.set(reconIterator.next()-1);

      // check if point is in a cluster at this layer
      if (clusterIndex >= 0) {

        // get the cluster and set of sampled points
        TreeMap<Integer, Integer> cluster = this.clusters.get(i)[clusterIndex];
        TreeMap<Integer, Float> layerSamples = this.samples.get(i);

        // remove point from this cluster
        cluster.remove(key);

        // check if the point was sampled in this layer
        if (layerSamples.containsKey(key)) {

          // remove the point from the samples
          layerSamples.remove(key);

          // replace sampled point with the next clostsest point in the cluster
          if (!cluster.isEmpty()) {
            Integer newCenter = getClosestPoint(cluster, this.sortedClusters.get(i)[clusterIndex]);
            layerSamples.put(newCenter, (float)cluster.size());
          }
        }

        // check if data structure needs to be reconstructed
        this.checkForReconstruction();
        return;
      }
    }

    // remove point from unslampled layer
    layerIterator.next().remove(key);

    // check if data structure needs to be reconstructed
    this.checkForReconstruction();
  }

  // return the closest live point in a cluster
  private Integer getClosestPoint(TreeMap<Integer, Integer> cluster, TreeMap<Float, Integer> sortedCluster) {

    // get the closest points in sortedCluster
    Integer x = sortedCluster.get(sortedCluster.firstKey());

    while (!cluster.containsKey(x)) {

      // remove this entry and get the next one
      sortedCluster.pollFirstEntry();
      x = sortedCluster.get(sortedCluster.firstKey());

    }

    return x;
  }

  // check whether some layer needs to be reconstructed
  private void checkForReconstruction() {

    for (int i = 0; i < this.reconTimer.size(); i++) {
      if (this.reconTimer.get(i) <= 0) {

        // reconstruct data structure starting from layer i
        reconstructFromLayer(i);
        return;
      }
    }
  }

  // reconstruct the layers starting from layer i
  private void reconstructFromLayer(int i) {

    // delete the lower layers
    while (this.depth() - 1 > i) {
      this.layers.removeLast();
      this.samples.removeLast();
      this.clusters.removeLast();
      this.sortedClusters.removeLast();
      this.reconTimer.removeLast();
    }

    // construct the new layers
    while(this.layers.getLast().size() > this.sampleSize) {
      this.constructLayer();
    }
  }

  // creates a new layer
  private void constructLayer() {

    // get the last layer
    TreeMap<Integer, Integer> currentLayer = this.layers.getLast();

    // place the points contained in the unsampled layer in an array
    Integer[] points = currentLayer.keySet().toArray(new Integer[0]);

    int n = points.length;
    Random rng = new Random();

    // sample points as centers from this set
    TreeMap<Integer, Float> layerSamples = new TreeMap<Integer, Float>();

    for (int i = 0; i < this.sampleSize; i++) {
      Integer sample = points[rng.nextInt(n)];
      layerSamples.put(sample, 0.0f);
    }

    // place sampled points in array
    Integer[] layerSamplesArr = layerSamples.keySet().toArray(new Integer[0]);

    int m = layerSamplesArr.length;

    // find distance from each point in the set from the sampled points
    float[] dist = new float[n];

    // find an assignment of the points to the centers
    int[] assignment = new int[n];

    // find how many points are assigned to each cluster center
    int[] weights = new int[m];

    for (int i = 0; i < n; i++) {

      dist[i] = Float.POSITIVE_INFINITY;
      assignment[i] = 0;
      for (int j = 0; j < m; j++) {
        float x = this.metric.d(this.space.get(points[i]), this.space.get(layerSamplesArr[j]));
        if (x < dist[i]) {
          dist[i] = x;
          assignment[i] = j;
        }
      }
      weights[assignment[i]]++;
    }

    // set weights
    for (int i = 0; i < m; i++) {
      layerSamples.put(layerSamplesArr[i], (float)weights[i]);
    }

    // compute the value nu
    float[] distCopy = dist.clone();
    Arrays.sort(distCopy);
    float nu = distCopy[(int)Math.ceil(n*this.beta)]; // USE LINEAR SEARCH FOR O(log n) SPEEDUP!!

    // compute the clustering at this layer and create new layer of unclustered points
    @SuppressWarnings("unchecked")
    TreeMap<Integer, Integer>[] layerClustering = new TreeMap[m];

    @SuppressWarnings("unchecked")
    TreeMap<Float, Integer>[] layerSortedClustering = new TreeMap[m];

    TreeMap<Integer, Integer> newLayer = new TreeMap<Integer, Integer>();

    for (int i = 0; i < m; i++) {
      layerClustering[i] = new TreeMap<Integer, Integer>();
      layerSortedClustering[i] = new TreeMap<Float, Integer>();
    }

    for (int i = 0; i < n; i++) {
      if (dist[i] <= nu) {
        layerClustering[assignment[i]].put(points[i], points[i]);
        layerSortedClustering[assignment[i]].put(dist[i], points[i]);
        currentLayer.put(points[i], assignment[i]);
      }
      else {
        currentLayer.put(points[i], -1);
        newLayer.put(points[i], -1);
      }
    }

    // update the data structures
    this.layers.add(newLayer);

    this.samples.add(layerSamples);

    this.clusters.add(layerClustering);

    this.sortedClusters.add(layerSortedClustering);

    this.reconTimer.add((int)Math.ceil(n*this.tau));
  }

  // returns the number of layers in the data structure
  public int depth() {
    return this.layers.size();
  }

  // clusters the coreset and returns the value of the clustering
  public TreeMap<Integer, Integer> cluster() {

    TreeMap<Integer, Float> coresetWeights = new TreeMap<Integer, Float>();

    // add final unsampled layer
    Integer[] finalLayer = layers.getLast().keySet().toArray(new Integer[0]);

    for (Integer key : finalLayer) {
      coresetWeights.put(key, 1.0f);
    }

    // place the samples from each layer into the map
    for (int i = 0; i < samples.size(); i++) {
      coresetWeights.putAll(samples.get(i));
    }

    // create a map of the actual points
    TreeMap<Integer, float[]> coresetPoints = new TreeMap<Integer, float[]>();

    Integer[] coresetPointsArr = coresetWeights.keySet().toArray(new Integer[0]);

    for (Integer key : coresetPointsArr) {
      coresetPoints.put(key, space.get(key));
    }

    // call the static algorithm on the coreset
    OnlineKMedian staticAlgo = new OnlineKMedian(k, metric);

    return staticAlgo.cluster(coresetPoints, coresetWeights);
  }

  ////____//// METHODS FOR TESTING ////____////

  public void printStats() {

    // print times until reconstruction
    System.out.print("Reconstruction timer: ");
    System.out.println(this.reconTimer);

    // print sizes of layers
    int j = 0;
    LinkedList<Integer> sizes = new LinkedList<Integer>();
    for (int i = 0; i < depth(); i++) {
      sizes.add(layers.get(i).size());
    }
    System.out.print("Layer sizes:          ");
    System.out.println(sizes);

    // print number of points in the space
    System.out.print("Size of metric space: ");
    System.out.println(this.space.size());
    System.out.println("");
  }

  public String name() {
    return String.valueOf(k) + "_" + String.valueOf((int)alpha) + "_BCLP";
  }
}
