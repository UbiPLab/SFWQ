import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

public class KASE {

    private static Pairing pairing;
    private static Element g;
    private static int n = 10;//maximum documents
    private static Element[] pubK;

    public static void Setup(int rBit){
        //rBit是Zp中阶数p的比特长度；qBit是G中阶数的比特长度

        TypeACurveGenerator pg = new TypeACurveGenerator(rBit, 512);

        long start = System.currentTimeMillis();
        PairingParameters typeAParams = pg.generate();
        long END = System.currentTimeMillis();
        System.out.println(END-start);
        pairing = PairingFactory.getPairing(typeAParams);
        //产生一个G_1群的生成元g
        g = pairing.getG1().newRandomElement().getImmutable();
        Element a = pairing.getZr().newRandomElement().getImmutable();
        pubK = new Element[2 * n + 1];
        for (int i = 1; i <= 2 * n ; i++) {
            Element g_i = g.powZn(a.pow(BigInteger.valueOf(i)));
            pubK[i] = g_i;
        }
    }
    public static Element[] Keygen(){

        Element y = pairing.getZr().newRandomElement().getImmutable();
        Element v = g.powZn(y).getImmutable();
        Element pk = v;
        Element msk = y;
        Element[] keypair = new Element[2];
        keypair[0] = pk;
        keypair[1] = msk;
        return keypair;
    }
    public static Element[] Encrypt(Element pk, Element[] PubK , int i, String w) throws NoSuchAlgorithmException {
        Element g_1 = PubK[1];
        Element g_n = PubK[n];
        Element g_i = PubK[i];
        Element t = pairing.getZr().newRandomElement().getImmutable();
        Element c1 = g.powZn(t).getImmutable();
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
    public static Element Extract(Element msk, Element[] PubK, int[] S){
        Element kagg = pairing.getG1().newOneElement().getImmutable();
        for (int j : S) {
            int item = n + 1 - j;
            Element element = PubK[item];
            Element mul = element.powZn(msk).getImmutable();
            kagg = kagg.mul(mul);
        }
//        for (int j = 0; j < S.length; j++) {
//            int item = n + 1 - j;
//            Element element = PubK[item];
//            Element mul = element.powZn(msk).getImmutable();
//            kagg = kagg.mul(mul);
//        }
        return kagg;
    }
    public static Element Trapdoor(Element kagg, String w) throws NoSuchAlgorithmException {
        MessageDigest instance = MessageDigest.getInstance("sha-256");
        byte[] byteArray_G_1 = instance.digest((w).getBytes());
        //将byte[] byteArray_G_1哈希到G_1群
        Element hash_G_1 = pairing.getG1().newElement().setFromHash(byteArray_G_1, 0, byteArray_G_1.length);
        Element Tr = kagg.mul(hash_G_1).getImmutable();
        return Tr;
    }
    public static Element Adjust(int i, Element[] PubK , int[] S, Element Tr){
        Element mul_all = pairing.getG1().newOneElement().getImmutable();
        for (int j : S) {
            if (j != i){
                int item = n + 1 - j + i;
                Element element = PubK[item];
                mul_all = mul_all.mul(element);
            }
        }
//        for (int j = 0; j < S.length; j++) {
//            if (j != i){
//                int item = n + 1 - j + i;
//                Element element = PubK[item];
//                mul_all = mul_all.mul(element);
//            }
//        }
        Element Tr_i = Tr.mul(mul_all);
        return Tr_i;
    }
    public static boolean Test(Element Tr_i, Element[] PubK , int i, int[] S , Element[] elements){
        Element c1 = elements[0];
        Element c2 = elements[1];
        Element cw = elements[2];
        Element pub = pairing.getG1().newOneElement().getImmutable();
        for (int j : S) {
            int item = n + 1 - j;
            Element element = PubK[item];;
            pub = pub.mul(element);
        }
//        for (int j = 0; j < S.length; j++) {
//            int item = n + 1 - j;
//            Element element = PubK[item];;
//            pub = pub.mul(element);
//        }
        Element on = pairing.pairing(Tr_i, c1).getImmutable();
        Element down = pairing.pairing(pub, c2).getImmutable();
        Element right = on.div(down).getImmutable();
        if (cw.isEqual(right)) {
            return true;
        }
        return false;


    }


    public static Pairing getPairing() {
        return pairing;
    }

    public static Element getG() {
        return g;
    }

    public static int getN() {
        return n;
    }

    public static Element[] getPubK() {
        return pubK;
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        int rBit = 512;
        //1.Setup():初始化

        Setup(rBit);


        //2.DataOwner产生公私钥对
        Element[] keypair = Keygen();
        Element pk = keypair[0];
        Element msk = keypair[1];
        ArrayList<Element[]> encrypts = new ArrayList<>();
        encrypts.add(null);
        HashMap<Integer, String> keywordsMap = new HashMap<>();
        for (int i = 1; i <= n; i++) {
            if (i % 2 == 0){
                keywordsMap.put(i, "偶数");
            }else {
                keywordsMap.put(i, "奇数");
            }

        }
        //3.DataOwner根据其关键字喝索引加密每个文档
        for (int i = 1; i <=10 ; i++) {
            Element[] encrypt = Encrypt(pk, pubK, i, keywordsMap.get(i));
            encrypts.add(encrypt);
        }
        //想要搜索的文档编号集合
        int[] S = {2, 4, 6, 8, 10};
        //4. 数据所有者根据关键字生成聚合密钥
        Element kagg = Extract(msk, pubK, S);
        //5.用户根据聚合密钥和关键字生成唯一的陷门
        Element Tr = Trapdoor(kagg, "偶数");
        //6.云服务器为每一个想要搜索的文档生成正确的陷门，并进行Test
        for (int i : S) {
            Element Tri = Adjust(i, pubK, S, Tr);
            boolean test = Test(Tri, pubK, i, S, encrypts.get(i));
            if (test) {
                System.out.println(i);
            }
        }
    }
}

