package src;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

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

public class Testsets
	{
		private static String[] typeName = new String[] { "UK", "USA" };// �������
		private static String end_data = "hdfs://xinze-ubuntu:9000/user/xinze/xinze-hadoop/output/MLE_data";

		private static Map<String, Double> rates = new HashMap<String, Double>();// ��ѵ���ͼ����Ľ���ĵ�д��һ��hashmap��
		private static double[] priors = new double[] { 0L, 0L };// �������
		private static double[] likehood0 = new double[] { 0L, 0L }; // �������ĵ��г��ֵĵ����ֵ���û��ʱ��+1ƽ��

		public static class BayesTestMap extends Mapper<NullWritable, Text, Text, Text>
			{

				private Text filenameKey;// �ļ���

				@Override
				protected void setup(Context context) throws IOException, InterruptedException
					{
						// TODO Auto-generated method stub
						InputSplit split = context.getInputSplit();
						Path path = ((FileSplit) split).getPath();// ��split�л�ȡ�ļ�·��
						String string = path.toString();
						String typename = string.substring(string.lastIndexOf("/") + 1);// ��ȡ�ļ���
						filenameKey = new Text(typename);
					}

				@Override
				protected void map(NullWritable key, Text value, Context context)
						throws IOException, InterruptedException
					{
						// TODO Auto-generated method stub
						double total_rates[] = new double[] { priors[0], priors[1] };// ��Ÿ��ĵ������ڸ�������еĸ���
						StringTokenizer itr = new StringTokenizer(value.toString());
						while (itr.hasMoreTokens())
							{
								String word = itr.nextToken();
//								double rate0 = rates.get("UK" + ":" + word) == likehood0[0] ? 0
//										: rates.get("UK" + ":" + word);
								double rate0 = rates.get("UK" + ":" + word) == null ? likehood0[1] 
										: rates.get("UK" + ":" + word);
								double rate1 = rates.get("USA" + ":" + word) == null ? likehood0[1]
										: rates.get("USA" + ":" + word);
								// �ۼӸ��ĵ��и������ʵĸ���
								total_rates[0] += rate0;
								total_rates[1] += rate1;
							}
						context.write(filenameKey,
								new Text((total_rates[0] > total_rates[1] ? typeName[0] : typeName[1])));
					}
			}

		public static class BayesTestReducer extends Reducer<Text, Text, Text, Text>
			{
				@Override
				protected void reduce(Text arg0, Iterable<Text> arg1, Context arg2)
						throws IOException, InterruptedException
					{
						// TODO Auto-generated method stub
						// �����κδ���ֱ֮�ӽ�map�Ľ��д���ļ��С�
						arg2.write(arg0, arg1.iterator().next());
					}
			}

		// ���ĵ��ж�ȡ���ݣ�д��һ��hashmap�У��������
		private static void readFromFile()
			{
				Configuration conf = new Configuration();
				FSDataInputStream fsr = null;
				BufferedReader bufferedReader = null;
				String lineTxt = null;
				try
					{
						FileSystem fs = FileSystem.get(URI.create(end_data), conf);
						fsr = fs.open(new Path(end_data));
						bufferedReader = new BufferedReader(new InputStreamReader(fsr));
						lineTxt = bufferedReader.readLine();
						String[] str = lineTxt.split("\t");
						// �ĵ�����д����Ǹ�������������ʺ�û�г�����ѵ�����е��ʵĸ���
						likehood0 = new double[] { Double.parseDouble(str[0].trim()),
								Double.parseDouble(str[1].trim()) };
						priors = new double[] { Double.parseDouble(str[2].trim()), Double.parseDouble(str[3].trim()) };
						// �ڶ��п�ʼ�����ȡ�������ʵĸ���
						while ((lineTxt = bufferedReader.readLine()) != null)
							{

								String[] className = lineTxt.split(":");
								String[] wordName = className[1].split("\t");
								String classnameString = className[0];// �����
								String wordstString = wordName[0];// ����
								rates.put(classnameString + ":" + wordstString, Double.parseDouble(wordName[1]));
							}

					}
				catch (Exception e)
					{
						e.printStackTrace();
					}
				finally
					{
						if (bufferedReader != null)
							{
								try
									{
										bufferedReader.close();
									}
								catch (IOException e)
									{
										e.printStackTrace();
									}
							}
					}

			}

		public static void main(String[] args) throws Exception
			{

				readFromFile();

				Configuration conf = new Configuration();
				conf.addResource(new Path("/home/xinze/Documents/hadoop-2.7.3/etc/hadoop/core-site.xml"));
				conf.addResource(new Path("/home/xinze/Documents/hadoop-2.7.3/etc/hadoop/hdfs-site.xml"));
				String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
				if (otherArgs.length < 2)
					{
						System.err.println("Usage: wordcount <in> [<in>...] <out>");
						System.exit(2);
					}
				Job job = Job.getInstance(conf, "Naivebayes Classifier");
				job.setJarByClass(Testsets.class);
				job.setInputFormatClass(Filesinput.class);
				job.setOutputKeyClass(Text.class);
				job.setOutputValueClass(Text.class);
				job.setMapperClass(BayesTestMap.class);
				job.setReducerClass(BayesTestReducer.class);
				for (int i = 0; i < otherArgs.length - 1; ++i)
					{
						FileInputFormat.addInputPath(job, new Path(otherArgs[i]));
					}
				FileOutputFormat.setOutputPath(job, new Path(otherArgs[otherArgs.length - 1]));
				System.exit(job.waitForCompletion(true) ? 0 : 1);

			}
	}
