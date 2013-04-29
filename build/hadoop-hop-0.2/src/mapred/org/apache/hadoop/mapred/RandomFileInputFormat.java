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
	private FileStatus[] inputFiles;
	private int maxsubsplit;
	
	/**
	 * Splits files given by {@link #listStatus(JobConf)} into RandomFileSplits, which consist of 
	 * multiple (4 by default) paths */
	
	@Override
	public synchronized InputSplit[] getSplits(JobConf job, int numSplits)
	throws IOException{
		maxsubsplit = job.getInt("io.split.maxsubsplit", 4);
		randomizeInput(job);
		List<RandomFileSplit> splits = new ArrayList<RandomFileSplit>
											(Math.min(numSplits, inputFiles.length));
		int processIndex = 0;
		while (processIndex < inputFiles.length){
			//get the number of splits to be sampled further
			List<FileStatus> splitsToProcess = Arrays.asList(inputFiles).subList(processIndex, 
					processIndex+maxsubsplit<inputFiles.length ? (processIndex+maxsubsplit) : inputFiles.length);
			
			//variables for creating RandomFileSplit

			
			for (int i = 0; i < splitsToProcess.size(); i++){
				Path[] path = new Path[splitsToProcess.size()];
				long[] offsets = new long[splitsToProcess.size()];
				long[] lengths = new long[splitsToProcess.size()];
				int tmpIndex = 0;
				boolean largerSample;
				int numberOfSplits = splitsToProcess.size();
				for (FileStatus file : splitsToProcess){
					path[tmpIndex] = file.getPath();
					//determines if file can be split even or some of sub-samples will have to be larger
					largerSample = (i == 0) ? (file.getLen()%numberOfSplits != 0) : 
						((file.getLen()-(offsets[i-1]+lengths[i-1]))%numberOfSplits != 0);
					//sets the offset and length of each sub-split
					if (largerSample) {
						offsets[tmpIndex] = i + i* (file.getLen()/numberOfSplits);
						lengths[tmpIndex++] = (i+1 == numberOfSplits) ? (file.getLen() - offsets[tmpIndex-1])
								: (1 + file.getLen()/numberOfSplits);	
					}else{
						offsets[tmpIndex] = i* (file.getLen()/numberOfSplits);
						lengths[tmpIndex++] = (i+1 == numberOfSplits) ? (file.getLen() - offsets[tmpIndex-1]) 
								: (file.getLen()/numberOfSplits);
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
	
	
	// Reads and shuffles the input files sequence in an array
	private void randomizeInput(JobConf job) throws IOException{
		ArrayList<Pair> temporaryStorage = new ArrayList<Pair>();
		inputFiles =  super.listStatus(job);
		Random rand = new Random(System.currentTimeMillis());
		
		//Put into Pair data structure with assigned random integer used for shuffling
		for (FileStatus file : inputFiles){
			 temporaryStorage.add(new Pair<FileStatus>
			 (rand.nextInt(Integer.MAX_VALUE), file)); 
		}
		  
		//Perform sorting
		Collections.sort(temporaryStorage, new Comparator<Pair>(){
			public int compare(Pair pair1, Pair pair2){
				return (int)pair1.getFirst() - (int)pair2.getFirst();
			}
		});
		  
		// Store list of files into an array
		FileStatus[] shuffledInput = new FileStatus[inputFiles.length];
		int index = 0;
		for (Pair storedFile: temporaryStorage){
			 shuffledInput[index++] = (FileStatus) storedFile.getSecond();
		}
		this.inputFiles = shuffledInput;
	  }
}
