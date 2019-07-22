package jsasha.lt.test;

import jsasha.lt.LT_dataSrc;
import jsasha.lt.LogicTree;

/**
 * проверка быстродействия работы с отметками
 * 
 */
public class Test4 extends LT_dataSrc {

  protected void changed() {
  }

  public void run(LogicTree lt) {
    lt.dataAccess(this);
    initMarks();
    int[] res = new int[len];
    Descr d;

    long dt1 = System.currentTimeMillis();
    long dt2 = dt1;

    for (int i = 1; i < len; i++) {
      d = markNodeTree(i);
      res[i] = d.count;

      if ((i % 10000) == 0) {
        System.out.println("  " + (i / 1000) + " 000: "
                + ((System.currentTimeMillis() - dt2) / 1000) + " s");
        dt2 = System.currentTimeMillis();
      }
    }

    dt1 = (System.currentTimeMillis() - dt1) / 1000;
    System.out.println("done");

    for (int i = 0; i < 10; i++) {
      if (i >= lt.vars.length) {
        break;
      }
      System.out.println("  " + (lt.vars.length - i) + ": " + res[lt.vars[i]]);
    }

    System.out.println("exec time: " + dt1 + " s");
    System.out.println("exec time: " + (dt1 / 60) + " m " + (dt1 % 60) + " s");
  }
}
