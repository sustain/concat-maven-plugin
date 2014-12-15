package com.github.bomas.concat;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.*;
import java.util.List;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which concatenates several files and creates a new file as specified.
 * 
 * @Mojo( name = "concat" )
 * @goal concat
 * 
 * @Mojo( defaultPhase = "process-sources" )
 * @phase process-sources
 */
public class ConcatMojo extends AbstractMojo {
	
	
	/**
	 * The resulting file
	 * 
	 * @parameter
	 * @required
	 */
	private File outputFile;
	
	
	/**
	 * Files to concatenate
	 * 
	 * @parameter
	 * @required
	 */
	private List<File> concatFiles;
	
	
	/**
	 * Append newline after each concatenation
	 * 
	 * @parameter
	 */
	private boolean appendNewline=false;

	
	/**
	 * Skip non-existent or unreadable files
	 *
	 * @parameter
	 */
	private boolean skipUnreadableFiles=true;


	/**
	 * output before each file. The string {FILENAME} wil be replaced by the current file name. The string {NL} will be replaced by a line separator
	 *
	 * @parameter
	 */
	private String fileHeader=null;

	/**
	 * output after each file. The string {FILENAME} wil be replaced by the current file name. The string {NL} will be replaced by a line separator
	 *
	 * @parameter
	 */
	private String fileFooter=null;


	/* (non-Javadoc)
	 * @see org.apache.maven.plugin.AbstractMojo#execute()
	 */
	public void execute() throws MojoExecutionException {
        String lineSeparator = System.getProperty("line.separator");
		if(validate()){
			getLog().info("Going to concatenate files to destination file: " + outputFile.getAbsolutePath());
			try {
                FileUtils.writeStringToFile(outputFile, "", false);
				for(File inputFile : concatFiles){
                    if (skipUnreadableFiles) {
                        if (!inputFile.exists()) {
                            getLog().warn("Skipping non-existent file: " + inputFile.getAbsolutePath());
                            continue;
                        } else if (!inputFile.canRead()) {
                            getLog().warn("Skipping unreadable file: " + inputFile.getAbsolutePath());
                            continue;
                        }
                    }
					getLog().debug("Concatenating file: " + inputFile.getAbsolutePath());
                    String input;
                    try (InputStream inputStream = new FileInputStream(inputFile)) {
                        BOMInputStream bomInputStream = new BOMInputStream(inputStream);
                        ByteOrderMark bom = bomInputStream.getBOM();
                        String charsetName = bom == null ? "UTF-8" : bom.getCharsetName();
                        input = IOUtils.toString(bomInputStream, charsetName);
                    }
                    if (fileHeader != null) {
                        String header = fileHeader.replace("{FILENAME}", inputFile.getName()).replace("{NL}", lineSeparator);
                        FileUtils.writeStringToFile(outputFile, header, true);
                    }
					FileUtils.writeStringToFile(outputFile, input, true);
                    if (fileFooter != null) {
                        String footer = fileFooter.replace("{FILENAME}", inputFile.getName()).replace("{NL}", lineSeparator);
                        FileUtils.writeStringToFile(outputFile, footer, true);
                    }
					if(appendNewline){
                        FileUtils.writeStringToFile(outputFile, lineSeparator, true);
					}

				}
			}catch(IOException e){
				throw new MojoExecutionException("Failed to concatenate", e);
			}
		}
	}
	
	private boolean validate() throws MojoExecutionException{
		if(outputFile == null){
			throw new MojoExecutionException("Please specify a correct outPutFile");
		}
		
		if(concatFiles == null || concatFiles.size() ==0){
			throw new MojoExecutionException("Please specify the file(s) to concatenate");
		}else if (!skipUnreadableFiles) {
			for(File file : concatFiles){
				if(!file.exists()){
					throw new MojoExecutionException(file.getAbsolutePath() + " does not exists.");
				}
			}
				
		}
		
		return true;
	}
}
