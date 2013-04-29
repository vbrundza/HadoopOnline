/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.mapred;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.apache.hadoop.util.PairStorage;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * Treats keys as offset in file and value as line. 
 */
public class LineRecordReader implements RecordReader<LongWritable, Text> {
  private static final Log LOG
    = LogFactory.getLog(LineRecordReader.class.getName());

  private CompressionCodecFactory compressionCodecs = null;
  private long start;
  private long pos;
  private long end;
  private LineReader in;
  private PairStorage<Text> dataStorage;
  int maxLineLength;
  private boolean shuffle = false; //Added @VB
  private static final boolean SHUFFLING_DISABLED = false;   //@VB
  private boolean shuffleStatus = false;  //@VB for shuffle check 

  /**
   * A class that provides a line reader from an input stream.
   * @deprecated Use {@link org.apache.hadoop.util.LineReader} instead.
   */
  @Deprecated
  public static class LineReader extends org.apache.hadoop.util.LineReader {
    LineReader(InputStream in) {
      super(in);
    }
    LineReader(InputStream in, int bufferSize) {
      super(in, bufferSize);
    }
    public LineReader(InputStream in, Configuration conf) throws IOException {
      super(in, conf);
    }
  }

  public LineRecordReader(Configuration job, 
                          FileSplit split) throws IOException {
    this.maxLineLength = job.getInt("mapred.linerecordreader.maxlength",
                                    Integer.MAX_VALUE);
    start = split.getStart();
    end = start + split.getLength();
    final Path file = split.getPath();
    compressionCodecs = new CompressionCodecFactory(job);
    final CompressionCodec codec = compressionCodecs.getCodec(file);

    // open the file and seek to the start of the split
    FileSystem fs = file.getFileSystem(job);
    FSDataInputStream fileIn = fs.open(split.getPath());
    boolean skipFirstLine = false;
    if (codec != null) {
      in = new LineReader(codec.createInputStream(fileIn), job);
      end = Long.MAX_VALUE;
    } else {
      if (start != 0) {
        skipFirstLine = true;
        --start;
        fileIn.seek(start);
      }
      in = new LineReader(fileIn, job);
    }
    if (skipFirstLine) {  // skip first line and re-establish "start".
      start += in.readLine(new Text(), 0,
                           (int)Math.min((long)Integer.MAX_VALUE, end - start));
    }
    this.pos = start;
    this.shuffle = job.getBoolean("io.file.shuffle", SHUFFLING_DISABLED);
    if (shuffle) readAllLines();
    this.pos = start;
  }
  
  public LineRecordReader(InputStream in, long offset, long endOffset,
                          int maxLineLength) {
    this.maxLineLength = maxLineLength;
    this.in = new LineReader(in);
    this.start = offset;
    this.pos = offset;
    this.end = endOffset;
    this.shuffle = false;
  }

  public LineRecordReader(InputStream in, long offset, long endOffset, 
                          Configuration job) 
    throws IOException{
    this.maxLineLength = job.getInt("mapred.linerecordreader.maxlength",
                                    Integer.MAX_VALUE);
    this.in = new LineReader(in, job);
    this.start = offset;
    this.pos = offset;
    this.end = endOffset; 
    this.shuffle = job.getBoolean("io.file.shuffle", SHUFFLING_DISABLED);
    if (shuffle) readAllLines();
    this.pos = offset;
  }
  
  public LongWritable createKey() {
    return new LongWritable();
  }
  
  public Text createValue() {
    return new Text();
  }
  
  /** Read a line. */
  public synchronized boolean next(LongWritable key, Text value)
    throws IOException {
	  
	if(shuffle) {
		  key.set(pos);
		  value.clear();
		  Text temp = dataStorage.next();
		  if (temp != null){
			  value.append(temp.getBytes(), 0, temp.getLength());
			  pos += value.getLength();
			  return true;	  
		  } else {
			  //dataStorage = null;  //delete reference for GC to finish the work of cleaning
			  return false;
		  }
		  
		//return nextLocal(key, value);
	}
	else{
	    while (pos < end) {
	      key.set(pos);
	
	      int newSize = in.readLine(value, maxLineLength,
	                                Math.max((int)Math.min(Integer.MAX_VALUE, end-pos),
	                                         maxLineLength));
	      if (newSize == 0) {
	        return false;
	      }
	      pos += newSize;
	      if (newSize < maxLineLength) {
	        return true;
	      }
	
	      // line too long. try again
	      LOG.info("Skipped line of size " + newSize + " at pos " + (pos - newSize));
	    }
	
	    return false;
	}
  }
  
  /**
   * Get all the records from input and store them in a temporary data structure
   */
  
  private synchronized void readAllLines() throws IOException{
	  long tempPos = pos;
	  Text value = new Text();
	  if (shuffleStatus == false){
		  dataStorage = new PairStorage<Text>();
		  //ArrayList<Text> value = new ArrayList<Text>();
		  while(tempPos < end){
			  //value.add(index, new Text());
			  int newSize = in.readLine(value, maxLineLength, 
					  					Math.max((int)Math.min(Integer.MAX_VALUE, end-tempPos),
	                                         maxLineLength));
			  if (newSize == 0) break;
			  tempPos += newSize;
			  if (newSize < maxLineLength){
				  dataStorage.put(new Text(value));
			  }
			  else {
				  LOG.info("Skipped line of size " + newSize + " at pos " + (tempPos - newSize));
			  }
		  }
		  dataStorage.sort();
		  shuffleStatus = true;
	  }
  }
  
  /**
   * Read a line from a temporary data structure
   */
  
  public synchronized boolean nextLocal(LongWritable key, Text value) throws IOException{
	  key.set(pos);
	  value.clear();
	  value = dataStorage.next();
	  if (value != null){
		  pos += value.getLength();
		  return true;	  
	  } else {
		  //dataStorage = null;  //delete reference for GC to finish the work of cleaning
		  return false;
	  }
  }
  
  

  /**
   * Get the progress within the split
   */
  public float getProgress() {
    if (start == end) {
      return 0.0f;
    } else {
      return Math.min(1.0f, (pos - start) / (float)(end - start));
    }
  }
  
  public  synchronized long getPos() throws IOException {
    return pos;
  }

  public synchronized void close() throws IOException {
    if (in != null) {
      in.close(); 
    }
  }
}
