/*********************************************************************
 * Copyright (c) 2013 Boeing
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

package org.eclipse.osee.ats.ide.util.internal;

import java.util.Collection;
import org.eclipse.osee.ats.api.data.AtsUserGroups;
import org.eclipse.osee.ats.api.user.AtsUser;
import org.eclipse.osee.ats.core.users.AbstractAtsUserService;
import org.eclipse.osee.ats.ide.internal.AtsApiService;
import org.eclipse.osee.framework.core.data.ArtifactId;
import org.eclipse.osee.framework.core.data.UserToken;
import org.eclipse.osee.framework.core.enums.CoreAttributeTypes;
import org.eclipse.osee.framework.skynet.core.User;
import org.eclipse.osee.framework.skynet.core.UserManager;

/**
 * @author Donald G Dunne
 */
public class AtsUserServiceClientImpl extends AbstractAtsUserService {

   Boolean atsAdmin = null;
   Boolean atsDeleteWorkflowAdmin = null;
   private AtsUser currentUser;

   public AtsUserServiceClientImpl() {
      // For OSGI Instantiation
   }

   // for ReviewOsgiXml public void setConfigurationService(IAtsConfigurationsService configurationService)

   @Override
   public void setCurrentUser(AtsUser currentUser) {
      this.currentUser = currentUser;
   }

   @Override
   public void clearCaches() {
      currentUser = null;
      atsAdmin = null;
   }

   @Override
   public AtsUser getCurrentUser() {
      if (currentUser == null) {
         User user = UserManager.getUser();
         currentUser = new AtsUser();
         currentUser.setName(user.getName());
         currentUser.setUserId(user.getUserId());
         currentUser.setEmail(user.getEmail());
         currentUser.setAbridgedEmail(user.getSoleAttributeValue(CoreAttributeTypes.AbridgedEmail, ""));
         currentUser.setActive(user.isActive());
         currentUser.setId(user.getId());
         currentUser.getLoginIds().addAll(user.getLoginIds());
         currentUser.setPhone(user.getPhone());
         currentUser.setStoreObject(user);
      }
      return currentUser;
   }

   @Override
   public AtsUser getCurrentUserNoCache() {
      clearCaches();
      return getCurrentUser();
   }

   @Override
   public AtsUser getUserById(ArtifactId userId) {
      return configurationService.getConfigurations().getIdToUser().get(userId.getId());
   }

   @Override
   public String getCurrentUserId() {
      return UserManager.getUser().getUserId();
   }

   @Override
   public AtsUser getCurrentUserOrNull() {
      UserToken user = UserManager.getUser();
      if (user.isValid()) {
         return getUserById(user);
      }
      return null;
   }

   @Override
   public boolean isAtsAdmin() {
      if (atsAdmin == null) {
         atsAdmin = AtsApiService.get().userService().isInUserGroup(AtsUserGroups.AtsAdmin);
      }
      return atsAdmin;
   }

   @Override
   public boolean isAtsDeleteWorkflowAdmin() {
      if (atsDeleteWorkflowAdmin == null) {
         atsDeleteWorkflowAdmin = AtsApiService.get().userService().isUserMember(AtsUserGroups.AtsDeleteWorkflowAdmin,
            AtsApiService.get().getUserService().getCurrentUser());
      }

      return atsDeleteWorkflowAdmin;
   }

   @Override
   public Collection<AtsUser> getUsers() {
      return configurationService.getConfigurations().getUsers();
   }

}