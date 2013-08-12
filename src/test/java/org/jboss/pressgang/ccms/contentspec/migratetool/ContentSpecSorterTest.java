package org.jboss.pressgang.ccms.contentspec.migratetool;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;

import java.util.LinkedHashMap;
import java.util.Map;

import net.sf.ipsedixit.annotation.Arbitrary;
import net.sf.ipsedixit.integration.junit.JUnit4IpsedixitTestRunner;
import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.wrapper.TopicWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4IpsedixitTestRunner.class)
public class ContentSpecSorterTest {

    @Arbitrary Integer revision1;
    @Arbitrary Integer revision2;
    @Arbitrary Integer revision3;
    @Mock TopicWrapper topicWrapper;
    @Mock TopicWrapper topicWrapper2;
    @Mock TopicWrapper topicWrapper3;
    final ContentSpec contentSpec = new ContentSpec();
    final ContentSpec contentSpec2 = new ContentSpec();
    final ContentSpec contentSpec3 = new ContentSpec();

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void should() {
        // Given
        LinkedHashMap<TopicWrapper, ContentSpec> contentSpecs = new LinkedHashMap<TopicWrapper, ContentSpec>();
        contentSpecs.put(topicWrapper, contentSpec);
        contentSpecs.put(topicWrapper2, contentSpec2);
        contentSpecs.put(topicWrapper3, contentSpec3);
        given(topicWrapper.getRevision()). willReturn(revision1);
        given(topicWrapper2.getRevision()). willReturn(revision2);
        given(topicWrapper3.getRevision()). willReturn(revision3);

        // When
        ContentSpecSorter.sort(contentSpecs);

        // Then
        assertThat(contentSpecs.size(), is(3));
        TopicWrapper lastTopic = null;
        for (final Map.Entry<TopicWrapper, ContentSpec> entry : contentSpecs.entrySet()) {
            if (lastTopic != null) {
                assertTrue(entry.getKey().getRevision() > lastTopic.getRevision());
            } else {
                lastTopic = entry.getKey();
            }
        }
    }
}
