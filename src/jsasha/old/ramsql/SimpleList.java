package jsasha.old.ramsql;

public class SimpleList {

  private SimpleList rel1;
  private SimpleList rel2;
  public int note;

  // создание пустого списка
  public SimpleList() {
    rel1 = null;
    rel2 = null;
  }

  // создание списка
  public SimpleList(SimpleList a_rel1, SimpleList a_rel2) {
    rel1 = a_rel1;
    rel2 = a_rel2;
  }

  public SimpleList getRel1() {
    return rel1;
  }

  public SimpleList getRel2() {
    return rel2;
  }

  @Override
  public String toString() {
    String s1, s2;

    if (rel1 == null) {
      s1 = "0";
    } else {
      s1 = "(" + rel1.toString() + ")";
      for (int i = 0; i < Symbols.lst.length; i++) {
        if (s1.equalsIgnoreCase(Symbols.lst[i])) {
          s1 = Character.toString(Symbols.names[i]);
        }
      }
    }

    if (rel2 == null) {
      s2 = "";
    } else {
      s2 = rel2.toString();
    }

    return s1 + s2;
  }

  public SimpleList(String a_str) {
    // создание списка из строки

    int len;
    if (a_str == null) {
      len = 0;
    } else {
      len = a_str.length();
    }

    if (len == 0) {
      throw new SimpleListException("Cannot create list from empty string");
    }

    char[] ca = a_str.toCharArray();

    // удаляем неподходящие символы
    len = 0;
    for (int i = 0; i < ca.length; i++) {
      if ((ca[i] == '0') || (ca[i] == '1') || (ca[i] == '(') || (ca[i] == ')') || ((ca[i] >= 'a') && (ca[i] <= 'n'))) {
        if (i > len) {
          ca[len] = ca[i];
          ca[i] = '.';
        }
        len = len + 1;
      }
    }

    sListFromCharArray(ca, 0, len);
  }

  private SimpleList(char[] ca, int i0, int len) {
    sListFromCharArray(ca, i0, len);
  }

  private int findClose(char[] ca, int i0, int len) {
    int n = 1;
    int i = i0 + 1;
    while ((n > 0) && (i < i0 + len)) {
      switch (ca[i]) {
        case '(':
          n++;
          break;
        case ')':
          n--;
          break;
      }
      i++;
    }

    if (n == 0) {
      return i - i0;
    } else {
      throw new SimpleListException("Cannot find \")\" (\"" + String.valueOf(ca) + "\" " + i0 + "/" + len + ")");
    }
  }

  private void sListFromCharArray(char[] ca, int i0, int len) {
    int i0_2, len_2;

    if (len == 0) {
      throw new SimpleListException("Cannot create list from empty string (\"" + String.valueOf(ca) + "\" " + i0 + "/" + len + ")");
    }

    // создаем первый подсписок (rel1)
    switch (ca[i0]) {
      case '0':
        rel1 = null;
        i0_2 = i0 + 1;
        len_2 = len - 1;
        break;
//      case '1':
//        rel1 = new SimpleList();
//        i0_2 = i0 + 1;
//        len_2 = len - 1;
//        break;
      case '(':
        int len_1 = findClose(ca, i0, len) - 2;
        int i0_1 = i0 + 1;
        rel1 = new SimpleList(ca, i0_1, len_1);
        i0_2 = i0 + len_1 + 2;
        len_2 = len - len_1 - 2;
        break;
      default:
        rel1 = null;
        for (int j = 0; j < Symbols.names.length; j++) {
          if (ca[i0] == Symbols.names[j]) {
            rel1 = ListFromBitString(Symbols.btS[j]);
            break;
          }
        }

        if (rel1 == null) {
          throw new SimpleListException("Unexpected symbol (\"" + String.valueOf(ca) + "\" " + i0 + "/" + len + ")");
        }
        i0_2 = i0 + 1;
        len_2 = len - 1;
    }

    // создаем второй подсписок (rel2)
    if (len_2 < 0) {
      throw new SimpleListException("Unexpected end of list (\"" + String.valueOf(ca) + "\" " + i0 + "/" + len + ")");
    } else if (len_2 == 0) {
      rel2 = null;
    } else {
      rel2 = new SimpleList(ca, i0_2, len_2);
    }
  }

  public void ClearNote() {
    // рекурсивное удаление примечаний из списка

    note = 0;

    if (rel1 != null) {
      rel1.ClearNote();
    }
    if (rel2 != null) {
      rel2.ClearNote();
    }
  }

