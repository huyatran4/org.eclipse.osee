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

package org.eclipse.osee.orcs.db.internal.search.engines;

import static org.eclipse.osee.framework.core.enums.CoreAttributeTypes.Name;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.eclipse.osee.framework.core.OrcsTokenService;
import org.eclipse.osee.framework.core.data.ApplicabilityToken;
import org.eclipse.osee.framework.core.data.ArtifactId;
import org.eclipse.osee.framework.core.data.ArtifactReadable;
import org.eclipse.osee.framework.core.data.ArtifactToken;
import org.eclipse.osee.framework.core.data.ArtifactTypeToken;
import org.eclipse.osee.framework.core.data.AttributeId;
import org.eclipse.osee.framework.core.data.AttributeTypeGeneric;
import org.eclipse.osee.framework.core.data.AttributeTypeToken;
import org.eclipse.osee.framework.core.data.Branch;
import org.eclipse.osee.framework.core.data.BranchCategoryToken;
import org.eclipse.osee.framework.core.data.BranchId;
import org.eclipse.osee.framework.core.data.GammaId;
import org.eclipse.osee.framework.core.data.IAttribute;
import org.eclipse.osee.framework.core.data.RelationTypeSide;
import org.eclipse.osee.framework.core.data.TransactionDetails;
import org.eclipse.osee.framework.core.data.TransactionId;
import org.eclipse.osee.framework.core.data.UserService;
import org.eclipse.osee.framework.core.enums.BranchArchivedState;
import org.eclipse.osee.framework.core.enums.LoadLevel;
import org.eclipse.osee.framework.core.enums.ModificationType;
import org.eclipse.osee.framework.core.enums.RelationSide;
import org.eclipse.osee.framework.core.sql.OseeSql;
import org.eclipse.osee.framework.jdk.core.type.HashCollection;
import org.eclipse.osee.framework.jdk.core.type.Id;
import org.eclipse.osee.framework.jdk.core.type.OseeCoreException;
import org.eclipse.osee.framework.jdk.core.util.DateUtil;
import org.eclipse.osee.framework.resource.management.IResourceManager;
import org.eclipse.osee.jdbc.JdbcClient;
import org.eclipse.osee.jdbc.JdbcStatement;
import org.eclipse.osee.orcs.OseeDb;
import org.eclipse.osee.orcs.QueryType;
import org.eclipse.osee.orcs.core.ds.ApplicabilityDsQuery;
import org.eclipse.osee.orcs.core.ds.ArtifactData;
import org.eclipse.osee.orcs.core.ds.KeyValueStore;
import org.eclipse.osee.orcs.core.ds.LoadDataHandler;
import org.eclipse.osee.orcs.core.ds.LoadDataHandlerAdapter;
import org.eclipse.osee.orcs.core.ds.OptionsUtil;
import org.eclipse.osee.orcs.core.ds.QueryData;
import org.eclipse.osee.orcs.core.ds.QueryEngine;
import org.eclipse.osee.orcs.core.ds.criteria.CriteriaAttributeKeywords;
import org.eclipse.osee.orcs.data.ArtifactReadableImpl;
import org.eclipse.osee.orcs.data.TransactionReadable;
import org.eclipse.osee.orcs.db.internal.loader.SqlObjectLoader;
import org.eclipse.osee.orcs.db.internal.search.QuerySqlContext;
import org.eclipse.osee.orcs.db.internal.search.QuerySqlContextFactory;
import org.eclipse.osee.orcs.db.internal.sql.SelectiveArtifactSqlWriter;
import org.eclipse.osee.orcs.db.internal.sql.SqlHandlerFactory;
import org.eclipse.osee.orcs.db.internal.sql.join.SqlJoinFactory;
import org.eclipse.osee.orcs.search.ArtifactTable;
import org.eclipse.osee.orcs.search.ArtifactTableOptions;
import org.eclipse.osee.orcs.search.QueryFactory;
import org.eclipse.osee.orcs.search.TupleQuery;

/**
 * @author Roberto E. Escobar
 */
public class QueryEngineImpl implements QueryEngine {
   private static final int DefaultArtifactNum = 500;
   // set HashMap capacity to accommodate the artifactCount given the default load factor of 75%
   private static final int DefaultMapCapacity = (int) (DefaultArtifactNum / 0.75) + 1;
   private final ObjectQueryCallableFactory artifactQueryEngineFactory;
   private final QuerySqlContextFactory branchSqlContextFactory;
   private final QuerySqlContextFactory txSqlContextFactory;
   private final JdbcClient jdbcClient;
   private final SqlJoinFactory sqlJoinFactory;
   private final SqlObjectLoader sqlObjectLoader;
   private final KeyValueStore keyValue;
   private final SqlHandlerFactory handlerFactory;
   private final OrcsTokenService tokenService;
   private final IResourceManager resourceManager;

