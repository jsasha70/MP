package jsasha.lt;

public class LToper {

  public static int not(int x, LogicTree lt) {
    int args = lt.getArgs();
    if (x <= args) {
      return lt.add(x, 0);
    }

    int r2 = lt.getRel2(x);
    if (r2 != 0) {
      return lt.add(x, 0);
    }

    return lt.getRel1(x);
  }

  public static int or(int x1, int x2, LogicTree lt) {
    int args = lt.getArgs();
    if (x1 == 0) { // 0 + x2
      return x2;
    } else if (x2 == 0) { // x1 + 0
      return x1;
    } else if ((x1 == lt.one) || (x2 == lt.one)) { // 1 + ...
      return lt.one;
    } else {
      if ((x2 > args) || (x1 <= args)) {
        return lt.add(lt.add(x1, 0), x2);
      } else {
        return lt.add(lt.add(x2, 0), x1);
      }
    }
  }

  public static int and(int x1, int x2, LogicTree lt) {
    if ((x1 == 0) || (x2 == 0)) { // 0 * ...
      return 0;
    } else if (x1 == lt.one) { // 1 * x2
      return x2;
    } else if (x2 == lt.one) { // x1 * 1
      return x1;
    } else {
      return lt.add(lt.add(x1, lt.add(x2, 0)), 0);
    }
  }

  public static int xor(int x1, int x2, LogicTree lt) {
    if (x1 == 0) {
      return x2;
    } else if (x2 == 0) {
      return x1;
    } else if (x1 == lt.one) {
      return not(x2, lt);
    } else if (x2 == lt.one) {
      return not(x1, lt);
    } else {
      return lt.add(lt.add(x1, x2), lt.add(lt.add(x2, x1), 0));
    }
  }

  public static int im(int x1, int x2, LogicTree lt) {
    return lt.add(x1, x2);
  }
}
