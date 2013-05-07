package org.apache.hadoop.mapred;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Pair;

/*
 * Creates randomized input splits for further processing @ Mappers
 * Input files are shuffled and given to getSplits method
 * where it is processed 
 * By default each RandomFileSplit consist data from 4 input files
 * This parameter can be set in job configuration: "io.split.maxsubsplit" [int]
 * Author: V. Brundza
 */


public abstract class RandomFileInputFormat<K, V> extends 
	FileInputFormat<K, V> {
	private FileSplit[] inputPaths;
	private int maxsubsplit;
	
	/**
	 * Splits files given by {@link #listStatus(JobConf)} into RandomFileSplits, which consist of 
	 * multiple (4 by default) paths 
	 */
	@Override
	public synchronized InputSplit[] getSplits(JobConf job, int numSplits)
	throws IOException{
		getInputPaths(job, numSplits);
		int processIndex = 0;
		maxsubsplit = job.getInt("io.split.maxsubsplit", 4);
		List<RandomFileSplit> splits = new ArrayList<RandomFileSplit>
						(Math.min(numSplits, inputPaths.length));

		while (processIndex < inputPaths.length){
			//get the number of splits to be sampled
			List<FileSplit> splitsToProcess = Arrays.asList(inputPaths).subList(processIndex, 
					processIndex+ maxsubsplit<inputPaths.length ? 
					(processIndex+maxsubsplit) : inputPaths.length);
		
			for (int i = 0; i < splitsToProcess.size(); i++){
				Path[] path = new Path[splitsToProcess.size()];
				long[] offsets = new long[splitsToProcess.size()];
				long[] lengths = new long[splitsToProcess.size()];
				int tmpIndex = 0;
				boolean largerSample;
				int numberOfSplits = splitsToProcess.size();
				
				for (FileSplit file : splitsToProcess){
					path[tmpIndex] = file.getPath();
					//determines if file can be split even or some of sub-samples will have to be larger
					largerSample = (i == 0) ? (file.getLength()%numberOfSplits != 0) : 
						((file.getStart()+file.getLength()-(offsets[i-1]+lengths[i-1]))%numberOfSplits != 0);
					
					//sets the offset and length of each sub-split
					if (largerSample) {
						offsets[tmpIndex] = file.getStart() + i + i* (file.getLength()/numberOfSplits);
						lengths[tmpIndex++] = (i+1 == numberOfSplits) 
								? (file.getLength() - (offsets[tmpIndex-1] - file.getStart()))
								: (1 + file.getLength()/numberOfSplits);	
					}else{
						offsets[tmpIndex] = file.getStart() + i* (file.getLength()/numberOfSplits);
						lengths[tmpIndex++] = (i+1 == numberOfSplits) 
								? (file.getLength() - (offsets[tmpIndex-1] - file.getStart())) 
								: (file.getLength()/numberOfSplits);
					}
				}
				splits.add(new RandomFileSplit(path, offsets, lengths, job));
			}
			processIndex += maxsubsplit;
		}
		return splits.toArray(new RandomFileSplit[splits.size()]);
	}
	

	@Override
	public abstract RecordReader<K, V> getRecordReader
	(InputSplit split,JobConf job, Reporter reporter) throws IOException;
	
	
	/** 
	 * Returns the input paths for processing
	 */
	private void getInputPaths(JobConf job, int numSplits) throws IOException{
		
		this.inputPaths = (FileSplit[]) super.getSplits(job, numSplits);
		boolean inputSort = job.getBoolean("io.split.insort", false);
		
		// shuffles input splits if set by job properties
		if (inputSort){
		ArrayList<Pair> temporaryStorage = new ArrayList<Pair>();
		Random rand = new Random(System.currentTimeMillis());
		FileSplit[] shuffledInput = new FileSplit[inputPaths.length];
		int index = 0;
		
		for (InputSplit file : inputPaths){
			 temporaryStorage.add(new Pair<InputSplit>
			 (rand.nextInt(Integer.MAX_VALUE), file)); 
		}
		
		//Perform sorting
		Collections.sort(temporaryStorage, new Comparator<Pair>(){
			public int compare(Pair pair1, Pair pair2){
				return (int)pair1.getFirst() - (int)pair2.getFirst();
			}
		});
		
		for (Pair storedFile: temporaryStorage){
			shuffledInput[index++] = (FileSplit) storedFile.getSecond();
		}
		this.inputPaths = shuffledInput;
	  }
	}
}
