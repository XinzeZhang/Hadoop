package com.yuki.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import junit.framework.Test;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
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

public class TestFilesClass {

	private static String[] typeName = new String[] { "CHINA", "USA" };// 类别名称
	private static String end_data = "hdfs://Master:9000/user/hadoop/out/CountClassFilesOut/end_data";

	private static Map<String, Double> rates = new HashMap<String, Double>();// 将训练和计算后的结果文档写入一个hashmap中
	private static double[] priors = new double[] { 0L, 0L };// 先验概率
	private static double[] likehood0 = new double[] { 0L, 0L }; // 当测试文档中出现的单词字典中没有时，+1平滑

	public static class BayesTestMap extends
			Mapper<NullWritable, Text, Text, Text> {

		private Text filenameKey;// 文件名

		@Override
		protected void setup(Context context) throws IOException,
				InterruptedException {
			// TODO Auto-generated method stub
			InputSplit split = context.getInputSplit();
			Path path = ((FileSplit) split).getPath();// 从split中获取文件路径
			String string = path.toString();
			String typename = string.substring(string.lastIndexOf("/") + 1);// 获取文件名
			filenameKey = new Text(typename);
		}

		@Override
		protected void map(NullWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			double total_rates[] = new double[] { priors[0], priors[1] };// 存放该文档出现在各个类别中的概率
			StringTokenizer itr = new StringTokenizer(value.toString());
			while (itr.hasMoreTokens()) {
				String word = itr.nextToken();
				double rate0 = rates.get("CHINA" + ":" + word) == likehood0[0] ? 0
						: rates.get("CHINA" + ":" + word);
				double rate1 = rates.get("USA" + ":" + word) == null ? likehood0[1]
						: rates.get("USA" + ":" + word);
				// 累加该文档中各个单词的概率
				total_rates[0] += rate0;
				total_rates[1] += rate1;
			}
			context.write(filenameKey, new Text(
					(total_rates[0] > total_rates[1] ? typeName[0]
							: typeName[1])));
		}
	}

	public static class BayesTestReducer extends
			Reducer<Text, Text, Text, Text> {
		@Override
		protected void reduce(Text arg0, Iterable<Text> arg1, Context arg2)
				throws IOException, InterruptedException {
			// TODO Auto-generated method stub
			// 不做任何处理，之直接将map的结果写入文件中。
			arg2.write(arg0, arg1.iterator().next());
		}
	}

	// 从文档中读取数据，写入一张hashmap中，方便查找
	private static void readFromFile() {
		Configuration conf = new Configuration();
		FSDataInputStream fsr = null;
		BufferedReader bufferedReader = null;
		String lineTxt = null;
		try {
			FileSystem fs = FileSystem.get(URI.create(end_data), conf);
			fsr = fs.open(new Path(end_data));
			bufferedReader = new BufferedReader(new InputStreamReader(fsr));
			lineTxt = bufferedReader.readLine();
			String[] str = lineTxt.split("\t");
			// 文档首行写入的是各个类别的先验概率和没有出现在训练集中单词的概率
			likehood0 = new double[] { Double.parseDouble(str[0].trim()),
					Double.parseDouble(str[1].trim()) };
			priors = new double[] { Double.parseDouble(str[2].trim()),
					Double.parseDouble(str[3].trim()) };
			// 第二行开始逐个读取各个单词的概率
			while ((lineTxt = bufferedReader.readLine()) != null) {

				String[] className = lineTxt.split(":");
				String[] wordName = className[1].split("\t");
				String classnameString = className[0];// 类别名
				String wordstString = wordName[0];// 单词
				rates.put(classnameString + ":" + wordstString,
						Double.parseDouble(wordName[1]));
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public static void main(String[] args) throws Exception {

		readFromFile();

		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args)
				.getRemainingArgs();
		if (otherArgs.length < 2) {
			System.err.println("Usage: wordcount <in> [<in>...] <out>");
			System.exit(2);
		}
		Job job = Job.getInstance(conf, "bayes train");
		job.setJarByClass(Test.class);
		job.setInputFormatClass(WholeFileInputFormat.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.setMapperClass(BayesTestMap.class);
		job.setReducerClass(BayesTestReducer.class);
		for (int i = 0; i < otherArgs.length - 1; ++i) {
			FileInputFormat.addInputPath(job, new Path(otherArgs[i]));
		}
		FileOutputFormat.setOutputPath(job, new Path(
				otherArgs[otherArgs.length - 1]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);

	}
}
