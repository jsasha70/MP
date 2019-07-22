package jsasha.mp.compiler_utils;

public class OutFmt {

  public int len = -1;
  public char typ = ' ';
  public boolean isErr = false;
  public String err = "";

  public OutFmt(String s) {
    int nn = s.length();
    int n = s.indexOf(":");
    if ((n <= 0) || (n != (nn - 2))) {
      isErr = true;
      if (n < 0) {
        err = "no colon";
      } else if (n == 0) {
        err = "no number before colon";
      } else if (n == (nn - 1)) {
        err = "no type after colon";
      } else if (n != (nn - 2)) {
        err = "after colon must be only one char";
      } else {
        err = "err";
      }
      return;
    }

    try {
      len = Integer.parseInt(s.substring(0, n));
    } catch (NumberFormatException e) {
      isErr = true;
      err = "not a number \"" + s.substring(0, n) + "\"";
      return;
    }
    if (len <= 0) {
      isErr = true;
      err = "number must be positive";
      return;
    }

    typ = s.charAt(nn - 1);
    if ((typ != 'd') && (typ != 'h') && (typ != 'b') && (typ != 'o')) {
      isErr = true;
      err = "invalid type - not \"d\", \"h\", \"b\" or \"o\"";
      return;
    }
  }
}
