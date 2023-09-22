import java.util.*;
import java.io.*;

/*

implementation of (static) local search of Arya et al.

*/

public class OnlineKMedian {

  // parameter for k clustering
  private int k;

  // metric used to evaluate distances
  private Metric metric;

  // parameters for the algorithm
  private float alpha;

  private float beta;

  private float gamma;

  // the points defining the instance
  private TreeMap<Integer, float[]> points;

  // the weights of the points
  private TreeMap<Integer, Float> weights;

  // the number of points in the space
  private int n;

  // the cost of the clustering found
  private float clusteringCost;

  // auxiliary data structures used for solving the problem

  // an array of the keys corresponding to the points in the input
  private Integer[] keysArr;

  // an array of the points
  private float[][] pointsArr;

  // an array of the weights
  private float[] weightsArr;

  // an array containing medians found so far
  private int[] medians;

  // a BBT of the medians found so far
  private PriorityQueue<Integer> nonMedians;

  // keep track of medians found so far
  private int found = 0;

  // the following are used for finding the values of balls

  // distance from each point to the closest median
  private float[] distFromMedians;

  // arrays of the points sorted by their distances
  private int[][] sortedPointsArr;

  // sortedPointsDistArr[i][j] = d(pointArr[i], pointsArr[sortedPointsArr[i][j]])
  private float[][] sortedPointsDistArr;

  // arrays to make it easier to find the values of balls
  private float[][] ballValueAux1;
  private float[][] ballValueAux2;

  // define the parameters
  public OnlineKMedian(int k, Metric metric) {

    this.k = k;
    this.metric = metric;

    this.alpha = 2 + (float)Math.sqrt(3);
    this.beta = (this.alpha - 1)/(this.alpha - 2);
    this.gamma = this.alpha*(1 + this.alpha)/(this.alpha - 2);
  }

  // given an unweighted metric space, find a k clustering of the points
  public TreeMap<Integer, Integer> cluster(TreeMap<Integer, float[]> points) {

    // set all the weights to be 1
    TreeMap<Integer, Float> weights = new TreeMap<Integer, Float>();

    Set<Integer> keySet = points.keySet();

    for (Integer key : keySet) {
      weights.put(key, 1.0f);
    }

    // cluster this instance
    return cluster(points, weights);
  }

  // given a weighted metric space, find a k clustering of the points
  public TreeMap<Integer, Integer> cluster(TreeMap<Integer, float[]> points, TreeMap<Integer, Float> weights) {

    // save the input
    this.points = points;
    this.weights = weights;

    // the number of points in the input
    this.n = this.points.size();

    // create the auxiliary data structures
    setUpDataStructures();

    // get the clustering
    TreeMap<Integer, Integer> solution = cluster();

    // // perform 2 iterations of Lloyd's and return
    KMeansPlusPlus kmeanspp = new KMeansPlusPlus(k, metric);
    return kmeanspp.cluster(points, weights, solution);
  }

  // given a weighted metric space, find a k clustering of the points
  public TreeMap<Integer, Integer> cluster(float[][] points, float[] weights, int[] keys) {

    // the number of points in the input
    this.n = points.length;

    // create the auxiliary data structures
    setUpDataStructures(points, weights, keys);

    // get the clustering
    TreeMap<Integer, Integer> solution = cluster();

    this.points = new TreeMap<Integer, float[]>();
    this.weights = new TreeMap<Integer, Float>();

    for (int i = 0; i < n; i++) {
      this.points.put(keys[i], points[i]);
      this.weights.put(keys[i], weights[i]);
    }

    // // perform 2 iterations of Lloyd's and return
    KMeansPlusPlus kmeanspp = new KMeansPlusPlus(k, metric);
    return kmeanspp.cluster(this.points, this.weights, solution);
  }

  // implementation of online k-median algorithm
  private TreeMap<Integer, Integer> cluster() {

    // if we can take everything as a median
    if (n <= k) {
      clusteringCost = 0;

      // place all the keys into a BBT
      TreeMap<Integer, Integer> solution = new TreeMap<Integer, Integer>();
      for (int i=0; i < n; i++) {
        solution.put(keysArr[i], keysArr[i]);
      }
      return solution;
    }

    // implementation of online k-median algorithm
    while (found < k) {

      // find the next median
      int i = findNextMedian();

      // next median is i
      nonMedians.remove(i);
      medians[found] = i;

      found++;

      // update distances from medians
      for (int j=0; j < n; j++) {
        distFromMedians[j] = Math.min(distFromMedians[j], metric.d(pointsArr[j], pointsArr[i])); // removed *weightsArr[j]?
      }
    }

    // create a BBT to return solution
    TreeMap<Integer, Integer> solution = new TreeMap<Integer, Integer>();

    for (int j=0; j < k; j++) {
      solution.put(keysArr[medians[j]], keysArr[medians[j]]);
    }

    // compute the cost of the clustering
    clusteringCost = 0;
    for (int j = 0; j < n; j++) {
      clusteringCost += distFromMedians[j]*weightsArr[j];
    }

    return solution;
  }

