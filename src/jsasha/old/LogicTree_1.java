package jsasha.old;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import jsasha.lt.LT_dataSrc;
import jsasha.lt.LT_hash;
import jsasha.lt.LogicTreeException;

public class LogicTree_1 implements Serializable {

  private int[] rels1; // левые ссылки из узлов списков
  private int[] rels2; // правые ссылки из узлов списков
  private int args; // число параметров (плюс к этому еще нулевой элемент)
  private int len;
  public int[] vars = null; // возвращаемые значения
  public static final long serialVersionUID = 1;
  private LT_hash hash = null;
  private LT_dataSrc dataReciever = null;
  public final int one;
  public final int[] stats = new int[12];
  public static final String[] statsName = {
    "rel1 == 0",
    "rel1 == one",
    "rel2 == one",
    "rel1 == rel2",
    "--x = x",
    "list term not 0",
    "x + x = x",
    "x + -x = 1",
    "same node found",
    "call to enlargeArrays()",
    "--x = x (concat)",
    "sort"
  };

  public LogicTree_1(int args, int initSize) {
    if (initSize < (args + 2)) {
      initSize = args + 2; // плюс 0 и 1
    }
    this.args = args;

    rels1 = new int[initSize];
    rels2 = new int[initSize];
    len = args + 1;
    Arrays.fill(rels1, 0, len, -1);
    Arrays.fill(rels2, 0, len, -1);

    one = createOne();
    createHash();
  }

  public LogicTree_1(int args) {
    this(args, 10000);
  }

  private int createOne() {
    int ret = len;
    rels1[ret] = 0;
    rels2[ret] = 0;
    len++;
    return ret;
  }

  private void createHash() {
    hash = new LT_hash(len, rels1, rels2, one);
  }

  public int getLen() {
    return len;
  }

  public int getArgs() {
    return args;
  }

  private void enlargeArrays() {
    // увеличиваем длину массива
    int newSize = rels1.length * 2;

    rels1 = Arrays.copyOf(rels1, newSize);
    rels2 = Arrays.copyOf(rels2, newSize);

    createHash();

    if (dataReciever != null) {
      dataAccess(dataReciever);
    }
  }

  public void trimArrays() {
    // уменьшаем длину массива
    if (rels1.length != len) {
      rels1 = Arrays.copyOf(rels1, len);
      rels2 = Arrays.copyOf(rels2, len);

      if (hash != null) {
        createHash();
      }
    }
  }

  public final int add(int rel1, int rel2) {
    return add(rel1, rel2, false);
  }

  private int add(int rel1, int rel2, boolean simple) {
    if ((rel1 < 0) || (rel1 >= len)) {
      throw new LogicTreeException("Invalid rel 1:" + rel1);
    }
    if ((rel2 < 0) || (rel2 >= len)) {
      throw new LogicTreeException("Invalid rel 2:" + rel2);
    }
    if (hash == null) {
      createHash();
    }

    if (rel1 == 0) {
      stats[0]++;
      return one;
    } else if (rel1 == one) {
      stats[1]++;
      return rel2;
    } else if (rel2 == one) {
      stats[2]++;
      return one;
    } else if (rel1 == rel2) {
      stats[3]++;
      return one;
    } else if (!simple && (rel2 > 0) && (rel2 <= args)) {
      stats[5]++;
      return add(rel1, add(add(rel2, 0), 0));
    } else if (!simple && ((rel2 == 0) || (rel2 > args))
            && (rel1 > args) && (rels2[rel1] == 0) && (rels1[rel1] > args)) {
      if (rel2 == 0) {
        // --x = x
        stats[4]++;
        return rels1[rel1];
      } else { // if (rel2 > args) {
        // --x = x (concat)
        int ret = addConcat(rel1, rel2);
        if (ret >= 0) {
          stats[10]++;
          return ret;
        }
      }
    } else if (!simple && (rel2 > one)) {
      // x + x = x
      int r2 = rel2;
      while (r2 > one) {
        if (rels1[r2] == rel1) {
          stats[6]++;
          return rel2;
        }
        r2 = rels2[r2];
      }

      // x + -x = 1
      int r1;
      if ((rel1 > one) && (rels2[rel1] == 0)) {
        r1 = rels1[rel1];
      } else {
        r1 = hash.findNode(rel1, 0, rels1, rels2);
      }
      if (r1 > 0) {
        r2 = rel2;
        while (r2 > one) {
          if (rels1[r2] == r1) {
            stats[7]++;
            return one;
          }
          r2 = rels2[r2];
        }
      }

      // сортировка
      if (rel1 < rels1[rel2]) {
        int ret = addSort(rel1, rel2);
        if (ret >= 0) {
          stats[11]++;
          return ret;
        }
      }
    }

    int found = hash.findNode(rel1, rel2, rels1, rels2);
    if (found >= 0) {
      stats[8]++;
      return found;
    }

    if (len >= rels1.length) {
      // вышли за границы, увеличиваем длину массива
      stats[9]++;
      enlargeArrays();
    }

    int idx = len;
    len++;

    rels1[idx] = rel1;
    rels2[idx] = rel2;

    hash.setNodeHash(idx, rel1, rel2);

    return idx;
  }

