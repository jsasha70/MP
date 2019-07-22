package jsasha.old.ramsql;

import java.io.*;

public class Message {

  private int[] aRel1; // левые ссылки из узлов списков
  private int[] aRel2; // правые ссылки из узлов списков
  private int len;
  public int delta = 100; // такими порциями приращается длина трака (в узлах)
  private long findCount = 0;
  private long findCountSteps = 0;
  private int[] symbols;
  private int[] lastUsed1; // последнее использование узла в левой ссылке
  private int[] lastUsed2; // последнее использование узла в правой ссылке
  private int[] prevUsed1; // предыдущие использования узла в левой ссылке
  private int[] prevUsed2; // предыдущие использования узла в правой ссылке
  // работа хеша:
  private int heshBits = 0; // размерность хеша в битах (0 - отключен)
  private int heshType = 0; // тип хеша: 1-простой, 2-со сдвигом, 3-сумма
  private int[] heshIndex; // индекс (ссылка на последний узел с данным хешем)
  private int[] heshChainLen; // число узлов с данным хешем
  private int[] heshPrev; // цепочка узлов с одним хешем
  // сопоставление с другим траком (реальность):
  private int[] rltRef = null; // соответствующие номера узлов другого трака
//  private Message rltMsg = null; // ссылка на трак, с которым сделано сопоставление
  // отметки узлов (для массовой нерекурсивной обработки)
  private byte[] marks = null;
  private int marksLen = 0;
  // длина списков для узлов трака
  private int[] listLen = null;

  public Message(int max_len) {
    if (max_len < 1) {
      throw new SimpleListException("Invalid max len:" + max_len);
    }

    aRel1 = new int[max_len + 1];
    aRel2 = new int[max_len + 1];
    lastUsed1 = new int[max_len + 1];
    lastUsed2 = new int[max_len + 1];
    prevUsed1 = new int[max_len + 1];
    prevUsed2 = new int[max_len + 1];
    aRel1[0] = -1;
    aRel2[0] = -1;
    lastUsed1[0] = -1;
    lastUsed2[0] = -1;
    prevUsed1[0] = -1;
    prevUsed2[0] = -1;
    len = 0;

    symbols = new int[15];
    for (int i = 0; i < symbols.length; i++) {
      symbols[i] = 0;
    }
  }

  public Message() {
    this(100);
  }

  public void loadData(int[] a_rel1, int[] a_rel2, int a_len, int a_size) {
    int l_len = a_len > 0 ? a_len : (a_rel1.length - 1);
    int l_size = a_size > 0 ? a_size : (a_len + 1);

    if ((l_len + 1) > l_size) {
      throw new SimpleListException("Length " + l_len + "+1 must not be greater then size " + l_size);
    }

    if ((l_len + 1) > a_rel1.length) {
      throw new SimpleListException("Length " + l_len + "+1 must not be greater then array a_rel1 size " + a_rel1.length);
    }

    if ((l_len + 1) > a_rel2.length) {
      throw new SimpleListException("Length " + l_len + "+1 must not be greater then array a_rel2 size " + a_rel2.length);
    }

    len = l_len;
    if (aRel1.length < a_size) {
      aRel1 = new int[a_size];
      aRel2 = new int[a_size];
    }
    for (int i = 1; i <= len; i++) {
      aRel1[i] = a_rel1[i];
      aRel2[i] = a_rel2[i];
    }
    aRel1[0] = -1;
    aRel2[0] = -1;

    for (int i = 0; i < symbols.length; i++) {
      symbols[i] = 0;
    }

    rltRef = null;
//    rltMsg = null;
    marks = null;
    marksLen = 0;
    listLen = null;

    findCountClear();
    nodeUsageCalcAll();
    optimizeParams();
  }

  public int getArSize() {
    return aRel1.length;
  }

  public int getLen() {
    return len;
  }

  public int addNode(int rel1, int rel2) {
    if ((rel1 < 0) || (rel1 > len)) {
      throw new SimpleListException("Invalid rel 1:" + rel1);
    }
    if ((rel2 < 0) || (rel2 > len)) {
      throw new SimpleListException("Invalid rel 2:" + rel2);
    }

    int found = findNode(rel1, rel2);
    if (found > 0) {
      return found;
    }

    len++;

    if (len >= aRel1.length) {
      // вышли за границы, увеличиваем длину массива
      if (delta < 1) {
        delta = 1;
      }
      int[] tmp = new int[aRel1.length + delta];
      for (int i = 0; i < aRel1.length; i++) {
        tmp[i] = aRel1[i];
      }
      aRel1 = tmp;

      tmp = new int[aRel1.length];
      for (int i = 0; i < aRel2.length; i++) {
        tmp[i] = aRel2[i];
      }
      aRel2 = tmp;

      if (heshBits > 0) {
        tmp = new int[aRel1.length];
        for (int i = 0; i < heshPrev.length; i++) {
          tmp[i] = heshPrev[i];
        }
        heshPrev = tmp;
      }

      tmp = new int[aRel1.length];
      for (int i = 0; i < lastUsed1.length; i++) {
        tmp[i] = lastUsed1[i];
      }
      lastUsed1 = tmp;

      tmp = new int[aRel1.length];
      for (int i = 0; i < lastUsed2.length; i++) {
        tmp[i] = lastUsed2[i];
      }
      lastUsed2 = tmp;

      tmp = new int[aRel1.length];
      for (int i = 0; i < prevUsed1.length; i++) {
        tmp[i] = prevUsed1[i];
      }
      prevUsed1 = tmp;

      tmp = new int[aRel1.length];
      for (int i = 0; i < prevUsed2.length; i++) {
        tmp[i] = prevUsed2[i];
      }
      prevUsed2 = tmp;

      if (rltRef != null) {
        tmp = new int[aRel1.length];
        for (int i = 0; i < rltRef.length; i++) {
          tmp[i] = rltRef[i];
        }
        rltRef = tmp;
      }

      if (listLen != null) {
        tmp = new int[aRel1.length];
        for (int i = 0; i < listLen.length; i++) {
          tmp[i] = listLen[i];
        }
        listLen = tmp;
      }
    }

    aRel1[len] = rel1;
    aRel2[len] = rel2;
    if (rltRef != null) {
//      if (((rel1 == 0) || (rltRef[rel1] != 0)) && ((rel2 == 0) || (rltRef[rel2] != 0))) {
//        rltRef[len] = rltMsg.findNode(rltRef[rel1], rltRef[rel2]);
//      } else {
      rltRef[len] = 0;
//      }
    }

    if (listLen != null) {
      listLen[len] = listLen[rel2] + 1;
    }

    heshSetForNode(len);
    nodeUsageCalc(len);

    return len;
  }

