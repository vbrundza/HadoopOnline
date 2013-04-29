package org.apache.hadoop.mapred;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.LineRecordReader.LineReader;
import org.apache.hadoop.util.PairStorage;

/*
 * Reads lines from RandomFileSplit
 * Author: V. Brundza
 */


/**
 * Treats keys as offset in file and value as line.
 * {@link #RandomFileSplit}  is sampled from the multiple inputs 
 */

@SuppressWarnings("deprecation")
public class RandLineRecordReader implements RecordReader<LongWritable, Text> {
	private static final Log LOG
	  = LogFactory.getLog(RandLineRecordReader.class.getName());

	private long readBytes;		//how much read up till now
	private long start;
	private long end;
	private int noOfSubsplits;
	private FileSystem fs;
	private int maxLineLength;
	private Path[] paths;
	private long[] startPositions;
	private long[] filesLength;
	private org.apache.hadoop.util.LineReader in;
	private boolean shuffle = false;
//	private boolean shuffleStatus = false;
	private static final boolean SHUFFLING_DISABLED = false;
	private PairStorage<Text> dataStorage;
	private Configuration job;
	private long totalInputBytes = 0;
    
	public RandLineRecordReader(Configuration job, RandomFileSplit split) 
    		throws IOException{
    	fs = FileSystem.get(job);
    	this.paths = split.getPaths();
    	this.job = job;
    	this.readBytes = 0;
    	this.startPositions = split.getOffsets();
    	this.filesLength = split.getLengths();
        this.maxLineLength = job.getInt("mapred.linerecordreader.maxlength",
                Integer.MAX_VALUE);
        this.noOfSubsplits = split.getNumPaths();
        this.shuffle = job.getBoolean("io.file.shuffle", SHUFFLING_DISABLED);
        readAllLines();
    }
	
	/** Read a line. */
	@Override
	public synchronized boolean next(LongWritable key, Text value) throws IOException {
		key.set(readBytes);
		value.clear();
		Text temp = dataStorage.next();
		if (temp != null){
			value.append(temp.getBytes(), 0, temp.getLength());
			readBytes += value.getLength();
			return true;
		} else {
			return false;
		}
	}

	@Override
	public LongWritable createKey() {
		return new LongWritable();
	}

	@Override
	public Text createValue() {
		return new Text();
	}

	@Override
	public synchronized long getPos() throws IOException {
		//long currentPos = currentStream == null ? 0 : (currentStream.getPos() - startPositions[count]);
		return readBytes;
	}

	@Override
	public void close() throws IOException {
	  if (in != null) {
		  in.close(); 
      }
	}

	/**
	* Get the progress within the split
	*/
	@Override
	public float getProgress() throws IOException {
		return Math.min(1.0f, ((float)getPos()) / totalInputBytes);
	}
	
	/**
	* Get all the records from input and store them in a temporary data structure
	*/
	private synchronized void readAllLines() throws IOException{
	  long totalReadBytes = 0;
	  Text value = new Text();
		  dataStorage = new PairStorage<Text>();
		  //process every sub-split
			  for(int i=0; i<noOfSubsplits; i++){
				  long tempPos;
			      boolean skipFirstLine = false;
				  //get the information of split
				  Path file = paths[i];
			      FSDataInputStream inFile = fs.open(file);
			      start = startPositions[i];
			      end = startPositions[i] + filesLength[i]; 
			      
			      //if offset is not 0, skip the first line, as it might be not complete 
			      if (start != 0){
			    	skipFirstLine = true;
			    	inFile.seek(start);
			      }

			      in = new LineReader(inFile, job);
			      if (skipFirstLine) {  // skip first line and re-establish "start".
				      long consumedBytes = in.readLine(new Text(), 0,
				                              (int)Math.min((long)Integer.MAX_VALUE, end-start));
				      start += consumedBytes;
				      totalReadBytes += consumedBytes;
			      }
			      tempPos = start;
			      while (tempPos < end){
			    	  int readBytes = in.readLine(value, maxLineLength, 
							  					Math.max((int)Math.min(Integer.MAX_VALUE, end-tempPos),
			                                         maxLineLength));
					  if (readBytes == 0) break;
					  tempPos += readBytes;
					  totalReadBytes += readBytes;
					  
					  if (readBytes < maxLineLength){
						  dataStorage.put(new Text(value));
					  }
					  else {
						  LOG.info("Skipped line of size " + readBytes + " at pos "
								  + (tempPos - readBytes));
					  }
					  
			      }
			     
			      //if next mapper will skip full line, read it in advance
			      if (tempPos == end) {
				    readBytes = in.readLine(value, maxLineLength);
				    if (readBytes != 0) {
				    	dataStorage.put(new Text(value));
				    	totalReadBytes += readBytes;
				    }
			      }
			  }
		  //read bytes amount
		  totalInputBytes = totalReadBytes;
		  //if input have to be randomized
		  if (shuffle){
		  dataStorage.sort();
		  }
	}
}