   public QueryEngineImpl(ObjectQueryCallableFactory artifactQueryEngineFactory, QuerySqlContextFactory branchSqlContextFactory, QuerySqlContextFactory txSqlContextFactory, JdbcClient jdbcClient, SqlJoinFactory sqlJoinFactory, SqlHandlerFactory handlerFactory, SqlObjectLoader sqlObjectLoader, OrcsTokenService tokenService, KeyValueStore keyValue, IResourceManager resourceManager) {
      this.artifactQueryEngineFactory = artifactQueryEngineFactory;
      this.branchSqlContextFactory = branchSqlContextFactory;
      this.txSqlContextFactory = txSqlContextFactory;
      this.jdbcClient = jdbcClient;
      this.sqlJoinFactory = sqlJoinFactory;
      this.sqlObjectLoader = sqlObjectLoader;
      this.tokenService = tokenService;
      this.keyValue = keyValue;
      this.handlerFactory = handlerFactory;
      this.resourceManager = resourceManager;
   }

   @Override
   public int getArtifactCount(QueryData queryData) {
      if (isPostProcessRequired(queryData)) {
         return artifactQueryEngineFactory.getArtifactCount(queryData);
      }
      QueryData rootQueryData = queryData.getRootQueryData();
      return new SelectiveArtifactSqlWriter(sqlJoinFactory, jdbcClient, rootQueryData).getCount(handlerFactory);
   }

   private boolean isPostProcessRequired(QueryData queryData) {
      return OptionsUtil.getLegacyPostProcessing(queryData.getOptions()) && queryData.hasCriteriaType(
         CriteriaAttributeKeywords.class);
   }

   @Override
   public void runArtifactQuery(QueryData queryData, LoadDataHandler handler) {
      try {
         artifactQueryEngineFactory.createQuery(null, queryData, handler).call();
      } catch (Exception ex) {
         OseeCoreException.wrapAndThrow(ex);
      } finally {
         queryData.reset();
      }
   }

   @Override
   public int getBranchCount(QueryData queryData) {
      return getCount(branchSqlContextFactory, queryData);
   }

   @Override
   public void runBranchQuery(QueryData queryData, List<? super Branch> branches) {
      QuerySqlContext queryContext = branchSqlContextFactory.createQueryContext(null, queryData, QueryType.SELECT);
      sqlObjectLoader.loadBranches(branches, queryContext, queryData.getAllCriteria());
      queryData.reset();
   }

   @Override
   public int getTxCount(QueryData queryData) {
      return getCount(txSqlContextFactory, queryData);
   }

   private int getCount(QuerySqlContextFactory sqlContextFactory, QueryData queryData) {
      QuerySqlContext queryContext = sqlContextFactory.createQueryContext(null, queryData, QueryType.COUNT);
      int count = sqlObjectLoader.getCount(queryContext);
      queryData.reset();
      return count;
   }

   @Override
   public void runTxQuery(UserService userService, QueryData queryData, List<? super TransactionReadable> txs) {
      QuerySqlContext queryContext = txSqlContextFactory.createQueryContext(null, queryData, QueryType.SELECT);
      sqlObjectLoader.loadTransactions(userService, txs, queryContext);
      queryData.reset();
   }

   @Override
   public TupleQuery createTupleQuery() {
      return new TupleQueryImpl(jdbcClient, sqlJoinFactory, keyValue, tokenService);
   }

   @Override
   public List<ArtifactToken> asArtifactTokens(QueryData queryData) {
      List<ArtifactToken> tokens = new ArrayList<>(100);
      selectiveArtifactLoad(queryData, DefaultArtifactNum,
         stmt -> tokens.add(ArtifactToken.valueOf(stmt.getLong("art_id"), stmt.getString("value"),
            queryData.getBranch(), tokenService.getArtifactTypeOrCreate(stmt.getLong("art_type_id")))));
      return tokens;
   }

   /**
    * {@inheritDoc}
    */

   @Override
   public Map<ArtifactId, ArtifactReadable> asArtifactMap(QueryData queryData, QueryFactory queryFactory) {
      HashMap<ArtifactId, ArtifactReadable> artifacts = new HashMap<>(DefaultMapCapacity);
      loadArtifactsInto(queryData, queryFactory, a -> artifacts.put(a, a), DefaultArtifactNum);
      return artifacts;
   }

   /**
    * {@inheritDoc}
    */

   @Override
   public Map<ArtifactId, ArtifactReadable> asArtifactMap(QueryData queryData, QueryFactory queryFactory,
      Map<ArtifactId, ArtifactReadable> artifacts) {
      var artifactsMap =
         Objects.nonNull(artifacts) ? artifacts : new HashMap<ArtifactId, ArtifactReadable>(DefaultMapCapacity);
      loadArtifactsInto(queryData, queryFactory, a -> artifactsMap.put(a, a), DefaultArtifactNum);
      return artifacts;
   }

