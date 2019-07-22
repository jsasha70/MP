package jsasha.old;

import java.util.Arrays;
import jsasha.lt.LogicTreeException;

public class LogicTree_old {

  private int[] aRel1; // левые ссылки из узлов списков
  private int[] aRel2; // правые ссылки из узлов списков
  private int nArgs; // число параметров (плюс к этому еще нулевой элемент)
  private int nArgs1; // nArgs + 1
  private int len;
  private long findCount = 0;
  private long findCountSteps = 0;
  private int[] lastUsed1 = null; // последнее использование узла в левой ссылке (нужно?)
  private int[] lastUsed2 = null; // последнее использование узла в правой ссылке (нужно?)
  private int[] prevUsed1 = null; // предыдущие использования узла в левой ссылке (нужно?)
  private int[] prevUsed2 = null; // предыдущие использования узла в правой ссылке (нужно?)
  // работа хеша:
  private int hashBits = 0; // размерность хеша в битах (0 - отключен)
  private int[] hashIndex = null; // индекс (ссылка на последний узел с данным хешем)
  private int[] hashChainLen = null; // число узлов с данным хешем
  private int[] hashPrev = null; // цепочка узлов с одним хешем
  // отметки узлов (для массовой нерекурсивной обработки)
//  private byte[] marks = null;

  public LogicTree_old(int nArgs, int initLen) {
    if (initLen < (nArgs + 10)) {
      initLen = nArgs + 10;
    }
    this.nArgs = nArgs;
    this.nArgs1 = nArgs + 1;

    aRel1 = new int[initLen];
    aRel2 = new int[initLen];
    lastUsed1 = new int[initLen];
    lastUsed2 = new int[initLen];
    prevUsed1 = new int[initLen];
    prevUsed2 = new int[initLen];
    len = nArgs1;
    Arrays.fill(aRel1, 0, len, -1);
    Arrays.fill(aRel2, 0, len, -1);
    Arrays.fill(lastUsed1, 0, len, -1);
    Arrays.fill(lastUsed2, 0, len, -1);
    Arrays.fill(prevUsed1, 0, len, -1);
    Arrays.fill(prevUsed2, 0, len, -1);
    addNode(0, 0);
  }

  public LogicTree_old(int nArgs) {
    this(nArgs, 1000);
  }

