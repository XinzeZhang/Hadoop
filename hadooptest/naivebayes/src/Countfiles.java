package src;

import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class Countfiles
	{
		public static class BayesTrainMapper extends Mapper<NullWritable, Text, Text, Text>
			{
				private Text filenameKey;

				@Override
				protected void setup(Context context) throws IOException, InterruptedException
					{
						// TODO Auto-generated method stub
						InputSplit split = context.getInputSplit();
						Path path = ((FileSplit) split).getPath();// ��split�л�ȡ�ļ�·��
						// ��ȡ�ļ��ĸ�Ŀ¼����Ϊ�����key
						String pathString = path.toString();// �����ڶ���/�����Ŀ¼����Ϊ�����
						String string = pathString.substring(0, pathString.lastIndexOf("/"));
						String typename = string.substring(string.lastIndexOf("/") + 1);
						filenameKey = new Text(typename);
					}

				@Override
				protected void map(NullWritable key, Text value, Context context)
						throws IOException, InterruptedException
					{
						// TODO Auto-generated method stub
						context.write(filenameKey, new Text("1"));
					}
			}

		public static class BayesTrainReducer extends Reducer<Text, Text, Text, Text>
			{
				@Override
				protected void reduce(Text arg0, Iterable<Text> arg1, Context arg2)
						throws IOException, InterruptedException
					{
						// TODO Auto-generated method stub
						Long numsLong = 0L;
						for (@SuppressWarnings("unused")
						Text text : arg1)
							{
								numsLong++;
							}
						arg2.write(arg0, new Text(numsLong + ""));
					}
			}

		public static void main(String[] args) throws Exception
			{
				Configuration conf = new Configuration();
				String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
				if (otherArgs.length < 2)
					{
						System.err.println("Usage: wordcount <in> [<in>...] <out>");
						System.exit(2);
					}
				Job job = Job.getInstance(conf, "Naivebayes Classifier");
				job.setJarByClass(Countfiles.class);
				job.setInputFormatClass(Filesinput.class);
				job.setOutputKeyClass(Text.class);
				job.setOutputValueClass(Text.class);
				job.setMapperClass(BayesTrainMapper.class);
				job.setReducerClass(BayesTrainReducer.class);
				for (int i = 0; i < otherArgs.length - 1; ++i)
					{
						FileInputFormat.addInputPath(job, new Path(otherArgs[i]));
					}
				FileOutputFormat.setOutputPath(job, new Path(otherArgs[otherArgs.length - 1]));

				System.exit(job.waitForCompletion(true) ? 0 : 1);

			}
	}
