package jsasha.lt;

public class LT_hash {

  private int bits; // размерность хеша в битах (0 - отключен)
  private int mask;
  private int[] index = null; // индекс (ссылка на последний узел с данным хешем)
  private int[] chainLen = null; // число узлов с данным хешем
  private int[] prev = null; // цепочка узлов с одним хешем
  private int one;

  public LT_hash(int len, int[] rels1, int[] rels2, int one) {
    this.one = one;
    calcBits(rels1.length);
    createHash(rels1, rels2, len);
  }

  private int nodeHash(int rel1, int rel2) {
    // рассчет хеша для узла (пары ссылок); метод: сумма младших битов
    return ((rel1 & mask) + (rel2 & mask)) & mask;
  }

  public void setNodeHash(int node, int rel1, int rel2) {
    // установка хеша для узла
    int hash = nodeHash(rel1, rel2);

    prev[node] = index[hash];
    index[hash] = node;
    chainLen[hash]++;
  }

  public int findNode(int rel1, int rel2, int[] rels1, int[] rels2) {
    // поиск узла с заданными ветками (адресами на траке)

    if ((rel1 == -1) || (rel2 == -1)) {
      return 0;
    }
    if ((rel1 == 0) && (rel2 == 0)) {
      return one;  // единица
    }

//    int max_no = rel1;
//    if (max_no < rel2) {
//      max_no = rel2;
//    }

    // используем хеш для поиска
    int hash = nodeHash(rel1, rel2);
    int prevNode = index[hash];

//    while (prevNode > max_no) { // ???
    while (prevNode >= one) {
      if ((rels1[prevNode] == rel1) && (rels2[prevNode] == rel2)) {
        return prevNode;
      }
      prevNode = prev[prevNode];
    }

    return -1;
  }

  private void calcBits(int size) {
    if (size < 1024) {
      bits = 10;
    } else {
      int bt = size;
      while (bt > 3) {
        bt = bt >>> 1;
        bits++;
      }
      if (bt == 3) {
        bits += 2;
      } else {
        bits++;
      }
    }

    mask = (1 << bits) - 1;
  }

  private void createHash(int[] rels1, int[] rels2, int len) {
    if ((bits < 10) || (bits > 24)) {
      throw new LogicTreeException("Invalid hash dimension: bits=" + bits);
    }

    prev = new int[rels1.length];
    index = new int[1 << bits];
    chainLen = new int[index.length];

    for (int i = 0; i < index.length; i++) {
      index[i] = -1;
      chainLen[i] = 0;
    }

    for (int i = 0; i < len; i++) {
      if (rels1[i] == -1) {
        prev[i] = -1;
      } else {
        setNodeHash(i, rels1[i], rels2[i]);
      }
    }
  }

  public void dataAccess(LT_dataSrc s) {
    s.hashData(index, chainLen, prev);
  }
}