  public void loadData(int[] a_rel1, int[] a_rel2, int a_len, int a_size) {
    int l_len = a_len > 0 ? a_len : a_rel1.length;
    int l_size = a_size > 0 ? a_size : l_len;

    if (l_len > l_size) {
      throw new LogicTreeException("Length " + l_len + " must not be greater then size " + l_size);
    }
    if (l_len > a_rel1.length) {
      throw new LogicTreeException("Length " + l_len + " must not be greater then array a_rel1 size " + a_rel1.length);
    }
    if (l_len > a_rel2.length) {
      throw new LogicTreeException("Length " + l_len + " must not be greater then array a_rel2 size " + a_rel2.length);
    }
    if (a_rel1[0] != -1) {
      throw new LogicTreeException("a_rel1[0] must be -1, not " + a_rel1[0]);
    }
    if (a_rel2[0] != -1) {
      throw new LogicTreeException("a_rel2[0] must be -1, not " + a_rel2[0]);
    }

    len = l_len;
    nArgs = 0;
    for (int i = 0; i < len; i++) {
      if ((a_rel1[i] == -1) || (a_rel2[i] == -1)) {
        if ((a_rel1[i] != -1) || (a_rel2[i] != -1)) {
          throw new LogicTreeException("a_rel1[" + i + "] and a_rel2[" + i
                  + "] mast or mast not both be -1; got (" + a_rel1[i] + "," + a_rel2[i] + ")");
        }
        nArgs = i;
      } else {
        break;
      }
    }
    nArgs1 = nArgs + 1;

    if (aRel1.length < l_size) {
      aRel1 = new int[l_size];
      aRel2 = new int[l_size];
    }
    for (int i = 0; i < len; i++) {
      aRel1[i] = a_rel1[i];
      aRel2[i] = a_rel2[i];
    }

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

  private void enlargeArrays() {
    // увеличиваем длину массива
    int newSize = aRel1.length * 3 / 2;

    aRel1 = Arrays.copyOf(aRel1, newSize);
    aRel2 = Arrays.copyOf(aRel2, newSize);
    lastUsed1 = Arrays.copyOf(lastUsed1, newSize);
    lastUsed2 = Arrays.copyOf(lastUsed2, newSize);
    prevUsed1 = Arrays.copyOf(prevUsed1, newSize);
    prevUsed2 = Arrays.copyOf(prevUsed2, newSize);

    hashPrev = Arrays.copyOf(hashPrev, newSize);
    hashOptimize();
  }

  public final int addNode(int rel1, int rel2) {
    if ((rel1 < 0) || (rel1 >= len)) {
      throw new LogicTreeException("Invalid rel 1:" + rel1);
    }
    if ((rel2 < 0) || (rel2 >= len)) {
      throw new LogicTreeException("Invalid rel 2:" + rel2);
    }

    int found = findNode(rel1, rel2);
    if (found >= 0) {
      return found;
    }

    if (len >= aRel1.length) {
      // вышли за границы, увеличиваем длину массива
      enlargeArrays();
    }

    int idx = len;
    len++;

    aRel1[idx] = rel1;
    aRel2[idx] = rel2;

    nodeSetHash(idx);
    nodeUsageCalc(idx);

    return idx;
  }

  private void nodeUsageCalcAll() {
    // рассчет использования ссылок для всех узлов (например, после загрузки массива узлов из файла)
    // длина массива узлов len д.б. уже установлена

    lastUsed1 = utilCreArray(lastUsed1, aRel1.length);
    lastUsed2 = utilCreArray(lastUsed2, aRel1.length);
    prevUsed1 = utilCreArray(prevUsed1, aRel1.length);
    prevUsed2 = utilCreArray(prevUsed2, aRel1.length);
    Arrays.fill(lastUsed1, 0, nArgs1, -1);
    Arrays.fill(lastUsed2, 0, nArgs1, -1);
    Arrays.fill(prevUsed1, 0, nArgs1, -1);
    Arrays.fill(prevUsed2, 0, nArgs1, -1);

    for (int i = nArgs1; i < len; i++) {
      nodeUsageCalc(i);
    }
  }

  private void nodeUsageCalc(int node) {
    // учет использования ссылок на компоненты узла

    int rel1 = aRel1[node];
    int rel2 = aRel2[node];

    if (rel1 >= 0) {
      prevUsed1[node] = lastUsed1[rel1];
      lastUsed1[rel1] = node;
      lastUsed1[node] = -1;
    }

    if (rel2 >= 0) {
      prevUsed2[node] = lastUsed2[rel2];
      lastUsed2[rel2] = node;
      lastUsed2[node] = -1;
    }
  }

  private int hashCalc(int rel1, int rel2) {
    // рассчет хеша для узла (пары ссылок)
    // метод: сумма младших битов

    if (hashBits == 0) {
      return 0;
    }

    int mask = (1 << hashBits) - 1;
    return ((rel1 & mask) + (rel2 & mask)) & mask;
  }

  private void nodeSetHash(int node) {
    // установка хеша для узла (узлы должны перебираться последовательно по возрастанию)

    if (hashBits == 0) {
      return;
    }

    int hash = hashCalc(aRel1[node], aRel2[node]);

    hashPrev[node] = hashIndex[hash];
    hashIndex[hash] = node;
    hashChainLen[hash]++;
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

  public int nodeGetRel1(int node) {
    return aRel1[node];
  }

  public int nodeGetRel2(int node) {
    return aRel2[node];
  }

  public int findNode(int rel1, int rel2) {
    // поиск узла с заданными ветками (адресами на траке)

    if ((rel1 == -1) || (rel2 == -1)) {
      return 0;
    }
    if ((rel1 == 0) && (rel2 == 0) && (len > nArgs1)) {
      return nArgs1;  // единица
    }

    findCount++;

    int max_no = rel1;
    if (max_no < rel2) {
      max_no = rel2;
    }

    if (hashBits == 0) {
      // поиск без хеша
      for (int i = max_no + 1; i < len; i++) {
        findCountSteps++;
        if ((aRel1[i] == rel1) && (aRel2[i] == rel2)) {
          return i;
        }
      }
    } else {
      // поиск с хешем

      int hash = hashCalc(rel1, rel2);
      int prevNode = hashIndex[hash];

      while (prevNode > max_no) {
        findCountSteps++;
        if ((aRel1[prevNode] == rel1) && (aRel2[prevNode] == rel2)) {
          return prevNode;
        }
        prevNode = hashPrev[prevNode];
      }
    }

    return -1;
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

  public String checkUnique() {
    // проверка уникальности узлов трака

    int r1, r2, minR;
    for (int i = nArgs1; i < len; i++) {
      r1 = aRel1[i];
      r2 = aRel2[i];
      minR = nArgs1;
      if (minR < r1) {
        minR = r1;
      }
      if (minR < r2) {
        minR = r2;
      }
      for (int j = minR; j < i; j++) {
        if ((r1 == aRel1[j]) && (r2 == aRel2[j])) {
          return "ERROR: same nodes " + i + " and " + j;
        }
      }
    }

    return "No same nodes found";
  }

  public int hashGetBits() {
    return hashBits;
  }

  public int hashGetIndexLen() {
    if (hashIndex == null) {
      return 0;
    } else {
      return hashIndex.length;
    }
  }

  public int hashGetIndexNode(int hash) {
    if ((hashIndex == null) || (hash < 0) || (hash >= hashIndex.length)) {
      return 0;
    } else {
      return hashIndex[hash];
    }
  }

  public int hashGetChainLen(int hash) {
    if ((hashChainLen == null) || (hash < 0) || (hash >= hashChainLen.length)) {
      return 0;
    } else {
      return hashChainLen[hash];
    }
  }

  public void hashOptimize() {
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

    if (hashBits != bits) {
      hashSet(bits);
    }
  }

  private void hashSet(int bits) {
    // установка хеша (с полным пересчетом)

    if ((bits < 0) || (bits == 1) || (bits > 24)) {
      throw new LogicTreeException("Invalid hash dimension: bits=" + bits);
    }

    hashBits = bits;

    if (bits == 0) {
      hashIndex = null;
      hashChainLen = null;
      hashPrev = null;
    } else {
      hashPrev = utilCreArray(hashPrev, aRel1.length);
      hashIndex = utilCreArray(hashIndex, 1 << bits);
      hashChainLen = utilCreArray(hashChainLen, hashIndex.length);

      for (int i = 0; i < hashIndex.length; i++) {
        hashIndex[i] = -1;
        hashChainLen[i] = 0;
      }

      for (int i = 0; i < nArgs1; i++) {
        hashPrev[i] = -1;
      }
      for (int i = nArgs1; i < len; i++) {
        nodeSetHash(i);
      }
    }
  }

  public void optimizeParams() {
    hashOptimize();
  }

  public int nodeGetLastUsed1(int node) {
    if ((node < 0) || (node >= len)) {
      throw new LogicTreeException("No such node:" + node);
    }
    return lastUsed1[node];
  }

  public int nodeGetLastUsed2(int node) {
    if ((node < 0) || (node >= len)) {
      throw new LogicTreeException("No such node:" + node);
    }
    return lastUsed2[node];
  }

  public int nodeGetPrevUsed1(int node) {
    if ((node < 0) || (node >= len)) {
      throw new LogicTreeException("No such node:" + node);
    }
    return prevUsed1[node];
  }

  public int nodeGetPrevUsed2(int node) {
    if ((node < 0) || (node >= len)) {
      throw new LogicTreeException("No such node:" + node);
    }
    return prevUsed2[node];
  }
}
