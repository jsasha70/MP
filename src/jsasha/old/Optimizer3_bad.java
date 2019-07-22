package jsasha.old;

import java.util.Arrays;
import jsasha.lt.LogicTree;
import jsasha.lt.LogicTreeException;
import jsasha.lt.LogicTreeStore;

/**
 * оптимизация дифференцированием (без отслеживания подмножеств)
 * (не работает)
 */
public class Optimizer3_bad {

  private LogicTree lt;
  private LogicTree lt2;
  private int len;
  private int args;
  private int[] chain1; // ссылки на связанные узлы назад
  private int[] chain2; // ссылки вперед
  private int[] typ; // тип узла: 1 - из rel1, 2 - из rel2, 0 - не в цепочке
  private boolean[] haveVal;
  private boolean[] val;
  private int[] newVal;
  private int[] rels1;
  private int[] rels2;
  private int[] tr = new int[len];

  public LogicTree run(LogicTree lt) {
    this.lt = lt;
    this.lt2 = lt;
    int nn;
    int stepCount = 0;

    do {
      stepCount++;
      this.lt = this.lt2;
      nn = run1();
      this.lt2 = LogicTreeStore.condenseLT(this.lt2);
      System.out.println();
      System.out.println();
      System.out.println("step " + stepCount + "; changed " + nn + "; len: " + lt.getLen() + " -> " + lt2.getLen());
      nn = 0; // aaa; ???
    } while (nn > 0);

    return this.lt2;
  }

  public int run1() {
    len = lt.getLen();
    args = lt.getArgs();
    lt2 = new LogicTree(args, len);
    tr = new int[len];

    for (int i = 0; i <= args; i++) {
      tr[i] = i;
    }
    tr[lt.one] = lt2.one;

    rels1 = lt.arrayRels1();
    rels2 = lt.arrayRels2();
    newVal = new int[len];

    int r1, r2;
    int changeCount = 0;
    chain1 = new int[len]; // ссылки на связанные узлы назад
    chain2 = new int[len]; // ссылки вперед
    typ = new int[len]; // тип узла: 1 - из rel1, 2 - из rel2, 0 - не в цепочке
    Arrays.fill(typ, 0);
    int typ1count, typ2count; // счетчики узлов цепочки каждого типа
    int chainStart, chainStart0, chainFin; // начало и конец цепочки
    boolean done;
    int n, n1;
    haveVal = new boolean[len];
    Arrays.fill(haveVal, false);
    val = new boolean[len];
    val[0] = false;
    haveVal[0] = true;
    val[lt.one] = true;
    haveVal[lt.one] = true;

    for (int i = args + 1; i < len; i++) {
      r1 = rels1[i];
      r2 = rels2[i];

      if ((r1 == 0) || (r2 == 0) || (r1 == r2)) {
        tr[i] = lt2.add(tr[r1], tr[r2]);
      } else {
        if (r1 > r2) {
          chainStart = r1;
          chainFin = r2;
        } else {
          chainStart = r2;
          chainFin = r1;
        }
        chainStart0 = chainStart;
        chain1[chainStart] = chainFin;
        chain1[chainFin] = 0;
        chain2[chainFin] = chainStart;
        chain2[chainStart] = i;
        chain2[i] = 0;
        typ[r1] = 1;
        typ[r2] = 2;
        typ1count = 1;
        typ2count = 1;

        done = false;
        while ((typ1count > 0) && (typ2count > 0)) {
          // главный цикл поиска пересечений деревьев
          if (chainStart == 0) {
            throw new LogicTreeException("invalid condition 3");
          }

          r1 = rels1[chainStart];
          r2 = rels2[chainStart];

          // проверяем совпадение
          if ((typ[r1] == 2) || (typ[r2] == 1)) {
            // есть совпадение, вычисляем
            if (typ[r1] == 2) {
              done = calcOverlap(i, r1);
            }
            if (!done && (typ[r2] == 1)) {
              done = calcOverlap(i, r2);
            }
            if (done) {
              break;
            }
          }

          // располагаем r1 и r2 в цепочке
          if ((r1 > 0) && (typ[r1] == 0)) {
            typ[r1] = 1;
            typ1count++;
            n = chainStart;
            n1 = 0;
            while (r1 < n) {
              n1 = n;
              n = chain1[n];
            }
            if (n1 == 0) {
              throw new LogicTreeException("invalid condition 1");
            }
            if (n == 0) {
              // порядок: n1 chainFin r1
              chain2[r1] = chainFin;
              chainFin = r1;
            } else {
              // порядок: n1 r1 n
              chain2[n] = r1;
              chain2[r1] = n1;
            }
            chain1[n1] = r1;
            chain1[r1] = n;
          }
          if ((r2 > 0) && (typ[r2] == 0)) {
            typ[r2] = 2;
            typ2count++;
            n = chainStart;
            n1 = 0;
            while (r2 < n) {
              n1 = n;
              n = chain1[n];
            }
            if (n1 == 0) {
              throw new LogicTreeException("invalid condition 2");
            }
            if (n == 0) {
              // порядок: n1 chainFin r2
              chain2[r2] = chainFin;
              chainFin = r2;
            } else {
              // порядок: n1 r2 n
              chain2[n] = r2;
              chain2[r2] = n1;
            }
            chain1[n1] = r2;
            chain1[r2] = n;
          }
          if (typ[chainStart] == 1) {
            typ1count--;
          } else {
            typ2count--;
          }
          chainStart = chain1[chainStart];
        }

        // очищаем цепочку
        n = chainStart0;
        while (n > 0) {
          typ[n] = 0;
          n = chain1[n];
        }

        if (done) {
          changeCount++;
          continue;
        }

        tr[i] = lt2.add(tr[rels1[i]], tr[rels2[i]]);
      }
    }

    lt2.vars = new int[lt.vars.length];
    for (int i = 0; i < lt.vars.length; i++) {
      lt2.vars[i] = tr[lt.vars[i]];
    }

    return changeCount;
  }

