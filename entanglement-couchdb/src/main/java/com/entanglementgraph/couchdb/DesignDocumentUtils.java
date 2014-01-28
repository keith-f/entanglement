/*
 * Copyright 2013 Keith Flanagan
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.entanglementgraph.couchdb;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.ektorp.CouchDbConnector;
import org.ektorp.UpdateConflictException;
import org.ektorp.support.DesignDocument;
import org.ektorp.support.DesignDocumentFactory;
import org.ektorp.support.StdDesignDocumentFactory;

import java.util.Random;

/**
 * A couple of utilities, mostly copied from Ektorp's <code>CouchDbRepositorySupport</code>. However by separating these
 * methods, we can use the <code>@View</code> annotations outside of a 'repository' since this is a better fit for
 * Entanglement.
 *
 * @author Keith Flanagan
 */
public class DesignDocumentUtils {
  private void initDesignDocInternal(CouchDbConnector db, Object viewAnnotatedObject) {
    DesignDocumentFactory docFact = new StdDesignDocumentFactory();
    DesignDocument designDoc;
    String stdDesignDocumentId = "_design/"+viewAnnotatedObject.getClass().getSimpleName();
    if (db.contains(stdDesignDocumentId)) {
      designDoc = docFact.getFromDatabase(db, stdDesignDocumentId);
    } else {
      designDoc = docFact.newDesignDocumentInstance();
      designDoc.setId(stdDesignDocumentId);
    }
//    System.out.println("Generating DesignDocument for: "+ getHandledType());
    DesignDocument generated = docFact.generateFrom(viewAnnotatedObject);
    boolean changed = designDoc.mergeWith(generated);
//    if (log.isDebugEnabled()) {
//      debugDesignDoc(designDoc);
//    }
    if (changed) {
      System.out.println("DesignDocument changed or new. Updating database");
      for (int i=0; i<5; i++) {
        try {
          db.update(designDoc);
          break; // Don't retry if successful
        } catch (UpdateConflictException e) {
          System.out.println("Update conflict occurred when trying to update design document: "+ designDoc.getId());
          backOff();
          System.out.println("retrying initStandardDesignDocument for design document: "+ designDoc.getId());
        }
      }
    } else {
      System.out.println("DesignDocument was unchanged. Database was not updated.");
    }
  }

  /**
   * Wait a short while in order to prevent racing initializations from other repositories.
   */
  private void backOff() {
    try {
      Thread.sleep(new Random().nextInt(400));
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
    }
  }

//  protected void debugDesignDoc(DesignDocument generated) {
//    ObjectMapper om = new ObjectMapper();
//    om.configure(SerializationFeature.INDENT_OUTPUT, true);
//    om.getSerializationConfig().withSerializationInclusion(JsonInclude.Include.NON_NULL);
//    try {
//      String json = om.writeValueAsString(generated);
//      //log.debug("DesignDocument source:\n" + json);
//    } catch (Exception e) {
//      e.printStackTrace();
//      //log.error("Could not write generated design document as json", e);
//    }
//  }

//  protected DesignDocumentFactory getDesignDocumentFactory() {
//    if (designDocumentFactory == null) {
//      designDocumentFactory = new StdDesignDocumentFactory();
//    }
//    return designDocumentFactory;
//  }
}
