/*********************************************************************
 * Copyright (c) 2010 Boeing
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

package org.eclipse.osee.ats.ide.agile;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.jface.window.Window;
import org.eclipse.nebula.widgets.xviewer.XViewer;
import org.eclipse.nebula.widgets.xviewer.core.model.XViewerColumn;
import org.eclipse.osee.ats.api.IAtsWorkItem;
import org.eclipse.osee.ats.api.agile.IAgileSprint;
import org.eclipse.osee.ats.api.column.AtsColumnTokensDefault;
import org.eclipse.osee.ats.api.data.AtsArtifactImages;
import org.eclipse.osee.ats.api.data.AtsArtifactTypes;
import org.eclipse.osee.ats.api.data.AtsAttributeTypes;
import org.eclipse.osee.ats.api.data.AtsRelationTypes;
import org.eclipse.osee.ats.api.util.IAtsChangeSet;
import org.eclipse.osee.ats.core.column.SprintColumn;
import org.eclipse.osee.ats.ide.AtsArtifactImageProvider;
import org.eclipse.osee.ats.ide.column.BackgroundLoadingPreComputedColumnUI;
import org.eclipse.osee.ats.ide.internal.Activator;
import org.eclipse.osee.ats.ide.internal.AtsApiService;
import org.eclipse.osee.ats.ide.workflow.AbstractWorkflowArtifact;
import org.eclipse.osee.ats.ide.world.IAtsWorldArtifactEventColumn;
import org.eclipse.osee.ats.ide.world.WorldXViewer;
import org.eclipse.osee.framework.core.data.ArtifactToken;
import org.eclipse.osee.framework.core.exception.ArtifactDoesNotExist;
import org.eclipse.osee.framework.jdk.core.type.OseeCoreException;
import org.eclipse.osee.framework.logging.OseeLevel;
import org.eclipse.osee.framework.logging.OseeLog;
import org.eclipse.osee.framework.skynet.core.artifact.Artifact;
import org.eclipse.osee.framework.skynet.core.artifact.ArtifactCache;
import org.eclipse.osee.framework.skynet.core.event.model.ArtifactEvent;
import org.eclipse.osee.framework.skynet.core.event.model.ArtifactTopicEvent;
import org.eclipse.osee.framework.skynet.core.event.model.EventBasicGuidRelation;
import org.eclipse.osee.framework.skynet.core.event.model.EventTopicRelationTransfer;
import org.eclipse.osee.framework.skynet.core.transaction.TransactionManager;
import org.eclipse.osee.framework.ui.plugin.util.AWorkbench;
import org.eclipse.osee.framework.ui.swt.Displays;
import org.eclipse.osee.framework.ui.swt.ImageManager;
import org.eclipse.osee.framework.ui.swt.Widgets;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

/**
 * @author Donald G. Dunne
 */
public class SprintColumnUI extends BackgroundLoadingPreComputedColumnUI implements IAtsWorldArtifactEventColumn {

   public static SprintColumnUI instance = new SprintColumnUI();

   public static SprintColumnUI getInstance() {
      return instance;
   }

   private SprintColumnUI() {
      super(AtsColumnTokensDefault.SprintColumn);
   }

   /**
    * XViewer uses copies of column definitions so originals that are registered are not corrupted. Classes extending
    * XViewerValueColumn MUST extend this constructor so the correct sub-class is created
    */
   @Override
   public SprintColumnUI copy() {
      SprintColumnUI newXCol = new SprintColumnUI();
      super.copy(this, newXCol);
      return newXCol;
   }

   @Override
   public String getValue(IAtsWorkItem workItem, Map<Long, String> idToValueMap) {
      return SprintColumn.getTextValue(workItem, AtsApiService.get());
   }

