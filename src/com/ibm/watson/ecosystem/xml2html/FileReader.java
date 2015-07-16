package com.ibm.watson.ecosystem.xml2html;

import java.io.File;
import java.io.PrintWriter;

public class FileReader {
  public static void main(String[] args){
	
	Converter con = new Converter();
    File dir = new File("./xml");
    File[] fileList = dir.listFiles();
    if(fileList != null) {
      for(File current: fileList){
        String filename = current.getName();
        int pos = filename.lastIndexOf(".");
        if(pos > 0)
          filename = filename.substring(0, pos);
          System.out.println(filename);
        try{
          PrintWriter writer = new PrintWriter("./html/"+filename+".html", "UTF-8");
          con.convert(current, writer);
        } catch (Exception e){
        }
      }
    } else {
      System.err.println("Error: ./xml directory is empty");
    }
  }
}
