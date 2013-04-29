package org.apache.hadoop.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import org.apache.hadoop.io.Text;


public class DataShuffler {
	private ArrayList<Pair<Text>> swapPairList;

	
	@SuppressWarnings("unchecked")
	public ArrayList<Pair<Text>> dataShuffler(ArrayList<Pair<Text>> data){
		this.swapPairList = data;
		quickSort(0, data.size()-1);
		//return (new LinkedList(Arrays.asList(data)));
		return data;
	}
	
	/*
	 *  Quicksort for array list
	 */
	private void quickSort(int left, int right){
		int pivotValue = swapPairList.get(((left+right)/2)).getFirst();
		int i = left;
		int j = right;
		while (i <= j){
			while (swapPairList.get(i).getFirst() < pivotValue) i++;
			while (swapPairList.get(j).getFirst() > pivotValue) j--;
			if (i <= j){
				swapListValues(i,j);
				i++;
				j--;
			}
		}
		//sort remaining
		if (left < j) quickSort(left, j);
		if (right > i) quickSort(i, right);
	}

	
	private void swapListValues(int left, int right){
			Pair<Text> temp = swapPairList.get(left);
			swapPairList.set(left, swapPairList.get(right));
			swapPairList.set(right, temp);
	}
	

}
