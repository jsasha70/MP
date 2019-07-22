package jsasha.mp.compiler_utils;

public class NameStr {

  public String name = "";
  public String fullName = "";
  public int len1 = -1;
  public int len2 = -1;
  public boolean isIn = false;
  public boolean isOut = false;
  public boolean isName = true;
  public boolean isErr = false;
  public String err = "";

  public NameStr(String s, boolean stackOper) {
    fullName = s;

    if (stackOper) {
      if (fullName.startsWith(">")) {
        isIn = true;
        fullName = fullName.substring(1);
      }
      if (fullName.endsWith(">")) {
        isOut = true;
        fullName = fullName.substring(0, fullName.length() - 1);
      }
      if (!isIn && !isOut) {
        isErr = true;
        err = "no \">\"";
        return;
      }
    }

    int nn = fullName.length();
    int n1 = fullName.indexOf(":");
    n1 = (n1 == -1) ? nn : n1;
    if (n1 == 0) {
      isErr = true;
      err = "no name";
      return;
    }
    name = fullName.substring(0, n1);

    // проверяем полученное имя
    char[] cc = name.toCharArray();
    if (!Character.isLetter(cc[0])) {
      if (stackOper) {
        isName = false;
      } else {
        isErr = true;
        err = "illegal start of name \"" + cc[0] + "\"";
        return;
      }
    }
    for (int i = 1; i < cc.length; i++) {
      if (!Character.isLetter(cc[i]) && !Character.isDigit(cc[i]) && (cc[i] != '_')) {
        if (stackOper) {
          isName = false;
        } else {
          isErr = true;
          err = "illegal char \"" + cc[i] + "\" in name";
          return;
        }
      }
    }

    if (n1 >= nn) {
      return;
    }
    n1++;
    if (n1 >= nn) {
      isErr = true;
      err = "no number after colon";
      return;
    }

    // получаем первую длину
    int n2 = fullName.indexOf(":", n1);
    n2 = (n2 == -1) ? nn : n2;
    try {
      len1 = Integer.parseInt(fullName.substring(n1, n2));
    } catch (NumberFormatException e) {
      isErr = true;
      err = "not a number \"" + fullName.substring(n1, n2) + "\"";
      return;
    }
    if (len1 < 0) {
      isErr = true;
      err = "length \"" + len1 + "\" can not be negative";
      return;
    }

    if (n2 >= nn) {
      return;
    }
    n2++;
    if (n2 >= nn) {
      isErr = true;
      err = "no number after colon";
      return;
    }

    // получаем вторую длину
    try {
      len2 = Integer.parseInt(fullName.substring(n2));
    } catch (NumberFormatException e) {
      isErr = true;
      err = "not a number \"" + fullName.substring(n2) + "\"";
      return;
    }
    if (len2 < 1) {
      isErr = true;
      err = "length \"" + len2 + "\" must be positive";
      return;
    }

    if (isName) {
      checkName();
    }
  }

  private void checkName() {
    switch (name) {
      case "func":
      case "if":
      case "else":
      case "loop":
      case "def":
      case "exit":
      case "return":
        isErr = true;
        err = "keyword \"" + name + "\" can not be used as name";
        return;
    }
  }
}
