/**
 * Copyright (C) 2010-2017 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.web.entity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.xml.stream.XMLStreamException;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.config.Settings;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.info.CMISDocumentInfo;
import org.structr.cmis.info.CMISFolderInfo;
import org.structr.cmis.info.CMISItemInfo;
import org.structr.cmis.info.CMISPolicyInfo;
import org.structr.cmis.info.CMISRelationshipInfo;
import org.structr.cmis.info.CMISSecondaryInfo;
import org.structr.common.Permission;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.common.error.UnlicensedException;
import org.structr.common.fulltext.FulltextIndexer;
import org.structr.common.fulltext.Indexable;
import static org.structr.common.fulltext.Indexable.extractedContent;
import org.structr.core.Export;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Favoritable;
import org.structr.core.entity.Principal;
import org.structr.core.function.Functions;
import org.structr.core.graph.ModificationEvent;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.Tx;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.ConstantBooleanProperty;
import org.structr.core.property.IntProperty;
import org.structr.core.property.LongProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.core.script.Scripting;
import org.structr.files.cmis.config.StructrFileActions;
import org.structr.rest.common.XMLStructureAnalyzer;
import org.structr.schema.SchemaService;
import org.structr.schema.action.ActionContext;
import org.structr.schema.action.Function;
import org.structr.schema.action.JavaScriptSource;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.common.RenderContext;
import org.structr.web.entity.relation.MinificationSource;
import org.structr.web.entity.relation.UserFavoriteFile;
import org.structr.web.importer.CSVFileImportJob;
import org.structr.web.importer.DataImportManager;
import org.structr.web.importer.XMLFileImportJob;
import org.structr.web.property.FileDataProperty;

/**
 */
public interface File extends AbstractFile, Indexable, Linkable, JavaScriptSource, CMISInfo, CMISDocumentInfo, Favoritable {

	static class Impl { static { SchemaService.registerMixinType(File.class); }}

	public static final Property<String> relativeFilePath                        = new StringProperty("relativeFilePath").systemInternal();
	public static final Property<Long> size                                      = new LongProperty("size").indexed().systemInternal();
	public static final Property<String> url                                     = new StringProperty("url");
	public static final Property<Long> checksum                                  = new LongProperty("checksum").indexed().unvalidated().systemInternal();
	public static final Property<Integer> cacheForSeconds                        = new IntProperty("cacheForSeconds").cmis();
	public static final Property<Integer> version                                = new IntProperty("version").indexed().systemInternal();
	public static final Property<String> base64Data                              = new FileDataProperty<>("base64Data");
	public static final Property<Boolean> isFile                                 = new ConstantBooleanProperty("isFile", true);
	public static final Property<List<AbstractMinifiedFile>> minificationTargets = new StartNodes<>("minificationTarget", MinificationSource.class);
	public static final Property<List<User>> favoriteOfUsers                     = new StartNodes<>("favoriteOfUsers", UserFavoriteFile.class);
	public static final Property<Boolean> isTemplate                             = new BooleanProperty("isTemplate");

	public static final View publicView = new View(File.class, PropertyView.Public,
		type, name, size, url, owner, path, isFile, visibleToPublicUsers, visibleToAuthenticatedUsers, includeInFrontendExport, isFavoritable, isTemplate
	);

	public static final View uiView = new View(File.class, PropertyView.Ui,
		type, relativeFilePath, size, url, parent, checksum, version, cacheForSeconds, owner, isFile, hasParent, includeInFrontendExport, isFavoritable, isTemplate
	);

	@Override
	default boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (AbstractFile.super.onCreation(securityContext, errorBuffer)) {

			final PropertyMap changedProperties = new PropertyMap();

			if (Settings.FilesystemEnabled.getValue() && !getProperty(AbstractFile.hasParent)) {

				final Folder workingOrHomeDir = getCurrentWorkingDir();
				if (workingOrHomeDir != null && getProperty(AbstractFile.parent) == null) {

					changedProperties.put(AbstractFile.parent, workingOrHomeDir);
				}
			}

			changedProperties.put(hasParent, getProperty(parentId) != null);

			setProperties(securityContext, changedProperties);

			return true;
		}

