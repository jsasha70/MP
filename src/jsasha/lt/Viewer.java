package jsasha.lt;

import java.io.Console;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;

public class Viewer extends LT_dataSrc {

  private Console cons;
  private int depth = 1;
  private boolean treeView = false;

  public void changed() {
  }

  public void run(LogicTree lt) {
    lt.dataAccess(this);
//    calcUsage();

    if (vars == null) {
      vars = new int[0];
    }
    printHelp();

    cons = System.console();

    if (cons == null) {
      out("Error: no console");
      return;
    }

    boolean doExit = false;
    String s;
    while (!doExit) {
      out("");
      System.out.print(">>> ");
      s = cons.readLine();

      doExit = step(s);
    }
  }

  private void out(String s) {
    System.out.println(s);
  }

  private void printHelp() {
    out("args: " + args);
    out("vars: " + vars.length);
    out("len: " + len);
    out("? - this help");
    out("N - node");
    out("N-D - node N with depth D");
    out("[N] - var");
    out("[N]-D - var with depth D");
    out("t - toggle tree view");
    out("d - display depth");
    out("d N - set display depth to N");
    out("u N - usage of node N");
    out("e - exit");

  }

  private boolean step(String s) {
    switch (s) {
      case "?":
        printHelp();
        break;

      case "d":
        out("current depth: " + depth);
        break;

      case "e":
        return true;

      case "t":
        toggleTreeView();
        break;

      default:
        try {
          if (s.startsWith("d")) {
            setDepth(s);
          } else if (s.startsWith("[")) {
            viewVar(s);
          } else if (s.startsWith("u")) {
            viewUsage(s);
          } else {
            viewNode(s);
          }
        } catch (Throwable e) {
          out(e.toString());
        }
    }

    return false;
  }

  private void setDepth(String s) {
    int n = Integer.parseInt(s.substring(1).trim());
    if ((n > 0) && (n < 10)) {
      depth = n;
      out("depth set to: " + depth);
    } else {
      out("invalid depth: " + n);
      out("current depth: " + depth);
    }
  }

  private void viewVar(String s) {
    int d = depth;

    if (s.contains("-")) {
      d = Integer.parseInt(s.substring(s.indexOf("-") + 1).trim());
      s = s.substring(0, s.indexOf("-")).trim();
      if ((d < 1) || d >= 10) {
        out("invalid depth: " + d);
        return;
      }
    }

    if (!s.endsWith("]")) {
      out("incorrect var specification: " + s);
      return;
    }

    int n = Integer.parseInt(s.substring(1, s.length() - 1).trim());
    if ((n < 0) || (n >= vars.length)) {
      out("var number must be from 0 to " + (vars.length - 1));
      return;
    }

    out("var [" + n + "] = " + vars[n] + ":");
    HashSet<Integer> st = getStops(vars[n], d);
    out(nodeStr(vars[n], st));
    printTreeView(vars[n], st);
  }

  private void viewNode(String s) {
    int d = depth;

    if (s.contains("-")) {
      d = Integer.parseInt(s.substring(s.indexOf("-") + 1).trim());
      s = s.substring(0, s.indexOf("-")).trim();
      if ((d < 1) || d >= 10) {
        out("invalid depth: " + d);
        return;
      }
    }

    int n = Integer.parseInt(s.trim());
    if ((n < 0) || (n >= len)) {
      out("node number must be from 0 to " + (len - 1));
      return;
    }

    out("node " + n + " = (" + rels1[n] + "," + rels2[n] + "):");
    HashSet<Integer> st = getStops(n, d);
    out(nodeStr(n, st));
    printTreeView(n, st);
  }

  private String nodeStr(int n, HashSet<Integer> st) {
    if (n == one) {
      return "one";
    } else if ((n <= args) || st.contains(n)) {
      return "" + n;
    }

    int r1 = rels1[n];
    int r2 = rels2[n];

    String s = "(" + nodeStr(r1, st);
    while (r2 > args) {
      r1 = rels1[r2];
      r2 = rels2[r2];
      s += " " + nodeStr(r1, st);
    }
    s += ")";

    return s;
  }

  private HashSet<Integer> getStops(int node, int depth) {
    HashSet<Integer> st1 = new HashSet(100);
    getStops(st1, node, depth);

    HashSet<Integer> st2 = new HashSet(100);
    getStops2(st1, st2, node, depth);

    HashSet<Integer> st3 = new HashSet(100);
    getStops3(st2, st3, node, depth, false);
    return st3;
  }

