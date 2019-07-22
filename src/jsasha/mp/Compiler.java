package jsasha.mp;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import jsasha.mp.compiler_utils.*;
import jsasha.util.*;

public class Compiler {

  public final int maxErrCount = 10;
  private Token[] lst;
  private String[] files;
  private int pos;
  private boolean doExit;
  private boolean maxErr;
  private ArrayList<ErrDescr> errLst = new ArrayList();
  private ProgFuncs funcs;
  private DefProg prog;
  private FuncBlocks blocks;

  private void clearBeforeCompile() {
    pos = 0;
    doExit = false;
    maxErr = false;
    errLst.clear();
    funcs = new ProgFuncs();
    prog = null;
  }

  public boolean go(File src, File dst) {
    String[] ss = FileUtils.readLines(src);
    if (ss == null) {
      return false;
    }

    Parcer prc = new Parcer(src.getAbsolutePath(), ss, new ArrayList());
    boolean done = prc.go();
    if (!done) {
      return false;
    }

    lst = prc.getTokens().toArray(new Token[0]);
    files = prc.getFiles();

    done = compileProg();
    if (!done) {
      return false;
    }

    // сохранение в файл из prog
    done = FileUtils.writeObject(prog, dst);

    return done;
  }

  private void addBuiltInFunc(String name, int argLen, int retLen) {
    DefFunc func = new DefFunc();
    func.name = name + ":" + argLen + ":" + retLen;
    func.shortName = name;
    func.argLen = argLen;
    func.retLen = retLen;
    func.retFmt = new DefOutFormat[0];
    func.blocks = new DefBlock[0];
    funcs.add(func);
  }

  private void addBuiltInFuncs() {
    addBuiltInFunc("im", 2, 1);
  }

  private boolean compileProg() {
    clearBeforeCompile();

    addBuiltInFuncs();

    try {
      while (pos < lst.length) {
        if (skipComment()) {
          continue;
        }

        if (lst[pos].s.equals("func")) {
          compileFunc();
        } else {
          err("\"func\" expected, found \"" + lst[pos].s + "\"");
        }

        if (doExit || (errLst.size() > 0)) {
          printErrors();
          return false;
        }

        pos++;
      }
    } catch (UnexpectedEOF e) {
      err("Error: unexpected end of file");
      printErrors();
      return false;
    }

    prog = new DefProg();
    prog.func = funcs.toArray();
    prog.files = files;

    return true;
  }

  private void err(String s) {
    errLst.add(new ErrDescr(s, lst[pos]));
    if (errLst.size() >= maxErrCount) {
      doExit = true;
      maxErr = true;
    }
  }

  private void printErrors() {
    for (ErrDescr e : errLst) {
      System.out.println("Error in " + files[e.getFile()] + ":");
      System.out.println("  line " + e.getLine() + " pos " + e.getPos() + ": " + e.getErr());
      System.out.println();
    }
    if (maxErr) {
      System.out.println("... " + maxErrCount + " errors found, intrrupting compiling");
    }
  }

  private boolean skipComment() {
    if ((pos < lst.length) && lst[pos].s.startsWith("//")) {
      pos++;
      return true;
    }
    return false;
  }

  private Token nextToken(boolean skipComments) {
    pos++;
    if (skipComments) {
      boolean done = true;
      while (done) {
        done = skipComment();
      }
    }
    if (pos >= lst.length) {
      throw new UnexpectedEOF();
    }
    return lst[pos];
  }

