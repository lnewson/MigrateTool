package org.jboss.pressgang.ccms.contentspec.migratetool;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.constants.CSConstants;
import org.jboss.pressgang.ccms.contentspec.enums.BookType;
import org.jboss.pressgang.ccms.wrapper.PropertyTagInTopicWrapper;
import org.jboss.pressgang.ccms.wrapper.TopicWrapper;

public class SQLGenerator {
    private static String CONTENT_SPEC_TABLE_FORMAT = "INSERT INTO `ContentSpec`(`ContentSpecID`, `ContentSpecType`, " +
            "`Locale`, `LastModified`) VALUES ('%d', '%d', '%s', '%s');";
    private static String CONTENT_SPEC_AUD_TABLE_FORMAT = "INSERT INTO `ContentSpec_AUD`(`ContentSpecID`, `REV`, `REVTYPE`, " +
            "`ContentSpecType`, `Locale`, `LastModified`) VALUES ('%d', @REV, '0', '%d', '%s', '%s');";
    private static String CONTENT_SPEC_ADDED_BY_PROPERTY_FORMAT = "INSERT INTO `ContentSpecToPropertyTag`(`ContentSpecID`, " +
            "`PropertyTagID`, `Value`) VALUES ('%d', '14', '%s');";
    private static String CONTENT_SPEC_ADDED_BY_PROPERTY_AUD_FORMAT = "INSERT INTO `ContentSpecToPropertyTag_AUD`" +
            "(`ContentSpecToPropertyTagID`, `ContentSpecID`, `PropertyTagID`, `Value`, `REV`, `REVTYPE`) VALUES (LAST_INSERT_ID(), '%d', " +
            "'14', '%s', @REV,'0');";
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public String generateSQL(final Map<Integer, LinkedHashMap<TopicWrapper, ContentSpec>> contentSpecs) {
        final Date date = new Date();
        if (contentSpecs != null && !contentSpecs.isEmpty()) {
            final StringBuilder sql = new StringBuilder(generateRevision() + "\n");
            for (final Map.Entry<Integer, LinkedHashMap<TopicWrapper, ContentSpec>> contentSpecEntry : contentSpecs.entrySet()) {
                final Map<TopicWrapper, ContentSpec> contentSpec = contentSpecEntry.getValue();
                for (final Map.Entry<TopicWrapper, ContentSpec> contentSpecEntry2 : contentSpec.entrySet()) {
                    sql.append(generateSQLInsert(contentSpecEntry2.getKey(), contentSpecEntry2.getValue(), date) + "\n");
                    break;
                }
            }

            return sql.toString();
        } else {
            return "";
        }
    }

    protected String generateSQLInsert(final TopicWrapper contentSpecEntity, final ContentSpec contentSpec, final Date date) {
        String contentSpecSQL = String.format(CONTENT_SPEC_TABLE_FORMAT, contentSpec.getId(),
                BookType.getBookTypeId(contentSpec.getBookType()), contentSpecEntity.getLocale(), dateFormat.format(date));
        String contentSpec_AUDSQL = String.format(CONTENT_SPEC_AUD_TABLE_FORMAT, contentSpec.getId(),
                BookType.getBookTypeId(contentSpec.getBookType()), contentSpecEntity.getLocale(), dateFormat.format(date));

        final PropertyTagInTopicWrapper addedBy = contentSpecEntity.getProperty(CSConstants.ADDED_BY_PROPERTY_TAG_ID);
        if (addedBy != null) {
            String contentSpecAddedBySQL = String.format(CONTENT_SPEC_ADDED_BY_PROPERTY_FORMAT, contentSpec.getId(),
                    addedBy.getValue());
            String contentSpecAddedBy_AUDSQL = String.format(CONTENT_SPEC_ADDED_BY_PROPERTY_AUD_FORMAT, contentSpec.getId(),
                    addedBy.getValue());

            return contentSpecSQL + "\n" + contentSpec_AUDSQL + "\n" + contentSpecAddedBySQL + "\n" +
                    contentSpecAddedBy_AUDSQL;
        } else {
            return contentSpecSQL + "\n" + contentSpec_AUDSQL;
        }
    }

    protected String generateRevision() {
        return "INSERT INTO `REVINFO` (`REV`, `REVTSTMP`, `Flag`, `Message`, `Username`) VALUES (NULL, UNIX_TIMESTAMP() * 1000, '1', " +
                "" + "'Migration of Content Specs from plain text to Database Tables', NULL);\nSET @REV = LAST_INSERT_ID();";
    }
}
