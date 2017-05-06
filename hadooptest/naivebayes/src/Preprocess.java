package src;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class Preprocess
	{
		@SuppressWarnings("unused")
		private static String[] typeName = new String[] { "UK", "USA" };// �������
		@SuppressWarnings("unused")
		private static String dictionaryName = "DictionarySize";

		private static String pathMLEData = "hdfs://xinze-ubuntu:9000/user/xinze/xinze-hadoop/output/MLE_data";
		private static String pathPart = "hdfs://xinze-ubuntu:9000/user/xinze/xinze-hadoop/output/Countwords/part-r-00000";

		// ����Ϊmapreduce�����е�ͳ������
		private static Long dictionarySize = 0L;// �ʵ�Ĵ�С
		private static Long[] typeSize = new Long[] { 0L, 0L };// ÿ�����ĵ���������ע���������ʹ�С
		private static Long[] typeFileSize = new Long[] { 0L, 0L }; // ÿ������зֱ��ж��ٸ��ļ�

		public static void main(String[] args) throws Exception
			{
				Configuration conf = new Configuration();
				conf.addResource(new Path("/home/xinze/Documents/hadoop-2.7.3/etc/hadoop/core-site.xml"));
				conf.addResource(new Path("/home/xinze/Documents/hadoop-2.7.3/etc/hadoop/hdfs-site.xml"));
				acquireData(conf);// ѵ���ļ��л�ȡ����Ƶ�ʺ͵��ʵĸ���������
				RECountData(conf);// ����ÿ�����ʵĳ��ָ��ʣ�������д���µ��ļ���
			}

		public static HashMap<String, Integer> wordNum = new HashMap<String, Integer>();
		public static HashMap<String, Integer> classWordNum = new HashMap<String, Integer>();

		// ��ȡͳ������
		private static void acquireData(Configuration conf)
			{
				FSDataInputStream fsr = null;
				BufferedReader bufferedReader = null;
				String lineTxt = null;
				try
					{
						FileSystem fs = FileSystem.get(URI.create(pathPart), conf);
						fsr = fs.open(new Path(pathPart));
						bufferedReader = new BufferedReader(new InputStreamReader(fsr));

						while ((lineTxt = bufferedReader.readLine()) != null)
							{
								String[] className = lineTxt.split(":");
								String[] wordName = className[1].split("\t");
								String classnameString = className[0];// �����
								String wordstString = wordName[0];// ����
								int wordnum = Integer.parseInt(wordName[1]);
								// System.out.println(classnameString);
								// �ֱ����ÿ������еĵ�������
								if (classWordNum.get(classnameString) != null)
									{
										int value = classWordNum.get(classnameString);
										classWordNum.put(classnameString, value + wordnum);
									}
								else
									{
										classWordNum.put(classnameString, wordnum);
									}

								// �����ֵ��С
								if (wordNum.get(wordstString) != null)
									{
										int value = wordNum.get(wordstString);
										wordNum.put(wordstString, value + 1);
									}
								else
									{
										wordNum.put(wordstString, 1);
									}

							}

						dictionarySize = (long) wordNum.size();
						typeSize[0] = Long.parseLong(classWordNum.get("UK") + "");
						typeSize[1] = Long.parseLong(classWordNum.get("USA") + "");
						typeFileSize[0] = 240L;
						typeFileSize[1] = 500L;
						// System.out.println(dictionarySize);
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

		// ���¼���mapreduce�������е����ݲ��������ļ�
		private static void RECountData(Configuration conf) throws IOException
			{
				FSDataInputStream fsr = null;
				BufferedReader bufferedReader = null;
				String lineTxt = null;
				FSDataOutputStream os = null;

//				System.out.println(dictionarySize);
//				System.out.println(typeFileSize[0] + "\t" + typeFileSize[1]);
//				System.out.println(typeSize[0] + "\t" + typeSize[1]);

				try
					{
						// ���ļ�

						FileSystem fs = FileSystem.get(URI.create(pathPart), conf);

						fsr = fs.open(new Path(pathPart));

						bufferedReader = new BufferedReader(new InputStreamReader(fsr));
						// д�ļ�
						FileSystem hdfs = FileSystem.get(conf);
						System.out.println(typeSize[0] + "\t" + typeSize[1]);

//						Path osPath = new Path("hdfs://xinze-ubuntu:9000/user/xinze/xinze-hadoop/output/enddata");
//						FileSystem osFileSystem = osPath.getFileSystem(conf);
//						osFileSystem.mkdirs(osPath);
						
						os = hdfs.create(new Path(pathMLEData));
//						os = hdfs.create(new Path(pathCount.substring(0, pathCount.lastIndexOf("/") + 1) + "end_data"));

						double dou01 = Math.log10(1) - Math.log10(typeSize[0] + dictionarySize);
						double dou02 = Math.log10(1) - Math.log10(typeSize[1] + dictionarySize);
						// д���������
						double dou1 = Math.log10(typeFileSize[0]) - Math.log10(typeFileSize[0] + typeFileSize[1]);
						double dou2 = Math.log10(typeFileSize[1]) - Math.log10(typeFileSize[0] + typeFileSize[1]);
//						System.out.println(dou01 + "\t" + dou02 + "\t" + dou1 + "\t" + dou2 + "\n");
						os.write((dou01 + "\t" + dou02 + "\t" + dou1 + "\t" + dou2 + "\n").getBytes("UTF-8"));
						os.flush();
						// bufferedReader.readLine();// ���в���
						while ((lineTxt = bufferedReader.readLine()) != null)
							{

								String[] className = lineTxt.split(":");
								String[] wordName = className[1].split("\t");
								String classnameString = className[0];// �����
								String wordstString = wordName[0];// ����
								int nums = Integer.parseInt(wordName[1]);
								String content = "";
								if (classnameString.equals("UK"))
									{
										content = classnameString + ":" + wordstString + "\t"
												+ (Math.log10(nums + 1) - Math.log10(typeSize[0] + dictionarySize))
												+ "\n";
									}
								else
									{// USA
										content = classnameString + ":" + wordstString + "\t"
												+ (Math.log10(nums + 1) - Math.log10(typeSize[1] + dictionarySize))
												+ "\n";
									}

								os.write(content.getBytes("UTF-8"));
								os.flush();// д���ļ�
							}

					}
				catch (Exception e)
					{
						e.printStackTrace();
					}
				finally
					{
						os.close();
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
	}