  private void nodeUsageCalcAll() {
    // рассчет использования ссылок для всех узлов (например, после загрузки массива узлов из файла)
    // длина массива узлов len д.б. уже установлена

    lastUsed1 = utilCreArray(lastUsed1, aRel1.length);
    lastUsed2 = utilCreArray(lastUsed2, aRel1.length);
    prevUsed1 = utilCreArray(prevUsed1, aRel1.length);
    prevUsed2 = utilCreArray(prevUsed2, aRel1.length);
    lastUsed1[0] = -1;
    lastUsed2[0] = -1;
    prevUsed1[0] = -1;
    prevUsed2[0] = -1;

    for (int i = 1; i <= len; i++) {
      nodeUsageCalc(i);
    }
  }

  private void nodeUsageCalc(int node) {
    // учет использования ссылок на узел

    int rel1 = aRel1[node];
    int rel2 = aRel2[node];

    if (rel1 != 0) {
      prevUsed1[node] = lastUsed1[rel1];
      lastUsed1[rel1] = node;
      lastUsed1[node] = 0;
    }

    if (rel2 != 0) {
      prevUsed2[node] = lastUsed2[rel2];
      lastUsed2[rel2] = node;
      lastUsed2[node] = 0;
    }
  }

  public int heshCalc(int rel1, int rel2) {
    // рассчет хеша для узла (пары ссылок)
    // метод: XOR младших битов

    if (heshBits == 0) {
      return 0;
    }

    int mask = (1 << heshBits) - 1;
    int ret;

    switch (heshType) {
      case 1:
        ret = (rel1 & mask) ^ (rel2 & mask);
        break;
      case 2:
        int shift = heshBits / 2;
        int mask2 = (1 << shift) - 1;
        ret = (rel1 & mask) ^ ((rel2 << shift) & mask) ^ ((rel2 >>> (heshBits - shift)) & mask2);
        break;
      case 3:
        ret = ((rel1 & mask) + (rel2 & mask)) & mask;
        break;
      default:
        throw new SimpleListException("Invalid hesh type: " + heshType);
    }

    return ret;
  }

  private void heshSetForNode(int node) {
    // установка хеша для узла (узлы должны перебираться последовательно по возрастанию)
    // хеш-индекс должен быть предварительно обнулен

    if (heshBits == 0) {
      return;
    }

    int hesh = heshCalc(aRel1[node], aRel2[node]);

    heshPrev[node] = heshIndex[hesh];
    heshIndex[hesh] = node;
    heshChainLen[hesh]++;
  }

  public void heshSet(int bits, int type) {
    // установка хеша (с полным пересчетом)

    if ((bits < 0) || (bits == 1) || (bits > 24)) {
      throw new SimpleListException("Invalid hesh dimension: bits=" + bits);
    }

    heshBits = bits;
    heshType = type;

    if (bits == 0) {
      heshIndex = null;
      heshChainLen = null;
      heshPrev = null;
      heshType = 0;
    } else {
      heshPrev = utilCreArray(heshPrev, aRel1.length);
      heshIndex = utilCreArray(heshIndex, 1 << bits);
      heshChainLen = utilCreArray(heshChainLen, heshIndex.length);

      for (int i = 0; i < heshIndex.length; i++) {
        heshIndex[i] = 0;
        heshChainLen[i] = 0;
      }

      for (int i = 1; i <= len; i++) {
        heshSetForNode(i);
      }
    }
  }

  private int[] utilCreArray(int[] ar, int size) {
    if (size < 1) {
      return null;
    }

    if ((ar == null) || (ar.length != size)) {
      return new int[size];
    } else {
      return ar;
    }
  }

  public int get_rel1(int node) {
    return aRel1[node];
  }

  public int get_rel2(int node) {
    return aRel2[node];
  }

  public int findNode(int rel1, int rel2) {
    // поиск узла с заданными ветками (адресами на траке)

    if ((rel1 == 0) && (rel2 == 0) && (len > 0)) {
      return 1;
    }

    findCount++;

    int max_no = rel1;
    if ((max_no == 0) || (max_no < rel2)) {
      max_no = rel2;
    }

    if (heshBits == 0) {
      // поиск без хеша
      for (int i = max_no + 1; i <= len; i++) {
        findCountSteps++;
        if ((aRel1[i] == rel1) && (aRel2[i] == rel2)) {
          return i;
        }
      }
    } else {
      // поиск с хешем

      int hesh = heshCalc(rel1, rel2);
      int prevNode = heshIndex[hesh];

      while (prevNode > max_no) {
        findCountSteps++;
        if ((aRel1[prevNode] == rel1) && (aRel2[prevNode] == rel2)) {
          return prevNode;
        }
        prevNode = heshPrev[prevNode];
      }
    }

    return 0;
  }

  public void findCountClear() {
    findCount = 0;
    findCountSteps = 0;
  }

  public long findCountGet() {
    return findCount;
  }

  public long findCountGetSteps() {
    return findCountSteps;
  }

  public int findSimpleList(SimpleList lst) {
    // поиск на траке списка
    if (lst != null) {
      lst.ClearNote();
    }
    return findSimpleList2(lst);
  }

