package jsasha.lt.test;

import jsasha.lt.LogicTree;

/**
 * проверка сортировки списков
 * 
 */
public class Test2 {

  private int[] rels1;
  private int[] rels2;
  private int len;
  private int args;
  private int one;

  public void run(LogicTree lt) {
    len = lt.getLen();
    args = lt.getArgs();
    one = lt.one;

    rels1 = lt.arrayRels1();
    rels2 = lt.arrayRels2();

    int err1 = 0, err2 = 0;
    int err1node = -1, err2node = -1;
    for (int i = one; i < len; i++) {
      if ((rels2[i] > 0) && (rels2[i] <= args)) {
        err1++;
        if (err1node == -1) {
          err1node = i;
        }
      }
      if ((rels2[i] > args) && (rels1[i] <= rels1[rels2[i]])) {
        err2++;
        if (err2node == -1) {
          err2node = i;
        }
      }
    }

    if ((err1 == 0) && (err2 == 0)) {
      System.out.println("no sorting errors");
    } else {
      if (err1 > 0) {
        System.out.println("invalid list terminator: " + err1 + " errors; first err node: " + err1node);
      }
      if (err2 > 0) {
        System.out.println("invalid sequence in list: " + err2 + " errors; first err node: " + err2node);
      }
    }
  }
}