   @Override
   public List<ArtifactReadable> asArtifacts(QueryData queryData, QueryFactory queryFactory) {
      List<ArtifactReadable> artifacts = new ArrayList<>(DefaultArtifactNum);
      loadArtifactsInto(queryData, queryFactory, a -> artifacts.add(a), DefaultArtifactNum);
      return artifacts;
   }

   @Override
   public List<ArtifactReadable> asArtifact(QueryData queryData, QueryFactory queryFactory) {
      List<ArtifactReadable> artifacts = new ArrayList<>(1);
      loadArtifactsInto(queryData, queryFactory, a -> artifacts.add(a), 1);
      return artifacts;
   }

   @Override
   public Map<ArtifactId, ArtifactReadable> asViewToArtifactMap(QueryData queryData, QueryFactory queryFactory) {
      var artifactsMap = new HashMap<ArtifactId, ArtifactReadable>(DefaultMapCapacity);
      loadArtifactsInto(queryData, queryFactory, (view, artifact) -> artifactsMap.put(view, artifact), 1);
      return artifactsMap;
   }

   /**
    * Note: this function assumes that all queries that pass through here are grouped by their configuration
    */
   private void loadArtifactsInto(QueryData queryData, QueryFactory queryFactory,
      BiConsumer<ArtifactId, ArtifactReadable> artifactConsumer, int numArtifacts) {
      // make the HashMap capacity large enough to accommodate the artifactCount given the default load factor of 75%
      HashMap<ArtifactId, ArtifactReadable> artifactMap = new HashMap<>((int) (numArtifacts / 0.75) + 1);
      AtomicReference<Long> currentConfiguration = new AtomicReference<>(0L);
      List<ArtifactId> consumedArts = new LinkedList<>();
      Consumer<JdbcStatement> jdbcConsumer = stmt -> {
         Long artId = stmt.getLong("art_id");
         GammaId artGamma = GammaId.valueOf(stmt.getLong("art_gamma_id"));
         Long attr_id = stmt.getLong("attr_id");
         Long otherArtId = stmt.getLong("other_art_id");
         Long otherArtType = stmt.getLong("other_art_type_id");
         Long typeId = stmt.getLong("type_id");
         String value = stmt.getString("value");
         String uri = stmt.getString("uri");
         Long configuration = stmt.getLong("configuration");
         if (!currentConfiguration.get().equals(configuration)) {
            //this resets the maps for the next configuration such that they can be re-populated
            artifactMap.clear();
            consumedArts.clear();
            currentConfiguration.set(configuration);
         }

         ArtifactReadableImpl artifact = (ArtifactReadableImpl) artifactMap.get(ArtifactId.valueOf(artId));
         if (artifact == null && artGamma != null) {
            artifact = createArtifact(stmt, artId, stmt.getLong("art_type_id"), queryData, queryFactory, artGamma);
            artifactMap.put(artifact, artifact);
            if (stmt.getLong("top") == 1) {
               artifactConsumer.accept(ArtifactId.valueOf(configuration), artifact);
               consumedArts.add(ArtifactId.valueOf(artifact.getId()));
            } else {
               //if we have attr_id AND other artid then we are a reference artifact

               if (attr_id > 0 && otherArtId > 0 && otherArtType > 0) {
                  //see if the parent artifact has been created, if so add to the resourceArtifacts hash map
                  //if not, create? like relation logic?
                  Long sourceAttrId = stmt.getLong("rel_type"); //overloading the rel_type column as attr Id for reference artifacts
                  ArtifactReadableImpl sourceArtifact =
                     (ArtifactReadableImpl) artifactMap.get(ArtifactId.valueOf(otherArtId));
                  if (sourceArtifact != null) {
                     List<IAttribute<?>> values = sourceArtifact.getAttributesHashCollection().getValues();
                     Optional<IAttribute<?>> findFirst =
                        values.stream().filter(a -> a.getId().equals(sourceAttrId)).findFirst();
                     if (findFirst.isPresent()) {
                        sourceArtifact.putReferenceArtifact(AttributeId.valueOf(findFirst.get().getId()), artifact);
                     }
                  }

               }
            }
         } else {
            // If an artifact with top = 1 appears as an "otherArtifact" first, because it's related to an artifact with top = 1, it needs
            // to be added to the consumer as well.
            if (!consumedArts.contains(ArtifactId.valueOf(artifact.getId())) && stmt.getLong("top") == 1) {
               artifactConsumer.accept(ArtifactId.valueOf(configuration), artifact);
               consumedArts.add(ArtifactId.valueOf(artifact.getId()));
            }
            /**
             * if artifact is already in the map that means it was first found as a relation and applicability may not
             * have been set
             */
            Long applicId = stmt.getLong("app_id");
            artifactMap.get(ArtifactId.valueOf(artId)).getApplicabilityToken().setId(applicId);
            if (OptionsUtil.getIncludeApplicabilityTokens(queryData.getRootQueryData().getOptions())) {
               String applicValue = stmt.getString("app_value");
               artifactMap.get(ArtifactId.valueOf(artId)).getApplicabilityToken().setName(applicValue);
            }
         }

         if (attr_id != 0) {
            AttributeTypeGeneric<?> attributeType = tokenService.getAttributeTypeOrCreate(typeId);
            GammaId gamma = GammaId.valueOf(stmt.getLong("gamma_id"));
            Attribute<?> attribute =
               new Attribute<>(stmt.getLong("attr_id"), attributeType, value, uri, gamma, resourceManager);
            // Check if the attribute value has been added already to the artifact to prevent duplicate values.
            // This can happen when the same object is returned multiple times when following relations.
            if (!artifact.getAttributeList(attributeType).stream().anyMatch(a -> a.getId().equals(attribute.getId()))) {
               artifact.putAttributeValue(attributeType, attribute);
            }
            if (OptionsUtil.getIncludeLatestTransactionDetails(queryData.getRootQueryData().getOptions())) {

               if (artifact.getTxDetails().getAuthor().isInvalid()) {

                  Date time = null;

                  String maxTime = stmt.getString("max_time").replaceAll("'", "");
                  String timeStr = stmt.getString("timeStr").replaceAll("'", "");
                  if (maxTime.equals(timeStr)) {
                     try {
                        int indexOf = timeStr.indexOf(".");
                        int endIndex = timeStr.length() - 1;
                        if (indexOf > 0) {
                           endIndex = indexOf;
                        }
                        time = DateUtil.getDate("yyyyMMddHHmmss", timeStr);
                     } catch (ParseException ex) {
                        time = DateUtil.getSentinalDate();
                     }
                     String oseeComment = stmt.getString("osee_comment");
                     int txType = stmt.getInt("tx_type");
                     TransactionId suppTxId = TransactionId.valueOf(stmt.getLong("supp_tx_id"));
                     ArtifactId commitArtId = ArtifactId.valueOf(stmt.getLong("commit_art_id"));
                     Long buildId = stmt.getLong("build_id");
                     ArtifactId author = ArtifactId.valueOf(stmt.getLong("author"));
                     artifact.getTxDetails().setTime(time);
                     artifact.getTxDetails().setAuthor(author);
                     artifact.getTxDetails().setOseeComment(oseeComment);
                     artifact.getTxDetails().setTxType(txType);
                     artifact.getTxDetails().setTxId(suppTxId);
                     artifact.getTxDetails().setCommitArtId(commitArtId);
                     artifact.getTxDetails().setBuild_id(buildId);
                  }
               }
            }
         } else if (attr_id == 0) {
            RelationSide side = value.equals("A") ? RelationSide.SIDE_A : RelationSide.SIDE_B;

            ArtifactReadableImpl otherArtifact = (ArtifactReadableImpl) artifactMap.get(ArtifactId.valueOf(otherArtId));
            if (otherArtifact == null) {
               GammaId gamma = GammaId.valueOf(stmt.getLong("other_art_gamma_id"));
               otherArtifact = createArtifact(stmt, otherArtId, otherArtType, queryData, queryFactory, gamma);
               artifactMap.put(otherArtifact, otherArtifact);
            }
            /*
             * If using followSearch, multi rows of a relation may return in some conditions e.g. followSearch Ideally
             * should try and updates handlers so that select * isn't used, instead explicity calling out columns so
             * that some can be excluded in final select
             */

            RelationTypeSide relTypeSide = new RelationTypeSide(tokenService.getRelationTypeOrCreate(typeId), side);
            if (!artifact.areRelated(relTypeSide, otherArtifact)) {
               artifact.putRelation(tokenService.getRelationTypeOrCreate(typeId), side, otherArtifact);
            }
            if (OptionsUtil.getIncludeLatestTransactionDetails(queryData.getRootQueryData().getOptions())) {

               if (artifact.getTxDetails().getAuthor().isInvalid()) {

                  Date time = null;

                  String maxTime = stmt.getString("max_time").replaceAll("'", "");
                  String timeStr = stmt.getString("timeStr").replaceAll("'", "");
                  if (maxTime.equals(timeStr)) {
                     try {
                        time = DateUtil.getDate("yyyyMMddHHmmss", timeStr);
                     } catch (ParseException ex) {
                        time = DateUtil.getSentinalDate();
                     }

                     String oseeComment = stmt.getString("osee_comment");
                     int txType = stmt.getInt("tx_type");
                     TransactionId suppTxId = TransactionId.valueOf(stmt.getLong("supp_tx_id"));
                     ArtifactId commitArtId = ArtifactId.valueOf(stmt.getLong("commit_art_id"));
                     Long buildId = stmt.getLong("build_id");
                     ArtifactId author = ArtifactId.valueOf(stmt.getLong("author"));
                     artifact.getTxDetails().setTime(time);
                     artifact.getTxDetails().setAuthor(author);
                     artifact.getTxDetails().setOseeComment(oseeComment);
                     artifact.getTxDetails().setTxType(txType);
                     artifact.getTxDetails().setTxId(suppTxId);
                     artifact.getTxDetails().setCommitArtId(commitArtId);
                     artifact.getTxDetails().setBuild_id(buildId);
                  }
               }
            }

         }
      };

      selectiveArtifactLoad(queryData, numArtifacts, jdbcConsumer);
   }

