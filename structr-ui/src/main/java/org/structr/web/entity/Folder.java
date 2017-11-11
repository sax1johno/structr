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

import java.util.List;
import org.apache.chemistry.opencmis.commons.data.AllowableActions;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.structr.cmis.CMISInfo;
import org.structr.cmis.info.CMISDocumentInfo;
import org.structr.cmis.info.CMISFolderInfo;
import org.structr.cmis.info.CMISItemInfo;
import org.structr.cmis.info.CMISPolicyInfo;
import org.structr.cmis.info.CMISRelationshipInfo;
import org.structr.cmis.info.CMISSecondaryInfo;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.View;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.ModificationQueue;
import org.structr.core.notion.PropertySetNotion;
import org.structr.core.property.ConstantBooleanProperty;
import org.structr.core.property.EndNodes;
import org.structr.core.property.IntProperty;
import org.structr.core.property.Property;
import org.structr.core.property.PropertyMap;
import org.structr.core.property.StartNode;
import org.structr.files.cmis.config.StructrFolderActions;
import org.structr.schema.SchemaService;
import org.structr.web.entity.relation.Files;
import org.structr.web.entity.relation.Folders;
import org.structr.web.entity.relation.Images;
import org.structr.web.entity.relation.UserHomeDir;

public interface Folder extends AbstractFile, CMISInfo, CMISFolderInfo {

	static class Impl { static { SchemaService.registerMixinType(Folder.class); }}

	public static final Property<List<Folder>>   folders                 = new EndNodes<>("folders", Folders.class, new PropertySetNotion(id, name));
	public static final Property<List<File>> files                       = new EndNodes<>("files", Files.class, new PropertySetNotion(id, name));
	public static final Property<List<Image>>    images                  = new EndNodes<>("images", Images.class, new PropertySetNotion(id, name));
	public static final Property<Boolean>        isFolder                = new ConstantBooleanProperty("isFolder", true);
	public static final Property<User>           homeFolderOfUser        = new StartNode<>("homeFolderOfUser", UserHomeDir.class);

	public static final Property<Integer>        position                = new IntProperty("position").cmis().indexed();

	public static final View publicView = new View(Folder.class, PropertyView.Public,
			id, type, name, owner, isFolder, folders, files, parentId, visibleToPublicUsers, visibleToAuthenticatedUsers
	);

	public static final View uiView = new View(Folder.class, PropertyView.Ui,
			parent, owner, folders, files, images, isFolder, includeInFrontendExport
	);

	@Override
	default boolean onCreation(final SecurityContext securityContext, final ErrorBuffer errorBuffer) throws FrameworkException {

		if (AbstractFile.super.onCreation(securityContext, errorBuffer)) {

			setHasParent();

			return true;
		}

		return false;
	}

	@Override
	default boolean onModification(final SecurityContext securityContext, final ErrorBuffer errorBuffer, final ModificationQueue modificationQueue) throws FrameworkException {

		if (AbstractFile.super.onModification(securityContext, errorBuffer, modificationQueue)) {

			setHasParent();

			return true;
		}

		return false;
	}

	@Override
	default boolean isValid(final ErrorBuffer errorBuffer) {
		return AbstractFile.super.isValid(errorBuffer);
	}

	// ----- CMIS support -----
	@Override
	default CMISInfo getCMISInfo() {
		return this;
	}

	@Override
	default BaseTypeId getBaseTypeId() {
		return BaseTypeId.CMIS_FOLDER;
	}

	@Override
	default CMISFolderInfo getFolderInfo() {
		return this;
	}

	@Override
	default CMISDocumentInfo getDocumentInfo() {
		return null;
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
	default String getPath() {
		return getProperty(AbstractFile.path);
	}

	@Override
	default AllowableActions getAllowableActions() {
		return StructrFolderActions.getInstance();
	}

	@Override
	default String getChangeToken() {

		// versioning not supported yet.
		return null;
	}

	default void setHasParent() throws FrameworkException {

		// set property as super user
		setProperties(SecurityContext.getSuperUserInstance(), new PropertyMap(hasParent, getProperty(parentId) != null));
	}
}
