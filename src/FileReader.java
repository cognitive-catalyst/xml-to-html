/*
Copyright IBM

 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
*/

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
