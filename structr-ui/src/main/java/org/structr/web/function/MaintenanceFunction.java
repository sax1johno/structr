/**
 * Copyright (C) 2010-2019 Structr GmbH
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
package org.structr.web.function;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.Command;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.FlushCachesCommand;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.Tx;
import org.structr.rest.resource.MaintenanceParameterResource;
import org.structr.schema.action.ActionContext;

public class MaintenanceFunction extends UiAdvancedFunction {

	private static final Logger logger = LoggerFactory.getLogger(MaintenanceFunction.class);

	public static final String ERROR_MESSAGE_MAINTENANCE    = "Usage: ${maintenance(command, params)}. Example: ${maintenance('rebuildIndex'))')}";
	public static final String ERROR_MESSAGE_MAINTENANCE_JS = "Usage: ${{Structr.maintenance(command, params)}}. Example: ${{Structr.maintenance('rebuildIndex', {})}}";

	@Override
	public String getName() {
		return "maintenance";
	}

	@Override
	public Object apply(final ActionContext ctx, final Object caller, final Object[] sources) {

		try {
			assertArrayHasMinLengthAndAllElementsNotNull(sources, 1);

			final SecurityContext securityContext = ctx.getSecurityContext();
			final Map<String, Object> params      = new LinkedHashMap<>();
			final String commandName              = (String)sources[0];

			if (sources.length > 1 && sources[1] instanceof Map) {

				params.putAll((Map)sources[1]);
			}

			if (securityContext.isSuperUser()) {

				final App app = StructrApp.getInstance(securityContext);

				// determine maintenance command from string
				final Class<Command> commandType = MaintenanceParameterResource.getMaintenanceCommandClass(commandName);
				if (commandType != null) {

					final Command command = app.command(commandType);
					if (command instanceof MaintenanceCommand) {

						final MaintenanceCommand cmd = (MaintenanceCommand)command;

						// flush caches if required
						if (cmd.requiresFlushingOfCaches()) {

							try {

								app.command(FlushCachesCommand.class).execute(Collections.EMPTY_MAP);

							} catch (FrameworkException ex) {
								logger.warn("Flush caches failed before maintenance command {}: {}", commandName, ex.getMessage());
							}
						}

						if (cmd.requiresEnclosingTransaction()) {

							try (final Tx tx = app.tx()) {

								cmd.execute(params);

								tx.success();

							} catch (FrameworkException ex) {
								logger.warn("Unable to execute maintenance command {}: {}", commandName, ex.getMessage());
							}

						} else {

							try {

								cmd.execute(params);

							} catch (FrameworkException ex) {
								logger.warn("Unable to execute maintenance command {}: {}", commandName, ex.getMessage());
							}
						}

					} else {

						logger.error("{} is not a maintenance command, cannot execute using maintenance()", commandName);
					}

				} else {

					logger.error("Cannot execute maintenance command {}, command doesn't exist", commandName);
				}

			} else {

				logger.error("Cannot execute maintenance command {} with non-admin user {}", commandName, securityContext.getUser(false));
			}

			return "";

		} catch (IllegalArgumentException e) {

			logParameterError(caller, sources, e.getMessage(), ctx.isJavaScriptContext());
			return null;
		}
	}

	@Override
	public String usage(boolean inJavaScriptContext) {
		return (inJavaScriptContext ? ERROR_MESSAGE_MAINTENANCE_JS : ERROR_MESSAGE_MAINTENANCE);
	}

	@Override
	public String shortDescription() {
		return "Executes a maintenance command.";
	}
}
