package com.yuki.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.zookeeper.AsyncCallback.StatCallback;

public class ProcessData {

	private static String[] typeName = new String[] { "CHINA", "USA" };// 类别名称
	private static String dictionaryName = "DictionarySize";
	private static String pathCount = "hdfs://Master:9000/user/hadoop/out/CountClassFilesOut/part-r-00000";
	private static String pathPart = "hdfs://Master:9000/user/hadoop/out/CountClassWordsOut/part-r-00000";
	// 以下为mapreduce过程中的统计数据
	private static Long dictionarySize = 0L;// 词典的大小
	private static Long[] typeSize = new Long[] { 0L, 0L };// 每个类别的单词总数，注意数据类型大小
	private static Long[] typeFileSize = new Long[] { 0L, 0L }; // 每个类别中分别有多少个文件

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		acquireData(conf);// 训练文件中获取单词频率和单词的各类总数。
		RECountData(conf);// 计算每个单词的出现概率，将概率写入新的文件。
	}

	public static HashMap<String, Integer> wordNum = new HashMap<String, Integer>();
	public static HashMap<String, Integer> classWordNum = new HashMap<String, Integer>();

	// 读取统计数据
	private static void acquireData(Configuration conf) {
		FSDataInputStream fsr = null;
		BufferedReader bufferedReader = null;
		String lineTxt = null;
		try {
			FileSystem fs = FileSystem.get(URI.create(pathPart), conf);
			fsr = fs.open(new Path(pathPart));
			bufferedReader = new BufferedReader(new InputStreamReader(fsr));
			while ((lineTxt = bufferedReader.readLine()) != null) {
				String[] className = lineTxt.split(":");
				String[] wordName = className[1].split("\t");
				String classnameString = className[0];// 类别名
				String wordstString = wordName[0];// 单词
				int wordnum = Integer.parseInt(wordName[1]);
				// System.out.println(classnameString);
				// 分别计算每个类别中的单词总数
				if (classWordNum.get(classnameString) != null) {
					int value = classWordNum.get(classnameString);
					classWordNum.put(classnameString, value + wordnum);
				} else {
					classWordNum.put(classnameString, wordnum);
				}

				// 计算字典大小
				if (wordNum.get(wordstString) != null) {
					int value = wordNum.get(wordstString);
					wordNum.put(wordstString, value + 1);
				} else {
					wordNum.put(wordstString, 1);
				}

			}

			dictionarySize = (long) wordNum.size();
			typeSize[0] = Long.parseLong(classWordNum.get("CHINA") + "");
			typeSize[1] = Long.parseLong(classWordNum.get("USA") + "");
			typeFileSize[0] = 240L;
			typeFileSize[1] = 500L;
			// System.out.println(dictionarySize);
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

	// 重新计算mapreduce输出结果中的数据并生成新文件
	private static void RECountData(Configuration conf) throws IOException {
		FSDataInputStream fsr = null;
		BufferedReader bufferedReader = null;
		String lineTxt = null;
		FSDataOutputStream os = null;

		try {
			// 读文件
			FileSystem fs = FileSystem.get(URI.create(pathPart), conf);
			fsr = fs.open(new Path(pathPart));
			bufferedReader = new BufferedReader(new InputStreamReader(fsr));
			// 写文件
			FileSystem hdfs = FileSystem.get(conf);
			os = hdfs.create(new Path(pathCount.substring(0,
					pathCount.lastIndexOf("/") + 1)
					+ "end_data"));

			double dou01 = Math.log10(1)
					- Math.log10(typeSize[0] + dictionarySize);
			double dou02 = Math.log10(1)
					- Math.log10(typeSize[1] + dictionarySize);
			// 写入先验概率
			double dou1 = Math.log10(typeFileSize[0])
					- Math.log10(typeFileSize[0] + typeFileSize[1]);
			double dou2 = Math.log10(typeFileSize[1])
					- Math.log10(typeFileSize[0] + typeFileSize[1]);
			os.write((dou01 + "\t" + dou02 + "\t" + dou1 + "\t" + dou2 + "\n")
					.getBytes("UTF-8"));
			os.flush();
			// bufferedReader.readLine();// 首行不读
			while ((lineTxt = bufferedReader.readLine()) != null) {

				String[] className = lineTxt.split(":");
				String[] wordName = className[1].split("\t");
				String classnameString = className[0];// 类别名
				String wordstString = wordName[0];// 单词
				int nums = Integer.parseInt(wordName[1]);
				String content = "";
				if (classnameString.equals("CHINA")) {
					content = classnameString
							+ ":"
							+ wordstString
							+ "\t"
							+ (Math.log10(nums + 1) - Math.log10(typeSize[0]
									+ dictionarySize)) + "\n";
				} else {//USA
					content = classnameString
							+ ":"
							+ wordstString
							+ "\t"
							+ (Math.log10(nums + 1) - Math.log10(typeSize[1]
									+ dictionarySize)) + "\n";
				}

				os.write(content.getBytes("UTF-8"));
				os.flush();// 写入文件
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			os.close();
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

}
