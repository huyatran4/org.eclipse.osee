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

package org.eclipse.osee.testscript;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.eclipse.osee.framework.core.data.BranchId;
import org.eclipse.osee.framework.core.data.TransactionResult;
import org.eclipse.osee.framework.jdk.core.annotation.Swagger;

/**
 * @author Ryan T. Baldwin
 */
@Path("config")
@Swagger
public interface ScriptConfigEndpoint {

   @GET
   @Path("{branchId}")
   @Produces(MediaType.APPLICATION_JSON)
   public ScriptConfigToken getConfig(@PathParam("branchId") BranchId branchId);

   @POST
   @Path("{branchId}")
   @Produces(MediaType.APPLICATION_JSON)
   @Consumes(MediaType.APPLICATION_JSON)
   public TransactionResult createScriptConfiguration(@PathParam("branchId") BranchId branchId);

}