  // find the next median
  private int findNextMedian() {

    // get the max value ball from isolated
    Pair ball = maxValueIsolated();

    int i = ball.l;
    float r = ball.r;

    // find the index of the last point that is within beta*r of i
    int j = Arrays.binarySearch(sortedPointsDistArr[i], beta*r);
    if (j < 0) j = -j-2;

    // while we have more than one child (j > 0) find a max value child
    while (j > 0) {

      // find the max value child of (i, r)
      ball = maxValueChild(i, r, j);

      i = ball.l;
      r = ball.r;

      // find the index of the last point that is within beta*r of i
      j = Arrays.binarySearch(sortedPointsDistArr[i], beta*r);
      if (j < 0) j = -j-2;

      // we might have duplicate ponts
      if (r == 0.0f && j > 0)
        j = 0;
    }

    // next median is i
    return i;
  }

  // find the max value child of this ball
  private Pair maxValueChild(int i, float r, int l) {

    // keep track of ball of best value
    Pair ball = new Pair(-1, 0.0f);

    // the value of ball
    float bestValue = -Float.POSITIVE_INFINITY;

    for (int j = 0; j <= l; j++) {

      float value = ballValue(sortedPointsArr[i][j], r/alpha);

      if (value >= bestValue) {
        bestValue = value;
        ball = new Pair(sortedPointsArr[i][j], r/alpha);
      }
    }

    return ball;
  }

  // return the max value of isolated(x, medians) for some x not in medians
  private Pair maxValueIsolated() {

    // keep track of ball of best value
    Pair ball = new Pair(-1, 0.0f);

    // the value of ball
    float bestValue = -Float.POSITIVE_INFINITY;

    for (Integer j : nonMedians) {

      float r = isolated(j);

      float value = ballValue(j, r);

      if (value >= bestValue) {
        bestValue = value;
        ball = new Pair(j, r);
      }
    }

    return ball;
  }

  // return the radius of isolated(x, medians) for some x not in medians
  private float isolated(int i) {

    // if there are no medians, return distance of furthest point
    if (found == 0)
      return sortedPointsDistArr[i][n-1];

    return distFromMedians[i]/gamma;
  }

  // find the value of the ball with center
  private float ballValue(int i, float r) {

    // find the furthest point from point i that is not more than r away
    int j = Arrays.binarySearch(sortedPointsDistArr[i], r);

    if (j < 0) j = -j-2;

    // FOR NOISE
    if (j < 0) j = 0;

    return r*ballValueAux1[i][j] - ballValueAux2[i][j];
  }

  private void setUpDataStructures() {
    setUpDataStructures(null, null, null);
  }

  // set up the data structures needed to solve the problem efficiently
  private void setUpDataStructures(float[][] points, float[] weights, int[] keys) {

    // end if the input is empty
    if (n <= 0) return;

    if (points == null) {

      // place the keys of the points into an array
      keysArr = this.points.keySet().toArray(new Integer[0]);

      // the dimension of the data
      int d = this.points.get(keysArr[0]).length;

      // create an array to store the points
      pointsArr = new float[n][d];

      // create an array to store the weights
      weightsArr = new float[n];

      for (int i = 0; i < n; i++) {
        pointsArr[i] = this.points.get(keysArr[i]);
        weightsArr[i] = this.weights.get(keysArr[i]);
      }
    }
    else {
      pointsArr = points;
      weightsArr = weights;

      keysArr = new Integer[n];
      for (int i = 0; i < n; i++) {
        keysArr[i] = keys[i];
      }
    }

    // initialise array to store medians
    medians = new int[k];
    found = 0;

    // initialise PriorityQueue for searching non median points
    nonMedians = new PriorityQueue<Integer>();

    for (int i = 0; i < n; i++) {
      nonMedians.add(i);
    }

    // store distances of points from medians
    distFromMedians = new float[n];
    Arrays.fill(distFromMedians, Float.POSITIVE_INFINITY);

    // create a 2D array of points sorted by distance from each other
    sortedPointsArr = new int[n][n];
    sortedPointsDistArr = new float[n][n];

    Pair[] sortingArr = new Pair[n];

    for (int i = 0; i < n; i++) {

      // place the indices of the points and their distances from i into an array
      for (int j = 0; j < n; j++) {
        sortingArr[j] = new Pair(j, metric.d(pointsArr[i], pointsArr[j]));
      }

      Arrays.sort(sortingArr);

      for (int j = 0; j < n; j++) {
        Pair p = sortingArr[j];
        sortedPointsArr[i][j] = p.l;
        sortedPointsDistArr[i][j] = p.r;
      }
    }

    ballValueAux1 = new float[n][n];
    ballValueAux2 = new float[n][n];

    // compute the auxiliary information to enable fast ball value computations
    for (int i = 0; i < n; i++) {
      ballValueAux1[i][0] = weightsArr[sortedPointsArr[i][0]];
      ballValueAux2[i][0] = sortedPointsDistArr[i][0]*weightsArr[sortedPointsArr[i][0]];
      for (int j = 1; j < n; j++) {
        ballValueAux1[i][j] = ballValueAux1[i][j-1] + weightsArr[sortedPointsArr[i][j]];
        ballValueAux2[i][j] = ballValueAux2[i][j-1] + sortedPointsDistArr[i][j]*weightsArr[sortedPointsArr[i][j]];
      }
    }
  }

}
