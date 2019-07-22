package jsasha.old;

import java.util.Arrays;
import jsasha.lt.LT_dataSrc;
import jsasha.lt.LogicTree;

public class LT_mark_old extends LT_dataSrc {

  private int[] mm;
  private int[] stack;
  private int[] mask0 = null;
  private int[] mask1 = null;
  public int counter;

  protected void changed() {
  }

  public void init(LogicTree lt) {
    lt.dataAccess(this);
    mm = new int[len];
    stack = new int[len];

    mask0 = new int[32];
    mask1 = new int[32];
    for (int i = 0; i < 32; i++) {
      mask1[i] = (1 << i);
      mask0[i] = ~mask1[i];
    }
  }

  public void markAll(int idx) {
    int m1 = mask1[idx];
    for (int i = 0; i < len; i++) {
      mm[i] |= m1;
    }
  }

  public void markClearAll(int idx) {
    int m0 = mask0[idx];
    for (int i = 0; i < len; i++) {
      mm[i] &= m0;
    }
    counter = 0;
  }

  public void markClearAll() {
    Arrays.fill(mm, 0);
    counter = 0;
  }

  public void markVar(int idx, int i) {
    markClearAll(idx);
    markTree(idx, vars[i]);
  }

  public int markCount(int idx) {
    int m1 = mask1[idx];
    int ret = 0;
    for (int i = 0; i < len; i++) {
      if ((mm[i] & m1) != 0) {
        ret++;
      }
    }
    return ret;
  }

  public void markTree(int idx, int node) {
    int m1 = mask1[idx];

    if ((node == 0) || ((mm[node] & m1) != 0)) {
      return;
    }

    int i = 0;
    stack[0] = node;

    int r1, r2, nd;

    while (true) {
      nd = stack[i];
      if ((mm[nd] & m1) == 0) {
        mm[nd] |= m1;
        counter++;
      }

      r1 = rels1[stack[i]];
      if ((r1 > 0) && ((mm[r1] & m1) == 0)) {
        i++;
        stack[i] = r1;
      } else {
        r2 = rels2[stack[i]];
        if ((r2 > 0) && ((mm[r2] & m1) == 0)) {
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
