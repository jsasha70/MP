package jsasha.lt;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import jsasha.util.ListInt;

public class LogicTree implements Serializable {

  private int[] rels1; // левые ссылки из узлов списков
  private int[] rels2; // правые ссылки из узлов списков
  private int args; // число параметров (плюс к этому еще нулевой элемент)
  private int len;
  public int[] vars = null; // возвращаемые значения
  public static final long serialVersionUID = 1;
  private LT_hash hash = null;
  private LT_dataSrc dataReciever = null;
  public final int one;
  public final int[] stats = new int[13];
  public static final String[] statsName = {
    "rel1 == 0",
    "rel1 == one",
    "rel2 == one",
    "rel1 == rel2",
    "--x = x",
    "--x = x (arg)",
    "--x = x (concat)",
    "list term not 0",
    "x + x = x",
    "x + -x = 1",
    "same node found",
    "call to enlargeArrays()",
    "sort"
  };

  public LogicTree(int args, int initSize) {
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

  public LogicTree(int args) {
    this(args, 10000);
  }

  public LogicTree(LogicTree lt, boolean trim) {
    this(lt.args, trim ? lt.len : lt.rels1.length);
    len = lt.len;

    if (lt.vars != null) {
      vars = Arrays.copyOf(lt.vars, lt.vars.length);
    } else {
      vars = new int[0];
    }

    System.arraycopy(lt.rels1, 0, rels1, 0, rels1.length);
    System.arraycopy(lt.rels2, 0, rels2, 0, rels1.length);
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

  public int add(int rel1, int rel2) {
    int node = addNoSort(rel1, rel2);
    node = sort(node);
    return node;
  }

  public int addNoSort(int rel1, int rel2) {
    if ((rel1 < 0) || (rel1 >= len)) {
      throw new LogicTreeException("Invalid rel 1:" + rel1);
    }
    if ((rel2 < 0) || (rel2 >= len)) {
      throw new LogicTreeException("Invalid rel 2:" + rel2);
    }
    if (hash == null) {
      createHash();
    }

    if (rel1 == one) {
      stats[1]++;
      if (rel2 > one) {
        rel1 = rels1[rel2];
        rel2 = rels2[rel2];
      } else {
        return rel2;
      }
    }

    if (rel1 == 0) {
      stats[0]++;
      return one;
    } else if (rel2 == one) {
      stats[2]++;
      return one;
    } else if (rel1 == rel2) {
      stats[3]++;
      return one;
    }

    if ((rel2 > 0) && (rel2 <= args)) {
      stats[7]++;
      rel2 = addRule(addRule(rel2, 0), 0);
    }

//    if (((rel2 == 0) || (rel2 > args))
//            && (rel1 > args) && (rels2[rel1] == 0) && (rels1[rel1] > args)) {
    if ((rel1 > args) && (rels2[rel1] == 0)) {
      if (rels1[rel1] > args) {
        if (rels2[rels1[rel1]] == 0) {
          // --x = x (arg)
          stats[5]++;
          rel1 = rels1[rels1[rel1]];
        } else if (rel2 == 0) {
          // --x = x
          stats[4]++;
          return rels1[rel1];
        } else { // (rel2 > args)
          // --x = x (concat)
          int ret = addConcat(rel1, rel2);
          if (ret >= 0) {
            stats[6]++;
            return ret;
          }
        }
      }
    }

    if (rel2 > one) {
      // x + x = x
      int r2 = rel2;
      while (r2 > one) {
        if (rels1[r2] == rel1) {
          stats[8]++;
          return rel2;
        }
        r2 = rels2[r2];
      }
    }

    if (rel2 > one) {
      // x + -x = 1
      int r1, r2;
      if (rels2[rel1] == 0) {
        r1 = rels1[rel1];
      } else {
        r1 = hash.findNode(rel1, 0, rels1, rels2);
      }
      if (r1 > 0) {
        r2 = rel2;
        while (r2 > one) {
          if (rels1[r2] == r1) {
            stats[9]++;
            return one;
          }
          r2 = rels2[r2];
        }
      }
    }

//    if ((rel1 > one) && (rel2 > one)) { aaa;
//      // ax + x = x
//      int ret = addSublist(rel1, rel2);
//      if (ret > one) {
//        return ret;
////        if (ret == rel2) {
////          return rel2;
////        } else {
////          rel1 = rels1[ret];
////          rel2 = rels2[ret];
////        }
//      }
//    }

    return addRule(rel1, rel2);
  }

  public int addRule(int rel1, int rel2) {
    int found = hash.findNode(rel1, rel2, rels1, rels2);
    if (found >= 0) {
      stats[10]++;
      return found;
    }

    if (len >= rels1.length) {
      // вышли за границы, увеличиваем длину массива
      stats[11]++;
      enlargeArrays();
    }

    int idx = len;
    len++;

    rels1[idx] = rel1;
    rels2[idx] = rel2;

    hash.setNodeHash(idx, rel1, rel2);

    if (dataReciever != null) {
      dataReciever.addData(idx, rel1, rel2, len);
    }

    return idx;
  }

  public int sort(int node) {
    int rel1 = rels1[node];
    int rel2 = rels2[node];

    // сортировка
    if ((rel2 > one) && (rel1 < rels1[rel2])) {
      int ret = addSort(rel1, rel2);
      if (ret >= 0) {
        stats[12]++;
        return ret;
      }
    }

    return node;
  }

  private int addSort(int rel1, int rel2) {
    // сортировка
    // if (rel2 > one) {
    // if (rel1 < rels1[rel2]) {

    if (rel1 <= 0) {
      return -1;
    }

    int r21 = rels1[rel2];
    int r22 = rels2[rel2];
    int last = rel2;

    // вставляем в середину списка
    ListInt ss = new ListInt(5);
    // поиск места
    while (rel1 < r21) {
      ss.add(r21);
      last = r22;
      r21 = rels1[r22];
      r22 = rels2[r22];
    }
    last = add(rel1, last); // addRule ???
    while (ss.getLen() > 0) {
      last = add(ss.pop(), last); // addRule ???
    }
    return last;
  }
//  private int addSort(int rel1, int rel2) {
//    // сортировка
//    // if (rel2 > one) {
//    // if (rel1 < rels1[rel2]) {
//
//    if (rel1 < 0) {
//      return -1;
//    }
//
//    int r21 = rels1[rel2];
//    int r22 = rels2[rel2];
//    int last = rel2;
//
//    // вставляем в середину списка
//    ArrayList<Integer> ss = new ArrayList();
//    // поиск места
//    while (rel1 < r21) {
//      ss.add(r21);
//      last = r22;
//      r21 = rels1[r22];
//      r22 = rels2[r22];
//    }
//    last = addRule(rel1, last);
//    while (ss.size() > 0) {
//      last = addRule(ss.get(ss.size() - 1), last);
//      ss.remove(ss.size() - 1);
//    }
//    return last;
//  }

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
      r2 = add(lst2[i], r2);
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

  public boolean toTempFile() {
    LogicTree lt2 = new LogicTree(this, true);
    boolean ret = lt2.toFile(new File("temp.lt"), false);
    return ret;
  }

  public boolean toFile(File file, boolean doCondense) {
    LT_hash hash1 = hash;
    LT_dataSrc dataReciever1 = dataReciever;
    boolean done = LogicTreeStore.writeLT(this, file, doCondense);
    hash = hash1;
    dataReciever = dataReciever1;
    return done;
  }

  public void dataAccess(LT_dataSrc s) {
    dataReciever = s;
    if (hash == null) {
      createHash();
    }
    hash.dataAccess(s);
    dataReciever.ltData(rels1, rels2, args, len, vars, one, stats);
  }

  public int[] arrayRels1() {
    return Arrays.copyOf(rels1, len);
  }

  public int[] arrayRels2() {
    return Arrays.copyOf(rels2, len);
  }

  private int addSublist(int rel1, int rel2) {
    // ax + x = x

    boolean ul1, ul2;
    int r1, r2, rr11, rr12, rr21, rr22;

    // перебираем и сравниваем элементы списка
    r2 = rel2;
    while (r2 > one) {
      r1 = rels1[r2];
      if (r1 > one) {
        // сравниваем списки rel1 и r1
        ul1 = true;
        ul2 = true;
        rr12 = rel1;
        rr22 = r1;
        while ((rr12 > one) && (rr22 > one)) {
          rr11 = rels1[rr12];
          rr21 = rels1[rr22];
          if (rr11 > rr21) {
            ul2 = false;
            while (rr11 > rr21) {
              rr12 = rels2[rr12];
              rr11 = rels1[rr12];
            }
            if (rr11 != rr21) {
              ul1 = false;
            }
          } else if (rr11 < rr21) {
            ul1 = false;
            while (rr11 < rr21) {
              rr22 = rels2[rr22];
              rr21 = rels1[rr22];
            }
            if (rr11 != rr21) {
              ul2 = false;
            }
          }

          if (!ul1 && !ul2) {
            break;
          }

          rr12 = rels2[rr12];
          rr22 = rels2[rr22];
        }

        ul1 = ul1 && (rr22 == 0);
        ul2 = ul2 && (rr12 == 0);

        if (ul1) {
          return rel2;
        }

        if (ul2) {
          ListInt b = new ListInt(10);
          rr12 = rel2;
          while (rr12 > r2) {
            rr11 = rels1[rr12];
            b.add(rr11);
            rr12 = rels2[rr12];
          }

          r2 = rels2[r2];
          while (b.getLen() > 0) {
            r1 = b.pop();
            r2 = addRule(r1, r2);
          }

          return addRule(r1, r2);
        }
      }

      r2 = rels2[r2];
    }

    return -1;
  }

  public int xMinMin(int node) {
    // --x = x (для аргументов)
    if ((node > args) && (rels2[node] == 0) && (rels1[node] > args) && (rels2[rels1[node]] == 0)) {
      return rels1[rels1[node]];
    } else {
      return node;
    }
  }
//  public void setLen(int newLen) {
//    len = newLen;
//  }
//
//  public void setNode(int node, int rel1, int rel2) {
//    rels1[node] = rel1;
//    rels2[node] = rel2;
//  }
}
