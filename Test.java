import java.util.*;
import java.io.*;
import java.lang.*;
//import javafx.util.*;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class Test {

  // 'census' input path
  private static String census = "census";

  // 'song' input path
  private static String song = "song";

  // 'kddcup' input path
  private static String kddcup = "kddcup";

  // 'drift' input path
  private static String drift = "drift";

  // 'sift10M' input path
  private static String sift10M = "sift10M";

  // main function to run tests
  public static void main(String[] args) throws IOException, InterruptedException {

    /*

    set parameters for the tests

    */

    // dataset
    String dataset = kddcup;

    // parameter k
    int k = 50;

    // set the window length
    int windowLength = 5000;

    // create un update stream of length n
    int n = 100000;

    // number of queries to perform over the stream
    int queryCount = 100;

    // the metric to be used
    Metric metric = new LpNorm(2, 1.0f/n);

    // create update stream
    SlidingWindow updateStream = new SlidingWindow(n, windowLength, "../data/" + dataset, true);

    float beta = 0.5f;
    float epsilon = 0.2f;

    DynamicAlgorithm[] dynamicAlgorithms = new DynamicAlgorithm[2];

    dynamicAlgorithms[0] = new DynamicMP(k, metric, 500, beta, epsilon);
    dynamicAlgorithms[1] = new HenzingerTree(k, metric, 1000);

    /*

    run the tests

    */

    // large_test
    runTests(updateStream, dynamicAlgorithms, metric, dataset, queryCount, "large_test (data)/");

    // justification
    int[] kValues_j = { 50 };
    float[] alphaValues_j = { 250, 500, 1000 };
    int[] mValues_j = { 250, 500, 1000 };

    runBatchTests(3000, 2000, 100, kValues_j, alphaValues_j, mValues_j, metric, "justification (data)/", false);

    // experiments
    int[] kValues_e = { 10, 50, 100 };
    float[] alphaValues_e = { 500 };
    int[] mValues_e = { 1000 };

    runBatchTests(200, 100, 100, kValues_e, alphaValues_e, mValues_e, metric, "experiments (data)/", false);

    // justification_random
    runBatchTests(3000, 2000, 100, kValues_j, alphaValues_j, mValues_j, metric, "justification_random (data)/", true);

    // experiments_random
    runBatchTests(10000, 2000, 100, kValues_e, alphaValues_e, mValues_e, metric, "experiments_random (data)/", true);
  }

  // run tests on many algorithmss
  public static void runTests(SlidingWindow updateStream, DynamicAlgorithm[] dynamicAlgorithms, Metric metric, String dataset, int queryCount, String dir) throws IOException {

    // query frequency
    int queryFrequency = (int)(updateStream.streamLength()/queryCount);

    // the number of algorithms we are testing
    int l = dynamicAlgorithms.length;

    // measures the update times
    long[] updateTimes = new long[l];

    // measures the query times
    long[] queryTimes = new long[l];

    // measures the costs
    float[] costs = new float[l];

    // maintain the current instance in this BBT
    TreeMap<Integer, float[]> activePoints = new TreeMap<Integer, float[]>();

    // create output streams to write to files
    DataOutputStream[] updateTimeWriters = new DataOutputStream[l];
    DataOutputStream[] queryTimeWriters = new DataOutputStream[l];
    DataOutputStream[] costWriters = new DataOutputStream[l];

    for (int i = 0; i < l; i++) {
      updateTimeWriters[i] = new DataOutputStream(new FileOutputStream(dir + dataset + "_" + dynamicAlgorithms[i].name() + "_updatetime"));
      queryTimeWriters[i] = new DataOutputStream(new FileOutputStream(dir + dataset + "_" + dynamicAlgorithms[i].name() + "_querytime"));
      costWriters[i] = new DataOutputStream(new FileOutputStream(dir + dataset + "_" + dynamicAlgorithms[i].name() + "_cost"));
    }

    // handle the update stream
    for (int i = 0; i < updateStream.streamLength(); i++) {

      // update the active points
      if (updateStream.updateType(i))
        activePoints.put(updateStream.key(i), updateStream.point(i));
      if (!updateStream.updateType(i))
        activePoints.remove(updateStream.key(i));

      // handle and time the update for each one of the algorithms
      for (int j = 0; j < l; j++) {

        long s = System.nanoTime();

        if (updateStream.updateType(i))
          dynamicAlgorithms[j].insert(updateStream.key(i), updateStream.point(i));
        if (!updateStream.updateType(i))
          dynamicAlgorithms[j].delete(updateStream.key(i));

        updateTimes[j] += System.nanoTime() - s;
      }

      // perform query every queryFrequency updates (or on the last one)
      if (i % queryFrequency == 0 || i == updateStream.streamLength() - 1) {

        for (int j = 0; j < l; j++) {

          long s = System.nanoTime();

          // cluster and find the solution
          TreeMap<Integer, Integer> solution = dynamicAlgorithms[j].cluster();

          queryTimes[j] += System.nanoTime() - s;

          // find the cost
          costs[j] = cost(activePoints, solution, metric);
        }
      }

      // write to files
      for (int j = 0; j < l; j++) {
        updateTimeWriters[j].writeChars(Long.toString(updateTimes[j]) + "#");
      }

      if (i % queryFrequency == 0 || i == updateStream.streamLength() - 1) {
        for (int j = 0; j < l; j++) {
          queryTimeWriters[j].writeChars(Long.toString(queryTimes[j]) + "#");
          costWriters[j].writeChars(Float.toString(costs[j]) + "#");
        }
      }

      // print
      if (i % (int)(updateStream.streamLength()/100) == 0) {
        System.out.print(100*i/updateStream.streamLength());
        System.out.print("% complete (");
        System.out.print(i);
        System.out.println(" updates)");
      }
    }

    // close the file streams
    for (int i = 0; i < l; i++) {
      updateTimeWriters[i].close();
      queryTimeWriters[i].close();
      costWriters[i].close();
    }
  }

  // run the complete tests
  public static void runBatchTests(int n, int windowLength, int queryCount, int[] kValues, float[] alphaValues, int[] mValues, Metric metric, String dir, boolean randomOrder) throws IOException {

    float beta = 0.5f;
    float epsilon = 0.2f;

    String[] datasets = {census, song, kddcup, drift, sift10M};

    for (int k : kValues) {

      for (String dataset : datasets) {

        DynamicAlgorithm[] dynamicAlgorithms = new DynamicAlgorithm[alphaValues.length + mValues.length];

        for (int i = 0; i < alphaValues.length; i++) {
          dynamicAlgorithms[i] = new DynamicMP(k, metric, alphaValues[i], beta, epsilon);
        }

        for (int i = 0; i < mValues.length; i++) {
          dynamicAlgorithms[i + alphaValues.length] = new HenzingerTree(k, metric, mValues[i]);
        }

        SlidingWindow updateStream = new SlidingWindow(n, windowLength, "../data/" + dataset, randomOrder);

        System.out.println("----------------");
        System.out.println("Dataset: " + dataset);
        System.out.print("k: ");
        System.out.println(k);
        System.out.println("----------------");
        runTests(updateStream, dynamicAlgorithms, metric, dataset, queryCount, dir);

      }
    }
  }

  // compute the cost of solution with respect points with this metric
  public static float cost(TreeMap<Integer, float[]> points, TreeMap<Integer, Integer> solution, Metric metric) {

    Integer[] pointsArr = points.keySet().toArray(new Integer[0]);
    Integer[] solutionArr = solution.keySet().toArray(new Integer[0]);

    float cost = 0;

    for (int i = 0; i < pointsArr.length; i++) {
      float dist = Float.POSITIVE_INFINITY;

      for (int j = 0; j < solutionArr.length; j++) {

        float d = metric.d(points.get(pointsArr[i]), points.get(solutionArr[j]));
        if (d <= dist) dist = d;
      }

      cost += dist;
    }

    return cost;
  }
}
