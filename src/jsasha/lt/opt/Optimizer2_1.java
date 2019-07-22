package jsasha.lt.opt;

import java.util.Arrays;
import jsasha.lt.LT_dataSrc;
import jsasha.lt.LogicTree;
import jsasha.lt.LogicTreeException;

/**
 * дифференцирование
 * 
 */
public class Optimizer2_1 extends LT_dataSrc {

  private LogicTree lt2;
  //
  // работа с последовательностями
  private int[] seq1 = null;
  private int[] seq2 = null;
  private int seqStart = 0;
  private int seqFin = 0;
  private int seqLen = 0;
  private int[] newVal = null;
  private int seqPtr = 0;
  //
  // отметки для поиска списков
  private boolean[] mm = null;
  private int mmList = -1;
  // для диффренцирования по нескольким спискам
  private int mmLast;
  //
  // сохранение сделанных оптимизаций
  private int[] opt = null;
  private boolean optDone = false; // признак использования оптимизации opt в ф-ии add

  public void changed() {
    int size1 = seq1 == null ? 0 : seq1.length;
    int size2 = rels1.length;
    seq1 = seq1 == null ? new int[size2] : Arrays.copyOf(seq1, size2);
    seq2 = seq2 == null ? new int[size2] : Arrays.copyOf(seq2, size2);
    newVal = newVal == null ? new int[size2] : Arrays.copyOf(newVal, size2);
    mm = mm == null ? new boolean[size2] : Arrays.copyOf(mm, size2);
    opt = opt == null ? new int[size2] : Arrays.copyOf(opt, size2);
    Arrays.fill(seq1, size1, size2, -1);
    Arrays.fill(seq2, size1, size2, -1);
    Arrays.fill(newVal, size1, size2, -1);
    Arrays.fill(opt, size1, size2, -1);
  }

  public LogicTree run(LogicTree lt) {
    lt2 = new LogicTree(lt.getArgs());
    lt2.dataAccess(this);

    int len1 = lt.getLen();
    int[] tr = new int[len1];
    for (int i = 0; i <= one; i++) {
      tr[i] = i;
    }

    int node, node1, node2;
    for (int i = one + 1; i < len1; i++) {
      node1 = add(tr[lt.getRel1(i)], tr[lt.getRel2(i)], false);
      node2 = node1;
      if ((i % 100) == 0) {
        System.out.println(i); // ???
      }
      if (i == 184) {
        System.out.println(i); // ???
      }

      if (!optDone && (node1 > one)) {
        node = node1;
        while (true) {
          node = doDiff(node);
          if (node >= 0) {
            node2 = node;
          } else {
            break;
          }
        }
      }

      tr[i] = node2;
      opt[node1] = node2;
    }

    return lt2;
  }

  private int add(int rel1, int rel2) {
    return add(rel1, rel2, true);
  }

  private int add(int rel1, int rel2, boolean doSort) {
    if (opt[rel1] != -1) {
      int aaa = 123; // ???
    }
    if (opt[rel2] != -1) {
      int aaa = 123; // ???
    }

    optDone = false;
    int ret = doSort ? lt2.add(rel1, rel2) : lt2.addNoSort(rel1, rel2);
    if (opt[ret] != -1) {
      ret = opt[ret];
      optDone = true;
    }
    return ret;
  }

  private int doDiff(int node) {
    int rel2 = rels2[node];
    if (rel2 <= 0) {
      return -1;
    }
    int rel1 = rels1[node];

    int ret = doDiff1(node, rel1, rel2);
    if (ret != -1) {
      return ret;
    }

    ret = doDiff2(node, rel1, rel2);
    return ret;
  }

  private void markLists(int node, boolean val) {
    if (val) {
      if (mmList == node) {
        return;
      } else if (mmList != -1) {
        markLists(mmList, false);
      }
    }

    mmLast = -1;
    int r2 = node;
    int r1, r11;
    while (r2 > args) {
      r1 = rels1[r2];
      r2 = rels2[r2];
      if (r1 > 0) {
        if (r1 <= args) {
          mm[r1] = val;
          mmLast = r1;
        } else if (r1 > one) {
          r11 = rels1[r1];
          mm[r11] = val;
          mmLast = r11;
        }
      }
    }

    mmList = val ? node : -1;
  }

  private void seqNew(int node) {
    if (seqLen > 0) {
      seqClear();
    }

    seqPtr = node;
    seqStart = node;
    seqFin = node;
    seqLen = 1;
  }