  private int findSimpleList2(SimpleList lst) {
    // поиск на траке списка

    if (lst == null) {
      return 0;
    }

    if (lst.note > 0) {
      return lst.note;
    }

    int rel1 = 0;
    int rel2 = 0;

    SimpleList lst1 = lst.getRel1();
    SimpleList lst2 = lst.getRel2();

    if (lst1 != null) {
      rel1 = findSimpleList2(lst1);
      if (rel1 == 0) {
        return 0;
      }
    }
    if (lst2 != null) {
      rel2 = findSimpleList2(lst2);
      if (rel2 == 0) {
        return 0;
      }
    }

    int ret = findNode(rel1, rel2);
    lst.note = ret;
    return ret;
  }

  public int addSimpleList(SimpleList lst) {
    // добавление на трак списка
    if (lst != null) {
      lst.ClearNote();
    }
    return addSimpleList2(lst);
  }

  private int addSimpleList2(SimpleList lst) {
    // добавление на трак списка

    if (lst == null) {
      return 0;
    }

    int found = findSimpleList2(lst);
    if (found > 0) {
      return found;
    }

    // решили, что сначала всегда добавляем левую ветку (важно для мини-трака)
    int rel1 = addSimpleList2(lst.getRel1());
    int rel2 = addSimpleList2(lst.getRel2());

    return addNode(rel1, rel2);
  }

  public int old_cmpNodes(int node1, int node2) {
    // сравнение списков (узлов) node1 и node2:
    //    1 - node1 больше чем node2
    //    0 - списки равны
    //   -1 - node1 меньше чем node2

    int r1 = aRel1[node1];
    int r2 = aRel1[node2];

    if ((r1 == 0) && (r2 != 0)) {
      return -1;
    } else if ((r1 != 0) && (r2 == 0)) {
      return 1;
    } else if ((r1 != 0) && (r2 != 0) && (r1 != r2)) {
      int cmp1 = old_cmpNodes(r1, r2);
      if (cmp1 != 0) {
        return cmp1;
      }
    }

    // левые половинки списка равны, сравниваем правые
    r1 = aRel2[node1];
    r2 = aRel2[node2];

    if ((r1 == 0) && (r2 != 0)) {
      return -1;
    } else if ((r1 != 0) && (r2 == 0)) {
      return 1;
    } else if ((r1 != 0) && (r2 != 0) && (r1 != r2)) {
      return old_cmpNodes(r1, r2);
    } else {
      return 0;
    }
  }

  public String getBitString(int node, boolean doCondense) {
    if (doCondense) {
      String str = getBitString2(node);
      char[] c = str.toCharArray();
      int n = 0;
      for (int i = c.length - 1; i > 0; i--) {
        if (c[i] == '0') {
          n++;
        } else {
          break;
        }
      }

      if (n == 0) {
        return str;
      } else {
        return new String(c, 0, c.length - n);
      }
    } else {
      return getBitString2(node);
    }
  }

  private String getBitString2(int node) {
    if ((node < 1) || (node > len)) {
      throw new SimpleListException("No such node:" + node);
    }

    String ret;
    int rel1 = aRel1[node];
    int rel2 = aRel2[node];

    if (rel1 == 0) {
      ret = "0";
    } else {
      ret = "1" + getBitString2(rel1);
    }

    if (rel2 == 0) {
      ret = ret + "0";
    } else {
      ret = ret + "1" + getBitString2(rel2);
    }

    return ret;
  }

  public SimpleList getList(int node) {
    // получение списка по адресу на траке

    if ((node < 1) || (node > len)) {
      throw new SimpleListException("Invalid node:" + node);
    }

    SimpleList lst1;
    SimpleList lst2;

    int rel1 = aRel1[node];
    int rel2 = aRel2[node];

    if (rel1 == 0) {
      lst1 = null;
    } else {
      lst1 = getList(rel1);
    }

    if (rel2 == 0) {
      lst2 = null;
    } else {
      lst2 = getList(rel2);
    }

    return new SimpleList(lst1, lst2);
  }

  public String getListString(int node) {
    if ((node < 1) || (node > len)) {
      throw new SimpleListException("No such node:" + node);
    }

    String s1, s2;
    int rel1 = aRel1[node];
    int rel2 = aRel2[node];

    if (rel1 == 0) {
      s1 = "0";
    } else {
      s1 = "(" + getListString(rel1) + ")";
      for (int i = 0; i < Symbols.lst.length; i++) {
        if (s1.equalsIgnoreCase(Symbols.lst[i])) {
          s1 = Character.toString(Symbols.names[i]);
        }
      }
    }

    if (rel2 == 0) {
      s2 = "";
    } else {
      s2 = getListString(rel2);
    }

    return s1 + s2;
  }

  public int findMessage(Message msg, int node) {
    // поиск на траке мини-трака

    if ((node < 0) || (node > msg.len)) {
      throw new SimpleListException("Invalid node:" + node);
    }
    if (node == 0) {
      return 0;
    }

    msg.marksInit(node);
    msg.marksMarkList(node);

    if (!msg.rltCreate(this, true, false, true)) {
      return 0;
    }

    return msg.rltRef[node];
  }

  public int old_findMessage(Message msg, int node) {
    // поиск на траке мини-трака

    if ((node < 1) || (node > msg.len)) {
      throw new SimpleListException("Invalid node:" + node);
    }

    int[] ref = new int[node + 1];
    for (int i = 1; i < ref.length; i++) {
      ref[i] = 0;
    }
    ref[0] = -1;

    return old_findMessage2(msg, node, ref);
  }

  public int findMessage(Message msg) {
    // поиск на траке мини-трака (последний список)

    return findMessage(msg, msg.len);
  }

