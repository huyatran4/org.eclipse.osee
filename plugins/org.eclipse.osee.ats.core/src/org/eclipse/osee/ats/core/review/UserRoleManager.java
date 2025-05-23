/*********************************************************************
 * Copyright (c) 2004, 2007 Boeing
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

package org.eclipse.osee.ats.core.review;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.osee.ats.api.AtsApi;
import org.eclipse.osee.ats.api.IAtsWorkItem;
import org.eclipse.osee.ats.api.data.AtsAttributeTypes;
import org.eclipse.osee.ats.api.notify.AtsNotificationEventFactory;
import org.eclipse.osee.ats.api.notify.AtsNotifyType;
import org.eclipse.osee.ats.api.review.IAtsPeerReviewRoleManager;
import org.eclipse.osee.ats.api.review.IAtsPeerToPeerReview;
import org.eclipse.osee.ats.api.review.PeerToPeerReviewState;
import org.eclipse.osee.ats.api.review.ReviewRequiredMinimum;
import org.eclipse.osee.ats.api.review.ReviewRole;
import org.eclipse.osee.ats.api.review.ReviewRoleType;
import org.eclipse.osee.ats.api.review.UserRole;
import org.eclipse.osee.ats.api.review.UserRoleError;
import org.eclipse.osee.ats.api.user.AtsUser;
import org.eclipse.osee.ats.api.user.IAtsUserService;
import org.eclipse.osee.ats.api.util.IAtsChangeSet;
import org.eclipse.osee.ats.api.workdef.IAttributeResolver;
import org.eclipse.osee.ats.api.workdef.WidgetStatus;
import org.eclipse.osee.ats.api.workdef.model.StateDefinition;
import org.eclipse.osee.ats.api.workdef.model.WorkDefinition;
import org.eclipse.osee.framework.core.data.IAttribute;
import org.eclipse.osee.framework.jdk.core.type.CountingMap;
import org.eclipse.osee.framework.jdk.core.type.OseeCoreException;
import org.eclipse.osee.framework.jdk.core.util.AXml;

/**
 * @author Donald G. Dunne
 */
public class UserRoleManager implements IAtsPeerReviewRoleManager {

   protected final static String ROLE_ITEM_TAG = "Role";

   private final Matcher roleMatcher =
      java.util.regex.Pattern.compile("<" + ROLE_ITEM_TAG + ">(.*?)</" + ROLE_ITEM_TAG + ">",
         Pattern.DOTALL | Pattern.MULTILINE).matcher("");
   protected final IAtsUserService userService;
   protected final IAttributeResolver attrResolver;
   protected final IAtsPeerToPeerReview peerRev;
   protected final AtsApi atsApi;
   private final CountingMap<ReviewRole> actualCountMap;
   private final CountingMap<ReviewRoleType> actualTypeCountMap;
   private final Map<ReviewRoleType, Integer> expectedRoleTypeMap;
   private final Map<ReviewRole, Integer> expectedRoleMap;
   private final List<ReviewRequiredMinimum> reviewRequiredMinimums;
   private List<UserRole> roles;

   public UserRoleManager(IAtsPeerToPeerReview peerRev, AtsApi atsApi) {
      this.atsApi = atsApi;
      this.attrResolver = atsApi.getAttributeResolver();
      this.userService = atsApi.getUserService();
      this.peerRev = peerRev;
      expectedRoleTypeMap = peerRev.getWorkDefinition().getReviewRoleTypeMap();
      expectedRoleMap = peerRev.getWorkDefinition().getReviewRoleMap();
      reviewRequiredMinimums = peerRev.getWorkDefinition().getReviewRequiredMinimums();
      actualTypeCountMap = new CountingMap<>();
      for (UserRole userRole : getUserRoles()) {
         actualTypeCountMap.put(userRole.getRole().getReviewRoleType());
      }
      actualCountMap = new CountingMap<>();
      for (UserRole userRole : getUserRoles()) {
         actualCountMap.put((userRole.getRole()));
      }
   }

   public void ensureLoaded() {
      if (roles == null) {
         roles = new ArrayList<>();
         if (attrResolver != null) {
            for (String xml : attrResolver.getAttributesToStringList(peerRev, AtsAttributeTypes.Role)) {
               roleMatcher.reset(xml);
               while (roleMatcher.find()) {
                  UserRole item = new UserRole(roleMatcher.group(), peerRev.getWorkDefinition());
                  roles.add(item);
               }
            }
         }
      }
   }

   @Override
   public List<UserRole> getUserRoles() {
      ensureLoaded();
      return roles;
   }

   @Override
   public List<UserRole> getUserRoles(ReviewRole role) {
      List<UserRole> roles = new ArrayList<>();
      for (UserRole uRole : getUserRoles()) {
         if (uRole.getRole().equals(role)) {
            roles.add(uRole);
         }
      }
      return roles;
   }

