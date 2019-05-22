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
package org.structr.web.common;

import java.util.Set;
import org.structr.api.service.LicenseManager;
import org.structr.core.datasources.DataSources;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.function.Functions;
import org.structr.module.StructrModule;
import org.structr.schema.SourceFile;
import org.structr.schema.action.Actions;
import org.structr.web.datasource.CypherGraphDataSource;
import org.structr.web.datasource.FunctionDataSource;
import org.structr.web.datasource.IdRequestParameterGraphDataSource;
import org.structr.web.datasource.RestDataSource;
import org.structr.web.datasource.XPathGraphDataSource;
import org.structr.web.function.AddHeaderFunction;
import org.structr.web.function.AppendContentFunction;
import org.structr.web.function.BarcodeFunction;
import org.structr.web.function.ConfirmationKeyFunction;
import org.structr.web.function.CopyFileContentsFunction;
import org.structr.web.function.CreateArchiveFunction;
import org.structr.web.function.EscapeHtmlFunction;
import org.structr.web.function.FromJsonFunction;
import org.structr.web.function.FromXmlFunction;
import org.structr.web.function.GetContentFunction;
import org.structr.web.function.GetRequestHeaderFunction;
import org.structr.web.function.GetSessionAttributeFunction;
import org.structr.web.function.HttpDeleteFunction;
import org.structr.web.function.HttpGetFunction;
import org.structr.web.function.HttpHeadFunction;
import org.structr.web.function.HttpPostFunction;
import org.structr.web.function.HttpPutFunction;
import org.structr.web.function.IncludeChildFunction;
import org.structr.web.function.IncludeFunction;
import org.structr.web.function.IsLocaleFunction;
import org.structr.web.function.LogEventFunction;
import org.structr.web.function.MaintenanceFunction;
import org.structr.web.function.ParseFunction;
import org.structr.web.function.RemoveSessionAttributeFunction;
import org.structr.web.function.RenderFunction;
import org.structr.web.function.ScheduleFunction;
import org.structr.web.function.SendHtmlMailFunction;
import org.structr.web.function.SendPlaintextMailFunction;
import org.structr.web.function.SetContentFunction;
import org.structr.web.function.SetDetailsObjectFunction;
import org.structr.web.function.SetResponseCodeFunction;
import org.structr.web.function.SetResponseHeaderFunction;
import org.structr.web.function.SetSessionAttributeFunction;
import org.structr.web.function.StripHtmlFunction;
import org.structr.web.function.ToGraphObjectFunction;
import org.structr.web.function.ToJsonFunction;
import org.structr.web.function.UnescapeHtmlFunction;

/**
 */
public class UiModule implements StructrModule {

	@Override
	public void onLoad(final LicenseManager licenseManager) {

		DataSources.put(true, "ui", "idRequestParameterDataSource", new IdRequestParameterGraphDataSource("nodeId"));
		DataSources.put(true, "ui", "restDataSource",               new RestDataSource());
		DataSources.put(true, "ui", "cypherDataSource",             new CypherGraphDataSource());
		DataSources.put(true, "ui", "functionDataSource",           new FunctionDataSource());
		DataSources.put(true, "ui", "xpathDataSource",              new XPathGraphDataSource());
	}

	@Override
	public void registerModuleFunctions(final LicenseManager licenseManager) {

		Functions.put(licenseManager, new EscapeHtmlFunction());
		Functions.put(licenseManager, new UnescapeHtmlFunction());
		Functions.put(licenseManager, new StripHtmlFunction());
		Functions.put(licenseManager, new FromJsonFunction());
		Functions.put(licenseManager, new ToJsonFunction());
		Functions.put(licenseManager, new ToGraphObjectFunction());
		Functions.put(licenseManager, new IncludeFunction());
		Functions.put(licenseManager, new IncludeChildFunction());
		Functions.put(licenseManager, new RenderFunction());
		Functions.put(licenseManager, new SetDetailsObjectFunction());
		Functions.put(licenseManager, new ConfirmationKeyFunction());

		Functions.put(licenseManager, new SendHtmlMailFunction());
		Functions.put(licenseManager, new SendPlaintextMailFunction());
		Functions.put(licenseManager, new GetContentFunction());
		Functions.put(licenseManager, new SetContentFunction());
		Functions.put(licenseManager, new AppendContentFunction());
		Functions.put(licenseManager, new CopyFileContentsFunction());
		Functions.put(licenseManager, new SetSessionAttributeFunction());
		Functions.put(licenseManager, new GetSessionAttributeFunction());
		Functions.put(licenseManager, new RemoveSessionAttributeFunction());
		Functions.put(licenseManager, new IsLocaleFunction());

		Functions.put(licenseManager, new LogEventFunction());

		Functions.put(licenseManager, new HttpGetFunction());
		Functions.put(licenseManager, new HttpHeadFunction());
		Functions.put(licenseManager, new HttpPostFunction());
		Functions.put(licenseManager, new HttpPutFunction());
		Functions.put(licenseManager, new HttpDeleteFunction());
		Functions.put(licenseManager, new AddHeaderFunction());
		Functions.put(licenseManager, new SetResponseHeaderFunction());
		Functions.put(licenseManager, new SetResponseCodeFunction());
		Functions.put(licenseManager, new GetRequestHeaderFunction());
		Functions.put(licenseManager, new FromXmlFunction());
		Functions.put(licenseManager, new ParseFunction());
		Functions.put(licenseManager, new CreateArchiveFunction());
		Functions.put(licenseManager, new ScheduleFunction());
		Functions.put(licenseManager, new MaintenanceFunction());
		Functions.put(licenseManager, new BarcodeFunction());
	}

	@Override
	public String getName() {
		return "ui";
	}

	@Override
	public Set<String> getDependencies() {
		return null;
	}

	@Override
	public Set<String> getFeatures() {
		return null;
	}

	@Override
	public void insertImportStatements(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public void insertSourceCode(final AbstractSchemaNode schemaNode, final SourceFile buf) {
	}

	@Override
	public void insertSaveAction(final AbstractSchemaNode schemaNode, final SourceFile buf, final Actions.Type type) {
	}

	@Override
	public Set<String> getInterfacesForType(final AbstractSchemaNode schemaNode) {
		return null;
	}
}
