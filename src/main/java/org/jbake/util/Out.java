package org.jbake.util;

import java.io.IOException;
import java.io.Writer;

/**
 * Target that can be written to {@link Writer}.
 * 
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 * @version $Revision: $ $Author: $ $Date: $
 */
public interface Out {

	/**
	 * Opens a {@link Writer} to this target.
	 * 
	 * @throws IOException If opening fails.
	 */
	Writer getWriter() throws IOException;

}
