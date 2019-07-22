package jsasha.old;

import java.util.Arrays;
import jsasha.lt.LogicTree;
import jsasha.old.LT_dataSrc_old;

public class LT_mark extends LT_dataSrc_old {

  private static final int lenLog = 11; // 11 - число буферов - 2048
  private int[][] mm = null;
  private Descr[] dd = null;
  private static final int maskNodeNum = (1 << lenLog) - 1;
  // вспомогательные переменные
  private int[] stack = null;

  public LT_mark() {
    mm = new int[1 << (lenLog - 5)][];
    dd = new Descr[1 << lenLog];
    for (int i = 0; i < dd.length; i++) {
      dd[i] = new Descr(i);
    }
  }

  protected void changed() {
    stack = stack == null ? new int[len] : Arrays.copyOf(stack, len);
    for (int i = 0; i < mm.length; i++) {
      mm[i] = mm[i] == null ? new int[len] : Arrays.copyOf(mm[i], len);
    }
  }

  public void init(LogicTree lt) {
//    lt.dataAccess(this); !!! было раскомментировано
  }

  private void clearAndCopyMarks(Descr from, Descr to) {
    int n1, n2;
    if (to.count == 0) {
      n1 = from.minMark;
      n2 = from.maxMark;
    } else {
      n1 = getMinMark(from, to);
      n2 = getMaxMark(from, to);
    }

    int[] fromAr = mm[from.ar];
    int[] toAr = mm[to.ar];
    int fromMask1 = from.mask1;
    int toMask1 = to.mask1;
    int toMask0 = to.mask0;

    for (int i = n1; i <= n2; i++) {
      if ((fromAr[i] & fromMask1) == 0) {
        if ((toAr[i] & toMask1) != 0) {
          toAr[i] &= toMask0; // сброс бита
        }
      } else {
        if ((toAr[i] & toMask1) == 0) {
          toAr[i] |= toMask1; // установка бита
        }
      }
    }
    to.count = from.count;
    to.minMark = from.minMark;
    to.maxMark = from.maxMark;
  }

  private int getMinMark(Descr d1, Descr d2) {
    int ret = d1.minMark < d2.minMark ? d1.minMark : d2.minMark;
    if (ret == 0) {
      ret = d1.minMark > d2.minMark ? d1.minMark : d2.minMark;
    }
    return ret;
  }

  private int getMaxMark(Descr d1, Descr d2) {
    int ret = d1.maxMark > d2.maxMark ? d1.maxMark : d2.maxMark;
    return ret;
  }

  private void addMarks(Descr from, Descr to) {
    int n1 = from.minMark;
    int n2 = from.maxMark;
    int[] fromAr = mm[from.ar];
    int[] toAr = mm[to.ar];
    int fromMask1 = from.mask1;
    int toMask1 = to.mask1;
    int toCount = to.count;

    for (int i = n1; i <= n2; i++) {
      if (((fromAr[i] & fromMask1) != 0)
              && ((toAr[i] & toMask1) == 0)) {
        toAr[i] |= toMask1; // установка бита
        toCount++;
      }
    }

    to.count = toCount;
    to.minMark = getMinMark(from, to);
    to.maxMark = getMaxMark(from, to);
  }

  private Descr find(int node) {
    if (node == 0) {
      return null;
    }
    Descr d = dd[node & maskNodeNum];
    return d.node == node ? d : null;
  }

  private void markNode(Descr d, int node) {
    if (node == 0) {
      return;
    }

    if ((mm[d.ar][node] & d.mask1) == 0) {
      mm[d.ar][node] |= d.mask1;

      if (d.count == 0) {
        d.minMark = node;
        d.maxMark = node;
      } else {
        d.minMark = d.minMark < node ? d.minMark : node;
        d.maxMark = d.maxMark > node ? d.maxMark : node;
      }
      d.count++;
    }
  }

  public Descr markNodeTree(int node) {
    Descr d = find(node);
    if (d != null) {
      return d;
    }
    d = dd[node & maskNodeNum];
    d.node = node;

    if (node == 0) {
      markClearAll(d);
      return d;
    }

    int rel1 = rels1[node];
    int rel2 = rels2[node];
    Descr d1 = find(rel1);
    Descr d2 = find(rel2);

    if ((d1 != null) && (d2 != null)) {
      clearAndCopyMarks(d1, d);
      addMarks(d2, d);
      markNode(d, node);
    } else if ((d1 != null) && (d2 == null)) {
      clearAndCopyMarks(d1, d);
      markTree(d, rel2);
      markNode(d, node);
    } else if ((d1 == null) && (d2 != null)) {
      clearAndCopyMarks(d2, d);
      markTree(d, rel1);
      markNode(d, node);
    } else {
      markClearAll(d);
      markTree(d, node);
    }

    return d;
  }

  private void markClearAll(Descr d) {
    int n1 = d.minMark;
    int n2 = d.maxMark;
    int mask0 = d.mask0;
    int mask1 = d.mask1;
    int[] ar = mm[d.ar];
    for (int i = n1; i <= n2; i++) {
      if ((ar[i] & mask1) != 0) {
        ar[i] &= mask0;
      }
    }

    d.count = 0;
    d.minMark = 0;
    d.maxMark = 0;
  }

  private void markTree(Descr d, int node) {
    int mask1 = d.mask1;
    int[] ar = mm[d.ar];

    if ((node == 0) || ((ar[node] & mask1) != 0)) {
      return;
    }

    int n1 = d.minMark == 0 ? node : d.minMark;
    int nn = d.count;
    int i = 0;
    stack[0] = node;

    int r1, r2, nd;

    while (true) {
      nd = stack[i];
      if ((ar[nd] & mask1) == 0) {
        ar[nd] |= mask1;
        nn++;
        n1 = n1 < nd ? n1 : nd;
      }

      r1 = rels1[stack[i]];
      if ((r1 > 0) && ((ar[r1] & mask1) == 0)) {
        i++;
        stack[i] = r1;
      } else {
        r2 = rels2[stack[i]];
        if ((r2 > 0) && ((ar[r2] & mask1) == 0)) {
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

    d.count = nn;
    d.minMark = n1;
    d.maxMark = d.maxMark > node ? d.maxMark : node;
  }

  public static class Descr {

    // оисание соержания масива (меняется):
    public int node = -1;
    public int maxMark = 0;
    public int minMark = 0;
    public int count = 0;
    // описание положения массива (не меняется):
    public int bit = 0;
    public int ar = 0;
    public int mask1 = 1;
    public int mask0 = ~mask1;

    public Descr(int i) {
      ar = i >> 5;
      bit = i & 31;
      mask1 = 1 << bit;
      mask0 = ~mask1;
    }
  }
}
