package src;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;                            
import org.apache.hadoop.mapreduce.RecordReader;                                                                                                            
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

public class Filesrecord extends RecordReader<NullWritable, Text>
	{

		private FileSplit fileSplit;
		private Configuration conf;
		private Text value = new Text();
		private boolean processed = false;

		public void initialize(InputSplit arg0, TaskAttemptContext arg1) throws IOException, InterruptedException
			{
				// TODO Auto-generated method stub
				this.fileSplit = (FileSplit) arg0;
				this.conf = arg1.getConfiguration();
			}

		public NullWritable getCurrentKey() throws IOException, InterruptedException
			{
				// TODO Auto-generated method stub
				return NullWritable.get();
			}

		public Text getCurrentValue() throws IOException, InterruptedException
			{
				// TODO Auto-generated method stub
				return value;
			}

		public boolean nextKeyValue() throws IOException, InterruptedException
			{
				// TODO Auto-generated method stub
				if (!processed)
					{
						byte[] contents = new byte[(int) fileSplit.getLength()];
						Path file = fileSplit.getPath();
						FileSystem fs = file.getFileSystem(conf);
						FSDataInputStream in = null;
						try
							{
								in = fs.open(file);
								IOUtils.readFully(in, contents, 0, contents.length);
								value.set(contents, 0, contents.length);
							}
						finally
							{
								IOUtils.closeStream(in);
							}
						processed = true;
						return true;
					}

				return false;
			}

		public float getProgress() throws IOException, InterruptedException
			{
				// TODO Auto-generated method stub
				return processed ? 1.0f : 0.0f;
			}

		public void close() throws IOException
			{
				// TODO Auto-generated method stub

			}
	}