package jsasha.mp;

import java.util.ArrayList;
import jsasha.lt.LogicTree;

public class LT_Context {

  public ArrayList<LT_BlockVars> vv = new ArrayList();
  public LT_FuncStack st;

  public LT_Context(LT_FuncStack st) {
    this.st = st;
  }

  public LT_Context(LT_Context c1) {
    st = new LT_FuncStack(c1.st);
    for (LT_BlockVars i : c1.vv) {
      vv.add(new LT_BlockVars(i));
    }
  }

  public void join(int cond, LT_Context c2, LogicTree lt) {
    if (vv.size() != c2.vv.size()) {
      System.out.println("Error: LT_Context.join: length mismatch ("
              + vv.size() + " and " + c2.vv.size() + ")");
      throw new MpRunErr();
    }

    st.join(cond, c2.st, lt);

    int n = vv.size();
    for (int i = 0; i < n; i++) {
      vv.get(i).join(cond, c2.vv.get(i), lt);
    }
  }
}
