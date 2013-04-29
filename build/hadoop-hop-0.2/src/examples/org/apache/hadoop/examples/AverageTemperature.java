package org.apache.hadoop.examples;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.FileOutputFormat;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reducer;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class AverageTemperature extends Configured implements Tool {
	
	public static class TemperatureMap<K> extends MapReduceBase 
	implements Mapper<LongWritable, Text, Text, IntWritable>{
		private static final int MISSING = 9999;
		public void map(LongWritable key, Text value, OutputCollector<Text, 
				IntWritable> output, Reporter reporter) throws IOException{
			String line = value.toString();
			String year = line.substring(15,19);
			int airTemperature;
			if (line.charAt(87) == '+')
				airTemperature = Integer.parseInt(line.substring(88,92));
			else
				airTemperature = Integer.parseInt(line.substring(87,92));
			String quality = line.substring(92,93);
			if (airTemperature != MISSING && quality.matches("[01459]")){
				output.collect(new Text(year), new IntWritable(airTemperature));
			}
		}
		
	}
	
	public static class TemperatureReducer extends MapReduceBase 
	implements Reducer<Text, IntWritable, Text, IntWritable>{
		public void reduce(Text key, Iterator<IntWritable> values, OutputCollector<Text, 
				IntWritable> output, Reporter reporter) throws IOException{
			int sum = 0;
			int counter = 0;
			while(values.hasNext()){
				sum += values.next().get();
				counter++;
			}
			int averageTemp = sum/counter;
			output.collect(key, new IntWritable(averageTemp));
		}
	}
	
	private int printUsage() {
		System.out.println("avgtemperature [-s <frequency>] [-p] <inputDir> <outputDir>");
		ToolRunner.printGenericCommandUsage(System.out);
		return 1;
	}
	
	public int run(String [] args) throws Exception {
		JobConf conf = new JobConf(getConf(), AverageTemperature.class);
		conf.setJobName("avgtemp");
		
		conf.setOutputKeyClass(Text.class);
		conf.setOutputValueClass(IntWritable.class);
		
		conf.setMapperClass(TemperatureMap.class);
		conf.setReducerClass(TemperatureReducer.class);
		
		List<String> other_args = new ArrayList<String>();
		for (int i = 0; i < args.length; ++i) {
			try {
				if ("-s".equals(args[i])) {
		        	conf.setFloat("mapred.snapshot.frequency", Float.parseFloat(args[++i]));
		        	conf.setBoolean("mapred.map.pipeline", true);
				} else if ("-p".equals(args[i])) {
					conf.setBoolean("mapred.map.pipeline", true);
				} else {
					other_args.add(args[i]);
				}
			} catch (NumberFormatException except) {
				System.out.println("ERROR: Integer expected instead of "
						+ args[i]);
				return printUsage();
			} catch (ArrayIndexOutOfBoundsException except) {
				System.out.println("ERROR: Required parameter missing from "
						+ args[i - 1]);
				return printUsage();
			}
		}
		
		if (other_args.size() != 2) {
			System.out.println("ERROR: Wrong number of parameters: "
					+ other_args.size() + " instead of 2.");
			return printUsage();
		}
		FileInputFormat.addInputPath(conf, new Path(other_args.get(0)));
		FileOutputFormat.setOutputPath(conf, new Path(other_args.get(1)));
		JobClient.runJob(conf);
		return 0;
	}
	
	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new AverageTemperature(), args);
		System.exit(res);
	}
}
