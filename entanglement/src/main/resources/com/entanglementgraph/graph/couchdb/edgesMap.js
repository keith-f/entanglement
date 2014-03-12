
/*
 * A CouchDB View 'map' function that iterates over available patchsets (EdgeUpdates only).
 * View items have the following key structures:
 * [ <type>, <UID>, [0], [ all UIDS ] ]
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

      //Create a EdgeUpdateView (based on the EdgeUpdate, but with additional properties from the revision container)
      var edgeUpdateView = edgeUpdateToEdgeUpdateView(doc, update);

      for (i=0; i < allEdgeUids.length; i=i+1) {
        var outKey = [edge.keys.type, allEdgeUids[i], 0, allEdgeUids];
        emit(outKey, edgeUpdateView);
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
