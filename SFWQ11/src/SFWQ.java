import it.unisa.dia.gas.jpbc.*;

import java.io.*;
import java.util.*;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;


public class SFWQ {
    private static Pairing pairing;
    public static ArrayList<String> key_list = new ArrayList<>();
    //public static ArrayList<ArrayList<Integer>> value_list = new ArrayList<>();//存储的关键词对应的位置
    public static ArrayList<Integer> value_list = new ArrayList<>();//存储的关键词对应的位置
    private static Element g;
    private static ElementPowPreProcessing gPre;
    private static int n;//maximum documents
    private static Element[] pubK;
    public static ArrayList<String> symbol = new ArrayList<String>();
    public static ArrayList<String> condition = new ArrayList<>();
    public static ArrayList<String> character = new ArrayList<>();
    public static ArrayList<Integer> value = new ArrayList<>();
    public static Element A;
    public static Element B;


    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        String file = "D:\\DATA\\keywords.txt";
        ArrayList<String> cutwords = ExKeyword(file);
        String[] keywords = cutwords.toArray(new String[cutwords.size()]);
        n =keywords.length;
        int rBit = 160;
        Setup(rBit);
        A = pairing.getZr().newRandomElement().getImmutable();
        B = pairing.getZr().newRandomElement().getImmutable();
        Element[] keypair = Keygen();
        Element pk = keypair[0];
        Element msk = keypair[1];
        ArrayList<ArrayList<Element[]>> enkey_list=new ArrayList<>();
        for (int j = 0; j < keywords.length; j++) {
            ArrayList<Element[]> onekey=new ArrayList<>();
            for (int k = 0; k <keywords[j].length(); k++) {
                String x = Character.toString(keywords[j].charAt(k));
                Element[] y = Encrypt(pk, pubK, j + 1, x);
                onekey.add(y);
            }
            enkey_list.add(onekey);
        }
        String query = "*ow*";
        ArrayList<Element> tr = new ArrayList<>();
        Element AGG = Extract(msk, pubK);//可提前计算，不用算在时间里
        Element fix = gPre.powZn(A).powZn(B);//可提前计算
        long startTime3 = System.currentTimeMillis();
        int[] paragraph = ExFeature2(query);
        for (int k = 0; k < character.size(); k++) {
            String x = character.get(k);
            Element y = Trapdoor(AGG, x, fix);
            tr.add(y);
        }
        long endTime3 = System.currentTimeMillis();
        //time_user_query[K] = endTime2 - startTime2;
        //获取结束时间
        System.out.println("query-user run time:" + (endTime3 - startTime3) + "ms");

