package jsasha.old.ramsql;

public class Symbols {

  final public static char[] names = { // имена символов (односимвольные)
    '1', 'a', 'b', 'c', 'd',
    'e', 'f', 'g', 'h', 'i',
    'j', 'k', 'l', 'm', 'n'
  };
  final public static String[] btS = { // битовое представление символов
    "00", "010101", "01011", "011001", "01101",
    "0111", "100101", "10011", "101001", "10101",
    "1011", "110001", "11001", "1101", "111"
  };
  final public static String[] lst = { // списковое представление символов
    "(0)", "(0000)", "(001)", "(010)", "(0(00))",
    "(0(1))", "(100)", "(11)", "((00)0)", "((000))",
    "((01))", "((1)0)", "((10))", "(((00)))", "(((1)))"
  };

  public static Message getSymMsg(char name) {
    for (int i = 0; i < names.length; i++) {
      if (names[i] == name) {
        Message ret = new Message(4);
        String lst2 = lst[i];
        lst2 = lst2.substring(1, lst2.length() - 1);
        ret.addSimpleList(new SimpleList(lst2));
        return ret;
      }
    }

    throw new SimpleListException("Unknown name: " + name);
  }

  public static int addSymbol(Message msg, char name){
    // помещение символа на трак

    for (int i = 0; i < names.length; i++) {
      if (names[i] == name) {
        String lst2 = lst[i];
        lst2 = lst2.substring(1, lst2.length() - 1);
        return msg.addSimpleList(new SimpleList(lst2));
      }
    }

    throw new SimpleListException("Unknown name: " + name);
  }
}
