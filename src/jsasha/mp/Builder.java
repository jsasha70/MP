package jsasha.mp;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import jsasha.lt.LToper;
import jsasha.lt.LogicTree;
import jsasha.lt.LogicTreeStore;
import jsasha.lt.Stats;

public class Builder {

  private static DefProg prog;
  private static DefFunc func;
  private static LogicTree lt;
  private static RunStats stats;

  private static void clearBeforeRun() {
    prog = null;
    func = null;
    lt = null;
  }

  public static boolean run(DefProg a_prog, String funcName, File outFile) {
    clearBeforeRun();

    prog = a_prog;
    stats = new RunStats(prog);

    if (funcName == null) {
      func = prog.func[prog.func.length - 1];
    } else {
      for (int i = 0; i < prog.func.length; i++) {
        if (prog.func[i].name.equals(funcName) && (prog.func[i].blocks.length > 0)) {
          func = prog.func[i];
          break;
        }
      }
    }
    if (func == null) {
      err("func \"" + funcName + "\" not found in the file (or is builtin)");
      return false;
    }

    lt = new LogicTree(func.argLen);
    LT_Context ctx = new LT_Context(new LT_FuncStack(func.argLen));

    callFunc(func.id, ctx);

    if (ctx.st.getLen() != func.retLen) {
      err1("stack len on prog exit (" + ctx.st.getLen()
              + ") differs from required (" + func.retLen + ")", null);
    }
    lt.vars = ctx.st.getAll();
    for (int i = 0; i < lt.vars.length; i++) {
      lt.vars[i] = lt.xMinMin(lt.vars[i]);
    }

    LogicTree lt2 = LogicTreeStore.condenseLT(lt);

    ArrayList<int[]> ss = new ArrayList();
    ss.add(lt.stats);
    ss.add(lt2.stats);
    Stats stats2 = new Stats();
    stats2.init(lt2, ss);
    stats2.calc(new File(outFile.getPath() + ".xls"));

    boolean done = lt2.toFile(outFile, true);
    return done;
  }

  public static void printResult() {
    stats.print();
    System.out.println("LT len: " + lt.getLen());
    System.out.println("done");
  }

  private static boolean callBuiltinAnd(DefFunc f, LT_Context ctx) {
    if (f.argLen != (f.retLen * 2)) {
      return false;
    }

    int len = ctx.st.getLen();
    int n1 = len - f.argLen;
    int n2 = len - (f.argLen - f.retLen);
    int[] ss = ctx.st.getStInternal();
    for (int i = 0; i < f.retLen; i++) {
      ss[n1 + i] = LToper.and(ss[n1 + i], ss[n2 + i], lt);
    }
    ctx.st.decreaseLen(f.argLen - f.retLen);
    return true;
  }

  private static boolean callBuiltinOr(DefFunc f, LT_Context ctx) {
    if (f.argLen != (f.retLen * 2)) {
      return false;
    }

    int len = ctx.st.getLen();
    int n1 = len - f.argLen;
    int n2 = len - (f.argLen - f.retLen);
    int[] ss = ctx.st.getStInternal();
    for (int i = 0; i < f.retLen; i++) {
      ss[n1 + i] = LToper.or(ss[n1 + i], ss[n2 + i], lt);
    }
    ctx.st.decreaseLen(f.argLen - f.retLen);
    return true;
  }

  private static boolean callBuiltinXor(DefFunc f, LT_Context ctx) {
    if (f.argLen != (f.retLen * 2)) {
      return false;
    }

    int len = ctx.st.getLen();
    int n1 = len - f.argLen;
    int n2 = len - (f.argLen - f.retLen);
    int[] ss = ctx.st.getStInternal();
    for (int i = 0; i < f.retLen; i++) {
      ss[n1 + i] = LToper.xor(ss[n1 + i], ss[n2 + i], lt);
    }
    ctx.st.decreaseLen(f.argLen - f.retLen);
    return true;
  }

  private static boolean callBuiltinIm(DefFunc f, LT_Context ctx) {
    if (f.argLen != (f.retLen * 2)) {
      return false;
    }

    int len = ctx.st.getLen();
    int n1 = len - f.argLen;
    int n2 = len - (f.argLen - f.retLen);
    int[] ss = ctx.st.getStInternal();
    for (int i = 0; i < f.retLen; i++) {
      ss[n1 + i] = LToper.im(ss[n1 + i], ss[n2 + i], lt);
    }
    ctx.st.decreaseLen(f.argLen - f.retLen);
    return true;
  }

  private static boolean callBuiltinNot(DefFunc f, LT_Context ctx) {
    if (f.argLen != f.retLen) {
      return false;
    }

    int len = ctx.st.getLen();
    int n1 = len - f.argLen;
    int[] ss = ctx.st.getStInternal();
    for (int i = 0; i < f.retLen; i++) {
      ss[n1 + i] = LToper.not(ss[n1 + i], lt);
    }
    return true;
  }

