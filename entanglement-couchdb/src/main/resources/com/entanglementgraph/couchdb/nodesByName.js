
function(doc) {
  if(doc.nodeUpdates) {
    for (u=0; u<doc.nodeUpdates.length; u=u+1) {
      update = doc.nodeUpdates[u];
      node = update.node;
      keys = node.keys;
      if (keys.names && keys.names.length >=1) {
        // Pick any name as the view key. This is fine as long as we resolve all names prior to querying this view.
          outKey = [keys.type, keys.names[0], doc.timestamp];
          emit(outKey, update);
//        for (i=0; i<keys.names.length; i=i+1) {
//          outKey = [keys.type, keys.names[i], doc.timestamp];
//          emit(outKey, update);
//        }
      }
    }
  }
}

