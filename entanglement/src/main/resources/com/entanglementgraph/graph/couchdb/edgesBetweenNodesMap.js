
/*
 * A CouchDB View 'map' function that iterates over available patchsets and EdgeUpdates only to create an index
 * that can be used to search for edges between two specified nodes.
 *
 * View items have the following key structures:
 * [ <from node type>, "U|N", <UID|Name>, <to node type>, "U|N", <UID|Name>, <edge type>, [ all edge UIDS ], [all edge names] ]
 *
 * Values are of type EdgeUpdateView.
 *
 * @author Keith Flanagan
 */

function(doc) {
  if(doc.edgeUpdates) {
    for (u=0; u<doc.edgeUpdates.length; u=u+1) {
      var update = doc.edgeUpdates[u];
      var edge = update.edge;

      var allEdgeUids;
      var allEdgeNames;
      if (edge.keys.uids) { allEdgeUids = edge.keys.uids; } else { allEdgeUids = []; }
      if (edge.keys.names) { allEdgeNames = edge.keys.names; } else { allEdgeNames = []; }

      var allFromUids;
      var allFromNames;
      if (edge.from.uids) { allFromUids = edge.from.uids; } else { allFromUids = []; }
      if (edge.from.names) { allFromNames = edge.from.names; } else { allFromNames = []; }

      var allToUids;
      var allToNames;
      if (edge.to.uids) { allToUids = edge.to.uids; } else { allToUids = []; }
      if (edge.to.names) { allToNames = edge.to.names; } else { allToNames = []; }

      //Create a EdgeUpdateView (based on the EdgeUpdate, but with additional properties from the revision container)
      var edgeUpdateView = edgeUpdateToEdgeUpdateView(doc, update);


      // Emit 'from' node UID entries --> 'to' node UID/Name entries
      for (i=0; i < allFromUids.length; i=i+1) {
        for (j=0; i < allToUids.length; j=j+1) {
          var outKey = [edge.from.type, "U", allFromUids[i], edge.to.type, "U", allToUids[j], edge.keys.type, allEdgeUids, allEdgeNames];
          emit(outKey, edgeUpdateView);
        }
        for (j=0; i < allToNames.length; j=j+1) {
          var outKey = [edge.from.type, "U", allFromUids[i], edge.to.type, "N", allToNames[j], edge.keys.type, allEdgeUids, allEdgeNames];
          emit(outKey, edgeUpdateView);
        }
      }

      // Emit 'from' node Name entries --> 'to' node UID/Name entries
      for (i=0; i < allFromNames.length; i=i+1) {
        for (j=0; i < allToUids.length; j=j+1) {
          var outKey = [edge.from.type, "N", allFromNames[i], edge.to.type, "U", allToUids[j], edge.keys.type, allEdgeUids, allEdgeNames];
          emit(outKey, edgeUpdateView);
        }
        for (j=0; i < allToNames.length; j=j+1) {
          var outKey = [edge.from.type, "N", allFromNames[i], edge.to.type, "N", allToNames[j], edge.keys.type, allEdgeUids, allEdgeNames];
          emit(outKey, edgeUpdateView);
        }
      }

      /*
       * We could consider doing the above for 'to' node entries for convenience, but it would bloat (double) the
       * index size.
       * For now we don't do this (and instead need to perform two queries when finding edges between nodes).
       */
    }
  }
}

/*
 * Takes a revision item container, and an EdgeUpdate.
 * Returns a EdgeUpdate with additional properties taken from the container (such as graph ID, timestamp, etc).
 * The returned result is compatible with the EdgeUpdateView Java bean and contains fields useful for integration.
 */
function edgeUpdateToEdgeUpdateView(revisionItemContainer, update){
  var outVal = new Object();

  // Add all fields from the EdgeUpdate
  outVal.mergePol = update.mergePol;
  outVal.edge = update.edge;

  // Append additional fields from the root RevisionItemContainer
  outVal.timestamp = revisionItemContainer.timestamp;
  outVal.graphUid = revisionItemContainer.graphUid;
  outVal.patchUid = revisionItemContainer.patchUid;

  return outVal;
}
