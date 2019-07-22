package jsasha.mp;

public class ErrDescr {

  private String err;
  private Token tok = null;
  private DefInstr ii = null;

  public ErrDescr(String err, Token tok) {
    this.err = err;
    this.tok = tok;
  }

  public ErrDescr(String err, DefInstr ii) {
    this.err = err;
    this.ii = ii;
  }

  public String getErr() {
    return err;
  }

  public int getFile() {
    return tok == null ? ii.file : tok.file;
  }

  public int getLine() {
    return tok == null ? ii.line : tok.line;
  }

  public int getPos() {
    return tok == null ? ii.pos : tok.pos;
  }
}