  private void seqClear() {
    // очищаем цепочку
    int n = seqStart;
    int n2;
    while (n > 0) {
      n2 = seq1[n];
      newVal[n] = -1;
      seq1[n] = -1;
      seq2[n] = -1;
      n = n2;
    }
    seqLen = 0;
  }

  private void seqAdd(int rr) {
    // располагаем узел rr в цепочке, начиная поиск места с узла seqPtr
    if (rr <= 0) {
      throw new LogicTreeException("invalid condition 2");
    } else if ((seq2[rr] != -1) || (rr == seqPtr)) {
      return;
    }

    int n = rr > seqPtr ? seqStart : seqPtr;
    int nPrev = 0;
    while (rr < n) {
      nPrev = n;
      n = seq1[n];
    }
    if (nPrev == 0) {
      throw new LogicTreeException("invalid condition 1");
    }

    if (n == -1) {
      // порядок: nPrev chainFin rr
      seq2[rr] = seqFin;
      seqFin = rr;
    } else {
      // порядок: nPrev rr n
      seq2[n] = rr;
      seq2[rr] = nPrev;
    }
    seq1[nPrev] = rr;
    seq1[rr] = n;
    seqLen++;
  }

  private void seqAddRels(int node, int minVal) {
    int rel1 = rels1[node];
    int rel2 = rels2[node];
    if (rel1 >= minVal) {
      seqAdd(rel1);
    }
    if (rel2 >= minVal) {
      seqAdd(rel2);
    }
  }

  private void seqNext() {
    if (seqPtr > 0) {
      seqPtr = seq1[seqPtr];
    }
  }

  private int replaceVal(int node, int val1, int val2) {
    if (node == val1) {
      return val2;
    } else if (val1 == val2) {
      return -1;
    }

    seqNew(node);
    seqAddRels(node, one);
    seqNext();
    boolean found = false;

    while (seqPtr >= val1) {
      if (newVal[seqPtr] == -1) {
        if (seqPtr == val1) {
          newVal[seqPtr] = val2;
          found = true;
        } else {
          seqAddRels(seqPtr, val1);
        }
      }
      seqNext();
    }

    if (!found) {
      return -1;
    }

    int nn = seq2[val1];
    int r1, r2, nv1, nv2;
    while (nn > 0) {
      if ((nn > one) && (newVal[nn] == -1)) {
        r1 = rels1[nn];
        r2 = rels2[nn];
        nv1 = newVal[r1];
        nv2 = newVal[r2];
        if ((nv1 != -1) || (nv2 != -1)) {
          newVal[nn] = add(nv1 == -1 ? r1 : nv1, nv2 == -1 ? r2 : nv2);
        }
      }
      nn = seq2[nn];
    }

    int ret = newVal[seqStart];
    seqClear();
    return ret;
  }

  private int doDiff1a(int node, int rel1, int rel2) {
    // node - исходный узел (выражение)
    // rel1 - аргумент или его отрицание
    // rel2 - выражение, в которое подставляем значение аргумента

    int nv;
    if (rels2[rel1] == 0) {
      nv = replaceVal(rel2, rels1[rel1], 0);
    } else {
      nv = replaceVal(rel2, rel1, one);
    }

    if (nv == -1) {
      return - 1;
    } else if (nv == node) {
      throw new LogicTreeException("doDiff1a: same node after calc");
    }

    int ret = add(rel1, nv);

    if (ret == node) {
      throw new LogicTreeException("doDiff1a: same node after add");
    }

    return ret;
  }