  private void compileFunc() {
    Token t = nextToken(true);

    NameStr nm = new NameStr(t.s, false);
    if (nm.isErr) {
      err("func name expected (like f:16:8), found \"" + t.s + "\" (" + nm.err + ")");
      doExit = true;
      return;
    } else if ((nm.len1 == -1) || (nm.len2 == -1)) {
      err("func name expected (like f:16:8), found \"" + t.s + "\"");
      doExit = true;
      return;
    }

    if (funcs.byName(t.s) != null) {
      err("func \"" + t.s + "\" already defined");
      doExit = true;
      return;
    }

    DefFunc func = new DefFunc();
    func.name = t.s;
    func.shortName = nm.name;
    func.argLen = nm.len1;
    func.retLen = nm.len2;
    funcs.add(func);

    boolean fmtFound = false;
    t = nextToken(true);
    while (!t.s.equals("{")) {
      switch (t.s) {
        case "#out":
          if (fmtFound) {
            err("duplicated \"#out\" specification");
            doExit = true;
            return;
          }

          // формат вывода результата
          int totBits = 0;
          ArrayList<OutFmt> fmtLst = new ArrayList();
          OutFmt fmt;
          t = nextToken(true);
          while (!t.s.equals("{") && !t.s.equals("#out") && !t.s.equals("#log")) {
            fmt = new OutFmt(t.s);
            if (fmt.isErr) {
              err("data out format expected (like \"8:d\"), found \"" + t.s + "\" (" + fmt.err + ")");
            } else {
              fmtLst.add(fmt);
            }
            t = nextToken(true);
          }

          func.retFmt = new DefOutFormat[fmtLst.size()];
          for (int i = 0; i < func.retFmt.length; i++) {
            fmt = fmtLst.get(i);
            func.retFmt[i] = new DefOutFormat(fmt.len, fmt.typ);
            totBits += fmt.len;
          }

          if (totBits != func.retLen) {
            err("out format must contain " + func.retLen + " bit(s), specified " + totBits);
          }

          fmtFound = true;
          break;

        case "#log":
          func.log = true;
          t = nextToken(true);
          break;

        default:
          err("\"#out\", \"#log\" or \"{\" expected, found \"" + t.s + "\"");
          doExit = true;
          return;
      }
    }

    if (!fmtFound) {
      // формат вывода результата по умолчнию
      int tot = func.retLen;
      char typ = (tot <= 64) ? 'b' : 'h';
      int n = (tot + 7) / 8;

      func.retFmt = new DefOutFormat[n];
      int len;
      for (int i = n - 1; i >= 0; i--) {
        len = (tot > 8) ? 8 : tot;
        func.retFmt[i] = new DefOutFormat(len, typ);
        tot -= len;
      }
    }

    blocks = new FuncBlocks();
    DefBlock block = compileBlock('b', -1, func.argLen);
    if (doExit) {
      return;
    }
    if ((func.argLen + block.stackChange) != func.retLen) {
      err("stack length (" + (func.argLen + block.stackChange) + ") must be " + func.retLen);
    }

    func.blocks = blocks.toArray();

    checkFunc(func);
    if (doExit) {
      return;
    }
  }

  private void checkFunc(DefFunc func) {
    checkFuncIfElse(func);
    if (doExit) {
      return;
    }

    checkFuncLoopPoint(func);
    if (doExit) {
      return;
    }

    checkFuncPoint(func);
    if (doExit) {
      return;
    }
  }

  private DefInstr newInstr(Token t, char typ, NameStr nm, VarStr v) {
    DefInstr i = new DefInstr();
    i.line = t.line;
    i.pos = t.pos;
    i.file = t.file;
    i.text = t.s;
    i.typ = typ;
    if (nm != null) {
      i.bitsIn = nm.isIn ? nm.len1 : 0;
      i.bitsOut = nm.isOut ? ((nm.len2 == -1) ? nm.len1 : nm.len2) : 0;
    }
    if (v != null) {
      i.varRelCont = v.depth;
      i.varId = v.v.id;
    }
    return i;
  }

