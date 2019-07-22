package jsasha.mp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import java.util.ArrayList;

public class Interpretator {

  private static DefProg prog;
  private static DefFunc func;
  private static BigInteger stack;
  private static BigInteger progRet;
  private static int stackLen;
  private static RunStats stats;
  private static PrintStream logStream = null;
  private static int logMargin = 0;

  private static void clearBeforeRun() {
    prog = null;
    func = null;
    stack = BigInteger.ZERO;
    progRet = BigInteger.ZERO;
    stackLen = 0;
  }

  public static BigInteger getParam(String[] params, int argLen) {
    BigInteger b, b1;
    BigInteger ret = BigInteger.ZERO;
    int retLen = 0;
    int bLen;

    for (int i = 0; i < params.length; i++) {
      String s = params[i];

      if ((params.length == 1) && !s.contains(":")) {
        bLen = argLen;
      } else {
        int n = s.indexOf(':');
        if (n == -1) {
          err("no param size (after colon): \"" + params[i] + "\"");
          return null;
        } else if (n == (s.length() - 1)) {
          err("no param size (after colon): \"" + params[i] + "\"");
          return null;
        }

        try {
          bLen = Integer.parseInt(s.substring(n + 1));
        } catch (NumberFormatException e) {
          err("bad param size (after colon): \"" + params[i] + "\"");
          return null;
        }

        s = s.substring(0, n);
      }
      retLen += bLen;

      if (s.isEmpty()) {
        err("no number: \"" + params[i] + "\"");
        return null;
      }

      boolean isMinus = s.startsWith("-");
      if (isMinus) {
        s = s.substring(1);
      }

      if (s.isEmpty()) {
        err("no number: \"" + params[i] + "\"");
        return null;
      }

      char typ = 'd';
      int n = s.indexOf('#');
      if ((n >= 0) && ((n == 0) || (n != (s.length() - 2)))) {
        err("bad number \"" + s + "\"");
        return null;
      } else if (n > 0) {
        typ = s.charAt(n + 1);
        s = s.substring(0, n);
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

      b = null;
      try {
        b = new BigInteger(s, radix);
      } catch (NumberFormatException e) {
        err("wrong number \"" + s + "\" (radix " + radix + ")");
        return null;
      }

      n = b.bitLength();
      if (isMinus) {
        n++;
        b.negate();
      }

      if (n > bLen) {
        err("the number length of \"" + s + "\" (" + n
                + ") is greater, then length of argument (" + bLen + ")");
        return null;
      }

      if (i != 0) {
        ret = ret.shiftLeft(bLen);
      }
      b1 = BigInteger.ONE.shiftLeft(bLen).subtract(BigInteger.ONE);
      ret = ret.or(b1.and(b));
    }

    if (retLen != argLen) {
      err("the total number length \" (" + retLen
              + ") differs from required length (" + argLen + ")");
      return null;
    }

    return ret;
  }

  public static boolean run(DefProg a_prog, String funcName, String[] params, File logFile) {
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

    BigInteger param = getParam(params, func.argLen);
    if (param == null) {
      return false;
    }

    toStack(param, func.argLen);

    logOpen(logFile, func.log);
    callFunc(func.id, func.log);
    logClose();

    if (stackLen != func.retLen) {
      err1("stack len on prog exit (" + stackLen
              + ") differs from required (" + func.retLen + ")", null);
    }

    progRet = fromStack(stackLen, null);

    return true;
  }

  public static void printResult() {
    stats.print();

    System.out.print("ret val: ");
    DefOutFormat[] fmt = func.retFmt;
    for (int i = 0; i < fmt.length; i++) {
      switch (fmt[i].typ) {
        case 'b':
          String s = progRet.toString(2);
          while (s.length() < fmt[i].len) {
            s = "0" + s;
          }
          System.out.print(s + " ");
          break;
        case 'o':
          System.out.print(progRet.toString(8) + " ");
          break;
        case 'd':
          System.out.print(progRet.toString(10) + " ");
          break;
        case 'h':
          System.out.print(progRet.toString(16) + " ");
          break;
      }
    }
    System.out.println();
  }

  public static void toStack(BigInteger b, int len) {
    BigInteger b1 = BigInteger.ONE.shiftLeft(len).subtract(BigInteger.ONE);
    if (stackLen > 0) {
      stack = stack.shiftLeft(len).or(b1.and(b));
    } else {
      stack = stack.or(b1.and(b));
    }
    stackLen += len;
  }

  public static BigInteger fromStack(int len, DefInstr ii) {
    BigInteger ret = copyStack(len, ii);
    stack = stack.shiftRight(len);
    stackLen -= len;
    return ret;
  }

  public static BigInteger copyStack(int len, DefInstr ii) {
    if (stackLen < len) {
      err1("stack len (" + stackLen + ") is less then required (" + len + ")", ii);
    }

    BigInteger b1 = BigInteger.ONE.shiftLeft(len).subtract(BigInteger.ONE);
    BigInteger ret = stack.and(b1);
    return ret;
  }

  private static void callFuncBuiltinIm21() {
    if (stack.testBit(0) || !stack.testBit(1)) {
      stack = stack.shiftRight(1).setBit(0);
    } else {
      stack = stack.shiftRight(1).clearBit(0);
    }
    stackLen--;
  }

  private static void callFuncBuiltin(int funcId) {
    DefFunc f = prog.func[funcId];

    switch (funcId) {
      case 0:
        if (f.name.equals("im:2:1")) {
          callFuncBuiltinIm21();
          return;
        }
        break;
    }

    err1("builtin func mismatch (" + funcId + ")", null);
  }

  private static void callFunc(int funcId, boolean logRun) {
    DefFunc f = prog.func[funcId];
    logRun = logRun && f.log;

    stats.count(f.name);

    int stackLen0 = stackLen - f.argLen;
    if (stackLen0 < 0) {
      err1("stack len (" + stackLen + ") is less then required (" + f.argLen + ")", null);
    }

    if (funcId < 1) {
      callFuncBuiltin(funcId);
      return;
    }

    ArrayList<BlockVars> vv = new ArrayList();

    callBlock(f, vv, 0, stackLen0, logRun);
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

  private static BlockVars.Var getVar(DefInstr ii, ArrayList<BlockVars> vv) {
    return vv.get(vv.size() - ii.varRelCont - 1).vars[ii.varId];
  }

  private static void callBlock(DefFunc f, ArrayList<BlockVars> vv,
          int blockId, int stackLen0, boolean logRun) {

    DefBlock block = f.blocks[blockId];
    vv.add(new BlockVars(block));
    logMargin++;

    DefInstr ii;
    BlockVars.Var var = null;
    for (int i = 0; i < block.ii.length; i++) {
      ii = block.ii[i];

      if (((ii.blockId > 0) || (ii.funcId > 0)) && (ii.typ != 'i') && (ii.typ != 's')) {
        log(ii, vv, logRun);
      }

      switch (ii.typ) {
        case 'c':
          if (ii.bitsIn > 0) {
            fromStack(ii.bitsIn, ii);
            if (ii.text.equals(">>_") && (stackLen != stackLen0)) {
              err1("stack length must be " + stackLen0 + " but it is " + stackLen, ii);
            }
          } else if (ii.bitsOut > 0) {
            toStack(new BigInteger(ii.bits), ii.bitsOut);
          } else {
            err1("invalid instruction", ii);
          }
          break;

        case 'v':
          var = getVar(ii, vv);
          if ((ii.bitsIn > 0) && (ii.bitsOut > 0)) {
            var.v = copyStack(var.d.len, ii);
          } else if (ii.bitsIn > 0) {
            var.v = fromStack(var.d.len, ii);
          } else if (ii.bitsOut > 0) {
            toStack(var.v, var.d.len);
          } else {
            err1("invalid instruction", ii);
          }
          break;

        case 'f':
          callFunc(ii.funcId, logRun);
          break;

        case 'i':
          stats.count("if");
          boolean cond = stack.testBit(0);
          stack = stack.shiftRight(1);
          stackLen--;
          if (cond) {
            log(ii, vv, logRun);
            callBlock(f, vv, ii.blockId, stackLen0, logRun);
          } else if ((i + 1) < block.ii.length) { // обработка следующей за "if" инструкции "else"
            ii = block.ii[i + 1];
            if (ii.typ == 's') {
              log(ii, vv, logRun);
              callBlock(f, vv, ii.blockId, stackLen0, logRun);
            }
          }
          break;

        case 's':
          // "else" обрабатывается вместе с if
          // второй раз орабатывать не надо
          break;

        case 'l':
          for (int j = 0; j < ii.repeat; j++) {
            callBlock(f, vv, ii.blockId, stackLen0, logRun);
          }
          break;

        case ' ':
          if (ii.text.equals(".") && (stackLen != stackLen0)) {
            err1("stack length must be " + stackLen0 + " but it is " + stackLen, ii);
          }
          break;

        case 'b':
          callBlock(f, vv, ii.blockId, stackLen0, logRun);
          break;

        default:
          err1("unknown instruction type \"" + ii.typ + "\" (\"" + ii.text + "\")", ii);
          break;
      }

      if (((ii.blockId == 0) && (ii.funcId == 0)) && (ii.typ != 'i') && (ii.typ != 's')) {
        log(ii, vv, logRun);
      }

      if (stackLen < stackLen0) {
        err1("stack length must not be less then " + stackLen0 + ", but it is " + stackLen, ii);
      }
    }

    vv.remove(vv.size() - 1);
    logMargin--;
  }

  private static void log(DefInstr ii, ArrayList<BlockVars> vv, boolean logRun) {
    if (!logRun) {
      return;
    }

    if (logStream != null) {
      String s = null;

      switch (ii.typ) {
        case 'v':
          BlockVars.Var v = getVar(ii, vv);
          s = v.d.name + ":" + v.d.len + " = " + v.v.toString(16);
          break;

        case ' ':
          if (ii.text.equals(".")) {
            break;
          } else {
            return;
          }
      }

      String e = "                                                                                                    ";
      int n = logMargin * 2;
      n = n > 100 ? 100 : n;
      String s1 = e.substring(0, n) + ii.text;

      n = s1.length();
      n = n >= 25 ? 0 : (25 - n);
      s1 = s1 + e.substring(0, n) + " " + stackLen;

      if (s != null) {
        n = s1.length();
        n = (n >= 30) ? 0 : (30 - n);
        s1 = s1 + e.substring(0, n) + " " + s;
      }

      n = s1.length();
      n = n >= 55 ? 0 : (55 - n);
      s1 = s1 + e.substring(0, n) + " (" + ii.file + "-" + ii.line + "-" + ii.pos + ")";

      logStream.println(s1);
    }
  }

  private static void log(String s, boolean logRun) {
    if (!logRun) {
      return;
    }

    if (logStream != null) {
      String e = "                                                                                                    ";
      int n = logMargin * 2;
      n = n > 100 ? 100 : n;
      logStream.println(e.substring(0, n) + s);
    }
  }

  private static void logOpen(File f, boolean logRun) {
    if (logRun) {
      try {
        logStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(f, false), 32768));
        logMargin = -1;
      } catch (FileNotFoundException e) {
        logStream = null;
      }
    } else {
      logStream = null;
    }
  }

  private static void logClose() {
    if (logStream != null) {
      logStream.close();
      logStream = null;
    }
  }
}
