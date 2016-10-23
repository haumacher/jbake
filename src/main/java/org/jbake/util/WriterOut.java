package org.jbake.util;

import java.io.IOException;
import java.io.Writer;

/**
 * Trivial {@link Out} for a pre-allocated {@link Writer}.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public class WriterOut implements Out {

	private Writer writer;

	/**
	 * Creates a {@link WriterOut}.
	 *
	 * @param writer The {@link Writer} to write to.
	 */
	public WriterOut(Writer writer) {
		this.writer = writer;
	}

	@Override
	public Writer getWriter() throws IOException {
		return writer;
	}

}