  private int addSort(int rel1, int rel2) {
    // сортировка
    // if (rel2 > one) {
    // if (rel1 < rels1[rel2]) {

    if (rel1 < 0) {
      return -1;
    }

    int r21 = rels1[rel2];
    int r22 = rels2[rel2];
    int last = rel2;

    // вставляем в середину списка
    ArrayList<Integer> ss = new ArrayList();
    // поиск места
    while (rel1 < r21) {
      ss.add(r21);
      last = r22;
      r21 = rels1[r22];
      r22 = rels2[r22];
    }
    last = add(rel1, last, true);
    while (ss.size() > 0) {
      last = add(ss.get(ss.size() - 1), last, true);
      ss.remove(ss.size() - 1);
    }
    return last;
  }

  private int addConcat(int rel1, int rel2) {
    // --x = x (concat)
    // if ((rel1 > args) && (rels2[rel1] == 0) && (rels1[rel1] > args)) {
    // if (rel2 > args) {

    ArrayList<Integer> lst = new ArrayList();

    int r2 = rel2;
    while (r2 > args) {
      lst.add(rels1[r2]);
      r2 = rels2[r2];
    }

    r2 = rels1[rel1];
    while (r2 > args) {
      lst.add(rels1[r2]);
      r2 = rels2[r2];
    }
    if (r2 != 0) {
      return -1;
    }

    int[] lst2 = new int[lst.size()];
    int i = 0;
    for (Integer n : lst) {
      lst2[i] = n;
      i++;
    }
    Arrays.sort(lst2);

    r2 = 0;
    for (i = 0; i < lst2.length; i++) {
      r2 = add(lst2[i], r2, true);
    }
    return r2;
  }

  public int getRel1(int node) {
    return rels1[node];
  }

  public int getRel2(int node) {
    return rels2[node];
  }

  public void clearServiceData() {
    hash = null;
    dataReciever = null;
  }

  public boolean toFile(File file) {
    LT_hash hash1 = hash;
    LT_dataSrc dataReciever1 = dataReciever;
//    boolean done = LogicTreeStore.writeLT(this, file); aaa; !!! ??? восстановить
    hash = hash1;
    dataReciever = dataReciever1;
    return false;// done;
  }

  public void dataAccess(LT_dataSrc s) {
    dataReciever = s;
    if (hash == null) {
      createHash();
    }
    hash.dataAccess(s);
    s.ltData(rels1, rels2, args, len, vars, one, stats);
  }

  public int[] arrayRels1() {
    return Arrays.copyOf(rels1, len);
  }

  public int[] arrayRels2() {
    return Arrays.copyOf(rels2, len);
  }
}
