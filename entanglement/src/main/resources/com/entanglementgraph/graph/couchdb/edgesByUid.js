
function(doc) {
  if(doc.edgeUpdates) {
    for (u=0; u<doc.edgeUpdates.length; u=u+1) {
      update = doc.edgeUpdates[u];
      edge = update.edge;
      keys = edge.keys;
      if (keys.uids) {
        // Pick any UID as the view key. This is fine as long as we resolve all names prior to querying this view.
        var outKey = [keys.uids[0], doc.timestamp];
        var outVal = new Object();

        // Add all fields from the NodeModification
        outVal.mergePol = update.mergePol;
        outVal.edge = update.edge;

        // Append additional fields from the root RevisionItemContainer
        outVal.timestamp = doc.timestamp;
        outVal.graphUid = doc.graphUid;
        outVal.patchUid = doc.patchUid;
        
        emit(outKey, outVal);
      }
    }
  }
}

