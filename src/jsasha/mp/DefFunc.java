package jsasha.mp;

import java.io.Serializable;

public class DefFunc implements Serializable {

  public static final long serialVersionUID = 1;
  public String name;
  public String shortName;
  public int id;
  public int argLen;
  public int retLen;
  public boolean log = false;
  public DefOutFormat[] retFmt;
  public DefBlock[] blocks;
}
