package jsasha.mp.compiler_utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import jsasha.mp.*;

public class FuncBlocks {

  private ArrayList<DefBlock> blocks = new ArrayList();
  private ArrayList<DefBlock> blockStack = new ArrayList();
  private HashMap<String, VarStr> allVars = new HashMap();
  private ArrayList<DefVar> localVars;

  public DefBlock[] toArray() {
    return blocks.toArray(new DefBlock[0]);
  }

  public void addBlock(DefBlock b) {
    saveVars();
    b.id = blocks.size();
    blocks.add(b);
    blockStack.add(b);
    localVars = new ArrayList();
    calcAllVars();
  }

  public void exitBlock() {
    saveVars();
    blockStack.remove(blockStack.size() - 1);
    if (blockStack.size() > 0) {
      localVars = new ArrayList(Arrays.asList(blockStack.get(blockStack.size() - 1).vars));
    } else {
      localVars = new ArrayList();
    }
    calcAllVars();
  }

  public VarStr getVar(String name) {
    return allVars.get(name);
  }

  public void addVar(DefVar v) {
    v.id = localVars.size();
    localVars.add(v);
    allVars.put(v.name, new VarStr(0, v));
  }

  public void addVar(NameStr nm) {
    DefVar v;
    v = new DefVar();
    v.name = nm.name;
    v.len = nm.len1;
    addVar(v);
  }

  private void saveVars() {
    if (blockStack.size() > 0) {
      blockStack.get(blockStack.size() - 1).vars = localVars.toArray(new DefVar[0]);
      localVars = null;
    }
  }

  private void calcAllVars() {
    allVars.clear();
    int n = blockStack.size() - 1;

    if (n > 0) {
      DefBlock b;
      DefVar v;
      for (int i = 0; i < n; i++) {
        b = blockStack.get(i);
        for (int j = 0; j < b.vars.length; j++) {
          v = b.vars[j];
          allVars.put(v.name, new VarStr(n - i, v));
        }
      }
    }
    for (DefVar var : localVars) {
      allVars.put(var.name, new VarStr(0, var));
    }
  }
}