   @Override
   public List<AtsUser> getRoleUsers(ReviewRole role) {
      List<AtsUser> users = new ArrayList<>();
      for (UserRole uRole : getUserRoles()) {
         if ((uRole.getRole().equals(role)) && !users.contains(userService.getUserByUserId(uRole.getUserId()))) {
            users.add(userService.getUserByUserId(uRole.getUserId()));
         }
      }
      return users;
   }

   @Override
   public List<AtsUser> getRoleUsers(Collection<UserRole> roles) {
      List<AtsUser> users = new ArrayList<>();
      for (UserRole uRole : roles) {
         if (!users.contains(userService.getUserByUserId(uRole.getUserId()))) {
            users.add(userService.getUserByUserId(uRole.getUserId()));
         }
      }
      return users;
   }

   @Override
   public UserRoleError validateRoleTypeMinimums(StateDefinition fromStateDef, IAtsPeerReviewRoleManager roleMgr) {

      UserRoleError rError = validateRoleMinimums();
      UserRoleError reqError = validateReviewRequiredMinimums();
      if (!rError.getName().equals(UserRoleError.None.name())) {
         return rError;
      }
      if (!reqError.getName().equals(UserRoleError.None.name())) {
         return reqError;
      }

      // If in review state, all roles must have hours spent entered
      if (fromStateDef.getName().equals(PeerToPeerReviewState.Review.getName()) || fromStateDef.getName().equals(
         PeerToPeerReviewState.Meeting.getName())) {
         for (UserRole uRole : roleMgr.getUserRoles()) {
            if (uRole.getHoursSpent() == null) {
               return UserRoleError.HoursSpentMustBeEnteredForEachRole;
            }
         }
      }
      return UserRoleError.None;
   }

   public UserRoleError validateReviewRequiredMinimums() {
      return validateReviewRequiredMinimum(peerRev, reviewRequiredMinimums);
   }

   public UserRoleError validateReviewRequiredMinimum(IAtsPeerToPeerReview peerRev,
      List<ReviewRequiredMinimum> reviewRequiredMinimums) {
      StringBuilder userRoleError = new StringBuilder();
      for (ReviewRequiredMinimum reviewRequiredMinimum : reviewRequiredMinimums) {
         if (peerRev.getParentTeamWorkflow() != null && reviewRequiredMinimum.getParentTeamDef() != null && peerRev.getParentTeamWorkflow().getTeamDefinition().getArtifactToken().equals(
            reviewRequiredMinimum.getParentTeamDef())) {
            int actualCount = actualCountMap.get(reviewRequiredMinimum.getReviewRole());
            if (actualCount < reviewRequiredMinimum.getMin()) {
               userRoleError.append(
                  String.format("Must have minimum of %s [%s], not %s. \n", reviewRequiredMinimum.getMin(),
                     reviewRequiredMinimum.getReviewRole().getName(), actualCount).toString());
            }
         }
      }
      if (userRoleError.toString().isEmpty()) {
         return UserRoleError.None;
      } else {
         return new UserRoleError("MustMeetMinimumRole", userRoleError.toString(), WidgetStatus.Invalid_Incompleted);
      }
   }

   private UserRoleError validateRoleMinimums() {
      for (Entry<ReviewRole, Integer> expectedEntry : expectedRoleMap.entrySet()) {
         ReviewRole role = expectedEntry.getKey();
         int actualCount = actualCountMap.get(role);
         if (role.equals(ReviewRole.Reviewer)) {
            actualCount += actualCountMap.get(ReviewRole.ModeratorReviewer);
         }
         if (role.equals(ReviewRole.Moderator)) {
            actualCount += actualCountMap.get(ReviewRole.ModeratorReviewer);
         }
         int expectedCount = expectedEntry.getValue();
         if (actualCount < expectedCount) {
            return new UserRoleError("MustMeetMinimumRoleType", String.format("Must have minimum of %s [%s], not %s",
               expectedCount, expectedEntry.getKey(), actualCount), WidgetStatus.Invalid_Incompleted);
         }
      }
      return UserRoleError.None;
   }

   @Override
   public boolean isMinimumForRoleCountValid(ReviewRoleType reviewType) {
      int expectedRoleTypeMapCount =
         expectedRoleTypeMap.get(reviewType) == null ? 0 : expectedRoleTypeMap.get(reviewType);
      if (actualTypeCountMap.get(reviewType) >= expectedRoleTypeMapCount) {
         return true;
      }
      return false;
   }

