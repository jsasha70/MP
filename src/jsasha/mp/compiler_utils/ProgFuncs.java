package jsasha.mp.compiler_utils;

import java.util.ArrayList;
import java.util.HashMap;
import jsasha.mp.DefFunc;

public class ProgFuncs {

  private ArrayList<DefFunc> funcs = new ArrayList();
  private HashMap<String, DefFunc> funcByName = new HashMap();
  private HashMap<String, Integer> funcNames = new HashMap();

  public DefFunc[] toArray() {
    return funcs.toArray(new DefFunc[0]);
  }

  public void add(DefFunc f) {
    f.id = funcs.size();
    funcs.add(f);
    funcByName.put(f.name, f);
    funcNames.put(f.shortName, Integer.valueOf(1));
  }

  public DefFunc byName(String name) {
    return funcByName.get(name);
  }

  public boolean isShortName(String name) {
    return funcNames.get(name) != null;
  }
}
