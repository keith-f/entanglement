
/*
 * A CouchDB View 'map' function that iterates over available patchsets and EdgeUpdates only to create an index
 * that can be used to search for edges between two specified nodes.
 *
 * View items have the following key structures:
 * [ <FROM node type>, <FROM UID>, <TO node type>, <TO UID>, <edge type>, [ all edge UIDS ] ]
 *
 * also:
 * [ <TO node type>, <TO UID>, <FROM node type>, <FROM UID>, <edge type>, [ all edge UIDS ] ]
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
      if (edge.keys.uids) { allEdgeUids = edge.keys.uids; } else { allEdgeUids = []; }

      var allFromUids;
      if (edge.from.uids) { allFromUids = edge.from.uids; } else { allFromUids = []; }

      var allToUids;
      if (edge.to.uids) { allToUids = edge.to.uids; } else { allToUids = []; }

      //Create a EdgeUpdateView (based on the EdgeUpdate, but with additional properties from the revision container)
      var edgeUpdateView = edgeUpdateToEdgeUpdateView(doc, update);

      // Emit 'from' node UID entries --> 'to' node UID entries
      for (i=0; i < allFromUids.length; i=i+1) {
        for (j=0; j < allToUids.length; j=j+1) {
          var outKey = [edge.from.type, allFromUids[i], edge.to.type, allToUids[j], edge.keys.type, allEdgeUids];
          emit(outKey, edgeUpdateView);
        }
      }

      // Emit 'to' node UID entries --> 'from' node UID entries
      for (i=0; i < allToUids.length; i=i+1) {
        for (j=0; j < allFromUids.length; j=j+1) {
          var outKey = [edge.to.type, allToUids[i], edge.from.type, allFromUids[j], edge.keys.type, allEdgeUids];
          emit(outKey, edgeUpdateView);
        }
      }
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
