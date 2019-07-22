package jsasha.mp.run;

import jsasha.old.Optimizer2_old;
import jsasha.old.Optimizer1_old;
import jsasha.lt.opt.*;
import jsasha.lt.test.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import jsasha.lt.*;
import jsasha.lt.test.Test4;
import jsasha.mp.*;
import jsasha.mp.Compiler;
import jsasha.mp.compiler_utils.NameStr;
import jsasha.util.FileUtils;

public class Main {

  public static void main(String[] args) {
    if (args.length < 1) {
      printUsage();
    }

    switch (args[0]) {
      case "c":
        startCompile(args, false);
        break;

      case "r":
        startRun(args);
        break;

      case "cr":
        startCompile(args, true);
        args[1] = args[1] + "p";
        startRun(args);
        break;

      case "b":
        startBuild(args);
        break;

      case "s":
        startStats(args);
        break;

      case "o":
        startOptimize(args);
        break;

      case "t":
        startTest(args);
        break;

      case "view":
        startView(args);
        break;

      default:
        printUsage();
    }
  }

  public static void printUsage() {
    System.out.println("Usage:");
    System.out.println("  compile:");
    System.out.println("    java -jar mp.jar c <source-file.mp> [<dest-file.mpp>]");
    System.out.println("  run:");
    System.out.println("    java -jar mp.jar r <source-file.mpp> [<func_name>] <args>");
    System.out.println("    java -jar mp.jar r <source-file.lt> <args>");
    System.out.println("      <args> - numbers (123:8 123#h:10 123#o:16 100#b:3)");
    System.out.println("  compile and run:");
    System.out.println("    java -jar mp.jar cr <source-file.mp> [<func_name>] <args>");
    System.out.println("  build logic tree:");
    System.out.println("    java -jar mp.jar b <source-file.mpp> [<func_name>] [<dest-file.lt>]");
    System.out.println("  optimize:");
    System.out.println("    java -jar mp.jar o <opt nums N-N-...> <source-file.lt> <dest-file.lt>");
    System.out.println("  get statistics:");
    System.out.println("    java -jar mp.jar s <source-file.lt>");
    System.out.println("  view nodes:");
    System.out.println("    java -jar mp.jar view <source-file.lt>");
    System.exit(1);
  }

  private static void startCompile(String[] args, boolean andRun) {
    if ((!andRun && (args.length != 2) && (args.length != 3)) || (andRun && (args.length < 3))) {
      printUsage();
    }
    System.out.println("Compile:");

    String name1 = args[1];
    String name2 = (andRun || (args.length == 2)) ? args[1] + "p" : args[2];

    File f1 = new File(name1);
    File f2 = new File(name2);

    if (name1.contains(":") || !f1.exists() || !f1.isFile()) {
      System.out.println("Error: not exists or not a file: " + f1.getAbsolutePath());
      System.exit(2);
    }
    if (f2.exists() && !name2.startsWith(name1)) {
      System.out.println("Error: file exists: " + f2.getAbsolutePath());
      System.exit(2);
    }
    if (name2.contains(":") || f2.isDirectory()) {
      System.out.println("Error: not a file: " + f2.getAbsolutePath());
      System.exit(2);
    }

    if (f2.exists()) {
      f2.delete();
    }

    boolean done = (new Compiler()).go(f1, f2);
    if (!done) {
      System.exit(5);
    }

    System.out.println("done compiling");
  }

  private static void startRun(String[] args) {
    if (args.length < 3) {
      printUsage();
    }

    System.out.println("Run:");

    String fname = args[1];
    File f = new File(fname);
    File logFile = new File(fname + ".log");

    if (fname.contains(":") || !f.exists() || !f.isFile()) {
      System.out.println("Error: not exists or not a file: " + f.getAbsolutePath());
      System.exit(2);
    }

    NameStr nm = new NameStr(args[2], false);
    boolean haveName = ((nm.len1 != -1) && (nm.len2 != -1));
    if (haveName && (args.length < 4)) {
      printUsage();
    }

    Object o = FileUtils.readObject(f, null);

    if (o instanceof DefProg) {
      DefProg prog = (DefProg) o;
      if (prog == null) {
        System.exit(4);
      }

      String[] params = Arrays.copyOfRange(args, haveName ? 3 : 2, args.length);
      boolean done = Interpretator.run(prog, haveName ? args[2] : null, params, logFile);

      if (!done) {
        System.exit(5);
      }

      Interpretator.printResult();
    } else if (o instanceof LogicTree) {
      if (haveName) {
        System.out.println("Error: func name not relevant when running logic tree");
        System.exit(2);
      }

      LogicTree lt = (LogicTree) o;
      if (lt == null) {
        System.exit(4);
      }

      String[] params = Arrays.copyOfRange(args, 2, args.length);
      boolean done = LT_run.run(lt, params, logFile);

      if (!done) {
        System.exit(5);
      }

      LT_run.printResult();
    }
  }