   private void loadArtifactsInto(QueryData queryData, QueryFactory queryFactory,
      Consumer<ArtifactReadable> artifactConsumer, int numArtifacts) {
      // make the HashMap capacity large enough to accommodate the artifactCount given the default load factor of 75%
      HashMap<ArtifactId, ArtifactReadable> artifactMap = new HashMap<>((int) (numArtifacts / 0.75) + 1);
      List<ArtifactId> consumedArts = new LinkedList<>();
      Consumer<JdbcStatement> jdbcConsumer = stmt -> {
         Long artId = stmt.getLong("art_id");
         GammaId artGamma = GammaId.valueOf(stmt.getLong("art_gamma_id"));
         Long attr_id = stmt.getLong("attr_id");
         Long otherArtId = stmt.getLong("other_art_id");
         Long otherArtType = stmt.getLong("other_art_type_id");
         Long typeId = stmt.getLong("type_id");
         String value = stmt.getString("value");
         String uri = stmt.getString("uri");

         ArtifactReadableImpl artifact = (ArtifactReadableImpl) artifactMap.get(ArtifactId.valueOf(artId));
         if (artifact == null && artGamma != null) {
            artifact = createArtifact(stmt, artId, stmt.getLong("art_type_id"), queryData, queryFactory, artGamma);
            artifactMap.put(artifact, artifact);
            if (stmt.getLong("top") == 1) {
               artifactConsumer.accept(artifact);
               consumedArts.add(ArtifactId.valueOf(artifact.getId()));
            } else {
               //if we have attr_id AND other artid then we are a reference artifact

               if (attr_id > 0 && otherArtId > 0 && otherArtType > 0) {
                  //see if the parent artifact has been created, if so add to the resourceArtifacts hash map
                  //if not, create? like relation logic?
                  Long sourceAttrId = stmt.getLong("rel_type"); //overloading the rel_type column as attr Id for reference artifacts
                  ArtifactReadableImpl sourceArtifact =
                     (ArtifactReadableImpl) artifactMap.get(ArtifactId.valueOf(otherArtId));
                  if (sourceArtifact != null) {
                     List<IAttribute<?>> values = sourceArtifact.getAttributesHashCollection().getValues();
                     Optional<IAttribute<?>> findFirst =
                        values.stream().filter(a -> a.getId().equals(sourceAttrId)).findFirst();
                     if (findFirst.isPresent()) {
                        sourceArtifact.putReferenceArtifact(AttributeId.valueOf(findFirst.get().getId()), artifact);
                     }
                  }

               }
            }
         } else {
            // If an artifact with top = 1 appears as an "otherArtifact" first, because it's related to an artifact with top = 1, it needs
            // to be added to the consumer as well.
            if (!consumedArts.contains(ArtifactId.valueOf(artifact.getId())) && stmt.getLong("top") == 1) {
               artifactConsumer.accept(artifact);
               consumedArts.add(ArtifactId.valueOf(artifact.getId()));
            }
            /**
             * if artifact is already in the map that means it was first found as a relation and applicability may not
             * have been set
             */
            Long applicId = stmt.getLong("app_id");
            artifactMap.get(ArtifactId.valueOf(artId)).getApplicabilityToken().setId(applicId);
            if (OptionsUtil.getIncludeApplicabilityTokens(queryData.getRootQueryData().getOptions())) {
               String applicValue = stmt.getString("app_value");
               artifactMap.get(ArtifactId.valueOf(artId)).getApplicabilityToken().setName(applicValue);
            }
         }

         if (attr_id != 0) {
            AttributeTypeGeneric<?> attributeType = tokenService.getAttributeTypeOrCreate(typeId);
            GammaId gamma = GammaId.valueOf(stmt.getLong("gamma_id"));
            Attribute<?> attribute =
               new Attribute<>(stmt.getLong("attr_id"), attributeType, value, uri, gamma, resourceManager);
            // Check if the attribute value has been added already to the artifact to prevent duplicate values.
            // This can happen when the same object is returned multiple times when following relations.
            if (!artifact.getAttributeList(attributeType).stream().anyMatch(a -> a.getId().equals(attribute.getId()))) {
               artifact.putAttributeValue(attributeType, attribute);
            }
            if (OptionsUtil.getIncludeLatestTransactionDetails(queryData.getRootQueryData().getOptions())) {

               if (artifact.getTxDetails().getAuthor().isInvalid()) {

                  Date time = null;

                  String maxTime = stmt.getString("max_time").replaceAll("'", "");
                  String timeStr = stmt.getString("timeStr").replaceAll("'", "");
                  if (maxTime.equals(timeStr)) {
                     try {
                        int indexOf = timeStr.indexOf(".");
                        int endIndex = timeStr.length() - 1;
                        if (indexOf > 0) {
                           endIndex = indexOf;
                        }
                        time = DateUtil.getDate("yyyyMMddHHmmss", timeStr);
                     } catch (ParseException ex) {
                        time = DateUtil.getSentinalDate();
                     }
                     String oseeComment = stmt.getString("osee_comment");
                     int txType = stmt.getInt("tx_type");
                     TransactionId suppTxId = TransactionId.valueOf(stmt.getLong("supp_tx_id"));
                     ArtifactId commitArtId = ArtifactId.valueOf(stmt.getLong("commit_art_id"));
                     Long buildId = stmt.getLong("build_id");
                     ArtifactId author = ArtifactId.valueOf(stmt.getLong("author"));
                     artifact.getTxDetails().setTime(time);
                     artifact.getTxDetails().setAuthor(author);
                     artifact.getTxDetails().setOseeComment(oseeComment);
                     artifact.getTxDetails().setTxType(txType);
                     artifact.getTxDetails().setTxId(suppTxId);
                     artifact.getTxDetails().setCommitArtId(commitArtId);
                     artifact.getTxDetails().setBuild_id(buildId);
                  }
               }
            }
         } else if (attr_id == 0) {
            RelationSide side = value.equals("A") ? RelationSide.SIDE_A : RelationSide.SIDE_B;

            ArtifactReadableImpl otherArtifact = (ArtifactReadableImpl) artifactMap.get(ArtifactId.valueOf(otherArtId));
            if (otherArtifact == null) {
               GammaId gamma = GammaId.valueOf(stmt.getLong("other_art_gamma_id"));
               otherArtifact = createArtifact(stmt, otherArtId, otherArtType, queryData, queryFactory, gamma);
               artifactMap.put(otherArtifact, otherArtifact);
            }
            /*
             * If using followSearch, multi rows of a relation may return in some conditions e.g. followSearch Ideally
             * should try and updates handlers so that select * isn't used, instead explicity calling out columns so
             * that some can be excluded in final select
             */

            RelationTypeSide relTypeSide = new RelationTypeSide(tokenService.getRelationTypeOrCreate(typeId), side);
            if (!artifact.areRelated(relTypeSide, otherArtifact)) {
               artifact.putRelation(tokenService.getRelationTypeOrCreate(typeId), side, otherArtifact);
            }
            if (OptionsUtil.getIncludeLatestTransactionDetails(queryData.getRootQueryData().getOptions())) {

               if (artifact.getTxDetails().getAuthor().isInvalid()) {

                  Date time = null;

                  String maxTime = stmt.getString("max_time").replaceAll("'", "");
                  String timeStr = stmt.getString("timeStr").replaceAll("'", "");
                  if (maxTime.equals(timeStr)) {
                     try {
                        time = DateUtil.getDate("yyyyMMddHHmmss", timeStr);
                     } catch (ParseException ex) {
                        time = DateUtil.getSentinalDate();
                     }

                     String oseeComment = stmt.getString("osee_comment");
                     int txType = stmt.getInt("tx_type");
                     TransactionId suppTxId = TransactionId.valueOf(stmt.getLong("supp_tx_id"));
                     ArtifactId commitArtId = ArtifactId.valueOf(stmt.getLong("commit_art_id"));
                     Long buildId = stmt.getLong("build_id");
                     ArtifactId author = ArtifactId.valueOf(stmt.getLong("author"));
                     artifact.getTxDetails().setTime(time);
                     artifact.getTxDetails().setAuthor(author);
                     artifact.getTxDetails().setOseeComment(oseeComment);
                     artifact.getTxDetails().setTxType(txType);
                     artifact.getTxDetails().setTxId(suppTxId);
                     artifact.getTxDetails().setCommitArtId(commitArtId);
                     artifact.getTxDetails().setBuild_id(buildId);
                  }
               }
            }

         }
      };

      selectiveArtifactLoad(queryData, numArtifacts, jdbcConsumer);
   }

