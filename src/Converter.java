/* Copyright IBM

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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Converter {
	private Properties prop;
	private Set<String> xmlReplaceTagKeys;
	private Set<String> skipTagKeys;
	private Set<String> includeTagsKeys;
	private Set<String> headerTagKeys;
	int curDepth;
	boolean skippingSection;
	boolean tablesOn, figsOn, listsOn;

	public void convert(File f, PrintWriter pw) {
		try {
			int startingDepth = 0;
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			
			//Deal with xlink namespace issue
			dbFactory.setNamespaceAware(true);
			dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(f);
			System.out.println(doc.getXmlEncoding());
			replaceTags(doc.getDocumentElement(), doc);

			pw.println(prop.getProperty("htmlHtmlTag.open"));
			pw.println(prop.getProperty("htmlBodyTag.open"));
			pw.println(prop.getProperty("htmlMetaTag"));

			if (!prop.getProperty("forceStarterHeader").equals("")) {
				NodeList startHeaderList = doc.getElementsByTagName(prop.getProperty("forceStarterHeader"));
				if (startHeaderList.getLength() > 0
						&& startHeaderList.item(0).getNodeType() == Node.ELEMENT_NODE) {
					pw.println("<h1>" + startHeaderList.item(0).getTextContent() + "</h1>");
					startingDepth = 1;
				}
			}

			parseIt(doc.getDocumentElement(), pw, startingDepth);
			pw.flush();
			pw.println(prop.getProperty("htmlBodyTag.close"));
			pw.println(prop.getProperty("htmlHtmlTag.close"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getNodePath(Node node) {
		Node tmp = node;
		String path = "";
		for (; tmp != null; tmp = tmp.getParentNode())
			path = tmp.getNodeName() + "/" + path;
		return path;
	}

	// Function which will get to the childless node and replace tags
	private void replaceTags(Node node, Document doc) {
	//	if(node.getTextContent().contains("The onset occ"))
	//		System.out.println(node.getTextContent());
		NodeList nl = node.getChildNodes();
			for (String k : xmlReplaceTagKeys)
				if (!k.contains(".with") && prop.getProperty(k) != null
						&& prop.getProperty(k + ".with") != null) {
					if(node.getNodeName().equals(prop.getProperty(k))){
						doc.renameNode(node, node.getNamespaceURI(), prop.getProperty(k+".with"));
					}
			}

		for (int i = 0; i < nl.getLength(); ++i) {
			Node childNode = nl.item(i);
				replaceTags(childNode, doc);
		}
	}

	private void parseIt(Node node, PrintWriter pw, int startingDepth) {
		if (isIncludeTag(node)) {
			recursiveParse(node, pw, curDepth + startingDepth, false);
			curDepth = 1;
			return;
		}
		NodeList nl = node.getChildNodes();
		for (int i = 0; i < nl.getLength(); ++i) {
			Node childNode = nl.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				parseIt(childNode, pw, startingDepth);
			}
		}
	}

	private boolean isIncludeTag(Node n) {
		for (String k : includeTagsKeys) {
			if (n.getNodeName().equals(prop.getProperty(k)) || getNodePath(n).equals(prop.getProperty(k))) {
				if (prop.getProperty(k + ".forceDepth") != null)
					curDepth = Integer.parseInt(prop.getProperty(k + ".forceDepth"));
				return true;
			}
		}
		return false;
	}

	private Boolean isHeader(Node n) {
		for (String k : headerTagKeys) {
			if (n.getNodeName().equals(prop.getProperty(k))) {
				skippingSection = false;
				return true;
			}
		}
		return false;
	}

	private Boolean isSkipTag(Node n) {
		boolean excludeException = false;
		for (String k : skipTagKeys) {			
			//Checks for whether tag name/path is one on the skip list. If so, also checks for exclusions to the skip.
			if (n.getNodeName().equals(prop.getProperty(k)) || getNodePath(n).equals(prop.getProperty(k))) {
				for (String l : skipTagKeys) {
					if (l.startsWith(k + ".excludeOccurrence")){					   
							if(getOccurrence(n) == Integer.parseInt(prop.getProperty(l)))
						    return true;
						  excludeException = true;
					}
				}
				if(excludeException)
					return false;
				return true;
			}
			if (k.startsWith("skipTags.textMatch") && isHeader(n)
					&& n.getTextContent().contains(prop.getProperty(k))) {
				skippingSection = true;
				return true;
			}
		}
		return false;
	}

	private Boolean isSection(Node n) {
		if (n.getNodeName().equals(prop.getProperty("sectionTag")))
			return true;
		return false;
	}

	private Boolean isPara(Node n) {
		if (n.getNodeName().equals(prop.getProperty("paragraphTag")))
			return true;
		return false;
	}

	private Boolean isFigure(Node n) {
		if (n.getNodeName().equals(prop.getProperty("figureTag")))
			return true;
		return false;
	}

	private Boolean isTable(Node n) {
		if (n.getNodeName().equals(prop.getProperty("tableWrapperTag")))
			return true;
		return false;
	}

	private Boolean isList(Node n) {
		if (n.getNodeName().equals(prop.getProperty("listTag")))
			return true;
		return false;
	}

	private int getOccurrence(Node n) {
		int occurenceCount = 0;
		NodeList nl = n.getParentNode().getChildNodes();
		for(int i = 0; i < nl.getLength(); ++i){
			if(nl.item(i).equals(n))
				return occurenceCount;
			if(nl.item(i).getNodeName() == n.getNodeName())
				++occurenceCount;
		}
		
		return -1;
	}

	private void recursiveParse(Node n, PrintWriter pw, int depth, Boolean dummyHeaders) {
		pw.flush();
		NodeList nl = n.getChildNodes();
		int headerHit = 0;
		for (int i = 0; i < nl.getLength(); i++) {
			if (skippingSection && isHeader(nl.item(i)))
				skippingSection = false;
			if (isSkipTag(nl.item(i))) {
			} else if (isSection(nl.item(i))) {
				recursiveParse(nl.item(i), pw, depth + 1, dummyHeaders);
			} else if (isHeader(nl.item(i)) && !skippingSection) {
				headerHit = 1;
				pw.println("<h" + depth + ">" + nl.item(i).getTextContent() + "</h" + depth + ">");
			} else if (isPara(nl.item(i)) && !skippingSection) {
				if (dummyHeaders == true)
					pw.println("<h" + (depth + 1) + ">" + i + "</h" + (depth + 1) + ">");
				pw.println(nodeToString(nl.item(i)));
			} else if (isFigure(nl.item(i)) && figsOn && !skippingSection) {
				pw.println(nl.item(i).getTextContent());
			} else if (isTable(nl.item(i)) && tablesOn && !skippingSection) {
				tableParse(nl.item(i), pw, depth);
			} else if (isList(nl.item(i)) && listsOn && !skippingSection) {
				System.out.println("here");
				pw.println(prop.getProperty("htmlListTag.open"));
				listParse(nl.item(i), pw, depth);
				pw.println(prop.getProperty("htmlListTag.close"));
			} else {
				recursiveParse(nl.item(i), pw, depth + headerHit, dummyHeaders);
			}

		}
	}

	private void listParse(Node n, PrintWriter pw, int depth) {
		NodeList nl = n.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			String nodeName = nl.item(i).getNodeName();
			if (nodeName.equals(prop.getProperty("listElementTag"))) {
				pw.println(prop.getProperty("htmlListEntryTag.open"));
				recursiveParse(nl.item(i), pw, depth, false);
				pw.println(prop.getProperty("htmlListEntryTag.close"));
			}
		}
	}

	private void tableParse(Node n, PrintWriter pw, int depth) {
		NodeList nl = n.getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			String nodeName = nl.item(i).getNodeName();
			if (nodeName.equals(prop.getProperty("tableTag"))) {
				pw.println(prop.getProperty("htmlTableTag.open"));
				tableParse(nl.item(i), pw, depth);
				pw.println(prop.getProperty("htmlTableTag.close"));
			} else if (isHeader(nl.item(i))) {
				pw.println("<h6>" + nl.item(i).getTextContent() + "</h6>");
			} else if (nodeName.equals(prop.getProperty("tableEntryTag"))) {
				pw.println(prop.getProperty("htmlTableEntryTag.open"));
				pw.println(nl.item(i).getTextContent());
				pw.println(prop.getProperty("htmlTableEntryTag.close"));
			} else if (nodeName.equals(prop.getProperty("tableRowTag"))) {
				pw.println(prop.getProperty("htmlTableRowTag.open"));
				tableParse(nl.item(i), pw, depth);
				pw.println(prop.getProperty("htmlTableRowTag.close"));
			} else if (nodeName.equals(prop.getProperty("tableFooterTag"))) {
				recursiveParse(nl.item(i), pw, depth, false);
			} else {
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
			System.out.println("err: " + node.getTextContent());
			System.out.println("err: " + te.getMessage());
		}
		return sw.toString();
	}

	private void loadProperties() {
		try {
			String filename = "schema.properties";
			InputStream input = new FileInputStream(filename);
			prop.load(input);
			input.close();
			Set<Object> propobjects = prop.keySet();
			for (Object k : propobjects) {
				if (((String) k).startsWith("xmlReplaceTextTag.")) {
					xmlReplaceTagKeys.add((String) k);
				} else if (((String) k).startsWith("skipTags.")) {
					skipTagKeys.add((String) k);
				} else if (((String) k).startsWith("headerTags.")) {
					headerTagKeys.add((String) k);
				} else if (((String) k).startsWith("includeTags.")) {
					includeTagsKeys.add((String) k);
				}
			}
			if (prop.getProperty("tablesOn").equals("false"))
				tablesOn = false;
			if (prop.getProperty("listsOn").equals("false"))
				listsOn = false;
			if (prop.getProperty("figsOn").equals("false"))
				figsOn = false;
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	Converter() {
		prop = new Properties();
		xmlReplaceTagKeys = new HashSet<String>();
		skipTagKeys = new HashSet<String>();
		headerTagKeys = new HashSet<String>();
		includeTagsKeys = new HashSet<String>();
		curDepth = 1;
		skippingSection = false;
		tablesOn = true;
		listsOn = true;
		figsOn = true;
		loadProperties();
	}
}
