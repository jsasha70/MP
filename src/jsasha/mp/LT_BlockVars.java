package jsasha.mp;

import jsasha.lt.LToper;
import jsasha.lt.LogicTree;

public class LT_BlockVars {

  public DefBlock block;
  public Var[] vars;

  public LT_BlockVars(DefBlock block) {
    this.block = block;
    vars = new Var[block.vars.length];
    for (int i = 0; i < vars.length; i++) {
      vars[i] = new Var(block.vars[i]);
    }
  }

  public LT_BlockVars(LT_BlockVars b1) {
    block = b1.block;
    vars = new Var[b1.vars.length];
    for (int i = 0; i < b1.vars.length; i++) {
      vars[i] = new Var(b1.vars[i]);
    }
  }

  public void join(int cond, LT_BlockVars bw, LogicTree lt) {
    if (vars.length != bw.vars.length) {
      System.out.println("Error: LT_BlockVars.join: length mismatch ("
              + vars.length + " and " + bw.vars.length + ")");
      throw new MpRunErr();
    }
    for (int i = 0; i < vars.length; i++) {
      vars[i].v.join(cond, bw.vars[i].v, lt);
    }
  }

  public static class Var {

    LT_Var v;
    DefVar d;

    public Var(DefVar d) {
      this.d = d;
      v = new LT_Var(d.len);
    }

    public Var(Var v1) {
      this.d = v1.d;
      v = new LT_Var(v1.v);
    }
  }
}
