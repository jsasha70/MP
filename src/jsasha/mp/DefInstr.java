package jsasha.mp;

import java.io.Serializable;

public class DefInstr implements Serializable {

  public static final long serialVersionUID = 1;
  public int id;
  public int line; // номер строки в исходном тексте, начиная с 1
  public int pos; // номер позиции в строке (первый символ инструкции, начиная с 1)
  public int file; // номер файла, в котором инструкция
  public String text; // текст инструкции
  public char typ; // тип инструкции:
  //                    c - const (stack), v - var (stack), f - func (on stack),
  //                    e - exit, i - if, s - else, l - loop, ' ' - no op,
  //                    b - simple block
  public int bitsIn = 0; // число бит, снимаемые с трака или 0 (имеет смысл для v, f, i, s, l)
  public int bitsOut = 0; // число бит, помещаемые на трак (имеет смысл для c, v, f, i, s, l)
  public int varRelCont = 0; // относительный номер контекста (блока в стеке блоков) для переменной (для v)
  public int varId = 0; // номер (id) переменной (для v)
  public int funcId = 0; // номер вызываемой ф-ии (для f)
  public int blockId = 0; // номер (id) вызываемого блока (для b, i и l)
  public int repeat = 1; // число повторений (для l - loop N)
  public byte[] bits = null; // массив бит (для "c", кроме подчеркивания)

  @Override
  public String toString() {
    return text + " (" + file + "-" + line + "-" + pos + ")";
  }
}
