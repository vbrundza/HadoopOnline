package org.apache.hadoop.mapred;

/*
 * Author V. Brundza
 */

import java.io.IOException;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodecFactory;

/** An {@link InputFormat} for plain text files. Sampled files are broken into lines.
 * Either linefeed or carriage-return are used to signal end of line.  Keys are
 * the position in the file, and values are the line of text.. */

public class RandTextInputFormat extends RandomFileInputFormat<LongWritable, Text> 
	implements JobConfigurable{

	private CompressionCodecFactory compressionCodecs = null;
	
	@Override
	public void configure(JobConf conf) {
		compressionCodecs = new CompressionCodecFactory(conf);
	}

	@Override
	public RecordReader<LongWritable, Text> getRecordReader(InputSplit split,
			JobConf job, Reporter reporter) throws IOException {
		reporter.setStatus(split.toString());
		return new RandLineRecordReader(job, (RandomFileSplit) split);
	} 

}
