package jsasha.lt;

import java.io.PrintStream;
import java.util.Arrays;
import jsasha.util.ListInt;

public abstract class LT_dataSrc {

  protected int[] rels1; // from LogicTree
  protected int[] rels2; // from LogicTree
  protected int args; // from LogicTree
  protected int len; // from LogicTree
  protected int[] vars; // from LogicTree
  protected int one; // from LogicTree
  protected int[] hIndex; // from LT_hash
  protected int[] hChainLen; // from LT_hash
  protected int[] hPrev; // from LT_hash
  protected int[] stats;
  // поиск использования узлов
  private boolean usageInit = false;
  protected int[] lastUsed1 = null; // последнее использование узла в левой ссылке
  protected int[] lastUsed2 = null; // последнее использование узла в правой ссылке
  protected int[] prevUsed1 = null; // предыдущие использования узла в левой ссылке
  protected int[] prevUsed2 = null; // предыдущие использования узла в правой ссылке
  protected int[] countUsed1 = null;
  protected int[] countUsed2 = null;
  // связи между списками
  private boolean listInit = false;
  protected ListInt[] subList = null;
  protected ListInt[] upList = null;
  // отметки на ДЛ:
  private boolean markInit = false;
  private int[][] mm = null;
  private Descr[] dd = null;
  private static final int lenLog = 10; // 11 - число буферов - 2048
  private static final int maskNodeNum = (1 << lenLog) - 1;
  private int[] stack = null; // вспомогательные переменные (для отметок)
  // минимальная глубина (до аргумента)
  private boolean depthInit = false;
  protected int[] minDepth = null;
  protected int[] minDist = null;

  public LT_dataSrc() {
  }

  public void addData(int node, int rel1, int rel2, int len) {
    this.len = len;

    if (usageInit) {
      nodeUsageCalc(node);
    }

    if (depthInit) {
      nodeDepthCalc(node);
    }
  }

  private void nodeUsageCalc(int node) {
    // учет использования ссылок на компоненты узла

    int rel1 = rels1[node];
    int rel2 = rels2[node];

    if (rel1 >= 0) {
      prevUsed1[node] = lastUsed1[rel1];
      lastUsed1[rel1] = node;
      lastUsed1[node] = -1;
      countUsed1[rel1]++;
    }

    if (rel2 >= 0) {
      prevUsed2[node] = lastUsed2[rel2];
      lastUsed2[rel2] = node;
      lastUsed2[node] = -1;
      countUsed2[rel2]++;
    }
  }

  public void initMarks() {
    markInit = true;
    if (mm == null) {
      mm = new int[1 << (lenLog - 5)][];
      dd = new Descr[1 << lenLog];
      for (int i = 0; i < dd.length; i++) {
        dd[i] = new Descr(i);
      }
    }
    stack = stack == null ? new int[rels1.length] : Arrays.copyOf(stack, rels1.length);
    for (int i = 0; i < mm.length; i++) {
      mm[i] = mm[i] == null ? new int[rels1.length] : Arrays.copyOf(mm[i], rels1.length);
    }
  }

  private void initLists() {
    listInit = true;
    int prevLen = subList == null ? 0 : subList.length;
    if (prevLen < rels1.length) {
      subList = subList == null ? new ListInt[rels1.length] : Arrays.copyOf(subList, rels1.length);
      upList = upList == null ? new ListInt[rels1.length] : Arrays.copyOf(upList, rels1.length);
    }
  }

  private void initUsage() {
    usageInit = true;
    int prevLen = lastUsed1 == null ? 0 : lastUsed1.length;
    if (prevLen < rels1.length) {
      lastUsed1 = lastUsed1 == null ? new int[rels1.length] : Arrays.copyOf(lastUsed1, rels1.length);
      lastUsed2 = lastUsed2 == null ? new int[rels1.length] : Arrays.copyOf(lastUsed2, rels1.length);
      prevUsed1 = prevUsed1 == null ? new int[rels1.length] : Arrays.copyOf(prevUsed1, rels1.length);
      prevUsed2 = prevUsed2 == null ? new int[rels1.length] : Arrays.copyOf(prevUsed2, rels1.length);
      countUsed1 = countUsed1 == null ? new int[rels1.length] : Arrays.copyOf(countUsed1, rels1.length);
      countUsed2 = countUsed2 == null ? new int[rels1.length] : Arrays.copyOf(countUsed2, rels1.length);
      Arrays.fill(lastUsed1, prevLen, len, -1);
      Arrays.fill(lastUsed2, prevLen, len, -1);
      Arrays.fill(prevUsed1, prevLen, len, -1);
      Arrays.fill(prevUsed2, prevLen, len, -1);
      Arrays.fill(countUsed1, prevLen, len, 0);
      Arrays.fill(countUsed2, prevLen, len, 0);
    }
  }

  public void calcUsage() {
    initUsage();
    for (int i = 0; i < len; i++) {
      nodeUsageCalc(i);
    }
  }

  private void initDepth() {
    depthInit = true;
    int prevLen = minDepth == null ? 0 : minDepth.length;
    if (prevLen < rels1.length) {
      minDepth = minDepth == null ? new int[rels1.length] : Arrays.copyOf(minDepth, rels1.length);
      minDist = minDist == null ? new int[rels1.length] : Arrays.copyOf(minDist, rels1.length);
      Arrays.fill(minDepth, prevLen, len, -1);
      Arrays.fill(minDist, prevLen, len, -1);
    }
  }

  public void calcDepth() {
    initDepth();
    for (int i = 0; i < len; i++) {
      nodeDepthCalc(i);
    }
  }

