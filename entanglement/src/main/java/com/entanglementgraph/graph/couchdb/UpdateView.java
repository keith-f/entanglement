/*
 * Copyright 2013 Keith Flanagan
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.entanglementgraph.graph.couchdb;

import java.util.Comparator;

/**
 * @author Keith Flanagan
 */
public interface UpdateView {

  public static final class TimestampComparator implements Comparator<UpdateView> {
    @Override
    public int compare(UpdateView o1, UpdateView o2) {
      return Long.compare(o1.getTimestamp(), o2.getTimestamp());
    }
  }

  public static final TimestampComparator TIMESTAMP_COMPARATOR = new TimestampComparator();

  public static final class TimestampPatchComparator implements Comparator<UpdateView> {
    @Override
    public int compare(UpdateView o1, UpdateView o2) {
      int tsComp = Long.compare(o1.getTimestamp(), o2.getTimestamp());

      if (tsComp != 0) {
        return tsComp;
      }
      int patchUidComp = o1.getPatchUid().compareTo(o2.getPatchUid());
      if (patchUidComp != 0) {
        return patchUidComp;
      }
      int patchIdxComp = Integer.compare(o1.getUpdateIdx(), o2.getUpdateIdx());
      return patchIdxComp;
    }
  }

  public static final TimestampPatchComparator TIMESTAMP_PATCH_COMPARATOR = new TimestampPatchComparator();


  public long getTimestamp();

  public String getGraphUid();


  public String getPatchUid();


  public int getUpdateIdx();

}