   private ArtifactReadableImpl createArtifact(JdbcStatement stmt, Long artId, Long artifactTypeId, QueryData queryData,
      QueryFactory queryFactory, GammaId gammaId) {
      TransactionId txId = TransactionId.valueOf(stmt.getLong("transaction_id"));
      ModificationType modType = ModificationType.valueOf(stmt.getInt("mod_type"));
      ArtifactTypeToken artifactType = tokenService.getArtifactTypeOrCreate(artifactTypeId);
      String appValue = "";
      String oseeComment = "";
      Date time = null;
      int txType = -1;
      ArtifactId author = ArtifactId.SENTINEL;
      ArtifactId commitArtId = ArtifactId.SENTINEL;
      Long buildId = -1L;
      TransactionId suppTxId = TransactionId.SENTINEL;
      if (queryData.mainTableAliasExists(OseeDb.OSEE_KEY_VALUE_TABLE)) {
         appValue = stmt.getString("app_value");
      }
      if (OptionsUtil.getIncludeLatestTransactionDetails(queryData.getRootQueryData().getOptions())) {
         String timeStr = stmt.getString("timeStr").replaceAll("'", "");
         String maxTime = stmt.getString("max_time").replaceAll("'", "");
         if (timeStr.equals(maxTime)) {

            suppTxId = TransactionId.valueOf(stmt.getLong("supp_tx_id"));
            oseeComment = stmt.getString("osee_comment");
            try {
               time = DateUtil.getDate("yyyyMMddHHmmss", timeStr);
            } catch (ParseException ex) {
               time = DateUtil.getSentinalDate();
            }
            txType = stmt.getInt("tx_type");
            commitArtId = ArtifactId.valueOf(stmt.getLong("commit_art_id"));
            buildId = stmt.getLong("build_id");
            author = ArtifactId.valueOf(stmt.getLong("author"));
         }
      }
      ApplicabilityToken applic = ApplicabilityToken.valueOf(stmt.getLong("app_id"), appValue);

      TransactionDetails txDetails = new TransactionDetails(suppTxId, queryData.getBranch(), time, oseeComment, txType,
         commitArtId, buildId, author);

      return new ArtifactReadableImpl(artId, artifactType, queryData.getBranch(), queryData.getView(), applic, txId,
         txDetails, modType, queryFactory, gammaId);
   }

