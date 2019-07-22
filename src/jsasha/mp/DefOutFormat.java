package jsasha.mp;

import java.io.Serializable;

public class DefOutFormat implements Serializable {

  public static final long serialVersionUID = 1;
  public int len; // число бит
  public char typ; // тип представления (d, h, o, b)

  public DefOutFormat(int len, char typ) {
    this.len = len;
    this.typ = typ;
  }
}
