package jsasha.util;

import java.util.Arrays;

public class ListInt {

  private int[] ar;
  private int len = 0;
  private int lazyLimit = 32;

  public ListInt(int size) {
    if (size < 1) {
      size = 1;
    }
    ar = new int[size];
  }

  public ListInt() {
    this(2);
  }

  public void add(int n) {
    checkSize(1);
    ar[len] = n;
    len++;
  }

  public void add(ListInt nn) {
    checkSize(nn.len);
    System.arraycopy(nn.ar, 0, ar, len, nn.len);
    len += nn.len;
  }

  public void addLazySorted(int n) {
    add(n);
    if (len > lazyLimit) {
      sort();
      if (len > lazyLimit) {
        lazyLimit *= 2;
      }
    }
  }

  public void addSorted(ListInt nn) {
    int[] b = new int[len + nn.len];
    System.arraycopy(ar, 0, b, 0, len);
    System.arraycopy(nn.ar, 0, b, len, nn.len);
    Arrays.sort(b);
    checkSize(nn.len);
    int prev = 0;
    len = 0;
    for (int i = 0; i < b.length; i++) {
      if ((i == 0) || (b[i] != prev)) {
        ar[len] = b[i];
        len++;
      }
      prev = b[i];
    }
  }

  public void sort() {
    // сортировка и устранение повторов
    if (len > 1) {
      int[] b = Arrays.copyOf(ar, len);
      Arrays.sort(b);
      int prev = 0;
      len = 0;
      for (int i = 0; i < b.length; i++) {
        if ((i == 0) || (b[i] != prev)) {
          ar[len] = b[i];
          len++;
        }
        prev = b[i];
      }
    }
  }

  public int get(int i) {
    if (i >= len) {
      i = -1;
    }
    return ar[i];
  }

  public int pop() {
    len--;
    return ar[len];
  }

  public void delete(int i) {
    if (i == (len - 1)) {
      len--;
    } else {
      System.arraycopy(ar, i + 1, ar, i, len - i - 1);
    }
  }

  private void checkSize(int addLen) {
    if ((len + addLen) > ar.length) {
      if (addLen == 1) {
        ar = Arrays.copyOf(ar, ar.length * 2);
      } else {
        ar = Arrays.copyOf(ar, len + addLen);
      }
    }
  }

  public int getLen() {
    return len;
  }

  public void setLen(int len) {
    this.len = len;
  }

  public int[] getAr() {
    return ar;
  }
}
