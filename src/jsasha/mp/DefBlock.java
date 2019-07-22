package jsasha.mp;

import java.io.Serializable;

public class DefBlock implements Serializable {

  public static final long serialVersionUID = 1;
  public int id;
  public int parentId;
  public char typ; // тип блока (b - code, i - if, s - else, l - loop); первый блок может быть только типа "c"
  public int stackChange;
  public DefVar[] vars;
  public DefInstr[] ii;
}
