package com.sun.tools.xjc.addon.xew;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.builder.ToStringBuilder;

/**
 * Global configuration.
 */
public class GlobalConfiguration extends CommonConfiguration {

	private PrintWriter		   summaryWriter = null;

	private List<ControlEntry> controlList	 = new ArrayList<ControlEntry>();

	private static class ControlEntry {
		Pattern		pattern;

		ControlMode	controlMode;

		ControlEntry(Pattern pattern, ControlMode controlMode) {
			this.pattern = pattern;
			this.controlMode = controlMode;
		}

		@Override
		public String toString() {
			return "ControlEntry[" + pattern + "=" + controlMode + "]";
		}
	}

	/**
	 * Parse the given control file and initialize this config appropriately.
	 */
	void readControlFile(String fileName) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		try {
			controlList.clear();

			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();

				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}

				int separatorIndex = line.indexOf('=');

				if (separatorIndex <= 0) {
					logger.warn("Control file line \"" + line + "\" is invalid as does not have '=' separator.");
					continue;
				}

				String className = line.substring(0, separatorIndex);
				ControlMode controlMode;
				try {
					controlMode = ControlMode.valueOf(line.substring(separatorIndex + 1).trim().toUpperCase());
				}
				catch (IllegalArgumentException e) {
					logger.warn("Control file line \"" + line + "\" is invalid as control mode is unknown.");
					continue;
				}

				controlList.add(new ControlEntry(
				            className.startsWith("/") && className.endsWith("/") && className.length() > 2
				                        ? Pattern.compile(className.substring(1, className.length() - 1))
				                        : Pattern.compile(className, Pattern.LITERAL),
				            controlMode));
			}
			configurationValues.put(ConfigurationOption.CONTROL, fileName);
		}
		finally {
			reader.close();
		}
	}

	public String getControlFileName() {
		return (String) configurationValues.get(ConfigurationOption.CONTROL);
	}

	/**
	 * Returns {@code true} if given candidate class should be considered.
	 */
	public boolean isClassIncluded(String className) {
		boolean inclusionForced = false;
		boolean exclusionForced = false;

		for (ControlEntry controlEntry : controlList) {
			if (controlEntry.pattern.matcher(className).matches()) {
				switch (controlEntry.controlMode) {
				case INCLUDE:
					inclusionForced = true;
					break;
				case EXCLUDE:
					exclusionForced = true;
					break;
				default:
				}
			}
		}

		if (inclusionForced) {
			return true;
		}

		if (exclusionForced) {
			return false;
		}

		return true;
	}

	/**
	 * Returns {@code true} if given candidate class should not be removed from model.
	 */
	public boolean isClassUnmarkedForRemoval(String className) {
		for (ControlEntry controlEntry : controlList) {
			if (controlEntry.pattern.matcher(className).matches() && controlEntry.controlMode == ControlMode.KEEP) {
				return true;
			}
		}

		return false;
	}

	@Override
	protected ToStringBuilder appendProperties(ToStringBuilder builder) {
		super.appendProperties(builder);
		builder.append("controlList", controlList);

		return builder;
	}

	public void initSummaryWriter(String fileName) throws FileNotFoundException {
		closeSummary();
		summaryWriter = new PrintWriter(new FileOutputStream(fileName));
		configurationValues.put(ConfigurationOption.SUMMARY, fileName);
	}

	public String getSummaryFileName() {
		return (String) configurationValues.get(ConfigurationOption.SUMMARY);
	}

	//
	// Logging helpers
	//

	public void writeSummary(String s) {
		if (summaryWriter != null) {
			summaryWriter.println(s);
		}

		logger.info(s);
	}

	public void closeSummary() {
		if (summaryWriter != null) {
			summaryWriter.close();
		}
		configurationValues.remove(ConfigurationOption.SUMMARY);
	}
}