  private DefBlock compileBlock(char typ, int parentId, int stack) {
    int stack0 = stack;
    ArrayList<DefInstr> ii = new ArrayList();

    DefBlock block = new DefBlock();
    block.parentId = parentId;
    block.typ = typ;
    blocks.addBlock(block);

    Token t = lst[pos];
    if (!t.s.equals("{")) {
      err("\"{\" expected, found \"" + t.s + "\"");
      doExit = true;
      return null;
    }

    DefInstr instr;
    t = nextToken(false);

    while (!t.s.equals("}")) {
      instr = null;
      switch (t.s) {
        case ".":
          instr = newInstr(t, ' ', null, null);
          break;

        case "if":
        case "else":
          instr = handleIfElse(t, block.id, stack);
          break;

        case "loop":
          instr = handleLoop(t, block.id, stack);
          break;

        case "def":
          instr = handleDef(t);
          break;

        default:
          if (t.s.startsWith("//")) {
            instr = newInstr(t, ' ', null, null);
          } else if (t.s.contains(">")) {
            instr = handleExpr(t, stack);
          } else {
            err("unknown or unexpected instruction \"" + t.s + "\"");
          }
          break;
      }

      if (doExit) {
        return block;
      }

      if (instr != null) {
        stack += (instr.bitsOut - instr.bitsIn) * instr.repeat;
        if (stack < 0) {
          err("stack depth (" + stack + ") must not be negative");
        }
        instr.id = ii.size();
        ii.add(instr);
      }
      t = nextToken(false);
    }

    block.ii = ii.toArray(new DefInstr[0]);
    block.stackChange = stack - stack0;
    blocks.exitBlock();

    return block;
  }

  private DefInstr handleDef(Token t) {
    t = nextToken(true);
    if (!t.s.equals("{")) {
      err("\"{\" expected, found \"" + t.s + "\"");
      doExit = true;
      return null;
    }

    NameStr nm;
    VarStr v1;
    t = nextToken(true);
    while (!t.s.equals("}")) {
      nm = new NameStr(t.s, false);

      if (nm.isErr) {
        err("variable declaration expected (like v:8), found \"" + t.s + "\" (" + nm.err + ")");
      } else if ((nm.len1 <= 0) || (nm.len2 != -1)) {
        err("variable declaration expected (like v:8), found \"" + t.s + "\"");
      } else {
        v1 = blocks.getVar(nm.name);
        if (v1 != null) {
          err("var " + v1.v.name + " (" + v1.v.name + ":" + v1.v.len + ") already defined");
        } else if (funcs.isShortName(nm.name)) {
          err("var " + nm.fullName + " can not be named as function (" + nm.name + ")");
        } else {
          blocks.addVar(nm);
        }
      }

      t = nextToken(true);
    }
    return null;
  }

  private DefInstr handleIfElse(Token t, int parentBlock, int stack) {
    Token t0 = t;
    char typ = t.s.equals("if") ? 'i' : 's';

    t = nextToken(true);
    if (!t.s.equals("{")) {
      err("\"{\" expected, found \"" + t.s + "\"");
      doExit = true;
      return null;
    }

    if (typ == 'i') {
      stack--;
    }

    DefBlock block = compileBlock(typ, parentBlock, stack);
    if (doExit) {
      return null;
    }

    DefInstr ret = newInstr(t0, typ, null, null);
    ret.blockId = block.id;

    if (typ == 'i') {
      ret.bitsIn = 1; // для if записываем только изменение стека на бит условия
    } else {
      ret.bitsOut = block.stackChange; // изменение внутри блока записываем в else
      if (ret.bitsOut < 0) {           // потому что если else нет, то в if стек меняться не должен
        ret.bitsIn = -ret.bitsOut;
        ret.bitsOut = 0;
      }
    }

    return ret;
  }

  private DefInstr handleLoop(Token t, int parentBlock, int stack) {
    Token t0 = t;

    t = nextToken(true);
    int repeat = 0;
    try {
      repeat = Integer.parseInt(t.s);
    } catch (NumberFormatException e) {
      err("a number expected, found \"" + t.s + "\"");
      doExit = true;
      return null;
    }
    if (repeat <= 0) {
      err("repeat number must be positive");
      repeat = 1;
    }

    t = nextToken(true);
    if (!t.s.equals("{")) {
      err("\"{\" expected, found \"" + t.s + "\"");
      doExit = true;
      return null;
    }

    DefBlock block = compileBlock('l', parentBlock, stack);
    if (doExit) {
      return null;
    }

    DefInstr ret = newInstr(t0, 'l', null, null);
    ret.blockId = block.id;
    ret.repeat = repeat;

    if ((stack + block.stackChange * repeat) < 0) {
      err("stack depth (" + (stack + block.stackChange * repeat) + ") must not be negative");
      block.stackChange = 0;
    }

    ret.bitsOut = block.stackChange;
    if (ret.bitsOut < 0) {
      ret.bitsIn = -ret.bitsOut;
      ret.bitsOut = 0;
    }

    return ret;
  }