   @Override
   public List<Map<String, Object>> asArtifactMaps(QueryData queryData) {
      List<Map<String, Object>> maps = new ArrayList<>(DefaultArtifactNum);
      HashCollection<AttributeTypeToken, Object> attributes = new HashCollection<>();
      Long[] artifactId = new Long[] {Id.SENTINEL};
      Long[] applicability = new Long[] {Id.SENTINEL};
      ArtifactTypeToken[] artifactType = new ArtifactTypeToken[] {ArtifactTypeToken.SENTINEL};

      Consumer<JdbcStatement> consumer = stmt -> {
         Long newArtId = stmt.getLong("art_id");
         if (artifactId[0].equals(Id.SENTINEL)) {
            artifactId[0] = newArtId;
            applicability[0] = stmt.getLong("app_id");
            artifactType[0] = tokenService.getArtifactTypeOrCreate(stmt.getLong("art_type_id"));
         } else if (!artifactId[0].equals(newArtId)) {
            maps.add(createFieldMap(artifactType[0], artifactId[0], applicability[0], attributes));
            attributes.clear();
            artifactId[0] = newArtId;
            applicability[0] = stmt.getLong("app_id");
            artifactType[0] = tokenService.getArtifactTypeOrCreate(stmt.getLong("art_type_id"));
         }

         attributes.put(tokenService.getAttributeTypeOrCreate(stmt.getLong("type_id")), stmt.getString("value"));
      };
      selectiveArtifactLoad(queryData, DefaultArtifactNum, consumer);
      if (!artifactId[0].equals(Id.SENTINEL)) {
         maps.add(createFieldMap(artifactType[0], artifactId[0], applicability[0], attributes));
      }
      return maps;
   }