		return false;
	}

	@Override
	default boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (AbstractFile.super.onModification(securityContext, errorBuffer, modificationQueue)) {

			synchronized (this) {

				// save current security context
				final SecurityContext previousSecurityContext = getSecurityContext();

				// replace with SU context
				setSecurityContext(SecurityContext.getSuperUserInstance());

				// update metadata and parent as superuser
				FileHelper.updateMetadata(this, new PropertyMap(hasParent, getProperty(parentId) != null));

				// restore previous security context
				setSecurityContext(previousSecurityContext);
			}

			triggerMinificationIfNeeded(modificationQueue);

			return true;
		}

		return false;
	}

	@Override
	default void onNodeCreation() {

		final String uuid     = getUuid();
		final String filePath = File.getDirectoryPath(uuid) + "/" + uuid;

		try {
			unlockSystemPropertiesOnce();
			setProperties(getSecurityContext(), new PropertyMap(relativeFilePath, filePath));

		} catch (Throwable t) {

			logger.warn("Exception while trying to set relative file path {}: {}", new Object[]{filePath, t});

		}
	}

	@Override
	default void onNodeDeletion() {

		String filePath = null;
		try {
			final String path = getRelativeFilePath();

			if (path != null) {

				filePath = FileHelper.getFilePath(path);

				java.io.File toDelete = new java.io.File(filePath);

				if (toDelete.exists() && toDelete.isFile()) {

					toDelete.delete();
				}
			}

		} catch (Throwable t) {

			logger.debug("Exception while trying to delete file {}: {}", new Object[]{filePath, t});

		}

	}

	@Override
	default void afterCreation(SecurityContext securityContext) {

		try {

			final String filesPath        = Settings.FilesPath.getValue();
			final java.io.File fileOnDisk = new java.io.File(filesPath + "/" + getRelativeFilePath());

			if (fileOnDisk.exists()) {
				return;
			}

			fileOnDisk.getParentFile().mkdirs();

			try {

				fileOnDisk.createNewFile();

			} catch (IOException ex) {

				logger.error("Could not create file", ex);
				return;
			}

			FileHelper.updateMetadata(this, new PropertyMap(version, 0));

		} catch (FrameworkException ex) {

			logger.error("Could not create file", ex);
		}
	}

	@Override
	default public boolean isValid(final ErrorBuffer errorBuffer) {
		return AbstractFile.super.isValid(errorBuffer);
	}

	@Override
	default String getPath() {
		return FileHelper.getFolderPath(this);
	}

	@Export
	@Override
	default GraphObject getSearchContext(final String searchTerm, final int contextLength) {

		final String text = getProperty(extractedContent);
		if (text != null) {

			final FulltextIndexer indexer = StructrApp.getInstance(getSecurityContext()).getFulltextIndexer();
			return indexer.getContextObject(searchTerm, text, contextLength);
		}

		return null;
	}

	default void notifyUploadCompletion() {

		try {

			try (final Tx tx = StructrApp.getInstance().tx()) {

				synchronized (tx) {

					FileHelper.updateMetadata(this, new PropertyMap());

					tx.success();
				}
			}

			final FulltextIndexer indexer = StructrApp.getInstance(getSecurityContext()).getFulltextIndexer();
			indexer.addToFulltextIndex(this);

		} catch (FrameworkException fex) {

			logger.warn("Unable to index " + this, fex);
		}
	}

	default String getRelativeFilePath() {

		return getProperty(File.relativeFilePath);

	}

	default String getUrl() {

		return getProperty(File.url);

	}

	@Override
	default String getContentType() {

		return getProperty(File.contentType);

	}

	@Override
	default Long getSize() {

		return getProperty(size);

	}

	default Long getChecksum() {

		return getProperty(checksum);

	}

	default String getFormattedSize() {

		return FileUtils.byteCountToDisplaySize(getSize());

	}

	public static String getDirectoryPath(final String uuid) {

		return (uuid != null)
			? uuid.substring(0, 1) + "/" + uuid.substring(1, 2) + "/" + uuid.substring(2, 3) + "/" + uuid.substring(3, 4)
			: null;

	}

	default void increaseVersion() throws FrameworkException {

		final Integer _version = getProperty(File.version);

		unlockSystemPropertiesOnce();
		if (_version == null) {

			setProperties(getSecurityContext(), new PropertyMap(File.version, 1));

		} else {

			setProperties(getSecurityContext(), new PropertyMap(File.version, _version + 1));
		}
	}

	default void triggerMinificationIfNeeded(ModificationQueue modificationQueue) throws FrameworkException {

		final List<AbstractMinifiedFile> targets = getProperty(minificationTargets);

		if (!targets.isEmpty()) {

			// only run minification if the file version changed
			boolean versionChanged = false;
			for (ModificationEvent modState : modificationQueue.getModificationEvents()) {

				if (getUuid().equals(modState.getUuid())) {

					versionChanged = versionChanged ||
							modState.getRemovedProperties().containsKey(File.version) ||
							modState.getModifiedProperties().containsKey(File.version) ||
							modState.getNewProperties().containsKey(File.version);
				}
			}

			if (versionChanged) {

				for (AbstractMinifiedFile minifiedFile : targets) {

					try {
						minifiedFile.minify();
					} catch (IOException ex) {
						logger.warn("Could not automatically update minification target: ".concat(minifiedFile.getName()), ex);
					}

				}

			}

		}

	}

	@Override
	default InputStream getInputStream() {

		final String relativeFilePath = getRelativeFilePath();

		if (relativeFilePath != null) {

			final String filePath = FileHelper.getFilePath(relativeFilePath);

			FileInputStream fis = null;
			try {

				java.io.File fileOnDisk = new java.io.File(filePath);

				// Return file input stream
				fis = new FileInputStream(fileOnDisk);

				if (getProperty(isTemplate)) {

					boolean editModeActive = false;
					if (getSecurityContext().getRequest() != null) {
						final String editParameter = getSecurityContext().getRequest().getParameter("edit");
						if (editParameter != null) {
							editModeActive = !RenderContext.EditMode.NONE.equals(RenderContext.editMode(editParameter));
						}
					}

					if (!editModeActive) {

						final String content = IOUtils.toString(fis, "UTF-8");

						try {

							final String result = Scripting.replaceVariables(new ActionContext(getSecurityContext()), this, content);
							return IOUtils.toInputStream(result, "UTF-8");

						} catch (Throwable t) {

							logger.warn("Scripting error in {}:\n{}", getUuid(), content, t);
						}
					}
				}

				return fis;

			} catch (FileNotFoundException e) {
				logger.debug("File not found: {}", new Object[]{relativeFilePath});

				if (fis != null) {

					try {

						fis.close();

					} catch (IOException ignore) {}

				}
			} catch (IOException ex) {
				java.util.logging.Logger.getLogger(File.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		return null;
	}

	default FileOutputStream getOutputStream() {
		return getOutputStream(true, false);
	}

	default FileOutputStream getOutputStream(final boolean notifyIndexerAfterClosing, final boolean append) {

		if (getProperty(isTemplate)) {

			logger.error("File is in template mode, no write access allowed: {}", path);
			return null;
		}

		final String path = getRelativeFilePath();
		if (path != null) {

			final String filePath = FileHelper.getFilePath(path);

			try {

				final java.io.File fileOnDisk = new java.io.File(filePath);
				fileOnDisk.getParentFile().mkdirs();

				// Return file output stream and save checksum and size after closing
				final FileOutputStream fos = new FileOutputStream(fileOnDisk, append) {

					private boolean closed = false;

					@Override
					public void close() throws IOException {

						if (closed) {
							return;
						}

						try (Tx tx = StructrApp.getInstance().tx()) {

							super.close();

							final String _contentType = FileHelper.getContentMimeType(File.this);

							final PropertyMap changedProperties = new PropertyMap();
							changedProperties.put(checksum, FileHelper.getChecksum(File.this));
							changedProperties.put(size, FileHelper.getSize(File.this));
							changedProperties.put(contentType, _contentType);

							if (StringUtils.startsWith(_contentType, "image") || ImageHelper.isImageType(getProperty(name))) {
								changedProperties.put(NodeInterface.type, Image.class.getSimpleName());
							}

							unlockSystemPropertiesOnce();
							setProperties(getSecurityContext(), changedProperties);

							increaseVersion();

							if (notifyIndexerAfterClosing) {
								notifyUploadCompletion();
							}

							tx.success();

						} catch (Throwable ex) {

							logger.error("Could not determine or save checksum and size after closing file output stream", ex);

						}

						closed = true;
					}
				};

				return fos;

			} catch (FileNotFoundException e) {

				logger.error("File not found: {}", path);
			}

		}

		return null;

	}

	default java.io.File getFileOnDisk() {

		final String path = getRelativeFilePath();
		if (path != null) {

			return new java.io.File(FileHelper.getFilePath(path));
		}

		return null;
	}

	default Path getPathOnDisk() {

		final String path = getRelativeFilePath();
		if (path != null) {

			return Paths.get(FileHelper.getFilePath(path));
		}

		return null;
	}

	@Export
	default Map<String, Object> getFirstLines(final Map<String, Object> parameters) {

		final Map<String, Object> result = new LinkedHashMap<>();
		final LineAndSeparator ls        = getFirstLines(getNumberOrDefault(parameters, "num", 3));
		final String separator           = ls.getSeparator();

		switch (separator) {

			case "\n":
				result.put("separator", "LF");
				break;

			case "\r":
				result.put("separator", "CR");
				break;

			case "\r\n":
				result.put("separator", "CR+LF");
				break;
		}

		result.put("lines", ls.getLine());

		return result;
	}

	@Export
	default Map<String, Object> getCSVHeaders(final Map<String, Object> parameters) throws FrameworkException {

		if ("text/csv".equals(getProperty(File.contentType))) {

			final Map<String, Object> map       = new LinkedHashMap<>();
			final Function<Object, Object> func = Functions.get("get_csv_headers");

			if (func != null) {

				try {

					final Object[] sources = new Object[4];
					String delimiter       = ";";
					String quoteChar       = "\"";
					String recordSeparator = "\n";

					if (parameters != null) {

						if (parameters.containsKey("delimiter"))       { delimiter       = parameters.get("delimiter").toString(); }
						if (parameters.containsKey("quoteChar"))       { quoteChar       = parameters.get("quoteChar").toString(); }
						if (parameters.containsKey("recordSeparator")) { recordSeparator = parameters.get("recordSeparator").toString(); }
					}

					// allow web-friendly specification of line endings
					switch (recordSeparator) {

						case "CR+LF":
							recordSeparator = "\r\n";
							break;

						case "CR":
							recordSeparator = "\r";
							break;

						case "LF":
							recordSeparator = "\n";
							break;

						case "TAB":
							recordSeparator = "\t";
							break;
					}

					sources[0] = getFirstLines(1).getLine();
					sources[1] = delimiter;
					sources[2] = quoteChar;
					sources[3] = recordSeparator;

					map.put("headers", func.apply(new ActionContext(getSecurityContext()), null, sources));

				} catch (UnlicensedException ex) {

					logger.warn("CSV module is not available.");
				}
			}

			return map;

		} else {

			throw new FrameworkException(400, "File format is not CSV");
		}
	}

	@Export
	default void doCSVImport(final Map<String, Object> parameters) throws FrameworkException {

		CSVFileImportJob job = new CSVFileImportJob(this, getSecurityContext().getUser(false), parameters);
		DataImportManager.getInstance().addJob(job);

	}

	@Export
	default String getXMLStructure() throws FrameworkException {

		final String contentType = getProperty(File.contentType);

		if ("text/xml".equals(contentType) || "application/xml".equals(contentType)) {

			try (final Reader input = new InputStreamReader(getInputStream())) {

				final XMLStructureAnalyzer analyzer = new XMLStructureAnalyzer(input);
				final Gson gson                     = new GsonBuilder().setPrettyPrinting().create();

				return gson.toJson(analyzer.getStructure(100));

			} catch (XMLStreamException | IOException ex) {
				ex.printStackTrace();
			}
		}

		return null;
	}

	@Export
	default void doXMLImport(final Map<String, Object> config) throws FrameworkException {

		XMLFileImportJob job = new XMLFileImportJob(this, getSecurityContext().getUser(false), config);
		DataImportManager.getInstance().addJob(job);

	}

	default Folder getCurrentWorkingDir() {

		final Principal _owner  = getProperty(owner);
		Folder workingOrHomeDir = null;

		if (_owner != null && _owner instanceof User) {

			workingOrHomeDir = _owner.getProperty(User.workingDirectory);
			if (workingOrHomeDir == null) {

				workingOrHomeDir = _owner.getProperty(User.homeDirectory);
			}
		}

		return workingOrHomeDir;
	}

	default int getNumberOrDefault(final Map<String, Object> data, final String key, final int defaultValue) {

		final Object value = data.get(key);

		if (value != null) {

			// try number
			if (value instanceof Number) {
				return ((Number)value).intValue();
			}

			// try string
			if (value instanceof String) {
				try { return Integer.valueOf((String)value); } catch (NumberFormatException nex) {}
			}
		}

		return defaultValue;
	}

	default LineAndSeparator getFirstLines(final int num) {

		final StringBuilder lines = new StringBuilder();
		int separator[]           = new int[10];
		int separatorLength       = 0;

		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(getInputStream(), "utf-8"))) {

			int[] buf = new int[10010];

			int ch          = reader.read();
			int i           = 0;
			int l           = 0;

			// break on file end or if max char or line count is reached
			while (ch != -1 && i < 10000 && l < num) {

				switch (ch) {

					// CR
					case 13:

						// take only first line ending separator into account
						if (separator.length < 1) {

							// collect first separator char
							separator[separatorLength++] = ch;
						}

						// check next char only in case of CR
						ch = reader.read();

						// next char is LF ?
						if (ch == 10) {

							// CR + LF as line ending, collect second separator char
							separator[separatorLength++] = ch;

						} else {

							// CR only - do nothing
						}

						// append LF as line ending for display purposes
						buf[i++] = '\n';

						// add line to output
						lines.append(new String(buf, 0, i));

						// reset buffer
						buf = new int[10010];
						i=0;
						l++;

						break;

					// LF
					case 10:

						// take only first line ending separator into account
						if (separator.length < 1) {

							// collect first separator char
							separator[separatorLength++] = ch;
						}

						// must be LF only because two LF have to be ignored as empty line
						buf[i++] = '\n';

						// add line to output
						lines.append(new String(buf, 0, i));

						// reset buffer
						buf = new int[10010];
						i=0;
						l++;

						break;

					default:

						// no CR, no LF: Just add char
						buf[i++] = ch;
						break;
				}

				ch = reader.read();
			}

			lines.append(new String(buf, 0, i));

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return new LineAndSeparator(lines.toString(), new String(separator, 0, separatorLength));
	}

	// ----- interface JavaScriptSource -----
	@Override
	default String getJavascriptLibraryCode() {

		try (final InputStream is = getInputStream()) {

			return IOUtils.toString(new InputStreamReader(is));

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}

		return null;
	}

	// ----- CMIS support -----
	@Override
	default CMISInfo getCMISInfo() {
		return this;
	}

	@Override
	default BaseTypeId getBaseTypeId() {
		return BaseTypeId.CMIS_DOCUMENT;
	}

	@Override
	default CMISFolderInfo getFolderInfo() {
		return null;
	}

	@Override
	default CMISDocumentInfo getDocumentInfo() {
		return this;
	}

	@Override
	default CMISItemInfo geItemInfo() {
		return null;
	}

	@Override
	default CMISRelationshipInfo getRelationshipInfo() {
		return null;
	}

	@Override
	default CMISPolicyInfo getPolicyInfo() {
		return null;
	}

	@Override
	default CMISSecondaryInfo getSecondaryInfo() {
		return null;
	}

	@Override
	default String getParentId() {
		return getProperty(File.parentId);
	}

	@Override
	default AllowableActions getAllowableActions() {
		return new StructrFileActions(isImmutable());
	}

	@Override
	default String getChangeToken() {

		// versioning not supported yet.
		return null;
	}

	@Override
	default boolean isImmutable() {

		final Principal _owner = getOwnerNode();
		if (_owner != null) {

			return !_owner.isGranted(Permission.write, getSecurityContext());
		}

		return true;
	}

	// ----- interface Favoritable -----
	@Override
	default String getContext() {
		return getProperty(File.path);
	}

	@Override
	default String getFavoriteContent() {

		try (final InputStream is = getInputStream()) {

			return IOUtils.toString(is, "utf-8");

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}

		return null;
	}

	@Override
	default String getFavoriteContentType() {
		return getContentType();
	}

	@Override
	default void setFavoriteContent(final String content) throws FrameworkException {

		try (final OutputStream os = getOutputStream(true, false)) {

			IOUtils.write(content, os, Charset.defaultCharset());
			os.flush();

		} catch (IOException ioex) {
			logger.warn("", ioex);
		}
	}

	// ----- nested classes -----
	class LineAndSeparator {

		private String line      = null;
		private String separator = null;

		public LineAndSeparator(final String line, final String separator) {
			this.line      = line;
			this.separator = separator;
		}

		public String getLine() {
			return line;
		}

		public String getSeparator() {
			return separator;
		}
	}
}
