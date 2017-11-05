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
import org.structr.api.config.Settings;
import org.structr.common.KeyAndClass;
import org.structr.common.PropertyView;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.Favoritable;
import org.structr.core.entity.Principal;
import org.structr.core.entity.relationship.Groups;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.BooleanProperty;
import org.structr.core.property.ConstantBooleanProperty;
import org.structr.core.property.EndNode;
import org.structr.core.property.EndNodes;
import org.structr.core.property.Property;
import org.structr.core.property.StartNode;
import org.structr.core.property.StartNodes;
import org.structr.core.property.StringProperty;
import org.structr.web.entity.relation.UserFavoriteFavoritable;
import org.structr.web.entity.relation.UserHomeDir;
import org.structr.web.entity.relation.UserImage;
import org.structr.web.entity.relation.UserWorkDir;
import org.structr.web.property.ImageDataProperty;
import org.structr.web.property.UiNotion;
import org.structr.core.entity.Group;

public interface User extends Principal {

	public static final Property<String>            confirmationKey           = new StringProperty("confirmationKey").indexed();
	public static final Property<Boolean>           backendUser               = new BooleanProperty("backendUser").indexed();
	public static final Property<Boolean>           frontendUser              = new BooleanProperty("frontendUser").indexed();
	public static final Property<Image>             img                       = new StartNode<>("img", UserImage.class);
	public static final ImageDataProperty           imageData                 = new ImageDataProperty("imageData", new KeyAndClass(img, Image.class));
	public static final Property<Folder>            homeDirectory             = new EndNode<>("homeDirectory", UserHomeDir.class);
	public static final Property<Folder>            workingDirectory          = new EndNode<>("workingDirectory", UserWorkDir.class);
	public static final Property<List<Group>>       groups                    = new StartNodes<>("groups", Groups.class, new UiNotion());
	public static final Property<Boolean>           isUser                    = new ConstantBooleanProperty("isUser", true);
	public static final Property<String>            twitterName               = new StringProperty("twitterName").cmis().indexed();
	public static final Property<String>            localStorage              = new StringProperty("localStorage");
	public static final Property<List<Favoritable>> favorites                 = new EndNodes<>("favorites", UserFavoriteFavoritable.class);
	public static final Property<Boolean>           skipSecurityRelationships = new BooleanProperty("skipSecurityRelationships").defaultValue(Boolean.FALSE).indexed().readOnly();

	public static final org.structr.common.View uiView = new org.structr.common.View(User.class, PropertyView.Ui,
		type, name, eMail, isAdmin, password, publicKey, blocked, sessionIds, confirmationKey, backendUser, frontendUser,
			groups, img, homeDirectory, workingDirectory, isUser, locale, favorites,
			proxyUrl, proxyUsername, proxyPassword, skipSecurityRelationships
	);

	public static final org.structr.common.View publicView = new org.structr.common.View(User.class, PropertyView.Public,
		type, name, isUser
	);

	default void checkAndCreateHomeDirectory(final SecurityContext securityContext) throws FrameworkException {

		if (Settings.FilesystemEnabled.getValue()) {

			// use superuser context here
			final SecurityContext storedContext = SecurityContext.getSuperUserInstance();

			try {

				setSecurityContext(SecurityContext.getSuperUserInstance());

				Folder homeDir = getProperty(User.homeDirectory);
				if (homeDir == null) {

					// create home directory
					final App app     = StructrApp.getInstance();
					Folder homeFolder = app.nodeQuery(Folder.class).and(Folder.name, "home").and(Folder.parent, null).getFirst();

					if (homeFolder == null) {

						homeFolder = app.create(Folder.class,
							new NodeAttribute(Folder.name, "home"),
							new NodeAttribute(Folder.owner, null),
							new NodeAttribute(Folder.visibleToAuthenticatedUsers, true)
						);
					}

					app.create(Folder.class,
						new NodeAttribute(Folder.name, getUuid()),
						new NodeAttribute(Folder.owner, this),
						new NodeAttribute(AbstractFile.parent, homeFolder),
						new NodeAttribute(Folder.visibleToAuthenticatedUsers, true),
						new NodeAttribute(Folder.homeFolderOfUser, this)
					);
				}

			} catch (Throwable t) {


			} finally {

				// restore previous context
				setSecurityContext(storedContext);
			}
		}
	}

	default void checkAndRemoveHomeDirectory(final SecurityContext securityContext) throws FrameworkException {

		if (Settings.FilesystemEnabled.getValue()) {

			// use superuser context here
			final SecurityContext storedContext = getSecurityContext();

			try {

				setSecurityContext(SecurityContext.getSuperUserInstance());

				final Folder homeDir = getProperty(User.homeDirectory);
				if (homeDir != null) {

					StructrApp.getInstance().delete(homeDir);
				}

			} catch (Throwable t) {


			} finally {

				// restore previous context
				setSecurityContext(storedContext);
			}

		}
	}

	@Override
	default boolean shouldSkipSecurityRelationships() {
		return getProperty(User.skipSecurityRelationships);
	}
}
