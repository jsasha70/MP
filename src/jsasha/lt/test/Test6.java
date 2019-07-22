package jsasha.lt.test;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import jsasha.lt.LT_dataSrc;
import jsasha.lt.LogicTree;
import jsasha.util.ListInt;

/**
 * вычисления обратным ходом (со статистикой)
 * 
 */
public class Test6 extends LT_dataSrc {

  private boolean[] haveVal;
  private boolean[] haveAll;
  private int errNode;
  private boolean[] val;
  private ListInt q1;
  private ListInt q2;
  private PrintStream logPS = null;

  protected void changed() {
  }

  public void run(LogicTree lt, String fname1, String[] pp) {
    BigInteger argVal = BigInteger.ZERO;
    BigInteger argMask = BigInteger.ZERO;
    BigInteger varVal = BigInteger.ZERO;
    BigInteger varMask = BigInteger.ZERO;

    varVal = new BigInteger("41f1c4ddd1183083b48396129dec579e9b7ae61bcf24b743cfe59b7d558a2676", 16);
    varMask = new BigInteger("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);

//    varVal = new BigInteger("10", 2);
//    varMask = new BigInteger("11", 2);
//
//    argVal = new BigInteger("0001", 2);
//    argMask = new BigInteger("0011", 2);

    doRun(lt, fname1, argMask, argVal, varMask, varVal,
            (pp != null) && (pp.length > 0) ? pp[0] : null);
  }

  private void doRun(LogicTree lt, String fname1,
          BigInteger argMask, BigInteger argVal,
          BigInteger varMask, BigInteger varVal,
          String setNd) {

    long dt1 = System.currentTimeMillis();
//    logOpen(fname1);

    lt.dataAccess(this);
    calcLists();
    calcDepth();

    ListInt rr = new ListInt(len * 3 / 2);
    ListInt nd;
    int nFixed = 0;
    int len1 = len;
    int n, node1;
    for (int i = 0; i < len; i++) {
      nd = subList[i];
      if (nd != null) {
        n = nd.getLen();
        for (int j = 0; j < n; j++) {
          node1 = lt.addRule(nd.get(j), i);
          rr.add(node1);
          if (node1 < len1) {
            nFixed++;
          }
        }
      }
    }
    System.out.println("fixed nodes (list rels): " + nFixed + "; old len: " + len1 + "; new len: " + len);
//    calcUsage();

    haveVal = new boolean[len];
    haveAll = new boolean[len];
    val = new boolean[len];
    errNode = 0;
    q1 = new ListInt(10000); // только что присвоенные значения (нужно распространять дальше)
    q2 = new ListInt(10000); // какие значения нужно попытаться вычислить
    // потом можно использовать только первую очередь, а вторую обрабатывать сразу

    // записываем нчения нуля и единицы
    haveVal[one] = true;
    haveAll[one] = true;
    val[one] = true;
    haveVal[0] = true;
    haveAll[0] = true;
    val[0] = false;

    // записываем значения параметров
    for (int i = 0; i < args; i++) {
      if (argMask.testBit(i)) {
        setVal(args - i, argVal.testBit(i));
      }
    }

    // записываем значения переменных
    for (int i = 0; i < lt.vars.length; i++) {
      if (varMask.testBit(lt.vars.length - i - 1)) {
        setVal(lt.vars[i], varVal.testBit(lt.vars.length - i - 1));
      }
    }

    // записываем единицы по связям между списками
    while (rr.getLen() > 0) {
      node1 = rr.pop();
      setVal(node1, true);
    }

    setNodes(setNd);

    long dt2 = System.currentTimeMillis();
    int step = 0;
    while ((errNode == 0) && ((q1.getLen() > 0) || (q2.getLen() > 0))) {
//      if ((step % 10) == 0) {
//        System.out.print("step " + step + ": q1 " + q1.getLen());
//      }

      if (q1.getLen() > 0) {
        handleQ1();
      }
      q2.sort();

//      if ((step % 10) == 0) {
//        System.out.println(" q2 " + q2.getLen()
//                + " (" + ((System.currentTimeMillis() - dt2) / 1000) + " s)");
//        dt2 = System.currentTimeMillis();
//      }

      if (q2.getLen() > 0) {
        handleQ2();
      }
      q1.sort();

      step++;
    }

    dt1 = (System.currentTimeMillis() - dt1) / 1000;

    int n1 = 0, n2 = 0, n3 = 0, n4 = 0;
    for (int i = 1; i <= args; i++) {
      if (haveVal[i]) {
        n3++;
      } else {
        n4++;
      }
    }
    for (int i = args + 1; i < len1; i++) {
      if (haveVal[i]) {
        n1++;
      } else {
        n2++;
      }
    }

    logClose();

    if (errNode > 0) {
      System.out.println("! err node " + errNode + " !");
    }
    System.out.println("arg have val " + n3 + "; arg no val " + n4);
    System.out.println("have val " + (n1 - vars.length) + "; no val " + n2);
    System.out.println();

    checkVals();

    System.out.println();

//    printMinDepth(len1, len, " (list rels)");
//    System.out.println();
    printMinDepth(one + 1, len1, "");

    System.out.println();
    System.out.println("done; steps: " + step);

    System.out.println("exec time: " + dt1 + " s");
//    System.out.println("exec time: " + (dt1 / 60) + " m " + (dt1 % 60) + " s");

    PrintStream ps = null;
    try {
      ps = new PrintStream(
              new BufferedOutputStream(
              new FileOutputStream(fname1 + ".6.log", false), 32000));
      printNodes(ps, haveVal, val);
    } catch (Exception e) {
    } finally {
      if (ps != null) {
        ps.close();
      }
    }
  }

  private void handleQ1() {
    // q1 - только что вычисленные значения
    // помещаем в q2 то что м.б. можно из них получить

    int node, r, rel1, rel2;
    while (q1.getLen() > 0) {
      node = q1.pop();
      rel1 = rels1[node];
      rel2 = rels2[node];
      log("Q1:\t" + node + " (" + rel1 + "," + rel2 + ")");

      // использование данного узла
      r = lastUsed1[node];
      while (r > args) {
        if (!haveAll[r]) {
          log("\tq2.add:\t" + r);
          q2.addLazySorted(r);
          if (haveVal[r] && !haveVal[rels2[r]]) {
            log("\tq2.add:\t" + rels2[r]);
            q2.addLazySorted(rels2[r]);
          }
        }
        r = prevUsed1[r];
      }
      r = lastUsed2[node];
      while (r > args) {
        if (!haveAll[r]) {
          log("\tq2.add:\t" + r);
          q2.addLazySorted(r);
          if (haveVal[r] && !haveVal[rels1[r]]) {
            log("\tq2.add:\t" + rels1[r]);
            q2.addLazySorted(rels1[r]);
          }
        }
        r = prevUsed2[r];
      }

      // что использует данный узел
      if ((rel1 > 0) && !haveAll[rel1]) {
        log("\tq2.add:\t" + rel1);
        q2.addLazySorted(rel1);
      }
      if ((rel2 > 0) && !haveAll[rel2]) {
        log("\tq2.add:\t" + rel2);
        q2.addLazySorted(rel2);
      }
    }
  }

  private void handleQ2() {
    // q2 - то что м.б. можно вычислить
    // если получилось вычислить - помещаем в q1

    int node, r, rel1, rel2;
    while (q2.getLen() > 0) {
      node = q2.pop();
      rel1 = rels1[node];
      rel2 = rels2[node];
      log("Q2:\t" + node + " (" + rel1 + "," + rel2 + ")");

      if ((rel1 >= 0) && (rel2 >= 0)) {
        if (haveVal[rel1] && haveVal[rel2]) {
          setVal(node, val[rel2] || !val[rel1]);
        } else if (haveVal[rel1] && !val[rel1]) {
          setVal(node, true);
        } else if (haveVal[rel2] && val[rel2]) {
          setVal(node, true);
        }
      }

      r = lastUsed1[node];
      while (r > args) {
        if (haveVal[r]) {
          if (!val[r]) {
            setVal(node, true);
          } else if (haveVal[rels2[r]] && !val[rels2[r]]) {
            setVal(node, false);
          }
        }
        r = prevUsed1[r];
      }

      r = lastUsed2[node];
      while (r > args) {
        if (haveVal[r]) {
          if (!val[r]) {
            setVal(node, false);
          } else if (haveVal[rels1[r]] && val[rels1[r]]) {
            setVal(node, true);
          }
        }
        r = prevUsed2[r];
      }
    }
  }

  private void setVal(int node, boolean v) {
    if (haveVal[node]) {
      if (v ^ val[node]) {
        errNode = node;
        return;
      }
    } else {
      log("\tsetval:\t" + node + "\t" + true);
      val[node] = v;
      haveVal[node] = true;
      log("\tq1.add:\t" + node);
      q1.addLazySorted(node);
    }

    int rel1 = rels1[node];
    int rel2 = rels2[node];
    if ((rel1 == -1) || (rel2 == -1) || (haveVal[rel1] && haveVal[rel2])) {
      haveAll[node] = true;
    }
  }

  private void checkVals() {
    int nn = 0;
    int rel1, rel2;
    boolean hv, v, hv1, hv2, v1, v2;
    for (int i = one; i < len; i++) {
      rel1 = rels1[i];
      rel2 = rels2[i];
      hv = haveVal[i];
      hv1 = haveVal[rel1];
      hv2 = haveVal[rel2];
      v = val[i];
      v1 = val[rel1];
      v2 = val[rel2];

      nn++;

      if (hv) {
        if (hv1 && hv2 && ((v2 || !v1) ^ v)) {
          System.out.println("node " + i + " err: hv && hv1 && hv2 && ((v2 || !v1) ^ v)");
        } else if (hv1 && !v1 && !v) {
          System.out.println("node " + i + " err: hv && hv1 && !v1 && !v");
        } else if (hv2 && v2 && !v) {
          System.out.println("node " + i + " err: hv && hv2 && v2 && !v");
        } else if (hv2 && !v2 && !hv1) {
          System.out.println("node " + i + " err: hv && hv2 && !v2 && !hv1");
        } else if (hv1 && v1 && !hv2) {
          System.out.println("node " + i + " err: hv && hv1 && v1 && !hv2");
        } else {
          nn--;
        }
      } else {
        if (hv1 && hv2) {
          System.out.println("node " + i + " err: !hv && hv1 && hv2");
        } else if (hv1 && !v1) {
          System.out.println("node " + i + " err: !hv && hv1 && !v1");
        } else if (hv2 && v1) {
          System.out.println("node " + i + " err: !hv && hv2 && v1");
        } else {
          nn--;
        }
      }

      if (nn > 10) {
        System.out.println("...");
        return;
      }
    }
  }

  private void logOpen(String fname1) {
    try {
      logPS = new PrintStream(
              new BufferedOutputStream(
              new FileOutputStream(fname1 + ".6.txt", false), 32000));
    } catch (Exception e) {
      logPS = null;
      System.out.println("error opening log file \"" + fname1 + "\"");
    }
  }

  private void logClose() {
    if (logPS != null) {
      logPS.close();
    }
  }

  private void log(String s) {
    if (logPS != null) {
      logPS.println(s);
    }
  }

  private void printMinDepth(int from, int to, String note) {
    int nn1 = len, nn2 = len, n1, n2;
    for (int i = from; i < to; i++) {
      if (haveVal[i]) {
        n1 = minDepth[i];
        n2 = minDist[i];
        nn1 = (n1 > 0) && (n1 < nn1) ? n1 : nn1;
        nn2 = (n2 > 0) && (n2 < nn2) ? n2 : nn2;
      }
    }
    System.out.println("min depth/dist: " + nn1 + "/" + nn2 + note + ":");

    int r1, r2;
    ListInt b = new ListInt();
    for (int i = from; i < to; i++) {
      if (haveVal[i] && (minDepth[i] == nn1)) {
        r2 = i;
        while (r2 > args) {
          r1 = rels1[r2];
          if (minDepth[r1] <= nn1) {
            b.addLazySorted(r1);
          }
          r2 = rels2[r2];
        }
      }
    }

    b.sort();
    for (int i = 0; i < b.getLen(); i++) {
      r1 = b.get(i);
      System.out.println("" + r1 + "\t" + minDepth[r1] + "/" + minDist[r1]
              + "\t" + countUsed1[r1] + "+" + countUsed2[r1]);
      if (i >= 10) {
        System.out.println("...");
        break;
      }
    }
  }

  private void setNodes(String setNd) {
    if ((setNd == null) || setNd.isEmpty()) {
      return;
    }

    String[] ss = setNd.split(",");
    String[] vv;
    int node;
    for (int i = 0; i < ss.length; i++) {
      vv = ss[i].split("-");
      node = Integer.parseInt(vv[0]);
      setVal(node, vv[1].equals("1"));
    }
  }
}