  private void nodeDepthCalc(int node) {
    int rel1 = rels1[node];
    int rel2 = rels2[node];

    if (node <= one) {
      minDepth[node] = 0;
      minDist[node] = 0;
    } else if (((rel1 > 0) && (rel1 <= args)) || ((rel2 > 0) && (rel2 <= args))) {
      minDepth[node] = 1;
      minDist[node] = 1;
    } else {
      int n1 = minDepth[rel1];
      int n2 = minDepth[rel2];

      n1 = n1 == 0 ? n2 : n1 + 1;
      n1 = (n2 > 0) && (n2 < n1) ? n2 : n1;
      minDepth[node] = n1;

      n1 = minDist[rel1];
      n2 = minDist[rel2];

      n2 = n2 == 0 ? 0 : n2 + 1;
      n1 = n1 == 0 ? n2 : n1 + 1;
      n1 = (n2 > 0) && (n2 < n1) ? n2 : n1;
      minDist[node] = n1;
    }
  }

  public void ltData(int[] rels1, int[] rels2, int args, int len, int[] vars, int one, int[] stats) {
    this.rels1 = rels1;
    this.rels2 = rels2;
    this.args = args;
    this.len = len;
    this.vars = vars;
    this.one = one;
    this.stats = stats;

    if (markInit) {
      initMarks();
    }

    if (usageInit) {
      initUsage();
    }

    if (depthInit) {
      initDepth();
    }

    if (listInit) {
      initLists();
    }

    changed();
  }

  public void hashData(int[] index, int[] chainLen, int[] prev) {
    hIndex = index;
    hChainLen = chainLen;
    hPrev = prev;
  }

  protected abstract void changed();

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

  public Descr markVar(int v) {
    return markNodeTree(vars[v]);
  }

  public boolean isMark(Descr d, int node) {
    return (mm[d.ar][node] & d.mask1) != 0;
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

  public void calcLists() {
    initLists();

    if (!usageInit) {
      calcUsage();
    }
    ListInt[] pp = new ListInt[rels1.length];

    int u;
    for (int i = one + 1; i < len; i++) {
      // находим части списков, куда входит этот список
      u = lastUsed1[rels1[i]];
      while (u > 0) {
        // проверяем, что список u содержит список i
        if ((u != i) && isSublist(i, u)) {
          if (pp[u] == null) {
            pp[u] = new ListInt();
          }
          pp[u].add(i);
        }
        u = prevUsed1[u];
      }
    }

    ListInt ll, ll2;
    int rel2, llen, nrr;
    int[] rr;
    for (int i = one + 1; i < len; i++) {
      if (pp[i] == null) {
        ll = new ListInt();
      } else {
        ll = pp[i];
      }

      rel2 = rels2[i];
      while (rel2 > args) {
        if (pp[rel2] != null) {
          ll.addSorted(pp[rel2]);
        }
        rel2 = rels2[rel2];
      }

      llen = ll.getLen();
      if (llen > 0) {
        ll.sort();
        nrr = 0;
        rr = ll.getAr();
        for (int j = 0; j < llen; j++) {
          if (rels2[rr[j]] > args) {
            // сохраняем ссылки только на под-списки длиной более единицы
            rr[nrr] = rr[j];
            nrr++;
          }
        }
        ll.setLen(nrr);

        llen = ll.getLen();
        if (llen > 0) {
          subList[i] = ll;
          rr = ll.getAr();
          for (int j = 0; j < llen; j++) {
            ll2 = upList[rr[j]];
            if (ll2 == null) {
              ll2 = new ListInt();
              upList[rr[j]] = ll2;
            }
            ll2.add(i);
          }
        }
      }
    }
    for (int i = one + 1; i < len; i++) {
      if (upList[i] != null) {
        upList[i].sort();
      }
    }
  }

  private boolean isSublist(int lst1, int lst2) {
    // проверка что lst2 содержит lst1
    int r1_1 = rels1[lst1];
    int r1_2 = rels2[lst1];
    int r2_1 = rels1[lst2];
    int r2_2 = rels2[lst2];

    while ((r1_1 == r2_1) && (r1_2 > 0) && (r2_2 > 0)) {
      r1_1 = rels1[r1_2];
      r1_2 = rels2[r1_2];

      while (r2_1 > r1_1) {
        r2_1 = rels1[r2_2];
        r2_2 = rels2[r2_2];
      }
    }

    return (r1_1 == r2_1) && (r1_2 == 0);
  }

  public void printNodes(PrintStream ps, boolean haveVal[], boolean val[]) {
    String s;
    int rel1, rel2;
    String sp = "                                                                ";
    int nsp = 3 + ("" + (len - 1)).length() * 2;
    int n;
    for (int i = 0; i < len; i++) {
      s = "(" + rels1[i] + "," + rels2[i] + ")";
      n = nsp - s.length();
      n = n < 0 ? 0 : n;
      s = "" + i + "\t" + s + sp.substring(0, n);
      if (haveVal != null) {
        if (haveVal[i]) {
          rel1 = rels1[i];
          rel2 = rels2[i];
          s += "\t" + (val[i] ? "1" : "0") + "\t"
                  + (rel1 >= 0 && rel2 >= 0 ? "("
                  + (haveVal[rel1] ? (val[rel1] ? "1" : "0") : " ") + ","
                  + (haveVal[rel2] ? (val[rel2] ? "1" : "0") : " ") + ")" : "");
        } else {
          s += "\t\t";
        }
      }
      if ((i > one) && depthInit) {
        s += "\t" + minDepth[i] + "/" + minDist[i];
      }
      ps.println(s);
    }
  }
}
