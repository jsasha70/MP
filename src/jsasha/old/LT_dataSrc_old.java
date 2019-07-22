package jsasha.old;

import java.util.Arrays;

public abstract class LT_dataSrc_old {

  protected int[] rels1; // from LogicTree
  protected int[] rels2; // from LogicTree
  protected int args; // from LogicTree
  protected int len; // from LogicTree
  protected int[] vars; // from LogicTree
  protected int one; // from LogicTree
  protected int[] hIndex; // from LT_hash
  protected int[] hChainLen; // from LT_hash
  protected int[] hPrev; // from LT_hash
  protected boolean[] m = null;
  protected int[] stats;

  public void ltData(int[] rels1, int[] rels2, int args, int len, int[] vars, int one, int[] stats) {
    this.rels1 = rels1;
    this.rels2 = rels2;
    this.args = args;
    this.len = len;
    this.vars = vars;
    this.one = one;
    this.stats = stats;
    if (m == null) {
      m = new boolean[len];
    } else {
      m = Arrays.copyOf(m, len);
    }
    changed();
  }

  public void hashData(int[] index, int[] chainLen, int[] prev) {
    hIndex = index;
    hChainLen = chainLen;
    hPrev = prev;
  }

  protected abstract void changed();

  protected void markAll() {
    Arrays.fill(m, true);
  }

  protected void markClearAll() {
    Arrays.fill(m, false);
  }

  protected void markVar(int i) {
    markClearAll();
    markTree(vars[i]);
  }

  protected void markTree(int node) {
    if ((node == 0) || m[node]) {
      return;
    }

    int[] stack = new int[node];
    int i = 0;
    stack[0] = node;

    int r1, r2;

    while (true) {
      m[stack[i]] = true;

      r1 = rels1[stack[i]];
      if ((r1 > 0) && !m[r1]) {
        i++;
        stack[i] = r1;
      } else {
        r2 = rels2[stack[i]];
        if ((r2 > 0) && !m[r2]) {
          i++;
          stack[i] = r2;
        } else {
          i--;
          if (i < 0) {
            break;
          }
        }
      }
    }

    m[0] = false;
  }
}
