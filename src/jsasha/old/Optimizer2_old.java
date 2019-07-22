package jsasha.old;

import java.util.ArrayList;
import jsasha.lt.LogicTree;

/**
 * сортировка списков
 * 
 */
public class Optimizer2_old {

  public LogicTree run(LogicTree lt) {
    int len = lt.getLen();
    int args = lt.getArgs();
    LogicTree ret = new LogicTree(args, len);
    int[] tr = new int[len];

    for (int i = 0; i <= args; i++) {
      tr[i] = i;
    }
    tr[lt.one] = ret.one;

    int r1, r2, r21, r22, last = 0;
    for (int i = args + 1; i < len; i++) {
      tr[i] = ret.add(tr[lt.getRel1(i)], tr[lt.getRel2(i)]);
      r1 = ret.getRel1(tr[i]);
      r2 = ret.getRel2(tr[i]);

      if (r2 > args) {
        r21 = ret.getRel1(r2);
        r22 = ret.getRel2(r2);

        if (r1 < r21) {
          // вставляем в середину списка
          ArrayList<Integer> ss = new ArrayList();
          // поиск места
          while (r1 < r21) {
            ss.add(r21);
            last = r22;
            r21 = ret.getRel1(r22);
            r22 = ret.getRel2(r22);
          }
          last = ret.add(r1, last);
          while (ss.size() > 0) {
            last = ret.add(ss.get(ss.size() - 1), last);
            ss.remove(ss.size() - 1);
          }
          tr[i] = last;
        }
      }
    }

    ret.vars = new int[lt.vars.length];
    for (int i = 0; i < lt.vars.length; i++) {
      ret.vars[i] = tr[lt.vars[i]];
    }

    return ret;
  }
}
