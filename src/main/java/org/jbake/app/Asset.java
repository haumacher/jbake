package org.jbake.app;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deals with assets (static files such as css, js or image files).
 *
 * @author Jonathan Bullock <jonbullock@gmail.com>
 *
 */
public class Asset {

    private static final Logger LOGGER = LoggerFactory.getLogger(Asset.class);

    private File source;
	private File destination;
	private CompositeConfiguration config;
	private final List<Throwable> errors = new LinkedList<Throwable>();
	private final boolean ignoreHidden;

	/**
	 * Creates an instance of Asset.
	 *
	 * @param source
	 * @param destination
	 */
	public Asset(File source, File destination, CompositeConfiguration config) {
		this.source = source;
		this.config = config;
		this.destination = destination;
		ignoreHidden = config.getBoolean(ConfigUtil.Keys.ASSET_IGNORE_HIDDEN, false);
	}

	/**
	 * Copy all files from supplied path.
	 *
	 * @param path	The starting path
	 */
	public void copy(File path) {
		File[] assets = path.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return !ignoreHidden || !file.isHidden();
			}
		});
		if (assets != null) {
			Arrays.sort(assets);
			for (int i = 0; i < assets.length; i++) {
				if (assets[i].isFile()) {
					File sourceFile = assets[i];
					File destFile = new File(sourceFile.getPath().replace(source.getPath()+File.separator+config.getString(ConfigUtil.Keys.ASSET_FOLDER), destination.getPath()));
					
					if (destFile.exists() && destFile.lastModified() >= sourceFile.lastModified()) {
						// Prevent copying identical files over and over.
						continue;
					}
					
					try {
						FileUtils.copyFile(sourceFile, destFile);
						LOGGER.info("Copying [" + assets[i].getPath() + "]: done!");
					} catch (IOException e) {
						LOGGER.error("Copying [" + assets[i].getPath() + "]: failed!", e);
						errors.add(e);
					}
				}

				if (assets[i].isDirectory()) {
					copy(assets[i]);
				}
			}
		}
	}

	public List<Throwable> getErrors() {
		return new ArrayList<Throwable>(errors);
	}

}
