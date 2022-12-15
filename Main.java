import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class Main {


    public static void main(String[] args) throws FileNotFoundException, UnsupportedEncodingException {
        HashMap<String, Variable> d = XMLParser.Parser("big_net.xml");
//        BayesianNetwork.calc("testBigNet.txt");
//        BayesianNetwork.calc("input.txt");
        BayesianNetwork.two("D1=T|A1=T,A2=F,A3=T,C1=T,C2=v1", d);

    }

}