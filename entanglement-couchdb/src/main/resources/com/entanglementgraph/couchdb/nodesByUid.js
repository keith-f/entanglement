
function(doc) {
  if(doc.nodeUpdates) {
    for (u=0; u<doc.nodeUpdates.length; u=u+1) {
      update = doc.nodeUpdates[u];
      node = update.node;
      keys = node.keys;
      if (keys.uids) {
        // Pick any UID as the view key. This is fine as long as we resolve all names prior to querying this view.
        var outKey = [keys.uids[0], doc.timestamp];
        var outVal = new Object();

        // Add all fields from the NodeModification
        outVal.mergePol = update.mergePol;
        outVal.node = update.node;

        // Append additional fields from the root RevisionItemContainer
        outVal.timestamp = doc.timestamp;
        outVal.graphUid = doc.graphUid;
        outVal.patchUid = doc.patchUid;

        emit(outKey, outVal);
      }
    }
  }
}

