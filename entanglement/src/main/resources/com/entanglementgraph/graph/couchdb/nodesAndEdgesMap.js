
/*
 * A CouchDB View 'map' function that iterates over available patchsets and emits NodeUpdate and EdgeUpdate
 * view items with the one of the following key structures:
 * [ <type>, <UID>,           [0], [ all UIDS ] ]
 * [ <FROM type>, <FROM UID>, [1], [ all FROM UIDs ], EDGE type, [ EDGE UIDs] ]
 * [ <TO type>, <TO UID>,     [2], [ all TO UIDs ], EDGE type, [ EDGE UIDs] ]
 *
 * Values are either of the type NodeUpdateView or EdgeUpdateView.
 *
 * @author Keith Flanagan
 */

function(doc) {
  if(doc.nodeUpdates) {
    for (u=0; u<doc.nodeUpdates.length; u=u+1) {
      var update = doc.nodeUpdates[u];
      var node = update.node;
      var allUids;
      if (node.keys.uids) { allUids = node.keys.uids; } else { allUids = []; }

      //Create a NodeUpdateView (based on the NodeUpdate, but with additional properties from the revision container)
      var nodeUpdateView = nodeUpdateToNodeUpdateView(doc, update);

      for (i=0; i < allUids.length; i=i+1) {
        var outKey = [node.keys.type, allUids[i], 0, allUids];
        emit(outKey, nodeUpdateView);
      }
    }
  }

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

      // Emit 'from' node entries
      for (i=0; i < allFromUids.length; i=i+1) {
        var outKey = [edge.from.type, allFromUids[i], 1, allFromUids, edge.keys.type, allEdgeUids];
        emit(outKey, edgeUpdateView);
      }

      // Emit 'to' node entries
      for (i=0; i < allToUids.length; i=i+1) {
        var outKey = [edge.to.type, allToUids[i], 2, allToUids, edge.keys.type, allEdgeUids];
        emit(outKey, edgeUpdateView);
      }
    }
  }
}

/*
 * Takes a revision item container, and a NodeUpdate.
 * Returns a NodeUpdate with additional properties taken from the container (such as graph ID, timestamp, etc).
 * The returned result is compatible with the NodeUpdateView Java bean and contains fields useful for integration.
 */
function nodeUpdateToNodeUpdateView(revisionItemContainer, update) {
  var outVal = new Object();

  // Add all fields from the NodeUpdate
  outVal.mergePol = update.mergePol;
  outVal.node = update.node;

  // Append additional fields from the root RevisionItemContainer
  outVal.timestamp = revisionItemContainer.timestamp;
  outVal.graphUid = revisionItemContainer.graphUid;
  outVal.patchUid = revisionItemContainer.patchUid;

  return outVal;

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
