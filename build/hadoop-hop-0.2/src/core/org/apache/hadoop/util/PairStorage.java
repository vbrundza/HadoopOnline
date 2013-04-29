package org.apache.hadoop.util;

import java.util.ArrayList;
import java.util.Random;

import org.apache.hadoop.io.Text;


public class PairStorage<T> implements IndexedSortable{
private ArrayList<Text> linesStorage;
private Integer[] temporaryIndexes;
private Random rand;
private final int start = 0;
private int accessedIndexes = 0;

public PairStorage() {
	linesStorage = new ArrayList<Text>();
	rand = new Random(System.currentTimeMillis());
}

public void trimSize(){
	linesStorage.trimToSize();
}

public void put(Text value){
	linesStorage.add(value);
}

public Text get(int index){
	return linesStorage.get(index);
}

public Text next(){
	if (accessedIndexes < this.getLength()) {
		return this.get(accessedIndexes++);
		} else {
		accessedIndexes = 0;
		return null;
		}
}

public int compare(int i, int j){
	return (temporaryIndexes[i] - temporaryIndexes[j]);
}

public void swap (int i, int j){
	Text temp = linesStorage.get(i);
	linesStorage.set(i, linesStorage.get(j));
	linesStorage.set(j, temp);
	
	Integer tempValue = temporaryIndexes[i];
	temporaryIndexes[i] = temporaryIndexes[j];
	temporaryIndexes[j] = tempValue;	
}

public int getLength(){
	return linesStorage.size();
}

public void sort(){
	QuickSort sorter = new QuickSort();
	temporaryIndexes = new Integer[linesStorage.size()];
	for (int i = 0; i < linesStorage.size(); i++){
		temporaryIndexes[i] = rand.nextInt(Integer.MAX_VALUE);
	}
	sorter.sort(this, start, this.getLength());
	//let GC clean the array
	temporaryIndexes = null;
}
}
