import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;

public class XMLParser {

    public static HashMap<String, Variable> Parser(String xml_file){
        HashMap<String, Variable> data = new HashMap<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xml_file);
            NodeList vars = doc.getElementsByTagName("VARIABLE");
            for(int i=0; i< vars.getLength(); i++){
                Variable v = new Variable();
                Node var = vars.item(i);
                NodeList varDetails = var.getChildNodes();
                for(int j=0; j< varDetails.getLength(); j++) {
                    Node detail = varDetails.item(j);
                    if(detail.getNodeType()==Node.ELEMENT_NODE){
                        Element detailElement = (Element) detail;
                        String strElement = detailElement.getTagName();
                        String strElementVal = detailElement.getTextContent();
                        if(strElement.equals("NAME")){
                            data.put(strElementVal, v);
                            v.setVar_name(strElementVal);
                        }
                        else if(strElement.equals("OUTCOME")){
                            v.addOutcome(strElementVal);
                        }
//                        System.out.println(detailElement.getTagName() + ":" + detailElement.getTextContent());
                    }
                }
            }

            NodeList cpts = doc.getElementsByTagName("DEFINITION");
            for(int i=0; i< cpts.getLength(); i++){
                Node cpt = cpts.item(i);
                NodeList cptDetails = cpt.getChildNodes();
                String curr = null;
                for(int j=0; j< cptDetails.getLength(); j++) {
                    Node detail = cptDetails.item(j);
                    if(detail.getNodeType()==Node.ELEMENT_NODE){
                        Element detailElement = (Element) detail;
                        String tag = detailElement.getTagName();
                        String val = detailElement.getTextContent();
                        if(tag.equals("FOR")){
                            curr = val;
                        }
                        Variable v = data.get(curr);
                        if(tag.equals("GIVEN")){
                            v.addParent(val);
                        }
                        else if(tag.equals("TABLE")){
                            v.setCpt(val);
                        }
//                        System.out.println(detailElement.getTagName() + ":" + detailElement.getTextContent());
                    }
                }
            }
        }catch (SAXException | IOException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        return data;
    }

}


