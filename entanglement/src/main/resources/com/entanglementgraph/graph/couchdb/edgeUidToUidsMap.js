
/*
 * If a keyset contains at least 2 UIDs, this function emits a mapping of uid -> [ all uids ] for each UID in
 * the keyset. For example, if a edge has UIDs = [a, b, c], then the output will be:
 * a -> [a, b, c]
 * b -> [a, b, c]
 * c -> [a, b, c]
 */
function(doc) {
  if(doc.edgeUpdates) {
    for (u=0; u<doc.edgeUpdates.length; u=u+1) {
      update = doc.edgeUpdates[u];
      edge = update.edge;
      keys = edge.keys;

      if (keys.uids && keys.uids.length >=1) {
        // Pick any UID as the view key. This is fine as long as we resolve all UIDs prior to querying this view.
        outKey = keys.uids[0];
        emit(outKey, update);
      }
    }
  }
}

