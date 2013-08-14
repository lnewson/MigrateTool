import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.jboss.pressgang.ccms.contentspec.ContentSpec;
import org.jboss.pressgang.ccms.contentspec.migratetool.Migrator;
import org.jboss.pressgang.ccms.contentspec.migratetool.SQLGenerator;
import org.jboss.pressgang.ccms.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.provider.RESTProviderFactory;
import org.jboss.pressgang.ccms.provider.TopicProvider;
import org.jboss.pressgang.ccms.provider.TranslatedTopicProvider;
import org.jboss.pressgang.ccms.rest.v1.constants.CommonFilterConstants;
import org.jboss.pressgang.ccms.rest.v1.entities.contentspec.RESTTextContentSpecV1;
import org.jboss.pressgang.ccms.rest.v1.query.RESTTopicQueryBuilderV1;
import org.jboss.pressgang.ccms.rest.v1.query.RESTTranslatedTopicQueryBuilderV1;
import org.jboss.pressgang.ccms.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.wrapper.TranslatedTopicWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;
import org.jboss.pressgang.ccms.zanata.ZanataDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        final RESTProviderFactory providerFactory = RESTProviderFactory.create(
                "http://localhost:8080/pressgang-ccms/rest/");
        final SQLGenerator generator = new SQLGenerator();
        final Migrator migrator = new Migrator(providerFactory);
        //final TranslationMigrator translationMigrator = new TranslationMigrator(providerFactory, createZanataDetails());
        final TopicProvider topicProvider = providerFactory.getProvider(TopicProvider.class);

        final RESTTopicQueryBuilderV1 queryBuilder = new RESTTopicQueryBuilderV1();
//        queryBuilder.setTag(CSConstants.CONTENT_SPEC_TAG_ID, CommonFilterConstants.MATCH_TAG_STATE);
//        // Ignore the following messed up content specs
//        queryBuilder.setNotTopicIds(Arrays.asList(9003,9021,16241,16298,16407,18204));
        queryBuilder.setTopicIds(Arrays.asList(8740));

        final CollectionWrapper<TopicWrapper> contentSpecs = topicProvider.getTopicsWithQuery(queryBuilder.getQuery());
        final CollectionWrapper<TopicWrapper> translatedContentSpecs = topicProvider.newTopicCollection();
        addPushedRevisionContentSpecs(contentSpecs, translatedContentSpecs, providerFactory);

        final Map<Integer, LinkedHashMap<TopicWrapper, ContentSpec>> contentSpecMap = migrator.parseContentSpecs(contentSpecs);

        final String sql = generator.generateSQL(contentSpecMap);

        File file = new File("Migrate.sql");
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(sql.getBytes("UTF-8"));
        fos.close();

        log.info("Run the " + file.getAbsolutePath() + " SQL Script on the server.");

        Scanner scanner = new Scanner(System.in);
        String answer = "";
        while (!answer.toLowerCase().matches("y|yes|exit")) {
            log.info("Has the SQL script been run on the server? (y/n)");
            answer = scanner.nextLine();
        }

        if (answer.toLowerCase().matches("y|yes")) {
            final Map<TopicWrapper, RESTTextContentSpecV1> migrationMapping = migrator.migrateContentSpecs(contentSpecMap);
            //translationMigrator.migrateContentSpecs(translatedContentSpecs, migrationMapping);
        } else {
            System.exit(-1);
        }
    }

    protected static ZanataDetails createZanataDetails() {
        final ZanataDetails zanataDetails = new ZanataDetails();
        zanataDetails.setProject("skynet-topics");
        zanataDetails.setVersion("1");

        zanataDetails.setServer("http://localhost:8280/zanata/");
        zanataDetails.setUsername("admin");
        zanataDetails.setToken("7002eace916783077727c1d5165f47ee");

        return zanataDetails;
    }

    protected static void addPushedRevisionContentSpecs(final CollectionWrapper<TopicWrapper> contentSpecs,
            final CollectionWrapper<TopicWrapper> translatedContentSpecs,
            final DataProviderFactory providerFactory) {
        final TranslatedTopicProvider translatedTopicProvider = providerFactory.getProvider(TranslatedTopicProvider.class);
        final List<TopicWrapper> latestContentSpecs = new ArrayList<TopicWrapper>(contentSpecs.getItems());
        for (final TopicWrapper contentSpec : latestContentSpecs) {
            final RESTTranslatedTopicQueryBuilderV1 queryBuilder = new RESTTranslatedTopicQueryBuilderV1();
            queryBuilder.setLocale("en-US", CommonFilterConstants.MATCH_LOCALE_STATE);
            queryBuilder.setTopicIds(Arrays.asList(contentSpec.getId()));
            final CollectionWrapper<TranslatedTopicWrapper> translatedTopics = translatedTopicProvider.getTranslatedTopicsWithQuery
                    (queryBuilder.getQuery());

            for (final TranslatedTopicWrapper translatedTopic : translatedTopics.getItems()) {
                final TopicWrapper revTopic = translatedTopic.getTopic();
                if (!contentSpec.getRevision().equals(revTopic.getRevision())) {
                    contentSpecs.addItem(revTopic);
                    translatedContentSpecs.addItem(revTopic);
                }
            }
        }
    }
}
