package org.jboss.pressgang.ccms.contentspec.migratetool;

import java.util.List;
import java.util.Map;

import org.jboss.pressgang.ccms.contentspec.structures.StringToCSNodeCollection;
import org.jboss.pressgang.ccms.contentspec.utils.ContentSpecUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.EntityUtilities;
import org.jboss.pressgang.ccms.contentspec.utils.TranslationUtilities;
import org.jboss.pressgang.ccms.provider.ContentSpecProvider;
import org.jboss.pressgang.ccms.provider.DataProviderFactory;
import org.jboss.pressgang.ccms.provider.TranslatedContentSpecProvider;
import org.jboss.pressgang.ccms.rest.v1.entities.contentspec.RESTTextContentSpecV1;
import org.jboss.pressgang.ccms.utils.common.HashUtilities;
import org.jboss.pressgang.ccms.wrapper.ContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.TopicWrapper;
import org.jboss.pressgang.ccms.wrapper.TranslatedContentSpecWrapper;
import org.jboss.pressgang.ccms.wrapper.collection.CollectionWrapper;
import org.jboss.pressgang.ccms.zanata.ZanataDetails;
import org.jboss.pressgang.ccms.zanata.ZanataInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zanata.common.ContentType;
import org.zanata.common.LocaleId;
import org.zanata.common.ResourceType;
import org.zanata.rest.dto.resource.Resource;
import org.zanata.rest.dto.resource.TextFlow;

public class TranslationMigrator {
    private static final Logger log = LoggerFactory.getLogger(TranslationMigrator.class);

    private final ContentSpecProvider contentSpecProvider;
    private final DataProviderFactory providerFactory;
    private final ZanataInterface zanataInterface;

    public TranslationMigrator(final DataProviderFactory providerFactory, final ZanataDetails zanataDetails) {
        this.providerFactory = providerFactory;
        contentSpecProvider = providerFactory.getProvider(ContentSpecProvider.class);
        zanataInterface = new ZanataInterface(0.2, zanataDetails);
    }

    public void migrateContentSpecs(final CollectionWrapper<TopicWrapper> translatedContentSpecs, final Map<TopicWrapper,
            RESTTextContentSpecV1> mappedContentSpecs) {
        log.info("Starting to migrate the translations...");

        final int showPercent = 5;
        final float total = translatedContentSpecs.size();
        float current = 0;
        int lastPercent = 0;

        // Perform the migration of each content spec
        for (final TopicWrapper contentSpec : translatedContentSpecs.getItems()) {
            final RESTTextContentSpecV1 textContentSpec = mappedContentSpecs.get(contentSpec);
            if (textContentSpec != null) {
                final ContentSpecWrapper contentSpecEntity = contentSpecProvider.getContentSpec(textContentSpec.getId(),
                        textContentSpec.getRevision());
                pushContentSpecToZanata(contentSpecEntity);
                // Sleep just to give the server a rest
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {

                }
            }

            ++current;
            final int percent = Math.round(current / total * 100);
            if (percent - lastPercent >= showPercent) {
                lastPercent = percent;
                log.info("\tTranslation Migration " + percent + "% Done");
            }
        }

        log.info("Finished migrating the translations...");
    }

    /**
     * @param contentSpecEntity
     * @return
     */
    protected TranslatedContentSpecWrapper pushContentSpecToZanata(final ContentSpecWrapper contentSpecEntity) {
        final TranslatedContentSpecProvider translatedContentSpecProvider = providerFactory.getProvider(
                TranslatedContentSpecProvider.class);
        final String zanataId = "CS" + contentSpecEntity.getId() + "-" + contentSpecEntity.getRevision();
        final Resource zanataFile = zanataInterface.getZanataResource(zanataId);
        TranslatedContentSpecWrapper translatedContentSpec = EntityUtilities.getTranslatedContentSpecById(providerFactory,
                contentSpecEntity.getId(), contentSpecEntity.getRevision());

        if (zanataFile == null) {
            final Resource resource = new Resource();

            resource.setContentType(ContentType.TextPlain);
            resource.setLang(LocaleId.fromJavaName(contentSpecEntity.getLocale()));
            resource.setName(zanataId);
            resource.setRevision(1);
            resource.setType(ResourceType.FILE);

            final List<StringToCSNodeCollection> translatableStrings = ContentSpecUtilities.getTranslatableStrings(contentSpecEntity,
                    false);

            for (final StringToCSNodeCollection translatableStringData : translatableStrings) {
                final String translatableString = translatableStringData.getTranslationString();
                if (!translatableString.trim().isEmpty()) {
                    final TextFlow textFlow = new TextFlow();
                    textFlow.setContents(translatableString);
                    textFlow.setLang(LocaleId.fromJavaName(contentSpecEntity.getLocale()));
                    textFlow.setId(HashUtilities.generateMD5(translatableString));
                    textFlow.setRevision(1);

                    resource.getTextFlows().add(textFlow);
                }
            }

            // Create the document in Zanata
            if (!zanataInterface.createFile(resource)) {
                log.error("Content Spec ID {}, Revision {} failed to be created in Zanata.", contentSpecEntity.getId(),
                        contentSpecEntity.getRevision());
                return null;
            } else if (translatedContentSpec == null) {
                // Create the Translated Content Spec and it's nodes
                final TranslatedContentSpecWrapper newTranslatedContentSpec = TranslationUtilities.createTranslatedContentSpec(
                        providerFactory, contentSpecEntity);
                try {
                    // Save the translated content spec
                    translatedContentSpec = translatedContentSpecProvider.createTranslatedContentSpec(newTranslatedContentSpec);
                    if (translatedContentSpec == null) {
                        log.error("Content Spec ID {}, Revision {} failed to be created in PressGang.", contentSpecEntity.getId(),
                                contentSpecEntity.getRevision());
                        return null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("Content Spec ID {}, Revision {} failed to be created in PressGang.", contentSpecEntity.getId(),
                            contentSpecEntity.getRevision());
                    return null;
                }
            }
        } else {
            log.error("Content Spec ID {}, Revision {} already exists - Skipping.", contentSpecEntity.getId(),
                    contentSpecEntity.getRevision());
        }

        return translatedContentSpec;
    }
}