  private int old_findMessage2(Message msg, int node, int[] ref) {
    if (node == 0) {
      return 0;
    }

    if (ref[node] > 0) {
      return ref[node];
    }

    int rel1 = 0;
    int rel2 = 0;

    int node1 = msg.aRel1[node];
    int node2 = msg.aRel2[node];

    if (node1 > 0) {
      rel1 = old_findMessage2(msg, node1, ref);
      if (rel1 == 0) {
        return 0;
      }
    }
    if (node2 > 0) {
      rel2 = old_findMessage2(msg, node2, ref);
      if (rel2 == 0) {
        return 0;
      }
    }

    int ret = findNode(rel1, rel2);
    ref[node] = ret;
    return ret;
  }

  public int addMessage(Message msg, int node) {
    // добавление на трак мини-трака

    if ((node < 1) || (node > msg.len)) {
      throw new SimpleListException("Invalid node:" + node);
    }

    msg.marksInit(node);
    msg.marksMarkList(node);

    if (!msg.rltCreate(this, true, true, false)) {
      throw new SimpleListException("Unknown error finding message; node:" + node);
    }

    return msg.rltRef[node];
  }

  public int old_addMessage(Message msg, int node) {
    // добавление на трак мини-трака

    if ((node < 1) || (node > msg.len)) {
      throw new SimpleListException("Invalid node:" + node);
    }

    int[] ref = new int[node + 1];
    for (int i = 1; i < ref.length; i++) {
      ref[i] = 0;
    }
    ref[0] = -1;

    return old_addMessage2(msg, node, ref);
  }

  public int addMessage(Message msg) {
    // добавление на трак мини-трака (последний список)
    return addMessage(msg, msg.len);
  }

  private int old_addMessage2(Message msg, int node, int[] ref) {
    if (node == 0) {
      return 0;
    }

    int found = old_findMessage2(msg, node, ref);
    if (found > 0) {
      return found;
    }

    // решили, что сначала всегда добавляем левую ветку (важно для мини-трака)
    int rel1 = old_addMessage2(msg, msg.aRel1[node], ref);
    int rel2 = old_addMessage2(msg, msg.aRel2[node], ref);

    return addNode(rel1, rel2);
  }

  public int addByteSimp(byte b) {
    int n1 = addNode(0, 0);
    int n_prev = 0;
    int bb = b;

    int bit;
    for (int i = 0; i < 8; i++) {
      bit = bb & 0x80;
      if (bit == 0) {
        n_prev = addNode(0, n_prev);
      } else {
        n_prev = addNode(n1, n_prev);
      }
      bb = bb << 1;
    }

    return n_prev;
  }

  public int addByte2Simp(int b) {
    int n1 = addNode(0, 0);
    int n_prev = 0;
    int bb = b;

    int bit;
    for (int i = 0; i < 16; i++) {
      bit = bb & 0x8000;
      if (bit == 0) {
        n_prev = addNode(0, n_prev);
      } else {
        n_prev = addNode(n1, n_prev);
      }
      bb = bb << 1;
    }

    return n_prev;
  }

  public int addChar(char ch) {
    // добавление символа unicode в двоичном представлении

    int ichr = ch;

    int n_prev = addByte2Simp(ichr);

    int mark = addSymbol_a();

    n_prev = addNode(mark, n_prev);

    return n_prev;
  }

  public int addSymbol(char sym) {
    // добавление символа на трак

    for (int i = 0; i < Symbols.names.length; i++) {
      if (Symbols.names[i] == sym) {
        if (symbols[i] > 0) {
          return symbols[i];
        }

        symbols[i] = Symbols.addSymbol(this, sym);
        return symbols[i];
      }
    }

    throw new SimpleListException("Unknown symbol name: " + sym);
  }

  public int addSymbol_a() {
    // добавление символа a на трак

    if (symbols[1] == 0) {
      symbols[1] = Symbols.addSymbol(this, 'a');
    }
    return symbols[1];
  }

  public int addSymbol_b() {
    // добавление символа b на трак

    if (symbols[2] == 0) {
      symbols[2] = Symbols.addSymbol(this, 'b');
    }
    return symbols[2];
  }

  public int addSymbol_c() {
    // добавление символа c на трак

    if (symbols[3] == 0) {
      symbols[3] = Symbols.addSymbol(this, 'c');
    }
    return symbols[3];
  }

  public int addSymbol_d() {
    // добавление символа d на трак

    if (symbols[4] == 0) {
      symbols[4] = Symbols.addSymbol(this, 'd');
    }
    return symbols[4];
  }

  public int addSymbol_e() {
    // добавление символа e на трак

    if (symbols[5] == 0) {
      symbols[5] = Symbols.addSymbol(this, 'e');
    }
    return symbols[5];
  }

  public int addSymbol_f() {
    // добавление символа f на трак

    if (symbols[6] == 0) {
      symbols[6] = Symbols.addSymbol(this, 'f');
    }
    return symbols[6];
  }

  public int addSymbol_g() {
    // добавление символа g на трак

    if (symbols[7] == 0) {
      symbols[7] = Symbols.addSymbol(this, 'g');
    }
    return symbols[7];
  }

  public int addByte(byte b) {
    // добавление байта в двоичном представлении

    int n_prev = addByteSimp(b);

    int mark1 = addString("byte1", 0);
    int mark2 = addSymbol_b();
    int mark = addNode(mark2, mark1);

    n_prev = addNode(mark, n_prev);

    return n_prev;
  }

  public int addByte2(int b) {
    // добавление двух байт в двоичном представлении

    int n_prev = addByte2Simp(b);

    int mark1 = addString("byte2", 0);
    int mark2 = addSymbol_b();
    int mark = addNode(mark2, mark1);

    n_prev = addNode(mark, n_prev);

    return n_prev;
  }

  public int addString(String str, int prev_node) {
    // запись на трак строки (задом наперед)

    if ((str == null) || str.isEmpty()) {
      return prev_node;
    }

    char[] ch = str.toCharArray();

    int prev = prev_node;
    int ichr;

    for (int i = 0; i < ch.length; i++) {
      ichr = addChar(ch[i]);
      prev = addNode(ichr, prev);
    }

    return prev;
  }