  private static void startBuild(String[] args) {
    if ((args.length < 2) || (args.length > 4)) {
      printUsage();
    }
    System.out.println("Build:");

    String name1 = args[1];
    String funcName = null;
    String name2 = null;
    if (args.length > 2) {
      if (args[2].contains(":")) {
        funcName = args[2];
      } else {
        name2 = args[2];
      }
    }
    if (args.length > 3) {
      if (name2 == null) {
        name2 = args[3];
      } else {
        printUsage();
      }
    }
    if (name2 == null) {
      name2 = name1 + ".lt";
    }

    if ((funcName != null) && !funcName.contains(":")) {
      printUsage();
    }

    File f1 = new File(name1);
    File f2 = new File(name2);

    if (!f1.exists() || !f1.isFile()) {
      System.out.println("Error: not exists or not a file: " + f1.getAbsolutePath());
      System.exit(2);
    }
    if (f2.exists() && !name2.startsWith(name1)) {
      System.out.println("Error: file exists: " + f2.getAbsolutePath());
      System.exit(2);
    }
    if (f2.isDirectory()) {
      System.out.println("Error: not a file: " + f2.getAbsolutePath());
      System.exit(2);
    }

    if (f2.exists()) {
      f2.delete();
    }

    DefProg prog = (DefProg) FileUtils.readObject(f1, DefProg.class.getName());
    if (prog == null) {
      System.exit(4);
    }

    boolean done = Builder.run(prog, funcName, f2);

    if (!done) {
      System.exit(5);
    }

    Builder.printResult();
  }

  private static void startStats(String[] args) {
    if (args.length != 2) {
      printUsage();
    }
    System.out.println("Stats:");

    String fname = args[1];
    File f1 = new File(fname);
    File f2 = new File(fname + ".xls");

    if (!f1.exists() || !f1.isFile()) {
      System.out.println("Error: not exists or not a file: " + f1.getAbsolutePath());
      System.exit(2);
    }

    if (f2.exists()) {
      f2.delete();
    }

    LogicTree lt = (LogicTree) FileUtils.readObject(f1, LogicTree.class.getName());
    if (lt == null) {
      System.exit(4);
    }

    Stats stats = new Stats();

    stats.init(lt, null);
    stats.calc(f2);

    System.out.println("done");
  }

  private static void startOptimize(String[] args) {
    if (args.length != 4) {
      printUsage();
    }
    System.out.println("Optimize (and stats):");

    String optNumList = args[1];
    String name1 = args[2];
    String name2 = args[3];

    File f1 = new File(name1);
    File f2 = new File(name2);
    File f3 = new File(name2 + ".xls");

    if (!f1.exists() || !f1.isFile()) {
      System.out.println("Error: not exists or not a file: " + f1.getAbsolutePath());
      System.exit(2);
    }
    if (f2.exists() && !name2.startsWith(name1)) {
      System.out.println("Error: file exists: " + f2.getAbsolutePath());
      System.exit(2);
    }
    if (f2.isDirectory()) {
      System.out.println("Error: not a file: " + f2.getAbsolutePath());
      System.exit(2);
    }

    LogicTree lt = (LogicTree) FileUtils.readObject(f1, LogicTree.class.getName());
    if (lt == null) {
      System.exit(4);
    }

    String[] optNums = optNumList.split("-");

    LogicTree lt2 = lt;
    ArrayList<int[]> ss = new ArrayList();
    ss.add(lt.stats);
    long dt1 = System.currentTimeMillis();

    for (int i = 0; i < optNums.length; i++) {
      switch (optNums[i]) {
        case "1":
          lt2 = LogicTreeStore.condenseLT(lt2);
          ss.add(lt2.stats);
          break;

        case "2":
          lt2 = new Optimizer2().run(lt2);
          ss.add(lt2.stats);
          break;

        default:
          System.out.println("Error: invalid optimization number: " + optNumList);
          System.exit(2);
      }
    }

    dt1 = (System.currentTimeMillis() - dt1) / 1000;

    if (!lt2.toFile(f2, true)) {
      System.exit(2);
    }

    Stats stats = new Stats();

    stats.init(lt2, ss);
    stats.calc(f3);

    System.out.println("done");
    System.out.println("exec time: " + dt1 + " s");
    if (dt1 > 100) {
      System.out.println("exec time: " + (dt1 / 60) + " m " + (dt1 % 60) + " s");
    }
  }

  private static void startView(String[] args) {
    if (args.length != 2) {
      printUsage();
    }
    System.out.println("View:");

    String fname = args[1];
    File file = new File(fname);

    if (!file.exists() || !file.isFile()) {
      System.out.println("Error: not exists or not a file: " + file.getAbsolutePath());
      System.exit(2);
    }

    LogicTree lt = (LogicTree) FileUtils.readObject(file, LogicTree.class.getName());
    if (lt == null) {
      System.exit(4);
    }

    new Viewer().run(lt);

    System.out.println("done");
  }

  private static void startTest(String[] args) {
    if (args.length < 3) {
      printUsage();
    }
    System.out.println("Test:");

    String testNumList = args[1];
    String name1 = args[2];

    File f1 = new File(name1);

    if (!f1.exists() || !f1.isFile()) {
      System.out.println("Error: not exists or not a file: " + f1.getAbsolutePath());
      System.exit(2);
    }

    LogicTree lt = (LogicTree) FileUtils.readObject(f1, LogicTree.class.getName());
    if (lt == null) {
      System.exit(4);
    }

    String[] testNums = testNumList.split("-");
    for (int i = 0; i < testNums.length; i++) {
      System.out.println();
      System.out.println("Test " + testNums[i] + ":");
      switch (testNums[i]) {
        case "1":
          new Test1().run(lt);
          break;

        case "2":
          new Test2().run(lt);
          break;

        case "3":
          new Test3().run(lt);
          break;

        case "4":
          new Test4().run(lt);
          break;

        case "5":
          new Test5().run(lt);
          break;

        case "6":
          new Test6().run(lt, name1, Arrays.copyOfRange(args, 3, args.length));
          break;

        default:
          System.out.println("Error: invalid test number: " + testNums[i]);
          System.exit(2);
      }
    }

    System.out.println("done");
  }
}
