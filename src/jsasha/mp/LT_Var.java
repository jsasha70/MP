package jsasha.mp;

import java.util.Arrays;
import jsasha.lt.LToper;
import jsasha.lt.LogicTree;

public class LT_Var {

  public int[] v; // старший бит имеет индекс 0

  public LT_Var(int size) {
    v = new int[size];
  }

  public LT_Var(LT_Var v1) {
    // копия другой переменной (для if)
    v = Arrays.copyOf(v1.v, v1.v.length);
  }

  public void join(int cond, LT_Var v2, LogicTree lt) {
    // объединение значений, зависящих от условия cond
    // если cond, то берем текущее значение,
    // а если не cond, то берем значение v2
    if (v.length != v2.v.length) {
      System.out.println("Error: LT_Var.join: length mismatch (" + v.length + " and " + v2.v.length + ")");
      throw new MpRunErr();
    }
    for (int i = 0; i < v.length; i++) {
      if (v[i] != v2.v[i]) {
        // v[i] = v[i] and cond or v2.v[i] and not cond
        v[i] = LToper.or(LToper.and(v[i], cond, lt), LToper.and(v2.v[i], LToper.not(cond, lt), lt), lt);
      }
    }
  }
}