  public int addText(String txt, int textPrefix, int wordPrefix, int delimPrefix) {
    // запись на трак текста с разделением на слова и разделители

    if ((txt == null) || txt.isEmpty()) {
      return 0;
    }

    char[] ca = txt.toCharArray();

    return addText(ca, ca.length, textPrefix, wordPrefix, delimPrefix);
  }

  public int addText(char[] ca, int caLen, int textPrefix, int wordPrefix, int delimPrefix) {
    if (caLen < 1) {
      return 0;
    }

    int status = 0; // 0 - неопределенный статус, 1 - слово, 2 - разделитель
    int status1 = 0;
    int statusStart = 0, statusEnd;
    char ch;
    int prevNode = textPrefix;

    for (int i = 0; i < caLen; i++) {
      ch = ca[i];
      if (Character.isLetterOrDigit(ch) || (ch == '_')) {
        status1 = 1;
      } else {
        status1 = 2;
      }

      if (status1 != status) {
        statusEnd = i;
        prevNode = addText_word(ca, status, statusStart, statusEnd, prevNode, wordPrefix, delimPrefix);
        status = status1;
        statusStart = i;
      }
    }

    prevNode = addText_word(ca, status1, statusStart, caLen, prevNode, wordPrefix, delimPrefix);
    return prevNode;
  }

  private int addText_word(char[] ca, int status, int statusStart, int statusEnd, int prevNode, int wordPrefix, int delimPrefix) {
    if ((status == 0) || (statusEnd <= statusStart)) {
      return prevNode;
    }

    int head = 0;

    if (status == 1) {
      // запись слова
      if (wordPrefix > 0) {
        head = wordPrefix;
      }
      for (int i = statusStart; i < statusEnd; i++) {
        head = addNode(addChar(ca[i]), head);
      }
    } else {
      // запись разделителя (если не пробел)

      int isSpace = 0;
      if ((statusEnd == (statusStart + 1)) && (ca[statusStart] == ' ') && (statusStart > 0) && (statusEnd < ca.length)) {
        isSpace = 1;
      }

      if (isSpace == 0) {
        if (delimPrefix > 0) {
          head = delimPrefix;
        }
        for (int i = statusStart; i < statusEnd; i++) {
          head = addNode(addChar(ca[i]), head);
        }
      }
    }

    if (head > 0) {
      head = addNode(head, prevNode);
    } else {
      head = prevNode;
    }

    return head;
  }

  public int findSymbol_a() {
    // поиск на траке символа a

    int sym = symbols[1];

    if (sym == 0) {
      sym = findMessage(Symbols.getSymMsg('a'));
      symbols[1] = sym;
    }

    return sym;
  }

  public int findSymbol_b() {
    // поиск на траке символа b

    int sym = symbols[2];

    if (sym == 0) {
      sym = findMessage(Symbols.getSymMsg('b'));
      symbols[2] = sym;
    }

    return sym;
  }

  public int findSymbol_c() {
    // поиск на траке символа c

    int sym = symbols[3];

    if (sym == 0) {
      sym = findMessage(Symbols.getSymMsg('c'));
      symbols[3] = sym;
    }

    return sym;
  }

  public int findSymbol_d() {
    // поиск на траке символа d

    int sym = symbols[4];

    if (sym == 0) {
      sym = findMessage(Symbols.getSymMsg('d'));
      symbols[4] = sym;
    }

    return sym;
  }

  public int findSymbol_e() {
    // поиск на траке символа e

    int sym = symbols[5];

    if (sym == 0) {
      sym = findMessage(Symbols.getSymMsg('e'));
      symbols[5] = sym;
    }

    return sym;
  }

  public int findSymbol_f() {
    // поиск на траке символа f

    int sym = symbols[6];

    if (sym == 0) {
      sym = findMessage(Symbols.getSymMsg('f'));
      symbols[6] = sym;
    }

    return sym;
  }

  public int findSymbol_g() {
    // поиск на траке символа g

    int sym = symbols[7];

    if (sym == 0) {
      sym = findMessage(Symbols.getSymMsg('g'));
      symbols[7] = sym;
    }

    return sym;
  }

  public char getChar(int node) {
    // получение символа с трака

    int mark = findSymbol_a();

    if (mark == 0) {
      throw new SimpleListException("No chars at all (no symbol 'a')");
    }

    int nn = node;
    int ret = 0;

    if (aRel1[nn] != mark) {
      throw new SimpleListException("No char at " + node + " (no mark)");
    }

    for (int i = 0; i < 16; i++) {
      // перебираем биты
      nn = aRel2[nn];

      if (nn == 0) {
        throw new SimpleListException("Unexpected end of char at " + nn + " (bit " + i + ")");
      }

      ret = ret >> 1;

      if (aRel1[nn] == 1) {
        ret = ret | 0x8000;
      } else if (aRel1[nn] != 0) {
        throw new SimpleListException("Unexpected bit of char at " + nn + " (bit " + i + ")");
      }
    }

    if (aRel2[nn] != 0) {
      throw new SimpleListException("End of char not found at " + nn);
    }

    return (char) ret;
  }

