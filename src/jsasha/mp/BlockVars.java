package jsasha.mp;

import java.math.BigInteger;

public class BlockVars {

  public DefBlock block;
  public Var[] vars;

  public BlockVars(DefBlock block) {
    this.block = block;
    vars = new Var[block.vars.length];
    for (int i = 0; i < vars.length; i++) {
      vars[i] = new Var(block.vars[i]);
    }
  }

  public static class Var {

    BigInteger v = BigInteger.ZERO;
    DefVar d;

    public Var(DefVar d) {
      this.d = d;
    }

    @Override
    public String toString() {
      return v.toString(2);
    }
  }
}
