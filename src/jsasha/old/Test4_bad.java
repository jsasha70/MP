package jsasha.old;

import java.util.Arrays;
import jsasha.lt.LT_dataSrc;
import jsasha.lt.LogicTree;
import jsasha.lt.LogicTreeException;

/**
 * проверка быстродействия получения размера всех узлов (размера трака)
 * (не работает)
 */
public class Test4_bad extends LT_dataSrc {

  private int[] nodeSize = null;
  private int[] seqChain1 = null;
  private int[] seqChain2 = null;
  private int[] seqTyp = null;
  private int seqChainStart;
  private int seqChainFin;
  private int[] seqTypCount = new int[4]; // элемент с индексом 0 не используется
  private int seqPtr;
  private int seqLen;

  protected void changed() {
    nodeSize = nodeSize == null ? new int[len] : Arrays.copyOf(nodeSize, len);
    seqChain1 = seqChain1 == null ? new int[len] : Arrays.copyOf(seqChain1, len);
    seqChain2 = seqChain2 == null ? new int[len] : Arrays.copyOf(seqChain2, len);
    seqTyp = seqTyp == null ? new int[len] : Arrays.copyOf(seqTyp, len);
  }

  private void seqCreate(int n1, int n2) {
    seqLen = 0;
    if ((n1 == 0) || (n2 == 0) || (n1 == n2)) {
      return;
    }

    if (n1 > n2) {
      seqChainStart = n1;
      seqChainFin = n2;
    } else {
      seqChainStart = n2;
      seqChainFin = n1;
    }

    seqChain1[seqChainStart] = seqChainFin;
    seqChain1[seqChainFin] = 0;
    seqChain2[seqChainFin] = seqChainStart;
    seqChain2[seqChainStart] = 0;
    seqTyp[n1] = 1;
    seqTyp[n2] = 2;
    seqTypCount[3] = 0;
    seqTypCount[1] = 1;
    seqTypCount[2] = 1;
    seqPtr = seqChainStart;
    seqLen = 2;

    int typ;

    while ((seqTypCount[1] > 0) && (seqTypCount[2] > 0)) {
      // главный цикл поиска пересечений деревьев
      if (seqPtr == 0) {
        throw new LogicTreeException("invalid condition 3");
      }

      typ = seqTyp[seqPtr];

      if (typ != 3) {
        n1 = rels1[seqPtr];
        n2 = rels2[seqPtr];

        if (n1 > 0) {
          if (seqTyp[n1] == 0) {
            seqTyp[n1] = typ;
            seqTypCount[typ]++;
            // располагаем n1 в цепочке
            toChain(n1);
          } else if ((seqTyp[n1] != typ) && (seqTyp[n1] != 3)) {
            // есть пересечение ветвей
            seqTyp[n1] = 3;
            seqTypCount[3 - typ]--;
            seqTypCount[3]++;
          }
        }

        if (n2 > 0) {
          if (seqTyp[n2] == 0) {
            seqTyp[n2] = typ;
            seqTypCount[typ]++;
            // располагаем n2 в цепочке
            toChain(n2);
          } else if ((seqTyp[n2] != typ) && (seqTyp[n2] != 3)) {
            // есть пересечение ветвей
            seqTyp[n2] = 3;
            seqTypCount[3 - typ]--;
            seqTypCount[3]++;
          }
        }

        seqTypCount[typ]--;
      }

      seqPtr = seqChain1[seqPtr];
    }
  }