  public String getText(int node, int textPrefix, int wordPrefix, int delimPrefix) {
    // получение текста с трака

    if ((node < 1) || (node > len)) {
      throw new SimpleListException("Invalid node:" + node);
    }

//    char[] buf = new char[node * 2];
    char[] buf = new char[1000];
    int bufLen = 0;
    int afterWord = 0;
    int fin = 0;
    int curNode = node;
    int status;
    int fin2;
    int curNode2;
    char ch;

    while (fin == 0) {
      // перебор списка слов
      if ((curNode == 0) || (curNode == textPrefix)) {
        fin = 1;
        continue;
      }

      // обработка слова/разделителя
      status = 0;
      fin2 = 0;
      curNode2 = aRel1[curNode];
      while (fin2 == 0) {
        if ((curNode2 == 0) || (curNode2 == wordPrefix) || (curNode2 == delimPrefix)) {
          fin2 = 1;
          continue;
        }

        ch = getChar(aRel1[curNode2]);

        // получен символ, нужен ли перед ним пробел?
        if (status == 0) {
          if (Character.isLetterOrDigit(ch) || (ch == '_')) {
            if (afterWord == 1) {
              // нужен дополнительный пробел
              buf[bufLen] = ' ';
              bufLen++;
              buf = utilCheckExtend(buf, bufLen);
            }
            afterWord = 1;
          } else {
            afterWord = 0;
          }
          status = 2;
        }

        buf[bufLen] = ch;
        bufLen++;
        buf = utilCheckExtend(buf, bufLen);

        curNode2 = aRel2[curNode2];
      }

      curNode = aRel2[curNode];
    }

    if (bufLen == 0) {
      return "";
    }

    char[] ret = new char[bufLen];
    for (int i = 0; i < bufLen; i++) {
      ret[i] = buf[bufLen - i - 1];
    }

    return new String(ret);
  }

  private char[] utilCheckExtend(char[] ca, int caLen) {
    // проверка и при необходимости увеличение длины массива
    if (caLen < ca.length) {
      return ca;
    } else {
      char[] tmp = new char[(int) (caLen * 2)];
      for (int i = 0; i < caLen; i++) {
        tmp[i] = ca[i];
      }
      return tmp;
    }
  }

  public int addTextFromFile(String fileName) throws FileNotFoundException, IOException {
    // запись на трак текста из файла

    File file = new File(fileName);
    FileInputStream is = new FileInputStream(file);

    char[] buf = new char[(int) (file.length() * 2) + 10];

    InputStreamReader isr = new InputStreamReader(is, "windows-1251");
    BufferedReader br = new BufferedReader(isr);
    int eof = 0;
    int bufLen = 0;
    String str;
    char[] buf2;

    while (eof == 0) {
      str = br.readLine();
      if (str == null) {
        eof = 1;
      } else {
        buf2 = str.toCharArray();
        for (int i = 0; i < buf2.length; i++) {
          buf[bufLen] = buf2[i];
          bufLen++;
        }
        buf[bufLen] = '\r';
        bufLen++;
        buf[bufLen] = '\n';
        bufLen++;
      }
    }

    if (eof == 0) {
      throw new SimpleListException("EOF not reached reading file " + fileName);
    }

    if (bufLen == 0) {
      throw new SimpleListException("Nothing read form file " + fileName);
    }

    return addText(buf, bufLen, 0, 0, 0);
  }

  public String checkUnique() {
    // проверка уникальности узлов трака

    int r1, r2;
    for (int i = 1; i <= len; i++) {
      r1 = aRel1[i];
      r2 = aRel2[i];
      for (int j = 1; j < i; j++) {
        if ((r1 == aRel1[j]) && (r2 == aRel2[j])) {
          return "ERROR: same nodes " + i + " and " + j;
        }
      }
    }

    return "No same nodes found";
  }

  public int heshGetBits() {
    return heshBits;
  }

  public int heshGetIndexLen() {
    if (heshIndex == null) {
      return 0;
    } else {
      return heshIndex.length;
    }
  }

  public int heshGetIndexNode(int hesh) {
    if ((heshIndex == null) || (hesh < 0) || (hesh >= heshIndex.length)) {
      return 0;
    } else {
      return heshIndex[hesh];
    }
  }

  public int heshGetChainLen(int hesh) {
    if ((heshChainLen == null) || (hesh < 0) || (hesh >= heshChainLen.length)) {
      return 0;
    } else {
      return heshChainLen[hesh];
    }
  }

  public int heshGetPrevNode(int node) {
    if ((heshPrev == null) || (node < 1) || (node > len)) {
      return 0;
    } else {
      return heshPrev[node];
    }
  }

