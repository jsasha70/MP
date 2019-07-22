package jsasha.lt.test;

import jsasha.lt.LT_dataSrc;
import jsasha.lt.LogicTree;
import jsasha.util.ListInt;

/**
 * нахождение связей между списками, число этих связей
 * 
 */
public class Test5 extends LT_dataSrc {

  protected void changed() {
  }

  public void run(LogicTree lt) {
    lt.dataAccess(this);
    calcLists();

    System.out.println("Up list:");
    calc(upList);

    System.out.println("sub list:");
    calc(subList);
  }

  private void calc(ListInt[] lst) {
    int relCount = 0;
    int relTot = 0;
    int relMax = 0;
    int maxNode = 0;
    int nn = 0;
    int llen;
    for (int i = one + 1; i < len; i++) {
      if (lst[i] != null) {
        llen = lst[i].getLen();
        relCount++;
        relTot += llen;

        if (relMax < llen) {
          relMax = llen;
          maxNode = i;
        }

        if (nn < 5) {
          nn++;
          for (int j = 0; j < llen; j++) {
            System.out.println("" + i + "\t" + lst[i].get(j));
          }
        }
        if (nn == 5) {
          nn++;
          System.out.println("...");
        }
      }
    }

    int relAvg = 0;
    if (relCount > 0) {
      relAvg = relTot / relCount;
    }

    System.out.println("count: " + relCount + "; tot: " + relTot
            + "; avg: " + relAvg + "; max: " + relMax + " (" + maxNode + ")");
    System.out.println();
  }
}
