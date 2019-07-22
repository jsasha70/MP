package jsasha.lt;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.math.BigInteger;
import jsasha.mp.Interpretator;

public class LT_run {

  private static LogicTree lt;
  private static boolean[] vv;
  private static BigInteger retVal = null;
  private static PrintStream logStream = null;

  private static void clearBeforeRun() {
    lt = null;
    vv = null;
  }

  private static void createVV(int args, BigInteger param) {
    vv = new boolean[lt.getLen()];
    vv[0] = false;
    vv[lt.one] = true;
    for (int i = 0; i < args; i++) {
      vv[args - i] = param.testBit(i);
    }
  }

  public static boolean run(LogicTree a_lt, String[] params, File logFile) {
    clearBeforeRun();

    lt = a_lt;
    int args = lt.getArgs();
    int len = lt.getLen();

    BigInteger param = Interpretator.getParam(params, args);
    if (param == null) {
      return false;
    }

    createVV(args, param);

    for (int i = lt.one + 1; i < len; i++) {
      vv[i] = !vv[lt.getRel1(i)] || vv[lt.getRel2(i)];
    }

    // полученное значение
    retVal = BigInteger.ZERO;
    for (int i = 0; i < lt.vars.length; i++) {
      if (vv[lt.vars[i]]) {
        retVal = retVal.setBit(lt.vars.length - i - 1);
      }
    }

//    if (lt.getLen() <= 10000) {
      logOpen(logFile);
      String sp = "                                                                ";
      int nsp = 6 + ("" + (len - 1)).length() * 3;
      String s;
      int n;
      for (int i = 0; i < len; i++) {
        s = "" + i + " (" + lt.getRel1(i) + "," + lt.getRel2(i) + ")";
        n = nsp - s.length();
        n = n < 0 ? 0 : n;
        log(s + sp.substring(0, n) + (vv[i] ? 1 : 0));
      }
      logClose();
//    }

    return true;
  }

  public static void printResult() {
    System.out.println("ret val: " + retVal.toString(16));
  }

  private static void log(String s) {
    if (logStream != null) {
      logStream.println(s);
    }
  }

  private static void logOpen(File f) {
    try {
      logStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(f, false), 32768));
    } catch (FileNotFoundException e) {
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
