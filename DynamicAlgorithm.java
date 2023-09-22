import java.util.*;
import java.io.*;
import java.lang.Math;

// and abstract class for a dynamic clustering algorithm

abstract class DynamicAlgorithm {

  // insert a point
  public abstract void insert(int key, float[] point);

  // delete a point
  public abstract void delete(int key);

  // cluster the points and return the solution as a treemap
  public abstract TreeMap<Integer, Integer> cluster();

  // returns the name of the algorithm
  public abstract String name();
}
