import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.io.FileNotFoundException;
import java.io.PrintStream;


public class ReadFiles {
    private static ArrayList<String> FileList = new ArrayList<String>();
    private static HashMap<String, ArrayList<Integer>> Index = new HashMap<String, ArrayList<Integer>>();//G记录关键词-包含该关键词的文件
    private static HashMap<String, ArrayList<Integer>> Vector = new HashMap<String, ArrayList<Integer>>();

    public static void main(String[] args) throws IOException {
        String file = "D:\\DATA\\data";
        //求TF，可以缩减
        //返回每个文件address-> id-> word-> tf
        HashMap<String, HashMap<Integer, HashMap<String, Float>>> all_tf = tfAllFiles(file);
        System.out.println();
        //求IDF，由于Index生成代码在idf中，所以执行该部分
        HashMap<String, Float> idfs = idf(all_tf);
        System.out.println();
        //求TF-IDF，可以通过该值对关键词进行排序，提取频率较高的多少个关键词
        tf_idf(all_tf, idfs);
        //将关键词打印到1.txt文档
        PrintStream ps = new PrintStream("D:\\DATA\\keywords.txt");
        System.setOut(ps);
        output();

    }



    public static void GetVector() {
        Iterator iter = Index.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entrys = (Map.Entry) iter.next();
            String nword = entrys.getKey().toString();
            ArrayList<Integer> array = (ArrayList<Integer>) entrys.getValue();
            ArrayList<Integer> array1 = new ArrayList<Integer>();
            for (int i = 0; i < FileList.size(); i++) {
                if (array.contains((i + 1))) {
                    array1.add(1);
                } else array1.add(0);
            }
            BigDecimal[] VectorE = (BigDecimal[]) array1.toArray();
            Vector.put(nword, array1);

        }

    }



    // the list of file
    //get list of file (subfiles' path)for the directory, including sub-directory of it
    public static List<String> readDirs(String filepath) throws FileNotFoundException, IOException {
        try {
            File file = new File(filepath);
            if (!file.isDirectory()) {
                System.out.println("输入的[]");
                System.out.println("filepath:" + file.getAbsolutePath());
            } else {
                String[] flist = file.list();
                for (int i = 0; i < flist.length; i++) {
                    File newfile = new File(filepath + "\\" + flist[i]);
                    if (!newfile.isDirectory()) {
                        FileList.add(newfile.getAbsolutePath());
                    } else if (newfile.isDirectory()) //if file is a directory, call ReadDirs
                    {
                        readDirs(filepath + "\\" + flist[i]);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }
        return FileList;
    }

    //term frequency in a file, times for each word
    public static HashMap<String, Integer> normalTF(ArrayList<String> cutwords) {
        HashMap<String, Integer> resTF = new HashMap<String, Integer>();
        for (String word : cutwords) {
            if (resTF.get(word) == null) {
                resTF.put(word, 1);
                //System.out.println(word);
            } else {
                resTF.put(word, resTF.get(word) + 1);
                //System.out.println(word.toString());
            }
        }
        return resTF;
    }

    //term frequency in a file, frequency of each word
    public static HashMap<String, Float> tf(ArrayList<String> cutwords) {
        HashMap<String, Float> resTF = new HashMap<String, Float>();
        int wordLen = cutwords.size();
        //对于cutword中存储的每个word，提取其频次
        HashMap<String, Integer> intTF = ReadFiles.normalTF(cutwords);
        //HashSet set=map.entrySet();将map类型数据转换成集合set类型的。
        //iter=set.iterator();//获得集合的迭代器。迭代器只针对集合类型的数据
        Iterator iter = intTF.entrySet().iterator();
        //iterator for that get from TF
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            resTF.put(entry.getKey().toString(), Float.parseFloat(entry.getValue().toString()) / wordLen);
            //System.out.println(entry.getKey().toString() + " = " + Float.parseFloat(entry.getValue().toString()) / wordLen);
        }
        return resTF;
    }

    //tf times for file
    public static HashMap<String, HashMap<String, Integer>> normalTFAllFiles(String dirc) throws IOException {
        HashMap<String, HashMap<String, Integer>> allNormalTF = new HashMap<String, HashMap<String, Integer>>();
        List<String> filelist = ReadFiles.readDirs(dirc);
        for (String file : filelist) {
            HashMap<String, Integer> dict = new HashMap<String, Integer>();
            BufferedReader br = new BufferedReader(new FileReader(file));
            ArrayList<String> cutwords = new ArrayList<String>();
            String readLine = null;
            while ((readLine = br.readLine()) != null) {
                String[] wordsArr1 = readLine.split("[^a-zA-Z]");  //过滤出只含有字母的
                for (String word : wordsArr1) {
                    if (word.length() > 1) {  //去除长度为0的行
                        cutwords.add(word);
                    }
                }
            }
            br.close();
            //get cut word for one file
            dict = ReadFiles.normalTF(cutwords);
            allNormalTF.put(file, dict);
        }
        return allNormalTF;
    }

    //tf for all file 返回值为 地址->文件标识符->关键词，词频
    public static HashMap<String, HashMap<Integer, HashMap<String, Float>>> tfAllFiles(String dirc) throws IOException {
        HashMap<String, HashMap<Integer, HashMap<String, Float>>> allTF = new HashMap<String, HashMap<Integer, HashMap<String, Float>>>();
        List<String> filelist = ReadFiles.readDirs(dirc);   //将索dirc路径中所有文件的路径加入FileList中，并return给filelist
        Integer number = 1;
        //对filelist中的每个file，对其内容以非字母地方分割，将分割后的word加入
        for (String file : filelist) {
            HashMap<Integer, HashMap<String, Float>> numHash = new HashMap<Integer, HashMap<String, Float>>();
            HashMap<String, Float> dict = new HashMap<String, Float>();
            BufferedReader br = new BufferedReader(new FileReader(file));
            ArrayList<String> cutwords = new ArrayList<String>();
            String readLine = null;
            while ((readLine = br.readLine()) != null) {
                String[] wordsArr1 = readLine.split("[^a-zA-Z]");  //过滤出只含有字母的
                for (String word : wordsArr1) {
                    if (word.length() >1 &&word.length() <16) {  //去除长度小于4的关键词
                        word = word.toLowerCase();//变小写
                        cutwords.add(word);


                    }
                }
            }
            br.close();
            dict = ReadFiles.tf(cutwords);
            numHash.put(number, dict);
            allTF.put(file, numHash);
            number++;
        }

        return allTF;
    }

    public static HashMap<String, Float> idf(HashMap<String, HashMap<Integer, HashMap<String, Float>>> all_tf) {
        HashMap<String, Float> resIdf = new HashMap<String, Float>();//记录关键词-IDf
        HashMap<String, Integer> dict = new HashMap<String, Integer>();//记录关键词-包含该关键词的文件数
        int docNum = FileList.size();
        Integer b;
        for (int i = 0; i < docNum; i++) {
            //HashMap<String, Float> temp = all_tf.get(FileList.get(i));
            HashMap<String, Float> temp = all_tf.get(FileList.get(i)).get(i + 1);//i+1
            Iterator iter = temp.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String word = entry.getKey().toString();
                if (dict.get(word) == null) {
                    dict.put(word, 1);
                } else {
                    dict.put(word, dict.get(word) + 1);
                }
                ArrayList<Integer> a = new ArrayList<>();
                /*
                 *生成关键词-文件索引*/
                //i+1 文件标识符是从1开始的，而i从0开始所以要加1
                if (Index.get(word) == null) {
                    a.add(i + 1);
                    Index.put(word, a);
                } else {
                    a = Index.get(word);
                    a.add(i + 1);
                    Index.put(word, a);
                }
            }
        }
        //System.out.println("IDF for every word is:");
        Iterator iter_dict = dict.entrySet().iterator();
        while (iter_dict.hasNext()) {
            Map.Entry entry = (Map.Entry) iter_dict.next();
            Float value = (float) Math.log(docNum / Float.parseFloat(entry.getValue().toString()));
            resIdf.put(entry.getKey().toString(), value);
            //System.out.println(entry.getKey().toString() + " = " + value);
        }
        return resIdf;
    }

    public static void tf_idf(HashMap<String, HashMap<Integer, HashMap<String, Float>>> all_tf, HashMap<String, Float> idfs) {
        HashMap<String, HashMap<String, Float>> resTfIdf = new HashMap<String, HashMap<String, Float>>();
        int docNum = FileList.size();
        for (int i = 0; i < docNum; i++) {
            String filepath = FileList.get(i);
            HashMap<String, Float> tfidf = new HashMap<String, Float>();
            HashMap<String, Float> temp = all_tf.get(filepath).get(i + 1);//i+1
            Iterator iter = temp.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                String word = entry.getKey().toString();
                Float value = (Float) Float.parseFloat(entry.getValue().toString()) * idfs.get(word);
                tfidf.put(word, value);
            }
            resTfIdf.put(filepath, tfidf);
        }
        System.out.println("TF-IDF for Every file is :");
        DisTfIdf(resTfIdf);
    }

    public static void DisTfIdf(HashMap<String, HashMap<String, Float>> tfidf) {
        Iterator iter1 = tfidf.entrySet().iterator();
        while (iter1.hasNext()) {
            Map.Entry entrys = (Map.Entry) iter1.next();
            System.out.println("FileName: " + entrys.getKey().toString());
            System.out.print("{");
            HashMap<String, Float> temp = (HashMap<String, Float>) entrys.getValue();
            Iterator iter2 = temp.entrySet().iterator();
            while (iter2.hasNext()) {
                Map.Entry entry = (Map.Entry) iter2.next();
                System.out.print(entry.getKey().toString() + " = " + entry.getValue().toString() + ", ");
            }
            System.out.println("}");
        }
    }

    public static void output() {
        Iterator iter = Index.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entrys = (Map.Entry) iter.next();
            System.out.println(entrys.getKey().toString());
            //System.out.println(entrys.getValue());
        }
    }


}


