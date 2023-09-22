import java.util.*;
import java.io.*;
import java.lang.Math;

// generates a stream of m updates given data path

abstract class UpdateStreamGenerator {

  // returns the data point in update i
  public abstract float[] point(int i);

  // returns the (unique) key corresponding to the data point in update i
  public abstract int key(int i);

  // returns whether update i is an insertion (true) / deletion (false)
  public abstract boolean updateType(int i);

}

// generates update sequences in the sliding window model
class SlidingWindow extends UpdateStreamGenerator {

  // number of points to insert and delete
  private int n;

  // the length of the sliding window
  private int windowLength;

  // the data points
  private float[][] data;

  // stores the dimension of the data
  private int d;

  // permutation of points
  private int[] perm;

  SlidingWindow(int n, int windowLength, String path, boolean randomOrder, int offset) {

    this.n = n;
    this.windowLength = windowLength;

    // generate the permutation of points
    generatePermutation(randomOrder);

    // load the data
    this.loadData(path, offset);
  }

  SlidingWindow(int n, int windowLength, String path, boolean randomOrder) {

    this.n = n;
    this.windowLength = windowLength;

    // generate the permutation of points
    generatePermutation(randomOrder);

    // load the data
    this.loadData(path, 0);
  }

  SlidingWindow(int n, int windowLength, String path) {

    this.n = n;
    this.windowLength = windowLength;

    // generate the permutation of points
    generatePermutation(false);

    // load the data
    this.loadData(path, 0);
  }

  // returns the data point in update i
  public float[] point(int i) {
    return this.data[this.perm[this.key(i)]];
  }

  // returns the (unique) key corresponding to the data point in update i
  public int key(int i) {

    if (i < this.windowLength)
      return i;

    if (i > this.windowLength - 1 + 2*(this.n - this.windowLength))
      return i - this.n;

    if ((i - this.windowLength) % 2 == 1)
      return (i + this.windowLength - 1)/2;

    return (i - this.windowLength)/2;
  }

  // returns whether update i is an insertion (true) / deletion (false)
  public boolean updateType(int i) {

    if (i < this.windowLength)
      return true;

    if (i > this.windowLength - 1 + 2*(this.n - this.windowLength))
      return false;

    if ((i - this.windowLength) % 2 == 1)
      return true;

    return false;
  }

  // loads the data points
  private void loadData(String path, int offset) {

    Scanner scanner = null;

    // create scanner to read from file
    try {
      scanner = new Scanner(new File(path));
    } catch(FileNotFoundException error) {}

    this.n = Math.min(scanner.nextInt(), this.n - offset);
    this.d = scanner.nextInt();

    // ensure window is not too large
    this.windowLength = Math.min(n, windowLength);

    // initialise array to store data
    data = new float[this.n][this.d];

    // move past first offset many points
    for (int i = 0; i < offset*d; i++) {
      scanner.nextFloat();
    }

    // load the data from the file
    for (int i = 0; i < this.n; i++) {
      for (int j = 0; j < this.d; j++) {
        data[i][j] = scanner.nextFloat();
      }
    }

    // close the scanner
    scanner.close();
  }

  // generate a permutation of the points u.a.r
  private void generatePermutation(boolean randomOrder) {

    // create new array and place interegers 0 to n-1
    Integer[] permArray = new Integer[n];
    for (int i = 0; i < n; i++) {
      permArray[i] = i;
    }

    // shuffle the contents of the array
    if (randomOrder) {
      List<Integer> permList = Arrays.asList(permArray);

  		Collections.shuffle(permList);

      permList.toArray(permArray);
    }

    // place into perm
    this.perm = new int[n];
    for (int i = 0; i < n; i++) {
      perm[i] = permArray[i];
    }
  }

  // check if there is an update i
  public int streamLength() {
    return this.n*2;
  }
}
