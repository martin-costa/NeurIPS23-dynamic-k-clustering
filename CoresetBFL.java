import java.util.*;
import java.io.*;

/*

implementation of (static) coreset construction by Braverman et al. 2016

*/

public class CoresetBFL {

  // parameter for k clustering
  private int k;

  // metric used to evaluate distances
  private Metric metric;

  // the paramter rho of the rho-metric space
  private float rho;

  // coreset size
  private int m;

  // algorithm and factors for the (alpha, beta)-approximation
  private KMeansPlusPlus kmeanspp;

  // store the (alpha, beta)-approximation
  private int[][] clusters;
  private int[] clusterCenters;

  // the input points we compute a coreset on
  private float[][] points;
  private float[] weights;
  private int[] keys;

  // the coreset we compute
  private float[][] outPoints;
  private float[] outWeights;
  private int[] outKeys;

  public CoresetBFL(int k, Metric metric, int m) {
    this.k = k;
    this.metric = metric;

    this.rho = 1;

    this.m = m;

    this.kmeanspp = new KMeansPlusPlus(k, metric);
  }

  // returns an epsilon-coreset given a weighted set (P, w) of a rho-metric space
  // and an (alpha, beta)-approximation P --> B
  public void construct(float[][] points, float[] weights, int[] keys, float lambda, float epsilon) {

    this.points = points;
    this.weights = weights;
    this.keys = keys;

    int n = this.points.length;

    // set size to max of coreset construction parameter or k
    int m = Math.max(this.m, k);

    // if there aren't enough points
    if (n < m) {
      outPoints = points;
      outWeights = weights;
      outKeys = keys;
      return;
    }

    // get an (alpha, beta)-approximation
    approximate();

    // proceed with coreset construction
    coreset(m);
  }

  // constructs the coreset with m samples
  private void coreset(int m) {

    int n = this.points.length;

    // arrays of total weight per cluster
    float[] clusterWeights = new float[k];

    // total cost the approximation
    float v = 0;

    for (int i = 0; i < k; i++) {
      clusterWeights[i] = 0;
      for (int j = 0; j < clusters[i].length; j++) {
        clusterWeights[i] += weights[clusters[i][j]];
        v += weights[clusters[i][j]]*metric.d(points[clusters[i][j]], points[clusterCenters[i]]);
      }
    }

    // the arrays of sampling probabilities
    float[][] prob = new float[k][];

    for (int i = 0; i < k; i++) {
      prob[i] = new float[clusters[i].length];
      for (int j = 0; j < prob[i].length; j++) {
        prob[i][j] = 0.5f*weights[clusters[i][j]]*metric.d(points[clusters[i][j]], points[clusterCenters[i]])/v;
        prob[i][j] += 0.5f*weights[clusters[i][j]]/(k*clusterWeights[i]);
      }
    }

    samplePoints(m, prob);
  }

  public void samplePoints(int m, float[][] prob) {

    ArrayList<float[]> tempOutPoints = new ArrayList<float[]>();
    ArrayList<Float> tempOutWeights = new ArrayList<Float>();
     ArrayList<Integer> tempOutKeys = new ArrayList<Integer>();

    Random rng = new Random();

    // sample m points and place them into the corset output
    for (int l = 0; l < m; l++) {

      float r = rng.nextFloat();
      float t = 0;

      // sample a point
      for (int i = 0; i < k; i++) {

        int len = prob[i].length;

        for (int j = 0; j < len; j++) {

          t += prob[i][j];
          if (r <= t) {
            int idx = clusters[i][j];
            tempOutPoints.add(points[idx]);
            tempOutWeights.add(weights[idx]/(m*prob[i][j]));
            tempOutKeys.add(keys[idx]);

            // break from loop
            j = len;
            i = k;
          }
        }
      }
    }

    int s = tempOutPoints.size();

    outPoints = new float[s][];
    outWeights = new float[s];
    outKeys = new int[s];

    for (int i = 0; i < s; i++) {
      outPoints[i] = tempOutPoints.get(i);
      outWeights[i] = tempOutWeights.get(i);
      outKeys[i] = tempOutKeys.get(i);
    }
  }

  // computes an (alpha, beta)-approximation using kmeans++
  public void approximate() {

    // compute the (alpha beta)-approximation
    kmeanspp.cluster(points, weights, keys);

    clusters = kmeanspp.getClusters();
    clusterCenters = kmeanspp.getClusterCenters();
  }

  // returns the points computed by the coreset
  public float[][] getPoints() {
    return outPoints;
  }

  // returns the weights computed by the coreset
  public float[] getWeights() {
    return outWeights;
  }

  // returns keys
  public int[] getKeys() {
    return outKeys;
  }


}
