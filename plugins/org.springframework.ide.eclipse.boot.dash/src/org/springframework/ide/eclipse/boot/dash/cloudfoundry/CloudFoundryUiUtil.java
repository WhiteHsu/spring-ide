/*******************************************************************************
 * Copyright (c) 2015 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.boot.dash.cloudfoundry;

import java.net.URL;
import java.util.List;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.springframework.ide.eclipse.boot.dash.BootDashActivator;
import org.springframework.ide.eclipse.boot.dash.model.Operation;

public class CloudFoundryUiUtil {

	public static OrgsAndSpaces getCloudSpaces(final CloudFoundryTargetProperties targetProperties,
			IRunnableContext context) throws Exception {

		OrgsAndSpaces spaces = null;

		Operation<List<CloudSpace>> op = new Operation<List<CloudSpace>>(
				"Connecting to the Cloud Foundry target. Please wait while the list of spaces is resolved...") {
			protected List<CloudSpace> runOp(IProgressMonitor monitor) throws Exception {
				return getClient(targetProperties).getSpaces();
			}
		};

		List<CloudSpace> actualSpaces = op.run(context, true);
		if (actualSpaces != null && !actualSpaces.isEmpty()) {
			spaces = new OrgsAndSpaces(actualSpaces);
		}

		return spaces;
	}

	/**
	 * Get the client for an existing {@link CloudFoundryRunTarget}. Note that
	 * this may require the password to be set for that runtarget.
	 *
	 * @param runTarget
	 * @return client if connection was successful.
	 * @throws Exception
	 *             if there was an error connecting, including if password is
	 *             not set or invalid.
	 */
	public static CloudFoundryOperations getClient(CloudFoundryRunTarget runTarget) throws Exception {

		CloudFoundryTargetProperties targetProperties = (CloudFoundryTargetProperties) runTarget.getTargetProperties();

		return getClient(targetProperties);
	}

	public static CloudFoundryOperations getClient(CloudFoundryTargetProperties targetProperties) throws Exception {
		checkPassword(targetProperties.getPassword(), targetProperties.getUserName());
		return targetProperties.getSpaceName() != null
				? new CloudFoundryClient(
						new CloudCredentials(targetProperties.getUserName(), targetProperties.getPassword()),
						new URL(targetProperties.getUrl()), targetProperties.getOrganizationName(),
						targetProperties.getSpaceName(), targetProperties.isSelfsigned())
				: new CloudFoundryClient(
						new CloudCredentials(targetProperties.getUserName(), targetProperties.getPassword()),
						new URL(targetProperties.getUrl()), targetProperties.isSelfsigned());

	}

	public static void checkPassword(String password, String id) throws Exception {
		if (password == null) {
			throw BootDashActivator.asCoreException("No password stored or set for: " + id
					+ ". Please ensure that the password is set in the run target and it is up-to-date.");
		}
	}

	public static Shell getShell() {
		return PlatformUI.getWorkbench().getModalDialogShellProvider().getShell();
	}
}
