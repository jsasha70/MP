package jsasha.mp;

import java.math.BigInteger;
import java.util.Arrays;
import jsasha.lt.LToper;
import jsasha.lt.LogicTree;

public class LT_FuncStack {

  private int[] st = new int[1024];
  private int len = 0;

  public LT_FuncStack(int paramSize) {
    // заполнение стека начальнм параметром (вызов первой ф-ии)
    checkSize(paramSize);
    for (int i = 0; i < paramSize; i++) {
      st[i] = i + 1;
    }
    len = paramSize;
  }

  public LT_FuncStack(LT_FuncStack fs, int paramSize) {
    // заполнение стека параметром с предыдущего стека (вложенные вызовы ф-ий)
    checkSize(paramSize);
    System.arraycopy(fs.st, fs.len - paramSize, st, 0, paramSize);
    len = paramSize;
    fs.len -= paramSize;
  }

  public LT_FuncStack(LT_FuncStack fs) {
    // копия другого стека (для if)
    st = Arrays.copyOf(fs.st, fs.st.length);
    len = fs.len;
  }

  public void push(LT_Var v) {
    // помещаем переменную на стек
    checkSize(v.v.length);
    System.arraycopy(v.v, 0, st, len, v.v.length);
    len += v.v.length;
  }

  public void push(BigInteger c, int size, int one) {
    // помещаем константу на стек
    checkSize(size);
    for (int i = 0; i < size; i++) {
      st[len + size - i - 1] = c.testBit(i) ? one : 0;
    }
    len += size;
  }

  public void push(LT_FuncStack st2) {
    // помещаем стек на стек (для возврата из ф-ии)
    checkSize(st2.len);
    System.arraycopy(st2.st, 0, st, len, st2.len);
    len += st2.len;
  }

  public void pop(LT_Var v) {
    // получаем переменную со стека
    System.arraycopy(st, len - v.v.length, v.v, 0, v.v.length);
    len -= v.v.length;
  }

  public int popBit() {
    // получаем бит со стека
    len--;
    return st[len];
  }

  public void copy(LT_Var v) {
    // копируем переменную со стека (стек не меняется)
    System.arraycopy(st, len - v.v.length, v.v, 0, v.v.length);
  }

  public int getLen() {
    return len;
  }

  public void join(int cond, LT_FuncStack fs, LogicTree lt) {
    // объединение значений, зависящих от условия cond
    // если cond, то берем текущее значение,
    // а если не cond, то берем значение fs
    if (len != fs.len) {
      System.out.println("Error: FuncStack.join: length mismatch (" + len + " and " + fs.len + ")");
      throw new MpRunErr();
    }
    for (int i = 0; i < len; i++) {
      if (st[i] != fs.st[i]) {
        // st[i] = st[i] and cond or fs.st[i] and not cond
        st[i] = LToper.or(LToper.and(st[i], cond, lt), LToper.and(fs.st[i], LToper.not(cond, lt), lt), lt);
      }
    }
  }

  public int[] getAll() {
    return Arrays.copyOf(st, len);
  }

  private void checkSize(int size) {
    while ((len + size) > st.length) {
      st = Arrays.copyOf(st, st.length * 2);
    }
  }

  public int[] getStInternal() {
    return st;
  }

  public void decreaseLen(int size) {
    len -= size;
  }
}
