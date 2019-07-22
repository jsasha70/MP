package jsasha.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class FileUtils {

  public static String[] readLines(String fname) {
    return readLines(new File(fname));
  }

  public static String[] readLines(File file) {
    if (!file.exists() || !file.isFile()) {
      System.out.println("Error: file not exists: " + file.getAbsolutePath());
      return null;
    }

    BufferedReader is;
    try {
      is = new BufferedReader(new FileReader(file));
    } catch (FileNotFoundException e) {
      return null;
    }

    try {
      ArrayList<String> lst = new ArrayList(100);
      String s;
      while (true) {
        s = is.readLine();

        if (s == null) {
          break;
        } else {
          lst.add(s);
        }
      }
      return lst.toArray(new String[0]);
    } catch (IOException e) {
      System.out.println("Error: error reading file: " + file.getAbsolutePath());
      return null;
    } finally {
      try {
        is.close();
      } catch (Exception ee) {
      }
    }
  }

  public static boolean writeObject(Object o, File f) {
    // сохранение объекта в файл

    ObjectOutputStream oo = null;
    try {
      oo = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f, false), 32768));
    } catch (Exception e) {
      System.out.println("Error opening file " + f.getAbsolutePath() + ":");
      System.out.println(e.toString());
      return false;
    }

    try {
      oo.writeObject(o);
    } catch (Exception e) {
      System.out.println("Error writing file " + f.getAbsolutePath() + ":");
      System.out.println(e.toString());
      return false;
    } finally {
      try {
        oo.close();
      } catch (Exception ee) {
      }
    }

    return true;
  }

  public static Object readObject(File f, String objClassGetName) {
    // чтение объекта из файла

    ObjectInputStream oi = null;
    try {
      oi = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f), 32768));
    } catch (Exception e) {
      System.out.println("Error opening file " + f.getAbsolutePath() + ":");
      System.out.println(e.toString());
      return null;
    }

    Object ret = null;
    try {
      ret = oi.readObject();
    } catch (Exception e) {
      System.out.println("Error reading file " + f.getAbsolutePath() + ":");
      System.out.println(e.toString());
      return null;
    } finally {
      try {
        oi.close();
      } catch (Exception ee) {
      }
    }

    if ((objClassGetName != null) && !ret.getClass().getName().equals(objClassGetName)) {
      System.out.println("Error with file " + f.getAbsolutePath() + ":");
      System.out.println("data in the file is not \"" + objClassGetName
              + "\", but \"" + ret.getClass().getName() + "\"");
      return null;
    }

    return ret;
  }
}
