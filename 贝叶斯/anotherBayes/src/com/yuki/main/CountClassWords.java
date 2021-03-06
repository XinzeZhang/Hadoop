package com.yuki.main;

import java.io.IOException;
import java.util.StringTokenizer;

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

public class CountClassWords {

	public static class BayesTrainMapper extends
			Mapper<NullWritable, Text, Text, Text> {
		private Text filenameKey;

		@Override
		protected void setup(Context context) throws IOException,
				InterruptedException {
			// TODO Auto-generated method stub
			InputSplit split = context.getInputSplit();
			Path path = ((FileSplit) split).getPath();// 从split中获取文件路径
			// 获取文件的父目录名作为输入的key
			String pathString = path.toString();// 倒数第二个/后面的目录名即为类别名
			String string = pathString
					.substring(0, pathString.lastIndexOf("/"));
			String typename = string.substring(string.lastIndexOf("/") + 1);
			filenameKey = new Text(typename);
		}

		@Override
		protected void map(NullWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			StringTokenizer itr = new StringTokenizer(value.toString());// 分词
			while (itr.hasMoreTokens()) {
				String line = itr.nextToken();
				if (line.matches("[a-zA-Z]+")) { // 过滤掉数字开头或者无意义的词，只选择英文单词
					context.write(new Text(filenameKey + ":" + line), new Text(
							"1"));
				}
			}
		}
	}

	public static class BayesTrainReducer extends
			Reducer<Text, Text, Text, Text> {
		@Override
		protected void reduce(Text arg0, Iterable<Text> arg1, Context arg2)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			Long numsLong = 0L;
			for (Text text : arg1) {
				numsLong++;
			}
			arg2.write(arg0, new Text(numsLong + ""));
		}
	}

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args)
				.getRemainingArgs();
		if (otherArgs.length < 2) {
			System.err.println("Usage: wordcount <in> [<in>...] <out>");
			System.exit(2);
		}
		Job job = Job.getInstance(conf, "bayes train");
		job.setJarByClass(CountClassWords.class);
		job.setInputFormatClass(WholeFileInputFormat.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.setMapperClass(BayesTrainMapper.class);
		job.setReducerClass(BayesTrainReducer.class);
		for (int i = 0; i < otherArgs.length - 1; ++i) {
			FileInputFormat.addInputPath(job, new Path(otherArgs[i]));
		}
		FileOutputFormat.setOutputPath(job, new Path(
				otherArgs[otherArgs.length - 1]));

		System.exit(job.waitForCompletion(true) ? 0 : 1);

	}

}