   @Override
   public boolean handleAltLeftClick(TreeColumn treeColumn, TreeItem treeItem) {
      try {
         if (treeItem.getData() instanceof Artifact) {
            Artifact useArt = AtsApiService.get().getQueryServiceIde().getArtifact(treeItem);
            if (!useArt.isOfType(AtsArtifactTypes.AbstractWorkflowArtifact)) {
               return false;
            }

            boolean modified = promptChangeSprint(useArt);

            XViewer xViewer = (XViewer) ((XViewerColumn) treeColumn.getData()).getXViewer();
            if (modified) {
               useArt.persist("persist sprints via alt-left-click");
               xViewer.update(useArt, null);
               return true;
            }
         }
      } catch (OseeCoreException ex) {
         OseeLog.log(Activator.class, OseeLevel.SEVERE_POPUP, ex);
      }

      return false;
   }

   public static boolean promptChangeSprint(Artifact awa) {
      return promptChangeSprint(Arrays.asList(awa));
   }

   public static boolean promptChangeSprint(final Collection<? extends Artifact> awas) {
      // verify that all awas belong to the same backlog
      SprintItems items = new SprintItems(awas);

      if (items.isNoBacklogDetected()) {
         AWorkbench.popup("Workflow(s) must belong to a Backlog to set their Sprint.");
         return false;
      }
      if (items.isMultipleBacklogsDetected()) {
         AWorkbench.popup("All workflows must belong to same Backlog.");
         return false;
      }

      Artifact backlogArt = AtsApiService.get().getQueryServiceIde().getArtifact(items.getCommonBacklog());
      Artifact agileTeamArt = null;
      try {
         agileTeamArt = backlogArt.getRelatedArtifact(AtsRelationTypes.AgileTeamToBacklog_AgileTeam);
      } catch (ArtifactDoesNotExist ex) {
         // do nothing
      }
      if (agileTeamArt == null) {
         AWorkbench.popup("No Agile Team for Agile Backlog [%s]", backlogArt.toStringWithId());
         return false;
      }

      Set<IAgileSprint> activeSprints = getActiveSprints(agileTeamArt);

      if (activeSprints.isEmpty()) {
         AWorkbench.popup("No Active Sprints available for the Agile Team [%s]", agileTeamArt.toStringWithId());
         return false;
      }

      SprintFilteredListDialog dialog = createDialog(items, activeSprints);

      if (dialog.open() == Window.OK) {
         if (dialog.isRemoveFromSprint()) {
            IAtsChangeSet changes = AtsApiService.get().createChangeSet("Remove Sprint");
            for (Artifact awa : awas) {
               Collection<ArtifactToken> relatedSprintArts =
                  AtsApiService.get().getAgileService().getRelatedSprints(awa);
               for (ArtifactToken relatedSprint : relatedSprintArts) {
                  changes.unrelate(awa, AtsRelationTypes.AgileSprintToItem_AgileSprint, relatedSprint);
               }
            }
            changes.executeIfNeeded();

         } else {
            IAgileSprint selectedSprint = dialog.getSelectedFirst();
            Artifact newSprintArt = null;
            if (selectedSprint != null) {
               newSprintArt = AtsApiService.get().getQueryServiceIde().getArtifact(selectedSprint);
            }

            IAtsChangeSet changes = AtsApiService.get().createChangeSet("Set Sprint");
            for (Artifact awa : awas) {
               Collection<ArtifactToken> relatedSprintArts =
                  AtsApiService.get().getAgileService().getRelatedSprints(awa);
               for (ArtifactToken relatedSprint : relatedSprintArts) {
                  changes.unrelate(awa, AtsRelationTypes.AgileSprintToItem_AgileSprint, relatedSprint);
               }
               changes.relate(awa, AtsRelationTypes.AgileSprintToItem_AgileSprint, newSprintArt);
            }
            TransactionManager.persistInTransaction("Set Sprint", awas);
         }
         return true;
      }
      return false;
   }

   private static SprintFilteredListDialog createDialog(SprintItems items, Set<IAgileSprint> activeSprints) {
      SprintFilteredListDialog dialog = new SprintFilteredListDialog("Select Sprint", "Select Sprint", activeSprints);
      Window.setDefaultImage(
         ImageManager.getImage(AtsArtifactImageProvider.getKeyedImage(AtsArtifactImages.AGILE_SPRINT)));
      dialog.setInput(activeSprints);
      if (items.isCommonSelectedSprint() && items.getMultipleSprints().size() == 1) {
         dialog.setInitialSelections(Arrays.asList(items.getMultipleSprints().iterator().next()));
      }
      return dialog;
   }

