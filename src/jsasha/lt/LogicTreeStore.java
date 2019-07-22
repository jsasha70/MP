package jsasha.lt;

import java.io.File;
import java.util.Arrays;
import jsasha.util.FileUtils;

public class LogicTreeStore {

  public static boolean writeLT(LogicTree lt, File file, boolean doCondense) {
    if (doCondense) {
      lt = condenseLT(lt);
    }
    lt.trimArrays();
    lt.clearServiceData();
    boolean done = FileUtils.writeObject(lt, file);
    System.out.println("Saved " + lt.getLen() + " nodes");
    return done;
  }

  public static LogicTree condenseLT(LogicTree lt) {
    if ((lt.vars == null) || (lt.vars.length == 0)) {
      throw new LogicTreeException("condenseLT: no vars defined");
    }

    int len = lt.getLen();
    boolean[] m = new boolean[len];
    Arrays.fill(m, false);
    Arrays.fill(m, 0, lt.one + 1, true);

    for (int i = 0; i < lt.vars.length; i++) {
      markTree(lt.vars[i], lt, m);
    }

    int nExtra = 0;
    int maxUsed = 0;
    for (int i = 0; i < len; i++) {
      if (m[i]) {
        maxUsed = i;
      } else {
        nExtra++;
      }
    }

    if (nExtra > 0) {
      lt = runCondence(lt, m, maxUsed + 1, lt.getArgs());
    }

    return lt;
  }

  private static void markTree(int node, LogicTree lt, boolean[] m) {
    if (m[node]) {
      return;
    }

    if (node == 0) {
      m[node] = true;
      return;
    }

    int[] stack = new int[node];
    int i = 0;
    stack[0] = node;

    int r1, r2;

    while (true) {
      m[stack[i]] = true;

      r1 = lt.getRel1(stack[i]);
      if ((r1 >= 0) && !m[r1]) {
        i++;
        stack[i] = r1;
      } else {
        r2 = lt.getRel2(stack[i]);
        if ((r2 >= 0) && !m[r2]) {
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
  }

  private static LogicTree runCondence(LogicTree lt, boolean[] m, int len, int args) {
    LogicTree lt2 = new LogicTree(args, len);

    int nn = lt2.getLen();
    int[] tr = new int[len]; // трансляция номера узла из lt в lt2
    for (int i = 0; i < nn; i++) {
      tr[i] = i;
    }

    int r1, r2;
    for (int i = nn; i < len; i++) {
      if (m[i]) {
        r1 = tr[lt.getRel1(i)];
        r2 = tr[lt.getRel2(i)];
        tr[i] = lt2.add(r1, r2);
      } else {
        tr[i] = -1;
      }
    }

    lt2.vars = new int[lt.vars.length];
    for (int i = 0; i < lt2.vars.length; i++) {
      lt2.vars[i] = lt2.xMinMin(tr[lt.vars[i]]);
    }

    System.out.println("condensed from " + lt.getLen() + " to " + lt2.getLen());

    return lt2;
  }
}