   @Override
   public void addOrUpdateUserRole(UserRole userRole, IAtsChangeSet changes) {
      List<UserRole> roleItems = getUserRoles();
      boolean found = false;
      for (UserRole uRole : roleItems) {
         if (userRole.equals(uRole)) {
            uRole.update(userRole, peerRev.getWorkDefinition());
            found = true;
         }
      }
      if (!found) {
         roleItems.add(userRole);
         if (!peerRev.getAssignees().contains(getUser(userRole, atsApi))) {
            changes.addAssignee(peerRev, getUser(userRole, atsApi));
         }
         for (UserRole uRole : roleItems) {
            AtsUser user = atsApi.getUserService().getUserByUserId(uRole.getUserId());
            changes.addAssignee(peerRev, user);
         }
      }
   }

   @Override
   public void removeUserRole(UserRole userRole) {
      List<UserRole> roleItems = getUserRoles();
      roleItems.remove(userRole);
   }

   private List<UserRole> getStoredUserRoles() {
      // Add new ones: items in userRoles that are not in dbuserRoles
      List<UserRole> storedUserRoles = new ArrayList<>();
      for (IAttribute<Object> attr : atsApi.getAttributeResolver().getAttributes(peerRev, AtsAttributeTypes.Role)) {
         UserRole storedRole = new UserRole((String) attr.getValue(), peerRev.getWorkDefinition());
         storedUserRoles.add(storedRole);
      }
      return storedUserRoles;
   }

   @Override
   public void saveToArtifact(IAtsChangeSet changes) {
      try {
         List<UserRole> storedUserRoles = getStoredUserRoles();

         // Change existing ones
         for (IAttribute<Object> attr : atsApi.getAttributeResolver().getAttributes(peerRev, AtsAttributeTypes.Role)) {
            UserRole storedRole = new UserRole((String) attr.getValue(), peerRev.getWorkDefinition());
            for (UserRole pItem : getUserRoles()) {
               if (pItem.equals(storedRole)) {
                  changes.setAttribute(peerRev, attr, AXml.addTagData(ROLE_ITEM_TAG, pItem.toXml()));
               }
            }
         }
         List<UserRole> userRoles = getUserRoles();
         List<UserRole> updatedStoredUserRoles = getStoredUserRoles();
         // Remove deleted ones; items in dbuserRoles that are not in userRoles
         for (UserRole delUserRole : org.eclipse.osee.framework.jdk.core.util.Collections.setComplement(
            updatedStoredUserRoles, userRoles)) {
            for (IAttribute<Object> attr : atsApi.getAttributeResolver().getAttributes(peerRev,
               AtsAttributeTypes.Role)) {
               UserRole storedRole = new UserRole((String) attr.getValue(), peerRev.getWorkDefinition());
               if (storedRole.equals(delUserRole)) {
                  changes.deleteAttribute(peerRev, attr);
               }
            }
         }
         for (UserRole newRole : org.eclipse.osee.framework.jdk.core.util.Collections.setComplement(userRoles,
            updatedStoredUserRoles)) {
            changes.addAttribute(peerRev, AtsAttributeTypes.Role, AXml.addTagData(ROLE_ITEM_TAG, newRole.toXml()));
         }
         atsApi.getReviewService().updateHoursSpentRoles(peerRev, changes);
         validateUserRolesCompleted(storedUserRoles, userRoles, changes);
      } catch (OseeCoreException ex) {
         atsApi.getLogger().error(ex, "Can't create ats review role document");
      }
   }

   private void validateUserRolesCompleted(List<UserRole> currentUserRoles, List<UserRole> newUserRoles,
      IAtsChangeSet changes) {
      //all reviewers are complete; send notification to author/moderator
      int numCurrentCompleted = 0, numNewCompleted = 0;
      for (UserRole role : newUserRoles) {
         if (role.getRole().getReviewRoleType().equals(ReviewRoleType.Reviewer)) {
            if (!role.isCompleted()) {
               return;
            } else {
               numNewCompleted++;
            }
         }
      }
      for (UserRole role : currentUserRoles) {
         if (role.getRole().getReviewRoleType().equals(ReviewRoleType.Reviewer)) {
            if (role.isCompleted()) {
               numCurrentCompleted++;
            }
         }
      }
      if (numNewCompleted != numCurrentCompleted) {
         try {
            changes.addWorkItemNotificationEvent(
               AtsNotificationEventFactory.getWorkItemNotificationEvent(atsApi.getUserService().getCurrentUser(),
                  (IAtsWorkItem) peerRev, AtsNotifyType.Peer_Reviewers_Completed));
         } catch (OseeCoreException ex) {
            atsApi.getLogger().error(ex, "Error adding ATS Notification Event");
         }
      }
   }

   public static AtsUser getUser(UserRole item, AtsApi atsApi) {
      return atsApi.getUserService().getUserByUserId(item.getUserId());
   }

   @Override
   public WorkDefinition getWorkDefinition() {
      return peerRev.getWorkDefinition();
   }
}
