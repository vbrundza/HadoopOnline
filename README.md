The Hadoop Online (HOP) is a modified version of Hadoop
that supports pipelining between tasks and between jobs. For more
information on HOP, please see our website:

   http://code.google.com/p/hop/

To enable HOP-specific functionality (e.g. pipelining) below described settings should be used.

Enable pipelining between mappers and reducers:

[<code>mapred.map.pipeline = (boolean) true / false </code>]

Determine the frequency of reducers output. Can be set between 1 and
100 % (values of 0.01 and 1 accordingly):

[<code>mapred.snapshot.frequency = (float) 0.0 to 1.0</code>]

Enable input file shuffling for data bias reduction:

[<code>io.file.shuffle = (boolean) true / false</code>]

Set up the block level sampling rate (the number of files each sample
should consist of).
<ul>
<li>Default value = 4 </li> 
<li>Maximum value = total number of input files </li> 
<li>Minimum value = 1 (no sampling) </li>
</ul>

[<code>io.split.maxsubsplit = (int) 1 to noOfInputFiles</code>]

NOTICE:
To enable block level sampling jobs must be configured to use specific
input format: RandTextInputFormat.class It can be set as following:
[<code>JobConf.setInputFormat(RandTextInputFormat.class)</code>]
One of examples (topkwordcount) does have it set by default, so can
be executed without recompiling.

Shuffle input file splits before sampling:

[<code>io.split.insort = (boolean) true / false</code>]

NOTICE: 
Works only if RandTextInputFormat is set for a job.