  private void getStops(HashSet<Integer> st, int node, int depth) {
    if (node <= one) {
      return;
    }
    if (depth > 0) {
      getStops(st, rels1[node], depth - 1);
      getStops(st, rels2[node], depth);
    } else {
      st.add(node);
    }
  }

  private void getStops2(HashSet<Integer> st1, HashSet<Integer> st2, int node, int depth) {
    if (node <= one) {
      return;
    }
    if (st1.contains(node)) {
      if (depth == 0) {
        st2.add(node);
      }
    } else {
      if (depth > 0) {
        getStops2(st1, st2, rels1[node], depth - 1);
        getStops2(st1, st2, rels2[node], depth);
      } else {
        st2.add(node);
      }
    }
  }

  private void getStops3(HashSet<Integer> st2, HashSet<Integer> st3, int node, int depth, boolean isRel2) {
    if (node <= one) {
      return;
    }
    if (st2.contains(node) && !isRel2) {
      st3.add(node);
    } else {
      if (depth > 0) {
        getStops3(st2, st3, rels1[node], depth - 1, false);
        getStops3(st2, st3, rels2[node], depth, true);
      } else {
        st3.add(node);
      }
    }
  }

  private void viewUsage(String s) {
    int n = Integer.parseInt(s.substring(1).trim());
    if ((n < 0) || (n >= len)) {
      out("node number must be from 0 to " + (len - 1));
      return;
    }

    out("usage of node " + n + ":");

    ArrayList<Integer> r1 = new ArrayList();
    ArrayList<Integer> r2 = new ArrayList();

    for (int i = 0; i < len; i++) {
      if (rels1[i] == n) {
        r1.add(i);
      }
      if (rels2[i] == n) {
        r2.add(i);
      }
    }

    boolean found = false;

    if (r1.size() > 0) {
      found = true;
      out("  in rel 1:");
      for (Integer i : r1) {
        out("    " + i);
      }
    }

    if (r2.size() > 0) {
      found = true;
      out("  in rel 2:");
      for (Integer i : r2) {
        out("    " + i);
      }
    }

    if (!found) {
      out("not found");
    }
  }

  private void toggleTreeView() {
    treeView = !treeView;
    out("tree view is " + (treeView ? "on" : "off"));
  }

  private void printTreeView(int node, HashSet<Integer> st) {
    if (!treeView || (node <= one) || (node >= len)) {
      return;
    }

    TreeView tv = new TreeView();

    addNode(tv, 0, 0, node, st, false);
    System.out.println();
    tv.print(System.out);
  }

  private void addNode(TreeView tv, int line, int pos, int node,
          HashSet<Integer> st, boolean isRel2) {

    if ((node <= one) || (!isRel2 && st.contains(node))) {
      if (isRel2) {
        tv.append(line, pos, "-");
        pos++;
      }

      if (node == one) {
        tv.append(line, pos, "one");
      } else {
        tv.append(line, pos, node);
      }
      return;
    }

    if (isRel2) {
      pos += tv.appendMinus(line);
    }

    tv.append(line, pos, node);

    // печатаем rel1
    tv.append(line + 1, pos, "|");
    addNode(tv, line + 2, pos, rels1[node], st, false);

    // печатаем rel2
    addNode(tv, line, tv.getLine(line).length(), rels2[node], st, true);
  }

  private static class TreeView {

    ArrayList<StringBuilder> bb = new ArrayList(20);

    private int lineCount() {
      return bb.size();
    }

    private int lineLen(int fromLine) {
      int ret = 0;
      int nn = bb.size();
      int len;
      for (int i = fromLine; i < nn; i++) {
        len = bb.get(i).length();
        ret = ret < len ? len : ret;
      }
      return ret;
    }

    private int appendMinus(int line) {
      StringBuilder b = getLine(line);
      int nn = lineLen(line) + 1;
      int ret = 0;
      while (b.length() < nn) {
        b.append('-');
        ret++;
      }
      return ret;
    }

    private void append(int line, int pos, Object s) {
      StringBuilder b = getLine(line);
      while (b.length() < pos) {
        b.append(' ');
      }
      b.append(s);
    }

    private void print(PrintStream ps) {
      for (StringBuilder b : bb) {
        ps.println(b.toString());
      }
    }

    private StringBuilder getLine(int line) {
      while (bb.size() <= line) {
        bb.add(new StringBuilder(100));
      }
      return bb.get(line);
    }
  }
}
