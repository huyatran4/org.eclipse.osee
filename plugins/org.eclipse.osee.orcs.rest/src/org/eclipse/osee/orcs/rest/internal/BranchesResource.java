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

package org.eclipse.osee.orcs.rest.internal;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import org.eclipse.osee.framework.core.data.Branch;
import org.eclipse.osee.framework.core.data.BranchId;
import org.eclipse.osee.framework.core.enums.BranchType;
import org.eclipse.osee.framework.jdk.core.annotation.Swagger;
import org.eclipse.osee.framework.jdk.core.type.ResultSet;
import org.eclipse.osee.jdbc.JdbcService;
import org.eclipse.osee.orcs.OrcsApi;
import org.eclipse.osee.orcs.rest.internal.applicability.ApplicabilityEndpointImpl;
import org.eclipse.osee.orcs.rest.internal.applicability.ApplicabilityWebEndpointImpl;
import org.eclipse.osee.orcs.rest.model.ApplicabilityEndpoint;
import org.eclipse.osee.orcs.rest.model.ApplicabilityWebEndpoint;
import org.eclipse.osee.orcs.rest.model.ArtifactEndpoint;
import org.eclipse.osee.orcs.rest.model.GridCommanderEndpoint;
import org.eclipse.osee.orcs.rest.model.RelationEndpoint;
import org.eclipse.osee.orcs.rest.model.TupleEndpoint;
import org.eclipse.osee.orcs.search.BranchQuery;

/**
 * @author Roberto E. Escobar
 */
@Path("branch")
@Swagger
public class BranchesResource {
   private final OrcsApi orcsApi;
   private final JdbcService jdbcService;

   @Context
   UriInfo uriInfo;

   public BranchesResource(OrcsApi orcsApi, JdbcService jdbcService) {
      this.orcsApi = orcsApi;
      this.jdbcService = jdbcService;
   }

   @Path("{uuid}")
   public BranchResource getBranch(@PathParam("uuid") BranchId id) {
      return new BranchResource(uriInfo, id, orcsApi);
   }

   @POST
   @Path("nameQuery")
   @Consumes({MediaType.TEXT_PLAIN})
   @Produces({MediaType.APPLICATION_JSON})
   public BranchId getBranchByName(String branchName) {
      return orcsApi.getQueryFactory().branchQuery().andNameEquals(branchName).getResults().getExactlyOne();
   }

   @GET
   @Produces(MediaType.TEXT_HTML)
   public String getAsHtml() {
      BranchQuery query = orcsApi.getQueryFactory().branchQuery();
      ResultSet<Branch> results = query.andIsOfType(BranchType.BASELINE, BranchType.WORKING).getResults();

      HtmlWriter writer = new HtmlWriter(uriInfo, orcsApi);
      return writer.toHtml(results);
   }

   @Path("{branch}/tuple")
   public TupleEndpoint getTuples(@PathParam("branch") BranchId branch) {
      return new TupleEndpointImpl(orcsApi, branch);
   }

   @Path("{branch}/applic")
   public ApplicabilityEndpoint getApplicability(@PathParam("branch") BranchId branch) {
      return new ApplicabilityEndpointImpl(orcsApi, branch);
   }

   @Path("{branch}/artifact")
   public ArtifactEndpoint getArtifact(@PathParam("branch") BranchId branch) {
      return new ArtifactEndpointImpl(orcsApi, branch, uriInfo);
   }

   @Path("{branch}/relation")
   public RelationEndpoint getRelation(@PathParam("branch") BranchId branch) {
      return new RelationEndpointImpl(orcsApi, jdbcService, branch);
   }

   @Path("{branch}/gc")
   public GridCommanderEndpoint getGridCommander(@PathParam("branch") BranchId branch) {
      return new GridCommanderEndpointImpl(orcsApi, branch, uriInfo);
   }

   @Path("{branch}/applicability")
   public ApplicabilityWebEndpoint getApplicabilityWeb(@PathParam("branch") BranchId branch) {
      return new ApplicabilityWebEndpointImpl(orcsApi, branch);
   }
}