  public static SimpleList ListFromBitString(String bs) {
    if ((bs == null) || bs.isEmpty()) {
      return null;
    }

    // вычисляем число нулей, которые нужно добавить в конце
    int n0 = 0;
    int n1 = 0;
    int n = bs.length();
    for (int i = 0; i < n; i++) {
      if (bs.charAt(i) == '0') {
        n0++;
      } else {
        n1++;
      }
    }
    String str;
    if (n0 < (n1 + 2)) {
      n = n1 + 2 - n0; // число добавляемых нулей
      char[] add0 = new char[n];
      for (int i = 0; i < add0.length; i++) {
        add0[i] = '0';
      }
      str = bs + (new String(add0));
    } else {
      str = bs;
    }

    char[] ca = str.toCharArray();
    return ListFromBitString2(ca, 0, ca.length);
  }

  private static int GetBSlen(char[] bs, int start) {
    int n0 = 0;
    int n1 = 0;
    int i = start;

    while ((i < bs.length) && ((n0 - n1) != 2)) {
      if (bs[i] == '0') {
        n0++;
      } else {
        n1++;
      }
      i++;
    }

    if ((n0 - n1) == 2) {
      return i - start;
    } else {
      throw new SimpleListException("Cannot find valid bit string in \"" + String.valueOf(bs) + "\" from " + start);
    }
  }

  private static SimpleList ListFromBitString2(char[] bs, int start, int len) {
    if (len < 2) {
      throw new SimpleListException("Too short bit string (\"" + String.valueOf(bs) + "\" " + start + "/" + len + ")");
    }

    SimpleList lst1 = null;
    SimpleList lst2 = null;

    int start1, start2, len1, len2;

    if (bs[start] == '0') {
      start2 = start + 1;
      len2 = len - 1;
    } else {
      start1 = start + 1;
      len1 = GetBSlen(bs, start1);
      lst1 = ListFromBitString2(bs, start1, len1);
      start2 = start1 + len1;
      len2 = len - (start2 - start);
    }

    if (len < 1) {
      throw new SimpleListException("Too short bit string (\"" + String.valueOf(bs) + "\" " + start2 + "/" + len2 + ")");
    }

    int len22;

    if (bs[start2] == '0') {
      len22 = 1;
      if (len22 != len2) {
        start2 += len22;
        throw new SimpleListException("Unnessesary rest of bit string \"" + String.valueOf(bs) + "\" from " + start2);
      }
    } else {
      start2 += 1;
      len2 -= 1;
      len22 = GetBSlen(bs, start2);
      if (len22 != len2) {
        start2 += len22;
        throw new SimpleListException("Unnessesary rest of bit string \"" + String.valueOf(bs) + "\" from " + start2);
      }
      lst2 = ListFromBitString2(bs, start2, len2);
    }

    return new SimpleList(lst1, lst2);
  }

  public String toBitString(boolean doCondense) {
    if (doCondense) {
      String str = toBitString2();
      char[] c = str.toCharArray();
      int n = 0;
      for (int i = c.length - 1; i > 0; i--) {
        if (c[i] != '0') {
          n = c.length - i - 1;
          break;
        }
      }

      if (n == 0) {
        return str;
      } else {
        return new String(c, 0, c.length - n);
      }
    } else {
      return toBitString2();
    }
  }

  private String toBitString2() {
    String ret;

    if (rel1 == null) {
      ret = "0";
    } else {
      ret = "1" + rel1.toBitString2();
    }

    if (rel2 == null) {
      ret = ret + "0";
    } else {
      ret = ret + "1" + rel2.toBitString2();
    }

    return ret;
  }

  public int cmp(SimpleList lst2) {
    // сравнение со списком lst2:
    //    1 - список больше lst2
    //    0 - список равен lst2
    //   -1 - список меньше lst2

    SimpleList r2 = lst2.rel1;

    if ((rel1 == null) && (r2 != null)) {
      return -1;
    } else if ((rel1 != null) && (r2 == null)) {
      return 1;
    } else if ((rel1 != null) && (r2 != null)) {
      int cmp1 = rel1.cmp(r2);
      if (cmp1 != 0) {
        return cmp1;
      }
    }

    // левые половинки списка равны, сравниваем правые
    r2 = lst2.rel2;

    if ((rel2 == null) && (r2 != null)) {
      return -1;
    } else if ((rel2 != null) && (r2 == null)) {
      return 1;
    } else if ((rel2 != null) && (r2 != null)) {
      return rel2.cmp(r2);
    } else {
      return 0;
    }
  }
}
