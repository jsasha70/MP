package jsasha.mp.compiler_utils;

import jsasha.mp.DefVar;

public class VarStr {

  public int depth;
  public DefVar v;

  public VarStr(int depth, DefVar v) {
    this.depth = depth;
    this.v = v;
  }
}
