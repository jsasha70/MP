package jsasha.lt;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;

public class Stats extends LT_dataSrc {

  private int[] noDepend; // vars.length + 1
  private int hChainLenMax;
  private int hChainLenAvg;
  private int[] distMin; // vars.length
  private int[] distMax; // vars.length
  private int[] widthMin; // vars.length
  private int[] widthMax; // vars.length
  private int[] depthMin; // vars.length
  private int[] depthMax; // vars.length
  private int[] count; // vars.length
  private int usedCount1, usedCount2, usedTot1, usedTot2, usedAvg1, usedAvg2;
  private int usedMax1, usedMax2, usedZero1, usedZero2, usedOne1, usedOne2;
  private int usedCountArg1, usedCountArg2, usedTotArg1, usedTotArg2, usedAvgArg1;
  private int usedAvgArg2, usedMaxArg1, usedMaxArg2;
  private int[] listCount, listLenMax, listLenAvg;
//  private long exprNodes;
  private ArrayList<int[]> ss;

  public void init(LogicTree lt, ArrayList<int[]> ss) {
    lt.dataAccess(this);
    initMarks();
    this.ss = ss;
  }

  protected void changed() {
  }

  public void calc(File f) {
    calcChainLen();
    calcDepth2();
    calcNoDepend();
    calcCount();
    calcUsed();
    calcList();
//    calcExprNodes();

    ArrayList<String> txt = new ArrayList();
    String s;

    // заголовок
    s = "";
    txt.add(s);
    s = "Статистика\tmin\tavg\tmax\ttot";
    for (int i = 0; i < vars.length; i++) {
      s += "\t" + (vars.length - i);
    }
    txt.add(s);

    addTabLine("dist min", distMin, txt, false);
    addTabLine("dist max", distMax, txt, false);
    addTabLine("ширина min", widthMin, txt, false);
    addTabLine("ширина max", widthMax, txt, false);
    addTabLine("глубина min", depthMin, txt, false);
    addTabLine("глубина max", depthMax, txt, false);
    addTabLine("node count", count, txt, false);

    addTabLine("Неиспользуемые парам", noDepend, txt, false);

    addTabLine("list count", listCount, txt, false);
    addTabLine("list len avg", listLenAvg, txt, false);
    addTabLine("list len max", listLenMax, txt, false);

    s = "";
    txt.add(s);
    txt.add(s);

    s = "lt size\t\t" + len;
    txt.add(s);
    s = "multy use count 1\t\t" + usedCount1;
    txt.add(s);
    s = "multy use count 2\t\t" + usedCount2;
    txt.add(s);
    s = "multy use tot 1\t\t" + usedTot1;
    txt.add(s);
    s = "multy use tot 2\t\t" + usedTot2;
    txt.add(s);
    s = "multy use avg 1\t\t" + usedAvg1;
    txt.add(s);
    s = "multy use avg 2\t\t" + usedAvg2;
    txt.add(s);
    s = "multy use max 1\t\t" + usedMax1;
    txt.add(s);
    s = "multy use max 2\t\t" + usedMax2;
    txt.add(s);
    s = "used zero 1\t\t" + usedZero1;
    txt.add(s);
    s = "used zero 2\t\t" + usedZero2;
    txt.add(s);
    s = "used one 1\t\t" + usedOne1;
    txt.add(s);
    s = "used one 2\t\t" + usedOne2;
    txt.add(s);
    s = "multy use arg count 1\t\t" + usedCountArg1;
    txt.add(s);
    s = "multy use arg count 2\t\t" + usedCountArg2;
    txt.add(s);
    s = "multy use arg tot 1\t\t" + usedTotArg1;
    txt.add(s);
    s = "multy use arg tot 2\t\t" + usedTotArg2;
    txt.add(s);
    s = "multy use arg avg 1\t\t" + usedAvgArg1;
    txt.add(s);
    s = "multy use arg avg 2\t\t" + usedAvgArg2;
    txt.add(s);
    s = "multy use arg max 1\t\t" + usedMaxArg1;
    txt.add(s);
    s = "multy use arg max 2\t\t" + usedMaxArg2;
    txt.add(s);
//    s = "expression nodes count\t\t" + exprNodes;
//    txt.add(s);
    s = "hash array size\t\t" + hChainLen.length;
    txt.add(s);
    s = "hash chain len avg\t\t" + hChainLenAvg;
    txt.add(s);
    s = "hash chain len max\t\t" + hChainLenMax;
    txt.add(s);

    for (int i = 0; i < stats.length; i++) {
      s = "" + i + ": " + LogicTree.statsName[i] + "\t";
      if (ss == null) {
        s += "\t" + stats[i];
      } else {
        for (int[] s1 : ss) {
          s += "\t" + s1[i];
        }
      }
      txt.add(s);
    }

    try {
      PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(f, false)), false, "cp1251");
      for (String i : txt) {
        ps.println(i);
      }
      ps.close();
    } catch (FileNotFoundException e) {
    } catch (UnsupportedEncodingException e) {
      System.out.println("Error: bad encoding");
      System.exit(6);
    }
  }

  private void addTabLine(String name, int[] v, ArrayList<String> txt, boolean printZero) {
    if ((v.length < vars.length) || (v.length > (vars.length + 1))) {
      return;
    }

    int nn = vars.length;
    boolean haveTot = (v.length == (nn + 1));
    int n1 = -1, n2 = 0, n3 = -1;
    for (int i = 0; i < nn; i++) {
      if ((n1 == -1) || (n1 > v[i])) {
        n1 = v[i];
      }
      if ((n3 == -1) || (n3 < v[i])) {
        n3 = v[i];
      }
      n2 += v[i];
    }
    n2 = n2 / nn;

    String sn1 = printZero || (n1 != 0) ? "" + n1 : "";
    String sn2 = printZero || (n2 != 0) ? "" + n2 : "";
    String sn3 = printZero || (n3 != 0) ? "" + n3 : "";

    String s = name + "\t" + sn1 + "\t" + sn2 + "\t" + sn3;
    if (haveTot && (printZero || (v[nn] != 0))) {
      s += "\t" + v[nn];
    } else {
      s += "\t";
    }

    for (int i = 0; i < nn; i++) {
      if (printZero || (v[i] != 0)) {
        s += "\t" + v[i];
      } else {
        s += "\t";
      }
    }

    txt.add(s);
  }

  private void calcNoDepend() {
    // число параметров, от которых не зависит результат
    noDepend = new int[vars.length + 1]; // последний элемент - для сводной информации
    boolean[] mm = new boolean[args];

    Descr d;
    for (int i = 0; i < vars.length; i++) {
      d = markVar(i);
      for (int j = 0; j < args; j++) {
        if (isMark(d, j + 1)) {
          mm[j] = true;
        } else {
          noDepend[i]++;
        }
      }
    }

    for (int j = 0; j < args; j++) {
      if (!mm[j]) {
        noDepend[vars.length]++;
      }
    }
  }

  private void calcChainLen() {
    hChainLenMax = 0;
    hChainLenAvg = 0;
    for (int i = 0; i < hChainLen.length; i++) {
      hChainLenAvg += hChainLen[i];
      if (hChainLenMax < hChainLen[i]) {
        hChainLenMax = hChainLen[i];
      }
    }
    hChainLenAvg = hChainLenAvg / hChainLen.length;
  }

  private static int minNotZero(int n1, int n2) {
    if ((n1 != 0) & (n2 != 0)) {
      return n1 < n2 ? n1 : n2;
    } else if (n1 != 0) {
      return n1;
    } else if (n2 != 0) {
      return n2;
    } else {
      return 0;
    }
  }

  private static int maxNotZero(int n1, int n2) {
    if ((n1 != 0) & (n2 != 0)) {
      return n1 > n2 ? n1 : n2;
    } else if (n1 != 0) {
      return n1;
    } else if (n2 != 0) {
      return n2;
    } else {
      return 0;
    }
  }

  private void calcDepth2() {
    // глубина дерева для каждой переменной

    int[] nMin = new int[len];
    int[] nMax = new int[len];
    int[] wMin = new int[len];
    int[] wMax = new int[len];
    int[] dMin = new int[len];
    int[] dMax = new int[len];
    int r1, r2, n1, n2;

    for (int i = one; i < len; i++) {
      r1 = rels1[i];
      r2 = rels2[i];

      n1 = 0;
      n1 = (r1 >= 1) && (r1 <= args) ? 1 : (((r1 > args) && (nMin[r1] > 0)) ? nMin[r1] + 1 : 0);
      n2 = 0;
      n2 = (r2 >= 1) && (r2 <= args) ? 1 : (((r2 > args) && (nMin[r2] > 0)) ? nMin[r2] + 1 : 0);
      if ((n1 > 0) || (n2 > 0)) {
        nMin[i] = minNotZero(n1, n2);
      }

      n1 = 0;
      n1 = (r1 >= 1) && (r1 <= args) ? 1 : (((r1 > args) && (nMax[r1] > 0)) ? nMax[r1] + 1 : 0);
      n2 = 0;
      n2 = (r2 >= 1) && (r2 <= args) ? 1 : (((r2 > args) && (nMax[r2] > 0)) ? nMax[r2] + 1 : 0);
      if ((n1 > 0) || (n2 > 0)) {
        nMax[i] = maxNotZero(n1, n2);
      }

      n1 = 0;
      n1 = (r1 >= 1) && (r1 <= args) ? 1 : ((r1 > args) && (wMin[r1] > 0) ? wMin[r1] : 0);
      n2 = 0;
      n2 = (r2 >= 1) && (r2 <= args) ? 1 : ((r2 > args) && (wMin[r2] > 0) ? wMin[r2] + 1 : 0);
      if ((n1 > 0) || (n2 > 0)) {
        wMin[i] = minNotZero(n1, n2);
      }

      n1 = 0;
      n1 = (r1 >= 1) && (r1 <= args) ? 1 : ((r1 > args) && (wMax[r1] > 0) ? wMax[r1] : 0);
      n2 = 0;
      n2 = (r2 >= 1) && (r2 <= args) ? 1 : ((r2 > args) && (wMax[r2] > 0) ? wMax[r2] + 1 : 0);
      if ((n1 > 0) || (n2 > 0)) {
        wMax[i] = maxNotZero(n1, n2);
      }

      n1 = 0;
      n1 = (r1 >= 1) && (r1 <= args) ? 1 : ((r1 > args) && (dMin[r1] > 0) ? dMin[r1] + 1 : 0);
      n2 = 0;
      n2 = (r2 >= 1) && (r2 <= args) ? 1 : ((r2 > args) && (dMin[r2] > 0) ? dMin[r2] : 0);
      if ((n1 > 0) || (n2 > 0)) {
        dMin[i] = minNotZero(n1, n2);
      }

      n1 = 0;
      n1 = (r1 >= 1) && (r1 <= args) ? 1 : ((r1 > args) && (dMax[r1] > 0) ? dMax[r1] + 1 : 0);
      n2 = 0;
      n2 = (r2 >= 1) && (r2 <= args) ? 1 : ((r2 > args) && (dMax[r2] > 0) ? dMax[r2] : 0);
      if ((n1 > 0) || (n2 > 0)) {
        dMax[i] = maxNotZero(n1, n2);
      }
    }

    distMin = new int[vars.length];
    distMax = new int[vars.length];
    widthMin = new int[vars.length];
    widthMax = new int[vars.length];
    depthMin = new int[vars.length];
    depthMax = new int[vars.length];

    for (int i = 0; i < vars.length; i++) {
      distMin[i] = nMin[vars[i]];
      distMax[i] = nMax[vars[i]];
      widthMin[i] = wMin[vars[i]];
      widthMax[i] = wMax[vars[i]];
      depthMin[i] = dMin[vars[i]];
      depthMax[i] = dMax[vars[i]];
    }
  }

  private void calcCount() {
    count = new int[vars.length];

    Descr d;
    for (int i = 0; i < vars.length; i++) {
      d = markVar(i);
      for (int j = 0; j < len; j++) {
        if (isMark(d, j)) {
          count[i]++;
        }
      }
    }
  }

  private void calcUsed() {
    int[] used1 = new int[len];
    int[] used2 = new int[len];

    for (int i = one; i < len; i++) {
      used1[rels1[i]]++;
      used2[rels2[i]]++;
    }

    usedZero1 = used1[0];
    usedZero2 = used2[0];
    usedOne1 = used1[one];
    usedOne2 = used2[one];

    for (int i = 1; i <= args; i++) {
      if (used1[i] > 1) {
        usedCountArg1++;
        usedTotArg1 += used1[i];
        usedMaxArg1 = usedMaxArg1 < used1[i] ? used1[i] : usedMaxArg1;
      }
      if (used2[i] > 1) {
        usedCountArg2++;
        usedTotArg2 += used2[i];
        usedMaxArg2 = usedMaxArg2 < used2[i] ? used2[i] : usedMaxArg2;
      }
    }
    usedAvgArg1 = usedCountArg1 > 0 ? usedTotArg1 / usedCountArg1 : 0;
    usedAvgArg2 = usedCountArg2 > 0 ? usedTotArg2 / usedCountArg2 : 0;

    for (int i = one + 1; i < len; i++) {
      if (used1[i] > 1) {
        usedCount1++;
        usedTot1 += used1[i];
        usedMax1 = usedMax1 < used1[i] ? used1[i] : usedMax1;
      }
      if (used2[i] > 1) {
        usedCount2++;
        usedTot2 += used2[i];
        usedMax2 = usedMax2 < used2[i] ? used2[i] : usedMax2;
      }
    }
    usedAvg1 = usedCount1 > 0 ? usedTot1 / usedCount1 : 0;
    usedAvg2 = usedCount2 > 0 ? usedTot2 / usedCount2 : 0;
  }

  private void calcList() {
    int[] listLen = new int[len];
    int[] used1 = new int[len];

    listCount = new int[vars.length + 1];
    listLenAvg = new int[vars.length + 1];
    listLenMax = new int[vars.length + 1];

    Descr d;
    for (int i = 0; i < vars.length; i++) {
      d = markVar(i);

      Arrays.fill(listLen, 0);
      Arrays.fill(used1, 0);
      for (int j = one; j < len; j++) {
        if (isMark(d, j)) {
          used1[rels1[j]]++;
          if (rels2[j] > 0) {
            listLen[j] = listLen[rels2[j]] + 1;
          }
        }
      }

      for (int j = one; j < len; j++) {
        if (isMark(d, j)) {
          if ((listLen[j] > 1) && (used1[j] > 0)) {
            listCount[i]++;
            listLenAvg[i] += listLen[j];
            listLenMax[i] = listLenMax[i] < listLen[j] ? listLen[j] : listLenMax[i];
          }
        }
      }
      if (listCount[i] > 0) {
        listLenAvg[i] = listLenAvg[i] / listCount[i];
      }
    }

    Arrays.fill(listLen, 0);
    Arrays.fill(used1, 0);
    for (int j = one; j < len; j++) {
      used1[rels1[j]]++;
      if (rels2[j] > 0) {
        listLen[j] = listLen[rels2[j]] + 1;
      }
    }
    for (int j = one; j < len; j++) {
      if ((listLen[j] > 1) && (used1[j] > 0)) {
        listCount[vars.length]++;
        listLenAvg[vars.length] += listLen[j];
        listLenMax[vars.length] = listLenMax[vars.length] < listLen[j] ? listLen[j] : listLenMax[vars.length];
      }
    }
    if (listCount[vars.length] > 0) {
      listLenAvg[vars.length] = listLenAvg[vars.length] / listCount[vars.length];
    }
  }
//  private void calcExprNodes() {
//    long[] cc = new long[len];
//    Arrays.fill(cc, 0);
//
//    for (int i = one; i < len; i++) {
//      cc[i] = cc[rels1[i]] + cc[rels2[i]] + 1;
//    }
//
//    exprNodes = 0;
//    for (int i = 0; i < vars.length; i++) {
//      exprNodes += cc[vars[i]];
//    }
//  }
}
