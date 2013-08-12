package org.jboss.pressgang.ccms.contentspec.migratetool;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.wrapper.TopicWrapper;

public class ContentSpecSorter {
    private static final TopicRevisionComparator REVISION_COMPARATOR = new TopicRevisionComparator();

    public static void sort(final Map<TopicWrapper, ContentSpec> map) {
        sort(map, REVISION_COMPARATOR);
    }

    public static void sort(final Map<TopicWrapper, ContentSpec> map, final Comparator<Map.Entry<TopicWrapper, ContentSpec>> comparator) {
        // If the map is empty then just return
        if (map.isEmpty())
            return;

        // Create the list of entries
        final List<Map.Entry<TopicWrapper, ContentSpec>> entries = new LinkedList<Map.Entry<TopicWrapper, ContentSpec>>(map.entrySet());

        // sort the list based on the comparator
        Collections.sort(entries, comparator);

        // put the values back in the right order
        map.clear();
        for (final Map.Entry<TopicWrapper, ContentSpec> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }
    }
}

class TopicRevisionComparator implements Comparator<Map.Entry<TopicWrapper, ContentSpec>> {

    @Override
    public int compare(Map.Entry<TopicWrapper, ContentSpec> contentSpecEntry, Map.Entry<TopicWrapper, ContentSpec> contentSpecEntry2) {
        // Null ordering checks
        if (contentSpecEntry.getKey() == null && contentSpecEntry2.getKey() == null) {
            return 0;
        } else if (contentSpecEntry.getKey() == null) {
            return -1;
        } else if (contentSpecEntry2 == null) {
            return 1;
        }

        // Null ordering checks
        if (contentSpecEntry.getKey().getRevision() == null && contentSpecEntry2.getKey().getRevision() == null) {
            return 0;
        } else if (contentSpecEntry.getKey().getRevision() == null) {
            return -1;
        } else if (contentSpecEntry2.getKey().getRevision() == null) {
            return 1;
        }

        // Sort by revision
        if (contentSpecEntry.getKey().getRevision().equals(contentSpecEntry2.getKey().getRevision())) {
            return 0;
        } else if (contentSpecEntry.getKey().getRevision() < contentSpecEntry2.getKey().getRevision()) {
            return -1;
        } else {
            return 1;
        }
    }
}
