package src;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

//�ļ�����������
public class my {

	public static void main(String[] args) throws Exception {
		renameFiles("/home/xinze/Downloads/Bayes/rename/UK","UK_");
	}

	// �������ļ��������ļ����������ض��ַ���
	public static void renameFiles(String filePath, String fix) {

		File fl = new File(filePath);
		String[] files = fl.list();
		File f = null;
		String filename = "";
		for (String file : files) {
			f = new File(fl, file);// ע��,����һ��Ҫд��File(fl,file)���д��File(file)���в�ͨ��,һ��Ҫȫ·��
			filename = f.getName();
			String postFix = filename.substring(filename.lastIndexOf("."),
					filename.length());// ��ȡ��׺��
//			System.out.println(postFix);
			f.renameTo(new File(fl.getAbsolutePath() + "//"+ fix
					+ filename.substring(0, filename.lastIndexOf(".") - 1)
					 + postFix));
		}
	}

	// ����ʱ���˳�����������ļ�
	public static void renameFileByTime(String filePath) {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");// �������ڸ�ʽ
		File fl = new File(filePath); // ��Ŀ¼�ַ����е�\�滻��\\
		String[] files = fl.list();
		File f = null;
		String filename = "";
		int i = 0;
		for (String file : files) {
			f = new File(fl, file);// ע��,����һ��Ҫд��File(fl,file)���д��File(file)���в�ͨ��,һ��Ҫȫ·��
			filename = f.getName();
			String postFix = filename.substring(filename.lastIndexOf("."),
					filename.length());// ��ȡ��׺��
			String newName = df.format(new Date());// new
													// Date()Ϊ��ȡ��ǰϵͳʱ�䣬����ʱ�������������ļ�
			f.renameTo(new File(fl.getAbsolutePath() + "//" + newName + "_" + i
					+ postFix));
			i++;
			if(i>2147483647)//����int�͵������ֵ��Χʱ��i����
				i=0;
		}
	}
}
