
/*
 * If a keyset contains at least 2 names, this function emits a mapping of [type, name] -> [ all names ] for each
 * name in the keyset. For example, if a edge has names = [a, b, c], then the output will be:
 * [type, a] -> [a, b, c]
 * [type, b] -> [a, b, c]
 * [type, c] -> [a, b, c]
 */
function(doc) {
  if(doc.edgeUpdates) {
    for (u=0; u<doc.edgeUpdates.length; u=u+1) {
      update = doc.edgeUpdates[u];
      edge = update.edge;
      keys = edge.keys;
      if (keys.names && keys.names.length > 1) {
        for (i=0; i<keys.names.length; i=i+1) {
          outKey = [keys.type, keys.names[i]];
          emit(outKey, keys.names);
        }
      }
    }
  }
}

