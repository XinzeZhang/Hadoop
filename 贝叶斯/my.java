package com.yuki.bayes;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

//文件批量重命名
public class my {

	public static void main(String[] args) throws Exception {
		renameFileByTime("C:\\Users\\yuki\\Desktop\\country\\test");
	}

	// 在批量文件的所有文件名最后添加特定字符串
	public static void renameFiles(String filePath, String fix) {

		if (fix == null)
			return;

		File fl = new File(filePath);
		String[] files = fl.list();
		File f = null;
		String filename = "";
		for (String file : files) {
			f = new File(fl, file);// 注意,这里一定要写成File(fl,file)如果写成File(file)是行不通的,一定要全路径
			filename = f.getName();
			String postFix = filename.substring(filename.lastIndexOf("."),
					filename.length());// 获取后缀名
			System.out.println(postFix);
			f.renameTo(new File(fl.getAbsolutePath() + "//"
					+ filename.substring(0, filename.lastIndexOf(".") - 1)
					+ fix + postFix));
		}
	}

	// 按照时间和顺序重新命名文件
	public static void renameFileByTime(String filePath) {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");// 设置日期格式
		File fl = new File(filePath); // 将目录字符串中的\替换成\\
		String[] files = fl.list();
		File f = null;
		String filename = "";
		int i = 0;
		for (String file : files) {
			f = new File(fl, file);// 注意,这里一定要写成File(fl,file)如果写成File(file)是行不通的,一定要全路径
			filename = f.getName();
			String postFix = filename.substring(filename.lastIndexOf("."),
					filename.length());// 获取后缀名
			String newName = df.format(new Date());// new
													// Date()为获取当前系统时间，按照时间来重新命名文件
			f.renameTo(new File(fl.getAbsolutePath() + "//" + newName + "_" + i
					+ postFix));
			i++;
			if(i>2147483647)//超过int型的最大数值范围时，i清零
				i=0;
		}
	}
}
