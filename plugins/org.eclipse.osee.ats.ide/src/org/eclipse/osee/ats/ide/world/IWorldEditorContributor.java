/*********************************************************************
 * Copyright (c) 2025 Boeing
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Boeing - initial API and implementation
 **********************************************************************/

package org.eclipse.osee.ats.ide.world;

import org.eclipse.jface.action.IToolBarManager;

public interface IWorldEditorContributor {

   default public void createToolbar(IToolBarManager toolBarManager, WorldEditor worldEditor) {
      // do nothing
   }

}
