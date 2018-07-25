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
	
	/**
	 * -pretty prettify the format of displayed values, make it more human readable
	 */
	public FFprobe setPretty() {
		command_line.ifHasNotParameter(() -> {
			command_line.addParameters("-pretty");
		}, "-pretty");
		return this;
	}
	
	public boolean isPretty() {
		return command_line.hasParameters("-pretty");
	}
	
	public enum FFPrintFormat {
		_default {
			public String toString() {
				return "default";
			}
		},
		compact, csv, flat, ini, json, xml;
	}
	
	/**
	 * -print_format format set the output printing format
	 */
	public FFprobe setPrintFormat(FFPrintFormat print_format) {
		command_line.ifHasNotParameter(() -> {
			command_line.addParameters("-print_format", print_format.toString().toLowerCase());
		}, "-print_format", "-of");
		return this;
	}
	
	public boolean hasPrintFormat() {
		return command_line.hasParameters("-print_format", "-of");
	}
	
	/**
	 * -show_format show format/container info
	 * @return
	 */
	public FFprobe setShowFormat() {
		command_line.ifHasNotParameter(() -> {
			command_line.addParameters("-show_format");
		}, "-show_format");
		return this;
	}
	
	public boolean isShowFormat() {
		return command_line.hasParameters("-show_format");
	}
	
	/**
	 * -show_data show packets data
	 */
	public FFprobe setShowData() {
		command_line.ifHasNotParameter(() -> {
			command_line.addParameters("-show_data");
		}, "-show_data");
		return this;
	}
	
	public boolean isShowData() {
		return command_line.hasParameters("-show_data");
	}
	
	/**
	 * -show_error show probing error
	 */
	public FFprobe setShowError() {
		command_line.ifHasNotParameter(() -> {
			command_line.addParameters("-show_error");
		}, "-show_error");
		return this;
	}
	
	public boolean isShowError() {
		return command_line.hasParameters("-show_error");
	}
	
	/**
	 * -show_frames show frames info
	 */
	public FFprobe setShowFrames() {
		command_line.ifHasNotParameter(() -> {
			command_line.addParameters("-show_frames");
		}, "-show_frames");
		return this;
	}
	
	public boolean isShowFrames() {
		return command_line.hasParameters("-show_frames");
	}
	
	/**
	 * -show_log show log
	 */
	public FFprobe setShowLog() {
		command_line.ifHasNotParameter(() -> {
			command_line.addParameters("-show_log");
		}, "-show_log");
		return this;
	}
	
	public boolean isShowLog() {
		return command_line.hasParameters("-show_log");
	}
	
	/**
	 * -show_packets show packets info
	 */
	public FFprobe setShowPackets() {
		command_line.ifHasNotParameter(() -> {
			command_line.addParameters("-show_packets");
		}, "-show_packets");
		return this;
	}
	
	public boolean isShowPackets() {
		return command_line.hasParameters("-show_packets");
	}
	
	/**
	 * -show_programs show programs info
	 */
	public FFprobe setShowPrograms() {
		command_line.ifHasNotParameter(() -> {
			command_line.addParameters("-show_programs");
		}, "-show_programs");
		return this;
	}
	
	public boolean isShowPrograms() {
		return command_line.hasParameters("-show_programs");
	}
	
	/**
	 * -show_streams show streams info
	 */
	public FFprobe setShowStreams() {
		command_line.ifHasNotParameter(() -> {
			command_line.addParameters("-show_streams");
		}, "-show_streams");
		return this;
	}
	
	public boolean isShowStreams() {
		return command_line.hasParameters("-show_streams");
	}
	
	/**
	 * -show_chapters show chapters info
	 */
	public FFprobe setShowChapters() {
		command_line.ifHasNotParameter(() -> {
			command_line.addParameters("-show_chapters");
		}, "-show_chapters");
		return this;
	}
	
	public boolean isShowChapters() {
		return command_line.hasParameters("-show_chapters");
	}
	
}