  private int doDiff1(int node, int rel1, int rel2) {
    if ((rel1 <= args) || (rels2[rel1] == 0)) {
      // дифферецирование по аргументу
      return doDiff1a(node, rel1, rel2);
    }

    // диффренцирование по списку
    int nv = replaceList(rel2, rel1, one);

    if (nv == -1) {
      return - 1;
    } else if (nv == node) {
      throw new LogicTreeException("doDiff1: same node after calc");
    }

    int ret = add(rel1, nv);

    if (ret == node) {
      throw new LogicTreeException("doDiff1: same node after add");
    }

    return ret;
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

  private int replaceList(int node, int val1, int val2) {
    // в дереве node зменяем все над-списки val1 на val2
    if (node == val1) {
      return val2;
    } else if (val1 == val2) {
      return -1;
    }

    seqNew(node);

    int v1rel1 = rels1[val1];
    int lastFound = -1;
    int r1;
    while (seqPtr > v1rel1) {
      r1 = rels1[seqPtr];
      if ((r1 == v1rel1) && isSublist(val1, seqPtr)) {
        newVal[seqPtr] = val2;
        lastFound = seqPtr;
      } else {
        seqAddRels(seqPtr, v1rel1);
      }
      seqNext();
    }

    if (lastFound == -1) {
      return -1;
    }

    int nn = seq2[lastFound];
    int r2, nv1, nv2;
    while (nn > 0) {
      if (newVal[nn] == -1) {
        r1 = rels1[nn];
        r2 = rels2[nn];
        nv1 = newVal[r1];
        nv2 = newVal[r2];
        if ((nv1 != -1) || (nv2 != -1)) {
          newVal[nn] = add(nv1 == -1 ? r1 : nv1, nv2 == -1 ? r2 : nv2);
        }
      }
      nn = seq2[nn];
    }

    int ret = newVal[seqStart];
    seqClear();
    return ret;
  }

//  private int findMarkedList(int node) {
//    // поиск в указанном дереве помеченного списка
//
//    seqNew(node);
//
//    int min = mmLast > one ? mmLast : one;
//    int r1, ret;
//    while (seqPtr > min) {
//      r1 = rels1[seqPtr];
//      if (mm[r1]) {
//        ret = checkMarkedList(seqPtr, r1);
//        if (ret > 0) {
//          seqClear();
//          return ret;
//        }
//      }
//      seqAddRels(seqPtr, min);
//      seqNext();
//    }
//
//    seqClear();
//    return -1;
//  }
  private int findMarkedList(int node) {
    // поиск в указанном дереве помеченного списка

    seqNew(node);

    int min = mmLast > one ? mmLast : one;
    int r1, ret;

    // помечаем список узлов
    while (seqPtr > min) {
      seqAddRels(seqPtr, min);
      seqNext();
    }

    // проходим аргументы
    seqPtr = seqFin;
    while (seqPtr <= one) {
      seqPtr = seq2[seqPtr];
    }

    // находим помеченный список
    while (seqPtr > 0) {
      r1 = rels1[seqPtr];
      if (mm[r1]) {
        ret = checkMarkedList(seqPtr, r1);
        if (ret > 0) {
          seqClear();
          return ret;
        }
      }
      seqPtr = seq2[seqPtr];
    }

    seqClear();
    return -1;
  }

  private int checkMarkedList(int node, int rel1) {
    // выясняем, помечен ли список node, либо аргумент rel1
    // (rel1 помечен)

    int r2 = mmList;
    int r1, r11;
    while (r2 > args) {
      r1 = rels1[r2];
      if (r1 > 0) {
        if (r1 <= args) {
          if (r1 == rel1) {
            // будем дифференцировать по аргументу
            return r1;
          }
        } else if (r1 > one) {
          r11 = rels1[r1];
          if (r11 == rel1) {
            if ((r11 <= args) && (rels2[r1] == 0)) {
              // нашли отрицание аргумента
              // (но дифференцировать будем по аргументу)
              return r1;
            }

            // сравниваем списки node и r1
            if (isSublist(r1, node)) {
              return node;
            }
          }
        }
      }
      r2 = rels2[r2];
    }

    return -1;
  }

  private int doDiff2(int node, int rel1, int rel2) {
    // диффренцирование по нескольким спискам (из списка rel2)

    if ((rel1 <= one) || (rel2 <= one)) {
      return -1;
    }

    markLists(rel2, true);
    if (mmLast == -1) {
      return -1;
    }

    // находим по чему будем дифференцировать
    int lst = findMarkedList(rel1);
    if (lst == -1) {
      return -1;
    }

    if ((lst <= args) || ((rels1[lst] <= args) && (rels2[lst] == 0))) {
      // дифференцируем по аргументу (аргумент или его отрицание определяет значение выражения)
      return doDiff1a(node, lst, node);
    } else {
      // дифференцируем по списку
      int nv = replaceList(node, lst, one);

      if (nv == -1) {
        return - 1;
      } else if (nv == node) {
        throw new LogicTreeException("doDiff2: same node after calc");
      }

      int ret = add(lst, nv);

      if (ret == node) {
        throw new LogicTreeException("doDiff2: same node after add");
      }

      return ret;
    }
  }
}
