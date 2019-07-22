package jsasha.mp;

import java.util.HashMap;
import java.util.Map.Entry;

public class RunStats {

  private DefProg prog;
  private HashMap<String, Cnt> nn = new HashMap();

  public RunStats(DefProg prog) {
    this.prog = prog;
  }

  public void count(String name) {
    Cnt c = nn.get(name);
    if (c == null) {
      c = new Cnt();
      nn.put(name, c);
    }
    c.n++;
  }

  private void clearUsed() {
    for (Entry<String, Cnt> i : nn.entrySet()) {
      i.getValue().used = false;
    }
  }

  public void print() {
    clearUsed();
    String space = "                         ";

    for (int i = 0; i < prog.func.length; i++) {
      String name = prog.func[i].name;
      Cnt c = nn.get(name);
      if (c != null) {
        c.used = true;
        String num = "" + c.n;
        System.out.println(space.substring(0, 25 - num.length()) + num + " " + name);
      }
    }

    for (Entry<String, Cnt> i : nn.entrySet()) {
      String name = i.getKey();
      Cnt c = i.getValue();
      if (!c.used) {
        String num = "" + c.n;
        System.out.println(space.substring(0, 25 - num.length()) + num + " " + name);
      }
    }
  }

  public static class Cnt {

    int n = 0;
    boolean used = false;
  }
}
