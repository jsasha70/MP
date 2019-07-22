package jsasha.lt.opt;

import java.util.Arrays;
import jsasha.lt.LT_dataSrc;
import jsasha.lt.LogicTree;
import jsasha.lt.LogicTreeException;

/**
 * дифференцирование
 * 
 */
public class Optimizer2_2 extends LT_dataSrc {

  private LogicTree lt2;
  //
  // ветки узла оптимизируются раздельно
  private int nrel1, nrel2;
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
  private int mmLast; // для диффренцирования по нескольким спискам
  //
  // сохранение сделанных оптимизаций
  private int[] opt = null;
  private boolean optDone = false; // признак использования оптимизации opt в ф-ии add
  //
  // прерывание оптимизации
  private boolean interrupted;

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
    interrupted = false;
    seq1 = null;
    seq2 = null;
    newVal = null;
    mm = null;
    opt = null;

    lt2 = new LogicTree(lt.getArgs());
    lt2.dataAccess(this);

    int len1 = lt.getLen();
    int[] tr = new int[len1];
    for (int i = 0; i <= one; i++) {
      tr[i] = i;
    }

    int node, node2, nrel1_orig, nrel2_orig, len_orig;
    for (int i = one + 1; i < len1; i++) {
      if (interrupted) {
        node = add(tr[lt.getRel1(i)], tr[lt.getRel2(i)], true);
        tr[i] = node;
      } else {
        len_orig = len;
        node = add(tr[lt.getRel1(i)], tr[lt.getRel2(i)], false);
        nrel1 = rels1[node];
        nrel2 = rels2[node];
        nrel1_orig = nrel1;
        nrel2_orig = nrel2;

        if (!optDone && (node > one)) {
          while (doDiff()) {
          }
        }

        if ((nrel1 == nrel1_orig) && (nrel2 == nrel2_orig)) {
          node2 = lt2.sort(node);
        } else {
          node2 = add(nrel1, nrel2, true);
        }

//      node2 = condense(len_orig, node2);

        tr[i] = node2;
        opt[node] = node2;

        if ((len - i) > 400000) {
          interrupted = true;
          System.out.println("opt interrupted  at " + i + "; " + (len1 - i) + " rest");
          if (mmList != -1) {
            markLists(mmList, false);
          }
        }
      }
    }

    lt2.vars = new int[lt.vars.length];
    for (int i = 0; i < lt.vars.length; i++) {
      lt2.vars[i] = lt2.xMinMin(tr[lt.vars[i]]);
    }

    return lt2;
  }

//  private int condense(int len_orig, int node2) {
//    if ((len_orig >= len) || ((node2 == len_orig) && len == (node2 + 1))) {
//      return node2;
//    }
//
//    if (node2 < len_orig) {
//      trim(len_orig, len_orig);
//      return node2;
//    }
//
//    seqNew(node2);
//    int r1, r2;
//    while (seqPtr > 0) {
//      r1 = rels1[seqPtr];
//      r2 = rels2[seqPtr];
//
//      if (r1 >= len_orig) {
//        seqAdd(r1);
//      }
//      if (r2 >= len_orig) {
//        seqAdd(r2);
//      }
//
//      seqNext();
//    }
//
//    int nn = len_orig;
//    seqPtr = seqFin;
//    while (seqPtr > 0) {
//      r1 = rels1[seqPtr];
//      r2 = rels2[seqPtr];
//
//      if (r1 >= len_orig) {
//        r1 = newVal[r1];
//      }
//      if (r2 >= len_orig) {
//        r2 = newVal[r2];
//      }
//
//      lt2.setNode(nn, r1, r2);
//
//      newVal[seqPtr] = nn;
//      nn++;
//      seqPtr = seq2[seqPtr];
//    }
//
//    node2 = newVal[node2];
//    seqClear();
//
//    trim(len_orig, nn);
//    return node2;
//  }
//
//  private void trim(int len_orig, int toLen) {
//    Arrays.fill(opt, len_orig, len, -1);
//    lt2.setLen(toLen);
//  }
  private int add(int rel1, int rel2, boolean doSort) {
    optDone = false;
    rel1 = opt[rel1] == -1 ? rel1 : opt[rel1];
    rel2 = opt[rel2] == -1 ? rel2 : opt[rel2];

    int ret = doSort ? lt2.add(rel1, rel2) : lt2.addNoSort(rel1, rel2);
    if (opt[ret] != -1) {
      ret = opt[ret];
      optDone = true;
    }
    return ret;
  }

  private boolean doDiff() {
    if (nrel2 <= 0) {
      return false;
    }

    boolean ret1 = doDiff1();
    boolean ret2 = doDiff2();
    return ret1 || ret2;
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
          newVal[nn] = add(nv1 == -1 ? r1 : nv1, nv2 == -1 ? r2 : nv2, true);
        }
      }
      nn = seq2[nn];
    }

    int ret = newVal[seqStart];
    seqClear();
    return ret;
  }

  private int doDiff1a(int arg, int expr) {
    // arg - аргумент или его отрицание
    // expr - выражение, в которое подставляем значение аргумента

    int nv;
    if (rels2[arg] == 0) {
      nv = replaceVal(expr, rels1[arg], 0);
    } else {
      nv = replaceVal(expr, arg, one);
    }

    if (nv == -1) {
      return -1;
    } else if (nv == expr) {
      throw new LogicTreeException("doDiff1a: same node after calc");
    }

    return nv;
  }

  private boolean doDiff1() {
    if ((nrel1 <= args) || (rels2[nrel1] == 0)) {
      // дифферецирование по аргументу
      int nv = doDiff1a(nrel1, nrel2);
      if (nv == -1) {
        return false;
      } else {
        nrel2 = nv;
        return true;
      }
    }

    // диффренцирование по списку
    int nv = replaceList(nrel2, nrel1, one);

    if (nv == -1) {
      return false;
    } else if (nv == nrel2) {
      throw new LogicTreeException("doDiff1: same node after calc");
    }

    nrel2 = nv;
    return true;
  }

  private boolean isSublist(int lst1, int lst2) {
    // проверка что lst2 содержит lst1

    if (lst1 == lst2) {
      return true;
    }

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
          newVal[nn] = add(nv1 == -1 ? r1 : nv1, nv2 == -1 ? r2 : nv2, true);
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
              return r1;
            }
          }
        }
      }
      r2 = rels2[r2];
    }

    return -1;
  }

  private boolean doDiff2() {
    // диффренцирование по нескольким спискам (из списка nrel2)

    if ((nrel1 <= one) || (nrel2 <= one)) {
      return false;
    }

    int node = add(nrel1, nrel2, true);
    if (optDone || (node == nrel2)) {
      return false;
    }

    markLists(nrel2, true);
    if (mmLast == -1) {
      return false;
    }

    // находим по чему будем дифференцировать
    int lst = findMarkedList(nrel1);
    if (lst == -1) {
      return false;
    }

    int nv;
    if ((lst <= args) || ((rels1[lst] <= args) && (rels2[lst] == 0))) {
      // дифференцируем по аргументу (аргумент или его отрицание определяет значение выражения)
      nv = doDiff1a(lst, node);
    } else {
      // дифференцируем по списку
      nv = replaceList(node, lst, one);
    }

    if ((nv == -1) || (nv == node)) {
      throw new LogicTreeException("doDiff2: invalid calc result");
    }

    nrel1 = lst;
    nrel2 = nv;

    return true;
  }
}
