import java.util.*;
import java.io.*;
import java.lang.*;
//import javafx.util.*;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

public class RunHenzingerKale{

  // main function to run tests
  public static void main(String[] args) throws IOException, InterruptedException {

    /*

    test our algorithm with the relevant input params

    */

    // parameter k
    int k = Integer.valueOf(args[0]);

    // dataset
    String dataset = args[1];

    // create un update stream of length n
    int n = Integer.valueOf(args[2]);

    // set the window length
    int windowLength = Integer.valueOf(args[3]);

    // number of queries to perform over the stream
    int queryCount = Integer.valueOf(args[4]);

    // the metric to be used
    Metric metric = new LpNorm(2, 1.0f/n);

    // create update stream
    SlidingWindow updateStream = new SlidingWindow(n, windowLength, "../data/" + dataset, true);

    // the parameter psi (determines number of points sampled per layer)
    int psi = Integer.valueOf(args[5]);

    // the directory in which to store the output
    String dir = "test_results/";

    DynamicAlgorithm[] dynamicAlgorithms = new DynamicAlgorithm[1];

    dynamicAlgorithms[0] = new HenzingerTree(k, metric, psi);

    /*

    run the test

    */

    Test.runTests(updateStream, dynamicAlgorithms, metric, dataset, queryCount, dir);
  }
}
