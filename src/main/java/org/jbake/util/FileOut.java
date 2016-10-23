package org.jbake.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * {@link Out} targeting a {@link File}.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class FileOut implements Out {

	private final File path;
	private final Charset encoding;
	
	/**
	 * Creates a {@link FileOut}.
	 *
	 * @param path The {@link File} to write to.
	 * @param encoding The encoding to use for writing.
	 */
	public FileOut(File path, Charset encoding) {
		this.path = path;
		this.encoding = encoding;
	}

	@Override
	public Writer getWriter() throws IOException {
		return FileOut.createWriter(path, encoding);
	}

	/**
	 * Algorithm for opening a writer to a given {@link File}.
	 * 
	 * @param file The {@link File} to write to.
	 * @param encoding The encoding to use.
	 * @return A {@link Writer} writing to the given file.
	 * @throws IOException If creating the file fails.
	 */
	public static Writer createWriter(File file, Charset encoding) throws IOException,
			UnsupportedEncodingException, FileNotFoundException {
		if (!file.exists()) {
	        file.getParentFile().mkdirs();
	        file.createNewFile();
	    }
	
		return new OutputStreamWriter(new FileOutputStream(file), encoding);
	}

}