  private void seqCreateFull(int n1, int n2) {
    seqLen = 0;
    if ((n1 == 0) || (n2 == 0) || (n1 == n2)) {
      return;
    }

    if (n1 > n2) {
      seqChainStart = n1;
      seqChainFin = n2;
    } else {
      seqChainStart = n2;
      seqChainFin = n1;
    }

    seqChain1[seqChainStart] = seqChainFin;
    seqChain1[seqChainFin] = 0;
    seqChain2[seqChainFin] = seqChainStart;
    seqChain2[seqChainStart] = 0;
    seqTyp[n1] = 1;
    seqTyp[n2] = 2;
    seqTypCount[3] = 0;
    seqTypCount[1] = 1;
    seqTypCount[2] = 1;
    seqPtr = seqChainStart;
    seqLen = 2;

    int typ;

    while (seqPtr > 0) {
      // главный цикл поиска пересечений деревьев
      if (seqPtr == 0) {
        throw new LogicTreeException("invalid condition 3");
      }

      typ = seqTyp[seqPtr];

      n1 = rels1[seqPtr];
      n2 = rels2[seqPtr];

      if (n1 > 0) {
        if (seqTyp[n1] == 0) {
          seqTyp[n1] = typ;
          seqTypCount[typ]++;
          // располагаем n1 в цепочке
          toChain(n1);
        } else if ((seqTyp[n1] != typ) && (seqTyp[n1] != 3)) {
          // есть пересечение ветвей
          seqTyp[n1] = 3;
          seqTypCount[3 - typ]--;
          seqTypCount[3]++;
        }
      }

      if (n2 > 0) {
        if (seqTyp[n2] == 0) {
          seqTyp[n2] = typ;
          seqTypCount[typ]++;
          // располагаем n2 в цепочке
          toChain(n2);
        } else if ((seqTyp[n2] != typ) && (seqTyp[n2] != 3)) {
          // есть пересечение ветвей
          seqTyp[n2] = 3;
          seqTypCount[3 - typ]--;
          seqTypCount[3]++;
        }
      }

      seqTypCount[typ]--;
      seqPtr = seqChain1[seqPtr];
    }
  }

  private void seqClear() {
    // очищаем цепочку
    int n = seqChainStart;
    while (n > 0) {
      seqTyp[n] = 0;
      n = seqChain1[n];
    }
    seqLen = 0;
  }

  private void toChain(int rr) {
    // располагаем узел rr в цепочке, начиная поиск места с узла seqPtr
    int n = seqPtr;
    int nPrev = 0;
    if (rr >= n) {
      throw new LogicTreeException("invalid condition 11");
    }
    while (rr < n) {
      nPrev = n;
      n = seqChain1[n];
    }
    if (nPrev == 0) {
      throw new LogicTreeException("invalid condition 1");
    }
    if (n == 0) {
      // порядок: nPrev chainFin rr
      seqChain2[rr] = seqChainFin;
      seqChainFin = rr;
    } else {
      // порядок: nPrev rr n
      seqChain2[n] = rr;
      seqChain2[rr] = nPrev;
    }
    seqChain1[nPrev] = rr;
    seqChain1[rr] = n;
    seqLen++;
  }

  public void run(LogicTree lt) {
    long dt1 = System.currentTimeMillis();
    lt.dataAccess(this);

    for (int i = 1; i <= args; i++) {
      nodeSize[i] = 1;
    }

//    int nn;
    for (int i = args + 1; i < len; i++) {
      if (i % 10000 == 0) {
        System.out.println(i);
      }

      nodeSize[i] = 1;
      if (rels2[i] == 0) {
        nodeSize[i] += nodeSize[rels1[i]];
        continue;
      }

      seqCreateFull(rels1[i], rels2[i]);

      nodeSize[i] += seqLen;

//      nn = seqChainStart;
//      while (nn > 0) {
//        if (seqTyp[nn] == 3) {
//          nodeSize[i] -= nodeSize[nn];
//        }
//        nn = seqChain1[nn];
//      }

//      markClearAll();
//      markTree(i);
//      int n = 0;
//      for (int j = 0; j <= i; j++) {
//        if (m[j]) {
//          n++;
//        }
//      }
//      if (nodeSize[i] != n) {
//        throw new LogicTreeException("invalid condition 13");
//      }

      if (nodeSize[i] <= 0) {
        throw new LogicTreeException("invalid condition 13");
      }
      if (nodeSize[i] > i) {
        throw new LogicTreeException("invalid condition 12");
      }

      seqClear();
    }

    for (int i = 0; i < vars.length; i++) {
      System.out.println("  " + (vars.length - i) + ": " + nodeSize[vars[i]]);
    }
    System.out.println("exec time: " + ((System.currentTimeMillis() - dt1) / 1000) + " s");
  }
}