   private static Set<IAgileSprint> getActiveSprints(Artifact agileTeamArt) {
      Set<IAgileSprint> activeSprints = new HashSet<>();
      for (Artifact sprintArt : agileTeamArt.getRelatedArtifacts(AtsRelationTypes.AgileTeamToSprint_Sprint)) {
         IAgileSprint agileSprint = AtsApiService.get().getWorkItemService().getAgileSprint(sprintArt);
         if (agileSprint.isActive()) {
            activeSprints.add(agileSprint);
         }
      }
      return activeSprints;
   }

   @Override
   public void handleColumnMultiEdit(TreeColumn treeColumn, Collection<TreeItem> treeItems) {
      try {
         Set<AbstractWorkflowArtifact> awas = new HashSet<>();
         for (TreeItem item : treeItems) {
            if (item.getData() instanceof Artifact) {
               Artifact art = AtsApiService.get().getQueryServiceIde().getArtifact(item);
               if (art instanceof AbstractWorkflowArtifact) {
                  awas.add((AbstractWorkflowArtifact) art);
               }
            }
         }
         promptChangeSprint(awas);
         return;
      } catch (OseeCoreException ex) {
         OseeLog.log(Activator.class, OseeLevel.SEVERE_POPUP, ex);
      }
   }

   /**
    * Don't want columns to listen to their own events, so have WorldXViewerEventManager call here to tell columns to
    * handle
    */
   @Override
   public void handleArtifactEvent(ArtifactEvent artifactEvent, WorldXViewer xViewer) {
      if (!Widgets.isAccessible(xViewer.getTree())) {
         return;
      }
      for (EventBasicGuidRelation rel : artifactEvent.getRelations()) {
         Long relTypeId = rel.getRelTypeGuid();
         if (AtsRelationTypes.AgileSprintToItem.getId().equals(relTypeId)) {
            Artifact workflow = ArtifactCache.getActive(rel.getArtB());
            if (workflow != null) {
               IAtsWorkItem workItem = AtsApiService.get().getWorkItemService().getWorkItem(workflow);
               String newValue = getValue(workItem, preComputedValueMap);
               preComputedValueMap.put(workflow.getId(), newValue);
               xViewer.update(workflow, null);
            }
         }
      }
   }

   /**
    * Don't want columns to listen to their own events, so have WorldXViewerEventManager call here to tell columns to
    * handle
    */
   @Override
   public void handleArtifactTopicEvent(ArtifactTopicEvent artifactTopicEvent, WorldXViewer xViewer) {
      if (!Widgets.isAccessible(xViewer.getTree())) {
         return;
      }
      for (EventTopicRelationTransfer rel : artifactTopicEvent.getRelations()) {
         Long relTypeId = rel.getRelTypeId();
         if (AtsRelationTypes.AgileSprintToItem.getId().equals(relTypeId)) {
            Artifact workflow = ArtifactCache.getActive(rel.getArtBToken());
            if (workflow != null) {
               IAtsWorkItem workItem = AtsApiService.get().getWorkItemService().getWorkItem(workflow);
               String newValue = getValue(workItem, preComputedValueMap);
               preComputedValueMap.put(workflow.getId(), newValue);
               xViewer.update(workflow, null);
            }
         }
      }
   }

   @Override
   public Color getForeground(Object element, XViewerColumn xCol, int columnIndex) {
      if (xCol.getId().equals(instance.getId()) && (element instanceof IAtsWorkItem)) {
         IAtsWorkItem workItem = (IAtsWorkItem) element;
         IAgileSprint sprint = AtsApiService.get().getAgileService().getSprint(workItem);
         if (sprint.isInWork() && AtsApiService.get().getAttributeResolver().getSoleAttributeValue(sprint,
            AtsAttributeTypes.CurrentSprint, false)) {
            return Displays.getSystemColor(SWT.COLOR_DARK_GREEN);
         }
      }
      return super.getBackground(element, xCol, columnIndex);
   }

}
