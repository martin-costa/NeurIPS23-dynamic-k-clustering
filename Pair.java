// simple pair class
class Pair implements Comparable<Pair> {

  // stores and integer and a float
  public int l;
  public float r;

  Pair(int l, float r) {
    this.l = l;
    this.r = r;
  }

  // compares by looking at the value of the float
  @Override
  public int compareTo(Pair p){
    float d = this.r - p.r;
    if (d < 0)
      return -1;
    if (d > 0)
      return 1;
    return 0;
  }

  @Override
  public String toString() {
    return "[" + Integer.toString(l) + ", " + Float.toString(r) + "]";
  }
}
