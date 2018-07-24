/*
 * This file is part of fflauncher.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * Copyright (C) hdsdi3g for hd3g.tv 2018
 * 
*/
package tv.hd3g.fflauncher;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEventLocator;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ffmpeg.ffprobe.FfprobeType;
import org.xml.sax.SAXException;

import tv.hd3g.execprocess.CommandLineProcessor.CommandLine;
import tv.hd3g.execprocess.ExecutableFinder;

public class FFprobe extends FFbase {
	private static Logger log = LogManager.getLogger();
	
	public FFprobe(ExecutableFinder exec_finder, CommandLine command_line) throws FileNotFoundException {
		super(exec_finder, command_line);
	}
	
	/**
	 * @see https://github.com/hdsdi3g/ffprobe-jaxb
	 */
	public static FfprobeType fromXML(String xml_content) throws JAXBException, ParserConfigurationException, SAXException, IOException {
		JAXBContext jc = JAXBContext.newInstance(FfprobeType.class.getPackageName());
		
		Unmarshaller unmarshaller = jc.createUnmarshaller();
		// prepare an error catcher if trouble are catched during import.
		unmarshaller.setEventHandler((ValidationEventHandler) e -> {
			ValidationEventLocator localtor = e.getLocator();
			log.warn("XML validation: " + e.getMessage() + " [s" + e.getSeverity() + "] at line " + localtor.getLineNumber() + ", column " + localtor.getColumnNumber() + " offset " + localtor.getOffset() + " node: " + localtor.getNode() + ", object " + localtor.getObject());
			return true;
		});
		
		DocumentBuilderFactory xmlDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder xmlDocumentBuilder = xmlDocumentBuilderFactory.newDocumentBuilder();
		xmlDocumentBuilder.setErrorHandler(null);
		
		Document document = xmlDocumentBuilder.parse(new ByteArrayInputStream(xml_content.getBytes(StandardCharsets.UTF_8)));
		
		JAXBElement<FfprobeType> result = unmarshaller.unmarshal(document, FfprobeType.class);
		return result.getValue();
	}
	
	// TODO implements ffprobe
	
	// TODO add recipe: ffprobe -print_format xml -show_streams -show_format -hide_banner -i <my-media-file>
	
}
