package org.apache.hadoop.util;

import java.util.Comparator;

public class Pair<U> {

	private final Integer first;
	private final U second;
	
	public Pair(Integer first, U second){
		this.first=first;
		this.second=second;
	}
	
	public int getFirst(){
		return first;
	}
	
	public U getSecond(){
		return second;
	}
	
	@Override
	public String toString(){
		return (first + " " + second.toString());
	}
	
	public class PairComparator implements Comparator<Pair>{
		public int compare(Pair pair1, Pair pair2){
			return (int) pair1.getFirst() - (int) pair2.getFirst();
		}
	}
	
}