  private boolean calcOverlap(int i, int rr) {
    if ((rr == 0) || (rr == lt.one)) {
      throw new LogicTreeException("invalid condition 4");
    }

    int ret0 = calcFast(i, rr, false);
    int ret1 = calcFast(i, rr, true);

    if ((ret0 == -1) && (ret1 == -1)) {
      return false;
    } else if ((ret0 != -1) && (ret1 != -1)) {
      if (ret0 == ret1) {
        tr[i] = ret0 == 0 ? 0 : lt2.one;
      } else if ((ret0 == 0) && (ret1 == 1)) {
        tr[i] = tr[rr];
      } else if ((ret0 == 1) && (ret1 == 0)) {
        tr[i] = lt2.add(tr[rr], 0);
      } else {
        throw new LogicTreeException("invalid condition 5");
      }
      return true;
    } else if ((ret0 == 1) && (ret1 == -1)) {
      int node = calcFull(i, rr, true);
      tr[i] = lt2.add(tr[rr], node);
      return true;
    } else if ((ret0 == 0) && (ret1 == -1)) {
      int node = calcFull(i, rr, true);
      tr[i] = lt2.add(lt2.add(tr[rr], lt2.add(node, 0)), 0);
      return true;
    } else if ((ret0 == -1) && (ret1 == 1)) {
      int node = calcFull(i, rr, false);
      tr[i] = lt2.add(lt2.add(node, 0), tr[rr]);
      return true;
    } else if ((ret0 == -1) && (ret1 == 0)) {
      int node = calcFull(i, rr, false);
      tr[i] = lt2.add(lt2.add(node, tr[rr]), 0);
      return true;
    } else {
      throw new LogicTreeException("invalid condition 6");
    }
  }

  private int calcFast(int i, int rr, boolean v) {
    // возврат: -1 - не определено, 0 - false, 1 - true

    haveVal[rr] = true;
    val[rr] = v;

    int n = rr;
    boolean haveVal1, val1 = true;
    int r1, r2;
    while (n != i) {
      n = chain2[n];
      if (n > args) {
        haveVal1 = false;
        r1 = rels1[n];
        r2 = rels2[n];

        if (haveVal[r1] && !val[r1]) {
          haveVal1 = true;
          val1 = true;
        } else if (haveVal[r2] && val[r2]) {
          haveVal1 = true;
          val1 = true;
        } else if (haveVal[r1] && haveVal[r2]) {
          haveVal1 = true;
          val1 = false;
        }

        if (haveVal1) {
          haveVal[n] = true;
          val[n] = val1;
        }
      }
    }

    int retVal = -1;
    if (haveVal[i]) {
      retVal = val[1] ? 1 : 0;
    }

    // очищаем значения
    haveVal[rr] = false;
    n = rr;
    while (n != i) {
      n = chain2[n];
      haveVal[n] = false;
    }

    return retVal;
  }

  private int calcFull(int i, int rr, boolean v) {
    // возврат - номер созданного узла в lt2

    newVal[rr] = v ? lt2.one : 0;

    int n = rr;
    int r1, r2;
    while (n != i) {
      n = chain2[n];
      if (n <= args) {
        newVal[n] = tr[n];
      } else {
        r1 = rels1[n];
        r2 = rels2[n];
        // r1, r2 - номера из lt

        if ((r1 >= rr) && (typ[r1] > 0)) {
          r1 = newVal[r1];
        } else {
          r1 = tr[r1];
        }
        if ((r2 >= rr) && (typ[r2] > 0)) {
          r2 = newVal[r2];
        } else {
          r2 = tr[r2];
        }
        // теперь r1, r2 - номера из lt2

        newVal[n] = lt2.add(r1, r2);
      }
    }

    return newVal[i];
  }
}