        long startTime4 = System.currentTimeMillis();
        Element[][] trr = new Element[keywords.length][tr.size()];
        for (int i = 0; i < keywords.length; i++) {
            Element[] temp = new Element[tr.size()];
            for (int j = 0; j < tr.size(); j++) {
                temp[j] = Adjust(i + 1, pubK, tr.get(j));
            }
            trr[i] = temp;
        }
        boolean[] R=new boolean[keywords.length];
        for (int i = 0; i < enkey_list.size(); i++) {
            R[i]= matchArrays(trr[i], paragraph, enkey_list.get(i));
        }
        ArrayList<String> Results=new ArrayList<>();
        for (int i = 0; i <R.length; i++) {
            if(R[i]==true)
                Results.add(keywords[i]);
        }
        System.out.println("R = " + Results);
        long endTime4 = System.currentTimeMillis(); //获取结束时间
        System.out.println("query-server run time:" + (endTime4 - startTime4) + "ms");
        //time_server_query[K] = endTime3 - startTime3;
        character.clear();
        value.clear();


    }
        /*String save_address = "D:\\cream\\实验\\SFWQ1\\实验记录\\data.txt";
        File f1 = new File(save_address);
        PrintWriter output = null;
        try {
            output = new PrintWriter(f1);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        output.println("setup time : " + "\n");
        output.println(Arrays.toString(time_setup) + "\n");
        output.println("user-query time : " + "\n");
        output.println(Arrays.toString(time_user_query) + "\n");
        output.println("server_query size : " + "\n");
        output.println(Arrays.toString(time_server_query) + "\n");


        output.close();
    }*/



    public static Element Extract(Element msk, Element[] PubK) {
        Element kagg = pairing.getG1().newOneElement().getImmutable();
        for (int j = 1; j <= n; j++) {
            int item = n + 1 - j;
            Element element = PubK[item];
            Element mul = element.powZn(msk).getImmutable();
            kagg = kagg.mul(mul);
        }
        return kagg;
    }

    private static int findCharInArray(ArrayList<Element[]> arr, Element target, int startIndex, Element gAB) {
        for (int i = startIndex; i < arr.size(); i++) {
            if (Test(target, pubK, arr.get(i), gAB)) {
                return i; // Character found at index i
            }
        }
        return -1; // Character not found
    }

    public static boolean matchArrays(Element[] trr, int[] C, ArrayList<Element[]> enkey_list) {
        int groupIndexA = 0;
        int groupIndexB = 0;
        boolean F = false;
        Element gAB = gPre.powZn(A).powZn(B);
        for (int i = 0; i < C.length; i++) {
            int groupLength = C[i];
            // Find the first character in A group in B
            int foundIndex = findCharInArray(enkey_list, trr[groupIndexA], groupIndexB, gAB);
            if (foundIndex == -1 || 0 > enkey_list.size() - groupLength) {
                return false; // Character not found in B or B is out of bounds, match failed
            }
            int f = foundIndex;
            // Check if the subsequent characters in A group match the corresponding characters in B
            for (int j = 1; j < groupLength; j++) {
                Element currentCharA = trr[groupIndexA + j];
                f++;
                if (value.get(groupIndexA + j) != 1) {
                    f = f + value.get(groupIndexA + j) - 1;
                }
                Element[] currentCharB = enkey_list.get(f);
                if (!Test(currentCharA, pubK, currentCharB, gAB)) {
                    F = true;
                    break; // Characters don't match, match failed
                }
            }
            if (F) {
                i = i - 1;
                groupIndexB = foundIndex + 1;
                F = false;
            } else {
                groupIndexA += groupLength;
                groupIndexB = foundIndex + groupLength;
            }
        }

        return true; // All characters matched successfully
    }

    public static Element Trapdoor(Element kagg, String w, Element X) throws NoSuchAlgorithmException {
        MessageDigest instance = MessageDigest.getInstance("sha-256");
        byte[] byteArray_G_1 = instance.digest((w).getBytes());
        //将byte[] byteArray_G_1哈希到G_1群
        Element hash_G_1 = pairing.getG1().newElement().setFromHash(byteArray_G_1, 0, byteArray_G_1.length);
        Element Tr = kagg.mul(hash_G_1).mul(X).getImmutable();
        return Tr;
    }

    public static Element Adjust(int i, Element[] PubK, Element Tr) {
        Element mul_all = pairing.getG1().newOneElement().getImmutable();
        for (int j = 1; j <= n; j++) {
            if (j != i) {
                int item = n + 1 - j + i;
                Element element = PubK[item];
                mul_all = mul_all.mul(element);
            }
        }
        Element Tr_i = Tr.mul(mul_all);
        return Tr_i;
    }

    public static boolean Test(Element Tr_i, Element[] PubK, Element[] elements, Element gAB) {
        Element c1 = elements[0];
        Element c2 = elements[1];
        Element cw = elements[2];
        Element pub = pairing.getG1().newOneElement().getImmutable();
        for (int j = 1; j <= n; j++) {
            int item = n + 1 - j;
            Element element = PubK[item];
            pub = pub.mul(element);
        }
        Element on = pairing.pairing(Tr_i, c1).getImmutable();
        Element down = pairing.pairing(pub, c2).getImmutable();
        Element right = on.div(down).getImmutable();
        Element CW = cw.mul(pairing.pairing(gAB, c1)).getImmutable();
        if (CW.isEqual(right)) {
            return true;
        }
        return false;


    }

    //extract keywords from a file and put them in ArrayList
    public static ArrayList<String> ExKeyword(String dirc) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(dirc));
        ArrayList<String> cutwords = new ArrayList<String>();
        String readLine = null;
        while ((readLine = br.readLine()) != null) {
            String[] wordsArr1 = readLine.split("[^a-zA-Z]");  //过滤出只含有字母的
            for (String word : wordsArr1) {
                if (word.length() >= 1) {  //去除长number, dict度为0的行
                    cutwords.add(word);
                }
            }
        }
        return cutwords;
    }

    public static int[] ExFeature2(String a) {
        Integer i = 0;
        int numm = 1;
        while (i != a.length() && a.charAt(i) != ('*')) {

            String temp = Character.toString(a.charAt(i));
            i++;
            if (temp.equals("?")) {
                numm++;
                continue;
            }
            character.add(temp);
            //condition.add("=");
            value.add(numm);
            numm = 1;


        }
        String[] array = a.substring(i, a.length()).split("\\*");
        List<String> filteredList = new ArrayList<>();
        for (String str : array) {
            if (!str.isEmpty()) {
                filteredList.add(str);
            }
        }
        int num = 1;
        String[] subx = filteredList.toArray(new String[0]);
        int[] paragraph = new int[subx.length + 1];
        paragraph[0] = character.size();
        int para2 = character.size();
        for (int j = 0; j < subx.length; j++) {
            for (int k = 0; k < subx[j].length(); k++) {
                if (subx[j].charAt(k) == '?') {
                    num++;
                    continue;
                }
                character.add(Character.toString(subx[j].charAt(k)));
                if ((k == 0) && character.isEmpty() == false) {
                    //condition.add(">");//其实是>=
                    value.add(num);
                } else {
                    //condition.add("=");
                    value.add(num);
                }

                num = 1;
            }
            paragraph[j + 1] = character.size() - para2;
            para2 = character.size();

        }

        return paragraph;
    }

    public static int[] ExFeature(String a) {
        String temp = "0";
        String temp1;
        int index = 0;
        ArrayList<Integer> list_temp = new ArrayList<>();
        int temp3;
        int k = index;
        for (int j = 0; j < a.length(); j++) {
            temp1 = Character.toString(a.charAt(j));
            temp3 = j;
            list_temp.add(index++);
            key_list.add(temp1);
            value_list.add(temp3);
        }

        int[] ints = new int[list_temp.size()];
        for (int j = 0; j < list_temp.size(); j++) {
            ints[j] = list_temp.get(k + j);
        }
        return ints;
    }

    public static void Setup(int rBit) {
        //rBit是Zp中阶数p的比特长度；qBit是G中阶数的比特长度
        TypeACurveGenerator pg = new TypeACurveGenerator(rBit, 512);
        PairingParameters typeAParams = pg.generate();

        pairing = PairingFactory.getPairing(typeAParams);
        //产生一个G_1群的生成元g
        g = pairing.getG1().newRandomElement().getImmutable();

        gPre = g.getElementPowPreProcessing();

        Element a = pairing.getZr().newRandomElement().getImmutable();
        pubK = new Element[2 * n + 1];


        for (int i = 1; i <= 2 * n; i++) {
            Element g_i = gPre.powZn(a.pow(BigInteger.valueOf(i))).getImmutable();
            pubK[i] = g_i;

        }

    }

    public static Element[] Keygen() {
        Element y = pairing.getZr().newRandomElement().getImmutable();
        Element v = gPre.powZn(y).getImmutable();
        Element pk = v;
        Element msk = y;
        Element[] keypair = new Element[2];
        keypair[0] = pk;
        keypair[1] = msk;
        return keypair;
    }

    public static Element[] Encrypt(Element pk, Element[] PubK, int i, String w) throws NoSuchAlgorithmException {
        Element g_1 = PubK[1];
        Element g_n = PubK[n];
        Element g_i = PubK[i];
        Element t = pairing.getZr().newRandomElement().getImmutable();
        Element c1 = gPre.powZn(t).getImmutable();
        Element c2 = pk.mul(g_i).powZn(t).getImmutable();
        MessageDigest instance = MessageDigest.getInstance("sha-256");
        byte[] byteArray_G_1 = instance.digest((w).getBytes());
        //将byte[] byteArray_G_1哈希到G_1群
        Element hash_G_1 = pairing.getG1().newElement().setFromHash(byteArray_G_1, 0, byteArray_G_1.length);
        Element on = pairing.pairing(g, hash_G_1).powZn(t).getImmutable();
        Element down = pairing.pairing(g_1, g_n).powZn(t).getImmutable();
        Element cw = on.div(down);
        Element[] elements = {c1, c2, cw};
        return elements;
    }


}

