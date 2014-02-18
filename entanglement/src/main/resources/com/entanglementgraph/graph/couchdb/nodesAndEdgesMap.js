
/*
 * A CouchDB View 'map' function that iterates over available patchsets and emits NodeUpdate and EdgeUpdate
 * view items with the one of the following key structures:
 * [ "U|N", <type>, <UID|Name>, [0|1], [ all UIDS ], [all Names]
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
      var allNames;
      if (node.keys.uids) { allUids = node.keys.uids; } else { allUids = []; }
      if (node.keys.names) { allNames = node.keys.names; } else { allNames = []; }

      //Create a NodeUpdateView (based on the NodeUpdate, but with additional properties from the revision container)
      var nodeUpdateView = nodeUpdateToNodeUpdateView(doc, update);

      for (i=0; i < allUids.length; i=i+1) {
        var outKey = ["U", node.keys.type, allUids[i], 0, allUids, allNames];
        emit(outKey, nodeUpdateView);
      }


      for (i=0; i < allNames.length; i=i+1) {
        var outKey = ["N", node.keys.type, allNames[i], 0, allUids, allNames];
        emit(outKey, nodeUpdateView);
      }
    }
  }

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

      //Create a EdgeUpdateView (based on the EdgeUpdate, but with additional properties from the revision container)
      var edgeUpdateView = edgeUpdateToEdgeUpdateView(doc, update);

      for (i=0; i < allFromUids.length; i=i+1) {
        var outKey = ["U", node.keys.type, allFromUids[i], 1, allFromUids, allFromNames, edge.keys.type, allEdgeUids, allEdgeNames];
        emit(outKey, edgeUpdateView);
      }


      for (i=0; i < allFromNames.length; i=i+1) {
        var outKey = ["N", node.keys.type, allFromNames[i], 1, allFromUids, allFromNames, edge.keys.type, allEdgeUids, allEdgeNames];
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