   private Map<String, Object> createFieldMap(ArtifactTypeToken artifactType, Long artifactId, Long applicability,
      HashCollection<AttributeTypeToken, Object> attributes) {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("Name", attributes.getValues(Name));
      map.put("Artifact Type", artifactType.getIdString());
      map.put("Artifact Id", artifactId.toString());
      map.put("Applicability Id", applicability.toString());

      List<AttributeTypeToken> attributeTypes = new ArrayList<>(attributes.keySet());
      Collections.sort(attributeTypes);
      for (AttributeTypeToken attributeType : attributeTypes) {
         List<Object> attributeValues = attributes.getValues(attributeType);
         if (attributeValues.size() == 1) {
            map.put(attributeType.getName(), attributeValues.get(0));
         } else {
            map.put(attributeType.getName(), attributeValues);
         }
      }
      return map;
   }

   @Override
   public Map<ArtifactId, ArtifactToken> asArtifactTokenMap(QueryData queryData) {
      Map<ArtifactId, ArtifactToken> tokens = new HashMap<>(DefaultMapCapacity);
      Consumer<JdbcStatement> consumer = stmt -> {
         ArtifactToken token = ArtifactToken.valueOf(stmt.getLong("art_id"), stmt.getString("value"),
            queryData.getBranch(), tokenService.getArtifactTypeOrCreate(stmt.getLong("art_type_id")));
         tokens.put(token, token);
      };

      selectiveArtifactLoad(queryData, DefaultArtifactNum, consumer);
      return tokens;
   }

