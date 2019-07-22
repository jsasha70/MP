package jsasha.lt.test;

import java.util.Arrays;
import java.util.Random;
import jsasha.lt.LogicTree;

/**
 * поиск неподвижных точек случайным перебором
 * 
 */
public class Test1 {

  private int[] rels1;
  private int[] rels2;
  private boolean[] val;
  private boolean[] val1;
  private boolean[] m;
  private boolean[] fixed;
  private int len;
  private int args;
  private int one;

  public void run(LogicTree lt) {
    len = lt.getLen();
    args = lt.getArgs();
    one = lt.one;

    rels1 = lt.arrayRels1();
    rels2 = lt.arrayRels2();

    val = new boolean[len];
    val1 = new boolean[len];
    m = new boolean[len];
    fixed = new boolean[len];

    Arrays.fill(fixed, true);
    Arrays.fill(m, true);
    genArgs();
    calc();
    System.arraycopy(val, 0, val1, 0, len);

    int dif;
    int n1 = len + 1;
    int n = 0;
    int stepCount = 0;
    int idleCount = 1;
    while (idleCount > 0) {
      stepCount++;
//      if (stepCount > 100) {
//        markFixed();
//      } else {
      Arrays.fill(m, true);
//      }
      genArgs();
      calc();
      n = compare();
      if (n == 0) {
        break;
      }
      dif = n1 - n;
      n1 = n;
      if (dif == 0) {
        idleCount--;
      } else {
        idleCount = stepCount + 100;
        System.out.println("step " + stepCount + "; fixed " + n + "; dif " + dif);
      }
    }
    System.out.println("step " + stepCount + "; fixed " + n);

    if (n > 0) {
      n = 10;
      for (int i = one + 1; i < len; i++) {
        if (fixed[i]) {
          System.out.println(i);
          n--;
          if (n <= 0) {
            break;
          }
        }
      }
    }
  }

  private int compare() {
    int ret = 0;
    for (int i = one + 1; i < len; i++) {
      if (fixed[i]) {
        if ((val[i] == val1[i])) {
          ret++;
        } else {
          fixed[i] = false;
        }
      }
    }
    return ret;
  }

  private void genArgs() {
    val[0] = false;
    val[args + 1] = true;
    Random rnd = new Random();
    for (int i = 1; i <= args; i++) {
      val[i] = rnd.nextBoolean();
    }
  }

  private void calc() {
    for (int i = one + 1; i < len; i++) {
      if (m[i]) {
        val[i] = val[rels2[i]] || !val[rels1[i]];
      }
    }
  }

//  private void markFixed() {
//    Arrays.fill(m, false);
//    for (int i = one + 1; i < len; i++) {
//      if (fixed[i]) {
//        markTree(i);
//      }
//    }
//  }

  protected void markTree(int node) {
    if (m[node]) {
      return;
    }

    if (node == 0) {
      m[node] = true;
      return;
    }

    int[] stack = new int[node];
    int i = 0;
    stack[0] = node;

    int r1, r2;

    while (true) {
      m[stack[i]] = true;

      r1 = rels1[stack[i]];
      if ((r1 >= 0) && !m[r1]) {
        i++;
        stack[i] = r1;
      } else {
        r2 = rels2[stack[i]];
        if ((r2 >= 0) && !m[r2]) {
          i++;
          stack[i] = r2;
        } else {
          i--;
          if (i < 0) {
            break;
          }
        }
      }
    }
  }
}