  public void heshOptimize() {
    int bits = 0;

    if (len < 1000) {
      bits = 10;
    } else {
      int bt = len;
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

    if ((heshBits != bits) || (heshType != 3)) {
      heshSet(bits, 3);
    }
  }

  public void deltaOptimize() {
    delta = aRel1.length / 10;
    if (delta < 100) {
      delta = 100;
    }
  }

  public void optimizeParams() {
    deltaOptimize();
    heshOptimize();
  }

  public boolean rltCreate(Message msg, boolean marksOnly, boolean doAdd, boolean stopOnNotFound) {
    if (msg == null) {
//      rltMsg = null;
      rltRef = null;
      return false;
    }

    if (doAdd && stopOnNotFound) {
      throw new SimpleListException("Parameters doAdd and stopOnNotFound cannot be set to TRUE simultaniously");
    }

//    rltMsg = msg;
    rltRef = new int[aRel1.length];
    rltRef[0] = 0;

    int rel1, rel2;
    boolean ret = true;

    for (int i = 1; i <= len; i++) {
      if ((!marksOnly) || ((i <= marksLen) && (marks[i] != 0))) {
        rel1 = aRel1[i];
        rel2 = aRel2[i];

        if (((rel1 == 0) || (rltRef[rel1] != 0)) && ((rel2 == 0) || (rltRef[rel2] != 0))) {
          if (doAdd) {
            rltRef[i] = msg.addNode(rltRef[rel1], rltRef[rel2]);
          } else {
            rltRef[i] = msg.findNode(rltRef[rel1], rltRef[rel2]);
          }
        } else {
          rltRef[i] = 0;
        }

        if (rltRef[i] == 0) {
          ret = false;
          if (stopOnNotFound) {
            return ret;
          }
        }
      }
    }

    return ret;
  }

  public int rltGetRef(int node) {
    if ((rltRef == null) || (node < 1) || (node > len)) {
      return 0;
    } else {
      return rltRef[node];
    }
  }

  public void marksInit(int a_marksLen) {
    // инициализация массива отметок
    if ((a_marksLen < 0) || (a_marksLen > len)) {
      throw new SimpleListException("Wrong marks array len: " + a_marksLen);
    }

    if (a_marksLen == 0) {
      marksLen = 0;
      marks = null;
    } else if (a_marksLen <= marksLen) {
      for (int i = 0; i < marks.length; i++) {
        marks[i] = 0;
      }
    } else {
      marksLen = a_marksLen;
      marks = new byte[a_marksLen + 1];
    }
  }

  public void marksInit() {
    // инициализация массива отметок
    marksInit(len);
  }

  public void marksSet(int node, byte m) {
    if (marks == null) {
      throw new SimpleListException("Marks not initialized");
    }
    if ((node < 1) || (node > marksLen)) {
      throw new SimpleListException("Wrong node:" + node);
    }

    marks[node] = m;
  }

  public byte marksGet(int node) {
    if (marks == null) {
      throw new SimpleListException("Marks not initialized");
    }
    if ((node < 1) || (node > marksLen)) {
      throw new SimpleListException("Wrong node:" + node);
    }

    return marks[node];
  }

  public void marksMarkList(int node) {
    // отметка узла и всех ссылок от узла

    if (marks == null) {
      throw new SimpleListException("Marks not initialized");
    }
    if ((node < 1) || (node > marksLen)) {
      throw new SimpleListException("Wrong node:" + node);
    }

    int rel1, rel2;

    marks[node] = 1;
    for (int i = node; i > 1; i--) {
      if (marks[i] != 0) {
        // отмечаем узлы по связям
        rel1 = aRel1[i];
        rel2 = aRel2[i];
        if (rel1 > 0) {
          marks[rel1] = 1;
        }
        if (rel2 > 0) {
          marks[rel2] = 1;
        }
      }
    }
  }

  public int cmpNodes(int node1, int node2) {
    // сравнение списков (узлов) node1 и node2:
    //    1 - node1 больше чем node2
    //    0 - списки равны
    //   -1 - node1 меньше чем node2

    if (node1 == node2) {
      return 0;
    }
    if ((node1 < 0) || (node1 > len)) {
      throw new SimpleListException("Wrong node1: " + node1);
    }
    if ((node2 < 0) || (node2 > len)) {
      throw new SimpleListException("Wrong node2: " + node2);
    }
    if ((node1 == 0) || (node2 == 0)) {
      if ((node1 == 0) && (node2 != 0)) {
        return -1;
      } else if ((node1 != 0) && (node2 == 0)) {
        return 1;
      } else {
        return 0;
      }
    }

    int nd1 = node1;
    int nd2 = node2;
    int r1, r2;

    while ((nd1 != 0) && (nd2 != 0) && (nd1 != nd2)) {
      r1 = aRel2[nd1];
      r2 = aRel2[nd2];

      if (r1 == r2) {
        nd1 = aRel1[nd1];
        nd2 = aRel1[nd2];
      } else {
        nd1 = r1;
        nd2 = r2;
      }
    }

    if ((nd1 == 0) && (nd2 != 0)) {
      return -1;
    } else if ((nd1 != 0) && (nd2 == 0)) {
      return 1;
    } else {
      throw new SimpleListException("Not unique nodes around " + nd1 + " and " + nd2
              + " while comparing " + node1 + " and " + node2);
    }
  }

  public void listLenInit() {
    // инициализация механизма подсчета длины списков для узлов трака

    if (listLen != null) {
      return;
    }

    listLen = new int[aRel1.length];
    listLen[0] = 0;
    for (int i = 1; i <= len; i++) {
      listLen[i] = listLen[aRel2[i]] + 1;
    }
  }

  public int cmpNodesStr(int node1, int node2) {
    // сравнение списков (узлов) node1 и node2 как строк
    // (без подразумеваемых ведущих пробелов):
    //    1 - node1 больше чем node2
    //    0 - списки равны
    //   -1 - node1 меньше чем node2

    if (node1 == node2) {
      return 0;
    }
    if ((node1 < 0) || (node1 > len)) {
      throw new SimpleListException("Wrong node1: " + node1);
    }
    if ((node2 < 0) || (node2 > len)) {
      throw new SimpleListException("Wrong node2: " + node2);
    }
    if ((node1 == 0) || (node2 == 0)) {
      if ((node1 == 0) && (node2 != 0)) {
        return -1;
      } else if ((node1 != 0) && (node2 == 0)) {
        return 1;
      } else {
        return 0;
      }
    }

    listLenInit();

    int nd1 = node1;
    int nd2 = node2;
    int len1, len2, prev1, prev2;

    while ((nd1 != 0) && (nd2 != 0) && (nd1 != nd2)) {
      len1 = listLen[nd1];
      len2 = listLen[nd2];

      if (len1 > len2) {
        for (int i = (len1 - len2); i > 0; i--) {
          nd1 = aRel2[nd1];
        }
        if (nd1 == nd2) {
          return 1;
        }
      } else if (len1 < len2) {
        for (int i = (len2 - len1); i > 0; i--) {
          nd2 = aRel2[nd2];
        }
        if (nd1 == nd2) {
          return -1;
        }
      }

      // nd1 не равно nd2, а длины списков равны
      // ищем совпадение начала списка
      prev1 = nd1;
      prev2 = nd2;
      while ((nd1 != 0) && (nd2 != 0) && (nd1 != nd2)) {
        prev1 = nd1;
        prev2 = nd2;
        nd1 = aRel2[nd1];
        nd2 = aRel2[nd2];
      }

      if (nd1 != nd2) {
        throw new SimpleListException("Unknown error around " + nd1 + " and " + nd2
                + " while comparing " + node1 + " and " + node2);
      }

      nd1 = aRel1[prev1];
      nd2 = aRel1[prev2];
    }

    if ((nd1 == 0) && (nd2 != 0)) {
      return -1;
    } else if ((nd1 != 0) && (nd2 == 0)) {
      return 1;
    } else {
      throw new SimpleListException("Not unique nodes around " + nd1 + " and " + nd2
              + " while comparing " + node1 + " and " + node2);
    }
  }

  public String getString(int node, int stringPrefix) {
    // получение текста с трака

    if ((node < 0) || (node > len)) {
      throw new SimpleListException("Invalid node:" + node);
    }
    if (node == 0) {
      return "";
    }

    char[] buf = new char[100];
    int bufLen = 0;
    int fin = 0;
    int curNode = node;
    char ch;

    while (fin == 0) {
      if ((curNode == 0) || (curNode == stringPrefix)) {
        fin = 1;
        continue;
      }

      ch = getChar(aRel1[curNode]);

      buf[bufLen] = ch;
      bufLen++;
      buf = utilCheckExtend(buf, bufLen);

      curNode = aRel2[curNode];
    }

    if (bufLen == 0) {
      return "";
    }

    char[] ret = new char[bufLen];
    for (int i = 0; i < bufLen; i++) {
      ret[i] = buf[bufLen - i - 1];
    }

    return new String(ret);
  }

  public int getLastUsed1(int node) {
    if ((node < 0) || (node > len)) {
      throw new SimpleListException("No such node:" + node);
    }
    return lastUsed1[node];
  }

  public int getLastUsed2(int node) {
    if ((node < 0) || (node > len)) {
      throw new SimpleListException("No such node:" + node);
    }
    return lastUsed2[node];
  }

  public int getPrevUsed1(int node) {
    if ((node < 0) || (node > len)) {
      throw new SimpleListException("No such node:" + node);
    }
    return prevUsed1[node];
  }

  public int getPrevUsed2(int node) {
    if ((node < 0) || (node > len)) {
      throw new SimpleListException("No such node:" + node);
    }
    return prevUsed2[node];
  }

  public boolean isByte(int node) {
    if ((node < 1) || (node > len)) {
      throw new SimpleListException("No such node:" + node);
    }
    int symbB = findSymbol_b();
    int au1 = aRel1[node];
    int au2 = aRel2[node];

    if ((au1 == 0) || (au2 == 0)) {
      return false;
    }

    int au11 = aRel1[au1];
    au1 = aRel2[au1];
    if ((au11 != symbB) || (au1 == 0)) {
      return false;
    }

    try {
      au11 = aRel1[au1];
      au1 = aRel2[au1];
      if ((au1 == 0) || (getChar(au11) != 'b')) {
        return false;
      }
      au11 = aRel1[au1];
      au1 = aRel2[au1];
      if ((au1 == 0) || (getChar(au11) != 'y')) {
        return false;
      }
      au11 = aRel1[au1];
      au1 = aRel2[au1];
      if ((au1 == 0) || (getChar(au11) != 't')) {
        return false;
      }
      au11 = aRel1[au1];
      au1 = aRel2[au1];
      if ((au1 == 0) || (getChar(au11) != 'e')) {
        return false;
      }
      au11 = aRel1[au1];
      au1 = aRel2[au1];
      if ((au1 != 0) || (getChar(au11) != '1')) {
        return false;
      }
    } catch (SimpleListException e) {
      return false;
    }

    int au21;
    for (int i = 0; i < 8; i++) {
      if (au2 == 0) {
        return false;
      }
      au21 = aRel1[au2];
      if ((au21 != 0) && (au21 != 1)) {
        return false;
      }
      au2 = aRel2[au2];
    }

    if (au2 != 0) {
      return false;
    }

    return true;
  }

  public boolean isByte2(int node) {
    if ((node < 1) || (node > len)) {
      throw new SimpleListException("No such node:" + node);
    }
    int symbB = findSymbol_b();
    int au1 = aRel1[node];
    int au2 = aRel2[node];

    if ((au1 == 0) || (au2 == 0)) {
      return false;
    }

    int au11 = aRel1[au1];
    au1 = aRel2[au1];
    if ((au11 != symbB) || (au1 == 0)) {
      return false;
    }

    try {
      au11 = aRel1[au1];
      au1 = aRel2[au1];
      if ((au1 == 0) || (getChar(au11) != 'b')) {
        return false;
      }
      au11 = aRel1[au1];
      au1 = aRel2[au1];
      if ((au1 == 0) || (getChar(au11) != 'y')) {
        return false;
      }
      au11 = aRel1[au1];
      au1 = aRel2[au1];
      if ((au1 == 0) || (getChar(au11) != 't')) {
        return false;
      }
      au11 = aRel1[au1];
      au1 = aRel2[au1];
      if ((au1 == 0) || (getChar(au11) != 'e')) {
        return false;
      }
      au11 = aRel1[au1];
      au1 = aRel2[au1];
      if ((au1 != 0) || (getChar(au11) != '2')) {
        return false;
      }
    } catch (SimpleListException e) {
      return false;
    }

    int au21;
    for (int i = 0; i < 16; i++) {
      if (au2 == 0) {
        return false;
      }
      au21 = aRel1[au2];
      if ((au21 != 0) && (au21 != 1)) {
        return false;
      }
      au2 = aRel2[au2];
    }

    if (au2 != 0) {
      return false;
    }

    return true;
  }

  public boolean isChar(int node) {
    try {
      char ch = getChar(node);
    } catch (SimpleListException e) {
      return false;
    }

    return true;
  }

  public boolean isString(int node, int prev_node) {
    if ((node < 1) || (node > len)) {
      throw new SimpleListException("No such node:" + node);
    }

    int au = node;

    if (au == 0) {
      return false;
    }

    while ((au != 0) && (au != prev_node)) {
      if (!isChar(aRel1[au])) {
        return false;
      }
      au = aRel2[au];
    }

    if (au != prev_node) {
      return false;
    }

    return true;
  }
}
