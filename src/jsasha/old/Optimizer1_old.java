package jsasha.old;

import jsasha.lt.LogicTree;

/**
 * конкатенация списков
 * (устранение переходов через ноль)
 */
public class Optimizer1_old {

  public LogicTree run(LogicTree lt) {
    int len = lt.getLen();
    int args = lt.getArgs();
    LogicTree ret = new LogicTree(args, len);
    int[] tr = new int[len];

    for (int i = 0; i <= args; i++) {
      tr[i] = i;
    }
    tr[lt.one] = ret.one;

    int r1, r2, r11, r12, r111, r112, last;
    for (int i = args + 1; i < len; i++) {
      tr[i] = ret.add(tr[lt.getRel1(i)], tr[lt.getRel2(i)]);
      r1 = ret.getRel1(tr[i]);
      r2 = ret.getRel2(tr[i]);

      if (r1 > args) {
        r11 = ret.getRel1(r1);
        r12 = ret.getRel2(r1);
        if ((r12 == 0) && (r11 > args)) {
          r112 = r11;
          last = r2;
          do {
            r111 = ret.getRel1(r112);
            last = ret.add(r111, last);
            r112 = ret.getRel2(r112);
          } while (r112 > args);

          tr[i] = last;
        }
      }
    }

    ret.vars = new int[lt.vars.length];
    for (int i = 0; i < lt.vars.length; i++) {
      ret.vars[i] = ret.xMinMin(tr[lt.vars[i]]);
    }

    return ret;
  }
}
