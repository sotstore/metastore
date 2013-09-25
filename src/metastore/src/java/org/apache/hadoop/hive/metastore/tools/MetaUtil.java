package org.apache.hadoop.hive.metastore.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class MetaUtil {

  public static<T> ArrayList<T> CollectionToArrayList(Collection<T> c){
    ArrayList<T> list = new ArrayList<T>();
    list.addAll(c);
    return list;
  }

  public static<T> HashSet<T> CollectionToHashSet(Collection<T> c){
    HashSet<T> set = new HashSet<T>();
    set.addAll(c);
    return set;
  }

}
