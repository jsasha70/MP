package jsasha.mp;

import java.io.File;
import java.util.ArrayList;
import jsasha.util.FileUtils;

public class Parcer {

  private ArrayList<Token> lst = new ArrayList(1000);
  private String[] lines;
  private int fileNo;
  private ArrayList<String> files;

  public Parcer(String fname, String[] lines, ArrayList<String> files) {
    this.lines = lines;
    this.files = files;
    this.fileNo = files.size();
    this.files.add(fname);
  }

  public Parcer(String fname, ArrayList<String> lines, ArrayList<String> files) {
    this(fname, lines.toArray(new String[0]), files);
  }

  public boolean go() {
    String s;
    boolean done;
    for (int i = 0; i < lines.length; i++) {
      s = lines[i].replaceAll("\t", " ");

      if (s.isEmpty()) {
        continue;
      }

      done = handleLine(s, i + 1);
      if (!done) {
        return false;
      }
    }

    return true;
  }

  private boolean handleLine(String s, int lineNo) {
    char[] cc = s.toCharArray();
    if (cc.length != s.length()) { // проверка на всякий случай
      System.out.println("Error: parcer: internal length error");
      return false;
    }

    int p1 = 0, p2;
    String incl = "#include ";
    boolean handled;

    while (p1 < cc.length) {
      if (cc[p1] == ' ') {
        p1++;
        continue;
      }
      p2 = 0;
      handled = false;

      switch (cc[p1]) {
        case '.':
          p2 = p1 + 1;
          break;

        case '{':
          p2 = p1 + 1;
          break;

        case '}':
          p2 = p1 + 1;
          break;

        case '#':
          if (s.startsWith(incl, p1)) {
            p2 = p1 + incl.length();
            handled = true;
            if (!handleInclude(s.substring(p2))) {
              return false;
            }
            p2 = s.length();
          }

        case '/':
          if (s.startsWith("//", p1)) {
            p2 = cc.length; // коментарий - до конца строки
          }
          break;
      }

      if (p2 == 0) { // общий поиск конца токена
        p2 = p1 + 1;
        while ((p2 < cc.length) && (cc[p2] != ' ') && (cc[p2] != '.') && (cc[p2] != '{') && (cc[p2] != '}')) {
          p2++;
        }
      }

      if (!handled) {
        lst.add(new Token(s.substring(p1, p2), lineNo, p1 + 1, fileNo)); // создаем токен
      }

      p1 = p2; // переходим к следующему
    }

    return true;
  }

  private boolean handleInclude(String s) {
    s = s.trim();

    if (s.isEmpty()) {
      System.out.println("Error: parcer (include): file not specified");
      return false;
    }

    File f = new File(s);
    if (!f.exists() || !f.isFile()) {
      System.out.println("Error: parcer (include): not exists or not a file: " + f.getAbsolutePath());
      return false;
    }

    String[] ss = FileUtils.readLines(f);
    if (ss == null) {
      return false;
    }

    Parcer prc2 = new Parcer(f.getAbsolutePath(), ss, files);
    boolean done = prc2.go();

    if (done) {
      lst.addAll(prc2.getTokens());
    }

    return done;
  }

  public ArrayList<Token> getTokens() {
    return lst;
  }

  public String[] getFiles() {
    return files.toArray(new String[0]);
  }
}