  private static boolean callFuncBuiltin(int funcId, LT_Context ctx) {
    DefFunc f = prog.func[funcId];

    switch (f.shortName) {
      case "im":
        return callBuiltinIm(f, ctx);

      case "and":
        return callBuiltinAnd(f, ctx);

      case "or":
        return callBuiltinOr(f, ctx);

      case "not":
        return callBuiltinNot(f, ctx);

      case "xor":
        return callBuiltinXor(f, ctx);
    }
    return false;
  }

  private static void callFunc(int funcId, LT_Context ctx1) {
    DefFunc f = prog.func[funcId];
    stats.count(f.name);

    LT_Context ctx = new LT_Context(new LT_FuncStack(ctx1.st, f.argLen));

    if (callFuncBuiltin(funcId, ctx)) {
      ctx1.st.push(ctx.st);
      return;
    }

    callBlock(f, ctx, 0);

    if (ctx.st.getLen() != f.retLen) {
      err1("stack len on func " + f.name + " exit (" + ctx.st.getLen()
              + ") differs from required (" + f.retLen + ")", null);
    }

    ctx1.st.push(ctx.st);
  }

  private static void err1(String err, DefInstr ii) {
    if (ii == null) {
      System.out.println("Error: " + err);
    } else {
      System.out.println("Error in " + ii.file + "-" + ii.line + "-" + ii.pos + ": " + err);
    }
    throw new MpRunErr();
  }

  private static void err(String err) {
    System.out.println("Error: " + err);
  }

  private static LT_BlockVars.Var getVar(DefInstr ii, LT_Context ctx) {
    return ctx.vv.get(ctx.vv.size() - ii.varRelCont - 1).vars[ii.varId];
  }

  private static void callBlock(DefFunc f, LT_Context ctx, int blockId) {
    DefBlock block = f.blocks[blockId];
    ctx.vv.add(new LT_BlockVars(block));

    DefInstr ii;
    LT_BlockVars.Var var = null;
    for (int i = 0; i < block.ii.length; i++) {
      ii = block.ii[i];

      switch (ii.typ) {
        case 'c':
          if (ii.bitsIn > 0) {
            ctx.st.decreaseLen(ii.bitsIn);
            if (ii.text.equals(">>_") && (ctx.st.getLen() != 0)) {
              err1("func stack length is not zero: " + ctx.st.getLen(), ii);
            }
          } else if (ii.bitsOut > 0) {
            ctx.st.push(new BigInteger(ii.bits), ii.bitsOut, lt.one);
          } else {
            err1("invalid instruction", ii);
          }
          break;

        case 'v':
          var = getVar(ii, ctx);
          if ((ii.bitsIn > 0) && (var.d.len != ii.bitsIn)) {
            err1("var " + var.d.name + ":" + var.d.len + " lenhth mismatch: ii.bitsIn=" + ii.bitsIn, ii);
          }
          if ((ii.bitsOut > 0) && (var.d.len != ii.bitsOut)) {
            err1("var " + var.d.name + ":" + var.d.len + " lenhth mismatch: ii.bitsOut=" + ii.bitsOut, ii);
          }
          if ((ii.bitsIn > 0) && (ii.bitsOut > 0)) {
            ctx.st.copy(var.v);
          } else if (ii.bitsIn > 0) {
            ctx.st.pop(var.v);
          } else if (ii.bitsOut > 0) {
            ctx.st.push(var.v);
          } else {
            err1("invalid instruction", ii);
          }
          break;

        case 'f':
          callFunc(ii.funcId, ctx);
          break;

        case 'i':
          stats.count("if");
          int cond = ctx.st.popBit();
          DefInstr iiElse = null;
          if ((i + 1) < block.ii.length) { // поиск следующей за "if" инструкции "else"
            iiElse = block.ii[i + 1];
            if (iiElse.typ != 's') {
              iiElse = null;
            }
          }
          LT_Context ctx2 = new LT_Context(ctx);

          // выполняем if
          callBlock(f, ctx, ii.blockId);

          if (iiElse != null) {
            // выполняем else
            callBlock(f, ctx2, iiElse.blockId);
          }

          // соединяем результаты
          ctx.join(cond, ctx2, lt);
          break;

        case 's':
          // "else" обрабатывается вместе с if
          // второй раз орабатывать не надо
          break;

        case 'l':
          for (int j = 0; j < ii.repeat; j++) {
            callBlock(f, ctx, ii.blockId);
          }
          break;

        case ' ':
          if (ii.text.equals(".") && (ctx.st.getLen() != 0)) {
            err1("func stack length is not zero: " + ctx.st.getLen(), ii);
          }
          break;

        case 'b':
          callBlock(f, ctx, ii.blockId);
          break;

        default:
          err1("unknown instruction type \"" + ii.typ + "\" (\"" + ii.text + "\")", ii);
          break;
      }
    }

    ctx.vv.remove(ctx.vv.size() - 1);
  }
}