  private DefInstr handleExpr(Token t, int stack) {
    DefInstr ret = null;

    if (t.s.equals(">>_")) {
      ret = newInstr(t, 'c', null, null);
      ret.bitsIn = stack;
    } else if (t.s.startsWith(">_:")) {
      ret = newInstr(t, 'c', null, null);
      try {
        ret.bitsIn = Integer.parseInt(t.s.substring(3));
      } catch (NumberFormatException e) {
        err("not a number \"" + t.s.substring(3) + "\"");
        return null;
      }
      if (ret.bitsIn <= 0) {
        err("number of bits \"" + ret.bitsIn + "\" must be positive");
        return null;
      }
    } else {
      NameStr nm = new NameStr(t.s, true);
      if (nm.isErr) {
        err("bad instruction \"" + t.s + "\" (" + nm.err + ")");
        return null;
      } else if (nm.len1 == -1) {
        err("bad instruction \"" + t.s + "\"");
        return null;
      }

      if (nm.len2 != -1) {
        ret = handleExprFunc(t, nm);
      } else if (nm.isName) {
        ret = handleExprVar(t, nm);
      } else {
        ret = handleExprConst(t, nm);
      }
    }

    return ret;
  }

  private DefInstr handleExprVar(Token t, NameStr nm) {
    VarStr v = blocks.getVar(nm.name);
    if (v == null) {
      err("var \"" + nm.name + "\" not defined");
      return null;
    } else if (nm.len1 != v.v.len) {
      err("var \"" + nm.name + "\" has length " + v.v.len + ", not " + nm.len1);
      return null;
    }

    DefInstr ret = newInstr(t, 'v', nm, v);
    return ret;
  }

  private DefInstr handleExprFunc(Token t, NameStr nm) {
    if (!nm.isName) {
      err("bad instruction \"" + t.s + "\"");
      return null;
    }

    DefFunc f = funcs.byName(nm.fullName);
    if (f == null) {
      err("func \"" + nm.fullName + "\" not defined");
      return null;
    } else if (!nm.isIn || !nm.isOut) {
      err("bad instruction \"" + t.s + "\"");
      return null;
    }

    DefInstr ret = newInstr(t, 'f', nm, null);
    ret.funcId = f.id;
    return ret;
  }

  private DefInstr handleExprConst(Token t, NameStr nm) {
    if (nm.isIn || !nm.isOut) {
      err("bad instruction \"" + t.s + "\"");
      return null;
    }

    boolean isMinus = nm.name.startsWith("-");
    if (isMinus) {
      nm.name = nm.name.substring(1);
    }

    char typ = 'd';
    int n = nm.name.indexOf('#');
    if ((n >= 0) && ((n == 0) || (n != (nm.name.length() - 2)))) {
      err("bad instruction \"" + t.s + "\"");
      return null;
    } else if (n > 0) {
      typ = nm.name.charAt(n + 1);
      nm.name = nm.name.substring(0, n);
    }

    int radix = 10;
    switch (typ) {
      case 'd':
        radix = 10;
        break;

      case 'h':
        radix = 16;
        break;

      case 'o':
        radix = 8;
        break;

      case 'b':
        radix = 2;
        break;

      default:
        err("wrong radix \"" + typ + "\" (must be d, h, o or b)");
        return null;
    }

    BigInteger nn;
    try {
      nn = new BigInteger(nm.name, radix);
    } catch (NumberFormatException e) {
      err("wrong number \"" + nm.name + "\" (radix " + radix + ")");
      return null;
    }

    n = nn.bitLength();
    if (isMinus) {
      n++;
      nn.negate();
    }

    if (n > nm.len1) {
      err("the number length (" + n + ") is greater, then number of moving bits (" + nm.len1 + ")");
      return null;
    }

    DefInstr ret = newInstr(t, 'c', nm, null);
    ret.bits = nn.toByteArray();
    return ret;
  }