   @Override
   public List<ArtifactId> asArtifactIds(QueryData queryData) {
      List<ArtifactId> ids = new ArrayList<>(DefaultArtifactNum);

      if (isPostProcessRequired(queryData)) {
         LoadDataHandlerAdapter handler = new LoadDataHandlerAdapter() {
            @Override
            public void onData(ArtifactData data) {
               ids.add(data);
            }
         };
         OptionsUtil.setLoadLevel(queryData.getOptions(), LoadLevel.ARTIFACT_AND_ATTRIBUTE_DATA);
         try {
            runArtifactQuery(queryData, handler);
            return ids;
         } catch (Exception ex) {
            OseeCoreException.wrapAndThrow(ex);
         }
      }

      selectiveArtifactLoad(queryData, DefaultArtifactNum, stmt -> ids.add(ArtifactId.valueOf(stmt.getLong("art_id"))));
      return ids;
   }

   private void selectiveArtifactLoad(QueryData queryData, int numArtifacts, Consumer<JdbcStatement> consumer) {
      QueryData rootQueryData = queryData.getRootQueryData();
      new SelectiveArtifactSqlWriter(sqlJoinFactory, jdbcClient, rootQueryData).runSql(consumer, handlerFactory,
         numArtifacts);
   }

   @Override
   public ApplicabilityDsQuery createApplicabilityDsQuery() {
      return new ApplicabilityDsQueryImpl(jdbcClient, sqlJoinFactory);
   }

   @Override
   public boolean isArchived(BranchId branchId) {
      String SELECT_IS_BRANCH_ARCHIVED = "select archived from osee_branch where branch_id = ?";
      Integer result = jdbcClient.fetchOrException(Integer.class,
         () -> new OseeCoreException("Failed to get Branch archived state for %s", branchId), SELECT_IS_BRANCH_ARCHIVED,
         branchId);
      return BranchArchivedState.valueOf(result.intValue()).isArchived();
   }

   @Override
   public void getBranchCategoryGammaIds(Consumer<GammaId> consumer, BranchId branchId, BranchCategoryToken category) {
      String SELECT_BRANCH_CATEGORY_GAMMA =
         "SELECT gamma_id FROM osee_branch_category WHERE branch_id=? AND category = ?";
      jdbcClient.runQuery(stmt -> consumer.accept(GammaId.valueOf(stmt.getLong("gamma_id"))),
         SELECT_BRANCH_CATEGORY_GAMMA, branchId, category);
   }

   @Override
   public void getBranchCategories(Consumer<BranchCategoryToken> consumer, BranchId branchId) {
      jdbcClient.runQuery(stmt -> consumer.accept(BranchCategoryToken.valueOf(stmt.getLong("category"))),
         OseeSql.GET_CURRENT_BRANCH_CATEGORIES.getSql(), branchId);
   }

   @Override
   public ArtifactTable asArtifactsTable(QueryData queryData, QueryFactory queryFactory) {
      List<ArtifactReadable> artifacts = new ArrayList<>(DefaultArtifactNum);
      loadArtifactsInto(queryData, queryFactory, a -> artifacts.add(a), DefaultArtifactNum);
      int fixedColumnNum = 3;

      ArtifactTableOptions tableOptions = queryData.getTableoptions();
      List<AttributeTypeToken> attributeColumns = tableOptions.getAttributeColumns();
      String[][] data = new String[artifacts.size()][attributeColumns.size() + fixedColumnNum];

      if (attributeColumns.isEmpty()) {
         HashSet<AttributeTypeToken> columnMap = new HashSet<AttributeTypeToken>();
         for (ArtifactReadable artifact : artifacts) {

            columnMap.addAll(artifact.getArtifactType().getValidAttributeTypes());
         }
         attributeColumns.addAll(columnMap);
         Collections.sort(attributeColumns);
      }

      for (int i = 0; i < artifacts.size(); i++) {
         ArtifactReadable artifact = artifacts.get(i);
         data[i][0] = artifact.getIdString();
         data[i][1] = artifact.getArtifactType().getName();
         data[i][2] = artifact.getApplicability().getIdString();

         for (int j = fixedColumnNum; j < data[i].length; j++) {
            data[i][j] = artifact.getAttributeValuesAsString(attributeColumns.get(j - fixedColumnNum));
         }

      }

      return new ArtifactTable(tableOptions, data);
   }

}