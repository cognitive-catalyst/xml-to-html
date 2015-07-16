/**************************************************
 * This is in very early stages of development. 
 * Please contact me if you need something supported.
 * 
 * Andrew Ayres
 *******************************************************/

package com.ibm.watson.ecosystem.xml2html;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Converter {
  public Properties prop;
	
  public void convert(File f, PrintWriter pw){
    try{
    Boolean abstractDummy = false;
    Boolean bodyDummy = true;
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(f);
    pw.println("<html>");
    pw.println("<body>");
    pw.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />");
    NodeList nList = doc.getElementsByTagName("header");
    if(nList.getLength() > 0 && nList.item(0).getNodeType() == Node.ELEMENT_NODE){
      Element ne = (Element) nList.item(0);
      NodeList metaNodes = ne.getElementsByTagName("contentMeta");
      if(metaNodes.getLength() > 0 && metaNodes.item(0).getNodeType() == Node.ELEMENT_NODE){
        Element abstractEl = (Element) metaNodes.item(0);
        NodeList titleNl = abstractEl.getElementsByTagName("titleGroup");
        if(titleNl.getLength() > 0)
          recursiveParse(titleNl.item(0), pw, 1, abstractDummy);
        NodeList abstractNl = abstractEl.getElementsByTagName("abstractGroup");
        if(abstractNl.getLength() > 0 && !checkForTitle(abstractNl.item(0))){
          pw.print("<h2>Abstract</h2>");
        }
        for(int i = 0; i < abstractNl.getLength(); i++){
          recursiveParse(abstractNl.item(i), pw, 2, abstractDummy);
        }
      }
    }
    
    nList = doc.getElementsByTagName("body");
    if(nList.item(0).getNodeType() == Node.ELEMENT_NODE){
      recursiveParse(nList.item(0), pw, 1, bodyDummy);
    }
    
    pw.flush();
    pw.println("</body>");
    pw.println("</html>");
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  private String convertTags(String s){
    String tmp1 = s.replaceAll("<"+prop.getProperty("boldTag")+">", "<b>");
    String tmp2 = tmp1.replaceAll("</"+prop.getProperty("boldTag")+">", "</b>");
    String tmp3 = tmp2.replaceAll("<"+prop.getProperty("italicTag")+">", "<i>");
    String tmp4 = tmp3.replaceAll("</"+prop.getProperty("italicTag")+">", "</i>");
    String tmp5 = tmp4.replaceAll("<"+prop.getProperty("underlineTag")+">", "<u>");
    String tmp6 = tmp5.replaceAll("</"+prop.getProperty("underlineTag")+">", "</u>");
    String tmp7 = tmp6.replaceAll("<"+prop.getProperty("refOpenTag"), "<a href=");
    String tmp8 = tmp7.replaceAll("</"+prop.getProperty("refCloseTag")+">", "</a>");
    String tmp9 = tmp8.replaceAll("<"+prop.getProperty("linkOpenTag"), "<a href=");
    String tmp10 = tmp9.replaceAll("</"+prop.getProperty("linkCloseTag")+">", "</a>");
    
    return tmp10;
  }
  
  private Boolean checkForTitle(Node n){
    NodeList nl = n.getChildNodes();
    for(int i = 0; i < nl.getLength(); i++){
      String nodeName = nl.item(i).getNodeName();
      if(nodeName.equals(prop.getProperty("headerTag")) || nodeName.equals(prop.getProperty("headerTagAlt"))){
        return true;
      } else if (nodeName == "p"){
        return false;
      } else if(checkForTitle(nl.item(i))){
        return true;
      }
    }
    return false;
  }
  
  private void recursiveParse(Node n, PrintWriter pw, int depth, Boolean dummyHeaders){
    pw.flush();
    NodeList nl = n.getChildNodes();
    for(int i = 0; i < nl.getLength(); i++){
      String nodeName = nl.item(i).getNodeName();
      if(nodeName.equals(prop.getProperty("sectionTag"))){
        recursiveParse(nl.item(i), pw, depth+1, dummyHeaders);
      } else if(nodeName.equals(prop.getProperty("headerTag")) || nodeName.equals(prop.getProperty("headerTagAlt"))){
        pw.println("<h" + depth +">"+ convertTags(nl.item(i).getTextContent()) + "</h" + depth + ">");
      } else if(nodeName.equals(prop.getProperty("paragraphTag"))){
    	if(dummyHeaders==true)
          pw.println("<h"+(depth+1)+">"+i+"</h"+(depth+1)+">");
        pw.println(convertTags(nodeToString(nl.item(i))));
      } else if(nodeName.equals(prop.getProperty("figureTag"))){
        pw.println(convertTags(nl.item(i).getTextContent()));
      } else if(nodeName.equals(prop.getProperty("tableWrapperTag"))){
        tableParse(nl.item(i), pw, depth);
      } else if(nodeName.equals(prop.getProperty("listTag"))){
        pw.println("<ol style=\"list-style-type: decimal\">");
        listParse(nl.item(i), pw, depth);
        pw.println("</ol>");
      } else if(nodeName.equals(prop.getProperty("skipTag")) || nodeName.equals(prop.getProperty("skipTagAlt"))){
      } else {
        recursiveParse(nl.item(i), pw, depth, dummyHeaders);
      }
    }
  }
  
  private void listParse(Node n, PrintWriter pw, int depth){
    NodeList nl = n.getChildNodes();
    for(int i = 0; i < nl.getLength(); i++){
      String nodeName = nl.item(i).getNodeName();
      if(nodeName.equals(prop.getProperty("listElementTag"))){
        pw.println("<li>");
        recursiveParse(nl.item(i), pw, depth, false);
        pw.println("</li>");
      }
    }
  }
  
  private void tableParse(Node n, PrintWriter pw, int depth){
    NodeList nl = n.getChildNodes();
    for(int i = 0; i < nl.getLength(); i++){
      String nodeName = nl.item(i).getNodeName();
      if(nodeName.equals(prop.getProperty("tableTag"))){
        pw.println("<table>");
        tableParse(nl.item(i), pw, depth);
        pw.println("</table>");
      } else if(nodeName.equals(prop.getProperty("headerTag")) || nodeName.equals(prop.getProperty("headerTagAlt"))){
        pw.println("<h6>"+ convertTags(nl.item(i).getTextContent()) + "</h6>");
      } else if(nodeName.equals(prop.getProperty("tableEntryTag"))){
        pw.println("<td>"+ convertTags(nl.item(i).getTextContent()) + "</td>");
      } else if(nodeName.equals(prop.getProperty("tableRowTag"))){
          pw.println("<tr>");
          tableParse(nl.item(i), pw, depth);
          pw.println("</tr>");
      } else if(nodeName.equals(prop.getProperty("tableFooterTag"))){
        recursiveParse(nl.item(i), pw, depth, false);
      }  else {
        tableParse(nl.item(i), pw, depth);
      }
    }
  }
  
  private String nodeToString(Node node) {
    StringWriter sw = new StringWriter();
    try {
      Transformer t = TransformerFactory.newInstance().newTransformer();
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      t.transform(new DOMSource(node), new StreamResult(sw));
    } catch (TransformerException te) {
      System.out.println("nodeToString Transformer Exception");
    }
    return sw.toString();
  }
  
  private void loadProperties(){
	try {
	  String filename = "schema.properties";
	  InputStream input = new FileInputStream(filename);
	  prop.load(input);
	  System.out.println(prop.getProperty("database"));
	  input.close();
	} catch (IOException ex) {
	  ex.printStackTrace();
    }
  }
  
  Converter(){
	prop = new Properties();
    loadProperties();
  }
}