  private void err(String s, DefInstr ii) {
    errLst.add(new ErrDescr(s, ii));
    if (errLst.size() >= maxErrCount) {
      doExit = true;
      maxErr = true;
    }
  }

  private void checkFuncIfElse(DefFunc func) {
    // проверка правильности последовательности if-else
    // и изменения стека в блоках if и else

    for (int b = 0; b < func.blocks.length; b++) {
      boolean prevIf = false;
      int prevStackChange = 0;
      int stackChange;
      DefInstr[] iii = func.blocks[b].ii;
      DefInstr ii, prev_ii = null;
      for (int i = 0; i < iii.length; i++) {
        ii = iii[i];
        if (ii.typ == ' ') {
          continue;
        }

        if (prevIf) {
          if (ii.typ == 's') {
            stackChange = func.blocks[ii.blockId].stackChange;
            if (stackChange != prevStackChange) {
              err("stack change in \"else\" (" + stackChange
                      + ") differs from that in \"if\" (" + prevStackChange + ")", ii);
            }
          } else {
            if (prevStackChange != 0) {
              err("stack change in \"if\" without \"else\" must be 0, but it is " + prevStackChange, prev_ii);
            }
          }
        } else if (ii.typ == 's') {
          err("\"else\" without \"if\"", ii);
        }

        if (ii.typ == 'i') {
          prevIf = true;
          prevStackChange = func.blocks[ii.blockId].stackChange;
          prev_ii = ii;
        } else {
          prevIf = false;
        }
      }
    }
  }

  private void checkFuncPoint(DefFunc func) {
    // проверка нулевой длины стека на командах "."
    checkFuncPointBlock(func.argLen, func.blocks[0], func);
  }

  private void checkFuncPointBlock(int stack, DefBlock b, DefFunc func) {
    if ((b.typ == 'l') && (b.stackChange != 0)) {
      return; // точки в блоке loop с меняющимся стаком быть не может
    }

    DefInstr ii;
    for (int i = 0; i < b.ii.length; i++) {
      ii = b.ii[i];

      switch (ii.typ) {
        case ' ':
          if (ii.text.equals(".") && (stack != 0)) {
            err("stack depth " + stack + " is not zero", ii);
          }
          break;

        case 'b':
        case 'l':
          checkFuncPointBlock(stack, func.blocks[ii.blockId], func);
          break;

        case 'i':
          stack--;
          checkFuncPointBlock(stack, func.blocks[ii.blockId], func);
          stack++;
          break;

        case 's':
          checkFuncPointBlock(stack - func.blocks[ii.blockId].stackChange,
                  func.blocks[ii.blockId], func);
          break;
      }

      stack += ii.bitsOut - ii.bitsIn;
      if (stack < 0) {
        err("stack depth " + stack + " is less then zero", ii);
      }
    }
  }

  private void checkFuncLoopPoint(DefFunc func) {
    // точка и ">>_" в блоке loop и вложенных блоках допустима только если блок loop не меняет глубину стека
    checkFuncLoopPointBlock(func.blocks[0], func, false);
  }

  private void checkFuncLoopPointBlock(DefBlock b, DefFunc func, boolean noPoint) {
    if ((b.typ == 'l') && (b.stackChange != 0)) {
      noPoint = true;
    }

    DefInstr ii;
    for (int i = 0; i < b.ii.length; i++) {
      ii = b.ii[i];

      switch (ii.typ) {
        case 'b':
        case 'l':
        case 'i':
        case 's':
          checkFuncLoopPointBlock(func.blocks[ii.blockId], func, noPoint);
          break;

        default:
          if (noPoint) {
            switch (ii.text) {
              case ".":
              case ">>_":
                err("instruction \"" + ii.text + "\" is impossible inside a loop with changing stack depth", ii);
                break;
            }
          }
          break;
      }
    }
  }
}
