package jsasha.mp;

public class Token {

  public String s;
  public int line;
  public int pos;
  public int file;

  public Token(String s, int line, int pos, int fileNo) {
    this.s = s;
    this.line = line;
    this.pos = pos;
    this.file = fileNo;
  }

  @Override
  public String toString() {
    return "(" + line + "-" + pos + ") " + s;
  }
}
