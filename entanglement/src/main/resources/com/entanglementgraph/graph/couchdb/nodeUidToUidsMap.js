
/*
 * If a keyset contains at least 2 UIDs, this function emits a mapping of uid -> [ all uids ] for each UID in
 * the keyset. For example, if a node has UIDs = [a, b, c], then the output will be:
 * a -> [a, b, c]
 * b -> [a, b, c]
 * c -> [a, b, c]
 */
function(doc) {
  if(doc.nodeUpdates) {
    for (u=0; u<doc.nodeUpdates.length; u=u+1) {
      update = doc.nodeUpdates[u];
      node = update.node;
      keys = node.keys;

      if (keys.uids && keys.uids.length >=1) {
        // Pick any UID as the view key. This is fine as long as we resolve all UIDs prior to querying this view.
        outKey = keys.uids[0];
        emit(outKey, update);
      }

//      if (keys.uids && keys.uids.length > 1) {
//        for (i=0; i<keys.uids.length; i=i+1) {
//          outKey = keys.uids[i];
//          emit(outKey, keys.uids);
//        }
//      }
    }
  }
}

