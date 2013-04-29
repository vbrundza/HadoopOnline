package org.apache.hadoop.mapred;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.fs.BlockLocation;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.io.Text;

/*
 * Contains blocks information of input files
 * Number of input files is determined during runtime
 * Blocks are determined by start index {link #startoffset},
 * and lengths {link #lengths}
 * Author V. Brundza
 */

public class RandomFileSplit implements InputSplit{
	private Path[] files;
	private long[] startoffset;
	private long[] lengths;
	private long sumlength;
	private JobConf job;
	
	/**
	 * Default empty constructor
	 */
	RandomFileSplit() {};
	
	  /** Constructs a split.
	   * @param files the list of files 
	   * @param startoffset the position of the first byte in the each file to process
	   * @param lengths the number of bytes in the each file to process
	   * @param job job configuration
	   */
	RandomFileSplit(Path[] files, long[] startoffset, long[] lengths, JobConf job) {
		this.files = files;
		this.startoffset = startoffset;
		this.lengths = lengths;
		this.job = job;
		sumlength = 0;
		for(long length: lengths){
			sumlength += length;
		}
	}
	
	public long getLength() throws IOException{
		return sumlength;
	}
	
	/**
	 * Get the number of bytes to process in each file of the <code>RandomFileSplit</code>.
	 * 
	 * @return the array of bytes in the input split.
	 */
	public long[] getLengths(){
		return lengths;
	}
	
	/**
	 * Get the number of bytes to process in separate file of the <code>RandomFileSplit</code>.
	 * 
	 * @param index index of file as given in {@link #getFileIndex} 
	 * @return the number of bytes in file to process.
	 */
	public long getLength(int index){
		return lengths[index];
		
	}
	
	/**
	 * Get the start positions of each file to process in the <code>RandomFileSplit</code>.
	 * 
	 * @return the array of offset bytes in the input split.
	 */
	public long[] getOffsets(){
		return startoffset;
	}
	
	/**
	 * Get the start positions of separate file to process in the <code>RandomFileSplit</code>.
	 * 
	 * @param index index of file as given in {@link #getFileIndex}  
	 * @return the offset byte of file in the input split or -1 if index is out of bounds
	 */
	public long getOffset(int index){
		return ((index< startoffset.length)&&(index >= 0)) ? startoffset[index] : -1;
	}
	
	/**
	 * Get the list of files to process in the <code>RandomFileSplit</code>.
	 * 
	 * @return the array of paths of files in the input split.
	 */
	public Path[] getPaths(){
		return files;
	}
	
	/**
	 * Get the number of files to process in the <code>RandomFileSplit</code>.
	 * 
	 * @return the number of paths of files in the input split.
	 */
	public int getNumPaths(){
		return files.length;
	}
	
	/**
	 * Get the file to process in the <code>RandomFileSplit</code>.
	 * 
	 * @param index index of file as given in {@link #getFileIndex}
	 * @return file path in the input split or null if file does not exist.
	 */
	public Path getPath(int index){
		return ((index< files.length)&&(index >= 0)) ? files[index] : null;
	}
	
	/**
	 * Get the index of given file in the <code>RandomFileSplit</code>.
	 * 
	 * @param path path of file to search
	 * @return file index in the input split or -1 if file does not exist.
	 */
	public int getFileIndex(Path path){
		return Arrays.asList(files).indexOf(path);
	}
	
	public String[] getLocations() throws IOException {
		HashSet<String> hosts = new HashSet<String>();
		int index = 0;
		for (Path file : files){
			FileSystem fs = file.getFileSystem(job);
		    FileStatus status = fs.getFileStatus(file);
		    BlockLocation[] blkLocations = fs.getFileBlockLocations(status,
		    		startoffset[index], lengths[index++]);
		    if (blkLocations != null && blkLocations.length > 0) {
		        addToSet(hosts, blkLocations[0].getHosts());
		    }
		}
		return hosts.toArray(new String[hosts.size()]);
	}
	
	private void addToSet(Set<String> set, String[] array) {
		for(String s:array)
		    set.add(s); 
	}	

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeLong(sumlength);
		out.writeInt(files.length);
		for (Path file : files){
			Text.writeString(out, file.toString());
		}
		out.writeInt(startoffset.length);
		for (long offset : startoffset){
			out.writeLong(offset);
		}
		out.writeInt(lengths.length);
		for (long length : lengths){
			out.writeLong(length);
		}
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		sumlength = in.readLong();
		int arraySize = in.readInt();
		files = new Path[arraySize];
		for(int i=0; i<arraySize; i++){
			files[i] = new Path(Text.readString(in));
		}
		arraySize = in.readInt();
		startoffset = new long[arraySize];
		for (int i=0; i<arraySize; i++){
			startoffset[i] = in.readLong();
		}
		arraySize = in.readInt();
		lengths = new long[arraySize];
		for (int i=0; i<arraySize; i++){
			lengths[i] = in.readLong();
		}
	}
	
	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		for (int i=0; i < files.length; i++){
			sb.append(files[i].toUri().getPath() + ": Offset: " + startoffset[i] + " : Length: " +lengths[i]);
		    if (i < files.length -1) {
		    	sb.append("\n");
		    }
		}
		return sb.toString();
	}

}
