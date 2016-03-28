/*******************************************************************************
 * Copyright (c) 2016 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.boot.dash.cloudfoundry.client;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CFApplication extends CFEntity {
	//TODO: lots of this infos should be moved to application details

	int getInstances();
	int getRunningInstances();
	Map<String, String> getEnvAsMap();
	int getMemory();
	UUID getGuid();
	List<String> getServices();
	String getDetectedBuildpack();
	String getBuildpackUrl();
	List<String> getUris();
	CFAppState getState();
	int getDiskQuota();
	Integer getTimeout();
	String getCommand();
	String getStack();
}
