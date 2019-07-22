package jsasha.lt.test;

import java.util.Arrays;
import jsasha.lt.LogicTree;

/**
 * проверка дублей узлов
 * 
 */
public class Test3 {

  private int[] rels1;
  private int[] rels2;
  private boolean[] m;
  private int len;
  private int args;
  private int one;
  private int[] vars;

  public void run(LogicTree lt) {
    len = lt.getLen();
    args = lt.getArgs();
    one = lt.one;
    vars = lt.vars;

    rels1 = lt.arrayRels1();
    rels2 = lt.arrayRels2();

    System.out.println("args " + args + ", " + "len " + len);

    int n1 = checkDoubles();
    int n2 = checkWaste();

    System.out.println("checkDoubles errors: " + n1);
    System.out.println("checkWaste errors: " + n2);
  }

  private int checkDoubles() {
    System.out.println("checkDoubles:");

    long[] rr = new long[len - one];
    long r1, r2;

    for (int i = 0; i < rr.length; i++) {
      r1 = rels1[i + one];
      r2 = rels2[i + one];

      rr[i] = (r1 << 24) | r2;
    }

    Arrays.sort(rr);

    long prev = -2, nn = 0;
    int ret = 0;
    for (int i = 0; i < rr.length; i++) {
      if (rr[i] == prev) {
        ret++;
        nn++;
        r1 = rr[i] >> 24;
        r2 = rr[i] & 0xffffff;
        System.out.println("(" + r1 + "," + r2 + "): " + nn);
      } else {
        prev = rr[i];
        nn = 1;
      }
    }

    System.out.println("checkDoubles done");
    return ret;
  }

  private int checkWaste() {
    System.out.println("checkWaste:");

    m = new boolean[len];
    Arrays.fill(m, false);

    boolean[] toDo = new boolean[len];
    Arrays.fill(toDo, false);

    for (int i = 0; i < vars.length; i++) {
      toDo[vars[i]] = true;
    }

    int done = 1, r1, r2;
    while (done > 0) {
      done = 0;

      for (int i = 0; i < len; i++) {
        if (toDo[i]) {
          done++;
          m[i] = true;
          toDo[i] = false;
          r1 = rels1[i];
          r2 = rels2[i];
          if ((r1 >= 0) && !m[r1]) {
            toDo[r1] = true;
          }
          if ((r2 >= 0) && !m[r2]) {
            toDo[r2] = true;
          }
        }
      }
    }

    int ret = 0;
    int nn = 0;
    for (int i = 0; i < len; i++) {
      if (!m[i]) {
        ret++;
        if (nn < 10) {
          System.out.println(i);
        } else if (nn == 10) {
          System.out.println("...");
        }
        nn++;
      }
    }

    System.out.println("checkWaste done");
    return ret;
  }
}
