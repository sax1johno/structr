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
package org.structr.media;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.common.SecurityContext;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.Export;
import org.structr.core.GraphObjectMap;
import org.structr.core.JsonInput;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.graph.Tx;
import org.structr.core.property.Property;
import org.structr.core.property.StringProperty;
import org.structr.rest.RestMethodResult;
import org.structr.web.common.FileHelper;
import org.structr.web.entity.FileMixin;

/**
 * A video whose binary data will be stored on disk.
 */
public class VideoFileMixin extends FileMixin implements VideoFile {

	private static final Logger logger = LoggerFactory.getLogger(VideoFileMixin.class.getName());

	@Override
	public boolean onCreation(SecurityContext securityContext, ErrorBuffer errorBuffer) throws FrameworkException {

		updateVideoInfo();

		return super.onCreation(securityContext, errorBuffer);
	}

	@Override
	public boolean onModification(SecurityContext securityContext, ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		updateVideoInfo();

		return super.onModification(securityContext, errorBuffer, modificationQueue);
	}

	public String getDiskFilePath(final SecurityContext securityContext) {

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final String path = getRelativeFilePath();

			tx.success();

			if (path != null) {
				return new java.io.File(FileHelper.getFilePath(path)).getAbsolutePath();
			}

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}

		return null;
	}

	@Export
	public void convert(final String scriptName, final String newFileName) throws FrameworkException {
		AVConv.newInstance(securityContext, this, newFileName).doConversion(scriptName);
	}

	@Export
	public void grab(final String scriptName, final String imageName, final long timeIndex) throws FrameworkException {
		AVConv.newInstance(securityContext, this, imageName).grabFrame(scriptName, imageName, timeIndex);
	}

	@Export
	public RestMethodResult getMetadata() throws FrameworkException {

		final Map<String, String> metadata = AVConv.newInstance(securityContext, this).getMetadata();
		final RestMethodResult result      = new RestMethodResult(200);
		final GraphObjectMap map           = new GraphObjectMap();

		for (final Entry<String, String> entry : metadata.entrySet()) {
			map.setProperty(new StringProperty(entry.getKey()), entry.getValue());
		}

		result.addContent(map);

		return result;
	}

	@Export
	public void setMetadata(final String key, final String value) throws FrameworkException {
		AVConv.newInstance(securityContext, this).setMetadata(key, value);
	}

	@Export
	public void setMetadata(final JsonInput metadata) throws FrameworkException {

		final Map<String, String> map = new LinkedHashMap<>();

		for (final Entry<String, Object> entry : metadata.entrySet()) {
			map.put(entry.getKey(), entry.getValue().toString());
		}

		AVConv.newInstance(securityContext, this).setMetadata(map);
	}

	@Export
	public void updateVideoInfo() {

		try (final Tx tx = StructrApp.getInstance(securityContext).tx()) {

			final Map<String, Object> info = AVConv.newInstance(securityContext, this).getVideoInfo();
			if (info != null && info.containsKey("streams")) {

				final List<Map<String, Object>> streams = (List<Map<String, Object>>)info.get("streams");
				for (final Map<String, Object> stream : streams) {

					final String codecType = (String)stream.get("codec_type");
					if (codecType != null) {

						if ("video".equals(codecType)) {

							setIfNotNull(videoCodecName, stream.get("codec_long_name"));
							setIfNotNull(videoCodec,     stream.get("codec_name"));
							setIfNotNull(pixelFormat,    stream.get("pix_fmt"));
							setIfNotNull(width,          toInt(stream.get("width")));
							setIfNotNull(height,         toInt(stream.get("height")));
							setIfNotNull(duration,       toDouble(stream.get("duration")));


						} else if ("audio".equals(codecType)) {

							setIfNotNull(audioCodecName, stream.get("codec_long_name"));
							setIfNotNull(audioCodec,     stream.get("codec_name"));
							setIfNotNull(sampleRate,     toInt(stream.get("sampleRate")));
						}
					}
				}
			}

			tx.success();

		} catch (FrameworkException fex) {
			logger.warn("", fex);
		}
	}

	private void setIfNotNull(final Property key, final Object value) throws FrameworkException {

		if (value != null) {
			setProperty(key, value);
		}
	}

	private Integer toInt(final Object value) {

		if (value instanceof Number) {
			return ((Number)value).intValue();
		}

		if (value instanceof String) {

			try {
				return Integer.valueOf((String)value);

			} catch (Throwable t) {
				return null;
			}
		}

		return null;
	}

	private Double toDouble(final Object value) {

		if (value instanceof Number) {
			return ((Number)value).doubleValue();
		}

		if (value instanceof String) {

			try {
				return Double.valueOf((String)value);

			} catch (Throwable t) {
				return null;
			}
		}

		return null;
	}
}
